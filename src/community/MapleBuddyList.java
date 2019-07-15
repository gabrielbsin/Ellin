/*
 * This file is part of the OdinMS MapleStory Private Server
 * Copyright (C) 2011 Patrick Huy and Matthias Butz
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package community;

import database.DatabaseConnection;
import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MapleBuddyList implements Serializable {

    public static enum BuddyAddResult {
        BUDDYLIST_FULL,
        ALREADY_ON_LIST,
        OK,
        NOT_FOUND
    }

    public static enum BuddyDelResult {
        NOT_ON_LIST,
        IN_CASH_SHOP, 
        OK,
        ERROR
    }

    private static final long serialVersionUID = 1413738569L;
    
    private byte capacity;
    private boolean changed = false;
    private final Map<Integer, MapleBuddyListEntry> buddies;

    public MapleBuddyList(byte capacity) {
        this.capacity = capacity;
        this.buddies = new LinkedHashMap<>();
    }

    public boolean contains(int characterId) {
        return buddies.containsKey(Integer.valueOf(characterId));
    }

    public byte getCapacity() {
        return capacity;
    }

    public void setCapacity(byte capacity) {
        this.capacity = capacity;
    }

    public MapleBuddyListEntry get(int characterId) {
        return buddies.get(Integer.valueOf(characterId));
    }

    public MapleBuddyListEntry get(String characterName) {
        String lowerCaseName = characterName.toLowerCase();
        for (MapleBuddyListEntry ble : buddies.values()) {
            if (ble.getName().toLowerCase().equals(lowerCaseName)) {
                return ble;
            }
        }
        return null;
    }

    public void put(MapleBuddyListEntry entry) {
        buddies.put(Integer.valueOf(entry.getCharacterId()), entry);
        changed = true;
    }

    public void remove(int characterId) {
        buddies.remove(Integer.valueOf(characterId));
        changed = true;
    }

    public Collection<MapleBuddyListEntry> getBuddies() {
        return buddies.values();
    }

    public boolean isFull() {
        return buddies.size() >= capacity;
    }

    public int[] getBuddyIds() {
        int buddyIds[] = new int[buddies.size()];
        int i = 0;
        for (MapleBuddyListEntry ble : buddies.values()) {
            buddyIds[i++] = ble.getCharacterId();
        }
        return buddyIds;
    }

    public void loadFromTransfer(final List<MapleBuddyListEntry> buddies) {
        for (MapleBuddyListEntry ii : buddies) {
            put(new MapleBuddyListEntry(ii.getName(), ii.getCharacterId(), -1));
        }
    }

    public void loadFromDb(int characterId) throws SQLException {
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT b.buddyid, c.name as buddyname FROM buddyentries as b, characters as c WHERE c.id = b.buddyid AND b.owner = ?")) {
            ps.setInt(1, characterId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    put(new MapleBuddyListEntry(rs.getString("buddyname"), rs.getInt("buddyid"), -1));
                }
            }
        }
    }
    
    public void setChanged(boolean v) {
	this.changed = v;
    }

    public boolean changed() {
	return changed;
    }
}
