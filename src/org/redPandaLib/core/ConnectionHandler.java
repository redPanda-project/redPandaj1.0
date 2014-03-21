/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redPandaLib.core;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.redPandaLib.services.MessageVerifierHsqlDb;
import org.redPandaLib.services.MessageDownloader;
import crypt.Utils;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.security.SecureRandom;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.redPandaLib.ByteUtils;
import org.redPandaLib.Main;
import org.redPandaLib.core.messages.RawMsg;
import org.redPandaLib.crypt.AESCrypt;
import org.redPandaLib.crypt.ECKey;
import org.redPandaLib.crypt.RC4;
import org.redPandaLib.crypt.Sha256Hash;

/**
 *
 * @author robin
 */
public class ConnectionHandler extends Thread {

    public static final int READ_BUFFER_SIZE = 1024 * 200;
    public static Selector selector;
    static ExecutorService threadPool = Executors.newFixedThreadPool(4);

    static {
        try {
            selector = Selector.open();
        } catch (IOException ex) {
            Logger.getLogger(ConnectionHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public ConnectionHandler() {
    }

    public void addConnection(Peer peer, boolean connectionPending) {

        //TODO: remove later when it will be saved...
        //peer.keyToIdHis = new HashMap<Integer, ECKey>();
        //peer.keyToIdMine = new ArrayList<Integer>();

        peer.requestedMsgs = 0;
        //peer.getPendingMessages().clear();//TODO muss weg, wenn das syncen richtig geht

        peer.readBuffer = ByteBuffer.allocate(1024 * 200);
        peer.writeBufferLock.lock();
        peer.writeBuffer = ByteBuffer.allocate(1024 * 200);
        peer.writeBufferLock.unlock();
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
            Logger.getLogger(ConnectionHandler.class.getName()).log(Level.SEVERE, null, ex);
        }

        sendHeader(peer);
    }

    @Override
    public void run() {

        final String orgName = Thread.currentThread().getName();
        Thread.currentThread().setName(orgName + " - IncomingHandler - Main");

        while (!Main.shutdown) {

            //System.out.println("NEW KEY RUN!!!!");
            int readyChannels = 0;
            try {
                readyChannels = selector.select();
            } catch (Exception e) {
                //Main.sendBroadCastMsg("key was canceled...");
                try {
                    sleep(100);
                } catch (InterruptedException ex) {
                    Logger.getLogger(ConnectionHandler.class.getName()).log(Level.SEVERE, null, ex);
                }
                continue;
            }


            //System.out.println("" + selector.isOpen() + " " + selector.keys().size());

            Set<SelectionKey> selectedKeys = selector.selectedKeys();

            if (readyChannels == 0 && selectedKeys.isEmpty()) {
                System.out.print(".");

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
                    ByteBuffer readBuffer = peer.readBuffer;
                    ByteBuffer writeBuffer = peer.writeBuffer;

                    if (key.isConnectable()) {

                        //System.out.println("dwdhjawdgawgd ");

                        boolean connected = false;
                        try {
                            connected = peer.getSocketChannel().finishConnect();
                        } catch (IOException e) {
                        }
//                            System.out.println("finished!");

                        if (!connected) {
                            Log.put("connection could not be established...", 6);
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
                            read = peer.getSocketChannel().read(readBuffer);
                        } else {

                            if (peer.readKey == null) {
                                System.out.println("ERROR 14276, encryption enabled, but I got no key?!?");
                                peer.disconnect();
                                continue;
                            }

                            read = peer.getSocketChannel().read(peer.readBufferCrypted);
                            peer.readBufferCrypted.flip();

                            int readCryptBytes = Math.min(peer.readBufferCrypted.remaining(), peer.readBuffer.remaining());

                            byte[] buffer = new byte[readCryptBytes];
                            peer.readBufferCrypted.get(buffer);
                            peer.readBufferCrypted.compact();
                            byte[] decrypt = peer.readKey.decrypt(buffer);
                            peer.readBuffer.put(decrypt);

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
                            //succesfully read some bytes...
                            PeerTrustData peerTrustData = peer.getPeerTrustData();

                            if (peerTrustData != null && !peerTrustData.sendMessages.isEmpty()) {
                                peerTrustData.lastSuccessfulySendMessageHeader = peerTrustData.sendMessages.get(peerTrustData.sendMessages.size() - 1);
                            }
                        }

                        if (read == 0) {

                            if (readBuffer.hasRemaining()) {
                                //System.out.println("dgqdgqzudg23c365nx367t34t53 " + peer.readBuffer + " " + key.isReadable() + " " + key.isValid());
                                System.out.println("dafuq!");
                                //closeConnection(peer, key);
                                peer.disconnect();
                                continue;
                            }

                            if (peer.readBuffer.capacity() * 2 > 1024 * 1024) {
                                //throw new RuntimeException("buffer to high exiting!!!!");
                                System.exit(-199);
                            }
                            ByteBuffer allocate = ByteBuffer.allocate(peer.readBuffer.capacity() * 2);
                            allocate.put(peer.readBuffer.array());
                            allocate.position(peer.readBuffer.position());
//                            allocate.limit(peer.readBuffer.limit());
                            //System.out.println("buffer voll?? " + peer.readBuffer.toString());
                            peer.readBuffer = allocate;
//                                System.out.println("readBuffer full, generated new one with 2x larger size....");
                        } else if (read == -1) {
                            System.out.println("closing connection: not readable! " + readBuffer);
                            peer.disconnect();
                        } else {

                            peer.lastActionOnConnection = System.currentTimeMillis();
                            //System.out.println("setting lastActionOn: " + peer.nonce);

//                                System.out.println("a: " + read + " ");
//                            System.out.println(new String(.array(), 0, read));
                            //peer.getSocketChannel().write(allocate);


                            int pos = peer.readBuffer.position();
                            int lim = peer.readBuffer.limit();
                            int parsedBytes = 0;

                            peer.readBuffer.flip(); //switch buffer for reading
//                                System.out.println("incoming string: " + new String(peer.readBuffer.array(), peer.readBuffer.arrayOffset(), peer.readBuffer.limit()));


                            if (!peer.firstCommandsProceeded) {

                                if (peer.readBuffer.limit() > 14) { //start needs to be 15 bytes!
                                    String magic = readString(peer.readBuffer, 4);
                                    int version = (int) peer.readBuffer.get();
                                    long nonce = readBuffer.getLong();
//                                    byte[] nonce = new byte[10];
//                                    peer.readBuffer.get(nonce);
                                    int port = readUnsignedShort(readBuffer);

                                    Log.put("Verbindungsaufbau (" + peer.ip + "): " + magic + " " + version + " " + nonce + " " + port, 10);



                                    if (magic != null && magic.equals(Test.MAGIC) && version == Test.VERSION) {
                                        //System.out.println("ready! " + magic);

                                        peer.port = port;
                                        peer.nonce = nonce;

                                        if (nonce == Test.NONCE) {
                                            System.out.println("found myself!" + peer.ip + ":" + peer.port);

                                            Test.removeByIpAndPortPeer(peer);
                                            peer.disconnect();
                                            continue;
                                        }


                                        //addPeer(new Peer(socket.getInetAddress().getHostAddress(), port));
//                                        Peer newPeer = new Peer(peer.ip, port);
//                                        newPeer.nonce = nonce;

                                        boolean found = false;
//                                            boolean closeNew = false;

                                        synchronized (Test.peerList) {

                                            for (Peer pfromList : (ArrayList<Peer>) Test.peerList.clone()) {


                                                if (pfromList.equals(peer)) {
                                                    continue;
                                                }


                                                if (peer.equalsNonce(pfromList)) {
                                                    found = true;

//                                                        System.out.println("nonce1: " + peer.nonce + " nonce2: " + pfromList.nonce);
                                                    if (pfromList.isConnected() || pfromList.isConnecting) {
                                                        System.out.println("Peer already in peerlist... closing old connection...");
                                                        pfromList.disconnect();
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

                                        }
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

                                        peer.retries = 0;




                                        peer.writeBufferLock.lock();
                                        //first ping...
                                        peer.lastPinged = System.currentTimeMillis();
                                        writeBuffer.put((byte) 100); //ping


                                        //init auth
                                        boolean foundAuthKey = false;
                                        for (PeerTrustData pt : Test.peerTrusts) {
                                            if (pt.nonce == peer.nonce) {
                                                foundAuthKey = true;

//                                                        System.out.println("authkey found for nonce: " + peer.nonce);

                                                Random r = new SecureRandom();

                                                byte[] toEncrypt = new byte[32];
                                                r.nextBytes(toEncrypt);

                                                writeBuffer.put((byte) 52);
                                                writeBuffer.put(toEncrypt);

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
                                        System.out.println("WRONG magic and or protocoll - VERSION, deleting peer.... " + peer.ip + " " + peer.port);
                                        Test.removeByIpAndPortPeer(peer);
                                        peer.disconnect();

                                    }
                                    parsedBytes = 15;
                                }


                            } else {


                                int localParsedBytes = 0;
                                while (peer.isConnected()) {
                                    byte command = peer.readBuffer.get();
                                    localParsedBytes = parseCommands(command, peer, parsedBytes, writeBuffer, readBuffer);
                                    peer.setWriteBufferFilled(); //TODO: nachher besser machen?
                                    if (peer.isCryptedConnection()) {
                                        peer.parsedCryptedBytes += localParsedBytes;
                                    }

                                    if (localParsedBytes == -1) {
                                        parsedBytes = 1; //somit ist nach dem naechste befehl alles auf 0 gesetzt! Buffer wurde entschluesselt... CMD 55!
                                    }

                                    parsedBytes += localParsedBytes;

                                    if (localParsedBytes == 0 || !readBuffer.hasRemaining()) {
                                        break;
                                    }
                                    peer.readBuffer.position(parsedBytes);
//                                        System.out.println("parseBytes: " + parsedBytes + " " + peer.readBuffer);

//                                        System.out.println("parseBytes: " + parsedBytes + " " + peer.readBuffer);
                                }









                            }

                            //System.out.println("bytes in readbuffer: " + peer.readBuffer.remaining() + " parsedbytes: " + parsedBytes);

                            //int parseBytes = peer.readBuffer.limit() - peer.readBuffer.arrayOffset();


                            readBuffer.position(parsedBytes);


                            readBuffer.compact();

                            //peer.readBuffer.flip(); //switch buffer for writing
                            //readBuffer.limit(readBuffer.capacity());


                            //peer.readBuffer.limit(lim);
                            //peer.readBuffer.position(pos - parsedBytes);


                        }

                    } else if (key.isWritable()) {

                        //System.out.println("have to write from buffer...");

                        peer.writeBufferLock.lock();

//                                int position = writeBuffer.position();
//                                int limit = writeBuffer.limit();
                        int writtenBytes = 0;
                        boolean remainingBytes = false;
                        peer.writeBuffer.flip();

                        if (peer.writeBufferCrypted == null) {
                            remainingBytes = peer.writeBuffer.hasRemaining();
                        } else {
                            peer.writeBufferCrypted.flip();
                            remainingBytes = peer.writeBufferCrypted.hasRemaining();
                            peer.writeBufferCrypted.compact();
                        }

                        //switch buffer for reading
                        if (!remainingBytes) {
                            key.interestOps(SelectionKey.OP_READ);
                        } else {
                            //System.out.println("write from buffer...");
                            writtenBytes = peer.writeBytesToPeer(writeBuffer);
                        }
                        writeBuffer.compact();
//                                peer.writeBuffer.flip(); //switch buffer for writing
//                                writeBuffer.limit(writeBuffer.capacity());
//                            writeBuffer.limit(limit);
//                            writeBuffer.position(position - writtenBytes);

                        //System.out.println("wrote from buffer...");

                        Test.outBytes += writtenBytes;
                        peer.writeBufferLock.unlock();

                    }

                } catch (IOException e) {
                    Peer peer = ((Peer) key.attachment());
                    System.out.println("error! " + peer.ip + ":" + peer.port);
                    //e.printStackTrace();
                    //closeConnection(peer, key);
                    peer.disconnect();





                }

            }
        }

    }

    public static void sendNewAuthKey(Peer peer) {
        peer.peerTrustData = new PeerTrustData();

        Random r = new SecureRandom();

        byte[] myPart = new byte[16];
        r.nextBytes(myPart);




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

        System.out.println("requested new authkey... " + peer.nonce);

    }

    private int parseCommands(byte command, final Peer peer, int parsedBytes, final ByteBuffer writeBuffer, final ByteBuffer readBuffer) {

        if (peer.getPeerTrustData() == null) {

            if (command == (byte) 1 || command == (byte) 2 || command == (byte) 10 || command == (byte) 51 || command == (byte) 52 || command == (byte) 53 || command == (byte) 55 || command == (byte) 100 || command == (byte) 101) {
                // erlaubte befehle welche ohne verschluesselung ausgefuehrt werden duerfen
            } else {
                System.out.println("PEER wanted to send a command (" + command + ") which is only allowed after auth. : " + peer.ip);
                peer.disconnect();
                return 0;
            }

        }

        //System.out.println(peer.ip + " incoming command: " + command);
        if (command == (byte) 1) {
            Log.put("Command: get PeerList", 8);

            ByteBuffer tempWriteBuffer = ByteBuffer.allocate(2048);
            tempWriteBuffer.put((byte) 2);


            ArrayList<Peer> clonedPeerList = Test.getClonedPeerList();
            ArrayList<Peer> validPeers = new ArrayList<Peer>();
            ArrayList<Peer> validPeersIPv6 = new ArrayList<Peer>();
            for (Peer p1 : clonedPeerList) {

                if (p1.equals(peer)) {
                    continue;
                }


                if (!p1.isConnected()) {
                    continue;
                }


                if (p1.connectAble == 1 && p1.ip.split("\\.").length == 4) {
                    validPeers.add(p1);
                }
                if (p1.connectAble == 1 && p1.ip.split("\\.").length != 4) {
                    validPeersIPv6.add(p1);
                }

            }

            if (validPeers.size() > 254) {
                System.out.println("WARNING, error code: Xj3GFc3xui (code must be extened for so much peers...)");
                return 1;
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
                tempWriteBuffer.put((byte) 10);
                tempWriteBuffer.put((byte) p1.ip.length());
                tempWriteBuffer.put(p1.ip.getBytes());
                putUnsignedShot(tempWriteBuffer, p1.port);
            }

            peer.writeBufferLock.lock();
            tempWriteBuffer.flip();
            writeBuffer.put(tempWriteBuffer);
            peer.writeBufferLock.unlock();
            peer.setWriteBufferFilled();
            return 1;
        } else if (command == (byte) 2) {
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
                Peer newPeer = new Peer(ip, port);
                //System.out.println("Peerexchange: new peer " + ip + ":" + port);
                Test.findPeer(newPeer);

            }


            return 1 + 2 + needBytes;

        } else if (command == (byte) 3) {
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

            if (howMuchAddresses * 33 > readBuffer.remaining()) {
                return 0; //need more bytes in readBuffer...
            }




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
//            Log.put("Command: have message", 8);

            if (21 > readBuffer.remaining()) {
                return 0;
            }

            int pubkey_id_local = readBuffer.getInt();
            byte public_type = readBuffer.get();
            long timestamp = readBuffer.getLong();
            int nonce = readBuffer.getInt();
            int messageId = readBuffer.getInt();


            ECKey id2KeyHis = peer.getId2KeyHis(pubkey_id_local);


            //System.out.println("Key for message: " + Channel.byte2String(id2KeyHis.getPubKey()));

            if (id2KeyHis == null) {
                System.out.println("WARNING: key not found -.- " + pubkey_id_local + " " + peer.getKeyToIdHis().size());

//                for (Entry<Integer, ECKey> entry :peer.keyToIdHis.entrySet()) {
//                    
//                    System.out.println("");
//                    
//                }


                return 1 + 21;
            }




            RawMsg rawMsg = new RawMsg(id2KeyHis, timestamp, nonce);
            rawMsg.public_type = public_type;
            //boolean contains = MessageHolder.contains(rawMsg);

            //System.out.println("Peer got following msg: " + timestamp + " " + channelId + " " + nonce + " " + messageId);

            //if (!contains) {



//                System.out.println("found msg i want to have...");
            peer.addPendingMessage(messageId, rawMsg);
            peer.getPeerTrustData().addIntroducedMessage(messageId);

            peer.getPeerTrustData().synchronizedMessages++;

            //System.out.println("added pending msg... + " + messageId);


//                synchronized (writeBuffer) {
//                    writeBuffer.put((byte) 6);
//                    writeBuffer.putInt(messageId);
//                }


            //}

            return 1 + 21;

        } else if (command == (byte) 6) {
            Log.put("Command: get message content", 8);
            if (4 > readBuffer.remaining()) {
                return 0;
            }

            final int id = readBuffer.getInt();

            Runnable runnable = new Runnable() {
                @Override
                public void run() {

                    final String orgName = Thread.currentThread().getName();
                    Thread.currentThread().setName(orgName + " - send Message Thread");

                    RawMsg rawMsg = null;
                    rawMsg = MessageHolder.getRawMsg(id);
                    if (rawMsg == null) {
                        System.out.println("HM diese Nachricht habe ich nicht, id: " + id + " ip: " + peer.getIp());
                        return;
                    }
                    //ToDo: msg might have been deleted due to false signature...

                    peer.writeBufferLock.lock();
                    writeBuffer.put((byte) 7);
                    writeBuffer.putInt(id);
                    writeBuffer.put(rawMsg.getSignature());
                    writeBuffer.putInt(rawMsg.getContent().length);

//                        if (writeBuffer.remaining() < rawMsg.getContent().length) {
//                            ByteBuffer allocate = ByteBuffer.allocate(writeBuffer.capacity() + rawMsg.getContent().length + 30);
//                            allocate.put(writeBuffer.array());
//                            //allocate.position(writeBuffer.position());
//                            peer.writeBuffer = allocate;
//                        }
                    //System.out.println("SIZE:" + writeBuffer.remaining() + " " + rawMsg.getContent().length);
                    peer.writeBuffer.put(rawMsg.getContent());
                    peer.writeBufferLock.unlock();
                    peer.setWriteBufferFilled();


                }
            };


            threadPool.execute(runnable);


            return 1 + 4;


        } else if (command == (byte) 7) {



            if (4 + RawMsg.SIGNATURE_LENGRTH + 4 > readBuffer.remaining()) {
                return 0;
            }

            final int msgId = readBuffer.getInt();



            byte[] signature = new byte[RawMsg.SIGNATURE_LENGRTH];
            readBuffer.get(signature);

            //System.out.println("SIGNATURE  : " + Utils.bytesToHexString(signature));

            int contentLength = readBuffer.getInt();

            if (contentLength > readBuffer.remaining()) {
                return 0;
            }

//            Log.put("Command: msg content incoming...", 8);

            byte[] content = new byte[contentLength];
            readBuffer.get(content);
            final RawMsg get = peer.getPendingMessages().get(msgId);
            //RawMsg get = Test.messageStore.getMessageById(msgId);



            peer.requestedMsgs--;



            if (get == null) {
                System.out.println("WARNING: found a message, likely hard timeout out already, skipping content... " + msgId + " " + peer.getPendingMessages().size());
                return 1 + 4 + RawMsg.SIGNATURE_LENGRTH + 4 + contentLength;
            }

            get.signature = signature;
            get.content = content;

            threadPool.execute(
                    new Runnable() {
                @Override
                public void run() {
                    final String orgName = Thread.currentThread().getName();
                    Thread.currentThread().setName(orgName + " - addMessage + ev. broadcast");
                    RawMsg addMessage = MessageHolder.addMessage(get);

                    System.out.println("DWGDYWGDYW " + addMessage.key.database_id);

                    synchronized (peer.getPendingMessages()) {
                        peer.getPendingMessages().remove(msgId);
                    }



                    if (addMessage.getChannel() == null) {
                        //isPublic...
                        MessageDownloader.publicMsgsLoaded++;
                    }


//                    System.out.println("KEY: " + Channel.byte2String(addMessage.key.getPubKey()));
//                    System.out.println("timestamp: " + addMessage.timestamp);
//                    System.out.println("CONTENT hash: " + Sha256Hash.create(addMessage.content));
//                    System.out.println("signature hash: " + Sha256Hash.create(addMessage.signature));

                    synchronized (peer.getLoadedMsgs()) {
                        peer.getLoadedMsgs().add(addMessage.database_Id);
                    }





                    if (!Settings.BROADCAST_MSGS_AFTER_VERIFICATION) {
                        Test.broadcastMsg(addMessage);
                    }

                    MessageDownloader.trigger();
                    MessageVerifierHsqlDb.trigger();

                }
            });


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










            return 1 + 4 + RawMsg.SIGNATURE_LENGRTH + 4 + contentLength;

        } else if (command == (byte) 8) {
            //            Log.put("Command: control data request...", 8);
            PeerTrustData peerTrustData = peer.getPeerTrustData();

            writeBuffer.put((byte) 9);
            writeBuffer.putInt(peerTrustData.introducedMessages.size());
            writeBuffer.putInt(peerTrustData.keyToIdHis.size());
            writeBuffer.putInt(peerTrustData.keyToIdMine.size());
            writeBuffer.putInt(peerTrustData.sendMessages.size());

            String toHashkeyToIdHis = "";
            for (Entry<Integer, ECKey> entry : peerTrustData.keyToIdHis.entrySet()) {
                int key = entry.getKey();

                toHashkeyToIdHis += "" + key;

            }

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



            return 1;

        } else if (command == (byte) 9) {

            if (1 + 4 + 4 + 4 + 4 + 4 + 4 > readBuffer.remaining()) {
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
            System.out.println("introducedMessages: " + peerTrustData.introducedMessages.size() + " - " + sendMessages);
            System.out.println("keyToIdHis: " + peerTrustData.keyToIdHis.size() + " - " + keyToIdMine);
            System.out.println("keyToIdMine: " + peerTrustData.keyToIdMine.size() + " - " + keyToIdHis);
            System.out.println("sendMessages: " + peerTrustData.sendMessages.size() + " - " + introducedMessages);

            String toHashkeyToIdHis = "";
            for (Entry<Integer, ECKey> entry : peerTrustData.keyToIdHis.entrySet()) {
                int key = entry.getKey();

                toHashkeyToIdHis += "" + key;

            }

            String toHashkeyToIdMine = "";
            for (int key : peerTrustData.keyToIdMine) {
                toHashkeyToIdMine += "" + key;
            }

            try {
                int myHashkeyToIdMine = Sha256Hash.create(toHashkeyToIdMine.getBytes("UTF-8")).hashCode();
                int myHashkeyToIdHis = Sha256Hash.create(toHashkeyToIdHis.getBytes("UTF-8")).hashCode();

                System.out.println("Hash keyToIdMine: " + myHashkeyToIdHis + " - " + hashkeyToIdMine);

                if (peerTrustData.keyToIdHis.size() != keyToIdMine || myHashkeyToIdHis != hashkeyToIdMine) {
                    System.out.println("keyToIdHis wrong, clearing....");
                    System.out.println("list: " + toHashkeyToIdHis);
                    peerTrustData.keyToIdHis.clear();
                }


                System.out.println("Hash keyToIdMine: " + myHashkeyToIdMine + " - " + hashkeyToIdHis);

                if (peerTrustData.keyToIdMine.size() != keyToIdHis || myHashkeyToIdMine != hashkeyToIdHis) {
                    System.out.println("keyToIdMine wrong, clearing....");
                    System.out.println("list: " + toHashkeyToIdMine);
                    peerTrustData.keyToIdMine.clear();
                }
            } catch (UnsupportedEncodingException ex) {
                Logger.getLogger(ConnectionHandler.class.getName()).log(Level.SEVERE, null, ex);
            }

            while (peerTrustData.sendMessages.size() > introducedMessages) {
                peerTrustData.sendMessages.remove(peerTrustData.sendMessages.size() - 1);
                System.out.println("triming sendMessages...");
            }

            while (peerTrustData.introducedMessages.size() > sendMessages) {
                peerTrustData.introducedMessages.remove(peerTrustData.introducedMessages.size() - 1);
                System.out.println("triming introducedMessages...");
            }



            return 1 + 4 + 4 + 4 + 4 + 4 + 4;

        } else if (command == (byte) 10) {

            if (1 + +1 + 2 > readBuffer.remaining()) {
                return 0;
            }

            byte get = readBuffer.get();

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
                System.out.println("dafuq, thought I had an authkey for node. Generating new AuthKey: " + peer.nonce);
                //peer.peerTrustData = new PeerTrustData();

                sendNewAuthKey(peer);

            }


            if (peer.peerIsHigher()) {
                System.arraycopy(otherAuthKeyBytes, 0, peer.peerTrustData.authKey, 16, 16);
            } else {
                System.arraycopy(otherAuthKeyBytes, 0, peer.peerTrustData.authKey, 0, 16);
            }

            peer.peerTrustData.nonce = peer.nonce;
            Test.peerTrusts.add(peer.peerTrustData);

            System.out.println("added key from other side... " + peer.nonce);




            System.out.println("requesting auth ...: " + peer.nonce);

            Random r = new SecureRandom();

            byte[] toEncrypt = new byte[32];
            r.nextBytes(toEncrypt);

            writeBuffer.put((byte) 52);
            writeBuffer.put(toEncrypt);

            peer.toEncodeForAuthFromMe = toEncrypt;
            //peer.peerTrustData = pt;

//            System.out.println("request authentication...");

            peer.setWriteBufferFilled();

            return 1 + 16;


        } else if (command == (byte) 52) {

            if (32 > readBuffer.remaining()) {
                return 0;
            }
            byte[] toEnc = new byte[32];
            readBuffer.get(toEnc);


            boolean send = false;
            for (PeerTrustData pt : Test.peerTrusts) {

                if (pt.nonce != peer.nonce) {
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

                if (pt.nonce != peer.nonce) {
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
                        System.out.println("Im higher");
                        System.arraycopy(peer.toEncodeForAuthFromMe, 0, rc4CrpytKeySeedWrite, 32, 32);
                        System.arraycopy(peer.toEncodeForAuthFromHim, 0, rc4CrpytKeySeedWrite, 64, 32);

                        System.arraycopy(peer.toEncodeForAuthFromHim, 0, rc4CrpytKeySeedRead, 32, 32);
                        System.arraycopy(peer.toEncodeForAuthFromMe, 0, rc4CrpytKeySeedRead, 64, 32);
                    } else {
                        System.out.println("Im lower");
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

                    System.out.println("KABUM, initialisiere Verschluesselung, key fuer ARC4  " + Utils.bytesToHexString(rc4CryptKeyFalseWrite) + " -- " + Utils.bytesToHexString(rc4CryptKeyFalseRead));
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
                    System.out.println("PEER is now trusted...");

                    if (peer.peerTrustData.ips.contains(peer.ip)) {
                        System.out.println("IP schon in den trusted Data...");
                    } else {
                        peer.peerTrustData.ips.add(peer.ip);
                        peer.peerTrustData.port = peer.port;
                        System.out.println("IP added and port set!");
                    }


                    peer.writeBufferLock.lock();
                    //ab nun werden alle bytes verschluesselt, dies wird mit dem folgenden Befehl gestarted
                    writeBuffer.put((byte) 55);
                    //schicke dieses byte noch unverschluesselt...
                    peer.setWriteBufferFilled();

                    if (peer.writeBuffer.position() != 0) {
                        System.out.println("ACHTUNG konnte nun verschluessel byte nicht direkt schicken...");
                    }

//                        try {
//                            sleep(3000);
//                        } catch (InterruptedException ex) {
//                            Logger.getLogger(ConnectionHandler.class.getName()).log(Level.SEVERE, null, ex);
//                        }

                    //TODO make better
                    //peer.writeBufferCrypted = ByteBuffer.allocate(peer.writeBuffer.capacity());
                    //peer.readBufferCrypted = ByteBuffer.allocate(peer.readBuffer.capacity());
                    peer.writeBufferCrypted = ByteBuffer.allocate(1024 * 1024);



                    //FULL CONNECTED TO NODE!
                    //NUN darf die Verbindung genutzt werden zum syncen usw...

                    writeBuffer.put((byte) 8);

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

                    peer.getPeerTrustData().removeNotSuccesfulSendMessages();

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
                peer.disconnect();
                return 0;
            }

            peer.readBufferCrypted = ByteBuffer.allocate(READ_BUFFER_SIZE);
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

        } else if (command == (byte) 101) {

            double a = ((System.currentTimeMillis() - peer.lastPinged) / 10.);
            double b = peer.ping * 9 / 10.;
            double c = a + b;
            peer.ping = c;
//            System.out.println("ping: " + (System.currentTimeMillis() - peer.lastPinged) + " avg.: " + Math.round(c*100)/100.);

            return 1;
        } else if (command == (byte) 100) {
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
            peer.disconnect();
            return 1;
        }

        System.out.println(
                "Wrong protocol, disconnecting + removing peer, Command was: " + command + " crypt-Stauts: out: " + (peer.writeBufferCrypted != null) + ", in: " + (peer.readBufferCrypted != null));
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
        peer.disconnect();
        //TODO add again
        //Test.removePeer(peer);

        return 0;

    }

    private void syncAllMessagesSince(final long time, final Peer peer) {
        new Thread() {
            @Override
            public void run() {
                final String orgName = Thread.currentThread().getName();
                Thread.currentThread().setName(orgName + " - syncMessages");

                try {

//                            if (Settings.SUPERNODE) {
//                                time = 0;
//                            }
                    System.out.println("full sync... from: " + time);
                    ResultSet executeQuery = MessageHolder.getAllMessages(time, Long.MAX_VALUE);
                    System.out.println("query okay...");
                    //                        for (RawMsg m : MessageHolder.getAllMessages(time, System.currentTimeMillis())) {
                    while (executeQuery.next()) {
//                        System.out.println("sending message...");
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



                        if (!peer.isConnected() || !peer.isAuthed()) {
                            break;
                        }

                        if (peer.getSendMessages().contains(m.database_Id)) {
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
                            if (size != 0) {
                                System.out.println("writeBuffer position: " + size + " ip: " + peer.ip);
                                //peer.setWriteBufferFilled();
                                try {
                                    sleep(100);
                                    System.out.println("SLEEP");
                                } catch (InterruptedException ex) {
                                    Logger.getLogger(ConnectionHandler.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            } else {
                                breakLoop = true;
                            }

                        }
                        peer.writeMessage(m);
//                        System.out.println("send");


                    }
                } catch (SQLException ex) {
                    Logger.getLogger(ConnectionHandler.class.getName()).log(Level.SEVERE, null, ex);
                }
                System.out.println("finish");

            }
        ;
    }

    .start();
    }

    void addServerSocketChannel(ServerSocketChannel serverSocketChannel) {
        try {
            selector.wakeup();
            SelectionKey key = serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            selector.wakeup();
            System.out.println("added ServerSocketChannel");








        } catch (IOException ex) {
            Logger.getLogger(ConnectionHandler.class.getName()).log(Level.SEVERE, null, ex);
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
        writeBuffer.putLong(Test.NONCE);
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
}
