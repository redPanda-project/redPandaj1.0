/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redPandaLib.core;

import org.redPandaLib.services.MessageDownloader;
import java.util.Map.Entry;
import java.util.Set;
import org.redPandaLib.core.messages.RawMsg;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.redPandaLib.Main;
import org.redPandaLib.crypt.ECKey;
import org.redPandaLib.crypt.RC4;

/**
 *
 * @author rflohr
 */
public class Peer implements Comparable<Peer> {

    String ip;
    int port;
    public int connectAble = 0;
    public long retries = 0;
    long lastRetryAfter5 = 0;
    long lastActionOnConnection = 0;
    int cnt = 0;
    public long connectedSince = 0;
    long lastAllMsgsQuerried = Settings.till;
    long nonce;
    private ArrayList<String> filterAdresses;
    private SocketChannel socketChannel;
//    public ArrayList<ByteBuffer> readBuffers = new ArrayList<ByteBuffer>();
//    public ArrayList<ByteBuffer> writeBuffers = new ArrayList<ByteBuffer>();
    public ByteBuffer readBuffer;
    public ByteBuffer writeBuffer;
    private SelectionKey selectionKey;
    public boolean firstCommandsProceeded;
    private boolean connected = false;
    public boolean isConnecting;
    public long lastPinged = 0;
    public double ping = 0;
    public int requestedMsgs = 0;
    public PeerTrustData peerTrustData;
    byte[] toEncodeForAuthFromMe;
    byte[] toEncodeForAuthFromHim;
    boolean requestedNewAuth;
    boolean authed = false;
    RC4 writeKey;
    RC4 readKey;
    ByteBuffer writeBufferCrypted;
    ByteBuffer readBufferCrypted;
    public int trustRetries = 0;
    public final ReentrantLock writeBufferLock = new ReentrantLock();
    public Thread connectinThread;
    public int parsedCryptedBytes = 0;
    public long syncMessagesSince = 0;
    public ArrayList<Integer> removedSendMessages = new ArrayList<Integer>();
    public int maxSimultaneousRequests = 1;

