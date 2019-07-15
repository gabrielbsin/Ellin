/*
 This file is part of the OdinMS Maple Story Server
 Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as
 published by the Free Software Foundation version 3 as published by
 the Free Software Foundation. You may not use, modify or distribute
 this program under any other version of the GNU Affero General Public
 License.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package server.quest.actions;

import client.player.Player;
import client.player.PlayerJob;
import client.player.skills.PlayerSkill;
import client.player.skills.PlayerSkillFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import provider.MapleData;
import provider.MapleDataTool;
import server.quest.MapleQuest;
import server.quest.MapleQuestActionType;

/**
 *
 * @author Tyler (Twdtwd)
 */
public final class SkillAction extends MapleQuestAction {
    int itemEffect;
    Map<Integer, SkillData> skillData = new HashMap<>();

    public SkillAction(MapleQuest quest, MapleData data) {
        super(MapleQuestActionType.SKILL, quest);
        processData(data);
    }
	
    @Override
    public void processData(MapleData data) {
        for (MapleData sEntry : data) {
            byte skillLevel = 0;
            int skillid = MapleDataTool.getInt(sEntry.getChildByPath("id"));
            MapleData skillLevelData = sEntry.getChildByPath("skillLevel");
            if(skillLevelData != null)
                    skillLevel = (byte) MapleDataTool.getInt(skillLevelData);
            int masterLevel = MapleDataTool.getInt(sEntry.getChildByPath("masterLevel"));
            List<Integer> jobs = new ArrayList<>();

            MapleData applicableJobs = sEntry.getChildByPath("job");
            if (applicableJobs != null) {
                for (MapleData applicableJob : applicableJobs.getChildren()) {
                    jobs.add(MapleDataTool.getInt(applicableJob));
                }
            }
          skillData.put(skillid, new SkillData(skillid, skillLevel, masterLevel, jobs));
        }
    }
	
    @Override
    public void run(Player p, Integer extSelection) {
        for (SkillData skill : skillData.values()) {
            PlayerSkill skillObject = PlayerSkillFactory.getSkill(skill.getId());
            boolean shouldLearn = false;
            if(skill.jobsContains(p.getJob()) || skillObject.isBeginnerSkill()) {
                shouldLearn = true;
            }
            byte skillLevel = (byte) Math.max(skill.getLevel(), p.getSkillLevel(skillObject));
            int masterLevel = Math.max(skill.getMasterLevel(), p.getMasterLevel(skillObject));
            if (shouldLearn) {
                p.changeSkillLevel(skillObject, skillLevel, masterLevel);
            }
        }
    }
	
    private class SkillData {
        protected int id, level, masterLevel;
        List<Integer> jobs = new ArrayList<>();

        public SkillData(int id, int level, int masterLevel, List<Integer> jobs) {
            this.id = id;
            this.level = level;
            this.masterLevel = masterLevel;
            this.jobs = jobs;
        }

        public int getId() {
            return id;
        }

        public int getLevel() {
            return level;
        }

        public int getMasterLevel() {
            return masterLevel;
        }

        public boolean jobsContains(PlayerJob job) {
            return jobs.contains(job.getId());
        }
    }
} 