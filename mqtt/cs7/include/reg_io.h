/**
 * =====================================================================================
 *
 *       Filename:  reg_io.h
 *
 *    Description:  Register read write helper macros and functions
 *    				with register offset definitions.
 *
 *         Author:  Thanx
 *   Organization:  Cantavi
 *
 * =====================================================================================
 */

#ifndef REG_IO_H
#define REG_IO_H

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/mman.h>
#include <fcntl.h>
#include <stdint.h>

/**
 * =======================================================
 * Macros for device file names
 * =======================================================
 */

#define ETH_TO_AUDIO_DEVICE_0		"/dev/uio0"
#define FILTER_DEVICE_0 			"/dev/uio1"
#define VOLUME_DEVICE_0				"/dev/uio2"
#define FILTER_DEVICE_1 			"/dev/uio3"
#define VOLUME_DEVICE_1				"/dev/uio4"
#define ZEDBOARDOLED_0				"/dev/uio5"
#define PMOD_CONTROLLER_0			"/dev/uio6"


#define AUDIO_TO_ETH_DEVICE_0		"/dev/uio7"
#define FULL_UDP_STACK_IP_0			"/dev/uio8"
#define ETH_PLC_SEQ_0			    "/dev/uio9"
#define ADAU1761_CODEC_0			"/dev/uio10"
#define TIME_SYNC_0					"/dev/uio11"

/**
 * =======================================================
 * Audio to AXI register offset definitions
 * =======================================================
 * rValue(2) <= adcLeftFIFOEmpty;
 *	rValue(3) <= adcRightFIFOEmpty;
 *	rValue(4) <= adcLeftFIFOFull;
 *	rValue(5) <= aacRightFIFOFull;
 */
#define AUDIO_TO_AXI_INTERFACE_REG0_OFFSET 0		//New IP read status// Write 0x03 to reset fifos
#define AUDIO_TO_AXI_INTERFACE_REG1_OFFSET 4		//New IP read L audio
#define AUDIO_TO_AXI_INTERFACE_REG2_OFFSET 8		//New IP read R audio
#define AUDIO_TO_AXI_INTERFACE_REG3_OFFSET 12

/**
 * =======================================================
 * AXI to Audio register offset definitions
 * =======================================================
 * rValue(2) <= dacLeftFIFOEmpty;
 *	rValue(3) <= dacRightFIFOEmpty;
 *	rValue(4) <= dacLeftFIFOFull;
 *	rValue(5) <= dacRightFIFOFull;
 */
#define AXI_TO_AUDIO_INTERFACE_REG0_OFFSET 0  // New IP read status// Write 0x03 to reset fifos
#define AXI_TO_AUDIO_INTERFACE_REG1_OFFSET 4  // New IP write L audio
#define AXI_TO_AUDIO_INTERFACE_REG2_OFFSET 8 // New IP write R audio
#define AXI_TO_AUDIO_INTERFACE_REG3_OFFSET 12

/**
 * =======================================================
 * FIR_Filter register offset definitions
 * =======================================================
 */
#define FILTER_CONTROL_REG0_OFFSET 0
#define FILTER_CONTROL_REG1_OFFSET 4
#define FILTER_CONTROL_REG2_OFFSET 8
#define FILTER_CONTROL_REG3_OFFSET 12
#define FILTER_CONTROL_REG4_OFFSET 16
#define FILTER_CONTROL_REG5_OFFSET 20
#define FILTER_CONTROL_REG6_OFFSET 24
#define FILTER_CONTROL_REG7_OFFSET 28
#define FILTER_CONTROL_REG8_OFFSET 32
#define FILTER_CONTROL_REG9_OFFSET 36
#define FILTER_CONTROL_REG10_OFFSET 40
#define FILTER_CONTROL_REG11_OFFSET 44
#define FILTER_CONTROL_REG12_OFFSET 48
#define FILTER_CONTROL_REG13_OFFSET 52
#define FILTER_CONTROL_REG14_OFFSET 56
#define FILTER_CONTROL_REG15_OFFSET 60
#define FILTER_CONTROL_REG16_OFFSET 64
#define FILTER_CONTROL_REG17_OFFSET 68
#define FILTER_CONTROL_REG18_OFFSET 72
#define FILTER_CONTROL_REG19_OFFSET 76

