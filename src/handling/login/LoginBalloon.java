/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package handling.login;

import constants.ServerProperties;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author GabrielSin
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
                loginBallon.add(new LoginBalloon("Free GM for non asian!", 236, 122));
                loginBallon.add(new LoginBalloon("Join today, you scrub!", 0, 276));
                loginBallon.add(new LoginBalloon("So join if you are a HENEHOE!", 196, 263));
            }
        }
        return loginBallon;
    }  
}
