package org.jikesrvm.cellspu;

public interface VM_TrapConstants {

  public static final int TRAP_UNKNOWN = -1;
  public static final int TRAP_NULL_POINTER = 0;
  public static final int TRAP_ARRAY_BOUNDS = 1;
  public static final int TRAP_DIVIDE_BY_ZERO = 2;
  public static final int TRAP_STACK_OVERFLOW = 3;
  public static final int TRAP_CHECKCAST = 4; // opt-compiler
  public static final int TRAP_REGENERATE = 5; // opt-compiler
  public static final int TRAP_JNI_STACK = 6; // jni
  public static final int TRAP_MUST_IMPLEMENT = 7;
  public static final int TRAP_STORE_CHECK = 8; // opt-compiler
  public static final int TRAP_STACK_OVERFLOW_FATAL = 9; // assertion checking
  public static final int TRAP_CODE_CACHE_FULL = 10;
  public static final int TRAP_OBJECT_CACHE_FULL = 11;
  public static final int TRAP_CLASS_TIBS_CACHE_FULL = 12;
  public static final int TRAP_STATIC_CACHE_FULL = 13;
  public static final int TRAP_UNIMPLEMENTED_METHOD = 14;
  public static final int TRAP_CLASS_NOT_RESOLVED_FOR_SUBARCH = 14;
}
