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
package org.jikesrvm.compilers.opt;

import org.jikesrvm.compilers.opt.ir.OPT_Instruction;

/**
 * Resource usage map representation
 * Used by the scheduler to accomodate resource patterns
 *
 * @see OPT_OperatorClass
 * @see org.jikesrvm.compilers.opt.ir.OPT_Operator
 */
final class OPT_ResourceMap {
  private static final int verbose = 0;

  private static void debug(String s) {
    System.out.println(s);
  }

  // Padding
  // For internal use only.
  private static final String ZEROS = dup(32, '0');

  private static String toBinaryPad32(int value) {
    String s = Integer.toBinaryString(value);
    return ZEROS.substring(s.length()) + s;
  }

  // GROWABLE Resource Usage map.
  private int[] rumap;
  // Current size of the RU map.
  private int size;

  // Grows the RU map to a given size.
  // For internal use only.
  private void grow(int s) {
    if (verbose >= 2) {
      debug("Growing from " + size + " to " + s);
    }
    if (size >= s) {
      return;
    }
    int len = rumap.length;
    if (len < s) {
      for (; len < s; len <<= 1) ;
      int[] t = new int[len];
      for (int i = 0; i < rumap.length; i++) {
        t[i] = rumap[i];
      }
      for (int i = rumap.length; i < len; i++) {
        t[i] = OPT_OperatorClass.NONE;
      }
      rumap = t;
    }
    size = s;
  }

  /**
   * Creates new resource map.
   */
  public OPT_ResourceMap() {
    this(4);
  }

  /**
   * Creates new resource map with desired initial length.
   *
   * @param length desired initial length of the resource map
   */
  public OPT_ResourceMap(int length) {
    rumap = new int[length];
    size = 0;
    for (int i = 0; i < length; i++) {
      rumap[i] = OPT_OperatorClass.NONE;
    }
  }

  /**
   * Reserves resources for given instruction at given time.
   *
   * @param i instruction
   * @param time time to schedule
   * @return true if succeeded, false if there was a conflict
   * @see #unschedule(OPT_Instruction)
   */
  public boolean schedule(OPT_Instruction i, int time) {
    if (OPT_SchedulingInfo.isScheduled(i)) {
      throw new InternalError("Already scheduled");
    }
    OPT_OperatorClass opc = i.operator().getOpClass();
    if (verbose >= 2) {
      debug("Op Class=" + opc);
    }
    for (int alt = 0; alt < opc.masks.length; alt++) {
      int[] ru = opc.masks[alt];
      if (schedule(ru, time)) {
        OPT_SchedulingInfo.setInfo(i, alt, time);
        return true;
      }
    }
    return false;
  }

  /**
   * Frees resources for given instruction.
   *
   * @param i instruction
   * @see #schedule(OPT_Instruction,int)
   */
  public void unschedule(OPT_Instruction i) {
    if (!OPT_SchedulingInfo.isScheduled(i)) {
      throw new InternalError("Not scheduled");
    }
    OPT_OperatorClass opc = i.operator().getOpClass();
    int[] ru = opc.masks[OPT_SchedulingInfo.getAlt(i)];
    unschedule(ru, OPT_SchedulingInfo.getTime(i));
    OPT_SchedulingInfo.resetInfo(i);
  }

  /**
   * Returns a string representation of the resource map.
   *
   * @return a string representation of the resource map
   */
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < size; i++) {
      sb.append(toBinaryPad32(rumap[i])).append("\n");
    }
    return sb.toString();
  }

  // Binds resources for given resource usage pattern at given time.
  // Returns false if there is a resource conflict.
  // For internal use only.
  private boolean schedule(int[] usage, int time) {
    grow(time + usage.length);
    if (verbose >= 1) {
      debug("Pattern (" + usage.length + ")");
      for (int anUsage : usage) debug("   " + toBinaryPad32(anUsage));
      debug("");
    }
    for (int i = 0; i < usage.length; i++) {
      if ((usage[i] & rumap[time + i]) != 0) {
        return false;
      }
    }
    for (int i = 0; i < usage.length; i++) {
      rumap[time + i] |= usage[i];
    }
    return true;
  }

  // Unbinds resources for given resource usage pattern at given time.
  // For internal use only.
  private void unschedule(int[] usage, int time) {
    for (int i = 0; i < usage.length; i++) {
      rumap[time + i] &= ~usage[i];
    }
  }

  // Generates a string of a given length filled by a given character.
  // For internal use only.
  private static String dup(int len, char c) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < len; i++) {
      sb.append(c);
    }
    return sb.toString();
  }
}



