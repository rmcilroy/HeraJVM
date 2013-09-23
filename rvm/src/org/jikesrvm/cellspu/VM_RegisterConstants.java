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

import org.jikesrvm.VM_SizeConstants;

/**
 * Register Usage Conventions for Cell SPU
 */
public interface VM_RegisterConstants extends VM_SizeConstants {
  // Machine instructions.
  //
  int LG_INSTRUCTION_WIDTH = 2;                      // log2 of instruction width in bytes, cell-spu
  int INSTRUCTION_WIDTH = 1 << LG_INSTRUCTION_WIDTH; // instruction width in bytes, cell-spu

  // OS register convention (for mapping parameters in JNI calls)
  // These constants encode conventions for AIX, OSX, and Linux 
  // (at least roughly for PowerPC since non of these os's run on the Cell-SPU).
  int FIRST_OS_PARAMETER_GPR = 3;
  int LAST_OS_PARAMETER_GPR = 10;

  // Jikes RVM's general purpose register usage (4x32 bits words wide - usually just use preferred word).
  //
  int REGISTER_ZERO = 0;

  int FRAME_POINTER = 1; 
  int FIRST_VOLATILE_GPR = FIRST_OS_PARAMETER_GPR;
  //                                            ...
  int LAST_VOLATILE_GPR = 20;
  int FIRST_SCRATCH_GPR = LAST_VOLATILE_GPR + 1;
  int LAST_SCRATCH_GPR = 30;

  int FIRST_RVM_RESERVED_NV_GPR = 31;

  int JTOC_POINTER = 2;
  int PROCESSOR_REGISTER = FIRST_RVM_RESERVED_NV_GPR + 1;
  int TRAP_ENTRY_REG = PROCESSOR_REGISTER + 1;   // entry point for traps
  int STACK_TOP_TEMP = TRAP_ENTRY_REG + 1;   // Temporary register for storing top 4 words of stack
  int LINK_REG      = STACK_TOP_TEMP + 1;   // Register for storing return link address during function calls
  
  int LAST_RVM_RESERVED_NV_GPR = LINK_REG;
  int FIRST_NONVOLATILE_GPR = LAST_RVM_RESERVED_NV_GPR + 1;
  //                                            ...
  int LAST_NONVOLATILE_GPR = 99;

  int FIRST_LONG_NONVOLATILE_GPR = LAST_NONVOLATILE_GPR + 1;
  //                                            ...
  int LAST_LONG_NONVOLATILE_GPR = 127;
  
  int NUM_GPRS = 128;

  int NUM_NONVOLATILE_GPRS = LAST_NONVOLATILE_GPR - FIRST_NONVOLATILE_GPR + 1;
 
  // Register mnemonics (for use by debugger/machine code printers).
  //
  String[] GPR_NAMES = RegisterConstantsHelper.gprNames();

  /**
   * This class exists only to kludge around the fact that we can't
   * put static clinit blocks in interfaces.  As a result,
   * it is awkward to write 'nice' code to initialize the register names
   * based on the values of the constants.
   */
  class RegisterConstantsHelper {
    static String[] gprNames() {
      String[] names =
          {"R0",
           "R1",
           "R2",
           "R3",
           "R4",
           "R5",
           "R6",
           "R7",
           "R8",
           "R9",
           "R10",
           "R11",
           "R12",
           "R13",
           "R14",
           "R15",
           "R16",
           "R17",
           "R18",
           "R19",
           "R20",
           "R21",
           "R22",
           "R23",
           "R24",
           "R25",
           "R26",
           "R27",
           "R28",
           "R29",
           "R30",
           "R31",
           "R32",
           "R33",
           "R34",
           "R35",
           "R36",
           "R37",
           "R38",
           "R39",
           "R40",
           "R41",
           "R42",
           "R43",
           "R44",
           "R45",
           "R46",
           "R47",
           "R48",
           "R49",
           "R50",
           "R51",
           "R52",
           "R53",
           "R54",
           "R55",
           "R56",
           "R57",
           "R58",
           "R59",
           "R60",
           "R61",
           "R62",
           "R63",
           "R64",
           "R65",
           "R66",
           "R67",
           "R68",
           "R69",
           "R70",
           "R71",
           "R72",
           "R73",
           "R74",
           "R75",
           "R76",
           "R77",
           "R78",
           "R79",
           "R80",
           "R81",
           "R82",
           "R83",
           "R84",
           "R85",
           "R86",
           "R87",
           "R88",
           "R89",
           "R90",
           "R91",
           "R92",
           "R93",
           "R94",
           "R95",
           "R96",
           "R97",
           "R98",
           "R99",
           "R100",
           "R101",
           "R102",
           "R103",
           "R104",
           "R105",
           "R106",
           "R107",
           "R108",
           "R109",
           "R110",
           "R111",
           "R112",
           "R113",
           "R114",
           "R115",
           "R116",
           "R117",
           "R118",
           "R119",
           "R120",
           "R121",
           "R122",
           "R123",
           "R124",
           "R125",
           "R126",
           "R127"};
      names[FRAME_POINTER] = "FP";
      names[JTOC_POINTER] = "JT";
      return names;
    }
  }
}

