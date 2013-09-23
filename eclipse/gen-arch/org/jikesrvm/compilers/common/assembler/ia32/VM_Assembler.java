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
package org.jikesrvm.compilers.common.assembler.ia32;

import org.jikesrvm.*;
import org.jikesrvm.runtime.VM_Magic;
import org.jikesrvm.compilers.baseline.ia32.VM_Compiler;
import org.jikesrvm.ia32.VM_RegisterConstants;
import org.jikesrvm.compilers.common.assembler.VM_ForwardReference;
import org.jikesrvm.compilers.common.assembler.VM_AbstractAssembler;

import org.vmmagic.pragma.*;
import org.vmmagic.unboxed.*;

/**
 *  <P> This class is the low-level assembler for Intel; it contains
 * functionality for encoding specific instructions into an array of
 * bytes.  It consists of three parts: </P>
 * <UL>
 *  <LI> Some support that handles common operations for generating
 *       any IA32 instruction, such as encoding the operands into the
 *       ModRM and SIB bytes
 *  <LI> Some hand-coded methods that emit instructions with
 *       distinctive formats or with special consistency requirements,
 *       such as FXCH or CMOV
 *  <LI> Machine-generated methods that emit instructions with
 *       relatively standard formats, such as binary accumulation
 *       instructions like ADD and SUB.
 * </UL>
 *  <P> This assembler provides a direct interface to the IA32 ISA: it
 * contains emit methods that generate specific IA32 opcodes, and each
 * emit method specifies the addressing modes and operand sizes that
 * it expects.  Thus, it has an emit method that generates an ADD of a
 * register-displacement operand  and an immediate operand.  It is the
 * job of the client to determine the addressing modes, operand
 * sizes and exact opcodes that it desires. </P>
 *
 *  <P> This assembler does provide support for forward branches.  It
 * is permitted to specify a branch operand as an arbitrary label, and
 * later to inform the assembler to which instruction this label
 * refers.  The assembler, when informed to what the label refers,
 * will go back and generate the appropriate offsets for all branches
 * that refer to the given label. The mechanism is implemented by the
 * following methods and helper classes:
 * <UL>
 * <LI> {@link #forwardRefs}
 * <LI> {@link #resolveForwardReferences}
 * <LI> {@link #patchUnconditionalBranch}
 * <LI> {@link #patchConditionalBranch}
 * <LI> {@link #emitJCC_Cond_Label}
 * <LI> {@link #emitJMP_Label}
 * <LI> {@link #emitCALL_Label}
 * <LI> {@link VM_ForwardReference}
 * </UL>
 * </P>
 *
 *  <P> There is also support for generating tableswitches.  This
 * consists providing support for storing tables of relative addresses
 * that can be used to compute the target of a tableswitch.  This
 * support assumes a particular code sequence for tableswitches
 * (followed by baseline and optimizing compilers).  See {@link
 * #emitOFFSET_Imm_ImmOrLabel} for details. </P>
 *
 *  <P> The automatically-generated emit methods of this assembler
 * exploit regularities in the IA32 binary encoding; for example,
 * several instructions (ADD, ADC, SUB, AND, OR, XOR) can be
 * classified as binary accumulators that share a common set of
 * permitted addressing modes and binary encoding all the way down a
 * special case for enconding operands EAX and an immediate.  A shell
 * script (genAssembler.sh in the intel assembler source directory)
 * explots this by specifying a generic way of emtting a binary
 * accumulation and then calling it for each specific opcode that fits
 * that format.  These generated methods are combined with the
 * hand-coded ones (from VM_Assembler.in, also in the assembler
 * source directory) as part of the Jikes RVM build process. </P>
 *
 *  <P> This assembler is shared by the baseline and optimizing
 * compilers: it used directly by the baseline compiler, while the
 * optimizing compiler has an {@link org.jikesrvm.compilers.opt.OPT_Assembler}
 * that is built on top of this one to match
 * {@link org.jikesrvm.compilers.opt.ir.OPT_Instruction}s and
 * {@link org.jikesrvm.compilers.opt.ir.OPT_Operator}s to the emit methods
 * this assembler provides.  The {@link org.jikesrvm.compilers.opt.OPT_Assembler}
 * is entirely machine-generated, and this
 * requires that the methods for encoding instructions use a stylized
 * naming and signiture convention that is designed to make the method
 * signiture computable from the opcode and the operand types.  The
 * naming convention is as follows:
 *
 * <PRE>
 *   final void emit<EM>opcode</EM>\(_<EM>operand code</EM>\)*[_<EM>size</EM>](\(<EM>operand arguments</EM>\)*)
 * </PRE>
 *
 * where the following substitutions are made:
 * <DL>
 * <DT> <EM>opcode</EM>
 * <DI> is the opcode of the instruction (e.g. ADC, MOV, etc)
 *
 * <DT> <EM>operand code</EM>
 * <DI> represents the type of the nth operand:
 *   <DL>
 *   <DT> "Imm"     <DI> immediate operands
 *   <DT> "Reg"     <DI> register operands
 *   <DT> "RegInd"  <DI> register indirect operands
 *   <DT> "RegDisp" <DI> register displacement
 *   <DT> "RegOff"  <DI> shifted index + displacement
 *   <DT> "RegIdx"  <DI> register base + shifted index + displacement
 *   <DT> "Cond"    <DI> condition codes
 *   </DL>
 *
 * <DT> <EM>size</EM>
 * <DI> indicates non-word-sized operations
 *   <DL>
 *   <DT> "Byte"    <DI> bytes
 *   <DT> "Word"    <DI> Intel "words" (i.e. 16 bites)
 *   <DT> "Quad"    <DI> quad words (i.e. double-precision floating point)
 *   </DL>
 *
 * <DT> <EM>operand arguments</EM>
 * <DI> are the needed components of the operands, in order
 *  <DL>
 *  <DT> "Imm"
 *  <DI>
 *    <UL>
 *     <LI> immediate value (int)
 *    </UL>
 *  <DT> "Reg"
 *  <DI>
 *    <UL>
 *     <LI> register number (byte)
 *    </UL>
 *  <DT> "RegInd"
 *  <DI>
 *    <UL>
 *     <LI> register number (byte)
 *    </UL>
 *  <DT> "RegDisp"
 *  <DI>
 *    <UL>
 *     <LI> register number (byte)
 *     <LI> displacement (int)
 *    </UL>
 *  <DT> "RegOff"
 *  <DI>
 *    <UL>
 *     <LI> index register (byte)
 *     <LI> scale (short)
 *     <LI> displacement (int)
 *    </UL>
 *  <DT> "RegIdx"
 *  <DI>
 *    <UL>
 *     <LI> base register (byte)
 *     <LI> index register (byte)
 *     <LI> scale (short)
 *     <LI> displacement (int)
 *    </UL>
 *  <DT> "Cond"
 *  <DI>
 *    <UL>
 *     <LI> condition code mask (byte)
 *    </UL>
 *  </DL>
 * </DL>
 *
 * @see org.jikesrvm.compilers.opt.OPT_Assembler
 * @see VM_Lister
 * @see VM_ForwardReference
 *
*/
public abstract class VM_Assembler extends VM_AbstractAssembler implements VM_RegisterConstants, VM_AssemblerConstants {

  /**
   * The lister object is used to print generated machine code.
   */
  protected final VM_Lister lister;

  /**
   * The array holding the generated binary code.
   */
  private byte [] machineCodes;

  /**
   * The current end of the generated machine code
   */
  protected int         mi;

  /**
   * Create an assembler with a given machine code buffer size that
   * will not print the machine code as it generates it.
   * The buffer size is merely a heuristic, because the assembler will
   * expand its buffer if it becomes full.
   *
   * @param bytecodeSize initial machine code buffer size.
   */
  protected VM_Assembler (int bytecodeSize) {
    this(bytecodeSize, false);
  }

  /**
   * Create an assembler with a given machine code buffer size and
   * tell it whether or not to print machine code as it generates it.
   * The buffer size is merely a heuristic, because the assembler will
   * expand its buffer if it becomes full.
   *
   * @param bytecodeSize initial machine code buffer size.
   * @param shouldPrint whether to dump generated machine code.
   */
  protected VM_Assembler (int bytecodeSize, boolean shouldPrint) {
    machineCodes = new byte[bytecodeSize*CODE_EXPANSION_FACTOR + CODE_OVERHEAD_TERM];
    lister = shouldPrint ? new VM_Lister((ArchitectureSpecific.VM_Assembler) this) : null;
  }

  /**
   * Create an assembler with a given machine code buffer size and
   * tell it whether or not to print machine code as it generates it.
   * The buffer size is merely a heuristic, because the assembler will
   * expand its buffer if it becomes full.
   *
   * @param bytecodeSize initial machine code buffer size.
   * @param shouldPrint whether to dump generated machine code.
   * @param comp VM_Compiler instance that this assembler is associated with;
   *           currently ignored on IA32.
   */
  protected VM_Assembler (int bytecodeSize, boolean shouldPrint, VM_Compiler comp) {
    this(bytecodeSize, shouldPrint);
  }

  /**
   * Heuristic constant used to calculate initial size of the machine
   * code buffer.  This is an average of how many bytes of generated
   * code come from a given bytecode in the baseline compiler.
   */
  private static final int CODE_EXPANSION_FACTOR =  12;

  /**
   * Heuristic constant used to calculate initial size of the machine
   * code buffer.  This is an estimate of the fixed method overhead
   * code generated by the baseline compiler, such as method
   * prologue.
   */
  private static final int CODE_OVERHEAD_TERM    = 100;

  /**
   * Return the current offset in the generated code buffer of the
   * end of the genertaed machine code.
   *
   * @return the end of the generated machine code.
   */
  public final int getMachineCodeIndex () {
    return mi;
  }

  /**
   * Set the given byte offset in the machine code array to the
   * given byte value.  This is the low-level function by which the
   * assembler produces binary code into its machine code buffer.
   * This function will resize the underlying machine code array if
   * the index given exceeds the array's length.
   *
   * @param index the byte offset into which to write
   * @param data the byte data value to write
   */
  @NoNullCheck
  @NoBoundsCheck
  protected final void setMachineCodes(int index, byte data) {
    if(index < machineCodes.length) {
      machineCodes[index] = data;
    } else {
      growMachineCodes(index, data);
    }
  }

  @NoInline
  private void growMachineCodes(int index, byte data) {
    byte [] old = machineCodes;
    machineCodes = new byte [2 * old.length ];
    System.arraycopy(old, 0, machineCodes, 0, old.length);
    machineCodes[index] = data;
  }


  /**
   * Create a VM_MachineCode object
   */
  public final VM_MachineCode finalizeMachineCode(int[] bytecodeMap) {
    return new ArchitectureSpecific.VM_MachineCode(getMachineCodes(), bytecodeMap);
  }

  /**
   * Should code created by this assembler instance be allocated in the
   * hot code code space? By default the answer is false (ie, no).
   */
  protected boolean isHotCode() { return false; }

  /**
   * Return a copy of the generated code as a VM_CodeArray.
   * @return a copy of the generated code as a VM_CodeArray.
   */
  public final ArchitectureSpecific.VM_CodeArray getMachineCodes () {
    int len = getMachineCodeIndex();
    ArchitectureSpecific.VM_CodeArray trimmed = ArchitectureSpecific.VM_CodeArray.Factory.create(len, isHotCode());
    for (int i=0; i<len; i++) {
      trimmed.set(i, machineCodes[i]);
    }
    return trimmed;
  }

  /**
   * Give the lister a message associated with a particular
   * bytecode.  This is used by the baseline assembler to print the
   * bytecode associated with portions of machine code.  (The
   * optimizing compiler does not do this because its association
   * between bytecodes and generated code is much less direct).
   *
   * @param bytecodeNumber the offset of the current bytecode in the
   *     current method's bytecode array.
   * @param bc a message descriptive of the current bytecode.
   */
  public final void noteBytecode (int bytecodeNumber, String bc) {
    if (lister != null) lister.noteBytecode(bytecodeNumber, bc);
  }

  @NoInline
  public final void noteBytecode (int i, String bcode, int x) {
    noteBytecode(i, bcode+" "+x);
  }

  @NoInline
  public final void noteBytecode (int i, String bcode, long x) {
    noteBytecode(i, bcode+" "+x);
  }

  @NoInline
  public final void noteBytecode (int i, String bcode, Object o) {
    noteBytecode(i, bcode+" "+o);
  }

  @NoInline
  public final void noteBytecode (int i, String bcode, int x, int y) {
    noteBytecode(i, bcode+" "+x+" "+y);
  }

  @NoInline
  public final void noteBranchBytecode (int i, String bcode, int off,
               int bt) {
    noteBytecode(i, bcode +" "+off+" ["+bt+"] ");
  }

  @NoInline
  public final void noteTableswitchBytecode (int i, int l, int h, int d) {
    noteBytecode(i, "tableswitch [" + l + "--" + h + "] " + d);
  }

  @NoInline
  public final void noteLookupswitchBytecode (int i, int n, int d) {
    noteBytecode(i, "lookupswitch [<" + n + ">]" + d);
  }

  /**
   * Inform the lister of a comment related to the currently
   * generated machine code.
   *
   * @param comment a comment string
   */
  public final void comment (String comment) {
    if (lister != null) lister.comment(mi, comment);
  }

  /**
   * Print the raw bits of the current instruction.  It takes the
   * start of the instruction as a parameter, and prints from that
   * start to the current machine code index.
   *
   * @see #getMachineCodeIndex
   *
   * @param start the starting index of the last instruction.
   */
  public final void writeLastInstruction(int start) {
    for (int j=start; j<mi; j++) {
      if (j < machineCodes.length) {
        VM.sysWrite(VM_Lister.hex(machineCodes[j]));
      } else {
        VM.sysWrite(VM_Lister.hex((byte)0x0));
      }
    }
  }

  /**
   * Find out whether a given signed value can be represented in a
   * given number of bits.
   *
   * @param val the value to be represented
   * @param bits the number of bits to use.
   * @return true if val can be encoded in bits.
   */
  protected static boolean fits (Offset val, int bits) {
    Word o = val.toWord().rsha(bits-1);
    return (o.isZero() || o.isMax());
  }

  /**
   * Find out whether a given signed value can be represented in a
   * given number of bits.
   *
   * @param val the value to be represented
   * @param bits the number of bits to use.
   * @return true if val can be encoded in bits.
   */
  protected static boolean fits (int val, int bits) {
    val = val >> bits-1;
    return (val == 0 || val == -1);
  }

  /**
   * In the representation of addressing modes in the ModRM and SIB
   * bytes, the code for register-displacement for on ESP has a
   * special meaning.  Thus, when register-displacement mode using ESP
   * is desired, this special SIB (scale-index-base) byte must be
   * emitted.
   */
  private static final byte SIBforESP = (byte) ((0<<6) + (4<<3) + ESP); // (scale factor 1) no index, ESP is base

  /**
   * Return a ModRM byte encoding a source and destination register
   * (i.e. for a register-register instruction).
   *
   * @param reg1 the r/m register.
   * @param reg2 the other register or extended opcode.
   * @return the encoded ModRM byte.
   */
  private byte regRegModRM(byte reg1, byte reg2) {
    return (byte) ((3 << 6) | (reg2 << 3) | reg1);
  }

  /**
   * Return a ModRM byte encoding a source register-32-bit-displacement
   * operand and a destination register.  Note that the displacement
   * is handled separately, and not encoded in the ModRM itself.
   *
   * @param reg1 the r/m register.
   * @param reg2 the other register or extended opcode.
   * @return the encoded ModRM byte.
   */
  private byte regDisp32RegModRM(byte reg1, byte reg2) {
    return (byte) ((2 << 6) | (reg2 << 3) | reg1);
  }

  /**
   * Return a ModRM byte encoding a source register-8-bit-displacement
   * operand and a destination register.  Note that the displacement
   * is handled separately, and not encoded in the ModRM itself.
   *
   * @param reg1 the r/m register.
   * @param reg2 the other register or extended opcode.
   * @return the encoded ModRM byte.
   */
  private byte regDisp8RegModRM(byte reg1, byte reg2) {
    return (byte) ((1 << 6) | (reg2 << 3) | reg1);
  }

  /**
   * Return a ModRM byte encoding a source register-indirect
   * operand and a destination register.
   *
   * @param reg1 the r/m register.
   * @param reg2 the other register or extended opcode.
   * @return the encoded ModRM byte.
   */
  private byte regIndirectRegModRM(byte reg1, byte reg2) {
    return (byte) ((reg2 << 3) | reg1);
  }

  /**
   * The more complex IA32 addressing modes require a
   * scale-index-base (SIB) byte.  This is used to encode addressing
   * modes such as [ indexReg \<\< scale + baseReg ].  This method
   * encodes the SIB byte for a given base, index and scale.
   *
   * @param scale the shift amount for the index register value.
   * @param baseReg the base register.
   * @param indexReg the index register.
   * @return the encoded SIB byte.
   */
  private byte sib(short scale, byte baseReg, byte indexReg) {
    return (byte) ((scale << 6) | (indexReg << 3) | baseReg);
  }

  /**
   * Generate the appropriate bytes into the generated machine code
   * to represent a regsiter-regsiter instruction.
   *
   * @param reg1 the r/m operand.
   * @param reg2 the other register or extended opcode.
   */
  private void emitRegRegOperands(byte reg1, byte reg2) {
    setMachineCodes(mi++, regRegModRM(reg1, reg2));
  }

  /**
   * Generate the appropriate bytes into the generated machine code
   * to represent a regsiter-32-bit-displacement--regsiter
   * instruction. This method generates the appropriate ModRM, the SIB
   * if needed for the ESP special case, and the little-endian encoded
   * 32 bit displacement.
   *
   * @see #SIBforESP
   *
   * @param reg1 the r/m operand.
   * @param disp the 32 bit displacement.
   * @param reg2 the other register or extended opcode.
   */
  private void emitRegDisp32RegOperands(byte reg1, int disp, byte reg2) {
    setMachineCodes(mi++, regDisp32RegModRM(reg1, reg2));
    if (reg1 == ESP) setMachineCodes(mi++, SIBforESP);
    emitImm32(disp);
  }

  /**
   * Generate the appropriate bytes into the generated machine code
   * to represent a regsiter-8-bit-displacement--regsiter
   * instruction. This method generates the appropriate ModRM, the SIB
   * if needed for the ESP special case, and the little-endian encoded
   * 32 bit displacement.
   *
   * @see #SIBforESP
   *
   * @param reg1 the r/m operand.
   * @param disp the 8 bit displacement.
   * @param reg2 the other register or extended opcode.
   */
  private void emitRegDisp8RegOperands(byte reg1, byte disp, byte reg2) {
    setMachineCodes(mi++, regDisp8RegModRM(reg1, reg2));
    if (reg1 == ESP) setMachineCodes(mi++, SIBforESP);
    emitImm8(disp);
  }

  /**
   * Generate the appropriate bytes into the generated machine code
   * to represent a regsiter-displacement--regsiter instruction.  This
   * method simply chooses the appropriate lower-level method based on
   * displacement size
   *
   * @see #emitRegDisp32RegOperands
   * @see #emitRegDisp8RegOperands
   *
   * @param reg1 the r/m operand.
   * @param disp the displacement.
   * @param reg2 the other register or extended opcode.
   */
  private void emitRegDispRegOperands(byte reg1, Offset disp, byte reg2) {
    if (fits(disp,8))
      emitRegDisp8RegOperands(reg1, (byte)disp.toInt(), reg2);
    else {
      if (VM.VerifyAssertions) VM._assert(fits(disp,32));
      emitRegDisp32RegOperands(reg1, disp.toInt(), reg2);
    }
  }

  /**
   * Generate the appropriate bytes into the generated machine code
   * to express a register-indirect--register instruction.  This
   * method handles low-level encoding issues, specifically the
   * special cases for register indirect mode on ESP and EBP.  Using
   * ESP requires an SIB byte, and EBP cannot be used in indirect mode
   * at all (that encoding is used to express scaled-index-displacement
   * mode) so this method uses register-displacement with a 0
   * displacement to fake it.
   *
   * @see #emitRegDispRegOperands
   *
   * @param reg1 the r/m operand
   * @param reg2 the other register or extended opcode
   */
  private void emitRegIndirectRegOperands(byte reg1, byte reg2) {
    if (reg1 == EBP) {
      emitRegDispRegOperands(reg1, Offset.zero(), reg2);
    } else {
      setMachineCodes(mi++, regIndirectRegModRM(reg1, reg2));
      if (reg1 == ESP) setMachineCodes(mi++, SIBforESP);
    }
  }

  /**
   * Generate the appropriate bytes into the generated code to denote
   * a scaled-register+displacement--register instruction.  This
   * expresses the case where the SIB byte is used, but no base
   * register is desired.  This method handles the somewhat convoluted
   * special case used to express this mode (both the r/m register and
   * the base register must be 5).
   *
   * @param index the index register for the r/m operand
   * @param scale the amount to shift the index register
   * @param disp the displacement for the r/m operand
   * @param reg2 the other operand or the extended opcode
   */
  private void emitRegOffRegOperands(byte index, short scale, Offset disp, byte reg2) {
    setMachineCodes(mi++, regIndirectRegModRM((byte) 0x4, reg2));
    setMachineCodes(mi++, sib(scale, (byte) 0x5, index));
    emitImm32(disp);
  }

  /**
   * Generate the appropriate bytes into the generated code to denote
   * an absolute-addresss--register instruction.  This
   * expresses the case where the SIB byte is used, but no base and no
   * index register is desired.  This method handles the somewhat convoluted
   * special case used to express this mode (both the r/m register and
   * the base register must both be 5, and the index must 4; scale can be
   * anything, and we choose 0).
   *
   * @param disp the displacement for the r/m operand
   * @param reg2 the other operand or the extended opcode
   */
  private void emitAbsRegOperands(Offset disp, byte reg2) {
    setMachineCodes(mi++, regIndirectRegModRM((byte) 0x5, reg2));
    // setMachineCodes(mi++, sib((byte) 0x0, (byte) 0x5, (byte) 0x4));
    emitImm32(disp);
  }

  /**
   * Generate the full glory of scaled-index-base-displacement
   * addressing to the generated machine code.  This method handles
   * various special cases, mostly choosing the smallest displacement
   * possible.
   *
   * @param base the base register for the r/m operand
   * @param index the index register for the r/m operand
   * @param scale the amount to shift the index register
   * @param disp the displacement for the r/m operand
   * @param reg2 the other operand or the extended opcode
   */
  private void emitSIBRegOperands(byte base, byte index, short scale, Offset disp, byte reg2) {
    if (VM.VerifyAssertions) VM._assert(index != ESP);
    if (disp.EQ(Offset.zero()) && base != EBP) {
      setMachineCodes(mi++, regIndirectRegModRM((byte) 0x4, reg2));
      setMachineCodes(mi++, sib(scale, base, index));
    } else if (fits(disp,8)) {
      setMachineCodes(mi++, regDisp8RegModRM((byte) 0x4, reg2));
      setMachineCodes(mi++, sib(scale, base, index));
      emitImm8((byte)disp.toInt());
    } else {
      setMachineCodes(mi++, regDisp32RegModRM((byte) 0x4, reg2));
      setMachineCodes(mi++, sib(scale, base, index));
      emitImm32(disp);
    }
  }

  /**
   * Generate the smallest-byte-first IA32 encoding of 32 bit
   * immediates into the generated code.
   *
   * @param disp the displacement to generate.
   */
  private void emitImm32(Offset disp) {
    if (VM.VerifyAssertions) VM._assert(fits(disp,32));
    mi = emitImm32(disp.toInt(), mi);
  }

  /**
   * Generate the smallest-byte-first IA32 encoding of 32 bit
   * immediates into the generated code.
   *
   * @param imm the immediate to generate.
   */
  private void emitImm32(int imm) {
    mi = emitImm32(imm, mi);
  }

  /**
   * Generate the smallest-byte-first IA32 encoding of 16 bit
   * immediates into the generated code.
   *
   * @param imm the immediate to generate.
   */
  private void emitImm16(int imm) {
    mi = emitImm16(imm, mi);
  }

  /**
   * Generate the IA32 encoding of an 8 bit immediate into the
   * generated code.
   *
   * @param imm the immediate to generate.
   */
  private void emitImm8(int imm) {
    mi = emitImm8(imm, mi);
  }

  /**
   * Generate the smallest-byte-first IA32 encoding of 16 bit
   * immediates into the generated code at the location specified.
   *
   * @param imm the immediate to generate.
   * @param idx the location in the generated code to write.
   */
  private int emitImm16(int imm, int idx) {
    setMachineCodes(idx++, (byte) ((imm >>  0) & 0xFF));
    setMachineCodes(idx++, (byte) ((imm >>  8) & 0xFF));
    return idx;
  }

  /**
   * Generate the smallest-byte-first IA32 encoding of 32 bit
   * immediates into the generated code at the location specified.
   *
   * @param imm the immediate to generate.
   * @param idx the location in the generated code to write.
   */
  private int emitImm32(int imm, int idx) {
    setMachineCodes(idx++, (byte) ((imm >>  0) & 0xFF));
    setMachineCodes(idx++, (byte) ((imm >>  8) & 0xFF));
    setMachineCodes(idx++, (byte) ((imm >> 16) & 0xFF));
    setMachineCodes(idx++, (byte) ((imm >> 24) & 0xFF));
    return idx;
  }

  /**
   * Generate the IA32 encoding of an 8 bit immediate into the
   * generated code at the location specified.
   *
   * @param imm the immediate to generate.
   * @param idx the location in the generated code to write.
   */
  private int emitImm8(int imm, int idx) {
    setMachineCodes(idx++, (byte) imm);
    return idx;
  }

  /**
   * Generate a conditional opcode given the base opcode and the
   * condition code desired.  The CMOVcc, SETcc and Jcc families of
   * instructions all have opcodes defined as a base opcode plus some
   * bits representing the condition code.  (of course, FCMOV does not
   * this, since that would be too logical).
   *
   * @param opCode the base opcode to emit
   * @param cond the condition code desired
   */
  private void emitCondOpByte(byte opCode, byte cond) {
    setMachineCodes(mi++, (byte) (opCode | cond));
  }

  /**
   * Generate a locking prefix word into the generated code.  Locking
   * operations on IA32 are expressed by writing a locking byte before
   * the instruction.
   */
  public final void emitLockNextInstruction() {
    setMachineCodes(mi++, (byte) 0xF0);
    if (lister != null) lister.lockPrefix();
  }

  /**
   * Generate a branch likely prefix into the generated code.
   */
  public final void emitBranchLikelyNextInstruction() {
    setMachineCodes(mi++, (byte) 0x3E);
    if (lister != null) lister.lockPrefix();
  }

  /**
   * Generate a branch unlikely prefix into the generated code.
   */
  public final void emitBranchUnlikelyNextInstruction() {
    setMachineCodes(mi++, (byte) 0x2E);
    if (lister != null) lister.lockPrefix();
  }

  /**
   * Generate a patch point into the generated code.
   * (1) force patch point to be 16 bit aligned by optionally
   *   generating a nop.
   * (2) emit a short branch (2 byte) around 3 bytes of nops.
   * (3) If the the code is later patched, we first patch the 3
   *   nop bytes to be the upper 24 bits of a long jump
   *   instruction, then update the 2 bytes of the patch
   *   point to be an unconditional jump with a 32 bit immediate.
   */
  public final void emitPatchPoint() {
    if ((mi & 0x1) == 1) {
      emitNOP();
    }
    VM_ForwardReference r = forwardJMP();
    emitNOP();
    emitNOP();
    emitNOP();
    r.resolve(this);
  }

  /**
   * Apply a patch.
   * We expect the following instruction stream:
   *  i1; JMP rel8; NOP; NOP; NOP; i2;
   * where patchOffset is the index of the last NOP.
   * We patch it to be
   *  i1; JMP rel32; i2;
   *
   * @param code    the code array to patch
   * @param patchOffset the offset of the last byte of the patch point
   * @param rel32     the new immediate to use in the branch instruction
   *          the code patcher is going to lay down before patchOffset
   */
  public static void patchCode(ArchitectureSpecific.VM_CodeArray code,
                   int patchOffset,
                   int rel32) {
    byte p0 = (byte)0xE9;
    byte p1 = (byte) (rel32 & 0x000000ff);
    byte p2 = (byte)((rel32 & 0x0000ff00) >>>  8);
    byte p3 = (byte)((rel32 & 0x00ff0000) >>> 16);
    byte p4 = (byte)((rel32 & 0xff000000) >>> 24);
    if ((patchOffset & 0x2) == 0x2) {
      // (a) lay down p4,p3,p2 one byte at a time
      // (b) pick up the two bytes before p0 and then
      //   lay down b2b1p0p1 as a word.
      code.set(patchOffset--, p4);
      code.set(patchOffset--, p3);
      code.set(patchOffset--, p2);
      patchOffset -= 2; // skip over p1, p0
      byte b1 = code.get(patchOffset--);
      byte b2 = code.get(patchOffset);
      int patch = (((int)p1&0xff) << 24) | (((int)p0&0xff) << 16) |
                  (((int)b1&0xff) << 8)  |  ((int)b2&0xff);
      VM_Magic.setIntAtOffset(code, Offset.fromIntSignExtend(patchOffset), patch);
    } else {
      // (a) lay down p4
      // (b) lay down p0p1p2p3 as a word
      code.set(patchOffset--, p4);
      patchOffset -= 3; // skip over p0p1p2p3
      int patch = (((int)p3&0xff) << 24) | (((int)p2&0xff) << 16) |
                  (((int)p1&0xff) << 8)  |  ((int)p0&0xff);
      VM_Magic.setIntAtOffset(code, Offset.fromIntSignExtend(patchOffset), patch);
    }
  }

  /**
   * Return the appropriate condition code if we want to
   * reverse the sense of the branch
   */
  public final byte flipCode(byte cond) {
    switch (cond) {
      case EQ: return NE;
      case NE: return EQ;
      case LT: return GE;
      case GT: return LE;
      case LE: return GT;
      case GE: return LT;
      default: throw new InternalError("Unexpected condition code");
    }
  }

  /**
   * Generate a forward JMP instruction into the generated code.
   * This form is used when the compiler wants to hang onto the
   * forward reference object and call resolve on it directly.  These
   * forward references are not handled by the mechanism in the
   * assembler; the client is responsible for calling resolve on the
   * reference when generating the target instruction.  The baseline
   * compiler uses this form for jumps within the machine code for a
   * single bytecode.
   */
  public final VM_ForwardReference forwardJMP () {
    int miStart = mi;
    VM_ForwardReference r =  new VM_ForwardReference.ShortBranch(mi);
    setMachineCodes(mi++, (byte) 0xEB);
    mi += 1; // leave space for displacement
    if (lister != null) lister.I(miStart, "JMP", 0);
    return r;
  }

  /**
   * Generate a forward Jcc instruction into the generated code.
   * This form is used when the compiler wants to hang onto the
   * forward reference object and call resolve on it directly.  These
   * forward references are not handled by the mechanism in the
   * assembler; the client is responsible for calling resolve on the
   * reference when generating the target instruction.  The baseline
   * compiler uses this form for jumps within the machine code for a
   * single bytecode.
   *
   * @param cond the condition code on which to branch
   */
  public final VM_ForwardReference forwardJcc (byte cond) {
    int miStart = mi;
    VM_ForwardReference r =  new VM_ForwardReference.ShortBranch(mi);
    setMachineCodes(mi++, (byte) (0x70 + cond));
    mi += 1; // leave space for displacement
    if (lister != null) lister.I(miStart, "J" + CONDITION[cond], 0);
    return r;
  }

  /**
   * The set of outstanding forward references.  This list is used by
   * the assembler to keep track of all outstanding forward
   * references.  References are put on this list by the emit methods
   * for JMP and Jcc when they find a branch that is going forward.
   * Each reference must understand what instruction it is looking for
   * and how to patch its source instruction.  Then, when the client
   * calls resolveForwardBranches, the assembler searches this list to
   * find branches that match the instruction currently being
   * generated, and calls the resolve method on each one that does.
   *
   * All forward branches have a label as the branch target; clients
   * can arbirarily associate labels and instructions, but must be
   * consistent in giving the chosen label as the target of branches
   * to an instruction and calling resolveForwardBranches with the
   * given label immediately before emitting the target instruction.
   * See the header comments of VM_ForwardReference for more details.
   */
  protected VM_ForwardReference forwardRefs;

  /**
   * Resolve all forward branches that have the given target, and
   * make them branch to the instruction currently being generated.
   * Clients of the assembler call this method immediately before they
   * emit the instruction intended to be the target of the given
   * label.
   *
   * All forward branches have a label as the branch target; clients
   * can arbirarily associate labels and instructions, but must be
   * consistent in giving the chosen label as the target of branches
   * to an instruction and calling resolveForwardBranches with the
   * given label immediately before emitting the target instruction.
   * See the header comments of VM_ForwardReference for more details.
   *
   * @param label the forward branch label to resolve
   */
  public final void resolveForwardReferences (int label) {
    if (forwardRefs == null) return; // premature optimization
    forwardRefs = VM_ForwardReference.resolveMatching(this, forwardRefs, label);
  }

  /**
   * Set up a code sequence to push a return address. This involves pushing the 
   * current instruction address, and setting up an ADD that gets resolved later.
   * 
   * @param bReturn the return address that is to be loaded.
   */
  public final void generateLoadReturnAddress(int bReturn) {
    /* Push the IP */
    emitCALL_Imm(mi + 5);
    VM_ForwardReference r = new VM_ForwardReference.LoadReturnAddress(mi, bReturn);
    forwardRefs = VM_ForwardReference.enqueue(forwardRefs, r);
    /* Fake MAX_INTEGER to ensure 32 bit immediate */
    emitADD_RegInd_Imm(ESP, Integer.MAX_VALUE);
  }
  
  /**
   * Patch the code sequence at sourceIndex to load the complete instruction address
   * of the current instruction.
   *
   * @param sourceIndex the machine code offset of the load return addres to patch.
   */
  public final void patchLoadReturnAddress(int sourceIndex) {
    /* We have the following pattern:
     * | PUSH EIP | [SP] += | IMM32 |
     *            ^         ^
     *  sourceIndex        +3 */
    int ipDelta = mi - sourceIndex;
    emitImm32(ipDelta, sourceIndex + 3);
  }

  /**
   * Make a forward reference and emit a long JMP
   * @param btarget optional
   * @return a forward reference for patching later
   */
  public final VM_ForwardReference generatePendingJMP(int btarget) {
    int miStart = mi;
    VM_ForwardReference r = new VM_ForwardReference.UnconditionalBranch(mi, btarget);
    setMachineCodes(mi++, (byte) 0xE9);
    mi += 4; // leave space for displacement
    if (lister != null) lister.I(miStart, "JMP", 0);
    return r;
  }
  // END OSR SUPPORT //

  /**
   * Make the given unconditional branch branch to the current
   * generated instruction.  It is the client's responsibility to
   * ensure the given source index really does contain an
   * unconditional branch.
   *
   * @param sourceIndex the machine code offset of the unconditional
   *          branch to patch.
   */
  public final void patchUnconditionalBranch (int sourceIndex) {
    if (lister != null) lister.comefrom(mi, sourceIndex);
    int relOffset = mi - (sourceIndex+5);
    sourceIndex++; // skip the op code
    emitImm32(relOffset, sourceIndex);
  }

  /**
   * Make the given conditional branch branch to the current
   * generated instruction.  It is the client's responsibility to
   * ensure the given source index really does contain an
   * conditional branch.
   *
   * @param sourceIndex the machine code offset of the conditional
   *          branch to patch.
   */
  public final void patchConditionalBranch (int sourceIndex) {
    if (lister != null) lister.comefrom(mi, sourceIndex);
    int relOffset = mi - (sourceIndex+6);
    sourceIndex += 2; // skip the (two byte) op code
    emitImm32(relOffset, sourceIndex);
  }

  /**
   * Make the given unconditional branch branch to the current
   * generated instruction.  It is the client's responsibility to
   * ensure the given source index really does contain an
   * unconditional branch.  This instruction requires that the branch
   * have been generated with an 8 bit offset.
   *
   * @param sourceIndex the machine code offset of the unconditional
   *          branch to patch.
   */
  public final void patchShortBranch (int sourceIndex) {
    if (lister != null) lister.comefrom(mi, sourceIndex);
    int relOffset = mi - (sourceIndex+2);
    if (VM.VerifyAssertions) VM._assert(fits(relOffset, 8));
    sourceIndex++; // skip the op code
    emitImm8((byte)relOffset, sourceIndex);
  }

  /////////////////////////////////////
  // table switch support /////////////
  /////////////////////////////////////

  /**
   * <P> An OFFSET instruction is not really an instruction; it is rather
   * an address (of an instruction) that is written into the binary
   * code.  These are used to build a table of addresses for table
   * switches.  The code for tableswitches first captures the current
   * pc, then jumps to that value plus the value from a table of
   * relative offsets indexed by the value of the switch expression. </P>
   *
   * <P> This mechanism assumes code for emitting tableswitch looks as
   * follows; it is not very nice, but no relocatable IA32 code for such
   * switches is.  Indeed, GCC generates something similar when given
   * -fpic.  Note that default cases must be handled separateky. </P>
   *
   * <PRE>
   *     CALL x ([SP] gets IP of first OFFSET instruction)
   *     OFFSET 0 (case 0 target)
   *     OFFSET 1 (case 1 target)
   *     ...
   *     OFFSET n (case n target)
   *
   *   -------------------------------
   *
   *   x:  [SP] = [SP] + [ [SP] + case expression value<<2 ]
   *     RET (goes to adjusted location in [SP])
   * </PRE>
   *
   * <P> The above mechanism means that the RET instruction is
   * effectively a register-indexed relative jump (the whole reason
   * for this mess is that IA32 has no such instruction).  The offset
   * is based on the case expression, and the jump is relative to the
   * instruction following the CALL.  Thus, table offsets must be
   * relative to the start of the table, not their own addresses
   * within it. </P>
   *
   * <P> Therefore, handling of forward bracnhes is slightly different
   * from the normal case of JMP and Jcc; when a forward branch is
   * found, the current table entry (c) is stored into the table.
   * When the fixup code runs, it uses this index to compute the
   * start of the table, and store the offset of the target
   * instruction relative to that.  Similarly, for backward branches,
   * the case index c is used to find the start of the table to store
   * te appropriate relative offset. </P>
   *
   * @see #patchSwitchCase
   *
   * @param c the table entry being emitted (i.e. the value of the
   * switch expression corresponding to this target)
   * @param mTarget the method-relative target offset
   * @param bTarget the label associated with the branch target instrucion
   */
  public final void emitOFFSET_Imm_ImmOrLabel(int c, int mTarget, int bTarget) {
    int miStart = mi;
    if (0 < mTarget) { // resolved (backward) reference
      int delta = mTarget - (mi - (c<<2));  // delta is from start of table of 4-byte entries (not this entry)
      emitImm32(delta);
      if (lister != null) lister.I(miStart, "DATA", mTarget);
    } else {        // unresolved forward reference
      VM_ForwardReference r =  new VM_ForwardReference.SwitchCase(mi, bTarget);
      forwardRefs = VM_ForwardReference.enqueue(forwardRefs, r);
      emitImm32(c);
      if (lister != null) lister.I(miStart, "DATA", c);
    }
  }

  /**
   * Patch a tableswitch offset table entry at the given source
   * index.  This method resolves the table entry at the given source
   * index to point to the current instruction.
   *
   * @see #emitOFFSET_Imm_ImmOrLabel
   *
   * @param sourceIndex the location of the offset to patch
   */
  public final void patchSwitchCase (int sourceIndex) {
    if (lister != null) lister.comefrom(mi, sourceIndex);
    int c = 0;
    c |= (machineCodes[sourceIndex+0] & 0xFF) <<  0;
    c |= (machineCodes[sourceIndex+1] & 0xFF) <<  8;
    c |= (machineCodes[sourceIndex+2] & 0xFF) << 16;
    c |= (machineCodes[sourceIndex+3] & 0xFF) << 24;  // c = case index
    int delta = mi - (sourceIndex - (c<<2)); // from begining of table of 4-byte entries to here
    setMachineCodes(sourceIndex+0, (byte) ((delta >>  0) & 0xFF));
    setMachineCodes(sourceIndex+1, (byte) ((delta >>  8) & 0xFF));
    setMachineCodes(sourceIndex+2, (byte) ((delta >> 16) & 0xFF));
    setMachineCodes(sourceIndex+3, (byte) ((delta >> 24) & 0xFF));
  }

  /////////////////////////////////////
  // instructions (hand coded)       //
  /////////////////////////////////////

  /**
   * Conditionally move the source to the destination, i.e.
   * <PRE>
   * if (cond) dst = src
   * </PRE>
   */
  public final void emitCMOV_Cond_Reg_Reg(byte cond, byte dst, byte src) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x0F);
    emitCondOpByte((byte)0x40, cond);
    emitRegRegOperands(src, dst);
    if (lister != null) lister.RR(miStart, "CMOV" + CONDITION[cond], dst, src);
  }

  /**
   * Conditionally move the source to the destination, i.e.
   * <PRE>
   * if (cond) dst = [src + disp]
   * </PRE>
   */
  public final void emitCMOV_Cond_Reg_RegDisp(byte cond, byte dst, byte src, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x0F);
    emitCondOpByte((byte)0x40, cond);
    emitRegDispRegOperands(src, disp, dst);
    if (lister != null) lister.RRD(miStart, "CMOV" + CONDITION[cond], dst, src, disp);
  }

  /**
   * Conditionally move the source to the destination, i.e.
   * <PRE>
   * if (cond) dst = [src]
   * </PRE>
   */
  public final void emitCMOV_Cond_Reg_RegInd(byte cond, byte dst, byte src) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x0F);
    emitCondOpByte((byte)0x40, cond);
    emitRegIndirectRegOperands(src, dst);
    if (lister != null) lister.RRN(miStart, "CMOV" + CONDITION[cond], dst, src);
  }

  /**
   * Conditionally move the source to the destination, i.e.
   * <PRE>
   * if (cond) dst = [index2<<scale2 + disp2]
   * </PRE>
   */
  public final void emitCMOV_Cond_Reg_RegOff(byte cond, byte dst, byte index2, short scale2, Offset disp2) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x0F);
    emitCondOpByte((byte)0x40, cond);
    emitRegOffRegOperands(index2, scale2, disp2, dst);
    if (lister != null) lister.RRFD(miStart, "CMOV" + CONDITION[cond], dst, index2, scale2, disp2);
  }

  /**
   * Conditionally move the source to the destination, i.e.
   * <PRE>
   * if (cond) dst = [disp2]
   * </PRE>
   */
  public final void emitCMOV_Cond_Reg_Abs(byte cond, byte dst, Offset disp2) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x0F);
    emitCondOpByte((byte)0x40, cond);
    emitAbsRegOperands(disp2, dst);
    if (lister != null) lister.RRA(miStart, "CMOV" + CONDITION[cond], dst, disp2);
  }

  /**
   * Conditionally move the source to the destination, i.e.
   * <PRE>
   * if (cond) dst = [base2 + index2<<scale2 + disp2]
   * </PRE>
   */
  public final void emitCMOV_Cond_Reg_RegIdx(byte cond, byte dst, byte base2, byte index2, short scale2, Offset disp2) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x0F);
    emitCondOpByte((byte)0x40, cond);
    emitSIBRegOperands(base2, index2, scale2, disp2, dst);
    if (lister != null) lister.RRXD(miStart, "CMOV" + CONDITION[cond], dst, base2, index2, scale2, disp2);
  }

  /**
   * Set destination to zero or one, if the given condition is false
   * or true, respectively.  That is,
   * <PRE>
   * dst = (cond)? 1: 0
   * </PRE>
   *
   * @param cond the condition to be tested
   * @param dst the destination register
   */
  public final void emitSET_Cond_Reg_Byte(byte cond, byte dst) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x0F);
    emitCondOpByte((byte)0x90, cond);
    emitRegRegOperands(dst, (byte) 0x00);
    if (lister != null) lister.R(miStart, "SET" + CONDITION[cond], dst);
  }

  public final void emitSET_Cond_RegDisp_Byte(byte cond, byte dst, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x0F);
    emitCondOpByte((byte)0x90, cond);
    emitRegDispRegOperands(dst, disp, (byte) 0x00);
    if (lister != null) lister.RD(miStart, "SET" + CONDITION[cond], dst, disp);
  }

  public final void emitSET_Cond_RegInd_Byte(byte cond, byte dst) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x0F);
    emitCondOpByte((byte)0x90, cond);
    emitRegIndirectRegOperands(dst, (byte) 0x00);
    if (lister != null) lister.RN(miStart, "SET" + CONDITION[cond], dst);
  }

  public final void emitSET_Cond_RegIdx_Byte(byte cond, byte base, byte index, short scale, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x0F);
    emitCondOpByte((byte)0x90, cond);
    emitSIBRegOperands(base, index, scale, disp, (byte) 0x00);
    if (lister != null) lister.RXD(miStart, "SET" + CONDITION[cond], base, index, scale, disp);
  }

  public final void emitSET_Cond_RegOff_Byte(byte cond, byte index, short scale, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x0F);
    emitCondOpByte((byte)0x90, cond);
    emitRegOffRegOperands(index, scale, disp, (byte) 0x00);
    if (lister != null) lister.RFD(miStart, "SET" + CONDITION[cond], index, scale, disp);
  }

  public final void emitSET_Cond_Abs_Byte(byte cond, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x0F);
    emitCondOpByte((byte)0x90, cond);
    emitAbsRegOperands(disp, (byte) 0x00);
    if (lister != null) lister.RA(miStart, "SET" + CONDITION[cond], disp);
  }

  public final void emitIMUL2_Reg_Reg(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xAF);
    emitRegRegOperands(srcReg, dstReg);
    if (lister != null) lister.RR(miStart, "IMUL", dstReg, srcReg);
  }

  public final void emitIMUL2_Reg_RegInd(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xAF);
    emitRegIndirectRegOperands(srcReg, dstReg);
    if (lister != null) lister.RRN(miStart, "IMUL", dstReg, srcReg);
  }

  public final void emitIMUL2_Reg_RegDisp(byte dstReg, byte srcReg, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xAF);
    emitRegDispRegOperands(srcReg, srcDisp, dstReg);
    if (lister != null) lister.RRD(miStart, "IMUL", dstReg, srcReg, srcDisp);
  }

  public final void emitIMUL2_Reg_RegOff(byte dstReg, byte srcIndex, short scale, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xAF);
    emitRegOffRegOperands(srcIndex, scale, disp, dstReg);
    if (lister != null) lister.RRFD(miStart, "IMUL", dstReg, srcIndex, scale, disp);
  }

  public final void emitIMUL2_Reg_Abs(byte dstReg, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xAF);
    emitAbsRegOperands(disp, dstReg);
    if (lister != null) lister.RRA(miStart, "IMUL", dstReg, disp);
  }

  public final void emitIMUL2_Reg_RegIdx(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xAF);
    emitSIBRegOperands(srcBase, srcIndex, scale, disp, dstReg);
    if (lister != null) lister.RRXD(miStart, "IMUL", dstReg, srcBase, srcIndex, scale, disp);
  }

  public final void emitIMUL2_Reg_Imm(byte dstReg, int imm) {
    int miStart = mi;
    if (fits(imm,8)) {
      setMachineCodes(mi++, (byte) 0x6B);
      emitRegRegOperands(dstReg, dstReg);
      emitImm8((byte)imm);
    } else {
      setMachineCodes(mi++, (byte) 0x69);
      emitRegRegOperands(dstReg, dstReg);
      emitImm32(imm);
    }
    if (lister != null) lister.RI(miStart, "IMUL", dstReg, imm);
  }

  // trap
  public final void emitINT_Imm (int v) {
    if (VM.VerifyAssertions) VM._assert(v <= 0xFF);
    int miStart = mi;
    if (v == 3) { // special case interrupt
      setMachineCodes(mi++, (byte) 0xCC);
    } else {
      setMachineCodes(mi++, (byte) 0xCD);
      setMachineCodes(mi++, (byte) v);
    }
    if (lister != null) lister.I(miStart, "INT", v);
  }

  /**
   * Conditionally branch to the given target, i.e.
   * <PRE>
   * if (cond) then IP = (instruction @ label)
   * </PRE>
   *
   * This emit method is expecting only a forward branch (that is
   * what the Label operand means); it creates a VM_ForwardReference
   * to the given label, and puts it into the assembler's list of
   * references to resolve.  This emiiter knows it emits conditional
   * branches, so it uses VM_ForwardReference.ConditionalBranch as the
   * forward reference type to create.
   *
   * All forward branches have a label as the branch target; clients
   * can arbirarily associate labels and instructions, but must be
   * consistent in giving the chosen label as the target of branches
   * to an instruction and calling resolveForwardBranches with the
   * given label immediately before emitting the target instruction.
   * See the header comments of VM_ForwardReference for more details.
   *
   * @param cond the IA32 ISA condition code bits to mask into opcode
   * @param label the label associated with the branch target instrucion
   *
   * @see VM_ForwardReference.ConditionalBranch
   */
  public final void emitJCC_Cond_Label (byte cond, int label) {
    int miStart = mi;
    VM_ForwardReference r =  new VM_ForwardReference.ConditionalBranch(mi, label);
    forwardRefs = VM_ForwardReference.enqueue(forwardRefs, r);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) (0x80 + cond));
    mi += 4; // leave space for displacement    TODO!! handle short branches
    if (lister != null) lister.I(miStart, "J" + CONDITION[cond], label);
  }

  /**
   * Conditionally branch to the given target, i.e.
   * <PRE>
   * if (cond) then IP = mTarget
   * </PRE>
   *
   * This emit method emits only backward branches (that is what
   * branching to an Imm operand means), so it simply writes the
   * appropriate binary code without bothering with the forward
   * reference mechanism.
   *
   * @param cond the IA32 ISA condition code bits to mask into opcode
   * @param mTarget the method-relative target offset
   */
  public final void emitJCC_Cond_Imm (byte cond, int mTarget) {
    int miStart = mi;
    int relOffset = mTarget - (mi + 1 + 1); // address relative to next instruction
    if (fits(relOffset, 8)) {
      emitCondOpByte((byte)0x70, cond);
      emitImm8((byte)relOffset);
    } else {
    setMachineCodes(mi++, (byte) 0x0F);
    emitCondOpByte((byte)0x80, cond);
    relOffset = mTarget - (mi + 4); // address relative to next instruction
    emitImm32(relOffset);
    }
    if (lister != null) lister.I(miStart, "J" + CONDITION[cond], relOffset);
  }

  /**
   * Conditionally branch to the given target, i.e.
   * <PRE>
   * if (cond) then IP = mTarget -or- (instruction @ bTarget)
   * </PRE>
   *
   * This emit method represents a branch that could be either
   * forward or backward; it simply calls either the Label or Imm
   * emit method.
   *
   * @see #emitJCC_Cond_Label
   * @see #emitJCC_Cond_Imm
   *
   * @param cond the IA32 ISA condition code bits to mask into opcode
   * @param mTarget the method-relative target offset
   * @param bTarget the label associated with the branch target instrucion
   */
  public final void emitJCC_Cond_ImmOrLabel (byte cond, int mTarget, int bTarget) {
    if (mTarget == 0) { // forward branch
      emitJCC_Cond_Label(cond, bTarget);
    } else { // backward branch
      emitJCC_Cond_Imm(cond, mTarget);
    }
  }

  public final void emitLEA_Reg_RegDisp(byte dstReg, byte srcReg, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x8D);
    emitRegDispRegOperands(srcReg, srcDisp, dstReg);
    if (lister != null) lister.RRD(miStart, "LEA", dstReg, srcReg, srcDisp);
  }

  public final void emitLEA_Reg_RegInd(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x8D);
    emitRegIndirectRegOperands(srcReg, dstReg);
    if (lister != null) lister.RRN(miStart, "LEA", dstReg, srcReg);
  }

  public final void emitLEA_Reg_RegOff(byte dstReg, byte idx, short scale, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x8D);
    emitRegOffRegOperands(idx, scale, disp, dstReg);
    if (lister != null) lister.RRFD(miStart, "LEA", dstReg, idx, scale, disp);
  }

  public final void emitLEA_Reg_Abs(byte dstReg, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x8D);
    emitAbsRegOperands(disp, dstReg);
    if (lister != null) lister.RRA(miStart, "LEA", dstReg, disp);
  }

  public final void emitLEA_Reg_RegIdx(byte dstReg, byte base, byte idx, short scale, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x8D);
    emitSIBRegOperands(base, idx, scale, disp, dstReg);
    if (lister != null) lister.RRXD(miStart, "LEA", dstReg, base, idx, scale, disp);
  }

  public final void emitMOV_Reg_Imm(byte dst, int imm) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) (0xB8 | dst));
    emitImm32(imm);
    if (lister != null) lister.RI(miStart, "MOV", dst, imm);
  }

  // pop address and goto it
  public final void emitRET () {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xC3);
    if (lister != null) lister.OP(miStart, "RET");
  }

  // pop address and goto it, pop parameterBytes additional bytes
  public final void emitRET_Imm (int parameterBytes) {
    int miStart = mi;
    if (parameterBytes == 0) {
      setMachineCodes(mi++, (byte) 0xC3);
      if (lister != null) lister.OP(miStart, "RET");
    } else {
      setMachineCodes(mi++, (byte) 0xC2);
      emitImm16(parameterBytes);
      if (VM.VerifyAssertions) VM._assert ((parameterBytes & 0xffff0000) == 0);
      if (lister != null) lister.I(miStart, "RET", parameterBytes);
    }
  }

  // allocate stack frame for procedure
  public final void emitENTER_Imm (int frameSize) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xC8);
    emitImm16(frameSize);
    setMachineCodes(mi++, (byte) 0x0);
    if (lister != null) lister.I(miStart, "ENTER", frameSize);
  }

  // sign extends EAX into EDX
  public final void emitCDQ () {
    int miStart = mi;
    setMachineCodes(mi++, (byte)0x99);
    if (lister != null) lister.OP(miStart, "CDQ");
  }

  // edx:eax <- time stamp counter
  // on Linux this appears to be unpriviledged
  public final void emitRDTSC() {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x31);
    if (lister != null) lister.OP(miStart, "RDTSC");
  }

  // software prefetch
  public final void emitPREFETCHNTA_Reg(byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++,(byte) 0x18);
    emitRegIndirectRegOperands(srcReg, (byte) 0);
    if (lister != null) lister.R(miStart, "PREFETCHNTA", srcReg);
  }

  // wait-loop pause
  public final void emitPAUSE () {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++,(byte) 0x90);
    if (lister != null) lister.OP(miStart, "PAUSE");
  }

  // nop
  public final void emitNOP () {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x90);
    if (lister != null) lister.OP(miStart, "NOP");
  }

  ////////////////////////////////////////////
  // hand-coded floating point instructions //
  ////////////////////////////////////////////

  // floating point conditional moves
  public final void emitFCMOV_Cond_Reg_Reg(byte cond, byte reg1, byte reg2) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(reg1 == FP0);
    switch (cond) {
      case LLT:
        setMachineCodes(mi++, (byte) 0xDA);
        setMachineCodes(mi++, (byte) (0xC0 + reg2));
        break;
      case EQ:
        setMachineCodes(mi++, (byte) 0xDA);
        setMachineCodes(mi++, (byte) (0xC8 + reg2));
        break;
      case LLE:
        setMachineCodes(mi++, (byte) 0xDA);
        setMachineCodes(mi++, (byte) (0xD0 + reg2));
        break;
      case PE:
        setMachineCodes(mi++, (byte) 0xDA);
        setMachineCodes(mi++, (byte) (0xD8 + reg2));
        break;
      case LGE:
        setMachineCodes(mi++, (byte) 0xDB);
        setMachineCodes(mi++, (byte) (0xC0 + reg2));
        break;
      case NE:
        setMachineCodes(mi++, (byte) 0xDB);
        setMachineCodes(mi++, (byte) (0xC8 + reg2));
        break;
      case LGT:
        setMachineCodes(mi++, (byte) 0xDB);
        setMachineCodes(mi++, (byte) (0xD0 + reg2));
        break;
      case PO:
        setMachineCodes(mi++, (byte) 0xDB);
        setMachineCodes(mi++, (byte) (0xD8 + reg2));
        break;
      default:
        if (VM.VerifyAssertions) VM._assert(VM.NOT_REACHED);
    }
    if (lister != null) lister.RR(miStart, "FCMOV" + CONDITION[cond], reg1, reg2);
  }

  // floating point push of ST(i) into ST(0)
  public final void emitFLD_Reg_Reg(byte destReg, byte srcReg) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(destReg == FP0);
    setMachineCodes(mi++, (byte) 0xD9);
    setMachineCodes(mi++, (byte) (0xC0 + srcReg));
    if (lister != null) lister.R(miStart, "FLD", srcReg);
  }

  // floating point copy of ST(0) into ST(I)
  public final void emitFST_Reg_Reg(byte destReg, byte srcReg) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(srcReg == FP0);
    setMachineCodes(mi++, (byte) 0xDD);
    setMachineCodes(mi++, (byte) (0xD0 + destReg));
    if (lister != null) lister.R(miStart, "FST", destReg);
  }

  // floating point pop of ST(0) into ST(I)
  public final void emitFSTP_Reg_Reg(byte destReg, byte srcReg) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(srcReg == FP0);
    setMachineCodes(mi++, (byte) 0xDD);
    setMachineCodes(mi++, (byte) (0xD8 + destReg));
    if (lister != null) lister.R(miStart, "FST", destReg);
  }

  // Change Sign: Top of FPU register stack -= Top og FPU register stack
  public final void emitFCHS () {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xD9);
    setMachineCodes(mi++, (byte) 0xE0);
    if (lister != null) lister.OP(miStart, "FADD32");
  }


  public final void emitFUCOMPP () {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xDA);
    setMachineCodes(mi++, (byte) 0xE9);
    if (lister != null) lister.OP(miStart, "FUCOMPP");
  }

  // Store Status Word into AX register/noexecptions
  public final void emitFNSTSW () {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xDF);
    setMachineCodes(mi++, (byte) 0xE0);
    if (lister != null) lister.OP(miStart, "FNSTSW");
  }

  // Store AH into Flags
  public final void emitSAHF () {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x9E);
    if (lister != null) lister.OP(miStart, "SAHF");
  }

  // Real Remainder:
  // Top of FPU register stack <- ST(0) - (Q*ST(1)
  // Q is the interger value obtained from truncating
  // ST(0)/ST(1) toward 0
  public final void emitFPREM () {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xD9);
    setMachineCodes(mi++, (byte) 0xF8);
    if (lister != null) lister.OP(miStart, "FPREM");
  }

  // Blow away floating point state
  public final void emitFINIT() {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x9B);
    setMachineCodes(mi++, (byte) 0xDB);
    setMachineCodes(mi++, (byte) 0xE3);
    if (lister != null) lister.OP(miStart, "FINIT");
  }

  // Blow away floating point state
  // Pending exceptions??? Don't tell me about pending exceptions!!
  public final void emitFNINIT() {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xDB);
    setMachineCodes(mi++, (byte) 0xE3);
    if (lister != null) lister.OP(miStart, "FNINIT");
  }

  // Declare we are no longer using FP register
  public final void emitFFREE_Reg(byte reg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xDD);
    setMachineCodes(mi++, (byte) ( (byte)0xC0 + reg ));
    if (lister != null) lister.R(miStart, "FFREE", reg);
  }

  // The dreaded FXCH
  // (symbol of all that's wrong with Intel floating point :)
  public final void emitFXCH_Reg_Reg(byte regOne, byte regTwo) {
    int miStart = mi;

    // at least one reg must not be FP0
    byte nonZeroReg = FP0; // :)
    if (regOne == FP0 && regTwo == FP0)
      // do nothing; this is stupid
      return;
    else if (regOne == FP0 && regTwo != FP0)
      nonZeroReg = regTwo;
    else if (regTwo == FP0 && regOne != FP0)
      nonZeroReg = regOne;

    // if not, bad instruction, so die
    if (nonZeroReg == FP0)
      VM._assert(false, "FXCH of " + regOne + ", " + regTwo);

    // generate it, with special case (of course) for FP1
    setMachineCodes(mi++, (byte) 0xD9);
    if (nonZeroReg == FP1)
      setMachineCodes(mi++, (byte) 0xC9);
    else
      setMachineCodes(mi++, (byte) (0xC8 | nonZeroReg));

    // list it
    if (lister != null) lister.R(miStart, "FXCH", nonZeroReg);
  }
  
  public final void emitXORPS_Reg_Reg(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x57);
    emitRegRegOperands(dstReg, srcReg);
    if (lister != null) lister.RR(miStart, "XORPS", dstReg, srcReg);
  }
  
    public final void emitXORPD_Reg_Reg(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x57);
    emitRegRegOperands(dstReg, srcReg);
    if (lister != null) lister.RR(miStart, "XORPD", dstReg, srcReg);
  }

  /**
   * Compare and exchange 8 bytes
   * <PRE>
   * cmpxchg8b [dst + disp]
   * </PRE>
   */
  public final void emitCMPXCHG8B_RegDisp(byte dst, Offset disp) {
      int miStart = mi;
      setMachineCodes(mi++, (byte) 0x0F);
      setMachineCodes(mi++, (byte) 0xC7);
      emitRegDispRegOperands(dst, disp, (byte)1);
      if (lister != null) lister.RD(miStart, "CMPXCHG8B" , dst, disp);
  }

  /**
   * Compare and exchange 8 bytes
   * <PRE>
   * cmpxchg8b [dst]
   * </PRE>
   */
  public final void emitCMPXCHG8B_RegInd(byte dst) {
      int miStart = mi;
      setMachineCodes(mi++, (byte) 0x0F);
      setMachineCodes(mi++, (byte) 0xC7);
      emitRegIndirectRegOperands(dst, (byte)1);
      if (lister != null) lister.R(miStart, "CMPXCHG8B" , dst);
  }

  /**
   * Compare and exchange 8 bytes
   * <PRE>
   * cmpxchg8b [index2<<scale2 + disp2]
   * </PRE>
   */
  public final void emitCMPXCHG8B_RegOff(byte index2, short scale2, Offset disp2) {
      int miStart = mi;
      setMachineCodes(mi++, (byte) 0x0F);
      setMachineCodes(mi++, (byte) 0xC7);
      emitRegOffRegOperands(index2, scale2, disp2, (byte)1);
      if (lister != null) lister.RFD(miStart, "CMPXCHG8B", index2, scale2, disp2);
  }

  /**
   * Compare and exchange 8 bytes
   * <PRE>
   * cmpxchg8b [base + index2<<scale2 + disp2]
   * </PRE>
   */
  public final void emitCMPXCHG8B_RegIdx(byte base2, byte index2, short scale2, Offset disp2) {
      int miStart = mi;
      setMachineCodes(mi++, (byte) 0x0F);
      setMachineCodes(mi++, (byte) 0xC7);
      emitSIBRegOperands(base2, index2, scale2, disp2, (byte)1);
      if (lister != null) lister.RXD(miStart, "CMPXCHG8B", base2, index2, scale2, disp2);
  }

  /*
   * BELOW HERE ARE AUTOMATICALLY-GENERATED INSTRUCTIONS.  DO NOT EDIT.
   *
   * These instructions are generated by genAssembler.sh in the
   * src-generated/ia32-assembler directory.  Please make all needed
   * edits to that script.
   */
  /**
   * Generate a register--register ADC. That is,
   * <PRE>
   * [dstReg] +CF=  srcReg
   * </PRE>
   *
   * @param dstReg the destination register
   * @param srcReg the source register
   */
  public final void emitADC_RegInd_Reg(byte dstReg, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x11);
    emitRegIndirectRegOperands(dstReg, srcReg);
    if (lister != null) lister.RNR(miStart, "ADC", dstReg, srcReg);
  }

  /**
   * Generate a register-offset--register ADC. That is,
   * <PRE>
   * [dstReg<<dstScale + dstDisp] +CF=  srcReg
   * </PRE>
   *
   * @param dstIndex the destination index register
   * @param dstScale the destination shift amount
   * @param dstDisp the destination displacement
   * @param srcReg the source register
   */
  public final void emitADC_RegOff_Reg(byte dstIndex, short dstScale, Offset dstDisp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x11);
    emitRegOffRegOperands(dstIndex, dstScale, dstDisp, srcReg);
    if (lister != null) lister.RFDR(miStart, "ADC", dstIndex, dstScale, dstDisp, srcReg);
  }

  // [dstDisp] +CF=  srcReg
  public final void emitADC_Abs_Reg(Offset dstDisp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x11);
    emitAbsRegOperands(dstDisp, srcReg);
    if (lister != null) lister.RAR(miStart, "ADC", dstDisp, srcReg);
  }

  // [dstBase + dstIndex<<scale + dstDisp] +CF=  srcReg
  public final void emitADC_RegIdx_Reg(byte dstBase, byte dstIndex, short scale, Offset dstDisp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x11);
    emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, srcReg);
    if (lister != null) lister.RXDR(miStart, "ADC", dstBase, dstIndex, scale, dstDisp, srcReg);
  }

  // [dstReg + dstDisp] +CF=  srcReg
  public final void emitADC_RegDisp_Reg(byte dstReg, Offset disp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x11);
    emitRegDispRegOperands(dstReg, disp, srcReg);
    if (lister != null) lister.RDR(miStart, "ADC", dstReg, disp, srcReg);
  }

  // dstReg +CF=  srcReg
  public final void emitADC_Reg_Reg(byte dstReg, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x11);
    emitRegRegOperands(dstReg, srcReg);
    if (lister != null) lister.RR(miStart, "ADC", dstReg, srcReg);
  }

  // dstReg +CF=  [srcReg + srcDisp]
  public final void emitADC_Reg_RegDisp(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x13);
    emitRegDispRegOperands(srcReg, disp, dstReg);
    if (lister != null) lister.RRD(miStart, "ADC", dstReg, srcReg, disp);
  }

  // dstReg +CF=  [srcIndex<<scale + srcDisp]
  public final void emitADC_Reg_RegOff(byte dstReg, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x13);
    emitRegOffRegOperands(srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRFD(miStart, "ADC", dstReg, srcIndex, scale, srcDisp);
  }

  // dstReg +CF=  [srcDisp]
  public final void emitADC_Reg_Abs(byte dstReg, Offset srcDisp) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x13);
    emitAbsRegOperands(srcDisp, dstReg);
    if (lister != null) lister.RRA(miStart, "ADC", dstReg, srcDisp);
  }

  // dstReg +CF=  [srcBase + srcIndex<<scale + srcDisp]
  public final void emitADC_Reg_RegIdx(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x13);
    emitSIBRegOperands(srcBase, srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRXD(miStart, "ADC", dstReg, srcBase, srcIndex, scale, srcDisp);
  }

  // dstReg +CF=  [srcReg]
  public final void emitADC_Reg_RegInd(byte dstReg, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x13);
    emitRegIndirectRegOperands(srcReg, dstReg);
    if (lister != null) lister.RRN(miStart, "ADC", dstReg, srcReg);
  }

  /**
   * Generate a register--register ADC. That is,
   * <PRE>
   * [dstReg] +CF=  (word)  srcReg
   * </PRE>
   *
   * @param dstReg the destination register
   * @param srcReg the source register
   */
  public final void emitADC_RegInd_Reg_Word(byte dstReg, byte srcReg) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x11);
    emitRegIndirectRegOperands(dstReg, srcReg);
    if (lister != null) lister.RNR(miStart, "ADC", dstReg, srcReg);
  }

  /**
   * Generate a register-offset--register ADC. That is,
   * <PRE>
   * [dstReg<<dstScale + dstDisp] +CF=  (word)  srcReg
   * </PRE>
   *
   * @param dstIndex the destination index register
   * @param dstScale the destination shift amount
   * @param dstDisp the destination displacement
   * @param srcReg the source register
   */
  public final void emitADC_RegOff_Reg_Word(byte dstIndex, short dstScale, Offset dstDisp, byte srcReg) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x11);
    emitRegOffRegOperands(dstIndex, dstScale, dstDisp, srcReg);
    if (lister != null) lister.RFDR(miStart, "ADC", dstIndex, dstScale, dstDisp, srcReg);
  }

  // [dstDisp] +CF=  (word)  srcReg
  public final void emitADC_Abs_Reg_Word(Offset dstDisp, byte srcReg) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x11);
    emitAbsRegOperands(dstDisp, srcReg);
    if (lister != null) lister.RAR(miStart, "ADC", dstDisp, srcReg);
  }

  // [dstBase + dstIndex<<scale + dstDisp] +CF=  (word)  srcReg
  public final void emitADC_RegIdx_Reg_Word(byte dstBase, byte dstIndex, short scale, Offset dstDisp, byte srcReg) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x11);
    emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, srcReg);
    if (lister != null) lister.RXDR(miStart, "ADC", dstBase, dstIndex, scale, dstDisp, srcReg);
  }

  // [dstReg + dstDisp] +CF=  (word)  srcReg
  public final void emitADC_RegDisp_Reg_Word(byte dstReg, Offset disp, byte srcReg) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x11);
    emitRegDispRegOperands(dstReg, disp, srcReg);
    if (lister != null) lister.RDR(miStart, "ADC", dstReg, disp, srcReg);
  }

  // dstReg +CF=  (word)  srcReg
  public final void emitADC_Reg_Reg_Word(byte dstReg, byte srcReg) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x11);
    emitRegRegOperands(dstReg, srcReg);
    if (lister != null) lister.RR(miStart, "ADC", dstReg, srcReg);
  }

  // dstReg +CF=  (word)  [srcReg + srcDisp]
  public final void emitADC_Reg_RegDisp_Word(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x13);
    emitRegDispRegOperands(srcReg, disp, dstReg);
    if (lister != null) lister.RRD(miStart, "ADC", dstReg, srcReg, disp);
  }

  // dstReg +CF=  (word)  [srcIndex<<scale + srcDisp]
  public final void emitADC_Reg_RegOff_Word(byte dstReg, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x13);
    emitRegOffRegOperands(srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRFD(miStart, "ADC", dstReg, srcIndex, scale, srcDisp);
  }

  // dstReg +CF=  (word)  [srcDisp]
  public final void emitADC_Reg_Abs_Word(byte dstReg, Offset srcDisp) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x13);
    emitAbsRegOperands(srcDisp, dstReg);
    if (lister != null) lister.RRA(miStart, "ADC", dstReg, srcDisp);
  }

  // dstReg +CF=  (word)  [srcBase + srcIndex<<scale + srcDisp]
  public final void emitADC_Reg_RegIdx_Word(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x13);
    emitSIBRegOperands(srcBase, srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRXD(miStart, "ADC", dstReg, srcBase, srcIndex, scale, srcDisp);
  }

  // dstReg +CF=  (word)  [srcReg]
  public final void emitADC_Reg_RegInd_Word(byte dstReg, byte srcReg) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x13);
    emitRegIndirectRegOperands(srcReg, dstReg);
    if (lister != null) lister.RRN(miStart, "ADC", dstReg, srcReg);
  }

  /**
   * Generate a register--register ADC. That is,
   * <PRE>
   * [dstReg] +CF=  (byte)  srcReg
   * </PRE>
   *
   * @param dstReg the destination register
   * @param srcReg the source register
   */
  public final void emitADC_RegInd_Reg_Byte(byte dstReg, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x10);
    emitRegIndirectRegOperands(dstReg, srcReg);
    if (lister != null) lister.RNR(miStart, "ADC", dstReg, srcReg);
  }

  /**
   * Generate a register-offset--register ADC. That is,
   * <PRE>
   * [dstReg<<dstScale + dstDisp] +CF=  (byte)  srcReg
   * </PRE>
   *
   * @param dstIndex the destination index register
   * @param dstScale the destination shift amount
   * @param dstDisp the destination displacement
   * @param srcReg the source register
   */
  public final void emitADC_RegOff_Reg_Byte(byte dstIndex, short dstScale, Offset dstDisp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x10);
    emitRegOffRegOperands(dstIndex, dstScale, dstDisp, srcReg);
    if (lister != null) lister.RFDR(miStart, "ADC", dstIndex, dstScale, dstDisp, srcReg);
  }

  // [dstDisp] +CF=  (byte)  srcReg
  public final void emitADC_Abs_Reg_Byte(Offset dstDisp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x10);
    emitAbsRegOperands(dstDisp, srcReg);
    if (lister != null) lister.RAR(miStart, "ADC", dstDisp, srcReg);
  }

  // [dstBase + dstIndex<<scale + dstDisp] +CF=  (byte)  srcReg
  public final void emitADC_RegIdx_Reg_Byte(byte dstBase, byte dstIndex, short scale, Offset dstDisp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x10);
    emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, srcReg);
    if (lister != null) lister.RXDR(miStart, "ADC", dstBase, dstIndex, scale, dstDisp, srcReg);
  }

  // [dstReg + dstDisp] +CF=  (byte)  srcReg
  public final void emitADC_RegDisp_Reg_Byte(byte dstReg, Offset disp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x10);
    emitRegDispRegOperands(dstReg, disp, srcReg);
    if (lister != null) lister.RDR(miStart, "ADC", dstReg, disp, srcReg);
  }

  // dstReg +CF=  (byte)  srcReg
  public final void emitADC_Reg_Reg_Byte(byte dstReg, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x10);
    emitRegRegOperands(dstReg, srcReg);
    if (lister != null) lister.RR(miStart, "ADC", dstReg, srcReg);
  }

  // dstReg +CF=  (byte)  [srcReg + srcDisp]
  public final void emitADC_Reg_RegDisp_Byte(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x12);
    emitRegDispRegOperands(srcReg, disp, dstReg);
    if (lister != null) lister.RRD(miStart, "ADC", dstReg, srcReg, disp);
  }

  // dstReg +CF=  (byte)  [srcIndex<<scale + srcDisp]
  public final void emitADC_Reg_RegOff_Byte(byte dstReg, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x12);
    emitRegOffRegOperands(srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRFD(miStart, "ADC", dstReg, srcIndex, scale, srcDisp);
  }

  // dstReg +CF=  (byte)  [srcDisp]
  public final void emitADC_Reg_Abs_Byte(byte dstReg, Offset srcDisp) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x12);
    emitAbsRegOperands(srcDisp, dstReg);
    if (lister != null) lister.RRA(miStart, "ADC", dstReg, srcDisp);
  }

  // dstReg +CF=  (byte)  [srcBase + srcIndex<<scale + srcDisp]
  public final void emitADC_Reg_RegIdx_Byte(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x12);
    emitSIBRegOperands(srcBase, srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRXD(miStart, "ADC", dstReg, srcBase, srcIndex, scale, srcDisp);
  }

  // dstReg +CF=  (byte)  [srcReg]
  public final void emitADC_Reg_RegInd_Byte(byte dstReg, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x12);
    emitRegIndirectRegOperands(srcReg, dstReg);
    if (lister != null) lister.RRN(miStart, "ADC", dstReg, srcReg);
  }

  // dstReg +CF=  imm
  public final void emitADC_Reg_Imm(byte dstReg, int imm) {
    int miStart = mi;
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x2" is really part of the opcode
        emitRegRegOperands(dstReg, (byte) 0x2);
        emitImm8((byte)imm);
    } else if (dstReg == EAX) {
        setMachineCodes(mi++, (byte) 0x15);
        emitImm32(imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x2" is really part of the opcode
        emitRegRegOperands(dstReg, (byte) 0x2);
        emitImm32(imm);
    }
        if (lister != null) lister.RI(miStart, "ADC", dstReg, imm);
    }

  // [dstReg + dstDisp] +CF=  imm
  public final void emitADC_RegDisp_Imm(byte dstReg, Offset disp, int imm) {
    int miStart = mi;
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x2" is really part of the opcode
        emitRegDispRegOperands(dstReg, disp, (byte) 0x2);
        emitImm8((byte)imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x2" is really part of the opcode
        emitRegDispRegOperands(dstReg, disp, (byte) 0x2);
        emitImm32(imm);
    }
    if (lister != null) lister.RDI(miStart, "ADC", dstReg, disp, imm);
  }

  // [dstIndex<<scale + dstDisp] +CF=  imm
  public final void emitADC_RegOff_Imm(byte dstIndex, short scale, Offset dstDisp, int imm) {
    int miStart = mi;
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x2" is really part of the opcode
        emitRegOffRegOperands(dstIndex, scale, dstDisp, (byte) 0x2);
        emitImm8((byte)imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x2" is really part of the opcode
        emitRegOffRegOperands(dstIndex, scale, dstDisp, (byte) 0x2);
        emitImm32(imm);
    }
    if (lister != null) lister.RFDI(miStart, "ADC", dstIndex, scale, dstDisp, imm);
  }

  // [dstDisp] +CF=  imm
  public final void emitADC_Abs_Imm(Offset dstDisp, int imm) {
    int miStart = mi;
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x2" is really part of the opcode
        emitAbsRegOperands(dstDisp, (byte) 0x2);
        emitImm8((byte)imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x2" is really part of the opcode
        emitAbsRegOperands(dstDisp, (byte) 0x2);
        emitImm32(imm);
    }
    if (lister != null) lister.RAI(miStart, "ADC", dstDisp, imm);
  }

  // [dstBase + dstIndex<<scale + dstDisp] +CF=  imm
  public final void emitADC_RegIdx_Imm(byte dstBase, byte dstIndex, short scale, Offset dstDisp, int imm) {
    int miStart = mi;
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x2" is really part of the opcode
        emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, (byte) 0x2);
        emitImm8((byte)imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x2" is really part of the opcode
        emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, (byte) 0x2);
        emitImm32(imm);
    }
    if (lister != null) lister.RXDI(miStart, "ADC", dstBase, dstIndex, scale, dstDisp, imm);
  }

  // [dstReg] +CF=  imm
  public final void emitADC_RegInd_Imm(byte dstReg, int imm) {
    int miStart = mi;
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x2" is really part of the opcode
        emitRegIndirectRegOperands(dstReg, (byte) 0x2);
        emitImm8((byte)imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x2" is really part of the opcode
        emitRegIndirectRegOperands(dstReg, (byte) 0x2);
        emitImm32(imm);
    }
    if (lister != null) lister.RNI(miStart, "ADC", dstReg, imm);
  }

  // dstReg +CF=  (word)  imm
  public final void emitADC_Reg_Imm_Word(byte dstReg, int imm) {
    int miStart = mi;
            setMachineCodes(mi++, (byte) 0x66);
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x2" is really part of the opcode
        emitRegRegOperands(dstReg, (byte) 0x2);
        emitImm8((byte)imm);
    } else if (dstReg == EAX) {
        setMachineCodes(mi++, (byte) 0x15);
        emitImm16(imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x2" is really part of the opcode
        emitRegRegOperands(dstReg, (byte) 0x2);
        emitImm16(imm);
    }
        if (lister != null) lister.RI(miStart, "ADC", dstReg, imm);
    }

  // [dstReg + dstDisp] +CF=  (word)  imm
  public final void emitADC_RegDisp_Imm_Word(byte dstReg, Offset disp, int imm) {
    int miStart = mi;
            setMachineCodes(mi++, (byte) 0x66);
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x2" is really part of the opcode
        emitRegDispRegOperands(dstReg, disp, (byte) 0x2);
        emitImm8((byte)imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x2" is really part of the opcode
        emitRegDispRegOperands(dstReg, disp, (byte) 0x2);
        emitImm16(imm);
    }
    if (lister != null) lister.RDI(miStart, "ADC", dstReg, disp, imm);
  }

  // [dstIndex<<scale + dstDisp] +CF=  (word)  imm
  public final void emitADC_RegOff_Imm_Word(byte dstIndex, short scale, Offset dstDisp, int imm) {
    int miStart = mi;
            setMachineCodes(mi++, (byte) 0x66);
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x2" is really part of the opcode
        emitRegOffRegOperands(dstIndex, scale, dstDisp, (byte) 0x2);
        emitImm8((byte)imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x2" is really part of the opcode
        emitRegOffRegOperands(dstIndex, scale, dstDisp, (byte) 0x2);
        emitImm32(imm);
    }
    if (lister != null) lister.RFDI(miStart, "ADC", dstIndex, scale, dstDisp, imm);
  }

  // [dstDisp] +CF=  (word)  imm
  public final void emitADC_Abs_Imm_Word(Offset dstDisp, int imm) {
    int miStart = mi;
            setMachineCodes(mi++, (byte) 0x66);
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x2" is really part of the opcode
        emitAbsRegOperands(dstDisp, (byte) 0x2);
        emitImm8((byte)imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x2" is really part of the opcode
        emitAbsRegOperands(dstDisp, (byte) 0x2);
        emitImm32(imm);
    }
    if (lister != null) lister.RAI(miStart, "ADC", dstDisp, imm);
  }

  // [dstBase + dstIndex<<scale + dstDisp] +CF=  (word)  imm
  public final void emitADC_RegIdx_Imm_Word(byte dstBase, byte dstIndex, short scale, Offset dstDisp, int imm) {
    int miStart = mi;
            setMachineCodes(mi++, (byte) 0x66);
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x2" is really part of the opcode
        emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, (byte) 0x2);
        emitImm8((byte)imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x2" is really part of the opcode
        emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, (byte) 0x2);
        emitImm32(imm);
    }
    if (lister != null) lister.RXDI(miStart, "ADC", dstBase, dstIndex, scale, dstDisp, imm);
  }

  // [dstReg] +CF=  (word)  imm
  public final void emitADC_RegInd_Imm_Word(byte dstReg, int imm) {
    int miStart = mi;
            setMachineCodes(mi++, (byte) 0x66);
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x2" is really part of the opcode
        emitRegIndirectRegOperands(dstReg, (byte) 0x2);
        emitImm8((byte)imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x2" is really part of the opcode
        emitRegIndirectRegOperands(dstReg, (byte) 0x2);
        emitImm16(imm);
    }
    if (lister != null) lister.RNI(miStart, "ADC", dstReg, imm);
  }

  // dstReg +CF= (byte) imm
  public final void emitADC_Reg_Imm_Byte(byte dstReg, int imm) {
    int miStart = mi;
    if (dstReg == EAX) {
        setMachineCodes(mi++, (byte) 0x14);
        emitImm8(imm);
    } else {
        setMachineCodes(mi++, (byte) 0x80);
        // "register 0x2" is really part of the opcode
        emitRegRegOperands(dstReg, (byte) 0x2);
        emitImm8(imm);
    }
        if (lister != null) lister.RI(miStart, "ADC", dstReg, imm);
    }

  // [dstReg + dstDisp] +CF= (byte) imm
  public final void emitADC_RegDisp_Imm_Byte(byte dstReg, Offset disp, int imm) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x80);
    // "register 0x2" is really part of the opcode
    emitRegDispRegOperands(dstReg, disp, (byte) 0x2);
    emitImm8(imm);
    if (lister != null) lister.RDI(miStart, "ADC", dstReg, disp, imm);
  }

  // [dstBase + dstIndex<<scale + dstDisp] +CF= (byte) imm
  public final void emitADC_RegIdx_Imm_Byte(byte dstBase, byte dstIndex, short scale, Offset dstDisp, int imm) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x80);
    // "register 0x2" is really part of the opcode
    emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, (byte) 0x2);
    emitImm8(imm);
    if (lister != null) lister.RXDI(miStart, "ADC", dstBase, dstIndex, scale, dstDisp, imm);
  }

  // [dstIndex<<scale + dstDisp] +CF= (byte) imm
  public final void emitADC_RegOff_Imm_Byte(byte dstIndex, short scale, Offset dstDisp, int imm) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x80);
    // "register 0x2" is really part of the opcode
    emitRegOffRegOperands(dstIndex, scale, dstDisp, (byte) 0x2);
    emitImm8(imm);
    if (lister != null) lister.RFDI(miStart, "ADC", dstIndex, scale, dstDisp, imm);
  }

  // [dstDisp] +CF= (byte) imm
  public final void emitADC_Abs_Imm_Byte(Offset dstDisp, int imm) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x80);
    // "register 0x2" is really part of the opcode
    emitAbsRegOperands(dstDisp, (byte) 0x2);
    emitImm8(imm);
    if (lister != null) lister.RAI(miStart, "ADC", dstDisp, imm);
  }

  // [dstReg] +CF= (byte) imm
  public final void emitADC_RegInd_Imm_Byte(byte dstReg, int imm) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x80);
    // "register 0x2" is really part of the opcode
    emitRegIndirectRegOperands(dstReg, (byte) 0x2);
    emitImm8(imm);
    if (lister != null) lister.RNI(miStart, "ADC", dstReg, imm);
  }

  /**
   * Generate a register--register ADD. That is,
   * <PRE>
   * [dstReg] +=  srcReg
   * </PRE>
   *
   * @param dstReg the destination register
   * @param srcReg the source register
   */
  public final void emitADD_RegInd_Reg(byte dstReg, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x01);
    emitRegIndirectRegOperands(dstReg, srcReg);
    if (lister != null) lister.RNR(miStart, "ADD", dstReg, srcReg);
  }

  /**
   * Generate a register-offset--register ADD. That is,
   * <PRE>
   * [dstReg<<dstScale + dstDisp] +=  srcReg
   * </PRE>
   *
   * @param dstIndex the destination index register
   * @param dstScale the destination shift amount
   * @param dstDisp the destination displacement
   * @param srcReg the source register
   */
  public final void emitADD_RegOff_Reg(byte dstIndex, short dstScale, Offset dstDisp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x01);
    emitRegOffRegOperands(dstIndex, dstScale, dstDisp, srcReg);
    if (lister != null) lister.RFDR(miStart, "ADD", dstIndex, dstScale, dstDisp, srcReg);
  }

  // [dstDisp] +=  srcReg
  public final void emitADD_Abs_Reg(Offset dstDisp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x01);
    emitAbsRegOperands(dstDisp, srcReg);
    if (lister != null) lister.RAR(miStart, "ADD", dstDisp, srcReg);
  }

  // [dstBase + dstIndex<<scale + dstDisp] +=  srcReg
  public final void emitADD_RegIdx_Reg(byte dstBase, byte dstIndex, short scale, Offset dstDisp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x01);
    emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, srcReg);
    if (lister != null) lister.RXDR(miStart, "ADD", dstBase, dstIndex, scale, dstDisp, srcReg);
  }

  // [dstReg + dstDisp] +=  srcReg
  public final void emitADD_RegDisp_Reg(byte dstReg, Offset disp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x01);
    emitRegDispRegOperands(dstReg, disp, srcReg);
    if (lister != null) lister.RDR(miStart, "ADD", dstReg, disp, srcReg);
  }

  // dstReg +=  srcReg
  public final void emitADD_Reg_Reg(byte dstReg, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x01);
    emitRegRegOperands(dstReg, srcReg);
    if (lister != null) lister.RR(miStart, "ADD", dstReg, srcReg);
  }

  // dstReg +=  [srcReg + srcDisp]
  public final void emitADD_Reg_RegDisp(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x03);
    emitRegDispRegOperands(srcReg, disp, dstReg);
    if (lister != null) lister.RRD(miStart, "ADD", dstReg, srcReg, disp);
  }

  // dstReg +=  [srcIndex<<scale + srcDisp]
  public final void emitADD_Reg_RegOff(byte dstReg, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x03);
    emitRegOffRegOperands(srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRFD(miStart, "ADD", dstReg, srcIndex, scale, srcDisp);
  }

  // dstReg +=  [srcDisp]
  public final void emitADD_Reg_Abs(byte dstReg, Offset srcDisp) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x03);
    emitAbsRegOperands(srcDisp, dstReg);
    if (lister != null) lister.RRA(miStart, "ADD", dstReg, srcDisp);
  }

  // dstReg +=  [srcBase + srcIndex<<scale + srcDisp]
  public final void emitADD_Reg_RegIdx(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x03);
    emitSIBRegOperands(srcBase, srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRXD(miStart, "ADD", dstReg, srcBase, srcIndex, scale, srcDisp);
  }

  // dstReg +=  [srcReg]
  public final void emitADD_Reg_RegInd(byte dstReg, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x03);
    emitRegIndirectRegOperands(srcReg, dstReg);
    if (lister != null) lister.RRN(miStart, "ADD", dstReg, srcReg);
  }

  /**
   * Generate a register--register ADD. That is,
   * <PRE>
   * [dstReg] +=  (word)  srcReg
   * </PRE>
   *
   * @param dstReg the destination register
   * @param srcReg the source register
   */
  public final void emitADD_RegInd_Reg_Word(byte dstReg, byte srcReg) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x01);
    emitRegIndirectRegOperands(dstReg, srcReg);
    if (lister != null) lister.RNR(miStart, "ADD", dstReg, srcReg);
  }

  /**
   * Generate a register-offset--register ADD. That is,
   * <PRE>
   * [dstReg<<dstScale + dstDisp] +=  (word)  srcReg
   * </PRE>
   *
   * @param dstIndex the destination index register
   * @param dstScale the destination shift amount
   * @param dstDisp the destination displacement
   * @param srcReg the source register
   */
  public final void emitADD_RegOff_Reg_Word(byte dstIndex, short dstScale, Offset dstDisp, byte srcReg) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x01);
    emitRegOffRegOperands(dstIndex, dstScale, dstDisp, srcReg);
    if (lister != null) lister.RFDR(miStart, "ADD", dstIndex, dstScale, dstDisp, srcReg);
  }

  // [dstDisp] +=  (word)  srcReg
  public final void emitADD_Abs_Reg_Word(Offset dstDisp, byte srcReg) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x01);
    emitAbsRegOperands(dstDisp, srcReg);
    if (lister != null) lister.RAR(miStart, "ADD", dstDisp, srcReg);
  }

  // [dstBase + dstIndex<<scale + dstDisp] +=  (word)  srcReg
  public final void emitADD_RegIdx_Reg_Word(byte dstBase, byte dstIndex, short scale, Offset dstDisp, byte srcReg) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x01);
    emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, srcReg);
    if (lister != null) lister.RXDR(miStart, "ADD", dstBase, dstIndex, scale, dstDisp, srcReg);
  }

  // [dstReg + dstDisp] +=  (word)  srcReg
  public final void emitADD_RegDisp_Reg_Word(byte dstReg, Offset disp, byte srcReg) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x01);
    emitRegDispRegOperands(dstReg, disp, srcReg);
    if (lister != null) lister.RDR(miStart, "ADD", dstReg, disp, srcReg);
  }

  // dstReg +=  (word)  srcReg
  public final void emitADD_Reg_Reg_Word(byte dstReg, byte srcReg) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x01);
    emitRegRegOperands(dstReg, srcReg);
    if (lister != null) lister.RR(miStart, "ADD", dstReg, srcReg);
  }

  // dstReg +=  (word)  [srcReg + srcDisp]
  public final void emitADD_Reg_RegDisp_Word(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x03);
    emitRegDispRegOperands(srcReg, disp, dstReg);
    if (lister != null) lister.RRD(miStart, "ADD", dstReg, srcReg, disp);
  }

  // dstReg +=  (word)  [srcIndex<<scale + srcDisp]
  public final void emitADD_Reg_RegOff_Word(byte dstReg, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x03);
    emitRegOffRegOperands(srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRFD(miStart, "ADD", dstReg, srcIndex, scale, srcDisp);
  }

  // dstReg +=  (word)  [srcDisp]
  public final void emitADD_Reg_Abs_Word(byte dstReg, Offset srcDisp) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x03);
    emitAbsRegOperands(srcDisp, dstReg);
    if (lister != null) lister.RRA(miStart, "ADD", dstReg, srcDisp);
  }

  // dstReg +=  (word)  [srcBase + srcIndex<<scale + srcDisp]
  public final void emitADD_Reg_RegIdx_Word(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x03);
    emitSIBRegOperands(srcBase, srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRXD(miStart, "ADD", dstReg, srcBase, srcIndex, scale, srcDisp);
  }

  // dstReg +=  (word)  [srcReg]
  public final void emitADD_Reg_RegInd_Word(byte dstReg, byte srcReg) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x03);
    emitRegIndirectRegOperands(srcReg, dstReg);
    if (lister != null) lister.RRN(miStart, "ADD", dstReg, srcReg);
  }

  /**
   * Generate a register--register ADD. That is,
   * <PRE>
   * [dstReg] +=  (byte)  srcReg
   * </PRE>
   *
   * @param dstReg the destination register
   * @param srcReg the source register
   */
  public final void emitADD_RegInd_Reg_Byte(byte dstReg, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x00);
    emitRegIndirectRegOperands(dstReg, srcReg);
    if (lister != null) lister.RNR(miStart, "ADD", dstReg, srcReg);
  }

  /**
   * Generate a register-offset--register ADD. That is,
   * <PRE>
   * [dstReg<<dstScale + dstDisp] +=  (byte)  srcReg
   * </PRE>
   *
   * @param dstIndex the destination index register
   * @param dstScale the destination shift amount
   * @param dstDisp the destination displacement
   * @param srcReg the source register
   */
  public final void emitADD_RegOff_Reg_Byte(byte dstIndex, short dstScale, Offset dstDisp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x00);
    emitRegOffRegOperands(dstIndex, dstScale, dstDisp, srcReg);
    if (lister != null) lister.RFDR(miStart, "ADD", dstIndex, dstScale, dstDisp, srcReg);
  }

  // [dstDisp] +=  (byte)  srcReg
  public final void emitADD_Abs_Reg_Byte(Offset dstDisp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x00);
    emitAbsRegOperands(dstDisp, srcReg);
    if (lister != null) lister.RAR(miStart, "ADD", dstDisp, srcReg);
  }

  // [dstBase + dstIndex<<scale + dstDisp] +=  (byte)  srcReg
  public final void emitADD_RegIdx_Reg_Byte(byte dstBase, byte dstIndex, short scale, Offset dstDisp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x00);
    emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, srcReg);
    if (lister != null) lister.RXDR(miStart, "ADD", dstBase, dstIndex, scale, dstDisp, srcReg);
  }

  // [dstReg + dstDisp] +=  (byte)  srcReg
  public final void emitADD_RegDisp_Reg_Byte(byte dstReg, Offset disp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x00);
    emitRegDispRegOperands(dstReg, disp, srcReg);
    if (lister != null) lister.RDR(miStart, "ADD", dstReg, disp, srcReg);
  }

  // dstReg +=  (byte)  srcReg
  public final void emitADD_Reg_Reg_Byte(byte dstReg, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x00);
    emitRegRegOperands(dstReg, srcReg);
    if (lister != null) lister.RR(miStart, "ADD", dstReg, srcReg);
  }

  // dstReg +=  (byte)  [srcReg + srcDisp]
  public final void emitADD_Reg_RegDisp_Byte(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x02);
    emitRegDispRegOperands(srcReg, disp, dstReg);
    if (lister != null) lister.RRD(miStart, "ADD", dstReg, srcReg, disp);
  }

  // dstReg +=  (byte)  [srcIndex<<scale + srcDisp]
  public final void emitADD_Reg_RegOff_Byte(byte dstReg, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x02);
    emitRegOffRegOperands(srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRFD(miStart, "ADD", dstReg, srcIndex, scale, srcDisp);
  }

  // dstReg +=  (byte)  [srcDisp]
  public final void emitADD_Reg_Abs_Byte(byte dstReg, Offset srcDisp) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x02);
    emitAbsRegOperands(srcDisp, dstReg);
    if (lister != null) lister.RRA(miStart, "ADD", dstReg, srcDisp);
  }

  // dstReg +=  (byte)  [srcBase + srcIndex<<scale + srcDisp]
  public final void emitADD_Reg_RegIdx_Byte(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x02);
    emitSIBRegOperands(srcBase, srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRXD(miStart, "ADD", dstReg, srcBase, srcIndex, scale, srcDisp);
  }

  // dstReg +=  (byte)  [srcReg]
  public final void emitADD_Reg_RegInd_Byte(byte dstReg, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x02);
    emitRegIndirectRegOperands(srcReg, dstReg);
    if (lister != null) lister.RRN(miStart, "ADD", dstReg, srcReg);
  }

  // dstReg +=  imm
  public final void emitADD_Reg_Imm(byte dstReg, int imm) {
    int miStart = mi;
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x0" is really part of the opcode
        emitRegRegOperands(dstReg, (byte) 0x0);
        emitImm8((byte)imm);
    } else if (dstReg == EAX) {
        setMachineCodes(mi++, (byte) 0x05);
        emitImm32(imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x0" is really part of the opcode
        emitRegRegOperands(dstReg, (byte) 0x0);
        emitImm32(imm);
    }
        if (lister != null) lister.RI(miStart, "ADD", dstReg, imm);
    }

  // [dstReg + dstDisp] +=  imm
  public final void emitADD_RegDisp_Imm(byte dstReg, Offset disp, int imm) {
    int miStart = mi;
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x0" is really part of the opcode
        emitRegDispRegOperands(dstReg, disp, (byte) 0x0);
        emitImm8((byte)imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x0" is really part of the opcode
        emitRegDispRegOperands(dstReg, disp, (byte) 0x0);
        emitImm32(imm);
    }
    if (lister != null) lister.RDI(miStart, "ADD", dstReg, disp, imm);
  }

  // [dstIndex<<scale + dstDisp] +=  imm
  public final void emitADD_RegOff_Imm(byte dstIndex, short scale, Offset dstDisp, int imm) {
    int miStart = mi;
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x0" is really part of the opcode
        emitRegOffRegOperands(dstIndex, scale, dstDisp, (byte) 0x0);
        emitImm8((byte)imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x0" is really part of the opcode
        emitRegOffRegOperands(dstIndex, scale, dstDisp, (byte) 0x0);
        emitImm32(imm);
    }
    if (lister != null) lister.RFDI(miStart, "ADD", dstIndex, scale, dstDisp, imm);
  }

  // [dstDisp] +=  imm
  public final void emitADD_Abs_Imm(Offset dstDisp, int imm) {
    int miStart = mi;
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x0" is really part of the opcode
        emitAbsRegOperands(dstDisp, (byte) 0x0);
        emitImm8((byte)imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x0" is really part of the opcode
        emitAbsRegOperands(dstDisp, (byte) 0x0);
        emitImm32(imm);
    }
    if (lister != null) lister.RAI(miStart, "ADD", dstDisp, imm);
  }

  // [dstBase + dstIndex<<scale + dstDisp] +=  imm
  public final void emitADD_RegIdx_Imm(byte dstBase, byte dstIndex, short scale, Offset dstDisp, int imm) {
    int miStart = mi;
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x0" is really part of the opcode
        emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, (byte) 0x0);
        emitImm8((byte)imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x0" is really part of the opcode
        emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, (byte) 0x0);
        emitImm32(imm);
    }
    if (lister != null) lister.RXDI(miStart, "ADD", dstBase, dstIndex, scale, dstDisp, imm);
  }

  // [dstReg] +=  imm
  public final void emitADD_RegInd_Imm(byte dstReg, int imm) {
    int miStart = mi;
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x0" is really part of the opcode
        emitRegIndirectRegOperands(dstReg, (byte) 0x0);
        emitImm8((byte)imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x0" is really part of the opcode
        emitRegIndirectRegOperands(dstReg, (byte) 0x0);
        emitImm32(imm);
    }
    if (lister != null) lister.RNI(miStart, "ADD", dstReg, imm);
  }

  // dstReg +=  (word)  imm
  public final void emitADD_Reg_Imm_Word(byte dstReg, int imm) {
    int miStart = mi;
            setMachineCodes(mi++, (byte) 0x66);
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x0" is really part of the opcode
        emitRegRegOperands(dstReg, (byte) 0x0);
        emitImm8((byte)imm);
    } else if (dstReg == EAX) {
        setMachineCodes(mi++, (byte) 0x05);
        emitImm16(imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x0" is really part of the opcode
        emitRegRegOperands(dstReg, (byte) 0x0);
        emitImm16(imm);
    }
        if (lister != null) lister.RI(miStart, "ADD", dstReg, imm);
    }

  // [dstReg + dstDisp] +=  (word)  imm
  public final void emitADD_RegDisp_Imm_Word(byte dstReg, Offset disp, int imm) {
    int miStart = mi;
            setMachineCodes(mi++, (byte) 0x66);
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x0" is really part of the opcode
        emitRegDispRegOperands(dstReg, disp, (byte) 0x0);
        emitImm8((byte)imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x0" is really part of the opcode
        emitRegDispRegOperands(dstReg, disp, (byte) 0x0);
        emitImm16(imm);
    }
    if (lister != null) lister.RDI(miStart, "ADD", dstReg, disp, imm);
  }

  // [dstIndex<<scale + dstDisp] +=  (word)  imm
  public final void emitADD_RegOff_Imm_Word(byte dstIndex, short scale, Offset dstDisp, int imm) {
    int miStart = mi;
            setMachineCodes(mi++, (byte) 0x66);
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x0" is really part of the opcode
        emitRegOffRegOperands(dstIndex, scale, dstDisp, (byte) 0x0);
        emitImm8((byte)imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x0" is really part of the opcode
        emitRegOffRegOperands(dstIndex, scale, dstDisp, (byte) 0x0);
        emitImm32(imm);
    }
    if (lister != null) lister.RFDI(miStart, "ADD", dstIndex, scale, dstDisp, imm);
  }

  // [dstDisp] +=  (word)  imm
  public final void emitADD_Abs_Imm_Word(Offset dstDisp, int imm) {
    int miStart = mi;
            setMachineCodes(mi++, (byte) 0x66);
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x0" is really part of the opcode
        emitAbsRegOperands(dstDisp, (byte) 0x0);
        emitImm8((byte)imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x0" is really part of the opcode
        emitAbsRegOperands(dstDisp, (byte) 0x0);
        emitImm32(imm);
    }
    if (lister != null) lister.RAI(miStart, "ADD", dstDisp, imm);
  }

  // [dstBase + dstIndex<<scale + dstDisp] +=  (word)  imm
  public final void emitADD_RegIdx_Imm_Word(byte dstBase, byte dstIndex, short scale, Offset dstDisp, int imm) {
    int miStart = mi;
            setMachineCodes(mi++, (byte) 0x66);
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x0" is really part of the opcode
        emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, (byte) 0x0);
        emitImm8((byte)imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x0" is really part of the opcode
        emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, (byte) 0x0);
        emitImm32(imm);
    }
    if (lister != null) lister.RXDI(miStart, "ADD", dstBase, dstIndex, scale, dstDisp, imm);
  }

  // [dstReg] +=  (word)  imm
  public final void emitADD_RegInd_Imm_Word(byte dstReg, int imm) {
    int miStart = mi;
            setMachineCodes(mi++, (byte) 0x66);
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x0" is really part of the opcode
        emitRegIndirectRegOperands(dstReg, (byte) 0x0);
        emitImm8((byte)imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x0" is really part of the opcode
        emitRegIndirectRegOperands(dstReg, (byte) 0x0);
        emitImm16(imm);
    }
    if (lister != null) lister.RNI(miStart, "ADD", dstReg, imm);
  }

  // dstReg += (byte) imm
  public final void emitADD_Reg_Imm_Byte(byte dstReg, int imm) {
    int miStart = mi;
    if (dstReg == EAX) {
        setMachineCodes(mi++, (byte) 0x04);
        emitImm8(imm);
    } else {
        setMachineCodes(mi++, (byte) 0x80);
        // "register 0x0" is really part of the opcode
        emitRegRegOperands(dstReg, (byte) 0x0);
        emitImm8(imm);
    }
        if (lister != null) lister.RI(miStart, "ADD", dstReg, imm);
    }

  // [dstReg + dstDisp] += (byte) imm
  public final void emitADD_RegDisp_Imm_Byte(byte dstReg, Offset disp, int imm) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x80);
    // "register 0x0" is really part of the opcode
    emitRegDispRegOperands(dstReg, disp, (byte) 0x0);
    emitImm8(imm);
    if (lister != null) lister.RDI(miStart, "ADD", dstReg, disp, imm);
  }

  // [dstBase + dstIndex<<scale + dstDisp] += (byte) imm
  public final void emitADD_RegIdx_Imm_Byte(byte dstBase, byte dstIndex, short scale, Offset dstDisp, int imm) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x80);
    // "register 0x0" is really part of the opcode
    emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, (byte) 0x0);
    emitImm8(imm);
    if (lister != null) lister.RXDI(miStart, "ADD", dstBase, dstIndex, scale, dstDisp, imm);
  }

  // [dstIndex<<scale + dstDisp] += (byte) imm
  public final void emitADD_RegOff_Imm_Byte(byte dstIndex, short scale, Offset dstDisp, int imm) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x80);
    // "register 0x0" is really part of the opcode
    emitRegOffRegOperands(dstIndex, scale, dstDisp, (byte) 0x0);
    emitImm8(imm);
    if (lister != null) lister.RFDI(miStart, "ADD", dstIndex, scale, dstDisp, imm);
  }

  // [dstDisp] += (byte) imm
  public final void emitADD_Abs_Imm_Byte(Offset dstDisp, int imm) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x80);
    // "register 0x0" is really part of the opcode
    emitAbsRegOperands(dstDisp, (byte) 0x0);
    emitImm8(imm);
    if (lister != null) lister.RAI(miStart, "ADD", dstDisp, imm);
  }

  // [dstReg] += (byte) imm
  public final void emitADD_RegInd_Imm_Byte(byte dstReg, int imm) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x80);
    // "register 0x0" is really part of the opcode
    emitRegIndirectRegOperands(dstReg, (byte) 0x0);
    emitImm8(imm);
    if (lister != null) lister.RNI(miStart, "ADD", dstReg, imm);
  }

  /**
   * Generate a register--register AND. That is,
   * <PRE>
   * [dstReg] &=  srcReg
   * </PRE>
   *
   * @param dstReg the destination register
   * @param srcReg the source register
   */
  public final void emitAND_RegInd_Reg(byte dstReg, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x21);
    emitRegIndirectRegOperands(dstReg, srcReg);
    if (lister != null) lister.RNR(miStart, "AND", dstReg, srcReg);
  }

  /**
   * Generate a register-offset--register AND. That is,
   * <PRE>
   * [dstReg<<dstScale + dstDisp] &=  srcReg
   * </PRE>
   *
   * @param dstIndex the destination index register
   * @param dstScale the destination shift amount
   * @param dstDisp the destination displacement
   * @param srcReg the source register
   */
  public final void emitAND_RegOff_Reg(byte dstIndex, short dstScale, Offset dstDisp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x21);
    emitRegOffRegOperands(dstIndex, dstScale, dstDisp, srcReg);
    if (lister != null) lister.RFDR(miStart, "AND", dstIndex, dstScale, dstDisp, srcReg);
  }

  // [dstDisp] &=  srcReg
  public final void emitAND_Abs_Reg(Offset dstDisp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x21);
    emitAbsRegOperands(dstDisp, srcReg);
    if (lister != null) lister.RAR(miStart, "AND", dstDisp, srcReg);
  }

  // [dstBase + dstIndex<<scale + dstDisp] &=  srcReg
  public final void emitAND_RegIdx_Reg(byte dstBase, byte dstIndex, short scale, Offset dstDisp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x21);
    emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, srcReg);
    if (lister != null) lister.RXDR(miStart, "AND", dstBase, dstIndex, scale, dstDisp, srcReg);
  }

  // [dstReg + dstDisp] &=  srcReg
  public final void emitAND_RegDisp_Reg(byte dstReg, Offset disp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x21);
    emitRegDispRegOperands(dstReg, disp, srcReg);
    if (lister != null) lister.RDR(miStart, "AND", dstReg, disp, srcReg);
  }

  // dstReg &=  srcReg
  public final void emitAND_Reg_Reg(byte dstReg, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x21);
    emitRegRegOperands(dstReg, srcReg);
    if (lister != null) lister.RR(miStart, "AND", dstReg, srcReg);
  }

  // dstReg &=  [srcReg + srcDisp]
  public final void emitAND_Reg_RegDisp(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x23);
    emitRegDispRegOperands(srcReg, disp, dstReg);
    if (lister != null) lister.RRD(miStart, "AND", dstReg, srcReg, disp);
  }

  // dstReg &=  [srcIndex<<scale + srcDisp]
  public final void emitAND_Reg_RegOff(byte dstReg, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x23);
    emitRegOffRegOperands(srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRFD(miStart, "AND", dstReg, srcIndex, scale, srcDisp);
  }

  // dstReg &=  [srcDisp]
  public final void emitAND_Reg_Abs(byte dstReg, Offset srcDisp) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x23);
    emitAbsRegOperands(srcDisp, dstReg);
    if (lister != null) lister.RRA(miStart, "AND", dstReg, srcDisp);
  }

  // dstReg &=  [srcBase + srcIndex<<scale + srcDisp]
  public final void emitAND_Reg_RegIdx(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x23);
    emitSIBRegOperands(srcBase, srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRXD(miStart, "AND", dstReg, srcBase, srcIndex, scale, srcDisp);
  }

  // dstReg &=  [srcReg]
  public final void emitAND_Reg_RegInd(byte dstReg, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x23);
    emitRegIndirectRegOperands(srcReg, dstReg);
    if (lister != null) lister.RRN(miStart, "AND", dstReg, srcReg);
  }

  /**
   * Generate a register--register AND. That is,
   * <PRE>
   * [dstReg] &=  (word)  srcReg
   * </PRE>
   *
   * @param dstReg the destination register
   * @param srcReg the source register
   */
  public final void emitAND_RegInd_Reg_Word(byte dstReg, byte srcReg) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x21);
    emitRegIndirectRegOperands(dstReg, srcReg);
    if (lister != null) lister.RNR(miStart, "AND", dstReg, srcReg);
  }

  /**
   * Generate a register-offset--register AND. That is,
   * <PRE>
   * [dstReg<<dstScale + dstDisp] &=  (word)  srcReg
   * </PRE>
   *
   * @param dstIndex the destination index register
   * @param dstScale the destination shift amount
   * @param dstDisp the destination displacement
   * @param srcReg the source register
   */
  public final void emitAND_RegOff_Reg_Word(byte dstIndex, short dstScale, Offset dstDisp, byte srcReg) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x21);
    emitRegOffRegOperands(dstIndex, dstScale, dstDisp, srcReg);
    if (lister != null) lister.RFDR(miStart, "AND", dstIndex, dstScale, dstDisp, srcReg);
  }

  // [dstDisp] &=  (word)  srcReg
  public final void emitAND_Abs_Reg_Word(Offset dstDisp, byte srcReg) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x21);
    emitAbsRegOperands(dstDisp, srcReg);
    if (lister != null) lister.RAR(miStart, "AND", dstDisp, srcReg);
  }

  // [dstBase + dstIndex<<scale + dstDisp] &=  (word)  srcReg
  public final void emitAND_RegIdx_Reg_Word(byte dstBase, byte dstIndex, short scale, Offset dstDisp, byte srcReg) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x21);
    emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, srcReg);
    if (lister != null) lister.RXDR(miStart, "AND", dstBase, dstIndex, scale, dstDisp, srcReg);
  }

  // [dstReg + dstDisp] &=  (word)  srcReg
  public final void emitAND_RegDisp_Reg_Word(byte dstReg, Offset disp, byte srcReg) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x21);
    emitRegDispRegOperands(dstReg, disp, srcReg);
    if (lister != null) lister.RDR(miStart, "AND", dstReg, disp, srcReg);
  }

  // dstReg &=  (word)  srcReg
  public final void emitAND_Reg_Reg_Word(byte dstReg, byte srcReg) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x21);
    emitRegRegOperands(dstReg, srcReg);
    if (lister != null) lister.RR(miStart, "AND", dstReg, srcReg);
  }

  // dstReg &=  (word)  [srcReg + srcDisp]
  public final void emitAND_Reg_RegDisp_Word(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x23);
    emitRegDispRegOperands(srcReg, disp, dstReg);
    if (lister != null) lister.RRD(miStart, "AND", dstReg, srcReg, disp);
  }

  // dstReg &=  (word)  [srcIndex<<scale + srcDisp]
  public final void emitAND_Reg_RegOff_Word(byte dstReg, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x23);
    emitRegOffRegOperands(srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRFD(miStart, "AND", dstReg, srcIndex, scale, srcDisp);
  }

  // dstReg &=  (word)  [srcDisp]
  public final void emitAND_Reg_Abs_Word(byte dstReg, Offset srcDisp) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x23);
    emitAbsRegOperands(srcDisp, dstReg);
    if (lister != null) lister.RRA(miStart, "AND", dstReg, srcDisp);
  }

  // dstReg &=  (word)  [srcBase + srcIndex<<scale + srcDisp]
  public final void emitAND_Reg_RegIdx_Word(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x23);
    emitSIBRegOperands(srcBase, srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRXD(miStart, "AND", dstReg, srcBase, srcIndex, scale, srcDisp);
  }

  // dstReg &=  (word)  [srcReg]
  public final void emitAND_Reg_RegInd_Word(byte dstReg, byte srcReg) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x23);
    emitRegIndirectRegOperands(srcReg, dstReg);
    if (lister != null) lister.RRN(miStart, "AND", dstReg, srcReg);
  }

  /**
   * Generate a register--register AND. That is,
   * <PRE>
   * [dstReg] &=  (byte)  srcReg
   * </PRE>
   *
   * @param dstReg the destination register
   * @param srcReg the source register
   */
  public final void emitAND_RegInd_Reg_Byte(byte dstReg, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x20);
    emitRegIndirectRegOperands(dstReg, srcReg);
    if (lister != null) lister.RNR(miStart, "AND", dstReg, srcReg);
  }

  /**
   * Generate a register-offset--register AND. That is,
   * <PRE>
   * [dstReg<<dstScale + dstDisp] &=  (byte)  srcReg
   * </PRE>
   *
   * @param dstIndex the destination index register
   * @param dstScale the destination shift amount
   * @param dstDisp the destination displacement
   * @param srcReg the source register
   */
  public final void emitAND_RegOff_Reg_Byte(byte dstIndex, short dstScale, Offset dstDisp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x20);
    emitRegOffRegOperands(dstIndex, dstScale, dstDisp, srcReg);
    if (lister != null) lister.RFDR(miStart, "AND", dstIndex, dstScale, dstDisp, srcReg);
  }

  // [dstDisp] &=  (byte)  srcReg
  public final void emitAND_Abs_Reg_Byte(Offset dstDisp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x20);
    emitAbsRegOperands(dstDisp, srcReg);
    if (lister != null) lister.RAR(miStart, "AND", dstDisp, srcReg);
  }

  // [dstBase + dstIndex<<scale + dstDisp] &=  (byte)  srcReg
  public final void emitAND_RegIdx_Reg_Byte(byte dstBase, byte dstIndex, short scale, Offset dstDisp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x20);
    emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, srcReg);
    if (lister != null) lister.RXDR(miStart, "AND", dstBase, dstIndex, scale, dstDisp, srcReg);
  }

  // [dstReg + dstDisp] &=  (byte)  srcReg
  public final void emitAND_RegDisp_Reg_Byte(byte dstReg, Offset disp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x20);
    emitRegDispRegOperands(dstReg, disp, srcReg);
    if (lister != null) lister.RDR(miStart, "AND", dstReg, disp, srcReg);
  }

  // dstReg &=  (byte)  srcReg
  public final void emitAND_Reg_Reg_Byte(byte dstReg, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x20);
    emitRegRegOperands(dstReg, srcReg);
    if (lister != null) lister.RR(miStart, "AND", dstReg, srcReg);
  }

  // dstReg &=  (byte)  [srcReg + srcDisp]
  public final void emitAND_Reg_RegDisp_Byte(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x22);
    emitRegDispRegOperands(srcReg, disp, dstReg);
    if (lister != null) lister.RRD(miStart, "AND", dstReg, srcReg, disp);
  }

  // dstReg &=  (byte)  [srcIndex<<scale + srcDisp]
  public final void emitAND_Reg_RegOff_Byte(byte dstReg, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x22);
    emitRegOffRegOperands(srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRFD(miStart, "AND", dstReg, srcIndex, scale, srcDisp);
  }

  // dstReg &=  (byte)  [srcDisp]
  public final void emitAND_Reg_Abs_Byte(byte dstReg, Offset srcDisp) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x22);
    emitAbsRegOperands(srcDisp, dstReg);
    if (lister != null) lister.RRA(miStart, "AND", dstReg, srcDisp);
  }

  // dstReg &=  (byte)  [srcBase + srcIndex<<scale + srcDisp]
  public final void emitAND_Reg_RegIdx_Byte(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x22);
    emitSIBRegOperands(srcBase, srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRXD(miStart, "AND", dstReg, srcBase, srcIndex, scale, srcDisp);
  }

  // dstReg &=  (byte)  [srcReg]
  public final void emitAND_Reg_RegInd_Byte(byte dstReg, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x22);
    emitRegIndirectRegOperands(srcReg, dstReg);
    if (lister != null) lister.RRN(miStart, "AND", dstReg, srcReg);
  }

  // dstReg &=  imm
  public final void emitAND_Reg_Imm(byte dstReg, int imm) {
    int miStart = mi;
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x4" is really part of the opcode
        emitRegRegOperands(dstReg, (byte) 0x4);
        emitImm8((byte)imm);
    } else if (dstReg == EAX) {
        setMachineCodes(mi++, (byte) 0x25);
        emitImm32(imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x4" is really part of the opcode
        emitRegRegOperands(dstReg, (byte) 0x4);
        emitImm32(imm);
    }
        if (lister != null) lister.RI(miStart, "AND", dstReg, imm);
    }

  // [dstReg + dstDisp] &=  imm
  public final void emitAND_RegDisp_Imm(byte dstReg, Offset disp, int imm) {
    int miStart = mi;
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x4" is really part of the opcode
        emitRegDispRegOperands(dstReg, disp, (byte) 0x4);
        emitImm8((byte)imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x4" is really part of the opcode
        emitRegDispRegOperands(dstReg, disp, (byte) 0x4);
        emitImm32(imm);
    }
    if (lister != null) lister.RDI(miStart, "AND", dstReg, disp, imm);
  }

  // [dstIndex<<scale + dstDisp] &=  imm
  public final void emitAND_RegOff_Imm(byte dstIndex, short scale, Offset dstDisp, int imm) {
    int miStart = mi;
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x4" is really part of the opcode
        emitRegOffRegOperands(dstIndex, scale, dstDisp, (byte) 0x4);
        emitImm8((byte)imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x4" is really part of the opcode
        emitRegOffRegOperands(dstIndex, scale, dstDisp, (byte) 0x4);
        emitImm32(imm);
    }
    if (lister != null) lister.RFDI(miStart, "AND", dstIndex, scale, dstDisp, imm);
  }

  // [dstDisp] &=  imm
  public final void emitAND_Abs_Imm(Offset dstDisp, int imm) {
    int miStart = mi;
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x4" is really part of the opcode
        emitAbsRegOperands(dstDisp, (byte) 0x4);
        emitImm8((byte)imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x4" is really part of the opcode
        emitAbsRegOperands(dstDisp, (byte) 0x4);
        emitImm32(imm);
    }
    if (lister != null) lister.RAI(miStart, "AND", dstDisp, imm);
  }

  // [dstBase + dstIndex<<scale + dstDisp] &=  imm
  public final void emitAND_RegIdx_Imm(byte dstBase, byte dstIndex, short scale, Offset dstDisp, int imm) {
    int miStart = mi;
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x4" is really part of the opcode
        emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, (byte) 0x4);
        emitImm8((byte)imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x4" is really part of the opcode
        emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, (byte) 0x4);
        emitImm32(imm);
    }
    if (lister != null) lister.RXDI(miStart, "AND", dstBase, dstIndex, scale, dstDisp, imm);
  }

  // [dstReg] &=  imm
  public final void emitAND_RegInd_Imm(byte dstReg, int imm) {
    int miStart = mi;
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x4" is really part of the opcode
        emitRegIndirectRegOperands(dstReg, (byte) 0x4);
        emitImm8((byte)imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x4" is really part of the opcode
        emitRegIndirectRegOperands(dstReg, (byte) 0x4);
        emitImm32(imm);
    }
    if (lister != null) lister.RNI(miStart, "AND", dstReg, imm);
  }

  // dstReg &=  (word)  imm
  public final void emitAND_Reg_Imm_Word(byte dstReg, int imm) {
    int miStart = mi;
            setMachineCodes(mi++, (byte) 0x66);
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x4" is really part of the opcode
        emitRegRegOperands(dstReg, (byte) 0x4);
        emitImm8((byte)imm);
    } else if (dstReg == EAX) {
        setMachineCodes(mi++, (byte) 0x25);
        emitImm16(imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x4" is really part of the opcode
        emitRegRegOperands(dstReg, (byte) 0x4);
        emitImm16(imm);
    }
        if (lister != null) lister.RI(miStart, "AND", dstReg, imm);
    }

  // [dstReg + dstDisp] &=  (word)  imm
  public final void emitAND_RegDisp_Imm_Word(byte dstReg, Offset disp, int imm) {
    int miStart = mi;
            setMachineCodes(mi++, (byte) 0x66);
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x4" is really part of the opcode
        emitRegDispRegOperands(dstReg, disp, (byte) 0x4);
        emitImm8((byte)imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x4" is really part of the opcode
        emitRegDispRegOperands(dstReg, disp, (byte) 0x4);
        emitImm16(imm);
    }
    if (lister != null) lister.RDI(miStart, "AND", dstReg, disp, imm);
  }

  // [dstIndex<<scale + dstDisp] &=  (word)  imm
  public final void emitAND_RegOff_Imm_Word(byte dstIndex, short scale, Offset dstDisp, int imm) {
    int miStart = mi;
            setMachineCodes(mi++, (byte) 0x66);
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x4" is really part of the opcode
        emitRegOffRegOperands(dstIndex, scale, dstDisp, (byte) 0x4);
        emitImm8((byte)imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x4" is really part of the opcode
        emitRegOffRegOperands(dstIndex, scale, dstDisp, (byte) 0x4);
        emitImm32(imm);
    }
    if (lister != null) lister.RFDI(miStart, "AND", dstIndex, scale, dstDisp, imm);
  }

  // [dstDisp] &=  (word)  imm
  public final void emitAND_Abs_Imm_Word(Offset dstDisp, int imm) {
    int miStart = mi;
            setMachineCodes(mi++, (byte) 0x66);
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x4" is really part of the opcode
        emitAbsRegOperands(dstDisp, (byte) 0x4);
        emitImm8((byte)imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x4" is really part of the opcode
        emitAbsRegOperands(dstDisp, (byte) 0x4);
        emitImm32(imm);
    }
    if (lister != null) lister.RAI(miStart, "AND", dstDisp, imm);
  }

  // [dstBase + dstIndex<<scale + dstDisp] &=  (word)  imm
  public final void emitAND_RegIdx_Imm_Word(byte dstBase, byte dstIndex, short scale, Offset dstDisp, int imm) {
    int miStart = mi;
            setMachineCodes(mi++, (byte) 0x66);
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x4" is really part of the opcode
        emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, (byte) 0x4);
        emitImm8((byte)imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x4" is really part of the opcode
        emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, (byte) 0x4);
        emitImm32(imm);
    }
    if (lister != null) lister.RXDI(miStart, "AND", dstBase, dstIndex, scale, dstDisp, imm);
  }

  // [dstReg] &=  (word)  imm
  public final void emitAND_RegInd_Imm_Word(byte dstReg, int imm) {
    int miStart = mi;
            setMachineCodes(mi++, (byte) 0x66);
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x4" is really part of the opcode
        emitRegIndirectRegOperands(dstReg, (byte) 0x4);
        emitImm8((byte)imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x4" is really part of the opcode
        emitRegIndirectRegOperands(dstReg, (byte) 0x4);
        emitImm16(imm);
    }
    if (lister != null) lister.RNI(miStart, "AND", dstReg, imm);
  }

  // dstReg &= (byte) imm
  public final void emitAND_Reg_Imm_Byte(byte dstReg, int imm) {
    int miStart = mi;
    if (dstReg == EAX) {
        setMachineCodes(mi++, (byte) 0x24);
        emitImm8(imm);
    } else {
        setMachineCodes(mi++, (byte) 0x80);
        // "register 0x4" is really part of the opcode
        emitRegRegOperands(dstReg, (byte) 0x4);
        emitImm8(imm);
    }
        if (lister != null) lister.RI(miStart, "AND", dstReg, imm);
    }

  // [dstReg + dstDisp] &= (byte) imm
  public final void emitAND_RegDisp_Imm_Byte(byte dstReg, Offset disp, int imm) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x80);
    // "register 0x4" is really part of the opcode
    emitRegDispRegOperands(dstReg, disp, (byte) 0x4);
    emitImm8(imm);
    if (lister != null) lister.RDI(miStart, "AND", dstReg, disp, imm);
  }

  // [dstBase + dstIndex<<scale + dstDisp] &= (byte) imm
  public final void emitAND_RegIdx_Imm_Byte(byte dstBase, byte dstIndex, short scale, Offset dstDisp, int imm) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x80);
    // "register 0x4" is really part of the opcode
    emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, (byte) 0x4);
    emitImm8(imm);
    if (lister != null) lister.RXDI(miStart, "AND", dstBase, dstIndex, scale, dstDisp, imm);
  }

  // [dstIndex<<scale + dstDisp] &= (byte) imm
  public final void emitAND_RegOff_Imm_Byte(byte dstIndex, short scale, Offset dstDisp, int imm) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x80);
    // "register 0x4" is really part of the opcode
    emitRegOffRegOperands(dstIndex, scale, dstDisp, (byte) 0x4);
    emitImm8(imm);
    if (lister != null) lister.RFDI(miStart, "AND", dstIndex, scale, dstDisp, imm);
  }

  // [dstDisp] &= (byte) imm
  public final void emitAND_Abs_Imm_Byte(Offset dstDisp, int imm) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x80);
    // "register 0x4" is really part of the opcode
    emitAbsRegOperands(dstDisp, (byte) 0x4);
    emitImm8(imm);
    if (lister != null) lister.RAI(miStart, "AND", dstDisp, imm);
  }

  // [dstReg] &= (byte) imm
  public final void emitAND_RegInd_Imm_Byte(byte dstReg, int imm) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x80);
    // "register 0x4" is really part of the opcode
    emitRegIndirectRegOperands(dstReg, (byte) 0x4);
    emitImm8(imm);
    if (lister != null) lister.RNI(miStart, "AND", dstReg, imm);
  }

  /**
   * Generate a register--register CMP. That is,
   * <PRE>
   * [dstReg] ==  srcReg
   * </PRE>
   *
   * @param dstReg the destination register
   * @param srcReg the source register
   */
  public final void emitCMP_RegInd_Reg(byte dstReg, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x39);
    emitRegIndirectRegOperands(dstReg, srcReg);
    if (lister != null) lister.RNR(miStart, "CMP", dstReg, srcReg);
  }

  /**
   * Generate a register-offset--register CMP. That is,
   * <PRE>
   * [dstReg<<dstScale + dstDisp] ==  srcReg
   * </PRE>
   *
   * @param dstIndex the destination index register
   * @param dstScale the destination shift amount
   * @param dstDisp the destination displacement
   * @param srcReg the source register
   */
  public final void emitCMP_RegOff_Reg(byte dstIndex, short dstScale, Offset dstDisp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x39);
    emitRegOffRegOperands(dstIndex, dstScale, dstDisp, srcReg);
    if (lister != null) lister.RFDR(miStart, "CMP", dstIndex, dstScale, dstDisp, srcReg);
  }

  // [dstDisp] ==  srcReg
  public final void emitCMP_Abs_Reg(Offset dstDisp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x39);
    emitAbsRegOperands(dstDisp, srcReg);
    if (lister != null) lister.RAR(miStart, "CMP", dstDisp, srcReg);
  }

  // [dstBase + dstIndex<<scale + dstDisp] ==  srcReg
  public final void emitCMP_RegIdx_Reg(byte dstBase, byte dstIndex, short scale, Offset dstDisp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x39);
    emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, srcReg);
    if (lister != null) lister.RXDR(miStart, "CMP", dstBase, dstIndex, scale, dstDisp, srcReg);
  }

  // [dstReg + dstDisp] ==  srcReg
  public final void emitCMP_RegDisp_Reg(byte dstReg, Offset disp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x39);
    emitRegDispRegOperands(dstReg, disp, srcReg);
    if (lister != null) lister.RDR(miStart, "CMP", dstReg, disp, srcReg);
  }

  // dstReg ==  srcReg
  public final void emitCMP_Reg_Reg(byte dstReg, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x39);
    emitRegRegOperands(dstReg, srcReg);
    if (lister != null) lister.RR(miStart, "CMP", dstReg, srcReg);
  }

  // dstReg ==  [srcReg + srcDisp]
  public final void emitCMP_Reg_RegDisp(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x3B);
    emitRegDispRegOperands(srcReg, disp, dstReg);
    if (lister != null) lister.RRD(miStart, "CMP", dstReg, srcReg, disp);
  }

  // dstReg ==  [srcIndex<<scale + srcDisp]
  public final void emitCMP_Reg_RegOff(byte dstReg, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x3B);
    emitRegOffRegOperands(srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRFD(miStart, "CMP", dstReg, srcIndex, scale, srcDisp);
  }

  // dstReg ==  [srcDisp]
  public final void emitCMP_Reg_Abs(byte dstReg, Offset srcDisp) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x3B);
    emitAbsRegOperands(srcDisp, dstReg);
    if (lister != null) lister.RRA(miStart, "CMP", dstReg, srcDisp);
  }

  // dstReg ==  [srcBase + srcIndex<<scale + srcDisp]
  public final void emitCMP_Reg_RegIdx(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x3B);
    emitSIBRegOperands(srcBase, srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRXD(miStart, "CMP", dstReg, srcBase, srcIndex, scale, srcDisp);
  }

  // dstReg ==  [srcReg]
  public final void emitCMP_Reg_RegInd(byte dstReg, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x3B);
    emitRegIndirectRegOperands(srcReg, dstReg);
    if (lister != null) lister.RRN(miStart, "CMP", dstReg, srcReg);
  }

  /**
   * Generate a register--register CMP. That is,
   * <PRE>
   * [dstReg] ==  (word)  srcReg
   * </PRE>
   *
   * @param dstReg the destination register
   * @param srcReg the source register
   */
  public final void emitCMP_RegInd_Reg_Word(byte dstReg, byte srcReg) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x39);
    emitRegIndirectRegOperands(dstReg, srcReg);
    if (lister != null) lister.RNR(miStart, "CMP", dstReg, srcReg);
  }

  /**
   * Generate a register-offset--register CMP. That is,
   * <PRE>
   * [dstReg<<dstScale + dstDisp] ==  (word)  srcReg
   * </PRE>
   *
   * @param dstIndex the destination index register
   * @param dstScale the destination shift amount
   * @param dstDisp the destination displacement
   * @param srcReg the source register
   */
  public final void emitCMP_RegOff_Reg_Word(byte dstIndex, short dstScale, Offset dstDisp, byte srcReg) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x39);
    emitRegOffRegOperands(dstIndex, dstScale, dstDisp, srcReg);
    if (lister != null) lister.RFDR(miStart, "CMP", dstIndex, dstScale, dstDisp, srcReg);
  }

  // [dstDisp] ==  (word)  srcReg
  public final void emitCMP_Abs_Reg_Word(Offset dstDisp, byte srcReg) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x39);
    emitAbsRegOperands(dstDisp, srcReg);
    if (lister != null) lister.RAR(miStart, "CMP", dstDisp, srcReg);
  }

  // [dstBase + dstIndex<<scale + dstDisp] ==  (word)  srcReg
  public final void emitCMP_RegIdx_Reg_Word(byte dstBase, byte dstIndex, short scale, Offset dstDisp, byte srcReg) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x39);
    emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, srcReg);
    if (lister != null) lister.RXDR(miStart, "CMP", dstBase, dstIndex, scale, dstDisp, srcReg);
  }

  // [dstReg + dstDisp] ==  (word)  srcReg
  public final void emitCMP_RegDisp_Reg_Word(byte dstReg, Offset disp, byte srcReg) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x39);
    emitRegDispRegOperands(dstReg, disp, srcReg);
    if (lister != null) lister.RDR(miStart, "CMP", dstReg, disp, srcReg);
  }

  // dstReg ==  (word)  srcReg
  public final void emitCMP_Reg_Reg_Word(byte dstReg, byte srcReg) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x39);
    emitRegRegOperands(dstReg, srcReg);
    if (lister != null) lister.RR(miStart, "CMP", dstReg, srcReg);
  }

  // dstReg ==  (word)  [srcReg + srcDisp]
  public final void emitCMP_Reg_RegDisp_Word(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x3B);
    emitRegDispRegOperands(srcReg, disp, dstReg);
    if (lister != null) lister.RRD(miStart, "CMP", dstReg, srcReg, disp);
  }

  // dstReg ==  (word)  [srcIndex<<scale + srcDisp]
  public final void emitCMP_Reg_RegOff_Word(byte dstReg, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x3B);
    emitRegOffRegOperands(srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRFD(miStart, "CMP", dstReg, srcIndex, scale, srcDisp);
  }

  // dstReg ==  (word)  [srcDisp]
  public final void emitCMP_Reg_Abs_Word(byte dstReg, Offset srcDisp) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x3B);
    emitAbsRegOperands(srcDisp, dstReg);
    if (lister != null) lister.RRA(miStart, "CMP", dstReg, srcDisp);
  }

  // dstReg ==  (word)  [srcBase + srcIndex<<scale + srcDisp]
  public final void emitCMP_Reg_RegIdx_Word(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x3B);
    emitSIBRegOperands(srcBase, srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRXD(miStart, "CMP", dstReg, srcBase, srcIndex, scale, srcDisp);
  }

  // dstReg ==  (word)  [srcReg]
  public final void emitCMP_Reg_RegInd_Word(byte dstReg, byte srcReg) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x3B);
    emitRegIndirectRegOperands(srcReg, dstReg);
    if (lister != null) lister.RRN(miStart, "CMP", dstReg, srcReg);
  }

  /**
   * Generate a register--register CMP. That is,
   * <PRE>
   * [dstReg] ==  (byte)  srcReg
   * </PRE>
   *
   * @param dstReg the destination register
   * @param srcReg the source register
   */
  public final void emitCMP_RegInd_Reg_Byte(byte dstReg, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x38);
    emitRegIndirectRegOperands(dstReg, srcReg);
    if (lister != null) lister.RNR(miStart, "CMP", dstReg, srcReg);
  }

  /**
   * Generate a register-offset--register CMP. That is,
   * <PRE>
   * [dstReg<<dstScale + dstDisp] ==  (byte)  srcReg
   * </PRE>
   *
   * @param dstIndex the destination index register
   * @param dstScale the destination shift amount
   * @param dstDisp the destination displacement
   * @param srcReg the source register
   */
  public final void emitCMP_RegOff_Reg_Byte(byte dstIndex, short dstScale, Offset dstDisp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x38);
    emitRegOffRegOperands(dstIndex, dstScale, dstDisp, srcReg);
    if (lister != null) lister.RFDR(miStart, "CMP", dstIndex, dstScale, dstDisp, srcReg);
  }

  // [dstDisp] ==  (byte)  srcReg
  public final void emitCMP_Abs_Reg_Byte(Offset dstDisp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x38);
    emitAbsRegOperands(dstDisp, srcReg);
    if (lister != null) lister.RAR(miStart, "CMP", dstDisp, srcReg);
  }

  // [dstBase + dstIndex<<scale + dstDisp] ==  (byte)  srcReg
  public final void emitCMP_RegIdx_Reg_Byte(byte dstBase, byte dstIndex, short scale, Offset dstDisp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x38);
    emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, srcReg);
    if (lister != null) lister.RXDR(miStart, "CMP", dstBase, dstIndex, scale, dstDisp, srcReg);
  }

  // [dstReg + dstDisp] ==  (byte)  srcReg
  public final void emitCMP_RegDisp_Reg_Byte(byte dstReg, Offset disp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x38);
    emitRegDispRegOperands(dstReg, disp, srcReg);
    if (lister != null) lister.RDR(miStart, "CMP", dstReg, disp, srcReg);
  }

  // dstReg ==  (byte)  srcReg
  public final void emitCMP_Reg_Reg_Byte(byte dstReg, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x38);
    emitRegRegOperands(dstReg, srcReg);
    if (lister != null) lister.RR(miStart, "CMP", dstReg, srcReg);
  }

  // dstReg ==  (byte)  [srcReg + srcDisp]
  public final void emitCMP_Reg_RegDisp_Byte(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x3A);
    emitRegDispRegOperands(srcReg, disp, dstReg);
    if (lister != null) lister.RRD(miStart, "CMP", dstReg, srcReg, disp);
  }

  // dstReg ==  (byte)  [srcIndex<<scale + srcDisp]
  public final void emitCMP_Reg_RegOff_Byte(byte dstReg, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x3A);
    emitRegOffRegOperands(srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRFD(miStart, "CMP", dstReg, srcIndex, scale, srcDisp);
  }

  // dstReg ==  (byte)  [srcDisp]
  public final void emitCMP_Reg_Abs_Byte(byte dstReg, Offset srcDisp) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x3A);
    emitAbsRegOperands(srcDisp, dstReg);
    if (lister != null) lister.RRA(miStart, "CMP", dstReg, srcDisp);
  }

  // dstReg ==  (byte)  [srcBase + srcIndex<<scale + srcDisp]
  public final void emitCMP_Reg_RegIdx_Byte(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x3A);
    emitSIBRegOperands(srcBase, srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRXD(miStart, "CMP", dstReg, srcBase, srcIndex, scale, srcDisp);
  }

  // dstReg ==  (byte)  [srcReg]
  public final void emitCMP_Reg_RegInd_Byte(byte dstReg, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x3A);
    emitRegIndirectRegOperands(srcReg, dstReg);
    if (lister != null) lister.RRN(miStart, "CMP", dstReg, srcReg);
  }

  // dstReg ==  imm
  public final void emitCMP_Reg_Imm(byte dstReg, int imm) {
    int miStart = mi;
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x7" is really part of the opcode
        emitRegRegOperands(dstReg, (byte) 0x7);
        emitImm8((byte)imm);
    } else if (dstReg == EAX) {
        setMachineCodes(mi++, (byte) 0x3D);
        emitImm32(imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x7" is really part of the opcode
        emitRegRegOperands(dstReg, (byte) 0x7);
        emitImm32(imm);
    }
        if (lister != null) lister.RI(miStart, "CMP", dstReg, imm);
    }

  // [dstReg + dstDisp] ==  imm
  public final void emitCMP_RegDisp_Imm(byte dstReg, Offset disp, int imm) {
    int miStart = mi;
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x7" is really part of the opcode
        emitRegDispRegOperands(dstReg, disp, (byte) 0x7);
        emitImm8((byte)imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x7" is really part of the opcode
        emitRegDispRegOperands(dstReg, disp, (byte) 0x7);
        emitImm32(imm);
    }
    if (lister != null) lister.RDI(miStart, "CMP", dstReg, disp, imm);
  }

  // [dstIndex<<scale + dstDisp] ==  imm
  public final void emitCMP_RegOff_Imm(byte dstIndex, short scale, Offset dstDisp, int imm) {
    int miStart = mi;
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x7" is really part of the opcode
        emitRegOffRegOperands(dstIndex, scale, dstDisp, (byte) 0x7);
        emitImm8((byte)imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x7" is really part of the opcode
        emitRegOffRegOperands(dstIndex, scale, dstDisp, (byte) 0x7);
        emitImm32(imm);
    }
    if (lister != null) lister.RFDI(miStart, "CMP", dstIndex, scale, dstDisp, imm);
  }

  // [dstDisp] ==  imm
  public final void emitCMP_Abs_Imm(Offset dstDisp, int imm) {
    int miStart = mi;
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x7" is really part of the opcode
        emitAbsRegOperands(dstDisp, (byte) 0x7);
        emitImm8((byte)imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x7" is really part of the opcode
        emitAbsRegOperands(dstDisp, (byte) 0x7);
        emitImm32(imm);
    }
    if (lister != null) lister.RAI(miStart, "CMP", dstDisp, imm);
  }

  // [dstBase + dstIndex<<scale + dstDisp] ==  imm
  public final void emitCMP_RegIdx_Imm(byte dstBase, byte dstIndex, short scale, Offset dstDisp, int imm) {
    int miStart = mi;
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x7" is really part of the opcode
        emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, (byte) 0x7);
        emitImm8((byte)imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x7" is really part of the opcode
        emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, (byte) 0x7);
        emitImm32(imm);
    }
    if (lister != null) lister.RXDI(miStart, "CMP", dstBase, dstIndex, scale, dstDisp, imm);
  }

  // [dstReg] ==  imm
  public final void emitCMP_RegInd_Imm(byte dstReg, int imm) {
    int miStart = mi;
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x7" is really part of the opcode
        emitRegIndirectRegOperands(dstReg, (byte) 0x7);
        emitImm8((byte)imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x7" is really part of the opcode
        emitRegIndirectRegOperands(dstReg, (byte) 0x7);
        emitImm32(imm);
    }
    if (lister != null) lister.RNI(miStart, "CMP", dstReg, imm);
  }

  // dstReg ==  (word)  imm
  public final void emitCMP_Reg_Imm_Word(byte dstReg, int imm) {
    int miStart = mi;
            setMachineCodes(mi++, (byte) 0x66);
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x7" is really part of the opcode
        emitRegRegOperands(dstReg, (byte) 0x7);
        emitImm8((byte)imm);
    } else if (dstReg == EAX) {
        setMachineCodes(mi++, (byte) 0x3D);
        emitImm16(imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x7" is really part of the opcode
        emitRegRegOperands(dstReg, (byte) 0x7);
        emitImm16(imm);
    }
        if (lister != null) lister.RI(miStart, "CMP", dstReg, imm);
    }

  // [dstReg + dstDisp] ==  (word)  imm
  public final void emitCMP_RegDisp_Imm_Word(byte dstReg, Offset disp, int imm) {
    int miStart = mi;
            setMachineCodes(mi++, (byte) 0x66);
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x7" is really part of the opcode
        emitRegDispRegOperands(dstReg, disp, (byte) 0x7);
        emitImm8((byte)imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x7" is really part of the opcode
        emitRegDispRegOperands(dstReg, disp, (byte) 0x7);
        emitImm16(imm);
    }
    if (lister != null) lister.RDI(miStart, "CMP", dstReg, disp, imm);
  }

  // [dstIndex<<scale + dstDisp] ==  (word)  imm
  public final void emitCMP_RegOff_Imm_Word(byte dstIndex, short scale, Offset dstDisp, int imm) {
    int miStart = mi;
            setMachineCodes(mi++, (byte) 0x66);
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x7" is really part of the opcode
        emitRegOffRegOperands(dstIndex, scale, dstDisp, (byte) 0x7);
        emitImm8((byte)imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x7" is really part of the opcode
        emitRegOffRegOperands(dstIndex, scale, dstDisp, (byte) 0x7);
        emitImm32(imm);
    }
    if (lister != null) lister.RFDI(miStart, "CMP", dstIndex, scale, dstDisp, imm);
  }

  // [dstDisp] ==  (word)  imm
  public final void emitCMP_Abs_Imm_Word(Offset dstDisp, int imm) {
    int miStart = mi;
            setMachineCodes(mi++, (byte) 0x66);
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x7" is really part of the opcode
        emitAbsRegOperands(dstDisp, (byte) 0x7);
        emitImm8((byte)imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x7" is really part of the opcode
        emitAbsRegOperands(dstDisp, (byte) 0x7);
        emitImm32(imm);
    }
    if (lister != null) lister.RAI(miStart, "CMP", dstDisp, imm);
  }

  // [dstBase + dstIndex<<scale + dstDisp] ==  (word)  imm
  public final void emitCMP_RegIdx_Imm_Word(byte dstBase, byte dstIndex, short scale, Offset dstDisp, int imm) {
    int miStart = mi;
            setMachineCodes(mi++, (byte) 0x66);
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x7" is really part of the opcode
        emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, (byte) 0x7);
        emitImm8((byte)imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x7" is really part of the opcode
        emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, (byte) 0x7);
        emitImm32(imm);
    }
    if (lister != null) lister.RXDI(miStart, "CMP", dstBase, dstIndex, scale, dstDisp, imm);
  }

  // [dstReg] ==  (word)  imm
  public final void emitCMP_RegInd_Imm_Word(byte dstReg, int imm) {
    int miStart = mi;
            setMachineCodes(mi++, (byte) 0x66);
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x7" is really part of the opcode
        emitRegIndirectRegOperands(dstReg, (byte) 0x7);
        emitImm8((byte)imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x7" is really part of the opcode
        emitRegIndirectRegOperands(dstReg, (byte) 0x7);
        emitImm16(imm);
    }
    if (lister != null) lister.RNI(miStart, "CMP", dstReg, imm);
  }

  // dstReg == (byte) imm
  public final void emitCMP_Reg_Imm_Byte(byte dstReg, int imm) {
    int miStart = mi;
    if (dstReg == EAX) {
        setMachineCodes(mi++, (byte) 0x3C);
        emitImm8(imm);
    } else {
        setMachineCodes(mi++, (byte) 0x80);
        // "register 0x7" is really part of the opcode
        emitRegRegOperands(dstReg, (byte) 0x7);
        emitImm8(imm);
    }
        if (lister != null) lister.RI(miStart, "CMP", dstReg, imm);
    }

  // [dstReg + dstDisp] == (byte) imm
  public final void emitCMP_RegDisp_Imm_Byte(byte dstReg, Offset disp, int imm) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x80);
    // "register 0x7" is really part of the opcode
    emitRegDispRegOperands(dstReg, disp, (byte) 0x7);
    emitImm8(imm);
    if (lister != null) lister.RDI(miStart, "CMP", dstReg, disp, imm);
  }

  // [dstBase + dstIndex<<scale + dstDisp] == (byte) imm
  public final void emitCMP_RegIdx_Imm_Byte(byte dstBase, byte dstIndex, short scale, Offset dstDisp, int imm) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x80);
    // "register 0x7" is really part of the opcode
    emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, (byte) 0x7);
    emitImm8(imm);
    if (lister != null) lister.RXDI(miStart, "CMP", dstBase, dstIndex, scale, dstDisp, imm);
  }

  // [dstIndex<<scale + dstDisp] == (byte) imm
  public final void emitCMP_RegOff_Imm_Byte(byte dstIndex, short scale, Offset dstDisp, int imm) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x80);
    // "register 0x7" is really part of the opcode
    emitRegOffRegOperands(dstIndex, scale, dstDisp, (byte) 0x7);
    emitImm8(imm);
    if (lister != null) lister.RFDI(miStart, "CMP", dstIndex, scale, dstDisp, imm);
  }

  // [dstDisp] == (byte) imm
  public final void emitCMP_Abs_Imm_Byte(Offset dstDisp, int imm) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x80);
    // "register 0x7" is really part of the opcode
    emitAbsRegOperands(dstDisp, (byte) 0x7);
    emitImm8(imm);
    if (lister != null) lister.RAI(miStart, "CMP", dstDisp, imm);
  }

  // [dstReg] == (byte) imm
  public final void emitCMP_RegInd_Imm_Byte(byte dstReg, int imm) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x80);
    // "register 0x7" is really part of the opcode
    emitRegIndirectRegOperands(dstReg, (byte) 0x7);
    emitImm8(imm);
    if (lister != null) lister.RNI(miStart, "CMP", dstReg, imm);
  }

  /**
   * Generate a register--register OR. That is,
   * <PRE>
   * [dstReg] |=  srcReg
   * </PRE>
   *
   * @param dstReg the destination register
   * @param srcReg the source register
   */
  public final void emitOR_RegInd_Reg(byte dstReg, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x09);
    emitRegIndirectRegOperands(dstReg, srcReg);
    if (lister != null) lister.RNR(miStart, "OR", dstReg, srcReg);
  }

  /**
   * Generate a register-offset--register OR. That is,
   * <PRE>
   * [dstReg<<dstScale + dstDisp] |=  srcReg
   * </PRE>
   *
   * @param dstIndex the destination index register
   * @param dstScale the destination shift amount
   * @param dstDisp the destination displacement
   * @param srcReg the source register
   */
  public final void emitOR_RegOff_Reg(byte dstIndex, short dstScale, Offset dstDisp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x09);
    emitRegOffRegOperands(dstIndex, dstScale, dstDisp, srcReg);
    if (lister != null) lister.RFDR(miStart, "OR", dstIndex, dstScale, dstDisp, srcReg);
  }

  // [dstDisp] |=  srcReg
  public final void emitOR_Abs_Reg(Offset dstDisp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x09);
    emitAbsRegOperands(dstDisp, srcReg);
    if (lister != null) lister.RAR(miStart, "OR", dstDisp, srcReg);
  }

  // [dstBase + dstIndex<<scale + dstDisp] |=  srcReg
  public final void emitOR_RegIdx_Reg(byte dstBase, byte dstIndex, short scale, Offset dstDisp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x09);
    emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, srcReg);
    if (lister != null) lister.RXDR(miStart, "OR", dstBase, dstIndex, scale, dstDisp, srcReg);
  }

  // [dstReg + dstDisp] |=  srcReg
  public final void emitOR_RegDisp_Reg(byte dstReg, Offset disp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x09);
    emitRegDispRegOperands(dstReg, disp, srcReg);
    if (lister != null) lister.RDR(miStart, "OR", dstReg, disp, srcReg);
  }

  // dstReg |=  srcReg
  public final void emitOR_Reg_Reg(byte dstReg, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x09);
    emitRegRegOperands(dstReg, srcReg);
    if (lister != null) lister.RR(miStart, "OR", dstReg, srcReg);
  }

  // dstReg |=  [srcReg + srcDisp]
  public final void emitOR_Reg_RegDisp(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x0B);
    emitRegDispRegOperands(srcReg, disp, dstReg);
    if (lister != null) lister.RRD(miStart, "OR", dstReg, srcReg, disp);
  }

  // dstReg |=  [srcIndex<<scale + srcDisp]
  public final void emitOR_Reg_RegOff(byte dstReg, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x0B);
    emitRegOffRegOperands(srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRFD(miStart, "OR", dstReg, srcIndex, scale, srcDisp);
  }

  // dstReg |=  [srcDisp]
  public final void emitOR_Reg_Abs(byte dstReg, Offset srcDisp) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x0B);
    emitAbsRegOperands(srcDisp, dstReg);
    if (lister != null) lister.RRA(miStart, "OR", dstReg, srcDisp);
  }

  // dstReg |=  [srcBase + srcIndex<<scale + srcDisp]
  public final void emitOR_Reg_RegIdx(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x0B);
    emitSIBRegOperands(srcBase, srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRXD(miStart, "OR", dstReg, srcBase, srcIndex, scale, srcDisp);
  }

  // dstReg |=  [srcReg]
  public final void emitOR_Reg_RegInd(byte dstReg, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x0B);
    emitRegIndirectRegOperands(srcReg, dstReg);
    if (lister != null) lister.RRN(miStart, "OR", dstReg, srcReg);
  }

  /**
   * Generate a register--register OR. That is,
   * <PRE>
   * [dstReg] |=  (word)  srcReg
   * </PRE>
   *
   * @param dstReg the destination register
   * @param srcReg the source register
   */
  public final void emitOR_RegInd_Reg_Word(byte dstReg, byte srcReg) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x09);
    emitRegIndirectRegOperands(dstReg, srcReg);
    if (lister != null) lister.RNR(miStart, "OR", dstReg, srcReg);
  }

  /**
   * Generate a register-offset--register OR. That is,
   * <PRE>
   * [dstReg<<dstScale + dstDisp] |=  (word)  srcReg
   * </PRE>
   *
   * @param dstIndex the destination index register
   * @param dstScale the destination shift amount
   * @param dstDisp the destination displacement
   * @param srcReg the source register
   */
  public final void emitOR_RegOff_Reg_Word(byte dstIndex, short dstScale, Offset dstDisp, byte srcReg) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x09);
    emitRegOffRegOperands(dstIndex, dstScale, dstDisp, srcReg);
    if (lister != null) lister.RFDR(miStart, "OR", dstIndex, dstScale, dstDisp, srcReg);
  }

  // [dstDisp] |=  (word)  srcReg
  public final void emitOR_Abs_Reg_Word(Offset dstDisp, byte srcReg) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x09);
    emitAbsRegOperands(dstDisp, srcReg);
    if (lister != null) lister.RAR(miStart, "OR", dstDisp, srcReg);
  }

  // [dstBase + dstIndex<<scale + dstDisp] |=  (word)  srcReg
  public final void emitOR_RegIdx_Reg_Word(byte dstBase, byte dstIndex, short scale, Offset dstDisp, byte srcReg) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x09);
    emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, srcReg);
    if (lister != null) lister.RXDR(miStart, "OR", dstBase, dstIndex, scale, dstDisp, srcReg);
  }

  // [dstReg + dstDisp] |=  (word)  srcReg
  public final void emitOR_RegDisp_Reg_Word(byte dstReg, Offset disp, byte srcReg) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x09);
    emitRegDispRegOperands(dstReg, disp, srcReg);
    if (lister != null) lister.RDR(miStart, "OR", dstReg, disp, srcReg);
  }

  // dstReg |=  (word)  srcReg
  public final void emitOR_Reg_Reg_Word(byte dstReg, byte srcReg) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x09);
    emitRegRegOperands(dstReg, srcReg);
    if (lister != null) lister.RR(miStart, "OR", dstReg, srcReg);
  }

  // dstReg |=  (word)  [srcReg + srcDisp]
  public final void emitOR_Reg_RegDisp_Word(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x0B);
    emitRegDispRegOperands(srcReg, disp, dstReg);
    if (lister != null) lister.RRD(miStart, "OR", dstReg, srcReg, disp);
  }

  // dstReg |=  (word)  [srcIndex<<scale + srcDisp]
  public final void emitOR_Reg_RegOff_Word(byte dstReg, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x0B);
    emitRegOffRegOperands(srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRFD(miStart, "OR", dstReg, srcIndex, scale, srcDisp);
  }

  // dstReg |=  (word)  [srcDisp]
  public final void emitOR_Reg_Abs_Word(byte dstReg, Offset srcDisp) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x0B);
    emitAbsRegOperands(srcDisp, dstReg);
    if (lister != null) lister.RRA(miStart, "OR", dstReg, srcDisp);
  }

  // dstReg |=  (word)  [srcBase + srcIndex<<scale + srcDisp]
  public final void emitOR_Reg_RegIdx_Word(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x0B);
    emitSIBRegOperands(srcBase, srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRXD(miStart, "OR", dstReg, srcBase, srcIndex, scale, srcDisp);
  }

  // dstReg |=  (word)  [srcReg]
  public final void emitOR_Reg_RegInd_Word(byte dstReg, byte srcReg) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x0B);
    emitRegIndirectRegOperands(srcReg, dstReg);
    if (lister != null) lister.RRN(miStart, "OR", dstReg, srcReg);
  }

  /**
   * Generate a register--register OR. That is,
   * <PRE>
   * [dstReg] |=  (byte)  srcReg
   * </PRE>
   *
   * @param dstReg the destination register
   * @param srcReg the source register
   */
  public final void emitOR_RegInd_Reg_Byte(byte dstReg, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x08);
    emitRegIndirectRegOperands(dstReg, srcReg);
    if (lister != null) lister.RNR(miStart, "OR", dstReg, srcReg);
  }

  /**
   * Generate a register-offset--register OR. That is,
   * <PRE>
   * [dstReg<<dstScale + dstDisp] |=  (byte)  srcReg
   * </PRE>
   *
   * @param dstIndex the destination index register
   * @param dstScale the destination shift amount
   * @param dstDisp the destination displacement
   * @param srcReg the source register
   */
  public final void emitOR_RegOff_Reg_Byte(byte dstIndex, short dstScale, Offset dstDisp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x08);
    emitRegOffRegOperands(dstIndex, dstScale, dstDisp, srcReg);
    if (lister != null) lister.RFDR(miStart, "OR", dstIndex, dstScale, dstDisp, srcReg);
  }

  // [dstDisp] |=  (byte)  srcReg
  public final void emitOR_Abs_Reg_Byte(Offset dstDisp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x08);
    emitAbsRegOperands(dstDisp, srcReg);
    if (lister != null) lister.RAR(miStart, "OR", dstDisp, srcReg);
  }

  // [dstBase + dstIndex<<scale + dstDisp] |=  (byte)  srcReg
  public final void emitOR_RegIdx_Reg_Byte(byte dstBase, byte dstIndex, short scale, Offset dstDisp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x08);
    emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, srcReg);
    if (lister != null) lister.RXDR(miStart, "OR", dstBase, dstIndex, scale, dstDisp, srcReg);
  }

  // [dstReg + dstDisp] |=  (byte)  srcReg
  public final void emitOR_RegDisp_Reg_Byte(byte dstReg, Offset disp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x08);
    emitRegDispRegOperands(dstReg, disp, srcReg);
    if (lister != null) lister.RDR(miStart, "OR", dstReg, disp, srcReg);
  }

  // dstReg |=  (byte)  srcReg
  public final void emitOR_Reg_Reg_Byte(byte dstReg, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x08);
    emitRegRegOperands(dstReg, srcReg);
    if (lister != null) lister.RR(miStart, "OR", dstReg, srcReg);
  }

  // dstReg |=  (byte)  [srcReg + srcDisp]
  public final void emitOR_Reg_RegDisp_Byte(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x0A);
    emitRegDispRegOperands(srcReg, disp, dstReg);
    if (lister != null) lister.RRD(miStart, "OR", dstReg, srcReg, disp);
  }

  // dstReg |=  (byte)  [srcIndex<<scale + srcDisp]
  public final void emitOR_Reg_RegOff_Byte(byte dstReg, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x0A);
    emitRegOffRegOperands(srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRFD(miStart, "OR", dstReg, srcIndex, scale, srcDisp);
  }

  // dstReg |=  (byte)  [srcDisp]
  public final void emitOR_Reg_Abs_Byte(byte dstReg, Offset srcDisp) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x0A);
    emitAbsRegOperands(srcDisp, dstReg);
    if (lister != null) lister.RRA(miStart, "OR", dstReg, srcDisp);
  }

  // dstReg |=  (byte)  [srcBase + srcIndex<<scale + srcDisp]
  public final void emitOR_Reg_RegIdx_Byte(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x0A);
    emitSIBRegOperands(srcBase, srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRXD(miStart, "OR", dstReg, srcBase, srcIndex, scale, srcDisp);
  }

  // dstReg |=  (byte)  [srcReg]
  public final void emitOR_Reg_RegInd_Byte(byte dstReg, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x0A);
    emitRegIndirectRegOperands(srcReg, dstReg);
    if (lister != null) lister.RRN(miStart, "OR", dstReg, srcReg);
  }

  // dstReg |=  imm
  public final void emitOR_Reg_Imm(byte dstReg, int imm) {
    int miStart = mi;
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x1" is really part of the opcode
        emitRegRegOperands(dstReg, (byte) 0x1);
        emitImm8((byte)imm);
    } else if (dstReg == EAX) {
        setMachineCodes(mi++, (byte) 0x0D);
        emitImm32(imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x1" is really part of the opcode
        emitRegRegOperands(dstReg, (byte) 0x1);
        emitImm32(imm);
    }
        if (lister != null) lister.RI(miStart, "OR", dstReg, imm);
    }

  // [dstReg + dstDisp] |=  imm
  public final void emitOR_RegDisp_Imm(byte dstReg, Offset disp, int imm) {
    int miStart = mi;
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x1" is really part of the opcode
        emitRegDispRegOperands(dstReg, disp, (byte) 0x1);
        emitImm8((byte)imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x1" is really part of the opcode
        emitRegDispRegOperands(dstReg, disp, (byte) 0x1);
        emitImm32(imm);
    }
    if (lister != null) lister.RDI(miStart, "OR", dstReg, disp, imm);
  }

  // [dstIndex<<scale + dstDisp] |=  imm
  public final void emitOR_RegOff_Imm(byte dstIndex, short scale, Offset dstDisp, int imm) {
    int miStart = mi;
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x1" is really part of the opcode
        emitRegOffRegOperands(dstIndex, scale, dstDisp, (byte) 0x1);
        emitImm8((byte)imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x1" is really part of the opcode
        emitRegOffRegOperands(dstIndex, scale, dstDisp, (byte) 0x1);
        emitImm32(imm);
    }
    if (lister != null) lister.RFDI(miStart, "OR", dstIndex, scale, dstDisp, imm);
  }

  // [dstDisp] |=  imm
  public final void emitOR_Abs_Imm(Offset dstDisp, int imm) {
    int miStart = mi;
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x1" is really part of the opcode
        emitAbsRegOperands(dstDisp, (byte) 0x1);
        emitImm8((byte)imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x1" is really part of the opcode
        emitAbsRegOperands(dstDisp, (byte) 0x1);
        emitImm32(imm);
    }
    if (lister != null) lister.RAI(miStart, "OR", dstDisp, imm);
  }

  // [dstBase + dstIndex<<scale + dstDisp] |=  imm
  public final void emitOR_RegIdx_Imm(byte dstBase, byte dstIndex, short scale, Offset dstDisp, int imm) {
    int miStart = mi;
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x1" is really part of the opcode
        emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, (byte) 0x1);
        emitImm8((byte)imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x1" is really part of the opcode
        emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, (byte) 0x1);
        emitImm32(imm);
    }
    if (lister != null) lister.RXDI(miStart, "OR", dstBase, dstIndex, scale, dstDisp, imm);
  }

  // [dstReg] |=  imm
  public final void emitOR_RegInd_Imm(byte dstReg, int imm) {
    int miStart = mi;
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x1" is really part of the opcode
        emitRegIndirectRegOperands(dstReg, (byte) 0x1);
        emitImm8((byte)imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x1" is really part of the opcode
        emitRegIndirectRegOperands(dstReg, (byte) 0x1);
        emitImm32(imm);
    }
    if (lister != null) lister.RNI(miStart, "OR", dstReg, imm);
  }

  // dstReg |=  (word)  imm
  public final void emitOR_Reg_Imm_Word(byte dstReg, int imm) {
    int miStart = mi;
            setMachineCodes(mi++, (byte) 0x66);
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x1" is really part of the opcode
        emitRegRegOperands(dstReg, (byte) 0x1);
        emitImm8((byte)imm);
    } else if (dstReg == EAX) {
        setMachineCodes(mi++, (byte) 0x0D);
        emitImm16(imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x1" is really part of the opcode
        emitRegRegOperands(dstReg, (byte) 0x1);
        emitImm16(imm);
    }
        if (lister != null) lister.RI(miStart, "OR", dstReg, imm);
    }

  // [dstReg + dstDisp] |=  (word)  imm
  public final void emitOR_RegDisp_Imm_Word(byte dstReg, Offset disp, int imm) {
    int miStart = mi;
            setMachineCodes(mi++, (byte) 0x66);
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x1" is really part of the opcode
        emitRegDispRegOperands(dstReg, disp, (byte) 0x1);
        emitImm8((byte)imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x1" is really part of the opcode
        emitRegDispRegOperands(dstReg, disp, (byte) 0x1);
        emitImm16(imm);
    }
    if (lister != null) lister.RDI(miStart, "OR", dstReg, disp, imm);
  }

  // [dstIndex<<scale + dstDisp] |=  (word)  imm
  public final void emitOR_RegOff_Imm_Word(byte dstIndex, short scale, Offset dstDisp, int imm) {
    int miStart = mi;
            setMachineCodes(mi++, (byte) 0x66);
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x1" is really part of the opcode
        emitRegOffRegOperands(dstIndex, scale, dstDisp, (byte) 0x1);
        emitImm8((byte)imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x1" is really part of the opcode
        emitRegOffRegOperands(dstIndex, scale, dstDisp, (byte) 0x1);
        emitImm32(imm);
    }
    if (lister != null) lister.RFDI(miStart, "OR", dstIndex, scale, dstDisp, imm);
  }

  // [dstDisp] |=  (word)  imm
  public final void emitOR_Abs_Imm_Word(Offset dstDisp, int imm) {
    int miStart = mi;
            setMachineCodes(mi++, (byte) 0x66);
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x1" is really part of the opcode
        emitAbsRegOperands(dstDisp, (byte) 0x1);
        emitImm8((byte)imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x1" is really part of the opcode
        emitAbsRegOperands(dstDisp, (byte) 0x1);
        emitImm32(imm);
    }
    if (lister != null) lister.RAI(miStart, "OR", dstDisp, imm);
  }

  // [dstBase + dstIndex<<scale + dstDisp] |=  (word)  imm
  public final void emitOR_RegIdx_Imm_Word(byte dstBase, byte dstIndex, short scale, Offset dstDisp, int imm) {
    int miStart = mi;
            setMachineCodes(mi++, (byte) 0x66);
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x1" is really part of the opcode
        emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, (byte) 0x1);
        emitImm8((byte)imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x1" is really part of the opcode
        emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, (byte) 0x1);
        emitImm32(imm);
    }
    if (lister != null) lister.RXDI(miStart, "OR", dstBase, dstIndex, scale, dstDisp, imm);
  }

  // [dstReg] |=  (word)  imm
  public final void emitOR_RegInd_Imm_Word(byte dstReg, int imm) {
    int miStart = mi;
            setMachineCodes(mi++, (byte) 0x66);
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x1" is really part of the opcode
        emitRegIndirectRegOperands(dstReg, (byte) 0x1);
        emitImm8((byte)imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x1" is really part of the opcode
        emitRegIndirectRegOperands(dstReg, (byte) 0x1);
        emitImm16(imm);
    }
    if (lister != null) lister.RNI(miStart, "OR", dstReg, imm);
  }

  // dstReg |= (byte) imm
  public final void emitOR_Reg_Imm_Byte(byte dstReg, int imm) {
    int miStart = mi;
    if (dstReg == EAX) {
        setMachineCodes(mi++, (byte) 0x0C);
        emitImm8(imm);
    } else {
        setMachineCodes(mi++, (byte) 0x80);
        // "register 0x1" is really part of the opcode
        emitRegRegOperands(dstReg, (byte) 0x1);
        emitImm8(imm);
    }
        if (lister != null) lister.RI(miStart, "OR", dstReg, imm);
    }

  // [dstReg + dstDisp] |= (byte) imm
  public final void emitOR_RegDisp_Imm_Byte(byte dstReg, Offset disp, int imm) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x80);
    // "register 0x1" is really part of the opcode
    emitRegDispRegOperands(dstReg, disp, (byte) 0x1);
    emitImm8(imm);
    if (lister != null) lister.RDI(miStart, "OR", dstReg, disp, imm);
  }

  // [dstBase + dstIndex<<scale + dstDisp] |= (byte) imm
  public final void emitOR_RegIdx_Imm_Byte(byte dstBase, byte dstIndex, short scale, Offset dstDisp, int imm) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x80);
    // "register 0x1" is really part of the opcode
    emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, (byte) 0x1);
    emitImm8(imm);
    if (lister != null) lister.RXDI(miStart, "OR", dstBase, dstIndex, scale, dstDisp, imm);
  }

  // [dstIndex<<scale + dstDisp] |= (byte) imm
  public final void emitOR_RegOff_Imm_Byte(byte dstIndex, short scale, Offset dstDisp, int imm) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x80);
    // "register 0x1" is really part of the opcode
    emitRegOffRegOperands(dstIndex, scale, dstDisp, (byte) 0x1);
    emitImm8(imm);
    if (lister != null) lister.RFDI(miStart, "OR", dstIndex, scale, dstDisp, imm);
  }

  // [dstDisp] |= (byte) imm
  public final void emitOR_Abs_Imm_Byte(Offset dstDisp, int imm) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x80);
    // "register 0x1" is really part of the opcode
    emitAbsRegOperands(dstDisp, (byte) 0x1);
    emitImm8(imm);
    if (lister != null) lister.RAI(miStart, "OR", dstDisp, imm);
  }

  // [dstReg] |= (byte) imm
  public final void emitOR_RegInd_Imm_Byte(byte dstReg, int imm) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x80);
    // "register 0x1" is really part of the opcode
    emitRegIndirectRegOperands(dstReg, (byte) 0x1);
    emitImm8(imm);
    if (lister != null) lister.RNI(miStart, "OR", dstReg, imm);
  }

  /**
   * Generate a register--register SBB. That is,
   * <PRE>
   * [dstReg] -CF=  srcReg
   * </PRE>
   *
   * @param dstReg the destination register
   * @param srcReg the source register
   */
  public final void emitSBB_RegInd_Reg(byte dstReg, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x19);
    emitRegIndirectRegOperands(dstReg, srcReg);
    if (lister != null) lister.RNR(miStart, "SBB", dstReg, srcReg);
  }

  /**
   * Generate a register-offset--register SBB. That is,
   * <PRE>
   * [dstReg<<dstScale + dstDisp] -CF=  srcReg
   * </PRE>
   *
   * @param dstIndex the destination index register
   * @param dstScale the destination shift amount
   * @param dstDisp the destination displacement
   * @param srcReg the source register
   */
  public final void emitSBB_RegOff_Reg(byte dstIndex, short dstScale, Offset dstDisp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x19);
    emitRegOffRegOperands(dstIndex, dstScale, dstDisp, srcReg);
    if (lister != null) lister.RFDR(miStart, "SBB", dstIndex, dstScale, dstDisp, srcReg);
  }

  // [dstDisp] -CF=  srcReg
  public final void emitSBB_Abs_Reg(Offset dstDisp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x19);
    emitAbsRegOperands(dstDisp, srcReg);
    if (lister != null) lister.RAR(miStart, "SBB", dstDisp, srcReg);
  }

  // [dstBase + dstIndex<<scale + dstDisp] -CF=  srcReg
  public final void emitSBB_RegIdx_Reg(byte dstBase, byte dstIndex, short scale, Offset dstDisp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x19);
    emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, srcReg);
    if (lister != null) lister.RXDR(miStart, "SBB", dstBase, dstIndex, scale, dstDisp, srcReg);
  }

  // [dstReg + dstDisp] -CF=  srcReg
  public final void emitSBB_RegDisp_Reg(byte dstReg, Offset disp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x19);
    emitRegDispRegOperands(dstReg, disp, srcReg);
    if (lister != null) lister.RDR(miStart, "SBB", dstReg, disp, srcReg);
  }

  // dstReg -CF=  srcReg
  public final void emitSBB_Reg_Reg(byte dstReg, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x19);
    emitRegRegOperands(dstReg, srcReg);
    if (lister != null) lister.RR(miStart, "SBB", dstReg, srcReg);
  }

  // dstReg -CF=  [srcReg + srcDisp]
  public final void emitSBB_Reg_RegDisp(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x1B);
    emitRegDispRegOperands(srcReg, disp, dstReg);
    if (lister != null) lister.RRD(miStart, "SBB", dstReg, srcReg, disp);
  }

  // dstReg -CF=  [srcIndex<<scale + srcDisp]
  public final void emitSBB_Reg_RegOff(byte dstReg, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x1B);
    emitRegOffRegOperands(srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRFD(miStart, "SBB", dstReg, srcIndex, scale, srcDisp);
  }

  // dstReg -CF=  [srcDisp]
  public final void emitSBB_Reg_Abs(byte dstReg, Offset srcDisp) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x1B);
    emitAbsRegOperands(srcDisp, dstReg);
    if (lister != null) lister.RRA(miStart, "SBB", dstReg, srcDisp);
  }

  // dstReg -CF=  [srcBase + srcIndex<<scale + srcDisp]
  public final void emitSBB_Reg_RegIdx(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x1B);
    emitSIBRegOperands(srcBase, srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRXD(miStart, "SBB", dstReg, srcBase, srcIndex, scale, srcDisp);
  }

  // dstReg -CF=  [srcReg]
  public final void emitSBB_Reg_RegInd(byte dstReg, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x1B);
    emitRegIndirectRegOperands(srcReg, dstReg);
    if (lister != null) lister.RRN(miStart, "SBB", dstReg, srcReg);
  }

  /**
   * Generate a register--register SBB. That is,
   * <PRE>
   * [dstReg] -CF=  (word)  srcReg
   * </PRE>
   *
   * @param dstReg the destination register
   * @param srcReg the source register
   */
  public final void emitSBB_RegInd_Reg_Word(byte dstReg, byte srcReg) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x19);
    emitRegIndirectRegOperands(dstReg, srcReg);
    if (lister != null) lister.RNR(miStart, "SBB", dstReg, srcReg);
  }

  /**
   * Generate a register-offset--register SBB. That is,
   * <PRE>
   * [dstReg<<dstScale + dstDisp] -CF=  (word)  srcReg
   * </PRE>
   *
   * @param dstIndex the destination index register
   * @param dstScale the destination shift amount
   * @param dstDisp the destination displacement
   * @param srcReg the source register
   */
  public final void emitSBB_RegOff_Reg_Word(byte dstIndex, short dstScale, Offset dstDisp, byte srcReg) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x19);
    emitRegOffRegOperands(dstIndex, dstScale, dstDisp, srcReg);
    if (lister != null) lister.RFDR(miStart, "SBB", dstIndex, dstScale, dstDisp, srcReg);
  }

  // [dstDisp] -CF=  (word)  srcReg
  public final void emitSBB_Abs_Reg_Word(Offset dstDisp, byte srcReg) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x19);
    emitAbsRegOperands(dstDisp, srcReg);
    if (lister != null) lister.RAR(miStart, "SBB", dstDisp, srcReg);
  }

  // [dstBase + dstIndex<<scale + dstDisp] -CF=  (word)  srcReg
  public final void emitSBB_RegIdx_Reg_Word(byte dstBase, byte dstIndex, short scale, Offset dstDisp, byte srcReg) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x19);
    emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, srcReg);
    if (lister != null) lister.RXDR(miStart, "SBB", dstBase, dstIndex, scale, dstDisp, srcReg);
  }

  // [dstReg + dstDisp] -CF=  (word)  srcReg
  public final void emitSBB_RegDisp_Reg_Word(byte dstReg, Offset disp, byte srcReg) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x19);
    emitRegDispRegOperands(dstReg, disp, srcReg);
    if (lister != null) lister.RDR(miStart, "SBB", dstReg, disp, srcReg);
  }

  // dstReg -CF=  (word)  srcReg
  public final void emitSBB_Reg_Reg_Word(byte dstReg, byte srcReg) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x19);
    emitRegRegOperands(dstReg, srcReg);
    if (lister != null) lister.RR(miStart, "SBB", dstReg, srcReg);
  }

  // dstReg -CF=  (word)  [srcReg + srcDisp]
  public final void emitSBB_Reg_RegDisp_Word(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x1B);
    emitRegDispRegOperands(srcReg, disp, dstReg);
    if (lister != null) lister.RRD(miStart, "SBB", dstReg, srcReg, disp);
  }

  // dstReg -CF=  (word)  [srcIndex<<scale + srcDisp]
  public final void emitSBB_Reg_RegOff_Word(byte dstReg, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x1B);
    emitRegOffRegOperands(srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRFD(miStart, "SBB", dstReg, srcIndex, scale, srcDisp);
  }

  // dstReg -CF=  (word)  [srcDisp]
  public final void emitSBB_Reg_Abs_Word(byte dstReg, Offset srcDisp) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x1B);
    emitAbsRegOperands(srcDisp, dstReg);
    if (lister != null) lister.RRA(miStart, "SBB", dstReg, srcDisp);
  }

  // dstReg -CF=  (word)  [srcBase + srcIndex<<scale + srcDisp]
  public final void emitSBB_Reg_RegIdx_Word(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x1B);
    emitSIBRegOperands(srcBase, srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRXD(miStart, "SBB", dstReg, srcBase, srcIndex, scale, srcDisp);
  }

  // dstReg -CF=  (word)  [srcReg]
  public final void emitSBB_Reg_RegInd_Word(byte dstReg, byte srcReg) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x1B);
    emitRegIndirectRegOperands(srcReg, dstReg);
    if (lister != null) lister.RRN(miStart, "SBB", dstReg, srcReg);
  }

  /**
   * Generate a register--register SBB. That is,
   * <PRE>
   * [dstReg] -CF=  (byte)  srcReg
   * </PRE>
   *
   * @param dstReg the destination register
   * @param srcReg the source register
   */
  public final void emitSBB_RegInd_Reg_Byte(byte dstReg, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x18);
    emitRegIndirectRegOperands(dstReg, srcReg);
    if (lister != null) lister.RNR(miStart, "SBB", dstReg, srcReg);
  }

  /**
   * Generate a register-offset--register SBB. That is,
   * <PRE>
   * [dstReg<<dstScale + dstDisp] -CF=  (byte)  srcReg
   * </PRE>
   *
   * @param dstIndex the destination index register
   * @param dstScale the destination shift amount
   * @param dstDisp the destination displacement
   * @param srcReg the source register
   */
  public final void emitSBB_RegOff_Reg_Byte(byte dstIndex, short dstScale, Offset dstDisp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x18);
    emitRegOffRegOperands(dstIndex, dstScale, dstDisp, srcReg);
    if (lister != null) lister.RFDR(miStart, "SBB", dstIndex, dstScale, dstDisp, srcReg);
  }

  // [dstDisp] -CF=  (byte)  srcReg
  public final void emitSBB_Abs_Reg_Byte(Offset dstDisp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x18);
    emitAbsRegOperands(dstDisp, srcReg);
    if (lister != null) lister.RAR(miStart, "SBB", dstDisp, srcReg);
  }

  // [dstBase + dstIndex<<scale + dstDisp] -CF=  (byte)  srcReg
  public final void emitSBB_RegIdx_Reg_Byte(byte dstBase, byte dstIndex, short scale, Offset dstDisp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x18);
    emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, srcReg);
    if (lister != null) lister.RXDR(miStart, "SBB", dstBase, dstIndex, scale, dstDisp, srcReg);
  }

  // [dstReg + dstDisp] -CF=  (byte)  srcReg
  public final void emitSBB_RegDisp_Reg_Byte(byte dstReg, Offset disp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x18);
    emitRegDispRegOperands(dstReg, disp, srcReg);
    if (lister != null) lister.RDR(miStart, "SBB", dstReg, disp, srcReg);
  }

  // dstReg -CF=  (byte)  srcReg
  public final void emitSBB_Reg_Reg_Byte(byte dstReg, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x18);
    emitRegRegOperands(dstReg, srcReg);
    if (lister != null) lister.RR(miStart, "SBB", dstReg, srcReg);
  }

  // dstReg -CF=  (byte)  [srcReg + srcDisp]
  public final void emitSBB_Reg_RegDisp_Byte(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x1A);
    emitRegDispRegOperands(srcReg, disp, dstReg);
    if (lister != null) lister.RRD(miStart, "SBB", dstReg, srcReg, disp);
  }

  // dstReg -CF=  (byte)  [srcIndex<<scale + srcDisp]
  public final void emitSBB_Reg_RegOff_Byte(byte dstReg, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x1A);
    emitRegOffRegOperands(srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRFD(miStart, "SBB", dstReg, srcIndex, scale, srcDisp);
  }

  // dstReg -CF=  (byte)  [srcDisp]
  public final void emitSBB_Reg_Abs_Byte(byte dstReg, Offset srcDisp) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x1A);
    emitAbsRegOperands(srcDisp, dstReg);
    if (lister != null) lister.RRA(miStart, "SBB", dstReg, srcDisp);
  }

  // dstReg -CF=  (byte)  [srcBase + srcIndex<<scale + srcDisp]
  public final void emitSBB_Reg_RegIdx_Byte(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x1A);
    emitSIBRegOperands(srcBase, srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRXD(miStart, "SBB", dstReg, srcBase, srcIndex, scale, srcDisp);
  }

  // dstReg -CF=  (byte)  [srcReg]
  public final void emitSBB_Reg_RegInd_Byte(byte dstReg, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x1A);
    emitRegIndirectRegOperands(srcReg, dstReg);
    if (lister != null) lister.RRN(miStart, "SBB", dstReg, srcReg);
  }

  // dstReg -CF=  imm
  public final void emitSBB_Reg_Imm(byte dstReg, int imm) {
    int miStart = mi;
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x3" is really part of the opcode
        emitRegRegOperands(dstReg, (byte) 0x3);
        emitImm8((byte)imm);
    } else if (dstReg == EAX) {
        setMachineCodes(mi++, (byte) 0x1D);
        emitImm32(imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x3" is really part of the opcode
        emitRegRegOperands(dstReg, (byte) 0x3);
        emitImm32(imm);
    }
        if (lister != null) lister.RI(miStart, "SBB", dstReg, imm);
    }

  // [dstReg + dstDisp] -CF=  imm
  public final void emitSBB_RegDisp_Imm(byte dstReg, Offset disp, int imm) {
    int miStart = mi;
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x3" is really part of the opcode
        emitRegDispRegOperands(dstReg, disp, (byte) 0x3);
        emitImm8((byte)imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x3" is really part of the opcode
        emitRegDispRegOperands(dstReg, disp, (byte) 0x3);
        emitImm32(imm);
    }
    if (lister != null) lister.RDI(miStart, "SBB", dstReg, disp, imm);
  }

  // [dstIndex<<scale + dstDisp] -CF=  imm
  public final void emitSBB_RegOff_Imm(byte dstIndex, short scale, Offset dstDisp, int imm) {
    int miStart = mi;
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x3" is really part of the opcode
        emitRegOffRegOperands(dstIndex, scale, dstDisp, (byte) 0x3);
        emitImm8((byte)imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x3" is really part of the opcode
        emitRegOffRegOperands(dstIndex, scale, dstDisp, (byte) 0x3);
        emitImm32(imm);
    }
    if (lister != null) lister.RFDI(miStart, "SBB", dstIndex, scale, dstDisp, imm);
  }

  // [dstDisp] -CF=  imm
  public final void emitSBB_Abs_Imm(Offset dstDisp, int imm) {
    int miStart = mi;
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x3" is really part of the opcode
        emitAbsRegOperands(dstDisp, (byte) 0x3);
        emitImm8((byte)imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x3" is really part of the opcode
        emitAbsRegOperands(dstDisp, (byte) 0x3);
        emitImm32(imm);
    }
    if (lister != null) lister.RAI(miStart, "SBB", dstDisp, imm);
  }

  // [dstBase + dstIndex<<scale + dstDisp] -CF=  imm
  public final void emitSBB_RegIdx_Imm(byte dstBase, byte dstIndex, short scale, Offset dstDisp, int imm) {
    int miStart = mi;
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x3" is really part of the opcode
        emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, (byte) 0x3);
        emitImm8((byte)imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x3" is really part of the opcode
        emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, (byte) 0x3);
        emitImm32(imm);
    }
    if (lister != null) lister.RXDI(miStart, "SBB", dstBase, dstIndex, scale, dstDisp, imm);
  }

  // [dstReg] -CF=  imm
  public final void emitSBB_RegInd_Imm(byte dstReg, int imm) {
    int miStart = mi;
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x3" is really part of the opcode
        emitRegIndirectRegOperands(dstReg, (byte) 0x3);
        emitImm8((byte)imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x3" is really part of the opcode
        emitRegIndirectRegOperands(dstReg, (byte) 0x3);
        emitImm32(imm);
    }
    if (lister != null) lister.RNI(miStart, "SBB", dstReg, imm);
  }

  // dstReg -CF=  (word)  imm
  public final void emitSBB_Reg_Imm_Word(byte dstReg, int imm) {
    int miStart = mi;
            setMachineCodes(mi++, (byte) 0x66);
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x3" is really part of the opcode
        emitRegRegOperands(dstReg, (byte) 0x3);
        emitImm8((byte)imm);
    } else if (dstReg == EAX) {
        setMachineCodes(mi++, (byte) 0x1D);
        emitImm16(imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x3" is really part of the opcode
        emitRegRegOperands(dstReg, (byte) 0x3);
        emitImm16(imm);
    }
        if (lister != null) lister.RI(miStart, "SBB", dstReg, imm);
    }

  // [dstReg + dstDisp] -CF=  (word)  imm
  public final void emitSBB_RegDisp_Imm_Word(byte dstReg, Offset disp, int imm) {
    int miStart = mi;
            setMachineCodes(mi++, (byte) 0x66);
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x3" is really part of the opcode
        emitRegDispRegOperands(dstReg, disp, (byte) 0x3);
        emitImm8((byte)imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x3" is really part of the opcode
        emitRegDispRegOperands(dstReg, disp, (byte) 0x3);
        emitImm16(imm);
    }
    if (lister != null) lister.RDI(miStart, "SBB", dstReg, disp, imm);
  }

  // [dstIndex<<scale + dstDisp] -CF=  (word)  imm
  public final void emitSBB_RegOff_Imm_Word(byte dstIndex, short scale, Offset dstDisp, int imm) {
    int miStart = mi;
            setMachineCodes(mi++, (byte) 0x66);
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x3" is really part of the opcode
        emitRegOffRegOperands(dstIndex, scale, dstDisp, (byte) 0x3);
        emitImm8((byte)imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x3" is really part of the opcode
        emitRegOffRegOperands(dstIndex, scale, dstDisp, (byte) 0x3);
        emitImm32(imm);
    }
    if (lister != null) lister.RFDI(miStart, "SBB", dstIndex, scale, dstDisp, imm);
  }

  // [dstDisp] -CF=  (word)  imm
  public final void emitSBB_Abs_Imm_Word(Offset dstDisp, int imm) {
    int miStart = mi;
            setMachineCodes(mi++, (byte) 0x66);
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x3" is really part of the opcode
        emitAbsRegOperands(dstDisp, (byte) 0x3);
        emitImm8((byte)imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x3" is really part of the opcode
        emitAbsRegOperands(dstDisp, (byte) 0x3);
        emitImm32(imm);
    }
    if (lister != null) lister.RAI(miStart, "SBB", dstDisp, imm);
  }

  // [dstBase + dstIndex<<scale + dstDisp] -CF=  (word)  imm
  public final void emitSBB_RegIdx_Imm_Word(byte dstBase, byte dstIndex, short scale, Offset dstDisp, int imm) {
    int miStart = mi;
            setMachineCodes(mi++, (byte) 0x66);
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x3" is really part of the opcode
        emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, (byte) 0x3);
        emitImm8((byte)imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x3" is really part of the opcode
        emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, (byte) 0x3);
        emitImm32(imm);
    }
    if (lister != null) lister.RXDI(miStart, "SBB", dstBase, dstIndex, scale, dstDisp, imm);
  }

  // [dstReg] -CF=  (word)  imm
  public final void emitSBB_RegInd_Imm_Word(byte dstReg, int imm) {
    int miStart = mi;
            setMachineCodes(mi++, (byte) 0x66);
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x3" is really part of the opcode
        emitRegIndirectRegOperands(dstReg, (byte) 0x3);
        emitImm8((byte)imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x3" is really part of the opcode
        emitRegIndirectRegOperands(dstReg, (byte) 0x3);
        emitImm16(imm);
    }
    if (lister != null) lister.RNI(miStart, "SBB", dstReg, imm);
  }

  // dstReg -CF= (byte) imm
  public final void emitSBB_Reg_Imm_Byte(byte dstReg, int imm) {
    int miStart = mi;
    if (dstReg == EAX) {
        setMachineCodes(mi++, (byte) 0x1C);
        emitImm8(imm);
    } else {
        setMachineCodes(mi++, (byte) 0x80);
        // "register 0x3" is really part of the opcode
        emitRegRegOperands(dstReg, (byte) 0x3);
        emitImm8(imm);
    }
        if (lister != null) lister.RI(miStart, "SBB", dstReg, imm);
    }

  // [dstReg + dstDisp] -CF= (byte) imm
  public final void emitSBB_RegDisp_Imm_Byte(byte dstReg, Offset disp, int imm) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x80);
    // "register 0x3" is really part of the opcode
    emitRegDispRegOperands(dstReg, disp, (byte) 0x3);
    emitImm8(imm);
    if (lister != null) lister.RDI(miStart, "SBB", dstReg, disp, imm);
  }

  // [dstBase + dstIndex<<scale + dstDisp] -CF= (byte) imm
  public final void emitSBB_RegIdx_Imm_Byte(byte dstBase, byte dstIndex, short scale, Offset dstDisp, int imm) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x80);
    // "register 0x3" is really part of the opcode
    emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, (byte) 0x3);
    emitImm8(imm);
    if (lister != null) lister.RXDI(miStart, "SBB", dstBase, dstIndex, scale, dstDisp, imm);
  }

  // [dstIndex<<scale + dstDisp] -CF= (byte) imm
  public final void emitSBB_RegOff_Imm_Byte(byte dstIndex, short scale, Offset dstDisp, int imm) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x80);
    // "register 0x3" is really part of the opcode
    emitRegOffRegOperands(dstIndex, scale, dstDisp, (byte) 0x3);
    emitImm8(imm);
    if (lister != null) lister.RFDI(miStart, "SBB", dstIndex, scale, dstDisp, imm);
  }

  // [dstDisp] -CF= (byte) imm
  public final void emitSBB_Abs_Imm_Byte(Offset dstDisp, int imm) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x80);
    // "register 0x3" is really part of the opcode
    emitAbsRegOperands(dstDisp, (byte) 0x3);
    emitImm8(imm);
    if (lister != null) lister.RAI(miStart, "SBB", dstDisp, imm);
  }

  // [dstReg] -CF= (byte) imm
  public final void emitSBB_RegInd_Imm_Byte(byte dstReg, int imm) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x80);
    // "register 0x3" is really part of the opcode
    emitRegIndirectRegOperands(dstReg, (byte) 0x3);
    emitImm8(imm);
    if (lister != null) lister.RNI(miStart, "SBB", dstReg, imm);
  }

  /**
   * Generate a register--register SUB. That is,
   * <PRE>
   * [dstReg] -=  srcReg
   * </PRE>
   *
   * @param dstReg the destination register
   * @param srcReg the source register
   */
  public final void emitSUB_RegInd_Reg(byte dstReg, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x29);
    emitRegIndirectRegOperands(dstReg, srcReg);
    if (lister != null) lister.RNR(miStart, "SUB", dstReg, srcReg);
  }

  /**
   * Generate a register-offset--register SUB. That is,
   * <PRE>
   * [dstReg<<dstScale + dstDisp] -=  srcReg
   * </PRE>
   *
   * @param dstIndex the destination index register
   * @param dstScale the destination shift amount
   * @param dstDisp the destination displacement
   * @param srcReg the source register
   */
  public final void emitSUB_RegOff_Reg(byte dstIndex, short dstScale, Offset dstDisp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x29);
    emitRegOffRegOperands(dstIndex, dstScale, dstDisp, srcReg);
    if (lister != null) lister.RFDR(miStart, "SUB", dstIndex, dstScale, dstDisp, srcReg);
  }

  // [dstDisp] -=  srcReg
  public final void emitSUB_Abs_Reg(Offset dstDisp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x29);
    emitAbsRegOperands(dstDisp, srcReg);
    if (lister != null) lister.RAR(miStart, "SUB", dstDisp, srcReg);
  }

  // [dstBase + dstIndex<<scale + dstDisp] -=  srcReg
  public final void emitSUB_RegIdx_Reg(byte dstBase, byte dstIndex, short scale, Offset dstDisp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x29);
    emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, srcReg);
    if (lister != null) lister.RXDR(miStart, "SUB", dstBase, dstIndex, scale, dstDisp, srcReg);
  }

  // [dstReg + dstDisp] -=  srcReg
  public final void emitSUB_RegDisp_Reg(byte dstReg, Offset disp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x29);
    emitRegDispRegOperands(dstReg, disp, srcReg);
    if (lister != null) lister.RDR(miStart, "SUB", dstReg, disp, srcReg);
  }

  // dstReg -=  srcReg
  public final void emitSUB_Reg_Reg(byte dstReg, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x29);
    emitRegRegOperands(dstReg, srcReg);
    if (lister != null) lister.RR(miStart, "SUB", dstReg, srcReg);
  }

  // dstReg -=  [srcReg + srcDisp]
  public final void emitSUB_Reg_RegDisp(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x2B);
    emitRegDispRegOperands(srcReg, disp, dstReg);
    if (lister != null) lister.RRD(miStart, "SUB", dstReg, srcReg, disp);
  }

  // dstReg -=  [srcIndex<<scale + srcDisp]
  public final void emitSUB_Reg_RegOff(byte dstReg, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x2B);
    emitRegOffRegOperands(srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRFD(miStart, "SUB", dstReg, srcIndex, scale, srcDisp);
  }

  // dstReg -=  [srcDisp]
  public final void emitSUB_Reg_Abs(byte dstReg, Offset srcDisp) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x2B);
    emitAbsRegOperands(srcDisp, dstReg);
    if (lister != null) lister.RRA(miStart, "SUB", dstReg, srcDisp);
  }

  // dstReg -=  [srcBase + srcIndex<<scale + srcDisp]
  public final void emitSUB_Reg_RegIdx(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x2B);
    emitSIBRegOperands(srcBase, srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRXD(miStart, "SUB", dstReg, srcBase, srcIndex, scale, srcDisp);
  }

  // dstReg -=  [srcReg]
  public final void emitSUB_Reg_RegInd(byte dstReg, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x2B);
    emitRegIndirectRegOperands(srcReg, dstReg);
    if (lister != null) lister.RRN(miStart, "SUB", dstReg, srcReg);
  }

  /**
   * Generate a register--register SUB. That is,
   * <PRE>
   * [dstReg] -=  (word)  srcReg
   * </PRE>
   *
   * @param dstReg the destination register
   * @param srcReg the source register
   */
  public final void emitSUB_RegInd_Reg_Word(byte dstReg, byte srcReg) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x29);
    emitRegIndirectRegOperands(dstReg, srcReg);
    if (lister != null) lister.RNR(miStart, "SUB", dstReg, srcReg);
  }

  /**
   * Generate a register-offset--register SUB. That is,
   * <PRE>
   * [dstReg<<dstScale + dstDisp] -=  (word)  srcReg
   * </PRE>
   *
   * @param dstIndex the destination index register
   * @param dstScale the destination shift amount
   * @param dstDisp the destination displacement
   * @param srcReg the source register
   */
  public final void emitSUB_RegOff_Reg_Word(byte dstIndex, short dstScale, Offset dstDisp, byte srcReg) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x29);
    emitRegOffRegOperands(dstIndex, dstScale, dstDisp, srcReg);
    if (lister != null) lister.RFDR(miStart, "SUB", dstIndex, dstScale, dstDisp, srcReg);
  }

  // [dstDisp] -=  (word)  srcReg
  public final void emitSUB_Abs_Reg_Word(Offset dstDisp, byte srcReg) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x29);
    emitAbsRegOperands(dstDisp, srcReg);
    if (lister != null) lister.RAR(miStart, "SUB", dstDisp, srcReg);
  }

  // [dstBase + dstIndex<<scale + dstDisp] -=  (word)  srcReg
  public final void emitSUB_RegIdx_Reg_Word(byte dstBase, byte dstIndex, short scale, Offset dstDisp, byte srcReg) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x29);
    emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, srcReg);
    if (lister != null) lister.RXDR(miStart, "SUB", dstBase, dstIndex, scale, dstDisp, srcReg);
  }

  // [dstReg + dstDisp] -=  (word)  srcReg
  public final void emitSUB_RegDisp_Reg_Word(byte dstReg, Offset disp, byte srcReg) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x29);
    emitRegDispRegOperands(dstReg, disp, srcReg);
    if (lister != null) lister.RDR(miStart, "SUB", dstReg, disp, srcReg);
  }

  // dstReg -=  (word)  srcReg
  public final void emitSUB_Reg_Reg_Word(byte dstReg, byte srcReg) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x29);
    emitRegRegOperands(dstReg, srcReg);
    if (lister != null) lister.RR(miStart, "SUB", dstReg, srcReg);
  }

  // dstReg -=  (word)  [srcReg + srcDisp]
  public final void emitSUB_Reg_RegDisp_Word(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x2B);
    emitRegDispRegOperands(srcReg, disp, dstReg);
    if (lister != null) lister.RRD(miStart, "SUB", dstReg, srcReg, disp);
  }

  // dstReg -=  (word)  [srcIndex<<scale + srcDisp]
  public final void emitSUB_Reg_RegOff_Word(byte dstReg, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x2B);
    emitRegOffRegOperands(srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRFD(miStart, "SUB", dstReg, srcIndex, scale, srcDisp);
  }

  // dstReg -=  (word)  [srcDisp]
  public final void emitSUB_Reg_Abs_Word(byte dstReg, Offset srcDisp) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x2B);
    emitAbsRegOperands(srcDisp, dstReg);
    if (lister != null) lister.RRA(miStart, "SUB", dstReg, srcDisp);
  }

  // dstReg -=  (word)  [srcBase + srcIndex<<scale + srcDisp]
  public final void emitSUB_Reg_RegIdx_Word(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x2B);
    emitSIBRegOperands(srcBase, srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRXD(miStart, "SUB", dstReg, srcBase, srcIndex, scale, srcDisp);
  }

  // dstReg -=  (word)  [srcReg]
  public final void emitSUB_Reg_RegInd_Word(byte dstReg, byte srcReg) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x2B);
    emitRegIndirectRegOperands(srcReg, dstReg);
    if (lister != null) lister.RRN(miStart, "SUB", dstReg, srcReg);
  }

  /**
   * Generate a register--register SUB. That is,
   * <PRE>
   * [dstReg] -=  (byte)  srcReg
   * </PRE>
   *
   * @param dstReg the destination register
   * @param srcReg the source register
   */
  public final void emitSUB_RegInd_Reg_Byte(byte dstReg, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x28);
    emitRegIndirectRegOperands(dstReg, srcReg);
    if (lister != null) lister.RNR(miStart, "SUB", dstReg, srcReg);
  }

  /**
   * Generate a register-offset--register SUB. That is,
   * <PRE>
   * [dstReg<<dstScale + dstDisp] -=  (byte)  srcReg
   * </PRE>
   *
   * @param dstIndex the destination index register
   * @param dstScale the destination shift amount
   * @param dstDisp the destination displacement
   * @param srcReg the source register
   */
  public final void emitSUB_RegOff_Reg_Byte(byte dstIndex, short dstScale, Offset dstDisp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x28);
    emitRegOffRegOperands(dstIndex, dstScale, dstDisp, srcReg);
    if (lister != null) lister.RFDR(miStart, "SUB", dstIndex, dstScale, dstDisp, srcReg);
  }

  // [dstDisp] -=  (byte)  srcReg
  public final void emitSUB_Abs_Reg_Byte(Offset dstDisp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x28);
    emitAbsRegOperands(dstDisp, srcReg);
    if (lister != null) lister.RAR(miStart, "SUB", dstDisp, srcReg);
  }

  // [dstBase + dstIndex<<scale + dstDisp] -=  (byte)  srcReg
  public final void emitSUB_RegIdx_Reg_Byte(byte dstBase, byte dstIndex, short scale, Offset dstDisp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x28);
    emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, srcReg);
    if (lister != null) lister.RXDR(miStart, "SUB", dstBase, dstIndex, scale, dstDisp, srcReg);
  }

  // [dstReg + dstDisp] -=  (byte)  srcReg
  public final void emitSUB_RegDisp_Reg_Byte(byte dstReg, Offset disp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x28);
    emitRegDispRegOperands(dstReg, disp, srcReg);
    if (lister != null) lister.RDR(miStart, "SUB", dstReg, disp, srcReg);
  }

  // dstReg -=  (byte)  srcReg
  public final void emitSUB_Reg_Reg_Byte(byte dstReg, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x28);
    emitRegRegOperands(dstReg, srcReg);
    if (lister != null) lister.RR(miStart, "SUB", dstReg, srcReg);
  }

  // dstReg -=  (byte)  [srcReg + srcDisp]
  public final void emitSUB_Reg_RegDisp_Byte(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x2A);
    emitRegDispRegOperands(srcReg, disp, dstReg);
    if (lister != null) lister.RRD(miStart, "SUB", dstReg, srcReg, disp);
  }

  // dstReg -=  (byte)  [srcIndex<<scale + srcDisp]
  public final void emitSUB_Reg_RegOff_Byte(byte dstReg, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x2A);
    emitRegOffRegOperands(srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRFD(miStart, "SUB", dstReg, srcIndex, scale, srcDisp);
  }

  // dstReg -=  (byte)  [srcDisp]
  public final void emitSUB_Reg_Abs_Byte(byte dstReg, Offset srcDisp) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x2A);
    emitAbsRegOperands(srcDisp, dstReg);
    if (lister != null) lister.RRA(miStart, "SUB", dstReg, srcDisp);
  }

  // dstReg -=  (byte)  [srcBase + srcIndex<<scale + srcDisp]
  public final void emitSUB_Reg_RegIdx_Byte(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x2A);
    emitSIBRegOperands(srcBase, srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRXD(miStart, "SUB", dstReg, srcBase, srcIndex, scale, srcDisp);
  }

  // dstReg -=  (byte)  [srcReg]
  public final void emitSUB_Reg_RegInd_Byte(byte dstReg, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x2A);
    emitRegIndirectRegOperands(srcReg, dstReg);
    if (lister != null) lister.RRN(miStart, "SUB", dstReg, srcReg);
  }

  // dstReg -=  imm
  public final void emitSUB_Reg_Imm(byte dstReg, int imm) {
    int miStart = mi;
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x5" is really part of the opcode
        emitRegRegOperands(dstReg, (byte) 0x5);
        emitImm8((byte)imm);
    } else if (dstReg == EAX) {
        setMachineCodes(mi++, (byte) 0x2D);
        emitImm32(imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x5" is really part of the opcode
        emitRegRegOperands(dstReg, (byte) 0x5);
        emitImm32(imm);
    }
        if (lister != null) lister.RI(miStart, "SUB", dstReg, imm);
    }

  // [dstReg + dstDisp] -=  imm
  public final void emitSUB_RegDisp_Imm(byte dstReg, Offset disp, int imm) {
    int miStart = mi;
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x5" is really part of the opcode
        emitRegDispRegOperands(dstReg, disp, (byte) 0x5);
        emitImm8((byte)imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x5" is really part of the opcode
        emitRegDispRegOperands(dstReg, disp, (byte) 0x5);
        emitImm32(imm);
    }
    if (lister != null) lister.RDI(miStart, "SUB", dstReg, disp, imm);
  }

  // [dstIndex<<scale + dstDisp] -=  imm
  public final void emitSUB_RegOff_Imm(byte dstIndex, short scale, Offset dstDisp, int imm) {
    int miStart = mi;
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x5" is really part of the opcode
        emitRegOffRegOperands(dstIndex, scale, dstDisp, (byte) 0x5);
        emitImm8((byte)imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x5" is really part of the opcode
        emitRegOffRegOperands(dstIndex, scale, dstDisp, (byte) 0x5);
        emitImm32(imm);
    }
    if (lister != null) lister.RFDI(miStart, "SUB", dstIndex, scale, dstDisp, imm);
  }

  // [dstDisp] -=  imm
  public final void emitSUB_Abs_Imm(Offset dstDisp, int imm) {
    int miStart = mi;
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x5" is really part of the opcode
        emitAbsRegOperands(dstDisp, (byte) 0x5);
        emitImm8((byte)imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x5" is really part of the opcode
        emitAbsRegOperands(dstDisp, (byte) 0x5);
        emitImm32(imm);
    }
    if (lister != null) lister.RAI(miStart, "SUB", dstDisp, imm);
  }

  // [dstBase + dstIndex<<scale + dstDisp] -=  imm
  public final void emitSUB_RegIdx_Imm(byte dstBase, byte dstIndex, short scale, Offset dstDisp, int imm) {
    int miStart = mi;
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x5" is really part of the opcode
        emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, (byte) 0x5);
        emitImm8((byte)imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x5" is really part of the opcode
        emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, (byte) 0x5);
        emitImm32(imm);
    }
    if (lister != null) lister.RXDI(miStart, "SUB", dstBase, dstIndex, scale, dstDisp, imm);
  }

  // [dstReg] -=  imm
  public final void emitSUB_RegInd_Imm(byte dstReg, int imm) {
    int miStart = mi;
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x5" is really part of the opcode
        emitRegIndirectRegOperands(dstReg, (byte) 0x5);
        emitImm8((byte)imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x5" is really part of the opcode
        emitRegIndirectRegOperands(dstReg, (byte) 0x5);
        emitImm32(imm);
    }
    if (lister != null) lister.RNI(miStart, "SUB", dstReg, imm);
  }

  // dstReg -=  (word)  imm
  public final void emitSUB_Reg_Imm_Word(byte dstReg, int imm) {
    int miStart = mi;
            setMachineCodes(mi++, (byte) 0x66);
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x5" is really part of the opcode
        emitRegRegOperands(dstReg, (byte) 0x5);
        emitImm8((byte)imm);
    } else if (dstReg == EAX) {
        setMachineCodes(mi++, (byte) 0x2D);
        emitImm16(imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x5" is really part of the opcode
        emitRegRegOperands(dstReg, (byte) 0x5);
        emitImm16(imm);
    }
        if (lister != null) lister.RI(miStart, "SUB", dstReg, imm);
    }

  // [dstReg + dstDisp] -=  (word)  imm
  public final void emitSUB_RegDisp_Imm_Word(byte dstReg, Offset disp, int imm) {
    int miStart = mi;
            setMachineCodes(mi++, (byte) 0x66);
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x5" is really part of the opcode
        emitRegDispRegOperands(dstReg, disp, (byte) 0x5);
        emitImm8((byte)imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x5" is really part of the opcode
        emitRegDispRegOperands(dstReg, disp, (byte) 0x5);
        emitImm16(imm);
    }
    if (lister != null) lister.RDI(miStart, "SUB", dstReg, disp, imm);
  }

  // [dstIndex<<scale + dstDisp] -=  (word)  imm
  public final void emitSUB_RegOff_Imm_Word(byte dstIndex, short scale, Offset dstDisp, int imm) {
    int miStart = mi;
            setMachineCodes(mi++, (byte) 0x66);
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x5" is really part of the opcode
        emitRegOffRegOperands(dstIndex, scale, dstDisp, (byte) 0x5);
        emitImm8((byte)imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x5" is really part of the opcode
        emitRegOffRegOperands(dstIndex, scale, dstDisp, (byte) 0x5);
        emitImm32(imm);
    }
    if (lister != null) lister.RFDI(miStart, "SUB", dstIndex, scale, dstDisp, imm);
  }

  // [dstDisp] -=  (word)  imm
  public final void emitSUB_Abs_Imm_Word(Offset dstDisp, int imm) {
    int miStart = mi;
            setMachineCodes(mi++, (byte) 0x66);
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x5" is really part of the opcode
        emitAbsRegOperands(dstDisp, (byte) 0x5);
        emitImm8((byte)imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x5" is really part of the opcode
        emitAbsRegOperands(dstDisp, (byte) 0x5);
        emitImm32(imm);
    }
    if (lister != null) lister.RAI(miStart, "SUB", dstDisp, imm);
  }

  // [dstBase + dstIndex<<scale + dstDisp] -=  (word)  imm
  public final void emitSUB_RegIdx_Imm_Word(byte dstBase, byte dstIndex, short scale, Offset dstDisp, int imm) {
    int miStart = mi;
            setMachineCodes(mi++, (byte) 0x66);
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x5" is really part of the opcode
        emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, (byte) 0x5);
        emitImm8((byte)imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x5" is really part of the opcode
        emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, (byte) 0x5);
        emitImm32(imm);
    }
    if (lister != null) lister.RXDI(miStart, "SUB", dstBase, dstIndex, scale, dstDisp, imm);
  }

  // [dstReg] -=  (word)  imm
  public final void emitSUB_RegInd_Imm_Word(byte dstReg, int imm) {
    int miStart = mi;
            setMachineCodes(mi++, (byte) 0x66);
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x5" is really part of the opcode
        emitRegIndirectRegOperands(dstReg, (byte) 0x5);
        emitImm8((byte)imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x5" is really part of the opcode
        emitRegIndirectRegOperands(dstReg, (byte) 0x5);
        emitImm16(imm);
    }
    if (lister != null) lister.RNI(miStart, "SUB", dstReg, imm);
  }

  // dstReg -= (byte) imm
  public final void emitSUB_Reg_Imm_Byte(byte dstReg, int imm) {
    int miStart = mi;
    if (dstReg == EAX) {
        setMachineCodes(mi++, (byte) 0x2C);
        emitImm8(imm);
    } else {
        setMachineCodes(mi++, (byte) 0x80);
        // "register 0x5" is really part of the opcode
        emitRegRegOperands(dstReg, (byte) 0x5);
        emitImm8(imm);
    }
        if (lister != null) lister.RI(miStart, "SUB", dstReg, imm);
    }

  // [dstReg + dstDisp] -= (byte) imm
  public final void emitSUB_RegDisp_Imm_Byte(byte dstReg, Offset disp, int imm) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x80);
    // "register 0x5" is really part of the opcode
    emitRegDispRegOperands(dstReg, disp, (byte) 0x5);
    emitImm8(imm);
    if (lister != null) lister.RDI(miStart, "SUB", dstReg, disp, imm);
  }

  // [dstBase + dstIndex<<scale + dstDisp] -= (byte) imm
  public final void emitSUB_RegIdx_Imm_Byte(byte dstBase, byte dstIndex, short scale, Offset dstDisp, int imm) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x80);
    // "register 0x5" is really part of the opcode
    emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, (byte) 0x5);
    emitImm8(imm);
    if (lister != null) lister.RXDI(miStart, "SUB", dstBase, dstIndex, scale, dstDisp, imm);
  }

  // [dstIndex<<scale + dstDisp] -= (byte) imm
  public final void emitSUB_RegOff_Imm_Byte(byte dstIndex, short scale, Offset dstDisp, int imm) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x80);
    // "register 0x5" is really part of the opcode
    emitRegOffRegOperands(dstIndex, scale, dstDisp, (byte) 0x5);
    emitImm8(imm);
    if (lister != null) lister.RFDI(miStart, "SUB", dstIndex, scale, dstDisp, imm);
  }

  // [dstDisp] -= (byte) imm
  public final void emitSUB_Abs_Imm_Byte(Offset dstDisp, int imm) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x80);
    // "register 0x5" is really part of the opcode
    emitAbsRegOperands(dstDisp, (byte) 0x5);
    emitImm8(imm);
    if (lister != null) lister.RAI(miStart, "SUB", dstDisp, imm);
  }

  // [dstReg] -= (byte) imm
  public final void emitSUB_RegInd_Imm_Byte(byte dstReg, int imm) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x80);
    // "register 0x5" is really part of the opcode
    emitRegIndirectRegOperands(dstReg, (byte) 0x5);
    emitImm8(imm);
    if (lister != null) lister.RNI(miStart, "SUB", dstReg, imm);
  }

  /**
   * Generate a register--register TEST. That is,
   * <PRE>
   * [dstReg] &=  srcReg
   * </PRE>
   *
   * @param dstReg the destination register
   * @param srcReg the source register
   */
  public final void emitTEST_RegInd_Reg(byte dstReg, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x85);
    emitRegIndirectRegOperands(dstReg, srcReg);
    if (lister != null) lister.RNR(miStart, "TEST", dstReg, srcReg);
  }

  /**
   * Generate a register-offset--register TEST. That is,
   * <PRE>
   * [dstReg<<dstScale + dstDisp] &=  srcReg
   * </PRE>
   *
   * @param dstIndex the destination index register
   * @param dstScale the destination shift amount
   * @param dstDisp the destination displacement
   * @param srcReg the source register
   */
  public final void emitTEST_RegOff_Reg(byte dstIndex, short dstScale, Offset dstDisp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x85);
    emitRegOffRegOperands(dstIndex, dstScale, dstDisp, srcReg);
    if (lister != null) lister.RFDR(miStart, "TEST", dstIndex, dstScale, dstDisp, srcReg);
  }

  // [dstDisp] &=  srcReg
  public final void emitTEST_Abs_Reg(Offset dstDisp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x85);
    emitAbsRegOperands(dstDisp, srcReg);
    if (lister != null) lister.RAR(miStart, "TEST", dstDisp, srcReg);
  }

  // [dstBase + dstIndex<<scale + dstDisp] &=  srcReg
  public final void emitTEST_RegIdx_Reg(byte dstBase, byte dstIndex, short scale, Offset dstDisp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x85);
    emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, srcReg);
    if (lister != null) lister.RXDR(miStart, "TEST", dstBase, dstIndex, scale, dstDisp, srcReg);
  }

  // [dstReg + dstDisp] &=  srcReg
  public final void emitTEST_RegDisp_Reg(byte dstReg, Offset disp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x85);
    emitRegDispRegOperands(dstReg, disp, srcReg);
    if (lister != null) lister.RDR(miStart, "TEST", dstReg, disp, srcReg);
  }

  // dstReg &=  srcReg
  public final void emitTEST_Reg_Reg(byte dstReg, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x85);
    emitRegRegOperands(dstReg, srcReg);
    if (lister != null) lister.RR(miStart, "TEST", dstReg, srcReg);
  }

  /**
   * Generate a register--register TEST. That is,
   * <PRE>
   * [dstReg] &=  (word)  srcReg
   * </PRE>
   *
   * @param dstReg the destination register
   * @param srcReg the source register
   */
  public final void emitTEST_RegInd_Reg_Word(byte dstReg, byte srcReg) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x85);
    emitRegIndirectRegOperands(dstReg, srcReg);
    if (lister != null) lister.RNR(miStart, "TEST", dstReg, srcReg);
  }

  /**
   * Generate a register-offset--register TEST. That is,
   * <PRE>
   * [dstReg<<dstScale + dstDisp] &=  (word)  srcReg
   * </PRE>
   *
   * @param dstIndex the destination index register
   * @param dstScale the destination shift amount
   * @param dstDisp the destination displacement
   * @param srcReg the source register
   */
  public final void emitTEST_RegOff_Reg_Word(byte dstIndex, short dstScale, Offset dstDisp, byte srcReg) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x85);
    emitRegOffRegOperands(dstIndex, dstScale, dstDisp, srcReg);
    if (lister != null) lister.RFDR(miStart, "TEST", dstIndex, dstScale, dstDisp, srcReg);
  }

  // [dstDisp] &=  (word)  srcReg
  public final void emitTEST_Abs_Reg_Word(Offset dstDisp, byte srcReg) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x85);
    emitAbsRegOperands(dstDisp, srcReg);
    if (lister != null) lister.RAR(miStart, "TEST", dstDisp, srcReg);
  }

  // [dstBase + dstIndex<<scale + dstDisp] &=  (word)  srcReg
  public final void emitTEST_RegIdx_Reg_Word(byte dstBase, byte dstIndex, short scale, Offset dstDisp, byte srcReg) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x85);
    emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, srcReg);
    if (lister != null) lister.RXDR(miStart, "TEST", dstBase, dstIndex, scale, dstDisp, srcReg);
  }

  // [dstReg + dstDisp] &=  (word)  srcReg
  public final void emitTEST_RegDisp_Reg_Word(byte dstReg, Offset disp, byte srcReg) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x85);
    emitRegDispRegOperands(dstReg, disp, srcReg);
    if (lister != null) lister.RDR(miStart, "TEST", dstReg, disp, srcReg);
  }

  // dstReg &=  (word)  srcReg
  public final void emitTEST_Reg_Reg_Word(byte dstReg, byte srcReg) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x85);
    emitRegRegOperands(dstReg, srcReg);
    if (lister != null) lister.RR(miStart, "TEST", dstReg, srcReg);
  }

  /**
   * Generate a register--register TEST. That is,
   * <PRE>
   * [dstReg] &=  (byte)  srcReg
   * </PRE>
   *
   * @param dstReg the destination register
   * @param srcReg the source register
   */
  public final void emitTEST_RegInd_Reg_Byte(byte dstReg, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x84);
    emitRegIndirectRegOperands(dstReg, srcReg);
    if (lister != null) lister.RNR(miStart, "TEST", dstReg, srcReg);
  }

  /**
   * Generate a register-offset--register TEST. That is,
   * <PRE>
   * [dstReg<<dstScale + dstDisp] &=  (byte)  srcReg
   * </PRE>
   *
   * @param dstIndex the destination index register
   * @param dstScale the destination shift amount
   * @param dstDisp the destination displacement
   * @param srcReg the source register
   */
  public final void emitTEST_RegOff_Reg_Byte(byte dstIndex, short dstScale, Offset dstDisp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x84);
    emitRegOffRegOperands(dstIndex, dstScale, dstDisp, srcReg);
    if (lister != null) lister.RFDR(miStart, "TEST", dstIndex, dstScale, dstDisp, srcReg);
  }

  // [dstDisp] &=  (byte)  srcReg
  public final void emitTEST_Abs_Reg_Byte(Offset dstDisp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x84);
    emitAbsRegOperands(dstDisp, srcReg);
    if (lister != null) lister.RAR(miStart, "TEST", dstDisp, srcReg);
  }

  // [dstBase + dstIndex<<scale + dstDisp] &=  (byte)  srcReg
  public final void emitTEST_RegIdx_Reg_Byte(byte dstBase, byte dstIndex, short scale, Offset dstDisp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x84);
    emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, srcReg);
    if (lister != null) lister.RXDR(miStart, "TEST", dstBase, dstIndex, scale, dstDisp, srcReg);
  }

  // [dstReg + dstDisp] &=  (byte)  srcReg
  public final void emitTEST_RegDisp_Reg_Byte(byte dstReg, Offset disp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x84);
    emitRegDispRegOperands(dstReg, disp, srcReg);
    if (lister != null) lister.RDR(miStart, "TEST", dstReg, disp, srcReg);
  }

  // dstReg &=  (byte)  srcReg
  public final void emitTEST_Reg_Reg_Byte(byte dstReg, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x84);
    emitRegRegOperands(dstReg, srcReg);
    if (lister != null) lister.RR(miStart, "TEST", dstReg, srcReg);
  }

  // dstReg &=  imm
  public final void emitTEST_Reg_Imm(byte dstReg, int imm) {
    int miStart = mi;
    if (false) {
    } else if (dstReg == EAX) {
        setMachineCodes(mi++, (byte) 0xA9);
        emitImm32(imm);
    } else {
        setMachineCodes(mi++, (byte) 0xF7);
        // "register 0x0" is really part of the opcode
        emitRegRegOperands(dstReg, (byte) 0x0);
        emitImm32(imm);
    }
        if (lister != null) lister.RI(miStart, "TEST", dstReg, imm);
    }

  // [dstReg + dstDisp] &=  imm
  public final void emitTEST_RegDisp_Imm(byte dstReg, Offset disp, int imm) {
    int miStart = mi;
    if (false) {
    } else {
        setMachineCodes(mi++, (byte) 0xF7);
        // "register 0x0" is really part of the opcode
        emitRegDispRegOperands(dstReg, disp, (byte) 0x0);
        emitImm32(imm);
    }
    if (lister != null) lister.RDI(miStart, "TEST", dstReg, disp, imm);
  }

  // [dstIndex<<scale + dstDisp] &=  imm
  public final void emitTEST_RegOff_Imm(byte dstIndex, short scale, Offset dstDisp, int imm) {
    int miStart = mi;
    if (false) {
    } else {
        setMachineCodes(mi++, (byte) 0xF7);
        // "register 0x0" is really part of the opcode
        emitRegOffRegOperands(dstIndex, scale, dstDisp, (byte) 0x0);
        emitImm32(imm);
    }
    if (lister != null) lister.RFDI(miStart, "TEST", dstIndex, scale, dstDisp, imm);
  }

  // [dstDisp] &=  imm
  public final void emitTEST_Abs_Imm(Offset dstDisp, int imm) {
    int miStart = mi;
    if (false) {
    } else {
        setMachineCodes(mi++, (byte) 0xF7);
        // "register 0x0" is really part of the opcode
        emitAbsRegOperands(dstDisp, (byte) 0x0);
        emitImm32(imm);
    }
    if (lister != null) lister.RAI(miStart, "TEST", dstDisp, imm);
  }

  // [dstBase + dstIndex<<scale + dstDisp] &=  imm
  public final void emitTEST_RegIdx_Imm(byte dstBase, byte dstIndex, short scale, Offset dstDisp, int imm) {
    int miStart = mi;
    if (false) {
    } else {
        setMachineCodes(mi++, (byte) 0xF7);
        // "register 0x0" is really part of the opcode
        emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, (byte) 0x0);
        emitImm32(imm);
    }
    if (lister != null) lister.RXDI(miStart, "TEST", dstBase, dstIndex, scale, dstDisp, imm);
  }

  // [dstReg] &=  imm
  public final void emitTEST_RegInd_Imm(byte dstReg, int imm) {
    int miStart = mi;
    if (false) {
    } else {
        setMachineCodes(mi++, (byte) 0xF7);
        // "register 0x0" is really part of the opcode
        emitRegIndirectRegOperands(dstReg, (byte) 0x0);
        emitImm32(imm);
    }
    if (lister != null) lister.RNI(miStart, "TEST", dstReg, imm);
  }

  // dstReg &=  (word)  imm
  public final void emitTEST_Reg_Imm_Word(byte dstReg, int imm) {
    int miStart = mi;
            setMachineCodes(mi++, (byte) 0x66);
    if (false) {
    } else if (dstReg == EAX) {
        setMachineCodes(mi++, (byte) 0xA9);
        emitImm16(imm);
    } else {
        setMachineCodes(mi++, (byte) 0xF7);
        // "register 0x0" is really part of the opcode
        emitRegRegOperands(dstReg, (byte) 0x0);
        emitImm16(imm);
    }
        if (lister != null) lister.RI(miStart, "TEST", dstReg, imm);
    }

  // [dstReg + dstDisp] &=  (word)  imm
  public final void emitTEST_RegDisp_Imm_Word(byte dstReg, Offset disp, int imm) {
    int miStart = mi;
            setMachineCodes(mi++, (byte) 0x66);
    if (false) {
    } else {
        setMachineCodes(mi++, (byte) 0xF7);
        // "register 0x0" is really part of the opcode
        emitRegDispRegOperands(dstReg, disp, (byte) 0x0);
        emitImm16(imm);
    }
    if (lister != null) lister.RDI(miStart, "TEST", dstReg, disp, imm);
  }

  // [dstIndex<<scale + dstDisp] &=  (word)  imm
  public final void emitTEST_RegOff_Imm_Word(byte dstIndex, short scale, Offset dstDisp, int imm) {
    int miStart = mi;
            setMachineCodes(mi++, (byte) 0x66);
    if (false) {
    } else {
        setMachineCodes(mi++, (byte) 0xF7);
        // "register 0x0" is really part of the opcode
        emitRegOffRegOperands(dstIndex, scale, dstDisp, (byte) 0x0);
        emitImm32(imm);
    }
    if (lister != null) lister.RFDI(miStart, "TEST", dstIndex, scale, dstDisp, imm);
  }

  // [dstDisp] &=  (word)  imm
  public final void emitTEST_Abs_Imm_Word(Offset dstDisp, int imm) {
    int miStart = mi;
            setMachineCodes(mi++, (byte) 0x66);
    if (false) {
    } else {
        setMachineCodes(mi++, (byte) 0xF7);
        // "register 0x0" is really part of the opcode
        emitAbsRegOperands(dstDisp, (byte) 0x0);
        emitImm32(imm);
    }
    if (lister != null) lister.RAI(miStart, "TEST", dstDisp, imm);
  }

  // [dstBase + dstIndex<<scale + dstDisp] &=  (word)  imm
  public final void emitTEST_RegIdx_Imm_Word(byte dstBase, byte dstIndex, short scale, Offset dstDisp, int imm) {
    int miStart = mi;
            setMachineCodes(mi++, (byte) 0x66);
    if (false) {
    } else {
        setMachineCodes(mi++, (byte) 0xF7);
        // "register 0x0" is really part of the opcode
        emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, (byte) 0x0);
        emitImm32(imm);
    }
    if (lister != null) lister.RXDI(miStart, "TEST", dstBase, dstIndex, scale, dstDisp, imm);
  }

  // [dstReg] &=  (word)  imm
  public final void emitTEST_RegInd_Imm_Word(byte dstReg, int imm) {
    int miStart = mi;
            setMachineCodes(mi++, (byte) 0x66);
    if (false) {
    } else {
        setMachineCodes(mi++, (byte) 0xF7);
        // "register 0x0" is really part of the opcode
        emitRegIndirectRegOperands(dstReg, (byte) 0x0);
        emitImm16(imm);
    }
    if (lister != null) lister.RNI(miStart, "TEST", dstReg, imm);
  }

  // dstReg &= (byte) imm
  public final void emitTEST_Reg_Imm_Byte(byte dstReg, int imm) {
    int miStart = mi;
    if (dstReg == EAX) {
        setMachineCodes(mi++, (byte) 0xA8);
        emitImm8(imm);
    } else {
        setMachineCodes(mi++, (byte) 0xF6);
        // "register 0x0" is really part of the opcode
        emitRegRegOperands(dstReg, (byte) 0x0);
        emitImm8(imm);
    }
        if (lister != null) lister.RI(miStart, "TEST", dstReg, imm);
    }

  // [dstReg + dstDisp] &= (byte) imm
  public final void emitTEST_RegDisp_Imm_Byte(byte dstReg, Offset disp, int imm) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF6);
    // "register 0x0" is really part of the opcode
    emitRegDispRegOperands(dstReg, disp, (byte) 0x0);
    emitImm8(imm);
    if (lister != null) lister.RDI(miStart, "TEST", dstReg, disp, imm);
  }

  // [dstBase + dstIndex<<scale + dstDisp] &= (byte) imm
  public final void emitTEST_RegIdx_Imm_Byte(byte dstBase, byte dstIndex, short scale, Offset dstDisp, int imm) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF6);
    // "register 0x0" is really part of the opcode
    emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, (byte) 0x0);
    emitImm8(imm);
    if (lister != null) lister.RXDI(miStart, "TEST", dstBase, dstIndex, scale, dstDisp, imm);
  }

  // [dstIndex<<scale + dstDisp] &= (byte) imm
  public final void emitTEST_RegOff_Imm_Byte(byte dstIndex, short scale, Offset dstDisp, int imm) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF6);
    // "register 0x0" is really part of the opcode
    emitRegOffRegOperands(dstIndex, scale, dstDisp, (byte) 0x0);
    emitImm8(imm);
    if (lister != null) lister.RFDI(miStart, "TEST", dstIndex, scale, dstDisp, imm);
  }

  // [dstDisp] &= (byte) imm
  public final void emitTEST_Abs_Imm_Byte(Offset dstDisp, int imm) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF6);
    // "register 0x0" is really part of the opcode
    emitAbsRegOperands(dstDisp, (byte) 0x0);
    emitImm8(imm);
    if (lister != null) lister.RAI(miStart, "TEST", dstDisp, imm);
  }

  // [dstReg] &= (byte) imm
  public final void emitTEST_RegInd_Imm_Byte(byte dstReg, int imm) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF6);
    // "register 0x0" is really part of the opcode
    emitRegIndirectRegOperands(dstReg, (byte) 0x0);
    emitImm8(imm);
    if (lister != null) lister.RNI(miStart, "TEST", dstReg, imm);
  }

  /**
   * Generate a register--register XOR. That is,
   * <PRE>
   * [dstReg] ~=  srcReg
   * </PRE>
   *
   * @param dstReg the destination register
   * @param srcReg the source register
   */
  public final void emitXOR_RegInd_Reg(byte dstReg, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x31);
    emitRegIndirectRegOperands(dstReg, srcReg);
    if (lister != null) lister.RNR(miStart, "XOR", dstReg, srcReg);
  }

  /**
   * Generate a register-offset--register XOR. That is,
   * <PRE>
   * [dstReg<<dstScale + dstDisp] ~=  srcReg
   * </PRE>
   *
   * @param dstIndex the destination index register
   * @param dstScale the destination shift amount
   * @param dstDisp the destination displacement
   * @param srcReg the source register
   */
  public final void emitXOR_RegOff_Reg(byte dstIndex, short dstScale, Offset dstDisp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x31);
    emitRegOffRegOperands(dstIndex, dstScale, dstDisp, srcReg);
    if (lister != null) lister.RFDR(miStart, "XOR", dstIndex, dstScale, dstDisp, srcReg);
  }

  // [dstDisp] ~=  srcReg
  public final void emitXOR_Abs_Reg(Offset dstDisp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x31);
    emitAbsRegOperands(dstDisp, srcReg);
    if (lister != null) lister.RAR(miStart, "XOR", dstDisp, srcReg);
  }

  // [dstBase + dstIndex<<scale + dstDisp] ~=  srcReg
  public final void emitXOR_RegIdx_Reg(byte dstBase, byte dstIndex, short scale, Offset dstDisp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x31);
    emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, srcReg);
    if (lister != null) lister.RXDR(miStart, "XOR", dstBase, dstIndex, scale, dstDisp, srcReg);
  }

  // [dstReg + dstDisp] ~=  srcReg
  public final void emitXOR_RegDisp_Reg(byte dstReg, Offset disp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x31);
    emitRegDispRegOperands(dstReg, disp, srcReg);
    if (lister != null) lister.RDR(miStart, "XOR", dstReg, disp, srcReg);
  }

  // dstReg ~=  srcReg
  public final void emitXOR_Reg_Reg(byte dstReg, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x31);
    emitRegRegOperands(dstReg, srcReg);
    if (lister != null) lister.RR(miStart, "XOR", dstReg, srcReg);
  }

  // dstReg ~=  [srcReg + srcDisp]
  public final void emitXOR_Reg_RegDisp(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x33);
    emitRegDispRegOperands(srcReg, disp, dstReg);
    if (lister != null) lister.RRD(miStart, "XOR", dstReg, srcReg, disp);
  }

  // dstReg ~=  [srcIndex<<scale + srcDisp]
  public final void emitXOR_Reg_RegOff(byte dstReg, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x33);
    emitRegOffRegOperands(srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRFD(miStart, "XOR", dstReg, srcIndex, scale, srcDisp);
  }

  // dstReg ~=  [srcDisp]
  public final void emitXOR_Reg_Abs(byte dstReg, Offset srcDisp) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x33);
    emitAbsRegOperands(srcDisp, dstReg);
    if (lister != null) lister.RRA(miStart, "XOR", dstReg, srcDisp);
  }

  // dstReg ~=  [srcBase + srcIndex<<scale + srcDisp]
  public final void emitXOR_Reg_RegIdx(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x33);
    emitSIBRegOperands(srcBase, srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRXD(miStart, "XOR", dstReg, srcBase, srcIndex, scale, srcDisp);
  }

  // dstReg ~=  [srcReg]
  public final void emitXOR_Reg_RegInd(byte dstReg, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x33);
    emitRegIndirectRegOperands(srcReg, dstReg);
    if (lister != null) lister.RRN(miStart, "XOR", dstReg, srcReg);
  }

  /**
   * Generate a register--register XOR. That is,
   * <PRE>
   * [dstReg] ~=  (word)  srcReg
   * </PRE>
   *
   * @param dstReg the destination register
   * @param srcReg the source register
   */
  public final void emitXOR_RegInd_Reg_Word(byte dstReg, byte srcReg) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x31);
    emitRegIndirectRegOperands(dstReg, srcReg);
    if (lister != null) lister.RNR(miStart, "XOR", dstReg, srcReg);
  }

  /**
   * Generate a register-offset--register XOR. That is,
   * <PRE>
   * [dstReg<<dstScale + dstDisp] ~=  (word)  srcReg
   * </PRE>
   *
   * @param dstIndex the destination index register
   * @param dstScale the destination shift amount
   * @param dstDisp the destination displacement
   * @param srcReg the source register
   */
  public final void emitXOR_RegOff_Reg_Word(byte dstIndex, short dstScale, Offset dstDisp, byte srcReg) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x31);
    emitRegOffRegOperands(dstIndex, dstScale, dstDisp, srcReg);
    if (lister != null) lister.RFDR(miStart, "XOR", dstIndex, dstScale, dstDisp, srcReg);
  }

  // [dstDisp] ~=  (word)  srcReg
  public final void emitXOR_Abs_Reg_Word(Offset dstDisp, byte srcReg) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x31);
    emitAbsRegOperands(dstDisp, srcReg);
    if (lister != null) lister.RAR(miStart, "XOR", dstDisp, srcReg);
  }

  // [dstBase + dstIndex<<scale + dstDisp] ~=  (word)  srcReg
  public final void emitXOR_RegIdx_Reg_Word(byte dstBase, byte dstIndex, short scale, Offset dstDisp, byte srcReg) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x31);
    emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, srcReg);
    if (lister != null) lister.RXDR(miStart, "XOR", dstBase, dstIndex, scale, dstDisp, srcReg);
  }

  // [dstReg + dstDisp] ~=  (word)  srcReg
  public final void emitXOR_RegDisp_Reg_Word(byte dstReg, Offset disp, byte srcReg) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x31);
    emitRegDispRegOperands(dstReg, disp, srcReg);
    if (lister != null) lister.RDR(miStart, "XOR", dstReg, disp, srcReg);
  }

  // dstReg ~=  (word)  srcReg
  public final void emitXOR_Reg_Reg_Word(byte dstReg, byte srcReg) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x31);
    emitRegRegOperands(dstReg, srcReg);
    if (lister != null) lister.RR(miStart, "XOR", dstReg, srcReg);
  }

  // dstReg ~=  (word)  [srcReg + srcDisp]
  public final void emitXOR_Reg_RegDisp_Word(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x33);
    emitRegDispRegOperands(srcReg, disp, dstReg);
    if (lister != null) lister.RRD(miStart, "XOR", dstReg, srcReg, disp);
  }

  // dstReg ~=  (word)  [srcIndex<<scale + srcDisp]
  public final void emitXOR_Reg_RegOff_Word(byte dstReg, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x33);
    emitRegOffRegOperands(srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRFD(miStart, "XOR", dstReg, srcIndex, scale, srcDisp);
  }

  // dstReg ~=  (word)  [srcDisp]
  public final void emitXOR_Reg_Abs_Word(byte dstReg, Offset srcDisp) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x33);
    emitAbsRegOperands(srcDisp, dstReg);
    if (lister != null) lister.RRA(miStart, "XOR", dstReg, srcDisp);
  }

  // dstReg ~=  (word)  [srcBase + srcIndex<<scale + srcDisp]
  public final void emitXOR_Reg_RegIdx_Word(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x33);
    emitSIBRegOperands(srcBase, srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRXD(miStart, "XOR", dstReg, srcBase, srcIndex, scale, srcDisp);
  }

  // dstReg ~=  (word)  [srcReg]
  public final void emitXOR_Reg_RegInd_Word(byte dstReg, byte srcReg) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x33);
    emitRegIndirectRegOperands(srcReg, dstReg);
    if (lister != null) lister.RRN(miStart, "XOR", dstReg, srcReg);
  }

  /**
   * Generate a register--register XOR. That is,
   * <PRE>
   * [dstReg] ~=  (byte)  srcReg
   * </PRE>
   *
   * @param dstReg the destination register
   * @param srcReg the source register
   */
  public final void emitXOR_RegInd_Reg_Byte(byte dstReg, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x30);
    emitRegIndirectRegOperands(dstReg, srcReg);
    if (lister != null) lister.RNR(miStart, "XOR", dstReg, srcReg);
  }

  /**
   * Generate a register-offset--register XOR. That is,
   * <PRE>
   * [dstReg<<dstScale + dstDisp] ~=  (byte)  srcReg
   * </PRE>
   *
   * @param dstIndex the destination index register
   * @param dstScale the destination shift amount
   * @param dstDisp the destination displacement
   * @param srcReg the source register
   */
  public final void emitXOR_RegOff_Reg_Byte(byte dstIndex, short dstScale, Offset dstDisp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x30);
    emitRegOffRegOperands(dstIndex, dstScale, dstDisp, srcReg);
    if (lister != null) lister.RFDR(miStart, "XOR", dstIndex, dstScale, dstDisp, srcReg);
  }

  // [dstDisp] ~=  (byte)  srcReg
  public final void emitXOR_Abs_Reg_Byte(Offset dstDisp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x30);
    emitAbsRegOperands(dstDisp, srcReg);
    if (lister != null) lister.RAR(miStart, "XOR", dstDisp, srcReg);
  }

  // [dstBase + dstIndex<<scale + dstDisp] ~=  (byte)  srcReg
  public final void emitXOR_RegIdx_Reg_Byte(byte dstBase, byte dstIndex, short scale, Offset dstDisp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x30);
    emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, srcReg);
    if (lister != null) lister.RXDR(miStart, "XOR", dstBase, dstIndex, scale, dstDisp, srcReg);
  }

  // [dstReg + dstDisp] ~=  (byte)  srcReg
  public final void emitXOR_RegDisp_Reg_Byte(byte dstReg, Offset disp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x30);
    emitRegDispRegOperands(dstReg, disp, srcReg);
    if (lister != null) lister.RDR(miStart, "XOR", dstReg, disp, srcReg);
  }

  // dstReg ~=  (byte)  srcReg
  public final void emitXOR_Reg_Reg_Byte(byte dstReg, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x30);
    emitRegRegOperands(dstReg, srcReg);
    if (lister != null) lister.RR(miStart, "XOR", dstReg, srcReg);
  }

  // dstReg ~=  (byte)  [srcReg + srcDisp]
  public final void emitXOR_Reg_RegDisp_Byte(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x32);
    emitRegDispRegOperands(srcReg, disp, dstReg);
    if (lister != null) lister.RRD(miStart, "XOR", dstReg, srcReg, disp);
  }

  // dstReg ~=  (byte)  [srcIndex<<scale + srcDisp]
  public final void emitXOR_Reg_RegOff_Byte(byte dstReg, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x32);
    emitRegOffRegOperands(srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRFD(miStart, "XOR", dstReg, srcIndex, scale, srcDisp);
  }

  // dstReg ~=  (byte)  [srcDisp]
  public final void emitXOR_Reg_Abs_Byte(byte dstReg, Offset srcDisp) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x32);
    emitAbsRegOperands(srcDisp, dstReg);
    if (lister != null) lister.RRA(miStart, "XOR", dstReg, srcDisp);
  }

  // dstReg ~=  (byte)  [srcBase + srcIndex<<scale + srcDisp]
  public final void emitXOR_Reg_RegIdx_Byte(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x32);
    emitSIBRegOperands(srcBase, srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRXD(miStart, "XOR", dstReg, srcBase, srcIndex, scale, srcDisp);
  }

  // dstReg ~=  (byte)  [srcReg]
  public final void emitXOR_Reg_RegInd_Byte(byte dstReg, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x32);
    emitRegIndirectRegOperands(srcReg, dstReg);
    if (lister != null) lister.RRN(miStart, "XOR", dstReg, srcReg);
  }

  // dstReg ~=  imm
  public final void emitXOR_Reg_Imm(byte dstReg, int imm) {
    int miStart = mi;
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x6" is really part of the opcode
        emitRegRegOperands(dstReg, (byte) 0x6);
        emitImm8((byte)imm);
    } else if (dstReg == EAX) {
        setMachineCodes(mi++, (byte) 0x35);
        emitImm32(imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x6" is really part of the opcode
        emitRegRegOperands(dstReg, (byte) 0x6);
        emitImm32(imm);
    }
        if (lister != null) lister.RI(miStart, "XOR", dstReg, imm);
    }

  // [dstReg + dstDisp] ~=  imm
  public final void emitXOR_RegDisp_Imm(byte dstReg, Offset disp, int imm) {
    int miStart = mi;
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x6" is really part of the opcode
        emitRegDispRegOperands(dstReg, disp, (byte) 0x6);
        emitImm8((byte)imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x6" is really part of the opcode
        emitRegDispRegOperands(dstReg, disp, (byte) 0x6);
        emitImm32(imm);
    }
    if (lister != null) lister.RDI(miStart, "XOR", dstReg, disp, imm);
  }

  // [dstIndex<<scale + dstDisp] ~=  imm
  public final void emitXOR_RegOff_Imm(byte dstIndex, short scale, Offset dstDisp, int imm) {
    int miStart = mi;
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x6" is really part of the opcode
        emitRegOffRegOperands(dstIndex, scale, dstDisp, (byte) 0x6);
        emitImm8((byte)imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x6" is really part of the opcode
        emitRegOffRegOperands(dstIndex, scale, dstDisp, (byte) 0x6);
        emitImm32(imm);
    }
    if (lister != null) lister.RFDI(miStart, "XOR", dstIndex, scale, dstDisp, imm);
  }

  // [dstDisp] ~=  imm
  public final void emitXOR_Abs_Imm(Offset dstDisp, int imm) {
    int miStart = mi;
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x6" is really part of the opcode
        emitAbsRegOperands(dstDisp, (byte) 0x6);
        emitImm8((byte)imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x6" is really part of the opcode
        emitAbsRegOperands(dstDisp, (byte) 0x6);
        emitImm32(imm);
    }
    if (lister != null) lister.RAI(miStart, "XOR", dstDisp, imm);
  }

  // [dstBase + dstIndex<<scale + dstDisp] ~=  imm
  public final void emitXOR_RegIdx_Imm(byte dstBase, byte dstIndex, short scale, Offset dstDisp, int imm) {
    int miStart = mi;
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x6" is really part of the opcode
        emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, (byte) 0x6);
        emitImm8((byte)imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x6" is really part of the opcode
        emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, (byte) 0x6);
        emitImm32(imm);
    }
    if (lister != null) lister.RXDI(miStart, "XOR", dstBase, dstIndex, scale, dstDisp, imm);
  }

  // [dstReg] ~=  imm
  public final void emitXOR_RegInd_Imm(byte dstReg, int imm) {
    int miStart = mi;
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x6" is really part of the opcode
        emitRegIndirectRegOperands(dstReg, (byte) 0x6);
        emitImm8((byte)imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x6" is really part of the opcode
        emitRegIndirectRegOperands(dstReg, (byte) 0x6);
        emitImm32(imm);
    }
    if (lister != null) lister.RNI(miStart, "XOR", dstReg, imm);
  }

  // dstReg ~=  (word)  imm
  public final void emitXOR_Reg_Imm_Word(byte dstReg, int imm) {
    int miStart = mi;
            setMachineCodes(mi++, (byte) 0x66);
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x6" is really part of the opcode
        emitRegRegOperands(dstReg, (byte) 0x6);
        emitImm8((byte)imm);
    } else if (dstReg == EAX) {
        setMachineCodes(mi++, (byte) 0x35);
        emitImm16(imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x6" is really part of the opcode
        emitRegRegOperands(dstReg, (byte) 0x6);
        emitImm16(imm);
    }
        if (lister != null) lister.RI(miStart, "XOR", dstReg, imm);
    }

  // [dstReg + dstDisp] ~=  (word)  imm
  public final void emitXOR_RegDisp_Imm_Word(byte dstReg, Offset disp, int imm) {
    int miStart = mi;
            setMachineCodes(mi++, (byte) 0x66);
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x6" is really part of the opcode
        emitRegDispRegOperands(dstReg, disp, (byte) 0x6);
        emitImm8((byte)imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x6" is really part of the opcode
        emitRegDispRegOperands(dstReg, disp, (byte) 0x6);
        emitImm16(imm);
    }
    if (lister != null) lister.RDI(miStart, "XOR", dstReg, disp, imm);
  }

  // [dstIndex<<scale + dstDisp] ~=  (word)  imm
  public final void emitXOR_RegOff_Imm_Word(byte dstIndex, short scale, Offset dstDisp, int imm) {
    int miStart = mi;
            setMachineCodes(mi++, (byte) 0x66);
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x6" is really part of the opcode
        emitRegOffRegOperands(dstIndex, scale, dstDisp, (byte) 0x6);
        emitImm8((byte)imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x6" is really part of the opcode
        emitRegOffRegOperands(dstIndex, scale, dstDisp, (byte) 0x6);
        emitImm32(imm);
    }
    if (lister != null) lister.RFDI(miStart, "XOR", dstIndex, scale, dstDisp, imm);
  }

  // [dstDisp] ~=  (word)  imm
  public final void emitXOR_Abs_Imm_Word(Offset dstDisp, int imm) {
    int miStart = mi;
            setMachineCodes(mi++, (byte) 0x66);
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x6" is really part of the opcode
        emitAbsRegOperands(dstDisp, (byte) 0x6);
        emitImm8((byte)imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x6" is really part of the opcode
        emitAbsRegOperands(dstDisp, (byte) 0x6);
        emitImm32(imm);
    }
    if (lister != null) lister.RAI(miStart, "XOR", dstDisp, imm);
  }

  // [dstBase + dstIndex<<scale + dstDisp] ~=  (word)  imm
  public final void emitXOR_RegIdx_Imm_Word(byte dstBase, byte dstIndex, short scale, Offset dstDisp, int imm) {
    int miStart = mi;
            setMachineCodes(mi++, (byte) 0x66);
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x6" is really part of the opcode
        emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, (byte) 0x6);
        emitImm8((byte)imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x6" is really part of the opcode
        emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, (byte) 0x6);
        emitImm32(imm);
    }
    if (lister != null) lister.RXDI(miStart, "XOR", dstBase, dstIndex, scale, dstDisp, imm);
  }

  // [dstReg] ~=  (word)  imm
  public final void emitXOR_RegInd_Imm_Word(byte dstReg, int imm) {
    int miStart = mi;
            setMachineCodes(mi++, (byte) 0x66);
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0x83);
        // "register 0x6" is really part of the opcode
        emitRegIndirectRegOperands(dstReg, (byte) 0x6);
        emitImm8((byte)imm);
    } else {
        setMachineCodes(mi++, (byte) 0x81);
        // "register 0x6" is really part of the opcode
        emitRegIndirectRegOperands(dstReg, (byte) 0x6);
        emitImm16(imm);
    }
    if (lister != null) lister.RNI(miStart, "XOR", dstReg, imm);
  }

  // dstReg ~= (byte) imm
  public final void emitXOR_Reg_Imm_Byte(byte dstReg, int imm) {
    int miStart = mi;
    if (dstReg == EAX) {
        setMachineCodes(mi++, (byte) 0x34);
        emitImm8(imm);
    } else {
        setMachineCodes(mi++, (byte) 0x80);
        // "register 0x6" is really part of the opcode
        emitRegRegOperands(dstReg, (byte) 0x6);
        emitImm8(imm);
    }
        if (lister != null) lister.RI(miStart, "XOR", dstReg, imm);
    }

  // [dstReg + dstDisp] ~= (byte) imm
  public final void emitXOR_RegDisp_Imm_Byte(byte dstReg, Offset disp, int imm) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x80);
    // "register 0x6" is really part of the opcode
    emitRegDispRegOperands(dstReg, disp, (byte) 0x6);
    emitImm8(imm);
    if (lister != null) lister.RDI(miStart, "XOR", dstReg, disp, imm);
  }

  // [dstBase + dstIndex<<scale + dstDisp] ~= (byte) imm
  public final void emitXOR_RegIdx_Imm_Byte(byte dstBase, byte dstIndex, short scale, Offset dstDisp, int imm) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x80);
    // "register 0x6" is really part of the opcode
    emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, (byte) 0x6);
    emitImm8(imm);
    if (lister != null) lister.RXDI(miStart, "XOR", dstBase, dstIndex, scale, dstDisp, imm);
  }

  // [dstIndex<<scale + dstDisp] ~= (byte) imm
  public final void emitXOR_RegOff_Imm_Byte(byte dstIndex, short scale, Offset dstDisp, int imm) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x80);
    // "register 0x6" is really part of the opcode
    emitRegOffRegOperands(dstIndex, scale, dstDisp, (byte) 0x6);
    emitImm8(imm);
    if (lister != null) lister.RFDI(miStart, "XOR", dstIndex, scale, dstDisp, imm);
  }

  // [dstDisp] ~= (byte) imm
  public final void emitXOR_Abs_Imm_Byte(Offset dstDisp, int imm) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x80);
    // "register 0x6" is really part of the opcode
    emitAbsRegOperands(dstDisp, (byte) 0x6);
    emitImm8(imm);
    if (lister != null) lister.RAI(miStart, "XOR", dstDisp, imm);
  }

  // [dstReg] ~= (byte) imm
  public final void emitXOR_RegInd_Imm_Byte(byte dstReg, int imm) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x80);
    // "register 0x6" is really part of the opcode
    emitRegIndirectRegOperands(dstReg, (byte) 0x6);
    emitImm8(imm);
    if (lister != null) lister.RNI(miStart, "XOR", dstReg, imm);
  }

  /**
   * Generate a register--register MOV. That is,
   * <PRE>
   * [dstReg] :=  srcReg
   * </PRE>
   *
   * @param dstReg the destination register
   * @param srcReg the source register
   */
  public final void emitMOV_RegInd_Reg(byte dstReg, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x89);
    emitRegIndirectRegOperands(dstReg, srcReg);
    if (lister != null) lister.RNR(miStart, "MOV", dstReg, srcReg);
  }

  /**
   * Generate a register-offset--register MOV. That is,
   * <PRE>
   * [dstReg<<dstScale + dstDisp] :=  srcReg
   * </PRE>
   *
   * @param dstIndex the destination index register
   * @param dstScale the destination shift amount
   * @param dstDisp the destination displacement
   * @param srcReg the source register
   */
  public final void emitMOV_RegOff_Reg(byte dstIndex, short dstScale, Offset dstDisp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x89);
    emitRegOffRegOperands(dstIndex, dstScale, dstDisp, srcReg);
    if (lister != null) lister.RFDR(miStart, "MOV", dstIndex, dstScale, dstDisp, srcReg);
  }

  // [dstDisp] :=  srcReg
  public final void emitMOV_Abs_Reg(Offset dstDisp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x89);
    emitAbsRegOperands(dstDisp, srcReg);
    if (lister != null) lister.RAR(miStart, "MOV", dstDisp, srcReg);
  }

  // [dstBase + dstIndex<<scale + dstDisp] :=  srcReg
  public final void emitMOV_RegIdx_Reg(byte dstBase, byte dstIndex, short scale, Offset dstDisp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x89);
    emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, srcReg);
    if (lister != null) lister.RXDR(miStart, "MOV", dstBase, dstIndex, scale, dstDisp, srcReg);
  }

  // [dstReg + dstDisp] :=  srcReg
  public final void emitMOV_RegDisp_Reg(byte dstReg, Offset disp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x89);
    emitRegDispRegOperands(dstReg, disp, srcReg);
    if (lister != null) lister.RDR(miStart, "MOV", dstReg, disp, srcReg);
  }

  // dstReg :=  srcReg
  public final void emitMOV_Reg_Reg(byte dstReg, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x89);
    emitRegRegOperands(dstReg, srcReg);
    if (lister != null) lister.RR(miStart, "MOV", dstReg, srcReg);
  }

  // dstReg :=  [srcReg + srcDisp]
  public final void emitMOV_Reg_RegDisp(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x8B);
    emitRegDispRegOperands(srcReg, disp, dstReg);
    if (lister != null) lister.RRD(miStart, "MOV", dstReg, srcReg, disp);
  }

  // dstReg :=  [srcIndex<<scale + srcDisp]
  public final void emitMOV_Reg_RegOff(byte dstReg, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x8B);
    emitRegOffRegOperands(srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRFD(miStart, "MOV", dstReg, srcIndex, scale, srcDisp);
  }

  // dstReg :=  [srcDisp]
  public final void emitMOV_Reg_Abs(byte dstReg, Offset srcDisp) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x8B);
    emitAbsRegOperands(srcDisp, dstReg);
    if (lister != null) lister.RRA(miStart, "MOV", dstReg, srcDisp);
  }

  // dstReg :=  [srcBase + srcIndex<<scale + srcDisp]
  public final void emitMOV_Reg_RegIdx(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x8B);
    emitSIBRegOperands(srcBase, srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRXD(miStart, "MOV", dstReg, srcBase, srcIndex, scale, srcDisp);
  }

  // dstReg :=  [srcReg]
  public final void emitMOV_Reg_RegInd(byte dstReg, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x8B);
    emitRegIndirectRegOperands(srcReg, dstReg);
    if (lister != null) lister.RRN(miStart, "MOV", dstReg, srcReg);
  }

  /**
   * Generate a register--register MOV. That is,
   * <PRE>
   * [dstReg] :=  (byte)  srcReg
   * </PRE>
   *
   * @param dstReg the destination register
   * @param srcReg the source register
   */
  public final void emitMOV_RegInd_Reg_Byte(byte dstReg, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x88);
    emitRegIndirectRegOperands(dstReg, srcReg);
    if (lister != null) lister.RNR(miStart, "MOV", dstReg, srcReg);
  }

  /**
   * Generate a register-offset--register MOV. That is,
   * <PRE>
   * [dstReg<<dstScale + dstDisp] :=  (byte)  srcReg
   * </PRE>
   *
   * @param dstIndex the destination index register
   * @param dstScale the destination shift amount
   * @param dstDisp the destination displacement
   * @param srcReg the source register
   */
  public final void emitMOV_RegOff_Reg_Byte(byte dstIndex, short dstScale, Offset dstDisp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x88);
    emitRegOffRegOperands(dstIndex, dstScale, dstDisp, srcReg);
    if (lister != null) lister.RFDR(miStart, "MOV", dstIndex, dstScale, dstDisp, srcReg);
  }

  // [dstDisp] :=  (byte)  srcReg
  public final void emitMOV_Abs_Reg_Byte(Offset dstDisp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x88);
    emitAbsRegOperands(dstDisp, srcReg);
    if (lister != null) lister.RAR(miStart, "MOV", dstDisp, srcReg);
  }

  // [dstBase + dstIndex<<scale + dstDisp] :=  (byte)  srcReg
  public final void emitMOV_RegIdx_Reg_Byte(byte dstBase, byte dstIndex, short scale, Offset dstDisp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x88);
    emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, srcReg);
    if (lister != null) lister.RXDR(miStart, "MOV", dstBase, dstIndex, scale, dstDisp, srcReg);
  }

  // [dstReg + dstDisp] :=  (byte)  srcReg
  public final void emitMOV_RegDisp_Reg_Byte(byte dstReg, Offset disp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x88);
    emitRegDispRegOperands(dstReg, disp, srcReg);
    if (lister != null) lister.RDR(miStart, "MOV", dstReg, disp, srcReg);
  }

  // dstReg :=  (byte)  srcReg
  public final void emitMOV_Reg_Reg_Byte(byte dstReg, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x88);
    emitRegRegOperands(dstReg, srcReg);
    if (lister != null) lister.RR(miStart, "MOV", dstReg, srcReg);
  }

  // dstReg :=  (byte)  [srcReg + srcDisp]
  public final void emitMOV_Reg_RegDisp_Byte(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x8A);
    emitRegDispRegOperands(srcReg, disp, dstReg);
    if (lister != null) lister.RRD(miStart, "MOV", dstReg, srcReg, disp);
  }

  // dstReg :=  (byte)  [srcIndex<<scale + srcDisp]
  public final void emitMOV_Reg_RegOff_Byte(byte dstReg, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x8A);
    emitRegOffRegOperands(srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRFD(miStart, "MOV", dstReg, srcIndex, scale, srcDisp);
  }

  // dstReg :=  (byte)  [srcDisp]
  public final void emitMOV_Reg_Abs_Byte(byte dstReg, Offset srcDisp) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x8A);
    emitAbsRegOperands(srcDisp, dstReg);
    if (lister != null) lister.RRA(miStart, "MOV", dstReg, srcDisp);
  }

  // dstReg :=  (byte)  [srcBase + srcIndex<<scale + srcDisp]
  public final void emitMOV_Reg_RegIdx_Byte(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x8A);
    emitSIBRegOperands(srcBase, srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRXD(miStart, "MOV", dstReg, srcBase, srcIndex, scale, srcDisp);
  }

  // dstReg :=  (byte)  [srcReg]
  public final void emitMOV_Reg_RegInd_Byte(byte dstReg, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x8A);
    emitRegIndirectRegOperands(srcReg, dstReg);
    if (lister != null) lister.RRN(miStart, "MOV", dstReg, srcReg);
  }

  /**
   * Generate a register--register MOV. That is,
   * <PRE>
   * [dstReg] :=  (word)  srcReg
   * </PRE>
   *
   * @param dstReg the destination register
   * @param srcReg the source register
   */
  public final void emitMOV_RegInd_Reg_Word(byte dstReg, byte srcReg) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x89);
    emitRegIndirectRegOperands(dstReg, srcReg);
    if (lister != null) lister.RNR(miStart, "MOV", dstReg, srcReg);
  }

  /**
   * Generate a register-offset--register MOV. That is,
   * <PRE>
   * [dstReg<<dstScale + dstDisp] :=  (word)  srcReg
   * </PRE>
   *
   * @param dstIndex the destination index register
   * @param dstScale the destination shift amount
   * @param dstDisp the destination displacement
   * @param srcReg the source register
   */
  public final void emitMOV_RegOff_Reg_Word(byte dstIndex, short dstScale, Offset dstDisp, byte srcReg) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x89);
    emitRegOffRegOperands(dstIndex, dstScale, dstDisp, srcReg);
    if (lister != null) lister.RFDR(miStart, "MOV", dstIndex, dstScale, dstDisp, srcReg);
  }

  // [dstDisp] :=  (word)  srcReg
  public final void emitMOV_Abs_Reg_Word(Offset dstDisp, byte srcReg) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x89);
    emitAbsRegOperands(dstDisp, srcReg);
    if (lister != null) lister.RAR(miStart, "MOV", dstDisp, srcReg);
  }

  // [dstBase + dstIndex<<scale + dstDisp] :=  (word)  srcReg
  public final void emitMOV_RegIdx_Reg_Word(byte dstBase, byte dstIndex, short scale, Offset dstDisp, byte srcReg) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x89);
    emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, srcReg);
    if (lister != null) lister.RXDR(miStart, "MOV", dstBase, dstIndex, scale, dstDisp, srcReg);
  }

  // [dstReg + dstDisp] :=  (word)  srcReg
  public final void emitMOV_RegDisp_Reg_Word(byte dstReg, Offset disp, byte srcReg) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x89);
    emitRegDispRegOperands(dstReg, disp, srcReg);
    if (lister != null) lister.RDR(miStart, "MOV", dstReg, disp, srcReg);
  }

  // dstReg :=  (word)  srcReg
  public final void emitMOV_Reg_Reg_Word(byte dstReg, byte srcReg) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x89);
    emitRegRegOperands(dstReg, srcReg);
    if (lister != null) lister.RR(miStart, "MOV", dstReg, srcReg);
  }

  // dstReg :=  (word)  [srcReg + srcDisp]
  public final void emitMOV_Reg_RegDisp_Word(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x8B);
    emitRegDispRegOperands(srcReg, disp, dstReg);
    if (lister != null) lister.RRD(miStart, "MOV", dstReg, srcReg, disp);
  }

  // dstReg :=  (word)  [srcIndex<<scale + srcDisp]
  public final void emitMOV_Reg_RegOff_Word(byte dstReg, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x8B);
    emitRegOffRegOperands(srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRFD(miStart, "MOV", dstReg, srcIndex, scale, srcDisp);
  }

  // dstReg :=  (word)  [srcDisp]
  public final void emitMOV_Reg_Abs_Word(byte dstReg, Offset srcDisp) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x8B);
    emitAbsRegOperands(srcDisp, dstReg);
    if (lister != null) lister.RRA(miStart, "MOV", dstReg, srcDisp);
  }

  // dstReg :=  (word)  [srcBase + srcIndex<<scale + srcDisp]
  public final void emitMOV_Reg_RegIdx_Word(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x8B);
    emitSIBRegOperands(srcBase, srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRXD(miStart, "MOV", dstReg, srcBase, srcIndex, scale, srcDisp);
  }

  // dstReg :=  (word)  [srcReg]
  public final void emitMOV_Reg_RegInd_Word(byte dstReg, byte srcReg) {
    int miStart = mi; 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x8B);
    emitRegIndirectRegOperands(srcReg, dstReg);
    if (lister != null) lister.RRN(miStart, "MOV", dstReg, srcReg);
  }

  /**
   * Generate a register--register CMPXCHG. That is,
   * <PRE>
   * [dstReg] <->=  srcReg
   * </PRE>
   *
   * @param dstReg the destination register
   * @param srcReg the source register
   */
  public final void emitCMPXCHG_RegInd_Reg(byte dstReg, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xB1);
    emitRegIndirectRegOperands(dstReg, srcReg);
    if (lister != null) lister.RNR(miStart, "CMPXCHG", dstReg, srcReg);
  }

  /**
   * Generate a register-offset--register CMPXCHG. That is,
   * <PRE>
   * [dstReg<<dstScale + dstDisp] <->=  srcReg
   * </PRE>
   *
   * @param dstIndex the destination index register
   * @param dstScale the destination shift amount
   * @param dstDisp the destination displacement
   * @param srcReg the source register
   */
  public final void emitCMPXCHG_RegOff_Reg(byte dstIndex, short dstScale, Offset dstDisp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xB1);
    emitRegOffRegOperands(dstIndex, dstScale, dstDisp, srcReg);
    if (lister != null) lister.RFDR(miStart, "CMPXCHG", dstIndex, dstScale, dstDisp, srcReg);
  }

  // [dstDisp] <->=  srcReg
  public final void emitCMPXCHG_Abs_Reg(Offset dstDisp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xB1);
    emitAbsRegOperands(dstDisp, srcReg);
    if (lister != null) lister.RAR(miStart, "CMPXCHG", dstDisp, srcReg);
  }

  // [dstBase + dstIndex<<scale + dstDisp] <->=  srcReg
  public final void emitCMPXCHG_RegIdx_Reg(byte dstBase, byte dstIndex, short scale, Offset dstDisp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xB1);
    emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, srcReg);
    if (lister != null) lister.RXDR(miStart, "CMPXCHG", dstBase, dstIndex, scale, dstDisp, srcReg);
  }

  // [dstReg + dstDisp] <->=  srcReg
  public final void emitCMPXCHG_RegDisp_Reg(byte dstReg, Offset disp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xB1);
    emitRegDispRegOperands(dstReg, disp, srcReg);
    if (lister != null) lister.RDR(miStart, "CMPXCHG", dstReg, disp, srcReg);
  }

  // dstReg <->=  srcReg
  public final void emitCMPXCHG_Reg_Reg(byte dstReg, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xB1);
    emitRegRegOperands(dstReg, srcReg);
    if (lister != null) lister.RR(miStart, "CMPXCHG", dstReg, srcReg);
  }

  /**
   * Generate a register--register BT. That is,
   * <PRE>
   * [dstReg] BT=  srcReg
   * </PRE>
   *
   * @param dstReg the destination register
   * @param srcReg the source register
   */
  public final void emitBT_RegInd_Reg(byte dstReg, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xA3);
    emitRegIndirectRegOperands(dstReg, srcReg);
    if (lister != null) lister.RNR(miStart, "BT", dstReg, srcReg);
  }

  /**
   * Generate a register-offset--register BT. That is,
   * <PRE>
   * [dstReg<<dstScale + dstDisp] BT=  srcReg
   * </PRE>
   *
   * @param dstIndex the destination index register
   * @param dstScale the destination shift amount
   * @param dstDisp the destination displacement
   * @param srcReg the source register
   */
  public final void emitBT_RegOff_Reg(byte dstIndex, short dstScale, Offset dstDisp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xA3);
    emitRegOffRegOperands(dstIndex, dstScale, dstDisp, srcReg);
    if (lister != null) lister.RFDR(miStart, "BT", dstIndex, dstScale, dstDisp, srcReg);
  }

  // [dstDisp] BT=  srcReg
  public final void emitBT_Abs_Reg(Offset dstDisp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xA3);
    emitAbsRegOperands(dstDisp, srcReg);
    if (lister != null) lister.RAR(miStart, "BT", dstDisp, srcReg);
  }

  // [dstBase + dstIndex<<scale + dstDisp] BT=  srcReg
  public final void emitBT_RegIdx_Reg(byte dstBase, byte dstIndex, short scale, Offset dstDisp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xA3);
    emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, srcReg);
    if (lister != null) lister.RXDR(miStart, "BT", dstBase, dstIndex, scale, dstDisp, srcReg);
  }

  // [dstReg + dstDisp] BT=  srcReg
  public final void emitBT_RegDisp_Reg(byte dstReg, Offset disp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xA3);
    emitRegDispRegOperands(dstReg, disp, srcReg);
    if (lister != null) lister.RDR(miStart, "BT", dstReg, disp, srcReg);
  }

  // dstReg BT=  srcReg
  public final void emitBT_Reg_Reg(byte dstReg, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xA3);
    emitRegRegOperands(dstReg, srcReg);
    if (lister != null) lister.RR(miStart, "BT", dstReg, srcReg);
  }

  // dstReg BT=  imm
  public final void emitBT_Reg_Imm(byte dstReg, int imm) {
    int miStart = mi;
            setMachineCodes(mi++, (byte) 0x0F);
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0xBA);
        // "register 0x4" is really part of the opcode
        emitRegRegOperands(dstReg, (byte) 0x4);
        emitImm8((byte)imm);
    } else {
        throw new InternalError("Data too large for BT instruction");
    }
        if (lister != null) lister.RI(miStart, "BT", dstReg, imm);
    }

  // [dstReg + dstDisp] BT=  imm
  public final void emitBT_RegDisp_Imm(byte dstReg, Offset disp, int imm) {
    int miStart = mi;
            setMachineCodes(mi++, (byte) 0x0F);
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0xBA);
        // "register 0x4" is really part of the opcode
        emitRegDispRegOperands(dstReg, disp, (byte) 0x4);
        emitImm8((byte)imm);
    } else {
        throw new InternalError("Data too large for BT instruction");
    }
    if (lister != null) lister.RDI(miStart, "BT", dstReg, disp, imm);
  }

  // [dstIndex<<scale + dstDisp] BT=  imm
  public final void emitBT_RegOff_Imm(byte dstIndex, short scale, Offset dstDisp, int imm) {
    int miStart = mi;
            setMachineCodes(mi++, (byte) 0x0F);
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0xBA);
        // "register 0x4" is really part of the opcode
        emitRegOffRegOperands(dstIndex, scale, dstDisp, (byte) 0x4);
        emitImm8((byte)imm);
    } else {
        throw new InternalError("Data too large for BT instruction");
    }
    if (lister != null) lister.RFDI(miStart, "BT", dstIndex, scale, dstDisp, imm);
  }

  // [dstDisp] BT=  imm
  public final void emitBT_Abs_Imm(Offset dstDisp, int imm) {
    int miStart = mi;
            setMachineCodes(mi++, (byte) 0x0F);
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0xBA);
        // "register 0x4" is really part of the opcode
        emitAbsRegOperands(dstDisp, (byte) 0x4);
        emitImm8((byte)imm);
    } else {
        throw new InternalError("Data too large for BT instruction");
    }
    if (lister != null) lister.RAI(miStart, "BT", dstDisp, imm);
  }

  // [dstBase + dstIndex<<scale + dstDisp] BT=  imm
  public final void emitBT_RegIdx_Imm(byte dstBase, byte dstIndex, short scale, Offset dstDisp, int imm) {
    int miStart = mi;
            setMachineCodes(mi++, (byte) 0x0F);
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0xBA);
        // "register 0x4" is really part of the opcode
        emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, (byte) 0x4);
        emitImm8((byte)imm);
    } else {
        throw new InternalError("Data too large for BT instruction");
    }
    if (lister != null) lister.RXDI(miStart, "BT", dstBase, dstIndex, scale, dstDisp, imm);
  }

  // [dstReg] BT=  imm
  public final void emitBT_RegInd_Imm(byte dstReg, int imm) {
    int miStart = mi;
            setMachineCodes(mi++, (byte) 0x0F);
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0xBA);
        // "register 0x4" is really part of the opcode
        emitRegIndirectRegOperands(dstReg, (byte) 0x4);
        emitImm8((byte)imm);
    } else {
        throw new InternalError("Data too large for BT instruction");
    }
    if (lister != null) lister.RNI(miStart, "BT", dstReg, imm);
  }

  /**
   * Generate a register--register BTC. That is,
   * <PRE>
   * [dstReg] BTC=  srcReg
   * </PRE>
   *
   * @param dstReg the destination register
   * @param srcReg the source register
   */
  public final void emitBTC_RegInd_Reg(byte dstReg, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xBB);
    emitRegIndirectRegOperands(dstReg, srcReg);
    if (lister != null) lister.RNR(miStart, "BTC", dstReg, srcReg);
  }

  /**
   * Generate a register-offset--register BTC. That is,
   * <PRE>
   * [dstReg<<dstScale + dstDisp] BTC=  srcReg
   * </PRE>
   *
   * @param dstIndex the destination index register
   * @param dstScale the destination shift amount
   * @param dstDisp the destination displacement
   * @param srcReg the source register
   */
  public final void emitBTC_RegOff_Reg(byte dstIndex, short dstScale, Offset dstDisp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xBB);
    emitRegOffRegOperands(dstIndex, dstScale, dstDisp, srcReg);
    if (lister != null) lister.RFDR(miStart, "BTC", dstIndex, dstScale, dstDisp, srcReg);
  }

  // [dstDisp] BTC=  srcReg
  public final void emitBTC_Abs_Reg(Offset dstDisp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xBB);
    emitAbsRegOperands(dstDisp, srcReg);
    if (lister != null) lister.RAR(miStart, "BTC", dstDisp, srcReg);
  }

  // [dstBase + dstIndex<<scale + dstDisp] BTC=  srcReg
  public final void emitBTC_RegIdx_Reg(byte dstBase, byte dstIndex, short scale, Offset dstDisp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xBB);
    emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, srcReg);
    if (lister != null) lister.RXDR(miStart, "BTC", dstBase, dstIndex, scale, dstDisp, srcReg);
  }

  // [dstReg + dstDisp] BTC=  srcReg
  public final void emitBTC_RegDisp_Reg(byte dstReg, Offset disp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xBB);
    emitRegDispRegOperands(dstReg, disp, srcReg);
    if (lister != null) lister.RDR(miStart, "BTC", dstReg, disp, srcReg);
  }

  // dstReg BTC=  srcReg
  public final void emitBTC_Reg_Reg(byte dstReg, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xBB);
    emitRegRegOperands(dstReg, srcReg);
    if (lister != null) lister.RR(miStart, "BTC", dstReg, srcReg);
  }

  // dstReg BTC=  imm
  public final void emitBTC_Reg_Imm(byte dstReg, int imm) {
    int miStart = mi;
            setMachineCodes(mi++, (byte) 0x0F);
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0xBA);
        // "register 0x7" is really part of the opcode
        emitRegRegOperands(dstReg, (byte) 0x7);
        emitImm8((byte)imm);
    } else {
        throw new InternalError("Data too large for BTC instruction");
    }
        if (lister != null) lister.RI(miStart, "BTC", dstReg, imm);
    }

  // [dstReg + dstDisp] BTC=  imm
  public final void emitBTC_RegDisp_Imm(byte dstReg, Offset disp, int imm) {
    int miStart = mi;
            setMachineCodes(mi++, (byte) 0x0F);
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0xBA);
        // "register 0x7" is really part of the opcode
        emitRegDispRegOperands(dstReg, disp, (byte) 0x7);
        emitImm8((byte)imm);
    } else {
        throw new InternalError("Data too large for BTC instruction");
    }
    if (lister != null) lister.RDI(miStart, "BTC", dstReg, disp, imm);
  }

  // [dstIndex<<scale + dstDisp] BTC=  imm
  public final void emitBTC_RegOff_Imm(byte dstIndex, short scale, Offset dstDisp, int imm) {
    int miStart = mi;
            setMachineCodes(mi++, (byte) 0x0F);
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0xBA);
        // "register 0x7" is really part of the opcode
        emitRegOffRegOperands(dstIndex, scale, dstDisp, (byte) 0x7);
        emitImm8((byte)imm);
    } else {
        throw new InternalError("Data too large for BTC instruction");
    }
    if (lister != null) lister.RFDI(miStart, "BTC", dstIndex, scale, dstDisp, imm);
  }

  // [dstDisp] BTC=  imm
  public final void emitBTC_Abs_Imm(Offset dstDisp, int imm) {
    int miStart = mi;
            setMachineCodes(mi++, (byte) 0x0F);
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0xBA);
        // "register 0x7" is really part of the opcode
        emitAbsRegOperands(dstDisp, (byte) 0x7);
        emitImm8((byte)imm);
    } else {
        throw new InternalError("Data too large for BTC instruction");
    }
    if (lister != null) lister.RAI(miStart, "BTC", dstDisp, imm);
  }

  // [dstBase + dstIndex<<scale + dstDisp] BTC=  imm
  public final void emitBTC_RegIdx_Imm(byte dstBase, byte dstIndex, short scale, Offset dstDisp, int imm) {
    int miStart = mi;
            setMachineCodes(mi++, (byte) 0x0F);
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0xBA);
        // "register 0x7" is really part of the opcode
        emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, (byte) 0x7);
        emitImm8((byte)imm);
    } else {
        throw new InternalError("Data too large for BTC instruction");
    }
    if (lister != null) lister.RXDI(miStart, "BTC", dstBase, dstIndex, scale, dstDisp, imm);
  }

  // [dstReg] BTC=  imm
  public final void emitBTC_RegInd_Imm(byte dstReg, int imm) {
    int miStart = mi;
            setMachineCodes(mi++, (byte) 0x0F);
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0xBA);
        // "register 0x7" is really part of the opcode
        emitRegIndirectRegOperands(dstReg, (byte) 0x7);
        emitImm8((byte)imm);
    } else {
        throw new InternalError("Data too large for BTC instruction");
    }
    if (lister != null) lister.RNI(miStart, "BTC", dstReg, imm);
  }

  /**
   * Generate a register--register BTR. That is,
   * <PRE>
   * [dstReg] BTR=  srcReg
   * </PRE>
   *
   * @param dstReg the destination register
   * @param srcReg the source register
   */
  public final void emitBTR_RegInd_Reg(byte dstReg, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xB3);
    emitRegIndirectRegOperands(dstReg, srcReg);
    if (lister != null) lister.RNR(miStart, "BTR", dstReg, srcReg);
  }

  /**
   * Generate a register-offset--register BTR. That is,
   * <PRE>
   * [dstReg<<dstScale + dstDisp] BTR=  srcReg
   * </PRE>
   *
   * @param dstIndex the destination index register
   * @param dstScale the destination shift amount
   * @param dstDisp the destination displacement
   * @param srcReg the source register
   */
  public final void emitBTR_RegOff_Reg(byte dstIndex, short dstScale, Offset dstDisp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xB3);
    emitRegOffRegOperands(dstIndex, dstScale, dstDisp, srcReg);
    if (lister != null) lister.RFDR(miStart, "BTR", dstIndex, dstScale, dstDisp, srcReg);
  }

  // [dstDisp] BTR=  srcReg
  public final void emitBTR_Abs_Reg(Offset dstDisp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xB3);
    emitAbsRegOperands(dstDisp, srcReg);
    if (lister != null) lister.RAR(miStart, "BTR", dstDisp, srcReg);
  }

  // [dstBase + dstIndex<<scale + dstDisp] BTR=  srcReg
  public final void emitBTR_RegIdx_Reg(byte dstBase, byte dstIndex, short scale, Offset dstDisp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xB3);
    emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, srcReg);
    if (lister != null) lister.RXDR(miStart, "BTR", dstBase, dstIndex, scale, dstDisp, srcReg);
  }

  // [dstReg + dstDisp] BTR=  srcReg
  public final void emitBTR_RegDisp_Reg(byte dstReg, Offset disp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xB3);
    emitRegDispRegOperands(dstReg, disp, srcReg);
    if (lister != null) lister.RDR(miStart, "BTR", dstReg, disp, srcReg);
  }

  // dstReg BTR=  srcReg
  public final void emitBTR_Reg_Reg(byte dstReg, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xB3);
    emitRegRegOperands(dstReg, srcReg);
    if (lister != null) lister.RR(miStart, "BTR", dstReg, srcReg);
  }

  // dstReg BTR=  imm
  public final void emitBTR_Reg_Imm(byte dstReg, int imm) {
    int miStart = mi;
            setMachineCodes(mi++, (byte) 0x0F);
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0xBA);
        // "register 0x6" is really part of the opcode
        emitRegRegOperands(dstReg, (byte) 0x6);
        emitImm8((byte)imm);
    } else {
        throw new InternalError("Data too large for BTR instruction");
    }
        if (lister != null) lister.RI(miStart, "BTR", dstReg, imm);
    }

  // [dstReg + dstDisp] BTR=  imm
  public final void emitBTR_RegDisp_Imm(byte dstReg, Offset disp, int imm) {
    int miStart = mi;
            setMachineCodes(mi++, (byte) 0x0F);
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0xBA);
        // "register 0x6" is really part of the opcode
        emitRegDispRegOperands(dstReg, disp, (byte) 0x6);
        emitImm8((byte)imm);
    } else {
        throw new InternalError("Data too large for BTR instruction");
    }
    if (lister != null) lister.RDI(miStart, "BTR", dstReg, disp, imm);
  }

  // [dstIndex<<scale + dstDisp] BTR=  imm
  public final void emitBTR_RegOff_Imm(byte dstIndex, short scale, Offset dstDisp, int imm) {
    int miStart = mi;
            setMachineCodes(mi++, (byte) 0x0F);
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0xBA);
        // "register 0x6" is really part of the opcode
        emitRegOffRegOperands(dstIndex, scale, dstDisp, (byte) 0x6);
        emitImm8((byte)imm);
    } else {
        throw new InternalError("Data too large for BTR instruction");
    }
    if (lister != null) lister.RFDI(miStart, "BTR", dstIndex, scale, dstDisp, imm);
  }

  // [dstDisp] BTR=  imm
  public final void emitBTR_Abs_Imm(Offset dstDisp, int imm) {
    int miStart = mi;
            setMachineCodes(mi++, (byte) 0x0F);
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0xBA);
        // "register 0x6" is really part of the opcode
        emitAbsRegOperands(dstDisp, (byte) 0x6);
        emitImm8((byte)imm);
    } else {
        throw new InternalError("Data too large for BTR instruction");
    }
    if (lister != null) lister.RAI(miStart, "BTR", dstDisp, imm);
  }

  // [dstBase + dstIndex<<scale + dstDisp] BTR=  imm
  public final void emitBTR_RegIdx_Imm(byte dstBase, byte dstIndex, short scale, Offset dstDisp, int imm) {
    int miStart = mi;
            setMachineCodes(mi++, (byte) 0x0F);
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0xBA);
        // "register 0x6" is really part of the opcode
        emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, (byte) 0x6);
        emitImm8((byte)imm);
    } else {
        throw new InternalError("Data too large for BTR instruction");
    }
    if (lister != null) lister.RXDI(miStart, "BTR", dstBase, dstIndex, scale, dstDisp, imm);
  }

  // [dstReg] BTR=  imm
  public final void emitBTR_RegInd_Imm(byte dstReg, int imm) {
    int miStart = mi;
            setMachineCodes(mi++, (byte) 0x0F);
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0xBA);
        // "register 0x6" is really part of the opcode
        emitRegIndirectRegOperands(dstReg, (byte) 0x6);
        emitImm8((byte)imm);
    } else {
        throw new InternalError("Data too large for BTR instruction");
    }
    if (lister != null) lister.RNI(miStart, "BTR", dstReg, imm);
  }

  /**
   * Generate a register--register BTS. That is,
   * <PRE>
   * [dstReg] BTS=  srcReg
   * </PRE>
   *
   * @param dstReg the destination register
   * @param srcReg the source register
   */
  public final void emitBTS_RegInd_Reg(byte dstReg, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xAB);
    emitRegIndirectRegOperands(dstReg, srcReg);
    if (lister != null) lister.RNR(miStart, "BTS", dstReg, srcReg);
  }

  /**
   * Generate a register-offset--register BTS. That is,
   * <PRE>
   * [dstReg<<dstScale + dstDisp] BTS=  srcReg
   * </PRE>
   *
   * @param dstIndex the destination index register
   * @param dstScale the destination shift amount
   * @param dstDisp the destination displacement
   * @param srcReg the source register
   */
  public final void emitBTS_RegOff_Reg(byte dstIndex, short dstScale, Offset dstDisp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xAB);
    emitRegOffRegOperands(dstIndex, dstScale, dstDisp, srcReg);
    if (lister != null) lister.RFDR(miStart, "BTS", dstIndex, dstScale, dstDisp, srcReg);
  }

  // [dstDisp] BTS=  srcReg
  public final void emitBTS_Abs_Reg(Offset dstDisp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xAB);
    emitAbsRegOperands(dstDisp, srcReg);
    if (lister != null) lister.RAR(miStart, "BTS", dstDisp, srcReg);
  }

  // [dstBase + dstIndex<<scale + dstDisp] BTS=  srcReg
  public final void emitBTS_RegIdx_Reg(byte dstBase, byte dstIndex, short scale, Offset dstDisp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xAB);
    emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, srcReg);
    if (lister != null) lister.RXDR(miStart, "BTS", dstBase, dstIndex, scale, dstDisp, srcReg);
  }

  // [dstReg + dstDisp] BTS=  srcReg
  public final void emitBTS_RegDisp_Reg(byte dstReg, Offset disp, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xAB);
    emitRegDispRegOperands(dstReg, disp, srcReg);
    if (lister != null) lister.RDR(miStart, "BTS", dstReg, disp, srcReg);
  }

  // dstReg BTS=  srcReg
  public final void emitBTS_Reg_Reg(byte dstReg, byte srcReg) {
    int miStart = mi; 
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xAB);
    emitRegRegOperands(dstReg, srcReg);
    if (lister != null) lister.RR(miStart, "BTS", dstReg, srcReg);
  }

  // dstReg BTS=  imm
  public final void emitBTS_Reg_Imm(byte dstReg, int imm) {
    int miStart = mi;
            setMachineCodes(mi++, (byte) 0x0F);
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0xBA);
        // "register 0x5" is really part of the opcode
        emitRegRegOperands(dstReg, (byte) 0x5);
        emitImm8((byte)imm);
    } else {
        throw new InternalError("Data too large for BTS instruction");
    }
        if (lister != null) lister.RI(miStart, "BTS", dstReg, imm);
    }

  // [dstReg + dstDisp] BTS=  imm
  public final void emitBTS_RegDisp_Imm(byte dstReg, Offset disp, int imm) {
    int miStart = mi;
            setMachineCodes(mi++, (byte) 0x0F);
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0xBA);
        // "register 0x5" is really part of the opcode
        emitRegDispRegOperands(dstReg, disp, (byte) 0x5);
        emitImm8((byte)imm);
    } else {
        throw new InternalError("Data too large for BTS instruction");
    }
    if (lister != null) lister.RDI(miStart, "BTS", dstReg, disp, imm);
  }

  // [dstIndex<<scale + dstDisp] BTS=  imm
  public final void emitBTS_RegOff_Imm(byte dstIndex, short scale, Offset dstDisp, int imm) {
    int miStart = mi;
            setMachineCodes(mi++, (byte) 0x0F);
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0xBA);
        // "register 0x5" is really part of the opcode
        emitRegOffRegOperands(dstIndex, scale, dstDisp, (byte) 0x5);
        emitImm8((byte)imm);
    } else {
        throw new InternalError("Data too large for BTS instruction");
    }
    if (lister != null) lister.RFDI(miStart, "BTS", dstIndex, scale, dstDisp, imm);
  }

  // [dstDisp] BTS=  imm
  public final void emitBTS_Abs_Imm(Offset dstDisp, int imm) {
    int miStart = mi;
            setMachineCodes(mi++, (byte) 0x0F);
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0xBA);
        // "register 0x5" is really part of the opcode
        emitAbsRegOperands(dstDisp, (byte) 0x5);
        emitImm8((byte)imm);
    } else {
        throw new InternalError("Data too large for BTS instruction");
    }
    if (lister != null) lister.RAI(miStart, "BTS", dstDisp, imm);
  }

  // [dstBase + dstIndex<<scale + dstDisp] BTS=  imm
  public final void emitBTS_RegIdx_Imm(byte dstBase, byte dstIndex, short scale, Offset dstDisp, int imm) {
    int miStart = mi;
            setMachineCodes(mi++, (byte) 0x0F);
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0xBA);
        // "register 0x5" is really part of the opcode
        emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, (byte) 0x5);
        emitImm8((byte)imm);
    } else {
        throw new InternalError("Data too large for BTS instruction");
    }
    if (lister != null) lister.RXDI(miStart, "BTS", dstBase, dstIndex, scale, dstDisp, imm);
  }

  // [dstReg] BTS=  imm
  public final void emitBTS_RegInd_Imm(byte dstReg, int imm) {
    int miStart = mi;
            setMachineCodes(mi++, (byte) 0x0F);
    if (fits(imm,8)) {
        setMachineCodes(mi++, (byte) 0xBA);
        // "register 0x5" is really part of the opcode
        emitRegIndirectRegOperands(dstReg, (byte) 0x5);
        emitImm8((byte)imm);
    } else {
        throw new InternalError("Data too large for BTS instruction");
    }
    if (lister != null) lister.RNI(miStart, "BTS", dstReg, imm);
  }

  // pc = {future address from label | imm}
  public final void emitCALL_ImmOrLabel(int imm, int label) {
    if (imm == 0)
        emitCALL_Label( label );
    else
        emitCALL_Imm( imm );
  }

  /**
   *  Branch to the given target with a CALL instruction
   * <PRE>
   * IP = (instruction @ label)
   * </PRE>
   *
   *  This emit method is expecting only a forward branch (that is
   * what the Label operand means); it creates a VM_ForwardReference
   * to the given label, and puts it into the assembler's list of
   * references to resolve.  This emitter knows the branch is
   * unconditional, so it uses VM_ForwardReference.UnconditionalBranch
   * as the forward reference type to create.
   *
   *  All forward branches have a label as the branch target; clients
   * can arbirarily associate labels and instructions, but must be
   * consistent in giving the chosen label as the target of branches
   * to an instruction and calling resolveForwardBranches with the
   * given label immediately before emitting the target instruction.
   * See the header comments of VM_ForwardReference for more details.
   *
   * @param label the label associated with the branch target instrucion
   *
   * @see VM_ForwardReference.UnconditionalBranch
   */
  public final void emitCALL_Label(int label) {

      // if alignment checking on, force alignment here
      if (VM.AlignmentChecking) {
        while (((mi + 5) % 4) != 0) {
          emitNOP();
        }
      }

      int miStart = mi;
      VM_ForwardReference r =
        new VM_ForwardReference.UnconditionalBranch(mi, label);
      forwardRefs = VM_ForwardReference.enqueue(forwardRefs, r);
      setMachineCodes(mi++, (byte) 0xE8);
      mi += 4; // leave space for displacement
      if (lister != null) lister.I(miStart, "CALL", label);
  }

  // pc = imm
  public final void emitCALL_Imm(int imm) {
    int miStart = mi;
        setMachineCodes(mi++, (byte) 0xE8);
        // offset of next instruction (this instruction is 5 bytes,
        // but we just accounted for one of them in the mi++ above)
        emitImm32(imm - (mi + 4));
    if (lister != null) lister.I(miStart, "CALL", imm);
  }

  // pc = dstReg
  public final void emitCALL_Reg(byte dstReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xFF);
    // "register 0x2" is really part of the CALL opcode
    emitRegRegOperands(dstReg, (byte) 0x2);
    if (lister != null) lister.R(miStart, "CALL", dstReg);
  }

  // pc = [dstReg + destDisp]
  public final void emitCALL_RegDisp(byte dstReg, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xFF);
    // "register 0x2" is really part of the CALL opcode
    emitRegDispRegOperands(dstReg, disp, (byte) 0x2);
    if (lister != null) lister.RD(miStart, "CALL", dstReg, disp);
  }

  // pc = [dstReg]
  public final void emitCALL_RegInd(byte dstReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xFF);
    // "register 0x2" is really part of the CALL opcode
    emitRegIndirectRegOperands(dstReg, (byte) 0x2);
    if (lister != null) lister.RN(miStart, "CALL", dstReg);
  }

  // pc = [dstIndex<<scale + dstDisp]
  public final void emitCALL_RegOff(byte dstIndex, short scale, Offset dstDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xFF);
    // "register 0x2" is really part of the CALL opcode
    emitRegOffRegOperands(dstIndex, scale, dstDisp, (byte) 0x2);
    if (lister != null) lister.RFD(miStart, "CALL", dstIndex, scale, dstDisp);
  }

  // pc = [dstDisp]
  public final void emitCALL_Abs(Offset dstDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xFF);
    // "register 0x2" is really part of the CALL opcode
    emitAbsRegOperands(dstDisp, (byte) 0x2);
    if (lister != null) lister.RA(miStart, "CALL", dstDisp);
  }

    // pc = [dstBase + dstIndex<<scale + dstDisp]
  public final void emitCALL_RegIdx(byte dstBase, byte dstIndex, short scale, Offset dstDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xFF);
    // "register 0x2" is really part of the CALL opcode
    emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, (byte) 0x2);
    if (lister != null) lister.RXD(miStart, "CALL", dstBase, dstIndex, scale, dstDisp);
  }

  // pc = {future address from label | imm}
  public final void emitJMP_ImmOrLabel(int imm, int label) {
    if (imm == 0)
        emitJMP_Label( label );
    else
        emitJMP_Imm( imm );
  }

  /**
   *  Branch to the given target with a JMP instruction
   * <PRE>
   * IP = (instruction @ label)
   * </PRE>
   *
   *  This emit method is expecting only a forward branch (that is
   * what the Label operand means); it creates a VM_ForwardReference
   * to the given label, and puts it into the assembler's list of
   * references to resolve.  This emitter knows the branch is
   * unconditional, so it uses VM_ForwardReference.UnconditionalBranch
   * as the forward reference type to create.
   *
   *  All forward branches have a label as the branch target; clients
   * can arbirarily associate labels and instructions, but must be
   * consistent in giving the chosen label as the target of branches
   * to an instruction and calling resolveForwardBranches with the
   * given label immediately before emitting the target instruction.
   * See the header comments of VM_ForwardReference for more details.
   *
   * @param label the label associated with the branch target instrucion
   *
   * @see VM_ForwardReference.UnconditionalBranch
   */
  public final void emitJMP_Label(int label) {

      // if alignment checking on, force alignment here
      if (VM.AlignmentChecking) {
        while (((mi + 5) % 4) != 0) {
          emitNOP();
        }
      }

      int miStart = mi;
      VM_ForwardReference r =
        new VM_ForwardReference.UnconditionalBranch(mi, label);
      forwardRefs = VM_ForwardReference.enqueue(forwardRefs, r);
      setMachineCodes(mi++, (byte) 0xE9);
      mi += 4; // leave space for displacement
      if (lister != null) lister.I(miStart, "JMP", label);
  }

  // pc = imm
  public final void emitJMP_Imm(int imm) {
    int miStart = mi;
    // can we fit the offset from the next instruction into 8
    // bits, assuming this instruction is 2 bytes (which it will
        // be if the offset fits into 8 bits)?
    int relOffset = imm - (mi + 2);
        if (fits(relOffset,8)) {
        // yes, so use short form.
        setMachineCodes(mi++, (byte) 0xEB);
        emitImm8((byte) relOffset);
        } else {
        // no, must use 32 bit offset and ignore relOffset to
        // account for the fact that this instruction now has to
        // be 5 bytes long.
        setMachineCodes(mi++, (byte) 0xE9);
        // offset of next instruction (this instruction is 5 bytes,
        // but we just accounted for one of them in the mi++ above)
        emitImm32(imm - (mi + 4));
    }
    if (lister != null) lister.I(miStart, "JMP", imm);
  }

  // pc = dstReg
  public final void emitJMP_Reg(byte dstReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xFF);
    // "register 0x4" is really part of the JMP opcode
    emitRegRegOperands(dstReg, (byte) 0x4);
    if (lister != null) lister.R(miStart, "JMP", dstReg);
  }

  // pc = [dstReg + destDisp]
  public final void emitJMP_RegDisp(byte dstReg, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xFF);
    // "register 0x4" is really part of the JMP opcode
    emitRegDispRegOperands(dstReg, disp, (byte) 0x4);
    if (lister != null) lister.RD(miStart, "JMP", dstReg, disp);
  }

  // pc = [dstReg]
  public final void emitJMP_RegInd(byte dstReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xFF);
    // "register 0x4" is really part of the JMP opcode
    emitRegIndirectRegOperands(dstReg, (byte) 0x4);
    if (lister != null) lister.RN(miStart, "JMP", dstReg);
  }

  // pc = [dstIndex<<scale + dstDisp]
  public final void emitJMP_RegOff(byte dstIndex, short scale, Offset dstDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xFF);
    // "register 0x4" is really part of the JMP opcode
    emitRegOffRegOperands(dstIndex, scale, dstDisp, (byte) 0x4);
    if (lister != null) lister.RFD(miStart, "JMP", dstIndex, scale, dstDisp);
  }

  // pc = [dstDisp]
  public final void emitJMP_Abs(Offset dstDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xFF);
    // "register 0x4" is really part of the JMP opcode
    emitAbsRegOperands(dstDisp, (byte) 0x4);
    if (lister != null) lister.RA(miStart, "JMP", dstDisp);
  }

    // pc = [dstBase + dstIndex<<scale + dstDisp]
  public final void emitJMP_RegIdx(byte dstBase, byte dstIndex, short scale, Offset dstDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xFF);
    // "register 0x4" is really part of the JMP opcode
    emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, (byte) 0x4);
    if (lister != null) lister.RXD(miStart, "JMP", dstBase, dstIndex, scale, dstDisp);
  }

  // --  reg
  public void emitDEC_Reg(byte reg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) (0x48 | reg));
    if (lister != null) lister.R(miStart, "DEC", reg);
  }
  // --  [reg + disp]
  public final void emitDEC_RegDisp(byte reg, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xFF);
    // "register 0x1" is really part of the opcode
    emitRegDispRegOperands(reg, disp, (byte) 0x1);
    if (lister != null) lister.RD(miStart, "DEC", reg, disp);
  }

  // --  [reg]
  public final void emitDEC_RegInd(byte reg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xFF);
    // "register 0x1" is really part of the opcode
    emitRegIndirectRegOperands(reg, (byte) 0x1);
    if (lister != null) lister.RN(miStart, "DEC", reg);
  }

  // --  [index<<scale + disp]
  public final void emitDEC_RegOff(byte index, short scale, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xFF);
    // "register 0x1" is really part of the opcode
    emitRegOffRegOperands(index, scale, disp, (byte) 0x1);
    if (lister != null) lister.RFD(miStart, "DEC", index, scale, disp);
  }

  // --  [disp]
  public final void emitDEC_Abs(Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xFF);
    // "register 0x1" is really part of the opcode
    emitAbsRegOperands(disp, (byte) 0x1);
    if (lister != null) lister.RA(miStart, "DEC", disp);
  }

  // --  [base + index<<scale + disp]
  public final void emitDEC_RegIdx(byte base, byte index, short scale, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xFF);
    // "register 0x1" is really part of the opcode
    emitSIBRegOperands(base, index, scale, disp, (byte) 0x1);
    if (lister != null) lister.RXD(miStart, "DEC", base, index, scale, disp);
  }

  // ++  reg
  public void emitINC_Reg(byte reg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) (0x40 | reg));
    if (lister != null) lister.R(miStart, "INC", reg);
  }
  // ++  [reg + disp]
  public final void emitINC_RegDisp(byte reg, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xFF);
    // "register 0x0" is really part of the opcode
    emitRegDispRegOperands(reg, disp, (byte) 0x0);
    if (lister != null) lister.RD(miStart, "INC", reg, disp);
  }

  // ++  [reg]
  public final void emitINC_RegInd(byte reg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xFF);
    // "register 0x0" is really part of the opcode
    emitRegIndirectRegOperands(reg, (byte) 0x0);
    if (lister != null) lister.RN(miStart, "INC", reg);
  }

  // ++  [index<<scale + disp]
  public final void emitINC_RegOff(byte index, short scale, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xFF);
    // "register 0x0" is really part of the opcode
    emitRegOffRegOperands(index, scale, disp, (byte) 0x0);
    if (lister != null) lister.RFD(miStart, "INC", index, scale, disp);
  }

  // ++  [disp]
  public final void emitINC_Abs(Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xFF);
    // "register 0x0" is really part of the opcode
    emitAbsRegOperands(disp, (byte) 0x0);
    if (lister != null) lister.RA(miStart, "INC", disp);
  }

  // ++  [base + index<<scale + disp]
  public final void emitINC_RegIdx(byte base, byte index, short scale, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xFF);
    // "register 0x0" is really part of the opcode
    emitSIBRegOperands(base, index, scale, disp, (byte) 0x0);
    if (lister != null) lister.RXD(miStart, "INC", base, index, scale, disp);
  }

  // -  reg
  public final void emitNEG_Reg(byte reg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF7);
    emitRegRegOperands(reg, (byte) 0x3);
    if (lister != null) lister.R(miStart, "NEG", reg);
  }
  // -  [reg + disp]
  public final void emitNEG_RegDisp(byte reg, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF7);
    // "register 0x3" is really part of the opcode
    emitRegDispRegOperands(reg, disp, (byte) 0x3);
    if (lister != null) lister.RD(miStart, "NEG", reg, disp);
  }

  // -  [reg]
  public final void emitNEG_RegInd(byte reg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF7);
    // "register 0x3" is really part of the opcode
    emitRegIndirectRegOperands(reg, (byte) 0x3);
    if (lister != null) lister.RN(miStart, "NEG", reg);
  }

  // -  [index<<scale + disp]
  public final void emitNEG_RegOff(byte index, short scale, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF7);
    // "register 0x3" is really part of the opcode
    emitRegOffRegOperands(index, scale, disp, (byte) 0x3);
    if (lister != null) lister.RFD(miStart, "NEG", index, scale, disp);
  }

  // -  [disp]
  public final void emitNEG_Abs(Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF7);
    // "register 0x3" is really part of the opcode
    emitAbsRegOperands(disp, (byte) 0x3);
    if (lister != null) lister.RA(miStart, "NEG", disp);
  }

  // -  [base + index<<scale + disp]
  public final void emitNEG_RegIdx(byte base, byte index, short scale, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF7);
    // "register 0x3" is really part of the opcode
    emitSIBRegOperands(base, index, scale, disp, (byte) 0x3);
    if (lister != null) lister.RXD(miStart, "NEG", base, index, scale, disp);
  }

  // ~  reg
  public final void emitNOT_Reg(byte reg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF7);
    emitRegRegOperands(reg, (byte) 0x2);
    if (lister != null) lister.R(miStart, "NOT", reg);
  }
  // ~  [reg + disp]
  public final void emitNOT_RegDisp(byte reg, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF7);
    // "register 0x2" is really part of the opcode
    emitRegDispRegOperands(reg, disp, (byte) 0x2);
    if (lister != null) lister.RD(miStart, "NOT", reg, disp);
  }

  // ~  [reg]
  public final void emitNOT_RegInd(byte reg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF7);
    // "register 0x2" is really part of the opcode
    emitRegIndirectRegOperands(reg, (byte) 0x2);
    if (lister != null) lister.RN(miStart, "NOT", reg);
  }

  // ~  [index<<scale + disp]
  public final void emitNOT_RegOff(byte index, short scale, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF7);
    // "register 0x2" is really part of the opcode
    emitRegOffRegOperands(index, scale, disp, (byte) 0x2);
    if (lister != null) lister.RFD(miStart, "NOT", index, scale, disp);
  }

  // ~  [disp]
  public final void emitNOT_Abs(Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF7);
    // "register 0x2" is really part of the opcode
    emitAbsRegOperands(disp, (byte) 0x2);
    if (lister != null) lister.RA(miStart, "NOT", disp);
  }

  // ~  [base + index<<scale + disp]
  public final void emitNOT_RegIdx(byte base, byte index, short scale, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF7);
    // "register 0x2" is really part of the opcode
    emitSIBRegOperands(base, index, scale, disp, (byte) 0x2);
    if (lister != null) lister.RXD(miStart, "NOT", base, index, scale, disp);
  }

  // --  (word)  reg
  public void emitDEC_Reg_Word(byte reg) {
    int miStart = mi;
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) (0x48 | reg));
    if (lister != null) lister.R(miStart, "DEC", reg);
  }
  // --  (word)  [reg + disp]
  public final void emitDEC_RegDisp_Word(byte reg, Offset disp) {
    int miStart = mi;
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0xFF);
    // "register 0x1" is really part of the opcode
    emitRegDispRegOperands(reg, disp, (byte) 0x1);
    if (lister != null) lister.RD(miStart, "DEC", reg, disp);
  }

  // --  (word)  [reg]
  public final void emitDEC_RegInd_Word(byte reg) {
    int miStart = mi;
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0xFF);
    // "register 0x1" is really part of the opcode
    emitRegIndirectRegOperands(reg, (byte) 0x1);
    if (lister != null) lister.RN(miStart, "DEC", reg);
  }

  // --  (word)  [index<<scale + disp]
  public final void emitDEC_RegOff_Word(byte index, short scale, Offset disp) {
    int miStart = mi;
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0xFF);
    // "register 0x1" is really part of the opcode
    emitRegOffRegOperands(index, scale, disp, (byte) 0x1);
    if (lister != null) lister.RFD(miStart, "DEC", index, scale, disp);
  }

  // --  (word)  [disp]
  public final void emitDEC_Abs_Word(Offset disp) {
    int miStart = mi;
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0xFF);
    // "register 0x1" is really part of the opcode
    emitAbsRegOperands(disp, (byte) 0x1);
    if (lister != null) lister.RA(miStart, "DEC", disp);
  }

  // --  (word)  [base + index<<scale + disp]
  public final void emitDEC_RegIdx_Word(byte base, byte index, short scale, Offset disp) {
    int miStart = mi;
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0xFF);
    // "register 0x1" is really part of the opcode
    emitSIBRegOperands(base, index, scale, disp, (byte) 0x1);
    if (lister != null) lister.RXD(miStart, "DEC", base, index, scale, disp);
  }

  // ++  (word)  reg
  public void emitINC_Reg_Word(byte reg) {
    int miStart = mi;
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) (0x40 | reg));
    if (lister != null) lister.R(miStart, "INC", reg);
  }
  // ++  (word)  [reg + disp]
  public final void emitINC_RegDisp_Word(byte reg, Offset disp) {
    int miStart = mi;
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0xFF);
    // "register 0x0" is really part of the opcode
    emitRegDispRegOperands(reg, disp, (byte) 0x0);
    if (lister != null) lister.RD(miStart, "INC", reg, disp);
  }

  // ++  (word)  [reg]
  public final void emitINC_RegInd_Word(byte reg) {
    int miStart = mi;
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0xFF);
    // "register 0x0" is really part of the opcode
    emitRegIndirectRegOperands(reg, (byte) 0x0);
    if (lister != null) lister.RN(miStart, "INC", reg);
  }

  // ++  (word)  [index<<scale + disp]
  public final void emitINC_RegOff_Word(byte index, short scale, Offset disp) {
    int miStart = mi;
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0xFF);
    // "register 0x0" is really part of the opcode
    emitRegOffRegOperands(index, scale, disp, (byte) 0x0);
    if (lister != null) lister.RFD(miStart, "INC", index, scale, disp);
  }

  // ++  (word)  [disp]
  public final void emitINC_Abs_Word(Offset disp) {
    int miStart = mi;
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0xFF);
    // "register 0x0" is really part of the opcode
    emitAbsRegOperands(disp, (byte) 0x0);
    if (lister != null) lister.RA(miStart, "INC", disp);
  }

  // ++  (word)  [base + index<<scale + disp]
  public final void emitINC_RegIdx_Word(byte base, byte index, short scale, Offset disp) {
    int miStart = mi;
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0xFF);
    // "register 0x0" is really part of the opcode
    emitSIBRegOperands(base, index, scale, disp, (byte) 0x0);
    if (lister != null) lister.RXD(miStart, "INC", base, index, scale, disp);
  }

  // -  (word)  reg
  public final void emitNEG_Reg_Word(byte reg) {
    int miStart = mi;
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0xF7);
    emitRegRegOperands(reg, (byte) 0x3);
    if (lister != null) lister.R(miStart, "NEG", reg);
  }
  // -  (word)  [reg + disp]
  public final void emitNEG_RegDisp_Word(byte reg, Offset disp) {
    int miStart = mi;
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0xF7);
    // "register 0x3" is really part of the opcode
    emitRegDispRegOperands(reg, disp, (byte) 0x3);
    if (lister != null) lister.RD(miStart, "NEG", reg, disp);
  }

  // -  (word)  [reg]
  public final void emitNEG_RegInd_Word(byte reg) {
    int miStart = mi;
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0xF7);
    // "register 0x3" is really part of the opcode
    emitRegIndirectRegOperands(reg, (byte) 0x3);
    if (lister != null) lister.RN(miStart, "NEG", reg);
  }

  // -  (word)  [index<<scale + disp]
  public final void emitNEG_RegOff_Word(byte index, short scale, Offset disp) {
    int miStart = mi;
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0xF7);
    // "register 0x3" is really part of the opcode
    emitRegOffRegOperands(index, scale, disp, (byte) 0x3);
    if (lister != null) lister.RFD(miStart, "NEG", index, scale, disp);
  }

  // -  (word)  [disp]
  public final void emitNEG_Abs_Word(Offset disp) {
    int miStart = mi;
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0xF7);
    // "register 0x3" is really part of the opcode
    emitAbsRegOperands(disp, (byte) 0x3);
    if (lister != null) lister.RA(miStart, "NEG", disp);
  }

  // -  (word)  [base + index<<scale + disp]
  public final void emitNEG_RegIdx_Word(byte base, byte index, short scale, Offset disp) {
    int miStart = mi;
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0xF7);
    // "register 0x3" is really part of the opcode
    emitSIBRegOperands(base, index, scale, disp, (byte) 0x3);
    if (lister != null) lister.RXD(miStart, "NEG", base, index, scale, disp);
  }

  // ~  (word)  reg
  public final void emitNOT_Reg_Word(byte reg) {
    int miStart = mi;
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0xF7);
    emitRegRegOperands(reg, (byte) 0x2);
    if (lister != null) lister.R(miStart, "NOT", reg);
  }
  // ~  (word)  [reg + disp]
  public final void emitNOT_RegDisp_Word(byte reg, Offset disp) {
    int miStart = mi;
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0xF7);
    // "register 0x2" is really part of the opcode
    emitRegDispRegOperands(reg, disp, (byte) 0x2);
    if (lister != null) lister.RD(miStart, "NOT", reg, disp);
  }

  // ~  (word)  [reg]
  public final void emitNOT_RegInd_Word(byte reg) {
    int miStart = mi;
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0xF7);
    // "register 0x2" is really part of the opcode
    emitRegIndirectRegOperands(reg, (byte) 0x2);
    if (lister != null) lister.RN(miStart, "NOT", reg);
  }

  // ~  (word)  [index<<scale + disp]
  public final void emitNOT_RegOff_Word(byte index, short scale, Offset disp) {
    int miStart = mi;
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0xF7);
    // "register 0x2" is really part of the opcode
    emitRegOffRegOperands(index, scale, disp, (byte) 0x2);
    if (lister != null) lister.RFD(miStart, "NOT", index, scale, disp);
  }

  // ~  (word)  [disp]
  public final void emitNOT_Abs_Word(Offset disp) {
    int miStart = mi;
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0xF7);
    // "register 0x2" is really part of the opcode
    emitAbsRegOperands(disp, (byte) 0x2);
    if (lister != null) lister.RA(miStart, "NOT", disp);
  }

  // ~  (word)  [base + index<<scale + disp]
  public final void emitNOT_RegIdx_Word(byte base, byte index, short scale, Offset disp) {
    int miStart = mi;
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0xF7);
    // "register 0x2" is really part of the opcode
    emitSIBRegOperands(base, index, scale, disp, (byte) 0x2);
    if (lister != null) lister.RXD(miStart, "NOT", base, index, scale, disp);
  }

  // --  (byte)  reg
  public final void emitDEC_Reg_Byte(byte reg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xFE);
    emitRegRegOperands(reg, (byte) 0x1);
    if (lister != null) lister.R(miStart, "DEC", reg);
  }
  // --  (byte)  [reg + disp]
  public final void emitDEC_RegDisp_Byte(byte reg, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xFE);
    // "register 0x1" is really part of the opcode
    emitRegDispRegOperands(reg, disp, (byte) 0x1);
    if (lister != null) lister.RD(miStart, "DEC", reg, disp);
  }

  // --  (byte)  [reg]
  public final void emitDEC_RegInd_Byte(byte reg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xFE);
    // "register 0x1" is really part of the opcode
    emitRegIndirectRegOperands(reg, (byte) 0x1);
    if (lister != null) lister.RN(miStart, "DEC", reg);
  }

  // --  (byte)  [index<<scale + disp]
  public final void emitDEC_RegOff_Byte(byte index, short scale, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xFE);
    // "register 0x1" is really part of the opcode
    emitRegOffRegOperands(index, scale, disp, (byte) 0x1);
    if (lister != null) lister.RFD(miStart, "DEC", index, scale, disp);
  }

  // --  (byte)  [disp]
  public final void emitDEC_Abs_Byte(Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xFE);
    // "register 0x1" is really part of the opcode
    emitAbsRegOperands(disp, (byte) 0x1);
    if (lister != null) lister.RA(miStart, "DEC", disp);
  }

  // --  (byte)  [base + index<<scale + disp]
  public final void emitDEC_RegIdx_Byte(byte base, byte index, short scale, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xFE);
    // "register 0x1" is really part of the opcode
    emitSIBRegOperands(base, index, scale, disp, (byte) 0x1);
    if (lister != null) lister.RXD(miStart, "DEC", base, index, scale, disp);
  }

  // ++  (byte)  reg
  public final void emitINC_Reg_Byte(byte reg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xFE);
    emitRegRegOperands(reg, (byte) 0x0);
    if (lister != null) lister.R(miStart, "INC", reg);
  }
  // ++  (byte)  [reg + disp]
  public final void emitINC_RegDisp_Byte(byte reg, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xFE);
    // "register 0x0" is really part of the opcode
    emitRegDispRegOperands(reg, disp, (byte) 0x0);
    if (lister != null) lister.RD(miStart, "INC", reg, disp);
  }

  // ++  (byte)  [reg]
  public final void emitINC_RegInd_Byte(byte reg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xFE);
    // "register 0x0" is really part of the opcode
    emitRegIndirectRegOperands(reg, (byte) 0x0);
    if (lister != null) lister.RN(miStart, "INC", reg);
  }

  // ++  (byte)  [index<<scale + disp]
  public final void emitINC_RegOff_Byte(byte index, short scale, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xFE);
    // "register 0x0" is really part of the opcode
    emitRegOffRegOperands(index, scale, disp, (byte) 0x0);
    if (lister != null) lister.RFD(miStart, "INC", index, scale, disp);
  }

  // ++  (byte)  [disp]
  public final void emitINC_Abs_Byte(Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xFE);
    // "register 0x0" is really part of the opcode
    emitAbsRegOperands(disp, (byte) 0x0);
    if (lister != null) lister.RA(miStart, "INC", disp);
  }

  // ++  (byte)  [base + index<<scale + disp]
  public final void emitINC_RegIdx_Byte(byte base, byte index, short scale, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xFE);
    // "register 0x0" is really part of the opcode
    emitSIBRegOperands(base, index, scale, disp, (byte) 0x0);
    if (lister != null) lister.RXD(miStart, "INC", base, index, scale, disp);
  }

  // -  (byte)  reg
  public final void emitNEG_Reg_Byte(byte reg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF6);
    emitRegRegOperands(reg, (byte) 0x3);
    if (lister != null) lister.R(miStart, "NEG", reg);
  }
  // -  (byte)  [reg + disp]
  public final void emitNEG_RegDisp_Byte(byte reg, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF6);
    // "register 0x3" is really part of the opcode
    emitRegDispRegOperands(reg, disp, (byte) 0x3);
    if (lister != null) lister.RD(miStart, "NEG", reg, disp);
  }

  // -  (byte)  [reg]
  public final void emitNEG_RegInd_Byte(byte reg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF6);
    // "register 0x3" is really part of the opcode
    emitRegIndirectRegOperands(reg, (byte) 0x3);
    if (lister != null) lister.RN(miStart, "NEG", reg);
  }

  // -  (byte)  [index<<scale + disp]
  public final void emitNEG_RegOff_Byte(byte index, short scale, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF6);
    // "register 0x3" is really part of the opcode
    emitRegOffRegOperands(index, scale, disp, (byte) 0x3);
    if (lister != null) lister.RFD(miStart, "NEG", index, scale, disp);
  }

  // -  (byte)  [disp]
  public final void emitNEG_Abs_Byte(Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF6);
    // "register 0x3" is really part of the opcode
    emitAbsRegOperands(disp, (byte) 0x3);
    if (lister != null) lister.RA(miStart, "NEG", disp);
  }

  // -  (byte)  [base + index<<scale + disp]
  public final void emitNEG_RegIdx_Byte(byte base, byte index, short scale, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF6);
    // "register 0x3" is really part of the opcode
    emitSIBRegOperands(base, index, scale, disp, (byte) 0x3);
    if (lister != null) lister.RXD(miStart, "NEG", base, index, scale, disp);
  }

  // ~  (byte)  reg
  public final void emitNOT_Reg_Byte(byte reg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF6);
    emitRegRegOperands(reg, (byte) 0x2);
    if (lister != null) lister.R(miStart, "NOT", reg);
  }
  // ~  (byte)  [reg + disp]
  public final void emitNOT_RegDisp_Byte(byte reg, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF6);
    // "register 0x2" is really part of the opcode
    emitRegDispRegOperands(reg, disp, (byte) 0x2);
    if (lister != null) lister.RD(miStart, "NOT", reg, disp);
  }

  // ~  (byte)  [reg]
  public final void emitNOT_RegInd_Byte(byte reg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF6);
    // "register 0x2" is really part of the opcode
    emitRegIndirectRegOperands(reg, (byte) 0x2);
    if (lister != null) lister.RN(miStart, "NOT", reg);
  }

  // ~  (byte)  [index<<scale + disp]
  public final void emitNOT_RegOff_Byte(byte index, short scale, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF6);
    // "register 0x2" is really part of the opcode
    emitRegOffRegOperands(index, scale, disp, (byte) 0x2);
    if (lister != null) lister.RFD(miStart, "NOT", index, scale, disp);
  }

  // ~  (byte)  [disp]
  public final void emitNOT_Abs_Byte(Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF6);
    // "register 0x2" is really part of the opcode
    emitAbsRegOperands(disp, (byte) 0x2);
    if (lister != null) lister.RA(miStart, "NOT", disp);
  }

  // ~  (byte)  [base + index<<scale + disp]
  public final void emitNOT_RegIdx_Byte(byte base, byte index, short scale, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF6);
    // "register 0x2" is really part of the opcode
    emitSIBRegOperands(base, index, scale, disp, (byte) 0x2);
    if (lister != null) lister.RXD(miStart, "NOT", base, index, scale, disp);
  }

  // BSWAP reg
  public final void emitBSWAP_Reg(byte reg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) (0xC8+reg));
    if (lister != null) lister.R(miStart, "bswap", reg);
  }
  // EAX:EDX = EAX / srcReg
  public final void emitDIV_Reg_Reg(byte dstReg, byte srcReg) {
      int miStart = mi;
      if (VM.VerifyAssertions) VM._assert(dstReg == EAX);
      setMachineCodes(mi++, (byte) 0xF7);
      emitRegRegOperands(srcReg, (byte) 0x6);
      if (lister != null) lister.RR(miStart, "DIV", dstReg, srcReg);
  }

  // EAX:EDX = EAX / [srcReg + disp]
  public final void emitDIV_Reg_RegDisp(byte dstReg, byte srcReg, Offset disp) {
      int miStart = mi;
      if (VM.VerifyAssertions) VM._assert(dstReg == EAX);
      setMachineCodes(mi++, (byte) 0xF7);
      emitRegDispRegOperands(srcReg, disp, (byte) 0x6);
      if (lister != null) lister.RRD(miStart, "DIV", dstReg, srcReg, disp);
  }

  // EAX:EDX = EAX / [srcReg]
  public final void emitDIV_Reg_RegInd(byte dstReg, byte srcReg) {
      int miStart = mi;
      if (VM.VerifyAssertions) VM._assert(dstReg == EAX);
      setMachineCodes(mi++, (byte) 0xF7);
      emitRegIndirectRegOperands(srcReg, (byte) 0x6);
      if (lister != null) lister.RRN(miStart, "DIV", dstReg, srcReg);
  }

  // EAX:EDX = EAX / [baseReg + idxRef<<scale + disp]
  public final void emitDIV_Reg_RegIdx(byte dstReg, byte baseReg, byte idxReg, short scale, Offset disp) {
      int miStart = mi;
      if (VM.VerifyAssertions) VM._assert(dstReg == EAX);
      setMachineCodes(mi++, (byte) 0xF7);
      emitSIBRegOperands(baseReg, idxReg, scale, disp, (byte) 0x6);
      if (lister != null) lister.RRXD(miStart, "DIV", dstReg, baseReg, idxReg, scale, disp);
  }

  // EAX:EDX = EAX / [idxRef<<scale + disp]
  public final void emitDIV_Reg_RegOff(byte dstReg, byte idxReg, short scale, Offset disp) {
      int miStart = mi;
      if (VM.VerifyAssertions) VM._assert(dstReg == EAX);
      setMachineCodes(mi++, (byte) 0xF7);
      emitRegOffRegOperands(idxReg, scale, disp, (byte) 0x6);
      if (lister != null) lister.RRFD(miStart, "DIV", dstReg, idxReg, scale, disp);
  }

  // EAX:EDX = EAX / [disp]
  public final void emitDIV_Reg_Abs(byte dstReg, Offset disp) {
      int miStart = mi;
      if (VM.VerifyAssertions) VM._assert(dstReg == EAX);
      setMachineCodes(mi++, (byte) 0xF7);
      emitAbsRegOperands(disp, (byte) 0x6);
      if (lister != null) lister.RRA(miStart, "DIV", dstReg, disp);
  }

  // EAX:EDX = EAX u/ srcReg
  public final void emitIDIV_Reg_Reg(byte dstReg, byte srcReg) {
      int miStart = mi;
      if (VM.VerifyAssertions) VM._assert(dstReg == EAX);
      setMachineCodes(mi++, (byte) 0xF7);
      emitRegRegOperands(srcReg, (byte) 0x7);
      if (lister != null) lister.RR(miStart, "IDIV", dstReg, srcReg);
  }

  // EAX:EDX = EAX u/ [srcReg + disp]
  public final void emitIDIV_Reg_RegDisp(byte dstReg, byte srcReg, Offset disp) {
      int miStart = mi;
      if (VM.VerifyAssertions) VM._assert(dstReg == EAX);
      setMachineCodes(mi++, (byte) 0xF7);
      emitRegDispRegOperands(srcReg, disp, (byte) 0x7);
      if (lister != null) lister.RRD(miStart, "IDIV", dstReg, srcReg, disp);
  }

  // EAX:EDX = EAX u/ [srcReg]
  public final void emitIDIV_Reg_RegInd(byte dstReg, byte srcReg) {
      int miStart = mi;
      if (VM.VerifyAssertions) VM._assert(dstReg == EAX);
      setMachineCodes(mi++, (byte) 0xF7);
      emitRegIndirectRegOperands(srcReg, (byte) 0x7);
      if (lister != null) lister.RRN(miStart, "IDIV", dstReg, srcReg);
  }

  // EAX:EDX = EAX u/ [baseReg + idxRef<<scale + disp]
  public final void emitIDIV_Reg_RegIdx(byte dstReg, byte baseReg, byte idxReg, short scale, Offset disp) {
      int miStart = mi;
      if (VM.VerifyAssertions) VM._assert(dstReg == EAX);
      setMachineCodes(mi++, (byte) 0xF7);
      emitSIBRegOperands(baseReg, idxReg, scale, disp, (byte) 0x7);
      if (lister != null) lister.RRXD(miStart, "IDIV", dstReg, baseReg, idxReg, scale, disp);
  }

  // EAX:EDX = EAX u/ [idxRef<<scale + disp]
  public final void emitIDIV_Reg_RegOff(byte dstReg, byte idxReg, short scale, Offset disp) {
      int miStart = mi;
      if (VM.VerifyAssertions) VM._assert(dstReg == EAX);
      setMachineCodes(mi++, (byte) 0xF7);
      emitRegOffRegOperands(idxReg, scale, disp, (byte) 0x7);
      if (lister != null) lister.RRFD(miStart, "IDIV", dstReg, idxReg, scale, disp);
  }

  // EAX:EDX = EAX u/ [disp]
  public final void emitIDIV_Reg_Abs(byte dstReg, Offset disp) {
      int miStart = mi;
      if (VM.VerifyAssertions) VM._assert(dstReg == EAX);
      setMachineCodes(mi++, (byte) 0xF7);
      emitAbsRegOperands(disp, (byte) 0x7);
      if (lister != null) lister.RRA(miStart, "IDIV", dstReg, disp);
  }

  // EAX:EDX = EAX * srcReg
  public final void emitMUL_Reg_Reg(byte dstReg, byte srcReg) {
      int miStart = mi;
      if (VM.VerifyAssertions) VM._assert(dstReg == EAX);
      setMachineCodes(mi++, (byte) 0xF7);
      emitRegRegOperands(srcReg, (byte) 0x4);
      if (lister != null) lister.RR(miStart, "MUL", dstReg, srcReg);
  }

  // EAX:EDX = EAX * [srcReg + disp]
  public final void emitMUL_Reg_RegDisp(byte dstReg, byte srcReg, Offset disp) {
      int miStart = mi;
      if (VM.VerifyAssertions) VM._assert(dstReg == EAX);
      setMachineCodes(mi++, (byte) 0xF7);
      emitRegDispRegOperands(srcReg, disp, (byte) 0x4);
      if (lister != null) lister.RRD(miStart, "MUL", dstReg, srcReg, disp);
  }

  // EAX:EDX = EAX * [srcReg]
  public final void emitMUL_Reg_RegInd(byte dstReg, byte srcReg) {
      int miStart = mi;
      if (VM.VerifyAssertions) VM._assert(dstReg == EAX);
      setMachineCodes(mi++, (byte) 0xF7);
      emitRegIndirectRegOperands(srcReg, (byte) 0x4);
      if (lister != null) lister.RRN(miStart, "MUL", dstReg, srcReg);
  }

  // EAX:EDX = EAX * [baseReg + idxRef<<scale + disp]
  public final void emitMUL_Reg_RegIdx(byte dstReg, byte baseReg, byte idxReg, short scale, Offset disp) {
      int miStart = mi;
      if (VM.VerifyAssertions) VM._assert(dstReg == EAX);
      setMachineCodes(mi++, (byte) 0xF7);
      emitSIBRegOperands(baseReg, idxReg, scale, disp, (byte) 0x4);
      if (lister != null) lister.RRXD(miStart, "MUL", dstReg, baseReg, idxReg, scale, disp);
  }

  // EAX:EDX = EAX * [idxRef<<scale + disp]
  public final void emitMUL_Reg_RegOff(byte dstReg, byte idxReg, short scale, Offset disp) {
      int miStart = mi;
      if (VM.VerifyAssertions) VM._assert(dstReg == EAX);
      setMachineCodes(mi++, (byte) 0xF7);
      emitRegOffRegOperands(idxReg, scale, disp, (byte) 0x4);
      if (lister != null) lister.RRFD(miStart, "MUL", dstReg, idxReg, scale, disp);
  }

  // EAX:EDX = EAX * [disp]
  public final void emitMUL_Reg_Abs(byte dstReg, Offset disp) {
      int miStart = mi;
      if (VM.VerifyAssertions) VM._assert(dstReg == EAX);
      setMachineCodes(mi++, (byte) 0xF7);
      emitAbsRegOperands(disp, (byte) 0x4);
      if (lister != null) lister.RRA(miStart, "MUL", dstReg, disp);
  }

  // EAX:EDX = EAX * srcReg
  public final void emitIMUL1_Reg_Reg(byte dstReg, byte srcReg) {
      int miStart = mi;
      if (VM.VerifyAssertions) VM._assert(dstReg == EAX);
      setMachineCodes(mi++, (byte) 0xF7);
      emitRegRegOperands(srcReg, (byte) 0x5);
      if (lister != null) lister.RR(miStart, "IMUL1", dstReg, srcReg);
  }

  // EAX:EDX = EAX * [srcReg + disp]
  public final void emitIMUL1_Reg_RegDisp(byte dstReg, byte srcReg, Offset disp) {
      int miStart = mi;
      if (VM.VerifyAssertions) VM._assert(dstReg == EAX);
      setMachineCodes(mi++, (byte) 0xF7);
      emitRegDispRegOperands(srcReg, disp, (byte) 0x5);
      if (lister != null) lister.RRD(miStart, "IMUL1", dstReg, srcReg, disp);
  }

  // EAX:EDX = EAX * [srcReg]
  public final void emitIMUL1_Reg_RegInd(byte dstReg, byte srcReg) {
      int miStart = mi;
      if (VM.VerifyAssertions) VM._assert(dstReg == EAX);
      setMachineCodes(mi++, (byte) 0xF7);
      emitRegIndirectRegOperands(srcReg, (byte) 0x5);
      if (lister != null) lister.RRN(miStart, "IMUL1", dstReg, srcReg);
  }

  // EAX:EDX = EAX * [baseReg + idxRef<<scale + disp]
  public final void emitIMUL1_Reg_RegIdx(byte dstReg, byte baseReg, byte idxReg, short scale, Offset disp) {
      int miStart = mi;
      if (VM.VerifyAssertions) VM._assert(dstReg == EAX);
      setMachineCodes(mi++, (byte) 0xF7);
      emitSIBRegOperands(baseReg, idxReg, scale, disp, (byte) 0x5);
      if (lister != null) lister.RRXD(miStart, "IMUL1", dstReg, baseReg, idxReg, scale, disp);
  }

  // EAX:EDX = EAX * [idxRef<<scale + disp]
  public final void emitIMUL1_Reg_RegOff(byte dstReg, byte idxReg, short scale, Offset disp) {
      int miStart = mi;
      if (VM.VerifyAssertions) VM._assert(dstReg == EAX);
      setMachineCodes(mi++, (byte) 0xF7);
      emitRegOffRegOperands(idxReg, scale, disp, (byte) 0x5);
      if (lister != null) lister.RRFD(miStart, "IMUL1", dstReg, idxReg, scale, disp);
  }

  // EAX:EDX = EAX * [disp]
  public final void emitIMUL1_Reg_Abs(byte dstReg, Offset disp) {
      int miStart = mi;
      if (VM.VerifyAssertions) VM._assert(dstReg == EAX);
      setMachineCodes(mi++, (byte) 0xF7);
      emitAbsRegOperands(disp, (byte) 0x5);
      if (lister != null) lister.RRA(miStart, "IMUL1", dstReg, disp);
  }

  // dstReg := (byte) srcReg (sign extended)
  public final void emitMOVSX_Reg_Reg_Byte(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xBE);
    emitRegRegOperands(srcReg, dstReg);
    if (lister != null) lister.RR(miStart, "MOVSX", dstReg, srcReg);
  }

  // dstReg := (byte) [srcReg + disp] (sign extended)
  public final void emitMOVSX_Reg_RegDisp_Byte(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xBE);
    emitRegDispRegOperands(srcReg, disp, dstReg);
    if (lister != null) lister.RRD(miStart, "MOVSX", dstReg, srcReg, disp);
  }

  // dstReg := (byte) [srcReg] (sign extended)
  public final void emitMOVSX_Reg_RegInd_Byte(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xBE);
    emitRegIndirectRegOperands(srcReg, dstReg);
    if (lister != null) lister.RRN(miStart, "MOVSX", dstReg, srcReg);
  }

  // dstReg := (byte) [srcIndex<<scale + disp] (sign extended)
  public final void emitMOVSX_Reg_RegOff_Byte(byte dstReg, byte srcIndex, short scale, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xBE);
    emitRegOffRegOperands(srcIndex, scale, disp, dstReg);
    if (lister != null) lister.RRFD(miStart, "MOVSX", dstReg, srcIndex, scale, disp);
  }

  // dstReg := (byte) [disp] (sign extended)
  public final void emitMOVSX_Reg_Abs_Byte(byte dstReg, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xBE);
    emitAbsRegOperands(disp, dstReg);
    if (lister != null) lister.RRA(miStart, "MOVSX", dstReg, disp);
  }

  // dstReg := (byte) [srcBase + srcIndex<<scale + disp] (sign extended)
  public final void emitMOVSX_Reg_RegIdx_Byte(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xBE);
    emitSIBRegOperands(srcBase, srcIndex, scale, disp, dstReg);
    if (lister != null) lister.RRXD(miStart, "MOVSX", dstReg, srcBase, srcIndex, scale, disp);
  }

  // dstReg := (word) srcReg (sign extended)
  public final void emitMOVSX_Reg_Reg_Word(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xBF);
    emitRegRegOperands(srcReg, dstReg);
    if (lister != null) lister.RR(miStart, "MOVSX", dstReg, srcReg);
  }

  // dstReg := (word) [srcReg + disp] (sign extended)
  public final void emitMOVSX_Reg_RegDisp_Word(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xBF);
    emitRegDispRegOperands(srcReg, disp, dstReg);
    if (lister != null) lister.RRD(miStart, "MOVSX", dstReg, srcReg, disp);
  }

  // dstReg := (word) [srcReg] (sign extended)
  public final void emitMOVSX_Reg_RegInd_Word(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xBF);
    emitRegIndirectRegOperands(srcReg, dstReg);
    if (lister != null) lister.RRN(miStart, "MOVSX", dstReg, srcReg);
  }

  // dstReg := (word) [srcIndex<<scale + disp] (sign extended)
  public final void emitMOVSX_Reg_RegOff_Word(byte dstReg, byte srcIndex, short scale, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xBF);
    emitRegOffRegOperands(srcIndex, scale, disp, dstReg);
    if (lister != null) lister.RRFD(miStart, "MOVSX", dstReg, srcIndex, scale, disp);
  }

  // dstReg := (word) [disp] (sign extended)
  public final void emitMOVSX_Reg_Abs_Word(byte dstReg, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xBF);
    emitAbsRegOperands(disp, dstReg);
    if (lister != null) lister.RRA(miStart, "MOVSX", dstReg, disp);
  }

  // dstReg := (word) [srcBase + srcIndex<<scale + disp] (sign extended)
  public final void emitMOVSX_Reg_RegIdx_Word(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xBF);
    emitSIBRegOperands(srcBase, srcIndex, scale, disp, dstReg);
    if (lister != null) lister.RRXD(miStart, "MOVSX", dstReg, srcBase, srcIndex, scale, disp);
    }

  // dstReg := (byte) srcReg (zero extended)
  public final void emitMOVZX_Reg_Reg_Byte(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xB6);
    emitRegRegOperands(srcReg, dstReg);
    if (lister != null) lister.RR(miStart, "MOVZX", dstReg, srcReg);
  }

  // dstReg := (byte) [srcReg + disp] (zero extended)
  public final void emitMOVZX_Reg_RegDisp_Byte(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xB6);
    emitRegDispRegOperands(srcReg, disp, dstReg);
    if (lister != null) lister.RRD(miStart, "MOVZX", dstReg, srcReg, disp);
  }

  // dstReg := (byte) [srcReg] (zero extended)
  public final void emitMOVZX_Reg_RegInd_Byte(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xB6);
    emitRegIndirectRegOperands(srcReg, dstReg);
    if (lister != null) lister.RRN(miStart, "MOVZX", dstReg, srcReg);
  }

  // dstReg := (byte) [srcIndex<<scale + disp] (zero extended)
  public final void emitMOVZX_Reg_RegOff_Byte(byte dstReg, byte srcIndex, short scale, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xB6);
    emitRegOffRegOperands(srcIndex, scale, disp, dstReg);
    if (lister != null) lister.RRFD(miStart, "MOVZX", dstReg, srcIndex, scale, disp);
  }

  // dstReg := (byte) [disp] (zero extended)
  public final void emitMOVZX_Reg_Abs_Byte(byte dstReg, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xB6);
    emitAbsRegOperands(disp, dstReg);
    if (lister != null) lister.RRA(miStart, "MOVZX", dstReg, disp);
  }

  // dstReg := (byte) [srcBase + srcIndex<<scale + disp] (zero extended)
  public final void emitMOVZX_Reg_RegIdx_Byte(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xB6);
    emitSIBRegOperands(srcBase, srcIndex, scale, disp, dstReg);
    if (lister != null) lister.RRXD(miStart, "MOVZX", dstReg, srcBase, srcIndex, scale, disp);
  }

  // dstReg := (word) srcReg (zero extended)
  public final void emitMOVZX_Reg_Reg_Word(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xB7);
    emitRegRegOperands(srcReg, dstReg);
    if (lister != null) lister.RR(miStart, "MOVZX", dstReg, srcReg);
  }

  // dstReg := (word) [srcReg + disp] (zero extended)
  public final void emitMOVZX_Reg_RegDisp_Word(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xB7);
    emitRegDispRegOperands(srcReg, disp, dstReg);
    if (lister != null) lister.RRD(miStart, "MOVZX", dstReg, srcReg, disp);
  }

  // dstReg := (word) [srcReg] (zero extended)
  public final void emitMOVZX_Reg_RegInd_Word(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xB7);
    emitRegIndirectRegOperands(srcReg, dstReg);
    if (lister != null) lister.RRN(miStart, "MOVZX", dstReg, srcReg);
  }

  // dstReg := (word) [srcIndex<<scale + disp] (zero extended)
  public final void emitMOVZX_Reg_RegOff_Word(byte dstReg, byte srcIndex, short scale, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xB7);
    emitRegOffRegOperands(srcIndex, scale, disp, dstReg);
    if (lister != null) lister.RRFD(miStart, "MOVZX", dstReg, srcIndex, scale, disp);
  }

  // dstReg := (word) [disp] (zero extended)
  public final void emitMOVZX_Reg_Abs_Word(byte dstReg, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xB7);
    emitAbsRegOperands(disp, dstReg);
    if (lister != null) lister.RRA(miStart, "MOVZX", dstReg, disp);
  }

  // dstReg := (word) [srcBase + srcIndex<<scale + disp] (zero extended)
  public final void emitMOVZX_Reg_RegIdx_Word(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xB7);
    emitSIBRegOperands(srcBase, srcIndex, scale, disp, dstReg);
    if (lister != null) lister.RRXD(miStart, "MOVZX", dstReg, srcBase, srcIndex, scale, disp);
    }

  // arithemetic shift left of reg by imm
  public final void emitSAL_Reg_Imm_Byte(byte reg, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD0);
        emitRegRegOperands(reg, (byte) 0x4);
    } else {
        setMachineCodes(mi++, (byte) 0xC0);
        emitRegRegOperands(reg, (byte) 0x4);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RI(miStart, "SAL", reg, imm);
    }

  // arithemetic shift left of [reg] by imm
  public final void emitSAL_RegInd_Imm_Byte(byte reg, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD0);
        emitRegIndirectRegOperands(reg, (byte) 0x4);
    } else {
        setMachineCodes(mi++, (byte) 0xC0);
        emitRegIndirectRegOperands(reg, (byte) 0x4);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RNI(miStart, "SAL", reg, imm);
    }

  // arithemetic shift left of [reg + disp] by imm
  public final void emitSAL_RegDisp_Imm_Byte(byte reg, Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD0);
        emitRegDispRegOperands(reg, disp, (byte) 0x4);
    } else {
        setMachineCodes(mi++, (byte) 0xC0);
        emitRegDispRegOperands(reg, disp, (byte) 0x4);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RDI(miStart, "SAL", reg, disp, imm);
    }

  // arithemetic shift left of [index<<scale + disp] by imm
  public final void emitSAL_RegOff_Imm_Byte(byte index, short scale, Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD0);
        emitRegOffRegOperands(index, scale, disp, (byte) 0x4);
    } else {
        setMachineCodes(mi++, (byte) 0xC0);
        emitRegOffRegOperands(index, scale, disp, (byte) 0x4);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RFDI(miStart, "SAL", index, scale, disp, imm);
    }

  // arithemetic shift left of [disp] by imm
  public final void emitSAL_Abs_Imm_Byte(Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD0);
        emitAbsRegOperands(disp, (byte) 0x4);
    } else {
        setMachineCodes(mi++, (byte) 0xC0);
        emitAbsRegOperands(disp, (byte) 0x4);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RAI(miStart, "SAL", disp, imm);
  }

  // arithemetic shift left of [base + index<<scale + disp] by imm
  public final void emitSAL_RegIdx_Imm_Byte(byte base, byte index, short scale, Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD0);
        emitSIBRegOperands(base, index, scale, disp, (byte) 0x4);
    } else {
        setMachineCodes(mi++, (byte) 0xC0);
        emitSIBRegOperands(base, index, scale, disp, (byte) 0x4);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RXDI(miStart, "SAL", base, index, scale, disp, imm);
  }

  // arithemetic shift left of dataReg by shiftBy
  public final void emitSAL_Reg_Reg_Byte(byte dataReg, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD2);
    emitRegRegOperands(dataReg, (byte) 0x4);
    if (lister != null) lister.RR(miStart, "SAL", dataReg, shiftBy);
  }

  // arithemetic shift left of [dataReg] by shiftBy
  public final void emitSAL_RegInd_Reg_Byte(byte dataReg, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD2);
    emitRegIndirectRegOperands(dataReg, (byte) 0x4);
    if (lister != null) lister.RNR(miStart, "SAL", dataReg, shiftBy);
  }

  // arithemetic shift left of [dataReg + disp] by shiftBy
  public final void emitSAL_RegDisp_Reg_Byte(byte dataReg, Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD2);
    emitRegDispRegOperands(dataReg, disp, (byte) 0x4);
    if (lister != null) lister.RDR(miStart, "SAL", dataReg, disp, shiftBy);
  }

  // arithemetic shift left of [indexReg<<scale + disp] by shiftBy
  public final void emitSAL_RegOff_Reg_Byte(byte indexReg, short scale, Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD2);
    emitRegOffRegOperands(indexReg, scale, disp, (byte) 0x4);
    if (lister != null) lister.RFDR(miStart, "SAL", indexReg, scale, disp, shiftBy);
  }

  // arithemetic shift left of [disp] by shiftBy
  public final void emitSAL_Abs_Reg_Byte(Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD2);
    emitAbsRegOperands(disp, (byte) 0x4);
    if (lister != null) lister.RAR(miStart, "SAL", disp, shiftBy);
  }

  // arithemetic shift left of [baseReg + indexReg<<scale + disp] by shiftBy
  public final void emitSAL_RegIdx_Reg_Byte(byte baseReg, byte indexReg, short scale, Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD2);
    emitSIBRegOperands(baseReg, indexReg, scale, disp, (byte) 0x4);
    if (lister != null) lister.RXDR(miStart, "SAL", baseReg, indexReg, scale, disp, shiftBy);
  }

  // arithemetic shift left of reg by imm
  public final void emitSAL_Reg_Imm_Word(byte reg, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
        setMachineCodes(mi++, (byte) 0x66);
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitRegRegOperands(reg, (byte) 0x4);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitRegRegOperands(reg, (byte) 0x4);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RI(miStart, "SAL", reg, imm);
    }

  // arithemetic shift left of [reg] by imm
  public final void emitSAL_RegInd_Imm_Word(byte reg, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
        setMachineCodes(mi++, (byte) 0x66);
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitRegIndirectRegOperands(reg, (byte) 0x4);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitRegIndirectRegOperands(reg, (byte) 0x4);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RNI(miStart, "SAL", reg, imm);
    }

  // arithemetic shift left of [reg + disp] by imm
  public final void emitSAL_RegDisp_Imm_Word(byte reg, Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
        setMachineCodes(mi++, (byte) 0x66);
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitRegDispRegOperands(reg, disp, (byte) 0x4);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitRegDispRegOperands(reg, disp, (byte) 0x4);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RDI(miStart, "SAL", reg, disp, imm);
    }

  // arithemetic shift left of [index<<scale + disp] by imm
  public final void emitSAL_RegOff_Imm_Word(byte index, short scale, Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
        setMachineCodes(mi++, (byte) 0x66);
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitRegOffRegOperands(index, scale, disp, (byte) 0x4);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitRegOffRegOperands(index, scale, disp, (byte) 0x4);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RFDI(miStart, "SAL", index, scale, disp, imm);
    }

  // arithemetic shift left of [disp] by imm
  public final void emitSAL_Abs_Imm_Word(Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
        setMachineCodes(mi++, (byte) 0x66);
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitAbsRegOperands(disp, (byte) 0x4);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitAbsRegOperands(disp, (byte) 0x4);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RAI(miStart, "SAL", disp, imm);
  }

  // arithemetic shift left of [base + index<<scale + disp] by imm
  public final void emitSAL_RegIdx_Imm_Word(byte base, byte index, short scale, Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
        setMachineCodes(mi++, (byte) 0x66);
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitSIBRegOperands(base, index, scale, disp, (byte) 0x4);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitSIBRegOperands(base, index, scale, disp, (byte) 0x4);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RXDI(miStart, "SAL", base, index, scale, disp, imm);
  }

  // arithemetic shift left of dataReg by shiftBy
  public final void emitSAL_Reg_Reg_Word(byte dataReg, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0xD3);
    emitRegRegOperands(dataReg, (byte) 0x4);
    if (lister != null) lister.RR(miStart, "SAL", dataReg, shiftBy);
  }

  // arithemetic shift left of [dataReg] by shiftBy
  public final void emitSAL_RegInd_Reg_Word(byte dataReg, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0xD3);
    emitRegIndirectRegOperands(dataReg, (byte) 0x4);
    if (lister != null) lister.RNR(miStart, "SAL", dataReg, shiftBy);
  }

  // arithemetic shift left of [dataReg + disp] by shiftBy
  public final void emitSAL_RegDisp_Reg_Word(byte dataReg, Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0xD3);
    emitRegDispRegOperands(dataReg, disp, (byte) 0x4);
    if (lister != null) lister.RDR(miStart, "SAL", dataReg, disp, shiftBy);
  }

  // arithemetic shift left of [indexReg<<scale + disp] by shiftBy
  public final void emitSAL_RegOff_Reg_Word(byte indexReg, short scale, Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0xD3);
    emitRegOffRegOperands(indexReg, scale, disp, (byte) 0x4);
    if (lister != null) lister.RFDR(miStart, "SAL", indexReg, scale, disp, shiftBy);
  }

  // arithemetic shift left of [disp] by shiftBy
  public final void emitSAL_Abs_Reg_Word(Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0xD3);
    emitAbsRegOperands(disp, (byte) 0x4);
    if (lister != null) lister.RAR(miStart, "SAL", disp, shiftBy);
  }

  // arithemetic shift left of [baseReg + indexReg<<scale + disp] by shiftBy
  public final void emitSAL_RegIdx_Reg_Word(byte baseReg, byte indexReg, short scale, Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0xD3);
    emitSIBRegOperands(baseReg, indexReg, scale, disp, (byte) 0x4);
    if (lister != null) lister.RXDR(miStart, "SAL", baseReg, indexReg, scale, disp, shiftBy);
  }

  // arithemetic shift left of reg by imm
  public final void emitSAL_Reg_Imm(byte reg, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitRegRegOperands(reg, (byte) 0x4);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitRegRegOperands(reg, (byte) 0x4);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RI(miStart, "SAL", reg, imm);
    }

  // arithemetic shift left of [reg] by imm
  public final void emitSAL_RegInd_Imm(byte reg, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitRegIndirectRegOperands(reg, (byte) 0x4);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitRegIndirectRegOperands(reg, (byte) 0x4);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RNI(miStart, "SAL", reg, imm);
    }

  // arithemetic shift left of [reg + disp] by imm
  public final void emitSAL_RegDisp_Imm(byte reg, Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitRegDispRegOperands(reg, disp, (byte) 0x4);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitRegDispRegOperands(reg, disp, (byte) 0x4);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RDI(miStart, "SAL", reg, disp, imm);
    }

  // arithemetic shift left of [index<<scale + disp] by imm
  public final void emitSAL_RegOff_Imm(byte index, short scale, Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitRegOffRegOperands(index, scale, disp, (byte) 0x4);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitRegOffRegOperands(index, scale, disp, (byte) 0x4);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RFDI(miStart, "SAL", index, scale, disp, imm);
    }

  // arithemetic shift left of [disp] by imm
  public final void emitSAL_Abs_Imm(Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitAbsRegOperands(disp, (byte) 0x4);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitAbsRegOperands(disp, (byte) 0x4);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RAI(miStart, "SAL", disp, imm);
  }

  // arithemetic shift left of [base + index<<scale + disp] by imm
  public final void emitSAL_RegIdx_Imm(byte base, byte index, short scale, Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitSIBRegOperands(base, index, scale, disp, (byte) 0x4);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitSIBRegOperands(base, index, scale, disp, (byte) 0x4);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RXDI(miStart, "SAL", base, index, scale, disp, imm);
  }

  // arithemetic shift left of dataReg by shiftBy
  public final void emitSAL_Reg_Reg(byte dataReg, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD3);
    emitRegRegOperands(dataReg, (byte) 0x4);
    if (lister != null) lister.RR(miStart, "SAL", dataReg, shiftBy);
  }

  // arithemetic shift left of [dataReg] by shiftBy
  public final void emitSAL_RegInd_Reg(byte dataReg, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD3);
    emitRegIndirectRegOperands(dataReg, (byte) 0x4);
    if (lister != null) lister.RNR(miStart, "SAL", dataReg, shiftBy);
  }

  // arithemetic shift left of [dataReg + disp] by shiftBy
  public final void emitSAL_RegDisp_Reg(byte dataReg, Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD3);
    emitRegDispRegOperands(dataReg, disp, (byte) 0x4);
    if (lister != null) lister.RDR(miStart, "SAL", dataReg, disp, shiftBy);
  }

  // arithemetic shift left of [indexReg<<scale + disp] by shiftBy
  public final void emitSAL_RegOff_Reg(byte indexReg, short scale, Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD3);
    emitRegOffRegOperands(indexReg, scale, disp, (byte) 0x4);
    if (lister != null) lister.RFDR(miStart, "SAL", indexReg, scale, disp, shiftBy);
  }

  // arithemetic shift left of [disp] by shiftBy
  public final void emitSAL_Abs_Reg(Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD3);
    emitAbsRegOperands(disp, (byte) 0x4);
    if (lister != null) lister.RAR(miStart, "SAL", disp, shiftBy);
  }

  // arithemetic shift left of [baseReg + indexReg<<scale + disp] by shiftBy
  public final void emitSAL_RegIdx_Reg(byte baseReg, byte indexReg, short scale, Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD3);
    emitSIBRegOperands(baseReg, indexReg, scale, disp, (byte) 0x4);
    if (lister != null) lister.RXDR(miStart, "SAL", baseReg, indexReg, scale, disp, shiftBy);
  }

  // arithemetic shift right of reg by imm
  public final void emitSAR_Reg_Imm_Byte(byte reg, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD0);
        emitRegRegOperands(reg, (byte) 0x7);
    } else {
        setMachineCodes(mi++, (byte) 0xC0);
        emitRegRegOperands(reg, (byte) 0x7);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RI(miStart, "SAR", reg, imm);
    }

  // arithemetic shift right of [reg] by imm
  public final void emitSAR_RegInd_Imm_Byte(byte reg, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD0);
        emitRegIndirectRegOperands(reg, (byte) 0x7);
    } else {
        setMachineCodes(mi++, (byte) 0xC0);
        emitRegIndirectRegOperands(reg, (byte) 0x7);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RNI(miStart, "SAR", reg, imm);
    }

  // arithemetic shift right of [reg + disp] by imm
  public final void emitSAR_RegDisp_Imm_Byte(byte reg, Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD0);
        emitRegDispRegOperands(reg, disp, (byte) 0x7);
    } else {
        setMachineCodes(mi++, (byte) 0xC0);
        emitRegDispRegOperands(reg, disp, (byte) 0x7);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RDI(miStart, "SAR", reg, disp, imm);
    }

  // arithemetic shift right of [index<<scale + disp] by imm
  public final void emitSAR_RegOff_Imm_Byte(byte index, short scale, Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD0);
        emitRegOffRegOperands(index, scale, disp, (byte) 0x7);
    } else {
        setMachineCodes(mi++, (byte) 0xC0);
        emitRegOffRegOperands(index, scale, disp, (byte) 0x7);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RFDI(miStart, "SAR", index, scale, disp, imm);
    }

  // arithemetic shift right of [disp] by imm
  public final void emitSAR_Abs_Imm_Byte(Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD0);
        emitAbsRegOperands(disp, (byte) 0x7);
    } else {
        setMachineCodes(mi++, (byte) 0xC0);
        emitAbsRegOperands(disp, (byte) 0x7);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RAI(miStart, "SAR", disp, imm);
  }

  // arithemetic shift right of [base + index<<scale + disp] by imm
  public final void emitSAR_RegIdx_Imm_Byte(byte base, byte index, short scale, Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD0);
        emitSIBRegOperands(base, index, scale, disp, (byte) 0x7);
    } else {
        setMachineCodes(mi++, (byte) 0xC0);
        emitSIBRegOperands(base, index, scale, disp, (byte) 0x7);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RXDI(miStart, "SAR", base, index, scale, disp, imm);
  }

  // arithemetic shift right of dataReg by shiftBy
  public final void emitSAR_Reg_Reg_Byte(byte dataReg, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD2);
    emitRegRegOperands(dataReg, (byte) 0x7);
    if (lister != null) lister.RR(miStart, "SAR", dataReg, shiftBy);
  }

  // arithemetic shift right of [dataReg] by shiftBy
  public final void emitSAR_RegInd_Reg_Byte(byte dataReg, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD2);
    emitRegIndirectRegOperands(dataReg, (byte) 0x7);
    if (lister != null) lister.RNR(miStart, "SAR", dataReg, shiftBy);
  }

  // arithemetic shift right of [dataReg + disp] by shiftBy
  public final void emitSAR_RegDisp_Reg_Byte(byte dataReg, Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD2);
    emitRegDispRegOperands(dataReg, disp, (byte) 0x7);
    if (lister != null) lister.RDR(miStart, "SAR", dataReg, disp, shiftBy);
  }

  // arithemetic shift right of [indexReg<<scale + disp] by shiftBy
  public final void emitSAR_RegOff_Reg_Byte(byte indexReg, short scale, Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD2);
    emitRegOffRegOperands(indexReg, scale, disp, (byte) 0x7);
    if (lister != null) lister.RFDR(miStart, "SAR", indexReg, scale, disp, shiftBy);
  }

  // arithemetic shift right of [disp] by shiftBy
  public final void emitSAR_Abs_Reg_Byte(Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD2);
    emitAbsRegOperands(disp, (byte) 0x7);
    if (lister != null) lister.RAR(miStart, "SAR", disp, shiftBy);
  }

  // arithemetic shift right of [baseReg + indexReg<<scale + disp] by shiftBy
  public final void emitSAR_RegIdx_Reg_Byte(byte baseReg, byte indexReg, short scale, Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD2);
    emitSIBRegOperands(baseReg, indexReg, scale, disp, (byte) 0x7);
    if (lister != null) lister.RXDR(miStart, "SAR", baseReg, indexReg, scale, disp, shiftBy);
  }

  // arithemetic shift right of reg by imm
  public final void emitSAR_Reg_Imm_Word(byte reg, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
        setMachineCodes(mi++, (byte) 0x66);
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitRegRegOperands(reg, (byte) 0x7);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitRegRegOperands(reg, (byte) 0x7);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RI(miStart, "SAR", reg, imm);
    }

  // arithemetic shift right of [reg] by imm
  public final void emitSAR_RegInd_Imm_Word(byte reg, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
        setMachineCodes(mi++, (byte) 0x66);
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitRegIndirectRegOperands(reg, (byte) 0x7);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitRegIndirectRegOperands(reg, (byte) 0x7);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RNI(miStart, "SAR", reg, imm);
    }

  // arithemetic shift right of [reg + disp] by imm
  public final void emitSAR_RegDisp_Imm_Word(byte reg, Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
        setMachineCodes(mi++, (byte) 0x66);
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitRegDispRegOperands(reg, disp, (byte) 0x7);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitRegDispRegOperands(reg, disp, (byte) 0x7);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RDI(miStart, "SAR", reg, disp, imm);
    }

  // arithemetic shift right of [index<<scale + disp] by imm
  public final void emitSAR_RegOff_Imm_Word(byte index, short scale, Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
        setMachineCodes(mi++, (byte) 0x66);
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitRegOffRegOperands(index, scale, disp, (byte) 0x7);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitRegOffRegOperands(index, scale, disp, (byte) 0x7);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RFDI(miStart, "SAR", index, scale, disp, imm);
    }

  // arithemetic shift right of [disp] by imm
  public final void emitSAR_Abs_Imm_Word(Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
        setMachineCodes(mi++, (byte) 0x66);
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitAbsRegOperands(disp, (byte) 0x7);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitAbsRegOperands(disp, (byte) 0x7);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RAI(miStart, "SAR", disp, imm);
  }

  // arithemetic shift right of [base + index<<scale + disp] by imm
  public final void emitSAR_RegIdx_Imm_Word(byte base, byte index, short scale, Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
        setMachineCodes(mi++, (byte) 0x66);
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitSIBRegOperands(base, index, scale, disp, (byte) 0x7);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitSIBRegOperands(base, index, scale, disp, (byte) 0x7);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RXDI(miStart, "SAR", base, index, scale, disp, imm);
  }

  // arithemetic shift right of dataReg by shiftBy
  public final void emitSAR_Reg_Reg_Word(byte dataReg, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0xD3);
    emitRegRegOperands(dataReg, (byte) 0x7);
    if (lister != null) lister.RR(miStart, "SAR", dataReg, shiftBy);
  }

  // arithemetic shift right of [dataReg] by shiftBy
  public final void emitSAR_RegInd_Reg_Word(byte dataReg, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0xD3);
    emitRegIndirectRegOperands(dataReg, (byte) 0x7);
    if (lister != null) lister.RNR(miStart, "SAR", dataReg, shiftBy);
  }

  // arithemetic shift right of [dataReg + disp] by shiftBy
  public final void emitSAR_RegDisp_Reg_Word(byte dataReg, Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0xD3);
    emitRegDispRegOperands(dataReg, disp, (byte) 0x7);
    if (lister != null) lister.RDR(miStart, "SAR", dataReg, disp, shiftBy);
  }

  // arithemetic shift right of [indexReg<<scale + disp] by shiftBy
  public final void emitSAR_RegOff_Reg_Word(byte indexReg, short scale, Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0xD3);
    emitRegOffRegOperands(indexReg, scale, disp, (byte) 0x7);
    if (lister != null) lister.RFDR(miStart, "SAR", indexReg, scale, disp, shiftBy);
  }

  // arithemetic shift right of [disp] by shiftBy
  public final void emitSAR_Abs_Reg_Word(Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0xD3);
    emitAbsRegOperands(disp, (byte) 0x7);
    if (lister != null) lister.RAR(miStart, "SAR", disp, shiftBy);
  }

  // arithemetic shift right of [baseReg + indexReg<<scale + disp] by shiftBy
  public final void emitSAR_RegIdx_Reg_Word(byte baseReg, byte indexReg, short scale, Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0xD3);
    emitSIBRegOperands(baseReg, indexReg, scale, disp, (byte) 0x7);
    if (lister != null) lister.RXDR(miStart, "SAR", baseReg, indexReg, scale, disp, shiftBy);
  }

  // arithemetic shift right of reg by imm
  public final void emitSAR_Reg_Imm(byte reg, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitRegRegOperands(reg, (byte) 0x7);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitRegRegOperands(reg, (byte) 0x7);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RI(miStart, "SAR", reg, imm);
    }

  // arithemetic shift right of [reg] by imm
  public final void emitSAR_RegInd_Imm(byte reg, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitRegIndirectRegOperands(reg, (byte) 0x7);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitRegIndirectRegOperands(reg, (byte) 0x7);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RNI(miStart, "SAR", reg, imm);
    }

  // arithemetic shift right of [reg + disp] by imm
  public final void emitSAR_RegDisp_Imm(byte reg, Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitRegDispRegOperands(reg, disp, (byte) 0x7);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitRegDispRegOperands(reg, disp, (byte) 0x7);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RDI(miStart, "SAR", reg, disp, imm);
    }

  // arithemetic shift right of [index<<scale + disp] by imm
  public final void emitSAR_RegOff_Imm(byte index, short scale, Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitRegOffRegOperands(index, scale, disp, (byte) 0x7);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitRegOffRegOperands(index, scale, disp, (byte) 0x7);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RFDI(miStart, "SAR", index, scale, disp, imm);
    }

  // arithemetic shift right of [disp] by imm
  public final void emitSAR_Abs_Imm(Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitAbsRegOperands(disp, (byte) 0x7);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitAbsRegOperands(disp, (byte) 0x7);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RAI(miStart, "SAR", disp, imm);
  }

  // arithemetic shift right of [base + index<<scale + disp] by imm
  public final void emitSAR_RegIdx_Imm(byte base, byte index, short scale, Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitSIBRegOperands(base, index, scale, disp, (byte) 0x7);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitSIBRegOperands(base, index, scale, disp, (byte) 0x7);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RXDI(miStart, "SAR", base, index, scale, disp, imm);
  }

  // arithemetic shift right of dataReg by shiftBy
  public final void emitSAR_Reg_Reg(byte dataReg, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD3);
    emitRegRegOperands(dataReg, (byte) 0x7);
    if (lister != null) lister.RR(miStart, "SAR", dataReg, shiftBy);
  }

  // arithemetic shift right of [dataReg] by shiftBy
  public final void emitSAR_RegInd_Reg(byte dataReg, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD3);
    emitRegIndirectRegOperands(dataReg, (byte) 0x7);
    if (lister != null) lister.RNR(miStart, "SAR", dataReg, shiftBy);
  }

  // arithemetic shift right of [dataReg + disp] by shiftBy
  public final void emitSAR_RegDisp_Reg(byte dataReg, Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD3);
    emitRegDispRegOperands(dataReg, disp, (byte) 0x7);
    if (lister != null) lister.RDR(miStart, "SAR", dataReg, disp, shiftBy);
  }

  // arithemetic shift right of [indexReg<<scale + disp] by shiftBy
  public final void emitSAR_RegOff_Reg(byte indexReg, short scale, Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD3);
    emitRegOffRegOperands(indexReg, scale, disp, (byte) 0x7);
    if (lister != null) lister.RFDR(miStart, "SAR", indexReg, scale, disp, shiftBy);
  }

  // arithemetic shift right of [disp] by shiftBy
  public final void emitSAR_Abs_Reg(Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD3);
    emitAbsRegOperands(disp, (byte) 0x7);
    if (lister != null) lister.RAR(miStart, "SAR", disp, shiftBy);
  }

  // arithemetic shift right of [baseReg + indexReg<<scale + disp] by shiftBy
  public final void emitSAR_RegIdx_Reg(byte baseReg, byte indexReg, short scale, Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD3);
    emitSIBRegOperands(baseReg, indexReg, scale, disp, (byte) 0x7);
    if (lister != null) lister.RXDR(miStart, "SAR", baseReg, indexReg, scale, disp, shiftBy);
  }

  // logical shift left of reg by imm
  public final void emitSHL_Reg_Imm_Byte(byte reg, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD0);
        emitRegRegOperands(reg, (byte) 0x4);
    } else {
        setMachineCodes(mi++, (byte) 0xC0);
        emitRegRegOperands(reg, (byte) 0x4);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RI(miStart, "SHL", reg, imm);
    }

  // logical shift left of [reg] by imm
  public final void emitSHL_RegInd_Imm_Byte(byte reg, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD0);
        emitRegIndirectRegOperands(reg, (byte) 0x4);
    } else {
        setMachineCodes(mi++, (byte) 0xC0);
        emitRegIndirectRegOperands(reg, (byte) 0x4);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RNI(miStart, "SHL", reg, imm);
    }

  // logical shift left of [reg + disp] by imm
  public final void emitSHL_RegDisp_Imm_Byte(byte reg, Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD0);
        emitRegDispRegOperands(reg, disp, (byte) 0x4);
    } else {
        setMachineCodes(mi++, (byte) 0xC0);
        emitRegDispRegOperands(reg, disp, (byte) 0x4);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RDI(miStart, "SHL", reg, disp, imm);
    }

  // logical shift left of [index<<scale + disp] by imm
  public final void emitSHL_RegOff_Imm_Byte(byte index, short scale, Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD0);
        emitRegOffRegOperands(index, scale, disp, (byte) 0x4);
    } else {
        setMachineCodes(mi++, (byte) 0xC0);
        emitRegOffRegOperands(index, scale, disp, (byte) 0x4);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RFDI(miStart, "SHL", index, scale, disp, imm);
    }

  // logical shift left of [disp] by imm
  public final void emitSHL_Abs_Imm_Byte(Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD0);
        emitAbsRegOperands(disp, (byte) 0x4);
    } else {
        setMachineCodes(mi++, (byte) 0xC0);
        emitAbsRegOperands(disp, (byte) 0x4);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RAI(miStart, "SHL", disp, imm);
  }

  // logical shift left of [base + index<<scale + disp] by imm
  public final void emitSHL_RegIdx_Imm_Byte(byte base, byte index, short scale, Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD0);
        emitSIBRegOperands(base, index, scale, disp, (byte) 0x4);
    } else {
        setMachineCodes(mi++, (byte) 0xC0);
        emitSIBRegOperands(base, index, scale, disp, (byte) 0x4);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RXDI(miStart, "SHL", base, index, scale, disp, imm);
  }

  // logical shift left of dataReg by shiftBy
  public final void emitSHL_Reg_Reg_Byte(byte dataReg, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD2);
    emitRegRegOperands(dataReg, (byte) 0x4);
    if (lister != null) lister.RR(miStart, "SHL", dataReg, shiftBy);
  }

  // logical shift left of [dataReg] by shiftBy
  public final void emitSHL_RegInd_Reg_Byte(byte dataReg, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD2);
    emitRegIndirectRegOperands(dataReg, (byte) 0x4);
    if (lister != null) lister.RNR(miStart, "SHL", dataReg, shiftBy);
  }

  // logical shift left of [dataReg + disp] by shiftBy
  public final void emitSHL_RegDisp_Reg_Byte(byte dataReg, Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD2);
    emitRegDispRegOperands(dataReg, disp, (byte) 0x4);
    if (lister != null) lister.RDR(miStart, "SHL", dataReg, disp, shiftBy);
  }

  // logical shift left of [indexReg<<scale + disp] by shiftBy
  public final void emitSHL_RegOff_Reg_Byte(byte indexReg, short scale, Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD2);
    emitRegOffRegOperands(indexReg, scale, disp, (byte) 0x4);
    if (lister != null) lister.RFDR(miStart, "SHL", indexReg, scale, disp, shiftBy);
  }

  // logical shift left of [disp] by shiftBy
  public final void emitSHL_Abs_Reg_Byte(Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD2);
    emitAbsRegOperands(disp, (byte) 0x4);
    if (lister != null) lister.RAR(miStart, "SHL", disp, shiftBy);
  }

  // logical shift left of [baseReg + indexReg<<scale + disp] by shiftBy
  public final void emitSHL_RegIdx_Reg_Byte(byte baseReg, byte indexReg, short scale, Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD2);
    emitSIBRegOperands(baseReg, indexReg, scale, disp, (byte) 0x4);
    if (lister != null) lister.RXDR(miStart, "SHL", baseReg, indexReg, scale, disp, shiftBy);
  }

  // logical shift left of reg by imm
  public final void emitSHL_Reg_Imm_Word(byte reg, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
        setMachineCodes(mi++, (byte) 0x66);
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitRegRegOperands(reg, (byte) 0x4);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitRegRegOperands(reg, (byte) 0x4);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RI(miStart, "SHL", reg, imm);
    }

  // logical shift left of [reg] by imm
  public final void emitSHL_RegInd_Imm_Word(byte reg, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
        setMachineCodes(mi++, (byte) 0x66);
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitRegIndirectRegOperands(reg, (byte) 0x4);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitRegIndirectRegOperands(reg, (byte) 0x4);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RNI(miStart, "SHL", reg, imm);
    }

  // logical shift left of [reg + disp] by imm
  public final void emitSHL_RegDisp_Imm_Word(byte reg, Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
        setMachineCodes(mi++, (byte) 0x66);
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitRegDispRegOperands(reg, disp, (byte) 0x4);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitRegDispRegOperands(reg, disp, (byte) 0x4);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RDI(miStart, "SHL", reg, disp, imm);
    }

  // logical shift left of [index<<scale + disp] by imm
  public final void emitSHL_RegOff_Imm_Word(byte index, short scale, Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
        setMachineCodes(mi++, (byte) 0x66);
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitRegOffRegOperands(index, scale, disp, (byte) 0x4);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitRegOffRegOperands(index, scale, disp, (byte) 0x4);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RFDI(miStart, "SHL", index, scale, disp, imm);
    }

  // logical shift left of [disp] by imm
  public final void emitSHL_Abs_Imm_Word(Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
        setMachineCodes(mi++, (byte) 0x66);
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitAbsRegOperands(disp, (byte) 0x4);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitAbsRegOperands(disp, (byte) 0x4);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RAI(miStart, "SHL", disp, imm);
  }

  // logical shift left of [base + index<<scale + disp] by imm
  public final void emitSHL_RegIdx_Imm_Word(byte base, byte index, short scale, Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
        setMachineCodes(mi++, (byte) 0x66);
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitSIBRegOperands(base, index, scale, disp, (byte) 0x4);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitSIBRegOperands(base, index, scale, disp, (byte) 0x4);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RXDI(miStart, "SHL", base, index, scale, disp, imm);
  }

  // logical shift left of dataReg by shiftBy
  public final void emitSHL_Reg_Reg_Word(byte dataReg, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0xD3);
    emitRegRegOperands(dataReg, (byte) 0x4);
    if (lister != null) lister.RR(miStart, "SHL", dataReg, shiftBy);
  }

  // logical shift left of [dataReg] by shiftBy
  public final void emitSHL_RegInd_Reg_Word(byte dataReg, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0xD3);
    emitRegIndirectRegOperands(dataReg, (byte) 0x4);
    if (lister != null) lister.RNR(miStart, "SHL", dataReg, shiftBy);
  }

  // logical shift left of [dataReg + disp] by shiftBy
  public final void emitSHL_RegDisp_Reg_Word(byte dataReg, Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0xD3);
    emitRegDispRegOperands(dataReg, disp, (byte) 0x4);
    if (lister != null) lister.RDR(miStart, "SHL", dataReg, disp, shiftBy);
  }

  // logical shift left of [indexReg<<scale + disp] by shiftBy
  public final void emitSHL_RegOff_Reg_Word(byte indexReg, short scale, Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0xD3);
    emitRegOffRegOperands(indexReg, scale, disp, (byte) 0x4);
    if (lister != null) lister.RFDR(miStart, "SHL", indexReg, scale, disp, shiftBy);
  }

  // logical shift left of [disp] by shiftBy
  public final void emitSHL_Abs_Reg_Word(Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0xD3);
    emitAbsRegOperands(disp, (byte) 0x4);
    if (lister != null) lister.RAR(miStart, "SHL", disp, shiftBy);
  }

  // logical shift left of [baseReg + indexReg<<scale + disp] by shiftBy
  public final void emitSHL_RegIdx_Reg_Word(byte baseReg, byte indexReg, short scale, Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0xD3);
    emitSIBRegOperands(baseReg, indexReg, scale, disp, (byte) 0x4);
    if (lister != null) lister.RXDR(miStart, "SHL", baseReg, indexReg, scale, disp, shiftBy);
  }

  // logical shift left of reg by imm
  public final void emitSHL_Reg_Imm(byte reg, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitRegRegOperands(reg, (byte) 0x4);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitRegRegOperands(reg, (byte) 0x4);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RI(miStart, "SHL", reg, imm);
    }

  // logical shift left of [reg] by imm
  public final void emitSHL_RegInd_Imm(byte reg, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitRegIndirectRegOperands(reg, (byte) 0x4);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitRegIndirectRegOperands(reg, (byte) 0x4);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RNI(miStart, "SHL", reg, imm);
    }

  // logical shift left of [reg + disp] by imm
  public final void emitSHL_RegDisp_Imm(byte reg, Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitRegDispRegOperands(reg, disp, (byte) 0x4);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitRegDispRegOperands(reg, disp, (byte) 0x4);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RDI(miStart, "SHL", reg, disp, imm);
    }

  // logical shift left of [index<<scale + disp] by imm
  public final void emitSHL_RegOff_Imm(byte index, short scale, Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitRegOffRegOperands(index, scale, disp, (byte) 0x4);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitRegOffRegOperands(index, scale, disp, (byte) 0x4);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RFDI(miStart, "SHL", index, scale, disp, imm);
    }

  // logical shift left of [disp] by imm
  public final void emitSHL_Abs_Imm(Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitAbsRegOperands(disp, (byte) 0x4);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitAbsRegOperands(disp, (byte) 0x4);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RAI(miStart, "SHL", disp, imm);
  }

  // logical shift left of [base + index<<scale + disp] by imm
  public final void emitSHL_RegIdx_Imm(byte base, byte index, short scale, Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitSIBRegOperands(base, index, scale, disp, (byte) 0x4);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitSIBRegOperands(base, index, scale, disp, (byte) 0x4);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RXDI(miStart, "SHL", base, index, scale, disp, imm);
  }

  // logical shift left of dataReg by shiftBy
  public final void emitSHL_Reg_Reg(byte dataReg, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD3);
    emitRegRegOperands(dataReg, (byte) 0x4);
    if (lister != null) lister.RR(miStart, "SHL", dataReg, shiftBy);
  }

  // logical shift left of [dataReg] by shiftBy
  public final void emitSHL_RegInd_Reg(byte dataReg, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD3);
    emitRegIndirectRegOperands(dataReg, (byte) 0x4);
    if (lister != null) lister.RNR(miStart, "SHL", dataReg, shiftBy);
  }

  // logical shift left of [dataReg + disp] by shiftBy
  public final void emitSHL_RegDisp_Reg(byte dataReg, Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD3);
    emitRegDispRegOperands(dataReg, disp, (byte) 0x4);
    if (lister != null) lister.RDR(miStart, "SHL", dataReg, disp, shiftBy);
  }

  // logical shift left of [indexReg<<scale + disp] by shiftBy
  public final void emitSHL_RegOff_Reg(byte indexReg, short scale, Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD3);
    emitRegOffRegOperands(indexReg, scale, disp, (byte) 0x4);
    if (lister != null) lister.RFDR(miStart, "SHL", indexReg, scale, disp, shiftBy);
  }

  // logical shift left of [disp] by shiftBy
  public final void emitSHL_Abs_Reg(Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD3);
    emitAbsRegOperands(disp, (byte) 0x4);
    if (lister != null) lister.RAR(miStart, "SHL", disp, shiftBy);
  }

  // logical shift left of [baseReg + indexReg<<scale + disp] by shiftBy
  public final void emitSHL_RegIdx_Reg(byte baseReg, byte indexReg, short scale, Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD3);
    emitSIBRegOperands(baseReg, indexReg, scale, disp, (byte) 0x4);
    if (lister != null) lister.RXDR(miStart, "SHL", baseReg, indexReg, scale, disp, shiftBy);
  }

  // logical shift right of reg by imm
  public final void emitSHR_Reg_Imm_Byte(byte reg, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD0);
        emitRegRegOperands(reg, (byte) 0x5);
    } else {
        setMachineCodes(mi++, (byte) 0xC0);
        emitRegRegOperands(reg, (byte) 0x5);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RI(miStart, "SHR", reg, imm);
    }

  // logical shift right of [reg] by imm
  public final void emitSHR_RegInd_Imm_Byte(byte reg, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD0);
        emitRegIndirectRegOperands(reg, (byte) 0x5);
    } else {
        setMachineCodes(mi++, (byte) 0xC0);
        emitRegIndirectRegOperands(reg, (byte) 0x5);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RNI(miStart, "SHR", reg, imm);
    }

  // logical shift right of [reg + disp] by imm
  public final void emitSHR_RegDisp_Imm_Byte(byte reg, Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD0);
        emitRegDispRegOperands(reg, disp, (byte) 0x5);
    } else {
        setMachineCodes(mi++, (byte) 0xC0);
        emitRegDispRegOperands(reg, disp, (byte) 0x5);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RDI(miStart, "SHR", reg, disp, imm);
    }

  // logical shift right of [index<<scale + disp] by imm
  public final void emitSHR_RegOff_Imm_Byte(byte index, short scale, Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD0);
        emitRegOffRegOperands(index, scale, disp, (byte) 0x5);
    } else {
        setMachineCodes(mi++, (byte) 0xC0);
        emitRegOffRegOperands(index, scale, disp, (byte) 0x5);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RFDI(miStart, "SHR", index, scale, disp, imm);
    }

  // logical shift right of [disp] by imm
  public final void emitSHR_Abs_Imm_Byte(Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD0);
        emitAbsRegOperands(disp, (byte) 0x5);
    } else {
        setMachineCodes(mi++, (byte) 0xC0);
        emitAbsRegOperands(disp, (byte) 0x5);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RAI(miStart, "SHR", disp, imm);
  }

  // logical shift right of [base + index<<scale + disp] by imm
  public final void emitSHR_RegIdx_Imm_Byte(byte base, byte index, short scale, Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD0);
        emitSIBRegOperands(base, index, scale, disp, (byte) 0x5);
    } else {
        setMachineCodes(mi++, (byte) 0xC0);
        emitSIBRegOperands(base, index, scale, disp, (byte) 0x5);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RXDI(miStart, "SHR", base, index, scale, disp, imm);
  }

  // logical shift right of dataReg by shiftBy
  public final void emitSHR_Reg_Reg_Byte(byte dataReg, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD2);
    emitRegRegOperands(dataReg, (byte) 0x5);
    if (lister != null) lister.RR(miStart, "SHR", dataReg, shiftBy);
  }

  // logical shift right of [dataReg] by shiftBy
  public final void emitSHR_RegInd_Reg_Byte(byte dataReg, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD2);
    emitRegIndirectRegOperands(dataReg, (byte) 0x5);
    if (lister != null) lister.RNR(miStart, "SHR", dataReg, shiftBy);
  }

  // logical shift right of [dataReg + disp] by shiftBy
  public final void emitSHR_RegDisp_Reg_Byte(byte dataReg, Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD2);
    emitRegDispRegOperands(dataReg, disp, (byte) 0x5);
    if (lister != null) lister.RDR(miStart, "SHR", dataReg, disp, shiftBy);
  }

  // logical shift right of [indexReg<<scale + disp] by shiftBy
  public final void emitSHR_RegOff_Reg_Byte(byte indexReg, short scale, Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD2);
    emitRegOffRegOperands(indexReg, scale, disp, (byte) 0x5);
    if (lister != null) lister.RFDR(miStart, "SHR", indexReg, scale, disp, shiftBy);
  }

  // logical shift right of [disp] by shiftBy
  public final void emitSHR_Abs_Reg_Byte(Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD2);
    emitAbsRegOperands(disp, (byte) 0x5);
    if (lister != null) lister.RAR(miStart, "SHR", disp, shiftBy);
  }

  // logical shift right of [baseReg + indexReg<<scale + disp] by shiftBy
  public final void emitSHR_RegIdx_Reg_Byte(byte baseReg, byte indexReg, short scale, Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD2);
    emitSIBRegOperands(baseReg, indexReg, scale, disp, (byte) 0x5);
    if (lister != null) lister.RXDR(miStart, "SHR", baseReg, indexReg, scale, disp, shiftBy);
  }

  // logical shift right of reg by imm
  public final void emitSHR_Reg_Imm_Word(byte reg, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
        setMachineCodes(mi++, (byte) 0x66);
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitRegRegOperands(reg, (byte) 0x5);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitRegRegOperands(reg, (byte) 0x5);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RI(miStart, "SHR", reg, imm);
    }

  // logical shift right of [reg] by imm
  public final void emitSHR_RegInd_Imm_Word(byte reg, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
        setMachineCodes(mi++, (byte) 0x66);
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitRegIndirectRegOperands(reg, (byte) 0x5);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitRegIndirectRegOperands(reg, (byte) 0x5);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RNI(miStart, "SHR", reg, imm);
    }

  // logical shift right of [reg + disp] by imm
  public final void emitSHR_RegDisp_Imm_Word(byte reg, Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
        setMachineCodes(mi++, (byte) 0x66);
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitRegDispRegOperands(reg, disp, (byte) 0x5);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitRegDispRegOperands(reg, disp, (byte) 0x5);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RDI(miStart, "SHR", reg, disp, imm);
    }

  // logical shift right of [index<<scale + disp] by imm
  public final void emitSHR_RegOff_Imm_Word(byte index, short scale, Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
        setMachineCodes(mi++, (byte) 0x66);
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitRegOffRegOperands(index, scale, disp, (byte) 0x5);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitRegOffRegOperands(index, scale, disp, (byte) 0x5);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RFDI(miStart, "SHR", index, scale, disp, imm);
    }

  // logical shift right of [disp] by imm
  public final void emitSHR_Abs_Imm_Word(Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
        setMachineCodes(mi++, (byte) 0x66);
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitAbsRegOperands(disp, (byte) 0x5);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitAbsRegOperands(disp, (byte) 0x5);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RAI(miStart, "SHR", disp, imm);
  }

  // logical shift right of [base + index<<scale + disp] by imm
  public final void emitSHR_RegIdx_Imm_Word(byte base, byte index, short scale, Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
        setMachineCodes(mi++, (byte) 0x66);
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitSIBRegOperands(base, index, scale, disp, (byte) 0x5);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitSIBRegOperands(base, index, scale, disp, (byte) 0x5);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RXDI(miStart, "SHR", base, index, scale, disp, imm);
  }

  // logical shift right of dataReg by shiftBy
  public final void emitSHR_Reg_Reg_Word(byte dataReg, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0xD3);
    emitRegRegOperands(dataReg, (byte) 0x5);
    if (lister != null) lister.RR(miStart, "SHR", dataReg, shiftBy);
  }

  // logical shift right of [dataReg] by shiftBy
  public final void emitSHR_RegInd_Reg_Word(byte dataReg, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0xD3);
    emitRegIndirectRegOperands(dataReg, (byte) 0x5);
    if (lister != null) lister.RNR(miStart, "SHR", dataReg, shiftBy);
  }

  // logical shift right of [dataReg + disp] by shiftBy
  public final void emitSHR_RegDisp_Reg_Word(byte dataReg, Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0xD3);
    emitRegDispRegOperands(dataReg, disp, (byte) 0x5);
    if (lister != null) lister.RDR(miStart, "SHR", dataReg, disp, shiftBy);
  }

  // logical shift right of [indexReg<<scale + disp] by shiftBy
  public final void emitSHR_RegOff_Reg_Word(byte indexReg, short scale, Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0xD3);
    emitRegOffRegOperands(indexReg, scale, disp, (byte) 0x5);
    if (lister != null) lister.RFDR(miStart, "SHR", indexReg, scale, disp, shiftBy);
  }

  // logical shift right of [disp] by shiftBy
  public final void emitSHR_Abs_Reg_Word(Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0xD3);
    emitAbsRegOperands(disp, (byte) 0x5);
    if (lister != null) lister.RAR(miStart, "SHR", disp, shiftBy);
  }

  // logical shift right of [baseReg + indexReg<<scale + disp] by shiftBy
  public final void emitSHR_RegIdx_Reg_Word(byte baseReg, byte indexReg, short scale, Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0xD3);
    emitSIBRegOperands(baseReg, indexReg, scale, disp, (byte) 0x5);
    if (lister != null) lister.RXDR(miStart, "SHR", baseReg, indexReg, scale, disp, shiftBy);
  }

  // logical shift right of reg by imm
  public final void emitSHR_Reg_Imm(byte reg, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitRegRegOperands(reg, (byte) 0x5);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitRegRegOperands(reg, (byte) 0x5);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RI(miStart, "SHR", reg, imm);
    }

  // logical shift right of [reg] by imm
  public final void emitSHR_RegInd_Imm(byte reg, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitRegIndirectRegOperands(reg, (byte) 0x5);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitRegIndirectRegOperands(reg, (byte) 0x5);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RNI(miStart, "SHR", reg, imm);
    }

  // logical shift right of [reg + disp] by imm
  public final void emitSHR_RegDisp_Imm(byte reg, Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitRegDispRegOperands(reg, disp, (byte) 0x5);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitRegDispRegOperands(reg, disp, (byte) 0x5);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RDI(miStart, "SHR", reg, disp, imm);
    }

  // logical shift right of [index<<scale + disp] by imm
  public final void emitSHR_RegOff_Imm(byte index, short scale, Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitRegOffRegOperands(index, scale, disp, (byte) 0x5);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitRegOffRegOperands(index, scale, disp, (byte) 0x5);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RFDI(miStart, "SHR", index, scale, disp, imm);
    }

  // logical shift right of [disp] by imm
  public final void emitSHR_Abs_Imm(Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitAbsRegOperands(disp, (byte) 0x5);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitAbsRegOperands(disp, (byte) 0x5);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RAI(miStart, "SHR", disp, imm);
  }

  // logical shift right of [base + index<<scale + disp] by imm
  public final void emitSHR_RegIdx_Imm(byte base, byte index, short scale, Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitSIBRegOperands(base, index, scale, disp, (byte) 0x5);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitSIBRegOperands(base, index, scale, disp, (byte) 0x5);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RXDI(miStart, "SHR", base, index, scale, disp, imm);
  }

  // logical shift right of dataReg by shiftBy
  public final void emitSHR_Reg_Reg(byte dataReg, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD3);
    emitRegRegOperands(dataReg, (byte) 0x5);
    if (lister != null) lister.RR(miStart, "SHR", dataReg, shiftBy);
  }

  // logical shift right of [dataReg] by shiftBy
  public final void emitSHR_RegInd_Reg(byte dataReg, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD3);
    emitRegIndirectRegOperands(dataReg, (byte) 0x5);
    if (lister != null) lister.RNR(miStart, "SHR", dataReg, shiftBy);
  }

  // logical shift right of [dataReg + disp] by shiftBy
  public final void emitSHR_RegDisp_Reg(byte dataReg, Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD3);
    emitRegDispRegOperands(dataReg, disp, (byte) 0x5);
    if (lister != null) lister.RDR(miStart, "SHR", dataReg, disp, shiftBy);
  }

  // logical shift right of [indexReg<<scale + disp] by shiftBy
  public final void emitSHR_RegOff_Reg(byte indexReg, short scale, Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD3);
    emitRegOffRegOperands(indexReg, scale, disp, (byte) 0x5);
    if (lister != null) lister.RFDR(miStart, "SHR", indexReg, scale, disp, shiftBy);
  }

  // logical shift right of [disp] by shiftBy
  public final void emitSHR_Abs_Reg(Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD3);
    emitAbsRegOperands(disp, (byte) 0x5);
    if (lister != null) lister.RAR(miStart, "SHR", disp, shiftBy);
  }

  // logical shift right of [baseReg + indexReg<<scale + disp] by shiftBy
  public final void emitSHR_RegIdx_Reg(byte baseReg, byte indexReg, short scale, Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD3);
    emitSIBRegOperands(baseReg, indexReg, scale, disp, (byte) 0x5);
    if (lister != null) lister.RXDR(miStart, "SHR", baseReg, indexReg, scale, disp, shiftBy);
  }

  // rotate left with carry of reg by imm
  public final void emitRCL_Reg_Imm_Byte(byte reg, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD0);
        emitRegRegOperands(reg, (byte) 0x2);
    } else {
        setMachineCodes(mi++, (byte) 0xC0);
        emitRegRegOperands(reg, (byte) 0x2);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RI(miStart, "RCL", reg, imm);
    }

  // rotate left with carry of [reg] by imm
  public final void emitRCL_RegInd_Imm_Byte(byte reg, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD0);
        emitRegIndirectRegOperands(reg, (byte) 0x2);
    } else {
        setMachineCodes(mi++, (byte) 0xC0);
        emitRegIndirectRegOperands(reg, (byte) 0x2);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RNI(miStart, "RCL", reg, imm);
    }

  // rotate left with carry of [reg + disp] by imm
  public final void emitRCL_RegDisp_Imm_Byte(byte reg, Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD0);
        emitRegDispRegOperands(reg, disp, (byte) 0x2);
    } else {
        setMachineCodes(mi++, (byte) 0xC0);
        emitRegDispRegOperands(reg, disp, (byte) 0x2);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RDI(miStart, "RCL", reg, disp, imm);
    }

  // rotate left with carry of [index<<scale + disp] by imm
  public final void emitRCL_RegOff_Imm_Byte(byte index, short scale, Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD0);
        emitRegOffRegOperands(index, scale, disp, (byte) 0x2);
    } else {
        setMachineCodes(mi++, (byte) 0xC0);
        emitRegOffRegOperands(index, scale, disp, (byte) 0x2);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RFDI(miStart, "RCL", index, scale, disp, imm);
    }

  // rotate left with carry of [disp] by imm
  public final void emitRCL_Abs_Imm_Byte(Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD0);
        emitAbsRegOperands(disp, (byte) 0x2);
    } else {
        setMachineCodes(mi++, (byte) 0xC0);
        emitAbsRegOperands(disp, (byte) 0x2);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RAI(miStart, "RCL", disp, imm);
  }

  // rotate left with carry of [base + index<<scale + disp] by imm
  public final void emitRCL_RegIdx_Imm_Byte(byte base, byte index, short scale, Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD0);
        emitSIBRegOperands(base, index, scale, disp, (byte) 0x2);
    } else {
        setMachineCodes(mi++, (byte) 0xC0);
        emitSIBRegOperands(base, index, scale, disp, (byte) 0x2);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RXDI(miStart, "RCL", base, index, scale, disp, imm);
  }

  // rotate left with carry of dataReg by shiftBy
  public final void emitRCL_Reg_Reg_Byte(byte dataReg, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD2);
    emitRegRegOperands(dataReg, (byte) 0x2);
    if (lister != null) lister.RR(miStart, "RCL", dataReg, shiftBy);
  }

  // rotate left with carry of [dataReg] by shiftBy
  public final void emitRCL_RegInd_Reg_Byte(byte dataReg, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD2);
    emitRegIndirectRegOperands(dataReg, (byte) 0x2);
    if (lister != null) lister.RNR(miStart, "RCL", dataReg, shiftBy);
  }

  // rotate left with carry of [dataReg + disp] by shiftBy
  public final void emitRCL_RegDisp_Reg_Byte(byte dataReg, Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD2);
    emitRegDispRegOperands(dataReg, disp, (byte) 0x2);
    if (lister != null) lister.RDR(miStart, "RCL", dataReg, disp, shiftBy);
  }

  // rotate left with carry of [indexReg<<scale + disp] by shiftBy
  public final void emitRCL_RegOff_Reg_Byte(byte indexReg, short scale, Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD2);
    emitRegOffRegOperands(indexReg, scale, disp, (byte) 0x2);
    if (lister != null) lister.RFDR(miStart, "RCL", indexReg, scale, disp, shiftBy);
  }

  // rotate left with carry of [disp] by shiftBy
  public final void emitRCL_Abs_Reg_Byte(Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD2);
    emitAbsRegOperands(disp, (byte) 0x2);
    if (lister != null) lister.RAR(miStart, "RCL", disp, shiftBy);
  }

  // rotate left with carry of [baseReg + indexReg<<scale + disp] by shiftBy
  public final void emitRCL_RegIdx_Reg_Byte(byte baseReg, byte indexReg, short scale, Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD2);
    emitSIBRegOperands(baseReg, indexReg, scale, disp, (byte) 0x2);
    if (lister != null) lister.RXDR(miStart, "RCL", baseReg, indexReg, scale, disp, shiftBy);
  }

  // rotate left with carry of reg by imm
  public final void emitRCL_Reg_Imm_Word(byte reg, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
        setMachineCodes(mi++, (byte) 0x66);
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitRegRegOperands(reg, (byte) 0x2);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitRegRegOperands(reg, (byte) 0x2);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RI(miStart, "RCL", reg, imm);
    }

  // rotate left with carry of [reg] by imm
  public final void emitRCL_RegInd_Imm_Word(byte reg, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
        setMachineCodes(mi++, (byte) 0x66);
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitRegIndirectRegOperands(reg, (byte) 0x2);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitRegIndirectRegOperands(reg, (byte) 0x2);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RNI(miStart, "RCL", reg, imm);
    }

  // rotate left with carry of [reg + disp] by imm
  public final void emitRCL_RegDisp_Imm_Word(byte reg, Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
        setMachineCodes(mi++, (byte) 0x66);
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitRegDispRegOperands(reg, disp, (byte) 0x2);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitRegDispRegOperands(reg, disp, (byte) 0x2);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RDI(miStart, "RCL", reg, disp, imm);
    }

  // rotate left with carry of [index<<scale + disp] by imm
  public final void emitRCL_RegOff_Imm_Word(byte index, short scale, Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
        setMachineCodes(mi++, (byte) 0x66);
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitRegOffRegOperands(index, scale, disp, (byte) 0x2);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitRegOffRegOperands(index, scale, disp, (byte) 0x2);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RFDI(miStart, "RCL", index, scale, disp, imm);
    }

  // rotate left with carry of [disp] by imm
  public final void emitRCL_Abs_Imm_Word(Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
        setMachineCodes(mi++, (byte) 0x66);
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitAbsRegOperands(disp, (byte) 0x2);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitAbsRegOperands(disp, (byte) 0x2);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RAI(miStart, "RCL", disp, imm);
  }

  // rotate left with carry of [base + index<<scale + disp] by imm
  public final void emitRCL_RegIdx_Imm_Word(byte base, byte index, short scale, Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
        setMachineCodes(mi++, (byte) 0x66);
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitSIBRegOperands(base, index, scale, disp, (byte) 0x2);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitSIBRegOperands(base, index, scale, disp, (byte) 0x2);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RXDI(miStart, "RCL", base, index, scale, disp, imm);
  }

  // rotate left with carry of dataReg by shiftBy
  public final void emitRCL_Reg_Reg_Word(byte dataReg, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0xD3);
    emitRegRegOperands(dataReg, (byte) 0x2);
    if (lister != null) lister.RR(miStart, "RCL", dataReg, shiftBy);
  }

  // rotate left with carry of [dataReg] by shiftBy
  public final void emitRCL_RegInd_Reg_Word(byte dataReg, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0xD3);
    emitRegIndirectRegOperands(dataReg, (byte) 0x2);
    if (lister != null) lister.RNR(miStart, "RCL", dataReg, shiftBy);
  }

  // rotate left with carry of [dataReg + disp] by shiftBy
  public final void emitRCL_RegDisp_Reg_Word(byte dataReg, Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0xD3);
    emitRegDispRegOperands(dataReg, disp, (byte) 0x2);
    if (lister != null) lister.RDR(miStart, "RCL", dataReg, disp, shiftBy);
  }

  // rotate left with carry of [indexReg<<scale + disp] by shiftBy
  public final void emitRCL_RegOff_Reg_Word(byte indexReg, short scale, Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0xD3);
    emitRegOffRegOperands(indexReg, scale, disp, (byte) 0x2);
    if (lister != null) lister.RFDR(miStart, "RCL", indexReg, scale, disp, shiftBy);
  }

  // rotate left with carry of [disp] by shiftBy
  public final void emitRCL_Abs_Reg_Word(Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0xD3);
    emitAbsRegOperands(disp, (byte) 0x2);
    if (lister != null) lister.RAR(miStart, "RCL", disp, shiftBy);
  }

  // rotate left with carry of [baseReg + indexReg<<scale + disp] by shiftBy
  public final void emitRCL_RegIdx_Reg_Word(byte baseReg, byte indexReg, short scale, Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0xD3);
    emitSIBRegOperands(baseReg, indexReg, scale, disp, (byte) 0x2);
    if (lister != null) lister.RXDR(miStart, "RCL", baseReg, indexReg, scale, disp, shiftBy);
  }

  // rotate left with carry  of reg by imm
  public final void emitRCL_Reg_Imm(byte reg, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitRegRegOperands(reg, (byte) 0x2);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitRegRegOperands(reg, (byte) 0x2);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RI(miStart, "RCL", reg, imm);
    }

  // rotate left with carry  of [reg] by imm
  public final void emitRCL_RegInd_Imm(byte reg, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitRegIndirectRegOperands(reg, (byte) 0x2);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitRegIndirectRegOperands(reg, (byte) 0x2);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RNI(miStart, "RCL", reg, imm);
    }

  // rotate left with carry  of [reg + disp] by imm
  public final void emitRCL_RegDisp_Imm(byte reg, Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitRegDispRegOperands(reg, disp, (byte) 0x2);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitRegDispRegOperands(reg, disp, (byte) 0x2);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RDI(miStart, "RCL", reg, disp, imm);
    }

  // rotate left with carry  of [index<<scale + disp] by imm
  public final void emitRCL_RegOff_Imm(byte index, short scale, Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitRegOffRegOperands(index, scale, disp, (byte) 0x2);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitRegOffRegOperands(index, scale, disp, (byte) 0x2);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RFDI(miStart, "RCL", index, scale, disp, imm);
    }

  // rotate left with carry  of [disp] by imm
  public final void emitRCL_Abs_Imm(Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitAbsRegOperands(disp, (byte) 0x2);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitAbsRegOperands(disp, (byte) 0x2);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RAI(miStart, "RCL", disp, imm);
  }

  // rotate left with carry  of [base + index<<scale + disp] by imm
  public final void emitRCL_RegIdx_Imm(byte base, byte index, short scale, Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitSIBRegOperands(base, index, scale, disp, (byte) 0x2);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitSIBRegOperands(base, index, scale, disp, (byte) 0x2);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RXDI(miStart, "RCL", base, index, scale, disp, imm);
  }

  // rotate left with carry  of dataReg by shiftBy
  public final void emitRCL_Reg_Reg(byte dataReg, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD3);
    emitRegRegOperands(dataReg, (byte) 0x2);
    if (lister != null) lister.RR(miStart, "RCL", dataReg, shiftBy);
  }

  // rotate left with carry  of [dataReg] by shiftBy
  public final void emitRCL_RegInd_Reg(byte dataReg, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD3);
    emitRegIndirectRegOperands(dataReg, (byte) 0x2);
    if (lister != null) lister.RNR(miStart, "RCL", dataReg, shiftBy);
  }

  // rotate left with carry  of [dataReg + disp] by shiftBy
  public final void emitRCL_RegDisp_Reg(byte dataReg, Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD3);
    emitRegDispRegOperands(dataReg, disp, (byte) 0x2);
    if (lister != null) lister.RDR(miStart, "RCL", dataReg, disp, shiftBy);
  }

  // rotate left with carry  of [indexReg<<scale + disp] by shiftBy
  public final void emitRCL_RegOff_Reg(byte indexReg, short scale, Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD3);
    emitRegOffRegOperands(indexReg, scale, disp, (byte) 0x2);
    if (lister != null) lister.RFDR(miStart, "RCL", indexReg, scale, disp, shiftBy);
  }

  // rotate left with carry  of [disp] by shiftBy
  public final void emitRCL_Abs_Reg(Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD3);
    emitAbsRegOperands(disp, (byte) 0x2);
    if (lister != null) lister.RAR(miStart, "RCL", disp, shiftBy);
  }

  // rotate left with carry  of [baseReg + indexReg<<scale + disp] by shiftBy
  public final void emitRCL_RegIdx_Reg(byte baseReg, byte indexReg, short scale, Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD3);
    emitSIBRegOperands(baseReg, indexReg, scale, disp, (byte) 0x2);
    if (lister != null) lister.RXDR(miStart, "RCL", baseReg, indexReg, scale, disp, shiftBy);
  }

  // rotate right with carry of reg by imm
  public final void emitRCR_Reg_Imm_Byte(byte reg, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD0);
        emitRegRegOperands(reg, (byte) 0x3);
    } else {
        setMachineCodes(mi++, (byte) 0xC0);
        emitRegRegOperands(reg, (byte) 0x3);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RI(miStart, "RCR", reg, imm);
    }

  // rotate right with carry of [reg] by imm
  public final void emitRCR_RegInd_Imm_Byte(byte reg, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD0);
        emitRegIndirectRegOperands(reg, (byte) 0x3);
    } else {
        setMachineCodes(mi++, (byte) 0xC0);
        emitRegIndirectRegOperands(reg, (byte) 0x3);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RNI(miStart, "RCR", reg, imm);
    }

  // rotate right with carry of [reg + disp] by imm
  public final void emitRCR_RegDisp_Imm_Byte(byte reg, Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD0);
        emitRegDispRegOperands(reg, disp, (byte) 0x3);
    } else {
        setMachineCodes(mi++, (byte) 0xC0);
        emitRegDispRegOperands(reg, disp, (byte) 0x3);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RDI(miStart, "RCR", reg, disp, imm);
    }

  // rotate right with carry of [index<<scale + disp] by imm
  public final void emitRCR_RegOff_Imm_Byte(byte index, short scale, Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD0);
        emitRegOffRegOperands(index, scale, disp, (byte) 0x3);
    } else {
        setMachineCodes(mi++, (byte) 0xC0);
        emitRegOffRegOperands(index, scale, disp, (byte) 0x3);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RFDI(miStart, "RCR", index, scale, disp, imm);
    }

  // rotate right with carry of [disp] by imm
  public final void emitRCR_Abs_Imm_Byte(Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD0);
        emitAbsRegOperands(disp, (byte) 0x3);
    } else {
        setMachineCodes(mi++, (byte) 0xC0);
        emitAbsRegOperands(disp, (byte) 0x3);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RAI(miStart, "RCR", disp, imm);
  }

  // rotate right with carry of [base + index<<scale + disp] by imm
  public final void emitRCR_RegIdx_Imm_Byte(byte base, byte index, short scale, Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD0);
        emitSIBRegOperands(base, index, scale, disp, (byte) 0x3);
    } else {
        setMachineCodes(mi++, (byte) 0xC0);
        emitSIBRegOperands(base, index, scale, disp, (byte) 0x3);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RXDI(miStart, "RCR", base, index, scale, disp, imm);
  }

  // rotate right with carry of dataReg by shiftBy
  public final void emitRCR_Reg_Reg_Byte(byte dataReg, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD2);
    emitRegRegOperands(dataReg, (byte) 0x3);
    if (lister != null) lister.RR(miStart, "RCR", dataReg, shiftBy);
  }

  // rotate right with carry of [dataReg] by shiftBy
  public final void emitRCR_RegInd_Reg_Byte(byte dataReg, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD2);
    emitRegIndirectRegOperands(dataReg, (byte) 0x3);
    if (lister != null) lister.RNR(miStart, "RCR", dataReg, shiftBy);
  }

  // rotate right with carry of [dataReg + disp] by shiftBy
  public final void emitRCR_RegDisp_Reg_Byte(byte dataReg, Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD2);
    emitRegDispRegOperands(dataReg, disp, (byte) 0x3);
    if (lister != null) lister.RDR(miStart, "RCR", dataReg, disp, shiftBy);
  }

  // rotate right with carry of [indexReg<<scale + disp] by shiftBy
  public final void emitRCR_RegOff_Reg_Byte(byte indexReg, short scale, Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD2);
    emitRegOffRegOperands(indexReg, scale, disp, (byte) 0x3);
    if (lister != null) lister.RFDR(miStart, "RCR", indexReg, scale, disp, shiftBy);
  }

  // rotate right with carry of [disp] by shiftBy
  public final void emitRCR_Abs_Reg_Byte(Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD2);
    emitAbsRegOperands(disp, (byte) 0x3);
    if (lister != null) lister.RAR(miStart, "RCR", disp, shiftBy);
  }

  // rotate right with carry of [baseReg + indexReg<<scale + disp] by shiftBy
  public final void emitRCR_RegIdx_Reg_Byte(byte baseReg, byte indexReg, short scale, Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD2);
    emitSIBRegOperands(baseReg, indexReg, scale, disp, (byte) 0x3);
    if (lister != null) lister.RXDR(miStart, "RCR", baseReg, indexReg, scale, disp, shiftBy);
  }

  // rotate right with carry of reg by imm
  public final void emitRCR_Reg_Imm_Word(byte reg, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
        setMachineCodes(mi++, (byte) 0x66);
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitRegRegOperands(reg, (byte) 0x3);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitRegRegOperands(reg, (byte) 0x3);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RI(miStart, "RCR", reg, imm);
    }

  // rotate right with carry of [reg] by imm
  public final void emitRCR_RegInd_Imm_Word(byte reg, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
        setMachineCodes(mi++, (byte) 0x66);
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitRegIndirectRegOperands(reg, (byte) 0x3);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitRegIndirectRegOperands(reg, (byte) 0x3);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RNI(miStart, "RCR", reg, imm);
    }

  // rotate right with carry of [reg + disp] by imm
  public final void emitRCR_RegDisp_Imm_Word(byte reg, Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
        setMachineCodes(mi++, (byte) 0x66);
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitRegDispRegOperands(reg, disp, (byte) 0x3);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitRegDispRegOperands(reg, disp, (byte) 0x3);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RDI(miStart, "RCR", reg, disp, imm);
    }

  // rotate right with carry of [index<<scale + disp] by imm
  public final void emitRCR_RegOff_Imm_Word(byte index, short scale, Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
        setMachineCodes(mi++, (byte) 0x66);
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitRegOffRegOperands(index, scale, disp, (byte) 0x3);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitRegOffRegOperands(index, scale, disp, (byte) 0x3);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RFDI(miStart, "RCR", index, scale, disp, imm);
    }

  // rotate right with carry of [disp] by imm
  public final void emitRCR_Abs_Imm_Word(Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
        setMachineCodes(mi++, (byte) 0x66);
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitAbsRegOperands(disp, (byte) 0x3);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitAbsRegOperands(disp, (byte) 0x3);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RAI(miStart, "RCR", disp, imm);
  }

  // rotate right with carry of [base + index<<scale + disp] by imm
  public final void emitRCR_RegIdx_Imm_Word(byte base, byte index, short scale, Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
        setMachineCodes(mi++, (byte) 0x66);
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitSIBRegOperands(base, index, scale, disp, (byte) 0x3);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitSIBRegOperands(base, index, scale, disp, (byte) 0x3);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RXDI(miStart, "RCR", base, index, scale, disp, imm);
  }

  // rotate right with carry of dataReg by shiftBy
  public final void emitRCR_Reg_Reg_Word(byte dataReg, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0xD3);
    emitRegRegOperands(dataReg, (byte) 0x3);
    if (lister != null) lister.RR(miStart, "RCR", dataReg, shiftBy);
  }

  // rotate right with carry of [dataReg] by shiftBy
  public final void emitRCR_RegInd_Reg_Word(byte dataReg, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0xD3);
    emitRegIndirectRegOperands(dataReg, (byte) 0x3);
    if (lister != null) lister.RNR(miStart, "RCR", dataReg, shiftBy);
  }

  // rotate right with carry of [dataReg + disp] by shiftBy
  public final void emitRCR_RegDisp_Reg_Word(byte dataReg, Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0xD3);
    emitRegDispRegOperands(dataReg, disp, (byte) 0x3);
    if (lister != null) lister.RDR(miStart, "RCR", dataReg, disp, shiftBy);
  }

  // rotate right with carry of [indexReg<<scale + disp] by shiftBy
  public final void emitRCR_RegOff_Reg_Word(byte indexReg, short scale, Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0xD3);
    emitRegOffRegOperands(indexReg, scale, disp, (byte) 0x3);
    if (lister != null) lister.RFDR(miStart, "RCR", indexReg, scale, disp, shiftBy);
  }

  // rotate right with carry of [disp] by shiftBy
  public final void emitRCR_Abs_Reg_Word(Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0xD3);
    emitAbsRegOperands(disp, (byte) 0x3);
    if (lister != null) lister.RAR(miStart, "RCR", disp, shiftBy);
  }

  // rotate right with carry of [baseReg + indexReg<<scale + disp] by shiftBy
  public final void emitRCR_RegIdx_Reg_Word(byte baseReg, byte indexReg, short scale, Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0xD3);
    emitSIBRegOperands(baseReg, indexReg, scale, disp, (byte) 0x3);
    if (lister != null) lister.RXDR(miStart, "RCR", baseReg, indexReg, scale, disp, shiftBy);
  }

  // rotate right with carry of reg by imm
  public final void emitRCR_Reg_Imm(byte reg, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitRegRegOperands(reg, (byte) 0x3);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitRegRegOperands(reg, (byte) 0x3);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RI(miStart, "RCR", reg, imm);
    }

  // rotate right with carry of [reg] by imm
  public final void emitRCR_RegInd_Imm(byte reg, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitRegIndirectRegOperands(reg, (byte) 0x3);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitRegIndirectRegOperands(reg, (byte) 0x3);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RNI(miStart, "RCR", reg, imm);
    }

  // rotate right with carry of [reg + disp] by imm
  public final void emitRCR_RegDisp_Imm(byte reg, Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitRegDispRegOperands(reg, disp, (byte) 0x3);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitRegDispRegOperands(reg, disp, (byte) 0x3);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RDI(miStart, "RCR", reg, disp, imm);
    }

  // rotate right with carry of [index<<scale + disp] by imm
  public final void emitRCR_RegOff_Imm(byte index, short scale, Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitRegOffRegOperands(index, scale, disp, (byte) 0x3);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitRegOffRegOperands(index, scale, disp, (byte) 0x3);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RFDI(miStart, "RCR", index, scale, disp, imm);
    }

  // rotate right with carry of [disp] by imm
  public final void emitRCR_Abs_Imm(Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitAbsRegOperands(disp, (byte) 0x3);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitAbsRegOperands(disp, (byte) 0x3);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RAI(miStart, "RCR", disp, imm);
  }

  // rotate right with carry of [base + index<<scale + disp] by imm
  public final void emitRCR_RegIdx_Imm(byte base, byte index, short scale, Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitSIBRegOperands(base, index, scale, disp, (byte) 0x3);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitSIBRegOperands(base, index, scale, disp, (byte) 0x3);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RXDI(miStart, "RCR", base, index, scale, disp, imm);
  }

  // rotate right with carry of dataReg by shiftBy
  public final void emitRCR_Reg_Reg(byte dataReg, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD3);
    emitRegRegOperands(dataReg, (byte) 0x3);
    if (lister != null) lister.RR(miStart, "RCR", dataReg, shiftBy);
  }

  // rotate right with carry of [dataReg] by shiftBy
  public final void emitRCR_RegInd_Reg(byte dataReg, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD3);
    emitRegIndirectRegOperands(dataReg, (byte) 0x3);
    if (lister != null) lister.RNR(miStart, "RCR", dataReg, shiftBy);
  }

  // rotate right with carry of [dataReg + disp] by shiftBy
  public final void emitRCR_RegDisp_Reg(byte dataReg, Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD3);
    emitRegDispRegOperands(dataReg, disp, (byte) 0x3);
    if (lister != null) lister.RDR(miStart, "RCR", dataReg, disp, shiftBy);
  }

  // rotate right with carry of [indexReg<<scale + disp] by shiftBy
  public final void emitRCR_RegOff_Reg(byte indexReg, short scale, Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD3);
    emitRegOffRegOperands(indexReg, scale, disp, (byte) 0x3);
    if (lister != null) lister.RFDR(miStart, "RCR", indexReg, scale, disp, shiftBy);
  }

  // rotate right with carry of [disp] by shiftBy
  public final void emitRCR_Abs_Reg(Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD3);
    emitAbsRegOperands(disp, (byte) 0x3);
    if (lister != null) lister.RAR(miStart, "RCR", disp, shiftBy);
  }

  // rotate right with carry of [baseReg + indexReg<<scale + disp] by shiftBy
  public final void emitRCR_RegIdx_Reg(byte baseReg, byte indexReg, short scale, Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD3);
    emitSIBRegOperands(baseReg, indexReg, scale, disp, (byte) 0x3);
    if (lister != null) lister.RXDR(miStart, "RCR", baseReg, indexReg, scale, disp, shiftBy);
  }

  // rotate left of reg by imm
  public final void emitROL_Reg_Imm_Byte(byte reg, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD0);
        emitRegRegOperands(reg, (byte) 0x0);
    } else {
        setMachineCodes(mi++, (byte) 0xC0);
        emitRegRegOperands(reg, (byte) 0x0);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RI(miStart, "ROL", reg, imm);
    }

  // rotate left of [reg] by imm
  public final void emitROL_RegInd_Imm_Byte(byte reg, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD0);
        emitRegIndirectRegOperands(reg, (byte) 0x0);
    } else {
        setMachineCodes(mi++, (byte) 0xC0);
        emitRegIndirectRegOperands(reg, (byte) 0x0);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RNI(miStart, "ROL", reg, imm);
    }

  // rotate left of [reg + disp] by imm
  public final void emitROL_RegDisp_Imm_Byte(byte reg, Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD0);
        emitRegDispRegOperands(reg, disp, (byte) 0x0);
    } else {
        setMachineCodes(mi++, (byte) 0xC0);
        emitRegDispRegOperands(reg, disp, (byte) 0x0);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RDI(miStart, "ROL", reg, disp, imm);
    }

  // rotate left of [index<<scale + disp] by imm
  public final void emitROL_RegOff_Imm_Byte(byte index, short scale, Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD0);
        emitRegOffRegOperands(index, scale, disp, (byte) 0x0);
    } else {
        setMachineCodes(mi++, (byte) 0xC0);
        emitRegOffRegOperands(index, scale, disp, (byte) 0x0);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RFDI(miStart, "ROL", index, scale, disp, imm);
    }

  // rotate left of [disp] by imm
  public final void emitROL_Abs_Imm_Byte(Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD0);
        emitAbsRegOperands(disp, (byte) 0x0);
    } else {
        setMachineCodes(mi++, (byte) 0xC0);
        emitAbsRegOperands(disp, (byte) 0x0);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RAI(miStart, "ROL", disp, imm);
  }

  // rotate left of [base + index<<scale + disp] by imm
  public final void emitROL_RegIdx_Imm_Byte(byte base, byte index, short scale, Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD0);
        emitSIBRegOperands(base, index, scale, disp, (byte) 0x0);
    } else {
        setMachineCodes(mi++, (byte) 0xC0);
        emitSIBRegOperands(base, index, scale, disp, (byte) 0x0);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RXDI(miStart, "ROL", base, index, scale, disp, imm);
  }

  // rotate left of dataReg by shiftBy
  public final void emitROL_Reg_Reg_Byte(byte dataReg, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD2);
    emitRegRegOperands(dataReg, (byte) 0x0);
    if (lister != null) lister.RR(miStart, "ROL", dataReg, shiftBy);
  }

  // rotate left of [dataReg] by shiftBy
  public final void emitROL_RegInd_Reg_Byte(byte dataReg, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD2);
    emitRegIndirectRegOperands(dataReg, (byte) 0x0);
    if (lister != null) lister.RNR(miStart, "ROL", dataReg, shiftBy);
  }

  // rotate left of [dataReg + disp] by shiftBy
  public final void emitROL_RegDisp_Reg_Byte(byte dataReg, Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD2);
    emitRegDispRegOperands(dataReg, disp, (byte) 0x0);
    if (lister != null) lister.RDR(miStart, "ROL", dataReg, disp, shiftBy);
  }

  // rotate left of [indexReg<<scale + disp] by shiftBy
  public final void emitROL_RegOff_Reg_Byte(byte indexReg, short scale, Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD2);
    emitRegOffRegOperands(indexReg, scale, disp, (byte) 0x0);
    if (lister != null) lister.RFDR(miStart, "ROL", indexReg, scale, disp, shiftBy);
  }

  // rotate left of [disp] by shiftBy
  public final void emitROL_Abs_Reg_Byte(Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD2);
    emitAbsRegOperands(disp, (byte) 0x0);
    if (lister != null) lister.RAR(miStart, "ROL", disp, shiftBy);
  }

  // rotate left of [baseReg + indexReg<<scale + disp] by shiftBy
  public final void emitROL_RegIdx_Reg_Byte(byte baseReg, byte indexReg, short scale, Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD2);
    emitSIBRegOperands(baseReg, indexReg, scale, disp, (byte) 0x0);
    if (lister != null) lister.RXDR(miStart, "ROL", baseReg, indexReg, scale, disp, shiftBy);
  }

  // rotate left of reg by imm
  public final void emitROL_Reg_Imm_Word(byte reg, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
        setMachineCodes(mi++, (byte) 0x66);
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitRegRegOperands(reg, (byte) 0x0);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitRegRegOperands(reg, (byte) 0x0);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RI(miStart, "ROL", reg, imm);
    }

  // rotate left of [reg] by imm
  public final void emitROL_RegInd_Imm_Word(byte reg, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
        setMachineCodes(mi++, (byte) 0x66);
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitRegIndirectRegOperands(reg, (byte) 0x0);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitRegIndirectRegOperands(reg, (byte) 0x0);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RNI(miStart, "ROL", reg, imm);
    }

  // rotate left of [reg + disp] by imm
  public final void emitROL_RegDisp_Imm_Word(byte reg, Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
        setMachineCodes(mi++, (byte) 0x66);
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitRegDispRegOperands(reg, disp, (byte) 0x0);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitRegDispRegOperands(reg, disp, (byte) 0x0);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RDI(miStart, "ROL", reg, disp, imm);
    }

  // rotate left of [index<<scale + disp] by imm
  public final void emitROL_RegOff_Imm_Word(byte index, short scale, Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
        setMachineCodes(mi++, (byte) 0x66);
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitRegOffRegOperands(index, scale, disp, (byte) 0x0);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitRegOffRegOperands(index, scale, disp, (byte) 0x0);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RFDI(miStart, "ROL", index, scale, disp, imm);
    }

  // rotate left of [disp] by imm
  public final void emitROL_Abs_Imm_Word(Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
        setMachineCodes(mi++, (byte) 0x66);
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitAbsRegOperands(disp, (byte) 0x0);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitAbsRegOperands(disp, (byte) 0x0);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RAI(miStart, "ROL", disp, imm);
  }

  // rotate left of [base + index<<scale + disp] by imm
  public final void emitROL_RegIdx_Imm_Word(byte base, byte index, short scale, Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
        setMachineCodes(mi++, (byte) 0x66);
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitSIBRegOperands(base, index, scale, disp, (byte) 0x0);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitSIBRegOperands(base, index, scale, disp, (byte) 0x0);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RXDI(miStart, "ROL", base, index, scale, disp, imm);
  }

  // rotate left of dataReg by shiftBy
  public final void emitROL_Reg_Reg_Word(byte dataReg, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0xD3);
    emitRegRegOperands(dataReg, (byte) 0x0);
    if (lister != null) lister.RR(miStart, "ROL", dataReg, shiftBy);
  }

  // rotate left of [dataReg] by shiftBy
  public final void emitROL_RegInd_Reg_Word(byte dataReg, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0xD3);
    emitRegIndirectRegOperands(dataReg, (byte) 0x0);
    if (lister != null) lister.RNR(miStart, "ROL", dataReg, shiftBy);
  }

  // rotate left of [dataReg + disp] by shiftBy
  public final void emitROL_RegDisp_Reg_Word(byte dataReg, Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0xD3);
    emitRegDispRegOperands(dataReg, disp, (byte) 0x0);
    if (lister != null) lister.RDR(miStart, "ROL", dataReg, disp, shiftBy);
  }

  // rotate left of [indexReg<<scale + disp] by shiftBy
  public final void emitROL_RegOff_Reg_Word(byte indexReg, short scale, Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0xD3);
    emitRegOffRegOperands(indexReg, scale, disp, (byte) 0x0);
    if (lister != null) lister.RFDR(miStart, "ROL", indexReg, scale, disp, shiftBy);
  }

  // rotate left of [disp] by shiftBy
  public final void emitROL_Abs_Reg_Word(Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0xD3);
    emitAbsRegOperands(disp, (byte) 0x0);
    if (lister != null) lister.RAR(miStart, "ROL", disp, shiftBy);
  }

  // rotate left of [baseReg + indexReg<<scale + disp] by shiftBy
  public final void emitROL_RegIdx_Reg_Word(byte baseReg, byte indexReg, short scale, Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0xD3);
    emitSIBRegOperands(baseReg, indexReg, scale, disp, (byte) 0x0);
    if (lister != null) lister.RXDR(miStart, "ROL", baseReg, indexReg, scale, disp, shiftBy);
  }

  // rotate left of reg by imm
  public final void emitROL_Reg_Imm(byte reg, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitRegRegOperands(reg, (byte) 0x0);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitRegRegOperands(reg, (byte) 0x0);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RI(miStart, "ROL", reg, imm);
    }

  // rotate left of [reg] by imm
  public final void emitROL_RegInd_Imm(byte reg, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitRegIndirectRegOperands(reg, (byte) 0x0);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitRegIndirectRegOperands(reg, (byte) 0x0);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RNI(miStart, "ROL", reg, imm);
    }

  // rotate left of [reg + disp] by imm
  public final void emitROL_RegDisp_Imm(byte reg, Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitRegDispRegOperands(reg, disp, (byte) 0x0);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitRegDispRegOperands(reg, disp, (byte) 0x0);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RDI(miStart, "ROL", reg, disp, imm);
    }

  // rotate left of [index<<scale + disp] by imm
  public final void emitROL_RegOff_Imm(byte index, short scale, Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitRegOffRegOperands(index, scale, disp, (byte) 0x0);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitRegOffRegOperands(index, scale, disp, (byte) 0x0);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RFDI(miStart, "ROL", index, scale, disp, imm);
    }

  // rotate left of [disp] by imm
  public final void emitROL_Abs_Imm(Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitAbsRegOperands(disp, (byte) 0x0);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitAbsRegOperands(disp, (byte) 0x0);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RAI(miStart, "ROL", disp, imm);
  }

  // rotate left of [base + index<<scale + disp] by imm
  public final void emitROL_RegIdx_Imm(byte base, byte index, short scale, Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitSIBRegOperands(base, index, scale, disp, (byte) 0x0);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitSIBRegOperands(base, index, scale, disp, (byte) 0x0);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RXDI(miStart, "ROL", base, index, scale, disp, imm);
  }

  // rotate left of dataReg by shiftBy
  public final void emitROL_Reg_Reg(byte dataReg, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD3);
    emitRegRegOperands(dataReg, (byte) 0x0);
    if (lister != null) lister.RR(miStart, "ROL", dataReg, shiftBy);
  }

  // rotate left of [dataReg] by shiftBy
  public final void emitROL_RegInd_Reg(byte dataReg, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD3);
    emitRegIndirectRegOperands(dataReg, (byte) 0x0);
    if (lister != null) lister.RNR(miStart, "ROL", dataReg, shiftBy);
  }

  // rotate left of [dataReg + disp] by shiftBy
  public final void emitROL_RegDisp_Reg(byte dataReg, Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD3);
    emitRegDispRegOperands(dataReg, disp, (byte) 0x0);
    if (lister != null) lister.RDR(miStart, "ROL", dataReg, disp, shiftBy);
  }

  // rotate left of [indexReg<<scale + disp] by shiftBy
  public final void emitROL_RegOff_Reg(byte indexReg, short scale, Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD3);
    emitRegOffRegOperands(indexReg, scale, disp, (byte) 0x0);
    if (lister != null) lister.RFDR(miStart, "ROL", indexReg, scale, disp, shiftBy);
  }

  // rotate left of [disp] by shiftBy
  public final void emitROL_Abs_Reg(Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD3);
    emitAbsRegOperands(disp, (byte) 0x0);
    if (lister != null) lister.RAR(miStart, "ROL", disp, shiftBy);
  }

  // rotate left of [baseReg + indexReg<<scale + disp] by shiftBy
  public final void emitROL_RegIdx_Reg(byte baseReg, byte indexReg, short scale, Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD3);
    emitSIBRegOperands(baseReg, indexReg, scale, disp, (byte) 0x0);
    if (lister != null) lister.RXDR(miStart, "ROL", baseReg, indexReg, scale, disp, shiftBy);
  }

  // rotate right of reg by imm
  public final void emitROR_Reg_Imm_Byte(byte reg, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD0);
        emitRegRegOperands(reg, (byte) 0x1);
    } else {
        setMachineCodes(mi++, (byte) 0xC0);
        emitRegRegOperands(reg, (byte) 0x1);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RI(miStart, "ROR", reg, imm);
    }

  // rotate right of [reg] by imm
  public final void emitROR_RegInd_Imm_Byte(byte reg, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD0);
        emitRegIndirectRegOperands(reg, (byte) 0x1);
    } else {
        setMachineCodes(mi++, (byte) 0xC0);
        emitRegIndirectRegOperands(reg, (byte) 0x1);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RNI(miStart, "ROR", reg, imm);
    }

  // rotate right of [reg + disp] by imm
  public final void emitROR_RegDisp_Imm_Byte(byte reg, Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD0);
        emitRegDispRegOperands(reg, disp, (byte) 0x1);
    } else {
        setMachineCodes(mi++, (byte) 0xC0);
        emitRegDispRegOperands(reg, disp, (byte) 0x1);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RDI(miStart, "ROR", reg, disp, imm);
    }

  // rotate right of [index<<scale + disp] by imm
  public final void emitROR_RegOff_Imm_Byte(byte index, short scale, Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD0);
        emitRegOffRegOperands(index, scale, disp, (byte) 0x1);
    } else {
        setMachineCodes(mi++, (byte) 0xC0);
        emitRegOffRegOperands(index, scale, disp, (byte) 0x1);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RFDI(miStart, "ROR", index, scale, disp, imm);
    }

  // rotate right of [disp] by imm
  public final void emitROR_Abs_Imm_Byte(Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD0);
        emitAbsRegOperands(disp, (byte) 0x1);
    } else {
        setMachineCodes(mi++, (byte) 0xC0);
        emitAbsRegOperands(disp, (byte) 0x1);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RAI(miStart, "ROR", disp, imm);
  }

  // rotate right of [base + index<<scale + disp] by imm
  public final void emitROR_RegIdx_Imm_Byte(byte base, byte index, short scale, Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD0);
        emitSIBRegOperands(base, index, scale, disp, (byte) 0x1);
    } else {
        setMachineCodes(mi++, (byte) 0xC0);
        emitSIBRegOperands(base, index, scale, disp, (byte) 0x1);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RXDI(miStart, "ROR", base, index, scale, disp, imm);
  }

  // rotate right of dataReg by shiftBy
  public final void emitROR_Reg_Reg_Byte(byte dataReg, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD2);
    emitRegRegOperands(dataReg, (byte) 0x1);
    if (lister != null) lister.RR(miStart, "ROR", dataReg, shiftBy);
  }

  // rotate right of [dataReg] by shiftBy
  public final void emitROR_RegInd_Reg_Byte(byte dataReg, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD2);
    emitRegIndirectRegOperands(dataReg, (byte) 0x1);
    if (lister != null) lister.RNR(miStart, "ROR", dataReg, shiftBy);
  }

  // rotate right of [dataReg + disp] by shiftBy
  public final void emitROR_RegDisp_Reg_Byte(byte dataReg, Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD2);
    emitRegDispRegOperands(dataReg, disp, (byte) 0x1);
    if (lister != null) lister.RDR(miStart, "ROR", dataReg, disp, shiftBy);
  }

  // rotate right of [indexReg<<scale + disp] by shiftBy
  public final void emitROR_RegOff_Reg_Byte(byte indexReg, short scale, Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD2);
    emitRegOffRegOperands(indexReg, scale, disp, (byte) 0x1);
    if (lister != null) lister.RFDR(miStart, "ROR", indexReg, scale, disp, shiftBy);
  }

  // rotate right of [disp] by shiftBy
  public final void emitROR_Abs_Reg_Byte(Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD2);
    emitAbsRegOperands(disp, (byte) 0x1);
    if (lister != null) lister.RAR(miStart, "ROR", disp, shiftBy);
  }

  // rotate right of [baseReg + indexReg<<scale + disp] by shiftBy
  public final void emitROR_RegIdx_Reg_Byte(byte baseReg, byte indexReg, short scale, Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD2);
    emitSIBRegOperands(baseReg, indexReg, scale, disp, (byte) 0x1);
    if (lister != null) lister.RXDR(miStart, "ROR", baseReg, indexReg, scale, disp, shiftBy);
  }

  // rotate right of reg by imm
  public final void emitROR_Reg_Imm_Word(byte reg, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
        setMachineCodes(mi++, (byte) 0x66);
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitRegRegOperands(reg, (byte) 0x1);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitRegRegOperands(reg, (byte) 0x1);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RI(miStart, "ROR", reg, imm);
    }

  // rotate right of [reg] by imm
  public final void emitROR_RegInd_Imm_Word(byte reg, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
        setMachineCodes(mi++, (byte) 0x66);
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitRegIndirectRegOperands(reg, (byte) 0x1);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitRegIndirectRegOperands(reg, (byte) 0x1);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RNI(miStart, "ROR", reg, imm);
    }

  // rotate right of [reg + disp] by imm
  public final void emitROR_RegDisp_Imm_Word(byte reg, Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
        setMachineCodes(mi++, (byte) 0x66);
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitRegDispRegOperands(reg, disp, (byte) 0x1);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitRegDispRegOperands(reg, disp, (byte) 0x1);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RDI(miStart, "ROR", reg, disp, imm);
    }

  // rotate right of [index<<scale + disp] by imm
  public final void emitROR_RegOff_Imm_Word(byte index, short scale, Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
        setMachineCodes(mi++, (byte) 0x66);
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitRegOffRegOperands(index, scale, disp, (byte) 0x1);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitRegOffRegOperands(index, scale, disp, (byte) 0x1);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RFDI(miStart, "ROR", index, scale, disp, imm);
    }

  // rotate right of [disp] by imm
  public final void emitROR_Abs_Imm_Word(Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
        setMachineCodes(mi++, (byte) 0x66);
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitAbsRegOperands(disp, (byte) 0x1);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitAbsRegOperands(disp, (byte) 0x1);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RAI(miStart, "ROR", disp, imm);
  }

  // rotate right of [base + index<<scale + disp] by imm
  public final void emitROR_RegIdx_Imm_Word(byte base, byte index, short scale, Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
        setMachineCodes(mi++, (byte) 0x66);
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitSIBRegOperands(base, index, scale, disp, (byte) 0x1);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitSIBRegOperands(base, index, scale, disp, (byte) 0x1);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RXDI(miStart, "ROR", base, index, scale, disp, imm);
  }

  // rotate right of dataReg by shiftBy
  public final void emitROR_Reg_Reg_Word(byte dataReg, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0xD3);
    emitRegRegOperands(dataReg, (byte) 0x1);
    if (lister != null) lister.RR(miStart, "ROR", dataReg, shiftBy);
  }

  // rotate right of [dataReg] by shiftBy
  public final void emitROR_RegInd_Reg_Word(byte dataReg, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0xD3);
    emitRegIndirectRegOperands(dataReg, (byte) 0x1);
    if (lister != null) lister.RNR(miStart, "ROR", dataReg, shiftBy);
  }

  // rotate right of [dataReg + disp] by shiftBy
  public final void emitROR_RegDisp_Reg_Word(byte dataReg, Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0xD3);
    emitRegDispRegOperands(dataReg, disp, (byte) 0x1);
    if (lister != null) lister.RDR(miStart, "ROR", dataReg, disp, shiftBy);
  }

  // rotate right of [indexReg<<scale + disp] by shiftBy
  public final void emitROR_RegOff_Reg_Word(byte indexReg, short scale, Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0xD3);
    emitRegOffRegOperands(indexReg, scale, disp, (byte) 0x1);
    if (lister != null) lister.RFDR(miStart, "ROR", indexReg, scale, disp, shiftBy);
  }

  // rotate right of [disp] by shiftBy
  public final void emitROR_Abs_Reg_Word(Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0xD3);
    emitAbsRegOperands(disp, (byte) 0x1);
    if (lister != null) lister.RAR(miStart, "ROR", disp, shiftBy);
  }

  // rotate right of [baseReg + indexReg<<scale + disp] by shiftBy
  public final void emitROR_RegIdx_Reg_Word(byte baseReg, byte indexReg, short scale, Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
        setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0xD3);
    emitSIBRegOperands(baseReg, indexReg, scale, disp, (byte) 0x1);
    if (lister != null) lister.RXDR(miStart, "ROR", baseReg, indexReg, scale, disp, shiftBy);
  }

  // rotate right of reg by imm
  public final void emitROR_Reg_Imm(byte reg, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitRegRegOperands(reg, (byte) 0x1);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitRegRegOperands(reg, (byte) 0x1);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RI(miStart, "ROR", reg, imm);
    }

  // rotate right of [reg] by imm
  public final void emitROR_RegInd_Imm(byte reg, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitRegIndirectRegOperands(reg, (byte) 0x1);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitRegIndirectRegOperands(reg, (byte) 0x1);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RNI(miStart, "ROR", reg, imm);
    }

  // rotate right of [reg + disp] by imm
  public final void emitROR_RegDisp_Imm(byte reg, Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitRegDispRegOperands(reg, disp, (byte) 0x1);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitRegDispRegOperands(reg, disp, (byte) 0x1);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RDI(miStart, "ROR", reg, disp, imm);
    }

  // rotate right of [index<<scale + disp] by imm
  public final void emitROR_RegOff_Imm(byte index, short scale, Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitRegOffRegOperands(index, scale, disp, (byte) 0x1);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitRegOffRegOperands(index, scale, disp, (byte) 0x1);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RFDI(miStart, "ROR", index, scale, disp, imm);
    }

  // rotate right of [disp] by imm
  public final void emitROR_Abs_Imm(Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitAbsRegOperands(disp, (byte) 0x1);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitAbsRegOperands(disp, (byte) 0x1);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RAI(miStart, "ROR", disp, imm);
  }

  // rotate right of [base + index<<scale + disp] by imm
  public final void emitROR_RegIdx_Imm(byte base, byte index, short scale, Offset disp, int imm) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(fits(imm,8)); 
    if (imm == 1) {
        setMachineCodes(mi++, (byte) 0xD1);
        emitSIBRegOperands(base, index, scale, disp, (byte) 0x1);
    } else {
        setMachineCodes(mi++, (byte) 0xC1);
        emitSIBRegOperands(base, index, scale, disp, (byte) 0x1);
        emitImm8((byte)imm);
    }
    if (lister != null) lister.RXDI(miStart, "ROR", base, index, scale, disp, imm);
  }

  // rotate right of dataReg by shiftBy
  public final void emitROR_Reg_Reg(byte dataReg, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD3);
    emitRegRegOperands(dataReg, (byte) 0x1);
    if (lister != null) lister.RR(miStart, "ROR", dataReg, shiftBy);
  }

  // rotate right of [dataReg] by shiftBy
  public final void emitROR_RegInd_Reg(byte dataReg, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD3);
    emitRegIndirectRegOperands(dataReg, (byte) 0x1);
    if (lister != null) lister.RNR(miStart, "ROR", dataReg, shiftBy);
  }

  // rotate right of [dataReg + disp] by shiftBy
  public final void emitROR_RegDisp_Reg(byte dataReg, Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD3);
    emitRegDispRegOperands(dataReg, disp, (byte) 0x1);
    if (lister != null) lister.RDR(miStart, "ROR", dataReg, disp, shiftBy);
  }

  // rotate right of [indexReg<<scale + disp] by shiftBy
  public final void emitROR_RegOff_Reg(byte indexReg, short scale, Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD3);
    emitRegOffRegOperands(indexReg, scale, disp, (byte) 0x1);
    if (lister != null) lister.RFDR(miStart, "ROR", indexReg, scale, disp, shiftBy);
  }

  // rotate right of [disp] by shiftBy
  public final void emitROR_Abs_Reg(Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD3);
    emitAbsRegOperands(disp, (byte) 0x1);
    if (lister != null) lister.RAR(miStart, "ROR", disp, shiftBy);
  }

  // rotate right of [baseReg + indexReg<<scale + disp] by shiftBy
  public final void emitROR_RegIdx_Reg(byte baseReg, byte indexReg, short scale, Offset disp, byte shiftBy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX); 
    setMachineCodes(mi++, (byte) 0xD3);
    emitSIBRegOperands(baseReg, indexReg, scale, disp, (byte) 0x1);
    if (lister != null) lister.RXDR(miStart, "ROR", baseReg, indexReg, scale, disp, shiftBy);
  }

  // left <<= shiftBy (with bits from right shifted in)
  public final void emitSHLD_Reg_Reg_Imm(byte left, byte right, int shiftBy) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xA4);
    emitRegRegOperands(left, right);
    emitImm8((byte)shiftBy);
    if (lister != null) lister.RRI(miStart, "SHLD", left, right, shiftBy);
  }

  // [left] <<= shiftBy (with bits from right shifted in)
  public final void emitSHLD_RegInd_Reg_Imm(byte left, byte right, int shiftBy) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xA4);
    emitRegIndirectRegOperands(left, right);
    emitImm8((byte)shiftBy);
    if (lister != null) lister.RNRI(miStart, "SHLD", left, right, shiftBy);
  }

  // [left + disp] <<= shiftBy (with bits from right shifted in)
  public final void emitSHLD_RegDisp_Reg_Imm(byte left, Offset disp, byte right, int shiftBy) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xA4);
    emitRegDispRegOperands(left, disp, right);
    emitImm8((byte)shiftBy);
    if (lister != null) lister.RDRI(miStart, "SHLD", left, disp, right, shiftBy);
  }

  // [leftBase + leftIndex<<scale + disp] <<= shiftBy (with bits from right shifted in)
  public final void emitSHLD_RegIdx_Reg_Imm(byte leftBase, byte leftIndex, short scale, Offset disp, byte right, int shiftBy) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xA4);
    emitSIBRegOperands(leftBase, leftIndex, scale, disp, right);
    emitImm8((byte)shiftBy);
    if (lister != null) lister.RXDRI(miStart, "SHLD", leftBase, leftIndex, scale, disp, right, shiftBy);
  }

  // [leftIndex<<scale + disp] <<= shiftBy (with bits from right shifted in)
  public final void emitSHLD_RegOff_Reg_Imm(byte leftIndex, short scale, Offset disp, byte right, int shiftBy) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xA4);
    emitRegOffRegOperands(leftIndex, scale, disp, right);
    emitImm8((byte)shiftBy);
    if (lister != null) lister.RFDRI(miStart, "SHLD", leftIndex, scale, disp, right, shiftBy);
  }

  // [disp] <<= shiftBy (with bits from right shifted in)
  public final void emitSHLD_Abs_Reg_Imm(Offset disp, byte right, int shiftBy) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xA4);
    emitAbsRegOperands(disp, right);
    emitImm8((byte)shiftBy);
    if (lister != null) lister.RARI(miStart, "SHLD", disp, right, shiftBy);
  }

  // left <<= shiftBy (with bits from right shifted in)
  public final void emitSHLD_Reg_Reg_Reg(byte left, byte right, byte shiftBy) {
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX);
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xA5);
    emitRegRegOperands(left, right);
    if (lister != null) lister.RRR(miStart, "SHLD", left, right, shiftBy);
  }

  // [left] <<= shiftBy (with bits from right shifted in)
  public final void emitSHLD_RegInd_Reg_Reg(byte left, byte right, byte shiftBy) {
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX);
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xA5);
    emitRegIndirectRegOperands(left, right);
    if (lister != null) lister.RNRI(miStart, "SHLD", left, right, shiftBy);
  }

  // [left + disp] <<= shiftBy (with bits from right shifted in)
  public final void emitSHLD_RegDisp_Reg_Reg(byte left, Offset disp, byte right, byte shiftBy) {
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX);
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xA5);
    emitRegDispRegOperands(left, disp, right);
    if (lister != null) lister.RDRI(miStart, "SHLD", left, disp, right, shiftBy);
  }

  // [leftBase + leftIndex<<scale + disp] <<= shiftBy (with bits from right shifted in)
  public final void emitSHLD_RegIdx_Reg_Reg(byte leftBase, byte leftIndex, short scale, Offset disp, byte right, byte shiftBy) {
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX);
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xA5);
    emitSIBRegOperands(leftBase, leftIndex, scale, disp, right);
    if (lister != null) lister.RXDRR(miStart, "SHLD", leftBase, leftIndex, scale, disp, right, shiftBy);
  }

  // [leftIndex<<scale + disp] <<= shiftBy (with bits from right shifted in)
  public final void emitSHLD_RegOff_Reg_Reg(byte leftIndex, short scale, Offset disp, byte right, byte shiftBy) {
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX);
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xA5);
    emitRegOffRegOperands(leftIndex, scale, disp, right);
    if (lister != null) lister.RFDRR(miStart, "SHLD", leftIndex, scale, disp, right, shiftBy);
  }

  // [disp] <<= shiftBy (with bits from right shifted in)
  public final void emitSHLD_Abs_Reg_Reg(Offset disp, byte right, byte shiftBy) {
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX);
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xA5);
    emitAbsRegOperands(disp, right);
    if (lister != null) lister.RARR(miStart, "SHLD", disp, right, shiftBy);
  }

  // left <<= shiftBy (with bits from right shifted in)
  public final void emitSHRD_Reg_Reg_Imm(byte left, byte right, int shiftBy) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xAC);
    emitRegRegOperands(left, right);
    emitImm8((byte)shiftBy);
    if (lister != null) lister.RRI(miStart, "SHRD", left, right, shiftBy);
  }

  // [left] <<= shiftBy (with bits from right shifted in)
  public final void emitSHRD_RegInd_Reg_Imm(byte left, byte right, int shiftBy) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xAC);
    emitRegIndirectRegOperands(left, right);
    emitImm8((byte)shiftBy);
    if (lister != null) lister.RNRI(miStart, "SHRD", left, right, shiftBy);
  }

  // [left + disp] <<= shiftBy (with bits from right shifted in)
  public final void emitSHRD_RegDisp_Reg_Imm(byte left, Offset disp, byte right, int shiftBy) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xAC);
    emitRegDispRegOperands(left, disp, right);
    emitImm8((byte)shiftBy);
    if (lister != null) lister.RDRI(miStart, "SHRD", left, disp, right, shiftBy);
  }

  // [leftBase + leftIndex<<scale + disp] <<= shiftBy (with bits from right shifted in)
  public final void emitSHRD_RegIdx_Reg_Imm(byte leftBase, byte leftIndex, short scale, Offset disp, byte right, int shiftBy) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xAC);
    emitSIBRegOperands(leftBase, leftIndex, scale, disp, right);
    emitImm8((byte)shiftBy);
    if (lister != null) lister.RXDRI(miStart, "SHRD", leftBase, leftIndex, scale, disp, right, shiftBy);
  }

  // [leftIndex<<scale + disp] <<= shiftBy (with bits from right shifted in)
  public final void emitSHRD_RegOff_Reg_Imm(byte leftIndex, short scale, Offset disp, byte right, int shiftBy) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xAC);
    emitRegOffRegOperands(leftIndex, scale, disp, right);
    emitImm8((byte)shiftBy);
    if (lister != null) lister.RFDRI(miStart, "SHRD", leftIndex, scale, disp, right, shiftBy);
  }

  // [disp] <<= shiftBy (with bits from right shifted in)
  public final void emitSHRD_Abs_Reg_Imm(Offset disp, byte right, int shiftBy) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xAC);
    emitAbsRegOperands(disp, right);
    emitImm8((byte)shiftBy);
    if (lister != null) lister.RARI(miStart, "SHRD", disp, right, shiftBy);
  }

  // left <<= shiftBy (with bits from right shifted in)
  public final void emitSHRD_Reg_Reg_Reg(byte left, byte right, byte shiftBy) {
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX);
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xAD);
    emitRegRegOperands(left, right);
    if (lister != null) lister.RRR(miStart, "SHRD", left, right, shiftBy);
  }

  // [left] <<= shiftBy (with bits from right shifted in)
  public final void emitSHRD_RegInd_Reg_Reg(byte left, byte right, byte shiftBy) {
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX);
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xAD);
    emitRegIndirectRegOperands(left, right);
    if (lister != null) lister.RNRI(miStart, "SHRD", left, right, shiftBy);
  }

  // [left + disp] <<= shiftBy (with bits from right shifted in)
  public final void emitSHRD_RegDisp_Reg_Reg(byte left, Offset disp, byte right, byte shiftBy) {
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX);
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xAD);
    emitRegDispRegOperands(left, disp, right);
    if (lister != null) lister.RDRI(miStart, "SHRD", left, disp, right, shiftBy);
  }

  // [leftBase + leftIndex<<scale + disp] <<= shiftBy (with bits from right shifted in)
  public final void emitSHRD_RegIdx_Reg_Reg(byte leftBase, byte leftIndex, short scale, Offset disp, byte right, byte shiftBy) {
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX);
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xAD);
    emitSIBRegOperands(leftBase, leftIndex, scale, disp, right);
    if (lister != null) lister.RXDRR(miStart, "SHRD", leftBase, leftIndex, scale, disp, right, shiftBy);
  }

  // [leftIndex<<scale + disp] <<= shiftBy (with bits from right shifted in)
  public final void emitSHRD_RegOff_Reg_Reg(byte leftIndex, short scale, Offset disp, byte right, byte shiftBy) {
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX);
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xAD);
    emitRegOffRegOperands(leftIndex, scale, disp, right);
    if (lister != null) lister.RFDRR(miStart, "SHRD", leftIndex, scale, disp, right, shiftBy);
  }

  // [disp] <<= shiftBy (with bits from right shifted in)
  public final void emitSHRD_Abs_Reg_Reg(Offset disp, byte right, byte shiftBy) {
    if (VM.VerifyAssertions) VM._assert(shiftBy == ECX);
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xAD);
    emitAbsRegOperands(disp, right);
    if (lister != null) lister.RARR(miStart, "SHRD", disp, right, shiftBy);
  }

  // pop dstReg, SP -= 4
  public final void emitPOP_Reg (byte dstReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) (0x58 + dstReg));
    if (lister != null) lister.R(miStart, "POP", dstReg);
  }

  // pop [dstReg + dstDisp], SP -= 4
  public final void emitPOP_RegDisp (byte dstReg, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x8F);
    emitRegDispRegOperands(dstReg, disp, (byte) 0x0);
    if (lister != null) lister.RD(miStart, "POP", dstReg, disp);
  }

  // pop [dstReg], SP -= 4
  public final void emitPOP_RegInd (byte dstReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x8F);
    emitRegIndirectRegOperands(dstReg, (byte) 0x0);
    if (lister != null) lister.RN(miStart, "POP", dstReg);
  }

  // pop [dstBase + dstNdx<<scale + dstDisp], SP -= 4
  public final void emitPOP_RegIdx (byte base, byte index, short scale, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x8F);
    emitSIBRegOperands(base, index, scale, disp, (byte) 0x0);
    if (lister != null) lister.RXD(miStart, "POP", base, index, scale, disp);
  }

  // pop [dstNdx<<scale + dstDisp], SP -= 4
  public final void emitPOP_RegOff (byte index, short scale, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x8F);
    emitRegOffRegOperands(index, scale, disp, (byte) 0x0);
    if (lister != null) lister.RFD(miStart, "POP", index, scale, disp);
  }

  // pop [dstDisp], SP -= 4
  public final void emitPOP_Abs (Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x8F);
    emitAbsRegOperands(disp, (byte) 0x0);
    if (lister != null) lister.RA(miStart, "POP", disp);
  }

  // push dstReg, SP += 4
  public final void emitPUSH_Reg (byte dstReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) (0x50 + dstReg));
    if (lister != null) lister.R(miStart, "PUSH", dstReg);
  }

  // push [dstReg + dstDisp], SP += 4
  public final void emitPUSH_RegDisp (byte dstReg, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xFF);
    emitRegDispRegOperands(dstReg, disp, (byte) 0x6);
    if (lister != null) lister.RD(miStart, "PUSH", dstReg, disp);
  }

  // push [dstReg], SP += 4
  public final void emitPUSH_RegInd (byte dstReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xFF);
    emitRegIndirectRegOperands(dstReg, (byte) 0x6);
    if (lister != null) lister.RN(miStart, "PUSH", dstReg);
  }

  // push [dstBase + dstNdx<<scale + dstDisp], SP += 4
  public final void emitPUSH_RegIdx (byte base, byte index, short scale, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xFF);
    emitSIBRegOperands(base, index, scale, disp, (byte) 0x6);
    if (lister != null) lister.RXD(miStart, "PUSH", base, index, scale, disp);
  }

  // push [dstNdx<<scale + dstDisp], SP += 4
  public final void emitPUSH_RegOff (byte index, short scale, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xFF);
    emitRegOffRegOperands(index, scale, disp, (byte) 0x6);
    if (lister != null) lister.RFD(miStart, "PUSH", index, scale, disp);
  }

  // push [dstDisp], SP += 4
  public final void emitPUSH_Abs (Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xFF);
    emitAbsRegOperands(disp, (byte) 0x6);
    if (lister != null) lister.RA(miStart, "PUSH", disp);
  }

  // push imm, SP += 4
  public final void emitPUSH_Imm(int imm) {
    int miStart = mi;
    if (fits(imm, 8)) {
      setMachineCodes(mi++, (byte) 0x6A);
      emitImm8(imm);
    } else {
      setMachineCodes(mi++, (byte) 0x68);
      emitImm32(imm);
    }
    if (lister != null) lister.I(miStart, "PUSH", imm);
  }

  
  // dstReg <<=  srcReg
  public final void emitADDSS_Reg_Reg(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x58);
    emitRegRegOperands(srcReg, dstReg);
    if (lister != null) lister.RR(miStart, "ADDSS", dstReg, srcReg);
  }

  // dstReg <<=  [srcReg + srcDisp]
  public final void emitADDSS_Reg_RegDisp(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x58);
    emitRegDispRegOperands(srcReg, disp, dstReg);
    if (lister != null) lister.RRD(miStart, "ADDSS", dstReg, srcReg, disp);
  }

  // dstReg <<=  [srcIndex<<scale + srcDisp]
  public final void emitADDSS_Reg_RegOff(byte dstReg, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x58);
    emitRegOffRegOperands(srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRFD(miStart, "ADDSS", dstReg, srcIndex, scale, srcDisp);
  }

  // dstReg <<=  [srcDisp]
  public final void emitADDSS_Reg_Abs(byte dstReg, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x58);
    emitAbsRegOperands(srcDisp, dstReg);
    if (lister != null) lister.RRA(miStart, "ADDSS", dstReg, srcDisp);
  }

  // dstReg <<=  [srcBase + srcIndex<<scale + srcDisp]
  public final void emitADDSS_Reg_RegIdx(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x58);
    emitSIBRegOperands(srcBase, srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRXD(miStart, "ADDSS", dstReg, srcBase, srcIndex, scale, srcDisp);
  }

  // dstReg <<=  [srcReg]
  public final void emitADDSS_Reg_RegInd(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x58);
    emitRegIndirectRegOperands(srcReg, dstReg);
    if (lister != null) lister.RRN(miStart, "ADDSS", dstReg, srcReg);
  }
  
  
  // dstReg <<=  srcReg
  public final void emitSUBSS_Reg_Reg(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x5C);
    emitRegRegOperands(srcReg, dstReg);
    if (lister != null) lister.RR(miStart, "SUBSS", dstReg, srcReg);
  }

  // dstReg <<=  [srcReg + srcDisp]
  public final void emitSUBSS_Reg_RegDisp(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x5C);
    emitRegDispRegOperands(srcReg, disp, dstReg);
    if (lister != null) lister.RRD(miStart, "SUBSS", dstReg, srcReg, disp);
  }

  // dstReg <<=  [srcIndex<<scale + srcDisp]
  public final void emitSUBSS_Reg_RegOff(byte dstReg, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x5C);
    emitRegOffRegOperands(srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRFD(miStart, "SUBSS", dstReg, srcIndex, scale, srcDisp);
  }

  // dstReg <<=  [srcDisp]
  public final void emitSUBSS_Reg_Abs(byte dstReg, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x5C);
    emitAbsRegOperands(srcDisp, dstReg);
    if (lister != null) lister.RRA(miStart, "SUBSS", dstReg, srcDisp);
  }

  // dstReg <<=  [srcBase + srcIndex<<scale + srcDisp]
  public final void emitSUBSS_Reg_RegIdx(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x5C);
    emitSIBRegOperands(srcBase, srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRXD(miStart, "SUBSS", dstReg, srcBase, srcIndex, scale, srcDisp);
  }

  // dstReg <<=  [srcReg]
  public final void emitSUBSS_Reg_RegInd(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x5C);
    emitRegIndirectRegOperands(srcReg, dstReg);
    if (lister != null) lister.RRN(miStart, "SUBSS", dstReg, srcReg);
  }
  
  
  // dstReg <<=  srcReg
  public final void emitMULSS_Reg_Reg(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x59);
    emitRegRegOperands(srcReg, dstReg);
    if (lister != null) lister.RR(miStart, "MULSS", dstReg, srcReg);
  }

  // dstReg <<=  [srcReg + srcDisp]
  public final void emitMULSS_Reg_RegDisp(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x59);
    emitRegDispRegOperands(srcReg, disp, dstReg);
    if (lister != null) lister.RRD(miStart, "MULSS", dstReg, srcReg, disp);
  }

  // dstReg <<=  [srcIndex<<scale + srcDisp]
  public final void emitMULSS_Reg_RegOff(byte dstReg, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x59);
    emitRegOffRegOperands(srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRFD(miStart, "MULSS", dstReg, srcIndex, scale, srcDisp);
  }

  // dstReg <<=  [srcDisp]
  public final void emitMULSS_Reg_Abs(byte dstReg, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x59);
    emitAbsRegOperands(srcDisp, dstReg);
    if (lister != null) lister.RRA(miStart, "MULSS", dstReg, srcDisp);
  }

  // dstReg <<=  [srcBase + srcIndex<<scale + srcDisp]
  public final void emitMULSS_Reg_RegIdx(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x59);
    emitSIBRegOperands(srcBase, srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRXD(miStart, "MULSS", dstReg, srcBase, srcIndex, scale, srcDisp);
  }

  // dstReg <<=  [srcReg]
  public final void emitMULSS_Reg_RegInd(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x59);
    emitRegIndirectRegOperands(srcReg, dstReg);
    if (lister != null) lister.RRN(miStart, "MULSS", dstReg, srcReg);
  }
  
  
  // dstReg <<=  srcReg
  public final void emitDIVSS_Reg_Reg(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x5E);
    emitRegRegOperands(srcReg, dstReg);
    if (lister != null) lister.RR(miStart, "DIVSS", dstReg, srcReg);
  }

  // dstReg <<=  [srcReg + srcDisp]
  public final void emitDIVSS_Reg_RegDisp(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x5E);
    emitRegDispRegOperands(srcReg, disp, dstReg);
    if (lister != null) lister.RRD(miStart, "DIVSS", dstReg, srcReg, disp);
  }

  // dstReg <<=  [srcIndex<<scale + srcDisp]
  public final void emitDIVSS_Reg_RegOff(byte dstReg, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x5E);
    emitRegOffRegOperands(srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRFD(miStart, "DIVSS", dstReg, srcIndex, scale, srcDisp);
  }

  // dstReg <<=  [srcDisp]
  public final void emitDIVSS_Reg_Abs(byte dstReg, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x5E);
    emitAbsRegOperands(srcDisp, dstReg);
    if (lister != null) lister.RRA(miStart, "DIVSS", dstReg, srcDisp);
  }

  // dstReg <<=  [srcBase + srcIndex<<scale + srcDisp]
  public final void emitDIVSS_Reg_RegIdx(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x5E);
    emitSIBRegOperands(srcBase, srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRXD(miStart, "DIVSS", dstReg, srcBase, srcIndex, scale, srcDisp);
  }

  // dstReg <<=  [srcReg]
  public final void emitDIVSS_Reg_RegInd(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x5E);
    emitRegIndirectRegOperands(srcReg, dstReg);
    if (lister != null) lister.RRN(miStart, "DIVSS", dstReg, srcReg);
  }
  
  
  // dstReg <<=  srcReg
  public final void emitMOVSS_Reg_Reg(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x10);
    emitRegRegOperands(srcReg, dstReg);
    if (lister != null) lister.RR(miStart, "MOVSS", dstReg, srcReg);
  }

  // dstReg <<=  [srcReg + srcDisp]
  public final void emitMOVSS_Reg_RegDisp(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x10);
    emitRegDispRegOperands(srcReg, disp, dstReg);
    if (lister != null) lister.RRD(miStart, "MOVSS", dstReg, srcReg, disp);
  }

  // dstReg <<=  [srcIndex<<scale + srcDisp]
  public final void emitMOVSS_Reg_RegOff(byte dstReg, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x10);
    emitRegOffRegOperands(srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRFD(miStart, "MOVSS", dstReg, srcIndex, scale, srcDisp);
  }

  // dstReg <<=  [srcDisp]
  public final void emitMOVSS_Reg_Abs(byte dstReg, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x10);
    emitAbsRegOperands(srcDisp, dstReg);
    if (lister != null) lister.RRA(miStart, "MOVSS", dstReg, srcDisp);
  }

  // dstReg <<=  [srcBase + srcIndex<<scale + srcDisp]
  public final void emitMOVSS_Reg_RegIdx(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x10);
    emitSIBRegOperands(srcBase, srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRXD(miStart, "MOVSS", dstReg, srcBase, srcIndex, scale, srcDisp);
  }

  // dstReg <<=  [srcReg]
  public final void emitMOVSS_Reg_RegInd(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x10);
    emitRegIndirectRegOperands(srcReg, dstReg);
    if (lister != null) lister.RRN(miStart, "MOVSS", dstReg, srcReg);
  }
  

  /**
   * Generate a register--register MOVSS. That is,
   * <PRE>
   * [dstReg] <<=  srcReg
   * </PRE>
   *
   * @param dstReg the destination register
   * @param srcReg the source register
   */
  public final void emitMOVSS_RegInd_Reg(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x11);
    emitRegIndirectRegOperands(dstReg, srcReg);
    if (lister != null) lister.RNR(miStart, "MOVSS", dstReg, srcReg);
  }

  /**
   * Generate a register-offset--register MOVSS. That is,
   * <PRE>
   * [dstReg<<dstScale + dstDisp] <<=  srcReg
   * </PRE>
   *
   * @param dstIndex the destination index register
   * @param dstScale the destination shift amount
   * @param dstDisp the destination displacement
   * @param srcReg the source register
   */
  public final void emitMOVSS_RegOff_Reg(byte dstIndex, short dstScale, Offset dstDisp, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x11);
    emitRegOffRegOperands(dstIndex, dstScale, dstDisp, srcReg);
    if (lister != null) lister.RFDR(miStart, "MOVSS", dstIndex, dstScale, dstDisp, srcReg);
  }

  // [dstDisp] <<=  srcReg
  public final void emitMOVSS_Abs_Reg(Offset dstDisp, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x11);
    emitAbsRegOperands(dstDisp, srcReg);
    if (lister != null) lister.RAR(miStart, "MOVSS", dstDisp, srcReg);
  }

  // [dstBase + dstIndex<<scale + dstDisp] <<=  srcReg
  public final void emitMOVSS_RegIdx_Reg(byte dstBase, byte dstIndex, short scale, Offset dstDisp, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x11);
    emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, srcReg);
    if (lister != null) lister.RXDR(miStart, "MOVSS", dstBase, dstIndex, scale, dstDisp, srcReg);
  }

  // [dstReg + dstDisp] <<=  srcReg
  public final void emitMOVSS_RegDisp_Reg(byte dstReg, Offset disp, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x11);
    emitRegDispRegOperands(dstReg, disp, srcReg);
    if (lister != null) lister.RDR(miStart, "MOVSS", dstReg, disp, srcReg);
  }

  
  // dstReg <<=  srcReg
  public final void emitCVTSI2SS_Reg_Reg(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x2A);
    emitRegRegOperands(srcReg, dstReg);
    if (lister != null) lister.RR(miStart, "CVTSI2SS", dstReg, srcReg);
  }

  // dstReg <<=  [srcReg + srcDisp]
  public final void emitCVTSI2SS_Reg_RegDisp(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x2A);
    emitRegDispRegOperands(srcReg, disp, dstReg);
    if (lister != null) lister.RRD(miStart, "CVTSI2SS", dstReg, srcReg, disp);
  }

  // dstReg <<=  [srcIndex<<scale + srcDisp]
  public final void emitCVTSI2SS_Reg_RegOff(byte dstReg, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x2A);
    emitRegOffRegOperands(srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRFD(miStart, "CVTSI2SS", dstReg, srcIndex, scale, srcDisp);
  }

  // dstReg <<=  [srcDisp]
  public final void emitCVTSI2SS_Reg_Abs(byte dstReg, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x2A);
    emitAbsRegOperands(srcDisp, dstReg);
    if (lister != null) lister.RRA(miStart, "CVTSI2SS", dstReg, srcDisp);
  }

  // dstReg <<=  [srcBase + srcIndex<<scale + srcDisp]
  public final void emitCVTSI2SS_Reg_RegIdx(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x2A);
    emitSIBRegOperands(srcBase, srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRXD(miStart, "CVTSI2SS", dstReg, srcBase, srcIndex, scale, srcDisp);
  }

  // dstReg <<=  [srcReg]
  public final void emitCVTSI2SS_Reg_RegInd(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x2A);
    emitRegIndirectRegOperands(srcReg, dstReg);
    if (lister != null) lister.RRN(miStart, "CVTSI2SS", dstReg, srcReg);
  }
  
  
  // dstReg <<=  srcReg
  public final void emitCVTSS2SD_Reg_Reg(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x5A);
    emitRegRegOperands(srcReg, dstReg);
    if (lister != null) lister.RR(miStart, "CVTSS2SD", dstReg, srcReg);
  }

  // dstReg <<=  [srcReg + srcDisp]
  public final void emitCVTSS2SD_Reg_RegDisp(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x5A);
    emitRegDispRegOperands(srcReg, disp, dstReg);
    if (lister != null) lister.RRD(miStart, "CVTSS2SD", dstReg, srcReg, disp);
  }

  // dstReg <<=  [srcIndex<<scale + srcDisp]
  public final void emitCVTSS2SD_Reg_RegOff(byte dstReg, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x5A);
    emitRegOffRegOperands(srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRFD(miStart, "CVTSS2SD", dstReg, srcIndex, scale, srcDisp);
  }

  // dstReg <<=  [srcDisp]
  public final void emitCVTSS2SD_Reg_Abs(byte dstReg, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x5A);
    emitAbsRegOperands(srcDisp, dstReg);
    if (lister != null) lister.RRA(miStart, "CVTSS2SD", dstReg, srcDisp);
  }

  // dstReg <<=  [srcBase + srcIndex<<scale + srcDisp]
  public final void emitCVTSS2SD_Reg_RegIdx(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x5A);
    emitSIBRegOperands(srcBase, srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRXD(miStart, "CVTSS2SD", dstReg, srcBase, srcIndex, scale, srcDisp);
  }

  // dstReg <<=  [srcReg]
  public final void emitCVTSS2SD_Reg_RegInd(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x5A);
    emitRegIndirectRegOperands(srcReg, dstReg);
    if (lister != null) lister.RRN(miStart, "CVTSS2SD", dstReg, srcReg);
  }
  
  
  // dstReg <<=  srcReg
  public final void emitCVTSS2SI_Reg_Reg(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x2D);
    emitRegRegOperands(srcReg, dstReg);
    if (lister != null) lister.RR(miStart, "CVTSS2SI", dstReg, srcReg);
  }

  // dstReg <<=  [srcReg + srcDisp]
  public final void emitCVTSS2SI_Reg_RegDisp(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x2D);
    emitRegDispRegOperands(srcReg, disp, dstReg);
    if (lister != null) lister.RRD(miStart, "CVTSS2SI", dstReg, srcReg, disp);
  }

  // dstReg <<=  [srcIndex<<scale + srcDisp]
  public final void emitCVTSS2SI_Reg_RegOff(byte dstReg, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x2D);
    emitRegOffRegOperands(srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRFD(miStart, "CVTSS2SI", dstReg, srcIndex, scale, srcDisp);
  }

  // dstReg <<=  [srcDisp]
  public final void emitCVTSS2SI_Reg_Abs(byte dstReg, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x2D);
    emitAbsRegOperands(srcDisp, dstReg);
    if (lister != null) lister.RRA(miStart, "CVTSS2SI", dstReg, srcDisp);
  }

  // dstReg <<=  [srcBase + srcIndex<<scale + srcDisp]
  public final void emitCVTSS2SI_Reg_RegIdx(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x2D);
    emitSIBRegOperands(srcBase, srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRXD(miStart, "CVTSS2SI", dstReg, srcBase, srcIndex, scale, srcDisp);
  }

  // dstReg <<=  [srcReg]
  public final void emitCVTSS2SI_Reg_RegInd(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x2D);
    emitRegIndirectRegOperands(srcReg, dstReg);
    if (lister != null) lister.RRN(miStart, "CVTSS2SI", dstReg, srcReg);
  }
  
  
  // dstReg <<=  srcReg
  public final void emitCVTTSS2SI_Reg_Reg(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x2C);
    emitRegRegOperands(srcReg, dstReg);
    if (lister != null) lister.RR(miStart, "CVTTSS2SI", dstReg, srcReg);
  }

  // dstReg <<=  [srcReg + srcDisp]
  public final void emitCVTTSS2SI_Reg_RegDisp(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x2C);
    emitRegDispRegOperands(srcReg, disp, dstReg);
    if (lister != null) lister.RRD(miStart, "CVTTSS2SI", dstReg, srcReg, disp);
  }

  // dstReg <<=  [srcIndex<<scale + srcDisp]
  public final void emitCVTTSS2SI_Reg_RegOff(byte dstReg, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x2C);
    emitRegOffRegOperands(srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRFD(miStart, "CVTTSS2SI", dstReg, srcIndex, scale, srcDisp);
  }

  // dstReg <<=  [srcDisp]
  public final void emitCVTTSS2SI_Reg_Abs(byte dstReg, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x2C);
    emitAbsRegOperands(srcDisp, dstReg);
    if (lister != null) lister.RRA(miStart, "CVTTSS2SI", dstReg, srcDisp);
  }

  // dstReg <<=  [srcBase + srcIndex<<scale + srcDisp]
  public final void emitCVTTSS2SI_Reg_RegIdx(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x2C);
    emitSIBRegOperands(srcBase, srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRXD(miStart, "CVTTSS2SI", dstReg, srcBase, srcIndex, scale, srcDisp);
  }

  // dstReg <<=  [srcReg]
  public final void emitCVTTSS2SI_Reg_RegInd(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x2C);
    emitRegIndirectRegOperands(srcReg, dstReg);
    if (lister != null) lister.RRN(miStart, "CVTTSS2SI", dstReg, srcReg);
  }
  
  
  // dstReg <<=  srcReg
  public final void emitUCOMISS_Reg_Reg(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x2E);
    emitRegRegOperands(srcReg, dstReg);
    if (lister != null) lister.RR(miStart, "UCOMISS", dstReg, srcReg);
  }

  // dstReg <<=  [srcReg + srcDisp]
  public final void emitUCOMISS_Reg_RegDisp(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x2E);
    emitRegDispRegOperands(srcReg, disp, dstReg);
    if (lister != null) lister.RRD(miStart, "UCOMISS", dstReg, srcReg, disp);
  }

  // dstReg <<=  [srcIndex<<scale + srcDisp]
  public final void emitUCOMISS_Reg_RegOff(byte dstReg, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x2E);
    emitRegOffRegOperands(srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRFD(miStart, "UCOMISS", dstReg, srcIndex, scale, srcDisp);
  }

  // dstReg <<=  [srcDisp]
  public final void emitUCOMISS_Reg_Abs(byte dstReg, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x2E);
    emitAbsRegOperands(srcDisp, dstReg);
    if (lister != null) lister.RRA(miStart, "UCOMISS", dstReg, srcDisp);
  }

  // dstReg <<=  [srcBase + srcIndex<<scale + srcDisp]
  public final void emitUCOMISS_Reg_RegIdx(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x2E);
    emitSIBRegOperands(srcBase, srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRXD(miStart, "UCOMISS", dstReg, srcBase, srcIndex, scale, srcDisp);
  }

  // dstReg <<=  [srcReg]
  public final void emitUCOMISS_Reg_RegInd(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x2E);
    emitRegIndirectRegOperands(srcReg, dstReg);
    if (lister != null) lister.RRN(miStart, "UCOMISS", dstReg, srcReg);
  }
  
  
  // dstReg <<=  srcReg
  public final void emitCMPEQSS_Reg_Reg(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitRegRegOperands(srcReg, dstReg);
    setMachineCodes(mi++, (byte) 0);
    if (lister != null) lister.RR(miStart, "CMPEQSS", dstReg, srcReg);
  }

  // dstReg <<=  [srcReg + srcDisp]
  public final void emitCMPEQSS_Reg_RegDisp(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitRegDispRegOperands(srcReg, disp, dstReg);
    setMachineCodes(mi++, (byte) 0);
    if (lister != null) lister.RRD(miStart, "CMPEQSS", dstReg, srcReg, disp);
  }

  // dstReg <<=  [srcIndex<<scale + srcDisp]
  public final void emitCMPEQSS_Reg_RegOff(byte dstReg, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitRegOffRegOperands(srcIndex, scale, srcDisp, dstReg);
    setMachineCodes(mi++, (byte) 0);
    if (lister != null) lister.RRFD(miStart, "CMPEQSS", dstReg, srcIndex, scale, srcDisp);
  }

  // dstReg <<=  [srcDisp]
  public final void emitCMPEQSS_Reg_Abs(byte dstReg, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitAbsRegOperands(srcDisp, dstReg);
    setMachineCodes(mi++, (byte) 0);
    if (lister != null) lister.RRA(miStart, "CMPEQSS", dstReg, srcDisp);
  }

  // dstReg <<=  [srcBase + srcIndex<<scale + srcDisp]
  public final void emitCMPEQSS_Reg_RegIdx(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitSIBRegOperands(srcBase, srcIndex, scale, srcDisp, dstReg);
    setMachineCodes(mi++, (byte) 0);
    if (lister != null) lister.RRXD(miStart, "CMPEQSS", dstReg, srcBase, srcIndex, scale, srcDisp);
  }

  // dstReg <<=  [srcReg]
  public final void emitCMPEQSS_Reg_RegInd(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitRegIndirectRegOperands(srcReg, dstReg);
    setMachineCodes(mi++, (byte) 0);
    if (lister != null) lister.RRN(miStart, "CMPEQSS", dstReg, srcReg);
  }
  
  
  // dstReg <<=  srcReg
  public final void emitCMPLTSS_Reg_Reg(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitRegRegOperands(srcReg, dstReg);
    setMachineCodes(mi++, (byte) 1);
    if (lister != null) lister.RR(miStart, "CMPLTSS", dstReg, srcReg);
  }

  // dstReg <<=  [srcReg + srcDisp]
  public final void emitCMPLTSS_Reg_RegDisp(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitRegDispRegOperands(srcReg, disp, dstReg);
    setMachineCodes(mi++, (byte) 1);
    if (lister != null) lister.RRD(miStart, "CMPLTSS", dstReg, srcReg, disp);
  }

  // dstReg <<=  [srcIndex<<scale + srcDisp]
  public final void emitCMPLTSS_Reg_RegOff(byte dstReg, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitRegOffRegOperands(srcIndex, scale, srcDisp, dstReg);
    setMachineCodes(mi++, (byte) 1);
    if (lister != null) lister.RRFD(miStart, "CMPLTSS", dstReg, srcIndex, scale, srcDisp);
  }

  // dstReg <<=  [srcDisp]
  public final void emitCMPLTSS_Reg_Abs(byte dstReg, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitAbsRegOperands(srcDisp, dstReg);
    setMachineCodes(mi++, (byte) 1);
    if (lister != null) lister.RRA(miStart, "CMPLTSS", dstReg, srcDisp);
  }

  // dstReg <<=  [srcBase + srcIndex<<scale + srcDisp]
  public final void emitCMPLTSS_Reg_RegIdx(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitSIBRegOperands(srcBase, srcIndex, scale, srcDisp, dstReg);
    setMachineCodes(mi++, (byte) 1);
    if (lister != null) lister.RRXD(miStart, "CMPLTSS", dstReg, srcBase, srcIndex, scale, srcDisp);
  }

  // dstReg <<=  [srcReg]
  public final void emitCMPLTSS_Reg_RegInd(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitRegIndirectRegOperands(srcReg, dstReg);
    setMachineCodes(mi++, (byte) 1);
    if (lister != null) lister.RRN(miStart, "CMPLTSS", dstReg, srcReg);
  }
  
  
  // dstReg <<=  srcReg
  public final void emitCMPLESS_Reg_Reg(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitRegRegOperands(srcReg, dstReg);
    setMachineCodes(mi++, (byte) 2);
    if (lister != null) lister.RR(miStart, "CMPLESS", dstReg, srcReg);
  }

  // dstReg <<=  [srcReg + srcDisp]
  public final void emitCMPLESS_Reg_RegDisp(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitRegDispRegOperands(srcReg, disp, dstReg);
    setMachineCodes(mi++, (byte) 2);
    if (lister != null) lister.RRD(miStart, "CMPLESS", dstReg, srcReg, disp);
  }

  // dstReg <<=  [srcIndex<<scale + srcDisp]
  public final void emitCMPLESS_Reg_RegOff(byte dstReg, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitRegOffRegOperands(srcIndex, scale, srcDisp, dstReg);
    setMachineCodes(mi++, (byte) 2);
    if (lister != null) lister.RRFD(miStart, "CMPLESS", dstReg, srcIndex, scale, srcDisp);
  }

  // dstReg <<=  [srcDisp]
  public final void emitCMPLESS_Reg_Abs(byte dstReg, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitAbsRegOperands(srcDisp, dstReg);
    setMachineCodes(mi++, (byte) 2);
    if (lister != null) lister.RRA(miStart, "CMPLESS", dstReg, srcDisp);
  }

  // dstReg <<=  [srcBase + srcIndex<<scale + srcDisp]
  public final void emitCMPLESS_Reg_RegIdx(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitSIBRegOperands(srcBase, srcIndex, scale, srcDisp, dstReg);
    setMachineCodes(mi++, (byte) 2);
    if (lister != null) lister.RRXD(miStart, "CMPLESS", dstReg, srcBase, srcIndex, scale, srcDisp);
  }

  // dstReg <<=  [srcReg]
  public final void emitCMPLESS_Reg_RegInd(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitRegIndirectRegOperands(srcReg, dstReg);
    setMachineCodes(mi++, (byte) 2);
    if (lister != null) lister.RRN(miStart, "CMPLESS", dstReg, srcReg);
  }
  
  
  // dstReg <<=  srcReg
  public final void emitCMPUNORDSS_Reg_Reg(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitRegRegOperands(srcReg, dstReg);
    setMachineCodes(mi++, (byte) 3);
    if (lister != null) lister.RR(miStart, "CMPUNORDSS", dstReg, srcReg);
  }

  // dstReg <<=  [srcReg + srcDisp]
  public final void emitCMPUNORDSS_Reg_RegDisp(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitRegDispRegOperands(srcReg, disp, dstReg);
    setMachineCodes(mi++, (byte) 3);
    if (lister != null) lister.RRD(miStart, "CMPUNORDSS", dstReg, srcReg, disp);
  }

  // dstReg <<=  [srcIndex<<scale + srcDisp]
  public final void emitCMPUNORDSS_Reg_RegOff(byte dstReg, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitRegOffRegOperands(srcIndex, scale, srcDisp, dstReg);
    setMachineCodes(mi++, (byte) 3);
    if (lister != null) lister.RRFD(miStart, "CMPUNORDSS", dstReg, srcIndex, scale, srcDisp);
  }

  // dstReg <<=  [srcDisp]
  public final void emitCMPUNORDSS_Reg_Abs(byte dstReg, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitAbsRegOperands(srcDisp, dstReg);
    setMachineCodes(mi++, (byte) 3);
    if (lister != null) lister.RRA(miStart, "CMPUNORDSS", dstReg, srcDisp);
  }

  // dstReg <<=  [srcBase + srcIndex<<scale + srcDisp]
  public final void emitCMPUNORDSS_Reg_RegIdx(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitSIBRegOperands(srcBase, srcIndex, scale, srcDisp, dstReg);
    setMachineCodes(mi++, (byte) 3);
    if (lister != null) lister.RRXD(miStart, "CMPUNORDSS", dstReg, srcBase, srcIndex, scale, srcDisp);
  }

  // dstReg <<=  [srcReg]
  public final void emitCMPUNORDSS_Reg_RegInd(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitRegIndirectRegOperands(srcReg, dstReg);
    setMachineCodes(mi++, (byte) 3);
    if (lister != null) lister.RRN(miStart, "CMPUNORDSS", dstReg, srcReg);
  }
  
  
  // dstReg <<=  srcReg
  public final void emitCMPNESS_Reg_Reg(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitRegRegOperands(srcReg, dstReg);
    setMachineCodes(mi++, (byte) 4);
    if (lister != null) lister.RR(miStart, "CMPNESS", dstReg, srcReg);
  }

  // dstReg <<=  [srcReg + srcDisp]
  public final void emitCMPNESS_Reg_RegDisp(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitRegDispRegOperands(srcReg, disp, dstReg);
    setMachineCodes(mi++, (byte) 4);
    if (lister != null) lister.RRD(miStart, "CMPNESS", dstReg, srcReg, disp);
  }

  // dstReg <<=  [srcIndex<<scale + srcDisp]
  public final void emitCMPNESS_Reg_RegOff(byte dstReg, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitRegOffRegOperands(srcIndex, scale, srcDisp, dstReg);
    setMachineCodes(mi++, (byte) 4);
    if (lister != null) lister.RRFD(miStart, "CMPNESS", dstReg, srcIndex, scale, srcDisp);
  }

  // dstReg <<=  [srcDisp]
  public final void emitCMPNESS_Reg_Abs(byte dstReg, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitAbsRegOperands(srcDisp, dstReg);
    setMachineCodes(mi++, (byte) 4);
    if (lister != null) lister.RRA(miStart, "CMPNESS", dstReg, srcDisp);
  }

  // dstReg <<=  [srcBase + srcIndex<<scale + srcDisp]
  public final void emitCMPNESS_Reg_RegIdx(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitSIBRegOperands(srcBase, srcIndex, scale, srcDisp, dstReg);
    setMachineCodes(mi++, (byte) 4);
    if (lister != null) lister.RRXD(miStart, "CMPNESS", dstReg, srcBase, srcIndex, scale, srcDisp);
  }

  // dstReg <<=  [srcReg]
  public final void emitCMPNESS_Reg_RegInd(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitRegIndirectRegOperands(srcReg, dstReg);
    setMachineCodes(mi++, (byte) 4);
    if (lister != null) lister.RRN(miStart, "CMPNESS", dstReg, srcReg);
  }
  
  
  // dstReg <<=  srcReg
  public final void emitCMPNLTSS_Reg_Reg(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitRegRegOperands(srcReg, dstReg);
    setMachineCodes(mi++, (byte) 5);
    if (lister != null) lister.RR(miStart, "CMPNLTSS", dstReg, srcReg);
  }

  // dstReg <<=  [srcReg + srcDisp]
  public final void emitCMPNLTSS_Reg_RegDisp(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitRegDispRegOperands(srcReg, disp, dstReg);
    setMachineCodes(mi++, (byte) 5);
    if (lister != null) lister.RRD(miStart, "CMPNLTSS", dstReg, srcReg, disp);
  }

  // dstReg <<=  [srcIndex<<scale + srcDisp]
  public final void emitCMPNLTSS_Reg_RegOff(byte dstReg, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitRegOffRegOperands(srcIndex, scale, srcDisp, dstReg);
    setMachineCodes(mi++, (byte) 5);
    if (lister != null) lister.RRFD(miStart, "CMPNLTSS", dstReg, srcIndex, scale, srcDisp);
  }

  // dstReg <<=  [srcDisp]
  public final void emitCMPNLTSS_Reg_Abs(byte dstReg, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitAbsRegOperands(srcDisp, dstReg);
    setMachineCodes(mi++, (byte) 5);
    if (lister != null) lister.RRA(miStart, "CMPNLTSS", dstReg, srcDisp);
  }

  // dstReg <<=  [srcBase + srcIndex<<scale + srcDisp]
  public final void emitCMPNLTSS_Reg_RegIdx(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitSIBRegOperands(srcBase, srcIndex, scale, srcDisp, dstReg);
    setMachineCodes(mi++, (byte) 5);
    if (lister != null) lister.RRXD(miStart, "CMPNLTSS", dstReg, srcBase, srcIndex, scale, srcDisp);
  }

  // dstReg <<=  [srcReg]
  public final void emitCMPNLTSS_Reg_RegInd(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitRegIndirectRegOperands(srcReg, dstReg);
    setMachineCodes(mi++, (byte) 5);
    if (lister != null) lister.RRN(miStart, "CMPNLTSS", dstReg, srcReg);
  }
  
  
  // dstReg <<=  srcReg
  public final void emitCMPNLESS_Reg_Reg(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitRegRegOperands(srcReg, dstReg);
    setMachineCodes(mi++, (byte) 6);
    if (lister != null) lister.RR(miStart, "CMPNLESS", dstReg, srcReg);
  }

  // dstReg <<=  [srcReg + srcDisp]
  public final void emitCMPNLESS_Reg_RegDisp(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitRegDispRegOperands(srcReg, disp, dstReg);
    setMachineCodes(mi++, (byte) 6);
    if (lister != null) lister.RRD(miStart, "CMPNLESS", dstReg, srcReg, disp);
  }

  // dstReg <<=  [srcIndex<<scale + srcDisp]
  public final void emitCMPNLESS_Reg_RegOff(byte dstReg, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitRegOffRegOperands(srcIndex, scale, srcDisp, dstReg);
    setMachineCodes(mi++, (byte) 6);
    if (lister != null) lister.RRFD(miStart, "CMPNLESS", dstReg, srcIndex, scale, srcDisp);
  }

  // dstReg <<=  [srcDisp]
  public final void emitCMPNLESS_Reg_Abs(byte dstReg, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitAbsRegOperands(srcDisp, dstReg);
    setMachineCodes(mi++, (byte) 6);
    if (lister != null) lister.RRA(miStart, "CMPNLESS", dstReg, srcDisp);
  }

  // dstReg <<=  [srcBase + srcIndex<<scale + srcDisp]
  public final void emitCMPNLESS_Reg_RegIdx(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitSIBRegOperands(srcBase, srcIndex, scale, srcDisp, dstReg);
    setMachineCodes(mi++, (byte) 6);
    if (lister != null) lister.RRXD(miStart, "CMPNLESS", dstReg, srcBase, srcIndex, scale, srcDisp);
  }

  // dstReg <<=  [srcReg]
  public final void emitCMPNLESS_Reg_RegInd(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitRegIndirectRegOperands(srcReg, dstReg);
    setMachineCodes(mi++, (byte) 6);
    if (lister != null) lister.RRN(miStart, "CMPNLESS", dstReg, srcReg);
  }
  
  
  // dstReg <<=  srcReg
  public final void emitCMPORDSS_Reg_Reg(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitRegRegOperands(srcReg, dstReg);
    setMachineCodes(mi++, (byte) 7);
    if (lister != null) lister.RR(miStart, "CMPORDSS", dstReg, srcReg);
  }

  // dstReg <<=  [srcReg + srcDisp]
  public final void emitCMPORDSS_Reg_RegDisp(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitRegDispRegOperands(srcReg, disp, dstReg);
    setMachineCodes(mi++, (byte) 7);
    if (lister != null) lister.RRD(miStart, "CMPORDSS", dstReg, srcReg, disp);
  }

  // dstReg <<=  [srcIndex<<scale + srcDisp]
  public final void emitCMPORDSS_Reg_RegOff(byte dstReg, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitRegOffRegOperands(srcIndex, scale, srcDisp, dstReg);
    setMachineCodes(mi++, (byte) 7);
    if (lister != null) lister.RRFD(miStart, "CMPORDSS", dstReg, srcIndex, scale, srcDisp);
  }

  // dstReg <<=  [srcDisp]
  public final void emitCMPORDSS_Reg_Abs(byte dstReg, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitAbsRegOperands(srcDisp, dstReg);
    setMachineCodes(mi++, (byte) 7);
    if (lister != null) lister.RRA(miStart, "CMPORDSS", dstReg, srcDisp);
  }

  // dstReg <<=  [srcBase + srcIndex<<scale + srcDisp]
  public final void emitCMPORDSS_Reg_RegIdx(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitSIBRegOperands(srcBase, srcIndex, scale, srcDisp, dstReg);
    setMachineCodes(mi++, (byte) 7);
    if (lister != null) lister.RRXD(miStart, "CMPORDSS", dstReg, srcBase, srcIndex, scale, srcDisp);
  }

  // dstReg <<=  [srcReg]
  public final void emitCMPORDSS_Reg_RegInd(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitRegIndirectRegOperands(srcReg, dstReg);
    setMachineCodes(mi++, (byte) 7);
    if (lister != null) lister.RRN(miStart, "CMPORDSS", dstReg, srcReg);
  }
  
  
  // dstReg <<=  srcReg
  public final void emitMOVD_Reg_Reg(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x7E);
    emitRegRegOperands(srcReg, dstReg);
    if (lister != null) lister.RR(miStart, "MOVD", dstReg, srcReg);
  }

  /**
   * Generate a register--register MOVD. That is,
   * <PRE>
   * [dstReg] <<=  srcReg
   * </PRE>
   *
   * @param dstReg the destination register
   * @param srcReg the source register
   */
  public final void emitMOVD_RegInd_Reg(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x7E);
    emitRegIndirectRegOperands(dstReg, srcReg);
    if (lister != null) lister.RNR(miStart, "MOVD", dstReg, srcReg);
  }

  /**
   * Generate a register-offset--register MOVD. That is,
   * <PRE>
   * [dstReg<<dstScale + dstDisp] <<=  srcReg
   * </PRE>
   *
   * @param dstIndex the destination index register
   * @param dstScale the destination shift amount
   * @param dstDisp the destination displacement
   * @param srcReg the source register
   */
  public final void emitMOVD_RegOff_Reg(byte dstIndex, short dstScale, Offset dstDisp, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x7E);
    emitRegOffRegOperands(dstIndex, dstScale, dstDisp, srcReg);
    if (lister != null) lister.RFDR(miStart, "MOVD", dstIndex, dstScale, dstDisp, srcReg);
  }

  // [dstDisp] <<=  srcReg
  public final void emitMOVD_Abs_Reg(Offset dstDisp, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x7E);
    emitAbsRegOperands(dstDisp, srcReg);
    if (lister != null) lister.RAR(miStart, "MOVD", dstDisp, srcReg);
  }

  // [dstBase + dstIndex<<scale + dstDisp] <<=  srcReg
  public final void emitMOVD_RegIdx_Reg(byte dstBase, byte dstIndex, short scale, Offset dstDisp, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x7E);
    emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, srcReg);
    if (lister != null) lister.RXDR(miStart, "MOVD", dstBase, dstIndex, scale, dstDisp, srcReg);
  }

  // [dstReg + dstDisp] <<=  srcReg
  public final void emitMOVD_RegDisp_Reg(byte dstReg, Offset disp, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x7E);
    emitRegDispRegOperands(dstReg, disp, srcReg);
    if (lister != null) lister.RDR(miStart, "MOVD", dstReg, disp, srcReg);
  }

  
  // dstReg <<=  srcReg
  public final void emitMOVQ_Reg_Reg(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x7E);
    emitRegRegOperands(srcReg, dstReg);
    if (lister != null) lister.RR(miStart, "MOVQ", dstReg, srcReg);
  }

  // dstReg <<=  [srcReg + srcDisp]
  public final void emitMOVQ_Reg_RegDisp(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x7E);
    emitRegDispRegOperands(srcReg, disp, dstReg);
    if (lister != null) lister.RRD(miStart, "MOVQ", dstReg, srcReg, disp);
  }

  // dstReg <<=  [srcIndex<<scale + srcDisp]
  public final void emitMOVQ_Reg_RegOff(byte dstReg, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x7E);
    emitRegOffRegOperands(srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRFD(miStart, "MOVQ", dstReg, srcIndex, scale, srcDisp);
  }

  // dstReg <<=  [srcDisp]
  public final void emitMOVQ_Reg_Abs(byte dstReg, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x7E);
    emitAbsRegOperands(srcDisp, dstReg);
    if (lister != null) lister.RRA(miStart, "MOVQ", dstReg, srcDisp);
  }

  // dstReg <<=  [srcBase + srcIndex<<scale + srcDisp]
  public final void emitMOVQ_Reg_RegIdx(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x7E);
    emitSIBRegOperands(srcBase, srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRXD(miStart, "MOVQ", dstReg, srcBase, srcIndex, scale, srcDisp);
  }

  // dstReg <<=  [srcReg]
  public final void emitMOVQ_Reg_RegInd(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF3);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x7E);
    emitRegIndirectRegOperands(srcReg, dstReg);
    if (lister != null) lister.RRN(miStart, "MOVQ", dstReg, srcReg);
  }
  

  /**
   * Generate a register--register MOVQ. That is,
   * <PRE>
   * [dstReg] <<=  srcReg
   * </PRE>
   *
   * @param dstReg the destination register
   * @param srcReg the source register
   */
  public final void emitMOVQ_RegInd_Reg(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xD6);
    emitRegIndirectRegOperands(dstReg, srcReg);
    if (lister != null) lister.RNR(miStart, "MOVQ", dstReg, srcReg);
  }

  /**
   * Generate a register-offset--register MOVQ. That is,
   * <PRE>
   * [dstReg<<dstScale + dstDisp] <<=  srcReg
   * </PRE>
   *
   * @param dstIndex the destination index register
   * @param dstScale the destination shift amount
   * @param dstDisp the destination displacement
   * @param srcReg the source register
   */
  public final void emitMOVQ_RegOff_Reg(byte dstIndex, short dstScale, Offset dstDisp, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xD6);
    emitRegOffRegOperands(dstIndex, dstScale, dstDisp, srcReg);
    if (lister != null) lister.RFDR(miStart, "MOVQ", dstIndex, dstScale, dstDisp, srcReg);
  }

  // [dstDisp] <<=  srcReg
  public final void emitMOVQ_Abs_Reg(Offset dstDisp, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xD6);
    emitAbsRegOperands(dstDisp, srcReg);
    if (lister != null) lister.RAR(miStart, "MOVQ", dstDisp, srcReg);
  }

  // [dstBase + dstIndex<<scale + dstDisp] <<=  srcReg
  public final void emitMOVQ_RegIdx_Reg(byte dstBase, byte dstIndex, short scale, Offset dstDisp, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xD6);
    emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, srcReg);
    if (lister != null) lister.RXDR(miStart, "MOVQ", dstBase, dstIndex, scale, dstDisp, srcReg);
  }

  // [dstReg + dstDisp] <<=  srcReg
  public final void emitMOVQ_RegDisp_Reg(byte dstReg, Offset disp, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xD6);
    emitRegDispRegOperands(dstReg, disp, srcReg);
    if (lister != null) lister.RDR(miStart, "MOVQ", dstReg, disp, srcReg);
  }

  
  // dstReg <<=  srcReg
  public final void emitADDSD_Reg_Reg(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x58);
    emitRegRegOperands(srcReg, dstReg);
    if (lister != null) lister.RR(miStart, "ADDSD", dstReg, srcReg);
  }

  // dstReg <<=  [srcReg + srcDisp]
  public final void emitADDSD_Reg_RegDisp(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x58);
    emitRegDispRegOperands(srcReg, disp, dstReg);
    if (lister != null) lister.RRD(miStart, "ADDSD", dstReg, srcReg, disp);
  }

  // dstReg <<=  [srcIndex<<scale + srcDisp]
  public final void emitADDSD_Reg_RegOff(byte dstReg, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x58);
    emitRegOffRegOperands(srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRFD(miStart, "ADDSD", dstReg, srcIndex, scale, srcDisp);
  }

  // dstReg <<=  [srcDisp]
  public final void emitADDSD_Reg_Abs(byte dstReg, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x58);
    emitAbsRegOperands(srcDisp, dstReg);
    if (lister != null) lister.RRA(miStart, "ADDSD", dstReg, srcDisp);
  }

  // dstReg <<=  [srcBase + srcIndex<<scale + srcDisp]
  public final void emitADDSD_Reg_RegIdx(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x58);
    emitSIBRegOperands(srcBase, srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRXD(miStart, "ADDSD", dstReg, srcBase, srcIndex, scale, srcDisp);
  }

  // dstReg <<=  [srcReg]
  public final void emitADDSD_Reg_RegInd(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x58);
    emitRegIndirectRegOperands(srcReg, dstReg);
    if (lister != null) lister.RRN(miStart, "ADDSD", dstReg, srcReg);
  }
  
  
  // dstReg <<=  srcReg
  public final void emitSUBSD_Reg_Reg(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x5C);
    emitRegRegOperands(srcReg, dstReg);
    if (lister != null) lister.RR(miStart, "SUBSD", dstReg, srcReg);
  }

  // dstReg <<=  [srcReg + srcDisp]
  public final void emitSUBSD_Reg_RegDisp(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x5C);
    emitRegDispRegOperands(srcReg, disp, dstReg);
    if (lister != null) lister.RRD(miStart, "SUBSD", dstReg, srcReg, disp);
  }

  // dstReg <<=  [srcIndex<<scale + srcDisp]
  public final void emitSUBSD_Reg_RegOff(byte dstReg, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x5C);
    emitRegOffRegOperands(srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRFD(miStart, "SUBSD", dstReg, srcIndex, scale, srcDisp);
  }

  // dstReg <<=  [srcDisp]
  public final void emitSUBSD_Reg_Abs(byte dstReg, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x5C);
    emitAbsRegOperands(srcDisp, dstReg);
    if (lister != null) lister.RRA(miStart, "SUBSD", dstReg, srcDisp);
  }

  // dstReg <<=  [srcBase + srcIndex<<scale + srcDisp]
  public final void emitSUBSD_Reg_RegIdx(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x5C);
    emitSIBRegOperands(srcBase, srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRXD(miStart, "SUBSD", dstReg, srcBase, srcIndex, scale, srcDisp);
  }

  // dstReg <<=  [srcReg]
  public final void emitSUBSD_Reg_RegInd(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x5C);
    emitRegIndirectRegOperands(srcReg, dstReg);
    if (lister != null) lister.RRN(miStart, "SUBSD", dstReg, srcReg);
  }
  
  
  // dstReg <<=  srcReg
  public final void emitMULSD_Reg_Reg(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x59);
    emitRegRegOperands(srcReg, dstReg);
    if (lister != null) lister.RR(miStart, "MULSD", dstReg, srcReg);
  }

  // dstReg <<=  [srcReg + srcDisp]
  public final void emitMULSD_Reg_RegDisp(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x59);
    emitRegDispRegOperands(srcReg, disp, dstReg);
    if (lister != null) lister.RRD(miStart, "MULSD", dstReg, srcReg, disp);
  }

  // dstReg <<=  [srcIndex<<scale + srcDisp]
  public final void emitMULSD_Reg_RegOff(byte dstReg, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x59);
    emitRegOffRegOperands(srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRFD(miStart, "MULSD", dstReg, srcIndex, scale, srcDisp);
  }

  // dstReg <<=  [srcDisp]
  public final void emitMULSD_Reg_Abs(byte dstReg, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x59);
    emitAbsRegOperands(srcDisp, dstReg);
    if (lister != null) lister.RRA(miStart, "MULSD", dstReg, srcDisp);
  }

  // dstReg <<=  [srcBase + srcIndex<<scale + srcDisp]
  public final void emitMULSD_Reg_RegIdx(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x59);
    emitSIBRegOperands(srcBase, srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRXD(miStart, "MULSD", dstReg, srcBase, srcIndex, scale, srcDisp);
  }

  // dstReg <<=  [srcReg]
  public final void emitMULSD_Reg_RegInd(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x59);
    emitRegIndirectRegOperands(srcReg, dstReg);
    if (lister != null) lister.RRN(miStart, "MULSD", dstReg, srcReg);
  }
  
  
  // dstReg <<=  srcReg
  public final void emitDIVSD_Reg_Reg(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x5E);
    emitRegRegOperands(srcReg, dstReg);
    if (lister != null) lister.RR(miStart, "DIVSD", dstReg, srcReg);
  }

  // dstReg <<=  [srcReg + srcDisp]
  public final void emitDIVSD_Reg_RegDisp(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x5E);
    emitRegDispRegOperands(srcReg, disp, dstReg);
    if (lister != null) lister.RRD(miStart, "DIVSD", dstReg, srcReg, disp);
  }

  // dstReg <<=  [srcIndex<<scale + srcDisp]
  public final void emitDIVSD_Reg_RegOff(byte dstReg, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x5E);
    emitRegOffRegOperands(srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRFD(miStart, "DIVSD", dstReg, srcIndex, scale, srcDisp);
  }

  // dstReg <<=  [srcDisp]
  public final void emitDIVSD_Reg_Abs(byte dstReg, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x5E);
    emitAbsRegOperands(srcDisp, dstReg);
    if (lister != null) lister.RRA(miStart, "DIVSD", dstReg, srcDisp);
  }

  // dstReg <<=  [srcBase + srcIndex<<scale + srcDisp]
  public final void emitDIVSD_Reg_RegIdx(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x5E);
    emitSIBRegOperands(srcBase, srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRXD(miStart, "DIVSD", dstReg, srcBase, srcIndex, scale, srcDisp);
  }

  // dstReg <<=  [srcReg]
  public final void emitDIVSD_Reg_RegInd(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x5E);
    emitRegIndirectRegOperands(srcReg, dstReg);
    if (lister != null) lister.RRN(miStart, "DIVSD", dstReg, srcReg);
  }
  
  
  // dstReg <<=  srcReg
  public final void emitMOVSD_Reg_Reg(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x10);
    emitRegRegOperands(srcReg, dstReg);
    if (lister != null) lister.RR(miStart, "MOVSD", dstReg, srcReg);
  }

  // dstReg <<=  [srcReg + srcDisp]
  public final void emitMOVSD_Reg_RegDisp(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x10);
    emitRegDispRegOperands(srcReg, disp, dstReg);
    if (lister != null) lister.RRD(miStart, "MOVSD", dstReg, srcReg, disp);
  }

  // dstReg <<=  [srcIndex<<scale + srcDisp]
  public final void emitMOVSD_Reg_RegOff(byte dstReg, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x10);
    emitRegOffRegOperands(srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRFD(miStart, "MOVSD", dstReg, srcIndex, scale, srcDisp);
  }

  // dstReg <<=  [srcDisp]
  public final void emitMOVSD_Reg_Abs(byte dstReg, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x10);
    emitAbsRegOperands(srcDisp, dstReg);
    if (lister != null) lister.RRA(miStart, "MOVSD", dstReg, srcDisp);
  }

  // dstReg <<=  [srcBase + srcIndex<<scale + srcDisp]
  public final void emitMOVSD_Reg_RegIdx(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x10);
    emitSIBRegOperands(srcBase, srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRXD(miStart, "MOVSD", dstReg, srcBase, srcIndex, scale, srcDisp);
  }

  // dstReg <<=  [srcReg]
  public final void emitMOVSD_Reg_RegInd(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x10);
    emitRegIndirectRegOperands(srcReg, dstReg);
    if (lister != null) lister.RRN(miStart, "MOVSD", dstReg, srcReg);
  }
  

  /**
   * Generate a register--register MOVSD. That is,
   * <PRE>
   * [dstReg] <<=  srcReg
   * </PRE>
   *
   * @param dstReg the destination register
   * @param srcReg the source register
   */
  public final void emitMOVSD_RegInd_Reg(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x11);
    emitRegIndirectRegOperands(dstReg, srcReg);
    if (lister != null) lister.RNR(miStart, "MOVSD", dstReg, srcReg);
  }

  /**
   * Generate a register-offset--register MOVSD. That is,
   * <PRE>
   * [dstReg<<dstScale + dstDisp] <<=  srcReg
   * </PRE>
   *
   * @param dstIndex the destination index register
   * @param dstScale the destination shift amount
   * @param dstDisp the destination displacement
   * @param srcReg the source register
   */
  public final void emitMOVSD_RegOff_Reg(byte dstIndex, short dstScale, Offset dstDisp, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x11);
    emitRegOffRegOperands(dstIndex, dstScale, dstDisp, srcReg);
    if (lister != null) lister.RFDR(miStart, "MOVSD", dstIndex, dstScale, dstDisp, srcReg);
  }

  // [dstDisp] <<=  srcReg
  public final void emitMOVSD_Abs_Reg(Offset dstDisp, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x11);
    emitAbsRegOperands(dstDisp, srcReg);
    if (lister != null) lister.RAR(miStart, "MOVSD", dstDisp, srcReg);
  }

  // [dstBase + dstIndex<<scale + dstDisp] <<=  srcReg
  public final void emitMOVSD_RegIdx_Reg(byte dstBase, byte dstIndex, short scale, Offset dstDisp, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x11);
    emitSIBRegOperands(dstBase, dstIndex, scale, dstDisp, srcReg);
    if (lister != null) lister.RXDR(miStart, "MOVSD", dstBase, dstIndex, scale, dstDisp, srcReg);
  }

  // [dstReg + dstDisp] <<=  srcReg
  public final void emitMOVSD_RegDisp_Reg(byte dstReg, Offset disp, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x11);
    emitRegDispRegOperands(dstReg, disp, srcReg);
    if (lister != null) lister.RDR(miStart, "MOVSD", dstReg, disp, srcReg);
  }

  
  // dstReg <<=  srcReg
  public final void emitCVTSI2SD_Reg_Reg(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x2A);
    emitRegRegOperands(srcReg, dstReg);
    if (lister != null) lister.RR(miStart, "CVTSI2SD", dstReg, srcReg);
  }

  // dstReg <<=  [srcReg + srcDisp]
  public final void emitCVTSI2SD_Reg_RegDisp(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x2A);
    emitRegDispRegOperands(srcReg, disp, dstReg);
    if (lister != null) lister.RRD(miStart, "CVTSI2SD", dstReg, srcReg, disp);
  }

  // dstReg <<=  [srcIndex<<scale + srcDisp]
  public final void emitCVTSI2SD_Reg_RegOff(byte dstReg, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x2A);
    emitRegOffRegOperands(srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRFD(miStart, "CVTSI2SD", dstReg, srcIndex, scale, srcDisp);
  }

  // dstReg <<=  [srcDisp]
  public final void emitCVTSI2SD_Reg_Abs(byte dstReg, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x2A);
    emitAbsRegOperands(srcDisp, dstReg);
    if (lister != null) lister.RRA(miStart, "CVTSI2SD", dstReg, srcDisp);
  }

  // dstReg <<=  [srcBase + srcIndex<<scale + srcDisp]
  public final void emitCVTSI2SD_Reg_RegIdx(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x2A);
    emitSIBRegOperands(srcBase, srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRXD(miStart, "CVTSI2SD", dstReg, srcBase, srcIndex, scale, srcDisp);
  }

  // dstReg <<=  [srcReg]
  public final void emitCVTSI2SD_Reg_RegInd(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x2A);
    emitRegIndirectRegOperands(srcReg, dstReg);
    if (lister != null) lister.RRN(miStart, "CVTSI2SD", dstReg, srcReg);
  }
  
  
  // dstReg <<=  srcReg
  public final void emitCVTSD2SS_Reg_Reg(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x5A);
    emitRegRegOperands(srcReg, dstReg);
    if (lister != null) lister.RR(miStart, "CVTSD2SS", dstReg, srcReg);
  }

  // dstReg <<=  [srcReg + srcDisp]
  public final void emitCVTSD2SS_Reg_RegDisp(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x5A);
    emitRegDispRegOperands(srcReg, disp, dstReg);
    if (lister != null) lister.RRD(miStart, "CVTSD2SS", dstReg, srcReg, disp);
  }

  // dstReg <<=  [srcIndex<<scale + srcDisp]
  public final void emitCVTSD2SS_Reg_RegOff(byte dstReg, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x5A);
    emitRegOffRegOperands(srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRFD(miStart, "CVTSD2SS", dstReg, srcIndex, scale, srcDisp);
  }

  // dstReg <<=  [srcDisp]
  public final void emitCVTSD2SS_Reg_Abs(byte dstReg, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x5A);
    emitAbsRegOperands(srcDisp, dstReg);
    if (lister != null) lister.RRA(miStart, "CVTSD2SS", dstReg, srcDisp);
  }

  // dstReg <<=  [srcBase + srcIndex<<scale + srcDisp]
  public final void emitCVTSD2SS_Reg_RegIdx(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x5A);
    emitSIBRegOperands(srcBase, srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRXD(miStart, "CVTSD2SS", dstReg, srcBase, srcIndex, scale, srcDisp);
  }

  // dstReg <<=  [srcReg]
  public final void emitCVTSD2SS_Reg_RegInd(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x5A);
    emitRegIndirectRegOperands(srcReg, dstReg);
    if (lister != null) lister.RRN(miStart, "CVTSD2SS", dstReg, srcReg);
  }
  
  
  // dstReg <<=  srcReg
  public final void emitCVTSD2SI_Reg_Reg(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x2D);
    emitRegRegOperands(srcReg, dstReg);
    if (lister != null) lister.RR(miStart, "CVTSD2SI", dstReg, srcReg);
  }

  // dstReg <<=  [srcReg + srcDisp]
  public final void emitCVTSD2SI_Reg_RegDisp(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x2D);
    emitRegDispRegOperands(srcReg, disp, dstReg);
    if (lister != null) lister.RRD(miStart, "CVTSD2SI", dstReg, srcReg, disp);
  }

  // dstReg <<=  [srcIndex<<scale + srcDisp]
  public final void emitCVTSD2SI_Reg_RegOff(byte dstReg, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x2D);
    emitRegOffRegOperands(srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRFD(miStart, "CVTSD2SI", dstReg, srcIndex, scale, srcDisp);
  }

  // dstReg <<=  [srcDisp]
  public final void emitCVTSD2SI_Reg_Abs(byte dstReg, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x2D);
    emitAbsRegOperands(srcDisp, dstReg);
    if (lister != null) lister.RRA(miStart, "CVTSD2SI", dstReg, srcDisp);
  }

  // dstReg <<=  [srcBase + srcIndex<<scale + srcDisp]
  public final void emitCVTSD2SI_Reg_RegIdx(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x2D);
    emitSIBRegOperands(srcBase, srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRXD(miStart, "CVTSD2SI", dstReg, srcBase, srcIndex, scale, srcDisp);
  }

  // dstReg <<=  [srcReg]
  public final void emitCVTSD2SI_Reg_RegInd(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x2D);
    emitRegIndirectRegOperands(srcReg, dstReg);
    if (lister != null) lister.RRN(miStart, "CVTSD2SI", dstReg, srcReg);
  }
  
  
  // dstReg <<=  srcReg
  public final void emitCVTTSD2SI_Reg_Reg(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x2C);
    emitRegRegOperands(srcReg, dstReg);
    if (lister != null) lister.RR(miStart, "CVTTSD2SI", dstReg, srcReg);
  }

  // dstReg <<=  [srcReg + srcDisp]
  public final void emitCVTTSD2SI_Reg_RegDisp(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x2C);
    emitRegDispRegOperands(srcReg, disp, dstReg);
    if (lister != null) lister.RRD(miStart, "CVTTSD2SI", dstReg, srcReg, disp);
  }

  // dstReg <<=  [srcIndex<<scale + srcDisp]
  public final void emitCVTTSD2SI_Reg_RegOff(byte dstReg, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x2C);
    emitRegOffRegOperands(srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRFD(miStart, "CVTTSD2SI", dstReg, srcIndex, scale, srcDisp);
  }

  // dstReg <<=  [srcDisp]
  public final void emitCVTTSD2SI_Reg_Abs(byte dstReg, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x2C);
    emitAbsRegOperands(srcDisp, dstReg);
    if (lister != null) lister.RRA(miStart, "CVTTSD2SI", dstReg, srcDisp);
  }

  // dstReg <<=  [srcBase + srcIndex<<scale + srcDisp]
  public final void emitCVTTSD2SI_Reg_RegIdx(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x2C);
    emitSIBRegOperands(srcBase, srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRXD(miStart, "CVTTSD2SI", dstReg, srcBase, srcIndex, scale, srcDisp);
  }

  // dstReg <<=  [srcReg]
  public final void emitCVTTSD2SI_Reg_RegInd(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x2C);
    emitRegIndirectRegOperands(srcReg, dstReg);
    if (lister != null) lister.RRN(miStart, "CVTTSD2SI", dstReg, srcReg);
  }
  
  
  // dstReg <<=  srcReg
  public final void emitUCOMISD_Reg_Reg(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x2E);
    emitRegRegOperands(srcReg, dstReg);
    if (lister != null) lister.RR(miStart, "UCOMISD", dstReg, srcReg);
  }

  // dstReg <<=  [srcReg + srcDisp]
  public final void emitUCOMISD_Reg_RegDisp(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x2E);
    emitRegDispRegOperands(srcReg, disp, dstReg);
    if (lister != null) lister.RRD(miStart, "UCOMISD", dstReg, srcReg, disp);
  }

  // dstReg <<=  [srcIndex<<scale + srcDisp]
  public final void emitUCOMISD_Reg_RegOff(byte dstReg, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x2E);
    emitRegOffRegOperands(srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRFD(miStart, "UCOMISD", dstReg, srcIndex, scale, srcDisp);
  }

  // dstReg <<=  [srcDisp]
  public final void emitUCOMISD_Reg_Abs(byte dstReg, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x2E);
    emitAbsRegOperands(srcDisp, dstReg);
    if (lister != null) lister.RRA(miStart, "UCOMISD", dstReg, srcDisp);
  }

  // dstReg <<=  [srcBase + srcIndex<<scale + srcDisp]
  public final void emitUCOMISD_Reg_RegIdx(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x2E);
    emitSIBRegOperands(srcBase, srcIndex, scale, srcDisp, dstReg);
    if (lister != null) lister.RRXD(miStart, "UCOMISD", dstReg, srcBase, srcIndex, scale, srcDisp);
  }

  // dstReg <<=  [srcReg]
  public final void emitUCOMISD_Reg_RegInd(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x66);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0x2E);
    emitRegIndirectRegOperands(srcReg, dstReg);
    if (lister != null) lister.RRN(miStart, "UCOMISD", dstReg, srcReg);
  }
  
  
  // dstReg <<=  srcReg
  public final void emitCMPEQSD_Reg_Reg(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitRegRegOperands(srcReg, dstReg);
    setMachineCodes(mi++, (byte) 0);
    if (lister != null) lister.RR(miStart, "CMPEQSD", dstReg, srcReg);
  }

  // dstReg <<=  [srcReg + srcDisp]
  public final void emitCMPEQSD_Reg_RegDisp(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitRegDispRegOperands(srcReg, disp, dstReg);
    setMachineCodes(mi++, (byte) 0);
    if (lister != null) lister.RRD(miStart, "CMPEQSD", dstReg, srcReg, disp);
  }

  // dstReg <<=  [srcIndex<<scale + srcDisp]
  public final void emitCMPEQSD_Reg_RegOff(byte dstReg, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitRegOffRegOperands(srcIndex, scale, srcDisp, dstReg);
    setMachineCodes(mi++, (byte) 0);
    if (lister != null) lister.RRFD(miStart, "CMPEQSD", dstReg, srcIndex, scale, srcDisp);
  }

  // dstReg <<=  [srcDisp]
  public final void emitCMPEQSD_Reg_Abs(byte dstReg, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitAbsRegOperands(srcDisp, dstReg);
    setMachineCodes(mi++, (byte) 0);
    if (lister != null) lister.RRA(miStart, "CMPEQSD", dstReg, srcDisp);
  }

  // dstReg <<=  [srcBase + srcIndex<<scale + srcDisp]
  public final void emitCMPEQSD_Reg_RegIdx(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitSIBRegOperands(srcBase, srcIndex, scale, srcDisp, dstReg);
    setMachineCodes(mi++, (byte) 0);
    if (lister != null) lister.RRXD(miStart, "CMPEQSD", dstReg, srcBase, srcIndex, scale, srcDisp);
  }

  // dstReg <<=  [srcReg]
  public final void emitCMPEQSD_Reg_RegInd(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitRegIndirectRegOperands(srcReg, dstReg);
    setMachineCodes(mi++, (byte) 0);
    if (lister != null) lister.RRN(miStart, "CMPEQSD", dstReg, srcReg);
  }
  
  
  // dstReg <<=  srcReg
  public final void emitCMPLTSD_Reg_Reg(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitRegRegOperands(srcReg, dstReg);
    setMachineCodes(mi++, (byte) 1);
    if (lister != null) lister.RR(miStart, "CMPLTSD", dstReg, srcReg);
  }

  // dstReg <<=  [srcReg + srcDisp]
  public final void emitCMPLTSD_Reg_RegDisp(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitRegDispRegOperands(srcReg, disp, dstReg);
    setMachineCodes(mi++, (byte) 1);
    if (lister != null) lister.RRD(miStart, "CMPLTSD", dstReg, srcReg, disp);
  }

  // dstReg <<=  [srcIndex<<scale + srcDisp]
  public final void emitCMPLTSD_Reg_RegOff(byte dstReg, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitRegOffRegOperands(srcIndex, scale, srcDisp, dstReg);
    setMachineCodes(mi++, (byte) 1);
    if (lister != null) lister.RRFD(miStart, "CMPLTSD", dstReg, srcIndex, scale, srcDisp);
  }

  // dstReg <<=  [srcDisp]
  public final void emitCMPLTSD_Reg_Abs(byte dstReg, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitAbsRegOperands(srcDisp, dstReg);
    setMachineCodes(mi++, (byte) 1);
    if (lister != null) lister.RRA(miStart, "CMPLTSD", dstReg, srcDisp);
  }

  // dstReg <<=  [srcBase + srcIndex<<scale + srcDisp]
  public final void emitCMPLTSD_Reg_RegIdx(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitSIBRegOperands(srcBase, srcIndex, scale, srcDisp, dstReg);
    setMachineCodes(mi++, (byte) 1);
    if (lister != null) lister.RRXD(miStart, "CMPLTSD", dstReg, srcBase, srcIndex, scale, srcDisp);
  }

  // dstReg <<=  [srcReg]
  public final void emitCMPLTSD_Reg_RegInd(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitRegIndirectRegOperands(srcReg, dstReg);
    setMachineCodes(mi++, (byte) 1);
    if (lister != null) lister.RRN(miStart, "CMPLTSD", dstReg, srcReg);
  }
  
  
  // dstReg <<=  srcReg
  public final void emitCMPLESD_Reg_Reg(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitRegRegOperands(srcReg, dstReg);
    setMachineCodes(mi++, (byte) 2);
    if (lister != null) lister.RR(miStart, "CMPLESD", dstReg, srcReg);
  }

  // dstReg <<=  [srcReg + srcDisp]
  public final void emitCMPLESD_Reg_RegDisp(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitRegDispRegOperands(srcReg, disp, dstReg);
    setMachineCodes(mi++, (byte) 2);
    if (lister != null) lister.RRD(miStart, "CMPLESD", dstReg, srcReg, disp);
  }

  // dstReg <<=  [srcIndex<<scale + srcDisp]
  public final void emitCMPLESD_Reg_RegOff(byte dstReg, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitRegOffRegOperands(srcIndex, scale, srcDisp, dstReg);
    setMachineCodes(mi++, (byte) 2);
    if (lister != null) lister.RRFD(miStart, "CMPLESD", dstReg, srcIndex, scale, srcDisp);
  }

  // dstReg <<=  [srcDisp]
  public final void emitCMPLESD_Reg_Abs(byte dstReg, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitAbsRegOperands(srcDisp, dstReg);
    setMachineCodes(mi++, (byte) 2);
    if (lister != null) lister.RRA(miStart, "CMPLESD", dstReg, srcDisp);
  }

  // dstReg <<=  [srcBase + srcIndex<<scale + srcDisp]
  public final void emitCMPLESD_Reg_RegIdx(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitSIBRegOperands(srcBase, srcIndex, scale, srcDisp, dstReg);
    setMachineCodes(mi++, (byte) 2);
    if (lister != null) lister.RRXD(miStart, "CMPLESD", dstReg, srcBase, srcIndex, scale, srcDisp);
  }

  // dstReg <<=  [srcReg]
  public final void emitCMPLESD_Reg_RegInd(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitRegIndirectRegOperands(srcReg, dstReg);
    setMachineCodes(mi++, (byte) 2);
    if (lister != null) lister.RRN(miStart, "CMPLESD", dstReg, srcReg);
  }
  
  
  // dstReg <<=  srcReg
  public final void emitCMPUNORDSD_Reg_Reg(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitRegRegOperands(srcReg, dstReg);
    setMachineCodes(mi++, (byte) 3);
    if (lister != null) lister.RR(miStart, "CMPUNORDSD", dstReg, srcReg);
  }

  // dstReg <<=  [srcReg + srcDisp]
  public final void emitCMPUNORDSD_Reg_RegDisp(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitRegDispRegOperands(srcReg, disp, dstReg);
    setMachineCodes(mi++, (byte) 3);
    if (lister != null) lister.RRD(miStart, "CMPUNORDSD", dstReg, srcReg, disp);
  }

  // dstReg <<=  [srcIndex<<scale + srcDisp]
  public final void emitCMPUNORDSD_Reg_RegOff(byte dstReg, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitRegOffRegOperands(srcIndex, scale, srcDisp, dstReg);
    setMachineCodes(mi++, (byte) 3);
    if (lister != null) lister.RRFD(miStart, "CMPUNORDSD", dstReg, srcIndex, scale, srcDisp);
  }

  // dstReg <<=  [srcDisp]
  public final void emitCMPUNORDSD_Reg_Abs(byte dstReg, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitAbsRegOperands(srcDisp, dstReg);
    setMachineCodes(mi++, (byte) 3);
    if (lister != null) lister.RRA(miStart, "CMPUNORDSD", dstReg, srcDisp);
  }

  // dstReg <<=  [srcBase + srcIndex<<scale + srcDisp]
  public final void emitCMPUNORDSD_Reg_RegIdx(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitSIBRegOperands(srcBase, srcIndex, scale, srcDisp, dstReg);
    setMachineCodes(mi++, (byte) 3);
    if (lister != null) lister.RRXD(miStart, "CMPUNORDSD", dstReg, srcBase, srcIndex, scale, srcDisp);
  }

  // dstReg <<=  [srcReg]
  public final void emitCMPUNORDSD_Reg_RegInd(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitRegIndirectRegOperands(srcReg, dstReg);
    setMachineCodes(mi++, (byte) 3);
    if (lister != null) lister.RRN(miStart, "CMPUNORDSD", dstReg, srcReg);
  }
  
  
  // dstReg <<=  srcReg
  public final void emitCMPNESD_Reg_Reg(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitRegRegOperands(srcReg, dstReg);
    setMachineCodes(mi++, (byte) 4);
    if (lister != null) lister.RR(miStart, "CMPNESD", dstReg, srcReg);
  }

  // dstReg <<=  [srcReg + srcDisp]
  public final void emitCMPNESD_Reg_RegDisp(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitRegDispRegOperands(srcReg, disp, dstReg);
    setMachineCodes(mi++, (byte) 4);
    if (lister != null) lister.RRD(miStart, "CMPNESD", dstReg, srcReg, disp);
  }

  // dstReg <<=  [srcIndex<<scale + srcDisp]
  public final void emitCMPNESD_Reg_RegOff(byte dstReg, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitRegOffRegOperands(srcIndex, scale, srcDisp, dstReg);
    setMachineCodes(mi++, (byte) 4);
    if (lister != null) lister.RRFD(miStart, "CMPNESD", dstReg, srcIndex, scale, srcDisp);
  }

  // dstReg <<=  [srcDisp]
  public final void emitCMPNESD_Reg_Abs(byte dstReg, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitAbsRegOperands(srcDisp, dstReg);
    setMachineCodes(mi++, (byte) 4);
    if (lister != null) lister.RRA(miStart, "CMPNESD", dstReg, srcDisp);
  }

  // dstReg <<=  [srcBase + srcIndex<<scale + srcDisp]
  public final void emitCMPNESD_Reg_RegIdx(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitSIBRegOperands(srcBase, srcIndex, scale, srcDisp, dstReg);
    setMachineCodes(mi++, (byte) 4);
    if (lister != null) lister.RRXD(miStart, "CMPNESD", dstReg, srcBase, srcIndex, scale, srcDisp);
  }

  // dstReg <<=  [srcReg]
  public final void emitCMPNESD_Reg_RegInd(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitRegIndirectRegOperands(srcReg, dstReg);
    setMachineCodes(mi++, (byte) 4);
    if (lister != null) lister.RRN(miStart, "CMPNESD", dstReg, srcReg);
  }
  
  
  // dstReg <<=  srcReg
  public final void emitCMPNLTSD_Reg_Reg(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitRegRegOperands(srcReg, dstReg);
    setMachineCodes(mi++, (byte) 5);
    if (lister != null) lister.RR(miStart, "CMPNLTSD", dstReg, srcReg);
  }

  // dstReg <<=  [srcReg + srcDisp]
  public final void emitCMPNLTSD_Reg_RegDisp(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitRegDispRegOperands(srcReg, disp, dstReg);
    setMachineCodes(mi++, (byte) 5);
    if (lister != null) lister.RRD(miStart, "CMPNLTSD", dstReg, srcReg, disp);
  }

  // dstReg <<=  [srcIndex<<scale + srcDisp]
  public final void emitCMPNLTSD_Reg_RegOff(byte dstReg, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitRegOffRegOperands(srcIndex, scale, srcDisp, dstReg);
    setMachineCodes(mi++, (byte) 5);
    if (lister != null) lister.RRFD(miStart, "CMPNLTSD", dstReg, srcIndex, scale, srcDisp);
  }

  // dstReg <<=  [srcDisp]
  public final void emitCMPNLTSD_Reg_Abs(byte dstReg, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitAbsRegOperands(srcDisp, dstReg);
    setMachineCodes(mi++, (byte) 5);
    if (lister != null) lister.RRA(miStart, "CMPNLTSD", dstReg, srcDisp);
  }

  // dstReg <<=  [srcBase + srcIndex<<scale + srcDisp]
  public final void emitCMPNLTSD_Reg_RegIdx(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitSIBRegOperands(srcBase, srcIndex, scale, srcDisp, dstReg);
    setMachineCodes(mi++, (byte) 5);
    if (lister != null) lister.RRXD(miStart, "CMPNLTSD", dstReg, srcBase, srcIndex, scale, srcDisp);
  }

  // dstReg <<=  [srcReg]
  public final void emitCMPNLTSD_Reg_RegInd(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitRegIndirectRegOperands(srcReg, dstReg);
    setMachineCodes(mi++, (byte) 5);
    if (lister != null) lister.RRN(miStart, "CMPNLTSD", dstReg, srcReg);
  }
  
  
  // dstReg <<=  srcReg
  public final void emitCMPNLESD_Reg_Reg(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitRegRegOperands(srcReg, dstReg);
    setMachineCodes(mi++, (byte) 6);
    if (lister != null) lister.RR(miStart, "CMPNLESD", dstReg, srcReg);
  }

  // dstReg <<=  [srcReg + srcDisp]
  public final void emitCMPNLESD_Reg_RegDisp(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitRegDispRegOperands(srcReg, disp, dstReg);
    setMachineCodes(mi++, (byte) 6);
    if (lister != null) lister.RRD(miStart, "CMPNLESD", dstReg, srcReg, disp);
  }

  // dstReg <<=  [srcIndex<<scale + srcDisp]
  public final void emitCMPNLESD_Reg_RegOff(byte dstReg, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitRegOffRegOperands(srcIndex, scale, srcDisp, dstReg);
    setMachineCodes(mi++, (byte) 6);
    if (lister != null) lister.RRFD(miStart, "CMPNLESD", dstReg, srcIndex, scale, srcDisp);
  }

  // dstReg <<=  [srcDisp]
  public final void emitCMPNLESD_Reg_Abs(byte dstReg, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitAbsRegOperands(srcDisp, dstReg);
    setMachineCodes(mi++, (byte) 6);
    if (lister != null) lister.RRA(miStart, "CMPNLESD", dstReg, srcDisp);
  }

  // dstReg <<=  [srcBase + srcIndex<<scale + srcDisp]
  public final void emitCMPNLESD_Reg_RegIdx(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitSIBRegOperands(srcBase, srcIndex, scale, srcDisp, dstReg);
    setMachineCodes(mi++, (byte) 6);
    if (lister != null) lister.RRXD(miStart, "CMPNLESD", dstReg, srcBase, srcIndex, scale, srcDisp);
  }

  // dstReg <<=  [srcReg]
  public final void emitCMPNLESD_Reg_RegInd(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitRegIndirectRegOperands(srcReg, dstReg);
    setMachineCodes(mi++, (byte) 6);
    if (lister != null) lister.RRN(miStart, "CMPNLESD", dstReg, srcReg);
  }
  
  
  // dstReg <<=  srcReg
  public final void emitCMPORDSD_Reg_Reg(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitRegRegOperands(srcReg, dstReg);
    setMachineCodes(mi++, (byte) 7);
    if (lister != null) lister.RR(miStart, "CMPORDSD", dstReg, srcReg);
  }

  // dstReg <<=  [srcReg + srcDisp]
  public final void emitCMPORDSD_Reg_RegDisp(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitRegDispRegOperands(srcReg, disp, dstReg);
    setMachineCodes(mi++, (byte) 7);
    if (lister != null) lister.RRD(miStart, "CMPORDSD", dstReg, srcReg, disp);
  }

  // dstReg <<=  [srcIndex<<scale + srcDisp]
  public final void emitCMPORDSD_Reg_RegOff(byte dstReg, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitRegOffRegOperands(srcIndex, scale, srcDisp, dstReg);
    setMachineCodes(mi++, (byte) 7);
    if (lister != null) lister.RRFD(miStart, "CMPORDSD", dstReg, srcIndex, scale, srcDisp);
  }

  // dstReg <<=  [srcDisp]
  public final void emitCMPORDSD_Reg_Abs(byte dstReg, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitAbsRegOperands(srcDisp, dstReg);
    setMachineCodes(mi++, (byte) 7);
    if (lister != null) lister.RRA(miStart, "CMPORDSD", dstReg, srcDisp);
  }

  // dstReg <<=  [srcBase + srcIndex<<scale + srcDisp]
  public final void emitCMPORDSD_Reg_RegIdx(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset srcDisp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitSIBRegOperands(srcBase, srcIndex, scale, srcDisp, dstReg);
    setMachineCodes(mi++, (byte) 7);
    if (lister != null) lister.RRXD(miStart, "CMPORDSD", dstReg, srcBase, srcIndex, scale, srcDisp);
  }

  // dstReg <<=  [srcReg]
  public final void emitCMPORDSD_Reg_RegInd(byte dstReg, byte srcReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xF2);
    setMachineCodes(mi++, (byte) 0x0F);
    setMachineCodes(mi++, (byte) 0xC2);
    emitRegIndirectRegOperands(srcReg, dstReg);
    setMachineCodes(mi++, (byte) 7);
    if (lister != null) lister.RRN(miStart, "CMPORDSD", dstReg, srcReg);
  }
  
  // dstReg += () [srcReg + disp]
  public final void emitFADD_Reg_RegDisp(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xD8);
    // The register'' 0 is really part of the opcode
    emitRegDispRegOperands(srcReg, disp, (byte) 0);
    if (lister != null) lister.RRD(miStart, "FADD", dstReg, srcReg, disp);
  }

  // dstReg += () [srcReg]
  public final void emitFADD_Reg_RegInd(byte dstReg, byte srcReg) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xD8);
    // The register'' 0 is really part of the opcode
    emitRegIndirectRegOperands(srcReg, (byte) 0);
    if (lister != null) lister.RRN(miStart, "FADD", dstReg, srcReg);
  }

  // dstReg += () [srcBase + srcIndex<<scale + disp]
  public final void emitFADD_Reg_RegIdx(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xD8);
    // The register'' 0 is really part of the opcode
    emitSIBRegOperands(srcBase, srcIndex, scale, disp, (byte) 0);
    if (lister != null) lister.RRXD(miStart, "FADD", dstReg, srcBase, srcIndex, scale, disp);
  }

  // dstReg += () [srcIndex<<scale + disp]
  public final void emitFADD_Reg_RegOff(byte dstReg, byte srcIndex, short scale, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xD8);
    // The register'' 0 is really part of the opcode
    emitRegOffRegOperands(srcIndex, scale, disp, (byte) 0);
    if (lister != null) lister.RRFD(miStart, "FADD", dstReg, srcIndex, scale, disp);
  }

  // dstReg += () [disp]
  public final void emitFADD_Reg_Abs(byte dstReg, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xD8);
    // The register'' 0 is really part of the opcode
    emitAbsRegOperands(disp, (byte) 0);
    if (lister != null) lister.RRA(miStart, "FADD", dstReg, disp);
  }

  // dstReg += (quad) [srcReg + disp]
  public final void emitFADD_Reg_RegDisp_Quad(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDC);
    // The register'' 0 is really part of the opcode
    emitRegDispRegOperands(srcReg, disp, (byte) 0);
    if (lister != null) lister.RRD(miStart, "FADD", dstReg, srcReg, disp);
  }

  // dstReg += (quad) [srcReg]
  public final void emitFADD_Reg_RegInd_Quad(byte dstReg, byte srcReg) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDC);
    // The register'' 0 is really part of the opcode
    emitRegIndirectRegOperands(srcReg, (byte) 0);
    if (lister != null) lister.RRN(miStart, "FADD", dstReg, srcReg);
  }

  // dstReg += (quad) [srcBase + srcIndex<<scale + disp]
  public final void emitFADD_Reg_RegIdx_Quad(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDC);
    // The register'' 0 is really part of the opcode
    emitSIBRegOperands(srcBase, srcIndex, scale, disp, (byte) 0);
    if (lister != null) lister.RRXD(miStart, "FADD", dstReg, srcBase, srcIndex, scale, disp);
  }

  // dstReg += (quad) [srcIndex<<scale + disp]
  public final void emitFADD_Reg_RegOff_Quad(byte dstReg, byte srcIndex, short scale, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDC);
    // The register'' 0 is really part of the opcode
    emitRegOffRegOperands(srcIndex, scale, disp, (byte) 0);
    if (lister != null) lister.RRFD(miStart, "FADD", dstReg, srcIndex, scale, disp);
  }

  // dstReg += (quad) [disp]
  public final void emitFADD_Reg_Abs_Quad(byte dstReg, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDC);
    // The register'' 0 is really part of the opcode
    emitAbsRegOperands(disp, (byte) 0);
    if (lister != null) lister.RRA(miStart, "FADD", dstReg, disp);
  }

  // dstReg += () [srcReg + disp]
  public final void emitFIADD_Reg_RegDisp(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDA);
    // The register'' 0 is really part of the opcode
    emitRegDispRegOperands(srcReg, disp, (byte) 0);
    if (lister != null) lister.RRD(miStart, "FIADD", dstReg, srcReg, disp);
  }

  // dstReg += () [srcReg]
  public final void emitFIADD_Reg_RegInd(byte dstReg, byte srcReg) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDA);
    // The register'' 0 is really part of the opcode
    emitRegIndirectRegOperands(srcReg, (byte) 0);
    if (lister != null) lister.RRN(miStart, "FIADD", dstReg, srcReg);
  }

  // dstReg += () [srcBase + srcIndex<<scale + disp]
  public final void emitFIADD_Reg_RegIdx(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDA);
    // The register'' 0 is really part of the opcode
    emitSIBRegOperands(srcBase, srcIndex, scale, disp, (byte) 0);
    if (lister != null) lister.RRXD(miStart, "FIADD", dstReg, srcBase, srcIndex, scale, disp);
  }

  // dstReg += () [srcIndex<<scale + disp]
  public final void emitFIADD_Reg_RegOff(byte dstReg, byte srcIndex, short scale, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDA);
    // The register'' 0 is really part of the opcode
    emitRegOffRegOperands(srcIndex, scale, disp, (byte) 0);
    if (lister != null) lister.RRFD(miStart, "FIADD", dstReg, srcIndex, scale, disp);
  }

  // dstReg += () [disp]
  public final void emitFIADD_Reg_Abs(byte dstReg, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDA);
    // The register'' 0 is really part of the opcode
    emitAbsRegOperands(disp, (byte) 0);
    if (lister != null) lister.RRA(miStart, "FIADD", dstReg, disp);
  }

  // dstReg += (word) [srcReg + disp]
  public final void emitFIADD_Reg_RegDisp_Word(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDE);
    // The register'' 0 is really part of the opcode
    emitRegDispRegOperands(srcReg, disp, (byte) 0);
    if (lister != null) lister.RRD(miStart, "FIADD", dstReg, srcReg, disp);
  }

  // dstReg += (word) [srcReg]
  public final void emitFIADD_Reg_RegInd_Word(byte dstReg, byte srcReg) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDE);
    // The register'' 0 is really part of the opcode
    emitRegIndirectRegOperands(srcReg, (byte) 0);
    if (lister != null) lister.RRN(miStart, "FIADD", dstReg, srcReg);
  }

  // dstReg += (word) [srcBase + srcIndex<<scale + disp]
  public final void emitFIADD_Reg_RegIdx_Word(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDE);
    // The register'' 0 is really part of the opcode
    emitSIBRegOperands(srcBase, srcIndex, scale, disp, (byte) 0);
    if (lister != null) lister.RRXD(miStart, "FIADD", dstReg, srcBase, srcIndex, scale, disp);
  }

  // dstReg += (word) [srcIndex<<scale + disp]
  public final void emitFIADD_Reg_RegOff_Word(byte dstReg, byte srcIndex, short scale, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDE);
    // The register'' 0 is really part of the opcode
    emitRegOffRegOperands(srcIndex, scale, disp, (byte) 0);
    if (lister != null) lister.RRFD(miStart, "FIADD", dstReg, srcIndex, scale, disp);
  }

  // dstReg += (word) [disp]
  public final void emitFIADD_Reg_Abs_Word(byte dstReg, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDE);
    // The register'' 0 is really part of the opcode
    emitAbsRegOperands(disp, (byte) 0);
    if (lister != null) lister.RRA(miStart, "FIADD", dstReg, disp);
  }

  // dstReg += srcReg
  public final void emitFADD_Reg_Reg(byte dstReg, byte srcReg) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(srcReg == FP0 || dstReg == FP0);
    if (dstReg == FP0) {
    setMachineCodes(mi++, (byte) 0xD8);
    setMachineCodes(mi++, (byte) (0xC0 | srcReg));
    } else if (srcReg == FP0) {
    setMachineCodes(mi++, (byte) 0xDC);
    setMachineCodes(mi++, (byte) (0xC0 | dstReg));
    }
    if (lister != null) lister.RR(miStart, "FADD", dstReg, srcReg);
  }

  // srcReg += ST(0); pop stack
  public final void emitFADDP_Reg_Reg(byte dstReg, byte srcReg) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(srcReg == FP0);
    setMachineCodes(mi++, (byte) 0xDE);
    setMachineCodes(mi++, (byte) (0xC0 | dstReg));
    if (lister != null) lister.R(miStart, "FADDP", dstReg);
  }

  // dstReg /= () [srcReg + disp]
  public final void emitFDIV_Reg_RegDisp(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xD8);
    // The register'' 6 is really part of the opcode
    emitRegDispRegOperands(srcReg, disp, (byte) 6);
    if (lister != null) lister.RRD(miStart, "FDIV", dstReg, srcReg, disp);
  }

  // dstReg /= () [srcReg]
  public final void emitFDIV_Reg_RegInd(byte dstReg, byte srcReg) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xD8);
    // The register'' 6 is really part of the opcode
    emitRegIndirectRegOperands(srcReg, (byte) 6);
    if (lister != null) lister.RRN(miStart, "FDIV", dstReg, srcReg);
  }

  // dstReg /= () [srcBase + srcIndex<<scale + disp]
  public final void emitFDIV_Reg_RegIdx(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xD8);
    // The register'' 6 is really part of the opcode
    emitSIBRegOperands(srcBase, srcIndex, scale, disp, (byte) 6);
    if (lister != null) lister.RRXD(miStart, "FDIV", dstReg, srcBase, srcIndex, scale, disp);
  }

  // dstReg /= () [srcIndex<<scale + disp]
  public final void emitFDIV_Reg_RegOff(byte dstReg, byte srcIndex, short scale, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xD8);
    // The register'' 6 is really part of the opcode
    emitRegOffRegOperands(srcIndex, scale, disp, (byte) 6);
    if (lister != null) lister.RRFD(miStart, "FDIV", dstReg, srcIndex, scale, disp);
  }

  // dstReg /= () [disp]
  public final void emitFDIV_Reg_Abs(byte dstReg, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xD8);
    // The register'' 6 is really part of the opcode
    emitAbsRegOperands(disp, (byte) 6);
    if (lister != null) lister.RRA(miStart, "FDIV", dstReg, disp);
  }

  // dstReg /= (quad) [srcReg + disp]
  public final void emitFDIV_Reg_RegDisp_Quad(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDC);
    // The register'' 6 is really part of the opcode
    emitRegDispRegOperands(srcReg, disp, (byte) 6);
    if (lister != null) lister.RRD(miStart, "FDIV", dstReg, srcReg, disp);
  }

  // dstReg /= (quad) [srcReg]
  public final void emitFDIV_Reg_RegInd_Quad(byte dstReg, byte srcReg) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDC);
    // The register'' 6 is really part of the opcode
    emitRegIndirectRegOperands(srcReg, (byte) 6);
    if (lister != null) lister.RRN(miStart, "FDIV", dstReg, srcReg);
  }

  // dstReg /= (quad) [srcBase + srcIndex<<scale + disp]
  public final void emitFDIV_Reg_RegIdx_Quad(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDC);
    // The register'' 6 is really part of the opcode
    emitSIBRegOperands(srcBase, srcIndex, scale, disp, (byte) 6);
    if (lister != null) lister.RRXD(miStart, "FDIV", dstReg, srcBase, srcIndex, scale, disp);
  }

  // dstReg /= (quad) [srcIndex<<scale + disp]
  public final void emitFDIV_Reg_RegOff_Quad(byte dstReg, byte srcIndex, short scale, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDC);
    // The register'' 6 is really part of the opcode
    emitRegOffRegOperands(srcIndex, scale, disp, (byte) 6);
    if (lister != null) lister.RRFD(miStart, "FDIV", dstReg, srcIndex, scale, disp);
  }

  // dstReg /= (quad) [disp]
  public final void emitFDIV_Reg_Abs_Quad(byte dstReg, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDC);
    // The register'' 6 is really part of the opcode
    emitAbsRegOperands(disp, (byte) 6);
    if (lister != null) lister.RRA(miStart, "FDIV", dstReg, disp);
  }

  // dstReg /= () [srcReg + disp]
  public final void emitFIDIV_Reg_RegDisp(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDA);
    // The register'' 6 is really part of the opcode
    emitRegDispRegOperands(srcReg, disp, (byte) 6);
    if (lister != null) lister.RRD(miStart, "FIDIV", dstReg, srcReg, disp);
  }

  // dstReg /= () [srcReg]
  public final void emitFIDIV_Reg_RegInd(byte dstReg, byte srcReg) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDA);
    // The register'' 6 is really part of the opcode
    emitRegIndirectRegOperands(srcReg, (byte) 6);
    if (lister != null) lister.RRN(miStart, "FIDIV", dstReg, srcReg);
  }

  // dstReg /= () [srcBase + srcIndex<<scale + disp]
  public final void emitFIDIV_Reg_RegIdx(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDA);
    // The register'' 6 is really part of the opcode
    emitSIBRegOperands(srcBase, srcIndex, scale, disp, (byte) 6);
    if (lister != null) lister.RRXD(miStart, "FIDIV", dstReg, srcBase, srcIndex, scale, disp);
  }

  // dstReg /= () [srcIndex<<scale + disp]
  public final void emitFIDIV_Reg_RegOff(byte dstReg, byte srcIndex, short scale, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDA);
    // The register'' 6 is really part of the opcode
    emitRegOffRegOperands(srcIndex, scale, disp, (byte) 6);
    if (lister != null) lister.RRFD(miStart, "FIDIV", dstReg, srcIndex, scale, disp);
  }

  // dstReg /= () [disp]
  public final void emitFIDIV_Reg_Abs(byte dstReg, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDA);
    // The register'' 6 is really part of the opcode
    emitAbsRegOperands(disp, (byte) 6);
    if (lister != null) lister.RRA(miStart, "FIDIV", dstReg, disp);
  }

  // dstReg /= (word) [srcReg + disp]
  public final void emitFIDIV_Reg_RegDisp_Word(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDE);
    // The register'' 6 is really part of the opcode
    emitRegDispRegOperands(srcReg, disp, (byte) 6);
    if (lister != null) lister.RRD(miStart, "FIDIV", dstReg, srcReg, disp);
  }

  // dstReg /= (word) [srcReg]
  public final void emitFIDIV_Reg_RegInd_Word(byte dstReg, byte srcReg) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDE);
    // The register'' 6 is really part of the opcode
    emitRegIndirectRegOperands(srcReg, (byte) 6);
    if (lister != null) lister.RRN(miStart, "FIDIV", dstReg, srcReg);
  }

  // dstReg /= (word) [srcBase + srcIndex<<scale + disp]
  public final void emitFIDIV_Reg_RegIdx_Word(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDE);
    // The register'' 6 is really part of the opcode
    emitSIBRegOperands(srcBase, srcIndex, scale, disp, (byte) 6);
    if (lister != null) lister.RRXD(miStart, "FIDIV", dstReg, srcBase, srcIndex, scale, disp);
  }

  // dstReg /= (word) [srcIndex<<scale + disp]
  public final void emitFIDIV_Reg_RegOff_Word(byte dstReg, byte srcIndex, short scale, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDE);
    // The register'' 6 is really part of the opcode
    emitRegOffRegOperands(srcIndex, scale, disp, (byte) 6);
    if (lister != null) lister.RRFD(miStart, "FIDIV", dstReg, srcIndex, scale, disp);
  }

  // dstReg /= (word) [disp]
  public final void emitFIDIV_Reg_Abs_Word(byte dstReg, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDE);
    // The register'' 6 is really part of the opcode
    emitAbsRegOperands(disp, (byte) 6);
    if (lister != null) lister.RRA(miStart, "FIDIV", dstReg, disp);
  }

  // dstReg /= srcReg
  public final void emitFDIV_Reg_Reg(byte dstReg, byte srcReg) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(srcReg == FP0 || dstReg == FP0);
    if (dstReg == FP0) {
    setMachineCodes(mi++, (byte) 0xD8);
    setMachineCodes(mi++, (byte) (0xF0 | srcReg));
    } else if (srcReg == FP0) {
    setMachineCodes(mi++, (byte) 0xDC);
    setMachineCodes(mi++, (byte) (0xF8 | dstReg));
    }
    if (lister != null) lister.RR(miStart, "FDIV", dstReg, srcReg);
  }

  // srcReg /= ST(0); pop stack
  public final void emitFDIVP_Reg_Reg(byte dstReg, byte srcReg) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(srcReg == FP0);
    setMachineCodes(mi++, (byte) 0xDE);
    setMachineCodes(mi++, (byte) (0xF8 | dstReg));
    if (lister != null) lister.R(miStart, "FDIVP", dstReg);
  }

  // dstReg /= () [srcReg + disp]
  public final void emitFDIVR_Reg_RegDisp(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xD8);
    // The register'' 7 is really part of the opcode
    emitRegDispRegOperands(srcReg, disp, (byte) 7);
    if (lister != null) lister.RRD(miStart, "FDIVR", dstReg, srcReg, disp);
  }

  // dstReg /= () [srcReg]
  public final void emitFDIVR_Reg_RegInd(byte dstReg, byte srcReg) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xD8);
    // The register'' 7 is really part of the opcode
    emitRegIndirectRegOperands(srcReg, (byte) 7);
    if (lister != null) lister.RRN(miStart, "FDIVR", dstReg, srcReg);
  }

  // dstReg /= () [srcBase + srcIndex<<scale + disp]
  public final void emitFDIVR_Reg_RegIdx(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xD8);
    // The register'' 7 is really part of the opcode
    emitSIBRegOperands(srcBase, srcIndex, scale, disp, (byte) 7);
    if (lister != null) lister.RRXD(miStart, "FDIVR", dstReg, srcBase, srcIndex, scale, disp);
  }

  // dstReg /= () [srcIndex<<scale + disp]
  public final void emitFDIVR_Reg_RegOff(byte dstReg, byte srcIndex, short scale, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xD8);
    // The register'' 7 is really part of the opcode
    emitRegOffRegOperands(srcIndex, scale, disp, (byte) 7);
    if (lister != null) lister.RRFD(miStart, "FDIVR", dstReg, srcIndex, scale, disp);
  }

  // dstReg /= () [disp]
  public final void emitFDIVR_Reg_Abs(byte dstReg, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xD8);
    // The register'' 7 is really part of the opcode
    emitAbsRegOperands(disp, (byte) 7);
    if (lister != null) lister.RRA(miStart, "FDIVR", dstReg, disp);
  }

  // dstReg /= (quad) [srcReg + disp]
  public final void emitFDIVR_Reg_RegDisp_Quad(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDC);
    // The register'' 7 is really part of the opcode
    emitRegDispRegOperands(srcReg, disp, (byte) 7);
    if (lister != null) lister.RRD(miStart, "FDIVR", dstReg, srcReg, disp);
  }

  // dstReg /= (quad) [srcReg]
  public final void emitFDIVR_Reg_RegInd_Quad(byte dstReg, byte srcReg) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDC);
    // The register'' 7 is really part of the opcode
    emitRegIndirectRegOperands(srcReg, (byte) 7);
    if (lister != null) lister.RRN(miStart, "FDIVR", dstReg, srcReg);
  }

  // dstReg /= (quad) [srcBase + srcIndex<<scale + disp]
  public final void emitFDIVR_Reg_RegIdx_Quad(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDC);
    // The register'' 7 is really part of the opcode
    emitSIBRegOperands(srcBase, srcIndex, scale, disp, (byte) 7);
    if (lister != null) lister.RRXD(miStart, "FDIVR", dstReg, srcBase, srcIndex, scale, disp);
  }

  // dstReg /= (quad) [srcIndex<<scale + disp]
  public final void emitFDIVR_Reg_RegOff_Quad(byte dstReg, byte srcIndex, short scale, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDC);
    // The register'' 7 is really part of the opcode
    emitRegOffRegOperands(srcIndex, scale, disp, (byte) 7);
    if (lister != null) lister.RRFD(miStart, "FDIVR", dstReg, srcIndex, scale, disp);
  }

  // dstReg /= (quad) [disp]
  public final void emitFDIVR_Reg_Abs_Quad(byte dstReg, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDC);
    // The register'' 7 is really part of the opcode
    emitAbsRegOperands(disp, (byte) 7);
    if (lister != null) lister.RRA(miStart, "FDIVR", dstReg, disp);
  }

  // dstReg /= () [srcReg + disp]
  public final void emitFIDIVR_Reg_RegDisp(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDA);
    // The register'' 7 is really part of the opcode
    emitRegDispRegOperands(srcReg, disp, (byte) 7);
    if (lister != null) lister.RRD(miStart, "FIDIVR", dstReg, srcReg, disp);
  }

  // dstReg /= () [srcReg]
  public final void emitFIDIVR_Reg_RegInd(byte dstReg, byte srcReg) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDA);
    // The register'' 7 is really part of the opcode
    emitRegIndirectRegOperands(srcReg, (byte) 7);
    if (lister != null) lister.RRN(miStart, "FIDIVR", dstReg, srcReg);
  }

  // dstReg /= () [srcBase + srcIndex<<scale + disp]
  public final void emitFIDIVR_Reg_RegIdx(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDA);
    // The register'' 7 is really part of the opcode
    emitSIBRegOperands(srcBase, srcIndex, scale, disp, (byte) 7);
    if (lister != null) lister.RRXD(miStart, "FIDIVR", dstReg, srcBase, srcIndex, scale, disp);
  }

  // dstReg /= () [srcIndex<<scale + disp]
  public final void emitFIDIVR_Reg_RegOff(byte dstReg, byte srcIndex, short scale, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDA);
    // The register'' 7 is really part of the opcode
    emitRegOffRegOperands(srcIndex, scale, disp, (byte) 7);
    if (lister != null) lister.RRFD(miStart, "FIDIVR", dstReg, srcIndex, scale, disp);
  }

  // dstReg /= () [disp]
  public final void emitFIDIVR_Reg_Abs(byte dstReg, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDA);
    // The register'' 7 is really part of the opcode
    emitAbsRegOperands(disp, (byte) 7);
    if (lister != null) lister.RRA(miStart, "FIDIVR", dstReg, disp);
  }

  // dstReg /= (word) [srcReg + disp]
  public final void emitFIDIVR_Reg_RegDisp_Word(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDE);
    // The register'' 7 is really part of the opcode
    emitRegDispRegOperands(srcReg, disp, (byte) 7);
    if (lister != null) lister.RRD(miStart, "FIDIVR", dstReg, srcReg, disp);
  }

  // dstReg /= (word) [srcReg]
  public final void emitFIDIVR_Reg_RegInd_Word(byte dstReg, byte srcReg) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDE);
    // The register'' 7 is really part of the opcode
    emitRegIndirectRegOperands(srcReg, (byte) 7);
    if (lister != null) lister.RRN(miStart, "FIDIVR", dstReg, srcReg);
  }

  // dstReg /= (word) [srcBase + srcIndex<<scale + disp]
  public final void emitFIDIVR_Reg_RegIdx_Word(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDE);
    // The register'' 7 is really part of the opcode
    emitSIBRegOperands(srcBase, srcIndex, scale, disp, (byte) 7);
    if (lister != null) lister.RRXD(miStart, "FIDIVR", dstReg, srcBase, srcIndex, scale, disp);
  }

  // dstReg /= (word) [srcIndex<<scale + disp]
  public final void emitFIDIVR_Reg_RegOff_Word(byte dstReg, byte srcIndex, short scale, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDE);
    // The register'' 7 is really part of the opcode
    emitRegOffRegOperands(srcIndex, scale, disp, (byte) 7);
    if (lister != null) lister.RRFD(miStart, "FIDIVR", dstReg, srcIndex, scale, disp);
  }

  // dstReg /= (word) [disp]
  public final void emitFIDIVR_Reg_Abs_Word(byte dstReg, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDE);
    // The register'' 7 is really part of the opcode
    emitAbsRegOperands(disp, (byte) 7);
    if (lister != null) lister.RRA(miStart, "FIDIVR", dstReg, disp);
  }

  // dstReg /= srcReg
  public final void emitFDIVR_Reg_Reg(byte dstReg, byte srcReg) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(srcReg == FP0 || dstReg == FP0);
    if (dstReg == FP0) {
    setMachineCodes(mi++, (byte) 0xD8);
    setMachineCodes(mi++, (byte) (0xF8 | srcReg));
    } else if (srcReg == FP0) {
    setMachineCodes(mi++, (byte) 0xDC);
    setMachineCodes(mi++, (byte) (0xF0 | dstReg));
    }
    if (lister != null) lister.RR(miStart, "FDIVR", dstReg, srcReg);
  }

  // srcReg /= ST(0); pop stack
  public final void emitFDIVRP_Reg_Reg(byte dstReg, byte srcReg) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(srcReg == FP0);
    setMachineCodes(mi++, (byte) 0xDE);
    setMachineCodes(mi++, (byte) (0xF0 | dstReg));
    if (lister != null) lister.R(miStart, "FDIVRP", dstReg);
  }

  // dstReg x= () [srcReg + disp]
  public final void emitFMUL_Reg_RegDisp(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xD8);
    // The register'' 1 is really part of the opcode
    emitRegDispRegOperands(srcReg, disp, (byte) 1);
    if (lister != null) lister.RRD(miStart, "FMUL", dstReg, srcReg, disp);
  }

  // dstReg x= () [srcReg]
  public final void emitFMUL_Reg_RegInd(byte dstReg, byte srcReg) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xD8);
    // The register'' 1 is really part of the opcode
    emitRegIndirectRegOperands(srcReg, (byte) 1);
    if (lister != null) lister.RRN(miStart, "FMUL", dstReg, srcReg);
  }

  // dstReg x= () [srcBase + srcIndex<<scale + disp]
  public final void emitFMUL_Reg_RegIdx(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xD8);
    // The register'' 1 is really part of the opcode
    emitSIBRegOperands(srcBase, srcIndex, scale, disp, (byte) 1);
    if (lister != null) lister.RRXD(miStart, "FMUL", dstReg, srcBase, srcIndex, scale, disp);
  }

  // dstReg x= () [srcIndex<<scale + disp]
  public final void emitFMUL_Reg_RegOff(byte dstReg, byte srcIndex, short scale, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xD8);
    // The register'' 1 is really part of the opcode
    emitRegOffRegOperands(srcIndex, scale, disp, (byte) 1);
    if (lister != null) lister.RRFD(miStart, "FMUL", dstReg, srcIndex, scale, disp);
  }

  // dstReg x= () [disp]
  public final void emitFMUL_Reg_Abs(byte dstReg, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xD8);
    // The register'' 1 is really part of the opcode
    emitAbsRegOperands(disp, (byte) 1);
    if (lister != null) lister.RRA(miStart, "FMUL", dstReg, disp);
  }

  // dstReg x= (quad) [srcReg + disp]
  public final void emitFMUL_Reg_RegDisp_Quad(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDC);
    // The register'' 1 is really part of the opcode
    emitRegDispRegOperands(srcReg, disp, (byte) 1);
    if (lister != null) lister.RRD(miStart, "FMUL", dstReg, srcReg, disp);
  }

  // dstReg x= (quad) [srcReg]
  public final void emitFMUL_Reg_RegInd_Quad(byte dstReg, byte srcReg) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDC);
    // The register'' 1 is really part of the opcode
    emitRegIndirectRegOperands(srcReg, (byte) 1);
    if (lister != null) lister.RRN(miStart, "FMUL", dstReg, srcReg);
  }

  // dstReg x= (quad) [srcBase + srcIndex<<scale + disp]
  public final void emitFMUL_Reg_RegIdx_Quad(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDC);
    // The register'' 1 is really part of the opcode
    emitSIBRegOperands(srcBase, srcIndex, scale, disp, (byte) 1);
    if (lister != null) lister.RRXD(miStart, "FMUL", dstReg, srcBase, srcIndex, scale, disp);
  }

  // dstReg x= (quad) [srcIndex<<scale + disp]
  public final void emitFMUL_Reg_RegOff_Quad(byte dstReg, byte srcIndex, short scale, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDC);
    // The register'' 1 is really part of the opcode
    emitRegOffRegOperands(srcIndex, scale, disp, (byte) 1);
    if (lister != null) lister.RRFD(miStart, "FMUL", dstReg, srcIndex, scale, disp);
  }

  // dstReg x= (quad) [disp]
  public final void emitFMUL_Reg_Abs_Quad(byte dstReg, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDC);
    // The register'' 1 is really part of the opcode
    emitAbsRegOperands(disp, (byte) 1);
    if (lister != null) lister.RRA(miStart, "FMUL", dstReg, disp);
  }

  // dstReg x= () [srcReg + disp]
  public final void emitFIMUL_Reg_RegDisp(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDA);
    // The register'' 1 is really part of the opcode
    emitRegDispRegOperands(srcReg, disp, (byte) 1);
    if (lister != null) lister.RRD(miStart, "FIMUL", dstReg, srcReg, disp);
  }

  // dstReg x= () [srcReg]
  public final void emitFIMUL_Reg_RegInd(byte dstReg, byte srcReg) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDA);
    // The register'' 1 is really part of the opcode
    emitRegIndirectRegOperands(srcReg, (byte) 1);
    if (lister != null) lister.RRN(miStart, "FIMUL", dstReg, srcReg);
  }

  // dstReg x= () [srcBase + srcIndex<<scale + disp]
  public final void emitFIMUL_Reg_RegIdx(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDA);
    // The register'' 1 is really part of the opcode
    emitSIBRegOperands(srcBase, srcIndex, scale, disp, (byte) 1);
    if (lister != null) lister.RRXD(miStart, "FIMUL", dstReg, srcBase, srcIndex, scale, disp);
  }

  // dstReg x= () [srcIndex<<scale + disp]
  public final void emitFIMUL_Reg_RegOff(byte dstReg, byte srcIndex, short scale, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDA);
    // The register'' 1 is really part of the opcode
    emitRegOffRegOperands(srcIndex, scale, disp, (byte) 1);
    if (lister != null) lister.RRFD(miStart, "FIMUL", dstReg, srcIndex, scale, disp);
  }

  // dstReg x= () [disp]
  public final void emitFIMUL_Reg_Abs(byte dstReg, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDA);
    // The register'' 1 is really part of the opcode
    emitAbsRegOperands(disp, (byte) 1);
    if (lister != null) lister.RRA(miStart, "FIMUL", dstReg, disp);
  }

  // dstReg x= (word) [srcReg + disp]
  public final void emitFIMUL_Reg_RegDisp_Word(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDE);
    // The register'' 1 is really part of the opcode
    emitRegDispRegOperands(srcReg, disp, (byte) 1);
    if (lister != null) lister.RRD(miStart, "FIMUL", dstReg, srcReg, disp);
  }

  // dstReg x= (word) [srcReg]
  public final void emitFIMUL_Reg_RegInd_Word(byte dstReg, byte srcReg) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDE);
    // The register'' 1 is really part of the opcode
    emitRegIndirectRegOperands(srcReg, (byte) 1);
    if (lister != null) lister.RRN(miStart, "FIMUL", dstReg, srcReg);
  }

  // dstReg x= (word) [srcBase + srcIndex<<scale + disp]
  public final void emitFIMUL_Reg_RegIdx_Word(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDE);
    // The register'' 1 is really part of the opcode
    emitSIBRegOperands(srcBase, srcIndex, scale, disp, (byte) 1);
    if (lister != null) lister.RRXD(miStart, "FIMUL", dstReg, srcBase, srcIndex, scale, disp);
  }

  // dstReg x= (word) [srcIndex<<scale + disp]
  public final void emitFIMUL_Reg_RegOff_Word(byte dstReg, byte srcIndex, short scale, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDE);
    // The register'' 1 is really part of the opcode
    emitRegOffRegOperands(srcIndex, scale, disp, (byte) 1);
    if (lister != null) lister.RRFD(miStart, "FIMUL", dstReg, srcIndex, scale, disp);
  }

  // dstReg x= (word) [disp]
  public final void emitFIMUL_Reg_Abs_Word(byte dstReg, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDE);
    // The register'' 1 is really part of the opcode
    emitAbsRegOperands(disp, (byte) 1);
    if (lister != null) lister.RRA(miStart, "FIMUL", dstReg, disp);
  }

  // dstReg x= srcReg
  public final void emitFMUL_Reg_Reg(byte dstReg, byte srcReg) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(srcReg == FP0 || dstReg == FP0);
    if (dstReg == FP0) {
    setMachineCodes(mi++, (byte) 0xD8);
    setMachineCodes(mi++, (byte) (0xC8 | srcReg));
    } else if (srcReg == FP0) {
    setMachineCodes(mi++, (byte) 0xDC);
    setMachineCodes(mi++, (byte) (0xC8 | dstReg));
    }
    if (lister != null) lister.RR(miStart, "FMUL", dstReg, srcReg);
  }

  // srcReg x= ST(0); pop stack
  public final void emitFMULP_Reg_Reg(byte dstReg, byte srcReg) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(srcReg == FP0);
    setMachineCodes(mi++, (byte) 0xDE);
    setMachineCodes(mi++, (byte) (0xC8 | dstReg));
    if (lister != null) lister.R(miStart, "FMULP", dstReg);
  }

  // dstReg -= () [srcReg + disp]
  public final void emitFSUB_Reg_RegDisp(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xD8);
    // The register'' 4 is really part of the opcode
    emitRegDispRegOperands(srcReg, disp, (byte) 4);
    if (lister != null) lister.RRD(miStart, "FSUB", dstReg, srcReg, disp);
  }

  // dstReg -= () [srcReg]
  public final void emitFSUB_Reg_RegInd(byte dstReg, byte srcReg) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xD8);
    // The register'' 4 is really part of the opcode
    emitRegIndirectRegOperands(srcReg, (byte) 4);
    if (lister != null) lister.RRN(miStart, "FSUB", dstReg, srcReg);
  }

  // dstReg -= () [srcBase + srcIndex<<scale + disp]
  public final void emitFSUB_Reg_RegIdx(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xD8);
    // The register'' 4 is really part of the opcode
    emitSIBRegOperands(srcBase, srcIndex, scale, disp, (byte) 4);
    if (lister != null) lister.RRXD(miStart, "FSUB", dstReg, srcBase, srcIndex, scale, disp);
  }

  // dstReg -= () [srcIndex<<scale + disp]
  public final void emitFSUB_Reg_RegOff(byte dstReg, byte srcIndex, short scale, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xD8);
    // The register'' 4 is really part of the opcode
    emitRegOffRegOperands(srcIndex, scale, disp, (byte) 4);
    if (lister != null) lister.RRFD(miStart, "FSUB", dstReg, srcIndex, scale, disp);
  }

  // dstReg -= () [disp]
  public final void emitFSUB_Reg_Abs(byte dstReg, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xD8);
    // The register'' 4 is really part of the opcode
    emitAbsRegOperands(disp, (byte) 4);
    if (lister != null) lister.RRA(miStart, "FSUB", dstReg, disp);
  }

  // dstReg -= (quad) [srcReg + disp]
  public final void emitFSUB_Reg_RegDisp_Quad(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDC);
    // The register'' 4 is really part of the opcode
    emitRegDispRegOperands(srcReg, disp, (byte) 4);
    if (lister != null) lister.RRD(miStart, "FSUB", dstReg, srcReg, disp);
  }

  // dstReg -= (quad) [srcReg]
  public final void emitFSUB_Reg_RegInd_Quad(byte dstReg, byte srcReg) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDC);
    // The register'' 4 is really part of the opcode
    emitRegIndirectRegOperands(srcReg, (byte) 4);
    if (lister != null) lister.RRN(miStart, "FSUB", dstReg, srcReg);
  }

  // dstReg -= (quad) [srcBase + srcIndex<<scale + disp]
  public final void emitFSUB_Reg_RegIdx_Quad(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDC);
    // The register'' 4 is really part of the opcode
    emitSIBRegOperands(srcBase, srcIndex, scale, disp, (byte) 4);
    if (lister != null) lister.RRXD(miStart, "FSUB", dstReg, srcBase, srcIndex, scale, disp);
  }

  // dstReg -= (quad) [srcIndex<<scale + disp]
  public final void emitFSUB_Reg_RegOff_Quad(byte dstReg, byte srcIndex, short scale, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDC);
    // The register'' 4 is really part of the opcode
    emitRegOffRegOperands(srcIndex, scale, disp, (byte) 4);
    if (lister != null) lister.RRFD(miStart, "FSUB", dstReg, srcIndex, scale, disp);
  }

  // dstReg -= (quad) [disp]
  public final void emitFSUB_Reg_Abs_Quad(byte dstReg, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDC);
    // The register'' 4 is really part of the opcode
    emitAbsRegOperands(disp, (byte) 4);
    if (lister != null) lister.RRA(miStart, "FSUB", dstReg, disp);
  }

  // dstReg -= () [srcReg + disp]
  public final void emitFISUB_Reg_RegDisp(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDA);
    // The register'' 4 is really part of the opcode
    emitRegDispRegOperands(srcReg, disp, (byte) 4);
    if (lister != null) lister.RRD(miStart, "FISUB", dstReg, srcReg, disp);
  }

  // dstReg -= () [srcReg]
  public final void emitFISUB_Reg_RegInd(byte dstReg, byte srcReg) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDA);
    // The register'' 4 is really part of the opcode
    emitRegIndirectRegOperands(srcReg, (byte) 4);
    if (lister != null) lister.RRN(miStart, "FISUB", dstReg, srcReg);
  }

  // dstReg -= () [srcBase + srcIndex<<scale + disp]
  public final void emitFISUB_Reg_RegIdx(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDA);
    // The register'' 4 is really part of the opcode
    emitSIBRegOperands(srcBase, srcIndex, scale, disp, (byte) 4);
    if (lister != null) lister.RRXD(miStart, "FISUB", dstReg, srcBase, srcIndex, scale, disp);
  }

  // dstReg -= () [srcIndex<<scale + disp]
  public final void emitFISUB_Reg_RegOff(byte dstReg, byte srcIndex, short scale, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDA);
    // The register'' 4 is really part of the opcode
    emitRegOffRegOperands(srcIndex, scale, disp, (byte) 4);
    if (lister != null) lister.RRFD(miStart, "FISUB", dstReg, srcIndex, scale, disp);
  }

  // dstReg -= () [disp]
  public final void emitFISUB_Reg_Abs(byte dstReg, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDA);
    // The register'' 4 is really part of the opcode
    emitAbsRegOperands(disp, (byte) 4);
    if (lister != null) lister.RRA(miStart, "FISUB", dstReg, disp);
  }

  // dstReg -= (word) [srcReg + disp]
  public final void emitFISUB_Reg_RegDisp_Word(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDE);
    // The register'' 4 is really part of the opcode
    emitRegDispRegOperands(srcReg, disp, (byte) 4);
    if (lister != null) lister.RRD(miStart, "FISUB", dstReg, srcReg, disp);
  }

  // dstReg -= (word) [srcReg]
  public final void emitFISUB_Reg_RegInd_Word(byte dstReg, byte srcReg) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDE);
    // The register'' 4 is really part of the opcode
    emitRegIndirectRegOperands(srcReg, (byte) 4);
    if (lister != null) lister.RRN(miStart, "FISUB", dstReg, srcReg);
  }

  // dstReg -= (word) [srcBase + srcIndex<<scale + disp]
  public final void emitFISUB_Reg_RegIdx_Word(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDE);
    // The register'' 4 is really part of the opcode
    emitSIBRegOperands(srcBase, srcIndex, scale, disp, (byte) 4);
    if (lister != null) lister.RRXD(miStart, "FISUB", dstReg, srcBase, srcIndex, scale, disp);
  }

  // dstReg -= (word) [srcIndex<<scale + disp]
  public final void emitFISUB_Reg_RegOff_Word(byte dstReg, byte srcIndex, short scale, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDE);
    // The register'' 4 is really part of the opcode
    emitRegOffRegOperands(srcIndex, scale, disp, (byte) 4);
    if (lister != null) lister.RRFD(miStart, "FISUB", dstReg, srcIndex, scale, disp);
  }

  // dstReg -= (word) [disp]
  public final void emitFISUB_Reg_Abs_Word(byte dstReg, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDE);
    // The register'' 4 is really part of the opcode
    emitAbsRegOperands(disp, (byte) 4);
    if (lister != null) lister.RRA(miStart, "FISUB", dstReg, disp);
  }

  // dstReg -= srcReg
  public final void emitFSUB_Reg_Reg(byte dstReg, byte srcReg) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(srcReg == FP0 || dstReg == FP0);
    if (dstReg == FP0) {
    setMachineCodes(mi++, (byte) 0xD8);
    setMachineCodes(mi++, (byte) (0xE0 | srcReg));
    } else if (srcReg == FP0) {
    setMachineCodes(mi++, (byte) 0xDC);
    setMachineCodes(mi++, (byte) (0xE8 | dstReg));
    }
    if (lister != null) lister.RR(miStart, "FSUB", dstReg, srcReg);
  }

  // srcReg -= ST(0); pop stack
  public final void emitFSUBP_Reg_Reg(byte dstReg, byte srcReg) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(srcReg == FP0);
    setMachineCodes(mi++, (byte) 0xDE);
    setMachineCodes(mi++, (byte) (0xE8 | dstReg));
    if (lister != null) lister.R(miStart, "FSUBP", dstReg);
  }

  // dstReg -= () [srcReg + disp]
  public final void emitFSUBR_Reg_RegDisp(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xD8);
    // The register'' 5 is really part of the opcode
    emitRegDispRegOperands(srcReg, disp, (byte) 5);
    if (lister != null) lister.RRD(miStart, "FSUBR", dstReg, srcReg, disp);
  }

  // dstReg -= () [srcReg]
  public final void emitFSUBR_Reg_RegInd(byte dstReg, byte srcReg) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xD8);
    // The register'' 5 is really part of the opcode
    emitRegIndirectRegOperands(srcReg, (byte) 5);
    if (lister != null) lister.RRN(miStart, "FSUBR", dstReg, srcReg);
  }

  // dstReg -= () [srcBase + srcIndex<<scale + disp]
  public final void emitFSUBR_Reg_RegIdx(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xD8);
    // The register'' 5 is really part of the opcode
    emitSIBRegOperands(srcBase, srcIndex, scale, disp, (byte) 5);
    if (lister != null) lister.RRXD(miStart, "FSUBR", dstReg, srcBase, srcIndex, scale, disp);
  }

  // dstReg -= () [srcIndex<<scale + disp]
  public final void emitFSUBR_Reg_RegOff(byte dstReg, byte srcIndex, short scale, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xD8);
    // The register'' 5 is really part of the opcode
    emitRegOffRegOperands(srcIndex, scale, disp, (byte) 5);
    if (lister != null) lister.RRFD(miStart, "FSUBR", dstReg, srcIndex, scale, disp);
  }

  // dstReg -= () [disp]
  public final void emitFSUBR_Reg_Abs(byte dstReg, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xD8);
    // The register'' 5 is really part of the opcode
    emitAbsRegOperands(disp, (byte) 5);
    if (lister != null) lister.RRA(miStart, "FSUBR", dstReg, disp);
  }

  // dstReg -= (quad) [srcReg + disp]
  public final void emitFSUBR_Reg_RegDisp_Quad(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDC);
    // The register'' 5 is really part of the opcode
    emitRegDispRegOperands(srcReg, disp, (byte) 5);
    if (lister != null) lister.RRD(miStart, "FSUBR", dstReg, srcReg, disp);
  }

  // dstReg -= (quad) [srcReg]
  public final void emitFSUBR_Reg_RegInd_Quad(byte dstReg, byte srcReg) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDC);
    // The register'' 5 is really part of the opcode
    emitRegIndirectRegOperands(srcReg, (byte) 5);
    if (lister != null) lister.RRN(miStart, "FSUBR", dstReg, srcReg);
  }

  // dstReg -= (quad) [srcBase + srcIndex<<scale + disp]
  public final void emitFSUBR_Reg_RegIdx_Quad(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDC);
    // The register'' 5 is really part of the opcode
    emitSIBRegOperands(srcBase, srcIndex, scale, disp, (byte) 5);
    if (lister != null) lister.RRXD(miStart, "FSUBR", dstReg, srcBase, srcIndex, scale, disp);
  }

  // dstReg -= (quad) [srcIndex<<scale + disp]
  public final void emitFSUBR_Reg_RegOff_Quad(byte dstReg, byte srcIndex, short scale, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDC);
    // The register'' 5 is really part of the opcode
    emitRegOffRegOperands(srcIndex, scale, disp, (byte) 5);
    if (lister != null) lister.RRFD(miStart, "FSUBR", dstReg, srcIndex, scale, disp);
  }

  // dstReg -= (quad) [disp]
  public final void emitFSUBR_Reg_Abs_Quad(byte dstReg, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDC);
    // The register'' 5 is really part of the opcode
    emitAbsRegOperands(disp, (byte) 5);
    if (lister != null) lister.RRA(miStart, "FSUBR", dstReg, disp);
  }

  // dstReg -= () [srcReg + disp]
  public final void emitFISUBR_Reg_RegDisp(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDA);
    // The register'' 5 is really part of the opcode
    emitRegDispRegOperands(srcReg, disp, (byte) 5);
    if (lister != null) lister.RRD(miStart, "FISUBR", dstReg, srcReg, disp);
  }

  // dstReg -= () [srcReg]
  public final void emitFISUBR_Reg_RegInd(byte dstReg, byte srcReg) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDA);
    // The register'' 5 is really part of the opcode
    emitRegIndirectRegOperands(srcReg, (byte) 5);
    if (lister != null) lister.RRN(miStart, "FISUBR", dstReg, srcReg);
  }

  // dstReg -= () [srcBase + srcIndex<<scale + disp]
  public final void emitFISUBR_Reg_RegIdx(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDA);
    // The register'' 5 is really part of the opcode
    emitSIBRegOperands(srcBase, srcIndex, scale, disp, (byte) 5);
    if (lister != null) lister.RRXD(miStart, "FISUBR", dstReg, srcBase, srcIndex, scale, disp);
  }

  // dstReg -= () [srcIndex<<scale + disp]
  public final void emitFISUBR_Reg_RegOff(byte dstReg, byte srcIndex, short scale, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDA);
    // The register'' 5 is really part of the opcode
    emitRegOffRegOperands(srcIndex, scale, disp, (byte) 5);
    if (lister != null) lister.RRFD(miStart, "FISUBR", dstReg, srcIndex, scale, disp);
  }

  // dstReg -= () [disp]
  public final void emitFISUBR_Reg_Abs(byte dstReg, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDA);
    // The register'' 5 is really part of the opcode
    emitAbsRegOperands(disp, (byte) 5);
    if (lister != null) lister.RRA(miStart, "FISUBR", dstReg, disp);
  }

  // dstReg -= (word) [srcReg + disp]
  public final void emitFISUBR_Reg_RegDisp_Word(byte dstReg, byte srcReg, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDE);
    // The register'' 5 is really part of the opcode
    emitRegDispRegOperands(srcReg, disp, (byte) 5);
    if (lister != null) lister.RRD(miStart, "FISUBR", dstReg, srcReg, disp);
  }

  // dstReg -= (word) [srcReg]
  public final void emitFISUBR_Reg_RegInd_Word(byte dstReg, byte srcReg) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDE);
    // The register'' 5 is really part of the opcode
    emitRegIndirectRegOperands(srcReg, (byte) 5);
    if (lister != null) lister.RRN(miStart, "FISUBR", dstReg, srcReg);
  }

  // dstReg -= (word) [srcBase + srcIndex<<scale + disp]
  public final void emitFISUBR_Reg_RegIdx_Word(byte dstReg, byte srcBase, byte srcIndex, short scale, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDE);
    // The register'' 5 is really part of the opcode
    emitSIBRegOperands(srcBase, srcIndex, scale, disp, (byte) 5);
    if (lister != null) lister.RRXD(miStart, "FISUBR", dstReg, srcBase, srcIndex, scale, disp);
  }

  // dstReg -= (word) [srcIndex<<scale + disp]
  public final void emitFISUBR_Reg_RegOff_Word(byte dstReg, byte srcIndex, short scale, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDE);
    // The register'' 5 is really part of the opcode
    emitRegOffRegOperands(srcIndex, scale, disp, (byte) 5);
    if (lister != null) lister.RRFD(miStart, "FISUBR", dstReg, srcIndex, scale, disp);
  }

  // dstReg -= (word) [disp]
  public final void emitFISUBR_Reg_Abs_Word(byte dstReg, Offset disp) {
    int miStart = mi;
    // Must store result to top of stack
    if (VM.VerifyAssertions) VM._assert(dstReg == 0);
    setMachineCodes(mi++, (byte) 0xDE);
    // The register'' 5 is really part of the opcode
    emitAbsRegOperands(disp, (byte) 5);
    if (lister != null) lister.RRA(miStart, "FISUBR", dstReg, disp);
  }

  // dstReg -= srcReg
  public final void emitFSUBR_Reg_Reg(byte dstReg, byte srcReg) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(srcReg == FP0 || dstReg == FP0);
    if (dstReg == FP0) {
    setMachineCodes(mi++, (byte) 0xD8);
    setMachineCodes(mi++, (byte) (0xE8 | srcReg));
    } else if (srcReg == FP0) {
    setMachineCodes(mi++, (byte) 0xDC);
    setMachineCodes(mi++, (byte) (0xE0 | dstReg));
    }
    if (lister != null) lister.RR(miStart, "FSUBR", dstReg, srcReg);
  }

  // srcReg -= ST(0); pop stack
  public final void emitFSUBRP_Reg_Reg(byte dstReg, byte srcReg) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(srcReg == FP0);
    setMachineCodes(mi++, (byte) 0xDE);
    setMachineCodes(mi++, (byte) (0xE0 | dstReg));
    if (lister != null) lister.R(miStart, "FSUBRP", dstReg);
  }

  // top of stack loaded from (double word) [reg + disp]
  public final void emitFLD_Reg_RegDisp(byte dummy, byte reg, Offset disp) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(dummy == FP0);
    setMachineCodes(mi++, (byte) 0xD9);
    emitRegDispRegOperands(reg, disp, (byte) 0);
    if (lister != null) lister.RD(miStart, "FLD", reg, disp);
  }

  // top of stack loaded from (double word) [reg]
  public final void emitFLD_Reg_RegInd(byte dummy, byte reg) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(dummy == FP0);
    setMachineCodes(mi++, (byte) 0xD9);
    emitRegIndirectRegOperands(reg, (byte) 0);
    if (lister != null) lister.RN(miStart, "FLD", reg);
  }

  // top of stack loaded from (double word) [baseReg + idxReg<<scale + disp]
  public final void emitFLD_Reg_RegIdx(byte dummy, byte baseReg, byte idxReg, short scale, Offset disp) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(dummy == FP0);
    setMachineCodes(mi++, (byte) 0xD9);
    emitSIBRegOperands(baseReg, idxReg, scale, disp, (byte) 0);
    if (lister != null) lister.RXD(miStart, "FLD", baseReg, idxReg, scale, disp);
  }

  // top of stack loaded from (double word) [idxReg<<scale + disp]
  public final void emitFLD_Reg_RegOff(byte dummy, byte idxReg, short scale, Offset disp) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(dummy == FP0);
    setMachineCodes(mi++, (byte) 0xD9);
    emitRegOffRegOperands(idxReg, scale, disp, (byte) 0);
    if (lister != null) lister.RFD(miStart, "FLD", idxReg, scale, disp);
  }

  // top of stack loaded from (double word) [disp]
  public final void emitFLD_Reg_Abs(byte dummy, Offset disp) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(dummy == FP0);
    setMachineCodes(mi++, (byte) 0xD9);
    emitAbsRegOperands(disp, (byte) 0);
    if (lister != null) lister.RA(miStart, "FLD", disp);
  }

  // top of stack loaded from (quad) [reg + disp]
  public final void emitFLD_Reg_RegDisp_Quad(byte dummy, byte reg, Offset disp) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(dummy == FP0);
    setMachineCodes(mi++, (byte) 0xDD);
    emitRegDispRegOperands(reg, disp, (byte) 0);
    if (lister != null) lister.RD(miStart, "FLD", reg, disp);
  }

  // top of stack loaded from (quad) [reg]
  public final void emitFLD_Reg_RegInd_Quad(byte dummy, byte reg) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(dummy == FP0);
    setMachineCodes(mi++, (byte) 0xDD);
    emitRegIndirectRegOperands(reg, (byte) 0);
    if (lister != null) lister.RN(miStart, "FLD", reg);
  }

  // top of stack loaded from (quad) [baseReg + idxReg<<scale + disp]
  public final void emitFLD_Reg_RegIdx_Quad(byte dummy, byte baseReg, byte idxReg, short scale, Offset disp) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(dummy == FP0);
    setMachineCodes(mi++, (byte) 0xDD);
    emitSIBRegOperands(baseReg, idxReg, scale, disp, (byte) 0);
    if (lister != null) lister.RXD(miStart, "FLD", baseReg, idxReg, scale, disp);
  }

  // top of stack loaded from (quad) [idxReg<<scale + disp]
  public final void emitFLD_Reg_RegOff_Quad(byte dummy, byte idxReg, short scale, Offset disp) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(dummy == FP0);
    setMachineCodes(mi++, (byte) 0xDD);
    emitRegOffRegOperands(idxReg, scale, disp, (byte) 0);
    if (lister != null) lister.RFD(miStart, "FLD", idxReg, scale, disp);
  }

  // top of stack loaded from (quad) [disp]
  public final void emitFLD_Reg_Abs_Quad(byte dummy, Offset disp) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(dummy == FP0);
    setMachineCodes(mi++, (byte) 0xDD);
    emitAbsRegOperands(disp, (byte) 0);
    if (lister != null) lister.RA(miStart, "FLD", disp);
  }

  // top of stack loaded from (word) [reg + disp]
  public final void emitFILD_Reg_RegDisp_Word(byte dummy, byte reg, Offset disp) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(dummy == FP0);
    setMachineCodes(mi++, (byte) 0xDF);
    emitRegDispRegOperands(reg, disp, (byte) 0);
    if (lister != null) lister.RD(miStart, "FILD", reg, disp);
  }

  // top of stack loaded from (word) [reg]
  public final void emitFILD_Reg_RegInd_Word(byte dummy, byte reg) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(dummy == FP0);
    setMachineCodes(mi++, (byte) 0xDF);
    emitRegIndirectRegOperands(reg, (byte) 0);
    if (lister != null) lister.RN(miStart, "FILD", reg);
  }

  // top of stack loaded from (word) [baseReg + idxReg<<scale + disp]
  public final void emitFILD_Reg_RegIdx_Word(byte dummy, byte baseReg, byte idxReg, short scale, Offset disp) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(dummy == FP0);
    setMachineCodes(mi++, (byte) 0xDF);
    emitSIBRegOperands(baseReg, idxReg, scale, disp, (byte) 0);
    if (lister != null) lister.RXD(miStart, "FILD", baseReg, idxReg, scale, disp);
  }

  // top of stack loaded from (word) [idxReg<<scale + disp]
  public final void emitFILD_Reg_RegOff_Word(byte dummy, byte idxReg, short scale, Offset disp) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(dummy == FP0);
    setMachineCodes(mi++, (byte) 0xDF);
    emitRegOffRegOperands(idxReg, scale, disp, (byte) 0);
    if (lister != null) lister.RFD(miStart, "FILD", idxReg, scale, disp);
  }

  // top of stack loaded from (word) [disp]
  public final void emitFILD_Reg_Abs_Word(byte dummy, Offset disp) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(dummy == FP0);
    setMachineCodes(mi++, (byte) 0xDF);
    emitAbsRegOperands(disp, (byte) 0);
    if (lister != null) lister.RA(miStart, "FILD", disp);
  }

  // top of stack loaded from (double word) [reg + disp]
  public final void emitFILD_Reg_RegDisp(byte dummy, byte reg, Offset disp) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(dummy == FP0);
    setMachineCodes(mi++, (byte) 0xDB);
    emitRegDispRegOperands(reg, disp, (byte) 0);
    if (lister != null) lister.RD(miStart, "FILD", reg, disp);
  }

  // top of stack loaded from (double word) [reg]
  public final void emitFILD_Reg_RegInd(byte dummy, byte reg) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(dummy == FP0);
    setMachineCodes(mi++, (byte) 0xDB);
    emitRegIndirectRegOperands(reg, (byte) 0);
    if (lister != null) lister.RN(miStart, "FILD", reg);
  }

  // top of stack loaded from (double word) [baseReg + idxReg<<scale + disp]
  public final void emitFILD_Reg_RegIdx(byte dummy, byte baseReg, byte idxReg, short scale, Offset disp) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(dummy == FP0);
    setMachineCodes(mi++, (byte) 0xDB);
    emitSIBRegOperands(baseReg, idxReg, scale, disp, (byte) 0);
    if (lister != null) lister.RXD(miStart, "FILD", baseReg, idxReg, scale, disp);
  }

  // top of stack loaded from (double word) [idxReg<<scale + disp]
  public final void emitFILD_Reg_RegOff(byte dummy, byte idxReg, short scale, Offset disp) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(dummy == FP0);
    setMachineCodes(mi++, (byte) 0xDB);
    emitRegOffRegOperands(idxReg, scale, disp, (byte) 0);
    if (lister != null) lister.RFD(miStart, "FILD", idxReg, scale, disp);
  }

  // top of stack loaded from (double word) [disp]
  public final void emitFILD_Reg_Abs(byte dummy, Offset disp) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(dummy == FP0);
    setMachineCodes(mi++, (byte) 0xDB);
    emitAbsRegOperands(disp, (byte) 0);
    if (lister != null) lister.RA(miStart, "FILD", disp);
  }

  // top of stack loaded from (quad) [reg + disp]
  public final void emitFILD_Reg_RegDisp_Quad(byte dummy, byte reg, Offset disp) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(dummy == FP0);
    setMachineCodes(mi++, (byte) 0xDF);
    emitRegDispRegOperands(reg, disp, (byte) 5);
    if (lister != null) lister.RD(miStart, "FILD", reg, disp);
  }

  // top of stack loaded from (quad) [reg]
  public final void emitFILD_Reg_RegInd_Quad(byte dummy, byte reg) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(dummy == FP0);
    setMachineCodes(mi++, (byte) 0xDF);
    emitRegIndirectRegOperands(reg, (byte) 5);
    if (lister != null) lister.RN(miStart, "FILD", reg);
  }

  // top of stack loaded from (quad) [baseReg + idxReg<<scale + disp]
  public final void emitFILD_Reg_RegIdx_Quad(byte dummy, byte baseReg, byte idxReg, short scale, Offset disp) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(dummy == FP0);
    setMachineCodes(mi++, (byte) 0xDF);
    emitSIBRegOperands(baseReg, idxReg, scale, disp, (byte) 5);
    if (lister != null) lister.RXD(miStart, "FILD", baseReg, idxReg, scale, disp);
  }

  // top of stack loaded from (quad) [idxReg<<scale + disp]
  public final void emitFILD_Reg_RegOff_Quad(byte dummy, byte idxReg, short scale, Offset disp) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(dummy == FP0);
    setMachineCodes(mi++, (byte) 0xDF);
    emitRegOffRegOperands(idxReg, scale, disp, (byte) 5);
    if (lister != null) lister.RFD(miStart, "FILD", idxReg, scale, disp);
  }

  // top of stack loaded from (quad) [disp]
  public final void emitFILD_Reg_Abs_Quad(byte dummy, Offset disp) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(dummy == FP0);
    setMachineCodes(mi++, (byte) 0xDF);
    emitAbsRegOperands(disp, (byte) 5);
    if (lister != null) lister.RA(miStart, "FILD", disp);
  }

  // top of stack stored to (word) [reg + disp]
  public final void emitFIST_RegDisp_Reg_Word(byte reg, Offset disp, byte dummy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(dummy == FP0);
    setMachineCodes(mi++, (byte) 0xDF);
    emitRegDispRegOperands(reg, disp, (byte) 2);
    if (lister != null) lister.RD(miStart, "FIST", reg, disp);
  }

  // top of stack stored to (word) [reg]
  public final void emitFIST_RegInd_Reg_Word(byte reg, byte dummy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(dummy == FP0);
    setMachineCodes(mi++, (byte) 0xDF);
    emitRegIndirectRegOperands(reg, (byte) 2);
    if (lister != null) lister.RN(miStart, "FIST", reg);
  }

  // top of stack stored to (word) [baseReg + idxReg<<scale + disp]
  public final void emitFIST_RegIdx_Reg_Word(byte baseReg, byte idxReg, short scale, Offset disp, byte dummy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(dummy == FP0);
    setMachineCodes(mi++, (byte) 0xDF);
    emitSIBRegOperands(baseReg, idxReg, scale, disp, (byte) 2);
    if (lister != null) lister.RXD(miStart, "FIST", baseReg, idxReg, scale, disp);
  }

  // top of stack stored to (word) [idxReg<<scale + disp]
  public final void emitFIST_RegOff_Reg_Word(byte idxReg, short scale, Offset disp, byte dummy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(dummy == FP0);
    setMachineCodes(mi++, (byte) 0xDF);
    emitRegOffRegOperands(idxReg, scale, disp, (byte) 2);
    if (lister != null) lister.RFD(miStart, "FIST", idxReg, scale, disp);
  }

  // top of stack stored to (word) [disp]
  public final void emitFIST_Abs_Reg_Word(Offset disp, byte dummy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(dummy == FP0);
    setMachineCodes(mi++, (byte) 0xDF);
    emitAbsRegOperands(disp, (byte) 2);
    if (lister != null) lister.RA(miStart, "FIST", disp);
  }

  // top of stack stored to (double word) [reg + disp]
  public final void emitFIST_RegDisp_Reg(byte reg, Offset disp, byte dummy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(dummy == FP0);
    setMachineCodes(mi++, (byte) 0xDB);
    emitRegDispRegOperands(reg, disp, (byte) 2);
    if (lister != null) lister.RD(miStart, "FIST", reg, disp);
  }

  // top of stack stored to (double word) [reg]
  public final void emitFIST_RegInd_Reg(byte reg, byte dummy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(dummy == FP0);
    setMachineCodes(mi++, (byte) 0xDB);
    emitRegIndirectRegOperands(reg, (byte) 2);
    if (lister != null) lister.RN(miStart, "FIST", reg);
  }

  // top of stack stored to (double word) [baseReg + idxReg<<scale + disp]
  public final void emitFIST_RegIdx_Reg(byte baseReg, byte idxReg, short scale, Offset disp, byte dummy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(dummy == FP0);
    setMachineCodes(mi++, (byte) 0xDB);
    emitSIBRegOperands(baseReg, idxReg, scale, disp, (byte) 2);
    if (lister != null) lister.RXD(miStart, "FIST", baseReg, idxReg, scale, disp);
  }

  // top of stack stored to (double word) [idxReg<<scale + disp]
  public final void emitFIST_RegOff_Reg(byte idxReg, short scale, Offset disp, byte dummy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(dummy == FP0);
    setMachineCodes(mi++, (byte) 0xDB);
    emitRegOffRegOperands(idxReg, scale, disp, (byte) 2);
    if (lister != null) lister.RFD(miStart, "FIST", idxReg, scale, disp);
  }

  // top of stack stored to (double word) [disp]
  public final void emitFIST_Abs_Reg(Offset disp, byte dummy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(dummy == FP0);
    setMachineCodes(mi++, (byte) 0xDB);
    emitAbsRegOperands(disp, (byte) 2);
    if (lister != null) lister.RA(miStart, "FIST", disp);
  }

  // top of stack stored to (word) [reg + disp]
  public final void emitFISTP_RegDisp_Reg_Word(byte reg, Offset disp, byte dummy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(dummy == FP0);
    setMachineCodes(mi++, (byte) 0xDF);
    emitRegDispRegOperands(reg, disp, (byte) 3);
    if (lister != null) lister.RD(miStart, "FISTP", reg, disp);
  }

  // top of stack stored to (word) [reg]
  public final void emitFISTP_RegInd_Reg_Word(byte reg, byte dummy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(dummy == FP0);
    setMachineCodes(mi++, (byte) 0xDF);
    emitRegIndirectRegOperands(reg, (byte) 3);
    if (lister != null) lister.RN(miStart, "FISTP", reg);
  }

  // top of stack stored to (word) [baseReg + idxReg<<scale + disp]
  public final void emitFISTP_RegIdx_Reg_Word(byte baseReg, byte idxReg, short scale, Offset disp, byte dummy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(dummy == FP0);
    setMachineCodes(mi++, (byte) 0xDF);
    emitSIBRegOperands(baseReg, idxReg, scale, disp, (byte) 3);
    if (lister != null) lister.RXD(miStart, "FISTP", baseReg, idxReg, scale, disp);
  }

  // top of stack stored to (word) [idxReg<<scale + disp]
  public final void emitFISTP_RegOff_Reg_Word(byte idxReg, short scale, Offset disp, byte dummy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(dummy == FP0);
    setMachineCodes(mi++, (byte) 0xDF);
    emitRegOffRegOperands(idxReg, scale, disp, (byte) 3);
    if (lister != null) lister.RFD(miStart, "FISTP", idxReg, scale, disp);
  }

  // top of stack stored to (word) [disp]
  public final void emitFISTP_Abs_Reg_Word(Offset disp, byte dummy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(dummy == FP0);
    setMachineCodes(mi++, (byte) 0xDF);
    emitAbsRegOperands(disp, (byte) 3);
    if (lister != null) lister.RA(miStart, "FISTP", disp);
  }

  // top of stack stored to (double word) [reg + disp]
  public final void emitFISTP_RegDisp_Reg(byte reg, Offset disp, byte dummy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(dummy == FP0);
    setMachineCodes(mi++, (byte) 0xDB);
    emitRegDispRegOperands(reg, disp, (byte) 3);
    if (lister != null) lister.RD(miStart, "FISTP", reg, disp);
  }

  // top of stack stored to (double word) [reg]
  public final void emitFISTP_RegInd_Reg(byte reg, byte dummy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(dummy == FP0);
    setMachineCodes(mi++, (byte) 0xDB);
    emitRegIndirectRegOperands(reg, (byte) 3);
    if (lister != null) lister.RN(miStart, "FISTP", reg);
  }

  // top of stack stored to (double word) [baseReg + idxReg<<scale + disp]
  public final void emitFISTP_RegIdx_Reg(byte baseReg, byte idxReg, short scale, Offset disp, byte dummy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(dummy == FP0);
    setMachineCodes(mi++, (byte) 0xDB);
    emitSIBRegOperands(baseReg, idxReg, scale, disp, (byte) 3);
    if (lister != null) lister.RXD(miStart, "FISTP", baseReg, idxReg, scale, disp);
  }

  // top of stack stored to (double word) [idxReg<<scale + disp]
  public final void emitFISTP_RegOff_Reg(byte idxReg, short scale, Offset disp, byte dummy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(dummy == FP0);
    setMachineCodes(mi++, (byte) 0xDB);
    emitRegOffRegOperands(idxReg, scale, disp, (byte) 3);
    if (lister != null) lister.RFD(miStart, "FISTP", idxReg, scale, disp);
  }

  // top of stack stored to (double word) [disp]
  public final void emitFISTP_Abs_Reg(Offset disp, byte dummy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(dummy == FP0);
    setMachineCodes(mi++, (byte) 0xDB);
    emitAbsRegOperands(disp, (byte) 3);
    if (lister != null) lister.RA(miStart, "FISTP", disp);
  }

  // top of stack stored to (quad) [reg + disp]
  public final void emitFISTP_RegDisp_Reg_Quad(byte reg, Offset disp, byte dummy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(dummy == FP0);
    setMachineCodes(mi++, (byte) 0xDF);
    emitRegDispRegOperands(reg, disp, (byte) 7);
    if (lister != null) lister.RD(miStart, "FISTP", reg, disp);
  }

  // top of stack stored to (quad) [reg]
  public final void emitFISTP_RegInd_Reg_Quad(byte reg, byte dummy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(dummy == FP0);
    setMachineCodes(mi++, (byte) 0xDF);
    emitRegIndirectRegOperands(reg, (byte) 7);
    if (lister != null) lister.RN(miStart, "FISTP", reg);
  }

  // top of stack stored to (quad) [baseReg + idxReg<<scale + disp]
  public final void emitFISTP_RegIdx_Reg_Quad(byte baseReg, byte idxReg, short scale, Offset disp, byte dummy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(dummy == FP0);
    setMachineCodes(mi++, (byte) 0xDF);
    emitSIBRegOperands(baseReg, idxReg, scale, disp, (byte) 7);
    if (lister != null) lister.RXD(miStart, "FISTP", baseReg, idxReg, scale, disp);
  }

  // top of stack stored to (quad) [idxReg<<scale + disp]
  public final void emitFISTP_RegOff_Reg_Quad(byte idxReg, short scale, Offset disp, byte dummy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(dummy == FP0);
    setMachineCodes(mi++, (byte) 0xDF);
    emitRegOffRegOperands(idxReg, scale, disp, (byte) 7);
    if (lister != null) lister.RFD(miStart, "FISTP", idxReg, scale, disp);
  }

  // top of stack stored to (quad) [disp]
  public final void emitFISTP_Abs_Reg_Quad(Offset disp, byte dummy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(dummy == FP0);
    setMachineCodes(mi++, (byte) 0xDF);
    emitAbsRegOperands(disp, (byte) 7);
    if (lister != null) lister.RA(miStart, "FISTP", disp);
  }

  // top of stack stored to (double word) [reg + disp]
  public final void emitFST_RegDisp_Reg(byte reg, Offset disp, byte dummy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(dummy == FP0);
    setMachineCodes(mi++, (byte) 0xD9);
    emitRegDispRegOperands(reg, disp, (byte) 2);
    if (lister != null) lister.RD(miStart, "FST", reg, disp);
  }

  // top of stack stored to (double word) [reg]
  public final void emitFST_RegInd_Reg(byte reg, byte dummy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(dummy == FP0);
    setMachineCodes(mi++, (byte) 0xD9);
    emitRegIndirectRegOperands(reg, (byte) 2);
    if (lister != null) lister.RN(miStart, "FST", reg);
  }

  // top of stack stored to (double word) [baseReg + idxReg<<scale + disp]
  public final void emitFST_RegIdx_Reg(byte baseReg, byte idxReg, short scale, Offset disp, byte dummy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(dummy == FP0);
    setMachineCodes(mi++, (byte) 0xD9);
    emitSIBRegOperands(baseReg, idxReg, scale, disp, (byte) 2);
    if (lister != null) lister.RXD(miStart, "FST", baseReg, idxReg, scale, disp);
  }

  // top of stack stored to (double word) [idxReg<<scale + disp]
  public final void emitFST_RegOff_Reg(byte idxReg, short scale, Offset disp, byte dummy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(dummy == FP0);
    setMachineCodes(mi++, (byte) 0xD9);
    emitRegOffRegOperands(idxReg, scale, disp, (byte) 2);
    if (lister != null) lister.RFD(miStart, "FST", idxReg, scale, disp);
  }

  // top of stack stored to (double word) [disp]
  public final void emitFST_Abs_Reg(Offset disp, byte dummy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(dummy == FP0);
    setMachineCodes(mi++, (byte) 0xD9);
    emitAbsRegOperands(disp, (byte) 2);
    if (lister != null) lister.RA(miStart, "FST", disp);
  }

  // top of stack stored to (quad) [reg + disp]
  public final void emitFST_RegDisp_Reg_Quad(byte reg, Offset disp, byte dummy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(dummy == FP0);
    setMachineCodes(mi++, (byte) 0xDD);
    emitRegDispRegOperands(reg, disp, (byte) 2);
    if (lister != null) lister.RD(miStart, "FST", reg, disp);
  }

  // top of stack stored to (quad) [reg]
  public final void emitFST_RegInd_Reg_Quad(byte reg, byte dummy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(dummy == FP0);
    setMachineCodes(mi++, (byte) 0xDD);
    emitRegIndirectRegOperands(reg, (byte) 2);
    if (lister != null) lister.RN(miStart, "FST", reg);
  }

  // top of stack stored to (quad) [baseReg + idxReg<<scale + disp]
  public final void emitFST_RegIdx_Reg_Quad(byte baseReg, byte idxReg, short scale, Offset disp, byte dummy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(dummy == FP0);
    setMachineCodes(mi++, (byte) 0xDD);
    emitSIBRegOperands(baseReg, idxReg, scale, disp, (byte) 2);
    if (lister != null) lister.RXD(miStart, "FST", baseReg, idxReg, scale, disp);
  }

  // top of stack stored to (quad) [idxReg<<scale + disp]
  public final void emitFST_RegOff_Reg_Quad(byte idxReg, short scale, Offset disp, byte dummy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(dummy == FP0);
    setMachineCodes(mi++, (byte) 0xDD);
    emitRegOffRegOperands(idxReg, scale, disp, (byte) 2);
    if (lister != null) lister.RFD(miStart, "FST", idxReg, scale, disp);
  }

  // top of stack stored to (quad) [disp]
  public final void emitFST_Abs_Reg_Quad(Offset disp, byte dummy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(dummy == FP0);
    setMachineCodes(mi++, (byte) 0xDD);
    emitAbsRegOperands(disp, (byte) 2);
    if (lister != null) lister.RA(miStart, "FST", disp);
  }

  // top of stack stored to (double word) [reg + disp]
  public final void emitFSTP_RegDisp_Reg(byte reg, Offset disp, byte dummy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(dummy == FP0);
    setMachineCodes(mi++, (byte) 0xD9);
    emitRegDispRegOperands(reg, disp, (byte) 3);
    if (lister != null) lister.RD(miStart, "FSTP", reg, disp);
  }

  // top of stack stored to (double word) [reg]
  public final void emitFSTP_RegInd_Reg(byte reg, byte dummy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(dummy == FP0);
    setMachineCodes(mi++, (byte) 0xD9);
    emitRegIndirectRegOperands(reg, (byte) 3);
    if (lister != null) lister.RN(miStart, "FSTP", reg);
  }

  // top of stack stored to (double word) [baseReg + idxReg<<scale + disp]
  public final void emitFSTP_RegIdx_Reg(byte baseReg, byte idxReg, short scale, Offset disp, byte dummy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(dummy == FP0);
    setMachineCodes(mi++, (byte) 0xD9);
    emitSIBRegOperands(baseReg, idxReg, scale, disp, (byte) 3);
    if (lister != null) lister.RXD(miStart, "FSTP", baseReg, idxReg, scale, disp);
  }

  // top of stack stored to (double word) [idxReg<<scale + disp]
  public final void emitFSTP_RegOff_Reg(byte idxReg, short scale, Offset disp, byte dummy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(dummy == FP0);
    setMachineCodes(mi++, (byte) 0xD9);
    emitRegOffRegOperands(idxReg, scale, disp, (byte) 3);
    if (lister != null) lister.RFD(miStart, "FSTP", idxReg, scale, disp);
  }

  // top of stack stored to (double word) [disp]
  public final void emitFSTP_Abs_Reg(Offset disp, byte dummy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(dummy == FP0);
    setMachineCodes(mi++, (byte) 0xD9);
    emitAbsRegOperands(disp, (byte) 3);
    if (lister != null) lister.RA(miStart, "FSTP", disp);
  }

  // top of stack stored to (quad) [reg + disp]
  public final void emitFSTP_RegDisp_Reg_Quad(byte reg, Offset disp, byte dummy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(dummy == FP0);
    setMachineCodes(mi++, (byte) 0xDD);
    emitRegDispRegOperands(reg, disp, (byte) 3);
    if (lister != null) lister.RD(miStart, "FSTP", reg, disp);
  }

  // top of stack stored to (quad) [reg]
  public final void emitFSTP_RegInd_Reg_Quad(byte reg, byte dummy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(dummy == FP0);
    setMachineCodes(mi++, (byte) 0xDD);
    emitRegIndirectRegOperands(reg, (byte) 3);
    if (lister != null) lister.RN(miStart, "FSTP", reg);
  }

  // top of stack stored to (quad) [baseReg + idxReg<<scale + disp]
  public final void emitFSTP_RegIdx_Reg_Quad(byte baseReg, byte idxReg, short scale, Offset disp, byte dummy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(dummy == FP0);
    setMachineCodes(mi++, (byte) 0xDD);
    emitSIBRegOperands(baseReg, idxReg, scale, disp, (byte) 3);
    if (lister != null) lister.RXD(miStart, "FSTP", baseReg, idxReg, scale, disp);
  }

  // top of stack stored to (quad) [idxReg<<scale + disp]
  public final void emitFSTP_RegOff_Reg_Quad(byte idxReg, short scale, Offset disp, byte dummy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(dummy == FP0);
    setMachineCodes(mi++, (byte) 0xDD);
    emitRegOffRegOperands(idxReg, scale, disp, (byte) 3);
    if (lister != null) lister.RFD(miStart, "FSTP", idxReg, scale, disp);
  }

  // top of stack stored to (quad) [disp]
  public final void emitFSTP_Abs_Reg_Quad(Offset disp, byte dummy) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(dummy == FP0);
    setMachineCodes(mi++, (byte) 0xDD);
    emitAbsRegOperands(disp, (byte) 3);
    if (lister != null) lister.RA(miStart, "FSTP", disp);
  }

  public final void emitFCOMI_Reg_Reg (byte reg1, byte reg2) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(reg1 == FP0);
    setMachineCodes(mi++, (byte) 0xDB);
    setMachineCodes(mi++, (byte)  (0xF0 | reg2));
    if (lister != null) lister.RR(miStart, "FCOMI", reg1, reg2);
  }


  public final void emitFCOMIP_Reg_Reg (byte reg1, byte reg2) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(reg1 == FP0);
    setMachineCodes(mi++, (byte) 0xDF);
    setMachineCodes(mi++, (byte)  (0xF0 | reg2));
    if (lister != null) lister.RR(miStart, "FCOMIP", reg1, reg2);
  }


  public final void emitFUCOMI_Reg_Reg (byte reg1, byte reg2) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(reg1 == FP0);
    setMachineCodes(mi++, (byte) 0xDB);
    setMachineCodes(mi++, (byte)  (0xE8 | reg2));
    if (lister != null) lister.RR(miStart, "FUCOMI", reg1, reg2);
  }


  public final void emitFUCOMIP_Reg_Reg (byte reg1, byte reg2) {
    int miStart = mi;
    if (VM.VerifyAssertions) VM._assert(reg1 == FP0);
    setMachineCodes(mi++, (byte) 0xDF);
    setMachineCodes(mi++, (byte)  (0xE8 | reg2));
    if (lister != null) lister.RR(miStart, "FUCOMIP", reg1, reg2);
  }


  public final void emitMOV_RegInd_Imm_Byte(byte dst, int imm) {
      int miStart = mi;
      setMachineCodes(mi++, (byte) 0xC6);
      emitRegIndirectRegOperands(dst, (byte) 0x0);
      emitImm8(imm);
      if (lister != null) lister.RNI(miStart, "MOV", dst, imm);
  }

  public final void emitMOV_RegDisp_Imm_Byte(byte dst, Offset disp, int imm) {
      int miStart = mi;
      setMachineCodes(mi++, (byte) 0xC6);
      emitRegDispRegOperands(dst, disp, (byte) 0x0);
      emitImm8(imm);
      if (lister != null) lister.RDI(miStart, "MOV", dst, disp, imm);
  }

  public final void emitMOV_RegIdx_Imm_Byte(byte dst, byte idx, short scale, Offset disp, int imm) {
      int miStart = mi;
      setMachineCodes(mi++, (byte) 0xC6);
      emitSIBRegOperands(dst, idx, scale, disp, (byte) 0x0);
      emitImm8(imm);
      if (lister != null) lister.RXDI(miStart, "MOV", dst, idx, scale, disp, imm);
  }

  public final void emitMOV_RegOff_Imm_Byte(byte idx, short scale, Offset disp, int imm) {
      int miStart = mi;
      setMachineCodes(mi++, (byte) 0xC6);
      emitRegOffRegOperands(idx, scale, disp, (byte) 0x0);
      emitImm8(imm);
      if (lister != null) lister.RFDI(miStart, "MOV", idx, scale, disp, imm);
  }

  public final void emitMOV_Abs_Imm_Byte(Offset disp, int imm) {
      int miStart = mi;
      setMachineCodes(mi++, (byte) 0xC6);
      emitAbsRegOperands(disp, (byte) 0x0);
      emitImm8(imm);
      if (lister != null) lister.RAI(miStart, "MOV", disp, imm);
  }

  public final void emitMOV_RegInd_Imm_Word(byte dst, int imm) {
      int miStart = mi;
      setMachineCodes(mi++, (byte) 0x66);

      setMachineCodes(mi++, (byte) 0xC7);
      emitRegIndirectRegOperands(dst, (byte) 0x0);
      emitImm16(imm);
      if (lister != null) lister.RNI(miStart, "MOV", dst, imm);
  }

  public final void emitMOV_RegDisp_Imm_Word(byte dst, Offset disp, int imm) {
      int miStart = mi;
      setMachineCodes(mi++, (byte) 0x66);

      setMachineCodes(mi++, (byte) 0xC7);
      emitRegDispRegOperands(dst, disp, (byte) 0x0);
      emitImm16(imm);
      if (lister != null) lister.RDI(miStart, "MOV", dst, disp, imm);
  }

  public final void emitMOV_RegIdx_Imm_Word(byte dst, byte idx, short scale, Offset disp, int imm) {
      int miStart = mi;
      setMachineCodes(mi++, (byte) 0x66);

      setMachineCodes(mi++, (byte) 0xC7);
      emitSIBRegOperands(dst, idx, scale, disp, (byte) 0x0);
      emitImm16(imm);
      if (lister != null) lister.RXDI(miStart, "MOV", dst, idx, scale, disp, imm);
  }

  public final void emitMOV_RegOff_Imm_Word(byte idx, short scale, Offset disp, int imm) {
      int miStart = mi;
      setMachineCodes(mi++, (byte) 0x66);

      setMachineCodes(mi++, (byte) 0xC7);
      emitRegOffRegOperands(idx, scale, disp, (byte) 0x0);
      emitImm16(imm);
      if (lister != null) lister.RFDI(miStart, "MOV", idx, scale, disp, imm);
  }

  public final void emitMOV_Abs_Imm_Word(Offset disp, int imm) {
      int miStart = mi;
      setMachineCodes(mi++, (byte) 0x66);

      setMachineCodes(mi++, (byte) 0xC7);
      emitAbsRegOperands(disp, (byte) 0x0);
      emitImm16(imm);
      if (lister != null) lister.RAI(miStart, "MOV", disp, imm);
  }

  public final void emitMOV_RegInd_Imm(byte dst, int imm) {
      int miStart = mi;
      setMachineCodes(mi++, (byte) 0xC7);
      emitRegIndirectRegOperands(dst, (byte) 0x0);
      emitImm32(imm);
      if (lister != null) lister.RNI(miStart, "MOV", dst, imm);
  }

  public final void emitMOV_RegDisp_Imm(byte dst, Offset disp, int imm) {
      int miStart = mi;
      setMachineCodes(mi++, (byte) 0xC7);
      emitRegDispRegOperands(dst, disp, (byte) 0x0);
      emitImm32(imm);
      if (lister != null) lister.RDI(miStart, "MOV", dst, disp, imm);
  }

  public final void emitMOV_RegIdx_Imm(byte dst, byte idx, short scale, Offset disp, int imm) {
      int miStart = mi;
      setMachineCodes(mi++, (byte) 0xC7);
      emitSIBRegOperands(dst, idx, scale, disp, (byte) 0x0);
      emitImm32(imm);
      if (lister != null) lister.RXDI(miStart, "MOV", dst, idx, scale, disp, imm);
  }

  public final void emitMOV_RegOff_Imm(byte idx, short scale, Offset disp, int imm) {
      int miStart = mi;
      setMachineCodes(mi++, (byte) 0xC7);
      emitRegOffRegOperands(idx, scale, disp, (byte) 0x0);
      emitImm32(imm);
      if (lister != null) lister.RFDI(miStart, "MOV", idx, scale, disp, imm);
  }

  public final void emitMOV_Abs_Imm(Offset disp, int imm) {
      int miStart = mi;
      setMachineCodes(mi++, (byte) 0xC7);
      emitAbsRegOperands(disp, (byte) 0x0);
      emitImm32(imm);
      if (lister != null) lister.RAI(miStart, "MOV", disp, imm);
  }

  // save FPU state ignoring pending exceptions
  public final void emitFNSAVE_RegDisp (byte dstReg, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xDD);
    emitRegDispRegOperands(dstReg, disp, (byte) 6);
    if (lister != null) lister.RD(miStart, "FNSAVE", dstReg, disp);
  }

  // save FPU state ignoring pending exceptions
  public final void emitFNSAVE_RegInd (byte dstReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xDD);
    emitRegIndirectRegOperands(dstReg, (byte) 6);
    if (lister != null) lister.RN(miStart, "FNSAVE", dstReg);
  }

  // save FPU state ignoring pending exceptions
  public final void emitFNSAVE_RegIdx (byte baseReg, byte indexReg, short scale, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xDD);
    emitSIBRegOperands(baseReg, indexReg, scale, disp, (byte) 6);
    if (lister != null) lister.RXD(miStart, "FNSAVE", baseReg, indexReg, scale, disp);
  }

  // save FPU state ignoring pending exceptions
  public final void emitFNSAVE_RegOff (byte indexReg, short scale, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xDD);
    emitRegOffRegOperands(indexReg, scale, disp, (byte) 6);
    if (lister != null) lister.RFD(miStart, "FNSAVE", indexReg, scale, disp);
  }

  // save FPU state ignoring pending exceptions
  public final void emitFNSAVE_Abs (Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xDD);
    emitAbsRegOperands(disp, (byte) 6);
    if (lister != null) lister.RA(miStart, "FNSAVE", disp);
  }

  // save FPU state respecting pending exceptions
  public final void emitFSAVE_RegDisp (byte dstReg, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x9B);
    setMachineCodes(mi++, (byte) 0xDD);
    emitRegDispRegOperands(dstReg, disp, (byte) 6);
    if (lister != null) lister.RD(miStart, "FSAVE", dstReg, disp);
  }

  // save FPU state respecting pending exceptions
  public final void emitFSAVE_RegInd (byte dstReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x9B);
    setMachineCodes(mi++, (byte) 0xDD);
    emitRegIndirectRegOperands(dstReg, (byte) 6);
    if (lister != null) lister.RN(miStart, "FSAVE", dstReg);
  }

  // save FPU state respecting pending exceptions
  public final void emitFSAVE_RegIdx (byte baseReg, byte indexReg, short scale, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x9B);
    setMachineCodes(mi++, (byte) 0xDD);
    emitSIBRegOperands(baseReg, indexReg, scale, disp, (byte) 6);
    if (lister != null) lister.RXD(miStart, "FSAVE", baseReg, indexReg, scale, disp);
  }

  // save FPU state respecting pending exceptions
  public final void emitFSAVE_RegOff (byte indexReg, short scale, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x9B);
    setMachineCodes(mi++, (byte) 0xDD);
    emitRegOffRegOperands(indexReg, scale, disp, (byte) 6);
    if (lister != null) lister.RFD(miStart, "FSAVE", indexReg, scale, disp);
  }

  // save FPU state respecting pending exceptions
  public final void emitFSAVE_Abs (Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x9B);
    setMachineCodes(mi++, (byte) 0xDD);
    emitAbsRegOperands(disp, (byte) 6);
    if (lister != null) lister.RA(miStart, "FSAVE", disp);
  }

  // restore FPU state
  public final void emitFRSTOR_RegDisp (byte dstReg, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xDD);
    emitRegDispRegOperands(dstReg, disp, (byte) 4);
    if (lister != null) lister.RD(miStart, "FRSTOR", dstReg, disp);
  }

  // restore FPU state
  public final void emitFRSTOR_RegInd (byte dstReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xDD);
    emitRegIndirectRegOperands(dstReg, (byte) 4);
    if (lister != null) lister.RN(miStart, "FRSTOR", dstReg);
  }

  // restore FPU state
  public final void emitFRSTOR_RegIdx (byte baseReg, byte indexReg, short scale, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xDD);
    emitSIBRegOperands(baseReg, indexReg, scale, disp, (byte) 4);
    if (lister != null) lister.RXD(miStart, "FRSTOR", baseReg, indexReg, scale, disp);
  }

  // restore FPU state
  public final void emitFRSTOR_RegOff (byte indexReg, short scale, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xDD);
    emitRegOffRegOperands(indexReg, scale, disp, (byte) 4);
    if (lister != null) lister.RFD(miStart, "FRSTOR", indexReg, scale, disp);
  }

  // restore FPU state
  public final void emitFRSTOR_Abs (Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xDD);
    emitAbsRegOperands(disp, (byte) 4);
    if (lister != null) lister.RA(miStart, "FRSTOR", disp);
  }

  // load FPU control word
  public final void emitFLDCW_RegDisp (byte dstReg, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xD9);
    emitRegDispRegOperands(dstReg, disp, (byte) 5);
    if (lister != null) lister.RD(miStart, "FLDCW", dstReg, disp);
  }

  // load FPU control word
  public final void emitFLDCW_RegInd (byte dstReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xD9);
    emitRegIndirectRegOperands(dstReg, (byte) 5);
    if (lister != null) lister.RN(miStart, "FLDCW", dstReg);
  }

  // load FPU control word
  public final void emitFLDCW_RegIdx (byte baseReg, byte indexReg, short scale, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xD9);
    emitSIBRegOperands(baseReg, indexReg, scale, disp, (byte) 5);
    if (lister != null) lister.RXD(miStart, "FLDCW", baseReg, indexReg, scale, disp);
  }

  // load FPU control word
  public final void emitFLDCW_RegOff (byte indexReg, short scale, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xD9);
    emitRegOffRegOperands(indexReg, scale, disp, (byte) 5);
    if (lister != null) lister.RFD(miStart, "FLDCW", indexReg, scale, disp);
  }

  // load FPU control word
  public final void emitFLDCW_Abs (Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xD9);
    emitAbsRegOperands(disp, (byte) 5);
    if (lister != null) lister.RA(miStart, "FLDCW", disp);
  }

  // store FPU control word, checking for exceptions
  public final void emitFSTCW_RegDisp (byte dstReg, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x9B);
    setMachineCodes(mi++, (byte) 0xD9);
    emitRegDispRegOperands(dstReg, disp, (byte) 7);
    if (lister != null) lister.RD(miStart, "FSTCW", dstReg, disp);
  }

  // store FPU control word, checking for exceptions
  public final void emitFSTCW_RegInd (byte dstReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x9B);
    setMachineCodes(mi++, (byte) 0xD9);
    emitRegIndirectRegOperands(dstReg, (byte) 7);
    if (lister != null) lister.RN(miStart, "FSTCW", dstReg);
  }

  // store FPU control word, checking for exceptions
  public final void emitFSTCW_RegIdx (byte baseReg, byte indexReg, short scale, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x9B);
    setMachineCodes(mi++, (byte) 0xD9);
    emitSIBRegOperands(baseReg, indexReg, scale, disp, (byte) 7);
    if (lister != null) lister.RXD(miStart, "FSTCW", baseReg, indexReg, scale, disp);
  }

  // store FPU control word, checking for exceptions
  public final void emitFSTCW_RegOff (byte indexReg, short scale, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x9B);
    setMachineCodes(mi++, (byte) 0xD9);
    emitRegOffRegOperands(indexReg, scale, disp, (byte) 7);
    if (lister != null) lister.RFD(miStart, "FSTCW", indexReg, scale, disp);
  }

  // store FPU control word, checking for exceptions
  public final void emitFSTCW_Abs (Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0x9B);
    setMachineCodes(mi++, (byte) 0xD9);
    emitAbsRegOperands(disp, (byte) 7);
    if (lister != null) lister.RA(miStart, "FSTCW", disp);
  }

  // store FPU control word, ignoring exceptions
  public final void emitFNSTCW_RegDisp (byte dstReg, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xD9);
    emitRegDispRegOperands(dstReg, disp, (byte) 7);
    if (lister != null) lister.RD(miStart, "FNSTCW", dstReg, disp);
  }

  // store FPU control word, ignoring exceptions
  public final void emitFNSTCW_RegInd (byte dstReg) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xD9);
    emitRegIndirectRegOperands(dstReg, (byte) 7);
    if (lister != null) lister.RN(miStart, "FNSTCW", dstReg);
  }

  // store FPU control word, ignoring exceptions
  public final void emitFNSTCW_RegIdx (byte baseReg, byte indexReg, short scale, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xD9);
    emitSIBRegOperands(baseReg, indexReg, scale, disp, (byte) 7);
    if (lister != null) lister.RXD(miStart, "FNSTCW", baseReg, indexReg, scale, disp);
  }

  // store FPU control word, ignoring exceptions
  public final void emitFNSTCW_RegOff (byte indexReg, short scale, Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xD9);
    emitRegOffRegOperands(indexReg, scale, disp, (byte) 7);
    if (lister != null) lister.RFD(miStart, "FNSTCW", indexReg, scale, disp);
  }

  // store FPU control word, ignoring exceptions
  public final void emitFNSTCW_Abs (Offset disp) {
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xD9);
    emitAbsRegOperands(disp, (byte) 7);
    if (lister != null) lister.RA(miStart, "FNSTCW", disp);
  }

  // load 1.0 into FP0
  public final void emitFLD1_Reg(byte dstReg) {
    if (VM.VerifyAssertions) VM._assert(dstReg == FP0);
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xD9);
    setMachineCodes(mi++, (byte) 0xE8);
    if (lister != null) lister.R(miStart, "FLD1", dstReg);
  }

  // load log_2(10) into FP0
  public final void emitFLDL2T_Reg(byte dstReg) {
    if (VM.VerifyAssertions) VM._assert(dstReg == FP0);
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xD9);
    setMachineCodes(mi++, (byte) 0xE9);
    if (lister != null) lister.R(miStart, "FLDL2T", dstReg);
  }

  // load log_2(e) into FP0
  public final void emitFLDL2E_Reg(byte dstReg) {
    if (VM.VerifyAssertions) VM._assert(dstReg == FP0);
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xD9);
    setMachineCodes(mi++, (byte) 0xEA);
    if (lister != null) lister.R(miStart, "FLDL2E", dstReg);
  }

  // load pi into FP0
  public final void emitFLDPI_Reg(byte dstReg) {
    if (VM.VerifyAssertions) VM._assert(dstReg == FP0);
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xD9);
    setMachineCodes(mi++, (byte) 0xEB);
    if (lister != null) lister.R(miStart, "FLDPI", dstReg);
  }

  // load log_10(2) into FP0
  public final void emitFLDLG2_Reg(byte dstReg) {
    if (VM.VerifyAssertions) VM._assert(dstReg == FP0);
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xD9);
    setMachineCodes(mi++, (byte) 0xEC);
    if (lister != null) lister.R(miStart, "FLDLG2", dstReg);
  }

  // load log_e(2) into FP0
  public final void emitFLDLN2_Reg(byte dstReg) {
    if (VM.VerifyAssertions) VM._assert(dstReg == FP0);
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xD9);
    setMachineCodes(mi++, (byte) 0xED);
    if (lister != null) lister.R(miStart, "FLDLN2", dstReg);
  }

  // load 0.0 into FP0
  public final void emitFLDZ_Reg(byte dstReg) {
    if (VM.VerifyAssertions) VM._assert(dstReg == FP0);
    int miStart = mi;
    setMachineCodes(mi++, (byte) 0xD9);
    setMachineCodes(mi++, (byte) 0xEE);
    if (lister != null) lister.R(miStart, "FLDZ", dstReg);
  }

}
