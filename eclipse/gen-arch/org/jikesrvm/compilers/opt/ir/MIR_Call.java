
/*
 * THIS FILE IS MACHINE_GENERATED. DO NOT EDIT.
 * See InstructionFormats.template, InstructionFormatList.dat,
 * OperatorList.dat, etc.
 */

package org.jikesrvm.compilers.opt.ir;

import org.jikesrvm.VM_Configuration;
import org.jikesrvm.compilers.opt.ir.ia32.OPT_IA32ConditionOperand; //NOPMD


/**
 * The MIR_Call InstructionFormat class.
 *
 * The header comment for {@link OPT_Instruction} contains
 * an explanation of the role of InstructionFormats in the
 * opt compiler's IR.
 */
@SuppressWarnings("unused")  // Machine generated code is never 100% clean
public final class MIR_Call extends OPT_InstructionFormat {
  /**
   * InstructionFormat identification method for MIR_Call.
   * @param i an instruction
   * @return <code>true</code> if the InstructionFormat of the argument
   *         instruction is MIR_Call or <code>false</code>
   *         if it is not.
   */
  public static boolean conforms(OPT_Instruction i) {
    return conforms(i.operator);
  }
  /**
   * InstructionFormat identification method for MIR_Call.
   * @param o an instruction
   * @return <code>true</code> if the InstructionFormat of the argument
   *         operator is MIR_Call or <code>false</code>
   *         if it is not.
   */
  public static boolean conforms(OPT_Operator o) {
    return o.format == MIR_Call_format;
  }

  /**
   * Get the operand called Result from the
   * argument instruction. Note that the returned operand
   * will still point to its containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Result
   */
  public static OPT_RegisterOperand getResult(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_Call");
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
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_Call");
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
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_Call");
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
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_Call");
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
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_Call");
    return i.getOperand(0) != null;
  }

