/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package server.partyquest.mcpq;

import community.MapleParty;
import handling.channel.ChannelServer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import packet.creators.CarnivalPackets;
import packet.creators.PacketCreator;
import packet.transfer.write.OutPacket;
import client.player.Player;
import client.player.buffs.Disease;
import server.life.MobSkill;
import server.life.MobSkillFactory;
import server.maps.Field;
import server.partyquest.mcpq.MCField.MCTeam;

/**
 * Provides an interface for Monster Carnival-specific party methods and variables.
 *
 * @author s4nta
 */
public class MCParty {

    private final Player leader;
    private final MapleParty party;
    private List<Player> characters = new ArrayList<>();
    private int availCP = 0;
    private int totalCP = 0;
    private String leaderName = "";
    private MCField.MCTeam team = MCField.MCTeam.NONE;
    private MCField field;
    private MCParty enemy;

    public MCParty(MapleParty party) {
        this.party = party;
        this.leader = party.getLeader().getPlayer();
        this.leaderName = party.getLeader().getName();
        party.getMembers().stream().filter((chr) -> !(!chr.isOnline())).map((chr) -> ChannelServer.getInstance(chr.getChannel()).getPlayerStorage().getCharacterById(chr.getId())).forEachOrdered((c) -> {
            characters.add(c);
        });
    }

    public int getSize() {
        return this.characters.size();
    }

    /**
     * Checks if the underlying MapleParty still exists in the same way it did when it was created.
     * That is, if there were no players who left the party.
     *
     * [MENTION=850422]return[/MENTION] True if the underlying MapleParty still exists in its original format.
     * @return 
     */
    public boolean exists() {
        Collection<Player> members = getMembers();
        return members.stream().noneMatch((chr) -> (chr.getParty() == null || chr.getParty() != this.party));
    }
    
    public Player getLeader() {
        return this.leader;
    }
    
    public String getLeaderName() {
        return this.leaderName;
    }

    public int getAverageLevel() {
        int sum = 0, num = 0;
        for (Player chr : getMembers()) {
            sum += chr.getLevel();
            num += 1;
        }
        return sum / num;
    }

    public boolean checkLevels() {
        if (MonsterCarnival.DEBUG) {
            return true;
        }
        return getMembers().stream().map((chr) -> chr.getLevel()).noneMatch((lv) -> (lv < MonsterCarnival.MIN_LEVEL || lv > MonsterCarnival.MAX_LEVEL));
    }

    public boolean checkChannels() {
        if (MonsterCarnival.DEBUG) {
            return true;
        }
        return getMembers().stream().noneMatch((chr) -> (chr.getClient().getChannel() != party.getLeader().getChannel()));
    }

    public boolean checkMaps() {
        if (MonsterCarnival.DEBUG) {
            return true;
        }
        return getMembers().stream().noneMatch((chr) -> (chr.getMapId() != MonsterCarnival.MAP_LOBBY));
    }

    public void warp(int map) {
        getMembers().forEach((chr) -> {
            chr.changeMap(map);
        });
    }

    public void warp(Field map) {
        getMembers().forEach((chr) -> {
            chr.changeMap(map, map.getPortal(0));
        });
    }

    public void warp(Field map, String portal) {
        getMembers().forEach((chr) -> {
            chr.changeMap(map, map.getPortal(portal));
        });
    }

    public void warp(MCField.MCMaps type) {
        Field m = this.field.getMap(type);
        getMembers().forEach((chr) -> {
            chr.changeMap(m, m.getPortal(0));
        });
    }

    public void clock(int secs) {
        getMembers().forEach((chr) -> {
            chr.getClient().announce(PacketCreator.GetClockTimer(secs));
        });
    }

    public void notice(String msg) {
        broadcast(PacketCreator.ServerNotice(6, msg));
    }
    
    public void notice2(String msg) {
        broadcast(PacketCreator.ServerNotice(5, msg));
    }

    public void broadcast(OutPacket pkt) {
        getMembers().forEach((chr) -> {
            chr.getClient().announce(pkt);
        });
    }

    /**
     * Sets MCPQTeam, MCPQParty, and MCPQField for a given character.
     * [MENTION=2000183830]para[/MENTION]m chr Character to update.
     * @param chr
     */
    public void updatePlayer(Player chr) {
        chr.setMCPQTeam(this.team);
        chr.setMCPQParty(this);
        chr.setMCPQField(this.field);
    }

