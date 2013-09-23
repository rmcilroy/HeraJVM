#ifndef SPU_DEFS_H
#define SPU_DEFS_H

/************* Spu memory layout *****************/

	// 0x0
#define RUNTIME_CODE_START  		  0x0
	
#define TRAP_ENTRYPOINT						0x680
	// 0x400
#define CODE_ENTRYPOINT     		  0x700
#define CODE_ENTRYPOINT_END       0x1000

	// 0x800
#define OBJECT_CACHE_TABLE  			0x1000
#define OBJECT_CACHE_TABLE_LENGTH 0x2000
#define OBJECT_CACHE_TABLE_END    OBJECT_CACHE_TABLE + OBJECT_CACHE_TABLE_LENGTH
	
	// 0x3000
#define CODE_CACHE_START    		  OBJECT_CACHE_TABLE_END
#define CODE_CACHE_LENGTH   		  0xD000
#define CODE_CACHE_END	    		 	CODE_CACHE_START + CODE_CACHE_LENGTH

	// 0x10000
#define OBJECT_CACHE_START  			CODE_CACHE_END
#define OBJECT_CACHE_LENGTH 			0x20000
#define OBJECT_CACHE_END    		  OBJECT_CACHE_START + OBJECT_CACHE_LENGTH

	// 0x30000
#define STATICS_START 			  		OBJECT_CACHE_END
#define STATICS_LENGTH				  	0x4000
#define STATICS_END					    	STATICS_START + STATICS_LENGTH

	//0x34000
#define CLASS_TIBS_START 			  	STATICS_END
#define CLASS_TIBS_LENGTH					0x1000
#define CLASS_TIBS_END				    CLASS_TIBS_START + CLASS_TIBS_LENGTH

	// 0x35000
#define JTOC_TABLE_START					CLASS_TIBS_END
#define JTOC_TABLE_LENGTH				  0x1000
#define JTOC_TABLE_END						JTOC_TABLE_START + JTOC_TABLE_LENGTH
	
	// 0x36000
#define STATICS_TOC               JTOC_TABLE_END
#define STATICS_TOC_LENGTH        0x1000
#define STATICS_TOC_END           STATICS_TOC + STATICS_TOC_LENGTH

	// 0x37000
#define TIB_TABLE                 STATICS_TOC_END
#define TIB_TABLE_LENGTH          0x800
#define TIB_TABLE_END             TIB_TABLE + TIB_TABLE_LENGTH

	//0x37800
#define SIZE_STATICS_TABLE        TIB_TABLE_END
#define SIZE_STATICS_TABLE_LENGTH 0x800
#define SIZE_STATICS_TABLE_END    SIZE_STATICS_TABLE + SIZE_STATICS_TABLE_LENGTH
	
	// 0x38000
#define ATOMIC_CACHE_LINE					= SIZE_STATICS_TABLE_END
#define ATOMIC_CACHE_LINE_LENTH   = 128  // size of cache line
#define ATOMIC_CACHE_LINE_END     = ATOMIC_CACHE_LINE + ATOMIC_CACHE_LINE_LENTH
	// 0x38080
		
	// 0x39800
#define STACK_END									0x40000 - STACK_SIZE_NORMAL
#define STACK_BEGIN								0x3fff0


#define JTOC_PTR						     (JTOC_TABLE_START + (JTOC_TABLE_LENGTH / 2))

// Tag group from proxy MFC commands
#define PROXY_TAG_GROUP           0xf
#define PROXY_TAG_GROUP_BM        (1 << 0xf)


// Communication values used in mailboxes
// (see also VM_Com_Constants in jikes cellspu package)
#define ACK                       0x1
#define NACK                      0x2
#define ERR_TOO_MANY_PARAMS       0x3
#define ERR_METHOD_NOT_PREPARED   0x4
#define ERR_METHOD_NOT_LOADED     0x5
#define ERR_PARAMS_NOT_LOADED     0x6
#define ERR_UNKNOWN_CMD           0x7

#define RUNTIME_COPY_COMPLETE     0x10
#define JAVA_VM_STARTED           0x11
#define SET_PROCESSOR_REG         0x12

#define LOAD_STATIC_METHOD           0x20
#define LOAD_WORD_PARAM              0x21
#define LOAD_DOUBLE_PARAM            0x22
#define RUN_METHOD_RETURNING_VOID    0x23
#define RUN_METHOD_RETURNING_INT     0x24
#define RUN_METHOD_RETURNING_FLOAT   0x25
#define RUN_METHOD_RETURNING_LONG    0x26
#define RUN_METHOD_RETURNING_DOUBLE  0x27
#define RUN_METHOD_RETURNING_REF     0x28
#define LOAD_CLASS_STATICS           0x29

#define RETURN_VALUE_V           0x30  // returning a integer value
#define RETURN_VALUE_I           0x31  // returning a integer value
#define RETURN_VALUE_L_UPPER     0x32  // returning a long value (upper word)
#define RETURN_VALUE_L_LOWER     0x33  // returning a long value (lower word)
#define RETURN_VALUE_F           0x34  // returning a float value
#define RETURN_VALUE_D_UPPER     0x35  // returning a double value (upper word)
#define RETURN_VALUE_D_LOWER     0x36  // returning a double value (lower word)
#define RETURN_VALUE_R           0x37  // returning a reference value

#define TRAP_MESSAGE             0x40
#define CONSOLE_WRITE_CHAR       0x41
#define CONSOLE_WRITE_INT        0x42
#define CONSOLE_WRITE_INT_BOTH   0x43
#define CONSOLE_WRITE_INT_HEX    0x44
#define CONSOLE_WRITE_LONG       0x45
#define CONSOLE_WRITE_LONG_BOTH  0x46
#define CONSOLE_WRITE_LONG_HEX   0x47
#define CONSOLE_WRITE_DOUBLE     0x48
#define FAKE_TRAP_MESSAGE        0x49
#define FAKE_TRAP_MESSAGE_STR    0x4A
#define FAKE_TRAP_MESSAGE_INT    0x4B

#endif
