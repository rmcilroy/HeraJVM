package org.jikesrvm.scheduler.greenthreads;

import org.jikesrvm.SubordinateArchitecture;
import org.jikesrvm.VM;
import org.jikesrvm.VM_Constants;
import org.jikesrvm.cellspu.VM_SubArchBootRecord;
import org.jikesrvm.classloader.VM_Class;
import org.jikesrvm.classloader.VM_Method;
import org.jikesrvm.runtime.VM_BootRecord;
import org.jikesrvm.runtime.VM_Magic;
import org.vmmagic.pragma.Entrypoint;
import org.vmmagic.unboxed.Address;
import org.vmmagic.unboxed.Offset;

import static org.jikesrvm.runtime.VM_SysCall.sysCall;

public class VM_SubArchMigration implements VM_Constants {


	public static final int RUN_METHOD_RETURNING_VOID  =  0x23;
	public static final int RUN_METHOD_RETURNING_INT   =  0x24;
	public static final int RUN_METHOD_RETURNING_FLOAT =  0x25;
	public static final int RUN_METHOD_RETURNING_LONG  =  0x26;
	public static final int RUN_METHOD_RETURNING_DOUBLE=  0x27;
	public static final int RUN_METHOD_RETURNING_REF   =  0x28;
	
	@Entrypoint
	public static void migrateMethodReturningVoid (VM_Method methodRef, 
				                                         Address paramsStart,
				                                         int paramsLength) {
		
  	VM_Class decClass       = methodRef.getDeclaringClass();
  	int methodTocOffset     = decClass.getSubArchTocIdx().plus(SubordinateArchitecture.VM_ArchConstants.TIB_TABLE_JTOC_OFF).toInt();
  	int methodSubArchOffset = methodRef.getSubArchOffset().toInt();

  	VM_Magic.sync();
  	
  	if(VM.VerifyAssertions) VM._assert(VM_SubArchBootRecord.isSubArchStarted()); 
  	
  	
		int threadId = sysCall.migrateToSubArch(RUN_METHOD_RETURNING_VOID,
																						-1,
																						methodTocOffset,
																						methodSubArchOffset,
																						paramsStart,
																						paramsLength);
		// wait for thread to migrate back here
		VM_Wait.subArchWait(threadId);
		
		return;
	}
	
	@Entrypoint
	public static int migrateMethodReturningInt (VM_Method methodRef,
			                                         Address paramsStart,
			                                         int paramsLength) {
		
		VM_Class decClass       = methodRef.getDeclaringClass();
  	int methodTocOffset     = decClass.getSubArchTocIdx().plus(SubordinateArchitecture.VM_ArchConstants.TIB_TABLE_JTOC_OFF).toInt();
  	int methodSubArchOffset = methodRef.getSubArchOffset().toInt();

  	VM_Magic.sync();
  	
  	if(VM.VerifyAssertions) VM._assert(VM_SubArchBootRecord.isSubArchStarted()); 
  	
  	
		int threadId = sysCall.migrateToSubArch(RUN_METHOD_RETURNING_INT,
																						-1,
																						methodTocOffset,
																						methodSubArchOffset,
																						paramsStart,
																						paramsLength);

		// wait for thread to migrate back here
		VM_Wait.subArchWait(threadId);

		int ret = sysCall.subArchGetIntReturn(threadId);

		// get return value
		return ret;
	}
	
	@Entrypoint
	public static float migrateMethodReturningFloat (VM_Method methodRef, 
      	                                           Address paramsStart,
      	                                           int paramsLength) {

		VM_Class decClass       = methodRef.getDeclaringClass();
  	int methodTocOffset     = decClass.getSubArchTocIdx().plus(SubordinateArchitecture.VM_ArchConstants.TIB_TABLE_JTOC_OFF).toInt();
  	int methodSubArchOffset = methodRef.getSubArchOffset().toInt();

  	VM_Magic.sync();
  	
  	if(VM.VerifyAssertions) VM._assert(VM_SubArchBootRecord.isSubArchStarted()); 
  	
  	
		int threadId = sysCall.migrateToSubArch(RUN_METHOD_RETURNING_FLOAT,
																						-1,
																						methodTocOffset,
																						methodSubArchOffset,
																						paramsStart,
																						paramsLength);
		
		// wait for thread to migrate back here
		VM_Wait.subArchWait(threadId);

		// get return value
		return sysCall.subArchGetFloatReturn(threadId);
	}

	@Entrypoint
	public static long migrateMethodReturningLong (VM_Method methodRef,
                                                 Address paramsStart,
                                                 int paramsLength) {

		VM_Class decClass       = methodRef.getDeclaringClass();
  	int methodTocOffset     = decClass.getSubArchTocIdx().plus(SubordinateArchitecture.VM_ArchConstants.TIB_TABLE_JTOC_OFF).toInt();
  	int methodSubArchOffset = methodRef.getSubArchOffset().toInt();

  	VM_Magic.sync();
  	
  	if(VM.VerifyAssertions) VM._assert(VM_SubArchBootRecord.isSubArchStarted()); 
  	
  	
		int threadId = sysCall.migrateToSubArch(RUN_METHOD_RETURNING_LONG,
																						-1,
																						methodTocOffset,
																						methodSubArchOffset,
																						paramsStart,
																						paramsLength);
		
		// wait for thread to migrate back here
		VM_Wait.subArchWait(threadId);

		// get return value
		return sysCall.subArchGetLongReturn(threadId);
	}

	@Entrypoint
	public static double migrateMethodReturningDouble (VM_Method methodRef,
                                                     Address paramsStart,
                                                     int paramsLength) {
		VM_Class decClass       = methodRef.getDeclaringClass();
  	int methodTocOffset     = decClass.getSubArchTocIdx().plus(SubordinateArchitecture.VM_ArchConstants.TIB_TABLE_JTOC_OFF).toInt();
  	int methodSubArchOffset = methodRef.getSubArchOffset().toInt();

  	VM_Magic.sync();
  	
  	if(VM.VerifyAssertions) VM._assert(VM_SubArchBootRecord.isSubArchStarted()); 
  	
  	
		int threadId = sysCall.migrateToSubArch(RUN_METHOD_RETURNING_DOUBLE,
																						-1,
																						methodTocOffset,
																						methodSubArchOffset,
																						paramsStart,
																						paramsLength);
		
		// wait for thread to migrate back here
		VM_Wait.subArchWait(threadId);

		// get return value
		return sysCall.subArchGetDoubleReturn(threadId);
	}

	@Entrypoint
	public static Address migrateMethodReturningRef (VM_Method methodRef,
      																						 Address paramsStart,
      																						 int paramsLength) {
		VM_Class decClass       = methodRef.getDeclaringClass();
  	int methodTocOffset     = decClass.getSubArchTocIdx().plus(SubordinateArchitecture.VM_ArchConstants.TIB_TABLE_JTOC_OFF).toInt();
  	int methodSubArchOffset = methodRef.getSubArchOffset().toInt();

  	VM_Magic.sync();
  	
  	if(VM.VerifyAssertions) VM._assert(VM_SubArchBootRecord.isSubArchStarted()); 
  	
  	
		int threadId = sysCall.migrateToSubArch(RUN_METHOD_RETURNING_REF,
																						-1,
																						methodTocOffset,
																						methodSubArchOffset,
																						paramsStart,
																						paramsLength);
		// wait for thread to migrate back here
		VM_Wait.subArchWait(threadId);

		// get return value
		return sysCall.subArchGetRefReturn(threadId);
	}
}
