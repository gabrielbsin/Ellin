package client.player.skills;

import client.player.PlayerJob;
import constants.SkillConstants.Assassin;
import constants.SkillConstants.Bandit;
import constants.SkillConstants.Bishop;
import constants.SkillConstants.Cleric;
import constants.SkillConstants.DarkKnight;
import constants.SkillConstants.DragonKnight;
import constants.SkillConstants.FPArchMage;
import constants.SkillConstants.FPMage;
import constants.SkillConstants.Hermit;
import constants.SkillConstants.Hero;
import constants.SkillConstants.ILArchMage;
import constants.SkillConstants.Marauder;
import constants.SkillConstants.Paladin;
import constants.SkillConstants.Ranger;
import constants.SkillConstants.Shadower;
import constants.SkillConstants.Sniper;
import constants.SkillConstants.SuperGm;
import java.util.ArrayList;
import java.util.List;

import provider.MapleData;
import provider.MapleDataTool;
import server.MapleStatEffect;
import server.life.Element;

public class PlayerSkill {
    
    public int job;
    private final int id;
    private Element element;
    private int animationTime;
    public boolean action, chargeskill;
    private final List<MapleStatEffect> effects = new ArrayList<>();

    private PlayerSkill(int id) {
        this.id = id;
        this.job = id / 10000;
    }

    public int getId() {
        return id;
    }

    public static PlayerSkill loadFromData(int id, MapleData data) {
        PlayerSkill ret = new PlayerSkill(id);
        boolean isBuff = false;
        int skillType = MapleDataTool.getInt("skillType", data, -1);
        String elem = MapleDataTool.getString("elemAttr", data, null);
        if (elem != null) {
            ret.element = Element.getFromChar(elem.charAt(0));
        } else {
            ret.element = Element.NEUTRAL;
        }
        MapleData effect = data.getChildByPath("effect");
        if (skillType != -1) {
            if (skillType == 2) {
                isBuff = true;
            }
        } else {
            MapleData action = data.getChildByPath("action");
            MapleData hit = data.getChildByPath("hit");
            MapleData ball = data.getChildByPath("ball");
            isBuff = effect != null && hit == null && ball == null;
            isBuff |= action != null && MapleDataTool.getString("0", action, "").equals("alert2");
            switch (id) {
                case 1121006: // rush
                case 1221007: // rush
                case 1321003: // rush
                case 5201006: // Recoil Shot
                case DragonKnight.Sacrifice:
                case FPArchMage.BigBang:
                case ILArchMage.BigBang:
                case Bishop.BigBang:
                case FPMage.Explosion:
                case FPMage.PoisonMist: 
                case Cleric.Heal: 
                case Ranger.MortalBlow: 
                case Sniper.MortalBlow: 
                case Assassin.Drain:
                case Hermit.ShadowWeb: 
                case Bandit.Steal: 
                case Shadower.Smokescreen: 
                case SuperGm.HealnDispel: 
                case Hero.MonsterMagnet:
                case Paladin.MonsterMagnet: 
                case DarkKnight.MonsterMagnet: 
                case Marauder.EnergyCharge:
                    isBuff = false; 
                    break;
                case 1001: // recovery
                case 1002: // nimble feet 
                case 1004: // monster riding
                case 1005: // echo of hero
                case 1001003: // iron body
                case 1101004: // sword booster
                case 1201004: // sword booster
                case 1101005: // axe booster
                case 1201005: // bw booster
                case 1301004: // spear booster
                case 1301005: // polearm booster 
                case 3101002: // bow booster
                case 3201002: // crossbow booster
                case 4101003: // claw booster
                case 4201002: // dagger booster
                case 1101007: // power guard
                case 1201007: // power guard
                case 1101006: // rage
                case 1301006: // iron will
                case 1301007: // hyperbody
                case 1111002: // combo attack
                case 1211006: // blizzard charge bw
                case 1211004: // fire charge bw
                case 1211008: // lightning charge bw
                case 1221004: // divine charge bw
                case 1211003: // fire charge sword
                case 1211005: // ice charge sword
                case 1211007: // thunder charge sword
                case 1221003: // holy charge sword
                case 1311008: // dragon blood
                case 1121000: // maple warrior
                case 1221000: // maple warrior
                case 1321000: // maple warrior
                case 2121000: // maple warrior
                case 2221000: // maple warrior
                case 2321000: // maple warrior
                case 3121000: // maple warrior
                case 3221000: // maple warrior
                case 4121000: // maple warrior
                case 4221000: // maple warrior
                case 1121002: // power stance
                case 1221002: // power stance
                case 1321002: // power stance
                case 1121010: // enrage
                case 1321007: // beholder
                case 1320008: // beholder healing
                case 1320009: // beholder buff
                case 2001002: // magic guard
                case 2001003: // magic armor
                case 2101001: // meditation
                case 2201001: // meditation
                case 2301003: // invincible
                case 2301004: // bless
                case 2111005: // spell booster
                case 2211005: // spell booster
                case 2311003: // holy symbol
                case 2311006: // summon dragon
                case 2121004: // infinity
                case 2221004: // infinity
                case 2321004: // infinity
                case 2321005: // holy shield
                case 2121005: // elquines
                case 2221005: // ifrit
                case 2321003: // bahamut
                case 3121006: // phoenix
                case 3221005: // frostprey
                case 3111002: // puppet
                case 3211002: // puppet
                case 3111005: // silver hawk
                case 3211005: // golden eagle
                case 3001003: // focus
                case 3101004: // soul arrow bow
                case 3201004: // soul arrow crossbow
                case 3121002: // sharp eyes
                case 3221002: // sharp eyes
                case 3121008: // concentrate
                case 3221006: // blind
                case 4001003: // dark sight
                case 4101004: // haste
                case 4201003: // haste
                case 4111001: // meso up
                case 4111002: // shadow partner
                case 4121006: // shadow stars
                case 4211003: // pick pocket
                case 4211005: // meso guard
                case 5111005: // Transformation (Buccaneer)
                case 5121003: // Super Transformation (Viper)
                case 5220002: // wrath of the octopi
                case 5211001: // Pirate octopus summon
                case 5211002: // Pirate bird summon
                case 5221006: // BattleShip
                case 9001000: // haste
                case 9101001: // super haste
                case 9101002: // holy symbol
                case 9101003: // bless
                case 9101004: // hide
                case 9101008: // hyper body
                case 1121011: // hero's will
                case 1221012: // hero's will
                case 1321010: // hero's will
                case 2321009: // hero's will
                case 2221008: // hero's will
                case 2121008: // hero's will
                case 3121009: // hero's will
                case 3221008: // hero's will
                case 4121009: // hero's will
                case 4221008: // hero's will
                case 2101003: // slow
                case 2201003: // slow
                case 2111004: // seal
                case 2211004: // seal
                case 1111007: // armor crash
                case 1211009: // magic crash
                case 1311007: // power crash
                case 2311005: // doom 
                case 2121002: // mana reflection
                case 2221002: // mana reflection
                case 2321002: // mana reflection
                case 2311001: // dispel
                case 1201006: // threaten 
                case 4121004: // ninja ambush
                case 4221004: // ninja ambush
                case 5121009: // [PIRATE] Speed Infusion
                case 5221010: // [PIRATE] Speed Infusion
                    isBuff = true;
                break;
            }
        }
        
        ret.chargeskill = data.getChildByPath("keydown") != null;
        
        for (MapleData level : data.getChildByPath("level")) {
            MapleStatEffect statEffect = MapleStatEffect.loadSkillEffectFromData(level, id, isBuff,  Byte.parseByte(level.getName()));
            ret.effects.add(statEffect);
        }
        ret.animationTime = 0;
        if (effect != null) {
            for (MapleData effectEntry : effect) {
                ret.animationTime += MapleDataTool.getIntConvert("delay", effectEntry, 0);
            }
        }
     return ret;
    }

