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
package org.jikesrvm.cellspu;

import org.jikesrvm.SubordinateArchitecture;
import org.jikesrvm.VM;
import org.jikesrvm.classloader.VM_Atom;
import org.jikesrvm.compilers.common.assembler.VM_ForwardReference;
import org.jikesrvm.compilers.common.assembler.cellspu.VM_Assembler;
import org.jikesrvm.compilers.common.assembler.cellspu.VM_AssemblerConstants;
import org.jikesrvm.objectmodel.VM_JavaHeader;
import org.jikesrvm.objectmodel.VM_JavaHeaderConstants;
import org.jikesrvm.objectmodel.VM_ObjectModel;
import org.jikesrvm.objectmodel.VM_TIBLayoutConstants;
import org.jikesrvm.runtime.VM_Magic;
import org.jikesrvm.runtime.VM_Memory;
import org.jikesrvm.runtime.VM_Statics;
import org.jikesrvm.runtime.VM_SubArchStatics;
import org.vmmagic.unboxed.Address;
import org.vmmagic.unboxed.Offset;

/**
 * A place to put hand written machine code typically invoked by VM_Magic
 * methods.
 *
 * Hand coding of small inline instruction sequences is typically handled by
 * each compiler's implementation of VM_Magic methods.  A few VM_Magic methods
 * are so complex that their implementations require many instructions.
 * But our compilers do not inline arbitrary amounts of machine code.
 * We therefore write such code blocks here, out of line.
 *
 * These code blocks can be shared by all compilers. They can be branched to
 * via a jtoc offset (obtained from VM_SubArchEntrypoints.XXXInstructionsMethod).
 *
 * 17 Mar 1999 Derek Lieber
 *
 * 15 Jun 2001 Dave Grove and Bowen Alpern (Derek believed that compilers
 * could inline these methods if they wanted.  We do not believe this would
 * be very easy since they return thru the LR.)
 */
