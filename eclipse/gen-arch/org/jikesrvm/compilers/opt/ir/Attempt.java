
/*
 * THIS FILE IS MACHINE_GENERATED. DO NOT EDIT.
 * See InstructionFormats.template, InstructionFormatList.dat,
 * OperatorList.dat, etc.
 */

package org.jikesrvm.compilers.opt.ir;

import org.jikesrvm.VM_Configuration;
import org.jikesrvm.compilers.opt.ir.ia32.OPT_IA32ConditionOperand; //NOPMD


/**
 * The Attempt InstructionFormat class.
 *
 * The header comment for {@link OPT_Instruction} contains
 * an explanation of the role of InstructionFormats in the
 * opt compiler's IR.
 */
@SuppressWarnings("unused")  // Machine generated code is never 100% clean
public final class Attempt extends OPT_InstructionFormat {
  /**
   * InstructionFormat identification method for Attempt.
   * @param i an instruction
   * @return <code>true</code> if the InstructionFormat of the argument
   *         instruction is Attempt or <code>false</code>
   *         if it is not.
   */
  public static boolean conforms(OPT_Instruction i) {
    return conforms(i.operator);
  }
  /**
   * InstructionFormat identification method for Attempt.
   * @param o an instruction
   * @return <code>true</code> if the InstructionFormat of the argument
   *         operator is Attempt or <code>false</code>
   *         if it is not.
   */
  public static boolean conforms(OPT_Operator o) {
    return o.format == Attempt_format;
  }

