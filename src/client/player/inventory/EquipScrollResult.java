/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package client.player.inventory;

/**
 * 
 * @author GabrielSin
 */
public enum EquipScrollResult {
    
    FAIL(0),
    SUCCESS(1),
    CURSE(2);
    
    private int value = -1;

    private EquipScrollResult(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
