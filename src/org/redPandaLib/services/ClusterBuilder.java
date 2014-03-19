/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redPandaLib.services;

import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.redPandaLib.ByteUtils;
import org.redPandaLib.Main;
import org.redPandaLib.NewMessageListener;
import org.redPandaLib.SpecialChannels;
import org.redPandaLib.core.Channel;
import org.redPandaLib.core.ConnectionHandler;
import org.redPandaLib.core.MessageHolder;
import org.redPandaLib.core.Test;
import org.redPandaLib.core.messages.ControlMsg;
import org.redPandaLib.core.messages.RawMsg;
import org.redPandaLib.core.messages.TextMessageContent;
import org.redPandaLib.core.messages.TextMsg;

/**
 *
 * @author rflohr
 */
public class ClusterBuilder {

    static WorkingThread workingThread;

    public static void start() {
        workingThread = new WorkingThread();
        //workingThread.start();//TODO: turn on!
    }

    static class WorkingThread extends Thread {

        @Override
        public void run() {

            final String orgName = Thread.currentThread().getName();
            Thread.currentThread().setName(orgName + " - ClusterBuilder");

            if (System.currentTimeMillis() - Test.localSettings.lastSendAllMyChannels > 1000 * 60 * 60 * 24 * 7) {

                Test.localSettings.lastSendAllMyChannels = System.currentTimeMillis();

                //send all my ch<annels to channels xD
                for (Channel c1 : Main.getChannels()) {
                    for (Channel c2 : Main.getChannels()) {
                        if (SpecialChannels.isSpecial(c1) != null || SpecialChannels.isSpecial(c2) != null || c1.equals(c2)) {
                            continue;
                        }

                        byte[] pubkeybytes = c2.getKey().getPubKey();

                        byte[] content = new byte[4 + 33];
                        ByteBuffer wrap = ByteBuffer.wrap(content);
                        wrap.putInt(2);
                        wrap.put(pubkeybytes);

                        ControlMsg build = ControlMsg.build(c1, System.currentTimeMillis(), 87478, content);
                        RawMsg addMessage = MessageHolder.addMessage(build);
                        Test.broadcastMsg(addMessage);


                    }
                }
            }


            while (true) {
                try {



                    String myIp;

                    myIp = IpChecker.getIp();



                    if (Test.localSettings.myIp.equals(myIp)) {
                        //System.out.println("found my external ip: " + myIp + " - Ip didnt change... do nothing...");
                        try {
                            sleep(30 * 60 * 1000);
                        } catch (InterruptedException ex) {
                            Logger.getLogger(ClusterBuilder.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        continue;
                    }

                    System.out.println("found new external ip: " + myIp + " - sending to all channels");

                    Test.localSettings.myIp = myIp;
                    Test.localSettings.save();


                    for (Channel channel : Main.getChannels()) {


                        byte[] content = new byte[4 + 4 + 2];
                        ByteBuffer wrap = ByteBuffer.wrap(content);
                        wrap.putInt(1);//myIp command

                        String[] split = myIp.split("\\.");


                        if (split.length != 4) {
                            System.out.println("my ip ist wrong...");
                            break;
                        }

                        for (String a : split) {
                            int asdd = Integer.parseInt(a);
                            wrap.put((byte) asdd);
                        }

                        wrap.put(ByteUtils.intToUnsignedShortAsBytes(Test.getMyPort()));




                        ControlMsg build = ControlMsg.build(channel, System.currentTimeMillis(), 4789, content);
                        RawMsg addMessage = MessageHolder.addMessage(build);
                        Test.broadcastMsg(addMessage);
                        //Test.messageStore.addDecryptedContent(addMessage.getKey().database_id, (int) addMessage.database_Id, TextMsg.BYTE, ((TextMsg) addMessage).getText(), ((TextMsg) addMessage).getIdentity(), true);

//                        TextMessageContent textMessageContent = TextMessageContent.fromTextMsg((TextMsg) addMessage, true);
//                        for (NewMessageListener listener : Main.listeners) {
//                            listener.newMessage(textMessageContent);
//                        }
                    }
                } catch (Exception ex) {
                    Logger.getLogger(ClusterBuilder.class.getName()).log(Level.SEVERE, null, ex);
                }

                try {
                    sleep(60 * 1000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(ClusterBuilder.class.getName()).log(Level.SEVERE, null, ex);
                }

            }

        }
    }
}
