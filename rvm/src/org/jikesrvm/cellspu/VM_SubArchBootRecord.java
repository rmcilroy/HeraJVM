package org.jikesrvm.cellspu;

import org.jikesrvm.VM;
import org.jikesrvm.VM_Constants;
import org.jikesrvm.SubordinateArchitecture.VM_CodeArray;
import org.jikesrvm.runtime.VM_Magic;
import org.jikesrvm.runtime.VM_Memory;
import org.jikesrvm.runtime.VM_SubArchEntrypoints;
import org.jikesrvm.runtime.VM_SubArchStatics;
import org.vmmagic.pragma.Uninterruptible;
import org.vmmagic.pragma.UninterruptibleNoWarn;
import org.vmmagic.unboxed.Address;
import org.vmmagic.unboxed.AddressArray;
import org.vmmagic.unboxed.Offset;


public class VM_SubArchBootRecord implements VM_Constants {

	// reference to global subarch bootrecord
	private static VM_SubArchBootRecord br = null;
	// TODO - Remove
	private static AddressArray fakeTrapStrsStatic = AddressArray.create(4096);
	private static int fakeTrapStrsIdx = 0;
	
	// JTOC address and middle offset
  public Address jtocStart;
  public int     jtocMiddleOffset;
  
  public int jtocNumericOffset;
  public int jtocReferenceOffset;

  public int jtocLastCachedNumericOffset;
  public int jtocLastCachedReferenceOffset;
  
  public Object classTOCsTable;
  public Object staticsSizeTable;
  
  // signal that JTOC has been modified
  public /* TODO - volatile? */ int jtocDirty;
  
	// Out of line machine code necessary to support runtime
  public VM_CodeArray oolRuntimeCodeInstructions;
  public int oolRuntimeCodeLength;
  
  // Runtime methods
  public VM_CodeArray runtimeEntryMethod;
  public int runtimeEntryLength;
  
  // table of strings used by fakeTrap's
  public AddressArray fakeTrapStrs;
  
  /** 
   * Signals that the subarch processors / threads have booted up
   */
  private volatile int subArchBootComplete = 0;
  
  /**
   * Specifies the number of subarch processors / threads which have been brought up
   */
  private volatile int noSubArchProcs = 0;
  
  public void init() {
  	if (VM.writingBootImage) {
  		VM._assert(br == null);
  		br = this;
  		
  		this.jtocStart     = VM_Magic.objectAsAddress(VM_SubArchStatics.getSlotsAsIntArray());
  		this.jtocMiddleOffset = VM_SubArchStatics.middleOfTable << LOG_BYTES_IN_INT;
			this.jtocNumericOffset   = BYTES_IN_INT - (VM_SubArchStatics.getNumberOfNumericSlots() << LOG_BYTES_IN_INT);
			this.jtocReferenceOffset = VM_SubArchStatics.getNumberOfReferenceSlots() << LOG_BYTES_IN_INT;
			this.jtocLastCachedNumericOffset   = 0;
			this.jtocLastCachedReferenceOffset = 0;
			this.jtocDirty = 1;
			
			this.classTOCsTable   = VM_SubArchStatics.getMethodTIB();
			this.staticsSizeTable = VM_SubArchStatics.getSizeStaticsTable();
			
			this.oolRuntimeCodeInstructions = VM_OutOfLineMachineCode.runtimeInstructionsMainMem;
			this.oolRuntimeCodeLength = VM_Memory.alignUp(this.oolRuntimeCodeInstructions.length() << LOG_BYTES_IN_INT, BYTES_IN_QUAD);
			
	  	this.runtimeEntryMethod = (VM_CodeArray) VM_SubArchEntrypoints.runtimeEntry.getCurrentEntryCodeArray(true);
	  	this.runtimeEntryLength = VM_SubArchEntrypoints.runtimeEntry.getSubArchLength();
  	
	  	this.fakeTrapStrs = fakeTrapStrsStatic;
  	}	
  }
  
  @Uninterruptible
  public static void setJtocDirty() {
  	if (VM.runningVM) {
  		br.jtocDirty = 1;
  	}
  }
  
  @Uninterruptible
  public static boolean isJtocDirty() {
  	if (VM.runningVM) {
  		return br.jtocDirty != 0;
  	} else {
  		return true;
  	}
  }
  
  @Uninterruptible
  public static void updateJtocNumeric() {
  	if (VM.runningVM) {
  		br.jtocNumericOffset = BYTES_IN_INT - (VM_SubArchStatics.getNumberOfNumericSlots() << LOG_BYTES_IN_INT);
  	}
  }
  
  @Uninterruptible
  public static void updateJtocReference() {
  	if (VM.runningVM) {
    	br.jtocReferenceOffset = (VM_SubArchStatics.getNumberOfReferenceSlots() << LOG_BYTES_IN_INT);
  	}
  }
  
  @UninterruptibleNoWarn
  public static boolean verifyUpdate(Offset slot) {
  	if (VM.runningVM) {
  		//VM.sysWriteln("offset - " + slot.toInt() + " last cached num = " + br.jtocLastCachedNumericOffset + " lst cached ref = " + br.jtocLastCachedReferenceOffset);
  		// assert that slot being updated is not one that has already been cached by subarch
  		return (slot.sLE(Offset.fromIntSignExtend(br.jtocLastCachedNumericOffset)) || slot.sGE(Offset.fromIntSignExtend(br.jtocLastCachedReferenceOffset)));
  	} else {
  		return true;
  	}
  }

	public static boolean isSubArchStarted() {
		if (VM.runningVM) {
  		return br.subArchBootComplete != 0;
  	} else {
  		return false;
  	}
	}
	
	public static int getNumSubArchProcs() {
		if (VM.runningVM) {
  		return br.noSubArchProcs;
  	} else {
  		return 0;
  	}
	}

	public static int addFakeTrapStr(Address address) {
		fakeTrapStrsStatic.set(fakeTrapStrsIdx, address);
		fakeTrapStrsIdx += 1;
		return fakeTrapStrsIdx - 1;
	}
}
