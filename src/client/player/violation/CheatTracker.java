package client.player.violation;

import client.player.Player;
import client.player.PlayerStringUtil;
import constants.SkillConstants.Bowmaster;
import handling.world.service.BroadcastService;
import java.awt.Point;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import packet.creators.PacketCreator;
import tools.FileLogger;
import tools.StringUtil;
import tools.TimerTools.CheatTrackerTimer;

public class CheatTracker {

    private long regenHPSince;
    private long regenMPSince;
    private int numHPRegens;
    private int numMPRegens;
    private int numSequentialAttacks;
    private long lastAttackTime;
    private long lastDamage = 0;
    private long takingDamageSince;
    private byte numZeroDamageTaken = 0;
    private int numSequentialDamage = 0;
    private long lastDamageTakenTime = 0;
    private int numSequentialSummonAttack = 0;
    private long summonSummonTime = 0;
    private int numSameDamage = 0;
    private int numGotMissed;
    private long attackingSince;
    private Point lastMonsterMove;
    private int monsterMoveCount;
    private int attacksWithoutHit = 0;
    private byte dropsPerSecond = 0;
    private long lastDropTime = 0;
    private int gmMessage = 100;
    private WeakReference<Player> p;
    private long[] lastTime = new long[6];
    private Boolean pickupComplete = Boolean.TRUE;
    private ScheduledFuture<?> invalidationTask;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock rL = lock.readLock(), wL = lock.writeLock();
    private Map<CheatingOffense, CheatingOffenseEntry> offenses = Collections.synchronizedMap(new LinkedHashMap<>());

    public CheatTracker(Player p) {
        this.p = new WeakReference<>(p);
        invalidationTask = CheatTrackerTimer.getInstance().register(new InvalidationTask(), 60000);
        takingDamageSince = attackingSince = regenMPSince = regenHPSince = System.currentTimeMillis();
    }

    /**
     * Speed Hack Checker
     *
     * @param skillId
     * @return speedhacking or not
     */
    public boolean checkAttack(int skillId) {
        numSequentialAttacks++;
        long oldLastAttackTime = lastAttackTime;
        lastAttackTime = System.currentTimeMillis();
        long attackTime = lastAttackTime - attackingSince;
        if (numSequentialAttacks > 3) {
            final int divisor;
            if (skillId != Bowmaster.Hurricane) {
                divisor = 50;
            } else {
                divisor = 350;
            }
            if (attackTime / divisor < numSequentialAttacks) {
                registerOffense(CheatingOffense.FASTATTACK);
                return false;
            }
        }
        if (lastAttackTime - oldLastAttackTime > 1500) {
            attackingSince = lastAttackTime;
            numSequentialAttacks = 0;
        }
        return true;
    }
    
    public final void checkDrop(final boolean dc) {
        if ((System.currentTimeMillis() - lastDropTime) < 1000) {
            dropsPerSecond++;
            if (dropsPerSecond >= (dc ? 32 : 16) && p.get() != null  && !p.get().isGameMaster()) {
                if (dc) {
                    p.get().getClient().getSession().close();
                } else {
                    FileLogger.print(FileLogger.EXPLOITS, "" + p.get().getName() + "Check Drop method hack");
                }
            }
        } else {
            dropsPerSecond = 0;
        }
        lastDropTime = System.currentTimeMillis();
    }
    
    public final void checkMoveMonster(final Point pos) {
        if (pos == lastMonsterMove) {
            monsterMoveCount++;
            if (monsterMoveCount > 15) {
                registerOffense(CheatingOffense.MOVE_MONSTERS);
            }
        } else {
            lastMonsterMove = pos;
            monsterMoveCount = 1;
        }
    }

    /**
     * Checks God Mode
     *
     */
    public void checkTakeDamage() {
        numSequentialDamage++;
        long oldLastDamageTakenTime = lastDamageTakenTime;
        lastDamageTakenTime = System.currentTimeMillis();

        long timeBetweenDamage = lastDamageTakenTime - takingDamageSince;

        if (timeBetweenDamage / 1000 < numSequentialDamage) {
            registerOffense(CheatingOffense.FAST_TAKE_DAMAGE);
        }
        if (lastDamageTakenTime - oldLastDamageTakenTime > 4500) {
            takingDamageSince = lastDamageTakenTime;
            numSequentialDamage = 0;
        }
    }
    
    
    public final void checkTakeDamage(final int damage) {
        numSequentialDamage++;
        lastDamageTakenTime = System.currentTimeMillis();
        if (lastDamageTakenTime - takingDamageSince / 500 < numSequentialDamage) {
            registerOffense(CheatingOffense.FAST_TAKE_DAMAGE);
        }
        if (lastDamageTakenTime - takingDamageSince > 4500) {
            takingDamageSince = lastDamageTakenTime;
            numSequentialDamage = 0;
        }
        if (damage == 0) {
            numZeroDamageTaken++;
            if (numZeroDamageTaken >= 35) { 
                numZeroDamageTaken = 0;
                registerOffense(CheatingOffense.HIGH_AVOID);
            }
        } else if (damage != -1) {
            numZeroDamageTaken = 0;
        }
    }
    
