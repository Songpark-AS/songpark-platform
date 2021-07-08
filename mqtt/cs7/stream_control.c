/**
 * =====================================================================================
 *
 *       Filename:  stream_control.c
 *
 *    Description:  audio stream control functions and macros
 *
 *        Version:  1.0
 *         Author:  Thanx
 *   Organization:  Cantavi
 *
 * =====================================================================================
 */

#include "include/stream_control.h"
#include "include/ui_control.h"
#include <unistd.h> // for usleep
/**
 * =======================================================
 * read_stream reads 4 bytes unsigned int data
 * from the specified channel
 * @param   : stream_base  : base address of the stream
 * @param   : channel      : channel ID
 * @return  : returns 4 bytes unsigned int data
 * =======================================================
 */
unsigned read_stream (void *stream_base, CHANNEL_ID channel) {
  unsigned buffer = 0;
  if (stream_base == NULL) {
    return -1;
  }

//  if (channel == CHANNEL_ID_L) {
//    buffer = ReadReg(stream_base, AUDIO_TO_AXI_INTERFACE_REG0_OFFSET);
//  }
//  else if (channel == CHANNEL_ID_R) {
//    buffer = ReadReg(stream_base, AUDIO_TO_AXI_INTERFACE_REG1_OFFSET);
//  }
  if (channel == CHANNEL_ID_L) {
	  do{
		  buffer = ReadReg(stream_base, AUDIO_TO_AXI_INTERFACE_REG1_OFFSET);
		  if(buffer == 0xFF000000){
			  usleep(1/48000 * 1000000*16);
		  }
	  }while(buffer > 0xFF000000);

    }
    else if (channel == CHANNEL_ID_R) {
    	do{
			  buffer = ReadReg(stream_base, AUDIO_TO_AXI_INTERFACE_REG2_OFFSET);
			  if(buffer == 0xFF000000){
				  usleep(1/48000 * 1000000*16);
			  }
		  }while(buffer == 0xFF000000);
    }
  else {
    return -1;
  }
  return buffer;
}

/**
 * =======================================================
 * write_stream writes 4 bytes unsigned int data
 * to the spicified channel
 * @param  stream_base  : base address of the stream
 * @param  channel      : channel ID
 * @param  data         : 4 bytes unsigned int data
 * @return  : returns 0 upon success
 *            -1 otherwise
 * =======================================================
 */
int write_stream (void *stream_base, CHANNEL_ID channel, unsigned int data) {
  if (stream_base == NULL) {
    return -1;
  }

//  if (channel == CHANNEL_ID_L) {
//    WriteReg(stream_base, AXI_TO_AUDIO_INTERFACE_REG0_OFFSET, data);
//  }
//  else if (channel == CHANNEL_ID_R) {
//    WriteReg(stream_base, AXI_TO_AUDIO_INTERFACE_REG1_OFFSET, data);
//  }
  if (channel == CHANNEL_ID_L) {
      WriteReg(stream_base, AXI_TO_AUDIO_INTERFACE_REG1_OFFSET, data);
    }
    else if (channel == CHANNEL_ID_R) {
      WriteReg(stream_base, AXI_TO_AUDIO_INTERFACE_REG2_OFFSET, data);
    }
  else {
    return -1;
  }
  return 0;
}



/**
 * =======================================================
 * read_fifo_staus reads 4 bytes unsigned int data
 * from the specified channel
 * @param   : stream_base  : base address of the stream
 * @param   : fifo      : fifo ID
 * @return  : returns 4 bytes unsigned int data
 * =======================================================
 *
 * rValue(2) <= dacLeftFIFOEmpty;
	rValue(3) <= dacRightFIFOEmpty;
	rValue(4) <= dacLeftFIFOFull;
	rValue(5) <= dacRightFIFOFull;
 * =======================================================
 */
unsigned read_fifo_status (void *stream_base, FIFO_ID fifo) {
  unsigned d, buffer = 0;
  if (stream_base == NULL) {
    return -1;
  }

  if (fifo == ADC_FIFO_ID) {
    buffer = ReadReg(stream_base, AUDIO_TO_AXI_INTERFACE_REG0_OFFSET);
    d=buffer;
        printf("Reg0: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
        	      	    		  BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));

    if(buffer&0x04){
    	//LeftFIFOEmpty
    }else if(buffer&0x08){
    	//RightFIFOEmpty
    }else if(buffer&0x10){
    	//LeftFIFOFull
    }else if(buffer&0x20){
    	//RightFIFOFull
    }
  }
  else if (fifo == DAC_FIFO_ID) {
    buffer = ReadReg(stream_base, AXI_TO_AUDIO_INTERFACE_REG0_OFFSET);
    d=buffer;
    printf("Reg0: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
    	      	    		  BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));


    if(buffer&0x04){
		//LeftFIFOEmpty
	}else if(buffer&0x08){
		//RightFIFOEmpty
	}else if(buffer&0x10){
		//LeftFIFOFull
	}else if(buffer&0x20){
		//RightFIFOFull
	}
  }

  else {
    return -1;
  }
  return buffer;
}

void reset_adc_fifos(void *stream_base){
	unsigned int d = ReadReg(stream_base, AUDIO_TO_ETH_INTERFACE_REG0_OFFSET);
	      WriteReg(stream_base, AUDIO_TO_ETH_INTERFACE_REG0_OFFSET, d|0x03);
	      sleep(1);
	      WriteReg(stream_base, AUDIO_TO_ETH_INTERFACE_REG0_OFFSET, d&0xFFFFFFFC);

}


void reset_dac_fifos(void *stream_base){
	unsigned int d = ReadReg(stream_base, ETH_TO_AUDIO_INTERFACE_REG0_OFFSET);
	      WriteReg(stream_base, ETH_TO_AUDIO_INTERFACE_REG0_OFFSET, d|0x03);
	      sleep(1);
	      WriteReg(stream_base, ETH_TO_AUDIO_INTERFACE_REG0_OFFSET, d&0xFFFFFFFC);

}


void set_buf_corr_on(void *stream_base){
	unsigned int d = ReadReg(stream_base, ETH_TO_AUDIO_INTERFACE_REG0_OFFSET);
	      WriteReg(stream_base, ETH_TO_AUDIO_INTERFACE_REG0_OFFSET, d|0x20);

	      d = ReadReg(stream_base, ETH_TO_AUDIO_INTERFACE_REG0_OFFSET);
		  printf("Reg0: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
				  BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));

}


void get_e2a_status(void *stream_base){

	      unsigned d = ReadReg(stream_base, ETH_TO_AUDIO_INTERFACE_REG0_OFFSET);
		  printf("Reg0: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
				  BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));

}

void clear_buf_corr(void *stream_base){
	unsigned int d = ReadReg(stream_base, ETH_TO_AUDIO_INTERFACE_REG0_OFFSET);
	      WriteReg(stream_base, ETH_TO_AUDIO_INTERFACE_REG0_OFFSET, d&0xFFFFFFDF);

	      d = ReadReg(stream_base, ETH_TO_AUDIO_INTERFACE_REG0_OFFSET);
		  printf("Reg0: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
				  BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));

}


void set_rx_hdr_strip(void *stream_base){

	      WriteReg(stream_base, ETH_TO_AUDIO_INTERFACE_REG10_OFFSET, 0x01);
	      sleep(1);
	     unsigned int d = ReadReg(stream_base, ETH_TO_AUDIO_INTERFACE_REG10_OFFSET);
		  printf("Reg10: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
				  BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));

}


