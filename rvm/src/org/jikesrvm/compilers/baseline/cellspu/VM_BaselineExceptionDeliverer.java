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

import org.jikesrvm.SubordinateArchitecture;
import org.jikesrvm.VM;
import org.jikesrvm.classloader.VM_NormalMethod;
import org.jikesrvm.compilers.baseline.VM_BaselineCompiledMethod;
import org.jikesrvm.compilers.common.VM_CompiledMethod;
import org.jikesrvm.objectmodel.VM_ObjectModel;
import org.jikesrvm.cellspu.VM_BaselineConstants;
import org.jikesrvm.runtime.VM_Magic;
import org.jikesrvm.scheduler.VM_Scheduler;
import org.vmmagic.unboxed.LocalAddress;
import org.vmmagic.unboxed.Offset;

/**
 *  Handle exception delivery and stack unwinding for methods compiled
 * by baseline compiler.
 */ 
// TODO - extends VM_ExceptionDeliverer 
public abstract class VM_BaselineExceptionDeliverer implements VM_BaselineConstants {

  /**
   * Pass control to a catch block.
   */
  public void deliverException(VM_CompiledMethod compiledMethod, LocalAddress catchBlockInstructionAddress,
                               Throwable exceptionObject, SubordinateArchitecture.VM_Registers registers) {

  	VM._assert(NOT_REACHED);
  	
  	LocalAddress fp = registers.getInnermostFramePointer();
    VM_NormalMethod method = (VM_NormalMethod) compiledMethod.getMethod();

    // reset sp to "empty expression stack" state
    //
    LocalAddress sp = fp.plus(VM_Compiler.getEmptyStackOffset(method));

    // push exception object as argument to catch block
    //
    sp = sp.minus(BYTES_IN_ADDRESS);
    sp.store(VM_Magic.objectAsLocalAddress(exceptionObject));

    // set address at which to resume executing frame
    //
    registers.ip = catchBlockInstructionAddress;

    // branch to catch block
    //
    VM.enableGC(); // disabled right before VM_Runtime.deliverException was called
    if (VM.VerifyAssertions) VM._assert(registers.inuse);

    registers.inuse = false;
    // Todo - fix VM_Magic.restoreHardwareExceptionState(registers);
    if (VM.VerifyAssertions) VM._assert(NOT_REACHED);
  }

  /**
   * Unwind a stackframe.
   */
  public void unwindStackFrame(VM_CompiledMethod compiledMethod, SubordinateArchitecture.VM_Registers registers) {

  	VM._assert(NOT_REACHED);
  	
  	VM_NormalMethod method = (VM_NormalMethod) compiledMethod.getMethod();
    VM_BaselineCompiledMethod bcm = (VM_BaselineCompiledMethod) compiledMethod;
    if (method.isSynchronized()) {
      LocalAddress ip = registers.getInnermostInstructionAddress();
      Offset instr = compiledMethod.getInstructionOffset(ip);
      Offset lockOffset = ((VM_BaselineCompiledMethod) compiledMethod).getLockAcquisitionOffset();
      if (instr.sGT(lockOffset)) { // we actually have the lock, so must unlock it.
        Object lock;
        if (method.isStatic()) {
          lock = method.getDeclaringClass().getClassForType();
        } else {
          LocalAddress fp = registers.getInnermostFramePointer();
          int location = bcm.getGeneralLocalLocation(0);
          LocalAddress addr;
          if (VM_Compiler.isRegister(location)) {
            lock = VM_Magic.addressAsObject(registers.gprs.get(location).toAddress());
          } else {
            addr =
                fp.plus(VM_Compiler.locationToOffset(location) -
                        BYTES_IN_ADDRESS); //location offsets are positioned on top of their stackslot
            lock = VM_Magic.addressAsObject(addr.loadAddress());
          }
        }
        if (VM_ObjectModel.holdsLock(lock, VM_Scheduler.getCurrentThread())) {
          VM_ObjectModel.genericUnlock(lock);
        }
      }
    }
    // restore non-volatile registers
    LocalAddress fp = registers.getInnermostFramePointer();
    Offset frameOffset = Offset.zero(); // FIXME fromIntSignExtend(VM_Compiler.getFrameSize(bcm));

    //for (int i = bcm.getLastFloatStackRegister(); i >= FIRST_FLOAT_LOCAL_REGISTER; --i) {
    //  frameOffset = frameOffset.minus(BYTES_IN_DOUBLE);
    //  long temp = VM_Magic.getLongAtOffset(VM_Magic.addressAsObject(fp), frameOffset);
    //  registers.fprs[i] = VM_Magic.longBitsAsDouble(temp);
    //}

    for (int i = bcm.getLastFixedStackRegister(); i >= FIRST_FIXED_LOCAL_REGISTER; --i) {
      frameOffset = frameOffset.minus(BYTES_IN_ADDRESS);
      registers.gprs.set(i, fp.loadWord(frameOffset));
    }

    registers.unwindStackFrame();
  }
}
