/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redPandaLib.upnp;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 *
 * @author sony
 */
public class Main {

    public static void main(String[] args) throws UnknownHostException {

        String localHost = InetAddress.getLocalHost().getHostName();
        System.out.println("ss " + localHost);
        for (InetAddress ia : InetAddress.getAllByName(localHost)) {
            System.out.println(ia.getHostAddress());
        }

//        Portforward.start(1234,"192.168.0.100");
        //Portforward.start(1234,"192.168.178.59");
//                Portforward.start(1234,localHost);

    }
}
