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
package org.jikesrvm.compilers.opt;

import org.jikesrvm.*;

/**
 * Class to handle command-line arguments and options for the
 * optimizng compiler.
 * <p>
 * Note: This file is mechanically generated from OPT_Options.template
 *       and MasterOptions.template
 * <p>
 * Note: Boolean options are defined in /Users/ross/Documents/PhD/jikes/jikesrvm_cell/rvm/src-generated/options/BooleanOptions.opt.dat /Users/ross/Documents/PhD/jikes/jikesrvm_cell/rvm/src-generated/options/SharedBooleanOptions.dat
 *       All other options are defined in /Users/ross/Documents/PhD/jikes/jikesrvm_cell/rvm/src-generated/options/ValueOptions.opt.dat /Users/ross/Documents/PhD/jikes/jikesrvm_cell/rvm/src-generated/options/SharedValueOptions.dat
 *       (value, enumeration, bitmask)
 *
 **/
public class OPT_Options implements Cloneable {

  // Non-template instance fields that we don't want
  //  available on the command-line)
  private int OPTIMIZATION_LEVEL = 1;    // The OPT level

  private void printOptionsHeader() {
    VM.sysWrite("Current value of options at optimization level ",OPTIMIZATION_LEVEL, ":\n");
  }

// BEGIN CODE GENERATED FROM MasterOptions.template
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
   // Begin template-specified options
   public boolean LOCAL_CONSTANT_PROP           = true; // Perform local constant propagation
   public boolean LOCAL_COPY_PROP               = true; // Perform local copy propagation
   public boolean LOCAL_CSE                     = true; // Perform local common subexpression elimination
   public boolean GLOBAL_BOUNDS_CHECK           = false; // Perform global Array Bound Check elimination on Demand
   public boolean MONITOR_REMOVAL               = true; // Try to remove unnecessary monitor operations
   public boolean INVOKEE_THREAD_LOCAL          = false; // Compile the method assuming the invokee is thread-local
   public boolean NO_CALLEE_EXCEPTIONS          = false; // Assert that any callee of this compiled method will not throw exceptions?
   public boolean SIMPLE_ESCAPE_IPA             = false; // Eagerly compute method summaries for simple escape analysis
   public boolean FIELD_ANALYSIS                = true; // Eagerly compute method summaries for flow-insensitive field analysis
   public boolean SCALAR_REPLACE_AGGREGATES     = true; // Perform scalar replacement of aggregates
   public boolean REORDER_CODE                  = true; // Reorder basic blocks for improved locality and branch prediction
   public boolean REORDER_CODE_PH               = true; // Reorder basic blocks using Pettis and Hansen Algo2
   public boolean INLINE_NEW                    = true; // Inline allocation of scalars and arrays
   public boolean INLINE_WRITE_BARRIER          = true; // Inline write barriers for generational collectors
   public boolean INLINE                        = true; // Inline statically resolvable calls
   public boolean GUARDED_INLINE                = true; // Guarded inlining of non-final virtual calls
   public boolean GUARDED_INLINE_INTERFACE      = true; // Speculatively inline non-final interface calls
   public boolean STATIC_SPLITTING              = true; // CFG splitting to create hot traces based on static heuristics
   public boolean REDUNDANT_BRANCH_ELIMINATION  = true; // Eliminate redundant conditional branches
   public boolean PREEX_INLINE                  = true; // Pre-existence based inlining
   public boolean SSA                           = false; // Should SSA form be constructed on the HIR?
   public boolean LOAD_ELIMINATION              = false; // Should we perform redundant load elimination during SSA pass?
   public boolean COALESCE_AFTER_SSA            = false; // Should we coalesce move instructions after leaving SSA?
   public boolean EXPRESSION_FOLDING            = false; // Should we try to fold expressions with constants in SSA form?
   public boolean LIVE_RANGE_SPLITTING          = false; // Split live ranges using LIR SSA pass?
   public boolean GCP                           = false; // Perform global code placement
   public boolean GCSE                          = false; // Perform global code placement
   public boolean VERBOSE_GCP                   = false; // Perform noisy global code placement
   public boolean LICM_IGNORE_PEI               = false; // Asume PEIs do not throw or state is not observable
   public boolean TURN_WHILES_INTO_UNTILS       = false; // Turn whiles into untils
   public boolean LOOP_VERSIONING               = false; // Loop versioning
   public boolean HANDLER_LIVENESS              = false; // Store liveness for handlers to improve dependence graph at PEIs
   public boolean SCHEDULE_PREPASS              = false; // Perform prepass instruction scheduling
   public boolean NO_CHECKCAST                  = false; // Should all checkcast operations be (unsafely) eliminated?
   public boolean NO_CHECKSTORE                 = false; // Should all checkstore operations be (unsafely) eliminated?
   public boolean NO_BOUNDS_CHECK               = false; // Should all bounds check operations be (unsafely) eliminated?
   public boolean NO_NULL_CHECK                 = false; // Should all null check operations be (unsafely) eliminated?
   public boolean NO_SYNCHRO                    = false; // Should all synchronization operations be (unsafely) eliminated?
   public boolean NO_THREADS                    = false; // Should all yield points be (unsafely) eliminated?
   public boolean NO_CACHE_FLUSH                = VM.BuildForIA32; // Should cache flush instructions (PowerPC SYNC/ISYNC) be omitted? NOTE: Cannot be correctly changed via the command line!
   public boolean READS_KILL                    = false; // Should we constrain optimizations by enforcing reads-kill?
   public boolean MONITOR_NOP                   = false; // Should we treat all monitorenter/monitorexit bytecodes as nops?
   public boolean STATIC_STATS                  = false; // Should we dump out compile-time statistics for basic blocks?
   public boolean CODE_PATCH_NOP                = false; // Should all patch point be (unsafely) eliminated (at initial HIR)?
   public boolean INSTRUMENTATION_SAMPLING      = false; // Perform code transformation to sample instrumentation code.
   public boolean NO_DUPLICATION                = false; // When performing inst. sampling, should it be done without duplicating code?
   public boolean PROCESSOR_SPECIFIC_COUNTER    = true; // Should there be one CBS counter per processor for SMP performance?
   public boolean REMOVE_YP_FROM_CHECKING       = false; // Should yieldpoints be removed from the checking code (requires finite sample interval)
   public boolean FREQ_FOCUS_EFFORT             = false; // Focus compilation effort based on frequency profile data
   public boolean PRINT_PHASES                  = false; // Print short message for each compilation phase
   public boolean PRINT_ALL_IR                  = false; // Dump the IR after each compiler phase
   public boolean PRINT_DETAILED_INLINE_REPORT  = false; // Print detailed report of compile-time inlining decisions
   public boolean PRINT_INLINE_REPORT           = false; // Print detailed report of compile-time inlining decisions
   public boolean PRINT_DOMINATORS              = false; // Print dominators
   public boolean PRINT_POST_DOMINATORS         = false; // Print post-dominators
   public boolean PRINT_SSA                     = false; // Print SSA form
   public boolean PRINT_DG_BURS                 = false; // Print dependence graph before burs
   public boolean PRINT_DG_SCHED_PRE            = false; // Print dependence graph before prepass scheduling
   public boolean PRINT_DG_SCHED_POST           = false; // Print dependence graph before postpass scheduling
   public boolean PRINT_COALESCING              = false; // Print coalescing output
   public boolean PRINT_HIGH                    = false; // Print IR after initial generation
   public boolean PRINT_FINAL_HIR               = false; // Print IR just before conversion to LIR
   public boolean PRINT_LOW                     = false; // Print IR after conversion to LIR
   public boolean PRINT_FINAL_LIR               = false; // Print IR just before conversion to MIR
   public boolean PRINT_MIR                     = false; // Print IR after conversion to MIR
   public boolean PRINT_FINAL_MIR               = false; // Print IR just before conversion to machine code
   public boolean PRINT_CFG                     = false; // Print control flow graph too when IR is printed
   public boolean PRINT_SCHEDULE_PRE            = false; // Print IR after prepass scheduling
   public boolean PRINT_SCHEDULE_POST           = false; // Print IR after postpass scheduling
   public boolean PRINT_REGALLOC                = false; // Print IR before and after register allocation
   public boolean PRINT_CALLING_CONVENTIONS     = false; // Print IR after expanding calling conventions
   public boolean VCG_DG_BURS                   = false; // Dump dependence graph before BURS in vcg form
   public boolean VCG_DG_SCHED_PRE              = false; // Dump dependence graph before prepass scheduling in vcg form
   public boolean VCG_DG_SCHED_POST             = false; // Dump dependence graph before postpass scheduling in vcg form
   public boolean DEBUG_CODEGEN                 = false; // Enable debugging support for final assembly
   public boolean DEBUG_INSTRU_SAMPLING         = false; // Enable debugging statements for instrumentation sampling
   public boolean DEBUG_INSTRU_SAMPLING_DETAIL  = false; // Enable detailed debugging statements for instrumentation sampling
   public boolean OSR_GUARDED_INLINING          = true; // Insert OSR point at off branch of guarded inlining?
   public boolean OSR_INLINE_POLICY             = true; // Use OSR knowledge to drive more aggressive inlining?
   public boolean PRELOAD_AS_BOOT               = false; // Apply boot options to preload_class
   public boolean PRINT_METHOD                  = false; // Print method name at start of compilation
   public boolean PRINT_MACHINECODE             = false; // Print final machine code
   public int IC_MAX_TARGET_SIZE                = (4*org.jikesrvm.classloader.VM_NormalMethod.CALL_COST-org.jikesrvm.classloader.VM_NormalMethod.SIMPLE_OPERATION_COST); // Static inlining heuristic: Upper bound on callee size
   public int IC_MAX_INLINE_DEPTH               = 5; // Static inlining heuristic: Upper bound on depth of inlining
   public int IC_MAX_ALWAYS_INLINE_TARGET_SIZE  = (2*org.jikesrvm.classloader.VM_NormalMethod.CALL_COST-org.jikesrvm.classloader.VM_NormalMethod.SIMPLE_OPERATION_COST); // Static inlining heuristic: Always inline callees of this size or smaller
   public int IC_MASSIVE_METHOD_SIZE            = 2048; // Static inlining heuristic: If root method is already this big, then only inline trivial methods
   public int AI_MAX_TARGET_SIZE                = (20*org.jikesrvm.classloader.VM_NormalMethod.CALL_COST-org.jikesrvm.classloader.VM_NormalMethod.SIMPLE_OPERATION_COST); // Adaptive inlining heuristic: Upper bound on callee size
   public double AI_MIN_CALLSITE_FRACTION       = 0.4; // Adaptive inlining heuristc: Minimum fraction of callsite distribution for guarded inlining of a callee
   public String EDGE_COUNT_INPUT_FILE          = null; // Input file of edge counter profile data
   public byte INLINING_GUARD                   = IG_CODE_PATCH; // Selection of guard mechanism for inlined virtual calls that cannot be statically bound
   public byte FP_MODE                          = FP_STRICT; // Selection of strictness level for floating point computations
   private java.util.HashSet<String> EXCLUDE          = null; // Exclude methods from being opt compiled
   public int UNROLL_LOG                        = 2; // Unroll loops. Duplicates the loop body 2^n times.
   public int COND_MOVE_CUTOFF                  = 5; // How many extra instructions will we insert in order to remove a conditional branch?
   public int LOAD_ELIMINATION_ROUNDS           = 3; // How many rounds of redundant load elimination will we attempt?
   public String ALLOC_ADVICE_SITES             = null; // Read allocation advice attributes for all classes from this file
   public byte FREQUENCY_STRATEGY               = COUNTERS_FREQ; // How to compute block and edge frequencies?
   public byte SPILL_COST_ESTIMATE              = BLOCK_COUNT_SPILL_COST; // Selection of spilling heuristic
   public float INFREQUENT_THRESHOLD            = 0.01f; // Cumulative threshold which defines the set of infrequent basic blocks
   public double CBS_HOTNESS                    = 0.98; // Threshold at which a conditional branch is considered to be skewed
   public int IR_PRINT_LEVEL                    = 0; // Only print IR compiled above this level
   public String PRELOAD_CLASS                  = null; // Class to preload upon 1st OPT compilation
   private java.util.HashSet<String> METHOD_TO_PRINT  = null; // Only apply print options against methods whose name contains this string
   // End template-specified options

