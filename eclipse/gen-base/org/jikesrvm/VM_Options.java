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
package org.jikesrvm;

import org.vmmagic.pragma.*;
import org.jikesrvm.annotations.NoSubArchCompile;

/**
 * Class to handle command-line arguments and options meant
 * for the core runtime system of the VM.
 * <p>
 * Note: This file is mechanically generated from VM_Options.template.
 * <p>
 * Note: Boolean options are defined in /Users/ross/Documents/PhD/jikes/jikesrvm_cell/rvm/src-generated/options/BooleanOptions.vm.dat
 *       All other options are defined in /Users/ross/Documents/PhD/jikes/jikesrvm_cell/rvm/src-generated/options/ValueOptions.vm.dat
 *       (value, enumeration)
 * <p>
 * NOTE: This class does not support all of the types of
 *       options found in the other Jikes RVM options
 *       files.  This is intentional and is done to enable
 *       -X:vm options to be processed very early in the
 *       booting process.
 *
 **/
@NoSubArchCompile
@Uninterruptible
public class VM_Options extends VM_Configuration {

   // Begin template-specified options
   public static boolean MeasureCompilation            = false; // Time all compilations and report on exit
   public static boolean MeasureCompilationPhases      = false; // Time all compilation sub-phases and report on exit
   public static boolean stackTraceFull                = false; // Stack traces to consist of VM and application frames
   public static boolean stackTraceAtExit              = false; // Dump a stack trace (via VM.syswrite) upon exit
   public static boolean TraceClassLoading             = false; // More detailed tracing then -verbose:class
   public static boolean ErrorsFatal                   = false; // Exit when non-fatal errors are detected; used for regression testing
   public static int maxSystemTroubleRecursionDepth    = 3; // If we get deeper than this in one of the System Trouble functions, try to die.
   public static int interruptQuantum                  = (VM.BuildForLinux&&VM.BuildForIA32)?20:10; // Timer interrupt scheduling quantum in ms
   public static int schedulingMultiplier              = 1; // Scheduling quantum = interruptQuantum * schedulingMultiplier
   public static int TraceThreadScheduling             = 0; // Trace actions taken by thread scheduling
   public static int VerboseStackTracePeriod           = 0; // Trace every nth time a stack trace is created
   public static String EdgeCounterFile                = null; // Input file of edge counter profile data
   public static int CBSCallSamplesPerTick             = 8; // How many CBS call samples (Prologue/Epilogue) should we take per time tick
   public static int CBSCallSampleStride               = 2; // Stride between each CBS call sample (Prologue/Epilogue) within a sampling window
   public static int CBSMethodSamplesPerTick           = 0; // How many CBS method samples (any yieldpoint) should we take per time tick
   public static int CBSMethodSampleStride             = 3; // Stride between each CBS method sample (any yieldpoint) within a sampling window
   // End template-specified options

   // Begin generated support for "Enumeration" options
   // End generated support for "Enumeration" options

