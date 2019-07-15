/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package packet.creators;

import client.Client;
import client.player.Player;
import client.player.PlayerJob;
import constants.ServerProperties;
import handling.login.LoginBalloon;
import handling.login.LoginServer;
import static handling.login.handler.CharLoginHeaders.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import packet.opcode.SendPacketOpcode;
import packet.transfer.write.OutPacket;
import packet.transfer.write.WritingPacket;
import tools.HexTool;

/**
 *
 * @author GabrielSin
 */
public class LoginPackets {
    
    /**
     * Sends a hello packet.
     * 
     * @param mapleVersion The maple client version.
     * @param sendIv the IV used by the server for sending
     * @param recvIv the IV used by the server for receiving
     * @param testServer 
     * @return
     */
    public static OutPacket GetHello(short mapleVersion, byte[] sendIv, byte[] recvIv, boolean testServer) {
        WritingPacket mplew = new WritingPacket(16);
        mplew.writeShort(0x0D);
        mplew.writeShort(ServerProperties.World.MAPLE_VERSION);
        mplew.writeMapleAsciiString("");
        mplew.write(recvIv);
        mplew.write(sendIv);
        mplew.write(SERVER_GLOBAL);
        return mplew.getPacket();
    }
    
    /**
    * Sends a ping packet.
    * @return The packet.
    */
    public static OutPacket PingMessage() {
        WritingPacket wp = new WritingPacket(2);
        wp.writeShort(SendPacketOpcode.PING.getValue());
        return wp.getPacket();
    }
    
    /**
     * Gets a login failed packet.
     * 
     * Possible values for <code>reason</code>:<br>
     * 3: ID deleted or blocked<br>
     * 4: Incorrect password<br>
     * 5: Not a registered id<br>
     * 6: System error<br>
     * 7: Already logged in<br>
     * 8: System error<br>
     * 9: System error<br>
     * 10: Cannot process so many connections<br>
     * 11: Only users older than 20 can use this channel<br>
     * 13: Unable to log on as master at this ip<br>
     * 14: Wrong gateway or personal info and weird korean button<br>
     * 15: Processing request with that korean button!<br>
     * 16: Please verify your account through email...<br>
     * 17: Wrong gateway or personal info<br>
     * 21: Please verify your account through email...<br>
     * 23: License agreement<br>
     * 25: Maple Europe notice =[<br>
     * 27: Some weird full client notice, probably for trial versions<br>
     * 
     * @param reason The reason logging in failed.
     * @return The login failed packet.
     */
    public static OutPacket GetLoginStatus(int reason) {
        WritingPacket wp = new WritingPacket(16);
        wp.writeShort(SendPacketOpcode.LOGIN_STATUS.getValue());
        wp.writeInt(reason);
        wp.writeShort(0);
        return wp.getPacket();
    }
    
    public static OutPacket GetPermBan(byte reason) {
        WritingPacket wp = new WritingPacket(16);
        wp.writeShort(SendPacketOpcode.LOGIN_STATUS.getValue());
        wp.writeShort(0x02);
        wp.write(0x0);
        wp.write(reason);
        wp.write(HexTool.getByteArrayFromHexString("01 01 01 01 00"));
        return wp.getPacket();
    }
    
    public static OutPacket GetTempBan(long timestampTill, byte reason) {
        WritingPacket wp = new WritingPacket(17);
        wp.writeShort(SendPacketOpcode.LOGIN_STATUS.getValue());
        wp.write(0x02);
        wp.write(HexTool.getByteArrayFromHexString("00 00 00 00 00"));
        wp.write(reason);
        wp.writeLong(timestampTill);
        return wp.getPacket();
    }
    
