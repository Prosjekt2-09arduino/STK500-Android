package no.group09.stk500_v1;

/**
 *  Copyright 2013 UbiCollab
 *  
 *  This file is part of STK500ForJava.
 *
 *	STK500ForJava is free software: you can redistribute it and/or modify
 *	it under the terms of the GNU Lesser General Public License as published by
 *	the Free Software Foundation, either version 3 of the License, or
 *	(at your option) any later version.
 *
 *	STK500ForJava is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU Lesser General Public License for more details.
 *
 *	You should have received a copy of the GNU Lesser General Public License
 *	along with STK500ForJava.  If not, see <http://www.gnu.org/licenses/>.
 */
/**
 * STK500 constants used by the Optiboot boot loader and our protocol
 */
public class ConstantsStk500v1 {
	public static final byte STK_OK              = 0x10;
	public static final byte STK_FAILED          = 0x11;  // Not used
	public static final byte STK_UNKNOWN         = 0x12;  // Not used
	public static final byte STK_NODEVICE        = 0x13;  // Not used
	public static final byte STK_INSYNC          = 0x14;  // ' '
	public static final byte STK_NOSYNC          = 0x15;  // Not used
	public static final byte ADC_CHANNEL_ERROR   = 0x16;  // Not used
	public static final byte ADC_MEASURE_OK      = 0x17;  // Not used
	public static final byte PWM_CHANNEL_ERROR   = 0x18;  // Not used
	public static final byte PWM_ADJUST_OK       = 0x19;  // Not used
	public static final byte CRC_EOP             = 0x20;  // 'SPACE'
	public static final byte STK_GET_SYNC        = 0x30;  // '0'
	public static final byte STK_GET_SIGN_ON     = 0x31;  // '1'
	public static final byte STK_SET_PARAMETER   = 0x40;  // '@'
	public static final byte STK_GET_PARAMETER   = 0x41;  // 'A'
	public static final byte STK_SET_DEVICE      = 0x42;  // 'B'
	public static final byte STK_SET_DEVICE_EXT  = 0x45;  // 'E'
	public static final byte STK_ENTER_PROGMODE  = 0x50;  // 'P'
	public static final byte STK_LEAVE_PROGMODE  = 0x51;  // 'Q'
	public static final byte STK_CHIP_ERASE      = 0x52;  // 'R'
	public static final byte STK_CHECK_AUTOINC   = 0x53;  // 'S'
	public static final byte STK_LOAD_ADDRESS    = 0x55;  // 'U'
	public static final byte STK_UNIVERSAL       = 0x56;  // 'V'
	public static final byte STK_PROG_FLASH      = 0x60;  // '`'
	public static final byte STK_PROG_DATA       = 0x61;  // 'a'
	public static final byte STK_PROG_FUSE       = 0x62;  // 'b'
	public static final byte STK_PROG_LOCK       = 0x63;  // 'c'
	public static final byte STK_PROG_PAGE       = 0x64;  // 'd'
	public static final byte STK_PROG_FUSE_EXT   = 0x65;  // 'e'
	public static final byte STK_READ_FLASH      = 0x70;  // 'p'
	public static final byte STK_READ_DATA       = 0x71;  // 'q'
	public static final byte STK_READ_FUSE       = 0x72;  // 'r'
	public static final byte STK_READ_LOCK       = 0x73;  // 's'
	public static final byte STK_READ_PAGE       = 0x74;  // 't'
	public static final byte STK_READ_SIGN       = 0x75;  // 'u'
	public static final byte STK_READ_OSCCAL     = 0x76;  // 'v'
	public static final byte STK_READ_FUSE_EXT   = 0x77;  // 'w'
	public static final byte STK_READ_OSCCAL_EXT = 0x78;  // 'x'
}
