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

/**
 * Constants associated with subarch waits.
 *
 *
 * @see VM_ThreadSubArchWaitData
 * @see VM_ThreadSubArchQueue
 */
public interface VM_ThreadSubArchConstants {

	public static final int SUBARCH_READY_BIT             = 0x1 << 31;
	
}
