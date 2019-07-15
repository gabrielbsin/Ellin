package server.movement;

import java.awt.Point;
import packet.transfer.write.WritingPacket;


public class ChairMovement extends AbstractLifeMovement  {
	
    private int unk;

    public ChairMovement(int type, Point position, int duration, int newstate) {
        super(type, position, duration, newstate);
    }

    public int getUnk() {
        return unk;
    }

    public void setUnk(int unk) {
        this.unk = unk;
    }

    @Override
    public void serialize(WritingPacket wp) {
        wp.write(getType());
        wp.writeShort(getPosition().x);
        wp.writeShort(getPosition().y);
        wp.writeShort(unk);
        wp.write(getNewstate());
        wp.writeShort(getDuration());
    }
}

