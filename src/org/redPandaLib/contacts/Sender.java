/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redPandaLib.contacts;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import org.redPandaLib.Main;
import org.redPandaLib.core.Channel;
import org.redPandaLib.core.Test;

/**
 *
 * @author rflohr
 */
public class Sender {

    public static byte[] generateBytesToSend(ArrayList<Contact> contacts) {

        int byteCount = 0;

        for (Contact c : contacts) {
            byteCount += 4;
            byteCount += c.getByteCount();
        }

        ByteBuffer buffer = ByteBuffer.allocate(1 + 8 + byteCount);

        buffer.put((byte) 12);
        buffer.putLong(Test.localSettings.identity);

        try {
            for (Contact c : contacts) {
                byte[] nameBytes = c.name.getBytes();
                buffer.putInt(nameBytes.length);
                buffer.put(nameBytes);
                buffer.putInt(c.getPhoneNumberHash());
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            Test.sendStacktrace(e);
        }

        return buffer.array();
    }
}
