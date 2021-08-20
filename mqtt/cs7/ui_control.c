/*
 * =====================================================================================
 *
 *       Filename:  ui_control.c
 *
 *    Description:  User interface functions
 *
 *         Author:  Thanx
 *   Organization:  Cantavi
 *
 * =====================================================================================
 */

#include "include/ui_control.h"
#include "include/udpclient.h"
#include <sys/socket.h>
#include <arpa/inet.h>
#include <netinet/in.h>
#include "include/adau1761_controller.h"

unsigned long total_bytes_send_to_net = 0;
unsigned long total_bytes_send_to_dma = 0;
unsigned long total_bytes_read_from_net=0;
unsigned long axi_to_net_mq_w_error_count =0;
unsigned long axi_to_net_mq_r_error_count =0;
unsigned long net_to_axi_mq_w_error_count =0;
unsigned long net_to_axi_mq_r_error_count =0;
unsigned long net_read_error_count =0;
unsigned long net_send_error_count =0;
unsigned long audio_to_eth_reg_w_error_count=0;
unsigned long axi_to_net_reg_w_error_count=0;

char * print_ip(unsigned int ip, unsigned char * bytes);
char * print_mac(unsigned long ip, unsigned char * bytes);

#define FIFO_SIZE 512
#define AXI_PKT_SIZE FIFO_SIZE/8
#define UDP_PKT_SIZE AXI_PKT_SIZE*4
unsigned volatile pkt_len = 96;
unsigned pkt_len_rl_samples = 96;

char menuBuf[17] = {65, 65, 65, 65, 65, 65, 65, 65, 65, 65, 65, 65, 65, 65, 65, 65, 65};
int cursorPos = 0;
int menuPos = 0;
char menuitem[MENULENGTH][32] = {
	"VOL_AUX_L   \0",
	"VOL_AUX_R   \0",
	"VOL_STREAM_L\0",
	"VOL_STREAM_R\0",
	"AUX_LowP    \0",
	"AUX_BandP   \0",
	"AUX_HighP   \0",
	"STREAM_LowP \0",
	"STREAM_BandP\0",
	"STREAM_HighP\0",
	"Net Counts\0",
	"MAC\0",
	"LOCAL IP\0",
  "DEST IP\0",
  "GTWAY IP\0",
  "MASK IP\0",
  "DEST PORT\0",
  "PKT LEN\0",
  "LOOP TIMER\0",
  "LATENCY TIMER\0",
  "SAMPLE FIFO TIMER\0",
  "NET ERRORS\0",
  "LOST PACKETS\0",
  "TX PACKETS\0",
  "RX PACKETS\0",
  "PACKET LENGTH\0",
  "ETH CONTROL\0",
  "RX BUF LENGTH\0",
  "SEQ ORD\0",
};
int setting[MENULENGTH] = {5, 5, 5, 5, 0, 0, 0, 0, 0, 0};
int settingRange[MENULENGTH] = {10, 10, 10, 10, 1, 1, 1, 1, 1, 1};

int menuUp = 0;
int menuDown = 0;
int menuSelect = 0;
int volSwitch = 0;
int globalVol = 8;
int sockfd;

//void *send_audio_function(void *arg);
void *pmod_function(void *arg);
void *recv_function(void *arg);
void *button_function(void *arg);

void *axi_to_net_mq_reader(void *arg);
void *axi_to_net_mq_writer (void* data);
void *net_to_axi_mq_reader (void *data);
void *net_to_axi_mq_writer (void *data);

void *axi_to_net_mq_reader(void *arg)
{
//	unsigned buf[AXI_PKT_SIZE];
	unsigned buffer[AXI_PKT_SIZE];
	int fd_fifo;
	int IRQEnable = 1;
	int n,z;
//	char buffer[sizeof(unsigned)*AXI_PKT_SIZE];
//	char *hello = "Hello from client";


	while (1)
	{
		n = mq_receive(msgq_axi_to_net_r, (char*)buffer, AXI_PKT_SIZE*4, 0);
		if (n == -1) {
		      perror ("axi_to_net send_audio_to_network : mq_receive(msgq_axi_to_net_r)\n");

		      axi_to_net_mq_r_error_count++;

		      sleep(1);
		}else
		{
			z = udp_client_send(buffer,n);
			if (z != -1) {
	//			sleep(2);
	//			printf("Pkt sent.\n");
				//return 0;
				total_bytes_send_to_net +=z;
			}
			else{
				fprintf (stderr, "Error:: udp_send => sleeping for 1s :: Code=%d\n",z);
				//		    		//exit(1);

				net_send_error_count++;
				sleep(1);
	//	        return 1;
			}
	//			    printf("Hello message sent.\n");
		}
	}

}


/**
 * =======================================================
 * reads audio data from axi and writes it into axi
 * @param   : data  : thread specific data
 * @return  : returns NULL
 * =======================================================
 */



void *axi_to_net_mq_writer (void* data) {
	//  unsigned l_buff = 0;
	//  unsigned r_buff = 0;
	  unsigned buffer[AXI_PKT_SIZE];
	  unsigned i = 0;
	  unsigned tmpL = 0;
	  unsigned tmpR = 0;
//	  while (1) {
//		  //printf ("Waiting for L data\n");
//		  for (i =0; i<AXI_PKT_SIZE; i++){
//				buffer[i] = read_stream (audio_to_eth_base_0, CHANNEL_ID_L);
//
//				i++;
//
//				buffer[i] = read_stream (audio_to_eth_base_0, CHANNEL_ID_R);
//				tmpR = buffer[i];
//
//		  }
//
//
//	    //buffer[0] = l_buff;
//	    if (mq_send(msgq_axi_to_net_w, (char *)buffer, sizeof(buffer) , 0) != 0) {
//	          perror ("axi_to_net audio_axi_reader: mq_send");
//	          axi_to_net_mq_w_error_count++;
//	          sleep(1);
//	        }
//
//
//	  }

	  return NULL;
}

/**
 * =======================================================
 * reads audio data from network and writes it into fifo
 * @param   : data  : thread specific data
 * @return  : returns NULL
 * =======================================================
 */
void *net_to_axi_mq_reader (void *data) {
  unsigned buffer[UDP_PKT_SIZE/4];
  int i=0, n=0;
//  while (1) {
//	  n = mq_receive(msgq_net_to_axi_r, (char*)buffer, UDP_PKT_SIZE, 0);
//    if (n == -1) {
//      perror ("net_to_axi network_reader_stream : mq_receive(msgq_id_r)\n");
//
//      net_to_axi_mq_r_error_count++;
//
//      sleep(1);
//    }else{
//
//		for(i=0; i<n/4;i++){
//			if (write_stream (axi_to_audio_base_0, CHANNEL_ID_L, buffer[i]) != 0) {
//			  fprintf (stderr, "network_stream : write_stream(axi_to_audio_base_1, CHANNEL_ID_L)");
//
//			  audio_to_eth_reg_w_error_count++;
//
//			  sleep(1);
//			}
//
//			i++;
//
//			if (write_stream (axi_to_audio_base_0, CHANNEL_ID_R, buffer[i]) != 0) {
//			  fprintf (stderr, "network_stream : write_stream(axi_to_audio_base_1, CHANNEL_ID_R)");
//
//			  audio_to_eth_reg_w_error_count++;
//			  sleep(1);
//			}
//		}
//    }
//
//  }

  return NULL;
}

/**
 * =======================================================
 * reads audio data from fifo and writes it into axi
 * @param   : data  : thread specific data
 * @return  : returns NULL
 * =======================================================
 */
void *net_to_axi_mq_writer (void *data) {
  char buffer[UDP_PKT_SIZE];
  int n =0;
  while (1) {
	  n = udp_client_recv(buffer, UDP_PKT_SIZE);
    if (n == -1) {
      fprintf (stderr, "Error :: net_to_axi_mq_writer no udp pkt received\n");

	  net_read_error_count++;

      sleep(1);
    }else{
    	total_bytes_read_from_net += n;
		if (mq_send(msgq_net_to_axi_w, (char*)buffer, n, 0) != 0) {
		  perror ("Error :: net_to_axi network_writer_stream : mq_send");

		  net_to_axi_mq_w_error_count++;

		  sleep(1);
		}
    }

  }

  return NULL;
}

/**
 * =======================================================
 * read and parse user input
 * @param   : data  : thread specific data
 * @return  : returns NULL
 * =======================================================
 */
