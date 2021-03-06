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

import java.util.Enumeration;
import org.jikesrvm.compilers.opt.OPT_BitSetMapping;
import org.jikesrvm.compilers.opt.OPT_ReverseEnumerator;

/**
 * This class represents a set of OPT_Registers corresponding to the
 * physical register set. This class holds the architecture-independent
 * functionality
 *
 * <P> Implementation Note: Each register has an integer field
 * OPT_Register.number.  This class must number the physical registers so
 * that get(n) returns an OPT_Register r with r.number = n!
 */
public abstract class OPT_GenericPhysicalRegisterSet implements OPT_BitSetMapping {

  /**
   * Return the total number of physical registers.
   */
  public abstract int getNumberOfPhysicalRegisters();

  /**
   * @return the FP register
   */
  public abstract OPT_Register getFP();

  /**
   * @return the processor register
   */
  public abstract OPT_Register getPR();

  /**
   * @return the nth physical GPR
   */
  public abstract OPT_Register getGPR(int n);

  /**
   * @return the first GPR return
   */
  public abstract OPT_Register getFirstReturnGPR();

  /**
   * @return the nth physical FPR
   */
  public abstract OPT_Register getFPR(int n);

  /**
   * @return the nth physical register in the pool.
   */
  public abstract OPT_Register get(int n);

  /**
   * Enumerate all the physical registers in this set.
   */
  public abstract Enumeration<OPT_Register> enumerateAll();

  /**
   * Enumerate all the GPRs in this set.
   */
  public abstract Enumeration<OPT_Register> enumerateGPRs();

  /**
   * Enumerate all the volatile GPRs in this set.
   */
  public abstract Enumeration<OPT_Register> enumerateVolatileGPRs();

  /**
   * Enumerate all the nonvolatile GPRs in this set.
   */
  public abstract Enumeration<OPT_Register> enumerateNonvolatileGPRs();

  /**
   * Enumerate all the volatile FPRs in this set.
   */
  public abstract Enumeration<OPT_Register> enumerateVolatileFPRs();

  /**
   * Enumerate all the nonvolatile FPRs in this set.
   */
  public abstract Enumeration<OPT_Register> enumerateNonvolatileFPRs();

  /**
   * Enumerate all the volatile physical registers
   */
  public abstract Enumeration<OPT_Register> enumerateVolatiles();

  /**
   * Enumerate all the nonvolatile GPRs in this set, backwards
   */
  public Enumeration<OPT_Register> enumerateNonvolatileGPRsBackwards() {
    return new OPT_ReverseEnumerator<OPT_Register>(enumerateNonvolatileGPRs());
  }

  /**
   * Enumerate all the nonvolatile FPRs in this set, backwards.
   */
  public Enumeration<OPT_Register> enumerateNonvolatileFPRsBackwards() {
    return new OPT_ReverseEnumerator<OPT_Register>(enumerateNonvolatileFPRs());
  }

  /**
   * Implementation of the OPT_BitSetMapping interface.
   */
  public final Object getMappedObject(int n) {
    return get(n);
  }

  /**
   * Implementation of the OPT_BitSetMapping interface.
   */
  public final int getMappedIndex(Object o) {
    OPT_Register r = (OPT_Register) o;
    return r.number;
  }

  /**
   * Implementation of the OPT_BitSetMapping interface.
   */
  public final int getMappingSize() {
    return getNumberOfPhysicalRegisters();
  }
}
