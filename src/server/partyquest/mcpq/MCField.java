/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package server.partyquest.mcpq;

import handling.channel.ChannelServer;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import packet.creators.CarnivalPackets;
import packet.creators.EffectPackets;
import packet.creators.PacketCreator;
import packet.transfer.write.OutPacket;
import client.player.Player;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import server.MapleStatEffect;
import server.itens.ItemInformationProvider;
import server.maps.Field;
import server.maps.FieldItem;
import server.maps.reactors.Reactor;
import server.maps.portal.Portal;
import tools.TimerTools.EventTimer;

/**
 * Keeps track of a specific field (1-6). Handles all packet broadcasting, etc.
 * @author s4nta
 */
public class MCField {

    /**
     * Different teams for MCPQ.
     */
    public enum MCTeam {
        RED(0),
        BLUE(1),
        NONE(-1);
        public final int code;

        MCTeam(int code) {
            this.code = code;
        }

        public final int getEnemyTeamCode() {
            return Math.abs(this.code - 1);
        }
    }

    /**
     * Represents the current state of the field.
     */
    public enum MCState {
        LOBBY,
        BATTLE,
        END;
    }

    /**
     * Keys to access different map instances relating to this field.
     */
    public enum MCMaps {
        LOBBY(0),
        BATTLEFIELD(1),
        RESURRECT(2),
        VICTORY(3),
        DEFEAT(4),
        NONE(-1);
        private final int code;

        MCMaps(int code) {
            this.code = code;
        }

        public static MCMaps getByCode(int code) {
            for (MCMaps m : values()) {
                if (m.code == code) {
                    return m;
                }
            }
            return NONE;
        }
    }

    private final int arena;
    private final ChannelServer cserv;
    private MCParty red, blue;
    private MCState state;
    private final Map<MCMaps, Field> mapInstances = new HashMap<>();
    private long startTime;
    private MCBattlefield battlefield;
    private final ReentrantReadWriteLock MCFieldLocked = new ReentrantReadWriteLock();

    // Timer Tasks
    private ScheduledFuture<?> acceptRequestsTask, validateRoomTask, startBattleTask,
            validateBattleTask, runBattleTask, endBattleTask,
            spawnMonstersTask;

    public MCField(int arena, ChannelServer cserv, MCParty red, MCParty blue) {
        this.arena = arena;
        this.cserv = cserv;
        this.red = red;
        this.blue = blue;
        this.state = MCState.LOBBY;
    }

    public boolean isFull() {
        if (MonsterCarnival.DEBUG) {
            return false;
        }
        return this.red != null && this.blue != null;
    }

    public boolean needsRequest() {
        return this.red != null && this.blue == null;
    }

    /**
     * Resets the state of the field, warping out players and resetting tasks.
     *   [MENTION=2000183830]para[/MENTION]m warpPlayers Warp out players or not
     * @param warpPlayers
     */
    public void deregister(boolean warpPlayers) {
        if (warpPlayers) {
            if (this.red != null) {
                this.red.deregisterPlayers();
            }
            if (this.blue != null) {
                this.blue.deregisterPlayers();
            }
        }


        this.red = null;
        this.blue = null;
        this.state = MCState.LOBBY;

        if (this.acceptRequestsTask != null) {
            this.acceptRequestsTask.cancel(true);
            this.acceptRequestsTask = null;
        }

        if (this.validateRoomTask != null) {
            this.validateRoomTask.cancel(true);
            this.validateRoomTask = null;
        }

        if (this.startBattleTask != null) {
            this.startBattleTask.cancel(true);
            this.startBattleTask = null;
        }

        if (this.endBattleTask != null) {
            this.endBattleTask.cancel(true);
            this.endBattleTask = null;
        }

        if (this.runBattleTask != null) {
            this.runBattleTask.cancel(true);
            this.runBattleTask = null;
        }

        if (this.validateBattleTask != null) {
            this.validateBattleTask.cancel(true);
            this.validateBattleTask = null;
        }

        if (this.spawnMonstersTask != null) {
            this.spawnMonstersTask.cancel(true);
            this.spawnMonstersTask = null;
        }

        for (MCMaps mapType : MCMaps.values()) {
            this.mapInstances.remove(mapType);
        }
    }

