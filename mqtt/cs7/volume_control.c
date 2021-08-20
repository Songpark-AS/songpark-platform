/**
 * =====================================================================================
 *
 *       Filename:  volume_control.c
 *
 *    Description:  Volume control helper macros and functions
 *
 *         Author:  Thanx
 *   Organization:  Cantavi
 *
 * =====================================================================================
 */

#include "include/volume_control.h"

/**
 * =======================================================
 * set_volume function sets the volume gain value
 * to the volume control IP
 * @param   : volume_base : volume control base address
 * @param   : gain        : gain value
 * @param   : channel     : channel ID
 * @return  : returns 0 upon success
 *            -1 otherwise
 * =======================================================
 */
int set_volume (void *volume_base, unsigned int gain, CHANNEL_ID channel) {
  return 0;
}
