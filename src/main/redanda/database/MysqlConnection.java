package main.redanda.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLInvalidAuthorizationSpecException;
import java.sql.SQLSyntaxErrorException;
import java.sql.Statement;

public class MysqlConnection {

    public static String db_file = "data/messages";
    private Connection con = null;
    public String path = "";
    private String databaseName;
    private String user;
    private String password;

    public MysqlConnection(String databaseName, String user, String password) throws SQLException {
        this.databaseName = databaseName;
        this.user = user;
        this.password = password;
        if (initConnection()) {
            return;
        }
    }

//    public void reconnect() {
//
//        new Thread() {
//
//            @Override
//            public void run() {
//                try {
//                    System.out.println("CLOSING");
//                    //con.closeFully();
//                    //con.createStatement().execute("SHUTDOWN");
//                    con.abort(Executors.newFixedThreadPool(1));
//                    System.out.println("CLOSED");
//                } catch (SQLException ex) {
//                    ex.printStackTrace();
//                }
//                try {
//                    System.out.println("INIT GLEICH");
//                    initConnection();
//                    Test.messageStore = new DirectMessageStore(con);
//                    System.out.println("INIT FINISH");
//                } catch (SQLException ex) {
//                    ex.printStackTrace();
//                }
//            }
//        }.start();
//
//    }
    private boolean initConnection() throws SQLException {
        try {
            // Treiberklasse laden
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("No mysql jdbc driver found.");
        }
        con = null;
        try {
            con = DriverManager.getConnection(
                    "jdbc:mysql://localhost/" + databaseName, user, password);
        } catch (SQLInvalidAuthorizationSpecException e) {
            //mega hack
//            String[] exts = {".data", ".script", ".properties", ".log"};
//            for (String extension : exts) {
//                File f = new File(path + db_file + extension);
//                f.delete();
//            }
//            System.out.println("REMOVE DATABASE FILE");
            con = DriverManager.getConnection(
                    "jdbc:mysql://localhost/" + databaseName, user, password);
        }

        Statement stmt = con.createStatement();

        //            PubKey
        //id INTEGER
        //key BINARY(33)
        if (false) {
            stmt.executeUpdate("drop table if exists channelmessage");
            stmt.executeUpdate("drop table if exists message");
            stmt.executeUpdate("drop table if exists channel");
            stmt.executeUpdate("drop table if exists pubkey");
        }

        stmt.executeUpdate("drop table if exists peerMessagesIntroducedToMe");
        stmt.executeUpdate("drop table if exists peerMessagesIntroducedToHim");
        stmt.executeUpdate("create  table if not exists pubkey (pubkey_id integer PRIMARY KEY AUTO_INCREMENT, pubkey BINARY(33) UNIQUE)");
        stmt.executeUpdate("create  table if not exists channel (channel_id integer PRIMARY KEY AUTO_INCREMENT, pubkey_id INTEGER UNIQUE, private_key BINARY(32) UNIQUE, name MEDIUMBLOB)");

        stmt.executeUpdate("create  table if not exists message (message_id INTEGER PRIMARY KEY AUTO_INCREMENT, pubkey_id INTEGER, public_type TINYINT, timestamp BIGINT, nonce INTEGER,  signature BINARY(72), content MEDIUMBLOB, verified boolean)");
        stmt.executeUpdate("create  table if not exists channelmessage (pubkey_id INTEGER, message_id INTEGER PRIMARY KEY AUTO_INCREMENT, message_type INTEGER, public_type TINYINT, timestamp BIGINT, nonce INTEGER, decryptedContent MEDIUMBLOB, identity BIGINT, fromMe BOOLEAN, FOREIGN KEY (pubkey_id) REFERENCES pubkey(pubkey_id))");

        stmt.executeUpdate("create  table if not exists sticks (pubkey_id INTEGER, message_id INTEGER, difficulty DOUBLE, validTill BIGINT, FOREIGN KEY (pubkey_id) REFERENCES pubkey(pubkey_id))");
        stmt.executeUpdate("create  table if not exists peerMessagesIntroducedToMe (peer_id BIGINT, message_id INTEGER)");
        stmt.executeUpdate("create  table if not exists peerMessagesIntroducedToHim (peer_id BIGINT, message_id INTEGER, FOREIGN KEY (message_id) REFERENCES message(message_id) ON DELETE CASCADE)");
        stmt.executeUpdate("create  table if not exists haveToSendMessageToPeer (peer_id BIGINT, message_id INTEGER, FOREIGN KEY (message_id) REFERENCES message(message_id) ON DELETE CASCADE)");
        stmt.executeUpdate("create  table if not exists filterChannels (peer_id BIGINT, channel_id INTEGER)");//, FOREIGN KEY (channel_id) REFERENCES channel(channel_id) ON DELETE CASCADE

        stmt.executeUpdate("create  table if not exists notReadMessage (message_id INTEGER, FOREIGN KEY (message_id) REFERENCES channelmessage(message_id) ON DELETE CASCADE)");

        stmt.executeUpdate("create  table if not exists channelKnownLevel (forChannel INTEGER, identity BIGINT, fromChannel INTEGER, level INTEGER)");

        stmt.executeUpdate("create  table if not exists peerConnectionInformation (ip VARCHAR(254), port INTEGER, status INTEGER, avoidUntil BIGINT)");

        stmt.executeUpdate("CREATE TABLE if not exists msgcounter (id INTEGER not null primary key)");
        ResultSet executeQuery = stmt.executeQuery("SELECT * FROM msgcounter");
        if (!executeQuery.next()) {//only insert if empty
            stmt.executeUpdate("INSERT INTO msgcounter VALUES (1)");
        }
        executeQuery.close();
        stmt.executeUpdate("CREATE TABLE if not exists msgcounterchannel (id INTEGER not null primary key)");
        executeQuery = stmt.executeQuery("SELECT * FROM msgcounterchannel");
        if (!executeQuery.next()) {//only insert if empty
            stmt.executeUpdate("INSERT INTO msgcounterchannel VALUES (2)");
        }
        executeQuery.close();

        //        ResultSet executeQuery = stmt.executeQuery("SELECT * FROM information_schema.statistics");
        //
        //
        //        System.out.println("d3uwne3quzne " + executeQuery.getFetchSize());
        //        executeQuery.close();
        //        stmt.executeUpdate("create CACHED table if not exists syncHash (channel_id integer, from BIGINT, to BIGINT, count INTEGER, hashcode INTEGER)");
        String[] keys = {"ip", "status", "avoidUntil"};
        String tableName = "peerConnectionInformation";
        for (String key : keys) {
            try {
                stmt.executeUpdate("CREATE INDEX " + tableName + key + "Index ON " + tableName + "(" + key + ")");
            } catch (SQLSyntaxErrorException e) {
            }
        }

        try {
            stmt.executeUpdate("CREATE INDEX messagePubkeyIndex ON message(pubkey_id)");
        } catch (SQLSyntaxErrorException e) {
        }
        try {
            stmt.executeUpdate("CREATE INDEX messageTimestampIndex ON message(timestamp)");
        } catch (SQLSyntaxErrorException e) {
        }
        try {
            stmt.executeUpdate("CREATE INDEX messageNonceIndex ON message(nonce)");
        } catch (SQLSyntaxErrorException e) {
        }
        try {
            stmt.executeUpdate("CREATE INDEX messageMsgIdIndex ON message(message_id)");
        } catch (SQLSyntaxErrorException e) {
        }
        try {
            stmt.executeUpdate("CREATE INDEX messageVerifiedIndex ON message(verified)");
        } catch (SQLSyntaxErrorException e) {
        }

        try {
            stmt.executeUpdate("CREATE INDEX peerMessagesIntroducedToMeIndex ON peerMessagesIntroducedToMe(peer_id,message_id)");
        } catch (SQLSyntaxErrorException e) {
        }
        try {
            stmt.executeUpdate("CREATE INDEX peerMessagesIntroducedToHimIndex ON peerMessagesIntroducedToHim(peer_id,message_id)");
        } catch (SQLSyntaxErrorException e) {
        }
        try {
            stmt.executeUpdate("CREATE INDEX peerMessagesIntroducedToHimIndexForMsgId ON peerMessagesIntroducedToHim(message_id)");
        } catch (SQLSyntaxErrorException e) {
        }
        String[] keys2 = {"pubkey_id", "message_type", "message_id", "timestamp"};
        String tableName2 = "channelmessage";
        for (String key : keys2) {
            try {
                stmt.executeUpdate("CREATE INDEX " + tableName2 + key + "Index ON " + tableName2 + "(" + key + ")");
            } catch (SQLSyntaxErrorException e) {
            }
        }
//        try {
//            stmt.executeUpdate("CREATE INDEX syncHashchannel_idIndex ON syncHash(channel_id)");
//        } catch (SQLSyntaxErrorException e) {
//        }

        try {
            stmt.executeUpdate("CREATE INDEX haveToSendMessageToPeerPeerIdIndex ON haveToSendMessageToPeer(peer_id)");
        } catch (SQLSyntaxErrorException e) {
        }

//////        //stmt.executeUpdate("drop table if exists filterChannels");
//////        //stmt.executeUpdate("drop table if exists haveToSendMessageToPeer");
//////        stmt.executeUpdate("create table if not exists pubkey (pubkey_id integer PRIMARY KEY AUTO_INCREMENT, pubkey BINARY(33) UNIQUE)");
//////        //Channel
//////        //id INTEGER
//////        //pubkey_id INTEGER
//////        //private_key BINARY(32)
//////        //name LONGVARBINARY
//////        stmt.executeUpdate("create table if not exists channel (channel_id integer PRIMARY KEY AUTO_INCREMENT, pubkey_id INTEGER UNIQUE, private_key BINARY(32) UNIQUE, name MEDIUMBLOB)");
//////        //Message
//////        //id INTEGERstmt.executeUpdate("create  table if not exists message (message_id INTEGER GENERATED BY DEFAULT AS IDENTITY(START WITH 1, INCREMENT BY 2)PRIMARY KEY, pubkey_id INTEGER, public_type TINYINT, timestamp BIGINT, nonce INTEGER,  signature BINARY(72), content LONGVARBINARY, verified boolean)");
//////        //key_id INTEGER
//////        //channel_id integer
//////        //timestamp BIGINT
//////        //nonce INTEGER
//////        //signature BINARY(72)
//////        //content LONGVARBINARY
//////        //verified boolean
//////        //readable boolean
//////        //decrypted_content LONGVARBINARY
//////        //stmt.executeUpdate("drop table if exists message");
//////        stmt.executeUpdate("create  table if not exists message (message_id INTEGER GENERATED BY DEFAULT AS IDENTITY(START WITH 1, INCREMENT BY 2)PRIMARY KEY, pubkey_id INTEGER, public_type TINYINT, timestamp BIGINT, nonce INTEGER,  signature BINARY(72), content LONGVARBINARY, verified boolean)");
//////        stmt.executeUpdate("create  table if not exists message (message_id INTEGER PRIMARY KEY AUTO_INCREMENT, pubkey_id INTEGER, public_type TINYINT, timestamp BIGINT, nonce INTEGER,  signature BINARY(72), content MEDIUMBLOB, verified boolean)");
//////        stmt.executeUpdate("create  table if not exists channelmessage (pubkey_id INTEGER, message_id INTEGER PRIMARY KEY, message_type INTEGER, timestamp BIGINT,decryptedContent MEDIUMBLOB, identity BIGINT, fromMe BOOLEAN, FOREIGN KEY (pubkey_id) REFERENCES pubkey(pubkey_id))");
//////        stmt.executeUpdate("create  table if not exists channelmessageHistory (pubkey_id INTEGER, message_id INTEGER PRIMARY KEY, message_type INTEGER, timestamp BIGINT,decryptedContent MEDIUMBLOB, identity BIGINT, fromMe BOOLEAN, FOREIGN KEY (pubkey_id) REFERENCES pubkey(pubkey_id))");
//////        //table for sticks
//////        stmt.executeUpdate("create  table if not exists sticks (pubkey_id INTEGER, message_id INTEGER, difficulty DOUBLE, validTill BIGINT, FOREIGN KEY (pubkey_id) REFERENCES pubkey(pubkey_id))");
//////        stmt.executeUpdate("create  table if not exists peerMessagesIntroducedToMe (peer_id BIGINT, message_id INTEGER)");
//////        stmt.executeUpdate("create  table if not exists peerMessagesIntroducedToHim (peer_id BIGINT, message_id INTEGER, FOREIGN KEY (message_id) REFERENCES message(message_id) ON DELETE CASCADE)");
//////        stmt.executeUpdate("create  table if not exists haveToSendMessageToPeer (peer_id BIGINT, message_id INTEGER, FOREIGN KEY (message_id) REFERENCES message(message_id) ON DELETE CASCADE)");
//////        stmt.executeUpdate("create  table if not exists filterChannels (peer_id BIGINT, channel_id INTEGER)");//, FOREIGN KEY (channel_id) REFERENCES channel(channel_id) ON DELETE CASCADE
//////        stmt.executeUpdate("create  table if not exists notReadMessage (message_id INTEGER, FOREIGN KEY (message_id) REFERENCES channelmessage(message_id) ON DELETE CASCADE)");
//////        stmt.executeUpdate("create  table if not exists channelKnownLevel (forChannel INTEGER, identity BIGINT, fromChannel INTEGER, level INTEGER)");
//////        //        ResultSet executeQuery = stmt.executeQuery("SELECT * FROM information_schema.statistics");
//////        //
//////        //
//////        //        System.out.println("d3uwne3quzne " + executeQuery.getFetchSize());
//////        //        executeQuery.close();
//////        //        stmt.executeUpdate("create CACHED table if not exists syncHash (channel_id integer, from BIGINT, to BIGINT, count INTEGER, hashcode INTEGER)");
//////
//////        stmt.executeUpdate("CREATE TABLE msgcounter (id INTEGER not null primary key)");
//////        ResultSet executeQuery = stmt.executeQuery("SELECT * FROM msgcounter");
//////        if (!executeQuery.next()) {//only insert if empty
//////            stmt.executeUpdate("INSERT INTO msgcounter VALUES (1); # start from 1");
//////        }
//////
//////        try {
//////            stmt.executeUpdate("ALTER TABLE channelmessage ADD public_type TINYINT");
//////        } catch (SQLSyntaxErrorException e) {
//////        }
//////        try {
//////            stmt.executeUpdate("ALTER TABLE channelmessage ADD nonce INTEGER");
//////        } catch (SQLSyntaxErrorException e) {
//////        }
//////
//////        try {
//////            stmt.executeUpdate("ALTER TABLE channelmessageHistory ADD public_type TINYINT");
//////        } catch (SQLSyntaxErrorException e) {
//////        }
//////        try {
//////            stmt.executeUpdate("ALTER TABLE channelmessageHistory ADD nonce INTEGER");
//////        } catch (SQLSyntaxErrorException e) {
//////        }
//////
//////        try {
//////            stmt.executeUpdate("CREATE INDEX messagePubkeyIndex ON message(pubkey_id)");
//////        } catch (SQLSyntaxErrorException e) {
//////        }
//////        try {
//////            stmt.executeUpdate("CREATE INDEX messageTimestampIndex ON message(timestamp)");
//////        } catch (SQLSyntaxErrorException e) {
//////        }
//////        try {
//////            stmt.executeUpdate("CREATE INDEX messageNonceIndex ON message(nonce)");
//////        } catch (SQLSyntaxErrorException e) {
//////        }
//////        try {
//////            stmt.executeUpdate("CREATE INDEX messageMsgIdIndex ON message(message_id)");
//////        } catch (SQLSyntaxErrorException e) {
//////        }
//////        try {
//////            stmt.executeUpdate("CREATE INDEX messageVerifiedIndex ON message(verified)");
//////        } catch (SQLSyntaxErrorException e) {
//////        }
//////
//////        try {
//////            stmt.executeUpdate("CREATE INDEX peerMessagesIntroducedToMeIndex ON peerMessagesIntroducedToMe(peer_id,message_id)");
//////        } catch (SQLSyntaxErrorException e) {
//////        }
//////        try {
//////            stmt.executeUpdate("CREATE INDEX peerMessagesIntroducedToHimIndex ON peerMessagesIntroducedToHim(peer_id,message_id)");
//////        } catch (SQLSyntaxErrorException e) {
//////        }
//////        try {
//////            stmt.executeUpdate("CREATE INDEX peerMessagesIntroducedToHimIndexForMsgId ON peerMessagesIntroducedToHim(message_id)");
//////        } catch (SQLSyntaxErrorException e) {
//////        }
//////        String[] keys = {"pubkey_id", "message_type", "message_id", "timestamp"};
//////        String tableName = "channelmessage";
//////        for (String key : keys) {
//////            try {
//////                stmt.executeUpdate("CREATE INDEX " + tableName + key + "Index ON " + tableName + "(" + key + ")");
//////            } catch (SQLSyntaxErrorException e) {
//////            }
//////        }
//////
//////        try {
//////            stmt.executeUpdate("CREATE INDEX haveToSendMessageToPeerPeerIdIndex ON haveToSendMessageToPeer(peer_id)");
//////        } catch (SQLSyntaxErrorException e) {
//////        }
//        try {
//            stmt.executeUpdate("CREATE INDEX syncHashchannel_idIndex ON syncHash(channel_id)");
//        } catch (SQLSyntaxErrorException e) {
//        }
        //        try {
        //            stmt.executeUpdate("CREATE INDEX syncHashFromIndex ON syncHash(from)");
        //        } catch (SQLSyntaxErrorException e) {
        //        }
        //
        //        try {
        //            stmt.executeUpdate("CREATE INDEX syncHashToIndex ON syncHash(to)");
        //        } catch (SQLSyntaxErrorException e) {
        //        }
        //stmt.executeUpdate("create CACHED table if not exists stick (stick_id INTEGER PRIMARY KEY IDENTITY, pubkey_id INTEGER, timestamp BIGINT, nonce INTEGER,  signature BINARY(72), content LONGVARBINARY, verified boolean)");
        //            stmt.executeUpdate("insert into person values(1, 'leo')");
        //            stmt.executeUpdate("insert into person values(2, 'yui')");
        //            ResultSet rs2 = stmt.executeQuery("select * from person");
        //            while (rs2.next()) {
        //                // read the result set
        //                System.out.println("name = " + rs2.getString("name"));
        //                System.out.println("id = " + rs2.getInt("id"));
        //            }
        //
        //
        //
        //
        //
        //            String sql2 = "CREATE TABLE if not exists test (id INT NOT NULL,content INT NOT NULL ,PRIMARY KEY (id))";
        //            stmt.execute(sql2);
        //
        ////            String sql3 = "INSERT INTO test (id ,content) VALUES ('1',  '55');";
        ////            stmt.execute(sql3);
        //
        //            // Alle Kunden ausgeben
        //            String sql = "SELECT * FROM test";
        //            ResultSet rs = stmt.executeQuery(sql);
        //
        //            while (rs.next()) {
        //                String id = rs.getString(1);
        //                String content = rs.getString(2);
        //                System.out.println(id + ", " + content + " ");
        //            }
        //
        //            // Resultset schließen
        //            rs.close();
        // Statement schließen
        stmt.close();
        return false;
    }

//    public static void main(String[] args) {
//        try {
//            new MysqlConnection();
//        } catch (SQLException ex) {
//            Logger.getLogger(HsqlConnection.class.getName()).log(Level.SEVERE, null, ex);
//        }
//    }
//    public void init() {
//
//        try {
//            // Treiberklasse laden
//            Class.forName("org.hsqldb.jdbcDriver");
//        } catch (ClassNotFoundException e) {
//            System.err.println("Treiberklasse nicht gefunden!");
//            return;
//        }
//
//        try {
//            con =  DriverManager.getConnection(
//                    "jdbc:hsqldb:file:" + db_file + "; shutdown=true", "root", "");
//            Statement stmt = con.createStatement();
//
////            PubKey
////id INTEGER
////key BINARY(33)
//            stmt.executeUpdate("create table if not exists pubkey (id integer, key BINARY(33))");
//
////Channel
////id INTEGER
////pubkey_id INTEGER
////private_key BINARY(32)
////name LONGVARBINARY
//            stmt.executeUpdate("create table if not exists channel (id integer, pubkey_id INTEGER, private_key BINARY(32), name LONGVARBINARY)");
//
////Message
////id INTEGER
////key_id INTEGER
////channel_id integer
////timestamp BIGINT
////nonce INTEGER
////signature BINARY(72)
////content LONGVARBINARY
////verified boolean
////readable boolean
////decrypted_content LONGVARBINARY
//            //stmt.executeUpdate("drop table if exists message");
//            stmt.executeUpdate("create table if not exists message (id INTEGER, key_id INTEGER, channel_id integer, timestamp BIGINT, nonce INTEGER, signature BINARY(72), content LONGVARBINARY, verified boolean, readable boolean, decryptedContent LONGVARBINARY)");
//
//            // Statement schließen
//            stmt.close();
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
////        finally {
////            if (con != null) {
////                try {
////                    con.close();
////                } catch (SQLException e) {
////                    e.printStackTrace();
////                }
////            }
////        }
//
//    }
    public Connection getConnection() {
        return con;
    }
}
