package packet.transfer.read;

import constants.ServerProperties;
import constants.TypeConstants.MapleLanguageType;
import java.awt.Point;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

public class ReadingMaple {

    private final ByteStream bs;

    public ReadingMaple(ByteStream bs) {
        this.bs = bs;
    }

    public byte readByte() {
        return (byte) this.bs.readByte();
    }

    public String toString(boolean a) {
        return this.bs.toString(true);
    }

    public int readInt() {
        int byte1 = this.bs.readByte();
        int byte2 = this.bs.readByte();
        int byte3 = this.bs.readByte();
        int byte4 = this.bs.readByte();
        return (byte4 << 24) + (byte3 << 16) + (byte2 << 8) + byte1;
    }

    public long readInt2() {
        int byte1 = this.bs.readByte();
        int byte2 = this.bs.readByte();
        int byte3 = this.bs.readByte();
        int byte4 = this.bs.readByte();
        return (byte4 << 24) + (byte3 << 16) + (byte2 << 8) + byte1;
    }

    public short readShort() {
        int byte1 = this.bs.readByte();
        int byte2 = this.bs.readByte();
        return (short) ((byte2 << 8) + byte1);
    }

    public int readShort2() {
        int byte1 = this.bs.readByte();
        int byte2 = this.bs.readByte();
        return (byte2 << 8) + byte1;
    }

    public char readChar() {
        return (char) readShort();
    }

    public long readLong() {
        long byte1 = this.bs.readByte();
        long byte2 = this.bs.readByte();
        long byte3 = this.bs.readByte();
        long byte4 = this.bs.readByte();
        long byte5 = this.bs.readByte();
        long byte6 = this.bs.readByte();
        long byte7 = this.bs.readByte();
        long byte8 = this.bs.readByte();
        return (byte8 << 56) + (byte7 << 48) + (byte6 << 40) + (byte5 << 32) + (byte4 << 24) + (byte3 << 16) + (byte2 << 8) + byte1;
    }

    public long readFLong() {
        long byte1 = this.bs.readByte();
        long byte2 = this.bs.readByte();
        long byte3 = this.bs.readByte();
        long byte4 = this.bs.readByte();
        long byte5 = this.bs.readByte();
        long byte6 = this.bs.readByte();
        long byte7 = this.bs.readByte();
        long byte8 = this.bs.readByte();
        return (byte8 << 24) + (byte7 << 16) + (byte6 << 8) + byte5 + (byte4 << 56) + (byte3 << 48) + (byte2 << 40) + (byte1 << 32);
    }

    public float readFloat() {
        return Float.intBitsToFloat(readInt());
    }

    public double readDouble() {
        return Double.longBitsToDouble(readLong());
    }

    public String readAsciiString(int n) {
        byte[] ret = new byte[n];
        for (int x = 0; x < n; x++) {
            ret[x] = readByte();
        }
        return new String(ret, Charset.forName(MapleLanguageType.LANGUAGE_PT_BR.getAscii()));
    }

    public String readNullTerminatedAsciiString() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte b = 1;
        while (b != 0) {
            b = readByte();
            baos.write(b);
        }
        byte[] buf = baos.toByteArray();
        return new String(buf, Charset.forName(MapleLanguageType.LANGUAGE_PT_BR.getAscii()));
    }

    public long getBytesRead() {
        return this.bs.getBytesRead();
    }

    public String readMapleAsciiString() {
        return readAsciiString(readShort());
    }

    public String newreadMapleAsciiString() {
        return readAsciiString(readByte());
    }

    public Point readPos() {
        int x = readShort();
        int y = readShort();
        return new Point(x, y);
    }

    public Point readIntPos() {
        int x = readInt();
        int y = readInt();
        return new Point(x, y);
    }

    public byte[] read(int num) {
        byte[] ret = new byte[num];
        for (int x = 0; x < num; x++) {
            ret[x] = readByte();
        }
        return ret;
    }

    public void skip(int num) {
        for (int x = 0; x < num; x++) {
            readByte();
        }
    }

    public long available() {
        return this.bs.available();
    }

    @Override
    public String toString() {
        return this.bs.toString();
    }

    public void seek(long offset) {
        try {
            this.bs.seek(offset);
        } catch (IOException e) {
            System.err.println("");
            if (!ServerProperties.Misc.RELEASE) {
                e.printStackTrace();
            }
        }
    }

    public long getPosition() {
        try {
            return this.bs.getPosition();
        } catch (Exception e) {
            System.err.println("");
            if (!ServerProperties.Misc.RELEASE) {
                e.printStackTrace();
            }
        }
        return -1L;
    }
}
