package main.redanda.core;


import java.io.IOException;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * To change this template, choose Tools | Templates and open the template in
 * the editor.
 */
/**
 *
 * @author rflohr
 */
public class MyOwnPublicKeyImpl implements ECPublicKey {

    String publicKey;

    public MyOwnPublicKeyImpl(String publicKey) {
        this.publicKey = publicKey;
    }

    @Override
    public String getAlgorithm() {
        return "EC";
    }

    @Override
    public String getFormat() {
        return "X.509";
    }

    @Override
    public byte[] getEncoded() {
        try {
            return Channel.string2Byte(publicKey);
        } catch (IOException ex) {
            Logger.getLogger(MyOwnPublicKeyImpl.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;

    }

    @Override
    public ECPoint getW() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ECParameterSpec getParams() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
