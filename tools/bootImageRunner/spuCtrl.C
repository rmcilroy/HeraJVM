#include   <stdlib.h>
#include   <stdio.h>
#include   <string.h>
#include   <malloc.h>
#include   <errno.h>
#include   <libspe2.h>
#include   <pthread.h>
#include   <unistd.h>

/* Interface to virtual machine data structures. */
#define NEED_BOOT_RECORD_DECLARATIONS
#define NEED_VIRTUAL_MACHINE_DECLARATIONS
#define NEED_EXIT_STATUS_CODES
#define NEED_MM_INTERFACE_DECLARATIONS
#include <InterfaceDeclarations.h>
extern "C" void setLinkage(VM_BootRecord *);
extern "C" void sysConsoleWriteChar(unsigned value);
extern "C" void sysConsoleWriteInteger(int value, int hexToo);
extern "C" void sysConsoleWriteLong(long long value, int hexToo);
extern "C" void sysConsoleWriteDouble(double value,  int postDecimalDigits);

#include   "spuCtrl.h"

extern spe_program_handle_t cellspu_bootloader;

#define MAX_SPU_THREADS 16
#define CTX_FLAGS SPE_MAP_PS | SPE_EVENTS_ENABLE

/* Internal data structures */
struct ctrl_thread_args {
	int no_spus;
	struct VM_SubArchBootRecord * boot_record;
	struct VM_BootRecord * main_boot_record;
};

/* Global data */
pthread_t ctrl_thread;
pthread_cond_t exit_signal;
pthread_mutex_t exit_mutex;
SpuThreadData * global_spu_data;

/* Functions */

/* Spu context thread entry point */
void * spu_bootstrap_thread(void *arg) {
	spe_context_ptr_t ctx;
	unsigned int entry = SPE_DEFAULT_ENTRY;
	ctx = *((spe_context_ptr_t *)arg);

	if (spe_context_run(ctx, &entry, 0, NULL, NULL, NULL) < 0) {
		perror ("SPU could not start or quit in failure");
		exit (1);
	}
	pthread_exit(NULL);
}

// implemented below
void * spu_support_thread(void *arg);


/* Start the Spu threads */
void startSpuThreads(int spu_threads, SpuThreadData * spu_data) {

	int i, no_spus;

	/* Determine the number of SPE threads to create */
  no_spus = spe_cpu_info_get(SPE_COUNT_USABLE_SPES, -1);

	if (spu_threads < 0) {
		spu_threads = no_spus;
	} else if (no_spus < spu_threads) {
		spu_threads = no_spus;
		printf("Warning: Only %i Cell SPU processors available\n", spu_threads);
	}

	spu_data->no_spu_threads = spu_threads;
	spu_data->spus = (SpuData *) malloc(sizeof(SpuData) * spu_threads);
	
	if ((spu_data->spus == NULL)) {
		perror("Failed to allocate SPU data for threads");
	}

	printf("Bringing up %i Cell SPU threads\n", spu_threads);

	/* create the context gang */
	if ((spu_data->gang = spe_gang_context_create(0)) == NULL) {
		perror("Failed creating Cell SPU gang context");
		exit(1);
	}

	for(i=0; i<spu_threads; i++) {
		/* Create context */
		if ((spu_data->spus[i].ctx = spe_context_create (CTX_FLAGS, spu_data->gang)) == NULL) {
			perror ("Failed creating Cell SPU context");
			exit (1);
		}

		/* load bootloader into spu's */
		if (spe_program_load (spu_data->spus[i].ctx, &cellspu_bootloader)) {
			perror ("Failed loading Cell SPU bootloader");
			exit (1);
		}

		/* create a thread for each SPU */
		if (pthread_create (&(spu_data->spus[i].boot_thread),
												NULL,
												&spu_bootstrap_thread,
												&(spu_data->spus[i].ctx))) {
			perror ("Failed creating Cell SPU thread");
			exit (1);
		}
	}
}


/* Initialize java subarch thread data */
void initJavaThreads(SpuThreadData * spu_data, int no_threads) {
	int i;
	
	spu_data->threads = (SpuJavaThreadData *) malloc(no_threads * sizeof (SpuJavaThreadData));

	for (i=0; i<no_threads; i++) {
		spu_data->threads[i].in_use   = 0;
		spu_data->threads[i].complete = 0;
		spu_data->threads[i].next = NULL;
	}

	spu_data->workToDo = NULL;
	pthread_mutex_init(&spu_data->lock, NULL); 
  pthread_cond_init (&spu_data->condVar, NULL);
}


/* Register the event handlers for each context */
void registerEventHandlers(SpuThreadData * spu_data) {
	int i, no_spus;
	no_spus = spu_data->no_spu_threads;
	
	for (i=0; i<no_spus; i++) {
		spe_event_unit_t event;

		spu_data->spus[i].evnt_handler = spe_event_handler_create();
		event.events  = SPE_EVENT_OUT_INTR_MBOX;
		event.spe     = spu_data->spus[i].ctx;

		if (spe_event_handler_register(spu_data->spus[i].evnt_handler, &event)) {
			perror("Failed registering event handler for Cell SPU");
			exit(1);
		}
	}
}

