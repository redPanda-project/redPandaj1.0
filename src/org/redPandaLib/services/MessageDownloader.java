/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redPandaLib.services;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.redPandaLib.core.*;
import org.redPandaLib.core.messages.RawMsg;

/**
 *
 * @author rflohr
 */
public class MessageDownloader {

    private static ArrayList<RawMsgEntry> requestedMsgs = new ArrayList<RawMsgEntry>();
    private static boolean triggered = false;
    private static MyThread myThread = new MyThread();
    private static int MAX_REQUEST_PER_PEER = 30;
    private static boolean allowInterrupt = false;
    private static final String syncInterrupt = new String();
    public static int publicMsgsLoaded = 0;

    public static void trigger() {
        triggered = true;
        synchronized (syncInterrupt) {
            if (allowInterrupt) {
                myThread.interrupt();
            }
        }
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
                while (true) {
                    try {
                        sleep(1000 * 10);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(MessageDownloader.class.getName()).log(Level.SEVERE, null, ex);
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

    static class RawMsgEntry {

        RawMsg m;
        long requestedWhen = 0;
        Peer requestedFromPeer;

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


            while (true) {

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

                Collections.shuffle(clonedPeerList, new Random());

                for (Peer p : clonedPeerList) {

                    //if (!p.isConnected() || System.currentTimeMillis() - p.connectedSince < 500) {
                    if (!p.isConnected() || !p.isAuthed()) {
                        continue;
                    }




                    if (p.requestedMsgs > MAX_REQUEST_PER_PEER) {
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

                        if (MessageHolder.contains(m)) {
                            synchronized (p.getPendingMessages()) {
                                p.getPendingMessages().remove(messageId);
                            }
                            System.out.print("|");
                            continue;
                        }




                        //STICKS!!
                        if (m.public_type == 20 || m.public_type > 50) {

                            if (Settings.MAXPUBLICMSGS < publicMsgsLoaded) {

                                if (!soutedPublicMsgsMax) {
                                    System.out.println("Public msgs: " + publicMsgsLoaded + " MAX: " + Settings.MAXPUBLICMSGS);
                                    soutedPublicMsgsMax = true;
                                }



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


                            //Checke ob schon geladen wird und ueberpruefe timeout, falls peer zu langsam, disconnected...
                            if (requestedMsgs.contains(asEntryMsg)) {
                                RawMsgEntry get = requestedMsgs.get(requestedMsgs.indexOf(asEntryMsg));
                                long delay = System.currentTimeMillis() - get.requestedWhen;
                                if (delay < 6000L) {
                                    continue;
                                }

                                if (delay > 240000L) {
                                    System.out.println("MSG hard timeout... " + get.requestedFromPeer.getIp());
                                    requestedMsgs.remove(get);
                                } else {
                                    continue;
//                                    System.out.println("MSG soft timeout... just requesting at another peer");
//                                    if (get.requestedFromPeer == p) {
//                                        System.out.println("already requested from this peer...");
//                                        continue;
//                                    }
                                }
                            }


                            if (!p.isConnected() || !p.isAuthed()) {
                                break;
                            }


                            requestedOne = true;
                            //Nachricht soll geladen werden

                            requestedMsgs.add(asEntryMsg);
                            asEntryMsg.requestedWhen = System.currentTimeMillis();
                            asEntryMsg.requestedFromPeer = p;

                            ByteBuffer writeBuffer = p.writeBuffer;
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

                            System.out.println("requested... " + p.requestedMsgs);
//                        

//                        System.out.println("Nachricht download: " + p.ip + ":" + p.port + " MsgTimestamp: " + m.timestamp + " Requested: " + p.requestedMsgs);

                            p.requestedMsgs++;

                            if (p.requestedMsgs > MAX_REQUEST_PER_PEER) {
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
                                Test.broadcastMsg(addMessage);;
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

                synchronized (syncInterrupt) {
                    allowInterrupt = true;
                }
                try {

                    //System.out.println("wait");

                    if (shortWait) {
                        //sleep(100);
                    } else {
                        sleep(5000);
                    }

                } catch (InterruptedException ex) {
                }
                synchronized (syncInterrupt) {
                    allowInterrupt = false;
                }
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
            }
        }
    }
}