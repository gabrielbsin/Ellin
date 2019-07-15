package community;

import java.util.Objects;

public class MapleBuddyInvitedEntry {

    public String name;
    public int inviter;
    public long expiration;

    public MapleBuddyInvitedEntry(final String n, final int inviterid) {
        name = n.toLowerCase();
        inviter = inviterid;
        expiration = System.currentTimeMillis() + 10 * 60 * 1000; 
    }

    @Override
    public final boolean equals(Object other) {
        if (!(other instanceof MapleBuddyInvitedEntry)) {
            return false;
        }
        MapleBuddyInvitedEntry oth = (MapleBuddyInvitedEntry) other;
        return (inviter == oth.inviter && name.equals(oth.name));
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 97 * hash + Objects.hashCode(this.name);
        hash = 97 * hash + this.inviter;
        hash = 97 * hash + (int) (this.expiration ^ (this.expiration >>> 32));
        return hash;
    }
    
    public static class BuddylistEntry {
	private final String name;
	private final int cid;
	private int channel;
	private boolean visible;

        public BuddylistEntry(String name, int characterId, int channel, boolean visible) {
            super();
            this.name = name;
            this.cid = characterId;
            this.channel = channel;
            this.visible = visible;
        }

        public int getChannel() {
            return channel;
        }

        public void setChannel(int channel) {
            this.channel = channel;
        }

        public boolean isOnline() {
            return channel >= 0;
        }

        public void setOffline() {
            channel = -1;
        }

        public String getName() {
            return name;
        }

        public int getCharacterId() {
            return cid;
        }

        public void setVisible(boolean visible) {
            this.visible = visible;
        }

        public boolean isVisible() {
            return visible;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + cid;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
               return false; 
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final BuddylistEntry other = (BuddylistEntry) obj;
            if (cid != other.cid) {
                return false;
            }
            return true;
        }   
    }
}
