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
package client.player;

import client.Client;
import client.player.Player.FameStatus;
import client.player.buffs.BuffStat;
import static client.player.buffs.BuffStat.*;
import client.player.buffs.BuffStatValueHolder;
import client.player.buffs.Disease;
import client.player.buffs.DiseaseValueHolder;
import client.player.inventory.Equip;
import client.player.inventory.Inventory;
import client.player.inventory.InventoryIdentifier;
import client.player.inventory.types.InventoryType;
import client.player.inventory.Item;
import client.player.inventory.ItemFactory;
import client.player.inventory.ItemPet;
import client.player.inventory.ItemRing;
import client.player.inventory.TamingMob;
import client.player.skills.PlayerSkill;
import client.player.skills.PlayerSkillEntry;
import client.player.skills.PlayerSkillFactory;
import client.player.skills.PlayerSkillMacro;
import client.player.violation.CheatTracker;
import community.MapleBuddyList;
import constants.ExperienceConstants;
import server.quest.MapleQuestStatus;
import community.MapleGuild;
import community.MapleGuildCharacter;
import java.awt.Point;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledFuture;
import community.MapleParty;
import constants.*;
import database.DatabaseConnection;
import database.DatabaseException;
import java.util.Date;
import java.util.EnumMap;
import java.util.concurrent.locks.Lock;
import packet.transfer.write.OutPacket;
import handling.channel.ChannelServer;
import handling.world.messenger.MapleMessenger;
import handling.world.messenger.MapleMessengerCharacter;
import community.MaplePartyCharacter;
import community.MaplePartyOperation;
import handling.world.PlayerBuffValueHolder;
import handling.world.PlayerCoolDownValueHolder;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import scripting.event.EventInstanceManager;
import server.itens.ItemInformationProvider;
import server.shops.Shop;
import server.MapleStatEffect;
import server.itens.StorageKeeper;
import server.itens.Trade;
import server.life.MapleMonster;
import server.maps.object.AbstractAnimatedFieldObject;
import server.maps.MapleDoor;
import server.maps.Field;
import server.maps.object.FieldObject;
import server.maps.object.FieldObjectType;
import server.maps.MapleSummon;
import server.quest.MapleQuest;
import tools.Pair;
import server.itens.InventoryManipulator;
import server.minirooms.Merchant;
import server.minirooms.PlayerShop;
import server.life.MobSkill;
import cashshop.CashShop;
import client.ClientLoginState;
import client.player.inventory.types.ItemRingType;
import constants.SkillConstants;
import handling.world.PlayerBuffStorage;
import handling.world.service.BroadcastService;
import handling.world.service.GuildService;
import handling.world.service.MessengerService;
import handling.world.service.PartyService;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicInteger;
import packet.creators.EffectPackets;
import packet.creators.GuildPackets;
import packet.creators.MonsterPackets;
import packet.creators.PacketCreator;
import packet.creators.PartyPackets;
import packet.creators.PetPackets;
import constants.SkillConstants.Beginner;
import constants.SkillConstants.Bishop;
import constants.SkillConstants.Brawler;
import constants.SkillConstants.Buccaneer;
import constants.SkillConstants.Corsair;
import constants.SkillConstants.Crusader;
import constants.SkillConstants.DarkKnight;
import constants.SkillConstants.FPArchMage;
import constants.SkillConstants.Gm;
import constants.SkillConstants.Hermit;
import constants.SkillConstants.Hero;
import constants.SkillConstants.ILArchMage;
import constants.SkillConstants.Magician;
import constants.SkillConstants.Marauder;
import constants.SkillConstants.Priest;
import constants.SkillConstants.Ranger;
import constants.SkillConstants.Sniper;
import constants.SkillConstants.SpearMan;
import constants.SkillConstants.SuperGm;
import constants.SkillConstants.Swordman;
import packet.creators.MinigamePackets;
import server.shops.ShopFactory;
import tools.TimerTools.CharacterTimer;
import server.minirooms.Minigame;
import server.minirooms.PlayerShopItem;
import server.life.MapleLifeFactory;
import server.maps.FieldLimit;
import server.maps.MapleFoothold;
import server.maps.SavedLocationType;
import server.maps.portal.Portal;
import tools.ConcurrentEnumMap;
import tools.FileLogger;
import server.partyquest.mcpq.MCField;
import server.partyquest.mcpq.MCField.MCTeam;
import server.partyquest.mcpq.MCParty;
import server.partyquest.mcpq.MonsterCarnival;
import tools.Randomizer;
import tools.TimerTools.EventTimer;
import tools.locks.MonitoredLockType;
import tools.locks.MonitoredReentrantLock;

public class Player extends AbstractAnimatedFieldObject implements Serializable {
    
    protected int id;
    protected int world;
    protected int accountid;
    protected int level;
    protected int worldRanking;
    protected int worldRankingChange;
    protected int jobRanking;
    protected int jobRankingChange;
    protected int hair;
    protected int eyes;
    protected int gender;
    protected int pop;
    protected int gm;
    protected String name;
   
    protected int savedLocations[];
    
    protected final AtomicInteger exp = new AtomicInteger();
    protected final AtomicInteger meso = new AtomicInteger();
    
    protected int savedSpawnPoint;
    
    protected PlayerSkin skin = PlayerSkin.NORMAL;
    protected PlayerJob job = PlayerJob.BEGINNER;
    
    private Client client;
   
    protected final PlayerStatsManager stats;
    protected Map<Short, MapleQuestStatus> quests;
    private transient EventInstanceManager eventInstance;
    private transient Map<BuffStat, BuffStatValueHolder> effects = new ConcurrentEnumMap<>(BuffStat.class);
    protected Map<Integer, PlayerKeyBinding> keymap = new LinkedHashMap<>();
    
    // Field
    protected int mapId, doorSlot = -1;
    private boolean canDoor = true;
    protected transient Field field;
    private transient Set<MapleMonster> controlled;
    private transient Set<FieldObject> visibleMapObjects;
    private transient ReentrantReadWriteLock visibleMapObjectsLock;
    private Map<Integer, MapleDoor> doors = new LinkedHashMap<>();
    // Skills
    private int battleShipHP = 0;
    private int energyBar = 0;
    private boolean Berserk = false;
    protected Map<PlayerSkill, PlayerSkillEntry> skills = new LinkedHashMap<>();
    public volatile PlayerSkillMacro[] skillMacros;
    private ScheduledFuture<?> beholderHealingSchedule;
    private ScheduledFuture<?> beholderBuffSchedule;
    private ScheduledFuture<?> BerserkSchedule;
    private transient Map<Disease, DiseaseValueHolder> diseases;
    private transient Map<Integer, PlayerCoolDownValueHolder> coolDowns;
    // AntiCheater
    private boolean hasCheat = false;
    public long lastRequestTime = 0, lastUsedCashItem;
    public long lastSelectNPCTime = 0;
    public long lastAttackTime = 0;
    public long lastHitTime = System.currentTimeMillis();
    public long lastHPTime, lastMPTime;
    private final transient CheatTracker antiCheat;
    private ScheduledFuture<?> dragonBloodSchedule;
    private ScheduledFuture<?> mapTimeLimitTask = null;
    //Aneis
    private List<ItemRing> crushRings = new LinkedList<>();
    private List<ItemRing> friendshipRings = new LinkedList<>();
    private List<ItemRing> weddingRings = new LinkedList<>();
    // Mounts
    protected TamingMob tamingMob;
    // Game
    protected int omokWins;
    protected int omokTies;
    protected int omokLosses;
    protected int matchCardWins;
    protected int matchCardTies;
    protected int matchCardLosses;
    private Minigame miniGame;
    // Guild
    protected int guild;
    protected int guildRank;
    protected int allianceRank;
    protected MapleGuildCharacter mgc = null;
    // Party
    protected MapleParty party;
    protected MaplePartyCharacter mpc = null;
    // Interaction
    protected int merchantMesos;
    protected boolean hasMerchant;
    protected MapleBuddyList buddyList;
    private transient Trade trade = null;
    private Shop shop = null;
    private PlayerShop playerShop = null;
    private Merchant hiredMerchant = null;
    // Messenger
    private MapleMessenger messenger;
    // Fame
    protected long lastFameTime;
    protected List<Integer> lastMonthFameIDs;
    // Summons
    private Map<Integer, MapleSummon> summons;
    private final List<MapleSummon> pirateSummons = new LinkedList<>();
    // Inventory
    protected final Inventory[] inventory;
    private boolean shield = false;
    private int chair, itemEffect, slots = 0;
    protected int petAutoHP, petAutoMP;
    private StorageKeeper storage = null;
    private static final List<Pair<Byte, Integer>> inventorySlots = new ArrayList<>();
    private static ItemInformationProvider ii = ItemInformationProvider.getInstance();
    // Mariage
    protected int partner;
    private int spouseItemId = 0;
    // GM
    private boolean hidden;
    // Pet
    protected List<ItemPet> pets;
    protected byte[] petStore;
    // Carnival
    private MCTeam MCPQTeam;
    private MCParty MCPQParty;
    private MCField MCPQField;
    private int availableCP = 0;
    private int totalCP = 0;  
    // Playtime
    public long playtimeStart;
    public long playtime;
    // Others
    private int targetHpBarHash = 0;
    private long targetHpBarTime = 0;
    protected int[] rocks, regrocks;
    private String chalkBoardText;
    private boolean challenged = false, allowMapChange = true, canSmega = true, smegaEnabled = true;
    private boolean changedTrockLocations, changedRegrockLocations, changedSavedLocations, changedReports, changedSkillMacros;
    protected int votePoints, ariantPoints, ringRequest;
    private long lastPortalEntry = 0, lastCatch = 0, useTime = 0;
    private Date time;
    private CashShop cashShop;
    private long npcCd;
    private RockPaperScissors rps = null;
    private int owlSearch;
    // Speed Quiz Test 
    private transient SpeedQuiz sq;
    private long lastSpeedQuiz;
    // Third kill feature
    private long loginTime = System.currentTimeMillis();
    private AtomicInteger mobKills = new AtomicInteger(0);
    
    protected ScheduledFuture<?> expireTask;
    protected ScheduledFuture<?> recoveryTask;
    private final List<ScheduledFuture<?>> timers = new ArrayList<>();
    
    public String dataString;
    public String[] ariantRoomLeader = new String[3];
    public int[] ariantRoomSlot = new int[3];
    
    private int newWarpMap = -1;
    private boolean canWarpMap = true; 
    private int canWarpCounter = 0;     
    
    private Lock chrLock = new MonitoredReentrantLock(MonitoredLockType.CHR, true);
    private Lock effLock = new MonitoredReentrantLock(MonitoredLockType.EFF, true);
    private Lock saveLock = new MonitoredReentrantLock(MonitoredLockType.CHR_SAVE, true);
    private Lock prtLock = new MonitoredReentrantLock(MonitoredLockType.PRT);

    private Player(final boolean ChannelServer) {
        setStance(0);
        inventory = new Inventory[InventoryType.values().length];
        for (InventoryType type : InventoryType.values()) {
            inventory[type.ordinal()] = new Inventory(type, (byte) 100);
        }

        savedLocations = new int[SavedLocationType.values().length];
        for (int i = 0; i < SavedLocationType.values().length; i++) {
            savedLocations[i] = -1;
        } 
        if (ChannelServer) {
            changedReports = false;
            changedTrockLocations = false;
            changedRegrockLocations = false;
            changedSavedLocations = false;
            changedSkillMacros = false;
            lastHPTime = 0;
            lastMPTime = 0;
            petStore = new byte[3];
            for (int i = 0; i < petStore.length; i++) {
                petStore[i] = (byte) -1;
            }
            pets = new ArrayList<>();
            visibleMapObjects = new LinkedHashSet<>();
            visibleMapObjectsLock = new ReentrantReadWriteLock();
            controlled = new LinkedHashSet<>();
            summons = new LinkedHashMap<>();
            diseases = new ConcurrentEnumMap<>(Disease.class);
            coolDowns = new LinkedHashMap<>();
            rocks = new int[10];
            regrocks = new int[5];
        }
        stats = new PlayerStatsManager(this);
        quests = new LinkedHashMap<>();
        antiCheat = new CheatTracker(this);
        setPosition(new Point(0, 0));
    }

    public Player getThis() {
        return this;
    }
    
