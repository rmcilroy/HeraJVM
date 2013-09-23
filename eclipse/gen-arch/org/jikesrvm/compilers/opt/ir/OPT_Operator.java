/*
 *  This file is part of the Jikes RVM project (http://jikesrvm.org).
 *
 *  This file is licensed to You under the Common Public License (CPL);
 *  You may not use this file except in compliance with the License. You
 *  may obtain a copy of the License at
 *
 *      http://www.opensource.org/licenses/cpl1.0.php
 *
 *  See the COPYRIGHT.txt file distributed with this work for information
 *  regarding copyright ownership.
 */
/*
 * NOTE: OPT_Operator.java is mechanically generated from
 * OPT_Operator.template using the operator definitions in
 * OperatorList.dat and ia32OperatorList.dat
 *
 * DO NOT MANUALLY EDIT THE JAVA FILE.
 */

package org.jikesrvm.compilers.opt.ir;

import org.jikesrvm.*;
import org.jikesrvm.ArchitectureSpecific.OPT_PhysicalDefUse;
import org.jikesrvm.compilers.opt.*;

/**
 * An OPT_Operator represents the operator of an {@link OPT_Instruction}.
 * For each operator in the IR, we create exactly one OPT_Operator instance
 * to represent it. These instances are all stored in static fields
 * of {@link OPT_Operators}. Since only one instance is created for each
 * semantic operator, they can be compared using <code>==</code>.
 * <p>
 * A common coding practive is to implement the {@link OPT_Operators}
 * interface to be able to reference the IR operators within a class
 * without having to prepend 'OPT_Operators.' everywhere.
 *
 * @see OPT_Operators
 * @see OPT_Instruction
 * @see OPT_OperatorNames
 */
public final class OPT_Operator {

  /**
   * The operators opcode.
   * This value serves as a unique id suitable for use in switches
   */
  public final char opcode;

  /**
   * Encoding of the operator's InstructionFormat.
   * This field is only meant to be directly referenced
   * from the mechanically generated InstructionFormat
   * classes defined in the instructionFormats package.
   * {@link OPT_Instruction} contains an explanation
   * of the role of InstructionFormats in the IR.
   */
  public final byte format;

  /**
   * encoding of operator traits (characteristics)
   */
  private final int traits;

  /**
   * How many operands of the operator are (pure) defs?
   */
  private final int numberDefs;

  /**
   * How many operands of the operator are both defs and uses?
   * Only non-zero on IA32, 390.
   */
  private final int numberDefUses;

  /**
   * How many operands of the operator are pure uses?
   * Only contains a valid value for non-variableLength operators
   */
  private final int numberUses;

  /**
   * Physical registers that are implicitly defined by the operator.
   */
  public final int implicitDefs;

  /**
   * Physical registers that are implicitly used by the operator.
   */
  public final int implicitUses;


  /**
   * Operator Class of the operator; used for instruction scheduling.
   */
  OPT_OperatorClass opClass;

  /**
   * Sets the operator class.
   *
   * @param opClass operator class
   */
  public void setOpClass(OPT_OperatorClass opClass) {
    this.opClass = opClass;
  }

  /**
   * Gets the operator class.
   *
   * @return operator class
   */
  public OPT_OperatorClass getOpClass() {
    return opClass;
  }


  /**
   * Returns the string representation of this operator.
   *
   * @return the name of the operator
   */
  public String toString() {
    return OPT_OperatorNames.toString(this);
  }

  /**
   * Returns the number of operands that are defs.
   * By convention, operands are ordered in instructions
   * such that all defs are first, followed by all
   * combined defs/uses, followed by all pure uses.
   *
   * @return number of operands that are pure defs
   */
  public int getNumberOfPureDefs() {
    if (VM.VerifyAssertions) VM._assert(!hasVarDefs());
    return numberDefs;
  }

  /**
   * Returns the number of operands that are pure defs
   * and are not in the variable-length part of the operand list.
   * By convention, operands are ordered in instructions
   * such that all defs are first, followed by all
   * combined defs/uses, followed by all pure uses.
   *
   * @return how many non-variable operands are pure defs
   */
  public int getNumberOfFixedPureDefs() {
    return numberDefs;
  }

  /**
   * Returns the number of operands that are pure uses
   * and are not in the variable-length part of the operand list.
   * By convention, operands are ordered in instructions
   * such that all defs are first, followed by all
   * combined defs/uses, followed by all pure uses.
   *
   * @return how many non-variable operands are pure uses
   */
  public int getNumberOfFixedPureUses() {
    return numberUses;
  }

  /**
   * Returns the number of operands that are defs
   * and uses.
   * By convention, operands are ordered in instructions
   * such that all defs are first, followed by all
   * combined defs/uses, followed by all pure uses.
   *
   * @return number of operands that are combined defs and uses
   */
  public int getNumberOfDefUses() {
    return numberDefUses;
  }

  /**
   * Returns the number of operands that are pure uses.
   * By convention, operands are ordered in instructions
   * such that all defs are first, followed by all
   * combined defs/uses, followed by all pure uses.
   *
   * @return number of operands that are pure uses
   */
  public int getNumberOfPureUses() {
    return numberUses;
  }

  /**
   * Returns the number of operands that are defs
   * (either pure defs or combined def/uses).
   * By convention, operands are ordered in instructions
   * such that all defs are first, followed by all
   * combined defs/uses, followed by all pure uses.
   *
   * @return number of operands that are defs
   */
  public int getNumberOfDefs() {
    if (VM.VerifyAssertions) VM._assert(!hasVarDefs());
    return numberDefs + numberDefUses;
  }

  /**
   * Returns the number of operands that are uses
   * (either combined def/uses or pure uses).
   * By convention, operands are ordered in instructions
   * such that all defs are first, followed by all
   * combined defs/uses, followed by all pure uses.
   *
   * @return how many operands are uses
   */
  public int getNumberOfUses() {
    if (VM.VerifyAssertions) VM._assert(!hasVarUses());
    return numberDefUses + numberUses;
  }

  /**
   * Returns the number of operands that are pure uses
   * and are not in the variable-length part of the operand list.
   * By convention, operands are ordered in instructions
   * such that all defs are first, followed by all
   * combined defs/uses, followed by all pure uses.
   *
   * @return how many non-variable operands are pure uses
   */
  public int getNumberOfPureFixedUses() {
    return numberUses;
  }

  /**
   * Returns the number of operands that are uses
   * (either combined use/defs or pure uses)
   * and are not in the variable-length part of the operand list.
   * By convention, operands are ordered in instructions
   * such that all defs are first, followed by all
   * combined defs/uses, followed by all pure uses.
   *
   * @return number of non-variable operands are uses
   */
  public int getNumberOfFixedUses() {
    return numberDefUses + numberUses;
  }

  /**
   * Returns the number of physical registers that are
   * implicitly defined by this operator.
   *
   * @return number of implicit defs
   */
  public int getNumberOfImplicitDefs() {
    return OPT_Bits.populationCount(implicitDefs);
  }

  /**
   * Returns the number of physical registers that are
   * implicitly used by this operator.
   *
   * @return number of implicit uses
   */
  public int getNumberOfImplicitUses() {
    return OPT_Bits.populationCount(implicitUses);
  }

  /*
   * The following are used to encode operator traits in OperatorList.dat.
   * Had to make a few of them public (yuck) to let us get at them
   * from OPT_InstructionFormat.java.
   */
  // operator has no interesting traits
  public static final int none         = 0x00000000;
  // operator is a simple move operation from one "register" to another
  private static final int move         = 0x00000001;
  // operator is an intraprocedural branch of some form
  private static final int branch       = 0x00000002;
  // operator is some kind of call (interprocedural branch)
  private static final int call         = 0x00000004;
  // modifer for branches/calls
  private static final int conditional  = 0x00000008;
  // modifier for branches/calls, mostly on MIR
  private static final int indirect     = 0x00000010;
  // an explicit load of a value from memory
  private static final int load         = 0x00000020;
  // operator is modeled as a load by memory system, mostly on MIR
  private static final int memAsLoad    = 0x00000040;
  // an explicit store of a value to memory
  private static final int store        = 0x00000080;
  // operator is modeled as a store by memory system, mostly on MIR
  private static final int memAsStore   = 0x00000100;
  // is an exception throw
  private static final int ethrow       = 0x00000200;
  // an immediate PEI (null_check, int_zero_check, but _not_ call);
  private static final int immedPEI     = 0x00000400;
  // operator is some kind of compare (val,val)-> cond
  private static final int compare      = 0x00000800;
  // an explicit memory allocation
  private static final int alloc        = 0x00001000;
  // a return instruction (interprocedural branch)
  private static final int ret          = 0x00002000;
  // operator has a variable number of uses
  public static final int varUses      = 0x00004000;
  // operator has a variable number of defs
  public static final int varDefs      = 0x00008000;
  // operator is a potential thread switch point for some reason
  // other than being a call/immedPEI
  private static final int tsp          = 0x00010000;
  // operator is an acquire (monitorenter/lock) HIR only
  private static final int acquire      = 0x00020000;
  // operator is a relase (monitorexit/unlock) HIR only
  private static final int release      = 0x00040000;
  // operator either directly or indirectly may casue dynamic linking
  private static final int dynLink      = 0x00080000;
  // operator is a yield point
  private static final int yieldPoint   = 0x00100000;
  // operator pops floating-point stack after performing defs
  private static final int fpPop        = 0x00200000;
  // operator pushs floating-point stack before performing defs
  private static final int fpPush       = 0x00400000;
  // operator is commutative
  private static final int commutative  = 0x00800000;

  /**
   * Does the operator represent a simple move (the value is unchanged)
   * from one "register" location to another "register" location?
   *
   * @return <code>true</code> if the operator is a simple move
   *         or <code>false</code> if it is not.
   */
  public boolean isMove() {
    return (traits & move) != 0;
  }

  /**
   * Is the operator an intraprocedural branch?
   *
   * @return <code>true</code> if the operator is am
   *         intraprocedural branch or <code>false</code> if it is not.
   */
  public boolean isBranch() {
    return (traits & branch) != 0;
  }

  /**
   * Is the operator a conditional intraprocedural branch?
   *
   * @return <code>true</code> if the operator is a conditoonal
   *         intraprocedural branch or <code>false</code> if it is not.
   */
  public boolean isConditionalBranch() {
    return (traits & (branch|conditional)) == (branch|conditional);
  }

  /**
   * Is the operator an unconditional intraprocedural branch?
   * We consider various forms of switches to be unconditional
   * intraprocedural branches, even though they are multi-way branches
   * and we may not no exactly which target will be taken.
   * This turns out to be the right thing to do, since some
   * arm of the switch will always be taken (unlike conditional branches).
   *
   * @return <code>true</code> if the operator is an unconditional
   *         intraprocedural branch or <code>false</code> if it is not.
   */
  public boolean isUnconditionalBranch() {
    return (traits & (branch|conditional)) == branch;
  }

  /**
   * Is the operator a direct intraprocedural branch?
   * In the HIR and LIR we consider switches to be direct branches,
   * because their targets are known precisely.
   *
   * @return <code>true</code> if the operator is a direct
   *         intraprocedural branch or <code>false</code> if it is not.
   */
  public boolean isDirectBranch() {
    return (traits & (branch|indirect)) == branch;
  }

  /**
   * Is the operator an indirect intraprocedural branch?
   *
   * @return <code>true</code> if the operator is an indirect
   *         interprocedural branch or <code>false</code> if it is not.
   */
  public boolean isIndirectBranch() {
    return (traits & (branch|indirect)) == (branch|indirect);
  }

  /**
   * Is the operator a call (one kind of interprocedural branch)?
   *
   * @return <code>true</code> if the operator is a call
   *         or <code>false</code> if it is not.
   */
  public boolean isCall() {
    return (traits & call) != 0;
  }

  /**
   * Is the operator a conditional call?
   * We only allow conditional calls in the MIR, since they
   * tend to only be directly implementable on some architecutres.
   *
   * @return <code>true</code> if the operator is a
   *         conditional call or <code>false</code> if it is not.
   */
  public boolean isConditionalCall() {
    return (traits & (call|conditional)) == (call|conditional);
  }

