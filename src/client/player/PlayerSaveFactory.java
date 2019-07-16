package client.player;

import client.player.inventory.Inventory;
import client.player.inventory.types.InventoryType;
import client.player.inventory.Item;
import client.player.inventory.ItemFactory;
import client.player.inventory.ItemPet;
import client.player.skills.PlayerSkill;
import client.player.skills.PlayerSkillEntry;
import client.player.skills.PlayerSkillMacro;
import community.MapleBuddyListEntry;
import constants.MapConstants;
import database.DatabaseException;
import handling.world.PlayerCoolDownValueHolder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import server.maps.SavedLocationType;
import server.maps.portal.Portal;
import server.quest.MapleQuestStatus;
import tools.Pair;

/**
 * @author GabrielSin (http://forum.ragezone.com/members/822844.html)
 */

public class PlayerSaveFactory {
    
    public enum DeleteType {
        
        CHARACTER("characters", "id"),
        SKILL("skills", "characterid"),
        BUDDY("buddies", "characterid"),
        BUDDY_ENTRIES("buddyentries", "owner"),
        BBS_THREADS("bbs_threads", "postercid"),
        KEYMAP("keymap", "characterid"),
        FAME_LOG("famelog", "characterid"),
        QUEST("queststatus", "characterid"),
        WISH_LIST("wishlist", "characterid"),
        SKILL_MACRO("scrillmacros", "characterid"),
        SKILL_COOLDOWN("cooldowns", "charid"),
        SAVED_LOCATION("savedlocations", "characterid"),
        REG_LOCATIONS("regrocklocations", "characterid"),
        INVENTORY_ITEMS("inventoryitems", "characterid"),
        TROCK_LOCATIONS("trocklocations", "characterid");
        
        String type, field;
        
        private DeleteType (String type, String field) {
            this.type = type;
            this.field = field;
        }
        
