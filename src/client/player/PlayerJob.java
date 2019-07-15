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

package client.player;

public enum PlayerJob {
    BEGINNER(0),

    WARRIOR(100),
    FIGHTER(110), CRUSADER(111), HERO(112),
    PAGE(120), WHITEKNIGHT(121), PALADIN(122),
    SPEARMAN(130), DRAGONKNIGHT(131), DARKKNIGHT(132),

    MAGICIAN(200),
    FP_WIZARD(210), FP_MAGE(211), FP_ARCHMAGE(212),
    IL_WIZARD(220), IL_MAGE(221), IL_ARCHMAGE(222),
    CLERIC(230), PRIEST(231), BISHOP(232),

    BOWMAN(300),
    HUNTER(310), RANGER(311), BOWMASTER(312),
    CROSSBOWMAN(320), SNIPER(321), MARKSMAN(322),

    THIEF(400),
    ASSASSIN(410), HERMIT(411), NIGHTLORD(412),
    BANDIT(420), CHIEFBANDIT(421), SHADOWER(422),

    PIRATE(500),
    BRAWLER(510), MARAUDER(511), BUCCANEER(512),
    GUNSLINGER(520), OUTLAW(521), CORSAIR(522),

    MAPLELEAF_BRIGADIER(800),
    GM(900), SUPERGM(910);

    final int jobId;

    private PlayerJob(int id) {
        jobId = id;
    }

    public int getId() {
        return jobId;
    }
    
    public static final int  
        CLASS_BEGINNER = 0,
        CLASS_WARRIOR = 1,
        CLASS_MAGICIAN = 2,
        CLASS_BOWMAN = 3,
        CLASS_THIEF = 4,
        CLASS_PIRATE = 5,
        CLASS_GAMEMASTER = 9
    ;

    public static PlayerJob getById(int id) {
        for (PlayerJob l : PlayerJob.values()) {
            if (l.getId() == id) {
                return l;
            }
        }
        return null;
    }
    
    public static boolean checkJobMask(int mask, PlayerJob toCheck) {
        int maskToCheck = getBy5ByteEncoding(toCheck);
        return (mask & maskToCheck) == maskToCheck;
    }
	
    public static PlayerJob getEncoding(int encoded) {
        switch (encoded) {
            case 2:
                return WARRIOR;
            case 4:
                return MAGICIAN;
            case 8:
                return BOWMAN;
            case 16:
                return THIEF;
            case 32: 
                return PIRATE;
            default:
                return BEGINNER;
        }
    }
    
    public static int getAdvancement(int jobId) {
        if (jobId == 0) {
            return 0; 
        }
        if (jobId % 100 == 0)
            return 1; 
        switch (jobId % 10) {
            case 0:
                return 2; 
            case 1:
                return 3; 
            case 2:
                return 4; 
        }
        return -1;
    }
	
    public boolean isA(PlayerJob basejob) {        
        return getId() >= basejob.getId() && getId() / 100 == basejob.getId() / 100;
    }
    
    public static int getJobPath(int jobid) {
        return (jobid / 100);
    }
    
    public boolean isA(short jobid) {
        return getId() >= jobid && getId() / 100 == jobid / 100;
    }
    
    public int getBaseJob() {
        return getId() - (getId() % 100);
    }
    
    public int getJobNiche() {
        return (jobId / 100) % 10;
    }
    
    public static boolean isBeginner(int jobid) {
        return (getJobPath(jobid) == CLASS_BEGINNER);
    }
    
    public static boolean isWarrior(int jobid) {
        return (getJobPath(jobid) == CLASS_WARRIOR);
    }

    public static boolean isMage(int jobid) {
        return (getJobPath(jobid) == CLASS_MAGICIAN);
    }

    public static boolean isArcher(int jobid) {
        return (getJobPath(jobid) == CLASS_BOWMAN);
    }

    public static boolean isThief(int jobid) {
        return (getJobPath(jobid) == CLASS_THIEF);
    }

    public static boolean isPirate(int jobid) {
        return (getJobPath(jobid) == CLASS_PIRATE);
    }

    public static boolean isGameMaster(int jobid) {
        return (getJobPath(jobid) == CLASS_GAMEMASTER);
    }
    
    public static PlayerJob getBy5ByteEncoding(int encoded) {
        switch (encoded) {
            case 2:
                return WARRIOR;
            case 4:
                return MAGICIAN;
            case 8:
                return BOWMAN;
            case 16:
                return THIEF;
            case 32: 
                return PIRATE;
            default:
                return BEGINNER;
        }
    }
    
    public static int getBy5ByteEncoding(PlayerJob job) {
        switch (job) {
            case WARRIOR:
                case FIGHTER:
                case CRUSADER:
                case HERO:
                case PAGE:
                case WHITEKNIGHT:
                case PALADIN:
                case SPEARMAN:
                case DRAGONKNIGHT:
                case DARKKNIGHT:
                    return 2;
            case MAGICIAN:
                case FP_WIZARD:
                case FP_MAGE:
                case FP_ARCHMAGE:
                case IL_WIZARD:
                case IL_MAGE:
                case IL_ARCHMAGE:
                case CLERIC:
                case PRIEST:
                case BISHOP:
                    return 4;
            case BOWMAN:
                case HUNTER:
                case RANGER:
                case BOWMASTER:
                case CROSSBOWMAN:
                case SNIPER:
                case MARKSMAN:
                    return 8;
            case THIEF:
                case ASSASSIN:
                case HERMIT:
                case NIGHTLORD:
                case BANDIT:
                case CHIEFBANDIT:
                case SHADOWER:
                    return 16;
            case PIRATE:
                case BRAWLER:
                case MARAUDER:
                case BUCCANEER:
                case GUNSLINGER:
                case OUTLAW:
                case CORSAIR:
                return 32;
            default:
                return 1;
        }
    }
}
