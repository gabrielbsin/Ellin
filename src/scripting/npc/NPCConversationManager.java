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

package scripting.npc;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;

import client.player.Player;
import client.Client;
import client.player.PlayerJob;
import server.itens.InventoryManipulator;
import server.itens.ItemInformationProvider;
import server.shops.ShopFactory;
import server.quest.MapleQuest;
import client.player.PlayerStat;
import community.MapleGuild;
import server.maps.Field;
import java.util.Map;
import java.util.Random;
import server.quest.MapleQuestStatus;
import handling.channel.ChannelServer;
import community.MaplePartyCharacter;
import handling.world.service.BroadcastService;
import packet.creators.EffectPackets;
import packet.creators.InteractionPackets;
import packet.creators.PacketCreator;
import client.player.PlayerSkin;
import client.player.SpeedQuiz;
import client.player.buffs.BuffStat;
import client.player.inventory.Equip;
import client.player.inventory.Inventory;
import client.player.inventory.types.InventoryType;
import client.player.inventory.Item;
import client.player.inventory.ItemFactory;
import client.player.skills.PlayerSkill;
import client.player.skills.PlayerSkillEntry;
import client.player.skills.PlayerSkillFactory;
import constants.GameConstants;
import constants.ItemConstants;
import database.DatabaseConnection;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import provider.MapleData;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;
import scripting.AbstractPlayerInteraction;
import server.MapleStatEffect;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.life.MapleMonsterInformationProvider;
import server.life.MonsterDropEntry;
import server.maps.FieldManager;
import server.maps.object.FieldObject;
import server.maps.object.FieldObjectType;
import server.maps.portal.Portal;
import server.partyquest.mcpq.MCParty;
import tools.Pair;


public class NPCConversationManager extends AbstractPlayerInteraction {


    private int npc;
    private Player p;
    private String getText;
    private byte lastMsg = -1;
    private String fileName = null;
    private MCParty mcParty;
    private List<MaplePartyCharacter> otherParty;
    private Collection<Player> characters = new LinkedHashSet<>();

    public NPCConversationManager(Client c, int npc) {
        super(c);
        this.c = c;
        this.npc = npc;
    }

    public NPCConversationManager(Client c, int npc, Player p) {
        super(c);
        this.c = c;
        this.npc = npc;
        this.p = p;
    }

    public NPCConversationManager(Client c, int npc, Player p, String fileName) {
        super(c);
        this.c = c;
        this.npc = npc;
        this.p = p;
        this.fileName = fileName;
    }

    public NPCConversationManager(Client c, int npc, List<MaplePartyCharacter> otherParty, MCParty mParty) {
        super(c);
        this.c = c;
        this.npc = npc;
        this.otherParty = otherParty;
        this.mcParty = mParty;
    }	
	
    public int getNpc() {
        return npc;
    } 
    
    public String getFileName() {
        return fileName;
    }
    
    public void dispose() {
        NPCScriptManager.getInstance().dispose(c);
    }
    
    public void sendNext(String text) {
        getClient().getSession().write(PacketCreator.GetNPCTalk(npc, (byte) 0, text, "00 01"));
    }
    
    public void sendPrev(String text) {
        getClient().getSession().write(PacketCreator.GetNPCTalk(npc, (byte) 0, text, "01 00"));
    }

    public void sendNextPrev(String text) {
        getClient().getSession().write(PacketCreator.GetNPCTalk(npc, (byte) 0, text, "01 01"));
    }

    public void sendOk(String text) {
        getClient().getSession().write(PacketCreator.GetNPCTalk(npc, (byte) 0, text, "00 00"));
    }

    public void sendYesNo(String text) {
        getClient().getSession().write(PacketCreator.GetNPCTalk(npc, (byte) 1, text, ""));
    }

    public void sendAcceptDecline(String text) {
        getClient().getSession().write(PacketCreator.GetNPCTalk(npc, (byte) 0x0C, text, ""));
    }

    public void sendSimple(String text) {
        getClient().getSession().write(PacketCreator.GetNPCTalk(npc, (byte) 4, text, ""));
    }

    public void sendSimplel(String text, String... selections) {
        if (selections.length > 0) {
            text += "#b\r\n";
        }

        for (int i=0; i<selections.length; i++) {
            text += "#L" + i + "#" + selections[i] + "#l\r\n";
        }
        sendSimple(text);
    } 
    
