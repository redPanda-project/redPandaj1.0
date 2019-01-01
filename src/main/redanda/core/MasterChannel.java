/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package main.redanda.core;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import main.redanda.core.messages.ControlMsg;
import main.redanda.core.messages.RawMsg;

/**
 *
 * @author robin
 */
public class MasterChannel {

    public static void pushAllChannels() {
        try {
            for (Channel c : Test.channels) {
                pushChannel(c);
            }
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(MasterChannel.class.getName()).log(Level.SEVERE, null, ex);

        }
    }

    public static void pushChannel(Channel channel) throws UnsupportedEncodingException {

        Channel masterChannel = getMasterChannel();

        byte[] chanName = channel.name.getBytes("UTF-8");

        //addcmd: (int) 1 - public bytes - (int) name laenge - name
        byte[] array = new byte[4 + 33 + 4 + chanName.length];
        ByteBuffer command = ByteBuffer.wrap(array);
        command.putInt(1);
        command.put(channel.key.getPubKey());
        command.putInt(chanName.length);
        command.put(chanName);
        ControlMsg build = ControlMsg.build(masterChannel, System.currentTimeMillis(), 789, array);
        RawMsg addMessage = MessageHolder.addMessage(build);
        Test.broadcastMsg(addMessage);

    }

    public static void pushIdentity() throws UnsupportedEncodingException {

        Channel masterChannel = getMasterChannel();


        //addcmd: (int) 2 - 8 bytes der identitaet...
        byte[] array = new byte[4 + 8];
        ByteBuffer command = ByteBuffer.wrap(array);
        command.putInt(2);
        command.putLong(Test.localSettings.identity);
        ControlMsg build = ControlMsg.build(masterChannel, System.currentTimeMillis(), 789, array);
        RawMsg addMessage = MessageHolder.addMessage(build);
        Test.broadcastMsg(addMessage);

    }

    private static Channel getMasterChannel() {
        return Test.channels.get(0);
    }
}