   // Begin generated support for "Enumeration" options
   // INLINING_GUARD
   public static final byte IG_METHOD_TEST = 0;
   public final boolean guardWithMethodTest() { return INLINING_GUARD == IG_METHOD_TEST; }
   public static final byte IG_CLASS_TEST = 1;
   public final boolean guardWithClassTest() { return INLINING_GUARD == IG_CLASS_TEST; }
   public static final byte IG_CODE_PATCH = 2;
   public final boolean guardWithCodePatch() { return INLINING_GUARD == IG_CODE_PATCH; }

   // FP_MODE
   public static final byte FP_STRICT = 0;
   public final boolean strictFP() { return FP_MODE == FP_STRICT; }
   public static final byte FP_ALLOW_FMA = 1;
   public final boolean allowFMA() { return FP_MODE == FP_ALLOW_FMA; }
   public static final byte FP_LOOSE = 2;
   public final boolean allowAssocFP() { return FP_MODE == FP_LOOSE; }

   // FREQUENCY_STRATEGY
   public static final byte COUNTERS_FREQ = 0;
   public final boolean frequencyCounters() { return FREQUENCY_STRATEGY == COUNTERS_FREQ; }
   public static final byte STATIC_FREQ = 1;
   public final boolean staticFrequencyEstimates() { return FREQUENCY_STRATEGY == STATIC_FREQ; }
   public static final byte DUMB_FREQ = 2;
   public final boolean dumbFrequency() { return FREQUENCY_STRATEGY == DUMB_FREQ; }
   public static final byte INVERSE_COUNTERS_FREQ = 3;
   public final boolean inverseFrequencyCounters() { return FREQUENCY_STRATEGY == INVERSE_COUNTERS_FREQ; }

   // SPILL_COST_ESTIMATE
   public static final byte SIMPLE_SPILL_COST = 0;
   public final boolean simpleSpillCost() { return SPILL_COST_ESTIMATE == SIMPLE_SPILL_COST; }
   public static final byte BRAINDEAD_SPILL_COST = 1;
   public final boolean brainDeadSpillCost() { return SPILL_COST_ESTIMATE == BRAINDEAD_SPILL_COST; }
   public static final byte BLOCK_COUNT_SPILL_COST = 2;
   public final boolean blockCountSpillCost() { return SPILL_COST_ESTIMATE == BLOCK_COUNT_SPILL_COST; }

   // End generated support for "Enumeration" options

   // Begin generated support for "Set" options
   // EXCLUDE
   public boolean isEXCLUDE(String q) { return EXCLUDE != null && EXCLUDE.contains(q); }
   public boolean fuzzyMatchEXCLUDE(String q) {
     if (EXCLUDE == null) return false;
     for (final String s : EXCLUDE) {
       if (q.indexOf(s) > -1) return true;
     }
     return false;
   }
   public boolean hasEXCLUDE() { return !(EXCLUDE == null || EXCLUDE.isEmpty()); }
   public java.util.Iterator<String> getEXCLUDEs() {
     if (EXCLUDE == null) {
       return new java.util.Iterator<String>() {
         public boolean hasNext() { return false; }
         public String next() { return null; }
          public void remove() {}
       };
     } else {
       return EXCLUDE.iterator();
     }
   }
   // METHOD_TO_PRINT
   public boolean isMETHOD_TO_PRINT(String q) { return METHOD_TO_PRINT != null && METHOD_TO_PRINT.contains(q); }
   public boolean fuzzyMatchMETHOD_TO_PRINT(String q) {
     if (METHOD_TO_PRINT == null) return false;
     for (final String s : METHOD_TO_PRINT) {
       if (q.indexOf(s) > -1) return true;
     }
     return false;
   }
   public boolean hasMETHOD_TO_PRINT() { return !(METHOD_TO_PRINT == null || METHOD_TO_PRINT.isEmpty()); }
   public java.util.Iterator<String> getMETHOD_TO_PRINTs() {
     if (METHOD_TO_PRINT == null) {
       return new java.util.Iterator<String>() {
         public boolean hasNext() { return false; }
         public String next() { return null; }
          public void remove() {}
       };
     } else {
       return METHOD_TO_PRINT.iterator();
     }
   }
   // End generated support for "Set" options


   @SuppressWarnings("unchecked")
   public Object clone() throws CloneNotSupportedException {
     OPT_Options clone = (OPT_Options)super.clone();
     if (EXCLUDE != null) {
       clone.EXCLUDE = (java.util.HashSet)this.EXCLUDE.clone();
     }
     if (METHOD_TO_PRINT != null) {
       clone.METHOD_TO_PRINT = (java.util.HashSet)this.METHOD_TO_PRINT.clone();
     }
     return clone;
   }

  public OPT_Options dup() {
    try {
      return (OPT_Options) clone();
    }
    catch (CloneNotSupportedException e) {
      final InternalError error = new InternalError("Unexpected CloneNotSupportedException.");
      error.initCause(e);
      throw error;
    }
  }


