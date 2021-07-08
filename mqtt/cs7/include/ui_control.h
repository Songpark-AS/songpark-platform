/*
 * =====================================================================================
 *
 *       Filename:  ui_control.h
 *
 *    Description:  User interface functions
 *
 *         Author:  Thanx
 *   Organization:  Cantavi
 *
 * =====================================================================================
 */

#ifndef UI_CONTROL_H
#define UI_CONTROL_H
#include <pthread.h>
#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <termios.h>
#include <string.h>
#include <mqueue.h>
#include "reg_io.h"
#include "stream_control.h"
#include "filter_control.h"
#include "volume_control.h"
#include "udpclient.h"


#define N2A_MSGQOBJ_NAME    "/net_to_axi_stream" // name of the fifo
#define A2N_MSGQOBJ_NAME    "/axi_to_net_stream" // name of the fifo

/**
 * =======================================================
 * Macro to set the status of the current command
 * @param   : status_msg  : status message
 * @return  : none
 * =======================================================
 */
#define SET_STATUS(status_msg) sprintf(params.status, status_msg)



/**
 * =======================================================
 * Device base address pointers
 * =======================================================
 */
void *zedboard_oled_base_0;
void *pmod_controller_base_0;
void *filter_control_base_0;
void *filter_control_base_1;
void *volume_control_base_0;
void *volume_control_base_1;
void *eth_to_audio_base_0;
void *audio_to_eth_base_0;
void *full_udp_stack_ip_base_0;
void *eth_packet_sequencer_base_0;
void *packet_time_enforcer_base_0;
void *adau1761_base_0;
void *time_sync_base_0;


dev_param zedboard_oled_params_0;
dev_param pmod_controller_params_0;
dev_param filter_control_params_0;
dev_param filter_control_params_1;
dev_param volume_control_params_0;
dev_param volume_control_params_1;

dev_param eth_to_audio_params_0;
dev_param audio_to_eth_params_0;
dev_param eth_packet_sequencer_params_0;
dev_param full_udp_stack_ip_params_0;
dev_param packet_time_enforcer_params_0;
dev_param adau1761_params_0;
dev_param time_sync_params_0;



int exportfd, directionfd;



#define MENULENGTH 29
#define A 1
#define B 2
#define SWITCH 4
#define BUTTON 8

int GPIO_BTN_0;
int GPIO_BTN_1;
int GPIO_BTN_2;
int GPIO_BTN_3;
int GPIO_BTN_4;

int GPIO_LED_0;
int GPIO_LED_1;
int GPIO_LED_2;
int GPIO_LED_3;
int GPIO_LED_4;
int GPIO_LED_5;
int GPIO_LED_6;
int GPIO_LED_7;

pthread_t axi_to_net_mq_reader_thread;
pthread_t axi_to_net_mq_writer_thread; // thread to write network audio data to fifo
pthread_t net_to_axi_mq_reader_thread; // thread to write fifo audio data to axi audio
pthread_t net_to_axi_mq_writer_thread;
//pthread_t loopback_thread; // thread to loop back audio in to out through axi
pthread_t ui_input_reader_thread; //main ui input reading thread
pthread_t ui_draw_thread; // ui draw thread




pthread_t pmod_thread;
//pthread_t recv_thread;
pthread_t button_thread;

typedef struct _server_addr {
	char ip[64];
	int port;
}server_addr;

/**
 * =======================================================
 * structure to hold ui parameters
 * =======================================================
 */
typedef struct _ui_parameters {
	int v_global;
    int vl_lpbk;
    int vl_net;
    int vr_lpbk;
    int vr_net;
    char filter_b_lpbk;
    char filter_b_net;
    char filter_l_lpbk;
    char filter_l_net;
    char filter_h_lpbk;
    char filter_h_net;
    char status[64];
}ui_parameters;

ui_parameters params; //shared structure variable to ui_parameters structure

server_addr saddr;

char local_ip[64];
char local_mac[64];
char dest_ip[64];
char mask_ip[64];
char gateway_ip[64];
unsigned dest_port;
unsigned sync_port;


int quit_flag; // flag to control the threads

mqd_t msgq_axi_to_net_r; // reader fifo descripter
mqd_t msgq_axi_to_net_w; // writer fifo descripter
struct mq_attr attr_axi_to_net_r; // reader fifo attributes
struct mq_attr attr_axi_to_net_w; // writer fifo attributes



mqd_t msgq_net_to_axi_r; // reader fifo descripter
mqd_t msgq_net_to_axi_w; // writer fifo descripter
struct mq_attr attr_net_to_axi_r; // reader fifo attributes
struct mq_attr attr_net_to_axi_w; // writer fifo attributes

/**
 * =======================================================
 * read_raw function reads raw input from standard input
 * @return  : returns the charecter read from stdin
 * =======================================================
 */
int read_raw();

/**
 * =======================================================
 * reads audio data from axi and writes it into axi
 * @param   : data  : thread specific data
 * @return  : returns NULL
 * =======================================================
 */
void *loopback (void *data);

/**
 * =======================================================
 * reads audio data from network and writes it into axi
 * @param   : data  : thread specific data
 * @return  : returns NULL
 * =======================================================
 */
void *network_reader_stream (void *data);

/**
 * =======================================================
 * reads audio data from network and writes it into axi
 * @param   : data  : thread specific data
 * @return  : returns NULL
 * =======================================================
 */
void *network_writer_stream (void *data);

/**
 * =======================================================
 * read and parse user input
 * @param   : data  : thread specific data
 * @return  : returns NULL
 * =======================================================
 */
void *ui_input_reader (void *data);

/**
 * =======================================================
 * draw the user interface
 * @param   : data  : thread specific data
 * @return  : returns NULL
 * =======================================================
 */
 void ui_draw ();

/**
 * =======================================================
 * ui_init initialize the user interface
 * @return  : returns 0 upon success
 *            -1 otherwise
 * =======================================================
 */
int ui_init (int argc, char *argv[]);

void init_gpios();

/**
 * =======================================================
 * ui_run starts the user interface
 * @return  : returns 0 upon success
 *            -1 otherwise
 * =======================================================
 */
int ui_run ();

/**
 * =======================================================
 * ui_exit will do clean up routines and
 * do safe termination the application
 * =======================================================
 */
void ui_exit ();
#endif //UI_CONTROL_H