/* Get the physical id of the SPU */
int getPhysID(unsigned long long tSpeID, unsigned long long tGroup, int tPid)
{
	int tPhysID = -1;
	char fname[80];
	FILE *fp;

	sprintf(fname, "/spu/gang-%d-%lld/spethread-%d-%lld/phys-id",
					tPid,
					tGroup,
					tPid,
					tSpeID);
	fp = fopen( fname, "r");
	if (fp != NULL) {
		char line[80];
		fread( line, sizeof line, 1, fp);
		tPhysID = strtol(line, (char**)NULL, 0);
		fclose(fp);
	}
	
	return tPhysID;
}

/* Register the event handlers for each context */
void waitForSpus(SpuThreadData * spu_data) {
	int i, no_spus;
	pid_t pid;
	no_spus = spu_data->no_spu_threads;
	pid = getpid();

	// used to order reorder spu's by physical id
	unsigned int phys_ids [no_spus];

	for (i=0; i<no_spus; i++) {		
		unsigned int message_data [2];
		unsigned long long spu_id;
			
		if (spe_out_intr_mbox_read(spu_data->spus[i].ctx, 
															 message_data,
															 2, 
															 SPE_MBOX_ALL_BLOCKING) < 0) {
			perror("Failed reading SPU mailbox while awaiting SPU boot");
			exit(1);
		}

		spu_id = ((unsigned long long)message_data[0]) << 32 | message_data[1];

		// find out the physical spu number for each thread
		phys_ids[i] = getPhysID(spu_id, (unsigned long long)spu_data->gang, pid);
		spu_data->spus[i].phys_id = phys_ids[i];
	
		if (spe_in_mbox_write(spu_data->spus[i].ctx,
													&(phys_ids[i]),
													1,
													SPE_MBOX_ALL_BLOCKING) < 0) {
			perror("Failed writing phys_id to SPU mailbox while SPU was booting");
			exit(1);
		}
	}		
	
	// order ctxs and threads by phys id
	SpuData * new_spus = (SpuData *) malloc(sizeof(SpuData) * no_spus);
	for (i=0; i<no_spus; i++) {	
		int j, current_idx;
		unsigned int current;
		current = 0xefffffff;
		current_idx = 0;
		for (j=0; j<no_spus; j++) {
			if (phys_ids[j] < current) {
				current = phys_ids[j];
				current_idx = j;
			}
		}
		memcpy(&(new_spus[i]), &(spu_data->spus[current_idx]), sizeof(SpuData));
		phys_ids[current_idx] = 0xefffffff;
	}
	free(spu_data->spus);
	spu_data->spus = new_spus;
}


void loadTocTables(SpuThreadData * spu_data, int spu_no) {

	int error = 0;

	error |= spe_mfcio_get(spu_data->spus[spu_no].ctx, 
												 TIB_TABLE,
												 (void *)spu_data->boot_record->classTOCsTable,
												 TIB_TABLE_LENGTH, PROXY_TAG_GROUP, 0, 0); 

	error |= spe_mfcio_get(spu_data->spus[spu_no].ctx, 
												 SIZE_STATICS_TABLE,
												 (void *)spu_data->boot_record->staticsSizeTable,
												 SIZE_STATICS_TABLE_LENGTH, PROXY_TAG_GROUP, 0, 0);

	if (error) {
		fprintf(stderr, "Error loading toc tables\n");
		exit(1);
	}
}

/* Load essential runtime code onto the SPUs */
void * loadRuntimeOnSpus(SpuThreadData * spu_data) {
	int i, no_spus;
	no_spus = spu_data->no_spu_threads;

	char * jtocPtr = (char * ) (spu_data->boot_record->jtocStart + 
															spu_data->boot_record->jtocMiddleOffset);
	int jtocNumOff = spu_data->boot_record->jtocNumericOffset;
	int jtocRefOff = spu_data->boot_record->jtocReferenceOffset;
	spu_data->boot_record->jtocLastCachedNumericOffset   = (uint32_t)jtocNumOff;
	spu_data->boot_record->jtocLastCachedReferenceOffset = (uint32_t)jtocRefOff;
	spu_data->boot_record->jtocDirty = 0;

	char * jtocStart = FLOOR16(jtocPtr + jtocNumOff);
	char * jtocEnd   = CEIL16(jtocPtr + jtocRefOff);
	int jtocLength   = (int)(jtocEnd - jtocStart);
	
	// copy data across to each spu
	for (i=0; i<no_spus; i++) {
		int error = 0;

		error |= spe_mfcio_get(spu_data->spus[i].ctx, 
													 RUNTIME_CODE_START,
													 (void *) spu_data->boot_record->oolRuntimeCodeInstructions,
													 spu_data->boot_record->oolRuntimeCodeLength,
													 PROXY_TAG_GROUP, 0, 0);

		if ((RUNTIME_CODE_START + spu_data->boot_record->oolRuntimeCodeLength) >= CODE_ENTRYPOINT) {
			fprintf(stderr, "Error, Cell SPU ool runtime code too long\n");
			exit(1);
		}

		// copy across the runtime entry method
	  error |= spe_mfcio_get(spu_data->spus[i].ctx, 
													 CODE_ENTRYPOINT,
													 (void *) spu_data->boot_record->runtimeEntryMethod,
													 spu_data->boot_record->runtimeEntryLength,
		  											 PROXY_TAG_GROUP, 0, 0);
		
		if ((CODE_ENTRYPOINT + spu_data->boot_record->runtimeEntryLength) >= CODE_ENTRYPOINT_END) {
			fprintf(stderr, "Error, Cell SPU code entrypoint is too long\n");
			exit(1);
		}


		// copy the JTOC across
		error |= spe_mfcio_get(spu_data->spus[i].ctx,
													 FLOOR16(JTOC_PTR + jtocNumOff),
													 jtocStart,
													 jtocLength,
													 PROXY_TAG_GROUP, 0, 0);


		spu_data->spus[i].jtocStart = jtocPtr + jtocNumOff;
		spu_data->spus[i].jtocEnd   = jtocPtr + jtocRefOff;

		loadTocTables(spu_data, i);

		if (error) {
			perror("Failed loading runtime on Cell SPUs");
			exit(1);
		}
	}

	// wait for each SPU dma to complete
	for (i=0; i<no_spus; i++) {
		unsigned int status, write_complete;

		if (spe_mfcio_tag_status_read(spu_data->spus[i].ctx, 
																	PROXY_TAG_GROUP_BM,
																	SPE_TAG_ALL,
																	&status)) {
			perror("Failed while waiting for DMA copy of runtime to Cell SPUs");
			exit(1);
		}

		write_complete = RUNTIME_COPY_COMPLETE;

		// tell spu to jump to java runtime
		if (spe_in_mbox_write(spu_data->spus[i].ctx,
													&write_complete,
													1,
													SPE_MBOX_ANY_NONBLOCKING) < 0) {
			perror("Failed writing phys_id to SPU mailbox while SPU was booting");
			exit(1);
		}

		spu_data->spus[i].jtocDirty = 0;
		spu_data->spus[i].in_use = 0;
	}
}

