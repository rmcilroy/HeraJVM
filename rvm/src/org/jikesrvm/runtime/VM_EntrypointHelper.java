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
package org.jikesrvm.runtime;

import org.jikesrvm.VM;
import org.jikesrvm.VM_Constants;
import org.jikesrvm.classloader.VM_Atom;
import org.jikesrvm.classloader.VM_BootstrapClassLoader;
import org.jikesrvm.classloader.VM_Class;
import org.jikesrvm.classloader.VM_Field;
import org.jikesrvm.classloader.VM_Member;
import org.jikesrvm.classloader.VM_NormalMethod;
import org.jikesrvm.classloader.VM_Type;
import org.jikesrvm.classloader.VM_TypeReference;

/**
 * Helper class for retrieving entrypoints. Entrypoints are fields and
 * methods of the virtual machine that are needed by compiler-generated
 * machine code or C runtime code.
 */
public class VM_EntrypointHelper {
  /**
   * Get description of virtual machine component (field or method).
   * Note: This is method is intended for use only by VM classes that need
   * to address their own fields and methods in the runtime virtual machine
   * image.  It should not be used for general purpose class loading.
   * @param classDescriptor  class  descriptor - something like "Lorg/jikesrvm/VM_Runtime;"
   * @param memberName       member name       - something like "invokestatic"
   * @param memberDescriptor member descriptor - something like "()V"
   * @return corresponding VM_Member object
   */
  private static VM_Member getMember(String classDescriptor, String memberName, String memberDescriptor) {
    VM_Atom clsDescriptor = VM_Atom.findOrCreateAsciiAtom(classDescriptor);
    VM_Atom memName = VM_Atom.findOrCreateAsciiAtom(memberName);
    VM_Atom memDescriptor = VM_Atom.findOrCreateAsciiAtom(memberDescriptor);
    try {
      VM_TypeReference tRef =
          VM_TypeReference.findOrCreate(VM_BootstrapClassLoader.getBootstrapClassLoader(), clsDescriptor);
      VM_Class cls = (VM_Class) tRef.resolve(false);
      cls.resolve(false);

      VM_Member member;
      if ((member = cls.findDeclaredField(memName, memDescriptor)) != null) {
        return member;
      }
      if ((member = cls.findDeclaredMethod(memName, memDescriptor)) != null) {
        return member;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    // The usual causes for getMember() to fail are:
    //  1. you mispelled the class name, member name, or member signature
    //  2. the class containing the specified member didn't get compiled
    //
    VM.sysWrite("VM_Entrypoints.getMember: can't resolve class=" +
                classDescriptor +
                " member=" +
                memberName +
                " desc=" +
                memberDescriptor +
                "\n");
    VM._assert(VM_Constants.NOT_REACHED);
    return null; // placate jikes
  }

  public static VM_NormalMethod getMethod(String klass, String member, String descriptor, final boolean runtimeServiceMethod) {
    VM_NormalMethod m = (VM_NormalMethod) getMember(klass, member, descriptor);
    m.setRuntimeServiceMethod(runtimeServiceMethod);
    return m;
  }

  public static VM_NormalMethod getMethod(String klass, String member, String descriptor) {
    return getMethod(klass, member, descriptor, true);
  }

  public static VM_Field getField(String klass, String member, String descriptor) {
    return (VM_Field) getMember(klass, member, descriptor);
  }

  /**
   * Get description of virtual machine field.
   * @param klass class containing field
   * @param memberName member name - something like "invokestatic"
   * @param type of field
   * @return corresponding VM_Field
   */
  static VM_Field getField(Class<?> klass, String member, Class<?> type) {
    if (!VM.runningVM) { // avoid compiling this code into the boot image
      try {
        VM_TypeReference klassTRef = VM_TypeReference.findOrCreate(klass);
        VM_Class cls = klassTRef.resolve(false).asClass();
        cls.resolve(false);

        VM_Atom memName = VM_Atom.findOrCreateAsciiAtom(member);
        VM_Atom typeName = VM_TypeReference.findOrCreate(type).getName();

        VM_Field field = cls.findDeclaredField(memName, typeName);
        if (field != null) {
          return field;
        }
      } catch(Throwable t) {
        throw new Error("VM_Entrypoints.getField: can't resolve class=" +
            klass + " member=" + member + " desc=" + type, t);
      }
    }
    throw new Error("VM_Entrypoints.getField: can't resolve class=" +
        klass + " member=" + member + " desc=" + type);
  }

  /**
   * Get description of virtual machine method.
   * @param klass class  containing method
   * @param memberName member name - something like "invokestatic"
   * @param memberDescriptor member descriptor - something like "()V"
   * @return corresponding VM_Method
   */
  public static VM_NormalMethod getMethod(Class<?> klass, String member, String descriptor) {
    if (!VM.runningVM) { // avoid compiling this code into the boot image
      try {
        VM_TypeReference klassTRef = VM_TypeReference.findOrCreate(klass);
        VM_Class cls = klassTRef.resolve(false).asClass();
        cls.resolve(false);

        VM_Atom memName = VM_Atom.findOrCreateAsciiAtom(member);
        VM_Atom memDescriptor = VM_Atom.findOrCreateAsciiAtom(descriptor);

        VM_NormalMethod m = (VM_NormalMethod)cls.findDeclaredMethod(memName, memDescriptor);
        if (m != null) {
          m.setRuntimeServiceMethod(true);
          return m;
        }
      } catch(Throwable t) {
        throw new Error("VM_Entrypoints.getField: can't resolve class=" +
            klass + " member=" + member + " desc=" + descriptor, t);
      }
    }
    throw new Error("VM_Entrypoints.getMethod: can't resolve class=" +
        klass + " method=" + member + " desc=" + descriptor);
  }
  
  /**
   * Get description of virtual machine class.
   * @param klass class  
   * @return corresponding VM_Class
   */
  static VM_Class getClass(Class<?> klass) {
    try {
      VM_TypeReference klassTRef = VM_TypeReference.findOrCreate(klass);
      VM_Class cls = klassTRef.resolve(false).asClass();
      cls.resolve(false);

      return cls;
    } catch(Throwable t) {
    	System.out.println(t);
      throw new Error("VM_Entrypoints.getClass: can't resolve class=" + klass);
    }
  }
  
  private static void resolveClassInSubArch(Class<?> cls) {
  	VM_Class klass = getClass(cls);
  	klass.load(true);
  	klass.resolve(true);
  }
  
  public static void resolveEntryPointsInSubArch() {
  	VM_Type.resolveTypesForSubArch();
  	resolveClassInSubArch(java.lang.JikesRVMHelpers.class);
  	resolveClassInSubArch(java.lang.StringBuilder.class);
  	resolveClassInSubArch(java.lang.StringBuffer.class);
  	resolveClassInSubArch(java.io.PrintStream.class);
  	resolveClassInSubArch(java.io.PrintWriter.class);
  	resolveClassInSubArch(org.jikesrvm.classloader.VM_TypeReference.class);
  	resolveClassInSubArch(org.jikesrvm.classloader.VM_Atom.class);
  	resolveClassInSubArch(org.jikesrvm.classloader.VM_Type.class);
  	resolveClassInSubArch(org.jikesrvm.classloader.VM_Primitive.class);
  	resolveClassInSubArch(org.jikesrvm.classloader.VM_Class.class);
  	resolveClassInSubArch(org.jikesrvm.classloader.VM_Array.class);
  	resolveClassInSubArch(org.jikesrvm.classloader.VM_FieldReference.class);
  	resolveClassInSubArch(org.jikesrvm.classloader.VM_MethodReference.class);
  	resolveClassInSubArch(org.jikesrvm.classloader.VM_Field.class);
  	resolveClassInSubArch(org.jikesrvm.classloader.VM_NormalMethod.class);
  	resolveClassInSubArch(org.jikesrvm.runtime.VM_Math.class);
  	resolveClassInSubArch(org.jikesrvm.runtime.VM_Runtime.class);
  	resolveClassInSubArch(org.jikesrvm.objectmodel.VM_ObjectModel.class);
  	resolveClassInSubArch(org.jikesrvm.objectmodel.VM_JavaHeader.class);
  	resolveClassInSubArch(org.jikesrvm.SubordinateArchitecture.VM_OutOfLineMachineCode.class);
  	resolveClassInSubArch(org.jikesrvm.SubordinateArchitecture.VM_RuntimeMethods.class);
  	resolveClassInSubArch(org.jikesrvm.SubordinateArchitecture.VM_ProcessorLocalState.class);
  	resolveClassInSubArch(java.lang.JikesRVMSupport.class);
  	resolveClassInSubArch(org.jikesrvm.VM.class);
  	resolveClassInSubArch(org.jikesrvm.scheduler.VM_Synchronization.class);
  	resolveClassInSubArch(org.jikesrvm.scheduler.VM_ThinLock.class);
  	resolveClassInSubArch(org.jikesrvm.scheduler.VM_ProcessorLock.class);
  	resolveClassInSubArch(org.jikesrvm.scheduler.greenthreads.VM_GreenLock.class);
  	resolveClassInSubArch(org.jikesrvm.scheduler.greenthreads.VM_GreenProcessor.class);
  	resolveClassInSubArch(org.jikesrvm.scheduler.greenthreads.VM_GreenSubArchProcessor.class);
  	resolveClassInSubArch(org.jikesrvm.scheduler.greenthreads.VM_GreenScheduler.class);
  	resolveClassInSubArch(org.jikesrvm.scheduler.greenthreads.VM_GreenThread.class);
  	resolveClassInSubArch(org.jikesrvm.scheduler.greenthreads.VM_GreenThreadQueue.class);
  	resolveClassInSubArch(org.jikesrvm.scheduler.greenthreads.VM_GlobalGreenThreadQueue.class);
  	resolveClassInSubArch(org.jikesrvm.scheduler.greenthreads.VM_ThreadQueue.class);
  	resolveClassInSubArch(org.jikesrvm.scheduler.greenthreads.VM_ThreadProxyWaitingQueue.class);
  	resolveClassInSubArch(org.jikesrvm.scheduler.greenthreads.VM_ThreadProxy.class);
  	resolveClassInSubArch(org.mmtk.vm.Assert.class);
  	resolveClassInSubArch(org.mmtk.vm.ActivePlan.class);
  	resolveClassInSubArch(org.mmtk.vm.Collection.class);
  	resolveClassInSubArch(org.mmtk.vm.ObjectModel.class);
  	resolveClassInSubArch(org.mmtk.vm.Memory.class);
  	resolveClassInSubArch(org.mmtk.policy.LargeObjectSpace.class);
  	resolveClassInSubArch(org.mmtk.policy.ImmortalSpace.class);
  	resolveClassInSubArch(org.mmtk.policy.MarkSweepSpace.class);
  	resolveClassInSubArch(org.mmtk.policy.LargeObjectLocal.class);
  	resolveClassInSubArch(org.mmtk.policy.ImmortalLocal.class);
  	resolveClassInSubArch(org.mmtk.policy.MarkSweepLocal.class);
  }
  
  private static void initClassInSubArch(Class<?> cls) {
  	VM_Class klass = getClass(cls);
  	klass.instantiate(true);
    klass.initialize(true);
  }
  
  public static void initEntryPointsInSubArch() {
  	VM_Type.instantiateTypesForSubArch();
  	initClassInSubArch(java.lang.JikesRVMHelpers.class);
  	initClassInSubArch(java.lang.StringBuilder.class);
  	initClassInSubArch(java.lang.StringBuffer.class);
  	initClassInSubArch(java.io.PrintStream.class);
  	initClassInSubArch(java.io.PrintWriter.class);
  	// TODO - Fix
  	//initClassInSubArch(org.jikesrvm.classloader.VM_Atom.class);
  	//initClassInSubArch(org.jikesrvm.classloader.VM_TypeReference.class);
  	//initClassInSubArch(org.jikesrvm.classloader.VM_Type.class);
  	//initClassInSubArch(org.jikesrvm.classloader.VM_Primitive.class);
  	//initClassInSubArch(org.jikesrvm.classloader.VM_Class.class);
  	//initClassInSubArch(org.jikesrvm.classloader.VM_Array.class);
  	//initClassInSubArch(org.jikesrvm.classloader.VM_FieldReference.class);
  	//initClassInSubArch(org.jikesrvm.classloader.VM_MethodReference.class);
  	//initClassInSubArch(org.jikesrvm.classloader.VM_Field.class);
  	//initClassInSubArch(org.jikesrvm.classloader.VM_NormalMethod.class);
  	//initClassInSubArch(org.jikesrvm.runtime.VM_Math.class);
  	initClassInSubArch(org.jikesrvm.runtime.VM_Runtime.class);
  	initClassInSubArch(org.jikesrvm.objectmodel.VM_ObjectModel.class);
  	initClassInSubArch(org.jikesrvm.objectmodel.VM_JavaHeader.class);
  	//initClassInSubArch(org.jikesrvm.SubordinateArchitecture.VM_OutOfLineMachineCode.class);	
  	initClassInSubArch(org.jikesrvm.SubordinateArchitecture.VM_RuntimeMethods.class);
  	initClassInSubArch(org.jikesrvm.SubordinateArchitecture.VM_ProcessorLocalState.class);
  	initClassInSubArch(java.lang.JikesRVMSupport.class);
  	initClassInSubArch(org.jikesrvm.VM.class);
  	initClassInSubArch(org.jikesrvm.scheduler.VM_Synchronization.class);
  	initClassInSubArch(org.jikesrvm.scheduler.VM_ThinLock.class);
  	initClassInSubArch(org.jikesrvm.scheduler.VM_ProcessorLock.class);
  	initClassInSubArch(org.jikesrvm.scheduler.greenthreads.VM_GreenLock.class);
  	initClassInSubArch(org.jikesrvm.scheduler.greenthreads.VM_GreenProcessor.class);
  	initClassInSubArch(org.jikesrvm.scheduler.greenthreads.VM_GreenSubArchProcessor.class);
  	initClassInSubArch(org.jikesrvm.scheduler.greenthreads.VM_GreenScheduler.class);
  	initClassInSubArch(org.jikesrvm.scheduler.greenthreads.VM_GreenThread.class);
  	initClassInSubArch(org.jikesrvm.scheduler.greenthreads.VM_GreenThreadQueue.class);
  	initClassInSubArch(org.jikesrvm.scheduler.greenthreads.VM_GlobalGreenThreadQueue.class);
  	initClassInSubArch(org.jikesrvm.scheduler.greenthreads.VM_ThreadQueue.class);
  	initClassInSubArch(org.jikesrvm.scheduler.greenthreads.VM_ThreadProxyWaitingQueue.class);
  	initClassInSubArch(org.jikesrvm.scheduler.greenthreads.VM_ThreadProxy.class);
  	initClassInSubArch(org.mmtk.vm.Assert.class);
  	initClassInSubArch(org.mmtk.vm.ActivePlan.class);
  	initClassInSubArch(org.mmtk.vm.Collection.class);
  	initClassInSubArch(org.mmtk.vm.ObjectModel.class);
  	initClassInSubArch(org.mmtk.vm.Memory.class);
  	initClassInSubArch(org.mmtk.policy.LargeObjectSpace.class);
  	initClassInSubArch(org.mmtk.policy.ImmortalSpace.class);
  	initClassInSubArch(org.mmtk.policy.MarkSweepSpace.class);
  	initClassInSubArch(org.mmtk.policy.LargeObjectLocal.class);
  	initClassInSubArch(org.mmtk.policy.ImmortalLocal.class);
  	initClassInSubArch(org.mmtk.policy.MarkSweepLocal.class);
  }
}
