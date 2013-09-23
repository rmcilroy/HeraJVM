package org.jikesrvm.cellspu;

import org.jikesrvm.scheduler.greenthreads.VM_SubArchMigration;

/**
 * Communication values used by spu -> ppu communication.  These should be the
 * same as those defined in spuDefs.h
 *
 */
public interface VM_ComConstants {
	
	
	public static final int ACK                    =  0x1;
	public static final int NACK                   =  0x2;
	public static final int ERR_TOO_MANY_PARAMS    =  0x3;
	public static final int ERR_METHOD_NOT_PREPARED=  0x4;
	public static final int ERR_METHOD_NOT_LOADED  =  0x5;
	public static final int ERR_PARAMS_NOT_LOADED  =  0x6;
	public static final int ERR_UNKNOWN_CMD        =  0x7;

	public static final int RUNTIME_COPY_COMPLETE  =  0x10;
	public static final int JAVA_VM_STARTED        =  0x11;
	public static final int SET_PROCESSOR_REG      =  0x12;

	public static final int LOAD_STATIC_METHOD         =  0x20;
	public static final int LOAD_WORD_PARAM            =  0x21;
	public static final int LOAD_DOUBLE_PARAM          =  0x22;
	public static final int RUN_METHOD_RETURNING_VOID  =  VM_SubArchMigration.RUN_METHOD_RETURNING_VOID;
	public static final int RUN_METHOD_RETURNING_INT   =  VM_SubArchMigration.RUN_METHOD_RETURNING_INT;
	public static final int RUN_METHOD_RETURNING_FLOAT =  VM_SubArchMigration.RUN_METHOD_RETURNING_FLOAT;
	public static final int RUN_METHOD_RETURNING_LONG  =  VM_SubArchMigration.RUN_METHOD_RETURNING_LONG;
	public static final int RUN_METHOD_RETURNING_DOUBLE=  VM_SubArchMigration.RUN_METHOD_RETURNING_DOUBLE;
	public static final int RUN_METHOD_RETURNING_REF   =  VM_SubArchMigration.RUN_METHOD_RETURNING_REF;
	public static final int LOAD_CLASS_STATICS         =  0x29;
	
	public static final int RETURN_VALUE_V         =  0x30;  // returning a integer value
	public static final int RETURN_VALUE_I         =  0x31;  // returning a integer value
	public static final int RETURN_VALUE_L_UPPER   =  0x32;  // returning a long value (upper word)
	public static final int RETURN_VALUE_L_LOWER   =  0x33;  // returning a long value (lower word)
	public static final int RETURN_VALUE_F         =  0x34;  // returning a float value
	public static final int RETURN_VALUE_D_UPPER   =  0x35;  // returning a double value (upper word)
	public static final int RETURN_VALUE_D_LOWER   =  0x36;  // returning a double value (lower word)
	public static final int RETURN_VALUE_R         =  0x37;  // returning a reference value

	public static final int TRAP_MESSAGE					 =  0x40;
	public static final int CONSOLE_WRITE_CHAR     =  0x41;
	public static final int CONSOLE_WRITE_INT      =  0x42;
	public static final int CONSOLE_WRITE_INT_BOTH =  0x43;
	public static final int CONSOLE_WRITE_INT_HEX  =  0x44;
	public static final int CONSOLE_WRITE_LONG     =  0x45;
	public static final int CONSOLE_WRITE_LONG_BOTH=  0x46;
	public static final int CONSOLE_WRITE_LONG_HEX =  0x47;
	public static final int CONSOLE_WRITE_DOUBLE   =  0x48;
	public static final int FAKE_TRAP_MESSAGE      =  0x49;
	public static final int FAKE_TRAP_MESSAGE_STR  =  0x4A;
	public static final int FAKE_TRAP_MESSAGE_INT  =  0x4B;
}
