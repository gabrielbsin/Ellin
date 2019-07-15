/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package client.player.commands;

import client.Client;
import client.ClientLoginState;
import constants.CommandConstants.CoomandRank;
import database.DatabaseConnection;
import handling.channel.ChannelServer;
import handling.world.service.BroadcastService;
import handling.world.service.FindService;
import java.awt.Point;
import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import packet.creators.PacketCreator;
import packet.transfer.write.OutPacket;
import client.player.Player;
import client.player.PlayerNote;
import client.player.commands.object.CommandExecute;
import client.player.inventory.Equip;
import client.player.inventory.Item;
import java.util.Arrays;
import java.util.Map;
import provider.MapleData;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;
import scripting.portal.PortalScriptManager;
import server.ShutdownServer;
import server.expeditions.MapleExpedition;
import server.itens.InventoryManipulator;
import server.itens.ItemInformationProvider;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.life.MapleMonsterInformationProvider;
import server.life.npc.MapleNPC;
import server.maps.Field;
import server.maps.object.FieldObject;
import server.maps.reactors.Reactor;
import server.maps.reactors.ReactorFactory;
import server.maps.reactors.ReactorStats;
import tools.Pair;
import tools.StringUtil;
import tools.TimerTools.MiscTimer;

/**
 * 
 * @author GabrielSin
 */
public class AdminCommand {
    
    public static CoomandRank getPlayerLevelRequired() {
        return CoomandRank.ADMIN;
    }
    
    public static class ShutdownTime extends AdminCommand.Shutdown {

        private static ScheduledFuture<?> ts = null;
        private int minutesLeft = 0;

        @Override
        public boolean execute(Client c, String[] splitted) {
            this.minutesLeft = Integer.parseInt(splitted[1]);
            
            c.getPlayer().dropMessage(6, "Desligando o servidor em... " + this.minutesLeft + " minutos");
            
            if (ts == null && (t == null || !t.isAlive())) {
                t = new Thread(ShutdownServer.getInstance());
                ts = MiscTimer.getInstance().register(() -> {
                    if (AdminCommand.ShutdownTime.this.minutesLeft == 0) {
                        ShutdownServer.getInstance().shutdown();
                        AdminCommand.Shutdown.t.start();
                        AdminCommand.ShutdownTime.ts.cancel(false);
                        return;
                    }
                    BroadcastService.broadcastMessage(PacketCreator.ServerNotice(0, "O servidor será desligado em " + AdminCommand.ShutdownTime.this.minutesLeft + " minutos. Por favor, faça logoff com segurança."));
                    AdminCommand.ShutdownTime.this.minutesLeft--;
                }, 60000L);
            } else {
                c.getPlayer().dropMessage(6, "Já existe um desligamento em andamento ou o encerramento não foi concluído. Por favor, espere.");
            }
            return true;
        }
    }
    
    public static class Position extends CommandExecute {
        @Override
        public boolean execute(Client c, String[] splitted) {
            float xpos = c.getPlayer().getPosition().x;
            float ypos = c.getPlayer().getPosition().y;
            float fh = c.getPlayer().getMap().getFootholds().findBelow(c.getPlayer().getPosition()).getId();
            c.getPlayer().dropMessage(6, "Position: (" + xpos + ", " + ypos + ")");
            c.getPlayer().dropMessage(6, "Foothold ID: " + fh);
            return true;
        }
    }
    
