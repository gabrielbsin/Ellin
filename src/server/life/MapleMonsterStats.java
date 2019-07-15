package server.life;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import server.life.components.BanishInfo;
import server.life.components.LoseItem;
import server.life.components.SelfDestruction;
import tools.Pair;

public class MapleMonsterStats {
    
    private int PADamage;
    private int PDDamage;
    private int MADamage;
    private int MDDamage; 
    private int cp;
    private int exp;
    private int dropPeriod;
    private int hp;
    private int mp;
    private int level;
    private int accuracy;
    private int removeAfter;
    private int buffToGive;
    private byte tagColor;
    private byte tagBgColor;
    private boolean boss;
    private boolean undead;
    private boolean publicReward;
    private boolean explosive;
    private boolean friendly;
    private boolean firstAttack;
    private boolean changeable;
    private String name;
    private BanishInfo banish;
    private List<LoseItem> loseItem = null;
    private SelfDestruction selfDestruction = null;
    private final Map<String, Integer> animationTimes = new HashMap<>();
    private final Map<Element, ElementalEffectiveness> resistance = new HashMap<>();
    private final List<Pair<Integer, Integer>> skills = new ArrayList<>();
    private List<Integer> revives = Collections.emptyList();

    public int getExp() {
        return exp;
    }

    public void setExp(int exp) {
        this.exp = exp;
    }

    public int getHp() {
        return hp;
    }

    public void setHp(int hp) {
        this.hp = hp;
    }

    public int getMp() {
        return mp;
    }

    public void setMp(int mp) {
        this.mp = mp;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getRemoveAfter() {
        return removeAfter;
    }

    public void setRemoveAfter(int removeAfter) {
        this.removeAfter = removeAfter;
    }

    public void setBoss(boolean boss) {
        this.boss = boss;
    }

    public boolean isBoss() {
        return boss;
    }

    public void setPublicReward(boolean ffaLoot) {
        this.publicReward = ffaLoot;
    }

    public boolean isPublicReward() {
        return publicReward;
    }

    public void setAnimationTime(String name, int delay) {
        animationTimes.put(name, delay);
    }

    public int getAnimationTime(String name) {
        Integer ret = animationTimes.get(name);
        if (ret == null) {
            return 500;
        }
        return ret.intValue();
    }

    public boolean isMobile() {
        return animationTimes.containsKey("move") || animationTimes.containsKey("fly");
    }

    public List<Integer> getRevives() {
        return revives;
    }

    public void setRevives(List<Integer> revives) {
        this.revives = revives;
    }

    public void setUndead(boolean undead) {
        this.undead = undead;
    }

    public boolean getUndead() {
        return undead;
    }

    public void setEffectiveness (Element e, ElementalEffectiveness ee) {
        resistance.put(e, ee);
    }

    public ElementalEffectiveness getEffectiveness (Element e) {
        ElementalEffectiveness elementalEffectiveness = resistance.get(e);
        if (elementalEffectiveness == null) {
            return ElementalEffectiveness.NORMAL;
        } else {
            return elementalEffectiveness;
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public byte getTagColor() {
        return tagColor;
    }

    public void setTagColor(int tagColor) {
        this.tagColor = (byte) tagColor;
    }

    public byte getTagBgColor() {
        return tagBgColor;
    }

    public void setTagBgColor(int tagBgColor) {
        this.tagBgColor = (byte) tagBgColor;
    }

    public void setSkills(List<Pair<Integer, Integer>> skills) {
        skills.forEach((skill) -> {
            this.skills.add(skill);
        });
    }

    public List<Pair<Integer, Integer>> getSkills() {
        return Collections.unmodifiableList(this.skills);
    }

    public int getNoSkills() {
        return this.skills.size();
    }

    public boolean hasSkill(int skillId, int level) {
        return skills.stream().anyMatch((skill) -> (skill.getLeft() == skillId && skill.getRight() == level));
    }

    public void setFirstAttack(boolean firstAttack) {
        this.firstAttack = firstAttack;
    }

    public boolean isFirstAttack() {
        return firstAttack;
    }

    public void setBuffToGive(int buff) {
        this.buffToGive = buff;
    }

    public int getBuffToGive() {
        return buffToGive;
    }

    public int getCp() {
        return cp;
    }

    public void setCp(int cp) {
        this.cp = cp;
    }

    public void setPADamage(int dmg) {
        this.PADamage = dmg;
    }

    public int getPADamage() {
        return PADamage;
    }

    public int getDropPeriod() {
        return dropPeriod;
    }

    public void setDropPeriod(int dropPeriod) {
        this.dropPeriod = dropPeriod;
    }

    public int getPDDamage() {
        return PDDamage;
    }

    public int getMADamage() {
        return MADamage;
    }

    public int getMDDamage() {
        return MDDamage;
    }

    public boolean isFriendly() {
        return friendly;
    }

    public void setFriendly(boolean value) {
        this.friendly = value;
    }

    public void setPDDamage(int PDDamage) {
        this.PDDamage = PDDamage;
    }

    public void setMADamage(int MADamage) {
        this.MADamage = MADamage;
    }

    public void setMDDamage(int MDDamage) {
        this.MDDamage = MDDamage;
    } 

    public void setExplosive(boolean explosive) {
        this.explosive = explosive;
    }

    public boolean isExplosive() {
        return explosive;
    }

    public List<LoseItem> loseItem() {
        return loseItem;
    }

    public void addLoseItem(LoseItem li) {
        if (loseItem == null) {
            loseItem = new LinkedList<>();
        }
        loseItem.add(li);
    }

    public SelfDestruction selfDestruction() {
        return selfDestruction;
    }

    public void setSelfDestruction(SelfDestruction sd) {
        this.selfDestruction = sd;
    }
    
    public BanishInfo getBanishInfo() {
        return banish;
    }

    public void setBanishInfo(BanishInfo banish) {
        this.banish = banish;
    }
    
    public int getAccuracy() {
        return this.accuracy;
    }

    public void setAccuracy(final int acc) {
        this.accuracy = acc;
    }

    public int dropsMeso() {
        if (getRemoveAfter() != 0 || getCp() > 0 || selfDestruction() != null) {
            return 0;
        } else if (isExplosive()) {
            return 7;
        } else if (isBoss()) {
            return 2;
        }
        return 1;
    }

    public void setChange(boolean change) {
        this.changeable = change;
    }

    public boolean isChangeable() {
        return changeable;
    }
}
