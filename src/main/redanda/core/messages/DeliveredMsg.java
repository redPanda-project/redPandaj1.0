/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package main.redanda.core.messages;

import java.nio.ByteBuffer;
import main.redanda.core.Channel;
import main.redanda.core.Test;
import main.redanda.crypt.ECKey;

/**
 *
 * @author robin
 */
public class DeliveredMsg extends RawMsg {

    public static final byte BYTE = (byte) 4;

    protected DeliveredMsg(ECKey key, long timestamp, int nonce, byte[] signature, byte[] content, byte[] decryptedContent, Channel channel, boolean verified, boolean readable, int database_Id) {
        super(key, timestamp, nonce, signature, content, decryptedContent, channel, verified, readable, database_Id);
        public_type = 20;
    }

    protected DeliveredMsg(ECKey key, long timestamp, int nonce) {
        super(key, timestamp, nonce);
        public_type = 20;
    }

    public static DeliveredMsg build(Channel channel, byte public_type_from_msg, long timeStamp, int nonce) {
        ECKey key = channel.getKey();
        DeliveredMsg deliveredMsg = new DeliveredMsg(key, System.currentTimeMillis(), 108);

        deliveredMsg.channel = channel;

        byte[] content = new byte[1 + 8 + 1 + 8 + 4];
        //byte[] content = new byte[32];
        ByteBuffer wrap = ByteBuffer.wrap(content);
        wrap.put(BYTE);
        wrap.putLong(Test.localSettings.identity);
        wrap.put(public_type_from_msg);
        wrap.putLong(timeStamp);
        wrap.putInt(nonce);

        deliveredMsg.decryptedContent = content;
        deliveredMsg.readable = true;

        deliveredMsg.encrypt();
        deliveredMsg.sign();

        return deliveredMsg;
    }

    public long getIdentity() {
        ByteBuffer allocate = ByteBuffer.allocate(8);
        allocate.put(decryptedContent, 1, 8);
        allocate.flip();
        return allocate.getLong();
        //return new Long(decryptedContent, 1 + 8 + 4, decryptedContent.length - (1 + 8 + 4));
        //return "asd";
    }
}