void waitForRuntime(SpuThreadData * spu_data) {
	int i;
	unsigned int message_data[2];
	
	for (i=0; i<spu_data->no_spu_threads; i++) {
		if (spe_out_intr_mbox_read(spu_data->spus[i].ctx, 
															 message_data,
															 2, 
															 SPE_MBOX_ALL_BLOCKING) < 0) {
			perror("Failed reading SPU mailbox while awaiting SPU boot");
			exit(1);
		}

		if ((message_data[0] == JAVA_VM_STARTED) &&
				message_data[1] == spu_data->spus[i].phys_id) { 
			//	printf("SPU %i Java VM started\n", spu_data->spus[i].phys_id);
		} else {
			printf("message_data[0] - 0x%x, message_data[1] - 0x%x\n",message_data[0], message_data[1]);
			fprintf(stderr, "SPU %i did not succecfully transition to Java VM\n",
							spu_data->spus[i].phys_id);
			exit(1);
		}
	}
}

void startSpuSupportThreads(SpuThreadData * spu_data) {
	int i;
	for (i=0; i<spu_data->no_spu_threads; i++) {
		if (pthread_create (&(spu_data->spus[i].support_thread),
												NULL,
												&spu_support_thread,
												(void*)i)) {
			perror ("Failed creating Cell SPU thread");
			exit (1);
		}
	}
}

/* Spu control function */
void * spuCtrlFunc(void *arg) {
	int i;
	SpuThreadData * spu_data;
	struct ctrl_thread_args * ctrlArgs = *((struct ctrl_thread_args **) ctrlArgs);
	
	if ((spu_data = (SpuThreadData *) malloc(sizeof(SpuThreadData))) == NULL) {
		perror("Failed to allocate SPU data\n");
	}

	spu_data->boot_record = ctrlArgs->boot_record;
	spu_data->main_boot_record = ctrlArgs->main_boot_record;

	// initialize threads
	startSpuThreads(ctrlArgs->no_spus, spu_data);
	initJavaThreads(spu_data, MAX_JAVA_SPU_THREADS);

	free(ctrlArgs);

	// register interest in events
	registerEventHandlers(spu_data);

	// wait for the threads to signal that they have started
	waitForSpus(spu_data);

	// transfer runtime code to spus
	loadRuntimeOnSpus(spu_data);

	// wait for SPU to transition to Java runtime
	waitForRuntime(spu_data);

	global_spu_data = spu_data;

	// start support threads
	startSpuSupportThreads(spu_data);

	// signal completion of SPU boot process
	spu_data->boot_record->noSubArchProcs = spu_data->no_spu_threads;
	spu_data->boot_record->subArchBootComplete = 1;

	// now sit waiting for the kill signal
	pthread_mutex_lock(&exit_mutex);
	pthread_cond_wait(&exit_signal, &exit_mutex);
  pthread_mutex_unlock(&exit_mutex);

	/* shutdown other threads */		
	for (i=0; i<spu_data->no_spu_threads; i++) {
		if (pthread_cancel (spu_data->spus[i].boot_thread)) {
			perror("Failed to shutdown Cell SPU thread");
			exit (1);
		}
		
		if (pthread_cancel (spu_data->spus[i].support_thread)) {
			perror("Failed to shutdown Cell SPU support thread");
			exit (1);
		}

		// destroy resources
		spe_event_handler_destroy(spu_data->spus[i].evnt_handler);
		spe_context_destroy(spu_data->spus[i].ctx);
	}
	spe_gang_context_destroy(spu_data->gang);
}

/* Initialise the SPU control thread and start the SPUs */
int initSpuCtrl(int no_spus, 
								struct VM_SubArchBootRecord * subArchBootRecord,
								struct VM_BootRecord * bootRecord) {
	struct ctrl_thread_args * args = (struct ctrl_thread_args *) malloc(sizeof(struct ctrl_thread_args));

	args->no_spus = no_spus;
	args->boot_record = subArchBootRecord;
	args->main_boot_record = bootRecord;

	pthread_cond_init(&exit_signal, NULL);
	pthread_mutex_init(&exit_mutex, NULL);

	if (pthread_create (&ctrl_thread,
											NULL,
											&spuCtrlFunc,
											&args)) {
		perror ("Failed creating Cell SPU control thread");
		exit (1);
	}
}


