package org.jikesrvm;

public abstract class VM_MachineCode {

	public abstract int[] getBytecodeMap();

	public abstract VM_CodeArray getInstructions();

	public abstract void setBytecodeMap(int[] newmap);

}
