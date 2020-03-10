package krohigewagma.jp.paperang;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;

import com.jcraft.jzlib.CRC32;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/*
 * 参考：https://lang-ship.com/blog/?p=1305
 *       https://lang-ship.com/blog/?p=1318
 */
public class PaperangController {
    // CRC
    private static byte[] crc = new byte[]{
            (byte)0x02,                                     // 1.開始
            (byte)0x18, (byte)0x00,                         // 2.制御コード
            (byte)0x04, (byte)0x00,                         // 3.データ長さ
            (byte)0x78, (byte)0x7A, (byte)0xCE, (byte)0x33, // 4.CRC_KEY
            (byte)0x2C, (byte)0x89, (byte)0x80, (byte)0xF0, // 5.CRC(4.から求める)
            (byte)0x03                                      // 6.終端
    };

    private ImageMode image_mode = ImageMode.MODE3x3;

    /**
     * 標準のキー
     * CRCキーを送るときはこれを使ってCRCを求める
     */
    private long standardkey = 0x35769521;

    /**
     * コマンドを送るときはこれを使ってCRCを求める
     * standardkeyとなぜ違うのか、それはまだわかっていない
     */
    private long crckey = 0x6968634 ^ 0x2e696d;

    /**
     * コマンドの戻りを受け取るバイトの最大サイズ
     */
    private int max_msg_length = 1024;

    /**
     * BluetoothでSPPで通信する際のUUID
     */
    private static String SPP = "00001101-0000-1000-8000-00805F9B34FB";

    private List<PaperangListener> listeners = null;

    /**
     * Bluetoothで通信するためのソケット
     */
    private BluetoothSocket socket = null;
    private OutputStream os = null;
    private InputStream is = null;

    Thread thread = null;

    /**
     * 空データ
     */
    private static byte[] EMPRYDATA = new byte[0];


    /**
     * ペアリング済みのPAPERANGを取得します
     * @return
     */
    public static List<BluetoothDevice> find(){
        BluetoothAdapter bt = BluetoothAdapter.getDefaultAdapter();
        if(bt.equals(null)){
            Log.i("PAPERANG", "Bluetooth not support");
            return null;
        }
        if(!bt.isEnabled()){
            Log.i("PAPERANG", "disable bluetooth");
            return null;
        }

        // ペアリング済みの機器の一覧からPAPERANGを探す
        Set<BluetoothDevice> devices = bt.getBondedDevices();
        List<BluetoothDevice> deviceList = new ArrayList<>();
        for(BluetoothDevice device : devices){
            if(!"Paperang".equals(device.getName())){
                continue;
            }
            Log.i("PAPERANG", "match " + device.getAddress() + ":" + device.getName());
            deviceList.add(device);
        }
        return deviceList;
    }

    public void addPaperangListener(PaperangListener listener){
        if(listeners == null){
            listeners = new ArrayList<PaperangListener>();
        }
        listeners.add(listener);
    }

    public void removeListener(PaperangListener listener){
        if(listeners.contains(listener)){
            listeners.remove(listener);
        }
    }

