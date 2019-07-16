package handling;

import packet.transfer.write.OutPacket;
import client.Client;
import constants.ServerProperties;
import cashshop.CashCouponRequest;
import cashshop.CashShopOperation;
import constants.GameConstants;
import java.io.IOException;
import handling.channel.ChannelServer;
import handling.channel.handler.MTSHandler;
import handling.channel.handler.MonsterCarnivalHandler;
import handling.channel.handler.*;
import handling.login.LoginServer;
import handling.login.handler.*;
import handling.mina.PacketReader;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import packet.creators.LoginPackets;
import packet.creators.PacketCreator;
import packet.crypto.MapleCrypto;
import packet.opcode.RecvPacketOpcode;
import tools.FileLogger;
import tools.HexTool;
import tools.Randomizer;

public class ServerHandler extends org.apache.mina.core.service.IoHandlerAdapter {
    
    private int channel = -1;
    private static AtomicLong sessionId = new AtomicLong(7777);

    public ServerHandler(final int channel) {
        this.channel = channel;
    }

    @Override
    public void messageSent(IoSession session, Object message) throws Exception {
        Runnable r = ((OutPacket) message).getOnSend();
        if (r != null)
            r.run();
        super.messageSent(session, message);
    }

    @Override
    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
        if (cause instanceof IOException || cause instanceof ClassCastException) {
            return;
        }
        Client mc = (Client) session.getAttribute(Client.CLIENT_KEY);
        if (mc != null && mc.getPlayer() != null) {
            FileLogger.printError(FileLogger.EXCEPTION_CAUGHT, cause, "Exception caused by: " + mc.getPlayer());
        }
    }

    @Override
    public void sessionOpened(IoSession session) throws Exception {
       if (channel > -1) {
	    if (ChannelServer.getInstance(channel).isShutdown() || ChannelServer.getInstance(channel) == null) {
		session.close();
		return;
	    }
	} else {
            if (LoginServer.isShutdown()) {
                session.close();
                return;
            }
        }
       
        final byte serverRecv[] = new byte[]{70, 114, 122, (byte) Randomizer.nextInt(255)};
        final byte serverSend[] = new byte[]{82, 48, 120, (byte) Randomizer.nextInt(255)};
        final byte ivRecv[] = serverRecv;
        final byte ivSend[] = serverSend;
        byte key[] = {0x13, 0x00, 0x00, 0x00, 0x08, 0x00, 0x00, 0x00, 0x06, 0x00, 0x00, 0x00, (byte) 0xB4, 0x00, 0x00, 0x00, 0x1B, 0x00, 0x00, 0x00, 0x0F, 0x00, 0x00, 0x00, 0x33, 0x00, 0x00, 0x00, 0x52, 0x00, 0x00, 0x00};
      
        MapleCrypto sendCypher = new MapleCrypto(key, ivSend, (short) (0xFFFF - ServerProperties.World.MAPLE_VERSION));
        MapleCrypto recvCypher = new MapleCrypto(key, ivRecv, ServerProperties.World.MAPLE_VERSION);
        Client client = new Client(sendCypher, recvCypher, session);
        
        client.setChannel(channel);
        client.setSessionId(sessionId.getAndIncrement()); 
        session.write(LoginPackets.GetHello(ServerProperties.World.MAPLE_VERSION, ivSend, ivRecv, false));
        
        
        session.setAttribute(Client.CLIENT_KEY, client);
        FileLogger.print("ListIP.txt", "IP: " + session.getRemoteAddress());
    }
    
      @Override
    public void sessionClosed(IoSession session) throws Exception {
        Client client = (Client) session.getAttribute(Client.CLIENT_KEY);
        if (client != null) {
            try {
                client.disconnect(true, true);
            } catch (Throwable t) {
                FileLogger.printError(FileLogger.ACCOUNT_STUCK, t);
            } finally {
                session.close();
                session.removeAttribute(Client.CLIENT_KEY);      
            }
        }
        super.sessionClosed(session);
    }

      @Override
    public void messageReceived(final IoSession session, final Object message) {
        try {
            PacketReader packet = new PacketReader((byte[])message);
            final short header_num = packet.readShort();
            for (final RecvPacketOpcode recv : RecvPacketOpcode.values()) {
                if (recv.getValue() == header_num) {
                    final Client c = (Client) session.getAttribute(Client.CLIENT_KEY);
                    handlePacket(recv, packet, c);
                    return;
                }
            }
        } catch (Exception e) {
           FileLogger.printError(FileLogger.PACKET_LOG, e);
        }
    }

    @Override
    public void sessionIdle(final IoSession session, final IdleStatus status) throws Exception {
        final Client client = (Client) session.getAttribute(Client.CLIENT_KEY);
        if (client != null) {
            client.sendPing();
        }
        super.sessionIdle(session, status);
    }
    
     public static boolean isSpamHeader(RecvPacketOpcode header) {
        switch (header) {
            case MOVE_LIFE:
            case MOVE_PLAYER:
            case SPECIAL_MOVE:
            case MOVE_SUMMON:
            case MOVE_PET:
            case QUEST_ACTION:
            case HEAL_OVER_TIME:
            case CHANGE_KEYMAP:
            case USE_INNER_PORTAL:
            case TAKE_DAMAGE:
            case NPC_ACTION:
                return true;
        }
        return false;
    }
     
    public static final void handlePacket(final RecvPacketOpcode header, final PacketReader reader, final Client c) throws Exception {
        if (GameConstants.LOG_PACKETS && !isSpamHeader(header)) {
            String tab = "";
            for (int i = 4; i > header.name().length() / 8; i--) {
                tab += "\t";
            }
            System.out.println("[Recv]\t" + header.name() + tab + "|\t" + header.getValue() + "\t|\t" + HexTool.getOpcodeToString(header.getValue()));
            FileLogger.log("PacketLog.txt", "\r\n\r\n[Recv]\t" + header.name() + tab + "|\t" + header.getValue() + "\t|\t" + HexTool.getOpcodeToString(header.getValue()) + "\r\n\r\n");
        }
        switch (header) {
            case PONG:
                c.pongReceived();
                break;
            case AFTER_LOGIN:
                if (ServerProperties.Login.ENABLE_PIN) {
                   CharLoginHandler.AfterLogin(reader, c);
                } else {
                  c.getSession().write(LoginPackets.PinOperation((byte) CharLoginHeaders.PIN_ACCEPTED));
                }
                break;
            case SERVERLIST_REQUEST:
            case SERVERLIST_REREQUEST:    
                CharLoginHandler.ServerListRequest(c);
                break;
            case ACCEPT_TOS:
                CharLoginHandler.AcceptToS(reader, c);
                break;
            case SET_GENDER:
                CharLoginHandler.SetGender(reader, c);
                break;
            case REGISTER_PIN:
                CharLoginHandler.RegisterPin(reader, c);
                break;
            case CHARLIST_REQUEST:
                CharLoginHandler.CharlistRequest(reader, c);
                break;
            case CHAR_SELECT:    
                CharLoginHandler.CharacterSelect(reader, c);
                break; 
            case LOGIN_PASSWORD:
                CharLoginHandler.Login(reader, c);
                break;  
            case RELOG:
                c.getSession().write(LoginPackets.GetRelogResponse());
                break;   
           case SERVERSTATUS_REQUEST:
                CharLoginHandler.ServerStatusRequest(c);
                break;
           case CHECK_CHAR_NAME:
                CharLoginHandler.CheckCharName(reader.readMapleAsciiString(), c);
                break;
           case CREATE_CHAR:
                CharLoginHandler.CreateChar(reader, c);
                break;
           case DELETE_CHAR:
                CharLoginHandler.DeleteChar(reader, c);
                break;  
           case VIEW_ALL_CHAR:
                CharLoginHandler.ViewChar(reader, c);
                break;
           case PICK_ALL_CHAR:
                CharLoginHandler.PickCharHandler(reader, c);
                break;
           // END OF LOGIN SERVER
           case CLIENT_ERROR:
                break;
            case CHANGE_CHANNEL:
                InterServerHandler.ChangeChannel(reader, c);
                break;
            case GENERAL_CHAT:
                ChatHandler.GeneralChat(reader, c);
                break;    
            case WHISPER:
                ChatHandler.Whisper_Find(reader, c);
                break;   
            case NPC_TALK:
                NPCHandler.NPCTalk(reader, c);
                break;    
            case NPC_TALK_MORE:
                NPCHandler.NPCMoreTalk(reader, c);
                break;    
            case QUEST_ACTION:
                NPCHandler.QuestAction(reader, c);
                break;    
            case NPC_SHOP:
                NPCHandler.NPCShop(reader, c, c.getPlayer());
                break;    
            case ITEM_GATHER:
                InventoryHandler.ItemGather(reader, c);
                break;                
            case ITEM_MOVE:
                InventoryHandler.ItemMove(reader, c);
                break;   
            case MESO_DROP:
                PlayerHandler.DropMeso(reader, c);
                break; 
            case PLAYER_LOGGEDIN:
                InterServerHandler.Loggedin(reader, c);
                break;  
            case CHANGE_MAP:
                if (reader.available() == 0) {
                    InterServerHandler.LeaveCS(reader, c);
                } else {
                    PlayerHandler.ChangeMap(reader, c);
                }
                break;
            case MOVE_LIFE:
                MobHandler.MoveMonster(reader, c);
                break; 
            case MELEE_ATTACK:
                PlayerHandler.MeleeAttack(reader, c);
                break;    
            case RANGED_ATTACK:
                PlayerHandler.RangedAttack(reader, c);
                break;
            case MAGIC_ATTACK:
                PlayerHandler.MagicDamage(reader, c);
                break;
            case TAKE_DAMAGE:
                PlayerHandler.TakeDamage(reader, c);
                break;    
            case MOVE_PLAYER:
                PlayerHandler.MovePlayer(reader, c.getPlayer());
                break;  
            case USE_CASH_ITEM:
                InventoryHandler.UseCashItem(reader, c);
                break;
            case USE_ITEM:
                InventoryHandler.UseItem(reader, c);
                break;
            case USE_RETURN_SCROLL:
                InventoryHandler.UseItem(reader, c);
                break;
            case USE_UPGRADE_SCROLL:
                InventoryHandler.UseUpgradeScroll(reader, c);
                break;                
            case USE_SUMMON_BAG:
                InventoryHandler.UseSummonBag(reader, c);
                break;
            case FACE_EXPRESSION:
                PlayerHandler.ChangeEmotion(reader, c);
                break;
            case HEAL_OVER_TIME:
                PlayerHandler.ReplenishHpMp(reader, c);
                break;                
            case ITEM_PICKUP:
                InventoryHandler.PickupPlayer(reader, c);
                break;
            case CHAR_INFO_REQUEST:
                PlayerHandler.OpenInfo(reader, c);
                break;
            case SPECIAL_MOVE:
                PlayerHandler.SpecialMove(reader, c);
                break;                
            case USE_INNER_PORTAL:
                PlayerHandler.InnerPortal(reader, c);
                break;
            case TROCK_ADD_MAP:
                PlayerHandler.TrockAddMap(reader, c, c.getPlayer());
                break;
            case CANCEL_BUFF:
                PlayerHandler.CancelBuffHandler(reader, c);
                break;                
            case CANCEL_ITEM_EFFECT:
                PlayerHandler.CancelItemEffect(reader, c);
                break;
            case PLAYER_INTERACTION:
                PlayerInteractionHandler.handleAction(reader, c);
                break;
            case RPS_ACTION:
                PlayerInteractionHandler.RockPaperScissors(reader, c);
                break;
            case DISTRIBUTE_AP:
                StatsHandling.DistributeAP(reader, c);
                break;
            case DISTRIBUTE_SP:
                StatsHandling.DistributeSP(reader, c);
                break;
            case CHANGE_KEYMAP:
                PlayerHandler.ChangeKeymap(reader, c);
                break;
            case CHANGE_MAP_SPECIAL:
                PlayerHandler.ChangeMapSpecial(reader, c);
                break;                
            case STORAGE:
                NPCHandler.Storage(reader, c);
                break;
            case GIVE_FAME:
                PlayersHandler.GiveFame(reader, c);
                break; 
            case PARTY_OPERATION:
                PartyHandler.handlePartyOperation(reader, c);
                break;                
            case DENY_PARTY_REQUEST:
                PartyHandler.PartyResponse(reader, c);
                break;                
            case PARTYCHAT:
                ChatHandler.PrivateChat(reader, c);
                break;
            case USE_DOOR:
                PlayersHandler.UseDoor(reader, c);
                break;
            case ENTER_MTS:
                MTSHandler.EnterMTS(reader, c);
                break;               
            case ENTER_CASH_SHOP:
                InterServerHandler.EnterCS(reader, c);
                break;
             case DAMAGE_SUMMON:
                SummonHandler.DamageSummon(reader, c);
                break;
            case MOVE_SUMMON:
                SummonHandler.MoveSummon(reader, c);
                break;
            case SUMMON_ATTACK:
                SummonHandler.SummonAttack(reader, c);
                break;
            case BUDDYLIST_MODIFY:
                BuddyListHandler.BuddyOperation(reader, c);
                break;
            case ENTERED_SHIP_MAP:
                break;
            case USE_ITEMEFFECT:
                PlayerHandler.UseItemEffect(reader, c);
                break;  
            case CHAIR:
                PlayerHandler.UseChair(reader, c);
                break;    
            case USE_CHAIR_ITEM:
                PlayerHandler.UseItemChair(reader, c);
                break;            
            case DAMAGE_REACTOR:
                PlayersHandler.HitReactor(reader, c);
                break;
            case GUILD_OPERATION:
                GuildHandler.Guild(reader, c);
                break;
            case DENY_GUILD_REQUEST:
                GuildHandler.DenyGuildRequest(reader, c);
                break;    
            case BBS_OPERATION:
                BBSHandler.BBSOperation(reader, c);
                break;                
            case SKILL_EFFECT:
                PlayerHandler.SkillEffect(reader, c);
                break;
             case MESSENGER:
                ChatHandler.Messenger(reader, c);
                break;
            case NPC_ACTION:
                NPCHandler.NPCAnimation(reader, c);
                break;                 
            case TOUCHING_CS:
                InterServerHandler.TouchingCS(reader, c);
                break; 
             case BUY_CS_ITEM:
                CashShopOperation.CashShopAction(reader, c);
                break;               
            case COUPON_CODE:
                CashCouponRequest.CouponCode(reader, c);
                break;
             case SPAWN_PET:
                PetHandler.SpawnPet(reader, c);
                break;
            case MOVE_PET:
                PetHandler.MovePet(reader, c);
                break;                 
            case PET_CHAT:
                PetHandler.PetChat(reader, c);
                break;  
            case PET_COMMAND:
                PetHandler.PetCommand(reader, c);
                break; 
            case PET_FOOD:
                PetHandler.PetFood(reader, c);
                break;
             case PET_LOOT:
                InventoryHandler.PetMapItemPickUp(reader, c);
                break;
            case AUTO_AGGRO:
                MobHandler.AutoAggro(reader, c);
                break;                 
            case MONSTER_BOMB:
                MobHandler.MonsterBomb(reader, c);
                break;
            case CANCEL_DEBUFF:
               // BuffHandler.CancelDebuff(reader, c);
                break;                
             case USE_SKILL_BOOK:
                InventoryHandler.UseSkillBook(reader, c);
                break;               
            case SKILL_MACRO:
                PlayerHandler.SkillMacroAssign(reader, c);
                break;
            case NOTE_ACTION:
                PlayersHandler.Note(reader, c);
                break;
            case MAPLETV:
                break;                
            case ENABLE_ACTION:
                PlayersHandler.EnableActions(reader, c);
                break; 
            case USE_CATCH_ITEM:
                InventoryHandler.UseCatchItem(reader, c);
                break;                
            case USE_MOUNT_FOOD:
                InventoryHandler.UseMountFood(reader, c);
                break;
            case CLOSE_CHALKBOARD:
                c.getPlayer().setChalkboard(null);
                c.getPlayer().getMap().broadcastMessage(PacketCreator.UseChalkBoard(c.getPlayer(), true));
                break;
            case DUEY_ACTION:
                NPCHandler.Duey(reader, c);
                break;
            case MONSTER_CARNIVAL:
                MonsterCarnivalHandler.MonsterCarnivalParty(reader, c);
                break;               
            case RING_ACTION:
                PlayersHandler.RingAction(reader, c);
                break;
            case SPOUSE_CHAT:
                ChatHandler.Spouse_Chat(reader, c);
                break;
            case REPORT_PLAYER:
                NotificationsHandler.ReportPlayer(reader, c);
                break;
            case PASSIVE_ENERGY:
                PlayerHandler.PassiveEnergy(reader, c);
                break;
            case UNSTUCK:
                c.getSession().write(PacketCreator.EnableActions());
                break; 
            case MOB_DAMAGE_MOB:
                MobHandler.FriendlyDamage(reader, c);
                break;
            case MTS_OP:
               // MTSHandler.MTS(reader, c);
                break;   
            case UNKNOWN:    
                break;
            case ALLIANCE_OPERATION:
                AllianceHandler.HandleAlliance(reader, c);
                break; 
            case PET_AUTO_POT:
                PetHandler.PetAutoPotion(reader, c);
                break; 
            case FREDRICK_REQUEST:
                HiredMerchantHandler.FredrickRequest(reader, c);
                break;
            case HIRED_MERCHANT_REQUEST:
                HiredMerchantHandler.HiredMerchantRequest(reader, c);
                break;
            case SILVER_BOX:
                InventoryHandler.UseSilverBox(reader, c);
                break;  
            case OWL_WARP:
                InventoryHandler.OwlWarp(reader, c);
                break;
            case OWL_ACTION:
                InventoryHandler.UseOwlOfMinerva(reader, c);
                break;
            case LOGGED_OUT:
                break;
            case PET_ITEM_IGNORE:
                PetHandler.PetExcludeItems(reader, c);
                break;
            case GRENADE:
                MobHandler.GrenadeEffect(reader, c);
                break;
            default:
                if (reader.available() >= 0) {
                    FileLogger.logPacket(String.valueOf(header), "[" + header.toString() + "] " + reader.toString());
                }
                break;
        }
    } 
}
