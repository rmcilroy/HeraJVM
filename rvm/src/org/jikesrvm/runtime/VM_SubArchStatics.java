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
package org.jikesrvm.runtime;

import org.jikesrvm.SubordinateArchitecture;
import org.jikesrvm.VM;
import org.jikesrvm.VM_Constants;
import org.jikesrvm.ArchitectureSpecific.VM_CodeArray;
import org.jikesrvm.cellspu.VM_SubArchBootRecord;
import org.jikesrvm.classloader.VM_Type;
import org.jikesrvm.classloader.VM_TypeReference;
import org.jikesrvm.memorymanagers.mminterface.MM_Constants;
import org.jikesrvm.memorymanagers.mminterface.MM_Interface;
import org.jikesrvm.util.VM_HashMap;
import org.vmmagic.pragma.Uninterruptible;
import org.vmmagic.pragma.UninterruptibleNoWarn;
import org.vmmagic.unboxed.Address;
import org.vmmagic.unboxed.AddressArray;
import org.vmmagic.unboxed.Extent;
import org.vmmagic.unboxed.Offset;
import org.vmmagic.unboxed.Word;

/**
 * This class is an implementation of VM_Statics for the subordinate architecture.
 * It is laid out in the same manner, however, contains only constants required
 * by the methods compiled for the subarch.  Static fields and methods are kept seperatly.
 */
public class VM_SubArchStatics implements VM_Constants {
  /**
   * Static data values (pointed to by subarch's jtoc register).
   * This is currently fixed-size, although at one point the system's plans
   * called for making it dynamically growable.  We could also make it
   * non-contiguous.
   */
	public static final int NO_SLOTS      = 0x400;  // 2kB (512 entries)
	public static final int SIZE_OF_TABLE = NO_SLOTS << LOG_BYTES_IN_INT;
  private static final int[] slots = new int[NO_SLOTS]; 

  /**
   * Object version of the slots used during boot image creation and
   * destroyed shortly after. This is required to support conversion
   * of a slot address to its associated object during boot image
   * creation.
   */
  private static Object[] objectSlots = new Object[0x400];
  
  
  /** Pointers to per-class numeric static blocks */
  private static final AddressArray staticsTOC  = AddressArray.create(0x400);
  
  /** Pointers to per-class reference static blocks */
  private static final AddressArray methodTIB = AddressArray.create(0x200);
  
  /** Each slot holds the size of the num,ref and method TOCs */
  private static final int[] sizeStaticsTable = new int [0x200];
  
  private static int currTOCidx = 0;
  
  
  /**
   * The middle of the table, references are slots above this and
   * numeric values below this. The JTOC points to the middle of the
   * table.
   */
  public static final int middleOfTable = slots.length / 2;

  /** Next available numeric slot number */
  private static int nextNumericSlot = middleOfTable - 1;

  /**
   * Numeric slot hole. Holes are created to align 8byte values. We
   * allocate into a hole rather than consume another numeric slot.
   */
  private static int numericSlotHole = middleOfTable;

  /** Next available reference slot number */
  private static int nextReferenceSlot = middleOfTable + 1;

	public static Offset mainStaticTocAddrOff   = Offset.fromIntZeroExtend(0xdeadbeef);
	public static Offset mainJTOCAddrOff   	 	  = Offset.fromIntZeroExtend(0xdeadbeef);
  
  /**
   * Mapping from int like literals (ints and floats) to the jtoc slot
   * that contains them.
   */
  private static final VM_HashMap<Integer, Integer> intSizeLiterals = new VM_HashMap<Integer, Integer>();

  /**
   * Mapping from long like literals (longs and doubles) to the jtoc
   * slot that contains them.
   */
  private static final VM_HashMap<Long, Integer> longSizeLiterals = new VM_HashMap<Long, Integer>();

