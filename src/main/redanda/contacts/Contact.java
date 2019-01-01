package main.redanda.contacts;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import main.redanda.crypt.Sha256Hash;

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
