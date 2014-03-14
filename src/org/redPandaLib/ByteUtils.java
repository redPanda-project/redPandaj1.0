/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redPandaLib;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

/**
 *
 * @author robin
 */
public class ByteUtils {

    public static byte[] intToUnsignedShortAsBytes(int unsignedShort) {

        if (unsignedShort < 0) {
            throw new BufferUnderflowException();
        } else if (unsignedShort > 65535) {
            throw new BufferOverflowException();
        }

        byte[] bs = new byte[]{
            (byte) (unsignedShort >>> 8),
            (byte) unsignedShort};
        return bs;
    }

    public static int bytesToUnsignedShortAsInt(byte[] bytes, int off) {

        if (bytes.length - off < 2) {
            throw new BufferUnderflowException();
        }

        int value = (0x000000FF & bytes[0]) << 8 | (0x000000FF & bytes[1]);

        return value;

    }

    public static int bytesToInt(byte[] array, int off) {
        byte[] buffer = new byte[4];
        System.arraycopy(array, off, buffer, 0, 4);

        int value = buffer[0] << 24 | buffer[1] << 16 | buffer[2] << 8
                | buffer[3];
        return value;
    }

    public byte[] intToBytes(int value) {

        byte[] bs = new byte[]{
            (byte) (value >>> 24),
            (byte) (value >>> 16),
            (byte) (value >>> 8),
            (byte) value};

        return bs;
    }
}
