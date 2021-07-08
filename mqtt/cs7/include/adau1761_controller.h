
#ifndef ADAU1761_CONTROLLER_H
#define ADAU1761_CONTROLLER_H


/****************** Include Files ********************/
#include <stdint.h>
#include "ui_control.h"
#define ADAU1761_CONTROLLER_S00_AXI_SLV_REG0_OFFSET 0
#define ADAU1761_CONTROLLER_S00_AXI_SLV_REG1_OFFSET 4
#define ADAU1761_CONTROLLER_S00_AXI_SLV_REG2_OFFSET 8
#define ADAU1761_CONTROLLER_S00_AXI_SLV_REG3_OFFSET 12
#define ADAU1761_CONTROLLER_S00_AXI_SLV_REG4_OFFSET 16
#define ADAU1761_CONTROLLER_S00_AXI_SLV_REG5_OFFSET 20




//#define AUDIO_BASE XPAR_ADAU1761_CONTROLLER_0_S00_AXI_BASEADDR

#define AUDIO_BASE adau1761_base_0


#define CTRL_ADDR ADAU1761_CONTROLLER_S00_AXI_SLV_REG0_OFFSET
#define BUSY_ADDR ADAU1761_CONTROLLER_S00_AXI_SLV_REG1_OFFSET
#define WRITE_LOW_DATA_ADDR ADAU1761_CONTROLLER_S00_AXI_SLV_REG2_OFFSET
#define WRITE_HIGH_DATA_ADDR ADAU1761_CONTROLLER_S00_AXI_SLV_REG3_OFFSET
#define READ_LOW_DATA_ADDR ADAU1761_CONTROLLER_S00_AXI_SLV_REG4_OFFSET
#define READ_HIGH_DATA_ADDR ADAU1761_CONTROLLER_S00_AXI_SLV_REG5_OFFSET

#define RESET_POS 25
#define START_POS 24
#define READ_POS 19
#define NBYTES_POS 16
#define ADDR_POS 0



/**************************** Type Definitions *****************************/
//typedef unsigned long uint64_t;
//typedef unsigned int uint32_t;
/**
 *
 * Write a value to a ADAU1761_CONTROLLER register. A 32 bit write is performed.
 * If the component is implemented in a smaller width, only the least
 * significant data is written.
 *
 * @param   BaseAddress is the base address of the ADAU1761_CONTROLLERdevice.
 * @param   RegOffset is the register offset from the base to write to.
 * @param   Data is the data written to the register.
 *
 * @return  None.
 *
 * @note
 * C-style signature:
 * 	void ADAU1761_CONTROLLER_mWriteReg(u32 BaseAddress, unsigned RegOffset, u32 Data)
 *
 */
#define ADAU1761_CONTROLLER_mWriteReg(BaseAddress, RegOffset, Data) \
	*((unsigned *)((BaseAddress) + (RegOffset))) = (Data)

/**
 *
 * Read a value from a ADAU1761_CONTROLLER register. A 32 bit read is performed.
 * If the component is implemented in a smaller width, only the least
 * significant data is read from the register. The most significant data
 * will be read as 0.
 *
 * @param   BaseAddress is the base address of the ADAU1761_CONTROLLER device.
 * @param   RegOffset is the register offset from the base to write to.
 *
 * @return  Data is the data from the register.
 *
 * @note
 * C-style signature:
 * 	u32 ADAU1761_CONTROLLER_mReadReg(u32 BaseAddress, unsigned RegOffset)
 *
 */
#define ADAU1761_CONTROLLER_mReadReg(BaseAddress, RegOffset) \
    *(unsigned *)((BaseAddress) + (RegOffset))

/************************** Function Prototypes ****************************/
/**
 *
 * Run a self-test on the driver/device. Note this may be a destructive test if
 * resets of the device are performed.
 *
 * If the hardware system is not built correctly, this function may never
 * return to the caller.
 *
 * @param   baseaddr_p is the base address of the ADAU1761_CONTROLLER instance to be worked on.
 *
 * @return
 *
 *    - XST_SUCCESS   if all self-test code passed
 *    - XST_FAILURE   if any self-test code failed
 *
 * @note    Caching must be turned off for this function to work.
 * @note    Self test may fail if data memory and device are not on the same bus.
 *
 */
uint32_t ADAU1761_CONTROLLER_Reg_SelfTest(void * baseaddr_p);

void write_adau1761(uint32_t addr, uint64_t data, uint32_t nbytes);
uint64_t read_adau1761(uint32_t addr, uint32_t nbytes) ;
void reset_adau1761(void);
void init_adau1761(void);
int init_codec(void);

#endif // ADAU1761_CONTROLLER_H
