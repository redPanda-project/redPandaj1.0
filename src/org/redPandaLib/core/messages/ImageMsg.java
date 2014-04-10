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

    private static int MAX_FILE_SIZE = 1024 * 1024 * 10;
    public static final byte BYTE = (byte) 2;

    protected ImageMsg(ECKey key, long timestamp, int nonce) {
        super(key, timestamp, nonce);
        public_type = 20;
    }

    public ImageMsg(ECKey key, long timestamp, int nonce, byte[] signature, byte[] content, byte[] decryptedContent, Channel channel, boolean verified, boolean readable, int database_Id) {
        super(key, timestamp, nonce, signature, content, decryptedContent, channel, verified, readable, database_Id);
        public_type = 20;
    }

    public static ImageMsg build(Channel channel, String pathToImage) {
        try {
            File file = new File(pathToImage);

            byte[] messageContentBytes = read(file);

            System.out.println("image byytes: " + messageContentBytes.length);

            ECKey key = channel.getKey();
            ImageMsg imgMsg = new ImageMsg(key, System.currentTimeMillis(), 105);

            imgMsg.channel = channel;


            byte[] content = new byte[1 + 8 + messageContentBytes.length];
            ByteBuffer wrap = ByteBuffer.wrap(content);
            wrap.put(BYTE);
            wrap.putLong(Test.localSettings.identity);
            wrap.put(messageContentBytes);

            imgMsg.decryptedContent = content;
            imgMsg.readable = true;

            imgMsg.encrypt();
            imgMsg.sign();

            return imgMsg;

        } catch (FileNotFoundException ex) {
            Logger.getLogger(ImageMsg.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(ImageMsg.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }

    public byte[] getText() {

        ByteBuffer wrap = ByteBuffer.wrap(decryptedContent);
        wrap.get();
        wrap.getLong();

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

    public String getTextString() {
        try {
            return new String(getText(), "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(ImageMsg.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
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
