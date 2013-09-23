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
import org.jikesrvm.runtime.VM_Magic;
import static org.jikesrvm.runtime.VM_SysCall.sysCall;
import org.jikesrvm.runtime.VM_Time;
import org.jikesrvm.scheduler.VM_Scheduler;
import org.vmmagic.pragma.Uninterruptible;

/**
 * Low priority thread to run when there's nothing else to do.
 * This thread also handles initializing the virtual processor
 * for execution.
 *
 * This follows the Singleton pattern.
 */
final class VM_SubArchIdleThread extends VM_GreenThread {

  /**
   * A thread to run if there is no other work for a virtual processor.
   */
  VM_SubArchIdleThread(VM_GreenSubArchProcessor processorAffinity) {
    super("VM_SubArchIdleThread");
    makeDaemon(true);
    super.processorAffinity = processorAffinity;
    setPriority(Thread.MIN_PRIORITY);
  }

  /**
   * Is this the idle thread?
   * @return true
   */
  @Uninterruptible
  @Override
  public boolean isIdleThread() {
    return true;
  }

  @Override
  public void run() { // overrides VM_Thread
    if (state != State.RUNNABLE)
      changeThreadState(State.NEW, State.RUNNABLE);
    VM_GreenSubArchProcessor myProcessor = VM_GreenSubArchProcessor.getCurrentProcessor();
    if (VM.ExtremeAssertions) VM._assert(myProcessor == processorAffinity);

    myProcessor.initializeProcessor();

    // Only perform load balancing if there is more than one processor.
    final boolean loadBalancing = VM_GreenScheduler.numSubArchProcessors > 1;
    main:
    while (true) {
      if (VM_Scheduler.terminated) terminate();
      // FIXME: if (VM.VerifyAssertions) VM._assert(processorAffinity.idleQueue.isEmpty());

      for ( ; ; ) {
        VM_GreenSubArchProcessor.idleProcessor = myProcessor;
        if (availableWork(myProcessor)) {
          if (VM.ExtremeAssertions) {
            VM._assert(myProcessor == VM_GreenSubArchProcessor.getCurrentProcessor());
          }
          VM_GreenThread.yield(VM_GreenSubArchProcessor.getCurrentProcessor().idleQueue);
          continue main;
        }
      }
    }
  }

  /**
   * @return true, if there appears to be a runnable thread for the processor to execute
   */
  private static boolean availableWork(VM_GreenSubArchProcessor p) {
    if (!p.readyQueue.isEmpty()) return true;
    VM_Magic.isync();
    if (!p.transferQueue.isEmpty()) return true;
    if (VM_GreenScheduler.wakeupQueue.isReady()) {
      VM_GreenScheduler.wakeupMutex.lock("wakeup mutex");
      VM_GreenThread t = VM_GreenScheduler.wakeupQueue.dequeue();
      VM_GreenScheduler.wakeupMutex.unlock();
      if (t != null) {
        t.schedule();
        return true;
      }
    }
    return false;
  }
}
