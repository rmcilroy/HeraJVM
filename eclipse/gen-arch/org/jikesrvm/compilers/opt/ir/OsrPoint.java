
/*
 * THIS FILE IS MACHINE_GENERATED. DO NOT EDIT.
 * See InstructionFormats.template, InstructionFormatList.dat,
 * OperatorList.dat, etc.
 */

package org.jikesrvm.compilers.opt.ir;

import org.jikesrvm.VM_Configuration;
import org.jikesrvm.compilers.opt.ir.ia32.OPT_IA32ConditionOperand; //NOPMD


/**
 * The OsrPoint InstructionFormat class.
 *
 * The header comment for {@link OPT_Instruction} contains
 * an explanation of the role of InstructionFormats in the
 * opt compiler's IR.
 */
@SuppressWarnings("unused")  // Machine generated code is never 100% clean
public final class OsrPoint extends OPT_InstructionFormat {
  /**
   * InstructionFormat identification method for OsrPoint.
   * @param i an instruction
   * @return <code>true</code> if the InstructionFormat of the argument
   *         instruction is OsrPoint or <code>false</code>
   *         if it is not.
   */
  public static boolean conforms(OPT_Instruction i) {
    return conforms(i.operator);
  }
  /**
   * InstructionFormat identification method for OsrPoint.
   * @param o an instruction
   * @return <code>true</code> if the InstructionFormat of the argument
   *         operator is OsrPoint or <code>false</code>
   *         if it is not.
   */
  public static boolean conforms(OPT_Operator o) {
    return o.format == OsrPoint_format;
  }

