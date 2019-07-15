package client.player.inventory;

public class ItemPetCommand {
   
    private final int petId;
    private final int skillId;
    private final int prob;
    private final int inc;

    public ItemPetCommand(int petId, int skillId, int prob, int inc) {
        this.petId = petId;
        this.skillId = skillId;
        this.prob = prob;
        this.inc = inc;
    }

    public int getPetId() {
        return petId;
    }

    public int getSkillId() {
        return skillId;
    }

    public int getProbability() {
        return prob;
    }

    public int getIncrease() {
        return inc;
    }
}

