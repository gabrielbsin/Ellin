/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server.maps.reactors;

public class ReactorDropEntry {
    
    public ReactorDropEntry(int itemId, int chance, int questid) {
        this.itemId = itemId;
        this.chance = chance;
        this.questid = questid;
    }
    public int itemId, chance, questid;
    public int assignedRangeStart, assignedRangeLength;
}
