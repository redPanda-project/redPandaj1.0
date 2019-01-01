/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package main.redanda;

import java.nio.ByteBuffer;

import main.redanda.crypt.AddressFormatException;
import main.redanda.crypt.Utils;

/**
 *
 * @author robin
 */
public class TestMain {

    public static void main(String[] args) throws AddressFormatException {
            
            
            //        Channel generateNewPublic = Channel.generateNewPublic("test");
            //        String exportForHumans = generateNewPublic.exportForHumans();
            //        System.out.println("oo " + exportForHumans);
    //        Channel importFromHuman = Channel.importFromHuman("pur2kruJJPy6wQaK2KrT65st2dMQETX6sK343XQ9c3AakwYDkZk88pFc6", "test");
            //System.out.println(""+ eCKey.getPrivKeyBytes().length);
            
    //        if (importFromHuman == null) {
    //            System.out.println("Channel invalid...");
    //        } else {
    //            System.out.println("Channel okay!");
    //        }
            
            //        ECKey eCKey = new ECKey();
            //        Sha256Hash hash = Sha256Hash.create("asdf".getBytes());
            //        ECDSASignature sign = eCKey.sign(hash);
            //
            //        byte[] encodeToDER = new byte[72];
            //        byte[] sigBytes = sign.encodeToDER();
            //        System.arraycopy(sigBytes, 0, encodeToDER, 0, sigBytes.length);
            //
            //
            //        System.out.println("len: " + encodeToDER.length);
            //
            //        Sha256Hash hash2 = Sha256Hash.create("asdf".getBytes());
            //        boolean verify = eCKey.verify(hash2.getBytes(), encodeToDER);
            //
            //        System.out.println("" + verify);
            //        System.out.println("" + verify);
            //        ECKey eCKey = new ECKey();
            //        String signMessage = eCKey.signMessage("asdf");
            //
            //        for (int i = 0; i < 500; i++) {
            //            try {
            //                eCKey.verifyMessage("asdf", signMessage);
            //            } catch (SignatureException ex) {
            //                Logger.getLogger(TestMain.class.getName()).log(Level.SEVERE, null, ex);
            //            }
            //        }
            //        ByteBuffer allocate = ByteBuffer.allocate(30);
            //        //
            //        allocate.putChar('A');
            //        //        allocate.put((byte) 257);
            //        //
            //        //
            //        allocate.flip();
            //        System.out.println("" + allocate.getChar());
            //        allocate.flip();
            //        allocate.limit(allocate.capacity());
            //
            //
            //        System.out.println("" + allocate);
            //
            //        allocate.putChar('B');
            //
            //        allocate.flip();
            //        System.out.println("" + allocate.getChar());
            //        allocate.flip();
            //
            //        int unsingedShort = 6500;
            //
            //
            //        allocate.put(ByteUtils.intToUnsignedShortAsBytes(unsingedShort));
            //
            //        allocate.position(0);
            //
            //        byte[] bs = new byte[] {allocate.get(),allocate.get()};
            //
            //        System.out.println("valiuEWe: " + ByteUtils.bytesToUnsignedShortAsInt(bs));
            //        byte[] bs = new byte[]{
            //            (byte) (unsingedShort >>> 8),
            //            (byte) unsingedShort};
            //
            //        allocate.put(bs);
            //
            //        System.out.println("" + allocate);
            //
            //        allocate.position(0);
            //        byte g1 = allocate.get();
            //        byte g2 = allocate.get();
            //
            //        System.out.println("a: " + (int) g1  + " b: " + (int) g2);
            //        int firstByte = 0x000000FF & ((int)g1);
            //        int secondByte = 0x000000FF & ((int)g2);
            //        char anUnsignedShort = (char) (firstByte << 8 | secondByte);
            //
            //        System.out.println("vvv: " + (int) anUnsignedShort);
            //
            //        allocate.position(0);
            //
            //        int value = (0x000000FF & allocate.get()) << 8 | (0x000000FF & allocate.get()) ;
            //
            //        System.out.println("value: " + value);
            //        String a = "";
            //
            //        for (int i = 0; i < 4; i++) {
            //            a += (char) allocate.get();
            //        }
            //        System.out.println("" + a);
            //        allocate.position(0);
            //        System.out.println("" + Utils.bytesToHexString(allocate.array()));
            //        Msg msg = new Msg(System.currentTimeMillis(), "nonce", SpecialChannels.MAIN, "", 2, "hallo ich bin der content");
            //        System.out.println("" + msg);
            //
            //
            //        Msg instance = Msg.getInstance(msg.toString());
            //        System.out.println("" + instance.verify());
            //
            //
            //        //suche privaten schluessel zum entschluesseln...
            //
            //        if (instance.getChannel().equals(SpecialChannels.MAIN)) {
            //            instance.setChannel(SpecialChannels.MAIN);
            //        }
            //
            //
            //        Main.getChannels();
            //        Main.getChannels();

    }

    private static String showContent(ByteBuffer b) {

        if (!b.hasRemaining()) {
            return "buffer is empty";
        }


        byte[] bytes = new byte[b.remaining()];
        b.get(bytes);
        b.rewind();
        return Utils.bytesToHexString(bytes);

    }
}
