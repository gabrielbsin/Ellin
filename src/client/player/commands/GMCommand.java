/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package client.player.commands;

import client.Client;
import constants.CommandConstants.CoomandRank;
import handling.channel.ChannelServer;
import handling.world.CheaterData;
import handling.world.World;
import handling.world.service.BroadcastService;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import launch.Start;
import packet.creators.EffectPackets;
import packet.creators.PacketCreator;
import client.player.Player;
import client.player.PlayerJob;
import client.player.PlayerQuery;
import client.player.PlayerStat;
import client.player.commands.object.CommandExecute;
import client.player.commands.object.CommandProcessorUtil;
import static client.player.commands.object.CommandProcessorUtil.getOptionalIntArg;
import client.player.inventory.Equip;
import client.player.inventory.types.InventoryType;
import client.player.inventory.Item;
import client.player.skills.PlayerSkillFactory;
import constants.ItemConstants;
import constants.ServerProperties;
import java.awt.Point;
import server.itens.InventoryManipulator;
import server.itens.ItemInformationProvider;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.maps.Field;
import server.maps.FieldManager;
import server.maps.object.FieldObject;
import server.maps.object.FieldObjectType;
import server.maps.portal.Portal;
import tools.StringUtil;

/**
 * 
 * @author GabrielSin
 */
public class GMCommand {
    
    public static CoomandRank getPlayerLevelRequired() {
        return CoomandRank.GM;
    }

    public static class Hide extends CommandExecute {
        @Override
        public boolean execute(Client c, String[] splitted) {
            PlayerSkillFactory.getSkill(9001004).getEffect(1).applyTo(c.getPlayer());
            return true;
        }
    }
    
    public static class Drop extends CommandExecute {

        @Override
        public boolean execute(Client c, String[] splitted) {
            final int itemId = Integer.parseInt(splitted[1]);
            final short quantity = (short) CommandProcessorUtil.getOptionalIntArg(splitted, 2, 1);
            ItemInformationProvider ii = ItemInformationProvider.getInstance();
            if (ItemConstants.isPet(itemId)) {
                c.getPlayer().dropMessage(5, "Please purchase a pet from the cash shop instead.");
            } else if (!ii.itemExists(itemId)) {
                c.getPlayer().dropMessage(5, itemId + " does not exist.");
            } else {
                Item toDrop;
                if (ItemConstants.getInventoryType(itemId) == InventoryType.EQUIP) {
                    toDrop = ii.randomizeStats((Equip) ii.getEquipById(itemId));
                } else {
                    toDrop = new Item(itemId, (byte) 0, (short) quantity, (byte) 0);
                }
                c.getPlayer().getMap().spawnItemDrop(c.getPlayer(), c.getPlayer(), toDrop, c.getPlayer().getPosition(), true, true);
            }
            return true;
        }
    }
    
    public static class Spawn extends CommandExecute {
        @Override
        public boolean execute(Client c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().yellowMessage("Syntax: !spawn <mobid>");
                return false;
            }