/* Stop the Spu Threads */
void stopSpuThreads() {
	if (global_spu_data != NULL) {
		pthread_mutex_lock(&exit_mutex);
		pthread_cond_signal(&exit_signal);
		pthread_mutex_unlock(&exit_mutex);

		if (pthread_join (ctrl_thread, NULL)) {
			perror("Failed while waiting for Cell SPU control thread to exit");
			exit (1);
		}
	}
}

/* select a Thread ID that is not currently inuse */
int chooseThreadID(SpuThreadData * spu_data) {
	int i;

	for (i=0; i<MAX_JAVA_SPU_THREADS; i++) {
		if (!spu_data->threads[i].in_use) {
			spu_data->threads[i].in_use = 1;
			spu_data->threads[i].complete = 0;
			return i;
		}
	}

	fprintf(stderr, "No free SPU Threads\n");
	exit(1);
}

/* select an SPU that is not currently inuse */
int releaseThread(SpuThreadData * spu_data, int thread_id) {
	spu_data->threads[thread_id].in_use = 0;
}

void reloadJtoc(SpuThreadData * spu_data, int spu_no) {
	int i, error;

	error = 0;

	// check if JTOC needs reloaded
	if (spu_data->boot_record->jtocDirty) {
		for (i=0; i<spu_data->no_spu_threads; i++) {
			spu_data->spus[i].jtocDirty = 1;
		}
	}
	if (spu_data->spus[spu_no].jtocDirty) {



		char * jtocPtr = (char * ) (spu_data->boot_record->jtocStart + 
																spu_data->boot_record->jtocMiddleOffset);
		int jtocNumOff = spu_data->boot_record->jtocNumericOffset;
		int jtocRefOff = spu_data->boot_record->jtocReferenceOffset;
		spu_data->boot_record->jtocLastCachedNumericOffset   = (uint32_t)jtocNumOff;
		spu_data->boot_record->jtocLastCachedReferenceOffset = (uint32_t)jtocRefOff;
		spu_data->boot_record->jtocDirty = 0;
	

		// copy across the JTOC updates - numerics
		char * jtocStart = (jtocPtr + jtocNumOff);
		char * oldJtocStart = spu_data->spus[spu_no].jtocStart;

		if ((jtocStart < oldJtocStart) && 
				!LONG_BOUNDARY(jtocStart)) {
			error |= spe_mfcio_get(spu_data->spus[spu_no].ctx, 
														 JTOC_PTR - (jtocPtr - jtocStart),
														 jtocStart,
														 4, PROXY_TAG_GROUP, 0, 0);
			jtocStart += 4;
		}
		if ((jtocStart < oldJtocStart) && 
				!QUAD_BOUNDARY(jtocStart)) {

			int cpLen = ((jtocStart + 4) == oldJtocStart) ? 4 : 8;

			error |= spe_mfcio_get(spu_data->spus[spu_no].ctx, 
														 JTOC_PTR - (jtocPtr - jtocStart),
														 jtocStart,
														 cpLen, PROXY_TAG_GROUP, 0, 0);
			jtocStart += cpLen;
		}
		if ((jtocStart < oldJtocStart) && 
				!LONG_BOUNDARY(oldJtocStart)) {

			oldJtocStart -= 4;
			error |= spe_mfcio_get(spu_data->spus[spu_no].ctx, 
														 JTOC_PTR - (jtocPtr - oldJtocStart),
														 oldJtocStart,
														 4, PROXY_TAG_GROUP, 0, 0);
		}
		if ((jtocStart < oldJtocStart) && 
				!QUAD_BOUNDARY(oldJtocStart)) {

			int cpLen = ((jtocStart + 4) == oldJtocStart) ? 4 : 8;
			oldJtocStart -= cpLen;

			error |= spe_mfcio_get(spu_data->spus[spu_no].ctx, 
														 JTOC_PTR - (jtocPtr - oldJtocStart),
														 oldJtocStart,
														 cpLen, PROXY_TAG_GROUP, 0, 0);
		}
		if (jtocStart < oldJtocStart) {
			int cpLen = oldJtocStart - jtocStart;

			error |= spe_mfcio_get(spu_data->spus[spu_no].ctx, 
														 JTOC_PTR - (jtocPtr - jtocStart),
														 jtocStart,
														 cpLen, PROXY_TAG_GROUP, 0, 0);
		}	
		
		// copy across the JTOC updates - references
		char * jtocEnd   = (jtocPtr + jtocRefOff);
		char * oldJtocEnd = spu_data->spus[spu_no].jtocEnd;
		if ((jtocEnd > oldJtocEnd) && 
				!LONG_BOUNDARY(jtocEnd)) {
			jtocEnd -= 4;
			error |= spe_mfcio_get(spu_data->spus[spu_no].ctx, 
														 JTOC_PTR + (jtocEnd - jtocPtr),
														 jtocEnd,
														 4, PROXY_TAG_GROUP, 0, 0);
		}
		if ((jtocEnd > oldJtocEnd) && 
				!QUAD_BOUNDARY(jtocEnd)) {

			int cpLen = ((jtocEnd - 4) == oldJtocEnd) ? 4 : 8;
			jtocEnd -= cpLen;

			error |= spe_mfcio_get(spu_data->spus[spu_no].ctx, 
														 JTOC_PTR + (jtocEnd - jtocPtr),
														 jtocEnd,
														 cpLen, PROXY_TAG_GROUP, 0, 0);
		}
		if ((jtocEnd > oldJtocEnd) && 
				!LONG_BOUNDARY(oldJtocEnd)) {

			error |= spe_mfcio_get(spu_data->spus[spu_no].ctx, 
														 JTOC_PTR + (oldJtocEnd - jtocPtr),
														 oldJtocEnd,
														 4, PROXY_TAG_GROUP, 0, 0);
			oldJtocEnd += 4;
		}
		if ((jtocEnd > oldJtocEnd) && 
				!QUAD_BOUNDARY(oldJtocEnd)) {

			int cpLen = ((jtocEnd - 4) == oldJtocEnd) ? 4 : 8;

			error |= spe_mfcio_get(spu_data->spus[spu_no].ctx, 
														 JTOC_PTR + (oldJtocEnd - jtocPtr),
														 oldJtocEnd,
														 cpLen, PROXY_TAG_GROUP, 0, 0);
			oldJtocEnd += cpLen;
		}
		if (jtocEnd > oldJtocEnd) {
			int cpLen = jtocEnd - oldJtocEnd;

			error |= spe_mfcio_get(spu_data->spus[spu_no].ctx, 
														 JTOC_PTR + (oldJtocEnd - jtocPtr),
														 oldJtocEnd,
														 cpLen, PROXY_TAG_GROUP, 0, 0);
		}	

		spu_data->spus[spu_no].jtocStart = (jtocPtr + jtocNumOff);
		spu_data->spus[spu_no].jtocEnd   = (jtocPtr + jtocRefOff);

		if (error) {
			perror("Error recopying JTOC to SPU");
			exit(1);
		}
	}
}

