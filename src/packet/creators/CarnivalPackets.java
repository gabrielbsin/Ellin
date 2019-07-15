/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package packet.creators;

import client.player.Player;
import packet.opcode.SendPacketOpcode;
import packet.transfer.write.OutPacket;
import packet.transfer.write.WritingPacket;
import server.partyquest.mcpq.MCParty;

public class CarnivalPackets {
    
    public static OutPacket StartMonsterCarnival(Player p) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.MONSTER_CARNIVAL_START.getValue());
        wp.write(p.getTeam()); 
        wp.writeShort(p.getAvailableCP()); 
        wp.writeShort(p.getTotalCP()); 
        wp.writeShort(p.getMCPQField().getRed().getAvailableCP()); 
        wp.writeShort(p.getMCPQField().getRed().getTotalCP()); 
        wp.writeShort(p.getMCPQField().getBlue().getAvailableCP()); 
        wp.writeShort(p.getMCPQField().getBlue().getAvailableCP()); 
        wp.writeShort(0);
        wp.writeLong(0); 
        return wp.getPacket();
    }
    
    public static OutPacket UpdatePersonalCP(Player p) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.MONSTER_CARNIVAL_OBTAINED_CP.getValue());
        wp.writeShort(p.getAvailableCP()); 
        wp.writeShort(p.getTotalCP()); 
        return wp.getPacket();
    }
    
    public static OutPacket UpdatePartyCP(MCParty pty) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.MONSTER_CARNIVAL_PARTY_CP.getValue());
        wp.write(pty.getTeam().code);  
        wp.writeShort(pty.getAvailableCP());  
        wp.writeShort(pty.getTotalCP());  
        return wp.getPacket();
    }

    public static OutPacket PlayerSummoned(int tab, int num, String name) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.MONSTER_CARNIVAL_SUMMON.getValue());
        wp.write(tab);
        wp.write(num);
        wp.writeMapleAsciiString(name);
        return wp.getPacket();
    }
    
    public static OutPacket PlayerDiedMessage(Player p, int loss) { 
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.MONSTER_CARNIVAL_DIED.getValue());
        wp.write(p.getTeam()); 
        wp.writeMapleAsciiString(p.getName());
        wp.write(loss);
        return wp.getPacket();
    }  
    
    public static OutPacket CarnivalMessage(int message) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.MONSTER_CARNIVAL_MESSAGE.getValue());
        wp.write(message); 
        return wp.getPacket();
    }
    
    public static OutPacket CarnivalLeave(int team, String name) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.MONSTER_CARNIVAL_LEAVE.getValue());
        wp.write(0); 
        wp.write(team);  
        wp.writeMapleAsciiString(name); 
        return wp.getPacket();
    }
}