void *ui_input_reader (void *data) {
  char c[8];
  unsigned buf ;
  unsigned long tmp ;
  unsigned int gain = 0;
  unsigned int dest_ip = 0;
  unsigned int source_ip = 0;
  unsigned int gw_ip = 0;
  unsigned int mac = 0;
  unsigned int dest_port = 0;
  unsigned char ip[128]={0};
  unsigned char ipAddress[4];
  unsigned char macAddress[6];

//   oled_clear(zedboard_oled_params_0.base_address);
//   	sprintf(&menuBuf[0], "%s", "Welcome");
//   	oled_print_message(&menuBuf[0], 0, zedboard_oled_params_0.base_address);
//   	sprintf(&menuBuf[0], "%s-%s-", "Cantavi", "S");
//   	oled_print_message(&menuBuf[0], 2, zedboard_oled_params_0.base_address);

  	ui_draw();
  while (1) {
    SET_STATUS("Enter Command\n");
    //ui_draw();
    scanf ("%s",c);
    if(strcmp (c,"vgg") == 0) {
    		SET_STATUS("Entered global gain\n");
            ui_draw();
            scanf ("%d",&gain);
            params.v_global = gain;
            globalVol = gain;

            set_volume (volume_control_base_0, params.vl_lpbk*gain, CHANNEL_ID_L);
            set_volume (volume_control_base_0, params.vr_lpbk*gain, CHANNEL_ID_R);

            set_volume (volume_control_base_1, params.vl_net*gain, CHANNEL_ID_L);
            set_volume (volume_control_base_1, params.vr_net*gain, CHANNEL_ID_R);

            // oled_clear(zedboard_oled_params_0.base_address);
            // sprintf(&menuBuf[0], "%s%3d", "VOL_GLOBAL", gain);
            // oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);

            update_leds();
            ui_vol_draw();
      }else if(strcmp (c,"vll") == 0) {
        SET_STATUS("Entered left gain\n");
        ui_vol_draw();
        scanf ("%d",&gain);
        set_volume (volume_control_base_0, params.v_global*gain, CHANNEL_ID_L);
        params.vl_lpbk = gain;
        setting[1] = params.vl_lpbk;
        // oled_clear(zedboard_oled_params_0.base_address);
		// sprintf(&menuBuf[0], "%s%3d", menuitem[0], setting[1]);
		// oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);

        ui_vol_draw();
      }
      else if(strcmp (c,"vlr") == 0) {
        SET_STATUS("Entered right gain\n");
        ui_vol_draw();
        scanf ("%d",&gain);
        set_volume (volume_control_base_0, params.v_global*gain, CHANNEL_ID_R);
        params.vr_lpbk = gain;
        setting[0] = gain;

        // oled_clear(zedboard_oled_params_0.base_address);
		// sprintf(&menuBuf[0], "%s%3d", menuitem[1], setting[0]);
		// oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);

        ui_vol_draw();
      }
      else if(strcmp (c,"vnl") == 0) {
        SET_STATUS("Entered left gain\n");
        ui_vol_draw();
        scanf ("%d",&gain);
        set_volume (volume_control_base_1, params.v_global*gain, CHANNEL_ID_L);
        params.vl_net = gain;
        setting[3] = gain;

        // oled_clear(zedboard_oled_params_0.base_address);
		// sprintf(&menuBuf[0], "%s%3d", menuitem[2], setting[3]);
		// oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);

        ui_vol_draw();
      }
      else if(strcmp (c,"vnr") == 0) {
        SET_STATUS("Entered right gain\n");
        ui_vol_draw();
        scanf ("%d",&gain);
        set_volume (volume_control_base_1, params.v_global*gain, CHANNEL_ID_R);
        params.vr_net = gain;
        setting[2] = gain;
        // oled_clear(zedboard_oled_params_0.base_address);
		// sprintf(&menuBuf[0], "%s%3d", menuitem[3], setting[2]);
		// oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
        ui_vol_draw();
      }
      else if ( strcmp (c, "lfhe") == 0){
          set_filter_type (filter_control_base_0, FILTER_HIGH_PASS, 1);
          params.filter_h_lpbk = 1;
          setting[6] = 1;
        //   oled_clear(zedboard_oled_params_0.base_address);
        //   sprintf(&menuBuf[0], "%s%3d", menuitem[6], setting[5]);
        //   oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
          ui_draw();
      }
      else if ( strcmp (c, "lfhd") == 0){
          set_filter_type (filter_control_base_0, FILTER_HIGH_PASS, 0);
          params.filter_h_lpbk = 0;
          setting[6] = 0;
        //   oled_clear(zedboard_oled_params_0.base_address);
        //   		sprintf(&menuBuf[0], "%s%3d", menuitem[6], setting[6]);
        //   		oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
          ui_draw();
      }
      else if ( strcmp (c, "lfbe") == 0){
          set_filter_type (filter_control_base_0, FILTER_BAND_PASS, 1);
          params.filter_b_lpbk = 1;
          setting[5] = 1;
        //   oled_clear(zedboard_oled_params_0.base_address);
        //             sprintf(&menuBuf[0], "%s%3d", menuitem[5], setting[5]);
        //             oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
          ui_draw();
      }
      else if ( strcmp (c, "lfbd") == 0){
          set_filter_type (filter_control_base_0, FILTER_BAND_PASS, 0);
          params.filter_b_lpbk = 0;
          setting[5] = 0;
        //   oled_clear(zedboard_oled_params_0.base_address);
        //             sprintf(&menuBuf[0], "%s%3d", menuitem[5], setting[5]);
        //             oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
          ui_draw();
      }
      else if ( strcmp (c, "lfle") == 0){
          set_filter_type (filter_control_base_0, FILTER_LOW_PASS, 1);
          params.filter_l_lpbk = 1;
          setting[4] = 1;
        //   oled_clear(zedboard_oled_params_0.base_address);
        //             sprintf(&menuBuf[0], "%s%3d", menuitem[4], setting[4]);
        //             oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
          ui_draw();
      }
      else if ( strcmp (c, "lfld") == 0){
          set_filter_type (filter_control_base_0, FILTER_LOW_PASS, 0);
          params.filter_l_lpbk = 0;
          setting[4] = 0;
        //   oled_clear(zedboard_oled_params_0.base_address);
        //             sprintf(&menuBuf[0], "%s%3d", menuitem[4], setting[4]);
        //             oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
          ui_draw();
      }
      else if ( strcmp (c, "nfhe") == 0){
          set_filter_type (filter_control_base_1, FILTER_HIGH_PASS, 1);
          params.filter_h_net = 1;
          setting[9] = 1;
        //   oled_clear(zedboard_oled_params_0.base_address);
        //             sprintf(&menuBuf[1], "%s%3d", menuitem[9], setting[9]);
        //             oled_print_message(&menuBuf[1], 1, zedboard_oled_params_0.base_address);
          ui_draw();
      }
      else if ( strcmp (c, "nfhd") == 0){
          set_filter_type (filter_control_base_1, FILTER_HIGH_PASS, 0);
          params.filter_h_net = 0;
          setting[9] = 0;
        //   oled_clear(zedboard_oled_params_0.base_address);
        //             sprintf(&menuBuf[1], "%s%3d", menuitem[9], setting[9]);
        //             oled_print_message(&menuBuf[1], 1, zedboard_oled_params_0.base_address);
          ui_draw();
      }
      else if ( strcmp (c, "nfbe") == 0){
          set_filter_type (filter_control_base_1, FILTER_BAND_PASS, 1);
          params.filter_b_net = 1;
          setting[8] = 1;
        //   oled_clear(zedboard_oled_params_0.base_address);
        //             sprintf(&menuBuf[1], "%s%3d", menuitem[8], setting[8]);
        //             oled_print_message(&menuBuf[1], 1, zedboard_oled_params_0.base_address);
          ui_draw();
      }
      else if ( strcmp (c, "nfbd") == 0){
          set_filter_type (filter_control_base_1, FILTER_BAND_PASS, 0);
          params.filter_b_net = 0;
          setting[8] = 0;
        //   oled_clear(zedboard_oled_params_0.base_address);
        //             sprintf(&menuBuf[1], "%s%3d", menuitem[8], setting[8]);
        //             oled_print_message(&menuBuf[1], 1, zedboard_oled_params_0.base_address);
          ui_draw();
      }
      else if ( strcmp (c, "nfle") == 0){
          set_filter_type (filter_control_base_1, FILTER_LOW_PASS, 1);
          params.filter_l_net = 1;
          setting[7] = 1;
        //   oled_clear(zedboard_oled_params_0.base_address);
        //             sprintf(&menuBuf[1], "%s%3d", menuitem[7], setting[7]);
        //             oled_print_message(&menuBuf[1], 1, zedboard_oled_params_0.base_address);
          ui_draw();
      }
      else if ( strcmp (c, "nfld") == 0){
          set_filter_type (filter_control_base_1, FILTER_LOW_PASS, 0);
          params.filter_l_net = 0;
          setting[7] = 0;
          ui_draw();
      }
      else if ( strcmp (c, "nstats") == 0){

			printf("total_bytes_send_to_dma count is::%d\n",total_bytes_send_to_dma);
			printf("total_bytes_send_to_net count is::%d\n",total_bytes_send_to_net);
			printf("total_bytes_read_from_net count is::%d\n",total_bytes_read_from_net);
			printf("audio_to_eth_reg_w_error count is::%d\n",audio_to_eth_reg_w_error_count);
//			printf("axi_to_net_reg_w_error count is::%d\n",axi_to_net_reg_w_error_count);
			printf("axi_to_net_mq_r_error count is::%d\n",axi_to_net_mq_r_error_count);
			printf("net_to_axi_mq_w_error count is::%d\n",net_to_axi_mq_w_error_count);
			printf("net_to_axi_mq_r_error count is::%d\n",net_to_axi_mq_r_error_count);
			printf("net_read_error count is::%d\n",net_read_error_count);
			printf("net_send_error count is::%d\n",net_send_error_count);
			// oled_clear(zedboard_oled_params_0.base_address);
			// sprintf(&menuBuf[0], "%s::%3d", menuitem[10], total_bytes_send_to_net);
			// oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
			ui_draw();

      }
      else if ( strcmp (c, "astatus") == 0){
    	  int buffer = read_fifo_status (audio_to_eth_base_0, ADC_FIFO_ID);
    	  if(buffer&0x04){
    	      	//LeftFIFOEmpty
    		  printf("ADC left audio fifo Empty :: val::0x%02X\n",buffer);
    	  }
    	  if(buffer&0x08){
    	      	//RightFIFOEmpty
    		  printf("ADC right audio fifo Empty :: val::0X%02X\n",buffer);
    	      }
    	  if(buffer&0x10){
    	      	//LeftFIFOFull
    		  printf("ADC left audio fifo Full :: val::0x%02X\n",buffer);
    	      }
    	  if(buffer&0x20){
    	      	//RightFIFOFull
    		  printf("ADC right audio fifo Full :: val::0x%02X\n",buffer);
    	      }
    	  if(buffer&0x3C == 0){
    		  printf("All ADC audio fifo Flags clear :: val::0X%02X\n",buffer);
    	  }
    	  printf("All ADC audio fifo Flags:: val::0X%02X\n",buffer);
    	  ui_draw();
      }
      else if ( strcmp (c, "dstatus") == 0){
    	  int buffer = read_fifo_status (eth_to_audio_base_0, DAC_FIFO_ID);
    	  if(buffer&0x04){
    	      	      	//LeftFIFOEmpty
			  printf("DAC left audio fifo Empty :: val::0x%02X\n",buffer);
		  }
		  if(buffer&0x08){
				//RightFIFOEmpty
			  printf("DAC right audio fifo Empty :: val::0x%02X\n",buffer);
			  }
		  if(buffer&0x10){
				//LeftFIFOFull
			  printf("DAC left audio fifo Full :: val::0x%02X\n",buffer);
			  }
		  if(buffer&0x20){
				//RightFIFOFull
			  printf("DAC right audio fifo Full :: val::0x%02X\n",buffer);
			  }
		  if(buffer&0x3C == 0){
			  printf("All DAC audio fifo Flags clear :: val::0x%02X\n",buffer);
		  }
		  printf("All DAC audio fifo Flags :: val::0x%02X\n",buffer);
		  ui_draw();
      }
      else if ( strcmp (c, "areset") == 0){
    	  reset_adc_fifos(audio_to_eth_base_0);//LSB is left, MSB is Right
    	  printf("ADC audio FIFOs Reset\n");
    	//   oled_clear(zedboard_oled_params_0.base_address);
    	//   sprintf(&menuBuf[0], "%s", "ADC FIFOs Rst");
    	//   oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
    	  ui_draw();
      }

      else if ( strcmp (c, "dreset") == 0){
    	  time_sync_tx_off(full_udp_stack_ip_base_0);
    	  time_sync_off(time_sync_base_0);
    	  set_sync_rst(time_sync_base_0);
    	  sleep(1);
			time_sync_en(time_sync_base_0);
			usleep(100);
			sleep(2);
			time_sync_tx_en(full_udp_stack_ip_base_0);
		  printf("Enable time sync...\n");

    	  reset_dac_fifos(eth_to_audio_base_0);//LSB is left, MSB is Right
    	  printf("DAC audio FIFOs Reset\n>");
    	//   oled_clear(zedboard_oled_params_0.base_address);
    	//   sprintf(&menuBuf[0], "%s", "DAC FIFOs Rst");
    	//   oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
    	  ui_draw();
      }

      else if ( strcmp (c, "preset") == 0){
    	  //reset_pkt_seq(eth_to_audio_base_0, DAC_FIFO_ ID,0x03);//LSB is left, MSB is Right
    	  reset_pkt_seq(eth_packet_sequencer_base_0);

          	  printf("PLC Pkt seq Reset\n>");
          	//   oled_clear(zedboard_oled_params_0.base_address);
          	//   sprintf(&menuBuf[0], "%s", "PKT SEQ Rst");
          	//   oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
          	  ui_draw();
            }

      else if ( strcmp (c, "pbtreset") == 0){
          	  //reset_pkt_seq(eth_to_audio_base_0, DAC_FIFO_ ID,0x03);//LSB is left, MSB is Right
    	  	  //reset_pkt_time_enf(packet_time_enforcer_base_0);


			  printf("Pkt timing unit reset\n>");
			//   oled_clear(zedboard_oled_params_0.base_address);
			//   sprintf(&menuBuf[0], "%s", "PKT TU Rst");
			//   oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
			  ui_draw();
		  }
      else if ( strcmp (c, "pbtrreset") == 0){
                	  //reset_pkt_seq(eth_to_audio_base_0, DAC_FIFO_ ID,0x03);//LSB is left, MSB is Right
    	  	  	  reset_pkt_time_enf_rx(packet_time_enforcer_base_0);

      			  printf("Pkt timing unit reset rx\n>");
      			//   oled_clear(zedboard_oled_params_0.base_address);
      			//   sprintf(&menuBuf[0], "%s", "PKT TU RX Rst");
      			//   oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
      			  ui_draw();
      		  }
      else if ( strcmp (c, "pbtoff") == 0){
                	  //reset_pkt_seq(eth_to_audio_base_0, DAC_FIFO_ ID,0x03);//LSB is left, MSB is Right
    	  	  	  pkt_time_enf_disable(packet_time_enforcer_base_0);

      			  printf("Pkt timing unit disable\n>");
      			//   oled_clear(zedboard_oled_params_0.base_address);
      			//   sprintf(&menuBuf[0], "%s", "PKT TU OFF");
      			//   oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
      			  ui_draw();
      		  }

      else if ( strcmp (c, "pbton") == 0){
                      	  //reset_pkt_seq(eth_to_audio_base_0, DAC_FIFO_ ID,0x03);//LSB is left, MSB is Right
          	  	  	  pkt_time_enf_enable(packet_time_enforcer_base_0);

            			  printf("Pkt timing unit enable\n>");
            			//   oled_clear(zedboard_oled_params_0.base_address);
            			//   sprintf(&menuBuf[0], "%s", "PKT TU ON");
            			//   oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
            			  ui_draw();
            		  }


      else if ( strcmp (c, "pseqoff") == 0){
          	  //reset_pkt_seq(eth_to_audio_base_0, DAC_FIFO_ ID,0x03);//LSB is left, MSB is Right
          	  disble_pkt_seq(eth_packet_sequencer_base_0);

			  printf("Pkt seq Disabled\n>");
			//   oled_clear(zedboard_oled_params_0.base_address);
			//   sprintf(&menuBuf[0], "%s", "PKT SEQ OFF");
			//   oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
			  ui_draw();
		  }

      else if ( strcmp (c, "pseqon") == 0){
                	  //reset_pkt_seq(eth_to_audio_base_0, DAC_FIFO_ ID,0x03);//LSB is left, MSB is Right
                	  enable_pkt_seq(eth_packet_sequencer_base_0);

      			  printf("Pkt seq Enabled\n>");
      			//   oled_clear(zedboard_oled_params_0.base_address);
      			//   sprintf(&menuBuf[0], "%s", "PKT SEQ ON");
      			//   oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
      			  ui_draw();
      		  }


      else if ( strcmp (c, "creset") == 0){
                	  unsigned buf = read_eth_param (full_udp_stack_ip_base_0, ETH_CONTROL_REG_ID);
                	      	  write_eth_param(full_udp_stack_ip_base_0, ETH_CONTROL_ID, buf|0x02);
                	      	 sleep(1);
                	      	write_eth_param(full_udp_stack_ip_base_0, ETH_CONTROL_ID, buf&0xFFFFFFFD);
                	      	printf("Stream core reset status was:0x%X\n>",buf);
                	      	// oled_clear(zedboard_oled_params_0.base_address);
                	      	// sprintf(&menuBuf[0], "%s", "Stream Core Rst");
                	      	// oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
                	      	ui_draw();
                  }
            else if ( strcmp (c, "freset") == 0){

          	  unsigned buf = read_eth_param (full_udp_stack_ip_base_0, ETH_CONTROL_REG_ID);
          	     write_eth_param(full_udp_stack_ip_base_0, ETH_CONTROL_ID, buf|2);
          	     sleep(1);
      			write_eth_param(full_udp_stack_ip_base_0, ETH_CONTROL_ID, buf&0xFFFFFFFD);
      			printf("Stream core reset status was:0x%X\n>",buf);

      			reset_pkt_seq(eth_packet_sequencer_base_0);

				printf("PLC Pkt seq Reset\n>");
				///reset_pkt_time_enf(packet_time_enforcer_base_0);


				//printf("Pkt timing unit reset\n>");
               	  reset_adc_fifos(audio_to_eth_base_0);//LSB is left, MSB is Right
               	  printf("ADC audio FIFOs Reset\n");
               	  reset_dac_fifos(eth_to_audio_base_0);//LSB is left, MSB is Right
               	  printf("DAC audio FIFOs Reset\n>");


               	//  oled_clear(zedboard_oled_params_0.base_address);
      			// sprintf(&menuBuf[0], "%s", "Full Stream Rst");
      			// oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
      			ui_draw();
                 }

            else if ( strcmp (c, "rxrsync") == 0){

            	unsigned buf = read_eth_param (full_udp_stack_ip_base_0, ETH_CONTROL_REG_ID);
				 write_eth_param(full_udp_stack_ip_base_0, ETH_CONTROL_ID, buf|2);
				 sleep(1);
				write_eth_param(full_udp_stack_ip_base_0, ETH_CONTROL_ID, buf&0xFFFFFFFD);
				printf("Stream core reset status was:0x%X\n>",buf);

				reset_pkt_seq(eth_packet_sequencer_base_0);

				printf("Pkt seq Reset\n>");
				//reset_pkt_time_enf(packet_time_enforcer_base_0);


				//printf("Pkt timing unit reset\n>");
				  reset_adc_fifos(audio_to_eth_base_0);//LSB is left, MSB is Right
				  printf("ADC audio FIFOs Reset\n");
				  reset_dac_fifos(eth_to_audio_base_0);//LSB is left, MSB is Right
				  printf("DAC audio FIFOs Reset\n>");
                     	//  oled_clear(zedboard_oled_params_0.base_address);
            			// sprintf(&menuBuf[0], "%s", "RX re sync");
            			// oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
            			ui_draw();
                       }

      else if ( strcmp (c, "strteston") == 0){
			  set_test_mode_on(audio_to_eth_base_0);//wValue(7);
			  printf("Put Audio to Ethernet IP in test mode\n");
			//   oled_clear(zedboard_oled_params_0.base_address);
			//   sprintf(&menuBuf[0], "%s", "Test Mode: ON");
			// oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
			ui_draw();
		  }
      else if ( strcmp (c, "strtestm1") == 0){
      			  set_test_mode(audio_to_eth_base_0, 0);//wValue(10 down to 9);
      			  printf("Put Audio to Ethernet IP in test ramp mode\n");
      			oled_clear(zedboard_oled_params_0.base_address);
      			sprintf(&menuBuf[0], "%s", "LR Ramp");
      		    oled_print_message(&menuBuf[0], 2, zedboard_oled_params_0.base_address);
      			ui_draw();
      		  }
      else if ( strcmp (c, "strtestm2") == 0){
			  set_test_mode(audio_to_eth_base_0, 1);//wValue(10 down to 9);
			  printf("Put Audio to Ethernet IP in test fixed LR=0x55 mode\n");
			  oled_clear(zedboard_oled_params_0.base_address);
			  sprintf(&menuBuf[0], "%s", "LR 0x555555");
			  oled_print_message(&menuBuf[0], 2, zedboard_oled_params_0.base_address);
			  ui_draw();
			ui_draw();
		  }
      else if ( strcmp (c, "strtestm3") == 0){
			  set_test_mode(audio_to_eth_base_0, 2);//wValue(10 down to 9);
			  printf("Put Audio to Ethernet IP in test fixed L=0x00 R=0xFF mode\n");
			//   oled_clear(zedboard_oled_params_0.base_address);
			//   sprintf(&menuBuf[0], "%s", "L=0x00 R=0xFF");
			//  oled_print_message(&menuBuf[0], 2, zedboard_oled_params_0.base_address);
			 ui_draw();
			ui_draw();
		  }
      else if ( strcmp (c, "strtestm4") == 0){
			  set_test_mode(audio_to_eth_base_0, 3);//wValue(10 down to 9);
			  printf("Put Audio to Ethernet IP in test bit flip mode\n");
			//   oled_clear(zedboard_oled_params_0.base_address);
			//   sprintf(&menuBuf[0], "%s", "LR FLIP");
			//   oled_print_message(&menuBuf[0], 2, zedboard_oled_params_0.base_address);
			  ui_draw();
		  }
      else if ( strcmp (c, "strtestm5") == 0){
     			  set_test_mode(audio_to_eth_base_0, 4);//wValue(10 down to 9);
     			  printf("Put Audio to Ethernet IP in test sine wave mode\n");
     			//   oled_clear(zedboard_oled_params_0.base_address);
     			//   sprintf(&menuBuf[0], "%s", "LR SQW");
     			//   oled_print_message(&menuBuf[0], 2, zedboard_oled_params_0.base_address);
     			  ui_draw();
     		  }
      else if ( strcmp (c, "strtestoff") == 0){
			  set_test_mode_off(audio_to_eth_base_0);//wValue(7);
			  printf("Put Audio to Ethernet IP in test mode\n");
			//   oled_clear(zedboard_oled_params_0.base_address);
			//   sprintf(&menuBuf[0], "%s", "Test Mode: OFF");
			//   oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);

			ui_draw();
			}

    //------------------------------------------------------------------------------------------------------
      else if ( strcmp (c, "sethdrstrip") == 0){//done
    	  set_rx_hdr_strip(eth_to_audio_base_0);//wValue(7);
		  printf("Ethernet to Audio IP header strip on\n");
		//   oled_clear(zedboard_oled_params_0.base_address);
		//   sprintf(&menuBuf[0], "%s", "HDR Strip: ON");
		//   oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
		  ui_draw();
	  }
      else if ( strcmp (c, "clearhdrstrip") == 0){//done
          	clear_rx_hdr_strip(eth_to_audio_base_0);//wValue(7);
			printf("Ethernet to Audio IP header strip off\n");
			// oled_clear(zedboard_oled_params_0.base_address);
			// sprintf(&menuBuf[0], "%s", "HDR Strip: OFF");
			// oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
			ui_draw();
		  }


      else if ( strcmp (c, "strxteston") == 0){
      			  set_xtest_mode_on(eth_to_audio_base_0);//wValue(7);
      			  printf("Put Ethernet to Audio IP in test mode\n");
      			//   oled_clear(zedboard_oled_params_0.base_address);
      			//   sprintf(&menuBuf[0], "%s", "xTest Mode: ON");
      			// oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
      			ui_draw();
      		  }
            else if ( strcmp (c, "strxtestm1") == 0){
            			  set_xtest_mode(eth_to_audio_base_0, 0);//wValue(10 down to 9);
            			  printf("Put Ethernet to Audio IP in test ramp mode\n");
            			oled_clear(zedboard_oled_params_0.base_address);
            			sprintf(&menuBuf[0], "%s", "LR Ramp");
            		    oled_print_message(&menuBuf[0], 2, zedboard_oled_params_0.base_address);
            			ui_draw();
            		  }
            else if ( strcmp (c, "strxtestm2") == 0){
      			  set_xtest_mode(eth_to_audio_base_0, 1);//wValue(10 down to 9);
      			  printf("Put Ethernet to Audio IP in test fixed LR=0x55 mode\n");
      			//   oled_clear(zedboard_oled_params_0.base_address);
      			//   sprintf(&menuBuf[0], "%s", "LR 0x555555");
      			//   oled_print_message(&menuBuf[0], 2, zedboard_oled_params_0.base_address);
      			  ui_draw();
      			ui_draw();
      		  }
            else if ( strcmp (c, "strxtestm3") == 0){
      			  set_xtest_mode(eth_to_audio_base_0, 2);//wValue(10 down to 9);
      			  printf("Put Ethernet to Audio IP in test fixed L=0x00 R=0xFF mode\n");
      			//   oled_clear(zedboard_oled_params_0.base_address);
      			//   sprintf(&menuBuf[0], "%s", "L=0x00 R=0xFF");
      			//  oled_print_message(&menuBuf[0], 2, zedboard_oled_params_0.base_address);
      			//  ui_draw();
      			ui_draw();
      		  }
            else if ( strcmp (c, "strxtestm4") == 0){
      			  set_xtest_mode(eth_to_audio_base_0, 3);//wValue(10 down to 9);
      			  printf("Put Ethernet to Audio IP in test bit flip mode\n");
      			//   oled_clear(zedboard_oled_params_0.base_address);
      			//   sprintf(&menuBuf[0], "%s", "LR FLIP");
      			//   oled_print_message(&menuBuf[0], 2, zedboard_oled_params_0.base_address);
      			  ui_draw();
      		  }
            else if ( strcmp (c, "strxtestm5") == 0){
           			  set_xtest_mode(eth_to_audio_base_0, 4);//wValue(10 down to 9);
           			  printf("Put Ethernet to Audio IP in test square wave mode\n");
           			//   oled_clear(zedboard_oled_params_0.base_address);
           			//   sprintf(&menuBuf[0], "%s", "LR SQR");
           			//   oled_print_message(&menuBuf[0], 2, zedboard_oled_params_0.base_address);
           			  ui_draw();
           		  }
            else if ( strcmp (c, "strxtestoff") == 0){
      			  set_xtest_mode_off(eth_to_audio_base_0);//wValue(7);
      			  printf("Put Ethernet to Audio IP in test mode\n");
      			//   oled_clear(zedboard_oled_params_0.base_address);
      			//   sprintf(&menuBuf[0], "%s", "xTest Mode: OFF");
      			//   oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);

      			ui_draw();
      			}
            else if ( strcmp (c, "sethdrlen") == 0){
            	scanf ("%d",&tmp);
			  set_tx_hdr_length(eth_to_audio_base_0, tmp);//wValue(7);
			  printf("Set hdr length...\n");
			//   oled_clear(zedboard_oled_params_0.base_address);
			//   sprintf(&menuBuf[0], "%s%d", "HDR LEN:",tmp);
			//   oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);

			ui_draw();
			}
    //------------------------------------------------------------------------------------------------------
            else if ( strcmp (c, "setsyncen") == 0){

            	time_sync_en(time_sync_base_0);
            	usleep(100);
            	time_sync_tx_en(full_udp_stack_ip_base_0);
			  printf("Enable time sync...\n");
			//   oled_clear(zedboard_oled_params_0.base_address);
			//   sprintf(&menuBuf[0], "%s", "SYNC: ON");
			//   oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
			ui_draw();
		  }

            else if ( strcmp (c, "setsyncoff") == 0){

				time_sync_off(time_sync_base_0);
				//time_sync_tx_off(full_udp_stack_ip_base_0);
			  printf("Disable time sync...\n");
			//   oled_clear(zedboard_oled_params_0.base_address);
			//   sprintf(&menuBuf[0], "%s", "SYNC: OFF");
			//   oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
			ui_draw();
		  }
            else if ( strcmp (c, "setsyncrst") == 0){

                                    	set_sync_rst(time_sync_base_0);

                        			  printf("Reset time sync...\n");
                        			//   oled_clear(zedboard_oled_params_0.base_address);
                        			//   sprintf(&menuBuf[0], "%s", "SYNC: RST");
                        			//   oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
                        			ui_draw();
                        		  }


            else if ( strcmp (c, "setsyncdly") == 0){

            	scanf ("%d",&tmp);
                        	set_sync_pkt_dly(time_sync_base_0,tmp);

            			  printf("Set time sync response wait delay...\n");
            			//   oled_clear(zedboard_oled_params_0.base_address);
            			//   sprintf(&menuBuf[0], "%s", "SYNC: DLY");
            			//   oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
            			ui_draw();
            		  }

	else if ( strcmp (c, "setsyncon") == 0){

					initiate_sync(time_sync_base_0);
				  set_sync_on(audio_to_eth_base_0);
				  printf("Enable time sync...\n");
				//   oled_clear(zedboard_oled_params_0.base_address);
				//   sprintf(&menuBuf[0], "%s", "SYNC: ON");
				//   oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
				ui_draw();
			  }

	else if ( strcmp (c, "setsyncoff") == 0){

				  set_sync_off(audio_to_eth_base_0);
				  printf("Disable time sync...\n");
				//   oled_clear(zedboard_oled_params_0.base_address);
				//   sprintf(&menuBuf[0], "%s", "SYNC: OFF");
				//   oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
				ui_draw();
			  }
	else if ( strcmp (c, "tgsync") == 0){

						set_sync_off(audio_to_eth_base_0);
					sleep(1);
					  set_sync_on(audio_to_eth_base_0);
					  printf("Toggle time sync...\n");
					//   oled_clear(zedboard_oled_params_0.base_address);
					//   sprintf(&menuBuf[0], "%s", "SYNC: ON");
					//   oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
					ui_draw();
				  }
      else if ( strcmp (c, "strpktcon") == 0){

			  set_pcnt_mode_on(audio_to_eth_base_0);
			  printf("Enable UDP packet count\n");
			//   oled_clear(zedboard_oled_params_0.base_address);
			//   sprintf(&menuBuf[0], "%s", "Pkt Count: ON");
			//   oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
			ui_draw();
		  }
      else if ( strcmp (c, "strpktcoff") == 0){

			  set_pcnt_mode_off(audio_to_eth_base_0);
			  printf("Disable UDP packet count\n");
			//   oled_clear(zedboard_oled_params_0.base_address);
			//   sprintf(&menuBuf[0], "%s", "Pkt Count: OFF");
			//   oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
			ui_draw();
			}

      else if ( strcmp (c, "strsndon") == 0){

      			  set_stream_send_on(audio_to_eth_base_0);
      			  printf("Enable UDP payload generation\n");
      			// oled_clear(zedboard_oled_params_0.base_address);
      			// sprintf(&menuBuf[0], "%s", "Pkt Send: ON");
      			// oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
      			ui_draw();
      		  }
            else if ( strcmp (c, "strsndoff") == 0){

      			  set_stream_send_off(audio_to_eth_base_0);
      			  printf("Disable UDP payload generation \n");
      			// oled_clear(zedboard_oled_params_0.base_address);
      			// sprintf(&menuBuf[0], "%s", "Pkt Send: OFF");
      			// oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
      			ui_draw();
      			}

      else if ( strcmp (c, "strselarm") == 0){
          	  reset_dac_fifos(eth_to_audio_base_0);//LSB is left, MSB is Right
          	  printf("Play arm stream\n>");
          	// oled_clear(zedboard_oled_params_0.base_address);
          	// sprintf(&menuBuf[0], "%s", "Pkt Send: ARM");
          	// oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
          	ui_draw();
            }
      else if ( strcmp (c, "strseleth") == 0){
               	  reset_dac_fifos(eth_to_audio_base_0);//LSB is left, MSB is Right
               	  printf("Play Eth stream\n>");
               	// oled_clear(zedboard_oled_params_0.base_address);
               	// sprintf(&menuBuf[0], "%s", "Pkt Send: ETH");
               	// oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
               	ui_draw();
                 }

      else if ( strcmp (c, "strstart") == 0){
//    	  set_sync_off(audio_to_eth_base_0);
    	  unsigned buf = read_eth_param (full_udp_stack_ip_base_0, ETH_CONTROL_REG_ID);

		write_eth_param(full_udp_stack_ip_base_0, ETH_CONTROL_ID, buf|0x02);
		buf = read_eth_param (full_udp_stack_ip_base_0, ETH_CONTROL_REG_ID);

		sleep(2);
		reset_adc_fifos(audio_to_eth_base_0);//LSB is left, MSB is Right
		printf("Stream tx started status was:0x%X\n>",buf);
//		sleep(1);
//		initiate_sync(time_sync_base_0);
//		set_sync_on(audio_to_eth_base_0);
		printf("Enable time sync...\n");
		// oled_clear(zedboard_oled_params_0.base_address);
		// sprintf(&menuBuf[0], "%s", "UDP Send: ON");
		// oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
		ui_draw();
      }

      else if ( strcmp (c, "strrxon") == 0){
          	  unsigned buf = read_eth_param (full_udp_stack_ip_base_0, ETH_CONTROL_REG_ID);
          	      	  write_eth_param(full_udp_stack_ip_base_0, ETH_CONTROL_ID, buf|0x04);
          	      	printf("Stream rx started status was:0x%X\n>",buf);
          	      	// oled_clear(zedboard_oled_params_0.base_address);
          	      	// sprintf(&menuBuf[0], "%s", "UDP Rx: ON");
          	      	// oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
          	      	ui_draw();
            }


      else if ( strcmp (c, "strstop") == 0){
    	  unsigned buf = read_eth_param (full_udp_stack_ip_base_0, ETH_CONTROL_REG_ID);
    	  set_sync_off(audio_to_eth_base_0);
    	  				  printf("Disable time sync...\n");
    	  write_eth_param(full_udp_stack_ip_base_0, ETH_CONTROL_ID, buf&0xFFFFFFFD);
    	//   oled_clear(zedboard_oled_params_0.base_address);
    	//   sprintf(&menuBuf[0], "%s", "UDP Send: OFF");
    	//   oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
    	  buf = read_eth_param (full_udp_stack_ip_base_0, ETH_CONTROL_REG_ID);
    	  printf("Stream tx stopped status was:0x%X\n>",buf);
            }
      else if ( strcmp (c, "strrxstop") == 0){
          	  unsigned buf = read_eth_param (full_udp_stack_ip_base_0, ETH_CONTROL_REG_ID);
          	  write_eth_param(full_udp_stack_ip_base_0, ETH_CONTROL_ID, buf&0xFFFFFFFB);
          	//   oled_clear(zedboard_oled_params_0.base_address);
          	//   sprintf(&menuBuf[0], "%s", "UDP Receive: OFF");
          	//   oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
          	  printf("Stream rx stopped status was:0x%X\n>",buf);
                  }
      else if(strcmp (c,"strmac") == 0) {
		  //SET_STATUS("Enter MAC Address (xx.xx.xx.xx.xx.xx format)");
    	  scanf("%s",ip);
		  extractMacAddress(ip,&macAddress[0]);
		  printf("\nMac Address: %02x. %02x. %02x. %02x. %02x. %02x\n",macAddress[0],macAddress[1],macAddress[2],macAddress[3],macAddress[4],macAddress[5]);
		  //write_eth_mac(full_udp_stack_ip_base_0, MAC_ID, ((macAddress[0]<<40)|(macAddress[1]<<32)|macAddress[2]<<24)|(macAddress[3]<<16)|(macAddress[4]<<8)|(macAddress[5]));
		  write_eth_mac(full_udp_stack_ip_base_0, MAC_ID, &macAddress[0]);
		//   oled_clear(zedboard_oled_params_0.base_address);
		// sprintf(&menuBuf[0], "%s", menuitem[11]);
		// oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
		// sprintf(&menuBuf[0], "%02x%02x%02x%02x%02x%02x",macAddress[0],macAddress[1],macAddress[2],macAddress[3],macAddress[4],macAddress[5]);
		// oled_print_message(&menuBuf[0], 2, zedboard_oled_params_0.base_address);

		  ui_draw();
	}
      else if(strcmp (c,"strsip") == 0) {
      		  //SET_STATUS("Enter Dest IP Address (xxx.xxx.xxx.xxx format): ");
          	  scanf("%s",ip);
      		  extractIpAddress(ip,&ipAddress[0]);
      		  printf("\nIp Address: %03d. %03d. %03d. %03d\n",ipAddress[0],ipAddress[1],ipAddress[2],ipAddress[3]);
      		  write_eth_param(full_udp_stack_ip_base_0, SYNC_IP_ID, (ipAddress[0]<<24)|(ipAddress[1]<<16)|(ipAddress[2]<<8)|(ipAddress[3]));

      		//   oled_clear(zedboard_oled_params_0.base_address);
      		//   sprintf(&menuBuf[0], "%s", menuitem[13]);
      		//   		oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
      		//   		sprintf(&menuBuf[0], "%s", ip);
      		//   		oled_print_message(&menuBuf[0], 2, zedboard_oled_params_0.base_address);

      		  ui_draw();
      	}

      else if(strcmp (c,"strdip") == 0) {
		  //SET_STATUS("Enter Dest IP Address (xxx.xxx.xxx.xxx format): ");
    	  scanf("%s",ip);
		  extractIpAddress(ip,&ipAddress[0]);
		  printf("\nIp Address: %03d. %03d. %03d. %03d\n",ipAddress[0],ipAddress[1],ipAddress[2],ipAddress[3]);
		  write_eth_param(full_udp_stack_ip_base_0, DEST_IP_ID, (ipAddress[0]<<24)|(ipAddress[1]<<16)|(ipAddress[2]<<8)|(ipAddress[3]));

		//   oled_clear(zedboard_oled_params_0.base_address);
		//   sprintf(&menuBuf[0], "%s", menuitem[13]);
		//   		oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
		//   		sprintf(&menuBuf[0], "%s", ip);
		//   		oled_print_message(&menuBuf[0], 2, zedboard_oled_params_0.base_address);

		  ui_draw();
	}
      else if(strcmp (c,"strlip") == 0) {
      		  //SET_STATUS("Enter Local IP Address (xxx.xxx.xxx.xxx format): ");
    	  scanf("%s",ip);
      		  extractIpAddress(ip,&ipAddress[0]);
      		  printf("\nIp Address: %03d. %03d. %03d. %03d\n",ipAddress[0],ipAddress[1],ipAddress[2],ipAddress[3]);
      		write_eth_param(full_udp_stack_ip_base_0, LOCAL_IP_ID, (ipAddress[0]<<24)|(ipAddress[1]<<16)|(ipAddress[2]<<8)|(ipAddress[3]));

      		//   oled_clear(zedboard_oled_params_0.base_address);
      		// sprintf(&menuBuf[0], "%s", menuitem[12]);
      		// 		oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
      		// 		sprintf(&menuBuf[0], "%s", ip);
      		// 		oled_print_message(&menuBuf[0], 2, zedboard_oled_params_0.base_address);

      		  ui_draw();
      	}

      else if(strcmp (c,"strsip") == 0) {
      		  //SET_STATUS("Enter Dest IP Address (xxx.xxx.xxx.xxx format): ");
          	  scanf("%s",ip);
      		  extractIpAddress(ip,&ipAddress[0]);
      		  printf("\nIp Address: %03d. %03d. %03d. %03d\n",ipAddress[0],ipAddress[1],ipAddress[2],ipAddress[3]);
      		  write_eth_param(full_udp_stack_ip_base_0, SYNC_IP_ID, (ipAddress[0]<<24)|(ipAddress[1]<<16)|(ipAddress[2]<<8)|(ipAddress[3]));

      		//   oled_clear(zedboard_oled_params_0.base_address);
      		//   sprintf(&menuBuf[0], "%s", "SYNC_IP:");
      		//   		oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
      		//   		sprintf(&menuBuf[0], "%s", ip);
      		//   		oled_print_message(&menuBuf[0], 2, zedboard_oled_params_0.base_address);

      		  ui_draw();
      	}

      else if(strcmp (c,"strgip") == 0) {
           		  //SET_STATUS("Enter Gateway IP Address (xxx.xxx.xxx.xxx format): ");
           		  scanf("%s",ip);
           		  extractIpAddress(ip,&ipAddress[0]);
           		  printf("\nIp Address: %03d. %03d. %03d. %03d\n",ipAddress[0],ipAddress[1],ipAddress[2],ipAddress[3]);
           		write_eth_param(full_udp_stack_ip_base_0, GTWAY_IP_ID, (ipAddress[0]<<24)|(ipAddress[1]<<16)|(ipAddress[2]<<8)|(ipAddress[3]));

           		//   oled_clear(zedboard_oled_params_0.base_address);
           		// sprintf(&menuBuf[0], "%s", menuitem[14]);
           		// 		oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
           		// 		sprintf(&menuBuf[0], "%s", ip);
           		// 		oled_print_message(&menuBuf[0], 2, zedboard_oled_params_0.base_address);

           		  ui_draw();
           	}
      else if(strcmp (c,"strmask") == 0) {
                 		  //SET_STATUS("Enter Netmask IP Address (xxx.xxx.xxx.xxx format): ");
    	  scanf("%s",ip);
		  extractIpAddress(ip,&ipAddress[0]);
		  printf("\nIp Address: %03d. %03d. %03d. %03d\n",ipAddress[0],ipAddress[1],ipAddress[2],ipAddress[3]);
		  write_eth_param(full_udp_stack_ip_base_0, MASK_IP_ID, (ipAddress[0]<<24)|(ipAddress[1]<<16)|(ipAddress[2]<<8)|(ipAddress[3]));

		//   oled_clear(zedboard_oled_params_0.base_address);
		//   sprintf(&menuBuf[0], "%s", menuitem[15]);
		//   		oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
		//   		sprintf(&menuBuf[0], "%s", ip);
		//   		oled_print_message(&menuBuf[0], 2, zedboard_oled_params_0.base_address);
		  ui_draw();
     }

      else if(strcmp (c,"strport") == 0) {
              //SET_STATUS("Enter port:\n");

              scanf ("%d",&dest_port);
              write_eth_param(full_udp_stack_ip_base_0, DEST_PORT_ID, dest_port);

              oled_clear(zedboard_oled_params_0.base_address);
              sprintf(&menuBuf[0], "%s", menuitem[16]);
              		  		oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
              		  		sprintf(&menuBuf[0], "%d", dest_port);
              		  		oled_print_message(&menuBuf[0], 2, zedboard_oled_params_0.base_address);
              ui_draw();
            }
      else if(strcmp (c,"strsport") == 0) {
                    //SET_STATUS("Enter port:\n");

                    scanf ("%d",&dest_port);
                    write_eth_param(full_udp_stack_ip_base_0, SYNC_PORT_ID, dest_port);

                    // oled_clear(zedboard_oled_params_0.base_address);
                    // sprintf(&menuBuf[0], "%s", "SYNC PORT");
                    // 		  		oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
                    // 		  		sprintf(&menuBuf[0], "%d", dest_port);
                    // 		  		oled_print_message(&menuBuf[0], 2, zedboard_oled_params_0.base_address);
                    ui_draw();
                  }
      else if(strcmp (c,"strnsp") == 0) {//cant set speed
      		  //SET_STATUS("Enter Net speed 0=10M, 1=100M, 2=1G: ");
      		  scanf ("%d",&tmp);

      		buf = read_eth_param (full_udp_stack_ip_base_0, ETH_CONTROL_REG_ID);
      		if(tmp == 0){
      		   write_eth_param(full_udp_stack_ip_base_0, ETH_CONTROL_ID, buf&0xFFFFFFF9);
      		}else if(tmp == 1){
       		   write_eth_param(full_udp_stack_ip_base_0, ETH_CONTROL_ID, buf&0xFFFFFFFB);
       		}else if(tmp == 2){
       		   write_eth_param(full_udp_stack_ip_base_0, ETH_CONTROL_ID, buf&0xFFFFFFFD);
       		}


      		//   oled_clear(zedboard_oled_params_0.base_address);
      		// sprintf(&menuBuf[0], "%s%s", menuitem[12], ip);
      		// oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);

      		  ui_draw();
      	}
      else if(strcmp (c,"strplen") == 0) {
                    //SET_STATUS("Enter payload length in 24 bit samples:\n");
                    //UDP header is 8 bytes
                    scanf ("%d",&tmp);
                    int c = (tmp-12)%6;
                    pkt_len = tmp-12;
                    pkt_len_rl_samples = pkt_len/6;
                    if (c > 0){
                    	printf("Warning:: Bad packet length:: The packet length formula is:: 6xN + 2 \n Choose N wisely!!!\n");
                    }
                    //write_eth_param(full_udp_stack_ip_base_0, PKT_LEN_ID, ((tmp*3) +8));//+8 done in fpga
                    //write_audio_to_eth_param(audio_to_eth_base_0, PAYLEN_REG_ID, tmp*3);
                    set_stream_plen(audio_to_eth_base_0,tmp);
                    // oled_clear(zedboard_oled_params_0.base_address);
            		// sprintf(&menuBuf[0], "Pkt Length:%5d", (tmp));
            		// oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
                    ui_draw();
                  }

      else if(strcmp (c,"strporder") == 0) {

                          scanf ("%d",&tmp);

                          set_stream_porder(eth_packet_sequencer_base_0,tmp);
                        //   oled_clear(zedboard_oled_params_0.base_address);
                  		// sprintf(&menuBuf[0], "Pkt Seq Odr:%d", (tmp));
                  		// oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
                          ui_draw();
                        }
//      else if(strcmp (c,"setsqwdly") == 0) {//PKT_WAIT_DELAY
//
//                                scanf ("%d",&tmp);
//
//                                set_stream_pkt_wait(eth_packet_sequencer_base_0,tmp*(0x40));//in micro seconds
//                                oled_clear(zedboard_oled_params_0.base_address);
//                        		sprintf(&menuBuf[0], "Pkt Seq WDly:%d us", (tmp));
//                        		oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
//                                ui_draw();
//                              }
      else if(strcmp (c,"setppdly") == 0) {//PKT_WAIT_DELAY

                                      scanf ("%d",&tmp);

                                      set_stream_pkt_wait(eth_packet_sequencer_base_0,tmp*(0x40));//in micro seconds
                                    //   oled_clear(zedboard_oled_params_0.base_address);
                              		// sprintf(&menuBuf[0], "Pkt Seq WDly:%d us", (tmp));
                              		// oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
                                      ui_draw();
                                    }
      else if(strcmp (c,"setsqsdly") == 0) {//PKT_SEND_DELAY

                                      scanf ("%d",&tmp);

                                    //   set_stream_pkt_send_delay(eth_packet_sequencer_base_0,tmp*(0x40));//in micro seconds
                                    //   oled_clear(zedboard_oled_params_0.base_address);
                              		// sprintf(&menuBuf[0], "Pkt Seq SDly:%d us", (tmp));
                              		// oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
                                      ui_draw();
                                    }


      else if(strcmp (c,"setplcpktsize") == 0)
      {
				SET_STATUS("Set PLC packet size (same as incoming rx packet size)\n");
				//UDP header is 8 bytes
				scanf ("%d",&tmp);
				int c = (tmp-2)%6;

				if (c > 0){
					printf("Warning:: Bad packet length:: The packet length formula is:: 6xN + 2 \n Choose N wisely!!!\n");
				}

				set_stream_rx_pkt_size(eth_packet_sequencer_base_0,(tmp-2));//in bytes
				// oled_clear(zedboard_oled_params_0.base_address);
				// sprintf(&menuBuf[0], "PLC pSz:%d", ((tmp-2)));
				// oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
				ui_draw();
			  }


      else if(strcmp (c,"setpblim") == 0) {
				//SET_STATUS("Enter payload length in 24 bit samples:\n");
				//UDP header is 8 bytes
				scanf ("%d",&tmp);

				set_stream_rx_buf_lim(eth_packet_sequencer_base_0,tmp);//in micro seconds
				set_stream_rx_buf_lim2(eth_to_audio_base_0,tmp);//in micro seconds
				// oled_clear(zedboard_oled_params_0.base_address);
				// sprintf(&menuBuf[0], "PoBlim:%d ", (tmp));
				// oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
				ui_draw();
			  }


      else if(strcmp (c,"setdlim") == 0) {
      				//SET_STATUS("Enter payload length in 24 bit samples:\n");
      				//UDP header is 8 bytes
      				scanf ("%d",&tmp);
      				set_stream_rx_buf_dlim(eth_to_audio_base_0,tmp);//in micro seconds
      				// oled_clear(zedboard_oled_params_0.base_address);
      				// sprintf(&menuBuf[0], "Dlim:%d ", (tmp));
      				// oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
      				ui_draw();
      			  }

      else if(strcmp (c,"setbslk") == 0) {
            				//SET_STATUS("Enter payload length in 24 bit samples:\n");
            				//UDP header is 8 bytes
            				scanf ("%d",&tmp);
            				set_stream_rx_buf_slack(eth_to_audio_base_0,tmp);//in micro seconds
            				// oled_clear(zedboard_oled_params_0.base_address);
            				// sprintf(&menuBuf[0], "BSlk:%d ", (tmp));
            				// oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
            				ui_draw();
            			  }
      else if(strcmp (c,"setbdlm") == 0) {
                  				//SET_STATUS("Enter payload length in 24 bit samples:\n");
                  				//UDP header is 8 bytes
                  				scanf ("%d",&tmp);
                  				set_stream_rx_buf_docc_lim(eth_to_audio_base_0,tmp);//in micro seconds
                  				// oled_clear(zedboard_oled_params_0.base_address);
                  				// sprintf(&menuBuf[0], "Bdoc:%d ", (tmp));
                  				// oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
                  				ui_draw();
                  			  }
      else if(strcmp (c,"setprdly") == 0) {
     				//SET_STATUS("Enter payload length in 24 bit samples:\n");
     				//UDP header is 8 bytes
     				scanf ("%d",&tmp);

     				set_plc_replace_delay(eth_packet_sequencer_base_0,tmp);//in clocks
     				// oled_clear(zedboard_oled_params_0.base_address);
     				// sprintf(&menuBuf[0], "RepDly:%d ", (tmp));
     				// oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
     				ui_draw();
     			  }
      else if(strcmp (c,"setsqlkdly") == 0) {
           				//SET_STATUS("Enter payload length in 24 bit samples:\n");
           				//UDP header is 8 bytes
           				scanf ("%d",&tmp);
           				set_seq_replace_lock_delay(eth_packet_sequencer_base_0,tmp);//in clocks
           				// oled_clear(zedboard_oled_params_0.base_address);
           				// sprintf(&menuBuf[0], "SQLkDly:%d ", (tmp));
           				// oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
           				ui_draw();
           			  }
      else if(strcmp (c,"sqwon") == 0) {
							  //SET_STATUS("Enter payload length in 24 bit samples:\n");

							  set_stream_pkt_wait_enable(eth_packet_sequencer_base_0);//in micro seconds
							//   oled_clear(zedboard_oled_params_0.base_address);
							// sprintf(&menuBuf[0], "Pkt Seq Dly:CUS");
							// oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
							  ui_draw();
							}
      else if(strcmp (c,"sqwoff") == 0) {

							set_stream_pkt_wait_disable(eth_packet_sequencer_base_0);//in micro seconds
							// oled_clear(zedboard_oled_params_0.base_address);
							// sprintf(&menuBuf[0], "Pkt Seq Dly:DFT");
							// oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
							ui_draw();
						  }
      else if(strcmp (c,"sqsdon") == 0) {

      							  set_stream_pkt_send_delay_enable(eth_packet_sequencer_base_0);//in micro seconds
      							//   oled_clear(zedboard_oled_params_0.base_address);
      							// sprintf(&menuBuf[0], "Pkt Seq Dly:CUS");
      							// oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
      							  ui_draw();
      							}
            else if(strcmp (c,"sqsdoff") == 0) {

      							set_stream_pkt_send_delay_disable(eth_packet_sequencer_base_0);//in micro seconds
      							// oled_clear(zedboard_oled_params_0.base_address);
      							// sprintf(&menuBuf[0], "Pkt Seq Dly:DFT");
      							// oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
      							ui_draw();
      						  }
            else if(strcmp (c,"plcoff") == 0) {

            	    set_plc_disable(eth_to_audio_base_0);//in micro seconds
					oled_clear(zedboard_oled_params_0.base_address);
					// sprintf(&menuBuf[0], "PLC:OFF");
					// oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
					ui_draw();
				  }
            else if(strcmp (c,"plcon") == 0) {

					set_plc_enable(eth_to_audio_base_0);//in micro seconds
					oled_clear(zedboard_oled_params_0.base_address);
					sprintf(&menuBuf[0], "PLC:ON");
					oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
					ui_draw();
				  }
            else if(strcmp (c,"strblen") == 0) {
                          //SET_STATUS("Enter payload length in 24 bit samples:\n");
                          //UDP header is 8 bytes
                          scanf ("%d",&tmp);
                          //write_eth_param(full_udp_stack_ip_base_0, PKT_LEN_ID, ((tmp*3) +8));//+8 done in fpga
                          //write_audio_to_eth_param(audio_to_eth_base_0, PAYLEN_REG_ID, tmp*3);
                          set_stream_blen(eth_to_audio_base_0,tmp);
                          sleep(1);
                          reset_dac_fifos(eth_to_audio_base_0);//LSB is left, MSB is Right

                        //   oled_clear(zedboard_oled_params_0.base_address);
                  		// sprintf(&menuBuf[0], "%s:%5d", menuitem[27], (tmp));
                  		// oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
                          ui_draw();
                        }
      else if(strcmp (c,"setpoutdly") == 0) {
    	  scanf ("%d",&tmp);
    	  set_playout_delay(eth_to_audio_base_0,tmp*96);
    	//   oled_clear(zedboard_oled_params_0.base_address);
		// sprintf(&menuBuf[0], "%s:%5d", "POUDLY", (tmp));
		// oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
		ui_draw();
      }
      else if(strcmp (c,"getpoutdly") == 0) {
			//SET_STATUS("Enter payload length in 24 bit samples:\n");
			//UDP header is 8 bytes
			tmp = get_playout_delay(eth_to_audio_base_0);
			printf("The current audio playout delay is:%d samples\n",(tmp));
			printf("The current audio playout delay is:%f ms\n",(tmp/96.0));
			// oled_clear(zedboard_oled_params_0.base_address);
			// sprintf(&menuBuf[0], "%d samples", (tmp));
			// oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
			// sprintf(&menuBuf[0], "%d ms", (tmp/96));
			// oled_print_message(&menuBuf[0], 2, zedboard_oled_params_0.base_address);
			ui_draw();
		  }

      else if(strcmp (c,"getpouttime") == 0) {
      			//SET_STATUS("Enter payload length in 24 bit samples:\n");
      			//UDP header is 8 bytes
      			tmp = get_instant_playout_time(eth_to_audio_base_0);
      			printf("The current audio playout time is:%f ms\n",(tmp/96.0));
      			// oled_clear(zedboard_oled_params_0.base_address);
      			// sprintf(&menuBuf[0], "TCO:%d ", (tmp));
      			// oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
      			// sprintf(&menuBuf[0], "%d ms", (tmp/96));
      			// oled_print_message(&menuBuf[0], 2, zedboard_oled_params_0.base_address);
      			ui_draw();
      		  }
      else if(strcmp (c,"getsynctime") == 0) {
			//SET_STATUS("Enter payload length in 24 bit samples:\n");
			//UDP header is 8 bytes
			tmp = get_sync_time(eth_to_audio_base_0);
			printf("The current audio sync time is:%f ms\n",(tmp/96.0));
			// oled_clear(zedboard_oled_params_0.base_address);
			// sprintf(&menuBuf[0], "TC:%d ", (tmp));
			// oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
			// sprintf(&menuBuf[0], "%d ms", (tmp/96));
			// oled_print_message(&menuBuf[0], 2, zedboard_oled_params_0.base_address);
			ui_draw();
		  }
      else if(strcmp (c,"getalle2a") == 0) {
      			//SET_STATUS("Enter payload length in 24 bit samples:\n");
      			//UDP header is 8 bytes

    	  printf("All reg values for the Eth to audio block\n");
      			get_all_eth2audio(eth_to_audio_base_0);
      			printf("The current audio sync time is:%f ms\n",(tmp/96.0));
      			// oled_clear(zedboard_oled_params_0.base_address);
      			// sprintf(&menuBuf[0], "TC:%d ", (tmp));
      			// oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
      			// sprintf(&menuBuf[0], "%d ms", (tmp/96));
      			// oled_print_message(&menuBuf[0], 2, zedboard_oled_params_0.base_address);
      			ui_draw();
      		  }
      else if(strcmp (c,"getrxtc") == 0) {
      			//SET_STATUS("Enter payload length in 24 bit samples:\n");
      			//UDP header is 8 bytes
      			tmp =  get_rx_time_code(eth_to_audio_base_0);
      			printf("The current audio rxtc is:%d samples\n",(tmp));
      			printf("The current audio rxtc is:%f ms\n",(tmp/96.0));
      			// oled_clear(zedboard_oled_params_0.base_address);
      			// sprintf(&menuBuf[0], "rtc:%d smp", (tmp));
      			// oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
      			// sprintf(&menuBuf[0], "rtc:%d ms", (tmp/96));
      			// oled_print_message(&menuBuf[0], 2, zedboard_oled_params_0.base_address);
      			ui_draw();
      		  }

      else if(strcmp (c,"getrxtceff") == 0) {
				//SET_STATUS("Enter payload length in 24 bit samples:\n");
				//UDP header is 8 bytes
				tmp =  get_rx_time_code_eff(eth_to_audio_base_0);
				printf("The current audio rxtceff is:%d samples\n",(tmp));
				printf("The current audio rxtceff is:%f ms\n",(tmp/96.0));
				// oled_clear(zedboard_oled_params_0.base_address);
				// sprintf(&menuBuf[0], "rtc:%d smp", (tmp));
				// oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
				// sprintf(&menuBuf[0], "rtc:%d ms", (tmp/96));
				// oled_print_message(&menuBuf[0], 2, zedboard_oled_params_0.base_address);
				ui_draw();
			  }

      else if(strcmp (c,"getrxtcocc") == 0) {
			//SET_STATUS("Enter payload length in 24 bit samples:\n");
			//UDP header is 8 bytes
			tmp =  get_rx_time_code_occ(eth_to_audio_base_0);
			printf("The current audio rxtc is:%d samples\n",(tmp));
			// oled_clear(zedboard_oled_params_0.base_address);
			// sprintf(&menuBuf[0], "rtcocc:%d smp", (tmp));
			// oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);

			ui_draw();
		  }
      else if(strcmp (c,"setlooplimit") == 0) {
          	  scanf ("%d",&tmp);
          	  set_loop_limit(eth_to_audio_base_0,tmp);
          	//   oled_clear(zedboard_oled_params_0.base_address);
      		// sprintf(&menuBuf[0], "%s:%5d", "LOOPLIM", (tmp));
      		// oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
      		ui_draw();
            }
      else if(strcmp (c,"gettcvars") == 0) {
    	  unsigned long tmp1, tmp2, tmp3;
			//SET_STATUS("Enter payload length in 24 bit samples:\n");
			//UDP header is 8 bytes

		tmp =  get_rx_time_code(eth_to_audio_base_0);
		printf("The current audio rxtc is:%0.3f ms\n",(tmp/96.0));
		tmp = get_rx_time_code_fout(eth_to_audio_base_0);
		printf("The current audio rxtcfout is:%0.3f ms\n",(tmp/96.0));
		  tmp1 =  get_rx_time_code_eff(eth_to_audio_base_0);
	      printf("The current audio rxtceff is:%0.3f ms\n",(tmp1/96.0));
    	  tmp2 = get_playout_delay(eth_to_audio_base_0);
    	  printf("The current audio playout delay is: %0.3f\n----------------------------------------\n",(tmp2/96.0));
    	  printf("The current audio sched playout time is: %0.3f\n",((tmp1+tmp2)/96.0));
    	  tmp = get_sync_time(eth_to_audio_base_0);
    	  printf("The current audio sync time is:%0.3f ms\n",(tmp/96.0));
    	  tmp = get_instant_playout_time(eth_to_audio_base_0);
    	  printf("The current audio playout time is:%0.3f ms\n",(tmp/96.0));
    	  tmp = get_dac_ofifo_occ(eth_to_audio_base_0);
    	  printf("The current odd audio buf sample count is:%d samples\n",(tmp));
    	  tmp = get_dac_efifo_occ(eth_to_audio_base_0);
    	  printf("The current even audio buf sample count is:%d samples\n",(tmp));


//			tmp =  get_rx_time_code_occ(eth_to_audio_base_0);
//			printf("The current audio rxtc occ is:%d\n",(tmp));

			// oled_clear(zedboard_oled_params_0.base_address);
			// sprintf(&menuBuf[0], "tsyncvars");
			// oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);

			ui_draw();
		  }
      else if(strcmp (c,"getpoutr") == 0) {
      			//SET_STATUS("Enter payload length in 24 bit samples:\n");
      			//UDP header is 8 bytes
      			tmp = get_instant_playout_reads(eth_to_audio_base_0);
      			printf("The current audio playout reads is:%d samples\n",(tmp));
      			// oled_clear(zedboard_oled_params_0.base_address);
      			// sprintf(&menuBuf[0], "%d samples", (tmp));
      			// oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
      			ui_draw();
      		  }
      else if(strcmp (c,"getpoutw") == 0) {
      			//SET_STATUS("Enter payload length in 24 bit samples:\n");
      			//UDP header is 8 bytes
      			tmp = get_instant_playout_writes(eth_to_audio_base_0);
      			printf("The current audio playout writes is:%d samples\n",(tmp));

      			// oled_clear(zedboard_oled_params_0.base_address);
      			// sprintf(&menuBuf[0], "%d samples", (tmp));
      			// oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
      			// oled_print_message(&menuBuf[1], 2, zedboard_oled_params_0.base_address);
      			ui_draw();
      		  }


      else if(strcmp (c,"getfocc") == 0) {
				//SET_STATUS("Enter payload length in 24 bit samples:\n");
				//UDP header is 8 bytes
				tmp = get_dac_fifo_occ(eth_to_audio_base_0);
				printf("The current audio buf sample count is:%d samples\n",(tmp));

				// oled_clear(zedboard_oled_params_0.base_address);
				// sprintf(&menuBuf[0], "BUF OCC::");
				// sprintf(&menuBuf[1], "%d samples", (tmp));
				// oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
				// oled_print_message(&menuBuf[1], 2, zedboard_oled_params_0.base_address);
				ui_draw();
			  }
      else if(strcmp (c,"setcrwin") == 0) {

          	  set_buf_corr_window(eth_to_audio_base_0,tmp);
          	//   oled_clear(zedboard_oled_params_0.base_address);
      		// sprintf(&menuBuf[0], "%s:%5d", "POUDLY", (tmp));
      		// oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
      		ui_draw();
            }

      else if ( strcmp (c, "tgsrcorr") == 0){
			  clear_buf_corr(eth_to_audio_base_0);
			  sleep(1);
          	  set_buf_corr_on(eth_to_audio_base_0);
			  printf("Toggle sample rate correction\n>");
			//   oled_clear(zedboard_oled_params_0.base_address);
			//   sprintf(&menuBuf[0], "%s", "SRCorr:ON");
			//   oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
			  ui_draw();
		  }

      else if ( strcmp (c, "setcoron") == 0){
    	  set_buf_corr_on(eth_to_audio_base_0);
          	  printf("Enable sample rate correction\n>");
          	//   oled_clear(zedboard_oled_params_0.base_address);
          	//   sprintf(&menuBuf[0], "%s", "SRCorr:ON");
          	//   oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
          	  ui_draw();
            }



      else if ( strcmp (c, "setcoroff") == 0){
          	  clear_buf_corr(eth_to_audio_base_0);
			  printf("Disable sample rate correction\n>");
			//   oled_clear(zedboard_oled_params_0.base_address);
			//   sprintf(&menuBuf[0], "%s", "SRCorr:OFF");
			//   oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
			  ui_draw();
		  }
      else if ( strcmp (c, "gete2astatus") == 0){
    	  	  printf("Eth to Audio ststus is::\n>");
    	  	  get_e2a_status(eth_to_audio_base_0);

			//   oled_clear(zedboard_oled_params_0.base_address);
			//   sprintf(&menuBuf[0], "%s", "ETH2AUD:Status");
			//   oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
			  ui_draw();
		  }

      else if(strcmp (c,"getmac") == 0) {
      		  //SET_STATUS("Enter MAC Address (xx.xx.xx.xx.xx.xx format)");
          	  //scanf("%s",ip);
      		  //extractMacAddress(ip,&macAddress[0]);
      		  //printf("\nMac Address: %02x. %02x. %02x. %02x. %02x. %02x\n",macAddress[0],macAddress[1],macAddress[2],macAddress[3],macAddress[4],macAddress[5]);
      		read_eth_mac(full_udp_stack_ip_base_0, MAC_ID,&ip[0]);
      		//printf("\nMAC Address:");
//      		      		   print_mac(tmp, ip);
      		//   oled_clear(zedboard_oled_params_0.base_address);
      		// sprintf(&menuBuf[0], "%s", menuitem[11]);
      		// oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
      		// sprintf(&menuBuf[0], "%s", ip);
      		// oled_print_message(&menuBuf[0], 2, zedboard_oled_params_0.base_address);

      		  ui_draw();
      	}

            else if(strcmp (c,"getdip") == 0) {
      		  //SET_STATUS("Enter Dest IP Address (xxx.xxx.xxx.xxx format): ");
          	  //scanf("%s",ip);
      		  //extractIpAddress(ip,&ipAddress[0]);
            //printf("\nIp Address: %03d. %03d. %03d. %03d\n",ipAddress[0],ipAddress[1],ipAddress[2],ipAddress[3]);
      		  tmp=read_eth_param(full_udp_stack_ip_base_0, DEST_IP_ID);
      		printf("\nDest Ip Address:");
      		   print_ip(tmp, ip);
      		//   oled_clear(zedboard_oled_params_0.base_address);
      		//   sprintf(&menuBuf[0], "%s", menuitem[13]);
      		//   		oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
      		//   		sprintf(&menuBuf[0], "%s", ip);
      		//   		oled_print_message(&menuBuf[0], 2, zedboard_oled_params_0.base_address);

      		  ui_draw();
      	}
            else if(strcmp (c,"getlip") == 0) {

            		tmp = read_eth_param(full_udp_stack_ip_base_0, LOCAL_IP_ID);
            		printf("\nLocal Ip Address:");print_ip(tmp, &ip[0]);
            		//   oled_clear(zedboard_oled_params_0.base_address);
            		// sprintf(&menuBuf[0], "%s", menuitem[12]);
            		// 		oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
            		// 		sprintf(&menuBuf[0], "%s", ip);
            		// 		oled_print_message(&menuBuf[0], 2, zedboard_oled_params_0.base_address);

            		  ui_draw();
            	}

            else if(strcmp (c,"getgip") == 0) {

                 		tmp=read_eth_param(full_udp_stack_ip_base_0, GTWAY_IP_ID);

                 		      		printf("\nGateway Ip Address:");
                 		      		  print_ip(tmp, ip);

                 		//   oled_clear(zedboard_oled_params_0.base_address);
                 		// sprintf(&menuBuf[0], "%s", menuitem[14]);
                 		// 		oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
                 		// 		sprintf(&menuBuf[0], "%s", ip);
                 		// 		oled_print_message(&menuBuf[0], 2, zedboard_oled_params_0.base_address);

                 		  ui_draw();
                 	}
            else if(strcmp (c,"getmask") == 0) {

      		  tmp=read_eth_param(full_udp_stack_ip_base_0, MASK_IP_ID);
      		printf("\nMask Ip Address:");
      		  print_ip(tmp, ip);
      		//   oled_clear(zedboard_oled_params_0.base_address);
      		//   sprintf(&menuBuf[0], "%s", menuitem[15]);
      		//   		oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
      		//   		sprintf(&menuBuf[0], "%s", ip);
      		//   		oled_print_message(&menuBuf[0], 2, zedboard_oled_params_0.base_address);
      		  ui_draw();
           }

            else if(strcmp (c,"getport") == 0) {
            	dest_port = read_eth_param(full_udp_stack_ip_base_0, DEST_PORT_ID);
            	printf("The port is:%d\n",dest_port);
                    // oled_clear(zedboard_oled_params_0.base_address);
                    // sprintf(&menuBuf[0], "%s", menuitem[16]);
					// oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
					// sprintf(&menuBuf[0], "%d", dest_port);
					// oled_print_message(&menuBuf[0], 2, zedboard_oled_params_0.base_address);
                    ui_draw();
                  }

            else if(strcmp (c,"getplen") == 0) {

                          tmp = get_stream_plen(audio_to_eth_base_0);
                          printf("The udp payload size is:%d\n",tmp);
                        //   oled_clear(zedboard_oled_params_0.base_address);
                  		// sprintf(&menuBuf[0], "%s%5d", menuitem[25], (tmp));
                  		// oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
                          ui_draw();
                        }
            else if(strcmp (c,"getporder") == 0) {

				  tmp = get_stream_porder(eth_packet_sequencer_base_0);
				  printf("The udp packet sequencer order is:%d\n",tmp);
				//   oled_clear(zedboard_oled_params_0.base_address);
				// sprintf(&menuBuf[0], "%s%5d", menuitem[26], (tmp));
				// oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
				  ui_draw();
				}
            else if(strcmp (c,"getpdrop") == 0) {

//				  tmp = get_pkts_dropped(packet_time_enforcer_base_0);
            	tmp = get_pkts_dropped(eth_packet_sequencer_base_0);
				  printf("The number of packets dropped is:%u\n",tmp);
				//   oled_clear(zedboard_oled_params_0.base_address);
				// sprintf(&menuBuf[0], "%s%d", "NPD:", (tmp));
				// oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
				  ui_draw();
				}
            else if(strcmp (c,"getpreplaced") == 0) {

            //				  tmp = get_pkts_dropped(packet_time_enforcer_base_0);
                        	tmp = get_pkts_replaced(eth_packet_sequencer_base_0);
            				  printf("The number of packets replaced is:%u\n",tmp);
            				//   oled_clear(zedboard_oled_params_0.base_address);
            				// sprintf(&menuBuf[0], "%s%d", "NPR:", (tmp));
            				// oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
            				  ui_draw();
            				}
            else if(strcmp (c,"getsqgs") == 0) {

				  tmp = get_seq_gs(eth_packet_sequencer_base_0);
				  printf("The udp packet sequencer gs is:%08x\n",tmp);
				//   oled_clear(zedboard_oled_params_0.base_address);
				// sprintf(&menuBuf[0], "%s%5d", "GST:", (tmp));
				// oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
				  ui_draw();
				}
			else if(strcmp (c,"getsqrx") == 0) {

//				  tmp = get_pkts_dropped(packet_time_enforcer_base_0);
				tmp = get_seq_pkts_rx(eth_packet_sequencer_base_0);
				  printf("The number of packets sqrx is:%u\n",tmp);
				//   oled_clear(zedboard_oled_params_0.base_address);
				// sprintf(&menuBuf[0], "%s%d", "SRX:", (tmp));
				// oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
				  ui_draw();
				}
			else if(strcmp (c,"getsqtx") == 0) {//get_seq_pkts_rx

//				  tmp = get_pkts_dropped(packet_time_enforcer_base_0);
				tmp = get_seq_pkts_tx(eth_packet_sequencer_base_0);
				  printf("The number of packets sqtx is:%u\n",tmp);
				//   oled_clear(zedboard_oled_params_0.base_address);
				// sprintf(&menuBuf[0], "%s%d", "STX:", (tmp));
				// oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
				  ui_draw();
				}
			else if(strcmp (c,"getsqov") == 0) {//get_seq_pkts_rx

			//				  tmp = get_pkts_dropped(packet_time_enforcer_base_0);
							tmp = get_seq_pkts_ov(eth_packet_sequencer_base_0);
							  printf("The number of packets sqov is:%u\n",tmp);
							//   oled_clear(zedboard_oled_params_0.base_address);
							// sprintf(&menuBuf[0], "%s%d", "SOV:", (tmp));
							// oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
							  ui_draw();
							}
            else if(strcmp (c,"setptl1") == 0) {//spkt2pkt
				//SET_STATUS("Enter payload length in 24 bit samples:\n");
				//UDP header is 8 bytes
				scanf ("%d",&tmp);
				//write_eth_param(full_udp_stack_ip_base_0, PKT_LEN_ID, ((tmp*3) +8));//+8 done in fpga
				//write_audio_to_eth_param(audio_to_eth_base_0, PAYLEN_REG_ID, tmp*3);
				set_spkt_to_pkt_delay_limit(packet_time_enforcer_base_0,tmp);//just sample clock counts no need to multiply by 2
				// oled_clear(zedboard_oled_params_0.base_address);
				// sprintf(&menuBuf[0], "Buffer2Drop:%d", (tmp));
				// oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
				ui_draw();
			  }

            else if(strcmp (c,"setptpidl") == 0) {//spkt2pkt_locked
            				scanf ("%d",&tmp);
            				set_spkt_to_pkt_locked_delay_limit(packet_time_enforcer_base_0,tmp);//just sample clock counts no need to multiply by 2
            				// oled_clear(zedboard_oled_params_0.base_address);
            				// sprintf(&menuBuf[0], "BL2Drop:%d", (tmp));
            				// oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
            				ui_draw();
            			  }

		  else if(strcmp (c,"setptl2") == 0) {//spkt2pkt

			  scanf ("%d",&tmp);//in milliseconds

			  set_accum_pkt_to_pkt_delay_limit(packet_time_enforcer_base_0,tmp);//just sample clock counts no need to multiply by 2
			//   oled_clear(zedboard_oled_params_0.base_address);
			//   sprintf(&menuBuf[0], "BufferA2Drop:%d", (tmp));
			//   oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
			  ui_draw();
			}

            else if(strcmp (c,"getptl1") == 0) {

			  tmp = get_pkts_drop_spkt_limit(packet_time_enforcer_base_0);
			  printf("The delayed pkt2pkt drop limit is:%d\n",(tmp));
			  printf("The delayed pkt2pkt drop limit is:%u ms\n",(tmp/(96)));//just sample clock counts no need to multiply by 2
			//   oled_clear(zedboard_oled_params_0.base_address);
			//   sprintf(&menuBuf[0], "%s%10u", "PDLM:", (tmp));
			//   oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
			  ui_draw();
			}
            else if(strcmp (c,"getptpidl") == 0) {

			  tmp = get_pkts_drop_spkt_locked_limit(packet_time_enforcer_base_0);
			  printf("The delayed pkt2pkt secondary drop limit is:%d\n",(tmp));
			  printf("The delayed pkt2pkt secondary drop limit is:%u ms\n",(tmp/(96)));//just sample clock counts no need to multiply by 2
			//   oled_clear(zedboard_oled_params_0.base_address);
			//   sprintf(&menuBuf[0], "%s%10u", "PDSLM:", (tmp));
			//   oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
			  ui_draw();
			}
            else if(strcmp (c,"getptl2") == 0) {

			  tmp = get_pkts_adrop_spkt_limit(packet_time_enforcer_base_0);
			  printf("The delayed pkt running drop limit is:%d\n",(tmp));
			  printf("The delayed pkt running drop limit is:%u ms\n",(tmp/(96)));
			//   oled_clear(zedboard_oled_params_0.base_address);
			// sprintf(&menuBuf[0], "%s%10u", "RDLM:", (tmp));
			// oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
			  ui_draw();
			}

            else if(strcmp (c,"getptlock") == 0) {

			  tmp = get_pkts_ptrx_lock(packet_time_enforcer_base_0);
			  printf("The packet timming unit lock status is:%08X\n",tmp);
			//   oled_clear(zedboard_oled_params_0.base_address);
			// sprintf(&menuBuf[0], "%s%d", "LOCK:", (tmp));
			// oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
			  ui_draw();
			}


            else if(strcmp (c,"ptpidon") == 0) {

            	pkt_to_pkt_locked_drop_enable(packet_time_enforcer_base_0);
            			  printf("The packet timming unit pid tatus is on.\n");
            			//   oled_clear(zedboard_oled_params_0.base_address);
            			// sprintf(&menuBuf[0], "%s", "PID:ON");
            			// oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
            			  ui_draw();
            			}
            else if(strcmp (c,"ptpidoff") == 0) {

                        	pkt_to_pkt_locked_drop_disable(packet_time_enforcer_base_0);
                        			  printf("The packet timming unit pid tatus is off.\n");
                        			//   oled_clear(zedboard_oled_params_0.base_address);
                        			// sprintf(&menuBuf[0], "%s", "PID:OFF");
                        			// oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
                        			  ui_draw();
                        			}
            else if(strcmp (c,"getptstatus") == 0) {
            	tmp = get_pkts_pt_accum(packet_time_enforcer_base_0);
            			tmp = get_pkts_ptrx_lock(packet_time_enforcer_base_0);
            			  tmp = get_pkts_pt_disable(packet_time_enforcer_base_0);

            			  printf("The packet timming unit status is:%08X\n",tmp==0?1:0);
            			//   oled_clear(zedboard_oled_params_0.base_address);
            			// sprintf(&menuBuf[0], "%s%d", "PBT:", (tmp));
            			// oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
            			  ui_draw();
            			}

            else if(strcmp (c,"setptlock") == 0) {

			  tmp = set_pkts_ptrx_lock(packet_time_enforcer_base_0, 1);
			  printf("The packet timming unit lock status is:%08X\n",tmp);
			//   oled_clear(zedboard_oled_params_0.base_address);
			// sprintf(&menuBuf[0], "%s%10u", "LOCK:", (tmp));
			// oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
			  ui_draw();
			}

            else if(strcmp (c,"setptaccum") == 0) {

            			  tmp = set_pkts_pt_accum(packet_time_enforcer_base_0, 1);
            			  printf("The packet timming unit accum status is:%08X\n",tmp);
            			//   oled_clear(zedboard_oled_params_0.base_address);
            			// sprintf(&menuBuf[0], "%s%10u", "ACCUM:", (tmp));
            			// oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
            			  ui_draw();
            			}
            else if(strcmp (c,"clrptaccum") == 0) {

			  tmp = set_pkts_pt_accum(packet_time_enforcer_base_0, 0);
			  printf("The packet timming unit accum status is:%08X\n",tmp);
			//   oled_clear(zedboard_oled_params_0.base_address);
			// sprintf(&menuBuf[0], "%s%10u", "ACCUM:", (tmp));
			// oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
			  ui_draw();
			}


            else if(strcmp (c,"getptaccum") == 0) {

			  tmp = get_pkts_pt_accum(packet_time_enforcer_base_0);
			  printf("The packet timming unit accum status is:%08X\n",tmp);
			//   oled_clear(zedboard_oled_params_0.base_address);
			// sprintf(&menuBuf[0], "%s%10u", "ACCUM:", (tmp));
			// oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
			  ui_draw();
			}

            else if(strcmp (c,"clrptlock") == 0) {

			  tmp = set_pkts_ptrx_lock(packet_time_enforcer_base_0, 0);
			  printf("The packet timming unit lock status is:%u\n",tmp);
			//   oled_clear(zedboard_oled_params_0.base_address);
			// sprintf(&menuBuf[0], "%s%10u", "LOCK:", (tmp));
			// oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
			  ui_draw();
			}

            else if(strcmp (c,"getblen") == 0) {

				  tmp = get_stream_blen(eth_to_audio_base_0);
				  printf("The audio rx buff sample count is:%d\n",tmp);
				//   oled_clear(zedboard_oled_params_0.base_address);
				// sprintf(&menuBuf[0], "%s%d", "RXBOCP:", (tmp));
				// oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
				  ui_draw();
				}
            else if(strcmp (c,"getbrdly") == 0) {

			  tmp = get_stream_readout_delay(eth_to_audio_base_0);
			  printf("The current audio rx buff readout delay is:%f ms\n",(tmp/96.0));
			//   oled_clear(zedboard_oled_params_0.base_address);
			// sprintf(&menuBuf[0], "%s%d", "BRDLY:", (tmp));
			// oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
			  ui_draw();
			}

		else if(strcmp (c,"getnetdly") == 0) {

		  tmp = get_stream_net_delay(packet_time_enforcer_base_0);
		  printf("The current audio network delay is:%f ms\n",(tmp/96.0));
		//   oled_clear(zedboard_oled_params_0.base_address);
		//   sprintf(&menuBuf[0], "%s%d", "NETDLY:", (tmp));
		//   oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
		  ui_draw();
		}

		else if(strcmp (c,"getpktbuilddly") == 0) {

		  tmp = get_stream_pktbuild_delay(packet_time_enforcer_base_0);
		  printf("The current audio pkt build delay is:%f ms\n",(tmp/96.0));
		//   oled_clear(zedboard_oled_params_0.base_address);
		//   sprintf(&menuBuf[0], "%s%d", "PKTBUILD:", (tmp));
		//   oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
		  ui_draw();
		}


		else if(strcmp (c,"getpkt2pktdly") == 0) {

				  tmp = get_stream_spkt_delay(packet_time_enforcer_base_0);
				  printf("The current audio pkt to pkt delay is:%f ms\n",(tmp/96.0));
				//   oled_clear(zedboard_oled_params_0.base_address);
				//   sprintf(&menuBuf[0], "%s%d", "PKTBUILD:", (tmp));
				//   oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
				  ui_draw();
				}


		else if(strcmp (c,"getaccpktdly") == 0) {

			  tmp = get_stream_accum_pkt_delay(packet_time_enforcer_base_0);
			  printf("The current audio pkt accum delay is:%f ms\n",(tmp/96.0));
			//   oled_clear(zedboard_oled_params_0.base_address);
			//   sprintf(&menuBuf[0], "%s%d", "PKTBUILD:", (tmp));
			//   oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
			  ui_draw();
			}





		else if(strcmp (c,"setnetdly") == 0) {
			scanf ("%d",&tmp);
		  tmp = set_stream_net_delay(packet_time_enforcer_base_0,tmp);
		  printf("The current audio network delay is:%d sample counts\n",(tmp));
		//   oled_clear(zedboard_oled_params_0.base_address);
		//   sprintf(&menuBuf[0], "%s%d", "NETDLY:", (tmp));
		//   oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
		  ui_draw();
		}

		else if(strcmp (c,"setpktdly") == 0) {
			scanf ("%d",&tmp);
		  tmp = set_stream_pktbuild_delay(packet_time_enforcer_base_0, tmp);
		  printf("The current audio pkt build delay is:%d sample clocks\n",(tmp));
		//   oled_clear(zedboard_oled_params_0.base_address);
		//   sprintf(&menuBuf[0], "%s%d", "PKTBUILD:", (tmp));
		//   oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
		  ui_draw();
		}



		else if(strcmp (c,"ptdfon") == 0) {

					  tmp = set_pkts_pt_diff_status(packet_time_enforcer_base_0, 1);
					  printf("The packet timming unit diff status is:%08X\n",tmp);
					//   oled_clear(zedboard_oled_params_0.base_address);
					// sprintf(&menuBuf[0], "%s%u", "ACCUM:", (tmp));
					// oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
					  ui_draw();
					}
		else if(strcmp (c,"ptdfoff") == 0) {

		  tmp = set_pkts_pt_diff_status(packet_time_enforcer_base_0, 0);
		  printf("The packet timming unit diff status is:%08X\n",tmp);
		//   oled_clear(zedboard_oled_params_0.base_address);
		// sprintf(&menuBuf[0], "%s%10u", "ACCUM:", (tmp));
		// oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
		  ui_draw();
		}


		else if(strcmp (c,"getptdiffs") == 0) {

		  tmp = get_pkts_pt_diff_status(packet_time_enforcer_base_0);
		  printf("The packet timming unit diff status is:%08X\n",tmp);
		//   oled_clear(zedboard_oled_params_0.base_address);
		// sprintf(&menuBuf[0], "%s%u", "ACCUM:", (tmp));
		// oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
		  ui_draw();
		}


		else if(strcmp (c,"setptdiffl") == 0) {//spkt2pkt

		  scanf ("%d",&tmp);//in milliseconds

		  set_diff_pkt_to_pkt_delay_limit(packet_time_enforcer_base_0,tmp);//just sample clock counts no need to multiply by 2
		//   oled_clear(zedboard_oled_params_0.base_address);
		// sprintf(&menuBuf[0], "BufferA2Drop:%d", (tmp));
		// oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
		  ui_draw();
		}

		else if(strcmp (c,"getptdiffl") == 0) {

		  tmp = get_diff_pkt_to_pkt_delay_limit(packet_time_enforcer_base_0);
		  printf("The delayed diff pkt2pkt drop limit is:%d\n",(tmp));
		  printf("The delayed diff pkt2pkt drop limit is:%u ms\n",(tmp/(96)));//just sample clock counts no need to multiply by 2
		//   oled_clear(zedboard_oled_params_0.base_address);
		// sprintf(&menuBuf[0], "%s%u", "PDLM:", (tmp));
		// oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
		  ui_draw();
		}


		else if(strcmp (c,"getptdiffv") == 0) {

				  tmp = get_diff_pkt_to_pkt_delay(packet_time_enforcer_base_0);
				  printf("The diff pkt2pkt drop is:%d\n",(tmp));
				  printf("The diff pkt2pkt drop is:%u ms\n",(tmp/(96)));//just sample clock counts no need to multiply by 2
				//   oled_clear(zedboard_oled_params_0.base_address);
				// sprintf(&menuBuf[0], "%s%u", "PDLM:", (tmp));
				// oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
				  ui_draw();
				}

		else if(strcmp (c,"getsqos") == 0)
		{
			  //SET_STATUS("Enter payload length in 24 bit samples:\n");

			  tmp = get_out_exec_state(eth_packet_sequencer_base_0);//in micro seconds
			//   oled_clear(zedboard_oled_params_0.base_address);
			// sprintf(&menuBuf[0], "OXecS=%d",tmp);
			// oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
			  ui_draw();
		}

		else if(strcmp (c,"getsqis") == 0)
				{
					  //SET_STATUS("Enter payload length in 24 bit samples:\n");

					  tmp = get_in_exec_state(eth_packet_sequencer_base_0);//in micro seconds
					//   oled_clear(zedboard_oled_params_0.base_address);
					// sprintf(&menuBuf[0], "IXecS=%d",tmp);
					// oled_print_message(&menuBuf[0], 1, zedboard_oled_params_0.base_address);
					  ui_draw();
				}

      else if (strcmp (c, "help") == 0) {
        printf ("|==================+================================================|\n");
        printf ("|Command           |Description                                     |\n");
        printf ("|==================+================================================|\n");
        printf ("|vgg <gain value>  |volume gain for global level					 |\n");
        printf ("|------------------+------------------------------------------------|\n");
        printf ("|vll <gain value>  |volume gain for left channel of loopback stream |\n");
        printf ("|------------------+------------------------------------------------|\n");
        printf ("|vlr <gain value>  |volume gain for right channel of loopback stream|\n");
        printf ("|------------------+------------------------------------------------|\n");
        printf ("|vnl <gain value>  |volume gain for left channel of network stream  |\n");
        printf ("|------------------+------------------------------------------------|\n");
        printf ("|vnr <gain value>  |volume gain for right channel of network stream |\n");
        printf ("|------------------+------------------------------------------------|\n");
        printf ("|    lfhe/lfhd     |High pass enable/Disable for loopback           |\n");
        printf ("|------------------+------------------------------------------------|\n");
        printf ("|    lfbe/lfbd     |Band pass enable/Disable for loopback           |\n");
        printf ("|------------------+------------------------------------------------|\n");
        printf ("|    lfle/lfld     |Low pass enable/Disable for loopback            |\n");
        printf ("|------------------+------------------------------------------------|\n");
        printf ("|    nfhe/nfhd     |High pass enable/Disable for network            |\n");
        printf ("|------------------+------------------------------------------------|\n");
        printf ("|    nfbe/nfbd     |Band pass enable/Disable for network            |\n");
        printf ("|------------------+------------------------------------------------|\n");
        printf ("|    nfle/nfld     |Low pass enable/Disable for network             |\n");
        printf ("|------------------+------------------------------------------------|\n");
        printf ("|    nstats        |Byte counts and error counts for the network    |\n");
        printf ("|------------------+------------------------------------------------|\n");
        printf ("|    astatus       |Hardware ADC audio FIFO Status                  |\n");
        printf ("|------------------+------------------------------------------------|\n");
        printf ("|    dstatus       |Hardware DAC audio FIFO Status                  |\n");
        printf ("|------------------+------------------------------------------------|\n");
        printf ("|    areset        |Hardware ADC audio FIFO Reset                   |\n");
        printf ("|------------------+------------------------------------------------|\n");
        printf ("|    dreset        |Hardware DAC audio FIFO Reset                   |\n");
        printf ("|------------------+------------------------------------------------|\n");
        printf ("|    freset        | Resets the udp core and the audio fifos.       |\n");
        printf ("|------------------+------------------------------------------------|\n");
        printf ("|    preset   		| Resets the packet sequencer  					 |\n");
        printf ("|------------------+------------------------------------------------|\n");
        printf ("|    creset 		| Resets just the udp core.						 |\n");
        printf ("|------------------+------------------------------------------------|\n");
        printf ("|    rxrsync 		| Re-syncronise the receive audio chain       	 |\n");
        printf ("|------------------+------------------------------------------------|\n");
		printf ("|    strstart      |Start streaming                                 |\n");
        printf ("|------------------+------------------------------------------------|\n");
        printf ("|    strstop       |Stop streaming                                  |\n");
        printf ("|------------------+------------------------------------------------|\n");
        printf ("|    strseleth     |Play ETH stream                                 |\n");
        printf ("|------------------+------------------------------------------------|\n");
        printf ("|    strselarm     |Play ARM stream                                 |\n");
        printf ("|------------------+------------------------------------------------|\n");
        printf ("|    strdip <ip>   |Streaming destination IP                        |\n");
        printf ("|------------------+------------------------------------------------|\n");
        printf ("|    strlip <ip>   |Streaming local IP                              |\n");
        printf ("|------------------+------------------------------------------------|\n");
        printf ("|    strport <port>|Streaming port                                  |\n");
        printf ("|------------------+------------------------------------------------|\n");
        printf ("|    strgip <ip>   |Streaming gateway IP                            |\n");
        printf ("|------------------+------------------------------------------------|\n");
        printf ("|    strmask <ip>  |Streaming netmask IP                            |\n");
        printf ("|------------------+------------------------------------------------|\n");
        printf ("|    strmac <mac>  |Streaming MAC Address                           |\n");
        printf ("|------------------+------------------------------------------------|\n");
        printf ("|    strnsp <sp>   |Streaming net speed                             |\n");
        printf ("|------------------+------------------------------------------------|\n");
        printf ("|    strplen <len> |Streaming payload packet length                 |\n");
        printf ("|------------------+------------------------------------------------|\n");
        printf ("|  strporder <ord> |Set streaming packet sequencer order      |\n");
        printf ("|------------------+------------------------------------------------|\n");
		printf ("|    getdip       |Read streaming destination IP                   |\n");
		printf ("|------------------+------------------------------------------------|\n");
		printf ("|    getlip       |Read streaming local IP                         |\n");
		printf ("|------------------+------------------------------------------------|\n");
		printf ("|    getport      |Read streaming port                             |\n");
		printf ("|------------------+------------------------------------------------|\n");
		printf ("|    getgip       |Read streaming gateway IP                       |\n");
		printf ("|------------------+------------------------------------------------|\n");
		printf ("|    getmask      |Read streaming netmask IP                       |\n");
		printf ("|------------------+------------------------------------------------|\n");
		printf ("|    getmac       |Read streaming MAC Address                      |\n");
		printf ("|------------------+------------------------------------------------|\n");
		printf ("|    getnsp       |Read streaming net speed                        |\n");
		printf ("|------------------+------------------------------------------------|\n");
		printf ("|    getplen      |Read streaming payload packet length            |\n");
		printf ("|------------------+------------------------------------------------|\n");
		printf ("|    getblen      |Read streaming rx sample buffer length          |\n");
		printf ("|------------------+------------------------------------------------|\n");
	    printf ("|    getporder    |Read streaming packet sequencer order            |\n");
        printf ("|------------------+------------------------------------------------|\n");
        printf ("|    exit     		|Exit the application				             |\n");
        printf ("|==================+================================================|\n");
        getchar ();
        getchar ();
      }else if ( strcmp (c, "exit") == 0){
    	  gain = 0;
    	  globalVol = gain;

		  set_volume (volume_control_base_0, params.vl_lpbk*gain, CHANNEL_ID_L);
		  set_volume (volume_control_base_0, params.vr_lpbk*gain, CHANNEL_ID_R);

		  set_volume (volume_control_base_1, params.vl_net*gain, CHANNEL_ID_L);
		  set_volume (volume_control_base_1, params.vr_net*gain, CHANNEL_ID_R);
		  update_leds();

		//   oled_clear(zedboard_oled_params_0.base_address);
		// sprintf(&menuBuf[0], "%s", "  BYE   ");
		// oled_print_message(&menuBuf[0], 0, zedboard_oled_params_0.base_address);
		// sprintf(&menuBuf[1], "%s-%s-", "Cantavi", "S");
		// oled_print_message(&menuBuf[0], 2, zedboard_oled_params_0.base_address);
		ui_exit ();
        SET_STATUS("Shutting down audio.\nClosing app\n");
        exit(0);
      }
      else {
        SET_STATUS("Invalid Command\n");
        // oled_clear(zedboard_oled_params_0.base_address);
		// sprintf(&menuBuf[0], "%s", "  INVALID  ");
		// oled_print_message(&menuBuf[0], 0, zedboard_oled_params_0.base_address);
		// sprintf(&menuBuf[0], "%s", "  Command  ");
		// oled_print_message(&menuBuf[0], 2, zedboard_oled_params_0.base_address);

        ui_draw();
      }
  }
  return NULL;
}

