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
import org.redPandaLib.core.messages.RawMsg;
import org.redPandaLib.crypt.ECKey;
import org.redPandaLib.services.MessageDownloader;

/**
 *
 * @author rflohr
 */
public class PeerTrustData implements Serializable {

    long nonce;
    byte[] authKey;
    int trustLevel;
    long lastSeen = 0;
    ArrayList<String> ips;
    int port;
    public HashMap<Integer, ECKey> keyToIdHis = new HashMap<Integer, ECKey>();
    public ArrayList<Integer> sendMessages = new ArrayList<Integer>();
    public HashMap<Integer, RawMsg> pendingMessagesPublic = new HashMap<Integer, RawMsg>();
    public ArrayList<Integer> loadedMsgs = new ArrayList<Integer>();
    public ArrayList<Integer> keyToIdMine = new ArrayList<Integer>();
    public HashMap<Integer, RawMsg> pendingMessages = new HashMap<Integer, RawMsg>();
    public int synchronizedMessages = 0;
    public int lastSuccessfulySendMessageHeader = 0;

    public PeerTrustData() {
        authKey = new byte[32];
        ips = new ArrayList<String>();
    }

    public void removeNotSuccesfulSendMessages() {
        int lastIndexOf = sendMessages.lastIndexOf(lastSuccessfulySendMessageHeader);
        for (int i = lastIndexOf + 1; i < sendMessages.size(); i++) {
            sendMessages.remove(i);
        }
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
}