    public final void checkSameDamage(final int dmg) {
        if (dmg > 2000 && lastDamage == dmg) {
            numSameDamage++;

            if (numSameDamage > 5) {
                numSameDamage = 0;
                registerOffense(CheatingOffense.SAME_DAMAGE, numSameDamage + " times: " + dmg);
            }
        } else {
            lastDamage = dmg;
            numSameDamage = 0;
        }
    }

    public int checkDamage(long dmg) {
        if (dmg > 1 && lastDamage == dmg) {
            numSameDamage++;
        } else {
            lastDamage = dmg;
            numSameDamage = 0;
        }
        return numSameDamage;
    }

    /**
     * Type 0 - Save limit.
     * Type 1 - Contact GM.
     * Type 2 - Mesos drop.
     * Type 3 - Smega.
     * Type 4 - NPC.
     * Type 5 - Change map.
     * Type 6 - N/A.
     * Type 7 - Commands.
     * 
     * @param limit
     * @param type
     * @return
     */
    public synchronized boolean Spam(int limit, int type) {
        if (type < 0 || lastTime.length < type) {
            type = 1;  
        }
        if (System.currentTimeMillis() < limit + lastTime[type]) {
            return true;
        }
        lastTime[type] = System.currentTimeMillis();
        return false;
    }

    /**
     * Resets HP Regen handler if Hp is full
     *
     */
    public void resetHPRegen() {
        regenHPSince = System.currentTimeMillis();
        numHPRegens = 0;
    }

    /**
     * Checks HP Regen speed
     *
     * @return Fast HP Regen or not
     */
    public boolean checkHPRegen() {
        if (p.get().getStat().getHp() == p.get().getStat().getMaxHp()) {
            resetHPRegen();
            return true;
        }
        numHPRegens++;
        if ((System.currentTimeMillis() - regenHPSince) / 10000 < numHPRegens) {
            registerOffense(CheatingOffense.FAST_HP_REGEN);
            return false;
        }
        return true;
    }

    /**
     * Resets MP Regen handler if Mp is full
     *
     */
    public void resetMPRegen() {
        regenMPSince = System.currentTimeMillis();
        numMPRegens = 0;
    }

    /**
     * Checks MP Regen speed
     *
     * @return Fast MP Regen or not
     */
    public boolean checkMPRegen() {
        if (p.get().getStat().getMp() == p.get().getStat().getMaxMp()) {
            resetMPRegen();
            return true;
        }
        numMPRegens++;
        long allowedRegens = (System.currentTimeMillis() - regenMPSince) / 10000;
        if (allowedRegens < numMPRegens) {
            registerOffense(CheatingOffense.FAST_MP_REGEN);
            return false;
        }
        return true;
    }

    public void resetSummonAttack() {
        summonSummonTime = System.currentTimeMillis();
        numSequentialSummonAttack = 0;
    }

   public final boolean checkSummonAttack() {
        numSequentialSummonAttack++;
        if ((System.currentTimeMillis() - summonSummonTime) / (2000 + 1) < numSequentialSummonAttack) {
            registerOffense(CheatingOffense.FAST_SUMMON_ATTACK);
            return false;
        }
        return true;
    }


    public void checkPickupAgain() {
        synchronized (pickupComplete) {
            if (pickupComplete) {
                pickupComplete = Boolean.FALSE;
            } else {
                registerOffense(CheatingOffense.TUBI);
            }
        }
    }

    public void pickupComplete() {
        synchronized (pickupComplete) {
            pickupComplete = Boolean.TRUE;
        }
    }

    public int getAttacksWithoutHit() {
        return attacksWithoutHit;
    }

    public void setAttacksWithoutHit(int attacksWithoutHit) {
        this.attacksWithoutHit = attacksWithoutHit;
    }
    
    public int getNumGotMissed() {
        return numGotMissed;
    }

