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

import static org.jikesrvm.runtime.VM_SysCall.sysCall;

import org.jikesrvm.SubordinateArchitecture.VM_ProcessorLocalState;
import org.jikesrvm.annotations.NoSubArchCompile;
import org.jikesrvm.VM;
import org.jikesrvm.VM_Constants;
import org.jikesrvm.objectmodel.VM_ThinLockConstants;
import org.jikesrvm.runtime.VM_Magic;
import org.jikesrvm.scheduler.VM_Processor;
import org.jikesrvm.scheduler.VM_ProcessorLock;
import org.jikesrvm.scheduler.VM_Scheduler;
import org.jikesrvm.scheduler.VM_Thread;
import org.vmmagic.pragma.Inline;
import org.vmmagic.pragma.LogicallyUninterruptible;
import org.vmmagic.pragma.Uninterruptible;
import org.vmmagic.unboxed.Offset;

/**
 * Multiplex execution of large number of VM_Threads on small
 * number of o/s kernel threads.
 */
@Uninterruptible
public final class VM_GreenSubArchProcessor extends VM_Processor {
  
	/**
   * thread previously running on this processor
   */
  public VM_GreenThread previousThread;

  /**
   * Should this processor dispatch a new VM_Thread when
   * "threadSwitch" is called?
   * Also used to decide if it's safe to call yield() when
   * contending for a lock.
   * A value of:
   *    1 means "yes" (switching enabled)
   * <= 0 means "no"  (switching disabled)
   */
  private int threadSwitchingEnabledCount;

  /**
   * Was "threadSwitch" called while this processor had
   * thread switching disabled?
   */
  int threadSwitchPending;
  
  /**
   * non-null --> a processor that has no work to do
   */
  static VM_GreenSubArchProcessor idleProcessor;

  /**
   * The reason given for disabling thread switching
   */
  private final String[] threadSwitchDisabledReason = VM.VerifyAssertions ? new String[10] : null;
  /**
   * threads to be added to ready queue
   */
  public VM_GlobalGreenThreadQueue transferQueue;
  /** guard for transferQueue */
  public final VM_ProcessorLock transferMutex;

  /** guard for collectorThread */
  public final VM_ProcessorLock collectorThreadMutex;

  /** the collector thread to run */
  public VM_GreenThread collectorThread;
  
  /**
   * threads waiting for a timeslice in which to run
   */
  VM_GreenThreadQueue readyQueue;

  /**
   * thread to run when nothing else to do
   */
  VM_GreenThreadQueue idleQueue;

  /**
   * Create data object to be associated with an subarch processor
   * @param id id that will be returned by getCurrentProcessorId() for
   * this processor.
   */
  @NoSubArchCompile
  public VM_GreenSubArchProcessor(int id) {
    super(id);
    this.transferMutex = new VM_ProcessorLock();
    this.collectorThreadMutex = new VM_ProcessorLock();
    this.transferQueue = new VM_GlobalGreenThreadQueue(this.transferMutex);
    this.readyQueue = new VM_GreenThreadQueue();
    this.idleQueue = new VM_GreenThreadQueue();
    
    // TODO - Remove hack
    this.threadId = id << VM_ThinLockConstants.TL_THREAD_ID_SHIFT;

    sysCall.sysVirtualSubArchProcessorBind(VM_Magic.objectAsAddress(this), id - 1);
    
  	// TODO - Remove when initializeProcessor is being called by subarch
    isInitialized = true;
  }

  /**
   * Code executed to initialize a subarch processor and
   * prepare it to execute Java threads.
   */
  public void initializeProcessor() {
  	// subarch processor is now ready to run java threads
    isInitialized = true;
    
    // enable multiprocessing
    //
    enableThreadSwitching();
  }

  /**
   * Is it ok to switch to a new VM_Thread in this processor?
   */
  @Inline
  @Override
  public boolean threadSwitchingEnabled() {
    return threadSwitchingEnabledCount == 1;
  }

  /**
   * Enable thread switching in this processor.
   */
  @Override
  public void enableThreadSwitching() {
    ++threadSwitchingEnabledCount;
    if (VM.VerifyAssertions) {
      VM._assert(threadSwitchingEnabledCount <= 1);
//      if (MM_Interface.gcInProgress()) {
//        VM._assert(threadSwitchingEnabledCount < 1 || getCurrentProcessorId() == 0);
//      }
    }
    if (threadSwitchingEnabled() && threadSwitchPending != 0) {
      takeYieldpoint = threadSwitchPending;
      threadSwitchPending = 0;
    }
  }

  /**
   * Disable thread switching in this processor.
   * @param reason for disabling thread switching
   */
  @Inline
  @Override
  public void disableThreadSwitching(String reason) {
    --threadSwitchingEnabledCount;
    if (VM.VerifyAssertions && (-threadSwitchingEnabledCount < threadSwitchDisabledReason.length)) {
    	VM_Magic.setObjectAtOffset(threadSwitchDisabledReason,
          Offset.fromIntZeroExtend(-threadSwitchingEnabledCount << VM_Constants.BYTES_IN_ADDRESS),
          reason);
     //  threadSwitchDisabledReason[-threadSwitchingEnabledCount] = reason;
    }
  }