    public static class Expeds extends CommandExecute {
        @Override
        public boolean execute(Client c, String[] splitted) {
            for (ChannelServer ch : ChannelServer.getAllInstances()) {
                if (ch.getExpeditions().isEmpty()) {
                    c.getPlayer().yellowMessage("No Expeditions in Channel " + ch.getChannelId());
                    continue;
                }
                c.getPlayer().yellowMessage("Expeditions in Channel " + ch.getChannelId());
                int id = 0;
                for (MapleExpedition exped : ch.getExpeditions()) {
                    id++;
                    c.getPlayer().yellowMessage("> Expedition " + id);
                    c.getPlayer().yellowMessage(">> Type: " + exped.getType().toString());
                    c.getPlayer().yellowMessage(">> Status: " + (exped.isRegistering() ? "REGISTERING" : "UNDERWAY"));
                    c.getPlayer().yellowMessage(">> Size: " + exped.getMembers().size());
                    c.getPlayer().yellowMessage(">> Leader: " + exped.getLeader().getName());
                    int memId = 2;
                    for (Player member : exped.getMembers()) {
                        if (exped.isLeader(member)) {
                                continue;
                        }
                        c.getPlayer().yellowMessage(">>> Member " + memId + ": " + member.getName());
                        memId++;
                    }
                }
            }
            return true;
        }
    }
    
    public static class Seeds extends CommandExecute {
        @Override
        public boolean execute(Client c, String[] splitted) {
            if (c.getPlayer().getMapId() != 910010000) {
                c.getPlayer().yellowMessage("This command can only be used in HPQ.");
                return false;
            }
            Point pos[] = {new Point(7, -207), new Point(179, -447), new Point(-3, -687), new Point(-357, -687), new Point(-538, -447), new Point(-359, -207)};
            int seed[] = {4001097, 4001096, 4001095, 4001100, 4001099, 4001098};
            for (int i = 0; i < pos.length; i++) {
                Item item = new Item(seed[i], (byte) 0, (short) 1);
                c.getPlayer().getMap().spawnItemDrop(c.getPlayer(), c.getPlayer(), item, pos[i], false, true);
                try {
                        Thread.sleep(100);
                } catch (InterruptedException e) {
                        e.printStackTrace();
                }
            }
            return true;
        }
    }

    public static class ReloadEvents extends CommandExecute {
        @Override
        public boolean execute(Client c, String[] splitted) {
            for (ChannelServer ch : ChannelServer.getAllInstances()) {
                ch.reloadEventScriptManager();
            }
            c.getPlayer().dropMessage(5, "Reloaded Events");
            return true;
        }
    }
    
    public static class ReloadDrops extends CommandExecute {
        @Override
        public boolean execute(Client c, String[] splitted) {
            MapleMonsterInformationProvider.getInstance().clearDrops();
            c.getPlayer().dropMessage(5, "Reloaded Drops");
            return true;
        }
    }
    
    public static class ReloadPortals extends CommandExecute {
        @Override
        public boolean execute(Client c, String[] splitted) {
            PortalScriptManager.getInstance().clearScripts();
            c.getPlayer().dropMessage(5, "Reloaded Portal");
            return true;
        }
    }
    
    public static class ReloadMaps extends CommandExecute {
        @Override
        public boolean execute(Client c, String[] splitted) {
            
            Field oldMap = c.getPlayer().getMap();
            Field newMap = c.getChannelServer().getMapFactory().resetMap(c.getPlayer().getMapId());
            int callerid = c.getPlayer().getId();

            for (Player p : oldMap.getCharacters()) {
                p.changeMap(newMap);
                if (p.getId() != callerid) {
                    p.dropMessage("You have been relocated due to map reloading. Sorry for the inconvenience.");
                }
            }
            newMap.Respawn(false);
            return true;
        }
     }

    public static class Shutdown extends CommandExecute {

        public static Thread t = null;

        @Override
        public boolean execute(Client c, String[] splitted) {
            c.getPlayer().dropMessage(6, "Desligando...");
            if (t == null || !t.isAlive()) {
                t = new Thread(ShutdownServer.getInstance());
                ShutdownServer.getInstance().shutdown();
                t.start();
            } else {
                c.getPlayer().dropMessage(6, "Já existe um desligamento em andamento. Por favor, aguarde.");
            }
            return true;
        }
    }
    
    public static class ServerMessage extends CommandExecute {

