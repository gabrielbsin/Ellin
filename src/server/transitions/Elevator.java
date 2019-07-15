/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package server.transitions;

import client.player.Player;
import java.util.List;
import handling.channel.ChannelServer;
import tools.TimerTools.MapTimer;
import server.PropertiesTable;
import server.maps.Field;
import server.maps.object.FieldObject;

/**
 * @author GabrielSin
 */
public class Elevator {

public static PropertiesTable prop = new PropertiesTable();
public Field elevatorS, elevatorM, returnMap, arrive;

    public void Start(ChannelServer channel) {
        prop.setProperty("isUp", Boolean.FALSE);
        prop.setProperty("isDown", Boolean.FALSE);
        channel.getMapFactory().getMap(222020200).setReactorState();
        onDown(channel);
    }

    public final void onDown(ChannelServer channel) {
        channel.getMapFactory().getMap(222020100).resetReactors();
        warpAllPlayer(222020210, 222020211);
        prop.setProperty("isDown", Boolean.TRUE);
        MapTimer.getInstance().schedule(() -> {
            goingUp(channel);
        }, 60000);
    }
      
    public void goingUp(ChannelServer channel) {
        warpAllPlayer(222020110, 222020111);
        prop.setProperty("isDown", Boolean.FALSE);
        MapTimer.getInstance().schedule(() -> {
            onUp(channel);
        }, 50000);
        channel.getMapFactory().getMap(222020100).setReactorState();
    }

    public void onUp(ChannelServer channel) {
        channel.getMapFactory().getMap(222020200).resetReactors();
        warpAllPlayer(222020111, 222020200);
        prop.setProperty("isUp", Boolean.TRUE);
        MapTimer.getInstance().schedule(() -> {
            goingDown(channel);
        }, 60000);
    }

    public void goingDown(ChannelServer channel) {
        warpAllPlayer(222020211, 222020100);
        prop.setProperty("isUp", Boolean.FALSE);
        MapTimer.getInstance().schedule(() -> {
            onDown(channel);
        }, 50000);
        channel.getMapFactory().getMap(222020200).setReactorState();
    }

    public void warpAllPlayer(int from, int to) {
        for (ChannelServer cserv : ChannelServer.getAllInstances()) {
            final Field tomap = cserv.getMapFactory().getMap(to);
            final Field frommap = cserv.getMapFactory().getMap(from);
            List<Player> list = frommap.getCharactersThreadsafe();
            if (tomap != null && frommap != null && list != null && frommap.getCharactersSize() > 0) {
                for (FieldObject mmo : list) {
                    ((Player) mmo).changeMap(tomap, tomap.getPortal(0));
                }
            }
        }
    }

    public static PropertiesTable getProperties() {
        return Elevator.prop;
    }

    public static boolean elevatorIsDown() {
        return getProperties().getProperty("isDown").equals(Boolean.TRUE);
    }

    public static boolean elevatorIsUp() {
        return getProperties().getProperty("isUp").equals(Boolean.TRUE);
    }
}