void clear_rx_hdr_strip(void *stream_base){

	      WriteReg(stream_base, ETH_TO_AUDIO_INTERFACE_REG10_OFFSET, 0x00);
	      sleep(1);
	      unsigned int d = ReadReg(stream_base, ETH_TO_AUDIO_INTERFACE_REG10_OFFSET);
		  printf("Reg10: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
				  BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));

}


void set_buf_corr_window(void *stream_base, unsigned data){

	      WriteReg(stream_base, ETH_TO_AUDIO_INTERFACE_REG23_OFFSET, data);
	     unsigned int d = ReadReg(stream_base, ETH_TO_AUDIO_INTERFACE_REG23_OFFSET);
		  printf("Reg23: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
				  BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));

}



void set_test_mode_on(void *stream_base){
		  unsigned int d = ReadReg(stream_base, AUDIO_TO_ETH_INTERFACE_REG0_OFFSET)|0x00000080;
	      WriteReg(stream_base, AUDIO_TO_ETH_INTERFACE_REG0_OFFSET, d);
}

void set_test_mode(void *stream_base, unsigned mode){
	unsigned int d=0;
	switch(mode){
	case 0:
		d = ReadReg(stream_base, AUDIO_TO_ETH_INTERFACE_REG0_OFFSET)&0xFFFFF1FF;//
		break;
	case 1:
			d = (ReadReg(stream_base, AUDIO_TO_ETH_INTERFACE_REG0_OFFSET)&0xFFFFF1FF)|0x00000200;
			break;
	case 2:
		    d = (ReadReg(stream_base, AUDIO_TO_ETH_INTERFACE_REG0_OFFSET)&0xFFFFF1FF)|0x00000400;
			break;
	case 3:
			d = (ReadReg(stream_base, AUDIO_TO_ETH_INTERFACE_REG0_OFFSET)&0xFFFFF1FF)|0x00000600;
			break;
	case 4:
				d = (ReadReg(stream_base, AUDIO_TO_ETH_INTERFACE_REG0_OFFSET)&0xFFFFF1FF)|0x00000800;
				break;

	default:
		d = ReadReg(stream_base, AUDIO_TO_ETH_INTERFACE_REG0_OFFSET)&0xFFFFF1FF;
		break;
	}
	WriteReg(stream_base, AUDIO_TO_ETH_INTERFACE_REG0_OFFSET, d);
}


void set_test_mode_off(void *stream_base){
		  unsigned int d = ReadReg(stream_base, AUDIO_TO_ETH_INTERFACE_REG0_OFFSET);
	      WriteReg(stream_base, AUDIO_TO_ETH_INTERFACE_REG0_OFFSET, d&0xFFFFFF7F);

	      d = ReadReg(stream_base, AUDIO_TO_ETH_INTERFACE_REG0_OFFSET);
	      	      printf("Reg0: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
	      	    		  BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));
}





void set_xtest_mode_on(void *stream_base){
		  //unsigned int d = ReadReg(stream_base, AUDIO_TO_ETH_INTERFACE_REG0_OFFSET)|0x00000080;
	      WriteReg(stream_base, AUDIO_TO_ETH_INTERFACE_REG7_OFFSET, 0x01);

	      unsigned int d = ReadReg(stream_base, ETH_TO_AUDIO_INTERFACE_REG7_OFFSET);
	      	printf("Reg7: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
	      		  BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));

}

void set_xtest_mode(void *stream_base, unsigned mode){
	unsigned int d=0;
	switch(mode){
	case 0:
		d = 0;//
		break;
	case 1:
			d = 1;
			break;
	case 2:
		    d = 2;
			break;
	case 3:
			d = 3;
			break;
	case 4:
			d = 4;
			break;

	default:
		d = 0;
		break;
	}
	WriteReg(stream_base, ETH_TO_AUDIO_INTERFACE_REG8_OFFSET, d);
	d = ReadReg(stream_base, ETH_TO_AUDIO_INTERFACE_REG8_OFFSET);
			      	      printf("Reg8: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
			      	    		  BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));
}


void set_xtest_mode_off(void *stream_base){
//		  unsigned int d = ReadReg(stream_base, AUDIO_TO_ETH_INTERFACE_REG7_OFFSET);
	      WriteReg(stream_base, ETH_TO_AUDIO_INTERFACE_REG7_OFFSET, 0);

	      unsigned int d = ReadReg(stream_base, ETH_TO_AUDIO_INTERFACE_REG7_OFFSET);
	      	      printf("Reg7: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
	      	    		  BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));
}


void set_sync_on(void *stream_base){
		      WriteReg(stream_base, AUDIO_TO_ETH_INTERFACE_REG6_OFFSET, 1);

		      unsigned int d = ReadReg(stream_base, AUDIO_TO_ETH_INTERFACE_REG6_OFFSET);
		      	      printf("Reg6: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
		      	    		  BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));
	}
void time_sync_en(void *stream_base){

	WriteReg(stream_base, AUDIO_TO_ETH_INTERFACE_REG0_OFFSET, 1);
	unsigned int d = ReadReg(stream_base, AUDIO_TO_ETH_INTERFACE_REG0_OFFSET);
	printf("Reg0: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
		  BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));

	}
void set_sync_pkt_dly(void *stream_base, unsigned int v){
	WriteReg(stream_base, AUDIO_TO_ETH_INTERFACE_REG5_OFFSET, v);
			unsigned int d = ReadReg(stream_base, AUDIO_TO_ETH_INTERFACE_REG4_OFFSET);
			printf("Reg0: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
			BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));
}

void set_sync_rst(void *stream_base){
	unsigned int d = ReadReg(stream_base, AUDIO_TO_ETH_INTERFACE_REG0_OFFSET);
	WriteReg(stream_base, AUDIO_TO_ETH_INTERFACE_REG0_OFFSET, d|0x02);
	sleep(1);
	WriteReg(stream_base, AUDIO_TO_ETH_INTERFACE_REG0_OFFSET, d&0xFFFFFFFD);
	d = ReadReg(stream_base, AUDIO_TO_ETH_INTERFACE_REG0_OFFSET);
	printf("Reg0: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
	BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));

}


//void set_sync_rst(void *stream_base){
//	WriteReg(stream_base, AUDIO_TO_ETH_INTERFACE_REG4_OFFSET, 1);
//	unsigned int d = ReadReg(stream_base, AUDIO_TO_ETH_INTERFACE_REG4_OFFSET);
//	printf("Reg4: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
//	BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));
//
//	sleep(1);
//
//	WriteReg(stream_base, AUDIO_TO_ETH_INTERFACE_REG4_OFFSET, 0);
//	d = ReadReg(stream_base, AUDIO_TO_ETH_INTERFACE_REG4_OFFSET);
//	printf("Reg4: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
//	BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));
//
//}

void time_sync_off(void *stream_base){

	WriteReg(stream_base, AUDIO_TO_ETH_INTERFACE_REG0_OFFSET, 0);
	unsigned int d = ReadReg(stream_base, AUDIO_TO_ETH_INTERFACE_REG0_OFFSET);
	printf("Reg0: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
	BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));

	}


