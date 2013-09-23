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
package org.jikesrvm.compilers.common.assembler.cellspu;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Random;

import org.jikesrvm.SubordinateArchitecture;
import org.jikesrvm.VM;
import org.jikesrvm.VM_Services;
import org.jikesrvm.SubordinateArchitecture.VM_TrapConstants;
import org.jikesrvm.compilers.baseline.VM_BaselineCompiler;
import org.jikesrvm.compilers.baseline.cellspu.VM_Compiler;
import org.jikesrvm.compilers.common.assembler.VM_AbstractAssembler;
import org.jikesrvm.compilers.common.assembler.VM_ForwardReference;
import org.jikesrvm.compilers.common.assembler.cellspu.VM_AssemblerShortBranch;
import org.jikesrvm.cellspu.VM_BaselineConstants;
import org.jikesrvm.cellspu.VM_MachineCode;
import org.jikesrvm.cellspu.VM_Disassembler;
import org.vmmagic.pragma.NoInline;
import org.vmmagic.unboxed.Offset;

/**
 * Machine code generators:
 *
 * Corresponding to a Cell SPU assembler instruction of the form
 *    xx A,B,C
 * there will be a method
 *    void emitXX (int A, int B, int C).
 *
 * The emitXX method appends this instruction to an VM_MachineCode object.
 *
 * mIP will be incremented to point to the next machine instruction.
 *
 * Machine code generators:
 */
public abstract class VM_Assembler extends VM_AbstractAssembler implements VM_BaselineConstants, VM_AssemblerConstants {

  /** Machine code being assembled */
  private final VM_MachineCode mc;
  /** Debug output? */
  private final boolean shouldPrint;
  /**  // VM_Baseline compiler instance for this assembler.  May be null. */
  final VM_Compiler compiler;
  /** current machine code instruction */
  private int mIP;

  public VM_Assembler(int length) {
    this(length, false, null);
  }

  public VM_Assembler(int length, boolean sp, VM_Compiler comp) {

    if (VM.VerifyAssertions) VM._assert(S3 <= LAST_SCRATCH_GPR); // need 4 scratch
    
    mc = new SubordinateArchitecture.VM_MachineCode();
    shouldPrint = sp;
    compiler = comp;
    mIP = 0;
  }

  public VM_Assembler(int length, boolean sp) {
    this(length, sp, null);
  }
  
  public static boolean fits(int val, int bits) {
    val = val >> (bits - 1);
    return (val == 0 || val == -1);
  }
  
  public static boolean fitsU(int val, int bits) {
    val = val >> (bits);
    return (val == 0);
  }

  @NoInline
  public void noteBytecode(int i, String bcode) {
    String s1 = VM_Services.getHexString(mIP << LG_INSTRUCTION_WIDTH, true);
    VM.sysWrite(s1 + ": [" + i + "] " + bcode + "\n");
  }

  @NoInline
  public void noteBytecode(int i, String bcode, int x) {
    noteBytecode(i, bcode + " " + x);
  }

  @NoInline
  public void noteBytecode(int i, String bcode, long x) {
    noteBytecode(i, bcode + " " + x);
  }

  @NoInline
  public void noteBytecode(int i, String bcode, Object o) {
    noteBytecode(i, bcode + " " + o);
  }

  @NoInline
  public void noteBytecode(int i, String bcode, int x, int y) {
    noteBytecode(i, bcode + " " + x + " " + y);
  }

  @NoInline
  public void noteBranchBytecode(int i, String bcode, int off, int bt) {
    noteBytecode(i, bcode + " " + off + " [" + bt + "] ");
  }

  @NoInline
  public void noteTableswitchBytecode(int i, int l, int h, int d) {
    noteBytecode(i, "tableswitch [" + l + "--" + h + "] " + d);
  }

  @NoInline
  public void noteLookupswitchBytecode(int i, int n, int d) {
    noteBytecode(i, "lookupswitch [<" + n + ">]" + d);
  }

  /* Handling backward branch references */

  public int getMachineCodeIndex() {
    return mIP;
  }

  /* Handling forward branch references */

  VM_ForwardReference forwardRefs = null;

  /* call before emiting code for the branch */
  final void reserveForwardBranch(int where) {
    VM_ForwardReference fr = new VM_ForwardReference.UnconditionalBranch(mIP, where);
    forwardRefs = VM_ForwardReference.enqueue(forwardRefs, fr);
  }

  /* call before emiting code for the branch */
  final void reserveForwardConditionalBranch(int where) {
    emitNOP();
    VM_ForwardReference fr = new VM_ForwardReference.ConditionalBranch(mIP, where);
    forwardRefs = VM_ForwardReference.enqueue(forwardRefs, fr);
  }

  /* call before emiting code for the branch */
  final void reserveShortForwardConditionalBranch(int where) {
    VM_ForwardReference fr = new VM_ForwardReference.ConditionalBranch(mIP, where);
    forwardRefs = VM_ForwardReference.enqueue(forwardRefs, fr);
  }

  /* call before emiting data for the case branch */
  final void reserveForwardCase(int where) {
    VM_ForwardReference fr = new VM_ForwardReference.SwitchCase(mIP, where);
    forwardRefs = VM_ForwardReference.enqueue(forwardRefs, fr);
  }

  /* call before emiting code for the target */
  public final void resolveForwardReferences(int label) {
    if (forwardRefs == null) return;
    forwardRefs = VM_ForwardReference.resolveMatching(this, forwardRefs, label);
  }

  public final void patchShortBranch(int sourceMachinecodeIndex) {
    int delta = mIP - sourceMachinecodeIndex;
    int instr = mc.getInstruction(sourceMachinecodeIndex);
    if ((delta >>> 15) == 0) { // delta (positive) fits in 16 bits
    	instr |= (delta << 7);
    	mc.putInstruction(sourceMachinecodeIndex, instr);
    } else {
      throw new InternalError("Long offset doesn't fit in short branch\n");
    }
  }

  public final void patchConditionalBranch(int sourceMachinecodeIndex) {
  	patchShortBranch(sourceMachinecodeIndex);
  }

  public final void patchUnconditionalBranch(int sourceMachinecodeIndex) {
  	patchShortBranch(sourceMachinecodeIndex);
  }
					
  public final void registerLoadReturnAddress(int bReturn) {
    VM_ForwardReference r = new VM_ForwardReference.LoadReturnAddress(mIP, bReturn);
    forwardRefs = VM_ForwardReference.enqueue(forwardRefs, r);
  }

  public final VM_ForwardReference generatePendingJMP(int bTarget) {
    return this.emitForwardBR();
  }

  public final void patchSwitchCase(int sourceMachinecodeIndex) {
    int delta = (mIP - sourceMachinecodeIndex) << 2;
    // correction is number of bytes of source off switch base
    int correction = (int) mc.getInstruction(sourceMachinecodeIndex);
    int offset = delta + correction;
    mc.putInstruction(sourceMachinecodeIndex, offset);
  }
  

  public final void patchLoadReturnAddress(int sourceIndex) {
    // TODO - patchLoadReturnAddress
  	VM._assert(NOT_REACHED);
  }


  /* ===============================================================  
   * machine instructions
   * ===============================================================
   */

  /* 
   * Load / Store Instructions
   * ________________________________
   */
  
  /** Load QuadWord */
    static final int LQDtemplate = 0x34 << 24;
  
  /** Load QuadWord */
  public final void emitLQD(int RT, int RA, int immed) {
  	if (VM.VerifyAssertions) VM._assert(fits(immed, 10));
    int mi = LQDtemplate | (0x3ff & immed) << 14 | RA << 7 | RT << 0;
    mIP++;
    mc.addInstruction(mi);
  }
  
  /** Load QuadWord (x-form) */
    static final int LQXtemplate = 0x1c4 << 21;
  
  /** Load QuadWord (x-form) */
  public final void emitLQX(int RT, int RA, int RB) {
    int mi = LQXtemplate | RB << 14 | RA << 7 | RT << 0;
    mIP++;
    mc.addInstruction(mi);
  }

  /** Load QuadWord (a-form) */
    static final int LQAtemplate = 0x61 << 23;
  
  /** Load QuadWord (a-form) */
  public final void emitLQA(int RT, int immed) {
	if (VM.VerifyAssertions) VM._assert(fits(immed, 16));
    int mi = LQAtemplate | (0xffff & immed) << 7 | RT << 0;
    mIP++;
    mc.addInstruction(mi);
  }
  
  /** Load QuadWord Instruction Relative (a-form) */
    static final int LQRtemplate = 0x67 << 23;
  
  /** Load QuadWord Instruction Relative (a-form) */
  public final void emitLQR(int RT, int immed) {
	if (VM.VerifyAssertions) VM._assert(fits(immed, 16));
    int mi = LQRtemplate | (0xffff & immed) << 7 | RT << 0;
    mIP++;
    mc.addInstruction(mi);
  }
  
  /** Store QuadWord */
    static final int STQDtemplate = 0x24 << 24;
  
  /** Store QuadWord */
  public final void emitSTQD(int RT, int RA, int immed) {
	if (VM.VerifyAssertions) VM._assert(fits(immed, 10));
    int mi = STQDtemplate | (0x3ff & immed) << 14 | RA << 7 | RT << 0;
    mIP++;
    mc.addInstruction(mi);
  }
  
  /** Store QuadWord (x-form) */
    static final int STQXtemplate = 0x144 << 21;
  
  /** Store QuadWord (x-form) */
  public final void emitSTQX(int RT, int RA, int RB) {
    int mi = STQXtemplate | RB << 14 | RA << 7 | RT << 0;
    mIP++;
    mc.addInstruction(mi);
  }

  /** Store QuadWord (a-form) */
    static final int STQAtemplate = 0x41 << 23;
  
  /** Store QuadWord (a-form) */
  public final void emitSTQA(int RT, int immed) {
	if (VM.VerifyAssertions) VM._assert(fits(immed, 16));
    int mi = STQAtemplate | (0xffff & immed) << 7 | RT << 0;
    mIP++;
    mc.addInstruction(mi);
  }
  
  /** Store QuadWord Instruction Relative (a-form) */
    static final int STQRtemplate = 0x47 << 23;
  
  /** Store QuadWord Instruction Relative (a-form) */
  public final void emitSTQR(int RT, int immed) {
	if (VM.VerifyAssertions) VM._assert(fits(immed, 16));
    int mi = STQRtemplate | (0xffff & immed) << 7 | RT << 0;
    mIP++;
    mc.addInstruction(mi);
  }
  
  /** Generate Controls for Byte Insertion (d-form) */
    static final int CBDtemplate = 0x1f4 << 21;
  
  /** Generate Controls for Byte Insertion (d-form) */
  public final void emitCBD(int RT, int RA, int immed) {
    int mi = CBDtemplate | (0x7f & immed) << 14 | RA << 7 | RT << 0;
    mIP++;
    mc.addInstruction(mi);
  }
  
  /** Generate Controls for Byte Insertion (x-form) */
    static final int CBXtemplate = 0x1d4 << 21;
  
  /** Generate Controls for Byte Insertion (x-form) */
  public final void emitCBX(int RT, int RA, int RB) {
    int mi = CBXtemplate | RB << 14 | RA << 7 | RT << 0;
    mIP++;
    mc.addInstruction(mi);
  }
  
  /** Generate Controls for Halfword Insertion (d-form) */
    static final int CHDtemplate = 0x1f5 << 21;
  
  /** Generate Controls for Halfword Insertion (d-form) */
  public final void emitCHD(int RT, int RA, int immed) {
    int mi = CHDtemplate | (0x7f & immed) << 14 | RA << 7 | RT << 0;
    mIP++;
    mc.addInstruction(mi);
  }
  
  /** Generate Controls for Halfword Insertion (x-form) */
    static final int CHXtemplate = 0x1d5 << 21;
  
  /** Generate Controls for Halfword Insertion (x-form) */
  public final void emitCHX(int RT, int RA, int RB) {
    int mi = CHXtemplate | RB << 14 | RA << 7 | RT << 0;
    mIP++;
    mc.addInstruction(mi);
  }
  
  /** Generate Controls for Word Insertion (d-form) */
    static final int CWDtemplate = 0x1f6 << 21;
  
  /** Generate Controls for Word Insertion (d-form) */
  public final void emitCWD(int RT, int RA, int immed) {
    int mi = CWDtemplate | (0x7f & immed) << 14 | RA << 7 | RT << 0;
    mIP++;
    mc.addInstruction(mi);
  }
  
  /** Generate Controls for Word Insertion (x-form) */
    static final int CWXtemplate = 0x1d6 << 21;
  
  /** Generate Controls for Word Insertion (x-form) */
  public final void emitCWX(int RT, int RA, int RB) {
    int mi = CWXtemplate | RB << 14 | RA << 7 | RT << 0;
    mIP++;
    mc.addInstruction(mi);
  }
  
  /** Generate Controls for Doubleword Insertion (d-form) */
    static final int CDDtemplate = 0x1f7 << 21;
  
  /** Generate Controls for Doubleword Insertion (d-form) */
  public final void emitCDD(int RT, int RA, int immed) {
    int mi = CDDtemplate | (0x7f & immed) << 14 | RA << 7 | RT << 0;
    mIP++;
    mc.addInstruction(mi);
  }
  
  /** Generate Controls for Doubleword Insertion (x-form) */
    static final int CDXtemplate = 0x1d7 << 21;
  
  /** Generate Controls for Doubleword Insertion (x-form) */
  public final void emitCDX(int RT, int RA, int RB) {
    int mi = CDXtemplate | RB << 14 | RA << 7 | RT << 0;
    mIP++;
    mc.addInstruction(mi);
  }
  
  /** 
   * Generate a set of instructions which load the byte at the address in 
   * addrReg (aligned to 16 Bytes) + offsetReg into the preferred slot of the target
   * register.
   * Destroys S0
   * 
   * @param destReg Register into which the word will be loaded (as an int word)
   * @param addrReg  Register which contains the address to be loaded in bytes (aligned to 16 bytes)
   * @param offsetReg Register which contains offset of address being loaded in bytes
   */
  public final void emitLoadByte(int destReg, int addrReg, int offsetReg) {
  	if (VM.VerifyAssertions) VM._assert(addrReg != S0 && offsetReg != S0);
  	if (VM.VerifyAssertions) VM._assert(destReg != offsetReg);
  	
    emitLQX(destReg, addrReg, offsetReg);
    // rotate to preferred slot
    emitROTQBY(destReg, destReg, offsetReg);
    // rotate back to preferred byte slot
    emitROTQBYI(destReg, destReg, 13);
    // sign extend
    emitXSBH(S0, destReg);
    emitXSHW(destReg, S0);
  }
  
  /** 
   * Generate a set of instructions which load the byte at the address in 
   * addrReg + offsetReg into the preferred slot of the target register.
   * Destroys S0
   * 
   * @param destReg Register into which the word will be loaded (as an int word)
   * @param addrReg  Register which contains the address to be loaded in bytes (aligned to 16 bytes)
   * @param offsetReg Register which contains offset of address being loaded in bytes
   */
  public final void emitLoadByteUnaligned(int destReg, int addrReg, int offsetReg) {
  	if (VM.VerifyAssertions) VM._assert(destReg != offsetReg && destReg != addrReg);
  	
    emitLQX(destReg, addrReg, offsetReg);
    // rotate to preferred slot
    emitROTQBY(destReg, destReg, offsetReg);
    emitROTQBY(destReg, destReg, addrReg);
    // rotate back to preferred byte slot
    emitROTQBYI(destReg, destReg, 13);
    // sign extend
    emitXSBH(S0, destReg);
    emitXSHW(destReg, S0);
  }  
  
  /** 
   * Generate a set of instructions which load the byte at the address in 
   * addrReg into the preferred slot of the target register.
   * Destroys S0
   * 
   * @param destReg Register into which the word will be loaded (as an int word)
   * @param addrReg  Register which contains the address to be loaded in bytes (aligned to 16 bytes)
   */
  public final void emitLoadByteUnaligned(int destReg, int addrReg) {
  	if (VM.VerifyAssertions) VM._assert(destReg != addrReg);
  	
    emitLQD(destReg, addrReg, 0);
    // rotate to preferred slot
    emitROTQBY(destReg, destReg, addrReg);
    // rotate back to preferred byte slot
    emitROTQBYI(destReg, destReg, 13);
    // sign extend
    emitXSBH(S0, destReg);
    emitXSHW(destReg, S0);
  }
  
  /** 
   * Generate a set of instructions which load the byte at the address in 
   * addrReg (aligned to 16 Bytes) + offsetReg into the preferred slot of the target
   * register.
   * Destroys S0
   * 
   * @param destReg Register into which the word will be loaded (as an int word)
   * @param addrReg  Register which contains the address to be loaded in bytes (aligned to 16 bytes)
   * @param offsetReg Register which contains offset of address being loaded in bytes
   */
  public final void emitLoadByte(int destReg, int addrReg, Offset offset) {
  	
    int offsetI = offset.toInt();
    if (fits((offsetI >> 4), 10)) {
      emitLQD(destReg, addrReg, offsetI >> 4);
      // rotate to preferred slot
      emitROTQBYI(destReg, destReg, offsetI - 3);
      // sign extend
      emitXSBH(S0, destReg);
      emitXSHW(destReg, S0);
    } else {
    	if (VM.VerifyAssertions) VM._assert(addrReg != S0);
      emitILW(S0, offsetI);
      emitLoadByte(destReg, addrReg, S0);
    }
  }
  
