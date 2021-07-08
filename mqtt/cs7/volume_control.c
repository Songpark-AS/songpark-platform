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
  if (volume_base == NULL) {
    return -1;
  }

  if (channel == CHANNEL_ID_L) {
    WriteReg (volume_base, VOLUME_CONTROL_REG0_OFFSET, (gain));
  }
  else if (channel == CHANNEL_ID_R) {
    WriteReg (volume_base, VOLUME_CONTROL_REG1_OFFSET, (gain));
  }else if (channel == 2) {
	    WriteReg (volume_base, VOLUME_CONTROL_REG2_OFFSET, (gain));
  }
  else if (channel == 3) {
		    WriteReg (volume_base, VOLUME_CONTROL_REG3_OFFSET, (gain));
  }
  else {
    return -1;
  }

  return 0;
}
