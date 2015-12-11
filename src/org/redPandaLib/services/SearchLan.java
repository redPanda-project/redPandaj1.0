/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redPandaLib.services;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.redPandaLib.core.Peer;
import org.redPandaLib.core.Test;

/**
 *
 * @author Tyrael
 */
public class SearchLan {

    public static void searchLan() {
        new Thread() {

            @Override
            public void run() {
                try {
                    InetAddress localAddress = InetAddress.getLocalHost();
                    String subnet = getSubnet(localAddress);
                    System.out.println(subnet);
                    for (int i = 1; i <= 255; i++) {
                        String host = subnet + i;

                        Peer peer = new Peer(host, 59558);
                        //peer.retries = 10;
                        Test.findPeer(peer);
                    }

                } catch (UnknownHostException ex) {
                    Logger.getLogger(SearchLan.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }.start();


    }

    public static String getSubnet(InetAddress address) {
        byte[] a = address.getAddress();
        String subnet = "";
        for (int i = 0; i < a.length - 1; i++) {
            if (a[i] != 0) {
                subnet += (256 + a[i]) + ".";
            } else {
                subnet += 0 + ".";
            }
        }
        return subnet;
    }
}
