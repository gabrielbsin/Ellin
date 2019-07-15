/*
 This file is part of the OdinMS Maple Story Server
 Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as
 published by the Free Software Foundation version 3 as published by
 the Free Software Foundation. You may not use, modify or distribute
 this program under any other version of the GNU Affero General Public
 License.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package server.quest.actions;

import client.Client;
import client.player.Player;
import client.player.PlayerJob;
import client.player.inventory.Inventory;
import client.player.inventory.types.InventoryType;
import client.player.inventory.Item;
import constants.ItemConstants;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import packet.creators.PacketCreator;
import provider.MapleData;
import provider.MapleDataTool;
import server.itens.InventoryManipulator;
import server.quest.MapleQuest;
import server.quest.MapleQuestActionType;
import tools.Pair;
import tools.Randomizer;

/**
 *
 * @author Tyler (Twdtwd)
 */
public final class ItemAction extends MapleQuestAction {
    List<ItemData> items = new ArrayList<>();

    public ItemAction(MapleQuest quest, MapleData data) {
        super(MapleQuestActionType.ITEM, quest);
        processData(data);
    }

    @Override
    public void processData(MapleData data) {
        for (MapleData iEntry : data.getChildren()) {
            int id = MapleDataTool.getInt(iEntry.getChildByPath("id"));
            int count = MapleDataTool.getInt(iEntry.getChildByPath("count"), 1);

            Integer prop = null;
            MapleData propData = iEntry.getChildByPath("prop");
            if (propData != null) {
                prop = MapleDataTool.getInt(propData);
            }

            int gender = 2;
            if (iEntry.getChildByPath("gender") != null) {
                gender = MapleDataTool.getInt(iEntry.getChildByPath("gender"));
            }

            int job = -1;
            if (iEntry.getChildByPath("job") != null) {
                job = MapleDataTool.getInt(iEntry.getChildByPath("job"));
            }
            items.add(new ItemData(Integer.parseInt(iEntry.getName()), id, count, prop, job, gender));
        }
        Collections.sort(items, (ItemData o1, ItemData o2) -> o1.map - o2.map);
    }
        
    @Override
    public void run(Player p, Integer extSelection) {
        List<Pair<Integer, Integer>> takeItem = new LinkedList<>();
        List<Pair<Integer, Integer>> giveItem = new LinkedList<>();
        
        int props = 0, rndProps = 0, accProps = 0;
        
        for (ItemData item : items) {
            if (item.getProp() != null && item.getProp() != -1 && canGetItem(item, p)) {
                props += item.getProp();
            }
        }
        int extNum = 0;
        if (props > 0) {
            rndProps = Randomizer.nextInt(props);
        }
        for (ItemData iEntry : items) {
            if (!canGetItem(iEntry, p)) {
                continue;
            }
            if (iEntry.getProp() != null) {
                if (iEntry.getProp() == -1) {
                    if (extSelection != extNum++)
                        continue;
                } else {
                    accProps += iEntry.getProp();
                    if (accProps <= rndProps) {
                        continue;
                    } else {
                        accProps = Integer.MIN_VALUE;
                    }
                }
            }
            
            if (iEntry.getCount() < 0) {
                takeItem.add(new Pair<>(iEntry.getId(), iEntry.getCount()));
            } else {
                giveItem.add(new Pair<>(iEntry.getId(), iEntry.getCount()));
            }
        }
        for(Pair<Integer, Integer> iPair: takeItem) {
            InventoryType type = ItemConstants.getInventoryType(iPair.getLeft());
                int quantity = iPair.getRight() * -1; 
                if (type.equals(InventoryType.EQUIP)) {
                    if (p.getInventory(type).countById(iPair.getLeft()) < quantity) {
                        if (p.getInventory(InventoryType.EQUIPPED).countById(iPair.getLeft()) > quantity) {
                            type = InventoryType.EQUIPPED;
                        }
                    }
                }

                InventoryManipulator.removeById(p.getClient(), type, iPair.getLeft(), quantity, true, false);
                p.announce(PacketCreator.GetShowItemGain(iPair.getLeft(), (short) iPair.getRight().shortValue(), true));
        }

        for (Pair<Integer, Integer> iPair: giveItem) {
            InventoryManipulator.addById(p.getClient(), iPair.getLeft(), (short) iPair.getRight().shortValue(), "Add by quest");
            p.announce(PacketCreator.GetShowItemGain(iPair.getLeft(), (short) iPair.getRight().shortValue(), true));
        }
    }
	