  /**
   * Get the operand called Result from the
   * argument instruction. Note that the returned operand
   * will still point to its containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Result
   */
  public static OPT_RegisterOperand getResult(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Attempt");
    return (OPT_RegisterOperand) i.getOperand(0);
  }
  /**
   * Get the operand called Result from the argument
   * instruction clearing its instruction pointer. The returned
   * operand will not point to any containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Result
   */
  public static OPT_RegisterOperand getClearResult(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Attempt");
    return (OPT_RegisterOperand) i.getClearOperand(0);
  }
  /**
   * Set the operand called Result in the argument
   * instruction to the argument operand. The operand will
   * now point to the argument instruction as its containing
   * instruction.
   * @param i the instruction in which to store the operand
   * @param Result the operand to store
   */
  public static void setResult(OPT_Instruction i, OPT_RegisterOperand Result) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Attempt");
    i.putOperand(0, Result);
  }
  /**
   * Return the index of the operand called Result
   * in the argument instruction.
   * @param i the instruction to access.
   * @return the index of the operand called Result
   *         in the argument instruction
   */
  public static int indexOfResult(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Attempt");
    return 0;
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
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Attempt");
    return i.getOperand(0) != null;
  }

  /**
   * Get the operand called Address from the
   * argument instruction. Note that the returned operand
   * will still point to its containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Address
   */
  public static OPT_Operand getAddress(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Attempt");
    return (OPT_Operand) i.getOperand(1);
  }
  /**
   * Get the operand called Address from the argument
   * instruction clearing its instruction pointer. The returned
   * operand will not point to any containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Address
   */
  public static OPT_Operand getClearAddress(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Attempt");
    return (OPT_Operand) i.getClearOperand(1);
  }
  /**
   * Set the operand called Address in the argument
   * instruction to the argument operand. The operand will
   * now point to the argument instruction as its containing
   * instruction.
   * @param i the instruction in which to store the operand
   * @param Address the operand to store
   */
  public static void setAddress(OPT_Instruction i, OPT_Operand Address) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Attempt");
    i.putOperand(1, Address);
  }
  /**
   * Return the index of the operand called Address
   * in the argument instruction.
   * @param i the instruction to access.
   * @return the index of the operand called Address
   *         in the argument instruction
   */
  public static int indexOfAddress(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Attempt");
    return 1;
  }
  /**
   * Does the argument instruction have a non-null
   * operand named Address?
   * @param i the instruction to access.
   * @return <code>true</code> if the instruction has an non-null
   *         operand named Address or <code>false</code>
   *         if it does not.
   */
  public static boolean hasAddress(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Attempt");
    return i.getOperand(1) != null;
  }

  /**
   * Get the operand called Offset from the
   * argument instruction. Note that the returned operand
   * will still point to its containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Offset
   */
  public static OPT_Operand getOffset(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Attempt");
    return (OPT_Operand) i.getOperand(2);
  }
  /**
   * Get the operand called Offset from the argument
   * instruction clearing its instruction pointer. The returned
   * operand will not point to any containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Offset
   */
  public static OPT_Operand getClearOffset(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Attempt");
    return (OPT_Operand) i.getClearOperand(2);
  }
  /**
   * Set the operand called Offset in the argument
   * instruction to the argument operand. The operand will
   * now point to the argument instruction as its containing
   * instruction.
   * @param i the instruction in which to store the operand
   * @param Offset the operand to store
   */
  public static void setOffset(OPT_Instruction i, OPT_Operand Offset) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Attempt");
    i.putOperand(2, Offset);
  }
  /**
   * Return the index of the operand called Offset
   * in the argument instruction.
   * @param i the instruction to access.
   * @return the index of the operand called Offset
   *         in the argument instruction
   */
  public static int indexOfOffset(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Attempt");
    return 2;
  }
  /**
   * Does the argument instruction have a non-null
   * operand named Offset?
   * @param i the instruction to access.
   * @return <code>true</code> if the instruction has an non-null
   *         operand named Offset or <code>false</code>
   *         if it does not.
   */
  public static boolean hasOffset(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Attempt");
    return i.getOperand(2) != null;
  }

  /**
   * Get the operand called OldValue from the
   * argument instruction. Note that the returned operand
   * will still point to its containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called OldValue
   */
  public static OPT_Operand getOldValue(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Attempt");
    return (OPT_Operand) i.getOperand(3);
  }
  /**
   * Get the operand called OldValue from the argument
   * instruction clearing its instruction pointer. The returned
   * operand will not point to any containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called OldValue
   */
  public static OPT_Operand getClearOldValue(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Attempt");
    return (OPT_Operand) i.getClearOperand(3);
  }
  /**
   * Set the operand called OldValue in the argument
   * instruction to the argument operand. The operand will
   * now point to the argument instruction as its containing
   * instruction.
   * @param i the instruction in which to store the operand
   * @param OldValue the operand to store
   */
  public static void setOldValue(OPT_Instruction i, OPT_Operand OldValue) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Attempt");
    i.putOperand(3, OldValue);
  }
  /**
   * Return the index of the operand called OldValue
   * in the argument instruction.
   * @param i the instruction to access.
   * @return the index of the operand called OldValue
   *         in the argument instruction
   */
  public static int indexOfOldValue(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Attempt");
    return 3;
  }
  /**
   * Does the argument instruction have a non-null
   * operand named OldValue?
   * @param i the instruction to access.
   * @return <code>true</code> if the instruction has an non-null
   *         operand named OldValue or <code>false</code>
   *         if it does not.
   */
  public static boolean hasOldValue(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Attempt");
    return i.getOperand(3) != null;
  }

  /**
   * Get the operand called NewValue from the
   * argument instruction. Note that the returned operand
   * will still point to its containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called NewValue
   */
  public static OPT_Operand getNewValue(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Attempt");
    return (OPT_Operand) i.getOperand(4);
  }
  /**
   * Get the operand called NewValue from the argument
   * instruction clearing its instruction pointer. The returned
   * operand will not point to any containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called NewValue
   */
  public static OPT_Operand getClearNewValue(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Attempt");
    return (OPT_Operand) i.getClearOperand(4);
  }
  /**
   * Set the operand called NewValue in the argument
   * instruction to the argument operand. The operand will
   * now point to the argument instruction as its containing
   * instruction.
   * @param i the instruction in which to store the operand
   * @param NewValue the operand to store
   */
  public static void setNewValue(OPT_Instruction i, OPT_Operand NewValue) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Attempt");
    i.putOperand(4, NewValue);
  }
  /**
   * Return the index of the operand called NewValue
   * in the argument instruction.
   * @param i the instruction to access.
   * @return the index of the operand called NewValue
   *         in the argument instruction
   */
  public static int indexOfNewValue(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Attempt");
    return 4;
  }
  /**
   * Does the argument instruction have a non-null
   * operand named NewValue?
   * @param i the instruction to access.
   * @return <code>true</code> if the instruction has an non-null
   *         operand named NewValue or <code>false</code>
   *         if it does not.
   */
  public static boolean hasNewValue(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Attempt");
    return i.getOperand(4) != null;
  }

  /**
   * Get the operand called Location from the
   * argument instruction. Note that the returned operand
   * will still point to its containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Location
   */
  public static OPT_LocationOperand getLocation(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Attempt");
    return (OPT_LocationOperand) i.getOperand(5);
  }
  /**
   * Get the operand called Location from the argument
   * instruction clearing its instruction pointer. The returned
   * operand will not point to any containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Location
   */
  public static OPT_LocationOperand getClearLocation(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Attempt");
    return (OPT_LocationOperand) i.getClearOperand(5);
  }
  /**
   * Set the operand called Location in the argument
   * instruction to the argument operand. The operand will
   * now point to the argument instruction as its containing
   * instruction.
   * @param i the instruction in which to store the operand
   * @param Location the operand to store
   */
  public static void setLocation(OPT_Instruction i, OPT_LocationOperand Location) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Attempt");
    i.putOperand(5, Location);
  }
  /**
   * Return the index of the operand called Location
   * in the argument instruction.
   * @param i the instruction to access.
   * @return the index of the operand called Location
   *         in the argument instruction
   */
  public static int indexOfLocation(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Attempt");
    return 5;
  }
  /**
   * Does the argument instruction have a non-null
   * operand named Location?
   * @param i the instruction to access.
   * @return <code>true</code> if the instruction has an non-null
   *         operand named Location or <code>false</code>
   *         if it does not.
   */
  public static boolean hasLocation(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Attempt");
    return i.getOperand(5) != null;
  }

  /**
   * Get the operand called Guard from the
   * argument instruction. Note that the returned operand
   * will still point to its containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Guard
   */
  public static OPT_Operand getGuard(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Attempt");
    return (OPT_Operand) i.getOperand(6);
  }
  /**
   * Get the operand called Guard from the argument
   * instruction clearing its instruction pointer. The returned
   * operand will not point to any containing instruction.
   * @param i the instruction to fetch the operand from
   * @return the operand called Guard
   */
  public static OPT_Operand getClearGuard(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Attempt");
    return (OPT_Operand) i.getClearOperand(6);
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
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Attempt");
    i.putOperand(6, Guard);
  }
  /**
   * Return the index of the operand called Guard
   * in the argument instruction.
   * @param i the instruction to access.
   * @return the index of the operand called Guard
   *         in the argument instruction
   */
  public static int indexOfGuard(OPT_Instruction i) {
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Attempt");
    return 6;
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
    if (VM_Configuration.ExtremeAssertions && !conforms(i)) fail(i, "Attempt");
    return i.getOperand(6) != null;
  }


  /**
   * Create an instruction of the Attempt instruction format.
   * @param o the instruction's operator
   * @param Result the instruction's Result operand
   * @param Address the instruction's Address operand
   * @param Offset the instruction's Offset operand
   * @param OldValue the instruction's OldValue operand
   * @param NewValue the instruction's NewValue operand
   * @param Location the instruction's Location operand
   * @param Guard the instruction's Guard operand
   * @return the newly created Attempt instruction
   */
  public static OPT_Instruction create(OPT_Operator o
                   , OPT_RegisterOperand Result
                   , OPT_Operand Address
                   , OPT_Operand Offset
                   , OPT_Operand OldValue
                   , OPT_Operand NewValue
                   , OPT_LocationOperand Location
                   , OPT_Operand Guard
                )
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(o)) fail(o, "Attempt");
    OPT_Instruction i = new OPT_Instruction(o, 7);
    i.putOperand(0, Result);
    i.putOperand(1, Address);
    i.putOperand(2, Offset);
    i.putOperand(3, OldValue);
    i.putOperand(4, NewValue);
    i.putOperand(5, Location);
    i.putOperand(6, Guard);
    return i;
  }

  /**
   * Mutate the argument instruction into an instruction of the
   * Attempt instruction format having the specified
   * operator and operands.
   * @param i the instruction to mutate
   * @param o the instruction's operator
   * @param Result the instruction's Result operand
   * @param Address the instruction's Address operand
   * @param Offset the instruction's Offset operand
   * @param OldValue the instruction's OldValue operand
   * @param NewValue the instruction's NewValue operand
   * @param Location the instruction's Location operand
   * @param Guard the instruction's Guard operand
   * @return the mutated instruction
   */
  public static OPT_Instruction mutate(OPT_Instruction i, OPT_Operator o
                   , OPT_RegisterOperand Result
                   , OPT_Operand Address
                   , OPT_Operand Offset
                   , OPT_Operand OldValue
                   , OPT_Operand NewValue
                   , OPT_LocationOperand Location
                   , OPT_Operand Guard
                )
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(o)) fail(o, "Attempt");
    i.resizeNumberOfOperands(7);

    i.operator = o;
    i.putOperand(0, Result);
    i.putOperand(1, Address);
    i.putOperand(2, Offset);
    i.putOperand(3, OldValue);
    i.putOperand(4, NewValue);
    i.putOperand(5, Location);
    i.putOperand(6, Guard);
    return i;
  }
  /**
   * Create an instruction of the Attempt instruction format.
   * @param o the instruction's operator
   * @param Result the instruction's Result operand
   * @param Address the instruction's Address operand
   * @param Offset the instruction's Offset operand
   * @param OldValue the instruction's OldValue operand
   * @param NewValue the instruction's NewValue operand
   * @param Location the instruction's Location operand
   * @return the newly created Attempt instruction
   */
  public static OPT_Instruction create(OPT_Operator o
                   , OPT_RegisterOperand Result
                   , OPT_Operand Address
                   , OPT_Operand Offset
                   , OPT_Operand OldValue
                   , OPT_Operand NewValue
                   , OPT_LocationOperand Location
                )
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(o)) fail(o, "Attempt");
    OPT_Instruction i = new OPT_Instruction(o, 7);
    i.putOperand(0, Result);
    i.putOperand(1, Address);
    i.putOperand(2, Offset);
    i.putOperand(3, OldValue);
    i.putOperand(4, NewValue);
    i.putOperand(5, Location);
    return i;
  }

  /**
   * Mutate the argument instruction into an instruction of the
   * Attempt instruction format having the specified
   * operator and operands.
   * @param i the instruction to mutate
   * @param o the instruction's operator
   * @param Result the instruction's Result operand
   * @param Address the instruction's Address operand
   * @param Offset the instruction's Offset operand
   * @param OldValue the instruction's OldValue operand
   * @param NewValue the instruction's NewValue operand
   * @param Location the instruction's Location operand
   * @return the mutated instruction
   */
  public static OPT_Instruction mutate(OPT_Instruction i, OPT_Operator o
                   , OPT_RegisterOperand Result
                   , OPT_Operand Address
                   , OPT_Operand Offset
                   , OPT_Operand OldValue
                   , OPT_Operand NewValue
                   , OPT_LocationOperand Location
                )
  {
    if (VM_Configuration.ExtremeAssertions && !conforms(o)) fail(o, "Attempt");
    i.resizeNumberOfOperands(7);

    i.operator = o;
    i.putOperand(0, Result);
    i.putOperand(1, Address);
    i.putOperand(2, Offset);
    i.putOperand(3, OldValue);
    i.putOperand(4, NewValue);
    i.putOperand(5, Location);
    i.putOperand(6, null);
    return i;
  }
}

