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

import org.vmmagic.unboxed.WordArray;

import org.jikesrvm.classloader.VM_Method;
import org.jikesrvm.compilers.baseline.VM_BaselineCompiledMethod;

public class SubordinateArchitecture {
  public static class VM_Assembler extends org.jikesrvm.compilers.common.assembler.cellspu.VM_Assembler {
    public VM_Assembler (int bytecodeSize) {
      super(bytecodeSize, false);
    }
    public VM_Assembler (int bytecodeSize, boolean shouldPrint, VM_Compiler compiler) {
      super(bytecodeSize, shouldPrint, compiler);
    }
    public VM_Assembler (int bytecodeSize, boolean shouldPrint) {
      super(bytecodeSize, shouldPrint);
    }
  }
  public interface VM_ArchConstants extends org.jikesrvm.cellspu.VM_ArchConstants {}
  public interface VM_BaselineConstants extends org.jikesrvm.cellspu.VM_BaselineConstants {}
  public static final class VM_BaselineExceptionDeliverer extends org.jikesrvm.compilers.baseline.cellspu.VM_BaselineExceptionDeliverer {}
  public static final class VM_BaselineGCMapIterator extends org.jikesrvm.compilers.baseline.cellspu.VM_BaselineGCMapIterator {
    public VM_BaselineGCMapIterator(WordArray registerLocations) {
      super(registerLocations);
    }
  }
  public static final class VM_CodeArray extends org.jikesrvm.cellspu.VM_CodeArray {
    public VM_CodeArray() { super(0);}
    public VM_CodeArray(int size) { super(size);}
    public static VM_CodeArray create (int size) { // only intended to be called from VM_CodeArray.factory
      if (VM.runningVM) VM._assert(false);  // should be hijacked
      return new VM_CodeArray(size);
    }
  }
  public static final class VM_Compiler extends org.jikesrvm.compilers.baseline.cellspu.VM_Compiler {
    public VM_Compiler(VM_BaselineCompiledMethod cm, int[] genLocLoc, int[] floatLocLoc) {
      super(cm, genLocLoc /**, floatLocLoc /* */ );
    }}
  public static final class VM_DynamicLinkerHelper extends org.jikesrvm.cellspu.VM_DynamicLinkerHelper {}
  public static final class VM_InterfaceMethodConflictResolver extends org.jikesrvm.cellspu.VM_InterfaceMethodConflictResolver {}
  public static final class VM_LazyCompilationTrampolineGenerator extends org.jikesrvm.cellspu.VM_LazyCompilationTrampolineGenerator {}
  public static final class VM_MachineCode extends org.jikesrvm.cellspu.VM_MachineCode {
  }
  public static final class VM_MachineReflection extends org.jikesrvm.cellspu.VM_MachineReflection {}
  public static final class VM_MultianewarrayHelper extends org.jikesrvm.cellspu.VM_MultianewarrayHelper {}
  public static final class VM_OutOfLineMachineCode extends org.jikesrvm.cellspu.VM_OutOfLineMachineCode {}
  public static final class VM_ProcessorLocalState extends org.jikesrvm.cellspu.VM_ProcessorLocalState {}
  public interface VM_RegisterConstants extends org.jikesrvm.cellspu.VM_RegisterConstants {}
  public static final class VM_Registers extends org.jikesrvm.cellspu.VM_Registers {}
  public interface VM_StackframeLayoutConstants extends org.jikesrvm.cellspu.VM_StackframeLayoutConstants {}
  public interface VM_TrapConstants extends org.jikesrvm.cellspu.VM_TrapConstants {}
  public static final class VM_SubArchBootRecord extends org.jikesrvm.cellspu.VM_SubArchBootRecord {}
  public static final class VM_RuntimeMethods extends org.jikesrvm.cellspu.VM_RuntimeMethods {}
}
