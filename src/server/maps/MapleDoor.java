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
package server.maps;

import client.player.Player;
import constants.MapConstants;
import java.awt.Point;
import tools.Pair;


import server.maps.object.FieldDoorObject;
import server.maps.portal.Portal;

/**
 *
 * @author Matze
 * @author Ronan
 */
public class MapleDoor {
    private int ownerId;
    private Field town;
    private Portal townPortal;
    private final Field target;
    private Pair<String, Integer> posStatus = null;
    
    private FieldDoorObject townDoor;
    private FieldDoorObject areaDoor;

    public MapleDoor(Player owner, Point targetPosition) {
        this.ownerId = owner.getId();
        this.target = owner.getMap();
        
        if(target.canDeployDoor(targetPosition)) {
            if (MapConstants.USE_ENFORCE_MDOOR_POSITION) {
                posStatus = target.getDoorPositionStatus(targetPosition);
            }
            
            if(posStatus == null) {
                this.town = this.target.getReturnField();
                this.townPortal = getDoorPortal(owner.getDoorSlot());

                if(townPortal != null) {
                    this.areaDoor = new FieldDoorObject(ownerId, town, target, false, targetPosition, townPortal.getPosition());
                    this.townDoor = new FieldDoorObject(ownerId, target, town, true, townPortal.getPosition(), targetPosition);

                    this.areaDoor.setPairOid(this.townDoor.getObjectId());
                    this.townDoor.setPairOid(this.areaDoor.getObjectId());
                } else {
                    this.ownerId = -1;
                }
            } else {
                this.ownerId = -3;
            }
        } else {
            this.ownerId = -2;
        }
    }
    
    private Portal getDoorPortal(int slot) {
        try {
            return town.getAvailableDoorPortals().get(slot);
        } catch (IndexOutOfBoundsException e) {
            try {
                return town.getAvailableDoorPortals().get(0);
            } catch (IndexOutOfBoundsException ex) {
                return null;
            }
        }
    }
    
    public int getOwnerId() {
        return ownerId;
    }

    public FieldDoorObject getTownDoor() {
        return townDoor;
    }
    
    public FieldDoorObject getAreaDoor() {
        return areaDoor;
    }
    
    public Field getTown() {
        return town;
    }

    public Portal getTownPortal() {
        return townPortal;
    }

    public Field getTarget() {
        return target;
    }

    public Pair<String, Integer> getDoorStatus() {
        return posStatus;
    }
}
