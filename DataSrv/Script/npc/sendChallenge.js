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
var status = 0;
var party, mcParty;

function start(chrs, pty) {
    status = -1;
    party = chrs;
    mcParty = pty;
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode == -1) {
        cm.getPlayer().setChallenged(false);
        cm.dispose();
    } else {
        if (mode == 0) {
            mcParty.getLeader().dropMessage("The opposing party has declined your invitation.");
            cm.getPlayer().dropMessage("You have denied a '" + mcParty.getLeaderName() + "' request.");
            cm.getPlayer().setChallenged(false);
            cm.dispose();
            return;
        }
    }
    if (mode == -1) 
        cm.dispose();
    else {
        if (mode == 1)
            status++;
        else 
            status--;
        if (status == 0) {
            if (cm.getParty().getMembers().size() == party.size()) {
                var snd = "";
                cm.getPlayer().setChallenged(true);
                for (var i = 0; i < party.size(); i++) {
                    snd += "#b" + party.get(i).getName() + " / Level " + party.get(i).getLevel() + " / " + party.get(i).getJobNameById(party.get(i).getJobId()) + "#k\r\n\r\nWould you like to batlle this party at the Monster Carnival?";
                }
                cm.sendAcceptDecline(snd);
           } 
        } else if (status == 1) {
	    var code = cm.getPlayer().getMCPQField().acceptRequest(mcParty); 
            if (code == 1) {
                cm.getPlayer().dropMessage("You accepted the '" + mcParty.getLeaderName() + "' fight request.");
                cm.getPlayer().setChallenged(false);
            } else {
                cm.sendOk("An unknown error occurred.");
            }
            cm.dispose();
        }
    }
}