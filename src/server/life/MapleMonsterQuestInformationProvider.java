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
package server.life;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import database.DatabaseConnection;
import java.sql.SQLException;

public class MapleMonsterQuestInformationProvider {

    private static MapleMonsterQuestInformationProvider instance = null;
    private final Map<Integer, List<QuestDropEntry>> drops = new HashMap<>();
    
    public static MapleMonsterQuestInformationProvider getInstance() {
        if (instance == null) {
            instance = new MapleMonsterQuestInformationProvider();
        }
        return instance;
    }

    public List<QuestDropEntry> retrieveDropChances(int monsterId) {
        if (drops.containsKey(monsterId)) {
            return drops.get(monsterId);
        }
        List<QuestDropEntry> ret = new LinkedList<>();
        PreparedStatement ps = null;
	ResultSet rs = null;
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT itemid, chance, monsterid, questid FROM monsterquestdrops " + " WHERE (monsterid = ? AND chance >= 0) OR (monsterid <= 0)");
            ps.setInt(1, monsterId);
            rs = ps.executeQuery();
            while (rs.next()) {
                ret.add(new QuestDropEntry(rs.getInt("itemid"), rs.getInt("chance"),  rs.getInt("questid")));
            }
            con.close();
        } catch (SQLException e) {
            return ret;
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
                if (rs != null) {
                    rs.close();
                }
                if (con != null && !con.isClosed()) {
                    con.close();
                }
            } catch (SQLException ignore) {
                return ret;
            }
	}
        drops.put(monsterId, ret);
        return ret;
    }

    public void clearDrops() {
        drops.clear();
    }
    
    public static class QuestDropEntry {

        public QuestDropEntry(int itemId, int chance, int questid) {
            this.itemId = itemId;
            this.chance = chance;
            this.questid = questid;
        }

        public int itemId;
        public int chance;
        public int questid;
        public int assignedRangeStart;
        public int assignedRangeLength;

        @Override
        public String toString() {
            return itemId + " chance: " + chance;
        }
    }
}
