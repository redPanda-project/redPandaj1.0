/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redPandaLib.core.messages;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.redPandaLib.core.Channel;
import org.redPandaLib.core.Test;
import org.spongycastle.util.io.TeeInputStream;

/**
 *
 * @author robin
 */
public class TextMessageContent implements Serializable {

    public int database_id;
    public int pubkey_id;
    public int public_type;
    public int message_type;
    public long timestamp;
    public byte[] decryptedContent;
    //
    public Channel channel;
    public long identity;
    public String text;
    public boolean fromMe;
    //

    public TextMessageContent() {
    }

    public TextMessageContent(int database_id, int pubkey_id, int public_type, int message_type, long timestamp, byte[] decryptedContent, Channel channel, long identity, String text, boolean fromMe) {
        this.database_id = database_id;
        this.pubkey_id = pubkey_id;
        this.public_type = public_type;
        this.message_type = message_type;
        this.timestamp = timestamp;
        this.decryptedContent = decryptedContent;
        this.channel = channel;
        this.identity = identity;
        this.text = text;
        this.fromMe = fromMe;
    }

    public static TextMessageContent fromTextMsg(TextMsg textMsg, boolean fromMe) {
        try {
            String text = new String(textMsg.getText(), "UTF-8");

            return new TextMessageContent(textMsg.database_Id, textMsg.key.database_id, textMsg.public_type, TextMsg.BYTE, textMsg.timestamp, textMsg.decryptedContent, textMsg.channel, textMsg.getIdentity(), text, fromMe);
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(TextMessageContent.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public static TextMessageContent fromDeliveredMsg(DeliveredMsg deliveredMsg, boolean fromMe) {
        return new TextMessageContent(deliveredMsg.database_Id, deliveredMsg.key.database_id, deliveredMsg.public_type, DeliveredMsg.BYTE, deliveredMsg.timestamp, deliveredMsg.decryptedContent, deliveredMsg.channel, deliveredMsg.getIdentity(), null, fromMe);
    }

    public Channel getChannel() {
        return channel;
    }

    public int getDatabase_id() {
        return database_id;
    }

    public byte[] getDecryptedContent() {
        return decryptedContent;
    }

    public boolean isFromMe() {
        return fromMe;
    }

    public long getIdentity() {
        return identity;
    }

    public int getMessage_type() {
        return message_type;
    }

    public int getPubkey_id() {
        return pubkey_id;
    }

    public String getText() {
        return text;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getName() {
        if (!Test.localSettings.identity2Name.containsKey(identity)) {
            return "unknown";
        }
        return Test.localSettings.identity2Name.get(identity);
    }

    @Override
    public boolean equals(Object obj) {

        if (!(obj instanceof TextMessageContent)) {
            return false;
        }

        return (database_id == ((TextMessageContent) obj).database_id);
    }
}
