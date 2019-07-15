/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server.life;

public class MonsterDropEntry {
    
    public MonsterDropEntry(int itemId, int chance, int Minimum, int Maximum, short questid, int MonsterId) {
        this.itemId = itemId;
        this.chance = chance;
        this.questid = questid;
        this.Minimum = Minimum;
        this.Maximum = Maximum;
        this.MonsterId = MonsterId;
    }
    
    public short questid;
    public int itemId, chance, Minimum, Maximum, MonsterId;
    
}