/**
 * =======================================================
 * Volume Control register offset definitions
 * =======================================================
 */
#define VOLUME_CONTROL_REG0_OFFSET 0
#define VOLUME_CONTROL_REG1_OFFSET 4
#define VOLUME_CONTROL_REG2_OFFSET 8
#define VOLUME_CONTROL_REG3_OFFSET 12

/**
 * =======================================================
 * PMOD register offset definitions
 * =======================================================
 */
#define PMOD_INTERFACE_REG0_OFFSET 0
#define PMOD_INTERFACE_REG1_OFFSET 4
#define PMOD_INTERFACE_REG2_OFFSET 8
#define PMOD_INTERFACE_REG3_OFFSET 12


/**
 * =======================================================
 * ETH_UDP_IP register offset definitions
 * =======================================================
 *  slv_reg0_mac;
 *  slv_reg1_mac;
 *  slv_reg2_local_ip;
 *  slv_reg3_gateway_ip;
 *  slv_reg4_subnet_mask;
 *  slv_reg5_dest_ip;
 *  slv_reg6_dest_port;
 *  slv_reg7_loop_timer;
 *  slv_reg8_latency_timer;
 *  slv_reg9_sample_fifo_timer;
 *  slv_regA_net_errors;
 *  slv_regB_lost_packets;
 *  slv_regC_tx_packets;
 *  slv_regD_rx_packets;
 *  slv_regE_length;
 *  slv_regF_ctrl;
 */
#define ETH_UDP_REG0_OFFSET 0
#define ETH_UDP_REG1_OFFSET 4
#define ETH_UDP_REG2_OFFSET 8
#define ETH_UDP_REG3_OFFSET 12
#define ETH_UDP_REG4_OFFSET 16
#define ETH_UDP_REG5_OFFSET 20
#define ETH_UDP_REG6_OFFSET 24
#define ETH_UDP_REG7_OFFSET 28
#define ETH_UDP_REG8_OFFSET 32
#define ETH_UDP_REG9_OFFSET 36
#define ETH_UDP_REG10_OFFSET 40
#define ETH_UDP_REG11_OFFSET 44
#define ETH_UDP_REG12_OFFSET 48
#define ETH_UDP_REG13_OFFSET 52
#define ETH_UDP_REG14_OFFSET 56
#define ETH_UDP_REG15_OFFSET 60
#define ETH_UDP_REG16_OFFSET 64
#define ETH_UDP_REG17_OFFSET 68
#define ETH_UDP_REG18_OFFSET 72
#define ETH_UDP_REG19_OFFSET 76

/**
 * =======================================================
 * Audio to ETH register offset definitions
 * =======================================================
 *  rValue(2) <= adcLeftFIFOEmpty;
 *	rValue(3) <= adcRightFIFOEmpty;
 *	rValue(4) <= adcLeftFIFOFull;
 *	rValue(5) <= aacRightFIFOFull;
 */
#define AUDIO_TO_ETH_INTERFACE_REG0_OFFSET 0		//New IP read status// Write 0x03 to reset fifos
#define AUDIO_TO_ETH_INTERFACE_REG1_OFFSET 4		//New IP read L audio
#define AUDIO_TO_ETH_INTERFACE_REG2_OFFSET 8		//New IP read R audio
#define AUDIO_TO_ETH_INTERFACE_REG3_OFFSET 12
#define AUDIO_TO_ETH_INTERFACE_REG4_OFFSET 16
#define AUDIO_TO_ETH_INTERFACE_REG5_OFFSET 20
#define AUDIO_TO_ETH_INTERFACE_REG6_OFFSET 24
#define AUDIO_TO_ETH_INTERFACE_REG7_OFFSET 28
#define AUDIO_TO_ETH_INTERFACE_REG8_OFFSET 32



