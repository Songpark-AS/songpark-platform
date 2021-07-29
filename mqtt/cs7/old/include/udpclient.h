/*
 * Header file for the simple UDP broadcast client.
 *         Author:  Thanx
 *   Organization:  Cantavi
 */


#ifndef UDPCLIENT_H
#define UDPCLIENT_H



/*
 * Sets up UDP broadcast client.
 *
 * @param:
 * char *broadcast_address: Address to use for receiving data
 * int broadcast_port: Port to use for data broadcast
 *
 * @return:
 * Error code: 0 - normal exit, 1 - error
 */

int udp_client_setup(char *broadcast_address, int broadcast_port);


/*
 * Receive data over UDP.
 *
 * @param:
 * unsigned *buffer: Pointer to the data to the buffer where the received data gets stored
 * int buffer_size: Number of bytes to receive
 *
 * @return:
 * Error code: 0 - normal exit, 1 - error
 */

int udp_client_recv(unsigned *buffer,int buffer_size );


/*
 * Send data over UDP.
 *
 * @param:
 * unsigned *buffer: Pointer to the data to the buffer where the received data gets stored
 * int buffer_size: Number of bytes to receive
 *
 * @return:
 * Error code: 0 - normal exit, 1 - error
 */
int udp_client_send(unsigned *buffer,int buffer_size );

#endif
