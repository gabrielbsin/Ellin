/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package server.partyquest.mcpq;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import packet.creators.CarnivalPackets;
import packet.creators.PacketCreator;
import client.player.Player;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.life.MobSkillFactory;
import server.life.SpawnPoint;
import server.maps.Field;
import server.maps.object.FieldObjectType;
import server.maps.reactors.Reactor;
import server.maps.reactors.ReactorFactory;
import server.partyquest.mcpq.MCField.MCTeam;
import static server.partyquest.mcpq.MCField.MCTeam.RED;
import server.partyquest.mcpq.MCWZData.MCGuardianGenPos;
import server.partyquest.mcpq.MCWZData.MCMobGenPos;
import server.partyquest.mcpq.MCWZData.MCSummonMob;


/**
 * Keeps track of guardians and spawns in MCPQ.
 *
 * @author s4nta
 */
public class MCBattlefield {

    private final Field map;
    private MCWZData wzData;
    private int numGuardiansSpawned = 0;
    private int numMonstersSpawned = 0;
    private final Map<Integer, MCGuardianGenPos> redGuardianIdToPos = new HashMap<>();
    private final Map<Integer, MCGuardianGenPos> blueGuardianIdToPos = new HashMap<>();
    private final Map<Integer, MCGuardian> redReactors = new HashMap<>();
    private final Map<Integer, MCGuardian> blueReactors = new HashMap<>();
    private final List<MCGuardianGenPos> originalRedGuardianSpawns = new ArrayList<>();
    private final List<MCGuardianGenPos> originalBlueGuardianSpawns = new ArrayList<>();
    private final List<MCGuardianGenPos> originalGuardianSpawns = new ArrayList<>();
    private final List<MCMobGenPos> originalRedSpawns = new ArrayList<>();
    private final List<MCMobGenPos> originalBlueSpawns = new ArrayList<>();
    private final List<MCMobGenPos> originalUnifiedSpawns = new ArrayList<>();
    private final List<SpawnPoint> originalSpawns = new ArrayList<>();
    private final List<SpawnPoint> addedSpawns = new ArrayList<>();

    public MCBattlefield(Field battleInstance) {
        this.map = battleInstance;
        fetchCarnivalData();
        getOriginalSpawnPoints();
        populateGuardianSpawns();
        populateMobSpawns();
    }

    private void fetchCarnivalData() {
        wzData = this.map.getMCPQData();
        if (wzData == null) {
            MCTracker.log("[MCPQ] Fetching carnival failed for map " + map.getId());
        }
    }

    private void getOriginalSpawnPoints() {
        this.map.getSpawnPoints().forEach((sp) -> {
            originalSpawns.add((SpawnPoint) sp);
        });
    }

    private void populateGuardianSpawns() {
        for (MCGuardianGenPos gpos : wzData.guardianGenPosList) {
            switch (gpos.team) {
                case 0:
                    originalRedGuardianSpawns.add(gpos);
                    break;
                case 1:
                    originalBlueGuardianSpawns.add(gpos);
                    break;
                default:
                    originalGuardianSpawns.add(gpos);
            }
        }
    }

    private void populateMobSpawns() {
        for (MCMobGenPos mpos : wzData.mobGenPosList) {
            switch (mpos.team) {
                case 0:
                    originalRedSpawns.add(mpos);
                    break;
                case 1:
                    originalBlueSpawns.add(mpos);
                    break;
                default:
                    originalUnifiedSpawns.add(mpos);
                    break;
            }
        }
    }

