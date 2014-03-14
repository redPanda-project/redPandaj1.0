/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redPandaLib.database;

import crypt.Utils;
import java.io.UnsupportedEncodingException;
import java.sql.*;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.redPandaLib.core.Channel;
import org.redPandaLib.core.Test;
import org.redPandaLib.core.messages.RawMsg;
import org.redPandaLib.core.messages.TextMessageContent;
import org.redPandaLib.core.messages.TextMsg;
import org.redPandaLib.crypt.ECKey;

/**
 *
 * @author rflohr
 */
public class DirectMessageStore implements MessageStore {

    public Connection connection;

    public DirectMessageStore(Connection connection) throws SQLException {
        this.connection = connection;
        //connection = new HsqlConnection().getConnection();
    }

    public boolean containsMsg(RawMsg msg) {
        try {
            int pubkeyIdWithInsert = getPubkeyIdWithInsert(connection, msg.getKey().getPubKey());
            int messageId = getMessageId(connection, pubkeyIdWithInsert, msg.public_type, msg.timestamp, msg.nonce);

            return (messageId != -1);

        } catch (SQLException ex) {
            Logger.getLogger(DirectMessageStore.class.getName()).log(Level.SEVERE, null, ex);
        }

        return false;
    }

    public int getPubkeyId(ECKey key) {
        try {
            int pubkeyIdWithInsert = getPubkeyIdWithInsert(connection, key.getPubKey());
            return pubkeyIdWithInsert;
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return -1;
    }

    public int getMsgId(RawMsg msg) {
        try {
            int pubkeyIdWithInsert = getPubkeyIdWithInsert(connection, msg.getKey().getPubKey());
            int messageId = getMessageId(connection, pubkeyIdWithInsert, msg.public_type, msg.timestamp, msg.nonce);
            return messageId;
        } catch (SQLException ex) {
            Logger.getLogger(DirectMessageStore.class.getName()).log(Level.SEVERE, null, ex);
        }

        return -1;
    }

    public void saveMsg(RawMsg msg) {
        try {
            int pubkeyIdWithInsert = getPubkeyIdWithInsert(connection, msg.getKey().getPubKey());

            //System.out.println("KeyId: " + pubkeyIdWithInsert);
            int messageIdWithInsert = getMessageIdWithInsert(connection, pubkeyIdWithInsert, msg.public_type, msg.timestamp, msg.nonce, msg.signature, msg.content, msg.verified);


            //System.out.println("Message ID: " + messageIdWithInsert);





        } catch (SQLException ex) {
            Logger.getLogger(Wrapper.class.getName()).log(Level.SEVERE, null, ex);
        }
//        try {
//            hsqlConnection.getConnection().commit();
//            System.out.println("Commiting changes...");
//        } catch (SQLException ex) {
//            Logger.getLogger(HsqlMessageStore.class.getName()).log(Level.SEVERE, null, ex);
//        }

    }

    private int getPubkeyIdWithInsert(Connection connection, byte[] pubkeyBytes) throws SQLException {
        //get Key Id
        String query = "SELECT pubkey_id,pubkey from pubkey WHERE pubkey = ?";
        //stmt.executeQuery("SELECT id,key from pubkey WHERE key EQUASLS "+ msg.getKey().getPubKey())

        PreparedStatement pstmt = connection.prepareStatement(query);
        pstmt.setBytes(1, pubkeyBytes);
        ResultSet executeQuery = pstmt.executeQuery();



//        boolean found = false;
//        while (executeQuery.next()) {
//            found = true;
//            int aInt = executeQuery.getInt("id");
//            byte[] bytes = executeQuery.getBytes("pubkey");
//
//            System.out.println("ID: " + aInt + " bytes: " + Channel.byte2String(bytes));
//        }

        if (!executeQuery.next()) {
            System.out.println("noch nicht in der db 2");

            executeQuery.close();
            pstmt.close();

            query = "INSERT into pubkey (pubkey) VALUES (?)";
            pstmt = connection.prepareStatement(query);
            pstmt.setBytes(1, pubkeyBytes);
            pstmt.execute();
            pstmt.close();

            //get Key Id
            query = "SELECT pubkey_id,pubkey from pubkey WHERE pubkey = ?";
            //stmt.executeQuery("SELECT id,key from pubkey WHERE key EQUASLS "+ msg.getKey().getPubKey())

            pstmt = connection.prepareStatement(query);
            pstmt.setBytes(1, pubkeyBytes);
            executeQuery = pstmt.executeQuery();
            executeQuery.next();//braucht man das?
        }


        int aInt = executeQuery.getInt("pubkey_id");

        executeQuery.close();
        pstmt.close();

        //byte[] bytes = executeQuery.getBytes("pubkey");
        //System.out.println("ID: " + aInt + " bytes: " + Channel.byte2String(bytes));

        return aInt;

    }

    private byte[] getPubkeyById(Connection connection, int pubkey_id) throws SQLException {
        //get Key Id
        String query = "SELECT pubkey from pubkey WHERE pubkey_id = ?";
        //stmt.executeQuery("SELECT id,key from pubkey WHERE key EQUASLS "+ msg.getKey().getPubKey())

        PreparedStatement pstmt = connection.prepareStatement(query);
        pstmt.setInt(1, pubkey_id);
        ResultSet executeQuery = pstmt.executeQuery();


        if (!executeQuery.next()) {
            executeQuery.close();
            pstmt.close();
            return null;
        }


        byte[] b = executeQuery.getBytes("pubkey");

        executeQuery.close();
        pstmt.close();

        //byte[] bytes = executeQuery.getBytes("pubkey");
        //System.out.println("ID: " + aInt + " bytes: " + Channel.byte2String(bytes));

        return b;

    }

    /**
     * Returns the id, -1 if the message not in db
     *
     * @param connection
     * @param pubkey_id
     * @param timestamp
     * @param nonce
     * @return
     * @throws SQLException
     */
    private int getMessageId(Connection connection, int pubkey_id, byte pubkey_type, long timestamp, int nonce) throws SQLException {
        //get Key Id
        String query = "SELECT message_id from message WHERE pubkey_id = ? AND public_type = ? AND timestamp = ? AND nonce = ?";
        PreparedStatement pstmt = connection.prepareStatement(query);
        pstmt.setInt(1, pubkey_id);
        pstmt.setByte(2, pubkey_type);
        pstmt.setLong(3, timestamp);
        pstmt.setInt(4, nonce);
        ResultSet executeQuery = pstmt.executeQuery();



//        boolean found = false;
//        while (executeQuery.next()) {
//            found = true;
//            int aInt = executeQuery.getInt("id");
//            byte[] bytes = executeQuery.getBytes("pubkey");
//
//            System.out.println("ID: " + aInt + " bytes: " + Channel.byte2String(bytes));
//        }

        if (!executeQuery.next()) {
            return -1;
        }

        int aInt = executeQuery.getInt("message_id");

        executeQuery.close();
        pstmt.close();

        return aInt;

    }

    private int getMessageIdWithInsert(Connection connection, int pubkey_id, byte pubkey_type, long timestamp, int nonce, byte[] signature, byte[] content, boolean verified) throws SQLException {
        //get Key Id
        String query = "SELECT message_id from message WHERE pubkey_id = ? AND public_type = ? AND timestamp = ? AND nonce = ?";
        PreparedStatement pstmt = connection.prepareStatement(query);
        pstmt.setInt(1, pubkey_id);
        pstmt.setByte(2, pubkey_type);
        pstmt.setLong(3, timestamp);
        pstmt.setInt(4, nonce);
        ResultSet executeQuery = pstmt.executeQuery();



//        boolean found = false;
//        while (executeQuery.next()) {
//            found = true;
//            int aInt = executeQuery.getInt("id");
//            byte[] bytes = executeQuery.getBytes("pubkey");
//
//            System.out.println("ID: " + aInt + " bytes: " + Channel.byte2String(bytes));
//        }

        if (!executeQuery.next()) {
            //System.out.println("noch nicht in der db");

            //System.out.println("id: " + pubkey_id + " timestamp: " + timestamp + " nonce: " + nonce);


            executeQuery.close();
            pstmt.close();
            //message_id INTEGER PRIMARY KEY IDENTITY, pubkey_id INTEGER, timestamp BIGINT, nonce INTEGER,  signature BINARY(72), content LONGVARBINARY, verified boolean
            query = "INSERT into message (pubkey_id,public_type,timestamp,nonce,signature,content,verified) VALUES (?,?,?,?,?,?,?)";
            pstmt = connection.prepareStatement(query);
            pstmt.setInt(1, pubkey_id);
            pstmt.setByte(2, pubkey_type);
            pstmt.setLong(3, timestamp);
            pstmt.setInt(4, nonce);
            pstmt.setBytes(5, signature);
            pstmt.setBytes(6, content);
            pstmt.setBoolean(7, verified);
            pstmt.execute();
            pstmt.close();

            //get Key Id
            query = "SELECT message_id from message WHERE pubkey_id = ? AND public_type = ? AND timestamp = ? AND nonce = ?";
            pstmt = connection.prepareStatement(query);
            pstmt.setInt(1, pubkey_id);
            pstmt.setByte(2, pubkey_type);
            pstmt.setLong(3, timestamp);
            pstmt.setInt(4, nonce);
            executeQuery = pstmt.executeQuery();
            executeQuery.next();//braucht man das?
        }


        int aInt = executeQuery.getInt("message_id");

        executeQuery.close();
        pstmt.close();

        return aInt;

    }

    public void showTablePubkey() {
        try {
            System.out.println("Table content: ");
            Statement stmt = connection.createStatement();
            String query = "SELECT pubkey_id,pubkey from pubkey";

            ResultSet executeQuery = stmt.executeQuery(query);
            while (executeQuery.next()) {
                int aInt = executeQuery.getInt("pubkey_id");
                byte[] bytes = executeQuery.getBytes("pubkey");
                System.out.println("ID: " + aInt + " key: " + Channel.byte2String(bytes));
            }
        } catch (SQLException ex) {
            Logger.getLogger(DirectMessageStore.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void showTableMessage() {
        //message_id INTEGER PRIMARY KEY IDENTITY, pubkey_id INTEGER, timestamp BIGINT, nonce INTEGER,  signature BINARY(72), content LONGVARBINARY, verified boolean
        try {
            System.out.println("Table Message: ");
            Statement stmt = connection.createStatement();
            String query = "SELECT * from message";

            ResultSet executeQuery = stmt.executeQuery(query);
            while (executeQuery.next()) {
                int aInt = executeQuery.getInt("message_id");
                int pubkey_id = executeQuery.getInt("pubkey_id");
                byte[] bytes = executeQuery.getBytes("content");
                if (bytes == null) {
                    System.out.println("ID: " + aInt);
                } else {
                    System.out.println("ID: " + aInt + " pubkeyid: " + pubkey_id + " content: " + Utils.bytesToHexString(bytes));
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(DirectMessageStore.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Warning, we assume that the pubkeybytes are known and are not fetched
     * from DB. So the resulting object doesnt contain an ECKey!!!
     *
     * @param connection
     * @param message_id
     * @return
     * @throws SQLException
     */
    @Override
    public RawMsg getMessageById(int message_id) {
        try {
            //get Key Id
            String query = "SELECT * from message WHERE message_id = ?";
            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setInt(1, message_id);
            ResultSet executeQuery = pstmt.executeQuery();

            if (!executeQuery.next()) {
                executeQuery.close();
                pstmt.close();
                return null;
            }





            //message_id INTEGER PRIMARY KEY IDENTITY, pubkey_id INTEGER, timestamp BIGINT, nonce INTEGER,  signature BINARY(72), content LONGVARBINARY, verified boolean
            //int pubkey_id = executeQuery.getInt("pubkey_id");
            //byte[] pubkeyBytes = getPubkeyById(connection, pubkey_id);

            byte public_type = executeQuery.getByte("public_type");
            long timestamp = executeQuery.getLong("timestamp");
            int nonce = executeQuery.getInt("nonce");
            byte[] signature = executeQuery.getBytes("signature");
            byte[] content = executeQuery.getBytes("content");
            boolean verified = executeQuery.getBoolean("verified");
            RawMsg rawMsg = new RawMsg(timestamp, nonce, signature, content, verified);


//            System.out.println("sign: " + Utils.bytesToHexString(signature));
//            System.out.println("content: " + Utils.bytesToHexString(content));

            rawMsg.public_type = public_type;

            executeQuery.close();
            pstmt.close();
            return rawMsg;
        } catch (SQLException ex) {
            Logger.getLogger(DirectMessageStore.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }

    @Override
    public void quit() {
        try {
            connection.commit();
            connection.close();
        } catch (SQLException ex) {
            Logger.getLogger(DirectMessageStore.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    @Override
    public ResultSet getAllMessagesForSync(long from, long to) {
//        ArrayList<RawMsg> list = new ArrayList<RawMsg>();

        try {
            //get Key Id
            String query = "SELECT message_id,pubkey.pubkey_id, pubkey,public_type,timestamp,nonce,signature,content,verified from message left join pubkey on (pubkey.pubkey_id = message.pubkey_id) WHERE timestamp > ? order by timestamp asc";
            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setLong(1, from);
            ResultSet executeQuery = pstmt.executeQuery();

            return executeQuery;

//            while (executeQuery.next()) {
//                int message_id = executeQuery.getInt("message_id");
//                int pubkey_id = executeQuery.getInt("pubkey_id");
//                byte[] bytes = executeQuery.getBytes("pubkey");
//                ECKey ecKey = new ECKey(null, bytes);
//                ecKey.database_id = pubkey_id;
//
//
//                byte public_type = executeQuery.getByte("public_type");
//                long timestamp = executeQuery.getLong("timestamp");
//                int nonce = executeQuery.getInt("nonce");
//                byte[] signature = executeQuery.getBytes("signature");
//                byte[] content = executeQuery.getBytes("content");
//                boolean verified = executeQuery.getBoolean("verified");
//                RawMsg rawMsg = new RawMsg(timestamp, nonce, signature, content, verified);
//                rawMsg.database_Id = message_id;
//                rawMsg.key = ecKey;
//                rawMsg.public_type = public_type;
//                list.add(rawMsg);
//            }


//            executeQuery.close();
//            pstmt.close();
        } catch (SQLException ex) {
            Logger.getLogger(DirectMessageStore.class.getName()).log(Level.SEVERE, null, ex);
        }



        return null;
    }

    @Override
    public ArrayList<RawMsg> getMessagesForPubkey(byte[] pubKey, long from, int to) {


        ECKey ecKey = new ECKey(null, pubKey);
        int pubkeyId = getPubkeyId(ecKey);
        ecKey.database_id = pubkeyId;

        ArrayList<RawMsg> list = new ArrayList<RawMsg>();

        try {
            //get Key Id
            String query = "SELECT message_id,timestamp,nonce,signature,content,verified from message left join pubkey on (pubkey.pubkey_id = message.pubkey_id) WHERE timestamp > ? AND pubkey = ? order by timestamp asc";
            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setLong(1, from);
            pstmt.setBytes(2, pubKey);
            ResultSet executeQuery = pstmt.executeQuery();

            while (executeQuery.next()) {
                int message_id = executeQuery.getInt("message_id");
                byte public_type = executeQuery.getByte("public_type");
                long timestamp = executeQuery.getLong("timestamp");
                int nonce = executeQuery.getInt("nonce");
                byte[] signature = executeQuery.getBytes("signature");
                byte[] content = executeQuery.getBytes("content");
                boolean verified = executeQuery.getBoolean("verified");
                RawMsg rawMsg = new RawMsg(timestamp, nonce, signature, content, verified);
                rawMsg.database_Id = message_id;
                rawMsg.key = ecKey;
                rawMsg.public_type = public_type;
                list.add(rawMsg);
            }


            executeQuery.close();
            pstmt.close();
        } catch (SQLException ex) {
            Logger.getLogger(DirectMessageStore.class.getName()).log(Level.SEVERE, null, ex);
        }



        return list;
    }

    public void addDecryptedContent(int pubkey_id, int message_id, int message_type, byte[] decryptedContent, long identity, boolean fromMe) {
        try {
            //channelmessage (channel_id INTEGER, message_id INTEGER, message_type INTEGER, decryptedContent LONGVARBINARY);
            String query = "INSERT into channelmessage (pubkey_id,message_id,message_type,decryptedContent,identity,fromMe) VALUES (?,?,?,?,?,?)";
            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setInt(1, pubkey_id);
            pstmt.setInt(2, message_id);
            pstmt.setInt(3, message_type);
            pstmt.setBytes(4, decryptedContent);
            pstmt.setLong(5, identity);
            pstmt.setBoolean(6, fromMe);
            pstmt.execute();
            pstmt.close();
        } catch (SQLException ex) {
            Logger.getLogger(DirectMessageStore.class.getName()).log(Level.SEVERE, null, ex);
        }


    }

    public ArrayList<TextMessageContent> getMessageContentsForPubkey(byte[] pubKey, long from, long to) {


        ECKey ecKey = new ECKey(null, pubKey);
        int pubkeyId = getPubkeyId(ecKey);

        ArrayList<TextMessageContent> list = new ArrayList<TextMessageContent>();
        Channel instanceByPublicKey = Channel.getInstanceByPublicKey(pubKey);

        try {
            //get Key Id
            String query = "SELECT message_id,message_type,decryptedContent,timestamp,identity,fromMe,nonce from channelmessage LEFT JOIN message on (channelmessage.message_id = message.message_id) WHERE pubkey_id =? AND timestamp < ? AND timestamp > ? "
             + "UNION (SELECT message_id,message_type,decryptedContent,timestamp,identity,fromMe,nonce from channelmessage LEFT JOIN message on (channelmessage.message_id = message.message_id) WHERE pubkey_id =? AND timestamp < ? ORDER BY message_type, timestamp LIMIT 200)";

            System.out.println("QUERY: " + query);

            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setInt(1, pubkeyId);
            pstmt.setLong(2, to);
            pstmt.setLong(3, from);
            pstmt.setInt(4, pubkeyId);
            pstmt.setLong(5, to);

            System.out.println("STM: " + pstmt.toString());

            ResultSet executeQuery = pstmt.executeQuery();

            //System.out.println("kndkjwhd");

            System.out.println("reading data...");

            while (executeQuery.next()) {
                int message_id = executeQuery.getInt("message_id");
                int message_type = executeQuery.getInt("message_type");
                byte[] decryptedContent = executeQuery.getBytes("decryptedContent");
                long timestamp = executeQuery.getLong("timestamp");
                long identity = executeQuery.getLong("identity");
                boolean fromMe = executeQuery.getBoolean("fromMe");
                long nonce = executeQuery.getLong("nonce");
                //TextMsg textMsg = new TextMsg(ecKey, timestamp, to, null, null, decryptedContent, instanceByPublicKey, true, true, message_id);
                //TextMessageContent textMessageContent = new TextMessageContent(message_id, pubkeyId, message_type, timestamp, decryptedContent, instanceByPublicKey , , query, fromMe)fromMe);
                //if (decryptedContent.length < 1 + 8 + 1) {
                //continue;
                //}
                //TextMessageContent fromTextMsg = TextMessageContent.fromTextMsg(textMsg, fromMe);
                TextMessageContent textMessageContent = new TextMessageContent();
                textMessageContent.database_id = message_id;
                textMessageContent.channel = instanceByPublicKey;
                textMessageContent.text = new String(decryptedContent, "UTF-8");
                textMessageContent.message_type = message_type;
                textMessageContent.fromMe = fromMe;
                textMessageContent.timestamp = timestamp;
                textMessageContent.identity = identity;
                textMessageContent.decryptedContent = decryptedContent;

                //System.out.println("MSG!! " + message_id);

                list.add(textMessageContent);
                System.out.println("added");
            }
            executeQuery.close();

            System.out.println("SIZE: " + list.size());

//            long lastFrom = from;
//
//            while (list.size() < 45) {
//
//                if (from - lastFrom > 1000 * 60 * 60 * 24 * 31L) {
//                    System.out.println("break");
//                    break;
//                }
//
//                long lastlastFrom = lastFrom;
//                lastFrom -= 1000 * 60 * 60 * 6;
//
//                System.out.println("inter 6 hours before from: " + lastFrom + " to: " + lastlastFrom);
//
//                query = "SELECT message_id,message_type,decryptedContent,timestamp,identity,fromMe,nonce from channelmessage LEFT JOIN message on (channelmessage.message_id = message.message_id) WHERE pubkey_id = ? AND timestamp <= ? AND timestamp > ? ORDER BY message_type ASC, timestamp";
//                pstmt = connection.prepareStatement(query);
//                pstmt.setInt(1, pubkeyId);
//                pstmt.setLong(2, lastlastFrom);
//                pstmt.setLong(3, lastFrom);
//                executeQuery = pstmt.executeQuery();
//
//                //System.out.println("kndkjwhd");
//                int index = 0;
//                while (executeQuery.next()) {
//                    int message_id = executeQuery.getInt("message_id");
//                    int message_type = executeQuery.getInt("message_type");
//                    byte[] decryptedContent = executeQuery.getBytes("decryptedContent");
//                    long timestamp = executeQuery.getLong("timestamp");
//                    long identity = executeQuery.getLong("identity");
//                    boolean fromMe = executeQuery.getBoolean("fromMe");
//                    long nonce = executeQuery.getLong("nonce");
//                    //TextMsg textMsg = new TextMsg(ecKey, timestamp, to, null, null, decryptedContent, instanceByPublicKey, true, true, message_id);
//                    //TextMessageContent textMessageContent = new TextMessageContent(message_id, pubkeyId, message_type, timestamp, decryptedContent, instanceByPublicKey , , query, fromMe)fromMe);
//                    //if (decryptedContent.length < 1 + 8 + 1) {
//                    //continue;
//                    //}
//                    //TextMessageContent fromTextMsg = TextMessageContent.fromTextMsg(textMsg, fromMe);
//                    TextMessageContent textMessageContent = new TextMessageContent();
//                    textMessageContent.database_id = message_id;
//                    textMessageContent.channel = instanceByPublicKey;
//                    textMessageContent.text = new String(decryptedContent, "UTF-8");
//                    textMessageContent.message_type = message_type;
//                    textMessageContent.fromMe = fromMe;
//                    textMessageContent.timestamp = timestamp;
//                    textMessageContent.identity = identity;
//                    textMessageContent.decryptedContent = decryptedContent;
//
//                    //System.out.println("MSG!! " + message_id);
//
//                    list.add(index, textMessageContent);
//                    index++;
//                }
//                executeQuery.close();
//
//            }



            pstmt.close();
        } catch (UnsupportedEncodingException ex) {

            Test.sendStacktrace(ex);

        } catch (SQLException ex) {
            Test.sendStacktrace(ex);
        }



        return list;
    }

    @Override
    public void showTableMessageContent() {
        try {
            System.out.println("Table Message Content: ");
            Statement stmt = connection.createStatement();
            String query = "SELECT * from channelmessage";

            ResultSet executeQuery = stmt.executeQuery(query);
            while (executeQuery.next()) {
                int message_id = executeQuery.getInt("message_id");
                int message_type = executeQuery.getInt("message_type");
                byte[] decryptedContent = executeQuery.getBytes("decryptedContent");
                //long timestamp = executeQuery.getLong("timestamp");
                System.out.println("MSG message_id: " + message_id + " message_type: " + message_type + " decryptedContent: " + new String(decryptedContent, "UTF-8"));
            }
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(DirectMessageStore.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SQLException ex) {
            Logger.getLogger(DirectMessageStore.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public int getMessageCount() {
        try {
            String query = "SELECT count(message_id) from message";
            //stmt.executeQuery("SELECT id,key from pubkey WHERE key EQUASLS "+ msg.getKey().getPubKey())
            Statement createStatement = connection.createStatement();
            ResultSet executeQuery = createStatement.executeQuery(query);

            executeQuery.next();
            int aInt = executeQuery.getInt(1);
            executeQuery.close();
            createStatement.close();

            return aInt;
        } catch (SQLException ex) {
            Logger.getLogger(DirectMessageStore.class.getName()).log(Level.SEVERE, null, ex);
        }

        return -1;
    }

    @Override
    public void commitDatabase() {
//        try {
//            connection.commit();
//        } catch (SQLException ex) {
//            Logger.getLogger(DirectMessageStore.class.getName()).log(Level.SEVERE, null, ex);
//        }
    }

    @Override
    public Connection getConnection() {
        return connection;
    }

    private void removeMessagesFromChannel(Connection connection, int pubkey_id, byte pubkey_type, long timestamp) throws SQLException {
        //get Key Id
        //String query = "SELECT message_id from message WHERE pubkey_id = ? AND public_type = ? AND timestamp = ? AND nonce = ?";
        String query =
                "DELETE FROM message WHERE pubkey_id = ? AND public_type = 20 AND timestamp < ?";
        PreparedStatement pstmt = connection.prepareStatement(query);
        pstmt.setInt(1, pubkey_id);
        pstmt.setLong(2, timestamp);
        pstmt.execute();
    }
}
