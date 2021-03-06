/*
 *  This file is part of the Jikes RVM project (http://jikesrvm.org).
 *
 *  This file is licensed to You under the Common Public License (CPL);
 *  You may not use this file except in compliance with the License. You
 *  may obtain a copy of the License at
 *
 *      http://www.opensource.org/licenses/cpl1.0.php
 *
 *  See the COPYRIGHT.txt file distributed with this work for information
 *  regarding copyright ownership.
 */
package org.jikesrvm.adaptive.controller;

import java.util.Enumeration;
import java.util.Vector;
import org.jikesrvm.VM;
import org.jikesrvm.VM_Callbacks;
import org.jikesrvm.adaptive.OSR_OrganizerThread;
import org.jikesrvm.adaptive.database.VM_AOSDatabase;
import org.jikesrvm.adaptive.database.callgraph.VM_PartialCallGraph;
import org.jikesrvm.adaptive.database.methodsamples.VM_MethodCountData;
import org.jikesrvm.adaptive.measurements.VM_RuntimeMeasurements;
import org.jikesrvm.adaptive.measurements.instrumentation.VM_Instrumentation;
import org.jikesrvm.adaptive.measurements.organizers.VM_Organizer;
import org.jikesrvm.adaptive.recompilation.VM_CompilationThread;
import org.jikesrvm.adaptive.recompilation.instrumentation.VM_CounterBasedSampling;
import org.jikesrvm.adaptive.util.VM_AOSLogging;
import org.jikesrvm.adaptive.util.VM_AOSOptions;
import org.jikesrvm.adaptive.util.VM_BlockingPriorityQueue;
import org.jikesrvm.compilers.baseline.VM_EdgeCounts;
import org.jikesrvm.compilers.common.VM_RecompilationManager;
import org.jikesrvm.scheduler.greenthreads.VM_GreenProcessor;

// TODO - deal with for subarch

/**
 * This class contains top level adaptive compilation subsystem functions.
 */