void time_sync_tx_en(){
	unsigned buf = read_eth_param (full_udp_stack_ip_base_0, ETH_CONTROL_REG_ID);

			write_eth_param(full_udp_stack_ip_base_0, ETH_CONTROL_ID, buf|0x01);

	}


void time_sync_tx_off(void *stream_base){//Only for debug::dangerous will block reponses so to call it!!!!
	unsigned buf = read_eth_param (full_udp_stack_ip_base_0, ETH_CONTROL_REG_ID);
	write_eth_param(full_udp_stack_ip_base_0, ETH_CONTROL_ID, buf&0xFFFFFFFE);

	}

void initiate_sync(){
	unsigned int d = ReadReg(full_udp_stack_ip_base_0, AUDIO_TO_ETH_INTERFACE_REG0_OFFSET);
		      WriteReg(full_udp_stack_ip_base_0, AUDIO_TO_ETH_INTERFACE_REG0_OFFSET, d|1);
		      d = ReadReg(full_udp_stack_ip_base_0, AUDIO_TO_ETH_INTERFACE_REG0_OFFSET);
		      		      	      printf("Reg0-E: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
		      		      	    		  BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));

			  WriteReg(time_sync_base_0, AUDIO_TO_ETH_INTERFACE_REG0_OFFSET, 1);
						  d = ReadReg(full_udp_stack_ip_base_0, AUDIO_TO_ETH_INTERFACE_REG0_OFFSET);
											  printf("Reg0-E: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
													  BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));
		      sleep(1);

		      WriteReg(full_udp_stack_ip_base_0, AUDIO_TO_ETH_INTERFACE_REG0_OFFSET, d&0xFFFFFFFE);
		      		      d = ReadReg(full_udp_stack_ip_base_0, AUDIO_TO_ETH_INTERFACE_REG0_OFFSET);
		      		      		      	      printf("Reg0-D: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
		      		      		      	    		  BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));


		      WriteReg(time_sync_base_0, AUDIO_TO_ETH_INTERFACE_REG0_OFFSET, 0);
		      d = ReadReg(time_sync_base_0, AUDIO_TO_ETH_INTERFACE_REG0_OFFSET);
		      	      printf("Reg0-D: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
		      	    		  BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));
	}





void set_sync_off(void *stream_base){
		      WriteReg(stream_base, AUDIO_TO_ETH_INTERFACE_REG6_OFFSET, 0);
		      unsigned int d = ReadReg(full_udp_stack_ip_base_0, AUDIO_TO_ETH_INTERFACE_REG0_OFFSET);

		      		      sleep(1);

		      		      WriteReg(full_udp_stack_ip_base_0, AUDIO_TO_ETH_INTERFACE_REG0_OFFSET, d&0xFFFFFFFE);
		      d = ReadReg(stream_base, AUDIO_TO_ETH_INTERFACE_REG6_OFFSET);
		      printf("Reg6: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
		      		      	    		  BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));
	}


void set_tx_hdr_length(void *stream_base, unsigned int data){

	WriteReg(stream_base, ETH_TO_AUDIO_INTERFACE_REG9_OFFSET, data);
	unsigned int d = ReadReg(stream_base, ETH_TO_AUDIO_INTERFACE_REG9_OFFSET);
		      	      printf("Reg9: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
		      	    		  BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));
}

void set_pcnt_mode_on(void *stream_base){
		  unsigned int d = ReadReg(stream_base, AUDIO_TO_ETH_INTERFACE_REG0_OFFSET)|0x00000040;
	      WriteReg(stream_base, AUDIO_TO_ETH_INTERFACE_REG0_OFFSET, d);

	      d = ReadReg(stream_base, AUDIO_TO_ETH_INTERFACE_REG0_OFFSET);
	      	      printf("Reg0: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
	      	    		  BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));
}


void set_pcnt_mode_off(void *stream_base){
		  unsigned int d = ReadReg(stream_base, AUDIO_TO_ETH_INTERFACE_REG0_OFFSET);
	      WriteReg(stream_base, AUDIO_TO_ETH_INTERFACE_REG0_OFFSET, d&0xFFFFFFBF);

	      d = ReadReg(stream_base, AUDIO_TO_ETH_INTERFACE_REG0_OFFSET);
	      	      printf("Reg0: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
	      	    		  BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));
}



void set_stream_send_on(void *stream_base){
		  unsigned int d = ReadReg(stream_base, AUDIO_TO_ETH_INTERFACE_REG0_OFFSET);
	      WriteReg(stream_base, AUDIO_TO_ETH_INTERFACE_REG0_OFFSET, d|0x00000100);
	      d = ReadReg(stream_base, AUDIO_TO_ETH_INTERFACE_REG0_OFFSET);
	      printf("Reg0: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
	    		  BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));

}


void set_stream_send_off(void *stream_base){
		  unsigned int d = ReadReg(stream_base, AUDIO_TO_ETH_INTERFACE_REG0_OFFSET);
	      WriteReg(stream_base, AUDIO_TO_ETH_INTERFACE_REG0_OFFSET, d&0xFFFFFEFF);

	      d = ReadReg(stream_base, AUDIO_TO_ETH_INTERFACE_REG0_OFFSET);
		  printf("Reg0: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
				  BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));
}


unsigned int get_test_mode(void *stream_base){
		if(ReadReg(stream_base, AUDIO_TO_ETH_INTERFACE_REG0_OFFSET)&0x00000080){
			printf("We are in test mode no ADC just a counter\n");
	      return 1;
		}else{
			printf("We are in run mode.\n");
		}
}

void set_stream_plen(void *stream_base, unsigned int data){

	WriteReg(stream_base, AUDIO_TO_ETH_INTERFACE_REG3_OFFSET, data);

}
unsigned int get_stream_plen(void *stream_base){

	return ReadReg(stream_base, AUDIO_TO_ETH_INTERFACE_REG3_OFFSET);

}
void set_stream_blen(void *stream_base, unsigned int data){

	WriteReg(stream_base, ETH_TO_AUDIO_INTERFACE_REG5_OFFSET, data);

}

unsigned int get_instant_playout_time(void *stream_base){

	unsigned int d = ReadReg(stream_base, ETH_TO_AUDIO_INTERFACE_REG16_OFFSET);
	printf("Reg16: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
		  BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));

	return d;
}


unsigned int get_rx_time_code(void *stream_base){

	unsigned int d = ReadReg(stream_base, ETH_TO_AUDIO_INTERFACE_REG18_OFFSET);
	printf("Reg18: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
		  BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));

	return d;
}


unsigned int get_rx_time_code_eff(void *stream_base){

	unsigned int d = ReadReg(stream_base, ETH_TO_AUDIO_INTERFACE_REG19_OFFSET);
	printf("Reg19: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
		  BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));

	return d;
}


unsigned int get_rx_time_code_fout(void *stream_base){

	unsigned int d = ReadReg(stream_base, ETH_TO_AUDIO_INTERFACE_REG21_OFFSET);
	printf("Reg21: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
		  BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));

	return d;
}



unsigned int get_rx_time_code_occ(void *stream_base){

	unsigned int d = ReadReg(stream_base, ETH_TO_AUDIO_INTERFACE_REG20_OFFSET);
	printf("Reg20: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
		  BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));

	return d;
}


