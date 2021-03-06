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

import org.jikesrvm.VM_Registers;
import org.jikesrvm.compilers.common.assembler.VM_AbstractAssembler;
import org.vmmagic.pragma.Uninterruptible;
import org.vmmagic.unboxed.LocalAddress;
import org.vmmagic.unboxed.Offset;

/**
 * Wrappers around machine specific code
 */
public abstract class VM_MachineSpecific {

  /* common to all ISAs */
  /**
   * The following method will emit code that moves a reference to an
   * object's TIB into a destination register.
   *
   * @param asm the assembler object to emit code with
   * @param dest the number of the destination register
   * @param object the number of the register holding the object reference
   * @param tibOffset the offset of the tib from the object header
   */
  public abstract void baselineEmitLoadTIB(VM_AbstractAssembler asm, int dest, int object, Offset tibOffset);

  /**
   * The following method initializes a thread stack as if
   * "startoff" method had been called by an empty baseline-compiled
   * "sentinel" frame with one local variable.
   *
   * @param contextRegisters The context registers for this thread
   * @param ip The instruction pointer for the "startoff" method
   * @param sp The base of the stack
   */
  public abstract void initializeStack(VM_Registers contextRegisters, LocalAddress ip, LocalAddress sp);

  /* unique to IA */
  /**
   * A thread's stack has been moved or resized.
   * Adjust the ESP register to reflect new position.
   *
   * @param registers The registers for this thread
   * @param delta The displacement to be applied
   * @param traceAdjustments Log all adjustments to stderr if true
   */
  @Uninterruptible
  public void adjustESP(VM_Registers registers, Offset delta, boolean traceAdjustments) {
    if (VM.VerifyAssertions) VM._assert(VM.NOT_REACHED);
  }
}
