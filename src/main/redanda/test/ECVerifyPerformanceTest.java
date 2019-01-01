/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.redanda.test;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import main.redanda.core.Channel;
import main.redanda.core.LocalSettings;
import main.redanda.core.Test;
import main.redanda.core.messages.TextMsg;
import main.redanda.crypt.ECKey;

/**
 *
 * @author robin
 */
public class ECVerifyPerformanceTest {

    public static void main(String[] args) {
        
        double[] v = new double[15];
        
        for (int i = 1; i < 15; i++) {
            v[i] = test(i);
            System.out.println(i + " per second: " + v[i] + " plus of: " + (v[i] - v[i-1]));
        }

    }

    public static double test(int ths) {
        Test.localSettings = new LocalSettings();

        ECKey ecKey = new ECKey();

        final TextMsg build = TextMsg.build(new Channel(new ECKey(), "name"), "dhuagfeszufdhuagfeszufdhuagfeszufdhuagfen42134szufdhuagfeszufdhuagfeszufdhuagfeszufdhuagfeszuf");

        long t = System.currentTimeMillis();
        final int anz = 5000;

        ArrayList<Thread> arrayList = new ArrayList<Thread>();

        for (int l = 0; l < ths; l++) {
            Thread thread = new Thread() {
                @Override
                public void run() {
                    for (int i = 1; i < anz; i++) {
                        build.verify();
                    }
                }

            };
            arrayList.add(thread);
            thread.start();
        }

        for (Thread t2 : arrayList) {
            try {
                t2.join();
            } catch (InterruptedException ex) {
                Logger.getLogger(ECVerifyPerformanceTest.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        double a = (1000. * (anz * ths) / (System.currentTimeMillis() - t));

        
        return a;
    }

}