  /**
   * Take a string (most likely a command-line argument) and try to proccess it
   * as an option command.  Return true if the string was understood, false
   * otherwise.
   *
   * @param prefix a Sring to use as a command prefix when printing help.
   * @param arg a String to try to process as an option command
   * @return true if successful, false otherwise
   */
  @org.vmmagic.pragma.NoOptCompile
  public boolean processAsOption(String prefix, String arg) {

    // First handle the "option commands"
    if (arg.equals("help")) {
       printHelp(prefix);
       return true;
    }
    if (arg.equals("printOptions")) {
       printOptions();
       return true;
    }
    if (arg.length() == 0) {
      printHelp(prefix);
      return true;
    }
    // Make sure only process O? option if initial runtime compiler!
    if ((prefix.indexOf("irc")!=-1 ||
         prefix.indexOf("bc")!=-1 ||
         prefix.indexOf("eoc")!=-1) &&
        instanceProcessAsOption(arg)) {
      return true;
    }

    // Required format of arg is 'name=value'
    // Split into 'name' and 'value' strings
    int split = arg.indexOf('=');
    if (split == -1) {
      if (!(arg.equals("O0") || arg.equals("O1") || arg.equals("O2") || arg.equals("O3"))) {
        VM.sysWrite("  Illegal option specification!\n  \""+arg+
                      "\" must be specified as a name-value pair in the form of option=value\n");
      }
      return false;
    }
    String name = arg.substring(0,split);
    String value = arg.substring(split+1);

    //Begin generated command-line processing
    if (name.equals("local_constant_prop")) {
      if (value.equals("true")) {
        LOCAL_CONSTANT_PROP = true;
        return true;
      } else if (value.equals("false")) {
          LOCAL_CONSTANT_PROP = false;
        return true;
      } else
        return false;
    }
    if (name.equals("local_copy_prop")) {
      if (value.equals("true")) {
        LOCAL_COPY_PROP = true;
        return true;
      } else if (value.equals("false")) {
          LOCAL_COPY_PROP = false;
        return true;
      } else
        return false;
    }
    if (name.equals("local_cse")) {
      if (value.equals("true")) {
        LOCAL_CSE = true;
        return true;
      } else if (value.equals("false")) {
          LOCAL_CSE = false;
        return true;
      } else
        return false;
    }
    if (name.equals("global_bounds")) {
      if (value.equals("true")) {
        GLOBAL_BOUNDS_CHECK = true;
        return true;
      } else if (value.equals("false")) {
          GLOBAL_BOUNDS_CHECK = false;
        return true;
      } else
        return false;
    }
    if (name.equals("monitor_removal")) {
      if (value.equals("true")) {
        MONITOR_REMOVAL = true;
        return true;
      } else if (value.equals("false")) {
          MONITOR_REMOVAL = false;
        return true;
      } else
        return false;
    }
    if (name.equals("invokee_thread_local")) {
      if (value.equals("true")) {
        INVOKEE_THREAD_LOCAL = true;
        return true;
      } else if (value.equals("false")) {
          INVOKEE_THREAD_LOCAL = false;
        return true;
      } else
        return false;
    }
    if (name.equals("no_callee_exceptions")) {
      if (value.equals("true")) {
        NO_CALLEE_EXCEPTIONS = true;
        return true;
      } else if (value.equals("false")) {
          NO_CALLEE_EXCEPTIONS = false;
        return true;
      } else
        return false;
    }
    if (name.equals("simple_escape_ipa")) {
      if (value.equals("true")) {
        SIMPLE_ESCAPE_IPA = true;
        return true;
      } else if (value.equals("false")) {
          SIMPLE_ESCAPE_IPA = false;
        return true;
      } else
        return false;
    }
    if (name.equals("field_analysis")) {
      if (value.equals("true")) {
        FIELD_ANALYSIS = true;
        return true;
      } else if (value.equals("false")) {
          FIELD_ANALYSIS = false;
        return true;
      } else
        return false;
    }
    if (name.equals("scalar_replace_aggregates")) {
      if (value.equals("true")) {
        SCALAR_REPLACE_AGGREGATES = true;
        return true;
      } else if (value.equals("false")) {
          SCALAR_REPLACE_AGGREGATES = false;
        return true;
      } else
        return false;
    }
    if (name.equals("reorder_code")) {
      if (value.equals("true")) {
        REORDER_CODE = true;
        return true;
      } else if (value.equals("false")) {
          REORDER_CODE = false;
        return true;
      } else
        return false;
    }
    if (name.equals("reorder_code_ph")) {
      if (value.equals("true")) {
        REORDER_CODE_PH = true;
        return true;
      } else if (value.equals("false")) {
          REORDER_CODE_PH = false;
        return true;
      } else
        return false;
    }
    if (name.equals("inline_new")) {
      if (value.equals("true")) {
        INLINE_NEW = true;
        return true;
      } else if (value.equals("false")) {
          INLINE_NEW = false;
        return true;
      } else
        return false;
    }
    if (name.equals("inline_write_barrier")) {
      if (value.equals("true")) {
        INLINE_WRITE_BARRIER = true;
        return true;
      } else if (value.equals("false")) {
          INLINE_WRITE_BARRIER = false;
        return true;
      } else
        return false;
    }
    if (name.equals("inline")) {
      if (value.equals("true")) {
        INLINE = true;
        return true;
      } else if (value.equals("false")) {
          INLINE = false;
        return true;
      } else
        return false;
    }
    if (name.equals("guarded_inline")) {
      if (value.equals("true")) {
        GUARDED_INLINE = true;
        return true;
      } else if (value.equals("false")) {
          GUARDED_INLINE = false;
        return true;
      } else
        return false;
    }
    if (name.equals("guarded_inline_interface")) {
      if (value.equals("true")) {
        GUARDED_INLINE_INTERFACE = true;
        return true;
      } else if (value.equals("false")) {
          GUARDED_INLINE_INTERFACE = false;
        return true;
      } else
        return false;
    }
    if (name.equals("static_splitting")) {
      if (value.equals("true")) {
        STATIC_SPLITTING = true;
        return true;
      } else if (value.equals("false")) {
          STATIC_SPLITTING = false;
        return true;
      } else
        return false;
    }
    if (name.equals("redundant_branch_elimination")) {
      if (value.equals("true")) {
        REDUNDANT_BRANCH_ELIMINATION = true;
        return true;
      } else if (value.equals("false")) {
          REDUNDANT_BRANCH_ELIMINATION = false;
        return true;
      } else
        return false;
    }
    if (name.equals("preex_inline")) {
      if (value.equals("true")) {
        PREEX_INLINE = true;
        return true;
      } else if (value.equals("false")) {
          PREEX_INLINE = false;
        return true;
      } else
        return false;
    }
    if (name.equals("ssa")) {
      if (value.equals("true")) {
        SSA = true;
        return true;
      } else if (value.equals("false")) {
          SSA = false;
        return true;
      } else
        return false;
    }
    if (name.equals("load_elimination")) {
      if (value.equals("true")) {
        LOAD_ELIMINATION = true;
        return true;
      } else if (value.equals("false")) {
          LOAD_ELIMINATION = false;
        return true;
      } else
        return false;
    }
    if (name.equals("coalesce_after_ssa")) {
      if (value.equals("true")) {
        COALESCE_AFTER_SSA = true;
        return true;
      } else if (value.equals("false")) {
          COALESCE_AFTER_SSA = false;
        return true;
      } else
        return false;
    }
    if (name.equals("expression_folding")) {
      if (value.equals("true")) {
        EXPRESSION_FOLDING = true;
        return true;
      } else if (value.equals("false")) {
          EXPRESSION_FOLDING = false;
        return true;
      } else
        return false;
    }
    if (name.equals("live_range_splitting")) {
      if (value.equals("true")) {
        LIVE_RANGE_SPLITTING = true;
        return true;
      } else if (value.equals("false")) {
          LIVE_RANGE_SPLITTING = false;
        return true;
      } else
        return false;
    }
    if (name.equals("gcp")) {
      if (value.equals("true")) {
        GCP = true;
        return true;
      } else if (value.equals("false")) {
          GCP = false;
        return true;
      } else
        return false;
    }
    if (name.equals("gcse")) {
      if (value.equals("true")) {
        GCSE = true;
        return true;
      } else if (value.equals("false")) {
          GCSE = false;
        return true;
      } else
        return false;
    }
    if (name.equals("verbose_gcp")) {
      if (value.equals("true")) {
        VERBOSE_GCP = true;
        return true;
      } else if (value.equals("false")) {
          VERBOSE_GCP = false;
        return true;
      } else
        return false;
    }
    if (name.equals("licm_ignore_pei")) {
      if (value.equals("true")) {
        LICM_IGNORE_PEI = true;
        return true;
      } else if (value.equals("false")) {
          LICM_IGNORE_PEI = false;
        return true;
      } else
        return false;
    }
    if (name.equals("unwhile")) {
      if (value.equals("true")) {
        TURN_WHILES_INTO_UNTILS = true;
        return true;
      } else if (value.equals("false")) {
          TURN_WHILES_INTO_UNTILS = false;
        return true;
      } else
        return false;
    }
    if (name.equals("loop_versioning")) {
      if (value.equals("true")) {
        LOOP_VERSIONING = true;
        return true;
      } else if (value.equals("false")) {
          LOOP_VERSIONING = false;
        return true;
      } else
        return false;
    }
    if (name.equals("handler_liveness")) {
      if (value.equals("true")) {
        HANDLER_LIVENESS = true;
        return true;
      } else if (value.equals("false")) {
          HANDLER_LIVENESS = false;
        return true;
      } else
        return false;
    }
    if (name.equals("schedule_prepass")) {
      if (value.equals("true")) {
        SCHEDULE_PREPASS = true;
        return true;
      } else if (value.equals("false")) {
          SCHEDULE_PREPASS = false;
        return true;
      } else
        return false;
    }
    if (name.equals("no_checkcast")) {
      if (value.equals("true")) {
        NO_CHECKCAST = true;
        return true;
      } else if (value.equals("false")) {
          NO_CHECKCAST = false;
        return true;
      } else
        return false;
    }
    if (name.equals("no_checkstore")) {
      if (value.equals("true")) {
        NO_CHECKSTORE = true;
        return true;
      } else if (value.equals("false")) {
          NO_CHECKSTORE = false;
        return true;
      } else
        return false;
    }
    if (name.equals("no_bounds_check")) {
      if (value.equals("true")) {
        NO_BOUNDS_CHECK = true;
        return true;
      } else if (value.equals("false")) {
          NO_BOUNDS_CHECK = false;
        return true;
      } else
        return false;
    }
    if (name.equals("no_null_check")) {
      if (value.equals("true")) {
        NO_NULL_CHECK = true;
        return true;
      } else if (value.equals("false")) {
          NO_NULL_CHECK = false;
        return true;
      } else
        return false;
    }
    if (name.equals("no_synchro")) {
      if (value.equals("true")) {
        NO_SYNCHRO = true;
        return true;
      } else if (value.equals("false")) {
          NO_SYNCHRO = false;
        return true;
      } else
        return false;
    }
    if (name.equals("no_threads")) {
      if (value.equals("true")) {
        NO_THREADS = true;
        return true;
      } else if (value.equals("false")) {
          NO_THREADS = false;
        return true;
      } else
        return false;
    }
    if (name.equals("no_cache_flush")) {
      if (value.equals("true")) {
        NO_CACHE_FLUSH = true;
        return true;
      } else if (value.equals("false")) {
          NO_CACHE_FLUSH = false;
        return true;
      } else
        return false;
    }
    if (name.equals("reads_kill")) {
      if (value.equals("true")) {
        READS_KILL = true;
        return true;
      } else if (value.equals("false")) {
          READS_KILL = false;
        return true;
      } else
        return false;
    }
    if (name.equals("monitor_nop")) {
      if (value.equals("true")) {
        MONITOR_NOP = true;
        return true;
      } else if (value.equals("false")) {
          MONITOR_NOP = false;
        return true;
      } else
        return false;
    }
    if (name.equals("static_stats")) {
      if (value.equals("true")) {
        STATIC_STATS = true;
        return true;
      } else if (value.equals("false")) {
          STATIC_STATS = false;
        return true;
      } else
        return false;
    }
    if (name.equals("code_patch_nop")) {
      if (value.equals("true")) {
        CODE_PATCH_NOP = true;
        return true;
      } else if (value.equals("false")) {
          CODE_PATCH_NOP = false;
        return true;
      } else
        return false;
    }
    if (name.equals("instrumentation_sampling")) {
      if (value.equals("true")) {
        INSTRUMENTATION_SAMPLING = true;
        return true;
      } else if (value.equals("false")) {
          INSTRUMENTATION_SAMPLING = false;
        return true;
      } else
        return false;
    }
    if (name.equals("no_duplication")) {
      if (value.equals("true")) {
        NO_DUPLICATION = true;
        return true;
      } else if (value.equals("false")) {
          NO_DUPLICATION = false;
        return true;
      } else
        return false;
    }
    if (name.equals("processor_specific_counter")) {
      if (value.equals("true")) {
        PROCESSOR_SPECIFIC_COUNTER = true;
        return true;
      } else if (value.equals("false")) {
          PROCESSOR_SPECIFIC_COUNTER = false;
        return true;
      } else
        return false;
    }
    if (name.equals("remove_yp_from_checking")) {
      if (value.equals("true")) {
        REMOVE_YP_FROM_CHECKING = true;
        return true;
      } else if (value.equals("false")) {
          REMOVE_YP_FROM_CHECKING = false;
        return true;
      } else
        return false;
    }
    if (name.equals("focusEffort")) {
      if (value.equals("true")) {
        FREQ_FOCUS_EFFORT = true;
        return true;
      } else if (value.equals("false")) {
          FREQ_FOCUS_EFFORT = false;
        return true;
      } else
        return false;
    }
    if (name.equals("phases")) {
      if (value.equals("true")) {
        PRINT_PHASES = true;
        return true;
      } else if (value.equals("false")) {
          PRINT_PHASES = false;
        return true;
      } else
        return false;
    }
    if (name.equals("print_all_ir")) {
      if (value.equals("true")) {
        PRINT_ALL_IR = true;
        return true;
      } else if (value.equals("false")) {
          PRINT_ALL_IR = false;
        return true;
      } else
        return false;
    }
    if (name.equals("print_detailed_inline_report")) {
      if (value.equals("true")) {
        PRINT_DETAILED_INLINE_REPORT = true;
        return true;
      } else if (value.equals("false")) {
          PRINT_DETAILED_INLINE_REPORT = false;
        return true;
      } else
        return false;
    }
    if (name.equals("print_inline_report")) {
      if (value.equals("true")) {
        PRINT_INLINE_REPORT = true;
        return true;
      } else if (value.equals("false")) {
          PRINT_INLINE_REPORT = false;
        return true;
      } else
        return false;
    }
    if (name.equals("dom")) {
      if (value.equals("true")) {
        PRINT_DOMINATORS = true;
        return true;
      } else if (value.equals("false")) {
          PRINT_DOMINATORS = false;
        return true;
      } else
        return false;
    }
    if (name.equals("pdom")) {
      if (value.equals("true")) {
        PRINT_POST_DOMINATORS = true;
        return true;
      } else if (value.equals("false")) {
          PRINT_POST_DOMINATORS = false;
        return true;
      } else
        return false;
    }
    if (name.equals("print_ssa")) {
      if (value.equals("true")) {
        PRINT_SSA = true;
        return true;
      } else if (value.equals("false")) {
          PRINT_SSA = false;
        return true;
      } else
        return false;
    }
    if (name.equals("print_dg_burs")) {
      if (value.equals("true")) {
        PRINT_DG_BURS = true;
        return true;
      } else if (value.equals("false")) {
          PRINT_DG_BURS = false;
        return true;
      } else
        return false;
    }
    if (name.equals("print_dg_sched_pre")) {
      if (value.equals("true")) {
        PRINT_DG_SCHED_PRE = true;
        return true;
      } else if (value.equals("false")) {
          PRINT_DG_SCHED_PRE = false;
        return true;
      } else
        return false;
    }
    if (name.equals("print_dg_sched_post")) {
      if (value.equals("true")) {
        PRINT_DG_SCHED_POST = true;
        return true;
      } else if (value.equals("false")) {
          PRINT_DG_SCHED_POST = false;
        return true;
      } else
        return false;
    }
    if (name.equals("pcoal")) {
      if (value.equals("true")) {
        PRINT_COALESCING = true;
        return true;
      } else if (value.equals("false")) {
          PRINT_COALESCING = false;
        return true;
      } else
        return false;
    }
    if (name.equals("high")) {
      if (value.equals("true")) {
        PRINT_HIGH = true;
        return true;
      } else if (value.equals("false")) {
          PRINT_HIGH = false;
        return true;
      } else
        return false;
    }
    if (name.equals("final_hir")) {
      if (value.equals("true")) {
        PRINT_FINAL_HIR = true;
        return true;
      } else if (value.equals("false")) {
          PRINT_FINAL_HIR = false;
        return true;
      } else
        return false;
    }
    if (name.equals("low")) {
      if (value.equals("true")) {
        PRINT_LOW = true;
        return true;
      } else if (value.equals("false")) {
          PRINT_LOW = false;
        return true;
      } else
        return false;
    }
    if (name.equals("final_lir")) {
      if (value.equals("true")) {
        PRINT_FINAL_LIR = true;
        return true;
      } else if (value.equals("false")) {
          PRINT_FINAL_LIR = false;
        return true;
      } else
        return false;
    }
    if (name.equals("mir")) {
      if (value.equals("true")) {
        PRINT_MIR = true;
        return true;
      } else if (value.equals("false")) {
          PRINT_MIR = false;
        return true;
      } else
        return false;
    }
    if (name.equals("final_mir")) {
      if (value.equals("true")) {
        PRINT_FINAL_MIR = true;
        return true;
      } else if (value.equals("false")) {
          PRINT_FINAL_MIR = false;
        return true;
      } else
        return false;
    }
    if (name.equals("cfg")) {
      if (value.equals("true")) {
        PRINT_CFG = true;
        return true;
      } else if (value.equals("false")) {
          PRINT_CFG = false;
        return true;
      } else
        return false;
    }
    if (name.equals("print_schedule_pre")) {
      if (value.equals("true")) {
        PRINT_SCHEDULE_PRE = true;
        return true;
      } else if (value.equals("false")) {
          PRINT_SCHEDULE_PRE = false;
        return true;
      } else
        return false;
    }
    if (name.equals("print_schedule_post")) {
      if (value.equals("true")) {
        PRINT_SCHEDULE_POST = true;
        return true;
      } else if (value.equals("false")) {
          PRINT_SCHEDULE_POST = false;
        return true;
      } else
        return false;
    }
    if (name.equals("regalloc")) {
      if (value.equals("true")) {
        PRINT_REGALLOC = true;
        return true;
      } else if (value.equals("false")) {
          PRINT_REGALLOC = false;
        return true;
      } else
        return false;
    }
    if (name.equals("print_calling_conventions")) {
      if (value.equals("true")) {
        PRINT_CALLING_CONVENTIONS = true;
        return true;
      } else if (value.equals("false")) {
          PRINT_CALLING_CONVENTIONS = false;
        return true;
      } else
        return false;
    }
    if (name.equals("vcg_dg_burs")) {
      if (value.equals("true")) {
        VCG_DG_BURS = true;
        return true;
      } else if (value.equals("false")) {
          VCG_DG_BURS = false;
        return true;
      } else
        return false;
    }
    if (name.equals("vcg_dg_sched_pre")) {
      if (value.equals("true")) {
        VCG_DG_SCHED_PRE = true;
        return true;
      } else if (value.equals("false")) {
          VCG_DG_SCHED_PRE = false;
        return true;
      } else
        return false;
    }
    if (name.equals("vcg_dg_sched_post")) {
      if (value.equals("true")) {
        VCG_DG_SCHED_POST = true;
        return true;
      } else if (value.equals("false")) {
          VCG_DG_SCHED_POST = false;
        return true;
      } else
        return false;
    }
    if (name.equals("cgd")) {
      if (value.equals("true")) {
        DEBUG_CODEGEN = true;
        return true;
      } else if (value.equals("false")) {
          DEBUG_CODEGEN = false;
        return true;
      } else
        return false;
    }
    if (name.equals("debug_instru_sampling")) {
      if (value.equals("true")) {
        DEBUG_INSTRU_SAMPLING = true;
        return true;
      } else if (value.equals("false")) {
          DEBUG_INSTRU_SAMPLING = false;
        return true;
      } else
        return false;
    }
    if (name.equals("debug_instru_sampling_detail")) {
      if (value.equals("true")) {
        DEBUG_INSTRU_SAMPLING_DETAIL = true;
        return true;
      } else if (value.equals("false")) {
          DEBUG_INSTRU_SAMPLING_DETAIL = false;
        return true;
      } else
        return false;
    }
    if (name.equals("osr_guarded_inlining")) {
      if (value.equals("true")) {
        OSR_GUARDED_INLINING = true;
        return true;
      } else if (value.equals("false")) {
          OSR_GUARDED_INLINING = false;
        return true;
      } else
        return false;
    }
    if (name.equals("osr_inline_policy")) {
      if (value.equals("true")) {
        OSR_INLINE_POLICY = true;
        return true;
      } else if (value.equals("false")) {
          OSR_INLINE_POLICY = false;
        return true;
      } else
        return false;
    }
    if (name.equals("preload_as_boot")) {
      if (value.equals("true")) {
        PRELOAD_AS_BOOT = true;
        return true;
      } else if (value.equals("false")) {
          PRELOAD_AS_BOOT = false;
        return true;
      } else
        return false;
    }
    if (name.equals("verbose")) {
      if (value.equals("true")) {
        PRINT_METHOD = true;
        return true;
      } else if (value.equals("false")) {
          PRINT_METHOD = false;
        return true;
      } else
        return false;
    }
    if (name.equals("mc")) {
      if (value.equals("true")) {
        PRINT_MACHINECODE = true;
        return true;
      } else if (value.equals("false")) {
          PRINT_MACHINECODE = false;
        return true;
      } else
        return false;
    }
    if (name.equals("ic_max_target_size")) {
       IC_MAX_TARGET_SIZE = VM_CommandLineArgs.primitiveParseInt(value);
       return true;
     }
    if (name.equals("ic_max_inline_depth")) {
       IC_MAX_INLINE_DEPTH = VM_CommandLineArgs.primitiveParseInt(value);
       return true;
     }
    if (name.equals("ic_max_always_inline_target_size")) {
       IC_MAX_ALWAYS_INLINE_TARGET_SIZE = VM_CommandLineArgs.primitiveParseInt(value);
       return true;
     }
    if (name.equals("ic_massive_method_size")) {
       IC_MASSIVE_METHOD_SIZE = VM_CommandLineArgs.primitiveParseInt(value);
       return true;
     }
    if (name.equals("ai_max_target_size")) {
       AI_MAX_TARGET_SIZE = VM_CommandLineArgs.primitiveParseInt(value);
       return true;
     }
    if (name.equals("ai_min_callsite_fraction")) {
          AI_MIN_CALLSITE_FRACTION = VM_CommandLineArgs.primitiveParseFloat(value);
       return true;
     }
    if (name.equals("edge_count_input_file")) {
       EDGE_COUNT_INPUT_FILE = value;
       return true;
     }
    if (name.equals("inlining_guard")) {
       if (value.equals("ig_method_test")) {
         INLINING_GUARD = IG_METHOD_TEST;
         return true;
       }
       if (value.equals("ig_class_test")) {
         INLINING_GUARD = IG_CLASS_TEST;
         return true;
       }
       if (value.equals("ig_code_patch")) {
         INLINING_GUARD = IG_CODE_PATCH;
         return true;
       }
       return false;
     }
    if (name.equals("fp_mode")) {
       if (value.equals("strict")) {
         FP_MODE = FP_STRICT;
         return true;
       }
       if (value.equals("allow_fma")) {
         FP_MODE = FP_ALLOW_FMA;
         return true;
       }
       if (value.equals("allow_assoc")) {
         FP_MODE = FP_LOOSE;
         return true;
       }
       return false;
     }
    if (name.equals("exclude")) {
       if (EXCLUDE == null) {
         EXCLUDE = new java.util.HashSet<String>();
       }
       EXCLUDE.add(value);
       return true;
     }
    if (name.equals("unroll_log")) {
       UNROLL_LOG = VM_CommandLineArgs.primitiveParseInt(value);
       return true;
     }
    if (name.equals("cond_move_cutoff")) {
       COND_MOVE_CUTOFF = VM_CommandLineArgs.primitiveParseInt(value);
       return true;
     }
    if (name.equals("load_elimination_rounds")) {
       LOAD_ELIMINATION_ROUNDS = VM_CommandLineArgs.primitiveParseInt(value);
       return true;
     }
    if (name.equals("alloc_advice_sites")) {
       ALLOC_ADVICE_SITES = value;
       return true;
     }
    if (name.equals("frequency_strategy")) {
       if (value.equals("counters")) {
         FREQUENCY_STRATEGY = COUNTERS_FREQ;
         return true;
       }
       if (value.equals("static")) {
         FREQUENCY_STRATEGY = STATIC_FREQ;
         return true;
       }
       if (value.equals("dumb")) {
         FREQUENCY_STRATEGY = DUMB_FREQ;
         return true;
       }
       if (value.equals("inverse")) {
         FREQUENCY_STRATEGY = INVERSE_COUNTERS_FREQ;
         return true;
       }
       return false;
     }
    if (name.equals("spill_cost_estimate")) {
       if (value.equals("simple")) {
         SPILL_COST_ESTIMATE = SIMPLE_SPILL_COST;
         return true;
       }
       if (value.equals("brainDead")) {
         SPILL_COST_ESTIMATE = BRAINDEAD_SPILL_COST;
         return true;
       }
       if (value.equals("blockCount")) {
         SPILL_COST_ESTIMATE = BLOCK_COUNT_SPILL_COST;
         return true;
       }
       return false;
     }
    if (name.equals("infrequent_threshold")) {
            INFREQUENT_THRESHOLD = VM_CommandLineArgs.primitiveParseFloat(value);
       return true;
     }
    if (name.equals("cbs_hotness")) {
          CBS_HOTNESS = VM_CommandLineArgs.primitiveParseFloat(value);
       return true;
     }
    if (name.equals("ir_print_level")) {
       IR_PRINT_LEVEL = VM_CommandLineArgs.primitiveParseInt(value);
       return true;
     }
    if (name.equals("preload_class")) {
       PRELOAD_CLASS = value;
       return true;
     }
    if (name.equals("method_to_print")) {
       if (METHOD_TO_PRINT == null) {
         METHOD_TO_PRINT = new java.util.HashSet<String>();
       }
       METHOD_TO_PRINT.add(value);
       return true;
     }
       //End generated command-line processing

    // None of the above tests matched, so this wasn't an option
    return false;
  }

