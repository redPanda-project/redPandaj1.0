/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redPandaLib.core.messages;

import java.nio.ByteBuffer;
import org.redPandaLib.core.Channel;
import org.redPandaLib.core.Test;
import org.redPandaLib.crypt.ECKey;

/**
 *
 * @author robin
 */
public class ControlMsg extends RawMsg {

    public static final byte BYTE = (byte) 10;

    protected ControlMsg(ECKey key, long timestamp, int nonce, byte[] signature, byte[] content, byte[] decryptedContent, Channel channel, boolean verified, boolean readable, int database_Id) {
        super(key, timestamp, nonce, signature, content, decryptedContent, channel, verified, readable, database_Id);
        public_type = 51;
    }

    protected ControlMsg(ECKey key, long timestamp, int nonce) {
        super(key, timestamp, nonce);
        public_type = 51;
    }

    public static ControlMsg build(Channel channel, long timeStamp, int nonce, byte[] command) {
        ECKey key = channel.getKey();
        ControlMsg controlMsg = new ControlMsg(key, System.currentTimeMillis(), nonce);

        controlMsg.channel = channel;


        byte[] content = new byte[1 + command.length];
        ByteBuffer wrap = ByteBuffer.wrap(content);
        wrap.put(BYTE);
        wrap.put(command);


        controlMsg.decryptedContent = content;
        controlMsg.readable = true;

        controlMsg.encrypt();
        controlMsg.sign();

        return controlMsg;
    }
}
