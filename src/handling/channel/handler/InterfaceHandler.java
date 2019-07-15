/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package handling.channel.handler;

import client.Client;
import handling.channel.ChannelServer;
import handling.mina.PacketReader;
import server.maps.Field;

/**
 *
 * @author GabrielSin
 */
public class InterfaceHandler {

    public static void ShipObjectRequest(PacketReader packet, Client c) {
        int mapId = packet.readInt();
        ChannelServer cserv = ChannelServer.getInstance(c.getChannel());
        Field mapStats = cserv.getMapFactory().getMap(mapId);
    }   
}
