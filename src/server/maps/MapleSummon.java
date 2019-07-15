package server.maps;

import server.maps.object.FieldObjectType;
import server.maps.object.AbstractAnimatedFieldObject;
import java.awt.Point;

import client.player.Player;
import client.Client;
import client.player.skills.PlayerSkillFactory;
import constants.SkillConstants.Bishop;
import constants.SkillConstants.Corsair;
import constants.SkillConstants.FPArchMage;
import constants.SkillConstants.ILArchMage;
import constants.SkillConstants.Outlaw;
import constants.SkillConstants.Priest;
import constants.SkillConstants.Ranger;
import constants.SkillConstants.Sniper;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import packet.creators.PacketCreator;


public class MapleSummon extends AbstractAnimatedFieldObject {

    private final int ownerid, skillLevel, ownerLevel, skill;
    private int fh;
    private Field map; 
    private short hp;
    private SummonMovementType movementType;
    private final ReentrantReadWriteLock summonedLock = new ReentrantReadWriteLock();

    public MapleSummon(final Player owner, int skill, final Point pos, final SummonMovementType movementType) {
        super();
        this.ownerid = owner.getId();
        this.ownerLevel = owner.getLevel();
        this.skill = skill;
        this.map = owner.getMap();
        this.skillLevel = owner.getSkillLevel(PlayerSkillFactory.getSkill(skill));
        this.movementType = movementType;
        setPosition(pos);
        try {
            this.fh = owner.getMap().getFootholds().findBelow(pos).getId();
        } catch (NullPointerException e) {
            this.fh = 0;  
        }
    }

    @Override
    public void sendSpawnData(Client client) {
    }

    @Override
    public void sendDestroyData(Client client) {
        client.getSession().write(PacketCreator.RemoveSpecialMapObject(this, false));
    }
    
    public void lockSummon() {
        summonedLock.writeLock().lock();
    }
    
    public void unlockSummon() {
        summonedLock.writeLock().unlock();
    }

    public final Player getOwner() {
        return map.getCharacterById(ownerid);
    }

    public final int getOwnerId() {
	return ownerid;
    }
    
    public final void updateMap(final Field map) {
        this.map = map;
    }
    
    public final int getSkill() {
        return skill;
    }
    
    public final int getFh() {
        return fh;
    }

    public final void setFh(final int fh) {
        this.fh = fh;
    }

    public int getHP() {
        return this.hp;
    }
    
    public final int getOwnerLevel() {
        return ownerLevel;
    }

    public void addHP(int delta) {
        this.hp += delta;
    }
    
    public boolean hurt(int loss) {
        hp -= Math.min(loss, hp);
        return hp == 0;
    }

    public SummonMovementType getMovementType() {
        return movementType;
    }
    
    public boolean isStationary() {
        return (skill == Ranger.Puppet || skill == Sniper.Puppet || skill == Outlaw.Octopus);
    }

    public boolean isPuppet() {
        switch (skill) {
            case Ranger.Puppet:
            case Sniper.Puppet:
            case Outlaw.Octopus:
                return  true;
        }
        return false;
    }

    public boolean isSummon() {
        switch (skill) {
            case Priest.SummonDragon:
            case Bishop.Bahamut:
            case FPArchMage.Elquines:
            case ILArchMage.Ifrit:
            case Outlaw.Octopus:
            case Outlaw.Gaviota:
            case Corsair.WrathOfTheOctopi:
                return true;
        }
        return false;
    }

    public int getSkillLevel() {
        return skillLevel;
    }

    @Override
    public FieldObjectType getType() {
        return FieldObjectType.SUMMON;
    }
    
    public boolean canMultiSummon() {
        switch (skill) {
            case Outlaw.Octopus:
            case Outlaw.Gaviota:
            case Corsair.WrathOfTheOctopi:
                return true;
        }
        return false;
    }

    public boolean isOctopus() {
        switch (skill) {
            case Outlaw.Octopus:
            case Corsair.WrathOfTheOctopi:
                return true;
        }
        return false;
    }
}