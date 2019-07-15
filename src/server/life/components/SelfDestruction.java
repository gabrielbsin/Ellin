package server.life.components;

public class SelfDestruction {
    
    private final byte action;
    private final int removeAfter;
    private final int hp;

    public SelfDestruction(byte action, int removeAfter, int hp) {
        this.action = action;
        this.removeAfter = removeAfter;
        this.hp = hp;
    }

    public int getHp() {
        return hp;
    }

    public byte getAction() {
        return action;
    }

    public int removeAfter() {
        return removeAfter;
    }
}