    /**
     * Sets MCPQTeam, MCPQParty, and MCPQ field for all characters in the party.
     * Unlike deregisterPlayers, this method does NOT warp players to the lobby map.
     */
    public void updatePlayers() {
        getMembers().forEach((chr) -> {
            this.updatePlayer(chr);
        });
    }

    /**
     * Resets MCPQ variables for a given character.
     * [MENTION=2000183830]para[/MENTION]m chr Character to reset.
     * @param p
     */
    public static void deregisterPlayer(Player p) {
        if (p != null) {
            p.setMCPQTeam(MCTeam.NONE);
            p.setMCPQParty(null);
            p.setMCPQField(null);
            p.setChallenged(false);


            p.setAvailableCP(0);
            p.setTotalCP(0);
        }
    }

    /**
     * Resets MCPQ variables for all characters in the party.
     * Unlike updatePlayers, this method DOES warp players to the lobby map.
     */
    public void deregisterPlayers() {
        getMembers().stream().map((chr) -> {
            MCParty.deregisterPlayer(chr);
            return chr;
        }).forEachOrdered((chr) -> {
            chr.changeMap(MonsterCarnival.MAP_EXIT);
        });
    }

    public void removePlayer(Player chr) {
        characters.remove(chr);
        deregisterPlayer(chr);
    }

    public void startBattle() {
        characters.forEach((chr) -> {
            chr.getClient().getSession().write(CarnivalPackets.StartMonsterCarnival(chr));
        });
    }

    /**
     * Uses some amount of available CP.
     * [MENTION=2000183830]para[/MENTION]m use A positive integer to be subtracted from available CP.
     * @param use
     */
    public void loseCP(int use) {
        // TODO: locks?
        if (use < 0) {
            System.err.println("Attempting to use negative CP.");
        }
        this.availCP -= use;
    }

    public void gainCP(int gain) {
        // TODO: locks?
        this.availCP += gain;
        this.totalCP += gain;
    }

    public MCParty getEnemy() {
        return enemy;
    }

    public void setEnemy(MCParty enemy) {
        this.enemy = enemy;
    }

    /**
     * Applies a MCSkill to the entire team. This is used on the team's own players
     * because it is called when the enemy team uses a debuff/cube of darkness.
     * [MENTION=2000183830]para[/MENTION]m skill Skill to apply.
     * [MENTION=850422]return[/MENTION] True if skill was applied, false otherwise.
     * @param skill
     * @return 
     */
    public boolean applyMCSkill(MCSkill skill) {
        MobSkill s = MobSkillFactory.getMobSkill(skill.getMobSkillID(), skill.getLevel());
        Disease disease = Disease.getType(skill.getMobSkillID());
        if (disease == null) {
            disease = Disease.DARKNESS;
            s = MobSkillFactory.getMobSkill(121, 6); // HACK: darkness
        } else if (disease == Disease.POISON) {
            return false;
        }

        // We only target players on the battlefield map.
        if (skill.getTarget() == 2) {
            for (Player chr : getMembers()) {
                if (MonsterCarnival.isBattlefieldMap(chr.getMapId())) {
                    chr.giveDebuff(disease, s);
                }
            }
            return true;
        } else {
            if (getRandomMember() != null) {
                getRandomMember().giveDebuff(disease, 1, 30000L, disease.getDisease(), 1);
                return true;
            } else {
                return false;
            }
        }
    }

    public void setField(MCField field) {
        this.field = field;
    }

    public void setTeam(MCTeam newTeam) {
        this.team = newTeam;
    }

    public MCTeam getTeam() {
        return team;
    }

    /**
     * Returns a collection of online members in the party.
     * [MENTION=850422]return[/MENTION] Online MCParty members.
     * @return 
     */
    public Collection<Player> getMembers() {
        return this.characters;
    }

    public Player getRandomMember() {
        List<Player> chrsOnMap = new ArrayList<>();
        this.characters.stream().filter((chr) -> (MonsterCarnival.isBattlefieldMap(chr.getMapId()))).forEachOrdered((chr) -> {
            chrsOnMap.add(chr);
        });
        if (chrsOnMap.isEmpty()) {
            return null;
        }
        return chrsOnMap.get(new Random().nextInt(chrsOnMap.size()));
    }

    public int getAvailableCP() {
        return availCP;
    }

    public int getTotalCP() {
        return totalCP;
    }
}  