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

package handling.channel;

import client.player.Player;
import constants.ServerProperties;
import database.DatabaseConnection;
import handling.ServerHandler;
import handling.login.LoginServer;
import community.MapleParty;
import community.MaplePartyCharacter;
import constants.GameConstants;
import handling.world.CheaterData;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.buffer.SimpleBufferAllocator;
import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.SocketSessionConfig;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import packet.creators.PacketCreator;
import packet.crypto.MapleCodecFactory;
import packet.transfer.write.OutPacket;
import provider.MapleDataProviderFactory;
import scripting.event.EventScriptManager;
import server.expeditions.MapleExpedition;
import server.expeditions.MapleExpeditionType;
import server.maps.FieldManager;
import server.maps.object.AbstractMapleFieldObject;
import server.minirooms.Merchant;
import server.minirooms.PlayerShop;
import server.minirooms.PlayerShopItem;
import server.transitions.AirPlane;
import server.transitions.Boats;
import server.transitions.Cabin;
import server.transitions.Elevator;
import server.transitions.Genie;
import server.transitions.Subway;
import server.transitions.Trains;
import tools.CollectionUtil;
import tools.Pair;
import tools.locks.MonitoredLockType;
import tools.locks.MonitoredReentrantLock;

public class ChannelServer  {
    private final int channel;
    public int expRate;
    public int mesoRate;
    public int dropRate;
    public int bossDropRate;
    public int petExpRate;
    public int mountExpRate;
    public int questExpRate;
    public int eventMap = 0;
    public boolean eventOn = false;
    private static boolean shutdown = false;
    private static boolean finishedShutdown = false;
    private short port = 7575;
    private String ip;
    private String arrayString = "";
    private PlayerStorage players;
    private String serverMessage;
    private IoAcceptor acceptor;
    private EventScriptManager eventSM;
    private final FieldManager mapFactory;
    private List<MapleExpedition> expeditions = new ArrayList<>();
    private List<MapleExpeditionType> expedType = new ArrayList<>();
    private final Map<Integer, String> names = new LinkedHashMap<>();
    private static final Map<Integer, ChannelServer> instances = new HashMap<>();
    private ConcurrentHashMap<Integer, Integer> mostSearchedItem = new ConcurrentHashMap<>();
    private ConcurrentLinkedQueue<Integer> topResults = new ConcurrentLinkedQueue<>();
    
    private Map<Integer, Integer> owlSearched = new LinkedHashMap<>();
    private Lock owlLock = new MonitoredReentrantLock(MonitoredLockType.WORLD_OWL);
    
    private final Map<Integer, Merchant> minirooms = new HashMap<>();
    private Lock activeMerchantsLock = new MonitoredReentrantLock(MonitoredLockType.WORLD_MERCHS, true);
    
    private Lock activePlayerShopsLock = new MonitoredReentrantLock(MonitoredLockType.WORLD_PSHOPS, true);
    private Map<Integer, PlayerShop> activePlayerShops = new LinkedHashMap<>();

