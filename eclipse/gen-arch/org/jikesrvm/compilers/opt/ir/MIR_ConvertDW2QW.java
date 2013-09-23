
/*
 * THIS FILE IS MACHINE_GENERATED. DO NOT EDIT.
 * See InstructionFormats.template, InstructionFormatList.dat,
 * OperatorList.dat, etc.
 */

package org.jikesrvm.compilers.opt.ir;

import org.jikesrvm.VM_Configuration;
import org.jikesrvm.compilers.opt.ir.ia32.OPT_IA32ConditionOperand; //NOPMD


/**
 * The MIR_ConvertDW2QW InstructionFormat class.
 *
 * The header comment for {@link OPT_Instruction} contains
 * an explanation of the role of InstructionFormats in the
 * opt compiler's IR.
 */
@SuppressWarnings("unused")  // Machine generated code is never 100% clean
public final class MIR_ConvertDW2QW extends OPT_InstructionFormat {
  /**
   * InstructionFormat identification method for MIR_ConvertDW2QW.
   * @param i an instruction
   * @return <code>true</code> if the InstructionFormat of the argument
   *         instruction is MIR_ConvertDW2QW or <code>false</code>
   *         if it is not.
   */
  public static boolean conforms(OPT_Instruction i) {
    return conforms(i.operator);
  }
  /**
   * InstructionFormat identification method for MIR_ConvertDW2QW.
   * @param o an instruction
   * @return <code>true</code> if the InstructionFormat of the argument
   *         operator is MIR_ConvertDW2QW or <code>false</code>
   *         if it is not.
   */
  public static boolean conforms(OPT_Operator o) {
    return o.format == MIR_ConvertDW2QW_format;
  }

  /**
   * Get the operand called Result1 from the
   * argument instruction. Note that the returned operand
   * will still point to its containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Result1
   */
  public static OPT_Operand getResult1(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_ConvertDW2QW");
    return (OPT_Operand) i.getOperand(0);
  }
  /**
   * Get the operand called Result1 from the argument
   * instruction clearing its instruction pointer. The returned
   * operand will not point to any containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Result1
   */
  public static OPT_Operand getClearResult1(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_ConvertDW2QW");
    return (OPT_Operand) i.getClearOperand(0);
  }
  /**
   * Set the operand called Result1 in the argument
   * instruction to the argument operand. The operand will
   * now point to the argument instruction as its containing
   * instruction.
   * @param i the instruction in which to store the operand
   * @param Result1 the operand to store
   */
  public static void setResult1(OPT_Instruction i, OPT_Operand Result1) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_ConvertDW2QW");
    i.putOperand(0, Result1);
  }
  /**
   * Return the index of the operand called Result1
   * in the argument instruction.
   * @param i the instruction to access.
   * @return the index of the operand called Result1
   *         in the argument instruction
   */
  public static int indexOfResult1(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_ConvertDW2QW");
    return 0;
  }
  /**
   * Does the argument instruction have a non-null
   * operand named Result1?
   * @param i the instruction to access.
   * @return <code>true</code> if the instruction has an non-null
   *         operand named Result1 or <code>false</code>
   *         if it does not.
   */
  public static boolean hasResult1(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_ConvertDW2QW");
    return i.getOperand(0) != null;
  }

  /**
   * Get the operand called Result2 from the
   * argument instruction. Note that the returned operand
   * will still point to its containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Result2
   */
  public static OPT_Operand getResult2(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_ConvertDW2QW");
    return (OPT_Operand) i.getOperand(1);
  }
  /**
   * Get the operand called Result2 from the argument
   * instruction clearing its instruction pointer. The returned
   * operand will not point to any containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Result2
   */
  public static OPT_Operand getClearResult2(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_ConvertDW2QW");
    return (OPT_Operand) i.getClearOperand(1);
  }
  /**
   * Set the operand called Result2 in the argument
   * instruction to the argument operand. The operand will
   * now point to the argument instruction as its containing
   * instruction.
   * @param i the instruction in which to store the operand
   * @param Result2 the operand to store
   */
  public static void setResult2(OPT_Instruction i, OPT_Operand Result2) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_ConvertDW2QW");
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
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_ConvertDW2QW");
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
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "MIR_ConvertDW2QW");
    return i.getOperand(1) != null;
  }


  /**
   * Create an instruction of the MIR_ConvertDW2QW instruction format.
   * @param o the instruction's operator
   * @param Result1 the instruction's Result1 operand
   * @param Result2 the instruction's Result2 operand
   * @return the newly created MIR_ConvertDW2QW instruction
   */
  public static OPT_Instruction create(OPT_Operator o
                   , OPT_Operand Result1
                   , OPT_Operand Result2
                )
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(o)) fail(o, "MIR_ConvertDW2QW");
    OPT_Instruction i = new OPT_Instruction(o, 5);
    i.putOperand(0, Result1);
    i.putOperand(1, Result2);
    return i;
  }

  /**
   * Mutate the argument instruction into an instruction of the
   * MIR_ConvertDW2QW instruction format having the specified
   * operator and operands.
   * @param i the instruction to mutate
   * @param o the instruction's operator
   * @param Result1 the instruction's Result1 operand
   * @param Result2 the instruction's Result2 operand
   * @return the mutated instruction
   */
  public static OPT_Instruction mutate(OPT_Instruction i, OPT_Operator o
                   , OPT_Operand Result1
                   , OPT_Operand Result2
                )
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(o)) fail(o, "MIR_ConvertDW2QW");
    i.operator = o;
    i.putOperand(0, Result1);
    i.putOperand(1, Result2);
    return i;
  }
}