    @Override
    public boolean check(Player p, Integer extSelection) {
        List<Pair<Item, InventoryType>> gainList = new LinkedList<>();
        List<Pair<Item, InventoryType>> selectList = new LinkedList<>();
        List<Pair<Item, InventoryType>> randomList = new LinkedList<>();
        
        List<Integer> allSlotUsed = new ArrayList(5);
        for (byte i = 0; i < 5; i++) {
            allSlotUsed.add(0);
        }

        for(ItemData item : items) {
            if (!canGetItem(item, p)) {
                continue;
            }
            
        InventoryType type = ItemConstants.getInventoryType(item.getId());
        if (item.getProp() != null) {
            Item toItem = new Item(item.getId(), (short) 0, (short) item.getCount());
            if (item.getProp() < 0) {
                selectList.add(new Pair<>(toItem, type));
            } else {
                randomList.add(new Pair<>(toItem, type));
            }
        } else {
            if(item.getCount() > 0) {
                Item toItem = new Item(item.getId(), (short) 0, (short) item.getCount());
                gainList.add(new Pair<>(toItem, type));
            } else {
                int quantity = item.getCount() * -1;

                int freeSlotCount = p.getInventory(type).freeSlotCountById(item.getId(), quantity);
                if (freeSlotCount == -1) {
                    if (type.equals(InventoryType.EQUIP) && p.getInventory(InventoryType.EQUIPPED).countById(item.getId()) > quantity)
                        continue;
                        p.dropMessage(1, "Por favor, verifique se você tem itens suficientes no seu inventário.");
                        return false;
                } else {
                        int idx = type.getType() - 1;   // more slots available from the given items!
                        allSlotUsed.set(idx, allSlotUsed.get(idx) - freeSlotCount);
                }
                }
        }
    }

    if (!randomList.isEmpty()) {
        int result;
        Client c = p.getClient();

        List<Integer> rndUsed = new ArrayList(5);
        for (byte i = 0; i < 5; i++) rndUsed.add(allSlotUsed.get(i));

        for (Pair<Item, InventoryType> it: randomList) {
            int idx = it.getRight().getType() - 1;

            result =  InventoryManipulator.checkSpaceProgressively(c, it.getLeft().getItemId(), it.getLeft().getQuantity(), "", rndUsed.get(idx));
            if (result % 2 == 0) {
                p.dropMessage(1, "Por favor, verifique se você tem espaço suficiente no seu inventário.");
                return false;
            }

            allSlotUsed.set(idx, Math.max(allSlotUsed.get(idx), result >> 1));
        }
    }

    if (!selectList.isEmpty()) {
        Pair<Item, InventoryType> selected = selectList.get(extSelection);
        gainList.add(selected);
    }

    if (!Inventory.checkSpots(p, gainList, allSlotUsed)) {
        p.dropMessage(1, "Please check if you have enough space in your inventory.");
        return false;
    }
    return true;
    }
        
    private boolean canGetItem(ItemData item, Player p) {
        if (item.getGender() != 2 && item.getGender() != p.getGender()) {
            return false;
        }
        if (item.getJob() != -1) {
            if (item.getJob() != p.getJob().getId()) {
                if (!PlayerJob.checkJobMask(item.getJob(), p.getJob())) {
                    return false;
                }
            }
        }
        return true;
    }
      
    private class ItemData {
        private final int map, id, count, job, gender;
        private final Integer prop;

        public ItemData(int map, int id, int count, Integer prop, int job, int gender) {
            this.map = map;
            this.id = id;
            this.count = count;
            this.prop = prop;
            this.job = job;
            this.gender = gender;
        }

        public int getId() {
            return id;
        }

        public int getCount() {
            return count;
        }

        public Integer getProp() {
            return prop;
        }

        public int getJob() {
            return job;
        }

        public int getGender() {
            return gender;
        }
    }
} 