/**
 * =======================================================
 * draw the user interface
 * @param   : data  : thread specific data
 * @return  : returns NULL
 * =======================================================
 */
void ui_vol_draw () {
    system ("clear");
    printf ("|============+============+============|\n");
    printf ("| Parameters +  Loopback  +   Network  |\n");
    printf ("|============+============+============|\n");
    printf ("|  Volume_G  +    %4d    +    %4d    |\n", params.v_global, params.v_global);
    printf ("|------------+------------+------------|\n");
    printf ("|  Volume_L  +    %4d    +    %4d    |\n", params.vl_lpbk, params.vl_net);
    printf ("|------------+------------+------------|\n");
    printf ("|  Volume_R  +    %4d    +    %4d    |\n", params.vr_lpbk, params.vr_net);
    printf ("|------------+------------+------------|\n");
    printf ("|  FILTER_B  +    %-4s    +    %-4s    |\n", (params.filter_b_lpbk ? "On":"Off"), (params.filter_b_net ? "On":"Off"));
    printf ("|------------+------------+------------|\n");
    printf ("|  FILTER_L  +    %-4s    +    %-4s    |\n",(params.filter_l_lpbk ? "On":"Off"), (params.filter_l_net ? "On":"Off"));
    printf ("|------------+------------+------------|\n");
    printf ("|  FILTER_H  +    %-4s    +    %-4s    |\n", (params.filter_h_lpbk ? "On":"Off"), (params.filter_h_net ? "On":"Off"));
    printf ("|============+============+============|\n");
    printf ("\n%s\n", params.status);

}

