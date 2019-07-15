/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package server.maps;

/**
 * 
 * @author GabrielSin
 */
public enum SummonMovementType {
        
    STATIONARY(0),
    FOLLOW(1),
    CIRCLE_FOLLOW(3);

    private final int val;

    private SummonMovementType(int val) {
        this.val = val;
    }

    public int getValue() {
        return val;
    }
}