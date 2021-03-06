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
package org.jikesrvm.compilers.opt.ir;

import org.jikesrvm.classloader.VM_TypeReference;

/**
 * This operand represents the null constant.
 *
 * @see OPT_Operand
 */
public final class OPT_NullConstantOperand extends OPT_ConstantOperand {

  /**
   * Return a new operand that is semantically equivalent to <code>this</code>.
   *
   * @return a copy of <code>this</code>
   */
  public OPT_Operand copy() {
    return new OPT_NullConstantOperand();
  }

  /**
   * Return the {@link VM_TypeReference} of the value represented by the operand.
   *
   * @return VM_TypeReference.NULL_TYPE
   */
  public VM_TypeReference getType() {
    return VM_TypeReference.NULL_TYPE;
  }

  /**
   * Does the operand represent a value of the reference data type?
   *
   * @return <code>true</code>
   */
  public boolean isRef() {
    return true;
  }

  /**
   * Does the operand definitely represent <code>null</code>?
   *
   * @return <code>true</code>
   */
  public boolean isDefinitelyNull() {
    return true;
  }

  /**
   * Are two operands semantically equivalent?
   *
   * @param op other operand
   * @return   <code>true</code> if <code>this</code> and <code>op</code>
   *           are semantically equivalent or <code>false</code>
   *           if they are not.
   */
  public boolean similar(OPT_Operand op) {
    return op instanceof OPT_NullConstantOperand;
  }

  /**
   * Returns the string representation of this operand.
   *
   * @return a string representation of this operand.
   */
  public String toString() {
    return "<null>";
  }
}
