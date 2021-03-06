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
import handling.channel.ChannelServer;
import provider.MapleData;
import provider.MapleDataTool;
import server.quest.MapleQuest;
import server.quest.MapleQuestActionType;

/**
 *
 * @author Tyler (Twdtwd)
 */
public final class ExpAction extends MapleQuestAction {
    int exp;

    public ExpAction(MapleQuest quest, MapleData data) {
        super(MapleQuestActionType.EXP, quest);
        processData(data);
    }

    @Override
    public void processData(MapleData data) {
        exp = MapleDataTool.getInt(data);
    }

    @Override
    public void run(Player p, Integer extSelection) {
        if (PlayerJob.isBeginner(p.getJob().getId()) && p.getLevel() < 10) {
            p.gainExperience(exp, true, true);
        } else {
            p.gainExperience(exp * ChannelServer.getInstance(p.getClient().getChannel()).getQuestRate(), true, true);  
        }
    }
} 
