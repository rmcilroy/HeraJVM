
/*
 * THIS FILE IS MACHINE_GENERATED. DO NOT EDIT.
 * See InstructionFormats.template, InstructionFormatList.dat,
 * OperatorList.dat, etc.
 */

package org.jikesrvm.compilers.opt.ir;

import org.jikesrvm.VM_Configuration;
import org.jikesrvm.compilers.opt.ir.ia32.OPT_IA32ConditionOperand; //NOPMD


/**
 * The LookupSwitch InstructionFormat class.
 *
 * The header comment for {@link OPT_Instruction} contains
 * an explanation of the role of InstructionFormats in the
 * opt compiler's IR.
 */
@SuppressWarnings("unused")  // Machine generated code is never 100% clean
public final class LookupSwitch extends OPT_InstructionFormat {
  /**
   * InstructionFormat identification method for LookupSwitch.
   * @param i an instruction
   * @return <code>true</code> if the InstructionFormat of the argument
   *         instruction is LookupSwitch or <code>false</code>
   *         if it is not.
   */
  public static boolean conforms(OPT_Instruction i) {
    return conforms(i.operator);
  }
  /**
   * InstructionFormat identification method for LookupSwitch.
   * @param o an instruction
   * @return <code>true</code> if the InstructionFormat of the argument
   *         operator is LookupSwitch or <code>false</code>
   *         if it is not.
   */
  public static boolean conforms(OPT_Operator o) {
    return o.format == LookupSwitch_format;
  }

