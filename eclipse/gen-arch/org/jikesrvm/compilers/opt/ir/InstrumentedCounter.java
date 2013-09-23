
/*
 * THIS FILE IS MACHINE_GENERATED. DO NOT EDIT.
 * See InstructionFormats.template, InstructionFormatList.dat,
 * OperatorList.dat, etc.
 */

package org.jikesrvm.compilers.opt.ir;

import org.jikesrvm.VM_Configuration;
import org.jikesrvm.compilers.opt.ir.ia32.OPT_IA32ConditionOperand; //NOPMD


/**
 * The InstrumentedCounter InstructionFormat class.
 *
 * The header comment for {@link OPT_Instruction} contains
 * an explanation of the role of InstructionFormats in the
 * opt compiler's IR.
 */
@SuppressWarnings("unused")  // Machine generated code is never 100% clean
public final class InstrumentedCounter extends OPT_InstructionFormat {
  /**
   * InstructionFormat identification method for InstrumentedCounter.
   * @param i an instruction
   * @return <code>true</code> if the InstructionFormat of the argument
   *         instruction is InstrumentedCounter or <code>false</code>
   *         if it is not.
   */
  public static boolean conforms(OPT_Instruction i) {
    return conforms(i.operator);
  }
  /**
   * InstructionFormat identification method for InstrumentedCounter.
   * @param o an instruction
   * @return <code>true</code> if the InstructionFormat of the argument
   *         operator is InstrumentedCounter or <code>false</code>
   *         if it is not.
   */
  public static boolean conforms(OPT_Operator o) {
    return o.format == InstrumentedCounter_format;
  }

