/*Pirate PQ Reactor - Spawns 6 of each Pirate when opened.
 *@author Jvlaple
 *2511001.js
 */

function act() {
    for (var i = 0; i < 6; i++) {
        rm.spawnMonster(9300124);
        rm.spawnMonster(9300125);
    }
}