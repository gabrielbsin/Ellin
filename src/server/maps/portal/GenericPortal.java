/*
	This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc> 
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

package server.maps.portal;

import java.awt.Point;

import client.player.Player;
import client.Client;
import client.player.violation.CheatingOffense;
import constants.MapConstants;
import packet.creators.PacketCreator;
import scripting.portal.PortalScriptManager;
import server.maps.Field;

public class GenericPortal implements Portal {
    private String name;
    private String target;
    private Point position;
    private int targetmap;
    private int id;
    private final int type;
    private String scriptName;
    private boolean portalState;

    public GenericPortal(int type) {
        this.type = type;
        portalState = OPEN;
    }

    @Override
    public void setPortalState(boolean state) {
        this.portalState = state;
    }

    @Override
    public boolean getPortalState() {
        return portalState;
    }

    @Override
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id  = id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Point getPosition() {
        return position;
    }

    @Override
    public String getTarget() {
        return target;
    }

    @Override
    public int getTargetMapId() {
        return targetmap;
    }

    @Override
    public int getType() {
        return type;
    }

    @Override
    public String getScriptName() {
        return scriptName;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPosition(Point position) {
        this.position = position;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public void setTargetMapId(int targetmapid) {
        this.targetmap = targetmapid;
    }

    @Override
    public void setScriptName(String scriptName) {
        this.scriptName = scriptName;
    }

    @Override
    public void enterPortal(Client c) {
        Player player = c.getPlayer();
        if (player.getMap().getPortalDisable() && !c.getPlayer().isGameMaster()) {
            c.getSession().write(PacketCreator.ServerNotice(5, "Portals are disabled."));
            c.getSession().write(PacketCreator.EnableActions());
            return;
        }
        if (System.currentTimeMillis() - player.getLastPortalEntry() < 2500) {
            c.getSession().write(PacketCreator.EnableActions());
            return;
        }
        player.setLastPortalEntry(System.currentTimeMillis());
        double distanceSq = getPosition().distanceSq(player.getPosition());
        if (distanceSq > 22500) {
            player.getCheatTracker().registerOffense(CheatingOffense.USING_FARAWAY_PORTAL, "Tried to use faraway portal - " + Math.sqrt(distanceSq));
        }
        boolean changed = false;
        if (getScriptName() != null) {
            try {
                changed = PortalScriptManager.getInstance().executePortalScript(this, c);
            } catch(NullPointerException npe) {
                npe.printStackTrace();
            }
        } else if (getTargetMapId() != MapConstants.NULL_MAP) {
            Field to = c.getPlayer().getEventInstance() == null ? c.getChannelServer().getMapFactory().getMap(getTargetMapId()) : c.getPlayer().getEventInstance().getMapInstance(getTargetMapId());
            Portal pto = to.getPortal(getTarget());
            if (pto == null) {
                pto = to.getPortal(0);
            }
            c.getPlayer().changeMap(to, pto); 
            changed = true;
        }
        if (!changed) {
            c.getSession().write(PacketCreator.EnableActions());
        } else {
            c.getPlayer().setLastPortalEntry(System.currentTimeMillis());
        }
    }
}
