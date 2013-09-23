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
package org.jikesrvm.cellspu;

import org.jikesrvm.VM_Constants;

/**
 * Registers used by baseline compiler code.
 */
public interface VM_BaselineConstants extends VM_Constants, VM_ArchConstants {

  // Dedicated registers
  int FP = FRAME_POINTER;
  int JTOC = JTOC_POINTER;
  
  // TODO - We can use more registers if necessary
  
  // Scratch general purpose registers
  int S0 = FIRST_SCRATCH_GPR;
  int S1 = FIRST_SCRATCH_GPR + 1;
  int S2 = FIRST_SCRATCH_GPR + 2;
  int S3 = FIRST_SCRATCH_GPR + 3;
  int S4 = FIRST_SCRATCH_GPR + 4;
  int S5 = FIRST_SCRATCH_GPR + 5;
  int S6 = FIRST_SCRATCH_GPR + 6;
  int S7 = FIRST_SCRATCH_GPR + 7;
  int S8 = FIRST_SCRATCH_GPR + 8;
  int S9 = FIRST_SCRATCH_GPR + 9;

  // Temporary general purpose registers
  int T0 = FIRST_VOLATILE_GPR;
  int T1 = FIRST_VOLATILE_GPR + 1;
  int T2 = FIRST_VOLATILE_GPR + 2;
  int T3 = FIRST_VOLATILE_GPR + 3;
  int T4 = FIRST_VOLATILE_GPR + 4;
  int T5 = FIRST_VOLATILE_GPR + 5;
  int T6 = FIRST_VOLATILE_GPR + 6;
  int T7 = FIRST_VOLATILE_GPR + 7;

  int VOLATILE_GPRS = LAST_VOLATILE_GPR - FIRST_VOLATILE_GPR + 1;
  int MIN_PARAM_REGISTERS = VOLATILE_GPRS;

  int FIRST_FIXED_LOCAL_REGISTER = FIRST_NONVOLATILE_GPR;
  int LAST_FIXED_LOCAL_REGISTER = LAST_NONVOLATILE_GPR;
  int LAST_FIXED_STACK_REGISTER = LAST_NONVOLATILE_GPR;
  
  int FIRST_FIXED_LONG_LOCAL_REGISTER = FIRST_LONG_NONVOLATILE_GPR;
  int LAST_FIXED_LONG_LOCAL_REGISTER = LAST_LONG_NONVOLATILE_GPR;
  int LAST_FIXED_LONG_STACK_REGISTER = LAST_LONG_NONVOLATILE_GPR;
}