  // Print a short description of every option
  public static void printHelp(String prefix) {

    instancePrintHelpHeader(prefix);

    //Begin generated help messages
    VM.sysWrite("Boolean Options ("+prefix+"<option>=true or "+prefix+":<option>=false)\n");
    VM.sysWrite("Option                        OptLevel Description\n");
    VM.sysWrite("local_constant_prop            0       Perform local constant propagation\n");
    VM.sysWrite("local_copy_prop                0       Perform local copy propagation\n");
    VM.sysWrite("local_cse                      0       Perform local common subexpression elimination\n");
    VM.sysWrite("global_bounds                          Perform global Array Bound Check elimination on Demand\n");
    VM.sysWrite("monitor_removal                1       Try to remove unnecessary monitor operations\n");
    VM.sysWrite("invokee_thread_local                   Compile the method assuming the invokee is thread-local\n");
    VM.sysWrite("no_callee_exceptions                   Assert that any callee of this compiled method will not throw exceptions?\n");
    VM.sysWrite("simple_escape_ipa                      Eagerly compute method summaries for simple escape analysis\n");
    VM.sysWrite("field_analysis                 0       Eagerly compute method summaries for flow-insensitive field analysis\n");
    VM.sysWrite("scalar_replace_aggregates      1       Perform scalar replacement of aggregates\n");
    VM.sysWrite("reorder_code                   0       Reorder basic blocks for improved locality and branch prediction\n");
    VM.sysWrite("reorder_code_ph                1       Reorder basic blocks using Pettis and Hansen Algo2\n");
    VM.sysWrite("inline_new                     0       Inline allocation of scalars and arrays\n");
    VM.sysWrite("inline_write_barrier           1       Inline write barriers for generational collectors\n");
    VM.sysWrite("inline                         0       Inline statically resolvable calls\n");
    VM.sysWrite("guarded_inline                 0       Guarded inlining of non-final virtual calls\n");
    VM.sysWrite("guarded_inline_interface       0       Speculatively inline non-final interface calls\n");
    VM.sysWrite("static_splitting               1       CFG splitting to create hot traces based on static heuristics\n");
    VM.sysWrite("redundant_branch_elimination   3       Eliminate redundant conditional branches\n");
    VM.sysWrite("preex_inline                   0       Pre-existence based inlining\n");
    VM.sysWrite("ssa                            3       Should SSA form be constructed on the HIR?\n");
    VM.sysWrite("load_elimination               3       Should we perform redundant load elimination during SSA pass?\n");
    VM.sysWrite("coalesce_after_ssa             3       Should we coalesce move instructions after leaving SSA?\n");
    VM.sysWrite("expression_folding             3       Should we try to fold expressions with constants in SSA form?\n");
    VM.sysWrite("live_range_splitting                   Split live ranges using LIR SSA pass?\n");
    VM.sysWrite("gcp                            3       Perform global code placement\n");
    VM.sysWrite("gcse                           3       Perform global code placement\n");
    VM.sysWrite("verbose_gcp                            Perform noisy global code placement\n");
    VM.sysWrite("licm_ignore_pei                        Asume PEIs do not throw or state is not observable\n");
    VM.sysWrite("unwhile                        3       Turn whiles into untils\n");
    VM.sysWrite("loop_versioning                        Loop versioning\n");
    VM.sysWrite("handler_liveness               3       Store liveness for handlers to improve dependence graph at PEIs\n");
    VM.sysWrite("schedule_prepass                       Perform prepass instruction scheduling\n");
    VM.sysWrite("no_checkcast                           Should all checkcast operations be (unsafely) eliminated?\n");
    VM.sysWrite("no_checkstore                          Should all checkstore operations be (unsafely) eliminated?\n");
    VM.sysWrite("no_bounds_check                        Should all bounds check operations be (unsafely) eliminated?\n");
    VM.sysWrite("no_null_check                          Should all null check operations be (unsafely) eliminated?\n");
    VM.sysWrite("no_synchro                             Should all synchronization operations be (unsafely) eliminated?\n");
    VM.sysWrite("no_threads                             Should all yield points be (unsafely) eliminated?\n");
    VM.sysWrite("no_cache_flush                         Should cache flush instructions (PowerPC SYNC/ISYNC) be omitted? NOTE: Cannot be correctly changed via the command line!\n");
    VM.sysWrite("reads_kill                             Should we constrain optimizations by enforcing reads-kill?\n");
    VM.sysWrite("monitor_nop                            Should we treat all monitorenter/monitorexit bytecodes as nops?\n");
    VM.sysWrite("static_stats                           Should we dump out compile-time statistics for basic blocks?\n");
    VM.sysWrite("code_patch_nop                         Should all patch point be (unsafely) eliminated (at initial HIR)?\n");
    VM.sysWrite("instrumentation_sampling               Perform code transformation to sample instrumentation code.\n");
    VM.sysWrite("no_duplication                         When performing inst. sampling, should it be done without duplicating code?\n");
    VM.sysWrite("processor_specific_counter             Should there be one CBS counter per processor for SMP performance?\n");
    VM.sysWrite("remove_yp_from_checking                Should yieldpoints be removed from the checking code (requires finite sample interval)\n");
    VM.sysWrite("focusEffort                            Focus compilation effort based on frequency profile data\n");
    VM.sysWrite("phases                                 Print short message for each compilation phase\n");
    VM.sysWrite("print_all_ir                           Dump the IR after each compiler phase\n");
    VM.sysWrite("print_detailed_inline_report           Print detailed report of compile-time inlining decisions\n");
    VM.sysWrite("print_inline_report                    Print detailed report of compile-time inlining decisions\n");
    VM.sysWrite("dom                                    Print dominators\n");
    VM.sysWrite("pdom                                   Print post-dominators\n");
    VM.sysWrite("print_ssa                              Print SSA form\n");
    VM.sysWrite("print_dg_burs                          Print dependence graph before burs\n");
    VM.sysWrite("print_dg_sched_pre                     Print dependence graph before prepass scheduling\n");
    VM.sysWrite("print_dg_sched_post                    Print dependence graph before postpass scheduling\n");
    VM.sysWrite("pcoal                                  Print coalescing output\n");
    VM.sysWrite("high                                   Print IR after initial generation\n");
    VM.sysWrite("final_hir                              Print IR just before conversion to LIR\n");
    VM.sysWrite("low                                    Print IR after conversion to LIR\n");
    VM.sysWrite("final_lir                              Print IR just before conversion to MIR\n");
    VM.sysWrite("mir                                    Print IR after conversion to MIR\n");
    VM.sysWrite("final_mir                              Print IR just before conversion to machine code\n");
    VM.sysWrite("cfg                                    Print control flow graph too when IR is printed\n");
    VM.sysWrite("print_schedule_pre                     Print IR after prepass scheduling\n");
    VM.sysWrite("print_schedule_post                    Print IR after postpass scheduling\n");
    VM.sysWrite("regalloc                               Print IR before and after register allocation\n");
    VM.sysWrite("print_calling_conventions              Print IR after expanding calling conventions\n");
    VM.sysWrite("vcg_dg_burs                            Dump dependence graph before BURS in vcg form\n");
    VM.sysWrite("vcg_dg_sched_pre                       Dump dependence graph before prepass scheduling in vcg form\n");
    VM.sysWrite("vcg_dg_sched_post                      Dump dependence graph before postpass scheduling in vcg form\n");
    VM.sysWrite("cgd                                    Enable debugging support for final assembly\n");
    VM.sysWrite("debug_instru_sampling                  Enable debugging statements for instrumentation sampling\n");
    VM.sysWrite("debug_instru_sampling_detail           Enable detailed debugging statements for instrumentation sampling\n");
    VM.sysWrite("osr_guarded_inlining           1       Insert OSR point at off branch of guarded inlining?\n");
    VM.sysWrite("osr_inline_policy              1       Use OSR knowledge to drive more aggressive inlining?\n");
    VM.sysWrite("preload_as_boot                        Apply boot options to preload_class\n");
    VM.sysWrite("verbose                                Print method name at start of compilation\n");
    VM.sysWrite("mc                                     Print final machine code\n");
    VM.sysWrite("\nValue Options ("+prefix+"<option>=<value>)\n");
    VM.sysWrite("Option                         Type    Description\n");
    VM.sysWrite("ic_max_target_size             int     Static inlining heuristic: Upper bound on callee size\n");
    VM.sysWrite("ic_max_inline_depth            int     Static inlining heuristic: Upper bound on depth of inlining\n");
    VM.sysWrite("ic_max_always_inline_target_si int     Static inlining heuristic: Always inline callees of this size or smaller\n");
    VM.sysWrite("ic_massive_method_size         int     Static inlining heuristic: If root method is already this big, then only inline trivial methods\n");
    VM.sysWrite("ai_max_target_size             int     Adaptive inlining heuristic: Upper bound on callee size\n");
    VM.sysWrite("ai_min_callsite_fraction       double  Adaptive inlining heuristc: Minimum fraction of callsite distribution for guarded inlining of a callee\n");
    VM.sysWrite("edge_count_input_file          String  Input file of edge counter profile data\n");
    VM.sysWrite("unroll_log                     int     Unroll loops. Duplicates the loop body 2^n times.\n");
    VM.sysWrite("cond_move_cutoff               int     How many extra instructions will we insert in order to remove a conditional branch?\n");
    VM.sysWrite("load_elimination_rounds        int     How many rounds of redundant load elimination will we attempt?\n");
    VM.sysWrite("alloc_advice_sites             String  Read allocation advice attributes for all classes from this file\n");
    VM.sysWrite("infrequent_threshold           float   Cumulative threshold which defines the set of infrequent basic blocks\n");
    VM.sysWrite("cbs_hotness                    double  Threshold at which a conditional branch is considered to be skewed\n");
    VM.sysWrite("ir_print_level                 int     Only print IR compiled above this level\n");
    VM.sysWrite("preload_class                  String  Class to preload upon 1st OPT compilation\n");
    VM.sysWrite("\nSelection Options (set option to one of an enumeration of possible values)\n");
    VM.sysWrite("\t\tSelection of guard mechanism for inlined virtual calls that cannot be statically bound\n");
    VM.sysWrite("inlining_guard                ");
    VM.sysWrite("ig_method_test ");
    VM.sysWrite("ig_class_test ");
    VM.sysWrite("ig_code_patch ");
    VM.sysWrite("\n");
    VM.sysWrite("\t\tSelection of strictness level for floating point computations\n");
    VM.sysWrite("fp_mode                       ");
    VM.sysWrite("strict ");
    VM.sysWrite("allow_fma ");
    VM.sysWrite("allow_assoc ");
    VM.sysWrite("\n");
    VM.sysWrite("\t\tHow to compute block and edge frequencies?\n");
    VM.sysWrite("frequency_strategy            ");
    VM.sysWrite("counters ");
    VM.sysWrite("static ");
    VM.sysWrite("dumb ");
    VM.sysWrite("inverse ");
    VM.sysWrite("\n");
    VM.sysWrite("\t\tSelection of spilling heuristic\n");
    VM.sysWrite("spill_cost_estimate           ");
    VM.sysWrite("simple ");
    VM.sysWrite("brainDead ");
    VM.sysWrite("blockCount ");
    VM.sysWrite("\n");
    VM.sysWrite("\nSet Options (option is a set of values)\n");
    VM.sysWrite("exclude                        Exclude methods from being opt compiled\n");
    VM.sysWrite("method_to_print                Only apply print options against methods whose name contains this string\n");
    instancePrintHelpFooter(prefix);

    VM.sysExit(VM.EXIT_STATUS_PRINTED_HELP_MESSAGE);
  }