/**
 * =======================================================
 * draw the user interface
 * @param   : data  : thread specific data
 * @return  : returns NULL
 * =======================================================
 */
void ui_draw () {
    //system ("clear");//causes minicom to clear
    //printf ("\n%s\n", params.status);
    printf ("\n>:");

}



/**
 * =======================================================
 * ui_init initialize the user interface
 * @return  : returns 0 upon success
 *            -1 otherwise
 * =======================================================
 */
int ui_init (int argc, char *argv[]) {



	eth_to_audio_params_0 = map_device (ETH_TO_AUDIO_DEVICE_0);
	    if (eth_to_audio_params_0.base_address == NULL) {
	       perror ("ui_init : eth_to_audio_params_0");
	       return -1;
	    }else{
	  	  eth_to_audio_base_0 = eth_to_audio_params_0.base_address;
	    }

	filter_control_params_0 = map_device(FILTER_DEVICE_0);
	  if (filter_control_params_0.base_address == NULL) {
		 perror ("ui_init : filter_control_params_0");
		 return -1;
	  }else{
		  filter_control_base_0 = filter_control_params_0.base_address;
	  }

	  volume_control_params_0 = map_device(VOLUME_DEVICE_0);
		if (volume_control_params_0.base_address == NULL) {
		   perror ("ui_init : volume_control_params_0");
		   return -1;
		}else{
		  volume_control_base_0 = volume_control_params_0.base_address;
		}

	  filter_control_params_1 = map_device(FILTER_DEVICE_1);
	  if (filter_control_params_1.base_address == NULL) {
		 perror ("ui_init : filter_control_params_1");
		 return -1;
	  }else{
		  filter_control_base_1 = filter_control_params_1.base_address;
	  }


	  volume_control_params_1 = map_device(VOLUME_DEVICE_1);
	    if (volume_control_params_1.base_address == NULL) {
	       perror ("ui_init : volume_control_params_1");
	       return -1;
	    }else{
	  	  volume_control_base_1 = volume_control_params_1.base_address;
	    }

	zedboard_oled_params_0 = map_device (ZEDBOARDOLED_0);

  if (zedboard_oled_params_0.base_address == NULL) {
     perror ("ui_init : zedboard_oled_params_0");
     return -1;
  }else{
	  zedboard_oled_base_0 = zedboard_oled_params_0.base_address;
  }

  pmod_controller_params_0 = map_device(PMOD_CONTROLLER_0);
  if (pmod_controller_params_0.base_address == NULL) {
     perror ("ui_init : pmod_controller_params_0");
     return -1;
  }else{
	  pmod_controller_base_0 = pmod_controller_params_0.base_address;
  }


   audio_to_eth_params_0 = map_device (AUDIO_TO_ETH_DEVICE_0);
      if (audio_to_eth_params_0.base_address == NULL) {
         perror ("ui_init : audio_to_eth_params_0");
         return -1;
      }else{
      	audio_to_eth_base_0 = audio_to_eth_params_0.base_address;
      }


      full_udp_stack_ip_params_0 = map_device (FULL_UDP_STACK_IP_0);
		if (full_udp_stack_ip_params_0.base_address == NULL) {
		   perror ("ui_init : full_udp_stack_ip_params_0");
		   return -1;
		}else{
			full_udp_stack_ip_base_0 = full_udp_stack_ip_params_0.base_address;
		}


      eth_packet_sequencer_params_0 = map_device (ETH_PLC_SEQ_0);
            if (eth_packet_sequencer_params_0.base_address == NULL) {
               perror ("ui_init : eth_plc_packet_sequencer_params_0");
               return -1;
            }else{
            	eth_packet_sequencer_base_0 = eth_packet_sequencer_params_0.base_address;
            }

//            packet_time_enforcer_params_0 = map_device (PACKET_TIME_ENFORCER_0);
//            if (eth_packet_sequencer_params_0.base_address == NULL) {
//               perror ("ui_init : packet_time_enforcer_params_0");
//               return -1;
//            }else{
//            	packet_time_enforcer_base_0 = packet_time_enforcer_params_0.base_address;
//            }



			adau1761_params_0 = map_device (ADAU1761_CODEC_0);
			if (adau1761_params_0.base_address == NULL) {
			   perror ("ui_init : adau1761_params_0 error");
			   return -1;
			}else{
				adau1761_base_0 = adau1761_params_0.base_address;
			}

			time_sync_params_0 = map_device (TIME_SYNC_0);
			if (time_sync_params_0.base_address == NULL) {
			   perror ("ui_init : time_sync_params_0 error");
			   return -1;
			}else{
				time_sync_base_0 = time_sync_params_0.base_address;
			}


			init_codec();
  //-------------------------------------------------------------------------------------
//
//  mkfifo("/tmp/myfifo", 0666);
//  mkfifo("/tmp/axififo", 0666);
char macAddress[6];
char ipAddress[4];
  init_gpios();
  //-------------------------------------------------------------------------------------
  //printf ("Setting up UDP Client on server ip_address=%s:: and port=%d\n",argv[1], atoi(argv[2]));
//  sprintf(saddr.ip, argv[1]);

  sprintf(local_mac, argv[1]);
  sprintf(local_ip, argv[2]);
  sprintf(dest_ip, argv[3]);
  sprintf(gateway_ip, argv[4]);
  sprintf(mask_ip, argv[5]);
  dest_port = atoi(argv[6]);
  sync_port = atoi(argv[7]);



  printf ("Set local mac=%s ::\n local address=%s ::\n dest address=%s ::\n gateway address=%s ::\n mask address=%s ::\n udp_port=%d ::\n sync_port=%d\n",local_mac, local_ip, dest_ip, gateway_ip, mask_ip, dest_port, sync_port);



	  extractMacAddress(local_mac,&macAddress[0]);
	  printf("\nMac Address: %02x. %02x. %02x. %02x. %02x. %02x\n",macAddress[0],macAddress[1],macAddress[2],macAddress[3],macAddress[4],macAddress[5]);
	  //write_eth_mac(full_udp_stack_ip_base_0, MAC_ID, ((macAddress[0]<<40)|(macAddress[1]<<32)|macAddress[2]<<24)|(macAddress[3]<<16)|(macAddress[4]<<8)|(macAddress[5]));
	  write_eth_mac(full_udp_stack_ip_base_0, MAC_ID, &macAddress[0]);

	  extractIpAddress(local_ip,&ipAddress[0]);
	  printf("\nLocal Ip Address: %03d. %03d. %03d. %03d\n",ipAddress[0],ipAddress[1],ipAddress[2],ipAddress[3]);
		write_eth_param(full_udp_stack_ip_base_0, LOCAL_IP_ID, (ipAddress[0]<<24)|(ipAddress[1]<<16)|(ipAddress[2]<<8)|(ipAddress[3]));


		extractIpAddress(dest_ip,&ipAddress[0]);
		printf("\nDest Ip Address: %03d. %03d. %03d. %03d\n",ipAddress[0],ipAddress[1],ipAddress[2],ipAddress[3]);
		write_eth_param(full_udp_stack_ip_base_0, DEST_IP_ID, (ipAddress[0]<<24)|(ipAddress[1]<<16)|(ipAddress[2]<<8)|(ipAddress[3]));


		extractIpAddress(mask_ip,&ipAddress[0]);
		printf("\nMask Ip Address: %03d. %03d. %03d. %03d\n",ipAddress[0],ipAddress[1],ipAddress[2],ipAddress[3]);
		write_eth_param(full_udp_stack_ip_base_0, MASK_IP_ID, (ipAddress[0]<<24)|(ipAddress[1]<<16)|(ipAddress[2]<<8)|(ipAddress[3]));

		extractIpAddress(gateway_ip,&ipAddress[0]);
		printf("\nGateway Ip Address: %03d. %03d. %03d. %03d\n",ipAddress[0],ipAddress[1],ipAddress[2],ipAddress[3]);
		write_eth_param(full_udp_stack_ip_base_0, GTWAY_IP_ID, (ipAddress[0]<<24)|(ipAddress[1]<<16)|(ipAddress[2]<<8)|(ipAddress[3]));


	write_eth_param(full_udp_stack_ip_base_0, DEST_PORT_ID, dest_port);
	write_eth_param(full_udp_stack_ip_base_0, SYNC_PORT_ID, sync_port);

  sockfd = udp_client_setup( argv[1], atoi(argv[2]));
  if (sockfd < 1) {
    fprintf (stderr, "udp_client_setup failed exiting\n");
    return -1;
  }else{

	 printf("UDP Client connected\n");

  }

  //--------------------------------------------------------------------------------------
    attr_axi_to_net_w.mq_flags = 0;
    attr_axi_to_net_w.mq_maxmsg = 4*AXI_PKT_SIZE*2;
    attr_axi_to_net_w.mq_msgsize = 4*AXI_PKT_SIZE;
    attr_axi_to_net_w.mq_curmsgs = 0;
    printf ("Configuring axi_to_net memory write buffers \n");
    //msgq_id_w = mq_open(MSGQOBJ_NAME, O_RDWR | O_CREAT | O_EXCL, S_IRWXU | S_IRWXG, &attr_w);
    //EXIST pathname already exists and O_CREAT and O_EXCL were used." I don't use O_EXCL.
    // msgq_axi_to_net_w = mq_open(A2N_MSGQOBJ_NAME, O_RDWR | O_CREAT , S_IRWXU | S_IRWXG, &attr_axi_to_net_w);
    // if (msgq_axi_to_net_w == (mqd_t)-1) {
    //   perror("Error ui_init : mq_open(msq_axi_to_net_w)\n");
    //   return -1;
    // }
    // printf ("Configuring axi_to_net memory read buffers\n");
    // msgq_axi_to_net_r = mq_open(A2N_MSGQOBJ_NAME, O_RDWR);
    // if (msgq_axi_to_net_r == (mqd_t)-1) {
    //   perror("Error ui_init : mq_open(msq_axi_to_net_r)\n");
    //   return -1;
    // }

    // printf ("Configuring axi_to_net memory read buffers attributes\n");
    // if (mq_getattr(msgq_axi_to_net_r, &attr_axi_to_net_r) != 0) {
    //   perror ("Error ui_init : mq_getattr(msg_axi_to_net_r)");
    //   return -1;
    // }

    //-------------------------------------------------------------------------------------
    attr_net_to_axi_w.mq_flags = 0;
    attr_net_to_axi_w.mq_maxmsg = UDP_PKT_SIZE*4;
    attr_net_to_axi_w.mq_msgsize = UDP_PKT_SIZE;
    attr_net_to_axi_w.mq_curmsgs = 0;
    printf ("Configuring net_to_axi memory write buffers \n");
    //msgq_id_w = mq_open(MSGQOBJ_NAME, O_RDWR | O_CREAT | O_EXCL, S_IRWXU | S_IRWXG, &attr_w);
    //EXIST pathname already exists and O_CREAT and O_EXCL were used." I don't use O_EXCL.
    // msgq_net_to_axi_w = mq_open(N2A_MSGQOBJ_NAME, O_RDWR | O_CREAT , S_IRWXU | S_IRWXG, &attr_net_to_axi_w);
    // if (msgq_net_to_axi_w == (mqd_t)-1) {
    //   perror("Error ui_init : mq_open(msq_net_to_axi_w)\n");
    //   return -1;
    // }
    // printf ("Configuring net_to_axi memory read buffers\n");
    // msgq_net_to_axi_r = mq_open(N2A_MSGQOBJ_NAME, O_RDWR);
    // if (msgq_net_to_axi_r == (mqd_t)-1) {
    //   perror("Error ui_init : mq_open(msq_net_to_axi_r)\n");
    //   return -1;
    // }

    // printf ("Configuring net_to_axi memory read buffers attributes\n");
    // if (mq_getattr(msgq_net_to_axi_r, &attr_net_to_axi_r) != 0) {
    //   perror ("Error ui_init : mq_getattr(msg_net_to_axi_r)");
    //   return -1;
    // }
    //-------------------------------------------------------------------------------------

  filter_coefficients coefficients;
  unsigned int filter_coefficients[15] = { 0x00002CB6, 0x0000596C, 0x00002CB6, 0x8097A63A, 0x3F690C9D, \
                                           0x074D9236, 0x00000000, 0xF8B26DCA, 0x9464B81B, 0x3164DB93, \
                                           0x12BEC333, 0xDA82799A, 0x12BEC333, 0x00000000, 0x0AFB0CCC };

  for (int i = 0; i < 15; i++) {
    coefficients.coefficients[i] = filter_coefficients[i];
  }

  printf ("Setting filter 0 coefficients..\n");
  if (set_coefficients (filter_control_base_0, coefficients) != 0) {
    fprintf (stderr, "set_coefficients\n");
  }

  printf ("Setting filter 0 coefficients..\n");
  if (set_coefficients (filter_control_base_1, coefficients) != 0) {
    fprintf (stderr, "set_coefficients\n");
  }

  printf ("Setting default Chan 0 L volume to 30..\n");
	set_volume (volume_control_base_0, 30, CHANNEL_ID_L);
	printf ("Setting default Chan 0 R volume to 30..\n");
	set_volume (volume_control_base_0, 30, CHANNEL_ID_R);
	printf ("Setting default Chan 1 L volume to 30..\n");
	set_volume (volume_control_base_1, 30, CHANNEL_ID_L);
	printf ("Setting default Chan 1 R volume to 30..\n");
	set_volume (volume_control_base_1, 30, CHANNEL_ID_R);
	printf ("Updating params ...\n");
	params.vl_lpbk = 30;
	params.v_global = 4;
	globalVol = 4;
	params.vr_lpbk = 30;
	params.vl_net  = 30;
	params.vr_net  = 30;

	set_sync_off(audio_to_eth_base_0);
	printf("Disable time sync...\n");
	unsigned buf = read_eth_param (full_udp_stack_ip_base_0, ETH_CONTROL_REG_ID);
	write_eth_param(full_udp_stack_ip_base_0, ETH_CONTROL_ID, buf&0xFFFFFFFE);
	printf("Disable tx ...\n");

	printf ("Init done ...\n");
  return 0;
}


