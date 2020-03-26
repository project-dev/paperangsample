package krohigewagma.jp.paperangsample;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.List;

import krohigewagma.jp.paperang.Command;
import krohigewagma.jp.paperang.PaperangController;

/*
 * 参考：https://lang-ship.com/blog/?p=1305
 *       https://lang-ship.com/blog/?p=1318
 */
public class MainActivity extends AppCompatActivity {

    private static String SPP = "00001101-0000-1000-8000-00805F9B34FB";
    private static final int RESULT_PICK_IMAGEFILE = 1001;

    private PaperangController ctl = null;
    private List<BluetoothDevice> devices = null;

    private Bitmap bmp = null;

    private Command nextCmd = Command.MAX_CMD;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        Spinner spinner = findViewById(R.id.cmd_spinner);
        final ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.cmd_spinner, R.layout.support_simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String cmd = adapter.getItem(i).toString();
                nextCmd =Command.getCommandAtName(cmd);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        Button btnConnect = findViewById(R.id.btn_connect);
        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                devices = PaperangController.find();
                try {
                    ctl.connect(devices.get(0));
                } catch (IOException e) {
                    Log.e("PAPERANG", e.getMessage());
                }
            }
        });

        Button btnDisconnect = findViewById(R.id.btn_disconnect);
        btnDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    ctl.disconnectBtCmd();
                    ctl.disconnect();
                } catch (IOException e) {
                    Log.e("PAPERANG", e.getMessage());
                }
            }
        });

        Button btnExec = findViewById(R.id.btn_exec);
        btnExec.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    switch(nextCmd){
                        case PRINT_DATA:
                            ctl.printImage(bmp);
                            break;
//                        case PRINT_DATA_COMPRESS:
//                            break;
//                        case FIRMWARE_DATA:
//                            break;
//                        case USB_UPDATE_FIRMWARE:
//                            break;
                        case GET_VERSION:
                            ctl.getVersion();
                            break;
                        case SENT_VERSION:
                            ctl.sentVersion();
                            break;
                        case GET_MODEL:
                            ctl.getModel();
                            break;
                        case SENT_MODEL:
                            ctl.sentModel();
                            break;
                        case GET_BT_MAC:
                            ctl.getBtMac();
                            break;
                        case SENT_BT_MAC:
                            ctl.sentBtMac();
                            break;
                        case GET_SN:
                            ctl.getSn();
                            break;
                        case SENT_SN:
                            ctl.sentSn();
                            break;
                        case GET_STATUS:
                            ctl.getStatus();
                            break;
                        case SENT_STATUS:
                            ctl.sentStatus();
                            break;
                        case GET_VOLTAGE:
                            ctl.getVoltage();
                            break;
                        case SENT_VOLTAGE :
                            ctl.sentVoltage();
                            break;
                        case GET_BAT_STATUS :
                            ctl.getBatStatus();
                            break;
                        case SENT_BAT_STATUS :
                            break;
                        case GET_TEMP:
                            ctl.getTemp();
                            break;
                        case SENT_TEMP:
                            ctl.sentTemp();
                            break;
//                        case SET_FACTORY_STATUS:
//                            ctl.setFactoryStatus();
//                            break;
                        case GET_FACTORY_STATUS :
                            ctl.getFactoryStatus();
                            break;
                        case SENT_FACTORY_STATUS :
                            ctl.sentFactoryStatus();
                            break;
                        case SENT_BT_STATUS:
                            ctl.sentBtStatus();
                            break;
//                        case SET_CRC_KEY:
//                          break;
//                        case SET_HEAT_DENSITY:
//                            break;
                        case FEED_LINE:
                            ctl.sendFeedLine((short)1);
                            break;
                        case PRINT_TEST_PAGE:
                            ctl.printTestPage();
                            break;
                        case GET_HEAT_DENSITY:
                            ctl.getHeatDensity();
                            break;
                        case SENT_HEAT_DENSITY:
                            ctl.sentHeatDensity();
                            break;
//                        case SET_POWER_DOWN_TIME:
//                            ctl.setPowerDownTime();
//                            break;
                        case GET_POWER_DOWN_TIME:
                            ctl.getPowerDownTime();
                            break;
                        case SENT_POWER_DOWN_TIME:
                            ctl.sentPowerDownTime();
                            break;
                        case FEED_TO_HEAD_LINE:
                            ctl.feedToHeadLine();
                            break;
                        case PRINT_DEFAULT_PARA:
                            ctl.printDefaultPara();
                            break;
                        case GET_BOARD_VERSION :
                            ctl.getBoardVersion();
                            break;
                        case SENT_BOARD_VERSION:
                            ctl.sentBoardVersion();
                            break;
                        case GET_HW_INFO:
                            ctl.getHwInfo();
                            break;
                        case SENT_HW_INFO:
                            ctl.sentHwInfo();
                            break;
//                        case SET_MAX_GAP_LENGTH :
//                            ctl.setMaxGapLength();
//                            break;
                        case GET_MAX_GAP_LENGTH:
                            ctl.getMaxGapLength();
                            break;
                        case SENT_MAX_GAP_LENGTH :
                            ctl.sentMaxGapLength();
                            break;
                        case GET_PAPER_TYPE :
                            ctl.getPaperType();
                            break;
                        case SENT_PAPER_TYPE:
                            ctl.sentPaperType();
                            break;
//                        case SET_PAPER_TYPE :
//                            ctl.setPaperType();
//                            break;
                        case GET_COUNTRY_NAME:
                            ctl.getCountryName();
                            break;
                        case SENT_COUNTRY_NAME:
                            ctl.sentCountryName();
                            break;
                        case DISCONNECT_BT_CMD:
                            ctl.disconnectBtCmd();
                            break;
                        default:
                            break;
                    }
                } catch (IOException e) {
                    Log.e("PAPERANG", e.getMessage());
                }
            }

        });

        Button btnImg = findViewById(R.id.btnImg);
        btnImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i("PAPERANG", "onTouch");
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                startActivityForResult(intent, RESULT_PICK_IMAGEFILE);
            }
        });

        Button btnPrintImg = findViewById(R.id.btnPrintImg);
        btnPrintImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i("PAPERANG", "onTouch");
                try {
                    devices = PaperangController.find();
                    ctl.connect(devices.get(0));
                } catch (IOException e) {
                    Log.e("PAPERANG", e.getMessage());
                }
                try {
                    ctl.printImage(bmp);
                    ctl.sendFeedLine((short)2);
                } catch (IOException e) {
                    Log.e("PAPERANG", e.getMessage());
                }finally{
                    try {
                        ctl.disconnect();
                    } catch (IOException e) {
                        Log.e("PAPERANG", e.getMessage());
                    }
                }
            }
        });

        ctl = new PaperangController();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i("PAPERANG", "onActivityResult");
        if (requestCode == RESULT_PICK_IMAGEFILE && resultCode == Activity.RESULT_OK) {
            if (data.getData() != null) {
                Log.i("PAPERANG", "PRINT IMAGE");
                Uri uri = data.getData();
                ParcelFileDescriptor pfd = null;
                try{
                    pfd = getContentResolver().openFileDescriptor(uri, "r");
                    FileDescriptor fd = pfd.getFileDescriptor();

                    // 画像を2値化する
                    Bitmap bitmap = BitmapFactory.decodeFileDescriptor(fd);
                    bmp = ctl.conv2Value(bitmap);

                    // 2値化した画像を表示
                    ImageView preview = findViewById(R.id.imgPreview);
                    preview.setImageBitmap(bmp);

                }catch(IOException e){
                    Log.e("PAPERANG", e.getMessage());

                }finally{
                    try{
                        if(pfd != null){
                            pfd.close();
                        }
                    }catch(IOException e){
                        Log.e("PAPERANG", e.getMessage());
                    }
                }
            }
        }
    }
}