public class VM_Controller implements VM_Callbacks.ExitMonitor,
                                      VM_Callbacks.AppStartMonitor,
                                      VM_Callbacks.AppCompleteMonitor,
                                      VM_Callbacks.AppRunStartMonitor,
                                      VM_Callbacks.AppRunCompleteMonitor,
                                      VM_Callbacks.RecompileAllDynamicallyLoadedMethodsMonitor {

  /**
   * Signals when the options and (optional) logging mechanism are enabled
   */
  public static boolean enabled = false;

  /**
   * Controller subsystem control options
   */
  public static VM_AOSOptions options = new VM_AOSOptions();

  /**
   * Deferred command line arguments for the opt compiler
   */
  private static String[] optCompilerOptions = new String[0];

  /**
   * Add a deferred command line argument
   */
  public static void addOptCompilerOption(String arg) {
    String[] tmp = new String[optCompilerOptions.length + 1];
    for (int i = 0; i < optCompilerOptions.length; i++) {
      tmp[i] = optCompilerOptions[i];
    }
    tmp[optCompilerOptions.length] = arg;
    optCompilerOptions = tmp;
  }

  /**
   * Get the deferred command line arguments
   */
  public static String[] getOptCompilerOptions() {return optCompilerOptions;}

  /**
   * The controller thread, it makes all the decisions
   * (the thread sets this field when it is created.)
   */
  public static VM_ControllerThread controllerThread = null;

  /**
   * Thread that will perform opt-compilations as directed by the controller
   * (the thread sets this field when it is created.)
   */
  public static VM_CompilationThread compilationThread = null;

  /**
   * Thread collecting osr request and pass it to controllerThread
   */
  public static OSR_OrganizerThread osrOrganizer = null;

  /**
   * Threads that will organize profile data as directed by the controller
   */
  public static Vector<VM_Organizer> organizers = new Vector<VM_Organizer>();

  /**
   * A blocking priority queue where organizers place events to
   * be processed by the controller
   * (an input to the controller thread)
   */
  public static VM_BlockingPriorityQueue controllerInputQueue;

  /**
   * A blocking priority queue where the controller will place methods
   * to be opt compiled
   * (an output of the controller thread)
   */
  public static VM_BlockingPriorityQueue compilationQueue;

  /**
   * The strategy used to make recompilation decisions
   */
  public static VM_RecompilationStrategy recompilationStrategy;

  /**
   * Controller virtual clock, ticked every taken yieldpoint.
   */
  public static int controllerClock = 0;

  /**
   * The main hot method raw data object.
   */
  public static VM_MethodCountData methodSamples;

  /**
   * The dynamic call graph
   */
  public static VM_PartialCallGraph dcg;

  /**
   * Used to shut down threads
   */
  private static final ThreadDeath threadDeath = new ThreadDeath();

  /**
   * Has the execution of boot completed successfully?
   */
  private static boolean booted = false;

  /**
   * Initialize the controller subsystem (called from VM.boot)
   * This method is called AFTER the command line options are processed.
   */
  public static void boot() {
    // Signal that the options and (optional) logging mechanism are set
    // VM_RuntimeCompiler checks this flag
    enabled = true;

    // Initialize the controller input queue
    controllerInputQueue = new VM_BlockingPriorityQueue(new VM_BlockingPriorityQueue.CallBack() {
      public void aboutToWait() { controllerThread.aboutToWait(); }
      public void doneWaiting() { controllerThread.doneWaiting(); }
    });

    compilationQueue = new VM_BlockingPriorityQueue();

    // Create the analytic model used to make cost/benefit decisions.
    recompilationStrategy = new VM_MultiLevelAdaptiveModel();

    // boot the runtime measurement systems
    VM_RuntimeMeasurements.boot();

    // Initialize subsystems, if being used
    VM_AdaptiveInlining.boot(options);

    // boot any instrumentation options
    VM_Instrumentation.boot(options);

    // boot the aos database
    VM_AOSDatabase.boot(options);

    VM_CounterBasedSampling.boot(options);

    createControllerThread();

    VM_Controller controller = new VM_Controller();
    VM_Callbacks.addExitMonitor(controller);
    VM_Callbacks.addAppStartMonitor(controller);
    VM_Callbacks.addAppCompleteMonitor(controller);
    VM_Callbacks.addAppRunStartMonitor(controller);
    VM_Callbacks.addAppRunCompleteMonitor(controller);

    // make sure the user hasn't explicitly prohibited this functionality
    if (!options.DISABLE_RECOMPILE_ALL_METHODS) {
      VM_Callbacks.addRecompileAllDynamicallyLoadedMethodsMonitor(controller);
    }

    booted = true;
  }

  /**
   * To be called when the VM is about to exit.
   * @param value the exit value
   */
  public void notifyExit(int value) {
    report();
  }

  /**
   * To be called when the application starts
   * @param app the application name
   */
  public void notifyAppStart(String app) {
    VM_AOSLogging.appStart(app);
    VM_AOSLogging.recordRecompAndThreadStats();
  }

  /**
   * To be called when the application completes
   * @param app the application name
   */
  public void notifyAppComplete(String app) {
    VM_AOSLogging.appComplete(app);
    VM_AOSLogging.recordRecompAndThreadStats();
  }

  /**
   * To be called when the application completes one of its run
   * @param app the application name
   * @param run the run number, i.e. what iteration of the app we have started
   */
  public void notifyAppRunStart(String app, int run) {
    VM_AOSLogging.appRunStart(app, run);
    VM_AOSLogging.recordRecompAndThreadStats();
  }

  /**
   * To be called when the application completes one of its run
   * @param app the application name
   * @param run the run number, i.e. what iteration of the app we have completed
   */
  public void notifyAppRunComplete(String app, int run) {
    VM_AOSLogging.appRunComplete(app, run);
    VM_AOSLogging.recordRecompAndThreadStats();
  }

  /**
   * Called when the application wants to recompile all dynamically
   *  loaded methods.  This can be expensive!
   */
  public void notifyRecompileAll() {
    VM_AOSLogging.recompilingAllDynamicallyLoadedMethods();
    VM_RecompilationManager.recompileAllDynamicallyLoadedMethods(false, false);
  }

  // Create the ControllerThread
  static void createControllerThread() {
    Object sentinel = new Object();
    VM_ControllerThread tt = new VM_ControllerThread(sentinel);
    tt.start();
    // wait until controller threads are up and running.
    try {
      synchronized (sentinel) {
        sentinel.wait();
      }
    } catch (Exception e) {
      e.printStackTrace();
      VM.sysFail("Failed to start up controller subsystem");
    }
  }

  /**
   * Process any command line arguments passed to the controller subsystem.
   * <p>
   * This method has the responsibility of creating the options object
   * if it does not already exist
   * <p>
   * NOTE: All command line argument processing should be handled via
   * the automatically generated code in VM_AOSOptions.java.
   * Don't even think of adding handwritten stuff here! --dave
   *
   * @param arg the command line argument to be processed
   */
  public static void processCommandLineArg(String arg) {
    if (!options.processAsOption("-X:aos", arg)) {
      VM.sysWrite("vm: illegal adaptive configuration directive \"" + arg + "\" specified as -X:aos:" + arg + "\n");
      VM.sysExit(VM.EXIT_STATUS_BOGUS_COMMAND_LINE_ARG);
    }
  }

  /**
   * This method is called when the VM is exiting to provide a hook to allow
   * the adpative optimization subsystem to generate a summary report.
   * It can also be called directly from driver programs to allow
   * reporting on a single run of a benchmark that the driver program
   * is executing in a loop (in which case the adaptive system isn't actually
   * exiting.....so some of the log messages may get a little wierd).
   */
  public static void report() {
    if (!booted) return;
    VM_ControllerThread.report();
    VM_RuntimeMeasurements.report();

    for (Enumeration<VM_Organizer> e = organizers.elements(); e.hasMoreElements();) {
      VM_Organizer organizer = e.nextElement();
      organizer.report();
    }

    if (options.FINAL_REPORT_LEVEL >= 2) {
      VM_EdgeCounts.dumpCounts();
      dcg.dumpGraph();
    }

    if (options.REPORT_INTERRUPT_STATS) {
      VM.sysWriteln("Timer Interrupt and Listener Stats");
      VM.sysWriteln("\tTotal number of clock ticks ", VM_GreenProcessor.timerTicks);
      VM.sysWriteln("\tReported clock ticks ", VM_GreenProcessor.reportedTimerTicks);
      VM.sysWriteln("\tController clock ", controllerClock);
      VM.sysWriteln("\tNumber of method samples taken ", (int) methodSamples.getTotalNumberOfSamples());
    }

    VM_AOSLogging.systemExiting();
  }

  /**
   * Stop all AOS threads and exit the adaptive system.
   * Can be used to assess code quality in a steady state by
   * allowing the adaptive system to run "for a while" and then
   * stoppping it
   */
  public static void stop() {
    if (!booted) return;

    VM.sysWriteln("AOS: Killing all adaptive system threads");
    for (Enumeration<VM_Organizer> e = organizers.elements(); e.hasMoreElements();) {
      VM_Organizer organizer = e.nextElement();
      organizer.kill(threadDeath, true);
    }
    compilationThread.kill(threadDeath, true);
    controllerThread.kill(threadDeath, true);
    VM_RuntimeMeasurements.stop();
    report();
  }
}
