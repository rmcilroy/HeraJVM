
/*
 * THIS FILE IS MACHINE_GENERATED. DO NOT EDIT.
 * See InstructionFormats.template, InstructionFormatList.dat,
 * OperatorList.dat, etc.
 */

package org.jikesrvm.compilers.opt.ir;

import org.jikesrvm.VM_Configuration;
import org.jikesrvm.compilers.opt.ir.ia32.OPT_IA32ConditionOperand; //NOPMD


/**
 * The Multianewarray InstructionFormat class.
 *
 * The header comment for {@link OPT_Instruction} contains
 * an explanation of the role of InstructionFormats in the
 * opt compiler's IR.
 */
@SuppressWarnings("unused")  // Machine generated code is never 100% clean
public final class Multianewarray extends OPT_InstructionFormat {
  /**
   * InstructionFormat identification method for Multianewarray.
   * @param i an instruction
   * @return <code>true</code> if the InstructionFormat of the argument
   *         instruction is Multianewarray or <code>false</code>
   *         if it is not.
   */
  public static boolean conforms(OPT_Instruction i) {
    return conforms(i.operator);
  }
  /**
   * InstructionFormat identification method for Multianewarray.
   * @param o an instruction
   * @return <code>true</code> if the InstructionFormat of the argument
   *         operator is Multianewarray or <code>false</code>
   *         if it is not.
   */
  public static boolean conforms(OPT_Operator o) {
    return o.format == Multianewarray_format;
  }

  /**
   * Get the operand called Result from the
   * argument instruction. Note that the returned operand
   * will still point to its containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Result
   */
  public static OPT_RegisterOperand getResult(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Multianewarray");
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
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Multianewarray");
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
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Multianewarray");
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
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Multianewarray");
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
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Multianewarray");
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
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Multianewarray");
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
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Multianewarray");
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
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Multianewarray");
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
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Multianewarray");
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
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Multianewarray");
    return i.getOperand(1) != null;
  }

  /**
   * Get the k'th operand called Dimension from the
   * argument instruction. Note that the returned operand
   * will still point to its containing instruction.
   * @param i the instruction to fetch the operand from
   * @param k the index of the operand
   * @return the k'th operand called Dimension
   */
  public static OPT_Operand getDimension(OPT_Instruction i, int k) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Multianewarray");
    return (OPT_Operand) i.getOperand(2+k);
  }
  /**
   * Get the k'th operand called Dimension from the argument
   * instruction clearing its instruction pointer. The returned
   * operand will not point to any containing instruction.
   * @param i the instruction to fetch the operand from
   * @param k the index of the operand
   * @return the k'th operand called Dimension
   */
  public static OPT_Operand getClearDimension(OPT_Instruction i, int k) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Multianewarray");
    return (OPT_Operand) i.getClearOperand(2+k);
  }
  /**
   * Set the k'th operand called Dimension in the argument
   * instruction to the argument operand. The operand will
   * now point to the argument instruction as its containing
   * instruction.
   * @param i the instruction in which to store the operand
   * @param k the index of the operand
   * @param o the operand to store
   */
  public static void setDimension(OPT_Instruction i, int k, OPT_Operand o) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Multianewarray");
    i.putOperand(2+k, o);
  }
  /**
   * Return the index of the k'th operand called Dimension
   * in the argument instruction.
   * @param i the instruction to access.
   * @param k the index of the operand.
   * @return the index of the k'th operand called Dimension
   *         in the argument instruction
   */
  public static int indexOfDimension(OPT_Instruction i, int k) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Multianewarray");
    return 2+k;
  }
  /**
   * Does the argument instruction have a non-null
   * k'th operand named Dimension?
   * @param i the instruction to access.
   * @param k the index of the operand.
   * @return <code>true</code> if the instruction has an non-null
   *         k'th operand named Dimension or <code>false</code>
   *         if it does not.
   */
  public static boolean hasDimension(OPT_Instruction i, int k) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Multianewarray");
    return i.getOperand(2+k) != null;
  }

  /**
   * Return the index of the first operand called Dimension
   * in the argument instruction.
   * @param i the instruction to access.
   * @return the index of the first operand called Dimension
   *         in the argument instruction
   */
  public static int indexOfDimensions(OPT_Instruction i)
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Multianewarray");
    return 2;
  }
  /**
   * Does the argument instruction have any operands
   * named Dimension?
   * @param i the instruction to access.
   * @return <code>true</code> if the instruction has operands
   *         named Dimension or <code>false</code> if it does not.
   */
  public static boolean hasDimensions(OPT_Instruction i)
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Multianewarray");
    return i.getNumberOfOperands()-2 > 0 && i.getOperand(2) != null;
  }

  /**
   * How many variable-length operands called Dimensions
   * does the argument instruction have?
   * @param i the instruction to access
   * @return the number of operands called Dimensions the instruction has
   */
  public static int getNumberOfDimensions(OPT_Instruction i)
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Multianewarray");
    return i.getNumberOfOperands()-2;
  }

  /**
   * Change the number of Dimensions that may be stored in
   * the argument instruction to numVarOps.
   * @param i the instruction to access
   * @param numVarOps the new number of variable operands called Dimensions
   *        that may be stored in the instruction
   */
  public static void resizeNumberOfDimensions(OPT_Instruction i, int numVarOps)
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Multianewarray");
  if (2+numVarOps>MIN_OPERAND_ARRAY_LENGTH)
    i.resizeNumberOfOperands(2+numVarOps);
  else
    for (int j = 2+numVarOps; j < MIN_OPERAND_ARRAY_LENGTH; j++)
      i.putOperand(j, null);
  }

  /**
   * Create an instruction of the Multianewarray instruction format.
   * @param o the instruction's operator
   * @param Result the instruction's Result operand
   * @param Type the instruction's Type operand
   * @param numVarOps the number of variable length operands that
   *                 will be stored in the insruction.
   * @return the newly created Multianewarray instruction
   */
  public static OPT_Instruction create(OPT_Operator o
                   , OPT_RegisterOperand Result
                   , OPT_TypeOperand Type
                   , int numVarOps
                )
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(o)) fail(o, "Multianewarray");
    OPT_Instruction i = new OPT_Instruction(o, Math.max(2+numVarOps, MIN_OPERAND_ARRAY_LENGTH));
    i.putOperand(0, Result);
    i.putOperand(1, Type);
    return i;
  }

  /**
   * Mutate the argument instruction into an instruction of the
   * Multianewarray instruction format having the specified
   * operator and operands.
   * @param i the instruction to mutate
   * @param o the instruction's operator
   * @param Result the instruction's Result operand
   * @param Type the instruction's Type operand
   * @param numVarOps the number of variable length operands that
   *                  will be stored in the insruction.
   * @return the mutated instruction
   */
  public static OPT_Instruction mutate(OPT_Instruction i, OPT_Operator o
                   , OPT_RegisterOperand Result
                   , OPT_TypeOperand Type
                   , int numVarOps
                )
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(o)) fail(o, "Multianewarray");
    if (2+numVarOps>MIN_OPERAND_ARRAY_LENGTH)
      i.resizeNumberOfOperands(2+numVarOps);

    i.operator = o;
    i.putOperand(0, Result);
    i.putOperand(1, Type);
    return i;
  }
}

