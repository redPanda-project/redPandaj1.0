/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redPandaLib.services;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.redPandaLib.Main;
import org.redPandaLib.core.*;
import org.redPandaLib.core.messages.RawMsg;

/**
 *
 * @author rflohr
 */
public class MessageDownloader {

    public static ArrayList<RawMsgEntry> requestedMsgs = new ArrayList<RawMsgEntry>();
    public static final ReentrantLock requestedMsgsLock = new ReentrantLock();
    private static boolean triggered = false;
    private static MyThread myThread = new MyThread();
    public static int MAX_REQUEST_PER_PEER = 10;
    private static boolean allowInterrupt = false;
    private static final ReentrantLock syncInterrupt = new ReentrantLock();
    public static int publicMsgsLoaded = 0;
    public static ReentrantLock publicMsgsLoadedLock = new ReentrantLock();
    public static int messagesToVerify = 0;
    private static Random random = new Random();
    public static long lastRun = 0;
    public static HashMap<Integer, Long> channelIdToLatestBlockTime = new HashMap<Integer, Long>();
    public static ReentrantLock channelIdToLatestBlockTimeLock = new ReentrantLock();
    public static int WAIT_FOR_OTHER_NODES_TO_INTRODUCE = 0;

    public static void trigger() {
        syncInterrupt.lock();
        if (allowInterrupt) {
            myThread.interrupt();
        } else {
            triggered = true;
        }
        syncInterrupt.unlock();
    }

    public static boolean isActive() {
        if (Test.localSettings == null) {
            return false;
        }

        if (Test.localSettings.PEX_ONLY) {
            return false;
        }

        return true;
    }

    public static void start() {
        myThread.start();

        new Thread() {

            @Override
            public void run() {

                final String orgName = Thread.currentThread().getName();
                Thread.currentThread().setName(orgName + " - MessageDownloader - decrease pubmsg");

                while (!Main.shutdown) {

                    try {
                        sleep(1000 * 60 * 60);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(MessageDownloader.class.getName()).log(Level.SEVERE, null, ex);
                    }

                    if (MessageDownloader.publicMsgsLoaded > 50) {
                        Log.put("not decreasing pubmsgs, msgs to verify: " + MessageDownloader.publicMsgsLoaded, 100);
                        continue;
                    }

                    publicMsgsLoaded -= Math.ceil(Settings.MAXPUBLICMSGS / 60) + 1;
                    if (publicMsgsLoaded < 0) {
                        publicMsgsLoaded = 0;
                    }

                    if (publicMsgsLoaded != 0) {
                        System.out.println("Decresed pubmsgs: " + publicMsgsLoaded);
                    }

                }
            }
        }.start();

    }

    public static class RawMsgEntry {

        public RawMsg m;
        public long requestedWhen = 0;
        public Peer requestedFromPeer;

