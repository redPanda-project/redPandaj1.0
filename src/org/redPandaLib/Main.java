/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redPandaLib;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.redPandaLib.core.*;
import org.redPandaLib.core.messages.RawMsg;
import org.redPandaLib.core.messages.TextMessageContent;
import org.redPandaLib.core.messages.TextMsg;
import org.redPandaLib.crypt.AddressFormatException;
import org.redPandaLib.database.DirectMessageStore;
import org.redPandaLib.database.HsqlConnection;
import org.redPandaLib.database.MessageStore;

/**
 *
 * @author robin
 */
public class Main {

    public static ArrayList<NewMessageListener> listeners = new ArrayList<NewMessageListener>();
    public static boolean shutdown = false;

    /**
     * Erster Parameter ist stellt ein, ob Befehle von der Konsole gelesen
     * werden soll. Falls true, blockt diese Methode ansonsten nicht.
     *
     * @param listenConsole
     * @param saver
     * @throws IOException
     */
    public static void startUp(boolean listenConsole, SaverInterface saver) throws IOException {

        Test.main(listenConsole, saver);
        Thread thread = new Thread() {

                            @Override
                            public void run() {
                                while (Test.STARTED_UP_SUCCESSFUL == false) {
                                    try {
                                        sleep(50);
                                    } catch (InterruptedException ex) {
                                        Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                                    }
                                }
                            }
                            
                        };
        
        thread.start();
        
        try {
            thread.join();
        } catch (InterruptedException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
        

    }

    public static void useHsqlDatabase() {
        try {
            Test.messageStore = new DirectMessageStore(new HsqlConnection().getConnection());
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Datenbank konnte nicht initialsiert werden. Abbruch...");
            System.exit(-2);
        }
    }

    public static void setMessageStore(Connection connection) throws SQLException {
        Test.messageStore = new DirectMessageStore(connection);
    }

    /**
     * Sendet eine Nachricht in den Main Channel.
     *
     * @param text
     */
    public static void sendBroadCastMsg(String text) {

//        Test.clientVersion++;
//
//        if (Test.channels == null) {
//            System.out.println("could not send msg, no identity found...");
//            return;
//        }
        //Msg msg = new Msg(System.currentTimeMillis(), 33, SpecialChannels.MAIN, Test.clientSeed, Test.clientVersion, "[" + Test.getNick() + "] " + text);
        //Test.processNewMessage(msg, true);
        //Channel channelById = Channel.getChannelById(-2);
        TextMsg build = TextMsg.build(SpecialChannels.MAIN, text);
        RawMsg addMessage = MessageHolder.addMessage(build);
        Test.broadcastMsg(addMessage);


//        try {
//            Test.messageStore.addDecryptedContent(addMessage.getKey().database_id, (int) addMessage.database_Id, TextMsg.BYTE, ((TextMsg) addMessage).getText().getBytes("UTF-8"));
//        } catch (UnsupportedEncodingException ex) {
//            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
//        }



    }

    public static ArrayList<Channel> getChannels() {
        ArrayList<Channel> channels = Test.getChannels();
        if (channels == null) {
            return null;
        }
        //channels.add(SpecialChannels.MAIN);
        return channels;
    }

    public static void sendMessageToChannel(Channel channel, String text) {
//        Test.clientVersion++;
//        Msg msg = new Msg(System.currentTimeMillis(), 99, channel, Test.clientSeed, Test.clientVersion, "[" + Test.getNick() + "] " + text);
//        Test.processNewMessage(msg, true);

//        RawMsg rawMsg = new RawMsg(channel.getKey(), System.currentTimeMillis(), 88);
//        rawMsg.content = text.getBytes();
//        rawMsg.sign();
//        MessageHolder.addMessage(rawMsg);
//        Test.broadcastMsg(rawMsg);

        if (text.length() < 7) {
            text += "       ";
        }

        TextMsg build = TextMsg.build(channel, text);
        RawMsg addMessage = MessageHolder.addMessage(build);
        Test.broadcastMsg(addMessage);
        Test.messageStore.addDecryptedContent(addMessage.getKey().database_id, (int) addMessage.database_Id, TextMsg.BYTE, ((TextMsg) addMessage).getText(), ((TextMsg) addMessage).getIdentity(), true);

        TextMessageContent textMessageContent = TextMessageContent.fromTextMsg((TextMsg) addMessage, true);
        for (NewMessageListener listener : Main.listeners) {
            listener.newMessage(textMessageContent);
        }


    }

    public static void addListener(NewMessageListener l) {
        listeners.add(l);
    }

    public static void shutdown() {

        if (shutdown) {
            System.out.println("already shutted down");
            return;
        }

        shutdown = true;
        Settings.connectToNewClientsTill = 0;
        System.out.println("disconnecting from peers...");
        if (Test.peerList != null) {
            for (Peer p : Test.peerList) {
                p.disconnect();
            }
        }
        System.out.println("shuting down database...");
        Test.messageStore.quit();
        System.out.println("Save peers...");
        Test.savePeers();
        System.out.println("save trustdata...");
        Test.saveTrustData();
    }

    /**
     * Excpetion = key falsch. Null = schon vorhanden, alles richtig = Channel
     * objekt
     *
     * @param key
     * @param name
     * @return
     * @throws AddressFormatException
     */
    public static Channel importChannelFromHuman(String key, String name) throws AddressFormatException {
        Channel importFromHuman;
        try {
            importFromHuman = Channel.importFromHuman(key, name);
        } catch (AddressFormatException ex) {
            throw new AddressFormatException();
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new AddressFormatException();
        }

        if (importFromHuman == null) {
            throw new AddressFormatException();
        }

        boolean added = importFromHuman.addToList();

        if (added) {
            return importFromHuman;
        } else {
            return null;
        }

    }

    public static void addChannel(Channel channel) {
        Test.addChannel(channel);
    }

    public static void removeChannel(Channel channel) {
        Test.removeChannel(channel);
    }

    public static ArrayList<TextMessageContent> getMessages(Channel channel) {
        return MessageHolder.getMessages(channel);
    }

    public static ArrayList<TextMessageContent> getMessages(Channel channel, long from, long to) {
        return MessageHolder.getMessages(channel, from, to);
    }

    public static void addMainChannel() {
        ArrayList<Channel> channels = Test.getChannels();

        if (!channels.contains(SpecialChannels.MAIN)) {
            Channel c = SpecialChannels.MAIN;
            channels.add(c);
        }
        Test.saver.saveIdentities(channels);
    }
}
