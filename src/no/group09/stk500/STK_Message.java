package no.group09.stk500;

/**
 * User: NinaMargrethe
 * Date: 3/7/13
 */

public enum STK_Message {

// *****************[ STK message constants ]***************************

    MESSAGE_START( (byte) 0x1B ),       //= ESC = 27 decimal
    TOKEN( (byte) 0x0E ),

// *****************[ STK general command constants ]**************************

    CMD_SIGN_ON( (byte) 0x01 ),
    CMD_SET_PARAMETER( (byte) 0x02 ),
    CMD_GET_PARAMETER( (byte) 0x03 ),
    CMD_SET_DEVICE_PARAMETERS( (byte) 0x04 ),
    CMD_OSCCAL( (byte) 0x05 ),
    CMD_LOAD_ADDRESS( (byte) 0x06 ),
    CMD_FIRMWARE_UPGRAD( (byte) 0x07 ),
    CMD_CHECK_TARGET_CON( (byte) 0x0E ),
    CMD_LOAD_EC_ID_TABLE( (byte) 0x0F ),


// *****************[ STK ISP command constants ]******************************

    CMD_ENTER_PROGMODE_ISP( (byte) 0x10 ),
    CMD_LEAVE_PROGMODE_ISP( (byte) 0x11 ),
    CMD_CHIP_ERASE_ISP( (byte) 0x12 ),
    CMD_PROGRAM_FLASH_ISP( (byte) 0x13 ),
    CMD_READ_FLASH_ISP( (byte) 0x14 ),
    CMD_PROGRAM_EEPROM_ISP( (byte) 0x15 ),
    CMD_READ_EEPROM_ISP( (byte) 0x16 ),
    CMD_PROGRAM_FUSE_ISP( (byte) 0x17 ),
    CMD_READ_FUSE_ISP( (byte) 0x18 ),
    CMD_PROGRAM_LOCK_ISP( (byte) 0x19 ),
    CMD_READ_LOCK_ISP( (byte) 0x1A ),
    CMD_READ_SIGNATURE_ISP( (byte) 0x1B ),
    CMD_READ_OSCCAL_ISP( (byte) 0x1C ),
    CMD_SPI_MULTI( (byte) 0x1D ),

// *****************[ STK PP command constants ]*******************************

    CMD_ENTER_PROGMODE_PP( (byte) 0x20 ),
    CMD_LEAVE_PROGMODE_PP( (byte) 0x21 ),
    CMD_CHIP_ERASE_PP( (byte) 0x22 ),
    CMD_PROGRAM_FLASH_PP( (byte) 0x23 ),
    CMD_READ_FLASH_PP( (byte) 0x24 ),
    CMD_PROGRAM_EEPROM_PP( (byte) 0x25 ),
    CMD_READ_EEPROM_PP( (byte) 0x26 ),
    CMD_PROGRAM_FUSE_PP( (byte) 0x27 ),
    CMD_READ_FUSE_PP( (byte) 0x28 ),
    CMD_PROGRAM_LOCK_PP( (byte) 0x29 ),
    CMD_READ_LOCK_PP( (byte) 0x2A ),
    CMD_READ_SIGNATURE_PP( (byte) 0x2B ),
    CMD_READ_OSCCAL_PP( (byte) 0x2C ),

    CMD_SET_CONTROL_STACK( (byte) 0x2D ),

// *****************[ STK HVSP command constants ]*****************************

    CMD_ENTER_PROGMODE_HVSP( (byte) 0x30 ),
    CMD_LEAVE_PROGMODE_HVSP( (byte) 0x31 ),
    CMD_CHIP_ERASE_HVSP( (byte) 0x32 ),
    CMD_PROGRAM_FLASH_HVSP( (byte) 0x33 ),
    CMD_READ_FLASH_HVSP( (byte) 0x34 ),
    CMD_PROGRAM_EEPROM_HVSP( (byte) 0x35 ),
    CMD_READ_EEPROM_HVSP( (byte) 0x36 ),
    CMD_PROGRAM_FUSE_HVSP( (byte) 0x37 ),
    CMD_READ_FUSE_HVSP( (byte) 0x38 ),
    CMD_PROGRAM_LOCK_HVSP( (byte) 0x39 ),
    CMD_READ_LOCK_HVSP( (byte) 0x3A ),
    CMD_READ_SIGNATURE_HVSP( (byte) 0x3B ),
    CMD_READ_OSCCAL_HVSP( (byte) 0x3C ),
    // These two are redefined since 0x30/0x31 collide
// with the STK600 bootloader.
    CMD_ENTER_PROGMODE_HVSP_STK600( (byte) 0x3D ),
    CMD_LEAVE_PROGMODE_HVSP_STK600( (byte) 0x3E ),

// *** XPROG command constants ***

    CMD_XPROG( (byte) 0x50 ),
    CMD_XPROG_SETMODE( (byte) 0x51 ),


// *** AVR32 JTAG Programming command ***

    CMD_JTAG_AVR32( (byte) 0x80 ),
    CMD_ENTER_PROGMODE_JTAG_AVR32( (byte) 0x81 ),
    CMD_LEAVE_PROGMODE_JTAG_AVR32( (byte) 0x82 ),


// *** AVR JTAG Programming command ***

