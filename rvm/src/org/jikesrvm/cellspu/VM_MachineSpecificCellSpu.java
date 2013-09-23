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
package org.jikesrvm.cellspu;

import org.jikesrvm.VM_Registers;
import org.jikesrvm.VM_MachineSpecific;
import org.jikesrvm.SubordinateArchitecture.VM_Assembler;
import org.jikesrvm.compilers.common.assembler.VM_AbstractAssembler;
import org.vmmagic.pragma.Interruptible;
import org.vmmagic.pragma.Uninterruptible;
import org.vmmagic.unboxed.LocalAddress;
import org.vmmagic.unboxed.Offset;

/**
 * Wrappers around cell-spu specific code common to both 32 & 64 bit
 */
public abstract class VM_MachineSpecificCellSpu extends VM_MachineSpecific implements VM_ArchConstants, VM_BaselineConstants{

  /**
   * Wrappers around CellSpu-specific code (32-bit specific)
   */
  public static final class CellSpu extends VM_MachineSpecificCellSpu {
    public static final CellSpu singleton = new CellSpu();
  }


  /**
   * The following method will emit code that moves a reference to an
   * object's TIB into a destination register.
   * 
   * Uses S0
   *
   * @param asm the assembler object to emit code with
   * @param dest the number of the destination register
   * @param object the number of the register holding the object reference
   * @param tibOffset the offset of the tib from the object header
   */
  @Interruptible
  public final void baselineEmitLoadTIB(VM_AbstractAssembler abstractAsm, int dest, int object, Offset tibOffset) {
    VM_Assembler asm = (VM_Assembler) abstractAsm;
    asm.emitLoadUnaligned(S1, object, tibOffset);
    asm.emitANDI(dest, S1, 0x1ff);   // mask index off index from status word
  }

  /**
   * The following method initializes a thread stack as if
   * "startoff" method had been called by an empty baseline-compiled
   *  "sentinel" frame with one local variable
   *
   * @param contextRegisters The context registers for this thread
   * @param ip The instruction pointer for the "startoff" method
   * @param sp The base of the stack
   */
  @Uninterruptible
  public final void initializeStack(VM_Registers contextRegisters, LocalAddress ip, LocalAddress sp) {
    // TODO - Finish this method
  }

}
