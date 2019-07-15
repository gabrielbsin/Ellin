package scripting.portal;

import client.Client;
import packet.creators.EffectPackets;
import scripting.AbstractPlayerInteraction;
import server.maps.portal.Portal;

public class PortalPlayerInteraction extends AbstractPlayerInteraction {
    
    private final Portal portal;
	
    public PortalPlayerInteraction(final Client c, final Portal portal) {
	super(c);
	this.portal = portal;
    }
	
    public Portal getPortal() {
	return portal;
    }
    
    public void playPortalSound() {
        c.getSession().write(EffectPackets.PlayPortalSound());
    } 
}
