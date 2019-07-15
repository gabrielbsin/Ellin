package server.life.components;

public class LoseItem {
    
    private final int id;
    private final byte chance;
    private final byte x;

    public LoseItem(int id, byte chance, byte x) {
        this.id = id;
        this.chance = chance;
        this.x = x;
    }

    public int getId() {
        return id;
    }

    public byte getChance() {
        return chance;
    }

    public byte getX() {
        return x;
    } 
}
