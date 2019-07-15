/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package server.transitions;

import java.awt.Point;
import handling.channel.ChannelServer;
import packet.creators.EffectPackets;
import packet.creators.PacketCreator;
import server.PropertiesTable;
import server.life.MapleLifeFactory;
import server.maps.Field;
import tools.TimerTools.MapTimer;

/**
 * @author GabrielSin
 */
public class Boats {
 
public long closeTime = 50 * 1000;
public long beginTime = 60 * 1000;
public long rideTime = 120 * 1000;
public long invasionTime = 30 * 1000; 
public static PropertiesTable prop = new PropertiesTable();  
public Field orbisBtf, boatToOrbis, orbisBoatCabin, orbisDocked, boatToEllinia, elliniaBtf, elliniaBoatCabin, elliniaDocked, orbisStation;

    public void Start(ChannelServer channel) {
        orbisBtf = channel.getMapFactory().getMap(200000112);
        elliniaBtf = channel.getMapFactory().getMap(101000301);
        boatToOrbis = channel.getMapFactory().getMap(200090010);
        boatToEllinia = channel.getMapFactory().getMap(200090000);
        orbisBoatCabin = channel.getMapFactory().getMap(200090011);
        elliniaBoatCabin = channel.getMapFactory().getMap(200090001);
        elliniaDocked = channel.getMapFactory().getMap(101000300);
        orbisStation = channel.getMapFactory().getMap(200000100);
        orbisDocked = channel.getMapFactory().getMap(200000111);
        setPortalOrbis();
        setPortalEllinia();
        scheduleNew();
    }

    public final void scheduleNew() {
        elliniaDocked.setDocked(true);
        orbisDocked.setDocked(true);
        
        elliniaDocked.broadcastMessage(PacketCreator.ShipEffect(true));
        orbisDocked.broadcastMessage(PacketCreator.ShipEffect(true));
        
        prop.setProperty("docked", Boolean.TRUE);
        prop.setProperty("entry", Boolean.TRUE);
        prop.setProperty("haveBalrog", Boolean.FALSE);
        MapTimer.getInstance().schedule(() -> {
            stopEntry();
        }, closeTime);
        MapTimer.getInstance().schedule(() -> {
            takeOff();
        }, beginTime);
    }

    public void stopEntry() {
        prop.setProperty("entry", Boolean.FALSE);
        orbisBoatCabin.resetReactors();
        elliniaBoatCabin.resetReactors();
    }

    public void takeOff() {
        prop.setProperty("docked", Boolean.FALSE);
        orbisBtf.warpEveryone(boatToEllinia.getId());
        elliniaBtf.warpEveryone(boatToOrbis.getId());
        elliniaDocked.setDocked(false);
        orbisDocked.setDocked(false);
        elliniaDocked.broadcastMessage(PacketCreator.ShipEffect(false));
        orbisDocked.broadcastMessage(PacketCreator.ShipEffect(false));
        MapTimer.getInstance().schedule(() -> {
            invasionBalrog();
        }, invasionTime);
        MapTimer.getInstance().schedule(() -> {
            arrived();
        }, rideTime);
    }

    public void arrived() {
        boatToOrbis.warpEveryone(orbisStation.getId());
        orbisBoatCabin.warpEveryone(orbisStation.getId());
        boatToEllinia.warpEveryone(elliniaDocked.getId());
        elliniaBoatCabin.warpEveryone(elliniaDocked.getId());
        boatToOrbis.killAllMonsters();
        boatToEllinia.killAllMonsters();
        scheduleNew();
    }

    public void invasionBalrog() {
        int numberSpawns = 2;
        if (numberSpawns > 0) {
            for (int i=0; i < numberSpawns; i++) {
                boatToOrbis.spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(8150000), new Point(485, -221));
                boatToEllinia.spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(8150000), new Point(-590, -221));
            }
            boatToOrbis.setDocked(true);
            boatToEllinia.setDocked(true);
            boatToOrbis.broadcastMessage(EffectPackets.MusicChange("Bgm04/ArabPirate"));
            boatToEllinia.broadcastMessage(EffectPackets.MusicChange("Bgm04/ArabPirate"));
            prop.setProperty("haveBalrog", Boolean.TRUE);
        }
    }
    
    public final void setPortalOrbis() {
        for (ChannelServer cserv : ChannelServer.getAllInstances()) {
            cserv.getMapFactory().getMap(200090011).getPortal("out00").setScriptName("OBoat1");
            cserv.getMapFactory().getMap(200090011).getPortal("out01").setScriptName("OBoat2");
        }
    }
     
    public final void setPortalEllinia() {
        for (ChannelServer cserv : ChannelServer.getAllInstances()) {
            cserv.getMapFactory().getMap(200090001).getPortal("out00").setScriptName("EBoat1");
            cserv.getMapFactory().getMap(200090001).getPortal("out01").setScriptName("EBoat2");
        }
   }

    public static PropertiesTable getProperties() {
        return prop;
    }
    
    public static boolean boatOpen() {
        return getProperties().getProperty("entry").equals(Boolean.TRUE);
    }

    public static boolean hasBalrog() {
        return getProperties().getProperty("haveBalrog").equals(Boolean.TRUE);
    }
}  