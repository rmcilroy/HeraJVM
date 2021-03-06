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
package org.jikesrvm.compilers.opt;

import org.jikesrvm.VM;
import org.jikesrvm.compilers.opt.ir.OPT_IR;

/**
 * An element in the opt compiler's optimzation plan
 * that aggregates together other OptimizationPlan elements.
 *
 * NOTE: Instances of subclasses of this class are
 *       held in OPT_OptimizationPlanner.masterPlan
 *       and thus represent global state.
 *       It is therefore incorrect for any per-compilation
 *       state to be stored in an instance field of
 *       one of these objects.
 */
public class OPT_OptimizationPlanCompositeElement extends OPT_OptimizationPlanElement {
  /**
   * Name of this element.
   */
  private String myName;
  /**
   * Ordered list of elements that together comprise this element.
   */
  private OPT_OptimizationPlanElement[] myElements;

  /**
   * Compose together the argument elements into a composite element
   * of an optimization plan.
   *
   * @param   n     The name for this phase
   * @param   e     The elements to compose
   */
  public OPT_OptimizationPlanCompositeElement(String n, OPT_OptimizationPlanElement[] e) {
    myName = n;
    myElements = e;
  }

  /**
   * Compose together the argument elements into a composite element
   * of an optimization plan.
   *
   * @param   n     The name for this phase
   * @param   e     The elements to compose
   */
  public OPT_OptimizationPlanCompositeElement(String n, Object[] e) {
    myName = n;
    myElements = new OPT_OptimizationPlanElement[e.length];
    for (int i = 0; i < e.length; i++) {
      if (e[i] instanceof OPT_OptimizationPlanElement) {
        myElements[i] = (OPT_OptimizationPlanElement) (e[i]);
      } else if (e[i] instanceof OPT_CompilerPhase) {
        myElements[i] = new OPT_OptimizationPlanAtomicElement((OPT_CompilerPhase) e[i]);
      } else {
        throw new OPT_OptimizingCompilerException("Unsupported plan element " + e[i]);
      }
    }
  }

  /**
   * This method is called to initialize the optimization plan support
   *  measuring compilation.
   */
  public void initializeForMeasureCompilation() {
    // initialize each composite object
    for (OPT_OptimizationPlanElement myElement : myElements) {
      myElement.initializeForMeasureCompilation();
    }
  }

  /**
   * Compose together the argument elements into a composite element
   * of an optimization plan.
   *
   * @param name The name associated with this composite.
   * @param elems An Object[] of OPT_CompilerPhases or
   *              OPT_OptimizationPlanElements to be composed
   * @return an OPT_OptimizationPlanCompositeElement that
   *         represents the composition.
   */
  public static OPT_OptimizationPlanCompositeElement compose(String name, Object[] elems) {
    return new OPT_OptimizationPlanCompositeElement(name, elems);
  }

  /**
   * Determine, possibly by consulting the passed options object,
   * if this optimization plan element should be performed.
   *
   * @param options The OPT_Options object for the current compilation.
   * @return true if the plan element should be performed.
   */
  public boolean shouldPerform(OPT_Options options) {
    for (OPT_OptimizationPlanElement myElement : myElements) {
      if (myElement.shouldPerform(options)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns true if the phase wants the IR dumped before and/or after it runs.
   * By default, printing is not enabled.
   * Subclasses should overide this method if they want to provide IR dumping.
   *
   * @param options the compiler options for the compilation
   * @param before true when invoked before perform, false otherwise.
   * @return true if the IR should be printed, false otherwise.
   */
  public boolean printingEnabled(OPT_Options options, boolean before) {
    return false;
  }

  /**
   * Do the work represented by this element in the optimization plan.
   * The assumption is that the work will modify the IR in some way.
   *
   * @param ir The OPT_IR object to work with.
   */
  public final void perform(OPT_IR ir) {
    if (printingEnabled(ir.options, true)) {
      if (!ir.options.hasMETHOD_TO_PRINT() || ir.options.fuzzyMatchMETHOD_TO_PRINT(ir.method.toString())) {
        OPT_CompilerPhase.dumpIR(ir, "Before " + getName());
      }
    }

    for (OPT_OptimizationPlanElement myElement : myElements) {
      if (myElement.shouldPerform(ir.options)) {
        myElement.perform(ir);
      }
    }

    if (printingEnabled(ir.options, false)) {
      if (!ir.options.hasMETHOD_TO_PRINT() || ir.options.fuzzyMatchMETHOD_TO_PRINT(ir.method.toString())) {
        OPT_CompilerPhase.dumpIR(ir, "After " + getName());
      }
    }
  }

  /**
   * @return a String which is the name of the phase.
   */
  public String getName() {
    return myName;
  }

  /**
   * Generate (to the sysWrite stream) a report of the
   * time spent performing this element of the optimization plan.
   *
   * @param indent Number of spaces to indent report.
   * @param timeCol Column number of time portion of report.
   * @param totalTime Total opt compilation time in seconds.
   */
  public final void reportStats(int indent, int timeCol, double totalTime) {
    double myTime = elapsedTime();
    if (myTime < 0.000001) {
      return;
    }
    // (1) Print header.
    int curCol = 0;
    for (curCol = 0; curCol < indent; curCol++) {
      VM.sysWrite(" ");
    }
    int myNamePtr = 0;
    while (curCol < timeCol && myNamePtr < myName.length()) {
      VM.sysWrite(myName.charAt(myNamePtr));
      myNamePtr++;
      curCol++;
    }
    VM.sysWrite("\n");
    // (2) print elements
    for (OPT_OptimizationPlanElement myElement : myElements) {
      myElement.reportStats(indent + 4, timeCol, totalTime);
    }
    // (3) print total
    curCol = 0;
    for (curCol = 0; curCol < indent + 4; curCol++) {
      VM.sysWrite(" ");
    }
    VM.sysWrite("TOTAL ");
    curCol += 6;
    while (curCol < timeCol) {
      VM.sysWrite(" ");
      curCol++;
    }
    prettyPrintTime(myTime, totalTime);
    VM.sysWriteln();
  }

  /**
   * Report the elapsed time spent in the PlanElement
   * @return time spend in the plan (in seconds)
   */
  public double elapsedTime() {
    double total = 0.0;
    for (OPT_OptimizationPlanElement myElement : myElements) {
      total += myElement.elapsedTime();
    }
    return total;
  }
}
