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

package server.maps;

import constants.MapConstants;
import server.maps.reactors.Reactor;
import server.maps.reactors.ReactorFactory;
import server.maps.portal.PortalFactory;
import java.awt.Point;
import java.awt.Rectangle;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import java.util.Map.Entry;
import database.DatabaseConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import provider.MapleData;
import provider.MapleDataProvider;
import provider.MapleDataTool;
import scripting.event.EventInstanceManager;
import server.life.AbstractLoadedMapleLife;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.life.npc.MapleNPC;
import tools.StringUtil;
import server.partyquest.mcpq.MCWZData;
import tools.locks.MonitoredLockType;
import tools.locks.MonitoredReentrantReadWriteLock;

public class FieldManager {

    private MapleDataProvider source;
    private MapleData nameData;
    private EventInstanceManager event;
    private Map<Integer, Field> maps = new HashMap<>();
    private ReentrantReadWriteLock.ReadLock mapsRLock;
    private ReentrantReadWriteLock.WriteLock mapsWLock;
    private int channel;
    
    public FieldManager(EventInstanceManager eim, MapleDataProvider source, MapleDataProvider stringSource, int channel) {
        this.source = source;
        this.nameData = stringSource.getData("Map.img");
        this.channel = channel;
        this.event = eim;
        
        ReentrantReadWriteLock rrwl = new MonitoredReentrantReadWriteLock(MonitoredLockType.MAP_FACTORY);
        this.mapsRLock = rrwl.readLock();
        this.mapsWLock = rrwl.writeLock();
    }
    
    public Field resetMap(int mapid) {
        mapsWLock.lock();
        try {
            maps.remove(Integer.valueOf(mapid));
        } finally {
            mapsWLock.unlock();
        }
        
        return getMap(mapid);
    }
    
    public Field loadMapFromWz(int mapid, Integer omapid) {
        return loadMapFromWz(mapid, omapid, true, true, true);
    }

    public Field loadMapFromWz(int mapid, Integer omapid, boolean respawns, boolean npcs) {
        return loadMapFromWz(mapid, omapid, respawns, npcs, true);
    }