    public MCParty getRed() {
        return red;
    }

    public MCParty getBlue() {
        return blue;
    }

    public void announce(OutPacket pkt) {
        if (this.red != null) {
            red.broadcast(pkt);
        } else {
            MCTracker.log("[MCPQ] Trying to announce packet to red when it is null.");
        }

        if (this.blue != null) {
            blue.broadcast(pkt);
        } else {
            MCTracker.log("[MCPQ] Trying to announce packet to blue when it is null.");
        }
    }

    /**
     * Gets a string representing the status of this room for the Spiegelmann NPC.
     *   [MENTION=850422]return[/MENTION] String representing the room's status.
     * @return 
     */
    public String getStatus() {
        if (isFull()) {
            return "";
        }
        if (this.state != MCState.LOBBY) {
            return "";
        }
        String waitingParty = "";
        switch (this.arena) {
            case 1:
            case 2:
            case 3:
            case 4:
                waitingParty = "(2~4 ppl)";
                break;
            case 5:
            case 6:
                waitingParty = "(3~6 ppl)";   
                break;
                
        }
        if (this.red != null) {
            waitingParty = "(" + this.red.getLeaderName() + "/" + this.red.getSize() + "users/Avg. Level " + this.red.getAverageLevel() + ")";
        }
        String fmt = "#L%d#Carnival Field %d%s#l\r\n";
        return String.format(fmt, this.arena, this.arena, waitingParty);
    }

    /**
     * Attempts to register a party in this field. If success, then all players in the party
     * will be warped to the waiting lobby. All players in the party will also have relevant
     * CPQ information assigned to them.
     *
     *   [MENTION=2000183830]para[/MENTION]m party The party to register.
     * @param party
     * @param team
     */
    public void register(MCParty party, MCTeam team) {
        if (this.red == null && team == MCTeam.RED) {
            party.setTeam(team);
            this.red = party;
        } else if (this.blue == null && team == MCTeam.BLUE) {
            party.setTeam(team);
            this.blue = party;
        } else {
            MCTracker.log("Attempting to register party when team is already set.");
            return;
        }
        party.setField(this);
        party.updatePlayers();
        party.warp(MCMaps.LOBBY);
        onPartyRegistered(party, team);
    }


    /**
     * Accepts a challenge from a team.
     *   [MENTION=2000183830]para[/MENTION]m index Index of team in requests.
     *   [MENTION=850422]return[/MENTION] 1 if the challenge was accepted successfully, 0 otherwise.
     * @param pt
     * @return 
     */
    public int acceptRequest(MCParty pt) {
        register(pt, MCTeam.BLUE);
        return 1;
    }

    /**
     * Starts a R3 minute waiting timer in the lobby for teams to accept requests.
     * If the timer completes the countdown, teams are deregistered and sent back to the lobby.
     */
    private void startLobbyTask(MCParty host) {
        host.clock(MonsterCarnival.TIME_LOBBYWAIT);
        this.acceptRequestsTask = EventTimer.getInstance().schedule(new AcceptingRequestsTask(this, host), 1000 * MonsterCarnival.TIME_LOBBYWAIT); // 3 minutes
        this.validateRoomTask = EventTimer.getInstance().register(new ValidateLobbyTask(this), 1000, 1000); // repeat every second
    }

    /**
     * Event handling for when a team registers.
     *
     *   [MENTION=2000183830]para[/MENTION]m party Party that registers.
     *   [MENTION=2000183830]para[/MENTION]m team Team of party.
     */
    private void onPartyRegistered(MCParty party, MCTeam team) {
        if (team == MCTeam.RED) {
            startLobbyTask(party);
        }
        if (team == MCTeam.BLUE) { // both teams are in
            this.validateRoomTask.cancel(true);
            this.acceptRequestsTask.cancel(true);
            blue.clock(10);
            red.clock(10);

            this.startBattleTask = EventTimer.getInstance().schedule(new GoBattlefieldTask(this), 1000 * 10); // 10 seconds

            red.notice("The Monster Carnival will begin in 10 seconds!");
            blue.notice("The Monster Carnival will begin in 10 seconds!");
        }
    }

