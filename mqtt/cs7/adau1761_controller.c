

/***************************** Include Files *******************************/
#include <stdio.h>
#include "include/adau1761_controller.h"
/************************** Function Definitions ***************************/
/*
 *
 * This application configures UART 16550 to baud rate 9600.
 * PS7 UART (Zynq) is not initialized by this application, since
 * bootrom/bsp configures it to baud rate 115200
 *
 * ------------------------------------------------
 * | UART TYPE   BAUD RATE                        |
 * ------------------------------------------------
 *   uartns550   9600
 *   uartlite    Configurable only in HW design
 *   ps7_uart    115200 (configured by bootrom/bsp)
 */



void write_adau1761(uint32_t addr, uint64_t data, uint32_t nbytes) {
    uint32_t busy;
    uint32_t control_word = (nbytes - 1) << NBYTES_POS | addr << ADDR_POS;
    uint32_t data_low = data << (8 * (8 - nbytes));
    uint32_t data_high = (data << (8 * (8 - nbytes))) >> 32;
    ADAU1761_CONTROLLER_mWriteReg(AUDIO_BASE, WRITE_LOW_DATA_ADDR, data_low);
    ADAU1761_CONTROLLER_mWriteReg(AUDIO_BASE, WRITE_HIGH_DATA_ADDR, data_high);
    ADAU1761_CONTROLLER_mWriteReg(AUDIO_BASE, CTRL_ADDR, control_word);
    ADAU1761_CONTROLLER_mWriteReg(AUDIO_BASE, CTRL_ADDR, control_word | (1<<START_POS));
    while ((busy = ADAU1761_CONTROLLER_mReadReg(AUDIO_BASE, BUSY_ADDR)))
        ;
}

uint64_t read_adau1761(uint32_t addr, uint32_t nbytes) {
    uint32_t busy;
    uint32_t control_word = 1 << READ_POS | (nbytes - 1) << NBYTES_POS | addr << ADDR_POS;
    uint64_t data_low;
    uint64_t data_high;
    ADAU1761_CONTROLLER_mWriteReg(AUDIO_BASE, CTRL_ADDR, control_word);
    ADAU1761_CONTROLLER_mWriteReg(AUDIO_BASE, CTRL_ADDR, control_word | (1<<START_POS));
    while ((busy = ADAU1761_CONTROLLER_mReadReg(AUDIO_BASE, BUSY_ADDR)))
        ;
    data_low = ADAU1761_CONTROLLER_mReadReg(AUDIO_BASE, READ_LOW_DATA_ADDR);
    data_high = ADAU1761_CONTROLLER_mReadReg(AUDIO_BASE, READ_HIGH_DATA_ADDR);
    return ((data_high << 32) | data_low) & ((1LL << 8 * nbytes) - 1);
}

void reset_adau1761(void) {
	printf("write adau1761 \n\r");
	printf ("Adau1761 Device address is::0x%08X!!!\n",AUDIO_BASE);
    ADAU1761_CONTROLLER_mWriteReg(AUDIO_BASE, CTRL_ADDR, 1<<RESET_POS);
    ADAU1761_CONTROLLER_mWriteReg(AUDIO_BASE, CTRL_ADDR, 0);
    printf("read adau1761 \n\r");
    read_adau1761(0, 1);
    read_adau1761(0, 1);
    read_adau1761(0, 1);
}

void init_adau1761(void) {
    volatile int i;
    //  setup PLL
    write_adau1761(0x4000, 0x0e, 1);
    //  Configure PLL
    write_adau1761(0x4002, 0x007d000c2301, 6);
    //  Wait for PLL to lock
    printf("init_adau1761()::Wait for PLL to lock \n\r");
    while (!(read_adau1761(0x4002, 6) & 2))
        ;
    //  Enable clock to core
    write_adau1761(0x4000, 0xf, 1);
    //  delay
    usleep(1000000);

    //  I2S master mode
    write_adau1761(0x4015, 0x01, 1);
    //  left mixer enable, mic 6dB
    write_adau1761(0x400a, 0x0f, 1);
    //  left mixer enable, mic 0dB
    //write_adau1761(0x400a, 0x01, 1);
    //  left 6db
    write_adau1761(0x400b, 0x07, 1);
    //  left 0db
    //write_adau1761(0x400b, 0x05, 1);
    //  right mixer enable, mic 6dB
    write_adau1761(0x400c, 0x0f, 1);
    //  right mixer enable, mic 0dB
    //write_adau1761(0x400c, 0x01, 1);
    //  right 6db
    write_adau1761(0x400d, 0x07, 1);
    //  right 0db
    //write_adau1761(0x400d, 0x05, 1);
    // Mic bias
    write_adau1761(0x4010, 0x5, 1);
    //  Playback left mixer unmute, enable
    write_adau1761(0x401c, 0x21, 1);
//    write_adau1761(0x401d, 0x66, 1); not needed we need to control the local input play back from the core vol

    //  Playback right mixer unmute, enable
    write_adau1761(0x401e, 0x21, 1);
    //write_adau1761(0x401e, 0x6D, 1);

//    write_adau1761(0x401f, 0x66, 1);  not needed we need to control the local input play back from the core vol


    //  Enable line out mixer left
	write_adau1761(0x4020, 0x05, 1);
	//  Enable line out mixer right
	write_adau1761(0x4021, 0x11, 1);


    //  Enable headphone output left
    write_adau1761(0x4023, 0xe7, 1);
    //  Enable headphone output right
    write_adau1761(0x4024, 0xe7, 1);

    //  Enable line out left
    write_adau1761(0x4025, 0xe7, 1);
    //  Enable line out right
    write_adau1761(0x4026, 0xe7, 1);

    //Configure ADC/DAC sample rate 96khz
    write_adau1761(0x4017, 0x06, 1);


    //Configure SERIAL port sample rate    96khz
    write_adau1761(0x40F8, 0x06, 1);


    //Configure DSP sample rate  Set to input data rate
    write_adau1761(0x40EB, 0x07, 1);



    //  Enable both ADCs
    write_adau1761(0x4019, 0x03, 1);
    //  Enable playback both channels
    write_adau1761(0x4029, 0x03, 1);
    //  Enable both DACs
    write_adau1761(0x402a, 0x03, 1);


    // Set DAC volume to 0 db
    write_adau1761(0x402b, 0x00, 1);
    write_adau1761(0x402c, 0x00, 1);



    //  Serial input L0,R0 to DAC L,R
    write_adau1761(0x40f2, 0x01, 1);
    //  Serial output ADC L,R to serial output L0,R0
    write_adau1761(0x40f3, 0x01, 1);
    //  Enable clocks to all engines
    write_adau1761(0x40f9, 0x7f, 1);
    //  Enable both clock generators
    write_adau1761(0x40fa, 0x03, 1);
}



int init_codec(void){
	    printf("Initializing reset_adau1761 \n\r");
	    reset_adau1761();
	    printf("Initializing init_adau1761 \n\r");
	    init_adau1761();
	    printf("Audio on adau1761 ready \n\r");
}

//
//int main()
//{
//	int i =0;
//    init_platform();
//
//    print("Initializing reset_adau1761 \n\r");
//    reset_adau1761();
//    print("Initializing init_adau1761 \n\r");
//    init_adau1761();
//    print("Audio looping on adau1761 ready \n\r");
//     while(1){
//    	 if(i==0){
//    		 print("Hope you hear the sound in a loop\n\r");
//    	 }
//    	 i = (i + 1) % 0x00FFFFFF;
//
//
//     }
//
//
//    cleanup_platform();
//    return 0;
//}

