/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.redanda.test;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.Date;

/**
 *
 * @author rflohr
 */
public class cputhrotteling {

    public static void main(String[] args) throws InterruptedException {

        ThreadMXBean tmb = ManagementFactory.getThreadMXBean();
        long time = new Date().getTime() * 1000000;
        long cput = 0;
        double cpuperc = -1;

        while (true) {

            if (tmb.isThreadCpuTimeSupported()) {
                if (new Date().getTime() * 1000000 - time > 1000000000) { //Reset once per second
                    time = new Date().getTime() * 1000000;
                    //cput = tmb.getCurrentThreadCpuTime();
                    cput = getTotalCpuTime(tmb);
                }

                if (!tmb.isThreadCpuTimeEnabled()) {
                    tmb.setThreadCpuTimeEnabled(true);
                }

                if (new Date().getTime() * 1000000 - time != 0) {
                    //cpuperc = (tmb.getCurrentThreadCpuTime() - cput) / (new Date().getTime() * 1000000.0 - time) * 100.0;
                    cpuperc = (getTotalCpuTime(tmb)- cput) / (new Date().getTime() * 1000000.0 - time) * 100.0;
                }
            }
            //System.out.println("checking...");

//If cpu usage is greater then 50%
            if (cpuperc > 25.0) {
                //sleep for a little bit.
                //System.out.println("sleeping");
                Thread.sleep(50);
                //System.out.println("sleeeped");
                continue;
            }
            long a = 0;

            long b = 1;
            for (int i = Integer.MIN_VALUE; i != Integer.MAX_VALUE; i++) {
                b++;
            }

        }

    }

    private static long getTotalCpuTime(ThreadMXBean tmb) {
        long[] allThreadIds = tmb.getAllThreadIds();
        //System.out.println("Total JVM Thread count: " + allThreadIds.length);
        long nano = 0;
        for (long id : allThreadIds) {
            nano += tmb.getThreadCpuTime(id);
        }
        return nano;
    }
}
