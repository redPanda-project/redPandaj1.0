package org.redPandaLib.kademlia;

import com.google.gson.Gson;
import kademlia.dht.KadContent;
import kademlia.node.KademliaId;
import kademlia.simulations.DHTContentImpl;

public class KadContentTest implements KadContent
    {

        public static final transient String TYPE = "testtype";

        private KademliaId key;
        private String data;
        private String ownerId;
        private final long createTs;
        private long updateTs;


        {
            this.createTs = this.updateTs = System.currentTimeMillis() / 1000L;
        }

        public KadContentTest()
        {

        }

        public KadContentTest(String ownerId, String data)
        {
            this.ownerId = ownerId;
            this.data = data;
            this.key = new KademliaId();
        }

        public KadContentTest(String data)
        {
            this.ownerId = "main";
            this.data = data;
            this.key = new KademliaId();
        }

        public KadContentTest(KademliaId key, String ownerId)
        {
            this.key = key;
            this.ownerId = ownerId;
        }

        public void setData(String newData)
        {
            this.data = newData;
            this.setUpdated();
        }

        @Override
        public KademliaId getKey()
        {
            return this.key;
        }

        @Override
        public String getType()
        {
            return TYPE;
        }

        @Override
        public String getOwnerId()
        {
            return this.ownerId;
        }

        /**
         * Set the content as updated
         */
        public void setUpdated()
        {
            this.updateTs = System.currentTimeMillis() / 1000L;
        }

        @Override
        public long getCreatedTimestamp()
        {
            return this.createTs;
        }

        @Override
        public long getLastUpdatedTimestamp()
        {
            return this.updateTs;
        }

        @Override
        public byte[] toSerializedForm()
        {
            Gson gson = new Gson();
            return gson.toJson(this).getBytes();
        }

        @Override
        public kademlia.simulations.DHTContentImpl fromSerializedForm(byte[] data)
        {
            System.out.println("string: " + new String(data));
            Gson gson = new Gson();
            kademlia.simulations.DHTContentImpl val = gson.fromJson(new String(data), kademlia.simulations.DHTContentImpl.class);
            return val;
        }

        @Override
        public String toString()
        {
            return "DHTContentImpl[{data=" + this.data + "{ {key:" + this.key + "}]";
        }



}