unsigned int get_playout_delay(void *stream_base){

	unsigned int d = ReadReg(stream_base, ETH_TO_AUDIO_INTERFACE_REG11_OFFSET);
	printf("Reg11: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
		  BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));

	return d;
}


void set_loop_limit(void *stream_base, unsigned int data){

	WriteReg(stream_base, ETH_TO_AUDIO_INTERFACE_REG29_OFFSET, data);
	unsigned int d = ReadReg(stream_base, ETH_TO_AUDIO_INTERFACE_REG11_OFFSET);
		printf("Reg29: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
			  BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));

}

void set_playout_delay(void *stream_base, unsigned int data){

	WriteReg(stream_base, ETH_TO_AUDIO_INTERFACE_REG11_OFFSET, data);
	unsigned int d = ReadReg(stream_base, ETH_TO_AUDIO_INTERFACE_REG11_OFFSET);
		printf("Reg11: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
			  BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));

}

unsigned int get_instant_playout_writes(void *stream_base){

	unsigned int d = ReadReg(stream_base, ETH_TO_AUDIO_INTERFACE_REG15_OFFSET);
	printf("Reg15: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
		  BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));

	return d;
}


unsigned int get_sync_time(void *stream_base){

	unsigned int d = ReadReg(stream_base, ETH_TO_AUDIO_INTERFACE_REG13_OFFSET);
	printf("Reg13: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
		  BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));

	return d;
}


unsigned int get_all_eth2audio(void *stream_base){

	unsigned int d = ReadReg(stream_base, ETH_TO_AUDIO_INTERFACE_REG0_OFFSET);
	printf("Reg0: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
	BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));

	d = ReadReg(stream_base, ETH_TO_AUDIO_INTERFACE_REG1_OFFSET);
	printf("Reg1: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
	BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));

	d = ReadReg(stream_base, ETH_TO_AUDIO_INTERFACE_REG2_OFFSET);
	printf("Reg2: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
	BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));

	d = ReadReg(stream_base, ETH_TO_AUDIO_INTERFACE_REG3_OFFSET);
	printf("Reg3: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
	BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));

	d = ReadReg(stream_base, ETH_TO_AUDIO_INTERFACE_REG4_OFFSET);
	printf("Reg4: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
	BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));

	d = ReadReg(stream_base, ETH_TO_AUDIO_INTERFACE_REG5_OFFSET);
	printf("Reg5: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
	BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));

	d = ReadReg(stream_base, ETH_TO_AUDIO_INTERFACE_REG6_OFFSET);
	printf("Reg6: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
	BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));

	d = ReadReg(stream_base, ETH_TO_AUDIO_INTERFACE_REG7_OFFSET);
	printf("Reg7: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
	BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));

	d = ReadReg(stream_base, ETH_TO_AUDIO_INTERFACE_REG8_OFFSET);
	printf("Reg8: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
	BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));

	d = ReadReg(stream_base, ETH_TO_AUDIO_INTERFACE_REG9_OFFSET);
	printf("Reg9: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
	BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));

	d = ReadReg(stream_base, ETH_TO_AUDIO_INTERFACE_REG10_OFFSET);
	printf("Reg10: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
	BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));

	d = ReadReg(stream_base, ETH_TO_AUDIO_INTERFACE_REG11_OFFSET);
	printf("Reg11: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
	BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));

	d = ReadReg(stream_base, ETH_TO_AUDIO_INTERFACE_REG12_OFFSET);
	printf("Reg12: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
	BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));

	d = ReadReg(stream_base, ETH_TO_AUDIO_INTERFACE_REG13_OFFSET);
	printf("Reg13: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
	BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));

	d = ReadReg(stream_base, ETH_TO_AUDIO_INTERFACE_REG14_OFFSET);
	printf("Reg14: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
	BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));

	d = ReadReg(stream_base, ETH_TO_AUDIO_INTERFACE_REG15_OFFSET);
	printf("Reg15: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
	BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));

	d = ReadReg(stream_base, ETH_TO_AUDIO_INTERFACE_REG16_OFFSET);
	printf("Reg16: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
	BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));

	d = ReadReg(stream_base, ETH_TO_AUDIO_INTERFACE_REG17_OFFSET);
	printf("Reg17: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
	BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));

	d = ReadReg(stream_base, ETH_TO_AUDIO_INTERFACE_REG18_OFFSET);
	printf("Reg18: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
	BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));

	d = ReadReg(stream_base, ETH_TO_AUDIO_INTERFACE_REG19_OFFSET);
	printf("Reg19: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
	BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));

	d = ReadReg(stream_base, ETH_TO_AUDIO_INTERFACE_REG23_OFFSET);
		printf("Reg23: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
		BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));
	d = ReadReg(stream_base, ETH_TO_AUDIO_INTERFACE_REG24_OFFSET);
		printf("Reg24: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
		BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));
	return d;
}

unsigned int get_dac_fifo_occ(void *stream_base){

	unsigned int d = ReadReg(stream_base, ETH_TO_AUDIO_INTERFACE_REG23_OFFSET);
	printf("Reg23: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
	BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));

	return d;
}


unsigned int get_dac_ofifo_occ(void *stream_base){

	unsigned int d = ReadReg(stream_base, ETH_TO_AUDIO_INTERFACE_REG23_OFFSET);
	printf("Reg23: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
	BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));

	return d;
}

unsigned int get_dac_efifo_occ(void *stream_base){

	unsigned int d = ReadReg(stream_base, ETH_TO_AUDIO_INTERFACE_REG24_OFFSET);
	printf("Reg24: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
	BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));

	return d;
}

unsigned int get_dummy(void *stream_base){

	unsigned int d = ReadReg(stream_base, ETH_TO_AUDIO_INTERFACE_REG18_OFFSET);
	printf("Reg18: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
		  BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));

	return d;
}


unsigned int get_instant_playout_reads(void *stream_base){

	unsigned int d = ReadReg(stream_base, ETH_TO_AUDIO_INTERFACE_REG14_OFFSET);
	printf("Reg14: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
		  BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));

	return d;
}


void reset_pkt_seq(void *stream_base){
	unsigned int d = ReadReg(stream_base, PKT_SEQ_INTERFACE_REG1_OFFSET);
	WriteReg(stream_base, PKT_SEQ_INTERFACE_REG1_OFFSET, d|0x01);
	//WriteReg(stream_base, PKT_TIME_ENFORCER_REG0_OFFSET, 0x01);
	sleep(1);

	//WriteReg(stream_base, PKT_TIME_ENFORCER_REG0_OFFSET, 0x00);
	WriteReg(stream_base, PKT_SEQ_INTERFACE_REG1_OFFSET, d&0xFFFFFFFE);
d = ReadReg(stream_base, PKT_SEQ_INTERFACE_REG1_OFFSET);
printf("Reg1: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
	  BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));

}

void disble_pkt_seq(void *stream_base){
	unsigned int d = ReadReg(stream_base, PKT_SEQ_INTERFACE_REG1_OFFSET);
	WriteReg(stream_base, PKT_SEQ_INTERFACE_REG1_OFFSET, d|0x02);

	d = ReadReg(stream_base, PKT_SEQ_INTERFACE_REG1_OFFSET);
	printf("Reg1: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
	  BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));

}