  /** 
   * Generate a set of instructions which load the byte at the address in 
   * addrReg + offsetReg into the preferred slot of the target
   * register.
   * Destroys S0
   * 
   * @param destReg Register into which the word will be loaded (as an int word)
   * @param addrReg  Register which contains the address to be loaded in bytes
   * @param offsetReg Register which contains offset of address being loaded in bytes
   */
  public final void emitLoadByteUnaligned(int destReg, int addrReg, Offset offset) {
  	if (VM.VerifyAssertions) VM._assert(addrReg != S0);
  	
    emitILW(S0, offset.toInt());
    emitLoadByteUnaligned(destReg, addrReg, S0);
  }

  /** 
   * Generate a set of instructions which load the unsigned char at the address in 
   * addrReg (aligned to 16 Bytes) + offsetReg into the preferred slot of the target
   * register.
   * 
   * @param destReg Register into which the word will be loaded (as an int word)
   * @param addrReg  Register which contains the address to be loaded in bytes (aligned to 16 bytes)
   * @param offsetReg Register which contains offset of address being loaded in bytes
   */
  public final void emitLoadChar(int destReg, int addrReg, int offsetReg) {
  	if (VM.VerifyAssertions) VM._assert(destReg != offsetReg);
  	
    emitLQX(destReg, addrReg, offsetReg);
    // rotate to preferred slot
    emitROTQBY(destReg, destReg, offsetReg);
    // rotate back to preferred char slot and mask
    emitROTMI(destReg, destReg, -(2 * BITS_IN_BYTE));
  }
  
  /** 
   * Generate a set of instructions which load the unsigned char at the address in 
   * addrReg + offsetReg into the preferred slot of the target
   * register.
   * 
   * @param destReg Register into which the word will be loaded (as an int word)
   * @param addrReg  Register which contains the address to be loaded in bytes 
   * @param offsetReg Register which contains offset of address being loaded in bytes
   */
  public final void emitLoadCharUnaligned(int destReg, int addrReg, int offsetReg) {
  	if (VM.VerifyAssertions) VM._assert(destReg != offsetReg && destReg != addrReg);
  	
    emitLQX(destReg, addrReg, offsetReg);
    // rotate to preferred slot
    emitROTQBY(destReg, destReg, addrReg);
    emitROTQBY(destReg, destReg, offsetReg);
    // rotate back to preferred char slot and mask
    emitROTMI(destReg, destReg, -(2 * BITS_IN_BYTE));
  }
  
  /** 
   * Generate a set of instructions which load the unsigned char at the address in 
   * addrReg into the preferred slot of the target register.
   * 
   * @param destReg Register into which the word will be loaded (as an int word)
   * @param addrReg  Register which contains the address to be loaded in bytes 
   * @param offsetReg Register which contains offset of address being loaded in bytes
   */
  public final void emitLoadCharUnaligned(int destReg, int addrReg) {
  	if (VM.VerifyAssertions) VM._assert(destReg != addrReg);
  	
    emitLQD(destReg, addrReg, 0);
    // rotate to preferred slot
    emitROTQBY(destReg, destReg, addrReg);
    // rotate back to preferred char slot and mask
    emitROTMI(destReg, destReg, -(2 * BITS_IN_BYTE));
  }
  
  /** 
   * Generate a set of instructions which load the unsigned char at the address in 
   * addrReg (aligned to 16 Bytes) + offsetReg into the preferred slot of the target
   * register.
   * Note: This only rotates word based upon offset alignment into preferred slot,
   *       so ensure addrReg is aligned or you want the full quadword.
   * 
   * @param destReg Register into which the word will be loaded (as an int word)
   * @param addrReg  Register which contains the address to be loaded in bytes (aligned to 16 bytes)
   * @param offsetReg Register which contains offset of address being loaded in bytes
   */
  public final void emitLoadChar(int destReg, int addrReg, Offset offset) {
    int offsetI = offset.toInt();
    if (fits((offsetI >> 4), 10)) {
      emitLQD(destReg, addrReg, offsetI >> 4);
      // rotate to preferred slot
      emitROTQBYI(destReg, destReg, offsetI);
      // rotate back to preferred char slot and mask
      emitROTMI(destReg, destReg, -(2 * BITS_IN_BYTE));
    } else {
    	if (VM.VerifyAssertions) VM._assert(addrReg != S0);
      emitILW(S0, offsetI);
      emitLoadChar(destReg, addrReg, S0);
    }
  }
  
  /** 
   * Generate a set of instructions which load the unsigned char at the address in 
   * addrReg + offsetReg into the preferred slot of the target
   * register.
   * Note: This only rotates word based upon offset alignment into preferred slot,
   *       so ensure addrReg is aligned or you want the full quadword.
   * 
   * @param destReg Register into which the word will be loaded (as an int word)
   * @param addrReg  Register which contains the address to be loaded in bytes
   * @param offsetReg Register which contains offset of address being loaded in bytes
   */
  public final void emitLoadCharUnaligned(int destReg, int addrReg, Offset offset) {
  	if (VM.VerifyAssertions) VM._assert(addrReg != S0);
    emitILW(S0, offset.toInt());
    emitLoadCharUnaligned(destReg, addrReg, S0);
  }
  
  
  /** 
   * Generate a set of instructions which load the short at the address in 
   * addrReg (aligned to 16 Bytes) + offsetReg into the preferred slot of the target
   * register.
   * Note: This only rotates word based upon offset alignment into preferred slot,
   *       so ensure addrReg is aligned or you want the full quadword.
   * 
   * @param destReg Register into which the word will be loaded (as an int word)
   * @param addrReg  Register which contains the address to be loaded in bytes (aligned to 16 bytes)
   * @param offsetReg Register which contains offset of address being loaded in bytes
   */
  public final void emitLoadShort(int destReg, int addrReg, int offsetReg) {
  	if (VM.VerifyAssertions) VM._assert(destReg != offsetReg);
  	
    emitLQX(destReg, addrReg, offsetReg);
    // rotate to preferred slot
    emitROTQBY(destReg, destReg, offsetReg);
    emitROTMI(destReg, destReg, -(2 * BITS_IN_BYTE));
    // sign extend
    emitXSHW(destReg, destReg);
  }
  
  /** 
   * Generate a set of instructions which load the short at the address in 
   * addrReg + offsetReg into the preferred slot of the target register.
   * Note: This only rotates word based upon offset alignment into preferred slot,
   *       so ensure addrReg is aligned or you want the full quadword.
   * Destroys S3
   * 
   * @param destReg Register into which the word will be loaded (as an int word)
   * @param addrReg  Register which contains the address to be loaded in bytes (aligned to 16 bytes)
   * @param offsetReg Register which contains offset of address being loaded in bytes
   */
  public final void emitLoadShortUnaligned(int destReg, int addrReg, int offsetReg) {
  	if (VM.VerifyAssertions) VM._assert(destReg != offsetReg && destReg != addrReg);
  	
    emitLQX(destReg, addrReg, offsetReg);
    // rotate to preferred slot
    emitROTQBY(destReg, destReg, addrReg);
    emitROTQBY(destReg, destReg, offsetReg);
    emitROTMI(destReg, destReg, -(2 * BITS_IN_BYTE));
    // sign extend
    emitXSHW(destReg, destReg);
  }
  
  /** 
   * Generate a set of instructions which load the short at the address in 
   * addrReg into the preferred slot of the target register.
   * Note: This only rotates word based upon offset alignment into preferred slot,
   *       so ensure addrReg is aligned or you want the full quadword.
   * Destroys S3
   * 
   * @param destReg Register into which the word will be loaded (as an int word)
   * @param addrReg  Register which contains the address to be loaded in bytes (aligned to 16 bytes)
   * @param offsetReg Register which contains offset of address being loaded in bytes
   */
  public final void emitLoadShortUnaligned(int destReg, int addrReg) {
  	if (VM.VerifyAssertions) VM._assert(destReg != addrReg);
  	
    emitLQD(destReg, addrReg, 0);
    // rotate to preferred slot
    emitROTQBY(destReg, destReg, addrReg);
    emitROTMI(destReg, destReg, -(2 * BITS_IN_BYTE));
    // sign extend
    emitXSHW(destReg, destReg);
  }
  /** 
   * Generate a set of instructions which load the short at the address in 
   * addrReg (aligned to 16 Bytes) + offsetReg into the preferred slot of the target
   * register.
   * Note: This only rotates word based upon offset alignment into preferred slot,
   *       so ensure addrReg is aligned or you want the full quadword.
   * Destroys S3
   * 
   * @param destReg Register into which the word will be loaded (as an int word)
   * @param addrReg  Register which contains the address to be loaded in bytes (aligned to 16 bytes)
   * @param offsetReg Register which contains offset of address being loaded in bytes
   */
  public final void emitLoadShort(int destReg, int addrReg, Offset offset) {
    int offsetI = offset.toInt();
    if (fits((offsetI >> 4), 10)) {
      emitLQD(destReg, addrReg, offsetI >> 4);
      if (offsetI != 0) {
      	emitROTQBYI(destReg, destReg, offsetI - 2);
      } else {
      	emitROTQBYI(destReg, destReg, 14);
      }
      // sign extend
      emitXSHW(destReg, destReg);
    } else {
    	if (VM.VerifyAssertions) VM._assert(addrReg != S0);
      emitILW(S0, offsetI);
      emitLoadShort(destReg, addrReg, S0);
    }
  }
  
  /** 
   * Generate a set of instructions which load the short at the address in 
   * addrReg + offsetReg into the preferred slot of the target
   * register.
   * Note: This only rotates word based upon offset alignment into preferred slot,
   *       so ensure addrReg is aligned or you want the full quadword.
   * Destroys S3
   * 
   * @param destReg Register into which the word will be loaded (as an int word)
   * @param addrReg  Register which contains the address to be loaded in bytes 
   * @param offsetReg Register which contains offset of address being loaded in bytes
   */
  public final void emitLoadShortUnaligned(int destReg, int addrReg, Offset offset) {
  	if (VM.VerifyAssertions) VM._assert(addrReg != S0);
  	emitILW(S0, offset.toInt());
    emitLoadShortUnaligned(destReg, addrReg, S0);
  }
  
  /** 
   * Generate a set of instructions which load the word at the address in 
   * addrReg into the preferred slot of the target register.
   * 
   * @param destReg Register into which the word will be loaded
   * @param addrReg	 Register which contains the address to be loaded in bytes
   */
  public final void emitLoad(int destReg, int addrReg) {
	  emitLQD(destReg, addrReg, 0);
  }
  
  /** 
   * Generate a set of instructions which load the word at the address in 
   * addrReg + offsetReg into the preferred slot of the target
   * register.
   * 
   * @param destReg Register into which the word will be loaded
   * @param addrReg  Register which contains the address to be loaded in bytes 
   * @param offsetReg Register which contains offset of address being loaded in bytes
   */
  public final void emitLoadUnaligned(int destReg, int addrReg, int offsetReg) {
  	if (VM.VerifyAssertions) VM._assert(destReg != offsetReg && destReg != addrReg);
    emitLQX(destReg, addrReg, offsetReg);
    // rotate to preferred slot
    emitROTQBY(destReg, destReg, addrReg);
    emitROTQBY(destReg, destReg, offsetReg);
  }
  /** 
   * Generate a set of instructions which load the word at the address in 
   * addrReg into the preferred slot of the target register.
   * 
   * @param destReg Register into which the word will be loaded
   * @param addrReg  Register which contains the address to be loaded in bytes 
   * @param offsetReg Register which contains offset of address being loaded in bytes
   */
  public final void emitLoadUnaligned(int destReg, int addrReg) {
  	if (VM.VerifyAssertions) VM._assert(destReg != addrReg);
    emitLQD(destReg, addrReg, 0);
    // rotate to preferred slot
    emitROTQBY(destReg, destReg, addrReg);
  }

  /** 
   * Generate a set of instructions which load the word/double word at the address in 
   * addrReg (aligned to 16 Bytes) + offsetReg (aligned to word) into the preferred slot of the target
   * register.
   * Note: This only rotates word based upon offset alignment into preferred slot,
   *       so ensure addrReg is aligned or you want the full quadword.
   * 
   * @param destReg Register into which the word will be loaded
   * @param addrReg  Register which contains the address to be loaded in bytes (aligned to 16 bytes)
   * @param offsetReg Register which contains offset of address being loaded in bytes
   */
  public final void emitLoad(int destReg, int addrReg, int offsetReg) {
  	if (VM.VerifyAssertions) VM._assert(destReg != offsetReg);
    emitLQX(destReg, addrReg, offsetReg);
    // rotate to preferred slot
    emitROTQBY(destReg, destReg, offsetReg);
  }
  
  /** 
   * Generate a set of instructions which load the word/double word at the address in 
   * addrReg (aligned to 16 Bytes) + offsetReg (aligned to word) into the preferred slot of the target
   * register.
   * Note: This only rotates word based upon offset alignment into preferred slot,
   *       so ensure addrReg is aligned or you want the full quadword.
   * 
   * @param destReg   Register into which the word will be loaded
   * @param addrReg   Register which contains the address to be loaded in bytes (aligned to 16 bytes)
   * @param offsetReg Register which contains offset of address being loaded in bytes
   * @param origQuad  Register which will contain the orignal Quad for a later store a
   */
  public final void emitLoad(int destReg, int addrReg, int offsetReg, int origQuad) {
  	if (VM.VerifyAssertions) VM._assert(offsetReg != origQuad);
  	
    emitLQX(origQuad, addrReg, offsetReg);
    // rotate to preferred slot
    emitROTQBY(destReg, origQuad, offsetReg);
  }
  
  /** 
   * Generate a set of instructions which load the word at the address in 
   * addrReg + offset (aligned to 16 bytes) into the preferred slot of the target register.
   * Note: This only rotates word based upon offset alignment into preferred slot,
   *       so ensure addrReg is aligned or you want the full quadword.
   * 
   * @param destReg Register into which the word will be loaded
   * @param addrReg  Register which contains the address to be loaded in bytes (aligned to 16 bytes)
   * @param offset Immediate offset of address being loaded in bytes
   */
  public final void emitLoad(int destReg, int addrReg, Offset offset) {
    int offsetI = offset.toInt();
    if (fits((offsetI >> 4), 10)) {
      emitLQD(destReg, addrReg, offsetI >> 4);
      // check whether offset is non-aligned
      if ((offsetI & (BYTES_IN_QUAD - 1)) != 0) {
        emitROTQBYI(destReg, destReg, offsetI);
      }
    } else {
    	if (VM.VerifyAssertions) VM._assert(addrReg != S0);
    	
      emitILW(S0, offsetI);
      emitLoad(destReg, addrReg, S0);
    }
  }
  
  /** 
   * Generate a set of instructions which load the word at the address in 
   * addrReg + offset into the preferred slot of the target register.
   * Note: This only rotates word based upon offset alignment into preferred slot,
   *       so ensure addrReg is aligned or you want the full quadword.
   * 
   * @param destReg Register into which the word will be loaded
   * @param addrReg  Register which contains the address to be loaded in bytes
   * @param offset Immediate offset of address being loaded in bytes
   */
  public final void emitLoadUnaligned(int destReg, int addrReg, Offset offset) {
  	if (VM.VerifyAssertions) VM._assert(addrReg != S0);
    emitILW(S0, offset.toInt());
    emitLoadUnaligned(destReg, addrReg, S0);
  }

  /** 
   * Generate a set of instructions which load the word at the address in 
   * addrReg + offset (aligned to 16 bytes) into the preferred slot of the target register.
   * Note: This only rotates word based upon offset alignment into preferred slot,
   *       so ensure addrReg is aligned or you want the full quadword.
   * 
   * @param destReg   Register into which the word will be loaded
   * @param addrReg   Register which contains the address to be loaded in bytes (aligned to 16 bytes)
   * @param offset    Immediate offset of address being loaded in bytes
   * @param origQuad  Used to store the orignal quad word for a later store
   */
  public final void emitLoad(int destReg, int addrReg, Offset offset, int origQuad) {
    int offsetI = offset.toInt();
    if (fits((offsetI >> 4), 10)) {
      emitLQD(origQuad, addrReg, offsetI >> 4);
      emitROTQBYI(destReg, origQuad, offsetI);
    } else {
    	if (VM.VerifyAssertions) VM._assert(addrReg != S0 && origQuad != S0);
      emitILW(S0, offsetI);
      emitLoad(destReg, addrReg, S0, origQuad);
    }
  }

