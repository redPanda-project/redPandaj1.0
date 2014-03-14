/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redPandaLib.core.messages;

import org.redPandaLib.crypt.ECKey;

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
