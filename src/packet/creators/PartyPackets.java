/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package packet.creators;

import community.MapleParty;
import community.MaplePartyCharacter;
import community.MaplePartyOperation;
import constants.MapConstants;
import handling.channel.handler.ChannelHeaders.PartyHeaders;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import packet.opcode.SendPacketOpcode;
import packet.transfer.write.OutPacket;
import packet.transfer.write.WritingPacket;
import server.maps.MapleDoor;
import server.maps.object.FieldDoorObject;
import tools.StringUtil;

public class PartyPackets {
    
    public static OutPacket UpdateParty(int forChannel, MapleParty party, MaplePartyOperation op, MaplePartyCharacter target) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.PARTY_OPERATION.getValue());
        switch (op) {
            case DISBAND:
            case EXPEL:
            case LEAVE:
                wp.write(0xC);
                wp.writeInt(party.getId());
                wp.writeInt(target.getId());
                if (op == MaplePartyOperation.DISBAND) {
                    wp.write(0);
                    wp.writeInt(party.getId());
                } else {
                    wp.write(1);
                    if (op == MaplePartyOperation.EXPEL) {
                        wp.writeBool(true);
                    } else {
                        wp.writeBool(false);
                    }
                    wp.writeMapleAsciiString(target.getName());
                    AddPartyStatus(forChannel, party, wp, false);
                }
                break;
            case JOIN:
                wp.write(0xF);
                wp.writeInt(party.getId());
                wp.writeMapleAsciiString(target.getName());
                AddPartyStatus(forChannel, party, wp, false);
                break;
            case SILENT_UPDATE:
            case LOG_ONOFF:
                wp.write(0x7);
                wp.writeInt(party == null ? 0 : party.getId());
                AddPartyStatus(forChannel, party, wp, false);
                break;
            case CHANGE_LEADER:
                wp.write(0x1A);
                wp.writeInt(target.getId());
                wp.write(1);
                break;
        }
        return wp.getPacket();
    }
    
    private static void AddPartyStatus(int forChannel, MapleParty party, WritingPacket wp, boolean leaving) {
        List<MaplePartyCharacter> partymembers = new ArrayList<>(party.getMembers());
        while (partymembers.size() < 6) {
            partymembers.add(new MaplePartyCharacter());
        }
        for (MaplePartyCharacter partychar : partymembers) {
            wp.writeInt(partychar.getId());
        }
        for (MaplePartyCharacter partychar : partymembers) {
            wp.writeAsciiString(StringUtil.getRightPaddedStr(partychar.getName(), '\0', 13));
        }
        for (MaplePartyCharacter partychar : partymembers) {
            wp.writeInt(partychar.getJobId());
        }
        for (MaplePartyCharacter partychar : partymembers) {
            wp.writeInt(partychar.getLevel());
        }
        for (MaplePartyCharacter partychar : partymembers) {
            if (partychar.isOnline()) {
                wp.writeInt(partychar.getChannel() - 1);
            } else {
                wp.writeInt(-2);
            }
        }
        wp.writeInt(party.getLeader().getId());
        for (MaplePartyCharacter partychar : partymembers) {
            if (partychar.isOnline()) {
                wp.writeInt(partychar.getMapId());
            } else {
                wp.writeInt(-2);
            }
        }
        for (MaplePartyCharacter partychar : partymembers) {
            if (partychar.getChannel() == forChannel && !leaving) {
                if (partychar.getDoors().size() > 0) {
                    boolean deployedPortal = false;
                    for (MapleDoor door : partychar.getDoors()) {
                        if(door.getOwnerId() == partychar.getId()) {
                            FieldDoorObject mdo = door.getTownDoor();
                            wp.writeInt(mdo.getTown().getId());
                            wp.writeInt(mdo.getArea().getId());
                            wp.writeInt(mdo.getPosition().x);
                            wp.writeInt(mdo.getPosition().y);
                            deployedPortal = true;
                        }
                    }

                    if(!deployedPortal) {
                        wp.writeInt(MapConstants.NULL_MAP);
                        wp.writeInt(MapConstants.NULL_MAP);
                        wp.writeInt(0);
                        wp.writeInt(0);
                    }
                } else {
                    wp.writeInt(MapConstants.NULL_MAP);
                    wp.writeInt(MapConstants.NULL_MAP);
                    wp.writeInt(0);
                    wp.writeInt(0);
                }
            } else {
                wp.writeInt(MapConstants.NULL_MAP);
                wp.writeInt(MapConstants.NULL_MAP);
                wp.writeInt(0);
                wp.writeInt(0);
            }
        }
    }
    
    public static OutPacket PartyCreated(MaplePartyCharacter partychar) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.PARTY_OPERATION.getValue());
        wp.writeShort(PartyHeaders.PARTY_CREATED);
        wp.writeInt(partychar.getId());
        if (partychar.getDoors().size() > 0) {
            boolean deployedPortal = false;

            for (MapleDoor door : partychar.getDoors()) {
                if (door.getOwnerId() == partychar.getId()) {
                    FieldDoorObject mdo = door.getAreaDoor();
                    wp.writeInt(mdo.getTo().getId());
                    wp.writeInt(mdo.getFrom().getId());
                    wp.writeInt(mdo.getPosition().x);
                    wp.writeInt(mdo.getPosition().y);
                    deployedPortal = true;
                }
            }

            if (!deployedPortal) {
                wp.writeInt(999999999);
                wp.writeInt(999999999);
                wp.writeInt(0);
                wp.writeInt(0);
            }
        } else {
            wp.writeInt(999999999);
            wp.writeInt(999999999);
            wp.writeInt(0);
            wp.writeInt(0);
        }
        return wp.getPacket();
    }
    
    public static OutPacket PartyStatusMessage(byte opCode) {
        WritingPacket wp = new WritingPacket(3);
        wp.writeShort(SendPacketOpcode.PARTY_OPERATION.getValue());
        wp.write(opCode);
        return wp.getPacket();
    }

    public static OutPacket PartyInvite(int partyID, String name) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.PARTY_OPERATION.getValue());
        wp.write(PartyHeaders.PARTY_INVITE);
        wp.writeInt(partyID);
        wp.writeMapleAsciiString(name);
        wp.write(0);
        return wp.getPacket();
    }

    public static OutPacket PartyInviteRejected(String name) {
        WritingPacket wp = new WritingPacket(5 + name.length());
        wp.writeShort(SendPacketOpcode.PARTY_OPERATION.getValue());
        wp.write(PartyHeaders.PARTY_INVITE_DENIED);
        wp.writeMapleAsciiString(name);
        return wp.getPacket();
    }
    
    public static OutPacket PartyPortal(int townID, int targetID, Point position) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.PARTY_OPERATION.getValue());
        wp.writeShort(0x22);
        wp.writeInt(townID);
        wp.writeInt(targetID);
        wp.writePos(position);
        return wp.getPacket();
    }

    public static OutPacket UpdatePartyMemberHP(int cID, int curHP, int maxHP) {
        WritingPacket wp = new WritingPacket();
        wp.writeShort(SendPacketOpcode.UPDATE_PARTYMEMBER_HP.getValue());
        wp.writeInt(cID);
        wp.writeInt(curHP);
        wp.writeInt(maxHP);
        return wp.getPacket();
    }
}
