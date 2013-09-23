
/*
 * THIS FILE IS MACHINE_GENERATED. DO NOT EDIT.
 * See InstructionFormats.template, InstructionFormatList.dat,
 * OperatorList.dat, etc.
 */

package org.jikesrvm.compilers.opt.ir;

import org.jikesrvm.VM_Configuration;
import org.jikesrvm.compilers.opt.ir.ia32.OPT_IA32ConditionOperand; //NOPMD


/**
 * The MIR_CondBranch InstructionFormat class.
 *
 * The header comment for {@link OPT_Instruction} contains
 * an explanation of the role of InstructionFormats in the
 * opt compiler's IR.
 */
@SuppressWarnings("unused")  // Machine generated code is never 100% clean
public final class MIR_CondBranch extends OPT_InstructionFormat {
  /**
   * InstructionFormat identification method for MIR_CondBranch.
   * @param i an instruction
   * @return <code>true</code> if the InstructionFormat of the argument
   *         instruction is MIR_CondBranch or <code>false</code>
   *         if it is not.
   */
  public static boolean conforms(OPT_Instruction i) {
    return conforms(i.operator);
  }
  /**
   * InstructionFormat identification method for MIR_CondBranch.
   * @param o an instruction
   * @return <code>true</code> if the InstructionFormat of the argument
   *         operator is MIR_CondBranch or <code>false</code>
   *         if it is not.
   */
  public static boolean conforms(OPT_Operator o) {
    return o.format == MIR_CondBranch_format;
  }