void waitForProxyload(SpuThreadData * spu_data, int spu_no) {
	unsigned int status;
	if (spu_data->spus[spu_no].jtocDirty) {
		// wait for JTOC to finish loading
		if (spe_mfcio_tag_status_read(spu_data->spus[spu_no].ctx, 
																	PROXY_TAG_GROUP_BM,
																	SPE_TAG_ALL,
																	&status)) {
			perror("Failed while waiting for DMA copy JTOC to Cell SPUs");
			exit(1);
		}

		// jtoc now up to date
		spu_data->spus[spu_no].jtocDirty = 0;
	}
}


/* Prepare SPU for migration of a thread */
void prepareMigration(SpuThreadData * spu_data,
											int chosenSpu,
											int methodClassTocOffset, 
											int methodSubArchOffset, 
											VM_Address paramsStart,
											int paramsLength)
{
	int i;
	unsigned int methodSignal[3];
	unsigned int paramSignal[2];
	unsigned int retSignal;
	unsigned int * params = (unsigned int *) paramsStart;

  reloadJtoc(spu_data, chosenSpu);
	loadTocTables(spu_data, chosenSpu);

	// TODO - see if we can defer this waiting (note offset required for method load)
	waitForProxyload(spu_data, chosenSpu);

	methodSignal[0] = LOAD_STATIC_METHOD;
	methodSignal[1] = (unsigned int) methodClassTocOffset;
	methodSignal[2] = (unsigned int) methodSubArchOffset;

	if (spe_in_mbox_write(spu_data->spus[chosenSpu].ctx,
												methodSignal,
												3,
												SPE_MBOX_ANY_NONBLOCKING) < 0) {
		perror("Failed while trying to signal method details to Cell SPU");
		exit(1);
	}

	// wait for ACK from SPU
	if (spe_out_intr_mbox_read(spu_data->spus[chosenSpu].ctx, 
														 &retSignal,
														 1, 
														 SPE_MBOX_ALL_BLOCKING) < 0) {
		perror("Failed reading SPU mailbox while awaiting SPU boot");
		exit(1);
	}
	if (retSignal != ACK) {
		spe_out_mbox_read(spu_data->spus[chosenSpu].ctx, &retSignal, 1);
		fprintf(stderr, "SPU did not ACK method load signal, returned error no. 0x%x\n", 
						retSignal);
		exit(1);
	}

	// load params
	for (i=0; i<paramsLength; i++) {
		// TODO - Double params
		paramSignal[0] = LOAD_WORD_PARAM;
		paramSignal[1] = params[paramsLength - (i + 1)];

		if (spe_in_mbox_write(spu_data->spus[chosenSpu].ctx,
													paramSignal,
													2,
													SPE_MBOX_ANY_NONBLOCKING) < 0) {
			perror("Failed while trying to signal method details to Cell SPU");
			exit(1);
		}
		// wait for ACK from SPU
		if (spe_out_intr_mbox_read(spu_data->spus[chosenSpu].ctx, 
															 &retSignal,
															 1, 
															 SPE_MBOX_ALL_BLOCKING) < 0) {
			perror("Failed reading SPU mailbox while awaiting SPU boot");
			exit(1);
		}
		if (retSignal != ACK) {
			spe_out_mbox_read(spu_data->spus[chosenSpu].ctx, &retSignal, 1);
			fprintf(stderr, "SPU did not ACK method para load signal, returned error no. 0x%x\n", 
							retSignal);
			exit(1);
		}
	}
}

