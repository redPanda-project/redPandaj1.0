/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redPandaLib.core;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.security.SecureRandom;
import java.security.Security;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLTransactionRollbackException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.redPandaLib.ChannelisNotWriteableException;
import org.redPandaLib.ImageTooLargeException;
import org.redPandaLib.Main;
import org.redPandaLib.NewMessageListener;
import org.redPandaLib.SpecialChannels;
import org.redPandaLib.core.messages.BlockMsg;
import org.redPandaLib.core.messages.DeliveredMsg;
import org.redPandaLib.core.messages.ImageMsg;
import org.redPandaLib.core.messages.InfoMsg;
import org.redPandaLib.core.messages.RawMsg;
import org.redPandaLib.core.messages.TextMessageContent;
import org.redPandaLib.core.messages.TextMsg;
import org.redPandaLib.crypt.AddressFormatException;
import org.redPandaLib.crypt.Base58;
import org.redPandaLib.crypt.ECKey;
import org.redPandaLib.crypt.Sha256Hash;
import org.redPandaLib.crypt.Utils;
import org.redPandaLib.database.DirectMessageStore;
import org.redPandaLib.database.HsqlConnection;
import org.redPandaLib.database.MessageStore;
import org.redPandaLib.services.ClusterBuilder;
import org.redPandaLib.services.LoadHistory;
import org.redPandaLib.services.MessageDownloader;
import org.redPandaLib.services.MessageVerifierHsqlDb;
import org.redPandaLib.services.SearchLan;
import org.redPandaLib.services.WatchDog;
//import org.redPandaLib.upnp.Portforward;

/**
 *
 * @author rflohr
 */
public class Test {

    static boolean DEBUG = true;
    static int DEBUG_LEVEL = 100;
    static boolean PORTFORWARD = false;
    static final int VERSION = 20;
    public static int MY_PORT;
    static String MAGIC = "k3gV";
    public static ArrayList<Peer> peerList = null;
    public static ArrayList<Channel> channels;
    public static long NONCE;
    static int outConns = 0;
    static int inConns = 0;
    //static ExecutorService threadPool = Executors.newCachedThreadPool();
    //static ExecutorService threadPool = Executors.newFixedThreadPool(MAX_CONNECTIONS * 2 + 5);
    //static ExecutorService threadPool2 = Executors.newCachedThreadPool();
    //static ExecutorService threadPool3 = Executors.newCachedThreadPool();
    public static Random random = new SecureRandom();
    public static String clientSeed;
    public static int clientVersion = 0;
    public static long outBytes = 0;
    public static long inBytes = 0;
    public static SaverInterface saver;
    private static long lastAllMsgsCleared = 0;
    public static LocalSettings localSettings;
    public static ConnectionHandler connectionHandler;
    private static ConnectionHandlerConnect connectionHandlerConnect;
    public static boolean NAT_OPEN = false;
    public static ArrayList<PeerTrustData> peerTrusts = new ArrayList<PeerTrustData>();
    public static MessageStore messageStore;
    public static String imageStoreFolder = "images/";
    public static String stackTraceString = "";
    public static long lastSentStackTrace = 0;
    public static ImageInfos imageInfos = new ImageInfosImageIO();
    public static Outboundthread outboundthread;

    static {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
        //Security.insertProviderAt(new org.bouncycastle.jce.provider.BouncyCastleProvider(), 2);
    }
    public static boolean STARTED_UP_SUCCESSFUL = false;
    private static long lastAddedKnownNodes;
    public static HsqlConnection hsqlConnection;

    /**
     * @param args the command line arguments
     */
    public static void main(boolean listenConsole, SaverInterface saver) throws IOException {

        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {

            @Override
            public void uncaughtException(Thread t, Throwable e) {
                System.out.println("uncaught exception!");
                e.printStackTrace();
                sendStacktrace(e);

                new Thread() {

                    @Override
                    public void run() {
                        try {
                            Thread.sleep(1000 * 60 * 10);
                        } catch (InterruptedException ex) {
                            Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
                        }

                        System.exit(-1);
                    }

                }.start();

            }
        });

//        new Thread() {
//
//            @Override
//            public void run() {
//
//                //ToDo: remove later...
//                while (true) {
//                    try {
//                        sleep(2000);
//                    } catch (InterruptedException ex) {
//                        Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
//                    }
//
//                    try {
//                        Class.forName("java.lang.management.ManagementFactory");
//                    } catch (ClassNotFoundException e) {
//                        return;
//                    }
//
//                    ThreadMXBean tmb = ManagementFactory.getThreadMXBean();
//                    long[] ids = tmb.findDeadlockedThreads();
//                    if (ids == null) {
//                        continue;
//                    }
//                    System.out.println("first id:" + ids[0]);
//
//                    ThreadInfo[] infos = tmb.getThreadInfo(ids);
//
//                    for (ThreadInfo info : infos) {
//                        System.out.println("Name: " + info.getThreadName());
//                    }
//
//                }
//
//            }
//
//        }.start();
        Test.saver = saver;

        //loadChannels();
        byte[] bytes = new byte[10];
        random.nextBytes(bytes);
        clientSeed = bytes.toString();

        localSettings = saver.loadLocalSettings();
        NONCE = localSettings.nonce;
        localSettings.identity2Name.put(-3317663402085509703L, "pY4x3g");
        localSettings.save();
        loadChannels();

        if (DEBUG) {
            System.out.println("Channels loaded...");
        }

        connectionHandler = new ConnectionHandler();
        connectionHandler.start();

//        connectionHandlerConnect = new ConnectionHandlerConnect();
//        connectionHandlerConnect.start();
        //threadPool.submit(new InboundThread());
        new InboundThread().start();
        //new Outboundthread().start();
        InputStreamReader inputStreamReader = new InputStreamReader(System.in, "UTF-8");
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        //cleanup
        Thread thread = new Thread() {

            @Override
            public void run() {

                final String orgName = Thread.currentThread().getName();
                Thread.currentThread().setName(orgName + " - ChronJobs for peer communication");

                long lastSaved = System.currentTimeMillis();

                while (!Main.shutdown) {

                    if (System.currentTimeMillis() - lastSaved > 120 * 1000) {

                        if (ConnectionHandler.allSockets.size() > 10) {
                            ConnectionHandler.removeUnusedSockets();
                        }
                        long ctime = System.currentTimeMillis();
                        //clean up trust data:
                        for (PeerTrustData ptd : (ArrayList<PeerTrustData>) peerTrusts.clone()) {
                            if (ctime - ptd.lastSeen > 1000 * 60 * 60 * 24 * 7) {
                                peerTrusts.remove(ptd);
                                messageStore.clearFilterChannel(ptd.internalId);
                                messageStore.removeMessageToSend(ptd.internalId);
                                System.out.println("remove peer trust, last seen over one week....");
                            }
                        }

                        try {
                            savePeers();
                            saveTrustData();
                            commitDatabase();
                            lastSaved = System.currentTimeMillis();
                        } catch (Exception e) {
                            Log.put("oh oh, konnte peers nicht speichern... ", 70);
                            //e.printStackTrace();
                        }
                    }

                    try {
                        sleep(Settings.pingDelay * 1000 + random.nextInt(200));
                        //sleep(2 * 1000 + random.nextInt(200));
                    } catch (InterruptedException ex) {
                        Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
                    }

                    if (peerList == null) {
                        continue;
                    }

                    //                    synchronized (peerList) {
                    for (Peer p : (ArrayList<Peer>) peerList.clone()) {

                        //ToDo: remove
                        try {
                            sleep(500);
                        } catch (InterruptedException ex) {
                            Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
                        }

                        if (p.getLastAnswered() > 1000 * 60 * 60 * 24 * 7 && peerList.size() > 3 && p.lastActionOnConnection != 0) {
                            removePeer(p);
                        }

                        if (p.lastPinged - p.lastActionOnConnection > Settings.pingDelay * 1000 * 2 + 30000
                                || (p.isConnecting && p.getLastAnswered() > 10000)
                                || (!p.isFullConnected() && p.getLastAnswered() > Settings.pingDelay * 1000)) {

                            if (p.isConnected() || p.isConnecting) {
                                if (DEBUG) {
                                    Log.put(Settings.pingTimeout + " sec timeout reached! " + p.ip, 10);
                                }
                                p.disconnect("timeout");
                                if (p.nonce == 0) {
                                    Log.put("removed peer from peerList, tried once and peer never connected before: " + p.ip + ":" + p.port, 20);
                                }

                                outboundthread.tryInterrupt();
                            } else if (p.getLastAnswered() > Settings.pingTimeout * 1000 * 2) {
                                p.writeBuffer = null;
                                p.readBuffer = null;
                                p.readBufferCrypted = null;
                                p.writeBufferCrypted = null;
                            }

                        } else if (p.isConnected()) {

                            //                                System.out.println("Pinging: " + p.nonce);
                            //p.ping();
                            p.cnt++;
                            if (p.cnt > Settings.peerListRequestDelay * 1000 / (Settings.pingDelay * 1000)) {
                                //p.connectionThread.writeString(ConnectionThread.GETPEERS);

                                if (p.isFullConnected()) {

                                    synchronized (p.writeBuffer) {
                                        if (p.writeBuffer.remaining() == 0) {
                                            System.out.println("Konnte peers nicht abfragen, buffer voll.");
                                        } else {
                                            p.writeBufferLock.lock();
                                            p.writeBuffer.put((byte) 1);
                                            p.writeBufferLock.unlock();
                                        }
                                    }
                                }

                                p.cnt = 0;
                            } else {

//                                    if (p.isFullConnected()) {
//
//                                        System.out.println("PING?: " + p.getLastAnswered() + " > " + Settings.pingDelay * 1000);
//
//                                    }
                                if (p.isFullConnected() && p.getLastAnswered() > Settings.pingDelay * 1000) {
                                    p.ping();
                                }

                                if (p.isConnected() && !p.authed) {

                                    p.trustRetries++;

                                    if (p.trustRetries < 2) {
                                        //                                            System.out.println("Found a bad guy... doing nothing...: " + p.nonce);
                                    } else {
                                        p.trustRetries = 0;
                                        System.out.println("Found a bad guy... requesting new key: " + p.nonce);
                                        //ConnectionHandler.sendNewAuthKey(p);
                                        p.disconnect("not authed...");
                                    }

                                    //System.out.println("Found a bad guy... requesting new key: " + p.nonce);
                                    //ConnectionHandler.sendNewAuthKey(p);
                                    //p.disconnect();
                                }

                            }
                        }

                        //                        }
                    }
                }
            }
        };
//
//        thread.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
//
//            @Override
//            public void uncaughtException(Thread t, Throwable e) {
//                sendStacktrace(e);
//
//                throw new RuntimeException("PING thread died....");
//            }
//        });

