
/*
 * THIS FILE IS MACHINE_GENERATED. DO NOT EDIT.
 * See InstructionFormats.template, InstructionFormatList.dat,
 * OperatorList.dat, etc.
 */

package org.jikesrvm.compilers.opt.ir;

import org.jikesrvm.VM_Configuration;
import org.jikesrvm.compilers.opt.ir.ia32.OPT_IA32ConditionOperand; //NOPMD


/**
 * The MIR_DoubleShift InstructionFormat class.
 *
 * The header comment for {@link OPT_Instruction} contains
 * an explanation of the role of InstructionFormats in the
 * opt compiler's IR.
 */
@SuppressWarnings("unused")  // Machine generated code is never 100% clean
public final class MIR_DoubleShift extends OPT_InstructionFormat {
  /**
   * InstructionFormat identification method for MIR_DoubleShift.
   * @param i an instruction
   * @return <code>true</code> if the InstructionFormat of the argument
   *         instruction is MIR_DoubleShift or <code>false</code>
   *         if it is not.
   */
  public static boolean conforms(OPT_Instruction i) {
    return conforms(i.operator);
  }
  /**
   * InstructionFormat identification method for MIR_DoubleShift.
   * @param o an instruction
   * @return <code>true</code> if the InstructionFormat of the argument
   *         operator is MIR_DoubleShift or <code>false</code>
   *         if it is not.
   */
  public static boolean conforms(OPT_Operator o) {
    return o.format == MIR_DoubleShift_format;
  }

  /**
   * Get the operand called Result from the
   * argument instruction. Note that the returned operand
   * will still point to its containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Result
   */
  public static OPT_Operand getResult(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_DoubleShift");
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
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_DoubleShift");
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
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_DoubleShift");
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
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_DoubleShift");
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
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_DoubleShift");
    return i.getOperand(0) != null;
  }

  /**
   * Get the operand called Source from the
   * argument instruction. Note that the returned operand
   * will still point to its containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Source
   */
  public static OPT_RegisterOperand getSource(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_DoubleShift");
    return (OPT_RegisterOperand) i.getOperand(1);
  }
  /**
   * Get the operand called Source from the argument
   * instruction clearing its instruction pointer. The returned
   * operand will not point to any containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Source
   */
  public static OPT_RegisterOperand getClearSource(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_DoubleShift");
    return (OPT_RegisterOperand) i.getClearOperand(1);
  }
  /**
   * Set the operand called Source in the argument
   * instruction to the argument operand. The operand will
   * now point to the argument instruction as its containing
   * instruction.
   * @param i the instruction in which to store the operand
   * @param Source the operand to store
   */
  public static void setSource(OPT_Instruction i, OPT_RegisterOperand Source) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_DoubleShift");
    i.putOperand(1, Source);
  }
  /**
   * Return the index of the operand called Source
   * in the argument instruction.
   * @param i the instruction to access.
   * @return the index of the operand called Source
   *         in the argument instruction
   */
  public static int indexOfSource(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_DoubleShift");
    return 1;
  }
  /**
   * Does the argument instruction have a non-null
   * operand named Source?
   * @param i the instruction to access.
   * @return <code>true</code> if the instruction has an non-null
   *         operand named Source or <code>false</code>
   *         if it does not.
   */
  public static boolean hasSource(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_DoubleShift");
    return i.getOperand(1) != null;
  }

  /**
   * Get the operand called BitsToShift from the
   * argument instruction. Note that the returned operand
   * will still point to its containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called BitsToShift
   */
  public static OPT_Operand getBitsToShift(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_DoubleShift");
    return (OPT_Operand) i.getOperand(2);
  }
  /**
   * Get the operand called BitsToShift from the argument
   * instruction clearing its instruction pointer. The returned
   * operand will not point to any containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called BitsToShift
   */
  public static OPT_Operand getClearBitsToShift(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_DoubleShift");
    return (OPT_Operand) i.getClearOperand(2);
  }
  /**
   * Set the operand called BitsToShift in the argument
   * instruction to the argument operand. The operand will
   * now point to the argument instruction as its containing
   * instruction.
   * @param i the instruction in which to store the operand
   * @param BitsToShift the operand to store
   */
  public static void setBitsToShift(OPT_Instruction i, OPT_Operand BitsToShift) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_DoubleShift");
    i.putOperand(2, BitsToShift);
  }
  /**
   * Return the index of the operand called BitsToShift
   * in the argument instruction.
   * @param i the instruction to access.
   * @return the index of the operand called BitsToShift
   *         in the argument instruction
   */
  public static int indexOfBitsToShift(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_DoubleShift");
    return 2;
  }
  /**
   * Does the argument instruction have a non-null
   * operand named BitsToShift?
   * @param i the instruction to access.
   * @return <code>true</code> if the instruction has an non-null
   *         operand named BitsToShift or <code>false</code>
   *         if it does not.
   */
  public static boolean hasBitsToShift(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_DoubleShift");
    return i.getOperand(2) != null;
  }


  /**
   * Create an instruction of the MIR_DoubleShift instruction format.
   * @param o the instruction's operator
   * @param Result the instruction's Result operand
   * @param Source the instruction's Source operand
   * @param BitsToShift the instruction's BitsToShift operand
   * @return the newly created MIR_DoubleShift instruction
   */
  public static OPT_Instruction create(OPT_Operator o
                   , OPT_Operand Result
                   , OPT_RegisterOperand Source
                   , OPT_Operand BitsToShift
                )
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(o)) fail(o, "MIR_DoubleShift");
    OPT_Instruction i = new OPT_Instruction(o, 5);
    i.putOperand(0, Result);
    i.putOperand(1, Source);
    i.putOperand(2, BitsToShift);
    return i;
  }

  /**
   * Mutate the argument instruction into an instruction of the
   * MIR_DoubleShift instruction format having the specified
   * operator and operands.
   * @param i the instruction to mutate
   * @param o the instruction's operator
   * @param Result the instruction's Result operand
   * @param Source the instruction's Source operand
   * @param BitsToShift the instruction's BitsToShift operand
   * @return the mutated instruction
   */
  public static OPT_Instruction mutate(OPT_Instruction i, OPT_Operator o
                   , OPT_Operand Result
                   , OPT_RegisterOperand Source
                   , OPT_Operand BitsToShift
                )
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(o)) fail(o, "MIR_DoubleShift");
    i.operator = o;
    i.putOperand(0, Result);
    i.putOperand(1, Source);
    i.putOperand(2, BitsToShift);
    return i;
  }
}

