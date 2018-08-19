package kademlia.node;

import kademlia.routing.Contact;

import java.math.BigInteger;
import java.util.Comparator;

/**
 * A Comparator to compare 2 keys to a given key
 *
 * @author Joshua Kissoon
 * @since 20140322
 */
public class KeyComparatorContacts implements Comparator<Contact> {

    private final BigInteger key;

    /**
     * @param key The NodeId relative to which the distance should be measured.
     */
    public KeyComparatorContacts(KademliaId key) {
        this.key = key.getInt();
    }

    /**
     * Compare two objects which must both be of type <code>Contact</code>
     * and determine which is closest to the identifier specified in the
     * constructor.
     *
     * @param n1 Contact 1 to compare distance from the key
     * @param n2 Contact 2 to compare distance from the key
     */
    @Override
    public int compare(Contact n1, Contact n2) {
        BigInteger b1 = n1.getNode().getNodeId().getInt();
        BigInteger b2 = n2.getNode().getNodeId().getInt();

        b1 = b1.xor(key);
        b2 = b2.xor(key);

        return b1.abs().compareTo(b2.abs());
    }
}
