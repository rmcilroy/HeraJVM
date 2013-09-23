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

import org.jikesrvm.runtime.VM_ArchEntrypoints;
import org.jikesrvm.runtime.VM_Magic;
import org.vmmagic.pragma.Uninterruptible;
import org.vmmagic.pragma.UninterruptibleNoWarn;
import org.vmmagic.unboxed.Address;
import org.vmmagic.unboxed.LocalAddress;
import org.vmmagic.unboxed.Offset;
import org.vmmagic.unboxed.WordArray;

/**
 * The machine state comprising a thread's execution context.
 */
@Uninterruptible
public abstract class VM_Registers extends org.jikesrvm.VM_Registers implements VM_ArchConstants {

  static LocalAddress invalidIP = LocalAddress.max();

  public VM_Registers() {
    gprs = WordArray.create(NUM_GPRS);
    ip = invalidIP;
  }

  // Return framepointer for the deepest stackframe
  //
  @Uninterruptible
  public final LocalAddress getInnermostFramePointer() {
  	// TODO 
    //return gprs.get(FRAME_POINTER).toAddress();
  	return null;
  }

  // Return next instruction address for the deepest stackframe
  //
  @Uninterruptible
  public final LocalAddress getInnermostInstructionAddress() {
  	// TODO 
    //if (ip.NE(invalidIP)) return ip; // ip set by hardware exception handler or VM_Magic.threadSwitch
    //return VM_Magic.getNextInstructionAddress(getInnermostFramePointer()); // ip set to -1 because we're unwinding
  	return null;
  }

  // update the machine state to unwind the deepest stackframe.
  //
  @Uninterruptible
  public final void unwindStackFrame() {
  	//  TODO 
    //ip = invalidIP; // if there was a valid value in ip, it ain't valid anymore
    //gprs.set(FRAME_POINTER, VM_Magic.getCallerFramePointer(getInnermostFramePointer()).toWord());
  }

  // set ip & fp. used to control the stack frame at which a scan of
  // the stack during GC will start, for ex., the top java frame for
  // a thread that is blocked in native code during GC.
  //
  @Uninterruptible
  public final void setInnermost(LocalAddress newip, LocalAddress newfp) {
  	// TODO 
    //ip = newip;
    //gprs.set(FRAME_POINTER, newfp.toWord());
  }

  // set ip and fp values to those of the caller. used just prior to entering
  // sigwait to set fp & ip so that GC will scan the threads stack
  // starting at the frame of the method that called sigwait.
  //
  @Uninterruptible
  public final void setInnermost() {
  	// TODO 
    //Address fp = VM_Magic.getFramePointer();
    //ip = VM_Magic.getReturnAddress(fp);
    //gprs.set(FRAME_POINTER, VM_Magic.getCallerFramePointer(fp).toWord());
  }
  
  @Uninterruptible
  public final LocalAddress getIPLocation() {
  	// TODO 
    //Offset ipOffset = VM_ArchEntrypoints.registersIPField.getOffset();
    //return VM_Magic.objectAsAddress(this).plus(ipOffset);
  	return null;
  }

}
