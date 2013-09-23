
/*
 * THIS FILE IS MACHINE_GENERATED. DO NOT EDIT.
 * See InstructionFormats.template, CommonOperands.dat,
 * OperatorList.dat, etc.
 */

package org.jikesrvm.compilers.opt.ir;

import org.jikesrvm.VM_Configuration;

/**
 * InstructionFormats that have a Result (which is OPT_RegisterOperand)
 */
public final class ResultCarrier extends OPT_InstructionFormat {
  private static final int[] _index = {
           -1    // Unassigned
           , 0    // Move
           , -1    // Return
           , -1    // Prologue
           , -1    // InstrumentedCounter
           , -1    // Empty
           , 0    // Nullary
           , 0    // New
           , 0    // NewArray
           , 0    // Multianewarray
           , -1    // Athrow
           , -1    // MonitorOp
           , -1    // CacheOp
           , -1    // NullCheck
           , -1    // ZeroCheck
           , -1    // BoundsCheck
           , -1    // StoreCheck
           , -1    // TypeCheck
           , 0    // InstanceOf
           , -1    // Trap
           , -1    // TrapIf
           , -1    // IfCmp
           , -1    // IfCmp2
           , -1    // InlineGuard
           , 0    // BooleanCmp
           , 0    // CondMove
           , -1    // Goto
           , -1    // Label
           , -1    // BBend
           , 0    // Unary
           , 0    // GuardedUnary
           , 0    // Binary
           , 0    // GuardedBinary
           , -1    // GuardedSet
           , 0    // ALoad
           , 0    // GetField
           , 0    // GetStatic
           , 0    // Load
           , -1    // AStore
           , -1    // PutField
           , -1    // PutStatic
           , -1    // Store
           , 0    // Prepare
           , 0    // Attempt
           , 0    // Call
           , -1    // TableSwitch
           , -1    // LookupSwitch
           , -1    // LowTableSwitch
           , -1    // Phi
           , -1    // OsrBarrier
           , -1    // OsrPoint
           , -1    // MIR_LowTableSwitch
           , -1    // MIR_Move
           , -1    // MIR_CondMove
           , 0    // MIR_Lea
           , -1    // MIR_BinaryAcc
           , -1    // MIR_Divide
           , -1    // MIR_Multiply
           , -1    // MIR_ConvertDW2QW
           , -1    // MIR_UnaryAcc
           , -1    // MIR_Compare
           , -1    // MIR_CompareExchange
           , -1    // MIR_CompareExchange8B
           , -1    // MIR_Trap
           , -1    // MIR_TrapIf
           , -1    // MIR_Branch
           , -1    // MIR_CondBranch
           , -1    // MIR_CondBranch2
           , 0    // MIR_Call
           , -1    // MIR_Empty
           , -1    // MIR_Return
           , -1    // MIR_Set
           , -1    // MIR_Test
           , -1    // MIR_Nullary
           , -1    // MIR_UnaryNoRes
           , -1    // MIR_Unary
           , -1    // MIR_XChng
           , -1    // MIR_DoubleShift
           , -1    // MIR_CaseLabel
           , -1    // MIR_FSave
           , -1    // MIR_RDTSC
           , -1    // MIR_CacheOp
        };

  /**
   * Does the instruction belong to an instruction format that
   * has an operand called Result?
   * @param i the instruction to test
   * @return <code>true</code> if the instruction's instruction
   *         format has an operand called Result and
   *         <code>false</code> if it does not.
   */
  public static boolean conforms(OPT_Instruction i) {
    return conforms(i.operator);
  }
  /**
   * Does the operator belong to an instruction format that
   * has an operand called Result?
   * @param o the operator to test
   * @return <code>true</code> if the instruction's instruction
   *         format has an operand called Result and
   *         <code>false</code> if it does not.
   */
  public static boolean conforms(OPT_Operator o) {
    return _index[o.format] != -1;
  }

  /**
   * Get the operand called Result from the
   * argument instruction. Note that the returned operand
   * will still point to its containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Result
   */
  public static OPT_RegisterOperand getResult(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "ResultCarrier");
    int index = _index[i.operator.format];
    return (OPT_RegisterOperand) i.getOperand(index);
  }
  /**
   * Get the operand called Result from the argument
   * instruction clearing its instruction pointer. The returned
   * operand will not point to any containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Result
   */
  public static OPT_RegisterOperand getClearResult(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "ResultCarrier");
    int index = _index[i.operator.format];
    return (OPT_RegisterOperand) i.getClearOperand(index);
  }
  /**
   * Set the operand called Result in the argument
   * instruction to the argument operand. The operand will
   * now point to the argument instruction as its containing
   * instruction.
   * @param i the instruction in which to store the operand
   * @param op the operand to store
   */
  public static void setResult(OPT_Instruction i, OPT_RegisterOperand op) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "ResultCarrier");
    int index = _index[i.operator.format];
    i.putOperand(index, op);
  }
  /**
   * Return the index of the operand called Result
   * in the argument instruction.
   * @param i the instruction to access.
   * @return the index of the operand called Result
   *         in the argument instruction
   */
  public static int indexOfResult(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "ResultCarrier");
    return _index[i.operator.format];
  }
  /**
   * Does the argument instruction have a non-null
   * operand named Result?
   * @param i the instruction to access.
   * @return <code>true</code> if the instruction has an non-null
   *         operand named Result or <code>false</code>
   *         if it does not.
   */
  public static boolean hasResult(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "ResultCarrier");
    int index = _index[i.operator.format];
    return i.getOperand(index) != null;
  }
}

