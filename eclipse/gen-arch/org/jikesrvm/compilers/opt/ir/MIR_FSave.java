
/*
 * THIS FILE IS MACHINE_GENERATED. DO NOT EDIT.
 * See InstructionFormats.template, InstructionFormatList.dat,
 * OperatorList.dat, etc.
 */

package org.jikesrvm.compilers.opt.ir;

import org.jikesrvm.VM_Configuration;
import org.jikesrvm.compilers.opt.ir.ia32.OPT_IA32ConditionOperand; //NOPMD


/**
 * The MIR_FSave InstructionFormat class.
 *
 * The header comment for {@link OPT_Instruction} contains
 * an explanation of the role of InstructionFormats in the
 * opt compiler's IR.
 */
@SuppressWarnings("unused")  // Machine generated code is never 100% clean
public final class MIR_FSave extends OPT_InstructionFormat {
  /**
   * InstructionFormat identification method for MIR_FSave.
   * @param i an instruction
   * @return <code>true</code> if the InstructionFormat of the argument
   *         instruction is MIR_FSave or <code>false</code>
   *         if it is not.
   */
  public static boolean conforms(OPT_Instruction i) {
    return conforms(i.operator);
  }
  /**
   * InstructionFormat identification method for MIR_FSave.
   * @param o an instruction
   * @return <code>true</code> if the InstructionFormat of the argument
   *         operator is MIR_FSave or <code>false</code>
   *         if it is not.
   */
  public static boolean conforms(OPT_Operator o) {
    return o.format == MIR_FSave_format;
  }

  /**
   * Get the operand called Destination from the
   * argument instruction. Note that the returned operand
   * will still point to its containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Destination
   */
  public static OPT_Operand getDestination(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_FSave");
    return (OPT_Operand) i.getOperand(0);
  }
  /**
   * Get the operand called Destination from the argument
   * instruction clearing its instruction pointer. The returned
   * operand will not point to any containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Destination
   */
  public static OPT_Operand getClearDestination(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_FSave");
    return (OPT_Operand) i.getClearOperand(0);
  }
  /**
   * Set the operand called Destination in the argument
   * instruction to the argument operand. The operand will
   * now point to the argument instruction as its containing
   * instruction.
   * @param i the instruction in which to store the operand
   * @param Destination the operand to store
   */
  public static void setDestination(OPT_Instruction i, OPT_Operand Destination) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_FSave");
    i.putOperand(0, Destination);
  }
  /**
   * Return the index of the operand called Destination
   * in the argument instruction.
   * @param i the instruction to access.
   * @return the index of the operand called Destination
   *         in the argument instruction
   */
  public static int indexOfDestination(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_FSave");
    return 0;
  }
  /**
   * Does the argument instruction have a non-null
   * operand named Destination?
   * @param i the instruction to access.
   * @return <code>true</code> if the instruction has an non-null
   *         operand named Destination or <code>false</code>
   *         if it does not.
   */
  public static boolean hasDestination(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_FSave");
    return i.getOperand(0) != null;
  }


  /**
   * Create an instruction of the MIR_FSave instruction format.
   * @param o the instruction's operator
   * @param Destination the instruction's Destination operand
   * @return the newly created MIR_FSave instruction
   */
  public static OPT_Instruction create(OPT_Operator o
                   , OPT_Operand Destination
                )
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(o)) fail(o, "MIR_FSave");
    OPT_Instruction i = new OPT_Instruction(o, 5);
    i.putOperand(0, Destination);
    return i;
  }

  /**
   * Mutate the argument instruction into an instruction of the
   * MIR_FSave instruction format having the specified
   * operator and operands.
   * @param i the instruction to mutate
   * @param o the instruction's operator
   * @param Destination the instruction's Destination operand
   * @return the mutated instruction
   */
  public static OPT_Instruction mutate(OPT_Instruction i, OPT_Operator o
                   , OPT_Operand Destination
                )
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(o)) fail(o, "MIR_FSave");
    i.operator = o;
    i.putOperand(0, Destination);
    return i;
  }
}