extern "C" void
runMigratedMethod(SpuThreadData * spu_data, int chosenSpu, int runMethodSignal)
{
	unsigned int signalData, err;

	// signal spu to run method
	signalData = (unsigned int) runMethodSignal;
	if (spe_in_mbox_write(spu_data->spus[chosenSpu].ctx,
												&signalData,
												1,
												SPE_MBOX_ANY_NONBLOCKING) < 0) {
		perror("Failed while trying to signal method details to Cell SPU");
		exit(1);
	}
	// check we get an ACK back
	if (spe_out_intr_mbox_read(spu_data->spus[chosenSpu].ctx, 
														 &signalData,
														 1, 
														 SPE_MBOX_ALL_BLOCKING) < 0) {
		perror("Failed reading SPU mailbox while awaiting SPU method invocation");
		exit(1);
	}
	// if method run was not acked
	if (signalData != ACK) {
		// read error signal
		spe_out_mbox_read(spu_data->spus[chosenSpu].ctx, &err, 1);
		fprintf(stderr, 
						"SPU did not ACK method run signal, signaled 0x%x, returned error no. 0x%x\n", 
						signalData, err);
		exit(1);
	}
}

void handleTrap (int chosenSpu) {
	unsigned int message;

	spe_out_mbox_read(global_spu_data->spus[chosenSpu].ctx,
										&(message), 1);
	fprintf(stderr, "Error, SPU %i trapped with value 0x%x\n", chosenSpu, message);
	exit(1);
}


pthread_mutex_t fakeTrapLock = PTHREAD_MUTEX_INITIALIZER;

void handleFakeTrap (int chosenSpu) {
	unsigned int message;
	char * str;
	int i, length;

	spe_out_mbox_read(global_spu_data->spus[chosenSpu].ctx,
										&(message), 1);
	
	pthread_mutex_lock(&fakeTrapLock);
	sysConsoleWriteChar('F');
	sysConsoleWriteChar('T');
	sysConsoleWriteChar('[');
	sysConsoleWriteInteger(chosenSpu, 0);
	sysConsoleWriteChar(']');
	sysConsoleWriteChar(':');
	sysConsoleWriteChar('>');
	sysConsoleWriteChar(' ');
	str = (char *) (global_spu_data->boot_record->fakeTrapStrs[message]);
	length = *((int *) (str - 4));
	for (i=0; i<length; i++) {
		sysConsoleWriteChar(str[i]);
	}
	sysConsoleWriteChar('\n');
	pthread_mutex_unlock(&fakeTrapLock);

	// send back ACK
	message = ACK;
	if (spe_in_mbox_write(global_spu_data->spus[chosenSpu].ctx,
												&(message), 1, SPE_MBOX_ANY_NONBLOCKING) < 0) {		
		fprintf(stderr, "Error writing ack for console write message\n");
		exit(1);
	}	
}

void handleFakeTrapInt (int chosenSpu) {
	unsigned int message;

	spe_out_mbox_read(global_spu_data->spus[chosenSpu].ctx,
										&(message), 1);
	
	pthread_mutex_lock(&fakeTrapLock);
	sysConsoleWriteChar('F');
	sysConsoleWriteChar('T');
	sysConsoleWriteChar('[');
	sysConsoleWriteInteger(chosenSpu, 0);
	sysConsoleWriteChar(']');
	sysConsoleWriteChar(':');
	sysConsoleWriteChar('>');
	sysConsoleWriteChar(' ');
	sysConsoleWriteInteger((int) message, 1);
	sysConsoleWriteChar('\n');
	pthread_mutex_unlock(&fakeTrapLock);
	

	// send back ACK
	message = ACK;
	if (spe_in_mbox_write(global_spu_data->spus[chosenSpu].ctx,
												&(message), 1, SPE_MBOX_ANY_NONBLOCKING) < 0) {		
		fprintf(stderr, "Error writing ack for console write message\n");
		exit(1);
	}	
}

void handleConsoleWrite (int chosenSpu) {
	unsigned int char_val, ack;

	if (spe_out_mbox_read(global_spu_data->spus[chosenSpu].ctx,
												&(char_val), 1) < 0) {		
		fprintf(stderr, "Error reading console write message\n");
		exit(1);
	}
	sysConsoleWriteChar(char_val);

	// send back ACK
	ack = ACK;
	if (spe_in_mbox_write(global_spu_data->spus[chosenSpu].ctx,
												&(ack), 1, SPE_MBOX_ANY_NONBLOCKING) < 0) {		
		fprintf(stderr, "Error writing ack for console write message\n");
		exit(1);
	}
} 

void handleConsoleIntWrite(int chosenSpu, int cmdSignal) {	
	unsigned int int_val, ack;

	if (spe_out_mbox_read(global_spu_data->spus[chosenSpu].ctx,
												&(int_val), 1) < 0) {		
		fprintf(stderr, "Error reading console write int message\n");
		exit(1);
	}
	sysConsoleWriteInteger(int_val, cmdSignal - CONSOLE_WRITE_INT);

	// send back ACK
	ack = ACK;
	if (spe_in_mbox_write(global_spu_data->spus[chosenSpu].ctx,
												&(ack), 1, SPE_MBOX_ANY_NONBLOCKING) < 0) {		
		fprintf(stderr, "Error writing ack for console write int message\n");
		exit(1);
	}
}

