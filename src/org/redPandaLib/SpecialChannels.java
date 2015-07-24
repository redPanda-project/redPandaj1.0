/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redPandaLib;

import java.util.Arrays;
import org.redPandaLib.core.Channel;

/**
 *
 * @author robin
 */
public class SpecialChannels {

    public static Channel MAIN;
    public static Channel SPAM;

    static {
        MAIN = Channel.getInstaceByPrivateKey("7M8CsPkboJiVEe9P7gh6SDHqjemqnjPy3JyZur9duBwG", "Main Channel", -2);
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
}
