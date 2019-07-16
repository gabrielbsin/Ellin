package client.player;

public enum PlayerEffects {
    
    LEVEL_UP(0x0),
    SKILL_USE(0x01),
    SKILL_AFFECTED(0x02),
    QUEST(0x03),
    PET(0x04),
    SKILL_SPECIAL(0x05),
    SAFETY_CHARM(0x06),
    PLAY_PORTAL_SE(0x07),
    JOB_ADVANCEMENT(0x08),
    QUEST_COMPLETE(0x09),
    INC_DEC_HP_EFFECT(0x0A),
    BUFF_ITEM_EFFECT(0x0B),
    MONSTERBOOK_CARD_GET(0x0D),
    ITEM_LEVEL_UP(0x0F);
    
    private final int effect;
	
    private PlayerEffects(int effect) {
        this.effect = effect;
    }

    public int getEffect() {
        return effect;
    }  
}
