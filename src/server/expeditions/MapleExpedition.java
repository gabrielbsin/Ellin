/*
	This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
		       Matthias Butz <matze@odinms.de>
		       Jan Christian Meyer <vimes@odinms.de>

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

package server.expeditions;

import client.player.Player;
import handling.world.service.BroadcastService;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import packet.creators.PacketCreator;
import packet.transfer.write.OutPacket;
import server.life.MapleMonster;
import server.maps.Field;
import tools.TimerTools.EventTimer;


/**
 *
 * @author SharpAceX(Alan)
 */
public class MapleExpedition {

    private static final int [] EXPEDITION_BOSSES = {
        8800000,// - Zakum's first body
        8800001,// - Zakum's second body
        8800002,// - Zakum's third body
        8800003,// - Zakum's Arm 1
        8800004,// - Zakum's Arm 2
        8800005,// - Zakum's Arm 3
        8800006,// - Zakum's Arm 4
        8800007,// - Zakum's Arm 5
        8800008,// - Zakum's Arm 6
        8800009,// - Zakum's Arm 7
        8800010,// - Zakum's Arm 8
        8810000,// - Horntail's Left Head
        8810001,// - Horntail's Right Head
        8810002,// - Horntail's Head A
        8810003,// - Horntail's Head B
        8810004,// - Horntail's Head C
        8810005,// - Horntail's Left Hand
        8810006,// - Horntail's Right Hand
        8810007,// - Horntail's Wings
        8810008,// - Horntail's Legs
        8810009,// - Horntail's Tails
        9420546,// - Scarlion Boss
        9420547,// - Scarlion Boss
        9420548,// - Angry Scarlion Boss
        9420549,// - Furious Scarlion Boss
        9420541,// - Targa
        9420542,// - Targa
        9420543,// - Angry Targa
        9420544,// - Furious Targa
    };
	
    private Player leader;
    private Field startMap;
    private long startTime;
    private boolean registering;
    private MapleExpeditionType type;
    private ArrayList<String> bossLogs;
    private ScheduledFuture<?> schedule;
    private List<Player> members = new ArrayList<>();
    private List<Integer> banned = new ArrayList<>();

    public MapleExpedition(Player p, MapleExpeditionType met) {
        leader = p;
        members.add(leader);
        startMap = p.getMap();
        type = met;
        bossLogs = new ArrayList<>();
        beginRegistration();
    }

    private void beginRegistration() {
        registering = true;
        leader.announce(PacketCreator.GetClockTimer(type.getRegistrationTime() * 60));
        startMap.broadcastMessage(leader, PacketCreator.ServerNotice(6, "[Expedition] " + leader.getName() + " has been declared the expedition captain. Please register for the expedition."), false);
        leader.announce(PacketCreator.ServerNotice(6, "[Expedition] You have become the expedition captain. Gather enough people for your team then talk to the NPC to start."));
        scheduleRegistrationEnd();
    }

    private void scheduleRegistrationEnd() {
        final MapleExpedition exped = this;
        startTime = System.currentTimeMillis() + type.getRegistrationTime() * 60 * 1000;

        schedule = EventTimer.getInstance().schedule(() -> {
            if (registering){
                leader.getClient().getChannelServer().getExpeditions().remove(exped);
                startMap.broadcastMessage(PacketCreator.ServerNotice(6, "[Expedition] The time limit has been reached. Expedition has been disbanded."));

                dispose(false);
            }
        }, type.getRegistrationTime() * 60 * 1000);
    }

    public void dispose(boolean log){
        broadcastExped(PacketCreator.DestroyClock());

        if (schedule != null){
            schedule.cancel(false);
        }
        if (log && !registering){
            //LogHelper.logExpedition(this);
        }
    }

    public void start(){
        registering = false;
        broadcastExped(PacketCreator.DestroyClock());
        broadcastExped(PacketCreator.ServerNotice(6, "[Expedition] The expedition has started! Good luck, brave heroes!"));
        startTime = System.currentTimeMillis();
        BroadcastService.broadcastGMMessage(PacketCreator.ServerNotice(6, "[Expedition] " + type.toString() + " Expedition started with leader: " + leader.getName()));
    }

    public String addMember(Player p) {
        if (!registering){
            return "Sorry, this expedition is already underway. Registration is closed!";
        }
        if (banned.contains(p.getId())){
            return "Sorry, you've been banned from this expedition by #b" + leader.getName() + "#k.";
        }
        if (members.size() >= type.getMaxSize()){ 
            return "Sorry, this expedition is full!";
        }
        if (members.add(p)){
            p.announce(PacketCreator.GetClockTimer((int)(startTime - System.currentTimeMillis()) / 1000));
            broadcastExped(PacketCreator.ServerNotice(6, "[Expedition] " + p.getName() + " has joined the expedition!"));
            return "You have registered for the expedition successfully!";
        } 
        return "Sorry, something went really wrong. Report this on the forum with a screenshot!";
    }

    private void broadcastExped(OutPacket data){
        for (Player member : members){
            member.getClient().announce(data);
        }
    }

    public boolean removeMember(Player p) {
        if (members.remove(p)) {
            p.announce(PacketCreator.DestroyClock());
            broadcastExped(PacketCreator.ServerNotice(6, "[Expedition] " + p.getName() + " has left the expedition."));
            p.dropMessage(6, "[Expedition] You have left this expedition.");
            return true;
        }

        return false;
    }

    public MapleExpeditionType getType() {
        return type;
    }

    public List<Player> getMembers() {
        return members;
    }

    public Player getLeader(){
        return leader;
    }
        
    public Field getRecruitingMap() {
        return startMap;
    }

    public boolean contains(Player player) {
        for (Player member : members){
            if (member.getId() == player.getId()){
                    return true;
            }
        }
        return false;
    }

    public boolean isLeader(Player player) {
        return leader.equals(player);
    }

    public boolean isRegistering(){
        return registering;
    }

    public boolean isInProgress(){
        return !registering;
    }

    public void ban(Player p) {
        if (!banned.contains(p.getId())) {
            banned.add(p.getId());
            members.remove(p);

            broadcastExped(PacketCreator.ServerNotice(6, "[Expedition] " + p.getName() + " has been banned from the expedition."));

            p.announce(PacketCreator.DestroyClock());
            p.dropMessage(6, "[Expedition] You have been banned from this expedition.");
        }
    }

    public long getStartTime(){
        return startTime;
    }

    public ArrayList<String> getBossLogs(){
        return bossLogs;
    }
	
    public void monsterKilled(Player chr, MapleMonster mob) {
        for (int i = 0; i < EXPEDITION_BOSSES.length; i++){
            if (mob.getId() == EXPEDITION_BOSSES[i]){
                String timeStamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
            //	bossLogs.add(">" + mob.getName() + " was killed after " + LogHelper.getTimeString(startTime) + " - " + timeStamp + "\r\n");
                return;
            }
        }
    }
}