  /**
   * Get the operand called InlinedTypeInfo from the
   * argument instruction. Note that the returned operand
   * will still point to its containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called InlinedTypeInfo
   */
  public static OPT_InlinedOsrTypeInfoOperand getInlinedTypeInfo(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "OsrPoint");
    return (OPT_InlinedOsrTypeInfoOperand) i.getOperand(0);
  }
  /**
   * Get the operand called InlinedTypeInfo from the argument
   * instruction clearing its instruction pointer. The returned
   * operand will not point to any containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called InlinedTypeInfo
   */
  public static OPT_InlinedOsrTypeInfoOperand getClearInlinedTypeInfo(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "OsrPoint");
    return (OPT_InlinedOsrTypeInfoOperand) i.getClearOperand(0);
  }
  /**
   * Set the operand called InlinedTypeInfo in the argument
   * instruction to the argument operand. The operand will
   * now point to the argument instruction as its containing
   * instruction.
   * @param i the instruction in which to store the operand
   * @param InlinedTypeInfo the operand to store
   */
  public static void setInlinedTypeInfo(OPT_Instruction i, OPT_InlinedOsrTypeInfoOperand InlinedTypeInfo) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "OsrPoint");
    i.putOperand(0, InlinedTypeInfo);
  }
  /**
   * Return the index of the operand called InlinedTypeInfo
   * in the argument instruction.
   * @param i the instruction to access.
   * @return the index of the operand called InlinedTypeInfo
   *         in the argument instruction
   */
  public static int indexOfInlinedTypeInfo(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "OsrPoint");
    return 0;
  }
  /**
   * Does the argument instruction have a non-null
   * operand named InlinedTypeInfo?
   * @param i the instruction to access.
   * @return <code>true</code> if the instruction has an non-null
   *         operand named InlinedTypeInfo or <code>false</code>
   *         if it does not.
   */
  public static boolean hasInlinedTypeInfo(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "OsrPoint");
    return i.getOperand(0) != null;
  }

  /**
   * Get the k'th operand called Element from the
   * argument instruction. Note that the returned operand
   * will still point to its containing instruction.
   * @param i the instruction to fetch the operand from
   * @param k the index of the operand
   * @return the k'th operand called Element
   */
  public static OPT_Operand getElement(OPT_Instruction i, int k) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "OsrPoint");
    return (OPT_Operand) i.getOperand(1+k);
  }
  /**
   * Get the k'th operand called Element from the argument
   * instruction clearing its instruction pointer. The returned
   * operand will not point to any containing instruction.
   * @param i the instruction to fetch the operand from
   * @param k the index of the operand
   * @return the k'th operand called Element
   */
  public static OPT_Operand getClearElement(OPT_Instruction i, int k) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "OsrPoint");
    return (OPT_Operand) i.getClearOperand(1+k);
  }
  /**
   * Set the k'th operand called Element in the argument
   * instruction to the argument operand. The operand will
   * now point to the argument instruction as its containing
   * instruction.
   * @param i the instruction in which to store the operand
   * @param k the index of the operand
   * @param o the operand to store
   */
  public static void setElement(OPT_Instruction i, int k, OPT_Operand o) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "OsrPoint");
    i.putOperand(1+k, o);
  }
  /**
   * Return the index of the k'th operand called Element
   * in the argument instruction.
   * @param i the instruction to access.
   * @param k the index of the operand.
   * @return the index of the k'th operand called Element
   *         in the argument instruction
   */
  public static int indexOfElement(OPT_Instruction i, int k) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "OsrPoint");
    return 1+k;
  }
  /**
   * Does the argument instruction have a non-null
   * k'th operand named Element?
   * @param i the instruction to access.
   * @param k the index of the operand.
   * @return <code>true</code> if the instruction has an non-null
   *         k'th operand named Element or <code>false</code>
   *         if it does not.
   */
  public static boolean hasElement(OPT_Instruction i, int k) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "OsrPoint");
    return i.getOperand(1+k) != null;
  }

  /**
   * Return the index of the first operand called Element
   * in the argument instruction.
   * @param i the instruction to access.
   * @return the index of the first operand called Element
   *         in the argument instruction
   */
  public static int indexOfElements(OPT_Instruction i)
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "OsrPoint");
    return 1;
  }
  /**
   * Does the argument instruction have any operands
   * named Element?
   * @param i the instruction to access.
   * @return <code>true</code> if the instruction has operands
   *         named Element or <code>false</code> if it does not.
   */
  public static boolean hasElements(OPT_Instruction i)
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "OsrPoint");
    return i.getNumberOfOperands()-1 > 0 && i.getOperand(1) != null;
  }

  /**
   * How many variable-length operands called Elements
   * does the argument instruction have?
   * @param i the instruction to access
   * @return the number of operands called Elements the instruction has
   */
  public static int getNumberOfElements(OPT_Instruction i)
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "OsrPoint");
    return i.getNumberOfOperands()-1;
  }

  /**
   * Change the number of Elements that may be stored in
   * the argument instruction to numVarOps.
   * @param i the instruction to access
   * @param numVarOps the new number of variable operands called Elements
   *        that may be stored in the instruction
   */
  public static void resizeNumberOfElements(OPT_Instruction i, int numVarOps)
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "OsrPoint");
  if (1+numVarOps>MIN_OPERAND_ARRAY_LENGTH)
    i.resizeNumberOfOperands(1+numVarOps);
  else
    for (int j = 1+numVarOps; j < MIN_OPERAND_ARRAY_LENGTH; j++)
      i.putOperand(j, null);
  }

  /**
   * Create an instruction of the OsrPoint instruction format.
   * @param o the instruction's operator
   * @param InlinedTypeInfo the instruction's InlinedTypeInfo operand
   * @param numVarOps the number of variable length operands that
   *                 will be stored in the insruction.
   * @return the newly created OsrPoint instruction
   */
  public static OPT_Instruction create(OPT_Operator o
                   , OPT_InlinedOsrTypeInfoOperand InlinedTypeInfo
                   , int numVarOps
                )
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(o)) fail(o, "OsrPoint");
    OPT_Instruction i = new OPT_Instruction(o, Math.max(1+numVarOps, MIN_OPERAND_ARRAY_LENGTH));
    i.putOperand(0, InlinedTypeInfo);
    return i;
  }

  /**
   * Mutate the argument instruction into an instruction of the
   * OsrPoint instruction format having the specified
   * operator and operands.
   * @param i the instruction to mutate
   * @param o the instruction's operator
   * @param InlinedTypeInfo the instruction's InlinedTypeInfo operand
   * @param numVarOps the number of variable length operands that
   *                  will be stored in the insruction.
   * @return the mutated instruction
   */
  public static OPT_Instruction mutate(OPT_Instruction i, OPT_Operator o
                   , OPT_InlinedOsrTypeInfoOperand InlinedTypeInfo
                   , int numVarOps
                )
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(o)) fail(o, "OsrPoint");
    if (1+numVarOps>MIN_OPERAND_ARRAY_LENGTH)
      i.resizeNumberOfOperands(1+numVarOps);

    i.operator = o;
    i.putOperand(0, InlinedTypeInfo);
    return i;
  }
}

