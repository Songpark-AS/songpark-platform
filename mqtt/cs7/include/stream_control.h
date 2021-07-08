/**
 * =====================================================================================
 *
 *       Filename:  stream_control.h
 *
 *    Description:  audio stream control functions and macros
 *
 *         Author:  Thanx
 *   Organization:  Cantavi
 *
 *
 * =====================================================================================
 */

#ifndef STREAM_CONTROL_H
#define STREAM_CONTROL_H

#include "reg_io.h"

#define BYTE_TO_BINARY_PATTERN "%c%c%c%c%c%c%c%c"
#define BYTE_TO_BINARY_NIB_PATTERN "%c%c%c%c %c%c%c%c"
#define BYTE_TO_BINARY(byte)  \
  (byte & 0x80 ? '1' : '0'), \
  (byte & 0x40 ? '1' : '0'), \
  (byte & 0x20 ? '1' : '0'), \
  (byte & 0x10 ? '1' : '0'), \
  (byte & 0x08 ? '1' : '0'), \
  (byte & 0x04 ? '1' : '0'), \
  (byte & 0x02 ? '1' : '0'), \
  (byte & 0x01 ? '1' : '0')

/**
 * =======================================================
 * read_stream reads 4 bytes unsigned data
 * from the specified channel
 * @param   : stream_base  : base address of the stream
 * @param   : channel      : channel ID
 * @return  : returns 4 bytes unsigned data
 * =======================================================
 */
unsigned read_stream (void *stream_base, CHANNEL_ID channel);

/**
 * =======================================================
 * write_stream writes 4 bytes unsigned data
 * to the spicified channel
 * @param  stream_base  : base address of the stream
 * @param  channel      : channel ID
 * @param  data         : 4 bytes unsigned data
 * @return  : returns 0 upon success
 *            -1 otherwise
 * =======================================================
 */
int write_stream (void *stream_base, CHANNEL_ID channel, unsigned data);


/**
 * =======================================================
 * read_fifo_staus reads 4 bytes unsigned data
 * from the specified channel
 * @param   : stream_base  : base address of the stream
 * @param   : fifo      : fifo ID
 * @return  : returns 4 bytes unsigned data
 * =======================================================
 */
unsigned read_fifo_status (void *stream_base, FIFO_ID fifo);


void set_stream_plen(void *stream_base, unsigned data);
unsigned int get_stream_plen(void *stream_base);

void set_stream_porder(void *stream_base, unsigned data);
void set_stream_pkt_wait(void *stream_base, unsigned long data);
void set_stream_pkt_send_delay(void *stream_base, unsigned long data);
void set_stream_rx_buf_lim(void *stream_base, unsigned long data);
void set_stream_rx_buf_lim2(void *stream_base, unsigned long data);
void set_stream_rx_buf_dlim(void *stream_base, unsigned int data);
void set_plc_replace_delay(void *stream_base, unsigned int data);
void set_stream_rx_pkt_size(void *stream_base, unsigned long data);
void reset_pkt_seq(void *stream_base);
void set_stream_pkt_wait_disable(void *stream_base);
void set_stream_pkt_wait_enable(void *stream_base);

void disble_pkt_seq(void *stream_base);
void enable_pkt_seq(void *stream_base);

unsigned int get_stream_porder(void *stream_base);
unsigned int get_pkts_dropped(void *stream_base);
unsigned int get_pkts_replaced(void *stream_base);
unsigned int get_seq_gs(void *stream_base);
unsigned int get_seq_pkts_rx(void *stream_base);
unsigned int get_seq_pkts_tx(void *stream_base);
unsigned long get_pkts_drop_spkt_limit(void *stream_base);
unsigned long get_pkts_drop_spkt_locked_limit(void *stream_base);
unsigned long get_pkts_adrop_spkt_limit(void *stream_base);
unsigned int get_pkts_ptrx_lock(void *stream_base);
unsigned int set_pkts_ptrx_lock(void *stream_base, unsigned int d);
unsigned int get_pkts_pt_disable(void *stream_base);