void init_gpios(){
	exportfd = open("/sys/class/gpio/export", O_WRONLY);
	if (exportfd < 0)
	{
		printf("Cannot open GPIO to export it\n");
		exit(1);
	}
	write(exportfd, "928", 4);
	write(exportfd, "929", 4);
	write(exportfd, "930", 4);
	write(exportfd, "931", 4);
	write(exportfd, "932", 4);
	write(exportfd, "933", 4);
	write(exportfd, "934", 4);
	write(exportfd, "935", 4);
	close(exportfd);
	printf("GPIO exported successfully\n");
	directionfd = open("/sys/class/gpio/gpio928/direction", O_RDWR);
	write(directionfd, "out", 4);
	close(directionfd);
	directionfd = open("/sys/class/gpio/gpio929/direction", O_RDWR);
	write(directionfd, "out", 4);
	close(directionfd);
	directionfd = open("/sys/class/gpio/gpio930/direction", O_RDWR);
	write(directionfd, "out", 4);
	close(directionfd);
	directionfd = open("/sys/class/gpio/gpio931/direction", O_RDWR);
	write(directionfd, "out", 4);
	close(directionfd);
	directionfd = open("/sys/class/gpio/gpio932/direction", O_RDWR);
	write(directionfd, "out", 4);
	close(directionfd);
	directionfd = open("/sys/class/gpio/gpio933/direction", O_RDWR);
	write(directionfd, "out", 4);
	close(directionfd);
	directionfd = open("/sys/class/gpio/gpio934/direction", O_RDWR);
	write(directionfd, "out", 4);
	close(directionfd);
	directionfd = open("/sys/class/gpio/gpio935/direction", O_RDWR);
	write(directionfd, "out", 4);
	close(directionfd);
	printf("GPIO direction set as output successfully\n");
	GPIO_LED_0 = open("/sys/class/gpio/gpio928/value", O_RDWR);
	GPIO_LED_1 = open("/sys/class/gpio/gpio929/value", O_RDWR);
	GPIO_LED_2 = open("/sys/class/gpio/gpio930/value", O_RDWR);
	GPIO_LED_3 = open("/sys/class/gpio/gpio931/value", O_RDWR);
	GPIO_LED_4 = open("/sys/class/gpio/gpio932/value", O_RDWR);
	GPIO_LED_5 = open("/sys/class/gpio/gpio933/value", O_RDWR);
	GPIO_LED_6 = open("/sys/class/gpio/gpio934/value", O_RDWR);
	GPIO_LED_7 = open("/sys/class/gpio/gpio935/value", O_RDWR);
}

