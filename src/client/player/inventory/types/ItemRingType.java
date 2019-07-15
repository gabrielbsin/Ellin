/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package client.player.inventory.types;

/**
 *
 * @author GabrielSin
 */
public enum ItemRingType {
    
    CRUSH_RING(0),
    FRIENDSHIP_RING(1),
    WEDDING_RING(2);

    int type = -1;

    private ItemRingType (int type) {
        this.type = type;
    } 

    public int getType() {
        return this.type;
    }
}
