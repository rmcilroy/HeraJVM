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
package org.jikesrvm.adaptive.measurements.instrumentation;

import org.jikesrvm.adaptive.database.VM_AOSDatabase;
import org.jikesrvm.adaptive.measurements.VM_RuntimeMeasurements;
import org.jikesrvm.adaptive.util.VM_AOSOptions;
import org.jikesrvm.compilers.opt.OPT_InstrumentedEventCounterManager;

/**
 * VM_Instrumentation.java
 *
 * This class is used to provide general functionality useful to
 * instrumenting methods.
 *
 *
 */

public final class VM_Instrumentation {

  /**
   * A pointer to a OPT_InstrumentedEventCounterManager, (See
   * VM_InstrumentedEventCounterManager.java for the idea behind a
   * counter manager) There can be multiple managers in use at the
   * same time (for example, one per method)., but for now we just use
   * one for everything.
   **/
  public static OPT_InstrumentedEventCounterManager eventCounterManager;

  /**
   * Called at boot time
   **/
  public static void boot(VM_AOSOptions options) {

    // If the system may perform any instrumentation that uses managed
    // event counters, initialize a counter manager here.
    if (options
        .INSERT_INSTRUCTION_COUNTERS ||
                                     options
                                         .INSERT_METHOD_COUNTERS_OPT ||
                                                                     options
                                                                         .INSERT_YIELDPOINT_COUNTERS ||
                                                                                                     options
                                                                                                         .INSERT_DEBUGGING_COUNTERS) {
      eventCounterManager = new VM_CounterArrayManager();
    }

    // If inserting method counters, initialize the counter space for
    // the invocation counters, using the eventCounterManager from above.
    if (options.INSERT_METHOD_COUNTERS_OPT) {
      VM_AOSDatabase.methodInvocationCounterData = new VM_MethodInvocationCounterData(eventCounterManager);

      // Method Counters have only one array of counters for the whole
      // program, so initialize it here. Make it automitacally double
      // in size when needed.
      VM_AOSDatabase.methodInvocationCounterData.
          automaticallyGrowCounters(true);

      // Report at end
      VM_RuntimeMeasurements.
          registerReportableObject(VM_AOSDatabase.methodInvocationCounterData);
    }

    /**
     * If collecting yieldpoint counts, initialize the
     * data here.
     **/
    if (options.INSERT_YIELDPOINT_COUNTERS) {
      // Create it here, because we need only one array of numbers,
      // not one per method.
      VM_AOSDatabase.yieldpointCounterData = new VM_YieldpointCounterData(eventCounterManager);

      // We want to report everything at the end.
      VM_RuntimeMeasurements.
          registerReportableObject(VM_AOSDatabase.yieldpointCounterData);

    }

    /**
     * If collecting instruction counts, initialize the
     * data here.
     **/
    if (options.INSERT_INSTRUCTION_COUNTERS) {
      VM_AOSDatabase.instructionCounterData = new VM_StringEventCounterData(eventCounterManager, "Instruction Counter");
      VM_AOSDatabase.instructionCounterData.automaticallyGrowCounters(true);

      // We want to report everything at the end.
      VM_RuntimeMeasurements.
          registerReportableObject(VM_AOSDatabase.instructionCounterData);
    }

    /**
     * If collecting instruction counts, initialize the
     * data here.
     **/
    if (options.INSERT_DEBUGGING_COUNTERS) {
      VM_AOSDatabase.debuggingCounterData = new VM_StringEventCounterData(eventCounterManager, "Debugging Counters");
      VM_AOSDatabase.debuggingCounterData.automaticallyGrowCounters(true);

      // We want to report everything at the end.
      VM_RuntimeMeasurements.
          registerReportableObject(VM_AOSDatabase.debuggingCounterData);
    }

  }

  /**
   * Calling this routine causes all future compilations not to insert
   * instrumentation, regardless of what the options say.  Used during
   * system shutdown.  Note, this method will not stop instrumentation
   * in currently compiled methods from executing.
   */
  static void disableInstrumentation() {
    instrumentationEnabled = false;
  }

  /**
   * Enable instrumentations, so that future compilations will not
   * perform any instrumentation.
   */
  static void enableInstrumentation() {
    instrumentationEnabled = true;
  }

  /**
   * Is it currently O.K. to compile a method and insert instrumentation?
   */
  public static boolean instrumentationEnabled() {
    return instrumentationEnabled;
  }

  private static boolean instrumentationEnabled = true;
}
