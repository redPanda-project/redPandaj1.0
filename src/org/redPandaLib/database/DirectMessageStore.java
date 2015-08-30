/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redPandaLib.database;

import crypt.Utils;
import java.io.UnsupportedEncodingException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.redPandaLib.Main;
import org.redPandaLib.core.Channel;
import org.redPandaLib.core.Log;
import org.redPandaLib.core.Settings;
import org.redPandaLib.core.Test;
import org.redPandaLib.core.messages.BlockMsg;
import org.redPandaLib.core.messages.RawMsg;
import org.redPandaLib.core.messages.TextMessageContent;
import org.redPandaLib.crypt.ECKey;

/**
 *
 * @author rflohr
 */
public class DirectMessageStore implements MessageStore {

    public Connection connection;
    private Integer messageCount = Integer.MIN_VALUE;
    private boolean resetMessageCount = true;
    private final ReentrantLock messageCountLock = new ReentrantLock();
    public static final int DATABASE_VERSION = 1;

    public DirectMessageStore(Connection connection) throws SQLException {
        this.connection = connection;
        connection.setAutoCommit(true);
        //connection = new HsqlConnection().getConnection();
    }

    public int containsMsg(RawMsg msg) {
        try {
            int pubkeyIdWithInsert = getPubkeyIdWithInsert(connection, msg.getKey().getPubKey());
            int messageId = getMessageId(connection, pubkeyIdWithInsert, msg.public_type, msg.timestamp, msg.nonce);

            return messageId;

        } catch (SQLException ex) {
            Logger.getLogger(DirectMessageStore.class.getName()).log(Level.SEVERE, null, ex);
        }

        return -1;
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
            boolean execute = pstmt.execute();
            boolean tryLock = messageCountLock.tryLock();
            if (!tryLock) {
                resetMessageCount = true;
            } else {
                messageCount++;
                messageCountLock.unlock();
            }

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
            PreparedStatement prepareStatement = connection.prepareStatement("SHUTDOWN");
            prepareStatement.execute();
            connection.close();
        } catch (SQLException ex) {
            Logger.getLogger(DirectMessageStore.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    /**
     * LIMIT 100 - self check if we have to call this method again!
     *
     * @param from
     * @param to
     * @param peer_id
     * @return
     */
    @Override
    public ResultSet getAllMessagesForSync(long from, long to, long peer_id) {
//        ArrayList<RawMsg> list = new ArrayList<RawMsg>();

        try {
            //get Key Id
            //String query = "SELECT message_id,pubkey.pubkey_id, pubkey,public_type,timestamp,nonce,signature,content,verified from message left join pubkey on (pubkey.pubkey_id = message.pubkey_id) WHERE timestamp > ? order by timestamp asc";
            String query = "SELECT message.message_id,pubkey.pubkey_id, pubkey,public_type,timestamp,nonce,signature,verified from haveToSendMessageToPeer left join message on (haveToSendMessageToPeer.message_id = message.message_id) left join pubkey on (message.pubkey_id = pubkey.pubkey_id) WHERE timestamp > ? AND peer_id = ? order by timestamp asc LIMIT 100";
            PreparedStatement pstmt = connection.prepareStatement(query);
            //pstmt.setFetchSize(100);
            pstmt.setLong(1, from);
            pstmt.setLong(2, peer_id);
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

    /**
     * Checks also for containig...
     *
     * @param pubkey_id
     * @param message_type
     * @param timestamp
     * @param decryptedContent
     * @param identity
     * @param fromMe
     * @param nonce
     * @param public_type
     */
    @Override
    public boolean addDecryptedContent(int pubkey_id, int message_type, long timestamp, byte[] decryptedContent, long identity, boolean fromMe, int nonce, byte public_type) {
        try {
            String query = "SELECT message_id from channelmessage WHERE pubkey_id = ? AND timestamp = ? AND nonce = ? and public_type = ?";
            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setInt(1, pubkey_id);
            pstmt.setLong(2, timestamp);
            pstmt.setInt(3, nonce);
            pstmt.setByte(4, public_type);
            ResultSet executeQuery = pstmt.executeQuery();
            boolean next = executeQuery.next();
            pstmt.close();

            if (next) {
                //System.out.println("message already in db!");
                return false;
            }

            System.out.println("message added ! #######################################");
            
            //channelmessage (channel_id INTEGER, message_id INTEGER, message_type INTEGER, decryptedContent LONGVARBINARY);
            query = "INSERT into channelmessage (pubkey_id,message_type,timestamp,decryptedContent,identity,fromMe,nonce,public_type) VALUES (?,?,?,?,?,?,?,?)";
            pstmt = connection.prepareStatement(query);
            pstmt.setInt(1, pubkey_id);
            pstmt.setInt(2, message_type);
            pstmt.setLong(3, timestamp);
            pstmt.setBytes(4, decryptedContent);
            pstmt.setLong(5, identity);
            pstmt.setBoolean(6, fromMe);
            pstmt.setInt(7, nonce);
            pstmt.setByte(8, public_type);
            pstmt.execute();
            pstmt.close();
            return true;
        } catch (Throwable ex) {
            ex.printStackTrace();
            Logger.getLogger(DirectMessageStore.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;

    }

    public void addDecryptedContent(int pubkey_id, int message_id, int message_type, long timestamp, byte[] decryptedContent, long identity, boolean fromMe, int nonce, byte public_type) {
        try {
            //channelmessage (channel_id INTEGER, message_id INTEGER, message_type INTEGER, decryptedContent LONGVARBINARY);
            String query = "INSERT into channelmessage (pubkey_id,message_id,message_type,timestamp,decryptedContent,identity,fromMe,nonce,public_type) VALUES (?,?,?,?,?,?,?,?,?)";
            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setInt(1, pubkey_id);
            pstmt.setInt(2, message_id);
            pstmt.setInt(3, message_type);
            pstmt.setLong(4, timestamp);
            pstmt.setBytes(5, decryptedContent);
            pstmt.setLong(6, identity);
            pstmt.setBoolean(7, fromMe);
            pstmt.setInt(8, nonce);
            pstmt.setByte(9, public_type);
            pstmt.execute();
            pstmt.close();
        } catch (Throwable ex) {
            ex.printStackTrace();

            //To dangerous?
////            if (ex instanceof java.sql.SQLIntegrityConstraintViolationException) {
////                System.out.println("DATABASE IS WRONG.... suggesting an old version, create hole new database and erase all data");
////
////                try {
////                    Statement createStatement = connection.createStatement();
////                    HsqlConnection.dropAllTables(createStatement);
////                } catch (Throwable ex2) {
////                    ex2.printStackTrace();
////                }
////                //restart!!
////                System.exit(0);
////
////            }

        }

    }

    /**
     * perhaps removed from and to in query for testing...
     *
     * @param pubKey
     * @param from
     * @param to
     * @return
     */
    public ArrayList<TextMessageContent> getMessageContentsForPubkey(byte[] pubKey, long from, long to) {

        ECKey ecKey = new ECKey(null, pubKey);
        int pubkeyId = getPubkeyId(ecKey);

        ArrayList<TextMessageContent> list = new ArrayList<TextMessageContent>();
        Channel instanceByPublicKey = Channel.getInstanceByPublicKey(pubKey);

        try {
            //get Key Id
            //String query = "(SELECT message_id,message_type,decryptedContent,timestamp,identity,fromMe,nonce from channelmessage LEFT JOIN message on (channelmessage.message_id = message.message_id) WHERE pubkey_id =? AND timestamp < ? AND timestamp > ?) "
            //        + "UNION (SELECT message_id,message_type,decryptedContent,timestamp,identity,fromMe,nonce from channelmessage LEFT JOIN message on (channelmessage.message_id = message.message_id) WHERE pubkey_id =? AND timestamp < ? ORDER BY message_type, timestamp LIMIT 200) ORDER BY timestamp";

            //String query = "SELECT message_id,message_type,decryptedContent,timestamp,identity,fromMe,nonce from channelmessage LEFT JOIN message on (channelmessage.message_id = message.message_id) WHERE pubkey_id =? AND timestamp < ? AND timestamp > ? ORDER BY timestamp";
            //String query = "SELECT message_id,message_type,decryptedContent,channelmessage.timestamp,identity,fromMe,nonce from channelmessage LEFT JOIN message on (channelmessage.message_id = message.message_id) WHERE pubkey_id =? AND timestamp > 0 ORDER BY timestamp DESC";
//WARNING added timestamp to channelmessage - old querys have to be edited!!!!
            //String query = "SELECT message_id,message_type,timestamp,decryptedContent,identity,fromMe from channelmessage WHERE pubkey_id =? AND timestamp > 0 ORDER BY timestamp DESC";
            String query = "SELECT channelmessage.message_id,message_type,timestamp,decryptedContent,identity,fromMe, (notReadMessage.message_id is NULL) as markedAsRead from channelmessage LEFT JOIN notReadMessage on (channelmessage.message_id = notReadMessage.message_id) WHERE pubkey_id =? AND timestamp > ? ORDER BY timestamp DESC";
            //System.out.println("QUERY: " + query);

            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setInt(1, pubkeyId);
            pstmt.setLong(2, from);
            //pstmt.setLong(2, to);

            //pstmt.setInt(4, pubkeyId);
            //pstmt.setLong(5, to);
            pstmt.setMaxRows(1000);

            //System.out.println("STM: " + pstmt.toString());
            ResultSet executeQuery = pstmt.executeQuery();

            //System.out.println("kndkjwhd");
            //System.out.println("reading data...");
            while (executeQuery.next()) {
                int message_id = executeQuery.getInt("message_id");
                int message_type = executeQuery.getInt("message_type");
                byte[] decryptedContent = executeQuery.getBytes("decryptedContent");
                long timestamp = executeQuery.getLong("timestamp");
                long identity = executeQuery.getLong("identity");
                boolean fromMe = executeQuery.getBoolean("fromMe");
                boolean read = executeQuery.getBoolean("markedAsRead");
                //long nonce = executeQuery.getLong("nonce");
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
                textMessageContent.read = read;

                //System.out.println("MSG!! " + message_id);
                list.add(textMessageContent);
                //System.out.println("added");
            }
            executeQuery.close();

            //System.out.println("SIZE: " + list.size());
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

        Collections.reverse(list);
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
        messageCountLock.lock();
        if (!resetMessageCount) {
            int tempMessageCount = messageCount;
            messageCountLock.unlock();
            return tempMessageCount;
        }
        messageCountLock.unlock();
        try {
            String query = "SELECT count(*) from message";
            //stmt.executeQuery("SELECT id,key from pubkey WHERE key EQUASLS "+ msg.getKey().getPubKey())
            Statement createStatement = connection.createStatement();
            ResultSet executeQuery = createStatement.executeQuery(query);

            executeQuery.next();
            int aInt = executeQuery.getInt(1);
            executeQuery.close();
            createStatement.close();

            messageCountLock.lock();
            messageCount = aInt;
            messageCountLock.unlock();
            resetMessageCount = false;

            return aInt;
        } catch (SQLException ex) {
            Logger.getLogger(DirectMessageStore.class.getName()).log(Level.SEVERE, null, ex);
        }

        return -1;
    }

    @Override
    public int getMessageCountToVerify() {
        try {
            String query = "SELECT count(message_id) from message WHERE verified = 0";
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

    @Override
    /**
     * removes all messages with given pubkey and public_type which are older
     * than timestamp
     */
    public int removeMessagesFromChannel(int pubkey_id, byte public_type, long timestamp) {

        if (Settings.DONT_REMOVE_UNUSED_MESSAGES) {
            return -1;
        }

        int updateCount = 0;

        try {
            //get Key Id
            //String query = "SELECT message_id from message WHERE pubkey_id = ? AND public_type = ? AND timestamp = ? AND nonce = ?";
            String query = "DELETE FROM message WHERE pubkey_id = ? AND public_type = ? AND timestamp < ?";
            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setInt(1, pubkey_id);
            pstmt.setByte(2, public_type);
            pstmt.setLong(3, timestamp);
            pstmt.execute();
            updateCount = pstmt.getUpdateCount();
            pstmt.close();
            resetMessageCounter();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return updateCount;
    }

    public void removeOldMessages(long timestamp) {
        try {
            //get Key Id
            //String query = "SELECT message_id from message WHERE pubkey_id = ? AND public_type = ? AND timestamp = ? AND nonce = ?";
            String query = "DELETE FROM message WHERE timestamp < ?";
            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setLong(1, timestamp);
            pstmt.execute();
            resetMessageCounter();
        } catch (SQLException ex) {
            Logger.getLogger(DirectMessageStore.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void removeOldMessagesDecryptedContent(long timestamp) {
        try {
            //get Key Id
            //String query = "SELECT message_id from message WHERE pubkey_id = ? AND public_type = ? AND timestamp = ? AND nonce = ?";
            String query = "DELETE FROM channelmessage WHERE timestamp < ?";
            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setLong(1, timestamp);
            pstmt.execute();
        } catch (SQLException ex) {
            Logger.getLogger(DirectMessageStore.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void removeMessagesDecryptedContent(Channel channel) {
        removeMessagesDecryptedContent(getPubkeyId(channel.getKey()));
    }

    public void removeMessagesDecryptedContent(int pubkey_id) {
        try {
            //get Key Id
            //String query = "SELECT message_id from message WHERE pubkey_id = ? AND public_type = ? AND timestamp = ? AND nonce = ?";
            String query = "DELETE FROM channelmessage WHERE pubkey_id = ?";
            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setLong(1, pubkey_id);
            pstmt.execute();
        } catch (SQLException ex) {
            Logger.getLogger(DirectMessageStore.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void checkpoint() {
        try {
            Statement stmt = getConnection().createStatement();
            stmt.execute("CHECKPOINT");
        } catch (SQLException ex) {
            Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void addStick(int pubkey_id, int message_id, double difficulty, long validTill) {
        try {
            //channelmessage (channel_id INTEGER, message_id INTEGER, message_type INTEGER, decryptedContent LONGVARBINARY);
            String query = "INSERT into sticks (pubkey_id,message_id,difficulty,validTill) VALUES (?,?,?,?)";
            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setInt(1, pubkey_id);
            pstmt.setInt(2, message_id);
            pstmt.setDouble(3, difficulty);
            pstmt.setLong(4, validTill);
            pstmt.execute();
            pstmt.close();
        } catch (SQLException ex) {
            Logger.getLogger(DirectMessageStore.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private boolean msgIntroducedToMe(long peer_id, int message_id) throws SQLException {
        //get Key Id
        String query = "SELECT peer_id from peerMessagesIntroducedToMe WHERE peer_id = ? AND message_id = ?";
        PreparedStatement pstmt = connection.prepareStatement(query);
        pstmt.setLong(1, peer_id);
        pstmt.setInt(2, message_id);
        ResultSet executeQuery = pstmt.executeQuery();

        if (!executeQuery.next()) {
            executeQuery.close();
            pstmt.close();
            return false;
        }
        executeQuery.close();
        pstmt.close();
        return true;

    }

    public int msgCountIntroducedToHim(long peer_id) {
        try {
            //get Key Id
            String query = "SELECT count(message_id) from peerMessagesIntroducedToHim WHERE peer_id = ?";
            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setLong(1, peer_id);
            ResultSet executeQuery = pstmt.executeQuery();

            executeQuery.next();
            int aInt = executeQuery.getInt(1);
            executeQuery.close();
            pstmt.close();

            return aInt;

        } catch (SQLException ex) {
            Logger.getLogger(DirectMessageStore.class.getName()).log(Level.SEVERE, null, ex);
        }
        return -1;
    }

    public int msgCountIntroducedToMe(long peer_id) {
        try {
            //get Key Id
            String query = "SELECT count(message_id) from peerMessagesIntroducedToMe WHERE peer_id = ?";
            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setLong(1, peer_id);
            ResultSet executeQuery = pstmt.executeQuery();

            executeQuery.next();
            int aInt = executeQuery.getInt(1);
            executeQuery.close();
            pstmt.close();

            return aInt;

        } catch (SQLException ex) {
            Logger.getLogger(DirectMessageStore.class.getName()).log(Level.SEVERE, null, ex);
        }
        return -1;
    }

    public void addMsgIntroducedToMe(long peer_id, int message_id) {

        try {
            if (msgIntroducedToMe(peer_id, message_id)) {
                return;
            }
        } catch (SQLException ex) {
            Logger.getLogger(DirectMessageStore.class.getName()).log(Level.SEVERE, null, ex);
        }

        try {
            String query = "INSERT into peerMessagesIntroducedToMe (peer_id,message_id) VALUES (?,?)";
            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setLong(1, peer_id);
            pstmt.setInt(2, message_id);
            pstmt.execute();
            pstmt.close();
        } catch (SQLException ex) {
            Logger.getLogger(DirectMessageStore.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private boolean msgIntroducedToHim(long peer_id, int message_id) throws SQLException {
        //get Key Id
        String query = "SELECT peer_id from peerMessagesIntroducedToHim WHERE peer_id = ? AND message_id = ?";
        PreparedStatement pstmt = connection.prepareStatement(query);
        pstmt.setLong(1, peer_id);
        pstmt.setInt(2, message_id);
        ResultSet executeQuery = pstmt.executeQuery();

        if (!executeQuery.next()) {
            executeQuery.close();
            pstmt.close();
            return false;
        }

        executeQuery.close();
        pstmt.close();
        return true;

    }

    public void addMsgIntroducedToHim(long peer_id, int message_id) {

        try {
            if (msgIntroducedToHim(peer_id, message_id)) {
                return;
            }
        } catch (SQLException ex) {
            Logger.getLogger(DirectMessageStore.class.getName()).log(Level.SEVERE, null, ex);
        }

        try {
            String query = "INSERT into peerMessagesIntroducedToHim (peer_id,message_id) VALUES (?,?)";
            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setLong(1, peer_id);
            pstmt.setInt(2, message_id);
            pstmt.execute();
            pstmt.close();
        } catch (SQLException ex) {
            Logger.getLogger(DirectMessageStore.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public int msgsToUser(long peer_id, long from) {
        try {
            //get Key Id
            String query = "SELECT count(message_id) from message WHERE timestamp > ? AND message_id NOT IN (SELECT message_id from peerMessagesIntroducedToHim WHERE peer_id = ?)";
            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setLong(1, from);
            pstmt.setLong(2, peer_id);
            ResultSet executeQuery = pstmt.executeQuery();

            executeQuery.next();
            int aInt = executeQuery.getInt(1);
            executeQuery.close();
            pstmt.close();

            return aInt;

        } catch (SQLException ex) {
            Logger.getLogger(DirectMessageStore.class.getName()).log(Level.SEVERE, null, ex);
        }
        return -1;
    }

    private boolean isFilteringAddress(long peer_id, int channel_id) throws SQLException {
        //get Key Id
        String query = "SELECT peer_id from filterChannels WHERE peer_id = ? AND channel_id = ?";
        PreparedStatement pstmt = connection.prepareStatement(query);
        pstmt.setLong(1, peer_id);
        pstmt.setInt(2, channel_id);
        ResultSet executeQuery = pstmt.executeQuery();

        if (!executeQuery.next()) {
            executeQuery.close();
            pstmt.close();
            return false;
        }

        executeQuery.close();
        pstmt.close();
        return true;

    }

    @Override
    public void addFilterChannel(long peer_id, int channel_id) {

        try {
            if (isFilteringAddress(peer_id, channel_id)) {
                //System.out.println("schon drin!");
                return;
            }
        } catch (SQLException ex) {
            Logger.getLogger(DirectMessageStore.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }

        try {
            String query = "INSERT into filterChannels (peer_id,channel_id) VALUES (?,?)";
            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setLong(1, peer_id);
            pstmt.setInt(2, channel_id);
            pstmt.execute();
            pstmt.close();
        } catch (SQLException ex) {
            Logger.getLogger(DirectMessageStore.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void delFilterChannel(long peer_id, int channel_id) {
        try {
            String query = "DELETE from filterChannels WHERE peer_id = ? AND channel_id = ?";
            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setLong(1, peer_id);
            pstmt.setInt(2, channel_id);
            pstmt.execute();
            pstmt.close();

            //-1 will be removed every init to ensure switch from full node to light node is working properly
            if (channel_id != -1) {
                //ToDo: improve to just remove the right messages, but that may be used to attack a node.
                query = "DELETE from haveToSendMessageToPeer WHERE peer_id = ?";
                pstmt = connection.prepareStatement(query);
                pstmt.setLong(1, peer_id);
                pstmt.execute();
                pstmt.close();
            }
        } catch (SQLException ex) {
            Logger.getLogger(DirectMessageStore.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void addMessageToSend(int message_id, int channel_id) {

        Log.put("ADDE MSG _ : " + message_id, 50);

        try {
            String query = "INSERT into haveToSendMessageToPeer (message_id,peer_id) SELECT ? as message_id,peer_id FROM filterChannels WHERE channel_id = ? OR channel_id = -1 group by peer_id";
            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setInt(1, message_id);
            pstmt.setInt(2, channel_id);
            pstmt.execute();
            pstmt.close();
        } catch (SQLException ex) {
            Logger.getLogger(DirectMessageStore.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void addMessageToSendToSpecificPeer(int message_id, int peer_id) {

        Log.put("ADDE MSG _ : " + message_id, 50);

        try {
            String query = "INSERT into haveToSendMessageToPeer (message_id,peer_id) VALUES (?,?)";
            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setInt(1, message_id);
            pstmt.setInt(2, peer_id);
            pstmt.execute();
            pstmt.close();
        } catch (SQLException ex) {
            Logger.getLogger(DirectMessageStore.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public boolean removeMessageToSend(long peer_id, int message_id) {
        try {
            String query = "DELETE from haveToSendMessageToPeer WHERE peer_id = ? AND message_id = ?";
            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setLong(1, peer_id);
            pstmt.setInt(2, message_id);
            pstmt.execute();
            int updateCount = pstmt.getUpdateCount();
            pstmt.close();

            return (updateCount > 0);

        } catch (SQLException ex) {
            Logger.getLogger(DirectMessageStore.class.getName()).log(Level.SEVERE, null, ex);
        }

        return false;
    }

    public boolean removeMessageToSend(long peer_id) {
        try {
            String query = "DELETE from haveToSendMessageToPeer WHERE peer_id = ?";
            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setLong(1, peer_id);
            pstmt.execute();
            int updateCount = pstmt.getUpdateCount();
            pstmt.close();

            return (updateCount > 0);

        } catch (SQLException ex) {
            Logger.getLogger(DirectMessageStore.class.getName()).log(Level.SEVERE, null, ex);
        }

        return false;
    }

    @Override
    public ResultSet getMessagesForBackSync(long time, int cnt) {
//        ArrayList<RawMsg> list = new ArrayList<RawMsg>();

        try {
            //get Key Id
            //String query = "SELECT message_id,pubkey.pubkey_id, pubkey,public_type,timestamp,nonce,signature,content,verified from message left join pubkey on (pubkey.pubkey_id = message.pubkey_id) WHERE timestamp > ? order by timestamp asc";
            String query = "SELECT message_id,pubkey.pubkey_id, pubkey,public_type,timestamp,nonce,signature,content,verified from message left join pubkey on (message.pubkey_id = pubkey.pubkey_id) WHERE timestamp < ? order by timestamp DESC LIMIT ?";
            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setLong(1, time);
            pstmt.setInt(2, cnt);
            ResultSet executeQuery = pstmt.executeQuery();

            return executeQuery;

//            while (executeQuery.
        } catch (SQLException ex) {
            Logger.getLogger(DirectMessageStore.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }

    @Override
    public void addUnreadMessage(long message_id) {
        try {
            //channelmessage (channel_id INTEGER, message_id INTEGER, message_type INTEGER, decryptedContent LONGVARBINARY);
            String query = "INSERT into notReadMessage (message_id) VALUES (?)";
            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setLong(1, message_id);
            pstmt.execute();
            pstmt.close();
        } catch (SQLException ex) {
            Logger.getLogger(DirectMessageStore.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    @Override
    public void markAsRead(long message_id) {
        try {
            //channelmessage (channel_id INTEGER, message_id INTEGER, message_type INTEGER, decryptedContent LONGVARBINARY);
            String query = "DELETE from notReadMessage WHERE message_id = ?";
            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setLong(1, message_id);
            pstmt.execute();
            pstmt.close();
        } catch (SQLException ex) {
            Logger.getLogger(DirectMessageStore.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    @Override
    public void resetMessageCounter() {
        resetMessageCount = true;
    }

    @Override
    public void clearFilterChannel(long peer_id) {
        try {
            String query = "DELETE from filterChannels WHERE peer_id = ?";
            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setLong(1, peer_id);
            pstmt.execute();
            pstmt.close();
        } catch (SQLException ex) {
            Logger.getLogger(DirectMessageStore.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void addKnownChannel(int forChannel, long identity, int fromChannel, int level) {
        try {
            String query = "DELETE from channelKnownLevel WHERE forChannel = ? AND identity = ? AND fromChannel = ?";
            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setInt(1, forChannel);
            pstmt.setLong(2, identity);
            pstmt.setInt(3, fromChannel);
            pstmt.execute();
            pstmt.close();

            query = "INSERT into channelKnownLevel (forChannel,identity,fromChannel,level) VALUES (?,?,?,?)";
            pstmt = connection.prepareStatement(query);
            pstmt.setInt(1, forChannel);
            pstmt.setLong(2, identity);
            pstmt.setInt(3, fromChannel);
            pstmt.setInt(4, level);
            pstmt.execute();
            pstmt.close();
        } catch (SQLException ex) {
            Logger.getLogger(DirectMessageStore.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void removeKnownChannelFromIdenity(long identity) {
        try {
            String query = "DELETE from channelKnownLevel WHERE identity = ?";
            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setLong(1, identity);
            pstmt.execute();
            pstmt.close();
        } catch (SQLException ex) {
            Logger.getLogger(DirectMessageStore.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void removeKnownChannelForCHannel(int channel_id) {
        try {
            String query = "DELETE from channelKnownLevel WHERE forChannel = ?";
            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setInt(1, channel_id);
            pstmt.execute();
            pstmt.close();
        } catch (SQLException ex) {
            Logger.getLogger(DirectMessageStore.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public HashMap<ECKey, Integer> getAllKnownChannels() {

        HashMap<ECKey, Integer> list = new HashMap<ECKey, Integer>();

        try {
            //get Key Id
            String query = "SELECT forChannel,MIN(level) as level FROM channelKnownLevel group by forChannel";
            PreparedStatement pstmt = connection.prepareStatement(query);
            ResultSet executeQuery = pstmt.executeQuery();

            boolean errorAlreadySend = false;

            while (executeQuery.next()) {
                int forChannel = executeQuery.getInt("forChannel");
                int level = executeQuery.getByte("level");

                byte[] channelBytes = getPubkeyById(connection, forChannel);

                if (channelBytes == null) {
                    if (!errorAlreadySend) {
                        removeKnownChannelForCHannel(forChannel);
                        Main.sendBroadCastMsg("Pubkey id wasnt in database - removed?!! 94624");
                        errorAlreadySend = true;
                    }
                    continue;
                }

                System.out.println("len: " + channelBytes.length);

                ECKey key = new ECKey(null, channelBytes);

                list.put(key, level);
            }

            executeQuery.close();
            pstmt.close();
        } catch (SQLException ex) {
            Logger.getLogger(DirectMessageStore.class.getName()).log(Level.SEVERE, null, ex);
        }
        return list;
    }

    public void moveChannelMessagesToHistory(long olderThan) {
        try {

            String query = "INSERT INTO channelmessageHistory (pubkey_id,message_id,message_type,timestamp,decryptedContent,identity,fromMe) "
                    + "SELECT pubkey_id,message_id,message_type,timestamp,decryptedContent,identity,fromMe FROM channelmessage WHERE timestamp < ?";
            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setLong(1, olderThan);
            pstmt.execute();

            query = "DELETE FROM channelmessage WHERE timestamp < ?";
            pstmt = connection.prepareStatement(query);
            pstmt.setLong(1, olderThan);
            pstmt.execute();

            pstmt.close();
        } catch (SQLException ex) {
            Logger.getLogger(DirectMessageStore.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public Long getLatestBlocktime(int pubkeyId) {

        long timestamp = -1;

        try {

            String query = "SELECT timestamp from message WHERE pubkey_id =? AND public_type = ? ORDER BY timestamp DESC";

            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setInt(1, pubkeyId);
            pstmt.setLong(2, BlockMsg.PUBLIC_TYPE);
            ResultSet executeQuery = pstmt.executeQuery();

            while (executeQuery.next()) {
                timestamp = executeQuery.getLong("timestamp");
            }
            executeQuery.close();
            pstmt.close();

        } catch (SQLException ex) {
            Test.sendStacktrace(ex);
        }
        return timestamp;
    }

}