    public void sendStyle(String text, int styles[]) {
        getClient().getSession().write(PacketCreator.GetNPCTalkStyle(npc, text, styles));
    }

    public void sendGetNumber(String text, int def, int min, int max) {
        getClient().getSession().write(PacketCreator.GetNPCTalkNum(npc, text, def, min, max));
    }

    public void sendGetText(String text) {
        getClient().getSession().write(PacketCreator.GetNPCTalkText(npc, text));
    }
    
    public void setGetText(String text) {
        this.getText = text;
    }

    public String getText() {
        return this.getText;
    }

    public void openShop(int id) {
        ShopFactory.getInstance().getShop(id).sendShop(c);
    }

    public void openNpc(int id) {
        dispose();
        NPCScriptManager.getInstance().start(getClient(), id, null, null);
    }

    public void changeJob(PlayerJob job) {
        getPlayer().changeJob(job);
    }

    public PlayerJob getJob() {
        return getPlayer().getJob();
    }

    public void startQuest(short id) {
        try {
            MapleQuest.getInstance(id).forceStart(getPlayer(), npc);
        } catch (NullPointerException ex) {}
    }

    public void completeQuest(short id) {
        try {
            MapleQuest.getInstance(id).forceComplete(getPlayer(), npc);
        } catch (NullPointerException ex) {}
    }

    public void startQuest(int id) {
        try {
            MapleQuest.getInstance(id).forceStart(getPlayer(), npc);
        } catch (NullPointerException ex) {}
    }

    public void completeQuest(int id) {
        try {
            MapleQuest.getInstance(id).forceComplete(getPlayer(), npc);
        } catch (NullPointerException ex) {}
    }

    public void forfeitQuest(int id) {
        try {
            MapleQuest.getInstance(id).forfeit(getPlayer());
        } catch (NullPointerException ex) {}
    }

    public void gainMeso(int gain) {
        getPlayer().gainMeso(gain, true, false, true);
    }

    public void gainExp(int gain) {
        getPlayer().gainExperience(gain * c.getChannelServer().getExpRate(), true, true);
    }

    public int getLevel() {
        return getPlayer().getLevel();
    }
        
    @Override
    public int getPlayerCount(int mapid) {
        return c.getChannelServer().getMapFactory().getMap(mapid).getCharacters().size();
    }

    public void teachSkill(int id, int level, int masterlevel) {
        getPlayer().changeSkillLevel(PlayerSkillFactory.getSkill(id), level, masterlevel);
    }
        
    public int getJobId() { 
        return getPlayer().getJob().getId(); 
    }

    public void clearSkills() {
        Map<PlayerSkill, PlayerSkillEntry> skills = getPlayer().getSkills();
        skills.entrySet().forEach((skill) -> {
            getPlayer().changeSkillLevel(skill.getKey(), 0, 0);
        });
    }

    public void rechargeStars() {
        ItemInformationProvider ii = ItemInformationProvider.getInstance();
        Item stars = getPlayer().getInventory(InventoryType.USE).getItem((byte) 1);
        if (ItemConstants.isThrowingStar(stars.getItemId()) || ItemConstants.isBullet(stars.getItemId())) {
            stars.setQuantity(ii.getSlotMax(getClient(), stars.getItemId()));
            c.getSession().write(PacketCreator.UpdateInventorySlot(InventoryType.USE, (Item) stars));
        }
    }

    public void showEffect(String effect) {
        getPlayer().getMap().broadcastMessage(EffectPackets.ShowEffect(effect));
    }

    public void playSound(String sound) {
        getClient().getPlayer().getMap().broadcastMessage(EffectPackets.PlaySound(sound));
    }

    @Override
    public String toString() {
        return "Conversation with NPC: " + npc;
    }

    public void updateBuddyCapacity(int capacity) {
        c.getPlayer().setBuddyCapacity((byte) capacity);
    }

    public int getBuddyCapacity() {
        return getPlayer().getBuddyCapacity();
    }

    public void setHair(int hair) {
        getPlayer().setHair(hair);
        getPlayer().getStat().updateSingleStat(PlayerStat.HAIR, hair);
        getPlayer().equipChanged();
    }

    public void setFace(int face) {
        getPlayer().setFace(face);
        getPlayer().getStat().updateSingleStat(PlayerStat.FACE, face);
        getPlayer().equipChanged();
    }