        public RawMsgEntry(RawMsg m) {
            this.m = m;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof RawMsgEntry) {
                RawMsgEntry o = (RawMsgEntry) obj;
                return m.equals(o.m);
            }
            return false;
        }
    }

    static class MyThread extends Thread {

        @Override
        public void run() {

//
//            try {
//                sleep(2000);
//            } catch (InterruptedException ex) {
//            }
            final String orgName = Thread.currentThread().getName();
            Thread.currentThread().setName(orgName + " - MessageDownloader");
            while (!Main.shutdown) {

                lastRun = System.currentTimeMillis();

                try {

                    //System.out.println("new round");
                    boolean shortWait = false;

                    if (!isActive()) {
                        try {
                            sleep(5000);
                        } catch (InterruptedException ex) {
                        }
                        continue;
                    }

                    boolean requestedOne = false;

                    boolean soutedPublicMsgsMax = false;
                    triggered = false;
                    //                System.out.println("Scanning msgs i want to have...");
                    ArrayList<Peer> clonedPeerList = Test.getClonedPeerList();

                    Collections.sort(clonedPeerList, new Comparator<Peer>() {

                        @Override
                        public int compare(Peer t, Peer t1) {
                            return (t1.getMessageLoadedCount() - t.getMessageLoadedCount());
                        }
                    });

//                    System.out.println("loaded: " + clonedPeerList.get(0).getMessageLoadedCount());
                    //System.out.println("top: " + clonedPeerList.get(0).getMessageLoadedCount() + " low: " + clonedPeerList.get(clonedPeerList.size() - 1).getMessageLoadedCount());
                    for (Peer p : clonedPeerList) {

                        //if (!p.isConnected() || System.currentTimeMillis() - p.connectedSince < 500) {
                        if (!p.isConnected() || !p.isAuthed() || !p.isCryptedConnection()) {
                            continue;
                        }

                        if (p.requestedMsgs > MAX_REQUEST_PER_PEER || p.requestedMsgs > p.maxSimultaneousRequests || System.currentTimeMillis() - p.connectedSince < 1000 * 2) {
                            //if (System.currentTimeMillis() - p.connectedSince < 1000 * 2) {
                            shortWait = true;
                            Log.put("shortwait... reason: " + (p.requestedMsgs > MAX_REQUEST_PER_PEER) + " " + (p.requestedMsgs > p.maxSimultaneousRequests) + " " + (System.currentTimeMillis() - p.connectedSince < 1000 * 10), 2);
                            continue;
                        }

//                    //Damit der download erstmal nur sehr langsam pro peer laeuft...
//                    if (System.currentTimeMillis() - p.lastActionOnConnection < 350) {
//                        shortWait = true;
//                        continue;
//                    }
                        //System.out.println("asdzadgg " + Settings.MAXPUBLICMSGS + " > " + publicMsgsLoaded);
                        if (Settings.MAXPUBLICMSGS > publicMsgsLoaded) {
                            int maxLoadPub = Settings.MAXPUBLICMSGS - publicMsgsLoaded;

                            synchronized (p.getPendingMessages()) {
                                int i = 0;

                                HashMap<Integer, RawMsg> asdf = (HashMap<Integer, RawMsg>) p.getPendingMessagesPublic().clone();

                                //System.out.println("asdbashd " + asdf.size());
                                for (Entry<Integer, RawMsg> entry : asdf.entrySet()) {
                                    i++;
                                    if (i >= maxLoadPub) {
                                        break;
                                    }
                                    //System.out.println("MOVED");
                                    p.getPendingMessages().put(entry.getKey(), entry.getValue());
                                    p.getPendingMessagesPublic().remove(entry.getKey());
                                }
                            }

                        }

                        HashMap<Integer, RawMsg> hm;
                        synchronized (p.getPendingMessages()) {
                            hm = ((HashMap<Integer, RawMsg>) p.getPendingMessages().clone());
                        }

//                    System.out.println("MSG Downloader: " + p.ip + ":" + p.port + " PendingMsgs: " + hm.size() + " Requested: " + p.requestedMsgs);
                        int msgsRequestedThisCycle = 0;

                        for (Entry<Integer, RawMsg> entry : hm.entrySet()) {

                            //so not all MAX_REQUEST_PER_PEER are loaded from one peer.
                            if (msgsRequestedThisCycle > 10) {
                                shortWait = true;
                                //System.out.println("shortwait: msgsRequestedThisCycle");
                                break;
                            }

//                        int bufferWaiting = 0;
//                        synchronized (p.writeBuffer) {
//                            bufferWaiting = p.writeBuffer.position();
//                        }
//
//                        if (bufferWaiting > 5) {
//                            System.out.println("something in buffer, not requesting new msgs from this peer: " + p.getIp());
//                            shortWait = true;
//                            break;
//                        }
                            RawMsg m = entry.getValue();
                            int messageId = entry.getKey();
                            RawMsgEntry asEntryMsg = new RawMsgEntry(m);

//                        if (m.public_type == 1) {
//                            //stick!!
//
//                            System.out.println("Stick: What to do? " + myMessageId);
//
//                        }
                            int myMessageId = MessageHolder.contains(m);

                            if (p.getPeerTrustData() == null) {

                                Log.put("no trust data found, not downloading messages from node...", 80);
                                synchronized (p.getPendingMessages()) {
                                    p.getPendingMessages().remove(messageId);
                                }
                                //Log.put("|", 200);
                                continue;
                            }

                            if (p.getPeerTrustData() != null && myMessageId != -1) {

                                RawMsg rawMsg = MessageHolder.getRawMsg(myMessageId);

                                if (rawMsg == null) {
                                    System.out.println("contains message but no content? likely message was removed because of wrong signature.");
                                    continue;
                                }

                                if (!rawMsg.verified) {
                                    Log.put("message in db but not verified, check again next run", 80);
                                    continue;
                                }

                                boolean removed = Test.messageStore.removeMessageToSend(p.getPeerTrustData().internalId, myMessageId);
                                Log.put("removed msg: " + p.getPeerTrustData().internalId + " - " + myMessageId + " -- " + m.public_type + " - " + removed, 80);

                                if (!removed && !p.removedSendMessages.contains(myMessageId)) {
                                    Log.put("not removed.... sending to other node", 80);
                                    m.database_Id = myMessageId;
                                    m.key.database_id = Test.messageStore.getPubkeyId(m.key);
                                    p.writeMessage(m);
                                    p.removedSendMessages.add(myMessageId);
                                }
////                                if (!removed) {
////                                    Log.put("not removed.... sending to other node", 80);
////                                    m.database_Id = myMessageId;
////                                    m.key.database_id = Test.messageStore.getPubkeyId(m.key);
////                                    p.writeMessage(m);
////                                }

                            }

                            if (myMessageId != -1) {
                                synchronized (p.getPendingMessages()) {
                                    p.getPendingMessages().remove(messageId);
                                }
                                Log.put("|", 200);
                                continue;
                            }

                            //This is just a hack for low priority msgs, see explanation after definition of Settings.REDUCE_TRAFFIC!!!
                            if (Settings.REDUCE_TRAFFIC && m.public_type >= 100) {
                                synchronized (p.getPendingMessages()) {
                                    p.getPendingMessages().remove(messageId);
                                }
                                Log.put("removed message, reduce traffic!!!", 40);
                                continue;
                            }

                            Long latestBlockTime;
                            if (Settings.DONT_REMOVE_UNUSED_MESSAGES) {
                                latestBlockTime = Long.MIN_VALUE;
                            } else {
                                channelIdToLatestBlockTimeLock.lock();
                                latestBlockTime = channelIdToLatestBlockTime.get(m.key.database_id);
                                if (latestBlockTime == null) {
                                    System.out.println("search in db: " + m.key.database_id);
                                    latestBlockTime = Test.messageStore.getLatestBlocktime(m.key.database_id);
                                    channelIdToLatestBlockTime.put(m.key.database_id, latestBlockTime);
                                }
                                channelIdToLatestBlockTimeLock.unlock();
                            }

                            if (m.public_type == 20 && latestBlockTime > m.timestamp) {
                                synchronized (p.getPendingMessages()) {
                                    p.getPendingMessages().remove(messageId);
                                }

                                p.writeBufferLock.lock();
                                if (p.writeBuffer == null) {
                                    p.writeBufferLock.unlock();
                                    return;
                                }
                                //m.database_Id = messageId;
                                //m.key.database_id = Test.messageStore.getPubkeyId(m.key);
                                if (p.writeBuffer.remaining() < 1 + 4 + 1 + 8 + 4 + 4) {
                                    ByteBuffer oldbuffer = p.writeBuffer;
                                    p.writeBuffer = ByteBuffer.allocate(p.writeBuffer.capacity() + 50);
                                    p.writeBuffer.put(oldbuffer.array());
                                    p.writeBuffer.position(oldbuffer.position());
                                    System.out.println("writebuffer was raised...");
                                }

                                p.writeBuffer.put((byte) 5);
                                p.writeBuffer.putInt(m.key.database_id);
                                p.writeBuffer.put(m.public_type);
                                p.writeBuffer.putLong(m.timestamp);
                                p.writeBuffer.putInt(m.nonce);
                                p.writeBuffer.putInt(m.database_Id);//TODO long zu int machen mit offset falls db zu gross!!

                                p.writeBufferLock.unlock();

                                p.setWriteBufferFilled();

                                Log.put("removed message, already in block!!!", -30);
                                continue;
                            }

                            //STICKS and msgs and blocks and images
                            if (m.public_type == 0 || m.public_type == 20 || m.public_type == 21 || m.public_type > 50) {

                                //normal message
                                if (Settings.MAXPUBLICMSGS < publicMsgsLoaded || Settings.lightClient) {

//                                    if (!soutedPublicMsgsMax) {
//                                        System.out.println("Public msgs: " + publicMsgsLoaded + " MAX: " + Settings.MAXPUBLICMSGS);
//                                        soutedPublicMsgsMax = true;
//                                    }
                                    if (m.getChannel() == null) {
                                        //isPublic...
                                        if (Settings.SUPERNODE) {
                                            p.getPendingMessagesPublic().put(messageId, m);
                                        }
                                        synchronized (p.getPendingMessages()) {
                                            p.getPendingMessages().remove(messageId);
                                        }
                                        continue;
                                    }
                                }

                                requestedMsgsLock.lock();
                                //Checke ob schon geladen wird und ueberpruefe timeout, falls peer zu langsam, disconnected...
                                if (requestedMsgs.contains(asEntryMsg)) {
                                    //ToDo: lock for requestedMsgs ? not threadsafe currently, exception will be cautch = restart of loop (hack)
                                    RawMsgEntry get = requestedMsgs.get(requestedMsgs.indexOf(asEntryMsg));
                                    long delay = System.currentTimeMillis() - get.requestedWhen;
                                    Log.put("delay: " + delay + " ip: " + get.requestedFromPeer.getIp(), 15);
                                    if (delay < 20000L) {
                                        requestedMsgsLock.unlock();
                                        continue;
                                    }

                                    if (delay > Settings.pingTimeout * 1000L + 60000L) {
                                        System.out.println("MSG hard timeout... " + get.requestedFromPeer.getIp());
                                        requestedMsgs.remove(get);
                                        get.requestedFromPeer.getPendingMessages().remove(messageId);
                                        get.requestedFromPeer.getPendingMessagesTimedOut().put(messageId, get.m);
                                        //ToDo: timeout for timeoutmessages
                                    } else {

                                        System.out.println("MSG soft timeout... just requesting at another peer");
                                        if (get.requestedFromPeer == p) {
                                            System.out.println("already requested from this peer... " + p.nonce);
                                            requestedMsgsLock.unlock();
                                            continue;
                                        }
                                    }
                                }

//                                System.out.println("u23zu323");
                                if (!p.isConnected() || !p.isAuthed()) {
                                    System.out.println("break1243!!");
                                    break;
                                }

                                requestedOne = true;
                                //Nachricht soll geladen werden

                                requestedMsgs.add(asEntryMsg);
                                requestedMsgsLock.unlock();
                                asEntryMsg.requestedWhen = System.currentTimeMillis();
                                asEntryMsg.requestedFromPeer = p;

//                                ByteBuffer writeBuffer = p.writeBuffer;
                                p.writeBufferLock.lock();
                                p.writeBuffer.put((byte) 6);
                                p.writeBuffer.putInt(messageId);
                                p.writeBufferLock.unlock();

                                Thread.interrupted();

                                p.setWriteBufferFilled();

                                if (Thread.interrupted()) {
                                    System.out.println("bd2n8xnrgm63734r9mt34y349qx5qn5qn935nxc69q56t");
                                }

                                msgsRequestedThisCycle++;
                                Log.put(" " + p.requestedMsgs + " (" + p.ip + "-" + messageId + ") ", 0);
                                //System.out.println("requested... " + p.requestedMsgs);
//                        

//                        System.out.println("Nachricht download: " + p.ip + ":" + p.port + " MsgTimestamp: " + m.timestamp + " Requested: " + p.requestedMsgs);
                                p.requestedMsgs++;

                                if (p.requestedMsgs > MAX_REQUEST_PER_PEER || p.requestedMsgs > p.maxSimultaneousRequests) {
                                    break;
                                }

                            } else if (m.public_type == 1) {

                                synchronized (p.getPendingMessages()) {
                                    p.getPendingMessages().remove(messageId);
                                }

                                // found a stick...
                                //checking difficulty
                                Stick stick = new Stick(m.getKey().getPubKey(), m.timestamp, m.nonce);

                                double difficulty = stick.getDifficulty();

                                if (difficulty < Stick.DIFFICULTY) {
                                    System.out.println("Stick is invalid, not enough difficulty...");
                                } else {
                                    System.out.println("Stick found: " + difficulty);
                                    m.verified = true;
                                    RawMsg addMessage = MessageHolder.addMessage(m);
                                    Test.broadcastMsg(addMessage);
                                    Test.messageStore.addStick(m.key.database_id, messageId, difficulty, m.timestamp + 1000 * 60 * 60 * 24 * 7);
                                }

                            } else if (m.public_type == 15) {
                                //remove all msgs from that chan that are older then this message with
                                // public type 20 or greater 50
                                //removeMessagesFromChannel
                                //TODO WRONG PLACE HERE! implement in ConnectionHandler!
                            } else {
                                System.out.println("Message type not defined.... " + m.public_type);
                            }
                        }

                    }

                    if (!triggered) {
                        syncInterrupt.lock();
                        allowInterrupt = true;
                        syncInterrupt.unlock();
                        try {

                            //System.out.println("wait");
                            if (shortWait) {
                                sleep(500);
                            } else {
                                sleep(1000 * 60 * 5);
                            }

                        } catch (InterruptedException ex) {
                        }
                        syncInterrupt.lock();
                        allowInterrupt = false;
                        syncInterrupt.unlock();
                    }

                    //sleep, so we can wait for others nodes to introduce the same message (this reduces CPU usage, but delays messages a bit)
                    if (WAIT_FOR_OTHER_NODES_TO_INTRODUCE != 0) {
                        try {
                            sleep(WAIT_FOR_OTHER_NODES_TO_INTRODUCE);
                        } catch (InterruptedException ex) {
                        }
                    }
                    //clear interrupted flag, might called twice, so writeBuffer would think it was interrupted...
                    interrupted();

                    //Keine nachricht zum requesten, warte auf neue Nachricht, bzw timeout checken
//                if (!requestedOne) {
//                    while (!triggered) {
//                        try {
//                            sleep(1000 * 5);
//                        } catch (InterruptedException ex) {
//                        }
//
//                        //muss timeout geprueft werden?
////                        if (!requestedMsgs.isEmpty()) {
////                            break;
////                        }
//
//
//
//                    }
//
//                }
                } catch (Throwable e) {

                    System.out.println("MessageDownloader exception!!!!: ");
                    e.printStackTrace();
                    Test.sendStacktrace("SLEEP ! catched msg downloader exc.: \n", e);
                    try {
                        sleep(60000);
                    } catch (InterruptedException ex) {
                    }

                }
            }
        }

    }

    public static void removeRequestedMessage(RawMsg toRemove) {
        requestedMsgsLock.lock();
        MessageDownloader.RawMsgEntry asEntryMsg = new MessageDownloader.RawMsgEntry(toRemove);
        MessageDownloader.requestedMsgs.remove(asEntryMsg);
        requestedMsgsLock.unlock();
    }
}
