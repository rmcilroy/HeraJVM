package org.jikesrvm.runtime;

import org.jikesrvm.classloader.VM_Method;
import org.jikesrvm.util.VM_HashMap;
import org.vmmagic.unboxed.Offset;

public interface VM_SubArchEntrypoints {
	
	public class OffsetTuple {
		public Offset methodOffset;
		public Offset sizeOffset;
	}
	
	public static final VM_HashMap<VM_Method, OffsetTuple> entryPointOffsets = new VM_HashMap<VM_Method, OffsetTuple>();
	
	String subArch = "cellspu";

  VM_Method runtimeEntry = 
  	VM_EntrypointHelper.getMethod("Lorg/jikesrvm/" + subArch + "/VM_RuntimeMethods;", 
  					 "runtimeEntry",
  					 "(I)V");

}