    public void setSkin(int color) {
        getPlayer().setSkinColor(PlayerSkin.getById(color));
        getPlayer().getStat().updateSingleStat(PlayerStat.SKIN, color);
        getPlayer().equipChanged();
    }

    @Override
    public void warpParty(int mapId) {
        Field target = getMap(mapId);
        getPlayer().getParty().getMembers().stream().map((chr) -> c.getChannelServer().getPlayerStorage().getCharacterByName(chr.getName())).filter((curChar) -> ((curChar.getEventInstance() == null && getPlayer().getEventInstance() == null) || curChar.getEventInstance() == getPlayer().getEventInstance())).forEachOrdered((curChar) -> {
            curChar.changeMap(target, target.getPortal(0));
        });
    }
        
    public String getName() {
        return getPlayer().getName();
    }
    
    public void gainFame(int fame) {
        getPlayer().gainFame(fame);
    }
    
    public byte getLastMsg() {
        return lastMsg;
    }

    public final void setLastMsg(final byte last) {
        this.lastMsg = last;
    }
    
    public String startSpeedQuiz() {
        if (getPlayer().getSpeedQuiz() != null) {
            getPlayer().setSpeedQuiz(null);
            return "Ahh..it seemed that something was broken. Please let the admins know about this issue right away!";
        }
        final long time = c.getPlayer().getLastSpeedQuiz();
        final long now = System.currentTimeMillis();
        if (time > 0) {
            boolean can = (time + 3600000) < now;
            if (!can) {
                int remaining = (int) ((((time + 3600000) - now) / 1000) / 60);
                return "You've already tried the speed quiz in the past hour. Please come back again in " + remaining + " minutes.";
            }
        }
        getPlayer().setLastSpeedQuiz(now);
        getPlayer().setSpeedQuiz(new SpeedQuiz(c, npc));
        return null;
    } 

    public void warpPartyWithExp(int mapId, int exp) {
        Field target = getMap(mapId);
        for (MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
            Player curChar = c.getChannelServer().getPlayerStorage().getCharacterByName(chr.getName());
            if ((curChar.getEventInstance() == null && c.getPlayer().getEventInstance() == null) || curChar.getEventInstance() == getPlayer().getEventInstance()) {
                curChar.changeMap(target, target.getPortal(0));
                curChar.gainExp(exp, true, false);
            }
        }
    }

    public void warpPartyWithExpMeso(int mapId, int exp, int meso) {
        Field target = getMap(mapId);
        for (MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
            Player curChar = c.getChannelServer().getPlayerStorage().getCharacterByName(chr.getName());
            if ((curChar.getEventInstance() == null && c.getPlayer().getEventInstance() == null) || curChar.getEventInstance() == getPlayer().getEventInstance()) {
                curChar.changeMap(target, target.getPortal(0));
                curChar.gainExp(exp, true, false);
                curChar.gainMeso(meso, true);
            }
        }
    }
    
    public void warpAllInMap(int mapid, int portal) {
        Field outMap;
        FieldManager mapFactory;
        mapFactory = ChannelServer.getInstance(c.getChannel()).getMapFactory();
        outMap = mapFactory.getMap(mapid);
        for (Player p : outMap.getCharacters()) {
            mapFactory = ChannelServer.getInstance(p.getClient().getChannel()).getMapFactory();
            p.getClient().getPlayer().changeMap(outMap, outMap.getPortal(portal));
            outMap = mapFactory.getMap(mapid);
            p.getClient().getPlayer().getEventInstance().unregisterPlayer(p.getClient().getPlayer()); 
        }
    }

    public void warpRandom(int mapid) {
        Field target = c.getChannelServer().getMapFactory().getMap(mapid);
        Random rand = new Random();
        Portal portal = target.getPortal(rand.nextInt(target.getPortals().size())); 
        getPlayer().changeMap(target, portal);
    }

    public int itemQuantity(int itemid) {
        InventoryType type = ItemInformationProvider.getInstance().getInventoryType(itemid);
        int possesed = getPlayer().getInventory(type).countById(itemid);
        return possesed;
    }
        