  /**
   * Get the operand called Result2 from the
   * argument instruction. Note that the returned operand
   * will still point to its containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Result2
   */
  public static OPT_RegisterOperand getResult2(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_Call");
    return (OPT_RegisterOperand) i.getOperand(1);
  }
  /**
   * Get the operand called Result2 from the argument
   * instruction clearing its instruction pointer. The returned
   * operand will not point to any containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Result2
   */
  public static OPT_RegisterOperand getClearResult2(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_Call");
    return (OPT_RegisterOperand) i.getClearOperand(1);
  }
  /**
   * Set the operand called Result2 in the argument
   * instruction to the argument operand. The operand will
   * now point to the argument instruction as its containing
   * instruction.
   * @param i the instruction in which to store the operand
   * @param Result2 the operand to store
   */
  public static void setResult2(OPT_Instruction i, OPT_RegisterOperand Result2) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_Call");
    i.putOperand(1, Result2);
  }
  /**
   * Return the index of the operand called Result2
   * in the argument instruction.
   * @param i the instruction to access.
   * @return the index of the operand called Result2
   *         in the argument instruction
   */
  public static int indexOfResult2(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_Call");
    return 1;
  }
  /**
   * Does the argument instruction have a non-null
   * operand named Result2?
   * @param i the instruction to access.
   * @return <code>true</code> if the instruction has an non-null
   *         operand named Result2 or <code>false</code>
   *         if it does not.
   */
  public static boolean hasResult2(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_Call");
    return i.getOperand(1) != null;
  }

  /**
   * Get the operand called Target from the
   * argument instruction. Note that the returned operand
   * will still point to its containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Target
   */
  public static OPT_Operand getTarget(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_Call");
    return (OPT_Operand) i.getOperand(2);
  }
  /**
   * Get the operand called Target from the argument
   * instruction clearing its instruction pointer. The returned
   * operand will not point to any containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Target
   */
  public static OPT_Operand getClearTarget(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_Call");
    return (OPT_Operand) i.getClearOperand(2);
  }
  /**
   * Set the operand called Target in the argument
   * instruction to the argument operand. The operand will
   * now point to the argument instruction as its containing
   * instruction.
   * @param i the instruction in which to store the operand
   * @param Target the operand to store
   */
  public static void setTarget(OPT_Instruction i, OPT_Operand Target) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_Call");
    i.putOperand(2, Target);
  }
  /**
   * Return the index of the operand called Target
   * in the argument instruction.
   * @param i the instruction to access.
   * @return the index of the operand called Target
   *         in the argument instruction
   */
  public static int indexOfTarget(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_Call");
    return 2;
  }
  /**
   * Does the argument instruction have a non-null
   * operand named Target?
   * @param i the instruction to access.
   * @return <code>true</code> if the instruction has an non-null
   *         operand named Target or <code>false</code>
   *         if it does not.
   */
  public static boolean hasTarget(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_Call");
    return i.getOperand(2) != null;
  }

  /**
   * Get the operand called Method from the
   * argument instruction. Note that the returned operand
   * will still point to its containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Method
   */
  public static OPT_MethodOperand getMethod(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_Call");
    return (OPT_MethodOperand) i.getOperand(3);
  }
  /**
   * Get the operand called Method from the argument
   * instruction clearing its instruction pointer. The returned
   * operand will not point to any containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Method
   */
  public static OPT_MethodOperand getClearMethod(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_Call");
    return (OPT_MethodOperand) i.getClearOperand(3);
  }
  /**
   * Set the operand called Method in the argument
   * instruction to the argument operand. The operand will
   * now point to the argument instruction as its containing
   * instruction.
   * @param i the instruction in which to store the operand
   * @param Method the operand to store
   */
  public static void setMethod(OPT_Instruction i, OPT_MethodOperand Method) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_Call");
    i.putOperand(3, Method);
  }
  /**
   * Return the index of the operand called Method
   * in the argument instruction.
   * @param i the instruction to access.
   * @return the index of the operand called Method
   *         in the argument instruction
   */
  public static int indexOfMethod(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_Call");
    return 3;
  }
  /**
   * Does the argument instruction have a non-null
   * operand named Method?
   * @param i the instruction to access.
   * @return <code>true</code> if the instruction has an non-null
   *         operand named Method or <code>false</code>
   *         if it does not.
   */
  public static boolean hasMethod(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_Call");
    return i.getOperand(3) != null;
  }

  /**
   * Get the k'th operand called Param from the
   * argument instruction. Note that the returned operand
   * will still point to its containing instruction.
   * @param i the instruction to fetch the operand from
   * @param k the index of the operand
   * @return the k'th operand called Param
   */
  public static OPT_Operand getParam(OPT_Instruction i, int k) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_Call");
    return (OPT_Operand) i.getOperand(4+k);
  }
  /**
   * Get the k'th operand called Param from the argument
   * instruction clearing its instruction pointer. The returned
   * operand will not point to any containing instruction.
   * @param i the instruction to fetch the operand from
   * @param k the index of the operand
   * @return the k'th operand called Param
   */
  public static OPT_Operand getClearParam(OPT_Instruction i, int k) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_Call");
    return (OPT_Operand) i.getClearOperand(4+k);
  }
  /**
   * Set the k'th operand called Param in the argument
   * instruction to the argument operand. The operand will
   * now point to the argument instruction as its containing
   * instruction.
   * @param i the instruction in which to store the operand
   * @param k the index of the operand
   * @param o the operand to store
   */
  public static void setParam(OPT_Instruction i, int k, OPT_Operand o) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_Call");
    i.putOperand(4+k, o);
  }
  /**
   * Return the index of the k'th operand called Param
   * in the argument instruction.
   * @param i the instruction to access.
   * @param k the index of the operand.
   * @return the index of the k'th operand called Param
   *         in the argument instruction
   */
  public static int indexOfParam(OPT_Instruction i, int k) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_Call");
    return 4+k;
  }
  /**
   * Does the argument instruction have a non-null
   * k'th operand named Param?
   * @param i the instruction to access.
   * @param k the index of the operand.
   * @return <code>true</code> if the instruction has an non-null
   *         k'th operand named Param or <code>false</code>
   *         if it does not.
   */
  public static boolean hasParam(OPT_Instruction i, int k) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_Call");
    return i.getOperand(4+k) != null;
  }

  /**
   * Return the index of the first operand called Param
   * in the argument instruction.
   * @param i the instruction to access.
   * @return the index of the first operand called Param
   *         in the argument instruction
   */
  public static int indexOfParams(OPT_Instruction i)
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_Call");
    return 4;
  }
  /**
   * Does the argument instruction have any operands
   * named Param?
   * @param i the instruction to access.
   * @return <code>true</code> if the instruction has operands
   *         named Param or <code>false</code> if it does not.
   */
  public static boolean hasParams(OPT_Instruction i)
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_Call");
    return i.getNumberOfOperands()-4 > 0 && i.getOperand(4) != null;
  }

  /**
   * How many variable-length operands called Params
   * does the argument instruction have?
   * @param i the instruction to access
   * @return the number of operands called Params the instruction has
   */
  public static int getNumberOfParams(OPT_Instruction i)
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_Call");
    return i.getNumberOfOperands()-4;
  }

  /**
   * Change the number of Params that may be stored in
   * the argument instruction to numVarOps.
   * @param i the instruction to access
   * @param numVarOps the new number of variable operands called Params
   *        that may be stored in the instruction
   */
  public static void resizeNumberOfParams(OPT_Instruction i, int numVarOps)
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_Call");
  if (4+numVarOps>MIN_OPERAND_ARRAY_LENGTH)
    i.resizeNumberOfOperands(4+numVarOps);
  else
    for (int j = 4+numVarOps; j < MIN_OPERAND_ARRAY_LENGTH; j++)
      i.putOperand(j, null);
  }

  /**
   * Create an instruction of the MIR_Call instruction format.
   * @param o the instruction's operator
   * @param Result the instruction's Result operand
   * @param Result2 the instruction's Result2 operand
   * @param Target the instruction's Target operand
   * @param Method the instruction's Method operand
   * @param numVarOps the number of variable length operands that
   *                 will be stored in the insruction.
   * @return the newly created MIR_Call instruction
   */
  public static OPT_Instruction create(OPT_Operator o
                   , OPT_RegisterOperand Result
                   , OPT_RegisterOperand Result2
                   , OPT_Operand Target
                   , OPT_MethodOperand Method
                   , int numVarOps
                )
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(o)) fail(o, "MIR_Call");
    OPT_Instruction i = new OPT_Instruction(o, Math.max(4+numVarOps, MIN_OPERAND_ARRAY_LENGTH));
    i.putOperand(0, Result);
    i.putOperand(1, Result2);
    i.putOperand(2, Target);
    i.putOperand(3, Method);
    return i;
  }
  /**
   * Create an instruction of the MIR_Call instruction format
   * with 0 variable arguments.
   * @param o the instruction's operator
   * @param Result the instruction's Result operand
   * @param Result2 the instruction's Result2 operand
   * @param Target the instruction's Target operand
   * @param Method the instruction's Method operand
   * @return the newly created MIR_Call instruction
   */
  public static OPT_Instruction create0(OPT_Operator o
                   , OPT_RegisterOperand Result
                   , OPT_RegisterOperand Result2
                   , OPT_Operand Target
                   , OPT_MethodOperand Method
                )
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(o)) fail(o, "MIR_Call");
    OPT_Instruction i = new OPT_Instruction(o, 5);
    i.putOperand(0, Result);
    i.putOperand(1, Result2);
    i.putOperand(2, Target);
    i.putOperand(3, Method);
    return i;
  }
  /**
   * Create an instruction of the MIR_Call instruction format
   * with 1 variable arguments.
   * @param o the instruction's operator
   * @param Result the instruction's Result operand
   * @param Result2 the instruction's Result2 operand
   * @param Target the instruction's Target operand
   * @param Method the instruction's Method operand
  * @param Param_1 the k'th variable argument called Param
   * @return the newly created MIR_Call instruction
   */
  public static OPT_Instruction create1(OPT_Operator o
                   , OPT_RegisterOperand Result
                   , OPT_RegisterOperand Result2
                   , OPT_Operand Target
                   , OPT_MethodOperand Method
                   , OPT_Operand Param_1
                )
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(o)) fail(o, "MIR_Call");
    OPT_Instruction i = new OPT_Instruction(o, 5);
    i.putOperand(0, Result);
    i.putOperand(1, Result2);
    i.putOperand(2, Target);
    i.putOperand(3, Method);
    i.putOperand(4, Param_1);
    return i;
  }

  /**
   * Mutate the argument instruction into an instruction of the
   * MIR_Call instruction format having the specified
   * operator and operands.
   * @param i the instruction to mutate
   * @param o the instruction's operator
   * @param Result the instruction's Result operand
   * @param Result2 the instruction's Result2 operand
   * @param Target the instruction's Target operand
   * @param Method the instruction's Method operand
   * @param numVarOps the number of variable length operands that
   *                  will be stored in the insruction.
   * @return the mutated instruction
   */
  public static OPT_Instruction mutate(OPT_Instruction i, OPT_Operator o
                   , OPT_RegisterOperand Result
                   , OPT_RegisterOperand Result2
                   , OPT_Operand Target
                   , OPT_MethodOperand Method
                   , int numVarOps
                )
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(o)) fail(o, "MIR_Call");
    if (4+numVarOps>MIN_OPERAND_ARRAY_LENGTH)
      i.resizeNumberOfOperands(4+numVarOps);

    i.operator = o;
    i.putOperand(0, Result);
    i.putOperand(1, Result2);
    i.putOperand(2, Target);
    i.putOperand(3, Method);
    return i;
  }
  /**
   * Mutate the argument instruction into an instruction of the
   * MIR_Call instruction format having the specified
   * operator, operands, and number of variable-length operands.
   * @param Result the instruction's Result operand
   * @param Result2 the instruction's Result2 operand
   * @param Target the instruction's Target operand
   * @param Method the instruction's Method operand
   * @return the mutated instruction
   */
  public static OPT_Instruction mutate0(OPT_Instruction i, OPT_Operator o
                   , OPT_RegisterOperand Result
                   , OPT_RegisterOperand Result2
                   , OPT_Operand Target
                   , OPT_MethodOperand Method
                )
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(o)) fail(o, "MIR_Call");
    i.operator = o;
    i.putOperand(0, Result);
    i.putOperand(1, Result2);
    i.putOperand(2, Target);
    i.putOperand(3, Method);
    return i;
  }
  /**
   * Mutate the argument instruction into an instruction of the
   * MIR_Call instruction format having the specified
   * operator, operands, and number of variable-length operands.
   * @param Result the instruction's Result operand
   * @param Result2 the instruction's Result2 operand
   * @param Target the instruction's Target operand
   * @param Method the instruction's Method operand
   * @param Param_1 the k'th variable argument called Param
   * @return the mutated instruction
   */
  public static OPT_Instruction mutate1(OPT_Instruction i, OPT_Operator o
                   , OPT_RegisterOperand Result
                   , OPT_RegisterOperand Result2
                   , OPT_Operand Target
                   , OPT_MethodOperand Method
                   , OPT_Operand Param_1
                )
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(o)) fail(o, "MIR_Call");
    i.operator = o;
    i.putOperand(0, Result);
    i.putOperand(1, Result2);
    i.putOperand(2, Target);
    i.putOperand(3, Method);
    i.putOperand(4, Param_1);
    return i;
  }
  /**
   * Create an instruction of the MIR_Call instruction format.
   * @param o the instruction's operator
   * @param Result the instruction's Result operand
   * @param Result2 the instruction's Result2 operand
   * @param Target the instruction's Target operand
   * @param numVarOps the number of variable length operands that
   *                 will be stored in the insruction.
   * @return the newly created MIR_Call instruction
   */
  public static OPT_Instruction create(OPT_Operator o
                   , OPT_RegisterOperand Result
                   , OPT_RegisterOperand Result2
                   , OPT_Operand Target
                   , int numVarOps
                )
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(o)) fail(o, "MIR_Call");
    OPT_Instruction i = new OPT_Instruction(o, Math.max(4+numVarOps, MIN_OPERAND_ARRAY_LENGTH));
    i.putOperand(0, Result);
    i.putOperand(1, Result2);
    i.putOperand(2, Target);
    return i;
  }
  /**
   * Create an instruction of the MIR_Call instruction format
   * with 0 variable arguments.
   * @param o the instruction's operator
   * @param Result the instruction's Result operand
   * @param Result2 the instruction's Result2 operand
   * @param Target the instruction's Target operand
   * @return the newly created MIR_Call instruction
   */
  public static OPT_Instruction create0(OPT_Operator o
                   , OPT_RegisterOperand Result
                   , OPT_RegisterOperand Result2
                   , OPT_Operand Target
                )
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(o)) fail(o, "MIR_Call");
    OPT_Instruction i = new OPT_Instruction(o, 5);
    i.putOperand(0, Result);
    i.putOperand(1, Result2);
    i.putOperand(2, Target);
    return i;
  }
  /**
   * Create an instruction of the MIR_Call instruction format
   * with 1 variable arguments.
   * @param o the instruction's operator
   * @param Result the instruction's Result operand
   * @param Result2 the instruction's Result2 operand
   * @param Target the instruction's Target operand
  * @param Param_1 the k'th variable argument called Param
   * @return the newly created MIR_Call instruction
   */
  public static OPT_Instruction create1(OPT_Operator o
                   , OPT_RegisterOperand Result
                   , OPT_RegisterOperand Result2
                   , OPT_Operand Target
                   , OPT_Operand Param_1
                )
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(o)) fail(o, "MIR_Call");
    OPT_Instruction i = new OPT_Instruction(o, 5);
    i.putOperand(0, Result);
    i.putOperand(1, Result2);
    i.putOperand(2, Target);
    i.putOperand(4, Param_1);
    return i;
  }

  /**
   * Mutate the argument instruction into an instruction of the
   * MIR_Call instruction format having the specified
   * operator and operands.
   * @param i the instruction to mutate
   * @param o the instruction's operator
   * @param Result the instruction's Result operand
   * @param Result2 the instruction's Result2 operand
   * @param Target the instruction's Target operand
   * @param numVarOps the number of variable length operands that
   *                  will be stored in the insruction.
   * @return the mutated instruction
   */
  public static OPT_Instruction mutate(OPT_Instruction i, OPT_Operator o
                   , OPT_RegisterOperand Result
                   , OPT_RegisterOperand Result2
                   , OPT_Operand Target
                   , int numVarOps
                )
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(o)) fail(o, "MIR_Call");
    if (4+numVarOps>MIN_OPERAND_ARRAY_LENGTH)
      i.resizeNumberOfOperands(4+numVarOps);

    i.operator = o;
    i.putOperand(0, Result);
    i.putOperand(1, Result2);
    i.putOperand(2, Target);
    i.putOperand(3, null);
    return i;
  }
  /**
   * Mutate the argument instruction into an instruction of the
   * MIR_Call instruction format having the specified
   * operator, operands, and number of variable-length operands.
   * @param Result the instruction's Result operand
   * @param Result2 the instruction's Result2 operand
   * @param Target the instruction's Target operand
   * @return the mutated instruction
   */
  public static OPT_Instruction mutate0(OPT_Instruction i, OPT_Operator o
                   , OPT_RegisterOperand Result
                   , OPT_RegisterOperand Result2
                   , OPT_Operand Target
                )
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(o)) fail(o, "MIR_Call");
    i.operator = o;
    i.putOperand(0, Result);
    i.putOperand(1, Result2);
    i.putOperand(2, Target);
    i.putOperand(3, null);
    return i;
  }
  /**
   * Mutate the argument instruction into an instruction of the
   * MIR_Call instruction format having the specified
   * operator, operands, and number of variable-length operands.
   * @param Result the instruction's Result operand
   * @param Result2 the instruction's Result2 operand
   * @param Target the instruction's Target operand
   * @param Param_1 the k'th variable argument called Param
   * @return the mutated instruction
   */
  public static OPT_Instruction mutate1(OPT_Instruction i, OPT_Operator o
                   , OPT_RegisterOperand Result
                   , OPT_RegisterOperand Result2
                   , OPT_Operand Target
                   , OPT_Operand Param_1
                )
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(o)) fail(o, "MIR_Call");
    i.operator = o;
    i.putOperand(0, Result);
    i.putOperand(1, Result2);
    i.putOperand(2, Target);
    i.putOperand(3, null);
    i.putOperand(4, Param_1);
    return i;
  }
  /**
   * Create an instruction of the MIR_Call instruction format.
   * @param o the instruction's operator
   * @param Result the instruction's Result operand
   * @param Result2 the instruction's Result2 operand
   * @param Method the instruction's Method operand
   * @param numVarOps the number of variable length operands that
   *                 will be stored in the insruction.
   * @return the newly created MIR_Call instruction
   */
  public static OPT_Instruction create(OPT_Operator o
                   , OPT_RegisterOperand Result
                   , OPT_RegisterOperand Result2
                   , OPT_MethodOperand Method
                   , int numVarOps
                )
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(o)) fail(o, "MIR_Call");
    OPT_Instruction i = new OPT_Instruction(o, Math.max(4+numVarOps, MIN_OPERAND_ARRAY_LENGTH));
    i.putOperand(0, Result);
    i.putOperand(1, Result2);
    i.putOperand(3, Method);
    return i;
  }
  /**
   * Create an instruction of the MIR_Call instruction format
   * with 0 variable arguments.
   * @param o the instruction's operator
   * @param Result the instruction's Result operand
   * @param Result2 the instruction's Result2 operand
   * @param Method the instruction's Method operand
   * @return the newly created MIR_Call instruction
   */
  public static OPT_Instruction create0(OPT_Operator o
                   , OPT_RegisterOperand Result
                   , OPT_RegisterOperand Result2
                   , OPT_MethodOperand Method
                )
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(o)) fail(o, "MIR_Call");
    OPT_Instruction i = new OPT_Instruction(o, 5);
    i.putOperand(0, Result);
    i.putOperand(1, Result2);
    i.putOperand(3, Method);
    return i;
  }
  /**
   * Create an instruction of the MIR_Call instruction format
   * with 1 variable arguments.
   * @param o the instruction's operator
   * @param Result the instruction's Result operand
   * @param Result2 the instruction's Result2 operand
   * @param Method the instruction's Method operand
  * @param Param_1 the k'th variable argument called Param
   * @return the newly created MIR_Call instruction
   */
  public static OPT_Instruction create1(OPT_Operator o
                   , OPT_RegisterOperand Result
                   , OPT_RegisterOperand Result2
                   , OPT_MethodOperand Method
                   , OPT_Operand Param_1
                )
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(o)) fail(o, "MIR_Call");
    OPT_Instruction i = new OPT_Instruction(o, 5);
    i.putOperand(0, Result);
    i.putOperand(1, Result2);
    i.putOperand(3, Method);
    i.putOperand(4, Param_1);
    return i;
  }

  /**
   * Mutate the argument instruction into an instruction of the
   * MIR_Call instruction format having the specified
   * operator and operands.
   * @param i the instruction to mutate
   * @param o the instruction's operator
   * @param Result the instruction's Result operand
   * @param Result2 the instruction's Result2 operand
   * @param Method the instruction's Method operand
   * @param numVarOps the number of variable length operands that
   *                  will be stored in the insruction.
   * @return the mutated instruction
   */
  public static OPT_Instruction mutate(OPT_Instruction i, OPT_Operator o
                   , OPT_RegisterOperand Result
                   , OPT_RegisterOperand Result2
                   , OPT_MethodOperand Method
                   , int numVarOps
                )
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(o)) fail(o, "MIR_Call");
    if (4+numVarOps>MIN_OPERAND_ARRAY_LENGTH)
      i.resizeNumberOfOperands(4+numVarOps);

    i.operator = o;
    i.putOperand(0, Result);
    i.putOperand(1, Result2);
    i.putOperand(2, null);
    i.putOperand(3, Method);
    return i;
  }
  /**
   * Mutate the argument instruction into an instruction of the
   * MIR_Call instruction format having the specified
   * operator, operands, and number of variable-length operands.
   * @param Result the instruction's Result operand
   * @param Result2 the instruction's Result2 operand
   * @param Method the instruction's Method operand
   * @return the mutated instruction
   */
  public static OPT_Instruction mutate0(OPT_Instruction i, OPT_Operator o
                   , OPT_RegisterOperand Result
                   , OPT_RegisterOperand Result2
                   , OPT_MethodOperand Method
                )
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(o)) fail(o, "MIR_Call");
    i.operator = o;
    i.putOperand(0, Result);
    i.putOperand(1, Result2);
    i.putOperand(2, null);
    i.putOperand(3, Method);
    return i;
  }
  /**
   * Mutate the argument instruction into an instruction of the
   * MIR_Call instruction format having the specified
   * operator, operands, and number of variable-length operands.
   * @param Result the instruction's Result operand
   * @param Result2 the instruction's Result2 operand
   * @param Method the instruction's Method operand
   * @param Param_1 the k'th variable argument called Param
   * @return the mutated instruction
   */
  public static OPT_Instruction mutate1(OPT_Instruction i, OPT_Operator o
                   , OPT_RegisterOperand Result
                   , OPT_RegisterOperand Result2
                   , OPT_MethodOperand Method
                   , OPT_Operand Param_1
                )
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(o)) fail(o, "MIR_Call");
    i.operator = o;
    i.putOperand(0, Result);
    i.putOperand(1, Result2);
    i.putOperand(2, null);
    i.putOperand(3, Method);
    i.putOperand(4, Param_1);
    return i;
  }
  /**
   * Create an instruction of the MIR_Call instruction format.
   * @param o the instruction's operator
   * @param Result the instruction's Result operand
   * @param Result2 the instruction's Result2 operand
   * @param numVarOps the number of variable length operands that
   *                 will be stored in the insruction.
   * @return the newly created MIR_Call instruction
   */
  public static OPT_Instruction create(OPT_Operator o
                   , OPT_RegisterOperand Result
                   , OPT_RegisterOperand Result2
                   , int numVarOps
                )
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(o)) fail(o, "MIR_Call");
    OPT_Instruction i = new OPT_Instruction(o, Math.max(4+numVarOps, MIN_OPERAND_ARRAY_LENGTH));
    i.putOperand(0, Result);
    i.putOperand(1, Result2);
    return i;
  }
  /**
   * Create an instruction of the MIR_Call instruction format
   * with 0 variable arguments.
   * @param o the instruction's operator
   * @param Result the instruction's Result operand
   * @param Result2 the instruction's Result2 operand
   * @return the newly created MIR_Call instruction
   */
  public static OPT_Instruction create0(OPT_Operator o
                   , OPT_RegisterOperand Result
                   , OPT_RegisterOperand Result2
                )
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(o)) fail(o, "MIR_Call");
    OPT_Instruction i = new OPT_Instruction(o, 5);
    i.putOperand(0, Result);
    i.putOperand(1, Result2);
    return i;
  }
  /**
   * Create an instruction of the MIR_Call instruction format
   * with 1 variable arguments.
   * @param o the instruction's operator
   * @param Result the instruction's Result operand
   * @param Result2 the instruction's Result2 operand
  * @param Param_1 the k'th variable argument called Param
   * @return the newly created MIR_Call instruction
   */
  public static OPT_Instruction create1(OPT_Operator o
                   , OPT_RegisterOperand Result
                   , OPT_RegisterOperand Result2
                   , OPT_Operand Param_1
                )
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(o)) fail(o, "MIR_Call");
    OPT_Instruction i = new OPT_Instruction(o, 5);
    i.putOperand(0, Result);
    i.putOperand(1, Result2);
    i.putOperand(4, Param_1);
    return i;
  }

  /**
   * Mutate the argument instruction into an instruction of the
   * MIR_Call instruction format having the specified
   * operator and operands.
   * @param i the instruction to mutate
   * @param o the instruction's operator
   * @param Result the instruction's Result operand
   * @param Result2 the instruction's Result2 operand
   * @param numVarOps the number of variable length operands that
   *                  will be stored in the insruction.
   * @return the mutated instruction
   */
  public static OPT_Instruction mutate(OPT_Instruction i, OPT_Operator o
                   , OPT_RegisterOperand Result
                   , OPT_RegisterOperand Result2
                   , int numVarOps
                )
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(o)) fail(o, "MIR_Call");
    if (4+numVarOps>MIN_OPERAND_ARRAY_LENGTH)
      i.resizeNumberOfOperands(4+numVarOps);

    i.operator = o;
    i.putOperand(0, Result);
    i.putOperand(1, Result2);
    i.putOperand(2, null);
    i.putOperand(3, null);
    return i;
  }
  /**
   * Mutate the argument instruction into an instruction of the
   * MIR_Call instruction format having the specified
   * operator, operands, and number of variable-length operands.
   * @param Result the instruction's Result operand
   * @param Result2 the instruction's Result2 operand
   * @return the mutated instruction
   */
  public static OPT_Instruction mutate0(OPT_Instruction i, OPT_Operator o
                   , OPT_RegisterOperand Result
                   , OPT_RegisterOperand Result2
                )
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(o)) fail(o, "MIR_Call");
    i.operator = o;
    i.putOperand(0, Result);
    i.putOperand(1, Result2);
    i.putOperand(2, null);
    i.putOperand(3, null);
    return i;
  }
  /**
   * Mutate the argument instruction into an instruction of the
   * MIR_Call instruction format having the specified
   * operator, operands, and number of variable-length operands.
   * @param Result the instruction's Result operand
   * @param Result2 the instruction's Result2 operand
   * @param Param_1 the k'th variable argument called Param
   * @return the mutated instruction
   */
  public static OPT_Instruction mutate1(OPT_Instruction i, OPT_Operator o
                   , OPT_RegisterOperand Result
                   , OPT_RegisterOperand Result2
                   , OPT_Operand Param_1
                )
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(o)) fail(o, "MIR_Call");
    i.operator = o;
    i.putOperand(0, Result);
    i.putOperand(1, Result2);
    i.putOperand(2, null);
    i.putOperand(3, null);
    i.putOperand(4, Param_1);
    return i;
  }
}

