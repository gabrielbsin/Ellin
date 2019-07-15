/**
 Donator shop
 @author Jvlaple
*/

var status = 0;

var items = Array(
	2022179, //Onyx Apple
	2022282, //Demon elixir
	1082223, //Stormcaster gloves
	1072344 //Facestomper
)

var quantities = Array(
	5, //Onyx apple
	5, //Demon elixir
	1,
	1
)

var costs = Array(
	1, //Onyx apple
	1, //Demon elixir
	5,
	5
)
	

function start() {
	status = -1;
	action(1, 0, 0);
}

function action(mode, type, selection) {
	if (mode == -1) {
		cm.dispose();
	} else {
		if (mode == 0 && status == 0) {
			cm.dispose();
			return;
		}
		if (mode == 1)
			status++;
		else
			status--;
		if (status == 0) {
			cm.sendNext("Hello, and welcome to the donator shop! Here, you can spend your donator points on whatever you like!\r\n\r\nYou currently have #r" + 
							cm.getPlayer().getDonatorPoints() + " #kdonator points.");
		} else if (status == 1 ) {
			var shop = "Pick an item to buy!\r\n";
			for (var i = 0; i < items.length; i++) {
				shop += "#L" + i + "##b#t" + items[i] + "# #rQuantity: " + quantities[i] + " #gCost: " + costs[i] + "\r\n";
			}
			cm.sendSimple(shop);
		} else if (status == 2) {
			if (cm.getPlayer().getDonatorPoints() >= costs[selection]) {
				cm.gainItem(items[selection], quantities[selection]);
				cm.getPlayer().gainDonatorPoints(-costs[selection]);
				cm.sendOk("Thanks for buying! Come back soon!");
				cm.dispose();
			} else {
				cm.sendOk("You don't have enough points!");
				cm.dispose();
			}
		}
	}
}