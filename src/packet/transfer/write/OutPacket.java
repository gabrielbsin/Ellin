package packet.transfer.write;

import tools.HexTool;

public class OutPacket implements Cloneable {

    private byte[] data;
    private Runnable onSend;

    public OutPacket(final byte[] data) {
        this.data = data;
    }

    public final byte[] getBytes() {
        return data;
    }

    public final Runnable getOnSend() {
        return onSend;
    }

    public void setOnSend(final Runnable onSend) {
        this.onSend = onSend;
    }

    @Override
    public String toString() {
        return HexTool.toString(data);
    }
    
    @Override
    public OutPacket clone() throws CloneNotSupportedException{    
      return (OutPacket) super.clone();  
    }   
}