        @Override
        public boolean execute(Client c, String[] splitted) {
            Collection<ChannelServer> cservs = ChannelServer.getAllInstances();
            String outputMessage = StringUtil.joinStringFrom(splitted, 1);
            for (ChannelServer cserv : cservs) {
                cserv.setServerMessage(outputMessage);
            }
            return true;
        }
    }
    
    public static class SaveAll extends CommandExecute {
        @Override
        public boolean execute(Client c, String[] splitted) {
            try {
                for (ChannelServer ch : ChannelServer.getAllInstances()) {
                    for (Player chr : ch.getPlayerStorage().getAllCharacters()) {
                        chr.saveDatabase();
                    }
                }
                System.out.println("[Noticia] Mundo salvo com sucesso!");
            } catch (Exception e) {
                System.out.println("[Noticia] Ocorreu um erro!");
                System.err.println(e);
            }
            return true;
        }
    }
    
    public static class WarpAllHere extends CommandExecute {
        @Override
        public boolean execute(Client c, String[] splitted) {
            List<Player> people = new LinkedList<>();
            for (ChannelServer cs : ChannelServer.getAllInstances()) {
                for (Player mch : cs.getPlayerStorage().getAllCharacters()) {
                    if (mch.getMapId() != c.getPlayer().getMapId() || mch.getClient().getChannel() != c.getChannel()) {
                        people.add(mch);
                    }
                }
            }
            String ip = c.getChannelServer().getIP();
            String[] socket = ip.split(":");
            
            for (Player p : people) {
                if (p.getClient().getChannel() != c.getChannel()) {
                    try {
                        p.getMap().removePlayer(p);
                        ChannelServer.getInstance(p.getClient().getChannel()).removePlayer(p);
                        p.getClient().updateLoginState(ClientLoginState.LOGIN_SERVER_TRANSITION, c.getSessionIPAddress());
                        p.getClient().getSession().write(PacketCreator.GetChannelChange(InetAddress.getByName(socket[0]), Integer.parseInt(socket[1])));
                    } catch (UnknownHostException ex) {
                        Logger.getLogger(AdminCommand.class.getName()).log(Level.SEVERE, null, ex);
                        return false;
                    }
                }
                p.changeMap(c.getPlayer().getMap(), c.getPlayer().getMap().getPortal("sp"));
            }
            return true;
        }
    }
    
    public static class SuperEquip extends CommandExecute {
        @Override
        public boolean execute(Client c, String[] splitted) {
            if (splitted.length != 3) {
                c.getPlayer().dropMessage("Syntax: !superequip <id> <stat>");
                return false;
            }
            int id;
            short stat;

            id = stat = 0;

            try {
                id = Integer.parseInt(splitted[1]);
                stat = (short) Integer.parseInt(splitted[2]);
            } catch (NumberFormatException nfe) {
                c.getPlayer().dropMessage("Error occured while parsing values. Please recheck and try again.");
                return false;
            }
            
            Equip eq = new Equip(id, (byte) -1);

            eq.setAcc(stat);
            eq.setAvoid(stat);
            eq.setInt(stat);
            eq.setDex(stat);
            eq.setLuk(stat);
            eq.setMatk(stat);
            eq.setMdef(stat);
            eq.setStr(stat);
            eq.setWatk(stat);
            eq.setWdef(stat);
            InventoryManipulator.addFromDrop(c, eq, new String(), false); 
            return true;
        }
    }
    
    public static class NPC extends CommandExecute {

        @Override
        public boolean execute(Client c, String[] splitted) {
            int npcId = Integer.parseInt(splitted[1]);
            MapleNPC npc = MapleLifeFactory.getNPC(npcId);
            if (npc != null && !npc.getName().equals("MISSINGNO")) {
                npc.setPosition(c.getPlayer().getPosition());
                npc.setCy(c.getPlayer().getPosition().y);
                npc.setRx0(c.getPlayer().getPosition().x);
                npc.setRx1(c.getPlayer().getPosition().x);
                npc.setFh(c.getPlayer().getMap().getFootholds().findBelow(c.getPlayer().getPosition()).getId());
                npc.getStats().setCustom(true);
                c.getPlayer().getMap().addMapObject(npc);
                c.getPlayer().getMap().broadcastMessage(PacketCreator.SpawnNPC(npc, true));
            } else {
                c.getPlayer().dropMessage(6, "You have entered an invalid Npc-Id");
                return false;
            }
            return true;
        }
    }