public abstract class VM_OutOfLineMachineCode
    implements VM_BaselineConstants, VM_AssemblerConstants {
	
	private static Offset codeCacheNextOff   = null;
	private static Offset codeCacheEndOff    = null;
	private static Offset objectCacheNextOff = null;
	private static Offset objectCacheEndOff  = null;
	private static Offset staticCacheNextOff = null;
	private static Offset staticCacheEndOff  = null;
	private static Offset classTibsCacheNextOff= null;
	private static Offset classTibsCacheEndOff = null;
	
  public static SubordinateArchitecture.VM_CodeArray runtimeInstructionsMainMem;

  // use int's so this doesn't mess up in the bootwriter (should be LocalAddresse)
  public static int initRuntimeInstructions = Integer.MAX_VALUE;
  public static int flushCacheInstructions = Integer.MAX_VALUE;
  public static int blockUntilTagCompletesInstructions = Integer.MAX_VALUE;
  public static int cacheMethodInstructions = Integer.MAX_VALUE;
  public static int allocObjectCacheInstructions = Integer.MAX_VALUE;
  public static int cacheObjectInstructions = Integer.MAX_VALUE;
  public static int cacheArrayInstructions = Integer.MAX_VALUE;
  public static int cacheArrayBlockInstructions = Integer.MAX_VALUE;
  public static int cacheStaticInstructions = Integer.MAX_VALUE;
  public static int cacheClassTibInstructions = Integer.MAX_VALUE;
  public static int reflectiveMethodInvokerInstructions = Integer.MAX_VALUE;
  public static int trapHandlerInstructions = Integer.MAX_VALUE;

	
  public static void init() {
  	if (VM.writingBootImage) {	

	  	// allocate space in subarch JTOC for memory allocation pointers
	  	codeCacheNextOff   = VM_SubArchStatics.allocateNumericSlot(BYTES_IN_INT);
	  	codeCacheEndOff    = VM_SubArchStatics.allocateNumericSlot(BYTES_IN_INT);
	  	objectCacheNextOff = VM_SubArchStatics.allocateNumericSlot(BYTES_IN_INT);
	  	objectCacheEndOff  = VM_SubArchStatics.allocateNumericSlot(BYTES_IN_INT);

	  	staticCacheNextOff = VM_SubArchStatics.allocateNumericSlot(BYTES_IN_INT);
	  	staticCacheEndOff  = VM_SubArchStatics.allocateNumericSlot(BYTES_IN_INT);
	  	classTibsCacheNextOff= VM_SubArchStatics.allocateNumericSlot(BYTES_IN_INT);
	  	classTibsCacheEndOff = VM_SubArchStatics.allocateNumericSlot(BYTES_IN_INT);
	  	
	  	VM_SubArchStatics.setSlotContents(codeCacheNextOff, CODE_CACHE_START);
	  	VM_SubArchStatics.setSlotContents(codeCacheEndOff, CODE_CACHE_END);
	  	VM_SubArchStatics.setSlotContents(objectCacheNextOff, OBJECT_CACHE_START);
	  	VM_SubArchStatics.setSlotContents(objectCacheEndOff, OBJECT_CACHE_END);

	  	VM_SubArchStatics.setSlotContents(staticCacheNextOff, STATICS_START);
	  	VM_SubArchStatics.setSlotContents(staticCacheEndOff, STATICS_END);
	  	VM_SubArchStatics.setSlotContents(classTibsCacheNextOff, CLASS_TIBS_START);
	  	VM_SubArchStatics.setSlotContents(classTibsCacheEndOff, CLASS_TIBS_END);

	  	// generate out of line instructions
	  	VM_Assembler asm = new SubordinateArchitecture.VM_Assembler(0);
	  	int codeStartOffset = RUNTIME_CODE_START;
	  	initRuntimeInstructions = codeStartOffset;
	  	codeStartOffset = genInstructions("generateInitRuntimeInstructions", asm, codeStartOffset);    
	  	flushCacheInstructions = codeStartOffset;
	  	codeStartOffset = genInstructions("generateFlushCacheInstructions", asm, codeStartOffset);    
	  	blockUntilTagCompletesInstructions = codeStartOffset;
	  	codeStartOffset = genInstructions("generateBlockUntilTagCompletes", asm, codeStartOffset);  
	  	cacheMethodInstructions = codeStartOffset;
	  	codeStartOffset = genInstructions("generateCacheMethodInstructions", asm, codeStartOffset);    
	  	allocObjectCacheInstructions = codeStartOffset;
	  	codeStartOffset = genInstructions("genAllocFromObjectCacheInstructions", asm, codeStartOffset);    
	  	cacheObjectInstructions = codeStartOffset;
	  	codeStartOffset = genInstructions("generateCacheObjectInstructions", asm, codeStartOffset);    
	  	cacheArrayInstructions = codeStartOffset;
	  	codeStartOffset = genInstructions("generateCacheArrayInstructions", asm, codeStartOffset);    
	  	cacheArrayBlockInstructions = codeStartOffset;
	  	codeStartOffset = genInstructions("generateCacheArrayBlockInstructions", asm, codeStartOffset);    
	  	cacheStaticInstructions = codeStartOffset;
	  	codeStartOffset = genInstructions("generateCacheStaticInstructions", asm, codeStartOffset);  
	  	cacheClassTibInstructions = codeStartOffset;
	  	codeStartOffset = genInstructions("generateCacheClassTibInstructions", asm, codeStartOffset);    
	  	reflectiveMethodInvokerInstructions = codeStartOffset;
	  	codeStartOffset = genInstructions("generateReflectiveMethodInvokerInstructions", asm, codeStartOffset);    

	    if (VM.VerifyAssertions) VM._assert(codeStartOffset <= TRAP_ENTRYPOINT);
	    codeStartOffset = TRAP_ENTRYPOINT;
	    
	    // TODO - sort out trap bit!
	    trapHandlerInstructions = codeStartOffset;
	    codeStartOffset = genInstructions("generateTrapHandlerInstructions", asm, codeStartOffset);    
	    
	    runtimeInstructionsMainMem = asm.makeMachineCode().getInstructions();
	    
	    if (VM.VerifyAssertions) VM._assert(codeStartOffset <= CODE_ENTRYPOINT);
  	}
  }
  

	private static int genInstructions(String genMethod, VM_Assembler asm, int codeStartOffset) {
		try {
			int diff, prev;
			prev = (asm.getMachineCodeIndex() << LG_INSTRUCTION_WIDTH);
			VM_OutOfLineMachineCode.class.getDeclaredMethod(genMethod, new Class[] {VM_Assembler.class}).invoke(VM_OutOfLineMachineCode.class, new Object[] {asm});
			diff = (asm.getMachineCodeIndex() << LG_INSTRUCTION_WIDTH) - prev;
			codeStartOffset = codeStartOffset + diff;
			return codeStartOffset;	
		} catch (Exception e) {
			e.printStackTrace();
			VM._assert(NOT_REACHED);
			return 0;
		}
	}  
  
  private static void generateInitRuntimeInstructions(VM_Assembler asm) {
   	// copy phys_id to T0 to be used in 
  	asm.emitORI(T0, 0, 0);
  	asm.emitILW(FP, STACK_BEGIN); // start stack from end of memory (leave buffer for stack objects)
  	asm.emitILA(JTOC, JTOC_TABLE_START + (JTOC_TABLE_LENGTH / 2));
  	asm.emitILA(TRAP_ENTRY_REG, TRAP_ENTRYPOINT);
  	asm.emitIL(PROCESSOR_REGISTER, -1);	  // This will be set later in VM_RuntimeMethods
	  
  	asm.emitILA(LINK_REG, CODE_ENTRYPOINT);
  	// runon to flush cache, jump to code entrypoint through link register
  }
  
  private static void generateFlushCacheInstructions(VM_Assembler asm) {
  	// reload numeric and ref static tables to flush statics
  	asm.emitLoad(S2, JTOC, VM_SubArchStatics.mainStaticTocAddrOff);
  	asm.emitILA(S3, STATICS_TOC);
  	asm.emitILA(S4, STATICS_TOC_LENGTH);
  	asm.emitGET(S2, S4, S3, STATIC_CACHE_READ_TAG_GROUP);
  	// reset statics cache as empty
  	asm.emitILA(T6, STATICS_START);
  	asm.emitStore(T6, JTOC, staticCacheNextOff);
  	
  	// clear out object cache table
  	// TODO - Add hint for branch
  	asm.emitIL(S4, 0);
  	asm.emitILA(T1, OBJECT_CACHE_TABLE);
  	asm.emitILW(S5, OBJECT_CACHE_TABLE_LENGTH);
  	int zeroLoopIndex = asm.getMachineCodeIndex();
  	asm.emitAI(S5, S5, -BYTES_IN_QUAD);
  	asm.emitSTQX(S4, T1, S5);
  	asm.emitBRNZ(S5, zeroLoopIndex);
  	
  	// reset cache as empty
  	asm.emitILA(T6, OBJECT_CACHE_START);
  	asm.emitStore(T6, JTOC, objectCacheNextOff);
  	
  	// wait for writes to complete (otherwise backing may be overwritten since cache now empty)
  	asm.emitBlockUntilComplete(OBJECT_CACHE_WRITE_TAG_GROUP);
  	// wait for static TOC to be reloaded
  	asm.emitBlockUntilComplete(STATIC_CACHE_READ_TAG_GROUP);
  	
  	// branch back to method
  	asm.emitBI(LINK_REG);
  }
    
 	/** Expects tag bitmask in S0 */
  private static void generateBlockUntilTagCompletes(VM_Assembler asm) {
  	// block until method is read
  	asm.emitBlockUntilComplete();	

  	// branch back to method
  	asm.emitBI(LINK_REG); 	
  }
  
  /**
   * trap Params  - methodRefReg(mainAddr): S5, staticsAddrReg: T7, methodOffsetReg: T5 (if methodOffset is in JTOC, size offset is int T6)
   * trap Returns - methodRefReg(localAddr): S5
   */
  private static void generateCacheMethodInstructions(VM_Assembler asm) {
  	// TODO - Branch hint
  	
  	// load size into T2
  	asm.emitAI(S1, T5, BYTES_IN_ADDRESS);
  	asm.emitCEQ(S0, JTOC, T7);
  	asm.emitSELB(T6, S1, T6, S0);
  	
  	asm.emitLoad(T2, T7, T6);
  	
  	genAllocFromCodeCache(asm);
  	
  	// pull code into local memory
  	asm.emitGET(S5, T2, T3, METHOD_CACHE_READ_TAG_GROUP);
  	
  	// update method pointer in class static to point to cached entry
  	asm.emitStore(T3, T7, T5);
  	// update S5 which is used to jump to the newly cached method
  	asm.emitORI(S5, T3, 0);
  	
  	asm.emitAI(LINK_REG, LINK_REG, BYTES_IN_ADDRESS);  // correct link reg

  	asm.emitILW(S0, (0x1 << METHOD_CACHE_READ_TAG_GROUP));

  	if (VM.VerifyAssertions) VM._assert(blockUntilTagCompletesInstructions != Integer.MAX_VALUE);
  	int currAddr = asm.getMachineCodeIndex() << LOG_BYTES_IN_INT;
  	asm._emitBR((blockUntilTagCompletesInstructions - currAddr) >> LOG_BYTES_IN_INT);
  }
  
  private static void generateCacheObjectInstructions(VM_Assembler asm) {
  	// Object reference in T4 and expected size in T2, S6 holds cache entry index
  	// output resulting reference in T1
  	
  	// correct pointer for header
  	asm.emitAI(S5, T4, -(3 * BYTES_IN_INT));
  	
  	// align transfer to 16 bytes
  	asm.emitANDI(S7, S5, 0xf);
  	asm.emitANDC(S5, S5, S7);
  	
  	// update size with alignment
  	asm.emitA(T2, T2, S7);
  	
  	// align to a 16 byte transfer
  	asm.emitANDI(S4, T2, 0xf);
  	asm.emitANDC(T2, T2, S4);
  	
  	// TODO get rid of this branch if possible
  	VM_ForwardReference fr1 = asm.emitForwardBRZ(S4);
  	asm.emitAI(T2, T2, BYTES_IN_QUAD);
  	fr1.resolve(asm);
  	
  	if (VM.VerifyAssertions) VM._assert(allocObjectCacheInstructions != Integer.MAX_VALUE);
  	int currAddr = asm.getMachineCodeIndex() << LOG_BYTES_IN_INT;
  	asm._emitBRSL(S4, (allocObjectCacheInstructions - currAddr) >> LOG_BYTES_IN_INT);
  	
  	// pull object into local memory
  	asm.emitGET(S5, T2, T3, OBJECT_CACHE_READ_TAG_GROUP);
  	
  	// generate actual local memory reference to object after alignment
  	asm.emitAI(S2, T3, (3 * BYTES_IN_INT));
  	asm.emitA(S3, S2, S7);
  	
  	// embed size into pointer
  	asm.emitSF(T2, S7, T2); // subtract size added before object for alignment 
  	asm.emitSHLI(T2, T2, 18);  
  	asm.emitOR(T1, S3, T2);
  	
  	// update object cache with this entry
  	asm.emitCWD(S4, JTOC, BYTES_IN_INT);
  	asm.emitSHUFB(S5, T1, T4, S4);
  	asm.emitILW(S4, OBJECT_CACHE_TABLE);
  	asm.emitStoreDouble(S5, S4, S6);
  	
  	asm.emitILW(S0, (0x1 << OBJECT_CACHE_READ_TAG_GROUP));
  	asm.emitAI(LINK_REG, LINK_REG, BYTES_IN_INT);  // correct link register
  	if (VM.VerifyAssertions) VM._assert(blockUntilTagCompletesInstructions != Integer.MAX_VALUE);
  	currAddr = asm.getMachineCodeIndex() << LOG_BYTES_IN_INT;
  	asm._emitBR((blockUntilTagCompletesInstructions - currAddr) >> LOG_BYTES_IN_INT);
  }


  private static void generateCacheArrayInstructions(VM_Assembler asm) {
  	// Pull header into local memory (Arrays are aligned to quadword boundaries
  	asm.emitAI(S5, T0, -BYTES_IN_QUAD);
  	
  	// returns local reference in T3
  	asm.emitIL(T2, BYTES_IN_QUAD);

  	if (VM.VerifyAssertions) VM._assert(allocObjectCacheInstructions != Integer.MAX_VALUE);
  	int currAddr = asm.getMachineCodeIndex() << LOG_BYTES_IN_INT;
  	asm._emitBRSL(S4, (allocObjectCacheInstructions - currAddr) >> LOG_BYTES_IN_INT);
  	
  	// pull object into local memory
  	asm.emitGET(S5, T2, T3, OBJECT_CACHE_READ_TAG_GROUP);
  	
  	// block until it has been array header is cached
  	asm.emitBlockUntilComplete(OBJECT_CACHE_READ_TAG_GROUP);
  	
  	// check how many block pointers we will require for this array
    asm.emitLoad(S0, T3, VM_ObjectModel.getArrayLengthOffset().plus(BYTES_IN_QUAD));

  	asm.emitIL(S7, 0);
  	
  	asm.emitROTMI(S3, S0, LOG_ARRAY_BLOCK_ENTRIES); // number of blocks
  	
  	// add extra block if needed
  	asm.emitANDI(S8, S0, ARRAY_BLOCK_MASK);
  	asm.emitCEQ(S5, S8, S7);
  	asm.emitAI(S8, S3, 1);   
  	asm.emitSELB(T2, S8, S3, S5);
  	
  	asm.emitSHLI(S0, T2, LOG_BYTES_IN_INT); // shift into bytes
  	
  	// align to quadword
  	asm.emitANDI(S3, S0, (BYTES_IN_QUAD - 1));
  	asm.emitCEQ(S5, S3, S7);
  	asm.emitSFI(S8, S3, BYTES_IN_QUAD);
  	asm.emitA(S3, S0, S8);   
  	asm.emitSELB(T2, S3, S0, S5);

  	if (VM.VerifyAssertions) VM._assert(allocObjectCacheInstructions != Integer.MAX_VALUE);
  	currAddr = asm.getMachineCodeIndex() << LOG_BYTES_IN_INT;
  	asm._emitBRSL(S4, (allocObjectCacheInstructions - currAddr) >> LOG_BYTES_IN_INT);
  	
  	// copy actual local memory reference to object
  	asm.emitORI(T5, T3, 0);

  	// update object cache with this entry
  	asm.emitCWD(S8, JTOC, BYTES_IN_INT);
  	asm.emitSHUFB(S5, T5, T0, S8);
  	asm.emitILW(S8, OBJECT_CACHE_TABLE);
  	asm.emitStoreDouble(S5, S8, S6);
  	
  	// zero out array block indexes
  	int zeroLoopIndex = asm.getMachineCodeIndex();
  	asm.emitAI(T2, T2, -BYTES_IN_QUAD);
  	asm.emitSTQX(S7, T5, T2);
  	asm.emitBRNZ(T2, zeroLoopIndex);
  	
  	// branch back to method
  	asm.emitAI(LINK_REG, LINK_REG, BYTES_IN_INT); // correct link register
  	asm.emitBI(LINK_REG);
  }

  private static void generateCacheArrayBlockInstructions(VM_Assembler asm) {
  	asm.emitLoad(S0, T5, VM_ObjectModel.getArrayLengthOffset());
    asm.emitORI(S9, T5, 0);  // TODO - try and remove this!!
    
    // check if we are loading the last block (if so it will be smaller than a full block)
  	asm.emitROTMI(T2, S0, (-LOG_ARRAY_BLOCK_ENTRIES));
  	asm.emitCEQ(S2, T2, S7);
  	
  	asm.emitILW(S5, ARRAY_BLOCK_ENTRIES);
  	asm.emitANDI(S6, S0, ARRAY_BLOCK_MASK);
  	
  	asm.emitSELB(T2, S5, S6, S2);  // select full block or end part block
  	asm.emitSHL(S0, T2, S8);       // shift into bytes
  	
  	// align to quadword boundary
  	asm.emitANDI(S3, S0, (BYTES_IN_QUAD - 1));
  	asm.emitCEQI(S5, S3, 0);
  	asm.emitSFI(S6, S3, BYTES_IN_QUAD);
  	asm.emitA(S3, S0, S6);   
  	asm.emitSELB(T2, S3, S0, S5);
  	
  	// allocate space for block
  	if (VM.VerifyAssertions) VM._assert(allocObjectCacheInstructions != Integer.MAX_VALUE);
  	int currAddr = asm.getMachineCodeIndex() << LOG_BYTES_IN_INT;
  	asm._emitBRSL(S4, (allocObjectCacheInstructions - currAddr) >> LOG_BYTES_IN_INT);
  	
  	// get block from main memory
  	asm.emitSHLI(S5, S7, LOG_ARRAY_BLOCK_ENTRIES);
  	asm.emitA(T0, T0, S5);
  	asm.emitGET(T0, T2, T3, OBJECT_CACHE_READ_TAG_GROUP);
  	
  	// result is T3 - update array table
  	asm.emitStore(T3, S9, S7);
  	
  	asm.emitILW(S0, (0x1 << OBJECT_CACHE_READ_TAG_GROUP));
  	asm.emitAI(LINK_REG, LINK_REG, BYTES_IN_INT);  // correct link register
  	if (VM.VerifyAssertions) VM._assert(blockUntilTagCompletesInstructions != Integer.MAX_VALUE);
  	currAddr = asm.getMachineCodeIndex() << LOG_BYTES_IN_INT;
  	asm._emitBR((blockUntilTagCompletesInstructions - currAddr) >> LOG_BYTES_IN_INT);
  }
  
  /**
   * Cache a static block (numeric or reference) in local memory
   * 
   * Trap Params: staticsAddrReg(mainMem Addr): T4, tocOffsetReg == T5
   * Trap Returns: staticsAddrReg(local Addr): T4
   */
  private static void generateCacheStaticInstructions(VM_Assembler asm) {
  	asm.emitILA(S0, STATIC_TOC_JTOC_OFF);
  	asm.emitSF(S2, S0, T5);               
  	asm.emitROTMI(S4, S2, -1);                    // get index to size table
  	
  	asm.emitILA(S0, SIZE_STATICS_TABLE);
  	asm.emitLoad(S3, S0, S4);                     // load size
  	
  	// shift size if we are looking for a ref block
  	asm.emitANDI(S0, T5, BYTES_IN_INT);
  	asm.emitCEQI(S0, S0, 0);
  	asm.emitROTMI(T3, S3, -10);                    
  	asm.emitSELB(T3, T3, S3, S0); 
  	
  	asm.emitILA(S3, (1<<10) - 1);
  	asm.emitAND(S2, T3, S3);                       // and mask
  	asm.emitSHLI(T3, S2, LOG_BYTES_IN_QUAD);
  	
  	// allocate space in the statics
  	//  TODO - implement a better allocation technique
		asm.emitLoad(S4, JTOC, staticCacheNextOff, T6);
		asm.emitLoad(S5, JTOC, staticCacheEndOff);
		
		// check if this will overflow the statics cache
		asm.emitA(S6, S4, T3);
		asm.emitCGT(S0, S6, S5);	
	
		asm.emitTRAP(S0, true, VM_TrapConstants.TRAP_STATIC_CACHE_FULL);
		// TODO - Chuck things out if the cache is full
		
  	asm.emitGET(T4, T3, S4, STATIC_CACHE_READ_TAG_GROUP);
  	
		// save the new staticsCacheNext
		asm.emitStore(S6, JTOC, staticCacheNextOff, T6);
		// update pointer to local memory address
		asm.emitORI(T4, S4, 0x0);
		
		// save the local memory value in the TOC
		asm.emitStore(T4, JTOC, T5);

  	asm.emitAI(LINK_REG, LINK_REG, BYTES_IN_ADDRESS);  // correct link reg

  	asm.emitILW(S0, (0x1 << STATIC_CACHE_READ_TAG_GROUP));
  	
  	if (VM.VerifyAssertions) VM._assert(blockUntilTagCompletesInstructions != Integer.MAX_VALUE);
  	int currAddr = asm.getMachineCodeIndex() << LOG_BYTES_IN_INT;
  	asm._emitBR((blockUntilTagCompletesInstructions - currAddr) >> LOG_BYTES_IN_INT);
  }
  
  /**
   * Cache a class TIB in local memory
   * 
   * Trap Params: tibAddrReg(mainMem Addr): T6, tocOffsetReg == T5 (if index loaded from object, T1(localMem), T4(mainMem) holds that object 
   * Trap Returns: tibAddrReg(local Addr): T6
   */
  private static void generateCacheClassTibInstructions(VM_Assembler asm) {
  	// if tibAddrReg is invalid, load the correct value 
  	asm.emitXORI(S0, T6, 0xffffffff);  	
  	VM_ForwardReference fr1 = asm.emitForwardBRZ(S0);
  	int backFromFindTib = asm.getMachineCodeIndex();
  	
  	asm.emitILA(S0, TIB_TABLE_JTOC_OFF);
  	asm.emitSF(S2, S0, T5);               // get index to table
  	
  	asm.emitILA(S0, SIZE_STATICS_TABLE);
  	asm.emitLoad(S3, S0, S2);                     // load size
  	asm.emitROTMI(S2, S3, -20);                     // and shift to method tib size
  	asm.emitSHLI(T3, S2, LOG_BYTES_IN_QUAD);
  	
  	// allocate space in the statics
  	//  TODO - implement a better allocation technique
		asm.emitLoad(S4, JTOC, classTibsCacheNextOff, T6);
		asm.emitLoad(S5, JTOC, classTibsCacheEndOff);
		
		// check if this will overflow the statics cache
		asm.emitA(S6, S4, T3);
		asm.emitCGT(S0, S6, S5);	
	
		asm.emitTRAP(S0, true, VM_TrapConstants.TRAP_CLASS_TIBS_CACHE_FULL);
		// TODO - Chuck things out if the cache is full
		
  	asm.emitGET(T6, T3, S4, STATIC_CACHE_READ_TAG_GROUP);
  	
		// save the new classTibCacheNext
		asm.emitStore(S6, JTOC, classTibsCacheNextOff, T6);
		// update pointer to local memory address
		asm.emitORI(T6, S4, 0x0);
		
		// save the local memory value in the TOC
		asm.emitStore(T6, JTOC, T5);

  	asm.emitAI(LINK_REG, LINK_REG, BYTES_IN_ADDRESS);  // correct link reg

  	asm.emitILW(S0, (0x1 << STATIC_CACHE_READ_TAG_GROUP));
  	
  	if (VM.VerifyAssertions) VM._assert(blockUntilTagCompletesInstructions != Integer.MAX_VALUE);
  	int currAddr = asm.getMachineCodeIndex() << LOG_BYTES_IN_INT;
  	asm._emitBR((blockUntilTagCompletesInstructions - currAddr) >> LOG_BYTES_IN_INT);
  	
  	fr1.resolve(asm);
  	// if we are here then the object had an invalid toc index - correct it
  	asm.emitLoadUnaligned(S4, T1, VM_JavaHeader.TIB_OFFSET);
  	asm.emitAI(S4, S4, TIB_SUBARCH_CLASS_IDX << LOG_BYTES_IN_ADDRESS);
  	asm.emitANDI(S3, S4, BYTES_IN_QUAD);
  	asm.emitILA(S2, ATOMIC_CACHE_LINE);
  	asm.emitA(S3, S2, S3);
  	asm.emitIL(S2, BYTES_IN_INT);
  	asm.emitGET(S4, S2, S3, STATIC_CACHE_READ_TAG_GROUP);
  	
  	// save the link register
  	asm.emitORI(S5, LINK_REG, 0x0);
  	asm.emitILA(S0, (0x1 << STATIC_CACHE_READ_TAG_GROUP));
  	
  	if (VM.VerifyAssertions) VM._assert(blockUntilTagCompletesInstructions != Integer.MAX_VALUE);
  	currAddr = asm.getMachineCodeIndex() << LOG_BYTES_IN_INT;
  	asm._emitBRSL(LINK_REG, (blockUntilTagCompletesInstructions - currAddr) >> LOG_BYTES_IN_INT);
  	
  	// restore the link register
  	asm.emitORI(LINK_REG, S5, 0x0);
  	
  	asm.emitLoadUnaligned(T5, S3);
  	// check if the object is actually resolved for the subarch
  	asm.emitILA(S0, VM_TIBLayoutConstants.NOT_RESOLVED_FOR_SUBARCH);
  	asm.emitCEQ(S1, T5, S0);
  	asm.emitTRAP(S1, true, VM_TrapConstants.TRAP_CLASS_NOT_RESOLVED_FOR_SUBARCH);
  	
  	// save the class toc index in the object
  	asm.emitLoadUnaligned(S6, T1, VM_JavaHeader.STATUS_OFFSET);
  	asm.emitANDI(S6, S6, ~0x1ff);
  	asm.emitROTMI(S5, T5, -LOG_BYTES_IN_ADDRESS);
  	asm.emitOR(S6, S6, S5);
  	asm.emitStore(S6, T1, VM_JavaHeader.STATUS_OFFSET);
  	
  	// save in main memory - use prepare and attempt for atomic save
  	asm.emitAI(S6, T4, VM_JavaHeader.STATUS_OFFSET.toInt());
		asm.emitANDI(S4, S6, ATOMIC_CACHE_LINE_LENTH - 1);
		asm.emitSF(T1, S4, S6);  // align to cache line
		asm.emitILA(T6, ATOMIC_CACHE_LINE);
		
		int atomicLoop = asm.getMachineCodeIndex();
		asm.emitGETLLAR(T1, T6);          // *(object+offset), setting processor's reservation address
		asm.emitATOMIC_WAIT(S0);
		
		asm.emitLoad(S0, T6, S4, S6);
		asm.emitANDI(S0, S0,  ~0x1ff);
		asm.emitOR(S0, S0, S5);
		asm.emitStore(S0, T6, S4, S6);
		
		asm.emitPUTLLC(T1, T6);
		asm.emitATOMIC_WAIT(S0);
		asm.emitBRNZ(S0, atomicLoop);

    asm.emitIOHL(T5, TIB_TABLE_JTOC_OFF);
		asm.emitLoad(T4, JTOC, T5);
		asm.emitBR(backFromFindTib);
  }
  
  private static void generateReflectiveMethodInvokerInstructions(VM_Assembler asm) {
  	//  save...
    asm.emitStore(LINK_REG, FP, Offset.fromIntSignExtend(STACKFRAME_NEXT_INSTRUCTION_OFFSET)); // ...return address
  	
    // TODO - spill area
    
    // save old frame pointer
    asm.emitStore(FP, FP, Offset.fromIntSignExtend(-VM_Memory.alignUp(STACKFRAME_HEADER_SIZE, BYTES_IN_QUAD)));     // allocate frame header and save old fp
    asm.emitAI(FP, FP, -VM_Memory.alignUp(STACKFRAME_HEADER_SIZE, BYTES_IN_QUAD));
    asm.emitILW(S4, INVISIBLE_METHOD_ID);
    asm.emitStore(S4, FP, Offset.fromIntSignExtend(STACKFRAME_METHOD_ID_OFFSET)); // set method id

    // save the method entry address
    asm.emitORI(S0, T0, 0x0);
    
    // TODO - proper parameter loads
    asm.emitORI(T0, T1, 0x0);
    asm.emitORI(T1, T2, 0x0);
    asm.emitORI(T2, T3, 0x0);
    asm.emitORI(T3, T4, 0x0);
    
    // branch and link to method
    asm.emitBISL(LINK_REG, S0);
    
    // method epilogue code
    asm.emitLoad(FP, FP);                                    // restore caller's frame
    asm.emitLoad(S0, FP, Offset.fromIntSignExtend(STACKFRAME_NEXT_INSTRUCTION_OFFSET));   // pick up return address
    asm.emitBI(S0);
  }
  
  /**
   * Generates the code necessary to handle traps
   */
  private static void generateTrapHandlerInstructions(VM_Assembler asm) {  	
  	asm.emitIL(S0, TRAP_MESSAGE);
  	asm.emitWRCH(SPU_WR_OUT_INTR_MBOX, S0);
  	asm.emitWRCH(SPU_WR_OUT_MBOX, S2); // write out the trap code
  	asm.emitRDCH(SPU_RD_IN_MBOX, S0);  // wait for instructions from ppu
  }
  
  /**
   * Generates code necessary to allocate a block of the length in T2 from the code cache
   * Code returns start of allocated block in T3.
   * 
   * @param asm VM_Assember
   */
  private static void genAllocFromObjectCacheInstructions(VM_Assembler asm) {
  	// TODO - implement a better allocation technique
  	asm.emitLoad(T3, JTOC, objectCacheNextOff, T7);
  	asm.emitLoad(T5, JTOC, objectCacheEndOff);
  	
  	// check if this will overflow the code cache
  	asm.emitA(T6, T3, T2);
  	asm.emitCGT(T5, T6, T5);	
  	
  	asm.emitTRAP(T5, true, VM_TrapConstants.TRAP_OBJECT_CACHE_FULL);
  	// TODO - Chuck things out if the cache is full
  	
  	// otherwise save the new objectCacheNext
  	asm.emitStore(T6, JTOC, objectCacheNextOff, T7);
  	
		asm.emitBI(S4);
  }

	/**
	 * Generates code necessary to allocate a block of the length in T2 from the code cache
	 * Code returns start of allocated block in T3.
	 * 
	 * @param asm VM_Assember
	 */
	private static void genAllocFromCodeCache(VM_Assembler asm) {
		// TODO - implement a better allocation technique
		asm.emitLoad(T3, JTOC, codeCacheNextOff, S6);
		asm.emitLoad(S4, JTOC, codeCacheEndOff);
		
		// check if this will overflow the code cache
		asm.emitA(S0, T3, T2);
		asm.emitCGT(S4, S0, S4);	
	
		asm.emitTRAP(S4, true, VM_TrapConstants.TRAP_CODE_CACHE_FULL);
		// TODO - Chuck things out if the cache is full
		
		// otherwise save the new codeCacheNext
		asm.emitStore(S0, JTOC, codeCacheNextOff, S6);
		
	}
}