  /**
   * Request the thread executing on the processor to take the next executed yieldpoint
   * and issue memory synchronization instructions
   */
  @Override
  public void requestPostCodePatchSync() {
     if (VM.VerifyAssertions) VM._assert(VM.NOT_REACHED);
  }

  /**
   * Get processor that's being used to run the current java thread.
   */
  @Inline
  public static VM_GreenSubArchProcessor getCurrentProcessor() {
    return (VM_GreenSubArchProcessor)VM_ProcessorLocalState.getCurrentProcessor();
  }

  /**
   * Get id of processor that's being used to run the current java thread.
   */
  @Inline
  public static int getCurrentProcessorId() {
    return getCurrentProcessor().id;
  }

  /**
   * Become next "ready" thread.
   * Note: This method is ONLY intended for use by VM_Thread.
   * @param timerTick   timer interrupted if true
   */
  @Override
  public void dispatch(boolean timerTick) {
    // no processor locks should be held across a thread switch
    if (VM.VerifyAssertions) checkLockCount(0);

    VM_GreenThread newThread = getRunnableThread();
    while (newThread.suspendIfPending()) {
      newThread = getRunnableThread();
    }

    previousThread = (VM_GreenThread)activeThread;
    activeThread = (VM_GreenThread)newThread;

    // TODO - Implement some load balancing if runqueue is not empty and other processors are idle

    // TODO - Deal with time intervals

    threadId = newThread.getLockingId();
    activeThreadStackLimit = newThread.stackLimit; // Delay this to last possible moment so we can sysWrite
    
    // TODO - VM_Magic.threadSwitch(previousThread, newThread.contextRegisters);
  }

  /**
   * Find a thread that can be run by this processor and remove it
   * from its queue.
   */
  @Inline
  private VM_GreenThread getRunnableThread() {
    // Is there a GC thread waiting to be scheduled?
    if (collectorThread != null) {
      // Schedule GC threads first. This avoids a deadlock when GC is trigerred
      // during scheduling (usually due to a write barrier)
      collectorThreadMutex.lock("getting runnable gc thread");
      if (VM.VerifyAssertions) VM._assert(collectorThread != null);
      VM_GreenThread ct = collectorThread;
      if (VM.TraceThreadScheduling > 1) {
        VM_Scheduler.trace("VM_Processor", "getRunnableThread: collector thread", ct.getIndex());
      }
      collectorThread = null;
      collectorThreadMutex.unlock();
      return ct;
    }

    for (int i = transferQueue.length(); 0 < i; i--) {
      transferMutex.lock("transfer queue mutex for dequeue");
      VM_GreenThread t = transferQueue.dequeue();
      transferMutex.unlock();
      if (VM.VerifyAssertions) VM._assert(!t.isGCThread());
      if (t.beingDispatched && t != VM_Scheduler.getCurrentThread()) {
        // thread's stack in use by some OTHER dispatcher
        if (VM.TraceThreadScheduling > 1) {
          VM_Scheduler.trace("VM_Processor", "getRunnableThread: stack in use", t.getIndex());
        }
        transferMutex.lock("transfer queue mutex for an enqueue due to dispatch");
        transferQueue.enqueue(t);
        transferMutex.unlock();
      } else {
        if (VM.TraceThreadScheduling > 1) {
          VM_Scheduler.trace("VM_Processor", "getRunnableThread: transfer to readyQueue", t.getIndex());
        }
        readyQueue.enqueue(t);
      }
    }

    if (!readyQueue.isEmpty()) {
      VM_GreenThread t = readyQueue.dequeue();
      if (VM.TraceThreadScheduling > 1) {
        VM_Scheduler.trace("VM_Processor", "getRunnableThread: readyQueue", t.getIndex());
      }
      if (VM.VerifyAssertions) {
        // local queue: no other dispatcher should be running on thread's stack
        VM._assert(!t.beingDispatched || t == VM_Scheduler.getCurrentThread());
      }
      return t;
    }
    
    if (!idleQueue.isEmpty()) {
      VM_GreenThread t = idleQueue.dequeue();
      if (VM.TraceThreadScheduling > 1) {
        VM_Scheduler.trace("VM_Processor", "getRunnableThread: idleQueue", t.getIndex());
      }
      if (VM.VerifyAssertions) {
        // local queue: no other dispatcher should be running on thread's stack
        VM._assert(!t.beingDispatched || t == VM_Scheduler.getCurrentThread());
      }
      return t;
    }
    // should only get here if the idle thread contended on a lock (due to debug)
    if (VM.VerifyAssertions) VM._assert(VM_Scheduler.getCurrentThread() instanceof VM_IdleThread);
    return VM_GreenScheduler.getCurrentThread();
  }

