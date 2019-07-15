/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package handling.world.service;

import community.MapleParty;
import community.MaplePartyCharacter;
import community.MaplePartyOperation;
import database.DatabaseConnection;
import handling.channel.ChannelServer;
import handling.world.World;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import packet.creators.PacketCreator;
import packet.creators.PartyPackets;
import packet.transfer.write.OutPacket;
import client.player.Player;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import scripting.event.EventInstanceManager;

public class PartyService {

    private static Map<Integer, MapleParty> parties = new HashMap<>();
    private static final AtomicInteger runningPartId = new AtomicInteger(1);
    private static final ReentrantReadWriteLock partyLock = new ReentrantReadWriteLock();

    static {
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("UPDATE `characters` SET `party` = -1")) {
            ps.executeUpdate();
        } catch (SQLException e) {
        }
    }

    public static void partyChat(int partyid, String chattext, String namefrom) {
        partyChat(partyid, chattext, namefrom, 1);
    }

    public static void partyChat(int partyid, String chattext, String namefrom, int mode) {
        MapleParty party = getParty(partyid);
        if (party == null) {
            return;
        }

        party.getMembers().forEach((partychar) -> {
            int ch = FindService.findChannel(partychar.getName());
            if (ch > 0) {
                Player p = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterByName(partychar.getName());
                if (p != null && !p.getName().equalsIgnoreCase(namefrom)) {
                    p.getClient().write(PacketCreator.PrivateChatMessage(namefrom, chattext, mode));
                }
            }
        });
    }

    public static void partyPacket(int partyid, OutPacket packet, MaplePartyCharacter exception) {
        MapleParty party = getParty(partyid);
        if (party == null) {
            return;
        }

        party.getMembers().forEach((partychar) -> {
            int ch = FindService.findChannel(partychar.getName());
            if (ch > 0 && (exception == null || partychar.getId() != exception.getId())) {
                Player chr = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterByName(partychar.getName());
                if (chr != null) {
                    chr.getClient().getSession().write(packet);
                }
            }
        });
    }

    public static void partyMessage(int partyid, String chattext) {
        MapleParty party = getParty(partyid);
        if (party == null) {
            return;
        }

        party.getMembers().forEach((partychar) -> {
            int ch = FindService.findChannel(partychar.getName());
            if (ch > 0) {
                Player p = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterByName(partychar.getName());
                if (p != null) {
                    p.dropMessage(5, chattext);
                }
            }
        });
    }

    public static void updateParty(int partyid, MaplePartyOperation operation, MaplePartyCharacter target) {
        MapleParty party = getParty(partyid);
        if (party == null) {
            return;
        }
        switch (operation) {
            case JOIN:
                party.addMember(target);
                break;
            case EXPEL:
            case LEAVE:
                party.removeMember(target);
                break;
            case DISBAND:
                disbandParty(partyid);  
                break;
            case SILENT_UPDATE:
            case LOG_ONOFF:
                party.updateMember(target);
                break;
            case CHANGE_LEADER:
                Player mc = party.getLeader().getPlayer();
                EventInstanceManager eim = mc.getEventInstance();

                if (eim != null && eim.isEventLeader(mc)) {
                    eim.changedLeader(target.getPlayer());
                }

                party.setLeader(target);
                break;
            default:
                throw new RuntimeException("Unhandeled updateParty operation " + operation.name());
        }

        if (operation == MaplePartyOperation.LEAVE || operation == MaplePartyOperation.EXPEL) {
            int ch = FindService.findChannel(target.getName());
            if (ch > 0) {
                Player p = World.getStorage(ch).getCharacterByName(target.getName());
                if (p != null) {
                    p.setParty(null);
                    p.setMPC(null);
                    p.getClient().getSession().write(PartyPackets.UpdateParty(p.getClient().getChannel(), party, operation, target));
                }
            }
        }
        if (party.getMembers().size() <= 0) {
            disbandParty(partyid);
        }
        party.getMembers().stream().filter((partyChar) -> !(partyChar == null)).forEachOrdered((partyChar) -> {
            int ch = FindService.findChannel(partyChar.getName());
            if (ch > 0) {
                Player chr = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterByName(partyChar.getName());
                if (chr != null) {
                    if (operation == MaplePartyOperation.DISBAND) {
                        chr.setParty(null);
                        chr.setMPC(null);

                    } else {
                        chr.setParty(party);
                        chr.setMPC(partyChar);
                    }
                    chr.getClient().write(PartyPackets.UpdateParty(chr.getClient().getChannel(), party, operation, target));
                }
            }
        });
    }

    public static MapleParty createParty(MaplePartyCharacter chrfor) {
        int partyid = runningPartId.getAndIncrement();
        MapleParty party = new MapleParty(partyid, chrfor);
        partyLock.writeLock().lock();
        try {
            parties.put(party.getId(), party);
        } finally {
            partyLock.writeLock().unlock();
        }
        return party;
    }

    public static MapleParty createPartyAndAdd(MaplePartyCharacter chrfor) {
        MapleParty party = new MapleParty(runningPartId.getAndIncrement(), chrfor);
        partyLock.writeLock().lock();
        try {
            parties.put(party.getId(), party);
        } finally {
            partyLock.writeLock().unlock();
        }
        return party;
    }

    public static MapleParty getParty(int partyid) {
        partyLock.writeLock().lock();
        try {
            return parties.get(partyid);
        } finally {
            partyLock.writeLock().unlock();
        }
    }

    public static MapleParty disbandParty(int partyid) {
        partyLock.writeLock().lock();
        try {
            MapleParty ret = parties.remove(partyid);
            if (ret == null) {
                return null;
            }
            return ret;
        }   finally {
            partyLock.writeLock().unlock();
        }
    }
}
