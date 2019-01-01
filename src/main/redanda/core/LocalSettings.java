/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package main.redanda.core;

import kademlia.node.KademliaId;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Random;

/**
 * @author robin
 */
public class LocalSettings implements Serializable {

    public KademliaId nonce;
    public int channelIdCounter;
    public boolean PEX_ONLY;
    public long identity;
    public String myIp;
    public long lastSendAllMyChannels;
    public HashMap<Long, String> identity2Name;
    public byte[] updateSignature;

    public LocalSettings() {
        nonce = new KademliaId();
        channelIdCounter = 1;
        PEX_ONLY = false;
        identity = new Random().nextLong();
        myIp = "";
        identity2Name = new HashMap<Long, String>();
    }

    public void setUpdateSignature(byte[] updateSignature) {
        this.updateSignature = updateSignature;
    }

    public byte[] getUpdateSignature() {
        return updateSignature;
    }

    public void save() {
        Test.saver.saveLocalSettings(this);
    }
}
