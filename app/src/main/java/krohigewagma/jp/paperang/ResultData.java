package krohigewagma.jp.paperang;

import androidx.annotation.NonNull;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

public class ResultData {
    private byte start;
    private int cmdVal;
    private int length;
    private byte[] data;
    private long crc;
    private byte end;
    private Command cmd;

    public ResultData( byte[] result){
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

    @NonNull
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
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
        sb.append(data);
        sb.append("\r\n");

        sb.append("CRC: ");
        sb.append(crc);
        sb.append("\r\n");

        sb.append("END: ");
        sb.append(end);
        sb.append("\r\n");

        return sb.toString();
    }
}
