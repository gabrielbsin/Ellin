/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package handling.channel.handler;

import constants.ExperienceConstants;
import client.player.Player;
import client.Client;
import client.player.inventory.Inventory;
import client.player.inventory.types.InventoryType;
import client.player.inventory.Item;
import client.player.inventory.ItemPet;
import client.player.inventory.ItemPetCommand;
import client.player.inventory.ItemPetFactory;
import client.player.violation.AutobanManager;
import client.player.violation.CheatingOffense;
import constants.ItemConstants;
import static handling.channel.handler.MovementParse.parseMovement;
import handling.mina.PacketReader;
import java.util.List;
import java.util.Random;
import packet.creators.PacketCreator;
import packet.creators.PetPackets;
import server.itens.InventoryManipulator;
import server.itens.ItemInformationProvider;
import server.maps.FieldLimit;
import server.movement.LifeMovementFragment;
import tools.Randomizer;

/**
 *
 * @author GabrielSin
 */
public class PetHandler {

    public static void SpawnPet(PacketReader packet, Client c) {
        packet.readInt();
        byte petSlot = packet.readByte();
        packet.readByte();
        c.getPlayer().spawnPet(petSlot, packet.readBool());
    }

    public static void MovePet(PacketReader packet, Client c) {
        final int petId = packet.readInt();
        packet.readInt();
        packet.readPos(); 
        List<LifeMovementFragment> mov = parseMovement(packet);
        Player p = c.getPlayer();
        if (mov != null && p != null && !mov.isEmpty()) { 
            final byte slot = p.getPetIndex(petId);
            if (slot == -1) {
                return;
            }
            p.getPet(slot).updatePosition(mov);
            p.getMap().broadcastMessage(p, PetPackets.MovePet(p.getId(), petId, slot, mov), false);
        }
    }

    public static void PetChat(PacketReader packet, Client c) {
        final int petId = (int) packet.readLong();
        Player p = c.getPlayer();
        if (p == null || p.getMap() == null) {
            return;
        }
        if (p.getPetIndex(petId) == -1 || p.getPetIndex(petId) > 3) {
            p.getCheatTracker().registerOffense(CheatingOffense.PACKET_EDIT, "Tried to pet chat with nonexistent pet");
            return;
        }
        byte type = packet.readByte();
        byte action = packet.readByte();
        int petItemId = p.getPet(p.getPetIndex(petId)).getPetItemId();
        if (action < 1 || ItemPetFactory.IsValidPetAction(petItemId, action)) {
            p.getCheatTracker().registerOffense(CheatingOffense.PACKET_EDIT, "Tried to pet chat with nonexistent action {" + action + "}");
            return;
        }
        String message = packet.readMapleAsciiString();
        if (message.length() > Byte.MAX_VALUE) {
            AutobanManager.getInstance().autoban(p.getClient(), p.getName() + " tried to packet edit with pets.");
            return;
        }
        p.getMap().broadcastMessage(p, PetPackets.PetChat(p.getId(),  type, action, message, p.getPetIndex(petId), c.getPlayer().haveItem(1832000, 1, true, false)), true);
        c.getSession().write(PacketCreator.EnableActions());
    }
    
    public static void PetExcludeItems(PacketReader packet, Client c) {
        Player p = c.getPlayer();
        int uniqueId = (int) packet.readLong();
        if (p == null) {
            return;
        }
        if (p.getPetIndex(uniqueId) == -1) {
            p.getCheatTracker().registerOffense(CheatingOffense.PACKET_EDIT, "Tried to use pet item ignore with nonexistent pet");
            return;
        }
        final ItemPet pet = p.getPetByUID(uniqueId);
        if (pet == null) {
            p.getCheatTracker().registerOffense(CheatingOffense.PACKET_EDIT,  "Tried to use pet item ignore with nonexistent pet");
            return;
        
        }
        pet.getExceptionList().clear();
        byte amount = (byte) Math.min(10, packet.readByte());
        for (int i = 0; i < amount; i++) {
            pet.addItemException(packet.readInt());
        }
    }

    public static void PetCommand(PacketReader packet, Client c) {
        final byte petIndex = c.getPlayer().getPetIndex((int) packet.readLong());
        if (petIndex == -1) {
            c.getPlayer().getCheatTracker().registerOffense(CheatingOffense.PACKET_EDIT, "Tried to use pet command with nonexistent pet");
            return;
        }
        ItemPet pet =  c.getPlayer().getPet(petIndex);
        packet.readByte();

        byte command = packet.readByte();
        final ItemPetCommand petCommand = ItemPetFactory.getPetCommand(pet.getPetItemId(), (int) command);
        if (petCommand == null) {
            c.getPlayer().getCheatTracker().registerOffense(CheatingOffense.PACKET_EDIT, "Tried to use nonexistent pet command");
            return;
        }
        boolean success = false;	
        if (Randomizer.nextInt(99) <= petCommand.getProbability()) {
                success = true;
                if (pet.getCloseness() < 30000) {
                    int newCloseness = pet.getCloseness() + petCommand.getIncrease();
                    if (newCloseness > 30000) {
                        newCloseness = 30000;
                    }
                    pet.setCloseness(newCloseness);
                    if (newCloseness >= ExperienceConstants.getClosenessNeededForLevel(pet.getLevel() + 1)) {
                        pet.setLevel(pet.getLevel() + 1);
                        c.getSession().write(PetPackets.ShowOwnPetLevelUp(petIndex));
                        c.getPlayer().getMap().broadcastMessage(PetPackets.ShowPetLevelUp(c.getPlayer(), petIndex));
                    }
                    c.getSession().write(PetPackets.updatePet(pet, true));
                }
        }
        Player p = c.getPlayer();
        p.getMap().broadcastMessage(p, PetPackets.CommandPetResponse(p.getId(), command, petIndex, success, false), true);
        c.getSession().write(PacketCreator.EnableActions());
    }

