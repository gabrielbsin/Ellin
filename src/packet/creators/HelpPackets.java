/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package packet.creators;

import java.util.List;
import packet.transfer.write.OutPacket;
import packet.transfer.write.WritingPacket;
import server.movement.LifeMovementFragment;
import tools.HexTool;
import tools.KoreanDateUtil;

public class HelpPackets {
    
    public static void SerializeMovementList(WritingPacket wp, List<LifeMovementFragment> moves) {
        wp.write(moves.size());
        for (LifeMovementFragment move : moves) {
            move.serialize(wp);
        }
    }
    
    public static void AddExpirationTime(WritingPacket wp, long time, boolean showexpirationtime) {
        if (time != 0) {
            wp.writeInt(KoreanDateUtil.getItemTimestamp(time));
        } else {
            wp.writeInt(400967355);
        }
        wp.write(showexpirationtime ? 1 : 2);
    }
    
    public static OutPacket GetPacketFromHexString(String hex) {
        return new OutPacket(HexTool.getByteArrayFromHexString(hex));
    }
    
}
