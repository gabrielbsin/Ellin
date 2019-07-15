package client.player.inventory;

import client.player.Player;
import client.player.buffs.BuffStat;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import packet.creators.PacketCreator;
import tools.Randomizer;

public class TamingMob implements Serializable {

    private int itemid;
    private int skillid;
    private int fatigue;
    private int exp;
    private int level;
    private long lastFatigue = 0;
    private final transient WeakReference<Player> owner;

    public TamingMob(Player owner, int id, int skillid) {
        this.itemid = id;
        this.skillid = skillid;
        this.fatigue = 0;
        this.level = 1;
        this.exp = 0;
        this.owner = new WeakReference<>(owner);
    }
    
    public int getItemId() {
        return itemid;
    }

    public int getSkillId() {
        return skillid;
    }

    public int getId() {
        switch (this.itemid) {
            case 1902000:
                return 1;
            case 1902001:
                return 2;
            case 1902002:
                return 3;
            case 1932000:
                return 4;
            case 1902008:
            case 1902009:
                return 5;
            default:
                return 0;
        }
    }

    public int getTiredness() {
        return fatigue;
    }

    public int getExp() {
        return exp;
    }

    public int getLevel() {
        return level;
    }

    public void setTiredness(int newtiredness) {
        this.fatigue += newtiredness;
        if (fatigue < 0) {
            fatigue = 0;
        }
    }

    public void increaseTiredness() {
        this.fatigue++;
        if (fatigue > 100 && owner.get() != null) {
            owner.get().cancelEffectFromBuffStat(BuffStat.MONSTER_RIDING);
        }
        update();
    }

    public void setExp(int newexp) {
        this.exp = newexp;
    }

    public void setLevel(int newlevel) {
        this.level = newlevel;
    }

    public void setItemId(int newitemid) {
        this.itemid = newitemid;
    }

    public void setSkillId(int skillid) {
        this.skillid = skillid;
    }
    
    public final boolean canTire(long now) {
        return lastFatigue > 0 && (lastFatigue + 30000 < now); 
    }

    public void startSchedule() {
        this.lastFatigue = System.currentTimeMillis();
    }

    public void cancelSchedule() {
        this.lastFatigue = 0;
    }
    
    public void increaseExp() {
        int e;
        if (level >= 1 && level <= 7) {
            e = Randomizer.nextInt(10) + 15;
        } else if (level >= 8 && level <= 15) {
            e = Randomizer.nextInt(13) + 15 / 2;
        } else if (level >= 16 && level <= 24) {
            e = Randomizer.nextInt(23) + 18 / 2;
        } else {
            e = Randomizer.nextInt(28) + 25 / 2;
        }
        setExp(exp + e);
    }
    
    public void update() {
        final Player p = owner.get();
        if (p != null) {
            p.getMap().broadcastMessage(PacketCreator.UpdateMount(p, false));
        }
    }
}