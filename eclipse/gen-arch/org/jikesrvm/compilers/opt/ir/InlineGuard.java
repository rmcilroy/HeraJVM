
/*
 * THIS FILE IS MACHINE_GENERATED. DO NOT EDIT.
 * See InstructionFormats.template, InstructionFormatList.dat,
 * OperatorList.dat, etc.
 */

package org.jikesrvm.compilers.opt.ir;

import org.jikesrvm.VM_Configuration;
import org.jikesrvm.compilers.opt.ir.ia32.OPT_IA32ConditionOperand; //NOPMD


/**
 * The InlineGuard InstructionFormat class.
 *
 * The header comment for {@link OPT_Instruction} contains
 * an explanation of the role of InstructionFormats in the
 * opt compiler's IR.
 */
@SuppressWarnings("unused")  // Machine generated code is never 100% clean
public final class InlineGuard extends OPT_InstructionFormat {
  /**
   * InstructionFormat identification method for InlineGuard.
   * @param i an instruction
   * @return <code>true</code> if the InstructionFormat of the argument
   *         instruction is InlineGuard or <code>false</code>
   *         if it is not.
   */
  public static boolean conforms(OPT_Instruction i) {
    return conforms(i.operator);
  }
  /**
   * InstructionFormat identification method for InlineGuard.
   * @param o an instruction
   * @return <code>true</code> if the InstructionFormat of the argument
   *         operator is InlineGuard or <code>false</code>
   *         if it is not.
   */
  public static boolean conforms(OPT_Operator o) {
    return o.format == InlineGuard_format;
  }

