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

public enum SavedLocationType {
    
    FREE_MARKET,
    WORLDTOUR, 
    FLORINA,
    MONSTER_CARNIVAL, 
    ARIANT_PQ,
    EVENT;
    
    public static SavedLocationType fromString(String Str) {
        return valueOf(Str);
    }    
}