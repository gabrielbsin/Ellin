/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server.life.components;

public class BanishInfo {
    
    private int type = 0;
    private final int map;
    private final String portal, msg;

    public BanishInfo(String msg, int map, String portal) {
        this.msg = msg;
        this.map = map;
        this.portal = portal;
    }

    public BanishInfo(int type, String msg, int map, String portal) {
        this.type = type;
        this.msg = msg;
        this.map = map;
        this.portal = portal;
    }

    public int getType() {
        return type;
    }

    public int getMap() {
        return map;
    }

    public String getPortal() {
        return portal;
    }

    public String getMsg() {
        return msg;
    }
}