  /** 
   * Generate a set of instructions which load the word at the address in 
   * addrReg (aligned to 16 bytes) + offset (aligned to double) into the preferred slot
   * of the target register.
   * 
   * @param destReg Register into which the word will be loaded
   * @param addrReg  Register which contains the address to be loaded in bytes
   * @param offsetReg Immediate offset of address being loaded in bytes
   */
  public final void emitLoadDouble(int destReg, int addrReg, int offsetReg) {
  	if (VM.VerifyAssertions) VM._assert(offsetReg != destReg);
    emitLQX(destReg, addrReg, offsetReg);
    // rotate to correct offset
    emitROTQBY(destReg, destReg, offsetReg);
  }

  /** 
   * Generate a set of instructions which load the word at the address in 
   * addrReg + offset into the preferred slot of the target register.
   * 
   * @param destReg Register into which the word will be loaded
   * @param addrReg  Register which contains the address to be loaded in bytes
   * @param offsetReg Immediate offset of address being loaded in bytes
   */
  public final void emitLoadDoubleUnaligned(int destReg, int addrReg, int offsetReg) {
  	if (VM.VerifyAssertions) VM._assert(offsetReg != destReg && addrReg != destReg);
  	emitLQX(destReg, addrReg, offsetReg);
    // rotate to correct offset
    emitROTQBY(destReg, destReg, addrReg);
    emitROTQBY(destReg, destReg, offsetReg);
  }
  
  /** 
   * Generate a set of instructions which load the word at the address in 
   * addrReg into the preferred slot of the target register.
   * 
   * @param destReg Register into which the word will be loaded
   * @param addrReg  Register which contains the address to be loaded in bytes
   * @param offsetReg Immediate offset of address being loaded in bytes
   */
  public final void emitLoadDoubleUnaligned(int destReg, int addrReg) {
  	if (VM.VerifyAssertions) VM._assert(addrReg != destReg);
    emitLQD(destReg, addrReg, 0);
    // rotate to correct offset
    emitROTQBY(destReg, destReg, addrReg);
  }
  /** 
   * Generate a set of instructions which load the word at the address in 
   * addrReg (aligned to 16 bytes) + offset into the preferred slot of the target register.
   * Note: This only rotates word based upon offset alignment into preferred slot,
   *       so ensure addrReg is aligned or you want the full quadword.
   * 
   * @param destReg Register into which the word will be loaded
   * @param addrReg	 Register which contains the address to be loaded in bytes
   * @param offset Immediate offset of address being loaded in bytes
   */
  public final void emitLoadDouble(int destReg, int addrReg, Offset offset) {
	  int offsetI = offset.toInt();
    if (VM.VerifyAssertions) VM._assert((offsetI & (BYTES_IN_DOUBLE - 1)) == 0);
  	if (fits((offsetI >> 4), 10)) {
		  emitLQD(destReg, addrReg, offsetI >> 4);
		  // check whether offset is non-aligned
		  if ((offsetI & (BYTES_IN_QUAD - 1)) != 0) {
			  emitROTQBYI(destReg, destReg, offsetI);
		  }
  	} else {
    	if (VM.VerifyAssertions) VM._assert(addrReg != S5);
			emitILW(S5, offsetI);
			emitLoadDouble(destReg, addrReg, S5);
  	}
  }
  
  /** 
   * Generate a set of instructions which load the word at the address in 
   * addrReg (aligned to 16 bytes) + offset into the preferred slot of the target register.
   * Note: This only rotates word based upon offset alignment into preferred slot,
   *       so ensure addrReg is aligned or you want the full quadword.
   * 
   * @param destReg Register into which the word will be loaded
   * @param addrReg	 Register which contains the address to be loaded in bytes
   * @param offset Immediate offset of address being loaded in bytes
   */
  public final void emitLoadDoubleUnaligned(int destReg, int addrReg, Offset offset) {
  	if (VM.VerifyAssertions) VM._assert(addrReg != S5);
  	emitILW(S5, offset.toInt());
		emitLoadDoubleUnaligned(destReg, addrReg, S5);
  }
  
  /** 
   * Generate a set of instructions which load the quadword at the address in 
   * addrReg (aligned to 16 bytes) into the target register.
   * 
   * @param destReg Register into which the word will be loaded
   * @param addrReg  Register which contains the address to be loaded in bytes (aligned to 16 bytes)
   */
  public final void emitLoadQuad(int destReg, int addrReg) {
    emitLQD(destReg, addrReg, 0);
  }

  /** 
   * Generate a set of instructions which load the word at the address in 
   * addrReg + offsetReg (aligned to 16 bytes) into the preferred slot of the target register.

   * 
   * @param destReg Register into which the word will be loaded
   * @param addrReg  Register which contains the address to be loaded in bytes (aligned to 16 bytes)
   * @param addrReg  Register which contains the offset of address to be loaded in bytes (aligned to 16 bytes)
   */
  public final void emitLoadQuad(int destReg, int addrReg, int offsetReg) {
    emitLQX(destReg, addrReg, offsetReg);
  }

  /** 
   * Generate a set of instructions which load the word at the address in 
   * addrReg + offsetReg (aligned to 16 bytes) into the preferred slot of the target register.

   * 
   * @param destReg Register into which the word will be loaded
   * @param addrReg  Register which contains the address to be loaded in bytes (aligned to 16 bytes)
   * @param addrReg  Register which contains the offset of address to be loaded in bytes (aligned to 16 bytes)
   */
  public final void emitLoadQuad(int destReg, int addrReg, Offset offset) {
  	int offsetI = offset.toInt();
  	if (VM.VerifyAssertions) VM._assert((offsetI & (BYTES_IN_QUAD - 1)) == 0);
    if (fits((offsetI >> 4), 10)) {
    	emitLQD(destReg, addrReg, offsetI);
    } else {
    	if (VM.VerifyAssertions) VM._assert(addrReg != S0);
    	emitILW(S0, offsetI);
    	emitLQX(destReg, addrReg, S0);
    }
  }
  
  /** 
   * Generate a set of instructions which stores the byte in the preferred slot of
   * the source register at the address in addrReg.
   * Note: Destroys S2 and S3
   * 
   * @param sourceReg Register which contains data to be written
   * @param addrReg	 Register which contains the address to be stored in bytes
   * @param offsetReg Register which contains offset of address being stored in bytes
   */
  public final void emitStoreByte(int sourceReg, int addrReg, int offsetReg) {
  	if (VM.VerifyAssertions) VM._assert(addrReg != S2 && sourceReg != S2 && offsetReg != S2);
  	if (VM.VerifyAssertions) VM._assert(addrReg != S3 && sourceReg != S3 && offsetReg != S3);
  	emitLQX(S2, addrReg, offsetReg);
  	emitCBX(S3, addrReg, offsetReg);
  	emitSHUFB(S2, sourceReg, S2, S3);
  	emitSTQX(S2, addrReg, offsetReg);
  }

  /** 
   * Generate a set of instructions which stores the byte in the preferred slot of
   * the source register at the address in addrReg.
   * Note: Destroys S2 and S3
   * 
   * @param sourceReg Register which contains data to be written
   * @param addrReg  Register which contains the address to be stored in bytes
   * @param offsetReg Register which contains offset of address being stored in bytes
   */
  public final void emitStoreByte(int sourceReg, int addrReg, Offset offset) {
    int offsetI = offset.toInt();
    if (fits((offsetI >> 4), 10)) {
    	if (VM.VerifyAssertions) VM._assert(addrReg != S2 && sourceReg != S2);
    	if (VM.VerifyAssertions) VM._assert(addrReg != S3 && sourceReg != S3);
      emitLQD(S2, addrReg, offsetI >> 4);
      emitCBD(S3, addrReg, offsetI);
      emitSHUFB(S2, sourceReg, S2, S3);
      emitSTQD(S2, addrReg, offsetI >> 4);
    } else {
    	if (VM.VerifyAssertions) VM._assert(addrReg != S0 && sourceReg != S0);
      emitILW(S0, offsetI);
      emitStoreByte(sourceReg, addrReg, S0);
    }
  }
  
  /** 
   * Generate a set of instructions which stores the short in the preferred slot of
   * the source register at the address in addrReg.
   * Note: Destroys S2 and S3
   * 
   * @param sourceReg Register which contains data to be written
   * @param addrReg	 Register which contains the address to be stored in bytes
   * @param offsetReg Register which contains offset of address being stored in bytes
   */
  public final void emitStoreShort(int sourceReg, int addrReg, int offsetReg) {
  	if (VM.VerifyAssertions) VM._assert(addrReg != S2 && sourceReg != S2 && offsetReg != S2);
  	if (VM.VerifyAssertions) VM._assert(addrReg != S3 && sourceReg != S3 && offsetReg != S3);
  	emitLQX(S2, addrReg, offsetReg);
  	emitCHX(S3, addrReg, offsetReg);
  	emitSHUFB(S2, sourceReg, S2, S3);
  	emitSTQX(S2, addrReg, offsetReg);
  }

  /** 
   * Generate a set of instructions which stores the short in the preferred slot of
   * the source register at the address in addrReg.
   * Note: Destroys S2 and S3
   * 
   * @param sourceReg Register which contains data to be written
   * @param addrReg  Register which contains the address to be stored in bytes
   * @param offsetReg Register which contains offset of address being stored in bytes
   */
  public final void emitStoreShort(int sourceReg, int addrReg, Offset offset) {
    int offsetI = offset.toInt();
    if (fits((offsetI >> 4), 10)) {
    	if (VM.VerifyAssertions) VM._assert(addrReg != S2 && sourceReg != S2);
    	if (VM.VerifyAssertions) VM._assert(addrReg != S3 && sourceReg != S3);
      emitLQD(S2, addrReg, offsetI >> 4);
      emitCHD(S3, addrReg, offsetI);
      emitSHUFB(S2, sourceReg, S2, S3);
      emitSTQD(S2, addrReg, offsetI >> 4);
    } else {
    	if (VM.VerifyAssertions) VM._assert(addrReg != S0 && sourceReg != S0);
      emitILW(S0, offsetI);
      emitStoreShort(sourceReg, addrReg, S0);
    }
  }

  /** 
   * Generate a set of instructions which stores the word in the preferred slot of
   * the source register at the address in addrReg.
   * Note: Destroys S2 and S3
   * 
   * @param sourceReg Register which contains data to be written
   * @param addrReg  Register which contains the address to be stored in bytes
   * @param offset Immediate offset of address being loaded in bytes
   */
  public final void emitStore(int sourceReg, int addrReg) {
  	if (VM.VerifyAssertions) VM._assert(addrReg != S2 && sourceReg != S2);
  	if (VM.VerifyAssertions) VM._assert(addrReg != S3 && sourceReg != S3);
    emitLQD(S2, addrReg, 0);
    emitCWD(S3, addrReg, 0);
    emitSHUFB(S2, sourceReg, S2, S3);
    emitSTQD(S2, addrReg, 0);
  }


  /** 
   * Generate a set of instructions which stores the word in the preferred slot of
   * the source register at the address in addrReg.
   * Note: Destroys S2 and S3
   * 
   * @param sourceReg Register which contains data to be written
   * @param addrReg  Register which contains the address to be stored in bytes
   * @param offsetReg Register which contains offset of address being stored in bytes
   */
  public final void emitStore(int sourceReg, int addrReg, int offsetReg) {
  	if (VM.VerifyAssertions) VM._assert(addrReg != S2 && sourceReg != S2 && offsetReg != S2);
  	if (VM.VerifyAssertions) VM._assert(addrReg != S3 && sourceReg != S3 && offsetReg != S3);
    emitLQX(S2, addrReg, offsetReg);
    emitCWX(S3, addrReg, offsetReg);
    emitSHUFB(S2, sourceReg, S2, S3);
    emitSTQX(S2, addrReg, offsetReg);
  }

  /** 
   * Generate a set of instructions which stores the word in the preferred slot of
   * the source register at the address in addrReg.
   * Note: Destroys S2 and S3
   * 
   * @param sourceReg Register which contains data to be written
   * @param addrReg   Register which contains the address to be stored in bytes
   * @param offsetReg Register which contains offset of address being stored in bytes
   * @param origQuad  Register containing the original quad word
   */
  public final void emitStore(int sourceReg, int addrReg, int offsetReg, int origQuad) {
  	if (VM.VerifyAssertions) VM._assert(addrReg != S2 && sourceReg != S2 && offsetReg != S2 && origQuad != S2);
  	if (VM.VerifyAssertions) VM._assert(addrReg != S3 && sourceReg != S3 && offsetReg != S3 && origQuad != S2);
    emitCWX(S3, addrReg, offsetReg);
    emitSHUFB(S2, sourceReg, origQuad, S3);
    emitSTQX(S2, addrReg, offsetReg);
  }
  /** 
   * Generate a set of instructions which stores the word in the preferred slot of
   * the source register at the address in addrReg.
   * Note: Destroys S2 and S3
   * 
   * @param sourceReg Register which contains data to be written
   * @param addrReg  Register which contains the address to be stored in bytes
   * @param offset Immediate offset of address being stored in bytes
   */
  public final void emitStore(int sourceReg, int addrReg, Offset offset) {
    int offsetI = offset.toInt();
    if (fits((offsetI >> 4), 10)) {
    	if (VM.VerifyAssertions) VM._assert(addrReg != S2 && sourceReg != S2);
    	if (VM.VerifyAssertions) VM._assert(addrReg != S3 && sourceReg != S3);
      emitLQD(S2, addrReg, (offsetI >> 4));
      emitCWD(S3, addrReg, offsetI);
      emitSHUFB(S2, sourceReg, S2, S3);
      emitSTQD(S2, addrReg, (offsetI >> 4));
    } else {
    	if (VM.VerifyAssertions) VM._assert(addrReg != S0 && sourceReg != S0);
      emitILW(S0, offsetI);
      emitStore(sourceReg, addrReg, S0);
    }
  }
  
  /** 
   * Generate a set of instructions which stores the word in the preferred slot of
   * the source register at the address in addrReg.
   * Note: Destroys S2 and S3 and perhaps S0
   * 
   * @param sourceReg Register which contains data to be written
   * @param addrReg  Register which contains the address to be stored in bytes
   * @param offset Immediate offset of address being stored in bytes
   * @param origQuad Register storing the original quad word
   */
  public final void emitStore(int sourceReg, int addrReg, Offset offset, int origQuad) {
    int offsetI = offset.toInt();
    if (fits((offsetI >> 4), 10)) {
    	if (VM.VerifyAssertions) VM._assert(addrReg != S2 && sourceReg != S2 && origQuad != S2);
    	if (VM.VerifyAssertions) VM._assert(addrReg != S3 && sourceReg != S3 && origQuad != S2);
      emitCWD(S3, addrReg, offsetI);
      emitSHUFB(S2, sourceReg, origQuad, S3);
      emitSTQD(S2, addrReg, (offsetI >> 4));
    } else {
    	if (VM.VerifyAssertions) VM._assert(addrReg != S0 && sourceReg != S0 && origQuad != S0);
      emitILW(S0, offsetI);
      emitStore(sourceReg, addrReg, S0, origQuad);
    }
  }
  
  /** 
   * Generate a set of instructions which stores the DoubleWord in the preferred slot of
   * the source register at the address in addrReg.  Note: addrReg must be aligned to 
   * double word
   * Note: Destroys S2 and S3
   * 
   * @param sourceReg Register which contains data to be written
   * @param addrReg	 Register which contains the address to be stored in bytes (must be aligned)
   */
  public final void emitStoreDouble(int sourceReg, int addrReg) {
  	if (VM.VerifyAssertions) VM._assert(addrReg != S2 && sourceReg != S2);
  	if (VM.VerifyAssertions) VM._assert(addrReg != S3 && sourceReg != S3);
  	emitLQD(S2, addrReg, 0);
  	emitCDD(S3, addrReg, 0);
  	emitSHUFB(S2, sourceReg, S2, S3);
  	emitSTQD(S2, addrReg, 0);
  }

  /** 
   * Generate a set of instructions which stores the DoubleWord in the preferred slot of
   * the source register at the address in addrReg.  Note: addrReg must be aligned to
   * to quadword and offsetReg must be aligned double word
   * Note: Destroys S2 and S3
   * 
   * @param sourceReg Register which contains data to be written
   * @param addrReg	 Register which contains the address to be stored in bytes (must be aligned)
   * @param offsetReg Register which contains offset of address being stored in bytes (must be aligned)
   */
  public final void emitStoreDouble(int sourceReg, int addrReg, int offsetReg) {
  	if (VM.VerifyAssertions) VM._assert(addrReg != S2 && sourceReg != S2 && offsetReg != S2);
  	if (VM.VerifyAssertions) VM._assert(addrReg != S3 && sourceReg != S3 && offsetReg != S3);
	  	emitLQX(S2, addrReg, offsetReg);
	  	emitCDX(S3, addrReg, offsetReg);
	  	emitSHUFB(S2, sourceReg, S2, S3);
	  	emitSTQX(S2, addrReg, offsetReg);
  }
  
