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

/* Grendel the Really Old
	Magician Job Advancement
	Victoria Road : Magic Library (101000003)

	Custom Quest 100006, 100008, 100100, 100101
*/

importPackage(Packages.client);
importPackage(Packages.tools);
importPackage(Packages.scripting);

var status = 0;
var job;
var pnpc = -1;

function start() {
	status = -1;
	action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode == -1) {
        cm.dispose();
    } else {
        if (mode == 0 && status == 2) {
            cm.sendOk("Make up your mind and visit me again.");
            cm.dispose();
            return;
        }
        if (mode == 1) {
            status++;
        } else {
            status--;
        }
        if (status == 0) {
            if (cm.getJobId() == 0) {
                if (cm.getLevel() > 7) {
                    cm.sendNext("So you decided to become a #rMagician#k?");
                } else {
                    cm.sendOk("Train a bit more and I can show you the way of the #rMagician#k.")
                    cm.dispose();
                }
            } else if (cm.getPlayer().getLevel() >= 200 && cm.getPlayer().getJob().isA(PlayerJob.MAGICIAN)) {
                cm.sendYesNo("Uau, voce ja atingiu o moximo de seu potencial. Voce gostaria de ter um NPC jogador representar o seu poder?");
                pnpc = 1;
            } else {
                if (cm.getLevel() >= 30 && cm.getJobId() == 200) {
                    if (cm.isQuestStarted(100006)) {
                        cm.completeQuest(100008);
                        if (cm.isQuestCompleted(100008)) {
                            status = 20;
                            cm.sendNext("I see you have done well. I will allow you to take the next step on your long road.");
                        } else {
                            cm.sendOk("Go and see the #rJob Instructor#k.")
                            cm.dispose();
                        }
                    } else {
                        status = 10;
                        cm.sendNext("The progress you have made is astonishing.");
                    }
                } else if (cm.isQuestStarted(100100)) {
                    cm.completeQuest(100101);
                    if (cm.isQuestCompleted(100101)) {
                        cm.sendOk("Alright, now take this to #bRobeira#k.");
                    } else {
                        cm.sendOk("Hey, " + cm.getPlayer().getName() + "! I need a #bBlack Charm#k. Go and find the Door of Dimension.");
                        cm.startQuest(100101);
                    }
                    cm.dispose();
                } else {
                    cm.sendOk("You have chosen wisely.");
                    cm.dispose();
                }
            }
        } else if (status == 1) {
            if (pnpc == 1) {
                var success = cm.createPlayerNPC();
                if (success) {
                    cm.sendOk("Parabens, seu simbolo de poder agora vai ser visto por todos os novos jogadores!");
                } else {
                    cm.sendOk("Houve um problema ao criar o NPC, ou voce ja tem um ou todos os pontos sao tomados.");
                }
                cm.dispose();
            } else {
                cm.sendNextPrev("It is an important and final choice. You will not be able to turn back.");
            }
        } else if (status == 2) {
            cm.sendYesNo("Do you want to become a #rMagician#k?");
        } else if (status == 3) {
            if (cm.getJobId() == 0)
                cm.changeJobById(200);
                cm.gainItem(1372005, 1);
                cm.sendOk("So be it! Now go, and go with pride.");
                cm.dispose();
            } else if (status == 11) {
                cm.sendNextPrev("You may be ready to take the next step as a #rFire/Poison Wizard#k, #rIce/Lightning Wizard#k or #rCleric#k.");
            } else if (status == 12) {
                cm.sendAcceptDecline("But first I must test your skills. Are you ready?");
            } else if (status == 13) {
                if (cm.haveItem(4031009)) {
                    cm.sendOk("ERROR!");
                } else {
                    cm.startQuest(100006);
                    cm.sendOk("Va ver o instrutor #bInstrutor de Classe#k perto de Ellinia. Ele lhe mostrara o caminho.");
                }
            } else if (status == 21) {
                cm.sendSimple("What do you want to become?#b\r\n#L0#Fire/Poison Wizard#l\r\n#L1#Ice/Lightning Wizard#l\r\n#L2#Cleric#l#k");
            } else if (status == 22) {
                var jobName;
                if (selection == 0) {
                    jobName = "Fire/Poison Wizard";
                    job = 210;
                } else if (selection == 1) {
                    jobName = "Ice/Lightning Wizard";
                    job = 220;
                } else {
                    jobName = "Cleric";
                    job = 230;
                }
                cm.sendYesNo("Do you want to become a #r" + jobName + "#k?");
            } else if (status == 23) {
                cm.changeJobById(job);
                cm.sendOk("So be it! Now go, and go with pride.");
            }
    }
}	