    public MapleStatEffect getEffect(final int level) {
        if (effects.size() < level) {
            if (effects.size() > 0) { 
                return effects.get(effects.size() - 1);
            }
            return null;
        } else if (level <= 0) {
            return effects.get(0);
        }
        return effects.get(level - 1);
    }

    public int getMaxLevel() {
        return effects.size();
    }

    public boolean canBeLearnedBy(PlayerJob job) {
	int jid = job.getId();
        int skillForJob = id / 10000;
        if (jid / 100 != skillForJob / 100 && skillForJob / 100 != 0) {
            return false;
        }
        if ((skillForJob / 10) % 10 > (jid / 10) % 10) {
            return false;
        }
        if (skillForJob % 10 > jid % 10) {
            return false;
        }
        return true;
    }

    public boolean isFourthJob() {
        return ((id / 10000) % 10) == 2;
    }

    public Element getElement() {
        return element;
    }

    public int getAnimationTime() {
        return animationTime;
    }

    public boolean isBeginnerSkill() {
        int jobId = id / 10000;
        return jobId == 0 || jobId == 1000 || jobId == 2000 || jobId == 2001 || jobId == 3000;
    }

    public boolean isGMSkill() {
        return id > 9000000;
    }

    public boolean getAction() {
        return action;
    }
    
    public boolean isChargeSkill() {
        return chargeskill;
    }

    public boolean isActiveSkill() {
    	return (id % 10000) / 1000 == 1;
    }
}