  /** 
   * Generate a set of instructions which stores the DoubleWord in the preferred slot of
   * the source register at the address in addrReg.  Note: addrReg and offset must be aligned to 
   * double word.
   * 
   * @param sourceReg Register which contains data to be written
   * @param addrReg	 Register which contains the address to be stored in bytes (must be aligned)
   * @param offset Immediate offset of address being stored in bytes
   */
  public final void emitStoreDouble(int sourceReg, int addrReg, Offset offset) {
  	int offsetI = offset.toInt();
    if (VM.VerifyAssertions) VM._assert((offsetI & (BYTES_IN_DOUBLE - 1)) == 0);
  	if (fits((offsetI >> 4), 10)) {
    	if (VM.VerifyAssertions) VM._assert(addrReg != S2 && sourceReg != S2);
    	if (VM.VerifyAssertions) VM._assert(addrReg != S3 && sourceReg != S3);
	    emitLQD(S2, addrReg, (offsetI >> 4));
	    emitCDD(S3, addrReg, offsetI);
	    emitSHUFB(S2, sourceReg, S2, S3);
	    emitSTQD(S2, addrReg, (offsetI >> 4));
  	} else {
    	if (VM.VerifyAssertions) VM._assert(addrReg != S0 && sourceReg != S0);
			emitILW(S0, offsetI);
			emitStoreDouble(sourceReg, addrReg, S0);  		
  	}
  }
  
  /* 
   * Constant-Formation Instructions
   * ________________________________
   */
  
  /** Emit a constant quad word into the datastream for loading in code */
  public final void emitConstantQuad(int w1, int w2, int w3, int w4) {
  	if (VM.VerifyAssertions) VM._assert((mIP % 4) == 0);
  	mc.addInstruction(w1);
  	mc.addInstruction(w2);
  	mc.addInstruction(w3);
  	mc.addInstruction(w4);
  	mIP += 4;
  }

  public final void emitILW(int RT, int val) {
    if (fits(val, 16)) {
      emitIL(RT, val);
    } else {
      emitILHU(RT, (val >>> 16));
      emitIOHL(RT, val & 0xFFFF);
    }
  }
  
  /** immediate load halfword */
    static final int ILHtemplate = 0x83 << 23;
  
  /** immediate load halfword */
  public final void emitILH(int RT, int immed) {
  	if (VM.VerifyAssertions) VM._assert(fitsU(immed, 16));
  	int mi = ILHtemplate | (0xffff & immed) << 7 | RT << 0;
  	mIP++;
  	mc.addInstruction(mi);
  }
  
  /** immediate load halfword upper */
    static final int ILHUtemplate = 0x82 << 23;
  
  /** immediate load halfword upper */
  public final void emitILHU(int RT, int immed) {
	if (VM.VerifyAssertions) VM._assert(fitsU(immed, 16));
    int mi = ILHUtemplate | (0xffff & immed) << 7 | RT << 0;
    mIP++;
    mc.addInstruction(mi);
  }

  /** immediate load word */
    static final int ILtemplate = 0x81 << 23;
  
  /** immediate load word */
  public final void emitIL(int RT, int immed) {
	if (VM.VerifyAssertions) VM._assert(fits(immed, 16));
    int mi = ILtemplate | (0xffff & immed) << 7 | RT << 0;
    mIP++;
    mc.addInstruction(mi);
  }
  
  /** immediate load address */
    static final int ILAtemplate = 0x21 << 25;
  
  /** immediate load address */
  public final void emitILA(int RT, int immed) {
	if (VM.VerifyAssertions) VM._assert(fitsU(immed, 18));
    int mi = ILAtemplate | (0x3ffff & immed) << 7 | RT << 0;
    mIP++;
    mc.addInstruction(mi);
  }
  
  public final void emitILA(int RT, int relative_address, int label) {
    if (relative_address == 0) {
      reserveForwardBranch(label);
    } else {
      relative_address -= mIP;
    }
    emitILA(RT, relative_address);
  }

  /** immediate Or Halfword Lower */
    static final int IOHLtemplate = 0xc1 << 23;
  
  /** immediate Or Halfword Lower */
  public final void emitIOHL(int RT, int immed) {
	if (VM.VerifyAssertions) VM._assert(fitsU(immed, 16));
    int mi = IOHLtemplate | (0xffff & immed) << 7 | RT << 0;
    mIP++;
    mc.addInstruction(mi);
  }
  
  /* 
   * Integer and Logical Instructions
   * ________________________________
   */

  /** add halfword */
  static final int AHtemplate = 0xc8 << 21;
  
  /** add halfword */
  public final void emitAH(int RT, int RA, int RB) {
    int mi = AHtemplate | RB << 14 | RA << 7 | RT << 0;
    mIP++;
    mc.addInstruction(mi);
  }
  
  /** add halfword immediate */
  static final int AHItemplate = 0x1d << 24;

  /** add halfword immediate */
  public final void emitAHI(int RT, int RA, int immed) {
	if (VM.VerifyAssertions) VM._assert(fits(immed, 10));
    int mi = AHItemplate | (0x3ff & immed) << 14 | RA << 7 | RT << 0;
    mIP++;
    mc.addInstruction(mi);
  }

  /** add word */
  static final int Atemplate = 0xc0 << 21;

  /** add word */
  public final void emitA(int RT, int RA, int RB) {
    int mi = Atemplate | RB << 14 | RA << 7 | RT << 0;
    mIP++;
    mc.addInstruction(mi);
  }

  /** add word immediate */
  static final int AItemplate = 0x1c << 24;

  /** add word immediate */
  public final void emitAI(int RT, int RA, int immed) {
	if (VM.VerifyAssertions) VM._assert(fits(immed, 10));
    int mi = AItemplate | (0x3ff & immed) << 14 | RA << 7 | RT << 0;
    mIP++;
    mc.addInstruction(mi);
  }
  
  /** subtract halfword */
    static final int SFHtemplate = 0x48 << 21;
  
  /** subtract halfword */
  public final void emitSFH(int RT, int RA, int RB) {
	  int mi = SFHtemplate | RB << 14 | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
  }

  // subtract halfword immediate
  static final int SFHItemplate = 0xd << 24;

  public final void emitSFHI(int RT, int RA, int immed) {
	  if (VM.VerifyAssertions) VM._assert(fits(immed, 10));
	  int mi = SFHItemplate | (0x3ff & immed) << 14 | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
  }

  // subtract word
  static final int SFtemplate = 0x40 << 21;

  public final void emitSF(int RT, int RA, int RB) {
	  int mi = SFtemplate | RB << 14 | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
  }

  // subtract word immediate
  static final int SFItemplate = 0xc << 24;

  public final void emitSFI(int RT, int RA, int immed) {
	  if (VM.VerifyAssertions) VM._assert(fits(immed, 10));
	  int mi = SFItemplate | (0x3ff & immed) << 14 | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
  }

  /** add word extended */
  static final int ADDXtemplate = 0x340 << 21;

  /** add word extended */
  public final void emitADDX(int RT, int RA, int RB) {
    int mi = ADDXtemplate | RB << 14 | RA << 7 | RT << 0;
    mIP++;
    mc.addInstruction(mi);
  }

  /** carry generate */
  static final int CGtemplate = 0xc2 << 21;

  /** carry generate */
  public final void emitCG(int RT, int RA, int RB) {
    int mi = CGtemplate | RB << 14 | RA << 7 | RT << 0;
    mIP++;
    mc.addInstruction(mi);
  }

  /** subtract from extended */
  static final int SFXtemplate = 0x341 << 21;

  /** subtract from extended */
  public final void emitSFX(int RT, int RA, int RB) {
    int mi = SFXtemplate | RB << 14 | RA << 7 | RT << 0;
    mIP++;
    mc.addInstruction(mi);
  }

  /** borrow generate */
  static final int BGtemplate = 0x42 << 21;

  /** carry generate */
  public final void emitBG(int RT, int RA, int RB) {
    int mi = BGtemplate | RB << 14 | RA << 7 | RT << 0;
    mIP++;
    mc.addInstruction(mi);
  }
  
  // Multiply
  static final int MPYtemplate = 0x3c4 << 21;

  public final void emitMPY(int RT, int RA, int RB) {
	  int mi = MPYtemplate | RB << 14 | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
  }

  /** Unsigned Multiply */
    static final int MPYUtemplate = 0x3cc << 21;
  
  /** Unsigned Multiply */
  public final void emitMPYU(int RT, int RA, int RB) {
	  int mi = MPYUtemplate | RB << 14 | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
  }

  /** Multiply High */
    static final int MPYHtemplate = 0x3c5 << 21;
  
  /** Multiply High */
  public final void emitMPYH(int RT, int RA, int RB) {
	  int mi = MPYHtemplate | RB << 14 | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
  }

  /** Multiply Immediate */
    static final int MPYItemplate = 0x74 << 24;
  
  /** Multiply Immediate */
  public final void emitMPYI(int RT, int RA, int immed) {
	  if (VM.VerifyAssertions) VM._assert(fits(immed, 10));
	  int mi = MPYItemplate | (0x3ff & immed) << 14 | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
  }

  /** Unsigned Multiply Immediate */
    static final int MPYUItemplate = 0x75 << 24;
  
  /** Unsigned Multiply Immediate */
  public final void emitMPYUI(int RT, int RA, int immed) {
	  if (VM.VerifyAssertions) VM._assert(fits(immed, 10));
	  int mi = MPYUItemplate | (0x3ff & immed) << 14 | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
  }

  /** Count Leading Zero's */
    static final int CLZtemplate = 0x2a5 << 21;

  /** Count Leading Zero's */
  public final void emitCLZ(int RT, int RA) {
	  int mi = CLZtemplate | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
  }

  /** Form Selection Mask for Words */
    static final int FSMtemplate = 0x1b4 << 21;
  
  /** Form Selection Mask for Words */
  public final void emitFSM(int RT, int RA) {
	  int mi = FSMtemplate | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
  }
  
  /** Form Selection Mask for Bytes Immediate */
  static final int FSMBItemplate = 0x65 << 23;

  /** Form Selection Mask for Words */
  public final void emitFSMBI(int RT, int immed) {
	  if (VM.VerifyAssertions) VM._assert(fitsU(immed, 16));
  	int mi = FSMBItemplate | (immed & 0xffff) << 7 | RT << 0;
  	mIP++;
  	mc.addInstruction(mi);
  }

  /** Extend Sign Byte to Halfword */
    static final int XSBHtemplate = 0x2b6 << 21;
  
  /** Extend Sign Byte to Halfword */
  public final void emitXSBH(int RT, int RA) {
	  int mi = XSBHtemplate | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
  }

  /** Extend Sign Halfword to Word */
    static final int XSHWtemplate = 0x2ae << 21;
  
  /** Extend Sign Halfword to Word */
  public final void emitXSHW(int RT, int RA) {
	  int mi = XSHWtemplate | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
  }

  /** Extend Sign Word to DoubleWord */
    static final int XSWDtemplate = 0x2a6 << 21;
  
  /** Extend Sign Word to DoubleWord */
  public final void emitXSWD(int RT, int RA) {
	  int mi = XSWDtemplate | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
  }
  
  /** And */
    static final int ANDtemplate = 0xc1 << 21;
  
  /** And */
  public final void emitAND(int RT, int RA, int RB) {
	  int mi = ANDtemplate | RB << 14 | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
  }

  /** And with Compliment  */
    static final int ANDCtemplate = 0x2c1 << 21;
  
  /** And with Compliment  */
  public final void emitANDC(int RT, int RA, int RB) {
	  int mi = ANDCtemplate | RB << 14 | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
  }
  
  /** And Byte Immediate */
    static final int ANDBItemplate = 0x16 << 24;
  
  /** And Byte Immediate */
  public final void emitANDBI(int RT, int RA, int immed) {
	  if (VM.VerifyAssertions) VM._assert(fitsU(immed, 8));
	  int mi = ANDBItemplate | (0x3ff & immed) << 14 | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
  }
  
  /** And Halfword Immediate */
    static final int ANDHItemplate = 0x15 << 24;
  
  /** And Halfword Immediate */
  public final void emitANDHI(int RT, int RA, int immed) {
	  if (VM.VerifyAssertions) VM._assert(fits(immed, 10));
	  int mi = ANDHItemplate | (0x3ff & immed) << 14 | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
  }

  /** And Word Immediate */
    static final int ANDItemplate = 0x14 << 24;
  
  /** And Word Immediate */
  public final void emitANDI(int RT, int RA, int immed) {
	  if (VM.VerifyAssertions) VM._assert(fits(immed, 10));
	  int mi = ANDItemplate | (0x3ff & immed) << 14 | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
  }
  
  /** Or */
    static final int ORtemplate = 0x41 << 21;
  
  /** Or */
  public final void emitOR(int RT, int RA, int RB) {
	  int mi = ORtemplate | RB << 14 | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
  }

  /** Or with Compliment  */
    static final int ORCtemplate = 0x2c9 << 21;
  
  /** Or with Compliment  */
  public final void emitORC(int RT, int RA, int RB) {
	  int mi = ORCtemplate | RB << 14 | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
  }
  
  /** Or Byte Immediate */
    static final int ORBItemplate = 0x6 << 24;
  
  /** Or Byte Immediate */
  public final void emitORBI(int RT, int RA, int immed) {
	  if (VM.VerifyAssertions) VM._assert(fitsU(immed, 8));
	  int mi = ORBItemplate | (0x3ff & immed) << 14 | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
  }
  
  /** Or Halfword Immediate */
    static final int ORHItemplate = 0x5 << 24;
  
  /** Or Halfword Immediate */
  public final void emitORHI(int RT, int RA, int immed) {
	  if (VM.VerifyAssertions) VM._assert(fits(immed, 10));
	  int mi = ORHItemplate | (0x3ff & immed) << 14 | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
  }

  /** Or Word Immediate */
    static final int ORItemplate = 0x4 << 24;
  
  /** Or Word Immediate */
  public final void emitORI(int RT, int RA, int immed) {
	  if (VM.VerifyAssertions) VM._assert(fits(immed, 10));
	  int mi = ORItemplate | (0x3ff & immed) << 14 | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
  }
  
  /** Xor */
    static final int XORtemplate = 0x241 << 21;
  
  /** Xor */
  public final void emitXOR(int RT, int RA, int RB) {
	  int mi = XORtemplate | RB << 14 | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
  }
  
  /** Xor Byte Immediate */
    static final int XORBItemplate = 0x46 << 24;
  
  /** Xor Byte Immediate */
  public final void emitXORBI(int RT, int RA, int immed) {
	  if (VM.VerifyAssertions) VM._assert(fitsU(immed, 8));
	  int mi = XORBItemplate | (0x3ff & immed) << 14 | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
  }
  
  /** Xor Halfword Immediate */
    static final int XORHItemplate = 0x45 << 24;
  
  /** Xor Halfword Immediate */
  public final void emitXORHI(int RT, int RA, int immed) {
	  if (VM.VerifyAssertions) VM._assert(fits(immed, 10));
	  int mi = XORHItemplate | (0x3ff & immed) << 14 | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
  }

  /** Xor Word Immediate */
    static final int XORItemplate = 0x44 << 24;
  
  /** Xor Word Immediate */
  public final void emitXORI(int RT, int RA, int immed) {
	  if (VM.VerifyAssertions) VM._assert(fits(immed, 10));
	  int mi = XORItemplate | (0x3ff & immed) << 14 | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
  }
  
  /** Nand */
    static final int NANDtemplate = 0xc9 << 21;
  
  /** Nand */
  public final void emitNAND(int RT, int RA, int RB) {
	  int mi = NANDtemplate | RB << 14 | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
  }

  /** Nor */
    static final int NORtemplate = 0x49 << 21;
  
  /** Nor */
  public final void emitNOR(int RT, int RA, int RB) {
	  int mi = NORtemplate | RB << 14 | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
  }

  /** Equivalent */
    static final int EQVtemplate = 0x249 << 21;
  
  /** Equivalent */
  public final void emitEQV(int RT, int RA, int RB) {
	  int mi = EQVtemplate | RB << 14 | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
  }
  
  /** Select Bits */
    static final int SELBtemplate = 0x8 << 28;
  
  /** Select Bits */
  public final void emitSELB(int RT, int RA, int RB, int RC) {
	  int mi = SELBtemplate | RT << 21 | RB << 14 | RA << 7 | RC << 0;
	  mIP++;
	  mc.addInstruction(mi);
  }  
  
  /** Shuffle Bytes */
    static final int SHUFBtemplate = 0xB << 28;
  
  /** Shuffle Bytes */
  public final void emitSHUFB(int RT, int RA, int RB, int RC) {
	  int mi = SHUFBtemplate | RT << 21 |  RB << 14 | RA << 7 | RC << 0;
	  mIP++;
	  mc.addInstruction(mi);
  }
  

  /* 
   * Shift and Rotate Instructions
   * ________________________________
   */
  
  /** Shift Left Halfword */
    static final int SHLHtemplate = 0x5f << 21;
  
  /** Shift Left Halfword */
  public final void emitSHLH(int RT, int RA, int RB) {
	  int mi = SHLHtemplate | RB << 14 | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
  }

  /** Shift Left Halfword Immediate */
    static final int SHLHItemplate = 0x7f << 21;
  