/**
 * =======================================================
 * ui_run starts the user interface
 * @return  : returns 0 upon success
 *            -1 otherwise
 * =======================================================
 */
int ui_run () {
	int r = 0;
	//--------------------------------------------------------------------------------



//  r = pthread_create(&axi_to_net_mq_reader_thread, NULL, axi_to_net_mq_reader, NULL);
//  	if (r != 0)
//  	{
//  		printf("Error - pthread_create() audio_reader_thread return code: %d\n", r);
//  		return -1;
//  	}
//
//r = pthread_create(&axi_to_net_mq_writer_thread, NULL, axi_to_net_mq_writer, NULL);
//	if (r != 0)
//	{
//		printf("Error - pthread_create() network_reader_stream_thread return code: %d\n", r);
//		return -1;
//	}
//
//	r = pthread_create(&net_to_axi_mq_reader_thread, NULL, net_to_axi_mq_reader, NULL);
//	if (r != 0)
//	{
//		printf("Error - pthread_create() network_writer_stream_thread return code: %d\n", r);
//		return -1;
//	}
//
//	r = pthread_create(&net_to_axi_mq_writer_thread, NULL, net_to_axi_mq_writer, NULL);
//	if (r != 0)
//	{
//		printf("Error - pthread_create() audio_to_network_thread return code: %d\n", r);
//		return -1;
//	}

	r= pthread_create (&ui_input_reader_thread, NULL, ui_input_reader, NULL);
	  if (r != 0) {
	    printf ("ui_init : pthread_create(&ui_input_reader_thread) code:%d",r);
	    return -1;
	  }
/*
  if (pthread_create (&ui_draw_thread, NULL, ui_draw, NULL) != 0) {
    perror ("ui_init : pthread_create(&ui_draw_thread)");
    return -1;
  }

  if`dd (pthread_join(ui_draw_thread, NULL) != 0) {
    perror ("(pthread_join(ui_draw_thread");
    return -1;
  }
*/
//  if (pthread_join(loopback_thread, NULL) != 0) {
//    perror ("pthread_join(loopback_thread)");
//    return -1;
//  }

//	if (pthread_join(net_to_axi_mq_writer_thread, NULL) != 0) {
//		perror ("pthread_join(audio_reader_thread)");
//		return -1;
//	  }
//
//	if (pthread_join(net_to_axi_mq_reader_thread, NULL) != 0) {
//		perror ("pthread_join(audio_to_network_thread)");
//		return -1;
//	  }
//
//
//  if (pthread_join(axi_to_net_mq_writer_thread, NULL) != 0) {
//    perror ("pthread_join(axi_to_net_mq_writer_thread)");
//    return -1;
//  }
//
//  if (pthread_join(axi_to_net_mq_reader_thread, NULL) != 0) {
//    perror ("pthread_join(axi_to_net_mq_reader_thread)");
//    return -1;
//  }

  if (pthread_join(ui_input_reader_thread, NULL) != 0) {
    perror ("(pthread_join(ui_input_reader_thread)");
    return -1;
  }

//  if (pthread_join(pmod_thread, NULL) != 0) {
//	    perror ("(pthread_join(pmod_thread)");
//	    return -1;
//  }



  return 0;
}


