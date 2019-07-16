package handling.login;

import constants.ServerProperties;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Eric (http://forum.ragezone.com/members/801110.html)
 */
public class LoginBalloon {
    
    public int nX;
    public int nY;
    public String sMessage;
    public final static List<LoginBalloon> loginBallon = new ArrayList<>();
    
    public LoginBalloon(String sMessage, int nX, int nY) {
        this.sMessage = sMessage;    
        this.nX = nX;
        this.nY = nY;
    }
    
    public final static List<LoginBalloon> getBalloons() {
        if (loginBallon.isEmpty()) {
            if (ServerProperties.Login.ENABLE_BALLONS) {
                loginBallon.add(new LoginBalloon("", 236, 122));
                loginBallon.add(new LoginBalloon("", 0, 276));
                loginBallon.add(new LoginBalloon("", 196, 263));
            }
        }
        return loginBallon;
    }  
}