    /**
     * Warps both parties in the field to the battlefield map.
     */
    private void goBattle() {
        Field map = getMap(MCMaps.BATTLEFIELD);
        if (MonsterCarnival.DEBUG)
            System.out.println("warping to battle " + map + " " + map.getId());
        if (red != null) {
            red.warp(map, "red00");
        } else {
            MCTracker.log("[MCPQ] Trying to warp red party when it is null.");
        }

        if (blue != null) {
            blue.warp(map, "blue00");
        } else {
            MCTracker.log("[MCPQ] Trying to warp blue party when it is null.");
        }

        Field field = this.getMap(MCMaps.BATTLEFIELD);
        field.beginSpawning();
        this.beginCarnival();

        red.notice2("  Monster Carnival is now underway!!");
        blue.notice2("  Monster Carnival is now underway!!");

        validateBattleTask = EventTimer.getInstance().register(new ValidateBattlefieldTask(this), 1000, 500); // check every second

        battlefield = new MCBattlefield(getMap(MCMaps.BATTLEFIELD));

        red.setEnemy(blue);
        blue.setEnemy(red);
        
    }

    private void beginCarnival() {
        red.clock(MonsterCarnival.TIME_BATTLE);
        blue.clock(MonsterCarnival.TIME_BATTLE);
        startTime = System.currentTimeMillis();

        endBattleTask = EventTimer.getInstance().schedule(new EndBattleTask(this), 1000 * MonsterCarnival.TIME_BATTLE);
        spawnMonstersTask = EventTimer.getInstance().register(new SpawnTask(this.battlefield), 1000 * 5);

    }

    public void endBattle(MCParty winner, MCParty loser) {
        endBattle(winner, loser, false);
    }

    public void endBattle(MCParty winner, MCParty loser, boolean abnormal) {
        // TODO: Abnormal win codes to prevent exploits

        validateBattleTask.cancel(true);
        spawnMonstersTask.cancel(true);

        MCWZData data = this.getMap(MCMaps.BATTLEFIELD).getMCPQData();

        winner.broadcast(EffectPackets.ShowEffect(data.effectWin));
        winner.broadcast(EffectPackets.PlaySound(data.soundWin));
        loser.broadcast(EffectPackets.ShowEffect(data.effectLose));
        loser.broadcast(EffectPackets.PlaySound(data.soundLose));

        this.getMap(MCMaps.BATTLEFIELD).killAllMonsters();
        this.getMap(MCMaps.BATTLEFIELD).clearDrops();
        this.deregister(false);

        EventTimer.getInstance().schedule(new WarpEndBattleTask(this, winner, loser), 1000 * 3);
    }

    /**
     * Handles CP gain and packet updates when a monster is killed.
     *   [MENTION=2000183830]para[/MENTION]m chr Character that kills the monster.
     *   [MENTION=2000183830]para[/MENTION]m cp CP gained.
     * @param chr
     * @param cp
     */
    public void monsterKilled(Player chr, int cp) {
        if (MonsterCarnival.DEBUG) {
             System.out.println(chr.getName() + " killed for +" + cp + " CP");
        }
        // TODO: Personal stats for CP gain
        this.gainCP(chr, cp);
    }

    /**
     * Handles game logic and packet broadcasting for CP gain.
     * Broadcasts personal CP update to chr, and broadcasts party CP update
     * to the entire field.
     *
     *   [MENTION=2000183830]para[/MENTION]m chr Character that gains CP.
     *   [MENTION=2000183830]para[/MENTION]m cp CP gained.
     * @param chr
     * @param cp
     */
    public void gainCP(Player chr, int cp) {
        if (cp < 1 || cp > 10) {
            MCTracker.log("[MCPQ] Adding invalid CP.");
        }
        MCParty pty = chr.getMCPQParty();
        if (pty != null) {
            chr.gainCP(cp);
            pty.gainCP(cp);
            chr.getClient().announce(CarnivalPackets.UpdatePersonalCP(chr));
            this.announce(CarnivalPackets.UpdatePartyCP(pty));
        } else {
            System.out.println("Adding CP to null party");
        }
    }