  /**
   * Is the operator an unconditional call?
   * Really only an interesting question in the MIR, since
   * it is by definition true for all HIR and LIR calls.
   *
   * @return <code>true</code> if the operator is an unconditional
   *         call or <code>false</code> if it is not.
   */
  public boolean isUnconditionalCall() {
    return (traits & (call|conditional)) == call;
  }

  /**
   * Is the operator a direct call?
   * Only interesting on the MIR.  In the HIR and LIR we pretend that
   * all calls are "direct" even though most of them aren't.
   *
   * @return <code>true</code> if the operator is a direct call
   *         or <code>false</code> if it is not.
   */
  public boolean isDirectCall() {
    return (traits & (call|indirect)) == call;
  }

  /**
   * Is the operator an indirect call?
   * Only interesting on the MIR.  In the HIR and LIR we pretend that
   * all calls are "direct" even though most of them aren't.
   *
   * @return <code>true</code> if the operator is an indirect call
   *         or <code>false</code> if it is not.
   */
  public boolean isIndirectCall() {
    return (traits & (call|indirect)) == (call|indirect);
  }

  /**
   * Is the operator an explicit load of a finite set of values from
   * a finite set of memory locations (load, load multiple, _not_ call)?
   *
   * @return <code>true</code> if the operator is an explicit load
   *         or <code>false</code> if it is not.
   */
  public boolean isExplicitLoad() {
    return (traits & load) != 0;
  }

  /**
   * Should the operator be treated as a load from some unknown location(s)
   * for the purposes of scheduling and/or modeling the memory subsystem?
   *
   * @return <code>true</code> if the operator is an implicit load
   *         or <code>false</code> if it is not.
   */
  public boolean isImplicitLoad() {
    return (traits & (load|memAsLoad|call)) != 0;
  }

  /**
   * Is the operator an explicit store of a finite set of values to
   * a finite set of memory locations (store, store multiple, _not_ call)?
   *
   * @return <code>true</code> if the operator is an explicit store
   *         or <code>false</code> if it is not.
   */
  public boolean isExplicitStore() {
    return (traits & store) != 0;
  }

  /**
   * Should the operator be treated as a store to some unknown location(s)
   * for the purposes of scheduling and/or modeling the memory subsystem?
   *
   * @return <code>true</code> if the operator is an implicit store
   *         or <code>false</code> if it is not.
   */
  public boolean isImplicitStore() {
    return (traits & (store|memAsStore|call)) != 0;
  }

  /**
   * Is the operator a throw of a Java exception?
   *
   * @return <code>true</code> if the operator is a throw
   *         or <code>false</code> if it is not.
   */
  public boolean isThrow() {
    return (traits & ethrow) != 0;
  }

  /**
   * Is the operator a PEI (Potentially Excepting Instruction)?
   *
   * @return <code>true</code> if the operator is a PEI
   *         or <code>false</code> if it is not.
   */
  public boolean isPEI() {
    return (traits & (ethrow|immedPEI)) != 0;
  }

  /**
   * Is the operator a potential GC point?
   *
   * @return <code>true</code> if the operator is a potential
   *         GC point or <code>false</code> if it is not.
   */
  public boolean isGCPoint() {
    return isPEI() || ((traits & (alloc|tsp)) != 0);
  }

  /**
   * is the operator a potential thread switch point?
   *
   * @return <code>true</code> if the operator is a potential
   *         threadswitch point or <code>false</code> if it is not.
   */
  public boolean isTSPoint() {
    return isGCPoint();
  }

  /**
   * Is the operator a compare (val,val) => condition?
   *
   * @return <code>true</code> if the operator is a compare
   *         or <code>false</code> if it is not.
   */
  public boolean isCompare() {
    return (traits & compare) != 0;
  }

  /**
   * Is the operator an actual memory allocation instruction
   * (NEW, NEWARRAY, etc)?
   *
   * @return <code>true</code> if the operator is an allocation
   *         or <code>false</code> if it is not.
   */
  public boolean isAllocation() {
    return (traits & alloc) != 0;
  }

  /**
   * Is the operator a return (interprocedural branch)?
   *
   * @return <code>true</code> if the operator is a return
   *         or <code>false</code> if it is not.
   */
  public boolean isReturn() {
    return (traits & ret) != 0;
  }

  /**
   * Can the operator have a variable number of uses?
   *
   * @return <code>true</code> if the operator has a variable number
   *         of uses or <code>false</code> if it does not.
   */
  public boolean hasVarUses() {
    return (traits & varUses) != 0;
  }

  /**
   * Can the operator have a variable number of uses?
   *
   * @return <code>true</code> if the operator has a variable number
   *         of uses or <code>false</code> if it does not.
   */
  public boolean hasVarDefs() {
    return (traits & varDefs) != 0;
  }

  /**
   * Can the operator have a variable number of uses or defs?
   *
   * @return <code>true</code> if the operator has a variable number
   *         of uses or defs or <code>false</code> if it does not.
   */
  public boolean hasVarUsesOrDefs() {
    return (traits & (varUses | varDefs)) != 0;
  }

  /**
   * Is the operator an acquire (monitorenter/lock)?
   *
   * @return <code>true</code> if the operator is an acquire
   *         or <code>false</code> if it is not.
   */
  public boolean isAcquire() {
    return (traits & acquire) != 0;
  }

  /**
   * Is the operator a release (monitorexit/unlock)?
   *
   * @return <code>true</code> if the operator is a release
   *         or <code>false</code> if it is not.
   */
  public boolean isRelease() {
    return (traits & release) != 0;
  }

  /**
   * Could the operator either directly or indirectly
   * cause dynamic class loading?
   *
   * @return <code>true</code> if the operator is a dynamic linking point
   *         or <code>false</code> if it is not.
   */
  public boolean isDynamicLinkingPoint() {
    return (traits & dynLink) != 0;
  }

  /**
   * Is the operator a yield point?
   *
   * @return <code>true</code> if the operator is a yield point
   *         or <code>false</code> if it is not.
   */
  public boolean isYieldPoint() {
    return (traits & yieldPoint) != 0;
  }

  /**
   * Does the operator pop the floating-point stack?
   *
   * @return <code>true</code> if the operator pops the floating-point
   * stack.
   *         or <code>false</code> if not.
   */
  public boolean isFpPop() {
    return (traits & fpPop) != 0;
  }

  /**
   * Does the operator push on the floating-point stack?
   *
   * @return <code>true</code> if the operator pushes on the floating-point
   * stack.
   *         or <code>false</code> if not.
   */
  public boolean isFpPush() {
    return (traits & fpPush) != 0;
  }

  /**
   * Is the operator commutative?
   *
   * @return <code>true</code> if the operator is commutative.
   *         or <code>false</code> if not.
   */
  public boolean isCommutative() {
    return (traits & commutative) != 0;
  }


