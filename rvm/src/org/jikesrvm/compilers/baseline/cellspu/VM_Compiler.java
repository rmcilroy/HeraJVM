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
package org.jikesrvm.compilers.baseline.cellspu;

import org.jikesrvm.SubordinateArchitecture;
import org.jikesrvm.VM;
import org.jikesrvm.VM_MachineCode;
import org.jikesrvm.SubordinateArchitecture.VM_TrapConstants;
import org.jikesrvm.classloader.VM_Array;
import org.jikesrvm.classloader.VM_Atom;
import org.jikesrvm.classloader.VM_Class;
import org.jikesrvm.classloader.VM_Field;
import org.jikesrvm.classloader.VM_FieldReference;
import org.jikesrvm.classloader.VM_MemberReference;
import org.jikesrvm.classloader.VM_Method;
import org.jikesrvm.classloader.VM_MethodReference;
import org.jikesrvm.classloader.VM_NormalMethod;
import org.jikesrvm.classloader.VM_Type;
import org.jikesrvm.classloader.VM_TypeReference;
import org.jikesrvm.compilers.baseline.VM_BBConstants;
import org.jikesrvm.compilers.baseline.VM_BaselineCompiledMethod;
import org.jikesrvm.compilers.baseline.VM_BaselineCompiler;
import org.jikesrvm.compilers.common.VM_CompiledMethod;
import org.jikesrvm.compilers.common.assembler.VM_ForwardReference;
import org.jikesrvm.compilers.common.assembler.cellspu.VM_Assembler;
import org.jikesrvm.compilers.common.assembler.cellspu.VM_AssemblerConstants;
import org.jikesrvm.memorymanagers.mminterface.MM_Constants;
import org.jikesrvm.objectmodel.VM_JavaHeader;
import org.jikesrvm.objectmodel.VM_ObjectModel;
import org.jikesrvm.cellspu.VM_BaselineConstants;
import org.jikesrvm.cellspu.VM_OutOfLineMachineCode;
import org.jikesrvm.cellspu.VM_SubArchBootRecord;
import org.jikesrvm.runtime.VM_Entrypoints;
import org.jikesrvm.runtime.VM_Magic;
import org.jikesrvm.runtime.VM_MagicNames;
import org.jikesrvm.runtime.VM_Memory;
import org.jikesrvm.runtime.VM_SubArchEntrypoints;
import org.jikesrvm.runtime.VM_SubArchStatics;
import org.jikesrvm.scheduler.VM_Thread;
import org.jikesrvm.util.VM_HashSet;
import org.vmmagic.pragma.Inline;
import org.vmmagic.pragma.Uninterruptible;
import org.vmmagic.unboxed.Offset;

/**
 * VM_Compiler is the baseline compiler class for powerPC architectures.
 */
