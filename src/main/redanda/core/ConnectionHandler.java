/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package main.redanda.core;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SignatureException;
import java.sql.ResultSet;
import java.sql.SQLException;

import kademlia.exceptions.ContentNotFoundException;
import kademlia.node.KademliaId;
import main.redanda.SpecialChannels;
import main.redanda.crypt.*;
import main.redanda.flaschenpost.Flaschenpost;
import main.redanda.services.MessageVerifierHsqlDb;
import main.redanda.services.MessageDownloader;

import java.lang.management.ThreadMXBean;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import main.redanda.ByteUtils;
import main.redanda.Main;

import main.redanda.core.messages.RawMsg;
import main.redanda.test.Start;

import static main.redanda.core.Test.*;

/**
 * @author robin
 */
public class ConnectionHandler extends Thread {

    public static final int READ_BUFFER_SIZE = 1024 * 50;
    public static final int WRITE_BUFFER_SIZE = 1024 * 500;
    public Selector selector;
    public static ArrayList<Socket> allSockets = new ArrayList<Socket>();
    //    public static ExecutorService threadPool = new ThreadPoolExecutor(1, 3, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());//Executors.newFixedThreadPool(4);
//    public static ExecutorService threadPool2 = new ThreadPoolExecutor(1, 6, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());//Executors.newFixedThreadPool(4);
    public static ExecutorService threadPool = Executors.newFixedThreadPool(20);
    public static ExecutorService threadPool2 = Executors.newFixedThreadPool(20);
    private boolean exit = false;
    public static long lastRun = 0;
    public static HashMap<ECKey, HashMap<PeerTrustData, Long>> ratingData = new HashMap<ECKey, HashMap<PeerTrustData, Long>>();
    private static Semaphore updateUploadLock = new Semaphore(2); //simultaneous upload slots

