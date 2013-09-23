
/*
 * THIS FILE IS MACHINE_GENERATED. DO NOT EDIT.
 * See InstructionFormats.template, InstructionFormatList.dat,
 * OperatorList.dat, etc.
 */

package org.jikesrvm.compilers.opt.ir;

import org.jikesrvm.VM_Configuration;
import org.jikesrvm.compilers.opt.ir.ia32.OPT_IA32ConditionOperand; //NOPMD


/**
 * The Phi InstructionFormat class.
 *
 * The header comment for {@link OPT_Instruction} contains
 * an explanation of the role of InstructionFormats in the
 * opt compiler's IR.
 */
@SuppressWarnings("unused")  // Machine generated code is never 100% clean
public final class Phi extends OPT_InstructionFormat {
  /**
   * InstructionFormat identification method for Phi.
   * @param i an instruction
   * @return <code>true</code> if the InstructionFormat of the argument
   *         instruction is Phi or <code>false</code>
   *         if it is not.
   */
  public static boolean conforms(OPT_Instruction i) {
    return conforms(i.operator);
  }
  /**
   * InstructionFormat identification method for Phi.
   * @param o an instruction
   * @return <code>true</code> if the InstructionFormat of the argument
   *         operator is Phi or <code>false</code>
   *         if it is not.
   */
  public static boolean conforms(OPT_Operator o) {
    return o.format == Phi_format;
  }

  /**
   * Get the operand called Result from the
   * argument instruction. Note that the returned operand
   * will still point to its containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Result
   */
  public static OPT_Operand getResult(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Phi");
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
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Phi");
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
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Phi");
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
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Phi");
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
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Phi");
    return i.getOperand(0) != null;
  }

