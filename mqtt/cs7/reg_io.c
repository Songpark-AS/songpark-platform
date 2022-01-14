/**
 * =====================================================================================
 *
 *       Filename:  reg_io.c
 *
 *    Description:  Register read write helper macros and functions
 *    				with register offset definitions.
 *
 *        Version:  1.0
 *         Author:  Thanx
 *   Organization:  Cantavi
 *
 * =====================================================================================
 */

#include "include/reg_io.h"


/**
 * =======================================================
 * Function to open the device file and map the register
 * area in to user accessible address space
 *
 * @Param	: device_file_name	: String specifies the
 * 			  											name of the device file
 *
 * @Return	: Returns the pointer to the mapped
 * 			  		Base address of the device file
 *
 * =======================================================
 */
dev_param map_device (const char *device_file_name) {

	int dev_fd = -1;
	void *base_address;
	dev_param ret_params;

	if (device_file_name == NULL) {
		fprintf (stderr, "map_device: NULL Pointer argument :%s\n", device_file_name);
		ret_params.base_address = NULL;
		return ret_params;
	}
	printf ("Trying to open device::%s!!!\n",device_file_name);
	dev_fd = open (device_file_name, O_RDWR);
	if (dev_fd < 1) {
		perror(device_file_name);
		ret_params.base_address = NULL;
		printf ("Opening device::%s failed!!!\n",device_file_name);
		return ret_params;
	}

	printf ("Maping device::%s!!!\n",device_file_name);
	base_address = mmap(NULL, PAGE_SIZE, PROT_READ|PROT_WRITE, MAP_SHARED, dev_fd, 0);
	printf ("Mapped device to Address::0x%08X!!!\n",base_address);
	//return base_address;
	ret_params.base_address = base_address;
	ret_params.dev_fd = dev_fd;
	return ret_params;
}

/**
 * =======================================================
 * Function to unmap the register map from user space
 *
 * @Param	: pointer to the base address of the
 * 			  	mapped area.
 *
 * @Return	: None
 *
 * =======================================================
 */
void unmap_device (void *device_base) {

	munmap(device_base, PAGE_SIZE);
}
