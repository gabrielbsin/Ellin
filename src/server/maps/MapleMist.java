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

import server.maps.object.FieldObjectType;
import server.maps.object.AbstractMapleFieldObject;
import client.player.Player;
import client.Client;
import client.player.skills.PlayerSkill;
import client.player.skills.PlayerSkillFactory;
import constants.SkillConstants.FPMage;
import constants.SkillConstants.Shadower;
import java.awt.Point;
import java.awt.Rectangle;
import packet.creators.PacketCreator;
import packet.transfer.write.OutPacket;
import server.MapleStatEffect;
import server.life.MapleMonster;
import server.life.MobSkill;


public class MapleMist extends AbstractMapleFieldObject {
    private Rectangle mistPosition;
    private Player owner = null;
    private MapleMonster mob = null;
    private MapleStatEffect source;
    private MobSkill skill;
    private boolean isMobMist, isPoisonMist, isRecoveryMist;
    private int skillDelay;

    public MapleMist(Rectangle mistPosition, MapleMonster mob, MobSkill skill) {
        this.mistPosition = mistPosition;
        this.mob = mob;
        this.skill = skill;
        isMobMist = true;
        isPoisonMist = true;
        isRecoveryMist = false;
        skillDelay = 0;
    }

    public MapleMist(Rectangle mistPosition, Player owner, MapleStatEffect source) {
        this.mistPosition = mistPosition;
        this.owner = owner;
        this.source = source;
        this.skillDelay = 8;
        this.isMobMist = false;
        this.isRecoveryMist = false;
        this.isPoisonMist = false;
        switch (source.getSourceId()) {
            case Shadower.Smokescreen:  
                isPoisonMist = false;
                break;
            case FPMage.PoisonMist:  
                isPoisonMist = true;
                break;
        }
    }

    @Override
    public FieldObjectType getType() {
        return FieldObjectType.MIST;
    }

    @Override
    public Point getPosition() {
        return mistPosition.getLocation();
    }

    public PlayerSkill getSourceSkill() {
        return PlayerSkillFactory.getSkill(source.getSourceId());
    }

    public boolean isMobMist() {
        return isMobMist;
    }

    public boolean isPoisonMist() {
        return isPoisonMist;
    }

    public boolean isRecoveryMist() {
    	return isRecoveryMist;
    }
    
    public int getSkillDelay() {
        return skillDelay;
    }

    public MapleMonster getMobOwner() {
        return mob;
    }

    public Player getOwner() {
        return owner;
    }

    public Rectangle getBox() {
        return mistPosition;
    }

    @Override
    public void setPosition(Point position) {
        throw new UnsupportedOperationException();
    }

    public final OutPacket makeDestroyData() {
        return PacketCreator.RemoveMist(getObjectId());
    }

    public final OutPacket makeSpawnData() {
        if (owner != null) {
            return PacketCreator.SpawnMist(getObjectId(), owner.getId(), getSourceSkill().getId(), owner.getSkillLevel(PlayerSkillFactory.getSkill(source.getSourceId())), this);
        }
        return PacketCreator.SpawnMist(getObjectId(), mob.getId(), skill.getSkillId(), skill.getSkillLevel(), this);
    }

    public final OutPacket makeFakeSpawnData(int level) {
        if (owner != null) {
            return PacketCreator.SpawnMist(getObjectId(), owner.getId(), getSourceSkill().getId(), level, this);
        }
        return PacketCreator.SpawnMist(getObjectId(), mob.getId(), skill.getSkillId(), skill.getSkillLevel(), this);
    }

    @Override
    public void sendSpawnData(Client client) {
        client.announce(makeSpawnData());
    }

    @Override
    public void sendDestroyData(Client client) {
        client.announce(makeDestroyData());
    }

    public boolean makeChanceResult() {
        return source.makeChanceResult();
    }
}
