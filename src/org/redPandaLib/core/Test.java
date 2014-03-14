/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redPandaLib.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.*;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.security.Security;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map.Entry;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.redPandaLib.Main;
import org.redPandaLib.SpecialChannels;
import org.redPandaLib.core.messages.RawMsg;
import org.redPandaLib.core.messages.TextMessageContent;
import org.redPandaLib.core.messages.TextMsg;
import org.redPandaLib.crypt.AddressFormatException;
import org.redPandaLib.crypt.Base58;
import org.redPandaLib.crypt.ECKey;
import org.redPandaLib.database.MessageStore;
import org.redPandaLib.services.ClusterBuilder;
import org.redPandaLib.services.MessageDownloader;
import org.redPandaLib.services.MessageVerifierHsqlDb;
//import org.redPandaLib.upnp.Portforward;

/**
 *
 * @author rflohr
 */
public class Test {

    static boolean DEBUG = true;
    static int DEBUG_LEVEL = 100;
    static boolean PORTFORWARD = false;
    static final int VERSION = 17;
    static int MY_PORT;
    static String MAGIC = "k3gV";
    public static ArrayList<Peer> peerList = null;
    public static ArrayList<Channel> channels;
    static long NONCE;
    static int outConns = 0;
    static int inConns = 0;
    //static ExecutorService threadPool = Executors.newCachedThreadPool();
    //static ExecutorService threadPool = Executors.newFixedThreadPool(MAX_CONNECTIONS * 2 + 5);
    //static ExecutorService threadPool2 = Executors.newCachedThreadPool();
    //static ExecutorService threadPool3 = Executors.newCachedThreadPool();
    static Random random = new Random();
    public static String clientSeed;
    public static int clientVersion = 0;
    public static long outBytes = 0;
    public static long inBytes = 0;
    public static SaverInterface saver;
    private static long lastAllMsgsCleared = 0;
    public static LocalSettings localSettings;
    private static ConnectionHandler connectionHandler;
    private static ConnectionHandlerConnect connectionHandlerConnect;
    public static boolean NAT_OPEN = false;
    public static ArrayList<PeerTrustData> peerTrusts = new ArrayList<PeerTrustData>();
    public static MessageStore messageStore;

    static {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
        //Security.insertProviderAt(new org.bouncycastle.jce.provider.BouncyCastleProvider(), 2);
    }
    public static boolean STARTED_UP_SUCCESSFUL = false;