    private synchronized Field loadMapFromWz(int mapid, Integer omapid, boolean respawns, boolean npcs, boolean reactors) {
        Field map;
        
        mapsRLock.lock();
        try {
            map = maps.get(omapid);
        } finally {
            mapsRLock.unlock();
        }

        if (map != null) {
            return map;
        }
        
        String mapName = getMapName(mapid);
        MapleData mapData = source.getData(mapName);
        if (mapData == null) {
            return null;
        }

        float monsterRate = 0;
        if (respawns) {
            MapleData mobRate = mapData.getChildByPath("info/mobRate");
            if (mobRate != null) {
                monsterRate = (float) mobRate.getData();
            }
        }
        map = new Field(mapid, channel, MapleDataTool.getInt("info/returnMap", mapData), monsterRate);
        map.setEventInstance(event);

        map.setFieldLimit(MapleDataTool.getInt(mapData.getChildByPath("info/fieldLimit"), 0));

        PortalFactory portalFactory = new PortalFactory();
        for (MapleData portal : mapData.getChildByPath("portal")) {
            map.addPortal(portalFactory.makePortal(MapleDataTool.getInt(portal.getChildByPath("pt")), portal));
        }
        List<MapleFoothold> allFootholds = new LinkedList<>();
        Point lBound = new Point();
        Point uBound = new Point();
        int swim = MapleDataTool.getIntConvert("swim", mapData, 0);
        map.setSwim(swim > 0);

        for (MapleData footRoot : mapData.getChildByPath("foothold")) {
            for (MapleData footCat : footRoot) {
                for (MapleData footHold : footCat) {
                    int x1 = MapleDataTool.getInt(footHold.getChildByPath("x1"));
                    int y1 = MapleDataTool.getInt(footHold.getChildByPath("y1"));
                    int x2 = MapleDataTool.getInt(footHold.getChildByPath("x2"));
                    int y2 = MapleDataTool.getInt(footHold.getChildByPath("y2"));
                    MapleFoothold fh = new MapleFoothold(new Point(x1, y1), new Point(x2, y2), Integer.parseInt(footHold.getName()));
                    fh.setPrev(MapleDataTool.getInt(footHold.getChildByPath("prev")));
                    fh.setNext(MapleDataTool.getInt(footHold.getChildByPath("next")));

                    if (fh.getX1() < lBound.x)
                        lBound.x = fh.getX1();
                    if (fh.getX2() > uBound.x)
                        uBound.x = fh.getX2();
                    if (fh.getY1() < lBound.y)
                        lBound.y = fh.getY1();
                    if (fh.getY2() > uBound.y)
                        uBound.y = fh.getY2();
                    allFootholds.add(fh);
                }
            }
        }
        MapleFootholdTree fTree = new MapleFootholdTree(lBound, uBound);
        allFootholds.forEach((fh) -> {
            fTree.insert(fh);
        });
        map.setFootholds(fTree);

        if (mapData.getChildByPath("area") != null) {
            for (MapleData area : mapData.getChildByPath("area")) {
                int x1 = MapleDataTool.getInt(area.getChildByPath("x1"));
                int y1 = MapleDataTool.getInt(area.getChildByPath("y1"));
                int x2 = MapleDataTool.getInt(area.getChildByPath("x2"));
                int y2 = MapleDataTool.getInt(area.getChildByPath("y2"));
                Rectangle mapArea = new Rectangle(x1, y1, (x2 - x1), (y2 - y1));
                map.addMapleArea(mapArea);
            }
        }
        try {
            Connection con = DatabaseConnection.getConnection();
            try (PreparedStatement ps = con.prepareStatement("SELECT * FROM spawns WHERE mid = ?")) {
                ps.setInt(1, mapid);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int id = rs.getInt("idd");
                        int f = rs.getInt("f");
                        boolean hide = false;
                        String type = rs.getString("type");
                        int fh = rs.getInt("fh");
                        int cy = rs.getInt("cy");
                        int rx0 = rs.getInt("rx0");
                        int rx1 = rs.getInt("rx1");
                        int x = rs.getInt("x");
                        int y = rs.getInt("y");
                        int mobTime = rs.getInt("mobtime");

                        AbstractLoadedMapleLife myLife = loadLife(id, f, hide, fh, cy, rx0, rx1, x, y, type);
                        switch (type) {
                            case "n":
                                map.addMapObject(myLife);
                                break;
                            case "m":
                                MapleMonster monster = (MapleMonster) myLife;
                                map.addMonsterSpawn(monster, mobTime, -1);
                                break;
                        }
                    }
                }
            }                                      
            } catch(SQLException e) {
               // log.info(e.toString());
            }
            for (MapleData life : mapData.getChildByPath("life")) {
                String id = MapleDataTool.getString(life.getChildByPath("id"));
                String type = MapleDataTool.getString(life.getChildByPath("type"));
                AbstractLoadedMapleLife myLife = loadLife(life, id, type);
                if (myLife instanceof MapleMonster) {
                    MapleMonster monster = (MapleMonster) myLife;
                    int mobTime = MapleDataTool.getInt("mobTime", life, 0);
                    int team = MapleDataTool.getInt("team", life, -1);
                    if (mobTime == -1) { 
                        map.spawnMonster(monster);
                    } else {
                        map.addMonsterSpawn(monster, mobTime, team);
                    }
                    map.addAllMonsterSpawn(monster, mobTime, team);
                } else {
                    map.addMapObject(myLife);
                }
            }


            FieldBoss.RegisterBossRespawn(map); // TODO

            if (reactors && mapData.getChildByPath("reactor") != null) {
                for (MapleData reactor : mapData.getChildByPath("reactor")) {
                    String id = MapleDataTool.getString(reactor.getChildByPath("id"));
                    if (id != null) {
                        Reactor newReactor = loadReactor(reactor, id);
                        map.spawnReactor(newReactor);
                    }
                }
            }

            MapleData mcData = mapData.getChildByPath("monsterCarnival");
            if (mcData != null) {
                MCWZData mcpqInfo = new MCWZData(mcData);
                map.setMCPQData(mcpqInfo);
                map.setRespawning(false);
            } 
            try {
                map.setMapName(MapleDataTool.getString("mapName", nameData.getChildByPath(getMapStringName(mapid)), ""));
                map.setStreetName(MapleDataTool.getString("streetName", nameData.getChildByPath(getMapStringName(mapid)), ""));
            } catch (Exception e) {
                map.setMapName("");
                map.setStreetName("");
            }
            map.setClock(mapData.getChildByPath("clock") != null);
            map.setEverlast(mapData.getChildByPath("everlast") != null);
            map.setTown(mapData.getChildByPath("town") != null);
            map.setHPDec(MapleDataTool.getIntConvert("decHP", mapData, 0)); 
            map.setHPDecProtect(MapleDataTool.getIntConvert("protectItem", mapData, 0)); 
            map.setForcedReturnField(MapleDataTool.getInt(mapData.getChildByPath("info/forcedReturn"), MapConstants.NULL_MAP));
            if (mapData.getChildByPath("shipObj") != null) {
                map.setBoat(true);
            } else {
                map.setBoat(false);
            }
            map.setTimeLimit(MapleDataTool.getIntConvert("timeLimit", mapData.getChildByPath("info"), -1));
            mapsWLock.lock();
            try {
                maps.put(omapid, map);
            } finally {
                mapsWLock.unlock();
            }
            return map;
    }
    
    public Field getMap(int mapid) {
        Integer omapid = Integer.valueOf(mapid);
        Field map;
        
        mapsRLock.lock();
        try {
            map = maps.get(omapid);
        } finally {
            mapsRLock.unlock();
        }
        
        return (map != null) ? map : loadMapFromWz(mapid, omapid);
    }
    
    public boolean isMapLoaded(int mapId) {
        mapsRLock.lock();
        try {
            return maps.containsKey(mapId);
        } finally {
            mapsRLock.unlock();
        }
    }

    private AbstractLoadedMapleLife loadLife(int id, int f, boolean hide, int fh, int cy, int rx0, int rx1, int x, int y, String type) {
        AbstractLoadedMapleLife myLife = MapleLifeFactory.getLife(id, type);
        myLife.setCy(cy);
        myLife.setF(f);
        myLife.setFh(fh);
        myLife.setRx0(rx0);
        myLife.setRx1(rx1);
        myLife.setPosition(new Point(x, y));
        myLife.setHide(hide);
        return myLife;
    }
    
    private AbstractLoadedMapleLife loadLife(MapleData life, String id, String type) {
        AbstractLoadedMapleLife myLife = MapleLifeFactory.getLife(Integer.parseInt(id), type);
        myLife.setCy(MapleDataTool.getInt(life.getChildByPath("cy")));
        MapleData dF = life.getChildByPath("f");
        if (dF != null) {
            myLife.setF(MapleDataTool.getInt(dF));
        }
        myLife.setFh(MapleDataTool.getInt(life.getChildByPath("fh")));
        myLife.setRx0(MapleDataTool.getInt(life.getChildByPath("rx0")));
        myLife.setRx1(MapleDataTool.getInt(life.getChildByPath("rx1")));
        myLife.setPosition(new Point(MapleDataTool.getInt(life.getChildByPath("x")), MapleDataTool.getInt(life.getChildByPath("y"))));

        if (MapleDataTool.getInt("hide", life, 0) == 1 && myLife instanceof MapleNPC) {
            myLife.setHide(true);
        }
        return myLife;
    }
	
    private Reactor loadReactor(MapleData reactor, String id) {
        Reactor myReactor = new Reactor(ReactorFactory.getReactor(Integer.parseInt(id)), Integer.parseInt(id));
        
        int x = MapleDataTool.getInt(reactor.getChildByPath("x"));
        int y = MapleDataTool.getInt(reactor.getChildByPath("y"));
        myReactor.setPosition(new Point(x, y));
        
        myReactor.setDelay(MapleDataTool.getInt(reactor.getChildByPath("reactorTime")) * 1000);
        myReactor.setState((byte) 0);
        myReactor.setName(MapleDataTool.getString(reactor.getChildByPath("name"), ""));
        return myReactor;
    }
    
    private String getMapName(int mapid) {
        String mapName = StringUtil.getLeftPaddedStr(Integer.toString(mapid), '0', 9);
        StringBuilder builder = new StringBuilder("Map/Map");
        int area = mapid / 100000000;
        builder.append(area);
        builder.append("/");
        builder.append(mapName);
        builder.append(".img");

        mapName = builder.toString();
        return mapName;
    }
	
    private String getMapStringName(int mapid) {
        StringBuilder builder = new StringBuilder();
        if (mapid < 100000000) 
            builder.append("maple");
        else if (mapid >= 100000000 && mapid < 200000000)
            builder.append("victoria");
        else if (mapid >= 200000000 && mapid < 300000000)
            builder.append("ossyria");
        else if (mapid >= 540000000 && mapid < 541010110)
            builder.append("MasteriaGL");
        else if (mapid >= 600000000 && mapid < 620000000)
            builder.append("singapore");
        else if (mapid >= 670000000 && mapid < 682000000)
            builder.append("weddingGL");
        else if (mapid >= 682000000 && mapid < 683000000)
            builder.append("HalloweenGL");
        else if (mapid >= 800000000 && mapid < 900000000)
            builder.append("jp");
        else
            builder.append("etc");
        
        builder.append("/");
        builder.append(mapid);

        String mapName = builder.toString();
        return mapName;		
    }
    
    public void setChannel(int channel) {
        this.channel = channel;
    }
    
    public Map<Integer, Field> getMaps() {
        mapsRLock.lock();
        try {
            return Collections.unmodifiableMap(maps);
        } finally {
            mapsRLock.unlock();
        }
    }
    
    public void dispose() {
        Collection<Field> mapValues;
        
        mapsRLock.lock();
        try {
            mapValues = maps.values();
        } finally {
            mapsRLock.unlock();
        }
        
        for(Field map: mapValues) map.setEventInstance(null);
        this.event = null;
    }
    
    public List<Field> getAllLoadedMaps() {
        List<Field> ret = new ArrayList<>();
        mapsRLock.lock();
        try {
            ret.addAll(maps.values());
        } finally {
            mapsRLock.unlock();
        }
        return ret;
    }
    
    public int getLoadedMapSize() {
        return maps.size();
    }

    public List<Integer> getLoadedMaps() {
        List<Integer> ret = new LinkedList<>();
        for (Entry<Integer, Field> entry : maps.entrySet()) {
            ret.add(entry.getKey());
        }
        return ret;
    }
}

