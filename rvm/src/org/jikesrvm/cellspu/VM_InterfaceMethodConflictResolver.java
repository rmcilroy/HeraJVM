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

import org.jikesrvm.SubordinateArchitecture;
import org.jikesrvm.VM;
import org.jikesrvm.classloader.VM_Method;
import org.jikesrvm.compilers.common.assembler.cellspu.VM_Assembler;
import org.jikesrvm.compilers.common.assembler.cellspu.VM_AssemblerConstants;
import org.jikesrvm.objectmodel.VM_ObjectModel;
import org.jikesrvm.runtime.VM_Magic;
import org.jikesrvm.runtime.VM_Memory;

/**
 * Generates a custom IMT-conflict resolution stub.
 * We create a binary search tree.
 */
public abstract class VM_InterfaceMethodConflictResolver implements VM_BaselineConstants, VM_AssemblerConstants {

  // Create a conflict resolution stub for the set of interface method signatures l.
  //
  public static SubordinateArchitecture.VM_CodeArray createStub(int[] sigIds, VM_Method[] targets) {
    // (1) Create an assembler.
    int numEntries = sigIds.length;
    VM_Assembler asm = new SubordinateArchitecture.VM_Assembler(numEntries); // pretend each entry is a bytecode

    // (2) signatures must be in ascending order (to build binary search tree).
    if (VM.VerifyAssertions) {
      for (int i = 1; i < sigIds.length; i++) {
        VM._assert(sigIds[i - 1] < sigIds[i]);
      }
    }

    // (3) Assign synthetic bytecode numbers to each switch such that we'll generate them
    // in ascending order.  This lets us use the general forward branching mechanisms
    // of the VM_Assembler.
    int[] bcIndices = new int[numEntries];
    assignBytecodeIndices(0, bcIndices, 0, numEntries - 1);

    // (4) Generate the stub.
    insertStubPrologue(asm);
    insertStubCase(asm, sigIds, targets, bcIndices, 0, numEntries - 1);

    SubordinateArchitecture.VM_CodeArray stub = null; //asm.makeMachineCode().getInstructions();

    // (5) synchronize icache with generated machine code that was written through dcache
    if (VM.runningVM) VM_Memory.sync(VM_Magic.objectAsAddress(stub), stub.length() << LG_INSTRUCTION_WIDTH);

    return stub;
  }

  // Assign ascending bytecode indices to each case (in the order they will be generated)
  private static int assignBytecodeIndices(int bcIndex, int[] bcIndices, int low, int high) {
    int middle = (high + low) / 2;
    bcIndices[middle] = bcIndex++;
    if (low == middle && middle == high) {
      return bcIndex;
    } else {
      // Recurse.
      if (low < middle) {
        bcIndex = assignBytecodeIndices(bcIndex, bcIndices, low, middle - 1);
      }
      if (middle < high) {
        bcIndex = assignBytecodeIndices(bcIndex, bcIndices, middle + 1, high);
      }
      return bcIndex;
    }
  }

  // Make a stub prologue: get TIB into S0
  // factor out to reduce code space in each call.
  //
  private static void insertStubPrologue(VM_Assembler asm) {
    // VM_ObjectModel.baselineEmitLoadTIB((SubordinateArchitecture.VM_Assembler) asm, S0, T0);
  }

  // Generate a subtree covering from low to high inclusive.
  private static void insertStubCase(VM_Assembler asm, int[] sigIds, VM_Method[] targets, int[] bcIndices, int low,
                                     int high) {
    // TODO
  }
}