    public static class MyNPCPos extends CommandExecute {

        @Override
        public boolean execute(Client c, String[] splitted) {
            Point pos = c.getPlayer().getPosition();
            c.getPlayer().dropMessage(6, "X: " + pos.x + " | Y: " + pos.y + " | RX0: " + (pos.x) + " | RX1: " + (pos.x));
            return true;
        }
    }
    
    public static class Notice extends CommandExecute {

        private static int getNoticeType(String typestring) {
            switch (typestring) {
                case "n":
                    return 0;
                case "p":
                    return 1;
                case "l":
                    return 2;
                case "nv":
                    return 5;
                case "v":
                    return 5;
                case "b":
                    return 6;
                default:
                    break;
            }
            return -1;
        }

        @Override
        public boolean execute(Client c, String[] splitted) {
            int joinmod = 1;
            int range = -1;
            switch (splitted[1]) {
                case "m":
                    range = 0;
                    break;
                case "c":
                    range = 1;
                    break;
                case "w":
                    range = 2;
                    break;
                default:
                    break;
            }

            int tfrom = 2;
            if (range == -1) {
                range = 2;
                tfrom = 1;
            }
            int type = getNoticeType(splitted[tfrom]);
            if (type == -1) {
                type = 0;
                joinmod = 0;
            }
            StringBuilder sb = new StringBuilder();
            if (splitted[tfrom].equals("nv")) {
                sb.append("[Notice]");
            } else {
                sb.append("");
            }
            joinmod += tfrom;
            sb.append(StringUtil.joinStringFrom(splitted, joinmod));

            OutPacket packet = PacketCreator.ServerNotice(type, sb.toString());
            switch (range) {
                case 0:
                    c.getPlayer().getMap().broadcastMessage(packet);
                    break;
                case 1:
                    ChannelServer.getInstance(c.getChannel()).broadcastPacket(packet);
                    break;
                case 2:
                    BroadcastService.broadcastMessage(packet);
                    break;
                default:
                    break;
            }
            return true;
        }
    }
    
    public static class Find extends CommandExecute {

