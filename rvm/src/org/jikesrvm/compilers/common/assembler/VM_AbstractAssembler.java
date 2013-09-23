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
package org.jikesrvm.compilers.common.assembler;

import org.jikesrvm.VM_MachineCode;

/**
 *  This class has been created to work around a bug whereby the system seems to have gotten
 * confused by the relationship between:
 *
 * VM_ForwardReference -> VM_ForwardReference.ShortBranch -> VM_Assembler.ShortBranch, and VM_Assembler
 *
 * This problem does not exist under IA32 since there is no need for VM_Assembler.ShortBranch
 */
public abstract class VM_AbstractAssembler {
  public abstract void patchShortBranch(int sourceMachinecodeIndex);

  public abstract void patchUnconditionalBranch(int sourceMachinecodeIndex);

  public abstract void patchConditionalBranch(int sourceMachinecodeIndex);

  public abstract void patchSwitchCase(int sourceMachinecodeIndex);

  public abstract void patchLoadReturnAddress(int sourceMachinecodeIndex);
  
  public abstract void resolveForwardReferences(int label);

  public abstract void noteBytecode (int bytecodeNumber, String bc);

  public abstract void noteBytecode (int i, String bcode, int x);

  public abstract void noteBytecode (int i, String bcode, long x);

  public abstract void noteBytecode (int i, String bcode, Object o);

  public abstract void noteBytecode (int i, String bcode, int x, int y);

  public abstract void noteBranchBytecode (int i, String bcode, int off,
               int bt);

  public abstract void noteTableswitchBytecode (int i, int l, int h, int d);

  public abstract void noteLookupswitchBytecode (int i, int n, int d);
  
  public abstract int getMachineCodeIndex ();

  public abstract VM_MachineCode finalizeMachineCode(int[] bytecodeMap);
}