    public void addSpawn(Player chr, int num) {
        if (numMonstersSpawned > wzData.mobGenMax) {
            chr.getClient().announce(CarnivalPackets.CarnivalMessage(3));
            return;
        }

        MCSummonMob mobToSummon = wzData.summons.get(num);
        MCMobGenPos spawnPos = getRandomSpawnPos(chr.getMCPQTeam());

        MCTeam team = chr.getMCPQTeam();
        if (spawnPos == null) { 
            chr.getClient().announce(CarnivalPackets.CarnivalMessage(2));
            return;
        }

        int spendCp = mobToSummon.spendCP;
        if (spendCp > chr.getAvailableCP()) {
            readdSpawn(spawnPos, team);
            chr.getClient().announce(CarnivalPackets.CarnivalMessage(1));
            return;
        }

        chr.getMCPQField().loseCP(chr, spendCp);
        this.map.broadcastMessage(CarnivalPackets.PlayerSummoned(MonsterCarnival.TAB_SPAWNS, num, chr.getName()));
        numMonstersSpawned++; 

        MapleMonster monster = MapleLifeFactory.getMonster(mobToSummon.id);
        Point pos = new Point(spawnPos.x, spawnPos.y);
        SpawnPoint sp = new SpawnPoint(monster, pos, !monster.isMobile(), mobToSummon.mobTime, 0, chr.getTeam());

        addedSpawns.add(sp);
        updateMonsterBuffs();
    }

    public void useSkill(Player chr, int num) {
        if (!wzData.skills.containsKey(num)) {
            MCTracker.log("Attempting to use a null skill.");
            return;
        }
        int realSkill = wzData.skills.get(num);
        MCSkill skill = MCSkillFactory.getMCSkill(realSkill);

        int spendCp = skill.getSpendCP();
        if (spendCp > chr.getAvailableCP()) {
            chr.getClient().announce(CarnivalPackets.CarnivalMessage(1));
            return;
        }

        MCParty teamToApply = chr.getMCPQParty().getEnemy();
        boolean success = teamToApply.applyMCSkill(skill);

        if (success) {
            chr.getMCPQField().loseCP(chr, spendCp);
            map.broadcastMessage(CarnivalPackets.PlayerSummoned(MonsterCarnival.TAB_DEBUFF, num, chr.getName()));
        } else {
            chr.getClient().getSession().write(CarnivalPackets.CarnivalMessage(5));
        }
    }
    
    public void readdSpawn(MCMobGenPos pos, MCTeam team) {
        List<MCMobGenPos> lst = null;
        if (this.wzData.mapDivided) {
            if (null == team) {
                return;
            } else switch (team) {
                case RED:
                    lst = originalRedSpawns;
                    break;
                case BLUE:
                    lst = originalBlueSpawns;
                    break;
                default:
                    return;
            }
        } else {
            lst = originalUnifiedSpawns;
        }
        
        if (lst == null) {
            return;
        } 
        lst.add(pos);
    }

    public void spawnGuardian(Player chr, int num) {
        if (numGuardiansSpawned > wzData.guardianGenMax) {
            chr.getClient().announce(CarnivalPackets.CarnivalMessage(3));
            return;
        }

        int guardianId = wzData.guardians.get(num);
        MCGuardian guardian = MCSkillFactory.getMCGuardian(guardianId);
        if (guardian == null) {
            MCTracker.log("Attempting to spawn invalid guardian.");
            return;
        }

        MCTeam team = chr.getMCPQTeam();
        if (team == MCTeam.RED) {
            if (redGuardianIdToPos.containsKey(guardianId)) {
                chr.getClient().announce(CarnivalPackets.CarnivalMessage(4));
                return;
            }
        } else if (team == MCTeam.BLUE) {
            if (blueGuardianIdToPos.containsKey(guardianId)) {
                chr.getClient().announce(CarnivalPackets.CarnivalMessage(4));
                return;
            }
        }
        int spendCp = guardian.getSpendCP();
        if (spendCp > chr.getAvailableCP()) {
            chr.getClient().announce(CarnivalPackets.CarnivalMessage(1));
            return;
        }

        chr.getMCPQField().loseCP(chr, spendCp);
        this.map.broadcastMessage(CarnivalPackets.PlayerSummoned(MonsterCarnival.TAB_GUARDIAN, num, chr.getName()));
        numGuardiansSpawned++; 
        MCGuardianGenPos genPos = getRandomGuardianPos(team);
        Point spawnPos = new Point(genPos.x, genPos.y);

        Reactor reactor;
        if (null == team) {
            return;
        } else switch (team) {
            case RED:
                reactor = new Reactor(ReactorFactory.getReactor(MonsterCarnival.GUARDIAN_RED), MonsterCarnival.GUARDIAN_RED);
                reactor.setPosition(spawnPos);
                redGuardianIdToPos.put(num, genPos);
                break;
            case BLUE:
                reactor = new Reactor(ReactorFactory.getReactor(MonsterCarnival.GUARDIAN_BLUE), MonsterCarnival.GUARDIAN_BLUE);
                reactor.setPosition(spawnPos);
                blueGuardianIdToPos.put(num, genPos);
                break;
            default:
                return;
        }

        reactor.setDelay(-1);
        map.spawnReactor(reactor);

        if (team == MCTeam.RED) {
            redReactors.put(reactor.getObjectId(), MCSkillFactory.getMCGuardian(num));
        } else {
            blueReactors.put(reactor.getObjectId(), MCSkillFactory.getMCGuardian(num));
        }

        map.setReactorState(reactor, (byte) 1); 
        updateMonsterBuffs();
    }