    public void challengeParty(MCParty party, int field) {
        Player leader = null;
        Field map = c.getChannelServer().getMapFactory().getMap(980000100 + 100 * field);
        for (FieldObject mmo : map.getAllPlayer()) {
            Player mc = (Player) mmo;
            if (mc.getParty() == null || mc.getMCPQParty() == null) {
                sendOk("We could not find a party in this room.\r\nProbably the group was designed inside the room!");
                return;
            }
            if (mc.getParty().getLeader().getId() == mc.getId()) {
                leader = mc;
                break;
            } 
        }
        if (leader != null && leader.getMCPQField() != null) {
            if (!leader.isChallenged()) {
                List<MaplePartyCharacter> members = new LinkedList<>();
                for (MaplePartyCharacter fucker : c.getPlayer().getParty().getMembers()) {
                    members.add(fucker);
                }
                NPCScriptManager.getInstance().start("sendChallenge", leader.getClient(), npc, members, party);
            } else {
                sendOk("The other party is responding to a different challenge.");
            }
        } else {
            sendOk("Could not find the leader!");
        }
    }       
        
    public void resetReactors() {
        getPlayer().getMap().resetReactors();
    }

    public void displayGuildRanks() {
        MapleGuild.displayGuildRanks(getClient(), npc);
    }
        
    public Player getCharByName(String namee) {
        try {
            return getClient().getChannelServer().getPlayerStorage().getCharacterByName(namee);
        } catch (Exception e) {
           return null;
        }
    }
   
    public void addRandomItem(int id) {
        ItemInformationProvider i = ItemInformationProvider.getInstance();
        InventoryManipulator.addFromDrop(getClient(), i.randomizeStats((Equip) i.getEquipById(id)), "", true);
    }

    public void GachaMessage(int itemid) throws Exception {
        GachaMessage(itemid, false);
    }    
            
    public void GachaMessage(int itemid, boolean rare) throws Exception {
        ItemInformationProvider ii = ItemInformationProvider.getInstance();
        InventoryType type = ii.getInventoryType(itemid);
        Item item = getPlayer().getInventory(type).findById(itemid);
        String itemName = ItemInformationProvider.getInstance().getName(item.getItemId());
        int[] gacMap = {100000000, 101000000, 102000000, 103000000, 105040300, 800000000, 809000101, 809000201, 600000000, 120000000};
        String mapName = getClient().getPlayer().getMapName(gacMap[(getNpc() != 9100117 && getNpc() != 9100109) ? (getNpc() - 9100100) : getNpc() == 9100109 ? 8 : 9]);
        if (!rare) {
            getPlayer().getMap().broadcastMessage(PacketCreator.ServerNotice(2, getPlayer().getName() + " : gained a(n) " + itemName + " from the " + mapName + " Gachapon! Congrats!"));
        } else {
            BroadcastService.broadcastMessage(PacketCreator.ItemMegaphone(getPlayer().getName() + " :  gained a(n) item from " + mapName + " Gachapon! Congrats!", false, c.getChannel(), item));
        }
    }
    
    public void changeJobById(int a) { 
        getPlayer().changeJob(PlayerJob.getById(a)); 
    } 
    
    @Override
    public boolean isQuestCompleted(int quest) {
        try {
            return getQuestStatus(quest) == MapleQuestStatus.Status.COMPLETED;
        } catch (NullPointerException e) {
            return false;
        }
    }

    @Override
    public boolean isQuestStarted(int quest) {
        try {
            return getQuestStatus(quest) == MapleQuestStatus.Status.STARTED;
        } catch (NullPointerException e) {
            return false;
        }
    }

    @Override
    public int countMonster() {
        Field map = c.getPlayer().getMap();
        double range = Double.POSITIVE_INFINITY;
        List<FieldObject> monsters = map.getMapObjectsInRange(c.getPlayer().getPosition(), range, Arrays.asList(FieldObjectType.MONSTER));
        return monsters.size();
    }

    @Override
    public int countReactor() {
        Field map = c.getPlayer().getMap();
        double range = Double.POSITIVE_INFINITY;
        List<FieldObject> reactors = map.getMapObjectsInRange(c.getPlayer().getPosition(), range, Arrays.asList(FieldObjectType.REACTOR));
        return reactors.size();
    }

    public int getDayOfWeek() {
        Calendar cal = Calendar.getInstance();
        int day = cal.get(Calendar.DAY_OF_WEEK);
        return day;
    }

    public void giveNPCBuff(Player p, int itemID) {
        ItemInformationProvider mii = ItemInformationProvider.getInstance();
        MapleStatEffect statEffect = mii.getItemEffect(itemID);
        statEffect.applyTo(p);
    }

