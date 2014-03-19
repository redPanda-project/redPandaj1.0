/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redPandaLib.services;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.redPandaLib.Main;
import org.redPandaLib.NewMessageListener;
import org.redPandaLib.SpecialChannels;
import org.redPandaLib.core.*;
import org.redPandaLib.core.messages.DeliveredMsg;
import org.redPandaLib.core.messages.RawMsg;
import org.redPandaLib.core.messages.TextMessageContent;
import org.redPandaLib.core.messages.TextMsg;
import org.redPandaLib.crypt.ECKey;
import org.redPandaLib.crypt.Utils;

/**
 *
 * @author robin
 */
public class MessageVerifierHsqlDb {

    private static final int loadMsgsPerQuery = 4;
    private static final MyThread myThread = new MyThread();
    //static ExecutorService threadPool = Executors.newFixedThreadPool(4);
    private static int runningThreads = 0;
    public static boolean PAUSE = false;

    public static void start() {
        myThread.setPriority(Thread.MIN_PRIORITY);
        myThread.start();
    }

    public static void trigger() {
        myThread.interrupt();
    }

    static class MyThread extends Thread {

        @Override
        public void run() {

//            threadPool = Executors.newFixedThreadPool(4);

            final String orgName = Thread.currentThread().getName();
            Thread.currentThread().setName(orgName + " - MessageVerifier");

            while (!Main.shutdown) {


                try {

                    boolean breaked = false;


                    if (PAUSE) {
                        try {
                            sleep(1000);
                        } catch (InterruptedException ex) {
                        }
                        continue;
                    }
                    Connection connection = Test.messageStore.getConnection();

                    //get Key Id
                    String query = "SELECT message_id, pubkey.pubkey_id, pubkey, public_type, timestamp, nonce, signature, content from message left join pubkey on (pubkey.pubkey_id = message.pubkey_id) WHERE verified = 0 order by timestamp LIMIT " + loadMsgsPerQuery;
                    PreparedStatement pstmt = connection.prepareStatement(query);
                    ResultSet executeQuery = pstmt.executeQuery();

                    int cnt = 0;

                    while (executeQuery.next()) {
                        cnt++;

                        int message_id = executeQuery.getInt("message_id");
                        int pubkey_id = executeQuery.getInt("pubkey.pubkey_id");
                        byte[] pubkey = executeQuery.getBytes("pubkey");
                        ECKey ecKey = new ECKey(null, pubkey);
                        ecKey.database_id = pubkey_id;

                        byte public_type = executeQuery.getByte("public_type");
                        long timestamp = executeQuery.getLong("timestamp");
                        int nonce = executeQuery.getInt("nonce");
                        byte[] signature = executeQuery.getBytes("signature");
                        byte[] content = executeQuery.getBytes("content");



                        //Stick
                        if (signature == null) {

                            System.out.println("is this a stick?");

                            continue;

                        }


                        RawMsg rawMsg = new RawMsg(timestamp, nonce, signature, content, false);
                        rawMsg.database_Id = message_id;
                        rawMsg.key = ecKey;
                        rawMsg.public_type = public_type;
                        boolean verify = rawMsg.verify();

                        if (verify) {
                            //System.out.println("signature richtig...");
                            PreparedStatement stmt = connection.prepareStatement("update message SET verified = true WHERE message_id = ?");
                            stmt.setInt(1, message_id);
                            stmt.executeUpdate();
                            stmt.close();

                            final RawMsg message = rawMsg.toSpecificMsgType();

                            //nur damit es auch wirklich da ist... bugsuche...
                            message.database_Id = message_id;
                            message.key.database_id = pubkey_id;

//                            synchronized (MessageHolder.msgs) {
//                                int index = MessageHolder.msgs.indexOf(m);
//                                MessageHolder.msgs.set(index, message);
//                            }

                            if (Settings.BROADCAST_MSGS_AFTER_VERIFICATION) {
                                if (timestamp > System.currentTimeMillis() - 1000 * 60 * 60 * 24 * 7) {
                                    new Thread() {
                                        @Override
                                        public void run() {
                                            final String orgName = Thread.currentThread().getName();
                                            Thread.currentThread().setName(orgName + " - broadCastMsg");
                                            Test.broadcastMsg(message);
                                        }
                                    }.start();
                                }
                            }




                            if (message instanceof TextMsg) {

                                TextMsg textMsg = (TextMsg) message;
                                long identity = textMsg.getIdentity();
                                boolean fromMe = (identity == Test.localSettings.identity);

                                Test.messageStore.addDecryptedContent(pubkey_id, message_id, TextMsg.BYTE, textMsg.getText(), textMsg.getIdentity(), fromMe);
                                TextMessageContent fromTextMsg = TextMessageContent.fromTextMsg(textMsg, fromMe);

                                for (NewMessageListener listener : Main.listeners) {
                                    listener.newMessage(fromTextMsg);
                                }

                                //send delivered msg
                                if (Settings.SEND_DELIVERED_MSG && SpecialChannels.isSpecial(pubkey) == null) {
                                    DeliveredMsg build = DeliveredMsg.build(message.getChannel(), message.public_type, message.timestamp, message.nonce);
                                    RawMsg addMessage = MessageHolder.addMessage(build);
                                    Test.broadcastMsg(addMessage);
                                }
                            } else if (message instanceof DeliveredMsg) {
                                DeliveredMsg deliveredMsg = (DeliveredMsg) message;
                                //System.out.println(deliveredMsg.getNick() + " in " + deliveredMsg.getChannel().name + " hat die Nachrich bekommen: " + deliveredMsg.timestamp + " " + deliveredMsg.nonce);


                                //TextMsg build = TextMsg.build(deliveredMsg.getChannel(), deliveredMsg.getIdentity() + " in " + deliveredMsg.getChannel().getName() + " hat die Nachrich bekommen: " + formatTime(new Date(deliveredMsg.timestamp)) + " " + deliveredMsg.nonce);
                                Test.messageStore.addDecryptedContent(pubkey_id, message_id, DeliveredMsg.BYTE, deliveredMsg.decryptedContent, deliveredMsg.getIdentity(), false);
                                TextMessageContent fromTextMsg = TextMessageContent.fromDeliveredMsg(deliveredMsg, false);

                                for (NewMessageListener listener : Main.listeners) {
                                    listener.newMessage(fromTextMsg);
                                }
                            } else {
                                //System.out.println("No textmsg?");
                            }





                        } else {
                            System.out.println("falsche signature...");
                            System.out.println("pubkey bytes    : " + Channel.byte2String(rawMsg.key.getPubKey()));
                            System.out.println("signature bytes : " + Utils.bytesToHexString(rawMsg.signature));
                            System.out.println("len : " + rawMsg.signature.length);


                            Peer loadedFrom = null;
                            for (Peer peer : Test.getClonedPeerList()) {
                                if (peer.getPeerTrustData() == null || peer.getLoadedMsgs() == null) {
                                    continue;
                                }
                                ArrayList<Integer> loadedMsgs = (ArrayList<Integer>) peer.getLoadedMsgs().clone();

                                for (int i : loadedMsgs) {
                                    if (i == message_id) {
                                        loadedFrom = peer;
                                        System.out.println("loaded from peer: " + peer.getIp() + ":" + peer.getPort());
                                    }
                                }

                            }

                            if (loadedFrom != null && loadedFrom.getPeerTrustData() != null) {
                                for (Entry<Integer, ECKey> a : loadedFrom.getKeyToIdHis().entrySet()) {
                                    System.out.println("keyToId - id: " + a.getKey() + " key: " + Channel.byte2String(a.getValue().getPubKey()));
                                }
                            }


                            PreparedStatement stmt = connection.prepareStatement("delete FROM message WHERE message_id = ?");
                            stmt.setInt(1, message_id);
                            stmt.executeUpdate();
                            stmt.close();
                        }


                    }


                    if (cnt == loadMsgsPerQuery) {
                        breaked = true;
                    }

                    executeQuery.close();





                    try {
                        if (breaked) {
                            sleep(20);
                        } else {
                            sleep(20000);
                        }
                    } catch (InterruptedException ex) {
                    }

                } catch (Exception ex) {
                    Logger.getLogger(MessageVerifierHsqlDb.class.getName()).log(Level.SEVERE, null, ex);
                    Test.sendStacktrace(ex);
                }

            }
        }
    }

    public static String formatTime(Date date) {

        String hours = "" + date.getHours();
        String minutes = "" + date.getMinutes();
        String seconds = "" + date.getSeconds();

        if (hours.length() == 1) {
            hours = "0" + hours;
        }
        if (minutes.length() == 1) {
            minutes = "0" + minutes;
        }
        if (seconds.length() == 1) {
            seconds = "0" + seconds;
        }

        return hours + ":" + minutes + ":" + seconds;


    }
}