/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package handling.channel.handler;

import client.player.Player;
import client.Client;
import handling.mina.PacketReader;
import community.MapleParty;
import community.MaplePartyCharacter;
import community.MaplePartyOperation;
import static handling.channel.handler.ChannelHeaders.PartyHeaders.*;
import handling.world.service.PartyService;
import packet.creators.PacketCreator;
import packet.creators.PartyPackets;

/**
 *
 * @author GabrielSin
 */
public class PartyHandler {
   
    public static void handlePartyOperation(PacketReader packet, Client c) {
        Player p = c.getPlayer();
        if (p == null || p.getMap() == null) {
           return;
        }
        switch (packet.readByte()) {
            case PARTY_CREATE:
                CreateParty(packet, p);
                break;
            case PARTY_LEAVE:
                LeaveParty(packet, p);
                break;
            case PARTY_ACCEPT_INVITE:
                AcceptInviteParty(packet, p);
                break;
            case PARTY_INVITE:
                InvitePartyPlayer(packet, p);
                break;
            case PARTY_EXPEL:
                ExpelPartyPlayer(packet, p);
                break;
            case PARTY_CHANGE_LEADER:
                ChangePartyLeader(packet, p);
                break;   
        }
    }
    
    public static void CreateParty(PacketReader packet, Player p) {
        MapleParty party = p.getParty();
        MaplePartyCharacter partyPlayer = p.getMPC();
        if (party == null) {
            party = PartyService.createParty(partyPlayer);
            if (party == null) {
                p.announce(PartyPackets.PartyStatusMessage(NOT_IN_PARTY));
                return;
            }
            p.setParty(party);
            p.setMPC(partyPlayer);
            p.announce(PartyPackets.PartyCreated(p.getMPC()));
        } else {
            p.announce(PartyPackets.PartyStatusMessage(ALREADY_IN_PARTY));
        }
    }
    
    public static void LeaveParty(PacketReader packet, Player p) {
        MapleParty party = p.getParty();
        MaplePartyCharacter partyPlayer = p.getMPC();
        if (party != null) {
            if (partyPlayer.equals(party.getLeader())) { 
                PartyService.updateParty(party.getId(), MaplePartyOperation.DISBAND, partyPlayer);
                if (p.getEventInstance() != null) {
                    p.getEventInstance().disbandParty();
                }
            } else {
                PartyService.updateParty(party.getId(), MaplePartyOperation.LEAVE, partyPlayer);
                if (p.getEventInstance() != null) {
                    p.getEventInstance().leftParty(p);
                }
            }
        } else {
           p.announce(PartyPackets.PartyStatusMessage(NOT_IN_PARTY));
        }
       p.setParty(null);
    }
    
    public static void AcceptInviteParty(PacketReader packet, Player p) {
        MapleParty party = p.getParty();
        int partyID = packet.readInt();
        if (party  == null) {
            party = PartyService.getParty(partyID);
            if (party != null) {
                if (party.getMembers().size() < 6) {
                    MaplePartyCharacter partyPlayer = new MaplePartyCharacter(p);
                    PartyService.updateParty(party.getId(), MaplePartyOperation.JOIN, partyPlayer);
                    p.receivePartyMemberHP();
                    p.updatePartyMemberHP();
                } else {
                    p.announce(PartyPackets.PartyStatusMessage(PARTY_FULL));
                }
            } else {
                p.announce(PacketCreator.ServerNotice(5, "The party you are trying to enter does not exist."));
            }
        } else {
            p.announce(PartyPackets.PartyStatusMessage(ALREADY_IN_PARTY));
        }
    }
    
    public static void InvitePartyPlayer(PacketReader packet, Player p) {
        String name = packet.readMapleAsciiString();
        MapleParty party = p.getParty();
        Player invited = p.getClient().getChannelServer().getPlayerStorage().getCharacterByName(name);
        
        if (invited == null) {
            p.announce(PartyPackets.PartyStatusMessage(PARTY_CANNOT_FIND));
            return;
        }
        if (invited.getParty() != null) {
            p.announce(PartyPackets.PartyStatusMessage(ALREADY_IN_PARTY));
            return;
        }
        if (party == null) {
            p.announce(PartyPackets.PartyStatusMessage(NOT_IN_PARTY));
            return;
        }
        if (party.getMembers().size() < 6) {
            invited.getClient().getSession().write(PartyPackets.PartyInvite(p.getPartyId(), p.getName()));
        } else {
            p.announce(PartyPackets.PartyStatusMessage(PARTY_FULL)); 
        }
    }
    
    public static void ExpelPartyPlayer(PacketReader packet, Player p) {
        MapleParty party = p.getParty();
        MaplePartyCharacter partyPlayer = p.getMPC();
        int cid = packet.readInt();
        
        if (party == null || partyPlayer == null) {
            p.announce(PartyPackets.PartyStatusMessage(NOT_IN_PARTY));
            return;
        }
        if (!partyPlayer.equals(party.getLeader())) {
            p.announce(PartyPackets.PartyStatusMessage(NOT_IN_PARTY));
            return;
        }
        
        final MaplePartyCharacter expelled = party.getMemberById(cid);
        if (expelled != null) { 
            PartyService.updateParty(party.getId(), MaplePartyOperation.EXPEL, expelled);
            if (p.getEventInstance() != null && expelled.isOnline()) {
                p.getEventInstance().leftParty(expelled.getPlayer());
            }
        }
    }
    
    public static void ChangePartyLeader(PacketReader packet, Player p) {
        final int newLeaderID = packet.readInt();
        MapleParty party = p.getParty();
        MaplePartyCharacter partyPlayer = p.getMPC();
        
        if (party == null) {
            p.announce(PartyPackets.PartyStatusMessage(NOT_IN_PARTY));
            return;
        }
        if (!partyPlayer.equals(party.getLeader())) {
             p.announce(PartyPackets.PartyStatusMessage(NOT_IN_PARTY));
            return;
        }
        
        final MaplePartyCharacter newLeadr = party.getMemberById(newLeaderID);
        final Player cfrom = p.getClient().getChannelServer().getPlayerStorage().getCharacterById(newLeaderID);
        
        if (newLeadr != null && cfrom.getMapId() == p.getMapId()) { 
            PartyService.updateParty(party.getId(), MaplePartyOperation.CHANGE_LEADER, newLeadr);
        } else {
            p.dropMessage(5, "The Party Leader can only be handed over to the party member in the same map.");
        }  
    }

    public static void PartyResponse(PacketReader packet, Client c) {
        packet.readByte();
        String from = packet.readMapleAsciiString();
        String to = packet.readMapleAsciiString(); 
        Player cfrom = c.getChannelServer().getPlayerStorage().getCharacterByName(from);
        if (cfrom != null) {
            cfrom.getClient().getSession().write(PartyPackets.PartyInviteRejected(to));
        }
    }
}
