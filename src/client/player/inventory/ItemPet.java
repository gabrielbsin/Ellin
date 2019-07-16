/*
This file is part of the OdinMS Maple Story Server
Copyright (C) 2008 ~ 2010 Patrick Huy <patrick.huy@frz.cc> 
Matthias Butz <matze@odinms.de>
Jan Christian Meyer <vimes@odinms.de>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License version 3
as published by the Free Software Foundation. You may not use, modify
or distribute this program under any other version of the
GNU Affero General Public License.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package client.player.inventory;

import client.player.inventory.types.InventoryType;
import tools.ObjectParser;
import client.player.Player;
import java.awt.Point;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.io.Serializable;

import java.util.List;
import database.DatabaseConnection;
import java.util.ArrayList;
import server.itens.ItemInformationProvider;
import server.movement.AbsoluteLifeMovement;
import server.movement.LifeMovement;
import server.movement.LifeMovementFragment;

public class ItemPet implements Serializable {

    private static final long serialVersionUID = 9179541993413738569L;
    
    private String name;
    private Point position;
    private int foothold = 0, stance = 0, uniqueid, petItemId, secondsLeft = 0;
    private byte fullness = 100, level = 1, summoned = 0;
    private short inventorypos = 0; 
    private short closeness = 0;
    private List<Integer> exceptionList;

    private ItemPet(final int petItemId, final int uniqueId) {
        this.exceptionList = new ArrayList<>();
        this.petItemId = petItemId;
        this.uniqueid = uniqueId;
    }

    private ItemPet(final int petItemId, final int uniqueId, final short inventoryPos) {
        this.exceptionList = new ArrayList<>();
        this.petItemId = petItemId;
        this.uniqueid = uniqueId;
        this.inventorypos = inventoryPos;
    }

    public static ItemPet loadDatabase(final int itemId, final int petId, final short inventoryPos) {
        try {
            final ItemPet ret = new ItemPet(itemId, petId, inventoryPos);
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT * FROM pets WHERE petid = ?")) {
                ps.setInt(1, petId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        rs.close();
                        ps.close();
                        return null;
                    }
                    ret.setName(rs.getString("name"));
                    ret.setCloseness(rs.getShort("closeness"));
                    ret.setLevel(rs.getByte("level"));
                    ret.setFullness(rs.getByte("fullness"));
                    ret.setSecondsLeft(rs.getInt("seconds"));
                    String[] excluded = rs.getString("excluded").split(",");
                    for(String id : excluded){
                        if (id.length() > 0){
                            ret.addItemException(ObjectParser.isInt(id));
                        }
                    }
                }
            }
            return ret;
        } catch (SQLException ex) {
            System.err.println("Pet loadDatabase: " + ex);
            return null;
        }
    }

    public final void saveDatabase() {
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("UPDATE pets SET name = ?, level = ?, closeness = ?, fullness = ?, seconds = ?, excluded = ? WHERE petid = ?")) {
                ps.setString(1, name);
                ps.setByte(2, level);
                ps.setShort(3, closeness);
                ps.setByte(4, fullness);
                ps.setInt(5, secondsLeft);
                StringBuilder excluded = new StringBuilder();
                for (int itemId : exceptionList){
                    excluded.append(itemId);
                    excluded.append(",");
                }
                if (excluded.toString().contains(",")) {
                    excluded.setLength(excluded.length() - 1);
                }
                ps.setString(6, excluded.toString());
                ps.setInt(7, uniqueid);
                ps.executeUpdate();
            }
        } catch (final SQLException ex) {
            System.err.println("Pet saveDatabase: " + ex);
        }
    }

    public static ItemPet createPet(final int itemId, final int uniqueId) {
        return createPet(itemId, ItemInformationProvider.getInstance().getName(itemId), 1, 0, 100, uniqueId, itemId == 5000054 ? 18000 : 0);
    }

    public static ItemPet createPet(int itemid, String name, int level, int closeness, int fullness, int uniqueid, int secondsLeft) {
        if (uniqueid <= -1) {
            uniqueid = InventoryIdentifier.getInstance();
        }
        try {
            try (PreparedStatement pse = DatabaseConnection.getConnection().prepareStatement("INSERT INTO pets (petid, name, level, closeness, fullness, seconds) VALUES (?, ?, ?, ?, ?, ?)")) {
                pse.setInt(1, uniqueid);
                pse.setString(2, name);
                pse.setByte(3, (byte) level);
                pse.setShort(4, (short) closeness);
                pse.setByte(5, (byte) fullness);
                pse.setInt(6, secondsLeft);
                pse.executeUpdate();
            }
        } catch (final SQLException ex) {
            System.err.println("Pet createPet: " + ex);
            return null;
        }
        final ItemPet pet = new ItemPet(itemid, uniqueid);
        pet.setName(name);
        pet.setLevel(level);
        pet.setFullness(fullness);
        pet.setCloseness(closeness);
        pet.setSecondsLeft(secondsLeft);
        return pet;
    }

    public final String getName() {
        return name;
    }

    public final void setName(final String name) {
        this.name = name;
    }

     public final boolean getSummoned() {
	return summoned > 0;
    }

    public final byte getSummonedValue() {
        return summoned;
    }

    public final void setSummoned(final int summoned) {
        this.summoned = (byte)summoned;
    }

    public final short getInventoryPosition() {
        return inventorypos;
    }

    public final void setInventoryPosition(final short inventorypos) {
        this.inventorypos = inventorypos;
    }

    public int getUniqueId() {
        return uniqueid;
    }

    public void setUniqueId(int id) {
        this.uniqueid = id;
    }

    public final short getCloseness() {
        return closeness;
    }

    public final void setCloseness(final int closeness) {
        this.closeness = (short) closeness;
    }

    public final byte getLevel() {
        return level;
    }

    public final void setLevel(final int level) {
        this.level = (byte) level;
    }

    public final byte getFullness() {
        return fullness;
    }

    public final void setFullness(final int fullness) {
        this.fullness = (byte) fullness;
    }

    public final int getFoothold() {
        return foothold;
    }

    public final void setFoothold(final int Fh) {
        this.foothold = Fh;
    }

    public final Point getPosition() {
        return position;
    }

    public final void setPosition(final Point pos) {
        this.position = pos;
    }

    public final int getStance() {
        return stance;
    }

    public final void setStance(final int stance) {
        this.stance = stance;
    }

    public final int getPetItemId() {
        return petItemId;
    }

    public final boolean canConsume(final int itemId) {
        final ItemInformationProvider mii = ItemInformationProvider.getInstance();
        for (final int petId : mii.petsCanConsume(itemId)) {
            if (petId == petItemId) {
                return true;
            }
        }
        return false;
    }

    public final void updatePosition(final List<LifeMovementFragment> movement) {
        for (final LifeMovementFragment move : movement) {
            if (move instanceof LifeMovement) {
                if (move instanceof AbsoluteLifeMovement) {
                    setPosition(((LifeMovement) move).getPosition());
                }
                setStance(((LifeMovement) move).getNewstate());
            }
        }
    }

    public final int getSecondsLeft() {
        return secondsLeft;
    }

    public final void setSecondsLeft(int sl) {
        this.secondsLeft = sl;
    }
    
    public List<Integer> getExceptionList(){
        return exceptionList;
    }

    public void addItemException(int x){
        if (!exceptionList.contains(x)) {
            exceptionList.add(x);
        }
    }
    
    public static boolean hasLabelRing(Player p, byte pos) {
        short slot = 0;
        switch (pos) {
            case 0:
                slot = -121;
                break;
            case 1:
                slot = -131;
                break;
            case 2:
                slot = -139;
                break;
        }
        return p.getInventory(InventoryType.EQUIPPED).getItem(slot) != null;
    }
    
    public static boolean hasQuoteRing(Player p, byte pos) {
        short slot = 0;
        switch (pos) {
            case 0:
                slot = -129;
                break;
            case 1:
                slot = -132;
                break;
            case 2:
                slot = -140;
                break;
        }
        return p.getInventory(InventoryType.EQUIPPED).getItem(slot) != null;
    }
}