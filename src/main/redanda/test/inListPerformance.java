/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.redanda.test;

import java.util.ArrayList;

/**
 *
 * @author robin
 */
public class inListPerformance {
    public static void main(String[] args) {
        
        ArrayList<Integer> arrayList = new ArrayList<Integer>(10);
        
        arrayList.add(1);
        arrayList.add(2);
        arrayList.add(11);
        arrayList.add(1451);
        arrayList.add(1433451);
        arrayList.add(13451);
        arrayList.add(1341);
        arrayList.add(1341);
        arrayList.add(113);
        arrayList.add(113);
        
        for (int i = 0; i<100000000;i++) {
            
            if (arrayList.contains(i)) {
                System.out.println("asdf " + i);
            }
            
            
        }
        
        
    }
}