  // print the options values
  @org.vmmagic.pragma.NoOptCompile
  public String toString() {
    StringBuilder result = new StringBuilder();

    // Begin generated option value printing
    result.append("\tlocal_constant_prop            = ").append(LOCAL_CONSTANT_PROP).append("\n");
    result.append("\tlocal_copy_prop                = ").append(LOCAL_COPY_PROP).append("\n");
    result.append("\tlocal_cse                      = ").append(LOCAL_CSE).append("\n");
    result.append("\tglobal_bounds                  = ").append(GLOBAL_BOUNDS_CHECK).append("\n");
    result.append("\tmonitor_removal                = ").append(MONITOR_REMOVAL).append("\n");
    result.append("\tinvokee_thread_local           = ").append(INVOKEE_THREAD_LOCAL).append("\n");
    result.append("\tno_callee_exceptions           = ").append(NO_CALLEE_EXCEPTIONS).append("\n");
    result.append("\tsimple_escape_ipa              = ").append(SIMPLE_ESCAPE_IPA).append("\n");
    result.append("\tfield_analysis                 = ").append(FIELD_ANALYSIS).append("\n");
    result.append("\tscalar_replace_aggregates      = ").append(SCALAR_REPLACE_AGGREGATES).append("\n");
    result.append("\treorder_code                   = ").append(REORDER_CODE).append("\n");
    result.append("\treorder_code_ph                = ").append(REORDER_CODE_PH).append("\n");
    result.append("\tinline_new                     = ").append(INLINE_NEW).append("\n");
    result.append("\tinline_write_barrier           = ").append(INLINE_WRITE_BARRIER).append("\n");
    result.append("\tinline                         = ").append(INLINE).append("\n");
    result.append("\tguarded_inline                 = ").append(GUARDED_INLINE).append("\n");
    result.append("\tguarded_inline_interface       = ").append(GUARDED_INLINE_INTERFACE).append("\n");
    result.append("\tstatic_splitting               = ").append(STATIC_SPLITTING).append("\n");
    result.append("\tredundant_branch_elimination   = ").append(REDUNDANT_BRANCH_ELIMINATION).append("\n");
    result.append("\tpreex_inline                   = ").append(PREEX_INLINE).append("\n");
    result.append("\tssa                            = ").append(SSA).append("\n");
    result.append("\tload_elimination               = ").append(LOAD_ELIMINATION).append("\n");
    result.append("\tcoalesce_after_ssa             = ").append(COALESCE_AFTER_SSA).append("\n");
    result.append("\texpression_folding             = ").append(EXPRESSION_FOLDING).append("\n");
    result.append("\tlive_range_splitting           = ").append(LIVE_RANGE_SPLITTING).append("\n");
    result.append("\tgcp                            = ").append(GCP).append("\n");
    result.append("\tgcse                           = ").append(GCSE).append("\n");
    result.append("\tverbose_gcp                    = ").append(VERBOSE_GCP).append("\n");
    result.append("\tlicm_ignore_pei                = ").append(LICM_IGNORE_PEI).append("\n");
    result.append("\tunwhile                        = ").append(TURN_WHILES_INTO_UNTILS).append("\n");
    result.append("\tloop_versioning                = ").append(LOOP_VERSIONING).append("\n");
    result.append("\thandler_liveness               = ").append(HANDLER_LIVENESS).append("\n");
    result.append("\tschedule_prepass               = ").append(SCHEDULE_PREPASS).append("\n");
    result.append("\tno_checkcast                   = ").append(NO_CHECKCAST).append("\n");
    result.append("\tno_checkstore                  = ").append(NO_CHECKSTORE).append("\n");
    result.append("\tno_bounds_check                = ").append(NO_BOUNDS_CHECK).append("\n");
    result.append("\tno_null_check                  = ").append(NO_NULL_CHECK).append("\n");
    result.append("\tno_synchro                     = ").append(NO_SYNCHRO).append("\n");
    result.append("\tno_threads                     = ").append(NO_THREADS).append("\n");
    result.append("\tno_cache_flush                 = ").append(NO_CACHE_FLUSH).append("\n");
    result.append("\treads_kill                     = ").append(READS_KILL).append("\n");
    result.append("\tmonitor_nop                    = ").append(MONITOR_NOP).append("\n");
    result.append("\tstatic_stats                   = ").append(STATIC_STATS).append("\n");
    result.append("\tcode_patch_nop                 = ").append(CODE_PATCH_NOP).append("\n");
    result.append("\tinstrumentation_sampling       = ").append(INSTRUMENTATION_SAMPLING).append("\n");
    result.append("\tno_duplication                 = ").append(NO_DUPLICATION).append("\n");
    result.append("\tprocessor_specific_counter     = ").append(PROCESSOR_SPECIFIC_COUNTER).append("\n");
    result.append("\tremove_yp_from_checking        = ").append(REMOVE_YP_FROM_CHECKING).append("\n");
    result.append("\tfocusEffort                    = ").append(FREQ_FOCUS_EFFORT).append("\n");
    result.append("\tphases                         = ").append(PRINT_PHASES).append("\n");
    result.append("\tprint_all_ir                   = ").append(PRINT_ALL_IR).append("\n");
    result.append("\tprint_detailed_inline_report   = ").append(PRINT_DETAILED_INLINE_REPORT).append("\n");
    result.append("\tprint_inline_report            = ").append(PRINT_INLINE_REPORT).append("\n");
    result.append("\tdom                            = ").append(PRINT_DOMINATORS).append("\n");
    result.append("\tpdom                           = ").append(PRINT_POST_DOMINATORS).append("\n");
    result.append("\tprint_ssa                      = ").append(PRINT_SSA).append("\n");
    result.append("\tprint_dg_burs                  = ").append(PRINT_DG_BURS).append("\n");
    result.append("\tprint_dg_sched_pre             = ").append(PRINT_DG_SCHED_PRE).append("\n");
    result.append("\tprint_dg_sched_post            = ").append(PRINT_DG_SCHED_POST).append("\n");
    result.append("\tpcoal                          = ").append(PRINT_COALESCING).append("\n");
    result.append("\thigh                           = ").append(PRINT_HIGH).append("\n");
    result.append("\tfinal_hir                      = ").append(PRINT_FINAL_HIR).append("\n");
    result.append("\tlow                            = ").append(PRINT_LOW).append("\n");
    result.append("\tfinal_lir                      = ").append(PRINT_FINAL_LIR).append("\n");
    result.append("\tmir                            = ").append(PRINT_MIR).append("\n");
    result.append("\tfinal_mir                      = ").append(PRINT_FINAL_MIR).append("\n");
    result.append("\tcfg                            = ").append(PRINT_CFG).append("\n");
    result.append("\tprint_schedule_pre             = ").append(PRINT_SCHEDULE_PRE).append("\n");
    result.append("\tprint_schedule_post            = ").append(PRINT_SCHEDULE_POST).append("\n");
    result.append("\tregalloc                       = ").append(PRINT_REGALLOC).append("\n");
    result.append("\tprint_calling_conventions      = ").append(PRINT_CALLING_CONVENTIONS).append("\n");
    result.append("\tvcg_dg_burs                    = ").append(VCG_DG_BURS).append("\n");
    result.append("\tvcg_dg_sched_pre               = ").append(VCG_DG_SCHED_PRE).append("\n");
    result.append("\tvcg_dg_sched_post              = ").append(VCG_DG_SCHED_POST).append("\n");
    result.append("\tcgd                            = ").append(DEBUG_CODEGEN).append("\n");
    result.append("\tdebug_instru_sampling          = ").append(DEBUG_INSTRU_SAMPLING).append("\n");
    result.append("\tdebug_instru_sampling_detail   = ").append(DEBUG_INSTRU_SAMPLING_DETAIL).append("\n");
    result.append("\tosr_guarded_inlining           = ").append(OSR_GUARDED_INLINING).append("\n");
    result.append("\tosr_inline_policy              = ").append(OSR_INLINE_POLICY).append("\n");
    result.append("\tpreload_as_boot                = ").append(PRELOAD_AS_BOOT).append("\n");
    result.append("\tverbose                        = ").append(PRINT_METHOD).append("\n");
    result.append("\tmc                             = ").append(PRINT_MACHINECODE).append("\n");
    result.append("\tic_max_target_size             = ").append(IC_MAX_TARGET_SIZE).append("\n");
    result.append("\tic_max_inline_depth            = ").append(IC_MAX_INLINE_DEPTH).append("\n");
    result.append("\tic_max_always_inline_target_si = ").append(IC_MAX_ALWAYS_INLINE_TARGET_SIZE).append("\n");
    result.append("\tic_massive_method_size         = ").append(IC_MASSIVE_METHOD_SIZE).append("\n");
    result.append("\tai_max_target_size             = ").append(AI_MAX_TARGET_SIZE).append("\n");
    result.append("\tai_min_callsite_fraction       = ").append(AI_MIN_CALLSITE_FRACTION).append("\n");
    result.append("\tedge_count_input_file          = ").append(EDGE_COUNT_INPUT_FILE).append("\n");
    result.append("\tunroll_log                     = ").append(UNROLL_LOG).append("\n");
    result.append("\tcond_move_cutoff               = ").append(COND_MOVE_CUTOFF).append("\n");
    result.append("\tload_elimination_rounds        = ").append(LOAD_ELIMINATION_ROUNDS).append("\n");
    result.append("\talloc_advice_sites             = ").append(ALLOC_ADVICE_SITES).append("\n");
    result.append("\tinfrequent_threshold           = ").append(INFREQUENT_THRESHOLD).append("\n");
    result.append("\tcbs_hotness                    = ").append(CBS_HOTNESS).append("\n");
    result.append("\tir_print_level                 = ").append(IR_PRINT_LEVEL).append("\n");
    result.append("\tpreload_class                  = ").append(PRELOAD_CLASS).append("\n");
    if (INLINING_GUARD == IG_METHOD_TEST)
       result.append("\tinlining_guard                 = IG_METHOD_TEST").append("\n");
    if (INLINING_GUARD == IG_CLASS_TEST)
       result.append("\tinlining_guard                 = IG_CLASS_TEST").append("\n");
    if (INLINING_GUARD == IG_CODE_PATCH)
       result.append("\tinlining_guard                 = IG_CODE_PATCH").append("\n");
    if (FP_MODE == FP_STRICT)
       result.append("\tfp_mode                        = FP_STRICT").append("\n");
    if (FP_MODE == FP_ALLOW_FMA)
       result.append("\tfp_mode                        = FP_ALLOW_FMA").append("\n");
    if (FP_MODE == FP_LOOSE)
       result.append("\tfp_mode                        = FP_LOOSE").append("\n");
    if (FREQUENCY_STRATEGY == COUNTERS_FREQ)
       result.append("\tfrequency_strategy             = COUNTERS_FREQ").append("\n");
    if (FREQUENCY_STRATEGY == STATIC_FREQ)
       result.append("\tfrequency_strategy             = STATIC_FREQ").append("\n");
    if (FREQUENCY_STRATEGY == DUMB_FREQ)
       result.append("\tfrequency_strategy             = DUMB_FREQ").append("\n");
    if (FREQUENCY_STRATEGY == INVERSE_COUNTERS_FREQ)
       result.append("\tfrequency_strategy             = INVERSE_COUNTERS_FREQ").append("\n");
    if (SPILL_COST_ESTIMATE == SIMPLE_SPILL_COST)
       result.append("\tspill_cost_estimate            = SIMPLE_SPILL_COST").append("\n");
    if (SPILL_COST_ESTIMATE == BRAINDEAD_SPILL_COST)
       result.append("\tspill_cost_estimate            = BRAINDEAD_SPILL_COST").append("\n");
    if (SPILL_COST_ESTIMATE == BLOCK_COUNT_SPILL_COST)
       result.append("\tspill_cost_estimate            = BLOCK_COUNT_SPILL_COST").append("\n");
    {
      String val = (EXCLUDE==null)?"[]":EXCLUDE.toString();
      result.append("\texclude                        = ").append(val).append("\n");
    }
    {
      String val = (METHOD_TO_PRINT==null)?"[]":METHOD_TO_PRINT.toString();
      result.append("\tmethod_to_print                = ").append(val).append("\n");
    }
    return result.toString();
    //End generated toString()
  }
  // return a String value of this options object
  @org.vmmagic.pragma.NoOptCompile
  public void printOptions() {
    printOptionsHeader();

    // Begin generated option value printing
    VM.sysWriteln("\tlocal_constant_prop            = ",LOCAL_CONSTANT_PROP);
    VM.sysWriteln("\tlocal_copy_prop                = ",LOCAL_COPY_PROP);
    VM.sysWriteln("\tlocal_cse                      = ",LOCAL_CSE);
    VM.sysWriteln("\tglobal_bounds                  = ",GLOBAL_BOUNDS_CHECK);
    VM.sysWriteln("\tmonitor_removal                = ",MONITOR_REMOVAL);
    VM.sysWriteln("\tinvokee_thread_local           = ",INVOKEE_THREAD_LOCAL);
    VM.sysWriteln("\tno_callee_exceptions           = ",NO_CALLEE_EXCEPTIONS);
    VM.sysWriteln("\tsimple_escape_ipa              = ",SIMPLE_ESCAPE_IPA);
    VM.sysWriteln("\tfield_analysis                 = ",FIELD_ANALYSIS);
    VM.sysWriteln("\tscalar_replace_aggregates      = ",SCALAR_REPLACE_AGGREGATES);
    VM.sysWriteln("\treorder_code                   = ",REORDER_CODE);
    VM.sysWriteln("\treorder_code_ph                = ",REORDER_CODE_PH);
    VM.sysWriteln("\tinline_new                     = ",INLINE_NEW);
    VM.sysWriteln("\tinline_write_barrier           = ",INLINE_WRITE_BARRIER);
    VM.sysWriteln("\tinline                         = ",INLINE);
    VM.sysWriteln("\tguarded_inline                 = ",GUARDED_INLINE);
    VM.sysWriteln("\tguarded_inline_interface       = ",GUARDED_INLINE_INTERFACE);
    VM.sysWriteln("\tstatic_splitting               = ",STATIC_SPLITTING);
    VM.sysWriteln("\tredundant_branch_elimination   = ",REDUNDANT_BRANCH_ELIMINATION);
    VM.sysWriteln("\tpreex_inline                   = ",PREEX_INLINE);
    VM.sysWriteln("\tssa                            = ",SSA);
    VM.sysWriteln("\tload_elimination               = ",LOAD_ELIMINATION);
    VM.sysWriteln("\tcoalesce_after_ssa             = ",COALESCE_AFTER_SSA);
    VM.sysWriteln("\texpression_folding             = ",EXPRESSION_FOLDING);
    VM.sysWriteln("\tlive_range_splitting           = ",LIVE_RANGE_SPLITTING);
    VM.sysWriteln("\tgcp                            = ",GCP);
    VM.sysWriteln("\tgcse                           = ",GCSE);
    VM.sysWriteln("\tverbose_gcp                    = ",VERBOSE_GCP);
    VM.sysWriteln("\tlicm_ignore_pei                = ",LICM_IGNORE_PEI);
    VM.sysWriteln("\tunwhile                        = ",TURN_WHILES_INTO_UNTILS);
    VM.sysWriteln("\tloop_versioning                = ",LOOP_VERSIONING);
    VM.sysWriteln("\thandler_liveness               = ",HANDLER_LIVENESS);
    VM.sysWriteln("\tschedule_prepass               = ",SCHEDULE_PREPASS);
    VM.sysWriteln("\tno_checkcast                   = ",NO_CHECKCAST);
    VM.sysWriteln("\tno_checkstore                  = ",NO_CHECKSTORE);
    VM.sysWriteln("\tno_bounds_check                = ",NO_BOUNDS_CHECK);
    VM.sysWriteln("\tno_null_check                  = ",NO_NULL_CHECK);
    VM.sysWriteln("\tno_synchro                     = ",NO_SYNCHRO);
    VM.sysWriteln("\tno_threads                     = ",NO_THREADS);
    VM.sysWriteln("\tno_cache_flush                 = ",NO_CACHE_FLUSH);
    VM.sysWriteln("\treads_kill                     = ",READS_KILL);
    VM.sysWriteln("\tmonitor_nop                    = ",MONITOR_NOP);
    VM.sysWriteln("\tstatic_stats                   = ",STATIC_STATS);
    VM.sysWriteln("\tcode_patch_nop                 = ",CODE_PATCH_NOP);
    VM.sysWriteln("\tinstrumentation_sampling       = ",INSTRUMENTATION_SAMPLING);
    VM.sysWriteln("\tno_duplication                 = ",NO_DUPLICATION);
    VM.sysWriteln("\tprocessor_specific_counter     = ",PROCESSOR_SPECIFIC_COUNTER);
    VM.sysWriteln("\tremove_yp_from_checking        = ",REMOVE_YP_FROM_CHECKING);
    VM.sysWriteln("\tfocusEffort                    = ",FREQ_FOCUS_EFFORT);
    VM.sysWriteln("\tphases                         = ",PRINT_PHASES);
    VM.sysWriteln("\tprint_all_ir                   = ",PRINT_ALL_IR);
    VM.sysWriteln("\tprint_detailed_inline_report   = ",PRINT_DETAILED_INLINE_REPORT);
    VM.sysWriteln("\tprint_inline_report            = ",PRINT_INLINE_REPORT);
    VM.sysWriteln("\tdom                            = ",PRINT_DOMINATORS);
    VM.sysWriteln("\tpdom                           = ",PRINT_POST_DOMINATORS);
    VM.sysWriteln("\tprint_ssa                      = ",PRINT_SSA);
    VM.sysWriteln("\tprint_dg_burs                  = ",PRINT_DG_BURS);
    VM.sysWriteln("\tprint_dg_sched_pre             = ",PRINT_DG_SCHED_PRE);
    VM.sysWriteln("\tprint_dg_sched_post            = ",PRINT_DG_SCHED_POST);
    VM.sysWriteln("\tpcoal                          = ",PRINT_COALESCING);
    VM.sysWriteln("\thigh                           = ",PRINT_HIGH);
    VM.sysWriteln("\tfinal_hir                      = ",PRINT_FINAL_HIR);
    VM.sysWriteln("\tlow                            = ",PRINT_LOW);
    VM.sysWriteln("\tfinal_lir                      = ",PRINT_FINAL_LIR);
    VM.sysWriteln("\tmir                            = ",PRINT_MIR);
    VM.sysWriteln("\tfinal_mir                      = ",PRINT_FINAL_MIR);
    VM.sysWriteln("\tcfg                            = ",PRINT_CFG);
    VM.sysWriteln("\tprint_schedule_pre             = ",PRINT_SCHEDULE_PRE);
    VM.sysWriteln("\tprint_schedule_post            = ",PRINT_SCHEDULE_POST);
    VM.sysWriteln("\tregalloc                       = ",PRINT_REGALLOC);
    VM.sysWriteln("\tprint_calling_conventions      = ",PRINT_CALLING_CONVENTIONS);
    VM.sysWriteln("\tvcg_dg_burs                    = ",VCG_DG_BURS);
    VM.sysWriteln("\tvcg_dg_sched_pre               = ",VCG_DG_SCHED_PRE);
    VM.sysWriteln("\tvcg_dg_sched_post              = ",VCG_DG_SCHED_POST);
    VM.sysWriteln("\tcgd                            = ",DEBUG_CODEGEN);
    VM.sysWriteln("\tdebug_instru_sampling          = ",DEBUG_INSTRU_SAMPLING);
    VM.sysWriteln("\tdebug_instru_sampling_detail   = ",DEBUG_INSTRU_SAMPLING_DETAIL);
    VM.sysWriteln("\tosr_guarded_inlining           = ",OSR_GUARDED_INLINING);
    VM.sysWriteln("\tosr_inline_policy              = ",OSR_INLINE_POLICY);
    VM.sysWriteln("\tpreload_as_boot                = ",PRELOAD_AS_BOOT);
    VM.sysWriteln("\tverbose                        = ",PRINT_METHOD);
    VM.sysWriteln("\tmc                             = ",PRINT_MACHINECODE);
    VM.sysWriteln("\tic_max_target_size             = ",IC_MAX_TARGET_SIZE);
    VM.sysWriteln("\tic_max_inline_depth            = ",IC_MAX_INLINE_DEPTH);
    VM.sysWriteln("\tic_max_always_inline_target_si = ",IC_MAX_ALWAYS_INLINE_TARGET_SIZE);
    VM.sysWriteln("\tic_massive_method_size         = ",IC_MASSIVE_METHOD_SIZE);
    VM.sysWriteln("\tai_max_target_size             = ",AI_MAX_TARGET_SIZE);
    VM.sysWriteln("\tai_min_callsite_fraction       = ",AI_MIN_CALLSITE_FRACTION);
    VM.sysWriteln("\tedge_count_input_file          = ",EDGE_COUNT_INPUT_FILE);
    VM.sysWriteln("\tunroll_log                     = ",UNROLL_LOG);
    VM.sysWriteln("\tcond_move_cutoff               = ",COND_MOVE_CUTOFF);
    VM.sysWriteln("\tload_elimination_rounds        = ",LOAD_ELIMINATION_ROUNDS);
    VM.sysWriteln("\talloc_advice_sites             = ",ALLOC_ADVICE_SITES);
    VM.sysWriteln("\tinfrequent_threshold           = ",INFREQUENT_THRESHOLD);
    VM.sysWriteln("\tcbs_hotness                    = ",CBS_HOTNESS);
    VM.sysWriteln("\tir_print_level                 = ",IR_PRINT_LEVEL);
    VM.sysWriteln("\tpreload_class                  = ",PRELOAD_CLASS);
    if (INLINING_GUARD == IG_METHOD_TEST)
       VM.sysWriteln("\tinlining_guard                 = IG_METHOD_TEST");
    if (INLINING_GUARD == IG_CLASS_TEST)
       VM.sysWriteln("\tinlining_guard                 = IG_CLASS_TEST");
    if (INLINING_GUARD == IG_CODE_PATCH)
       VM.sysWriteln("\tinlining_guard                 = IG_CODE_PATCH");
    if (FP_MODE == FP_STRICT)
       VM.sysWriteln("\tfp_mode                        = FP_STRICT");
    if (FP_MODE == FP_ALLOW_FMA)
       VM.sysWriteln("\tfp_mode                        = FP_ALLOW_FMA");
    if (FP_MODE == FP_LOOSE)
       VM.sysWriteln("\tfp_mode                        = FP_LOOSE");
    if (FREQUENCY_STRATEGY == COUNTERS_FREQ)
       VM.sysWriteln("\tfrequency_strategy             = COUNTERS_FREQ");
    if (FREQUENCY_STRATEGY == STATIC_FREQ)
       VM.sysWriteln("\tfrequency_strategy             = STATIC_FREQ");
    if (FREQUENCY_STRATEGY == DUMB_FREQ)
       VM.sysWriteln("\tfrequency_strategy             = DUMB_FREQ");
    if (FREQUENCY_STRATEGY == INVERSE_COUNTERS_FREQ)
       VM.sysWriteln("\tfrequency_strategy             = INVERSE_COUNTERS_FREQ");
    if (SPILL_COST_ESTIMATE == SIMPLE_SPILL_COST)
       VM.sysWriteln("\tspill_cost_estimate            = SIMPLE_SPILL_COST");
    if (SPILL_COST_ESTIMATE == BRAINDEAD_SPILL_COST)
       VM.sysWriteln("\tspill_cost_estimate            = BRAINDEAD_SPILL_COST");
    if (SPILL_COST_ESTIMATE == BLOCK_COUNT_SPILL_COST)
       VM.sysWriteln("\tspill_cost_estimate            = BLOCK_COUNT_SPILL_COST");
    {
      String val = (EXCLUDE==null)?"[]":EXCLUDE.toString();
      VM.sysWriteln("\texclude                        = ", val);
    }
    {
      String val = (METHOD_TO_PRINT==null)?"[]":METHOD_TO_PRINT.toString();
      VM.sysWriteln("\tmethod_to_print                = ", val);
    }
    //End generated option value printing
  }
// END CODE GENERATED FROM MasterOptions.template

