/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.redanda.test;

import main.redanda.core.Channel;
import main.redanda.crypt.ECKey;

/**
 *
 * @author rflohr
 */
public class CreateNewChannel {

    public static void main(String[] args) {

        Channel channel = new Channel(new ECKey(), "temp");
        System.out.println("new key to import: " + channel.exportForHumans());
        System.out.println("priv key: " + channel.getPrivateKey());

    }
}
