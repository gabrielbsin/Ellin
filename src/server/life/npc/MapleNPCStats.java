package server.life.npc;

import java.util.ArrayList;
import java.util.List;

public class MapleNPCStats {
    
    private String name;
    private int trunkPut, trunkGet;
    private boolean custom = false;
    private List<Integer> maps = new ArrayList<>();
    
    public MapleNPCStats() {}

    public MapleNPCStats(String name) {
        this.name = name;
    }
    
    public String getName() {
	return name;
    }

    public void setName(String name) {
	this.name = name;
    }
    
    public int getDepositCost() {
	return trunkPut;
    }

    public void setDepositCost(int trunkPut) {
        this.trunkPut = trunkPut;
    }
    
    public int getWithdrawCost() {
	return trunkGet;
    }

    public void setWithdrawCost(int trunkGet) {
        this.trunkGet = trunkGet;
    }
    
    public boolean addMap(Integer e) {
	return maps.add(e);
    }
    
    public List<Integer> getMaps() {
	  return maps;
    }

    public void setMaps(List<Integer> maps) {
	  this.maps = maps;
    }

    public boolean isCustom() {
        return custom;
    }

    public void setCustom(boolean custom) {
        this.custom = custom;
    }
}
