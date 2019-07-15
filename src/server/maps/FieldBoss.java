/**
 * Ellin é um servidor privado de MapleStory
 * Baseado em um servidor GMS-Like na v.62
 */

package server.maps;

import java.awt.Point;
import server.life.MapleLifeFactory;

/**
 * @brief FieldBoss
 * @author GabrielSin <gabrielsin@playellin.net>
 * @date   31/05/2018
 */
public class FieldBoss {
    
    public enum FieldBosses {
        
        MANO(2220000, 2700, new Point(439, 185), new Point(301, -85), new Point(107, -355), "A cool breeze was felt when Mano appeared."),
        STUMPY(3220000, 2700, new Point(867, 1282), new Point(810, 1570), new Point(838, 2197), "Stumpy apareceu com um som que toca a Stone Mountain."),
        KING_CLANG(5220001, 1200, new Point(-355, 179), new Point(-1283, -113), new Point(-571, -593), "A strange turban shell has appeared on the beach."), 
        TAE_ROON(7220000, 2100, new Point(-210, 33), new Point(-234, 393), new Point(-654, 33), "Tae Roon appeared with a loud growl."),
        ELIZA(8220000, 1200, new Point(665, 83), new Point(672, -217), new Point(-123, -217), "Eliza has appeared with a black whirlwind."),
        GHOST_PRIEST(7220002, 1800, new Point(-303, 543), new Point(227, 543), new Point(719, 543), "The area fills with an unpleasant force of evil.. even the occasional ones of the cats sound disturbing"),
        OLD_FOX(7220001, 2700, new Point(-169, -147), new Point(-517, 93), new Point(247, 93), "As the moon light dims,a long fox cry can be heard and the presence of the old fox can be felt."),
        DALE(6220000, 1800, new Point(710, 118), new Point(95, 119), new Point(-535, 120), "The huge crocodile Dale has come out from the swamp."),
        FAUST(5220002, 1800, new Point(1000, 278), new Point(557, 278), new Point(95, 278), "The blue fog became darker when Faust appeared."),
        TIMER(5220003, 1500, new Point(-467, 1032), new Point(532, 1032), new Point(-47, 1032), "Click clock! Timer has appeared with an irregular clock sound."),
        JENO(6220001, 2400,new Point(-4134, 416), new Point(-4283, 776), new Point(-3292, 776), "Jeno has appeared with a heavy sound of machinery."),
        LEV(8220003, 7200, new Point(-15, 2481), new Point(127, 1634), new Point(159, 1142), "Leviathan has appeared with a cold wind from over the gorge."),
        DEWU(3220001, 3600, new Point(-215, 275), new Point(298, 275), new Point(592, 275), "Dewu slowly appeared out of the sand dust."),
        CHIMERA(8220002, 2700, new Point(-1094, -405), new Point(-772, -116), new Point(-108, 181), "Chimera has appeared out of the darkness of the underground with a glitter in her eyes."),
        SHERP(4220000, 2700, new Point(-291, -20), new Point(-272, -500), new Point(-462, 640), "A strange shell has appeared from a grove of seaweed.");
        
        int mobid = -1, timeMob = -1;
        Point positionOne = null, positionTwo = null, positionThree = null;
        String msg = null;
        
        private FieldBosses (int mobid, int timeMob, Point positionOne, Point positionTwo, Point positionThree, String msg) {
            this.mobid = mobid;
            this.timeMob = timeMob;
            this.positionOne = positionOne;
            this.positionTwo = positionTwo;
            this.positionThree = positionThree;
            this.msg = msg;
        }
        
        public int getBoss() {
            return this.mobid;
        }
        
        public int getTimeMob() {
            return this.timeMob;
        }
        
        public Point getPositionOne() {
            return this.positionOne;
        }
        
        public Point getPositionTwo() {
            return this.positionTwo;
        }
        
        public Point getPositionThree() {
            return this.positionThree;
        }
        
        public String getMessage() {
            return this.msg;
        } 
        
        public static FieldBosses getBossMap(Field map) {
            switch (map.getId()) {
                case 104000400:
                    return MANO;
                case 101030404:
                    return STUMPY;
                case 110040000:
                    return KING_CLANG;
                case 250010304:
                    return TAE_ROON;
                case 200010300:
                    return ELIZA;
                case 250010503:
                    return GHOST_PRIEST;
                case 222010310:
                    return OLD_FOX;
                case 107000300:
                    return DALE;
                case 100040105:
                    return FAUST;
                case 220050100:
                    return TIMER;
                case 221040301:
                    return JENO;
                case 240040401:
                    return LEV;
                case 260010201:
                    return DEWU;
                case 261030000:
                    return CHIMERA;
                case 230020100:
                    return SHERP;
                default:
                    return null;
            }
        }
        
    }
    
    public static void RegisterBossRespawn(final Field map) {
        FieldBosses fb = FieldBosses.getBossMap(map);
        if (fb != null) {
            map.addAreaMonsterSpawn(MapleLifeFactory.getMonster(fb.getBoss()), fb.getPositionOne(), fb.getPositionTwo(), fb.getPositionThree(), fb.getTimeMob(), fb.getMessage());
            System.out.println("[Registrando Chefe] Chefe {" + String.valueOf(fb) + "} registrado.");
        }
    }
}