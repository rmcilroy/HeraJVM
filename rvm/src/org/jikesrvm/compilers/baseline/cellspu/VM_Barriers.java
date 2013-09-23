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
package org.jikesrvm.compilers.baseline.cellspu;

import org.jikesrvm.VM;
import org.jikesrvm.compilers.common.assembler.cellspu.VM_Assembler;
import org.jikesrvm.cellspu.VM_BaselineConstants;
import org.jikesrvm.runtime.VM_Entrypoints;
import org.vmmagic.unboxed.Offset;

/**
 * Class called from baseline compiler to generate architecture specific
 * write barrier for generational garbage collectors.  For baseline
 * compiled methods, the write barrier calls methods of VM_WriteBarrier.
 */
class VM_Barriers implements VM_BaselineConstants {

  // on entry T0, T1, and T2 already contain the appropriate values
  static void compileArrayStoreBarrier(VM_Compiler comp) {
  	VM._assert(NOT_REACHED);
  }

  //  on entry java stack contains ...|target_ref|ref_to_store|
  // T1 already contains the offset of the field on entry
  static void compilePutfieldBarrier(VM_Compiler comp, int locationMetadata) {
  	VM._assert(NOT_REACHED);

  }

  //  on entry java stack contains ...|target_ref|ref_to_store|
  static void compilePutfieldBarrierImm(VM_Compiler comp, Offset fieldOffset, int locationMetadata) {

  	VM._assert(NOT_REACHED);
  }

  //  on entry java stack contains ...|ref_to_store|
  // T0 already contains the offset of the field on entry
  static void compilePutstaticBarrier(VM_Compiler comp, int locationMetadata) {

  	VM._assert(NOT_REACHED);
  }

  //  on entry java stack contains ...|ref_to_store|
  static void compilePutstaticBarrierImm(VM_Compiler comp, Offset fieldOffset, int locationMetadata) {

  	VM._assert(NOT_REACHED);
  }
}
