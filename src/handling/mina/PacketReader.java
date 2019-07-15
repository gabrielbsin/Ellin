package handling.mina;

import java.awt.Point;
import tools.HexTool;

public class PacketReader {
    
    private final byte[] message;
    private int index = 0;

    public PacketReader(byte[] message) {
        this.message = message;
    }

    public int getBytesRead() {
        return index;
    }

    public int size() {
        return message.length;
    }

    public int available() {
	return message.length - index;
    }

    public byte[] read(int num) {
	byte[] ret = new byte[num];
	for (int i = 0; i < num; i++)
	    ret[i] = readByte();
	return ret;
    }

    public void skip(int num) {
        index += num;
    }

    public byte readByte() {
        return (byte)(message[index++] & 0xFF);
    }

    private int readDatatype(int count) {
        int ret = 0;
        for (int i = 0; i < count; i++) {
             ret += (message[index] & 0xFF) << (8 * i);
             index++;
        }
        return ret;
    }
    
    public void seek(long offset) {
        index = (int) offset;
    }
    
    public short readShort() {
        return (short)readDatatype(2);
    }

    public int readInt() {
        return readDatatype(4);
    }
    
    public boolean readBool() {
	return (readByte() > 0);
    }

    public long readLong() {
        long ret = message[index++] & 0xFF;
        for (int i = 1; i <= 7; i++)
            ret += ((message[index++] & 0xFF) << 8*i);
        return ret;
    }

    public Point readPos() {
	return new Point(readShort(), readShort());
    }

    public String readMapleAsciiString() {
        int length = readShort();
        char[] charArray = new char[length];
        for (int i = 0; i < length; i++) {
            charArray[i] = (char)message[index++];
        }
        return String.valueOf(charArray);
    }
    
    public final String readAsciiString(final int n) {
        final char ret[] = new char[n];
        for (int x = 0; x < n; x++) {
            ret[x] = (char) readByte();
        }
        return new String(ret);
    }

    @Override
    public String toString() {
        return HexTool.toString(message);
    }
    
    public final int readUShort() {
	int quest = readShort();
        if (quest < 0) { 
            quest += 65536; 
        }
        return quest;
    }
}
