/**
 * Ellin é um servidor privado de MapleStory
 * Baseado em um servidor GMS-Like na v.62
 */

package client.player;

import client.player.inventory.types.InventoryType;
import client.player.inventory.Item;
import client.player.inventory.ItemFactory;
import client.player.inventory.ItemPet;
import client.player.inventory.TamingMob;
import client.player.skills.PlayerSkillEntry;
import client.player.skills.PlayerSkillFactory;
import client.player.skills.PlayerSkillMacro;
import community.MapleBuddyList;
import community.MapleGuildCharacter;
import community.MapleParty;
import constants.MapConstants;
import handling.channel.ChannelServer;
import handling.world.service.PartyService;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import server.maps.FieldManager;
import server.maps.SavedLocationType;
import server.maps.portal.Portal;
import server.quest.MapleQuest;
import server.quest.MapleQuestStatus;
import tools.Pair;

/**
 * @brief PlayerFactory
 * @author GabrielSin <gabrielsin@playellin.net>
 * @date   30/03/2018
 */
public class PlayerFactory {
    
    public static ResultSet loadingCharacterStats(Player ret, ResultSet rs, Connection con) {
        try {
            if (!rs.next()) {
                rs.close();
                throw new RuntimeException("Loading char failed (not found)");
            }
            
            ret.name = rs.getString("name");
            ret.level = rs.getInt("level");
            ret.pop = rs.getInt("fame");
            
            ret.stats.str = rs.getInt("str");
            ret.stats.dex = rs.getInt("dex");
            ret.stats.int_ = rs.getInt("int");
            ret.stats.luk = rs.getInt("luk");
            ret.exp.set(rs.getInt("exp"));
            
            ret.stats.hp = rs.getInt("hp");
            ret.stats.maxHP = rs.getInt("maxhp");
            ret.stats.mp = rs.getInt("mp");
            ret.stats.maxMP = rs.getInt("maxmp");
            ret.stats.hpApUsed = rs.getInt("hpApUsed");
            ret.stats.mpApUsed = rs.getInt("mpApUsed");
            ret.stats.hpMpApUsed = rs.getInt("hpMpUsed");
            
            ret.stats.remainingSp = rs.getInt("sp");
            ret.stats.remainingAp = rs.getInt("ap");
            
            ret.meso.set(rs.getInt("meso"));
            
            ret.gm = rs.getInt("gm");
            
            ret.skin = PlayerSkin.getById(rs.getInt("skincolor"));
            ret.gender = rs.getInt("gender");
            ret.job = PlayerJob.getById(rs.getInt("job"));
            
            ret.partner = rs.getInt("spouseId");
            
            ret.hair = rs.getInt("hair");
            ret.eyes = rs.getInt("face");
            ret.accountid = rs.getInt("accountid");

            ret.mapId = rs.getInt("map");
            ret.savedSpawnPoint = rs.getInt("spawnpoint");
            ret.world = rs.getInt("world");

            ret.worldRanking = rs.getInt("rank");
            ret.worldRankingChange = rs.getInt("rankMove");
            ret.jobRanking = rs.getInt("jobRank");
            ret.jobRankingChange = rs.getInt("jobRankMove");

            ret.guild = rs.getInt("guildid");
            if (ret.guild > 0) {
                ret.mgc = new MapleGuildCharacter(ret);
            }
            
            ret.guildRank = rs.getInt("guildrank");
            ret.allianceRank = rs.getInt("allianceRank");
            
            
            ret.buddyList = new MapleBuddyList(rs.getByte("buddyCapacity"));
            ret.petAutoHP = rs.getInt("autoHpPot");
            ret.petAutoMP = rs.getInt("autoMpPot");
            
            ret.getInventory(InventoryType.EQUIP).setSlotLimit(rs.getByte("equipslots"));
            ret.getInventory(InventoryType.USE).setSlotLimit(rs.getByte("useslots"));
            ret.getInventory(InventoryType.SETUP).setSlotLimit(rs.getByte("setupslots"));
            ret.getInventory(InventoryType.ETC).setSlotLimit(rs.getByte("etcslots"));
            
            ret.playtime = rs.getLong("playtime");
            ret.playtimeStart = Calendar.getInstance().getTimeInMillis();
            
            ret.dataString = rs.getString("dataString");
            
            ret.ariantPoints = rs.getInt("ariantPoints");
            ret.merchantMesos = rs.getInt("merchantMesos");
            ret.hasMerchant = rs.getInt("HasMerchant") == 1;
            
            ret.omokWins = rs.getInt("omokwins");
            ret.omokLosses = rs.getInt("omoklosses");
            ret.omokTies = rs.getInt("omokties");
            ret.matchCardWins = rs.getInt("matchcardwins");
            ret.matchCardLosses = rs.getInt("matchcardlosses");
            ret.matchCardTies = rs.getInt("matchcardties");
            
            return rs;
        } catch (RuntimeException | SQLException e) {
            e.printStackTrace(); 
        }
        return null;
    }
    
