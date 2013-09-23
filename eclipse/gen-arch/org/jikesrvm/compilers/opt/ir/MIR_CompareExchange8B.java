
/*
 * THIS FILE IS MACHINE_GENERATED. DO NOT EDIT.
 * See InstructionFormats.template, InstructionFormatList.dat,
 * OperatorList.dat, etc.
 */

package org.jikesrvm.compilers.opt.ir;

import org.jikesrvm.VM_Configuration;
import org.jikesrvm.compilers.opt.ir.ia32.OPT_IA32ConditionOperand; //NOPMD


/**
 * The MIR_CompareExchange8B InstructionFormat class.
 *
 * The header comment for {@link OPT_Instruction} contains
 * an explanation of the role of InstructionFormats in the
 * opt compiler's IR.
 */
@SuppressWarnings("unused")  // Machine generated code is never 100% clean
public final class MIR_CompareExchange8B extends OPT_InstructionFormat {
  /**
   * InstructionFormat identification method for MIR_CompareExchange8B.
   * @param i an instruction
   * @return <code>true</code> if the InstructionFormat of the argument
   *         instruction is MIR_CompareExchange8B or <code>false</code>
   *         if it is not.
   */
  public static boolean conforms(OPT_Instruction i) {
    return conforms(i.operator);
  }
  /**
   * InstructionFormat identification method for MIR_CompareExchange8B.
   * @param o an instruction
   * @return <code>true</code> if the InstructionFormat of the argument
   *         operator is MIR_CompareExchange8B or <code>false</code>
   *         if it is not.
   */
  public static boolean conforms(OPT_Operator o) {
    return o.format == MIR_CompareExchange8B_format;
  }