    public void onGuardianHit(Player p, Reactor reactor) {
        if (MonsterCarnival.DEBUG) {
            System.out.println("STATE: " + reactor.getState());
        }
        MCTeam team = p.getMCPQTeam();
        if (team == MCTeam.RED && reactor.getId() == MonsterCarnival.GUARDIAN_RED) {
            return;
        }
        if (team == MCTeam.BLUE && reactor.getId() == MonsterCarnival.GUARDIAN_BLUE) {
            return;
        }
        reactor.setState((byte) (reactor.getState() + 1));
        map.broadcastMessage(PacketCreator.TriggerReactor(reactor, reactor.getState()));

        if (reactor.getState() > 3) {
            int reactorObjId = reactor.getObjectId();
            map.destroyReactor(reactorObjId);

            MCGuardian guard;
            MCWZData.MCGuardianGenPos guardianGenPos;
            if (team == MCField.MCTeam.RED) {
                guard = blueReactors.remove(reactorObjId);
                guardianGenPos = blueGuardianIdToPos.remove(guard.getType());
            } else {
                guard = redReactors.remove(reactorObjId);
                guardianGenPos = redGuardianIdToPos.remove(guard.getType());
            }
            numGuardiansSpawned--;
            
            if (MonsterCarnival.DEBUG) {
                System.out.println("Removing reactor with x = " + guardianGenPos.x);
            }
            if (wzData.mapDivided) {
                if (team == MCTeam.RED) {
                    originalBlueGuardianSpawns.add(guardianGenPos);
                } else {
                    originalRedGuardianSpawns.add(guardianGenPos);
                }
            } else {
                originalGuardianSpawns.add(guardianGenPos);
            }

            if (MonsterCarnival.DEBUG) {
                System.out.println("Attempting to remove buff " + guard.getName());
            }
            updateMonsterBuffs();
        }
    }

    private MCGuardianGenPos getRandomGuardianPos(MCTeam team) {
        if (this.wzData.mapDivided) {
            if (null == team) {
                return null;
            } else switch (team) {
                case RED: {
                    int randIndex = (int) Math.floor(Math.random() * this.originalRedGuardianSpawns.size());
                    return originalRedGuardianSpawns.remove(randIndex);
                }
                case BLUE: {
                    int randIndex = (int) Math.floor(Math.random() * this.originalBlueGuardianSpawns.size());
                    return originalBlueGuardianSpawns.remove(randIndex);
                }
                default:
                    return null;
            }
        } else {
            int randIndex = (int) Math.floor(Math.random() * this.originalGuardianSpawns.size());
            return originalGuardianSpawns.remove(randIndex);
        }
    }

