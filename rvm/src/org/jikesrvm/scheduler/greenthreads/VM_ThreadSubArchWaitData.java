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

import org.vmmagic.pragma.Uninterruptible;

/**
 * Object specifying subarch threads to wait for.
 * Used as event wait data for {@link VM_ThreadEventWaitQueue#enqueue}.
 *
 *
 * @see VM_ThreadEventWaitData
 */
@Uninterruptible
public final class VM_ThreadSubArchWaitData extends VM_ThreadEventWaitData implements VM_ThreadSubArchConstants {

	public int subArchThreadStatus;

  // Offsets of the corresponding entries in VM_ThreadSubArchQueue's
  // file descriptor arrays
  public int subArchThrdsOffset;

  /**
   * Constructor.
   * @param maxWaitCycle the timestamp when the wait should end
   */
  public VM_ThreadSubArchWaitData(long maxWaitNano, int threadId) {
    super(maxWaitNano);
    subArchThreadStatus = threadId;
    subArchThrdsOffset = -1;
  }

  /**
   * Accept a {@link VM_ThreadEventWaitQueue} to inform it
   * of the actual type of this object.
   */
  public void accept(VM_ThreadEventWaitDataVisitor visitor) {
    visitor.visitThreadSubArchWaitData(this);
  }

  public void markAsReady() {
  	subArchThreadStatus |= SUBARCH_READY_BIT;
  }
}