  /**
   * Mapping from object literals to the jtoc slot that contains them.
   */
  private static final VM_HashMap<Object, Integer> objectLiterals = new VM_HashMap<Object, Integer>();


  
  /** Initialize some static values which are always available to the subarch */
  public static void init() {
  	//  Stick this in a numeric slot to save messing up things (e.g. in bootImageWriter and GC)
  	mainJTOCAddrOff = VM_SubArchStatics.allocateNumericSlot(BYTES_IN_INT);
  	VM_SubArchStatics.setSlotContents(mainJTOCAddrOff, VM_Magic.getTocPointer().toInt());
  	
  	mainStaticTocAddrOff = VM_SubArchStatics.allocateReferenceSlot();
  	VM_SubArchStatics.setSlotContents(mainStaticTocAddrOff, staticsTOC);
  	
  	// set all Class TOC pointers to Address.max to signal that they are invalid at present
  	if (VM.VerifyAssertions) VM._assert(currTOCidx == 0);
  	for (int i=0; i<methodTIB.length(); i++) {
  		methodTIB.set(i, Address.max());
  	}
  }
  
  /**
   * Conversion from JTOC slot index to JTOC offset.
   */
  @Uninterruptible
  public static Offset slotAsOffset(int slot) {
    return Offset.fromIntSignExtend((slot - middleOfTable) << LOG_BYTES_IN_INT);
  }

  /**
   * Conversion from JTOC offset to JTOC slot index.
   */
  @Uninterruptible
  public static int offsetAsSlot(Offset offset) {
    if (VM.VerifyAssertions) VM._assert((offset.toInt() & 3) == 0);
    return middleOfTable + (offset.toInt() >> LOG_BYTES_IN_INT);
  }

  /**
   * Return the lowest slot number in use
   */
  public static int getLowestInUseSlot() {
    return nextNumericSlot + 1;
  }

  /**
   * Return the highest slot number in use
   */
  public static int getHighestInUseSlot() {
    return nextReferenceSlot - (VM.BuildFor32Addr ? 1 : 2);
  }

  /**
   * Find the given literal in the int like literal map, if not found
   * create a slot for the literal and place an entry in the map
   * @param literal the literal value to find or create
   * @return the offset in the JTOC of the literal
   */
  public static int findOrCreateIntSizeLiteral(int literal) {
    Integer offsetAsInt;
    synchronized (intSizeLiterals) {
      offsetAsInt = intSizeLiterals.get(literal);
      if (offsetAsInt != null) {
        return offsetAsInt;
      } else {   	
        Offset newOff = allocateNumericSlot(BYTES_IN_INT);
        intSizeLiterals.put(literal, newOff.toInt());
        setSlotContents(newOff, literal);
        VM_SubArchBootRecord.updateJtocNumeric();
      	SubordinateArchitecture.VM_SubArchBootRecord.setJtocDirty();   
        return newOff.toInt();
      }
    }
  }

  /**
   * Find the given literal in the long like literal map, if not found
   * create a slot for the literal and place an entry in the map
   * @param literal the literal value to find or create
   * @return the offset in the JTOC of the literal
   */
  public static int findOrCreateLongSizeLiteral(long literal) {
    Integer offsetAsInt;
    synchronized (longSizeLiterals) {
      offsetAsInt = longSizeLiterals.get(literal);
      if (offsetAsInt != null) {
        return offsetAsInt;
      } else {
        Offset newOff = allocateNumericSlot(BYTES_IN_LONG);
        longSizeLiterals.put(literal, newOff.toInt());
        setSlotContents(newOff, literal);
      	SubordinateArchitecture.VM_SubArchBootRecord.setJtocDirty();
        return newOff.toInt();
      }
    }
  }

  /**
   * Find or allocate a slot in the jtoc for a class literal
   * @param typeReferenceID the type reference ID for the class
   * @return the offset of slot that was allocated
   */
  public static int findOrCreateClassLiteral(int typeReferenceID) {
    Class<?> literalAsClass = VM_TypeReference.getTypeRef(typeReferenceID).resolve(true).getClassForType();
    Integer offAsInt;
    synchronized (objectLiterals) {
      offAsInt = objectLiterals.get(literalAsClass);
      if (offAsInt != null) {
        return offAsInt;
      } else {
        Offset newOff = allocateReferenceSlot();
        objectLiterals.put(literalAsClass, newOff.toInt());
        setSlotContents(newOff, literalAsClass);
      	SubordinateArchitecture.VM_SubArchBootRecord.setJtocDirty();
        return newOff.toInt();
      }
    }
  }

