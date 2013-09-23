
/*
 * THIS FILE IS MACHINE_GENERATED. DO NOT EDIT.
 * See InstructionFormats.template, InstructionFormatList.dat,
 * OperatorList.dat, etc.
 */

package org.jikesrvm.compilers.opt.ir;

import org.jikesrvm.VM_Configuration;
import org.jikesrvm.compilers.opt.ir.ia32.OPT_IA32ConditionOperand; //NOPMD


/**
 * The MonitorOp InstructionFormat class.
 *
 * The header comment for {@link OPT_Instruction} contains
 * an explanation of the role of InstructionFormats in the
 * opt compiler's IR.
 */
@SuppressWarnings("unused")  // Machine generated code is never 100% clean
public final class MonitorOp extends OPT_InstructionFormat {
  /**
   * InstructionFormat identification method for MonitorOp.
   * @param i an instruction
   * @return <code>true</code> if the InstructionFormat of the argument
   *         instruction is MonitorOp or <code>false</code>
   *         if it is not.
   */
  public static boolean conforms(OPT_Instruction i) {
    return conforms(i.operator);
  }
  /**
   * InstructionFormat identification method for MonitorOp.
   * @param o an instruction
   * @return <code>true</code> if the InstructionFormat of the argument
   *         operator is MonitorOp or <code>false</code>
   *         if it is not.
   */
  public static boolean conforms(OPT_Operator o) {
    return o.format == MonitorOp_format;
  }

  /**
   * Get the operand called Ref from the
   * argument instruction. Note that the returned operand
   * will still point to its containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Ref
   */
  public static OPT_Operand getRef(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MonitorOp");
    return (OPT_Operand) i.getOperand(0);
  }
  /**
   * Get the operand called Ref from the argument
   * instruction clearing its instruction pointer. The returned
   * operand will not point to any containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Ref
   */
  public static OPT_Operand getClearRef(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MonitorOp");
    return (OPT_Operand) i.getClearOperand(0);
  }
  /**
   * Set the operand called Ref in the argument
   * instruction to the argument operand. The operand will
   * now point to the argument instruction as its containing
   * instruction.
   * @param i the instruction in which to store the operand
   * @param Ref the operand to store
   */
  public static void setRef(OPT_Instruction i, OPT_Operand Ref) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MonitorOp");
    i.putOperand(0, Ref);
  }
  /**
   * Return the index of the operand called Ref
   * in the argument instruction.
   * @param i the instruction to access.
   * @return the index of the operand called Ref
   *         in the argument instruction
   */
  public static int indexOfRef(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MonitorOp");
    return 0;
  }
  /**
   * Does the argument instruction have a non-null
   * operand named Ref?
   * @param i the instruction to access.
   * @return <code>true</code> if the instruction has an non-null
   *         operand named Ref or <code>false</code>
   *         if it does not.
   */
  public static boolean hasRef(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MonitorOp");
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
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MonitorOp");
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
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MonitorOp");
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
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MonitorOp");
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
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MonitorOp");
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
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MonitorOp");
    return i.getOperand(1) != null;
  }


  /**
   * Create an instruction of the MonitorOp instruction format.
   * @param o the instruction's operator
   * @param Ref the instruction's Ref operand
   * @param Guard the instruction's Guard operand
   * @return the newly created MonitorOp instruction
   */
  public static OPT_Instruction create(OPT_Operator o
                   , OPT_Operand Ref
                   , OPT_Operand Guard
                )
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(o)) fail(o, "MonitorOp");
    OPT_Instruction i = new OPT_Instruction(o, 5);
    i.putOperand(0, Ref);
    i.putOperand(1, Guard);
    return i;
  }

  /**
   * Mutate the argument instruction into an instruction of the
   * MonitorOp instruction format having the specified
   * operator and operands.
   * @param i the instruction to mutate
   * @param o the instruction's operator
   * @param Ref the instruction's Ref operand
   * @param Guard the instruction's Guard operand
   * @return the mutated instruction
   */
  public static OPT_Instruction mutate(OPT_Instruction i, OPT_Operator o
                   , OPT_Operand Ref
                   , OPT_Operand Guard
                )
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(o)) fail(o, "MonitorOp");
    i.operator = o;
    i.putOperand(0, Ref);
    i.putOperand(1, Guard);
    return i;
  }
}

