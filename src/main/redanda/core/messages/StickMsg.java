/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package main.redanda.core.messages;

import main.redanda.crypt.ECKey;

/**
 *
 * @author rflohr
 */
public class StickMsg extends RawMsg {

    public StickMsg(byte[] pubkey, long timestamp, int nonce) {
        super(new ECKey(null, pubkey), timestamp, nonce);
        public_type = 1;
    }
}