  /**
   * Get the operand called Value from the
   * argument instruction. Note that the returned operand
   * will still point to its containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Value
   */
  public static OPT_Operand getValue(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "LookupSwitch");
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
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "LookupSwitch");
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
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "LookupSwitch");
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
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "LookupSwitch");
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
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "LookupSwitch");
    return i.getOperand(0) != null;
  }

  /**
   * Get the operand called Unknown1 from the
   * argument instruction. Note that the returned operand
   * will still point to its containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Unknown1
   */
  public static OPT_Operand getUnknown1(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "LookupSwitch");
    return (OPT_Operand) i.getOperand(1);
  }
  /**
   * Get the operand called Unknown1 from the argument
   * instruction clearing its instruction pointer. The returned
   * operand will not point to any containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Unknown1
   */
  public static OPT_Operand getClearUnknown1(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "LookupSwitch");
    return (OPT_Operand) i.getClearOperand(1);
  }
  /**
   * Set the operand called Unknown1 in the argument
   * instruction to the argument operand. The operand will
   * now point to the argument instruction as its containing
   * instruction.
   * @param i the instruction in which to store the operand
   * @param Unknown1 the operand to store
   */
  public static void setUnknown1(OPT_Instruction i, OPT_Operand Unknown1) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "LookupSwitch");
    i.putOperand(1, Unknown1);
  }
  /**
   * Return the index of the operand called Unknown1
   * in the argument instruction.
   * @param i the instruction to access.
   * @return the index of the operand called Unknown1
   *         in the argument instruction
   */
  public static int indexOfUnknown1(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "LookupSwitch");
    return 1;
  }
  /**
   * Does the argument instruction have a non-null
   * operand named Unknown1?
   * @param i the instruction to access.
   * @return <code>true</code> if the instruction has an non-null
   *         operand named Unknown1 or <code>false</code>
   *         if it does not.
   */
  public static boolean hasUnknown1(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "LookupSwitch");
    return i.getOperand(1) != null;
  }

  /**
   * Get the operand called Unknown2 from the
   * argument instruction. Note that the returned operand
   * will still point to its containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Unknown2
   */
  public static OPT_Operand getUnknown2(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "LookupSwitch");
    return (OPT_Operand) i.getOperand(2);
  }
  /**
   * Get the operand called Unknown2 from the argument
   * instruction clearing its instruction pointer. The returned
   * operand will not point to any containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Unknown2
   */
  public static OPT_Operand getClearUnknown2(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "LookupSwitch");
    return (OPT_Operand) i.getClearOperand(2);
  }
  /**
   * Set the operand called Unknown2 in the argument
   * instruction to the argument operand. The operand will
   * now point to the argument instruction as its containing
   * instruction.
   * @param i the instruction in which to store the operand
   * @param Unknown2 the operand to store
   */
  public static void setUnknown2(OPT_Instruction i, OPT_Operand Unknown2) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "LookupSwitch");
    i.putOperand(2, Unknown2);
  }
  /**
   * Return the index of the operand called Unknown2
   * in the argument instruction.
   * @param i the instruction to access.
   * @return the index of the operand called Unknown2
   *         in the argument instruction
   */
  public static int indexOfUnknown2(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "LookupSwitch");
    return 2;
  }
  /**
   * Does the argument instruction have a non-null
   * operand named Unknown2?
   * @param i the instruction to access.
   * @return <code>true</code> if the instruction has an non-null
   *         operand named Unknown2 or <code>false</code>
   *         if it does not.
   */
  public static boolean hasUnknown2(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "LookupSwitch");
    return i.getOperand(2) != null;
  }

  /**
   * Get the operand called Default from the
   * argument instruction. Note that the returned operand
   * will still point to its containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Default
   */
  public static OPT_BranchOperand getDefault(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "LookupSwitch");
    return (OPT_BranchOperand) i.getOperand(3);
  }
  /**
   * Get the operand called Default from the argument
   * instruction clearing its instruction pointer. The returned
   * operand will not point to any containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Default
   */
  public static OPT_BranchOperand getClearDefault(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "LookupSwitch");
    return (OPT_BranchOperand) i.getClearOperand(3);
  }
  /**
   * Set the operand called Default in the argument
   * instruction to the argument operand. The operand will
   * now point to the argument instruction as its containing
   * instruction.
   * @param i the instruction in which to store the operand
   * @param Default the operand to store
   */
  public static void setDefault(OPT_Instruction i, OPT_BranchOperand Default) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "LookupSwitch");
    i.putOperand(3, Default);
  }
  /**
   * Return the index of the operand called Default
   * in the argument instruction.
   * @param i the instruction to access.
   * @return the index of the operand called Default
   *         in the argument instruction
   */
  public static int indexOfDefault(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "LookupSwitch");
    return 3;
  }
  /**
   * Does the argument instruction have a non-null
   * operand named Default?
   * @param i the instruction to access.
   * @return <code>true</code> if the instruction has an non-null
   *         operand named Default or <code>false</code>
   *         if it does not.
   */
  public static boolean hasDefault(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "LookupSwitch");
    return i.getOperand(3) != null;
  }

  /**
   * Get the operand called DefaultBranchProfile from the
   * argument instruction. Note that the returned operand
   * will still point to its containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called DefaultBranchProfile
   */
  public static OPT_BranchProfileOperand getDefaultBranchProfile(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "LookupSwitch");
    return (OPT_BranchProfileOperand) i.getOperand(4);
  }
  /**
   * Get the operand called DefaultBranchProfile from the argument
   * instruction clearing its instruction pointer. The returned
   * operand will not point to any containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called DefaultBranchProfile
   */
  public static OPT_BranchProfileOperand getClearDefaultBranchProfile(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "LookupSwitch");
    return (OPT_BranchProfileOperand) i.getClearOperand(4);
  }
  /**
   * Set the operand called DefaultBranchProfile in the argument
   * instruction to the argument operand. The operand will
   * now point to the argument instruction as its containing
   * instruction.
   * @param i the instruction in which to store the operand
   * @param DefaultBranchProfile the operand to store
   */
  public static void setDefaultBranchProfile(OPT_Instruction i, OPT_BranchProfileOperand DefaultBranchProfile) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "LookupSwitch");
    i.putOperand(4, DefaultBranchProfile);
  }
  /**
   * Return the index of the operand called DefaultBranchProfile
   * in the argument instruction.
   * @param i the instruction to access.
   * @return the index of the operand called DefaultBranchProfile
   *         in the argument instruction
   */
  public static int indexOfDefaultBranchProfile(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "LookupSwitch");
    return 4;
  }
  /**
   * Does the argument instruction have a non-null
   * operand named DefaultBranchProfile?
   * @param i the instruction to access.
   * @return <code>true</code> if the instruction has an non-null
   *         operand named DefaultBranchProfile or <code>false</code>
   *         if it does not.
   */
  public static boolean hasDefaultBranchProfile(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "LookupSwitch");
    return i.getOperand(4) != null;
  }

  /**
   * Get the k'th operand called Match from the
   * argument instruction. Note that the returned operand
   * will still point to its containing instruction.
   * @param i the instruction to fetch the operand from
   * @param k the index of the operand
   * @return the k'th operand called Match
   */
  public static OPT_IntConstantOperand getMatch(OPT_Instruction i, int k) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "LookupSwitch");
    return (OPT_IntConstantOperand) i.getOperand(5+k*3+0);
  }
  /**
   * Get the k'th operand called Match from the argument
   * instruction clearing its instruction pointer. The returned
   * operand will not point to any containing instruction.
   * @param i the instruction to fetch the operand from
   * @param k the index of the operand
   * @return the k'th operand called Match
   */
  public static OPT_IntConstantOperand getClearMatch(OPT_Instruction i, int k) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "LookupSwitch");
    return (OPT_IntConstantOperand) i.getClearOperand(5+k*3+0);
  }
  /**
   * Set the k'th operand called Match in the argument
   * instruction to the argument operand. The operand will
   * now point to the argument instruction as its containing
   * instruction.
   * @param i the instruction in which to store the operand
   * @param k the index of the operand
   * @param o the operand to store
   */
  public static void setMatch(OPT_Instruction i, int k, OPT_IntConstantOperand o) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "LookupSwitch");
    i.putOperand(5+k*3+0, o);
  }
  /**
   * Return the index of the k'th operand called Match
   * in the argument instruction.
   * @param i the instruction to access.
   * @param k the index of the operand.
   * @return the index of the k'th operand called Match
   *         in the argument instruction
   */
  public static int indexOfMatch(OPT_Instruction i, int k) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "LookupSwitch");
    return 5+k*3+0;
  }
  /**
   * Does the argument instruction have a non-null
   * k'th operand named Match?
   * @param i the instruction to access.
   * @param k the index of the operand.
   * @return <code>true</code> if the instruction has an non-null
   *         k'th operand named Match or <code>false</code>
   *         if it does not.
   */
  public static boolean hasMatch(OPT_Instruction i, int k) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "LookupSwitch");
    return i.getOperand(5+k*3+0) != null;
  }

  /**
   * Return the index of the first operand called Matches
   * in the argument instruction.
   * @param i the instruction to access.
   * @return the index of the first operand called Matches
   *         in the argument instruction
   */
  public static int indexOfMatches(OPT_Instruction i)
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "LookupSwitch");
    return 5;
  }
  /**
   * Does the argument instruction have any Matches
   * operands?
   * @param i the instruction to access.
   * @return <code>true</code> if the instruction has
   *         Matches operands or <code>false</code>
   *         if it does not.
   */
  public static boolean hasMatches(OPT_Instruction i)
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "LookupSwitch");
    return i.getNumberOfOperands()-5 > 0 && i.getOperand(5) != null;
  }

  /**
   * How many variable-length operands called Matches
   * does the argument instruction have?
   * @param i the instruction to access
   * @return the number of Matches operands the instruction has
   */
  public static int getNumberOfMatches(OPT_Instruction i)
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "LookupSwitch");
    return (i.getNumberOfOperands()-5)/3;
  }

  /**
   * Change the number of Matches operands that may be stored in
   * the argument instruction to numVarOps.
   * @param i the instruction to access
   * @param numVarOps the new number of variable operands called Matches
   *        that may be stored in the instruction
   */
  public static void resizeNumberOfMatches(OPT_Instruction i, int numVarOps)
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "LookupSwitch");
  if (5+numVarOps*3>MIN_OPERAND_ARRAY_LENGTH)
    i.resizeNumberOfOperands(5+numVarOps*3);
  else
    for (int j = 5+numVarOps*3; j < MIN_OPERAND_ARRAY_LENGTH; j++)
      i.putOperand(j, null);
  }
  /**
   * Get the k'th operand called Target from the
   * argument instruction. Note that the returned operand
   * will still point to its containing instruction.
   * @param i the instruction to fetch the operand from
   * @param k the index of the operand
   * @return the k'th operand called Target
   */
  public static OPT_BranchOperand getTarget(OPT_Instruction i, int k) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "LookupSwitch");
    return (OPT_BranchOperand) i.getOperand(5+k*3+1);
  }
  /**
   * Get the k'th operand called Target from the argument
   * instruction clearing its instruction pointer. The returned
   * operand will not point to any containing instruction.
   * @param i the instruction to fetch the operand from
   * @param k the index of the operand
   * @return the k'th operand called Target
   */
  public static OPT_BranchOperand getClearTarget(OPT_Instruction i, int k) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "LookupSwitch");
    return (OPT_BranchOperand) i.getClearOperand(5+k*3+1);
  }
  /**
   * Set the k'th operand called Target in the argument
   * instruction to the argument operand. The operand will
   * now point to the argument instruction as its containing
   * instruction.
   * @param i the instruction in which to store the operand
   * @param k the index of the operand
   * @param o the operand to store
   */
  public static void setTarget(OPT_Instruction i, int k, OPT_BranchOperand o) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "LookupSwitch");
    i.putOperand(5+k*3+1, o);
  }
  /**
   * Return the index of the k'th operand called Target
   * in the argument instruction.
   * @param i the instruction to access.
   * @param k the index of the operand.
   * @return the index of the k'th operand called Target
   *         in the argument instruction
   */
  public static int indexOfTarget(OPT_Instruction i, int k) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "LookupSwitch");
    return 5+k*3+1;
  }
  /**
   * Does the argument instruction have a non-null
   * k'th operand named Target?
   * @param i the instruction to access.
   * @param k the index of the operand.
   * @return <code>true</code> if the instruction has an non-null
   *         k'th operand named Target or <code>false</code>
   *         if it does not.
   */
  public static boolean hasTarget(OPT_Instruction i, int k) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "LookupSwitch");
    return i.getOperand(5+k*3+1) != null;
  }

  /**
   * Return the index of the first operand called Target
   * in the argument instruction.
   * @param i the instruction to access.
   * @return the index of the first operand called Target
   *         in the argument instruction
   */
  public static int indexOfTargets(OPT_Instruction i)
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "LookupSwitch");
    return 6;
  }
  /**
   * Does the argument instruction have any operands
   * named Target?
   * @param i the instruction to access.
   * @return <code>true</code> if the instruction has operands
   *         named Target or <code>false</code> if it does not.
   */
  public static boolean hasTargets(OPT_Instruction i)
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "LookupSwitch");
    return i.getNumberOfOperands()-6 > 0 && i.getOperand(6) != null;
  }

  /**
   * How many variable-length operands called Targets
   * does the argument instruction have?
   * @param i the instruction to access
   * @return the number of operands called Targets the instruction has
   */
  public static int getNumberOfTargets(OPT_Instruction i)
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "LookupSwitch");
    return (i.getNumberOfOperands()-5)/3;
  }

  /**
   * Change the number of Targets that may be stored in
   * the argument instruction to numVarOps.
   * @param i the instruction to access
   * @param numVarOps the new number of variable operands called Targets
   *        that may be stored in the instruction
   */
  public static void resizeNumberOfTargets(OPT_Instruction i, int numVarOps)
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "LookupSwitch");
  if (5+numVarOps*3>MIN_OPERAND_ARRAY_LENGTH)
    i.resizeNumberOfOperands(5+numVarOps*3);
  else
    for (int j = 5+numVarOps*3; j < MIN_OPERAND_ARRAY_LENGTH; j++)
      i.putOperand(j, null);
  }
  /**
   * Get the k'th operand called BranchProfile from the
   * argument instruction. Note that the returned operand
   * will still point to its containing instruction.
   * @param i the instruction to fetch the operand from
   * @param k the index of the operand
   * @return the k'th operand called BranchProfile
   */
  public static OPT_BranchProfileOperand getBranchProfile(OPT_Instruction i, int k) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "LookupSwitch");
    return (OPT_BranchProfileOperand) i.getOperand(5+k*3+2);
  }
  /**
   * Get the k'th operand called BranchProfile from the argument
   * instruction clearing its instruction pointer. The returned
   * operand will not point to any containing instruction.
   * @param i the instruction to fetch the operand from
   * @param k the index of the operand
   * @return the k'th operand called BranchProfile
   */
  public static OPT_BranchProfileOperand getClearBranchProfile(OPT_Instruction i, int k) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "LookupSwitch");
    return (OPT_BranchProfileOperand) i.getClearOperand(5+k*3+2);
  }
  /**
   * Set the k'th operand called BranchProfile in the argument
   * instruction to the argument operand. The operand will
   * now point to the argument instruction as its containing
   * instruction.
   * @param i the instruction in which to store the operand
   * @param k the index of the operand
   * @param o the operand to store
   */
  public static void setBranchProfile(OPT_Instruction i, int k, OPT_BranchProfileOperand o) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "LookupSwitch");
    i.putOperand(5+k*3+2, o);
  }
  /**
   * Return the index of the k'th operand called BranchProfile
   * in the argument instruction.
   * @param i the instruction to access.
   * @param k the index of the operand.
   * @return the index of the k'th operand called BranchProfile
   *         in the argument instruction
   */
  public static int indexOfBranchProfile(OPT_Instruction i, int k) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "LookupSwitch");
    return 5+k*3+2;
  }
  /**
   * Does the argument instruction have a non-null
   * k'th operand named BranchProfile?
   * @param i the instruction to access.
   * @param k the index of the operand.
   * @return <code>true</code> if the instruction has an non-null
   *         k'th operand named BranchProfile or <code>false</code>
   *         if it does not.
   */
  public static boolean hasBranchProfile(OPT_Instruction i, int k) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "LookupSwitch");
    return i.getOperand(5+k*3+2) != null;
  }

  /**
   * Return the index of the first operand called BranchProfile
   * in the argument instruction.
   * @param i the instruction to access.
   * @return the index of the first operand called BranchProfile
   *         in the argument instruction
   */
  public static int indexOfBranchProfiles(OPT_Instruction i)
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "LookupSwitch");
    return 7;
  }
  /**
   * Does the argument instruction have any operands
   * named BranchProfile?
   * @param i the instruction to access.
   * @return <code>true</code> if the instruction has operands
   *         named BranchProfile or <code>false</code> if it does not.
   */
  public static boolean hasBranchProfiles(OPT_Instruction i)
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "LookupSwitch");
    return i.getNumberOfOperands()-7 > 0 && i.getOperand(7) != null;
  }

  /**
   * How many variable-length operands called BranchProfiles
   * does the argument instruction have?
   * @param i the instruction to access
   * @return the number of operands called BranchProfiles the instruction has
   */
  public static int getNumberOfBranchProfiles(OPT_Instruction i)
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "LookupSwitch");
    return (i.getNumberOfOperands()-5)/3;
  }

  /**
   * Change the number of BranchProfiles that may be stored in
   * the argument instruction to numVarOps.
   * @param i the instruction to access
   * @param numVarOps the new number of variable operands called BranchProfiles
   *        that may be stored in the instruction
   */
  public static void resizeNumberOfBranchProfiles(OPT_Instruction i, int numVarOps)
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "LookupSwitch");
  if (5+numVarOps*3>MIN_OPERAND_ARRAY_LENGTH)
    i.resizeNumberOfOperands(5+numVarOps*3);
  else
    for (int j = 5+numVarOps*3; j < MIN_OPERAND_ARRAY_LENGTH; j++)
      i.putOperand(j, null);
  }

  /**
   * Create an instruction of the LookupSwitch instruction format.
   * @param o the instruction's operator
   * @param Value the instruction's Value operand
   * @param Unknown1 the instruction's Unknown1 operand
   * @param Unknown2 the instruction's Unknown2 operand
   * @param Default the instruction's Default operand
   * @param DefaultBranchProfile the instruction's DefaultBranchProfile operand
   * @param numVarOps the number of variable length operands that
   *                 will be stored in the insruction.
   * @return the newly created LookupSwitch instruction
   */
  public static OPT_Instruction create(OPT_Operator o
                   , OPT_Operand Value
                   , OPT_Operand Unknown1
                   , OPT_Operand Unknown2
                   , OPT_BranchOperand Default
                   , OPT_BranchProfileOperand DefaultBranchProfile
                   , int numVarOps
                )
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(o)) fail(o, "LookupSwitch");
    OPT_Instruction i = new OPT_Instruction(o, Math.max(5+numVarOps*3, MIN_OPERAND_ARRAY_LENGTH));
    i.putOperand(0, Value);
    i.putOperand(1, Unknown1);
    i.putOperand(2, Unknown2);
    i.putOperand(3, Default);
    i.putOperand(4, DefaultBranchProfile);
    return i;
  }

  /**
   * Mutate the argument instruction into an instruction of the
   * LookupSwitch instruction format having the specified
   * operator and operands.
   * @param i the instruction to mutate
   * @param o the instruction's operator
   * @param Value the instruction's Value operand
   * @param Unknown1 the instruction's Unknown1 operand
   * @param Unknown2 the instruction's Unknown2 operand
   * @param Default the instruction's Default operand
   * @param DefaultBranchProfile the instruction's DefaultBranchProfile operand
   * @param numVarOps the number of variable length operands that
   *                  will be stored in the insruction.
   * @return the mutated instruction
   */
  public static OPT_Instruction mutate(OPT_Instruction i, OPT_Operator o
                   , OPT_Operand Value
                   , OPT_Operand Unknown1
                   , OPT_Operand Unknown2
                   , OPT_BranchOperand Default
                   , OPT_BranchProfileOperand DefaultBranchProfile
                   , int numVarOps
                )
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(o)) fail(o, "LookupSwitch");
    if (5+numVarOps*3>MIN_OPERAND_ARRAY_LENGTH)
      i.resizeNumberOfOperands(5+numVarOps*3);

    i.operator = o;
    i.putOperand(0, Value);
    i.putOperand(1, Unknown1);
    i.putOperand(2, Unknown2);
    i.putOperand(3, Default);
    i.putOperand(4, DefaultBranchProfile);
    return i;
  }
}