  //-----------------//
  //  Load Balancing //
  //-----------------//

  /**
   * Add a thread to this processor's transfer queue.
   */
  public void transferThread(VM_Thread thread) {
  	if (VM.VerifyAssertions) VM._assert(thread instanceof VM_GreenThread);
  	VM_GreenThread t = (VM_GreenThread) thread;
    if (t.isGCThread()) {
      collectorThreadMutex.lock("gc thread transfer");
      collectorThread = t;
      /* Implied by transferring a gc thread */
      requestYieldToGC();
      collectorThreadMutex.unlock();
    } else if (this != getCurrentProcessor() ||
        (t.beingDispatched && t != VM_Scheduler.getCurrentThread())) {
      transferMutex.lock("thread transfer");
      transferQueue.enqueue(t);
      transferMutex.unlock();
    } else if (t.isIdleThread()) {
      idleQueue.enqueue(t);
    } else {
      readyQueue.enqueue(t);
    }
  }

  /**
   * Put thread onto most lightly loaded virtual processor.
   */
  public void scheduleThread(VM_GreenThread t) {
  	
  	// TODO - Deal with processor affinity

    // if t is the last runnable thread on this processor, don't move it
    if (t == VM_Scheduler.getCurrentThread() && readyQueue.isEmpty() && transferQueue.isEmpty()) {
      if (VM.TraceThreadScheduling > 0) {
        VM_Scheduler.trace("VM_Processor.scheduleThread", "staying on same processor:", t.getIndex());
      }
      getCurrentProcessor().transferThread(t);
      return;
    }

    // if a processor is idle, transfer t to it
    VM_GreenSubArchProcessor idle = idleProcessor;
    if (idle != null) {
      idleProcessor = null;
      if (VM.TraceThreadScheduling > 0) {
        VM_Scheduler.trace("VM_Processor.scheduleThread", "outgoing to idle processor:", t.getIndex());
      }
      idle.transferThread(t);
      return;
    }

    // otherwise distribute threads round robin
    if (VM.TraceThreadScheduling > 0) {
      VM_Scheduler.trace("VM_Processor.scheduleThread", "outgoing to round-robin processor:", t.getIndex());
    }
    chooseNextProcessor(t).transferThread(t);

  }

  /**
   * Cycle (round robin) through the available subarch processors.
   */
  private VM_GreenSubArchProcessor chooseNextProcessor(VM_GreenThread t) {
    t.chosenProcessorId = (t.chosenProcessorId % VM_GreenScheduler.numSubArchProcessors) + 1;
    return VM_GreenScheduler.subArchProcessors[t.chosenProcessorId];
  }

  //---------------------//
  // Garbage Collection  //
  //---------------------//

  @LogicallyUninterruptible
  /* GACK --dave */
  public void dumpProcessorState() {
    VM.sysWrite("Processor ");
    VM.sysWriteInt(id);
    if (this == VM_GreenSubArchProcessor.getCurrentProcessor()) VM.sysWrite(" (me)");
    VM.sysWrite(" running thread");
    if (activeThread != null) {
      activeThread.dump();
    } else {
      VM.sysWrite(" NULL Active Thread");
    }
    VM.sysWrite("\n");
    VM.sysWrite(" system thread id ");
    VM.sysWriteInt(pthread_id);
    VM.sysWrite("\n");
    VM.sysWrite(" transferQueue:");
    if (transferQueue != null) transferQueue.dump();
    VM.sysWrite(" readyQueue:");
    if (readyQueue != null) readyQueue.dump();
    VM.sysWrite(" idleQueue:");
    if (idleQueue != null) idleQueue.dump();
    VM.sysWrite(" status: ");
    int status = vpStatus;
    if (status == IN_NATIVE) VM.sysWrite("IN_NATIVE\n");
    if (status == IN_JAVA) VM.sysWrite("IN_JAVA\n");
    if (status == BLOCKED_IN_NATIVE) VM.sysWrite("BLOCKED_IN_NATIVE\n");
    VM.sysWrite(" timeSliceExpired: ");
    VM.sysWriteInt(timeSliceExpired);
    VM.sysWrite("\n");
  }

  /**
   * Fail if thread switching is disabled on this processor
   */
  @Override
  public void failIfThreadSwitchingDisabled() {
    if (!threadSwitchingEnabled()) {
      VM.sysWrite("No threadswitching on proc ", id);
      VM.sysWrite(" with addr ", VM_Magic.objectAsAddress(VM_GreenSubArchProcessor.getCurrentProcessor()));
      if (VM.VerifyAssertions) {
        for (int i=0; i <= -threadSwitchingEnabledCount; i++) {
          VM.sysWrite(" because: ", threadSwitchDisabledReason[i]);
        }
      }
      VM.sysWriteln();
      VM._assert(false);
    }
  }
}