            MapleMonster monster = MapleLifeFactory.getMonster(Integer.parseInt(splitted[1]));
            if (monster == null) {
                c.getPlayer().yellowMessage("monster does not exist.");
                return false;
            }
            if (splitted.length > 2) {
                for (int i = 0; i < Integer.parseInt(splitted[2]); i++) {
                    c.getPlayer().getMap().spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(Integer.parseInt(splitted[1])), c.getPlayer().getPosition());
                }
            } else {
                c.getPlayer().getMap().spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(Integer.parseInt(splitted[1])), c.getPlayer().getPosition());
            }
            return true;
        }
    }
    
    public static class Level extends CommandExecute {
        @Override
        public boolean execute(Client c, String[] splitted) {
            c.getPlayer().setLevel(Short.parseShort(splitted[1]));
            c.getPlayer().levelUp(false);
            c.getPlayer().setExp(0);
            c.getPlayer().getStat().updateSingleStat(PlayerStat.EXP, 0);
            return true;
        }
    }
    
    public static class Event extends CommandExecute {
        @Override
        public boolean execute(Client c, String[] splitted) {
            if (!c.getChannelServer().getEventStarted()) {
                int mapId = getOptionalIntArg(splitted, 1, c.getPlayer().getMapId());
                c.getChannelServer().setEvent(true);
                c.getChannelServer().setEventMap(mapId);
                BroadcastService.broadcastMessage(PacketCreator.ServerNotice(6, "[Event] Event started on channel (" + c.getChannel() + "). Please use @joinevent to participate in the event."));
            } else {
                c.getChannelServer().setEvent(false);
                BroadcastService.broadcastMessage(PacketCreator.ServerNotice(6, "[Event] Entrance to the event has been closed."));
            }
          return true;
        }
    }
    
    public static class Zakum extends CommandExecute {
        @Override
        public boolean execute(Client c, String[] splitted) {
            c.getPlayer().getMap().spawnFakeMonsterOnGroundBelow(MapleLifeFactory.getMonster(8800000), c.getPlayer().getPosition());
            for (int x = 8800003; x < 8800011; x++) {
                c.getPlayer().getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(x), c.getPlayer().getPosition());
            }
            return true;
        }
    }
    
    public static class Horntail extends CommandExecute {
        @Override
        public boolean execute(Client c, String[] splitted) {
            final Point targetPoint = c.getPlayer().getPosition();
            final Field targetMap = c.getPlayer().getMap();
              
            // Todo : horntail
            //targetMap.spawnHorntailOnGroundBelow(targetPoint);
            return true;
        }
    }
    
    public static class Cake extends CommandExecute {
        @Override
        public boolean execute(Client c, String[] splitted) {
            MapleMonster monster = MapleLifeFactory.getMonster(9400606);
            if (splitted.length > 1) {
                double mobHp = Double.parseDouble(splitted[1]);
                int newHp = (mobHp <= 0) ? Integer.MAX_VALUE : ((mobHp > Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int) mobHp);

                monster.getStats().setHp(newHp);
                monster.setHp(newHp);
            }

            c.getPlayer().getMap().spawnMonsterOnGroundBelow(monster, c.getPlayer().getPosition());
            return true;
        }
    }
    
    public static class Papu extends CommandExecute {
        @Override
        public boolean execute(Client c, String[] splitted) {
            c.getPlayer().getMap().spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(8500001), c.getPlayer().getPosition());
            return true;
        }
     }
    
    public static class Map extends CommandExecute {
        @Override
        public boolean execute(Client c, String[] splitted) {
            int mapid;
            try {
                mapid = Integer.parseInt(splitted[1]);
            } catch (NumberFormatException mwa) {
                return false;
            }
            Field warp = c.getPlayer().getWarpMap(mapid);
            if (warp == null) {
                c.getPlayer().yellowMessage("Map ID or name " + getOptionalIntArg(splitted, 2, 0) + " is invalid.");
                return false;
            } else {
                c.getPlayer().changeMap(warp);
            }
            return true;
        }
    }
    
    public static class Bomb extends CommandExecute {
        @Override
        public boolean execute(Client c, String[] splitted) {
            if (splitted.length > 1) {
                for (int i = 0; i < Integer.parseInt(splitted[1]); i++) {
                    c.getPlayer().spawnBomb();
                }
                c.getPlayer().dropMessage("Planted " + splitted[1] + " bombs.");
                return true;
            } else {
                 c.getPlayer().spawnBomb();
                c.getPlayer().dropMessage("Planted a bomb.");
                return true;
            }
        }
    }
    
    public static class KillAll extends CommandExecute {
        @Override
        public boolean execute(Client c, String[] splitted) {
            Field map = c.getPlayer().getMap();
            MapleMonster monster = MapleLifeFactory.getMonster(Integer.parseInt(splitted[1]));
            List<FieldObject> monsters = map.getMapObjectsInRange(c.getPlayer().getPosition(), Double.POSITIVE_INFINITY, Arrays.asList(FieldObjectType.MONSTER));

            int count = 0;
            for (FieldObject monstermo : monsters) {
                monster = (MapleMonster) monstermo;
                if (!monster.getStats().isFriendly() && !(monster.getId() >= 8810010 && monster.getId() <= 8810018)) {
                    map.damageMonster(c.getPlayer(), monster, Integer.MAX_VALUE);
                    count++;
                }
            }
            c.getPlayer().dropMessage(5, "Killed " + count + " monsters.");
            return true;
        }
    }
    
    public static class Levelup extends CommandExecute {
        @Override
        public boolean execute(Client c, String[] splitted) {
            c.getPlayer().levelUp(true);
            c.getPlayer().getStat().updateSingleStat(PlayerStat.EXP, 0);
            return true;
        }
    }
    
    public static class Clock extends CommandExecute {
        @Override
        public boolean execute(Client c, String[] splitted) {
            c.getPlayer().getMap().displayClock(c.getPlayer(), getOptionalIntArg(splitted, 1, 60));
            return true;
        }
    }
    
    public static class Buffme extends CommandExecute {
        @Override
        public boolean execute(Client c, String[] splitted) {
            final int[] array = {9001000, 9101002, 9101003, 9101008, 2001002, 1101007, 1005, 2301003, 5121009, 1111002, 4111001, 4111002, 4211003, 4211005, 1321000, 2321004, 3121002};
            for (int i : array) {
                PlayerSkillFactory.getSkill(i).getEffect(PlayerSkillFactory.getSkill(i).getMaxLevel()).applyTo(c.getPlayer());
            }
            return true;
        }
    }
    
    public static class Killmap extends CommandExecute {
        @Override
        public boolean execute(Client c, String[] splitted) {
            int players = 0;
            for (Player mch : c.getPlayer().getMap().getCharacters()) {
                if (mch != null && !mch.isGameMaster()) {
                    mch.kill();
                    players++;
                }
            }
            c.getPlayer().dropMessage("O total de " + players + " jogadores estao com HP zerado.");
            return true;
        }
    }
    
    public static class Mutemap extends CommandExecute {
        @Override
        public boolean execute(Client c, String[] splitted) {
            if (c.getPlayer().getMap().getProperties().getProperty("mute").equals(Boolean.TRUE)) {
                c.getPlayer().dropMessage("The map is already set to mute.");
                return false;
            }
            c.getPlayer().getMap().getProperties().setProperty("mute", Boolean.TRUE);
            c.getPlayer().dropMessage("The map [" + c.getPlayer().getMapName(c.getPlayer().getMapId()) + "] is muted.");
            return true;
        }
    }
    
    public static class Unmutemap extends CommandExecute {
        @Override
        public boolean execute(Client c, String[] splitted) {
            if (c.getPlayer().getMap().getProperties().getProperty("mute").equals(Boolean.FALSE)) {
                c.getPlayer().dropMessage("The 'no' map is set to mute.");
                return false;
            }
            c.getPlayer().getMap().getProperties().setProperty("mute", Boolean.FALSE);
            c.getPlayer().dropMessage("The map [" + c.getPlayer().getMapName(c.getPlayer().getMapId()) + "] is not muted.");
            return true;
        }
    }
    
    public static class Unbuffmap extends CommandExecute {
        @Override
        public boolean execute(Client c, String[] splitted) {
            for (Player map :  c.getPlayer().getMap().getCharacters()) {
                if (map != null && map != c.getPlayer()) {
                    map.cancelAllBuffs(false);
                }
            }
           return true;
        }
    }
    
    public static class Setall extends CommandExecute {
        @Override
        public boolean execute(Client c, String[] splitted) {
            if (splitted.length < 1) {
                c.getPlayer().dropMessage(6, "!setall <quantity stat>");
                return false;
            }
            final int x = Short.parseShort(splitted[1]);
            c.getPlayer().getStat().setStr(x);
            c.getPlayer().getStat().setDex(x);
            c.getPlayer().getStat().setInt(x);
            c.getPlayer().getStat().setLuk(x);
            c.getPlayer().getStat().updateSingleStat(PlayerStat.STR, x);
            c.getPlayer().getStat().updateSingleStat(PlayerStat.DEX, x);
            c.getPlayer().getStat().updateSingleStat(PlayerStat.INT, x);
            c.getPlayer().getStat().updateSingleStat(PlayerStat.LUK, x);
            return true;
        }
    }
    
    public static class Whereami extends CommandExecute {
        @Override
        public boolean execute(Client c, String[] splitted) {
           c.getPlayer().dropMessage("You are on map <" + c.getPlayer().getMap().getId() + ">.");
           return true;
        }
    }
    
    public static class Job extends CommandExecute {
        @Override
        public boolean execute(Client c, String[] splitted) {
            int jobId = Integer.parseInt(splitted[1]);
            if (PlayerJob.getById(jobId) != null) {
                c.getPlayer().changeJob(PlayerJob.getById(jobId));
            } else {
                c.getPlayer().dropMessage("Job not found!");
                return false;
            }
            return true;
        }
    }
    
    public static class Fullhp extends CommandExecute {
        @Override
        public boolean execute(Client c, String[] splitted) {
            c.getPlayer().getStat().setHp(c.getPlayer().getStat().getMaxHp());
            c.getPlayer().getStat().setMp(c.getPlayer().getStat().getMaxMp());
            c.getPlayer().getStat().updateSingleStat(PlayerStat.HP, c.getPlayer().getStat().getMaxHp());
            c.getPlayer().getStat().updateSingleStat(PlayerStat.MP, c.getPlayer().getStat().getMaxMp());
            return true;
        }
    }
    
    public static class GainMeso extends CommandExecute {
        @Override
        public boolean execute(Client c, String[] splitted) {
            if (splitted.length >= 2) {
                c.getPlayer().gainMeso(Integer.parseInt(splitted[1]), true);  
                return true;
            } else {
                c.getPlayer().gainMeso(Integer.MAX_VALUE - c.getPlayer().getMeso(), true);
                return true;
            }
        }
    }
    
    public static class HealHere extends CommandExecute {
        @Override
        public boolean execute(Client c, String[] splitted) {
            Player p = c.getPlayer();
            for (Player mch : p.getMap().getCharacters()) {
                if (mch != null) {
                    c.getPlayer().getStat().setHp(c.getPlayer().getStat().getMaxHp());
                    c.getPlayer().getStat().updateSingleStat(PlayerStat.HP, c.getPlayer().getStat().getMaxHp());
                    c.getPlayer().getStat().setMp(c.getPlayer().getStat().getMaxMp());
                    c.getPlayer().getStat().updateSingleStat(PlayerStat.MP, c.getPlayer().getStat().getMaxMp());
                }
            }
            return true;
        }
    }
    
    public static class DC extends CommandExecute {
        @Override
        public boolean execute(Client c, String[] splitted) {
            int level = 0;
            Player victim;
            if (splitted[1].charAt(0) == '-') {
                level = StringUtil.countCharacters(splitted[1], 'f');
                victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[2]);
            } else {
                victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            }
            if (level < 2 && victim != null) {
                victim.getClient().getSession().close();
                if (level >= 1) {
                    victim.getClient().disconnect(true, false);
                }
                return true;
            } else {
                c.getPlayer().dropMessage(6, "Please use dc -f instead, or the victim does not exist.");
                return false;
            }
        }
    }
    
    public static class Online extends CommandExecute {
        @Override
        public boolean execute(Client c, String[] splitted) {      
            c.getPlayer().yellowMessage("=========================");
            c.getPlayer().yellowMessage("Total Online " + ServerProperties.Login.SERVER_NAME + ": " + World.getConnected());
            c.getPlayer().yellowMessage("=========================");
            c.getPlayer().dropMessage(6, "<Channel (" + c.getChannel() + ")>");
            c.getPlayer().dropMessage(6, c.getChannelServer().getPlayerStorage().getOnlinePlayers(true));
            return true;
        }
    }
    
    public static class GiftNX extends CommandExecute {
        @Override
        public boolean execute(Client c, String[] splitted) {
            ChannelServer cserv = c.getChannelServer();
            Player receiver = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
            if (receiver == null) {
                c.getPlayer().dropMessage("Receiver not found!");
                return false;
            }
            cserv.getPlayerStorage().getCharacterByName(splitted[1]).getCashShop().gainCash(1, Integer.parseInt(splitted[2]));
            c.getPlayer().dropMessage(6, "Done, it was sent to " + splitted[1] + " the amount of " + splitted[2] + ".");
            return true;
        }
    }
    
    public static class Say extends CommandExecute {
        @Override
        public boolean execute(Client c, String[] splitted) {
            if (splitted.length > 1) {
                StringBuilder sb = new StringBuilder();
                sb.append("[");
                sb.append(c.getPlayer().getName());
                sb.append("] ");
                sb.append(StringUtil.joinStringFrom(splitted, 1));
                BroadcastService.broadcastMessage(PacketCreator.ServerNotice(6, sb.toString()));
            } else {
                c.getPlayer().dropMessage(6, "Syntax: !say <message>");
                return false;
            }
            return true;
        }
    }
    
    public static class Warn extends CommandExecute {
        @Override
        public boolean execute(Client c, String[] splitted) {
            if (splitted.length > 1) {
                StringBuilder sb = new StringBuilder();
                sb.append(StringUtil.joinStringFrom(splitted, 1));
                BroadcastService.broadcastMessage(PacketCreator.ServerNotice(1, sb.toString()));
            } else {
                c.getPlayer().dropMessage(6, "Syntax: !warn <message>");
                return false;
            }
            return true;
        }
    }
    
    public static class OpenPortalId extends CommandExecute {
        @Override
        public boolean execute(Client c, String[] splitted) {
            if (splitted.length < 2){
                c.getPlayer().yellowMessage("Syntax: !openportal <portalid>");
                return false;
            }
            BroadcastService.broadcastMessage(PacketCreator.ServerNotice(5, "The portal has now opened. Try pressing the up arrow key on portal!"));
            c.getPlayer().getMap().getPortal(splitted[1]).setPortalState(true);
            return true;
        }
    }
    
    public static class ClosePortalId extends CommandExecute {
        @Override
        public boolean execute(Client c, String[] splitted) {
            if (splitted.length < 2){
                c.getPlayer().yellowMessage("Syntax: !openportal <portalid>");
                return false;
            }
            BroadcastService.broadcastMessage(PacketCreator.ServerNotice(5, "The portal has been closed!"));
            c.getPlayer().getMap().getPortal(splitted[1]).setPortalState(false);
            return true;
        }
    }
    
    public static class ClosePortals extends CommandExecute {
        @Override
        public boolean execute(Client c, String[] splitted) {
            for (Portal portal : c.getPlayer().getMap().getPortals()){
                portal.setPortalState(false);
            }
            BroadcastService.broadcastMessage(PacketCreator.ServerNotice(5, "The portal has been closed!"));
            return true;
        }
    }
    
    public static class OpenPortals extends CommandExecute {
        @Override
        public boolean execute(Client c, String[] splitted) {
            for (Portal portal : c.getPlayer().getMap().getPortals()){
                portal.setPortalState(true);
            }
            BroadcastService.broadcastMessage(PacketCreator.ServerNotice(5, "The portal has now opened. Try pressing the up arrow key on portal!"));
            return true;
        }
    }
    
    public static class ItemCheck extends CommandExecute {
        @Override
        public boolean execute(Client c, String[] splitted) {
            if (splitted.length < 3 || splitted[1] == null || splitted[1].equals("") || splitted[2] == null || splitted[2].equals("")) {
                c.getPlayer().dropMessage(6, "!itemcheck <playername> <itemid>");
                return false;
            } else {
                int item = Integer.parseInt(splitted[2]);
                Player chr = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                int itemamount = chr.getItemQuantity(item, true);
                if (itemamount > 0) {
                    c.getPlayer().dropMessage(6, chr.getName() + " has " + itemamount + " (" + item + ").");
                } else {
                    c.getPlayer().dropMessage(6, chr.getName() + " doesn't have (" + item + ")");
                }
            }
            return true;
        }
    }
    
    public static class Song extends CommandExecute {
        @Override
        public boolean execute(Client c, String[] splitted) {
            c.getPlayer().getMap().broadcastMessage(EffectPackets.MusicChange(splitted[1]));
            return true;
        }
    }
    
    public static class RemoveItem extends CommandExecute {
        @Override
        public boolean execute(Client c, String[] splitted) {
            if (splitted.length < 3) {
                c.getPlayer().dropMessage(6, "Need <name> <itemid>");
                return false;
            }
            Player chr = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (chr == null) {
                c.getPlayer().dropMessage(6, "This player does not exist");
                return false;
            }
            chr.removeAll(Integer.parseInt(splitted[2]));
            c.getPlayer().dropMessage(6, "All items with the ID " + splitted[2] + " has been removed from the inventory of " + splitted[1] + ".");
            return true;
        }
    }
    
    public static class SpeakMega extends CommandExecute {
        @Override
        public boolean execute(Client c, String[] splitted) {
            Player victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            BroadcastService.broadcastSmega(PacketCreator.ServerNotice(3, victim == null ? c.getChannel() : victim.getClient().getChannel(), victim == null ? splitted[1] : victim.getName() + " : " + StringUtil.joinStringFrom(splitted, 2), true));
            return true;
        }
    }
    
    public static class Speak extends CommandExecute {
        @Override
        public boolean execute(Client c, String[] splitted) {
            Player victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (victim == null) {
                c.getPlayer().dropMessage(5, "unable to find '" + splitted[1]);
                return false;
            } else {
                victim.getMap().broadcastMessage(PacketCreator.GetChatText(victim.getId(), StringUtil.joinStringFrom(splitted, 2), victim.isGameMaster(), (byte) 0));
            }
            return true;
        }
    }

    public static class SpeakMap extends CommandExecute {
        @Override
        public boolean execute(Client c, String[] splitted) {
            for (Player victim : c.getPlayer().getMap().getCharactersThreadsafe()) {
                if (victim.getId() != c.getPlayer().getId()) {
                    victim.getMap().broadcastMessage(PacketCreator.GetChatText(victim.getId(), StringUtil.joinStringFrom(splitted, 1), victim.isGameMaster(), (byte) 0));
                }
            }
            return true;
        }
    }
   
    public static class MesoEveryone extends CommandExecute {
        @Override
        public boolean execute(Client c, String[] splitted) {
            for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                for (Player mch : cserv.getPlayerStorage().getAllCharacters()) {
                    mch.gainMeso(Integer.parseInt(splitted[1]), true);
                }
            }
            return true;
        }
    }
    
    public static class CharInfo extends CommandExecute {

        @Override
        public boolean execute(Client c, String[] splitted) {
            final StringBuilder builder = new StringBuilder();
            final Player other = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (other == null) {
                builder.append("...does not exist");
                c.getPlayer().dropMessage(6, builder.toString());
                return false;
            }
            builder.append(Client.getLogMessage(other, ""));
            builder.append(" at ").append(other.getPosition().x);
            builder.append(" /").append(other.getPosition().y);

            builder.append(" || HP : ");
            builder.append(other.getStat().getHp());
            builder.append(" /");
            builder.append(other.getStat().getCurrentMaxHp());

            builder.append(" || MP : ");
            builder.append(other.getStat().getMp());
            builder.append(" /");
            builder.append(other.getStat().getCurrentMaxMp());

            builder.append(" || WATK : ");
            builder.append(other.getStat().getTotalWatk());
            builder.append(" || MATK : ");
            builder.append(other.getStat().getTotalMagic());
            builder.append(" || MAXDAMAGE : ");
            builder.append(other.getStat().getCurrentMaxBaseDamage());
            
            builder.append(" || STR : ");
            builder.append(other.getStat().getStr());
            builder.append(" || DEX : ");
            builder.append(other.getStat().getDex());
            builder.append(" || INT : ");
            builder.append(other.getStat().getInt());
            builder.append(" || LUK : ");
            builder.append(other.getStat().getLuk());

            builder.append(" || Total STR : ");
            builder.append(other.getStat().getTotalStr());
            builder.append(" || Total DEX : ");
            builder.append(other.getStat().getTotalDex());
            builder.append(" || Total INT : ");
            builder.append(other.getStat().getTotalInt());
            builder.append(" || Total LUK : ");
            builder.append(other.getStat().getTotalLuk());

            builder.append(" || EXP : ");
            builder.append(other.getCurrentExp());

            builder.append(" || hasParty : ");
            builder.append(other.getParty() != null);

            builder.append(" || hasTrade: ");
            builder.append(other.getTrade() != null);

            builder.append(" || remoteAddress: ");

            other.getClient().DebugMessage(builder);

            c.getPlayer().dropMessage(6, builder.toString());
            return true;
        }
    }
    
    public static class Cheaters extends CommandExecute {

        @Override
        public boolean execute(Client c, String[] splitted) {
            List<CheaterData> cheaters = World.getCheaters();
            if (cheaters.isEmpty()) {
                c.getPlayer().dropMessage(6, "There are no cheaters at the moment.");
                return false;
            }
            for (int x = cheaters.size() - 1; x >= 0; x--) {
                CheaterData cheater = cheaters.get(x);
                c.getPlayer().dropMessage(6, cheater.getInfo());
            }
            return true;
        }
    }
    
    public static class Maxskills extends CommandExecute {
        @Override
        public boolean execute(Client c, String[] splitted) {
            c.getPlayer().maxAllSkills();
            return true;
        }
    }
    
    public static class Cleardrops extends CommandExecute {
        @Override
        public boolean execute(Client c, String[] splitted) {
            Field field = c.getPlayer().getMap();
            double range = Double.POSITIVE_INFINITY;
            List<FieldObject> items = field.getMapObjectsInRange(c.getPlayer().getPosition(), range, Arrays.asList(FieldObjectType.ITEM));
            for (FieldObject itemmo : items) {
                field.removeMapObject(itemmo);
                field.broadcastMessage(PacketCreator.RemoveItemFromMap(itemmo.getObjectId(), 4,
                c.getPlayer().getId()));
            }
            c.getPlayer().dropMessage("You have destroyed " + items.size() + " items on the ground.");
            return true;
        }
    }
    
    public static class ExpRate extends CommandExecute {
        @Override
        public boolean execute(Client c, String[] splitted) {
            if (splitted.length > 1) {
                final int rate = Integer.parseInt(splitted[1]);
                if (splitted.length > 2 && splitted[2].equalsIgnoreCase("all")) {
                    for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                        cserv.setExpRate(rate);
                    }
                } else {
                    c.getChannelServer().setExpRate(rate);
                }
                c.getPlayer().dropMessage(6, "Exprate has been changed to " + rate + "x");
            } else {
                c.getPlayer().dropMessage(6, "Syntax: !exprate <number> [all]");
                return false;
            }
            return true;
        }
    }
    
    public static class MesoRate extends CommandExecute {
        @Override
        public boolean execute(Client c, String[] splitted) {
            if (splitted.length > 1) {
                final int rate = Integer.parseInt(splitted[1]);
                if (splitted.length > 2 && splitted[2].equalsIgnoreCase("all")) {
                    for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                        cserv.setMesoRate(rate);
                    }
                } else {
                    c.getChannelServer().setMesoRate(rate);
                }
                c.getPlayer().dropMessage(6, "Exprate has been changed to " + rate + "x");
            } else {
                c.getPlayer().dropMessage(6, "Syntax: !exprate <number> [all]");
                return false;
            }
            return true;
        }
    }
    
    public static class DropRate extends CommandExecute {
        @Override
        public boolean execute(Client c, String[] splitted) {
            if (splitted.length > 1) {
                final int rate = Integer.parseInt(splitted[1]);
                if (splitted.length > 2 && splitted[2].equalsIgnoreCase("all")) {
                    for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                        cserv.setDropRate(rate);
                    }
                } else {
                    c.getChannelServer().setDropRate(rate);
                }
                c.getPlayer().dropMessage(6, "Drop Rate has been changed to " + rate + "x");
            } else {
                c.getPlayer().dropMessage(6, "Syntax: !droprate <number> [all]");
                return false;
            }
            return true;
        }
    }
    
    public static class QuestRate extends CommandExecute {
        @Override
        public boolean execute(Client c, String[] splitted) {
            if (splitted.length > 1) {
                final int rate = Integer.parseInt(splitted[1]);
                if (splitted.length > 2 && splitted[2].equalsIgnoreCase("all")) {
                    for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                        cserv.setQuestRate(rate);
                    }
                } else {
                    c.getChannelServer().setQuestRate(rate);
                }
                c.getPlayer().dropMessage(6, "Quest Rate has been changed to " + rate + "x");
            } else {
                c.getPlayer().dropMessage(6, "Syntax: !questrate <number> [all]");
                return false;
            }
            return true;
        }
    }
    
    public static class Fame extends CommandExecute {
        @Override
        public boolean execute(Client c, String[] splitted) {
            ChannelServer cserv = c.getChannelServer();
            Player victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
            if (victim != null) {
                victim.setFame(getOptionalIntArg(splitted, 2, 1));
                victim.getStat().updateSingleStat(PlayerStat.FAME, victim.getFame());
                return true;
            } else {
                c.getPlayer().dropMessage("Player not found!");
                return false;
            }
        }
    }
    
    public static class EventRules extends CommandExecute {
        @Override
        public boolean execute(Client c, String[] splitted) {
            for (Player p :  c.getPlayer().getMap().getAllPlayers()) {
                p.announce(PacketCreator.showEventInstructions());
            }
            return true;
        }
    }
    
    public static class SetName extends CommandExecute {
        @Override
        public boolean execute(Client c, String[] splitted) {
            if (splitted.length != 3) {
                return false;
            }
            ChannelServer cserv = c.getChannelServer();
            Player victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
            String newname = splitted[2];
            if (splitted.length == 3) {
                if (PlayerQuery.getIdByName(newname, 0) == -1) {
                    if (victim != null) {
                        victim.getClient().disconnect(false, false);
                        victim.getClient().getSession().close();
                        victim.setName(newname, true);
                        c.getPlayer().dropMessage(splitted[1] + " is now named " + newname + "");
                        return true;
                    } else {
                        c.getPlayer().dropMessage("The player " + splitted[1] + " is either offline or not in this channel");
                        return false;
                    }
                } else {
                    c.getPlayer().dropMessage("Character name in use.");
                    return false;
                }
            } else {
                c.getPlayer().dropMessage("Incorrect syntax !");
                return false;
            }
        }
    }
    
    public static class Uptime extends CommandExecute {
        @Override
        public boolean execute(Client c, String[] splitted) {
            c.getPlayer().dropMessage(6, "Server has been up for " + StringUtil.getReadableMillis(Start.startTime, System.currentTimeMillis()));
            return true;
        }
    }
    
    public static class item extends CommandExecute {
        @Override
        public boolean execute(Client c, String[] splitted) {
            
            ItemInformationProvider ii = ItemInformationProvider.getInstance();
            final int itemId = Integer.parseInt(splitted[1]);
            final short quantity = (short) getOptionalIntArg(splitted, 2, 1);
            
            if (ItemConstants.isPet(itemId)) {
                c.getPlayer().dropMessage(5, "Please purchase a pet from the cash shop instead.");
                return false;
            } else if (!ii.itemExists(itemId)) {
                c.getPlayer().dropMessage(5, itemId + " does not exist.");
                return false;
            } else {
                Item item;
                if (ItemConstants.getInventoryType(itemId) == InventoryType.EQUIP) {
                    item = ii.randomizeStats((Equip) ii.getEquipById(itemId));
                } else {
                    item = new Item(itemId, (byte) 0, quantity);
                }
                item.setOwner(c.getPlayer().getName());
                InventoryManipulator.addbyItem(c, item);
                return true;
            }
        }
    }
    
    public static class GoTo extends CommandExecute {

        private static final HashMap<String, Integer> gotoMaps = new HashMap<>();

        static {
            gotoMaps.put("gmmap", 180000000);
            gotoMaps.put("southperry", 2000000);
            gotoMaps.put("amherst", 1010000);
            gotoMaps.put("henesys", 100000000);
            gotoMaps.put("ellinia", 101000000);
            gotoMaps.put("perion", 102000000);
            gotoMaps.put("kerning", 103000000);
            gotoMaps.put("lithharbour", 104000000);
            gotoMaps.put("sleepywood", 105040300);
            gotoMaps.put("florina", 110000000);
            gotoMaps.put("orbis", 200000000);
            gotoMaps.put("happyville", 209000000);
            gotoMaps.put("elnath", 211000000);
            gotoMaps.put("ludibrium", 220000000);
            gotoMaps.put("aquaroad", 230000000);
            gotoMaps.put("leafre", 240000000);
            gotoMaps.put("mulung", 250000000);
            gotoMaps.put("herbtown", 251000000);
            gotoMaps.put("omegasector", 221000000);
            gotoMaps.put("koreanfolktown", 222000000);
            gotoMaps.put("newleafcity", 600000000);
            gotoMaps.put("sharenian", 990000000);
            gotoMaps.put("pianus", 230040420);
            gotoMaps.put("horntail", 240060200);
            gotoMaps.put("chorntail", 240060201);
            gotoMaps.put("mushmom", 100000005);
            gotoMaps.put("griffey", 240020101);
            gotoMaps.put("manon", 240020401);
            gotoMaps.put("zakum", 280030000);
            gotoMaps.put("czakum", 280030001);
            gotoMaps.put("papulatus", 220080001);
            gotoMaps.put("showatown", 801000000);
            gotoMaps.put("zipangu", 800000000);
            gotoMaps.put("ariant", 260000100);
            gotoMaps.put("nautilus", 120000000);
            gotoMaps.put("boatquay", 541000000);
            gotoMaps.put("malaysia", 550000000);
            gotoMaps.put("taiwan", 740000000);
            gotoMaps.put("thailand", 500000000);
            gotoMaps.put("erev", 130000000);
            gotoMaps.put("ellinforest", 300000000);
            gotoMaps.put("kampung", 551000000);
            gotoMaps.put("singapore", 540000000);
            gotoMaps.put("amoria", 680000000);
            gotoMaps.put("timetemple", 270000000);
            gotoMaps.put("pinkbean", 270050100);
            gotoMaps.put("peachblossom", 700000000);
            gotoMaps.put("fm", 910000000);
            gotoMaps.put("freemarket", 910000000);
            gotoMaps.put("oxquiz", 109020001);
            gotoMaps.put("ola", 109030101);
            gotoMaps.put("fitness", 109040000);
            gotoMaps.put("snowball", 109060000);
            gotoMaps.put("cashmap", 741010200);
            gotoMaps.put("golden", 950100000);
            gotoMaps.put("phantom", 610010000);
            gotoMaps.put("cwk", 610030000);
            gotoMaps.put("rien", 140000000);
        }

        @Override
        public boolean execute(Client c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(6, "Syntax: !goto <mapname>");
            } else {
                if (gotoMaps.containsKey(splitted[1])) {
                    Field target = c.getChannelServer().getMapFactory().getMap(gotoMaps.get(splitted[1]));
                    Portal targetPortal = target.getPortal(0);
                    c.getPlayer().changeMap(target, targetPortal);
                } else {
                    if (splitted[1].equals("locations")) {
                        c.getPlayer().dropMessage(6, "Use !goto <location>. Locations are as follows:");
                        StringBuilder sb = new StringBuilder();
                        for (String s : gotoMaps.keySet()) {
                            sb.append(s).append(", ");
                        }
                        c.getPlayer().dropMessage(6, sb.substring(0, sb.length() - 2));
                    } else {
                        c.getPlayer().dropMessage(6, "Invalid command syntax - Use !goto <location>. For a list of locations, use !goto locations.");
                        return false;
                    }
                }
            }
            return true;
        }
    }  
}
 
    

