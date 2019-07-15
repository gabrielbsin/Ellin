function start(mode, type, selection) {
    qm.playerMessage(5, "You have earned the title<Awakened Aran>");
    qm.EarnTitleMsg("You have earned the title<Awakened Aran>");
    qm.forceCompleteQuest();
    qm.dispose();
}


function end(mode, type, selection) {
    qm.dispose();
}