void update_leds(){
	if (globalVol >= 16)
		write(GPIO_LED_0, "1", 2);
	else
		write(GPIO_LED_0, "0", 2);

	if (globalVol >= 14)
		write(GPIO_LED_1, "1", 2);
	else
		write(GPIO_LED_1, "0", 2);

	if (globalVol >= 12)
		write(GPIO_LED_2, "1", 2);
	else
		write(GPIO_LED_2, "0", 2);

	if (globalVol >= 10)
		write(GPIO_LED_3, "1", 2);
	else
		write(GPIO_LED_3, "0", 2);

	if (globalVol >= 8)
		write(GPIO_LED_4, "1", 2);
	else
		write(GPIO_LED_4, "0", 2);

	if (globalVol >= 6)
		write(GPIO_LED_5, "1", 2);
	else
		write(GPIO_LED_5, "0", 2);

	if (globalVol >= 4)
		write(GPIO_LED_6, "1", 2);
	else
		write(GPIO_LED_6, "0", 2);

	if (globalVol >= 2)
		write(GPIO_LED_7, "1", 2);
	else
		write(GPIO_LED_7, "0", 2);
}

//void *send_audio_function(void *arg)
//{
//	short int buf[512];
//	int fd;
//	int IRQEnable = 1;
//	int i;
//	write(axi_to_audio_params_0.dev_fd, &IRQEnable, sizeof(IRQEnable));
//	printf("audio sample Interrupt Enabled\n");
//	if ((fd = open("/tmp/myfifo", O_RDONLY)) < 1)
//		printf("fifo read open error");
//	else
//		printf("FIFO READ open\n");
//
//	while (1)
//	{
//		read(fd, buf, 1024);
//		for (i = 0; i < 512; i++)//write the data and trigger an int for every write
//		{
//			read(axi_to_audio_params_0.dev_fd, &IRQEnable, sizeof(IRQEnable));
//			IRQEnable = 1;
//			write(axi_to_audio_params_0.dev_fd, &IRQEnable, sizeof(IRQEnable));
////			AXI_TO_AUDIO_REG_0 = (int)buf[i];
//			axi_to_audio_params_0.base_address = (int)buf[i];
//		}
//	}
//}

