/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redPandaLib;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.redPandaLib.core.Channel;
import org.redPandaLib.crypt.AddressFormatException;
import org.redPandaLib.crypt.Base58;
import org.redPandaLib.crypt.ECKey;
import org.redPandaLib.crypt.RSAKey;

/**
 * @author robin
 */
public class SpecialChannels {

    public static Channel MAIN;
    public static Channel SPAM;

    static {
        MAIN = Channel.getInstaceByPrivateKey("GvdCjbWe6s8vCNbiDyAKVSHmMngsjMett7o9TMB8ptJq", "Main Channel", -2);
        SPAM = Channel.getInstaceByPrivateKey("2c2s4jDb1ofJpd1JArVzxfWNYPtMb9aX7W8173X3sbs5", "Spam Channel", -3);
    }

    public static Channel isSpecial(Channel channel) {
        if (channel.equals(SpecialChannels.MAIN)) {
            return MAIN;
        }

        return null;
        //        if (instance.getChannel().equals(SpecialChannels.MAIN)) {
        //            instance.setChannel(SpecialChannels.MAIN);
        //        }

    }

    public static Channel isSpecial(byte[] pubkey) {
        if (Arrays.equals(pubkey, SpecialChannels.MAIN.getKey().getPubKey())) {
            return MAIN;
        } else if (Arrays.equals(pubkey, SpecialChannels.SPAM.getKey().getPubKey())) {
            return MAIN;
        }

        return null;
        //        if (instance.getChannel().equals(SpecialChannels.MAIN)) {
        //            instance.setChannel(SpecialChannels.MAIN);
        //        }

    }

    //Channel id = -4
    public static Channel getAnnouncementChannel() {
        try {
            byte[] publicKey = Base58.decode("hhrdhDyBiqfDUPsgjQ571ZcG5FdLK9MNWE8pmY2C7DT8");
            ECKey ecKey = new ECKey(null, publicKey);
            Channel channel = new Channel(ecKey, "Announcements");
            channel.setExtraEncryptionKey(Base58.decode("GWW85eVRSQVtpxuEJi2L9HGfzRjNZ9ug2ubp7A75Q2vC"));
            channel.setId(-4);
            channel.setPublic(true);
            return channel;
        } catch (AddressFormatException ex) {
            Logger.getLogger(SpecialChannels.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }

    //Channel id = -5
    public static Channel getUpdateChannel() {
        try {
            byte[] publicKey = Base58.decode("evkUMf9Zr6LgCeJgxH2DYGT37GY8VaCHP3vhh3wRHGYS");
            ECKey ecKey = new ECKey(null, publicKey);
            Channel channel = new Channel(ecKey, "Updates");
            channel.setId(-5);
            channel.setPublic(true);
            return channel;
        } catch (AddressFormatException ex) {
            Logger.getLogger(SpecialChannels.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }

    public static Channel getUpdateChannel(String privateKeyString) {

        try {
            byte[] publicKey = Base58.decode("evkUMf9Zr6LgCeJgxH2DYGT37GY8VaCHP3vhh3wRHGYS");
            ECKey ecKey = new ECKey(Base58.decode(privateKeyString), publicKey);
            Channel channel = new Channel(ecKey, "Updates");
            channel.setId(-5);
            channel.setPublic(true);
            return channel;
        } catch (AddressFormatException ex) {
            Logger.getLogger(SpecialChannels.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }
}
