/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server;

/**
 *
 * @author GABRIEL
 */
public abstract interface ShutdownServerMBean extends Runnable {
    
    public abstract void shutdown();
    
}
