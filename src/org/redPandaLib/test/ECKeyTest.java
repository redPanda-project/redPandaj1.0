/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redPandaLib.test;

import java.security.PublicKey;
import java.security.Signature;
import org.redPandaLib.SpecialChannels;
import org.redPandaLib.core.LocalSettings;
import org.redPandaLib.core.Test;
import org.redPandaLib.core.messages.TextMsg;
import org.redPandaLib.crypt.ECKey;

/**
 *
 * @author rflohr
 */
public class ECKeyTest {

    public static void main(String[] args) throws Exception {

        Test.localSettings = new LocalSettings();

        Test.localSettings.identity = 0;

        TextMsg build = TextMsg.build(SpecialChannels.MAIN, "djawdaad wadawd");

        Signature signature = Signature.getInstance("SHA256withECDSA");

        build.verify();
        
//        for (int cnt = 0; cnt < 700; cnt++) {
//            //SpecialChannels.MAIN.getKey().verify(build.getContent(), build.getSignature());
//            //build.verify();
//            signature.initVerify();
//            signature.update(message);
//            signature.verify(sigBytes);
//        }

    }
}