#define PKT_SEQ_INTERFACE_REG0_OFFSET 0		//New IP read status// Write 0x03 to reset fifos
#define PKT_SEQ_INTERFACE_REG1_OFFSET 4
#define PKT_SEQ_INTERFACE_REG2_OFFSET 8
#define PKT_SEQ_INTERFACE_REG3_OFFSET 12
#define PKT_SEQ_INTERFACE_REG4_OFFSET 16
#define PKT_SEQ_INTERFACE_REG5_OFFSET 20
#define PKT_SEQ_INTERFACE_REG6_OFFSET 24
#define PKT_SEQ_INTERFACE_REG7_OFFSET 28
#define PKT_SEQ_INTERFACE_REG8_OFFSET 32
#define PKT_SEQ_INTERFACE_REG9_OFFSET 36
#define PKT_SEQ_INTERFACE_REG10_OFFSET 40
#define PKT_SEQ_INTERFACE_REG11_OFFSET 44
#define PKT_SEQ_INTERFACE_REG12_OFFSET 48
#define PKT_SEQ_INTERFACE_REG13_OFFSET 52
#define PKT_SEQ_INTERFACE_REG14_OFFSET 56
#define PKT_SEQ_INTERFACE_REG15_OFFSET 60
#define PKT_SEQ_INTERFACE_REG16_OFFSET 64
#define PKT_SEQ_INTERFACE_REG17_OFFSET 68
#define PKT_SEQ_INTERFACE_REG18_OFFSET 72
#define PKT_SEQ_INTERFACE_REG19_OFFSET 76
#define PKT_SEQ_INTERFACE_REG20_OFFSET 80
#define PKT_SEQ_INTERFACE_REG21_OFFSET 84
#define PKT_SEQ_INTERFACE_REG22_OFFSET 88
#define PKT_SEQ_INTERFACE_REG23_OFFSET 92
#define PKT_SEQ_INTERFACE_REG24_OFFSET 96
#define PKT_SEQ_INTERFACE_REG25_OFFSET 100



#define PKT_TIME_ENFORCER_REG0_OFFSET 0		//New IP read status// Write 0x03 to reset fifos
#define PKT_TIME_ENFORCER_REG1_OFFSET 4
#define PKT_TIME_ENFORCER_REG2_OFFSET 8
#define PKT_TIME_ENFORCER_REG3_OFFSET 12
#define PKT_TIME_ENFORCER_REG4_OFFSET 16
#define PKT_TIME_ENFORCER_REG5_OFFSET 20
#define PKT_TIME_ENFORCER_REG6_OFFSET 24
#define PKT_TIME_ENFORCER_REG7_OFFSET 28
#define PKT_TIME_ENFORCER_REG8_OFFSET 32
#define PKT_TIME_ENFORCER_REG9_OFFSET 36
#define PKT_TIME_ENFORCER_REG10_OFFSET 40
#define PKT_TIME_ENFORCER_REG11_OFFSET 44
#define PKT_TIME_ENFORCER_REG12_OFFSET 48
#define PKT_TIME_ENFORCER_REG13_OFFSET 52
#define PKT_TIME_ENFORCER_REG14_OFFSET 56
#define PKT_TIME_ENFORCER_REG15_OFFSET 60
#define PKT_TIME_ENFORCER_REG16_OFFSET 64
#define PKT_TIME_ENFORCER_REG17_OFFSET 68
#define PKT_TIME_ENFORCER_REG18_OFFSET 72
#define PKT_TIME_ENFORCER_REG19_OFFSET 76
#define PKT_TIME_ENFORCER_REG20_OFFSET 80
#define PKT_TIME_ENFORCER_REG21_OFFSET 84
#define PKT_TIME_ENFORCER_REG22_OFFSET 88
#define PKT_TIME_ENFORCER_REG23_OFFSET 92
#define PKT_TIME_ENFORCER_REG24_OFFSET 96
#define PKT_TIME_ENFORCER_REG25_OFFSET 100
#define PKT_TIME_ENFORCER_REG26_OFFSET 104
#define PKT_TIME_ENFORCER_REG27_OFFSET 108
#define PKT_TIME_ENFORCER_REG28_OFFSET 112
#define PKT_TIME_ENFORCER_REG29_OFFSET 116
#define PKT_TIME_ENFORCER_REG30_OFFSET 120
#define PKT_TIME_ENFORCER_REG31_OFFSET 124
#define PKT_TIME_ENFORCER_REG32_OFFSET 128


