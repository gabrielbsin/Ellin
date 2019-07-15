/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package launch;

import client.player.skills.PlayerSkillFactory;
import community.MapleGuildRanking;
import constants.ServerProperties;
import database.DatabaseConnection;
import handling.channel.ChannelServer;
import handling.login.LoginServer;
import handling.login.LoginTools;
import handling.world.World;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import server.ShutdownServer;
import server.SpeedQuizFactory;
import tools.TimerTools;
import server.itens.ItemInformationProvider;
import server.life.MapleMonsterInformationProvider;
import server.quest.MapleQuest;

public class Start {
    
        public static Start instance = new Start();
        public static long startTime = System.currentTimeMillis();
           
        public static Start getInstance() {
            if (instance == null) {
                instance = new Start();
            }
            return instance;
        }
        
        public static void main(String[] args) throws InterruptedException {
            instance.run();
        }
        
        public static void run() throws InterruptedException {
            DatabaseConnection.getConnection();
            Connection c = DatabaseConnection.getConnection();
            try {
                PreparedStatement ps = c.prepareStatement("UPDATE accounts SET loggedin = 0");
                ps.executeUpdate();
                ps = c.prepareStatement("UPDATE characters SET HasMerchant = 0");
                ps.executeUpdate();
                ps.close();
            } catch (SQLException ex) {
                System.out.println("Could not reset databases " + ex);
            }
            System.out.println("[Starting][" + ServerProperties.Login.SERVER_NAME + " Revision " + ServerProperties.World.REVISION +"]");
            World.init();
            
            TimerTools.WorldTimer.getInstance().start();
            TimerTools.MiscTimer.getInstance().start();
            TimerTools.ClientTimer.getInstance().start();
            TimerTools.MountTimer.getInstance().start();        
            TimerTools.MonsterTimer.getInstance().start(); 
            TimerTools.ItemTimer.getInstance().start();
            TimerTools.MapTimer.getInstance().start();
            TimerTools.EventTimer.getInstance().start();
            TimerTools.AntiCheatTimer.getInstance().start();
            TimerTools.NPCTimer.getInstance().start();
            TimerTools.CharacterTimer.getInstance().start();
            TimerTools.SkillTimer.getInstance().start(); 
            TimerTools.PingTimer.getInstance().start();
            TimerTools.CheatTrackerTimer.getInstance().start();
            
            printLoad("WORLDS");
            MapleGuildRanking.getInstance().getRank();
            printLoad("QUESTS");
            MapleQuest.loadAllQuest();
            printLoad("PROVIDER");
            ItemInformationProvider.getInstance().getAllItems();
            printLoad("MONSTER");
            MapleMonsterInformationProvider.getInstance();
            printLoad("SKILLS");
            PlayerSkillFactory.cacheSkills();
            printLoad("BASICS");
            LoginTools.setUp();
            printLoad("SPEEDQUIZ");
            SpeedQuizFactory.getInstance().initialize();  
            System.out.println("[/////////////////////////////////////////////////]");
            System.out.println("[Loading Login]");
            LoginServer.runLoginMain();
            System.out.println("[Login initialized]");
            System.out.println("[/////////////////////////////////////////////////]");
            System.out.println("[Loading Channel]");
            ChannelServer.startChannelMain();
            System.out.println("[Channel initialized]");
            System.out.println("[/////////////////////////////////////////////////]");
            Runtime.getRuntime().addShutdownHook(new Thread(new Shutdown()));
            World.registerRespawn();
            if (ShutdownServer.getInstance() == null) {
                ShutdownServer.registerMBean();
            } else {
                System.out.println("--MBean server was already active--");
            }
            LoginServer.setOn();
            System.out.println("[Fully Initialized in " + (System.currentTimeMillis() - startTime) / 1000L + " seconds]");
            System.out.println("[/////////////////////////////////////////////////]");
        } 
        
    private static void printLoad(String thread) {
        System.out.println("[Loading Completed][" + thread + "][Completed in " + (System.currentTimeMillis() - startTime) + " milliseconds.]");
    }
    
    public static class Shutdown implements Runnable {

        @Override
        public void run() {
            ShutdownServer.getInstance().run();
        }
    }
}