  /**
   * Take a string (most likely a command-line argument) and try to proccess it
   * as an option command.  Return true if the string was understood, false
   * otherwise.
   *
   * @param arg a String to try to process as an option command
   * @return true if successful, false otherwise
   */
  @Interruptible
  @NoOptCompile
  public static boolean process(String arg) {

    // First handle the "option commands"
    if (arg.equals("help")) {
       printHelp();
       return true;
    }
    if (arg.equals("printOptions")) {
       printOptions();
       return true;
    }
    if (arg.length() == 0) {
      printHelp();
      return true;
    }

    // Required format of arg is 'name=value'
    // Split into 'name' and 'value' strings
    int split = arg.indexOf('=');
    if (split == -1) {
      VM.sysWrite("  Illegal option specification!\n  \""+arg+
                  "\" must be specified as a name-value pair in the form of option=value\n");
      return false;
    }
    String name = arg.substring(0,split);
    String value = arg.substring(split+1);

    // Begin generated command-line processing
    if (name.equals("measureCompilation")) {
      if (value.equals("true")) {
        MeasureCompilation = true;
        return true;
      } else if (value.equals("false")) {
          MeasureCompilation = false;
        return true;
      } else
        return false;
    }
    if (name.equals("measureCompilationPhases")) {
      if (value.equals("true")) {
        MeasureCompilationPhases = true;
        return true;
      } else if (value.equals("false")) {
          MeasureCompilationPhases = false;
        return true;
      } else
        return false;
    }
    if (name.equals("stackTraceFull")) {
      if (value.equals("true")) {
        stackTraceFull = true;
        return true;
      } else if (value.equals("false")) {
          stackTraceFull = false;
        return true;
      } else
        return false;
    }
    if (name.equals("stackTraceAtExit")) {
      if (value.equals("true")) {
        stackTraceAtExit = true;
        return true;
      } else if (value.equals("false")) {
          stackTraceAtExit = false;
        return true;
      } else
        return false;
    }
    if (name.equals("verboseTraceClassLoading")) {
      if (value.equals("true")) {
        TraceClassLoading = true;
        return true;
      } else if (value.equals("false")) {
          TraceClassLoading = false;
        return true;
      } else
        return false;
    }
    if (name.equals("errorsFatal")) {
      if (value.equals("true")) {
        ErrorsFatal = true;
        return true;
      } else if (value.equals("false")) {
          ErrorsFatal = false;
        return true;
      } else
        return false;
    }
    if (name.equals("maxSystemTroubleRecursionDepth")) {
       maxSystemTroubleRecursionDepth = VM_CommandLineArgs.primitiveParseInt(value);
       return true;
     }
    if (name.equals("interruptQuantum")) {
       interruptQuantum = VM_CommandLineArgs.primitiveParseInt(value);
       return true;
     }
    if (name.equals("schedulingMultiplier")) {
       schedulingMultiplier = VM_CommandLineArgs.primitiveParseInt(value);
       return true;
     }
    if (name.equals("traceThreadScheduling")) {
       TraceThreadScheduling = VM_CommandLineArgs.primitiveParseInt(value);
       return true;
     }
    if (name.equals("verboseStackTrace")) {
       VerboseStackTracePeriod = VM_CommandLineArgs.primitiveParseInt(value);
       return true;
     }
    if (name.equals("edgeCounterFile")) {
       EdgeCounterFile = value;
       return true;
     }
    if (name.equals("CBSCallSamplesPerTick")) {
       CBSCallSamplesPerTick = VM_CommandLineArgs.primitiveParseInt(value);
       return true;
     }
    if (name.equals("CBSCallSampleStride")) {
       CBSCallSampleStride = VM_CommandLineArgs.primitiveParseInt(value);
       return true;
     }
    if (name.equals("CBSMethodSamplesPerTick")) {
       CBSMethodSamplesPerTick = VM_CommandLineArgs.primitiveParseInt(value);
       return true;
     }
    if (name.equals("CBSMethodSampleStride")) {
       CBSMethodSampleStride = VM_CommandLineArgs.primitiveParseInt(value);
       return true;
     }
       //End generated command-line processing

    // None of the above tests matched, so this wasn't an option
    return false;
  }

