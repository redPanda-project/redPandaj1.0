/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redPandaLib;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.Cipher;
import javax.imageio.ImageIO;
import org.redPandaLib.core.*;
import org.redPandaLib.core.messages.ImageMsg;
import org.redPandaLib.core.messages.RawMsg;
import org.redPandaLib.core.messages.TextMessageContent;
import org.redPandaLib.core.messages.TextMsg;
import org.redPandaLib.crypt.AddressFormatException;
import org.redPandaLib.database.DirectMessageStore;
import org.redPandaLib.database.HsqlConnection;
import org.redPandaLib.database.MysqlConnection;

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

        try {
            //check for AES 256...
            if (Cipher.getMaxAllowedKeyLength("AES") < 256) {
                System.out.println("You haven't installed java cryptography extension correctly!!");
                System.exit(1313);
            }
        } catch (NoSuchAlgorithmException ex) {
            System.out.println("AES is not available on your jvm?!");
            System.exit(1314);
        }

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
            HsqlConnection hsqlConnection = new HsqlConnection();
            Test.hsqlConnection = hsqlConnection;
            Test.messageStore = new DirectMessageStore(hsqlConnection.getConnection());
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Datenbank konnte nicht initialsiert werden. Abbruch...");
            System.exit(-2);
        }
    }

    public static void useMysqlDatabase() {

        try {

            String[] split = Settings.EXTERNAL_DATABASE_LOGIN_CREDENTIALS.split(",");

            MysqlConnection mysqlConnection = new MysqlConnection(split[1], split[0], split[2]);
//            Test.hsqlConnection = mysqlConnection;
            Test.messageStore = new DirectMessageStore(mysqlConnection.getConnection());
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Datenbank konnte nicht initialsiert werden. Abbruch...");
            System.exit(-2);
        }
    }

    public static void setMessageStore(HsqlConnection hsqlConnection) throws SQLException {
        Test.hsqlConnection = hsqlConnection;
        Test.messageStore = new DirectMessageStore(hsqlConnection.getConnection());
    }

    public static void setImageStoreFolder(String path) {
        Test.imageStoreFolder = path;
    }

    public static void setImageInfos(ImageInfos i) {
        Test.imageInfos = i;
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
        TextMsg build = TextMsg.build(channel, text);
        RawMsg addMessage = MessageHolder.addMessage(build);
        Test.broadcastMsg(addMessage);
        Test.messageStore.addDecryptedContent(addMessage.getKey().database_id, (int) addMessage.database_Id, TextMsg.BYTE, addMessage.timestamp, ((TextMsg) addMessage).getText(), ((TextMsg) addMessage).getIdentity(), true);
        TextMessageContent textMessageContent = TextMessageContent.fromTextMsg((TextMsg) addMessage, true);
        textMessageContent.read = true;
        for (NewMessageListener listener : Main.listeners) {
            listener.newMessage(textMessageContent);
        }
    }

    public static void sendImageToChannel(Channel channel, String pathToFile, boolean lowPriority) {
        //        Test.clientVersion++;
        //        Msg msg = new Msg(System.currentTimeMillis(), 99, channel, Test.clientSeed, Test.clientVersion, "[" + Test.getNick() + "] " + text);
        //        Test.processNewMessage(msg, true);
        //        RawMsg rawMsg = new RawMsg(channel.getKey(), System.currentTimeMillis(), 88);
        //        rawMsg.content = text.getBytes();
        //        rawMsg.sign();
        //        MessageHolder.addMessage(rawMsg);
        //        Test.broadcastMsg(rawMsg);
        ArrayList<ImageMsg> build = ImageMsg.build(channel, pathToFile, lowPriority);

        if (build == null) {
            return;//todo throw exception
        }

        RawMsg addMessage = null;

        for (ImageMsg m : build) {
            addMessage = MessageHolder.addMessage(m);
            Test.broadcastMsg(addMessage);
        }
        //Test.messageStore.addDecryptedContent(addMessage.getKey().database_id, (int) addMessage.database_Id, TextMsg.BYTE, "image...".getBytes(), ((TextMsg) addMessage).getIdentity(), true);
        //        TextMessageContent textMessageContent = TextMessageContent.fromTextMsg((TextMsg) addMessage, true);
        //        for (NewMessageListener listener : Main.listeners) {
        //            listener.newMessage(textMessageContent);
        //        }

        String imageInfos = pathToFile;
        try {
            ImageInfos.Infos infos = Test.imageInfos.getInfos(pathToFile);
            imageInfos = pathToFile + "\n" + infos.width + "\n" + infos.heigth;
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }

        Test.messageStore.addDecryptedContent(addMessage.getKey().database_id, (int) addMessage.database_Id, ImageMsg.BYTE, addMessage.timestamp, imageInfos.getBytes(), ((ImageMsg) addMessage).getIdentity(), true);

        TextMessageContent textMessageContent = TextMessageContent.fromImageMsg((ImageMsg) addMessage, true, imageInfos);
        textMessageContent.read = true;
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
            System.exit(0);
            return;
        }

        shutdown = true;
        Settings.connectToNewClientsTill = 0;
        System.out.println("disconnecting from peers...");

        new Thread() {

            @Override
            public void run() {
                if (Test.peerList != null) {
                    for (Peer p : Test.peerList) {
                        p.disconnect("shutdown");
                    }
                }
            }

        }.start();

        System.out.println("shutting down database...");
        if (Test.messageStore != null) {
            Test.messageStore.quit();
        }
        System.out.println("Save peers...");
        Test.savePeers();
        System.out.println("done");
        System.out.println("save trustdata...");
        Test.saveTrustData();
        System.out.println("done");

        System.out.println("finished shutdown sequenze...");

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

    public static void addSpamChannel() {
        ArrayList<Channel> channels = Test.getChannels();

        if (!channels.contains(SpecialChannels.SPAM)) {
            Channel c = SpecialChannels.SPAM;
            channels.add(c);
        }
        Test.saver.saveIdentities(channels);
    }

    public static void removeOldMessages() {
        Test.messageStore.removeOldMessages(System.currentTimeMillis() - 1000 * 60 * 60 * 24 * 7);
    }

    public static void removeAllOldMessages() {
        Test.messageStore.removeOldMessages(Long.MAX_VALUE);
    }

    public static void removeOldMessagesDecryptedContent() {
        Test.messageStore.removeOldMessagesDecryptedContent(System.currentTimeMillis() - 1000 * 60 * 60 * 24 * 7);
    }

    public static boolean backup(String path, String pw) {
        try {
            ExportImport.writeXML(path, Test.localSettings.identity, Test.channels, Test.localSettings.identity2Name, pw);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public static boolean restoreBackup(String path, String pw) {
        try {
            ExportImport.readXML(path, pw);
            Test.localSettings.save();
            Test.saver.saveIdentities(Test.channels);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public static void markAsRead(long message_id) {
        Test.messageStore.markAsRead(message_id);
    }

    public static void internetConnectionInterrupted() {
        ArrayList<Peer> clonedPeerList = Test.getClonedPeerList();
        for (Peer peer : clonedPeerList) {
            peer.disconnect("internetConnectionInterrupted");
        }

        Test.triggerOutboundthread();

    }
}
