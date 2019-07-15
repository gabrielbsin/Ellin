/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package handling.channel.handler;

import static handling.channel.handler.ChannelHeaders.MovementHeaders.*;
import handling.mina.PacketReader;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import server.movement.AbsoluteLifeMovement;
import server.movement.ChairMovement;
import server.movement.ChangeEquipSpecialAwesome;
import server.movement.JumpDownMovement;
import server.movement.LifeMovement;
import server.movement.LifeMovementFragment;
import server.movement.RelativeLifeMovement;
import server.movement.TeleportMovement;
import server.maps.object.AnimatedMapleFieldObject;

/**
 *
 * @author GabrielSin
 */
public class MovementParse {
  
    public static final List<LifeMovementFragment> parseMovement(final PacketReader packet) {
	final List<LifeMovementFragment> res = new ArrayList<>();
        int numCommands = packet.readByte();
        for (int i = 0; i < numCommands; i++) {
            int command = packet.readByte();
            switch (command) {
                case NORMAL_MOVE: 
                case NORMAL_MOVE_2:
                case WINGS_FALL: {
                    Point pos = packet.readPos();
                    Point wobble = packet.readPos();
                    final int unk = packet.readShort();
                    final int newstate = packet.readByte();
                    final int duration = packet.readShort();
                    AbsoluteLifeMovement alm = new AbsoluteLifeMovement(command, pos, duration, newstate);
                    alm.setUnk(unk);
                    alm.setPixelsPerSecond(wobble);
                    res.add(alm);
                    break;
                }
                case JUMP_M:
                case JUMP_AND_KNOCKBACK:
                case FLASH_JUMP:
                case HORNTAIL_KNOCKBACK:
                case RECOIL_SHOT:
                case WINGS: {
                    Point pos = packet.readPos();
                    final int newstate = packet.readByte();
                    final int duration = packet.readShort();
                    RelativeLifeMovement rlm = new RelativeLifeMovement(command, pos, duration, newstate);
                    res.add(rlm);
                    break;
                }
                case UNK_SKILL:
                case TELEPORT:
                case ASSAULTER: 
                case ASSASSINATE: 
                case RUSH_M: 
                case UNK:{
                    Point pos = packet.readPos();
                    final int xwobble = packet.readShort();
                    final int ywobble = packet.readShort();
                    int newstate = packet.readByte();
                    TeleportMovement tm = new TeleportMovement(command, pos, newstate);
                    tm.setPixelsPerSecond(new Point(xwobble, ywobble));
                    res.add(tm);
                    break;
                } 
                case EQUIP_M: {
                    res.add(new ChangeEquipSpecialAwesome(packet.readByte()));
                    break;
                }
                case CHAIR: {
                    Point pos = packet.readPos();
                    final int unk = packet.readShort();
                    final int newstate = packet.readByte();
                    final int duration = packet.readShort();
                    ChairMovement cm = new ChairMovement(command, pos, duration, newstate);
                    cm.setUnk(unk);
                    res.add(cm);
                    break;
                }
                case JUMP_DOWN: {                 
                    Point pos = packet.readPos();
                    Point wobble = packet.readPos();
                    final int unk = packet.readShort();
                    final int fh = packet.readShort();
                    final int newstate = packet.readByte();
                    final int duration = packet.readShort();
                    JumpDownMovement jdm = new JumpDownMovement(command, pos, duration, newstate);
                    jdm.setUnk(unk);
                    jdm.setPixelsPerSecond(wobble);
                    jdm.setFH(fh);
                    res.add(jdm);
                    break;
                }
                default: {
                    return null;
                }
            }
        }
        if (numCommands != res.size()) {
            return null; 
        }
      return res;
    }

    public static void updatePosition(final List<LifeMovementFragment> movement, final AnimatedMapleFieldObject target, final int yoffset) {
        if (movement == null) {
            return;
        }
        for (final LifeMovementFragment move : movement) {
            if (move instanceof LifeMovement) {
                if (move instanceof AbsoluteLifeMovement) {
                    final Point position = ((LifeMovement) move).getPosition();
                    position.y += yoffset;
                    target.setPosition(position);
                }
                target.setStance(((LifeMovement) move).getNewstate());
            }
        }
    }
}
