/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package server.minirooms.components;

public class Message {
    
    public final byte slot;
    public final String message;

    public Message(String message, byte slot) {
        this.slot = slot;
        this.message = message;
    }
}