    /**
     * @param args the command line arguments
     */
    public static void main(boolean listenConsole, SaverInterface saver) throws IOException {
        // TODO code application logic here

        Test.saver = saver;


        //loadChannels();


        byte[] bytes = new byte[10];
        random.nextBytes(bytes);
        clientSeed = bytes.toString();


        connectionHandler = new ConnectionHandler();
        connectionHandler.start();

        connectionHandlerConnect = new ConnectionHandlerConnect();
        connectionHandlerConnect.start();

        //threadPool.submit(new InboundThread());
        new InboundThread().start();
        //new Outboundthread().start();
        InputStreamReader inputStreamReader = new InputStreamReader(System.in, "UTF-8");
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        //cleanup
        Thread thread = new Thread() {
            @Override
            public void run() {

                long lastSaved = System.currentTimeMillis();

                while (true) {


                    if (System.currentTimeMillis() - lastSaved > 120 * 1000) {
                        try {
                            savePeers();
                            saveTrustData();
                            commitDatabase();
                            lastSaved = System.currentTimeMillis();
                        } catch (Exception e) {
                            System.out.println("oh oh, konnte peers nicht speichern... ");
                            e.printStackTrace();

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

                        if (p.getLastAnswered() > 1000 * 60 * 60 * 24 * 7 && peerList.size() > 3 && p.lastActionOnConnection != 0) {
                            removePeer(p);
                        }


                        if (p.getLastAnswered() > Settings.pingTimeout * 1000 || (p.isConnecting && p.getLastAnswered() > 5000)) {


                            if (p.isConnected() || p.isConnecting) {
                                if (DEBUG) {
                                    System.out.println(Settings.pingTimeout + " sec timeout reached! " + p.ip);
                                }
                                p.disconnect();
                            }

                        } else {

                            if (p.isConnected()) {

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
                                                p.writeBuffer.put((byte) 1);
                                            }
                                        }
                                    }

                                    p.cnt = 0;
                                } else {

                                    if (p.isFullConnected()) {
                                        p.ping();
                                    }

                                    if (p.isConnected() && !p.authed) {

                                        p.trustRetries++;

                                        if (p.trustRetries < 5) {
                                            //                                            System.out.println("Found a bad guy... doing nothing...: " + p.nonce);
                                        } else {
                                            p.trustRetries = 0;
                                            System.out.println("Found a bad guy... requesting new key: " + p.nonce);
                                            ConnectionHandler.sendNewAuthKey(p);
                                            //p.disconnect();
                                        }

                                        //System.out.println("Found a bad guy... requesting new key: " + p.nonce);
                                        //ConnectionHandler.sendNewAuthKey(p);
                                        //p.disconnect();
                                    }

                                }
                            }


                        }

                        //                        }
                    }
                }
            }
        };

        thread.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                Main.sendBroadCastMsg(stacktrace2String(e));

                throw new RuntimeException("PING thread died....");
            }
        });

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

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                Main.shutdown();
                System.out.println("saved peers and msg before shutdown...");
            }
        });

        System.out.println("shutdownhook added...");

        if (listenConsole) {
            while (true) {
                String readLine = bufferedReader.readLine();

                if (peerList == null) {
                    continue;
                }

                if (readLine.equals("")) {

                    String output = "";

                    output += "Status listenPort: " + MY_PORT + " NONCE: " + Test.NONCE + "\n";

                    int actCons = 0;

                    ArrayList<Peer> list = (ArrayList<Peer>) peerList.clone();
                    Collections.sort(list);

                    output += "IP:PORT \t\t\t\t\t\t Nonce \t\t\t Last Answer \t Alive \t retries \t LoadedMsgs \t Ping \t Authed \t PMSG\n";

                    for (Peer peer : list) {

                        if (peer.isConnected()) {
                            actCons++;
                        }


                        //System.out.println("Peer: " + InetAddress.getByName(peer.ip) + ":" + peer.port + " Nonce: " + peer.nonce + " Last Answer: " + (System.currentTimeMillis() - peer.lastActionOnConnection) + " Alive: " + peer.isConnected() + " LastGetAllMsgs: " + peer.lastAllMsgsQuerried + " retries: " + peer.retries + " LoadedMsgs: " + peer.loadedMsgs + " ping: " + (Math.round(peer.ping * 100) / 100.));
                        String a = "Peer:  IP: [" + peer.ip + "]:" + peer.port;
                        while (a.length() < 45) {
                            a += " ";
                        }

                        String b = " \t" + peer.nonce;
                        while (b.length() < 20) {
                            b += " ";
                        }
                        String c;
                        if (peer.lastActionOnConnection != 0) {
                            c = " \t" + (System.currentTimeMillis() - peer.lastActionOnConnection);
                        } else {
                            c = " \t-";
                        }
//                        while (c.length() < 15) {
//                            c += " \t";
//                        }

                        if (peer.getPeerTrustData() == null) {
                            output += "" + a + " \t " + b + "\t " + c + "\t " + peer.isConnected() + "\t " + peer.retries + "\t " + "--" + " \t " + (Math.round(peer.ping * 100) / 100.) + "\t " + peer.authed + "\t " + "--" + " \t" + peer.requestedMsgs + " \t" + "--" + "\n";
                        } else {
                            output += "" + a + " \t " + b + "\t " + c + "\t " + peer.isConnected() + "\t " + peer.retries + "\t " + peer.getLoadedMsgs().size() + " \t " + (Math.round(peer.ping * 100) / 100.) + "\t " + peer.authed + "\t " + peer.getPendingMessages().size() + " \t" + peer.requestedMsgs + " \t" + peer.getPeerTrustData().synchronizedMessages + "\n";
                        }


                    }

                    output += "Connected to " + actCons + " peers. (NAT type: " + (NAT_OPEN ? "open" : "closed") + ")" + "\n";
                    output += "Traffic: " + inBytes / 1024. + " kb / " + outBytes / 1024. + " kb." + "\n";

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
                    output += "Processed messages: " + MessageHolder.getMessageCount();

                    System.out.println(output);

                    continue;
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

                    System.out.println("ChannelList: ");
                    for (Channel c : channels) {
                        System.out.println("#" + c.getId() + " \t " + c.getName() + " \t - " + c.exportForHumans() + " (" + Channel.byte2String(c.getKey().getPubKey()) + " - " + Channel.byte2String(c.getKey().getPrivKeyBytes()) + ")");
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
                    ArrayList<TextMessageContent> messages = MessageHolder.getMessages(channel);

                    for (TextMessageContent msg : messages) {
                        System.out.println("from me: " + msg.isFromMe() + " content: " + msg.getText());
                    }


                    System.out.println("Ok, writing to channel: " + channel.getName() + " EXPORT: " + channel.exportForHumans() + "\nContent:");



                    readLine = bufferedReader.readLine();
                    Main.sendMessageToChannel(channel, readLine);
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


                if (readLine.equals("S")) {
                    System.out.println("started mining...");
                    StickMiner.start();





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

                if (readLine.equals("r")) {
                    System.out.println("removing all peers + trust data...");
                    synchronized (peerList) {
                        ArrayList<Peer> peers = peerList;
                        peerList = new ArrayList<Peer>();
                        peerTrusts = new ArrayList<PeerTrustData>();
                        for (Peer peer : peers) {
                            peer.disconnect();
                        }
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
                    System.exit(77);
                    return;
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
                    //threadPool = Executors.newFixedThreadPool(Settings. * 2 + 5);
                    continue;
                }

                if (readLine.equals("c")) {
                    System.out.println("closing all connections....");
                    for (Peer peer : (ArrayList<Peer>) peerList.clone()) {
                        peer.disconnect();
                    }
                    continue;
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
                    continue;
                }

                if (readLine.equals("-")) {
                    Settings.MIN_CONNECTIONS--;
                    Settings.MAX_CONNECTIONS--;
                    System.out.println("-1 peer: " + Settings.MIN_CONNECTIONS);
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

    private static void loadChannels() {
        channels = saver.loadIdentities();
        synchronized (channels) {
//            if (!channels.contains(SpecialChannels.MAIN)) {
//                Channel c = SpecialChannels.MAIN;
//                channels.add(c);
//            }

            if (Channel.getChannelById(0) == null) {
                Channel newMasterChannel = new Channel();
                newMasterChannel.key = new ECKey();
                newMasterChannel.name = "Master";
                newMasterChannel.id = 0;
                channels.add(newMasterChannel);
                System.out.println("Generated new Master Key!");
            }


            if (channels.size() < 2) {
                channels.add(Channel.generateNew("Mine 1"));
                channels.add(Channel.generateNew("Mine 2"));
                System.out.println("generated new channels!");
            } else {
                for (Channel c : channels) {
                    System.out.println("Channel privkeys to write to: " + c.getPrivateKey() + " Pub: " + Base58.encode(c.getKey().getPubKey()));
                }
            }
            saver.saveIdentities(channels);
        }
    }

    public static void broadcastMsg(RawMsg rawMsg) {
        for (Peer p : getClonedPeerList()) {
            if (p.isConnected() && p.isAuthed() && p.syncMessagesSince <= rawMsg.timestamp) {
                p.writeMessage(rawMsg);
                p.setWriteBufferFilled();




            }
        }

    }

    static class InboundThread extends Thread {

        @Override
        public void run() {



            if (DEBUG) {
                System.out.println("inbound thrad started...");
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
                    } catch (BindException e) {

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


                localSettings = saver.loadLocalSettings();
                loadChannels();


                NONCE = localSettings.nonce;


                connectionHandler.addServerSocketChannel(serverSocketChannel);
                startedUpSuccessful();
            } catch (IOException ex) {
                Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
            }



        }
    }

    static class Outboundthread extends Thread {

        @Override
        public void run() {


            Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread t, Throwable e) {
                    e.printStackTrace();
                    Main.sendBroadCastMsg(stacktrace2String(e));
                }
            });


            long loopCount = 0;

            while (true) {



                loopCount++;

                if (Settings.connectToNewClientsTill < System.currentTimeMillis()) {
                    try {
                        sleep(2000);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    continue;
                }


                if (Settings.initFullNetworkSync) {
                    //just a one time action
                    Settings.initFullNetworkSync = false;
                    System.out.println("init full sync, closing all connections");
                    ArrayList<Peer> clone = (ArrayList<Peer>) peerList.clone();
                    peerList = new ArrayList<Peer>();
                    for (Peer peer : clone) {
                        peer.disconnect();
                    }


                }


                if (peerList == null || NONCE == 0) {
                    try {
                        sleep(100);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    continue;
                }


                if (peerList.size() == 0) {
                    addKnowNodes();
                }

                ArrayList<Peer> clonedPeerList = (ArrayList<Peer>) peerList.clone();


                //Collections.shuffle(peerList);

                Collections.sort(clonedPeerList);

                int actCons = 0;
                int connectingCons = 0;
                for (Peer peer : clonedPeerList) {
                    if (peer.getLastAnswered() < Settings.pingTimeout * 1000 && peer.isConnected()) {
                        actCons++;
                    }
                    if (peer.isConnecting) {
                        connectingCons++;
                    }
                }

                if (connectingCons == clonedPeerList.size()) {
                    //hack!
                    for (Peer peer : clonedPeerList) {
                        peer.disconnect();
                    }
                    continue;
                }


//                if (DEBUG) {
//                    System.out.println("search new peers....");
//                }

                int cnt = 0;
                for (Peer peer : clonedPeerList) {

                    if (Settings.connectToNewClientsTill < System.currentTimeMillis()) {
                        break;
                    }

                    cnt++;

                    if (actCons >= Settings.MIN_CONNECTIONS) {
                        if (DEBUG) {
//                            System.out.println("peers " + actCons + " are enough...");
                        }
                        if (cnt == 1 && actCons >= Settings.MAX_CONNECTIONS) {
                            for (Peer p1 : clonedPeerList) {
                                if (p1.isConnected()) {
                                    p1.disconnect();
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
                        if (DEBUG) {
                            //System.out.println("already connected: " + peer.ip + ":" + peer.port);
                        }
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




                    if (peer.ip.length() <= 15 && Settings.IPV6_ONLY) {
                        peerList.remove(peer);
                        if (DEBUG) {
                            System.out.println("removed peer from peerList, no ipv6 address: " + peer.ip + ":" + peer.port);
                        }
                        continue;
                    }

                    if (peer.ip.length() > 15 && Settings.IPV4_ONLY) {
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


                    if (peerList.size() > 20) {
                        //(System.currentTimeMillis() - peer.lastActionOnConnection > 1000 * 60 * 60 * 4)
                        if (peer.retries > 10) {
                            //peerList.remove(peer);
                            removePeer(peer);
                            if (DEBUG) {
                                System.out.println("removed peer from peerList, too many retries: " + peer.ip + ":" + peer.port);
                            }
                            continue;
                        }


                    }

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
                            System.out.println("try to connect to new node: " + peer.ip + ":" + peer.port);
                        }
                        connectTo(peer);
                        actCons++;
//                        try {
//                            sleep(50);
//                        } catch (InterruptedException ex) {
//                            Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
//                        }

                    } else {
                        System.out.println("connect state: " + peer.connectAble + " -- " + peer.ip + ":" + peer.port);
                    }

                }

                try {
                    //sleep(60000 + random.nextInt(30000));
                    sleep(5000 + random.nextInt(3000));
                } catch (InterruptedException ex) {
                    Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
                }


            }



        }
    }

    private static void addKnowNodes() {

        for (String host : Settings.knownNodes) {
            try {
                findPeer(new Peer(InetAddress.getByName(host).getHostAddress(), Settings.STD_PORT));
            } catch (UnknownHostException ex) {
                System.out.println("Could not resolve hostname: " + host);
            }
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
            @Override
            public UncaughtExceptionHandler getUncaughtExceptionHandler() {
                return new UncaughtExceptionHandler() {
                    @Override
                    public void uncaughtException(Thread t, Throwable e) {
                        Main.sendBroadCastMsg(stacktrace2String(e));
                        peer.disconnect();
                    }
                };
            }

            @Override
            public void run() {


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
                } catch (IOException ex) {
                    if (DEBUG) {
                        System.out.println("outgoing con failed...");
                    }
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
        new Outboundthread().start();
        MessageDownloader.start();
        MessageVerifierHsqlDb.start();
        addKnowNodes();
        ClusterBuilder.start();
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
        String out = "Stacktrace: \n";
        out += stacktrace2String(thrwbl);
        Main.sendBroadCastMsg(out);
    }
}