  // Print a short description of every option
  public static void printHelp() {

    VM.sysWrite("Commands\n");
    VM.sysWrite("-X:vm[:help]\t\t\tPrint brief description of the core VM's command-line arguments\n");
    VM.sysWrite("-X:vm:printOptions\t\tPrint the current values of the core VM's options\n");
    VM.sysWrite("\n");

    //Begin generated help messages
    VM.sysWrite("Boolean Options (-X:vm:<option>=true or -X:vm:<option>=false)\n");
    VM.sysWrite("Option                                 Description\n");
    VM.sysWrite("measureCompilation             Time all compilations and report on exit\n");
    VM.sysWrite("measureCompilationPhases       Time all compilation sub-phases and report on exit\n");
    VM.sysWrite("stackTraceFull                 Stack traces to consist of VM and application frames\n");
    VM.sysWrite("stackTraceAtExit               Dump a stack trace (via VM.syswrite) upon exit\n");
    VM.sysWrite("verboseTraceClassLoading       More detailed tracing then -verbose:class\n");
    VM.sysWrite("errorsFatal                    Exit when non-fatal errors are detected; used for regression testing\n");
    VM.sysWrite("\nValue Options (-X:vm:<option>=<value>)\n");
    VM.sysWrite("Option                         Type    Description\n");
    VM.sysWrite("maxSystemTroubleRecursionDepth int     If we get deeper than this in one of the System Trouble functions, try to die.\n");
    VM.sysWrite("interruptQuantum               int     Timer interrupt scheduling quantum in ms\n");
    VM.sysWrite("schedulingMultiplier           int     Scheduling quantum = interruptQuantum * schedulingMultiplier\n");
    VM.sysWrite("traceThreadScheduling          int     Trace actions taken by thread scheduling\n");
    VM.sysWrite("verboseStackTrace              int     Trace every nth time a stack trace is created\n");
    VM.sysWrite("edgeCounterFile                String  Input file of edge counter profile data\n");
    VM.sysWrite("CBSCallSamplesPerTick          int     How many CBS call samples (Prologue/Epilogue) should we take per time tick\n");
    VM.sysWrite("CBSCallSampleStride            int     Stride between each CBS call sample (Prologue/Epilogue) within a sampling window\n");
    VM.sysWrite("CBSMethodSamplesPerTick        int     How many CBS method samples (any yieldpoint) should we take per time tick\n");
    VM.sysWrite("CBSMethodSampleStride          int     Stride between each CBS method sample (any yieldpoint) within a sampling window\n");
    VM.sysWrite("\nSelection Options (set option to one of an enumeration of possible values)\n");

    VM.sysExit(VM.EXIT_STATUS_PRINTED_HELP_MESSAGE);
  }

  // print the options values
  public static void printOptions() {
    VM.sysWrite("Current value of VM options:\n");
    //Begin generated option value printing
    VM.sysWriteln("\tmeasureCompilation             = ",MeasureCompilation);
    VM.sysWriteln("\tmeasureCompilationPhases       = ",MeasureCompilationPhases);
    VM.sysWriteln("\tstackTraceFull                 = ",stackTraceFull);
    VM.sysWriteln("\tstackTraceAtExit               = ",stackTraceAtExit);
    VM.sysWriteln("\tverboseTraceClassLoading       = ",TraceClassLoading);
    VM.sysWriteln("\terrorsFatal                    = ",ErrorsFatal);
    VM.sysWriteln("\tmaxSystemTroubleRecursionDepth = ",maxSystemTroubleRecursionDepth);
    VM.sysWriteln("\tinterruptQuantum               = ",interruptQuantum);
    VM.sysWriteln("\tschedulingMultiplier           = ",schedulingMultiplier);
    VM.sysWriteln("\ttraceThreadScheduling          = ",TraceThreadScheduling);
    VM.sysWriteln("\tverboseStackTrace              = ",VerboseStackTracePeriod);
    VM.sysWriteln("\tedgeCounterFile                = ",EdgeCounterFile);
    VM.sysWriteln("\tCBSCallSamplesPerTick          = ",CBSCallSamplesPerTick);
    VM.sysWriteln("\tCBSCallSampleStride            = ",CBSCallSampleStride);
    VM.sysWriteln("\tCBSMethodSamplesPerTick        = ",CBSMethodSamplesPerTick);
    VM.sysWriteln("\tCBSMethodSampleStride          = ",CBSMethodSampleStride);
    //End generated option value printing
  }
}
