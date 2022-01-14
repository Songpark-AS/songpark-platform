/*
 * =====================================================================================
 *
 *       Filename:  app_main.c
 *
 *    Description:  application initialization functions
 *
 *         Author:  Thanx
 *   Organization:  Cantavi
 *
 * =====================================================================================
 */

#include <stdlib.h>
#include <unistd.h>
#include <stdio.h>
#include "include/ui_control.h"
#include "include/adau1761_controller.h"


int main (int argc, char *argv[]) {

	int ret = 0;
	printf ("Initializing Cantavi Streamer!!!\n");

	if (argc < 6){
		printf ("Error Initializing Cantavi Streamer arguments too few!!!\n");
		return EXIT_FAILURE;
	}
	// ret = ui_init (argc, argv);

	if (ret != 0) {
		fprintf (stderr, "ui_init failed\n");
		return EXIT_FAILURE;
	}




	printf ("Runnin Cantavi Streamer UI\n");
	ret = ui_run ();

	if (ret != 0) {
		fprintf (stderr, "ui_run failed\n");
		return EXIT_FAILURE;
	}

	ui_exit();

	return EXIT_SUCCESS;

}
