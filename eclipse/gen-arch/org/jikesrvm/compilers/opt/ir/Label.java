
/*
 * THIS FILE IS MACHINE_GENERATED. DO NOT EDIT.
 * See InstructionFormats.template, InstructionFormatList.dat,
 * OperatorList.dat, etc.
 */

package org.jikesrvm.compilers.opt.ir;

import org.jikesrvm.VM_Configuration;
import org.jikesrvm.compilers.opt.ir.ia32.OPT_IA32ConditionOperand; //NOPMD


/**
 * The Label InstructionFormat class.
 *
 * The header comment for {@link OPT_Instruction} contains
 * an explanation of the role of InstructionFormats in the
 * opt compiler's IR.
 */
@SuppressWarnings("unused")  // Machine generated code is never 100% clean
public final class Label extends OPT_InstructionFormat {
  /**
   * InstructionFormat identification method for Label.
   * @param i an instruction
   * @return <code>true</code> if the InstructionFormat of the argument
   *         instruction is Label or <code>false</code>
   *         if it is not.
   */
  public static boolean conforms(OPT_Instruction i) {
    return conforms(i.operator);
  }
  /**
   * InstructionFormat identification method for Label.
   * @param o an instruction
   * @return <code>true</code> if the InstructionFormat of the argument
   *         operator is Label or <code>false</code>
   *         if it is not.
   */
  public static boolean conforms(OPT_Operator o) {
    return o.format == Label_format;
  }

  /**
   * Get the operand called Block from the
   * argument instruction. Note that the returned operand
   * will still point to its containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Block
   */
  public static OPT_BasicBlockOperand getBlock(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Label");
    return (OPT_BasicBlockOperand) i.getOperand(0);
  }
  /**
   * Get the operand called Block from the argument
   * instruction clearing its instruction pointer. The returned
   * operand will not point to any containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Block
   */
  public static OPT_BasicBlockOperand getClearBlock(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Label");
    return (OPT_BasicBlockOperand) i.getClearOperand(0);
  }
  /**
   * Set the operand called Block in the argument
   * instruction to the argument operand. The operand will
   * now point to the argument instruction as its containing
   * instruction.
   * @param i the instruction in which to store the operand
   * @param Block the operand to store
   */
  public static void setBlock(OPT_Instruction i, OPT_BasicBlockOperand Block) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Label");
    i.putOperand(0, Block);
  }
  /**
   * Return the index of the operand called Block
   * in the argument instruction.
   * @param i the instruction to access.
   * @return the index of the operand called Block
   *         in the argument instruction
   */
  public static int indexOfBlock(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Label");
    return 0;
  }
  /**
   * Does the argument instruction have a non-null
   * operand named Block?
   * @param i the instruction to access.
   * @return <code>true</code> if the instruction has an non-null
   *         operand named Block or <code>false</code>
   *         if it does not.
   */
  public static boolean hasBlock(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Label");
    return i.getOperand(0) != null;
  }


  /**
   * Create an instruction of the Label instruction format.
   * @param o the instruction's operator
   * @param Block the instruction's Block operand
   * @return the newly created Label instruction
   */
  public static OPT_Instruction create(OPT_Operator o
                   , OPT_BasicBlockOperand Block
                )
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(o)) fail(o, "Label");
    OPT_Instruction i = new OPT_Instruction(o, 5);
    i.putOperand(0, Block);
    return i;
  }

  /**
   * Mutate the argument instruction into an instruction of the
   * Label instruction format having the specified
   * operator and operands.
   * @param i the instruction to mutate
   * @param o the instruction's operator
   * @param Block the instruction's Block operand
   * @return the mutated instruction
   */
  public static OPT_Instruction mutate(OPT_Instruction i, OPT_Operator o
                   , OPT_BasicBlockOperand Block
                )
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(o)) fail(o, "Label");
    i.operator = o;
    i.putOperand(0, Block);
    return i;
  }
}