    public static void loadingCharacterItems(Player ret, boolean channelserver) {
        try {
            for (Pair<Item, InventoryType> mit : ItemFactory.INVENTORY.loadItems(ret.getId(), false)) {
                ret.getInventory(mit.getRight()).addFromDB(mit.getLeft());
                Item item = mit.getLeft();
                if (item.getUniqueId() > -1) {
                    ItemPet pet = item.getPet();
                    if (pet != null && pet.getSummoned()) {
                        ret.addPet(mit.getLeft().getPet());
                    }
                    continue;
                }
                if (mit.getLeft().getRing() != null) {
                    ret.addRingToCache(mit.getLeft().getRing().getRingDatabaseId());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }    
    }
    
    public static void loadingCharacterMount(Player ret, int mountexp, int mountlevel, int mounttiredness) {
        int mountid = ret.getJobType() * 10000000 + 1004;
        if (ret.getInventory(InventoryType.EQUIPPED).getItem((byte) -18) != null) {
            ret.tamingMob = new TamingMob(ret, ret.getInventory(InventoryType.EQUIPPED).getItem((byte) -18).getItemId(), mountid);
        } else {
            ret.tamingMob = new TamingMob(ret, 0, mountid);
        }
        ret.tamingMob.setExp(mountexp);
        ret.tamingMob.setLevel(mountlevel);
        ret.tamingMob.setTiredness(mounttiredness);
    }
    
    public static void loadingCharacterIntoGame(Player ret, boolean channelserver, ResultSet rs) {
        try {
            if (channelserver) {
                FieldManager mapFactory = ChannelServer.getInstance(ret.getClient().getChannel()).getMapFactory();
                ret.field = mapFactory.getMap(ret.mapId);
                if (ret.field == null) {
                    ret.field = mapFactory.getMap(100000000);
                }
                int rMap = ret.field.getForcedReturnId();
                if (rMap != MapConstants.NULL_MAP) {
                    ret.field = mapFactory.getMap(rMap);
                }
                Portal portal = ret.field.getPortal(ret.savedSpawnPoint);
                if (portal == null) {
                    portal = ret.field.getPortal(0);
                    ret.savedSpawnPoint = 0;
                }
                ret.setPosition(portal.getPosition());

                int partyid = rs.getInt("party");
                if (partyid >= 0) {
                    MapleParty party = PartyService.getParty(partyid);
                    if (party != null && party.getMemberById(ret.id) != null) {
                        ret.mpc = party.getMemberById(ret.id);
                        if (ret.mpc != null) {
                            ret.party = party;
                        }
                    }
                }
                    
                final String[] pets = rs.getString("pets").split(",");
                for (int i = 0; i < ret.petStore.length; i++) {
                    ret.petStore[i] = Byte.parseByte(pets[i]);
                }
            }
        } catch (SQLException | NumberFormatException e) {
            e.printStackTrace();
        }    
    }
    
    public static void loadingCharacterLocations(Player ret, PreparedStatement ps, ResultSet rs, Connection con) {
        try {
            ps = con.prepareStatement("SELECT mapid FROM trocklocations WHERE characterid = ?");
            ps.setInt(1, ret.getId());
            rs = ps.executeQuery();
            int r = 0;
            while (rs.next()) {
                ret.rocks[r] = rs.getInt("mapid");
                r++;
            }
            while (r < 10) {
                ret.rocks[r] = 999999999;
                r++;
            }
            rs.close();
            ps.close();

            ps = con.prepareStatement("SELECT mapid FROM regrocklocations WHERE characterid = ?");
            ps.setInt(1, ret.getId());
            rs = ps.executeQuery();
            r = 0;
            while (rs.next()) {
                ret.regrocks[r] = rs.getInt("mapid");
                r++;
            }
            while (r < 5) {
                ret.regrocks[r] = 999999999;
                r++;
            }
            rs.close();
            ps.close();
            
            ps = con.prepareStatement("SELECT `locationtype`,`map` FROM savedlocations WHERE characterid = ?");
            ps.setInt(1, ret.getId());
            rs = ps.executeQuery();
            while (rs.next()) {
                String locationType = rs.getString("locationtype");
                int mapid = rs.getInt("map");
                ret.savedLocations[SavedLocationType.valueOf(locationType).ordinal()] = mapid;
            }
            rs.close();
            ps.close();
            
        } catch (SQLException | NumberFormatException e) {
            e.printStackTrace();
        }
    }
    
    public static void loadingCharacterAccountStats(Player ret, PreparedStatement ps, ResultSet rs, Connection con) {
        try {
            ps = con.prepareStatement("SELECT * FROM accounts WHERE id = ?");
            ps.setInt(1, ret.getAccountID());
            rs = ps.executeQuery();
            while (rs.next()) {
                ret.getClient().setAccountName(rs.getString("name"));
            }
            rs.close();
            ps.close();
        } catch (SQLException | NumberFormatException e) {
            e.printStackTrace();
        }
    }
    
    public static void loadingCharacterQuestStats(Player ret, PreparedStatement ps, PreparedStatement pse, ResultSet rs, Connection con) {
        try {
            ps = con.prepareStatement("SELECT * FROM queststatus WHERE characterid = ?");
            ps.setInt(1, ret.getId());
            rs = ps.executeQuery();
            pse = con.prepareStatement("SELECT * FROM questprogress WHERE queststatusid = ?");
            while (rs.next()) {
                MapleQuest q = MapleQuest.getInstance(rs.getShort("quest"));
                MapleQuestStatus status = new MapleQuestStatus(q, MapleQuestStatus.Status.getById(rs.getInt("status")));
                long cTime = rs.getLong("time");
                if (cTime > -1) {
                    status.setCompletionTime(cTime * 1000);
                }
                status.setForfeited(rs.getInt("forfeited"));
                ret.quests.put(q.getId(), status);
                pse.setInt(1, rs.getInt("queststatusid"));
                try (ResultSet rsProgress = pse.executeQuery()) {
                    while (rsProgress.next()) {
                        status.setProgress(rsProgress.getInt("progressid"), rsProgress.getString("progress"));
                    }
                }

            }
            rs.close();
            ps.close();
            pse.close();
        } catch (SQLException | NumberFormatException e) {
            e.printStackTrace();
        }  
    }
    
    public static void loadingCharacterSkillsAndMacros(Player ret, PreparedStatement ps, ResultSet rs, Connection con) {
        try {
            ps = con.prepareStatement("SELECT skillid, skilllevel, masterlevel FROM skills WHERE characterid = ?");
            ps.setInt(1, ret.getId());
            rs = ps.executeQuery();
                while (rs.next()) {
                    ret.skills.put(PlayerSkillFactory.getSkill(rs.getInt("skillid")), new PlayerSkillEntry(rs.getInt("skilllevel"), rs.getInt("masterlevel")));
                }
            rs.close();
            ps.close();

            ps = con.prepareStatement("SELECT `position`,`name`,`silent`,`skill1`,`skill2`,`skill3` " + "FROM `skillmacros` WHERE `characterid` = ? ORDER BY `position` DESC");
            ps.setInt(1, ret.id);
            rs = ps.executeQuery();
            byte macroPos = 0;
            for (boolean first = true; rs.next(); first = false) {
                macroPos = rs.getByte(1);
                if (first)
                        ret.skillMacros = new PlayerSkillMacro[macroPos + 1];
                ret.skillMacros[macroPos] = new PlayerSkillMacro(rs.getString(2), rs.getBoolean(3), rs.getInt(4), rs.getInt(5), rs.getInt(6));
            }
            if (ret.skillMacros == null) {
                ret.skillMacros = new PlayerSkillMacro[0];
            }
            for (macroPos--; macroPos >= 0; macroPos--) {
                ret.skillMacros[macroPos] = new PlayerSkillMacro("", false, 0, 0, 0); 
            }
            rs.close();
            ps.close();

            ps = con.prepareStatement("SELECT `key`,`type`,`action` FROM keymap WHERE characterid = ?");
            ps.setInt(1, ret.getId());
            rs = ps.executeQuery();
            while (rs.next()) {
                int key = rs.getInt("key");
                int type = rs.getInt("type");
                int action = rs.getInt("action");
                ret.keymap.put(Integer.valueOf(key), new PlayerKeyBinding(type, action));
            }
            rs.close();
            ps.close();
        } catch (SQLException | NumberFormatException e) {
            try {
                throw new SQLException("Failed to save keymap/macros of character " + ret.name, e);
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        } 
    }
    
    public static void loadingCharacterFame(Player ret, PreparedStatement ps, ResultSet rs, Connection con) {
        try {
            ps = con.prepareStatement("SELECT `characterid_to`,`when` FROM famelog WHERE characterid = ? AND DATEDIFF(NOW(),`when`) < 30");
            ps.setInt(1, ret.getId());
            rs = ps.executeQuery();
            ret.lastFameTime = 0;
            ret.lastMonthFameIDs = new ArrayList<>(31);
            while (rs.next()) {
                ret.lastFameTime = Math.max(ret.lastFameTime, rs.getTimestamp("when").getTime());
                ret.lastMonthFameIDs.add(rs.getInt("characterid_to"));
            }
            rs.close();
            ps.close();
        } catch (SQLException | NumberFormatException e) {
            e.printStackTrace();
        }
    }
}
