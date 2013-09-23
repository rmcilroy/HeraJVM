
/*
 * THIS FILE IS MACHINE_GENERATED. DO NOT EDIT.
 * See InstructionFormats.template, InstructionFormatList.dat,
 * OperatorList.dat, etc.
 */

package org.jikesrvm.compilers.opt.ir;

import org.jikesrvm.VM_Configuration;
import org.jikesrvm.compilers.opt.ir.ia32.OPT_IA32ConditionOperand; //NOPMD


/**
 * The MIR_Return InstructionFormat class.
 *
 * The header comment for {@link OPT_Instruction} contains
 * an explanation of the role of InstructionFormats in the
 * opt compiler's IR.
 */
@SuppressWarnings("unused")  // Machine generated code is never 100% clean
public final class MIR_Return extends OPT_InstructionFormat {
  /**
   * InstructionFormat identification method for MIR_Return.
   * @param i an instruction
   * @return <code>true</code> if the InstructionFormat of the argument
   *         instruction is MIR_Return or <code>false</code>
   *         if it is not.
   */
  public static boolean conforms(OPT_Instruction i) {
    return conforms(i.operator);
  }
  /**
   * InstructionFormat identification method for MIR_Return.
   * @param o an instruction
   * @return <code>true</code> if the InstructionFormat of the argument
   *         operator is MIR_Return or <code>false</code>
   *         if it is not.
   */
  public static boolean conforms(OPT_Operator o) {
    return o.format == MIR_Return_format;
  }

  /**
   * Get the operand called PopBytes from the
   * argument instruction. Note that the returned operand
   * will still point to its containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called PopBytes
   */
  public static OPT_IntConstantOperand getPopBytes(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_Return");
    return (OPT_IntConstantOperand) i.getOperand(0);
  }
  /**
   * Get the operand called PopBytes from the argument
   * instruction clearing its instruction pointer. The returned
   * operand will not point to any containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called PopBytes
   */
  public static OPT_IntConstantOperand getClearPopBytes(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_Return");
    return (OPT_IntConstantOperand) i.getClearOperand(0);
  }
  /**
   * Set the operand called PopBytes in the argument
   * instruction to the argument operand. The operand will
   * now point to the argument instruction as its containing
   * instruction.
   * @param i the instruction in which to store the operand
   * @param PopBytes the operand to store
   */
  public static void setPopBytes(OPT_Instruction i, OPT_IntConstantOperand PopBytes) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_Return");
    i.putOperand(0, PopBytes);
  }
  /**
   * Return the index of the operand called PopBytes
   * in the argument instruction.
   * @param i the instruction to access.
   * @return the index of the operand called PopBytes
   *         in the argument instruction
   */
  public static int indexOfPopBytes(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_Return");
    return 0;
  }
  /**
   * Does the argument instruction have a non-null
   * operand named PopBytes?
   * @param i the instruction to access.
   * @return <code>true</code> if the instruction has an non-null
   *         operand named PopBytes or <code>false</code>
   *         if it does not.
   */
  public static boolean hasPopBytes(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_Return");
    return i.getOperand(0) != null;
  }

  /**
   * Get the operand called Val from the
   * argument instruction. Note that the returned operand
   * will still point to its containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Val
   */
  public static OPT_Operand getVal(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_Return");
    return (OPT_Operand) i.getOperand(1);
  }
  /**
   * Get the operand called Val from the argument
   * instruction clearing its instruction pointer. The returned
   * operand will not point to any containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Val
   */
  public static OPT_Operand getClearVal(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_Return");
    return (OPT_Operand) i.getClearOperand(1);
  }
  /**
   * Set the operand called Val in the argument
   * instruction to the argument operand. The operand will
   * now point to the argument instruction as its containing
   * instruction.
   * @param i the instruction in which to store the operand
   * @param Val the operand to store
   */
  public static void setVal(OPT_Instruction i, OPT_Operand Val) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_Return");
    i.putOperand(1, Val);
  }
  /**
   * Return the index of the operand called Val
   * in the argument instruction.
   * @param i the instruction to access.
   * @return the index of the operand called Val
   *         in the argument instruction
   */
  public static int indexOfVal(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_Return");
    return 1;
  }
  /**
   * Does the argument instruction have a non-null
   * operand named Val?
   * @param i the instruction to access.
   * @return <code>true</code> if the instruction has an non-null
   *         operand named Val or <code>false</code>
   *         if it does not.
   */
  public static boolean hasVal(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_Return");
    return i.getOperand(1) != null;
  }

  /**
   * Get the operand called Val2 from the
   * argument instruction. Note that the returned operand
   * will still point to its containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Val2
   */
  public static OPT_Operand getVal2(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_Return");
    return (OPT_Operand) i.getOperand(2);
  }
  /**
   * Get the operand called Val2 from the argument
   * instruction clearing its instruction pointer. The returned
   * operand will not point to any containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Val2
   */
  public static OPT_Operand getClearVal2(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_Return");
    return (OPT_Operand) i.getClearOperand(2);
  }
  /**
   * Set the operand called Val2 in the argument
   * instruction to the argument operand. The operand will
   * now point to the argument instruction as its containing
   * instruction.
   * @param i the instruction in which to store the operand
   * @param Val2 the operand to store
   */
  public static void setVal2(OPT_Instruction i, OPT_Operand Val2) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_Return");
    i.putOperand(2, Val2);
  }
  /**
   * Return the index of the operand called Val2
   * in the argument instruction.
   * @param i the instruction to access.
   * @return the index of the operand called Val2
   *         in the argument instruction
   */
  public static int indexOfVal2(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_Return");
    return 2;
  }
  /**
   * Does the argument instruction have a non-null
   * operand named Val2?
   * @param i the instruction to access.
   * @return <code>true</code> if the instruction has an non-null
   *         operand named Val2 or <code>false</code>
   *         if it does not.
   */
  public static boolean hasVal2(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_Return");
    return i.getOperand(2) != null;
  }


  /**
   * Create an instruction of the MIR_Return instruction format.
   * @param o the instruction's operator
   * @param PopBytes the instruction's PopBytes operand
   * @param Val the instruction's Val operand
   * @param Val2 the instruction's Val2 operand
   * @return the newly created MIR_Return instruction
   */
  public static OPT_Instruction create(OPT_Operator o
                   , OPT_IntConstantOperand PopBytes
                   , OPT_Operand Val
                   , OPT_Operand Val2
                )
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(o)) fail(o, "MIR_Return");
    OPT_Instruction i = new OPT_Instruction(o, 5);
    i.putOperand(0, PopBytes);
    i.putOperand(1, Val);
    i.putOperand(2, Val2);
    return i;
  }

  /**
   * Mutate the argument instruction into an instruction of the
   * MIR_Return instruction format having the specified
   * operator and operands.
   * @param i the instruction to mutate
   * @param o the instruction's operator
   * @param PopBytes the instruction's PopBytes operand
   * @param Val the instruction's Val operand
   * @param Val2 the instruction's Val2 operand
   * @return the mutated instruction
   */
  public static OPT_Instruction mutate(OPT_Instruction i, OPT_Operator o
                   , OPT_IntConstantOperand PopBytes
                   , OPT_Operand Val
                   , OPT_Operand Val2
                )
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(o)) fail(o, "MIR_Return");
    i.operator = o;
    i.putOperand(0, PopBytes);
    i.putOperand(1, Val);
    i.putOperand(2, Val2);
    return i;
  }
}

