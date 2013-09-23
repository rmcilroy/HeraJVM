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

import org.jikesrvm.runtime.VM_SubArchStatics;
import org.vmmagic.unboxed.Offset;

/**
 * Architecture specific constants.
 */
public interface VM_ArchConstants extends VM_StackframeLayoutConstants, VM_RegisterConstants, VM_ChannelConstants, VM_ComConstants {
	
  int LT = 0;
  int GT = 1;
  int EQ = 2;
  int GE = 3;
  int LE = 4;
  int NE = 5;
  
	// reserved DMA transfer group tags
	public static final int METHOD_CACHE_READ_TAG_GROUP = 0x0;
	public static final int STATIC_CACHE_READ_TAG_GROUP = 0x1;
	public static final int OBJECT_CACHE_READ_TAG_GROUP = 0x2;
	public static final int OBJECT_CACHE_WRITE_TAG_GROUP= 0x3;
	public static final int PROXY_TAG_GROUP             = 0xf;
	
	public static final int LOG_ARRAY_BLOCK_ENTRIES       = 8;
	public static final int ARRAY_BLOCK_ENTRIES           = 0x1 << LOG_ARRAY_BLOCK_ENTRIES; // 256 bytes
	public static final int ARRAY_BLOCK_MASK            = ARRAY_BLOCK_ENTRIES - 1;
	
	// Memory layout
	// -------------
	
	// 0x0
	public static final int RUNTIME_CODE_START  		  = 0x0;
	
	public static final int TRAP_ENTRYPOINT						= 0x680;
	// 0x400
	public static final int CODE_ENTRYPOINT     		  = 0x700;
	
	// 0x1000
	public static final int OBJECT_CACHE_TABLE  			= 0x1000;
	public static final int OBJECT_CACHE_TABLE_LENGTH = 0x400 << LOG_BYTES_IN_DOUBLE;
	public static final int OBJECT_CACHE_TABLE_END    = OBJECT_CACHE_TABLE + OBJECT_CACHE_TABLE_LENGTH;
	
	// 0x3000
	public static final int CODE_CACHE_START    		  = OBJECT_CACHE_TABLE_END;
	public static final int CODE_CACHE_LENGTH   		  = 0xD000;
	public static final int CODE_CACHE_END	    		 	= CODE_CACHE_START + CODE_CACHE_LENGTH;

	// 0x10000
	public static final int OBJECT_CACHE_START  			= CODE_CACHE_END;
	public static final int OBJECT_CACHE_LENGTH 			= 0x20000;
	public static final int OBJECT_CACHE_END    		  = OBJECT_CACHE_START + OBJECT_CACHE_LENGTH;

	// 0x30000
	public static final int STATICS_START 			  		= OBJECT_CACHE_END;
	public static final int STATICS_LENGTH				  	= 0x4000;
	public static final int STATICS_END					    	= STATICS_START + STATICS_LENGTH;

	//0x34000
	public static final int CLASS_TIBS_START 			  	= STATICS_END;
	public static final int CLASS_TIBS_LENGTH					= 0x1000;
	public static final int CLASS_TIBS_END				  	= CLASS_TIBS_START + CLASS_TIBS_LENGTH;

	// 0x35000
	public static final int JTOC_TABLE_START					= CLASS_TIBS_END;
	public static final int JTOC_TABLE_LENGTH					= VM_SubArchStatics.SIZE_OF_TABLE;
	public static final int JTOC_TABLE_END						= JTOC_TABLE_START + JTOC_TABLE_LENGTH;
	
	// 0x36000
	public static final int STATICS_TOC               = JTOC_TABLE_END;
	public static final int STATICS_TOC_LENGTH        = 0x1000;
	public static final int STATICS_TOC_END           = STATICS_TOC + STATICS_TOC_LENGTH;

	// 0x37000
	public static final int TIB_TABLE                 = STATICS_TOC_END;
	public static final int TIB_TABLE_LENGTH          = 0x800;
	public static final int TIB_TABLE_END             = TIB_TABLE + TIB_TABLE_LENGTH;

	//0x37800
	public static final int SIZE_STATICS_TABLE        = TIB_TABLE_END;
	public static final int SIZE_STATICS_TABLE_LENGTH = 0x800;
	public static final int SIZE_STATICS_TABLE_END    = SIZE_STATICS_TABLE + SIZE_STATICS_TABLE_LENGTH;
	
	// 0x38000
	public static final int ATOMIC_CACHE_LINE					= SIZE_STATICS_TABLE_END;
	public static final int ATOMIC_CACHE_LINE_LENTH   = 128;  // size of cache line
	public static final int ATOMIC_CACHE_LINE_END     = ATOMIC_CACHE_LINE + ATOMIC_CACHE_LINE_LENTH;
	// 0x38080
	
	// 0x39800
	public static final int STACK_END									= 0x40000 - STACK_SIZE_NORMAL;
	public static final int STACK_BEGIN								= 0x3fff0;
	
	public static final int STATIC_TOC_JTOC_OFF       = STATICS_TOC - (JTOC_TABLE_START + (VM_SubArchStatics.middleOfTable << LOG_BYTES_IN_INT));
	public static final int TIB_TABLE_JTOC_OFF        = TIB_TABLE - (JTOC_TABLE_START + (VM_SubArchStatics.middleOfTable << LOG_BYTES_IN_INT));
	public static final int SIZE_STATICS_JTOC_OFF     = SIZE_STATICS_TABLE - (JTOC_TABLE_START + (VM_SubArchStatics.middleOfTable << LOG_BYTES_IN_INT));
	
}