    /**
     * 接続
     * @param device
     * @throws IOException
     */
    public void connect(BluetoothDevice device) throws IOException{
        if(socket != null){
            return;
        }
        socket = device.createRfcommSocketToServiceRecord(UUID.fromString(SPP));
        socket.connect();
        os = socket.getOutputStream();
        is = socket.getInputStream();
        os.write(crc);
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    while(thread != null && thread.isInterrupted() == false){
                        byte[] result = new byte[max_msg_length];
                        try {
                            if(is != null && socket != null && socket.isConnected() == true){
                                is.read(result);
                                ResultData resultData = new ResultData(result);
                                if(listeners != null){
                                    for (PaperangListener listener: listeners) {
                                        listener.onResult(resultData);
                                    }
                                }
                            }
                        } catch (IOException e) {
                            Log.e("PAPERANG", e.getMessage());
                        }
                        Thread.sleep(100);
                    }
                } catch(InterruptedException e){
                }
            }
        });
        thread.start();

        Log.i("PAPERANG", "connected");
    }

    /**
     * 切断
     */
    public void disconnect() throws IOException{
        //このコマンドを呼び出すべきなのかどうか判断できない
        //disconnectBtCmd();

        if(thread != null){
            thread.interrupt();
            thread = null;
        }

        try{
            if(socket != null && socket.isConnected()){
                is.close();
                os.close();
                socket.close();
            }
        }catch(Exception e){
            Log.e("PAPERANG", e.getMessage());
        }finally{
            os = null;
            is = null;
            socket = null;
        }
    }

    /**
     * コマンドの書き込み
     * @param cmd
     * @throws IOException
     */
    public void execute(byte[] cmd) throws IOException{
        Log.i("PAPERANG", "write command");
        os.write(cmd);
    }

    /**
     * コマンドを生成
     * @param cmd
     * @param data
     * @return
     * @throws IOException
     */
    public byte[] createCommand(Command cmd, byte[] data){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ByteBuffer bb;

        // 1.開始                 0x02   1Byte
        // 2.制御コード                  2Byte
        // 3.データ長さ                  2Byte
        // 4.データ                      nByte
        // 5.CRC(4.から求める)           4Byte
        // 6.終端                 0x03   1Byte
        byte[] cmdBuffer = null;

        try{
            // 開始(1byte)
            baos.write(0x02);

            // コマンド(2byte)
            bb = ByteBuffer.allocate(2);
            bb.order(ByteOrder.LITTLE_ENDIAN);
            bb.putShort((short)cmd.getVal());
            byte[] cmdByte = bb.array();
            byteLog("COMMAND", cmdByte);
            baos.write(cmdByte);

            // データの長さを記録(2byte)
            bb = ByteBuffer.allocate(2);
            bb.order(ByteOrder.LITTLE_ENDIAN);
            if(data == null){
                bb.putShort((short)0);
            }else{
                bb.putShort((short)data.length);
            }
            byte[] dataLenByte = bb.array();
            byteLog( "SIZE", dataLenByte);
            baos.write(dataLenByte);

            // データをリトルエンディアンで書き込む(nbyte)
            if(data == null){
                bb = ByteBuffer.allocate(0);
                bb.put(new byte[0]);
            }else{
                bb = ByteBuffer.allocate(data.length);
                bb.put(data);
            }
            bb.order(ByteOrder.LITTLE_ENDIAN);
            byte[] dataByte = bb.array();
            byteLog("DATA", dataByte);
            baos.write(dataByte);

            // CRC(4byte)
            CRC32 crc32 = new CRC32();
            crc32.reset(crckey);
            if(data == null){
                crc32.update(new byte[0], 0, 0);
            }else{
                crc32.update(data, 0, data.length);
            }
            long crcValue = crc32.getValue();

            bb = ByteBuffer.allocate(4);
            bb.order(ByteOrder.LITTLE_ENDIAN);
            bb.putInt((int)crcValue);
            byte[] crcByte = bb.array();
            byteLog("CRC", crcByte);
            baos.write(crcByte);

            // 終端バイト(1byte)
            baos.write(0x03);

            cmdBuffer = baos.toByteArray();
            byteLog("Command buffer", cmdBuffer);
            baos.close();
        } catch (IOException e) {
            Log.e("PAPERANG", e.getMessage());
        } finally{
            if(baos != null){
                try {
                    baos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return cmdBuffer;
    }

    /**
     * byte配列をログに出力
     * @param logTitle
     * @param buff
     */
    private void byteLog(String logTitle, byte[] buff){
        StringBuffer log = new StringBuffer();
/*
        for(int i = buff.length - 1; i >= 0; i--){
            log.append(Integer.toHexString(Math.abs(buff[i])));
            log.append(" ");
        }
 */
        for(int i = 0; i < buff.length ; i++){
            String txt = "00" + Integer.toHexString(Math.abs(buff[i]));
            log.append(txt.substring(txt.length() - 2) );
            if((i +1) % 48 == 0){
                Log.i("PAPERANG", logTitle + " : " + log.toString());
                log = new StringBuffer();
            }else{
                log.append(" ");
            }
        }
        if(log.length() > 0){
            Log.i("PAPERANG", logTitle + " : " + log.toString());
        }
    }

    public Bitmap convGrayscale(Bitmap img){
        int width = img.getWidth();
        int height = img.getHeight();
        //Log.i("PAPERANG", "width :" + Integer.toString(width));
        //Log.i("PAPERANG", "height:" + Integer.toString(height));

        if(width != 384) {
            // サイズ調整
            float par = 384f / (float) width;
            Log.i("PAPERANG", Float.toString(par));
            width = Math.round(width * par);
            height = Math.round(height * par);
            if (width != 384) {
                width = 384;
            }
        }
        //Log.i("PAPERANG", "width :" + Integer.toString(width));
        //Log.i("PAPERANG", "height:" + Integer.toString(height));

        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bmp);
        Paint paint = new Paint();

        // グレースケールのフィルタを適用する
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);

        Rect distRect = new Rect(0, 0, width, height);
        Rect srcRect = new Rect(0, 0, img.getWidth(), img.getHeight());
        canvas.drawBitmap(img, srcRect, distRect, paint);
        canvas = null;
        return bmp;
    }

    public void setImage_mode(ImageMode mode){
        this.image_mode = mode;
    }

    public ImageMode getImage_mode(){
        return this.image_mode;
    }

    /**
     * 参考：https://itech-program.com/python/994
     * @param img
     * @return
     */
    public Bitmap conv2Value(Bitmap img){
        Bitmap bmp = convGrayscale(img);
        int width = bmp.getWidth();
        int height = bmp.getHeight();
        int thresh = 128;
        int err = 0;

        int[][] matrix = null;
        int matrixsize = 0;
        switch(this.image_mode) {
            case MODE3x3:
                matrix = new int[][]{
                        {2, 3, 4}
                        ,{1, 0, 5}
                        ,{8, 7, 6}
                };
                matrixsize = 3;
                break;
            case MODE4x4:
                matrix = new int[][]{
                        {0, 8, 2, 10}
                        ,{12, 4, 14, 6}
                        ,{3, 11, 1, 9}
                        ,{15, 7, 13, 5}
                };
                matrixsize = 4;
                break;
        }

        int[] gray= new int[width * height];
        bmp.getPixels(gray, 0, width, 0, 0, width, height);

        for(int i = 0; i < matrixsize; i++){
            for(int j = 0; j < matrixsize; j++){
                //0～255に変換
                matrix[i][j] = matrix[i][j] * 16;
            }
        }

        for(int y = 0; y < height; y++){
            for(int x = 0; x < width; x++){
                int color = gray[y * width + x];
                int r = Color.red(color);
                switch(this.image_mode){
                    case NORMAL:
                        if(r + err < thresh){
                            err = r + err - 0;
                            gray[y * width + x] = Color.argb(255,0,0,0);
                        }else{
                            err = r + err - 255;
                            gray[y * width + x] = Color.argb(255,255,255,255);
                        }
                        break;
                    case MODE3x3:
                    case MODE4x4:
                        if(Color.red(gray[y * width + x]) < matrix[y % matrixsize][x % matrixsize]){
                            gray[y * width + x] = Color.argb(255,0,0,0);
                        }else{
                            gray[y * width + x] = Color.argb(255,255,255,255);
                        }
                        break;
                }
            }
        }

        bmp.setPixels(gray, 0, width, 0, 0, width, height);
        return bmp;
    }

    /**
     * イメージを印刷します
     * 横幅は384が最大らしい
     * リサイズする必要はあるのだろうか・・・？
     * @param img
     */
    public void printImage(Bitmap img) throws IOException {

        int width = img.getWidth();
        int height = img.getHeight();

        Log.i("PAPERANG", "width :" + Integer.toString(width));
        Log.i("PAPERANG", "height:" + Integer.toString(height));

        int[] pixcels = new int[width * height];
        img.getPixels(pixcels, 0, width, 0, 0, width, height);

        // バイト配列をビットに
        byte[] bits = new byte[48 * height];
        for(int y = 0; y < height; y++){
            StringBuffer line = new StringBuffer();
            for(int x = 0; x < width; x++){
                int r = Color.red(pixcels[y * width + x]);
                if(r == 0x00) {
                    line.append("1");
                    int idx = y * 48 + (x / 8);
                    //bits[idx] = (byte) (bits[idx] | (1 << (8 - x % 8)));
                    switch(x % 8){
                        case 0:
                            bits[idx] = (byte)(bits[idx] | (byte)0x80);
                            break;
                        case 1:
                            bits[idx] = (byte)(bits[idx] | (byte)0x40);
                            break;
                        case 2:
                            bits[idx] = (byte)(bits[idx] | (byte)0x20);
                            break;
                        case 3:
                            bits[idx] = (byte)(bits[idx] | (byte)0x10);
                            break;
                        case 4:
                            bits[idx] = (byte)(bits[idx] | (byte)0x08);
                            break;
                        case 5:
                            bits[idx] = (byte)(bits[idx] | (byte)0x04);
                            break;
                        case 6:
                            bits[idx] = (byte)(bits[idx] | (byte)0x02);
                            break;
                        case 7:
                            bits[idx] = (byte)(bits[idx] | (byte)0x01);
                            break;
                    }
                }else{
                    line.append("0");
                }
            }
        }

        int loffset = 41;
        if(height <= 41){
            loffset = height;
        }

        try {
            byte[] buff = new byte[48];
            ByteBuffer buffs = ByteBuffer.allocate(48 * loffset);
            buffs.order(ByteOrder.LITTLE_ENDIAN);
            for (int i = 0; i < bits.length; i++) {
                if(i % (48 * loffset) == 0 && i > 0){
                    printData(buffs.array());
                    buffs.clear();
                    buffs = ByteBuffer.allocate(48 * loffset);
                    buffs.order(ByteOrder.LITTLE_ENDIAN);
                }
                buffs.put(bits[i]);
            }
            printData(buffs.array());

        }catch(Exception e){
            if(e != null){
                if(e.getMessage() != null){
                    Log.e("PAPERANG", e.getMessage());
                }else{
                    Log.e("PAPERANG", e.toString());
                }
            }
        }
    }

    /**
     * 画像を印刷します
     * @param data
     * @throws IOException
     */
    public void printData(byte[] data) throws IOException{
        byte[] cmd = createCommand(Command.PRINT_DATA, data);
        execute(cmd);
    }

    /**
     * @deprecated 試していません
     * @throws IOException
     */
    public void printDataCompress() throws IOException{
        byte[] cmd = createCommand(Command.PRINT_DATA_COMPRESS, null);
        execute(cmd);
    }

    /**
     * @deprecated 試していません
     * @throws IOException
     */
    public void firmwareData() throws IOException{
        byte[] cmd = createCommand(Command.FIRMWARE_DATA, null);
        execute(cmd);
    }

    /**
     * @deprecated 試していません
     * @throws IOException
     */
    public void usbUpdateFirmware() throws IOException{
        byte[] cmd = createCommand(Command.USB_UPDATE_FIRMWARE, null);
        execute(cmd);
    }

    /**
     * バージョンを取得します
     * @throws IOException
     */
    public void getVersion() throws IOException{
        byte[] cmd = createCommand(Command.GET_VERSION, EMPRYDATA);
        execute(cmd);
    }

    /**
     * @deprecated 試していません
     * @throws IOException
     */
    public void sentVersion() throws IOException{
        byte[] cmd = createCommand(Command.SENT_VERSION, null);
        execute(cmd);
    }

    /**
     * @deprecated 試していません
     * @throws IOException
     */
    public void getModel() throws IOException{
        byte[] cmd = createCommand(Command.GET_MODEL, EMPRYDATA);
        execute(cmd);
    }

    /**
     * @deprecated 試していません
     * @throws IOException
     */
    public void sentModel() throws IOException{
        byte[] cmd = createCommand(Command.SENT_MODEL, null);
        execute(cmd);
    }

    /**
     * MACアドレスを取得します
     * @throws IOException
     */
    public void getBtMac() throws IOException{
        byte[] cmd = createCommand(Command.GET_BT_MAC, null);
        execute(cmd);
    }

    /**
     * @deprecated 試していません
     * @throws IOException
     */
    public void sentBtMac() throws IOException{
        byte[] cmd = createCommand(Command.SENT_BT_MAC, null);
        execute(cmd);
    }

    /**
     * @deprecated 試していません
     * @throws IOException
     */
    public void getSn() throws IOException{
        byte[] cmd = createCommand(Command.GET_SN, null);
        execute(cmd);
    }

    /**
     * @deprecated 試していません
     * @throws IOException
     */
    public void sentSn() throws IOException{
        byte[] cmd = createCommand(Command.SENT_SN, null);
        execute(cmd);
    }

    /**
     * @deprecated 試していません
     * @throws IOException
     */
    public void getStatus() throws IOException{
        byte[] cmd = createCommand(Command.GET_STATUS, null);
        execute(cmd);
    }

    /**
     * @deprecated 試していません
     * @throws IOException
     */
    public void sentStatus() throws IOException{
        byte[] cmd = createCommand(Command.SENT_STATUS, null);
        execute(cmd);
    }

    /**
     * @deprecated 試していません
     * @throws IOException
     */
    public void getVoltage() throws IOException{
        byte[] cmd = createCommand(Command.GET_VOLTAGE, null);
        execute(cmd);
    }

    /**
     * @deprecated 試していません
     * @throws IOException
     */
    public void sentVoltage() throws IOException{
        byte[] cmd = createCommand(Command.SENT_VOLTAGE , null);
        execute(cmd);
    }

    /**
     * @deprecated 試していません
     * @throws IOException
     */
    public void getBatStatus() throws IOException{
        byte[] cmd = createCommand(Command.GET_BAT_STATUS , null);
        execute(cmd);
    }

    /**
     * @deprecated 試していません
     * @throws IOException
     */
    public void sentBatStatus() throws IOException{
        byte[] cmd = createCommand(Command.SENT_BAT_STATUS , null);
        execute(cmd);
    }

    /**
     * @deprecated 試していません
     * @throws IOException
     */
    public void getTemp() throws IOException{
        byte[] cmd = createCommand(Command.GET_TEMP, null);
        execute(cmd);
    }

    /**
     * @deprecated 試していません
     * @throws IOException
     */
    public void sentTemp() throws IOException{
        byte[] cmd = createCommand(Command.SENT_TEMP, null);
        execute(cmd);
    }

    /**
     * @deprecated 試していません
     * @throws IOException
     */
    public void setFactoryStatus() throws IOException{
        byte[] cmd = createCommand(Command.SET_FACTORY_STATUS, null);
        execute(cmd);
    }

    /**
     * @deprecated 試していません
     * @throws IOException
     */
    public void getFactoryStatus() throws IOException{
        byte[] cmd = createCommand(Command.GET_FACTORY_STATUS , null);
        execute(cmd);
    }

    /**
     * @deprecated 試していません
     * @throws IOException
     */
    public void sentFactoryStatus() throws IOException{
        byte[] cmd = createCommand(Command.SENT_FACTORY_STATUS , null);
        execute(cmd);
    }

    /**
     * @deprecated 試していません
     * @throws IOException
     */
    public void sentBtStatus() throws IOException{
        byte[] cmd = createCommand(Command.SENT_BT_STATUS, null);
        execute(cmd);
    }

    /**
     * @deprecated 試していません
     * @throws IOException
     */
    public void setCrcKey() throws IOException{
        byte[] cmd = createCommand(Command.SET_CRC_KEY, null);
        execute(cmd);
    }

    /**
     * @deprecated 試していません
     * @param density
     * @throws IOException
     */
    public void setHeatDensity(int density) throws IOException{
        byte[] cmd = createCommand(Command.SET_HEAT_DENSITY, null);
        execute(cmd);
    }

    /**
     * 紙送り
     * @param lines 送る行数
     * @throws IOException
     */
    public void sendFeedLine(short lines) throws IOException{
        ByteBuffer b = ByteBuffer.allocate(2);
        b.putShort(lines);
        byte[] cmd = createCommand(Command.FEED_LINE, b.array());
        execute(cmd);
    }

    /**
     * テストページを印刷します
     * @throws IOException
     */
    public void printTestPage() throws IOException{
        byte[] cmd = createCommand(Command.PRINT_TEST_PAGE, EMPRYDATA);
        execute(cmd);
    }

    /**
     * @deprecated 試していません
     * @throws IOException
     */
    public void getHeatDensity() throws IOException{
        byte[] cmd = createCommand(Command.GET_HEAT_DENSITY, new byte[]{});
        execute(cmd);
    }

    /**
     * @deprecated 試していません
     * @throws IOException
     */
    public void sentHeatDensity() throws IOException{
        byte[] cmd = createCommand(Command.SENT_HEAT_DENSITY, null);
        execute(cmd);
    }

    /**
     * @deprecated 試していません
     * @throws IOException
     */
    public void setPowerDownTime() throws IOException{
        byte[] cmd = createCommand(Command.SET_POWER_DOWN_TIME, null);
        execute(cmd);
    }

    /**
     * @deprecated 試していません
     * @throws IOException
     */
    public void getPowerDownTime() throws IOException{
        byte[] cmd = createCommand(Command.GET_POWER_DOWN_TIME, null);
        execute(cmd);
    }

    /**
     * @deprecated 試していません
     * @throws IOException
     */
    public void sentPowerDownTime() throws IOException{
        byte[] cmd = createCommand(Command.SENT_POWER_DOWN_TIME, null);
        execute(cmd);
    }

    /**
     * @deprecated 試していません
     * @throws IOException
     */
    public void feedToHeadLine() throws IOException{
        byte[] cmd = createCommand(Command.FEED_TO_HEAD_LINE, null);
        execute(cmd);
    }

    /**
     * @deprecated 試していません
     * @throws IOException
     */
    public void printDefaultPara() throws IOException{
        byte[] cmd = createCommand(Command.PRINT_DEFAULT_PARA, null);
        execute(cmd);
    }

    /**
     * @deprecated 試していません
     * @throws IOException
     */
    public void getBoardVersion() throws IOException{
        byte[] cmd = createCommand(Command.GET_BOARD_VERSION , null);
        execute(cmd);
    }

    /**
     * @deprecated 試していません
     * @throws IOException
     */
    public void getHwInfo() throws IOException{
        byte[] cmd = createCommand(Command.GET_HW_INFO, null);
        execute(cmd);
    }

    /**
     * @deprecated 試していません
     * @throws IOException
     */
    public void sentHwInfo() throws IOException{
        byte[] cmd = createCommand(Command.SENT_HW_INFO, null);
        execute(cmd);
    }

    /**
     * @deprecated 試していません
     * @throws IOException
     */
    public void setMaxGapLength() throws IOException{
        byte[] cmd = createCommand(Command.SET_MAX_GAP_LENGTH , null);
        execute(cmd);
    }

    /**
     * @deprecated 試していません
     * @throws IOException
     */
    public void getMaxGapLength() throws IOException{
        byte[] cmd = createCommand(Command.GET_MAX_GAP_LENGTH, null);
        execute(cmd);
    }

    /**
     * @deprecated 試していません
     * @throws IOException
     */
    public void sentMaxGapLength() throws IOException{
        byte[] cmd = createCommand(Command.SENT_MAX_GAP_LENGTH , null);
        execute(cmd);
    }

    /**
     * @deprecated 試していません
     * @throws IOException
     */
    public void getPaperType() throws IOException{
        byte[] cmd = createCommand(Command.GET_PAPER_TYPE , null);
        execute(cmd);
    }

    /**
     * @deprecated 試していません
     * @throws IOException
     */
    public void sentPaperType() throws IOException{
        byte[] cmd = createCommand(Command.SENT_PAPER_TYPE, null);
        execute(cmd);
    }

    /**
     * @deprecated 試していません
     * @throws IOException
     */
    public void setPaperType() throws IOException{
        byte[] cmd = createCommand(Command.SET_PAPER_TYPE , null);
        execute(cmd);
    }

    /**
     * @deprecated 試していません
     * @throws IOException
     */
    public void getCountryName() throws IOException{
        byte[] cmd = createCommand(Command.GET_COUNTRY_NAME, null);
        execute(cmd);
    }

    /**
     * @deprecated 試していません
     * @throws IOException
     */
    public void sentCountryName() throws IOException{
        byte[] cmd = createCommand(Command.SENT_COUNTRY_NAME, null);
        execute(cmd);
    }

    /**
     * 切断します
     * @throws IOException
     */
    public void disconnectBtCmd() throws IOException{
        byte[] cmd = createCommand(Command.DISCONNECT_BT_CMD, null);
        execute(cmd);
    }
}
