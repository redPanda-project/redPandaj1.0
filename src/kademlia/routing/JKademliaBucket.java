package kademlia.routing;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.TreeSet;
import java.util.concurrent.locks.ReentrantLock;

import kademlia.KadConfiguration;
import kademlia.node.Node;
import main.redanda.crypt.Utils;

/**
 * A bucket in the Kademlia routing table
 *
 * @author Joshua Kissoon
 * @created 20140215
 */
public class JKademliaBucket implements KademliaBucket {

    /* How deep is this bucket in the Routing Table */
    private final int depth;

    /* Contacts stored in this routing table */
    private final TreeSet<Contact> contacts;

    /* A set of last seen contacts that can replace any current contact that is unresponsive */
    private final TreeSet<Contact> replacementCache;

    private final KadConfiguration config;
    private ReentrantLock lock;


    {
        contacts = new TreeSet<>();
        replacementCache = new TreeSet<>();
        lock = new ReentrantLock(false);
    }

    /**
     * @param depth  How deep in the routing tree is this bucket
     * @param config
     */
    public JKademliaBucket(int depth, KadConfiguration config) {
        this.depth = depth;
        this.config = config;
    }

    @Override
    public synchronized void insert(Contact c) {
        lock.lock();
        try {

            boolean contains = false;
            for (Contact cc : this.contacts) {
                if (Utils.bytesToHexString(cc.getNode().getNodeId().getBytes()).equals(Utils.bytesToHexString(c.getNode().getNodeId().getBytes()))) {
//                if (c.equals(cc)) {
                    contains = true;
                    break;
                }
            }

//            this.contacts.contains(c);

            if (contains) {
                /**
                 * If the contact is already in the bucket, lets update that we've seen it
                 * We need to remove and re-add the contact to get the Sorted Set to update sort order
                 */
                Contact tmp = this.removeFromContacts(c.getNode());
                tmp.setSeenNow();
                tmp.resetStaleCount();
                //lets update ip address, maybe it changed since last seen
                tmp.getNode().setInetAddress(c.getNode().getInetAddress());
                //check again?
                for (Contact cc : this.contacts) {
                    if (Utils.bytesToHexString(cc.getNode().getNodeId().getBytes()).equals(Utils.bytesToHexString(c.getNode().getNodeId().getBytes()))) {
                        throw new RuntimeException("contains method not working! ! !r745");
                    }
                }
                this.contacts.add(tmp);
            } else {

                //check again?
                for (Contact cc : this.contacts) {
                    if (Utils.bytesToHexString(cc.getNode().getNodeId().getBytes()).equals(Utils.bytesToHexString(c.getNode().getNodeId().getBytes()))) {
                        throw new RuntimeException("contains method not working! ! !r745");
                    }
                }


                /* If the bucket is filled, so put the contacts in the replacement cache */
                if (contacts.size() >= this.config.k()) {
                    /* If the cache is empty, we check if any contacts are stale and replace the stalest one */
                    Contact stalest = null;
                    for (Contact tmp : this.contacts) {
                        if (tmp.staleCount() >= this.config.stale()) {
                            /* Contact is stale */
                            if (stalest == null) {
                                stalest = tmp;
                            } else if (tmp.staleCount() > stalest.staleCount()) {
                                stalest = tmp;
                            }
                        }
                    }

                    /* If we have a stale contact, remove it and add the new contact to the bucket */
                    if (stalest != null) {
                        this.contacts.remove(stalest);
                        this.contacts.add(c);
                    } else {
                        /* No stale contact, lets insert this into replacement cache */
                        this.insertIntoReplacementCache(c);
                    }
                } else {
                    this.contacts.add(c);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public synchronized void insert(Node n) {
        this.insert(new Contact(n));
    }

    @Override
    public synchronized boolean containsContact(Contact c) {

        lock.lock();
        try {
            return this.contacts.contains(c);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public synchronized boolean containsNode(Node n) {
        return this.containsContact(new Contact(n));
    }

    @Override
    public synchronized boolean removeContact(Contact c) {
        lock.lock();
        try {
            /* If the contact does not exist, then we failed to remove it */
            if (!this.contacts.contains(c)) {
                return false;
            }

            /* Contact exist, lets remove it only if our replacement cache has a replacement */
            if (!this.replacementCache.isEmpty()) {
                /* Replace the contact with one from the replacement cache */
                this.contacts.remove(c);
                Contact replacement = this.replacementCache.first();
                this.contacts.add(replacement);
                this.replacementCache.remove(replacement);
            } else {
                /* There is no replacement, just increment the contact's stale count */
                this.getFromContacts(c.getNode()).incrementStaleCount();

                if (this.getFromContacts(c.getNode()).staleCount() > 500) {
                    this.contacts.remove(c);
                    System.out.println("removed contact because stale was over 500....");
                }

            }

            return true;
        } finally {
            lock.unlock();
        }
    }

    private synchronized Contact getFromContacts(Node n) {
        lock.lock();
        try {
            for (Contact c : this.contacts) {
                if (c.getNode().equals(n)) {
                    return c;
                }
            }

            /* This contact does not exist */
            throw new NoSuchElementException("The contact does not exist in the contacts list.");
        } finally {
            lock.unlock();
        }
    }

    private synchronized Contact removeFromContacts(Node n) {
        lock.lock();
        try {
            Contact found = null;

            ArrayList<Contact> toRemove = new ArrayList<>(1);

            for (Contact c : this.contacts) {
//                if (c.getNode().equals(n)) {
                if (Utils.bytesToHexString(c.getNode().getNodeId().getBytes()).equals(Utils.bytesToHexString(n.getNodeId().getBytes()))) {

                    toRemove.add(c);
                    found = c;
//                return c;
                }
            }

            if (found != null) {
//                for (Contact c : toRemove) {
//                    this.contacts.remove(c);
//                }
                int size = this.contacts.size();
                TreeSet<Contact> clone = (TreeSet<Contact>) this.contacts.clone();


                boolean b = this.contacts.remove(toRemove.get(0));

                //check again?
                for (Contact cc : this.contacts) {
                    if (Utils.bytesToHexString(cc.getNode().getNodeId().getBytes()).equals(Utils.bytesToHexString(n.getNodeId().getBytes()))) {

                        boolean equals = toRemove.get(0).equals(cc);

                        int i = toRemove.get(0).compareTo(cc);

                        boolean b2 = this.contacts.removeAll(toRemove);

                        throw new RuntimeException("contains method not working! ! !r745");
                    }
                }

                return found;
            }



            /* We got here means this element does not exist */
            throw new NoSuchElementException("Node does not exist in the replacement cache. ");
        } finally {
            lock.unlock();
        }
    }

    @Override
    public synchronized boolean removeNode(Node n) {
        return this.removeContact(new Contact(n));
    }

    @Override
    public synchronized int numContacts() {
        lock.lock();
        try {
            return this.contacts.size();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public synchronized int getDepth() {
        return this.depth;
    }

    @Override
    public synchronized List<Contact> getContacts() {
        lock.lock();
        try {
            final ArrayList<Contact> ret = new ArrayList<>();

            /* If we have no contacts, return the blank arraylist */
            if (this.contacts.isEmpty()) {
                return ret;
            }

            /* We have contacts, lets copy put them into the arraylist and return */
            for (Contact c : this.contacts) {
                ret.add(c);
            }

            return ret;
        } finally {
            lock.unlock();
        }
    }

    /**
     * When the bucket is filled, we keep extra contacts in the replacement cache.
     */
    private synchronized void insertIntoReplacementCache(Contact c) {
        lock.lock();
        try {

            /* Just return if this contact is already in our replacement cache */
            boolean contains = false;
            for (Contact cc : this.replacementCache) {
//                if (Utils.bytesToHexString(cc.getNode().getNodeId().getBytes()).equals(Utils.bytesToHexString(c.getNode().getNodeId().getBytes()))) {
                if (c.equals(cc)) {
                    contains = true;
                    break;
                }
            }
//            this.replacementCache.contains(c)
            if (contains) {
                /**
                 * If the contact is already in the bucket, lets update that we've seen it
                 * We need to remove and re-add the contact to get the Sorted Set to update sort order
                 */
                Contact tmp = this.removeFromReplacementCache(c.getNode());
                tmp.setSeenNow();
                this.replacementCache.add(tmp);
            } else if (this.replacementCache.size() > this.config.k()) {
                /* if our cache is filled, we remove the least recently seen contact */
                this.replacementCache.remove(this.replacementCache.last());
                this.replacementCache.add(c);
            } else {
                this.replacementCache.add(c);
            }
        } finally {
            lock.unlock();
        }
    }

    private synchronized Contact removeFromReplacementCache(Node n) {
        for (Contact c : this.replacementCache) {
            if (c.getNode().equals(n)) {
                this.replacementCache.remove(c);
                return c;
            }
        }

        /* We got here means this element does not exist */
        throw new NoSuchElementException("Node does not exist in the replacement cache. ");
    }

    @Override
    public synchronized String toString() {
        StringBuilder sb = new StringBuilder("Bucket at depth: ");
        sb.append(this.depth);
        sb.append("\n Nodes: \n");
        for (Contact n : this.contacts) {
            sb.append("Node: ");
            sb.append(n.getNode().getNodeId().toString());
            sb.append(" - ");
            sb.append(n.getNode().getSocketAddress());
            sb.append(" -");
            sb.append(" (stale: ");
            sb.append(n.staleCount());
            sb.append(")");
            sb.append("\n");
        }

        return sb.toString();
    }
}
