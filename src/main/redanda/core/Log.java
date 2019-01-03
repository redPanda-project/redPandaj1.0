/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package main.redanda.core;

import java.util.Arrays;
import java.util.List;

/**
 * @author robin
 */
public class Log {

    public static int LEVEL = 50;

    static {
//        System.out.println("is testing: " + isJUnitTest());
        if (isJUnitTest()) {
            LEVEL = 3000;
        }
    }

    public static void put(String msg, int level) {
        if (level > LEVEL) {
            return;
        }
        System.out.println(msg);
    }

    public static boolean isJUnitTest() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        List<StackTraceElement> list = Arrays.asList(stackTrace);
        for (StackTraceElement element : list) {
            if (element.getClassName().startsWith("org.junit.")) {
                return true;
            }
        }
        return false;
    }
}
