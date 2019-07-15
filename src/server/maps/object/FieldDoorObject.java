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
package server.maps.object;

import java.awt.Point;

import client.Client;
import client.player.Player;
import constants.MapConstants;
import packet.creators.PacketCreator;
import packet.creators.PartyPackets;
import server.maps.Field;

/**
 *
 * @author Ronan
 */
public class FieldDoorObject extends AbstractMapleFieldObject {
    private final int ownerId;
    private int pairOid;
    
    private final boolean isTown;
    private final Field from;
    private final Field to;
    private final Point toPos;
    
    public FieldDoorObject(int owner, Field destination, Field origin, boolean town, Point targetPosition, Point toPosition) {
        super();
        setPosition(targetPosition);
        
        ownerId = owner;
        isTown = town;
        from = origin;
        to = destination;
        toPos = toPosition;
    }
    
    public void warp(final Player chr, boolean toTown) {
        if (chr.getId() == ownerId || (chr.getParty() != null && chr.getParty().getMemberById(ownerId) != null)) {
            if (chr.getParty() == null && (to.isLastDoorOwner(chr.getId()) || toTown)) {
                chr.changeMap(to, toPos);
            } else {
                chr.changeMap(to, to.findClosestPlayerSpawnpoint(toPos));
            }   
        } else {
            chr.getClient().announce(PacketCreator.PortalBlocked(6));
            chr.getClient().announce(PacketCreator.EnableActions());
        }
    }

    @Override
    public void sendSpawnData(Client client) {
        if (from.getId() == client.getPlayer().getMapId()) {
            if (client.getPlayer().getParty() != null && (ownerId == client.getPlayer().getId() || client.getPlayer().getParty().getMemberById(ownerId) != null)) {
                client.announce(PartyPackets.PartyPortal(this.getFrom().getId(), this.getTo().getId(), this.toPosition()));
            }
            
            client.announce(PacketCreator.SpawnPortal(this.getFrom().getId(), this.getTo().getId(), this.toPosition()));
            if(!this.inTown()) {
                client.announce(PacketCreator.SpawnDoor(this.getOwnerId(), this.getPosition(), true));
            }
        }
    }

    @Override
    public void sendDestroyData(Client client) {
        if (from.getId() == client.getPlayer().getMapId()) {
            if (client.getPlayer().getParty() != null && (ownerId == client.getPlayer().getId() || client.getPlayer().getParty().getMemberById(ownerId) != null)) {
                client.announce(PartyPackets.PartyPortal(MapConstants.NULL_MAP, MapConstants.NULL_MAP, new Point(-1, -1)));
            }
            client.announce(PacketCreator.RemoveDoor(ownerId, isTown));
        }
    }
    
    public int getOwnerId() {
        return ownerId;
    }
    
    public void setPairOid(int oid) {
        this.pairOid = oid;
    }
    
    public int getPairOid() {
        return pairOid;
    }
    
    public boolean inTown() {
        return isTown;
    }
    
    public Field getFrom() {
        return from;
    }
    
    public Field getTo() {
        return to;
    }
    
    public Field getTown() {
        return isTown ? from : to;
    }
    
    public Field getArea() {
        return !isTown ? from : to;
    }
    
    public Point getAreaPosition() {
        return !isTown ? getPosition() : toPos;
    }
    
    public Point toPosition() {
        return toPos;
    }
    
    @Override
    public FieldObjectType getType() {
        return FieldObjectType.DOOR;
    }
}