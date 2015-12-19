/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redPandaLib.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import static java.lang.Thread.sleep;
import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.redPandaLib.Main;
import org.redPandaLib.NewMessageListener;
import org.redPandaLib.SpecialChannels;
import org.redPandaLib.core.*;
import org.redPandaLib.core.ImageInfos.Infos;
import static org.redPandaLib.core.Test.NAT_OPEN;
import static org.redPandaLib.core.Test.inBytes;
import static org.redPandaLib.core.Test.messageStore;
import static org.redPandaLib.core.Test.outBytes;
import static org.redPandaLib.core.Test.peerList;
import org.redPandaLib.core.messages.BlockMsg;
import org.redPandaLib.core.messages.DeliveredMsg;
import org.redPandaLib.core.messages.ImageMsg;
import org.redPandaLib.core.messages.InfoMsg;
import org.redPandaLib.core.messages.RawMsg;
import org.redPandaLib.core.messages.TextMessageContent;
import org.redPandaLib.core.messages.TextMsg;
import org.redPandaLib.crypt.ECKey;
import org.redPandaLib.crypt.Sha256Hash;
import org.redPandaLib.crypt.Utils;
import org.redPandaLib.database.DirectMessageStore;
import org.redPandaLib.database.HsqlConnection;

/**
 * MAX_PERMITS
 *
 * @author robin
 */
public class MessageVerifierHsqlDb {

    private static final int loadMsgsPerQuery = 200;
    public static final MyThread myThread = new MyThread();
    static ExecutorService threadPool = Executors.newFixedThreadPool(4);
    private static int runningThreads = 0;
    public static boolean PAUSE = false;
    public static ArrayList<ImageMsg> imageMsgs = new ArrayList<ImageMsg>();
    private static long lastRemovedOldMessages = 0;
    private static ExecutorService sendDeliveredMsgsThreads = Executors.newFixedThreadPool(2);
    private static long lastDbReconnected = 0;
    public static int MAX_PERMITS = 10;
    public static Semaphore sem = new Semaphore(MAX_PERMITS);
    public static boolean USES_UNREAD_STATUS = false;
    public static long lastRun = 0;
    public static long LAST_AUTO_GENERATED_BLOCK = 0;

    private static Thread lastThread = null;

    public static final ReentrantLock filesSystemLockForImages = new ReentrantLock(); //Need this lock because if check for all blocks then one might not be finished.