/**
 * =======================================================
 * ETH to Audio register offset definitions
 * =======================================================
 *  rValue(2) <= dacLeftFIFOEmpty;
 *	rValue(3) <= dacRightFIFOEmpty;
 *	rValue(4) <= dacLeftFIFOFull;
 *	rValue(5) <= dacRightFIFOFull;
 */
#define ETH_TO_AUDIO_INTERFACE_REG0_OFFSET 0  // New IP read status// Write 0x03 to reset fifos
#define ETH_TO_AUDIO_INTERFACE_REG1_OFFSET 4  // New IP write L audio
#define ETH_TO_AUDIO_INTERFACE_REG2_OFFSET 8 // New IP write R audio
#define ETH_TO_AUDIO_INTERFACE_REG3_OFFSET 12
#define ETH_TO_AUDIO_INTERFACE_REG4_OFFSET 16
#define ETH_TO_AUDIO_INTERFACE_REG5_OFFSET 20
#define ETH_TO_AUDIO_INTERFACE_REG6_OFFSET 24
#define ETH_TO_AUDIO_INTERFACE_REG7_OFFSET 28
#define ETH_TO_AUDIO_INTERFACE_REG8_OFFSET 32
#define ETH_TO_AUDIO_INTERFACE_REG9_OFFSET 36
#define ETH_TO_AUDIO_INTERFACE_REG10_OFFSET 40
#define ETH_TO_AUDIO_INTERFACE_REG11_OFFSET 44
#define ETH_TO_AUDIO_INTERFACE_REG12_OFFSET 48
#define ETH_TO_AUDIO_INTERFACE_REG13_OFFSET 52
#define ETH_TO_AUDIO_INTERFACE_REG14_OFFSET 56
#define ETH_TO_AUDIO_INTERFACE_REG15_OFFSET 60
#define ETH_TO_AUDIO_INTERFACE_REG16_OFFSET 64
#define ETH_TO_AUDIO_INTERFACE_REG17_OFFSET 68
#define ETH_TO_AUDIO_INTERFACE_REG18_OFFSET 72
#define ETH_TO_AUDIO_INTERFACE_REG19_OFFSET 76
#define ETH_TO_AUDIO_INTERFACE_REG20_OFFSET 80
#define ETH_TO_AUDIO_INTERFACE_REG21_OFFSET 84
#define ETH_TO_AUDIO_INTERFACE_REG22_OFFSET 88
#define ETH_TO_AUDIO_INTERFACE_REG23_OFFSET 92
#define ETH_TO_AUDIO_INTERFACE_REG24_OFFSET 96
#define ETH_TO_AUDIO_INTERFACE_REG25_OFFSET 100
#define ETH_TO_AUDIO_INTERFACE_REG26_OFFSET 104
#define ETH_TO_AUDIO_INTERFACE_REG27_OFFSET 108
#define ETH_TO_AUDIO_INTERFACE_REG28_OFFSET 112
#define ETH_TO_AUDIO_INTERFACE_REG29_OFFSET 116
#define ETH_TO_AUDIO_INTERFACE_REG30_OFFSET 120
#define ETH_TO_AUDIO_INTERFACE_REG31_OFFSET 124
#define ETH_TO_AUDIO_INTERFACE_REG32_OFFSET 128

/**
 * =======================================================
 * Macro to get the Arch specific page size
 * =======================================================
 */
#define PAGE_SIZE (sysconf(_SC_PAGESIZE))




/**
 * =======================================================
 * Device params struct
 * =======================================================
 */

typedef struct _dev_param {
	int dev_fd;
	void *base_address;
} dev_param;

/**
 * =======================================================
 * Channel ID enumarations
 * =======================================================
 */
typedef enum _CHANNEL_ID {
  CHANNEL_ID_L,
  CHANNEL_ID_R,
  CHANNEL_ID_MAX
}CHANNEL_ID;


/**
 * =======================================================
 * FIFO ID enumarations
 * =======================================================
 */
typedef enum _FIFO_ID {
  ADC_FIFO_ID,
  DAC_FIFO_ID,
  FIFI_ID_MAX
}FIFO_ID;


/**
 * =======================================================
 * ETH ID enumarations
 * =======================================================
 */