void enable_pkt_seq(void *stream_base){
	unsigned int d = ReadReg(stream_base, PKT_SEQ_INTERFACE_REG1_OFFSET);
	WriteReg(stream_base, PKT_SEQ_INTERFACE_REG1_OFFSET, d&0xFFFFFFFD);
	d = ReadReg(stream_base, PKT_SEQ_INTERFACE_REG1_OFFSET);
	printf("Reg1: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
	  BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));

}


void set_stream_porder(void *stream_base, unsigned int data){

	WriteReg(stream_base, PKT_SEQ_INTERFACE_REG0_OFFSET, data);
	unsigned int d = ReadReg(stream_base, PKT_SEQ_INTERFACE_REG0_OFFSET);
		printf("Reg0: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
						  BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));
}

void set_stream_pkt_wait_enable(void *stream_base){

	unsigned int d = ReadReg(stream_base, PKT_SEQ_INTERFACE_REG1_OFFSET);
		WriteReg(stream_base, PKT_SEQ_INTERFACE_REG1_OFFSET, d&0xFFFFFFFB);

		d = ReadReg(stream_base, PKT_SEQ_INTERFACE_REG1_OFFSET);
			printf("Reg1: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
							  BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));
}


void set_stream_pkt_wait_disable(void *stream_base){

	unsigned int d = ReadReg(stream_base, PKT_SEQ_INTERFACE_REG1_OFFSET);
		WriteReg(stream_base, PKT_SEQ_INTERFACE_REG1_OFFSET, d|0x00000004);

		d = ReadReg(stream_base, PKT_SEQ_INTERFACE_REG1_OFFSET);
			printf("Reg1: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
							  BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));
}


void set_stream_pkt_send_delay_enable(void *stream_base){

	unsigned int d = ReadReg(stream_base, PKT_SEQ_INTERFACE_REG1_OFFSET);
		WriteReg(stream_base, PKT_SEQ_INTERFACE_REG1_OFFSET, d&0xFFFFFFF7);

		d = ReadReg(stream_base, PKT_SEQ_INTERFACE_REG1_OFFSET);
			printf("Reg1: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
							  BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));
}


void set_stream_pkt_send_delay_disable(void *stream_base){

	unsigned int d = ReadReg(stream_base, PKT_SEQ_INTERFACE_REG1_OFFSET);
		WriteReg(stream_base, PKT_SEQ_INTERFACE_REG1_OFFSET, d|0x00000008);

		d = ReadReg(stream_base, PKT_SEQ_INTERFACE_REG1_OFFSET);
			printf("Reg1: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
							  BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));
}



void set_plc_disable(void *stream_base){
unsigned int d = ReadReg(stream_base, PKT_SEQ_INTERFACE_REG0_OFFSET);
		WriteReg(stream_base, PKT_SEQ_INTERFACE_REG0_OFFSET, d&0xFFFFFFEF);


		d = ReadReg(stream_base, PKT_SEQ_INTERFACE_REG0_OFFSET);
			printf("Reg0: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
							  BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));
}


void set_plc_enable(void *stream_base){

	unsigned int d = ReadReg(stream_base, PKT_SEQ_INTERFACE_REG0_OFFSET);
		WriteReg(stream_base, PKT_SEQ_INTERFACE_REG0_OFFSET, d|0x00000010);

		d = ReadReg(stream_base, PKT_SEQ_INTERFACE_REG0_OFFSET);
			printf("Reg0: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
							  BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));
}



void set_stream_pkt_wait(void *stream_base, unsigned long data){

	WriteReg(stream_base, PKT_SEQ_INTERFACE_REG2_OFFSET, data&0x00000000FFFFFFFF);
	WriteReg(stream_base, PKT_SEQ_INTERFACE_REG3_OFFSET, (data&0xFFFFFFFF00000000)>>32);

	unsigned int d = ReadReg(stream_base, PKT_SEQ_INTERFACE_REG2_OFFSET);
	printf("Reg2: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
					  BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));
	d = ReadReg(stream_base, PKT_SEQ_INTERFACE_REG3_OFFSET);
	printf("Reg3: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
					  BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));

}

void set_stream_pkt_send_delay(void *stream_base, unsigned long data){

	WriteReg(stream_base, PKT_SEQ_INTERFACE_REG4_OFFSET, data&0x00000000FFFFFFFF);
	WriteReg(stream_base, PKT_SEQ_INTERFACE_REG5_OFFSET, (data&0xFFFFFFFF00000000)>>32);

	unsigned int d = ReadReg(stream_base, PKT_SEQ_INTERFACE_REG4_OFFSET);
	printf("Reg4: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
					  BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));
	d = ReadReg(stream_base, PKT_SEQ_INTERFACE_REG5_OFFSET);
	printf("Reg5: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
					  BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));

}


void set_stream_rx_pkt_size(void *stream_base, unsigned long data){

	WriteReg(stream_base, PKT_SEQ_INTERFACE_REG7_OFFSET, data&0x00000000FFFFFFFF);


	unsigned int d = ReadReg(stream_base, PKT_SEQ_INTERFACE_REG7_OFFSET);
	printf("Reg7: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
					  BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));

}


void set_stream_rx_buf_lim(void *stream_base, unsigned long data){

	WriteReg(stream_base, PKT_SEQ_INTERFACE_REG6_OFFSET, data&0x00000000FFFFFFFF);


	unsigned int d = ReadReg(stream_base, PKT_SEQ_INTERFACE_REG6_OFFSET);
	printf("Reg6: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
					  BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));

}


void set_stream_rx_buf_dlim(void *stream_base, unsigned int data){

	WriteReg(stream_base, PKT_SEQ_INTERFACE_REG9_OFFSET, data);


	unsigned int d = ReadReg(stream_base, PKT_SEQ_INTERFACE_REG9_OFFSET);
	printf("Reg9: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
					  BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));

}



void set_stream_rx_buf_docc_lim(void *stream_base, unsigned int data){

	WriteReg(stream_base, PKT_SEQ_INTERFACE_REG11_OFFSET, data);


//	unsigned int d = ReadReg(stream_base, PKT_SEQ_INTERFACE_REG11_OFFSET);
//	printf("Reg11: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
//					  BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));

}




void set_stream_rx_buf_slack(void *stream_base, unsigned int data){

	WriteReg(stream_base, PKT_SEQ_INTERFACE_REG10_OFFSET, data);


//	unsigned int d = ReadReg(stream_base, PKT_SEQ_INTERFACE_REG11_OFFSET);
//	printf("Reg11: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
//					  BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));

}


void set_stream_rx_buf_lim2(void *stream_base, unsigned long data){

	WriteReg(stream_base, PKT_SEQ_INTERFACE_REG6_OFFSET, data&0x00000000FFFFFFFF);


	unsigned int d = ReadReg(stream_base, PKT_SEQ_INTERFACE_REG6_OFFSET);
	printf("Reg6: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
					  BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));

}


void set_plc_replace_delay(void *stream_base, unsigned int data){

	WriteReg(stream_base, PKT_SEQ_INTERFACE_REG8_OFFSET, data);


	unsigned int d = ReadReg(stream_base, PKT_SEQ_INTERFACE_REG8_OFFSET);
	printf("Reg8: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
					  BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));

}

void set_seq_replace_lock_delay(void *stream_base, unsigned int data){

	WriteReg(stream_base, PKT_SEQ_INTERFACE_REG10_OFFSET, data);


	unsigned int d = ReadReg(stream_base, PKT_SEQ_INTERFACE_REG10_OFFSET);
	printf("Reg10: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
					  BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));

}