    public static void start() {
        myThread.setPriority(Thread.MIN_PRIORITY);
        myThread.start();

//        new Thread() {
//
//            @Override
//            public void run() {
//                while (!Main.shutdown) {
//
//                    try {
//                        System.out.println("Database check alive: " + Test.messageStore.getConnection().isValid(2));
//                        if (!Test.messageStore.getConnection().isValid(2)) {
//
//                            if (System.currentTimeMillis() - lastDbReconnected > 1000 * 15) {
//                                Test.hsqlConnection.reconnect();
//                                lastDbReconnected = System.currentTimeMillis();
//                            }
//                        }
//                    } catch (SQLException ex) {
//                        Logger.getLogger(MessageVerifierHsqlDb.class.getName()).log(Level.SEVERE, null, ex);
//                    }
//                    try {
//                        sleep(60 * 1000);
//                    } catch (InterruptedException ex) {
//                        Logger.getLogger(MessageVerifierHsqlDb.class.getName()).log(Level.SEVERE, null, ex);
//                    }
//                }
//            }
//        }.start();
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

            try {
                sleep(2000);
            } catch (InterruptedException ex) {
            }

            while (!Main.shutdown) {

                lastRun = System.currentTimeMillis();

                if ((System.currentTimeMillis() - lastRemovedOldMessages) > 1000 * 60 * 60) {
                    lastRemovedOldMessages = System.currentTimeMillis();
                    if (Settings.REMOVE_OLD_MESSAGES) {
                        Test.messageStore.removeOldMessages(System.currentTimeMillis() - 1000 * 60 * 60 * 24 * 7);
                    }

                    //currently buggy moves to many messages ?!?
                    //Test.messageStore.moveChannelMessagesToHistory(System.currentTimeMillis() - 1000L * 60L * 60L * 24L * 30L * 2L);
                    if (Test.messageStore.getConnection() instanceof HsqlConnection) {
                        Test.messageStore.checkpoint();
                        System.out.println("checkpointed!!!!!");
                    }

                }

                try {

                    boolean breaked = false;

                    if (PAUSE) {
                        try {
                            sleep(1000);
                        } catch (InterruptedException ex) {
                        }
                        continue;
                    }
                    final Connection connection = Test.messageStore.getConnection();

                    Log.put("prepare select... " + connection.isValid(2), 70);

//                    if (!connection.isValid(2)) {
//
//
//                        if (System.currentTimeMillis() - lastDbReconnected > 1000 * 60 * 5) {
//                            Test.hsqlConnection.reconnect();
//                            lastDbReconnected = System.currentTimeMillis();
//                        }
//                        sleep(500);
//                        continue;
//                    }
                    //get Key Id
                    String query = "SELECT message_id, pubkey.pubkey_id, pubkey, public_type, timestamp, nonce, signature, content from message left join pubkey on (pubkey.pubkey_id = message.pubkey_id) WHERE verified = 0 order by timestamp LIMIT " + loadMsgsPerQuery;

                    PreparedStatement pstmt = connection.prepareStatement(query);
                    pstmt.setQueryTimeout(2);
                    Log.put("selecting", 70);
                    ResultSet executeQuery = pstmt.executeQuery();
                    Log.put("finish!", 70);

                    int cnt = 0;

                    while (executeQuery.next()) {
                        Log.put("new msg to verify... ", 70);
                        cnt++;

                        final int message_id = executeQuery.getInt("message_id");
                        final int pubkey_id = executeQuery.getInt("pubkey.pubkey_id");
                        final byte[] pubkey = executeQuery.getBytes("pubkey");
                        ECKey ecKey = new ECKey(null, pubkey);
                        ecKey.database_id = pubkey_id;

                        byte public_type = executeQuery.getByte("public_type");
                        final long timestamp = executeQuery.getLong("timestamp");
                        int nonce = executeQuery.getInt("nonce");
                        byte[] signature = executeQuery.getBytes("signature");
                        byte[] content = executeQuery.getBytes("content");

                        MessageDownloader.publicMsgsLoadedLock.lock();
                        MessageDownloader.publicMsgsLoaded--;
//                        if (MessageDownloader.publicMsgsLoaded < 0) {
//                            MessageDownloader.publicMsgsLoaded = 0;
//                        }

                        Log.put("msgs to verify: " + MessageDownloader.publicMsgsLoaded, 100);
                        MessageDownloader.publicMsgsLoadedLock.unlock();

                        //Stick
                        if (signature == null) {

                            System.out.println("is this a stick?");

                            continue;

                        }

                        RawMsg tempRawMsg = new RawMsg(timestamp, nonce, signature, content, false);
                        tempRawMsg.database_Id = message_id;
                        tempRawMsg.key = ecKey;
                        tempRawMsg.public_type = public_type;

                        final RawMsg rawMsg = tempRawMsg;

                        Runnable runnable;
                        runnable = new Runnable() {

                            @Override
                            public void run() {

                                try {
                                    setPriority(Thread.MIN_PRIORITY);

                                    final String orgName = Thread.currentThread().getName();
                                    if (orgName.length() < 20) {
                                        Thread.currentThread().setName(orgName + " - verify and broadcast thread");
                                    }

                                    boolean verify = false;
                                    try {
                                        verify = rawMsg.verify();
                                    } catch (NullPointerException ex) {
                                        Test.sendStacktrace(ex);
                                    }

                                    if (Main.shutdown) {
                                        return;
                                    }

                                    try {
                                        if (verify) {

                                            final RawMsg message = rawMsg.toSpecificMsgType();

                                            //nur damit es auch wirklich da ist... bugsuche...
                                            message.database_Id = message_id;
                                            message.key.database_id = pubkey_id;

//                            synchronized (MessageHolder.msgs) {
//                                int index = MessageHolder.msgs.indexOf(m);
//                                MessageHolder.msgs.set(index, message);
//                            }
                                            boolean succesfullCommited = false;
                                            while (!succesfullCommited) {
                                                PreparedStatement stmt = null;
                                                try {
                                                    //System.out.println("signature richtig...");
                                                    stmt = connection.prepareStatement("update message SET verified = true WHERE message_id = ?");
                                                    stmt.setInt(1, message_id);
                                                    stmt.executeUpdate();
                                                    stmt.close();
                                                    succesfullCommited = true;
                                                } catch (SQLIntegrityConstraintViolationException e) {
                                                    Log.put("Could not update verified status of message, have to try again....", 150);
                                                    stmt.close();
                                                }
                                            }

                                            if (message.public_type == BlockMsg.PUBLIC_TYPE) {

                                                System.out.println("i found a block!!!");

                                                MessageDownloader.channelIdToLatestBlockTimeLock.lock();
                                                Long get = MessageDownloader.channelIdToLatestBlockTime.get(pubkey_id);
                                                if (get == null || get < message.timestamp) {
                                                    MessageDownloader.channelIdToLatestBlockTime.put(pubkey_id, message.timestamp);
                                                }
                                                MessageDownloader.channelIdToLatestBlockTimeLock.unlock();

                                                if (message.readable) {

//                                                System.out.println("hex: " + Utils.bytesToHexString(message.decryptedContent));
                                                    BlockMsg blockMsg = (BlockMsg) message;
                                                    String text = "Block: " + blockMsg.getMessageCount() + " msgs (" + blockMsg.content.length / 1024. + " kb).";
                                                    boolean fromMe = (blockMsg.getIdentity() == Test.localSettings.identity);
                                                    Test.messageStore.addDecryptedContent(blockMsg.getKey().database_id, (int) blockMsg.database_Id, BlockMsg.BYTE, blockMsg.timestamp, text.getBytes(), ((BlockMsg) blockMsg).getIdentity(), fromMe, blockMsg.nonce, blockMsg.public_type);
//                                                    TextMessageContent textMessageContent = new TextMessageContent(blockMsg.database_Id, blockMsg.key.database_id, blockMsg.public_type, TextMsg.BYTE, blockMsg.timestamp, blockMsg.decryptedContent, blockMsg.channel, blockMsg.getIdentity(), text, true);
//                                                    textMessageContent.read = true;
//                                                    for (NewMessageListener listener : Main.listeners) {
//                                                        listener.newMessage(textMessageContent);
//                                                    }

                                                    //generate Count and Hash!
                                                    try {

                                                        HashAndCount generateMyHashAndMsgCount = generateMyHashAndMsgCount(blockMsg);

                                                        if (generateMyHashAndMsgCount == null) {
                                                            return;
                                                        }

                                                        int msgcount = generateMyHashAndMsgCount.cnt;
                                                        int hash = generateMyHashAndMsgCount.hash;

                                                        System.out.println("MyCnt: " + msgcount);
                                                        System.out.println("BlockCnt: " + blockMsg.getMessageCount());
                                                        System.out.println("Hashes: " + hash + " - " + blockMsg.getContentHash());

                                                        //compare cnt and hash:
                                                        if (blockMsg.getMessageCount() != msgcount || blockMsg.getContentHash() != hash) {
                                                            System.out.println("Count or hash not equal, syncing block with my database!");

                                                            ByteBuffer wrap = ByteBuffer.wrap(blockMsg.decryptedContent);
                                                        //read block!

                                                            //skip header
                                                            wrap.get(); //BlockMsg.BYTE); //cmd for block
                                                            wrap.getLong();//indenity who generated the block
                                                            wrap.getInt();//msgcount
                                                            wrap.getInt();//hash

                                                            int cnt = 0;

                                                            while (wrap.remaining() >= 8 + 4 + 4 + 8 + 4) {
                                                                cnt++;
                                                                //System.out.println("cnt: " + cnt);

                                                                long timestamp = wrap.getLong();
                                                                int nonce = wrap.getInt();
                                                                int message_type = wrap.getInt();
                                                                long identity = wrap.getLong();
                                                                int contentLenght = wrap.getInt();

                                                                //System.out.println("contentlen: " + contentLenght);
                                                                if (contentLenght > 1024 * 200) {
                                                                    System.out.println("Message content too big...");
                                                                    continue;
                                                                }

                                                                byte[] content = null;
                                                                if (contentLenght > 0) {
                                                                    content = new byte[contentLenght];
                                                                    try {
                                                                        wrap.get(content);
                                                                    } catch (Throwable e) {
                                                                        System.out.println("Wrong length!!1 - invalid block");
                                                                        break;
                                                                    }
                                                                }

                                                                fromMe = (identity == Test.localSettings.identity);

                                                                boolean added = Test.messageStore.addDecryptedContent(pubkey_id, TextMsg.BYTE, timestamp, content, identity, fromMe, nonce, (byte) 20);
                                                                if (added) {
                                                                    String string = "";
                                                                    if (contentLenght > 0) {
                                                                        string = new String(content, "UTF-8");
                                                                    }
                                                                    TextMessageContent textmsgcontent = new TextMessageContent(-1, pubkey_id, (byte) 20, message_type, timestamp, content, blockMsg.channel, identity, string, fromMe);

                                                                    for (NewMessageListener listener : Main.listeners) {
                                                                        listener.newMessage(textmsgcontent);
                                                                    }

                                                                    if (USES_UNREAD_STATUS && !fromMe) {
                                                                        Test.messageStore.addUnreadMessage(message_id);
                                                                    }
                                                                }

                                                            }

                                                            System.out.println("rdy!!! #################");

                                                            //we have to look if the block and my msgs are now the same:
                                                            generateMyHashAndMsgCount = generateMyHashAndMsgCount(blockMsg);

                                                            if (generateMyHashAndMsgCount == null) {
                                                                return;
                                                            }

                                                            msgcount = generateMyHashAndMsgCount.cnt;
                                                            hash = generateMyHashAndMsgCount.hash;

                                                            if (blockMsg.getMessageCount() != msgcount || blockMsg.getContentHash() != hash) {
                                                                System.out.println("i have to generate a new block");

                                                                if (System.currentTimeMillis() - LAST_AUTO_GENERATED_BLOCK > 1000 * 60 * 60 * 4 && System.currentTimeMillis() - blockMsg.timestamp < 1000 * 60 * 10) {
                                                                    Blocks.generate(blockMsg.channel);
                                                                    LAST_AUTO_GENERATED_BLOCK = System.currentTimeMillis();
                                                                }

                                                            } else {
                                                                System.out.println("all fine");
                                                            }

                                                        } else {

                                                            System.out.println("block and my database are the same!!! yeah!");
                                                        }

                                                    } catch (SQLException ex) {
                                                        Logger.getLogger(DirectMessageStore.class.getName()).log(Level.SEVERE, null, ex);
                                                    } catch (UnsupportedEncodingException ex) {
                                                        Logger.getLogger(MessageVerifierHsqlDb.class.getName()).log(Level.SEVERE, null, ex);
                                                    }

                                                } else {
                                                    System.out.println("block not for me...");
                                                }

                                                System.out.println("New block saved and send, doing cleanup...");

                                                //remove old block:
                                                int removeMessagesFromChannel = Test.messageStore.removeMessagesFromChannel(pubkey_id, BlockMsg.PUBLIC_TYPE, message.timestamp);
                                                System.out.println("removed old blocks: " + removeMessagesFromChannel);

                                                //remove old messages which are encrypted and now saved in the new block (only necessary data)...
                                                removeMessagesFromChannel = Test.messageStore.removeMessagesFromChannel(pubkey_id, (byte) 20, message.timestamp);
                                                System.out.println("removed old encrypted messages: " + removeMessagesFromChannel);

                                            } else // check for other msgs types with first byte of decrypted content
                                            if (message instanceof TextMsg) {

                                                TextMsg textMsg = (TextMsg) message;
                                                long identity = textMsg.getIdentity();
                                                boolean fromMe = (identity == Test.localSettings.identity);

                                                Test.messageStore.addDecryptedContent(pubkey_id, message_id, TextMsg.BYTE, textMsg.timestamp, textMsg.getText(), textMsg.getIdentity(), fromMe, textMsg.nonce, textMsg.public_type);
                                                TextMessageContent fromTextMsg = TextMessageContent.fromTextMsg(textMsg, fromMe);

                                                for (NewMessageListener listener : Main.listeners) {
                                                    listener.newMessage(fromTextMsg);
                                                }

                                                if (USES_UNREAD_STATUS && !fromMe) {
                                                    Test.messageStore.addUnreadMessage(message_id);
                                                }

                                                //send delivered msg
                                                if (Settings.SEND_DELIVERED_MSG && SpecialChannels.isSpecial(pubkey) == null) {

                                                    sendDeliveredMessage(message);

                                                }

                                                //TODO: REMOVE WHEN NOT NEEDED ANYMORE, generating stats for debugging and send to MainChannel.
                                                if (SpecialChannels.isSpecial(pubkey) != null) {
                                                    //System.out.println("Special channel text: " + fromTextMsg.text);
                                                    if (fromTextMsg.text.equals("status")) {

                                                        System.out.println("sending status");

                                                        String out = "Messages in db: ";

                                                        out += Test.messageStore.getMessageCount() + " - to verify: " + Test.messageStore.getMessageCountToVerify();

                                                        out += "\n\n";

                                                        int actCons = 0;

                                                        ArrayList<Peer> list = (ArrayList<Peer>) peerList.clone();
                                                        Collections.sort(list);

                                                        for (Peer peer : list) {

                                                            if (peer.isConnected()) {
                                                                actCons++;
                                                            }

                                                        }

                                                        out += "\nConnected to " + actCons + "/" + list.size() + " peers. (NAT type: " + (NAT_OPEN ? "open" : "closed") + ")";
                                                        out += "\nTraffic: " + inBytes / 1024. + " kb / " + outBytes / 1024. + " kb.";

                                                        Main.sendBroadCastMsg(out);

                                                    } else if (fromTextMsg.text.equals("knownTrigger")) {

                                                        System.out.println("trigger knwon channels by main channel...");

                                                        new Thread() {

                                                            @Override
                                                            public void run() {
                                                                KnownChannels.updateMyChannels();
                                                                KnownChannels.sendAllKnownChannels();
                                                            }

                                                        }.start();

                                                    }
                                                }

                                            } else if (message instanceof DeliveredMsg) {
                                                DeliveredMsg deliveredMsg = (DeliveredMsg) message;
                                                //System.out.println(deliveredMsg.getNick() + " in " + deliveredMsg.getChannel().name + " hat die Nachrich bekommen: " + deliveredMsg.timestamp + " " + deliveredMsg.nonce);

                                                //TextMsg build = TextMsg.build(deliveredMsg.getChannel(), deliveredMsg.getIdentity() + " in " + deliveredMsg.getChannel().getName() + " hat die Nachrich bekommen: " + formatTime(new Date(deliveredMsg.timestamp)) + " " + deliveredMsg.nonce);
                                                Test.messageStore.addDecryptedContent(pubkey_id, message_id, DeliveredMsg.BYTE, deliveredMsg.timestamp, deliveredMsg.decryptedContent, deliveredMsg.getIdentity(), false, deliveredMsg.nonce, deliveredMsg.public_type);
                                                TextMessageContent fromTextMsg = TextMessageContent.fromDeliveredMsg(deliveredMsg, false);

                                                for (NewMessageListener listener : Main.listeners) {
                                                    listener.newMessage(fromTextMsg);
                                                }

                                            } else if (message instanceof ImageMsg) {

                                                System.out.println("Found a IMAGE!");

                                                ImageMsg imageMsg = (ImageMsg) message;

                                                int partsForImage = imageMsg.getParts();
                                                int partNumber = imageMsg.getPartCount();

                                                filesSystemLockForImages.lock();

                                                try {

                                                    String partFileName = "imgpart-" + imageMsg.getTimestamp() + "-" + imageMsg.getIdentity() + "-" + partNumber + "-" + partsForImage + ".part";
                                                    writeBytesToFile(imageMsg.getImageBytes(), Test.imageStoreFolder + partFileName);

                                                    System.out.println("wrote bytes into part file");

                                                    System.out.println("check for all exiting parts...");

                                                    boolean missing = false;

                                                    for (int i = 0; i < partsForImage; i++) {
                                                        File file = new File(Test.imageStoreFolder + "imgpart-" + imageMsg.getTimestamp() + "-" + imageMsg.getIdentity() + "-" + i + "-" + partsForImage + ".part");
                                                        if (!file.exists()) {
                                                            missing = true;
                                                            break;
                                                        }
                                                    }

                                                    System.out.println("part missing: " + missing);

                                                    if (!missing) {

                                                        String pathToFile = Test.imageStoreFolder + "img-" + imageMsg.getChannel().getName() + "-" + imageMsg.getTimestamp() + ".jpg";
                                                        File outFile = new File(pathToFile);
                                                        try {
                                                            FileOutputStream fileOutputStream = new FileOutputStream(outFile);
                                                            try {

                                                                for (int i = 0; i < partsForImage; i++) {
                                                                    File readFile = new File(Test.imageStoreFolder + "imgpart-" + imageMsg.getTimestamp() + "-" + imageMsg.getIdentity() + "-" + i + "-" + partsForImage + ".part");

                                                                    byte[] buffer = new byte[1024 * 50];
                                                                    InputStream ios = null;
                                                                    int readBytes = 0;
                                                                    try {
                                                                        ios = new FileInputStream(readFile); //ToDoE: file not found exception ?!?, THREADED!!! need to sync that all are finished
                                                                        while ((readBytes = ios.read(buffer)) != -1) {
                                                                            fileOutputStream.write(buffer, 0, readBytes);
                                                                        }
                                                                    } finally {
                                                                        try {
                                                                            if (ios != null) {
                                                                                ios.close();
                                                                            }
                                                                        } catch (IOException e) {
                                                                        }
                                                                    }

                                                                    readFile.delete();

                                                                }

                                                                Infos infos = Test.imageInfos.getInfos(pathToFile);
                                                                if (infos == null) {
                                                                    Log.put("image is no image?", 0);

                                                                } else {
                                                                    String imageInfos = pathToFile + "\n" + infos.width + "\n" + infos.heigth;

                                                                    long identity = imageMsg.getIdentity();
                                                                    boolean fromMe = (identity == Test.localSettings.identity);

                                                                    Test.messageStore.addDecryptedContent(pubkey_id, message_id, ImageMsg.BYTE, imageMsg.getTimestamp(), imageInfos.getBytes(), imageMsg.getIdentity(), fromMe, imageMsg.nonce, imageMsg.public_type);
                                                                    TextMessageContent fromTextMsg = TextMessageContent.fromImageMsg(imageMsg, fromMe, imageInfos);

                                                                    for (NewMessageListener listener : Main.listeners) {
                                                                        listener.newMessage(fromTextMsg);
                                                                    }

                                                                    if (USES_UNREAD_STATUS && !fromMe) {
                                                                        Test.messageStore.addUnreadMessage(message_id);
                                                                    }

                                                                    //send delivered msg
                                                                    if (Settings.SEND_DELIVERED_MSG && SpecialChannels.isSpecial(pubkey) == null) {
                                                                        sendDeliveredMessage(message);
                                                                    }
                                                                }

                                                            } catch (IOException ex) {
                                                                Logger.getLogger(ImageSaver.class.getName()).log(Level.SEVERE, null, ex);
                                                            } finally {
                                                                try {
                                                                    fileOutputStream.close();
                                                                } catch (IOException ex) {
                                                                    Logger.getLogger(MessageVerifierHsqlDb.class.getName()).log(Level.SEVERE, null, ex);
                                                                }
                                                            }
                                                        } catch (FileNotFoundException ex) {
                                                            Logger.getLogger(ImageSaver.class.getName()).log(Level.SEVERE, null, ex);
                                                        }

                                                    }

                                                } catch (Throwable e) {
                                                    Test.sendStacktrace(e);
                                                } finally {
                                                    filesSystemLockForImages.unlock();
                                                }
//                                imageMsgs.add((ImageMsg) message);
//
//                                System.out.println("in ram: " + imageMsgs.size());
//
//                                ArrayList<ImageMsg> clonedList = (ArrayList<ImageMsg>) imageMsgs.clone();
//
//                                //search for complete ImageSet
//
//                                HashMap<String, ArrayList<ImageMsg>> map = new HashMap<String, ArrayList<ImageMsg>>();
//
//
//                                for (ImageMsg msg : clonedList) {
//                                    String imgKey = "" + msg.getTimestamp() + msg.getIdentity();
//
//                                    if (!map.containsKey(imgKey)) {
//                                        map.put(imgKey, new ArrayList<ImageMsg>());
//                                    }
//                                    ArrayList<ImageMsg> get = map.get(imgKey);
//                                    get.add(msg);
//
//                                    System.out.println("PARTS: " + get.size());
//
//                                    if (msg.getParts() == get.size()) {
//
//
//
//                                        System.out.println("Alle Parts geladen für img: " + imgKey);
//
//                                        Collections.sort(get, new Comparator<ImageMsg>() {
//                                            @Override
//                                            public int compare(ImageMsg o1, ImageMsg o2) {
//                                                return o1.getPartCount() - o2.getPartCount();
//                                            }
//                                        });
//
//
//                                        System.out.println("sorted...");
//
//                                        for (ImageMsg asdf : get) {
//                                            System.out.println("BLOCK: " + asdf.getPartCount());
//                                        }
//
//
//
//                                        ByteBuffer imageBytesBuffer = ByteBuffer.allocate(get.get(0).getParts() * get.get(0).getImageBytes().length);
//
//                                        int bytesUsed = 0;
//
//                                        for (ImageMsg asdf : get) {
//                                            System.out.println("BLOCK: " + asdf.getPartCount());
//                                            byte[] localBytes = asdf.getImageBytes();
//
//                                            imageBytesBuffer.put(localBytes);
//                                            bytesUsed += localBytes.length;
//
//                                            imageMsgs.remove(asdf);
//
//                                        }
//
//                                        imageBytesBuffer.flip();
//
//                                        byte[] wdwdw = new byte[bytesUsed];
//                                        imageBytesBuffer.get(wdwdw);
//
//                                        new ImageSaver(Test.imageStoreFolder).saveImage(wdwdw, "img-" + get.get(0).getTimestamp() + ".jpg");
//
//                                String fileName = Test.imageStoreFolder + "img-" + get.get(0).getTimestamp() + ".jpg";
//                                ByteArrayInputStream byteInputStream = new ByteArrayInputStream(wdwdw);
//                                BufferedImage read = ImageIO.read(byteInputStream);
//
//                                String imageInfos = fileName + "\n" + read.getWidth() + "\n" + read.getHeight();
//
//                                ImageMsg firstMsg = get.get(0);
//
//                                long identity = firstMsg.getIdentity();
//                                boolean fromMe = (identity == Test.localSettings.identity);
//
//                                Test.messageStore.addDecryptedContent(pubkey_id, message_id, ImageMsg.BYTE, imageInfos.getBytes(), firstMsg.getIdentity(), fromMe);
//                                TextMessageContent fromTextMsg = TextMessageContent.fromImageMsg(firstMsg, fromMe, imageInfos);
//
//                                for (NewMessageListener listener : Main.listeners) {
//                                    listener.newMessage(fromTextMsg);
//                                }
//
//                                //send delivered msg
//                                if (Settings.SEND_DELIVERED_MSG && SpecialChannels.isSpecial(pubkey) == null) {
//                                    DeliveredMsg build = DeliveredMsg.build(message.getChannel(), message.public_type, message.timestamp, message.nonce);
//                                    RawMsg addMessage = MessageHolder.addMessage(build);
//                                    Test.broadcastMsg(addMessage);
//                                }
                                                // }
                                                // }
                                                //new ImageSaver("").saveImage("", "loaded.jpg");
                                            } else if (message instanceof InfoMsg) {

                                                try {

                                                    InfoMsg infoMsg = (InfoMsg) message;
                                                    long identity = infoMsg.getIdentity();

                                                    HashMap<ECKey, Integer> levels = infoMsg.getLevels();

                                                    int pubKeyIdFrom = Test.messageStore.getPubkeyId(infoMsg.getKey());

                                                    for (ECKey channel : levels.keySet()) {

                                                        int pubKeyIdfor = Test.messageStore.getPubkeyId(channel);

                                                        int level = levels.get(channel) + 1;

                                                        Test.messageStore.addKnownChannel(pubKeyIdfor, identity, pubKeyIdFrom, level);

                                                        Log.put("add from other node channelLevels: " + Utils.bytesToHexString(channel.getPubKey()) + " lvel: " + level + " from: " + pubKeyIdFrom, 0);

                                                    }

                                                } catch (Throwable e) {
                                                    e.printStackTrace();
                                                }

                                            } else {
                                                //System.out.println("No textmsg?");
                                            }

                                            if (Settings.BROADCAST_MSGS_AFTER_VERIFICATION) {
                                                if (timestamp > System.currentTimeMillis() - 1000L * 60L * 60L * 24L * 7L) {
                                                    Runnable sendRunnable = new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            final String orgName = Thread.currentThread().getName();
                                                            if (orgName.length() < 20) {
                                                                Thread.currentThread().setName(orgName + " - broadCastMsg");
                                                            }
                                                            Test.broadcastMsg(message);

                                                        }
                                                    };
                                                    sendDeliveredMsgsThreads.submit(sendRunnable);
                                                }
                                            }

                                        } else {

                                            try {
                                                System.out.println("wrong signature...");
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
                                                            peer.getPeerTrustData().badMessages++;
                                                            peer.disconnect("bad signature!!!");
                                                            System.out.println("disconnect...");
                                                            //Test.peerTrusts.remove(peer.peerTrustData);
                                                            //System.out.println("REMOVED PEER TRUST!");
                                                        }
                                                    }

                                                }

                                                if (loadedFrom != null && loadedFrom.getPeerTrustData() != null) {
                                                    for (Entry<Integer, ECKey> a : loadedFrom.getKeyToIdHis().entrySet()) {
                                                        System.out.println("keyToId - id: " + a.getKey() + " key: " + Channel.byte2String(a.getValue().getPubKey()));
                                                    }
                                                }
                                            } catch (Exception e) {

                                                System.out.println("fignature wrong, data could not be displayed, exception thrown");

                                            }

                                            PreparedStatement stmt = connection.prepareStatement("delete FROM peerMessagesIntroducedToHim WHERE message_id = ?");
                                            stmt.setInt(1, message_id);
                                            stmt.executeUpdate();
                                            stmt.close();

                                            PreparedStatement stmt2 = connection.prepareStatement("delete FROM message WHERE message_id = ?");
                                            stmt2.setInt(1, message_id);
                                            stmt2.executeUpdate();
                                            stmt2.close();
                                            Test.messageStore.resetMessageCounter();
                                        }

                                    } catch (SQLException ex) {
                                        Logger.getLogger(MessageVerifierHsqlDb.class.getName()).log(Level.SEVERE, null, ex);
                                    }

                                    //if (sem.availablePermits() < MAX_PERMITS) {
                                    sem.release();
                                    Log.put("Available permits after release: " + sem.availablePermits(), 20);
                                    //}

                                } catch (Throwable e) {
                                    e.printStackTrace();
                                    Test.sendStacktrace(e);
                                }
                            }

                        };

                        threadPool.submit(runnable);
                        Log.put("Available permits: " + sem.availablePermits(), 20);
                        sem.acquireUninterruptibly();
                    }

                    //wait for all to finish
                    sem.acquireUninterruptibly(MAX_PERMITS);
                    sem.release(MAX_PERMITS);

                    breaked = interrupted();

                    if (cnt == loadMsgsPerQuery) {
                        breaked = true;
                    } else {
                        MessageDownloader.publicMsgsLoadedLock.lock();
                        MessageDownloader.publicMsgsLoaded = 0;
                        MessageDownloader.publicMsgsLoadedLock.unlock();
                    }

                    executeQuery.close();

                    try {
                        if (breaked) {
                            sleep(20);
                        } else {
                            sleep(1000 * 60 * 5);

                        }
                    } catch (InterruptedException ex) {
                    }

                } catch (org.hsqldb.HsqlException ex) {

                    System.out.println("HSQL DB exception, not that problem here");

                } catch (java.sql.SQLTransactionRollbackException ex) {

                    System.out.println("transaction rollback exception, not that problem here, sleep 10 second");
                    try {
                        sleep(11375);
                    } catch (InterruptedException ex1) {
                    }

                } catch (Throwable ex) {
                    try {
                        sleep(1000);
                    } catch (InterruptedException ex1) {
                    }

//                    if (ex instanceof SQLNonTransientConnectionException) {
//                        if (!Main.shutdown) {
//                            if (System.currentTimeMillis() - lastDbReconnected > 1000 * 60 * 5) {
//                                System.out.println("################################\n#   reconnect db\n################################");
//                                Test.hsqlConnection.reconnect();
//                                lastDbReconnected = System.currentTimeMillis();
//                            }
//                        }
//                    }
                    if (ex instanceof java.sql.SQLTransactionRollbackException) {
                        System.out.println("LOCK PROBLEM - but that may not cause any problems here.");
                        ex.printStackTrace();
                    }

                    Logger.getLogger(MessageVerifierHsqlDb.class.getName()).log(Level.SEVERE, null, ex);

                    if (ex instanceof NullPointerException) {
                        Test.sendStacktrace(ex);
                        System.out.println("RESTART, nullpointer in verifier!");
                        System.exit(0);
                    }

                    if (ex instanceof SQLIntegrityConstraintViolationException) {
                        System.out.println("#############################Please remove database files!!!!#######################");
                        System.out.println("Please remove database files!!!!");
                        System.out.println("Please remove database files!!!!");
                        System.out.println("Please remove database files!!!!");
                        System.out.println("SYSTEM EXIT");
                        System.exit(0);
                    } else {
                        //Test.sendStacktrace(ex);
                    }

                }
            }
        }

        private void sendDeliveredMessage(final RawMsg message) {
            sendDeliveredMsgsThreads.submit(new Runnable() {

                @Override
                public void run() {
                    DeliveredMsg build = DeliveredMsg.build(message.getChannel(), message.public_type, message.timestamp, message.nonce);
                    RawMsg addMessage = MessageHolder.addMessage(build);
                    Test.broadcastMsg(addMessage);
                }
            });
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

    public static void writeBytesToFile(byte[] b, String path) {
        File file = new File(path);
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            try {
                fileOutputStream.write(b);

            } catch (IOException ex) {
                Logger.getLogger(ImageSaver.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                try {
                    fileOutputStream.close();

                } catch (IOException ex) {
                    Logger.getLogger(MessageVerifierHsqlDb.class.getName()).log(Level.SEVERE, null, ex);
                }

            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(ImageSaver.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public static void printStack() {
        StackTraceElement[] stackTrace = MessageVerifierHsqlDb.myThread.getStackTrace();
        String ownStackTrace = "";
        for (StackTraceElement a : stackTrace) {
            ownStackTrace += a.toString() + "\n";
        }
        System.out.println("MessageVerifierHsqlDb:" + ownStackTrace);

    }

    private static HashAndCount generateMyHashAndMsgCount(BlockMsg blockMsg) throws SQLException {
        //get Key Id
        //String query = "SELECT pubkey_id,message_id,content,public_type,timestamp,nonce from message WHERE timestamp > ? and verified = true AND pubkey_id = ?";
        String query = "SELECT timestamp,message_type,public_type from channelmessage WHERE pubkey_id =? AND timestamp > ? AND timestamp < ? ORDER BY timestamp ASC";
        PreparedStatement pstmt = Test.messageStore.getConnection().prepareStatement(query);
        pstmt.setInt(1, blockMsg.channel.getKey().database_id);
        long asd = blockMsg.timestamp - BlockMsg.TIME_TO_SYNC_BACK;
        System.out.println("time: " + asd);
        pstmt.setLong(2, asd);
        pstmt.setLong(3, blockMsg.timestamp);

        ResultSet executeQuery = pstmt.executeQuery();

        int msgcount = 0;

        boolean breaked = false;

        byte[] dataArray = new byte[1024 * 200];
        ByteBuffer buffer = ByteBuffer.wrap(dataArray); //max size of a message!! should be regulated later!

        //ToDo: add hash from all data and count of messages to get an easier sync!
        //System.out.println("dwzdzwd " + executeQuery.next());
        while (executeQuery.next()) {

            int message_type = executeQuery.getInt("message_type");
            long timestamp = executeQuery.getLong("timestamp");
            byte public_type = executeQuery.getByte("public_type");

            //only pack a message into a block if the public_type is 20!
            if (public_type == 20) {

                //skip content which will be generated regulary, image messages should not have public_type 20! (with new version, old imgs will be deleted)
                if (message_type != TextMsg.BYTE) {
                    continue;
                }
                if (buffer.remaining() < 8 + 4 + 4) {
                    System.out.println("buffer full, exit routine, dont know what to do atm");
                    breaked = true;
                    return null;
                }

                //System.out.println("Data: msgtyp: " + message_type + " pubtyp: " + public_type);
                buffer.putLong(timestamp);
                buffer.putInt(message_type);
                buffer.putInt(public_type);
                msgcount++;
            }

        }
        executeQuery.close();
        pstmt.close();

        if (breaked) {
            System.out.println("abort...");
            return null;
        }

        //generated msgcount and hash:
        int hash = Sha256Hash.create(dataArray).hashCode();

        return new HashAndCount(msgcount, hash);

    }

    private static class HashAndCount {

        int cnt;
        int hash;

        public HashAndCount(int cnt, int hash) {
            this.cnt = cnt;
            this.hash = hash;
        }

    }
}
