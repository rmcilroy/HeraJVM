
/*
 * THIS FILE IS MACHINE_GENERATED. DO NOT EDIT.
 * See InstructionFormats.template, InstructionFormatList.dat,
 * OperatorList.dat, etc.
 */

package org.jikesrvm.compilers.opt.ir;

import org.jikesrvm.VM_Configuration;
import org.jikesrvm.compilers.opt.ir.ia32.OPT_IA32ConditionOperand; //NOPMD


/**
 * The MIR_CompareExchange InstructionFormat class.
 *
 * The header comment for {@link OPT_Instruction} contains
 * an explanation of the role of InstructionFormats in the
 * opt compiler's IR.
 */
@SuppressWarnings("unused")  // Machine generated code is never 100% clean
public final class MIR_CompareExchange extends OPT_InstructionFormat {
  /**
   * InstructionFormat identification method for MIR_CompareExchange.
   * @param i an instruction
   * @return <code>true</code> if the InstructionFormat of the argument
   *         instruction is MIR_CompareExchange or <code>false</code>
   *         if it is not.
   */
  public static boolean conforms(OPT_Instruction i) {
    return conforms(i.operator);
  }
  /**
   * InstructionFormat identification method for MIR_CompareExchange.
   * @param o an instruction
   * @return <code>true</code> if the InstructionFormat of the argument
   *         operator is MIR_CompareExchange or <code>false</code>
   *         if it is not.
   */
  public static boolean conforms(OPT_Operator o) {
    return o.format == MIR_CompareExchange_format;
  }

