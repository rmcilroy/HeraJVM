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

/**
 *
 * @see OPT_Operand
 */
public final class OPT_BranchProfileOperand extends OPT_Operand {
  public float takenProbability;

  public static final float ALWAYS = 1f;
  public static final float LIKELY = .99f;
  public static final float UNLIKELY = 1f - LIKELY;
  public static final float NEVER = 1f - ALWAYS;

  public OPT_BranchProfileOperand(float takenProbability) {
    this.takenProbability = takenProbability;
  }

  public OPT_BranchProfileOperand() {
    this.takenProbability = 0.5f;
  }

  public static OPT_BranchProfileOperand always() {
    return new OPT_BranchProfileOperand(ALWAYS);
  }

  public static OPT_BranchProfileOperand likely() {
    return new OPT_BranchProfileOperand(LIKELY);
  }

  public static OPT_BranchProfileOperand unlikely() {
    return new OPT_BranchProfileOperand(UNLIKELY);
  }

  public static OPT_BranchProfileOperand never() {
    return new OPT_BranchProfileOperand(NEVER);
  }

  /**
   * Returns a copy of this branch operand.
   *
   * @return a copy of this operand
   */
  public OPT_Operand copy() {
    return new OPT_BranchProfileOperand(takenProbability);
  }

  /**
   * Flip the probability (p = 1 - p)
   */
  public OPT_BranchProfileOperand flip() {
    takenProbability = 1f - takenProbability;
    return this;
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
    return (op instanceof OPT_BranchProfileOperand) &&
           (takenProbability == ((OPT_BranchProfileOperand) op).takenProbability);
  }

  /**
   * Returns the string representation of this operand.
   *
   * @return a string representation of this operand.
   */
  public String toString() {
    return "Probability: " + takenProbability;
  }

}




