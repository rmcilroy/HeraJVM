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
package org.jikesrvm.compilers.opt.ir.ppc;

import org.jikesrvm.classloader.VM_Atom;
import org.jikesrvm.classloader.VM_MethodReference;
import org.jikesrvm.classloader.VM_TypeReference;
import org.jikesrvm.compilers.opt.OPT_MagicNotImplementedException;
import org.jikesrvm.compilers.opt.ir.Binary;
import org.jikesrvm.compilers.opt.ir.CacheOp;
import org.jikesrvm.compilers.opt.ir.Empty;
import org.jikesrvm.compilers.opt.ir.Load;
import org.jikesrvm.compilers.opt.ir.OPT_AddressConstantOperand;
import org.jikesrvm.compilers.opt.ir.OPT_BC2IR;
import org.jikesrvm.compilers.opt.ir.OPT_GenerationContext;
import org.jikesrvm.compilers.opt.ir.OPT_IntConstantOperand;
import org.jikesrvm.compilers.opt.ir.OPT_Operand;
import org.jikesrvm.compilers.opt.ir.OPT_Operators;
import org.jikesrvm.compilers.opt.ir.OPT_RegisterOperand;
import org.jikesrvm.compilers.opt.ir.Store;
import org.jikesrvm.ppc.VM_StackframeLayoutConstants;
import org.jikesrvm.runtime.VM_MagicNames;
import org.vmmagic.unboxed.Offset;

/**
 * This class implements the machine-specific magics for the opt compiler.
 *
 * @see org.jikesrvm.compilers.opt.ir.OPT_GenerateMagic for the machine-independent magics.
 */
public abstract class OPT_GenerateMachineSpecificMagic implements OPT_Operators, VM_StackframeLayoutConstants {

