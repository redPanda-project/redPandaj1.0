/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redPandaLib.core;

import java.math.BigInteger;
import java.sql.ResultSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.redPandaLib.core.messages.RawMsg;
import java.util.ArrayList;
import org.redPandaLib.core.messages.TextMessageContent;
import org.redPandaLib.crypt.ECKey;

/**
 *
 * @author robin
 */
public class MessageHolder {

    public static ArrayList<RawMsg> msgs = new ArrayList<RawMsg>();

    /**
     * Only contains the necessary data for sync...
     *
     * @return
     */
    public static ResultSet getAllMessages(long from, long to) {
        //return (ArrayList<RawMsg>) msgs.clone();
        ResultSet allMessagesForSync = Test.messageStore.getAllMessagesForSync(from, to);

        //System.out.println("NACHRICHTEN GEFUNDEN: " + allMessagesForSync.);

        return allMessagesForSync;
    }

    public static RawMsg addMessage(RawMsg m) {
//        synchronized (MessageHolder.msgs) {
//            if (!msgs.contains(m)) {
//                msgs.add(m);
//            }
//        }




        Test.messageStore.saveMsg(m);
        int msgId = Test.messageStore.getMsgId(m);

        int pubkey_id = Test.messageStore.getPubkeyId(m.getKey());

        System.out.println("IDIDIDID: " + pubkey_id);



        ECKey key = new ECKey(null, m.getKey().getPubKey(), m.getKey().isCompressed());
        key.database_id = pubkey_id;


        m.key = key;
        m.database_Id = msgId;

        return m;

    }

    public static boolean contains(RawMsg m) {
        //return msgs.contains(m);

        return Test.messageStore.containsMsg(m);

    }
//
//    public static int getId(RawMsg m) {
//        //return getAllMessages().indexOf(m);
//        return Test.messageStore.getMsgId(m);
//    }

    public static RawMsg getRawMsg(int id) {
        //return getAllMessages().get(id);

        return Test.messageStore.getMessageById(id);

    }

//    public static ArrayList<RawMsg> getAllNotVerifiedMessages() {
//        //return (ArrayList<RawMsg>) msgs.clone();
//
//        return new ArrayList<RawMsg>();
//
//    }
    public static void removeMessage(RawMsg m) {
        synchronized (MessageHolder.msgs) {
            msgs.remove(m);
        }
    }

    public static int getMessageCount() {
        return Test.messageStore.getMessageCount();
    }

    public static ArrayList<TextMessageContent> getMessages(Channel channel) {
        return getMessages(channel, System.currentTimeMillis() - 1000 * 60 * 60 * 24, System.currentTimeMillis());
    }

    public static ArrayList<TextMessageContent> getMessages(Channel channel, long from, long to) {
        ArrayList<TextMessageContent> list = Test.messageStore.getMessageContentsForPubkey(channel.getKey().getPubKey(),
                from, to);
        return list;
    }
}
