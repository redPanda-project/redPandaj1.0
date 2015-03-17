/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redPandaLib.core.messages;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.redPandaLib.core.Channel;
import org.redPandaLib.core.Test;
import org.redPandaLib.crypt.ECKey;

/**
 *
 * @author robin
 */
public class ImageMsg extends RawMsg {

    private static int MAX_FILE_SIZE = 1024 * 1024 * 15;
    public static final byte BYTE = (byte) 2;

    protected ImageMsg(ECKey key, long timestamp, int nonce) {
        super(key, timestamp, nonce);
        public_type = 20;
    }

    public ImageMsg(ECKey key, long timestamp, int nonce, byte[] signature, byte[] content, byte[] decryptedContent, Channel channel, boolean verified, boolean readable, int database_Id) {
        super(key, timestamp, nonce, signature, content, decryptedContent, channel, verified, readable, database_Id);
        public_type = 20;
    }

    public static ArrayList<ImageMsg> build(Channel channel, String pathToImage, boolean lowPriority) {
        try {
            File file = new File(pathToImage);

            byte[] messageContentBytes = read(file);

            System.out.println("image bytes: " + messageContentBytes.length);

            ECKey key = channel.getKey();

            ArrayList<ImageMsg> msgs = new ArrayList<ImageMsg>();

            //
            int writtenBytes = 0;
            int blockSize = Math.max(1024 * 50, messageContentBytes.length / 30);

            System.out.println("blocksize 1 : " + blockSize);

            if (blockSize > 100 * 1024) {
                blockSize = 100 * 1024;
            }

            int block = 0;
            int blocks = messageContentBytes.length / blockSize + 1;

            long timeStamp = System.currentTimeMillis();

            while (writtenBytes < messageContentBytes.length) {

                int bytesThisRound = 0;
                if (block + 1 == blocks) {
                    //last block
                    bytesThisRound = messageContentBytes.length - writtenBytes;
                } else {
                    bytesThisRound = blockSize;
                }

                System.out.println("generating Img msg of size: " + bytesThisRound + " block: " + block + " of " + blocks);

                ImageMsg imgMsg = new ImageMsg(key, timeStamp, 110 + block);

                if (lowPriority) {
                    imgMsg.public_type = 100;
                }
                imgMsg.channel = channel;

                byte[] content = new byte[1 + 8 + 4 + 4 + bytesThisRound];
                ByteBuffer wrap = ByteBuffer.wrap(content);
                wrap.put(BYTE);
                wrap.putLong(Test.localSettings.identity);

                wrap.putInt(block);
                wrap.putInt(blocks);

                wrap.put(messageContentBytes, writtenBytes, bytesThisRound);

                imgMsg.decryptedContent = content;
                imgMsg.readable = true;

                imgMsg.encrypt();
                imgMsg.sign();

                writtenBytes += blockSize;
                block++;
                msgs.add(imgMsg);

            }

            return msgs;

        } catch (FileNotFoundException ex) {
            Logger.getLogger(ImageMsg.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(ImageMsg.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }

    public int getPartCount() {
        ByteBuffer wrap = ByteBuffer.wrap(decryptedContent);
        wrap.get();
        wrap.getLong();
        return wrap.getInt();
    }

    public int getParts() {
        ByteBuffer wrap = ByteBuffer.wrap(decryptedContent);
        wrap.get();
        wrap.getLong();
        wrap.getInt();
        return wrap.getInt();
    }

    public byte[] getImageBytes() {

        ByteBuffer wrap = ByteBuffer.wrap(decryptedContent);
        wrap.get();
        wrap.getLong();

        wrap.getInt();
        wrap.getInt();
        byte[] out = new byte[wrap.remaining()];
        wrap.get(out);
        return out;

        //System.out.println("decrypted bytes: " + Msg.bytesToHexString(decryptedContent));
//
//        try {
//            return new String(decryptedContent, 9, decryptedContent.length - 9, "UTF-8");
//        } catch (UnsupportedEncodingException ex) {
//            Logger.getLogger(TextMsg.class.getName()).log(Level.SEVERE, null, ex);
//        }
//
//        return "charset error";
    }

    public long getIdentity() {
        ByteBuffer wrap = ByteBuffer.wrap(decryptedContent);
        wrap.get();
        return wrap.getLong();
    }

    public static byte[] read(File file) throws IOException {

        if (file.length() > MAX_FILE_SIZE) {
            return null;
        }

        byte[] buffer = new byte[(int) file.length()];
        InputStream ios = null;
        try {
            ios = new FileInputStream(file);
            if (ios.read(buffer) == -1) {
                throw new IOException("EOF reached while trying to read the whole file");
            }
        } finally {
            try {
                if (ios != null) {
                    ios.close();
                }
            } catch (IOException e) {
            }
        }

        return buffer;
    }
}
