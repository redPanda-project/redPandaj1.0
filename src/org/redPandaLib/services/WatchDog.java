/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redPandaLib.services;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.redPandaLib.Main;
import org.redPandaLib.core.ConnectionHandler;
import org.redPandaLib.core.Test;

/**
 *
 * @author rflohr
 */
public class WatchDog {

    private static Thread thread;

    public static void start() {

        thread = new Thread() {

            @Override
            public void run() {

                while (true) {

                    try {
                        sleep(1000 * 60);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(WatchDog.class.getName()).log(Level.SEVERE, null, ex);
                    }

                    long delay = System.currentTimeMillis() - ConnectionHandler.lastRun;

                    if (delay > 1000 * 60 * 1) {

                        StackTraceElement[] stackTrace = Test.connectionHandler.getStackTrace();
                        String ownStackTrace = "";
                        for (StackTraceElement a : stackTrace) {
                            ownStackTrace += a.toString() + "\n";
                        }
                        String lastLine = stackTrace[stackTrace.length - 1].toString();
                        if (!lastLine.contains("ConnectionHandler.java:168") && !lastLine.contains("ConnectionHandler.java:215")) {
                            Main.sendBroadCastMsg("Wuff! Wuff! ConnectionHandler didn't run for quite a while: " + delay + "\nState: " + Test.connectionHandler.getState() + "\nStackTrace: " + ownStackTrace);
                            return;
                        } else {
                            //Main.sendBroadCastMsg("No Wuff!");
                        }

                    }

                }

            }

        };

        thread.start();

    }

}