    public Peer(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public boolean equalsIpAndPort(Object obj) {

        if (obj instanceof Peer) {

            Peer n2 = (Peer) obj;

            return (ip.equals(n2.ip) && port == n2.port);

        } else {
            return false;
        }

    }

    public boolean equalsNonce(Object obj) {

        if (obj instanceof Peer) {

            Peer n2 = (Peer) obj;

            //return (ip.equals(n2.ip) && port == n2.port && nonce == n2.nonce);
            return nonce == n2.nonce;

        } else {
            return false;
        }

    }

    public long getLastAnswered() {
        return System.currentTimeMillis() - lastActionOnConnection;
    }

//    void writeTo(String out) {
//        if (connectionThread != null) {
//            connectionThread.writeString(out);
////            if (Test.DEBUG) {
////                System.out.println("outout: " + out);
////            }
//        }
//    }
    public PeerSaveable toSaveable() {
//        ArrayList<Integer> loadedMsgsCloned = (ArrayList<Integer>) peerTrustData.loadedMsgs.clone();
//        ArrayList<Integer> sendMessagesCloned = (ArrayList<Integer>) peerTrustData.sendMessages.clone();
//        HashMap<Integer, RawMsg> pendingMessagesCloned = (HashMap<Integer, RawMsg>) peerTrustData.pendingMessages.clone();
//        HashMap<Integer, RawMsg> pendingMessagesPublicCloned = (HashMap<Integer, RawMsg>) peerTrustData.pendingMessagesPublic.clone();
        return new PeerSaveable(ip, port, lastAllMsgsQuerried, nonce, retries);
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    @Override
    public int compareTo(Peer o) {

        return o.getPriority() - getPriority();

//        int ret = (int) (retries - o.retries);
//
//        if (ret != 0) {
//            return ret;
//        }
//
//
//        int a = (int) (o.lastActionOnConnection - lastActionOnConnection);
//
//        if (a != 0) {
//            return a;
//        }
//
//        return (int) (o.lastAllMsgsQuerried - lastAllMsgsQuerried);
    }

    public int getPriority() {

        int a = 0;

        if (connected) {
            a += 2000;
        }

        if (nonce == 0) {
            a -= 1000;
        }

        if (ip.contains(":")) {
            a += 50;
        }

        if (peerTrustData != null) {
            if (peerTrustData.loadedMsgs != null) {
                a += peerTrustData.loadedMsgs.size();
            }

            a -= peerTrustData.badMessages * 100;

        }

        a += -retries * 200;

        return a;
    }

    public boolean iSameInstance(Peer p) {
        return super.equals(p);
    }

    public SocketChannel getSocketChannel() {
        return socketChannel;
    }

    public void setSocketChannel(SocketChannel socketChannel) {
        this.socketChannel = socketChannel;
    }
//    public void addWriteBuffer(ByteBuffer write) {
//        writeBuffers.add(write);
//        SelectionKey key = socketChannel.keyFor(ConnectionHandler.selector);
//        key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
//    }
//
//    public void addReadBuffer(ByteBuffer read) {
//        readBuffers.add(read);
//    }
//
//    public ArrayList<ByteBuffer> getReadBuffers() {
//        return readBuffers;
//    }
//
//    public ArrayList<ByteBuffer> getWriteBuffers() {
//        return writeBuffers;
//    }

    public void disconnect(String reason) {

        try {
            writeBufferLock.tryLock(2, TimeUnit.SECONDS);

            Log.put("DISCONNECT: " + reason, 30);

            setConnected(false);

            removeRequestedMsgs();

            if (isConnecting && connectinThread != null) {
                connectinThread.interrupt();
            }

            isConnecting = false;

            if (selectionKey != null) {
                selectionKey.cancel();
            }
            if (socketChannel != null) {
//            ByteBuffer a = ByteBuffer.allocate(1);
//            a.put((byte) 254);
//            a.flip();
//            try {
//                int write = socketChannel.write(a);
//                //System.out.println("QUIT bytes: " + write);
//            } catch (IOException ex) {
//            } catch (NotYetConnectedException e) {
//            }
                try {
                    socketChannel.close();
                } catch (IOException ex) {
                }
            }

            readBuffer = null;
            readBufferCrypted = null;
            writeBuffer = null;
            writeBufferCrypted = null;

            if (writeBufferLock.isHeldByCurrentThread()) {
                writeBufferLock.unlock();
            }
        } catch (InterruptedException ex) {
            Logger.getLogger(Peer.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public void removeRequestedMsgs() {

        if (peerTrustData != null) {
            MessageDownloader.requestedMsgsLock.lock();
            for (MessageDownloader.RawMsgEntry msg : (ArrayList<MessageDownloader.RawMsgEntry>) MessageDownloader.requestedMsgs.clone()) {
                if (msg.requestedFromPeer == this) {
                    MessageDownloader.requestedMsgs.remove(msg);
                    System.out.println("REMOVED MSG FROM REQUESTED, peer disconnected!!!!");
                }
            }
            MessageDownloader.requestedMsgsLock.unlock();
        }
    }

    public void ping() {

        if (getSelectionKey() == null || writeBuffer == null) {
            setConnected(false);
            return;
        }
        if (!getSelectionKey().isValid()) {
            System.out.println("selectionkey invalid11!");
            //disconnect();
            setConnected(false);
            return;
        }

        lastPinged = System.currentTimeMillis();

        if (writeBufferLock.tryLock()) {
            if (writeBuffer.capacity() > 0) {
                writeBuffer.put((byte) 100);
            } else {
                System.out.println("Konnte nicht pingen...");
            }
            writeBufferLock.unlock();
        }

        setWriteBufferFilled();

    }

    public SelectionKey getSelectionKey() {
        return selectionKey;
    }

    public void setSelectionKey(SelectionKey selectionKey) {
        this.selectionKey = selectionKey;
    }

    public boolean setWriteBufferFilled() {

        if (!isConnected()) {
            //System.out.println("::::");
            //throw new RuntimeException("dafuq !!!!!!!!!");
            return false;
        }

        //System.out.println("Bytes: " + writeBuffer.position());
        boolean remainingBytes;

        if (writeBuffer == null) {
            return false;
        }

////        ByteBuffer localWriteBuffer = writeBuffer;
////
////        writeBufferLock.lock();
////
////        int writtenBytes = 0;
////        localWriteBuffer.flip(); //switch buffer for reading
////        try {
////            writtenBytes = writeBytesToPeer(localWriteBuffer);
////        } catch (IOException ex) {
////            ex.printStackTrace();
////
////            setConnected(false);
////            System.out.println("error writing bytes to peer.");
////            try {
////                socketChannel.close();
////            } catch (IOException ex1) {
////            }
////
////        }
////        Test.outBytes += writtenBytes;
////
////        if (writeBufferCrypted == null) {
////            remainingBytes = localWriteBuffer.hasRemaining();
////        } else {
////            writeBufferCrypted.flip();
////            remainingBytes = writeBufferCrypted.hasRemaining();
////            writeBufferCrypted.compact();
////        }
////
////        localWriteBuffer.compact();
////
////        writeBufferLock.unlock();
////
////        if (!remainingBytes) {
////            return true;
////        }

//        if (writeBuffer.remaining() < 1024 * 1024) {
//            ByteBuffer allocate = ByteBuffer.allocate(writeBuffer.capacity() * 2);
//            allocate.put(writeBuffer.array());
//            allocate.position(writeBuffer.position());
//            writeBuffer = allocate;
////            System.out.println("writeBuffer voll, wurde verdoppelt...");
//        }
//        System.out.println("Writing stucked...");
        try {
            getSelectionKey().selector().wakeup();
            getSelectionKey().interestOps(getSelectionKey().interestOps() | SelectionKey.OP_WRITE);
            getSelectionKey().selector().wakeup();
             //System.out.println("added op_write...");
        } catch (CancelledKeyException e) {
            //disconnect();
            System.out.println("cancelled key exception");
        }

        return false;
    }

    int writeBytesToPeer(ByteBuffer writeBuffer) throws IOException {
        int writtenBytes;

        if (writeBufferCrypted == null) {
            writtenBytes = getSocketChannel().write(writeBuffer);
        } else {
            //TODO groesse anpassen vom crypted buffer
            byte[] buffer = new byte[writeBuffer.remaining()];
            writeBuffer.get(buffer);
            byte[] encryptedBytes = writeKey.encrypt(buffer);

            if (writeBufferCrypted.remaining() < encryptedBytes.length) {
                //buffer zu klein :(
                ByteBuffer newBuffer = ByteBuffer.allocate(writeBufferCrypted.capacity() + encryptedBytes.length);
                newBuffer.put(writeBufferCrypted);
                writeBufferCrypted = newBuffer;
            }

            writeBufferCrypted.put(encryptedBytes);
            writeBufferCrypted.flip();
            writtenBytes = getSocketChannel().write(writeBufferCrypted);
            writeBufferCrypted.compact();

            //System.out.println("crypted bytes: " + Utils.bytesToHexString(buffer) + " to " + Utils.bytesToHexString(encryptedBytes));
        }

        return writtenBytes;
    }

    /**
     * NOTICE: does not flush the writeStream
     *
     * @param m
     */
    public synchronized void writeMessage(RawMsg m) {
        //may be setted to null during work...
        ByteBuffer localWriteBuffer = writeBuffer;

        if (localWriteBuffer == null || readBuffer == null) {
            System.out.println("couldnt send msg, no buffers...");
            return;
        }

        if (m.database_Id == -1 || m.key.database_id == -1) {
            //Main.sendBroadCastMsg("HOLY SHIT  - nudm3284mz28423n4znc75z34c578n3485zc3857zc8345");
            try {
                throw new RuntimeException("HOLY SHIT  - nudm3284mz28423n4znc75z34c578n3485zc3857zc8345 " + m.database_Id + " " + m.key.database_id);
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
            return;
        }

//        if (peerTrustData.sendMessages.contains(m.database_Id)) {
//            return;
//        }
//
//        if (peerTrustData.isFilteringAddresses() && !peerTrustData.isPermittedAddress(m.key.database_id)) {
//            //System.out.println("peer doesnt want messages for this channel: " + m.key.getPubKey());
//            return;
//        }
        //peerTrustData.sendMessages.add(m.database_Id);
        //Test.messageStore.addMsgIntroducedToHim(peerTrustData.internalId, m.database_Id);
        //System.out.println("added to db: " + peerTrustData.internalId + " - " + m.database_Id);
        ECKey k = m.key;
        if (!peerTrustData.keyToIdMine.contains(k.database_id)) {

//            if (k.getPubKey().length != 33) {
//                System.out.println("Dbdzudgn268rtgx6345g345m: " + " len: " + k.getPubKey().length);
//                byte[] b = new byte[33];
//                int i = 0;
//                for (byte bb : k.getPubKey()) {
//                    b[i] = bb;
//                    i++;
//                }
//
//                k = new ECKey(null, b);
//
//                System.out.println("Dbdzudgn268rtgx6345g345m: " + " len: " + k.getPubKey().length);
//            }
            writeBufferLock.lock();
            if (writeBuffer == null) {
                writeBufferLock.unlock();
                return;
            }
            peerTrustData.keyToIdMine.add(k.database_id);
            //int indexOf = keyToIdMine.indexOf(k);
            int indexOf = m.key.database_id;
            localWriteBuffer.put((byte) 4);
            localWriteBuffer.put(k.getPubKey());
            localWriteBuffer.putInt(indexOf);
            //System.out.println("Msg index: " + indexOf);
            writeBufferLock.unlock();

        }

        if (m.public_type == -1) {
            throw new RuntimeException("omg!");
        }

        writeBufferLock.lock();
        if (writeBuffer == null) {
            writeBufferLock.unlock();
            return;
        }
        //int indexOfKey = keyToIdMine.indexOf(k);
        int indexOfKey = k.database_id;

        writeBuffer.put((byte) 5);
        writeBuffer.putInt(indexOfKey);
        writeBuffer.put(m.public_type);
        writeBuffer.putLong(m.timestamp);
        writeBuffer.putInt(m.nonce);
        writeBuffer.putInt(m.database_Id);//TODO long zu int machen mit offset falls db zu gross!!
        writeBufferLock.unlock();
        //should be run later manually...
////        boolean sureWrittenToPeer = setWriteBufferFilled();
////
////        //System.out.println("wrote msg to peer: " + ip + " " + m.database_Id);
////        if (!sureWrittenToPeer) {
////            //TODO
////        }

    }

    public void sendChannelToFilter(ECKey k) {
        int pubkeyId = Test.messageStore.getPubkeyId(k);
        k.database_id = pubkeyId;

        if (writeBuffer == null || readBuffer == null) {
            System.out.println("couldnt send msg, no buffers...");
            return;
        }

        if (k.database_id == -1) {
            //Main.sendBroadCastMsg("HOLY SHIT  - nudm3284mz28423n4znc75z34c578n3485zc3857zc8345");
            try {
                throw new RuntimeException("HOLY SHIT  - xXnudm3284mz28423n4znc75z34c578n3485zc3857zc8345  " + k.database_id);
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
            return;
        }

        if (!peerTrustData.keyToIdMine.contains(k.database_id)) {

//            if (k.getPubKey().length != 33) {
//                System.out.println("Dbdzudgn268rtgx6345g345m: " + " len: " + k.getPubKey().length);
//                byte[] b = new byte[33];
//                int i = 0;
//                for (byte bb : k.getPubKey()) {
//                    b[i] = bb;
//                    i++;
//                }
//
//                k = new ECKey(null, b);
//
//                System.out.println("Dbdzudgn268rtgx6345g345m: " + " len: " + k.getPubKey().length);
//            }
            writeBufferLock.lock();
            peerTrustData.keyToIdMine.add(k.database_id);
            //int indexOf = keyToIdMine.indexOf(k);
            int indexOf = k.database_id;
            writeBuffer.put((byte) 4);
            writeBuffer.put(k.getPubKey());
            writeBuffer.putInt(indexOf);
            //System.out.println("Msg index: " + indexOf);
            writeBufferLock.unlock();

        }

        writeBufferLock.lock();
        int indexOf = k.database_id;
        writeBuffer.put((byte) 60);
        writeBuffer.putInt(indexOf);
        writeBufferLock.unlock();

    }

    public void migratePeer(Peer otherPeer) {
//        peerTrustData.loadedMsgs = peerTrustData.loadedMsgs;
        requestedMsgs = otherPeer.requestedMsgs;
//        peerTrustData.sendMessages = peerTrustData.sendMessages;
//        peerTrustData.pendingMessages = peerTrustData.pendingMessages;
        //keyToIdHis = otherPeer.keyToIdHis;
        //keyToIdMine = otherPeer.keyToIdMine;
//        peerTrustData.synchronizedMessages = peerTrustData.synchronizedMessages;
//        peerTrustData.lastSuccessfulySendMessageHeader = peerTrustData.lastSuccessfulySendMessageHeader;
    }

    public boolean peerIsHigher() {
        return Test.NONCE > nonce;
    }

    boolean isFullConnected() {
        //System.out.println("hmm : " + (writeBufferCrypted != null));
        return (writeBufferCrypted != null && readBufferCrypted != null);
        //return (writeBufferCrypted != null);
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public boolean isAuthed() {
        return authed;
    }

    public boolean isCryptedConnection() {
        return readBufferCrypted != null;
    }

    public PeerTrustData getPeerTrustData() {
        return peerTrustData;
    }

    public HashMap<Integer, ECKey> getKeyToIdHis() {
        return getPeerTrustData().keyToIdHis;
    }

    public ArrayList<Integer> getKeyToIdMine() {
        return getPeerTrustData().keyToIdMine;
    }

    public ECKey getId2KeyHis(int cnt) {
        return getPeerTrustData().id2KeyHis(cnt);
    }

    void addPendingMessage(int messageId, RawMsg rawMsg) {
        getPeerTrustData().addPendingMessage(messageId, rawMsg);
    }

    public HashMap<Integer, RawMsg> getPendingMessages() {
        return getPeerTrustData().getPendingMessages();
    }

    public HashMap<Integer, RawMsg> getPendingMessagesTimedOut() {
        return getPeerTrustData().getPendingMessagesTimedOut();
    }

    public HashMap<Integer, RawMsg> getPendingMessagesPublic() {
        return getPeerTrustData().getPendingMessagesPublic();
    }

    public ArrayList<Integer> getLoadedMsgs() {
        return getPeerTrustData().loadedMsgs;
    }
}
