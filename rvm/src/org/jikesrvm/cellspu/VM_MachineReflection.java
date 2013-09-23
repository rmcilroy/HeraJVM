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

import org.jikesrvm.VM;
import org.jikesrvm.VM_Constants;
import org.jikesrvm.classloader.VM_Method;
import org.jikesrvm.classloader.VM_TypeReference;
import org.jikesrvm.runtime.VM_Memory;
import org.jikesrvm.runtime.VM_Reflection;
import org.vmmagic.unboxed.Word;
import org.vmmagic.unboxed.WordArray;

/**
 * Machine dependent portion of Reflective method invoker.
 */
public abstract class VM_MachineReflection implements VM_ArchConstants {

  /**
   * Determine number/type of registers/spills required to call specified method.
   * See also: VM_Compiler.loadParameters()
   */
  public static int countParameters(VM_Method method) {
  	// TODO
	  return 0;
  }

  /**
   * Collect parameters into arrays of registers/spills, as required to call specified method.
   */
  public static void packageParameters(VM_Method method, Object thisArg, Object[] otherArgs, WordArray GPRs,
                                       double[] FPRs, byte[] FPRmeta, WordArray Spills) {
  	// TODO
  }
}
