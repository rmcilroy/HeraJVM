package org.jikesrvm.cellspu;

import org.jikesrvm.VM;
import org.jikesrvm.VM_Constants;
import org.jikesrvm.SubordinateArchitecture.VM_ProcessorLocalState;
import org.jikesrvm.runtime.VM_Magic;
import org.jikesrvm.scheduler.VM_Processor;
import org.vmmagic.pragma.Entrypoint;
import org.vmmagic.pragma.Uninterruptible;
import org.vmmagic.unboxed.Address;
import org.vmmagic.unboxed.LocalAddress;
import org.vmmagic.unboxed.WordArray;

/**
 * Methods which are used used by the runtime on the cell spu
 *
 */
public class VM_RuntimeMethods implements VM_Constants, VM_ArchConstants {

	/** Offset into JTOC of class statics for class being loaded */
	private static int classStaticOffset;
	
	/** Offset into class static of method currently being loaded */
	private static int methodOffset;

	/** Entrypoint of method being loaded and run */
	private static VM_CodeArray methodEntry;
	
	/** Number of parameters expected by method */
	private static int numParams;
	
	/** parameters - Hack change these into arrays */
	private static Object hackParam0, hackParam1, hackParam2, hackParam3;
	
	/**
	 * This is the initial entry point of the runtime system on the SPU.
	 * It will sit waiting for a method to run to be supplied by the main runtime.
	 *
	 */
	@Entrypoint
	public static void runtimeEntry (int phys_id) {
		resetMethodDetails();
		
		VM_Magic.writeIntrMailBox(JAVA_VM_STARTED);
		VM_Magic.writeIntrMailBox(phys_id);

		int cmd = VM_Magic.readIntMailBox();
		while (cmd != SET_PROCESSOR_REG) {
			VM_Magic.writeIntrMailBox(ERR_UNKNOWN_CMD);
			VM_Magic.writeIntrMailBox(cmd);
			cmd = VM_Magic.readIntMailBox();
		}
		
		Address procAddr = VM_Magic.readRefMailBox();
		VM_ProcessorLocalState.setCurrentProcessor((VM_Processor)VM_Magic.addressAsObject(procAddr));
		
		VM_Magic.writeIntrMailBox(ACK);
		
		while (true) {
			// Read the next command from the main runtime
			cmd = VM_Magic.readIntMailBox();
			
			switch (cmd) {

			case LOAD_STATIC_METHOD:
				loadStaticMethod();
				break;
			case LOAD_WORD_PARAM:
				loadWordParam();
				break;
			case LOAD_DOUBLE_PARAM:
				loadDoubleParam();
				break;
			case RUN_METHOD_RETURNING_VOID:
				invokeVoidMethod();
				break;
			case RUN_METHOD_RETURNING_INT:
				invokeIntMethod();
				break;
			case RUN_METHOD_RETURNING_FLOAT:
				invokeFloatMethod();
				break;
			case RUN_METHOD_RETURNING_LONG:
				invokeLongMethod();
				break;
			case RUN_METHOD_RETURNING_DOUBLE:
				invokeDoubleMethod();
				break;
			case RUN_METHOD_RETURNING_REF:
				invokeRefMethod();
				break;
			default:
			{
				VM_Magic.writeIntrMailBox(ERR_UNKNOWN_CMD);
				VM_Magic.writeIntrMailBox(cmd);
				break;
			}
			}
		}
	}

	@Uninterruptible
	public static void write(char value) {
		int ret_signal;
		
		VM_Magic.writeIntrMailBox(CONSOLE_WRITE_CHAR);
		VM_Magic.writeMailBox(value);
		
		// wait for ack
		ret_signal = VM_Magic.readIntMailBox();
		
		// TODO - something with this ret signal
	}
	
