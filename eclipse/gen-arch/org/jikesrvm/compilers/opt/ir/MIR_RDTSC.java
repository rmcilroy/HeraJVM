
/*
 * THIS FILE IS MACHINE_GENERATED. DO NOT EDIT.
 * See InstructionFormats.template, InstructionFormatList.dat,
 * OperatorList.dat, etc.
 */

package org.jikesrvm.compilers.opt.ir;

import org.jikesrvm.VM_Configuration;
import org.jikesrvm.compilers.opt.ir.ia32.OPT_IA32ConditionOperand; //NOPMD


/**
 * The MIR_RDTSC InstructionFormat class.
 *
 * The header comment for {@link OPT_Instruction} contains
 * an explanation of the role of InstructionFormats in the
 * opt compiler's IR.
 */
@SuppressWarnings("unused")  // Machine generated code is never 100% clean
public final class MIR_RDTSC extends OPT_InstructionFormat {
  /**
   * InstructionFormat identification method for MIR_RDTSC.
   * @param i an instruction
   * @return <code>true</code> if the InstructionFormat of the argument
   *         instruction is MIR_RDTSC or <code>false</code>
   *         if it is not.
   */
  public static boolean conforms(OPT_Instruction i) {
    return conforms(i.operator);
  }
  /**
   * InstructionFormat identification method for MIR_RDTSC.
   * @param o an instruction
   * @return <code>true</code> if the InstructionFormat of the argument
   *         operator is MIR_RDTSC or <code>false</code>
   *         if it is not.
   */
  public static boolean conforms(OPT_Operator o) {
    return o.format == MIR_RDTSC_format;
  }

  /**
   * Get the operand called Dest1 from the
   * argument instruction. Note that the returned operand
   * will still point to its containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Dest1
   */
  public static OPT_RegisterOperand getDest1(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_RDTSC");
    return (OPT_RegisterOperand) i.getOperand(0);
  }
  /**
   * Get the operand called Dest1 from the argument
   * instruction clearing its instruction pointer. The returned
   * operand will not point to any containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Dest1
   */
  public static OPT_RegisterOperand getClearDest1(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_RDTSC");
    return (OPT_RegisterOperand) i.getClearOperand(0);
  }
  /**
   * Set the operand called Dest1 in the argument
   * instruction to the argument operand. The operand will
   * now point to the argument instruction as its containing
   * instruction.
   * @param i the instruction in which to store the operand
   * @param Dest1 the operand to store
   */
  public static void setDest1(OPT_Instruction i, OPT_RegisterOperand Dest1) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_RDTSC");
    i.putOperand(0, Dest1);
  }
  /**
   * Return the index of the operand called Dest1
   * in the argument instruction.
   * @param i the instruction to access.
   * @return the index of the operand called Dest1
   *         in the argument instruction
   */
  public static int indexOfDest1(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_RDTSC");
    return 0;
  }
  /**
   * Does the argument instruction have a non-null
   * operand named Dest1?
   * @param i the instruction to access.
   * @return <code>true</code> if the instruction has an non-null
   *         operand named Dest1 or <code>false</code>
   *         if it does not.
   */
  public static boolean hasDest1(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_RDTSC");
    return i.getOperand(0) != null;
  }

  /**
   * Get the operand called Dest2 from the
   * argument instruction. Note that the returned operand
   * will still point to its containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Dest2
   */
  public static OPT_RegisterOperand getDest2(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_RDTSC");
    return (OPT_RegisterOperand) i.getOperand(1);
  }
  /**
   * Get the operand called Dest2 from the argument
   * instruction clearing its instruction pointer. The returned
   * operand will not point to any containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Dest2
   */
  public static OPT_RegisterOperand getClearDest2(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_RDTSC");
    return (OPT_RegisterOperand) i.getClearOperand(1);
  }
  /**
   * Set the operand called Dest2 in the argument
   * instruction to the argument operand. The operand will
   * now point to the argument instruction as its containing
   * instruction.
   * @param i the instruction in which to store the operand
   * @param Dest2 the operand to store
   */
  public static void setDest2(OPT_Instruction i, OPT_RegisterOperand Dest2) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_RDTSC");
    i.putOperand(1, Dest2);
  }
  /**
   * Return the index of the operand called Dest2
   * in the argument instruction.
   * @param i the instruction to access.
   * @return the index of the operand called Dest2
   *         in the argument instruction
   */
  public static int indexOfDest2(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_RDTSC");
    return 1;
  }
  /**
   * Does the argument instruction have a non-null
   * operand named Dest2?
   * @param i the instruction to access.
   * @return <code>true</code> if the instruction has an non-null
   *         operand named Dest2 or <code>false</code>
   *         if it does not.
   */
  public static boolean hasDest2(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_RDTSC");
    return i.getOperand(1) != null;
  }


  /**
   * Create an instruction of the MIR_RDTSC instruction format.
   * @param o the instruction's operator
   * @param Dest1 the instruction's Dest1 operand
   * @param Dest2 the instruction's Dest2 operand
   * @return the newly created MIR_RDTSC instruction
   */
  public static OPT_Instruction create(OPT_Operator o
                   , OPT_RegisterOperand Dest1
                   , OPT_RegisterOperand Dest2
                )
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(o)) fail(o, "MIR_RDTSC");
    OPT_Instruction i = new OPT_Instruction(o, 5);
    i.putOperand(0, Dest1);
    i.putOperand(1, Dest2);
    return i;
  }

  /**
   * Mutate the argument instruction into an instruction of the
   * MIR_RDTSC instruction format having the specified
   * operator and operands.
   * @param i the instruction to mutate
   * @param o the instruction's operator
   * @param Dest1 the instruction's Dest1 operand
   * @param Dest2 the instruction's Dest2 operand
   * @return the mutated instruction
   */
  public static OPT_Instruction mutate(OPT_Instruction i, OPT_Operator o
                   , OPT_RegisterOperand Dest1
                   , OPT_RegisterOperand Dest2
                )
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(o)) fail(o, "MIR_RDTSC");
    i.operator = o;
    i.putOperand(0, Dest1);
    i.putOperand(1, Dest2);
    return i;
  }
}