  /**
   * Get the operand called Value from the
   * argument instruction. Note that the returned operand
   * will still point to its containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Value
   */
  public static OPT_Operand getValue(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "InlineGuard");
    return (OPT_Operand) i.getOperand(0);
  }
  /**
   * Get the operand called Value from the argument
   * instruction clearing its instruction pointer. The returned
   * operand will not point to any containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Value
   */
  public static OPT_Operand getClearValue(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "InlineGuard");
    return (OPT_Operand) i.getClearOperand(0);
  }
  /**
   * Set the operand called Value in the argument
   * instruction to the argument operand. The operand will
   * now point to the argument instruction as its containing
   * instruction.
   * @param i the instruction in which to store the operand
   * @param Value the operand to store
   */
  public static void setValue(OPT_Instruction i, OPT_Operand Value) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "InlineGuard");
    i.putOperand(0, Value);
  }
  /**
   * Return the index of the operand called Value
   * in the argument instruction.
   * @param i the instruction to access.
   * @return the index of the operand called Value
   *         in the argument instruction
   */
  public static int indexOfValue(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "InlineGuard");
    return 0;
  }
  /**
   * Does the argument instruction have a non-null
   * operand named Value?
   * @param i the instruction to access.
   * @return <code>true</code> if the instruction has an non-null
   *         operand named Value or <code>false</code>
   *         if it does not.
   */
  public static boolean hasValue(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "InlineGuard");
    return i.getOperand(0) != null;
  }

  /**
   * Get the operand called Guard from the
   * argument instruction. Note that the returned operand
   * will still point to its containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Guard
   */
  public static OPT_Operand getGuard(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "InlineGuard");
    return (OPT_Operand) i.getOperand(1);
  }
  /**
   * Get the operand called Guard from the argument
   * instruction clearing its instruction pointer. The returned
   * operand will not point to any containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Guard
   */
  public static OPT_Operand getClearGuard(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "InlineGuard");
    return (OPT_Operand) i.getClearOperand(1);
  }
  /**
   * Set the operand called Guard in the argument
   * instruction to the argument operand. The operand will
   * now point to the argument instruction as its containing
   * instruction.
   * @param i the instruction in which to store the operand
   * @param Guard the operand to store
   */
  public static void setGuard(OPT_Instruction i, OPT_Operand Guard) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "InlineGuard");
    i.putOperand(1, Guard);
  }
  /**
   * Return the index of the operand called Guard
   * in the argument instruction.
   * @param i the instruction to access.
   * @return the index of the operand called Guard
   *         in the argument instruction
   */
  public static int indexOfGuard(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "InlineGuard");
    return 1;
  }
  /**
   * Does the argument instruction have a non-null
   * operand named Guard?
   * @param i the instruction to access.
   * @return <code>true</code> if the instruction has an non-null
   *         operand named Guard or <code>false</code>
   *         if it does not.
   */
  public static boolean hasGuard(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "InlineGuard");
    return i.getOperand(1) != null;
  }

  /**
   * Get the operand called Goal from the
   * argument instruction. Note that the returned operand
   * will still point to its containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Goal
   */
  public static OPT_Operand getGoal(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "InlineGuard");
    return (OPT_Operand) i.getOperand(2);
  }
  /**
   * Get the operand called Goal from the argument
   * instruction clearing its instruction pointer. The returned
   * operand will not point to any containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Goal
   */
  public static OPT_Operand getClearGoal(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "InlineGuard");
    return (OPT_Operand) i.getClearOperand(2);
  }
  /**
   * Set the operand called Goal in the argument
   * instruction to the argument operand. The operand will
   * now point to the argument instruction as its containing
   * instruction.
   * @param i the instruction in which to store the operand
   * @param Goal the operand to store
   */
  public static void setGoal(OPT_Instruction i, OPT_Operand Goal) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "InlineGuard");
    i.putOperand(2, Goal);
  }
  /**
   * Return the index of the operand called Goal
   * in the argument instruction.
   * @param i the instruction to access.
   * @return the index of the operand called Goal
   *         in the argument instruction
   */
  public static int indexOfGoal(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "InlineGuard");
    return 2;
  }
  /**
   * Does the argument instruction have a non-null
   * operand named Goal?
   * @param i the instruction to access.
   * @return <code>true</code> if the instruction has an non-null
   *         operand named Goal or <code>false</code>
   *         if it does not.
   */
  public static boolean hasGoal(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "InlineGuard");
    return i.getOperand(2) != null;
  }

  /**
   * Get the operand called Target from the
   * argument instruction. Note that the returned operand
   * will still point to its containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Target
   */
  public static OPT_BranchOperand getTarget(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "InlineGuard");
    return (OPT_BranchOperand) i.getOperand(3);
  }
  /**
   * Get the operand called Target from the argument
   * instruction clearing its instruction pointer. The returned
   * operand will not point to any containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Target
   */
  public static OPT_BranchOperand getClearTarget(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "InlineGuard");
    return (OPT_BranchOperand) i.getClearOperand(3);
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
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "InlineGuard");
    i.putOperand(3, Target);
  }
  /**
   * Return the index of the operand called Target
   * in the argument instruction.
   * @param i the instruction to access.
   * @return the index of the operand called Target
   *         in the argument instruction
   */
  public static int indexOfTarget(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "InlineGuard");
    return 3;
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
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "InlineGuard");
    return i.getOperand(3) != null;
  }

  /**
   * Get the operand called BranchProfile from the
   * argument instruction. Note that the returned operand
   * will still point to its containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called BranchProfile
   */
  public static OPT_BranchProfileOperand getBranchProfile(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "InlineGuard");
    return (OPT_BranchProfileOperand) i.getOperand(4);
  }
  /**
   * Get the operand called BranchProfile from the argument
   * instruction clearing its instruction pointer. The returned
   * operand will not point to any containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called BranchProfile
   */
  public static OPT_BranchProfileOperand getClearBranchProfile(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "InlineGuard");
    return (OPT_BranchProfileOperand) i.getClearOperand(4);
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
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "InlineGuard");
    i.putOperand(4, BranchProfile);
  }
  /**
   * Return the index of the operand called BranchProfile
   * in the argument instruction.
   * @param i the instruction to access.
   * @return the index of the operand called BranchProfile
   *         in the argument instruction
   */
  public static int indexOfBranchProfile(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "InlineGuard");
    return 4;
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
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "InlineGuard");
    return i.getOperand(4) != null;
  }


  /**
   * Create an instruction of the InlineGuard instruction format.
   * @param o the instruction's operator
   * @param Value the instruction's Value operand
   * @param Guard the instruction's Guard operand
   * @param Goal the instruction's Goal operand
   * @param Target the instruction's Target operand
   * @param BranchProfile the instruction's BranchProfile operand
   * @return the newly created InlineGuard instruction
   */
  public static OPT_Instruction create(OPT_Operator o
                   , OPT_Operand Value
                   , OPT_Operand Guard
                   , OPT_Operand Goal
                   , OPT_BranchOperand Target
                   , OPT_BranchProfileOperand BranchProfile
                )
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(o)) fail(o, "InlineGuard");
    OPT_Instruction i = new OPT_Instruction(o, 5);
    i.putOperand(0, Value);
    i.putOperand(1, Guard);
    i.putOperand(2, Goal);
    i.putOperand(3, Target);
    i.putOperand(4, BranchProfile);
    return i;
  }

  /**
   * Mutate the argument instruction into an instruction of the
   * InlineGuard instruction format having the specified
   * operator and operands.
   * @param i the instruction to mutate
   * @param o the instruction's operator
   * @param Value the instruction's Value operand
   * @param Guard the instruction's Guard operand
   * @param Goal the instruction's Goal operand
   * @param Target the instruction's Target operand
   * @param BranchProfile the instruction's BranchProfile operand
   * @return the mutated instruction
   */
  public static OPT_Instruction mutate(OPT_Instruction i, OPT_Operator o
                   , OPT_Operand Value
                   , OPT_Operand Guard
                   , OPT_Operand Goal
                   , OPT_BranchOperand Target
                   , OPT_BranchProfileOperand BranchProfile
                )
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(o)) fail(o, "InlineGuard");
    i.operator = o;
    i.putOperand(0, Value);
    i.putOperand(1, Guard);
    i.putOperand(2, Goal);
    i.putOperand(3, Target);
    i.putOperand(4, BranchProfile);
    return i;
  }
}

