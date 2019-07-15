/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package handling.channel.handler;

import client.player.Player;
import client.Client;
import handling.mina.PacketReader;
import server.partyquest.mcpq.MCField;
import server.partyquest.mcpq.MCTracker;
import server.partyquest.mcpq.MonsterCarnival;

/**
 *
 * @author GabrielSin
 */
public class MonsterCarnivalHandler {

    public static void MonsterCarnivalParty(PacketReader slea, Client c) {
        int tab = slea.readByte();
        int num = slea.readByte();
        Player p = c.getPlayer();

        if (MonsterCarnival.DEBUG) {
            MCTracker.log("[MCHandler] " + p.getName() + " used tab "  + tab + " num " + num);
            System.out.println("[MCHandler] " + p.getName() + " used tab "  + tab + " num " + num);
        }

        if (p.getMCPQField() == null || p.getMCPQParty() == null) {
            MCTracker.log("[MCHandler] " + p.getName() + " attempting to use Monster Carnival handler without being in Monster Carnival");
            return;
        }

        MCField field = p.getMCPQField();
        switch (tab) {
            case 0:
                field.onAddSpawn(c.getPlayer(), num);
                break;
            case 1:
                field.onUseSkill(c.getPlayer(), num);
                break;
            case 2:
                field.onGuardianSummon(c.getPlayer(), num);
                break;
            default:
                break;
        }
    }
}