typedef enum _ETH_PARAM_ID {
	MAC_ID,
  LOCAL_IP_ID,
  DEST_IP_ID,
  SYNC_IP_ID,
  GTWAY_IP_ID,
  MASK_IP_ID,
  DEST_PORT_ID,
  SYNC_PORT_ID,
  PKT_LEN_ID,
  ETH_CONTROL_ID,
  NET_ID_MAX
}ETH_PARAM_ID;

/**
 * =======================================================
 * AUDIO TO ETH
 * =======================================================
 */
typedef enum _AUDIO_TO_ETH_REG_ID {
	PAYLEN_REG_ID,
  REG_ID_MAX
}AUDIO_TO_ETH_REG_ID;


/**
 * =======================================================
 * ETH REG ID enumarations
 * =======================================================
 * *  slv_reg0_mac;
 *  slv_reg1_mac;
 *  slv_reg2_local_ip;
 *  slv_reg3_gateway_ip;
 *  slv_reg4_subnet_mask;
 *  slv_reg5_dest_ip;
 *  slv_reg6_dest_port;
 *  slv_reg7_loop_timer;
 *  slv_reg8_latency_timer;
 *  slv_reg9_sample_fifo_timer;
 *  slv_regA_net_errors;
 *  slv_regB_lost_packets;
 *  slv_regC_tx_packets;
 *  slv_regD_rx_packets;
 *  slv_regE_length;
 *  slv_regF_ctrl;
 */
typedef enum _ETH_REG_ID {
  MAC_REG_ID,
  LOCAL_IP_REG_ID,
  DEST_IP_REG_ID,
  GTWAY_IP_REG_ID,
  MASK_IP_REG_ID,
  DEST_PORT_REG_ID,
  PKT_LEN_REG_ID,
  LOOP_TIMER_REG_ID,
  LATENCY_TIMER_REG_ID,
  SAMPLE_FIFO_TIMER_REG_ID,
  NET_ERRORS_REG_ID,
  LOST_PACKETS_REG_ID,
  TX_PACKETS_REG_ID,
  RX_PACKETS_REG_ID,
  PACKET_LENGTH_REG_ID,
  ETH_CONTROL_REG_ID,
  NET_REG_ID_MAX
}ETH_REG_ID;


/**
 * =======================================================
 * Filter type enumarations
 * =======================================================
 */
typedef enum _FILTER_TYPE {
  FILTER_BAND_PASS = 1,
  FILTER_HIGH_PASS = 2,
  FILTER_LOW_PASS = 4,
  FILTER_TYPE_MAX
}FILTER_TYPE;

/**
 * =======================================================
 * Macro for writing data into memory mapped registers.
 *
 * @Param 	: BaseAddress	: Base address of the mapped
 * 						register area
 * @Param 	: RegOffset		: Offset from the base address
 * 						where the data need to write
 * @Param 	: Data				: Data to be written
 *
 * @Return 	: None
 *
 * =======================================================
 */
#define WriteReg(BaseAddress, RegOffset, Data) \
	*((unsigned *)((BaseAddress) + (RegOffset))) = (Data)

/**
 * =======================================================
 * Macro for reading data from memory mapped registers
 *
 * @Param 	: BaseAddress	: Base address of the mapped
 * 						register area
 * @Param 	: RegOffset		: Offset from the base address
 * 						where the data need to read
 *
 * @Return 	: Returns unsigned data read from the mapped
 * 			  		register area
 *
 * =======================================================
 */
#define ReadReg(BaseAddress, RegOffset) \
    *(unsigned *)((BaseAddress) + (RegOffset))

/**
 * =======================================================
 * Function to open the device file and map the register
 * area in to user accessible address space
 *
 * @Param		: device_file_name	: String specifies the
 * 			  		name of the device file
 *
 * @Return	: Returns the pointer to the mapped
 * 			  		Base address of the device file
 *
 * =======================================================
 */
dev_param map_device (const char *device_file_name);

/**
 * =======================================================
 * Function to unmap the register map from user space
 *
 * @Param		: pointer to the base address of the
 * 			  		mapped area.
 *
 * @Return	: None
 *
 * =======================================================
 */
void unmap_device (void *device_base);

#endif //REG_IO_H