    /**
    * Gets a successful authentication and PIN Request packet.
    * @param c
    * @return The PIN request packet.
    */
    public static OutPacket GetAuthSuccess(Client c) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.LOGIN_STATUS.getValue());
        wp.write(0);
        wp.write(0);
        wp.writeInt(0);
        wp.writeInt(c.getAccountID());
        wp.write(c.getGender()); 
        wp.write(c.isGm() ? 1 : 0);
        wp.write(0);
        wp.writeMapleAsciiString(c.getAccountName());
        wp.write(0); 
        wp.write(0);
        wp.writeLong(0);
        wp.writeLong(0);
        wp.writeInt(8); 
        return wp.getPacket();
    }
    
    /**
     * 
     * @param cid
     * @param state
     * @return
     */ 
     public static OutPacket DeleteCharResponse(int cid, int state) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.DELETE_CHAR_RESPONSE.getValue());
        wp.writeInt(cid);
        wp.write(state);
        return wp.getPacket();
    }
     
    public static OutPacket AddNewCharEntry(Player chr) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.ADD_NEW_CHAR_ENTRY.getValue());
        wp.write(0);
        AddCharEntry(wp, chr);
        return wp.getPacket();
    }
    
    public static OutPacket AddNewCharEntry(Player p, boolean worked) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.ADD_NEW_CHAR_ENTRY.getValue());
        wp.write(worked ? 0 : 1);
        AddCharEntry(wp, p);
        return wp.getPacket();
    }
    
    /**
    * Adds an entry for a character to an existing
    * WritingPacket.
    * 
    * @param mplew The MaplePacketLittleEndianWrite instance to write the stats to.
    * @param chr The character to add.
    */
    private static void AddCharEntry(WritingPacket wp, Player chr) {
        PacketCreator.addCharStats(wp, chr);
        PacketCreator.AddCharLook(wp, chr, false);
        if (!chr.getJob().isA(PlayerJob.GM)) {
            wp.writeBool(true);
            wp.writeInt(chr.getWorldRank());
            wp.writeInt(chr.getWorldRankChange()); 
            wp.writeInt(chr.getJobRank());
            wp.writeInt(chr.getJobRankChange());
        } else {
            wp.writeBool(false);
        }
    }   
    
    /**
    * Gets a packet detailing a PIN operation.
    * Possible values for <code>mode</code>:<br>
    * 0 - PIN was accepted<br>
    * 1 - Register a new PIN<br>
    * 2 - Invalid pin / Reenter<br>
    * 3 - Connection failed due to system error<br>
    * 4 - Enter the pin
    * 
    * @param mode The mode.
    * @return 
    */
    public static OutPacket PinOperation(byte mode) {
        WritingPacket wp = new WritingPacket(3);
        wp.writeShort(SendPacketOpcode.PIN_OPERATION.getValue());
        wp.write(mode);
        return wp.getPacket();
    }
    
     public static OutPacket PinRegistered() { 
        WritingPacket mplew = new WritingPacket(); 
        mplew.writeShort(SendPacketOpcode.PIN_ASSIGNED.getValue()); 
        mplew.write(PIN_ACCEPTED); 
        return mplew.getPacket(); 
    }

    
    /**
    * Gets a packet requesting the client enter a PIN.
     * @param status
    * @return The request PIN packet.
    */
    public static OutPacket RequestPinStatus(byte status) {
        switch(status) {
            case 0:
                return PinOperation(PIN_ACCEPTED);
            case 1: 
                return PinOperation(PIN_REGISTER);
            case 2: 
                return PinOperation(PIN_REJECTED);
            case 4: 
                return PinOperation(PIN_REQUEST);
                
        }
        return null;
    }
    
    /**
    * Gets a packet detailing a server and its channels.
     * @param serverId The index of the server to create information about.
    * @param serverName The name of the server.
    * @param channelLoad Load of the channel - 1200 seems to be max.
    * @return The server info packet.
    */
    public static OutPacket getServerList(int serverId, String serverName, Map<Integer, Integer> channelLoad) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.SERVERLIST.getValue());
        wp.write(serverId);
        wp.writeMapleAsciiString(serverName);
        wp.write(LoginServer.getFlag());
        wp.writeMapleAsciiString(LoginServer.getEventMessage());
        wp.write(0x64);
        wp.write(0x0);
        wp.write(0x64);
        wp.write(0x0);
        wp.write(0x0);
        int lastChannel = 1;
        Set<Integer> channels = channelLoad.keySet();
        for (int i = 30; i > 0; i--) {
            if (channels.contains(i)) {
                lastChannel = i;
                break;
            }
        }
        wp.write(lastChannel);
        int load;
        for (int i = 1; i <= lastChannel; i++) {
            if (channels.contains(i)) {
                load = channelLoad.get(i);
            } else {
                load = 1200;
            }
            wp.writeMapleAsciiString(serverName + "-" + i);
            wp.writeInt(load);
            wp.write(1);
            wp.writeShort(i - 1);
        }
        wp.writeShort(LoginBalloon.getBalloons().size());
        for (LoginBalloon balloon : LoginBalloon.getBalloons()) {
            wp.writeShort(balloon.nX);
            wp.writeShort(balloon.nY);
            wp.writeMapleAsciiString(balloon.sMessage);
        }
        return wp.getPacket();  
    }
    
    public static OutPacket getEndOfServerList() {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.SERVERLIST.getValue());
        wp.write(0xFF);
        return wp.getPacket();
    }
    
    /**
    * Gets a packet detailing a server status message.
    * Possible values for <code>status</code>:<br>
    * 0 - Normal<br>
    * 1 - Highly populated<br>
    * 2 - Full
    * 
    * @param status The server status.
    * @return The server status packet.
    */
    public static OutPacket GetServerStatus(int status) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.SERVERSTATUS.getValue());
        wp.write(status);
        wp.write(1);
        return wp.getPacket();
    }
    
    /**
    * Gets a packet telling the client the IP of the channel server.
    * @param port The port the channel is on.
    * @param clientId The ID of the client.
    * @return The server IP packet.
    */
    public static OutPacket GetServerIP(int port, int clientId) {
        WritingPacket wp = new WritingPacket();

        wp.writeShort(SendPacketOpcode.SERVER_IP.getValue());
        wp.writeShort(0);
        try {
            wp.write(InetAddress.getByName(ServerProperties.World.HOST).getAddress());
        } catch (UnknownHostException e) {
            wp.write(ServerProperties.World.HOST_BYTE);
        }
        wp.writeShort(port);
        wp.writeInt(clientId); 
        wp.writeZeroBytes(5);
        return wp.getPacket();
    }
    
    public static OutPacket CharNameResponse(String charname, boolean nameUsed) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.CHAR_NAME_RESPONSE.getValue());
        wp.writeMapleAsciiString(charname);
        wp.writeBool(nameUsed);
        return wp.getPacket();
    }
    
    /**
    * Gets the response to a relog request.
    * 
    * @return The relog response packet.
    */
    public static OutPacket GetRelogResponse() {
        WritingPacket wp = new WritingPacket(3);
        wp.writeShort(SendPacketOpcode.RELOG_RESPONSE.getValue());
        wp.write(1);
        return wp.getPacket();
    }
    
    public static OutPacket ShowAllCharacter(int chars) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.ALL_CHARLIST.getValue());
        wp.write(1);
        wp.writeInt(chars);
        wp.writeInt(chars + (3 - chars % 3));
        return wp.getPacket();
    }
    
    public static OutPacket ShowAllCharacterInfo(int worldID, List<Player> chars) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.ALL_CHARLIST.getValue());
        wp.write(0);
        wp.write(worldID);
        wp.write(chars.size());
        for (Player chr : chars) {
            AddCharEntry(wp, chr);
        }
        return wp.getPacket();
    }
    
    /**
    * Gets a packet with a list of characters.
    * @param c The MapleClient to load characters of.
     * @param serverID The ID of the server requested.
    * @return The character list packet.
    */
    public static OutPacket GetCharList(Client c, int serverID) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.CHARLIST.getValue());
        wp.write(0);
        List<Player> chars = c.loadCharacters(serverID);
        wp.write((byte) chars.size());
        for (Player chr : chars) {
             AddCharEntry(wp, chr);
        }
        wp.writeInt(c.getCharacterSlots());
        return wp.getPacket();
    }   
}