  private boolean instanceProcessAsOption(String arg) {
    if (arg.startsWith("O")) {
      try {
        setOptLevel(Integer.parseInt(arg.substring(1)));
      } catch (NumberFormatException e) {
        return false;
      }
      return true;
    }
    return false;
  }

  private static void instancePrintHelpHeader(String prefix) {
    VM.sysWrite("Commands\n");
    VM.sysWrite(prefix+"[help]\t\t\tPrint brief description of opt compiler's command-line arguments\n");
    VM.sysWrite(prefix+"printOptions\t\tPrint the current values of opt compiler options\n");
    if (prefix.indexOf("irc")!=-1 || prefix.indexOf("bc")!=-1 || prefix.indexOf("eoc")!=-1) {
      VM.sysWrite(prefix+"O0\t\t\tSelect optimization level 0, minimal optimizations\n");
      VM.sysWrite(prefix+"O1\t\t\tSelect optimization level 1, modest optimizations\n");
      VM.sysWrite(prefix+"O2\t\t\tSelect optimization level 2\n");
    }
    VM.sysWrite("\n");
  }


  private static void instancePrintHelpFooter(String prefix) {
  }


  // accessor to get OPT level
  public int getOptLevel() {
    return OPTIMIZATION_LEVEL;
  }

  // Set the options to encode the optimizations enabled at the given opt label
  // and disabled all optimizations that are not enabled at the given opt label
  public void setOptLevel(int level) {
     OPTIMIZATION_LEVEL = level;
     // Begin generated opt-level logic
     if (level >= 0)
        LOCAL_CONSTANT_PROP = true;
     else
        LOCAL_CONSTANT_PROP = false;
     if (level >= 0)
        LOCAL_COPY_PROP = true;
     else
        LOCAL_COPY_PROP = false;
     if (level >= 0)
        LOCAL_CSE = true;
     else
        LOCAL_CSE = false;
     if (level >= 1)
        MONITOR_REMOVAL = true;
     else
        MONITOR_REMOVAL = false;
     if (level >= 0)
        FIELD_ANALYSIS = true;
     else
        FIELD_ANALYSIS = false;
     if (level >= 1)
        SCALAR_REPLACE_AGGREGATES = true;
     else
        SCALAR_REPLACE_AGGREGATES = false;
     if (level >= 0)
        REORDER_CODE = true;
     else
        REORDER_CODE = false;
     if (level >= 1)
        REORDER_CODE_PH = true;
     else
        REORDER_CODE_PH = false;
     if (level >= 0)
        INLINE_NEW = true;
     else
        INLINE_NEW = false;
     if (level >= 1)
        INLINE_WRITE_BARRIER = true;
     else
        INLINE_WRITE_BARRIER = false;
     if (level >= 0)
        INLINE = true;
     else
        INLINE = false;
     if (level >= 0)
        GUARDED_INLINE = true;
     else
        GUARDED_INLINE = false;
     if (level >= 0)
        GUARDED_INLINE_INTERFACE = true;
     else
        GUARDED_INLINE_INTERFACE = false;
     if (level >= 1)
        STATIC_SPLITTING = true;
     else
        STATIC_SPLITTING = false;
     if (level >= 3)
        REDUNDANT_BRANCH_ELIMINATION = true;
     else
        REDUNDANT_BRANCH_ELIMINATION = false;
     if (level >= 0)
        PREEX_INLINE = true;
     else
        PREEX_INLINE = false;
     if (level >= 3)
        SSA = true;
     else
        SSA = false;
     if (level >= 3)
        LOAD_ELIMINATION = true;
     else
        LOAD_ELIMINATION = false;
     if (level >= 3)
        COALESCE_AFTER_SSA = true;
     else
        COALESCE_AFTER_SSA = false;
     if (level >= 3)
        EXPRESSION_FOLDING = true;
     else
        EXPRESSION_FOLDING = false;
     if (level >= 3)
        GCP = true;
     else
        GCP = false;
     if (level >= 3)
        GCSE = true;
     else
        GCSE = false;
     if (level >= 3)
        TURN_WHILES_INTO_UNTILS = true;
     else
        TURN_WHILES_INTO_UNTILS = false;
     if (level >= 3)
        HANDLER_LIVENESS = true;
     else
        HANDLER_LIVENESS = false;
     if (level >= 1)
        OSR_GUARDED_INLINING = true;
     else
        OSR_GUARDED_INLINING = false;
     if (level >= 1)
        OSR_INLINE_POLICY = true;
     else
        OSR_INLINE_POLICY = false;
     // End generated opt-level logic
  }
}
