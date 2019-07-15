/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package server.life;

import java.awt.Point;

public abstract class Spawns {
    
    public abstract int getMonsterId();
    
    public abstract int getTeam();

    public abstract boolean shouldSpawn(long time);
    
    public abstract boolean shouldSpawn();
   
    public abstract MapleMonster getMonster();

    public abstract int getMobTime();

    public abstract Point getPosition();

    public abstract int getF();

    public abstract int getFh();
    
    public abstract boolean isTemporary();
    
    public abstract void setTemporary(boolean setTemporary);

    public abstract boolean shouldForceSpawn();
    
    public abstract void setDenySpawn(boolean val);
    
    public abstract boolean getDenySpawn();
    
}
