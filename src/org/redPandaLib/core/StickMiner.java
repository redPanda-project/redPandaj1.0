/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redPandaLib.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import org.redPandaLib.SpecialChannels;
import org.redPandaLib.core.messages.RawMsg;
import org.redPandaLib.core.messages.StickMsg;

/**
 *
 * @author rflohr
 */
public class StickMiner {

    public static int sticks = 0;
    public static final Boolean syncroniser = new Boolean(false);

    public static void main(String[] args) throws Exception {
        start();
    }

    public static void start() {

        for (int i = 0; i < 1; i++) {

            final int finalI = i;

            new Thread() {

                @Override
                public void run() {

                                final String orgName = Thread.currentThread().getName();
                                     if (!orgName.contains(" ")) {
            Thread.currentThread().setName(orgName + " - stickminer");
                                     }
                    
                    //setPriority(MIN_PRIORITY);

                    System.out.println("started...");



                    Stick lastStick = null;

                    while (true) {
                        ArrayList<Channel> clone = (ArrayList<Channel>) Test.channels.clone();
                        Collections.sort(clone);

                        Channel channel = clone.get(0);

                        Stick generate = Stick.generate(channel.getKey().getPubKey(), finalI * 1000000000);



                        if (generate.equals(lastStick)) {
                            System.out.println("gleich!");
                        } else {
                            synchronized (syncroniser) {
                                sticks++;
                                System.out.println("found sticks: " + sticks + " overallDifficulty: " + channel.diffuculty);
                                StickMsg stickMsg = new StickMsg(generate.pubkey, generate.timestamp, generate.nonce);
                                stickMsg.verified = true;
                                RawMsg addMessage = MessageHolder.addMessage(stickMsg);
                                Test.broadcastMsg(addMessage);
                                channel.diffuculty += generate.getDifficulty();

                            }
                        }



                    }


                }
            }.start();
        }
    }
}