unsigned int get_stream_porder(void *stream_base){

	unsigned int d = ReadReg(stream_base, PKT_SEQ_INTERFACE_REG0_OFFSET);
		printf("Reg0: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
						  BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));
		return d;
}


unsigned int get_seq_gs(void *stream_base){

	unsigned int d = ReadReg(stream_base, PKT_SEQ_INTERFACE_REG22_OFFSET);
	printf("replace_inprogress_out, new_pkt_ready_out, replace_pkt_end_in, replace_pkt_in, 1'b0, to_audio_m_axis_tlast, to_audio_m_axis_tready, to_audio_m_axis_tvalid, 1'b0, fm_udp_s_axis_tlast, fm_udp_s_axis_tready, fm_udp_s_axis_tvalid");
		printf("Reg22: "BYTE_TO_BINARY_NIB_PATTERN" "BYTE_TO_BINARY_NIB_PATTERN" "BYTE_TO_BINARY_NIB_PATTERN" "BYTE_TO_BINARY_NIB_PATTERN"\n",
						  BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));
		return d;
}


unsigned int get_pkts_dropped(void *stream_base){
	return ReadReg(stream_base, PKT_TIME_ENFORCER_REG12_OFFSET);
}

unsigned int get_pkts_replaced(void *stream_base){
	return ReadReg(stream_base, PKT_TIME_ENFORCER_REG13_OFFSET);
}

unsigned int get_seq_pkts_rx(void *stream_base){
	return ReadReg(stream_base, PKT_TIME_ENFORCER_REG21_OFFSET);
}

unsigned int get_seq_pkts_tx(void *stream_base){
	return ReadReg(stream_base, PKT_TIME_ENFORCER_REG20_OFFSET);
}

unsigned int get_seq_pkts_ov(void *stream_base){
	return ReadReg(stream_base, PKT_TIME_ENFORCER_REG19_OFFSET);
}




unsigned long get_pkts_drop_spkt_limit(void *stream_base){
	unsigned long d1 = ReadReg(stream_base, PKT_TIME_ENFORCER_REG3_OFFSET);
	unsigned long d2 = ReadReg(stream_base, PKT_TIME_ENFORCER_REG4_OFFSET);

	return (d2<<32)|d1;
}


unsigned long get_pkts_drop_spkt_locked_limit(void *stream_base){
	unsigned long d1 = ReadReg(stream_base, PKT_TIME_ENFORCER_REG21_OFFSET);
	unsigned long d2 = ReadReg(stream_base, PKT_TIME_ENFORCER_REG22_OFFSET);

	return (d2<<32)|d1;
}


unsigned long get_pkts_adrop_spkt_limit(void *stream_base){
	unsigned long d1 = ReadReg(stream_base, PKT_TIME_ENFORCER_REG5_OFFSET);
	unsigned long d2 = ReadReg(stream_base, PKT_TIME_ENFORCER_REG6_OFFSET);

	return (d2<<32)|d1;
}


unsigned int set_pkts_ptrx_lock(void *stream_base, unsigned int d1){
	WriteReg(stream_base, PKT_TIME_ENFORCER_REG8_OFFSET,d1);
	unsigned int d = ReadReg(stream_base, PKT_TIME_ENFORCER_REG8_OFFSET);
	if(d&0x01==1){
			printf("Packet timming unit locked..:: %08X\n",d);
		}else{
			printf("Packet timming unit unlocked..:: %08X\n",d);
		}
	printf("Reg8: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
			      BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));
	return d;
}


unsigned int set_pkts_pt_accum(void *stream_base, unsigned int d1){
	WriteReg(stream_base, PKT_TIME_ENFORCER_REG9_OFFSET,d1);
	unsigned int d = ReadReg(stream_base, PKT_TIME_ENFORCER_REG9_OFFSET);
	if(d&0x01==1){
			printf("Packet timming unit accum enabled..:: %08X\n",d);
		}else{
			printf("Packet timming unit accum disabled..:: %08X\n",d);
		}
	printf("Reg9: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
			      BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));
	return d;
}


unsigned int get_pkts_pt_accum(void *stream_base){
	unsigned int d = ReadReg(stream_base, PKT_TIME_ENFORCER_REG9_OFFSET);
	if(d&0x01==1){
			printf("Packet timming unit accum enabled..:: %08X\n",d);
		}else{
			printf("Packet timming unit accum disabled..:: %08X\n",d);
		}
	printf("Reg9: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
			      BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));
	return d;
}



unsigned int set_pkts_pt_diff_status(void *stream_base, unsigned int d1){
	WriteReg(stream_base, PKT_TIME_ENFORCER_REG20_OFFSET,d1);
	unsigned int d = ReadReg(stream_base, PKT_TIME_ENFORCER_REG20_OFFSET);
	if(d&0x01==1){
			printf("Packet timming unit diff enabled..:: %08X\n",d);
		}else{
			printf("Packet timming unit diff disabled..:: %08X\n",d);
		}
	printf("Reg20: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
			      BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));
	return d;
}


unsigned int get_pkts_pt_diff_status(void *stream_base){
	unsigned int d = ReadReg(stream_base, PKT_TIME_ENFORCER_REG20_OFFSET);
	if(d&0x01==1){
			printf("Packet timming unit accum enabled..:: %08X\n",d);
		}else{
			printf("Packet timming unit accum disabled..:: %08X\n",d);
		}
	printf("Reg20: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
			      BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));
	return d;
}

unsigned int get_pkts_ptrx_lock(void *stream_base){
	unsigned int d = ReadReg(stream_base, PKT_TIME_ENFORCER_REG8_OFFSET);
	if(d&0x01==1){
			printf("Packet timming unit locked..:: %08X\n",d);
		}else{
			printf("Packet timming unit unlocked..:: %08X\n",d);
		}
	printf("Reg8: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
		      BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));
	return d;
}


unsigned int get_pkts_pt_disable(void *stream_base){
	unsigned int d = ReadReg(stream_base, PKT_TIME_ENFORCER_REG2_OFFSET);
	if(d&0x01==1){
		printf("Packet timming unit disabled..:: %08X\n",d);
	}else{
		printf("Packet timming unit enabled..:: %08X\n",d);
	}
	printf("Reg2: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
			      BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));
	return d;
}

unsigned int get_stream_blen(void *stream_base){

	return ReadReg(stream_base, ETH_TO_AUDIO_INTERFACE_REG5_OFFSET);

}


unsigned int get_stream_readout_delay(void *stream_base){
	unsigned int d1 = ReadReg(stream_base, ETH_TO_AUDIO_INTERFACE_REG6_OFFSET);
	printf("The  RX buffer occupancy :: %d\n",((int)d1));
	return d1;

}


unsigned int get_stream_net_delay(void *stream_base){

	return ReadReg(stream_base, PKT_TIME_ENFORCER_REG10_OFFSET);

}

unsigned int get_stream_pktbuild_delay(void *stream_base){

	return ReadReg(stream_base, PKT_TIME_ENFORCER_REG11_OFFSET);

}


