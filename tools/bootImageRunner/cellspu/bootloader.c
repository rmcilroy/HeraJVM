#include <stdio.h>
#include "spuDefs.h"

void writeMailbox(int value) {
	asm("wrch $ch30, %0;"
			:
			: "r" (value)
			);
}

int readMailbox() {
	volatile int value;
	asm("rdch %0, $ch29;"
			: "=r" (value)
			: 
			);
	return value;
}

int readMailboxCnt() {
	volatile int value;
	asm("rchcnt  %0,$ch29;"
			: "=r" (value)
			:
			);
	return value;
}

void jumpRuntime(int phys_id) {
	asm("ori $0, %0, 0;"
      "bra 0x0;"
			:
			: "r" (phys_id)
			: "0"
			);
}

int main(unsigned long long id)
{
	int phys_id;
	int cpy_complete;

  // send spu_id to main processor
	writeMailbox((int) (id >> 32));
	writeMailbox((int) (id));

	// retrieve physical id of processor
	phys_id = readMailbox();

  //printf("Bringing up SPU %d\n", phys_id);

	// wait for copy complete signal
	cpy_complete = readMailbox();
	if (cpy_complete == RUNTIME_COPY_COMPLETE) {
		//printf("SPU %i jumping to Java Runtime\n", phys_id);
		jumpRuntime(phys_id);

		// should never be reached
		return -1;
	} else {
		printf("Runtime copy to SPU %i was not successful - Quitting\n", phys_id);
		return 1;
	}
}
