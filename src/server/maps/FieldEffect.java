package server.maps;

import client.Client;
import packet.creators.EffectPackets;
import packet.transfer.write.OutPacket;

public class FieldEffect {
    
    private final String msg;
    private final int itemId;
    private boolean active = true;

    public FieldEffect(String msg, int itemId) {
        this.msg = msg;
        this.itemId = itemId;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public OutPacket makeDestroyData() {
        return EffectPackets.RemoveMapEffect();
    }

    public OutPacket makeStartData() {
        return EffectPackets.StartMapEffect(msg, itemId, active);
    }

    public void sendStartData(Client client) {
        client.getSession().write(makeStartData());
    }
}