  /**
   * Find or allocate a slot in the jtoc for an object literal.
   * @param       literal value
   * @return offset of slot that was allocated
   * Side effect: literal value is stored into jtoc
   */
  public static int findOrCreateObjectLiteral(Object literal) {
    Integer offAsInt;
    synchronized (objectLiterals) {
      offAsInt = objectLiterals.get(literal);
      if (offAsInt != null) {
        return offAsInt;
      } else {
        Offset newOff = allocateReferenceSlot();
        objectLiterals.put(literal, newOff.toInt());
        setSlotContents(newOff, literal);
      	SubordinateArchitecture.VM_SubArchBootRecord.setJtocDirty();
        return newOff.toInt();
      }
    }
  }

  /**
   * Find a slot in the jtoc with this object literal in else return 0
   * @param  literal value
   * @return offset containing literal or 0
   */
  public static int findObjectLiteral(Object literal) {
    Integer offAsInt;
    synchronized (objectLiterals) {
      offAsInt = objectLiterals.get(literal);
    }
    if (offAsInt != null) {
      return offAsInt;
    } else {
      return 0;
    }
  }

  /**
   * Allocate a numeric slot in the jtoc.
   * @param size of slot
   * @return offset of slot that was allocated as int
   * (two slots are allocated for longs and doubles)
   */
  public static synchronized Offset allocateNumericSlot(int size) {
    // Result slot
    int slot;
    // Allocate two slots for wide items after possibly blowing
    // another slot for alignment.  Wide things are longs or doubles
    if (size == BYTES_IN_LONG) {
      // widen for a wide
      nextNumericSlot--;
      // check alignment
      if ((nextNumericSlot & 1) != 0) {
        // slot isn't 8byte aligned so increase by 1 and record hole
        nextNumericSlot--;
        numericSlotHole = nextNumericSlot + 2;
      }
      // Remember the slot and adjust the next available slot
      slot = nextNumericSlot;
      nextNumericSlot--;
      VM_SubArchBootRecord.updateJtocNumeric();
    } else {
      // 4byte quantity, try to reuse hole if one is available
      if ((numericSlotHole != middleOfTable) && (VM_SubArchBootRecord.verifyUpdate(slotAsOffset(numericSlotHole)))) {
        slot = numericSlotHole;
        numericSlotHole = middleOfTable;
      } else {
        slot = nextNumericSlot;
        nextNumericSlot--;
        numericSlotHole = middleOfTable;  // scrap hole as it is now in the cached JTOC part
        VM_SubArchBootRecord.updateJtocNumeric();
      }
    }
    if (nextNumericSlot < 0) {
      enlargeTable();
    }
    return slotAsOffset(slot);
  }

  /**
   * Allocate a reference slot in the jtoc.
   * @return offset of slot that was allocated as int
   * (two slots are allocated on 64bit architectures)
   */
  public static synchronized Offset allocateReferenceSlot() {
    int slot = nextReferenceSlot;
    if (VM.BuildFor64Addr) {
      nextReferenceSlot += 2;
    } else {
      nextReferenceSlot++;
    }
    if (nextReferenceSlot >= slots.length) {
      enlargeTable();
    }
    VM_SubArchBootRecord.updateJtocReference();
    return slotAsOffset(slot);
  }

//  /**
//   * Allocate a method reference slot in the jtoc.
//   * Two words are used - one for address, one for length to 
//   * bring in code to local memory
//   * @return offset of slot that was allocated as int
//   */
//  public static synchronized Offset allocateMethodSlot() {
//    int slot = nextReferenceSlot;
//    nextReferenceSlot += 2;
//  	
//    if (nextReferenceSlot >= slots.length) {
//      enlargeTable();
//    }
//    return slotAsOffset(slot);
//  }
  
  /**
   * Grow the statics table
   */
  private static void enlargeTable() {
    // !!TODO: enlarge slots[] and descriptions[], and modify jtoc register to
    // point to newly enlarged slots[]
    // NOTE: very tricky on IA32 because opt uses 32 bit literal address to access jtoc.
    VM.sysFail("VM_SubArchStatics.enlargeTable: jtoc is full");
  }