        @Override
        public boolean execute(Client c, String[] splitted) {
            ItemInformationProvider ii = ItemInformationProvider.getInstance();
            Player mc = c.getPlayer();
            if (splitted.length == 1) {
                mc.dropMessage(splitted[0] + ": <NPC> <MOB> <ITEM> <MAP> <SKILL>");
            } else {

                String type = splitted[1];
                String search = StringUtil.joinStringFrom(splitted, 2);
                MapleData data = null;
                MapleDataProvider dataProvider = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/" + "String"));
                mc.dropMessage("<<Type: " + type + " | Search: " + search + ">>");
                if (type.equalsIgnoreCase("NPC") || type.equalsIgnoreCase("NPCS")) {
                    List<String> retNpcs = new ArrayList<>();
                    data = dataProvider.getData("Npc.img");
                    List<Pair<Integer, String>> npcPairList = new LinkedList<>();
                    for (MapleData npcIdData : data.getChildren()) {
                        int npcIdFromData = Integer.parseInt(npcIdData.getName());
                        String npcNameFromData = MapleDataTool.getString(npcIdData.getChildByPath("name"), "NO-NAME");
                        npcPairList.add(new Pair<>(npcIdFromData, npcNameFromData));
                    }
                    for (Pair<Integer, String> npcPair : npcPairList) {
                        if (npcPair.getRight().toLowerCase().contains(search.toLowerCase())) {
                            retNpcs.add(npcPair.getLeft() + " - " + npcPair.getRight());
                        }
                    }
                    if (retNpcs != null && retNpcs.size() > 0) {
                        for (String singleRetNpc : retNpcs) {
                            mc.dropMessage(singleRetNpc);
                        }
                    } else {
                        mc.dropMessage("No NPC's Found");
                    }

                } else if (type.equalsIgnoreCase("MAP") || type.equalsIgnoreCase("MAPS")) {
                    List<String> retMaps = new ArrayList<>();
                    data = dataProvider.getData("Map.img");
                    List<Pair<Integer, String>> mapPairList = new LinkedList<>();
                    for (MapleData mapAreaData : data.getChildren()) {
                        for (MapleData mapIdData : mapAreaData.getChildren()) {
                            int mapIdFromData = Integer.parseInt(mapIdData.getName());
                            String mapNameFromData = MapleDataTool.getString(mapIdData.getChildByPath("streetName"), "NO-NAME") + " - " + MapleDataTool.getString(mapIdData.getChildByPath("mapName"), "NO-NAME");
                            mapPairList.add(new Pair<>(mapIdFromData, mapNameFromData));
                        }
                    }
                    for (Pair<Integer, String> mapPair : mapPairList) {
                        if (mapPair.getRight().toLowerCase().contains(search.toLowerCase())) {
                            retMaps.add(mapPair.getLeft() + " - " + mapPair.getRight());
                        }
                    }
                    if (retMaps != null && retMaps.size() > 0) {
                        for (String singleRetMap : retMaps) {
                            mc.dropMessage(singleRetMap);
                        }
                    } else {
                        mc.dropMessage("No Maps Found");
                    }

                } else if (type.equalsIgnoreCase("MOB") || type.equalsIgnoreCase("MOBS") || type.equalsIgnoreCase("MONSTER") || type.equalsIgnoreCase("MONSTERS")) {
                    List<String> retMobs = new ArrayList<>();
                    data = dataProvider.getData("Mob.img");
                    List<Pair<Integer, String>> mobPairList = new LinkedList<>();
                    for (MapleData mobIdData : data.getChildren()) {
                        int mobIdFromData = Integer.parseInt(mobIdData.getName());
                        String mobNameFromData = MapleDataTool.getString(mobIdData.getChildByPath("name"), "NO-NAME");
                        mobPairList.add(new Pair<>(mobIdFromData, mobNameFromData));
                    }
                    for (Pair<Integer, String> mobPair : mobPairList) {
                        if (mobPair.getRight().toLowerCase().contains(search.toLowerCase())) {
                            retMobs.add(mobPair.getLeft() + " - " + mobPair.getRight());
                        }
                    }
                    if (retMobs != null && retMobs.size() > 0) {
                        for (String singleRetMob : retMobs) {
                            mc.dropMessage(singleRetMob);
                        }
                    } else {
                        mc.dropMessage("No Mob's Found");
                    }

                } else if (type.equalsIgnoreCase("REACTOR") || type.equalsIgnoreCase("REACTORS")) {
                    mc.dropMessage("NOT ADDED YET");

                } else if (type.equalsIgnoreCase("ITEM") || type.equalsIgnoreCase("ITEMS")) {
                    List<String> retItems = new ArrayList<>();
                    ItemInformationProvider miip = ItemInformationProvider.getInstance();
                    for (Map.Entry<Integer, String> itemEntry : miip.getAllItems().entrySet()) {
                        if (itemEntry.getValue().toLowerCase().contains(search.toLowerCase())) {
                            int id = itemEntry.getKey();
                            retItems.add(id + " - " + miip.getName(id));
                        }
                    }
                    if (retItems != null && retItems.size() > 0) {
                        for (String singleRetItem : retItems) {
                            mc.dropMessage(singleRetItem);
                        }
                    } else {
                        mc.dropMessage("No Item's Found");
                    }

                } else if (type.equalsIgnoreCase("SKILL") || type.equalsIgnoreCase("SKILLS")) {
                    List<String> retSkills = new ArrayList<String>();
                    data = dataProvider.getData("Skill.img");
                    List<Pair<Integer, String>> skillPairList = new LinkedList<Pair<Integer, String>>();
                    for (MapleData skillIdData : data.getChildren()) {
                        int skillIdFromData = Integer.parseInt(skillIdData.getName());
                        String skillNameFromData = MapleDataTool.getString(skillIdData.getChildByPath("name"), "NO-NAME");
                        skillPairList.add(new Pair<Integer, String>(skillIdFromData, skillNameFromData));
                    }
                    for (Pair<Integer, String> skillPair : skillPairList) {
                        if (skillPair.getRight().toLowerCase().contains(search.toLowerCase())) {
                            retSkills.add(skillPair.getLeft() + " - " + skillPair.getRight());
                        }
                    }
                    if (retSkills != null && retSkills.size() > 0) {
                        for (String singleRetSkill : retSkills) {
                            mc.dropMessage(singleRetSkill);
                        }
                    } else {
                        mc.dropMessage("No Skills Found");
                    }
                } else {
                    mc.dropMessage("Sorry, that search call is unavailable");
                }
            }   
            return true;
        }
    }

