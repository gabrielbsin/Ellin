/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package handling.world.service;

import handling.channel.ChannelServer;
import java.util.List;
import packet.transfer.write.OutPacket;
import client.player.Player;

public class BroadcastService {

    public static void broadcastSmega(OutPacket message) {
        ChannelServer.getAllInstances().forEach((cs) -> {
            cs.broadcastSmega(message);
        });
    }

    public static void broadcastGMMessage(OutPacket message) {
        ChannelServer.getAllInstances().forEach((cs) -> {
            cs.broadcastGMMessage(message);
        });
    }

    public static void broadcastMessage(OutPacket message) {
        ChannelServer.getAllInstances().forEach((cs) -> {
            cs.broadcastMessage(message);
        });
    }

    public static void sendPacket(List<Integer> targetIds, OutPacket packet, int exception) {
        Player c;
        for (int i : targetIds) {
            if (i == exception) {
                continue;
            }
            int ch = FindService.findChannel(i);
            if (ch < 0) {
                continue;
            }
            c = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterById(i);
            if (c != null) {
                c.getClient().getSession().write(packet);
            }
        }
    }

    public static void sendGuildPacket(int targetIds, OutPacket packet, int exception, int guildid) {
        if (targetIds == exception) {
            return;
        }
        int ch = FindService.findChannel(targetIds);
        if (ch < 0) {
            return;
        }
        final Player c = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterById(targetIds);
        if (c != null && c.getGuildId() == guildid) {
            c.getClient().getSession().write(packet);
        }
    } 
}