    /**
     * Subtracts from available CP while leaving total CP untouched.
     *
     *   [MENTION=2000183830]para[/MENTION]m chr Character that loses CP.
     *   [MENTION=2000183830]para[/MENTION]m cp CP lost (should be positive number).
     * @param chr
     * @param cp
     */
    public void loseCP(Player chr, int cp) {
        if (cp < 1) {
            MCTracker.log("[MCPQ] Losing negative CP.");
        }
        MCParty pty = chr.getMCPQParty();
        if (pty != null) {
            chr.loseCP(cp);
            pty.loseCP(cp);
            chr.getClient().announce(CarnivalPackets.UpdatePersonalCP(chr));
            this.announce(CarnivalPackets.UpdatePartyCP(pty));
        }
    }

    /**
     * Handles a player looting an item.
     *   [MENTION=2000183830]para[/MENTION]m player Player that picked up the object.
     *   [MENTION=2000183830]para[/MENTION]m mapitem Object picked up.
     *
     *   [MENTION=850422]return[/MENTION] True if pickup was successful, false otherwise.
     * @param p
     * @param mapitem
     * @return 
     */
    public boolean onItemPickup(Player p, FieldItem mapitem) {
        if (mapitem == null) {
            MCTracker.log("[MCPQ] Attempting to loot null object.");
            return false;
        }
        int itemid = mapitem.getItem().getItemId();
        if (!MonsterCarnival.isCPQConsumeItem(itemid)) {
            return false;
        }
        MCParty pty = p.getMCPQParty();
        ItemInformationProvider ii = ItemInformationProvider.getInstance();
        MapleStatEffect itemEffect = ii.getItemEffect(itemid);
        if (!itemEffect.isConsumeOnPickup()) {
            return false;
        }

        if (itemEffect.isParty()) {
            pty.getMembers().stream().filter((chr) -> (chr.getStat().getHp() > 0)).forEachOrdered((chr) -> {
                itemEffect.applyTo(chr);
            });
        } else { // Single Target Item
            itemEffect.applyTo(p);
        } 
        // Status items
        if (itemEffect.getNuffSkill() != -1) {
            MCSkill debuff = MCSkillFactory.getMCSkill(itemEffect.getNuffSkill());
            if (debuff == null) {
                MCTracker.log("[MCPQ] debuff skill is null " + itemEffect.getNuffSkill());
                return false;
            }

            pty.getEnemy().applyMCSkill(debuff);
        }

        if (itemEffect.getCP() > 0) {
            this.gainCP(p, itemEffect.getCP());
        }

        return true;
    }

    public void onPlayerRespawn(Player player) {
        int cpLoss = Math.min(player.getAvailableCP(), MonsterCarnival.CP_LOSS_ON_DEATH);
        this.announce(CarnivalPackets.PlayerDiedMessage(player, cpLoss));
        this.loseCP(player, cpLoss);
        player.getStat().addMPHP(30000, 30000);
        player.changeMap(this.getMap(MCMaps.RESURRECT), this.getMap(MCMaps.RESURRECT).getPortal(0));
        player.getClient().getSession().write(PacketCreator.GetClockTimer(getTimeRemaining()));
        player.getClient().getSession().write(CarnivalPackets.StartMonsterCarnival(player));
    }

    public void onPlayerDisconnected(Player player) {
        MCParty pty = player.getMCPQParty();
        if (pty != null) {
            pty.removePlayer(player);
        } else {
            MCTracker.log("[MCPQ] Attempting to run player disconnect event when party is null for character " + player.getName());
        }
    }

    public void onAddSpawn(Player chr, int num) {
        if (this.battlefield != null) {
            battlefield.addSpawn(chr, num);
        } else {
            MCTracker.log("[MCPQ] Summoning guardian with null battlefield.");
        }
    }

    public void onUseSkill(Player chr, int num) {
        if (this.battlefield != null) {
            battlefield.useSkill(chr, num);
        } else {
            MCTracker.log("[MCPQ] Summoning guardian with null battlefield.");
        }
    }

    public void onGuardianSummon(Player chr, int num) {
        if (this.battlefield != null) {
            battlefield.spawnGuardian(chr, num);
        } else {
            MCTracker.log("[MCPQ] Summoning guardian with null battlefield.");
        }
    }

