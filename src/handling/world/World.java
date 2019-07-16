/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package handling.world;

import handling.channel.ChannelServer;
import handling.channel.PlayerStorage;
import handling.world.service.AllianceService;
import handling.world.service.FindService;
import handling.world.service.MessengerService;
import handling.world.service.PartyService;
import handling.world.service.RespawnService.Respawn;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import tools.CollectionUtil;
import tools.TimerTools.WorldTimer;

public class World {
    
    public static final int channelPerThread = 3;

    public static void init() {
        FindService.findChannel(0);
        AllianceService.allianceLocks.toString();
        MessengerService.getMessenger(0);
        PartyService.getParty(0);
    }
    
    public static String getStatus() throws RemoteException {
        StringBuilder ret = new StringBuilder();
        int totalUsers = 0;
        for (ChannelServer cs : ChannelServer.getAllInstances()) {
            ret.append("Channel ");
            ret.append(cs.getChannel());
            ret.append(": ");
            int channelUsers = cs.getConnectedClients();
            totalUsers += channelUsers;
            ret.append(channelUsers);
            ret.append(" users\n");
        }
        ret.append("Total users online: ");
        ret.append(totalUsers);
        ret.append("\n");
        return ret.toString();
    }
    
    public static Map<Integer, Integer> getConnected() {
        Map<Integer, Integer> ret = new HashMap<>();
        int total = 0;
        for (ChannelServer cs : ChannelServer.getAllInstances()) {
            int curConnected = cs.getConnectedClients();
            ret.put(cs.getChannel(), curConnected);
            total += curConnected;
        }
        ret.put(0, total);
        return ret;
    }
    
    public static boolean isConnected(String charName) {
        return FindService.findChannel(charName) > 0;
    }
    
    public static boolean isCharacterListConnected(List<String> charName) {
        for (ChannelServer cs : ChannelServer.getAllInstances()) {
            for (final String c : charName) {
                if (cs.getPlayerStorage().getCharacterByName(c) != null) {
                    return true;
                }
            }
        }
        return false;
    }
    
    public static PlayerStorage getStorage(int channel) {
        return ChannelServer.getInstance(channel).getPlayerStorage();
    }

    public static List<CheaterData> getCheaters() {
        List<CheaterData> allCheaters = new ArrayList<>();
        for (ChannelServer cs : ChannelServer.getAllInstances()) {
            allCheaters.addAll(cs.getCheaters());
        }
        Collections.sort(allCheaters);
        return CollectionUtil.copyFirst(allCheaters, 20);
    }

    public static void registerRespawn() {
        Integer[] chs = ChannelServer.getAllInstance().toArray(new Integer[0]);
        for (int i = 0; i < chs.length; i += channelPerThread) {
            WorldTimer.getInstance().register(new Respawn(chs, i), 4500);
        }
    }
}
