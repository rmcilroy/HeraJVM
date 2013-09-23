
/*
 * THIS FILE IS MACHINE_GENERATED. DO NOT EDIT.
 * See InstructionFormats.template, InstructionFormatList.dat,
 * OperatorList.dat, etc.
 */

package org.jikesrvm.compilers.opt.ir;

import org.jikesrvm.VM_Configuration;
import org.jikesrvm.compilers.opt.ir.ia32.OPT_IA32ConditionOperand; //NOPMD


/**
 * The Empty InstructionFormat class.
 *
 * The header comment for {@link OPT_Instruction} contains
 * an explanation of the role of InstructionFormats in the
 * opt compiler's IR.
 */
@SuppressWarnings("unused")  // Machine generated code is never 100% clean
public final class Empty extends OPT_InstructionFormat {
  /**
   * InstructionFormat identification method for Empty.
   * @param i an instruction
   * @return <code>true</code> if the InstructionFormat of the argument
   *         instruction is Empty or <code>false</code>
   *         if it is not.
   */
  public static boolean conforms(OPT_Instruction i) {
    return conforms(i.operator);
  }
  /**
   * InstructionFormat identification method for Empty.
   * @param o an instruction
   * @return <code>true</code> if the InstructionFormat of the argument
   *         operator is Empty or <code>false</code>
   *         if it is not.
   */
  public static boolean conforms(OPT_Operator o) {
    return o.format == Empty_format;
  }


  /**
   * Create an instruction of the Empty instruction format.
   * @param o the instruction's operator
   * @return the newly created Empty instruction
   */
  public static OPT_Instruction create(OPT_Operator o
                )
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(o)) fail(o, "Empty");
    OPT_Instruction i = new OPT_Instruction(o, 5);
    return i;
  }

  /**
   * Mutate the argument instruction into an instruction of the
   * Empty instruction format having the specified
   * operator and operands.
   * @param i the instruction to mutate
   * @param o the instruction's operator
   * @return the mutated instruction
   */
  public static OPT_Instruction mutate(OPT_Instruction i, OPT_Operator o
                )
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(o)) fail(o, "Empty");
    i.operator = o;
    return i;
  }
}

