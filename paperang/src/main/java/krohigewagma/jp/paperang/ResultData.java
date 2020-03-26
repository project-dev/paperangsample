package krohigewagma.jp.paperang;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;

public class ResultData {
    private byte start;
    private int cmdVal;
    private int length;
    private byte[] data;
    private long crc;
    private byte end;
    private Command cmd;
    private boolean isSuccess;

    public ResultData( byte[] result){
        isSuccess = false;
        if(result != null && result.length > 4) {
            this.start = result[0];
            this.cmdVal = result[2] << 4 | result[1];
            this.cmd = Command.getCommand(cmdVal);
            this.length = result[4] << 4 | result[3];
            this.crc = result[4 + length + 4] << 12 | result[4 + length + 3] << 8 | result[4 + length + 2] << 4 | result[4 + length + 1];
            this.end = result[4 + length + 4 + 1];
            ByteBuffer dataBuff = ByteBuffer.allocate(length);
            dataBuff.put(result, 5, length);
            this.data = dataBuff.array();
            isSuccess = true;
        }
    }

    public Command getCommand(){
        return cmd;
    }

    public byte[] getData(){
        return this.data;
    }

    public long getCrc(){
        return crc;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    @NonNull
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        if(true == isSuccess) {
            sb.append("START : ");
            sb.append(start);
            sb.append("\r\n");

            sb.append("COMMAND : ");
            sb.append(cmd == null ? "" : cmd.toString());
            sb.append("\r\n");

            sb.append("LENGTH : ");
            sb.append(Integer.toString(length));
            sb.append("\r\n");

            sb.append("DATA: ");
            //sb.append(data);
            StringBuffer log = new StringBuffer();
            for(int i = 0; i < data.length ; i++){
                String txt = "00" + Integer.toHexString(Math.abs(data[i]));
                log.append(txt.substring(txt.length() - 2) );
                if((i +1) % 48 == 0){
                    sb.append(log.toString());
                    log = new StringBuffer();
                }else{
                    sb.append(" ");
                }
            }
            if(log.length() > 0){
                sb.append(log.toString());
            }

            sb.append("\r\n");

            sb.append("CRC: ");
            sb.append(crc);
            sb.append("\r\n");

            sb.append("END: ");
            sb.append(end);
            sb.append("\r\n");
        }else{
            sb.append("faild execute");
        }
        return sb.toString();
    }
}