    CMD_JTAG_AVR( (byte) 0x90 ),

// *****************[ STK test command constants ]***************************

    CMD_ENTER_TESTMODE( (byte) 0x60 ),
    CMD_LEAVE_TESTMODE( (byte) 0x61 ),
    CMD_CHIP_WRITE( (byte) 0x62 ),
    CMD_PROGRAM_FLASH_PARTIAL( (byte) 0x63 ),
    CMD_PROGRAM_EEPROM_PARTIAL( (byte) 0x64 ),
    CMD_PROGRAM_SIGNATURE_ROW( (byte) 0x65 ),
    CMD_READ_FLASH_MARGIN( (byte) 0x66 ),
    CMD_READ_EEPROM_MARGIN( (byte) 0x67 ),
    CMD_READ_SIGNATURE_ROW_MARGIN( (byte) 0x68 ),
    CMD_PROGRAM_TEST_FUSE( (byte) 0x69 ),
    CMD_READ_TEST_FUSE( (byte) 0x6A ),
    CMD_PROGRAM_HIDDEN_FUSE_LOW( (byte) 0x6B ),
    CMD_READ_HIDDEN_FUSE_LOW( (byte) 0x6C ),
    CMD_PROGRAM_HIDDEN_FUSE_HIGH( (byte) 0x6D ),
    CMD_READ_HIDDEN_FUSE_HIGH( (byte) 0x6E ),
    CMD_PROGRAM_HIDDEN_FUSE_EXT( (byte) 0x6F ),
    CMD_READ_HIDDEN_FUSE_EXT( (byte) 0x70 ),

// *****************[ STK status constants ]***************************

    // Success
    STATUS_CMD_OK( (byte) 0x00 ),

    // Warnings
    STATUS_CMD_TOUT( (byte) 0x80 ),
    STATUS_RDY_BSY_TOUT( (byte) 0x81 ),
    STATUS_SET_PARAM_MISSING( (byte) 0x82 ),

    // Errors
    STATUS_CMD_FAILED( (byte) 0xC0 ),
    STATUS_CKSUM_ERROR( (byte) 0xC1 ),
    STATUS_CMD_UNKNOWN( (byte) 0xC9 ),
    STATUS_CMD_ILLEGAL_PARAMETER( (byte) 0xCA ),

    // Status
    STATUS_CONN_FAIL_MOSI( (byte) 0x01 ),
    STATUS_CONN_FAIL_RST( (byte) 0x02 ),
    STATUS_CONN_FAIL_SCK( (byte) 0x04 ),
    STATUS_TGT_NOT_DETECTED( (byte) 0x00 ),
    STATUS_ISP_READY( (byte) 0x10 ),
    STATUS_TGT_REVERSE_INSERTED( (byte) 0x20  ),

// hw_status
// Bits in status variable
// Bit 0-3: Slave MCU
// Bit 4-7: Master MCU

    STATUS_AREF_ERROR( 0 ),
// Set to '1' if AREF is short circuited

    STATUS_VTG_ERROR( 4 ),
// Set to '1' if VTG is short circuited

    STATUS_RC_CARD_ERROR( 5 ),
// Set to '1' if board id changes when board is powered

    STATUS_PROGMODE( 6 ),
// Set to '1' if board is in programming mode

    STATUS_POWER_SURGE( 7 ),
// Set to '1' if board draws excessive current

    // *****************[ STK parameter constants ]***************************
    PARAM_BUILD_NUMBER_LOW( (byte) 0x80 ), /* ??? */
    PARAM_BUILD_NUMBER_HIGH( (byte) 0x81 ), /* ??? */
    PARAM_HW_VER( (byte) 0x90 ),
    PARAM_SW_MAJOR( (byte) 0x91 ),
    PARAM_SW_MINOR( (byte) 0x92 ),
    PARAM_VTARGET( (byte) 0x94 ),
    PARAM_VADJUST( (byte) 0x95 ), /* STK500 only */
    PARAM_OSC_PSCALE( (byte) 0x96 ), /* STK500 only */
    PARAM_OSC_CMATCH( (byte) 0x97 ), /* STK500 only */
    PARAM_SCK_DURATION( (byte) 0x98 ), /* STK500 only */
    PARAM_TOPCARD_DETECT( (byte) 0x9A ), /* STK500 only */
    PARAM_STATUS( (byte) 0x9C ), /* STK500 only */
    PARAM_DATA( (byte) 0x9D ), /* STK500 only */
    PARAM_RESET_POLARITY( (byte) 0x9E ), /* STK500 only, and STK600 FW
                                                  * version <= 2.0.3 */
    PARAM_CONTROLLER_INIT( (byte) 0x9F ),