    public static Player loadinCharacterDatabase(int characterId, Client client, boolean channelserver) throws SQLException {
        final Player ret = new Player(channelserver);
        ret.client = client;
        ret.id = characterId;
        
        PreparedStatement ps = null;
        PreparedStatement pse = null;
        ResultSet rs = null;
        
        try {
            Connection con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT * FROM characters WHERE id = ?");
            ps.setInt(1, ret.getId());
            rs = ps.executeQuery();
            
            PlayerFactory.loadingCharacterStats(ret, rs, con);
            
            PlayerFactory.loadingCharacterItems(ret, channelserver);
            
            PlayerFactory.loadingCharacterMount(ret, rs.getInt("mountexp"), rs.getInt("mountlevel"), rs.getInt("mounttiredness"));
            
            PlayerFactory.loadingCharacterIntoGame(ret, channelserver, rs);
            
            rs.close();
            ps.close();
          
            
            PlayerFactory.loadingCharacterAccountStats(ret, ps, rs, con);
            
            ret.cashShop = new CashShop(ret.accountid, ret.id);
            
            if (channelserver) {
                PlayerFactory.loadingCharacterQuestStats(ret, ps, pse, rs, con);
                
                PlayerFactory.loadingCharacterSkillsAndMacros(ret, ps, rs, con);
                
                PlayerFactory.loadingCharacterLocations(ret, ps, rs, con);
                
                PlayerFactory.loadingCharacterFame(ret, ps, rs, con);
                
                ret.buddyList.loadFromDb(characterId);
                ret.storage = StorageKeeper.loadStorage(ret.accountid);
                ret.stats.recalcLocalStats();
                ret.stats.silentEnforceMaxHpMp();
            }
            return ret;
        } catch (SQLException ex) {
            System.out.println("[ERROR] Failed to load character!");
            FileLogger.printError(FileLogger.DATABASE_EXCEPTION, ex);
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
                if (pse != null) {
                    pse.close();
                }
                if (rs != null) {
                    rs.close();
                }
            } catch (SQLException ex) {
                System.out.println("[ERROR] Failed to load character!");
                FileLogger.printError(FileLogger.DATABASE_EXCEPTION, ex);
            }
        }
        return null;
    }
 
    public static Player getDefault(Client client, int chrid) {
        Player ret = getDefault(client);
        ret.id = chrid;
        return ret;
    }

    public static Player getDefault(Client client) {
        Player ret = new Player(false);
        
        ret.client = client;
        ret.stats.maxHP = GameConstants.DEFAULT_MAXHP;
        ret.stats.hp = GameConstants.DEFAULT_HP;
        ret.stats.maxMP = GameConstants.DEFAULT_MAXMP;
        ret.stats.mp = GameConstants.DEFAULT_MP;
        
        ret.field = null;
        ret.exp.set(0);
        ret.meso.set(0);
        ret.gm = 0;
        ret.job = GameConstants.DEFAULT_JOB;
        ret.level = GameConstants.DEFAULT_LEVEL;
        ret.accountid = client.getAccountID();
        ret.buddyList = new MapleBuddyList(GameConstants.DEFAULT_BUDDY);
        ret.tamingMob = null;
        
        ret.getInventory(InventoryType.EQUIP).setSlotLimit(GameConstants.DEFAULT_SLOTLIMIT);
        ret.getInventory(InventoryType.USE).setSlotLimit(GameConstants.DEFAULT_SLOTLIMIT);
        ret.getInventory(InventoryType.SETUP).setSlotLimit(GameConstants.DEFAULT_SLOTLIMIT);
        ret.getInventory(InventoryType.ETC).setSlotLimit(GameConstants.DEFAULT_SLOTLIMIT);
        
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT * FROM accounts WHERE id = ?")) {
                ps.setInt(1, ret.accountid);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        ret.client.setAccountName(rs.getString("name"));
                        
                    }
                }
            }
          } catch (SQLException e) {
            System.out.println("[ERROR] Failed getDefault function!");
            FileLogger.printError(FileLogger.DATABASE_EXCEPTION, e);
        }
        for (int i = 0; i < GameConstants.DEFAULT_KEY.length; i++) {
            ret.keymap.put(GameConstants.DEFAULT_KEY[i], new PlayerKeyBinding(GameConstants.DEFAULT_TYPE[i], GameConstants.DEFAULT_ACTION[i]));
        }
        ret.stats.recalcLocalStats();
        return ret;
    }
    
    public void saveNewCharDB(Player p) {
        Connection con = null;
        PreparedStatement ps = null;
        PreparedStatement pse = null;
        ResultSet rse = null;
        try {
            con = DatabaseConnection.getConnection();
            con.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
            con.setAutoCommit(false);
            /*                                                      1          2        3        4        5         6              7        8       9       10         11        12      13           14        15             16         17       18         19       20            21          22           23              24            25             26               27            28              29                 30                31                 32               33               34                   35              36            37             38            39           40            41              42            43        44             45          46              */
            ps = con.prepareStatement("INSERT INTO characters SET level = ?, fame = ?, str = ?, dex = ?, luk = ?, `int` = ?, " + "exp = ?, hp = ?, mp = ?, maxhp = ?, maxmp = ?, sp = ?, ap = ?, " + "gm = ?, skincolor = ?, gender = ?, job = ?, hair = ?, face = ?, map = ?, " + "meso = ?, hpApUsed = ?, mpApUsed = ?, spawnpoint = ?, party = ?, buddyCapacity = ?,  married = ?, partnerid = ?,  marriagequest = ?, alliancerank = ?, ariantPoints = ?, hpMpUsed = ?, matchcardwins = ?, matchcardlosses = ?, matchcardties = ?, omokwins = ?, omoklosses = ?, omokties = ?,  pets = ?, autoHpPot = ?, autoMpPot = ?, playtime = ?, spouseId = ?, accountid = ?, world = ?, name = ?", Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, 1);
            ps.setInt(2, 0);
            final PlayerStatsManager stat = p.stats;
            ps.setInt(3, stat.getStr());
            ps.setInt(4, stat.getDex());
            ps.setInt(5, stat.getLuk());
            ps.setInt(6, stat.getInt());
            ps.setInt(7, 0);
            ps.setInt(8, stat.getHp());
            ps.setInt(9, stat.getMp());
            ps.setInt(10, stat.getCurrentMaxHp());
            ps.setInt(11, stat.getCurrentMaxMp());
            ps.setInt(12, 0);
            ps.setInt(13, 0);
            ps.setInt(14, 0);
            ps.setInt(15, skin.getId());
            ps.setInt(16, gender);
            ps.setInt(17, job.getId());
            ps.setInt(18, hair);
            ps.setInt(19, eyes);
            ps.setInt(20, 0);
            ps.setInt(21, 0);
            ps.setInt(22, 0);
            ps.setInt(23, 0);
            ps.setInt(24, 0);
            ps.setInt(25, -1);
            ps.setInt(26, buddyList.getCapacity());
            ps.setInt(27, 0);
            ps.setInt(28, 0);
            ps.setInt(29, 0);
            ps.setInt(30, 0);
            ps.setInt(31, 0);
            ps.setInt(32, 0);
            ps.setInt(33, 0);
            ps.setInt(34, 0);
            ps.setInt(35, 0);
            ps.setInt(36, 0);
            ps.setInt(37, 0);
            ps.setInt(38, 0);
            ps.setString(39, "-1,-1,-1");
            ps.setInt(40, 0);
            ps.setInt(41, 0);
            ps.setLong(42, 0);
            ps.setInt(43, 0);
            ps.setInt(44, p.getAccountID());
            ps.setInt(45, p.world);
            ps.setString(46, p.name);
            ps.executeUpdate();
            
            rse = ps.getGeneratedKeys();
            if (rse.next()) {
                p.id = rse.getInt(1);
            } else {
                throw new DatabaseException("Inserting char failed.");
            }
            ps.close();
            rse.close();
            
            ps = con.prepareStatement("INSERT INTO queststatus (`queststatusid`, `characterid`, `quest`, `status`, `time`, `forfeited`, `completed`) VALUES (DEFAULT, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            try (PreparedStatement psee = con.prepareStatement("INSERT INTO questprogress VALUES (DEFAULT, ?, ?, ?)")) {
                ps.setInt(1, id);
                for (MapleQuestStatus q : quests.values()) {
                    ps.setInt(2, q.getQuest().getId());
                    ps.setInt(3, q.getStatus().getId());
                    ps.setInt(4, (int) (q.getCompletionTime() / 1000));
                    ps.setInt(5, q.getForfeited());
                    ps.setInt(6, q.getCompleted());
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
            
            ps = con.prepareStatement("INSERT INTO skills (characterid, skillid, skilllevel, masterlevel) VALUES (?, ?, ?, ?)");
            ps.setInt(1, id);
            for (Entry<PlayerSkill, PlayerSkillEntry> skill_ : skills.entrySet()) {
                ps.setInt(2, skill_.getKey().getId());
                ps.setInt(3, skill_.getValue().skillevel);
                ps.setInt(4, skill_.getValue().masterlevel);
                ps.executeUpdate();
            }
            ps.close();
            
            List<Pair<Item, InventoryType>> itemsWithType = new ArrayList<>();
            for (Inventory iv : inventory) {
                for (Item item : iv.list()) {
                    itemsWithType.add(new Pair<>(item, iv.getType()));
                }
            }
	    ItemFactory.INVENTORY.saveItems(itemsWithType, id);   
            
            ps = con.prepareStatement("INSERT INTO keymap (characterid, `key`, `type`, `action`) VALUES (?, ?, ?, ?)");
            ps.setInt(1, id);
            for (Entry<Integer, PlayerKeyBinding> keybinding : keymap.entrySet()) {
                ps.setInt(2, keybinding.getKey().intValue());
                ps.setInt(3, keybinding.getValue().getType());
                ps.setInt(4, keybinding.getValue().getAction());
                ps.executeUpdate();
            }
            ps.close();
            
            ps = con.prepareStatement("INSERT INTO mountdata (characterid, `Level`, `Exp`, `Fatigue`) VALUES (?, ?, ?, ?)");
            ps.setInt(1, id);
            ps.setByte(2, (byte) 1);
            ps.setInt(3, 0);
            ps.setByte(4, (byte) 0);
            ps.execute();
            ps.close();
            
            con.commit();
        } catch (DatabaseException | SQLException e) {
            System.out.println("[ERROR]  Error saving character data saveNewCharDB!");
            FileLogger.printError(FileLogger.DATABASE_EXCEPTION, e);
            try {
                con.rollback();
            } catch (SQLException ex) {
                System.out.println("[ERROR] Error rolling back saveNewCharDB!");
                FileLogger.printError(FileLogger.DATABASE_EXCEPTION, e);
            }
        } finally {
            try {
                if (pse != null) {
                    pse.close();
                }
                if (ps != null) {
                    ps.close();
                }
                if (rse != null) {
                    rse.close();
                }
                con.setAutoCommit(true);
                con.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            } catch (SQLException e) {
                System.out.println("[ERROR] Error going back to autocommit mode in saveNewCharDB!");
                FileLogger.printError(FileLogger.DATABASE_EXCEPTION, e);
            }
        } 
    }
   
    public synchronized void saveDatabase() {
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = DatabaseConnection.getConnection();
            con.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
            con.setAutoCommit(false);

            PlayerSaveFactory.savingCharacterStats(this, ps, con);

            PlayerSaveFactory.savingCharacterInventory(this);

            PlayerSaveFactory.savingCharacterQuests(this, ps, con);

            PlayerSaveFactory.savingCharacterSkills(this, ps, con);

            PlayerSaveFactory.savingCharacterSkillCoolDown(this, ps, con);

            PlayerSaveFactory.savingCharacterKeymap(this, ps, con);

            if (getBuddylist().changed()) {
                PlayerSaveFactory.savingCharacterBuddy(this, ps, con);
                getBuddylist().setChanged(false);
            }

            if (getCashShop() != null) {
                getCashShop().saveToDB();
            }

            if (getStorage() != null) {
                getStorage().saveToDB();
            }

            if (getChangedSkillMacros()) {
                PlayerSaveFactory.savingCharacterSkillMacros(this, ps, con);
            }

            if (getChangedSavedLocations()) {
                PlayerSaveFactory.savingCharacterSavedLocations(this, ps, con);
            }

            if (getChangedTrockLocations()) {
                PlayerSaveFactory.savingCharacterTrockLocations(this, ps, con);
            }

            if (getChangedRegrockLocations()) {
                PlayerSaveFactory.savingCharacterRegRockLocations(this, ps, con);
            }

            con.commit();
        } catch (DatabaseException | SQLException e) {
            System.out.println("[ERROR] Error rolling back saveToDB!");
            FileLogger.printError(FileLogger.DATABASE_EXCEPTION, e);
            try {
                con.rollback();
            } catch (SQLException ex) {
                System.out.println("[ERROR] Error rolling back saveToDB (" + getName() + ")!");
                FileLogger.printError(FileLogger.DATABASE_EXCEPTION, ex);
            }
        }  catch (Exception e) {
            FileLogger.printError(FileLogger.DATABASE_EXCEPTION, e, "Error saving " + name + " Level: " + level + " Job: " + job.getId());
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
                con.setAutoCommit(true);
                con.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            } catch (SQLException e) {
                System.out.println("[ERROR] Error rolling back saveToDB (" + getName() + ")!");
                FileLogger.printError(FileLogger.DATABASE_EXCEPTION, e);
            }
        } 
    }
      
    public void updateQuestInfo(int quest, String info) {
        MapleQuest q = MapleQuest.getInstance(quest);
        MapleQuestStatus qs = getQuest(q);
        qs.setInfo(info);
        
        synchronized (quests) {
            quests.put(q.getId(), qs);
        }
        
        announce(PacketCreator.UpdateQuest(qs, false));
        if (qs.getQuest().getInfoNumber() > 0) {
            announce(PacketCreator.UpdateQuest(qs, true));
        }
        announce(PacketCreator.UpdateQuestInfo((short) qs.getQuest().getId(), qs.getNpc()));
    }

    public void updateQuest(MapleQuestStatus quest) {
        synchronized (quests) {
            quests.put(quest.getQuestID(), quest);
        }
        switch(quest.getStatus()) {
            case STARTED:
                announce(PacketCreator.UpdateQuest(quest, false));
                if (quest.getQuest().getInfoNumber() > 0) {
                    announce(PacketCreator.UpdateQuest(quest, true));
                }
                announce(PacketCreator.UpdateQuestInfo((short) quest.getQuest().getId(), quest.getNpc()));
                break;
            case COMPLETED:
                announce(PacketCreator.CompleteQuest((short) quest.getQuest().getId(), quest.getCompletionTime()));
                if (GameConstants.EARN_QUESTPOINT) {
                    quest.setCompleted(quest.getCompleted() + GameConstants.QUESTPOINT_QTY);
                }
                break;
            case NOT_STARTED:
                announce(PacketCreator.UpdateQuest(quest, false));
                if (quest.getQuest().getInfoNumber() > 0) {
                    announce(PacketCreator.UpdateQuest(quest, true));
                }
                break;
            case UNDEFINED:
                break;
            default:
                break;
        }
    }
    
    public void cancelExpirationTask() {
        if (expireTask != null) {
            expireTask.cancel(false);
            expireTask = null;
        }
    }
    
    public void expirationTask() {
        if (expireTask == null) {
            expireTask = CharacterTimer.getInstance().register(() -> {
                long expiration, currenttime = System.currentTimeMillis();
                synchronized (inventory) {
                    List<Item> toberemove = new ArrayList<>();
                    List<Integer> toadd = new ArrayList<>();
                    for (Inventory inv : inventory) { 
                        for (Item item : inv.list()) {
                            expiration = item.getExpiration();
                            if (expiration > -1 && expiration < currenttime) {
                                boolean sendPetExpiration = false;
                                if (item.getPet() != null) {
                                    sendPetExpiration = true;
                                    if (ItemInformationProvider.getInstance().cannotRevive(item.getItemId())) {
                                        toberemove.add(item);
                                    } else {
                                        item.setExpiration(-1);
                                    }
                                } else {
                                    toberemove.add(item);
                                }
                                Pair<Integer, String> replace = ItemInformationProvider.getInstance().getReplaceOnExpire(item.getItemId());
                                if (replace.left != null && replace.left > 0) {
                                    toadd.add(replace.left);
                                    if (replace.right != null)
                                        dropMessage(replace.right);
                                }
                                if (sendPetExpiration) {
                                    announce(PetPackets.RemovePet(getId(), getPetIndex(item.getPet()), (byte) 2));
                                }
                                announce(PacketCreator.ItemExpired(item.getItemId()));
                            }
                        }
                        for (Item item : toberemove) {
                            InventoryManipulator.removeFromSlot(client, inv.getType(), item.getPosition(), item.getQuantity(), true);
                        }
                        for (Integer itemid : toadd) {
                            InventoryManipulator.addById(client, itemid, (short) 1, "");
                        }
                        toberemove.clear();
                    }
                }
            }, 60000);
        }
    }
    
     public boolean isActiveBuffedValue(int skillid) {
        LinkedList<BuffStatValueHolder> allBuffs;
        
        effLock.lock();
        chrLock.lock();
        try {
            allBuffs = new LinkedList<>(effects.values());
        } finally {
            chrLock.unlock();
            effLock.unlock();
        }
        
        for (BuffStatValueHolder mbsvh : allBuffs) {
            if (mbsvh.effect.isSkill() && mbsvh.effect.getSourceId() == skillid) {
                return true;
            }
        }
        return false;
    }

    public Integer getBuffedValue(BuffStat effect) {
        effLock.lock();
        chrLock.lock();
        try {
            BuffStatValueHolder mbsvh = effects.get(effect);
            if (mbsvh == null) {
                return null;
            }
            return Integer.valueOf(mbsvh.value);
        } finally {
            chrLock.unlock();
            effLock.unlock();
        }
    }

    public boolean isBuffFrom(BuffStat stat, PlayerSkill skill) {
        effLock.lock();
        chrLock.lock();
        try {
            BuffStatValueHolder mbsvh = effects.get(stat);
            if (mbsvh == null) {
                return false;
            }
            return mbsvh.effect.isSkill() && mbsvh.effect.getSourceId() == skill.getId();
        } finally {
            chrLock.unlock();
            effLock.unlock();
        }
    }

    public int getBuffSource(BuffStat stat) {
        effLock.lock();
        chrLock.lock();
        try {
            BuffStatValueHolder mbsvh = effects.get(stat);
            if (mbsvh == null) {
                return -1;
            }
            return mbsvh.effect.getSourceId();
        } finally {
            chrLock.unlock();
            effLock.unlock();
        }
    }
    
    public int getItemQuantity(int itemid) {
        return inventory[ItemInformationProvider.getInstance().getInventoryType(itemid).ordinal()].countById(itemid);
    }

    public int getItemQuantity(int itemid, boolean checkEquipped) {
        int possesed = inventory[ItemInformationProvider.getInstance().getInventoryType(itemid).ordinal()].countById(itemid);
        if (checkEquipped) {
            possesed += inventory[InventoryType.EQUIPPED.ordinal()].countById(itemid);
        }
        return possesed;
    }

    public void setBuffedValue(BuffStat effect, int value) {
        effLock.lock();
        chrLock.lock();
        try {
            BuffStatValueHolder mbsvh = effects.get(effect);
            if (mbsvh == null) {
                return;
            }
            mbsvh.value = value;
        } finally {
            chrLock.unlock();
            effLock.unlock();
        }
    }

    public Long getBuffedStarttime(BuffStat effect) {
        effLock.lock();
        chrLock.lock();
        try {
            BuffStatValueHolder mbsvh = effects.get(effect);
            if (mbsvh == null) {
                return null;
            }
            return Long.valueOf(mbsvh.startTime);
        } finally {
            chrLock.unlock();
            effLock.unlock();
        }
    }

    public MapleStatEffect getStatForBuff(BuffStat effect) {
        BuffStatValueHolder mbsvh = effects.get(effect);
        if (mbsvh == null) {
            return null;
        }
        return mbsvh.effect;
    }
   
    private void prepareDragonBlood(final MapleStatEffect bloodEffect) {
        if (this.dragonBloodSchedule != null) {
            this.dragonBloodSchedule.cancel(false);
        }
        this.dragonBloodSchedule = CharacterTimer.getInstance().register(new DragonBloodRunnable(bloodEffect), 4000, 4000);
    }

    public RockPaperScissors getRPS() {
        return rps;
    }

    public void setRPS(RockPaperScissors rps) {
        this.rps = rps;
    }  

     public void setOwlSearch(int id) {
        owlSearch = id;
    }
    
    public int getOwlSearch() {
        return owlSearch;
    }

    public void addSummon(int id, MapleSummon summon) {
        summons.put(id, summon);
    }

    public int getTargetHpBarHash() {
        return this.targetHpBarHash;
    }
    
    public void setTargetHpBarHash(int mobHash) {
        this.targetHpBarHash = mobHash;
    }
    
    public long getTargetHpBarTime() {
        return this.targetHpBarTime;
    }
    
    public void setTargetHpBarTime(long timeNow) {
        this.targetHpBarTime = timeNow;
    }
    
    public void setPlayerAggro(int mobHash) {
        setTargetHpBarHash(mobHash);
        setTargetHpBarTime(System.currentTimeMillis());
    }
    
    public void resetPlayerAggro() {
        if (getChannelServer().unregisterDisabledServerMessage(id)) {
            client.announceServerMessage();
        }
        setTargetHpBarHash(0);
        setTargetHpBarTime(0);
    }

    public final ChannelServer getChannelServer() {
        return ChannelServer.getInstance(this.getClient().getChannel());
    }
   
    private final class DragonBloodRunnable implements Runnable {

        private final MapleStatEffect effect;

        public DragonBloodRunnable(final MapleStatEffect effect) {
            this.effect = effect;
        }

        @Override
        public void run() {
            if (Player.this.stats.getHp() - this.effect.getX() > 1) {
                Player.this.cancelBuffStats(BuffStat.DRAGONBLOOD);
            } else {
                stats.addHP(-this.effect.getX());
                final int bloodEffectSourceId = this.effect.getSourceId();
                final OutPacket ownEffectPacket = EffectPackets.ShowOwnBuffEffect(bloodEffectSourceId, PlayerEffects.SKILL_SPECIAL.getEffect());
                Player.this.client.write(ownEffectPacket);
                final OutPacket otherEffectPacket = EffectPackets.BuffMapVisualEffect(Player.this.getId(), bloodEffectSourceId, 5);
                Player.this.field.broadcastMessage(Player.this, otherEffectPacket, false);
            }
        }
    }

   public void startMapTimeLimitTask(final Field from, final Field to) {
        if (to.getTimeLimit() > 0 && from != null) {
            final Player p = this;
            mapTimeLimitTask = CharacterTimer.getInstance().register(() -> {
                Portal pfrom = null;
                if (MapConstants.isMiniDungeonMap(getMap().getId())) {
                    pfrom = from.getPortal("MD00");
                } else {
                    pfrom = from.getPortal(0);
                }
                if (pfrom != null) {
                    p.changeMap(from, pfrom);
                }
            }, from.getTimeLimit() * 1000, from.getTimeLimit() * 1000);
        }
    }

    public void cancelMapTimeLimitTask() {
        if (mapTimeLimitTask != null) {
            mapTimeLimitTask.cancel(false);
        }
    }
    
    public void toggleVisibility(boolean login) {
        setVisibility(!isHidden());
    }
    
    public void setVisibility(boolean hide) {
        setVisibility(hide, false);
    }
    
    public void setVisibility(boolean hide, boolean login) {
        if (isGameMaster() && hide != this.hidden) {
            if (!hide) {
                this.hidden = false;
                
                announce(PacketCreator.StopHide());
                
                List<BuffStat> stat = Collections.singletonList(BuffStat.DARKSIGHT);
                field.broadcastGMMessage(this,PacketCreator.CancelForeignBuff(id, stat), false);       
                field.broadcastMessage(this, PacketCreator.SpawnPlayerMapObject(this), false);
                
                for (MapleSummon ms: this.getSummonsValues()) {
                    field.broadcastNONGMMessage(this, PacketCreator.SpawnSpecialFieldObject(ms, false), false);
                }
                
                updatePartyMemberHP();
                
            } else {
                this.hidden = true;
                announce(PacketCreator.ShowHide());
                if (!login) {
                    getMap().broadcastMessage(this, PacketCreator.RemovePlayerFromMap(getId()), false);
                }
                
                field.broadcastGMMessage(this, PacketCreator.SpawnPlayerMapObject(this), false);
                
                List<Pair<BuffStat, Integer>> ldsstat = Collections.singletonList(new Pair<BuffStat, Integer>(BuffStat.DARKSIGHT, 0));
                field.broadcastGMMessage(this, PacketCreator.BuffMapEffect(id, ldsstat, false), false);

                for (MapleMonster mon : this.getControlledMonsters()) {
                    mon.setController(null);
                    mon.setControllerHasAggro(false);
                    mon.setControllerKnowsAboutAggro(false);
                    mon.getMap().updateMonsterController(mon);
               }
            }
            showHint("You are currently " + (hidden ? "with " : "without ") + "hide.");
            announce(PacketCreator.EnableActions());
        }
    }
    
    public void registerEffect(MapleStatEffect effect, long starttime, ScheduledFuture<?> schedule) {
        registerEffect(effect, starttime, schedule, effect.getStatups());
    }

    public void registerEffect(MapleStatEffect effect, long starttime, ScheduledFuture<?> schedule, List<Pair<BuffStat, Integer>> statups) {
	if (effect.isHide()) {
	    field.broadcastMessage(this, PacketCreator.RemovePlayerFromMap(getId()), false);
	} else if (effect.isDragonBlood()) {
	    prepareDragonBlood(effect);
	} else if (effect.isBerserk()) {
	    checkBerserk(isHidden());
	} else if(effect.isMonsterRiding()) {
            getMount().startSchedule();
        } else if (effect.isBeholder()) {
	    prepareBeholderEffect();
	} else if (effect.isRecovery()) {
            int healInterval = (GameConstants.USE_ULTRA_RECOVERY) ? 2000 : 5000;
            final byte heal = (byte) effect.getX();
            
            chrLock.lock();
            try {
                if (recoveryTask != null) {
                    recoveryTask.cancel(false);
                }
                
                recoveryTask = CharacterTimer.getInstance().register(() -> {
                    if (getBuffSource(BuffStat.RECOVERY) == -1) {
                        chrLock.lock();
                        try {
                            if (recoveryTask != null) {
                                recoveryTask.cancel(false);
                                recoveryTask = null;
                            }
                        } finally {
                            chrLock.unlock();
                        }
                        
                        return;
                    }
                    
                    getStat().addHP(heal);
                    client.announce(PacketCreator.ShowOwnRecovery(heal));
                    getMap().broadcastMessage(Player.this, PacketCreator.ShowRecovery(id, heal), false);
                }, healInterval, healInterval);
            } finally {
                chrLock.unlock();
            }
            
        }
        effLock.lock();
        chrLock.lock();
        try {
            for (Pair<BuffStat, Integer> statup : statups) {
                int value = statup.getRight().intValue();
                effects.put(statup.getLeft(), new BuffStatValueHolder(effect, starttime, schedule, value));
            }
        } finally {
            chrLock.unlock();
            effLock.unlock();
        }
	stats.recalcLocalStats();
    }
    
    public int getSlot() {
        return slots;
    }

    public byte getSlots(int type) {
        return type == InventoryType.CASH.getType() ? 96 : inventory[type].getSlotLimit();
    }

    private List<BuffStat> getBuffStats(MapleStatEffect effect, long startTime) {
        final List<BuffStat> bstats = new ArrayList<>();
        final Map<BuffStat, BuffStatValueHolder> allBuffs = new EnumMap<>(effects);
        for (Entry<BuffStat, BuffStatValueHolder> stateffect : allBuffs.entrySet()) {
            final BuffStatValueHolder mbsvh = stateffect.getValue();
            if (mbsvh.effect.sameSource(effect) && (startTime == -1 || startTime == mbsvh.startTime)) {
                bstats.add(stateffect.getKey());
            }
        }
        return bstats;
    }

    private void deregisterBuffStats(List<BuffStat> stats) {
        chrLock.lock();
        try {
            List<BuffStatValueHolder> effectsToCancel = new ArrayList<>(stats.size());
            for (BuffStat stat : stats) {
                final BuffStatValueHolder mbsvh = effects.remove(stat);
                if (mbsvh != null) {
                    boolean addMbsvh = true;
                    for (BuffStatValueHolder contained : effectsToCancel) {
                        if (mbsvh.startTime == contained.startTime && contained.effect == mbsvh.effect) {
                            addMbsvh = false;
                        }
                     }
                    if (addMbsvh) {
                        effectsToCancel.add(mbsvh);
                    }
                    if (stat != null) {
                        switch (stat) {
                            case RECOVERY:
                                if (recoveryTask != null) {
                                    recoveryTask.cancel(false);
                                    recoveryTask = null;
                                }
                                break;
                            case SUMMON:
                            case PUPPET:
                                final int summonId = mbsvh.effect.getSourceId();
                                final MapleSummon summon = summons.get(summonId);
                                if (summon != null) {
                                    getMap().broadcastMessage(PacketCreator.RemoveSpecialMapObject(summon, true), summon.getPosition());
                                    getMap().removeMapObject(summon);
                                    removeVisibleMapObject(summon);
                                    summons.remove(summonId);

                                    if (summon.getSkill() == DarkKnight.Beholder) {
                                        if (beholderHealingSchedule != null) {
                                            beholderHealingSchedule.cancel(false);
                                            beholderHealingSchedule = null;
                                        }
                                        if (beholderBuffSchedule != null) {
                                            beholderBuffSchedule.cancel(false);
                                            beholderBuffSchedule = null;
                                        }
                                    }
                                }
                            case DRAGONBLOOD:
                                if (dragonBloodSchedule != null) {
                                    dragonBloodSchedule.cancel(false);
                                    dragonBloodSchedule = null;
                                }
                                break;
                            default:
                                break;
                        } 
                    } 
                }
            }
            for (BuffStatValueHolder cancelEffectCancelTasks : effectsToCancel) {
                if (getBuffStats(cancelEffectCancelTasks.effect, cancelEffectCancelTasks.startTime).isEmpty()) {
                    if (cancelEffectCancelTasks.schedule != null) {
                        cancelEffectCancelTasks.schedule.cancel(false);
                    }
                }
            }
        } finally {
            chrLock.unlock();
        }
    }
    
    public void cancelEffect(int itemId) {
        cancelEffect(ii.getItemEffect(itemId), false, -1);
    }

    /**
     * @param effect
     * @param overwrite when overwrite is set no data is sent and all the Buffstats in the StatEffect are deregistered
     * @param startTime
     */
    public void cancelEffect(MapleStatEffect effect, boolean overwrite, long startTime) {
        List<BuffStat> buffstats;
        if (!overwrite) {
            buffstats = getBuffStats(effect, startTime);
        } else {
            List<Pair<BuffStat, Integer>> statups = effect.getStatups();
            buffstats = new ArrayList<>(statups.size());
            for (Pair<BuffStat, Integer> statup : statups) {
                buffstats.add(statup.getLeft());
            }
        }
        if (buffstats.size() < 1) {
            return;
        }
        deregisterBuffStats(buffstats);
        if (effect.isMysticDoor()) {
            MapleDoor destroyDoor;
            
            chrLock.lock();
            try {
                destroyDoor = doors.remove(this.getId());
            } finally {
                chrLock.unlock();
            }
            
           if (destroyDoor != null) {
                destroyDoor.getTarget().removeMapObject(destroyDoor.getAreaDoor());
                destroyDoor.getTown().removeMapObject(destroyDoor.getTownDoor());
                
                for (Player chr : destroyDoor.getTarget().getCharacters()) {
                    destroyDoor.getAreaDoor().sendDestroyData(chr.getClient());
                }
                for (Player chr : destroyDoor.getTown().getCharacters()) {
                    destroyDoor.getTownDoor().sendDestroyData(chr.getClient());
                }
                prtLock.lock();
                try {
                    if (party != null) {
                        for (MaplePartyCharacter partyMembers : getParty().getMembers()) {
                            partyMembers.getPlayer().removeDoor(this.getId());
                            partyMembers.removeDoor(this.getId());
                        }
                        silentPartyUpdate();
                    }
                } finally {
                    prtLock.unlock();
                }
            }
        
	} 
        if (effect.isMonsterRiding()) {
            if (effect.getSourceId() != Corsair.Battleship) {
                this.getMount().cancelSchedule();
            }
        }
        if (effect.isEnergy()) {
            if (getEnergy() > 0) {
                this.setEnergyBar(0);
            }
        }
        switch(effect.getSourceId()) {
            case SpearMan.HyperBody:
            case Gm.HyperBody:
            case SuperGm.HyperBody:
                List<Pair<PlayerStat, Integer>> statup = new ArrayList<>(4);
                statup.add(new Pair<>(PlayerStat.HP, Math.min(stats.getHp(), stats.getCurrentMaxHp())));
                statup.add(new Pair<>(PlayerStat.MP, Math.min(stats.getMp(), stats.getCurrentMaxMp())));
                statup.add(new Pair<>(PlayerStat.MAXHP, stats.getMaxHp()));
                statup.add(new Pair<>(PlayerStat.MAXMP, stats.getMaxMp()));
                client.announce(PacketCreator.UpdatePlayerStats(statup));
                break;
            default:
                break;
        }
        if (!overwrite) {
            cancelPlayerBuffs(buffstats);  
        }
    }

    public void cancelBuffStats(BuffStat stat) {
        List<BuffStat> buffStatList = Arrays.asList(stat);
        deregisterBuffStats(buffStatList);
        cancelPlayerBuffs(buffStatList);
    }

    public void cancelEffectFromBuffStat(BuffStat stat) {
        BuffStatValueHolder effect;
        effLock.lock();
        chrLock.lock();
        try {
            effect = effects.get(stat);
        } finally {
            chrLock.unlock();
            effLock.unlock();
        }
        if (effect != null) {
            cancelEffect(effect.effect, false, -1);
        }
    }

     private void cancelPlayerBuffs(List<BuffStat> buffstats) {
        if (client.getChannelServer().getPlayerStorage().getCharacterById(getId()) != null) {
            stats.recalcLocalStats();
            stats.enforceMaxHpMp();
            client.announce(PacketCreator.CancelBuff(buffstats));
            if (buffstats.size() > 0) {
                getMap().broadcastMessage(this, PacketCreator.CancelForeignBuff(getId(), buffstats), false);
            }
        }
    }
 
    public void dispel() {
        List<BuffStatValueHolder> allBuffs;
        
        chrLock.lock();
        try {
            allBuffs = new ArrayList<>(effects.values());
        } finally {
            chrLock.unlock();
        }
        if (!isHidden()) {
            for (BuffStatValueHolder mbsvh : allBuffs) {
                if (mbsvh.effect.isSkill() && mbsvh.schedule != null && !mbsvh.effect.isMorph()) {
                    cancelEffect(mbsvh.effect, false, mbsvh.startTime);
                }
            }
        }
    }

   public void cancelAllBuffs(boolean disconnect) {
        if (disconnect) {
            effLock.lock();
            chrLock.lock();
            try {
                effects.clear();
            } finally {
                effLock.lock();
                chrLock.lock();
            }
        } else {
            final List<BuffStatValueHolder> allBuffs;
            
            effLock.lock();
            chrLock.lock();
            try {
                allBuffs = new ArrayList<>(effects.values());
            } finally {
                chrLock.unlock();
                effLock.unlock();
            }
            for (BuffStatValueHolder mbsvh : allBuffs) {
                cancelEffect(mbsvh.effect, false, mbsvh.startTime);
            }
        }
    }

    public void cancelMorphs() {
        LinkedList<BuffStatValueHolder> allBuffs = new LinkedList<>(effects.values());
        for (BuffStatValueHolder mbsvh : allBuffs) {
            switch (mbsvh.effect.getSourceId()) {
                case Marauder.Transformation:
                case Buccaneer.SuperTransformation:
                    return;
                default:
                    if (mbsvh.effect.isMorph()) {
                        cancelEffect(mbsvh.effect, false, mbsvh.startTime);
                        return;
                    }
            }
        }
    }

    public void silentGiveBuffs(List<PlayerBuffValueHolder> buffs) {
        if (buffs == null) {
            return;
        }
        for (PlayerBuffValueHolder mbsvh : buffs) {
            mbsvh.effect.silentApplyBuff(this, mbsvh.startTime);
        }
    }

    public List<PlayerBuffValueHolder> getAllBuffs() {
        effLock.lock();
        chrLock.lock();
        try {
         List<PlayerBuffValueHolder> ret = new ArrayList<>();
            for (BuffStatValueHolder mbsvh : effects.values()) {
                ret.add(new PlayerBuffValueHolder(mbsvh.startTime, mbsvh.effect));
            }
            return ret;
        } finally {
            chrLock.unlock();
            effLock.unlock();
        }   
    }

    public void cancelMagicDoor() {
        final List<BuffStatValueHolder> mbsvhList;
                
        chrLock.lock();
        try {
            mbsvhList = new ArrayList<>(effects.values());
        } finally {
            chrLock.unlock();
        }
        
        for (BuffStatValueHolder mbsvh : mbsvhList) {
            if (mbsvh.effect.isMysticDoor()) {
                cancelEffect(mbsvh.effect, false, mbsvh.startTime);
            }
        }
    }
    
    public void handleOrbgain() {
        int orbCount = getBuffedValue(BuffStat.COMBO);
        PlayerSkill combo = PlayerSkillFactory.getSkill(Crusader.ComboAttack);
        PlayerSkill advCombo = PlayerSkillFactory.getSkill(Hero.AdvancedComboAttack);

        MapleStatEffect cEffect = null;
        int advComboSkillLevel = getSkillLevel(advCombo);
        if (advComboSkillLevel > 0) {
            cEffect = advCombo.getEffect(advComboSkillLevel);
        } else {
            cEffect = combo.getEffect(getSkillLevel(combo));
        }

        if (orbCount < cEffect.getX() + 1) {
            int newOrbCount = orbCount + 1;
            if (advComboSkillLevel > 0 && cEffect.makeChanceResult()) {
                if (newOrbCount < cEffect.getX() + 1) {
                    newOrbCount++;
                }
            }

            List<Pair<BuffStat, Integer>> stat = Collections.singletonList(new Pair<>(BuffStat.COMBO, newOrbCount));
            setBuffedValue(BuffStat.COMBO, newOrbCount);
            int duration = cEffect.getDuration();
            duration += (int) ((getBuffedStarttime(BuffStat.COMBO) - System.currentTimeMillis()));

            getClient().getSession().write(PacketCreator.GiveBuff(1111002, duration, stat));
            getMap().broadcastMessage(this, PacketCreator.BuffMapEffect(getId(), stat, false), false);
        }
    }

    public void handleOrbconsume() {
        PlayerSkill combo = PlayerSkillFactory.getSkill(Crusader.ComboAttack);
        MapleStatEffect cEffect = combo.getEffect(getSkillLevel(combo));
        List<Pair<BuffStat, Integer>> stat = Collections.singletonList(new Pair<>(BuffStat.COMBO, 1));
        setBuffedValue(BuffStat.COMBO, 1);
        int duration = cEffect.getDuration();
        duration += (int) ((getBuffedStarttime(BuffStat.COMBO) - System.currentTimeMillis()));

        getClient().getSession().write(PacketCreator.GiveBuff(Crusader.ComboAttack, duration, stat, false, false, getMount()));
        getMap().broadcastMessage(this, PacketCreator.BuffMapEffect(getId(), stat, false), false);
    }

    public boolean isLeader() {
        return (getParty().getLeader().equals(new MaplePartyCharacter(client.getPlayer())));
    }
    
    /**
     * only for tests
     *
     * @param newmap
     */
    public void setMap(Field newmap) {
        this.field = newmap;
    }

    public int getMapId() {
        if (field != null) {
            return field.getId();
        }
        return mapId;
    }

    public int getInitialSpawnpoint() {
        return savedSpawnPoint;
    }
    
    public Field getMap() {
        return field;
    }

    public int getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }

    public int getLevel() {
        return level;
    }

    public int getWorldRank() {
        return worldRanking;
    }

    public int getWorldRankChange() {
        return worldRankingChange;
    }

    public int getJobRank() {
        return jobRanking;
    }

    public int getJobRankChange() {
        return jobRankingChange;
    }
    
    public String getAccountName() {
	return client.getAccountName();
    }

    public int getFame() {
        return pop;
    }

    public Client getClient() {
        return client;
    }
    
    public void setClient(Client c) {
	this.client = c;
    }
    
    public int getCurrentExp() {
        return exp.get();
    }

    public boolean isHidden() {
        return hidden;
    }

    public PlayerSkin getSkinColor() {
        return skin;
    }

    public PlayerJob getJob() {
        return job;
    }

    public int getGender() {
        return gender;
    }

    public int getHair() {
        return hair;
    }

    public int getFace() {
        return eyes;
    }
    
    public boolean getChangedTrockLocations() {
        return this.changedTrockLocations;
    }
    
    public boolean getChangedRegrockLocations() {
        return this.changedRegrockLocations;
    }
    
    public boolean getChangedSavedLocations() {
        return this.changedSavedLocations;
    }
    
    public boolean getChangedSkillMacros() {
        return this.changedSkillMacros;
    }
    
    public boolean getChangedReports() {
        return this.changedReports;
    }
    
    public void setChangedTrockLocations(boolean set) {
        this.changedTrockLocations = set;
    }
    
    public void setChangedRegrockLocations(boolean set) {
        this.changedRegrockLocations = set;
    }
    
    public void setChangedSavedLocations(boolean set) {
        this.changedSavedLocations = set;
    }
    
    public void setChangedSkillMacros(boolean set) {
        this.changedSkillMacros = set;
    }
    
    public void setChangedReports(boolean set) {
        this.changedReports = set;
    }
    
    public final boolean hasEquipped(int itemid) {
        return inventory[InventoryType.EQUIPPED.ordinal()].countById(itemid) >= 1;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public void setName(String name, boolean changeName) {
        if (!changeName) {
            this.name = name;
        } else {
            Connection con = DatabaseConnection.getConnection();
            try {
                con.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
                con.setAutoCommit(false);
                try (PreparedStatement sn = con.prepareStatement("UPDATE characters SET name = ? WHERE id = ?")) {
                    sn.setString(1, name);
                    sn.setInt(2, id);
                    sn.execute();
                    con.commit();
                }
                this.name = name;
            } catch (SQLException e) {
                System.err.println(e);
            }
        }
    }
   
    public void setExp(int exp) {
        this.exp.set(exp);
    }

    public void setHair(int hair) {
        this.hair = hair;
    }

    public void setFace(int face) {
        this.eyes = face;
    }

    public void setFame(int fame) {
        this.pop = fame;
    }

    public void setSkinColor(PlayerSkin skinColor) {
        this.skin = skinColor;
    }

    public void setGender(int gender) {
        this.gender = gender;
    }

    public void setGM(int gmlevel) {
        this.gm = gmlevel;
    }

    public CheatTracker getCheatTracker() {
        return antiCheat;
    }
   
    public MapleBuddyList getBuddylist() {
        return buddyList;
    }
    
    public void addPirateSummon(MapleSummon summon) {
        summon.lockSummon();
        try {
            pirateSummons.add(summon);
        } finally {
            summon.unlockSummon();
        }
    }

    public void removePirateSummon(MapleSummon summon) {
        summon.lockSummon();
        try {
            if (!pirateSummons.contains(summon)) {
                return;
            }
            pirateSummons.remove(pirateSummons.indexOf(summon));
        } finally {
            summon.unlockSummon();
        }
    }
    
   public void setChalkboard(String text) {
        this.chalkBoardText = text;
        if (chalkBoardText == null) {
            getMap().broadcastMessage(PacketCreator.UseChalkBoard(this, true));
        } else {
            getMap().broadcastMessage(PacketCreator.UseChalkBoard(this, false));
        }
    }

    public String getChalkboard() {
        return chalkBoardText;
    }

    public List<MapleSummon> getPirateSummons() {
        return pirateSummons;
    }

    public boolean hasPirateSummon(MapleSummon summon) {
        return pirateSummons.contains(summon);
    }

    public void addFame(int famechange) {
        this.pop += famechange;
    }

    public void removeItem(int id, int quantity) {
        InventoryManipulator.removeById(client, ItemConstants.getInventoryType(id), id, -quantity, true, false);
        client.getSession().write(PacketCreator.GetShowItemGain(id, (short) quantity, true));
    }

    public void removeAll(int id) {
        removeAll(id, true);
    }
   
    public void removeAll(int id, boolean show) {
        InventoryType type = ItemConstants.getInventoryType(id);
        int possessed = getInventory(type).countById(id);

        if (possessed > 0) {
            InventoryManipulator.removeById(getClient(), type, id, possessed, true, false);
            if (show) {
                getClient().getSession().write(PacketCreator.GetShowItemGain(id, (short) -possessed, true));
            }
        }
    }
    
    public void changeMap(int map) {
        Field warpMap;
        EventInstanceManager eim = getEventInstance();
        
        if (eim != null) {
            warpMap = eim.getMapInstance(map);
        } else {
            warpMap = client.getChannelServer().getMapFactory().getMap(map);
        }
        
        changeMap(warpMap, warpMap.getRandomPlayerSpawnpoint());
    }
    
    public void changeMap(int map, int portal) {
        Field warpMap;
        EventInstanceManager eim = getEventInstance();
        
        if (eim != null) {
            warpMap = eim.getMapInstance(map);
        } else {
            warpMap = client.getChannelServer().getMapFactory().getMap(map);
        }
        changeMap(warpMap, warpMap.getPortal(portal));
    }
    
    public void changeMap(int map, String portal) {
        Field warpMap;
        EventInstanceManager eim = getEventInstance();
        
        if (eim != null) {
            warpMap = eim.getMapInstance(map);
        } else {
            warpMap = client.getChannelServer().getMapFactory().getMap(map);
        }

        changeMap(warpMap, warpMap.getPortal(portal));
    }
    
    public void changeMap(int map, Portal portal) {
        Field warpMap;
        EventInstanceManager eim = getEventInstance();
        
        if (eim != null) {
            warpMap = eim.getMapInstance(map);
        } else {
            warpMap = client.getChannelServer().getMapFactory().getMap(map);
        }

        changeMap(warpMap, portal);
    }

    
    public void changeMap(Field to) {
        changeMap(to, to.getPortal(0));
    }
       
    public void changeMap(final Field target, final Portal pto) {
        canWarpCounter++;
        
        eventChangedMap(target.getId()); 
        Field to = getWarpMap(target.getId());
        
        changeMapInternal(to, pto.getPosition(), PacketCreator.GetWarpToMap(to, pto.getId(), this));
        canWarpMap = false;
        
        canWarpCounter--;
        if (canWarpCounter == 0) canWarpMap = true;
        
        eventAfterChangedMap(this.getMapId());
    }
    
    public void changeMap(final Field target, final Point pos) {
        canWarpCounter++;
        
        eventChangedMap(target.getId());
        Field to = getWarpMap(target.getId());
        changeMapInternal(to, pos, PacketCreator.GetWarpToMap(to, 0x80, this));
        canWarpMap = false;
        
        canWarpCounter--;
        if(canWarpCounter == 0) canWarpMap = true;
        
        eventAfterChangedMap(this.getMapId());
    }
   

    public void changeMapBanish(int mapid, String portal, String msg) {
        dropMessage(5, msg);
        Field map_ = client.getChannelServer().getMapFactory().getMap(mapid);
        changeMap(map_, map_.getPortal(portal));
    }

    private void changeMapInternal(final Field to, final Point pos, OutPacket warpPacket) {
        if (!canWarpMap || to == null) return;

        this.closePlayerInteractions();

        client.getSession().write(warpPacket);

        if (field.getCharacters().contains(Player.this)) {
            field.removePlayer(Player.this);
        }

        if (client.getChannelServer().getPlayerStorage().getCharacterById(getId()) != null) {
            field = to;
            setPosition(pos);
            to.addPlayer(Player.this);

            stats.recalcLocalStats();

            prtLock.lock();
            try {
                if (party != null) {
                    mpc.setMapId(to.getId());
                    silentPartyUpdate();
                    client.announce(PartyPackets.UpdateParty(client.getChannel(), party, MaplePartyOperation.SILENT_UPDATE, null));
                    updatePartyMemberHP();
                }
            } finally {
                prtLock.unlock();
            }
            for (final ItemPet pet : getPets()) {
                if (pet.getSummoned()) {
                    updatePetAuto();
                }
            }
        } else {
            FileLogger.printError(FileLogger.FIELD, "Character " + this.getName() + " got stuck when moving to map " + field.getId() + ".");
        }
        if (newWarpMap != -1) {
            canWarpMap = true;

            int temp = newWarpMap;
            newWarpMap = -1;
            changeMap(temp);
        } else {
            EventInstanceManager eim = getEventInstance();
            if(eim != null) {
                eim.recoverOpenedGate(this, field.getId());
            }
        }
        client.getSession().write(PacketCreator.EnableActions());
    }
    
    public void closePlayerInteractions() {
        closeNpcShop();
        closeTrade();
        closePlayerShop();
        closeMiniGame();
        closeHiredMerchant(false);
        closeMessenger();
    }
    
    public void closeNpcShop() {
        setShop(null);
    }
    
    public void closeTrade() {
        if (getTrade() != null) Trade.cancelTrade(getTrade(), this);
    }
    
    public void closeMessenger() {
        if (getMessenger() != null) {
            MapleMessengerCharacter messengerplayer = new MapleMessengerCharacter(this);
            MessengerService.leaveMessenger(getMessenger().getId(), messengerplayer);
        }
    }
  
    public void setMapId(int mapid) {
        this.mapId = mapid;
    }

    public final Map<Integer, PlayerKeyBinding> getKeyLayout() {
	return this.keymap;
    }
   
    public int getRingRequested() {
        return this.ringRequest;
    }

    public void setRingRequested(int set) {
        ringRequest = set;
    }
    
    public void leaveMap() {
        visibleMapObjectsLock.writeLock().lock();
        try {
            controlled.clear();
            visibleMapObjects.clear();
        } finally {
            visibleMapObjectsLock.writeLock().unlock();
        }
        if (chair != 0) {
            chair = 0;
        }
    }
    
    public void changeJob(PlayerJob newJob) {
        if (newJob == null) {
            return;
        }
        try {
            this.job = newJob;
            stats.remainingSp += 1;
            if (newJob.getId() % 10 == 2) {
                stats.remainingSp += 2;
            }
            
            stats.updateSingleStat(PlayerStat.AVAILABLESP, stats.getRemainingSp());
            stats.updateSingleStat(PlayerStat.JOB, newJob.getId());
            
            int maxHP = stats.getMaxHp();
            int maxMP = stats.getMaxMp();
            
            if (job.getId() == 100) {
                maxHP += Randomizer.rand(200, 250);
            } else if (job.getId() == 200) {
                maxMP += Randomizer.rand(100, 150);
            } else if (job.getId() % 100 == 0) {
                maxHP += Randomizer.rand(100, 150);
                maxHP += Randomizer.rand(25, 50);
            } else if (job.getId() > 0 && job.getId() < 200) {
                maxHP += Randomizer.rand(300, 350);
            } else if (job.getId() < 300) {
                maxMP += Randomizer.rand(450, 500);
            } else if (job.getId() > 0) {
                maxHP += Randomizer.rand(300, 350);
                maxMP += Randomizer.rand(150, 200);
            }
            if (maxHP >= 30000) {
                maxHP = 30000;
            }
            if (maxMP >= 30000) {
                maxMP = 30000;
            }
            
            stats.hp = maxHP;
            stats.mp = maxMP;
            
            List<Pair<PlayerStat, Integer>> statup = new ArrayList<>(7);
            statup.add(new Pair<>(PlayerStat.MAXHP, Integer.valueOf(maxHP)));
            statup.add(new Pair<>(PlayerStat.MAXMP, Integer.valueOf(maxMP)));
            statup.add(new Pair<>(PlayerStat.HP, Integer.valueOf(maxHP)));
            statup.add(new Pair<>(PlayerStat.MP, Integer.valueOf(maxMP)));
            statup.add(new Pair<>(PlayerStat.AVAILABLEAP, stats.getRemainingAp()));
            statup.add(new Pair<>(PlayerStat.AVAILABLESP, stats.getRemainingSp()));
            statup.add(new Pair<>(PlayerStat.JOB, Integer.valueOf(job.getId())));
            
            stats.recalcLocalStats();
            
            getClient().getSession().write(PacketCreator.UpdatePlayerStats(statup));
            getMap().broadcastMessage(this, PacketCreator.ShowThirdPersonEffect(getId(), PlayerEffects.JOB_ADVANCEMENT.getEffect()), false);

            if (!isGameMaster() && GameConstants.SHOW_JOB_UPDATE) {
                broadcastChangeJob(SkillConstants.getJobNameById(getJob().getId()), PlayerJob.getAdvancement(getJob().getId()));
            }
            if (getParty() != null) {
                silentPartyUpdate();
            }
            if (getGuild() != null) {
                guildUpdate();
            }
        } catch (Exception ex) {
            FileLogger.printError(FileLogger.PLAYER_STUCK, ex); 
        }
    }

    public void changeSkillLevel(PlayerSkill skill, int newLevel, int newMasterlevel) {
        skills.put(skill, new PlayerSkillEntry(newLevel, newMasterlevel));
        this.getClient().getSession().write(PacketCreator.UpdateSkill(skill.getId(), newLevel, newMasterlevel));
    }
    
    public void cancelAllBuffs(){
        for(BuffStatValueHolder mbsvh : new ArrayList<>(effects.values())){
            cancelEffect(mbsvh.effect, false, mbsvh.startTime);
        }
    }

    public void playerDead() {
        if (getEventInstance() != null) {
            getEventInstance().playerKilled(this);
        }
        
        dispelSkill();
        cancelMorphs(); 
        cancelAllBuffs(false);
        dispelDebuffs();
        
        if (getBuffedValue(BuffStat.MORPH) != null){
           cancelEffectFromBuffStat(BuffStat.MORPH);
        }
        
        if (getBuffedValue(BuffStat.MONSTER_RIDING) != null) {
            cancelEffectFromBuffStat(BuffStat.MONSTER_RIDING);
        }
        
        int possesed = 0;
        int i;
        for (i = 0; i < ItemConstants.CHARM_ITEM.length; i++) {
            int quantity = getItemQuantity(ItemConstants.CHARM_ITEM[i], false);
            if (possesed == 0 && quantity > 0) {
                possesed = quantity;
                break;
            }
        }
        if (possesed > 0) {
            possesed -= 1;
            getClient().getSession().write(EffectPackets.SelfCharmEffect((short) Math.min(0xFF, possesed), (short) 90));
            InventoryManipulator.removeById(getClient(), ItemInformationProvider.getInstance().getInventoryType(ItemConstants.CHARM_ITEM[i]), ItemConstants.CHARM_ITEM[i], 1, true, false);
        } else {
            if (this.getJob() != PlayerJob.BEGINNER) {
                int diePercentage = ExperienceConstants.getExpNeededForLevel(this.getLevel() + 1);
                if (this.getMap().isTown() || FieldLimit.REGULAREXPLOSS.check(field.getFieldLimit())) {
                    diePercentage *= 0.01;
                } else if (MonsterCarnival.isBattlefieldMap(this.getMapId())) {
                    diePercentage = 0;
                } 
                if (diePercentage == ExperienceConstants.getExpNeededForLevel(this.getLevel() + 1)) {
                    if (stats.getLuk() <= 100 && stats.getLuk() > 8) {
                        diePercentage *= 0.10 - (stats.getLuk() * 0.0005);
                    } else if (stats.getLuk() < 8) {
                        diePercentage *= 0.10;
                    } else {
                        diePercentage *= 0.10 - (100 * 0.0005);
                    }
                }
                if ((this.getCurrentExp() - diePercentage) > 0) {
                    this.gainExperience(-diePercentage, false, false);
                } else {
                    this.gainExperience(-this.getCurrentExp(), false, false);
                }
            }
        }
        getClient().getSession().write(PacketCreator.EnableActions());
    }

    public void updatePartyMemberHP() {
        prtLock.lock();
        try {
            if (party != null) {
                int channel = client.getChannel();
                for (MaplePartyCharacter partychar : party.getMembers()) {
                    if (partychar.getMapId() == getMapId() && partychar.getChannel() == channel) {
                        Player other = ChannelServer.getInstance(channel).getPlayerStorage().getCharacterByName(partychar.getName());
                        if (other != null) {
                            other.getClient().getSession().write(PartyPackets.UpdatePartyMemberHP(getId(), stats.getHp(), stats.getCurrentMaxHp()));
                        }
                    }
                }
            }
        } finally {
            prtLock.unlock();
        }
    }
     
    public void receivePartyMemberHP() {
        if (party != null) {
            int channel = client.getChannel();
            for (MaplePartyCharacter partychar : party.getMembers()) {
                if (partychar.getMapId() == getMapId() && partychar.getChannel() == channel) {
                    Player other = ChannelServer.getInstance(channel).getPlayerStorage().getCharacterByName(partychar.getName());
                    if (other != null) {
                        client.announce(PartyPackets.UpdatePartyMemberHP(other.getId(), other.getStat().getHp(), other.getStat().getCurrentMaxHp()));
                    }
                }
            }
        }
    }
    
    public final PlayerStatsManager getStat() {
        return stats;
    }
    
    public int getMp() {
        return getStat().getMp();
    }
    
    public int getHp() {
        return getStat().getHp();
    }
    
    public int getStr() {
        return getStat().getStr();
    }
    
    public int getInt() {
        return getStat().getInt();
    }
    
    public int getDex() {
        return getStat().getDex();
    }
    
    public int getRemainingAp() {
        return getStat().getRemainingAp();
    }
    
    public int getRemainingSp() {
        return getStat().getRemainingSp();
    }
    
    public int getLuk() {
        return getStat().getLuk();
    }
    
    public int getMaxMp() {
        return getStat().getMaxMp();
    }
    
    public int getMaxHp() {
        return getStat().getMaxHp();
    }
    
    public boolean hasMerchant() {
        return hasMerchant;
    }
    
    public boolean haveItem(int itemid) {
        return haveItem(itemid, 1, false, true);
    }
    
     public boolean haveItemEquiped(int itemid) {
        return haveItem(itemid, 1, true, false);
    }
            
    public int hasEXPCard() {
        Inventory iv = getInventory(InventoryType.CASH);
        ItemInformationProvider ii = ItemInformationProvider.getInstance();
        for (Integer id : ItemConstants.EXP_CARDS) {
            if (iv.countById(id) > 0) {
                if (ii.isExpOrDropCardTime(id)) {
                    return 2;
                }
            }
        }
        return 1;
    }
    
    public int hasDropCard() {
        Inventory iv = getInventory(InventoryType.CASH);
        ItemInformationProvider ii = ItemInformationProvider.getInstance();
        for (Integer id : ItemConstants.DROP_CARDS) {
            if (iv.countById(id) > 0) {
                if (ii.isExpOrDropCardTime(id)) {
                    return 2;
                }
            }
        }
        return 1;
    }
    
    public void incrementMonsterKills() {
        if (GameConstants.THIRD_KILL_EVENT) {
            mobKills.incrementAndGet();
        }
    }

    public int getThirdKillPercentage(int hours) {
        int mod = 0;
        if (GameConstants.THIRD_KILL_EVENT) {
            switch (hours) {
                case 0:
                    mod = 0;
                    break;
                case 1:
                    mod = 30;
                    break;
                case 2:
                    mod = 100;
                    break;
                case 3:
                    mod = 150;
                    break;
                case 4:
                    mod = 180;
                    break;
                default:
                    mod = 200;
                    break;
            }
        }
        return mod;
    }
    
    public int getMonsterkills () {
        return mobKills.get();
    }
    
    public void resetMonsterkills () {
        mobKills.set(0);
    }
    
    public void gainExpRiche() {
       int gainFirst = ExperienceConstants.getExpNeededForLevel(level);
       double realgain = gainFirst * 0.05;
       this.gainExp((int) realgain, 0, true, false, true);
    } 
    
    public void gainExperience(int gain, boolean show, boolean inChat) {
        gainExp(gain, show, inChat);
    }
    
    public void gainExp(int gain) {
        gainExp(gain, true, true);
    }

    public void gainExp(int gain, boolean show, boolean inChat) {
        gainExp(gain, show, inChat, true);
    }
    
    public void gainExp(int gain, boolean show, boolean inChat, boolean white) {
        gainExp(gain, 0, show, inChat, white);
    }

    public void gainExp(int gain, int party, boolean show, boolean inChat, boolean white) {
        if (hasDisease(Disease.CURSE)) {
            gain *= 0.5;
            party *= 0.5;
        }
	
        if (gain < 0) {
            gain = Integer.MAX_VALUE;
        }   
        if (party < 0) {
            party = Integer.MAX_VALUE;
        }   
        
        incrementMonsterKills();
        
        /*
        * After 1 hour of login until 2 hours:	Bonus 30% EXP at every 3rd mob hunted
        * 2 hours to 3 hours: Bonus 100% EXP at every 3rd mob hunted
        * 3 hours to 4 hours: Bonus 150% EXP at every 3rd mob hunted
        * 4 hours to 5 hours: Bonus 180% EXP at every 3rd mob hunted
        * 5 hours and above: Bonus 200% EXP at every 3rd mob hunted
        */

//        int hours = 1;
//        int thirdKillBonus = 0;
//        int thirdKillBonusPercentage = 0;
//        
//        if (getMonsterkills() > 2 && hours > 0 && GameConstants.THIRD_KILL_EVENT) {
//            thirdKillBonus = (int) (getThirdKillPercentage(hours) / 100f * gain);
//            thirdKillBonusPercentage = getThirdKillPercentage(hours);
//            resetMonsterkills();
//        }
//        
//        System.out.println("thirdKillBonus: " + thirdKillBonus);
        
        //int total = gain + party + thirdKillBonus;
        int total = gain + party;
        
        gainExpInternal(total, party, /*thirdKillBonusPercentage, hours,*/ show, inChat, white);
    }
    
    public void gainExpInternal(int gain, int party, /*int thirdKillBonusPercentage, int hours, */ boolean show, boolean inChat, boolean white) {
        if (level < 200) {
            if ((long) this.exp.get() + (long) gain > (long) Integer.MAX_VALUE) {
                int gainFirst = ExperienceConstants.getExpNeededForLevel(level) - this.exp.get();
                gain -= gainFirst + 1;
                this.gainExp(gainFirst + 1, false, inChat, white);
            }
            stats.updateSingleStat(PlayerStat.EXP, this.exp.addAndGet(gain));
            if (show && gain != 0) {
                client.announce(PacketCreator.GetShowExpGain(gain, party != 0 ? (party - 100) : 0, /*thirdKillBonusPercentage, hours,*/ inChat, white));
            }
            if (gm > 0) {
                while (exp.get() >= ExperienceConstants.getExpNeededForLevel(level)) {
                    levelUp(true);
                }
            } else if (exp.get() >= ExperienceConstants.getExpNeededForLevel(level)) {
                levelUp(true);
                int need = ExperienceConstants.getExpNeededForLevel(level);
                if (exp.get() >= need) {
                    setExp(need - 1);
                    stats.updateSingleStat(PlayerStat.EXP, need);
                }
            }
        }
    }
    
    public void energyChargeGain() {

        PlayerSkill energyCharge = PlayerSkillFactory.getSkill(Marauder.EnergyCharge);
        MapleStatEffect effect = energyCharge.getEffect(getSkillLevel(energyCharge));
        List<Pair<BuffStat, Integer>> stat;
        
        if (!isActiveBuffedValue(energyCharge.getId()) &&  getSkillLevel(energyCharge) > 0) {
            boolean showEffect = false;
            if (getEnergy() < 10000) {
                
                gainEnergy(102); // Todo: Corrigir formula de ganho de energia
                
                if (this.getEnergy() > 9999)  showEffect = true;
                
                
                stat = Collections.singletonList(new Pair<>(BuffStat.ENERGY_CHARGE, getEnergy()));
                setBuffedValue(BuffStat.ENERGY_CHARGE, getEnergy());

                client.getSession().write(PacketCreator.UsePirateSkill(stat, 0, 0, (short) 0)); 
                field.broadcastMessage(this, PacketCreator.BuffMapPirateEffect(this, stat, 0, 0));

                client.getSession().write(EffectPackets.ShowOwnBuffEffect(energyCharge.getId(), PlayerEffects.SKILL_USE.getEffect()));    
                field.broadcastMessage(this, EffectPackets.BuffMapVisualEffect(getId(), energyCharge.getId(), PlayerEffects.SKILL_USE.getEffect()));

            } 
            
            if (showEffect) {
                setEnergyBar(0);
                setBuffedValue(BuffStat.ENERGY_CHARGE, 10000);
                stat = Collections.singletonList(new Pair<>(BuffStat.ENERGY_CHARGE, 10000));
                effect.applyEnergyBuff(this, energyCharge.getId(), stat);
            }
        }
    }
    
    public int getEnergy() {
        return energyBar;
    }
    
    public void gainEnergy(int gain) {
        energyBar += gain;
    }
    
    public void setEnergyBar(int set) {
        energyBar = set;
    }

    public int getWorld() {
        return world;
    }

    public void setWorld(int world) {
        this.world = world;
    }
    
    public void silentPartyUpdate() {
        prtLock.lock();
        try {
            mpc = new MaplePartyCharacter(this);
            if (party != null) {
                PartyService.updateParty(party.getId(), MaplePartyOperation.SILENT_UPDATE, getMPC());
            }
        } finally {
            prtLock.unlock();
        }
    }
    
    public MaplePartyCharacter getMPC() {
        if (mpc == null) {
            mpc = new MaplePartyCharacter(this);
        }
        return mpc;
    }

    public boolean isGameMaster() {
        return gm > 1;
    }

    public int getAdministrativeLevel() {
        return gm;
    }

    public boolean hasGmLevel(int level) {
        return gm >= level;
    }

    public Inventory getInventory(InventoryType type) {
        return inventory[type.ordinal()];
    }

    public Shop getShop() {
        return shop;
    }

    public void setShop(Shop shop) {
        this.shop = shop;
    }

    public int getMeso() {
        return meso.get();
    }

    public int getSavedLocation(SavedLocationType type) {
        return savedLocations[type.ordinal()];
    }
    
    public int getSavedLocation(String type) {
        return savedLocations[SavedLocationType.fromString(type).ordinal()];
    }

    public void saveLocation(SavedLocationType type) {
        savedLocations[type.ordinal()] = getMapId();
        changedSavedLocations = true;
    }
    
    public void saveLocation(String type) {
        savedLocations[SavedLocationType.fromString(type).ordinal()] = getMapId();
        changedSavedLocations = true;
    }

    public void clearSavedLocation(SavedLocationType type) {
        savedLocations[type.ordinal()] = -1;
        changedSavedLocations = true;
    }
    
    public void clearSavedLocation(String type) {
        savedLocations[SavedLocationType.fromString(type).ordinal()] = -1;
        changedSavedLocations = true;
    }

    public void gainMeso(int gain, boolean show) {
        gainMeso(gain, show, false, false);
    }

    public void gainMeso(int gain, boolean show, boolean enableActions) {
        gainMeso(gain, show, enableActions, false);
    }

    public void gainMeso(int gain, boolean show, boolean enableActions, boolean inChat) {
        if (meso.get() + gain <= 0) {
            client.write(PacketCreator.EnableActions());
            return;
        }
        int newVal = meso.addAndGet(gain);
        stats.updateSingleStat(PlayerStat.MESO, newVal, enableActions);
        if (show) {
            client.getSession().write(PacketCreator.GetShowMesoGain(gain, inChat));
        }
    }

    /**
     * Adds this monster to the controlled list. The monster must exist on the
     * Map.
     *
     * @param monster
     * @param aggro
     */
    public void controlMonster(MapleMonster monster, boolean aggro) {
        monster.setController(this);
        controlled.add(monster);
        client.getSession().write(MonsterPackets.ControlMonster(monster, false, aggro));
    }

    public void uncontrolMonster(MapleMonster monster) {
        controlled.remove(monster);
    }

    public void checkMonsterAggro(MapleMonster monster) {
        if (!monster.controllerHasAggro()) {
            if (monster.getController() == this) {
                monster.setControllerHasAggro(true);
            } else {
                monster.switchController(this, true);
            }
        }
    }

    public Collection<MapleMonster> getControlledMonsters() {
        return Collections.unmodifiableCollection(controlled);
    }

    public int getNumControlledMonsters() {
        return controlled.size();
    }
    
    public Set<MapleMonster> getControlled() {
        return controlled;
    }

    @Override
    public String toString() {
        return "Character: " + this.name;
    }

    public int getAccountID() {
        return accountid;
    }
    
    public void dispelDebuff(Disease debuff) {
        if (hasDisease(debuff)) {
            long mask = debuff.getValue();
            
            this.announce(PacketCreator.CancelDebuff(mask));
            field.broadcastMessage(this, PacketCreator.CancelForeignDebuff(id, mask), false);
            
            chrLock.lock();
            try {
                diseases.remove(debuff);
            } finally {
                chrLock.unlock();
            }
        }
    }
    
    public void dispelDebuffs() {
        diseases.keySet().stream().forEach((d) -> {
            dispelDebuff(d);
        });
    }
    
    public void kill() {
        stats.setHp(0);
        stats.setMp(0);
        stats.updateSingleStat(PlayerStat.HP, 0);
        stats.updateSingleStat(PlayerStat.MP, 0);
    }

   public void updateQuestMobCount(int id) {
	int lastQuestProcessed = 0;
        try {
            synchronized (quests) {
                for (MapleQuestStatus q : quests.values()) {
                    lastQuestProcessed = q.getQuest().getId();
                    if (q.getStatus() == MapleQuestStatus.Status.COMPLETED || q.getQuest().canComplete(this, null)) {
                        continue;
                    }
                    String progress = q.getProgress(id);
                    if (!progress.isEmpty() && Integer.parseInt(progress) >= q.getQuest().getMobAmountNeeded(id)) {
                        continue;
                    }
                    if (q.progress(id)) {
                        client.announce(PacketCreator.UpdateQuest(q, false));
                    }
                }
            }
        } catch (NumberFormatException e) {
	    FileLogger.printError(FileLogger.EXCEPTION_CAUGHT, e, "MapleCharacter.mobKilled. CID: " + this.id + " last Quest Processed: " + lastQuestProcessed);
        }
    }
   
   public final byte getQuestStatus(final int quest) {
        synchronized (quests) {
            for (final MapleQuestStatus q : quests.values()) {
                if (q.getQuest().getId() == quest) {
                    return (byte) q.getStatus().getId();
                }
            }
            return 0;
        }
    }
    
    public final MapleQuestStatus getMapleQuestStatus(final int quest) {
        synchronized (quests) {
            for (final MapleQuestStatus q : quests.values()) {
                if (q.getQuest().getId() == quest) {
                    return q;
                }
            }
            return null;
        }
    }
 
    public MapleQuestStatus getQuest(MapleQuest quest) {
        synchronized (quests) {
            if (!quests.containsKey(quest.getId())) {
                return new MapleQuestStatus(quest, MapleQuestStatus.Status.NOT_STARTED);
            }
            return quests.get(quest.getId());
        }
    }
    
    public boolean needQuestItem(int questid, int itemid) {
        if (questid <= 0) {
            return true;
        }
        MapleQuest quest = MapleQuest.getInstance(questid);
        return getInventory(ItemConstants.getInventoryType(itemid)).countById(itemid) < quest.getItemAmountNeeded(itemid);
    }

    public final List<MapleQuestStatus> getStartedQuests() {
        List<MapleQuestStatus> ret = new LinkedList<>();
        for (MapleQuestStatus q : quests.values()) {
            if (q.getStatus().equals(MapleQuestStatus.Status.STARTED)) {
                ret.add(q);
            }
        }
        return Collections.unmodifiableList(ret);
    }

    public final int getStartedQuestsSize() {
        synchronized (quests) {
            int i = 0;
            for (MapleQuestStatus q : quests.values()) {
                if (q.getStatus().equals(MapleQuestStatus.Status.STARTED)) {
                    if (q.getQuest().getInfoNumber() > 0) {
                        i++;
                    }
                    i++;
                }
            }
            return i;
        }
    }

    public final List<MapleQuestStatus> getCompletedQuests() {
        synchronized (quests) {
            List<MapleQuestStatus> ret = new LinkedList<>();
            for (MapleQuestStatus q : quests.values()) {
                if (q.getStatus().equals(MapleQuestStatus.Status.COMPLETED)) {
                    ret.add(q);
                }
            }
            
            return Collections.unmodifiableList(ret);
        }
    }

    public PlayerShop getPlayerShop() {
        return playerShop;
    }

    public void setPlayerShop(PlayerShop playerShop) {
        this.playerShop = playerShop;
    }
    
    public Merchant getHiredMerchant() {
        return hiredMerchant;
    }
    
    public void setHiredMerchant(Merchant merchant) {
        this.hiredMerchant = merchant;
    }

    public Map<PlayerSkill, PlayerSkillEntry> getSkills() {
        return Collections.unmodifiableMap(skills);
    }

    public void removeBuffs() {
        LinkedList<BuffStatValueHolder> allBuffs = new LinkedList<>(effects.values());
        for (BuffStatValueHolder mbsvh : allBuffs) {
            cancelEffect(mbsvh.effect, false, mbsvh.startTime);
        }
    }
    
    public void dispelSkill() {
        dispelSkill(0);
    }

    public void dispelSkill(int skillId) {
        LinkedList<BuffStatValueHolder> allBuffs;
        chrLock.lock();
        try {
            allBuffs = new LinkedList<>(effects.values());
        } finally {
            chrLock.unlock();
        }
        for (BuffStatValueHolder mbsvh : allBuffs) {
            if (skillId == 0) {
                if (mbsvh.effect.isSkill()) {
                    switch (mbsvh.effect.getSourceId()) {
                        case Beginner.MonsterRider:
                        case DarkKnight.Beholder:
                        case FPArchMage.Elquines:
                        case ILArchMage.Ifrit:
                        case Priest.SummonDragon:
                        case Bishop.Bahamut:
                        case Ranger.Puppet:
                        case Ranger.SilverHawk:
                        case Sniper.Puppet:
                        case Sniper.GoldenEagle:
                        case Hermit.ShadowPartner:
                            cancelEffect(mbsvh.effect, false, mbsvh.startTime);
                            break;
                    }
                }
            } else {
                if (mbsvh.effect.isSkill() && mbsvh.effect.getSourceId() == skillId) {
                    cancelEffect(mbsvh.effect, false, mbsvh.startTime);
                    break;
                }
            }
        }
    }
   
    public int getSkillLevel(PlayerSkill skill) {
        PlayerSkillEntry ret = skills.get(skill);
        if (ret == null) {
            return 0;
        }
        return ret.skillevel;
    }
    
    public int getSkillLevel(int skill) {
        PlayerSkillEntry ret = skills.get(PlayerSkillFactory.getSkill(skill));
        if (ret == null) {
            return 0;
        }
        return ret.skillevel;
    }

    public int getMasterLevel(PlayerSkill skill) {
        PlayerSkillEntry ret = skills.get(skill);
        if (ret == null) {
            return 0;
        }
        return ret.masterlevel;
    }
    
    public boolean isRingEquipped(int ringId) {
        for (Item item : getInventory(InventoryType.EQUIPPED)) {
            Equip equip = (Equip) item;
            if (equip.getRing().getRingDatabaseId() == ringId) {
                return equip.getPosition() <= (byte) -1;
            }
        }
        return false;
    }
    
    public int getEquippedRing(int type) {
        for (Item item : getInventory(InventoryType.EQUIPPED)) {
            Equip equip = (Equip) item;
            if (equip.getRing() != null) {
                int itemId = equip.getItemId();
                if (ItemConstants.isCrushRing(itemId) && type == ItemRingType.CRUSH_RING.getType()) {
                    return equip.getRing().getRingDatabaseId();
                }
                if (ItemConstants.isFriendshipRing(itemId) && type == ItemRingType.FRIENDSHIP_RING.getType()) {
                    return equip.getRing().getRingDatabaseId();
                }
                if (ItemConstants.isWeddingRing(itemId) && type == ItemRingType.WEDDING_RING.getType()) {
                    return equip.getRing().getRingDatabaseId();
                }
            }
        }
        return 0;
    }

    public List<ItemRing> getCrushRings() {
        Collections.sort(crushRings);
        return crushRings;
    }

    public List<ItemRing> getFriendshipRings() {
        Collections.sort(friendshipRings);
        return friendshipRings;
    }

    public List<ItemRing> getWeddingRings() {
        Collections.sort(weddingRings);
        return weddingRings;
    }

    public void addRingToCache(int ringId) {
        ItemRing ring = ItemRing.loadingRing(ringId);
        if (ring != null) {
            if (ItemConstants.isCrushRing(ring.getItemId())) {
                crushRings.add(ring);
            } else if (ItemConstants.isFriendshipRing(ring.getItemId())) {
                friendshipRings.add(ring);
            } else if (ItemConstants.isWeddingRing(ring.getItemId())) {
                weddingRings.add(ring);
            }
        }
    }
    
    public void levelUp(boolean takeExp) {
        PlayerSkill improvingMaxHP = null;
        PlayerSkill improvingMaxMP = null;
        
        int improvingMaxHPLevel = 0;
        int improvingMaxMPLevel = 0;
        
        stats.remainingAp += 5;
        
        int maxHP = stats.maxHP;
        int maxMP = stats.maxMP;
        
        if (job == PlayerJob.BEGINNER) {
            maxHP += Randomizer.rand(12, 16);
            maxMP += Randomizer.rand(10, 12);
        } else if (job.isA(PlayerJob.WARRIOR)) {
            improvingMaxHP = PlayerSkillFactory.getSkill(Swordman.ImprovedMaxHpIncrease);
            improvingMaxHPLevel = getSkillLevel(improvingMaxHP);
            maxHP += Randomizer.rand(24, 28);
            maxMP += Randomizer.rand(4, 6);
            stats.remainingSp += 3;
        } else if (job.isA(PlayerJob.MAGICIAN)) {
            improvingMaxMP = PlayerSkillFactory.getSkill(Magician.ImprovedMaxMpIncrease);
            improvingMaxMPLevel = getSkillLevel(improvingMaxMP);
            maxHP += Randomizer.rand(10, 14);
            maxMP += Randomizer.rand(22, 24);
            stats.remainingSp += 3;
        } else if (job.isA(PlayerJob.BOWMAN) || job.isA(PlayerJob.THIEF) || job.isA(PlayerJob.GM)) {
            maxHP += Randomizer.rand(20, 24);
            maxMP += Randomizer.rand(14, 16);
            stats.remainingSp += 3;
        } else if (job.isA(PlayerJob.PIRATE)) {
            improvingMaxHP = PlayerSkillFactory.getSkill(Brawler.ImproveMaxHp);
            improvingMaxHPLevel = getSkillLevel(improvingMaxHP);
            maxHP += Randomizer.rand(22, 28);
            maxMP += Randomizer.rand(18, 23);
            stats.remainingSp += 3;
        }
        if (improvingMaxHPLevel > 0) {
            if (improvingMaxHP != null) {
                maxHP += improvingMaxHP.getEffect(improvingMaxHPLevel).getX();
            }
        }
        if (improvingMaxMPLevel > 0) {
            if (improvingMaxMP != null) {
                maxMP += improvingMaxMP.getEffect(improvingMaxMPLevel).getX();
            }
        }
        
        maxMP += stats.getTotalInt() / 10;
        if (takeExp) {
            exp.addAndGet(-ExperienceConstants.getExpNeededForLevel(level));
            if (exp.get() < 0) {
                exp.set(0);
            }
        }
        level++;
        
        if (level >= 200) {
            exp.set(0);
            level = 200;
        }
        
        stats.maxHP = (Math.min(30000, maxHP));
        stats.maxMP = (Math.min(30000, maxMP));
        
        if (level == 200) {
            exp.set(0);
        }
        
        stats.recalcLocalStats();
        
        stats.hp = maxHP;
        stats.mp = maxMP;
        
        List<Pair<PlayerStat, Integer>> statup = new ArrayList<>(8);
        statup.add(new Pair<>(PlayerStat.AVAILABLEAP, stats.getRemainingAp()));
        
        statup.add(new Pair<>(PlayerStat.HP, getStat().getCurrentMaxHp()));
        statup.add(new Pair<>(PlayerStat.MP, getStat().getCurrentMaxMp()));
        
        statup.add(new Pair<>(PlayerStat.MAXHP, stats.getMaxHp()));
        statup.add(new Pair<>(PlayerStat.MAXMP, stats.getMaxMp()));
        
        statup.add(new Pair<>(PlayerStat.EXP, Integer.valueOf(exp.get())));
        statup.add(new Pair<>(PlayerStat.LEVEL, level));
        statup.add(new Pair<>(PlayerStat.AVAILABLESP, stats.getRemainingSp()));
        
        this.announce(PacketCreator.UpdatePlayerStats(statup));
        getMap().broadcastMessage(this, PacketCreator.ShowThirdPersonEffect(getId(), PlayerEffects.LEVEL_UP.getEffect()), false);
        
        stats.recalcLocalStats();
        
        if (getParty() != null) {
            silentPartyUpdate();
        } 
        if (getGuild() != null) {
            guildUpdate();
        }
        
        setLevelUpHistory(this, level);
        
        if (level == 200 && !isGameMaster()) {
            BroadcastService.broadcastMessage(PacketCreator.ServerNotice(6, String.format(GameConstants.REACHED_MAX_LEVEL, this.name, this.name)));
        }
    }
    
    public void setLevelUpHistory(Player p, int level) {
        if (this.isGameMaster()) {
            return;
        }
        try {
            Connection con = DatabaseConnection.getConnection();
            try (PreparedStatement ps = con.prepareStatement("INSERT INTO levelhistory (accountid, characterid, level, date) VALUES (?, ?, ?, ?)")) {
                ps.setInt(1, this.accountid);
                ps.setInt(2, this.id);
                ps.setInt(3, level);
                ps.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
                ps.executeUpdate();
            } finally {
                con.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public void resetBattleshipHp() {
        this.battleShipHP = 4000 * getSkillLevel(PlayerSkillFactory.getSkill(Corsair.Battleship)) + ((getLevel() - 120) * 2000);
    }
    
    public int getCurrentBattleShipHP() {
        return battleShipHP;
    }
    
    public final void sendBattleshipHP(int damage) {
        this.battleShipHP -= damage;
        if (battleShipHP <= 0) {
            this.battleShipHP = 0;
            PlayerSkill battleship = PlayerSkillFactory.getSkill(Corsair.Battleship);
            int cooldown = battleship.getEffect(getSkillLevel(battleship)).getCoolDown();
            client.getSession().write(PacketCreator.SkillCooldown(Corsair.Battleship, cooldown));
            addCoolDown(Corsair.Battleship, System.currentTimeMillis(), cooldown * 1000);
            dispelSkill(Corsair.Battleship);
        } 
    }

    public void changeKeybinding(int key, PlayerKeyBinding keybinding) {
        if (keybinding.getType() != 0) {
            keymap.put(Integer.valueOf(key), keybinding);
        } else {
            keymap.remove(Integer.valueOf(key));
        }
    }
    
    public Field getWarpMap(int map) {
	Field target;
        EventInstanceManager eim = getEventInstance();
	if (eim == null) {
            target = client.getChannelServer().getMapFactory().getMap(map);
	} else {
            target = eim.getMapInstance(map);
	}
	return target;
    }
    
    private void eventChangedMap(int map) {
        EventInstanceManager eim = getEventInstance();
        if (eim != null) eim.changedMap(this, map);
    }
    
    private void eventAfterChangedMap(int map) {
        EventInstanceManager eim = getEventInstance();
        if (eim != null) eim.afterChangedMap(this, map);
    }

    public void broadcastChangeJob(String newJob, int typeJob) {
        BroadcastService.broadcastMessage(PacketCreator.ServerNotice(6, "[" + typeJob + "st Job] Congratulations to <" + getName() + "> on becoming a < " + newJob + ">!"));
    }

    public void sendKeymap() {
        client.getSession().write(PacketCreator.GetKeyMap(keymap));
    }
     
    public PlayerSkillMacro[] getMacros() {
        return skillMacros;
    }

    public void setMacros(PlayerSkillMacro[] newMacros) {
        skillMacros = newMacros;
        this.setChangedSkillMacros(true);
    }

    public void tempban(String reason, Calendar duration, int greason) {
        if (lastMonthFameIDs == null) {
            throw new RuntimeException("Trying to ban a non-loaded character (testhack)");
        }
        tempban(reason, duration, greason, client.getAccountID());
        client.getSession().close();
    }

    public static boolean tempban(String reason, Calendar duration, int greason, int accountid) {
        try {
            Connection con = DatabaseConnection.getConnection();
            try (PreparedStatement ps = con.prepareStatement("UPDATE accounts SET tempban = ?, banreason = ?, greason = ? WHERE id = ?")) {
                Timestamp TS = new Timestamp(duration.getTimeInMillis());
                ps.setTimestamp(1, TS);
                ps.setString(2, reason);
                ps.setInt(3, greason);
                ps.setInt(4, accountid);
                ps.executeUpdate();
            }
            return true;
        } catch (SQLException ex) {
            FileLogger.printError("TempBan.txt", ex);
        }
        return false;
    }

    public void ban(String reason) {
        try {
            getClient().banMacs();
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("UPDATE accounts SET banned = 1, banreason = ? WHERE id = ?");
            ps.setString(1, reason);
            ps.setInt(2, accountid);
            ps.executeUpdate();
            ps.close();
            
            ps = con.prepareStatement("INSERT INTO ipbans VALUES (DEFAULT, ?)");
            String[] ipSplit = client.getSession().getRemoteAddress().toString().split(":");
            ps.setString(1, ipSplit[0]);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException ex) {
            FileLogger.printError(FileLogger.DATABASE_EXCEPTION, ex);
        }
    }

    public static boolean ban(String id, String reason, boolean accountId) {
        PreparedStatement ps = null;
        try {
            Connection con = DatabaseConnection.getConnection();
            if (id.matches("/[0-9]{1,3}\\..*")) {
                ps = con.prepareStatement("INSERT INTO ipbans VALUES (DEFAULT, ?)");
                ps.setString(1, id);
                ps.executeUpdate();
                ps.close();
                return true;
            }
            if (accountId) {
                ps = con.prepareStatement("SELECT id FROM accounts WHERE name = ?");
            } else {
                ps = con.prepareStatement("SELECT accountid FROM characters WHERE name = ?");
            }

            boolean ret = false;
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    try (PreparedStatement psb = DatabaseConnection.getConnection().prepareStatement("UPDATE accounts SET banned = 1, banreason = ? WHERE id = ?")) {
                        psb.setString(1, reason);
                        psb.setInt(2, rs.getInt(1));
                        psb.executeUpdate();
                    }
                    ret = true;
                }
            }
            ps.close();
            return ret;
        } catch (SQLException ex) {
        } finally {
            try {
                if (ps != null && !ps.isClosed()) {
                    ps.close();
                }
            } catch (SQLException e) {
            }
        }
        return false;
    }
    
    public void ban(String reason, boolean dc) {
        try {
            client.banMacs();
         //   client.banHWID();
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("UPDATE accounts SET banned = 1, banreason = ? WHERE id = ?");
            ps.setString(1, reason);
            ps.setInt(2, accountid);
            ps.executeUpdate();
            ps.close();
            ps = con.prepareStatement("INSERT INTO ipbans VALUES (DEFAULT, ?)");
            ps.setString(1, client.getSession().getRemoteAddress().toString().split(":")[0]);
            ps.executeUpdate();
            ps.close();
          } catch (SQLException e) {
        }
        if (dc) {
            getClient().disconnect(true, true); 
        }
    }

    /**
     * Oid of players is always = the cid
     * @return 
     */
    @Override
    public int getObjectId() {
        return getId();
    }

    /**
     * Throws unsupported operation exception, oid of players is read only
     * @param id
     */
    @Override
    public void setObjectId(int id) {
        throw new UnsupportedOperationException();
    }

    public StorageKeeper getStorage() {
        return storage;
    }
    
    public List<Player> getPartyMembers() {
        List<Player> list = new LinkedList<>();
        
        prtLock.lock();
        try {
            if(party != null) {
                for(MaplePartyCharacter partyMembers: party.getMembers()) {
                    list.add(partyMembers.getPlayer());
                }
            }
        } finally {
            prtLock.unlock();
        }
        
        return list;
    }
    
    public int getAriantPoints() {
        return this.ariantPoints;
    }
    
    public void gainAriantPoints(int gain){
       this.ariantPoints += gain;
       dropMessage(5, "You " + (gain > 0 ? "gained" : "lost") + " (" + gain + ") point(s).");
    }
   
    public void gainVotePoints(int gain){
      this.votePoints += gain;
    }

    public int getvotePoints(){
        return this.votePoints;
    }
    
    public boolean allowedMapChange() {
        return this.allowMapChange;
    }

    public void setallowedMapChange(boolean allowed) {
        this.allowMapChange = allowed;
    }
    
    public void addVisibleMapObject(FieldObject mo) {
        visibleMapObjects.add(mo);
    }

    public void removeVisibleMapObject(FieldObject mo) {
        visibleMapObjects.remove(mo);
    }

    public boolean isMapObjectVisible(FieldObject mo) {
        return visibleMapObjects.contains(mo);
    }
    
    public Collection<FieldObject> getVisibleMapObjects() {
        return Collections.unmodifiableCollection(visibleMapObjects);
    }
    
    public String getPartyQuestItems() {
        return dataString;
    }

    public boolean gotPartyQuestItem(String partyquestchar) {
        return dataString.contains(partyquestchar);
    }

    public void removePartyQuestItem(String letter) {
        if (gotPartyQuestItem(letter)) {
            dataString = dataString.substring(0, dataString.indexOf(letter)) + dataString.substring(dataString.indexOf(letter) + letter.length());
        }
    }

    public void setPartyQuestItemObtained(String partyquestchar) {
        if (!dataString.contains(partyquestchar)) {
            this.dataString += partyquestchar;
        }
    }

    public boolean isAlive() {
        return stats.getHp() > 0;
    }

    public void setSlot(int slotid) {
        slots = slotid;
    }
    
    public boolean allowedToTarget(Player other) {
        return other != null && !other.isHidden() || this.getAdministrativeLevel() >= 3;
    }

    @Override
    public void sendDestroyData(Client client) {
        client.getSession().write(PacketCreator.RemovePlayerFromMap(this.getObjectId()));
    }

    @Override
    public void sendSpawnData(Client client) {
        if (!this.isHidden() || client.getPlayer().getAdministrativeLevel() > 1) {
            client.getSession().write(PacketCreator.SpawnPlayerMapObject(this));
        }
        if (this.isHidden()) {
            List<Pair<BuffStat, Integer>> stat = Collections.singletonList(new Pair<>(BuffStat.DARKSIGHT, 0));
            field.broadcastGMMessage(this, PacketCreator.BuffMapEffect(getId(), stat, false), false);
        }

        for (final ItemPet pet : pets){
            if (pet.getSummoned()){
                client.getSession().write(PetPackets.ShowPet(this, pet, false, false));
            }
        }
        if (chalkBoardText != null) {
            client.getSession().write(PacketCreator.UseChalkBoard(this, false));
        }

        if (summons != null) {
            for (final MapleSummon summon : summons.values()) {
                client.getSession().write(PacketCreator.SpawnSpecialFieldObject(summon, false));
            }
        }
    }

    public void setLastSelectNPCTime(long time) {
        this.lastSelectNPCTime = System.currentTimeMillis();
    }
    
    public long getLastSelectNPCTime() {
        return lastSelectNPCTime;
    }
    
    public void setLastTalkTime(long time) {
        lastSelectNPCTime = time;
    }
    
    public long getLastAttackTime() {
        return lastAttackTime;
    }
    
    public void setLastAttackTime(long time) {
        lastAttackTime = time;
    }
    
    public long getLastHitTime() {
        return lastHitTime;
    }
    
    public void setLastHitTime(long time) {
        lastHitTime = time;
    }
    
    public boolean canAction() {
        if (System.currentTimeMillis() > (lastHitTime+5000)) {
            return true;
        } else if (System.currentTimeMillis() < (lastHitTime+5000)) {
            return false;
        }
        return true;
    }

    public void TamingMob(int id, int skillid) {
        tamingMob = new TamingMob(this, id, skillid);
    }

    public TamingMob getMount() {
        return tamingMob;
    }
    
    public void equipChanged() {
        getMap().broadcastMessage(this, PacketCreator.UpdateCharLook(this), false);
        stats.recalcLocalStats();
        stats.enforceMaxHpMp();
        if (getMessenger() != null) {
            MessengerService.updateMessenger(getMessenger().getId(), getName(), client.getChannel());
        }
    }

    public final ItemPet getPet(final int index) {
        byte count = 0;
        if (pets == null) {
            return null;
        }
        for (final ItemPet pet : pets) {
            if (pet.getSummoned()) {
                if (count == index) {
                    return pet;
                }
                count++;
            }
        }
        return null;
    }
    
    public final ItemPet getPetByUID(final int uid) {
        if (pets == null) {
            return null;
        }
        for (final ItemPet pet : pets) {
            if (pet.getSummoned()) {
                if (pet.getUniqueId() == uid) {
                    return pet;
                }
            }
        }
        return null;
    }

    public void removePetCS(ItemPet pet) {
        pets.remove(pet);
    }

    public void addPet(final ItemPet pet) {
        if (pets.contains(pet)) {
            pets.remove(pet);
        }
        pets.add(pet);
    }
    
     public void removePet(ItemPet pet, boolean shiftLeft) {
        pet.setSummoned(0);
    }

   public final byte getPetIndex(final ItemPet petz) {
        byte count = 0;
        for (final ItemPet pet : pets) {
            if (pet.getSummoned()) {
                if (pet.getUniqueId() == petz.getUniqueId()) {
                    return count;
                }
                count++;
            }
        }
        return -1;
    }

    public final ArrayList<ItemPet> getSummonedPets() {
        return getSummonedPets(new ArrayList<>());
    }

    public final ArrayList<ItemPet> getSummonedPets(ArrayList<ItemPet> ret) {
        ret.clear();
        for (final ItemPet pet : pets) {
            if (pet.getSummoned()) {
                ret.add(pet);
            }
        }
        return ret;
    }
    
    public final byte getPetIndex(final int petId) {
        byte count = 0;
        for (final ItemPet pet : pets) {
            if (pet.getSummoned()) {
                if (pet.getUniqueId() == petId) {
                    return count;
                }
                count++;
            }
        }
        return -1;
    }
    
    public final byte getPetById(final int petId) {
        byte count = 0;
        for (final ItemPet pet : pets) {
            if (pet.getSummoned()) {
                if (pet.getPetItemId() == petId) {
                    return count;
                }
                count++;
            }
        }
        return -1;
    }
     
    public final List<ItemPet> getPets() {
        return pets;
    }

    public final void unequipAllPets() {
        for (final ItemPet pet : pets) {
            if (pet != null) {
                unequipPet(pet, true, false);
            }
        }
    }
    
    public void unequipPet(ItemPet pet, boolean shiftLeft, boolean hunger) {
        if (pet.getSummoned()) {
            pet.saveDatabase();
            
           client.getSession().write(PetPackets.PetStatUpdate(this));
           
            if (field != null) {
                field.broadcastMessage(this, PetPackets.ShowPet(this, pet, true, hunger), true);
            }
            removePet(pet, shiftLeft);
            if (GameConstants.GMS) {
                client.getSession().write(PetPackets.PetStatUpdate(this));
            }
            client.write(PacketCreator.EnableActions());
        }
    }
    
    public int getActivePets() {
        Connection con = DatabaseConnection.getConnection();
        try {
            try (PreparedStatement ps = con.prepareStatement("SELECT `pets` FROM characters WHERE id = ?")) {
                ps.setInt(1, getId());
                try (ResultSet rs = ps.executeQuery()) {
                    while(rs.next()) {
                        final String[] petss = rs.getString("pets").split(",");
                        List<Integer> pet_data = new ArrayList<>();
                        for (int i = 0; i < 3; i++) {
                            int v1 = Integer.parseInt(petss[i]);
                            if (v1 != -1)
                                pet_data.add(Integer.parseInt(petss[i]));
                        }
                        return pet_data.size();
                    }
                    ps.close();
                }
            }
        } catch (SQLException e) {
            System.out.println("Player was not added to the map due to a pet error!\r\nError: " + e.getMessage());
        }
        return 0;
    }
    
    public void spawnPet(byte slot) {
        spawnPet(slot, false, true);
    }
    
    public void spawnPet(byte slot, boolean lead) {
        spawnPet(slot, lead, true);
    }
    
    public void spawnPet(byte slot, boolean lead, boolean broadcast) {
        final Item item = getInventory(InventoryType.CASH).getItem(slot);
        if (item == null) {
            return;
        }
        switch (item.getItemId()) {
            case 5000047:
            case 5000028: {
                final ItemPet pet = ItemPet.createPet(item.getItemId() + 1, InventoryIdentifier.getInstance());
                if (pet != null) {
                    InventoryManipulator.addById(client, item.getItemId() + 1, (short) 1, item.getOwner(), "", pet);
                    InventoryManipulator.removeFromSlot(client, InventoryType.CASH, slot, (short) 1, false);
                }
                break;
            }
            default: {
                final ItemPet pet = item.getPet();
                if (FieldLimit.CANNOTUSEPET.check(field.getFieldLimit())) {
                    announce(PetPackets.RemovePet(this.getId(), getPetIndex(pet), (byte) 3));
                    return;
                } 
                if (pet != null && (item.getItemId() != 5000054 || pet.getSecondsLeft() > 0) && (item.getExpiration() == -1 || item.getExpiration() > System.currentTimeMillis())) {
                    if (pet.getSummoned()) { 
                        unequipPet(pet, true, false);
                    } else {
                        if (getSkillLevel(PlayerSkillFactory.getSkill(Beginner.FollowTheLead)) == 0 && getPet(0) != null) {
                            unequipPet(getPet(0), false, false);
                        }
                        final Point pos = getPosition();
                        pos.y -= 12;
                        pet.setPosition(pos);
                        MapleFoothold fh = field.getFootholds().findBelow(pet.getPosition());
                        pet.setFoothold(fh != null ? fh.getId() : 0);
                        pet.setStance(0);
                        pet.setSummoned(1);
                        addPet(pet);
                        pet.setSummoned(getPetIndex(pet) + 1); 
                        if (broadcast && getMap() != null) {
                            field.broadcastMessage(this, PetPackets.ShowPet(this, pet, false, false), true);
                            if (!pet.getExceptionList().isEmpty()) {
                                client.getSession().write(PetPackets.PetExceptionListResult(this, pet));
                            }
                            if (GameConstants.GMS) {
                               client.write(PetPackets.PetStatUpdate(this));
                            }
                        }
                    }
                } 
                break;
            }
        }
        client.getSession().write(PetPackets.EmptyStatUpdate());
    }
    
    public void updatePetAuto() {
        if (getAutoHpPot() > 0) {
            client.getSession().write(PetPackets.AutoHpPot(getAutoHpPot()));
        } 
        if (getAutoMpPot() > 0) {
            client.getSession().write(PetPackets.AutoMpPot(getAutoMpPot()));
        }
    }
    
    public void expireOnLogout() {
        for(Inventory inv : this.inventory) {
            for (Item item : inv.list()) {
                if (ItemInformationProvider.getInstance().isExpireOnLogout(item.getItemId())) {
                    inv.removeItem(item.getPosition());
                }
            }
        }
    }
    
    public boolean isChallenged() {
        return challenged;
    }

    public void setChallenged(boolean challenged) {
        this.challenged = challenged;
    }
    
    public FameStatus canGiveFame(Player from) {
        if (gm > 0) {
            return FameStatus.OK;
        } else if (from == null || lastMonthFameIDs == null || lastMonthFameIDs.contains(Integer.valueOf(from.getId()))) {
            return FameStatus.NOT_THIS_MONTH;
        } else {
            return FameStatus.OK;
        }
    }

    public void hasGivenFame(Player to) {
        lastFameTime = System.currentTimeMillis();
        lastMonthFameIDs.add(Integer.valueOf(to.getId()));
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("INSERT INTO famelog (characterid, characterid_to) VALUES (?, ?)")) {
                ps.setInt(1, getId());
                ps.setInt(2, to.getId());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
        }
    }
    
    public MapleParty getParty() {
        prtLock.lock();
        try {
            return party;
        } finally {
            prtLock.unlock();
        }
    }

    public int getPartyId() {
        prtLock.lock();
        try {
            return (party != null ? party.getId() : -1);
        } finally {
            prtLock.unlock();
        }
    }

    public void setParty(MapleParty p) {
        prtLock.lock();
        try {
            if (p == null) {
                this.mpc = null;
                doorSlot = -1;
                party = null;
            } else {
                party = p;
            }
        } finally {
            prtLock.unlock();
        }
    }

    public Trade getTrade() {
        return trade;
    }

    public void setTrade(Trade trade) {
        this.trade = trade;
    }

    public EventInstanceManager getEventInstance() {
        return eventInstance;
    }
    
    public void setEventInstance(EventInstanceManager eventInstance) {
        this.eventInstance = eventInstance;
    }
  
    public void addDoor(Integer owner, MapleDoor door) {
        chrLock.lock();
        try {
            doors.put(owner, door);
        } finally {
            chrLock.unlock();
        }
    }
    
    public void removeDoor(Integer owner) {
        chrLock.lock();
        try {
            doors.remove(owner);
        } finally {
            chrLock.unlock();
        }
    }
    
    public int getDoorSlot() {
        if(doorSlot == -1) {
            doorSlot = (party == null) ? 0 : party.getPartyDoor(this.getId());
        }
        
        return doorSlot;
    }

    public void clearDoors() {
        doors.clear();
    }

    public Map<Integer, MapleDoor> getDoors() {
        chrLock.lock();
        try {
            return Collections.unmodifiableMap(doors);
        } finally {
            chrLock.unlock();
        }
    }

    public boolean canDoor() {
        return canDoor;
    }
    
    public void fakeRelog() {
        client.getSession().write(PacketCreator.GetCharInfo(this));
        final Field mapp = getMap();
        mapp.removePlayer(this);
        mapp.addPlayer(this);
    }
	    
    public void disableDoor() {
        canDoor = false;
        CharacterTimer.getInstance().schedule(() -> {
           canDoor = true;
        }, 5000);
    }

    public Map<Integer, MapleSummon> getSummons() {
        return summons;
    }
    
    public Collection<MapleSummon> getSummonsValues() {
        return summons.values();
    }

    public int getChair() {
        return chair;
    }

    public int getItemEffect() {
        return itemEffect;
    }

    public void setChair(int chair) {
        this.chair = chair;
    }

    public void setItemEffect(int itemEffect) {
        this.itemEffect = itemEffect;
    }

    public Collection<Inventory> allInventories() {
        return Arrays.asList(inventory);
    }

    @Override
    public FieldObjectType getType() {
        return FieldObjectType.PLAYER;
    }
    
    public boolean isGuildLeader() { 
        return guild > 0 && guildRank < 3;
    }

    public int getGuildId() {
        return guild;
    }
       
    public int getGuildRank() {
        return guildRank;
    }

    public void setGuildId(int _id) {
        guild = _id;
        if (guild > 0) {
            if (mgc == null) {
                mgc = new MapleGuildCharacter(this);
            } else {
                mgc.setGuildId(guild);
            }
        } else {
            mgc = null;
        }
    }

    public void setGuildRank(int _rank) {
        guildRank = _rank;
        if (mgc != null) {
            mgc.setGuildRank(_rank);
        }
    }

    public MapleGuildCharacter getMGC() {
        return mgc;
    }
    
    public Minigame getMiniGame() {
        return miniGame;
    }
    
    public void setMiniGame(Minigame miniGame) {
        this.miniGame = miniGame;
    }
    
    public int getMiniGamePoints(String type, boolean omok) {
        if (omok) {
            switch (type) {
                case "wins":
                    return omokWins;
                case "losses":
                    return omokLosses;
                default:
                    return omokTies;
            }
        } else {
            switch (type) {
                case "wins":
                    return matchCardWins;
                case "losses":
                    return matchCardLosses;
                default:
                    return matchCardTies;
            }
        }
    }
    
    public void setMiniGamePoints(Player visitor, int winnerslot, boolean omok) {
        if (omok) {
            switch (winnerslot) {
                case 1:
                    this.omokWins++;
                    visitor.omokLosses++;
                    break;
                case 2:
                    visitor.omokWins++;
                    this.omokLosses++;
                    break;
                default:
                    this.omokTies++;
                    visitor.omokTies++;
                    break;
            }
        } else {
            switch (winnerslot) {
                case 1:
                    this.matchCardWins++;
                    visitor.matchCardLosses++;
                    break;
                case 2:
                    visitor.matchCardWins++;
                    this.matchCardLosses++;
                    break;
                default:
                    this.matchCardTies++;
                    visitor.matchCardTies++;
                    break;
            }
        }
    }
    
    public void guildUpdate() {
        if (this.guild <= 0) {
            return;
        }
        mgc.setLevel(this.level);
        mgc.setJobId(this.job.getId());
        GuildService.memberLevelJobUpdate(mgc);
    }

    public void genericGuildMessage(int code) {
        this.client.getSession().write(GuildPackets.GenericGuildMessage((byte) code));
    }
    
    public void saveGuildStatus() {
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("UPDATE characters SET guildid = ?, guildrank = ?, allianceRank = ? WHERE id = ?")) {
                ps.setInt(1, this.guild);
                ps.setInt(2, this.guildRank);
                ps.setInt(3, this.allianceRank);
                ps.setInt(4, this.id);
                ps.execute();
            }
        } catch (SQLException se) {
            System.err.println("SQL Error: " + se);
        }
    }
    
    public void setHasMerchant(boolean set) {
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("UPDATE characters SET HasMerchant = ? WHERE id = ?")) {
                ps.setInt(1, set ? 1 : 0);
                ps.setInt(2, getId());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("SQL Error: " + e);
        }
        hasMerchant = set;
    }
        
    public void empty() {
        try {
            if (client.getChannelServer().getPlayerStorage().getCharacterByName(getName()) != null) {
                client.getChannelServer().removePlayer(this);
            }
            if (getMount() != null) {
                this.getMount().cancelSchedule();
            }
            if (BerserkSchedule != null) {
                this.BerserkSchedule.cancel(true);
                this.BerserkSchedule = null;
            }
            if (beholderBuffSchedule != null) {
                this.beholderBuffSchedule.cancel(true);
                this.beholderBuffSchedule = null;
            }
            if (beholderHealingSchedule != null) {
                this.beholderHealingSchedule.cancel(true);
                this.beholderHealingSchedule = null;
            }
            this.buddyList = null;
            this.controlled = null;
            this.coolDowns = null;
            this.diseases = null;
            this.rps = null;
            if (recoveryTask != null) {
                recoveryTask.cancel(false);
            }
            if (dragonBloodSchedule != null) {
                this.dragonBloodSchedule.cancel(true);
                this.dragonBloodSchedule = null;
            }
            this.doors = null;
            this.effects = null;
            this.eventInstance = null;
            if (this.expireTask != null) {
                this.expireTask.cancel(true);
                this.expireTask = null;
            }
            this.crushRings = null;
            this.weddingRings = null;
            this.friendshipRings = null;
            if (keymap != null) {
                this.keymap.clear();
                this.keymap = null;
            }
            this.lastMonthFameIDs.clear();
            this.lastMonthFameIDs = null;
            if (mapTimeLimitTask != null) {
                this.mapTimeLimitTask.cancel(true);
                this.mapTimeLimitTask = null;
            }
            if (tamingMob != null) {
                tamingMob = null;
            }
            this.mgc = null;
            this.mpc = null;
            this.field = null;
            this.pets = null;
            this.party = null;
            if (quests != null) {
                this.quests.clear();
                this.quests = null;
            }
            this.savedLocations = null;
            this.shop = null;
            this.skillMacros = null;
            this.skills.clear();
            this.skills = null;
            this.storage = null;
            this.summons.clear();
            this.summons = null;
            this.visibleMapObjects.clear();
            this.visibleMapObjects = null;
            timers.forEach((sf) -> {
                sf.cancel(false);
            });
            this.timers.clear();
        } catch (final Throwable e) {
	    FileLogger.printError("Account_Empty.txt", e);
	}
    }
            
    
    public boolean haveItemEquipped(int itemid) {
        return getInventory(InventoryType.EQUIPPED).findById(itemid) != null;
    }
    
    public boolean haveItem(int itemid, int quantity, boolean checkEquipped, boolean greaterOrEquals) {
        int possesed = inventory[ItemInformationProvider.getInstance().getInventoryType(itemid).ordinal()].countById(itemid);
        
        if (checkEquipped)
            possesed += inventory[InventoryType.EQUIPPED.ordinal()].countById(itemid);
        return greaterOrEquals ? possesed >= quantity : possesed == quantity;
    }
    
    public void setMeso(int set) {
        meso.set(set);
        stats.updateSingleStat(PlayerStat.MESO, set, false);
    }

    public boolean getCanSmega() {
        return canSmega;
    }

    public void setCanSmega(boolean yn) {
        canSmega = yn;
    }

    public boolean getSmegaEnabled() {
        return smegaEnabled;
    }

    public void setSmegaEnabled(boolean yn) {
        smegaEnabled = yn;
    }

    public void sendServerNotice(String msg) {
        OutPacket packet = PacketCreator.ServerNotice(5, msg);
        BroadcastService.broadcastMessage(packet);
    }

    public void gainFame(int delta) {
        this.addFame(delta);
        stats.updateSingleStat(PlayerStat.FAME, this.pop);
    }

    public void yellowMessage(String m) {
        announce(PacketCreator.SendYellowTip(m));
    }
    
    public int getJobId() {
        return this.getJob().getId();
    }
    
    public void checkBerserk(final boolean isHidden) {
        if (BerserkSchedule != null) {
            BerserkSchedule.cancel(false);
        }
        final Player p = this;
        if (job.equals(PlayerJob.DARKKNIGHT)) {
            PlayerSkill BerserkX = PlayerSkillFactory.getSkill(DarkKnight.Berserk);
            final int skilllevel = getSkillLevel(BerserkX);
            if (skilllevel > 0) {
                Berserk = p.getHp() * 100 / p.getStat().getMaxHp() < BerserkX.getEffect(skilllevel).getX();
                BerserkSchedule = CharacterTimer.getInstance().register(() -> {
                    getClient().getSession().write(PacketCreator.ShowOwnBerserk(skilllevel, Berserk));
                    if (!isHidden) field.broadcastMessage(Player.this, PacketCreator.ShowBerserk(getId(), skilllevel, Berserk), false);
                    else field.broadcastGMMessage(Player.this, PacketCreator.ShowBerserk(getId(), skilllevel, Berserk), false);
                }, 5000, 3000);
            }
        }
    }
   
    public void setGMLevel(int level) {
        if (level >= 5) {
            this.gm = 5;
        } else if (level < 0) {
            this.gm = 0;
        } else {
            this.gm = level;
        }
    }

    public long getUseTime() {
        return useTime;
    }

    public void sendPolice(String text) {
    }
    
    public void questTimeLimit(final MapleQuest quest, int time) {
        ScheduledFuture<?> sf = CharacterTimer.getInstance().schedule(() -> {
            announce(PacketCreator.QuestExpire(quest.getId()));
            MapleQuestStatus newStatus = new MapleQuestStatus(quest, MapleQuestStatus.Status.NOT_STARTED);
            newStatus.setForfeited(getQuest(quest).getForfeited() + 1);
            updateQuest(newStatus);
        }, time * 60 * 1000);
        announce(PacketCreator.AddQuestTimeLimit(quest.getId(), time * 60 * 1000));
        timers.add(sf);
    }
    
    public void setMPC(MaplePartyCharacter mpc) {
        this.mpc = mpc;
    }

    public MapleStatEffect getBuffEffect(BuffStat stat) {
        effLock.lock();
        chrLock.lock();
        try {
            BuffStatValueHolder mbsvh = effects.get(stat);
            if (mbsvh == null) {
                return null;
            } else {
                return mbsvh.effect;
            }
        } finally {
            chrLock.unlock();
            effLock.unlock();
        }
    }
    
    public int getJobType() {
        return job.getId() / 1000;
    }

    public CashShop getCashShop() {
        return cashShop;
    }

    public final Inventory[] getInventorys() {
	return inventory;
    }
     
    public final long getLastFameTime() {
	return lastFameTime;
    }

    public final List<Integer> getFamedCharacters() {
	return lastMonthFameIDs;
    }

    public final int[] getSavedLocations() {
	return savedLocations;
    }
    
    public final void spawnSavedPets() {
        spawnSavedPets(false, false);
    }

    public final void spawnSavedPets(boolean lead, boolean broadcast) {
        for (int i = 0; i < petStore.length; i++) {
            if (petStore[i] > -1) {
                spawnPet(petStore[i], lead, broadcast);
            }
        }
        if (GameConstants.GMS) {
            client.getSession().write(PetPackets.PetStatUpdate(this));
        }
        petStore = new byte[]{-1, -1, -1};
    }

    public final byte[] getPetStores() {
        return petStore;
    }

    public void changeChannel(final int channel) {
        final ChannelServer toch = ChannelServer.getInstance(channel);
        if (channel == client.getChannel() || toch == null || toch.isShutdown()) {
            return;
        }
        
        if (this.getTrade() != null) {
            Trade.cancelTrade(getTrade(), this);
        }
        
        final ChannelServer ch = ChannelServer.getInstance(client.getChannel());
        if (getMessenger() != null) {
            MessengerService.silentLeaveMessenger(getMessenger().getId(), new MapleMessengerCharacter(this));
        }
        Merchant merchant = this.getHiredMerchant();
        if (merchant != null) {
            if (merchant.isOwner(this)) {
                merchant.setOpen(true);
            } else {
                merchant.removeVisitor(this);
            }
        }
        
        PlayerBuffStorage.addBuffsToStorage(getId(), getAllBuffs());
        PlayerBuffStorage.addCooldownsToStorage(getId(), getAllCooldowns());
        PlayerBuffStorage.addDiseaseToStorage(getId(), getAllDiseases());
        
        this.cancelAllBuffs(true);
        this.cancelAllDebuffs();
        
        if (this.getBuffedValue(BuffStat.PUPPET) != null) {
            this.cancelEffectFromBuffStat(BuffStat.PUPPET);
        }
        if (this.getBuffedValue(BuffStat.COMBO) != null) {
            this.cancelEffectFromBuffStat(BuffStat.COMBO);
        }
        
        
        this.getMap().removePlayer(this);
        ch.removePlayer(this);
        
        client.updateLoginState(ClientLoginState.CHANGE_CHANNEL, client.getSessionIPAddress());
        String[] socket = ChannelServer.getInstance(channel).getIP().split(":");
        try {
            announce(PacketCreator.GetChannelChange(InetAddress.getByName(socket[0]), Integer.parseInt(socket[1])));
        } catch (UnknownHostException ex) {
            FileLogger.printError(FileLogger.PLAYER_STUCK, ex);
        }
        
        saveDatabase();
        client.setPlayer(null); 
    }

    public int getAutoHpPot() {
        return petAutoHP;
    }

    public void setAutoHpPot(int itemId) {
        petAutoHP = itemId;
    }

    public int getAutoMpPot() {
        return petAutoMP;
    }

    public void setAutoMpPot(int itemId) {
        petAutoMP = itemId;
    }
    
    public long getPlaytime(){
        long time = Calendar.getInstance().getTimeInMillis();
        playtime += time - playtimeStart;
        playtimeStart = time;
        return playtime;
    }

    public void spawnBomb() {
        final MapleMonster bomb = MapleLifeFactory.getMonster(9300166);
        getMap().spawnMonsterOnGroudBelow(bomb, getPosition());
        EventTimer.getInstance().schedule(() -> {
            field.killMonster(bomb, client.getPlayer(), false, (byte) 1);
        }, 10 * 1000);
    }

    public int[] getRocks() {
        return rocks;
    }

    public int getRockSize() {
        int ret = 0;
        for (int i = 0; i < 10; i++) {
            if (rocks[i] != 999999999) {
                ret++;
            }
        }
        return ret;
    }

    public void deleteRocks(int map, boolean isVip) {
        if (isVip) {
            for (int i = 0; i < 10; i++) {
                if (rocks[i] == map) {
                    rocks[i] = 999999999;
                    changedTrockLocations = true;
                    break;
                }
            }
        } else {
            for (int i = 0; i < 5; i++) {
                if (regrocks[i] == map) {
                    regrocks[i] = 999999999;
                    changedRegrockLocations = true;
                    break;
                }
            }
        }
    }

    public void addRockMap(boolean isVip) {
        if (isVip) {
            if (getRockSize() >= 10) {
                return;
            }
            rocks[getRockSize()] = getMapId();
            changedTrockLocations = true;
        } else {
            if (getRegRockSize() >= 5) {
                return;
            }
            regrocks[getRegRockSize()] = getMapId();
            changedRegrockLocations = true;
        }
    }

    public boolean isRockMap(int id) {
        for (int i = 0; i < 10; i++) {
            if (rocks[i] == id) {
                return true;
            }
        }
        return false;
    }

    public int[] getRegRocks() {
        return regrocks;
    }

    public int getRegRockSize() {
        int ret = 0;
        for (int i = 0; i < 5; i++) {
            if (regrocks[i] != 999999999) {
                ret++;
            }
        }
        return ret;
    }

    public boolean isRegRockMap(int id) {
        for (int i = 0; i < 5; i++) {
            if (regrocks[i] == id) {
                return true;
            }
        }
        return false;
    }

    public void openShop(int id) {
        ShopFactory.getInstance().getShop(id).sendShop(getClient());
    }

    public void closePlayerShop() {
        PlayerShop mps = this.getPlayerShop();
        if (mps == null) {
            return;
        }
        
        if (mps.isOwner(this)) {
            mps.setOpen(false);
            
            client.getChannelServer().unregisterPlayerShop(mps);
            
            for (PlayerShopItem mpsi : mps.getItems()) {
                if (mpsi.getBundles() >= 2) {
                    Item iItem = mpsi.getItem().copy();
                    iItem.setQuantity((short) (mpsi.getBundles() * iItem.getQuantity()));
                    InventoryManipulator.addFromDrop(this.getClient(), iItem, "", false);
                } else if (mpsi.isExist()) {
                    InventoryManipulator.addFromDrop(this.getClient(), mpsi.getItem(), "", true);
                }
            }
            mps.closeShop();
        } else {
            mps.removeVisitor(this);
        }
        this.setPlayerShop(null);
    }

    public void closeMiniGame() {
        Minigame game = this.getMiniGame();
        if (game == null) {
            return;
        }
        
        this.setMiniGame(null);
        if (game.isOwner(this)) {
            this.getMap().broadcastMessage(MinigamePackets.RemoveCharBox(this));
            game.broadcastToVisitor(MinigamePackets.GetMiniGameClose());
        } else {
            game.removeVisitor(this);
        }
    }

   public void closeHiredMerchant(boolean closeMerchant) {
        Merchant merchant = this.getHiredMerchant();
        if (merchant == null) {
            return;
        }
        
        if (closeMerchant) {
            merchant.removeVisitor(this);
            this.setHiredMerchant(null);
        }
        else {
            if (merchant.isOwner(this)) {
                merchant.setOpen(true);
            } else {
                merchant.removeVisitor(this);
            }
            try {
                merchant.saveItems(false);
            } catch (SQLException ex) {
                ex.printStackTrace();
                System.out.println("Error while saving Hired Merchant items.");
            }
        }
    }

    public MapleFoothold getFoothold() {
        Point pos = this.getPosition();
        pos.y -= 6;
        return getMap().getFootholds().findBelow(pos);
    }

    public final boolean canHP(long now) {
        if (lastHPTime + 5000 < now) {
            lastHPTime = now;
            return true;
        }
        return false;
    }

    public final boolean canMP(long now) {
        if (lastMPTime + 5000 < now) {
            lastMPTime = now;
            return true;
        }
        return false;
    }
    
    public long getLastHPTime() {
        return lastHPTime;
    }

    public long getLastMPTime() {
        return lastMPTime;
    }

    public boolean isPartyLeader() {
        prtLock.lock();
        try {
            return party.getLeader().getId() == getId();
        } finally {
            prtLock.unlock();
        }
    }

    public boolean hasEmptySlot(byte invType) {
        return getInventory(InventoryType.getByType(invType)).getNextFreeSlot() > -1;
    }

    public long getNpcCooldown() {
        return npcCd;
    }
    
    public void setNpcCooldown(long d) {
        npcCd = d;
    }

    public long getLastUsedCashItem() {
        return lastUsedCashItem;
    }
    
    public void setLastUsedCashItem(long time) {
        this.lastUsedCashItem = time;
    }

    public enum FameStatus {
        OK, NOT_TODAY, NOT_THIS_MONTH
    }
    
    public long getLastRequestTime() {
        return lastRequestTime;
    }
    
    public void setLastRequestTime(long time) {
        lastRequestTime = time;
    }

    public int getBuddyCapacity() {
        return buddyList.getCapacity();
    }

    public void setBuddyCapacity(byte capacity) {
        buddyList.setCapacity(capacity);
        client.getSession().write(PacketCreator.UpdateBuddyCapacity(capacity));
    }

    public MapleMessenger getMessenger() {
        return messenger;
    }

    public void setMessenger(MapleMessenger messenger) {
        this.messenger = messenger;
    }
    
    public int getFh() {
        Point pos = this.getPosition();
	pos.y -= 6;
        if (getMap().getFootholds().findBelow(pos) == null) {
            return 0;
        } else {
            return getMap().getFootholds().findBelow(pos).getY1();
        }
    }

    public void setMatchCardPoints(Player visitor, int winnerslot) {
    }

    public void setHasCheat(boolean cheat) {
        this.hasCheat = cheat;
    }

    public boolean isHasCheat() {
        return this.hasCheat;
    }     
    
    public void addCoolDown(int skillId, long startTime, long length) {
        effLock.lock();
        chrLock.lock();
        try {
            if (this.coolDowns.containsKey(Integer.valueOf(skillId))) {
                this.coolDowns.remove(Integer.valueOf(skillId));
            }
            coolDowns.put(Integer.valueOf(skillId), new PlayerCoolDownValueHolder(skillId, startTime, length));
        } finally {
            chrLock.unlock();
            effLock.unlock();
        }
    }

    public void removeCooldown(int skillId) {
        effLock.lock();
        chrLock.lock();
        try {
            if (coolDowns.containsKey(Integer.valueOf(skillId))) {
                coolDowns.remove(Integer.valueOf(skillId));
            }
        } finally {
            chrLock.unlock();
            effLock.unlock();
        }   
    }

    public boolean skillisCooling(int skillId) {
        effLock.lock();
        chrLock.lock();
        try {
            return coolDowns.containsKey(Integer.valueOf(skillId));
        } finally {
            chrLock.unlock();
            effLock.unlock();
        }
    }
    
    public void giveCoolDowns(final int skillid, long starttime, long length) {  
        addCoolDown(skillid, starttime, length);
    }
    
    public void giveCoolDowns(final List<PlayerCoolDownValueHolder> cooldowns) {
        if (cooldowns != null) {
            for (PlayerCoolDownValueHolder cooldown : cooldowns) {
                coolDowns.put(cooldown.skillId, cooldown);
            }
        } else {
            try {
                Connection con = DatabaseConnection.getConnection();
                ResultSet rs;
                try (PreparedStatement ps = con.prepareStatement("SELECT skillid, starttime, length FROM cooldowns WHERE charid = ?")) {
                    ps.setInt(1, getId());
                    rs = ps.executeQuery();
                    while (rs.next()) {
                        final int skillid = rs.getInt("skillid");
                        final long length = rs.getLong("length"), startTime = rs.getLong("starttime");
                        if (skillid != 5221999 && (length + startTime < System.currentTimeMillis())) {
                            continue;
                        }
                        giveCoolDowns(rs.getInt("skillid"), rs.getLong("starttime"), rs.getLong("length"));
                    }
                }
                rs.close();
                
                PlayerSaveFactory.DeleteType.SKILL_COOLDOWN.removeFromType(con, this.id);

            } catch (SQLException e) {
                System.err.println("Error while retriving cooldown from SQL storage");
            }
        }
    }
    
    public int getCooldownSize() {
        return coolDowns.size();
    }
    
    public List<PlayerCoolDownValueHolder> getAllCooldowns() {
        List<PlayerCoolDownValueHolder> ret = new ArrayList<>();
        
        effLock.lock();
        chrLock.lock();
        try {
            for (PlayerCoolDownValueHolder mcdvh : coolDowns.values()) {
                ret.add(new PlayerCoolDownValueHolder(mcdvh.skillId, mcdvh.startTime, mcdvh.length));
            }
        } finally {
            chrLock.unlock();
            effLock.unlock();
        }
        
        return ret;
    }
    
    public void removeAllCooldownsExcept(int id, boolean packet) {
        effLock.lock();
        chrLock.lock();
        try {
            ArrayList<PlayerCoolDownValueHolder> list = new ArrayList<>(coolDowns.values());
            for (PlayerCoolDownValueHolder mcvh : list) {
                if (mcvh.skillId != id) {
                    coolDowns.remove(mcvh.skillId);
                    if (packet) {
                        client.announce(PacketCreator.SkillCooldown(mcvh.skillId, 0));
                    }
                }
            }
        } finally {
            chrLock.unlock();
            effLock.unlock();
        }
    }

    public final void giveSilentDebuff(final List<DiseaseValueHolder> ld) {
        chrLock.lock();
        try {
            if (ld != null) {
                ld.stream().forEach((disease) -> {
                    diseases.put(disease.disease, disease);
                });
            }
        } finally {
            chrLock.unlock();
        }
    }

    public String getMapName(int mapId) {
        return client.getChannelServer().getMapFactory().getMap(mapId).getMapName();
    }
    
    public int getDiseaseSize() {
        return diseases.size();
    }
    
    public void giveDebuff(final Disease disease, MobSkill skill) {
        giveDebuff(disease, skill.getX(), skill.getDuration(), skill.getSkillId(), skill.getSkillLevel());
    }

    public void giveDebuff(final Disease disease, int x, long duration, int skillid, int level) {
        if (isAlive() && !isActiveBuffedValue(Bishop.HolyShield) && !hasDisease(disease) && diseases.size() < 2) {
            if ((disease != Disease.SEDUCE) && (disease != Disease.STUN) && (getBuffedValue(BuffStat.HOLY_SHIELD) != null)) {
                return;
            }     
            chrLock.lock();
            try {
                this.diseases.put(disease, new DiseaseValueHolder(disease, System.currentTimeMillis(), duration));
            } finally {
                chrLock.unlock();
            } 
            
            final List<Pair<Disease, Integer>> debuff = Collections.singletonList(new Pair<>(disease, Integer.valueOf(x)));
            this.announce(PacketCreator.GiveDebuff(debuff, skillid, level, (int) duration));
            field.broadcastMessage(this, PacketCreator.GiveForeignDebuff(id, debuff, skillid, level), false);
        }
    }
    
    public final boolean hasDisease(final Disease dis) {
        chrLock.lock();
        try {
            return diseases.containsKey(dis);
        } finally {
            chrLock.unlock();
        }   
    }
    
    public final int getDiseasesSize() {
        chrLock.lock();
        try {
            return diseases.size();
        } finally {
            chrLock.unlock();
        }
    }
    
    public int getTeam() {   
        if (this.MCPQTeam == null) {
            return -1;
        }
      return this.MCPQTeam.code;
    }

    public MCField.MCTeam getMCPQTeam() {
        return MCPQTeam;
    }

    public void setMCPQTeam(MCField.MCTeam MCPQTeam) {
        this.MCPQTeam = MCPQTeam;
    }

    public MCParty getMCPQParty() {
        return MCPQParty;
    }

    public void setMCPQParty(MCParty MCPQParty) {
        this.MCPQParty = MCPQParty;
    }

    public MCField getMCPQField() {
        return MCPQField;
    }

    public void setMCPQField(MCField MCPQField) {
        this.MCPQField = MCPQField;
    }

    public int getAvailableCP() {
        return availableCP;
    }

    public void setAvailableCP(int availableCP) {
        this.availableCP = availableCP;
    }

    public int getTotalCP() {
        return totalCP;
    }

    public void setTotalCP(int totalCP) {
        this.totalCP = totalCP;
    }

    public void gainCP(int cp) {
        this.availableCP += cp;
        this.totalCP += cp;
    }

    public void loseCP(int cp) {
        this.availableCP -= cp;
    }  
    
    public final ArrayList<DiseaseValueHolder> getAllDiseases(ArrayList<DiseaseValueHolder> ret) {
        chrLock.lock();
        try {
            ret.clear();
            for (DiseaseValueHolder mc : diseases.values()) {
                if (mc != null) {
                    ret.add(mc);
                }
            }
            return ret;
        } finally {
            chrLock.unlock();
        }
    }

    public final List<DiseaseValueHolder> getAllDiseases() {
        chrLock.lock();
        try {
            return new ArrayList<>(diseases.values());
        } finally {
            chrLock.unlock();
        }
    }
    
    public void cancelAllDebuffs() {
        chrLock.lock();
        try {
            diseases.clear();
        } finally {
            chrLock.unlock();
        }
    }

    public void setLevel(int level) {
        this.level = level - 1;
    }

    public void setMap(int PmapId) {
        this.mapId = PmapId;
    }
    
    private void prepareBeholderEffect() {
	if (beholderHealingSchedule != null) {
	    beholderHealingSchedule.cancel(false);
	}
	if (beholderBuffSchedule != null) {
	    beholderBuffSchedule.cancel(false);
	}
	PlayerSkill healing = PlayerSkillFactory.getSkill(DarkKnight.AuraOfBeholder);
	int healingLevel = getSkillLevel(healing);
	if (healingLevel > 0) {
	    final MapleStatEffect healEffect = healing.getEffect(healingLevel);
	    int healInterval = healEffect.getX() * 1000;
	    beholderHealingSchedule = CharacterTimer.getInstance().register(() -> {
                stats.addHP(healEffect.getHp());
                client.getSession().write(EffectPackets.ShowOwnBuffEffect(DarkKnight.Beholder, PlayerEffects.SKILL_AFFECTED.getEffect()));
                field.broadcastMessage(Player.this, PacketCreator.SummonSkill(getId(), DarkKnight.Beholder, 5), true);
                field.broadcastMessage(Player.this, EffectPackets.BuffMapVisualEffect(getId(), DarkKnight.Beholder, PlayerEffects.SKILL_AFFECTED.getEffect()), false);
            }, healInterval, healInterval);
	}
	PlayerSkill buff = PlayerSkillFactory.getSkill(DarkKnight.HexOfBeholder);
	int buffLevel = getSkillLevel(buff);
	if (buffLevel > 0) {
	    final MapleStatEffect buffEffect = buff.getEffect(buffLevel);
	    int buffInterval = buffEffect.getX() * 1000;
	    beholderBuffSchedule = CharacterTimer.getInstance().register(() -> {
                buffEffect.applyTo(Player.this);
                client.getSession().write(EffectPackets.ShowOwnBuffEffect(DarkKnight.Beholder, PlayerEffects.SKILL_AFFECTED.getEffect()));
                field.broadcastMessage(Player.this, PacketCreator.SummonSkill(getId(), DarkKnight.Beholder, (int) (Math.random() * 3) + 6), true);
                field.broadcastMessage(Player.this, EffectPackets.BuffMapVisualEffect(getId(), DarkKnight.Beholder, PlayerEffects.SKILL_AFFECTED.getEffect()), false);
            }, buffInterval, buffInterval);
	}
    }
    
    public int getPartnerId() {
        return partner;
    }

    public void setPartnerId(final int mi) {
        this.partner = mi;
    }

    public int getMarriageItemId() {
        return spouseItemId;
    }

    public void setMarriageItemId(final int mi) {
        this.spouseItemId = mi;
    }
    
    public String getPartner() {
        return PlayerQuery.getNameById(partner);
    }
  
    public int countItem(int itemid) {
        InventoryType type = ItemInformationProvider.getInstance().getInventoryType(itemid);
        Inventory iv = inventory[type.ordinal()];
        int possesed = iv.countById(itemid);
        return possesed;
    }

    public long getLastPortalEntry() {
        return lastPortalEntry;
    }

    public void setLastPortalEntry(long lastPortalEntry) {
        this.lastPortalEntry = lastPortalEntry;
    }

    public void assassinate() {
        stats.addHP(-30000);
    }

    public void giveItemBuff(int itemID) {
        ItemInformationProvider mii = ItemInformationProvider.getInstance();
        MapleStatEffect statEffect = mii.getItemEffect(itemID);
        statEffect.applyTo(this);
    }

    public long getLastCatch() {
        return lastCatch;
    }

    public void setLastCatch(long lastCatch) {
        this.lastCatch = lastCatch;
    }
    
    public boolean hasShield() {
        return shield;
    }

    public void setShield(boolean shield) {
        this.shield = shield;
    }

    public void shield(ScheduledFuture<?> schedule) {
        if (this.shield) {
            return;
        }

        List<Pair<BuffStat, Integer>> stat = Collections.singletonList(new Pair<>(BuffStat.SHIELD, Integer.valueOf(1)));
        
        setBuffedValue(BuffStat.SHIELD, Integer.valueOf(1));
        
                
        getClient().getSession().write(PacketCreator.GiveBuff(2022269, 60 * 1000, stat));
        
        getMap().broadcastMessage(this, PacketCreator.BuffMapEffect(getId(), stat, false), false);
        
        
            
        this.shield = true;
       
    }

    public void cancelShield() {
        if (getClient().getChannelServer().getPlayerStorage().getCharacterById(getId()) != null) { 
            if (!this.shield) {
                return;
            }
            stats.recalcLocalStats();
            stats.enforceMaxHpMp();
            
            List<BuffStat> stat = Collections.singletonList(BuffStat.SHIELD);
               
            client.getSession().write(PacketCreator.CancelBuff(stat));
            field.broadcastMessage(this, PacketCreator.CancelForeignBuff(getId(), stat), false);
            
            this.shield = false;
        }
    }
    
    public void message(String m) {
        dropMessage(5, m);
    }
    
    public void dropMessage(int a, String string) {
        this.getClient().getSession().write(PacketCreator.ServerNotice(a, string));
    }

    public void dropMessage(String string) {
        dropMessage(5, string);
    }
    
    public void announce(OutPacket packet) {
        client.announce(packet);
    }
    
    public final void showHint(String msg) {
        showHint(msg, (short) 500);
    }
    
    public void showHint(String msg, short length) {
        client.announceHint(msg, (short) length);
    }
    
    public int getMerchantMeso() {
        return merchantMesos;
    }
    
    public void addMerchantMesos(int add) {
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("UPDATE characters SET merchantMesos = ? WHERE id = ?", Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, merchantMesos + add);
                ps.setInt(2, id);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            return;
        }
        merchantMesos += add;
    }

    public void setMerchantMeso(int set) {
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("UPDATE characters SET merchantMesos = ? WHERE id = ?", Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, set);
                ps.setInt(2, id);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            return;
        }
        merchantMesos = set;
    }
    
    public synchronized void withdrawMerchantMesos() {
        int merchantMeso = this.getMerchantMeso();
        if (merchantMeso > 0) {
            int possible = Integer.MAX_VALUE - merchantMeso;
            
            if (possible > 0) {
                if (possible < merchantMeso) {
                    this.gainMeso(possible, false);
                    this.setMerchantMeso(merchantMeso - possible);
                } else {
                    this.gainMeso(merchantMeso, false);
                    this.setMerchantMeso(0);
                }
            }
        }
    }
    
    public void updateAriantScore() {
        this.getMap().broadcastMessage(PacketCreator.UpdateAriantPQRanking(this.getName(), this.countItem(ItemConstants.ARIANT_JEWEL), false));
    }

    public int getRandomage(Player player) {
        int maxdamage = player.getStat().getCurrentMaxBaseDamage();
        int mindamage = player.getStat().calculateMinBaseDamage(player);
        return Randomizer.rand(mindamage, maxdamage);
    }

    public int getMinDmg(Player player) {
        int mindamage = player.getStat().calculateMinBaseDamage(player);
        return mindamage;
    }

    public int getMaxDmg(Player player) {
        int maxdamage = player.getStat().getCurrentMaxBaseDamage();
        return maxdamage;
    }

    public Date getTime() {
	return time;
    }

    public void setTime(Date time) {
	this.time = time;
    }
    
    public void removeAriantRoom(int room) {
        ariantRoomLeader[room] = "";
        ariantRoomSlot[room] = 0;
    }
    
    public String getAriantRoomLeaderName(int room) {
        return ariantRoomLeader[room];
    }

    public int getAriantSlotsRoom(int room) {
        return ariantRoomSlot[room];
    }
    
    public void setAriantRoomLeader(int room, String charname) {
        ariantRoomLeader[room] = charname;
    }

    public void setAriantSlotRoom(int room, int slot) {
        ariantRoomSlot[room] = slot;
    }
    
    public boolean getInteractionsOpen() {
        return trade != null || this.playerShop != null ||this.hiredMerchant != null; 
    }

    public void setAllianceRank(int rank) {
        allianceRank = rank;
        if (mgc != null) {
            mgc.setAllianceRank(rank);
        }
    }

    public int getAllianceRank() {
        return this.allianceRank;
    }
    
    public SpeedQuiz getSpeedQuiz() {
        return sq;
    }
 
    public void setSpeedQuiz(SpeedQuiz sq) {
        this.sq = sq;
    }
    
    public long getLastSpeedQuiz() {
        return lastSpeedQuiz;
    }
    
    public void setLastSpeedQuiz(final long t) {
        this.lastSpeedQuiz = t;
    } 

    public MapleGuild getGuild() {
        if (getGuildId() < 1) {
            return null;
        }
        return GuildService.getGuild(getGuildId());
    }

    public void maxAllSkills() {
        for (int skills : SkillConstants.allSkills) {
            maxSkill(skills);
        }
    }

    public void maxSkill(int skillid) {
        if (Math.floor(skillid / 10000) == getJob().getId() || isGameMaster() || skillid < 2000) {  
            PlayerSkill skill = PlayerSkillFactory.getSkill(skillid);
            int maxLevel = skill.getMaxLevel();  
            changeSkillLevel(skill, maxLevel, maxLevel);
        }
    }
    
    public int getAveragePartyLevel() {
        int averageLevel = 0, size = 0;
            for (MaplePartyCharacter pl : getParty().getMembers()) {
                averageLevel += pl.getLevel();
                size++;
            }
            if (size <= 0) {
                return level;
            }
        return averageLevel /= size;
    }
    
    public int getAverageMapLevel() {
        int averageLevel = 0, size = 0;
            for (Player pl : getMap().getCharacters()) {
                averageLevel += pl.getLevel();
                size++;
            }
            if (size <= 0) {
                return level;
            }
        return averageLevel /= size;
    }
    
    public void autoban(String reason) {
        this.ban(reason);
        announce(PacketCreator.SendPolice(String.format("You have been blocked by the#b %s Police for HACK reason.#k", ServerProperties.Login.SERVER_NAME)));
        CharacterTimer.getInstance().schedule(() -> {
            client.disconnect(false, false);
        }, 9000);
        
        BroadcastService.broadcastGMMessage(PacketCreator.ServerNotice(6, PlayerStringUtil.makeMapleReadable(this.name) + " was autobanned for " + reason));
    }
    
    public void gainItem(int id, short quantity, boolean showMessage) {
        gainItem(id, quantity, false, showMessage, -1);
    }
    
    public Item gainItem(int id, short quantity, boolean randomStats, boolean showMessage, long expires) {
        Item item = null;
        if (quantity >= 0) {
            ItemInformationProvider ii = ItemInformationProvider.getInstance();
 
            if (ItemConstants.getInventoryType(id).equals(InventoryType.EQUIP)) {
                item = ii.getEquipById(id);
            } else {
                item = new Item(id, (byte) 0, (short) quantity);
            }
            
            long l = Long.valueOf(expires);
            long time = 1000L * 60L * 60L * 24L * l;
            
            if (expires != -1) {
                for (int cards: ItemConstants.CARDS_4HRS) {
                    if (cards == item.getItemId()) {
                        item.setExpiration(System.currentTimeMillis() + 1000L * 60L * 60L * 4L);
                    } else {
                        item.setExpiration(System.currentTimeMillis() + expires);
                    }
                }
                if (item.getItemId() == 5211048 || item.getItemId() == 5360042) {
                    time = 1000L * 60L * 60L * 4L;
                    item.setExpiration(System.currentTimeMillis() + time);
                } else {
                    item.setExpiration(System.currentTimeMillis() + expires);
                }
            }
            if (!InventoryManipulator.checkSpace(getClient(), id, quantity, "")) {
                this.getClient().getPlayer().dropMessage(1, "Seu inventário está cheio. Por favor, remova um item do seu inventário e tente novamente.");
                return null;
            }
            if (ItemConstants.getInventoryType(id).equals(InventoryType.EQUIP) && !ItemConstants.isThrowingStar(id) && !ItemConstants.isBullet(id)) {
                if (randomStats) {
                    item = ii.randomizeStats((Equip) item);
                    InventoryManipulator.addFromDrop(getClient(), ii.randomizeStats((Equip) item), "", true);
                } else {
                    InventoryManipulator.addFromDrop(getClient(), (Equip) item, "", true);
                }
            } else {
                InventoryManipulator.addFromDrop(getClient(), item, "", true);
            }
        } else {
            InventoryManipulator.removeById(getClient(), ItemConstants.getInventoryType(id), id, -quantity, true, false);
        }
        if (showMessage) {
            this.getClient().getSession().write(PacketCreator.GetShowItemGain(id, quantity, false));
        }
        return item;
    }
    
    public boolean gainSlots(int type, int slots, boolean update) {
        slots += inventory[type].getSlotLimit();
        if (slots <= 96) {
            inventory[type].setSlotLimit(slots);

            this.saveDatabase();
            if (update) {
                client.announce(PacketCreator.UpdateInventorySlotLimit(type, slots));
            }

            return true;
        }
        return false;
    }
}