  /**
   * Fetch number of numeric jtoc slots currently allocated.
   */
  @Uninterruptible
  public static int getNumberOfNumericSlots() {
    return middleOfTable - nextNumericSlot;
  }

  /**
   * Fetch number of reference jtoc slots currently allocated.
   */
  @Uninterruptible
  public static int getNumberOfReferenceSlots() {
    return nextReferenceSlot - middleOfTable;
  }

  /**
   * Fetch total number of slots comprising the jtoc.
   */
  @Uninterruptible
  public static int getTotalNumberOfSlots() {
    return slots.length;
  }

  /**
   * Does specified jtoc slot contain a reference?
   * @param  slot obtained from offsetAsSlot()
   * @return true --> slot contains a reference
   */
  @Uninterruptible
  public static boolean isReference(int slot) {
    return slot > middleOfTable;
  }

  /**
   * Does specified jtoc slot contain an int sized literal?
   * @param  slot obtained from offsetAsSlot()
   * @return true --> slot contains a reference
   */
  public static boolean isIntSizeLiteral(int slot) {
    if (isReference(slot)) {
      return false;
    } else {
      int ival = getSlotContentsAsInt(slotAsOffset(slot));
      Integer offsetAsInt;
      synchronized (intSizeLiterals) {
        offsetAsInt = intSizeLiterals.get(ival);
      }
      if (offsetAsInt == null) {
        return false;
      } else {
        return slotAsOffset(slot).toInt() == offsetAsInt;
      }
    }
  }

  /**
   * Does specified jtoc slot contain a long sized literal?
   * @param  slot obtained from offsetAsSlot()
   * @return true --> slot contains a reference
   */
  public static boolean isLongSizeLiteral(int slot) {
    if (isReference(slot)) {
      return false;
    } else {
      long lval = getSlotContentsAsLong(slotAsOffset(slot));
      Integer offsetAsInt;
      synchronized (longSizeLiterals) {
        offsetAsInt = longSizeLiterals.get(lval);
      }
      if (offsetAsInt == null) {
        return false;
      } else {
        return slotAsOffset(slot).toInt() == offsetAsInt;
      }
    }
  }

  /**
   * Get size occupied by a reference
   */
  @Uninterruptible
  public static int getReferenceSlotSize() {
    return VM.BuildFor64Addr ? 2 : 1;
  }

  /**
   * Fetch jtoc object (for JNI environment and GC).
   */
  @Uninterruptible
  public static Address getSlots() {
    return VM_Magic.objectAsAddress(slots).plus(middleOfTable << LOG_BYTES_IN_INT);
  }

  /**
   * Fetch jtoc object (for JNI environment and GC).
   */
  @Uninterruptible
  public static int[] getSlotsAsIntArray() {
    return slots;
  }

  /**
   * Fetch contents of a slot, as an integer
   */
  @Uninterruptible
  public static int getSlotContentsAsInt(Offset offset) {
  	if (VM.runningVM) {
  		return VM_Magic.getIntAtOffset(slots, offset.plus(middleOfTable << LOG_BYTES_IN_INT));
  	} else {
  		int slot = offsetAsSlot(offset);
      return slots[slot];
    }
  }

  /**
   * Fetch contents of a slot-pair, as a long integer.
   */
  @Uninterruptible
  public static long getSlotContentsAsLong(Offset offset) {
  	if (VM.runningVM) {
    	long ret = VM_Magic.getLongAtOffset(slots, offset.plus(middleOfTable << LOG_BYTES_IN_INT));
	    return ret;
  	} else {
  		int slot = offsetAsSlot(offset);
  		long result;	
  		if (VM.LittleEndian) {
  			result = (((long) slots[slot + 1]) << BITS_IN_INT); // hi
  			result |= ((long) slots[slot]) & 0xFFFFFFFFL; // lo
  		} else {
  			result = (((long) slots[slot]) << BITS_IN_INT);     // hi
  			result |= ((long) slots[slot + 1]) & 0xFFFFFFFFL; // lo
  		}
  		return result;
  	}
  }

