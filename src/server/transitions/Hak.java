/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package server.transitions;

import server.maps.Field;

/**
 * @author JavaScriptz
 * Barcos
 * Removido Boats.js
 */
public class Hak {

public int returnTo[] = {200000141, 250000100};
public int rideTo[] = {250000100, 200000141};
public int birdRide[] = {200090300, 200090310};
public Field myRide, returnMap, map, docked, onRide;
public long timeOnRide = 60; 

        public Hak () {
           //Sem animação para terminar :(
      }
}  