  /**
   * "Semantic inlining" of methods of the VM_Magic class
   * Based on the methodName, generate a sequence of opt instructions
   * that implement the magic, updating the stack as necessary
   *
   * @param bc2ir the bc2ir object that is generating the
   *              ir containing this magic
   * @param gc == bc2ir.gc
   * @param meth the VM_Method that is the magic method
   */
  public static boolean generateMagic(OPT_BC2IR bc2ir, OPT_GenerationContext gc, VM_MethodReference meth)
      throws OPT_MagicNotImplementedException {
    VM_Atom methodName = meth.getName();
    if (methodName == VM_MagicNames.getFramePointer) {
      bc2ir.push(gc.temps.makeFPOp());
      gc.allocFrame = true;
    } else if (methodName == VM_MagicNames.getTocPointer) {
      bc2ir.push(gc.temps.makeJTOCOp(null, null));
    } else if (methodName == VM_MagicNames.getJTOC) {
      bc2ir.push(gc.temps.makeTocOp());
    } else if (methodName == VM_MagicNames.getCallerFramePointer) {
      OPT_Operand fp = bc2ir.popAddress();
      OPT_RegisterOperand val = gc.temps.makeTemp(VM_TypeReference.Address);
      bc2ir.appendInstruction(Load.create(REF_LOAD,
                                          val,
                                          fp,
                                          new OPT_AddressConstantOperand(Offset.fromIntSignExtend(
                                              STACKFRAME_FRAME_POINTER_OFFSET)),
                                          null));
      bc2ir.push(val.copyD2U());
    } else if (methodName == VM_MagicNames.setCallerFramePointer) {
      OPT_Operand val = bc2ir.popAddress();
      OPT_Operand fp = bc2ir.popAddress();
      bc2ir.appendInstruction(Store.create(REF_STORE,
                                           val,
                                           fp,
                                           new OPT_AddressConstantOperand(Offset.fromIntSignExtend(
                                               STACKFRAME_FRAME_POINTER_OFFSET)),
                                           null));
    } else if (methodName == VM_MagicNames.getCompiledMethodID) {
      OPT_Operand fp = bc2ir.popAddress();
      OPT_RegisterOperand val = gc.temps.makeTempInt();
      bc2ir.appendInstruction(Load.create(INT_LOAD,
                                          val,
                                          fp,
                                          new OPT_AddressConstantOperand(Offset.fromIntSignExtend(
                                              STACKFRAME_METHOD_ID_OFFSET)),
                                          null));
      bc2ir.push(val.copyD2U());
    } else if (methodName == VM_MagicNames.setCompiledMethodID) {
      OPT_Operand val = bc2ir.popInt();
      OPT_Operand fp = bc2ir.popAddress();
      bc2ir.appendInstruction(Store.create(INT_STORE,
                                           val,
                                           fp,
                                           new OPT_AddressConstantOperand(Offset.fromIntSignExtend(
                                               STACKFRAME_METHOD_ID_OFFSET)),
                                           null));
    } else if (methodName == VM_MagicNames.getNextInstructionAddress) {
      OPT_Operand fp = bc2ir.popAddress();
      OPT_RegisterOperand val = gc.temps.makeTemp(VM_TypeReference.Address);
      bc2ir.appendInstruction(Load.create(REF_LOAD,
                                          val,
                                          fp,
                                          new OPT_AddressConstantOperand(Offset.fromIntSignExtend(
                                              STACKFRAME_NEXT_INSTRUCTION_OFFSET)),
                                          null));
      bc2ir.push(val.copyD2U());
    } else if (methodName == VM_MagicNames.getReturnAddressLocation) {
      OPT_Operand fp = bc2ir.popAddress();
      OPT_RegisterOperand callerFP = gc.temps.makeTemp(VM_TypeReference.Address);
      OPT_RegisterOperand val = gc.temps.makeTemp(VM_TypeReference.Address);
      bc2ir.appendInstruction(Load.create(REF_LOAD,
                                          callerFP,
                                          fp,
                                          new OPT_AddressConstantOperand(Offset.fromIntSignExtend(
                                              STACKFRAME_FRAME_POINTER_OFFSET)),
                                          null));
      bc2ir.appendInstruction(Binary.create(REF_ADD,
                                            val,
                                            callerFP.copyRO(),
                                            new OPT_IntConstantOperand(STACKFRAME_NEXT_INSTRUCTION_OFFSET)));
      bc2ir.push(val.copyD2U());
    } else if (methodName == VM_MagicNames.isync) {
      if (!gc.options.NO_CACHE_FLUSH) {
        bc2ir.appendInstruction(Empty.create(READ_CEILING));
      }
    } else if (methodName == VM_MagicNames.sync) {
      if (!gc.options.NO_CACHE_FLUSH) {
        bc2ir.appendInstruction(Empty.create(WRITE_FLOOR));
      }
    } else if (methodName == VM_MagicNames.pause) {
      // IA-specific
    } else if (methodName == VM_MagicNames.dcbst) {
      bc2ir.appendInstruction(CacheOp.create(DCBST, bc2ir.popInt()));
    } else if (methodName == VM_MagicNames.dcbt) {
      bc2ir.appendInstruction(CacheOp.create(DCBT, bc2ir.popInt()));
    } else if (methodName == VM_MagicNames.dcbtst) {
      bc2ir.appendInstruction(CacheOp.create(DCBTST, bc2ir.popInt()));
    } else if (methodName == VM_MagicNames.dcbz) {
      bc2ir.appendInstruction(CacheOp.create(DCBZ, bc2ir.popInt()));
    } else if (methodName == VM_MagicNames.dcbzl) {
      bc2ir.appendInstruction(CacheOp.create(DCBZL, bc2ir.popInt()));
    } else if (methodName == VM_MagicNames.icbi) {
      bc2ir.appendInstruction(CacheOp.create(ICBI, bc2ir.popInt()));
    } else {
      // Distinguish between magics that we know we don't implement
      // (and never plan to implement) and those (usually new ones)
      // that we want to be warned that we don't implement.
      String msg = "Magic method not implemented: " + meth;
      if (methodName == VM_MagicNames.returnToNewStack) {
        throw OPT_MagicNotImplementedException.EXPECTED(msg);
      } else {
        return false;
        // throw OPT_MagicNotImplementedException.UNEXPECTED(msg);
      }
    }
    return true;
  }
}



