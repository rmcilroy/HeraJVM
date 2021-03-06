
/*
 * THIS FILE IS MACHINE_GENERATED. DO NOT EDIT.
 * See InstructionFormats.template, InstructionFormatList.dat,
 * OperatorList.dat, etc.
 */

package org.jikesrvm.compilers.opt.ir;

import org.jikesrvm.VM_Configuration;
import org.jikesrvm.compilers.opt.ir.ia32.OPT_IA32ConditionOperand; //NOPMD


/**
 * The NewArray InstructionFormat class.
 *
 * The header comment for {@link OPT_Instruction} contains
 * an explanation of the role of InstructionFormats in the
 * opt compiler's IR.
 */
@SuppressWarnings("unused")  // Machine generated code is never 100% clean
public final class NewArray extends OPT_InstructionFormat {
  /**
   * InstructionFormat identification method for NewArray.
   * @param i an instruction
   * @return <code>true</code> if the InstructionFormat of the argument
   *         instruction is NewArray or <code>false</code>
   *         if it is not.
   */
  public static boolean conforms(OPT_Instruction i) {
    return conforms(i.operator);
  }
  /**
   * InstructionFormat identification method for NewArray.
   * @param o an instruction
   * @return <code>true</code> if the InstructionFormat of the argument
   *         operator is NewArray or <code>false</code>
   *         if it is not.
   */
  public static boolean conforms(OPT_Operator o) {
    return o.format == NewArray_format;
  }

  /**
   * Get the operand called Result from the
   * argument instruction. Note that the returned operand
   * will still point to its containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Result
   */
  public static OPT_RegisterOperand getResult(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "NewArray");
    return (OPT_RegisterOperand) i.getOperand(0);
  }
  /**
   * Get the operand called Result from the argument
   * instruction clearing its instruction pointer. The returned
   * operand will not point to any containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Result
   */
  public static OPT_RegisterOperand getClearResult(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "NewArray");
    return (OPT_RegisterOperand) i.getClearOperand(0);
  }
  /**
   * Set the operand called Result in the argument
   * instruction to the argument operand. The operand will
   * now point to the argument instruction as its containing
   * instruction.
   * @param i the instruction in which to store the operand
   * @param Result the operand to store
   */
  public static void setResult(OPT_Instruction i, OPT_RegisterOperand Result) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "NewArray");
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
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "NewArray");
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
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "NewArray");
    return i.getOperand(0) != null;
  }

  /**
   * Get the operand called Type from the
   * argument instruction. Note that the returned operand
   * will still point to its containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Type
   */
  public static OPT_TypeOperand getType(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "NewArray");
    return (OPT_TypeOperand) i.getOperand(1);
  }
  /**
   * Get the operand called Type from the argument
   * instruction clearing its instruction pointer. The returned
   * operand will not point to any containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Type
   */
  public static OPT_TypeOperand getClearType(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "NewArray");
    return (OPT_TypeOperand) i.getClearOperand(1);
  }
  /**
   * Set the operand called Type in the argument
   * instruction to the argument operand. The operand will
   * now point to the argument instruction as its containing
   * instruction.
   * @param i the instruction in which to store the operand
   * @param Type the operand to store
   */
  public static void setType(OPT_Instruction i, OPT_TypeOperand Type) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "NewArray");
    i.putOperand(1, Type);
  }
  /**
   * Return the index of the operand called Type
   * in the argument instruction.
   * @param i the instruction to access.
   * @return the index of the operand called Type
   *         in the argument instruction
   */
  public static int indexOfType(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "NewArray");
    return 1;
  }
  /**
   * Does the argument instruction have a non-null
   * operand named Type?
   * @param i the instruction to access.
   * @return <code>true</code> if the instruction has an non-null
   *         operand named Type or <code>false</code>
   *         if it does not.
   */
  public static boolean hasType(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "NewArray");
    return i.getOperand(1) != null;
  }

  /**
   * Get the operand called Size from the
   * argument instruction. Note that the returned operand
   * will still point to its containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Size
   */
  public static OPT_Operand getSize(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "NewArray");
    return (OPT_Operand) i.getOperand(2);
  }
  /**
   * Get the operand called Size from the argument
   * instruction clearing its instruction pointer. The returned
   * operand will not point to any containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Size
   */
  public static OPT_Operand getClearSize(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "NewArray");
    return (OPT_Operand) i.getClearOperand(2);
  }
  /**
   * Set the operand called Size in the argument
   * instruction to the argument operand. The operand will
   * now point to the argument instruction as its containing
   * instruction.
   * @param i the instruction in which to store the operand
   * @param Size the operand to store
   */
  public static void setSize(OPT_Instruction i, OPT_Operand Size) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "NewArray");
    i.putOperand(2, Size);
  }
  /**
   * Return the index of the operand called Size
   * in the argument instruction.
   * @param i the instruction to access.
   * @return the index of the operand called Size
   *         in the argument instruction
   */
  public static int indexOfSize(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "NewArray");
    return 2;
  }
  /**
   * Does the argument instruction have a non-null
   * operand named Size?
   * @param i the instruction to access.
   * @return <code>true</code> if the instruction has an non-null
   *         operand named Size or <code>false</code>
   *         if it does not.
   */
  public static boolean hasSize(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "NewArray");
    return i.getOperand(2) != null;
  }


  /**
   * Create an instruction of the NewArray instruction format.
   * @param o the instruction's operator
   * @param Result the instruction's Result operand
   * @param Type the instruction's Type operand
   * @param Size the instruction's Size operand
   * @return the newly created NewArray instruction
   */
  public static OPT_Instruction create(OPT_Operator o
                   , OPT_RegisterOperand Result
                   , OPT_TypeOperand Type
                   , OPT_Operand Size
                )
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(o)) fail(o, "NewArray");
    OPT_Instruction i = new OPT_Instruction(o, 5);
    i.putOperand(0, Result);
    i.putOperand(1, Type);
    i.putOperand(2, Size);
    return i;
  }

  /**
   * Mutate the argument instruction into an instruction of the
   * NewArray instruction format having the specified
   * operator and operands.
   * @param i the instruction to mutate
   * @param o the instruction's operator
   * @param Result the instruction's Result operand
   * @param Type the instruction's Type operand
   * @param Size the instruction's Size operand
   * @return the mutated instruction
   */
  public static OPT_Instruction mutate(OPT_Instruction i, OPT_Operator o
                   , OPT_RegisterOperand Result
                   , OPT_TypeOperand Type
                   , OPT_Operand Size
                )
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(o)) fail(o, "NewArray");
    i.operator = o;
    i.putOperand(0, Result);
    i.putOperand(1, Type);
    i.putOperand(2, Size);
    return i;
  }
}