public abstract class VM_Compiler extends VM_BaselineCompiler
    implements VM_BaselineConstants, VM_BBConstants, VM_AssemblerConstants {

  // stackframe pseudo-constants //
  private int frameSize;
  private int spillOffset;
  private final int emptyStackOffset;
  private int localStartOffset;
  private int operandStackStartOffset;
  private int localSpills = 0;

  // current offset of the sp from fp
  public int spTopOffset;

  // If we're doing a short forward jump of less than
  // this number of bytecodes, then we can always use a short-form
  // branch (don't have to branch indirectly from a register).
  private static final int SHORT_FORWARD_LIMIT = 500;

  private static final boolean USE_NONVOLATILE_REGISTERS = true;

  private int firstFixedStackRegister; //after the fixed local registers !!

  private int lastFixedStackRegister;

  private int firstLongFixedStackRegister; //after the fixed local registers !!

  private int lastLongFixedStackRegister;
  
  private final int[] localFixedLocations;

  private final boolean use_nonvolatile_registers;
  
  private final SubordinateArchitecture.VM_Assembler asm;

  /**
   * Create a VM_Compiler object for the compilation of method.
   */
  protected VM_Compiler(VM_BaselineCompiledMethod cm, int[] genLocLoc) {
    super(cm);
    
    asm = (SubordinateArchitecture.VM_Assembler) abstractAsm;
    localFixedLocations = genLocLoc;
    
    shouldPrint = true;
    
    use_nonvolatile_registers = true;

    if (VM.VerifyAssertions) VM._assert(T6 <= LAST_VOLATILE_GPR); // need 6 gp temps
    if (VM.VerifyAssertions) VM._assert(S3 <= LAST_SCRATCH_GPR);  // need 3 scratch
    stackHeights = new int[bcodes.length()];

  	emptyStackOffset = getEmptyStackOffset(method);
  }

  @Override
  protected void initializeCompiler() {
    defineStackAndLocalLocations();     //alters framesize, this can only be performed after localTypes are filled in by buildReferenceMaps
  	
    frameSize = getInternalFrameSize(); //after defineStackAndLocalLocations !!
  }

  //----------------//
  // more interface //
  //----------------//

  // position of operand stack within method's stackframe.

  @Uninterruptible
  public static int getEmptyStackOffset(VM_NormalMethod m) {
  	// TODO - get a better method of working out parameter spills
    int params = m.getOperandWords() << LOG_BYTES_IN_STACKSLOT; // maximum parameter area
    int spill = params - (MIN_PARAM_REGISTERS << LOG_BYTES_IN_STACKSLOT);
    if (spill < 0) spill = 0;
    return STACKFRAME_HEADER_SIZE + spill;
  }
  
  // start position of locals within method's stackframe.
  @Uninterruptible
  private int getInternalStartLocalOffset() {
    int locals = localSpills << LOG_BYTES_IN_STACKSLOT;       // input param words + pure locals
    // TODO - Don't align this up once we do stack in registers (THINK ABOUT IT)
    return VM_Memory.alignUp((emptyStackOffset + locals), STACKFRAME_ALIGNMENT); // bottom-most local
  }

  @Uninterruptible
  private int getOperandStackOffset() {
  	int locals = localStartOffset = getInternalStartLocalOffset();
    int stack = method.getOperandWords() << LOG_BYTES_IN_STACKSLOT; // maximum stack size
    // TODO - Don't align this up once we do stack in registers (THINK ABOUT IT)
    return VM_Memory.alignUp((locals + stack), STACKFRAME_ALIGNMENT); // bottom-most local  
  }

  // size of method's stackframe.
  @Uninterruptible
  private int getInternalFrameSize() {
    int size = operandStackStartOffset = getOperandStackOffset();

    if (method.getDeclaringClass().hasDynamicBridgeAnnotation()) {
      size += (LAST_NONVOLATILE_GPR - FIRST_VOLATILE_GPR + 1) << LOG_BYTES_IN_ADDRESS;
    } else {
      size += (lastFixedStackRegister - FIRST_FIXED_LOCAL_REGISTER + 1) << LOG_BYTES_IN_ADDRESS;
      size += (lastLongFixedStackRegister - FIRST_FIXED_LONG_LOCAL_REGISTER + 1) << LOG_BYTES_IN_LONG;
    }
    size = VM_Memory.alignUp(size, STACKFRAME_ALIGNMENT);
    return size;
  }

//  // size of method's stackframe.
//  // only valid on compiled methods
//  @Uninterruptible
//  public static int getFrameSize(VM_BaselineCompiledMethod bcm) {
//    VM_NormalMethod m = (VM_NormalMethod) bcm.getMethod();
//    int size = getInternalStartLocalOffset(m);
//    if (m.getDeclaringClass().hasDynamicBridgeAnnotation()) {
//      size += (LAST_NONVOLATILE_GPR - FIRST_VOLATILE_GPR + 1) << LOG_BYTES_IN_ADDRESS;
//    } else {
//      int num_gpr = bcm.getLastFixedStackRegister() - FIRST_FIXED_LOCAL_REGISTER + 1;
//      size += (num_gpr << LOG_BYTES_IN_ADDRESS);
//    }
//    size = VM_Memory.alignUp(size, STACKFRAME_ALIGNMENT);
//    return size;
//  }

  private void defineStackAndLocalLocations() {

    int nextFixedLocalRegister = FIRST_FIXED_LOCAL_REGISTER;
    int nextFixedLongLocalRegister = FIRST_FIXED_LONG_LOCAL_REGISTER;

    //define local registers
    VM_TypeReference[] types = method.getParameterTypes();
    int localIndex = 0;
    if (!method.isStatic()) {
      if (localTypes[0] != ADDRESS_TYPE) VM._assert(false);
      if (!use_nonvolatile_registers || (nextFixedLocalRegister > LAST_FIXED_LOCAL_REGISTER)) {
        localFixedLocations[localIndex] = offsetToLocation(localOffset(localSpills));
        localSpills++;
      } else {
        localFixedLocations[localIndex] = nextFixedLocalRegister++;
      }
      localIndex++;
    }
    for (int i = 0; i < types.length; i++, localIndex++) {
    	
      // all types should fit in a single QuadWord register, seperate so we can store in memory correctly
    	if (types[i].isDoubleType() || types[i].isLongType()) {
    	   if (!use_nonvolatile_registers || (nextFixedLongLocalRegister > LAST_FIXED_LONG_LOCAL_REGISTER)) {
 	      	localFixedLocations[localIndex] = offsetToLocation(localOffset(localSpills));
 	      	localSpills+=2;
 	      } else {
 	      	localFixedLocations[localIndex] = nextFixedLongLocalRegister++;
 	      }
    	  localIndex++;
    	} else {
	      if (!use_nonvolatile_registers || (nextFixedLocalRegister > LAST_FIXED_LOCAL_REGISTER)) {
	      	localFixedLocations[localIndex] = offsetToLocation(localOffset(localSpills));
	      	localSpills++;
	      } else {
	      	localFixedLocations[localIndex] = nextFixedLocalRegister++;
	      }
    	}
    }

    //rest of locals, non parameters, could be reused for different types
    int nLocalWords = method.getLocalWords();
    for (; localIndex < nLocalWords; localIndex++) {
      byte currentLocal = localTypes[localIndex];

      if (currentLocal != VOID_TYPE) { //object, float, double or intlike
      	if (currentLocal == LONG_TYPE || currentLocal == DOUBLE_TYPE) {
	      	if (!use_nonvolatile_registers || (nextFixedLongLocalRegister > LAST_FIXED_LONG_LOCAL_REGISTER)) {
	          localFixedLocations[localIndex] = offsetToLocation(localOffset(localSpills));
	          localSpills+=2;
	      	} else {
	          localFixedLocations[localIndex] = nextFixedLongLocalRegister++;
	        }
      	} else {
      		if (!use_nonvolatile_registers || (nextFixedLocalRegister > LAST_FIXED_LOCAL_REGISTER)) {
	          localFixedLocations[localIndex] = offsetToLocation(localOffset(localSpills));
	          localSpills++;
      		} else {
	          localFixedLocations[localIndex] = nextFixedLocalRegister++;
	        }
      	}
      }
    }

    firstFixedStackRegister = nextFixedLocalRegister;
    firstLongFixedStackRegister = nextFixedLongLocalRegister;

    //define stack registers
    //KV: TODO
    lastFixedStackRegister = firstFixedStackRegister - 1;
    lastLongFixedStackRegister = firstLongFixedStackRegister - 1;
  }

  public final int getLastFixedStackRegister() {
    return lastFixedStackRegister;
  }
  
  public final int getLastFloatStackRegister() {
    return 0;
  }

  private int getGeneralLocalLocation(int index) {
    return localFixedLocations[index];
  }
  
  @Uninterruptible
  @Inline
  public static int getGeneralLocalLocation(int index, int[] localloc, VM_NormalMethod m) {
    return localloc[index];
  }

  @Uninterruptible
  @Inline
  public static int getFloatLocalLocation(int index, int[] localloc, VM_NormalMethod m) {
    return localloc[index];
  }

  /**
   * About to start generating code for bytecode biStart.
   * Perform any platform specific setup
   */
  @Override
  protected final void starting_bytecode() {
  	// TODO -see if something is needed here?
  }

  /**
   * Emit the prologue for the method
   */
  @Override
  protected final void emit_prologue() {
    spTopOffset = emptyStackOffset;
    genPrologue();
  }

  /**
   * Emit the code for a threadswitch tests (aka a yieldpoint).
   * @param whereFrom is this thread switch from a PROLOGUE, BACKEDGE, or EPILOGUE?
   */
  @Override
  protected final void emit_threadSwitchTest(int whereFrom) {
    genThreadSwitchTest(whereFrom);
  }

  /**
   * Emit the code to implement the spcified magic.
   * @param magicMethod desired magic
   */
  @Override
  protected final boolean emit_Magic(VM_MethodReference magicMethod) {
    return generateInlineCode(magicMethod);
  }

  /*
   * Helper functions for expression stack manipulation
   */
  private void discardSlot() {
  	discardSlots(1);
  }

  private  void discardSlots(int n) {
  	if (n == 0) return;
    int spTopOffsetNew = spTopOffset + (n * BYTES_IN_STACKSLOT);
    if ((spTopOffsetNew != operandStackStartOffset) && ((spTopOffsetNew & ~(BYTES_IN_QUAD - 1)) != (spTopOffset & ~(BYTES_IN_QUAD - 1))) ||
    		(((spTopOffsetNew - BYTES_IN_STACKSLOT) & (STACKFRAME_ALIGNMENT - 1)) == (3 * BYTES_IN_STACKSLOT))) {
    	asm.emitLQD(STACK_TOP_TEMP, FP, (spTopOffsetNew) >> 4);
    }
    spTopOffset = spTopOffsetNew;
  }

  // FIXME: Make pops and peeks clear out rest of register
  // TODO:  Use static registers to store control quadwords for shuffles
  
  /**
   * Emit the code to place long value from stackIdx of expression stack (top temp reg)
   * into reg
   * 
   * Warning, trashes S2 and S3, and if isPop is false - S1
   * 
   * @param reg register to pop the value into 
   * @param stackIdx index into the stack from FP in bytes
   * @param isPop whether element is being poped, and therefore STACK_TOP_TEMP should be modified
   */
	private void peekLongStackTop(int reg, int stackIdx, boolean isPop) {
		int reg_offset = stackIdx & (STACKFRAME_ALIGNMENT - 1);
  	if (reg_offset == 0) {
  		asm.emitORI(reg, STACK_TOP_TEMP, 0);
  	} else if (reg_offset == BYTES_IN_STACKSLOT) {
  		asm.emitSHLQBYI(reg, STACK_TOP_TEMP, 4);
  	} else if (reg_offset == (2 * BYTES_IN_STACKSLOT)) {
  		asm.emitSHLQBYI(reg, STACK_TOP_TEMP, 8);
  		if (isPop && (spTopOffset + BYTES_IN_LONG) != operandStackStartOffset) {
  			asm.emitLQD(STACK_TOP_TEMP, FP, (stackIdx + BYTES_IN_LONG) >> 4);
  		}
  	} else { // if (reg_offset == 3 * BYTES_IN_STACKSLOT) {
			int loadReg = (isPop) ? STACK_TOP_TEMP : S1;
  		asm.emitSHLQBYI(S2, STACK_TOP_TEMP, 12);
  		asm.emitLQD(loadReg, FP, (stackIdx + BYTES_IN_INT) >> 4);
  		asm.emitCWD(S3, FP, BYTES_IN_INT);
  		asm.emitSHUFB(reg, loadReg, S2, S3);
  	}
	}

  /**
   * Emit the code to place long value in reg int stackIdx of expression stack (top temp reg)
   * 
   * Warning, trashes S2 and S3, and if isPop is false - S1
   * 
   * @param reg register to pop the value into 
   * @param stackIdx index into the stack from FP in bytes
   * @param isPop whether element is being poped, and therefore STACK_TOP_TEMP should be advanced
   */
	private void pokeLongStackTop(int reg, int stackIdx, boolean isPop) {
		int reg_offset = stackIdx & (STACKFRAME_ALIGNMENT - 1);
		if (reg_offset == 0) {
  		asm.emitCDD(S3, FP, 0);
  		asm.emitSHUFB(STACK_TOP_TEMP, reg, STACK_TOP_TEMP, S3);
		} else if (reg_offset == BYTES_IN_STACKSLOT) {
  		// TODO : Think about optomising this by using a constant shufflebyte value
  		asm.emitROTQBYI(STACK_TOP_TEMP, STACK_TOP_TEMP, 4);  // align stack-top-temp with reg
  		asm.emitCDD(S3, FP, 0);
  		asm.emitSHUFB(STACK_TOP_TEMP, reg, STACK_TOP_TEMP, S3);
  		asm.emitROTQBYI(STACK_TOP_TEMP, STACK_TOP_TEMP, 12); // rotate back
  	} else if (reg_offset == (2 * BYTES_IN_STACKSLOT)) {
  		if (isPop) {
  			if ((spTopOffset + BYTES_IN_LONG) != operandStackStartOffset) {
  				asm.emitSTQD(STACK_TOP_TEMP, FP, (stackIdx + BYTES_IN_LONG) >> 4);	
  			}
  			asm.emitROTQBYI(STACK_TOP_TEMP, reg, 8);  // rotate bottom two slots to top-most slot
  		} else {
  			asm.emitCDD(S3, FP, BYTES_IN_LONG);
  			asm.emitSHUFB(STACK_TOP_TEMP, reg, STACK_TOP_TEMP, S3);
  		}
  	} else { // if (reg_offset == 3 * BYTES_IN_STACKSLOT) {
  		if (isPop) {
  			// TODO : Really think about optomising this by using a constant shufflebyte value
  			asm.emitROTQBYI(S2, reg, 4); // rotate to correct alignment
  			asm.emitCWD(S3, FP, 0);
  			asm.emitSHUFB(STACK_TOP_TEMP, S2, STACK_TOP_TEMP, S3);
  			asm.emitSTQD(STACK_TOP_TEMP, FP, (stackIdx + BYTES_IN_INT) >> 4);
  			asm.emitORI(STACK_TOP_TEMP, S2, 0);
  		} else {
  			// TODO : This is horrid, see if it can be improved
  			// prepare previous stack Quad load
  	  	asm.emitLQD(S2, FP, ((stackIdx + BYTES_IN_INT) >> 4));
  	  	// insert hi-word into STACK_TOP_TEMP
  	  	asm.emitCWD(S3, FP, stackIdx);
  	  	asm.emitSHUFB(STACK_TOP_TEMP, reg, STACK_TOP_TEMP, S3);
  	  	// insert low-word into previous stack Quad
  	  	asm.emitCWD(S3, FP, (stackIdx + BYTES_IN_INT));
  	  	asm.emitROTQBYI(S1, reg, BYTES_IN_INT);
  	  	asm.emitSHUFB(S2, S1, S2, S3);
  	  	asm.emitSTQD(S2, FP, ((stackIdx + BYTES_IN_INT) >> 4));
  		}
  	}
	}
	
  /**
   * Emit the code to push a word value
   * contained in 'reg' onto the expression stack
   * @param reg register containing the value to push
   */
  private void pushWord(int reg) {
  	spTopOffset -= BYTES_IN_STACKSLOT;
  	if ((spTopOffset & (STACKFRAME_ALIGNMENT - 1)) != (3 * BYTES_IN_STACKSLOT)) {
  		// shuffle into top temp word
  		asm.emitCWD(S3, FP, spTopOffset);
  		asm.emitSHUFB(STACK_TOP_TEMP, reg, STACK_TOP_TEMP, S3);
  	} else {
  		if ((spTopOffset + BYTES_IN_STACKSLOT) != operandStackStartOffset) {
  			asm.emitSTQD(STACK_TOP_TEMP, FP, (spTopOffset + BYTES_IN_INT >> 4));
  		}
  		asm.emitROTQBYI(STACK_TOP_TEMP, reg, 4);  // rotate preferred slot to top-most slot
  	}
  }

  /**
   * Emit the code to push a long value
   * contained in reg onto the expression stack
   * @param reg register containing the value to push
   */
  private void pushLong(int reg) {
  	spTopOffset -= 2 * BYTES_IN_STACKSLOT;
  	pokeLongStackTop(reg, spTopOffset, true);
  }
  
  private void pokeWordIdxInBytes(int reg, int idxBytes) {
  	int inStackTopTemp = STACKFRAME_ALIGNMENT - (spTopOffset & (STACKFRAME_ALIGNMENT - 1));
   	if (inStackTopTemp >= idxBytes) {
   		asm.emitCWD(S3, FP, spTopOffset + idxBytes);
   		asm.emitSHUFB(STACK_TOP_TEMP, reg, STACK_TOP_TEMP, S3);
   	} else {
   		asm.emitStore(reg, FP, Offset.fromIntSignExtend(spTopOffset + idxBytes));
   	}
  }

  /**
   * Emit the code to poke an int
   * contained in 'reg' onto the expression stack on position idx.
   * @param reg register to peek the value into
   */
  private void pokeWord(int reg, int idx) {
  	int idxBytes = (idx << LOG_BYTES_IN_STACKSLOT);
  	pokeWordIdxInBytes(reg, idxBytes);
  }

	private void pokeLongIdxInBytes(int reg, int idxBytes) {
		int inStackTopTemp = STACKFRAME_ALIGNMENT - (spTopOffset & (STACKFRAME_ALIGNMENT - 1));
   	if (inStackTopTemp >= idxBytes) {
   		pokeLongStackTop(reg, spTopOffset + idxBytes, false);
   	} else {
   		asm.emitStoreDouble(reg, FP, Offset.fromIntSignExtend(spTopOffset + idxBytes));
   	}
	}
	
  /**
   * Emit the code to peek a long value
   * from the expression stack into 'reg' 
   * @param reg register to peek
   * @param idx register holding the index offset from the stack to to be peaked
   */
  private void pokeLong(int reg, int idx) {
  	int idxBytes = (idx << LOG_BYTES_IN_STACKSLOT);
  	pokeLongIdxInBytes(reg, idxBytes);
  }

  /**
   * Emit the code to pop an word size (boolean, byte, char, short, int) value
   * from the expression stack into the register 'reg'.
   * @param reg register to pop the value into
   */
  private void popWord(int reg) {
  	// rotate into top temp word
		asm.emitROTQBYI(reg, STACK_TOP_TEMP, spTopOffset);
		if (((spTopOffset & (STACKFRAME_ALIGNMENT - 1)) == (3 * BYTES_IN_STACKSLOT)) && ((spTopOffset + BYTES_IN_STACKSLOT) != operandStackStartOffset)) {
			asm.emitLQD(STACK_TOP_TEMP, FP, (spTopOffset + BYTES_IN_INT) >> 4);
  	}
  	spTopOffset += BYTES_IN_STACKSLOT;
  }

  /**
   * Emit the code to pop a long value
   * contained on the expression stack into reg 
   * @param reg register to pop the value into 
   */
  private void popLong(int reg) {
  	peekLongStackTop(reg, spTopOffset, true);
  	spTopOffset += 2 * BYTES_IN_STACKSLOT;
  }

	private void peekWordIdxInBytes(int reg, int idxBytes) {
		int inStackTopTemp = STACKFRAME_ALIGNMENT - (spTopOffset & (STACKFRAME_ALIGNMENT - 1));
   	if (inStackTopTemp > idxBytes) {
  		asm.emitROTQBYI(reg, STACK_TOP_TEMP, spTopOffset + idxBytes);
   	} else {
   		asm.emitLoad(reg, FP, Offset.fromIntSignExtend(spTopOffset + idxBytes));
   	}
	}
	
  /**
   * Emit the code to peek an intlike (boolean, byte, char, short, int) value
   * from the expression stack into the register 'reg'.
   * @param reg register to peek the value into
   * @param idx register holding the index offset from the stack to to be peaked
   */
  final void peekWord(int reg, int idx) {
  	int idxBytes = (idx << LOG_BYTES_IN_STACKSLOT);
  	peekWordIdxInBytes(reg, idxBytes);
  }

	private void peekLongIdxInBytes(int reg, int idxBytes) {
		int inStackTopTemp = STACKFRAME_ALIGNMENT - (spTopOffset & (STACKFRAME_ALIGNMENT - 1));
   	if (inStackTopTemp >= idxBytes) {
   		peekLongStackTop(reg, spTopOffset + idxBytes, false);
   	} else {
   		asm.emitLoadDouble(reg, FP, Offset.fromIntSignExtend(spTopOffset + idxBytes));
   	}
	}
	
  /**
   * Emit the code to peek a long value
   * from the expression stack into 'reg' 
   * @param reg register to peek
   * @param idx register holding the index offset from the stack to to be peaked
   */
  private void peekLong(int reg, int idx) {
  	int idxBytes = (idx << LOG_BYTES_IN_STACKSLOT);
  	peekLongIdxInBytes(reg, idxBytes);
  }

  /*
  * Loading constants
  */

  /**
   * Emit code to load the null constant.
   */
  @Override
  protected final void emit_aconst_null() {
    asm.emitIL(T0, 0);
    pushWord(T0);
  }

  /**
   * Emit code to load an int constant.
   * @param val the int constant to load
   */
  @Override
  protected final void emit_iconst(int val) {
  	asm.emitILW(T0, val);
  	pushWord(T0);
  }

  /**
   * Emit code to load a long constant
   * @param val the lower 32 bits of long constant (upper32 are 0).
   */
  @Override
  protected final void emit_lconst(int val) {
  	asm.emitILW(T1, val);
  	asm.emitROTQMBYI(T0, T1, BYTES_IN_INT);
  	pushLong(T0);
  }

  /**
   * Emit code to load 0.0f
   */
  @Override
  protected final void emit_fconst_0() {
  	asm.emitLoad(T0, JTOC, VM_Entrypoints.zeroFloatField.getSubArchOffset());
  	pushWord(T0);
  }

  /**
   * Emit code to load 1.0f
   */
  @Override
  protected final void emit_fconst_1() {
  	asm.emitLoad(T0, JTOC, VM_Entrypoints.oneFloatField.getSubArchOffset());
  	pushWord(T0);
  }

  /**
   * Emit code to load 2.0f
   */
  @Override
  protected final void emit_fconst_2() {
  	asm.emitLoad(T0, JTOC, VM_Entrypoints.twoFloatField.getSubArchOffset());
  	pushWord(T0);
  }

  /**
   * Emit code to load 0.0d
   */
  @Override
  protected final void emit_dconst_0() {
  	asm.emitLoadDouble(T0, JTOC, VM_Entrypoints.zeroDoubleField.getSubArchOffset());
  	pushLong(T0);
  }

  /**
   * Emit code to load 1.0d
   */
  @Override
  protected final void emit_dconst_1() {
  	asm.emitLoadDouble(T0, JTOC, VM_Entrypoints.oneDoubleField.getSubArchOffset());
  	pushLong(T0);
  }

  /**
   * Emit code to load a 32 bit constant
   * @param offset JTOC offset of the constant
   * @param type the type of the constant
   */
  @Override
  protected final void emit_ldc(Offset offset, byte type) {
  	asm.emitLoad(T0, JTOC, offset);
    pushWord(T0);
  }

  /**
   * Emit code to load a 64 bit constant
   * @param offset JTOC offset of the constant
   * @param type the type of the constant
   */
  @Override
  protected final void emit_ldc2(Offset offset, byte type) {
  	asm.emitLoadDouble(T0, JTOC, offset);
    pushLong(T0);
  }

  /*
  * loading local variables
  */

  /**
   * Emit code to load an int local variable
   * @param index the local index to load
   */
  @Override
  protected final void emit_iload(int index) {
    copyByLocation(false, getGeneralLocalLocation(index), T0);
  	pushWord(T0);
  }

  /**
   * Emit code to load a long local variable
   * @param index the local index to load
   */
  @Override
  protected final void emit_lload(int index) {
    copyByLocation(true, getGeneralLocalLocation(index), T0);
  	pushLong(T0);
  }

  /**
   * Emit code to local a float local variable
   * @param index the local index to load
   */
  @Override
  protected final void emit_fload(int index) {
    copyByLocation(false, getGeneralLocalLocation(index), T0);
  	pushWord(T0);
  }

  /**
   * Emit code to load a double local variable
   * @param index the local index to load
   */
  @Override
  protected final void emit_dload(int index) {
    copyByLocation(true, getGeneralLocalLocation(index), T0);
  	pushLong(T0);
  }

  /**
   * Emit code to load a reference local variable
   * @param index the local index to load
   */
  @Override
  protected final void emit_aload(int index) {
    copyByLocation(false, getGeneralLocalLocation(index), T0);
  	pushWord(T0);
  }

  /*
  * storing local variables
  */

  /**
   * Emit code to store an int to a local variable
   * @param index the local index to load
   */
  @Override
  protected final void emit_istore(int index) {
  	popWord(T0);
    copyByLocation(false, T0, getGeneralLocalLocation(index));
  }

  /**
   * Emit code to store a long to a local variable
   * @param index the local index to load
   */
  @Override
  protected final void emit_lstore(int index) {
  	popLong(T0);
    copyByLocation(true, T0, getGeneralLocalLocation(index));
  }

  /**
   * Emit code to store a float to a local variable
   * @param index the local index to load
   */
  @Override
  protected final void emit_fstore(int index) {
  	popWord(T0);
    copyByLocation(false, T0, getGeneralLocalLocation(index));
  }

  /**
   * Emit code to store an double  to a local variable
   * @param index the local index to load
   */
  @Override
  protected final void emit_dstore(int index) {
  	popLong(T0);
    copyByLocation(true, T0, getGeneralLocalLocation(index));
  }

  /**
   * Emit code to store a reference to a local variable
   * @param index the local index to load
   */
  @Override
  protected final void emit_astore(int index) {
  	popWord(T0);
    copyByLocation(false, T0, getGeneralLocalLocation(index));
  }

  /*
  * array loads
  */

  /**
   * Emit code to load from an int array
   */
  @Override
  protected final void emit_iaload() {
    popWord(T1);      // T1 is array index
    popWord(T0);      // T0 is array ref

    asm.emitNullCheck(T0);   // must do this explicitly on SPU
    
  	genArrayCacheLookup(T3, T0, T1, LOG_BYTES_IN_INT);
  	
  	asm.emitANDI(T0, T1, ARRAY_BLOCK_MASK);  // mask to block index
  	asm.emitSHLI(T0, T0, LOG_BYTES_IN_INT);
    asm.emitLoad(T6, T3, T0);
    pushWord(T6);
  }

  /**
   * Emit code to load from a long array
   */
  @Override
  protected final void emit_laload() {
    popWord(T1);      // T1 is array index
    popWord(T0);      // T0 is array ref

    asm.emitNullCheck(T0);   // must do this explicitly on SPU
    
  	genArrayCacheLookup(T3, T0, T1, LOG_BYTES_IN_LONG);
  	
  	asm.emitANDI(T0, T1, ARRAY_BLOCK_MASK);  // mask to block index
  	asm.emitSHLI(T0, T0, LOG_BYTES_IN_DOUBLE);
    asm.emitLoadDouble(T6, T3, T0);
    pushLong(T6);
  }

  /**
   * Emit code to load from a float array
   */
  @Override
  protected final void emit_faload() {
    popWord(T1);      // T1 is array index
    popWord(T0);      // T0 is array ref

    asm.emitNullCheck(T0);   // must do this explicitly on SPU
    
  	genArrayCacheLookup(T3, T0, T1, LOG_BYTES_IN_FLOAT);
  	
  	asm.emitANDI(T0, T1, ARRAY_BLOCK_MASK);  // mask to block index
  	asm.emitSHLI(T0, T0, LOG_BYTES_IN_FLOAT);
    asm.emitLoad(T6, T3, T0);
    pushWord(T6);
  }

  /**
   * Emit code to load from a double array
   */
  @Override
  protected final void emit_daload() {
    popWord(T1);      // T1 is array index
    popWord(T0);      // T0 is array ref

    asm.emitNullCheck(T0);   // must do this explicitly on SPU
    
  	genArrayCacheLookup(T3, T0, T1, LOG_BYTES_IN_DOUBLE);
  	
  	asm.emitANDI(T0, T1, ARRAY_BLOCK_MASK);  // mask to block index
  	asm.emitSHLI(T0, T0, LOG_BYTES_IN_DOUBLE);
    asm.emitLoadDouble(T6, T3, T0);
    pushLong(T6);
  }

  /**
   * Emit code to load from a reference array
   */
  @Override
  protected final void emit_aaload() {
    popWord(T1);      // T1 is array index
    popWord(T0);      // T0 is array ref

    asm.emitNullCheck(T0);   // must do this explicitly on SPU
    
  	genArrayCacheLookup(T3, T0, T1, LOG_BYTES_IN_ADDRESS);
  	
  	asm.emitANDI(T0, T1, ARRAY_BLOCK_MASK);  // mask to block index
  	asm.emitSHLI(T0, T0, LOG_BYTES_IN_ADDRESS);
    asm.emitLoad(T6, T3, T0);
    pushWord(T6);
  }

  /**
   * Emit code to load from a byte/boolean array
   */
  @Override
  protected final void emit_baload() {
    popWord(T1);      // T1 is array index
    popWord(T0);      // T0 is array ref

    asm.emitNullCheck(T0);   // must do this explicitly on SPU
    
  	genArrayCacheLookup(T3, T0, T1, LOG_BYTES_IN_BYTE);
  	
  	asm.emitANDI(T0, T1, ARRAY_BLOCK_MASK);  // mask to block index
  	asm.emitSHLI(T0, T0, LOG_BYTES_IN_BYTE);
    asm.emitLoadByte(T6, T3, T0);
    pushWord(T6);
  }

  /**
   * Emit code to load from a char array
   */
  @Override
  protected final void emit_caload() {
    popWord(T1);      // T1 is array index
    popWord(T0);      // T0 is array ref

    asm.emitNullCheck(T0);   // must do this explicitly on SPU
    
  	genArrayCacheLookup(T3, T0, T1, LOG_BYTES_IN_CHAR);
  	
  	asm.emitANDI(T0, T1, ARRAY_BLOCK_MASK);  // mask to block index
  	asm.emitSHLI(T0, T0, LOG_BYTES_IN_CHAR);
    asm.emitLoadChar(T6, T3, T0);
    pushWord(T6);
  }

  /**
   * Emit code to load from a short array
   */
  @Override
  protected final void emit_saload() {
    popWord(T1);      // T1 is array index
    popWord(T0);      // T0 is array ref

    asm.emitNullCheck(T0);   // must do this explicitly on SPU
    
  	genArrayCacheLookup(T3, T0, T1, LOG_BYTES_IN_SHORT);
  	
  	asm.emitANDI(T0, T1, ARRAY_BLOCK_MASK);  // mask to block index
  	asm.emitSHLI(T0, T0, LOG_BYTES_IN_SHORT);
    asm.emitLoadShort(T6, T3, T0);
    pushWord(T6);
  }

  /*
  * array stores
  */

  /**
   * Emit code to store to an int array
   */
  @Override
  protected final void emit_iastore() {
  	popWord(T6);      // T6 is value to store
    popWord(T1);      // T1 is array index
    popWord(T0);      // T0 is array ref

    asm.emitNullCheck(T0);   // must do this explicitly on SPU
    
  	genArrayCacheLookup(T3, T0, T1, LOG_BYTES_IN_INT);
  	
  	asm.emitANDI(T0, T1, ARRAY_BLOCK_MASK);  // mask to block index
  	asm.emitSHLI(T0, T0, LOG_BYTES_IN_INT);
    asm.emitStore(T6, T3, T0);
  }

  /**
   * Emit code to store to a long array
   */
  @Override
  protected final void emit_lastore() {
  	popLong(T6);      // T6 is value to store
    popWord(T1);      // T1 is array index
    popWord(T0);      // T0 is array ref

    asm.emitNullCheck(T0);   // must do this explicitly on SPU
    
  	genArrayCacheLookup(T3, T0, T1, LOG_BYTES_IN_LONG);
  	
  	asm.emitANDI(T0, T1, ARRAY_BLOCK_MASK);  // mask to block index
  	asm.emitSHLI(T0, T0, LOG_BYTES_IN_LONG);
    asm.emitStoreDouble(T6, T3, T0);
  }

  /**
   * Emit code to store to a float array
   */
  @Override
  protected final void emit_fastore() {
  	popWord(T6);      // T6 is value to store
    popWord(T1);      // T1 is array index
    popWord(T0);      // T0 is array ref

    asm.emitNullCheck(T0);   // must do this explicitly on SPU
    
  	genArrayCacheLookup(T3, T0, T1, LOG_BYTES_IN_FLOAT);
  	
  	asm.emitANDI(T0, T1, ARRAY_BLOCK_MASK);  // mask to block index
  	asm.emitSHLI(T0, T0, LOG_BYTES_IN_FLOAT);
    asm.emitStore(T6, T3, T0);
  }

  /**
   * Emit code to store to a double array
   */
  @Override
  protected final void emit_dastore() {
  	popLong(T6);      // T6 is value to store
    popWord(T1);      // T1 is array index
    popWord(T0);      // T0 is array ref

    asm.emitNullCheck(T0);   // must do this explicitly on SPU
    
  	genArrayCacheLookup(T3, T0, T1, LOG_BYTES_IN_DOUBLE);
  	
  	asm.emitANDI(T0, T1, ARRAY_BLOCK_MASK);  // mask to block index
  	asm.emitSHLI(T0, T0, LOG_BYTES_IN_DOUBLE);
    asm.emitStoreDouble(T6, T3, T0);
  }

  /**
   * Emit code to store to a reference array
   */
  @Override
  protected final void emit_aastore() {
  	// do a class cast check first
    asm.emitLoad(T0, JTOC, VM_Entrypoints.checkstoreMethod.getSubArchOffset());
    // TODO : add hint for branch
    peekWord(T1, 0);    // T1 is value to store
    peekWord(T0, 2);    // T0 is array ref
    asm.emitBISL(LINK_REG, T0);   // checkstore(arrayref, value)

  	popWord(T6);      // T6 is value to store
    popWord(T1);      // T1 is array index
    popWord(T0);      // T0 is array ref

    asm.emitNullCheck(T0);   // must do this explicitly on SPU
    
  	genArrayCacheLookup(T3, T0, T1, LOG_BYTES_IN_ADDRESS);
    
    if (MM_Constants.NEEDS_WRITE_BARRIER) {
      VM._assert(NOT_REACHED);
    } else {
    	asm.emitANDI(T0, T1, ARRAY_BLOCK_MASK);  // mask to block index
    	asm.emitSHLI(T0, T0, LOG_BYTES_IN_ADDRESS);
      asm.emitStore(T6, T3, T0);
    }
  }

  /**
   * Emit code to store to a byte/boolean array
   */
  @Override
  protected final void emit_bastore() {
  	popWord(T6);      // T6 is value to store
    popWord(T1);      // T1 is array index
    popWord(T0);      // T0 is array ref

    asm.emitNullCheck(T0);   // must do this explicitly on SPU
    
  	genArrayCacheLookup(T3, T0, T1, LOG_BYTES_IN_BYTE);
  	
  	asm.emitANDI(T0, T1, ARRAY_BLOCK_MASK);  // mask to block index
  	asm.emitSHLI(T0, T0, LOG_BYTES_IN_BYTE);
    asm.emitStoreByte(T6, T3, T0);
  }

  /**
   * Emit code to store to a char array
   */
  @Override
  protected final void emit_castore() {
  	popWord(T6);      // T6 is value to store
    popWord(T1);      // T1 is array index
    popWord(T0);      // T0 is array ref

    asm.emitNullCheck(T0);   // must do this explicitly on SPU
    
  	genArrayCacheLookup(T3, T0, T1, LOG_BYTES_IN_CHAR);
  	
  	asm.emitANDI(T0, T1, ARRAY_BLOCK_MASK);  // mask to block index
  	asm.emitSHLI(T0, T0, LOG_BYTES_IN_CHAR);
    asm.emitStoreByte(T6, T3, T0);
  }

  /**
   * Emit code to store to a short array
   */
  @Override
  protected final void emit_sastore() {
  	popWord(T6);      // T6 is value to store
    popWord(T1);      // T1 is array index
    popWord(T0);      // T0 is array ref

    asm.emitNullCheck(T0);   // must do this explicitly on SPU
    
  	genArrayCacheLookup(T3, T0, T1, LOG_BYTES_IN_SHORT);
  	
  	asm.emitANDI(T0, T1, ARRAY_BLOCK_MASK);  // mask to block index
  	asm.emitSHLI(T0, T0, LOG_BYTES_IN_SHORT);
    asm.emitStoreShort(T6, T3, T0);
  }

  /*
  * expression stack manipulation
  */

  /**
   * Emit code to implement the pop bytecode
   */
  @Override
  protected final void emit_pop() {
    discardSlot();
  }

  /**
   * Emit code to implement the pop2 bytecode
   */
  @Override
  protected final void emit_pop2() {
    discardSlots(2);
  }

  /**
   * Emit code to implement the dup bytecode
   */
  @Override
  protected final void emit_dup() {
    peekWord(T0, 0);
    pushWord(T0);
  }

  /**
   * Emit code to implement the dup_x1 bytecode
   */
  @Override
  protected final void emit_dup_x1() {
    popWord(T0);
    popWord(T1);
    pushWord(T0);
    pushWord(T1);
    pushWord(T0);
  }

  /**
   * Emit code to implement the dup_x2 bytecode
   */
  @Override
  protected final void emit_dup_x2() {
    popWord(T0);
    popWord(T1);
    popWord(T2);
    pushWord(T0);
    pushWord(T2);
    pushWord(T1);
    pushWord(T0);
  }

  /**
   * Emit code to implement the dup2 bytecode
   */
  @Override
  protected final void emit_dup2() {
    peekWord(T0, 0);
    peekWord(T1, 1);
    pushWord(T1);
    pushWord(T0);
  }

  /**
   * Emit code to implement the dup2_x1 bytecode
   */
  @Override
  protected final void emit_dup2_x1() {
    popWord(T0);
    popWord(T1);
    popWord(T2);
    pushWord(T1);
    pushWord(T0);
    pushWord(T2);
    pushWord(T1);
    pushWord(T0);
  }

  /**
   * Emit code to implement the dup2_x2 bytecode
   */
  @Override
  protected final void emit_dup2_x2() {
    popWord(T0);
    popWord(T1);
    popWord(T2);
    popWord(T3);
    pushWord(T1);
    pushWord(T0);
    pushWord(T3);
    pushWord(T2);
    pushWord(T1);
    pushWord(T0);
  }

  /**
   * Emit code to implement the swap bytecode
   */
  @Override
  protected final void emit_swap() {
    popWord(T0);
    popWord(T1);
    pushWord(T0);
    pushWord(T1);
  }

  /*
  * int ALU
  */

  /**
   * Emit code to implement the iadd bytecode
   */
  @Override
  protected final void emit_iadd() {
    popWord(T0);
    popWord(T1);
    asm.emitA(T2, T1, T0);
    pushWord(T2);
  }

  /**
   * Emit code to implement the isub bytecode
   */
  @Override
  protected final void emit_isub() {
    popWord(T0);
    popWord(T1);
    asm.emitSF(T2, T0, T1);
    pushWord(T2);
  }

  /**
   * Emit code to implement the imul bytecode
   */
  @Override
  protected final void emit_imul() {
    popWord(T0);
    popWord(T1);
    asm.emitMPYH(S0, T0, T1);
    asm.emitMPYH(S1, T1, T0);
    asm.emitMPYU(S2, T0, T1);
    asm.emitA(T2, S0, S1);
    asm.emitA(T2, T2, S2);    
    pushWord(T2);
  }

  /**
   * Emit code to implement the idiv bytecode
   */
  @Override
  protected final void emit_idiv() {
    popWord(T0);
    popWord(T1);
    genDiv(T2, -1, T1, T0);  // do division (no remainder)
    pushWord(T2);
  }

  /**
   * Emit code to implement the irem bytecode
   */
  @Override
  protected final void emit_irem() {
    popWord(T0);
    popWord(T1);
    genDiv(T3, T2, T1, T0);  // do division (return remainder)
    pushWord(T2);
  }

  /**
   * Emit code to implement the ineg bytecode
   */
  @Override
  protected final void emit_ineg() {
    popWord(T0);
    asm.emitSFI(T1, T0, 0);
    pushWord(T1);
  }

  /**
   * Emit code to implement the ishl bytecode
   */
  @Override
  protected final void emit_ishl() {
    popWord(T1);
    popWord(T0);
    asm.emitANDI(T1, T1, 0x1F);
    asm.emitSHL(T0, T0, T1);
    pushWord(T0);
  }

  /**
   * Emit code to implement the ishr bytecode
   */
  @Override
  protected final void emit_ishr() {
    popWord(T1);
    popWord(T0);
    asm.emitANDI(T1, T1, 0x1f);
    asm.emitSFI(T1, T1, 0);
    asm.emitROTMA(T0, T0, T1);
    pushWord(T0);
  }

  /**
   * Emit code to implement the iushr bytecode
   */
  @Override
  protected final void emit_iushr() {
    popWord(T1);
    popWord(T0);
    asm.emitSFI(T1, T1, 0);
    asm.emitROTM(T0, T0, T1);
    pushWord(T0);
  }

  /**
   * Emit code to implement the iand bytecode
   */
  @Override
  protected final void emit_iand() {
    popWord(T1);
    popWord(T0);
    asm.emitAND(T2, T0, T1);
    pushWord(T2);
  }

  /**
   * Emit code to implement the ior bytecode
   */
  @Override
  protected final void emit_ior() {
    popWord(T1);
    popWord(T0);
    asm.emitOR(T2, T0, T1);
    pushWord(T2);
  }

  /**
   * Emit code to implement the ixor bytecode
   */
  @Override
  protected final void emit_ixor() {
    popWord(T1);
    popWord(T0);
    asm.emitXOR(T2, T0, T1);
    pushWord(T2);
  }

  /**
   * Emit code to implement the iinc bytecode
   * @param index index of local
   * @param val value to increment it by
   */
  @Override
  protected final void emit_iinc(int index, int val) {
    int loc = getGeneralLocalLocation(index);
    if (isRegister(loc)) {
    	if (VM_Assembler.fits(val, 10)) {
    		asm.emitAI(loc, loc, val);
    	} else {
    		asm.emitILW(S0, val);
    		asm.emitA(loc, loc, S0);
    	}
    } else {
      peekWord(T0, locationToOffset(loc));
      if (VM_Assembler.fits(val, 10)) {	
      	asm.emitAI(T0, T0, val);
      } else {
    		asm.emitILW(S0, val);
    		asm.emitA(T0, T0, S0);
      }
      pokeWord(T0, locationToOffset(loc));
    }
  }

  /*
  * long ALU
  */

  /**
   * Emit code to implement the ladd bytecode
   */
  @Override
  protected final void emit_ladd() {
    popLong(T0);
    popLong(T1);
    
    // mask each to make sure extra carries aren't generated
    asm.emitFSMBI(T2, 0xff00);
    asm.emitAND(T0, T0, T2);
    asm.emitAND(T1, T1, T2);
    
    asm.emitCG(T2, T1, T0);
    asm.emitSHLQBYI(T2, T2, BYTES_IN_INT);  // shift carry to next word up
    asm.emitADDX(T2, T1, T0);
    pushLong(T2);
  }

  /**
   * Emit code to implement the lsub bytecode
   */
  @Override
  protected final void emit_lsub() {
    popLong(T0);
    popLong(T1);
    
    // mask each to make sure extra carries aren't generated
    asm.emitFSMBI(T2, 0xff00);
    asm.emitAND(T0, T0, T2);
    asm.emitAND(T1, T1, T2);
    
    asm.emitBG(T2, T0, T1);
    asm.emitSHLQBYI(T2, T2, BYTES_IN_INT);  // shift borrow to next word up
    asm.emitSFX(T2, T0, T1);
    pushLong(T2);
  }

  /**
   * Emit code to implement the lmul bytecode
   */
  @Override
  protected final void emit_lmul() {
  	popLong(T0);
  	popLong(T1);
    
    // mask each to make sure extra carries aren't generated
    asm.emitFSMBI(T2, 0xff00);
    asm.emitAND(T0, T0, T2);
    asm.emitAND(T1, T1, T2);
    
  	genLongMul(T2, T1, T0, T3);
  	pushLong(T2);
  }

  /**
   * Emit code to implement the ldiv bytecode
   */
  @Override
  protected final void emit_ldiv() {
    popLong(T0);
    popLong(T1);
    
    // mask each to make sure extra carries aren't generated
    asm.emitFSMBI(T2, 0xff00);
    asm.emitAND(T0, T0, T2);
    asm.emitAND(T1, T1, T2);
    
    genLongDiv(T2, T3, T1, T0); 
    pushLong(T2);
  }

  /**
   * Emit code to implement the lrem bytecode
   */
  @Override
  protected final void emit_lrem() {
    popLong(T0);
    popLong(T1);
    
    // mask each to make sure extra carries aren't generated
    asm.emitFSMBI(T2, 0xff00);
    asm.emitAND(T0, T0, T2);
    asm.emitAND(T1, T1, T2);
    
    genLongDiv(T3, T2, T1, T0); 
    pushLong(T2);
  }

  /**
   * Emit code to implement the lneg bytecode
   */
  @Override
  protected final void emit_lneg() {
    popLong(T0);
    
    // mask each to make sure extra carries aren't generated
    asm.emitFSMBI(T2, 0xff00);
    asm.emitAND(T0, T0, T2);
    
    asm.emitIL(T1, 0);
    asm.emitBG(T2, T0, T1);
    asm.emitSHLQBYI(T2, T2, BYTES_IN_INT);  // shift borrow to next word up
    asm.emitSFX(T2, T0, T1);
    pushLong(T2);
  }

  /**
   * Emit code to implement the lshsl bytecode
   */
  @Override
  protected final void emit_lshl() {
    popWord(T0); 
    popLong(T1);
    
    // mask each to make sure extra carries aren't generated
    asm.emitFSMBI(T2, 0xff00);
    asm.emitAND(T1, T1, T2);
    
    asm.emitANDI(T0, T0, 0x3F);
  	asm.emitSHLQBYBI(T2, T1, T0);
  	asm.emitSHLQBI(T1, T2, T0);
  	pushLong(T1);
  }

  /**
   * Emit code to implement the lshr bytecode
   */
  @Override
  protected final void emit_lshr() {
    popWord(T0); 
    popLong(T1);
    asm.emitANDI(T0, T0, 0x3F);
    asm.emitSFI(T0, T0, 0);
    
    asm.emitFSMBI(T3, 0x00ff);
    asm.emitOR(T1, T1, T3);  // stick one's at end so rotate acts as arithmetic shift
    
  	asm.emitROTQBYBI(T2, T1, T0);
  	asm.emitROTQBI(T1, T2, T0);
  	pushLong(T1);

  }

  /**
   * Emit code to implement the lushr bytecode
   */
  @Override
  protected final void emit_lushr() {
    popWord(T0); 
    popLong(T1);
    
    // mask each to make sure extra carries aren't generated
    asm.emitFSMBI(T2, 0xff00);
    asm.emitAND(T1, T1, T2);
    
    asm.emitANDI(T0, T0, 0x3F);
    asm.emitSFI(T0, T0, 0);
  	asm.emitROTQMBYBI(T2, T1, T0);
  	asm.emitROTQMBI(T1, T2, T0);
  	pushLong(T1);
  }

  /**
   * Emit code to implement the land bytecode
   */
  @Override
  protected final void emit_land() {
    popLong(T0); 
    popLong(T1);
    asm.emitAND(T2, T0, T1);
    pushLong(T2);
  }

  /**
   * Emit code to implement the lor bytecode
   */
  @Override
  protected final void emit_lor() {
    popLong(T0); 
    popLong(T1);
    asm.emitOR(T2, T0, T1);
    pushLong(T2);
  }

  /**
   * Emit code to implement the lxor bytecode
   */
  @Override
  protected final void emit_lxor() {
    popLong(T0); 
    popLong(T1);
    asm.emitXOR(T2, T0, T1);
    pushLong(T2);
  }

  /*
  * float ALU
  */

  /**
   * Emit code to implement the fadd bytecode
   */
  @Override
  protected final void emit_fadd() {
  	popWord(T0);
  	popWord(T1);
  	asm.emitFA(T2, T1, T0);
  	pushWord(T2);
  }

  /**
   * Emit code to implement the fsub bytecode
   */
  @Override
  protected final void emit_fsub() {
  	popWord(T0);
  	popWord(T1);
  	asm.emitFS(T2, T1, T0);
  	pushWord(T2);
  }

  /**
   * Emit code to implement the fmul bytecode
   */
  @Override
  protected final void emit_fmul() {
  	popWord(T0);
  	popWord(T1);
  	asm.emitFM(T2, T1, T0);
  	pushWord(T2);
  }

  /**
   * Emit code to implement the fdiv bytecode
   */
  @Override
  protected final void emit_fdiv() {
  	popWord(T0);  // bottom
  	popWord(T1);  // top
  	asm.emitFREST(S0, T0);
  	asm.emitFI(S0, T0, S0);
  	asm.emitFM(S1, T1, S0);
  	asm.emitFNMS(S2, S1, T0, T1);
  	asm.emitFMA(S3, S2, S0, S1);
  	asm.emitAI(S4, S3, 1);
  	asm.emitFNMS(S2, T0, S4, S2);
  	asm.emitCGTI(T2, S2, -1);
  	asm.emitSELB(T2, S3, S4, T2);
  	pushWord(T2);
  }

  /**
   * Emit code to implement the frem bytecode
   */
  @Override
  protected final void emit_frem() {
  	//TODO implement or punt to ppu
  	VM._assert(NOT_REACHED);
  }

  /**
   * Emit code to implement the fneg bytecode
   */
  @Override
  protected final void emit_fneg() {
  	popWord(T1);
  	asm.emitLoad(T0, JTOC, VM_Entrypoints.zeroFloatField.getSubArchOffset());
  	asm.emitFA(T2, T1, T0);
  	pushWord(T2);
  }

  /*
  * double ALU
  */

  /**
   * Emit code to implement the dadd bytecode
   */
  @Override
  protected final void emit_dadd() {
  	popLong(T0);
  	popLong(T1);
  	asm.emitDFA(T2, T1, T0);
  	pushLong(T2);
  }

  /**
   * Emit code to implement the dsub bytecode
   */
  @Override
  protected final void emit_dsub() {
  	popLong(T0);
  	popLong(T1);
  	asm.emitDFS(T2, T1, T0);
  	pushLong(T2);
  }

  /**
   * Emit code to implement the dmul bytecode
   */
  @Override
  protected final void emit_dmul() {
  	popLong(T0);
  	popLong(T1);
  	asm.emitDFM(T2, T1, T0);
  	pushLong(T2);
  }

  /**
   * Emit code to implement the ddiv bytecode
   */
  @Override
  protected final void emit_ddiv() {
    // TODO !! VM._assert(NOT_REACHED);
  	
  	int tableOffset = VM_SubArchBootRecord.addFakeTrapStr(VM_Magic.objectAsAddress(VM_Atom.findOrCreateAsciiAtom("ddiv attempted").getBytes()));
  	asm.emitILW(S1, tableOffset);
    asm.emitFakeTrapStr(S0, S1);
  	emit_trap(SubordinateArchitecture.VM_TrapConstants.TRAP_UNIMPLEMENTED_METHOD);
  
  }

  /**
   * Emit code to implement the drem bytecode
   */
  @Override
  protected final void emit_drem() {
    // TODO !! VM._assert(NOT_REACHED);
  	
  	int tableOffset = VM_SubArchBootRecord.addFakeTrapStr(VM_Magic.objectAsAddress(VM_Atom.findOrCreateAsciiAtom("drem attempted").getBytes()));
  	asm.emitILW(S1, tableOffset);
    asm.emitFakeTrapStr(S0, S1);
  	emit_trap(SubordinateArchitecture.VM_TrapConstants.TRAP_UNIMPLEMENTED_METHOD);
  
  }

  /**
   * Emit code to implement the dneg bytecode
   */
  @Override
  protected final void emit_dneg() {
  	popLong(T1);
  	asm.emitLoadDouble(T0, JTOC, VM_Entrypoints.zeroDoubleField.getSubArchOffset());
  	asm.emitDFA(T2, T1, T0);
  	pushLong(T2);
  }

  /*
  * conversion ops
  */

  /**
   * Emit code to implement the i2l bytecode
   */
  @Override
  protected final void emit_i2l() {
  	popWord(T0);
  	asm.emitROTQMBYI(T2, T0, -BYTES_IN_INT);
  	asm.emitXSWD(T1, T2);
  	pushLong(T1);
  }

  /**
   * Emit code to implement the i2f bytecode
   */
  @Override
  protected final void emit_i2f() {
  	popWord(T0);
  	asm.emitCSFLT(T1, T0, 155);
  	pushWord(T1);
  }

  /**
   * Emit code to implement the i2d bytecode
   */
  @Override
  protected final void emit_i2d() {
    popWord(T0);                                                               // T0 is X (an int)
    asm.emitLoadDouble(T5, JTOC, VM_Entrypoints.IEEEmagicField.getSubArchOffset());  // T5 is MAGIC
    asm.emitCGTI(T4, T0, -1);
    asm.emitCWD(T3, JTOC, 4);   // TODO - replace this with constant
    asm.emitAI(T2, T5, -1);
    asm.emitSHUFB(T1, T0, T5, T3);
    asm.emitSHUFB(T2, T0, T2, T3);
    asm.emitSELB(T2, T2, T1, T4);
    asm.emitDFS(T1, T2, T5);
    pushLong(T1);
  }

  /**
   * Emit code to implement the l2i bytecode
   */
  @Override
  protected final void emit_l2i() {
    discardSlot();
  }

  /**
   * Emit code to implement the l2f bytecode
   */
  @Override
  protected final void emit_l2f() {
  	popLong(T0);
  	
    // mask each to make sure extra carries aren't generated
    asm.emitFSMBI(T2, 0xff00);
    asm.emitAND(T0, T0, T2);
    
    genl2d();
    asm.emitFRDS(T1, T2);  // FIXME - This can probably be done more efficiently
  	pushWord(T1);
  }

  /**
   * Emit code to implement the l2d bytecode
   */
  @Override
  protected final void emit_l2d() {
  	popLong(T0);

    // mask each to make sure extra carries aren't generated
    asm.emitFSMBI(T2, 0xff00);
    asm.emitAND(T0, T0, T2);
    
    genl2d();
  	pushLong(T2);
  }

  /**
   * Emit code to implement the f2i bytecode
   */
  @Override
  protected final void emit_f2i() {
  	popWord(T0);
  	asm.emitCFLTS(T1, T0, 173);
  	pushWord(T1);
  }

  /**
   * Emit code to implement the f2l bytecode
   */
  @Override
  protected final void emit_f2l() {
  	// TODO !! VM._assert(NOT_REACHED);
  	
  	int tableOffset = VM_SubArchBootRecord.addFakeTrapStr(VM_Magic.objectAsAddress(VM_Atom.findOrCreateAsciiAtom("f2l attempted")));
    asm.emitILW(S1, tableOffset);
  	asm.emitFakeTrapStr(S0, S1);
  	emit_trap(SubordinateArchitecture.VM_TrapConstants.TRAP_UNIMPLEMENTED_METHOD);
  
  }

  /**
   * Emit code to implement the f2d bytecode
   */
  @Override
  protected final void emit_f2d() {
  	popWord(T0);
  	asm.emitFESD(T1, T0);
  	pushLong(T1);
  }

  /**
   * Emit code to implement the d2i bytecode
   */
  @Override
  protected final void emit_d2i() {
  	// TODO - figure out or trap to PPU
  	VM._assert(NOT_REACHED);
  }

  /**
   * Emit code to implement the d2l bytecode
   */
  @Override
  protected final void emit_d2l() {
  	// TODO - figure out or trap to PPU
  	VM._assert(NOT_REACHED);
  }

  /**
   * Emit code to implement the d2f bytecode
   */
  @Override
  protected final void emit_d2f() {
  	popLong(T0);
  	asm.emitFRDS(T1, T0);
  	pushWord(T1);
  }

  /**
   * Emit code to implement the i2b bytecode
   */
  protected final void emit_i2b() {
  	popWord(T0);
  	asm.emitXSBH(T1, T0);
  	asm.emitXSHW(T2, T1);
  	pushWord(T2);
  }

  /**
   * Emit code to implement the i2c bytecode
   */
  protected final void emit_i2c() {
  	popWord(T0);
  	asm.emitFSMBI(T1, 0x3333);
  	asm.emitAND(T2, T1, T0);
  	pushWord(T2);
  }

  /**
   * Emit code to implement the i2s bytecode
   */
  protected final void emit_i2s() {
  	popWord(T0);
  	asm.emitXSHW(T2, T0);
  	pushWord(T2);
  }

  /*
  * comparison ops
  */

  /**
   * Emit code to implement the lcmp bytecode
   */
  protected final void emit_lcmp() {
  	popLong(T1);
  	popLong(T0);
  	asm.emitCGT(T3, T0, T1);
  	asm.emitCGT(T4, T1, T0);
  	asm.emitANDI(T3, T3, 0x1);
  	asm.emitOR(T5, T3, T4);
  	asm.emitANDI(T2, T5, 0x1);
  	asm.emitSHLI(T2, T2, LOG_BYTES_IN_INT);
  	asm.emitSHLQBY(T5, T5, T2);
  	pushWord(T5);
  }

  /**
   * Emit code to implement the fcmpl bytecode
   */
  protected final void emit_fcmpl() {
  	popWord(T1);
  	popWord(T0);
  	asm.emitFCGT(T2, T0, T1);
  	asm.emitFCEQ(T3, T0, T1);

  	asm.emitIL(T4, -1);            // default of less than 
  	asm.emitSELB(T4, T4, T2, T3);  // if equal -> 0 else -> prev
  	asm.emitIL(T5, 1);             
  	asm.emitSELB(T4, T4, T5, T2);  // if T0 > T1 -> 1 else -> prev

  	pushWord(T4);
  }

  /**
   * Emit code to implement the fcmpg bytecode
   */
  protected final void emit_fcmpg() {
  	popWord(T1);
  	popWord(T0);
  	asm.emitFCGT(T2, T1, T0);
  	asm.emitFCEQ(T3, T0, T1);

  	asm.emitIL(T4, 1);             // default of greater than 
  	asm.emitSELB(T4, T4, T2, T3);  // if equal -> 0 else -> prev
  	asm.emitIL(T5, -1);             
  	asm.emitSELB(T4, T4, T5, T2);  // if T1 > T0 -> 1 else -> prev

  	pushWord(T4);
  }

  /**
   * Emit code to implement the dcmpl bytecode
   */
  protected final void emit_dcmpl() {
  	popLong(T1);
  	popLong(T0);
  	asm.emitDFCGT(T2, T0, T1);
  	asm.emitDFCEQ(T3, T0, T1);

  	asm.emitIL(T4, -1);            // default of less than 
  	asm.emitSELB(T4, T4, T2, T3);  // if equal -> 0 else -> prev
  	asm.emitIL(T5, 1);             
  	asm.emitSELB(T4, T4, T5, T2);  // if T0 > T1 -> 1 else -> prev

  	pushWord(T4);
  }

  /**
   * Emit code to implement the dcmpg bytecode
   */
  protected final void emit_dcmpg() {
  	popLong(T1);
  	popLong(T0);
  	asm.emitDFCGT(T2, T1, T0);
  	asm.emitDFCEQ(T3, T0, T1);

  	asm.emitIL(T4, 1);             // default of greater than 
  	asm.emitSELB(T4, T4, T2, T3);  // if equal -> 0 else -> prev
  	asm.emitIL(T5, -1);             
  	asm.emitSELB(T4, T4, T5, T2);  // if T1 > T0 -> 1 else -> prev

  	pushWord(T4);

  }

  /*
  * branching
  */

  /**
   * Emit code to implement the ifeg bytecode
   * @param bTarget target bytecode of the branch
   */
  protected final void emit_ifeq(int bTarget) {
    popWord(T0);
    genCondBranch(T0, false, bTarget);
  }

  /**
   * Emit code to implement the ifne bytecode
   * @param bTarget target bytecode of the branch
   */
  protected final void emit_ifne(int bTarget) {
    popWord(T0);
    genCondBranch(T0, true, bTarget);
  }

  /**
   * Emit code to implement the iflt bytecode
   * @param bTarget target bytecode of the branch
   */
  protected final void emit_iflt(int bTarget) {
    popWord(T0);
    asm.emitILA(T2, 0);
    asm.emitCGT(T1, T2, T0);
    genCondBranch(T1, true, bTarget);	  
  }

  /**
   * Emit code to implement the ifge bytecode
   * @param bTarget target bytecode of the branch
   */
  protected final void emit_ifge(int bTarget) {
    popWord(T0);
    asm.emitILA(T2, 0);
    asm.emitCGT(T1, T2, T0);
    genCondBranch(T1, false, bTarget);	  
  }

  /**
   * Emit code to implement the ifgt bytecode
   * @param bTarget target bytecode of the branch
   */
  protected final void emit_ifgt(int bTarget) {
    popWord(T0);
    asm.emitCGTI(T1, T0, 0);
    genCondBranch(T1, true, bTarget);	  
  }

  /**
   * Emit code to implement the ifle bytecode
   * @param bTarget target bytecode of the branch
   */
  protected final void emit_ifle(int bTarget) {
    popWord(T0);
    asm.emitCGTI(T1, T0, 0);
    genCondBranch(T1, false, bTarget);	 
  }

  /**
   * Emit code to implement the if_icmpeq bytecode
   * @param bTarget target bytecode of the branch
   */
  protected final void emit_if_icmpeq(int bTarget) {
  	popWord(T1);
  	popWord(T0);
  	asm.emitCEQ(T2, T0, T1);
  	genCondBranch(T2, true, bTarget);
  }

  /**
   * Emit code to implement the if_icmpne bytecode
   * @param bTarget target bytecode of the branch
   */
  protected final void emit_if_icmpne(int bTarget) {
  	popWord(T1);
  	popWord(T0);
  	asm.emitCEQ(T2, T0, T1);
  	genCondBranch(T2, false, bTarget);
  }

  /**
   * Emit code to implement the if_icmplt bytecode
   * @param bTarget target bytecode of the branch
   */
  protected final void emit_if_icmplt(int bTarget) {
  	popWord(T1);
  	popWord(T0);
  	asm.emitCGT(T2, T1, T0);
  	genCondBranch(T2, true, bTarget);
  }

  /**
   * Emit code to implement the if_icmpge bytecode
   * @param bTarget target bytecode of the branch
   */
  protected final void emit_if_icmpge(int bTarget) {
  	popWord(T1);
  	popWord(T0);
  	asm.emitCGT(T2, T1, T0);
  	genCondBranch(T2, false, bTarget);
  }

  /**
   * Emit code to implement the if_icmpgt bytecode
   * @param bTarget target bytecode of the branch
   */
  protected final void emit_if_icmpgt(int bTarget) {
  	popWord(T1);
  	popWord(T0);
  	asm.emitCGT(T2, T0, T1);
  	genCondBranch(T2, true, bTarget);
  }

  /**
   * Emit code to implement the if_icmple bytecode
   * @param bTarget target bytecode of the branch
   */
  protected final void emit_if_icmple(int bTarget) {
  	popWord(T1);
  	popWord(T0);
  	asm.emitCGT(T2, T0, T1);
  	genCondBranch(T2, false, bTarget);
  }

  /**
   * Emit code to implement the if_acmpeq bytecode
   * @param bTarget target bytecode of the branch
   */
  protected final void emit_if_acmpeq(int bTarget) {
  	popWord(T1);
  	popWord(T0);
  	asm.emitCEQ(T2, T0, T1);
  	genCondBranch(T2, true, bTarget);
  }

  /**
   * Emit code to implement the if_acmpne bytecode
   * @param bTarget target bytecode of the branch
   */
  protected final void emit_if_acmpne(int bTarget) {
  	popWord(T1);
  	popWord(T0);
  	asm.emitCEQ(T2, T0, T1);
  	genCondBranch(T2, false, bTarget);
  }

  /**
   * Emit code to implement the ifnull bytecode
   * @param bTarget target bytecode of the branch
   */
  protected final void emit_ifnull(int bTarget) {
  	popWord(T0);
  	genCondBranch(T0, false, bTarget);
  }

  /**
   * Emit code to implement the ifnonnull bytecode
   * @param bTarget target bytecode of the branch
   */
  protected final void emit_ifnonnull(int bTarget) {
  	popWord(T0);
  	genCondBranch(T0, true, bTarget);
  }

  /**
   * Emit code to implement the goto and gotow bytecodes
   * @param bTarget target bytecode of the branch
   */
  protected final void emit_goto(int bTarget) {
    int mTarget = bytecodeMap[bTarget];
    if (bTarget - SHORT_FORWARD_LIMIT < biStart) {
    	asm.emitBR(mTarget, bTarget);
    } else {
    	asm.emitILA(T0, mTarget, bTarget);
    	asm.emitBI(T0);
    }
  }

  /**
   * Emit code to implement the jsr and jsrw bytecode
   * @param bTarget target bytecode of the jsr
   */
  protected final void emit_jsr(int bTarget) {
  	
  	boolean shortJmp = bTarget - SHORT_FORWARD_LIMIT < biStart;
  	
  	asm.emitLoadLR(T1); 
    int start = asm.getMachineCodeIndex();
    int delta = shortJmp ? 4 : 5; 
    asm.emitA(T1, delta * INSTRUCTION_WIDTH, T1);  
    pushWord(T1);
    if (shortJmp) {
    	asm.emitBR(bytecodeMap[bTarget], bTarget);
    } else {
    	asm.emitILA(T0, bytecodeMap[bTarget], bTarget);
    	asm.emitBI(T0);
    }
    int done = asm.getMachineCodeIndex();
    if (VM.VerifyAssertions) VM._assert((done - start) == delta);
  }

  /**
   * Emit code to implement the ret bytecode
   * @param index local variable containing the return address
   */
  protected final void emit_ret(int index) {
    int location = getGeneralLocalLocation(index);

    if (!isRegister(location)) {
    	peekWord(T0, locationToOffset(location));
      location = T0;
    }
    asm.emitBI(T0);
  }

  /**
   * Emit code to implement the tableswitch bytecode
   * @param defaultval bcIndex of the default target
   * @param low low value of switch
   * @param high high value of switch
   */
  protected final void emit_tableswitch(int defaultval, int low, int high) {
    int bTarget = biStart + defaultval;
    int mTarget = bytecodeMap[bTarget];
    int n = high - low + 1;       // n = number of normal cases (0..n-1)

    popWord(T0);  // T0 is index
    if (VM_Assembler.fits(-low, 10)) {
      asm.emitAI(T0, T0, -low);
    } else {
      asm.emitILW(T1, low);
      asm.emitSF(T0, T1, T0);
    }
    asm.emitILW(T2, n);
    asm.emitCLGT(T1, T2, T0);
    if (options.EDGE_COUNTERS) {
    	VM._assert(NOT_REACHED);
    	// TODO - edge counters
    } else {
    	genCondBranch(T1, false, bTarget);
    }
    VM_ForwardReference fr1 = asm.emitForwardBRSL(LINK_REG);
    for (int i = 0; i < n; i++) {
      int offset = bcodes.getTableSwitchOffset(i);
      bTarget = biStart + offset;
      mTarget = bytecodeMap[bTarget];
      asm.emitSwitchCase(i, mTarget, bTarget);
    }
    bcodes.skipTableSwitchOffsets(n);
    fr1.resolve(asm);   
    asm.emitSHLI(T0, T0, LOG_BYTES_IN_INT); // convert to bytes
    if (options.EDGE_COUNTERS) {
    	VM._assert(NOT_REACHED);
    	// TODO - edge counters
    }
    // LINK_REG is base of table
    asm.emitLoadUnaligned(T1, LINK_REG, T0);    // T0 is relative offset of desired case
    asm.emitA(LINK_REG, LINK_REG, T1); // T1 is absolute address of desired case
    asm.emitBI(LINK_REG);
  }

  /**
   * Emit code to implement the lookupswitch bytecode
   * @param defaultval bcIndex of the default target
   * @param npairs number of pairs in the lookup switch
   */
  protected final void emit_lookupswitch(int defaultval, int npairs) {

    if (options.EDGE_COUNTERS) {
    	VM._assert(NOT_REACHED);
    	// TODO - edge counters
    }

    popWord(T0); // T0 is key
    for (int i = 0; i < npairs; i++) {
      int match = bcodes.getLookupSwitchValue(i);
      if (VM_Assembler.fits(match, 10)) {
      	asm.emitCEQI(T1, T0, match);
      } else {
        asm.emitILW(T1, match);
        asm.emitCEQ(T1, T0, T1);
      }
      
      int offset = bcodes.getLookupSwitchOffset(i);
      int bTarget = biStart + offset;
      if (options.EDGE_COUNTERS) {
      	VM._assert(NOT_REACHED);
      	// TODO - edge counters
      } else {
      	genCondBranch(T1, true, bTarget);
      }
    }
    bcodes.skipLookupSwitchPairs(npairs);
    int bTarget = biStart + defaultval;
    int mTarget = bytecodeMap[bTarget];
    if (options.EDGE_COUNTERS) {
    	VM._assert(NOT_REACHED);
    	// TODO - edge counters
    }
    asm.emitBR(mTarget, bTarget);
  }
  
  /*
  * returns (from function; NOT ret)
  */

  /**
   * Emit code to implement the ireturn bytecode
   */
  protected final void emit_ireturn() {
    if (method.isSynchronized()) genSynchronizedMethodEpilogue();
    peekWord(T0, 0);
    genEpilogue();
  }

  /**
   * Emit code to implement the lreturn bytecode
   */
  protected final void emit_lreturn() {
    if (method.isSynchronized()) genSynchronizedMethodEpilogue();
    peekLong(T0, 0);
    genEpilogue();
  }

  /**
   * Emit code to implement the freturn bytecode
   */
  protected final void emit_freturn() {
    if (method.isSynchronized()) genSynchronizedMethodEpilogue();
    peekWord(T0, 0);
    genEpilogue();
  }

  /**
   * Emit code to implement the dreturn bytecode
   */
  protected final void emit_dreturn() {
    if (method.isSynchronized()) genSynchronizedMethodEpilogue();
    peekLong(T0, 0);
    genEpilogue();
  }

  /**
   * Emit code to implement the areturn bytecode
   */
  protected final void emit_areturn() {
    if (method.isSynchronized()) genSynchronizedMethodEpilogue();
    peekWord(T0, 0);
    genEpilogue();
  }

  /**
   * Emit code to implement the return bytecode
   */
  protected final void emit_return() {
    if (method.isSynchronized()) genSynchronizedMethodEpilogue();
    genEpilogue();
  }

  /*
  * field access
  */

  /**
   * Emit code to implement a dynamically linked getstatic
   * @param fieldRef the referenced field
   */
  protected final void emit_unresolved_getstatic(VM_FieldReference fieldRef) {
    emitDynamicLinkingSequence(T1, fieldRef, true);
    // TODO - sort out if final or not!! and volatile or not
    if (fieldRef.getSize() <= BYTES_IN_INT) { // field is one word
      asm.emitLoad(T0, JTOC, T1);
      pushWord(T0);
    } else { // field is two words (double or long)
      if (VM.VerifyAssertions) VM._assert(fieldRef.getSize() == BYTES_IN_LONG);
      asm.emitLoadDouble(T0, JTOC, T1);
      pushLong(T0);
    }
  }
  
  /**
   * Emit code to implement a getstatic
   * @param fieldRef the referenced field
   */
  protected final void emit_resolved_getstatic(VM_FieldReference fieldRef) {
    VM_Field field = fieldRef.peekResolvedField(true);
  	Offset fieldOffset = field.getSubArchOffset();

  	if (VM.VerifyAssertions && (field.isFinal() && field.getConstantValueIndex() > 0)) {
  		if (field.getType().isReferenceType()) 
  			VM._assert(fieldOffset.toInt() >= 0 && fieldOffset.toInt() <= field.getDeclaringClass().getStaticRefBlockLength());
  		else
  			VM._assert(fieldOffset.toInt() >= 0 && fieldOffset.toInt() <=  field.getDeclaringClass().getStaticNumericBlockLength());
  	}
  	
  	if (field.isVolatile()) {
  		genCacheFlush();
  	}
  	
    if (fieldRef.getSize() <= BYTES_IN_INT) { // field is one word
    	if (field.isFinal() && field.getConstantValueIndex() > 0) {
    		asm.emitLoad(T0, JTOC, fieldOffset);
    	} else {
    		// check if statics are cached
    		asm.emitILA(T5, getFieldTocOffset(field).toInt());
    		genStaticsLookup(T4, T5);
    		asm.emitLoad(T0, T4, fieldOffset);
    	}
    	pushWord(T0);
    } else { // field is two words (double or long ( or address on PPC64))
      if (VM.VerifyAssertions) VM._assert(fieldRef.getSize() == BYTES_IN_LONG);

    	if (field.isFinal()) {
      	asm.emitLoadDouble(T0, JTOC, fieldOffset);
  		} else {
  			// get the static offset for this classes static block
    		asm.emitILA(T5, getFieldTocOffset(field).toInt());
    		genStaticsLookup(T4, T5);
  			asm.emitLoadDouble(T0, T4, fieldOffset);
  		}
      pushLong(T0);
    }
  }

  /**
   * Emit code to implement a dynamically linked putstatic
   * @param fieldRef the referenced field
   */
  protected final void emit_unresolved_putstatic(VM_FieldReference fieldRef) {
    emitDynamicLinkingSequence(T0, fieldRef, true);
    // TODO - work out if reference is volatile or not
    if (MM_Constants.NEEDS_PUTSTATIC_WRITE_BARRIER && !fieldRef.getFieldContentsType().isPrimitiveType()) {
      VM_Barriers.compilePutstaticBarrier(this, fieldRef.getId()); // NOTE: offset is in T0 from emitDynamicLinkingSequence
      discardSlots(1);
      return;
    }
    if (fieldRef.getSize() <= BYTES_IN_INT) { // field is one word
      popWord(T1);
      asm.emitStore(T1, JTOC, T0);
    } else { // field is two words (double or long (or address on PPC64))
      if (VM.VerifyAssertions) VM._assert(fieldRef.getSize() == BYTES_IN_LONG);
      popLong(T1);
      asm.emitStoreDouble(T1, JTOC, T0);
    }
  }

  /**
   * Emit code to implement a putstatic
   * @param fieldRef the referenced field
   */
  protected final void emit_resolved_putstatic(VM_FieldReference fieldRef) {
    VM_Field field = fieldRef.peekResolvedField(true);
    Offset fieldOffset = field.getSubArchOffset();
    int jtocAddr = (VM_SubArchStatics.getSlotContentsAsInt(VM_SubArchStatics.mainJTOCAddrOff));
    
  	if (VM.VerifyAssertions) VM._assert(fieldOffset.toInt() <= field.getDeclaringClass().getStaticRefBlockLength() ||
        (-fieldOffset.toInt()) <=  field.getDeclaringClass().getStaticNumericBlockLength());
  	if (VM.VerifyAssertions) VM._assert(!field.isFinal());
  	
    if (MM_Constants.NEEDS_PUTSTATIC_WRITE_BARRIER && !fieldRef.getFieldContentsType().isPrimitiveType()) {
      VM_Barriers.compilePutstaticBarrierImm(this, fieldOffset, fieldRef.getId());
      discardSlots(1);
      return;
    }
    if (fieldRef.getSize() <= BYTES_IN_INT) { // field is one word
      popWord(T0);
  		asm.emitILA(T5, getFieldTocOffset(field).toInt());
  		genStaticsLookup(T4, T5);
  		asm.emitAI(T2, T4, fieldOffset.toInt());
      asm.emitStore(T0, T2);
      // store in main memory
      asm.emitILW(T1, jtocAddr + field.getOffset().toInt());
      asm.emitILW(T3, BYTES_IN_INT);
      asm.emitPUT(T1,T3,T2,OBJECT_CACHE_WRITE_TAG_GROUP);
    } else { // field is two words (double or long (or address on PPC64))
      if (VM.VerifyAssertions) VM._assert(fieldRef.getSize() == BYTES_IN_LONG);
      popLong(T0);
  		asm.emitILA(T5, getFieldTocOffset(field).toInt());
  		genStaticsLookup(T4, T5);
			asm.emitAI(T2, T2, fieldOffset.toInt());
    	asm.emitStoreDouble(T0, T2);
    	// store in main memory
    	asm.emitILW(T1, jtocAddr + field.getOffset().toInt());
      asm.emitILW(T3, BYTES_IN_LONG);
      asm.emitPUT(T1,T3,T2,OBJECT_CACHE_WRITE_TAG_GROUP); // written atomically and in order, so ok for volatile
    }
  }

  /**
   * Emit code to implement a dynamically linked getfield
   * @param fieldRef the referenced field
   */
  protected final void emit_unresolved_getfield(VM_FieldReference fieldRef) {

    VM_TypeReference fieldType = fieldRef.getFieldContentsType();
    
    // T2 = field offset from emitDynamicLinkingSequence()
    emitDynamicLinkingSequence(T3, T2, fieldRef, true);
    // T4 = object reference
    popWord(T4);
    asm.emitNullCheck(T4);
    genObjectCacheLookup(T1, T4, T2);  // lookup object address in cache
    
    if (fieldType.isReferenceType() || fieldType.isWordType()) {
      asm.emitLoadUnaligned(T0, T1, T3);
      pushWord(T0);
    } else if (fieldType.isBooleanType()) {
      // 8bit unsigned load
      asm.emitLoadByteUnaligned(T0, T1, T3);   // TODO - check if we need to make this unsigned
      pushWord(T0);
    } else if (fieldType.isByteType()) {
      // 8bit signed load
    	asm.emitLoadByteUnaligned(T0, T1, T3); 
      pushWord(T0);
    } else if (fieldType.isShortType()) {
      // 16bit signed load
    	asm.emitLoadShortUnaligned(T0, T1, T3); 
      pushWord(T0);
    } else if (fieldType.isCharType()) {
      // 16bit unsigned load
    	asm.emitLoadCharUnaligned(T0, T1, T3); 
      pushWord(T0);
    } else if (fieldType.isIntType() || fieldType.isFloatType()) {
      // 32bit load
    	asm.emitLoadUnaligned(T0, T1, T3); 
      pushWord(T0);
    } else {
      // 64bit load
      if (VM.VerifyAssertions) VM._assert(fieldType.isLongType() || fieldType.isDoubleType());
    	asm.emitLoadDoubleUnaligned(T0, T1, T3); 
      pushLong(T0);
    }
  }

  /**
   * Emit code to implement a getfield
   * @param fieldRef the referenced field
   */
  protected final void emit_resolved_getfield(VM_FieldReference fieldRef) {
  	
    VM_TypeReference fieldType = fieldRef.getFieldContentsType();
    VM_Field field = fieldRef.peekResolvedField(true);
    Offset fieldOffset = field.getSubArchOffset();
    int instanceSize = ((VM_Class)fieldRef.getType().peekType()).getInstanceSize();
    
    popWord(T4); // T1 = object reference
    asm.emitNullCheck(T4);
    
    if (field.isVolatile()) {
    	genCacheFlush();
    }
    
    asm.emitILW(T2, instanceSize);
    genObjectCacheLookup(T1, T4, T2);  // lookup object address in cache
    
    if (fieldType.isReferenceType() || fieldType.isWordType()) {
      asm.emitLoadUnaligned(T0, T1, fieldOffset);
      pushWord(T0);
    } else if (fieldType.isBooleanType()) {
      // 8bit unsigned load
      asm.emitLoadByteUnaligned(T0, T1, fieldOffset);   // TODO - check if we need to make this unsigned
      pushWord(T0);
    } else if (fieldType.isByteType()) {
      // 8bit signed load
    	asm.emitLoadByteUnaligned(T0, T1, fieldOffset); 
      pushWord(T0);
    } else if (fieldType.isShortType()) {
      // 16bit signed load
    	asm.emitLoadShortUnaligned(T0, T1, fieldOffset); 
      pushWord(T0);
    } else if (fieldType.isCharType()) {
      // 16bit unsigned load
    	asm.emitLoadCharUnaligned(T0, T1, fieldOffset); 
      pushWord(T0);
    } else if (fieldType.isIntType() || fieldType.isFloatType()) {
      // 32bit load
    	asm.emitLoadUnaligned(T0, T1, fieldOffset); 
      pushWord(T0);
    } else {
      // 64bit load
      if (VM.VerifyAssertions) VM._assert(fieldType.isLongType() || fieldType.isDoubleType());
    	asm.emitLoadDoubleUnaligned(T0, T1, fieldOffset); 
      pushLong(T0);
    }  
  }

	/**
   * Emit code to implement a dynamically linked putfield
   * @param fieldRef the referenced field
   */
  protected final void emit_unresolved_putfield(VM_FieldReference fieldRef) {

    VM_TypeReference fieldType = fieldRef.getFieldContentsType();
    // T1 = field offset from emitDynamicLinkingSequence()
    emitDynamicLinkingSequence(T3, T2, fieldRef, true);
    if (fieldType.isReferenceType()) {
      // 32bit reference store
      if (MM_Constants.NEEDS_WRITE_BARRIER) {
        // NOTE: offset is in T1 from emitDynamicLinkingSequence
        VM_Barriers.compilePutfieldBarrier((SubordinateArchitecture.VM_Compiler) this, fieldRef.getId());
        discardSlots(2);
      } else {
        popWord(T0);                // T0 = address value
        popWord(T4);                // T4 = object reference
        asm.emitNullCheck(T4);
        genObjectCacheLookup(T1, T4, T2);  // lookup object address in cache
        asm.emitStore(T0, T1, T3);
      }
    } else if (fieldType.isWordType()) {
      // 32bit word store
      popWord(T0);                // T0 = value
      popWord(T4);                // T1 = object reference
      asm.emitNullCheck(T4);
      genObjectCacheLookup(T1, T4, T2);  // lookup object address in cache
      asm.emitStore(T0, T1, T3);
    } else if (fieldType.isBooleanType() || fieldType.isByteType()) {
      // 8bit store
      popWord(T0); // T0 = value
      popWord(T4); // T1 = object reference
      asm.emitNullCheck(T4);
      genObjectCacheLookup(T1, T4, T2);  // lookup object address in cache
      asm.emitStoreByte(T0, T1, T3);
    } else if (fieldType.isShortType() || fieldType.isCharType()) {
      // 16bit store
      popWord(T0); // T0 = value
      popWord(T4); // T1 = object reference
      asm.emitNullCheck(T4);
      genObjectCacheLookup(T1, T4, T2);  // lookup object address in cache
      asm.emitStoreShort(T0, T1, T3);
    } else if (fieldType.isIntType() || fieldType.isFloatType()) {
      // 32bit store
      popWord(T0); // T0 = value
      popWord(T4); // T1 = object reference
      asm.emitNullCheck(T4);
      genObjectCacheLookup(T1, T4, T2);  // lookup object address in cache
      asm.emitStore(T0, T1, T3);
    } else {
      // 64bit store
      if (VM.VerifyAssertions) VM._assert(fieldType.isLongType() || fieldType.isDoubleType());
      popLong(T0);     // F0 = doubleword value
      popWord(T4);       // T1 = object reference
      asm.emitNullCheck(T4);
      genObjectCacheLookup(T1, T4, T2);  // lookup object address in cache
      asm.emitStoreDouble(T0, T1, T3);
    }
  }

  /**
   * Emit code to implement a putfield
   * @param fieldRef the referenced field
   */
  protected final void emit_resolved_putfield(VM_FieldReference fieldRef) {
    Offset fieldOffset = fieldRef.peekResolvedField(true).getSubArchOffset();
    VM_TypeReference fieldType = fieldRef.getFieldContentsType();
    int instanceSize = ((VM_Class)fieldRef.getType().peekType()).getInstanceSize();
	
    if (fieldType.isReferenceType()) {
      // 32bit reference store
      if (MM_Constants.NEEDS_WRITE_BARRIER) {
        // NOTE: offset is in T1 from emitDynamicLinkingSequence
        VM_Barriers.compilePutfieldBarrier((SubordinateArchitecture.VM_Compiler) this, fieldRef.getId());
        discardSlots(2);
      } else {
        popWord(T0);                // T0 = address value
        popWord(T4);                // T1 = object reference
        asm.emitNullCheck(T4);
        asm.emitILW(T2, instanceSize);
        genObjectCacheLookup(T1, T4, T2);  // lookup object address in cache
        asm.emitAI(T1, T1, fieldOffset.toInt());
        asm.emitStore(T0, T1);
        asm.emitILW(T3, BYTES_IN_ADDRESS);
        asm.emitAI(T4, T4, fieldOffset.toInt());
        asm.emitPUT(T4, T3, T1, OBJECT_CACHE_WRITE_TAG_GROUP);
      }
    } else if (fieldType.isWordType()) {
      // 32bit word store
      popWord(T0);                // T0 = value
      popWord(T4);                // T1 = object reference
      asm.emitNullCheck(T4);
      asm.emitILW(T2, instanceSize);
      genObjectCacheLookup(T1, T4, T2);  // lookup object address in cache
      asm.emitAI(T1, T1, fieldOffset.toInt());
      asm.emitStore(T0, T1);
      asm.emitILW(T3, BYTES_IN_INT);
      asm.emitAI(T4, T4, fieldOffset.toInt());
      asm.emitPUT(T4, T3, T1, OBJECT_CACHE_WRITE_TAG_GROUP);
    } else if (fieldType.isBooleanType() || fieldType.isByteType()) {
      // 8bit store
      popWord(T0); // T0 = value
      popWord(T4); // T1 = object reference
      asm.emitNullCheck(T4);
      asm.emitILW(T2, instanceSize);
      genObjectCacheLookup(T1, T4, T2);  // lookup object address in cache
      asm.emitAI(T1, T1, fieldOffset.toInt());
      asm.emitStoreByte(T0, T1, Offset.fromIntZeroExtend(0));
      asm.emitILW(T3, BYTES_IN_BYTE);
      asm.emitAI(T4, T4, fieldOffset.toInt());
      asm.emitPUT(T4, T3, T1, OBJECT_CACHE_WRITE_TAG_GROUP);
    } else if (fieldType.isShortType() || fieldType.isCharType()) {
      // 16bit store
      popWord(T0); // T0 = value
      popWord(T4); // T1 = object reference
      asm.emitNullCheck(T4);
      asm.emitILW(T2, instanceSize);
      genObjectCacheLookup(T1, T4, T2);  // lookup object address in cache
      asm.emitAI(T1, T1, fieldOffset.toInt());
      asm.emitStoreShort(T0, T1, Offset.fromIntZeroExtend(0));
      asm.emitILW(T3, BYTES_IN_SHORT);
      asm.emitAI(T4, T4, fieldOffset.toInt());
      asm.emitPUT(T4, T3, T1, OBJECT_CACHE_WRITE_TAG_GROUP);
    } else if (fieldType.isIntType() || fieldType.isFloatType()) {
      // 32bit store
      popWord(T0); // T0 = value
      popWord(T4); // T1 = object reference
      asm.emitNullCheck(T4);
      asm.emitILW(T2, instanceSize);
      genObjectCacheLookup(T1, T4, T2);  // lookup object address in cache
      asm.emitAI(T1, T1, fieldOffset.toInt());
      asm.emitStore(T0, T1);
      asm.emitILW(T3, BYTES_IN_INT);
      asm.emitAI(T4, T4, fieldOffset.toInt());
      asm.emitPUT(T4, T3, T1, OBJECT_CACHE_WRITE_TAG_GROUP);
    } else {
      // 64bit store
      if (VM.VerifyAssertions) VM._assert(fieldType.isLongType() || fieldType.isDoubleType());
      popLong(T0);     // F0 = doubleword value
      popWord(T4);       // T1 = object reference
      asm.emitNullCheck(T4);
      asm.emitILW(T2, instanceSize);
      genObjectCacheLookup(T1, T4, T2);  // lookup object address in cache
      asm.emitAI(T1, T1, fieldOffset.toInt());
      asm.emitStoreDouble(T0, T1);
      asm.emitILW(T3, BYTES_IN_DOUBLE);
      asm.emitAI(T4, T4, fieldOffset.toInt());
      asm.emitPUT(T4, T3, T1, OBJECT_CACHE_WRITE_TAG_GROUP);
    }
  }

  /*
   * method invocation
   */

  /**
   * Emit code to implement a dynamically linked invokevirtual
   * @param methodRef the referenced method
   */
  protected final void emit_unresolved_invokevirtual(VM_MethodReference methodRef) {
    // TODO !! VM._assert(NOT_REACHED);
  	
  	int tableOffset = VM_SubArchBootRecord.addFakeTrapStr(VM_Magic.objectAsAddress(VM_Atom.findOrCreateAsciiAtom("unresolved invoke virtual attempted").getBytes()));
  	asm.emitILW(S1, tableOffset);
    asm.emitFakeTrapStr(S0, S1);
  	emit_trap(SubordinateArchitecture.VM_TrapConstants.TRAP_UNIMPLEMENTED_METHOD);
  
  }

  /**
   * Emit code to implement invokevirtual
   * @param methodRef the referenced method
   */
  protected final void emit_resolved_invokevirtual(VM_MethodReference methodRef) {
    VM_Method methodInv    = methodRef.peekResolvedMethod(true);
    Offset methodOffset    = methodInv.getSubArchOffset();
  	int objectIndex = methodRef.getParameterWords(); // +1 for "this" parameter, -1 to load it
    int instanceSize = ((VM_Class)methodRef.getType().peekType()).getInstanceSize();
    
    peekWord(T4, objectIndex);
    
    asm.emitNullCheck(T4);
    asm.emitILW(T2, instanceSize);
    genObjectCacheLookup(T1, T4, T2);  // lookup object address in cache
    
    VM_ObjectModel.baselineEmitLoadTIB(asm, T5, T1, true); // returns in T5 index into TIB table 
    asm.emitIOHL(T5, TIB_TABLE_JTOC_OFF >> LOG_BYTES_IN_INT);
    asm.emitSHLI(T5, T5, LOG_BYTES_IN_INT);
    
    genClassTibLookup(T7, T5);   // T7 now holds pointer to relevent TIB
		asm.emitILA(T5, methodOffset.toInt());
    genMethodCacheLookup(S5, T7, T5);

    // TODO - Branch Hint
    
    // once in the method is cached in local memory, jump to it
    genMoveParametersToRegisters(true, methodRef);
    asm.emitBISL(LINK_REG, S5);
    genPopParametersAndPushReturnValue(true, methodRef);
  }

  /**
   * Emit code to implement a dynamically linked <code>invokespecial</code>
   * @param methodRef The referenced method
   * @param target    The method to invoke
   */
  protected final void emit_resolved_invokespecial(VM_MethodReference methodRef, VM_Method target) {
    // TODO !! VM._assert(NOT_REACHED);
  	
  	int tableOffset = VM_SubArchBootRecord.addFakeTrapStr(VM_Magic.objectAsAddress(VM_Atom.findOrCreateAsciiAtom("invoke special attempted").getBytes()));
  	asm.emitILW(S1, tableOffset);
    asm.emitFakeTrapStr(S0, S1);
  	emit_trap(SubordinateArchitecture.VM_TrapConstants.TRAP_UNIMPLEMENTED_METHOD);
  
  }

  /**
   * Emit code to implement invokespecial
   * @param methodRef the referenced method
   */
  protected final void emit_unresolved_invokespecial(VM_MethodReference methodRef) {
    // TODO !! VM._assert(NOT_REACHED);
  	
  	int tableOffset = VM_SubArchBootRecord.addFakeTrapStr(VM_Magic.objectAsAddress(VM_Atom.findOrCreateAsciiAtom("unresolved invoke special attempted").getBytes()));
  	asm.emitILW(S1, tableOffset);
    asm.emitFakeTrapStr(S0, S1);
  	emit_trap(SubordinateArchitecture.VM_TrapConstants.TRAP_UNIMPLEMENTED_METHOD);
  
  }

  /**
   * Emit code to implement a dynamically linked invokestatic
   * @param methodRef the referenced method
   */
  protected final void emit_unresolved_invokestatic(VM_MethodReference methodRef) {
  	// TODO !! VM._assert(NOT_REACHED);
  	
  	int tableOffset = VM_SubArchBootRecord.addFakeTrapStr(VM_Magic.objectAsAddress(VM_Atom.findOrCreateAsciiAtom("unresolved invoke static attempted").getBytes()));
  	asm.emitILW(S1, tableOffset);
    asm.emitFakeTrapStr(S0, S1);
  	emit_trap(SubordinateArchitecture.VM_TrapConstants.TRAP_UNIMPLEMENTED_METHOD);
  
  }

  /**
   * Emit code to implement invokestatic
   * @param methodRef the referenced method
   */
  protected final void emit_resolved_invokestatic(VM_MethodReference methodRef) {
  	VM_Method methodInv    = methodRef.peekResolvedMethod(true);
    Offset methodOffset    = methodInv.getSubArchOffset();
    
    // TODO - Branch Hint
    asm.emitILA(T5, getMethodTibOffset(methodInv).toInt());
		genClassTibLookup(T7, T5);

		asm.emitILA(T5, methodOffset.toInt());
    genMethodCacheLookup(S5, T7, T5);
    
    // once in the method is cached in local memory, jump to it
    genMoveParametersToRegisters(false, methodRef);
    asm.emitBISL(LINK_REG, S5);
    genPopParametersAndPushReturnValue(false, methodRef);
  }

  /**
   * Emit code to implement the invokeinterface bytecode
   * @param methodRef the referenced method
   */
  protected final void emit_invokeinterface(VM_MethodReference methodRef) {
   // TODO !! VM._assert(NOT_REACHED);
  	
  	int tableOffset = VM_SubArchBootRecord.addFakeTrapStr(VM_Magic.objectAsAddress(VM_Atom.findOrCreateAsciiAtom("invoke interface attempted").getBytes()));
  	asm.emitILW(S1, tableOffset);
    asm.emitFakeTrapStr(S0, S1);
  	emit_trap(SubordinateArchitecture.VM_TrapConstants.TRAP_UNIMPLEMENTED_METHOD);
  }

  /*
   * other object model functions
   */

  /**
   * Emit code to allocate a scalar object
   * @param typeRef the VM_Class to instantiate
   */
  protected final void emit_resolved_new(VM_Class typeRef) {
	  // TODO !! VM._assert(NOT_REACHED);
  	
  	int tableOffset = VM_SubArchBootRecord.addFakeTrapStr(VM_Magic.objectAsAddress(VM_Atom.findOrCreateAsciiAtom("resolved new attempted").getBytes()));
  	asm.emitILW(S1, tableOffset);
    asm.emitFakeTrapStr(S0, S1);
  	emit_trap(SubordinateArchitecture.VM_TrapConstants.TRAP_UNIMPLEMENTED_METHOD);
  
  }

  /**
   * Emit code to dynamically link and allocate a scalar object
   * @param typeRef the type reference to dynamically link & instantiate
   */
  protected final void emit_unresolved_new(VM_TypeReference typeRef) {
  	// TODO !! VM._assert(NOT_REACHED);
  	
  	int tableOffset = VM_SubArchBootRecord.addFakeTrapStr(VM_Magic.objectAsAddress(VM_Atom.findOrCreateAsciiAtom("unresolved new attempted").getBytes()));
  	asm.emitILW(S1, tableOffset);
    asm.emitFakeTrapStr(S0, S1);
  	emit_trap(SubordinateArchitecture.VM_TrapConstants.TRAP_UNIMPLEMENTED_METHOD);
  }

  /**
   * Emit code to allocate an array
   * @param array the VM_Array to instantiate
   */
  protected final void emit_resolved_newarray(VM_Array array) {
	  // TODO !! VM._assert(NOT_REACHED);
  	
  	int tableOffset = VM_SubArchBootRecord.addFakeTrapStr(VM_Magic.objectAsAddress(VM_Atom.findOrCreateAsciiAtom("resolved new array attempted").getBytes()));
  	asm.emitILW(S1, tableOffset);
    asm.emitFakeTrapStr(S0, S1);
  	emit_trap(SubordinateArchitecture.VM_TrapConstants.TRAP_UNIMPLEMENTED_METHOD);
  }

  /**
   * Emit code to dynamically link and allocate an array
   * @param typeRef the type reference to dynamically link & instantiate
   */
  protected final void emit_unresolved_newarray(VM_TypeReference typeRef) {
	  // TODO !! VM._assert(NOT_REACHED);
  	
  	int tableOffset = VM_SubArchBootRecord.addFakeTrapStr(VM_Magic.objectAsAddress(VM_Atom.findOrCreateAsciiAtom("unresolved new array attempted").getBytes()));
  	asm.emitILW(S1, tableOffset);
    asm.emitFakeTrapStr(S0, S1);
  	emit_trap(SubordinateArchitecture.VM_TrapConstants.TRAP_UNIMPLEMENTED_METHOD);
  
  }

  /**
   * Emit code to allocate a multi-dimensional array
   * @param typeRef the VM_Array to instantiate
   * @param dimensions the number of dimensions
   */
  protected final void emit_multianewarray(VM_TypeReference typeRef, int dimensions) {
	  // TODO !! VM._assert(NOT_REACHED);
  	
  	int tableOffset = VM_SubArchBootRecord.addFakeTrapStr(VM_Magic.objectAsAddress(VM_Atom.findOrCreateAsciiAtom("new multi dimensional array attempted").getBytes()));
  	asm.emitILW(S1, tableOffset);
    asm.emitFakeTrapStr(S0, S1);
  	emit_trap(SubordinateArchitecture.VM_TrapConstants.TRAP_UNIMPLEMENTED_METHOD);
  }

  /**
   * Emit code to implement the arraylength bytecode
   */
  protected final void emit_arraylength() {
  	popWord(T0);
  	genArrayTableLookup(T0);
    asm.emitLoad(S0, T5, VM_ObjectModel.getArrayLengthOffset());
    pushWord(S0);
  }

  /**
   * Emit code to implement the athrow bytecode
   */
  protected final void emit_athrow() {	
  	// TODO !! VM._assert(NOT_REACHED);
  	
  	int tableOffset = VM_SubArchBootRecord.addFakeTrapStr(VM_Magic.objectAsAddress(VM_Atom.findOrCreateAsciiAtom("athrow attempted").getBytes()));
  	asm.emitILW(S1, tableOffset);
    asm.emitFakeTrapStr(S0, S1);
  	emit_trap(SubordinateArchitecture.VM_TrapConstants.TRAP_UNIMPLEMENTED_METHOD);
  
  }

  /**
   * Emit code to implement the checkcast bytecode
   * @param typeRef   The LHS type
   */
  protected final void emit_checkcast(VM_TypeReference typeRef) {

  }

  /**
   * Emit code to implement the checkcast bytecode
   * @param type   The LHS type
   */
  protected final void emit_checkcast_resolvedClass(VM_Type type) {

  }

  /**
   * Emit code to implement the checkcast bytecode
   * @param type the LHS type
   */
  protected final void emit_checkcast_final(VM_Type type) {

  }

  /**
   * Emit code to implement the instanceof bytecode
   * @param typeRef the LHS type
   */
  protected final void emit_instanceof(VM_TypeReference typeRef) {
	  // TODO !! VM._assert(NOT_REACHED);
  	
  	int tableOffset = VM_SubArchBootRecord.addFakeTrapStr(VM_Magic.objectAsAddress(VM_Atom.findOrCreateAsciiAtom("instanceof attempted").getBytes()));
  	asm.emitILW(S1, tableOffset);
    asm.emitFakeTrapStr(S0, S1);
  	emit_trap(SubordinateArchitecture.VM_TrapConstants.TRAP_UNIMPLEMENTED_METHOD);
  
  }

  /**
   * Emit code to implement the instanceof bytecode
   * @param type     The LHS type
   */
  protected final void emit_instanceof_resolvedClass(VM_Type type) {
    // TODO !! VM._assert(NOT_REACHED);
  	
  	int tableOffset = VM_SubArchBootRecord.addFakeTrapStr(VM_Magic.objectAsAddress(VM_Atom.findOrCreateAsciiAtom("instanceof resolved class attempted").getBytes()));
  	asm.emitILW(S1, tableOffset);
    asm.emitFakeTrapStr(S0, S1);
  	emit_trap(SubordinateArchitecture.VM_TrapConstants.TRAP_UNIMPLEMENTED_METHOD);
  }

  /**
   * Emit code to implement the instanceof bytecode
   * @param type     The LHS type
   */
  protected final void emit_instanceof_final(VM_Type type) {
    // TODO !! VM._assert(NOT_REACHED);
  	
  	int tableOffset = VM_SubArchBootRecord.addFakeTrapStr(VM_Magic.objectAsAddress(VM_Atom.findOrCreateAsciiAtom("instanceof final attempted").getBytes()));
  	asm.emitILW(S1, tableOffset);
    asm.emitFakeTrapStr(S0, S1);
  	emit_trap(SubordinateArchitecture.VM_TrapConstants.TRAP_UNIMPLEMENTED_METHOD);
  }

  /**
   * Emit code to implement the monitorenter bytecode
   */
  protected final void emit_monitorenter() {
  	genSaveStackTopTemp();	
  	VM_SubArchEntrypoints.OffsetTuple offsets = VM_SubArchEntrypoints.entryPointOffsets.get(VM_Entrypoints.lockMethod);
    asm.emitILW(T5, offsets.methodOffset.toInt());
    asm.emitILW(T6, offsets.sizeOffset.toInt());
    asm.emitORI(T4, JTOC, 0x0);

  	genMethodCacheLookup(S5, T7, T5);  

    peekWord(T0, 0);
    
  	asm.emitBISL(LINK_REG, S5);                                  // out of line lock
    genRestoreStackTopTemp();
    discardSlot();
  }

  /**
   * Emit code to implement the monitorexit bytecode
   */
  protected final void emit_monitorexit() {
  	genSaveStackTopTemp();	
  	VM_SubArchEntrypoints.OffsetTuple offsets = VM_SubArchEntrypoints.entryPointOffsets.get(VM_Entrypoints.unlockMethod);
  	asm.emitILW(T5, offsets.methodOffset.toInt());
    asm.emitILW(T6, offsets.sizeOffset.toInt());
    asm.emitORI(T4, JTOC, 0x0);

  	genMethodCacheLookup(S5, T7, T5);  
  	
    peekWord(T0, 0);
    
  	asm.emitBISL(LINK_REG, S5);                                  // out of line lock
    genRestoreStackTopTemp();
    discardSlot();
  }

  protected final void emit_trap(int trapCode) {
  	asm.emitIL(T0, 1);
  	asm.emitTRAP(T0, true, trapCode);
  }

  private final Offset getFieldTocOffset(VM_Field field) {
  	VM_Class klass = field.getDeclaringClass();
  	if (field.getType().isReferenceType()) {
  		return Offset.fromIntSignExtend((klass.getSubArchTocIdx().toInt() * 2) + STATIC_TOC_JTOC_OFF + BYTES_IN_INT);
  	} else {
  		return Offset.fromIntSignExtend((klass.getSubArchTocIdx().toInt() * 2) + STATIC_TOC_JTOC_OFF);
  	}
  }
  
  private final Offset getMethodTibOffset(VM_Method method) {
  	VM_Class klass = method.getDeclaringClass();
  	return klass.getSubArchTocIdx().plus(TIB_TABLE_JTOC_OFF);
  }
  
  // offset of i-th local variable with respect to FP
  private int localOffset(int i) {
    int offset = localStartOffset - (i << LOG_BYTES_IN_STACKSLOT);
    if (VM.VerifyAssertions) VM._assert(offset < 0x8000);
    return offset;
  }

  @Uninterruptible
  public static boolean isRegister(int location) {
    return location > 0;
  }

  @Uninterruptible
  public static int locationToOffset(int location) {
    return -location;
  }

  @Uninterruptible
  public static int offsetToLocation(int offset) {
    return -offset;
  }
  
  /**
  *
  The workhorse routine that is responsible for copying values from
  one slot to another. Every value is in a <i>location</i> that
  represents either a numbered register or an offset from the frame
  pointer (registers are positive numbers and offsets are
  negative). This method will generate register moves, memory stores,
  or memory loads as needed to get the value from its source location
  to its target. This method also understands how to do a few conversions
  from one type of value to another (for instance float to word).
  *
  * @param srcType the type of the source (e.g. <code>INT_TYPE</code>)
  * @param src the source location
  * @param destType the type of the destination
  * @param dest the destination location
  */
 @Inline
 private void copyByLocation(boolean isLong, int src, int dest) {
   if (src == dest) {
     return;
   }

   boolean srcIsRegister = isRegister(src);
   boolean destIsRegister = isRegister(dest);

   if (!srcIsRegister) src = locationToOffset(src);
   if (!destIsRegister) dest = locationToOffset(dest);

   if (srcIsRegister) {
  	 if (destIsRegister) {
  		 // register to register move
  		 asm.emitORI(dest, src, 0x0);
  	 } else {
  		 if (isLong) {
  			 pokeLongIdxInBytes(src, dest - spTopOffset);
  		 } else {
  			 pokeWordIdxInBytes(src, dest - spTopOffset);
  		 }
  	 }
   } else {
  	 if (destIsRegister) {
  		 if (isLong) {
  			 peekLongIdxInBytes(dest, src - spTopOffset);
  		 } else {
  			 peekWordIdxInBytes(dest, src - spTopOffset);
  		 }
  	 } else {
  		 if (isLong) {
  			 peekLongIdxInBytes(S0, src - spTopOffset);
  			 pokeLongIdxInBytes(S0, dest - spTopOffset);
  		 } else {
  			 peekWordIdxInBytes(S0, src - spTopOffset);
  			 pokeLongIdxInBytes(S0, dest - spTopOffset);
  		 }
  	 }
   }
 }

  private void emitDynamicLinkingSequence(int reg, VM_MemberReference ref, boolean couldBeZero) {
  	VM._assert(NOT_REACHED);    
  }

  private void emitDynamicLinkingSequence(int reg, int instanceSizeReg, VM_MemberReference ref, boolean couldBeZero) {
  	// return size in instanceSizeReg
  	VM._assert(NOT_REACHED);    
  }
  
  private void genCacheFlush() {
  	asm.emitBRASL(LINK_REG, VM_OutOfLineMachineCode.flushCacheInstructions);
  }
  
  /** 
   * Performs a division of nominator/denominator returning quotient
   * Note: destroys S0, S1, S2, S3, S4 and S5
   * 
   * @param quotient register which holds quotient on return
   * @param remainder register which holds remainder on return (if positive when passed)
   * @param nominator register which holds nominator (destroyed)
   * @param denominator register which holds denominator (destroyed)
   */
  private void genDiv(int quotient, int remainder, int nominator, int denominator) {
  	
  	// check for divide by zero	
  	asm.emitTRAP(denominator, false, VM_TrapConstants.TRAP_DIVIDE_BY_ZERO);
  	
  	// TODO - add hint for branch
  	
  	// abs(nominator) and abs(denominator)
  	asm.emitSFI(S2, nominator, 0);
  	asm.emitSFI(S3, denominator, 0);
  	asm.emitCGTI(S4, nominator, -1);
  	asm.emitCGTI(S5, denominator, -1);
  	asm.emitSELB(nominator, S2, nominator, S4);
  	asm.emitSELB(denominator, S3, denominator, S5);
  	asm.emitXOR(S5, S4, S5);
  	
  	// setup initial variables
  	asm.emitIL(S0, 1);
  	asm.emitIL(quotient, 0);
  	asm.emitCLZ(S2, nominator);
  	asm.emitCLZ(S3, denominator);
  	asm.emitSF(S1, S2, S3);
  	asm.emitSHL(S0, S0, S1);  // curr_bit
  	asm.emitSHL(denominator, denominator, S1);  // curr_subtractor
  	
  	// start subtraction loop
  	int loopStart = asm.getMachineCodeIndex();
  	asm.emitOR(S2, quotient, S0);                   // tmp_quotient = quotient | curr_bit
  	asm.emitROTQMBII(S0, S0, -1);                   // curr_bit >>= 1
  	asm.emitCLGT(S1, denominator, nominator);
  	asm.emitSF(S3, denominator, nominator);         // tmp_rem = curr_rem - curr_subtractor
  	asm.emitSELB(nominator, S3, nominator, S1);     // curr_rem = (curr_rem < curr_subtractor) ? curr_rem : tmp_rem
  	asm.emitROTQMBII(denominator, denominator, -1); // curr_subtractor <<=1
  	asm.emitSELB(quotient, S2, quotient, S1);       // quotient = (curr_rem < curr_subtractor) ? quotient : tmp_quotient
  	asm.emitBRNZ(S0, loopStart);
  	

  	// correct sign on output values
  	asm.emitSFI(S0, quotient, 0);
  	asm.emitSELB(quotient, quotient, S0, S5);
  	
  	if (remainder >= 0) { // if we want the remainder too
  		asm.emitSFI(S1, nominator, 0);
  		asm.emitSELB(remainder, S1, nominator, S4);
  	}
  }
  
  /** 
   * Generates code which performs a Long Division
   * 
   * Destroys S0, S1, S2, S3, S4, S5, a, b and tmp
   */
  private void genLongMul(int result, int a, int b, int tmp) {
  	// rotate longs to allow correct multiplication using 16 bit multiplies
  	asm.emitROTQBYI(S2, a, 8);
  	asm.emitROTQBYI(S3, b, 8);
  	asm.emitSHLQBYI(S0, S2, 12);
  	asm.emitSHLQBYI(S1, S3, 12);
  	asm.emitROTMI(S2, S0, -16);
  	asm.emitROTMI(S3, S1, -16);
  	
  	// perform partial multiplications
  	asm.emitMPYU(S4, S2, S1);
  	asm.emitMPYU(S5, S0, S3);
  	
  	asm.emitA(result, S4, S5);
  	
  	asm.emitMPYU(S5, S2, S3);
  	asm.emitMPYU(S2, S0, S1);
  	
  	asm.emitROTMI(S3, S2, -16);
  	asm.emitA(result, result, S3);

  	asm.emitMPYH(S3, S1, a);
  	asm.emitMPYH(tmp, a, S1); 	
  	asm.emitA(tmp, tmp, S3); 	
  	asm.emitMPYU(S3, S1, a);
  	asm.emitA(tmp, tmp, S3);
  	
  	asm.emitILA(a, 0x10000);
  	asm.emitA(S1, S5, a);
  	asm.emitILA(a, 0xffff);
  	asm.emitAND(S2, S2, a);
  	
  	asm.emitCLGT(S3, S4, result);
  	asm.emitSELB(S5, S5, S0, S3);
  	
  	asm.emitROTMI(S1, result, -16);
  	asm.emitSHLI(S4, result, 16);
  	
  	asm.emitMPYH(result, S0, b);
  	asm.emitMPYH(a, b, S0);
  	asm.emitMPYU(b, S0, b);
  	
  	asm.emitA(S5, S5, S1);
  	asm.emitA(S4, S4, S2);
  	asm.emitA(result, result, a);
  	asm.emitA(result, result, b);

  	asm.emitCWD(a, JTOC, 0);
  	asm.emitCWD(b, JTOC, 4);

  	asm.emitSHUFB(S5, S5, S5, a);
  	asm.emitSHUFB(S5, S4, S5, b);
  	asm.emitA(S2, S5, result);
  	asm.emitA(result, tmp, S2);
  	asm.emitSHUFB(result, result, S5, a);
  }
  
  /** 
   * Performs a long division of nominator/denominator returning quotient
   * Note: destroys S0, S1, S2, S3, S4 and S5
   * 
   * @param quotient register which holds quotient on return
   * @param remainder register which holds remainder on return
   * @param nominator register which holds nominator (destroyed)
   * @param denominator register which holds denominator (destroyed)
   */
  private void genLongDiv(int quotient, int remainder, int nominator, int denominator) {
  	// check for divide by zero
	  asm.emitSHLQBYI(S0, denominator, BYTES_IN_WORD);
	  asm.emitOR(S0, S0, denominator);
	  asm.emitTRAP(S0, false, VM_TrapConstants.TRAP_DIVIDE_BY_ZERO);
  	
  	// TODO - add hint for branch
  	
  	// abs(nominator) and abs(denominator)
  	asm.emitIL(S0, 0);
    asm.emitBG(S2, nominator, S0);
    asm.emitSHLQBYI(S2, S2, BYTES_IN_INT);  // shift borrow to next word up
    asm.emitSFX(S2, nominator, S0);

    asm.emitBG(S3, denominator, S0);
    asm.emitSHLQBYI(S3, S3, BYTES_IN_INT);  // shift borrow to next word up
    asm.emitSFX(S3, denominator, S0);  	
    
  	asm.emitCGTI(S4, nominator, -1);
  	asm.emitCGTI(S5, denominator, -1);
  	asm.emitFSM(S4, S4);
  	asm.emitFSM(S5, S5);  	
  	
  	asm.emitSELB(nominator, S2, nominator, S4);
  	asm.emitSELB(denominator, S3, denominator, S5);
  	asm.emitXOR(S5, S4, S5);
  	
  	// setup initial variables
  	asm.emitCLZ(S2, nominator);
  	asm.emitCLZ(S3, denominator);
  	asm.emitCEQI(S0, S2, 32);
  	asm.emitCEQI(S1, S3, 32);
  	asm.emitIL(remainder, 32);
  	asm.emitXSWD(remainder, remainder);
  	asm.emitA(S2, S2, remainder);   // add 32 to second word
  	asm.emitA(S3, S3, remainder);   // add 32 to second word
  	asm.emitSHLQBYI(remainder, S2, 4);
  	asm.emitSHLQBYI(quotient, S3, 4);
  	asm.emitSELB(S2, S2, remainder, S0);
  	asm.emitSELB(S3, S3, quotient, S1);
  	asm.emitSF(S1, S2, S3);
  	
  	asm.emitIL(S0, 1);
  	asm.emitXSWD(S0, S0);  // make S0 = 1l;

  	asm.emitIL(S2, 7);
  	
  	// note SHLQBI can only shift up to 7 bits at a time, so do SHLQBYBI first
  	asm.emitSHLQBYBI(S0, S0, S1);  // curr_bit
  	asm.emitSHLQBYBI(denominator, denominator, S1);  // curr_subtractor
  	asm.emitSHLQBI(S0, S0, S1);  // curr_bit
  	asm.emitSHLQBI(denominator, denominator, S1);  // curr_subtractor

  	asm.emitIL(quotient, 0);
  	
  	// start subtraction loop
  	int loopStart = asm.getMachineCodeIndex();

  	asm.emitCLGT(S1, denominator, nominator);
  	asm.emitCEQ(S3, denominator, nominator);
  	asm.emitSHLQBYI(S2, S1, BYTES_IN_INT);
  	asm.emitAND(S3, S2, S3);   // S3 = HI_1=HI_2 && L0_1>LO_2
  	asm.emitOR(S1, S1, S3);    // S1 = HI_1>HI_2 || (HI_1=HI_2 && L0_1>LO_2)
  	asm.emitFSM(S1, S1);      // expand across quadword
  	
  	asm.emitOR(S2, quotient, S0);                   // tmp_quotient = quotient | curr_bit
  	asm.emitROTQMBII(S0, S0, -1);                   // curr_bit >>= 1

    asm.emitBG(S3, denominator, nominator);
    asm.emitSHLQBYI(S3, S3, BYTES_IN_INT);    // shift borrow to next word up
    asm.emitSFX(S3, denominator, nominator);  // tmp_rem = curr_rem - curr_subtractor
          
  	asm.emitSELB(nominator, S3, nominator, S1);     // curr_rem = (curr_rem < curr_subtractor) ? curr_rem : tmp_rem
  	asm.emitROTQMBII(denominator, denominator, -1); // curr_subtractor <<=1
  	asm.emitSELB(quotient, S2, quotient, S1);       // quotient = (curr_rem < curr_subtractor) ? quotient : tmp_quotient
  	
  	asm.emitSHLQBYI(S2, S0, 4);                   
  	asm.emitOR(S2, S2, S0);
  	asm.emitBRNZ(S2, loopStart);

  	// correct sign on output values
  	asm.emitIL(S0, 0);
    asm.emitBG(S1, quotient, S0);
    asm.emitSHLQBYI(S1, S1, BYTES_IN_INT);  // shift borrow to next word up
    asm.emitSFX(S1, nominator, S0);
  	asm.emitSELB(quotient, quotient, S1, S5);
  	
  	if (remainder >= 0) { // if we want the remainder too
      asm.emitBG(S1, nominator, S0);
      asm.emitSHLQBYI(S1, S1, BYTES_IN_INT);  // shift borrow to next word up
      asm.emitSFX(S1, nominator, S0);
  		asm.emitSELB(remainder, S1, nominator, S4);
  	}
  }
  
  /**
   * Generates code which takes the long in T0 and calculates the approximate double value corresponding to that in T2
   */
	private void genl2d() {
		asm.emitLoadDouble(T4, JTOC, VM_Entrypoints.Long2DoubleExpDef.getSubArchOffset());
    asm.emitLoadDouble(T5, JTOC, VM_Entrypoints.Long2DoubleShfCtrl.getSubArchOffset());  // TODO - check that entry point is aligned correctly
    asm.emitLoadDouble(S0, JTOC, VM_Entrypoints.Long2DoubleShfCtrl2.getSubArchOffset());  // TODO - check that entry point is aligned correctly
    
  	// abs(T0)
  	asm.emitIL(T3, 0);
  	asm.emitCGTI(T1, T0, -1);
  	asm.emitBG(T2, T0, T3);
  	asm.emitSHLQBYI(T2, T2, BYTES_IN_INT);
  	asm.emitSFX(T2, T0, T3);
  	asm.emitFSM(T1,T1);
  	asm.emitSELB(T0, T2, T0, T1);
  	
  	// get sign bit
  	asm.emitFSMBI(T3, 0x8080);
  	asm.emitANDBI(T3, T3, 0x80);
  	asm.emitANDC(T1, T3, T1);
  	
  	// put together shiftCtrl
  	asm.emitCDD(T2, JTOC, BYTES_IN_DOUBLE);
  	asm.emitSHUFB(T5, S0, T5, T2);
  	
  	// create double
  	asm.emitCLZ(T2, T0);
  	asm.emitSHL(T0, T0, T2);
  	asm.emitCEQI(T3, T2, 32);
  	asm.emitSF(T2, T2, T4);
  	asm.emitA(T0, T0, T0);
  	asm.emitANDC(T2, T2, T3);
  	asm.emitSHUFB(T4, T2, T0, T5);
  	asm.emitSHLQBII(T0, T4, 4);
  	asm.emitSHLQBYI(T3, T0, 8);
  	asm.emitDFA(T2, T0, T3);
  	
  	// add sign bit
  	asm.emitOR(T2, T2, T1);
	}
  
	/** Returns address of cached statics into staticsAddrReg */
	private void genStaticsLookup(int staticsAddrReg, int tocOffsetReg) {
		// get the static offset for this classes static block
		asm.emitLoad(staticsAddrReg, JTOC, tocOffsetReg);
		// check if statics are cached (i.e. the reference is in Low memory)
  	asm.emitROTMI(S1, staticsAddrReg, -20);
	  asm.emitILA(S0, VM_OutOfLineMachineCode.cacheStaticInstructions);
	  asm.emitLoadLR(LINK_REG); // Save rough return address (corrected in trap before returning) 
	
	  if (VM.VerifyAssertions) VM._assert(staticsAddrReg == T4 && tocOffsetReg == T5);
	  asm.emitBINZ(S1, S0);
	}
	
	/** Returns address of cached class TIB into tibAddrReg */
	private void genClassTibLookup(int tibAddrReg, int tocOffsetReg) {
		// get the class tib offset for this classes tib
		asm.emitLoad(tibAddrReg, JTOC, tocOffsetReg);
		// check if statics are cached (i.e. the reference is in Low memory)
  	asm.emitROTMI(S1, tibAddrReg, -20);
	  asm.emitILA(S0, VM_OutOfLineMachineCode.cacheClassTibInstructions);
	  asm.emitLoadLR(LINK_REG); // Save rough return address (corrected in trap before returning) 
	
	  if (VM.VerifyAssertions) VM._assert(tibAddrReg == T7 && tocOffsetReg == T5);
	  asm.emitBINZ(S1, S0);
	}
	
  /**
   * Generates a lookup for a method reference.  
   */
  private void genMethodCacheLookup(int methodRefReg, int staticsAddrReg, int methodOffsetReg) {
	  // Load method reference
    asm.emitLoad(methodRefReg, staticsAddrReg, methodOffsetReg);
    // check if method is cached in local memory
    asm.emitROTMI(S1, methodRefReg, -20);
	  asm.emitILA(S0, VM_OutOfLineMachineCode.cacheMethodInstructions);
	  asm.emitLoadLR(LINK_REG); // Save rough return address (corrected in trap before returning) 
		
	  if (VM.VerifyAssertions) VM._assert(methodRefReg == S5 && staticsAddrReg == T7 && methodOffsetReg == T5);
	  asm.emitBINZ(S1, S0);
  }
  
  private void genObjectCacheTableLookup(int objectRef, int cacheEntryIndex, int cachedAddr) {
  	// TODO - See if we can do this better
  	// TODO - check size of object is less than 2^14
  	
  	// hash reference
  	asm.emitROTMI(S0, objectRef, -10);  // Xor bits 3-12 with 13-25										
  	asm.emitXOR(S1, S0, objectRef);     // to index a 2 word (8byte) hashtable.	
  	asm.emitILW(S3, 0x1ff8);
  	asm.emitAND(cacheEntryIndex, S1, S3);
  	asm.emitILW(S4, OBJECT_CACHE_TABLE);
  	asm.emitLoad(cachedAddr, S4, cacheEntryIndex);
  }
  
  /**
   * Generate code to perform a lookup of the object reference in the 
   * local memory cache, replacing the main memory reference with the 
   * local memory reference.  If the object is not in the cache, then
   * it is loaded into the cache.
   * 
   * @param objectRef Register containing main memory address, replaced with local
   * memory address at end of generated code.
   * @param instanceSizeReg Register holding the size of the instance object
   */
  private void genObjectCacheLookup(int lookupRef, int objectRef, int instanceSizeReg) {

  	genObjectCacheTableLookup(objectRef, S6, S3);
  	
  	// check if entry is cached
  	asm.emitCEQ(S2, objectRef, S3);
  	
  	// shuffle cached address / size into object reference
  	asm.emitROTQBYI(lookupRef, S3, BYTES_IN_INT);

  	// check if cached entry is the right size
  	asm.emitROTMI(S1, lookupRef, -18);	
  	asm.emitCGT(S0, instanceSizeReg, S1);
  	
  	// combine both tests into a single value
  	asm.emitANDC(S2, S2, S0);
  	
  	// objectRef in T1 and instance size in T2 for trap
  	if (VM.VerifyAssertions) VM._assert(objectRef == T4 && instanceSizeReg == T2 && lookupRef == T1);
  	
  	// if not, move the object into the cache
  	asm.emitILA(S0, VM_OutOfLineMachineCode.cacheObjectInstructions);
  	asm.emitLoadLR(LINK_REG); // Save rough return address (corrected in trap before returning) 
  	asm.emitBIZ(S2, S0);
  }
  
  // Gen bounds check for array load/store bytecodes.
  // Does implicit null check and array bounds check.
  // Bounds check can always be implicit becuase array length is at negative offset from obj ptr.
  // Kills S0, S2 and S3
  private void genBoundsCheck(int arrayTableAddr, int index) {
    asm.emitLoad(S0, arrayTableAddr, VM_ObjectModel.getArrayLengthOffset());
    asm.emitCGT(S3, S0, index);      // Trap if index >= length
  	asm.emitTRAP(S3, false, VM_TrapConstants.TRAP_ARRAY_BOUNDS);
  }

	private void genArrayTableLookup(int objectRef) {
		genObjectCacheTableLookup(objectRef, S6, S3);   // lookup object reference in cache table

  	// check if array entry is cached
  	asm.emitCEQ(S1, objectRef, S3);
  	// shuffle cached address into object reference
  	asm.emitROTQBYI(T5, S3, BYTES_IN_INT);
  	
  	if (VM.VerifyAssertions) VM._assert(objectRef == T0);
  	// if not, move the array table into the cache
  	asm.emitILA(S0, VM_OutOfLineMachineCode.cacheArrayInstructions);
  	asm.emitLoadLR(LINK_REG);
  	asm.emitBIZ(S1, S0);
	}

  /**
   * Array index is modified to byte index
   */
  private void genArrayCacheLookup(int lookupRef, int objectRef, int arrayIndex, int arrayInstanceSizeLog) {
  	// TODO, hints for branches!!
  	
  	genArrayTableLookup(objectRef);  	
  	// array table of contence now in local memory at T5
  	
  	// do a bounds check
  	genBoundsCheck(T5, arrayIndex);
  	
  	// check if required block of array is loaded
  	asm.emitROTMI(S7, arrayIndex, (-LOG_ARRAY_BLOCK_ENTRIES));
  	asm.emitLoad(lookupRef, T5, S7);
  	
  	if (VM.VerifyAssertions) VM._assert(lookupRef == T3 && arrayIndex == T1);

  	asm.emitILW(S8, arrayInstanceSizeLog);
  	
  	// if not, move the array block into the cache
  	asm.emitILA(S0, VM_OutOfLineMachineCode.cacheArrayBlockInstructions);
  	asm.emitLoadLR(LINK_REG);
  	asm.emitBIZ(lookupRef, S0);
  	
  	// lookup ref points to block of array required
  }
  
  // Emit code to buy a stackframe, store incoming parameters,
  // and acquire method synchronization lock.
  //
  private void genPrologue() {

  	if (klass.hasBridgeFromNativeAnnotation()) {
      VM._assert(false); // TODO - deal with native bridge nicely
    }

    // Generate trap if new frame would cross guard page.
    //
    if (isInterruptible) {
      asm.emitStackOverflowCheck(frameSize);                            // clobbers R0, S0
    }

    // Buy frame.
    asm.emitStore(FP, FP, Offset.fromIntSignExtend(-frameSize));  // save old FP & buy new frame 
    asm.emitAI(FP, FP, -frameSize);
    		
    // Fill in frame header.
    asm.emitILA(S0, compiledMethod.getId());
    asm.emitStore(S0, FP, Offset.fromIntSignExtend(STACKFRAME_METHOD_ID_OFFSET)); // save compiled method id
    asm.emitStore(LINK_REG, FP, Offset.fromIntSignExtend(frameSize + STACKFRAME_NEXT_INSTRUCTION_OFFSET)); // save LR !!TODO: handle discontiguous stacks when saving return address

	    
    // If this is a "dynamic bridge" method, then save all registers except GPR0, FPR0, JTOC, and FP.
    //
    if (klass.hasDynamicBridgeAnnotation()) {
      VM._assert(false); // TODO - deal with dynamic bridge nicely
    } else {
      // Restore non-volatile registers.
      int offset = frameSize;
      
      asm.emitCWD(S0, JTOC, BYTES_IN_INT);
      asm.emitCDD(S1, JTOC, BYTES_IN_LONG);
      int j = FIRST_FIXED_LOCAL_REGISTER;
      
      for (int i = FIRST_FIXED_LONG_LOCAL_REGISTER; i <= lastLongFixedStackRegister; i+=2) {
    		offset -= BYTES_IN_QUAD;
      	if ((i + 2) <= lastLongFixedStackRegister) {
      		asm.emitSHUFB(S3, i+1, i, S1);
      		asm.emitSTQD(S3, FP, offset >> 4);
      	} else {
      		if (j > lastFixedStackRegister) {
        		asm.emitSTQD(i, FP, offset >> 4);
      		} else if (j+1 > lastFixedStackRegister) {
        		asm.emitSHUFB(S3, j, i, S1);
        		asm.emitSTQD(S3, FP, offset >> 4);
	      		j += 1;
      		} else {
	      		asm.emitSHUFB(S4, j+1, j, S0);
	      		asm.emitSHUFB(S3, S4, i, S1);
	      		asm.emitSTQD(S3, FP, offset >> 4);
	      		j += 2;
      		}
      	}
      }
      for (; j <= lastFixedStackRegister; j+=4) {
    		offset -= BYTES_IN_QUAD;
      	if (j+1 > lastFixedStackRegister) {
      		asm.emitSTQD(j, FP, offset >> 4);
      	} else {
	      	asm.emitSHUFB(S3, j+1, j, S0);
	      	if (j+2 > lastFixedStackRegister) {
		    		asm.emitSTQD(S3, FP, offset >> 4);
	      	} else if (j+3 > lastFixedStackRegister) {
		      	asm.emitSHUFB(S5, j+2, S3, S1);
		    		asm.emitSTQD(S5, FP, offset >> 4);
	      	} else {
	      		asm.emitSHUFB(S4, j+3, j+2, S0);
		      	asm.emitSHUFB(S5, S4, S3, S1);
		    		asm.emitSTQD(S5, FP, offset >> 4);
	      	}
      	}
      }
    }

    // load temp_stack_top
    // asm.emitLQD(STACK_TOP_TEMP, FP, (spTopOffset >> 4));

    // Setup locals.
    //
    genMoveParametersToLocals();                  // move parameters to locals

    // Perform a thread switch if so requested.
    /* defer generating prologues which may trigger GC, see emit_deferred_prologue*/
    if (method.isForOsrSpecialization()) {
      return;
    }

    genThreadSwitchTest(VM_Thread.PROLOGUE); //  (VM_BaselineExceptionDeliverer WONT release the lock (for synchronized methods) during prologue code)

    
    // Acquire method syncronization lock.  (VM_BaselineExceptionDeliverer will release the lock (for synchronized methods) after  prologue code)
    //
    if (method.isSynchronized()) {
      genSynchronizedMethodPrologue();

    }
    
    // TODO - preload statics potentially
    // TODO - find a better way to do this
  	VM_HashSet<VM_Type> classesReq = new VM_HashSet<VM_Type>();
    // load any class statics which are needed
    for (VM_MemberReference staticMemberRef: this.method.getStaticMemberRefs()) {
    	if (staticMemberRef.isFieldReference()) {
    		VM_Field field = staticMemberRef.asFieldReference().peekResolvedField(false);
    		if (field != null) {
    			classesReq.add(field.getDeclaringClass());
    		} else {
	    		// TODO - do some dynamic resolution here or something
	    		VM.sysWriteln("field Ref is " + staticMemberRef);
	    		VM._assert(NOT_REACHED);
    		}
    	} else if (staticMemberRef.isMethodReference()) {
    		VM_Method method = staticMemberRef.asMethodReference().peekResolvedMethod(false);
    		if (method != null) {
    			classesReq.add(method.getDeclaringClass());
    		} else {
	    		// TODO - do some dynamic resolution here or something
	    		VM.sysWriteln("method Ref is " + staticMemberRef);
	    		VM._assert(NOT_REACHED);
    		}
    	}
    }
	    
	  for (VM_Type staticType: classesReq) {
  		if (!staticType.isResolved(true)) {
  			staticType.load(true);
  			staticType.resolve(true);
  		}
	  }
  }

  protected final void emit_deferred_prologue() {
    if (VM.VerifyAssertions) VM._assert(method.isForOsrSpecialization());
    genThreadSwitchTest(VM_Thread.PROLOGUE);

    /* donot generate sync for synced method because we are reenter
     * the method in the middle.
     */
    //  if (method.isSymchronized()) genSynchronizedMethodPrologue();
  }

  // Emit code to acquire method synchronization lock.
  //
  private void genSynchronizedMethodPrologue() {  
    genSaveStackTopTemp();	
  	VM_SubArchEntrypoints.OffsetTuple offsets = VM_SubArchEntrypoints.entryPointOffsets.get(VM_Entrypoints.lockMethod);
    asm.emitILW(T5, offsets.methodOffset.toInt());
    asm.emitILW(T6, offsets.sizeOffset.toInt());
    asm.emitORI(T7, JTOC, 0x0);

  	genMethodCacheLookup(S5, T7, T5);
  	
  	if (method.isStatic()) { // put java.lang.Class object into T0
      Offset klassOffset = Offset.fromIntSignExtend(VM_SubArchStatics.findOrCreateObjectLiteral(klass.getClassForType()));
      asm.emitLoad(T0, JTOC, klassOffset);
    } else { // first local is "this" pointer
      copyByLocation(false, getGeneralLocalLocation(0), T0);
    }
    
  	asm.emitBISL(LINK_REG, S5);                                  // out of line lock
  	lockOffset = BYTES_IN_INT * (asm.getMachineCodeIndex() - 1); // after this instruction, the method has the monitor
    genRestoreStackTopTemp();
  }

  // Emit code to release method synchronization lock.
  //
  private void genSynchronizedMethodEpilogue() {
    genSaveStackTopTemp();
  	VM_SubArchEntrypoints.OffsetTuple offsets = VM_SubArchEntrypoints.entryPointOffsets.get(VM_Entrypoints.unlockMethod);
    asm.emitILW(T5, offsets.methodOffset.toInt());
    asm.emitILW(T6, offsets.sizeOffset.toInt());
    asm.emitORI(T7, JTOC, 0x0);

  	genMethodCacheLookup(S5, T7, T5);
  	
  	if (method.isStatic()) { // put java.lang.Class object into T0
      Offset klassOffset = Offset.fromIntSignExtend(VM_SubArchStatics.findOrCreateObjectLiteral(klass.getClassForType()));
      asm.emitLoad(T0, JTOC, klassOffset);
    } else { // first local is "this" pointer
      copyByLocation(false, getGeneralLocalLocation(0), T0);
    }
  	
  	asm.emitBISL(LINK_REG, S5);                                  // out of line unlock	
  	genRestoreStackTopTemp();
  }

	private void genRestoreStackTopTemp() {
		// restore stack top temp
		if (spTopOffset != operandStackStartOffset) {
			asm.emitLQD(STACK_TOP_TEMP, FP, ((spTopOffset & ~(BYTES_IN_QUAD - 1)) >> 4));
		}
	}

	private void genSaveStackTopTemp() {
		//  Save stack top temp
		if (spTopOffset != operandStackStartOffset) {
			asm.emitSTQD(STACK_TOP_TEMP, FP, ((spTopOffset & ~(BYTES_IN_QUAD - 1)) >> 4));
		}
	}

  // Emit code to discard stackframe and return to caller.
  //
  private void genEpilogue() {

  	
    if (klass.hasDynamicBridgeAnnotation()) {// Restore non-volatile registers.
      // we never return from a DynamicBridge frame
      VM._assert(false);
    } else {
      // Restore non-volatile registers.
      int offset = frameSize;
      
      int j = FIRST_FIXED_LOCAL_REGISTER;
      
      for (int i = FIRST_FIXED_LONG_LOCAL_REGISTER; i <= lastLongFixedStackRegister; i+=2) {
    		offset -= BYTES_IN_QUAD;
      	if ((i + 2) <= lastLongFixedStackRegister) {
      		asm.emitLQD(i, FP, offset >> 4);
      		asm.emitROTQBYI(i+1, i, BYTES_IN_LONG);
      	} else {
      		asm.emitLQD(i, FP, offset >> 4);
      		if (j <= lastFixedStackRegister) {
      			asm.emitROTQBYI(j, i, BYTES_IN_LONG);
      			j += 1;
      		}
      		if (j <= lastFixedStackRegister) {
      			asm.emitROTQBYI(j, i, 3*BYTES_IN_INT);
      			j += 1;
      		}
      	}
      }
      for (; j <= lastFixedStackRegister; j+=4) {
    		offset -= BYTES_IN_QUAD;
    		asm.emitLQD(j, FP, offset >> 4);
    		if (j+1 <= lastFixedStackRegister) {
    			asm.emitROTQBYI(j+1, j, BYTES_IN_INT);
    		}
    		if (j+2 <= lastFixedStackRegister) {
    			asm.emitROTQBYI(j+2, j, 2*BYTES_IN_INT);
    		}
    		if (j+3 <= lastFixedStackRegister) {
    			asm.emitROTQBYI(j+3, j, 3*BYTES_IN_INT);
    		}
      }

      if (frameSize <= 0x8000) {
        asm.emitAI(FP, FP, frameSize); // discard current frame
      	VM._assert((frameSize & (BYTES_IN_QUAD - 1)) == 0);
      } else {
        asm.emitLoad(FP, FP);          // discard current frame
      }
      asm.emitLoad(LINK_REG, FP, Offset.fromIntSignExtend(STACKFRAME_NEXT_INSTRUCTION_OFFSET));
      
      asm.emitBI(LINK_REG);  // branch always, through link register
    }
  }

  /**
   * Emit the code for a bytecode level conditional branch
   * @param cc the condition code to branch on
   * @param bTarget the target bytecode index
   */
  private void genCondBranch(int sourceReg, boolean cc, int bTarget) {

    if (options.EDGE_COUNTERS) {
    	// TODO - edge counters
    	VM._assert(false);
    } else {
      if (bTarget - SHORT_FORWARD_LIMIT < biStart) {
      	if (cc) {
      		asm.emitBRNZ(sourceReg, bytecodeMap[bTarget], bTarget);
      	} else {
      		asm.emitBRZ(sourceReg, bytecodeMap[bTarget], bTarget);
      	}
      } else {
      	asm.emitILA(S0, bytecodeMap[bTarget], bTarget);
      	if (cc) {
      		asm.emitBRNZ(sourceReg, bytecodeMap[bTarget], bTarget);
      	} else {
      		asm.emitBRZ(sourceReg, bytecodeMap[bTarget], bTarget);
      	}
      }
    }
  }

  /**
   * increment an edge counter.
   * @param counters register containing base of counter array
   * @param scratch scratch register
   * @param counterIdx index of counter to increment
   */
  private void incEdgeCounter(int counters, int scratch, int counterIdx) {
  	// TODO - edge counters
  	VM._assert(false);
  }

  /**
   * @param whereFrom is this thread switch from a PROLOGUE, BACKEDGE, or EPILOGUE?
   */
  private void genThreadSwitchTest(int whereFrom) {
    if (isInterruptible) {
    	// TODO - thread switch
    }
  }

  // parameter stuff //

  // store parameters from registers into local variables of current method.

  private void genMoveParametersToLocals() {
  	int spillOff = frameSize + STACKFRAME_HEADER_SIZE;
    int gp = FIRST_VOLATILE_GPR;

    int localIndex = 0;
    int srcLocation;
    int dstLocation;

    if (!method.isStatic()) {
      if (gp > LAST_VOLATILE_GPR) {
        spillOff += BYTES_IN_STACKSLOT;
        srcLocation = offsetToLocation(spillOff);
      } else {
        srcLocation = gp++;
      }
      dstLocation = getGeneralLocalLocation(localIndex++);
      
      copyByLocation(false, srcLocation, dstLocation);
    }

    VM_TypeReference[] types = method.getParameterTypes();
    for (int i = 0; i < types.length; i++, localIndex++) {
      VM_TypeReference t = types[i];
      if (t.isLongType() || t.isDoubleType()) {
        dstLocation = getGeneralLocalLocation(localIndex++);
        if (gp > LAST_VOLATILE_GPR) {
          spillOff += 2 * BYTES_IN_STACKSLOT;
          srcLocation = offsetToLocation(spillOff);
        } else {
          srcLocation = gp++;	
        }
        copyByLocation(true, srcLocation, dstLocation);
      } else { // t is float, int-like or object reference 
        dstLocation = getGeneralLocalLocation(localIndex);
        if (gp > LAST_VOLATILE_GPR) {
          spillOff += BYTES_IN_STACKSLOT;
          srcLocation = offsetToLocation(spillOff);
        } else {
          srcLocation = gp++;
        }
        copyByLocation(false, srcLocation, dstLocation);
      }
    }
  }

  // load parameters into registers before calling method "m".
  private void genMoveParametersToRegisters(boolean hasImplicitThisArg, VM_MethodReference m) {
    spillOffset = STACKFRAME_HEADER_SIZE;
  	int gp = FIRST_VOLATILE_GPR;
    int stackIndex = m.getParameterWords();
    if (hasImplicitThisArg) {
      if (gp > LAST_VOLATILE_GPR) {
        genSpillSlot(stackIndex);
      } else {
        peekWord(gp++, stackIndex);
      }
    }
    for (VM_TypeReference t : m.getParameterTypes()) {
      if (t.isLongType() || t.isDoubleType()) {
        stackIndex -= 2;
        if (gp > LAST_VOLATILE_GPR) {
          genSpillDoubleSlot(stackIndex);
        } else {
        	peekLong(gp, stackIndex);
        	gp++;
        }
      } else { // if (t.isFloatType() || t.isIntLikeType() || t.isReferenceType()) {
        stackIndex -= 1;
        if (gp > LAST_VOLATILE_GPR) {
          genSpillSlot(stackIndex);
        } else {
          peekWord(gp++, stackIndex);
        }
      }
    }
    if (VM.VerifyAssertions) VM._assert(stackIndex == 0);
    genSaveStackTopTemp();
  }

  // push return value of method "m" from register to operand stack.
  private void genPopParametersAndPushReturnValue(boolean hasImplicitThisArg, VM_MethodReference m) {
    genRestoreStackTopTemp();
  	VM_TypeReference t = m.getReturnType();
    discardSlots(m.getParameterWords() + (hasImplicitThisArg ? 1 : 0));
    if (!t.isVoidType()) {
      if (t.isLongType() || t.isDoubleType()) {
        pushLong(FIRST_VOLATILE_GPR);
      } else { //if (t.isFloatType() || t.isDoubleType() || t.isReference()) {
        pushWord(FIRST_VOLATILE_GPR);
      }
    }
  }

  private void genSpillSlot(int stackIndex) {
    peekWord(T0, stackIndex);
    asm.emitStore(T0, FP, spillOffset);
    spillOffset += BYTES_IN_STACKSLOT;
  }

  private void genSpillDoubleSlot(int stackIndex) {
    peekLong(T0, stackIndex);
    asm.emitStoreDouble(T0, FP, spillOffset);
    spillOffset += 2 * BYTES_IN_STACKSLOT;
  }

  protected final void emit_loadretaddrconst(int bcIndex) {
  	VM._assert(NOT_REACHED);
  }

  /**
   * Emit code to invoke a compiled method (with known jtoc offset).
   * Treat it like a resolved invoke static, but take care of
   * this object in the case.
   *
   * I have not thought about GCMaps for invoke_compiledmethod
   * TODO: Figure out what the above GCMaps comment means and fix it!
   */
  protected final void emit_invoke_compiledmethod(VM_CompiledMethod cm) {
  	VM._assert(NOT_REACHED);
  }

  protected final VM_ForwardReference emit_pending_goto(int bTarget) {
  	VM._assert(NOT_REACHED);
	  return null;
  }

  //*************************************************************************
  //                             MAGIC
  //*************************************************************************

  /*
   *  Generate inline machine instructions for special methods that cannot be
   *  implemented in java bytecodes. These instructions are generated whenever
   *  we encounter an "invokestatic" bytecode that calls a method with a
   *  signature of the form "static native VM_Magic.xxx(...)".
   *  23 Jan 1998 Derek Lieber
   *
   * NOTE: when adding a new "methodName" to "generate()", be sure to also
   * consider how it affects the values on the stack and update
   * "checkForActualCall()" accordingly.
   * If no call is actually generated, the map will reflect the status of the
   * locals (including parameters) at the time of the call but nothing on the
   * operand stack for the call site will be mapped.
   *  7 Jul 1998 Janice Shepherd
   */

  /** Generate inline code sequence for specified method.
   * @param methodToBeCalled method whose name indicates semantics of code to be generated
   * @return true if there was magic defined for the method
   */
  private boolean generateInlineCode(VM_MethodReference methodToBeCalled) {
  	VM_Atom methodName = methodToBeCalled.getName();

    if (methodToBeCalled.isSysCall()) {
  		VM._assert(NOT_REACHED);
  		return false;
    }
    
    if (methodToBeCalled.getType() == VM_TypeReference.Address) {
      // Address.xyz magic

      VM_TypeReference[] types = methodToBeCalled.getParameterTypes();

      // Loads all take the form:
      // ..., Address, [Offset] -> ..., Value

      if (methodName == VM_MagicNames.loadAddress ||
          methodName == VM_MagicNames.loadLocalAddress ||
          methodName == VM_MagicNames.loadObjectReference ||
          methodName == VM_MagicNames.loadWord) {

        if (types.length == 0) {
          popWord(T4);                  // pop base
          asm.emitILW(T2, BYTES_IN_ADDRESS); // just get what we need
          genObjectCacheLookup(T1, T4, T2);  // lookup object address in cache      
          asm.emitLoadUnaligned(T0, T1);    // *(base)
          pushWord(T0);                 // push *(base)
        } else {
          popWord(T1);                   // pop offset
          popWord(T4);                  // pop base
          asm.emitA(T4, T4, T1);
          asm.emitILW(T2, BYTES_IN_ADDRESS); // just get what we need
          genObjectCacheLookup(T1, T4, T2);  // lookup object address in cache    
          asm.emitLoadUnaligned(T0, T1);   // *(base+offset)
          pushWord(T0);                 // push *(base+offset)
        }
        return true;
      }

      if (methodName == VM_MagicNames.loadChar) {

        if (types.length == 0) {
        	popWord(T4);                  // pop base
          asm.emitILW(T2, BYTES_IN_CHAR); // just get what we need
          genObjectCacheLookup(T1, T4, T2);  // lookup object address in cache      
          asm.emitLoadCharUnaligned(T0, T1); // load with zero extension.
          pushWord(T0);                  // push *(base)
        } else {
          popWord(T1);                   // pop offset
          popWord(T4);                  // pop baseasm.emitA(T4, T4, T1);
          asm.emitA(T4, T4, T1);
          asm.emitILW(T2, BYTES_IN_CHAR); // just get what we need
          genObjectCacheLookup(T1, T4, T2);  // lookup object address in cache    
          asm.emitLoadCharUnaligned(T0, T1);   // *(base+offset)
          pushWord(T0);                  // push *(base+offset)
        }
        return true;
      }

      if (methodName == VM_MagicNames.loadShort) {

        if (types.length == 0) {
          popWord(T4);                  // pop base
          asm.emitILW(T2, BYTES_IN_SHORT); // just get what we need
          genObjectCacheLookup(T1, T4, T2);  // lookup object address in cache   
          asm.emitLoadShortUnaligned(T0, T1);       // load with sign extension.
          pushWord(T0);                  // push *(base)
        } else {
          popWord(T1);                   // pop offset
          popWord(T4);                  // pop base
          asm.emitA(T4, T4, T1);
          asm.emitILW(T2, BYTES_IN_SHORT); // just get what we need
          genObjectCacheLookup(T1, T4, T2);  // lookup object address in cache 
          asm.emitLoadShortUnaligned(T0, T1);       // load with sign extension.
          pushWord(T0);                  // push *(base+offset)
        }
        return true;
      }

      if (methodName == VM_MagicNames.loadByte) {
        if (types.length == 0) {
          popWord(T4);                  // pop base
          asm.emitILW(T2, BYTES_IN_BYTE); // just get what we need
          genObjectCacheLookup(T1, T4, T2);  // lookup object address in cache 
          asm.emitLoadByteUnaligned(T0, T1);       // load with sign extension.
          pushWord(T0);                  // push *(base)
        } else {
          popWord(T1);                   // pop offset
          popWord(T4);                  // pop base
          asm.emitA(T4, T4, T1);
          asm.emitILW(T2, BYTES_IN_BYTE); // just get what we need
          genObjectCacheLookup(T1, T4, T2);  // lookup object address in cache 
          asm.emitLoadByteUnaligned(T0, T1);       // load with sign extension.
          pushWord(T0);                  // push *(base+offset)
        }
        return true;
      }

      if (methodName == VM_MagicNames.loadInt || methodName == VM_MagicNames.loadFloat) {

        if (types.length == 0) {
          popWord(T4);                  // pop base
          asm.emitILW(T2, BYTES_IN_INT); // just get what we need
          genObjectCacheLookup(T1, T4, T2);  // lookup object address in cache 
          asm.emitLoadUnaligned(T0, T1);     // *(base)
          pushWord(T0);                  // push *(base)
        } else {
          popWord(T1);                   // pop offset
          popWord(T4);                  // pop base
          asm.emitA(T4, T4, T1);
          asm.emitILW(T2, BYTES_IN_INT); // just get what we need
          genObjectCacheLookup(T1, T4, T2);  // lookup object address in cache 
          asm.emitLoadUnaligned(T0, T1);    // *(base+offset)
          pushWord(T0);                  // push *(base+offset)
        }
        return true;
      }

      if (methodName == VM_MagicNames.loadDouble || methodName == VM_MagicNames.loadLong) {

        if (types.length == 0) {
          popWord(T4);                  // pop base
          asm.emitILW(T2, BYTES_IN_DOUBLE); // just get what we need
          genObjectCacheLookup(T1, T4, T2);  // lookup object address in cache 
          asm.emitLoadDoubleUnaligned(T0, T1);      // *(base)
          pushLong(T0);               // push double
        } else {
          popWord(T1);                   // pop offset
          popWord(T4);                  // pop base
          asm.emitA(T4, T4, T1);
          asm.emitILW(T2, BYTES_IN_DOUBLE); // just get what we need
          genObjectCacheLookup(T1, T4, T2);  // lookup object address in cache      
          asm.emitLoadDoubleUnaligned(T0, T1);    // *(base+offset)
          pushLong(T0);               // push *(base+offset)
        }
        return true;
      }
      
      //    Stores all take the form:
      // ..., Address, Value, [Offset] -> ...
      if (methodName == VM_MagicNames.store) {

        if (types[0] == VM_TypeReference.Word ||
            types[0] == VM_TypeReference.ObjectReference ||
            types[0] == VM_TypeReference.Address ||
            types[0] == VM_TypeReference.LocalAddress) {
          if (types.length == 1) {
            popWord(T0);                 // pop newvalue
            popWord(T4);                 // pop base
            asm.emitILW(T2, BYTES_IN_ADDRESS); 
            genObjectCacheLookup(T1, T4, T2);  // lookup object address in cache 
            asm.emitStore(T0, T1);      // *(base)
            asm.emitPUT(T4, T2, T1, OBJECT_CACHE_WRITE_TAG_GROUP);
          } else {
          	popWord(T1);                 // pop Offset
            popWord(T0);                 // pop newvalue
            popWord(T4);                 // pop base
            asm.emitA(T4, T4, T1);
            asm.emitILW(T2, BYTES_IN_ADDRESS); 
            genObjectCacheLookup(T1, T4, T2);  // lookup object address in cache 
            asm.emitStore(T0, T1);      // *(base)
            asm.emitPUT(T4, T2, T1, OBJECT_CACHE_WRITE_TAG_GROUP);
          }
          return true;
        }

        if (types[0] == VM_TypeReference.Byte) {
        	if (types.length == 1) {
            popWord(T0);                 // pop newvalue
            popWord(T4);                 // pop base
            asm.emitILW(T2, BYTES_IN_BYTE); 
            genObjectCacheLookup(T1, T4, T2);  // lookup object address in cache 
            asm.emitStoreByte(T0, T1, Offset.zero());      // *(base)
            asm.emitPUT(T4, T2, T1, OBJECT_CACHE_WRITE_TAG_GROUP);
          } else {
          	popWord(T1);                 // pop Offset
            popWord(T0);                 // pop newvalue
            popWord(T4);                 // pop base
            asm.emitA(T4, T4, T1);
            asm.emitILW(T2, BYTES_IN_BYTE); 
            genObjectCacheLookup(T1, T4, T2);  // lookup object address in cache 
            asm.emitStoreByte(T0, T1, Offset.zero());      // *(base)
            asm.emitPUT(T4, T2, T1, OBJECT_CACHE_WRITE_TAG_GROUP);
          }
          return true;
        }

        if (types[0] == VM_TypeReference.Int || types[0] == VM_TypeReference.Float) {
        	if (types.length == 1) {
            popWord(T0);                 // pop newvalue
            popWord(T4);                 // pop base
            asm.emitILW(T2, BYTES_IN_INT); 
            genObjectCacheLookup(T1, T4, T2);  // lookup object address in cache 
            asm.emitStore(T0, T1);      // *(base)
            asm.emitPUT(T4, T2, T1, OBJECT_CACHE_WRITE_TAG_GROUP);
          } else {
          	popWord(T1);                 // pop Offset
            popWord(T0);                 // pop newvalue
            popWord(T4);                 // pop base
            asm.emitA(T4, T4, T1);
            asm.emitILW(T2, BYTES_IN_INT); 
            genObjectCacheLookup(T1, T4, T2);  // lookup object address in cache 
            asm.emitStore(T0, T1);      // *(base)
            asm.emitPUT(T4, T2, T1, OBJECT_CACHE_WRITE_TAG_GROUP);
          }
          return true;
        }

        if (types[0] == VM_TypeReference.Short || types[0] == VM_TypeReference.Char) {
        	if (types.length == 1) {
        		popWord(T0);                 // pop newvalue
        		popWord(T4);                 // pop base
        		asm.emitILW(T2, BYTES_IN_SHORT); 
        		genObjectCacheLookup(T1, T4, T2);  // lookup object address in cache 
        		asm.emitStoreShort(T0, T1, Offset.zero());      // *(base)
        		asm.emitPUT(T4, T2, T1, OBJECT_CACHE_WRITE_TAG_GROUP);
          } else {
          	popWord(T1);                 // pop Offset
          	popWord(T0);                 // pop newvalue
          	popWord(T4);                 // pop base
          	asm.emitA(T4, T4, T1);
          	asm.emitILW(T2, BYTES_IN_SHORT); 
          	genObjectCacheLookup(T1, T4, T2);  // lookup object address in cache 
          	asm.emitStoreShort(T0, T1, Offset.zero());      // *(base)
          	asm.emitPUT(T4, T2, T1, OBJECT_CACHE_WRITE_TAG_GROUP);
          }
        	return true;
        }

        if (types[0] == VM_TypeReference.Double || types[0] == VM_TypeReference.Long) {
        	if (types.length == 1) {
        		popWord(T0);                 // pop newvalue
        		popWord(T4);                 // pop base
        		asm.emitILW(T2, BYTES_IN_DOUBLE); 
        		genObjectCacheLookup(T1, T4, T2);  // lookup object address in cache 
        		asm.emitStoreDouble(T0, T1, Offset.zero());      // *(base)
        		asm.emitPUT(T4, T2, T1, OBJECT_CACHE_WRITE_TAG_GROUP);
          } else {
          	popWord(T1);                 // pop Offset
          	popWord(T0);                 // pop newvalue
          	popWord(T4);                 // pop base
          	asm.emitA(T4, T4, T1);
          	asm.emitILW(T2, BYTES_IN_DOUBLE); 
          	genObjectCacheLookup(T1, T4, T2);  // lookup object address in cache 
          	asm.emitStoreDouble(T0, T1, Offset.zero());      // *(base)
          	asm.emitPUT(T4, T2, T1, OBJECT_CACHE_WRITE_TAG_GROUP);
          }
        	return true;
        }
      }
      
      if ((methodName == VM_MagicNames.prepareInt) ||
     		(methodName == VM_MagicNames.prepareObject) ||
        (methodName == VM_MagicNames.prepareAddress) ||
        (methodName == VM_MagicNames.prepareWord)) {
        if (types.length == 0) {
        	popWord(T0); // pop object
        } else {
        	popWord(T1); // pop offset
        	popWord(T0); // pop object
        	asm.emitA(T0, T1, T0);
        }
				genPrepareWord();
				// this Integer is not sign extended !!
				pushWord(T0); // push *(object+offset)
				return true;
      } 
      
      if ((methodName == VM_MagicNames.prepareLong)) {
      	if (types.length == 0) {
        	popWord(T0); // pop object
        } else {
        	popWord(T1); // pop offset
        	popWord(T0); // pop object
        	asm.emitA(T0, T1, T0);
        }
				genPrepareLong();
				// this Integer is not sign extended !!
				pushLong(T0); // push *(object+offset)
				return true;
      }
      
      if (methodName == VM_MagicNames.attempt &&
      		((types[0] == VM_TypeReference.Int) ||
          (types[0] == VM_TypeReference.Address) ||
          (types[0] == VM_TypeReference.Word))) {
				popWord(T2);  // pop newValue
				discardSlot(); // ignore oldValue
				if (types.length == 2) {
					popWord(T0); // pop object
				} else {
					popWord(T1); // pop offset
					popWord(T0); // pop object
					asm.emitA(T0, T1, T0);
				}
				genAttemptWord();
				pushWord(T0);  // push success of conditional store
				return true;
      }
      
      if (methodName == VM_MagicNames.attempt &&
          types[0] == VM_TypeReference.Long) {
				popLong(T2);  // pop newValue
				discardSlot(); // ignore oldValue
				if (types.length == 2) {
					popWord(T0); // pop object
				} else {
					popWord(T1); // pop offset
					popWord(T0); // pop object
					asm.emitA(T0, T1, T0);
				}
				genAttemptLong();
				pushWord(T0);  // push success of conditional store
				return true;
      }
    }
    
    if (methodToBeCalled.getType() == VM_TypeReference.LocalAddress) {
      // LocalAddress.xyz magic
      
      VM_TypeReference[] types = methodToBeCalled.getParameterTypes();

      // Loads all take the form:
      // ..., Address, [Offset] -> ..., Value

      if (methodName == VM_MagicNames.loadAddress ||
          methodName == VM_MagicNames.loadLocalAddress ||
          methodName == VM_MagicNames.loadObjectReference ||
          methodName == VM_MagicNames.loadWord) {

        if (types.length == 0) {
          popWord(T4);                  // pop base
          asm.emitLoad(T0, T4);    // *(base)
          pushWord(T0);                 // push *(base)
        } else {
          popWord(T1);                   // pop offset
          popWord(T4);                  // pop base
          asm.emitLoad(T0, T4, T1);   // *(base+offset)
          pushWord(T0);                 // push *(base+offset)
        }
        return true;
      }

      if (methodName == VM_MagicNames.loadChar) {

        if (types.length == 0) {
        	popWord(T4);                  // pop base
          asm.emitLoadCharUnaligned(T0, T4); // load with zero extension.
          pushWord(T0);                  // push *(base)
        } else {
          popWord(T1);                   // pop offset
          popWord(T4);                  // pop baseasm.emitA(T4, T4, T1);
          asm.emitLoadCharUnaligned(T0, T4, T1);   // *(base+offset)
          pushWord(T0);                  // push *(base+offset)
        }
        return true;
      }

      if (methodName == VM_MagicNames.loadShort) {

        if (types.length == 0) {
          popWord(T4);                  // pop base
          asm.emitLoadShortUnaligned(T0, T4);       // load with sign extension.
          pushWord(T0);                  // push *(base)
        } else {
          popWord(T1);                   // pop offset
          popWord(T4);                  // pop base
          asm.emitLoadShortUnaligned(T0, T4, T1);       // load with sign extension.
          pushWord(T0);                  // push *(base+offset)
        }
        return true;
      }

      if (methodName == VM_MagicNames.loadByte) {
        if (types.length == 0) {
          popWord(T4);                  // pop base
          asm.emitLoadByteUnaligned(T0, T4);       // load with sign extension.
          pushWord(T0);                  // push *(base)
        } else {
          popWord(T1);                   // pop offset
          popWord(T4);                  // pop base
          asm.emitLoadByteUnaligned(T0, T4, T1);       // load with sign extension.
          pushWord(T0);                  // push *(base+offset)
        }
        return true;
      }

      if (methodName == VM_MagicNames.loadInt || methodName == VM_MagicNames.loadFloat) {

        if (types.length == 0) {
          popWord(T4);                  // pop base
          asm.emitLoadUnaligned(T0, T4);     // *(base)
          pushWord(T0);                  // push *(base)
        } else {
          popWord(T1);                   // pop offset
          popWord(T4);                  // pop base
          asm.emitLoadUnaligned(T0, T4, T1);    // *(base+offset)
          pushWord(T0);                  // push *(base+offset)
        }
        return true;
      }

      if (methodName == VM_MagicNames.loadDouble || methodName == VM_MagicNames.loadLong) {

        if (types.length == 0) {
          popWord(T4);                  // pop base
          asm.emitLoadDoubleUnaligned(T0, T4);
          pushLong(T0);               // push double
        } else {
          popWord(T1);                   // pop offset
          popWord(T4);                  // pop base
          asm.emitLoadDoubleUnaligned(T0, T4, T1);
          pushLong(T0);               // push *(base+offset)
        }
        return true;
      }

      //    Stores all take the form:
      // ..., Address, Value, [Offset] -> ...
      if (methodName == VM_MagicNames.store) {

        if (types[0] == VM_TypeReference.Word ||
            types[0] == VM_TypeReference.ObjectReference ||
            types[0] == VM_TypeReference.Address ||
            types[0] == VM_TypeReference.LocalAddress) {
          if (types.length == 1) {
            popWord(T1);                 // pop newvalue
            popWord(T0);                 // pop base
            asm.emitStore(T1, T0);   // *(base) = newvalue
          } else {
            popWord(T1);                  // pop offset
            popWord(T2);                 // pop newvalue
            popWord(T0);                 // pop base
            asm.emitStore(T2, T0, T1); // *(base+offset) = newvalue
          }
          return true;
        }

        if (types[0] == VM_TypeReference.Byte) {
          if (types.length == 1) {
            popWord(T1);                  // pop newvalue
            popWord(T0);                 // pop base
            asm.emitStoreByte(T1, T0, Offset.zero());      // *(base) = newvalue
          } else {
            popWord(T1);                  // pop offset
            popWord(T2);                  // pop newvalue
            popWord(T0);                 // pop base
            asm.emitStoreByte(T2, T0, T1);    // *(base+offset) = newvalue
          }
          return true;
        }

        if (types[0] == VM_TypeReference.Int || types[0] == VM_TypeReference.Float) {
          if (types.length == 1) {
            popWord(T1);                  // pop newvalue
            popWord(T0);                 // pop base
            asm.emitStore(T1, T0, Offset.zero());      // *(base+offset) = newvalue
          } else {
            popWord(T1);                  // pop offset
            popWord(T2);                  // pop newvalue
            popWord(T0);                 // pop base
            asm.emitStore(T2, T0, T1);    // *(base+offset) = newvalue
          }
          return true;
        }

        if (types[0] == VM_TypeReference.Short || types[0] == VM_TypeReference.Char) {
          if (types.length == 1) {
            popWord(T1);                  // pop newvalue
            popWord(T0);                 // pop base
            asm.emitStoreShort(T1, T0, Offset.zero());      // *(base) = newvalue
          } else {
            popWord(T1);                  // pop offset
            popWord(T2);                  // pop newvalue
            popWord(T0);                 // pop base
            asm.emitStoreShort(T2, T0, T1);    // *(base+offset) = newvalue
          }
          return true;
        }

        if (types[0] == VM_TypeReference.Double || types[0] == VM_TypeReference.Long) {
          if (types.length == 1) {
            popLong(T1);                      // pop newvalue low and high
            popWord(T0);                          // pop base
            asm.emitStoreDouble(T1, T0, Offset.zero());           // *(base) = newvalue
          } else {
            popWord(T1);                           // pop offset
            popLong(T2);                      // pop newvalue low and high
            popWord(T0);                          // pop base
            asm.emitStoreDouble(T2, T0, T1);           // *(base+offset) = newvalue
          }
          return true;
        }
      }
    }

    if (methodName == VM_MagicNames.getFramePointer) {
      pushWord(FP);
    } else if (methodName == VM_MagicNames.getCallerFramePointer) {
      popWord(T0);                               // pop  frame pointer of callee frame
      asm.emitLoad(T1, T0, Offset.fromIntSignExtend(STACKFRAME_FRAME_POINTER_OFFSET)); // load frame pointer of caller frame
      pushWord(T1);                               // push frame pointer of caller frame
    } else if (methodName == VM_MagicNames.setCallerFramePointer) {
      popWord(T1); // value
      popWord(T0); // fp
      asm.emitStore(T1, T0, Offset.fromIntSignExtend(STACKFRAME_FRAME_POINTER_OFFSET)); // *(address+SFPO) := value
    } else if (methodName == VM_MagicNames.getCompiledMethodID) {
      popWord(T0);                           // pop  frame pointer of callee frame
      asm.emitLoad(T1, T0, Offset.fromIntSignExtend(STACKFRAME_METHOD_ID_OFFSET)); // load compiled method id
      pushWord(T1);                           // push method ID
    } else if (methodName == VM_MagicNames.setCompiledMethodID) {
      popWord(T1); // value
      popWord(T0); // fp
      asm.emitStore(T1, T0, Offset.fromIntSignExtend(STACKFRAME_METHOD_ID_OFFSET)); // *(address+SNIO) := value
    } else if (methodName == VM_MagicNames.getNextInstructionAddress) {
      popWord(T0);                                  // pop  frame pointer of callee frame
      asm.emitLoad(T1, T0, Offset.fromIntSignExtend(STACKFRAME_NEXT_INSTRUCTION_OFFSET)); // load frame pointer of caller frame
      pushWord(T1);                                  // push frame pointer of caller frame
    } else if (methodName == VM_MagicNames.getReturnAddressLocation) {
      popWord(T0);                                  // pop  frame pointer of callee frame
      asm.emitLoad(T1, T0, Offset.fromIntSignExtend(STACKFRAME_FRAME_POINTER_OFFSET));    // load frame pointer of caller frame
      asm.emitAI(T2, T1, STACKFRAME_NEXT_INSTRUCTION_OFFSET); // get location containing ret addr
      pushWord(T2);                                  // push frame pointer of caller frame
    } else if (methodName == VM_MagicNames.getTocPointer || methodName == VM_MagicNames.getJTOC) {
      // return the main JTOC, not our subarch one
    	asm.emitLoad(T0, JTOC, VM_SubArchStatics.mainJTOCAddrOff);
    	pushWord(T0);
    } else if (methodName == VM_MagicNames.getSubArchJTOC) {
    	pushWord(JTOC);
    } else if (methodName == VM_MagicNames.getProcessorRegister) {
      pushWord(PROCESSOR_REGISTER);
    } else if (methodName == VM_MagicNames.setProcessorRegister) {
      popWord(PROCESSOR_REGISTER);
    } else if (methodName == VM_MagicNames.runningOnSubArch) {
  		// the cell spu is a sub-arch, return true
  		asm.emitILW(T0, 1);
  		pushWord(T0);
    } else if (methodName == VM_MagicNames.readIntMailBox || 
    					 methodName == VM_MagicNames.readRefMailBox) {
    	// perform a blocking read on the mailbox register
    	asm.emitRDCH(SPU_RD_IN_MBOX, T0);
    	pushWord(T0);
    } else if (methodName == VM_MagicNames.writeMailBox) {
    	// perform a write to the mailbox register
    	popWord(T0);
    	asm.emitWRCH(SPU_WR_OUT_MBOX, T0);
    } else if (methodName == VM_MagicNames.writeMailBoxUpperWord) {
    	// perform double write to the mailbox register
    	popLong(T0);
    	asm.emitWRCH(SPU_WR_OUT_MBOX, T0);    // high word
    } else if (methodName == VM_MagicNames.writeMailBoxLowerWord) {
    	popLong(T0);
    	asm.emitSHLQBYI(T0, T0, BYTES_IN_INT);
    	asm.emitWRCH(SPU_WR_OUT_MBOX, T0);    // low word    	
    } else if (methodName == VM_MagicNames.writeIntrMailBox) {
    	// perform a blocking read on the mailbox register
    	popWord(T0);
    	asm.emitWRCH(SPU_WR_OUT_INTR_MBOX, T0);
    } else if (methodName == VM_MagicNames.cacheStaticMethod) {
   		// cache a static method in local memory.
    	popWord(T5);    // classStaticTocOffset
    	genClassTibLookup(T7, T5);

    	popWord(T5);    // method offset
      genMethodCacheLookup(S5, T7, T5);
      
  	  pushWord(S5);
    } else if (methodName == VM_MagicNames.cacheClassStatics) {
    	popWord(T5);
    	genStaticsLookup(T4, T5);
    } else if (methodName == VM_MagicNames.invokeMethodReturningVoid) {
      generateMethodInvocation(); // call method
    } else if (methodName == VM_MagicNames.invokeMethodReturningInt ||
    					 methodName == VM_MagicNames.invokeMethodReturningFloat ||
    					 methodName == VM_MagicNames.invokeMethodReturningObject) {
      generateMethodInvocation(); // call method
      pushWord(T0);       // push result
    } else if (methodName == VM_MagicNames.invokeMethodReturningLong || 
    	       	 methodName == VM_MagicNames.invokeMethodReturningDouble) {
      generateMethodInvocation(); // call method
      pushLong(T0);       // push result
    } else if (methodName == VM_MagicNames.objectAsAddress ||
    	    methodName == VM_MagicNames.objectAsLocalAddress ||
          methodName == VM_MagicNames.addressAsByteArray ||
          methodName == VM_MagicNames.addressAsObject ||
          methodName == VM_MagicNames.localAddressAsObject ||
          methodName == VM_MagicNames.addressAsObjectArray ||
          methodName == VM_MagicNames.objectAsType ||
          methodName == VM_MagicNames.objectAsShortArray ||
          methodName == VM_MagicNames.objectAsIntArray ||
          methodName == VM_MagicNames.objectAsProcessor ||
          methodName == VM_MagicNames.objectAsThread ||
          methodName == VM_MagicNames.threadAsCollectorThread ||
          methodName == VM_MagicNames.floatAsIntBits ||
          methodName == VM_MagicNames.intBitsAsFloat ||
          methodName == VM_MagicNames.doubleAsLongBits ||
          methodName == VM_MagicNames.longBitsAsDouble ||
          methodName == VM_MagicNames.addressArrayGetBacking) {
    	// no-op (a type change, not a representation change)
    } else if ((methodName == VM_MagicNames.prepareInt) ||
           		(methodName == VM_MagicNames.prepareObject) ||
              (methodName == VM_MagicNames.prepareAddress) ||
              (methodName == VM_MagicNames.prepareWord)) {
      popWord(T1); // pop offset
      popWord(T0); // pop object
      asm.emitA(T0, T1, T0);
      genPrepareWord();
      // this Integer is not sign extended !!
      pushWord(T0); // push *(object+offset)
    } else if ((methodName == VM_MagicNames.prepareLong)) {
		  popWord(T1); // pop offset
		  popWord(T0); // pop object
		  asm.emitA(T0, T1, T0);
		  genPrepareLong();
		  // this Integer is not sign extended !!
		  pushLong(T0); // push *(object+offset)
    } else if ((methodName == VM_MagicNames.attemptInt) ||
        (methodName == VM_MagicNames.attemptObject) ||
        (methodName == VM_MagicNames.attemptObjectReference) ||
        (methodName == VM_MagicNames.attemptAddress) ||
        (methodName == VM_MagicNames.attemptWord)) {
      popWord(T2);  // pop newValue
      discardSlot(); // ignore oldValue
      popWord(T1);  // pop offset
      popWord(T0);  // pop object
		  asm.emitA(T0, T1, T0);
		  genAttemptWord();
      pushWord(T0);  // push success of conditional store
    } else if ((methodName == VM_MagicNames.attemptLong)) {
      popLong(T2);  // pop newValue
      discardSlot(); // ignore oldValue
      popWord(T1);  // pop offset
      popWord(T0);  // pop object
		  asm.emitA(T0, T1, T0);
		  genAttemptLong();
      pushWord(T0);  // push success of conditional store
    } else if (methodName == VM_MagicNames.addressArrayCreate) {
      VM_Array type = methodToBeCalled.getType().resolve(true).asArray();
      emit_resolved_newarray(type);
    } else if (methodName == VM_MagicNames.getObjectType) {
      popWord(T4);                   // get object pointer
      asm.emitILW(T2, VM_JavaHeader.JAVA_HEADER_BYTES); // just get the header
      genObjectCacheLookup(T1, T4, T2);  // lookup object address in cache     
      VM_ObjectModel.baselineEmitLoadTIB(asm, T5, T1, true); // load TIB      
      genStaticsLookup(T4, T5);
      asm.emitLoad(T0, T5, Offset.fromIntSignExtend(TIB_TYPE_INDEX << LOG_BYTES_IN_ADDRESS)); // get "type" field from type information block
      pushWord(T0);               // *sp := type
    } else if (methodName == VM_MagicNames.addressArrayLength) {
      emit_arraylength();
    } else if (methodName == VM_MagicNames.addressArrayGet) {
      emit_iaload();
    } else if (methodName == VM_MagicNames.addressArraySet) {
      emit_iastore();
    } else if (methodName == VM_MagicNames.getIntAtOffset ||
    		       methodName == VM_MagicNames.getObjectAtOffset ||
    		       methodName == VM_MagicNames.getWordAtOffset) {
      popWord(T3);                // T3 = field offset
      popWord(T4);                // T4 = object reference
      asm.emitNullCheck(T4);
      // get size necessary to fit offset
      asm.emitAI(T2, T3,  BYTES_IN_ADDRESS);
      // if offset is negative, make size zero
      asm.emitCGTI(S0, T2, 0);
      asm.emitSELB(T2, S2, T2, S0); // HACK! S2 is zero from null check
      genObjectCacheLookup(T1, T4, T2);  // lookup object address in cache
      asm.emitLoadUnaligned(T0, T1, T3);
      pushWord(T0);
    } else if (methodName == VM_MagicNames.getByteAtOffset) {
      popWord(T3);                // T3 = field offset
      popWord(T4);                // T4 = object reference
      asm.emitNullCheck(T4);
      // get size necessary to fit offset
      asm.emitAI(T2, T3, BYTES_IN_INT);
      // if offset is negative, make size zero
      asm.emitCGTI(S0, T2, 0);
      asm.emitSELB(T2, S2, T2, S0); // HACK! S2 is zero from null check
      genObjectCacheLookup(T1, T4, T2);  // lookup object address in cache
      asm.emitLoadByteUnaligned(T0, T1, T3);
      pushWord(T0);
    } else if (methodName == VM_MagicNames.getCharAtOffset) {
      popWord(T3);                // T3 = field offset
      popWord(T4);                // T4 = object reference
      asm.emitNullCheck(T4);
      // get size necessary to fit offset
      asm.emitAI(T2, T3, BYTES_IN_INT);
      // if offset is negative, make size zero
      asm.emitCGTI(S0, T2, 0);
      asm.emitSELB(T2, S2, T2, S0); // HACK! S2 is zero from null check
      genObjectCacheLookup(T1, T4, T2);  // lookup object address in cache
      asm.emitLoadCharUnaligned(T0, T1, T3);
      pushWord(T0);
    } else if ((methodName == VM_MagicNames.getLongAtOffset) || (methodName == VM_MagicNames.getDoubleAtOffset)) {
      popWord(T3);                // T3 = field offset
      popWord(T4);                // T4 = object reference
      asm.emitNullCheck(T4);
      // get size necessary to fit offset
      asm.emitAI(T2, T3, BYTES_IN_LONG);
      // if offset is negative, make size zero
      asm.emitCGTI(S0, T2, 0);
      asm.emitSELB(T2, S2, T2, S0); // HACK! S2 is zero from null check
      genObjectCacheLookup(T1, T4, T2);  // lookup object address in cache
      asm.emitLoadDoubleUnaligned(T0, T1, T3);
      pushLong(T0);
    } else if (methodName == VM_MagicNames.setIntAtOffset) {
      popWord(T0);                // T0 = new value
      popWord(T3);                // T3 = field offset
      popWord(T4);                // T4 = object reference
      asm.emitNullCheck(T4);
      // get size necessary to fit offset
      asm.emitAI(T2, T3,  BYTES_IN_INT);
      // if offset is negative, make size zero
      asm.emitCGTI(S0, T2, 0);
      asm.emitSELB(T2, S2, T2, S0); // HACK! S2 is zero from null check
      genObjectCacheLookup(T1, T4, T2);  // lookup object address in cache
      asm.emitA(T1, T1, T3);
      asm.emitStore(T0, T1);
      asm.emitILW(T3, BYTES_IN_INT);
      asm.emitA(T4, T4, T3);
      asm.emitPUT(T4, T3, T1, OBJECT_CACHE_WRITE_TAG_GROUP);
    } else if (methodName == VM_MagicNames.setObjectAtOffset || methodName == VM_MagicNames.setWordAtOffset) {
      if (methodToBeCalled.getParameterTypes().length == 4) {
        discardSlot(); // discard locationMetadata parameter
      }
      popWord(T0);                // T0 = new value
      popWord(T3);                // T3 = field offset
      popWord(T4);                // T4 = object reference
      asm.emitNullCheck(T4);
      // get size necessary to fit offset
      asm.emitAI(T2, T3,  BYTES_IN_ADDRESS);
      // if offset is negative, make size zero
      asm.emitCGTI(S0, T2, 0);
      asm.emitSELB(T2, S2, T2, S0); // HACK! S2 is zero from null check
      genObjectCacheLookup(T1, T4, T2);  // lookup object address in cache
      asm.emitA(T1, T1, T3);
      asm.emitStore(T0, T1);
      asm.emitILW(T3, BYTES_IN_ADDRESS);
      asm.emitA(T4, T4, T3);
      asm.emitPUT(T4, T3, T1, OBJECT_CACHE_WRITE_TAG_GROUP);
    } else if (methodName == VM_MagicNames.setByteAtOffset) {
      popWord(T0);                // T0 = new value
      popWord(T3);                // T3 = field offset
      popWord(T4);                // T4 = object reference
      asm.emitNullCheck(T4);
      // get size necessary to fit offset
      asm.emitAI(T2, T3,  BYTES_IN_INT);
      // if offset is negative, make size zero
      asm.emitCGTI(S0, T2, 0);
      asm.emitSELB(T2, S2, T2, S0); // HACK! S2 is zero from null check
      genObjectCacheLookup(T1, T4, T2);  // lookup object address in cache
      asm.emitA(T1, T1, T3);
      asm.emitStoreByte(T0, T1, Offset.zero());
      asm.emitILW(T3, BYTES_IN_BYTE);
      asm.emitA(T4, T4, T3);
      asm.emitPUT(T4, T3, T1, OBJECT_CACHE_WRITE_TAG_GROUP);
    } else if (methodName == VM_MagicNames.setCharAtOffset) {
      popWord(T0);                // T0 = new value
      popWord(T3);                // T3 = field offset
      popWord(T4);                // T4 = object reference
      asm.emitNullCheck(T4);
      // get size necessary to fit offset
      asm.emitAI(T2, T3, BYTES_IN_INT);
      // if offset is negative, make size zero
      asm.emitCGTI(S0, T2, 0);
      asm.emitSELB(T2, S2, T2, S0); // HACK! S2 is zero from null check
      genObjectCacheLookup(T1, T4, T2);  // lookup object address in cache
      asm.emitA(T1, T1, T3);
      asm.emitStoreShort(T0, T1, Offset.zero());
      asm.emitILW(T3, BYTES_IN_CHAR);
      asm.emitA(T4, T4, T3);
      asm.emitPUT(T4, T3, T1, OBJECT_CACHE_WRITE_TAG_GROUP);
    } else if ((methodName == VM_MagicNames.setLongAtOffset) || (methodName == VM_MagicNames.setDoubleAtOffset)) {
      popLong(T0);                // T0 = new value
      popWord(T3);                // T3 = field offset
      popWord(T4);                // T4 = object reference
      asm.emitNullCheck(T4);
      // get size necessary to fit offset
      asm.emitAI(T2, T3, BYTES_IN_LONG);
      // if offset is negative, make size zero
      asm.emitCGTI(S0, T2, 0);
      asm.emitSELB(T2, S2, T2, S0); // HACK! S2 is zero from null check
      genObjectCacheLookup(T1, T4, T2);  // lookup object address in cache
      asm.emitA(T1, T1, T3);
      asm.emitStoreDouble(T0, T1, Offset.zero());
      asm.emitILW(T3, BYTES_IN_LONG);
      asm.emitA(T4, T4, T3);
      asm.emitPUT(T4, T3, T1, OBJECT_CACHE_WRITE_TAG_GROUP);
    } else if (methodName == VM_MagicNames.wordToInt ||
          methodName == VM_MagicNames.wordToAddress ||
          methodName == VM_MagicNames.wordToLocalAddress ||
          methodName == VM_MagicNames.wordToOffset ||
          methodName == VM_MagicNames.wordToObject ||
          methodName == VM_MagicNames.wordFromObject ||
          methodName == VM_MagicNames.wordToObjectReference ||
          methodName == VM_MagicNames.wordToExtent ||
          methodName == VM_MagicNames.wordToWord ||
          methodName == VM_MagicNames.codeArrayAsObject) {
    	// no-op
    } else if (methodName == VM_MagicNames.wordToLong) {
      asm.emitIL(T0, 0);
      pushWord(T0);
    } else if (methodName == VM_MagicNames.wordFromInt || methodName == VM_MagicNames.wordFromIntSignExtend) {
      // nop
    } else if (methodName == VM_MagicNames.wordFromIntZeroExtend) {
      // nop
    } else if (methodName == VM_MagicNames.wordFromLong) {
      discardSlot();
    } else if (methodName == VM_MagicNames.wordPlus) {
      popWord(T0);
      popWord(T1);
      asm.emitA(T2, T1, T0);
      pushWord(T2);
    } else if (methodName == VM_MagicNames.wordMinus || methodName == VM_MagicNames.wordDiff) {
      popWord(T0);
      popWord(T1);
      asm.emitSF(T2, T0, T1);
      pushWord(T2);
    } else if (methodName == VM_MagicNames.wordEQ) {
      generateAddrComparison(false, EQ);
    } else if (methodName == VM_MagicNames.wordNE) {
      generateAddrComparison(false, NE);
    } else if (methodName == VM_MagicNames.wordLT) {
      generateAddrComparison(false, LT);
    } else if (methodName == VM_MagicNames.wordLE) {
      generateAddrComparison(false, LE);
    } else if (methodName == VM_MagicNames.wordGT) {
      generateAddrComparison(false, GT);
    } else if (methodName == VM_MagicNames.wordGE) {
      generateAddrComparison(false, GE);
    } else if (methodName == VM_MagicNames.wordsLT) {
      generateAddrComparison(true, LT);
    } else if (methodName == VM_MagicNames.wordsLE) {
      generateAddrComparison(true, LE);
    } else if (methodName == VM_MagicNames.wordsGT) {
      generateAddrComparison(true, GT);
    } else if (methodName == VM_MagicNames.wordsGE) {
      generateAddrComparison(true, GE);
    } else if (methodName == VM_MagicNames.wordIsZero || methodName == VM_MagicNames.wordIsNull) {
      // unsigned comparison generating a boolean
      asm.emitIL(T0, 0);
      pushWord(T0);
      generateAddrComparison(false, EQ);
    } else if (methodName == VM_MagicNames.wordIsMax) {
      // unsigned comparison generating a boolean
      asm.emitIL(T0, -1);
      pushWord(T0);
      generateAddrComparison(false, EQ);
    } else if (methodName == VM_MagicNames.wordZero || methodName == VM_MagicNames.wordNull) {
      asm.emitIL(T0, 0);
      pushWord(T0);
    } else if (methodName == VM_MagicNames.wordOne) {
      asm.emitIL(T0, 1);
      pushWord(T0);
    } else if (methodName == VM_MagicNames.wordMax) {
      asm.emitIL(T0, -1);
      pushWord(T0);
    } else if (methodName == VM_MagicNames.wordAnd) {
      popWord(T0);
      popWord(T1);
      asm.emitAND(T2, T1, T0);
      pushWord(T2);
    } else if (methodName == VM_MagicNames.wordOr) {
      popWord(T0);
      popWord(T1);
      asm.emitOR(T2, T1, T0);
      pushWord(T2);
    } else if (methodName == VM_MagicNames.wordNot) {
      popWord(T0);
      asm.emitIL(T1, -1);
      asm.emitXOR(T2, T1, T0);
      pushWord(T2);
    } else if (methodName == VM_MagicNames.wordXor) {
      popWord(T0);
      popWord(T1);
      asm.emitXOR(T2, T1, T0);
      pushWord(T2);
    } else if (methodName == VM_MagicNames.wordLsh) {
      popWord(T0);
      popWord(T1);
      asm.emitSHL(T2, T1, T0);
      pushWord(T2);
    } else if (methodName == VM_MagicNames.wordRshl) {
      popWord(T0);
      popWord(T1);
      asm.emitSFI(T0, T0, 0);
      asm.emitROTM(T2, T1, T0);
      pushWord(T2);
    } else if (methodName == VM_MagicNames.wordRsha) {
      popWord(T0);
      popWord(T1);
      asm.emitSFI(T0, T0, 0);
      asm.emitROTMA(T2, T1, T0);
      pushWord(T2);  
    } else if (methodName == VM_MagicNames.getArrayLength) {
      emit_arraylength();
    } else if ((methodName == VM_MagicNames.sync) ||
    					(methodName == VM_MagicNames.isync)) {
    	// TODO - full flush or sync? Do flush at the moment
    	//asm.emitILW(S0, OBJECT_CACHE_WRITE_TAG_GROUP);
    	//asm.emitILA(T0, VM_OutOfLineMachineCode.blockUntilTagCompletesInstructions);
    	//asm.emitBISL(LINK_REG, T0);
    	asm.emitILA(T0, VM_OutOfLineMachineCode.flushCacheInstructions);
    	asm.emitBISL(LINK_REG, T0);
    } else if (methodName == VM_MagicNames.pause) {
      // NO-OP
    } else {
    	VM.sysWriteln("Error with VM_Magic - " + methodName);
  		VM._assert(NOT_REACHED);
  		return false;
  	}
  	return true;
  }

  /**
   * Gen code for prepare of a word.
   *
   * Expects address in T0, returns value in T0
   */
	private void genPrepareWord() {
		asm.emitANDI(T2, T0, ATOMIC_CACHE_LINE_LENTH - 1);
		asm.emitSF(T1, T2, T0);  // align to cache line
		asm.emitILA(T3, ATOMIC_CACHE_LINE);
		asm.emitGETLLAR(T1, T3);          // *(object+offset), setting processor's reservation address
		asm.emitATOMIC_WAIT(S0);
		asm.emitLoad(T0, T3, T2);
	}
	
  /**
   * Gen code for prepare of a long.
   *
   * Expects address in T0, returns value in T0
   */
	private void genPrepareLong() {
		asm.emitANDI(T2, T0, ATOMIC_CACHE_LINE_LENTH - 1);
		asm.emitSF(T1, T2, T0);  // align to cache line
		asm.emitILA(T3, ATOMIC_CACHE_LINE);
		asm.emitGETLLAR(T1, T3);          // *(object+offset), setting processor's reservation address
		asm.emitATOMIC_WAIT(S0);
		asm.emitLoadDouble(T0, T3, T2);
	}

  /**
   * Gen code for attempt of a word.
   *
   * Expects address in T0, value in T2, returns success in T0
   */
	private void genAttemptWord() {
		asm.emitANDI(T3, T0, ATOMIC_CACHE_LINE_LENTH - 1);
		asm.emitSF(T1, T3, T0);  // align to cache line
		asm.emitILA(T4, ATOMIC_CACHE_LINE);
		asm.emitStore(T2, T4, T3); // store new value in cache line
		asm.emitPUTLLC(T1, T4);
		asm.emitATOMIC_WAIT(S0);
		asm.emitCEQI(T0, S0, 0);
	}
	
  /**
   * Gen code for attempt of a long.
   *
   * Expects address in T0, value in T2, returns success in T0
   */
	private void genAttemptLong() {
		asm.emitANDI(T3, T0, ATOMIC_CACHE_LINE_LENTH - 1);
		asm.emitSF(T1, T3, T0);  // align to cache line
		asm.emitILA(T4, ATOMIC_CACHE_LINE);
		asm.emitStoreDouble(T2, T4, T3); // store new value in cache line
		asm.emitPUTLLC(T1, T4);
		asm.emitATOMIC_WAIT(S0);
		asm.emitCEQI(T0, S0, 0);
	}


  /** Emit code to perform an unsigned comparison on 2 address values
   * @param cc condition to test
   */
  private void generateAddrComparison(boolean signed, int cc) {
  	popWord(T1);
  	popWord(T0);
  	if (cc == EQ || cc == NE) {
  		asm.emitCEQ(T3, T0, T1);
  	} else if (cc == GT || cc == LE) {
  		if (signed) {
  			asm.emitCGT(T3, T0, T1);
  		} else {
  			asm.emitCLGT(T3, T0, T1);
  		}
  	} else if (cc == LT || cc == GE) {
  		if (signed) {
  			asm.emitCGT(T3, T1, T0);
  		} else {
  			asm.emitCLGT(T3, T1, T0);
  		}
  	}
  	// invert result for these comparisons
  	if (cc == NE || cc == LE || cc == GE) {
  		asm.emitXOR(T3, T3, T3);
  	}
  	pushWord(T3);
  }

  /**
   * Indicate if the specified {@link VM_Magic} method causes a frame to be created on the runtime stack.
   * @param methodToBeCalled   {@link VM_Method} of the magic method being called
   * @return <code>true</code> if <code>methodToBeCalled</code> causes a stackframe to be created
   */
  public static boolean checkForActualCall(VM_MethodReference methodToBeCalled) {
    VM_Atom methodName = methodToBeCalled.getName();
    return methodName == VM_MagicNames.invokeClassInitializer ||
           methodName == VM_MagicNames.invokeMethodReturningVoid ||
           methodName == VM_MagicNames.invokeMethodReturningInt ||
           methodName == VM_MagicNames.invokeMethodReturningLong ||
           methodName == VM_MagicNames.invokeMethodReturningFloat ||
           methodName == VM_MagicNames.invokeMethodReturningDouble ||
           methodName == VM_MagicNames.invokeMethodReturningObject ||
           methodName == VM_MagicNames.addressArrayCreate;
  }

  //----------------//
  // implementation //
  //----------------//

  /**
   * Generate code to invoke arbitrary method with arbitrary parameters/return value.
   * We generate inline code that calls "VM_OutOfLineMachineCode.reflectiveMethodInvokerInstructions"
   * which, at runtime, will create a new stackframe with an appropriately sized spill area
   * (but no register save area, locals, or operand stack), load up the specified
   * fpr's and gpr's, call the specified method, pop the stackframe, and return a value.
   */
  private void generateMethodInvocation() {
    // On entry the stack looks like this:
    //
    //                       hi-mem
    //            +-------------------------+    \
    //            |         code[]          |     |
    //            +-------------------------+     |
    //            |         gprs[]          |     |
    //            +-------------------------+     |- java operand stack
    //            |         fprs[]          |     |
    //            +-------------------------+     |
    //            |         fprMeta[]       |     |
    //            +-------------------------+     |
    //            |         spills[]        |     |
    //            +-------------------------+    /

    // fetch parameters and generate call to method invoker
    //
    asm.emitILA(S0, VM_OutOfLineMachineCode.reflectiveMethodInvokerInstructions);
    peekWord(T0, 4);        // t0 := code
    peekWord(T1, 3);        // t1 := gprs
    peekWord(T2, 2);        // t2 := fprs
    peekWord(T3, 1);        // t3 := fprMeta
    peekWord(T4, 0);        // t4 := spills
    asm.emitBISL(LINK_REG, S0);
    discardSlots(5);       // pop parameters
  }

  /**
   * Generate call and return sequence to invoke a C function through the
   * boot record field specificed by target.
   * Caller handles parameter passing and expression stack
   * (setting up args, pushing return, adjusting stack height).
   *
   * <pre>
   *  Create a linkage area that's compatible with RS6000 "C" calling conventions.
   * Just before the call, the stack looks like this:
   *
   *                     hi-mem
   *            +-------------------------+  . . . . . . . .
   *            |          ...            |                  \
   *            +-------------------------+                   |
   *            |          ...            |    \              |
   *            +-------------------------+     |             |
   *            |       (int val0)        |     |  java       |- java
   *            +-------------------------+     |-  operand   |   stack
   *            |       (int val1)        |     |    stack    |    frame
   *            +-------------------------+     |             |
   *            |          ...            |     |             |
   *            +-------------------------+     |             |
   *            |      (int valN-1)       |     |             |
   *            +-------------------------+    /              |
   *            |          ...            |                   |
   *            +-------------------------+                   |
   *            |                         | <-- spot for this frame's callee's return address
   *            +-------------------------+                   |
   *            |          MI             | <-- this frame's method id
   *            +-------------------------+                   |
   *            |       saved FP          | <-- this frame's caller's frame
   *            +-------------------------+  . . . . . . . . /
   *            |      saved JTOC         |
   *            +-------------------------+  . . . . . . . . . . . . . .
   *            | parameterN-1 save area  | +  \                         \
   *            +-------------------------+     |                         |
   *            |          ...            | +   |                         |
   *            +-------------------------+     |- register save area for |
   *            |  parameter1 save area   | +   |    use by callee        |
   *            +-------------------------+     |                         |
   *            |  parameter0 save area   | +  /                          |  rs6000
   *            +-------------------------+                               |-  linkage
   *        +20 |       TOC save area     | +                             |    area
   *            +-------------------------+                               |
   *        +16 |       (reserved)        | -    + == used by callee      |
   *            +-------------------------+      - == ignored by callee   |
   *        +12 |       (reserved)        | -                             |
   *            +-------------------------+                               |
   *         +8 |       LR save area      | +                             |
   *            +-------------------------+                               |
   *         +4 |       CR save area      | +                             |
   *            +-------------------------+                               |
   *  FP ->  +0 |       (backlink)        | -                             |
   *            +-------------------------+  . . . . . . . . . . . . . . /
   *
   * Notes:
   * 1. parameters are according to host OS calling convention.
   * 2. space is also reserved on the stack for use by callee
   *    as parameter save area
   * 3. parameters are pushed on the java operand stack left to right
   *    java conventions) but if callee saves them, they will
   *    appear in the parameter save area right to left (C conventions)
   */
  private void generateSysCall(int parametersSize, VM_Field target) {
  	VM._assert(NOT_REACHED);
  }

  /**
   * Generate a sys call where the address of the function or (when POWEROPEN_ABI is defined)
   * function descriptor have been loaded into S0 already
   */
  private void generateSysCall(int parametersSize) {
  	VM._assert(NOT_REACHED);
  }
  
  public void compile() {
  	super.compile();
    
    // method ended - update subarch method length
    method.setSubArchLength((VM_Memory.alignUp(asm.getMachineCodeIndex() << LG_INSTRUCTION_WIDTH, BYTES_IN_QUAD)));
  
  }
  
  protected final VM_MachineCode genStub() {
  	if (shouldPrint) VM.sysWrite("  Method Stub, should never be run\n");
  	
  	int tableOffset = VM_SubArchBootRecord.addFakeTrapStr(VM_Magic.objectAsAddress(VM_Atom.findOrCreateAsciiAtom("[Stub Method Called!!] : " + method.getDeclaringClass().toString() + "." + method.getName().toString() + method.getDescriptor()).getBytes()));
  	asm.emitILW(S1, tableOffset);
    asm.emitFakeTrapStr(S0, S1);
  	
  	emit_trap(SubordinateArchitecture.VM_TrapConstants.TRAP_UNIMPLEMENTED_METHOD);
  	   
    // method ended - update subarch method length
    method.setSubArchLength(VM_Memory.alignUp(asm.getMachineCodeIndex() << LG_INSTRUCTION_WIDTH, BYTES_IN_QUAD));
   
  	return asm.finalizeMachineCode(bytecodeMap);
  }
}