    public void reloadChar() {
        getPlayer().getClient().getSession().write(PacketCreator.GetCharInfo(getPlayer()));
        getPlayer().getMap().removePlayer(getPlayer());
        getPlayer().getMap().addPlayer(getPlayer());
    }

    public short gainItemRetPos(int itemid) {
        ItemInformationProvider ii = ItemInformationProvider.getInstance();
        Item it = ii.getEquipById(itemid);
        short ret = getPlayer().getInventory(InventoryType.EQUIP).addItem(it);
        c.getSession().write(PacketCreator.AddInventorySlot(InventoryType.EQUIP, it));
        c.getSession().write(PacketCreator.GetShowItemGain(itemid, (short)1, true));
        c.getSession().write(PacketCreator.EnableActions());
        return ret;
    }

    public void serverNotice(String msg) {
        getPlayer().sendServerNotice(msg);
    }  
	
    public boolean isPlayerInstance() {
        return c.getPlayer().getEventInstance() != null;
    }
    
    public int getMeso() {
        return getPlayer().getMeso();
    }
            
    public int getPlayersInMap(int mapId)  {                 
        return(getClient().getChannelServer().getMapFactory().getMap(mapId).getAllPlayer().size());         
    }
    public void showInventory(int type) {
        String send = "";
        Inventory invy = c.getPlayer().getInventory(InventoryType.getByType((byte)type));
        send = invy.list().stream().map((item) -> "#L" + item.getPosition() + "##v" + item.getItemId() + "# Quantity: #b" + item.getQuantity() + "#k#l\\r\\n").reduce(send, String::concat);
        sendSimple(send);
    }
	
    public String getInventory (int type) {
        String send = "";
        Inventory invy = c.getPlayer().getInventory(InventoryType.getByType((byte)type));
        send = invy.list().stream().map((item) -> "#L" + item.getPosition() + "##v" + item.getItemId() + "# Quantity: #b" + item.getQuantity() + "#k#l\\r\\n").reduce(send, String::concat);
        return send;
    }
	
    public Item getItem(int slot, int type) {
        Inventory invy = c.getPlayer().getInventory(InventoryType.getByType((byte)type));
        for (Item item : invy.list()) {
            if (item.getPosition() == slot) {
                return item;
            }
        }
        return null;
    }

    public int calcAvgLvl(int map) {
        int num = 0;
        int avg = 0;
        for (FieldObject mmo : c.getChannelServer().getMapFactory().getMap(map).getAllPlayer()) {
            avg += ((Player) mmo).getLevel();
            num++;
        }
        avg /= num;
        return avg;
    }

    public Player getChrById(int id) {
        ChannelServer cs = c.getChannelServer();
        return cs.getPlayerStorage().getCharacterById(id);
    }
    
    public Player getSender() {
        return this.p;
    }
    
    @Override
    public List<Player> getPartyMembers() {
        if (getPlayer().getParty() == null) {
            return null;
        }
        List<Player> chars = new LinkedList<>(); 
        getPlayer().getParty().getMembers().forEach((chr) -> {
            ChannelServer.getAllInstances().stream().map((channel) -> channel.getPlayerStorage().getCharacterById(chr.getId())).filter((ch) -> (ch != null)).forEachOrdered((ch) -> {
                chars.add(ch);
            });
        });
        return chars;
    }
    
    public boolean isUsingOldPqNpcStyle() {
        return GameConstants.USE_OLD_GMS_STYLED_PQ_NPCS && this.getPlayer().getParty() != null;
    }
	
    public int partyMembersInMap() {
        int inMap = 0;
        inMap = getPlayer().getMap().getCharacters().stream().filter((char2) -> (char2.getParty() == getPlayer().getParty())).map((_item) -> 1).reduce(inMap, Integer::sum);
        return inMap;
    }
        
    public void showFredrick() {
        getClient().getSession().write(InteractionPackets.GetFredrick(getPlayer(), 0x23));
    }
    
    public boolean hasMerchant() {
        return getPlayer().hasMerchant();
    }

    public boolean hasMerchantItems() {
        try {
            if (!ItemFactory.MERCHANT.loadItems(getPlayer().getId(), false).isEmpty()) {
                return true;
            }
        } catch (SQLException e) {
            return false;
        }
        return getPlayer().getMerchantMeso() != 0;
    }    
    
    public void closeDoor(int mapid){
        getClient().getChannelServer().getMapFactory().getMap(mapid).setReactorState();
    }

