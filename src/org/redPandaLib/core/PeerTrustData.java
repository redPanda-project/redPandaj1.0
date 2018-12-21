/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redPandaLib.core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import kademlia.node.KademliaId;
import org.redPandaLib.core.messages.RawMsg;
import org.redPandaLib.crypt.ECKey;
import org.redPandaLib.crypt.Utils;
import org.redPandaLib.services.MessageDownloader;

/**
 * @author rflohr
 */
public class PeerTrustData implements Serializable {

    private static final long serialVersionUID = -2628506219264601849L;

    public int internalId = -1;
    KademliaId nonce;
    byte[] authKey;
    int trustLevel;
    public long lastSeen = System.currentTimeMillis();
    public ArrayList<String> ips;
    int port;
    public int loadedMsgsCount = 0; //not 100 percent accurate, because not thread safe
    public int receivedMsgsCount = 0;
    public HashMap<Integer, ECKey> keyToIdHis = new HashMap<Integer, ECKey>();
    public HashMap<Integer, RawMsg> pendingMessagesPublic = new HashMap<Integer, RawMsg>();
    public ArrayList<Integer> loadedMsgs = new ArrayList<Integer>();  //not 100 percent accurate, because not thread safe
    public ArrayList<Integer> keyToIdMine = new ArrayList<Integer>();
    public HashMap<Integer, RawMsg> pendingMessages = new HashMap<Integer, RawMsg>();
    public HashMap<Integer, RawMsg> pendingMessagesTimedOut = new HashMap<Integer, RawMsg>();
    public long rating = 10000;

    public HashMap<Integer, RawMsg> getPendingMessagesTimedOut() {
        return pendingMessagesTimedOut;
    }

    public int synchronizedMessages = 0;
    public int badMessages = 0;
    public ArrayList<Integer> sendChannelsToFilter = new ArrayList<Integer>();
    public long backSyncedTill = Long.MAX_VALUE;
    public int peerMode = -1; //supernode, lightclient, normal? -1 = unset

    public PeerTrustData() {
        authKey = new byte[32];
        ips = new ArrayList<String>();
    }

    //    public void removePendingMessage(RawMsg m ) {
    //        pendingMessages.;
    //    }
    public void addPendingMessage(int id, RawMsg m) {
        synchronized (pendingMessages) {
            pendingMessages.put(id, m);
        }
        MessageDownloader.trigger();
    }

    public HashMap<Integer, RawMsg> getPendingMessagesPublic() {
        return pendingMessagesPublic;
    }

    public ECKey id2KeyHis(int id) {
        return keyToIdHis.get(id);

    }

    public int key2IdHis(ECKey k) {
        Set<Map.Entry<Integer, ECKey>> entrySet = keyToIdHis.entrySet();
        for (Map.Entry<Integer, ECKey> entry : entrySet) {
            if (entry.getValue().equals(k)) {
                return entry.getKey();
            }
        }
        return -1;
    }

    //    public ECKey id2KeyMine(int id) {
    //        return keyToIdMine.get(id);
    //    }
    //
    //    public int key2IdMine(ECKey k) {
    //
    //        if (!keyToIdMine.contains(k)) {
    //            keyToIdMine.add(k);
    //        }
    //
    //        return keyToIdMine.indexOf(k);
    //    }
    public HashMap<Integer, RawMsg> getPendingMessages() {
        return pendingMessages;
    }

    @Override
    protected PeerTrustData clone() throws CloneNotSupportedException {

        PeerTrustData cloned = (PeerTrustData) super.clone();

        cloned.keyToIdHis = (HashMap<Integer, ECKey>) keyToIdHis.clone();

        cloned.pendingMessagesPublic = (HashMap<Integer, RawMsg>) pendingMessagesPublic.clone();

        cloned.loadedMsgs = (ArrayList<Integer>) loadedMsgs.clone();

        cloned.keyToIdMine = (ArrayList<Integer>) keyToIdMine.clone();

        cloned.pendingMessages = (HashMap<Integer, RawMsg>) pendingMessages.clone();

        cloned.sendChannelsToFilter = (ArrayList<Integer>) sendChannelsToFilter.clone();

        System.out.println("CLONED: " + Utils.bytesToHexString(cloned.authKey));

        return cloned;
    }

    public void initInternalId() {
        synchronized (Test.peerTrusts) {

            internalId = 0;
            while (true) {
                internalId++;

                boolean inUse = false;
                for (PeerTrustData ptd : Test.peerTrusts) {

                    if (ptd == this) {
                        continue;
                    }

                    if (ptd.internalId == internalId) {
                        inUse = true;
                        break;
                    }
                }

                if (!inUse) {
                    break;
                }

            }

            Test.messageStore.clearFilterChannel(internalId);
            Test.messageStore.removeMessageToSend(internalId);

            System.out.println("GOT ID: " + internalId);
        }
    }

    public int getMessageLoadedCount() {
        return loadedMsgsCount + loadedMsgs.size();
    }
}
