
/*
 * THIS FILE IS MACHINE_GENERATED. DO NOT EDIT.
 * See InstructionFormats.template, InstructionFormatList.dat,
 * OperatorList.dat, etc.
 */

package org.jikesrvm.compilers.opt.ir;

import org.jikesrvm.VM_Configuration;
import org.jikesrvm.compilers.opt.ir.ia32.OPT_IA32ConditionOperand; //NOPMD


/**
 * The Return InstructionFormat class.
 *
 * The header comment for {@link OPT_Instruction} contains
 * an explanation of the role of InstructionFormats in the
 * opt compiler's IR.
 */
@SuppressWarnings("unused")  // Machine generated code is never 100% clean
public final class Return extends OPT_InstructionFormat {
  /**
   * InstructionFormat identification method for Return.
   * @param i an instruction
   * @return <code>true</code> if the InstructionFormat of the argument
   *         instruction is Return or <code>false</code>
   *         if it is not.
   */
  public static boolean conforms(OPT_Instruction i) {
    return conforms(i.operator);
  }
  /**
   * InstructionFormat identification method for Return.
   * @param o an instruction
   * @return <code>true</code> if the InstructionFormat of the argument
   *         operator is Return or <code>false</code>
   *         if it is not.
   */
  public static boolean conforms(OPT_Operator o) {
    return o.format == Return_format;
  }

  /**
   * Get the operand called Val from the
   * argument instruction. Note that the returned operand
   * will still point to its containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Val
   */
  public static OPT_Operand getVal(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Return");
    return (OPT_Operand) i.getOperand(0);
  }
  /**
   * Get the operand called Val from the argument
   * instruction clearing its instruction pointer. The returned
   * operand will not point to any containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Val
   */
  public static OPT_Operand getClearVal(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Return");
    return (OPT_Operand) i.getClearOperand(0);
  }
  /**
   * Set the operand called Val in the argument
   * instruction to the argument operand. The operand will
   * now point to the argument instruction as its containing
   * instruction.
   * @param i the instruction in which to store the operand
   * @param Val the operand to store
   */
  public static void setVal(OPT_Instruction i, OPT_Operand Val) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Return");
    i.putOperand(0, Val);
  }
  /**
   * Return the index of the operand called Val
   * in the argument instruction.
   * @param i the instruction to access.
   * @return the index of the operand called Val
   *         in the argument instruction
   */
  public static int indexOfVal(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Return");
    return 0;
  }
  /**
   * Does the argument instruction have a non-null
   * operand named Val?
   * @param i the instruction to access.
   * @return <code>true</code> if the instruction has an non-null
   *         operand named Val or <code>false</code>
   *         if it does not.
   */
  public static boolean hasVal(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Return");
    return i.getOperand(0) != null;
  }


  /**
   * Create an instruction of the Return instruction format.
   * @param o the instruction's operator
   * @param Val the instruction's Val operand
   * @return the newly created Return instruction
   */
  public static OPT_Instruction create(OPT_Operator o
                   , OPT_Operand Val
                )
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(o)) fail(o, "Return");
    OPT_Instruction i = new OPT_Instruction(o, 5);
    i.putOperand(0, Val);
    return i;
  }

  /**
   * Mutate the argument instruction into an instruction of the
   * Return instruction format having the specified
   * operator and operands.
   * @param i the instruction to mutate
   * @param o the instruction's operator
   * @param Val the instruction's Val operand
   * @return the mutated instruction
   */
  public static OPT_Instruction mutate(OPT_Instruction i, OPT_Operator o
                   , OPT_Operand Val
                )
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(o)) fail(o, "Return");
    i.operator = o;
    i.putOperand(0, Val);
    return i;
  }
  /**
   * Create an instruction of the Return instruction format.
   * @param o the instruction's operator
   * @return the newly created Return instruction
   */
  public static OPT_Instruction create(OPT_Operator o
                )
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(o)) fail(o, "Return");
    OPT_Instruction i = new OPT_Instruction(o, 5);
    return i;
  }

  /**
   * Mutate the argument instruction into an instruction of the
   * Return instruction format having the specified
   * operator and operands.
   * @param i the instruction to mutate
   * @param o the instruction's operator
   * @return the mutated instruction
   */
  public static OPT_Instruction mutate(OPT_Instruction i, OPT_Operator o
                )
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(o)) fail(o, "Return");
    i.operator = o;
    i.putOperand(0, null);
    return i;
  }
}

