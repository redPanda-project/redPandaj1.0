/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package main.redanda.services;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.Date;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import main.redanda.Main;
import main.redanda.core.Log;
import main.redanda.core.Peer;
import main.redanda.core.Test;

/**
 *
 * @author robin
 */
public class LoadHistory {

    private static Thread worker = null;
    private static boolean run = false;
    private static HashMap<Peer, Long> lastRequesteFrom = new HashMap<Peer, Long>();

    public static void sw() {

        if (worker == null) {

            run = true;

            worker = new Thread() {

                @Override
                public void run() {

                    ThreadMXBean tmb = ManagementFactory.getThreadMXBean();

                    long time = new Date().getTime() * 1000000;
                    long cput = 0;
                    double cpuperc = -1;

                    while (run && !Main.shutdown) {

                        if (tmb.isThreadCpuTimeSupported()) {
//                            if (new Date().getTime() * 1000000 - time > 1000000000) { //Reset once per second
                            time = new Date().getTime() * 1000000;
                            //cput = tmb.getCurrentThreadCpuTime();
                            cput = getTotalCpuTime(tmb);
//                            }

                            try {
                                sleep(1000);
                            } catch (InterruptedException ex) {
                                Logger.getLogger(LoadHistory.class.getName()).log(Level.SEVERE, null, ex);
                            }

                            if (!tmb.isThreadCpuTimeEnabled()) {
                                tmb.setThreadCpuTimeEnabled(true);
                            }

                            if (new Date().getTime() * 1000000 - time != 0) {
                                //cpuperc = (tmb.getCurrentThreadCpuTime() - cput) / (new Date().getTime() * 1000000.0 - time) * 100.0;
                                cpuperc = (getTotalCpuTime(tmb) - cput) / (new Date().getTime() * 1000000.0 - time) * 100.0;
                            }
                        } else {
                            try {
                                sleep(1000);
                            } catch (InterruptedException ex) {
                                Logger.getLogger(LoadHistory.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }

                        if (cpuperc > 200.0) {
                            System.out.println("too much load... " + cpuperc);
                            continue;
                        }

                        //System.out.println("go back in time...");
                        System.out.print("b");

                        for (Peer p : Test.getClonedPeerList()) {
                            if (p.isAuthed() && p.isConnected() && p.syncMessagesSince == 0) {

                                long backSyncedTill = p.getPeerTrustData().backSyncedTill;

                                //System.out.println("peer: " + p.getIp());
                                if (lastRequesteFrom.get(p) != null && lastRequesteFrom.get(p) == backSyncedTill) {
                                    //System.out.println("last request not done or finished?");
                                    continue;
                                }

                                lastRequesteFrom.remove(p);

                                p.writeBufferLock.lock();
                                p.writeBuffer.put((byte) 70);
                                p.writeBuffer.putLong(backSyncedTill);
                                p.writeBufferLock.unlock();
                                p.setWriteBufferFilled();

                                lastRequesteFrom.put(p, backSyncedTill);

                                Log.put("requested new sync back: " + p.nodeId + " time: " + backSyncedTill, 0);

                            } else {
                                lastRequesteFrom.remove(p);
                            }
                        }

                    }

                }
            };
            worker.start();

        } else {

            run = false;
            worker.interrupt();
            worker = null;

        }

    }

    public static long getTotalCpuTime(ThreadMXBean tmb) {
        long[] allThreadIds = tmb.getAllThreadIds();
        //System.out.println("Total JVM Thread count: " + allThreadIds.length);
        long nano = 0;
        for (long id : allThreadIds) {
            nano += tmb.getThreadCpuTime(id);
        }
        return nano;
    }
}