        public void removeFromType(Connection con, int typeInt) throws SQLException {
            try {
                String sql = "DELETE FROM " + this.type + " WHERE " + this.field + " = ?";
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setInt(1, typeInt);
                    ps.executeUpdate();
                }       
            } catch (SQLException e) {
                e.printStackTrace();    
            } 
        }
    }
    
    public static void savingCharacterStats(Player ret, PreparedStatement ps, Connection con) {
        try {
            /*                                                 1          2        3        4        5         6              7        8       9       10         11        12      13           14        15             16         17       18         19       20            21          22           23              24            25             26               27            28            29                   30                31                32               33               34                35                 36                  37            38            39          40           41             42           43              44              45             46             47            48           49      */
            ps = con.prepareStatement("UPDATE characters SET level = ?, fame = ?, str = ?, dex = ?, luk = ?, `int` = ?, " + "exp = ?, hp = ?, mp = ?, maxhp = ?, maxmp = ?, sp = ?, ap = ?, " + "gm = ?, skincolor = ?, gender = ?, job = ?, hair = ?, face = ?, map = ?, " + "meso = ?, hpApUsed = ?, mpApUsed = ?, spawnpoint = ?, party = ?, buddyCapacity = ?, mountlevel = ?, mountexp = ?, mounttiredness = ?, alliancerank = ?, ariantPoints = ?, hpMpUsed = ?, matchcardwins = ?, matchcardlosses = ?, matchcardties = ?, omokwins = ?, omoklosses = ?, omokties = ?, pets = ?, autoHpPot = ?, autoMpPot = ?, equipslots = ?, useslots = ?, setupslots = ?, etcslots = ?, spouseId = ?,  playtime = ?, dataString = ? WHERE id = ?");
            ps.setInt(1, ret.level);
            ps.setInt(2, ret.pop);
            ps.setInt(3, ret.stats.str);
            ps.setInt(4, ret.stats.dex);
            ps.setInt(5, ret.stats.luk);
            ps.setInt(6, ret.stats.int_);
            ps.setInt(7, ret.exp.get() < 0 ? 0 : ret.exp.get());
            ps.setInt(8, ret.stats.hp);
            ps.setInt(9, ret.stats.mp);
            ps.setInt(10, ret.stats.maxHP);
            ps.setInt(11, ret.stats.maxMP);
            ps.setInt(12, ret.stats.remainingSp);
            ps.setInt(13, ret.stats.remainingAp);
            ps.setInt(14, ret.gm);
            ps.setInt(15, ret.skin.getId());
            ps.setInt(16, ret.gender);
            ps.setInt(17, ret.job.getId());
            ps.setInt(18, ret.hair);
            ps.setInt(19, ret.eyes);
            if (ret.field == null) {
                ps.setInt(20, 0);
            } else {
                if (ret.field.getForcedReturnId() != MapConstants.NULL_MAP) {
                    ps.setInt(20, ret.field.getForcedReturnId());
                } else {
                    ps.setInt(20, ret.stats.getHp() < 1 ? ret.field.getReturnMapId() : ret.field.getId());
                }
            }
            ps.setInt(21, ret.meso.get());
            ps.setInt(22, ret.stats.hpApUsed);
            ps.setInt(23, ret.stats.mpApUsed);
            if (ret.field == null) {
                ps.setInt(24, 0);
            } else {
                Portal closest = ret.field.findClosestPlayerSpawnpoint(ret.getPosition());
                ps.setInt(24, closest != null ? closest.getId() : 0);
            }
            
            ps.setInt(25, ret.party != null ? ret.party.getId() : -1);
            ps.setInt(26, ret.buddyList.getCapacity());
            
            if (ret.tamingMob != null) {
                ps.setInt(27, ret.tamingMob.getLevel());
                ps.setInt(28, ret.tamingMob.getExp());
                ps.setInt(29, ret.tamingMob.getTiredness());
            } else {
                ps.setInt(27, 1);
                ps.setInt(28, 0);
                ps.setInt(29, 0);
            }
            
            ps.setInt(30, ret.allianceRank);
            ps.setInt(31, ret.ariantPoints);
            ps.setInt(32, ret.stats.hpMpApUsed);
            ps.setInt(33, ret.matchCardWins);
            ps.setInt(34, ret.matchCardLosses);
            ps.setInt(35, ret.matchCardTies);
            ps.setInt(36, ret.omokWins);
            ps.setInt(37, ret.omokLosses);
            ps.setInt(38, ret.omokTies);
            
            final StringBuilder petz = new StringBuilder();
            int petLength = 0;
            for (final ItemPet pet : ret.pets) {
                if (pet.getSummoned()) {
                    pet.saveDatabase();
                    petz.append(pet.getInventoryPosition());
                    petz.append(",");
                    petLength++;
                }
            }
            while (petLength < 3) {
                petz.append("-1,");
                petLength++;
            }
            final String petstring = petz.toString();
            ps.setString(39, petstring.substring(0, petstring.length() - 1));
            
            ps.setInt(40, ret.petAutoHP != 0 && ret.getItemQuantity(ret.petAutoHP) >= 1 ? ret.petAutoHP : 0);
            ps.setInt(41, ret.petAutoMP != 0 && ret.getItemQuantity(ret.petAutoMP) >= 1 ? ret.petAutoMP : 0);
            
            ps.setInt(42, ret.getInventory(InventoryType.getByType((byte) InventoryType.EQUIP.getType())).getSlotLimit());
            ps.setInt(43, ret.getInventory(InventoryType.getByType((byte) InventoryType.USE.getType())).getSlotLimit());
            ps.setInt(44, ret.getInventory(InventoryType.getByType((byte) InventoryType.SETUP.getType())).getSlotLimit());
            ps.setInt(45, ret.getInventory(InventoryType.getByType((byte) InventoryType.ETC.getType())).getSlotLimit());
            
            ps.setInt(46, ret.partner);
            
            ps.setLong(47, ret.getPlaytime());
            
            ps.setString(48, ret.dataString);
            
            ps.setInt(49, ret.id);
            
            if (ps.executeUpdate() < 1) {
                ps.close();
                throw new DatabaseException("Character not in database (" + ret.id + ")");
            }
            ps.close();
        } catch(SQLException ex) {
            ex.printStackTrace(); 
        }
    }
    
    public static PreparedStatement savingCharacterSkillMacros(Player ret, PreparedStatement ps, Connection con) {
        try {
            DeleteType.SKILL_MACRO.removeFromType(con, ret.id);
            ps = con.prepareStatement("INSERT INTO `skillmacros` " + "(`characterid`,`position`,`name`,`silent`,`skill1`,`skill2`,`skill3`) " + "VALUES (?,?,?,?,?,?,?)");
            ps.setInt(1, ret.id);
            for (byte pos = 0; pos < ret.skillMacros.length; pos++) {
                PlayerSkillMacro macro = ret.skillMacros[pos];
                if (macro.getName().isEmpty() && !macro.isSilent() && macro.getFirstSkill() == 0 && macro.getSecondSkill() == 0 && macro.getThirdSkill() == 0) {
                    continue;
                }
                ps.setByte(2, pos);
                ps.setString(3, macro.getName());
                ps.setBoolean(4, macro.isSilent());
                ps.setInt(5, macro.getFirstSkill());
                ps.setInt(6, macro.getSecondSkill());
                ps.setInt(7, macro.getThirdSkill());
                ps.addBatch();
            }
            ps.executeBatch();
            ps.close();
            
            ret.setChangedSkillMacros(false);
        } catch(SQLException ex) {
            ex.printStackTrace();  
        }
        return null;
    }
    
    public static void savingCharacterQuests(Player ret, PreparedStatement ps, Connection con) {
        try {
            DeleteType.QUEST.removeFromType(con, ret.id);
            ps = con.prepareStatement("INSERT INTO `queststatus` (`queststatusid`, `characterid`, `quest`, `status`, `time`, `forfeited`) VALUES (DEFAULT, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            try (PreparedStatement psee = con.prepareStatement("INSERT INTO questprogress VALUES (DEFAULT, ?, ?, ?)")) {
                ps.setInt(1, ret.id);
                for (MapleQuestStatus q : ret.quests.values()) {
                    ps.setInt(2, q.getQuest().getId());
                    ps.setInt(3, q.getStatus().getId());
                    ps.setInt(4, (int) (q.getCompletionTime() / 1000));
                    ps.setInt(5, q.getForfeited());
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        rs.next();
                        for (int mob : q.getProgress().keySet()) {
                            psee.setInt(1, rs.getInt(1));
                            psee.setInt(2, mob);
                            psee.setString(3, q.getProgress(mob));
                            psee.addBatch();
                        }
                        psee.executeBatch();
                        rs.close();
                    }
                }
                ps.close();
            }
        } catch(SQLException ex) {
            ex.printStackTrace(); 
        }
    }
    
    public static void savingCharacterSkills(Player ret, PreparedStatement ps, Connection con) {
        try {
            DeleteType.SKILL.removeFromType(con, ret.id);
            ps = con.prepareStatement("INSERT INTO `skills` (`characterid`, `skillid`, `skilllevel`, `masterlevel`) VALUES (?, ?, ?, ?)");
            ps.setInt(1, ret.id);
            for (Entry<PlayerSkill, PlayerSkillEntry> skill : ret.skills.entrySet()) {
                ps.setInt(2, skill.getKey().getId());
                ps.setInt(3, skill.getValue().skillevel);
                ps.setInt(4, skill.getValue().masterlevel);
                ps.executeUpdate();
            }
            ps.close();
        } catch(SQLException ex) {
            ex.printStackTrace(); 
        }
    }
    
    public static void savingCharacterSkillCoolDown(Player ret, PreparedStatement ps, Connection con) {
        try {
            List<PlayerCoolDownValueHolder> cd = ret.getAllCooldowns();
            if (cd.size() > 0) {
                ps = con.prepareStatement("INSERT INTO `cooldowns` (`charid`, `skillid`, `starttime`, `length`) VALUES (?, ?, ?, ?)");
                ps.setInt(1, ret.id);
                for (final PlayerCoolDownValueHolder cooling : cd) {
                    ps.setInt(2, cooling.skillId);
                    ps.setLong(3, cooling.startTime);
                    ps.setLong(4, cooling.length);
                    ps.execute();
                }
                ps.close();
            }
        } catch (SQLException ex) {
            ex.printStackTrace(); 
        }
    }
    
    public static void savingCharacterKeymap(Player ret, PreparedStatement ps, Connection con) {
        try {
            DeleteType.KEYMAP.removeFromType(con, ret.id);
            ps = con.prepareStatement("INSERT INTO `keymap` (`characterid`, `key`, `type`, `action`) VALUES (?, ?, ?, ?)");
            ps.setInt(1, ret.id);
            for (Entry<Integer, PlayerKeyBinding> keybinding : ret.keymap.entrySet()) {
                ps.setInt(2, keybinding.getKey().intValue());
                ps.setInt(3, keybinding.getValue().getType());
                ps.setInt(4, keybinding.getValue().getAction());
                ps.executeUpdate();
            }
            ps.close();
        } catch (SQLException ex) {
            ex.printStackTrace(); 
        }
    }
    
    public static void savingCharacterSavedLocations(Player ret, PreparedStatement ps, Connection con) {
        try {
            DeleteType.SAVED_LOCATION.removeFromType(con, ret.id);
            ps = con.prepareStatement("INSERT INTO `savedlocations` (`characterid`, `locationtype`, `map`) VALUES (?, ?, ?)");
            ps.setInt(1, ret.id);
            for (SavedLocationType savedLocationType : SavedLocationType.values()) {
                if (ret.savedLocations[savedLocationType.ordinal()] != -1) {
                    ps.setString(2, savedLocationType.name());
                    ps.setInt(3, ret.savedLocations[savedLocationType.ordinal()]);
                    ps.executeUpdate();
                }
            }
            ps.close();
            ret.setChangedSavedLocations(false);
        } catch (SQLException ex) {
            ex.printStackTrace(); 
        }
    }
    
    public static void savingCharacterBuddy(Player ret, PreparedStatement ps, Connection con) {
        try {
            DeleteType.BUDDY_ENTRIES.removeFromType(con, ret.id);
            ps = con.prepareStatement("INSERT INTO `buddyentries` (owner, `buddyid`) VALUES (?, ?)");
            ps.setInt(1, ret.id);
            for (MapleBuddyListEntry entry : ret.buddyList.getBuddies()) {
                ps.setInt(2, entry.getCharacterId());
                ps.execute();
            }
            ps.close();
        } catch (SQLException ex) {
            ex.printStackTrace(); 
        }
    }
    
    public static void savingCharacterTrockLocations(Player ret, PreparedStatement ps, Connection con) {
        try {
            DeleteType.TROCK_LOCATIONS.removeFromType(con, ret.id);
            for (int i = 0; i < ret.rocks.length; i++) {
                if (ret.rocks[i] != MapConstants.NULL_MAP) {
                    ps = con.prepareStatement("INSERT INTO `trocklocations` (`characterid`, `mapid`) VALUES(?, ?) ");
                    ps.setInt(1, ret.id);
                    ps.setInt(2, ret.rocks[i]);
                    ps.execute();
                    ps.close();
                }
            }
            ret.setChangedTrockLocations(false);
        } catch (SQLException ex) {
            ex.printStackTrace(); 
        }
    }
    
    public static void savingCharacterRegRockLocations(Player ret, PreparedStatement ps, Connection con) {
        try {
            DeleteType.REG_LOCATIONS.removeFromType(con, ret.id);
            for (int i = 0; i < ret.regrocks.length; i++) {
                if (ret.regrocks[i] != MapConstants.NULL_MAP) {
                    ps = con.prepareStatement("INSERT INTO `regrocklocations` (`characterid`, `mapid`) VALUES(?, ?) ");
                    ps.setInt(1, ret.id);
                    ps.setInt(2, ret.regrocks[i]);
                    ps.execute();
                    ps.close();
                }
            }
            ret.setChangedRegrockLocations(false);
        } catch (SQLException ex) {
            ex.printStackTrace(); 
        }
    }
    
    public static void savingCharacterInventory(Player ret) {
        try {
            List<Pair<Item, InventoryType>> itemsWithType = new ArrayList<>();
            for (Inventory iv : ret.inventory) {
                for (Item item : iv.list()) {
                    itemsWithType.add(new Pair<>(item, iv.getType()));
                }
            }
            ItemFactory.INVENTORY.saveItems(itemsWithType, ret.id);
        } catch (SQLException ex) {
            ex.printStackTrace(); 
        }
    }
}