  /** Shift Left Halfword Immediate */
  public final void emitSHLHI(int RT, int RA, int immed) {
	  if (VM.VerifyAssertions) VM._assert(fits(immed, 7));
	  int mi = SHLHItemplate | (0x7f & immed) << 14 | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
  }

  /** Shift Left Word */
    static final int SHLtemplate = 0x5b << 21;
  
  /** Shift Left Word */
  public final void emitSHL(int RT, int RA, int RB) {
	  int mi = SHLtemplate | RB << 14 | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
  }

  /** Shift Left Word Immediate */
    static final int SHLItemplate = 0x7b << 21;
  
  /** Shift Left Word Immediate */
  public final void emitSHLI(int RT, int RA, int immed) {
	  if (VM.VerifyAssertions) VM._assert(fits(immed, 7));
	  int mi = SHLItemplate | (0x7f & immed) << 14 | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
  }
  
  /** Shift Left QuadWord by Bytes */
  static final int SHLQBYtemplate = 0x1df << 21;

/** Shift Left QuadWord by Bytes  */
  public final void emitSHLQBY(int RT, int RA, int RB) {
  	int mi = SHLQBYtemplate | RB << 14 | RA << 7 | RT << 0;
  	mIP++;
  	mc.addInstruction(mi);
  }

  /** Shift Left QuadWord by Bytes Immediate */
  static final int SHLQBYItemplate = 0x1ff << 21;

	/** Shift Left  QuadWord by Bytes Immediate */
	public final void emitSHLQBYI(int RT, int RA, int immed) {
	  if (VM.VerifyAssertions) VM._assert(fits(immed, 7));
	  int mi = SHLQBYItemplate | (0x7f & immed) << 14 | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
	}

  /** Shift Left QuadWord by Bytes from Bit Shift Count */
  static final int SHLQBYBItemplate = 0x1cf << 21;

/** Shift Left QuadWord by Bytes from Bit Shift Count */
  public final void emitSHLQBYBI(int RT, int RA, int RB) {
  	int mi = SHLQBYBItemplate | RB << 14 | RA << 7 | RT << 0;
  	mIP++;
  	mc.addInstruction(mi);
  }
	
  /** Shift Left QuadWord by Bits */
  static final int SHLQBItemplate = 0x1db << 21;

  /** Shift Left QuadWord by Bits  */
  public final void emitSHLQBI(int RT, int RA, int RB) {
  	int mi = SHLQBItemplate | RB << 14 | RA << 7 | RT << 0;
  	mIP++;
  	mc.addInstruction(mi);
  }

  /** Shift Left QuadWord by Bits Immediate */
  static final int SHLQBIItemplate = 0x1fb << 21;

	/** Shift Left  QuadWord by Bits Immediate */
	public final void emitSHLQBII(int RT, int RA, int immed) {
	  if (VM.VerifyAssertions) VM._assert(fits(immed, 7));
	  int mi = SHLQBIItemplate | (0x7f & immed) << 14 | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
	}

  /** Rotate Left Halfword */
    static final int ROTHtemplate = 0x5c << 21;
  
  /** Rotate Left Halfword */
  public final void emitROTH(int RT, int RA, int RB) {
	  int mi = ROTHtemplate | RB << 14 | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
  }

  /** Rotate Left Halfword Immediate */
    static final int ROTHItemplate = 0x7c << 21;
  
  /** Rotate Left Halfword Immediate */
  public final void emitROTHI(int RT, int RA, int immed) {
	  int mi = ROTHItemplate | (0x7f & immed) << 14 | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
  }

  /** Rotate Left Word */
    static final int ROTtemplate = 0x58 << 21;
  
  /** Rotate Left Word */
  public final void emitROT(int RT, int RA, int RB) {
	  int mi = ROTtemplate | RB << 14 | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
  }

  /** Rotate Left Word Immediate */
    static final int ROTItemplate = 0x78 << 21;
  
  /** Rotate Left Word Immediate */
  public final void emitROTI(int RT, int RA, int immed) {
	  int mi = ROTItemplate | (0x7f & immed) << 14 | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
  }
  
  /** Rotate Left Word and Mask */
    static final int ROTMtemplate = 0x59 << 21;
  
  /** Rotate Left Word and Mask */
  public final void emitROTM(int RT, int RA, int RB) {
	  int mi = ROTMtemplate | RB << 14 | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
  }

  /** Rotate Left Word and Mask Immediate */
    static final int ROTMItemplate = 0x79 << 21;
  
  /** Rotate Left Word and Mask Immediate */
  public final void emitROTMI(int RT, int RA, int immed) {
	  int mi = ROTMItemplate | (0x7f & immed) << 14 | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
  }


  /** Rotate Left Word and Mask Algebraic */
    static final int ROTMAtemplate = 0x5a << 21;
  
  /** Rotate Left Word and Mask Algebraic */
  public final void emitROTMA(int RT, int RA, int RB) {
	  int mi = ROTMAtemplate | RB << 14 | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
  }

  /** Rotate Left Word and Mask Algebraic Immediate */
    static final int ROTMAItemplate = 0x7a << 21;
  
  /** Rotate Left Word and Mask Algebraic Immediate */
  public final void emitROTMAI(int RT, int RA, int immed) {
	  int mi = ROTMAItemplate | (0x7f & immed) << 14 | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
  }
  
  /** Rotate Left Quadword by Bytes */
    static final int ROTQBYtemplate = 0x1dc << 21;
  
  /** Rotate Left Quadword by Bytes  */
  public final void emitROTQBY(int RT, int RA, int RB) {
	  int mi = ROTQBYtemplate | RB << 14 | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
  }
  
  /** Rotate Left Quadword by Bytes  Immediate */
  static final int ROTQBYItemplate = 0x1fc << 21;

  /** Rotate Left Quadword by Bytes Immediate */
  public final void emitROTQBYI(int RT, int RA, int immed) {
	  int mi = ROTQBYItemplate | (0x7f & immed) << 14 | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
  }
  
  /** Rotate Left Quadword by Bytes and Mask */
  static final int ROTQMBYtemplate = 0x1dd << 21;

  /** Rotate Left Quadword by Bytes and Mask  */
  public final void emitROTQMBY(int RT, int RA, int RB) {
	  int mi = ROTQMBYtemplate | RB << 14 | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
  }
  
  /** Rotate Left Quadword by Bytes and Mask Immediate */
  static final int ROTQMBYItemplate = 0x1fd << 21;

  /** Rotate Left Quadword by Bytes and Mask Immediate */
  public final void emitROTQMBYI(int RT, int RA, int immed) {
	  int mi = ROTQMBYItemplate | (0x7f & immed) << 14 | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
  }

  /** Rotate Left Quadword Bytes by Bit Shift Count */
  static final int ROTQBYBItemplate = 0x1cc << 21;

  /** Rotate Left Quadword Bytes by Bit Shift Count  */
  public final void emitROTQBYBI(int RT, int RA, int RB) {
	  int mi = ROTQBYBItemplate | RB << 14 | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
  } 

  /** Rotate Left Quadword by Bits */
  static final int ROTQBItemplate = 0x1d8 << 21;

  /** Rotate Left Quadword by Bits */
  public final void emitROTQBI(int RT, int RA, int RB) {
	  int mi = ROTQBItemplate | RB << 14 | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
  } 
  
  /** Rotate Left Quadword by Bits and Mask */
  static final int ROTQMBItemplate = 0x1d9 << 21;

  /** Rotate Left Quadword by Bytes and Mask  */
  public final void emitROTQMBI(int RT, int RA, int RB) {
	  int mi = ROTQMBItemplate | RB << 14 | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
  }
  
  /** Rotate Left Quadword by Bits and Mask Immediate */
  static final int ROTQMBIItemplate = 0x1f9 << 21;

  /** Rotate Left Quadword by Bytes and Mask Immediate */
  public final void emitROTQMBII(int RT, int RA, int immed) {
	  int mi = ROTQMBIItemplate | (0x7f & immed) << 14 | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
  }
  
  /** Rotate Left and Mask Quadword Bytes by Bit Shift Count */
  static final int ROTQMBYBItemplate = 0x1cd << 21;

  /** Rotate Left and Mask Quadword Bytes by Bit Shift Count  */
  public final void emitROTQMBYBI(int RT, int RA, int RB) {
	  int mi = ROTQMBYBItemplate | RB << 14 | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
  } 
  
  /* 
   * Branch and Compare Instructions
   * ________________________________
   */
  
  /** Compare Equal Byte */
    static final int CEQBtemplate = 0x3d0 << 21;
  
  /** Compare Equal Byte */
  public final void emitCEQB(int RT, int RA, int RB) {
	  int mi = CEQBtemplate | RB << 14 | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
  }

  /** Compare Equal Byte Immediate */
    static final int CEQBItemplate = 0x7e << 24;
  
  /** Compare Equal Byte Immediate */
  public final void emitCEQBI(int RT, int RA, int immed) {
	  if (VM.VerifyAssertions) VM._assert(fits(immed, 10));
	  int mi = CEQBItemplate | (0x3ff & immed) << 14 | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
  }
  
  /** Compare Equal Halfword */
    static final int CEQHtemplate = 0x3c8 << 21;
  
  /** Compare Equal Halfword */
  public final void emitCEQH(int RT, int RA, int RB) {
	  int mi = CEQHtemplate | RB << 14 | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
  }

  /** Compare Equal Halfword Immediate */
    static final int CEQHItemplate = 0x7d << 24;
  
  /** Compare Equal Halfword Immediate */
  public final void emitCEQHI(int RT, int RA, int immed) {
	  if (VM.VerifyAssertions) VM._assert(fits(immed, 10));
	  int mi = CEQHItemplate | (0x3ff & immed) << 14 | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
  }
  
  /** Compare Equal Word */
    static final int CEQtemplate = 0x3c0 << 21;
  
  /** Compare Equal Word */
  public final void emitCEQ(int RT, int RA, int RB) {
	  int mi = CEQtemplate | RB << 14 | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
  }

  /** Compare Equal Word Immediate */
    static final int CEQItemplate = 0x7c << 24;
  
  /** Compare Equal Word Immediate */
  public final void emitCEQI(int RT, int RA, int immed) {
	  if (VM.VerifyAssertions) VM._assert(fits(immed, 10));
	  int mi = CEQItemplate | (0x3ff & immed) << 14 | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
  }
  
  /** Compare Greater Than Byte */
    static final int CGTBtemplate = 0x250 << 21;
  
  /** Compare Greater Than Byte */
  public final void emitCGTB(int RT, int RA, int RB) {
	  int mi = CGTBtemplate | RB << 14 | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
  }

  /** Compare Greater Than Byte Immediate */
    static final int CGTBItemplate = 0x4e << 24;
  
  /** Compare Greater Than Byte Immediate */
  public final void emitCGTBI(int RT, int RA, int immed) {
	  if (VM.VerifyAssertions) VM._assert(fits(immed, 10));
	  int mi = CGTBItemplate | (0x3ff & immed) << 14 | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
  }
  
  /** Compare Greater Than Halfword */
    static final int CGTHtemplate = 0x248 << 21;
  
  /** Compare Greater Than Halfword */
  public final void emitCGTH(int RT, int RA, int RB) {
	  int mi = CGTHtemplate | RB << 14 | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
  }

  /** Compare Greater Than Halfword Immediate */
    static final int CGTHItemplate = 0x4d << 24;
  
  /** Compare Greater Than Halfword Immediate */
  public final void emitCGTHI(int RT, int RA, int immed) {
	  if (VM.VerifyAssertions) VM._assert(fits(immed, 10));
	  int mi = CGTHItemplate | (0x3ff & immed) << 14 | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
  }
  
  /** Compare Greater Than Word */
    static final int CGTtemplate = 0x240 << 21;
  
  /** Compare Greater Than Word (RA > RB) */
  public final void emitCGT(int RT, int RA, int RB) {
	  int mi = CGTtemplate | RB << 14 | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
  }

  /** Compare Greater Than Word Immediate */
    static final int CGTItemplate = 0x4c << 24;
  
  /** Compare Greater Than Word Immediate */
  public final void emitCGTI(int RT, int RA, int immed) {
	  if (VM.VerifyAssertions) VM._assert(fits(immed, 10));
	  int mi = CGTItemplate | (0x3ff & immed) << 14 | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
  }
  
  /** Compare Logical Greater Than Byte */
    static final int CLGTBtemplate = 0x2d0 << 21;
  
  /** Compare Logical Greater Than Byte */
  public final void emitCLGTB(int RT, int RA, int RB) {
	  int mi = CLGTBtemplate | RB << 14 | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
  }

  /** Compare Logical Greater Than Byte Immediate */
    static final int CLGTBItemplate = 0x5e << 24;
  
  /** Compare Logical Greater Than Byte Immediate */
  public final void emitCLGTBI(int RT, int RA, int immed) {
	  if (VM.VerifyAssertions) VM._assert(fits(immed, 10));
	  int mi = CLGTBItemplate | (0x3ff & immed) << 14 | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
  }
  
  /** Compare Logical Greater Than Halfword */
    static final int CLGTHtemplate = 0x2c8 << 21;
  
  /** Compare Logical Greater Than Halfword */
  public final void emitCLGTH(int RT, int RA, int RB) {
	  int mi = CLGTHtemplate | RB << 14 | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
  }

  /** Compare Logical Greater Than Halfword Immediate */
    static final int CLGTHItemplate = 0x5d << 24;
  
  /** Compare Logical Greater Than Halfword Immediate */
  public final void emitCLGTHI(int RT, int RA, int immed) {
	  if (VM.VerifyAssertions) VM._assert(fits(immed, 10));
	  int mi = CLGTHItemplate | (0x3ff & immed) << 14 | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
  }
  
  /** Compare Logical Greater Than Word */
    static final int CLGTtemplate = 0x2c0 << 21;
  
  /** Compare Logical Greater Than Word */
  public final void emitCLGT(int RT, int RA, int RB) {
	  int mi = CLGTtemplate | RB << 14 | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
  }

  /** Compare Logical Greater Than Word Immediate */
    static final int CLGTItemplate = 0x5c << 24;
  
  /** Compare Logical Greater Than Word Immediate */
  public final void emitCLGTI(int RT, int RA, int immed) {
	  if (VM.VerifyAssertions) VM._assert(fits(immed, 10));
	  int mi = CLGTItemplate | (0x3ff & immed) << 14 | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
  }
  
  // Branch Relative
  static final int BRtemplate = 0x64 << 23;

  public void _emitBR(int relative_address) {
    if (VM.VerifyAssertions) VM._assert(fits(relative_address, 16));
    int mi = BRtemplate | (relative_address & 0xFFFF) << 7;
    mIP++;
    mc.addInstruction(mi);
  }

  public final void emitBR(int relative_address, int label) {
    if (relative_address == 0) {
      reserveForwardBranch(label);
    } else {
      relative_address -= mIP;
    }
    _emitBR(relative_address);
  }

  public final void emitBR(int relative_address) {
    relative_address -= mIP;
    if (VM.VerifyAssertions) VM._assert(relative_address < 0);
    _emitBR(relative_address);
  }

  public final VM_ForwardReference emitForwardBR() {
    VM_ForwardReference fr;
    if (compiler != null) {
      fr = new VM_AssemblerShortBranch(mIP, compiler.spTopOffset);
    } else {
      fr = new VM_ForwardReference.ShortBranch(mIP);
    }
    _emitBR(0);
    return fr;
  }

  // Branch Absolute
  static final int BRAtemplate = 0x60 << 23;

  public final void emitBRA(int address) {
  	// convert branch from byte to word
  	address = address >> LG_INSTRUCTION_WIDTH;
    if (VM.VerifyAssertions) VM._assert(fits(address, 16));
    int mi = BRAtemplate | (address & 0xFFFF) << 7;
    mIP++;
    mc.addInstruction(mi);
  }
  
  // Branch Relative and Set Link
  static final int BRSLtemplate = 0x66 << 23;

  public void _emitBRSL(int RT, int relative_address) {
    if (VM.VerifyAssertions) VM._assert(fits(relative_address, 16));
    int mi = BRSLtemplate | (relative_address & 0xFFFF) << 7 | RT << 0;
    mIP++;
    mc.addInstruction(mi);
  }

  public final void emitBRSL(int RT, int relative_address, int label) {
    if (relative_address == 0) {
      reserveForwardBranch(label);
    } else {
      relative_address -= mIP;
    }
    _emitBRSL(RT, relative_address);
  }

  public final VM_ForwardReference emitForwardBRSL(int RT) {
    VM_ForwardReference fr;
    if (compiler != null) {
      fr = new VM_AssemblerShortBranch(mIP, compiler.spTopOffset);
    } else {
      fr = new VM_ForwardReference.ShortBranch(mIP);
    }
    _emitBRSL(RT, 0);
    return fr;
  }
  
  // Branch Absolute and Set Link
  static final int BRASLtemplate = 0x62 << 23;

  public final void emitBRASL(int RT, int address) {
  	address = address >> LG_INSTRUCTION_WIDTH;
    if (VM.VerifyAssertions) VM._assert(fits(address, 16));
    int mi = BRASLtemplate | (address & 0xFFFF) << 7 | RT << 0;
    mIP++;
    mc.addInstruction(mi);
  }
  