  /**
   * Get the operand called OldValueHigh from the
   * argument instruction. Note that the returned operand
   * will still point to its containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called OldValueHigh
   */
  public static OPT_RegisterOperand getOldValueHigh(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CompareExchange8B");
    return (OPT_RegisterOperand) i.getOperand(0);
  }
  /**
   * Get the operand called OldValueHigh from the argument
   * instruction clearing its instruction pointer. The returned
   * operand will not point to any containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called OldValueHigh
   */
  public static OPT_RegisterOperand getClearOldValueHigh(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CompareExchange8B");
    return (OPT_RegisterOperand) i.getClearOperand(0);
  }
  /**
   * Set the operand called OldValueHigh in the argument
   * instruction to the argument operand. The operand will
   * now point to the argument instruction as its containing
   * instruction.
   * @param i the instruction in which to store the operand
   * @param OldValueHigh the operand to store
   */
  public static void setOldValueHigh(OPT_Instruction i, OPT_RegisterOperand OldValueHigh) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CompareExchange8B");
    i.putOperand(0, OldValueHigh);
  }
  /**
   * Return the index of the operand called OldValueHigh
   * in the argument instruction.
   * @param i the instruction to access.
   * @return the index of the operand called OldValueHigh
   *         in the argument instruction
   */
  public static int indexOfOldValueHigh(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CompareExchange8B");
    return 0;
  }
  /**
   * Does the argument instruction have a non-null
   * operand named OldValueHigh?
   * @param i the instruction to access.
   * @return <code>true</code> if the instruction has an non-null
   *         operand named OldValueHigh or <code>false</code>
   *         if it does not.
   */
  public static boolean hasOldValueHigh(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CompareExchange8B");
    return i.getOperand(0) != null;
  }

  /**
   * Get the operand called OldValueLow from the
   * argument instruction. Note that the returned operand
   * will still point to its containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called OldValueLow
   */
  public static OPT_RegisterOperand getOldValueLow(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CompareExchange8B");
    return (OPT_RegisterOperand) i.getOperand(1);
  }
  /**
   * Get the operand called OldValueLow from the argument
   * instruction clearing its instruction pointer. The returned
   * operand will not point to any containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called OldValueLow
   */
  public static OPT_RegisterOperand getClearOldValueLow(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CompareExchange8B");
    return (OPT_RegisterOperand) i.getClearOperand(1);
  }
  /**
   * Set the operand called OldValueLow in the argument
   * instruction to the argument operand. The operand will
   * now point to the argument instruction as its containing
   * instruction.
   * @param i the instruction in which to store the operand
   * @param OldValueLow the operand to store
   */
  public static void setOldValueLow(OPT_Instruction i, OPT_RegisterOperand OldValueLow) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CompareExchange8B");
    i.putOperand(1, OldValueLow);
  }
  /**
   * Return the index of the operand called OldValueLow
   * in the argument instruction.
   * @param i the instruction to access.
   * @return the index of the operand called OldValueLow
   *         in the argument instruction
   */
  public static int indexOfOldValueLow(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CompareExchange8B");
    return 1;
  }
  /**
   * Does the argument instruction have a non-null
   * operand named OldValueLow?
   * @param i the instruction to access.
   * @return <code>true</code> if the instruction has an non-null
   *         operand named OldValueLow or <code>false</code>
   *         if it does not.
   */
  public static boolean hasOldValueLow(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CompareExchange8B");
    return i.getOperand(1) != null;
  }

  /**
   * Get the operand called MemAddr from the
   * argument instruction. Note that the returned operand
   * will still point to its containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called MemAddr
   */
  public static OPT_MemoryOperand getMemAddr(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CompareExchange8B");
    return (OPT_MemoryOperand) i.getOperand(2);
  }
  /**
   * Get the operand called MemAddr from the argument
   * instruction clearing its instruction pointer. The returned
   * operand will not point to any containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called MemAddr
   */
  public static OPT_MemoryOperand getClearMemAddr(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CompareExchange8B");
    return (OPT_MemoryOperand) i.getClearOperand(2);
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
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CompareExchange8B");
    i.putOperand(2, MemAddr);
  }
  /**
   * Return the index of the operand called MemAddr
   * in the argument instruction.
   * @param i the instruction to access.
   * @return the index of the operand called MemAddr
   *         in the argument instruction
   */
  public static int indexOfMemAddr(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CompareExchange8B");
    return 2;
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
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CompareExchange8B");
    return i.getOperand(2) != null;
  }

  /**
   * Get the operand called NewValueHigh from the
   * argument instruction. Note that the returned operand
   * will still point to its containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called NewValueHigh
   */
  public static OPT_RegisterOperand getNewValueHigh(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CompareExchange8B");
    return (OPT_RegisterOperand) i.getOperand(3);
  }
  /**
   * Get the operand called NewValueHigh from the argument
   * instruction clearing its instruction pointer. The returned
   * operand will not point to any containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called NewValueHigh
   */
  public static OPT_RegisterOperand getClearNewValueHigh(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CompareExchange8B");
    return (OPT_RegisterOperand) i.getClearOperand(3);
  }
  /**
   * Set the operand called NewValueHigh in the argument
   * instruction to the argument operand. The operand will
   * now point to the argument instruction as its containing
   * instruction.
   * @param i the instruction in which to store the operand
   * @param NewValueHigh the operand to store
   */
  public static void setNewValueHigh(OPT_Instruction i, OPT_RegisterOperand NewValueHigh) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CompareExchange8B");
    i.putOperand(3, NewValueHigh);
  }
  /**
   * Return the index of the operand called NewValueHigh
   * in the argument instruction.
   * @param i the instruction to access.
   * @return the index of the operand called NewValueHigh
   *         in the argument instruction
   */
  public static int indexOfNewValueHigh(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CompareExchange8B");
    return 3;
  }
  /**
   * Does the argument instruction have a non-null
   * operand named NewValueHigh?
   * @param i the instruction to access.
   * @return <code>true</code> if the instruction has an non-null
   *         operand named NewValueHigh or <code>false</code>
   *         if it does not.
   */
  public static boolean hasNewValueHigh(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CompareExchange8B");
    return i.getOperand(3) != null;
  }

  /**
   * Get the operand called NewValueLow from the
   * argument instruction. Note that the returned operand
   * will still point to its containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called NewValueLow
   */
  public static OPT_RegisterOperand getNewValueLow(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CompareExchange8B");
    return (OPT_RegisterOperand) i.getOperand(4);
  }
  /**
   * Get the operand called NewValueLow from the argument
   * instruction clearing its instruction pointer. The returned
   * operand will not point to any containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called NewValueLow
   */
  public static OPT_RegisterOperand getClearNewValueLow(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CompareExchange8B");
    return (OPT_RegisterOperand) i.getClearOperand(4);
  }
  /**
   * Set the operand called NewValueLow in the argument
   * instruction to the argument operand. The operand will
   * now point to the argument instruction as its containing
   * instruction.
   * @param i the instruction in which to store the operand
   * @param NewValueLow the operand to store
   */
  public static void setNewValueLow(OPT_Instruction i, OPT_RegisterOperand NewValueLow) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CompareExchange8B");
    i.putOperand(4, NewValueLow);
  }
  /**
   * Return the index of the operand called NewValueLow
   * in the argument instruction.
   * @param i the instruction to access.
   * @return the index of the operand called NewValueLow
   *         in the argument instruction
   */
  public static int indexOfNewValueLow(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CompareExchange8B");
    return 4;
  }
  /**
   * Does the argument instruction have a non-null
   * operand named NewValueLow?
   * @param i the instruction to access.
   * @return <code>true</code> if the instruction has an non-null
   *         operand named NewValueLow or <code>false</code>
   *         if it does not.
   */
  public static boolean hasNewValueLow(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CompareExchange8B");
    return i.getOperand(4) != null;
  }


  /**
   * Create an instruction of the MIR_CompareExchange8B instruction format.
   * @param o the instruction's operator
   * @param OldValueHigh the instruction's OldValueHigh operand
   * @param OldValueLow the instruction's OldValueLow operand
   * @param MemAddr the instruction's MemAddr operand
   * @param NewValueHigh the instruction's NewValueHigh operand
   * @param NewValueLow the instruction's NewValueLow operand
   * @return the newly created MIR_CompareExchange8B instruction
   */
  public static OPT_Instruction create(OPT_Operator o
                   , OPT_RegisterOperand OldValueHigh
                   , OPT_RegisterOperand OldValueLow
                   , OPT_MemoryOperand MemAddr
                   , OPT_RegisterOperand NewValueHigh
                   , OPT_RegisterOperand NewValueLow
                )
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(o)) fail(o, "MIR_CompareExchange8B");
    OPT_Instruction i = new OPT_Instruction(o, 5);
    i.putOperand(0, OldValueHigh);
    i.putOperand(1, OldValueLow);
    i.putOperand(2, MemAddr);
    i.putOperand(3, NewValueHigh);
    i.putOperand(4, NewValueLow);
    return i;
  }

  /**
   * Mutate the argument instruction into an instruction of the
   * MIR_CompareExchange8B instruction format having the specified
   * operator and operands.
   * @param i the instruction to mutate
   * @param o the instruction's operator
   * @param OldValueHigh the instruction's OldValueHigh operand
   * @param OldValueLow the instruction's OldValueLow operand
   * @param MemAddr the instruction's MemAddr operand
   * @param NewValueHigh the instruction's NewValueHigh operand
   * @param NewValueLow the instruction's NewValueLow operand
   * @return the mutated instruction
   */
  public static OPT_Instruction mutate(OPT_Instruction i, OPT_Operator o
                   , OPT_RegisterOperand OldValueHigh
                   , OPT_RegisterOperand OldValueLow
                   , OPT_MemoryOperand MemAddr
                   , OPT_RegisterOperand NewValueHigh
                   , OPT_RegisterOperand NewValueLow
                )
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(o)) fail(o, "MIR_CompareExchange8B");
    i.operator = o;
    i.putOperand(0, OldValueHigh);
    i.putOperand(1, OldValueLow);
    i.putOperand(2, MemAddr);
    i.putOperand(3, NewValueHigh);
    i.putOperand(4, NewValueLow);
    return i;
  }
}

