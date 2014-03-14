/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redPandaLib.core;

/**
 *
 * @author robin
 */
public class Log {

    public static final int LEVEL = 5;

    public static void put(String msg, int level) {
        if (level > LEVEL) {
            return;
        }
        System.out.println(msg);
    }
}