  // Branch Indirect
  static final int BItemplate = 0x1a8 << 21;

  public final void _emitBI(int RA, int itr_change) {
	if (VM.VerifyAssertions) VM._assert((itr_change >= NO_CHANGE_INT) && (itr_change <= DISABLE_INT));
    int mi = BItemplate | itr_change << 18 | RA << 7;
    mIP++;
    mc.addInstruction(mi);
  }
  
  public final void emitBI(int RA) {
	  _emitBI(RA, NO_CHANGE_INT);
  }
  
  public final void emitBIE(int RA) {
	  _emitBI(RA, ENABLE_INT);
  }

  public final void emitBID(int RA) {
	  _emitBI(RA, DISABLE_INT);
  }  
  
  // Branch Indirect and Set Link
  static final int BISLtemplate = 0x1a9 << 21;

  public final void _emitBISL(int RT, int RA, int itr_change) {
	if (VM.VerifyAssertions) VM._assert((itr_change >= NO_CHANGE_INT) && (itr_change <= DISABLE_INT));
    int mi = BISLtemplate | itr_change << 18 | RA << 7 | RT << 0;
    mIP++;
    mc.addInstruction(mi);
  }
  
  public final void emitBISL(int RT, int RA) {
	  _emitBISL(RT, RA, NO_CHANGE_INT);
  }
  
  public final void emitBISLE(int RT, int RA) {
	  _emitBISL(RT, RA, ENABLE_INT);
  }

  public final void emitBISLD(int RT, int RA) {
	  _emitBISL(RT, RA, DISABLE_INT);
  }
  
  // Branch Relative If Not Zero Word
  static final int BRNZtemplate = 0x42 << 23;

  private void _emitBRNZ(int RT, int relative_address) {
    if (VM.VerifyAssertions) VM._assert(fits(relative_address, 16));
    int mi = BRNZtemplate | (relative_address & 0xFFFF) << 7 | RT << 0;
    mIP++;
    mc.addInstruction(mi);
  }

  public final void emitBRNZ(int RT, int relative_address, int label) {
    if (relative_address == 0) {
      reserveForwardBranch(label);
    } else {
      relative_address -= mIP;
    }
    _emitBRNZ(RT, relative_address);
  }

  public final void emitBRNZ(int RT, int relative_address) {
    relative_address -= mIP;
    if (VM.VerifyAssertions) VM._assert(relative_address < 0);
    _emitBRNZ(RT, relative_address);
  }

  public final VM_ForwardReference emitForwardBRNZ(int RT) {
    VM_ForwardReference fr;
    if (compiler != null) {
      fr = new VM_AssemblerShortBranch(mIP, compiler.spTopOffset);
    } else {
      fr = new VM_ForwardReference.ShortBranch(mIP);
    }
    _emitBRNZ(RT, 0);
    return fr;
  }
  
  // Branch Relative If Zero Word
  static final int BRZtemplate = 0x40 << 23;

  private void _emitBRZ(int RT, int relative_address) {
    if (VM.VerifyAssertions) VM._assert(fits(relative_address, 16));
    int mi = BRZtemplate | (relative_address & 0xFFFF) << 7 | RT << 0;
    mIP++;
    mc.addInstruction(mi);
  }

  public final void emitBRZ(int RT, int relative_address, int label) {
    if (relative_address == 0) {
      reserveForwardBranch(label);
    } else {
      relative_address -= mIP;
    }
    _emitBRZ(RT, relative_address);
  }

  public final void emitBRZ(int RT, int relative_address) {
    relative_address -= mIP;
    if (VM.VerifyAssertions) VM._assert(relative_address < 0);
    _emitBRZ(RT, relative_address);
  }

  public final VM_ForwardReference emitForwardBRZ(int RT) {
    VM_ForwardReference fr;
    if (compiler != null) {
      fr = new VM_AssemblerShortBranch(mIP, compiler.spTopOffset);
    } else {
      fr = new VM_ForwardReference.ShortBranch(mIP);
    }
    _emitBRZ(RT, 0);
    return fr;
  }

  // Branch Indirect If Zero
  static final int BIZtemplate = 0x128 << 21;

  public final void _emitBIZ(int RT, int RA, int itr_change) {
	if (VM.VerifyAssertions) VM._assert((itr_change >= NO_CHANGE_INT) && (itr_change <= DISABLE_INT));
    int mi = BIZtemplate | itr_change << 18 | RA << 7 | RT << 0;
    mIP++;
    mc.addInstruction(mi);
  }

  public final void emitBIZ(int RT, int RA) {
	  _emitBIZ(RT, RA, NO_CHANGE_INT);
  }
  
  public final void emitBIZE(int RT, int RA) {
	  _emitBIZ(RT, RA, ENABLE_INT);
  }

  public final void emitBIZD(int RT, int RA) {
	  _emitBIZ(RT, RA, DISABLE_INT);
  }

  // Branch Indirect If Not Zero
  static final int BINZtemplate = 0x129 << 21;

  public final void _emitBINZ(int RT, int RA, int itr_change) {
	if (VM.VerifyAssertions) VM._assert((itr_change >= NO_CHANGE_INT) && (itr_change <= DISABLE_INT));
    int mi = BINZtemplate | itr_change << 18 | RA << 7 | RT << 0;
    mIP++;
    mc.addInstruction(mi);
  }

  public final void emitBINZ(int RT, int RA) {
	  _emitBINZ(RT, RA, NO_CHANGE_INT);
  }
  
  public final void emitBINZE(int RT, int RA) {
	  _emitBINZ(RT, RA, ENABLE_INT);
  }

  public final void emitBINZD(int RT, int RA) {
	  _emitBINZ(RT, RA, DISABLE_INT);
  }
  
  // Switch Case Address
  public final void emitSwitchCase(int i, int relative_address, int bTarget) {
  	//delta i: difference between address of case i and of delta 0
    int data = i << 2;
    if (relative_address == 0) {
      reserveForwardCase(bTarget);
    } else {
      data += ((relative_address - mIP) << 2);
    }
    mIP++;
    mc.addInstruction(data);
  }

  /* 
   * Floating Point Instructions
   * ________________________________
   */

	// Floating Add
	static final int FAtemplate = 0x2c4 << 21;

	public final void emitFA(int RT, int RA, int RB) {
	  int mi = FAtemplate | RB << 14 | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
	}

	// Double Floating Add
	static final int DFAtemplate = 0x2cc << 21;

	public final void emitDFA(int RT, int RA, int RB) {
	  int mi = DFAtemplate | RB << 14 | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
	}

	// Floating Subtract
	static final int FStemplate = 0x2c5 << 21;

	public final void emitFS(int RT, int RA, int RB) {
	  int mi = FStemplate | RB << 14 | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
	}

	// Double Floating Subtract
	static final int DFStemplate = 0x2cd << 21;

	public final void emitDFS(int RT, int RA, int RB) {
	  int mi = DFStemplate | RB << 14 | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
	}

	// Floating Multiply
	static final int FMtemplate = 0x2c6 << 21;

	public final void emitFM(int RT, int RA, int RB) {
	  int mi = FMtemplate | RB << 14 | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
	}
	
	// Double Floating Multiply
	static final int DFMtemplate = 0x2ce << 21;

	public final void emitDFM(int RT, int RA, int RB) {
	  int mi = DFMtemplate | RB << 14 | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
	}

	// Convert Signed Integer to Floating
	static final int CSFLTtemplate = 0x1da << 22;

	public final void emitCSFLT(int RT, int RA, int immed) {
    if (VM.VerifyAssertions) VM._assert(fitsU(immed, 8));
	  int mi = CSFLTtemplate | (0xff & immed) << 14 | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
	}

	// Convert Floating to Signed Integer
	static final int CFLTStemplate = 0x1d8 << 22;

	public final void emitCFLTS(int RT, int RA, int immed) {
    if (VM.VerifyAssertions) VM._assert(fitsU(immed, 8));
	  int mi = CFLTStemplate | (0xff & immed) << 14 | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
	}

	// Convert Unsigned Integer to Floating
	static final int CUFLTtemplate = 0x1db << 22;

	public final void emitCUFLT(int RT, int RA, int immed) {
    if (VM.VerifyAssertions) VM._assert(fitsU(immed, 8));
	  int mi = CUFLTtemplate | (0xff & immed) << 14 | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
	}

	// Convert Floating to Unsigned Integer
	static final int CFLTUtemplate = 0x1d9 << 22;

	public final void emitCFLTU(int RT, int RA, int immed) {
    if (VM.VerifyAssertions) VM._assert(fitsU(immed, 8));
	  int mi = CFLTUtemplate | (0xff & immed) << 14 | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
	}

	// Floating Round Double to Single
	static final int FRDStemplate = 0x3b9 << 21;

	public final void emitFRDS(int RT, int RA) {
	  int mi = FRDStemplate | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
	}

	// Floating Sign Extend Single to Double
	static final int FESDtemplate = 0x3b8 << 21;

	public final void emitFESD(int RT, int RA) {
	  int mi = FESDtemplate | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
	}

	// Floating Compare Equal
	static final int FCEQtemplate = 0x3c2 << 21;

	public final void emitFCEQ(int RT, int RA, int RB) {
	  int mi = FCEQtemplate | RB << 14 | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
	}

	// Floating Compare Greater Than
	static final int FCGTtemplate = 0x2c2 << 21;

	public final void emitFCGT(int RT, int RA, int RB) {
	  int mi = FCGTtemplate | RB << 14 | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
	}

	// Double Floating Compare Equal
	static final int DFCEQtemplate = 0x3c3 << 21;

	public final void emitDFCEQ(int RT, int RA, int RB) {
	  int mi = DFCEQtemplate | RB << 14 | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
	}

	// Double Floating Compare Greater Than
	static final int DFCGTtemplate = 0x2c3 << 21;

	public final void emitDFCGT(int RT, int RA, int RB) {
	  int mi = DFCGTtemplate | RB << 14 | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
	}
	
	/** Floating Multiply and Add */
	static final int FMAtemplate = 0xe << 28;

	/** Floating Multiply and Add */
	public final void emitFMA(int RT, int RA, int RB, int RC) {
	  int mi = FMAtemplate | RT << 21 | RB << 14 | RA << 7 | RC << 0;
	  mIP++;
	  mc.addInstruction(mi);
	}

	/** Floating Multiply and Subtract */
	static final int FMStemplate = 0xf << 28;

	/** Floating Multiply and Subtract */
	public final void emitFMS(int RT, int RA, int RB, int RC) {
	  int mi = FMStemplate | RT << 21 | RB << 14 | RA << 7 | RC << 0;
	  mIP++;
	  mc.addInstruction(mi);
	}

	/** Floating Negative Multiply and Subtract */
	static final int FNMStemplate = 0xd << 28;

	/** Floating Negative Multiply and Subtract */
	public final void emitFNMS(int RT, int RA, int RB, int RC) {
	  int mi = FNMStemplate | RT << 21 | RB << 14 | RA << 7 | RC << 0;
	  mIP++;
	  mc.addInstruction(mi);
	}
	
	/** Floating Reciprocal Estimate */
	static final int FRESTtemplate = 0x1b8 << 21;

	/** Floating Reciprocal Estimate */
	public final void emitFREST(int RT, int RA) {
	  int mi = FRESTtemplate | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
	}

	/** Floating Interpolate */
	static final int FItemplate = 0x3d4 << 21;

	/** Floating Interpolate */
	public final void emitFI(int RT, int RA, int RB) {
	  int mi = FItemplate | RB << 14 | RA << 7 | RT << 0;
	  mIP++;
	  mc.addInstruction(mi);
	}

  /* 
   * Control Instructions
   * ________________________________
   */
	
	// nop
	static final int NOPtemplate = 0x201 << 21;

	public final void emitNOP() {
	  int mi = NOPtemplate;
	  mIP++;
	  mc.addInstruction(mi);
	}
	
	// Syncronize
	static final int SYNCtemplate = 0x2 << 21;

	public final void emitSYNC() {
	  int mi = SYNCtemplate;
	  mIP++;
	  mc.addInstruction(mi);
	}

	public final void emitSYNCC() {
	  int mi = SYNCtemplate | 0x1 << 20;
	  mIP++;
	  mc.addInstruction(mi);
	}

	// Syncronize Data
	static final int DSYNCtemplate = 0x3 << 21;

	public final void emitDSYNC() {
	  int mi = DSYNCtemplate;
	  mIP++;
	  mc.addInstruction(mi);
	}
	
	/** stop and signal */
  static final int STOPtemplate = 0x0 << 21;
  
  /** stop and signal */
	public final void emitSTOP(int signal) {

    if (VM.VerifyAssertions) VM._assert(fitsU(signal, 14));
	  int mi = STOPtemplate |  (0x3fff & signal) << 0;
	  mIP++;
	  mc.addInstruction(mi);
	}
	
	/** Load link reg with PC */
	public final void emitLoadLR(int RT) {
		_emitBRSL(RT, 1);
	}
	
	/** Read Channel */
  static final int RDCHtemplate = 0xD << 21;
	
	/** Read Channel */
	public final void emitRDCH(int CA, int RT) {
    if (VM.VerifyAssertions) VM._assert(fitsU(CA, 7));
	  int mi = RDCHtemplate |  (0x7f & CA) << 7 |  (0x7f & RT) << 0;
	  mIP++;
	  mc.addInstruction(mi);
	}
	
	/** Read Channel Count */
  static final int RCHCNTtemplate = 0xF << 21;
	
	/** Read Channel Count */
	public final void emitRCHCNT(int CA, int RT) {
    if (VM.VerifyAssertions) VM._assert(fitsU(CA, 7));
	  int mi = RCHCNTtemplate |  (0x7f & CA) << 7 |  (0x7f & RT) << 0;
	  mIP++;
	  mc.addInstruction(mi);
	}

	/** Write Channel */
  static final int WRCHtemplate = 0x10D << 21;
	
	/** Write Channel */
	public final void emitWRCH(int CA, int RT) {
    if (VM.VerifyAssertions) VM._assert(fitsU(CA, 7));
	  int mi = WRCHtemplate |  (0x7f & CA) << 7 |  (0x7f & RT) << 0;
	  mIP++;
	  mc.addInstruction(mi);
	}
	
	/**
	 * Perform a read from main memory to local memory
	 * 
	 * @param mainAddressReg  Register holding main memory address where data should be read
	 * @param lengthReg	      Register holding the length of data to be transferred
	 * @param localAddressReg Register holding the local address where data should be written
	 * @param tagGroup        Tag group to perform right under
	 */
	public final void emitGET(int mainAddressReg, int lengthReg, int localAddressReg, int tagGroup) {
		emitIL(S0, tagGroup);
		emitIL(S1, MFC_GET_OPCODE);
		emitWRCH(MFC_LOCAL_ADDR_CHAN, localAddressReg);
		emitWRCH(MFC_LO_MAIN_ADDR_CHAN, mainAddressReg);
		emitWRCH(MFC_TRANSFER_SIZE_CHAN, lengthReg);
		emitWRCH(MFC_TAG_ID_CHAN, S0);
		emitWRCH(MFC_CMD_CHAN, S1);
	}
	
	/**
	 * Perform a write to main memory from local memory
	 * 
	 * @param mainAddressReg  Register holding main memory address where data should be written
	 * @param lengthReg	      Register holding the length of data to be transferred
	 * @param localAddressReg Register holding the local address where data should be read
	 * @param tagGroup        Tag group to perform right under
	 */
	public final void emitPUT(int mainAddressReg, int lengthReg, int localAddressReg, int tagGroup) {
		emitIL(S0, tagGroup);
		emitIL(S1, MFC_PUT_OPCODE);
		emitWRCH(MFC_LOCAL_ADDR_CHAN, localAddressReg);
		emitWRCH(MFC_LO_MAIN_ADDR_CHAN, mainAddressReg);
		emitWRCH(MFC_TRANSFER_SIZE_CHAN, lengthReg);
		emitWRCH(MFC_TAG_ID_CHAN, S0);
		emitWRCH(MFC_CMD_CHAN, S1);
	}

	/**
	 * Perform a read from main memory to local memory
	 * 
	 * @param mainAddressReg  Register holding main memory address where data should be read
	 * @param lengthReg	      Register holding the length of data to be transferred
	 * @param localAddressReg Register holding the local address where data should be written
	 * @param tagGroup        Tag group to perform right under
	 */
	public final void emitGETL(int listAddrReg, int listSizeReg, int localAddrStartReg, int tagGroup) {
		emitIL(S0, tagGroup);
		emitIL(S1, MFC_GETL_OPCODE);
		emitWRCH(MFC_LOCAL_ADDR_CHAN, localAddrStartReg);
		emitWRCH(MFC_LIST_ADDR_CHAN, listAddrReg);
		emitWRCH(MFC_LIST_SIZE_CHAN, listSizeReg);
		emitWRCH(MFC_TAG_ID_CHAN, S0);
		emitWRCH(MFC_CMD_CHAN, S1);
	}
	