  /**
   * Fetch contents of a slot, as an object.
   */
  @Uninterruptible
  public static Object getSlotContentsAsObject(Offset offset) {
  	if (VM.runningVM) {
      return VM_Magic.getObjectAtOffset(slots, offset.plus(middleOfTable << LOG_BYTES_IN_INT));
	  } else {
	    return objectSlots[offsetAsSlot(offset)];
	  }
  }

  /**
   * Fetch contents of a slot, as an Address.
   */
  @UninterruptibleNoWarn
  public static Address getSlotContentsAsAddress(Offset offset) {
  	if (VM.runningVM) {
      if (VM.BuildFor32Addr) {
	      return Address.fromIntSignExtend(getSlotContentsAsInt(offset));
	    } else {
	      return Address.fromLong(getSlotContentsAsLong(offset));
	    }
	  } else {
	    // Addresses are represented by objects in the tools building the VM
	    Object unboxed = objectSlots[offsetAsSlot(offset)];
	    if (unboxed instanceof Address) {
	      return (Address) unboxed;
	    } else if (unboxed instanceof Word) {
	      return ((Word) unboxed).toAddress();
	    } else if (unboxed instanceof Extent) {
	      return ((Extent) unboxed).toWord().toAddress();
	    } else if (unboxed instanceof Offset) {
	      return ((Offset) unboxed).toWord().toAddress();
	    } else {
	      if (VM.VerifyAssertions) VM._assert(false);
	      return Address.zero();
	    }
	  }
  }

  /**
   * Set contents of a slot, as an integer.
   */
  @Uninterruptible
  public static void setSlotContents(Offset offset, int value) {
  	if (VM.runningVM) {
  		if (VM.VerifyAssertions) VM._assert(VM_SubArchBootRecord.verifyUpdate(offset));
  		VM_Magic.setIntAtOffset(slots, offset.plus(middleOfTable << LOG_BYTES_IN_INT), value);
	  } else {
	    slots[offsetAsSlot(offset)] = value;
	  }
  	SubordinateArchitecture.VM_SubArchBootRecord.setJtocDirty();
  }

  /**
   * Set contents of a slot, as a long integer.
   */
  @UninterruptibleNoWarn
  public static void setSlotContents(Offset offset, long value) {
  	if (VM.runningVM) {
  		if (VM.VerifyAssertions) VM._assert(VM_SubArchBootRecord.verifyUpdate(offset));
  		VM_Magic.setLongAtOffset(slots, offset.plus(middleOfTable << LOG_BYTES_IN_INT), value);
  	} else {
      int slot = offsetAsSlot(offset);
      if (VM.LittleEndian) {
        slots[slot + 1] = (int) (value >>> BITS_IN_INT); // hi
        slots[slot] = (int) (value); // lo
      } else {
        slots[slot] = (int) (value >>> BITS_IN_INT); // hi
        slots[slot + 1] = (int) (value); // lo
      }
    }
  	SubordinateArchitecture.VM_SubArchBootRecord.setJtocDirty();
  }

  /**
   * Set contents of a slot, as an object.
   */
  @UninterruptibleNoWarn
  public static void setSlotContents(Offset offset, Object object) {
    // NB uninterruptible warnings are disabled for this method due to
    // the array store which could cause a fault - this can't actually
    // happen as the fault would only ever occur when not running the
    // VM. We suppress the warning as we know the error can't happen.
  	
    if (VM.runningVM && MM_Constants.NEEDS_PUTSTATIC_WRITE_BARRIER) {
    	if (VM.VerifyAssertions) VM._assert(VM_SubArchBootRecord.verifyUpdate(offset));
  		MM_Interface.putstaticWriteBarrier(offset, object, 0);
    } else {
      setSlotContents(offset, VM_Magic.objectAsAddress(object).toWord());
    }
    if (VM.VerifyAssertions) VM._assert(offset.toInt() > 0);
    if (!VM.runningVM && objectSlots != null) {
      // When creating the boot image objectSlots is populated as
      // VM_Magic won't work in the bootstrap JVM.
      objectSlots[offsetAsSlot(offset)] = object;
    }
  	SubordinateArchitecture.VM_SubArchBootRecord.setJtocDirty();
  }
  
