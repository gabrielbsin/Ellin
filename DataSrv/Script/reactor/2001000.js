/*@author Jvlaple
 *Nependath Pot - Spawns Nependath or Dark Nependath
 */
 
function act() {
    if(rm.getMap().getSummonState()) {
        var count = Number(rm.getEventInstance().getIntProperty("statusStg7_c"));

        if(count < 7) {
            var nextCount = (count + 1);

            rm.spawnMonster(Math.random() >= .6 ? 9300049 : 9300048);
            rm.getEventInstance().setProperty("statusStg7_c", nextCount);
        }
        else {
            rm.spawnMonster(9300049);
        }
    }
}