  /**
   * Get the k'th operand called Value from the
   * argument instruction. Note that the returned operand
   * will still point to its containing instruction.
   * @param i the instruction to fetch the operand from
   * @param k the index of the operand
   * @return the k'th operand called Value
   */
  public static OPT_Operand getValue(OPT_Instruction i, int k) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Phi");
    return (OPT_Operand) i.getOperand(1+k*2+0);
  }
  /**
   * Get the k'th operand called Value from the argument
   * instruction clearing its instruction pointer. The returned
   * operand will not point to any containing instruction.
   * @param i the instruction to fetch the operand from
   * @param k the index of the operand
   * @return the k'th operand called Value
   */
  public static OPT_Operand getClearValue(OPT_Instruction i, int k) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Phi");
    return (OPT_Operand) i.getClearOperand(1+k*2+0);
  }
  /**
   * Set the k'th operand called Value in the argument
   * instruction to the argument operand. The operand will
   * now point to the argument instruction as its containing
   * instruction.
   * @param i the instruction in which to store the operand
   * @param k the index of the operand
   * @param o the operand to store
   */
  public static void setValue(OPT_Instruction i, int k, OPT_Operand o) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Phi");
    i.putOperand(1+k*2+0, o);
  }
  /**
   * Return the index of the k'th operand called Value
   * in the argument instruction.
   * @param i the instruction to access.
   * @param k the index of the operand.
   * @return the index of the k'th operand called Value
   *         in the argument instruction
   */
  public static int indexOfValue(OPT_Instruction i, int k) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Phi");
    return 1+k*2+0;
  }
  /**
   * Does the argument instruction have a non-null
   * k'th operand named Value?
   * @param i the instruction to access.
   * @param k the index of the operand.
   * @return <code>true</code> if the instruction has an non-null
   *         k'th operand named Value or <code>false</code>
   *         if it does not.
   */
  public static boolean hasValue(OPT_Instruction i, int k) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Phi");
    return i.getOperand(1+k*2+0) != null;
  }

  /**
   * Return the index of the first operand called Value
   * in the argument instruction.
   * @param i the instruction to access.
   * @return the index of the first operand called Value
   *         in the argument instruction
   */
  public static int indexOfValues(OPT_Instruction i)
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Phi");
    return 1;
  }
  /**
   * Does the argument instruction have any operands
   * named Value?
   * @param i the instruction to access.
   * @return <code>true</code> if the instruction has operands
   *         named Value or <code>false</code> if it does not.
   */
  public static boolean hasValues(OPT_Instruction i)
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Phi");
    return i.getNumberOfOperands()-1 > 0 && i.getOperand(1) != null;
  }

  /**
   * How many variable-length operands called Values
   * does the argument instruction have?
   * @param i the instruction to access
   * @return the number of operands called Values the instruction has
   */
  public static int getNumberOfValues(OPT_Instruction i)
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Phi");
    return (i.getNumberOfOperands()-1)/2;
  }

  /**
   * Change the number of Values that may be stored in
   * the argument instruction to numVarOps.
   * @param i the instruction to access
   * @param numVarOps the new number of variable operands called Values
   *        that may be stored in the instruction
   */
  public static void resizeNumberOfValues(OPT_Instruction i, int numVarOps)
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Phi");
  if (1+numVarOps*2>MIN_OPERAND_ARRAY_LENGTH)
    i.resizeNumberOfOperands(1+numVarOps*2);
  else
    for (int j = 1+numVarOps*2; j < MIN_OPERAND_ARRAY_LENGTH; j++)
      i.putOperand(j, null);
  }
  /**
   * Get the k'th operand called Pred from the
   * argument instruction. Note that the returned operand
   * will still point to its containing instruction.
   * @param i the instruction to fetch the operand from
   * @param k the index of the operand
   * @return the k'th operand called Pred
   */
  public static OPT_BasicBlockOperand getPred(OPT_Instruction i, int k) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Phi");
    return (OPT_BasicBlockOperand) i.getOperand(1+k*2+1);
  }
  /**
   * Get the k'th operand called Pred from the argument
   * instruction clearing its instruction pointer. The returned
   * operand will not point to any containing instruction.
   * @param i the instruction to fetch the operand from
   * @param k the index of the operand
   * @return the k'th operand called Pred
   */
  public static OPT_BasicBlockOperand getClearPred(OPT_Instruction i, int k) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Phi");
    return (OPT_BasicBlockOperand) i.getClearOperand(1+k*2+1);
  }
  /**
   * Set the k'th operand called Pred in the argument
   * instruction to the argument operand. The operand will
   * now point to the argument instruction as its containing
   * instruction.
   * @param i the instruction in which to store the operand
   * @param k the index of the operand
   * @param o the operand to store
   */
  public static void setPred(OPT_Instruction i, int k, OPT_BasicBlockOperand o) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Phi");
    i.putOperand(1+k*2+1, o);
  }
  /**
   * Return the index of the k'th operand called Pred
   * in the argument instruction.
   * @param i the instruction to access.
   * @param k the index of the operand.
   * @return the index of the k'th operand called Pred
   *         in the argument instruction
   */
  public static int indexOfPred(OPT_Instruction i, int k) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Phi");
    return 1+k*2+1;
  }
  /**
   * Does the argument instruction have a non-null
   * k'th operand named Pred?
   * @param i the instruction to access.
   * @param k the index of the operand.
   * @return <code>true</code> if the instruction has an non-null
   *         k'th operand named Pred or <code>false</code>
   *         if it does not.
   */
  public static boolean hasPred(OPT_Instruction i, int k) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Phi");
    return i.getOperand(1+k*2+1) != null;
  }

  /**
   * Return the index of the first operand called Pred
   * in the argument instruction.
   * @param i the instruction to access.
   * @return the index of the first operand called Pred
   *         in the argument instruction
   */
  public static int indexOfPreds(OPT_Instruction i)
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Phi");
    return 2;
  }
  /**
   * Does the argument instruction have any operands
   * named Pred?
   * @param i the instruction to access.
   * @return <code>true</code> if the instruction has operands
   *         named Pred or <code>false</code> if it does not.
   */
  public static boolean hasPreds(OPT_Instruction i)
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Phi");
    return i.getNumberOfOperands()-2 > 0 && i.getOperand(2) != null;
  }

  /**
   * How many variable-length operands called Preds
   * does the argument instruction have?
   * @param i the instruction to access
   * @return the number of operands called Preds the instruction has
   */
  public static int getNumberOfPreds(OPT_Instruction i)
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Phi");
    return (i.getNumberOfOperands()-1)/2;
  }

  /**
   * Change the number of Preds that may be stored in
   * the argument instruction to numVarOps.
   * @param i the instruction to access
   * @param numVarOps the new number of variable operands called Preds
   *        that may be stored in the instruction
   */
  public static void resizeNumberOfPreds(OPT_Instruction i, int numVarOps)
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Phi");
  if (1+numVarOps*2>MIN_OPERAND_ARRAY_LENGTH)
    i.resizeNumberOfOperands(1+numVarOps*2);
  else
    for (int j = 1+numVarOps*2; j < MIN_OPERAND_ARRAY_LENGTH; j++)
      i.putOperand(j, null);
  }

  /**
   * Create an instruction of the Phi instruction format.
   * @param o the instruction's operator
   * @param Result the instruction's Result operand
   * @param numVarOps the number of variable length operands that
   *                 will be stored in the insruction.
   * @return the newly created Phi instruction
   */
  public static OPT_Instruction create(OPT_Operator o
                   , OPT_Operand Result
                   , int numVarOps
                )
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(o)) fail(o, "Phi");
    OPT_Instruction i = new OPT_Instruction(o, Math.max(1+numVarOps*2, MIN_OPERAND_ARRAY_LENGTH));
    i.putOperand(0, Result);
    return i;
  }

  /**
   * Mutate the argument instruction into an instruction of the
   * Phi instruction format having the specified
   * operator and operands.
   * @param i the instruction to mutate
   * @param o the instruction's operator
   * @param Result the instruction's Result operand
   * @param numVarOps the number of variable length operands that
   *                  will be stored in the insruction.
   * @return the mutated instruction
   */
  public static OPT_Instruction mutate(OPT_Instruction i, OPT_Operator o
                   , OPT_Operand Result
                   , int numVarOps
                )
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(o)) fail(o, "Phi");
    if (1+numVarOps*2>MIN_OPERAND_ARRAY_LENGTH)
      i.resizeNumberOfOperands(1+numVarOps*2);

    i.operator = o;
    i.putOperand(0, Result);
    return i;
  }
}