	/**
	 * Perform a getllar MFC atomic command
	 * 
	 * @param mainAddressReg  Register holding main memory address where data should be read
	 * @param localAddressReg Register holding the local address where data should be written
	 */
	public final void emitGETLLAR(int mainAddressReg, int localAddrStartReg) {
		emitIL(S1, MFC_GETLLAR_OPCODE);
		emitWRCH(MFC_LOCAL_ADDR_CHAN, localAddrStartReg);
		emitWRCH(MFC_LIST_ADDR_CHAN, mainAddressReg);
		emitWRCH(MFC_CMD_CHAN, S1);
	}
	
	/**
	 * Perform a putllc MFC atomic command
	 * 
	 * @param mainAddressReg  Register holding main memory address where data should be read
	 * @param localAddressReg Register holding the local address where data should be written
	 */
	public final void emitPUTLLC(int mainAddressReg, int localAddrStartReg) {
		emitIL(S1, MFC_PUTLLC_OPCODE);
		emitWRCH(MFC_LOCAL_ADDR_CHAN, localAddrStartReg);
		emitWRCH(MFC_LIST_ADDR_CHAN, mainAddressReg);
		emitWRCH(MFC_CMD_CHAN, S1);
	}
	
	/** 
	 * Wait for the previous atomic instruction to complete
	 */
	public final void emitATOMIC_WAIT(int statReg) {
		emitRDCH(MFC_READ_ATOMIC_STAT, statReg);
		// TODO - check result
	}
	
	/**
	 * Blocks execution until the MFC updates to the specified tag group are complete
	 * Note: Tag group should be in S0
	 */
	public final void emitBlockUntilComplete() {
		emitIL(S1, 0);
		emitWRCH(MFC_WRITE_TAG_UPDATE, S1);
		int readChnCntLoop = mIP;
		emitRCHCNT(MFC_WRITE_TAG_UPDATE, S2);
		emitBRZ(S2, readChnCntLoop);
		emitRDCH(MFC_READ_TAG_STATUS, S2);
		emitWRCH(MFC_WRITE_TAG_MASK, S0);

		// TODO - don't poll, do something else instead
		int blockLoop = mIP;
		emitWRCH(MFC_WRITE_TAG_UPDATE, S1);
		emitRDCH(MFC_READ_TAG_STATUS, S2);
		emitBRZ(S2, blockLoop);
	}
	
	
	/**
	 * Blocks execution until the MFC updates to the specified tag group are complete
	 * 
	 * @param tagGroup tag group of requests to block on
	 */
	public final void emitBlockUntilComplete(int tagGroup) {
		emitIL(S0, (0x1<<tagGroup));
		emitBlockUntilComplete();
	}
	
  // -----------------------------------------------------------//
  // The following section contains assembler "macros" used by: //
  //    VM_Compiler                                             //
  //    VM_Barriers                                             //
  // -----------------------------------------------------------//

	/** 
	 * Destroys S2 and possibly S6
	 */
	public void emitTRAP(int source, boolean cc, int trapCode) {
		// TODO: Check if trap returns here 
	  // load return value to trap return register
	  //  _emitBRSL(S6, 4); // Note, this does not flush pipeline as it branches to next instruction
	  emitIL(S2, trapCode);
	  if (cc) {
	  	emitBINZ(source, TRAP_ENTRY_REG);   // Traps to interrupt handler
	  } else {
	  	emitBIZ(source, TRAP_ENTRY_REG);   // Traps to interrupt handler
	  }
	}
	
	public void emitFakeTrap(int source) {
  	emitIL(source, FAKE_TRAP_MESSAGE);
  	emitWRCH(SPU_WR_OUT_INTR_MBOX, source);
	}
	
	public void emitFakeTrapStr(int source, int bytesAddr) {
  	emitIL(source, FAKE_TRAP_MESSAGE_STR);
  	emitWRCH(SPU_WR_OUT_MBOX, bytesAddr);
  	emitWRCH(SPU_WR_OUT_INTR_MBOX, source);
  	// wait for ACK
  	emitRDCH(SPU_RD_IN_MBOX, source);
	}

	public void emitFakeTrapInt(int source, int intReg) {
  	emitIL(source, FAKE_TRAP_MESSAGE_INT);
  	emitWRCH(SPU_WR_OUT_MBOX, intReg);
  	emitWRCH(SPU_WR_OUT_INTR_MBOX, source);
  	// wait for ACK
  	emitRDCH(SPU_RD_IN_MBOX, source);
	}
	
  // Emit baseline stack overflow instruction sequence.
  // Before:   FP is current (calling) frame
  //           PR is the current VM_Processor, which contains a pointer to the active thread.
  // After:    R0, S2, S3 destroyed
  //
	
  public void emitStackOverflowCheck(int frameSize) {
	  //emitLoad(0, VM_RegisterConstants.PROCESSOR_REGISTER,
		//	          	  VM_Entrypoints.activeThreadStackLimitField.getSubArchOffset());
	  // TODO - Make this dynamic
  	emitILW(0, STACK_END);
  	emitAI(S2, FP, -frameSize);                        // S2 := &new frame
	  emitCLGT(S3, 0, S2);                              // Trap if new frame below guard page
	  emitTRAP(S3, true, VM_TrapConstants.TRAP_STACK_OVERFLOW);
  }
  

  /**
   * emit a trap on null check.
   * @param RA The register number containing the ptr to null check
   */
  public void emitNullCheck(int RA) {
  	emitTRAP(RA, false, VM_TrapConstants.TRAP_NULL_POINTER);
  }


  public static int getTargetOffset(int instr) {
    // TODO - Check for opcodes which can be retargeted
  	// FIXME - retarget 4 reg instructions (e.g. selb etc.)
  	return 0;
  }

  public boolean retargetInstruction(int mcIndex, int newRegister) {
  	
    int instr = mc.getInstruction(mcIndex);
    int offset = getTargetOffset(instr);
    if (offset < 0) {
      VM.sysWrite("Failed to retarget index=");
      VM.sysWrite(mcIndex);
      VM.sysWrite(", instr=");
      VM.sysWriteHex(instr);
      VM.sysWriteln();
      if (VM.VerifyAssertions) VM._assert(NOT_REACHED);
      return false;
    }

    instr = (instr & ~(0x3F << offset)) | (newRegister << offset);

    mc.putInstruction(mcIndex, instr);
    return true;
  }

  // Convert generated machine code into final form.
  //
  public SubordinateArchitecture.VM_MachineCode finalizeMachineCode(int[] bytecodeMap) {
    mc.setBytecodeMap(bytecodeMap);
    
    return makeMachineCode();
  }

  public SubordinateArchitecture.VM_MachineCode makeMachineCode() {
    mc.finish();
    if (shouldPrint) {
      VM.sysWriteln();
      SubordinateArchitecture.VM_CodeArray instructions = mc.getInstructions();
      boolean saved = VM_BaselineCompiler.options.PRINT_MACHINECODE;
      try {
        VM_BaselineCompiler.options.PRINT_MACHINECODE = false;
        for (int i = 0; i < instructions.length(); i++) {
          VM.sysWrite(VM_Services.getHexString(i << LG_INSTRUCTION_WIDTH, true));
          VM.sysWrite(" : ");
          VM.sysWrite(VM_Services.getHexString(instructions.get(i), false));
          VM.sysWrite("  ");
          VM.sysWrite(VM_Disassembler.disasm(instructions.get(i), i << LG_INSTRUCTION_WIDTH));
          VM.sysWrite("\n");
        }
      } finally {
        VM_BaselineCompiler.options.PRINT_MACHINECODE = saved;
      }
    }
    return (SubordinateArchitecture.VM_MachineCode) mc;
  }
  
  public static String getOpcodeName(int opcode) {
    String s;
    switch (opcode) {
    	// TODO - Generate correct opcode names
      default:
        s = "UNKNOWN";
        break;
    }
    return s;
  }

  /************************************************************************
   * Stub/s added for IA32 compatability
   */
  public static void patchCode(SubordinateArchitecture.VM_CodeArray code, int indexa, int indexb) {
    if (VM.VerifyAssertions) VM._assert(VM.NOT_REACHED);
  }
  
  
  /************************************************************************
   * Test assembler
   */

  private static final int D_FORM 		= 0x1;
  private static final int SHFT_IMMED_4 = 0x2;
  private static final int IMMED_NEG	= 0x4;
  private static final int IMMED_HEX 	= 0x8;
  private static final int SHFT_IMMED_2 = 0x10;
  private static final int IMMED_REL 	= 0x20;
  private static final int DONT_CHECK_ARG = 0x40;
  private static final int FROM_155		= 0x80;
  private static final int FROM_173		= 0x100;
  private static final int CHAN_READ_FORM 		= 0x200;
  private static final int CHAN_WRITE_FORM 		= 0x400;
  
  private static class Instruction {
	  
	  private static int inst_count = 0;
	 
	  public String name;
	  public int [] arg_ranges;
	  public int [] arg_vals;
	  public Object [] arg_vals_o;
	  public String [] arg_results;
	  public boolean [] arg_is_reg;
	  public Class[] arg_types;
	  private int inst_numb;
	  private boolean dont_check_arg = false;
	  
	  public String argsErrors = "";
	  public String methodPrefix = "";
	  
	  private static Random rand;
	  
	  public Instruction (String name, int [] arg_ranges, boolean [] arg_is_reg) {
	  	if (rand == null) {
	  		rand = new Random();
	  	}
	  	
		  this.name = name;
		  this.arg_ranges = arg_ranges;
		  this.arg_is_reg = arg_is_reg;
		  this.inst_numb = inst_count;
		  inst_count++;
		  
		  this.arg_vals = new int [arg_ranges.length];
		  this.arg_vals_o = new Object[arg_ranges.length];
		  this.arg_results = new String[arg_ranges.length];
		  
		  for (int i=0; i<arg_vals.length; i++) {
			  this.arg_vals[i] = rand.nextInt(1 << this.arg_ranges[i]);
			  this.arg_vals_o[i] = new Integer(this.arg_vals[i]);
			  if (this.arg_is_reg[i]) {
				  this.arg_results[i] = "$" + this.arg_vals[i];
			  } else {
				  this.arg_results[i] = "" + this.arg_vals[i];
			  }
		  }
		  
		  this.arg_types = new Class[arg_ranges.length];
		  for (int i=0; i<this.arg_types.length; i++) {
			  this.arg_types[i] = int.class;
		  }
	  }

	  public Instruction (String name, int [] arg_ranges, boolean [] arg_is_reg, int options) {
		  this(name, arg_ranges, arg_is_reg);

		  if ((options & IMMED_NEG) != 0) {
			  int last_val = this.arg_vals.length - 1;
			  if ((this.arg_vals[last_val] & (1 << (this.arg_ranges[last_val] - 1))) != 0) {
				  // sign extend value
				  this.arg_vals[last_val] = this.arg_vals[last_val] | 0xffffffff & ~((1 << this.arg_ranges[last_val]) - 1);

				  this.arg_vals_o[last_val] = new Integer(arg_vals[last_val]);
				  this.arg_results[last_val] = (arg_is_reg[last_val] ? "$" : "") + (this.arg_vals[last_val]);
			  }
		  } 
		  if ((options & SHFT_IMMED_4) != 0) {
			  int last_val = this.arg_vals.length - 1;
			  this.arg_results[last_val] = (arg_is_reg[last_val] ? "$" : "") + (this.arg_vals[last_val] << 4);
		  } 
		  if ((options & SHFT_IMMED_2) != 0) {
			  int last_val = this.arg_vals.length - 1;
			  this.arg_results[last_val] = (arg_is_reg[last_val] ? "$" : "") + (this.arg_vals[last_val] << 2);
		  } 
		  if ((options & FROM_155) != 0) {
			  int last_val = this.arg_vals.length - 1;
			  while (this.arg_vals[last_val] > 155 || this.arg_vals[last_val] < 28) this.arg_vals[last_val] = rand.nextInt(1 << 8);
			  this.arg_results[last_val] = (arg_is_reg[last_val] ? "$" : "") + (155 - (Integer.parseInt(this.arg_results[last_val])));
		  }
		  if ((options & FROM_173) != 0) {
			  int last_val = this.arg_vals.length - 1;
			  while (this.arg_vals[last_val] > 173 || this.arg_vals[last_val] < 46) this.arg_vals[last_val] = rand.nextInt(1 << 8);
			  this.arg_results[last_val] = (arg_is_reg[last_val] ? "$" : "") + (173 - (Integer.parseInt(this.arg_results[last_val])));
		  }
		  if ((options & IMMED_REL) != 0) {
			  int last_val = this.arg_vals.length - 1;
			  this.arg_results[last_val] = (arg_is_reg[last_val] ? "$" : "") + ((Integer.parseInt(this.arg_results[last_val])) + (4 * this.inst_numb));
		  } 
		  if ((options & IMMED_HEX) != 0) {
			  int last_val = this.arg_vals.length - 1;
			  int range = this.arg_ranges[last_val];
			  if ((options & SHFT_IMMED_4) != 0) range += 4;
			  if ((options & SHFT_IMMED_2) != 0) range += 2;
			  int prev_val = Integer.parseInt(this.arg_results[last_val]) & ((1 << range) - 1);
			  this.arg_results[last_val] = (arg_is_reg[last_val] ? "$" : "") + (Integer.toHexString(prev_val));
		  } 
		  // do any swaps at the end 
		  if ((options & D_FORM) != 0) {
			  // swap last two values for result
			  int last_val = this.arg_results.length - 1;
			  String tmp = this.arg_results[last_val];
			  this.arg_results[last_val] =  this.arg_results[last_val - 1];
			  this.arg_results[last_val - 1] = tmp; 
		  }
		  if ((options & CHAN_READ_FORM) != 0) {
			  // swap last two values for result, and make second one a channel
			  int last_val = this.arg_results.length - 1;
			  String tmp = this.arg_results[last_val];
			  this.arg_results[last_val] =  "$ch" + this.arg_results[last_val - 1];
			  this.arg_results[last_val - 1] = tmp; 
		  }
		  if ((options & CHAN_WRITE_FORM) != 0) {
			  // make first result a channel
			  int last_val = this.arg_results.length - 1;
			  this.arg_results[last_val - 1] =  "$ch" + this.arg_results[last_val - 1];
		  }
		  // do any swaps at the end 
		  if ((options & DONT_CHECK_ARG) != 0) {
			  this.dont_check_arg = true;
		  }
	  }
	  
	  public Instruction (String name, int [] arg_ranges, boolean [] arg_is_reg, String prefixStr) {
		  this(name, arg_ranges, arg_is_reg);
		  this.methodPrefix = prefixStr;
	  }

	  public Instruction (String name, int [] arg_ranges, boolean [] arg_is_reg, String prefixStr, int options) {
		  this(name, arg_ranges, arg_is_reg, options);
		  this.methodPrefix = prefixStr;
	  }
	  public boolean checkArgs(String[] args) {
	  	if (this.dont_check_arg) return true;
	  	if (args.length < arg_results.length) return false;

	  	boolean success = true;
	  	for(int i=0; i<arg_results.length; i++) {
	  		if (!arg_results[i].equals(args[i])) {
					success = false;
					argsErrors = argsErrors + "  : Arg " + (i + 1) + " should have been " + arg_results[i] + " but was instead " + args[i] + "\n";
				}
			}
			return success;
	  }
  }
  
