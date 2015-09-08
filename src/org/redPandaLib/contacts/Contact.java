/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redPandaLib.contacts;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import org.redPandaLib.crypt.Sha256Hash;

/**
 *
 * @author rflohr
 */
public class Contact implements Serializable {

    private static final long serialVersionUID = 1L;

    public String name;
    public String phoneNumber;

    public int getByteCount() {
        return name.getBytes().length + phoneNumber.getBytes().length;
    }

    public int getPhoneNumberHash() throws UnsupportedEncodingException {
        byte[] bytes = phoneNumber.getBytes("UTF-8");
        Sha256Hash create = Sha256Hash.create(bytes);
        return create.hashCode();
    }
}
