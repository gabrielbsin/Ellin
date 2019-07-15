/*
	This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc> 
                       Matthias Butz <matze@odinms.de>
                       Jan Christian Meyer <vimes@odinms.de>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License version 3
    as published by the Free Software Foundation. You may not use, modify
    or distribute this program under any other version of the
    GNU Affero General Public License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package community;

import java.awt.Point;
import java.io.Serializable;

import client.player.Player;
import client.player.PlayerJob;
import constants.MapConstants;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import server.maps.MapleDoor;

public class MaplePartyCharacter implements Serializable {
    private String name;
    private int id;
    private int level;
    private int channel;
    private int jobid;
    private PlayerJob jobname;
    private int fieldid;
    private int gender;
    private boolean married;
    private int doorTown = MapConstants.NULL_MAP;
    private int doorTarget = MapConstants.NULL_MAP;
    private Point doorPosition = new Point(0, 0);
    private Map<Integer, MapleDoor> doors = new LinkedHashMap<>();
    private boolean online;
    private Player character;
	
    public MaplePartyCharacter(Player maplechar) {
        this.character = maplechar;
        this.name = maplechar.getName();
        this.level = maplechar.getLevel();
        this.channel = maplechar.getClient().getChannel();
        this.id = maplechar.getId();
        this.jobid = maplechar.getJob().getId();
        this.jobname = maplechar.getJob();
        this.fieldid = maplechar.getMapId();
        this.online = true;
        this.gender = maplechar.getGender();
        for (Entry<Integer, MapleDoor> entry : maplechar.getDoors().entrySet()) {
            doors.put(entry.getKey(), entry.getValue());
        }
    }
	
    public MaplePartyCharacter() {
        this.name = "";
    }

    public Player getPlayer() {
        return character;
    }

    public int getLevel() {
        return level;
    }

    public int getChannel() {
        return channel;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public int getMapId() {
        return fieldid;
    }
    
    public boolean isLeader() {
        return getPlayer().isPartyLeader();
    }

    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }

    public int getJobId() {
        return jobid;
    }

    public PlayerJob getJobName() {
        return jobname;
    }

    public int getDoorTown() {
        return doorTown;
    }

    public int getDoorTarget() {
        return doorTarget;
    }

    public Point getDoorPosition() {
        return doorPosition;
    }

    public void addDoor(Integer owner, MapleDoor door) {
    	this.doors.put(owner, door);
    }
    
    public void removeDoor(Integer owner) {
    	this.doors.remove(owner);
    }
    
    public Collection<MapleDoor> getDoors() {
    	return Collections.unmodifiableCollection(doors.values());
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    public int getGender() {
        return gender;
    }

    public boolean isMarried() {
        return married;
    }
    
    public void setMapId(int mapid) {
        this.fieldid = mapid;
    }
    
    public String getJobNameById(int job) {
        switch (job) {
            case 0: return "Beginner";
                
            case 100: return "Warrior";
            case 110: return "Fighter";
            case 111: return "Crusader";
            case 112: return "Hero";
            case 120: return "Page";
            case 121: return "White Knight";
            case 122: return "Paladin";
            case 130: return "Spearman";
            case 131: return "Dragon Knight";
            case 132: return "Dark Knight";

            case 200: return "Magician";
            case 210: return "Wizard(Fire,Poison)";
            case 211: return "Mage(Fire,Poison)";
            case 212: return "Arch Mage(Fire,Poison)";
            case 220: return "Wizard(Ice,Lightning)";
            case 221: return "Mage(Ice,Lightning)";
            case 222: return "Arch Mage(Ice,Lightning)";
            case 230: return "Cleric";
            case 231: return "Priest";
            case 232: return "Bishop";
                
            case 300: return "Archer";
            case 310: return "Hunter";
            case 311: return "Ranger";
            case 312: return "Bowmaster";
            case 320: return "Crossbow man";
            case 321: return "Sniper";
            case 322: return "Crossbow Master";

            case 400: return "Rogue";
            case 410: return "Assassin";
            case 411: return "Hermit";
            case 412: return "Night Lord";
            case 420: return "Bandit";
            case 421: return "Chief Bandit";
            case 422: return "Shadower";
           
            case 500: return "Pirate";
            case 510: return "Infighter";
            case 511: return "Buccaneer";
            case 512: return "Viper";
            case 520: return "Gunslinger";
            case 521: return "Valkyrie";
            case 522: return "Captain";
                
            case 910: return "GM";
                
            default: return "Unknown Job";
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final MaplePartyCharacter other = (MaplePartyCharacter) obj;
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }
}