void handleConsoleLongWrite(int chosenSpu, int cmdSignal) {	
	unsigned int long_val[2], ack;

	if (spe_out_mbox_read(global_spu_data->spus[chosenSpu].ctx,
												long_val, 2) < 0) {		
		fprintf(stderr, "Error reading console write int message\n");
		exit(1);
	}
	sysConsoleWriteLong(*((long long *)long_val), cmdSignal - CONSOLE_WRITE_INT);

	// send back ACK
	ack = ACK;
	if (spe_in_mbox_write(global_spu_data->spus[chosenSpu].ctx,
												&(ack), 1, SPE_MBOX_ANY_NONBLOCKING) < 0) {		
		fprintf(stderr, "Error writing ack for console write int message\n");
		exit(1);
	}
}


void handleConsoleDoubleWrite(int chosenSpu) {	
	unsigned int double_val[3], ack;

	if (spe_out_mbox_read(global_spu_data->spus[chosenSpu].ctx,
												double_val, 3) < 0) {		
		fprintf(stderr, "Error reading console write int message\n");
		exit(1);
	}
	sysConsoleWriteDouble(*((double *)double_val), (int) double_val[2]);

	// send back ACK
	ack = ACK;
	if (spe_in_mbox_write(global_spu_data->spus[chosenSpu].ctx,
												&(ack), 1, SPE_MBOX_ANY_NONBLOCKING) < 0) {		
		fprintf(stderr, "Error writing ack for console write int message\n");
		exit(1);
	}
}

extern "C" int
supportSPU(SpuThreadData * spu_data, SpuJavaThreadData * thread, int chosenSpu) {

	unsigned int cmdSignal;
	char stop = 0;

	while (!stop) {
		if (spe_out_intr_mbox_read(spu_data->spus[chosenSpu].ctx, 
															 &cmdSignal, 1, SPE_MBOX_ALL_BLOCKING) < 0) {
			perror("Failed reading while waiting for a command signal from SPU");
			exit(1);
		}
		
		switch (cmdSignal) {
		case TRAP_MESSAGE:
			handleTrap(chosenSpu);
			break;	
		case FAKE_TRAP_MESSAGE:
			printf("Fake Trap Called from SPU\n");
			break;	
		case FAKE_TRAP_MESSAGE_STR:
			handleFakeTrap(chosenSpu);
			break;	
		case FAKE_TRAP_MESSAGE_INT:
			handleFakeTrapInt(chosenSpu);
			break;	
		case CONSOLE_WRITE_CHAR:
			handleConsoleWrite(chosenSpu);
			break;
		case CONSOLE_WRITE_INT:
		case CONSOLE_WRITE_INT_BOTH:
		case CONSOLE_WRITE_INT_HEX:
			handleConsoleIntWrite(chosenSpu, cmdSignal);
			break;		
		case CONSOLE_WRITE_LONG:
		case CONSOLE_WRITE_LONG_BOTH:
		case CONSOLE_WRITE_LONG_HEX:
			handleConsoleLongWrite(chosenSpu, cmdSignal);
			break;							
		case CONSOLE_WRITE_DOUBLE:
			handleConsoleDoubleWrite(chosenSpu);
			break;			
		case RETURN_VALUE_V:
			stop = 1;
			break;
		case RETURN_VALUE_I:
		case RETURN_VALUE_F:
		case RETURN_VALUE_R:
			if (spe_out_mbox_read(global_spu_data->spus[chosenSpu].ctx,
														&(thread->retVal[0]), 1) < 0) {
				perror("Error reading return value from SPU migrated method\n");
				exit(1);
			}
		
			stop = 1;
			break;
			
		case RETURN_VALUE_L_UPPER:
		case RETURN_VALUE_D_UPPER:
			if (spe_out_mbox_read(global_spu_data->spus[chosenSpu].ctx,
														&(thread->retVal[0]), 1) < 0) {
				perror("Error reading return value from SPU migrated method\n");
				exit(1);
			}

			break;

		case RETURN_VALUE_L_LOWER:
		case RETURN_VALUE_D_LOWER:
			if (spe_out_mbox_read(global_spu_data->spus[chosenSpu].ctx,
														&(thread->retVal[1]), 1) < 0) {
				perror("Error reading return value from SPU migrated method\n");
				exit(1);
			}

			stop = 1;
			break;

		default:
			{
				unsigned int err = 0;
				fprintf(stderr, "Unknown signal recieved from SPU: 0x%x", cmdSignal);
				spe_out_mbox_read(spu_data->spus[chosenSpu].ctx, &err, 1);
				fprintf(stderr, "Error signal was: 0x%x\n", err);
				exit(1);
			}
		}
	}
		
	return 0;
}

void doMigration(SpuThreadData * spu_data, SpuJavaThreadData * thread, int spuId)
{	
	prepareMigration(spu_data, 
									 spuId,
									 thread->methodClassTocOffset,
									 thread->methodSubArchOffset,
									 thread->paramsStart,
									 thread->paramsLength);

	runMigratedMethod(spu_data, spuId, thread->retType);

	supportSPU(spu_data, thread, spuId);

	// thread is now complete
	thread->complete = 1;
	// release SPU to allow it to do more work
	releaseThread(spu_data, spuId);

	return;
}