    public static void PetFood(PacketReader r, Client c) {
        Player p = c.getPlayer();
        int previousFullness = 100;
        ItemPet pet = null;
        if (p == null) {
            return;
        }
        for (final ItemPet pets : p.getPets()) {
            if (pets.getSummoned()) {
                if (pets.getFullness() < previousFullness) {
                    previousFullness = pets.getFullness();
                    pet = pets;
                }
            }
        }
        if (pet == null) {
            c.getSession().write(PacketCreator.EnableActions());
            return;
        }
        r.readInt();
        short foodSlot = r.readShort();
        int foodItemId = r.readInt();
        
        Item petFood = c.getPlayer().getInventory(InventoryType.USE).getItem((byte) foodSlot);
        if (petFood == null || petFood.getItemId() != foodItemId || petFood.getQuantity() <= 0 || foodItemId / 10000 != 212) {
            p.getCheatTracker().registerOffense(CheatingOffense.PACKET_EDIT, "Tried to use nonexistent pet food");
            return;
        }
        
        boolean gainCloseness = new Random().nextInt(101) <= 50;
        short closeness = pet.getCloseness();
        byte fullness = pet.getFullness();
        
        c.lockClient();
        try {
            if (fullness < 100) {
                int newFullness = fullness + 30;
                if (newFullness > 100) {
                    newFullness = 100;
                }
                
                pet.setFullness(newFullness);
                final byte index = p.getPetIndex(pet);

                if (gainCloseness && closeness < 30000) {
                    int newCloseness = closeness + 1;
                    if (newCloseness > 30000) {
                        newCloseness = 30000;
                    }
                    
                    pet.setCloseness(newCloseness);
                    if (newCloseness >= ExperienceConstants.getClosenessNeededForLevel(pet.getLevel() + 1)) {
                        pet.setLevel(pet.getLevel() + 1);

                        c.getSession().write(PetPackets.ShowOwnPetLevelUp(index));
                        p.getMap().broadcastMessage(PetPackets.ShowPetLevelUp(c.getPlayer(), index));
                    }
                }
                c.getSession().write(PetPackets.updatePet(pet, true));
                p.getMap().broadcastMessage(p, PetPackets.CommandPetResponse(p.getId(), (byte) 1, index, true, true), true);
            } else {
                if (gainCloseness) {
                    int newCloseness = closeness - 1;
                    if (newCloseness < 0) {
                        newCloseness = 0;
                    }
                    pet.setCloseness(newCloseness);
                    if (newCloseness < ExperienceConstants.getClosenessNeededForLevel(pet.getLevel())) {
                        pet.setLevel(pet.getLevel() - 1);
                    }
                }
                c.getSession().write(PetPackets.updatePet(pet, true));
                p.getMap().broadcastMessage(p, PetPackets.CommandPetResponse(p.getId(), (byte) 1, c.getPlayer().getPetIndex(pet), false, true), true);
            }
        } finally {
            c.unlockClient();
        }
        
        InventoryManipulator.removeById(c, InventoryType.USE, foodItemId, 1, true, false);
        c.getSession().write(PacketCreator.EnableActions());
    }

    public static void PetAutoPotion(PacketReader r, Client c) {
        Player p = c.getPlayer();
       
        int uniqueId = (int) r.readLong();
        r.readByte();
        r.readInt();
        short slot = r.readShort();
        int itemId = r.readInt();
        
        Inventory useInv = p.getInventory(InventoryType.USE);
        useInv.lockInventory();
        try {
            if (!p.isAlive() || p.getPetIndex(uniqueId) < 0 || p.getMap() == null) {
                p.announce(PacketCreator.EnableActions());
                return;
            }
            if (p.getPetIndex(uniqueId) == -1) {
                p.getCheatTracker().registerOffense(CheatingOffense.PACKET_EDIT, "Tried to use pet auto potion with nonexistent pet");
                return;
            }

            Item toUse = c.getPlayer().getInventory(InventoryType.USE).getItem(slot);
            ItemInformationProvider ii = ItemInformationProvider.getInstance();
            if (toUse == null || toUse.getItemId() != itemId || toUse.getQuantity() < 0 ) {
                p.getCheatTracker().registerOffense(CheatingOffense.PACKET_EDIT, "Tried to use nonexistent potion for pet auto potion");
                return;
            }
            if (p.getAutoHpPot() != toUse.getItemId() && p.getAutoMpPot() != toUse.getItemId()) {
                p.getCheatTracker().registerOffense(CheatingOffense.PACKET_EDIT, "Tried to use wrong potion for pet auto potion");
                return;
            }
            if (p.getAutoHpPot() == toUse.getItemId() && !p.haveItemEquiped(ItemConstants.HP_ITEM) || p.getAutoMpPot() == toUse.getItemId() && !p.haveItemEquiped(ItemConstants.MP_ITEM)) {
                p.getCheatTracker().registerOffense(CheatingOffense.PACKET_EDIT, "Tried to use pet auto potion without equip");
                return;
            }
            if (!FieldLimit.CANNOTUSEPOTION.check(p.getMap().getFieldLimit())) {
                InventoryManipulator.removeFromSlot(c, InventoryType.USE, slot, (short) 1, false);
                ii.getItemEffect(toUse.getItemId()).applyTo(c.getPlayer());
            }
        } finally {
            useInv.unlockInventory();
        }
    }
}
