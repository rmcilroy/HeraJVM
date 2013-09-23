
/*
 * THIS FILE IS MACHINE_GENERATED. DO NOT EDIT.
 * See InstructionFormats.template, InstructionFormatList.dat,
 * OperatorList.dat, etc.
 */

package org.jikesrvm.compilers.opt.ir;

import org.jikesrvm.VM_Configuration;
import org.jikesrvm.compilers.opt.ir.ia32.OPT_IA32ConditionOperand; //NOPMD


/**
 * The MIR_CondBranch2 InstructionFormat class.
 *
 * The header comment for {@link OPT_Instruction} contains
 * an explanation of the role of InstructionFormats in the
 * opt compiler's IR.
 */
@SuppressWarnings("unused")  // Machine generated code is never 100% clean
public final class MIR_CondBranch2 extends OPT_InstructionFormat {
  /**
   * InstructionFormat identification method for MIR_CondBranch2.
   * @param i an instruction
   * @return <code>true</code> if the InstructionFormat of the argument
   *         instruction is MIR_CondBranch2 or <code>false</code>
   *         if it is not.
   */
  public static boolean conforms(OPT_Instruction i) {
    return conforms(i.operator);
  }
  /**
   * InstructionFormat identification method for MIR_CondBranch2.
   * @param o an instruction
   * @return <code>true</code> if the InstructionFormat of the argument
   *         operator is MIR_CondBranch2 or <code>false</code>
   *         if it is not.
   */
  public static boolean conforms(OPT_Operator o) {
    return o.format == MIR_CondBranch2_format;
  }

