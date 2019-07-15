package handling.login;

import constants.ServerProperties;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import handling.ServerHandler;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.buffer.SimpleBufferAllocator;
import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import packet.crypto.MapleCodecFactory;

public class LoginServer {
    
    public static final int PORT = 8484;
    private static IoAcceptor acceptor;
    private static Map<Integer, Integer> load = new HashMap<>();
    private static String serverName, eventMessage;
    private static int flag, maxCharacters, userLimit, usersOn = 0;
    private static boolean finishedShutdown = true;
        
    public static final void addChannel(final int channel) {
        load.put(channel, 0);
    }

    public static final void removeChannel(final int channel) {
        load.remove(channel);
    }
    
    public static final String getIP(int channel) {
        return load.get(channel).toString();
    }
   
    public static final void runLoginMain() {
        userLimit = ServerProperties.Login.USER_LIMIT;
        serverName = ServerProperties.Login.SERVER_NAME;
        eventMessage = ServerProperties.Login.EVENT_MESSAGE;
        flag = ServerProperties.Login.FLAG;

        IoBuffer.setUseDirectBuffer(false);
        IoBuffer.setAllocator(new SimpleBufferAllocator());
        
        acceptor = new NioSocketAcceptor();
        acceptor.getFilterChain().addLast("codec", (IoFilter) new ProtocolCodecFilter(new MapleCodecFactory()));
        acceptor.setHandler(new ServerHandler(-1));
        
        System.out.println("Aberto na porta " + PORT + ".");
        try {
            acceptor.bind(new InetSocketAddress(PORT));
        } catch (IOException e) {
            System.err.println("Binding to port " + PORT + " failed" + e);
        }
    }
      	
    public static final void shutdown() {
        if (finishedShutdown) {
            return;
        }
        System.out.println("Shutting down login...");
        acceptor.unbind();
        finishedShutdown = true; 
    }

    public static void setLoad(final Map<Integer, Integer> load_, final int usersOn_) {
        load = load_;
        usersOn = usersOn_;
    }
    
    public static final int getUsersOn() {
        return usersOn;
    }

    public static final int getUserLimit() {
        return userLimit;
    }
    
    public static String getServerName() {
        return serverName;
    }

    public static String getEventMessage() {
        return eventMessage;
    }

    public static int getFlag() {
        return flag;
    }

    public int getMaxCharacters() {
        return maxCharacters;
    }

    public static final Map<Integer, Integer> getLoad() {
        return load;
    }

    public void setLoad(Map<Integer, Integer> load) {
        this.load = load;
    }

    public void setEventMessage(String newMessage) {
        this.eventMessage = newMessage;
    }

    public void setFlag(int newflag) {
        flag = newflag;
    }

    public void setUserLimit(int newLimit) {
        userLimit = newLimit;
    }
    
    public static final boolean isShutdown() {
        return finishedShutdown;
    }
    
    public static final void setOn() {
        finishedShutdown = false;
    }
    
    public static int getNumberOfSessions() {
        return acceptor.getManagedSessions().size();
    }
}
