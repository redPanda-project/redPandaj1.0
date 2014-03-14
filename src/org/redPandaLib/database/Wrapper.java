/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redPandaLib.database;

import crypt.Utils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.redPandaLib.core.Channel;
import org.redPandaLib.core.messages.RawMsg;

/**
 *
 * @author robin
 */
public class Wrapper {

    static HsqlConnection hsqlConnection;

    static {
        try {
            hsqlConnection = new HsqlConnection();
            //hsqlConnection.init();
        } catch (SQLException ex) {
            Logger.getLogger(Wrapper.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    static void saveMsg(RawMsg msg) {
        try {
            Connection connection = hsqlConnection.getConnection();
            Statement stmt = connection.createStatement();


            //get Key Id
            String query = "SELECT pubkey_id,pubkey from pubkey WHERE pubkey = ?";
            //stmt.executeQuery("SELECT id,key from pubkey WHERE key EQUASLS "+ msg.getKey().getPubKey())

            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setBytes(1, msg.getKey().getPubKey());
            ResultSet executeQuery = pstmt.executeQuery();

            boolean found = false;
            while (executeQuery.next()) {
                found = true;
                int aInt = executeQuery.getInt("pubkey_id");
                byte[] bytes = executeQuery.getBytes("pubkey");

                System.out.println("ID: " + aInt + " bytes: " + Channel.byte2String(bytes));
            }

            if (!found) {
                //System.out.println("noch nicht in der db");

                query = "INSERT into pubkey (pubkey) VALUES (?)";
                pstmt = connection.prepareStatement(query);
                pstmt.setBytes(1, msg.getKey().getPubKey());
                pstmt.execute();

                //get Key Id
                query = "SELECT pubkey_id,pubkey from pubkey WHERE pubkey = ?";
                //stmt.executeQuery("SELECT id,key from pubkey WHERE key EQUASLS "+ msg.getKey().getPubKey())

                pstmt = connection.prepareStatement(query);
                pstmt.setBytes(1, msg.getKey().getPubKey());
                executeQuery = pstmt.executeQuery();

            }

            while (executeQuery.next()) {
                int aInt = executeQuery.getInt("pubkey_id");
                byte[] bytes = executeQuery.getBytes("pubkey");

                System.out.println("ID: " + aInt + " bytes: " + Channel.byte2String(bytes));

            }



            query = "SELECT pubkey_id,pubkey from pubkey";
            executeQuery = stmt.executeQuery(query);
            while (executeQuery.next()) {
                executeQuery.getInt("pubkey_id");
                executeQuery.getBytes("pubkey");
            }



        } catch (SQLException ex) {
            Logger.getLogger(Wrapper.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
}