  /**
   * Set contents of a slot, as a VM_CodeArray.
   */
  @Uninterruptible
  public static void setSlotContents(Offset offset, VM_CodeArray code) {
  	if (VM.VerifyAssertions) VM._assert(VM_SubArchBootRecord.verifyUpdate(offset));
		setSlotContents(offset, VM_Magic.codeArrayAsObject(code));
  	SubordinateArchitecture.VM_SubArchBootRecord.setJtocDirty();
  }

  /**
   * Set contents of a slot, as a Word.
   */
  @Uninterruptible
  public static void setSlotContents(Offset offset, Word word) {
    if (VM.runningVM) {
    	if (VM.VerifyAssertions) VM._assert(VM_SubArchBootRecord.verifyUpdate(offset));
  		VM_Magic.setWordAtOffset(slots, offset.plus(middleOfTable << LOG_BYTES_IN_INT), word);
    } else {
      if (VM.BuildFor32Addr) {
        setSlotContents(offset, word.toInt());
      } else {
        setSlotContents(offset, word.toLong());
      }
    }
  	SubordinateArchitecture.VM_SubArchBootRecord.setJtocDirty();
  }

  /**
   * Inform VM_Statics that boot image instantiation is over and that
   * unnecessary data structures, for runtime, can be released
   */
  public static void bootImageInstantiationFinished() {
    objectSlots = null;
  }

  /**
   * Search for a type that this TIB
   * @param tibOff offset of TIB in JTOC
   * @return type of TIB or null
   */
  public static VM_Type findTypeOfTIBSlot(Offset tibOff) {
    for (VM_Type type : VM_Type.getTypes()) {
      if (type != null && type.getTibOffset().EQ(tibOff)) {
        return type;
      }
    }
    return null;
  }
  
  /** 
   * Store details of a new type in the subarch static tables
   */
  public static synchronized Offset addNewType(Address numStaticsBlock, int numStaticsLen,
  			                                       Address refStaticsBlock, int refStaticsLen,
  											              			   Address methodTIBBlock, int methodTIBLen) {
  	
  	if (VM.runningVM) {
	  	staticsTOC.set((2*currTOCidx), numStaticsBlock);
	  	staticsTOC.set((2*currTOCidx) + 1, refStaticsBlock);
  	} else {
  		// need to do this later as a fixup otherwise the value will be overwritten
  		VM_Magic.bootWriterFixup(staticsTOC.getBacking(), (2*currTOCidx) << LOG_BYTES_IN_ADDRESS, numStaticsBlock.toInt());
  		VM_Magic.bootWriterFixup(staticsTOC.getBacking(), ((2*currTOCidx) + 1) << LOG_BYTES_IN_ADDRESS, refStaticsBlock.toInt());  		
  	}
  	methodTIB.set(currTOCidx, methodTIBBlock);
  	
  	// compress the three sizes into a single entry
  	numStaticsLen = VM_Memory.alignUp(numStaticsLen, BYTES_IN_QUAD);
  	numStaticsLen = numStaticsLen >> LOG_BYTES_IN_QUAD;
  	refStaticsLen = VM_Memory.alignUp(refStaticsLen, BYTES_IN_QUAD);
  	refStaticsLen = refStaticsLen >> LOG_BYTES_IN_QUAD;
  	methodTIBLen  = VM_Memory.alignUp(methodTIBLen, BYTES_IN_QUAD);
  	methodTIBLen  = methodTIBLen >> LOG_BYTES_IN_QUAD;
  	
  	if (VM.VerifyAssertions) VM._assert((numStaticsLen < (1 << 11)) && 
  																			(refStaticsLen < (1 << 11)) && 
  																			(methodTIBLen < (1 << 11)));
  	
  	sizeStaticsTable[currTOCidx] = (methodTIBLen << 20) | (refStaticsLen << 10) | numStaticsLen;
  	
  	Offset ret = Offset.fromIntSignExtend(currTOCidx << LOG_BYTES_IN_ADDRESS);
  	currTOCidx++;
  	return ret;
  }

	public static Object getMethodTIB() {
		return methodTIB;
	}

	public static Object getSizeStaticsTable() {
		return sizeStaticsTable;
	}
}