    public void onGuardianHit(Player chr, Reactor reactor) {
        if (this.battlefield != null) {
            battlefield.onGuardianHit(chr, reactor);
        } else {
            MCTracker.log("[MCPQ] Hitting reactor with null battlefield.");
        }
    }

    public void onRevive(Player player) {
        MCTeam team = player.getMCPQTeam();
        Portal portal;
        if (team == MCTeam.RED) {
            portal = getMap(MCMaps.BATTLEFIELD).getPortal("red_revive");
        } else {
            portal = getMap(MCMaps.BATTLEFIELD).getPortal("blue_revive");
        }

        player.changeMap(getMap(MCMaps.BATTLEFIELD), portal);
        player.getClient().getSession().write(PacketCreator.GetClockTimer(getTimeRemaining()));
        player.getClient().getSession().write(CarnivalPackets.StartMonsterCarnival(player));
    }

    public int getTimeRemaining() {
        // TODO: add support for setting an explicit endTime instead of using the hack with MonsterCarnival variables
        return (int) ((startTime + 1000 * MonsterCarnival.TIME_BATTLE) - System.currentTimeMillis()) / 1000;
    }


    // Map Instances

    /**
     * Returns the map instance for a requested map. Creates a new map instance if unavailable.
     *   [MENTION=2000183830]para[/MENTION]m type Map instance to return.
     *   [MENTION=850422]return[/MENTION] The instanced map.
     */
    public Field getMap(MCMaps type) {
        if (this.mapInstances.containsKey(type)) {
            return this.mapInstances.get(type);
        }
        return createInstanceMap(type);
    }

    /**
     * Attempts to create an instanced map, based on the type passed in. Also creates a mapping in
     * this.mapInstances.
     *
     *   [MENTION=2000183830]para[/MENTION]m type Type of map to generate.
     *   [MENTION=850422]return[/MENTION] MapleMap for the instanced map if type is supported, otherwise null.
     */
    private Field createInstanceMap(MCMaps type) {
        int mapid = -1;
        switch (type) {
            case LOBBY:
                mapid = MonsterCarnival.getLobbyMap(this.arena);
                break;
            case BATTLEFIELD:
                mapid = MonsterCarnival.getBattleFieldMap(this.arena);
                break;
            case RESURRECT:
                mapid = MonsterCarnival.getResurrectionMap(this.arena);
                break;
            case VICTORY:
                mapid = MonsterCarnival.getVictoriousMap(this.arena);
                break;
            case DEFEAT:
                mapid = MonsterCarnival.getDefeatedMap(this.arena);
                break;
        }
        if (mapid == -1) return null;
        Field mapInstance = this.cserv.getMapFactory().getMap(mapid);
        this.mapInstances.put(type, mapInstance);
        return mapInstance;
    }

    // Timer Tasks

    public class ValidateLobbyTask implements Runnable {

        private final MCField field;

        /**
         * Timer task to ensure all players are on the right field.
         * If anything is wrong with the parties, the field is deregistered.
         *   [MENTION=2000183830]para[/MENTION]m field Field to run the validation task on.
         * @param field
         */
        public ValidateLobbyTask(MCField field) {
            this.field = field;
        }

        @Override
        public void run() {
            if (this.field.red == null) {
                this.field.deregister(true);
                return;
            }
            for (Player c : field.red.getMembers()) {
                if (c.getMap() != field.getMap(MCMaps.LOBBY)) {
                    this.field.deregister(true);
                    return;
                }
            }
            if (this.field.blue != null) {
                for (Player c : field.blue.getMembers()) {
                    if (c.getMap() != field.getMap(MCMaps.LOBBY)) {
                        this.field.deregister(true);
                        return;
                    }
                }
            }
        }
    }

    public class ValidateBattlefieldTask implements Runnable {

        private final MCField field;

        /**
         * Timer task to ensure all players are on the right field.
         * If anything is wrong with the parties, the field is deregistered.
         *   [MENTION=2000183830]para[/MENTION]m field Field to run the validation task on.
         * @param field
         */

        public ValidateBattlefieldTask(MCField field) {
            this.field = field;
        }

