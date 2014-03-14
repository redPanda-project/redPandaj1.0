/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redPandaLib.core;

import java.util.ArrayList;

/**
 *
 * @author robin
 */
public interface SaverInterface {

    public void savePeerss(ArrayList<Peer> peers);

    public ArrayList<Peer> loadPeers();

    public void saveIdentities(ArrayList<Channel> identities);

    public ArrayList<Channel> loadIdentities();

    public void saveLocalSettings(LocalSettings localSettings);

    public LocalSettings loadLocalSettings();
    
    public void saveTrustedPeers(ArrayList<PeerTrustData> peertrusts);
    
    public ArrayList<PeerTrustData> loadTrustedPeers();
}