    public void openDoor(int mapid){
       getClient().getChannelServer().getMapFactory().getMap(mapid).resetReactors();
    }

    public void resetMap(int mapid) {
       getClient().getChannelServer().getMapFactory().getMap(mapid).resetReactors();
    }
    
    public void resetStats() {
        int totAp = getPlayer().getStat().getStr() + getPlayer().getStat().getDex() + getPlayer().getStat().getLuk() + getPlayer().getStat().getInt() + getPlayer().getStat().getRemainingAp();
        getPlayer().getStat().setStr(4);
        getPlayer().getStat().setDex(4);
        getPlayer().getStat().setLuk(4);
        getPlayer().getStat().setInt(4);
        getPlayer().getStat().setRemainingAp(totAp - 16);
        getPlayer().getStat().updateSingleStat(PlayerStat.STR, 4);
        getPlayer().getStat().updateSingleStat(PlayerStat.DEX, 4);
        getPlayer().getStat().updateSingleStat(PlayerStat.LUK, 4);
        getPlayer().getStat().updateSingleStat(PlayerStat.INT, 4);
        getPlayer().getStat().updateSingleStat(PlayerStat.AVAILABLEAP, totAp);
    }
    
    public boolean isMorphed() {
        boolean morph = false;
        Integer morphed = getPlayer().getBuffedValue(BuffStat.MORPH);
        if (morphed != null) {
            morph = true;
        }
        return morph;
    }
        
    public int getMorphValue() {
        try {
            int morphid = getPlayer().getBuffedValue(BuffStat.MORPH).intValue();
            return morphid;
        } catch (NullPointerException n) {
            return -1;
        }
    }
    
    
    public final void sendRPS() {
        c.getSession().write(PacketCreator.GetRockPaperScissorsMode((byte) 8, -1, -1, -1));
    }
   
    public void displayDrops(int selection) {
        if (selection < 1) {
            sendOk("No monster with that name.");
            dispose();
            return;
        }
        MapleMonster job = MapleLifeFactory.getMonster(selection);
        String text = "";
        List ranks = MapleMonsterInformationProvider.getInstance().retrieveDrop(job.getId());
        if ((ranks == null) || (ranks.size() <= 0)) {
            sendOk("No drop was found.");
        } else {
            int num = 0;
            ItemInformationProvider ii = ItemInformationProvider.getInstance();
            for (int i = 0; i < ranks.size(); i++) {
                if ((i >= 1) && (i < ranks.size())) {
                    MonsterDropEntry de = (MonsterDropEntry)ranks.get(i);
                    String name = ii.getName(de.itemId);
                    if ((de.chance > 0) && (name != null) && (name.length() > 0) && ((de.questid <= 0) || ((de.questid > 0) && (MapleQuest.getInstance(de.questid).getName().length() > 0)))) {
                        if (num == 0) {
                            text = new StringBuilder().append(text).append("Drops monster #e").append(job.getStats().getName()).append("#n:\r\n").toString();
                            text = new StringBuilder().append(text).append("----------------------------------------------\r\n").toString();
                        }
                        double percent = 0.0D;
                        percent = Integer.valueOf(de.chance == 999999 ? 1000000 : de.chance).doubleValue() / 10000.0D * ChannelServer.getInstance(c.getChannel()).getExpRate();
                        if (percent >= 100.0D) {
                            percent = 100.0D;
                        }
                        String quantity = new StringBuilder().append("anywhere from ").append(de.Minimum).append("to ").append(de.Maximum).append(" quantity.").toString();
                        if (quantity.equals("em qualquer lugar a partir de 1 até 1.") || quantity.equals("anywhere from 1 to 1 quantity.")) {
                            text = new StringBuilder().append(text).append("#i").append(de.itemId).append("# ").append(percent).append("% chance.\r\n").append((de.questid > 0) && (MapleQuest.getInstance(de.questid).getName().length() > 0) ? new StringBuilder().append("#eQuest Req#n: ").append(MapleQuest.getInstance(de.questid).getName()).append(" to be started.\r\n").toString() : "\r\n").toString();
                        } else {
                          text = new StringBuilder().append(text).append("#i").append(de.itemId).append("# anywhere from ").append(de.Minimum).append(" to ").append(de.Maximum).append(", with ").append(percent).append("% chance.\r\n").append((de.questid > 0) && (MapleQuest.getInstance(de.questid).getName().length() > 0) ? new StringBuilder().append("#eQuest Req#n:").append(MapleQuest.getInstance(de.questid).getName()).append(" to be started.").toString() : "\r\n").toString();
                        }
                        num++;
                    }
                    if (num == 0) {
                        sendOk("No drop found.");
                    }
                }
            }
            List <MonsterDropEntry> dropEntry = new ArrayList(MapleMonsterInformationProvider.getInstance().retrieveDrop(selection));
            int fff = 0;
            int itemid = 0;
            double percent = 0.0D;
            if (fff == 0) {
                for (MonsterDropEntry dr : dropEntry) {
                    if (dr.itemId == 0) {
                        if (dr.Minimum * ChannelServer.getInstance(c.getChannel()).getMesoRate() < 99) {
                            itemid = 4031039;
                        } else if ((dr.Minimum * ChannelServer.getInstance(c.getChannel()).getMesoRate() > 100) && (dr.Minimum * ChannelServer.getInstance(c.getChannel()).getMesoRate() < 999)) {
                            itemid = 4031040;
                        } else if (dr.Minimum * ChannelServer.getInstance(c.getChannel()).getMesoRate() > 999) {
                            itemid = 4031041;
                        }
                        percent = Integer.valueOf(dr.chance == 999999 ? 1000000 : dr.chance * ChannelServer.getInstance(c.getChannel()).getDropRate()).doubleValue() / 10000.0D;
                        if (percent >= 100.0D) {
                            percent = 100.0D;
                        }
                        text = new StringBuilder().append(text).append("#i ").append(itemid).append("# anywhere from ").append(dr.Minimum * ChannelServer.getInstance(c.getChannel()).getDropRate()).append(" to ").append(dr.Maximum * ChannelServer.getInstance(c.getChannel()).getMesoRate()).append(", ").append(percent).append("% chance.").toString();
                        fff = 1;
                    }
                }
            }
            sendOk(text);
        }
    }
    
