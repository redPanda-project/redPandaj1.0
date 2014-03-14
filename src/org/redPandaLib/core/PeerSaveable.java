/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redPandaLib.core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import org.redPandaLib.core.messages.RawMsg;

/**
 *
 * @author robin
 */
public class PeerSaveable implements Serializable {

    String ip;
    int port;
    long lastAllMsgsQuerried;
    long nonce;
    long retries;
    //ArrayList<Integer> loadedMsgs;
    //ArrayList<Integer> sendMessages;
    //HashMap<Integer, RawMsg> pendingMessages;
    //HashMap<Integer, RawMsg> pendingMessagesPublic;
    int haus;
    //int synchronizedMessages;
//    int lastSuccessfulySendMessageHeader;

    public PeerSaveable(String ip, int port, long lastAllMsgsQuerried, long nonce, long retries) {
        this.ip = ip;
        this.port = port;
        this.lastAllMsgsQuerried = lastAllMsgsQuerried;
        this.nonce = nonce;
        this.retries = retries;
        //this.loadedMsgs = loadedMsgs;
        //this.sendMessages = sendMessages;
        //this.pendingMessages = pendingMessages;
        //this.pendingMessagesPublic = pendingMessagesPublic;
        //this.synchronizedMessages = synchronizedMessages;
//        this.lastSuccessfulySendMessageHeader = lastSuccessfulySendMessageHeader;
    }

    public Peer toPeer() {

        Peer out = new Peer(ip, port);
        out.lastAllMsgsQuerried = lastAllMsgsQuerried;
        out.nonce = nonce;
        out.retries = retries;
//        out.sendMessages = sendMessages;
//        out.pendingMessages = pendingMessages;
//        out.pendingMessagesPublic = pendingMessagesPublic;
//        out.loadedMsgs = loadedMsgs;
//        out.synchronizedMessages = synchronizedMessages;
//        out.lastSuccessfulySendMessageHeader = lastSuccessfulySendMessageHeader;

        return out;
    }
}