  /**
   * Get the operand called Cond1 from the
   * argument instruction. Note that the returned operand
   * will still point to its containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Cond1
   */
  public static OPT_IA32ConditionOperand getCond1(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CondBranch2");
    return (OPT_IA32ConditionOperand) i.getOperand(0);
  }
  /**
   * Get the operand called Cond1 from the argument
   * instruction clearing its instruction pointer. The returned
   * operand will not point to any containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Cond1
   */
  public static OPT_IA32ConditionOperand getClearCond1(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CondBranch2");
    return (OPT_IA32ConditionOperand) i.getClearOperand(0);
  }
  /**
   * Set the operand called Cond1 in the argument
   * instruction to the argument operand. The operand will
   * now point to the argument instruction as its containing
   * instruction.
   * @param i the instruction in which to store the operand
   * @param Cond1 the operand to store
   */
  public static void setCond1(OPT_Instruction i, OPT_IA32ConditionOperand Cond1) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CondBranch2");
    i.putOperand(0, Cond1);
  }
  /**
   * Return the index of the operand called Cond1
   * in the argument instruction.
   * @param i the instruction to access.
   * @return the index of the operand called Cond1
   *         in the argument instruction
   */
  public static int indexOfCond1(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CondBranch2");
    return 0;
  }
  /**
   * Does the argument instruction have a non-null
   * operand named Cond1?
   * @param i the instruction to access.
   * @return <code>true</code> if the instruction has an non-null
   *         operand named Cond1 or <code>false</code>
   *         if it does not.
   */
  public static boolean hasCond1(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CondBranch2");
    return i.getOperand(0) != null;
  }

  /**
   * Get the operand called Target1 from the
   * argument instruction. Note that the returned operand
   * will still point to its containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Target1
   */
  public static OPT_BranchOperand getTarget1(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CondBranch2");
    return (OPT_BranchOperand) i.getOperand(1);
  }
  /**
   * Get the operand called Target1 from the argument
   * instruction clearing its instruction pointer. The returned
   * operand will not point to any containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Target1
   */
  public static OPT_BranchOperand getClearTarget1(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CondBranch2");
    return (OPT_BranchOperand) i.getClearOperand(1);
  }
  /**
   * Set the operand called Target1 in the argument
   * instruction to the argument operand. The operand will
   * now point to the argument instruction as its containing
   * instruction.
   * @param i the instruction in which to store the operand
   * @param Target1 the operand to store
   */
  public static void setTarget1(OPT_Instruction i, OPT_BranchOperand Target1) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CondBranch2");
    i.putOperand(1, Target1);
  }
  /**
   * Return the index of the operand called Target1
   * in the argument instruction.
   * @param i the instruction to access.
   * @return the index of the operand called Target1
   *         in the argument instruction
   */
  public static int indexOfTarget1(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CondBranch2");
    return 1;
  }
  /**
   * Does the argument instruction have a non-null
   * operand named Target1?
   * @param i the instruction to access.
   * @return <code>true</code> if the instruction has an non-null
   *         operand named Target1 or <code>false</code>
   *         if it does not.
   */
  public static boolean hasTarget1(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CondBranch2");
    return i.getOperand(1) != null;
  }

  /**
   * Get the operand called BranchProfile1 from the
   * argument instruction. Note that the returned operand
   * will still point to its containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called BranchProfile1
   */
  public static OPT_BranchProfileOperand getBranchProfile1(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CondBranch2");
    return (OPT_BranchProfileOperand) i.getOperand(2);
  }
  /**
   * Get the operand called BranchProfile1 from the argument
   * instruction clearing its instruction pointer. The returned
   * operand will not point to any containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called BranchProfile1
   */
  public static OPT_BranchProfileOperand getClearBranchProfile1(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CondBranch2");
    return (OPT_BranchProfileOperand) i.getClearOperand(2);
  }
  /**
   * Set the operand called BranchProfile1 in the argument
   * instruction to the argument operand. The operand will
   * now point to the argument instruction as its containing
   * instruction.
   * @param i the instruction in which to store the operand
   * @param BranchProfile1 the operand to store
   */
  public static void setBranchProfile1(OPT_Instruction i, OPT_BranchProfileOperand BranchProfile1) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CondBranch2");
    i.putOperand(2, BranchProfile1);
  }
  /**
   * Return the index of the operand called BranchProfile1
   * in the argument instruction.
   * @param i the instruction to access.
   * @return the index of the operand called BranchProfile1
   *         in the argument instruction
   */
  public static int indexOfBranchProfile1(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CondBranch2");
    return 2;
  }
  /**
   * Does the argument instruction have a non-null
   * operand named BranchProfile1?
   * @param i the instruction to access.
   * @return <code>true</code> if the instruction has an non-null
   *         operand named BranchProfile1 or <code>false</code>
   *         if it does not.
   */
  public static boolean hasBranchProfile1(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CondBranch2");
    return i.getOperand(2) != null;
  }

  /**
   * Get the operand called Cond2 from the
   * argument instruction. Note that the returned operand
   * will still point to its containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Cond2
   */
  public static OPT_IA32ConditionOperand getCond2(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CondBranch2");
    return (OPT_IA32ConditionOperand) i.getOperand(3);
  }
  /**
   * Get the operand called Cond2 from the argument
   * instruction clearing its instruction pointer. The returned
   * operand will not point to any containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Cond2
   */
  public static OPT_IA32ConditionOperand getClearCond2(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CondBranch2");
    return (OPT_IA32ConditionOperand) i.getClearOperand(3);
  }
  /**
   * Set the operand called Cond2 in the argument
   * instruction to the argument operand. The operand will
   * now point to the argument instruction as its containing
   * instruction.
   * @param i the instruction in which to store the operand
   * @param Cond2 the operand to store
   */
  public static void setCond2(OPT_Instruction i, OPT_IA32ConditionOperand Cond2) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CondBranch2");
    i.putOperand(3, Cond2);
  }
  /**
   * Return the index of the operand called Cond2
   * in the argument instruction.
   * @param i the instruction to access.
   * @return the index of the operand called Cond2
   *         in the argument instruction
   */
  public static int indexOfCond2(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CondBranch2");
    return 3;
  }
  /**
   * Does the argument instruction have a non-null
   * operand named Cond2?
   * @param i the instruction to access.
   * @return <code>true</code> if the instruction has an non-null
   *         operand named Cond2 or <code>false</code>
   *         if it does not.
   */
  public static boolean hasCond2(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CondBranch2");
    return i.getOperand(3) != null;
  }

  /**
   * Get the operand called Target2 from the
   * argument instruction. Note that the returned operand
   * will still point to its containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Target2
   */
  public static OPT_BranchOperand getTarget2(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CondBranch2");
    return (OPT_BranchOperand) i.getOperand(4);
  }
  /**
   * Get the operand called Target2 from the argument
   * instruction clearing its instruction pointer. The returned
   * operand will not point to any containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Target2
   */
  public static OPT_BranchOperand getClearTarget2(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CondBranch2");
    return (OPT_BranchOperand) i.getClearOperand(4);
  }
  /**
   * Set the operand called Target2 in the argument
   * instruction to the argument operand. The operand will
   * now point to the argument instruction as its containing
   * instruction.
   * @param i the instruction in which to store the operand
   * @param Target2 the operand to store
   */
  public static void setTarget2(OPT_Instruction i, OPT_BranchOperand Target2) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CondBranch2");
    i.putOperand(4, Target2);
  }
  /**
   * Return the index of the operand called Target2
   * in the argument instruction.
   * @param i the instruction to access.
   * @return the index of the operand called Target2
   *         in the argument instruction
   */
  public static int indexOfTarget2(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CondBranch2");
    return 4;
  }
  /**
   * Does the argument instruction have a non-null
   * operand named Target2?
   * @param i the instruction to access.
   * @return <code>true</code> if the instruction has an non-null
   *         operand named Target2 or <code>false</code>
   *         if it does not.
   */
  public static boolean hasTarget2(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CondBranch2");
    return i.getOperand(4) != null;
  }

  /**
   * Get the operand called BranchProfile2 from the
   * argument instruction. Note that the returned operand
   * will still point to its containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called BranchProfile2
   */
  public static OPT_BranchProfileOperand getBranchProfile2(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CondBranch2");
    return (OPT_BranchProfileOperand) i.getOperand(5);
  }
  /**
   * Get the operand called BranchProfile2 from the argument
   * instruction clearing its instruction pointer. The returned
   * operand will not point to any containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called BranchProfile2
   */
  public static OPT_BranchProfileOperand getClearBranchProfile2(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CondBranch2");
    return (OPT_BranchProfileOperand) i.getClearOperand(5);
  }
  /**
   * Set the operand called BranchProfile2 in the argument
   * instruction to the argument operand. The operand will
   * now point to the argument instruction as its containing
   * instruction.
   * @param i the instruction in which to store the operand
   * @param BranchProfile2 the operand to store
   */
  public static void setBranchProfile2(OPT_Instruction i, OPT_BranchProfileOperand BranchProfile2) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CondBranch2");
    i.putOperand(5, BranchProfile2);
  }
  /**
   * Return the index of the operand called BranchProfile2
   * in the argument instruction.
   * @param i the instruction to access.
   * @return the index of the operand called BranchProfile2
   *         in the argument instruction
   */
  public static int indexOfBranchProfile2(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CondBranch2");
    return 5;
  }
  /**
   * Does the argument instruction have a non-null
   * operand named BranchProfile2?
   * @param i the instruction to access.
   * @return <code>true</code> if the instruction has an non-null
   *         operand named BranchProfile2 or <code>false</code>
   *         if it does not.
   */
  public static boolean hasBranchProfile2(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CondBranch2");
    return i.getOperand(5) != null;
  }


  /**
   * Create an instruction of the MIR_CondBranch2 instruction format.
   * @param o the instruction's operator
   * @param Cond1 the instruction's Cond1 operand
   * @param Target1 the instruction's Target1 operand
   * @param BranchProfile1 the instruction's BranchProfile1 operand
   * @param Cond2 the instruction's Cond2 operand
   * @param Target2 the instruction's Target2 operand
   * @param BranchProfile2 the instruction's BranchProfile2 operand
   * @return the newly created MIR_CondBranch2 instruction
   */
  public static OPT_Instruction create(OPT_Operator o
                   , OPT_IA32ConditionOperand Cond1
                   , OPT_BranchOperand Target1
                   , OPT_BranchProfileOperand BranchProfile1
                   , OPT_IA32ConditionOperand Cond2
                   , OPT_BranchOperand Target2
                   , OPT_BranchProfileOperand BranchProfile2
                )
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(o)) fail(o, "MIR_CondBranch2");
    OPT_Instruction i = new OPT_Instruction(o, 6);
    i.putOperand(0, Cond1);
    i.putOperand(1, Target1);
    i.putOperand(2, BranchProfile1);
    i.putOperand(3, Cond2);
    i.putOperand(4, Target2);
    i.putOperand(5, BranchProfile2);
    return i;
  }

  /**
   * Mutate the argument instruction into an instruction of the
   * MIR_CondBranch2 instruction format having the specified
   * operator and operands.
   * @param i the instruction to mutate
   * @param o the instruction's operator
   * @param Cond1 the instruction's Cond1 operand
   * @param Target1 the instruction's Target1 operand
   * @param BranchProfile1 the instruction's BranchProfile1 operand
   * @param Cond2 the instruction's Cond2 operand
   * @param Target2 the instruction's Target2 operand
   * @param BranchProfile2 the instruction's BranchProfile2 operand
   * @return the mutated instruction
   */
  public static OPT_Instruction mutate(OPT_Instruction i, OPT_Operator o
                   , OPT_IA32ConditionOperand Cond1
                   , OPT_BranchOperand Target1
                   , OPT_BranchProfileOperand BranchProfile1
                   , OPT_IA32ConditionOperand Cond2
                   , OPT_BranchOperand Target2
                   , OPT_BranchProfileOperand BranchProfile2
                )
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(o)) fail(o, "MIR_CondBranch2");
    i.resizeNumberOfOperands(6);

    i.operator = o;
    i.putOperand(0, Cond1);
    i.putOperand(1, Target1);
    i.putOperand(2, BranchProfile1);
    i.putOperand(3, Cond2);
    i.putOperand(4, Target2);
    i.putOperand(5, BranchProfile2);
    return i;
  }
  /**
   * Create an instruction of the MIR_CondBranch2 instruction format.
   * @param o the instruction's operator
   * @param Cond1 the instruction's Cond1 operand
   * @param Target1 the instruction's Target1 operand
   * @param BranchProfile1 the instruction's BranchProfile1 operand
   * @param Cond2 the instruction's Cond2 operand
   * @param BranchProfile2 the instruction's BranchProfile2 operand
   * @return the newly created MIR_CondBranch2 instruction
   */
  public static OPT_Instruction create(OPT_Operator o
                   , OPT_IA32ConditionOperand Cond1
                   , OPT_BranchOperand Target1
                   , OPT_BranchProfileOperand BranchProfile1
                   , OPT_IA32ConditionOperand Cond2
                   , OPT_BranchProfileOperand BranchProfile2
                )
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(o)) fail(o, "MIR_CondBranch2");
    OPT_Instruction i = new OPT_Instruction(o, 6);
    i.putOperand(0, Cond1);
    i.putOperand(1, Target1);
    i.putOperand(2, BranchProfile1);
    i.putOperand(3, Cond2);
    i.putOperand(5, BranchProfile2);
    return i;
  }

  /**
   * Mutate the argument instruction into an instruction of the
   * MIR_CondBranch2 instruction format having the specified
   * operator and operands.
   * @param i the instruction to mutate
   * @param o the instruction's operator
   * @param Cond1 the instruction's Cond1 operand
   * @param Target1 the instruction's Target1 operand
   * @param BranchProfile1 the instruction's BranchProfile1 operand
   * @param Cond2 the instruction's Cond2 operand
   * @param BranchProfile2 the instruction's BranchProfile2 operand
   * @return the mutated instruction
   */
  public static OPT_Instruction mutate(OPT_Instruction i, OPT_Operator o
                   , OPT_IA32ConditionOperand Cond1
                   , OPT_BranchOperand Target1
                   , OPT_BranchProfileOperand BranchProfile1
                   , OPT_IA32ConditionOperand Cond2
                   , OPT_BranchProfileOperand BranchProfile2
                )
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(o)) fail(o, "MIR_CondBranch2");
    i.resizeNumberOfOperands(6);

    i.operator = o;
    i.putOperand(0, Cond1);
    i.putOperand(1, Target1);
    i.putOperand(2, BranchProfile1);
    i.putOperand(3, Cond2);
    i.putOperand(4, null);
    i.putOperand(5, BranchProfile2);
    return i;
  }
  /**
   * Create an instruction of the MIR_CondBranch2 instruction format.
   * @param o the instruction's operator
   * @param Cond1 the instruction's Cond1 operand
   * @param BranchProfile1 the instruction's BranchProfile1 operand
   * @param Cond2 the instruction's Cond2 operand
   * @param Target2 the instruction's Target2 operand
   * @param BranchProfile2 the instruction's BranchProfile2 operand
   * @return the newly created MIR_CondBranch2 instruction
   */
  public static OPT_Instruction create(OPT_Operator o
                   , OPT_IA32ConditionOperand Cond1
                   , OPT_BranchProfileOperand BranchProfile1
                   , OPT_IA32ConditionOperand Cond2
                   , OPT_BranchOperand Target2
                   , OPT_BranchProfileOperand BranchProfile2
                )
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(o)) fail(o, "MIR_CondBranch2");
    OPT_Instruction i = new OPT_Instruction(o, 6);
    i.putOperand(0, Cond1);
    i.putOperand(2, BranchProfile1);
    i.putOperand(3, Cond2);
    i.putOperand(4, Target2);
    i.putOperand(5, BranchProfile2);
    return i;
  }

  /**
   * Mutate the argument instruction into an instruction of the
   * MIR_CondBranch2 instruction format having the specified
   * operator and operands.
   * @param i the instruction to mutate
   * @param o the instruction's operator
   * @param Cond1 the instruction's Cond1 operand
   * @param BranchProfile1 the instruction's BranchProfile1 operand
   * @param Cond2 the instruction's Cond2 operand
   * @param Target2 the instruction's Target2 operand
   * @param BranchProfile2 the instruction's BranchProfile2 operand
   * @return the mutated instruction
   */
  public static OPT_Instruction mutate(OPT_Instruction i, OPT_Operator o
                   , OPT_IA32ConditionOperand Cond1
                   , OPT_BranchProfileOperand BranchProfile1
                   , OPT_IA32ConditionOperand Cond2
                   , OPT_BranchOperand Target2
                   , OPT_BranchProfileOperand BranchProfile2
                )
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(o)) fail(o, "MIR_CondBranch2");
    i.resizeNumberOfOperands(6);

    i.operator = o;
    i.putOperand(0, Cond1);
    i.putOperand(1, null);
    i.putOperand(2, BranchProfile1);
    i.putOperand(3, Cond2);
    i.putOperand(4, Target2);
    i.putOperand(5, BranchProfile2);
    return i;
  }
  /**
   * Create an instruction of the MIR_CondBranch2 instruction format.
   * @param o the instruction's operator
   * @param Cond1 the instruction's Cond1 operand
   * @param BranchProfile1 the instruction's BranchProfile1 operand
   * @param Cond2 the instruction's Cond2 operand
   * @param BranchProfile2 the instruction's BranchProfile2 operand
   * @return the newly created MIR_CondBranch2 instruction
   */
  public static OPT_Instruction create(OPT_Operator o
                   , OPT_IA32ConditionOperand Cond1
                   , OPT_BranchProfileOperand BranchProfile1
                   , OPT_IA32ConditionOperand Cond2
                   , OPT_BranchProfileOperand BranchProfile2
                )
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(o)) fail(o, "MIR_CondBranch2");
    OPT_Instruction i = new OPT_Instruction(o, 6);
    i.putOperand(0, Cond1);
    i.putOperand(2, BranchProfile1);
    i.putOperand(3, Cond2);
    i.putOperand(5, BranchProfile2);
    return i;
  }

  /**
   * Mutate the argument instruction into an instruction of the
   * MIR_CondBranch2 instruction format having the specified
   * operator and operands.
   * @param i the instruction to mutate
   * @param o the instruction's operator
   * @param Cond1 the instruction's Cond1 operand
   * @param BranchProfile1 the instruction's BranchProfile1 operand
   * @param Cond2 the instruction's Cond2 operand
   * @param BranchProfile2 the instruction's BranchProfile2 operand
   * @return the mutated instruction
   */
  public static OPT_Instruction mutate(OPT_Instruction i, OPT_Operator o
                   , OPT_IA32ConditionOperand Cond1
                   , OPT_BranchProfileOperand BranchProfile1
                   , OPT_IA32ConditionOperand Cond2
                   , OPT_BranchProfileOperand BranchProfile2
                )
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(o)) fail(o, "MIR_CondBranch2");
    i.resizeNumberOfOperands(6);

    i.operator = o;
    i.putOperand(0, Cond1);
    i.putOperand(1, null);
    i.putOperand(2, BranchProfile1);
    i.putOperand(3, Cond2);
    i.putOperand(4, null);
    i.putOperand(5, BranchProfile2);
    return i;
  }
}