    /* STK600 parameters */
    PARAM_STATUS_TGT_CONN( (byte) 0xA1 ),
    PARAM_DISCHARGEDELAY( (byte) 0xA4 ),
    PARAM_SOCKETCARD_ID( (byte) 0xA5 ),
    PARAM_ROUTINGCARD_ID( (byte) 0xA6 ),
    PARAM_EXPCARD_ID( (byte) 0xA7 ),
    PARAM_SW_MAJOR_SLAVE1( (byte) 0xA8 ),
    PARAM_SW_MINOR_SLAVE1( (byte) 0xA9 ),
    PARAM_SW_MAJOR_SLAVE2( (byte) 0xAA ),
    PARAM_SW_MINOR_SLAVE2( (byte) 0xAB ),
    PARAM_BOARD_ID_STATUS( (byte) 0xAD ),
    PARAM_RESET( (byte) 0xB4 ),

    PARAM_JTAG_ALLOW_FULL_PAGE_STREAM( (byte) 0x50 ),
    PARAM_JTAG_EEPROM_PAGE_SIZE( (byte) 0x52 ),
    PARAM_JTAG_DAISY_BITS_BEFORE( (byte) 0x53 ),
    PARAM_JTAG_DAISY_BITS_AFTER( (byte) 0x54 ),
    PARAM_JTAG_DAISY_UNITS_BEFORE( (byte) 0x55 ),
    PARAM_JTAG_DAISY_UNITS_AFTER( (byte) 0x56 ),

    // *** Parameter constants for 2 byte values ***
    PARAM2_SCK_DURATION( (byte) 0xC0 ),
    PARAM2_CLOCK_CONF( (byte) 0xC1 ),
    PARAM2_AREF0( (byte) 0xC2 ),
    PARAM2_AREF1( (byte) 0xC3 ),

    PARAM2_JTAG_FLASH_SIZE_H( (byte) 0xC5 ),
    PARAM2_JTAG_FLASH_SIZE_L( (byte) 0xC6 ),
    PARAM2_JTAG_FLASH_PAGE_SIZE( (byte) 0xC7 ),
    PARAM2_RC_ID_TABLE_REV( (byte) 0xC8 ),
    PARAM2_EC_ID_TABLE_REV( (byte) 0xC9 ),

    /* STK600 XPROG section */
// XPROG modes
    XPRG_MODE_PDI( 0 ),
    XPRG_MODE_JTAG( 1 ),
    XPRG_MODE_TPI( 2 ),

    // XPROG commands
    XPRG_CMD_ENTER_PROGMODE( (byte) 0x01 ),
    XPRG_CMD_LEAVE_PROGMODE( (byte) 0x02 ),
    XPRG_CMD_ERASE( (byte) 0x03 ),
    XPRG_CMD_WRITE_MEM( (byte) 0x04 ),
    XPRG_CMD_READ_MEM( (byte) 0x05 ),
    XPRG_CMD_CRC( (byte) 0x06 ),
    XPRG_CMD_SET_PARAM( (byte) 0x07 ),

    // Memory types
    XPRG_MEM_TYPE_APPL( 1 ),
    XPRG_MEM_TYPE_BOOT( 2 ),
    XPRG_MEM_TYPE_EEPROM( 3 ),
    XPRG_MEM_TYPE_FUSE( 4 ),
    XPRG_MEM_TYPE_LOCKBITS( 5 ),
    XPRG_MEM_TYPE_USERSIG( 6 ),
    XPRG_MEM_TYPE_FACTORY_CALIBRATION( 7 ),

    // Erase types
    XPRG_ERASE_CHIP( 1 ),
    XPRG_ERASE_APP( 2 ),
    XPRG_ERASE_BOOT( 3 ),
    XPRG_ERASE_EEPROM( 4 ),
    XPRG_ERASE_APP_PAGE( 5 ),
    XPRG_ERASE_BOOT_PAGE( 6 ),
    XPRG_ERASE_EEPROM_PAGE( 7 ),
    XPRG_ERASE_USERSIG( 8 ),
    XPRG_ERASE_CONFIG( 9 ), // TPI only, prepare fuse write

    // Write mode flags
    XPRG_MEM_WRITE_ERASE( 0 ),
    XPRG_MEM_WRITE_WRITE( 1 ),

    // CRC types
    XPRG_CRC_APP( 1 ),
    XPRG_CRC_BOOT( 2 ),
    XPRG_CRC_FLASH( 3 ),

    // Error codes
    XPRG_ERR_OK( 0 ),
    XPRG_ERR_FAILED( 1 ),
    XPRG_ERR_COLLISION( 2 ),
    XPRG_ERR_TIMEOUT( 3 ),

    // XPROG parameters of different sizes
// 4-byte address
    XPRG_PARAM_NVMBASE( (byte) 0x01 ),
    // 2-byte page size
    XPRG_PARAM_EEPPAGESIZE( (byte) 0x02 ),
    // 1-byte, undocumented TPI param
    XPRG_PARAM_TPI_3( (byte) 0x03 ),
    // 1-byte, undocumented TPI param
    XPRG_PARAM_TPI_4( (byte) 0x04 ),

// *****************[ STK answer constants ]***************************

    ANSWER_CKSUM_ERROR( (byte) 0xB0  );


    byte byteValue;
    int intValue;

    STK_Message(byte b)
    {
        byteValue = b;
    }

    STK_Message(int i)
    {
        intValue = i;
    }


}
