/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package main.redanda.core.messages;

import crypt.Utils;
import java.nio.ByteBuffer;
import java.util.HashMap;
import main.redanda.core.Channel;
import main.redanda.core.Log;
import main.redanda.crypt.ECKey;

/**
 *
 * @author robin
 */
public class InfoMsg extends RawMsg {

    public static final byte BYTE = (byte) 11;

    protected InfoMsg(ECKey key, long timestamp, int nonce, byte[] signature, byte[] content, byte[] decryptedContent, Channel channel, boolean verified, boolean readable, int database_Id) {
        super(key, timestamp, nonce, signature, content, decryptedContent, channel, verified, readable, database_Id);
        public_type = 20;
    }

    protected InfoMsg(ECKey key, long timestamp, int nonce) {
        super(key, timestamp, nonce);
        public_type = 20;
    }

    /**
     *
     * @param channel - send to the specific channel
     * @param timeStamp
     * @param nonce
     * @param myChannelsAsCommand - include FIRST byte + identity, after that
     * key,level pairs
     * @return
     */
    public static InfoMsg build(Channel channel, long timeStamp, int nonce, byte[] myChannelsAsCommand) {

        ECKey key = channel.getKey();
        InfoMsg msg = new InfoMsg(key, timeStamp, nonce);

        msg.channel = channel;

        msg.decryptedContent = myChannelsAsCommand;
        msg.readable = true;

        msg.encrypt();
        msg.sign();

        return msg;
    }

    public long getIdentity() {
        ByteBuffer wrap = ByteBuffer.wrap(decryptedContent);
        wrap.get();
        return wrap.getLong();
    }

    public HashMap<ECKey, Integer> getLevels() {

        HashMap<ECKey, Integer> map = new HashMap<ECKey, Integer>();

        ByteBuffer wrap = ByteBuffer.wrap(decryptedContent);
        wrap.get();
        wrap.getLong();

        Log.put("decrypted Infos: " + Utils.bytesToHexString(decryptedContent), 0);

        while (wrap.hasRemaining()) {

            Log.put("remaining: " + wrap.remaining(), 0);
            byte[] pubKeyBytes = new byte[33];
            wrap.get(pubKeyBytes);
            int level = wrap.getInt();
            map.put(new ECKey(null, pubKeyBytes), level);
        }

        return map;

    }
}
