package client.player.inventory;

import client.player.inventory.types.ItemType;
import client.player.PlayerJob;

public class Equip extends Item {

    private byte upgradeSlots;
    private byte level;
    private byte locked;
    private short str;
    private short dex;
    private short _int;
    private short luk;
    private short hp;
    private short mp;
    private short watk;
    private short matk;
    private short wdef;
    private short mdef;
    private short acc;
    private short avoid;
    private short hands;
    private short speed;
    private short jump;
    private long expiration;
    private boolean wear = false;
    private PlayerJob job;

    public Equip(int id, short position) {
        super(id, position, (short) 1);
    }

    public Equip(int id, short position, int uniqueid) {
        super(id, position, (short) 1, uniqueid);  
    }

    @Override
    public Item copy() {
        Equip ret = new Equip(getItemId(), getPosition(), getUniqueId());
        ret.str = str;
        ret.dex = dex;
        ret._int = _int;
        ret.luk = luk;
        ret.hp = hp;
        ret.mp = mp;
        ret.matk = matk;
        ret.mdef = mdef;
        ret.watk = watk;
        ret.wdef = wdef;
        ret.acc = acc;
        ret.avoid = avoid;
        ret.hands = hands;
        ret.speed = speed;
        ret.jump = jump;
        ret.locked = locked;
        ret.upgradeSlots = upgradeSlots;
        ret.level = level;
        ret.expiration = expiration;
        ret.setOwner(getOwner());
        ret.setQuantity(getQuantity());
        ret.setGiftFrom(getGiftFrom());
        ret.setDisappearsAtLogout(disappearsAtLogout());
        return ret;
    }

    @Override
    public byte getType() {
        return ItemType.EQUIP;
    }

    public byte getUpgradeSlots() {
        return upgradeSlots;
    }

    public byte getLocked() {
        return locked;
    }
    
    @Override
    public long getExpiration() {
        return expiration;
    }

    public short getStr() {
        return str;
    }

    public short getDex() {
        return dex;
    }

    public short getInt() {
        return _int;
    }

    public short getLuk() {
        return luk;
    }

    public short getHp() {
        return hp;
    }

    public short getMp() {
        return mp;
    }

    public short getWatk() {
        return watk;
    }

    public short getMatk() {
        return matk;
    }

    public short getWdef() {
        return wdef;
    }

    public short getMdef() {
        return mdef;
    }

    public short getAcc() {
        return acc;
    }

    public short getAvoid() {
        return avoid;
    }

    public short getHands() {
        return hands;
    }

    public short getSpeed() {
        return speed;
    }

    public short getJump() {
        return jump;
    }

    public boolean isWearing() {
        return wear;
    }

    public void wear(boolean yes) {
        wear = yes;
    }
    
    @Override
    public void setExpiration(long expiration) {
        this.expiration = expiration;
    }

    public byte getLevel() {
        return level;
    }
    
    @Override
    public String getGiftFrom() {
        return giftFrom;
    }

    @Override
    public void setGiftFrom(String giftFrom) {
        this.giftFrom = giftFrom;
    }

    public PlayerJob getJob() {
        return job;
    }

    public void setStr(short str) {
        this.str = str;
    }

    public void setDex(short dex) {
        this.dex = dex;
    }

    public void setInt(short _int) {
        this._int = _int;
    }

    public void setLuk(short luk) {
        this.luk = luk;
    }

    public void setHp(short hp) {
        this.hp = hp;
    }

    public void setMp(short mp) {
        this.mp = mp;
    }

    public void setWatk(short watk) {
        this.watk = watk;
    }

    public void setMatk(short matk) {
        this.matk = matk;
    }

    public void setWdef(short wdef) {
        this.wdef = wdef;
    }

    public void setMdef(short mdef) {
        this.mdef = mdef;
    }

    public void setAcc(short acc) {
        this.acc = acc;
    }

    public void setAvoid(short avoid) {
        this.avoid = avoid;
    }

    public void setHands(short hands) {
        this.hands = hands;
    }

    public void setSpeed(short speed) {
        this.speed = speed;
    }

    public void setJump(short jump) {
        this.jump = jump;
    }

    public void setLocked(byte locked) {
        this.locked = locked;
    }

    public void setUpgradeSlots(byte upgradeSlots) {
        this.upgradeSlots = upgradeSlots;
    }

    public void setLevel(byte level) {
        this.level = level;
    }
    
    public void setJob(PlayerJob job) {
        this.job = job;
    }
    
    public void gainStrPoints(int gain){
       this.str += gain;
    }
    
    public void gainDexPoints(int gain){
       this.dex += gain;
    }
    
    public void gainIntPoints(int gain){
       this._int += gain;
    }
    
    public void gainLukPoints(int gain){
       this.luk += gain;
    }
    
    public void gainMatkPoints(int gain){
       this.matk += gain;
    }
    
    public void gainWatkPoints(int gain){
       this.watk += gain;
    }
    
    public void gainAccPoints(int gain){
       this.acc += gain;
    }
    
    public void gainAvoidPoints(int gain){
       this.avoid += gain;
    }
    
    public void gainJumpPoints(int gain){
       this.jump += gain;
    }
     
    public void gainSpeedPoints(int gain){
       this.speed += gain;
    }
     
    public void gainWdefPoints(int gain){
       this.wdef += gain;
    }
     
    public void gainMdefPoints(int gain){
       this.mdef += gain;
    }
     
    public void gainHpPoints(int gain){
       this.hp += gain;
    }
         
    public void gainMpPoints(int gain){
       this.mp += gain;
    } 

    @Override
    public void setQuantity(short quantity) {
        if (quantity < 0 || quantity > 1) {
            throw new RuntimeException("Setting the quantity to " + quantity + " on an equip (itemid: " + getItemId() + ")");
        }
        super.setQuantity(quantity);
    }
}