/* Support thread for each SPU */
void * spu_support_thread(void *arg) { 
	int spuId = (int) arg;

	for(;;) {
		SpuJavaThreadData * thread;

		pthread_mutex_lock (&global_spu_data->lock);
		while(global_spu_data->workToDo == NULL) {
			pthread_cond_wait(&global_spu_data->condVar, &global_spu_data->lock);
		}
		// steal work
		thread = global_spu_data->workToDo;
		global_spu_data->workToDo = thread->next;
		pthread_mutex_unlock(&global_spu_data->lock);

		if (thread->procAffinity == (char)-1 || thread->procAffinity == spuId) {
			// migrate thread
			doMigration(global_spu_data, thread, spuId);
		} else {
			printf("Fail migration procAffinity == %i\n", thread->procAffinity);
			// thread has a different processor affinity
			// add it back to the list of work
			pthread_mutex_lock (&global_spu_data->lock);
			thread->next = global_spu_data->workToDo;
			global_spu_data->workToDo = thread;
			
			// signal condition variable to wake up threads
			pthread_cond_broadcast(&global_spu_data->condVar);
			pthread_mutex_unlock (&global_spu_data->lock);
		}
	}
}


/* Update the status of a subarch thread for Java */
void updateStatus(int * subArchThreadStatus) {
	int id = MASK_ID(*subArchThreadStatus);

	if (global_spu_data->threads[id].complete) {
		*subArchThreadStatus |= SUBARCH_READY_BIT;
	}
}


/*********** SysCalls **************/

extern "C" int
sysVirtualSubArchProcessorBind(VM_Address procObj, int procID) 
{
	unsigned int signal[2];

	// TODO - do some checking here
	SpuData spu = global_spu_data->spus[procID];
	
	signal[0] = SET_PROCESSOR_REG;
	signal[1] = (unsigned int) procObj;

	// tell spu runtime its processor object
	if (spe_in_mbox_write(spu.ctx,
												signal,
												2,
												SPE_MBOX_ANY_NONBLOCKING) < 0) {
		perror("Failed while trying to signal method details to Cell SPU");
		exit(1);
	}

	// wait for ACK from SPU
	if (spe_out_intr_mbox_read(spu.ctx, 
														 signal,
														 1, 
														 SPE_MBOX_ALL_BLOCKING) < 0) {
		perror("Faied reading SPU mailbox while awaiting SPU boot");
		exit(1);
	}
	if (signal[0] != ACK) {
		spe_out_mbox_read(spu.ctx, signal, 1);
		fprintf(stderr, "SPU did not ACK setProcessor, returned error no. 0x%x\n", 
						signal[0]);
		exit(1);
	}

	return 0;
}


extern "C" int
migrateToSubArch(int retType,
								 int procAffinity,
								 int methodClassTocOffset,
								 int methodSubArchOffset, 
								 VM_Address paramsStart,
								 int paramsLength)
{
	int threadId;
	// lock the data structures while we add the work
	pthread_mutex_lock (&global_spu_data->lock);

	// choose a free threadId
	threadId = chooseThreadID(global_spu_data);

	// fill out data
	SpuJavaThreadData * thread = &global_spu_data->threads[threadId];
	thread->retType = retType;
	thread->procAffinity = procAffinity;
	thread->methodClassTocOffset = methodClassTocOffset;
	thread->methodSubArchOffset = methodSubArchOffset;
	thread->paramsStart = paramsStart;
	thread->paramsLength = paramsLength;

	// add it to the list of work
	thread->next = global_spu_data->workToDo;
	global_spu_data->workToDo = thread;

	// signal condition variable to wake up threads
	pthread_cond_broadcast(&global_spu_data->condVar);
	pthread_mutex_unlock (&global_spu_data->lock);
	
	return threadId;
}

/* Syscall accessed from Java code */
extern "C" VM_Address
subArchCheckStatus(int* subArchThrdStatus,
									 int count)
{
	int i;

	for (i=0; i<count; i++) {
		updateStatus(&(subArchThrdStatus[i]));
	}
	
	return 0;
}

void checkIdComplete(int id) {
	if (!global_spu_data->threads[id].complete) {
		fprintf(stderr, "Error, tried to get migrated methods return value before it completed\n");
		exit(1);
	}
}

/* Syscall accessed from Java code */
extern "C" int
subArchGetIntReturn(int threadId) {
	int id = MASK_ID(threadId);
	checkIdComplete(id);
	int ret = *((int*)&global_spu_data->threads[id].retVal[0]);
	global_spu_data->threads[id].in_use = 0;
	return ret;
}

/* Syscall accessed from Java code */
extern "C" float
subArchGetFloatReturn(int threadId) {
	int id = MASK_ID(threadId);
	checkIdComplete(id);
	float ret = *((float*)&global_spu_data->threads[id].retVal[0]);
	global_spu_data->threads[id].in_use = 0;
	return ret;
}

/* Syscall accessed from Java code */
extern "C" long long
subArchGetLongReturn(int threadId) {
	int id = MASK_ID(threadId);
	checkIdComplete(id);
	long long ret = *((long long*)&global_spu_data->threads[id].retVal[0]);
	global_spu_data->threads[id].in_use = 0;
	return ret;
}

/* Syscall accessed from Java code */
extern "C" double
subArchGetDoubleReturn(int threadId) {
	int id = MASK_ID(threadId);
	checkIdComplete(id);
	double ret = *((double*)&global_spu_data->threads[id].retVal[0]);
	global_spu_data->threads[id].in_use = 0;
	return ret;
}

/* Syscall accessed from Java code */
extern "C" VM_Address
subArchGetRefReturn(int threadId) {
	int id = MASK_ID(threadId);
	checkIdComplete(id);
	VM_Address ret = *((VM_Address*)&global_spu_data->threads[id].retVal[0]);
	global_spu_data->threads[id].in_use = 0;
	return ret;
}
