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
package org.jikesrvm.jni.ia32;

import org.jikesrvm.compilers.common.VM_CompiledMethod;
import org.jikesrvm.ia32.VM_BaselineConstants;
import org.jikesrvm.jni.VM_JNIEnvironment;
import org.jikesrvm.memorymanagers.mminterface.VM_GCMapIterator;
import org.jikesrvm.runtime.VM_Magic;
import org.jikesrvm.scheduler.VM_Thread;
import org.vmmagic.pragma.Uninterruptible;
import org.vmmagic.unboxed.Address;
import org.vmmagic.unboxed.AddressArray;
import org.vmmagic.unboxed.LocalAddress;
import org.vmmagic.unboxed.Offset;
import org.vmmagic.unboxed.WordArray;

/**
 * Iterator for stack frames inserted at the transition from Java to
 * JNI Native C.  It will report JREFs associated with the executing
 * C frames which are in the "JREFs stack" attached to the executing
 * Threads JNIEnvironment.  It will update register location addresses
 * for the non-volatile registers to point to the registers saved
 * in the transition frame.
 *
 * @see VM_JNICompiler
 */
@Uninterruptible
public abstract class VM_JNIGCMapIterator extends VM_GCMapIterator implements VM_BaselineConstants {

  // Java to Native C transition frame...(see VM_JNICompiler)
  //
  //  0         + saved FP   + <---- FP for Jave to Native C glue frame
  // -4         | methodID   |
  // -8         | saved EDI  |  non-volatile GPR (JTOC for baseline callers or ? for opt callers)
  // -C         | saved EBX  |  non-volatile GPR
  // -10        | saved EBP  |  non-volatile GPR
  // -14        | returnAddr |  (for return from OutOfLineMachineCode)
  // -18        | saved PR   |
  // -1C        | arg n-1    |  reordered arguments to native method
  // -20        |  ...       |  ...
  // -24        | arg 1      |  ...
  // -28        | arg 0      |  ...
  // -2C        | class/obj  |  required 2nd argument to all native methods
  // -30        | jniEnv     |  required 1st argument to all native methods
  // -34        | returnAddr |  return address pushed by call to native method
  //            + saved FP   +  <---- FP for called native method

  // additional instance fields added by this subclass of VM_GCMapIterator
  AddressArray jniRefs;
  int jniNextRef;
  int jniFramePtr;
  LocalAddress jniSavedReturnAddr;           // -> return addr in generated transition prolog

  public VM_JNIGCMapIterator(WordArray registerLocations) {
    this.registerLocations = registerLocations;
  }

  // Override newStackWalk() in parent class VM_GCMapIterator to
  // initialize iterator for scan of JNI JREFs stack of refs
  // Taken:    thread
  // Returned: nothing
  //
  public void newStackWalk(VM_Thread thread) {
    super.newStackWalk(thread);   // sets this.thread, inits registerLocations[]
    VM_JNIEnvironment env = this.thread.getJNIEnv();
    // the "primordial" thread, created by JDK in the bootimage, does not have
    // a JniEnv object, all threads created by the VM will.
    if (env != null) {
      this.jniRefs = env.refsArray();
      this.jniNextRef = env.refsTop();
      this.jniFramePtr = env.savedRefsFP();
    }
  }

  public void setupIterator(VM_CompiledMethod compiledMethod, Offset instructionOffset, LocalAddress framePtr) {
    this.framePtr = framePtr;

    // return address into generated prolog must be relocated if the code object
    // for that prolog/epilog is moved by GC
    jniSavedReturnAddr = framePtr.plus(VM_JNICompiler.JNI_RETURN_ADDRESS_OFFSET);
  }

  // return (address of) next ref in the current "frame" on the
  // threads JNIEnvironment stack of refs
  // When at the end of the current frame, update register locations to point
  // to the non-volatile registers saved in the JNI transition frame.
  //
  public LocalAddress getNextReferenceAddress() {
    // first report jni refs in the current frame in the jniRef side stack
    // until all in the frame are reported
    //
    if (jniNextRef > jniFramePtr) {
      LocalAddress ref_address = VM_Magic.objectAsLocalAddress(jniRefs).plus(jniNextRef);
      jniNextRef -= BYTES_IN_ADDRESS;
      return ref_address;
    }

    // no more refs to report, before returning 0, setup for processing
    // the next jni frame, if any

    // jniNextRef -> savedFramePtr for another "frame" of refs for another
    // sequence of Native C frames lower in the stack, or to 0 if this is the
    // last jni frame in the JNIRefs stack.  If more frames, initialize for a
    // later scan of those refs.
    //
    if (jniFramePtr > 0) {
      jniFramePtr = jniRefs.get(jniFramePtr >> LOG_BYTES_IN_ADDRESS).toInt();
      jniNextRef = jniNextRef - BYTES_IN_ADDRESS;
    }

    // set register locations for non-volatiles to point to registers saved in
    // the JNI transition frame at a fixed negative offset from the callers FP.
    // the save non-volatiles are EBX EBP and EDI.
    //
    registerLocations.set(JTOC, framePtr.plus(VM_JNICompiler.EDI_SAVE_OFFSET).toWord());
    registerLocations.set(EBX, framePtr.plus(VM_JNICompiler.EBX_SAVE_OFFSET).toWord());
    registerLocations.set(EBP, framePtr.plus(VM_JNICompiler.EBP_SAVE_OFFSET).toWord());

    return LocalAddress.zero();  // no more refs to report
  }

  public LocalAddress getNextReturnAddressAddress() {
    if (!jniSavedReturnAddr.isZero()) {
      LocalAddress ref_address = jniSavedReturnAddr;
      jniSavedReturnAddr = LocalAddress.zero();
      return ref_address;
    }
    return LocalAddress.zero();
  }

  public void reset() { }

  public void cleanupPointers() { }

  public int getType() {
    return VM_CompiledMethod.JNI;
  }
}
