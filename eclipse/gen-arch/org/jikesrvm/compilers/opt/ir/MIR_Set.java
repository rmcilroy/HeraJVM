
/*
 * THIS FILE IS MACHINE_GENERATED. DO NOT EDIT.
 * See InstructionFormats.template, InstructionFormatList.dat,
 * OperatorList.dat, etc.
 */

package org.jikesrvm.compilers.opt.ir;

import org.jikesrvm.VM_Configuration;
import org.jikesrvm.compilers.opt.ir.ia32.OPT_IA32ConditionOperand; //NOPMD


/**
 * The MIR_Set InstructionFormat class.
 *
 * The header comment for {@link OPT_Instruction} contains
 * an explanation of the role of InstructionFormats in the
 * opt compiler's IR.
 */
@SuppressWarnings("unused")  // Machine generated code is never 100% clean
public final class MIR_Set extends OPT_InstructionFormat {
  /**
   * InstructionFormat identification method for MIR_Set.
   * @param i an instruction
   * @return <code>true</code> if the InstructionFormat of the argument
   *         instruction is MIR_Set or <code>false</code>
   *         if it is not.
   */
  public static boolean conforms(OPT_Instruction i) {
    return conforms(i.operator);
  }
  /**
   * InstructionFormat identification method for MIR_Set.
   * @param o an instruction
   * @return <code>true</code> if the InstructionFormat of the argument
   *         operator is MIR_Set or <code>false</code>
   *         if it is not.
   */
  public static boolean conforms(OPT_Operator o) {
    return o.format == MIR_Set_format;
  }

  /**
   * Get the operand called Result from the
   * argument instruction. Note that the returned operand
   * will still point to its containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Result
   */
  public static OPT_Operand getResult(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_Set");
    return (OPT_Operand) i.getOperand(0);
  }
  /**
   * Get the operand called Result from the argument
   * instruction clearing its instruction pointer. The returned
   * operand will not point to any containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Result
   */
  public static OPT_Operand getClearResult(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_Set");
    return (OPT_Operand) i.getClearOperand(0);
  }
  /**
   * Set the operand called Result in the argument
   * instruction to the argument operand. The operand will
   * now point to the argument instruction as its containing
   * instruction.
   * @param i the instruction in which to store the operand
   * @param Result the operand to store
   */
  public static void setResult(OPT_Instruction i, OPT_Operand Result) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_Set");
    i.putOperand(0, Result);
  }
  /**
   * Return the index of the operand called Result
   * in the argument instruction.
   * @param i the instruction to access.
   * @return the index of the operand called Result
   *         in the argument instruction
   */
  public static int indexOfResult(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_Set");
    return 0;
  }
  /**
   * Does the argument instruction have a non-null
   * operand named Result?
   * @param i the instruction to access.
   * @return <code>true</code> if the instruction has an non-null
   *         operand named Result or <code>false</code>
   *         if it does not.
   */
  public static boolean hasResult(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_Set");
    return i.getOperand(0) != null;
  }

  /**
   * Get the operand called Cond from the
   * argument instruction. Note that the returned operand
   * will still point to its containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Cond
   */
  public static OPT_IA32ConditionOperand getCond(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_Set");
    return (OPT_IA32ConditionOperand) i.getOperand(1);
  }
  /**
   * Get the operand called Cond from the argument
   * instruction clearing its instruction pointer. The returned
   * operand will not point to any containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Cond
   */
  public static OPT_IA32ConditionOperand getClearCond(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_Set");
    return (OPT_IA32ConditionOperand) i.getClearOperand(1);
  }
  /**
   * Set the operand called Cond in the argument
   * instruction to the argument operand. The operand will
   * now point to the argument instruction as its containing
   * instruction.
   * @param i the instruction in which to store the operand
   * @param Cond the operand to store
   */
  public static void setCond(OPT_Instruction i, OPT_IA32ConditionOperand Cond) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_Set");
    i.putOperand(1, Cond);
  }
  /**
   * Return the index of the operand called Cond
   * in the argument instruction.
   * @param i the instruction to access.
   * @return the index of the operand called Cond
   *         in the argument instruction
   */
  public static int indexOfCond(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_Set");
    return 1;
  }
  /**
   * Does the argument instruction have a non-null
   * operand named Cond?
   * @param i the instruction to access.
   * @return <code>true</code> if the instruction has an non-null
   *         operand named Cond or <code>false</code>
   *         if it does not.
   */
  public static boolean hasCond(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_Set");
    return i.getOperand(1) != null;
  }


  /**
   * Create an instruction of the MIR_Set instruction format.
   * @param o the instruction's operator
   * @param Result the instruction's Result operand
   * @param Cond the instruction's Cond operand
   * @return the newly created MIR_Set instruction
   */
  public static OPT_Instruction create(OPT_Operator o
                   , OPT_Operand Result
                   , OPT_IA32ConditionOperand Cond
                )
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(o)) fail(o, "MIR_Set");
    OPT_Instruction i = new OPT_Instruction(o, 5);
    i.putOperand(0, Result);
    i.putOperand(1, Cond);
    return i;
  }

  /**
   * Mutate the argument instruction into an instruction of the
   * MIR_Set instruction format having the specified
   * operator and operands.
   * @param i the instruction to mutate
   * @param o the instruction's operator
   * @param Result the instruction's Result operand
   * @param Cond the instruction's Cond operand
   * @return the mutated instruction
   */
  public static OPT_Instruction mutate(OPT_Instruction i, OPT_Operator o
                   , OPT_Operand Result
                   , OPT_IA32ConditionOperand Cond
                )
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(o)) fail(o, "MIR_Set");
    i.operator = o;
    i.putOperand(0, Result);
    i.putOperand(1, Cond);
    return i;
  }
}

