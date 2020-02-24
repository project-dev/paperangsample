package krohigewagma.jp.paperang;

public enum Command {
    /**
     * 画像を印刷します。
     * 戻り値はありません。
     */
    PRINT_DATA(0x00, "PRINT_DATA"),

    /**
     * @deprecated 試していません
     */
    PRINT_DATA_COMPRESS(0x01, "PRINT_DATA_COMPRESS"),

    /**
     * @deprecated 試していません
     */
    FIRMWARE_DATA(0x02, "FIRMWARE_DATA"),

    /**
     * @deprecated 試していません
     */
    USB_UPDATE_FIRMWARE(0x03,"USB_UPDATE_FIRMWARE"),

    /**
     * バージョンを取得します
     */
    GET_VERSION(0x04, "GET_VERSION"),

    /**
     * @deprecated 試していません
     */
    SENT_VERSION(0x05, "SENT_VERSION"),

    /**
     * モデルを取得します
     */
    GET_MODEL(0x06, "GET_MODEL"),

    /**
     * @deprecated 試していません
     */
    SENT_MODEL(0x07, "SENT_MODEL"),

    /**
     * MACアドレスを取得します
     */
    GET_BT_MAC(0x08, "GET_BT_MAC"),

    /**
     * @deprecated 試していません
     */
    SENT_BT_MAC(0x09, "SENT_BT_MAC"),

    /**
     * シリアルナンバーを取得します
     * @deprecated 試していません
     */
    GET_SN(0x0A, "GET_SN"),

    /**
     * @deprecated 試していません
     */
    SENT_SN(0x0B, "SENT_SN"),

    /**
     * ステータスを取得します
     * @deprecated 試していません
     */
    GET_STATUS(0x0C, "GET_STATUS"),

    /**
     * @deprecated 試していません
     */
    SENT_STATUS(0x0D, "SENT_STATUS"),

    /**
     * @deprecated 試していません
     */
    GET_VOLTAGE(0x0E, "GET_VOLTAGE"),

    /**
     * @deprecated 試していません
     */
    SENT_VOLTAGE (0x0F, "SENT_VOLTAGE"),

    /**
     * @deprecated 試していません
     */
    GET_BAT_STATUS (0x10, "GET_BAT_STATUS"),

    /**
     * @deprecated 試していません
     */
    SENT_BAT_STATUS (0x11, "SENT_BAT_STATUS"),

    /**
     * @deprecated 試していません
     */
    GET_TEMP(0x12, "GET_TEMP"),

    /**
     * @deprecated 試していません
     */
    SENT_TEMP(0x13, "SENT_TEMP"),

    /**
     * @deprecated 試していません
     */
    SET_FACTORY_STATUS(0x14, "SET_FACTORY_STATUS"),

    /**
     * @deprecated 試していません
     */
    GET_FACTORY_STATUS (0x15, "GET_FACTORY_STATUS"),

    /**
     * @deprecated 試していません
     */
    SENT_FACTORY_STATUS (0x16, "SENT_FACTORY_STATUS"),

    /**
     * @deprecated 試していません
     */
    SENT_BT_STATUS(0x17, "SENT_BT_STATUS"),

    /**
     * CRC Keyを設定
     */
    SET_CRC_KEY(0x18, "SET_CRC_KEY"),

    /**
     * @deprecated 試していません
     */
    SET_HEAT_DENSITY(0x19, "SET_HEAT_DENSITY"),

    /**
     * 紙送り
     * データ 2byte 送る行数を指定する
     */
    FEED_LINE(0x1A, "FEED_LINE"),

    /**
     * テストページ印刷
     */
    PRINT_TEST_PAGE(0x1B, "PRINT_TEST_PAGE"),

    /**
     * @deprecated 試していません
     */
    GET_HEAT_DENSITY(0x1C, "GET_HEAT_DENSITY"),

    /**
     * @deprecated 試していません
     */
    SENT_HEAT_DENSITY(0x1D, "SENT_HEAT_DENSITY"),

    /**
     * @deprecated 試していません
     */
    SET_POWER_DOWN_TIME(0x1E, "SET_POWER_DOWN_TIME"),

    /**
     * @deprecated 試していません
     */
    GET_POWER_DOWN_TIME(0x1F, "GET_POWER_DOWN_TIME"),

    /**
     * @deprecated 試していません
     */
    SENT_POWER_DOWN_TIME(0x20, "SENT_POWER_DOWN_TIME"),

    /**
     * @deprecated 試していません
     */
    FEED_TO_HEAD_LINE(0x21, "FEED_TO_HEAD_LINE"),

    /**
     * @deprecated 試していません
     */
    PRINT_DEFAULT_PARA(0x22, "PRINT_DEFAULT_PARA"),

    /**
     * @deprecated 試していません
     */
    GET_BOARD_VERSION(0x23, "GET_BOARD_VERSION"),

    /**
     * @deprecated 試していません
     */
    SENT_BOARD_VERSION(0x24, "SENT_BOARD_VERSION"),

    /**
     * @deprecated 試していません
     */
    GET_HW_INFO(0x25, "GET_HW_INFO"),

    /**
     * @deprecated 試していません
     */
    SENT_HW_INFO(0x26, "SENT_HW_INFO"),

    /**
     * @deprecated 試していません
     */
    SET_MAX_GAP_LENGTH(0x27, "SET_MAX_GAP_LENGTH"),

    /**
     * @deprecated 試していません
     */
    GET_MAX_GAP_LENGTH(0x28, "GET_MAX_GAP_LENGTH"),

    /**
     * @deprecated 試していません
     */
    SENT_MAX_GAP_LENGTH(0x29, "SENT_MAX_GAP_LENGTH"),

    /**
     * @deprecated 試していません
     */
    GET_PAPER_TYPE(0x2A, "GET_PAPER_TYPE"),

    /**
     * @deprecated 試していません
     */
    SENT_PAPER_TYPE(0x2B, "SENT_PAPER_TYPE"),

    /**
     * @deprecated 試していません
     */
    SET_PAPER_TYPE(0x2C, "SET_PAPER_TYPE"),

    /**
     * @deprecated 試していません
     */
    GET_COUNTRY_NAME(0x2D,"GET_COUNTRY_NAME"),

    /**
     * @deprecated 試していません
     */
    SENT_COUNTRY_NAME(0x2E, "SENT_COUNTRY_NAME"),

    /**
     * 切断します
     */
    DISCONNECT_BT_CMD(0x2F, "DISCONNECT_BT_CMD"),

    /**
     * コマンドの最大の値
     * おそらくコマンドとして使う値ではないかと。
     * @deprecated 試していません
     */
    MAX_CMD(0x30, "MAX_CMD");

    private final int val;
    private final String name;

    public static Command getCommand(int cmdVal){
        for(Command cmd : values()){
            if(cmd.val == cmdVal){
                return cmd;
            }
        }
        throw new IllegalArgumentException("not found");
    }

    public static Command getCommandAtName(String name){
        for(Command cmd : values()){
            if(cmd.name.equals(name)){
                return cmd;
            }
        }
        return MAX_CMD;
    }

    private Command(int val, String name)
    {
        this.val = val;
        this.name = name;
    }



    public int getVal(){
        return this.val;
    }

}
