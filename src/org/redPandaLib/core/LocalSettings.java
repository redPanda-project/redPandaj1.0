/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redPandaLib.core;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Random;

/**
 *
 * @author robin
 */
public class LocalSettings implements Serializable {

    public long nonce;
    public int channelIdCounter;
    public boolean PEX_ONLY;
    public long identity;
    public String myIp;
    public long lastSendAllMyChannels;
    public HashMap<Long, String> identity2Name;

    public LocalSettings() {
        nonce = new Random().nextLong();
        channelIdCounter = 1;
        PEX_ONLY = false;
        identity = new Random().nextLong();
        myIp = "";
        identity2Name = new HashMap<Long, String>();
    }

    public void save() {
        Test.saver.saveLocalSettings(this);
    }
}
