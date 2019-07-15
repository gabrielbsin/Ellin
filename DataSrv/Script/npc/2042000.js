var MonsterCarnival = Packages.server.partyquest.mcpq.MonsterCarnival;
var MCTracker = Packages.server.partyquest.mcpq.MCTracker;
var MCParty = Packages.server.partyquest.mcpq.MCParty;
var MCField = Packages.server.partyquest.mcpq.MCField;
var MCTeam = Packages.server.partyquest.mcpq.MCField.MCTeam;

var status = -1;
var carnival, field, opposition;
var room = -1;
var selected = -1;

function start() {
    if (cm.getMapId() != 980000000) {
        MCTracker.log("Spiegelmann called on invalid map " + cm.getMapId() + " by player " + cm.getName());
        cm.sendOk("You are not authorized to do this.");
        cm.dispose();
        return;
    }
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode == -1) {
        cm.dispose();
        return;
    }
    if (mode == 1) status++;
    else status--;

    if (status == 0) {
        if (cm.getParty() == null) {
            cm.sendOk("You are not affiliated with any party here. You can only take on this challenge if you are in a party.");
            cm.dispose();
            return;
        } else if (!cm.isLeader()) {
            cm.sendOk("If you want to try Carnival PQ, please tell the #bleader of your party#k to talk to me.");
            cm.dispose();
            return;
        }
        carnival = MonsterCarnival.getMonsterCarnival(cm.getPlayer().getClient().getChannel());
        cm.sendSimple(carnival.getNPCAvailableFields());
    } else if (status == 1) {
        room = selection;
        if (room < 1 || room > 7) {
            cm.sendOk("That is not a valid room.");
            cm.dispose();
            return;
        }
        if (room == 7) {
            cm.sendOk("You wish to know about the Monster Carnival? Very well. The Monster Carnival is a place of trilling battles and exciting competiton against people just as strong and motivated as yourself. You must summon monsters and defeat the monsters summoned by the opposing party. That's the essence of the Monster Carnival. Once you enter the Carnival Field, the task is to earn CP by hunter monsters from the opposing party and use those CP's to distract the opposing party from hunting monsters. There are three ways to distract the other party; Summon a Monster, Skill or Protector. Please remember this though, it's never a good idea to save up CP just for the sake of it. The CP's you've used will also help determine the winner and the loser of the carnival.");
            cm.dispose();
            return;
        }
        if (cm.getPlayer().getClient().getChannelServer().getMapFactory().getMap(980000000 + room * 100).playerCount() > 0) {
            field = carnival.getField(room);
            var leaderName = field.getRed().getLeaderName();
            var p = cm.getPlayer().getClient().getChannelServer().getPlayerStorage().getCharacterByName(leaderName);
            if (p != null) {
                cm.sendAcceptDecline(carnival.getChallengers(p));
            }
        } else {
            cm.sendYesNo("Do you want to create a #bCarnival Field#k? Once the Carnival Field is open, you may accept invitations from other parties for #b3 minutes#k. Once you accept an invitation, the opposing party will automatically enter the Carnival Field.");
        }
        selected = selection;
    } else if (status == 2) {
        var code = carnival.registerStatus(cm.getParty(), selected);
        if (code == MonsterCarnival.STATUS_FIELD_FULL) {
            cm.sendOk("This room is currently full.")
        } else if (code == MonsterCarnival.STATUS_PARTY_SIZE) {
            cm.sendOk("Your size of the party does not meet the requirements for this quest.");
        } else if (code == MonsterCarnival.STATUS_PARTY_LEVEL) {
            cm.sendOk("One of the members in your party may not be Level 30~50. Please alter your party to meet the requirements.");
        } else if (code == MonsterCarnival.STATUS_PARTY_MISSING) {
            cm.sendOk("Please make sure everyone in your party is in this lobby.");
        } else if (code == MonsterCarnival.STATUS_FIELD_INVALID) {
            cm.sendOk("Unauthorized request.");
        }

        if (code == MonsterCarnival.STATUS_PROCEED) {
            field = carnival.getField(room);
            party = carnival.createParty(cm.getParty());
            field.register(party, MCTeam.RED);
            if (cm.getPlayer().isChallenged()) {
                cm.getPlayer().setChallenged(false);
            }
            cm.getPlayer().dropMessage("msg: " + field.arena);
            cm.getPlayer().dropMessage(6, "You will receive invitations from other parties for the next 3 minutes.");
        } else if (code == MonsterCarnival.STATUS_REQUEST) {
            field = carnival.getField(room);
            party = carnival.createParty(cm.getParty());
           // field.request(party, field);
            cm.challengeParty(party, 0);
        }
       // cm.dispose();
    }
}  