        @Override
        public void run() {
            if (this.field.red == null || field.red.getSize() == 0) {
                MCTracker.log("[MCPQ] Red team null when validating battlefield");
                field.endBattle(blue, red);
                return;
            }
            Collection<Player> members = Collections.unmodifiableCollection(field.red.getMembers());
            for (Player c : members) {
                if (c.getMap() != field.getMap(MCMaps.BATTLEFIELD) && c.getMap() != field.getMap(MCMaps.RESURRECT)) {
                    this.field.announce(CarnivalPackets.CarnivalLeave(MCTeam.RED.code, c.getName()));
                    red.removePlayer(c); // TODO: fix concurrent modification
                }
                if (c.getMap() == field.getMap(MCMaps.BATTLEFIELD) && !c.isAlive()) {
                    this.field.onPlayerRespawn(c);
                }
            }
            if (this.field.blue == null || field.blue.getSize() == 0) {
                MCTracker.log("[MCPQ] Blue team null when validating battlefield");
                field.endBattle(red, blue);
                return;
            }
            members = Collections.unmodifiableCollection(field.blue.getMembers());
            for (Player c : members) {
                if (c.getMap() != field.getMap(MCMaps.BATTLEFIELD) && c.getMap() != field.getMap(MCMaps.RESURRECT)) {
                    this.field.announce(CarnivalPackets.CarnivalLeave(MCTeam.BLUE.code, c.getName()));
                    blue.removePlayer(c);
                }
                if (c.getMap() == field.getMap(MCMaps.BATTLEFIELD) && !c.isAlive()) {
                    this.field.onPlayerRespawn(c);
                }
            }
        }
    }

    public class AcceptingRequestsTask implements Runnable {

        private final MCField field;
        private final MCParty host;

        /**
         * Runs a task that counts down for 3 minutes, then warps the hosting party out.
         *
         *   [MENTION=2000183830]para[/MENTION]m field Field to accept requests on.
         *   [MENTION=2000183830]para[/MENTION]m host Hosting party that will be warped out if they do not accept a request
         *             within 3 minutes.
         * @param field
         * @param host
         */
        public AcceptingRequestsTask(MCField field, MCParty host) {
            this.field = field;
            this.host = host;
        }

        @Override
        public void run() {
            Collection<Player> chrs = this.host.getMembers();
            chrs.forEach((c) -> {
                c.changeMap(MonsterCarnival.MAP_LOBBY);
            });
            this.field.deregister(true);
        }
    }

    public class GoBattlefieldTask implements Runnable {

        private final MCField field;

        public GoBattlefieldTask(MCField field) {
            this.field = field;
        }

        @Override
        public void run() {
            field.goBattle();

            field.red.startBattle();
            field.blue.startBattle();

            field.state = MCState.BATTLE;
        }
    }

    /* I have no idea why this doesn't work normally :/ */
    public class SpawnTask implements Runnable {

        private final MCBattlefield battleMap;

        public SpawnTask(MCBattlefield field) {
            this.battleMap = field;
        }

        @Override
        public void run() {
            // TODO: adjust spawn rates based on cp
            battleMap.spawningTask();
        }
    }

    public class EndBattleTask implements Runnable {
        private final MCField field;

        public EndBattleTask(MCField field) {
            this.field = field;
        }

        @Override
        public void run() {
            MCParty winner, loser;
            if (field.red.getTotalCP() > field.blue.getTotalCP()) {
                winner = field.red;
                loser = field.blue;
            } else if (field.red.getTotalCP() < field.blue.getTotalCP()) {
                winner = field.blue;
                loser = field.red;
            } else {
                // if tied: random chance
                // TODO: proper extension of time
                if (Math.random() < .5) {
                    winner = field.red;
                    loser = field.blue;
                } else {
                    winner = field.blue;
                    loser = field.red;
                }
            }

            field.state = MCState.END;
            field.endBattle(winner, loser);
        }
    }

    public class WarpEndBattleTask implements Runnable {

        private final MCField field;
        private final MCParty winner, loser;

        public WarpEndBattleTask(MCField field, MCParty winner, MCParty loser) {
            this.field = field;
            this.winner = winner;
            this.loser = loser;
        }

        @Override
        public void run() {
            winner.warp(field.getMap(MCMaps.VICTORY));
            loser.warp(field.getMap(MCMaps.DEFEAT));
        }
    } 
}