  public static void main (String args[]) { 
	  System.out.println("Testing assembly of instructions");
	  
	  VM_Assembler asm = new org.jikesrvm.SubordinateArchitecture.VM_Assembler(1024);

	  int [] none_r 		= {};
	  boolean [] none_b 	= {};
	  
	  int [] reg1_r 		= {7};
	  boolean [] reg1_b 	= {true};
	  
	  int [] reg2_r 		= {7, 7};
	  boolean [] reg2_b 	= {true, true};
	  boolean [] chan_b 	= {false, true};
	  
	  int [] reg3_r 		= {7, 7, 7};
	  boolean [] reg3_b 	= {true, true, true};
	  
	  int [] reg4_r 		= {7, 7, 7, 7};
	  boolean [] reg4_b 	= {true, true, true, true};

	  int [] immed16_r 		= {16};
	  boolean [] immed16_b 	= {false};
	  
	  int [] immed14_r 		= {14};
	  boolean [] immed14_b 	= {false};
	  
	  int [] reg_immed16_r 		= {7, 16};
	  boolean [] reg_immed16_b 	= {true, false};
	  
	  int [] reg_immed18_r 		= {7, 18};
	  boolean [] reg_immed18_b 	= {true, false};
	  
	  int [] reg2_immed10_r 	= {7, 7, 10};
	  boolean [] reg2_immed10_b = {true, true, false};

	  int [] reg2_immed8_r     	= {7, 7, 8};
	  boolean [] reg2_immed8_b  = {true, true, false};
	  
	  int [] reg2_immed7_r     	= {7, 7, 7};
	  boolean [] reg2_immed7_b  = {true, true, false};
	  	
	  Instruction [] instructions = { 
			  new Instruction("LQD",  reg2_immed10_r, reg2_immed10_b, D_FORM | SHFT_IMMED_4 | IMMED_NEG),
			  new Instruction("LQX",  reg3_r, reg3_b),
			  new Instruction("LQA",  reg_immed16_r, reg_immed16_b, IMMED_NEG | IMMED_HEX | SHFT_IMMED_2),
			  new Instruction("LQR",  reg_immed16_r, reg_immed16_b, IMMED_NEG | IMMED_HEX | SHFT_IMMED_2 | IMMED_REL),
			  new Instruction("STQD", reg2_immed10_r, reg2_immed10_b, D_FORM | SHFT_IMMED_4 | IMMED_NEG),
			  new Instruction("STQX", reg3_r, reg3_b),
			  new Instruction("STQA", reg_immed16_r, reg_immed16_b, IMMED_NEG | IMMED_HEX | SHFT_IMMED_2),
			  new Instruction("STQR", reg_immed16_r, reg_immed16_b, IMMED_NEG | IMMED_HEX | SHFT_IMMED_2 | IMMED_REL),
			  new Instruction("CBD",  reg2_immed7_r, reg2_immed7_b, D_FORM | IMMED_NEG),
			  new Instruction("CBX",  reg3_r, reg3_b),
			  new Instruction("CHD",  reg2_immed7_r, reg2_immed7_b, D_FORM | IMMED_NEG),
			  new Instruction("CHX",  reg3_r, reg3_b),
			  new Instruction("CWD",  reg2_immed7_r, reg2_immed7_b, D_FORM | IMMED_NEG),
			  new Instruction("CWX",  reg3_r, reg3_b),
			  new Instruction("CDD",  reg2_immed7_r, reg2_immed7_b, D_FORM | IMMED_NEG),
			  new Instruction("CDX",  reg3_r, reg3_b),
			  new Instruction("ILH",  reg_immed16_r, reg_immed16_b),
			  new Instruction("ILHU", reg_immed16_r, reg_immed16_b),
			  new Instruction("IL",   reg_immed16_r, reg_immed16_b, IMMED_NEG),
			  new Instruction("ILA",  reg_immed18_r, reg_immed18_b, IMMED_HEX),
			  new Instruction("IOHL", reg_immed16_r, reg_immed16_b),
			  new Instruction("AH",   reg3_r, reg3_b),
			  new Instruction("AHI",  reg2_immed10_r, reg2_immed10_b, IMMED_NEG),
			  new Instruction("A",    reg3_r, reg3_b),
			  new Instruction("AI",   reg2_immed10_r, reg2_immed10_b, IMMED_NEG),
			  new Instruction("SFH",  reg3_r, reg3_b),
			  new Instruction("SFHI", reg2_immed10_r, reg2_immed10_b, IMMED_NEG),
			  new Instruction("SF",   reg3_r, reg3_b),
			  new Instruction("SFI",  reg2_immed10_r, reg2_immed10_b, IMMED_NEG),
			  new Instruction("MPY",  reg3_r, reg3_b),
			  new Instruction("MPYU", reg3_r, reg3_b),
			  new Instruction("MPYI", reg2_immed10_r, reg2_immed10_b, IMMED_NEG),
			  new Instruction("MPYUI",reg2_immed10_r, reg2_immed10_b, IMMED_NEG),
			  new Instruction("CLZ", reg2_r, reg2_b),
			  new Instruction("FSM", reg2_r, reg2_b),
			  new Instruction("FSMBI", reg_immed16_r, reg_immed16_b),
			  new Instruction("XSBH", reg2_r, reg2_b),
			  new Instruction("XSHW", reg2_r, reg2_b),
			  new Instruction("XSWD", reg2_r, reg2_b),
			  new Instruction("AND",  reg3_r, reg3_b),
			  new Instruction("ANDC", reg3_r, reg3_b),
			  new Instruction("ANDBI",reg2_immed8_r, reg2_immed8_b),
			  new Instruction("ANDHI",reg2_immed10_r, reg2_immed10_b, IMMED_NEG),
			  new Instruction("ANDI", reg2_immed10_r, reg2_immed10_b, IMMED_NEG),
			  new Instruction("OR",   reg3_r, reg3_b),
			  new Instruction("ORC",  reg3_r, reg3_b),
			  new Instruction("ORBI", reg2_immed8_r, reg2_immed8_b),
			  new Instruction("ORHI", reg2_immed10_r, reg2_immed10_b, IMMED_NEG),
			  new Instruction("ORI",  reg2_immed10_r, reg2_immed10_b, IMMED_NEG),
			  new Instruction("XOR",  reg3_r, reg3_b),
			  new Instruction("XORBI",reg2_immed8_r, reg2_immed8_b),
			  new Instruction("XORHI",reg2_immed10_r, reg2_immed10_b, IMMED_NEG),
			  new Instruction("XORI", reg2_immed10_r, reg2_immed10_b, IMMED_NEG),
			  new Instruction("NAND", reg3_r, reg3_b),
			  new Instruction("NOR",  reg3_r, reg3_b),
			  new Instruction("EQV",  reg3_r, reg3_b),
			  new Instruction("SELB", reg4_r, reg4_b),
			  new Instruction("SHUFB",reg4_r, reg4_b),
			  new Instruction("SHLH", reg3_r, reg3_b),
			  new Instruction("SHLHI",reg2_immed7_r, reg2_immed7_b, IMMED_NEG),
			  new Instruction("SHL",  reg3_r, reg3_b),
			  new Instruction("SHLI", reg2_immed7_r, reg2_immed7_b, IMMED_NEG),
			  new Instruction("SHLQBY",  reg3_r, reg3_b),
			  new Instruction("SHLQBYI", reg2_immed7_r, reg2_immed7_b, IMMED_NEG),
			  new Instruction("SHLQBI",  reg3_r, reg3_b),
			  new Instruction("SHLQBYBI",  reg3_r, reg3_b),
			  new Instruction("SHLQBII", reg2_immed7_r, reg2_immed7_b, IMMED_NEG),
			  new Instruction("ROTH", reg3_r, reg3_b),
			  new Instruction("ROTHI",reg2_immed7_r, reg2_immed7_b, IMMED_NEG),
			  new Instruction("ROT",  reg3_r, reg3_b),
			  new Instruction("ROTI", reg2_immed7_r, reg2_immed7_b, IMMED_NEG),
			  new Instruction("ROTM",  reg3_r, reg3_b),
			  new Instruction("ROTMI", reg2_immed7_r, reg2_immed7_b, IMMED_NEG),
			  new Instruction("ROTMA",  reg3_r, reg3_b),
			  new Instruction("ROTMAI", reg2_immed7_r, reg2_immed7_b, IMMED_NEG),
			  new Instruction("ROTQBY",  reg3_r, reg3_b),
			  new Instruction("ROTQBYI", reg2_immed7_r, reg2_immed7_b, IMMED_NEG),
			  new Instruction("ROTQBYBI",  reg3_r, reg3_b),
			  new Instruction("ROTQBI",  reg3_r, reg3_b),
			  new Instruction("ROTQMBY",  reg3_r, reg3_b),
			  new Instruction("ROTQMBYI", reg2_immed7_r, reg2_immed7_b, IMMED_NEG),
			  new Instruction("ROTQMBYBI",  reg3_r, reg3_b),
			  new Instruction("ROTQMBI",  reg3_r, reg3_b),
			  new Instruction("ROTQMBII", reg2_immed7_r, reg2_immed7_b, IMMED_NEG),
			  new Instruction("CEQB", reg3_r, reg3_b),
			  new Instruction("CEQBI",reg2_immed10_r, reg2_immed10_b, IMMED_NEG),
			  new Instruction("CEQH", reg3_r, reg3_b),
			  new Instruction("CEQHI",reg2_immed10_r, reg2_immed10_b, IMMED_NEG),
			  new Instruction("CEQ",  reg3_r, reg3_b),
			  new Instruction("CEQI", reg2_immed10_r, reg2_immed10_b, IMMED_NEG),
			  new Instruction("CGTB", reg3_r, reg3_b),
			  new Instruction("CGTBI",reg2_immed10_r, reg2_immed10_b, IMMED_NEG),
			  new Instruction("CGTH", reg3_r, reg3_b),
			  new Instruction("CGTHI",reg2_immed10_r, reg2_immed10_b, IMMED_NEG),
			  new Instruction("CGT",  reg3_r, reg3_b),
			  new Instruction("CGTI", reg2_immed10_r, reg2_immed10_b, IMMED_NEG),
			  new Instruction("CLGTB",reg3_r, reg3_b),
			  new Instruction("CLGTBI",reg2_immed10_r, reg2_immed10_b, IMMED_NEG),
			  new Instruction("CLGTH",reg3_r, reg3_b),
			  new Instruction("CLGTHI",reg2_immed10_r, reg2_immed10_b, IMMED_NEG),
			  new Instruction("CLGT", reg3_r, reg3_b),
			  new Instruction("CLGTI",reg2_immed10_r, reg2_immed10_b, IMMED_NEG),
			  new Instruction("BR",   immed16_r, immed16_b, "_", IMMED_NEG | IMMED_HEX | SHFT_IMMED_2 | IMMED_REL),
			  new Instruction("BRA",  immed16_r, immed16_b, IMMED_NEG | IMMED_HEX | SHFT_IMMED_2),
			  new Instruction("BRSL", reg_immed16_r, reg_immed16_b, "_",IMMED_NEG | IMMED_HEX | SHFT_IMMED_2 | IMMED_REL),
			  new Instruction("BRASL",reg_immed16_r, reg_immed16_b, IMMED_NEG | IMMED_HEX | SHFT_IMMED_2),
			  new Instruction("BI",   reg1_r, reg1_b),
			  new Instruction("BIE",  reg1_r, reg1_b),
			  new Instruction("BID",  reg1_r, reg1_b),
			  new Instruction("BISL", reg2_r, reg2_b),
			  new Instruction("BISLE",reg2_r, reg2_b),
			  new Instruction("BISLD",reg2_r, reg2_b),
			  new Instruction("BRNZ", reg_immed16_r, reg_immed16_b, "_", IMMED_NEG | IMMED_HEX | SHFT_IMMED_2 | IMMED_REL),
			  new Instruction("BRZ",  reg_immed16_r, reg_immed16_b, "_", IMMED_NEG | IMMED_HEX | SHFT_IMMED_2 | IMMED_REL),
			  new Instruction("BIZ",  reg2_r, reg2_b),
			  new Instruction("BIZE", reg2_r, reg2_b),
			  new Instruction("BIZD", reg2_r, reg2_b),
			  new Instruction("BINZ", reg2_r, reg2_b),
			  new Instruction("BINZE",reg2_r, reg2_b),
			  new Instruction("BINZD",reg2_r, reg2_b),
			  new Instruction("FA",   reg3_r, reg3_b),
			  new Instruction("DFA",  reg3_r, reg3_b),
			  new Instruction("FS",   reg3_r, reg3_b),
			  new Instruction("DFS",  reg3_r, reg3_b),
			  new Instruction("FM",   reg3_r, reg3_b),
			  new Instruction("DFM",  reg3_r, reg3_b),
			  new Instruction("CSFLT",reg2_immed8_r, reg2_immed8_b, FROM_155),
			  new Instruction("CFLTS",reg2_immed8_r, reg2_immed8_b, FROM_173),
			  new Instruction("CUFLT",reg2_immed8_r, reg2_immed8_b, FROM_155),
			  new Instruction("CFLTU",reg2_immed8_r, reg2_immed8_b, FROM_173),
			  new Instruction("FMA",  reg4_r, reg4_b),
			  new Instruction("FMS",  reg4_r, reg4_b),
			  new Instruction("FNMS", reg4_r, reg4_b),
			  new Instruction("FRDS", reg2_r, reg2_b),
			  new Instruction("FI",   reg3_r, reg3_b),
			  new Instruction("FESD", reg2_r, reg2_b),
			  new Instruction("FCEQ", reg3_r, reg3_b),
			  new Instruction("FCGT", reg3_r, reg3_b),
			  new Instruction("DFCEQ", reg3_r, reg3_b),
			  new Instruction("DFCGT", reg3_r, reg3_b),
			  new Instruction("FREST",reg2_r, reg2_b),
			  new Instruction("NOP",  none_r, none_b),
			  new Instruction("SYNC", none_r, none_b),
			  new Instruction("SYNCC",none_r, none_b),
			  new Instruction("DSYNC",none_r, none_b),
			  new Instruction("STOP", immed14_r, immed14_b, DONT_CHECK_ARG),
			  new Instruction("RDCH",reg2_r, chan_b, CHAN_READ_FORM),
			  new Instruction("RCHCNT",reg2_r, chan_b, CHAN_READ_FORM),
			  new Instruction("WRCH",reg2_r, chan_b, CHAN_WRITE_FORM)
	  };
	  
	  try {
		  for (int i=0; i<instructions.length; i++) {
			  Instruction inst = instructions[i];
			  Class<VM_Assembler> asmClass = VM_Assembler.class;
			  Method emitMethod = asmClass.getDeclaredMethod(inst.methodPrefix + "emit" + inst.name, inst.arg_types);
		   	  emitMethod.invoke(asm, inst.arg_vals_o);
		  }
	  } catch (Exception e) {
		  System.out.println(e);
		  e.printStackTrace();
	  }
	  
	  asm.dumpInstructions(new File("dump_cell_spu.bin"));
	  asm.finalizeMachineCode(new int[2]);
	  
	  try {
		  String line;
		  boolean startedDump = false;
		  boolean success = true;
		  int i = 0;
	        
		  // wrap assembly into elf format
		  Process ldProc = Runtime.getRuntime().exec("spu-ld -r -b binary -o dump_cell_spu.o dump_cell_spu.bin");
		  ldProc.waitFor();

		  // dissasemble assembly
		  Process objdumpProc = Runtime.getRuntime().exec("/opt/cell/toolchain/bin/spu-objdump -D dump_cell_spu.o");
		  BufferedReader input = new BufferedReader(new InputStreamReader(objdumpProc.getInputStream()));
		  while ((line = input.readLine()) != null) {
			  if (startedDump) {
				  String paddingStr = "                                                          ";
				  System.out.print(instructions[i].name + " : " + paddingStr.substring(instructions[i].name.length(), paddingStr.length()));
				  
				  String [] lineTokens = line.split("\\s+|:|,|\\(|\\)");

				  if (!lineTokens[7].equalsIgnoreCase(instructions[i].name)) {
					  System.out.println("[Fail]");
					  System.out.println("  : Instruction emitted - " + lineTokens[7] + " should have been - " + instructions[i].name);
					  success = false;
				  } else if (!instructions[i].checkArgs(Arrays.copyOfRange(lineTokens, 8, lineTokens.length))) {
					  System.out.println("[Fail]");
					  System.out.println(instructions[i].argsErrors);
					  success = false;
				  } else {
					  System.out.println("[Pass]");
				  }
				  i++;
			  } else {
				  if (line.contains("_binary_dump_cell_spu_bin_start")) {
					  startedDump = true;
				  }
			  }
		  }
		  input.close();
		  
		  // emit null check test
		  System.out.println("\nCreating null check test");
		  asm = new org.jikesrvm.SubordinateArchitecture.VM_Assembler(1024);
		  
		  asm.emitILA(TRAP_ENTRY_REG, 0x1000);
		  asm.emitIL(3, 0x1234);
		  asm.emitNullCheck(3);
		  asm.emitIL(3, 0);
		  asm.emitNullCheck(3);
		  asm.dumpInstructions(new File("null_check.bin"));
		  //mc = asm.finalizeMachineCode(new int[0]);
		  Process objCpyProc = Runtime.getRuntime().exec("spu-objcopy -I binary -O elf32-spu null_check.bin null_check.o");
		  objCpyProc.waitFor();
		  ldProc = Runtime.getRuntime().exec("spu-ld null_check.o -o null_check");
		  ldProc.waitFor();
		  
		  // emit stack overflow check
		  System.out.println("\nCreating stack overflow check test");
		  asm = new org.jikesrvm.SubordinateArchitecture.VM_Assembler(1024);
		  
		  asm.emitILA(TRAP_ENTRY_REG, 		0x1000);
			asm.emitILA(FP, 0x3080);
			asm.emitILA(3, 0x3000);
			
			asm.emitStackOverflowCheck(0x40);
			asm.emitStackOverflowCheck(0x80);
			asm.emitStackOverflowCheck(0x100);
		  asm.dumpInstructions(new File("stack_check.bin"));
		  //mc = asm.finalizeMachineCode(new int[0]);
		  objCpyProc = Runtime.getRuntime().exec("spu-objcopy -I binary -O elf32-spu stack_check.bin stack_check.o");
		  objCpyProc.waitFor();
		  ldProc = Runtime.getRuntime().exec("spu-ld stack_check.o -o stack_check");
		  ldProc.waitFor();
		  
		  
		  if (success) {
			  System.out.println("\nOverall - [Pass]");
		  } else {
			  System.out.println("\nOverall - [Fail]");
		  }
	  } catch (Exception err) {
		  err.printStackTrace();
	  }
	  
  }

	private void dumpInstructions(File file) {
		mc.dumpInstructions(file);
	}
}
