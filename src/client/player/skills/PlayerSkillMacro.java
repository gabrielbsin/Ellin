package client.player.skills;

public class PlayerSkillMacro {

    private final String name;
    private final boolean silent;
    private final int skill1, skill2, skill3;

    public PlayerSkillMacro(String name, boolean silent, int skill1, int skill2, int skill3) {
        this.skill1 = skill1;
        this.skill2 = skill2;
        this.skill3 = skill3;
        this.name = name;
        this.silent = silent;
    }

    public int getFirstSkill() {
        return skill1;
    }

    public int getSecondSkill() {
        return skill2;
    }

    public int getThirdSkill() {
        return skill3;
    }

    public String getName() {
        return name;
    }
    
    public boolean isSilent() {
        return silent;
    }
}