    public static class ID extends Find {
    }

    public static class LookUp extends Find {
    }

    public static class Search extends Find {
    }

    public static class Warp extends CommandExecute {

        @Override
        public boolean execute(Client c, String[] splitted) {
            ChannelServer cserv = c.getChannelServer();
            Player victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
            if (victim != null) {
                if (splitted.length == 2) {
                    c.getPlayer().changeMap(victim.getMap(), victim.getMap().findClosestPlayerSpawnpoint(victim.getPosition()));
                } else {
                    Field target = ChannelServer.getInstance(c.getChannel()).getMapFactory().getMap(Integer.parseInt(splitted[2]));
                    victim.changeMap(target, target.getPortal(0));
                }
            } else {
                try {
                    victim = c.getPlayer();
                    int ch = FindService.findChannel(splitted[1]);
                    if (ch < 0) {
                        Field target = c.getChannelServer().getMapFactory().getMap(Integer.parseInt(splitted[1]));
                        c.getPlayer().changeMap(target, target.getPortal(0));
                    } else {
                        victim = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterByName(splitted[1]);
                        c.getPlayer().dropMessage(6, "Cross changing channel. Please wait.");
                        if (victim.getMapId() != c.getPlayer().getMapId()) {
                            final Field mapp = c.getChannelServer().getMapFactory().getMap(victim.getMapId());
                            c.getPlayer().changeMap(mapp, mapp.getPortal(0));
                        }
                        c.getPlayer().changeChannel(ch);
                    }
                } catch (NumberFormatException e) {
                    c.getPlayer().dropMessage(6, "Something went wrong " + e.getMessage());
                    return false;
                }
            }
            return true;
        }
    }
    
    public static class warpChHere extends CommandExecute {
        @Override
        public boolean execute(Client c, String[] splitted) {
            try {
                for (Player p : c.getChannelServer().getPlayerStorage().getAllCharacters()) {
                    p.changeMap(c.getPlayer().getMap(), c.getPlayer().getPosition());
                    p.dropMessage(5, "You have been warped to the event");
                }
                c.getPlayer().dropMessage(5, "Every player in your channel have been warped here");
            } catch (Exception e) {
                System.out.println("Something went wrong: " + e);
                return false;
            }
            return true;
        }
    }
    
    public static class WarpMapTo extends CommandExecute {

        @Override
        public boolean execute(Client c, String[] splitted) {
            try {
                final Field target = c.getChannelServer().getMapFactory().getMap(Integer.parseInt(splitted[1]));
                final Field from = c.getPlayer().getMap();
                for (Player p : from.getCharactersThreadsafe()) {
                    p.changeMap(target, target.getPortal(0));
                }
            } catch (NumberFormatException e) {
                c.getPlayer().dropMessage(5, "Error: " + e.getMessage());
                return false; 
            }
            return true;
        }
    }