        thread.start();

//        threadPool.submit(
//                new Thread() {
//
//                    @Override
//                    public void run() {
//                        super.run();
//                        while (peer.getLastAnswered() < 20000) {
//
//
//
//                            try {
//                                sleep(1000);
//                            } catch (InterruptedException ex) {
//                                Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
//                            }
//
//                            finalPrintWriter.write("ping\ngetPeers\n");
//                            finalPrintWriter.flush();
//
//                            //Random disconnect to test stability
//                            if (new Random().nextInt(2 * 120) == 0) {
//                                peer.lastAnswer = 0;
//                            }
//                        }
//                        try {
//                            socket.close();
//                        } catch (IOException ex) {
//                            Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
//                        }
//
//                    }
//                });
        try {
            Runtime.getRuntime().addShutdownHook(new Thread() {

                @Override
                public void run() {
                    final String orgName = Thread.currentThread().getName();
                    Thread.currentThread().setName(orgName + " - shutdownhook");
                    System.out.println("started shutdownhook...");
                    Main.shutdown();
                    System.out.println("shutdownhook done");
                }
            });
        } catch (IllegalStateException e) {
            //could not set shutdownhook, sailfish OS: VM already shutting down?
        }

        System.out.println("shutdownhook added...");

        //ToDo: remove
        while (peerList == null) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
            }
        }

        try {
            PreparedStatement prepareStatement = messageStore.getConnection().prepareStatement("SELECT COUNT(message_id) FROM haveToSendMessageToPeer");
            ResultSet executeQuery = prepareStatement.executeQuery();
            executeQuery.next();

            int a = executeQuery.getInt(1);

            //ToDo: remove or raise later...
            if (a > 200000) {
                Main.sendBroadCastMsg("MessagesToSync too big, truncating both tables: " + a);
                PreparedStatement prepareStatement2 = messageStore.getConnection().prepareStatement("TRUNCATE table haveToSendMessageToPeer");
                prepareStatement2.execute();
                prepareStatement2.close();

                prepareStatement2 = messageStore.getConnection().prepareStatement("TRUNCATE table filterChannels");
                prepareStatement2.execute();
                prepareStatement2.close();

            }

            executeQuery.close();
            prepareStatement.close();
        } catch (SQLException ex) {
        }

        if (listenConsole) {
            while (!Main.shutdown) {
                String readLine = bufferedReader.readLine();

                if (peerList == null) {
                    continue;
                }

                if (readLine.equals("")) {

                    System.out.println("Status listenPort: " + MY_PORT + " NONCE: " + Test.NONCE + "\n");

                    int actCons = 0;

                    ArrayList<Peer> list = (ArrayList<Peer>) peerList.clone();
                    Collections.sort(list);

//                    System.out.println("IP:PORT \t\t\t\t\t\t Nonce \t\t\t Last Answer \t Alive \t retries \t LoadedMsgs \t Ping \t Authed \t PMSG\n");
                    System.out.format("%50s %22s %12s %12s %7s %8s %10s %10s %10s %8s %10s %10s %10s\n", "[IP]:PORT", "nonce", "last answer", "conntected", "retries", "ping", "loaded Msg", "bytes out", "bytes in", "bad Msg", "ToSyncM", "RSM", "Rating");
                    for (Peer peer : list) {

                        if (peer.isConnected() && peer.authed && peer.writeBufferCrypted != null) {
                            actCons++;
                        }

                        //System.out.println("Peer: " + InetAddress.getByName(peer.ip) + ":" + peer.port + " Nonce: " + peer.nonce + " Last Answer: " + (System.currentTimeMillis() - peer.lastActionOnConnection) + " Alive: " + peer.isConnected() + " LastGetAllMsgs: " + peer.lastAllMsgsQuerried + " retries: " + peer.retries + " LoadedMsgs: " + peer.loadedMsgs + " ping: " + (Math.round(peer.ping * 100) / 100.));
                        String c;
                        if (peer.lastActionOnConnection != 0) {
                            c = "" + (System.currentTimeMillis() - peer.lastActionOnConnection);
                        } else {
                            c = "-";
                        }

                        if (peer.getPeerTrustData() == null) {
                            System.out.format("%50s %22d %12s %12s %7d %8s %10s %10d %10d %10d\n", "[" + peer.ip + "]:" + peer.port, peer.nonce, c, "" + peer.isConnected() + "/" + (peer.authed && peer.writeBufferCrypted != null), peer.retries, (Math.round(peer.ping * 100) / 100.), "-", peer.sendBytes, peer.receivedBytes, peer.removedSendMessages.size());
                        } else {
                            System.out.format("%50s %22d %12s %12s %7d %8s %10d %10d %10d %8s %10d %10d %10s %10s %10s\n", "[" + peer.ip + "]:" + peer.port, peer.nonce, c, "" + peer.isConnected() + "/" + (peer.authed && peer.writeBufferCrypted != null), peer.retries, (Math.round(peer.ping * 100) / 100.), peer.getPeerTrustData().loadedMsgs.size(), peer.sendBytes, peer.receivedBytes, peer.getPeerTrustData().badMessages, messagesToSync(peer.peerTrustData.internalId), peer.removedSendMessages.size(),
                                    //peer.peerTrustData.backSyncedTill == Long.MAX_VALUE ? "-" : formatInterval(System.currentTimeMillis() - peer.peerTrustData.backSyncedTill),
                                    peer.peerTrustData.rating,
                                    peer.peerTrustData.pendingMessagesTimedOut.size(), peer.peerTrustData.pendingMessagesTimedOut.size());
                        }

//                        while (c.length() < 15) {
//                            c += " \t";
//                        }
//                        if (peer.getPeerTrustData() == null) {
//                            output += "" + a + " \t " + b + "\t " + c + "\t " +  + "\t " + peer.retries + "\t " + "--" + " \t " + (Math.round(peer.ping * 100) / 100.) + "\t " + peer.authed + "\t " + "--" + " \t" + peer.requestedMsgs + " \t" + "--" + "\n";
//                        } else {
//                            output += "" + a + " \t " + b + "\t " + c + "\t " + peer.isConnected() + "\t " + peer.retries + "\t " + peer.getLoadedMsgs().size() + " \t " + (Math.round(peer.ping * 100) / 100.) + "\t " + peer.authed + "\t " + peer.getPendingMessages().size() + " \t" + peer.requestedMsgs + " \t" + peer.getPeerTrustData().synchronizedMessages + "\n";
//                        }
                    }

                    System.out.println("Not connected trust data:");

                    ArrayList<Peer> clonedPeerList = getClonedPeerList();

                    System.out.format("%12s %25s %12s %12s\n", "ID", "Last Seen", "SyncedMsgs", "ToSync");

                    ArrayList<PeerTrustData> peerTrustsCloned = (ArrayList<PeerTrustData>) peerTrusts.clone();

                    Collections.sort(peerTrustsCloned, new Comparator<PeerTrustData>() {

                        @Override
                        public int compare(PeerTrustData o1, PeerTrustData o2) {
                            return (int) (o2.lastSeen - o1.lastSeen);
                        }
                    });

                    for (PeerTrustData ptd : peerTrustsCloned) {

                        boolean found = false;
                        for (Peer p : clonedPeerList) {
                            if (p.isFullConnected() && p.peerTrustData == ptd) {
                                found = true;
                                break;
                            }
                        }

                        if (found) {
                            continue;
                        }

                        int messagesToSync = messagesToSync(ptd.internalId);

                        System.out.format("%12d %25s %12d %12d\n", ptd.internalId, formatInterval(System.currentTimeMillis() - ptd.lastSeen), ptd.synchronizedMessages, messagesToSync);

                    }

                    System.out.println("Connected to " + actCons + " peers. (NAT type: " + (NAT_OPEN ? "open" : "closed") + ")");
                    System.out.println("Traffic: " + inBytes / 1024. + " kb / " + outBytes / 1024. + " kb.");

                    System.out.println("Services last run: ConnectionHandler: " + (System.currentTimeMillis() - ConnectionHandler.lastRun) + " MessageDownloader: " + (System.currentTimeMillis() - MessageDownloader.lastRun) + " MessageVerifierHsqlDb: " + (System.currentTimeMillis() - MessageVerifierHsqlDb.lastRun));

                    //System.out.println("Processed messages: " + msgs.size());
//                    int unverifiedMsgs = 0;
//
//                    for (RawMsg m : MessageHolder.getAllNotVerifiedMessages()) {
//                        if (m.verified) {
//                            continue;
//                        }
//                        unverifiedMsgs++;
//                    }
//
                    //System.out.println("Saved Sockets: " + ConnectionHandler.allSockets.size());
                    new Thread() {

                        @Override
                        public void run() {
//                            try {
//                                Test.messageStore.getConnection().close();
//                            } catch (SQLException ex) {
//                                Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
//                            }
//                            Main.useHsqlDatabase();
                            System.out.println("Processed messages: " + MessageHolder.getMessageCount() + " - Queue to verify: " + MessageHolder.getMessageCountToVerify());

                            //MessageVerifierHsqlDb.sem.release();
                        }
                    }.start();

//                    System.out.println("reconnect!!");
//                    hsqlConnection.reconnect();
                    continue;
                }

                if (readLine.equals("v0")) {
                    MessageDownloader.publicMsgsLoaded = 0;
                }

                if (readLine.equals("T")) {
                    for (PeerTrustData ptd : Test.peerTrusts) {
                        System.out.println("PEER: " + ptd.nonce + " " + (System.currentTimeMillis() - ptd.lastSeen));
                        for (String ip : ptd.ips) {
                            System.out.println("        IP: " + ip);
                        }

                    }

                    continue;
                }

                if (readLine.equals("cs")) {
                    ConnectionHandler.removeUnusedSockets();
                    continue;
                }

                if (readLine.equals("T1")) {
                    ThreadMXBean tmb = ManagementFactory.getThreadMXBean();
                    long[] ids = tmb.findDeadlockedThreads();
                    ThreadInfo[] infos = tmb.getThreadInfo(ids);

                    for (ThreadInfo info : infos) {
                        System.out.println("Name: " + info.getThreadName());
                    }

                    continue;
                }

                if (readLine.equals("throw ex")) {
                    throw new RuntimeException("test exception thrown...");
                }

                if (readLine.equals("t")) {
                    messageStore.showTablePubkey();
                    //messageStore.showTableMessage();
                    //messageStore.showTableMessageContent();
//                    ArrayList<TextMessageContent> messages = getMessages(Channel.getChannelById(-2));
//
//                    for (TextMessageContent t : messages) {
//                        System.out.println("" + new String(t.decryptedContent));
//                    }

                    continue;
                }

                if (readLine.equals("tD")) {
                    for (Peer peer : getClonedPeerList()) {
                        System.out.println("Peer: " + peer.ip + ":" + peer.port);
                        if (peer.getPeerTrustData() == null) {
                            continue;
                        }
                        for (Entry<Integer, ECKey> a : peer.getKeyToIdHis().entrySet()) {
                            System.out.println("keyToId - id: " + a.getKey() + " key: " + Channel.byte2String(a.getValue().getPubKey()));
                        }
                    }
                    continue;
                }

                if (readLine.equals("M")) {
                    Main.addMainChannel();
                    continue;
                }

                if (readLine.equals("db33")) {
                    try {
                        PreparedStatement pstmt = messageStore.getConnection().prepareStatement("SELECT * FROM INFORMATION_SCHEMA.SYSTEM_SESSIONS");
                        ResultSet executeQuery = pstmt.executeQuery();
                        while (executeQuery.next()) {
                            String string = executeQuery.getString("CURRENT_STATEMENT");
                            System.out.println("asdf " + string);
                        }
                        pstmt.close();
                    } catch (SQLException ex) {
                        Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
                    }

                    continue;
                }

                if (readLine.equals("rm")) {
                    System.out.println("channel id? - remove msg from channel");
                    try {
                        int chanid = Integer.parseInt(bufferedReader.readLine());
                        Channel toRem = null;
                        for (Channel c : channels) {
                            if (c.id == chanid) {
                                toRem = c;
                                break;
                            }
                        }

                        if (toRem == null) {
                            System.out.println("channel not found.");
                        } else {
                            Main.removeMessagesForChannel(toRem);
                            System.out.println("done");
                        }

                    } catch (NumberFormatException e) {
                        System.out.println("No integer.");
                    }
                    continue;
                }

                if (readLine.equals("bt1")) {
                    System.out.println("asd  " + Test.messageStore.getLatestBlocktime(19));
                    continue;
                }
                
                if (readLine.equals("rM")) {
                    channels.remove(SpecialChannels.MAIN);
                    Test.saver.saveIdentities(channels);
                    continue;
                }

                if (readLine.equals("tt")) {
                    messageStore.quit();
                    continue;
                }

                if (readLine.equals("u")) {
                    System.out.println("Updating...");
//                    ProcessBuilder processBuilder = new ProcessBuilder("echo hallo");
//                    processBuilder.directory(new File("/home/rflohr/tmp/redPanda"));
//                    processBuilder.start();
                    //new ProcessBuilder("./rp.sh").directory(new File("/home/rflohr/tmp/redPanda")).start();
                    Process start = new ProcessBuilder("./update.sh").start();
                    try {
                        start.waitFor();
                        System.out.println("update successful, please start the jar again...");
                        System.exit(22);
                    } catch (InterruptedException ex) {
                        System.out.println("update error...");
                    }

                    continue;
                }

//                if (readLine.charAt(0) == '.') {
//                    writeAll(readLine + "\n");
//                    continue;
//                }
                if (readLine.equals("!")) {

//                    if (readLine.charAt(1) == 'i' && readLine.charAt(2) == ':') {
//
//                    System.out.println("trying to add priv key...");
//
//                    String substring = readLine.substring(3, readLine.length());
//                    Identity instanceByPrivateKey = Identity.getInstanceByPrivateKey(substring);
//
//                    identities.add(instanceByPrivateKey);
//
//                    System.out.println("Identities: " + identities.size());
//                    }
                    Channel channel = SpecialChannels.SPAM;
                    for (int i = 0; i < 100; i++) {

                        String msgLong = "";

                        for (int k = 0; k < 30; k++) {
                            msgLong += "duznx4273nx42834tn2384tn2c8t4cn28t428ct4n28t4nc283ctn283ct4nduznx4273nx42834tn2384tn2c8t4cn28t428ct4n28t4nc283ctn283ct4n";
                        }

                        TextMsg build = TextMsg.build(channel, msgLong + i);
                        RawMsg addMessage = MessageHolder.addMessage(build);
                        broadcastMsg(addMessage);
                        System.out.println("send...");
                    }
                    continue;
                }

                if (readLine.equals("I")) {

                    Main.addSpamChannel();

                    System.out.println("image send file img.jpg");
                    try {
                        Main.sendImageToChannel(SpecialChannels.SPAM, "img.jpg", true);
                    } catch (ImageTooLargeException ex) {
                        Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (ChannelisNotWriteableException ex) {
                        Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
                    }

                    continue;
                }

                if (readLine.equals("i")) {
                    System.out.println("Import address:");
                    String chanKey = bufferedReader.readLine();
                    System.out.println("Name:");
                    String chanName = bufferedReader.readLine();
                    try {
                        Channel importChannelFromHuman = Main.importChannelFromHuman(chanKey, chanName);

                        if (importChannelFromHuman == null) {
                            System.out.println("schon vorhanden.");
                        } else {
                            System.out.println("done");
                        }

                    } catch (AddressFormatException e) {
                        System.out.println("Key falsch.");
                    }

//                    try {
//                        Channel channel = Channel.importFromHuman(chanKey, chanName);
//                        channel.id = Channel.getNextId();
//                        boolean added = Test.addChannel(channel);
//                        if (added) {
//                            System.out.println("OK.");
//                        } else {
//                            System.out.println("Channel already in list.");
//                        }
//                    } catch (AddressFormatException ex) {
//                        Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
//                        System.out.println("Channel not parseable...");
//                    }
                    continue;
                }

                if (readLine.equals("s")) {

                    for (Channel c : Main.getChannels()) {
                        System.out.println("" + c.getName());
                    }

                    System.out.println("ChannelList: ");
                    for (Channel c : channels) {
                        if (c == null) {
                            System.out.println("dhuweghnueiwnru");
                        }
                        if (c.pub) {
                            System.out.println("#" + c.getId() + " \t " + c.getName() + " \t - " + c.exportForHumans() + " (pub: " + Channel.byte2String(c.getKey().getPubKey()) + " - " + Utils.bytesToHexString(c.getKey().getPubKey()) + " priv: NOT AVAILABLE)");
                        } else {
                            System.out.println("#" + c.getId() + " \t " + c.getName() + " \t - " + c.exportForHumans() + " (pub: " + Channel.byte2String(c.getKey().getPubKey()) + " - " + Utils.bytesToHexString(c.getKey().getPubKey()) + " priv: " + Channel.byte2String(c.getKey().getPrivKeyBytes()) + ")");
                        }
                    }
                    System.out.println("send message to channel number:");
                    readLine = bufferedReader.readLine();

                    int channelNumber;
                    try {
                        channelNumber = Integer.parseInt(readLine);
                    } catch (NumberFormatException e) {
                        System.out.println("No number, aborting...");
                        continue;
                    }

                    Channel channel = Channel.getChannelById(channelNumber);

                    if (channel == null) {
                        System.out.println("Number not found, aborting...");
                        continue;
                    }

                    if (channel.key.getPrivKeyBytes() == null) {
                        System.out.println("Channel has no private signing key, cant write to the channel!");
                        continue;
                    }
                    //ArrayList<TextMessageContent> messages = MessageHolder.getMessages(channel);

                    ArrayList<TextMessageContent> messages = Main.getMessages(channel, System.currentTimeMillis() - 1000L * 60L * 60L * 24L * 30L, System.currentTimeMillis());

                    for (TextMessageContent msg : messages) {
                        System.out.println("from me: " + msg.isFromMe() + " content: " + msg.getText());
                    }

                    System.out.println("Ok, writing to channel: " + channel.getName() + " EXPORT: " + channel.exportForHumans() + "\nContent:");

                    readLine = bufferedReader.readLine().replaceAll("#n", "\n");;
                    try {
                        Main.sendMessageToChannel(channel, readLine);
                    } catch (ChannelisNotWriteableException e) {
                    }
//                    TextMsg build = TextMsg.build(channel, readLine);
//                    MessageHolder.addMessage(build);
//                    broadcastMsg(build);

                    System.out.println("send...");

                    //Channel instaceByPrivateKey = Channel.getInstaceByPrivateKey(readLine, "unknown", Channel.getNextId());
//                    if (instaceByPrivateKey == null) {
//                        System.out.println("Channelkey looks wrong...");
//                        continue;
//                    }
//
//                    if (!channels.contains(instaceByPrivateKey)) {
//                        channels.add(instaceByPrivateKey);
//                    }
//
//                    System.out.println("Content:");
//                    readLine = bufferedReader.readLine();
//                    clientVersion++;
//
//                    RawMsg rawMsg = new RawMsg(instaceByPrivateKey.getKey(), System.currentTimeMillis(), 88);
//                    rawMsg.signature = new byte[32];
//                    rawMsg.content = readLine.getBytes();
//                    MessageHolder.addMessage(rawMsg);
//
//                    broadcastMsg(rawMsg);
                    //Msg msg = new Msg(System.currentTimeMillis(), 88, instaceByPrivateKey, clientSeed, clientVersion, "[" + getNick() + "] " + readLine);
                    //processNewMessage(msg, true);
                    continue;
                }

                if (readLine.equals("L")) {
                    SearchLan.searchLan();
                    continue;
                }

                if (readLine.equals("B")) {

                    System.out.println("found peers:");

                    for (Peer p : (ArrayList<Peer>) peerList.clone()) {
                        if (p.isAuthed() && p.isConnected() && p.syncMessagesSince == 0) {
                            System.out.println("IP: " + p.ip + " nonce: " + p.nonce);
                        }
                    }

                    LoadHistory.sw();

                    System.out.println("switched...");

                    continue;
                }

                if (readLine.equals("check2")) {
                    try {
                        Statement stmt = messageStore.getConnection().createStatement();
                        ResultSet rs = stmt.executeQuery("SELECT* FROM INFORMATION_SCHEMA.SYSTEM_SESSIONS");

                        int columnCount = rs.getMetaData().getColumnCount();

                        while (rs.next()) {
                            //String[] row = new String[columnCount];
                            for (int i = 0; i < columnCount; i++) {
                                //row[i] = rs.getString(i + 1);
                                System.out.println(rs.getMetaData().getColumnName(i + 1) + " - " + rs.getString(i + 1));
                            }
                            //result.add(row);
                        }

                    } catch (SQLException ex) {
                        Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    continue;
                }

                if (readLine.equals("check")) {
                    try {
                        Statement stmt = messageStore.getConnection().createStatement();
                        stmt.execute("CHECKPOINT");
                    } catch (SQLException ex) {
                        Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    continue;
                }

                if (readLine.equals("S")) {
                    System.out.println("started mining...");
                    StickMiner.start();
                    continue;
                }

                if (readLine.equals("xs")) {
                    System.out.println("save xml, use password:");
                    readLine = bufferedReader.readLine();
                    boolean backup = Main.backup("backup.xml", readLine);
                    if (backup) {
                        System.out.println("succesful");
                    } else {
                        System.out.println("failed");
                    }
                    continue;
                }

                if (readLine.equals("xl")) {
                    System.out.println("load xml, use password:");
                    readLine = bufferedReader.readLine();
                    boolean backup = Main.restoreBackup("backup.xml", readLine);
                    if (backup) {
                        System.out.println("succesful");
                    } else {
                        System.out.println("failed");
                    }
                    continue;
                }

                if (readLine.equals("N")) {
                    System.out.println("Add a new Channel, name?");
                    readLine = bufferedReader.readLine();
                    if (readLine == "") {
                        System.out.println("abort...");
                    } else {
                        Main.addChannel(Channel.generateNew(readLine));
                    }
                    continue;
                }

                if (readLine.equals("R")) {
                    System.out.println("removing all old data....");
                    Main.removeOldMessages();
                    System.out.println("crypted done, next decrypted...");
                    Main.removeOldMessagesDecryptedContent();
                    System.out.println("REMOVED ALL OLD DATA.");
                    continue;
                }

                if (readLine.equals("r")) {
                    System.out.println("removing all peers + trust data...");
                    synchronized (peerList) {
                        ArrayList<Peer> peers = peerList;
                        peerList = new ArrayList<Peer>();
                        peerTrusts = new ArrayList<PeerTrustData>();
                        saver.saveTrustedPeers(peerTrusts);
                        saver.savePeerss(peers);
                        for (Peer peer : peers) {
                            peer.disconnect("r");
                        }
                    }
                    continue;
                }

                if (readLine.equals("sticks")) {

                    try {
                        //get Key Id
                        String query = "SELECT pubkey_id,message_id,difficulty,validTill from sticks";

                        System.out.println("QUERY: " + query);

                        PreparedStatement pstmt = messageStore.getConnection().prepareStatement(query);
                        System.out.println("STM: " + pstmt.toString());
                        ResultSet executeQuery = pstmt.executeQuery();

                        //System.out.println("kndkjwhd");
                        System.out.println("reading data...");

                        while (executeQuery.next()) {
                            int message_id = executeQuery.getInt("message_id");
                            int pubkey_id = executeQuery.getInt("pubkey_id");
                            double difficulty = executeQuery.getDouble("difficulty");
                            long validTill = executeQuery.getLong("validTill");

                            System.out.println("STICK -  pubkey_id: " + pubkey_id + " diff: " + difficulty);

                        }
                        executeQuery.close();

                        System.out.println("done");

                        pstmt.close();
                    } catch (SQLException ex) {
                        Test.sendStacktrace(ex);
                    }

                    continue;
                }

                if (readLine.equals("rc")) {
                    System.out.println("removing channel: ");
                    readLine = bufferedReader.readLine();
                    try {
                        int id = Integer.parseInt(readLine);
                        Channel channelById = Channel.getChannelById(id);
                        channels.remove(channelById);
                        Test.saver.saveIdentities(channels);
                        System.out.println("Done..");
                    } catch (NumberFormatException e) {
                        System.out.println("Keine Nummer.");
                    }
                    continue;
                }

                if (readLine.equals("p")) {
                    if (!localSettings.PEX_ONLY) {
                        System.out.println("message downloader disabled, no new messages will be downloaded, working as PEX mostly...");
                        localSettings.PEX_ONLY = true;
                    } else {
                        System.out.println("message downloader enabled...");
                        localSettings.PEX_ONLY = false;
                    }
                    localSettings.save();
                    continue;
                }

                if (readLine.equals("P")) {
                    MasterChannel.pushAllChannels();
                    MasterChannel.pushIdentity();
                    continue;
                }

                if (readLine.equals("v")) {
                    if (!MessageVerifierHsqlDb.PAUSE) {
                        System.out.println("MessageVerifier deaktivated...");
                        MessageVerifierHsqlDb.PAUSE = true;
                    } else {
                        System.out.println("MessageVerifier running again...");
                        MessageVerifierHsqlDb.PAUSE = false;
                    }
                    continue;
                }

                if (readLine.equals("e")) {
                    System.out.println("e = exit ....");
                    System.exit(0);
                    return;
                }

                if (readLine.equals("badchanneltest")) {
                    channels.add(0, new Channel(new ECKey(), "hans"));
                    saver.saveIdentities(channels);
                    continue;
                }

//                if (readLine.equals("P")) {
//                    System.out.println("New startport: ");
//                    readLine = bufferedReader.readLine();
//                    try {
//                        int parseInt = Integer.parseInt(readLine);
//                        System.out.println("Done. Please restart.");
//                    } catch (NumberFormatException e) {
//                        System.out.println("No number, aborting...");
//                    }
//
//                    continue;
//                }
                if (readLine.equals("speer")) {
                    System.out.println("migrating to super peer, try to connect to max 100 nodes...");
                    Settings.MIN_CONNECTIONS = 100;
                    Settings.MAX_CONNECTIONS = 120;
                    Settings.SUPERNODE = true;
                    Settings.lightClient = false;
                    //threadPool = Executors.newFixedThreadPool(Settings. * 2 + 5);
                    continue;
                }

                if (readLine.equals("lpeer")) {
                    System.out.println("migrating to light peer...");
                    Settings.MIN_CONNECTIONS = 4;
                    Settings.MAX_CONNECTIONS = 10;
                    Settings.SUPERNODE = false;
                    Settings.lightClient = true;
                    //threadPool = Executors.newFixedThreadPool(Settings. * 2 + 5);
                    continue;
                }

                if (readLine.equals("c")) {
                    System.out.println("closing all connections....");
                    for (Peer peer : (ArrayList<Peer>) peerList.clone()) {
                        peer.disconnect("c");
                    }
                    triggerOutboundthread();
                    continue;
                }

                if (readLine.equals("cp")) {
                    try {
                        PreparedStatement pstmt = messageStore.getConnection().prepareStatement("CHECKPOINT DEFRAG");
                        pstmt.execute();
                        continue;
                    } catch (SQLException ex) {
                        Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }

                if (readLine.equals("C")) {
                    System.out.println("starting cluster builder");
                    ClusterBuilder.start();
                    continue;
                }

                if (readLine.equals("d")) {
                    System.out.println("debug info: ");
                    for (PeerTrustData ptd : peerTrusts) {
                        System.out.println("nonce: " + ptd.nonce + " lastSeen: " + ptd.lastSeen + " trustlvl: " + ptd.trustLevel);
                    }

                    ECKey ecKey = null;

                    System.out.format("%30s %10s %10s %20s %10s %20s %10s %20s %10s %20s\n", "Channel", "pubkeyId", "rawCount", "textbytes", "imageCount", "imageBytes", "infoCount", "infoBytes", "deliveredCount", "deliveredBytes");

                    for (Channel c : channels) {
                        int pubkeyId = Test.messageStore.getPubkeyId(c.getKey());

                        long textBytes = 0;
                        long imageBytes = 0;
                        long otherBytes = 0;
                        long infoBytes = 0;
                        long deliveredBytes = 0;
                        long rawMessageCount = 0;
                        long imageMessageCount = 0;
                        long otherMessageCount = 0;
                        long infoMessageCount = 0;
                        long deliveredMessageCount = 0;

                        //System.out.println("Chan: " + c.getName() + " id: " + pubkeyId);
                        ecKey = c.getKey();

                        try {
                            //get Key Id
                            String query = "SELECT pubkey_id,message_id,content,public_type,timestamp,nonce from message WHERE timestamp > ? and verified = true AND pubkey_id = ?";
                            PreparedStatement pstmt = Test.messageStore.getConnection().prepareStatement(query);
                            pstmt.setLong(1, 0);//System.currentTimeMillis() - 1000L * 60L * 60L * 24L * 7L * 4L);
                            pstmt.setInt(2, pubkeyId);
                            ResultSet executeQuery = pstmt.executeQuery();

                            //System.out.println("dwzdzwd " + executeQuery.next());
                            while (executeQuery.next()) {

                                int message_id = executeQuery.getInt("message_id");
                                int pubkey_id = executeQuery.getInt("pubkey_id");
                                //byte[] pubkeyBytes = executeQuery.getBytes("pubkey");
                                //ECKey ecKey = new ECKey(null, pubkeyBytes);
//                            ecKey.database_id = pubkey_id;
//
                                byte public_type = executeQuery.getByte("public_type");
                                long timestamp = executeQuery.getLong("timestamp");
                                int nonce = executeQuery.getInt("nonce");
//                            byte[] signature = executeQuery.getBytes("signature");
                                byte[] content = executeQuery.getBytes("content");
//                            boolean verified = executeQuery.getBoolean("verified");
                                RawMsg rawMsg = new RawMsg(timestamp, nonce, null, content, true);
                                rawMsg.database_Id = message_id;
                                rawMsg.key = ecKey;
                                rawMsg.public_type = public_type;

                                rawMsg = rawMsg.toSpecificMsgType();

                                if (rawMsg instanceof TextMsg) {
                                    textBytes += 8 + content.length;
                                    rawMessageCount++;
                                } else if (rawMsg instanceof ImageMsg) {
                                    imageBytes += 8 + content.length;
                                    imageMessageCount++;
                                } else if (rawMsg instanceof InfoMsg) {
                                    infoBytes += 8 + content.length;
                                    infoMessageCount++;
                                } else if (rawMsg instanceof DeliveredMsg) {
                                    deliveredBytes += 8 + content.length;
                                    deliveredMessageCount++;
                                } else {
                                    System.out.println("" + rawMsg.getClass());
                                    //System.out.println("nope");
                                    otherBytes += 8 + content.length;
                                    otherMessageCount++;
                                }

                            }
                            executeQuery.close();
                            pstmt.close();
                            //System.out.println("Found - messages: " + msgCnt + "  with  " + textBytes / 1024. + " kb text and " + imageBytes / 1024. + " kb images");
                            if (rawMessageCount != 0) {
                                System.out.format("%30s %10d %10d %20f %10d %20f %10d %20f %10d %20f\n", c.getName(), pubkeyId, rawMessageCount, textBytes / 1024., imageMessageCount, imageBytes / 1024., infoMessageCount, infoBytes / 1024., deliveredMessageCount, deliveredBytes / 1024.);
                            }
                        } catch (SQLException ex) {
                            Logger.getLogger(DirectMessageStore.class.getName()).log(Level.SEVERE, null, ex);
                        }

                    }

                    continue;
                }

                //generate one block
                if (readLine.equals("b")) {
                    System.out.println("generate block for channel number:");
                    readLine = bufferedReader.readLine();

                    int channelNumber;
                    try {
                        channelNumber = Integer.parseInt(readLine);
                    } catch (NumberFormatException e) {
                        System.out.println("No number, aborting...");
                        continue;
                    }

                    int pubkeyId = -100;
                    Channel chan = null;
                    for (Channel c : channels) {
                        if (c.getId() != channelNumber) {
                            continue;
                        }
                        chan = c;
                        pubkeyId = Test.messageStore.getPubkeyId(c.getKey());
                    }

                    if (pubkeyId == -100) {
                        System.out.println("channel not found...");
                        continue;
                    }

                    System.out.println("DB id key: " + pubkeyId);

                    long currentTime = System.currentTimeMillis() - BlockMsg.BLOCK_SYNC_TO_TIME; // have to go back some time to overcome the problem with new messages

                    try {
                        //get Key Id
                        //String query = "SELECT pubkey_id,message_id,content,public_type,timestamp,nonce from message WHERE timestamp > ? and verified = true AND pubkey_id = ?";
                        String query = "SELECT message_id,message_type,timestamp,decryptedContent,identity,fromMe,nonce,public_type from channelmessage WHERE pubkey_id =? AND timestamp > ? AND timestamp < ? ORDER BY timestamp ASC";
                        PreparedStatement pstmt = Test.messageStore.getConnection().prepareStatement(query);
                        pstmt.setInt(1, pubkeyId);
                        long asd = currentTime - BlockMsg.TIME_TO_SYNC_BACK;
                        System.out.println("time: " + asd);
                        pstmt.setLong(2, asd);
                        pstmt.setLong(3, currentTime);

                        ResultSet executeQuery = pstmt.executeQuery();

                        int msgcount = 0;

                        boolean breaked = false;

                        byte[] dataArray = new byte[1024 * 200]; //max size of a message!! should be regulated later!
                        ByteBuffer buffer = ByteBuffer.wrap(dataArray);

                        byte[] dataArrayHash = new byte[1024 * 200];
                        ByteBuffer bufferHash = ByteBuffer.wrap(dataArrayHash);

                        //ToDo: add hash from all data and count of messages to get an easier sync!
                        //System.out.println("dwzdzwd " + executeQuery.next());
                        while (executeQuery.next()) {

                            int message_id = executeQuery.getInt("message_id");
                            int message_type = executeQuery.getInt("message_type");
                            byte[] decryptedContent = executeQuery.getBytes("decryptedContent");
                            long timestamp = executeQuery.getLong("timestamp");
                            long identity = executeQuery.getLong("identity");
                            boolean fromMe = executeQuery.getBoolean("fromMe");

                            byte public_type = executeQuery.getByte("public_type");
                            int nonce = executeQuery.getInt("nonce");

                            if (decryptedContent == null) {
                                decryptedContent = "".getBytes();
                            }

                            //only pack a message into a block if the public_type is 20!
                            if (public_type == 20) {

                                //skip content which will be generated regulary, image messages should not have public_type 20! (with new version, old imgs will be deleted)
                                if (message_type != TextMsg.BYTE) {
                                    continue;
                                }
                                if (buffer.remaining() < 8 + 4 + 4 + 8 + 4 + decryptedContent.length) {
                                    System.out.println("buffer full, exit routine, dont know what to do atm");
                                    breaked = true;
                                    break;
                                }

                                //System.out.println("Data: msgtyp: " + message_type + " pubtyp: " + public_type + " " + new String(decryptedContent));
                                System.out.println("len: " + decryptedContent.length);
                                buffer.putLong(timestamp);
                                buffer.putInt(nonce);
                                buffer.putInt(message_type);
                                buffer.putLong(identity);
                                buffer.putInt(decryptedContent.length);
                                buffer.put(decryptedContent);

                                msgcount++;

                                //add data to hash bytes:b (dont use all data because when syncing we only need to get these data types)
                                bufferHash.putLong(timestamp);
                                bufferHash.putInt(message_type);
                                bufferHash.putInt(public_type);
                            }

                        }
                        executeQuery.close();
                        pstmt.close();

                        if (breaked) {
                            System.out.println("abort...");
                            continue;
                        }

                        //System.out.println("Found - messages: " + msgCnt + "  with  " + textBytes / 1024. + " kb text and " + imageBytes / 1024. + " kb images");
                        //System.out.format("%30s %10s %10s %20s %10s %20s %10s %20s %10s %20s\n", "Channel", "pubkeyId", "rawCount", "textbytes", "imageCount", "imageBytes", "infoCount", "infoBytes", "deliveredCount", "deliveredBytes");
                        //System.out.format("%30s %10d %10d %20f %10d %20f %10d %20f %10d %20f\n", chan.getName(), pubkeyId, rawMessageCount, textBytes / 1024., imageMessageCount, imageBytes / 1024., infoMessageCount, infoBytes / 1024., deliveredMessageCount, deliveredBytes / 1024.);
                        ByteBuffer finalBuffer = ByteBuffer.allocate(buffer.position() + 1 + 8 + 4 + 4);

                        System.out.println("size: " + buffer.position());

                        ByteBuffer lel = ByteBuffer.allocate(buffer.position());
                        lel.put(dataArray, 0, buffer.position());

                        dataArray = lel.array();//shrinked to actual size

                        finalBuffer.put(BlockMsg.BYTE); //cmd for block
                        finalBuffer.putLong(Test.localSettings.identity);//mark that i was the generator of the block
                        finalBuffer.putInt(msgcount);
                        finalBuffer.putInt(Sha256Hash.create(dataArrayHash).hashCode());
                        finalBuffer.put(dataArray);

                        double kbs = finalBuffer.position() / 1024.;
                        System.out.println("content block size: " + kbs + "  in " + msgcount + " messags.");

                        MessageDownloader.channelIdToLatestBlockTimeLock.lock();
                        MessageDownloader.channelIdToLatestBlockTime.put(pubkeyId, currentTime);
                        MessageDownloader.channelIdToLatestBlockTimeLock.unlock();

//                        System.out.println("hex: " + Utils.bytesToHexString(finalBuffer.array()));
                        BlockMsg build = BlockMsg.build(chan, currentTime, 45678, finalBuffer.array());

                        BlockMsg addMessage = (BlockMsg) MessageHolder.addMessage(build);
                        Test.broadcastMsg(addMessage);
                        String text = "New block generated with " + msgcount + " msgs (" + kbs + " kb).";
                        Test.messageStore.addDecryptedContent(addMessage.getKey().database_id, (int) addMessage.database_Id, BlockMsg.BYTE, addMessage.timestamp, text.getBytes(), ((BlockMsg) addMessage).getIdentity(), true, addMessage.nonce, addMessage.public_type);
                        TextMessageContent textMessageContent = new TextMessageContent(addMessage.database_Id, addMessage.key.database_id, addMessage.public_type, BlockMsg.BYTE, addMessage.timestamp, addMessage.decryptedContent, addMessage.channel, addMessage.getIdentity(), text, true);
                        textMessageContent.read = true;
                        for (NewMessageListener listener : Main.listeners) {
                            listener.newMessage(textMessageContent);
                        }

                        System.out.println("New block saved and send, doing cleanup...");

                        //remove old block:
                        int removeMessagesFromChannel = messageStore.removeMessagesFromChannel(pubkeyId, BlockMsg.PUBLIC_TYPE, currentTime);
                        System.out.println("removed old blocks: " + removeMessagesFromChannel);

                        //remove old messages which are encrypted and now saved in the new block (only necessary data)...
                        removeMessagesFromChannel = messageStore.removeMessagesFromChannel(pubkeyId, (byte) 20, currentTime);
                        System.out.println("removed old encrypted messages: " + removeMessagesFromChannel);

                    } catch (SQLException ex) {
                        Logger.getLogger(DirectMessageStore.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    continue;
                }

                if (readLine.equals("gs")) {
                    //generate grouped stacktraces
                    ArrayList<TextMessageContent> messages = Main.getMessages(Channel.getChannelById(-2));

                    for (TextMessageContent t : messages) {

                        if (!t.text.contains("ConnectionHandler.java:172") && !t.text.contains("pl.droidsonroids.gif.GifDrawable.<init>(GifDrawable.java:125)")) {

                            System.out.println("" + t.text);

                        }

                    }

                    continue;
                }

////                //generate one block
////                if (readLine.equals("rate")) {
////
////                    for (ECKey key : ConnectionHandler.ratingData.keySet()) {
////
////                        for (HashMap<PeerTrustData,ArrayList<Long>>: ConnectionHandler.ratingData.get(key)) {
////
////                        }
////
////                    }
////
////                    continue;
////                }
                if (readLine.equals("d2")) {

                    for (Peer p : peerList) {

                        if (p.writeBuffer != null && p.writeBufferCrypted != null) {

                            System.out.println("PEER: " + p.writeBuffer.position() + " " + p.writeBufferCrypted.position() + " " + p.readBuffer.position() + " " + p.readBufferCrypted.position());

                        }

                    }

                    continue;
                }

                if (readLine.equals("d3")) {

                    System.out.println("dhadghawdgfawzhjudgfawhudf ");

                    try {
                        //get Key Id
                        String query = "SELECT peer_id,message_id from haveToSendMessageToPeer";
                        PreparedStatement pstmt = Test.messageStore.getConnection().prepareStatement(query);
                        ResultSet executeQuery = pstmt.executeQuery();

                        //System.out.println("dwzdzwd " + executeQuery.next());
                        while (executeQuery.next()) {
                            long peer_id = executeQuery.getLong("peer_id");
                            int message_id = executeQuery.getInt("message_id");

                            System.out.println("haveTosendmsg: " + peer_id + " - " + message_id);

                        }
                        executeQuery.close();
                        pstmt.close();
                    } catch (SQLException ex) {
                        Logger.getLogger(DirectMessageStore.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    continue;
                }

                if (readLine.equals("d4")) {
                    MessageDownloader.requestedMsgsLock.lock();
                    for (MessageDownloader.RawMsgEntry msg : MessageDownloader.requestedMsgs) {

                        System.out.println("msg time: " + msg.requestedWhen);

                    }
                    MessageDownloader.requestedMsgsLock.unlock();
                    continue;
                }

                if (readLine.equals("d5")) {
                    //ConnectionHandler.removeUnusedSockets();
                    continue;
                }

                if (readLine.equals("d6")) {
                    Map<Thread, StackTraceElement[]> allStackTraces = Thread.getAllStackTraces();

                    System.out.println(allStackTraces.toString());

                    Collection<StackTraceElement[]> values = allStackTraces.values();

                    for (Thread thd : allStackTraces.keySet()) {

                        System.out.println("Thread Name: " + thd.getName() + " state: " + thd.getState());

                        StackTraceElement[] a = allStackTraces.get(thd);

                        for (int i = 0; i < a.length; i++) {

                            System.out.println("" + a[i].toString());
                        }
                        System.out.println("#######");

                    }
                    continue;
                }

//                if (readLine.equals("d7")) {
//
//                    //System.out.println("rdy keys: " + ConnectionHandler.selector.selectNow());
//                    try {
//                        for (SelectionKey key : ConnectionHandler.selector.keys()) {
//                            if (key.attachment() == null) {
//                                System.out.println("valid: " + key.isValid() + ", no attachment");
//                            } else {
//                                Peer peer = (Peer) key.attachment();
//                                System.out.println("valid: " + key.isValid() + " ip: " + peer.ip + ":" + peer.port);
//                            }
//                        }
//                    } catch (ConcurrentModificationException e) {
//                        System.out.println("list modified, try again...");
//                    }
//
//                    continue;
//                }
                if (readLine.equals("d8")) {
                    System.out.println("WARNING, clearing all keys, this means no inbound connections are now possible till restart!!!");
                    try {
                        for (SelectionKey key : Test.connectionHandler.selector.keys()) {
                            key.cancel();
                        }
                    } catch (ConcurrentModificationException e) {
                        System.out.println("list modified, try again...");
                    }
                    continue;
                }

                if (readLine.equals("d9")) {
                    connectionHandler.exit();
                    connectionHandler = new ConnectionHandler();
                    connectionHandler.start();

                    System.out.println("tried to renew the ConnectionHandler...");

                    continue;
                }

                if (readLine.equals("d10")) {
                    //removing all trust which arent connected
                    for (PeerTrustData trust : (ArrayList<PeerTrustData>) peerTrusts.clone()) {

                        boolean found = false;
                        for (Peer peer : getClonedPeerList()) {
                            if (peer.peerTrustData == trust) {
                                found = true;
                                break;
                            }
                        }

                        if (!found) {
                            System.out.println("removing trust: " + trust.internalId);
                            peerTrusts.remove(trust);
                            messageStore.clearFilterChannel(trust.internalId);
                            messageStore.removeMessageToSend(trust.internalId);
                        }
                    }

                    continue;
                }

                if (readLine.equals("d11")) {
                    try {
                        PreparedStatement prepareStatement = messageStore.getConnection().prepareStatement("SELECT COUNT(message_id) FROM haveToSendMessageToPeer");
                        ResultSet executeQuery = prepareStatement.executeQuery();
                        executeQuery.next();
                        System.out.println("MessagesToSync: " + executeQuery.getInt(1));
                        executeQuery.close();
                        prepareStatement.close();

                    } catch (SQLException ex) {
                        Logger.getLogger(Test.class
                                .getName()).log(Level.SEVERE, null, ex);
                    }
                    continue;
                }
                if (readLine.equals("d12")) {
                    try {
                        PreparedStatement prepareStatement = messageStore.getConnection().prepareStatement("TRUNCATE table haveToSendMessageToPeer");
                        prepareStatement.execute();
                        prepareStatement.close();

                        prepareStatement = messageStore.getConnection().prepareStatement("TRUNCATE table filterChannels");
                        prepareStatement.execute();
                        prepareStatement.close();

                    } catch (SQLException ex) {
                        Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    continue;
                }
                if (readLine.equals("d13")) {

                    ArrayList<Peer> clonedPeerList = getClonedPeerList();
                    Collections.sort(clonedPeerList);
                    for (Peer peer : clonedPeerList) {
                        if (peer.peerTrustData == null) {
                            continue;
                        }
                        if (messagesToSync(peer.peerTrustData.internalId) != 0) {

                            System.out.println("found: " + peer.nonce);
                            peer.removedSendMessages.clear();
                            peer.disconnect("teeest3123");

                            break;
                        }

                    }
                    continue;
                }

                if (readLine.equals("d14")) {
                    Channel channel = Channel.getChannelById(-2);
                    long a = System.currentTimeMillis();
                    ArrayList<TextMessageContent> messages = Main.getMessages(channel, System.currentTimeMillis() - 1000 * 60 * 60 * 24 * 30L, System.currentTimeMillis());
                    System.out.println("size: " + messages.size() + " - " + (System.currentTimeMillis() - a));
                    System.out.println("go");
                    Test.messageStore.moveChannelMessagesToHistory(System.currentTimeMillis() - 1000L * 60L * 60L * 24L * 30L * 2L);
                    System.out.println(": " + (System.currentTimeMillis() - 1000L * 60L * 60L * 24L * 30L * 2L));
                    System.out.println("done!!!");
                    continue;
                }

                if (readLine.equals("d15")) {

                    MessageVerifierHsqlDb.printStack();
                    continue;
                }
                if (readLine.equals("d16")) {
                    System.out.println("msgid: " + messageStore.getNextMessageId());
                    continue;
                }

                if (readLine.equals("ls")) {

                    ArrayList<Peer> clonedPeerList = getClonedPeerList();
                    Collections.sort(clonedPeerList);
                    for (Peer peer : clonedPeerList) {
                        if (peer.peerTrustData == null) {
                            continue;
                        }

                        System.out.println("peer: " + peer.nonce + " locked: " + peer.writeBufferLock.isLocked() + " holdcnt: " + peer.writeBufferLock.getHoldCount() + " queue: " + peer.writeBufferLock.getQueueLength());

                    }
                    continue;
                }

                if (readLine.equals("ls2")) {

                    ArrayList<Peer> clonedPeerList = getClonedPeerList();
                    Collections.sort(clonedPeerList);
                    for (Peer peer : clonedPeerList) {
                        if (peer.peerTrustData == null) {
                            continue;
                        }

                        System.out.println("peer: " + peer.nonce + " pending msgs: " + peer.getPeerTrustData().pendingMessages.size() + " - timeouted: " + peer.getPeerTrustData().pendingMessagesTimedOut);

                    }
                    continue;
                } else if (readLine.equals("ls3")) {

                    ArrayList<Peer> clonedPeerList = getClonedPeerList();
                    Collections.sort(clonedPeerList);
                    for (Peer peer : clonedPeerList) {
                        if (peer.peerTrustData == null) {
                            continue;
                        }

                        System.out.println("peer: " + peer.nonce + " requsted: " + peer.requestedMsgs);

                    }
                    continue;
                }

                if (readLine.equals("triggerSync")) {

                    ArrayList<Peer> clonedPeerList = getClonedPeerList();
                    Collections.sort(clonedPeerList);
                    for (Peer peer : clonedPeerList) {
                        if (peer.peerTrustData == null || !peer.authed || !peer.isConnected()) {
                            continue;
                        }

                        System.out.println("found: " + peer.nonce);
                        //peer.removedSendMessages.clear();
                        //peer.disconnect("teeest3123");
                        //triggerOutboundthread();
                        peer.writeBufferLock.lock();

                        //init sync
                        peer.writeBuffer.put((byte) 3);
                        //sync last two days
                        //writeBuffer.putLong(-1);// full sync!
                        //writeBuffer.putLong(0);
                        //writeBuffer.putLong(Math.max(System.currentTimeMillis() - 1000 * 60 * 60 * 24 * 2, Settings.till));
                        //writeBuffer.putLong(Math.max(System.currentTimeMillis() - 1000 * 60, Settings.till));
                        //writeBuffer.putLong(System.currentTimeMillis() - 1000 * 60 * 60);

                        long myTime = System.currentTimeMillis() - 1000 * 60 * 60 * 24 * 7;
                        if (Settings.SUPERNODE) {
                            myTime = 0;
                        }
                        peer.writeBuffer.putLong(myTime);
                        peer.writeBuffer.put((byte) 0);
                        //get PeerList
                        peer.writeBuffer.put((byte) 1);
//                    peer.setWriteBufferFilled();
                        peer.writeBufferLock.unlock();
                        peer.setWriteBufferFilled();

                        System.out.println("try to sync again...");

                    }

                    continue;
                }
                if (readLine.equals("triggerMD")) {
                    MessageDownloader.trigger();
                    continue;
                }
                // set log level
                if (readLine.equals("ll")) {
                    System.out.println("new log level:");
                    readLine = bufferedReader.readLine();
                    try {
                        int newLvl = Integer.parseInt(readLine);
                        Log.LEVEL = newLvl;
                        System.out.println("Done.");
                    } catch (NumberFormatException e) {
                        System.out.println("No Number.");
                    }
                    continue;
                }

                //mark as read
                if (readLine.equals("mar")) {
                    System.out.println("mark message as read (ID):");
                    readLine = bufferedReader.readLine();
                    try {
                        long id = Long.parseLong(readLine);
                        Main.markAsRead(id);
                        System.out.println("Done.");
                    } catch (NumberFormatException e) {
                        System.out.println("No Number.");
                    }
                    continue;
                }

                if (readLine.equals("D")) {
                    System.out.println("debug info: ");
                    for (PeerTrustData ptd : peerTrusts) {
                        System.out.println("nonce: " + ptd.nonce + " ID: " + ptd.internalId + " intToHim: " + messageStore.msgCountIntroducedToHim(ptd.internalId) + "  msgsIntroducedToMe: " + messageStore.msgCountIntroducedToMe(ptd.internalId) + " msgsToSyncToPeer: " + Test.messageStore.msgsToUser(ptd.internalId, System.currentTimeMillis() - 1000 * 60 * 60 * 24 * 7));
                    }
                    continue;
                }

                if (readLine.equals("a")) {
                    System.out.println("addPeerIp");
                    readLine = bufferedReader.readLine();
                    findPeer(new Peer(readLine, Settings.STD_PORT));
                    continue;
                }

                if (readLine.equals("f")) {
                    System.out.println("Init fullsync...");
                    Settings.initFullNetworkSync = true;
                    continue;
                }

                if (readLine.equals("+")) {
                    Settings.MIN_CONNECTIONS++;
                    Settings.MAX_CONNECTIONS++;
                    System.out.println("+1 peer: " + Settings.MIN_CONNECTIONS);
                    triggerOutboundthread();
                    continue;
                }

                if (readLine.equals("-")) {
                    Settings.MIN_CONNECTIONS--;
                    Settings.MAX_CONNECTIONS--;
                    System.out.println("-1 peer: " + Settings.MIN_CONNECTIONS);
                    triggerOutboundthread();
                    continue;
                }

                //clientVersion++;
                //Msg msg = new Msg(System.currentTimeMillis(), 55, SpecialChannels.MAIN, clientSeed, clientVersion, "[" + getNick() + "] " + readLine);
                //processNewMessage(msg, true);
//                TextMsg build = TextMsg.build(Channel.getChannelById(-2), readLine);
//                MessageHolder.addMessage(build);
//                broadcastMsg(build);
                //Main.sendMessageToChannel(Channel.getChannelById(-2), readLine);
                Main.sendBroadCastMsg(readLine);
                System.out.println("send...");

                //                byte[] toBytes = msg.toBytes();
                //
                //                ArrayList<Peer> dd = (ArrayList<Peer>) peerList.clone();
                //
                //                Collections.shuffle(dd);
                //
                //                for (Peer peer : dd) {
                //                    if (peer.connectionThread != null) {
                //                        peer.connectionThread.writeBytes(toBytes);
                //                    }
                //                }
            }

        }
    }

    public static int messagesToSync(long internalIdFromPeerTrust) {
        int messagesToSync = -1;
        //mysql querrys:
        try {
            PreparedStatement prepareStatement = messageStore.getConnection().prepareStatement("SELECT COUNT(message_id) FROM haveToSendMessageToPeer WHERE peer_id = ?");
            prepareStatement.setLong(1, internalIdFromPeerTrust);
            ResultSet executeQuery = prepareStatement.executeQuery();
            executeQuery.next();
            messagesToSync = executeQuery.getInt(1);
            executeQuery.close();
            prepareStatement.close();

        } catch (SQLTransactionRollbackException e) {
        } catch (SQLException ex) {
            Logger.getLogger(Test.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
        return messagesToSync;
    }

    private static void loadChannels() {
        channels = saver.loadIdentities();
        synchronized (channels) {
//            if (!channels.contains(SpecialChannels.MAIN)) {
//                Channel c = SpecialChannels.MAIN;
//                channels.add(c);
//            }

            if (channels.isEmpty() || channels.get(0) == null || !channels.get(0).getName().equals("Master")) {
                createNewMasterChannel();
            }

            if (Channel.getChannelById(-4) == null) {
                channels.add(SpecialChannels.getAnnouncementChannel());
                System.out.println("Added announcements channel!");
            }
//            if (channels.size() < 2) {
//                channels.add(Channel.generateNew("Mine 1"));
//                channels.add(Channel.generateNew("Mine 2"));
//                System.out.println("generated new channels!");
//            } else {
//                for (Channel c : channels) {
//                    System.out.println("Channel privkeys to write to: " + c.getPrivateKey() + " Pub: " + Base58.encode(c.getKey().getPubKey()));
//                }
//            }
            saver.saveIdentities(channels);
        }
    }

    private static void createNewMasterChannel() {
        Channel newMasterChannel = new Channel();
        newMasterChannel.key = new ECKey();
        newMasterChannel.name = "Master";
        newMasterChannel.id = 0;
        channels.add(0, newMasterChannel);
        System.out.println("Generated new Master Key!");
    }

    public static void broadcastMsg(RawMsg rawMsg) {

        if (rawMsg.timestamp < Settings.till) {
            return;
        }

        //ToDo: remove later
        int containsMsg = messageStore.containsMsg(rawMsg);
        if (containsMsg != rawMsg.database_Id) {
            throw new RuntimeException("!!!! kek " + containsMsg + " != " + rawMsg.database_Id);
        }

        Test.messageStore.addMessageToSend(rawMsg.database_Id, rawMsg.key.database_id);

        try {
            String query = "SELECT peer_id from haveToSendMessageToPeer WHERE message_id = ?";
            PreparedStatement pstmt = Test.messageStore.getConnection().prepareStatement(query);
            pstmt.setInt(1, rawMsg.database_Id);
            ResultSet executeQuery = pstmt.executeQuery();

            ArrayList<Peer> clonedPeerList = getClonedPeerList();

            while (executeQuery.next()) {

                for (Peer p : clonedPeerList) {
                    if (p.isConnected() && p.isAuthed() && p.syncMessagesSince <= rawMsg.timestamp && executeQuery.getInt(1) == p.getPeerTrustData().internalId) {
                        p.writeMessage(rawMsg);
                        p.setWriteBufferFilled();
                        break;
                    }
                }

            }

            pstmt.close();

        } catch (SQLException ex) {
            Logger.getLogger(DirectMessageStore.class
                    .getName()).log(Level.SEVERE, null, ex);
        }

        //System.out.println("channel_id: " + rawMsg.key.database_id);
    }

    static class InboundThread extends Thread {

        @Override
        public void run() {

            final String orgName = Thread.currentThread().getName();
            Thread.currentThread().setName(orgName + " - InboundThread");

            if (DEBUG) {
                System.out.println("inbound thread started...");
            }

            try {
                ServerSocketChannel serverSocketChannel;
                serverSocketChannel = ServerSocketChannel.open();
                serverSocketChannel.configureBlocking(false);

                boolean bound = false;
                //MY_PORT = Settings.STD_PORT;
                MY_PORT = Settings.getStartPort();
                ServerSocket serverSocket = null;

                if (DEBUG) {
                    System.out.println("searching port to bind to...");
                }

                while (!bound) {

                    bound = true;
                    try {
                        serverSocketChannel.socket().bind(new InetSocketAddress(MY_PORT));
                    } catch (Throwable e) {

                        if (DEBUG) {
                            System.out.println("could not bound to port: " + MY_PORT);
                        }

                        //e.printStackTrace();
                        bound = false;
                        //MY_PORT = Settings.STD_PORT + random.nextInt(30);
                        MY_PORT += 1;
                    }

                }

                if (DEBUG) {
                    System.out.println("bound successfuly to port: " + MY_PORT);
                }

                //port festgelegt...
                peerList = Test.saver.loadPeers();
                peerTrusts = Test.saver.loadTrustedPeers();

                if (DEBUG) {
                    System.out.println("loaded peerlist...");
                }
//                ArrayList<RawMsg> loadMsgs = Test.saver.loadMsgs();
//                for (RawMsg m : loadMsgs) {
//                    m.verifying = false;
//                }
//                MessageHolder.msgs = loadMsgs;
//
//
//                if (MessageHolder.msgs.isEmpty()) {
//                    //msgs.add(new Msg(System.currentTimeMillis(), "nonce", identities.get(0), clientSeed, clientVersion, "Warning old messages not loaded...."));
//                    System.out.println("Warning old messages not loaded....");
//                }

//                if (PORTFORWARD) {
//                    String localHost = InetAddress.getLocalHost().getHostName();
//                    InetAddress[] allByName = InetAddress.getAllByName(localHost);
//                    Portforward.start(MY_PORT, allByName[0].getHostAddress());
//                    System.out.println("Started UPNP portforward for port " + MY_PORT);
//                }
                connectionHandler.addServerSocketChannel(serverSocketChannel);
                startedUpSuccessful();
            } catch (IOException ex) {
                Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
    }

    public static void triggerOutboundthread() {
        if (outboundthread != null) {
            outboundthread.tryInterrupt();

        }
    }

    static class Outboundthread extends Thread {

        boolean allowInterrupt = false;

        public void tryInterrupt() {
            if (allowInterrupt) {
                interrupt();
            }
        }

        @Override
        public void run() {

            final String orgName = Thread.currentThread().getName();
            Thread.currentThread().setName(orgName + " - OutboundThread");

//            Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
//
//                @Override
//                public void uncaughtException(Thread t, Throwable e) {
//                    e.printStackTrace();
//                    sendStacktrace(e);
//                }
//            });
            long loopCount = 0;

            while (!Main.shutdown) {

                loopCount++;
                Log.put("loop: " + loopCount, 50);

                if (Settings.connectToNewClientsTill < System.currentTimeMillis()) {
                    try {
                        allowInterrupt = true;
                        sleep(1000 * 60 * 15);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
                    } finally {
                        allowInterrupt = false;
                    }
                    continue;
                }

                //ToDo: remove, not working anymore
                if (Settings.initFullNetworkSync) {
                    //just a one time action
                    Settings.initFullNetworkSync = false;
                    System.out.println("init full sync, closing all connections");
                    ArrayList<Peer> clone = (ArrayList<Peer>) peerList.clone();
                    peerList = new ArrayList<Peer>();
                    for (Peer peer : clone) {
                        peer.disconnect("full sync");
                    }

                    for (PeerTrustData ptd : (ArrayList<PeerTrustData>) peerTrusts.clone()) {
                        ptd.ips.clear();
                        ptd.keyToIdHis.clear();
                        ptd.keyToIdMine.clear();
                        ptd.loadedMsgs.clear();
                    }

                }

                if (peerList == null || NONCE == 0) {
                    try {
                        sleep(200);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    continue;
                }

                if (peerList.isEmpty()) {
                    addKnowNodes();
                }

                ArrayList<Peer> clonedPeerList = (ArrayList<Peer>) peerList.clone();

                //Collections.shuffle(peerList);
                try {
                    Collections.sort(clonedPeerList);
                } catch (java.lang.IllegalArgumentException e) {
                    try {
                        sleep(200);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    continue;
                }

                int actCons = 0;
                int connectingCons = 0;
                for (Peer peer : clonedPeerList) {
                    if (peer.getLastAnswered() < Settings.pingTimeout * 1000 && peer.isConnected() && peer.isAuthed() && peer.isCryptedConnection()) {

//                        String[] split = peer.ip.split("\\.");
//                        if (split.length == 4 && split[0].equals("192")) {
//                            //hack: is lan ip...
//                        } else {
//                            actCons++;
//                        }
                        actCons++;
                    } else if (peer.isConnecting) {
                        connectingCons++;
                    }
                }

                boolean fasterRetry = (actCons < 2);

//                if (connectingCons == clonedPeerList.size()) {
//                    //hack!
//                    for (Peer peer : clonedPeerList) {
//                        peer.disconnect("con neu");
//                    }
//                    continue;
//                }
//                if (DEBUG) {
//                    System.out.println("search new peers....");
//                }
                actCons += connectingCons;

                int cnt = 0;
                for (Peer peer : clonedPeerList) {

                    if (Settings.connectToNewClientsTill < System.currentTimeMillis()) {
                        break;
                    }

                    cnt++;

                    if (actCons >= Settings.MIN_CONNECTIONS) {
                        if (DEBUG) {
                            Log.put("peers " + actCons + " are enough...", 300);
                        }
                        if (cnt == 1 && actCons >= Settings.MAX_CONNECTIONS) {
                            for (Peer p1 : clonedPeerList) {
                                if (p1.isConnected()) {
                                    p1.disconnect("max cons");
                                    if (DEBUG) {
                                        System.out.println("closed one connection...");
                                    }
                                    break;
                                }
                            }
                        }
                        break;
                    }

                    if (peer.port == 0) {
                        continue;
                    }

                    if (peer.isConnected()) {
                        continue;
                    }

                    boolean alreadyConnectedToSameIpandPort = false;
                    for (Peer p2 : clonedPeerList) {
                        if (peer.equalsIpAndPort(p2) && (peer.isConnected() || peer.isConnecting)) {
                            alreadyConnectedToSameIpandPort = true;
                            break;
                        }
                    }

                    if (alreadyConnectedToSameIpandPort) {
//                        if (DEBUG) {
//                            System.out.println("already connected to same ip + prot, not connecting: " + peer.ip + ":" + peer.port);
//                        }
                        continue;
                    }
                    if (Settings.IPV6_ONLY && peer.ip.length() <= 15) {
                        peerList.remove(peer);
                        if (DEBUG) {
                            System.out.println("removed peer from peerList, no ipv6 address: " + peer.ip + ":" + peer.port);
                        }
                        continue;
                    }

                    if (Settings.IPV4_ONLY && peer.ip.length() > 15) {
                        peerList.remove(peer);
                        if (DEBUG) {
                            System.out.println("removed peer from peerList, no ipv4 address: " + peer.ip + ":" + peer.port);
                        }
                        continue;
                    }

                    boolean alreadyConnectedToSameTrustedNode = false;
                    String equalIp = null;
                    //already connected to same trusted node?
                    for (Peer p2 : clonedPeerList) {

                        if (alreadyConnectedToSameTrustedNode) {
                            break;
                        }

                        if (!p2.isConnected() && !p2.isConnecting) {
                            continue;
                        }

                        if (p2.peerTrustData == null) {
                            continue;
                        }

                        PeerTrustData ptd = p2.peerTrustData;

                        for (String ip : ptd.ips) {

                            if (peer.ip.equals(ip) && peer.port == ptd.port) {
                                //System.out.println("connected already over another IP: " + peer.ip + " -- " + ip);
                                alreadyConnectedToSameTrustedNode = true;
                                break;
                            }

                        }

                    }

                    if (alreadyConnectedToSameTrustedNode) {
                        //System.out.println("Prevented connecting to same node, connected to trusted node over another IP (v4-v6) already. ");
                        continue;
                    }

                    if (peer.isConnected() || peer.isConnecting) {
                        continue;
//                        peer.disconnect();
//                        if (DEBUG) {
//                            System.out.println("closing con, cuz i wanna connect...");
//                        }
                    }

//                    if (peerList.size() > 20) {
                    //(System.currentTimeMillis() - peer.lastActionOnConnection > 1000 * 60 * 60 * 4)
                    if (peer.retries > 10 || (peer.nonce == 0 && peer.retries >= 1)) {
                        //peerList.remove(peer);
                        removePeer(peer);
                        if (DEBUG) {
                            Log.put("removed peer from peerList, too many retries: " + peer.ip + ":" + peer.port, 20);
                        }
                        continue;
                    }

//                    }
                    if (peer.retries > 5 && actCons >= 2) {

//                        System.out.println("retry: " + loopCount + " % " + peer.retries + " = " + loopCount % peer.retries);
                        long lastRetryFor = System.currentTimeMillis() - peer.lastRetryAfter5;

                        //System.out.println("last retry for millis: " + lastRetryFor);
                        if (lastRetryFor < 1000 * 60 * 5) {
                            if (DEBUG) {
//                                 System.out.println("Skipp connecting, throttling retries...");
                            }
                            continue;
                        }

                        peer.lastRetryAfter5 = System.currentTimeMillis();

                    }

                    if (peer.connectAble != -1) {
                        if (DEBUG) {
                            Log.put("try to connect to new node: " + peer.ip + ":" + peer.port, 5);
                        }
                        connectTo(peer);
                        actCons++;
                        try {
                            sleep(200);
                        } catch (InterruptedException ex) {
                            Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
                        }

                    } else {
                        System.out.println("connect state: " + peer.connectAble + " -- " + peer.ip + ":" + peer.port);
                    }

                }

                try {
                    allowInterrupt = true;
                    if (fasterRetry) {
                        sleep(5000 + random.nextInt(3000));
                    } else {
                        sleep(60000 + random.nextInt(30000));
                    }
                } catch (InterruptedException ex) {
                } finally {
                    allowInterrupt = false;
                }

            }

        }
    }

    private static void addKnowNodes() {

        if (System.currentTimeMillis() - lastAddedKnownNodes < 1000 * 60 * 10) {
            return;
        }

        lastAddedKnownNodes = System.currentTimeMillis();

        for (String host : Settings.knownNodes) {
            findPeer(new Peer(host, Settings.STD_PORT));
//            try {
//                //ToDo: eats up cpu if no internet is available?!?
//                findPeer(new Peer(InetAddress.getByName(host).getHostAddress(), Settings.STD_PORT));
//            } catch (UnknownHostException ex) {
//                System.out.println("Could not resolve hostname: " + host);
//            }
        }

    }

    private static void connectTo(final Peer peer) {

        peer.retries++;
        peer.isConnecting = true;

//        if (peer.retries > 20 && peerList.size() > 3) {
//            removePeer(peer);
//            return;
//        }
//        if (peer.connectionThread != null && peer.connectionThread.alive) {
//            return;
//        }
//        threadPool2.submit(
        peer.connectinThread = new Thread() {

//            @Override
//            public UncaughtExceptionHandler getUncaughtExceptionHandler() {
//                return new UncaughtExceptionHandler() {
//
//                    @Override
//                    public void uncaughtException(Thread t, Throwable e) {
//                        sendStacktrace(e);
//                        peer.disconnect("exception 87842");
//                    }
//                };
//            }
            @Override
            public void run() {

                final String orgName = Thread.currentThread().getName();
                if (!orgName.contains(" ")) {
                    Thread.currentThread().setName(orgName + " - Connected to Node");
                }

                try {
                    //Socket socket = new Socket(peer.ip, peer.port);
                    //                            ConnectionThread connectionThread = new ConnectionThread(peer, socket);
                    //                            threadPool3.submit(connectionThread);
//                                                        Socket socket = new Socket(peer.ip, peer.port);
//                                                        InetAddress inetAddress = socket.getInetAddress();
//                                                        socket.close();

                    SocketChannel open = SocketChannel.open();
                    open.configureBlocking(false);
                    //open.connect(new InetSocketAddress("xana.hopto.org", peer.port));
                    //open.connect(new InetSocketAddress("xana.hopto.org", peer.port));
                    open.connect(new InetSocketAddress(peer.ip, peer.port));
                    peer.setSocketChannel(open);
                    if (!isInterrupted()) {
                        connectionHandler.addConnection(peer, true);
                    }
                    //connectionHandlerConnection.addConnection(peer);

                } catch (UnknownHostException ex) {
                    if (DEBUG) {
                        System.out.println("outgoing con failed, unknown host...");
                    }
                } catch (Exception ex) {

                    Log.put("outgoing con failed...", 150);
                }

            }
        };

        peer.connectinThread.start();
//                        );

    }

//    static void writeAll(final String msg) {
//
//
//        ArrayList<Peer> dd = (ArrayList<Peer>) peerList.clone();
//
////        Collections.shuffle(dd);
//
//        for (Peer peer : dd) {
//            peer.writeTo(msg);
//        }
//
//
//
//
////        threadPool.submit(
////                new Thread() {
////
////                    @Override
////                    public void run() {
////                        synchronized (outStreams) {
////                            for (final PrintWriter pw : outStreams) {
////
////
////
////                                pw.print(msg);
////                                if (pw.checkError()) {
////                                    outStreams.remove(pw);
////                                }
////                            }
////
////                        }
////                    }
////                });
//
//
//
//
//
//
//    }
    public static synchronized Peer findPeer(Peer peer) {

        for (Peer p : (ArrayList<Peer>) peerList.clone()) {

            if (p.equalsIpAndPort(peer)) {
                return p;
            }

        }

        peerList.add(peer);
        return peer;
    }

    public static synchronized Peer findPeerNonce(Peer peer) {
        synchronized (peerList) {

            for (Peer p : (ArrayList<Peer>) peerList.clone()) {

                if (p.equalsNonce(peer)) {
                    return p;
                }

            }

            peerList.add(peer);
        }
        return peer;
    }

    public static synchronized void removePeer(Peer peer) {
        synchronized (peerList) {
            peerList.remove(peer);
        }
    }

    public static synchronized void removeByIpAndPortPeer(Peer peer) {
        synchronized (peerList) {
            peerList.remove(peer);

            for (Peer p : getClonedPeerList()) {
                if (peer.equalsIpAndPort(p)) {
                    peerList.remove(p);
                }
            }

        }
    }

//
//    /**
//     * Nachricht darf noch nicht vorhanden sein!
//     */
//    public static void processNewMessage(Msg msg, boolean fromMe) {
//
//        msgs.add(msg);
//        //saver.saveMsgs(msgs);
//
//        //broadcast msg
//        if (System.currentTimeMillis() - msg.getSendTime() < 1000 * 60 * 30) {
//            String out = "msg:" + msg.toString();
//            String stringIdentity = msg.getChannel().stringIdentity();
//            for (Peer peer : (ArrayList<Peer>) peerList.clone()) {
//                if (peer.isConnected()) {
//                    msg.timesSend++;
//
//                    if (!peer.isPermittedAddress(stringIdentity)) {
//                        System.out.println("skipping client, because he doesnt listen to this address...");
//                    } else {
//                        //peer.writeTo(out);
//                        System.out.println("need to code broadcast!");
//                    }
//
//                }
//            }
//        }
//
//        //send to all listeners, but only if the message is for me (I know the private channel key)
//
//        if (msg.findPrivateKeyForChannel()) {
//
//
//            if (msg.getDecryptedContent().length() > 10000) {
//                System.out.println("Neue Nachricht für mich: " + msg.getChannel().name + " : " + msg.getDecryptedContent().substring(1, 10000));
//            } else {
//                System.out.println("Neue Nachricht für mich: " + msg.getChannel().name + " : " + msg.getDecryptedContent());
//            }
//
//            //Parse Message Content commands
//            MessageContent messageContent = msg.getMessageContent();
//
//
//
//            if (msg.channel.equals(SpecialChannels.MAIN)) {
//                //TODO: remove in official version
////                if (msg.getDecryptedContent().matches("(.*)clearxx(.*)")) {
////
////                    if ((System.currentTimeMillis() - lastAllMsgsCleared) > 30000) {
////
////                        for (Msg m : (ArrayList<Msg>) msgs.clone()) {
////                            if (m.timeStamp < msg.timeStamp) {
////                                msgs.remove(m);
////                            }
////                        }
////                        System.out.println("Cleaned all messages!!");
////                        lastAllMsgsCleared = System.currentTimeMillis();
////                    }
////                    saver.saveMsgs(msgs);
////                }
//            }
////
////            for (NewMessageListener listener : Main.listeners) {
////                listener.newMessage(msg, fromMe);
////            }
//        } else {
//            System.out.println("Neue Nachricht, nicht fuer mich...");
//        }
//
//
//    }
    public static ArrayList<Channel> getChannels() {
        return channels;
    }

    public static boolean addChannel(Channel c) {
        if (channels.contains(c)) {
            return false;
        }

        channels.add(c);
        saver.saveIdentities(channels);
        return true;
    }

    public static boolean removeChannel(Channel c) {
        if (!channels.contains(c)) {
            return false;
        }

        channels.remove(c);
        saver.saveIdentities(channels);
        return true;
    }

    public static LocalSettings getLocalSettings() {
        return localSettings;
    }

    public static ArrayList<Peer> getClonedPeerList() {
        return (ArrayList<Peer>) peerList.clone();
    }

    private static void startedUpSuccessful() {
        STARTED_UP_SUCCESSFUL = true;

////        //just a workaround, ToDo: bewerte Ips von einem Node
////        for (PeerTrustData ptd : peerTrusts) {
////            ptd.ips.clear();
////        }
        saveTrustData();

        File file = new File(Test.imageStoreFolder);
        file.mkdir();

        outboundthread = new Outboundthread();
        outboundthread.start();
        MessageDownloader.start();
        MessageVerifierHsqlDb.start();
        addKnowNodes();
        //ClusterBuilder.start();

//        new Thread() {
//
//            @Override
//            public void run() {
//                KnownChannels.updateMyChannels();
//
//                try {
//                    sleep(1000);
//                } catch (InterruptedException ex) {
//                }
//
//                if (System.currentTimeMillis() - Test.localSettings.lastSendAllMyChannels > 1000 * 60 * 60 * 24 * 2) {
//
//                    KnownChannels.sendAllKnownChannels();
//
//                    Test.localSettings.lastSendAllMyChannels = System.currentTimeMillis();
//                    Test.localSettings.save();
//
//                }
//
//                try {
//                    sleep(1000 * 60 * 60 * 4);
//                } catch (InterruptedException ex) {
//                }
//
//            }
//
//        }.start();
        WatchDog.start();

    }

    public static void savePeers() {
        if (Test.saver == null) {
            return;
        }
        if (peerList != null) {
            Test.saver.savePeerss((ArrayList<Peer>) peerList.clone());
        }

    }

    public static void saveTrustData() {
        if (Test.saver == null) {
            return;
        }
        if (peerTrusts != null) {
            Test.saver.saveTrustedPeers(peerTrusts);
        }

    }

    public static int getMyPort() {
        return MY_PORT;
    }

    private static void commitDatabase() {
        if (messageStore != null) {
            messageStore.commitDatabase();
        }
    }

    public static String stacktrace2String(Throwable thrwbl) {
        String ownStackTrace = "";
        ownStackTrace += thrwbl.getMessage() + "\n";
        for (StackTraceElement a : thrwbl.getStackTrace()) {
            ownStackTrace += a.toString() + "\n";
        }

        if (thrwbl.getCause() != null) {
            ownStackTrace += "caused by: " + thrwbl.getCause().getMessage() + "\n";
            for (StackTraceElement a : thrwbl.getCause().getStackTrace()) {
                ownStackTrace += a.toString() + "\n";
            }
        }

        return ownStackTrace;
    }

    public static void sendStacktrace(Throwable thrwbl) {

//        if (System.currentTimeMillis() - lastSentStackTrace < 1000*60*10) {
//            
//        }
        String out = "Stacktrace: \n";
        out += stacktrace2String(thrwbl);

        System.out.println("StackTrace: " + out);

//        stackTraceString += out + "\n#######################\n\n\n";
        try {
            PrintWriter outputWriter = new PrintWriter(new FileOutputStream("error.log", true));
            //PrintWriter outputWriter = new PrintWriter(new BufferedWriter(new FileWriter("error.log", true)));
            outputWriter.println("\n\n\n############## " + new Date() + " ###############\n\n\n" + out);
            outputWriter.close();
        } catch (IOException e) {
            //exception handling left as an exercise for the reader
        }

        Main.sendBroadCastMsg(out);
    }

    public static void sendStacktrace(String msg, Throwable thrwbl) {
        thrwbl.printStackTrace();
        String out = msg;
        out += stacktrace2String(thrwbl);
        Main.sendBroadCastMsg(out);
    }

    private static String formatInterval(final long l) {
        final long hr = TimeUnit.MILLISECONDS.toHours(l);
        final long min = TimeUnit.MILLISECONDS.toMinutes(l - TimeUnit.HOURS.toMillis(hr));
        final long sec = TimeUnit.MILLISECONDS.toSeconds(l - TimeUnit.HOURS.toMillis(hr) - TimeUnit.MINUTES.toMillis(min));
        final long ms = TimeUnit.MILLISECONDS.toMillis(l - TimeUnit.HOURS.toMillis(hr) - TimeUnit.MINUTES.toMillis(min) - TimeUnit.SECONDS.toMillis(sec));
        return String.format("%02d:%02d:%02d.%03d", hr, min, sec, ms);
    }

}
