/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package main.redanda.core.messages;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import main.redanda.core.Channel;
import main.redanda.core.Test;
import main.redanda.crypt.ECKey;

/**
 *
 * @author robin
 */
public class TextMsg extends RawMsg {

    public static final byte BYTE = (byte) 0;

    protected TextMsg(ECKey key, long timestamp, int nonce) {
        super(key, timestamp, nonce);
        public_type = 20;
    }

    public TextMsg(ECKey key, long timestamp, int nonce, byte[] signature, byte[] content, byte[] decryptedContent, Channel channel, boolean verified, boolean readable, int database_Id) {
        super(key, timestamp, nonce, signature, content, decryptedContent, channel, verified, readable, database_Id);
        public_type = 20;
    }

    public static TextMsg build(Channel channel, String messageContent) {
        try {
            ECKey key = channel.getKey();
            TextMsg textMsg = new TextMsg(key, System.currentTimeMillis(), Test.random.nextInt());

            textMsg.channel = channel;
            byte[] messageContentBytes = messageContent.getBytes("UTF-8");

            byte[] content = new byte[1 + 8 + messageContentBytes.length];
            ByteBuffer wrap = ByteBuffer.wrap(content);
            wrap.put(BYTE);
            wrap.putLong(Test.localSettings.identity);
            wrap.put(messageContentBytes);

            textMsg.decryptedContent = content;
            textMsg.readable = true;

            textMsg.encrypt();
            textMsg.sign();

            return textMsg;
        } catch (UnsupportedEncodingException ex) {
        }

        return null;
    }

    public byte[] getText() {

        ByteBuffer wrap = ByteBuffer.wrap(decryptedContent);
        wrap.get();
        wrap.getLong();

        byte[] out = new byte[wrap.remaining()];
        wrap.get(out);
        return out;

        //System.out.println("decrypted bytes: " + Msg.bytesToHexString(decryptedContent));
//
//        try {
//            return new String(decryptedContent, 9, decryptedContent.length - 9, "UTF-8");
//        } catch (UnsupportedEncodingException ex) {
//            Logger.getLogger(TextMsg.class.getName()).log(Level.SEVERE, null, ex);
//        }
//
//        return "charset error";
    }

    public String getTextString() {
        try {
            return new String(getText(), "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(TextMsg.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public long getIdentity() {
        ByteBuffer wrap = ByteBuffer.wrap(decryptedContent);
        wrap.get();
        return wrap.getLong();
    }
}