    public void searchToItemId(String message) {
        int itemid = Integer.parseInt(message);
        System.out.println("item : " + itemid);
        if (itemid == 0 || itemid < 0) {
            sendOk("You must place an item number.");
            dispose();
            return;
        } 
        try {
            String msg = "";
            List<String> retMobs = new ArrayList<>();
            MapleData data = null;
            MapleDataProvider dataProvider = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/" + "String"));
            data = dataProvider.getData("Mob.img");
            
            String nameItem = ItemInformationProvider.getInstance().getName(itemid);
            
            msg = new StringBuilder().append("#eItem ID#n: ").append(itemid).append("\r\n#eItem name#n: ").append(nameItem).append("\r\nIt is dropped by monsters:\r\n").toString();
            msg = new StringBuilder().append(msg).append("----------------------------------------------\r\n").toString();
            List<Pair<Integer,String>> mobPairList = new LinkedList<>();
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT dropperid FROM drop_data WHERE itemid = ?");
            ps.setInt(1, itemid);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int mobn = rs.getInt("dropperid");
                for (MapleData mobIdData : data.getChildren()) {
                    int mobIdFromData = Integer.parseInt(mobIdData.getName());
                    String mobNameFromData = MapleDataTool.getString(mobIdData.getChildByPath("name"), "NO-NAME");
                    mobPairList.add(new Pair<>(mobIdFromData, mobNameFromData));
                }
                for (Pair<Integer, String> mobPair : mobPairList) {
                    if (mobPair.getLeft() == (mobn) && !retMobs.contains(mobPair.getRight())) {
                        retMobs.add(mobPair.getRight());
                    }
                }

            }
            rs.close();
            ps.close();
            if (retMobs != null && retMobs.size() > 0) {
                int num = 1;
                for (String singleRetMob : retMobs) {
                    msg = new StringBuilder().append(msg).append("[").append(num).append("] ").append(singleRetMob).append("\r\n").toString();
                    num++;
                }
                sendSimple(msg);  
            } else {
                sendOk("No monster drops this item.");
            }
        } catch (SQLException e) {}
    } 
    
    public void searchToNameMob(String name) {
        if (name.equals("")) {
            sendOk("You need to put the name!");
            dispose();
            return;
        }
        MapleData data = null;
        String msg = "";
        MapleDataProvider dataProvider = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/" + "String"));
        List<String> retMobs = new ArrayList<>();
        data = dataProvider.getData("Mob.img");
        List<Pair<Integer, String>> mobPairList = new LinkedList<>();
        for (MapleData mobIdData : data.getChildren()) {
            mobPairList.add(new Pair<>(Integer.parseInt(mobIdData.getName()), MapleDataTool.getString(mobIdData.getChildByPath("name"), "NO-NAME")));
        }
        msg = new StringBuilder().append(msg).append("Name of monsters found:\r\n").toString();
        for (Pair<Integer, String> mobPair : mobPairList) {
            if (mobPair.getRight().toLowerCase().contains(name.toLowerCase())) {
                retMobs.add("#eID#n: " + mobPair.getLeft() + " | " + ("#eMonster Name#n: ") + mobPair.getRight());
            }
        }
        if (retMobs != null && retMobs.size() > 0) {
            int num = 1;
            for (String singleRetMob : retMobs) {
                msg = new StringBuilder().append(msg).append("[").append(num).append("] ").append(singleRetMob).append("\r\n").toString();
                num++;
            }
           sendSimple(msg);  
        } else {
            sendOk("No monster found!");
        }
    }
    
    public void searchToNameItem(String name) {
        if (name.equals("")) {
            sendOk("You need to put the name!");
            dispose();
            return;
        }
        String msg = "";
        List<String> retItems = new ArrayList<>();
        ItemInformationProvider miip = ItemInformationProvider.getInstance();
        msg = new StringBuilder().append(msg).append("Name of found items:\r\n").toString();
        for (Map.Entry<Integer, String> itemEntry : miip.getAllItems().entrySet()) {    
            if (itemEntry.getValue().toLowerCase().contains(name.toLowerCase())) {
                int id = itemEntry.getKey();
                retItems.add("#eID#n: " + id + " | " + ("#eItem Name#n: ") + miip.getName(id));
            }
        }
        if (retItems != null && retItems.size() > 0) {
            int num = 1;
            for (String singleRetItem : retItems) {
                msg = new StringBuilder().append(msg).append("[").append(num).append("] ").append(singleRetItem).append("\r\n").toString();
                num++;
            }
          sendSimple(msg);   
        } else {
            sendOk("No items found!");
        }
    }
    
    public void listMonsters() {
        String monster = "";
        String text = "Select the monster:#b\r\n";
        ArrayList monstersInMap = new ArrayList();
        List<FieldObject> monsters = getPlayer().getMap().getMapObjectsInRange(this.c.getPlayer().getPosition(), (1.0D / 0.0D), Arrays.asList(new FieldObjectType[]{FieldObjectType.MONSTER}));
        for (FieldObject curmob : monsters) {
            MapleMonster monsterlist = null;
            if (monsterlist == null) {
                monsterlist = (MapleMonster) curmob;
            }
            if (!monstersInMap.contains(Integer.valueOf(monsterlist.getId()))) {
                monstersInMap.add(Integer.valueOf(monsterlist.getId()));
            }
        }

        for (int i = 0; i < monstersInMap.size(); i++) {
            monster = MapleLifeFactory.getMonster(((Integer) monstersInMap.get(i)).intValue()).getName();
            text = new StringBuilder().append(text).append("#L").append(monstersInMap.get(i)).append("#").append(monster).append("#l\r\n").toString();
        }

        if (monster.isEmpty()) {
            sendOk("No monster found!");
            dispose();
        } else {
          sendSimple(text);
        }
    }
    
    private int getMobsIDFromName(String search) {
        MapleDataProvider dataProvider = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/" + "String"));
        MapleData data = dataProvider.getData("Mob.img");
        List<Pair<Integer, String>> mobPairList = new LinkedList<>();
        for (MapleData mobIdData : data.getChildren()) {
            int mobIdFromData = Integer.parseInt(mobIdData.getName());
            String mobNameFromData = MapleDataTool.getString(mobIdData.getChildByPath("name"), "NO-NAME");
            mobPairList.add(new Pair<>(mobIdFromData, mobNameFromData));
        }
        for (Pair<Integer, String> mobPair : mobPairList) {
            if (mobPair.getRight().toLowerCase().equals(search.toLowerCase()) && mobPair.getLeft() > 0) {
                
                return mobPair.getLeft();
            }
        }
        return 0;
    }
    
    public int getMobId(String mobname) {
        return getMobsIDFromName(mobname);
    }
}
