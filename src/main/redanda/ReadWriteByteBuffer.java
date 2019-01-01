/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package main.redanda;

import java.nio.ByteBuffer;

/**
 *
 * @author robin
 */
public class ReadWriteByteBuffer {

    private ByteBuffer buffer;
    public int readPos = 0;
    public int writePost = 0;

    public ReadWriteByteBuffer() {
        buffer = ByteBuffer.allocate(1024);
    }

    public byte[] getBytes() {
        return buffer.array();
    }

    public void writeTo(byte[] bytes) {
        buffer.put(bytes, writePost, bytes.length);
        writePost += bytes.length;
    }

    public void readLength(int len) {
        readPos += len;
    }
}
