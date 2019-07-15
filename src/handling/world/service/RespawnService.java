/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package handling.world.service;

import client.player.Player;
import client.player.buffs.DiseaseValueHolder;
import client.player.inventory.types.InventoryType;
import client.player.inventory.ItemPet;
import client.player.inventory.ItemPetFactory;
import handling.channel.ChannelServer;
import handling.world.PlayerCoolDownValueHolder;
import handling.world.World;
import java.util.ArrayList;
import java.util.List;
import packet.creators.PetPackets;
import packet.creators.PacketCreator;
import server.life.MapleMonster;
import server.maps.Field;
import server.maps.FieldItem;

public class RespawnService {
    
    public static class Respawn implements Runnable {
        
        private int numTimes = 0;
        private final ArrayList<Player> chrs = new ArrayList<>();
        private final ArrayList<MapleMonster> mobs = new ArrayList<>();
        private final ArrayList<ItemPet> pets = new ArrayList<>();
        private final List<ChannelServer> cservs = new ArrayList<>(World.channelPerThread);
        
        public Respawn(Integer[] chs, int c) {
            StringBuilder s = new StringBuilder("[Respawn Worker] Registering channels ");
            for (int i = 1; (i <= World.channelPerThread) && (chs.length >= c + i); i++) {
                cservs.add(ChannelServer.getInstance(c + i));
                s.append(c + i).append(" ");
            }
            System.out.println(s.toString());
        }
        
         @Override
        public void run() {
            numTimes++;
            long now = System.currentTimeMillis();
            for (ChannelServer cserv : cservs) {
                if (cserv != null && !cserv.hasFinishedShutdown()) {
                    for (Field map : cserv.getMapFactory().getAllLoadedMaps()) {
                        handleMap(map, numTimes, map.getCharactersSize(), now, chrs, mobs, pets);
                    }
                }
            }
            if (BuddyService.canPrune(now)) {
                BuddyService.prepareRemove();
            }
        }
    }
    
     public static void handleMap(final Field map, final int numTimes, final int size, final long now, ArrayList<Player> chrs, ArrayList<MapleMonster> monsters, ArrayList<ItemPet> pets) {
         if (map.itemCount() > 0) {
	    for (FieldItem item : map.getAllItemsThreadsafe()) {
	        if (item.shouldExpire()) {
		    item.expire(map);
	        } else if (item.shouldFFA()) {
		    item.setDropType((byte) 2);
	        }
	    }
	}
        if (map.getCharactersSize() > 0) {
            if (map.canSpawn()) {
                map.Respawn(false);
            }
            boolean hurt = map.canHurt(now);
            chrs = map.getCharactersThreadsafe(chrs);
            for (Player p : chrs) {
                handleCooldowns(p, numTimes, hurt, now, pets);
            }
        }
    }
    
    public static void handleCooldowns(final Player p, final int numTimes, final boolean hurt, final long now, ArrayList<ItemPet> pets) {
        if (p.getCooldownSize() > 0) {
            for (PlayerCoolDownValueHolder m : p.getAllCooldowns()) {
                if (m.startTime + m.length < now) {
                    final int skill = m.skillId;
                    p.removeCooldown(skill);
                    p.getClient().write(PacketCreator.SkillCooldown(skill, 0));
                }
            }
        }
        if (numTimes % 7 == 0 && p.getMount() != null && p.getMount().canTire(now)) {
            p.getMount().increaseTiredness();
        }
        if (numTimes % 13 == 0) {
            for (ItemPet pet : p.getSummonedPets()) {
                if (pet.getPetItemId() == 5000054 && pet.getSecondsLeft() > 0) {
                    pet.setSecondsLeft(pet.getSecondsLeft() - 1);
                    if (pet.getSecondsLeft() <= 0) {
                        p.unequipPet(pet, true, true);
                        return;
                    }
                }
                int newFullness = pet.getFullness() - ItemPetFactory.getHunger(pet.getPetItemId());
                if (newFullness <= 5) {
                    pet.setFullness(15);
                    p.unequipPet(pet, true, true);
                } else {
                    pet.setFullness(newFullness);
                    p.getClient().write(PetPackets.updatePet(pet, true));
                }
            }
        }
        if (hurt && p.isAlive()) {
            if (p.getInventory(InventoryType.EQUIPPED).findById(p.getMap().getHPDecProtect()) == null) {
                p.getStat().addHP(-p.getMap().getHPDec());
            }
        }
        if (p.getDiseaseSize() > 0) {
            for (DiseaseValueHolder dvh : p.getAllDiseases()) {
                if (dvh.startTime + dvh.length < now) {
                    p.dispelDebuff(dvh.disease);
                }
            }
        }
    }  
}
