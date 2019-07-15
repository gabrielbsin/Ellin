/**
 * Ellin é um servidor privado de MapleStory
 * Baseado em um servidor GMS-Like na v.62
 */

package server.life;

/**
 * @brief ChangeableStats
 * @author GabrielSin <gabrielsin@playellin.net>
 * @date   22/07/2018
 */
import constants.GameConstants;

public class ChangeableStats extends OverrideMonsterStats {

    public int watk, matk, wdef, mdef, level;

    public ChangeableStats(MapleMonsterStats stats, OverrideMonsterStats ostats) {
        hp = ostats.getHp();
        exp = ostats.getExp();
        mp = ostats.getMp();
        watk = stats.getPADamage();
        matk = stats.getMADamage();
        wdef = stats.getPDDamage();
        mdef = stats.getMDDamage();
        level = stats.getLevel();
    }

    public ChangeableStats(MapleMonsterStats stats, int newLevel, boolean pqMob) { 
        final double mod = (double) newLevel / (double) stats.getLevel();
        final double hpRatio = (double) stats.getHp() / (double) stats.getExp();
        final double pqMod = (pqMob ? 1.5 : 1.0); 
        hp = Math.min((int) Math.round((!stats.isBoss() ? GameConstants.getMonsterHP(newLevel) : (stats.getHp() * mod)) * pqMod), Integer.MAX_VALUE); // right here lol
        exp = Math.min((int) Math.round((!stats.isBoss() ? (GameConstants.getMonsterHP(newLevel) / hpRatio) : (stats.getExp())) * pqMod), Integer.MAX_VALUE);
        mp = Math.min((int) Math.round(stats.getMp() * mod * pqMod), Integer.MAX_VALUE);
        watk = Math.min((int) Math.round(stats.getPADamage() * mod), Integer.MAX_VALUE);
        matk = Math.min((int) Math.round(stats.getMADamage() * mod), Integer.MAX_VALUE);
        wdef = Math.min(Math.min(stats.isBoss() ? 30 : 20, (int) Math.round(stats.getPDDamage() * mod)), Integer.MAX_VALUE);
        mdef = Math.min(Math.min(stats.isBoss() ? 30 : 20, (int) Math.round(stats.getMDDamage() * mod)), Integer.MAX_VALUE);
        level = newLevel;
    }

    public ChangeableStats(MapleMonsterStats stats, float statModifier, boolean pqMob) {
        this(stats, (int)(statModifier * stats.getLevel()), pqMob);
    }
}
