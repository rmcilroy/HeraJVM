package org.jikesrvm.cellspu;

public interface VM_ChannelConstants {

	public static final int MFC_LOCAL_ADDR_CHAN      = 0x10;
	public static final int MFC_HI_MAIN_ADDR_CHAN    = 0x11;
	public static final int MFC_LO_MAIN_ADDR_CHAN    = 0x12;
	public static final int MFC_LIST_ADDR_CHAN       = 0x12;
	public static final int MFC_TRANSFER_SIZE_CHAN   = 0x13;
	public static final int MFC_LIST_SIZE_CHAN			 = 0x13;
	public static final int MFC_TAG_ID_CHAN          = 0x14;
	public static final int MFC_CMD_CHAN             = 0x15;
	public static final int MFC_WRITE_TAG_MASK			 = 0x16;
	public static final int MFC_WRITE_TAG_UPDATE		 = 0x17;
	public static final int MFC_READ_TAG_STATUS		   = 0x18;
	public static final int MFC_READ_LIST_STALL_STAT = 0x19;
	public static final int MFC_WRITE_LIST_STALL_ACK = 0x1A;
	public static final int MFC_READ_ATOMIC_STAT     = 0x1B;
	public static final int SPU_WR_OUT_MBOX					 = 0x1C;
	public static final int SPU_RD_IN_MBOX		       = 0x1D;
	public static final int SPU_WR_OUT_INTR_MBOX     = 0x1E;
	
	public static final int MFC_GET_OPCODE					 = 0x40;
	public static final int MFC_GETL_OPCODE				   = 0x44;
	public static final int MFC_PUT_OPCODE					 = 0x20;
	public static final int MFC_PUTL_OPCODE			     = 0x24;
	public static final int MFC_GETLLAR_OPCODE			 = 0xD0;
	public static final int MFC_PUTLLC_OPCODE				 = 0xB4;
	public static final int MFC_PUTLLUC_OPCODE			 = 0xB0;
	public static final int MFC_PUTQLLUC_OPCODE			 = 0xB8;
}
