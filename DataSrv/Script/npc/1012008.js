/*
 * This file is part of the OdinMS Maple Story Server
 * Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
 * 		     Matthias Butz <matze@odinms.de>
 * 		     Jan Christian Meyer <vimes@odinms.de>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License version 3
 * as published by the Free Software Foundation. You may not use, modify
 * or distribute this program under any other version of the
 * GNU Affero General Public License.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * Casey - Master of MiniGame
 * by Flav
 */

var select;

function start() {
	status = -1;
	action(1, 0, 0);
}

function action(mode, type, selection) {
	if (mode < 1) {
		cm.dispose();
		return;
	} else if (mode == 1) {
		status++;
	} else {
		status--;
	}

	if (status == 0) {
		cm.sendSimple("Hey you look like you need a breather from all that hunting. You should be enjoying the life, just like I am. Well, if you have a couple of itmes, I can make a trade with you for an item you can play minigames with. Now... what can I do for you?\r\n\r\n#b#L0#Create a minigame item#l#k\r\n#b#L1#Explain to me what minigames are about#l#k");
	} else if (status == 1) {
		if (selection == 0) {
			cm.sendSimple("You want to make the minigame item? Minigames aren't something you can just go ahead and play right off the bat. You'll need a specific set of items for a specific minigame. Which minigame item do you want to make?\r\n\r\n#b#L2#Omok Set#l#k\r\n#b#L3#A Set of Match Cards#l#k");
		} else if (selection == 1) {
			cm.sendSimple("You want to learn more about the minigames? Awesome! Ask me anything. Which minigame do you want to know more about?\r\n\r\n#b#L4#Omok#l#k\r\n#b#L5#Match Cards#l#k");
		}
	} else if (status == 2) {
		if (selection != null)
			select = selection;

		if (select == 2) {
			cm.sendNext("You want to play #bOmok#k, huh? To play it, you'll need the Omok Set. Only the ones with that item can open the room for a game of Omok, and you can play this game almost everywhere except for a few places at the market place.");
		} else if (select == 3) {
			if (!cm.haveItem(4030012, 15)) {
				cm.sendNext("You want #bA set of Match Cards#k? Hmmm... to make A set of Match Cards, you'll need some #bMonster Cards#k. Monster Card can be obtained by taking out the monsters all around the island. Collect 15 Monster Cards and you can make a set of A set of Match Cards.");
			} else {
				cm.gainItem(4030012, -15);
				cm.gainItem(4080100 , 1);
			}
		} else if (select == 4) {
			cm.sendNext("Here are the rules to the game of Omok. Listen carefully. Omok is a game where, you and your opponent will take turns laying a piece on the table until someone finds a way to lay 5 consecutive pieces in a line, be it horizontal, diagonal or vertical. That person will be the winner. For starters, only the ones with #bOmok Set#k can open a game room.");
		} else if (select == 5) {
			cm.sendNext("Here are the rules to the game of Match Cards. Listen carefully. Match Cards is just like the way it sounds, finding a matching pair among the number of cards laid on the table. When all the matching pairs are found, then the person with more matching pairs will win the game. Just like Omok, you'll need #bA set of Match Cards#k to open the game room.");
		}
	} else if (status == 3) {
		if (select == 2) {
			cm.sendSimple("The set also differs based on what kind of pieces you want to use for the game. Which set would you like to make?\r\n#b#L6#Slime & Mushroom Omok Set#l#k\r\n#b#L7#Slime & Octopus Omok Set#l#k\r\n#b#L8#Slime & Pig Omok Set#l#k\r\n#b#L9#Octopus & Mushroom Omok Set#l#k\r\n#b#L10#Pig & Octopus Omok Set#l#k\r\n#b#L11#Pig & Mushroom Omok Set#l#k");
		} else if (select == 4) {
			cm.sendNextPrev("Every game of Omok will cost you #r100 mesos#k. Even if you don't have #bOmok Set#k, you can enter the game room and play the game. If you don't have 100 mesos, however, then you won't be allowed in the room, period. The person opening the game room also needs 100 mesos to open the room, or there's no game. If you run out of mesos during the game, then you're automatically kicked out of the room!");
		} else if (select == 5) {
			cm.sendNextPrev("Every game of Match Cards will cost you #r100 mesos#k. Even if you don't have #bA set of Match Cards#k, you can enter the game room and play the game. If you don't have 100 mesos, however, then you won't be allowed in the room, period. The person opening the game room also needs 100 mesos to open the room, or there's no game. If you run out of mesos during the game, then you're automatically kicked out of the room!");
		}
	} else if (status == 4) {
		if (selection == 6) {
			if (!cm.haveItem(4030000, 99) && !cm.haveItem(4030001, 99) && !cm.haveItem(4030009, 1)) {
				cm.sendNext("#bYou want to make Slime & Mushroom Omok Set#k? Hmm ... get me the materials, and I can do just that. Listen carefully, the materials you need will be:   #r99 Omok Piece : Slime, 99 Omok Piece : Mushroom, 1 Omok Table#k. The monsters will porbably drop those every once in a while.");
			} else {
				cm.gainItem(4030000, -99);
				cm.gainItem(4030001, -99);
				cm.gainItem(4030009, -1);
				cm.gainItem(4080000, 1);
			}
		} else if (selection == 7) {
			if (!cm.haveItem(4030000, 99) && !cm.haveItem(4030010, 99) && !cm.haveItem(4030009, 1)) {
				cm.sendNext("#bYou want to make Slime & Octopus Omok Set#k? Hmm ... get me the materials, and I can do just that. Listen carefully, the materials you need will be:   #r99 Omok Piece : Slime, 99 Omok Piece : Octopus, 1 Omok Table#k. The monsters will porbably drop those every once in a while.");
			} else {
				cm.gainItem(4030000, -99);
				cm.gainItem(4030010, -99);
				cm.gainItem(4030009, -1);
				cm.gainItem(4080001, 1);
			}
		} else if (selection == 8) {
			if (!cm.haveItem(4030000, 99) && !cm.haveItem(4030011, 99) && !cm.haveItem(4030009, 1)) {
				cm.sendNext("#bYou want to make Slime & Pig Omok Set#k? Hmm ... get me the materials, and I can do just that. Listen carefully, the materials you need will be:   #r99 Omok Piece : Slime, 99 Omok Piece : Pig, 1 Omok Table#k. The monsters will porbably drop those every once in a while.");
			} else {
				cm.gainItem(4030000, -99);
				cm.gainItem(4030011, -99);
				cm.gainItem(4030009, -1);
				cm.gainItem(4080002, 1);
			}
		} else if (selection == 9) {
			if (!cm.haveItem(4030010, 99) && !cm.haveItem(4030001, 99) && !cm.haveItem(4030009, 1)) {
				cm.sendNext("#bYou want to make Octopus & Mushroom Omok Set#k? Hmm ... get me the materials, and I can do just that. Listen carefully, the materials you need will be:   #r99 Omok Piece : Octopus, 99 Omok Piece : Mushroom, 1 Omok Table#k. The monsters will porbably drop those every once in a while.");
			} else {
				cm.gainItem(4030010, -99);
				cm.gainItem(4030001, -99);
				cm.gainItem(4030009, -1);
				cm.gainItem(4080003, 1);
			}
		} else if (selection == 10) {
			if (!cm.haveItem(4030011, 99) && !cm.haveItem(4030010, 99) && !cm.haveItem(4030009, 1)) {
				cm.sendNext("#bYou want to make Pig & Octopus Omok Set#k? Hmm ... get me the materials, and I can do just that. Listen carefully, the materials you need will be:   #r99 Omok Piece : Pig, 99 Omok Piece : Octopus, 1 Omok Table#k. The monsters will porbably drop those every once in a while.");
			} else {
				cm.gainItem(4030011, -99);
				cm.gainItem(4030010, -99);
				cm.gainItem(4030009, -1);
				cm.gainItem(4030009, 1);
			}
		} else if (selection == 11) {
			if (!cm.haveItem(4030011, 99) && !cm.haveItem(4030001, 99) && !cm.haveItem(4030009, 1)) {
				cm.sendNext("#bYou want to make Pig & Mushroom Omok Set#k? Hmm ... get me the materials, and I can do just that. Listen carefully, the materials you need will be:   #r99 Omok Piece : Pig, 99 Omok Piece : Mushroom, 1 Omok Table#k. The monsters will porbably drop those every once in a while.");
			} else {
				cm.gainItem(4030011, -99);
				cm.gainItem(4030001, -99);
				cm.gainItem(4030009, -1);
				cm.gainItem(4080005, 1);
			}
		} else if (select == 4 || 5) {
			cm.sendNextPrev("Enter the room, and when you're ready to play, click on #bReady#k. Once the visitor clicks on #bReady#k, the owner of the room can press #bStart#k to start the game. If an unwanted visitor walks in, and you don't want to play with that person, the owner of the room has the right to kick the visitor out of the room. There will be a square box with x written on the right of that person. Click on that for a cold goodbye, ok?");
		}
	} else if (status == 5) {
		if (select == 4) {
			cm.sendNextPrev("When the first game starts, #bthe owner of the room goes first#k. Be ware that you'll be given a time limit, and you may lose your turn if you don't make your move on time. Normally, 3 x 3 is not allowed, but if there comes a point that it's absolutely necessary to put your piece there or face a game over, then you can put it there. 3 x 3 is allowed as the last line of defense! Oh, and it won't count if it's #r6 or 7 straigt#k. Only 5!");
		} else if (select == 5) {
			cm.sendNextPrev("Oh, and unlike Omok, on Match Cards, when you create the game room, you'll need to set your game on the number of cards you'll use for the game. There are 3 modes available, 3x4, 4x5, and 5x6, which will require 12, 20, and 30 cards. Remember, though, you won't be able to change it up once the room is open, so if you really wish to change it up, you may have to close the room and open another one.");
		}
	} else if (status == 6) {
		if (select == 4) {
			cm.sendNextPrev("If you know your back is against the wall, you can request a #bRedo#k. If the opponent accepts your request, then the opponent's last move, along with yours, will be canceled out. If you ever feel the need to go to the bathroom, or take an extended break, you can request a #btie#k. The game will end in a tie if the opponent accepts the request. This may be a good way to keep your friendship in tact with your buddy.");
		} else if (select == 5) {
			cm.sendNextPrev("When the first game starts, #bthe owner of the room goes first#k. Be ware that you'll be given a time limit, and you may lose your turn if you don't make your move on time. When you find a matching pair on your turn, you'll get to keep your turn, as long as you keep finding a pair of matching cards. Use your memorizing skills for a devastating combo of turns.");
		}
	} else if (status == 7) {
		if (select == 4) {
			cm.sendNextPrev("Once the game is over, and the next game starts, the loser will go first. Oh, and you can't leave in the middle of the game. If you do, you may need to request either a #bforfeit, or a tie#k. Of course, if you request a forfeit, you'll lose the game, so be careful of that. And if you click on \"Leave\" in the middle of the game and call to leave after the game, you'll leave the room right after the game is over, so this will be a much more useful way to leave.");
		} else if (select == 5) {
			cm.sendNextPrev("If you and your opponent have the same number of matched pairs, then whoever had a longer streak of matched pairs will win. If you ever feel the need to go to the bathroom, or take an extended break, you can request a #btie#k. The game will end in a tie if the opponent accepts the request. This may be a good way to keep your friendship in tact with your buddy.");
		}
	} else if (status == 8) {
		if (select == 5) {
			cm.sendPrev("Once the game is over, and the next game starts, the loser will go fisrt. Oh, and you can't leave in the middle of the game. If you do, you may need to request either a #bforfeit, or a tie#k. Of course, if you request a forfeit, you'll lose the game, so be careful of that. And if you click on \"Leave\" in the middle of the game and call to leave after the game, you'll leave the room right after the game is over, so this will be a much more useful way to leave.");
		}
	}
}