unsigned int set_pkts_pt_accum(void *stream_base, unsigned int d1);
unsigned int get_pkts_pt_accum(void *stream_base);

//void reset_fifos(void *stream_base, FIFO_ID fifo, unsigned data);
void reset_adc_fifos(void *stream_base);
void reset_dac_fifos(void *stream_base);
void set_test_mode_on(void *stream_base);
void set_test_mode_off(void *stream_base);
void set_rx_hdr_strip(void *stream_base);
void set_xtest_mode_on(void *stream_base);
void set_xtest_mode(void *stream_base, unsigned mode);
void clear_rx_hdr_strip(void *stream_base);
void set_rx_hdr_strip(void *stream_base);

void set_test_mode(void *stream_base, unsigned mode);

void set_pcnt_mode_on(void *stream_base);
void set_pcnt_mode_off(void *stream_base);

void set_tx_hdr_length(void *stream_base, unsigned data);
void set_stream_send_on(void *stream_base);
void set_stream_send_off(void *stream_base);

void set_sync_on(void *stream_base);
void set_sync_off(void *stream_base);
void set_xtest_mode_off(void *stream_base);

void write_audio_to_eth_param(void *audio_to_eth_base, AUDIO_TO_ETH_REG_ID param, unsigned data);
void write_eth_param(void *eth_base, ETH_PARAM_ID param, unsigned data);


//void write_eth_mac(void *eth_base, ETH_PARAM_ID param, unsigned long data);
void write_eth_mac(void *eth_base, ETH_PARAM_ID param, unsigned char * mac);


//unsigned long read_eth_mac(void *eth_base, ETH_PARAM_ID param);
unsigned long read_eth_mac(void *eth_base, ETH_PARAM_ID param, char* ip);


unsigned read_eth_param (void *stream_base, ETH_REG_ID param);

void set_stream_blen(void *stream_base, unsigned data);
unsigned int get_stream_blen(void *stream_base);
unsigned int get_stream_readout_delay(void *stream_base);
unsigned int get_instant_playout_time(void *stream_base);
void set_playout_delay(void *stream_base, unsigned data);
void set_loop_limit(void *stream_base, unsigned int data);
unsigned int get_instant_playout_delay(void *stream_base);
unsigned int get_instant_playout_writes(void *stream_base);
unsigned int get_instant_playout_reads(void *stream_base);
unsigned int get_stream_pktbuild_delay(void *stream_base);
unsigned int get_stream_net_delay(void *stream_base);
unsigned int set_stream_net_delay(void *stream_base, unsigned int d1);
unsigned int set_stream_pktbuild_delay(void *stream_base, unsigned int d1);
void set_diff_pkt_to_pkt_delay_limit(void *stream_base, unsigned long data);
void set_spkt_to_pkt_delay_limit(void *stream_base, unsigned long data);
void set_spkt_to_pkt_locked_delay_limit(void *stream_base, unsigned long data);
unsigned int set_pkts_pt_diff_status(void *stream_base, unsigned int d1);
unsigned int get_pkts_pt_diff_status(void *stream_base);
unsigned long get_diff_pkt_to_pkt_delay_limit(void *stream_base);
unsigned long get_diff_pkt_to_pkt_delay(void *stream_base);
void set_diff_pkt_to_pkt_delay_limit(void *stream_base, unsigned long data);

unsigned long get_in_exec_state(void *stream_base);
unsigned long get_out_exec_state(void *stream_base);

unsigned long get_stream_accum_pkt_delay(void *stream_base);
unsigned long get_stream_spkt_delay(void *stream_base);


void set_accum_pkt_to_pkt_delay_limit(void *stream_base, unsigned long data);
void set_accum_pkt_to_pkt_delay_limit(void *stream_base, unsigned long data);


void reset_pkt_time_enf(void *stream_base);
void reset_pkt_time_enf_rx(void *stream_base);
void pkt_time_enf_disable(void *stream_base);
void pkt_time_enf_enable(void *stream_base);
void pkt_to_pkt_locked_drop_enable(void *stream_base);

void pkt_to_pkt_locked_drop_disable(void *stream_base);



#endif //STREAM_CONTROL
