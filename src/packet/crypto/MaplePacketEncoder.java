/*
	This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 ~ 2010 Patrick Huy <patrick.huy@frz.cc> 
                       Matthias Butz <matze@odinms.de>
                       Jan Christian Meyer <vimes@odinms.de>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License version 3
    as published by the Free Software Foundation. You may not use, modify
    or distribute this program under any other version of the
    GNU Affero General Public License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package packet.crypto;

import client.Client;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;


import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import packet.transfer.write.OutPacket;
import tools.HexTool;

public class MaplePacketEncoder implements ProtocolEncoder {

    @Override
    public void encode(IoSession session, Object message, ProtocolEncoderOutput out) throws Exception {
	try {
	    final Client client = (Client) session.getAttribute(Client.CLIENT_KEY);
	    final byte[] input = ((OutPacket) message).getBytes();
	    if (client != null) {
		final byte[] unencrypted = new byte[input.length];
		System.arraycopy(input, 0, unencrypted, 0, input.length);
		final byte[] ret = new byte[unencrypted.length + 4];
		encrypt(unencrypted);
	
		    final byte[] header = client.getSendCrypto().getPacketHeader(unencrypted.length);
		    client.getSendCrypto().crypt(unencrypted);
		    System.arraycopy(header, 0, ret, 0, 4);
		    System.arraycopy(unencrypted, 0, ret, 4, unencrypted.length);
		    IoBuffer out_buffer = IoBuffer.wrap(ret);
		    session.write(out_buffer);

	    } else {
		out.write(IoBuffer.wrap(input));
	    }
	} catch (Exception e) {
	    System.out.println("ENCRYPTION EXCEPTION: "	+ e);
	}
    }
    
    public static byte[] encrypt(byte data[]) {
        for (int j = 0; j < 6; j++) {
            byte remember = 0;
            byte dataLength = (byte) (data.length & 0xFF);
            if (j % 2 == 0) {
                for (int i = 0; i < data.length; i++) {
                    byte cur = data[i];
                    cur = HexTool.rollLeft(cur, 3);
                    cur += dataLength;
                    cur ^= remember;
                    remember = cur;
                    cur = HexTool.rollRight(cur, (int) dataLength & 0xFF);
                    cur = ((byte) ((~cur) & 0xFF));
                    cur += 0x48;
                    dataLength--;
                    data[i] = cur;
                }
            } else {
                for (int i = data.length - 1; i >= 0; i--) {
                    byte cur = data[i];
                    cur = HexTool.rollLeft(cur, 4);
                    cur += dataLength;
                    cur ^= remember;
                    remember = cur;
                    cur ^= 0x13;
                    cur = HexTool.rollRight(cur, 3);
                    dataLength--;
                    data[i] = cur;
                }
            }
        }
        return data;
    }

    @Override
    public void dispose(IoSession is) throws Exception {
    }
}