unsigned long get_stream_accum_pkt_delay(void *stream_base){

		unsigned int d1 = ReadReg(stream_base, PKT_TIME_ENFORCER_REG12_OFFSET);
		unsigned int d2 = ReadReg(stream_base, PKT_TIME_ENFORCER_REG13_OFFSET);
		printf("The Accum Packet to Packet delay is :: %d\n",(d1));
		printf("The Accum Packet to Packet delay is :: %f ms\n",(d1)/96);
		return d1;
	}

unsigned long get_stream_spkt_delay(void *stream_base){

		unsigned int d1 = ReadReg(stream_base, PKT_TIME_ENFORCER_REG14_OFFSET);
		unsigned int d2 = ReadReg(stream_base, PKT_TIME_ENFORCER_REG15_OFFSET);
		printf("The Packet to Packet delay is :: %d\n",(d1));
		printf("The Packet to Packet delay is :: %f ms\n",(d1)/96);
		return d1;

}

unsigned int set_stream_net_delay(void *stream_base, unsigned int d1){
	WriteReg(stream_base, PKT_TIME_ENFORCER_REG10_OFFSET,d1);
	unsigned int d = ReadReg(stream_base, PKT_TIME_ENFORCER_REG10_OFFSET);

	printf("Reg10: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
			      BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));
	return d;
}


unsigned int set_stream_pktbuild_delay(void *stream_base, unsigned int d1){
	WriteReg(stream_base, PKT_TIME_ENFORCER_REG11_OFFSET,d1);
	unsigned int d = ReadReg(stream_base, PKT_TIME_ENFORCER_REG11_OFFSET);

	printf("Reg11: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
			      BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));
	return d;
}



void set_spkt_to_pkt_delay_limit(void *stream_base, unsigned long data){

	WriteReg(stream_base, PKT_TIME_ENFORCER_REG3_OFFSET, data&0x00000000FFFFFFFF);
	WriteReg(stream_base, PKT_TIME_ENFORCER_REG4_OFFSET, (data&0xFFFFFFFF00000000)>>32);

}


void set_spkt_to_pkt_locked_delay_limit(void *stream_base, unsigned long data){

	WriteReg(stream_base, PKT_TIME_ENFORCER_REG21_OFFSET, data&0x00000000FFFFFFFF);
	WriteReg(stream_base, PKT_TIME_ENFORCER_REG22_OFFSET, (data&0xFFFFFFFF00000000)>>32);

}



void set_diff_pkt_to_pkt_delay_limit(void *stream_base, unsigned long data){

	WriteReg(stream_base, PKT_TIME_ENFORCER_REG16_OFFSET, data&0x00000000FFFFFFFF);
	WriteReg(stream_base, PKT_TIME_ENFORCER_REG17_OFFSET, (data&0xFFFFFFFF00000000)>>32);

	unsigned int d = ReadReg(stream_base, PKT_TIME_ENFORCER_REG16_OFFSET);

		printf("Reg16: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
				      BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));


		d = ReadReg(stream_base, PKT_TIME_ENFORCER_REG17_OFFSET);

	printf("Reg17: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
				  BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));

}

unsigned long get_diff_pkt_to_pkt_delay_limit(void *stream_base){

		unsigned int d1 = ReadReg(stream_base, PKT_TIME_ENFORCER_REG16_OFFSET);
		unsigned int d2 = ReadReg(stream_base, PKT_TIME_ENFORCER_REG17_OFFSET);
		printf("The Diff Packet to Packet delay Limit is :: %d\n",(d1));
		printf("The Diff Packet to Packet delay Limit is :: %f ms\n",(d1)/96);
		return d1;
	}


unsigned long get_diff_pkt_to_pkt_delay(void *stream_base){

		unsigned int d1 = ReadReg(stream_base, PKT_TIME_ENFORCER_REG18_OFFSET);
		unsigned int d2 = ReadReg(stream_base, PKT_TIME_ENFORCER_REG19_OFFSET);
		printf("The Accum Packet to Packet delay is :: %d\n",(d1));
		printf("The Accum Packet to Packet delay is :: %f ms\n",(d1)/96.0);
		return d1;
	}
unsigned long get_in_exec_state(void *stream_base){

	unsigned int d = ReadReg(stream_base, PKT_TIME_ENFORCER_REG15_OFFSET);

	printf("IN_EXEC_STATE: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
				      BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));

	return d;
}

unsigned long get_out_exec_state(void *stream_base){

	unsigned int d = ReadReg(stream_base, PKT_TIME_ENFORCER_REG16_OFFSET);

	printf("OUT_EXEC_STATE: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
				      BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));

	return d;
}
void set_accum_pkt_to_pkt_delay_limit(void *stream_base, unsigned long data){

	WriteReg(stream_base, PKT_TIME_ENFORCER_REG5_OFFSET, data&0x00000000FFFFFFFF);
	WriteReg(stream_base, PKT_TIME_ENFORCER_REG6_OFFSET, (data&0xFFFFFFFF00000000)>>32);

}


void reset_pkt_time_enf(void *stream_base){
	WriteReg(stream_base, PKT_TIME_ENFORCER_REG0_OFFSET, 0x01);
	sleep(1);
	WriteReg(stream_base, PKT_TIME_ENFORCER_REG0_OFFSET, 0x00);
}
void reset_pkt_time_enf_rx(void *stream_base){
	WriteReg(stream_base, PKT_TIME_ENFORCER_REG1_OFFSET, 0x01);
	sleep(1);
	WriteReg(stream_base, PKT_TIME_ENFORCER_REG1_OFFSET, 0x00);
}
void pkt_time_enf_disable(void *stream_base){
	WriteReg(stream_base, PKT_TIME_ENFORCER_REG2_OFFSET, 0x01);
	unsigned int d = ReadReg(stream_base, PKT_TIME_ENFORCER_REG2_OFFSET);
	printf("Reg2: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
			      BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));
}
void pkt_time_enf_enable(void *stream_base){
	WriteReg(stream_base, PKT_TIME_ENFORCER_REG2_OFFSET, 0x00);
	unsigned int d = ReadReg(stream_base, PKT_TIME_ENFORCER_REG2_OFFSET);
	printf("Reg2: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
			      BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));
}


void pkt_to_pkt_locked_drop_enable(void *stream_base){
	WriteReg(stream_base, PKT_TIME_ENFORCER_REG23_OFFSET, 0x01);
	unsigned int d = ReadReg(stream_base, PKT_TIME_ENFORCER_REG23_OFFSET);
	printf("Reg23: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
			      BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));
}


void pkt_to_pkt_locked_drop_disable(void *stream_base){
	WriteReg(stream_base, PKT_TIME_ENFORCER_REG23_OFFSET, 0x00);
	unsigned int d = ReadReg(stream_base, PKT_TIME_ENFORCER_REG23_OFFSET);
	printf("Reg23: "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN" "BYTE_TO_BINARY_PATTERN"\n",
			      BYTE_TO_BINARY(d>>24),BYTE_TO_BINARY(d>>16),BYTE_TO_BINARY(d>>8), BYTE_TO_BINARY(d));
}



void stream_source_select(void *stream_base, FIFO_ID fifo, unsigned int data){
	  if (fifo == ADC_FIFO_ID) {
	      WriteReg(stream_base, ETH_TO_AUDIO_INTERFACE_REG0_OFFSET, data);
	    }
	    else if (fifo == DAC_FIFO_ID) {
	      WriteReg(stream_base, AUDIO_TO_AXI_INTERFACE_REG0_OFFSET, data);
	    }
}

