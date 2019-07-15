package client.player.skills;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;

import provider.MapleData;
import provider.MapleDataDirectoryEntry;
import provider.MapleDataFileEntry;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;
import tools.StringUtil;

public class PlayerSkillFactory {
    
    private static final Map<Integer, PlayerSkill> skills = new HashMap<>();
    private static final Map<Integer, PlayerSummonSkillEntry> SummonSkillInformation = new HashMap<>();
    private static final MapleData stringData = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/String")).getData("Skill.img");
   
    private final static ReentrantReadWriteLock locks = new ReentrantReadWriteLock();
    private final static ReadLock readLock = locks.readLock();

    public static final PlayerSkill getSkill(final int id) {
        if (!skills.isEmpty()) {
            readLock.lock();
            try{
                return skills.get(Integer.valueOf(id));
            } finally {
                readLock.unlock();
            }
        }
        
        final MapleDataProvider datasource = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/Skill"));
        final MapleDataDirectoryEntry root = datasource.getRoot();

        int skillid;
        MapleData summonData;
        PlayerSummonSkillEntry skillEntry;

        for (MapleDataFileEntry topDir : root.getFiles()) { 
            if (topDir.getName().length() <= 8) {
                for (MapleData data : datasource.getData(topDir.getName())) { 
                    if (data.getName().equals("skill")) {
                        for (MapleData dataTwo : data) { 
                            if (dataTwo != null) {
                                skillid = Integer.parseInt(dataTwo.getName());
                                skills.put(skillid, PlayerSkill.loadFromData(skillid, dataTwo));
                                summonData = dataTwo.getChildByPath("summon/attack1/info");
                                if (summonData != null) {
                                    skillEntry = new PlayerSummonSkillEntry();
                                    skillEntry.attackAfter = (short) MapleDataTool.getInt("attackAfter", summonData, 999999);
                                    skillEntry.type = (byte) MapleDataTool.getInt("type", summonData, 0);
                                    skillEntry.mobCount = (byte) MapleDataTool.getInt("mobCount", summonData, 1);
                                    SummonSkillInformation.put(skillid, skillEntry);
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    public static String getSkillName(int id) {
        String strId = Integer.toString(id);
        strId = StringUtil.getLeftPaddedStr(strId, '0', 7);
        MapleData skillroot = stringData.getChildByPath(strId);
        if (skillroot != null) {
            return MapleDataTool.getString(skillroot.getChildByPath("name"), "");
        }
        return null;
    }
    
    public static void cacheSkills(){
        int skillid = 9999999;
        for(MapleData skillData : stringData){
            skillid = Integer.parseInt(skillData.getName());
            try{
                if(isExist(skillid)){
                    getSkill(skillid);
                }
            } catch(RuntimeException e){
            }
        }
    }

    public static boolean isExist(int skillid) {
        PlayerSkill skill = getSkill(skillid);
        return skill != null;
    }  
    
    public static final PlayerSummonSkillEntry getSummonData(final int skillid) {
        return SummonSkillInformation.get(skillid);
    } 
}
