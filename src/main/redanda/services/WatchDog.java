/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.redanda.services;

import java.util.logging.Level;
import java.util.logging.Logger;
import main.redanda.Main;
import main.redanda.core.ConnectionHandler;
import main.redanda.core.Test;

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
                        if ((!lastLine.contains("ConnectionHandler.java:174") && !lastLine.contains("ConnectionHandler.java:221")) || (Test.peerListLock.isLocked())) {

                            String lockString = "";

                            if (Test.peerListLock.isLocked()) {
                                Thread ownerExtended = Test.peerListLock.getOwnerExtended();

                                if (ownerExtended == null) {
                                    Main.sendBroadCastMsg("peerlist is locked but getowner was null");
                                } else {
                                    lockString += "\n\n\npeerlist lockString: ";
                                    lockString += ownerExtended.getName() + "\nstacktrace of thread: ";

                                    lockString += Test.peerListLock.getLastSuccesfullLockThreadStack();
                                }

                            }

                            Main.sendBroadCastMsg("Wuff! Wuff! ConnectionHandler didn't run for quite a while (" + Test.peerListLock.isLocked() + "): " + delay + "\nState: " + Test.connectionHandler.getState() + "\nStackTrace: " + ownStackTrace + lockString);
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