  /**
   * Get the operand called Data from the
   * argument instruction. Note that the returned operand
   * will still point to its containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Data
   */
  public static OPT_IntConstantOperand getData(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "InstrumentedCounter");
    return (OPT_IntConstantOperand) i.getOperand(0);
  }
  /**
   * Get the operand called Data from the argument
   * instruction clearing its instruction pointer. The returned
   * operand will not point to any containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Data
   */
  public static OPT_IntConstantOperand getClearData(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "InstrumentedCounter");
    return (OPT_IntConstantOperand) i.getClearOperand(0);
  }
  /**
   * Set the operand called Data in the argument
   * instruction to the argument operand. The operand will
   * now point to the argument instruction as its containing
   * instruction.
   * @param i the instruction in which to store the operand
   * @param Data the operand to store
   */
  public static void setData(OPT_Instruction i, OPT_IntConstantOperand Data) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "InstrumentedCounter");
    i.putOperand(0, Data);
  }
  /**
   * Return the index of the operand called Data
   * in the argument instruction.
   * @param i the instruction to access.
   * @return the index of the operand called Data
   *         in the argument instruction
   */
  public static int indexOfData(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "InstrumentedCounter");
    return 0;
  }
  /**
   * Does the argument instruction have a non-null
   * operand named Data?
   * @param i the instruction to access.
   * @return <code>true</code> if the instruction has an non-null
   *         operand named Data or <code>false</code>
   *         if it does not.
   */
  public static boolean hasData(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "InstrumentedCounter");
    return i.getOperand(0) != null;
  }

  /**
   * Get the operand called Index from the
   * argument instruction. Note that the returned operand
   * will still point to its containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Index
   */
  public static OPT_IntConstantOperand getIndex(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "InstrumentedCounter");
    return (OPT_IntConstantOperand) i.getOperand(1);
  }
  /**
   * Get the operand called Index from the argument
   * instruction clearing its instruction pointer. The returned
   * operand will not point to any containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Index
   */
  public static OPT_IntConstantOperand getClearIndex(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "InstrumentedCounter");
    return (OPT_IntConstantOperand) i.getClearOperand(1);
  }
  /**
   * Set the operand called Index in the argument
   * instruction to the argument operand. The operand will
   * now point to the argument instruction as its containing
   * instruction.
   * @param i the instruction in which to store the operand
   * @param Index the operand to store
   */
  public static void setIndex(OPT_Instruction i, OPT_IntConstantOperand Index) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "InstrumentedCounter");
    i.putOperand(1, Index);
  }
  /**
   * Return the index of the operand called Index
   * in the argument instruction.
   * @param i the instruction to access.
   * @return the index of the operand called Index
   *         in the argument instruction
   */
  public static int indexOfIndex(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "InstrumentedCounter");
    return 1;
  }
  /**
   * Does the argument instruction have a non-null
   * operand named Index?
   * @param i the instruction to access.
   * @return <code>true</code> if the instruction has an non-null
   *         operand named Index or <code>false</code>
   *         if it does not.
   */
  public static boolean hasIndex(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "InstrumentedCounter");
    return i.getOperand(1) != null;
  }

  /**
   * Get the operand called Increment from the
   * argument instruction. Note that the returned operand
   * will still point to its containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Increment
   */
  public static OPT_Operand getIncrement(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "InstrumentedCounter");
    return (OPT_Operand) i.getOperand(2);
  }
  /**
   * Get the operand called Increment from the argument
   * instruction clearing its instruction pointer. The returned
   * operand will not point to any containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Increment
   */
  public static OPT_Operand getClearIncrement(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "InstrumentedCounter");
    return (OPT_Operand) i.getClearOperand(2);
  }
  /**
   * Set the operand called Increment in the argument
   * instruction to the argument operand. The operand will
   * now point to the argument instruction as its containing
   * instruction.
   * @param i the instruction in which to store the operand
   * @param Increment the operand to store
   */
  public static void setIncrement(OPT_Instruction i, OPT_Operand Increment) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "InstrumentedCounter");
    i.putOperand(2, Increment);
  }
  /**
   * Return the index of the operand called Increment
   * in the argument instruction.
   * @param i the instruction to access.
   * @return the index of the operand called Increment
   *         in the argument instruction
   */
  public static int indexOfIncrement(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "InstrumentedCounter");
    return 2;
  }
  /**
   * Does the argument instruction have a non-null
   * operand named Increment?
   * @param i the instruction to access.
   * @return <code>true</code> if the instruction has an non-null
   *         operand named Increment or <code>false</code>
   *         if it does not.
   */
  public static boolean hasIncrement(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "InstrumentedCounter");
    return i.getOperand(2) != null;
  }


  /**
   * Create an instruction of the InstrumentedCounter instruction format.
   * @param o the instruction's operator
   * @param Data the instruction's Data operand
   * @param Index the instruction's Index operand
   * @param Increment the instruction's Increment operand
   * @return the newly created InstrumentedCounter instruction
   */
  public static OPT_Instruction create(OPT_Operator o
                   , OPT_IntConstantOperand Data
                   , OPT_IntConstantOperand Index
                   , OPT_Operand Increment
                )
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(o)) fail(o, "InstrumentedCounter");
    OPT_Instruction i = new OPT_Instruction(o, 5);
    i.putOperand(0, Data);
    i.putOperand(1, Index);
    i.putOperand(2, Increment);
    return i;
  }

  /**
   * Mutate the argument instruction into an instruction of the
   * InstrumentedCounter instruction format having the specified
   * operator and operands.
   * @param i the instruction to mutate
   * @param o the instruction's operator
   * @param Data the instruction's Data operand
   * @param Index the instruction's Index operand
   * @param Increment the instruction's Increment operand
   * @return the mutated instruction
   */
  public static OPT_Instruction mutate(OPT_Instruction i, OPT_Operator o
                   , OPT_IntConstantOperand Data
                   , OPT_IntConstantOperand Index
                   , OPT_Operand Increment
                )
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(o)) fail(o, "InstrumentedCounter");
    i.operator = o;
    i.putOperand(0, Data);
    i.putOperand(1, Index);
    i.putOperand(2, Increment);
    return i;
  }
}

