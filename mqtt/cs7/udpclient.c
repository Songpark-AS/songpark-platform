/*
 * Simple UDP broadcast client.
 * Author:  Thanx
 *   Organization:  Cantavi
 */

#include <stdio.h>
#include <stdlib.h>
#include <sys/socket.h>		// For socket related functions
#include <netinet/in.h>		// For address struct
#include <arpa/inet.h>
#include <string.h>			// For memset


struct sockaddr_in receiving_address;
//struct sockaddr_out sending_address;
int client_socket;
int addr_size;


int get_client_socket(){
	return client_socket;
}

/*
 * Sets up UDP broadcast client.
 *
 * @param:
 * char *pp_address: Address to use for receiving data
 * int pp_port: Port to use for data broadcast
 *
 * @return:
 * Error code: 0 - normal exit, 1 - error
 */

int udp_client_setup(char *pp_address, int pp_port){

	/* Create UDP socket */
//	client_socket = socket(PF_INET, SOCK_DGRAM, 0);
//	client_socket = socket(PF_INET, SOCK_DGRAM, IPPROTO_UDP);

	// Creating socket file descriptor
	if ( (client_socket = socket(AF_INET, SOCK_DGRAM, 0)) < 0 ) {
		perror("socket creation failed exiting");
		exit(-1);
	}else{
		printf("UDP Client Socket open for send\n");
	}

//	int pp_enabled = 1;

	/* Enable broadcast */
//	if (setsockopt(client_socket, SOL_SOCKET, SO_BROADCAST, (void *) &pp_enabled,sizeof(pp_enabled)) < 0){
//		perror ("Error while enabling broadcast");
//		return 1;
//	}

	/* Configure settings in address struct */
	receiving_address.sin_family = AF_INET;
	receiving_address.sin_port = htons(pp_port);
	receiving_address.sin_addr.s_addr = inet_addr(pp_address);
	memset(receiving_address.sin_zero, '\0', sizeof receiving_address.sin_zero);
//
//	/* Connect to the server  */
//	bind(client_socket, (struct sockaddr *) &receiving_address, sizeof(receiving_address));
//
//	/* Initialize size variable to be used later on */
	addr_size = sizeof(struct sockaddr);

	return client_socket;
}

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

int udp_client_recv(char *buffer,int buffer_size ){
    return recvfrom(client_socket,buffer,buffer_size,0,(struct sockaddr *)&receiving_address,addr_size);

}


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

int udp_client_send(char *buffer,int buffer_size ){
//    if((sendto(client_socket,buffer,buffer_size,0,(struct sockaddr *)&receiving_address,addr_size)) != -1 ){
//        return 0;
//    }
//    else{
//        return 1;
//    }
	return sendto(client_socket,buffer,buffer_size,0,(struct sockaddr *)&receiving_address,addr_size);
}