	@Uninterruptible
	public static void write(int value, int mode) {
		int ret_signal;
		
		VM_Magic.writeIntrMailBox(CONSOLE_WRITE_INT + mode);
		VM_Magic.writeMailBox(value);
		
		// wait for ack
		ret_signal = VM_Magic.readIntMailBox();
		
		// TODO - something with this ret signal
	}
	
	@Uninterruptible
	public static void writeDouble(double value, int postDecimalDigits) {
		int ret_signal;
		
		VM_Magic.writeIntrMailBox(CONSOLE_WRITE_DOUBLE);
		VM_Magic.writeMailBoxUpperWord(value);
		VM_Magic.writeMailBoxLowerWord(value);
		VM_Magic.writeMailBox(postDecimalDigits);
		
		// wait for ack
		ret_signal = VM_Magic.readIntMailBox();
		
		// TODO - something with this ret signal
	}

	@Uninterruptible
	public static void write(long value, int mode) {
		int ret_signal;
		
		VM_Magic.writeIntrMailBox(CONSOLE_WRITE_LONG + mode);
		VM_Magic.writeMailBoxUpperWord(value);
		VM_Magic.writeMailBoxLowerWord(value);
		
		// wait for ack
		ret_signal = VM_Magic.readIntMailBox();
		
		// TODO - something with this ret signal
	}
	
	private static void loadStaticMethod() {
		// read method offset
		classStaticOffset = VM_Magic.readIntMailBox();
		methodOffset = VM_Magic.readIntMailBox();
		
		// start to pull method into local memory
		LocalAddress methodEntryRef = VM_Magic.cacheStaticMethod(methodOffset, classStaticOffset);
		methodEntry = (VM_CodeArray) VM_Magic.localAddressAsObject(methodEntryRef);
		VM_Magic.writeIntrMailBox(ACK);
	}

	private static void loadWordParam() {
		// read param
		
		// TODO - get rid of this - use arrays to store parameteres
		if (numParams == 0) {
			hackParam0 = VM_Magic.readRefMailBox();
		} else if (numParams == 1) {
			hackParam1 = VM_Magic.readRefMailBox();
		} else if (numParams == 2) {
			hackParam2 = VM_Magic.readRefMailBox();
		} else if (numParams == 3) {
			hackParam3 = VM_Magic.readRefMailBox();
		} else {
			VM_Magic.writeMailBox(ERR_TOO_MANY_PARAMS);
			VM_Magic.writeIntrMailBox(NACK);
			return;
		}

		numParams++;
		VM_Magic.writeIntrMailBox(ACK);
	}

	private static void loadDoubleParam() {
		// read param
		// TODO - get rid of this - use arrays to store parameteres
		
		VM_Magic.writeMailBox(ERR_TOO_MANY_PARAMS);
		VM_Magic.writeIntrMailBox(NACK);
		return;
	}
	
	private static boolean checkMethodIsLoaded() {
		if (methodOffset == -1) {
			VM_Magic.writeMailBox(ERR_METHOD_NOT_PREPARED);
			VM_Magic.writeIntrMailBox(NACK);
			return false;
		}
		if (methodEntry == null) {
			VM_Magic.writeMailBox(ERR_METHOD_NOT_LOADED);
			VM_Magic.writeIntrMailBox(NACK);
			return false;
		}
		if (numParams == -1) {
			VM_Magic.writeMailBox(ERR_PARAMS_NOT_LOADED);
			VM_Magic.writeIntrMailBox(NACK);
			return false;
		}
		return true;
	}
	
	private static void invokeVoidMethod() {
		if(!checkMethodIsLoaded()) {
			return;
		}
		
		VM_Magic.writeIntrMailBox(ACK);
		VM_Magic.invokeMethodReturningVoid(methodEntry, 
				                               (WordArray)hackParam0, 
				                               (double [])hackParam1,
				                               (byte [])  hackParam2,
				                               (WordArray)hackParam3);

		VM_Magic.sync();
		resetMethodDetails();
		
		VM_Magic.writeIntrMailBox(RETURN_VALUE_V);
	}

