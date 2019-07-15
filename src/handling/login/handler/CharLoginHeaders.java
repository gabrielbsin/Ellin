/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package handling.login.handler;

/**
 * 
 * @author GabrielSin   
 */
public class CharLoginHeaders {
    
    public static final byte
        SERVER_JAPAN = 3,
        SERVER_TEST = 5,
        SERVER_SEA = 7,
        SERVER_GLOBAL = 8, 
        SERVER_BRAZIL = 9
    ;
    
    public static final byte
        PIN_ACCEPTED = 0x00,
        PIN_REGISTER = 0x01,
        PIN_REJECTED = 0x02, 
        PIN_REQUEST = 0x04;
    
    public static final int 
        LOGIN_OK = 0, 
        LOGIN_BANNED = 2, 
        LOGIN_BLOCKED = 3,
        LOGIN_WRONG = 4,
        LOGIN_NOT_EXIST = 5,
        LOGIN_ALREADY = 7,
        LOGIN_ERROR = 8,
        LOGIN_ERROR_ = 9,
        LOGIN_CONNECTION = 10,
        LOGIN_EMAIL = 21,
        LOGIN_TOS = 23;
}