    public void setNumGotMissed(final int ngm) {
        numGotMissed = ngm;
    }

    public void incrementNumGotMissed() {
        numGotMissed++;
    }

    public void registerOffense(CheatingOffense offense) {
        registerOffense(offense, null);
    }

    public void registerOffense(CheatingOffense offense, String param) {
        Player chrhardref = p.get();
        if (chrhardref == null || !offense.isEnabled() || chrhardref.isGameMaster()) {
            return;
        }

        CheatingOffenseEntry entry = null;
        rL.lock();
        try {
            entry = offenses.get(offense);
        } finally {
            rL.unlock();
        }
        if (entry != null && entry.isExpired()) {
            expireEntry(entry);
            entry = null;
        }
        if (entry == null) {
            entry = new CheatingOffenseEntry(offense, chrhardref);
        }
        if (param != null) {
            entry.setParam(param);
        }
        entry.incrementCount();
        if (offense.shouldAutoban(entry.getCount())) {
            AutobanManager.getInstance().autoban(chrhardref.getClient(), StringUtil.makeEnumHumanReadable(offense.name()));
        }
        wL.lock();
        try {
            offenses.put(offense, entry);
        } finally {
            wL.unlock();
        }
        switch (offense) {
            case HIGH_DAMAGE:
            case ATTACK_FARAWAY_MONSTER:
            case SAME_DAMAGE:
                gmMessage--;
                if (gmMessage == 0) {
                    BroadcastService.broadcastGMMessage(PacketCreator.ServerNotice(6, "[GM Message] " + PlayerStringUtil.makeMapleReadable(chrhardref.getName()) + " suspected of hacking! " + StringUtil.makeEnumHumanReadable(offense.name()) + (param == null ? "" : (" - " + param))));
                    gmMessage = 100;
                }
                break;
        }
        CheatingOffensePersister.getInstance().persistEntry(entry);
    }

    public final void expireEntry(final CheatingOffenseEntry coe) {
        wL.lock();
        try {
            offenses.remove(coe.getOffense());
        } finally {
            wL.unlock();
        }
    }

    public final int getPoints() {
        int ret = 0;
        CheatingOffenseEntry[] offenses_copy;
        rL.lock();
        try {
            offenses_copy = offenses.values().toArray(new CheatingOffenseEntry[offenses.size()]);
        } finally {
            rL.unlock();
        }
        for (final CheatingOffenseEntry entry : offenses_copy) {
            if (entry.isExpired()) {
                expireEntry(entry);
            } else {
                ret += entry.getPoints();
            }
        }
        return ret;
    }

    public final Map<CheatingOffense, CheatingOffenseEntry> getOffenses() {
        return Collections.unmodifiableMap(offenses);
    }

    public final String getSummary() {
        final StringBuilder ret = new StringBuilder();
        final List<CheatingOffenseEntry> offenseList = new ArrayList<>();
        rL.lock();
        try {
            for (final CheatingOffenseEntry entry : offenses.values()) {
                if (!entry.isExpired()) {
                    offenseList.add(entry);
                }
            }
        } finally {
            rL.unlock();
        }
        Collections.sort(offenseList, (final CheatingOffenseEntry o1, final CheatingOffenseEntry o2) -> {
            final int thisVal = o1.getPoints();
            final int anotherVal = o2.getPoints();
            return (thisVal < anotherVal ? 1 : (thisVal == anotherVal ? 0 : -1));
        });
        final int to = Math.min(offenseList.size(), 4);
        for (int x = 0; x < to; x++) {
            ret.append(StringUtil.makeEnumHumanReadable(offenseList.get(x).getOffense().name()));
            ret.append(": ");
            ret.append(offenseList.get(x).getCount());
            if (x != to - 1) {
                ret.append(" ");
            }
        }
        return ret.toString();
    }

    public final void dispose() {
        if (invalidationTask != null) {
            invalidationTask.cancel(false);
        }
        invalidationTask = null;
    }

    private final class InvalidationTask implements Runnable {

        @Override
        public final void run() {
            CheatingOffenseEntry[] offensesCopy;
            rL.lock();
            try {
                offensesCopy = offenses.values().toArray(new CheatingOffenseEntry[offenses.size()]);
            } finally {
                rL.unlock();
            }
            for (CheatingOffenseEntry offense : offensesCopy) {
                if (offense.isExpired()) {
                    expireEntry(offense);
                }
            }
            if (p.get() == null) {
                dispose();
            }
        }
    }
}