  public static final OPT_Operator[] OperatorArray = {
     new OPT_Operator((char)0, OPT_InstructionFormat.Unary_format,  //GET_CLASS_OBJECT
                      (none | OPT_InstructionFormat.Unary_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)1, OPT_InstructionFormat.Nullary_format,  //GET_CAUGHT_EXCEPTION
                      (none | OPT_InstructionFormat.Nullary_traits),
                      1, 0, 0,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)2, OPT_InstructionFormat.CacheOp_format,  //SET_CAUGHT_EXCEPTION
                      (none | OPT_InstructionFormat.CacheOp_traits),
                      0, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)3, OPT_InstructionFormat.New_format,  //NEW
                      (alloc | immedPEI | OPT_InstructionFormat.New_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)4, OPT_InstructionFormat.New_format,  //NEW_UNRESOLVED
                      (alloc | immedPEI | dynLink | OPT_InstructionFormat.New_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)5, OPT_InstructionFormat.NewArray_format,  //NEWARRAY
                      (alloc | immedPEI | OPT_InstructionFormat.NewArray_traits),
                      1, 0, 2,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)6, OPT_InstructionFormat.NewArray_format,  //NEWARRAY_UNRESOLVED
                      (alloc | immedPEI | dynLink | OPT_InstructionFormat.NewArray_traits),
                      1, 0, 2,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)7, OPT_InstructionFormat.Athrow_format,  //ATHROW
                      (ethrow | OPT_InstructionFormat.Athrow_traits),
                      0, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)8, OPT_InstructionFormat.TypeCheck_format,  //CHECKCAST
                      (immedPEI | OPT_InstructionFormat.TypeCheck_traits),
                      0, 0, 3,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)9, OPT_InstructionFormat.TypeCheck_format,  //CHECKCAST_NOTNULL
                      (immedPEI | OPT_InstructionFormat.TypeCheck_traits),
                      0, 0, 3,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)10, OPT_InstructionFormat.TypeCheck_format,  //CHECKCAST_UNRESOLVED
                      (immedPEI | dynLink | OPT_InstructionFormat.TypeCheck_traits),
                      0, 0, 3,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)11, OPT_InstructionFormat.TypeCheck_format,  //MUST_IMPLEMENT_INTERFACE
                      (immedPEI | OPT_InstructionFormat.TypeCheck_traits),
                      0, 0, 3,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)12, OPT_InstructionFormat.InstanceOf_format,  //INSTANCEOF
                      (none | OPT_InstructionFormat.InstanceOf_traits),
                      1, 0, 3,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)13, OPT_InstructionFormat.InstanceOf_format,  //INSTANCEOF_NOTNULL
                      (none | OPT_InstructionFormat.InstanceOf_traits),
                      1, 0, 3,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)14, OPT_InstructionFormat.InstanceOf_format,  //INSTANCEOF_UNRESOLVED
                      (immedPEI | dynLink | OPT_InstructionFormat.InstanceOf_traits),
                      1, 0, 3,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)15, OPT_InstructionFormat.MonitorOp_format,  //MONITORENTER
                      (memAsLoad | memAsStore | acquire | tsp | OPT_InstructionFormat.MonitorOp_traits),
                      0, 0, 2,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)16, OPT_InstructionFormat.MonitorOp_format,  //MONITOREXIT
                      (memAsLoad | memAsStore | release | tsp | immedPEI | OPT_InstructionFormat.MonitorOp_traits),
                      0, 0, 2,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)17, OPT_InstructionFormat.NewArray_format,  //NEWOBJMULTIARRAY
                      (alloc | immedPEI | dynLink | OPT_InstructionFormat.NewArray_traits),
                      1, 0, 2,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)18, OPT_InstructionFormat.GetStatic_format,  //GETSTATIC
                      (load | OPT_InstructionFormat.GetStatic_traits),
                      1, 0, 2,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)19, OPT_InstructionFormat.PutStatic_format,  //PUTSTATIC
                      (store | OPT_InstructionFormat.PutStatic_traits),
                      0, 0, 3,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)20, OPT_InstructionFormat.GetField_format,  //GETFIELD
                      (load | OPT_InstructionFormat.GetField_traits),
                      1, 0, 4,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)21, OPT_InstructionFormat.PutField_format,  //PUTFIELD
                      (store | OPT_InstructionFormat.PutField_traits),
                      0, 0, 5,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)22, OPT_InstructionFormat.ZeroCheck_format,  //INT_ZERO_CHECK
                      (immedPEI | OPT_InstructionFormat.ZeroCheck_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)23, OPT_InstructionFormat.ZeroCheck_format,  //LONG_ZERO_CHECK
                      (immedPEI | OPT_InstructionFormat.ZeroCheck_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)24, OPT_InstructionFormat.BoundsCheck_format,  //BOUNDS_CHECK
                      (immedPEI | OPT_InstructionFormat.BoundsCheck_traits),
                      1, 0, 3,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)25, OPT_InstructionFormat.StoreCheck_format,  //OBJARRAY_STORE_CHECK
                      (immedPEI | OPT_InstructionFormat.StoreCheck_traits),
                      1, 0, 3,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)26, OPT_InstructionFormat.StoreCheck_format,  //OBJARRAY_STORE_CHECK_NOTNULL
                      (immedPEI | OPT_InstructionFormat.StoreCheck_traits),
                      1, 0, 3,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)27, OPT_InstructionFormat.InlineGuard_format,  //IG_PATCH_POINT
                      (branch | conditional | OPT_InstructionFormat.InlineGuard_traits),
                      0, 0, 5,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)28, OPT_InstructionFormat.InlineGuard_format,  //IG_CLASS_TEST
                      (branch | conditional | OPT_InstructionFormat.InlineGuard_traits),
                      0, 0, 5,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)29, OPT_InstructionFormat.InlineGuard_format,  //IG_METHOD_TEST
                      (branch | conditional | OPT_InstructionFormat.InlineGuard_traits),
                      0, 0, 5,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)30, OPT_InstructionFormat.TableSwitch_format,  //TABLESWITCH
                      (branch | OPT_InstructionFormat.TableSwitch_traits),
                      0, 0, 7,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)31, OPT_InstructionFormat.LookupSwitch_format,  //LOOKUPSWITCH
                      (branch | OPT_InstructionFormat.LookupSwitch_traits),
                      0, 0, 5,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)32, OPT_InstructionFormat.ALoad_format,  //INT_ALOAD
                      (load | OPT_InstructionFormat.ALoad_traits),
                      1, 0, 4,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)33, OPT_InstructionFormat.ALoad_format,  //LONG_ALOAD
                      (load | OPT_InstructionFormat.ALoad_traits),
                      1, 0, 4,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)34, OPT_InstructionFormat.ALoad_format,  //FLOAT_ALOAD
                      (load | OPT_InstructionFormat.ALoad_traits),
                      1, 0, 4,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)35, OPT_InstructionFormat.ALoad_format,  //DOUBLE_ALOAD
                      (load | OPT_InstructionFormat.ALoad_traits),
                      1, 0, 4,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)36, OPT_InstructionFormat.ALoad_format,  //REF_ALOAD
                      (load | OPT_InstructionFormat.ALoad_traits),
                      1, 0, 4,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)37, OPT_InstructionFormat.ALoad_format,  //UBYTE_ALOAD
                      (load | OPT_InstructionFormat.ALoad_traits),
                      1, 0, 4,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)38, OPT_InstructionFormat.ALoad_format,  //BYTE_ALOAD
                      (load | OPT_InstructionFormat.ALoad_traits),
                      1, 0, 4,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)39, OPT_InstructionFormat.ALoad_format,  //USHORT_ALOAD
                      (load | OPT_InstructionFormat.ALoad_traits),
                      1, 0, 4,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)40, OPT_InstructionFormat.ALoad_format,  //SHORT_ALOAD
                      (load | OPT_InstructionFormat.ALoad_traits),
                      1, 0, 4,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)41, OPT_InstructionFormat.AStore_format,  //INT_ASTORE
                      (store | OPT_InstructionFormat.AStore_traits),
                      0, 0, 5,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)42, OPT_InstructionFormat.AStore_format,  //LONG_ASTORE
                      (store | OPT_InstructionFormat.AStore_traits),
                      0, 0, 5,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)43, OPT_InstructionFormat.AStore_format,  //FLOAT_ASTORE
                      (store | OPT_InstructionFormat.AStore_traits),
                      0, 0, 5,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)44, OPT_InstructionFormat.AStore_format,  //DOUBLE_ASTORE
                      (store | OPT_InstructionFormat.AStore_traits),
                      0, 0, 5,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)45, OPT_InstructionFormat.AStore_format,  //REF_ASTORE
                      (store | OPT_InstructionFormat.AStore_traits),
                      0, 0, 5,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)46, OPT_InstructionFormat.AStore_format,  //BYTE_ASTORE
                      (store | OPT_InstructionFormat.AStore_traits),
                      0, 0, 5,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)47, OPT_InstructionFormat.AStore_format,  //SHORT_ASTORE
                      (store | OPT_InstructionFormat.AStore_traits),
                      0, 0, 5,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)48, OPT_InstructionFormat.IfCmp_format,  //INT_IFCMP
                      (branch | conditional | OPT_InstructionFormat.IfCmp_traits),
                      1, 0, 5,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)49, OPT_InstructionFormat.IfCmp2_format,  //INT_IFCMP2
                      (branch | conditional | OPT_InstructionFormat.IfCmp2_traits),
                      1, 0, 8,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)50, OPT_InstructionFormat.IfCmp_format,  //LONG_IFCMP
                      (branch | conditional | OPT_InstructionFormat.IfCmp_traits),
                      1, 0, 5,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)51, OPT_InstructionFormat.IfCmp_format,  //FLOAT_IFCMP
                      (branch | conditional | OPT_InstructionFormat.IfCmp_traits),
                      1, 0, 5,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)52, OPT_InstructionFormat.IfCmp_format,  //DOUBLE_IFCMP
                      (branch | conditional | OPT_InstructionFormat.IfCmp_traits),
                      1, 0, 5,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)53, OPT_InstructionFormat.IfCmp_format,  //REF_IFCMP
                      (branch | conditional | OPT_InstructionFormat.IfCmp_traits),
                      1, 0, 5,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)54, OPT_InstructionFormat.Label_format,  //LABEL
                      (none | OPT_InstructionFormat.Label_traits),
                      0, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)55, OPT_InstructionFormat.BBend_format,  //BBEND
                      (none | OPT_InstructionFormat.BBend_traits),
                      0, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)56, OPT_InstructionFormat.Empty_format,  //UNINT_BEGIN
                      (none | OPT_InstructionFormat.Empty_traits),
                      0, 0, 0,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)57, OPT_InstructionFormat.Empty_format,  //UNINT_END
                      (none | OPT_InstructionFormat.Empty_traits),
                      0, 0, 0,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)58, OPT_InstructionFormat.Empty_format,  //READ_CEILING
                      (memAsLoad | memAsStore | acquire | OPT_InstructionFormat.Empty_traits),
                      0, 0, 0,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)59, OPT_InstructionFormat.Empty_format,  //WRITE_FLOOR
                      (memAsLoad | memAsStore | release | OPT_InstructionFormat.Empty_traits),
                      0, 0, 0,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)60, OPT_InstructionFormat.Phi_format,  //PHI
                      (none | OPT_InstructionFormat.Phi_traits),
                      1, 0, 0,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)61, OPT_InstructionFormat.Unary_format,  //SPLIT
                      (none | OPT_InstructionFormat.Unary_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)62, OPT_InstructionFormat.GuardedUnary_format,  //PI
                      (none | OPT_InstructionFormat.GuardedUnary_traits),
                      1, 0, 2,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)63, OPT_InstructionFormat.Empty_format,  //NOP
                      (none | OPT_InstructionFormat.Empty_traits),
                      0, 0, 0,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)64, OPT_InstructionFormat.Move_format,  //INT_MOVE
                      (move | OPT_InstructionFormat.Move_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)65, OPT_InstructionFormat.Move_format,  //LONG_MOVE
                      (move | OPT_InstructionFormat.Move_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)66, OPT_InstructionFormat.Move_format,  //FLOAT_MOVE
                      (move | OPT_InstructionFormat.Move_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)67, OPT_InstructionFormat.Move_format,  //DOUBLE_MOVE
                      (move | OPT_InstructionFormat.Move_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)68, OPT_InstructionFormat.Move_format,  //REF_MOVE
                      (move | OPT_InstructionFormat.Move_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)69, OPT_InstructionFormat.Move_format,  //GUARD_MOVE
                      (move | OPT_InstructionFormat.Move_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)70, OPT_InstructionFormat.CondMove_format,  //INT_COND_MOVE
                      (compare | OPT_InstructionFormat.CondMove_traits),
                      1, 0, 5,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)71, OPT_InstructionFormat.CondMove_format,  //LONG_COND_MOVE
                      (compare | OPT_InstructionFormat.CondMove_traits),
                      1, 0, 5,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)72, OPT_InstructionFormat.CondMove_format,  //FLOAT_COND_MOVE
                      (compare | OPT_InstructionFormat.CondMove_traits),
                      1, 0, 5,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)73, OPT_InstructionFormat.CondMove_format,  //DOUBLE_COND_MOVE
                      (compare | OPT_InstructionFormat.CondMove_traits),
                      1, 0, 5,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)74, OPT_InstructionFormat.CondMove_format,  //REF_COND_MOVE
                      (compare | OPT_InstructionFormat.CondMove_traits),
                      1, 0, 5,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)75, OPT_InstructionFormat.CondMove_format,  //GUARD_COND_MOVE
                      (compare | OPT_InstructionFormat.CondMove_traits),
                      1, 0, 5,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)76, OPT_InstructionFormat.Binary_format,  //GUARD_COMBINE
                      (none | OPT_InstructionFormat.Binary_traits),
                      1, 0, 2,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)77, OPT_InstructionFormat.Binary_format,  //REF_ADD
                      (commutative | OPT_InstructionFormat.Binary_traits),
                      1, 0, 2,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)78, OPT_InstructionFormat.Binary_format,  //INT_ADD
                      (commutative | OPT_InstructionFormat.Binary_traits),
                      1, 0, 2,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)79, OPT_InstructionFormat.Binary_format,  //LONG_ADD
                      (commutative | OPT_InstructionFormat.Binary_traits),
                      1, 0, 2,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)80, OPT_InstructionFormat.Binary_format,  //FLOAT_ADD
                      (commutative | OPT_InstructionFormat.Binary_traits),
                      1, 0, 2,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)81, OPT_InstructionFormat.Binary_format,  //DOUBLE_ADD
                      (commutative | OPT_InstructionFormat.Binary_traits),
                      1, 0, 2,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)82, OPT_InstructionFormat.Binary_format,  //REF_SUB
                      (none | OPT_InstructionFormat.Binary_traits),
                      1, 0, 2,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)83, OPT_InstructionFormat.Binary_format,  //INT_SUB
                      (none | OPT_InstructionFormat.Binary_traits),
                      1, 0, 2,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)84, OPT_InstructionFormat.Binary_format,  //LONG_SUB
                      (none | OPT_InstructionFormat.Binary_traits),
                      1, 0, 2,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)85, OPT_InstructionFormat.Binary_format,  //FLOAT_SUB
                      (none | OPT_InstructionFormat.Binary_traits),
                      1, 0, 2,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)86, OPT_InstructionFormat.Binary_format,  //DOUBLE_SUB
                      (none | OPT_InstructionFormat.Binary_traits),
                      1, 0, 2,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)87, OPT_InstructionFormat.Binary_format,  //INT_MUL
                      (commutative | OPT_InstructionFormat.Binary_traits),
                      1, 0, 2,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)88, OPT_InstructionFormat.Binary_format,  //LONG_MUL
                      (commutative | OPT_InstructionFormat.Binary_traits),
                      1, 0, 2,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)89, OPT_InstructionFormat.Binary_format,  //FLOAT_MUL
                      (commutative | OPT_InstructionFormat.Binary_traits),
                      1, 0, 2,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)90, OPT_InstructionFormat.Binary_format,  //DOUBLE_MUL
                      (commutative | OPT_InstructionFormat.Binary_traits),
                      1, 0, 2,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)91, OPT_InstructionFormat.GuardedBinary_format,  //INT_DIV
                      (none | OPT_InstructionFormat.GuardedBinary_traits),
                      1, 0, 3,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)92, OPT_InstructionFormat.GuardedBinary_format,  //LONG_DIV
                      (none | OPT_InstructionFormat.GuardedBinary_traits),
                      1, 0, 3,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)93, OPT_InstructionFormat.Binary_format,  //FLOAT_DIV
                      (none | OPT_InstructionFormat.Binary_traits),
                      1, 0, 2,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)94, OPT_InstructionFormat.Binary_format,  //DOUBLE_DIV
                      (none | OPT_InstructionFormat.Binary_traits),
                      1, 0, 2,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)95, OPT_InstructionFormat.GuardedBinary_format,  //INT_REM
                      (none | OPT_InstructionFormat.GuardedBinary_traits),
                      1, 0, 3,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)96, OPT_InstructionFormat.GuardedBinary_format,  //LONG_REM
                      (none | OPT_InstructionFormat.GuardedBinary_traits),
                      1, 0, 3,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)97, OPT_InstructionFormat.Binary_format,  //FLOAT_REM
                      (none | OPT_InstructionFormat.Binary_traits),
                      1, 0, 2,
                      OPT_PhysicalDefUse.maskIEEEMagicUses,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)98, OPT_InstructionFormat.Binary_format,  //DOUBLE_REM
                      (none | OPT_InstructionFormat.Binary_traits),
                      1, 0, 2,
                      OPT_PhysicalDefUse.maskIEEEMagicUses,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)99, OPT_InstructionFormat.Unary_format,  //REF_NEG
                      (none | OPT_InstructionFormat.Unary_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)100, OPT_InstructionFormat.Unary_format,  //INT_NEG
                      (none | OPT_InstructionFormat.Unary_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)101, OPT_InstructionFormat.Unary_format,  //LONG_NEG
                      (none | OPT_InstructionFormat.Unary_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)102, OPT_InstructionFormat.Unary_format,  //FLOAT_NEG
                      (none | OPT_InstructionFormat.Unary_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)103, OPT_InstructionFormat.Unary_format,  //DOUBLE_NEG
                      (none | OPT_InstructionFormat.Unary_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)104, OPT_InstructionFormat.Binary_format,  //REF_SHL
                      (none | OPT_InstructionFormat.Binary_traits),
                      1, 0, 2,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)105, OPT_InstructionFormat.Binary_format,  //INT_SHL
                      (none | OPT_InstructionFormat.Binary_traits),
                      1, 0, 2,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)106, OPT_InstructionFormat.Binary_format,  //LONG_SHL
                      (none | OPT_InstructionFormat.Binary_traits),
                      1, 0, 2,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)107, OPT_InstructionFormat.Binary_format,  //REF_SHR
                      (none | OPT_InstructionFormat.Binary_traits),
                      1, 0, 2,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)108, OPT_InstructionFormat.Binary_format,  //INT_SHR
                      (none | OPT_InstructionFormat.Binary_traits),
                      1, 0, 2,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)109, OPT_InstructionFormat.Binary_format,  //LONG_SHR
                      (none | OPT_InstructionFormat.Binary_traits),
                      1, 0, 2,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)110, OPT_InstructionFormat.Binary_format,  //REF_USHR
                      (none | OPT_InstructionFormat.Binary_traits),
                      1, 0, 2,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)111, OPT_InstructionFormat.Binary_format,  //INT_USHR
                      (none | OPT_InstructionFormat.Binary_traits),
                      1, 0, 2,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)112, OPT_InstructionFormat.Binary_format,  //LONG_USHR
                      (none | OPT_InstructionFormat.Binary_traits),
                      1, 0, 2,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)113, OPT_InstructionFormat.Binary_format,  //REF_AND
                      (commutative | OPT_InstructionFormat.Binary_traits),
                      1, 0, 2,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)114, OPT_InstructionFormat.Binary_format,  //INT_AND
                      (commutative | OPT_InstructionFormat.Binary_traits),
                      1, 0, 2,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)115, OPT_InstructionFormat.Binary_format,  //LONG_AND
                      (commutative | OPT_InstructionFormat.Binary_traits),
                      1, 0, 2,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)116, OPT_InstructionFormat.Binary_format,  //REF_OR
                      (commutative | OPT_InstructionFormat.Binary_traits),
                      1, 0, 2,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)117, OPT_InstructionFormat.Binary_format,  //INT_OR
                      (commutative | OPT_InstructionFormat.Binary_traits),
                      1, 0, 2,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)118, OPT_InstructionFormat.Binary_format,  //LONG_OR
                      (commutative | OPT_InstructionFormat.Binary_traits),
                      1, 0, 2,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)119, OPT_InstructionFormat.Binary_format,  //REF_XOR
                      (commutative | OPT_InstructionFormat.Binary_traits),
                      1, 0, 2,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)120, OPT_InstructionFormat.Binary_format,  //INT_XOR
                      (commutative | OPT_InstructionFormat.Binary_traits),
                      1, 0, 2,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)121, OPT_InstructionFormat.Unary_format,  //REF_NOT
                      (none | OPT_InstructionFormat.Unary_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)122, OPT_InstructionFormat.Unary_format,  //INT_NOT
                      (none | OPT_InstructionFormat.Unary_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)123, OPT_InstructionFormat.Unary_format,  //LONG_NOT
                      (none | OPT_InstructionFormat.Unary_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)124, OPT_InstructionFormat.Binary_format,  //LONG_XOR
                      (commutative | OPT_InstructionFormat.Binary_traits),
                      1, 0, 2,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)125, OPT_InstructionFormat.Unary_format,  //INT_2ADDRSigExt
                      (none | OPT_InstructionFormat.Unary_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)126, OPT_InstructionFormat.Unary_format,  //INT_2ADDRZerExt
                      (none | OPT_InstructionFormat.Unary_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)127, OPT_InstructionFormat.Unary_format,  //LONG_2ADDR
                      (none | OPT_InstructionFormat.Unary_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)128, OPT_InstructionFormat.Unary_format,  //ADDR_2INT
                      (none | OPT_InstructionFormat.Unary_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)129, OPT_InstructionFormat.Unary_format,  //ADDR_2LONG
                      (none | OPT_InstructionFormat.Unary_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)130, OPT_InstructionFormat.Unary_format,  //INT_2LONG
                      (none | OPT_InstructionFormat.Unary_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)131, OPT_InstructionFormat.Unary_format,  //INT_2FLOAT
                      (none | OPT_InstructionFormat.Unary_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.maskIEEEMagicUses,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)132, OPT_InstructionFormat.Unary_format,  //INT_2DOUBLE
                      (none | OPT_InstructionFormat.Unary_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.maskIEEEMagicUses,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)133, OPT_InstructionFormat.Unary_format,  //LONG_2INT
                      (none | OPT_InstructionFormat.Unary_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)134, OPT_InstructionFormat.Unary_format,  //LONG_2FLOAT
                      (none | OPT_InstructionFormat.Unary_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)135, OPT_InstructionFormat.Unary_format,  //LONG_2DOUBLE
                      (none | OPT_InstructionFormat.Unary_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)136, OPT_InstructionFormat.Unary_format,  //FLOAT_2INT
                      (none | OPT_InstructionFormat.Unary_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)137, OPT_InstructionFormat.Unary_format,  //FLOAT_2LONG
                      (none | OPT_InstructionFormat.Unary_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)138, OPT_InstructionFormat.Unary_format,  //FLOAT_2DOUBLE
                      (none | OPT_InstructionFormat.Unary_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)139, OPT_InstructionFormat.Unary_format,  //DOUBLE_2INT
                      (none | OPT_InstructionFormat.Unary_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)140, OPT_InstructionFormat.Unary_format,  //DOUBLE_2LONG
                      (none | OPT_InstructionFormat.Unary_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)141, OPT_InstructionFormat.Unary_format,  //DOUBLE_2FLOAT
                      (none | OPT_InstructionFormat.Unary_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)142, OPT_InstructionFormat.Unary_format,  //INT_2BYTE
                      (none | OPT_InstructionFormat.Unary_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)143, OPT_InstructionFormat.Unary_format,  //INT_2USHORT
                      (none | OPT_InstructionFormat.Unary_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)144, OPT_InstructionFormat.Unary_format,  //INT_2SHORT
                      (none | OPT_InstructionFormat.Unary_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)145, OPT_InstructionFormat.Binary_format,  //LONG_CMP
                      (compare | OPT_InstructionFormat.Binary_traits),
                      1, 0, 2,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)146, OPT_InstructionFormat.Binary_format,  //FLOAT_CMPL
                      (compare | OPT_InstructionFormat.Binary_traits),
                      1, 0, 2,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)147, OPT_InstructionFormat.Binary_format,  //FLOAT_CMPG
                      (compare | OPT_InstructionFormat.Binary_traits),
                      1, 0, 2,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)148, OPT_InstructionFormat.Binary_format,  //DOUBLE_CMPL
                      (compare | OPT_InstructionFormat.Binary_traits),
                      1, 0, 2,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)149, OPT_InstructionFormat.Binary_format,  //DOUBLE_CMPG
                      (compare | OPT_InstructionFormat.Binary_traits),
                      1, 0, 2,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)150, OPT_InstructionFormat.Return_format,  //RETURN
                      (ret | OPT_InstructionFormat.Return_traits),
                      0, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)151, OPT_InstructionFormat.NullCheck_format,  //NULL_CHECK
                      (immedPEI | OPT_InstructionFormat.NullCheck_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)152, OPT_InstructionFormat.Goto_format,  //GOTO
                      (branch | OPT_InstructionFormat.Goto_traits),
                      0, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)153, OPT_InstructionFormat.Unary_format,  //BOOLEAN_NOT
                      (none | OPT_InstructionFormat.Unary_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)154, OPT_InstructionFormat.BooleanCmp_format,  //BOOLEAN_CMP_INT
                      (compare | OPT_InstructionFormat.BooleanCmp_traits),
                      1, 0, 4,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)155, OPT_InstructionFormat.BooleanCmp_format,  //BOOLEAN_CMP_ADDR
                      (compare | OPT_InstructionFormat.BooleanCmp_traits),
                      1, 0, 4,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)156, OPT_InstructionFormat.BooleanCmp_format,  //BOOLEAN_CMP_LONG
                      (compare | OPT_InstructionFormat.BooleanCmp_traits),
                      1, 0, 4,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)157, OPT_InstructionFormat.BooleanCmp_format,  //BOOLEAN_CMP_FLOAT
                      (compare | OPT_InstructionFormat.BooleanCmp_traits),
                      1, 0, 4,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)158, OPT_InstructionFormat.BooleanCmp_format,  //BOOLEAN_CMP_DOUBLE
                      (compare | OPT_InstructionFormat.BooleanCmp_traits),
                      1, 0, 4,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)159, OPT_InstructionFormat.Load_format,  //BYTE_LOAD
                      (load | OPT_InstructionFormat.Load_traits),
                      1, 0, 4,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)160, OPT_InstructionFormat.Load_format,  //UBYTE_LOAD
                      (load | OPT_InstructionFormat.Load_traits),
                      1, 0, 4,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)161, OPT_InstructionFormat.Load_format,  //SHORT_LOAD
                      (load | OPT_InstructionFormat.Load_traits),
                      1, 0, 4,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)162, OPT_InstructionFormat.Load_format,  //USHORT_LOAD
                      (load | OPT_InstructionFormat.Load_traits),
                      1, 0, 4,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)163, OPT_InstructionFormat.Load_format,  //REF_LOAD
                      (load | OPT_InstructionFormat.Load_traits),
                      1, 0, 4,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)164, OPT_InstructionFormat.Store_format,  //REF_STORE
                      (store | OPT_InstructionFormat.Store_traits),
                      0, 0, 5,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)165, OPT_InstructionFormat.Load_format,  //INT_LOAD
                      (load | OPT_InstructionFormat.Load_traits),
                      1, 0, 4,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)166, OPT_InstructionFormat.Load_format,  //LONG_LOAD
                      (load | OPT_InstructionFormat.Load_traits),
                      1, 0, 4,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)167, OPT_InstructionFormat.Load_format,  //FLOAT_LOAD
                      (load | OPT_InstructionFormat.Load_traits),
                      1, 0, 4,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)168, OPT_InstructionFormat.Load_format,  //DOUBLE_LOAD
                      (load | OPT_InstructionFormat.Load_traits),
                      1, 0, 4,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)169, OPT_InstructionFormat.Store_format,  //BYTE_STORE
                      (store | OPT_InstructionFormat.Store_traits),
                      0, 0, 5,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)170, OPT_InstructionFormat.Store_format,  //SHORT_STORE
                      (store | OPT_InstructionFormat.Store_traits),
                      0, 0, 5,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)171, OPT_InstructionFormat.Store_format,  //INT_STORE
                      (store | OPT_InstructionFormat.Store_traits),
                      0, 0, 5,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)172, OPT_InstructionFormat.Store_format,  //LONG_STORE
                      (store | OPT_InstructionFormat.Store_traits),
                      0, 0, 5,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)173, OPT_InstructionFormat.Store_format,  //FLOAT_STORE
                      (store | OPT_InstructionFormat.Store_traits),
                      0, 0, 5,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)174, OPT_InstructionFormat.Store_format,  //DOUBLE_STORE
                      (store | OPT_InstructionFormat.Store_traits),
                      0, 0, 5,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)175, OPT_InstructionFormat.Prepare_format,  //PREPARE_INT
                      (load | acquire | OPT_InstructionFormat.Prepare_traits),
                      1, 0, 4,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)176, OPT_InstructionFormat.Prepare_format,  //PREPARE_ADDR
                      (load | acquire | OPT_InstructionFormat.Prepare_traits),
                      1, 0, 4,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)177, OPT_InstructionFormat.Prepare_format,  //PREPARE_LONG
                      (load | acquire | OPT_InstructionFormat.Prepare_traits),
                      1, 0, 4,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)178, OPT_InstructionFormat.Attempt_format,  //ATTEMPT_INT
                      (load | store | compare | release | OPT_InstructionFormat.Attempt_traits),
                      1, 0, 6,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)179, OPT_InstructionFormat.Attempt_format,  //ATTEMPT_ADDR
                      (load | store | compare | release | OPT_InstructionFormat.Attempt_traits),
                      1, 0, 6,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)180, OPT_InstructionFormat.Attempt_format,  //ATTEMPT_LONG
                      (load | store | compare | release  | OPT_InstructionFormat.Attempt_traits),
                      1, 0, 6,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)181, OPT_InstructionFormat.Call_format,  //CALL
                      (call | memAsLoad | memAsStore | dynLink | immedPEI | OPT_InstructionFormat.Call_traits),
                      1, 0, 3,
                      OPT_PhysicalDefUse.maskcallDefs,
                      OPT_PhysicalDefUse.maskcallUses),
     new OPT_Operator((char)182, OPT_InstructionFormat.Call_format,  //SYSCALL
                      (call | memAsLoad | memAsStore | OPT_InstructionFormat.Call_traits),
                      1, 0, 3,
                      OPT_PhysicalDefUse.maskcallDefs,
                      OPT_PhysicalDefUse.maskcallUses),
     new OPT_Operator((char)183, OPT_InstructionFormat.Empty_format,  //YIELDPOINT_PROLOGUE
                      (tsp | yieldPoint | OPT_InstructionFormat.Empty_traits),
                      0, 0, 0,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)184, OPT_InstructionFormat.Empty_format,  //YIELDPOINT_EPILOGUE
                      (tsp | yieldPoint | OPT_InstructionFormat.Empty_traits),
                      0, 0, 0,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)185, OPT_InstructionFormat.Empty_format,  //YIELDPOINT_BACKEDGE
                      (tsp | yieldPoint | OPT_InstructionFormat.Empty_traits),
                      0, 0, 0,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)186, OPT_InstructionFormat.OsrPoint_format,  //YIELDPOINT_OSR
                      (tsp | yieldPoint | OPT_InstructionFormat.OsrPoint_traits),
                      0, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)187, OPT_InstructionFormat.OsrBarrier_format,  //OSR_BARRIER
                      (none | OPT_InstructionFormat.OsrBarrier_traits),
                      0, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)188, OPT_InstructionFormat.Prologue_format,  //IR_PROLOGUE
                      (immedPEI | OPT_InstructionFormat.Prologue_traits),
                      0, 0, 0,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)189, OPT_InstructionFormat.CacheOp_format,  //RESOLVE
                      (tsp | dynLink | immedPEI | OPT_InstructionFormat.CacheOp_traits),
                      0, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)190, OPT_InstructionFormat.Unary_format,  //RESOLVE_MEMBER
                      (tsp | dynLink | immedPEI | OPT_InstructionFormat.Unary_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)191, OPT_InstructionFormat.Nullary_format,  //GET_TIME_BASE
                      (none | OPT_InstructionFormat.Nullary_traits),
                      1, 0, 0,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)192, OPT_InstructionFormat.InstrumentedCounter_format,  //INSTRUMENTED_EVENT_COUNTER
                      (none | OPT_InstructionFormat.InstrumentedCounter_traits),
                      0, 0, 3,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)193, OPT_InstructionFormat.TrapIf_format,  //TRAP_IF
                      (immedPEI | OPT_InstructionFormat.TrapIf_traits),
                      1, 0, 4,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)194, OPT_InstructionFormat.Trap_format,  //TRAP
                      (immedPEI | OPT_InstructionFormat.Trap_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)195, OPT_InstructionFormat.Unary_format,  //FLOAT_AS_INT_BITS
                      (none | OPT_InstructionFormat.Unary_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)196, OPT_InstructionFormat.Unary_format,  //INT_BITS_AS_FLOAT
                      (none | OPT_InstructionFormat.Unary_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)197, OPT_InstructionFormat.Unary_format,  //DOUBLE_AS_LONG_BITS
                      (none | OPT_InstructionFormat.Unary_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)198, OPT_InstructionFormat.Unary_format,  //LONG_BITS_AS_DOUBLE
                      (none | OPT_InstructionFormat.Unary_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)199, OPT_InstructionFormat.GuardedUnary_format,  //ARRAYLENGTH
                      (none | OPT_InstructionFormat.GuardedUnary_traits),
                      1, 0, 2,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)200, OPT_InstructionFormat.GuardedUnary_format,  //GET_OBJ_TIB
                      (none | OPT_InstructionFormat.GuardedUnary_traits),
                      1, 0, 2,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)201, OPT_InstructionFormat.Unary_format,  //GET_CLASS_TIB
                      (none | OPT_InstructionFormat.Unary_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)202, OPT_InstructionFormat.Unary_format,  //GET_TYPE_FROM_TIB
                      (none | OPT_InstructionFormat.Unary_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)203, OPT_InstructionFormat.Unary_format,  //GET_SUPERCLASS_IDS_FROM_TIB
                      (none | OPT_InstructionFormat.Unary_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)204, OPT_InstructionFormat.Unary_format,  //GET_DOES_IMPLEMENT_FROM_TIB
                      (none | OPT_InstructionFormat.Unary_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)205, OPT_InstructionFormat.Unary_format,  //GET_ARRAY_ELEMENT_TIB_FROM_TIB
                      (none | OPT_InstructionFormat.Unary_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)206, OPT_InstructionFormat.LowTableSwitch_format,  //LOWTABLESWITCH
                      (branch | OPT_InstructionFormat.LowTableSwitch_traits),
                      0, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
  //////////////////////////
  // END   Architecture Independent opcodes.
  // BEGIN Architecture Dependent opcodes & MIR.
  //////////////////////////
     new OPT_Operator((char)(0 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //ADDRESS_CONSTANT
                      OPT_InstructionFormat.Unassigned_format,
                      (none),
                      0,0,0,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(1 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //INT_CONSTANT
                      OPT_InstructionFormat.Unassigned_format,
                      (none),
                      0,0,0,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(2 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //LONG_CONSTANT
                      OPT_InstructionFormat.Unassigned_format,
                      (none),
                      0,0,0,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(3 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //REGISTER
                      OPT_InstructionFormat.Unassigned_format,
                      (none),
                      0,0,0,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(4 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //OTHER_OPERAND
                      OPT_InstructionFormat.Unassigned_format,
                      (none),
                      0,0,0,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(5 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //NULL
                      OPT_InstructionFormat.Unassigned_format,
                      (none),
                      0,0,0,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(6 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //BRANCH_TARGET
                      OPT_InstructionFormat.Unassigned_format,
                      (none),
                      0,0,0,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(7 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //MATERIALIZE_FP_CONSTANT
                      OPT_InstructionFormat.Binary_format,
                      (none | OPT_InstructionFormat.Binary_traits),
                      1, 0, 2,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(8 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //GET_JTOC
                      OPT_InstructionFormat.Unary_format,
                      (none | OPT_InstructionFormat.Unary_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(9 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //GET_CURRENT_PROCESSOR
                      OPT_InstructionFormat.Nullary_format,
                      (none | OPT_InstructionFormat.Nullary_traits),
                      1, 0, 0,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(10 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //ROUND_TO_ZERO
                      OPT_InstructionFormat.Empty_format,
                      (none | OPT_InstructionFormat.Empty_traits),
                      0, 0, 0,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(11 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //CLEAR_FLOATING_POINT_STATE
                      OPT_InstructionFormat.Empty_format,
                      (none | OPT_InstructionFormat.Empty_traits),
                      0, 0, 0,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(12 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //PREFETCH
                      OPT_InstructionFormat.CacheOp_format,
                      (none | OPT_InstructionFormat.CacheOp_traits),
                      0, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(13 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //PAUSE
                      OPT_InstructionFormat.Empty_format,
                      (none | OPT_InstructionFormat.Empty_traits),
                      0, 0, 0,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(14 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //FP_ADD
                      OPT_InstructionFormat.Binary_format,
                      (none | OPT_InstructionFormat.Binary_traits),
                      1, 0, 2,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(15 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //FP_SUB
                      OPT_InstructionFormat.Binary_format,
                      (none | OPT_InstructionFormat.Binary_traits),
                      1, 0, 2,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(16 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //FP_MUL
                      OPT_InstructionFormat.Binary_format,
                      (none | OPT_InstructionFormat.Binary_traits),
                      1, 0, 2,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(17 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //FP_DIV
                      OPT_InstructionFormat.Binary_format,
                      (none | OPT_InstructionFormat.Binary_traits),
                      1, 0, 2,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(18 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //FP_NEG
                      OPT_InstructionFormat.Unary_format,
                      (none | OPT_InstructionFormat.Unary_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(19 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //FP_REM
                      OPT_InstructionFormat.Binary_format,
                      (none | OPT_InstructionFormat.Binary_traits),
                      1, 0, 2,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(20 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //INT_2FP
                      OPT_InstructionFormat.Unary_format,
                      (none | OPT_InstructionFormat.Unary_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(21 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //LONG_2FP
                      OPT_InstructionFormat.Unary_format,
                      (none | OPT_InstructionFormat.Unary_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(22 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //CMP_CMOV
                      OPT_InstructionFormat.CondMove_format,
                      (compare | OPT_InstructionFormat.CondMove_traits),
                      1, 0, 5,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(23 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //FCMP_CMOV
                      OPT_InstructionFormat.CondMove_format,
                      (compare | OPT_InstructionFormat.CondMove_traits),
                      1, 0, 5,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(24 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //LCMP_CMOV
                      OPT_InstructionFormat.CondMove_format,
                      (compare | OPT_InstructionFormat.CondMove_traits),
                      1, 0, 5,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(25 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //CMP_FCMOV
                      OPT_InstructionFormat.CondMove_format,
                      (compare | OPT_InstructionFormat.CondMove_traits),
                      1, 0, 5,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(26 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //FCMP_FCMOV
                      OPT_InstructionFormat.CondMove_format,
                      (compare | OPT_InstructionFormat.CondMove_traits),
                      1, 0, 5,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(27 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //CALL_SAVE_VOLATILE
                      OPT_InstructionFormat.MIR_Call_format,
                      (call | immedPEI | OPT_InstructionFormat.MIR_Call_traits),
                      2, 0, 2,
                      OPT_PhysicalDefUse.maskcallDefs,
                      OPT_PhysicalDefUse.maskcallUses),
     new OPT_Operator((char)(28 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //MIR_START
                      OPT_InstructionFormat.Unassigned_format,
                      (none),
                      0,0,0,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(29 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //REQUIRE_ESP
                      OPT_InstructionFormat.MIR_UnaryNoRes_format,
                      (none | OPT_InstructionFormat.MIR_UnaryNoRes_traits),
                      0, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(30 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //ADVISE_ESP
                      OPT_InstructionFormat.MIR_UnaryNoRes_format,
                      (none | OPT_InstructionFormat.MIR_UnaryNoRes_traits),
                      0, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(31 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //MIR_LOWTABLESWITCH
                      OPT_InstructionFormat.MIR_LowTableSwitch_format,
                      (branch | OPT_InstructionFormat.MIR_LowTableSwitch_traits),
                      0, 1, 0,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(32 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_FCLEAR
                      OPT_InstructionFormat.MIR_UnaryNoRes_format,
                      (none | OPT_InstructionFormat.MIR_UnaryNoRes_traits),
                      0, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(33 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //DUMMY_DEF
                      OPT_InstructionFormat.MIR_Nullary_format,
                      (none | OPT_InstructionFormat.MIR_Nullary_traits),
                      1, 0, 0,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(34 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //DUMMY_USE
                      OPT_InstructionFormat.MIR_UnaryNoRes_format,
                      (none | OPT_InstructionFormat.MIR_UnaryNoRes_traits),
                      0, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(35 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_FMOV_ENDING_LIVE_RANGE
                      OPT_InstructionFormat.MIR_Move_format,
                      (move | OPT_InstructionFormat.MIR_Move_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(36 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_FMOV
                      OPT_InstructionFormat.MIR_Move_format,
                      (move | OPT_InstructionFormat.MIR_Move_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(37 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_TRAPIF
                      OPT_InstructionFormat.MIR_TrapIf_format,
                      (immedPEI | OPT_InstructionFormat.MIR_TrapIf_traits),
                      1, 0, 4,
                      OPT_PhysicalDefUse.maskAF_CF_OF_PF_SF_ZF,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(38 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_OFFSET
                      OPT_InstructionFormat.MIR_CaseLabel_format,
                      (none | OPT_InstructionFormat.MIR_CaseLabel_traits),
                      0, 0, 2,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(39 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_LOCK_CMPXCHG
                      OPT_InstructionFormat.MIR_CompareExchange_format,
                      (compare | OPT_InstructionFormat.MIR_CompareExchange_traits),
                      0, 2, 1,
                      OPT_PhysicalDefUse.maskAF_CF_OF_PF_SF_ZF,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(40 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_LOCK_CMPXCHG8B
                      OPT_InstructionFormat.MIR_CompareExchange8B_format,
                      (compare | OPT_InstructionFormat.MIR_CompareExchange8B_traits),
                      0, 3, 2,
                      OPT_PhysicalDefUse.maskAF_CF_OF_PF_SF_ZF,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(41 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_ADC
                      OPT_InstructionFormat.MIR_BinaryAcc_format,
                      (none | OPT_InstructionFormat.MIR_BinaryAcc_traits),
                      0, 1, 1,
                      OPT_PhysicalDefUse.maskAF_CF_OF_PF_SF_ZF,
                      OPT_PhysicalDefUse.maskCF),
     new OPT_Operator((char)(42 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_ADD
                      OPT_InstructionFormat.MIR_BinaryAcc_format,
                      (none | OPT_InstructionFormat.MIR_BinaryAcc_traits),
                      0, 1, 1,
                      OPT_PhysicalDefUse.maskAF_CF_OF_PF_SF_ZF,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(43 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_AND
                      OPT_InstructionFormat.MIR_BinaryAcc_format,
                      (none | OPT_InstructionFormat.MIR_BinaryAcc_traits),
                      0, 1, 1,
                      OPT_PhysicalDefUse.maskAF_CF_OF_PF_SF_ZF,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(44 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_BSWAP
                      OPT_InstructionFormat.MIR_UnaryAcc_format,
                      (none | OPT_InstructionFormat.MIR_UnaryAcc_traits),
                      0, 1, 0,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(45 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_BT
                      OPT_InstructionFormat.MIR_Test_format,
                      (none | OPT_InstructionFormat.MIR_Test_traits),
                      0, 0, 2,
                      OPT_PhysicalDefUse.maskCF,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(46 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_BTC
                      OPT_InstructionFormat.MIR_Test_format,
                      (none | OPT_InstructionFormat.MIR_Test_traits),
                      0, 0, 2,
                      OPT_PhysicalDefUse.maskCF,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(47 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_BTR
                      OPT_InstructionFormat.MIR_Test_format,
                      (none | OPT_InstructionFormat.MIR_Test_traits),
                      0, 0, 2,
                      OPT_PhysicalDefUse.maskCF,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(48 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_BTS
                      OPT_InstructionFormat.MIR_Test_format,
                      (none | OPT_InstructionFormat.MIR_Test_traits),
                      0, 0, 2,
                      OPT_PhysicalDefUse.maskCF,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(49 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_SYSCALL
                      OPT_InstructionFormat.MIR_Call_format,
                      (call | OPT_InstructionFormat.MIR_Call_traits),
                      2, 0, 2,
                      OPT_PhysicalDefUse.maskcallDefs,
                      OPT_PhysicalDefUse.maskcallUses),
     new OPT_Operator((char)(50 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_CALL
                      OPT_InstructionFormat.MIR_Call_format,
                      (call | immedPEI | OPT_InstructionFormat.MIR_Call_traits),
                      2, 0, 2,
                      OPT_PhysicalDefUse.maskcallDefs,
                      OPT_PhysicalDefUse.maskcallUses),
     new OPT_Operator((char)(51 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_CDQ
                      OPT_InstructionFormat.MIR_ConvertDW2QW_format,
                      (none | OPT_InstructionFormat.MIR_ConvertDW2QW_traits),
                      1, 1, 0,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(52 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_CMOV
                      OPT_InstructionFormat.MIR_CondMove_format,
                      (none | OPT_InstructionFormat.MIR_CondMove_traits),
                      0, 1, 2,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.maskCF_OF_PF_SF_ZF),
     new OPT_Operator((char)(53 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_CMP
                      OPT_InstructionFormat.MIR_Compare_format,
                      (compare | OPT_InstructionFormat.MIR_Compare_traits),
                      0, 0, 2,
                      OPT_PhysicalDefUse.maskAF_CF_OF_PF_SF_ZF,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(54 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_CMPXCHG
                      OPT_InstructionFormat.MIR_CompareExchange_format,
                      (compare | OPT_InstructionFormat.MIR_CompareExchange_traits),
                      0, 2, 1,
                      OPT_PhysicalDefUse.maskAF_CF_OF_PF_SF_ZF,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(55 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_CMPXCHG8B
                      OPT_InstructionFormat.MIR_CompareExchange8B_format,
                      (compare | OPT_InstructionFormat.MIR_CompareExchange8B_traits),
                      0, 3, 2,
                      OPT_PhysicalDefUse.maskAF_CF_OF_PF_SF_ZF,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(56 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_DEC
                      OPT_InstructionFormat.MIR_UnaryAcc_format,
                      (none | OPT_InstructionFormat.MIR_UnaryAcc_traits),
                      0, 1, 0,
                      OPT_PhysicalDefUse.maskAF_OF_PF_SF_ZF,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(57 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_DIV
                      OPT_InstructionFormat.MIR_Divide_format,
                      (none | OPT_InstructionFormat.MIR_Divide_traits),
                      0, 2, 2,
                      OPT_PhysicalDefUse.maskAF_CF_OF_PF_SF_ZF,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(58 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_FADD
                      OPT_InstructionFormat.MIR_BinaryAcc_format,
                      (none | OPT_InstructionFormat.MIR_BinaryAcc_traits),
                      0, 1, 1,
                      OPT_PhysicalDefUse.maskC0_C1_C2_C3,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(59 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_FADDP
                      OPT_InstructionFormat.MIR_BinaryAcc_format,
                      (fpPop | OPT_InstructionFormat.MIR_BinaryAcc_traits),
                      0, 1, 1,
                      OPT_PhysicalDefUse.maskC0_C1_C2_C3,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(60 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_FCHS
                      OPT_InstructionFormat.MIR_UnaryAcc_format,
                      (none | OPT_InstructionFormat.MIR_UnaryAcc_traits),
                      0, 1, 0,
                      OPT_PhysicalDefUse.maskC0_C1_C2_C3,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(61 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_FCMOV
                      OPT_InstructionFormat.MIR_CondMove_format,
                      (none | OPT_InstructionFormat.MIR_CondMove_traits),
                      0, 1, 2,
                      OPT_PhysicalDefUse.maskC0_C1_C2_C3,
                      OPT_PhysicalDefUse.maskCF_PF_ZF),
     new OPT_Operator((char)(62 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_FCOMI
                      OPT_InstructionFormat.MIR_Compare_format,
                      (compare | OPT_InstructionFormat.MIR_Compare_traits),
                      0, 0, 2,
                      OPT_PhysicalDefUse.maskCF_PF_ZF,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(63 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_FCOMIP
                      OPT_InstructionFormat.MIR_Compare_format,
                      (compare | fpPop | OPT_InstructionFormat.MIR_Compare_traits),
                      0, 0, 2,
                      OPT_PhysicalDefUse.maskCF_PF_ZF,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(64 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_FDIV
                      OPT_InstructionFormat.MIR_BinaryAcc_format,
                      (none | OPT_InstructionFormat.MIR_BinaryAcc_traits),
                      0, 1, 1,
                      OPT_PhysicalDefUse.maskC0_C1_C2_C3,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(65 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_FDIVP
                      OPT_InstructionFormat.MIR_BinaryAcc_format,
                      (fpPop | OPT_InstructionFormat.MIR_BinaryAcc_traits),
                      0, 1, 1,
                      OPT_PhysicalDefUse.maskC0_C1_C2_C3,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(66 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_FDIVR
                      OPT_InstructionFormat.MIR_BinaryAcc_format,
                      (none | OPT_InstructionFormat.MIR_BinaryAcc_traits),
                      0, 1, 1,
                      OPT_PhysicalDefUse.maskC0_C1_C2_C3,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(67 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_FDIVRP
                      OPT_InstructionFormat.MIR_BinaryAcc_format,
                      (fpPop | OPT_InstructionFormat.MIR_BinaryAcc_traits),
                      0, 1, 1,
                      OPT_PhysicalDefUse.maskC0_C1_C2_C3,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(68 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_FEXAM
                      OPT_InstructionFormat.MIR_UnaryNoRes_format,
                      (none | OPT_InstructionFormat.MIR_UnaryNoRes_traits),
                      0, 0, 1,
                      OPT_PhysicalDefUse.maskC0_C1_C2_C3,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(69 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_FXCH
                      OPT_InstructionFormat.MIR_XChng_format,
                      (none | OPT_InstructionFormat.MIR_XChng_traits),
                      0, 2, 0,
                      OPT_PhysicalDefUse.maskC0_C1_C2_C3,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(70 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_FFREE
                      OPT_InstructionFormat.MIR_Nullary_format,
                      (none | OPT_InstructionFormat.MIR_Nullary_traits),
                      1, 0, 0,
                      OPT_PhysicalDefUse.maskC0_C1_C2_C3,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(71 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_FIADD
                      OPT_InstructionFormat.MIR_BinaryAcc_format,
                      (none | OPT_InstructionFormat.MIR_BinaryAcc_traits),
                      0, 1, 1,
                      OPT_PhysicalDefUse.maskC0_C1_C2_C3,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(72 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_FIDIV
                      OPT_InstructionFormat.MIR_BinaryAcc_format,
                      (none | OPT_InstructionFormat.MIR_BinaryAcc_traits),
                      0, 1, 1,
                      OPT_PhysicalDefUse.maskC0_C1_C2_C3,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(73 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_FIDIVR
                      OPT_InstructionFormat.MIR_BinaryAcc_format,
                      (none | OPT_InstructionFormat.MIR_BinaryAcc_traits),
                      0, 1, 1,
                      OPT_PhysicalDefUse.maskC0_C1_C2_C3,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(74 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_FILD
                      OPT_InstructionFormat.MIR_Move_format,
                      (fpPush | OPT_InstructionFormat.MIR_Move_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.maskC0_C1_C2_C3,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(75 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_FIMUL
                      OPT_InstructionFormat.MIR_BinaryAcc_format,
                      (none | OPT_InstructionFormat.MIR_BinaryAcc_traits),
                      0, 1, 1,
                      OPT_PhysicalDefUse.maskC0_C1_C2_C3,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(76 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_FINIT
                      OPT_InstructionFormat.MIR_Empty_format,
                      (none | OPT_InstructionFormat.MIR_Empty_traits),
                      0, 0, 0,
                      OPT_PhysicalDefUse.maskC0_C1_C2_C3,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(77 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_FIST
                      OPT_InstructionFormat.MIR_Move_format,
                      (none | OPT_InstructionFormat.MIR_Move_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.maskC0_C1_C2_C3,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(78 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_FISTP
                      OPT_InstructionFormat.MIR_Move_format,
                      (fpPop | OPT_InstructionFormat.MIR_Move_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.maskC0_C1_C2_C3,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(79 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_FISUB
                      OPT_InstructionFormat.MIR_BinaryAcc_format,
                      (none | OPT_InstructionFormat.MIR_BinaryAcc_traits),
                      0, 1, 1,
                      OPT_PhysicalDefUse.maskC0_C1_C2_C3,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(80 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_FISUBR
                      OPT_InstructionFormat.MIR_BinaryAcc_format,
                      (none | OPT_InstructionFormat.MIR_BinaryAcc_traits),
                      0, 1, 1,
                      OPT_PhysicalDefUse.maskC0_C1_C2_C3,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(81 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_FLD
                      OPT_InstructionFormat.MIR_Move_format,
                      (fpPush | OPT_InstructionFormat.MIR_Move_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.maskC0_C1_C2_C3,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(82 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_FLDCW
                      OPT_InstructionFormat.MIR_UnaryNoRes_format,
                      (none | OPT_InstructionFormat.MIR_UnaryNoRes_traits),
                      0, 0, 1,
                      OPT_PhysicalDefUse.maskC0_C1_C2_C3,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(83 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_FLD1
                      OPT_InstructionFormat.MIR_Nullary_format,
                      (fpPush | OPT_InstructionFormat.MIR_Nullary_traits),
                      1, 0, 0,
                      OPT_PhysicalDefUse.maskC0_C1_C2_C3,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(84 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_FLDL2T
                      OPT_InstructionFormat.MIR_Nullary_format,
                      (fpPush | OPT_InstructionFormat.MIR_Nullary_traits),
                      1, 0, 0,
                      OPT_PhysicalDefUse.maskC0_C1_C2_C3,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(85 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_FLDL2E
                      OPT_InstructionFormat.MIR_Nullary_format,
                      (fpPush | OPT_InstructionFormat.MIR_Nullary_traits),
                      1, 0, 0,
                      OPT_PhysicalDefUse.maskC0_C1_C2_C3,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(86 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_FLDPI
                      OPT_InstructionFormat.MIR_Nullary_format,
                      (fpPush | OPT_InstructionFormat.MIR_Nullary_traits),
                      1, 0, 0,
                      OPT_PhysicalDefUse.maskC0_C1_C2_C3,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(87 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_FLDLG2
                      OPT_InstructionFormat.MIR_Nullary_format,
                      (fpPush | OPT_InstructionFormat.MIR_Nullary_traits),
                      1, 0, 0,
                      OPT_PhysicalDefUse.maskC0_C1_C2_C3,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(88 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_FLDLN2
                      OPT_InstructionFormat.MIR_Nullary_format,
                      (fpPush | OPT_InstructionFormat.MIR_Nullary_traits),
                      1, 0, 0,
                      OPT_PhysicalDefUse.maskC0_C1_C2_C3,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(89 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_FLDZ
                      OPT_InstructionFormat.MIR_Nullary_format,
                      (fpPush | OPT_InstructionFormat.MIR_Nullary_traits),
                      1, 0, 0,
                      OPT_PhysicalDefUse.maskC0_C1_C2_C3,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(90 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_FMUL
                      OPT_InstructionFormat.MIR_BinaryAcc_format,
                      (none | OPT_InstructionFormat.MIR_BinaryAcc_traits),
                      0, 1, 1,
                      OPT_PhysicalDefUse.maskC0_C1_C2_C3,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(91 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_FMULP
                      OPT_InstructionFormat.MIR_BinaryAcc_format,
                      (fpPop | OPT_InstructionFormat.MIR_BinaryAcc_traits),
                      0, 1, 1,
                      OPT_PhysicalDefUse.maskC0_C1_C2_C3,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(92 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_FNSTCW
                      OPT_InstructionFormat.MIR_UnaryNoRes_format,
                      (none | OPT_InstructionFormat.MIR_UnaryNoRes_traits),
                      0, 0, 1,
                      OPT_PhysicalDefUse.maskC0_C1_C2_C3,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(93 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_FNINIT
                      OPT_InstructionFormat.MIR_Empty_format,
                      (none | OPT_InstructionFormat.MIR_Empty_traits),
                      0, 0, 0,
                      OPT_PhysicalDefUse.maskC0_C1_C2_C3,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(94 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_FNSAVE
                      OPT_InstructionFormat.MIR_FSave_format,
                      (none | OPT_InstructionFormat.MIR_FSave_traits),
                      0, 0, 1,
                      OPT_PhysicalDefUse.maskC0_C1_C2_C3,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(95 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_FPREM
                      OPT_InstructionFormat.MIR_BinaryAcc_format,
                      (none | OPT_InstructionFormat.MIR_BinaryAcc_traits),
                      0, 1, 1,
                      OPT_PhysicalDefUse.maskC0_C1_C2_C3,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(96 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_FRSTOR
                      OPT_InstructionFormat.MIR_FSave_format,
                      (none | OPT_InstructionFormat.MIR_FSave_traits),
                      0, 0, 1,
                      OPT_PhysicalDefUse.maskC0_C1_C2_C3,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(97 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_FST
                      OPT_InstructionFormat.MIR_Move_format,
                      (none | OPT_InstructionFormat.MIR_Move_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.maskC0_C1_C2_C3,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(98 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_FSTCW
                      OPT_InstructionFormat.MIR_UnaryNoRes_format,
                      (none | OPT_InstructionFormat.MIR_UnaryNoRes_traits),
                      0, 0, 1,
                      OPT_PhysicalDefUse.maskC0_C1_C2_C3,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(99 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_FSTP
                      OPT_InstructionFormat.MIR_Move_format,
                      (fpPop | OPT_InstructionFormat.MIR_Move_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.maskC0_C1_C2_C3,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(100 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_FSUB
                      OPT_InstructionFormat.MIR_BinaryAcc_format,
                      (none | OPT_InstructionFormat.MIR_BinaryAcc_traits),
                      0, 1, 1,
                      OPT_PhysicalDefUse.maskC0_C1_C2_C3,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(101 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_FSUBP
                      OPT_InstructionFormat.MIR_BinaryAcc_format,
                      (fpPop | OPT_InstructionFormat.MIR_BinaryAcc_traits),
                      0, 1, 1,
                      OPT_PhysicalDefUse.maskC0_C1_C2_C3,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(102 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_FSUBR
                      OPT_InstructionFormat.MIR_BinaryAcc_format,
                      (none | OPT_InstructionFormat.MIR_BinaryAcc_traits),
                      0, 1, 1,
                      OPT_PhysicalDefUse.maskC0_C1_C2_C3,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(103 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_FSUBRP
                      OPT_InstructionFormat.MIR_BinaryAcc_format,
                      (fpPop | OPT_InstructionFormat.MIR_BinaryAcc_traits),
                      0, 1, 1,
                      OPT_PhysicalDefUse.maskC0_C1_C2_C3,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(104 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_FUCOMI
                      OPT_InstructionFormat.MIR_Compare_format,
                      (compare | OPT_InstructionFormat.MIR_Compare_traits),
                      0, 0, 2,
                      OPT_PhysicalDefUse.maskCF_PF_ZF,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(105 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_FUCOMIP
                      OPT_InstructionFormat.MIR_Compare_format,
                      (compare | OPT_InstructionFormat.MIR_Compare_traits),
                      0, 0, 2,
                      OPT_PhysicalDefUse.maskCF_PF_ZF,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(106 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_IDIV
                      OPT_InstructionFormat.MIR_Divide_format,
                      (none | OPT_InstructionFormat.MIR_Divide_traits),
                      0, 2, 2,
                      OPT_PhysicalDefUse.maskAF_CF_OF_PF_SF_ZF,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(107 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_IMUL1
                      OPT_InstructionFormat.MIR_Multiply_format,
                      (none | OPT_InstructionFormat.MIR_Multiply_traits),
                      1, 1, 1,
                      OPT_PhysicalDefUse.maskAF_CF_OF_PF_SF_ZF,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(108 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_IMUL2
                      OPT_InstructionFormat.MIR_BinaryAcc_format,
                      (none | OPT_InstructionFormat.MIR_BinaryAcc_traits),
                      0, 1, 1,
                      OPT_PhysicalDefUse.maskAF_CF_OF_PF_SF_ZF,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(109 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_INC
                      OPT_InstructionFormat.MIR_UnaryAcc_format,
                      (none | OPT_InstructionFormat.MIR_UnaryAcc_traits),
                      0, 1, 0,
                      OPT_PhysicalDefUse.maskAF_OF_PF_SF_ZF,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(110 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_INT
                      OPT_InstructionFormat.MIR_Trap_format,
                      (immedPEI | OPT_InstructionFormat.MIR_Trap_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(111 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_JCC
                      OPT_InstructionFormat.MIR_CondBranch_format,
                      (branch | conditional | OPT_InstructionFormat.MIR_CondBranch_traits),
                      0, 0, 3,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.maskAF_CF_OF_PF_SF_ZF),
     new OPT_Operator((char)(112 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_JCC2
                      OPT_InstructionFormat.MIR_CondBranch2_format,
                      (branch | conditional | OPT_InstructionFormat.MIR_CondBranch2_traits),
                      0, 0, 6,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.maskAF_CF_OF_PF_SF_ZF),
     new OPT_Operator((char)(113 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_JMP
                      OPT_InstructionFormat.MIR_Branch_format,
                      (branch | OPT_InstructionFormat.MIR_Branch_traits),
                      0, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(114 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_LEA
                      OPT_InstructionFormat.MIR_Lea_format,
                      (none | OPT_InstructionFormat.MIR_Lea_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(115 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_LOCK
                      OPT_InstructionFormat.MIR_Empty_format,
                      (none | OPT_InstructionFormat.MIR_Empty_traits),
                      0, 0, 0,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(116 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_MOV
                      OPT_InstructionFormat.MIR_Move_format,
                      (move | OPT_InstructionFormat.MIR_Move_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(117 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_MOVZX__B
                      OPT_InstructionFormat.MIR_Unary_format,
                      (move | OPT_InstructionFormat.MIR_Unary_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(118 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_MOVSX__B
                      OPT_InstructionFormat.MIR_Unary_format,
                      (move | OPT_InstructionFormat.MIR_Unary_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(119 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_MOVZX__W
                      OPT_InstructionFormat.MIR_Unary_format,
                      (move | OPT_InstructionFormat.MIR_Unary_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(120 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_MOVSX__W
                      OPT_InstructionFormat.MIR_Unary_format,
                      (move | OPT_InstructionFormat.MIR_Unary_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(121 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_MUL
                      OPT_InstructionFormat.MIR_Multiply_format,
                      (none | OPT_InstructionFormat.MIR_Multiply_traits),
                      1, 1, 1,
                      OPT_PhysicalDefUse.maskAF_CF_OF_PF_SF_ZF,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(122 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_NEG
                      OPT_InstructionFormat.MIR_UnaryAcc_format,
                      (none | OPT_InstructionFormat.MIR_UnaryAcc_traits),
                      0, 1, 0,
                      OPT_PhysicalDefUse.maskAF_CF_OF_PF_SF_ZF,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(123 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_NOT
                      OPT_InstructionFormat.MIR_UnaryAcc_format,
                      (none | OPT_InstructionFormat.MIR_UnaryAcc_traits),
                      0, 1, 0,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(124 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_OR
                      OPT_InstructionFormat.MIR_BinaryAcc_format,
                      (none | OPT_InstructionFormat.MIR_BinaryAcc_traits),
                      0, 1, 1,
                      OPT_PhysicalDefUse.maskAF_CF_OF_PF_SF_ZF,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(125 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_PAUSE
                      OPT_InstructionFormat.MIR_Empty_format,
                      (none | OPT_InstructionFormat.MIR_Empty_traits),
                      0, 0, 0,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(126 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_PREFETCHNTA
                      OPT_InstructionFormat.MIR_CacheOp_format,
                      (none | OPT_InstructionFormat.MIR_CacheOp_traits),
                      0, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(127 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_POP
                      OPT_InstructionFormat.MIR_Nullary_format,
                      (none | OPT_InstructionFormat.MIR_Nullary_traits),
                      1, 0, 0,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(128 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_PUSH
                      OPT_InstructionFormat.MIR_UnaryNoRes_format,
                      (none | OPT_InstructionFormat.MIR_UnaryNoRes_traits),
                      0, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(129 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_RCL
                      OPT_InstructionFormat.MIR_BinaryAcc_format,
                      (none | OPT_InstructionFormat.MIR_BinaryAcc_traits),
                      0, 1, 1,
                      OPT_PhysicalDefUse.maskCF_OF,
                      OPT_PhysicalDefUse.maskCF),
     new OPT_Operator((char)(130 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_RCR
                      OPT_InstructionFormat.MIR_BinaryAcc_format,
                      (none | OPT_InstructionFormat.MIR_BinaryAcc_traits),
                      0, 1, 1,
                      OPT_PhysicalDefUse.maskCF_OF,
                      OPT_PhysicalDefUse.maskCF),
     new OPT_Operator((char)(131 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_ROL
                      OPT_InstructionFormat.MIR_BinaryAcc_format,
                      (none | OPT_InstructionFormat.MIR_BinaryAcc_traits),
                      0, 1, 1,
                      OPT_PhysicalDefUse.maskCF_OF,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(132 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_ROR
                      OPT_InstructionFormat.MIR_BinaryAcc_format,
                      (none | OPT_InstructionFormat.MIR_BinaryAcc_traits),
                      0, 1, 1,
                      OPT_PhysicalDefUse.maskCF_OF,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(133 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_RET
                      OPT_InstructionFormat.MIR_Return_format,
                      (ret | OPT_InstructionFormat.MIR_Return_traits),
                      0, 0, 3,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(134 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_SAL
                      OPT_InstructionFormat.MIR_BinaryAcc_format,
                      (none | OPT_InstructionFormat.MIR_BinaryAcc_traits),
                      0, 1, 1,
                      OPT_PhysicalDefUse.maskAF_CF_OF_PF_SF_ZF,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(135 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_SAR
                      OPT_InstructionFormat.MIR_BinaryAcc_format,
                      (none | OPT_InstructionFormat.MIR_BinaryAcc_traits),
                      0, 1, 1,
                      OPT_PhysicalDefUse.maskAF_CF_OF_PF_SF_ZF,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(136 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_SHL
                      OPT_InstructionFormat.MIR_BinaryAcc_format,
                      (none | OPT_InstructionFormat.MIR_BinaryAcc_traits),
                      0, 1, 1,
                      OPT_PhysicalDefUse.maskAF_CF_OF_PF_SF_ZF,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(137 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_SHR
                      OPT_InstructionFormat.MIR_BinaryAcc_format,
                      (none | OPT_InstructionFormat.MIR_BinaryAcc_traits),
                      0, 1, 1,
                      OPT_PhysicalDefUse.maskAF_CF_OF_PF_SF_ZF,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(138 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_SBB
                      OPT_InstructionFormat.MIR_BinaryAcc_format,
                      (none | OPT_InstructionFormat.MIR_BinaryAcc_traits),
                      0, 1, 1,
                      OPT_PhysicalDefUse.maskAF_CF_OF_PF_SF_ZF,
                      OPT_PhysicalDefUse.maskCF),
     new OPT_Operator((char)(139 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_SET__B
                      OPT_InstructionFormat.MIR_Set_format,
                      (none | OPT_InstructionFormat.MIR_Set_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.maskAF_CF_OF_PF_SF_ZF),
     new OPT_Operator((char)(140 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_SHLD
                      OPT_InstructionFormat.MIR_DoubleShift_format,
                      (none | OPT_InstructionFormat.MIR_DoubleShift_traits),
                      0, 1, 2,
                      OPT_PhysicalDefUse.maskAF_CF_OF_PF_SF_ZF,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(141 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_SHRD
                      OPT_InstructionFormat.MIR_DoubleShift_format,
                      (none | OPT_InstructionFormat.MIR_DoubleShift_traits),
                      0, 1, 2,
                      OPT_PhysicalDefUse.maskAF_CF_OF_PF_SF_ZF,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(142 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_SUB
                      OPT_InstructionFormat.MIR_BinaryAcc_format,
                      (none | OPT_InstructionFormat.MIR_BinaryAcc_traits),
                      0, 1, 1,
                      OPT_PhysicalDefUse.maskAF_CF_OF_PF_SF_ZF,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(143 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_TEST
                      OPT_InstructionFormat.MIR_Test_format,
                      (none | OPT_InstructionFormat.MIR_Test_traits),
                      0, 0, 2,
                      OPT_PhysicalDefUse.maskAF_CF_OF_PF_SF_ZF,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(144 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_XOR
                      OPT_InstructionFormat.MIR_BinaryAcc_format,
                      (none | OPT_InstructionFormat.MIR_BinaryAcc_traits),
                      0, 1, 1,
                      OPT_PhysicalDefUse.maskAF_CF_OF_PF_SF_ZF,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(145 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_RDTSC
                      OPT_InstructionFormat.MIR_RDTSC_format,
                      (none | OPT_InstructionFormat.MIR_RDTSC_traits),
                      2, 0, 0,
                      OPT_PhysicalDefUse.maskCF_OF,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(146 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_ADDSS
                      OPT_InstructionFormat.MIR_BinaryAcc_format,
                      (none | OPT_InstructionFormat.MIR_BinaryAcc_traits),
                      0, 1, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(147 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_SUBSS
                      OPT_InstructionFormat.MIR_BinaryAcc_format,
                      (none | OPT_InstructionFormat.MIR_BinaryAcc_traits),
                      0, 1, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(148 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_MULSS
                      OPT_InstructionFormat.MIR_BinaryAcc_format,
                      (none | OPT_InstructionFormat.MIR_BinaryAcc_traits),
                      0, 1, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(149 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_DIVSS
                      OPT_InstructionFormat.MIR_BinaryAcc_format,
                      (none | OPT_InstructionFormat.MIR_BinaryAcc_traits),
                      0, 1, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(150 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_ADDSD
                      OPT_InstructionFormat.MIR_BinaryAcc_format,
                      (none | OPT_InstructionFormat.MIR_BinaryAcc_traits),
                      0, 1, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(151 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_SUBSD
                      OPT_InstructionFormat.MIR_BinaryAcc_format,
                      (none | OPT_InstructionFormat.MIR_BinaryAcc_traits),
                      0, 1, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(152 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_MULSD
                      OPT_InstructionFormat.MIR_BinaryAcc_format,
                      (none | OPT_InstructionFormat.MIR_BinaryAcc_traits),
                      0, 1, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(153 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_DIVSD
                      OPT_InstructionFormat.MIR_BinaryAcc_format,
                      (none | OPT_InstructionFormat.MIR_BinaryAcc_traits),
                      0, 1, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(154 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_XORPS
                      OPT_InstructionFormat.MIR_BinaryAcc_format,
                      (none | OPT_InstructionFormat.MIR_BinaryAcc_traits),
                      0, 1, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(155 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_XORPD
                      OPT_InstructionFormat.MIR_BinaryAcc_format,
                      (none | OPT_InstructionFormat.MIR_BinaryAcc_traits),
                      0, 1, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(156 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_UCOMISS
                      OPT_InstructionFormat.MIR_Compare_format,
                      (compare | OPT_InstructionFormat.MIR_Compare_traits),
                      0, 0, 2,
                      OPT_PhysicalDefUse.maskCF_PF_ZF,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(157 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_UCOMISD
                      OPT_InstructionFormat.MIR_Compare_format,
                      (compare | OPT_InstructionFormat.MIR_Compare_traits),
                      0, 0, 2,
                      OPT_PhysicalDefUse.maskCF_PF_ZF,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(158 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_CMPEQSS
                      OPT_InstructionFormat.MIR_BinaryAcc_format,
                      (none | OPT_InstructionFormat.MIR_BinaryAcc_traits),
                      0, 1, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(159 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_CMPLTSS
                      OPT_InstructionFormat.MIR_BinaryAcc_format,
                      (none | OPT_InstructionFormat.MIR_BinaryAcc_traits),
                      0, 1, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(160 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_CMPLESS
                      OPT_InstructionFormat.MIR_BinaryAcc_format,
                      (none | OPT_InstructionFormat.MIR_BinaryAcc_traits),
                      0, 1, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(161 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_CMPUNORDSS
                      OPT_InstructionFormat.MIR_BinaryAcc_format,
                      (none | OPT_InstructionFormat.MIR_BinaryAcc_traits),
                      0, 1, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(162 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_CMPNESS
                      OPT_InstructionFormat.MIR_BinaryAcc_format,
                      (none | OPT_InstructionFormat.MIR_BinaryAcc_traits),
                      0, 1, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(163 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_CMPNLTSS
                      OPT_InstructionFormat.MIR_BinaryAcc_format,
                      (none | OPT_InstructionFormat.MIR_BinaryAcc_traits),
                      0, 1, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(164 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_CMPNLESS
                      OPT_InstructionFormat.MIR_BinaryAcc_format,
                      (none | OPT_InstructionFormat.MIR_BinaryAcc_traits),
                      0, 1, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(165 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_CMPORDSS
                      OPT_InstructionFormat.MIR_BinaryAcc_format,
                      (none | OPT_InstructionFormat.MIR_BinaryAcc_traits),
                      0, 1, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(166 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_CMPEQSD
                      OPT_InstructionFormat.MIR_BinaryAcc_format,
                      (none | OPT_InstructionFormat.MIR_BinaryAcc_traits),
                      0, 1, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(167 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_CMPLTSD
                      OPT_InstructionFormat.MIR_BinaryAcc_format,
                      (none | OPT_InstructionFormat.MIR_BinaryAcc_traits),
                      0, 1, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(168 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_CMPLESD
                      OPT_InstructionFormat.MIR_BinaryAcc_format,
                      (none | OPT_InstructionFormat.MIR_BinaryAcc_traits),
                      0, 1, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(169 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_CMPUNORDSD
                      OPT_InstructionFormat.MIR_BinaryAcc_format,
                      (none | OPT_InstructionFormat.MIR_BinaryAcc_traits),
                      0, 1, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(170 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_CMPNESD
                      OPT_InstructionFormat.MIR_BinaryAcc_format,
                      (none | OPT_InstructionFormat.MIR_BinaryAcc_traits),
                      0, 1, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(171 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_CMPNLTSD
                      OPT_InstructionFormat.MIR_BinaryAcc_format,
                      (none | OPT_InstructionFormat.MIR_BinaryAcc_traits),
                      0, 1, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(172 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_CMPNLESD
                      OPT_InstructionFormat.MIR_BinaryAcc_format,
                      (none | OPT_InstructionFormat.MIR_BinaryAcc_traits),
                      0, 1, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(173 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_CMPORDSD
                      OPT_InstructionFormat.MIR_BinaryAcc_format,
                      (none | OPT_InstructionFormat.MIR_BinaryAcc_traits),
                      0, 1, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(174 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_MOVSS
                      OPT_InstructionFormat.MIR_Move_format,
                      (move | OPT_InstructionFormat.MIR_Move_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(175 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_MOVSD
                      OPT_InstructionFormat.MIR_Move_format,
                      (move | OPT_InstructionFormat.MIR_Move_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(176 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_MOVD
                      OPT_InstructionFormat.MIR_Move_format,
                      (move | OPT_InstructionFormat.MIR_Move_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(177 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_MOVQ
                      OPT_InstructionFormat.MIR_Move_format,
                      (move | OPT_InstructionFormat.MIR_Move_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(178 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_CVTSI2SS
                      OPT_InstructionFormat.MIR_Unary_format,
                      (move | OPT_InstructionFormat.MIR_Unary_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(179 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_CVTSS2SD
                      OPT_InstructionFormat.MIR_Unary_format,
                      (move | OPT_InstructionFormat.MIR_Unary_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(180 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_CVTSS2SI
                      OPT_InstructionFormat.MIR_Unary_format,
                      (move | OPT_InstructionFormat.MIR_Unary_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(181 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_CVTTSS2SI
                      OPT_InstructionFormat.MIR_Unary_format,
                      (move | OPT_InstructionFormat.MIR_Unary_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(182 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_CVTSI2SD
                      OPT_InstructionFormat.MIR_Unary_format,
                      (move | OPT_InstructionFormat.MIR_Unary_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(183 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_CVTSD2SS
                      OPT_InstructionFormat.MIR_Unary_format,
                      (move | OPT_InstructionFormat.MIR_Unary_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(184 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_CVTSD2SI
                      OPT_InstructionFormat.MIR_Unary_format,
                      (move | OPT_InstructionFormat.MIR_Unary_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(185 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //IA32_CVTTSD2SI
                      OPT_InstructionFormat.MIR_Unary_format,
                      (move | OPT_InstructionFormat.MIR_Unary_traits),
                      1, 0, 1,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     new OPT_Operator((char)(186 + OPT_Operators.ARCH_INDEPENDENT_END_opcode),  //MIR_END
                      OPT_InstructionFormat.Unassigned_format,
                      (none),
                      0,0,0,
                      OPT_PhysicalDefUse.mask,
                      OPT_PhysicalDefUse.mask),
     null };

  // For HIR/LIR
  private OPT_Operator(char opcode, byte format, int traits,
                       int numDefs, int numDefUses, int numUses,
                       int iDefs, int iUses) {
    this.opcode       = opcode;
    this.format       = format;
    this.traits       = traits;
    this.numberDefs   = numDefs;
    this.numberDefUses= numDefUses;
    this.numberUses   = numUses;
    this.implicitDefs = iDefs;
    this.implicitUses = iUses;
  }

}
