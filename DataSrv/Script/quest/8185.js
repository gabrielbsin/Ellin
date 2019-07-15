/* Author: Xterminator (Modified by RMZero213)
	NPC Name: 		Roger
	Map(s): 		Maple Road : Lower level of the Training Camp (2)
	Description: 		Quest - Roger's Apple
*/

importPackage(net.sf.odinms.net.channel.handler);

var status = -1;

function start(mode, type, selection) {
	if (mode == -1) {
		qm.dispose();
	} else {
		if (mode == 1)
			status++;
		else
			status--;
		if (status == 0) {
			qm.sendNext("With all the strange occurrences in Masteria and New Leaf City, Lita Lawless must be busy! I'll bet that she's willing to accept my help...maybe I can earn some mesos in the process! A quick jaunt to the Kerning City subway and I'll be on my way to New Leaf City!");
		} else if (status == 1) {
			qm.sendNextPrev("I spoke with Lita, and she told me the tragic origin of the Headless Horseman. He was a former warrior of Crimsonwood Keep that was experimented on by the mysterious Alchemist, the same one who corrupted the Krakians. He now roams the Phantom Forest, and I must bring his Jack O'Lantern head to Lita as proof of my triumph!");
		} else if (status == 2) {
			qm.sendAcceptDecline("So you'll help me?");
		} else if (status == 3) {
			
			qm.forceStartQuest();
			qm.dispose();
		}
	}
}

var status = -1;

function end(mode, type, selection) {
	if (mode == -1) {
		qm.dispose();
	} else {
		if (mode == 1)
			status++;
		else
			status--;
		if (status == 0) {
			qm.sendNextPrev("#e#bHey, you did it!#n#k \r\n#rWow!#k Now I could complete my studies on your pet!");
		} else if (status == 1) {
			if (mode == 0) {
				qm.sendOk("I see... Come back when you wish to do it. I'm really excited to do this.");
				qm.dispose();
			} else {
				qm.sendNextPrev("Just saying, your new dragon's color is gonna be #e#rrandom#k#n! It's either gonna be #ggreen, #bblue, #rred, #dor very rarely#k, black. \r\n\r\n#fUI/UIWindow.img/QuestIcon/5/0# \r\n\r If you happen to not like your pet's new color, or if you ever wish to change your pet color again, #eyou can change it!#n Simply just #dbuy another Rock of Evolution, 10,000 mesos, #kand #dequip your new pet#k before talking to me again, but of course, I cannot return your pet as a baby dragon, only to another adult dragon.");
			}
		} else if (status == 2) {
			qm.sendYesNo("Now let me try to evolve your pet. You ready? Wanna see your cute baby dragon turn into either a matured dark black, blue, calm green, or fiery red adult dragon? It'll still have the same closeness, level, name, fullness, hunger, and equipment in case you're worried. \r\n\r #b#eDo you wish to continue or do you have some last-minute things to do first?#k#n");
                } else if (status == 3) {
			qm.sendNextPrev("Alright, here we go...! #rHYAHH!#k");
		} else if (status == 4) {
			var rand = 1 + Math.floor(Math.random() * 10);
			var after = 0;
			if (rand >= 1 && rand <= 3) {
				after = 5000030;
			} else if (rand >= 4 && rand <= 6) {
				after = 5000031;
			} else if (rand >= 7 && rand <= 9) {
				after = 5000032;
			} else if (rand == 10) {
				after = 5000033;
			} else {
				qm.sendOk("Something wrong. Try again.");
				qm.dispose();
			}
			qm.getPlayer().unequipAllPets(); //IMPORTANT, you can bug/crash yourself if you don't unequip the pet to be deleted
			SpawnPetHandler.evolve(qm.getPlayer().getClient(), 5000029, after);
			qm.sendOk("#bSWEET! IT WORKED!#k Your dragon has grown beautifully! #rYou may find your new pet under your 'CASH' inventory.\r It used to be a #i" + id + "##t" + id + "#, and now it's \r a #i" + after + "##t" + after + "#!#k \r\n\r\n#fUI/UIWindow.img/QuestIcon/4/0#\r\n#v"+after+"# #t"+after+"#\r\n\r\n#fUI/UIWindow.img/QuestIcon/8/0# 1000 EXP\r\n#fUI/UIWindow.img/QuestIcon/9/0# 2 Closeness\r\n#fUI/UIWindow.img/QuestIcon/6/0# 1 Fame\r\n#fUI/UIWindow.img/QuestIcon/7/0# 100 Mesos");
			qm.dispose();
		}
	}
}