    public static class WarpHere extends CommandExecute {

        @Override
        public boolean execute(Client c, String[] splitted) {
            Player victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (victim != null) {
                victim.changeMap(c.getPlayer().getMap(), c.getPlayer().getMap().findClosestPlayerSpawnpoint(c.getPlayer().getPosition()));
            } else {
                int ch = FindService.findChannel(splitted[1]);
                if (ch < 0) {
                    c.getPlayer().dropMessage(5, "Not found.");
                    return false;
                }
                victim = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterByName(splitted[1]);
                c.getPlayer().dropMessage(5, "Victim is cross changing channel.");
                victim.dropMessage(5, "Cross changing channel.");
                
                if (victim.getMapId() != c.getPlayer().getMapId()) {
                    final Field map = victim.getClient().getChannelServer().getMapFactory().getMap(c.getPlayer().getMapId());
                    victim.changeMap(map, map.getPortal(0));
                }
                victim.changeChannel(c.getChannel());
            }
            return true;
        }
    }
    
    public static class rgm extends CommandExecute {
        @Override
        public boolean execute(Client c, String[] splitted) {
           Player victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]); 
           if (victim != null) {
                victim.dropMessage("[GM - Responde] " + StringUtil.joinStringFrom(splitted, 2));
                return true;
            } else {
               int ch = FindService.findChannel(splitted[1]);
                if (ch < 0) {
                    PlayerNote.sendNote(c.getPlayer(), splitted[1], StringUtil.joinStringFrom(splitted, 2));
                    c.getPlayer().dropMessage(5, "Não encontrado, nota enviada!");
                    return false;
                }
            }
          return true;
        }
    }
    
    public static class PNPC extends CommandExecute {

        @Override
        public boolean execute(Client c, String[] splitted) {
            if (splitted.length < 1) {
                c.getPlayer().dropMessage(6, "!pnpc <npcid>");
                return false;
            }

            int npcId = Integer.parseInt(splitted[1]);
            MapleNPC npc = MapleLifeFactory.getNPC(npcId);
            if (npc != null && !npc.getName().equals("MISSINGNO")) {
                try {
                    npc.setPosition(c.getPlayer().getPosition());
                    npc.setCy(c.getPlayer().getPosition().y);
                    npc.setRx0(c.getPlayer().getPosition().x + 50);
                    npc.setRx1(c.getPlayer().getPosition().x - 50);
                    npc.setFh(c.getPlayer().getMap().getFootholds().findBelow(c.getPlayer().getPosition()).getId());
                    npc.getStats().setCustom(false);

                    Connection con = DatabaseConnection.getConnection();
                    try (PreparedStatement ps = con.prepareStatement("INSERT INTO spawns ( idd, f, fh, cy, rx0, rx1, type, x, y, mid ) VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ? )")) {
                        ps.setInt(1, npcId);
                        ps.setInt(2, 0);
                        ps.setInt(3, c.getPlayer().getMap().getFootholds().findBelow(c.getPlayer().getPosition()).getId());
                        ps.setInt(4, c.getPlayer().getPosition().y);
                        ps.setInt(5, c.getPlayer().getPosition().x + 50);
                        ps.setInt(6, c.getPlayer().getPosition().x - 50);
                        ps.setString(7, "n");
                        ps.setInt(8, c.getPlayer().getPosition().x);
                        ps.setInt(9, c.getPlayer().getPosition().y);
                        ps.setInt(10, c.getPlayer().getMapId());
                        ps.executeUpdate();
                    }
                    c.getPlayer().getMap().addMapObject(npc);
                    c.getPlayer().getMap().broadcastMessage(PacketCreator.SpawnNPC(npc));
                } catch (SQLException ex) {
                    c.getPlayer().dropMessage(6, "Failed to save NPC to the database");
                }
            } else {
                c.getPlayer().dropMessage("You have entered an invalid Npc-Id");
                return false;
            }
            return true;
        }
    }
    
    public static class SReactor extends CommandExecute {

        @Override
        public boolean execute(Client c, String[] splitted) {
            ReactorStats reactorSt = ReactorFactory.getReactor(Integer.parseInt(splitted[1]));
            Reactor reactor = new Reactor(reactorSt, Integer.parseInt(splitted[1]));
            reactor.setDelay(-1);
            reactor.setPosition(c.getPlayer().getPosition());
            c.getPlayer().getMap().spawnReactor(reactor);
            return true;
        }
    }
    
    public static class PMOB extends CommandExecute {

        @Override
        public boolean execute(Client c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(6, "!pmob <mobid> <mobTime>");
                return true;
            }

            int npcId = Integer.parseInt(splitted[1]);
            int mobTime = Integer.parseInt(splitted[2]);
            final MapleMonster mob = MapleLifeFactory.getMonster(npcId);
            if (mob != null && !mob.getName().equals("MISSINGNO")) {
                try {
                    mob.setPosition(c.getPlayer().getPosition());
                    mob.setCy(c.getPlayer().getPosition().y);
                    mob.setRx0(c.getPlayer().getPosition().x + 50);
                    mob.setRx1(c.getPlayer().getPosition().x - 50);
                    mob.setFh(c.getPlayer().getMap().getFootholds().findBelow(c.getPlayer().getPosition()).getId());
                    
                    Connection con = DatabaseConnection.getConnection();
                    try (PreparedStatement ps = con.prepareStatement("INSERT INTO spawns ( idd, f, fh, cy, rx0, rx1, type, x, y, mid, mobtime ) VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? )")) {
                        ps.setInt(1, npcId);
                        ps.setInt(2, 0);
                        ps.setInt(3, c.getPlayer().getMap().getFootholds().findBelow(c.getPlayer().getPosition()).getId());
                        ps.setInt(4, c.getPlayer().getPosition().y);
                        ps.setInt(5, c.getPlayer().getPosition().x + 50);
                        ps.setInt(6, c.getPlayer().getPosition().x - 50);
                        ps.setString(7, "m");
                        ps.setInt(8, c.getPlayer().getPosition().x);
                        ps.setInt(9, c.getPlayer().getPosition().y);
                        ps.setInt(10, c.getPlayer().getMapId());
                        ps.setInt(11, mobTime);
                        ps.executeUpdate();
                    }
                    c.getPlayer().getMap().addMonsterSpawn(mob, mobTime, -1);
                } catch (SQLException ex) {
                    c.getPlayer().dropMessage(6, "Failed to save NPC to the database");
                }
            } else {
                c.getPlayer().dropMessage("You have entered an invalid Npc-Id");
                return false;
            }
            return true;
        }
    }
    
    public static class ShowTrace extends CommandExecute {
        @Override
        public boolean execute(Client c, String[] splitted) {
            if (splitted.length < 2) {
                throw new IllegalArgumentException();
            }
            Thread[] threads = new Thread[Thread.activeCount()];
            Thread.enumerate(threads);
            Thread t = threads[Integer.parseInt(splitted[1])];
            c.getPlayer().dropMessage(6, t.toString() + ":");
            for (StackTraceElement elem : t.getStackTrace()) {
                c.getPlayer().dropMessage(6, elem.toString());
            }
            return true;
        }
    }
    
    public static class Threads extends CommandExecute {
        @Override
        public boolean execute(Client c, String[] splitted) {
            Thread[] threads = new Thread[Thread.activeCount()];
            Thread.enumerate(threads);
            String filter = "";
            if (splitted.length > 1) {
                filter = splitted[1];
            }
            for (int i = 0; i < threads.length; i++) {
                String tstring = threads[i].toString();
                if (tstring.toLowerCase().indexOf(filter.toLowerCase()) > -1) {
                    c.getPlayer().dropMessage(6, i + ": " + tstring);
                }
            }
            return true;
        }
    }
}