    public ConnectionHandler() {
        try {
            selector = Selector.open();
        } catch (IOException ex) {
            Logger.getLogger(ConnectionHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void exit() {
        exit = true;
        System.out.println("closing all connections....");
        for (Peer peer : (ArrayList<Peer>) peerList.clone()) {
            peer.disconnect("c");
        }

        System.out.println("Closing all associated channels...");
        for (SelectionKey key : selector.keys()) {
            try {
                key.channel().close();
            } catch (IOException ex) {
                Logger.getLogger(ConnectionHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        selector.wakeup();
        System.out.println("Closing selector...");
        try {
            selector.close();
        } catch (IOException ex) {
            Logger.getLogger(ConnectionHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
        selector.wakeup();
        interrupt();
    }

    public void addConnection(Peer peer, boolean connectionPending) {

        synchronized (allSockets) {
            allSockets.add(peer.getSocketChannel().socket());
        }
        //TODO: remove later when it will be saved...
        //peer.keyToIdHis = new HashMap<Integer, ECKey>();
        //peer.keyToIdMine = new ArrayList<Integer>();
        peer.requestedMsgs = 0;
        //peer.getPendingMessages().clear();//TODO muss weg, wenn das syncen richtig geht
        peer.writeBufferLock.lock();
        try {
            peer.readBuffer = ByteBuffer.allocate(READ_BUFFER_SIZE);
            peer.writeBuffer = ByteBuffer.allocate(WRITE_BUFFER_SIZE);
        } catch (Throwable e) {
            System.out.println("Speicher konnte nicht reserviert werden. Disconnect peer...");
            peer.disconnect("Speicher konnte nicht reserviert werden.");
        } finally {
            peer.writeBufferLock.unlock();
        }

        peer.firstCommandsProceeded = false;
        peer.lastActionOnConnection = System.currentTimeMillis();
        peer.setConnected(false);

        peer.requestedNewAuth = false;
        peer.authed = false;
        peer.writeKey = null;
        peer.readKey = null;
        peer.writeBufferCrypted = null;
        peer.readBufferCrypted = null;
        peer.trustRetries = 0;
        peer.removedSendMessages = new ArrayList<Integer>();
        peer.maxSimultaneousRequests = 1;
        peer.myInterestedChannelsCodedInHisIDs.clear();

        try {
            SocketChannel socketChannel = peer.getSocketChannel();
            socketChannel.configureBlocking(false);

            AbstractSelectableChannel socketC = socketChannel;

            selector.wakeup();
            //SelectionKey key = socketChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
            SelectionKey key;
            if (connectionPending) {
                peer.isConnecting = true;
                peer.setConnected(false);
                key = socketC.register(selector, SelectionKey.OP_CONNECT);
            } else {
                peer.isConnecting = false;
                peer.setConnected(true);
                key = socketC.register(selector, SelectionKey.OP_READ);
            }
            key.attach(peer);
            peer.setSelectionKey(key);
            selector.wakeup();
            //System.out.println("added con");
        } catch (IOException ex) {
            peer.disconnect("could not init connection....");
            return;
        }

        sendHeader(peer);
    }

    @Override
    public void run() {

        final String orgName = Thread.currentThread().getName();
        if (!orgName.contains(" ")) {
            Thread.currentThread().setName(orgName + " - IncomingHandler - Main");
        }

        while (!Main.shutdown && !exit) {

            lastRun = System.currentTimeMillis();
            Log.put("NEW KEY RUN - before select", 2000);
            int readyChannels = 0;
            try {
                readyChannels = selector.select();
            } catch (Exception e) {
                e.printStackTrace();
                //Main.sendBroadCastMsg("key was canceled...");
                try {
                    sleep(100);
                } catch (InterruptedException ex) {
                    Logger.getLogger(ConnectionHandler.class.getName()).log(Level.SEVERE, null, ex);
                }
                continue;
            }

            Log.put("NEW KEY RUN - after select", 2000);

//            try {
//                for (SelectionKey sk : selector.keys()) {
//                    Peer peer = (Peer) sk.attachment();
//                    if (peer == null) {
//                        continue;
//                    }
//                    if (peer.selectionKey != sk) {
//                        System.out.println("key was replaced.... - have to cancel key...");
//                    }
//                    if (!Test.peerList.contains(peer)) {
//                        System.out.println("Peer was removed! - have to cancel key...");
//                    }
//                }
//            } catch (ConcurrentModificationException e) {
//
//            }
            //System.out.println("" + selector.isOpen() + " " + selector.keys().size());
            Set<SelectionKey> selectedKeys = selector.selectedKeys();

            if (readyChannels == 0 && selectedKeys.isEmpty()) {
//                System.out.print(".");

//                    for (SelectionKey k : selector.keys()) {
//
//
//                        System.out.println("readyops: " + k.isAcceptable() + k.isConnectable() + k.isReadable() + k.isValid() + k.isWritable());
//
//                        if (k.readyOps() != 0) {
//                            k.cancel();
//                        }
//
//                    }
                try {
                    sleep(100);
                } catch (InterruptedException ex) {
                    Logger.getLogger(ConnectionHandler.class.getName()).log(Level.SEVERE, null, ex);
                }
                continue;
            }

            Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

            while (keyIterator.hasNext()) {

                if (exit) {
                    break;
                }

                SelectionKey key = keyIterator.next();
                keyIterator.remove();
                if (!key.isValid()) {
                    System.out.println("hmmm");
                    key.cancel();
                    //keyIterator.remove();
                    continue;
                }

                try {

                    if (key.isAcceptable()) {
                        // a connection was accepted by a ServerSocketChannel.

                        Log.put("incoming connection...", 12);

                        ServerSocketChannel s = (ServerSocketChannel) key.channel();
                        SocketChannel socketChannel = s.accept();
                        socketChannel.configureBlocking(false);
                        String ip = socketChannel.socket().getInetAddress().getHostAddress();

                        //System.out.println("IPS: " + socketChannel.socket().getInetAddress().getCanonicalHostName());
                        Peer peer1 = new Peer(ip, 0);
                        peer1.setSocketChannel(socketChannel);
                        addConnection(peer1, false);

//                            System.out.println("from ip: " + ip);
//                        SelectionKey newKey = socketChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
//                        key.attach(peer);
                        Test.NAT_OPEN = true;

                        continue;
                    }

                    Peer peer = (Peer) key.attachment();
                    if (peer == null) {
                        key.cancel();
                        continue;
                    }
//                    ByteBuffer readBuffer = peer.readBuffer;
//                    ByteBuffer writeBuffer = peer.writeBuffer;

                    if (!key.isValid()) {
                        peer.disconnect("key is invalid.");
                        continue;
                    }

                    if (key.isConnectable()) {

                        //System.out.println("dwdhjawdgawgd ");
                        boolean connected = false;
                        try {
                            connected = peer.getSocketChannel().finishConnect();
                        } catch (IOException e) {
                        } catch (SecurityException e) {
                        }
//                            System.out.println("finished!");

                        if (!connected) {
                            Log.put("connection could not be established...", 150);
                            key.cancel();
                            peer.setConnected(false);
                            peer.isConnecting = false;
                            peer.getSocketChannel().close();
                            continue;
                        }

                        //System.out.println("Connection established...");
                        key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                        peer.isConnecting = false;
                        peer.setConnected(true);

                        // a connection was established with a remote server.
                    } else if (key.isReadable()) {
//                        System.out.println("readable " + key.isValid());

                        //int read = peer.getSocketChannel().read(peer.readBuffer);
//                        System.out.println("ffffffff" + peer.readBuffer.toString());
                        int read = -2;

                        if (peer.readBufferCrypted == null) {
                            try {
                                read = peer.getSocketChannel().read(peer.readBuffer);
                            } catch (IOException e) {
                                key.cancel();
                                peer.disconnect("could not read...");
                                continue;
                            }

                            //check if this thread should exit!
                            if (interrupted()) {
                                return;
                            }

                        } else {

                            if (peer.readKey == null) {
                                System.out.println("ERROR 14276, encryption enabled, but I got no key?!?");
                                peer.disconnect("ERROR 14276");
                                continue;
                            }

                            read = peer.getSocketChannel().read(peer.readBufferCrypted);
                            //System.out.println("read bytes from socket " + read);

                            //check if this thread should exit!
                            if (interrupted()) {
                                return;
                            }

                            peer.readBufferCrypted.flip();

//                            if (peer.readBuffer.remaining() <= 1024) {
//                                System.out.println("############### readBuffer nearly full!! - " + peer.readBuffer.remaining());
//                            }

                            int readCryptBytes = Math.min(peer.readBufferCrypted.remaining(), peer.readBuffer.remaining());

                            byte[] buffer = new byte[readCryptBytes];
                            peer.readBufferCrypted.get(buffer);
                            peer.readBufferCrypted.compact();
                            byte[] decrypt = peer.readKey.decrypt(buffer);
                            if (peer.readBuffer != null) {
                                peer.readBuffer.put(decrypt);
                            } else {
                                peer.disconnect("readbuffer null");
                            }

                            //System.out.println("entschluesselte bytes: " + Utils.bytesToHexString(encrypt));
                        }
//                        System.out.println("gggggggggg" + peer.readBuffer.toString());
//                        peer.readBuffer.limit(read);

//                        System.out.println("adadadadad" + peer.readBuffer.toString());
                        if (read == -2) {
                            System.out.println("hgdjawhgdzawdtgzaud");
                        }

                        if (read > 0) {
                            Test.inBytes += read;
                            peer.receivedBytes += read;
                        }

                        if (read == 0) {

//                            if (readBuffer.hasRemaining()) {
//                                //System.out.println("dgqdgqzudg23c365nx367t34t53 " + peer.readBuffer + " " + key.isReadable() + " " + key.isValid());
//                                System.out.println("dafuq!");
//                                //closeConnection(peer, key);
//                                peer.disconnect("dafuq 2332");
//                                continue;
//                            }

                            if (peer.readBuffer.capacity() * 2 > 1024 * 1024 * 200) {
                                //throw new RuntimeException("buffer to high exiting!!!!");
//                                System.exit(-199);
                                peer.disconnect("readbuffer to high, prevent too high memory usage!");
                                continue;
                            }
                            System.out.println("we have to double the readbuffer... " + peer.readBuffer.capacity() * 2);


                            long allocatedMemory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
                            long presumableFreeMemory = Runtime.getRuntime().maxMemory() - allocatedMemory;

                            if (presumableFreeMemory < peer.readBuffer.capacity() * 2) {

                                System.out.println("not enough memory: only " + presumableFreeMemory / 1024 + " kbs free");

                                //we can not raise the buffer, lets wait some ms
                                //todo: prevent main io thread from sleeping?
                                //kill connection if more than 20 times here?
                                Thread.sleep(100);

                            } else {

                                ByteBuffer allocate = ByteBuffer.allocate(peer.readBuffer.capacity() * 2);
                                allocate.put(peer.readBuffer.array());
                                allocate.position(peer.readBuffer.position());
//                            allocate.limit(peer.readBuffer.limit());
                                //System.out.println("buffer voll?? " + peer.readBuffer.toString());
                                peer.readBuffer = allocate;
                                peer.lastBufferModified = System.currentTimeMillis();
//                                System.out.println("readBuffer full, generated new one with 2x larger size....");
                            }
                        } else if (read == -1) {
                            Log.put("closing connection " + peer.ip + ":" + peer.port + ": not readable! " + peer.readBuffer, 200);
                            peer.disconnect(" read == -1 from: " + peer.ip + ":" + peer.port);
                            Test.triggerOutboundthread();
                            key.cancel();
                        } else {


//                            System.out.println("before: " + peer.readBuffer);

                            //lets check if we can reduce the readbuffer
                            if (peer.readBuffer.capacity() > peer.readBuffer.position() + READ_BUFFER_SIZE && peer.readBuffer.position() != peer.readBuffer.capacity() && peer.lastBufferModified < System.currentTimeMillis() - 1000L * 5L) {

                                ByteBuffer allocate = ByteBuffer.allocate(Math.max(peer.readBuffer.position() + READ_BUFFER_SIZE, READ_BUFFER_SIZE));


                                System.out.println("we reduce the readbuffer... " + Math.max(peer.readBuffer.position(), READ_BUFFER_SIZE));

                                peer.readBuffer.flip();
                                allocate.put(peer.readBuffer);

//                                allocate.put(peer.readBuffer.array());
//                                allocate.position(peer.readBuffer.position());
//                                allocate.limit(peer.readBuffer.limit());


                                peer.readBuffer = allocate;
                                peer.lastBufferModified = System.currentTimeMillis();
                            }
//                            System.out.println("after: " + peer.readBuffer);

                            peer.lastActionOnConnection = System.currentTimeMillis();
                            //System.out.println("setting lastActionOn: " + peer.nonce);
//                            System.out.println("read bytes: " + read);

//                                System.out.println("a: " + read + " ");
//                            System.out.println(new String(.array(), 0, read));
                            //peer.getSocketChannel().write(allocate);
                            int pos = peer.readBuffer.position();
                            int lim = peer.readBuffer.limit();
                            int parsedBytes = 0;

                            peer.readBuffer.flip(); //switch buffer for reading
//                                System.out.println("incoming string: " + new String(peer.readBuffer.array(), peer.readBuffer.arrayOffset(), peer.readBuffer.limit()));

                            if (!peer.firstCommandsProceeded) {

                                if (peer.readBuffer.limit() > 14 - 8 + KademliaId.ID_LENGTH / 8) { //start needs to be 15 bytes!
                                    String magic = readString(peer.readBuffer, 4);
                                    int version = (int) peer.readBuffer.get();

                                    byte[] nonceBytes = new byte[KademliaId.ID_LENGTH / 8];
                                    peer.readBuffer.get(nonceBytes);

                                    KademliaId nonce = new KademliaId(nonceBytes);

//                                    byte[] nonce = new byte[10];
//                                    peer.readBuffer.get(nonce);
                                    int port = readUnsignedShort(peer.readBuffer);

                                    Log.put("Verbindungsaufbau (" + peer.ip + "): " + magic + " " + version + " " + nonce + " " + port, 10);

                                    if (magic != null && magic.equals(Test.MAGIC) && version == Test.VERSION) {
                                        //System.out.println("ready! " + magic);

                                        peer.port = port;
                                        peer.nodeId = nonce;

                                        if (nonce.equals(Test.NONCE)) {
                                            Log.put("found myself!" + peer.ip + ":" + peer.port, 200);

                                            Test.removeByIpAndPortPeer(peer);
                                            peer.disconnect(" found myself");
                                            continue;
                                        }

                                        //addPeer(new Peer(socket.getInetAddress().getHostAddress(), port));
//                                        Peer newPeer = new Peer(peer.ip, port);
//                                        newPeer.nonce = nonce;
                                        boolean found = false;
//                                            boolean closeNew = false;

                                        Test.peerListLock.lock();

                                        for (Peer pfromList : Test.getClonedPeerList()) {

                                            if (pfromList.equals(peer)) {
                                                continue;
                                            }

                                            if (peer.equalsNonce(pfromList)) {
                                                found = true;

//                                                        System.out.println("nonce1: " + peer.nonce + " nonce2: " + pfromList.nonce);
                                                if (pfromList.isConnected() || pfromList.isConnecting) {
                                                    Log.put("Peer already in peerlist... closing old connection...", 22);
                                                    pfromList.disconnect(" closing old con");
//                                                            System.out.println("Peer is already connected, closing new connection");
//                                                            peer.disconnect();
//                                                            closeNew = true;
//                                                            break;
                                                }
//
//                                                        pfromList.setSocketChannel(peer.getSocketChannel());
//                                                        pfromList.ip = peer.ip;
//                                                        pfromList.port = port;
//                                                        pfromList.readBuffer = readBuffer;
//                                                        pfromList.writeBuffer = writeBuffer;
//                                                        pfromList.setSelectionKey(peer.getSelectionKey());
//                                                        key.attach(pfromList);
//                                                        //Test.peerList.remove(peer);//remove new generated peer
//                                                        peer = pfromList;

                                                peer.migratePeer(pfromList);

                                                //ToDo: block
                                                Test.removePeer(pfromList);
                                                Test.findPeerNonce(peer);

//                                                        System.out.println("dbwdwgzdw " + peer.isConnected());
//
//                                                        System.out.println("Migrated this connection to existend peer object...");
//                                                        System.out.println("since: " + pfromList.lastAllMsgsQuerried);
                                                break;
                                            }

                                        }

                                        if (!found) {
//                                                    System.out.println("Peer not found in list, adding new peer...");
                                            //Test.peerList.add(peer);
                                            Test.findPeerNonce(peer);
//                                                    System.out.println("IP: " + peer.ip + " nonce: " + peer.nonce);
                                        }

//                                                if (!closeNew) {
                                        peer.port = port;
                                        peer.connectedSince = System.currentTimeMillis();
                                        peer.connectAble = 1;
                                        peer.lastActionOnConnection = System.currentTimeMillis();
                                        peer.firstCommandsProceeded = true;

//                                                }
                                        Test.peerListLock.unlock();
//                                        if (Settings.lightClient) {
////                                if (!found) { //new peer
//                                            String out = ADDFILTERADDRESS + ":";
//                                            for (Channel c : Test.getChannels()) {
//                                                out += c.stringIdentity();
//                                                out += ",";
//                                            }
//                                            out = out.substring(0, out.length() - 1);
//                                            writeString(out);
////                                }
//                                        }
//                                        //Peer nicley connected!
//                                        writeString("getMsgs:" + peer.lastAllMsgsQuerried);

//                                            if (!closeNew) {
                                        //peer.retries = 0;
                                        peer.writeBufferLock.lock();
                                        //first ping...
                                        peer.lastPinged = System.currentTimeMillis();
                                        peer.writeBuffer.put((byte) 100); //ping

                                        //init auth
                                        boolean foundAuthKey = false;
                                        for (PeerTrustData pt : Test.peerTrusts) {
                                            if (pt.nodeId.equals(peer.nodeId)) {
                                                foundAuthKey = true;

//                                                        System.out.println("authkey found for nonce: " + peer.nonce);
                                                Random r = new SecureRandom();

                                                byte[] toEncrypt = new byte[32];
                                                r.nextBytes(toEncrypt);

                                                peer.writeBuffer.put((byte) 52);
                                                peer.writeBuffer.put(toEncrypt);

                                                peer.toEncodeForAuthFromMe = toEncrypt;
                                                //peer.peerTrustData = pt;

//                                                        System.out.println("request authentication...");
                                                //break;
                                            }
                                        }

                                        if (!foundAuthKey) {
                                            sendNewAuthKey(peer);
                                        }

                                        //auth end
                                        peer.writeBufferLock.unlock();
                                        peer.setWriteBufferFilled();

//                                            }
                                    } else {

                                        peer.port = port;
                                        System.out.println("WRONG magic and or protocol - VERSION, deleting peer.... " + peer.ip + " " + peer.port);
                                        Test.removeByIpAndPortPeer(peer);
                                        peer.disconnect("WRONG magic and or protocol");

                                    }
                                    parsedBytes = 15 - 8 + KademliaId.ID_LENGTH / 8;
                                }

                            } else {

                                int localParsedBytes = 0;
                                while (peer.isConnected()) {
                                    byte command = peer.readBuffer.get();

                                    //System.out.println(peer.ip + "Command: " + command);
                                    try {
                                        localParsedBytes = parseCommands(command, peer, parsedBytes, peer.writeBuffer, peer.readBuffer);
                                    } catch (Throwable thb) {
                                        System.out.println("/////////////////// catched");
                                        thb.printStackTrace();
                                    }

                                    //System.out.println(peer.ip + "parsed bytes: " + localParsedBytes);
                                    if (peer.isCryptedConnection()) {
                                        peer.parsedCryptedBytes += localParsedBytes;
                                    }

                                    if (localParsedBytes == -1) {
                                        parsedBytes = 1; //somit ist nach dem naechste befehl alles auf 0 gesetzt! Buffer wurde entschluesselt... CMD 55!
                                    }

                                    parsedBytes += localParsedBytes;

                                    if (localParsedBytes == 0 || !peer.readBuffer.hasRemaining()) {
                                        break;
                                    }

                                    if (peer.readBuffer == null) {
                                        peer.disconnect("dafuq? 37456565");
                                        System.out.println("dafuq? 37456565");
                                    } else {
                                        peer.readBuffer.position(parsedBytes);
                                    }

                                    //System.out.println("switched position of readbuffer: " + parsedBytes);
//                                        System.out.println("parseBytes: " + parsedBytes + " " + peer.readBuffer);
//                                        System.out.println("parseBytes: " + parsedBytes + " " + peer.readBuffer);
                                }

                            }

                            peer.setWriteBufferFilled(); //TODO: nachher besser machen?

                            //System.out.println("bytes in readbuffer: " + peer.readBuffer.remaining() + " parsedbytes: " + parsedBytes);
                            //int parseBytes = peer.readBuffer.limit() - peer.readBuffer.arrayOffset();
                            peer.readBuffer.position(parsedBytes);

                            peer.readBuffer.compact();

                            //peer.readBuffer.flip(); //switch buffer for writing
                            //readBuffer.limit(readBuffer.capacity());
                            //peer.readBuffer.limit(lim);
                            //peer.readBuffer.position(pos - parsedBytes);
                        }

                    } else if (key.isWritable()) {

                        //System.out.println("key is writeAble");
                        peer.writeBufferLock.lock();

                        if (peer.writeBuffer == null) {
                            try {
                                if (key.isValid()) {
                                    key.interestOps(SelectionKey.OP_READ);
                                }
                            } catch (CancelledKeyException e) {
                            }
                            peer.writeBufferLock.unlock();
                            Log.put("writebuffer was null and writeable?", 0);
                            continue;
                        }

//                                int position = writeBuffer.position();
//                                int limit = writeBuffer.limit();
                        int writtenBytes = -1;
                        boolean remainingBytes = false;
                        peer.writeBuffer.flip();

                        if (peer.writeBufferCrypted == null) {
                            remainingBytes = peer.writeBuffer.hasRemaining();
                        } else {
                            peer.writeBufferCrypted.flip();
                            remainingBytes = (peer.writeBuffer.hasRemaining() || peer.writeBufferCrypted.hasRemaining());
                            peer.writeBufferCrypted.compact();
                        }

                        //System.out.println("remainingBytes: " + remainingBytes);
                        //switch buffer for reading
                        if (!remainingBytes) {
                            key.interestOps(SelectionKey.OP_READ);
                            //System.out.println("removed OP_WRITE");
                        } else {
                            //System.out.println("write from buffer...");
                            writtenBytes = peer.writeBytesToPeer(peer.writeBuffer);
                        }
                        peer.writeBuffer.compact();
//                                peer.writeBuffer.flip(); //switch buffer for writing
//                                writeBuffer.limit(writeBuffer.capacity());
//                            writeBuffer.limit(limit);
//                            writeBuffer.position(position - writtenBytes);


                        //lets check if we can reduce the writeBuffer
                        if (peer.writeBuffer.capacity() > peer.writeBuffer.position() + WRITE_BUFFER_SIZE && peer.writeBuffer.position() != peer.writeBuffer.capacity() && peer.lastBufferModified < System.currentTimeMillis() - 1000L * 5L) {

                            ByteBuffer allocate = ByteBuffer.allocate(Math.max(peer.writeBuffer.position() + WRITE_BUFFER_SIZE, WRITE_BUFFER_SIZE));


                            System.out.println("we reduce the writeBuffer... " + Math.max(peer.writeBuffer.position(), WRITE_BUFFER_SIZE));

                            peer.writeBuffer.flip();
                            allocate.put(peer.writeBuffer);

                            peer.writeBuffer = allocate;
                            peer.lastBufferModified = System.currentTimeMillis();
                        }


                        //System.out.println("wrote from buffer... " + writtenBytes + " ip: " + peer.ip);
                        if (writtenBytes > 0) {
                            Test.outBytes += writtenBytes;
                            peer.sendBytes += writtenBytes;
                        }
                        peer.writeBufferLock.unlock();

                    }

                } catch (IOException e) {
                    key.cancel();
                    Peer peer = ((Peer) key.attachment());
                    System.out.println("error! " + peer.ip + ":" + peer.port);
                    e.printStackTrace();
                    peer.disconnect("IOException");
                } catch (Throwable e) {
                    key.cancel();
                    Peer peer = ((Peer) key.attachment());
                    System.out.println("Catched fatal exception! " + peer.ip + ":" + peer.port);
                    e.printStackTrace();
                    //peer.disconnect(" IOException 4827f3fj");
                    Test.sendStacktrace("Fatal exception!", e);
                    peer.disconnect("Fatal exception");
                }

            }
        }

        System.out.println("ConnectionHandler thread died...");

    }

    public static void sendNewAuthKey(Peer peer) {
        peer.peerTrustData = new PeerTrustData();

        Random r = new SecureRandom();

        byte[] myPart = new byte[16];
        r.nextBytes(myPart);

//        System.out.println("higher: " + peer.peerIsHigher());

        if (peer.peerIsHigher()) {
            System.arraycopy(myPart, 0, peer.peerTrustData.authKey, 0, 16);
        } else {
            System.arraycopy(myPart, 0, peer.peerTrustData.authKey, 16, 16);
        }

        peer.requestedNewAuth = true;

        //request new
        peer.writeBufferLock.lock();
        peer.writeBuffer.put((byte) 51);
        peer.writeBuffer.put(myPart);
        peer.writeBufferLock.unlock();

        //peer.authed = true;
        Log.put("requested new authkey... " + peer.nodeId, 150);

    }

    private int parseCommands(byte command, final Peer peer, int parsedBytes, final ByteBuffer writeBuffer, final ByteBuffer readBuffer) {

        Log.put("Command byte: " + command + " ip:" + peer.ip, 150);

        if (peer.getPeerTrustData() == null || !peer.authed) {

            if (command == (byte) 1 || command == (byte) 2 || command == (byte) 10 || command == (byte) 51 || command == (byte) 52 || command == (byte) 53 || command == (byte) 55 || command == Command.PING || command == Command.PONG) {
                // erlaubte befehle welche ohne verschluesselung ausgefuehrt werden duerfen
            } else {
                System.out.println("PEER wanted to send a command (" + command + ") which is only allowed after auth. : " + peer.ip);
                peer.disconnect(" command but must be crypted");
                return 0;
            }

        }

        //System.out.println(peer.ip + " incoming command: " + command);
        if (command == Command.GET_PEER_LIST) {
            Log.put("Command: get PeerList", 8);

            ByteBuffer tempWriteBuffer = ByteBuffer.allocate(1024 * 10);
            tempWriteBuffer.put((byte) 2);

            ArrayList<Peer> clonedPeerList = Test.getClonedPeerList();
            ArrayList<Peer> validPeers = new ArrayList<Peer>();
            ArrayList<Peer> validPeersIPv6 = new ArrayList<Peer>();
            for (Peer p1 : clonedPeerList) {

                if (p1.equals(peer)) {
                    continue;
                }

                if (!p1.isConnected() || p1.writeBufferCrypted == null || !p1.authed) {
                    continue;
                }

                if (p1.connectAble == 1 && p1.ip.split("\\.").length == 4) {
                    if (validPeers.size() < 254) { //currently only 254 peers possible since the counter for it is just a byte. probably just send two seperated lists
                        validPeers.add(p1);
                    }
                }
                if (p1.connectAble == 1 && p1.ip.split("\\.").length != 4) {
                    validPeersIPv6.add(p1);
                }

            }

            tempWriteBuffer.put((byte) validPeers.size());
            tempWriteBuffer.put((byte) 0); // no ipv6 addresses yet

            for (Peer p1 : validPeers) {
                String[] split = p1.ip.split("\\.");

                if (split.length == 4) { // muss nicht mehr gepruft werden, siehe valid peers...

                    for (String a : split) {
                        int asdd = Integer.parseInt(a);
                        //System.out.println("Int: " + (int) ((byte) asdd & 0xFF));
                        tempWriteBuffer.put((byte) asdd);
                    }

                    putUnsignedShot(tempWriteBuffer, p1.port);

                }

            }

            for (Peer p1 : validPeersIPv6) {

                if (p1.ip.length() > 250) {
                    System.out.println("konnte IPv6 nicht sharen, zu lang: " + p1.ip);
                    continue;
                }

                tempWriteBuffer.put((byte) 10);
                tempWriteBuffer.put((byte) p1.ip.length());
                tempWriteBuffer.put(p1.ip.getBytes());
                putUnsignedShot(tempWriteBuffer, p1.port);
            }

            tempWriteBuffer.flip();
            peer.writeBufferLock.lock();
            writeBuffer.put(tempWriteBuffer);
            peer.writeBufferLock.unlock();
            return 1;
        } else if (command == Command.PEERLIST) {
            Log.put("Command: peerList", 8);

            if (2 > readBuffer.remaining()) {
                return 0;
            }

            int ipv4addresses = (int) readBuffer.get() & 0xFF;
            int ipv6addresses = (int) readBuffer.get() & 0xFF;

            int needBytes = ipv4addresses * 6 + ipv6addresses * 18;

            if (needBytes > readBuffer.remaining()) {
//                System.out.println("Bytesremaining: " + readBuffer.remaining() + " ipv4 addresses: " + ipv4addresses);
                return 0;
            }

//            System.out.println("parse " + ipv4addresses + " ipv4 addresses....");
            for (int i = 0; i < ipv4addresses; i++) {

                int ipblock1 = (int) readBuffer.get() & 0xFF;
                int ipblock2 = (int) readBuffer.get() & 0xFF;
                int ipblock3 = (int) readBuffer.get() & 0xFF;
                int ipblock4 = (int) readBuffer.get() & 0xFF;
                int port = readUnsignedShort(readBuffer);
                String ip = ipblock1 + "." + ipblock2 + "." + ipblock3 + "." + ipblock4;
                Peer peerToAdd = new Peer(ip, port);

                boolean alreadyInList = false;

                ArrayList<Peer> clonedPeerList = getClonedPeerList();

                for (Peer tpeer : clonedPeerList) {

                    if (tpeer.equalsIpAndPort(peerToAdd)) {
                        alreadyInList = true;
                    }

                }

//                System.out.println("asdasdasd " + alreadyInList + " " + ip + " " +port);
                if (!alreadyInList) {
                    Test.messageStore.insertPeerConnectionInformation(ip, port, 0, System.currentTimeMillis());
                }

            }

            return 1 + 2 + needBytes;

        } else if (command == Command.SYNC) {
            Log.put("Command: sync!", 8);
            if (2 > readBuffer.remaining()) {
                return 0;
            }

            final long timestamp = readBuffer.getLong();

            if (timestamp == -1) {
                peer.syncMessagesSince = System.currentTimeMillis() - 1000 * 60 * 60 * 24 * 7;
                //full sync
                syncAllMessagesSince(peer.syncMessagesSince, peer);
                return 1 + 8;
            }

            int howMuchAddresses = readBuffer.get();

            if (howMuchAddresses == 0) {

                peer.syncMessagesSince = timestamp;

                long myTime = System.currentTimeMillis() - 1000 * 60 * 60 * 24 * 7;
                if (Settings.SUPERNODE) {
                    myTime = 0;
                }
                syncAllMessagesSince(Math.max(timestamp, myTime), peer);
            }

            //obsolet??
            if (howMuchAddresses * 33 > readBuffer.remaining()) {
                return 0; //need more bytes in readBuffer...
            }

            //obsolet??
            byte[] address = new byte[33];
            for (int i = 0; i < howMuchAddresses; i++) {
                readBuffer.get(address);
                //ECKey eCKey = new ECKey(null, address);

                ECKey eCKey = new ECKey(null, address, true);

                System.out.println("Address to filter: " + eCKey);
            }

            return 1 + 8 + 1 + howMuchAddresses * 33;
        } else if (command == (byte) 4) {
            Log.put("Command: address to id", 8);

            if (37 > readBuffer.remaining()) {
                return 0;
            }

            byte[] address = new byte[33];
            readBuffer.get(address);
            //ECKey eCKey = new ECKey(null, address);
            ECKey eCKey = new ECKey(null, address, true);
            int id = readBuffer.getInt();
            //System.out.println("address to id: " + Channel.byte2String(eCKey.getPubKey()) + " id: " + id);

            //eCKey.database_id = Test.messageStore.getPubkeyId(eCKey);
//            if (peer.keyToIdHis.get(id) == null) {
//                System.out.println("Warning, index not found... closing connection...");
//                peer.disconnect();
//                return 1 + 37;
//            }
            //peer.keyToIdHis.put(id, eCKey);
            peer.getKeyToIdHis().put(id, eCKey);

            return 1 + 37;
        } else if (command == (byte) 5) {
            Log.put("Command: have message", 10);

            if (21 > readBuffer.remaining()) {
                return 0;
            }

            int pubkey_id_local = readBuffer.getInt();
            byte public_type = readBuffer.get();
            long timestamp = readBuffer.getLong();
            int nonce = readBuffer.getInt();
            int messageId = readBuffer.getInt();

            //peer.peerTrustData.introducedMessages++;
            ECKey id2KeyHis = peer.getId2KeyHis(pubkey_id_local);

            //System.out.println("Key for message: " + Channel.byte2String(id2KeyHis.getPubKey()));
            if (id2KeyHis == null) {
                System.out.println("WARNING: key not found -.- " + pubkey_id_local + " " + peer.getKeyToIdHis().size() + " --- disconnect");

//                for (Entry<Integer, ECKey> entry :peer.keyToIdHis.entrySet()) {
//                    
//                    System.out.println("");
//                    
//                }
                peer.disconnect("key not found 32323.");

                return 1 + 21;
            }

            RawMsg rawMsg = new RawMsg(id2KeyHis, timestamp, nonce);
            rawMsg.public_type = public_type;
            //boolean contains = MessageHolder.contains(rawMsg);

            //System.out.println("Peer got following msg: " + timestamp + " " + channelId + " " + nonce + " " + messageId);
            //if (!contains) {
//                System.out.println("found msg i want to have...");
            boolean dontAddMessage = false;

            //Check if I realy want this message, perhaps I removed the channel or the other side is spamming me.
            // or i just wrote to a channel i'm not listening to, like the main channel!!!
            if (Settings.lightClient) {

                if (!peer.myInterestedChannelsCodedInHisIDs.contains(pubkey_id_local)) {

                    //int my_pubkeyId = Test.messageStore.getPubkeyId(id2KeyHis);
                    if (Test.channels.contains(new Channel(id2KeyHis, null))) {
                        peer.myInterestedChannelsCodedInHisIDs.add(pubkey_id_local);
                        Log.put("added !! " + pubkey_id_local + " node: " + peer.nodeId, 100);
                    } else {

                        int pubkeyId = Test.messageStore.getPubkeyId(id2KeyHis);

                        peer.writeBufferLock.lock();
                        ECKey k = id2KeyHis;
                        if (!peer.peerTrustData.keyToIdMine.contains(pubkeyId)) {
                            peer.peerTrustData.keyToIdMine.add(pubkeyId);
                            //int indexOf = keyToIdMine.indexOf(k);
                            int indexOf = pubkeyId;
                            peer.writeBuffer.put((byte) 4);
                            peer.writeBuffer.put(k.getPubKey());
                            peer.writeBuffer.putInt(indexOf);
                            Log.put("key introduced", 0);
                        }

                        peer.writeBuffer.put((byte) 62);
                        peer.writeBuffer.putInt(pubkeyId);
                        peer.writeBufferLock.unlock();

                        dontAddMessage = true;

                        Log.put("channel was removed, sending to node.... " + pubkeyId + " node: " + peer.nodeId, 0);

                        //There may be still messages to sync to other nodes, we have to remove them!!
                        //ToDo: replace this hack!
                        Test.messageStore.removeMessageToSend(peer.getPeerTrustData().internalId);

                    }

                }

                Log.put("contains!!! " + pubkey_id_local + " node: " + peer.nodeId, 100);
            }

            if (!dontAddMessage) {

                HashMap<PeerTrustData, Long> perKey = ratingData.get(id2KeyHis);
                if (perKey == null) {
                    perKey = new HashMap<PeerTrustData, Long>();
                    ratingData.put(id2KeyHis, perKey);
                }

////                Long time = perKey.get(peer.peerTrustData);
////                if (time != null && time != 0L) {
////                    //there is already a time, we have to evaluate last round...
////                    System.out.println("there is already a time, we have to evaluate last round...");
////
////                    ArrayList<PeerTrustData> keySet = new ArrayList<PeerTrustData>(perKey.keySet());
////
////                    final HashMap<PeerTrustData, Long> perKeyFinal = perKey;
////                    Collections.sort(keySet, new Comparator<PeerTrustData>() {
////
////                        @Override
////                        public int compare(PeerTrustData t, PeerTrustData t1) {
////                            return (int) (perKeyFinal.get(t) - perKeyFinal.get(t1));
////                        }
////                    });
////
////                    int valueToAdd = 0;
////
////                    for (PeerTrustData ptd : keySet) {
////
////                        if (ptd == peer.peerTrustData) {
////                            continue;
////                        }
////
////                        long diff = peer.peerTrustData.rating - ptd.rating;
////                        double value1 = 10 / (1 + Math.pow(10, (float) diff / 8)) * 20;
////
////                        int value = (int) Math.floor(value1);
////
////                        if (value == 0) {
////                            value = 1;
////                        }
////
////                        ptd.rating += -value;
////                        valueToAdd += value;
////
////                        System.out.println(perKey.get(ptd) + " - value: " + (-value) + " -  " + ptd.rating + "    - " + ptd.ips);
////                        //reset round!
////                        perKey.put(ptd, 0L);
////                    }
////
////                    peer.peerTrustData.rating += valueToAdd;
////                    System.out.println(perKey.get(peer.peerTrustData) + " - Winner: " + (valueToAdd) + " -  " + peer.peerTrustData.rating + "    - " + peer.peerTrustData.ips);
////
////                }
////
////                perKey.put(peer.peerTrustData, System.currentTimeMillis());
                peer.addPendingMessage(messageId, rawMsg);
                //Test.messageStore.addMsgIntroducedToMe(peer.getPeerTrustData().internalId, messageId);
                peer.getPeerTrustData().synchronizedMessages++;

                Log.put("added pending msg... + " + peer.ip + " - " + messageId, 60);
//                synchronized (writeBuffer) {
//                    writeBuffer.put((byte) 6);
//                    writeBuffer.putInt(messageId);
//                }
                //}
            }
            return 1 + 21;

        } else if (command == (byte) 6) {
            Log.put("Command: get message content " + peer.ip + ":" + peer.port, -2);
            if (4 > readBuffer.remaining()) {
                return 0;
            }

            final int id = readBuffer.getInt();

            Runnable runnable = new Runnable() {

                @Override
                public void run() {

                    final String orgName = Thread.currentThread().getName();
                    if (!orgName.contains(" ")) {
                        Thread.currentThread().setName(orgName + " - send Message Thread");
                    }

                    RawMsg rawMsg = null;
                    rawMsg = MessageHolder.getRawMsg(id);
                    Log.put("message to fetch by id: " + id, -2);
                    if (rawMsg == null) {
                        System.out.println("HM diese Nachricht habe ich nicht, id: " + id + " ip: " + peer.getIp());
                        return;
                    }
                    //ToDo: msg might have been deleted due to false signature...

                    if (rawMsg.getContent() == null || rawMsg.getContent().length > 1024 * 201) {
                        Main.sendBroadCastMsg("ohoh, rawMsg.getContent() is null or too long...");
                        return;
                    }

                    int cnt = 0;
                    peer.writeBufferLock.lock();
                    while (peer.writeBuffer != null && peer.writeBuffer.remaining() < rawMsg.getContent().length + 1 + 4 + 72) {
                        if (!peer.isConnected()) {
                            peer.writeBufferLock.unlock();
                            return;
                        }
                        cnt++;
                        try {
                            peer.writeBufferLock.unlock();
                            sleep(1000);
                        } catch (InterruptedException ex) {
                            Logger.getLogger(ConnectionHandler.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        System.out.println("Buffer full, cnt: " + cnt + " contentlen: " + rawMsg.getContent().length + " remaining: " + ((peer.writeBuffer != null) ? peer.writeBuffer.remaining() : "null"));
                        if (cnt == 100) {
                            Main.sendBroadCastMsg("CODE 2kcvh5: buffer to small..." + cnt);
                            peer.writeBufferLock.unlock();
                            return;
                        }
                        peer.writeBufferLock.lock();
                    }

                    if (peer.writeBuffer == null) {
                        System.out.println("writebuffer was null while, wanted to write msg to node");
                        return;
                    }

                    writeBuffer.put((byte) 7);
                    writeBuffer.putInt(id);
                    if (rawMsg.getSignature().length != 72) {
                        throw new RuntimeException("hreuhrwuirhew signature got wrong length!!!");
                    }

                    writeBuffer.put(rawMsg.getSignature());
                    writeBuffer.putInt(rawMsg.getContent().length);

//                        if (writeBuffer.remaining() < rawMsg.getContent().length) {
//                            ByteBuffer allocate = ByteBuffer.allocate(writeBuffer.capacity() + rawMsg.getContent().length + 30);
//                            allocate.put(writeBuffer.array());
//                            //allocate.position(writeBuffer.position());
//                            peer.writeBuffer = allocate;
//                        }
                    //System.out.println("SIZE:" + writeBuffer.remaining() + " " + rawMsg.getContent().length);
                    //System.out.println("bytes: " + rawMsg.getContent().length);
                    //NullPointer thrown in HeapByteBuffer, dont know why, so hack!
                    try {
                        //todo: catch via another way?
                        peer.writeBuffer.put(rawMsg.getContent());
                    } catch (Throwable e) {
                        Main.sendBroadCastMsg("prevented exception 73632.");
                        peer.disconnect("prevented exception 73632");
                    }
                    peer.writeBufferLock.unlock();
                    peer.setWriteBufferFilled();

                    Log.put("wrote msg to peer " + peer.ip + " with byte length: " + rawMsg.getContent().length + " msgid: " + rawMsg.database_Id, -2);

                }
            };

            threadPool2.execute(runnable);
            //new Thread(runnable).start();

            return 1 + 4;

        } else if (command == (byte) 7) {
//message content
            if (4 + RawMsg.SIGNATURE_LENGRTH + 4 > readBuffer.remaining()) {
                return 0;
            }

            final int msgId = readBuffer.getInt();

            byte[] signature = new byte[RawMsg.SIGNATURE_LENGRTH];
            readBuffer.get(signature);

            //System.out.println("SIGNATURE  : " + Utils.bytesToHexString(signature));
            int contentLength = readBuffer.getInt();

//            System.out.println("###### content len: " + contentLength);
            if (contentLength > readBuffer.remaining()) {
                Log.put("not enough bytes yet... missing: " + (contentLength - readBuffer.remaining()) + " got: " + readBuffer.remaining() + " needed: " + contentLength, 20);
                return 0;
            }

//            Log.put("Command: msg content incoming...", 8);
            byte[] content = new byte[contentLength];
            readBuffer.get(content);
            RawMsg get = peer.getPendingMessages().get(msgId);
            //RawMsg get = Test.messageStore.getMessageById(msgId);

            if (get == null) {

                get = peer.getPendingMessagesTimedOut().get(msgId);

                if (get == null) {
                    System.out.println("WARNING: got message content for a message I cant find... " + msgId + " " + peer.getPendingMessages().size() + " ip: " + peer.ip);
                    return 1 + 4 + RawMsg.SIGNATURE_LENGRTH + 4 + contentLength;
                }

                System.out.println("WARNING: get message content for a timed out message... trying to add content to database...." + msgId + " " + peer.getPendingMessages().size() + " ip: " + peer.ip);

            }

            get.signature = signature;
            get.content = content;

            MessageDownloader.publicMsgsLoadedLock.lock();
            MessageDownloader.publicMsgsLoaded++;
            MessageDownloader.publicMsgsLoadedLock.unlock();

            final RawMsg getFinal = get;

            Log.put("execute new msg thread", 70);

            Runnable runnable = new Runnable() {

                @Override
                public void run() {
                    final String orgName = Thread.currentThread().getName();
                    if (!orgName.contains(" ")) {
                        Thread.currentThread().setName(orgName + " - addMessage + pr. broadcast");
                    }

                    RawMsg addMessage = MessageHolder.addMessage(getFinal);

                    peer.requestedMsgs--;
                    MessageDownloader.removeRequestedMessage(getFinal);
                    //System.out.println("DWGDYWGDYW " + addMessage.key.database_id);
//                            synchronized (peer.getPendingMessages()) {
//                                peer.getPendingMessages().remove(msgId);
//                            }
                    Log.put("msg: " + getFinal.timestamp + " " + getFinal.nonce, 100);

                    if (addMessage.getChannel() == null) {
                        //isPublic...
                        MessageDownloader.publicMsgsLoaded++;
                    }

//                    System.out.println("KEY: " + Channel.byte2String(addMessage.key.getPubKey()));
//                    System.out.println("timestamp: " + addMessage.timestamp);
//                    System.out.println("CONTENT hash: " + Sha256Hash.create(addMessage.content));
//                    System.out.println("signature hash: " + Sha256Hash.create(addMessage.signature));
                    synchronized (peer.getLoadedMsgs()) {
                        peer.addLoadedMsg(addMessage.database_Id);
                    }

                    if (addMessage.key.database_id == -1) {
                        addMessage = MessageHolder.addMessage(addMessage);
                        System.out.println("woooot " + Utils.bytesToHexString(getFinal.key.getPubKey()) + " " + getFinal.timestamp);
                    }

                    if (addMessage.key.database_id == -1) {
                        System.out.println("#####CODE: 19564 - NICHT GUT!!!! - try again to get PUBKEY ID");

                        RawMsg again = MessageHolder.addMessage(addMessage);

                        System.out.println("#####Key id is now: " + again.key.database_id);

                        System.out.println("#####Message not broadcasted...");
                        return;
                    }

                    if (Settings.BROADCAST_MSGS_AFTER_VERIFICATION) {
                        Test.broadcastMsg(addMessage);
                    }

                    MessageDownloader.trigger();
                    MessageVerifierHsqlDb.trigger();

                    if (peer.maxSimultaneousRequests <= MessageDownloader.MAX_REQUEST_PER_PEER) {
                        peer.maxSimultaneousRequests++;
                        Log.put("increased max req to: " + peer.maxSimultaneousRequests, -1);
                    }

                }
            };

            threadPool.execute(runnable);
//            runnable.run();

//            new Thread() {
//
//                @Override
//                public void run() {
//                    try {
//                        sleep(10000);
//                    } catch (InterruptedException ex) {
//                        Logger.getLogger(ConnectionHandler.class.getName()).log(Level.SEVERE, null, ex);
//                    }
//                    peer.requestedMsgs--;
//                    MessageDownloader.trigger();
//                }
//            }.start();
            Log.put(
                    "parsed bytes: " + (1 + 4 + RawMsg.SIGNATURE_LENGRTH + 4 + contentLength) + " remainig: " + readBuffer.remaining(), 200);

            return 1 + 4 + RawMsg.SIGNATURE_LENGRTH + 4 + contentLength;

        } else if (command == (byte) 8) {
            Log.put("Command: control data request...", 8);
            PeerTrustData peerTrustData = peer.getPeerTrustData();

            //ToDo: remove 0.s
            peer.writeBufferLock.lock();
            writeBuffer.put((byte) 9);
            writeBuffer.putInt(0);
            writeBuffer.putInt(peerTrustData.keyToIdHis.size());
            writeBuffer.putInt(peerTrustData.keyToIdMine.size());
            writeBuffer.putInt(0);

            ArrayList<Integer> keySet = new ArrayList<Integer>(peerTrustData.keyToIdHis.keySet());

            Collections.sort(keySet);

//            String toHashkeyToIdHis = "";
//            for (Entry<Integer, ECKey> entry : peerTrustData.keyToIdHis.entrySet()) {
//                int key = entry.getKey();
//                toHashkeyToIdHis += "" + key;
//            }
            String toHashkeyToIdHis = "";
            for (Integer key : keySet) {
                toHashkeyToIdHis += "" + key;
            }

            Collections.sort(peerTrustData.keyToIdMine);

            String toHashkeyToIdMine = "";
            for (int key : peerTrustData.keyToIdMine) {
                toHashkeyToIdMine += "" + key;
            }

            try {

                writeBuffer.putInt(Sha256Hash.create(toHashkeyToIdMine.getBytes("UTF-8")).hashCode());
                writeBuffer.putInt(Sha256Hash.create(toHashkeyToIdHis.getBytes("UTF-8")).hashCode());

            } catch (UnsupportedEncodingException ex) {
                Logger.getLogger(ConnectionHandler.class.getName()).log(Level.SEVERE, null, ex);
            }

            peer.writeBufferLock.unlock();

            return 1;

        } else if (command == (byte) 9) {

            if (4 + 4 + 4 + 4 + 4 + 4 > readBuffer.remaining()) {
                return 0;
            }

            PeerTrustData peerTrustData = peer.getPeerTrustData();

            int introducedMessages = readBuffer.getInt();
            int keyToIdHis = readBuffer.getInt();
            int keyToIdMine = readBuffer.getInt();
            int sendMessages = readBuffer.getInt();

            int hashkeyToIdMine = readBuffer.getInt();
            int hashkeyToIdHis = readBuffer.getInt();

            //mine = his
            //System.out.println("keyToIdHis: " + peerTrustData.keyToIdHis.size() + " - " + keyToIdMine);
            //System.out.println("keyToIdMine: " + peerTrustData.keyToIdMine.size() + " - " + keyToIdHis);
            Set<Integer> keySet = peerTrustData.keyToIdHis.keySet();
            ArrayList<Integer> arrayList = new ArrayList<Integer>(keySet);

//            Comparator<Integer> comparator = new Comparator<Integer>() {
//                public int compare(Integer o1, Integer o2) {
//                    return 0;
//                }
//            };
            Collections.sort(arrayList);

            String toHashkeyToIdHis = "";
            for (int key : arrayList) {
                toHashkeyToIdHis += "" + key;
            }

            Collections.sort(peerTrustData.keyToIdMine);

            String toHashkeyToIdMine = "";
            for (int key : peerTrustData.keyToIdMine) {
                toHashkeyToIdMine += "" + key;
            }

            try {
                int myHashkeyToIdMine = Sha256Hash.create(toHashkeyToIdMine.getBytes("UTF-8")).hashCode();
                int myHashkeyToIdHis = Sha256Hash.create(toHashkeyToIdHis.getBytes("UTF-8")).hashCode();

                Log.put("Hash keyToIdMine: " + myHashkeyToIdHis + " - " + hashkeyToIdMine, 30);

                if (peerTrustData.keyToIdHis.size() != keyToIdMine || myHashkeyToIdHis != hashkeyToIdMine) {
                    System.out.println("keyToIdHis wrong, clearing....");
                    System.out.println("list: " + toHashkeyToIdHis);
                    peerTrustData.keyToIdHis.clear();
                }

                Log.put("Hash keyToIdMine: " + myHashkeyToIdMine + " - " + hashkeyToIdHis, 30);

                if (peerTrustData.keyToIdMine.size() != keyToIdHis || myHashkeyToIdMine != hashkeyToIdHis) {
                    System.out.println("keyToIdMine wrong, clearing....");
                    System.out.println("list: " + toHashkeyToIdMine);
                    peerTrustData.keyToIdMine.clear();
                }
            } catch (UnsupportedEncodingException ex) {
                Logger.getLogger(ConnectionHandler.class.getName()).log(Level.SEVERE, null, ex);
            }

//            boolean trimedSendMsgs = false;
//            while (peerTrustData.sendMessages.size() > introducedMessages) {
//                peerTrustData.sendMessages.remove(peerTrustData.sendMessages.size() - 1);
//                trimedSendMsgs = true;
//            }
//            if (trimedSendMsgs) {
//                System.out.println("trimed sendMessages...");
//            }
//
//            boolean trimedIntroducedMessages = false;
//            while (peerTrustData.introducedMessages.size() > sendMessages) {
//                peerTrustData.introducedMessages.remove(peerTrustData.introducedMessages.size() - 1);
//                trimedIntroducedMessages = true;
//            }
//            if (trimedIntroducedMessages) {
//                System.out.println("trimed introducedMessages...");
//            }
//            for (Channel channel : Test.getChannels()) {
//                byte[] pubKey = channel.getKey().getPubKey();
//
//                //System.out.println("Channel key length: " + pubKey.length);
////                        writeBuffer.put((byte) 60);
////                        writeBuffer.put();
//            }
            if (Settings.lightClient) {

                //remove -1 for migration from super node to light node.
                peer.writeBuffer.put((byte) 62);
                peer.writeBuffer.putInt(-1);

                for (Channel channel : Test.channels) {

                    if (!peer.getPeerTrustData().sendChannelsToFilter.contains(peer)) {
                        peer.sendChannelToFilter(channel.key);
                    }

                }

                //TODO: code remove routine....
            } else {
                peer.writeBufferLock.lock();

                //hack for version compability - remove later!!!
                //ToDo: remove later
                if (!peerTrustData.keyToIdMine.contains(-1)) {
                    peerTrustData.keyToIdMine.add(-1);
                    writeBuffer.put((byte) 4);
                    writeBuffer.put(new byte[33]);
                    writeBuffer.putInt(-1);
                    //System.out.println("Msg index: " + indexOf);
                }

                peer.writeBuffer.put((byte) 60);
                peer.writeBuffer.putInt(-1);
                peer.writeBufferLock.unlock();
            }

            peer.writeBufferLock.lock();

            //init sync
            writeBuffer.put((byte) 3);
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
            writeBuffer.putLong(myTime);
            writeBuffer.put((byte) 0);
            //get PeerList
            writeBuffer.put((byte) 1);
//                    peer.setWriteBufferFilled();
            peer.writeBufferLock.unlock();

            return 1 + 4 + 4 + 4 + 4 + 4 + 4;

        } else if (command == (byte) 10) {

            if (1 + +1 + 2 > readBuffer.remaining()) {
                return 0;
            }

            byte get = readBuffer.get();

            if (get < 1) {
                System.out.println("omg, wrong IP address length....");
                peer.disconnect("ip address len < 0");
                return 0;
            }

            if (1 + 1 + ((int) get) + 2 > readBuffer.remaining()) {
                return 0;
            }

            byte[] ipBytes = new byte[get];
            readBuffer.get(ipBytes);
            String ip = new String(ipBytes);
            int port = readUnsignedShort(readBuffer);

            Peer newPeer = new Peer(ip, port);
            //System.out.println("Peerexchange: new peer " + ip + ":" + port);
            Test.findPeer(newPeer);

            return 1 + 1 + get + 2;

        } else if (command == (byte) 51) {

            if (1 + 16 > readBuffer.remaining()) {
                return 0;
            }
            byte[] otherAuthKeyBytes = new byte[16];
            readBuffer.get(otherAuthKeyBytes);

            if (!peer.requestedNewAuth) {
                //dafuq, thought I had an authkey for node. Generating new AuthKey
                System.out.println("dafuq, thought I had an authkey for node. Generating new AuthKey: " + peer.nodeId);
                //peer.peerTrustData = new PeerTrustData();

                sendNewAuthKey(peer);
            }

            if (peer.peerIsHigher()) {
                System.arraycopy(otherAuthKeyBytes, 0, peer.peerTrustData.authKey, 16, 16);
            } else {
                System.arraycopy(otherAuthKeyBytes, 0, peer.peerTrustData.authKey, 0, 16);
            }

            peer.peerTrustData.nodeId = peer.nodeId;
            Test.peerTrusts.add(peer.peerTrustData);
            peer.peerTrustData.initInternalId();

            saveTrustData();

            Log.put("added key from other side... " + peer.nodeId, 100);

            Log.put("requesting auth ...: " + peer.nodeId, 100);

            Random r = new SecureRandom();

            byte[] toEncrypt = new byte[32];
            r.nextBytes(toEncrypt);

            peer.writeBufferLock.lock();
            writeBuffer.put((byte) 52);
            writeBuffer.put(toEncrypt);
            peer.writeBufferLock.unlock();
            peer.toEncodeForAuthFromMe = toEncrypt;
            //peer.peerTrustData = pt;

//            System.out.println("request authentication...");
            return 1 + 16;

        } else if (command == (byte) 52) {

            if (32 > readBuffer.remaining()) {
                return 0;
            }
            byte[] toEnc = new byte[32];
            readBuffer.get(toEnc);

            boolean send = false;
            for (PeerTrustData pt : Test.peerTrusts) {

                if (!pt.nodeId.equals(peer.nodeId)) {
                    continue;
                }

                byte[] encode = AESCrypt.encode(pt.authKey, toEnc);

                peer.toEncodeForAuthFromHim = toEnc;

                peer.writeBufferLock.lock();
                writeBuffer.put((byte) 53);
                writeBuffer.put(encode);
                peer.writeBufferLock.unlock();
                send = true;

            }

            peer.setWriteBufferFilled();

            if (!send) {
                System.out.println("ERROR 77877");
            }

            //System.out.println("encoded data with authKey... " + Utils.bytesToHexString(toEnc) + " len: " + toEnc.length);
            //System.out.println("encoded data with authKey... " + Utils.bytesToHexString(encode) + " len: " + encode.length);
            return 1 + 32;

        } else if (command == (byte) 53) {

            if (48 > readBuffer.remaining()) {
                System.out.println("zu wenig bytes.... " + readBuffer.remaining());
                return 0;
            }
            byte[] thereEnc = new byte[48];
            readBuffer.get(thereEnc);

            if (peer.toEncodeForAuthFromHim == null || peer.toEncodeForAuthFromMe == null) {
                System.out.println("FATAL ERROR: 48727 - auth failed");
                return 1 + 48;
            }

            boolean found = false;
            for (PeerTrustData pt : Test.peerTrusts) {

                if (!pt.nodeId.equals(peer.nodeId)) {
                    continue;
                }

                found = true;

                byte[] myEnc = AESCrypt.encode(pt.authKey, peer.toEncodeForAuthFromMe);

                if (Arrays.equals(thereEnc, myEnc)) {

                    peer.peerTrustData = pt;

                    //System.out.println("Auth successfull with nonce: " + peer.nonce);
                    peer.authed = true;

                    //Schluessel ist peer.peer.peerTrustData.authKey + thereEnc + myEnc, falls die Verbindung von aussem kommt.
                    //ansonsten vertausche thereEnc mit myEnc.
                    byte[] rc4CrpytKeySeedWrite = new byte[32 + 32 + 32];
                    byte[] rc4CrpytKeySeedRead = new byte[32 + 32 + 32];
                    System.arraycopy(peer.peerTrustData.authKey, 0, rc4CrpytKeySeedWrite, 0, 32);
                    System.arraycopy(peer.peerTrustData.authKey, 0, rc4CrpytKeySeedRead, 0, 32);

                    if (peer.peerIsHigher()) {
                        //System.out.println("Im higher");
                        System.arraycopy(peer.toEncodeForAuthFromMe, 0, rc4CrpytKeySeedWrite, 32, 32);
                        System.arraycopy(peer.toEncodeForAuthFromHim, 0, rc4CrpytKeySeedWrite, 64, 32);

                        System.arraycopy(peer.toEncodeForAuthFromHim, 0, rc4CrpytKeySeedRead, 32, 32);
                        System.arraycopy(peer.toEncodeForAuthFromMe, 0, rc4CrpytKeySeedRead, 64, 32);
                    } else {
                        //System.out.println("Im lower");
                        System.arraycopy(peer.toEncodeForAuthFromMe, 0, rc4CrpytKeySeedRead, 32, 32);
                        System.arraycopy(peer.toEncodeForAuthFromHim, 0, rc4CrpytKeySeedRead, 64, 32);

                        System.arraycopy(peer.toEncodeForAuthFromHim, 0, rc4CrpytKeySeedWrite, 32, 32);
                        System.arraycopy(peer.toEncodeForAuthFromMe, 0, rc4CrpytKeySeedWrite, 64, 32);
                    }

//                    System.out.println("My NONCE: " + Test.NONCE);
//                    System.out.println("There NONCE: " + peer.nonce);
//                    System.out.println("KABUM, initialisiere Verschluesselung, key fuer ARC4  " + Utils.bytesToHexString(rc4CrpytKeySeed));
//
//                    System.out.println("my: " + Utils.bytesToHexString(myEnc));
//                    System.out.println("te: " + Utils.bytesToHexString(thereEnc));
                    byte[] rc4CryptKeyFalseWrite = Sha256Hash.create(rc4CrpytKeySeedWrite).getBytes();
                    byte[] rc4CryptKeyFalseRead = Sha256Hash.create(rc4CrpytKeySeedRead).getBytes();
//                    System.out.println("Hashed key is: " + Utils.bytesToHexString(rc4CryptKey));

                    //System.out.println("KABUM, initialisiere Verschluesselung, key fuer ARC4  " + Utils.bytesToHexString(rc4CryptKeyFalseWrite) + " -- " + Utils.bytesToHexString(rc4CryptKeyFalseRead));
                    //System.out.println("ARC4 active. IP: " + peer.ip);
                    //Beide bruachen einen eigenen rc4 verschluesseler da sich der Key aendert mit jedem byte welches verschluesselt wird...
                    //System.out.println("Key length: " + rc4CryptKeyFalse.length);
                    //Key fuer RC4 darf nur max 31 bytes haben, loesche letztes byte...
                    byte[] rc4CryptKeyWrite = new byte[31];
                    byte[] rc4CryptKeyRead = new byte[31];

                    System.arraycopy(rc4CryptKeyFalseWrite, 0, rc4CryptKeyWrite, 0, 31);
                    System.arraycopy(rc4CryptKeyFalseRead, 0, rc4CryptKeyRead, 0, 31);

                    if (peer.peerIsHigher()) {
                        peer.writeKey = new RC4(rc4CryptKeyWrite);
                        peer.readKey = new RC4(rc4CryptKeyRead);
                    } else {
                        peer.writeKey = new RC4(rc4CryptKeyRead);
                        peer.readKey = new RC4(rc4CryptKeyWrite);
                    }

                    peer.peerTrustData.lastSeen = System.currentTimeMillis();
                    Log.put("PEER is now trusted...", 300);

                    if (peer.peerTrustData.ips.contains(peer.ip)) {
                        Log.put("IP schon in den trusted Data...", 200);
                    } else {

                        if (peer.peerTrustData.ips.size() > 10) {
                            peer.peerTrustData.ips.clear();
                        }

                        peer.peerTrustData.ips.add(peer.ip);
//                        peer.peerTrustData.port = peer.port;
                        Log.put("IP added and port set!", 300);
                    }

                    //we have to update the port in every case!
                    peer.peerTrustData.port = peer.port;

                    peer.removeRequestedMsgs();

                    pt.pendingMessages = new HashMap<Integer, RawMsg>();
                    pt.pendingMessagesTimedOut = new HashMap<Integer, RawMsg>();
                    pt.pendingMessagesPublic = new HashMap<Integer, RawMsg>();

                    peer.writeBufferLock.lock();
                    //ab nun werden alle bytes verschluesselt, dies wird mit dem folgenden Befehl gestarted
                    writeBuffer.put((byte) 55);
                    //schicke dieses byte noch unverschluesselt...
                    //peer.setWriteBufferFilled();

                    peer.writeBufferLock.lock();

                    int writtenBytes = 0;
                    peer.writeBuffer.flip(); //switch buffer for reading
                    try {
                        writtenBytes = peer.writeBytesToPeer(peer.writeBuffer);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                    Test.outBytes += writtenBytes;
                    peer.sendBytes += writtenBytes;

                    peer.writeBuffer.compact();

                    if (peer.writeBuffer.position() != 0) {
                        System.out.println("ACHTUNG konnte nun verschluessel byte nicht direkt schicken...");
                    }
                    peer.writeBufferLock.unlock();

//                        try {
//                            sleep(3000);
//                        } catch (InterruptedException ex) {
//                            Logger.getLogger(ConnectionHandler.class.getName()).log(Level.SEVERE, null, ex);
//                        }
                    //TODO make better
                    //peer.writeBufferCrypted = ByteBuffer.allocate(peer.writeBuffer.capacity());
                    //peer.readBufferCrypted = ByteBuffer.allocate(peer.readBuffer.capacity());
                    try {
                        peer.writeBufferCrypted = ByteBuffer.allocate(1024 * 1024);
                    } catch (Throwable e) {
                        e.printStackTrace();
                        System.out.println("Speicher konnte nicht reserviert werden2. Disconnect peer...");
                        peer.disconnect("speicher voll 34642");
                        return 0;
                    }

                    //FULL CONNECTED TO NODE!
                    //NUN darf die Verbindung genutzt werden zum syncen usw...
                    writeBuffer.put(Command.UPDATE_REQUEST_TIMESTAMP);
                    writeBuffer.put(Command.ANDROID_UPDATE_REQUEST_TIMESTAMP);
                    writeBuffer.put((byte) 8);

                    peer.writeBufferLock.unlock();

                    //peer.getPeerTrustData().removeNotSuccesfulSendMessages();
                    peer.retries = 0;

                    //peer.setWriteBufferFilled();
                    //System.out.println("send byte 8 to node");
                    break;

                } else {
                    //System.out.println("WARNING: auth failed with nonce: " + peer.nonce + " key: " + Utils.bytesToHexString(pt.authKey));
                    //System.out.println("DATA: " + Utils.bytesToHexString(myEnc));
                    //System.out.println("DATA: " + Utils.bytesToHexString(thereEnc));
                }
            }

            if (!found) {
                System.out.println("erm, nicht gefunden?!?");
            }

            return 1 + 48;
        } else if (command == (byte) 55) {

            if (peer.readKey == null) {
                System.out.println("ERROR 14275, peer wants to crypt connection, but i got no key?!?");
                peer.disconnect("ERROR 14275");
                return 0;
            }

            try {
                peer.readBufferCrypted = ByteBuffer.allocate(READ_BUFFER_SIZE);
            } catch (Throwable e) {
                System.out.println("Speicher konnte nicht reserviert werden3. Disconnect peer...");
                peer.disconnect("Speicher konnte nicht reserviert werden3.");
            }
            //System.out.println("ab nun wird der lese strom entschluesselt...");

            //readBuffer.get();//index einen weiter schieben, (byte) 55 wurde schon verarbeitet oder auch nicht?
            //Verschluessel die schon gelesenen bytes...
            if (readBuffer.hasRemaining()) {
                //System.out.println("OH OH das wird schief gehen, remaining bytes: " + readBuffer.remaining());

                synchronized (peer.readBuffer) {

                    byte[] buffer = new byte[peer.readBuffer.remaining()];
                    peer.readBuffer.get(buffer);
                    peer.readBuffer.compact();
                    byte[] decrypt = peer.readKey.decrypt(buffer);
//                peer.readBuffer.clear();
                    peer.readBuffer.put(decrypt);
                    peer.readBuffer.flip();
//                peer.readBufferCrypted.put(buffer);

                    //System.out.println("ENCRYPTED: " + Utils.bytesToHexString(encrypt));
                    return -1; //readbuffer darf nicht mehr verschoben werden!
                }
            }

            return 1;//ja das muss so!!

        } else if (command == (byte) 60) {
            //addFilterMessage
            if (4 > readBuffer.remaining()) {
                System.out.println("zu wenig bytes.... " + readBuffer.remaining());
                return 0;
            }

            final int hisKeyId = readBuffer.getInt();
            //ECKey get = peer.getKeyToIdHis().get(hisKeyId);

            if (hisKeyId == -1) {
                //all channels...
                Test.messageStore.addFilterChannel(peer.getPeerTrustData().internalId, -1);
                Log.put("added all channels to get messages from: " + peer.ip, 20);
            } else {

                ECKey id2KeyHis = peer.getId2KeyHis(hisKeyId);

                if (id2KeyHis == null) {
                    //id2keys wrong!
                    peer.disconnect("id2KeyHis null");
                } else {
                    int pubkeyIdMine = Test.messageStore.getPubkeyId(id2KeyHis);

                    //System.out.println("FILTERMSG ID: " + pubkeyIdMine);
                    Test.messageStore.addFilterChannel(peer.getPeerTrustData().internalId, pubkeyIdMine);
                }
            }

            return 1 + 4;
        } else if (command == (byte) 61) {
            //set Client Type
            System.out.println("command not supported, yet");
            return 1 + 4;
        } else if (command == (byte) 62) {
            //remove channel - delFilterMessage
            if (4 > readBuffer.remaining()) {
                System.out.println("zu wenig bytes.... " + readBuffer.remaining());
                return 0;
            }

            final int hisKeyId = readBuffer.getInt();
            //ECKey get = peer.getKeyToIdHis().get(hisKeyId);

            if (hisKeyId == -1) {
                Test.messageStore.delFilterChannel(peer.getPeerTrustData().internalId, -1);
            } else {
                ECKey id2KeyHis = peer.getId2KeyHis(hisKeyId);

                if (id2KeyHis == null) {
                    //id2keys wrong!
                    peer.disconnect("id2KeyHis null");
                } else {
                    int pubkeyIdMine = Test.messageStore.getPubkeyId(id2KeyHis);
                    Test.messageStore.delFilterChannel(peer.getPeerTrustData().internalId, pubkeyIdMine);

                }
            }
            return 1 + 4;

        } else if (command == (byte) 70) {

            if (8 > readBuffer.remaining()) {
                System.out.println("zu wenig bytes.... " + readBuffer.remaining());
                return 0;
            }

            long timestamp = readBuffer.getLong();

            System.out.println("syncing back in time...");

            syncMessagesBack(timestamp, peer, 100);

            return 1 + 8;

        } else if (command == (byte) 71) {

            if (8 > readBuffer.remaining()) {
                System.out.println("zu wenig bytes.... " + readBuffer.remaining());
                return 0;
            }

            long timestamp = readBuffer.getLong();

            peer.getPeerTrustData().backSyncedTill = timestamp;

            System.out.println("setted back sync time to: " + timestamp);

            return 1 + 8;
        } else if (command == Command.PONG) {

            long curPing = (System.currentTimeMillis() - peer.lastPinged);

//            System.out.println("ping: " + curPing);

            if (peer.ping == 0) {
                peer.ping = curPing;
            } else {

                double a = (curPing / 10.);
                double b = peer.ping * 9 / 10.;
                double c = a + b;
                peer.ping = c;
            }
//            System.out.println("ping: " + (System.currentTimeMillis() - peer.lastPinged) + " avg.: " + Math.round(c*100)/100.);

            return 1;
        } else if (command == Command.PING) {
            //            System.out.println("Antworte auf ping...");
//            ByteBuffer allocate = ByteBuffer.allocate(1);
//            allocate.put((byte) 101);
//            allocate.flip();
//            try {
//                peer.getSocketChannel().write(allocate);
//            } catch (IOException ex) {
//                Logger.getLogger(ConnectionHandler.class.getName()).log(Level.SEVERE, null, ex);
//            }

            peer.writeBufferLock.lock();
            writeBuffer.put((byte) 101);
            peer.writeBufferLock.unlock();
            peer.setWriteBufferFilled();
            return 1;
        } else if (command == (byte) 254) {
            System.out.println("closing, other side init it...");
            peer.disconnect("closing, other side init it...");
            return 1;

        } else if (command == Command.UPDATE_REQUEST_TIMESTAMP) {
            peer.writeBufferLock.lock();
            writeBuffer.put(Command.UPDATE_ANSWER_TIMESTAMP);
            writeBuffer.putLong(Settings.getMyCurrentVersionTimestamp());
            peer.writeBufferLock.unlock();
            peer.setWriteBufferFilled();
            return 1;
        } else if (command == Command.UPDATE_ANSWER_TIMESTAMP) {

            if (8 > readBuffer.remaining()) {
                return 0;
            }

            long othersTimestamp = readBuffer.getLong();

//            System.out.println("Update found from: " + new Date(othersTimestamp) + " our version is from: " + new Date(Settings.getMyCurrentVersionTimestamp()));

            if (othersTimestamp + 10000 < Settings.getMyCurrentVersionTimestamp()) {
                System.out.println("WARNING: peer has outdated redPandaj version! " + peer.getNodeId());
            }

            //we have to add some tolerance, since windows may report up to ms accuracy and linux only to seconds
            if (othersTimestamp > Settings.getMyCurrentVersionTimestamp() + 10000 && Settings.getMyCurrentVersionTimestamp() != -1 && !Settings.seedNode) {

                System.out.println("own timestamp: " + Settings.getMyCurrentVersionTimestamp() + " others: " + othersTimestamp);

                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        updateUploadLock.acquireUninterruptibly();
                        try {
                            System.out.println("our version is outdated, we try to download it from this peer!");
                            peer.writeBufferLock.lock();
                            writeBuffer.put(Command.UPDATE_REQUEST_CONTENT);
                            peer.writeBufferLock.unlock();
                            peer.setWriteBufferFilled();


                            //lets not download another version in the next x seconds, otherwise our RAM may explode!
                            try {
                                Thread.sleep(10000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        } finally {
                            System.out.println("we can now download it from another peer...");
                            updateUploadLock.release();
                        }

                    }
                };

                threadPool.submit(runnable);
            }


            return 1 + 8;
        } else if (command == Command.UPDATE_REQUEST_CONTENT) {

            if (Settings.getMyCurrentVersionTimestamp() == -1) {
                return 1;
            }

            if (localSettings.getUpdateSignature() == null) {
                System.out.println("we dont have an official signature to upload that update to other peers!");
                return 1;
            }


            Runnable runnable = new Runnable() {
                @Override
                public void run() {

                    updateUploadLock.acquireUninterruptibly();

                    try {

                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        Path path;
                        if (Settings.seedNode) {
                            path = Paths.get("out/artifacts/redPandaj_jar/redPandaj.jar");
                        } else {
                            path = Paths.get("redPandaj.jar");
                        }

                        try {
                            System.out.println("we send the update to a peer!");
                            byte[] data = Files.readAllBytes(path);

                            ByteBuffer a = ByteBuffer.allocate(1 + 8 + 4 + RawMsg.SIGNATURE_LENGRTH + data.length);
                            a.put(Command.UPDATE_ANSWER_CONTENT);
                            a.putLong(Settings.getMyCurrentVersionTimestamp());
                            a.putInt(data.length);
                            a.put(localSettings.getUpdateSignature());
                            a.put(data);
                            a.flip();

                            peer.writeBufferLock.lock();


                            try {

//                            int pos = 0, toSend = 0;
//
//
//                            byte[] array = a.array();
//
//                            System.out.println("length: " + array.length);
//
//
//                            while (array.length - pos > 0) {
//                                System.out.println("pos: " + pos);
//
//                                toSend = Math.min(writeBuffer.remaining(), array.length - pos);
//
//                                System.out.println("writebuffer r: " + writeBuffer.remaining());
//
//                                System.out.println("toSend: " + toSend);
//
//                                writeBuffer.put(array, pos, toSend);
//
//                                pos += toSend;
//
//                                peer.setWriteBufferFilled();
//                                try {
//                                    Thread.sleep(200);
//                                } catch (InterruptedException e) {
//                                    e.printStackTrace();
//                                }
//                            }


                                if (peer.writeBuffer.remaining() < a.remaining()) {
                                    ByteBuffer allocate = ByteBuffer.allocate(peer.writeBuffer.capacity() + a.remaining() + 1024 * 1024 * 10);
                                    peer.writeBuffer.flip();
                                    allocate.put(peer.writeBuffer);
                                    peer.writeBuffer = allocate;
                                }


//                            System.out.println("" + writeBuffer);

//                            System.out.println("writing bytes: " + a.remaining());

                                peer.writeBuffer.put(a.array());
                                peer.setWriteBufferFilled();

                            } finally {
                                peer.writeBufferLock.unlock();
                            }


                            // only one upload at a time
                            int cnt = 0;
                            while (cnt < 6) {
                                cnt++;
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }

                                peer.writeBufferLock.lock();
                                try {
                                    if (!peer.isConnected() || (peer.writeBuffer.position() == 0 && peer.writeBufferCrypted.position() == 0)) {
                                        break;
                                    }
                                } finally {
                                    peer.writeBufferLock.unlock();
                                }

//                            System.out.println("peer still downloading...");

                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } finally {
                        updateUploadLock.release();
                    }


                }
            };

            threadPool.submit(runnable);

            return 1;
        } else if (command == Command.UPDATE_ANSWER_CONTENT) {

//            System.out.println("we get a update from node!");

            if (8 + 4 + RawMsg.SIGNATURE_LENGRTH > readBuffer.remaining()) {
                return 0;
            }


//            System.out.println("we get a update from node 2");
            long othersTimestamp = readBuffer.getLong();
            int toReadBytes = readBuffer.getInt();


            byte[] signature = new byte[RawMsg.SIGNATURE_LENGRTH];
            readBuffer.get(signature);

//            System.out.println("download in pipe: " + new Date(othersTimestamp));

            if (toReadBytes > readBuffer.remaining()) {
                return 0;
            }

            System.out.println("update completely in buffer!");

            byte[] data = new byte[toReadBytes];
            readBuffer.get(data);


            if (othersTimestamp > Settings.getMyCurrentVersionTimestamp() && Settings.getMyCurrentVersionTimestamp() != -1 && !Settings.seedNode) {
                System.out.println("we got the update successfully, install it!");


                System.out.println("signature found: " + Utils.bytesToHexString(signature));

                //lets check the signature chunk:
                Channel updateChannel = SpecialChannels.getUpdateChannel();


                ByteBuffer bytesToHash = ByteBuffer.allocate(8 + toReadBytes);

                bytesToHash.putLong(othersTimestamp);
                bytesToHash.put(data);


                Sha256Hash hash = Sha256Hash.create(bytesToHash.array());
                System.out.println("hash: " + Utils.bytesToHexString(hash.getBytes()));

                boolean verify = updateChannel.getKey().verify(hash.getBytes(), signature);

                System.out.println("update verified: " + verify);

                File file = new File("redPandaj.jar");
                long myCurrentVersionTimestamp = file.lastModified();
                if (!file.exists()) {
                    System.out.println("No jar to update found, exiting auto update!");
                    return 1 + 8 + 4 + RawMsg.SIGNATURE_LENGRTH + data.length;
                }

                if (myCurrentVersionTimestamp >= othersTimestamp) {
                    System.out.println("update not required, aborting...");
                    return 1 + 8 + 4 + RawMsg.SIGNATURE_LENGRTH + data.length;
                }


                if (verify) {

                    try (FileOutputStream fos = new FileOutputStream("update")) {
                        fos.write(data);
                        //fos.close(); There is no more need for this line since you had created the instance of "fos" inside the try. And this will automatically close the OutputStream
                        System.out.println("update store in update file, new timestamp: " + othersTimestamp);

                        File f = new File("update");
                        f.setLastModified(othersTimestamp);

                        Test.localSettings.setUpdateSignature(signature);
                        Test.localSettings.save();

                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        System.exit(0);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }


//                ECKey.ECDSASignature sign = updateChannel.getKey().sign(hash);
//
//                byte[] encodeToDER = new byte[RawMsg.SIGNATURE_LENGRTH];
//                byte[] sigBytes = sign.encodeToDER();
//                System.arraycopy(sigBytes, 0, encodeToDER, 0, sigBytes.length);
//
//                byte[] newBytes = new byte[encodeToDER.length];
//
//                int index = encodeToDER.length - 1;
//                while (true) {
//                    System.arraycopy(encodeToDER, 0, newBytes, 0, index + 1);
//                    if (newBytes[index] == (byte) 0) {
//                        newBytes = new byte[index];
//
//                        index--;
//                    } else {
//                        break;
//                    }
//
//                }
//
//                //System.out.println("Sigbytes len: " + sigBytes.length + " " + Utils.bytesToHexString(encodeToDER));
//                //System.out.println("Sigbytes len: " + sigBytes.length + " " + Utils.bytesToHexString(newBytes));
//                byte[] signature = encodeToDER;
//
//                System.out.println("signature: " + Utils.bytesToHexString(signature));


            }


            return 1 + 8 + 4 + RawMsg.SIGNATURE_LENGRTH + data.length;

        } else if (command == Command.ANDROID_UPDATE_REQUEST_TIMESTAMP) {
            peer.writeBufferLock.lock();
            writeBuffer.put(Command.ANDROID_UPDATE_ANSWER_TIMESTAMP);
            writeBuffer.putLong(Settings.getMyCurrentAndroidVersionTimestamp());
            peer.writeBufferLock.unlock();
            peer.setWriteBufferFilled();
            return 1;
        } else if (command == Command.ANDROID_UPDATE_ANSWER_TIMESTAMP) {

            if (8 > readBuffer.remaining()) {
                return 0;
            }

            long othersTimestamp = readBuffer.getLong();

            System.out.println("Update found from: " + new Date(othersTimestamp) + " our version is from: " + new Date(Settings.getMyCurrentVersionTimestamp()));

            if (othersTimestamp + 10000 < Settings.getMyCurrentAndroidVersionTimestamp()) {
                System.out.println("WARNING: peer has outdated android.apk version! " + peer.getNodeId());
            }

            //we have to add some tolerance, since windows may report up to ms accuracy and linux only to seconds
            if (othersTimestamp > Settings.getMyCurrentAndroidVersionTimestamp() + 10000) {
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        updateUploadLock.acquireUninterruptibly();
                        try {

                            if (othersTimestamp <= Settings.getMyCurrentAndroidVersionTimestamp()) {
                                //maybe we downloaded the update while waiting for lock!
                                System.out.println("already downloaded, skipping...");
                                return;
                            }

                            System.out.println("our android.apk version is outdated, we try to download it from this peer!");
                            peer.writeBufferLock.lock();
                            writeBuffer.put(Command.ANDROID_UPDATE_REQUEST_CONTENT);
                            peer.writeBufferLock.unlock();
                            peer.setWriteBufferFilled();


                            //lets not download another version in the next x seconds, otherwise our RAM may explode!
                            try {
                                Thread.sleep(10000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        } finally {
                            System.out.println("we can now download it from another peer...");
                            updateUploadLock.release();
                        }

                    }
                };

                threadPool.submit(runnable);
            }


            return 1 + 8;
        } else if (command == Command.ANDROID_UPDATE_REQUEST_CONTENT) {


            if (localSettings.getUpdateAndroidSignature() == null) {
                System.out.println("we dont have an official signature to upload that android.apk update to other peers!");
                return 1;
            }


            Runnable runnable = new Runnable() {
                @Override
                public void run() {

                    updateUploadLock.acquireUninterruptibly();

                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    Path path = Paths.get("android.apk");


                    try {
                        System.out.println("we send the android.apk update to a peer!");
                        byte[] data = Files.readAllBytes(path);


                        boolean verify = false;
                        int vcnt = 0;

                        //lets first check our signature!
                        Channel updateChannel = SpecialChannels.getUpdateChannel();

//                        while (!verify) {
                        ByteBuffer bytesToHash = ByteBuffer.allocate(8 + data.length);

                        bytesToHash.putLong(Settings.getMyCurrentAndroidVersionTimestamp() + vcnt);
                        bytesToHash.put(data);

                        Sha256Hash hash = Sha256Hash.create(bytesToHash.array());
                        System.out.println("hash: " + Utils.bytesToHexString(hash.getBytes()));

                        verify = updateChannel.getKey().verifyPrimitive(hash.getBytes(), localSettings.getUpdateAndroidSignature());
                        System.out.println("update verified: " + verify);
                        vcnt++;
                        System.out.println("cnt: " + vcnt);
//                        }


                        if (!verify) {
                            System.out.println("################################ asdf " + Settings.getMyCurrentAndroidVersionTimestamp());
                            return;
                        }


                        ByteBuffer a = ByteBuffer.allocate(1 + 8 + 4 + RawMsg.SIGNATURE_LENGRTH_PRIMITIVE + data.length);
                        a.put(Command.ANDROID_UPDATE_ANSWER_CONTENT);
                        a.putLong(Settings.getMyCurrentAndroidVersionTimestamp());
                        a.putInt(data.length);
                        a.put(localSettings.getUpdateAndroidSignature());
                        a.put(data);
                        a.flip();

                        peer.writeBufferLock.lock();


                        try {

                            if (peer.writeBuffer.remaining() < a.remaining()) {
                                ByteBuffer allocate = ByteBuffer.allocate(peer.writeBuffer.capacity() + a.remaining() + 1024 * 1024 * 10);
                                peer.writeBuffer.flip();
                                allocate.put(peer.writeBuffer);
                                peer.writeBuffer = allocate;
                            }

                            peer.writeBuffer.put(a.array());
                            peer.setWriteBufferFilled();

                        } finally {
                            peer.writeBufferLock.unlock();
                        }


                        // only one upload at a time
                        int cnt = 0;
                        while (cnt < 6) {
                            cnt++;
                            try {
                                Thread.sleep(10000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                            peer.writeBufferLock.lock();
                            try {
                                if (!peer.isConnected() || (peer.writeBuffer.position() == 0 && peer.writeBufferCrypted.position() == 0)) {
                                    break;
                                }
                            } finally {
                                peer.writeBufferLock.unlock();
                            }

//                            System.out.println("peer still downloading...");

                        }


                        updateUploadLock.release();


                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            };

            threadPool.submit(runnable);

            return 1;
        } else if (command == Command.ANDROID_UPDATE_ANSWER_CONTENT) {

//            System.out.println("we get an update from node!");

            if (8 + 4 + RawMsg.SIGNATURE_LENGRTH_PRIMITIVE > readBuffer.remaining()) {
                return 0;
            }


//            System.out.println("we get a update from node 2");
            long othersTimestamp = readBuffer.getLong();
            int toReadBytes = readBuffer.getInt();


            byte[] signature = new byte[RawMsg.SIGNATURE_LENGRTH_PRIMITIVE];
            readBuffer.get(signature);

//            System.out.println("download in pipe: " + new Date(othersTimestamp));

            if (toReadBytes > readBuffer.remaining()) {
                return 0;
            }

            System.out.println("update completely in buffer!");

            byte[] data = new byte[toReadBytes];
            readBuffer.get(data);


            if (othersTimestamp > Settings.getMyCurrentAndroidVersionTimestamp()) {
                System.out.println("we got the update successfully, lets copy it to hard drive if signature is correct");


                System.out.println("signature found: " + Utils.bytesToHexString(signature));

                //lets check the signature chunk:
                Channel updateChannel = SpecialChannels.getUpdateChannel();


                ByteBuffer bytesToHash = ByteBuffer.allocate(8 + toReadBytes);

                bytesToHash.putLong(othersTimestamp);
                bytesToHash.put(data);


                Sha256Hash hash = Sha256Hash.create(bytesToHash.array());
                System.out.println("hash: " + Utils.bytesToHexString(hash.getBytes()));

                boolean verify = updateChannel.getKey().verifyPrimitive(hash.getBytes(), signature);

                System.out.println("update verified: " + verify);

                File file = new File("android.apk");
                long myCurrentVersionTimestamp = file.lastModified();

                if (myCurrentVersionTimestamp >= othersTimestamp) {
                    System.out.println("update not required, aborting...");
                    return 1 + 8 + 4 + RawMsg.SIGNATURE_LENGRTH_PRIMITIVE + data.length;
                }


                if (verify) {

                    try (FileOutputStream fos = new FileOutputStream("android.apk")) {
                        fos.write(data);
                        //fos.close(); There is no more need for this line since you had created the instance of "fos" inside the try. And this will automatically close the OutputStream
                        System.out.println("update stored in android.apk file");

                        File f = new File("android.apk");
                        f.setLastModified(othersTimestamp);


                        Settings.resetMyCurrentAndroidVersionTimestamp();

                        Test.localSettings.setUpdateAndroidSignature(signature);
                        Test.localSettings.save();


                        Test.peerListLock.lock();
                        try {
                            for (Peer p : Test.peerList) {
                                if (!p.isConnected()) {
                                    continue;
                                }
                                p.writeBufferLock.lock();
                                try {
                                    p.writeBuffer.put(Command.ANDROID_UPDATE_ANSWER_TIMESTAMP);
                                    p.writeBuffer.putLong(Settings.getMyCurrentAndroidVersionTimestamp());
                                } finally {
                                    p.writeBufferLock.unlock();
                                }
                                p.setWriteBufferFilled();
                            }
                        } finally {
                            Test.peerListLock.unlock();
                        }


                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }


//                updateUploadLock.unlock();

//                ECKey.ECDSASignature sign = updateChannel.getKey().sign(hash);
//
//                byte[] encodeToDER = new byte[RawMsg.SIGNATURE_LENGRTH];
//                byte[] sigBytes = sign.encodeToDER();
//                System.arraycopy(sigBytes, 0, encodeToDER, 0, sigBytes.length);
//
//                byte[] newBytes = new byte[encodeToDER.length];
//
//                int index = encodeToDER.length - 1;
//                while (true) {
//                    System.arraycopy(encodeToDER, 0, newBytes, 0, index + 1);
//                    if (newBytes[index] == (byte) 0) {
//                        newBytes = new byte[index];
//
//                        index--;
//                    } else {
//                        break;
//                    }
//
//                }
//
//                //System.out.println("Sigbytes len: " + sigBytes.length + " " + Utils.bytesToHexString(encodeToDER));
//                //System.out.println("Sigbytes len: " + sigBytes.length + " " + Utils.bytesToHexString(newBytes));
//                byte[] signature = encodeToDER;
//
//                System.out.println("signature: " + Utils.bytesToHexString(signature));


            }


            return 1 + 8 + 4 + RawMsg.SIGNATURE_LENGRTH_PRIMITIVE + data.length;

        }

        byte nextByte = 0;

        if (readBuffer.hasRemaining()) {
            nextByte = readBuffer.get();
        }

        System.out.println(
                "Wrong protocol, disconnecting + removing peer, nonce: " + peer.nodeId + " Command was: " + command + " next byte: " + nextByte + " remaining: " + readBuffer.remaining() + " crypt-Stauts: out: " + (peer.writeBufferCrypted != null) + ", in: " + (peer.readBufferCrypted != null));
        ByteBuffer a = ByteBuffer.allocate(1);

        a.put(
                (byte) 255);
        a.flip();

        try {
            int write = peer.getSocketChannel().write(a);
            System.out.println("QUIT bytes: " + write);
        } catch (IOException ex) {
        } catch (java.nio.channels.NotYetConnectedException e) {
        }
        //System.out.println("closing, got panic command...");
        peer.disconnect(
                "WRONG BYTE!");
        //TODO add again
        Test.removePeer(peer);

        return 0;

    }

    public static void syncMessagesBack(final long time, final Peer peer, final int cnt) {
        new Thread() {

            @Override
            public void run() {

                long oldestTime = time;

                Log.put("!BACK! sync... from: " + time, 20);
                ResultSet executeQuery = MessageHolder.getMessagesForBackSync(time, cnt);
                Log.put("query okay...", 20);
                try {
                    while (executeQuery.next()) {

                        if (!peer.isConnected() || !peer.isAuthed()) {
                            break;
                        }

                        //System.out.println("sending message...");
                        int message_id = executeQuery.getInt("message_id");

                        int pubkey_id = executeQuery.getInt("pubkey_id");
                        byte[] bytes = executeQuery.getBytes("pubkey");
                        ECKey ecKey = new ECKey(null, bytes);
                        ecKey.database_id = pubkey_id;

                        byte public_type = executeQuery.getByte("public_type");
                        long timestamp = executeQuery.getLong("timestamp");
                        int nonce = executeQuery.getInt("nonce");
                        byte[] signature = executeQuery.getBytes("signature");
                        byte[] content = executeQuery.getBytes("content");
                        boolean verified = executeQuery.getBoolean("verified");
                        RawMsg m = new RawMsg(timestamp, nonce, signature, content, verified);
                        m.database_Id = message_id;
                        m.key = ecKey;
                        m.public_type = public_type;
                        //                list.add(rawMsg);

                        if (!m.verified) {
                            continue;
                        }

                        boolean breakLoop = false;

                        while (!breakLoop) {

                            if (!peer.isConnected() || !peer.isAuthed()) {
                                break;
                            }

                            int size = 0;
                            peer.writeBufferLock.lock();
                            size = peer.writeBuffer.position();
                            peer.writeBufferLock.unlock();
                            if (size > 1500) {
                                System.out.println("writeBuffer position: " + size + " ip: " + peer.ip);
                                //peer.setWriteBufferFilled();
                                try {
                                    sleep(100);
                                    System.out.println("SLEEP");

                                } catch (InterruptedException ex) {
                                    Logger.getLogger(ConnectionHandler.class
                                            .getName()).log(Level.SEVERE, null, ex);
                                }
                            } else {
                                breakLoop = true;
                            }

                        }

                        //write Message to sync data.
                        Test.messageStore.addMessageToSendToSpecificPeer(message_id, peer.getPeerTrustData().internalId);

                        peer.writeMessage(m);
                        peer.setWriteBufferFilled();

                        //if (m.timestamp < oldestTime) {
                        oldestTime = m.timestamp;
                        //}

                        Log.put("back sync one message.....!!! sleep!" + " msgid: " + message_id, 1);

                    }

                    peer.writeBufferLock.lock();
                    if (peer.writeBuffer == null) {
                        return;
                    }
                    peer.writeBuffer.put((byte) 71);
                    peer.writeBuffer.putLong(oldestTime);
                    peer.writeBufferLock.unlock();
                    peer.setWriteBufferFilled();

                } catch (SQLException ex) {
                    Logger.getLogger(ConnectionHandler.class
                            .getName()).log(Level.SEVERE, null, ex);
                } finally {
                    if (peer.writeBufferLock.isHeldByCurrentThread()) {
                        peer.writeBufferLock.lock();
                    }
                }

                Log.put("finish!!!!!", 20);

            }
        }.start();
    }

    public static void syncAllMessagesSince(final long time, final Peer peer) {
        new Thread() {

            @Override
            public void run() {
                final String orgName = Thread.currentThread().getName();
                if (!orgName.contains(" ")) {
                    Thread.currentThread().setName(orgName + " - syncMessages");
                }

                setPriority(MIN_PRIORITY);

                int msgsWithoutCPUcheck = 0;

                boolean MXBeanSupported = true;
                try {
                    Class.forName("java.lang.management.ManagementFactory");
                } catch (ClassNotFoundException e) {
                    MXBeanSupported = false;
                }

                try {

                    //currenlty sync 300 msg not more per connection establishment.!
                    int cntRows = -1;
                    while (cntRows == -1) {

//                            if (Settings.SUPERNODE) {
//                                time = 0;
//                            }
                        Log.put("full sync... from: " + time, 20);
                        ResultSet executeQuery = MessageHolder.getAllMessages(time, Long.MAX_VALUE, peer.getPeerTrustData().internalId);
                        Log.put("query okay...", 20);
                        //                        for (RawMsg m : MessageHolder.getAllMessages(time, System.currentTimeMillis())) {

                        cntRows = 0;

////                        if (MXBeanSupported) {
////
////                            ThreadMXBean tmb = ManagementFactory.getThreadMXBean();
////
////                            long time = new Date().getTime() * 1000000;
////                            long cput = 0;
////                            double cpuperc = -1;
////
                        while (executeQuery.next()) {
                            cntRows++;
                            msgsWithoutCPUcheck++;
////                                //System.out.println("asdf");
////
////                                while (msgsWithoutCPUcheck > 10) {
////                                    msgsWithoutCPUcheck = 0;
////                                    if (tmb.isThreadCpuTimeSupported()) {
////                                        if (new Date().getTime() * 1000000 - time > 1000000000) { //Reset once per second
////                                            time = new Date().getTime() * 1000000;
////                                            //cput = tmb.getCurrentThreadCpuTime();
////                                            cput = getTotalCpuTime(tmb);
////                                        }
////
////                                        if (!tmb.isThreadCpuTimeEnabled()) {
////                                            tmb.setThreadCpuTimeEnabled(true);
////                                        }
////
////                                        if (new Date().getTime() * 1000000 - time != 0) {
////                                            //cpuperc = (tmb.getCurrentThreadCpuTime() - cput) / (new Date().getTime() * 1000000.0 - time) * 100.0;
////                                            cpuperc = (getTotalCpuTime(tmb) - cput) / (new Date().getTime() * 1000000.0 - time) * 100.0;
////                                        }
////                                    }
////                                    //System.out.println("checking...");
////
//////If cpu usage is greater then 50%
////                                    if (cpuperc > 20.0) {
////                                        try {
////                                            //sleep for a little bit.
////                                            //System.out.println("sleeping," + peer.ip + " msg id: " + executeQuery.getInt("message_id"));
////                                            sleep(200);
////
////                                        } catch (InterruptedException ex) {
////                                            Logger.getLogger(ConnectionHandler.class
////                                                    .getName()).log(Level.SEVERE, null, ex);
////                                        }
////                                        //System.out.println("sleeeped");
////                                        continue;
////                                    } else {
////                                        break;
////                                    }
////                                }

                            if (!peer.isConnected() || !peer.isAuthed()) {
                                break;
                            }

                            //System.out.println("sending message...");
                            int message_id = executeQuery.getInt("message_id");

                            //System.out.println("Have to send message: " + peer.ip + " - " + message_id);
//                            if (peer.getSendMessages().contains(message_id)) {
//                                continue;
//                            }
                            int pubkey_id = executeQuery.getInt("pubkey_id");
                            byte[] bytes = executeQuery.getBytes("pubkey");
                            ECKey ecKey = new ECKey(null, bytes);
                            ecKey.database_id = pubkey_id;

                            byte public_type = executeQuery.getByte("public_type");
                            long timestamp = executeQuery.getLong("timestamp");
                            int nonce = executeQuery.getInt("nonce");
                            byte[] signature = executeQuery.getBytes("signature");
                            //byte[] content = executeQuery.getBytes("content"); CONTENT ISNT AVAILABLE!! WE DONT NEED THE CONTENT
                            boolean verified = executeQuery.getBoolean("verified");
                            RawMsg m = new RawMsg(timestamp, nonce, signature, null, verified); //CONTENT IS NOT NEEDED!!
                            m.database_Id = message_id;
                            m.key = ecKey;
                            m.public_type = public_type;
                            //                list.add(rawMsg);

                            if (!m.verified) {
                                continue;
                            }

                            boolean breakLoop = false;

                            while (!breakLoop) {

                                if (!peer.isConnected() || !peer.isAuthed()) {
                                    break;
                                }

                                int used = 0;
                                peer.writeBufferLock.lock();

                                if (peer.writeBuffer == null) {
                                    peer.writeBufferLock.unlock();
                                    return;
                                }

                                used = peer.writeBuffer.position();
                                peer.writeBufferLock.unlock();
                                if (used > 2000) {
                                    //System.out.println("SLEEP writeBuffer position: " + used + " ip: " + peer.ip);
                                    //peer.setWriteBufferFilled();
                                    try {
                                        sleep(100);

                                    } catch (InterruptedException ex) {
                                        Logger.getLogger(ConnectionHandler.class
                                                .getName()).log(Level.SEVERE, null, ex);
                                    }
                                } else {
                                    breakLoop = true;
                                }

                            }
                            peer.writeMessage(m);
                            Log.put("sync msg to node " + m.database_Id, 70);
                            peer.setWriteBufferFilled();
//                        System.out.println("send");

                        }

                        if (cntRows == 100) {
                            //we have to wait until the synced messages will be removed from database
                            try {
                                Log.put("we have to wait until the synced messages will be removed from database", 70);
                                sleep(10000);
                            } catch (InterruptedException e) {
                            }
                        }

////                        } else {
////
////                            if (executeQuery == null) {
////                                return;
////                            }
////                            //no MXBean supported, like android
////                            while (executeQuery.next()) {
////
////                                if (!peer.isConnected() || !peer.isAuthed()) {
////                                    break;
////                                }
////
////                                cntRows++;
////
////                                //System.out.println("sending message...");
////                                int message_id = executeQuery.getInt("message_id");
////
////                                int pubkey_id = executeQuery.getInt("pubkey_id");
////                                byte[] bytes = executeQuery.getBytes("pubkey");
////                                ECKey ecKey = new ECKey(null, bytes);
////                                ecKey.database_id = pubkey_id;
////
////                                byte public_type = executeQuery.getByte("public_type");
////                                long timestamp = executeQuery.getLong("timestamp");
////                                int nonce = executeQuery.getInt("nonce");
////                                byte[] signature = executeQuery.getBytes("signature");
////                                byte[] content = executeQuery.getBytes("content");
////                                boolean verified = executeQuery.getBoolean("verified");
////                                RawMsg m = new RawMsg(timestamp, nonce, signature, content, verified);
////                                m.database_Id = message_id;
////                                m.key = ecKey;
////                                m.public_type = public_type;
////                                //                list.add(rawMsg);
////
////                                if (!m.verified) {
////                                    continue;
////                                }
////
////                                boolean breakLoop = false;
////
////                                while (!breakLoop) {
////
////                                    if (!peer.isConnected() || !peer.isAuthed()) {
////                                        break;
////                                    }
////
////                                    int size = 0;
////                                    peer.writeBufferLock.lock();
////                                    size = peer.writeBuffer.position();
////                                    peer.writeBufferLock.unlock();
////                                    if (size > 200) {
////                                        System.out.println("writeBuffer position: " + size + " ip: " + peer.ip);
////                                        //peer.setWriteBufferFilled();
////                                        try {
////                                            sleep(100);
////                                            System.out.println("SLEEP");
////
////                                        } catch (InterruptedException ex) {
////                                            Logger.getLogger(ConnectionHandler.class
////                                                    .getName()).log(Level.SEVERE, null, ex);
////                                        }
////                                    } else {
////                                        breakLoop = true;
////                                    }
////
////                                }
////                                peer.writeMessage(m);
////                                peer.setWriteBufferFilled();
//////                        System.out.println("send");
////
////                            }
////
////                        }
////
                    }

                } catch (SQLException ex) {
                    Logger.getLogger(ConnectionHandler.class
                            .getName()).log(Level.SEVERE, null, ex);
                }
                Log.put("finish", 20);

            }

            private long getTotalCpuTime(ThreadMXBean tmb) {
                long[] allThreadIds = tmb.getAllThreadIds();
                //System.out.println("Total JVM Thread count: " + allThreadIds.length);
                long nano = 0;
                for (long id : allThreadIds) {
                    nano += tmb.getThreadCpuTime(id);
                }
                return nano;
            }
        }.start();
    }

    void addServerSocketChannel(ServerSocketChannel serverSocketChannel) {
        try {
            selector.wakeup();
            SelectionKey key = serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            selector.wakeup();
            System.out.println("added ServerSocketChannel");

        } catch (IOException ex) {
            Logger.getLogger(ConnectionHandler.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    public String readString(ByteBuffer byteBuffer, int length) {

        if (byteBuffer.limit() - byteBuffer.arrayOffset() < length) {
            return null; //not enough bytes rdy!
        }
        byteBuffer.position(byteBuffer.position() + length);
        return new String(byteBuffer.array(), byteBuffer.arrayOffset(), length);
    }

    private void sendHeader(Peer peer) {
        ByteBuffer writeBuffer = peer.writeBuffer;
        peer.writeBufferLock.lock();
        writeBuffer.put(Test.MAGIC.getBytes());
        writeBuffer.put((byte) Test.VERSION);
        writeBuffer.put(Test.NONCE.getBytes());
        putUnsignedShot(writeBuffer, Test.MY_PORT);
        peer.writeBufferLock.unlock();
    }
//    public void sendString(ByteBuffer byteBuffer, String string) {
//        byteBuffer.put(string.getBytes());
//    }

    public void putUnsignedShot(ByteBuffer b, int unsignedShort) {

        b.put(ByteUtils.intToUnsignedShortAsBytes(unsignedShort));

    }

    public int readUnsignedShort(ByteBuffer byteBuffer) {
        byte[] toParse = new byte[2];
        byteBuffer.get(toParse);
        return ByteUtils.bytesToUnsignedShortAsInt(toParse, 0);
    }
//    private void closeConnection(Peer peer, SelectionKey key) throws IOException {
//        peer.setConnected(false);
//        key.cancel();
//        peer.getSocketChannel().close();
//    }

    public static void closeAllSockets() {

        ArrayList<Socket> a = allSockets;

        allSockets = new ArrayList<Socket>();

        int cnt = 0;
        int closed = 0;
        for (Socket s : a) {
            cnt++;
            if (s.isClosed()) {
                closed++;
            }
            try {
                s.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        System.out.println("closed " + cnt + " sockets, already closed " + closed);

    }

    public static void removeUnusedSockets() {

        if (Test.getClonedPeerList() == null) {
            return;
        }

        ArrayList<Socket> toRemove = new ArrayList<Socket>();

        System.out.println("clean sockets: " + allSockets.size());

        synchronized (allSockets) {

            for (Socket socket : allSockets) {

                boolean found = false;

                for (Peer peer : Test.getClonedPeerList()) {

                    if (peer == null) {
                        Main.sendBroadCastMsg("null peer in list? 95178");
                        continue;
                    }

                    SocketChannel socketChannel = peer.getSocketChannel();

                    if (socketChannel == null) {
                        continue;
                    }

                    if (socket.equals(socketChannel.socket())) {
                        found = true;
                        break;
                    }

                }

                if (!found) {
                    if (socket.isClosed()) {
                        Log.put("found socket, which was not closed", 200);
                    } else {
                        Log.put("found closed socket", 200);
                    }
                    try {
                        //socket isnt used by any Peer, can be closed and removed.
                        socket.close();
                    } catch (IOException ex) {
                    }
                    toRemove.add(socket);
                }

            }

            allSockets.removeAll(toRemove);
        }
    }


    public static void insertNewAndroidUpdate() throws IOException {


        //lets test if we have the priv key before generating update
        String keyString = new String(Files.readAllBytes(Paths.get("privateSigningKey.txt")));
        keyString = keyString.replace("\n", "").replace("\r", "");
        System.out.println("privKey: '" + keyString + "'");
        if (SpecialChannels.getUpdateChannel(keyString) == null) {
            throw new RuntimeException("privkey not available!");
        }

        String fileName = "android.apk";

        File file = new File(fileName);

        long timestamp = (long) Math.ceil(file.lastModified() / 1000.) * 1000;

        System.out.println("android timestamp : " + timestamp);

        Path path = Paths.get(fileName);
        byte[] data = Files.readAllBytes(path);

        int updateSize = data.length;

//        data = new byte[32000];


        ByteBuffer toHash = ByteBuffer.allocate(8 + data.length);
        toHash.putLong(timestamp);
        toHash.put(data);


//        System.out.println(kademliaId.toString());
//        System.out.println(Utils.bytesToHexString(keyBytes));
//
//        System.out.println(Utils.bytesToHexString(keyBytes));


        //get priv key from external file:
//            String keyString = new String(Files.readAllBytes(Paths.get("privateSigningKey.txt")));


        //lets build the signing chunk:
        Channel updateChannel = SpecialChannels.getUpdateChannel(keyString);

        Sha256Hash hash = Sha256Hash.create(toHash.array());
        System.out.println("hash: " + Utils.bytesToHexString(hash.getBytes()));
        ECKey.ECDSASignature sign = updateChannel.getKey().sign(hash);


        byte[] signature = sign.toBytes();

        System.out.println("signature: " + Utils.bytesToHexString(signature));
        Test.localSettings.setUpdateAndroidSignature(signature);
        Test.localSettings.save();
        System.out.println("saved in local settings!");


    }


    public static void insertNewUpdate() throws IOException {


        //lets test if we have the priv key before generating update
        String keyString = new String(Files.readAllBytes(Paths.get("privateSigningKey.txt")));
        keyString = keyString.replace("\n", "").replace("\r", "");
        System.out.println("privKey: '" + keyString + "'");
        if (SpecialChannels.getUpdateChannel(keyString) == null) {
            throw new RuntimeException("privkey not available!");
        }


        File file = new File("out/artifacts/redPandaj_jar/redPandaj.jar");

        //windows reports up to ms, linux only up to seconds!
        long timestamp = (long) Math.ceil(file.lastModified() / 1000.) * 1000;

        System.out.println("timestamp : " + timestamp);

        Path path = Paths.get("out/artifacts/redPandaj_jar/redPandaj.jar");
        byte[] data = Files.readAllBytes(path);

        int updateSize = data.length;

//        data = new byte[32000];


        ByteBuffer toHash = ByteBuffer.allocate(8 + data.length);
        toHash.putLong(timestamp);
        toHash.put(data);


//        System.out.println(kademliaId.toString());
//        System.out.println(Utils.bytesToHexString(keyBytes));
//
//        System.out.println(Utils.bytesToHexString(keyBytes));


        //get priv key from external file:
//            String keyString = new String(Files.readAllBytes(Paths.get("privateSigningKey.txt")));


        //lets build the signing chunk:
        Channel updateChannel = SpecialChannels.getUpdateChannel(keyString);

        Sha256Hash hash = Sha256Hash.create(toHash.array());
        System.out.println("hash: " + Utils.bytesToHexString(hash.getBytes()));
        ECKey.ECDSASignature sign = updateChannel.getKey().sign(hash);

        byte[] encodeToDER = new byte[RawMsg.SIGNATURE_LENGRTH];
        byte[] sigBytes = sign.encodeToDER();
        System.arraycopy(sigBytes, 0, encodeToDER, 0, sigBytes.length);

        byte[] newBytes = new byte[encodeToDER.length];

        int index = encodeToDER.length - 1;
        while (true) {
            System.arraycopy(encodeToDER, 0, newBytes, 0, index + 1);
            if (newBytes[index] == (byte) 0) {
                newBytes = new byte[index];

                index--;
            } else {
                break;
            }

        }

        //System.out.println("Sigbytes len: " + sigBytes.length + " " + Utils.bytesToHexString(encodeToDER));
        //System.out.println("Sigbytes len: " + sigBytes.length + " " + Utils.bytesToHexString(newBytes));
        byte[] signature = encodeToDER;

        System.out.println("signature: " + Utils.bytesToHexString(signature));
        Test.localSettings.setUpdateSignature(signature);
        Test.localSettings.save();
        System.out.println("saved in local settings!");


    }


    public static void main(String[] args) throws IOException, InterruptedException, ContentNotFoundException, SignatureException {


        new Thread() {
            @Override
            public void run() {

                int cnt = 0;
                while (cnt < 2) {
                    cnt++;

                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (Test.STARTED_UP_SUCCESSFUL) {
//                        Flaschenpost.put(new KademliaId(), "Test".getBytes());
                        try {
                            Flaschenpost.put(KademliaId.fromBase58("2sdf3THKQ2x474z3v91sfod8xPAT"), "Test".getBytes());
                        } catch (AddressFormatException e) {
                            e.printStackTrace();
                        }

                    }
                }

            }
        }.start();

        Saver saver = new Saver();

        Test.saver = saver;
        Test.localSettings = saver.loadLocalSettings();

//        Thread.sleep(1000);
        insertNewUpdate();

        insertNewAndroidUpdate();
//        Thread.sleep(1000);
        Start.main(null);
    }
}