  /**
   * Get the operand called Cond from the
   * argument instruction. Note that the returned operand
   * will still point to its containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Cond
   */
  public static OPT_IA32ConditionOperand getCond(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CondBranch");
    return (OPT_IA32ConditionOperand) i.getOperand(0);
  }
  /**
   * Get the operand called Cond from the argument
   * instruction clearing its instruction pointer. The returned
   * operand will not point to any containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Cond
   */
  public static OPT_IA32ConditionOperand getClearCond(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CondBranch");
    return (OPT_IA32ConditionOperand) i.getClearOperand(0);
  }
  /**
   * Set the operand called Cond in the argument
   * instruction to the argument operand. The operand will
   * now point to the argument instruction as its containing
   * instruction.
   * @param i the instruction in which to store the operand
   * @param Cond the operand to store
   */
  public static void setCond(OPT_Instruction i, OPT_IA32ConditionOperand Cond) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CondBranch");
    i.putOperand(0, Cond);
  }
  /**
   * Return the index of the operand called Cond
   * in the argument instruction.
   * @param i the instruction to access.
   * @return the index of the operand called Cond
   *         in the argument instruction
   */
  public static int indexOfCond(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CondBranch");
    return 0;
  }
  /**
   * Does the argument instruction have a non-null
   * operand named Cond?
   * @param i the instruction to access.
   * @return <code>true</code> if the instruction has an non-null
   *         operand named Cond or <code>false</code>
   *         if it does not.
   */
  public static boolean hasCond(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CondBranch");
    return i.getOperand(0) != null;
  }

  /**
   * Get the operand called Target from the
   * argument instruction. Note that the returned operand
   * will still point to its containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Target
   */
  public static OPT_BranchOperand getTarget(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CondBranch");
    return (OPT_BranchOperand) i.getOperand(1);
  }
  /**
   * Get the operand called Target from the argument
   * instruction clearing its instruction pointer. The returned
   * operand will not point to any containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Target
   */
  public static OPT_BranchOperand getClearTarget(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CondBranch");
    return (OPT_BranchOperand) i.getClearOperand(1);
  }
  /**
   * Set the operand called Target in the argument
   * instruction to the argument operand. The operand will
   * now point to the argument instruction as its containing
   * instruction.
   * @param i the instruction in which to store the operand
   * @param Target the operand to store
   */
  public static void setTarget(OPT_Instruction i, OPT_BranchOperand Target) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CondBranch");
    i.putOperand(1, Target);
  }
  /**
   * Return the index of the operand called Target
   * in the argument instruction.
   * @param i the instruction to access.
   * @return the index of the operand called Target
   *         in the argument instruction
   */
  public static int indexOfTarget(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CondBranch");
    return 1;
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
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CondBranch");
    return i.getOperand(1) != null;
  }

  /**
   * Get the operand called BranchProfile from the
   * argument instruction. Note that the returned operand
   * will still point to its containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called BranchProfile
   */
  public static OPT_BranchProfileOperand getBranchProfile(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CondBranch");
    return (OPT_BranchProfileOperand) i.getOperand(2);
  }
  /**
   * Get the operand called BranchProfile from the argument
   * instruction clearing its instruction pointer. The returned
   * operand will not point to any containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called BranchProfile
   */
  public static OPT_BranchProfileOperand getClearBranchProfile(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CondBranch");
    return (OPT_BranchProfileOperand) i.getClearOperand(2);
  }
  /**
   * Set the operand called BranchProfile in the argument
   * instruction to the argument operand. The operand will
   * now point to the argument instruction as its containing
   * instruction.
   * @param i the instruction in which to store the operand
   * @param BranchProfile the operand to store
   */
  public static void setBranchProfile(OPT_Instruction i, OPT_BranchProfileOperand BranchProfile) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CondBranch");
    i.putOperand(2, BranchProfile);
  }
  /**
   * Return the index of the operand called BranchProfile
   * in the argument instruction.
   * @param i the instruction to access.
   * @return the index of the operand called BranchProfile
   *         in the argument instruction
   */
  public static int indexOfBranchProfile(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CondBranch");
    return 2;
  }
  /**
   * Does the argument instruction have a non-null
   * operand named BranchProfile?
   * @param i the instruction to access.
   * @return <code>true</code> if the instruction has an non-null
   *         operand named BranchProfile or <code>false</code>
   *         if it does not.
   */
  public static boolean hasBranchProfile(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_CondBranch");
    return i.getOperand(2) != null;
  }


  /**
   * Create an instruction of the MIR_CondBranch instruction format.
   * @param o the instruction's operator
   * @param Cond the instruction's Cond operand
   * @param Target the instruction's Target operand
   * @param BranchProfile the instruction's BranchProfile operand
   * @return the newly created MIR_CondBranch instruction
   */
  public static OPT_Instruction create(OPT_Operator o
                   , OPT_IA32ConditionOperand Cond
                   , OPT_BranchOperand Target
                   , OPT_BranchProfileOperand BranchProfile
                )
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(o)) fail(o, "MIR_CondBranch");
    OPT_Instruction i = new OPT_Instruction(o, 5);
    i.putOperand(0, Cond);
    i.putOperand(1, Target);
    i.putOperand(2, BranchProfile);
    return i;
  }

  /**
   * Mutate the argument instruction into an instruction of the
   * MIR_CondBranch instruction format having the specified
   * operator and operands.
   * @param i the instruction to mutate
   * @param o the instruction's operator
   * @param Cond the instruction's Cond operand
   * @param Target the instruction's Target operand
   * @param BranchProfile the instruction's BranchProfile operand
   * @return the mutated instruction
   */
  public static OPT_Instruction mutate(OPT_Instruction i, OPT_Operator o
                   , OPT_IA32ConditionOperand Cond
                   , OPT_BranchOperand Target
                   , OPT_BranchProfileOperand BranchProfile
                )
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(o)) fail(o, "MIR_CondBranch");
    i.operator = o;
    i.putOperand(0, Cond);
    i.putOperand(1, Target);
    i.putOperand(2, BranchProfile);
    return i;
  }
  /**
   * Create an instruction of the MIR_CondBranch instruction format.
   * @param o the instruction's operator
   * @param Cond the instruction's Cond operand
   * @param BranchProfile the instruction's BranchProfile operand
   * @return the newly created MIR_CondBranch instruction
   */
  public static OPT_Instruction create(OPT_Operator o
                   , OPT_IA32ConditionOperand Cond
                   , OPT_BranchProfileOperand BranchProfile
                )
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(o)) fail(o, "MIR_CondBranch");
    OPT_Instruction i = new OPT_Instruction(o, 5);
    i.putOperand(0, Cond);
    i.putOperand(2, BranchProfile);
    return i;
  }

  /**
   * Mutate the argument instruction into an instruction of the
   * MIR_CondBranch instruction format having the specified
   * operator and operands.
   * @param i the instruction to mutate
   * @param o the instruction's operator
   * @param Cond the instruction's Cond operand
   * @param BranchProfile the instruction's BranchProfile operand
   * @return the mutated instruction
   */
  public static OPT_Instruction mutate(OPT_Instruction i, OPT_Operator o
                   , OPT_IA32ConditionOperand Cond
                   , OPT_BranchProfileOperand BranchProfile
                )
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(o)) fail(o, "MIR_CondBranch");
    i.operator = o;
    i.putOperand(0, Cond);
    i.putOperand(1, null);
    i.putOperand(2, BranchProfile);
    return i;
  }
}