//void *pmod_function(void *arg)
//{
//	int position = 0;
//	int IRQEnable = 1;
//	int pmodData, i, j;
//	static int prevPmodData = A | B;
//	write(pmod_controller_params_0.dev_fd, &IRQEnable, sizeof(IRQEnable));
//	printf(" Pmod Interrupt Enabled\n");
//
//	while (1)
//	{
//		read(pmod_controller_params_0.dev_fd, &IRQEnable, sizeof(IRQEnable));
////		pmodData = PMOD_REG_3;
//		pmodData = *((unsigned *)(pmod_controller_params_0.base_address + PMOD_INTERFACE_REG3_OFFSET));
//
//		if (pmodData & BUTTON){
////			menuSelect = 1;
//			oled_clear(zedboard_oled_params_0.base_address);
//
//			for (i = 0; i < 4; i++)
//			{
//				if (i == cursorPos){
//					menuBuf[0] = 45;
//				}
//				else{
//					menuBuf[0] = 0;//32;
//				}
//				sprintf(&menuBuf[1], "%s%3d", menuitem[menuPos + i], setting[menuPos + i]);
//				oled_print_message(&menuBuf[0], i, zedboard_oled_params_0.base_address);
//			}
//
//			menuSelect = 0;
//			oled_clear(zedboard_oled_params_0.base_address);
//			for (i = 0; i < 4; i++)
//			{
//				if (i == cursorPos)
//					menuBuf[0] = 45;
//				else
//					menuBuf[0] = 0;//32;
//				sprintf(&menuBuf[1], "%s%3d", menuitem[menuPos + i], setting[menuPos + i]);
//				oled_print_message(&menuBuf[0], i, zedboard_oled_params_0.base_address);
//			}
//
//
//			while (!menuSelect)
//			{
//				if (menuUp)
//				{
//					menuUp = 0;
//					if (setting[menuPos + cursorPos] < settingRange[menuPos + cursorPos])
//						setting[menuPos + cursorPos]++;
//
//					oled_clear(zedboard_oled_params_0.base_address);
//					for (i = 0; i < 4; i++)
//					{
//						if (i == cursorPos)
//							menuBuf[0] = 45;
//						else
//							menuBuf[0] = 0;//32;
//						sprintf(&menuBuf[1], "%s%3d", menuitem[menuPos + i], setting[menuPos + i]);
//						oled_print_message(&menuBuf[0], i, zedboard_oled_params_0.base_address);
//					}
//					//network
////					VOLUME_1_REG_0  = 32 * globalVol * setting[3];
////					VOLUME_1_REG_1  = 32 * globalVol * setting[2];
//					params.vl_net = setting[3];
//					params.vr_net = setting[2];
//					set_volume (volume_control_base_1, params.vl_net*globalVol, CHANNEL_ID_L);
//					set_volume (volume_control_base_1, params.vr_net*globalVol, CHANNEL_ID_R);
//
//
//
//					//FILTER_1_REG_17 = setting[9];
//					set_filter_type (filter_control_base_1, FILTER_HIGH_PASS, setting[9]);
//					params.filter_h_net = setting[9];
////					FILTER_1_REG_18 = setting[8];
//					set_filter_type (filter_control_base_1, FILTER_BAND_PASS, setting[8]);
//					params.filter_b_net = setting[8];
////					FILTER_1_REG_19 = setting[7];
//					set_filter_type (filter_control_base_1, FILTER_LOW_PASS, setting[7]);
//					params.filter_l_net = setting[7];
//
//					//line in
////					VOLUME_0_REG_0  = 32 * globalVol * setting[1];
////					VOLUME_0_REG_1  = 32 * globalVol * setting[0];
//					params.vl_lpbk = setting[1];
//					params.vr_lpbk = setting[0];
//					set_volume (volume_control_base_1, params.vl_lpbk*globalVol, CHANNEL_ID_L);
//					set_volume (volume_control_base_1, params.vr_lpbk*globalVol, CHANNEL_ID_R);
//
////					FILTER_0_REG_17 = setting[6];
//					set_filter_type (filter_control_base_0, FILTER_HIGH_PASS, setting[6]);
//					params.filter_h_lpbk = setting[6];
////					FILTER_0_REG_18 = setting[5];
//					set_filter_type (filter_control_base_0, FILTER_BAND_PASS, setting[5]);
//					params.filter_b_lpbk = setting[5];
////					FILTER_0_REG_19 = setting[4];
//					set_filter_type (filter_control_base_0, FILTER_LOW_PASS, setting[4]);
//					params.filter_l_lpbk = setting[4];
//				}
//				else if (menuDown)
//				{
//					menuDown = 0;
//					if (setting[menuPos + cursorPos] > 0){
//						setting[menuPos + cursorPos]--;
//					}
//					oled_clear(zedboard_oled_params_0.base_address);
//					for (i = 0; i < 4; i++)
//					{
//						if (i == cursorPos)
//							menuBuf[0] = 45;
//						else
//							menuBuf[0] = 0;//32;
//						sprintf(&menuBuf[1], "%s%3d", menuitem[menuPos + i], setting[menuPos + i]);
//						oled_print_message(&menuBuf[0], i, zedboard_oled_params_0.base_address);
//					}
//					//net
////					VOLUME_1_REG_0  = 32 * globalVol * setting[3];
////					VOLUME_1_REG_1  = 32 * globalVol * setting[2];
//					params.vl_net = setting[3];
//					params.vr_net = setting[2];
//					set_volume (volume_control_base_1, params.vl_net*globalVol, CHANNEL_ID_L);
//					set_volume (volume_control_base_1, params.vr_net*globalVol, CHANNEL_ID_R);
//					//FILTER_1_REG_17 = setting[9];
//					set_filter_type (filter_control_base_1, FILTER_HIGH_PASS, setting[9]);
//					params.filter_h_net = setting[9];
////					FILTER_1_REG_18 = setting[8];
//					set_filter_type (filter_control_base_1, FILTER_BAND_PASS, setting[8]);
//					params.filter_b_net = setting[8];
////					FILTER_1_REG_19 = setting[7];
//					set_filter_type (filter_control_base_1, FILTER_LOW_PASS, setting[7]);
//					params.filter_l_net = setting[7];
//
//					//line in
////					VOLUME_0_REG_0  = 32 * globalVol * setting[1];
////					VOLUME_0_REG_1  = 32 * globalVol * setting[0];
//					set_volume (volume_control_base_0, params.vl_lpbk*globalVol, CHANNEL_ID_L);
//					set_volume (volume_control_base_0, params.vr_lpbk*globalVol, CHANNEL_ID_R);
////					FILTER_0_REG_17 = setting[6];
//					set_filter_type (filter_control_base_0, FILTER_HIGH_PASS, setting[6]);
//					params.filter_h_lpbk = setting[6];
////					FILTER_0_REG_18 = setting[5];
//					set_filter_type (filter_control_base_0, FILTER_BAND_PASS, setting[5]);
//					params.filter_b_lpbk = setting[5];
////					FILTER_0_REG_19 = setting[4];
//					set_filter_type (filter_control_base_0, FILTER_LOW_PASS, setting[4]);
//					params.filter_l_lpbk = setting[4];
//				}
//			}
//			menuSelect = 0;
//			oled_clear(zedboard_oled_params_0.base_address);
//			for (i = 0; i < 4; i++)
//			{
//				if (i == cursorPos)
//					menuBuf[0] = 62;
//				else
//					menuBuf[0] = 0;//32;
//				sprintf(&menuBuf[1], "%s%3d", menuitem[menuPos + i], setting[menuPos + i]);
//				oled_print_message(&menuBuf[0], i, zedboard_oled_params_0.base_address);
//			}
//
//
//
//		}
//
//		if (pmodData & SWITCH){
////			volSwitch = 1;
//			if (menuUp)
//			{
//				menuUp = 0;
//				if (globalVol < 16){
//					globalVol++;
//				}
//				for (j = 0; j < 16; j++)
//				{
//					if (globalVol > j){
//						menuBuf[j] = 35;
//					}
//					else{
//						menuBuf[j] = 32;
//					}
//				}
//				oled_clear(zedboard_oled_params_0.base_address);
//				for (i = 0; i < 4; oled_print_message(&menuBuf[0], i++, zedboard_oled_params_0.base_address));
//
//			}
//			else if (menuDown)
//			{
//				menuDown = 0;
//				if (globalVol > 0){
//					globalVol--;
//				}
//
//				for (j = 0; j < 16; j++)
//				{
//					if (globalVol > j)
//						menuBuf[j] = 35;
//					else
//						menuBuf[j] = 32;
//				}
//				oled_clear(zedboard_oled_params_0.base_address);
//				for (i = 0; i < 4; oled_print_message(&menuBuf[0], i++, zedboard_oled_params_0.base_address));
//			}
//
//			params.v_global = globalVol;
//			params.v_global = globalVol;
//
//			set_volume (volume_control_base_0, params.vl_lpbk*globalVol, CHANNEL_ID_L);
//			set_volume (volume_control_base_0, params.vr_lpbk*globalVol, CHANNEL_ID_R);
//
//			set_volume (volume_control_base_1, params.vl_net*globalVol, CHANNEL_ID_L);
//			set_volume (volume_control_base_1, params.vr_net*globalVol, CHANNEL_ID_R);
//		}
//		else{
//			volSwitch = 0;
//		}
//
//		if (pmodData == 0 || pmodData == 4) //A|B
//		{
//			if (prevPmodData & A){
//				menuUp = 1;
//			}
//
//			if (prevPmodData & B){
//				menuDown = 1;
//			}
//		}
//		prevPmodData = pmodData;
//		IRQEnable = 1;
//		write(pmod_controller_params_0.dev_fd, &IRQEnable, sizeof(IRQEnable));
//
//
//
//		update_leds();
//
//
//
//	}
//}

//void *recv_function(void *arg)
//{
//	short int buffer[512];
//	int fd_fifo;
//
//	if ((fd_fifo = open("/tmp/myfifo", O_WRONLY)) < 0)
//		printf("fifo write open error\n");
//	else
//		printf("fifo write open\n");
//
////	if (udp_client_setup("192.168.0.4", 12345)){
////		printf("Connection error\n");
////	}
////	else{
////		printf("Stream connected\n");
////	}
//
//	while (1) //get stream and send to axi_to_audio
//	{
//		udp_client_recv((unsigned int*)&buffer, 1024);
//		write(fd_fifo, buffer, 1024);
//	}
//}


void *button_function(void *arg)
{
	int exportfd, directionfd;
	int IRQEnable = 1;
	int button;
	exportfd = open("/sys/class/gpio/export", O_WRONLY);
	if (exportfd < 0)
	{
		printf("Cannot open GPIO to export it\n");
		exit(1);
	}
	write(exportfd, "992", 4);
	write(exportfd, "993", 4);
	write(exportfd, "994", 4);
	write(exportfd, "995", 4);
	write(exportfd, "996", 4);
	close(exportfd);
	printf("GPIO exported successfully\n");
	directionfd = open("/sys/class/gpio/gpio992/direction", O_RDWR);
	if (directionfd < 0)
	{
		printf("Cannot open GPIO direction it\n");
		exit(1);
	}
	write(directionfd, "out", 4);
	close(directionfd);
	directionfd = open("/sys/class/gpio/gpio993/direction", O_RDWR);
	if (directionfd < 0)
	{
		printf("Cannot open GPIO direction it\n");
		exit(1);
	}
	write(directionfd, "out", 4);
	close(directionfd);
	directionfd = open("/sys/class/gpio/gpio994/direction", O_RDWR);
	if (directionfd < 0)
	{
		printf("Cannot open GPIO direction it\n");
		exit(1);
	}
	write(directionfd, "out", 4);
	close(directionfd);
	directionfd = open("/sys/class/gpio/gpio995/direction", O_RDWR);
	if (directionfd < 0)
	{
		printf("Cannot open GPIO direction it\n");
		exit(1);
	}
	write(directionfd, "out", 4);
	close(directionfd);
	directionfd = open("/sys/class/gpio/gpio996/direction", O_RDWR);
	if (directionfd < 0)
	{
		printf("Cannot open GPIO direction it\n");
		exit(1);
	}
	write(directionfd, "out", 4);
	close(directionfd);
	printf("GPIO direction set as output successfully\n");
	GPIO_BTN_0 = open("/sys/class/gpio/gpio992/value", O_RDWR);
	if (GPIO_BTN_0 < 0)
	{
		printf("Cannot open GPIO value\n");
		exit(1);
	}
	GPIO_BTN_1 = open("/sys/class/gpio/gpio993/value", O_RDWR);
	if (GPIO_BTN_1 < 0)
	{
		printf("Cannot open GPIO value\n");
		exit(1);
	}
	GPIO_BTN_2 = open("/sys/class/gpio/gpio994/value", O_RDWR);
	if (GPIO_BTN_2 < 0)
	{
		printf("Cannot open GPIO value\n");
		exit(1);
	}
	GPIO_BTN_3 = open("/sys/class/gpio/gpio995/value", O_RDWR);
	if (GPIO_BTN_3 < 0)
	{
		printf("Cannot open GPIO value\n");
		exit(1);
	}
	GPIO_BTN_4 = open("/sys/class/gpio/gpio996/value", O_RDWR);
	if (GPIO_BTN_4 < 0)
	{
		printf("Cannot open GPIO value\n");
		exit(1);
	}

	int fd = open ("/dev/gpiochip0", O_RDWR);
	if (fd < 1) { printf("/dev/gpiochip0 error\n"); }

	while (1)
	{
		IRQEnable = 1;
		write(fd, &IRQEnable, sizeof(IRQEnable));
		read(fd, &IRQEnable, sizeof(IRQEnable));
		printf("Got gpio btn interrupt\n");
	}
}


/*
Function : extractIpAddress
Arguments :
1) sourceString - String pointer that contains ip address
2) ipAddress - Target variable short type array pointer that will store ip address octets
*/
void extractIpAddress(unsigned char *sourceString,unsigned char *ipAddress)
{
    unsigned short len=0;
    unsigned char  oct[4]={0},cnt=0,cnt1=0,i,buf[5];

    len=strlen(sourceString);
    for(i=0;i<len;i++)
    {
        if(sourceString[i]!='.'){
            buf[cnt++] =sourceString[i];
        }
        if(sourceString[i]=='.' || i==len-1){
            buf[cnt]='\0';
            cnt=0;
            oct[cnt1++]=atoi(buf);
        }
    }
    ipAddress[0]=oct[0];
    ipAddress[1]=oct[1];
    ipAddress[2]=oct[2];
    ipAddress[3]=oct[3];
}


char * print_ip(unsigned int ip, unsigned char * bytes)
{
//    unsigned char bytes[4];
    bytes[0] = ip & 0xFF;
    bytes[1] = (ip >> 8) & 0xFF;
    bytes[2] = (ip >> 16) & 0xFF;
    bytes[3] = (ip >> 24) & 0xFF;
    bytes[4] = 0;
    printf("%d.%d.%d.%d\n", bytes[3], bytes[2], bytes[1], bytes[0]);
    sprintf(bytes, "%d.%d.%d.%d\0", bytes[3], bytes[2], bytes[1], bytes[0]);
    return bytes;
}


char * print_mac(unsigned long ip, unsigned char * bytes)
{

    bytes[0] = ip & 0xFF;
    bytes[1] = (ip >> 8) & 0xFF;
    bytes[2] = (ip >> 16) & 0xFF;
    bytes[3] = (ip >> 24) & 0xFF;
    bytes[4] = (ip >> 32) & 0xFF;
    bytes[5] = (ip >> 40) & 0xFF;
    bytes[6] = 0;
    printf("%d.%d.%d.%d\n", bytes[5], bytes[4], bytes[3], bytes[2], bytes[1], bytes[0]);
    return bytes;
}


/*
Function : extractIpAddress
Arguments :
1) sourceString - String pointer that contains ip address
2) macAddress - Target variable short type array pointer that will store ip address octets
*/
void extractMacAddress(unsigned char *sourceString,unsigned char *macAddress)
{
    unsigned short len=0;
    unsigned char  oct[6]={0},cnt=0,cnt1=0,i,buf[5];

    len=strlen(sourceString);
    for(i=0;i<len;i++)
    {
        if(sourceString[i]!='.'){
            buf[cnt++] =sourceString[i];
        }
        if(sourceString[i]=='.' || i==len-1){
            buf[cnt]='\0';
            cnt=0;
            //oct[cnt1++]=atoi(buf);
            oct[cnt1++]=(unsigned char)strtol(buf, NULL, 16);
        }
    }
    macAddress[0]=oct[0];
    macAddress[1]=oct[1];
    macAddress[2]=oct[2];
    macAddress[3]=oct[3];
    macAddress[4]=oct[4];
    macAddress[5]=oct[5];
}


/**
 * =======================================================
 * ui_exit will do clean up routines and
 * do safe termination the application
 * =======================================================
 */
void ui_exit () {
  printf ("Exiting from Audio Mixer\n");
  sleep (2);
  unmap_device (zedboard_oled_base_0);
//  unmap_device (axi_to_audio_base_0);
  unmap_device (eth_packet_sequencer_base_0);
//  unmap_device (packet_time_enforcer_base_0);
  unmap_device (full_udp_stack_ip_base_0);
  unmap_device (eth_to_audio_base_0);
  unmap_device (audio_to_eth_base_0);
  unmap_device (time_sync_base_0);

  unmap_device (adau1761_base_0);

  unmap_device (pmod_controller_base_0);
  unmap_device (filter_control_base_0);
  unmap_device (filter_control_base_1);
  unmap_device (volume_control_base_0);
  unmap_device (volume_control_base_1);
}
