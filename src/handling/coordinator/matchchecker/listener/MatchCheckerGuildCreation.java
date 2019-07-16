/*
    This file is part of the HeavenMS MapleStory Server
    Copyleft (L) 2016 - 2018 RonanLana
    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation version 3 as published by
    the Free Software Foundation. You may not use, modify or distribute
    this program under any other version of the GNU Affero General Public
    License.
    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.
    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package handling.coordinator.matchchecker.listener;

import client.player.Player;
import community.MapleGuild;
import community.MapleGuildCharacter;
import community.MapleParty;
import constants.GameConstants;
import handling.coordinator.matchchecker.AbstractMatchCheckerListener;
import handling.coordinator.matchchecker.MatchCheckerListenerRecipe;
import handling.world.service.GuildService;
import java.util.Set;
import packet.creators.GuildPackets;
import packet.transfer.write.OutPacket;


/**
 *
 * @author Ronan
 */
public class MatchCheckerGuildCreation implements MatchCheckerListenerRecipe {
    
    private static void broadcastGuildCreationDismiss(Set<Player> nonLeaderMatchPlayers) {
        for (Player p : nonLeaderMatchPlayers) {
          //  if (p.isLoggedinWorld()) {
                p.announce(GuildPackets.GenericGuildMessage((byte) 0x26));
           // }
        }
    }
    
    public static AbstractMatchCheckerListener loadListener() {
        return (new MatchCheckerGuildCreation()).getListener();
    }
    
    @Override
    public AbstractMatchCheckerListener getListener() {
        return new AbstractMatchCheckerListener() {
            
            @Override
            public void onMatchCreated(Player leader, Set<Player> nonLeaderMatchPlayers, String message) {
                OutPacket createGuildPacket = GuildPackets.ContractGuildMember(leader.getPartyId(), message, leader.getName());
                
                for (Player chr : nonLeaderMatchPlayers) {
                    //if (chr.isLoggedinWorld()) {
                        chr.announce(createGuildPacket);
                   // }
                }
            }
            
            @Override
            public void onMatchAccepted(int leaderid, Set<Player> matchPlayers, String message) {
                Player leader = null;
                for (Player chr : matchPlayers) {
                    if (chr.getId() == leaderid) {
                        leader = chr;
                        break;
                    }
                }
                
                if (leader == null /*|| !leader.isLoggedinWorld()*/) {
                    broadcastGuildCreationDismiss(matchPlayers);
                    return;
                }
                matchPlayers.remove(leader);
                
                if (leader.getGuildId() > 0) {
                    leader.dropMessage(1, "You cannot create a new Guild while in one.");
                    broadcastGuildCreationDismiss(matchPlayers);
                    return;
                }
                int partyid = leader.getPartyId();
                if (partyid == -1 || !leader.isPartyLeader()) {
                    leader.dropMessage(1, "You cannot establish the creation of a new Guild without leading a party.");
                    broadcastGuildCreationDismiss(matchPlayers);
                    return;
                }
                if (leader.getMapId() != 200000301) {
                    leader.dropMessage(1, "You cannot establish the creation of a new Guild outside of the Guild Headquarters.");
                    broadcastGuildCreationDismiss(matchPlayers);
                    return;
                }
                for (Player p : matchPlayers) {
                    if (leader.getMap().getCharacterById(p.getId()) == null) {
                        leader.dropMessage(1, "You cannot establish the creation of a new Guild if one of the members is not present here.");
                        broadcastGuildCreationDismiss(matchPlayers);
                        return;
                    }
                }
                if (leader.getMeso() < GameConstants.GUILD_CRETECOST) {
                    leader.dropMessage(1, "You do not have " + GameConstants.numberWithCommas(GameConstants.GUILD_CRETECOST) + " mesos to create a Guild.");
                    broadcastGuildCreationDismiss(matchPlayers);
                    return;
                }
                
                int gid = GuildService.createGuild(leader.getId(), message);
                if (gid == 0) {
                    leader.getClient().getSession().write(GuildPackets.GenericGuildMessage((byte) 0x1C)); // 0x23
                    broadcastGuildCreationDismiss(matchPlayers);
                    return;
                }
                final MapleGuild guild = GuildService.getGuild(leader.getGuildId(), leader.getClient().getChannel());
                leader.gainMeso(-GameConstants.GUILD_CRETECOST, true, false, true);
                
                leader.setGuildId(gid);
                leader.setGuildRank(1);
                leader.saveGuildStatus();
                leader.dropMessage(1, "You have successfully created a Guild.");
                
                for (Player p : matchPlayers) {
                    boolean cofounder = p.getPartyId() == partyid;
                    
                    MapleGuildCharacter mgc = p.getMGC();
                    mgc.setGuildId(gid);
                    mgc.setGuildRank(cofounder ? 2 : 5);

                    GuildService.addGuildMember(mgc);
                   
                    mgc.setAllianceRank(5);
                    
                   // if (p.isLoggedinWorld()) {
                        p.announce(GuildPackets.ShowGuildInfo(p));
                        
                        if (cofounder) {
                            p.dropMessage(1, "You have successfully cofounded a Guild.");
                        } else {
                            p.dropMessage(1, "You have successfully joined the new Guild.");
                        }
                   // }
                    
                    p.saveGuildStatus(); // update database
                }
                
                guild.broadcastNameChanged();
                guild.broadcastEmblemChanged();
            }
            
            @Override
            public void onMatchDeclined(int leaderid, Set<Player> matchPlayers, String message) {
                for (Player p : matchPlayers) {
                    if (p.getId() == leaderid && p.getClient() != null) {
                        MapleParty.leaveParty(p.getParty(), p.getClient());
                    }
                    
                  //  if (p.isLoggedinWorld()) {
                        p.announce(GuildPackets.GenericGuildMessage((byte)0x24));
                   // }
                }
            }
            
            @Override
            public void onMatchDismissed(int leaderid, Set<Player> matchPlayers, String message) {
                
                Player leader = null;
                for (Player p : matchPlayers) {
                    if (p.getId() == leaderid) {
                        leader = p;
                        break;
                    }
                }
                
                String msg;
                if (leader != null && leader.getParty() == null) {
                    msg = "The Guild creation has been dismissed since the leader left the founding party.";
                } else {
                    msg = "The Guild creation has been dismissed since a member was already in a party when they answered.";
                }
                
                for (Player p : matchPlayers) {
                    if (p.getId() == leaderid && p.getClient() != null) {
                        MapleParty.leaveParty(p.getParty(), p.getClient());
                    }
                    
                  //  if (p.isLoggedinWorld()) {
                        p.message(msg);
                  //  }
                }
            }
        };
    }
}