package org.jikesrvm;

import org.vmmagic.pragma.Uninterruptible;
import org.vmmagic.unboxed.Address;
import org.vmmagic.unboxed.LocalAddress;
import org.vmmagic.unboxed.WordArray;

@Uninterruptible
public abstract class VM_Registers {

  // set by C hardware exception handler and VM_Runtime.athrow
  // and reset by each implementation of VM_ExceptionDeliverer.deliverException
  //
  public boolean inuse; // do exception registers currently contain live values?

  // The following are used both for thread context switching
  // and for hardware exception reporting/delivery.
  //
  public WordArray gprs; // word size general purpose registers (either 32 or 64 bit)
  public double[] fprs; // 64-bit floating point registers
  public LocalAddress ip; // instruction address register

  
  /**
   * Return framepointer for the deepest stackframe
   */
  public abstract LocalAddress getInnermostFramePointer();

  /**
   * Return next instruction address for the deepest stackframe
   */
  public abstract LocalAddress getInnermostInstructionAddress();
  

  /**
   * set ip & fp. used to control the stack frame at which a scan of
   * the stack during GC will start, for ex., the top java frame for
   * a thread that is blocked in native code during GC.
   */
  public abstract void setInnermost(LocalAddress newip, LocalAddress newfp);
  


  // update the machine state to unwind the deepest stackframe.
  //
  public abstract void unwindStackFrame();
  

  public abstract LocalAddress getIPLocation();
}