void write_audio_to_eth_param(void *audio_to_eth_base, AUDIO_TO_ETH_REG_ID param, unsigned int data){
	if (param == PAYLEN_REG_ID) {
		  WriteReg(audio_to_eth_base, ETH_UDP_REG3_OFFSET, data);
		}
}


void write_eth_param(void *eth_base, ETH_PARAM_ID param, unsigned int data){
	if (param == LOCAL_IP_ID) {
		  WriteReg(eth_base, ETH_UDP_REG2_OFFSET, data);
		}
		else if (param == GTWAY_IP_ID) {
		  WriteReg(eth_base, ETH_UDP_REG3_OFFSET, data);
		}
		else if (param == MASK_IP_ID) {
		  WriteReg(eth_base, ETH_UDP_REG4_OFFSET, data);
		}
	else if (param == DEST_IP_ID) {
	  WriteReg(eth_base, ETH_UDP_REG5_OFFSET, data);
	  WriteReg(eth_base, ETH_UDP_REG7_OFFSET, data);
	  WriteReg(eth_base, ETH_UDP_REG9_OFFSET, data);
	  WriteReg(eth_base, ETH_UDP_REG11_OFFSET, data);
	  WriteReg(eth_base, ETH_UDP_REG13_OFFSET, data);
	}
	else if (param == DEST_PORT_ID) {
		  WriteReg(eth_base, ETH_UDP_REG6_OFFSET, data);
		  WriteReg(eth_base, ETH_UDP_REG8_OFFSET, data);
		  WriteReg(eth_base, ETH_UDP_REG10_OFFSET, data);
		  WriteReg(eth_base, ETH_UDP_REG12_OFFSET, data);
		  WriteReg(eth_base, ETH_UDP_REG14_OFFSET, data);
		}

	else if (param == PKT_LEN_ID) {//not writen from command but depends on payload command
		  WriteReg(eth_base, ETH_UDP_REG18_OFFSET, data);
		}
	else if (param == ETH_CONTROL_ID) {//not writen from command but depends on payload command
			  WriteReg(eth_base, ETH_UDP_REG15_OFFSET, data);
			}
	else if (param == SYNC_IP_ID) {
				  WriteReg(eth_base, ETH_UDP_REG16_OFFSET, data);
				}
		else if (param == SYNC_PORT_ID) {
				  WriteReg(eth_base, ETH_UDP_REG17_OFFSET, data);
				}
//	else if (param == MAC_ID) {
//	  WriteReg(eth_base, ETH_UDP_REG0_OFFSET, (unsigned)(data & 0x00FFFFFF));
//	  WriteReg(eth_base, ETH_UDP_REG1_OFFSET, (unsigned)(data & 0x00FFFFFF));
//	}
}


void write_eth_mac(void *eth_base, ETH_PARAM_ID param, unsigned char * mac){
	if (param == MAC_ID) {

	  WriteReg(eth_base, ETH_UDP_REG0_OFFSET, (unsigned)(mac[0]<<16|mac[1]<<8|mac[2]));
	  WriteReg(eth_base, ETH_UDP_REG1_OFFSET, (unsigned)(mac[3]<<16|mac[4]<<8|mac[5]));

	}else{

	}
}



unsigned long read_eth_mac(void *eth_base, ETH_PARAM_ID param, char* ip){
	unsigned int d0, d1;
	unsigned long mac ;
	if (param == MAC_ID) {
	  d0 = ReadReg(eth_base, ETH_UDP_REG0_OFFSET);
	  d1 = ReadReg(eth_base, ETH_UDP_REG1_OFFSET);
	  printf("MAC Address Part 1: 0x%X\n",d0);
	  printf("MAC Address Part 1: 0x%X\n",d1);
	      printf("MAC Address: %02X.%02X.%02X.%02X.%02X.%02X\n", (d0 >> 16) & 0x000000FF, (d0 >> 8) & 0x000000FF, d0 & 0x000000FF, (d1 >> 16) & 0x000000FF, (d1 >> 8) & 0x000000FF, d1 & 0x000000FF);
	      sprintf(ip, "%02X%02X%02X%02X%02X%02X\0", (d0 >> 16) & 0x000000FF, (d0 >> 8) & 0x000000FF, d0 & 0x000000FF, (d1 >> 16) & 0x000000FF, (d1 >> 8) & 0x000000FF, d1 & 0x000000FF);
	}else{
		return -1;
	}
	mac = ((d0&0x00FFFFFF)<<24) | (d1&0x00FFFFFF);
	return mac;
}

/**
 * =======================================================
 * read_eth_status_regs reads 4 bytes unsigned int data
 * from the specified channel
 * @param   : eth_base  : base address of the stream
 * @param   : param      : reg ID
 * @return  : returns 4 bytes unsigned int data
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
 * =======================================================
 */
unsigned read_eth_param (void *eth_base, ETH_REG_ID param) {
	unsigned buffer = 0;
	if (eth_base == NULL) {
		return -1;
	}

    if (param == LOCAL_IP_REG_ID) {
    	buffer = ReadReg(eth_base, ETH_UDP_REG2_OFFSET);
	}
	else if (param == GTWAY_IP_REG_ID) {
		buffer = ReadReg(eth_base, ETH_UDP_REG3_OFFSET);
	}
	else if (param == MASK_IP_REG_ID) {
		buffer = ReadReg(eth_base, ETH_UDP_REG4_OFFSET);
	}
  	else if (param == DEST_IP_REG_ID) {
  		buffer = ReadReg(eth_base, ETH_UDP_REG5_OFFSET);
  	}
  	else if (param == DEST_PORT_REG_ID) {
  		buffer = ReadReg(eth_base, ETH_UDP_REG6_OFFSET);
	}
  	else if (param == LOOP_TIMER_REG_ID) {
  	  	buffer = ReadReg(eth_base, ETH_UDP_REG7_OFFSET);
    }
  	else if (param == LATENCY_TIMER_REG_ID) {
  	  	buffer = ReadReg(eth_base, ETH_UDP_REG8_OFFSET);
  	}
  	else if (param == SAMPLE_FIFO_TIMER_REG_ID) {
  	  	buffer = ReadReg(eth_base, ETH_UDP_REG9_OFFSET);
  	}
  	else if (param == NET_ERRORS_REG_ID) {
  	  	buffer = ReadReg(eth_base, ETH_UDP_REG10_OFFSET);
  	}
  	else if (param == LOST_PACKETS_REG_ID) {
  	  	buffer = ReadReg(eth_base, ETH_UDP_REG11_OFFSET);
  	}
  	else if (param == TX_PACKETS_REG_ID) {
  	  	buffer = ReadReg(eth_base, ETH_UDP_REG12_OFFSET);
  	}
  	else if (param == RX_PACKETS_REG_ID) {
  	  	buffer = ReadReg(eth_base, ETH_UDP_REG13_OFFSET);
  	}
  	else if (param == PACKET_LENGTH_REG_ID) {
  	  	buffer = ReadReg(eth_base, ETH_UDP_REG14_OFFSET);
  	}
  	else if (param == ETH_CONTROL_REG_ID) {//not read from command but depends on payload command
  		buffer = ReadReg(eth_base, ETH_UDP_REG15_OFFSET);
	}
	else {
		printf("Error requested register does not exist..\n\n");
		return -1;

	}
  return buffer;
}