  /**
   * Get the operand called OldValue from the
   * argument instruction. Note that the returned operand
   * will still point to its containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called OldValue
   */
  public static OPT_RegisterOperand getOldValue(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CompareExchange");
    return (OPT_RegisterOperand) i.getOperand(0);
  }
  /**
   * Get the operand called OldValue from the argument
   * instruction clearing its instruction pointer. The returned
   * operand will not point to any containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called OldValue
   */
  public static OPT_RegisterOperand getClearOldValue(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CompareExchange");
    return (OPT_RegisterOperand) i.getClearOperand(0);
  }
  /**
   * Set the operand called OldValue in the argument
   * instruction to the argument operand. The operand will
   * now point to the argument instruction as its containing
   * instruction.
   * @param i the instruction in which to store the operand
   * @param OldValue the operand to store
   */
  public static void setOldValue(OPT_Instruction i, OPT_RegisterOperand OldValue) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CompareExchange");
    i.putOperand(0, OldValue);
  }
  /**
   * Return the index of the operand called OldValue
   * in the argument instruction.
   * @param i the instruction to access.
   * @return the index of the operand called OldValue
   *         in the argument instruction
   */
  public static int indexOfOldValue(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CompareExchange");
    return 0;
  }
  /**
   * Does the argument instruction have a non-null
   * operand named OldValue?
   * @param i the instruction to access.
   * @return <code>true</code> if the instruction has an non-null
   *         operand named OldValue or <code>false</code>
   *         if it does not.
   */
  public static boolean hasOldValue(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CompareExchange");
    return i.getOperand(0) != null;
  }

  /**
   * Get the operand called MemAddr from the
   * argument instruction. Note that the returned operand
   * will still point to its containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called MemAddr
   */
  public static OPT_MemoryOperand getMemAddr(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CompareExchange");
    return (OPT_MemoryOperand) i.getOperand(1);
  }
  /**
   * Get the operand called MemAddr from the argument
   * instruction clearing its instruction pointer. The returned
   * operand will not point to any containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called MemAddr
   */
  public static OPT_MemoryOperand getClearMemAddr(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CompareExchange");
    return (OPT_MemoryOperand) i.getClearOperand(1);
  }
  /**
   * Set the operand called MemAddr in the argument
   * instruction to the argument operand. The operand will
   * now point to the argument instruction as its containing
   * instruction.
   * @param i the instruction in which to store the operand
   * @param MemAddr the operand to store
   */
  public static void setMemAddr(OPT_Instruction i, OPT_MemoryOperand MemAddr) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CompareExchange");
    i.putOperand(1, MemAddr);
  }
  /**
   * Return the index of the operand called MemAddr
   * in the argument instruction.
   * @param i the instruction to access.
   * @return the index of the operand called MemAddr
   *         in the argument instruction
   */
  public static int indexOfMemAddr(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CompareExchange");
    return 1;
  }
  /**
   * Does the argument instruction have a non-null
   * operand named MemAddr?
   * @param i the instruction to access.
   * @return <code>true</code> if the instruction has an non-null
   *         operand named MemAddr or <code>false</code>
   *         if it does not.
   */
  public static boolean hasMemAddr(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CompareExchange");
    return i.getOperand(1) != null;
  }

  /**
   * Get the operand called NewValue from the
   * argument instruction. Note that the returned operand
   * will still point to its containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called NewValue
   */
  public static OPT_RegisterOperand getNewValue(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CompareExchange");
    return (OPT_RegisterOperand) i.getOperand(2);
  }
  /**
   * Get the operand called NewValue from the argument
   * instruction clearing its instruction pointer. The returned
   * operand will not point to any containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called NewValue
   */
  public static OPT_RegisterOperand getClearNewValue(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CompareExchange");
    return (OPT_RegisterOperand) i.getClearOperand(2);
  }
  /**
   * Set the operand called NewValue in the argument
   * instruction to the argument operand. The operand will
   * now point to the argument instruction as its containing
   * instruction.
   * @param i the instruction in which to store the operand
   * @param NewValue the operand to store
   */
  public static void setNewValue(OPT_Instruction i, OPT_RegisterOperand NewValue) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CompareExchange");
    i.putOperand(2, NewValue);
  }
  /**
   * Return the index of the operand called NewValue
   * in the argument instruction.
   * @param i the instruction to access.
   * @return the index of the operand called NewValue
   *         in the argument instruction
   */
  public static int indexOfNewValue(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CompareExchange");
    return 2;
  }
  /**
   * Does the argument instruction have a non-null
   * operand named NewValue?
   * @param i the instruction to access.
   * @return <code>true</code> if the instruction has an non-null
   *         operand named NewValue or <code>false</code>
   *         if it does not.
   */
  public static boolean hasNewValue(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CompareExchange");
    return i.getOperand(2) != null;
  }


  /**
   * Create an instruction of the MIR_CompareExchange instruction format.
   * @param o the instruction's operator
   * @param OldValue the instruction's OldValue operand
   * @param MemAddr the instruction's MemAddr operand
   * @param NewValue the instruction's NewValue operand
   * @return the newly created MIR_CompareExchange instruction
   */
  public static OPT_Instruction create(OPT_Operator o
                   , OPT_RegisterOperand OldValue
                   , OPT_MemoryOperand MemAddr
                   , OPT_RegisterOperand NewValue
                )
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(o)) fail(o, "MIR_CompareExchange");
    OPT_Instruction i = new OPT_Instruction(o, 5);
    i.putOperand(0, OldValue);
    i.putOperand(1, MemAddr);
    i.putOperand(2, NewValue);
    return i;
  }

  /**
   * Mutate the argument instruction into an instruction of the
   * MIR_CompareExchange instruction format having the specified
   * operator and operands.
   * @param i the instruction to mutate
   * @param o the instruction's operator
   * @param OldValue the instruction's OldValue operand
   * @param MemAddr the instruction's MemAddr operand
   * @param NewValue the instruction's NewValue operand
   * @return the mutated instruction
   */
  public static OPT_Instruction mutate(OPT_Instruction i, OPT_Operator o
                   , OPT_RegisterOperand OldValue
                   , OPT_MemoryOperand MemAddr
                   , OPT_RegisterOperand NewValue
                )
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(o)) fail(o, "MIR_CompareExchange");
    i.operator = o;
    i.putOperand(0, OldValue);
    i.putOperand(1, MemAddr);
    i.putOperand(2, NewValue);
    return i;
  }
}