	private static void invokeIntMethod() {
		if(!checkMethodIsLoaded()) {
			return;
		}
		
		VM_Magic.writeIntrMailBox(ACK);
		
		int ret = VM_Magic.invokeMethodReturningInt(methodEntry, 
				                                        (WordArray)hackParam0, 
				                                        (double [])hackParam1,
				                                        (byte [])  hackParam2,
				                                        (WordArray)hackParam3);
		
		VM_Magic.sync();
		resetMethodDetails();
	
		VM_Magic.writeMailBox(ret);
		VM_Magic.writeIntrMailBox(RETURN_VALUE_I);
	}

	private static void invokeFloatMethod() {
		if(!checkMethodIsLoaded()) {
			return;
		}
		
		VM_Magic.writeIntrMailBox(ACK);
		float ret = VM_Magic.invokeMethodReturningFloat(methodEntry, 
				                                            (WordArray)hackParam0, 
				                                            (double [])hackParam1,
				                                            (byte [])  hackParam2,
				                                            (WordArray)hackParam3);

		VM_Magic.sync();
		resetMethodDetails();
	
		VM_Magic.writeMailBox(ret);
		VM_Magic.writeIntrMailBox(RETURN_VALUE_F);
	}
	
	private static void invokeLongMethod() {
		if(!checkMethodIsLoaded()) {
			return;
		}
		
		VM_Magic.writeIntrMailBox(ACK);
		long ret = VM_Magic.invokeMethodReturningLong(methodEntry, 
				                                          (WordArray)hackParam0, 
				                                          (double [])hackParam1,
				                                          (byte [])  hackParam2,
				                                          (WordArray)hackParam3);

		VM_Magic.sync();
		resetMethodDetails();
	
		VM_Magic.writeMailBoxUpperWord(ret);   // this writes the top word
		VM_Magic.writeIntrMailBox(RETURN_VALUE_L_UPPER);
		VM_Magic.writeMailBoxLowerWord(ret);   // this writes the lower word
		VM_Magic.writeIntrMailBox(RETURN_VALUE_L_LOWER);		
	}

	private static void invokeDoubleMethod() {
		if(!checkMethodIsLoaded()) {
			return;
		}
		
		VM_Magic.writeIntrMailBox(ACK);
		double ret = VM_Magic.invokeMethodReturningDouble(methodEntry, 
				                                             (WordArray)hackParam0, 
				                                             (double [])hackParam1,
				                                             (byte [])  hackParam2,
				                                             (WordArray)hackParam3);

		VM_Magic.sync();
		resetMethodDetails();
	
		VM_Magic.writeMailBoxUpperWord(ret);   // this writes the top word
		VM_Magic.writeIntrMailBox(RETURN_VALUE_D_UPPER);
		VM_Magic.writeMailBoxLowerWord(ret);   // this writes the lower word
		VM_Magic.writeIntrMailBox(RETURN_VALUE_D_LOWER);		
	}

	private static void invokeRefMethod() {
		if(!checkMethodIsLoaded()) {
			return;
		}
		
		VM_Magic.writeIntrMailBox(ACK);
		Object ret = VM_Magic.invokeMethodReturningObject(methodEntry, 
				                                              (WordArray)hackParam0, 
				                                              (double [])hackParam1,
				                                              (byte [])  hackParam2,
				                                              (WordArray)hackParam3);

		VM_Magic.sync();
		resetMethodDetails();
	
		VM_Magic.writeMailBox(VM_Magic.objectAsAddress(ret));
		VM_Magic.writeIntrMailBox(RETURN_VALUE_R);
		return;
	}

	/**
	 * Reset method details to default to prepare for next method to be invoked
	 */
	private static void resetMethodDetails() {
		methodOffset = -1;
		methodEntry  = null;
		numParams = 0;
		hackParam0 = hackParam1 = hackParam2 = hackParam3 = null;
	}
	
}
