/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package main.redanda.core.messages;

import java.nio.ByteBuffer;
import java.util.HashMap;
import main.redanda.core.Channel;
import main.redanda.crypt.ECKey;

/**
 *
 * @author robin
 */
public class BlockMsg extends RawMsg {

    public static final byte PUBLIC_TYPE = 0;
    public static final byte BYTE = (byte) 1;
    public static final long TIME_TO_SYNC_BACK = 1000L * 60L * 60L * 24L * 7L * 4L * 1L;
    public static final long BLOCK_SYNC_TO_TIME = 1000L * 60L * 5L;

    protected BlockMsg(ECKey key, long timestamp, int nonce, byte[] signature, byte[] content, byte[] decryptedContent, Channel channel, boolean verified, boolean readable, int database_Id) {
        super(key, timestamp, nonce, signature, content, decryptedContent, channel, verified, readable, database_Id);
        public_type = PUBLIC_TYPE;
    }

    protected BlockMsg(ECKey key, long timestamp, int nonce) {
        super(key, timestamp, nonce);
        public_type = PUBLIC_TYPE;
    }

    /**
     *
     * @param channel - send to the specific channel
     * @param timeStamp
     * @param nonce
     * @param allHistoryBytes - including cmd byte
     * @return
     */
    public static BlockMsg build(Channel channel, long timeStamp, int nonce, byte[] allHistoryBytes) {

        ECKey key = channel.getKey();
        BlockMsg msg = new BlockMsg(key, timeStamp, nonce);

        msg.channel = channel;

        msg.decryptedContent = allHistoryBytes;
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

    public long getMessageCount() {
        ByteBuffer wrap = ByteBuffer.wrap(decryptedContent);
        wrap.get();
        wrap.getLong();
        return wrap.getInt();
    }

    public int getContentHash() {
        ByteBuffer wrap = ByteBuffer.wrap(decryptedContent);
        wrap.get();
        wrap.getLong();
        wrap.getInt();
        return wrap.getInt();
    }

    public HashMap<ECKey, Integer> getLevels() {
        throw new UnsupportedOperationException();
//        HashMap<ECKey, Integer> map = new HashMap<ECKey, Integer>();
//
//        ByteBuffer wrap = ByteBuffer.wrap(decryptedContent);
//        wrap.get();
//        wrap.getLong();
//
//        Log.put("decrypted Infos: " + Utils.bytesToHexString(decryptedContent), 0);
//
//        while (wrap.hasRemaining()) {
//
//            Log.put("remaining: " + wrap.remaining(), 0);
//            byte[] pubKeyBytes = new byte[33];
//            wrap.get(pubKeyBytes);
//            int level = wrap.getInt();
//            map.put(new ECKey(null, pubKeyBytes), level);
//        }
//
//        return map;

    }
}