    private ChannelServer(final int channel) {
        this.channel = channel;
        this.mapFactory = new FieldManager(null, MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/Map")), MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/String")), channel);
    }
   
    public static Set<Integer> getAllInstance() {
        return new HashSet<>(instances.keySet());
    }

    public final void runStartupConfigurations() {
        setChannel(channel);
        try {
            expRate = ServerProperties.World.EXP;
            questExpRate = ServerProperties.World.QUEST_EXP;
            mesoRate = ServerProperties.World.MESO;
            dropRate = ServerProperties.World.DROP;
            bossDropRate = ServerProperties.World.BOSS_DROP;
            petExpRate = ServerProperties.World.PET_EXP;
            mountExpRate = ServerProperties.World.MOUNT_EXP;
            serverMessage = ServerProperties.World.SERVER_MESSAGE;
            eventSM = new EventScriptManager(this, ServerProperties.Channel.EVENTS.split(","));
            port = Short.parseShort(String.valueOf(port + channel));
        } catch (NumberFormatException e) {
            throw new RuntimeException(e);
        }
        
        ip = ServerProperties.World.HOST + ":" + port;
        try {
        IoBuffer.setUseDirectBuffer(false);
        IoBuffer.setAllocator(new SimpleBufferAllocator());

        players = new PlayerStorage(channel);
        acceptor = new NioSocketAcceptor();

        loadTransitions(this);
        loadSearchedItems();

        for (MapleExpeditionType exped : MapleExpeditionType.values()) {
            expedType.add(exped);
        }

        acceptor.setHandler(new ServerHandler(channel));
        acceptor.getSessionConfig().setIdleTime(IdleStatus.BOTH_IDLE, 30);
        acceptor.getFilterChain().addLast("codec", (IoFilter) new ProtocolCodecFilter(new MapleCodecFactory()));
        acceptor.bind(new InetSocketAddress(port));
        ((SocketSessionConfig) acceptor.getSessionConfig()).setTcpNoDelay(true);
        
        eventSM.init();   
                             
        } catch (IOException e) {
            e.printStackTrace();
        }
    } 
    
    public static final void startChannelMain() {
        for (int i = 0; i < ServerProperties.Channel.COUNT; i++) {
            newInstance(i + 1).runStartupConfigurations();
        }
    }
        
    public static Map<Integer, Integer> getChannelLoad() {
        Map<Integer, Integer> ret = new HashMap<>();
        for (ChannelServer cs : instances.values()) {
            ret.put(cs.getChannel(), cs.getConnectedClients());
        }
        return ret;
    }
    
    public final void setShutdown() {
        this.shutdown = true;
        saveSearchedItems();
        System.out.println("Canal " + channel + " está fechando e os comerciantes...");
    }

    public final void setFinishShutdown() {
        this.finishedShutdown = true;
        System.out.println("Canal " + channel + " desligamento concluido.");
    }

    public final boolean hasFinishedShutdown() {
        return finishedShutdown;
    }
   
    public final int getChannelId() {
        return channel;
    }
    
    public final void closeAllMerchant() {
        activeMerchantsLock.lock();
        try {
            final Iterator<Merchant> merchants = minirooms.values().iterator();
            while (merchants.hasNext()) {
                merchants.next().forceClose();
                merchants.remove();
            }
        } finally {
            activeMerchantsLock.unlock();
        }
    }
    
    public Map<Integer, Merchant> getHiredMerchants() {
        return minirooms;
    }

    public void addHiredMerchant(int chrid, Merchant hm) {
        activeMerchantsLock.lock();
        try {
            minirooms.put(chrid, hm);
        } finally {
            activeMerchantsLock.unlock();
        }
    }

    public void removeHiredMerchant(int chrid) {
        activeMerchantsLock.lock();
        try {        
            minirooms.remove(chrid);
        } finally {
           activeMerchantsLock.unlock();
        }
    }
    
    public Merchant getHiredMerchant(int ownerid) {
        activeMerchantsLock.lock();
        try {
            return minirooms.get(ownerid);
        } finally {
            activeMerchantsLock.unlock();
        }
    }
    
    public List<Merchant> getActiveMerchants() {
        List<Merchant> hmList = new ArrayList<>();
        activeMerchantsLock.lock();
        try {
            for (Merchant hmp : minirooms.values()) {
                if (hmp.isOpen()) {
                    hmList.add(hmp);
                }
            }
            return hmList;
        } finally {
            activeMerchantsLock.unlock();
        }
    }
    
    public void registerPlayerShop(PlayerShop ps) {
        activePlayerShopsLock.lock();
        try {
            activePlayerShops.put(ps.getOwner().getId(), ps);
        } finally {
            activePlayerShopsLock.unlock();
        }
    }
    
    public void unregisterPlayerShop(PlayerShop ps) {
        activePlayerShopsLock.lock();
        try {
            activePlayerShops.remove(ps.getOwner().getId());
        } finally {
            activePlayerShopsLock.unlock();
        }
    }
    
    public List<PlayerShop> getActivePlayerShops() {
        List<PlayerShop> psList = new ArrayList<>();
        activePlayerShopsLock.lock();
        try {
            for(PlayerShop mps : activePlayerShops.values()) {
                psList.add(mps);
            }
            
            return psList;
        } finally {
            activePlayerShopsLock.unlock();
        }
    }
    
    public PlayerShop getPlayerShop(int ownerid) {
        activePlayerShopsLock.lock();
        try {
            return activePlayerShops.get(ownerid);
        } finally {
            activePlayerShopsLock.unlock();
        }
    }
     
    public static void loadTransitions(ChannelServer channel) {
        try {
            AirPlane airPlane = new AirPlane();
            airPlane.Start(channel);
            
            Boats boats = new Boats();
            boats.Start(channel);

            Cabin cabine = new Cabin();
            cabine.Start(channel);

            Elevator elevator = new Elevator();
            elevator.Start(channel);

            Genie genie = new Genie();
            genie.Start(channel);

            Subway subway = new Subway();
            subway.Start(channel);

            Trains trains = new Trains();
            trains.Start(channel);

        } catch (Exception ex) {
            System.err.println(ex);
        }
    }
    
    public EventScriptManager getEventSM() {
        return eventSM;
    }
    
    public void reloadEvents() {
        eventSM.cancel();
        eventSM = null;
        eventSM = new EventScriptManager(this, getEvents());
        eventSM.init();
    }
    
    private static String [] getEvents(){
    	List<String> events = new ArrayList<>();
    	for (File file : new File(ServerProperties.Misc.DATA_ROOT + "/Script/event").listFiles()){
            events.add(file.getName().substring(0, file.getName().length() - 3));
    	}
    	return events.toArray(new String[0]);
    }
                  
    public List<Player> getPartyMembers(MapleParty party) {
        List<Player> partym = new ArrayList<>(8);
        for (MaplePartyCharacter partychar : party.getMembers()) {
            if (partychar.getChannel() == getChannelId()) {
                Player chr = getPlayerStorage().getCharacterByName(partychar.getName());
                if (chr != null) {
                    partym.add(chr);
                }
            }
        }
        return partym;
    }
       
    public boolean isConnected(String name) {
        return getPlayerStorage().getCharacterByName(name) != null;
    }   
      
    public String getCharacterName(int charId) {
        if (names.get(charId) != null) return names.get(charId);
        for (ChannelServer cs : ChannelServer.getAllInstances()) {
            for (Player mc : cs.getPlayerStorage().getAllCharacters()) {
                if (mc.getId() == charId) {
                    names.put(charId, mc.getName());
                    return mc.getName();
                }
            }
        }
        Connection con = DatabaseConnection.getConnection();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT * FROM characters WHERE id = ?");
            ps.setInt(1, charId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String name = rs.getString("name");
                names.put(charId, name);
                return name;
            }
        } catch (SQLException err) {
            return "SQL Error: " + err;
        }
        return "No user";
    } 
    
    public int find(String name) {
        int channel = -1;
        Player chr = getPlayerStorage().getCharacterByName(name);
        if (chr != null) {
            channel = chr.getClient().getChannel();
        }
        return channel;
    }
    
    
    public void broadcastMessage(OutPacket message) {
        broadcastPacket(message);
    }

    public void broadcastSmega(OutPacket message) {
        broadcastSmegaPacket(message);
    }

    public void broadcastGMMessage(OutPacket message) {
        broadcastGMPacket(message);
    }
    
    public final void broadcastPacket(final OutPacket data) {
        for (Player chr : players.getAllCharacters()) {
            chr.announce(data);
        }
    }

    public final void broadcastSmegaPacket(final OutPacket data) {
        for (Player chr : players.getAllCharacters()) {
            if (chr.getSmegaEnabled()) {
                chr.announce(data);
            }
        }
    }
    
    public final void broadcastGMPacket(final OutPacket data) {
        for (Player chr : players.getAllCharacters()) {
            if (chr.isGameMaster()) {
                chr.announce(data);
            }
        }
    }
    
    public void broadcastYellowMessage(String msg) {
        for (Player mc : getPlayerStorage().getAllCharacters()) {
            mc.announce(PacketCreator.SendYellowTip(msg));
        }
    }
     
    public void worldMessage(String msg) {
        for (Player mc : getPlayerStorage().getAllCharacters()) {
            mc.dropMessage(msg);
        }
    }

    public List<CheaterData> getCheaters() {
        List<CheaterData> cheaters = getPlayerStorage().getCheaters();

        Collections.sort(cheaters);
        return CollectionUtil.copyFirst(cheaters, 20);
    }

    public final void shutdown() {
        if (finishedShutdown) {
            return;
        }
        
        broadcastPacket(PacketCreator.ServerNotice(0, "Este canal será encerrado agora."));
        shutdown = true;

        System.out.println("Canal " + channel + ", salvando comerciantes...");
        closeAllMerchant(); 

        System.out.println("Canal " + channel + ", salvando personagens...");

        getPlayerStorage().disconnectAll();

        System.out.println("Canal " + channel + ", desvinculado...");

        acceptor.unbind();
        acceptor = null;

        instances.remove(channel);
        LoginServer.removeChannel(channel);
        setFinishShutdown();
    }
	
    public void unBind() {
        acceptor.unbind();
    }
    
    public FieldManager getMapFactory() {
        return mapFactory;
    }
  
    public static final ChannelServer newInstance(final int channel) {
        return new ChannelServer(channel);
    }
   
    public static final ChannelServer getInstance(final int channel) {
        return instances.get(channel);
    }
    
    public final void addPlayer(final Player p) {
        getPlayerStorage().registerPlayer(p);
        broadcastPacket(PacketCreator.ServerMessage(serverMessage));
    }
    
    public final void removePlayer(final Player chr) {
	getPlayerStorage().deregisterPlayer(chr);
    }
    
    public final void removePlayer(final int idz, final String namez) {
        getPlayerStorage().deregisterPlayer(idz, namez);
    }
    
    public final PlayerStorage getPlayerStorage() {
        if (players == null) {
            players = new PlayerStorage(channel);
        }
        return players;
    }

    public int getConnectedClients() {
        return  getPlayerStorage().getAllCharacters().size();
    }
	
    public void setServerMessage(String newMessage) {
        serverMessage = newMessage;
        broadcastPacket(PacketCreator.ServerMessage(serverMessage));
    }

    public String getArrayString() {
        return arrayString;
    }

    public void setArrayString(String newStr) {
        arrayString = newStr;
    }

    public int getChannel() {
        return channel;
    }

    public final void setChannel(final int channel) {
        instances.put(channel, this);
        LoginServer.addChannel(channel);
    }

    public static Collection<ChannelServer> getAllInstances() {
        return Collections.unmodifiableCollection(instances.values());
    }

    public String getIP() {
	return ip;
    }

    public boolean isShutdown() {
        return shutdown;
    }
    
    public boolean getEventStarted() {
        return eventOn;
    }
    
    public void setEvent(boolean set) {
        this.eventOn = set;
    }
    
    public int getEventMap() {
        return eventMap;
    }
    
    public void setEventMap(int map) {
        this.eventMap = map;
    }
          	
    public int getLoadedMaps() {
        return mapFactory.getLoadedMapSize();
    }

    public int getExpRate() {
        return expRate;
    }

    public void setExpRate(int expRate) {
        this.expRate = expRate;
    }
   
    public int getQuestRate() {
        return questExpRate;
    }

    public void setQuestRate(int QuestExpRate) {
        this.questExpRate = QuestExpRate;
    }
	
    public int getMesoRate() {
        return mesoRate;
    }

    public void setMesoRate(int mesoRate) {
        this.mesoRate = mesoRate;
    }

    public int getDropRate() {
        return dropRate;
    }

    public void setDropRate(int dropRate) {
        this.dropRate = dropRate;
    }

    public int getBossDropRate() {
        return bossDropRate;
    }

    public void setBossDropRate(int bossdropRate) {
        this.bossDropRate = bossdropRate;
    }

    public int getPetExpRate() {
        return petExpRate;
    }

    public void setPetExpRate(int petExpRate) {
        this.petExpRate = petExpRate;
    }

    public int getMountRate() {
        return mountExpRate;
    }

    public void setMountRate(int mountExpRate) {
        this.mountExpRate = mountExpRate;
    }

    /**
     * This will return the collection of items that has been searched through the
     * item Owl of Minerva
     * 
     * @return the Set<Entry<Integer, Integer>> mostSearchedItem
    */
    public Set<Map.Entry<Integer, Integer>> getMostSearchedItem() {
        return mostSearchedItem.entrySet();
    }

    /**
     * This will return the top 10 items that have been sought after with owl
     * 
     * @return the ArrayListy<Integer> topResults
    */
    public ConcurrentLinkedQueue<Integer> retrieveTopResults() {
        return topResults;
    }
	
    public void checkSearchedItems(int itemId) {
        loadSearchedItems();
    	if (!mostSearchedItem.contains(itemId)) {
            mostSearchedItem.put(itemId, 1);
            insertSearchedItem(itemId);
    	} else {
            int count = mostSearchedItem.get(itemId);
            mostSearchedItem.put(itemId, count++);
    	}
    	updateTopItemSearchResults();
    }
	
	
    /**
     * This will clear the current top 10 items 
     * that were searched through Owl of Minerva 
     * 
    */
    public void updateTopItemSearchResults() {
        topResults.clear(); 
        ArrayList<Map.Entry<Integer, Integer>> entries = new ArrayList<>(mostSearchedItem.entrySet());
        Collections.sort(entries, (o1, o2) -> { 
            Map.Entry<Integer, Integer> entry1 = o1;
            Map.Entry<Integer, Integer> entry2 = o2;
            if (entry1.getValue() > entry2.getValue()) {
                    return -1;
            }
            if (entry1.getValue() < entry2.getValue()) {
                    return 1;
            }
            return 0;
        });
        int i = 0;
        for (Map.Entry<Integer, Integer> item : entries) { 
            int itemId = item.getKey();
            topResults.add(itemId);
            i++;
            if (i == 9) {
                    break;
            }
        }
    }
	
    /**
     * This loads all the items that was searched with Owl of Minerva
     * from the database
     * 
    */
    public void loadSearchedItems() {
        Connection con = DatabaseConnection.getConnection();
        try {
            try (PreparedStatement ps = con.prepareStatement("SELECT * FROM topresults WHERE world = ?")) {
                ps.setInt(1, 1); // Todo world
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int itemid = rs.getInt("itemid");
                        int count = rs.getInt("count");
                        mostSearchedItem.put(itemid, count);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println(e);
        }
    }

    /**
     * This saves all the items that was searched with Owl of Minerva
     * to the database
     * 
    */
    public void saveSearchedItems() {
        Connection con = DatabaseConnection.getConnection();
        try {
            try (PreparedStatement ps = con.prepareStatement("UPDATE topresults SET count = ? WHERE world = ? and itemid = ?")) {
                for (Map.Entry<Integer, Integer> itemSearched : mostSearchedItem.entrySet()) {
                    ps.setInt(1, itemSearched.getValue());
                    ps.setInt(2, 1);  // Todo world
                    ps.setInt(3, itemSearched.getKey());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        } catch (SQLException e) {
            System.err.println(e);
        }
    }
    
    /**
     * This inserts a new searched item that was searched with Owl of Minerva
     * to the database
     * 
     * @param itemId
    */
    public void insertSearchedItem(int itemId) {
        Connection con = DatabaseConnection.getConnection();
        try {
            try (PreparedStatement ps = con.prepareStatement("INSERT INTO topresults (itemid, count, world) VALUES (?, ?, ?)")) {
                ps.setInt(1, itemId);
                ps.setInt(2, 1);
                ps.setInt(3, 1); // Todo world
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println(e);
        }
    }

    public void reloadEventScriptManager(){
    	eventSM.cancel();
    	eventSM = null;
    	eventSM = new EventScriptManager(this, getEvents());
    	eventSM.init();
    }

    public List<MapleExpedition> getExpeditions() {
    	return expeditions;
    }

    public void addOwlItemSearch(Integer itemid) {
        owlLock.lock();
        try {
            Integer cur = owlSearched.get(itemid);
            if(cur != null) {
                owlSearched.put(itemid, cur + 1);
            } else {
                owlSearched.put(itemid, 1);
            }
        } finally {
            owlLock.unlock();
        }
    }
    
    public List<Pair<Integer, Integer>> getOwlSearchedItems() {
        if (GameConstants.USE_ENFORCE_OWL_SUGGESTIONS) {
            return new ArrayList<>(0);
        }
        
        owlLock.lock();
        try {
            List<Pair<Integer, Integer>> searchCounts = new ArrayList<>(owlSearched.size());
            
            for (Map.Entry<Integer, Integer> e : owlSearched.entrySet()) {
                searchCounts.add(new Pair<>(e.getKey(), e.getValue()));
            }
            
            return searchCounts;
        } finally {
            owlLock.unlock();
        }
    }

    public List<Pair<PlayerShopItem, AbstractMapleFieldObject>> getAvailableItemBundles(int itemid) {
        List<Pair<PlayerShopItem, AbstractMapleFieldObject>> hmsAvailable = new ArrayList<>();

        for (Merchant hm : getActiveMerchants()) {
            List<PlayerShopItem> itemBundles = hm.sendAvailableBundles(itemid);

            for(PlayerShopItem mpsi : itemBundles) {
                hmsAvailable.add(new Pair<>(mpsi, (AbstractMapleFieldObject) hm));
            }
        }

        for (PlayerShop ps : getActivePlayerShops()) {
            List<PlayerShopItem> itemBundles = ps.sendAvailableBundles(itemid);

            for(PlayerShopItem mpsi : itemBundles) {
                hmsAvailable.add(new Pair<>(mpsi, (AbstractMapleFieldObject) ps));
            }
        }

        Collections.sort(hmsAvailable, (Pair<PlayerShopItem, AbstractMapleFieldObject> p1, Pair<PlayerShopItem, AbstractMapleFieldObject> p2) -> p1.getLeft().getPrice() - p2.getLeft().getPrice());

        hmsAvailable.subList(0, Math.min(hmsAvailable.size(), 200));    
        return hmsAvailable;
    }
}