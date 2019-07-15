///*
// * To change this license header, choose License Headers in Project Properties.
// * To change this template file, choose Tools | Templates
// * and open the template in the editor.
// */
//
//package server.life;
//
//import client.player.Player;
//import java.awt.Point;
//import java.util.concurrent.atomic.AtomicBoolean;
//import packet.creators.PacketCreator;
//import server.maps.Field;
//import tools.Randomizer;
//
///**
// * 
// * @author GabrielSin
// */
//public class SpawnPointAreaBoss extends Spawns {
//    
//    private MapleMonsterStats monster;
//    private Point pos1;
//    private Point pos2;
//    private Point pos3;
//    private long nextPossibleSpawn;
//    private int mobTime, fh, f, id;
//    private String msg;
//    private boolean denySpawn = false;
//    private final AtomicBoolean spawned = new AtomicBoolean(false);
//    
//    public SpawnPointAreaBoss(final MapleMonster monster, final Point pos1, final Point pos2, final Point pos3, final int mobTime, final String msg, final boolean shouldSpawn) {
//        if (monster != null) {
//            this.monster = monster.getStats();
//            this.id = monster.getId();
//            this.fh = monster.getFh();
//            this.f = monster.getF();
//            this.pos1 = pos1;
//            this.pos2 = pos2;
//            this.pos3 = pos3;
//            this.mobTime = (mobTime < 0 ? -1 : (mobTime * 1000));
//            this.msg = msg;
//            this.nextPossibleSpawn = System.currentTimeMillis() + (shouldSpawn ? 0 : this.mobTime);
//        }
//    }
//    
//    @Override
//    public void setDenySpawn(boolean val) {
//        denySpawn = val;
//    }
//    
//    @Override
//    public boolean getDenySpawn() {
//        return denySpawn;
//    }
//    
//    @Override
//    public final int getF() {
//	return f;
//    }
//
//    @Override
//    public final int getFh() {
//	return fh;
//    }
//    
//    @Override
//    public final MapleMonsterStats getMonster() {
//        return monster;
//    }
//    
//    @Override
//    public final boolean shouldSpawn() {
//        return shouldSpawn(System.currentTimeMillis());
//    }
//
//    @Override
//    public final boolean shouldSpawn(long time) {
//        if (mobTime < 0 || spawned.get() || denySpawn) {
//            return false;
//        }
//        return nextPossibleSpawn <= time;
//    }
//
//    /**
//     *
//     * @return
//     */
//    @Override
//    public final Point getPosition() {
//        final int rand = Randomizer.nextInt(3);
//        return rand == 0 ? pos1 : rand == 1 ? pos2 : pos3;
//    }
//    
//    @Override
//    public final MapleMonster getMonster(final Field map) {
//	final Point pos = getPosition();
//        final MapleMonster mob = new MapleMonster(id, monster);
//        mob.setPosition(pos);
//	mob.setCy(pos.y);
//	mob.setRx0(pos.x - 50);
//	mob.setRx1(pos.x + 50); 
//	mob.setFh(fh);
//	mob.setF(f);
//        spawned.set(true);
//        mob.addListener(new MonsterListener() {
//              @Override
//            public void monsterKilled(int aniTime) {
//                nextPossibleSpawn = System.currentTimeMillis();
//
//                if (mobTime > 0) {
//                    nextPossibleSpawn += mobTime;
//                } else {
//                    nextPossibleSpawn += aniTime;
//                }
//                spawned.set(false);
//            }
//
//            @Override
//            public void monsterDamaged(Player from, int trueDmg) {}
//
//            @Override
//            public void monsterHealed(int trueHeal) {
//            }
//        });
//        map.spawnMonster(mob);
//
//        if (msg != null) {
//            map.broadcastMessage(PacketCreator.ServerNotice(6, msg));
//        }
//        return mob;
//    }
//    
//    @Override
//    public final int getMonsterId() {
//        return id;
//    }
//    
//    @Override
//    public final int getMobTime() {
//        return mobTime;
//    }
//
//    @Override
//    public int getTeam() {
//        return -1;
//    }
//
//    @Override
//    public boolean isTemporary() {
//        return false;
//    }
//
//    @Override
//    public void setTemporary(boolean setTemporary) {
//    }
//    
//    @Override
//    public boolean shouldForceSpawn() {
//        return false;
//    }
//}
