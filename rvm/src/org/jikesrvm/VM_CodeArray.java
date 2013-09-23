package org.jikesrvm;

import org.vmmagic.pragma.Uninterruptible;

@Uninterruptible
public abstract class VM_CodeArray {

	public abstract int length();
	
	public abstract Object getBacking();
}
