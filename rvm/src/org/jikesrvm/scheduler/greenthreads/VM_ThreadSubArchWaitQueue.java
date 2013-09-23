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
package org.jikesrvm.scheduler.greenthreads;

import org.jikesrvm.VM;
import static org.jikesrvm.runtime.VM_SysCall.sysCall;
import org.jikesrvm.scheduler.VM_ProcessorLock;
import org.vmmagic.pragma.Interruptible;
import org.vmmagic.pragma.Uninterruptible;
import org.vmmagic.pragma.UninterruptibleNoWarn;

/**
 * A list of threads which are blocked on the main processor while they
 * are migrated to the subarch processor.
 */
@Uninterruptible
public final class VM_ThreadSubArchWaitQueue extends VM_ThreadEventWaitQueue
    implements VM_ThreadEventConstants, VM_ThreadSubArchConstants {

  /**
   * Class to safely downcast from <code>VM_ThreadEventWaitData</code>
   * to <code>VM_ThreadSubArchWaitData</code>.
   * We use this because an actual Java cast could result in
   * a thread switch, which is obviously bad in uninterruptible
   * code.
   */
  @Uninterruptible
  private static class WaitDataDowncaster extends VM_ThreadEventWaitDataVisitor {

    private VM_ThreadSubArchWaitData waitData;

    @Override
    void visitThreadSubArchWaitData(VM_ThreadSubArchWaitData waitData) {
      this.waitData = waitData;
    }

    @Override
    void visitThreadProcessWaitData(VM_ThreadProcessWaitData waitData) {
      if (VM.VerifyAssertions) VM._assert(false);
    }

    @Override
    void visitThreadIOWaitData(VM_ThreadIOWaitData waitData) {
      if (VM.VerifyAssertions) VM._assert(false);
    }
  }

  /**
   * Private downcaster object for this queue.
   * Avoids having to create them repeatedly.
   */
  private final WaitDataDowncaster myDowncaster = new WaitDataDowncaster();

  private static final int SUBARCH_SETSIZE = 32;

  /**
   * Array containing read, write, and exception file descriptor sets.
   * Used by subArchSelect().
   */
  private int[] allSubArchThrds = new int[SUBARCH_SETSIZE];
  
  private int currOffset;

  /** Guard for updating "selectInProgress" flag. */
  public static final VM_ProcessorLock statusCheckInProgressMutex = new VM_ProcessorLock();


  //-----------//
  // Interface //
  //-----------//

  /**
   * Poll subarch status to see which ones have become ready.
   * Called from superclass's {@link VM_ThreadEventWaitQueue#isReady()} method.
   * @return true if poll was successful, false if not
   */
  @Override
  public boolean pollForEvents() {
  	currOffset = 0;
  	
    // Interrogate all threads in the queue to determine
    // which subArch threads they are waiting for
    VM_GreenThread thread = head;
    while (thread != null) {
    	
    	// Safe downcast from VM_ThreadEventWaitData to VM_ThreadSubArchWaitData.
    	thread.waitData.accept(myDowncaster);
    	VM_ThreadSubArchWaitData waitData = myDowncaster.waitData;
    	if (VM.VerifyAssertions) VM._assert(waitData == thread.waitData);
        
      // copy across subarch wait status to set
    	waitData.subArchThrdsOffset = currOffset;
    	allSubArchThrds[currOffset++] = waitData.subArchThreadStatus;

    	if (VM.VerifyAssertions) VM._assert(currOffset < SUBARCH_SETSIZE);
    	
      thread = (VM_GreenThread)thread.getNext();
    }
    
    // Do the select()
    statusCheckInProgressMutex.lock("select in progress mutex");
    int ret = sysCall.subArchCheckStatus(allSubArchThrds, currOffset + 1);
    statusCheckInProgressMutex.unlock();

    // Did the subArchCheckStatus() succeed?
    return ret != -1;
  }

  /**
   * Determine whether or not given thread has become ready
   * to run, i.e., because the subarch migrated part has completed
   * If the thread is ready, update its wait flags appropriately.
   */
  @Override
  public boolean isReady(VM_GreenThread thread) {
    // Safe downcast from VM_ThreadEventWaitData to VM_ThreadSubArchWaitData.
    thread.waitData.accept(myDowncaster);
    VM_ThreadSubArchWaitData waitData = myDowncaster.waitData;
    if (VM.VerifyAssertions) VM._assert(waitData == thread.waitData);

    boolean ready = (allSubArchThrds[waitData.subArchThrdsOffset] & SUBARCH_READY_BIT) != 0;
    
    if (ready) {
    	waitData.subArchThreadStatus |= SUBARCH_READY_BIT;
      waitData.setFinished();
    }
    
    return ready;
  }

  @Interruptible
	@Override
	void dumpWaitDescription(VM_GreenThread thread) {
		thread.waitData.accept(myDowncaster);
    VM_ThreadSubArchWaitData waitData = myDowncaster.waitData;

    VM.sysWrite("SubArchWaitID: ");
    VM.sysWrite(Integer.toHexString(waitData.subArchThreadStatus));
	}

  @Interruptible
	@Override
	String getWaitDescription(VM_GreenThread thread) {
		thread.waitData.accept(myDowncaster);
    VM_ThreadSubArchWaitData waitData = myDowncaster.waitData;

    StringBuffer buffer = new StringBuffer();
    buffer.append("SubArchWaitID:");
    buffer.append(Integer.toHexString(waitData.subArchThreadStatus));
    return buffer.toString();
	}
}
