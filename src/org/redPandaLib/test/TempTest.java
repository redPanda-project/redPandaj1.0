/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redPandaLib.test;

import java.util.Collection;
import java.util.Map;

/**
 *
 * @author robin
 */
public class TempTest {

    public static void main(String[] args) {

        Map<Thread, StackTraceElement[]> allStackTraces = Thread.getAllStackTraces();

        System.out.println(allStackTraces.toString());

        Collection<StackTraceElement[]> values = allStackTraces.values();

        for (Thread thread : allStackTraces.keySet()) {

            System.out.println("Thread Name: " + thread.getName() + " state: " + thread.getState());
            
            StackTraceElement[] a = allStackTraces.get(thread);
            
            for (int i = 0; i < a.length;i++) {

                System.out.println("" + a[i].toString());
            }
            System.out.println("#######");
            
        }

    }
}
