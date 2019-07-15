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

package server.life.npc;

import client.Client;
import constants.NPCConstants;
import packet.creators.PacketCreator;
import server.life.AbstractLoadedMapleLife;
import server.shops.ShopFactory;
import server.maps.object.FieldObjectType;

public class MapleNPC extends AbstractLoadedMapleLife {
    
    private final MapleNPCStats stats;

    public MapleNPC(final int id, MapleNPCStats stats) {
        super(id);
        this.stats = stats;
    }

    public boolean hasShop() {
        return ShopFactory.getInstance().getShopForNPC(getId()) != null;
    }

    public void sendShop(Client c) {
        ShopFactory.getInstance().getShopForNPC(getId()).sendShop(c);
    }

    @Override
    public void sendSpawnData(Client client) {
        if (NPCConstants.DISABLE_MAPLETV) {
            for (int t = 0; t < NPCConstants.MAPLE_TV.length; t++ ){
                if (getId() == NPCConstants.MAPLE_TV[t]){
                    return;
                }
            }
        }
        client.getSession().write(PacketCreator.SpawnNPC(this, true));
        client.getSession().write(PacketCreator.SpawnNPCRequestController(this, true));
    }
	
    @Override
    public void sendDestroyData(Client client) {
    }

    @Override
    public FieldObjectType getType() {
        return FieldObjectType.NPC;
    }

    public String getName() {
        return stats.getName();
    }
    
    public MapleNPCStats getStats() {
        return stats;
    }
}