    private MCMobGenPos getRandomSpawnPos(MCTeam team) {
        List<MCMobGenPos> lst = null;
        if (this.wzData.mapDivided) {
            if (null == team) {
                return null;
            } else switch (team) {
                case RED:
                    lst = originalRedSpawns;
                    break;
                case BLUE:
                    lst = originalBlueSpawns;
                    break;
                default:
                    return null;
            }
        } else {
            lst = originalUnifiedSpawns;
        }
        
        if (lst == null) {
            return null;
        } 
        if (lst.isEmpty()) {
            return null;
        }
        int randIndex = (int) Math.floor(Math.random() * lst.size());
        return lst.remove(randIndex);
    }

    private void updateMonsterBuffs() {
        List<MCGuardian> redGuardians = new ArrayList<>();
        List<MCGuardian> blueGuardians = new ArrayList<>();

        this.redReactors.values().stream().map((g) -> {
            redGuardians.add(g);
            return g;
        }).filter((g) -> (MonsterCarnival.DEBUG)).forEachOrdered((g) -> {
            System.out.println("update buff red " + g.getMobSkillID());
        });
        this.blueReactors.values().stream().map((g) -> {
            blueGuardians.add(g);
            return g;
        }).filter((g) -> (MonsterCarnival.DEBUG)).forEachOrdered((g) -> {
            System.out.println("update buff blue " + g.getMobSkillID());
        });

        map.getAllMonsters().stream().filter((mmo) -> (mmo.getType() == FieldObjectType.MONSTER)).map((mmo) -> ((MapleMonster) mmo)).map((mob) -> {
            mob.dispel();
            return mob;
        }).forEachOrdered((mob) -> {
            if (mob.getTeam() == MCField.MCTeam.RED.code) {
                applyGuardians(mob, redGuardians);
            } else if (mob.getTeam() == MCField.MCTeam.BLUE.code) {
                applyGuardians(mob, blueGuardians);
            } else {
                MCTracker.log("[MCPQ] Attempting to give guardians to mob without team.");
            }
        });
    }

    private void giveMonsterBuffs(MapleMonster mob) {
        List<MCGuardian> redGuardians = new ArrayList<>();
        List<MCGuardian> blueGuardians = new ArrayList<>();

        this.redReactors.values().stream().map((g) -> {
            redGuardians.add(g);
            return g;
        }).filter((g) -> (MonsterCarnival.DEBUG)).forEachOrdered((g) -> {
            System.out.println("update buff red " + g.getMobSkillID());
        });
        this.blueReactors.values().stream().map((g) -> {
            blueGuardians.add(g);
            return g;
        }).filter((g) -> (MonsterCarnival.DEBUG)).forEachOrdered((g) -> {
            System.out.println("update buff blue " + g.getMobSkillID());
        });

        if (mob.getTeam() == MCTeam.RED.code) {
            applyGuardians(mob, redGuardians);
        } else if (mob.getTeam() == MCTeam.BLUE.code) {
            applyGuardians(mob, blueGuardians);
        } else {
            MCTracker.log("[MCPQ] Attempting to give guardians to mob without team.");
        }
    }

    private void applyGuardians(MapleMonster mob, List<MCGuardian> guardians) {
        guardians.stream().map((g) -> MobSkillFactory.getMobSkill(g.getMobSkillID(), g.getLevel())).forEachOrdered((sk) -> {
            sk.applyEffect(null, mob, true);
        });
    }

    public void spawningTask() {
        for (SpawnPoint sp : originalSpawns) {
            if (sp.shouldSpawn()) {
                MapleMonster m = sp.getMonster();
                giveMonsterBuffs(m);
                this.map.spawnMonster(m);
            }
        }
        for (SpawnPoint sp : addedSpawns) {
            if (sp.shouldSpawn()) {
                MapleMonster m = sp.getMonster();
                giveMonsterBuffs(m);
                this.map.spawnMonster(m);
            }
        }
    } 
}  
