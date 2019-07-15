package packet.transfer.write;

import constants.TypeConstants.MapleLanguageType;
import tools.HexTool;
import java.awt.Point;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

public final class WritingPacket {

    private static final Charset maple = Charset.forName(MapleLanguageType.LANGUAGE_PT_BR.getAscii());
    private ByteArrayOutputStream baos;

    public WritingPacket() {
	this(32);
    }
    
    public WritingPacket(int size) {
	this.baos = new ByteArrayOutputStream(size);
    }
      
    public void write0(int i) {
	for (int x = 0; x < i; ++x) {
	    baos.write((byte) 0);
	}
    }
    public void Hex(String packet) {
        try {
            baos.write(HexTool.getByteArrayFromHexString(packet));
        } catch (IOException ex) {
            
        }
    }
    
    public void write(byte[] b) {
	for (int x = 0; x < b.length; ++x) {
	    baos.write(b[x]);
	}
    } 
    
     public void write(int... b) {
        for (int i = 0; i < b.length; i++) {
            baos.write(b[i]);
        }
    }
    
    public void skip(int b) {
        write(new byte[b]);
    }
    
    public void write(byte b) {
	baos.write(b);
    }
    
    public void write(int b) {
        if (b != -88888)
	baos.write((byte) b);
    }
    
    public final void fill(int val, int num) {
        for (int i = 0; i < num; i++) {
            baos.write(val);
        }
    }
    
    public void writeBool(final boolean b) {
        WritingPacket.this.write(b ? 1 : 0);
    }
    
    public void writeShort(int i) {
        if (i != -88888){
	baos.write((byte) (i & 0xFF));
	baos.write((byte) ((i >>> 8) & 0xFF));
        }
    }
    
    public void writeInt(int i) {
        if (i != -88888){
	baos.write((byte) (i & 0xFF));
	baos.write((byte) ((i >>> 8) & 0xFF));
	baos.write((byte) ((i >>> 16) & 0xFF));
	baos.write((byte) ((i >>> 24) & 0xFF));
        }
    }
    
    public void writeInt(long i) {
	baos.write((byte) (i & 0xFF));
	baos.write((byte) ((i >>> 8) & 0xFF));
	baos.write((byte) ((i >>> 16) & 0xFF));
	baos.write((byte) ((i >>> 24) & 0xFF));
    }
    
    public void writeAsciiString(String s) {
	write(s.getBytes(maple));
    }
    
    public void writeAsciiString(String s, int max) {
	write(s.getBytes(maple));
	for (int i = s.getBytes(maple).length; i < max; ++i) {
	    WritingPacket.this.write(0);
	}
    }
    
    public void writeMapleAsciiString(String s) {
	writeShort(s.getBytes(maple).length);
	writeAsciiString(s);
    }
    
    public void writeNullTerminatedAsciiString(String s) {
	writeAsciiString(s);
	WritingPacket.this.write(0);
    }
    
    public void writePos(Point s) {
	writeShort(s.x);
	writeShort(s.y);
    }
    
    public void writeLong(long l) {
        if (l != -88888){
	baos.write((byte) (l & 0xFF));
	baos.write((byte) ((l >>> 8) & 0xFF));
	baos.write((byte) ((l >>> 16) & 0xFF));
	baos.write((byte) ((l >>> 24) & 0xFF));
	baos.write((byte) ((l >>> 32) & 0xFF));
	baos.write((byte) ((l >>> 40) & 0xFF));
	baos.write((byte) ((l >>> 48) & 0xFF));
	baos.write((byte) ((l >>> 56) & 0xFF));
        }
    }
    
    public void writePos2(Point s) {
        writeInt(s.x);
        writeInt(s.y);
    }
 
    public OutPacket getPacket() {
	return new OutPacket(baos.toByteArray());
    }
    
    @Override
    public String toString() {
	return HexTool.toString(baos.toByteArray());
    }

   public final void writeZeroBytes(final int i) {
        for (int x = 0; x < i; x++) {
            baos.write((byte) 0);
        }
    }
}
