/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redPandaLib.database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import org.redPandaLib.core.messages.RawMsg;
import org.redPandaLib.core.messages.TextMessageContent;
import org.redPandaLib.crypt.ECKey;

/**
 *
 * @author rflohr
 */
public interface MessageStore {

    public void saveMsg(RawMsg msg);

    public void quit();

    public void showTablePubkey();

    public void showTableMessage();

    public boolean containsMsg(RawMsg m);

    public int getMsgId(RawMsg m);

    public RawMsg getMessageById(int message_id);

    public ResultSet getAllMessagesForSync(long from, long to);

    public int getPubkeyId(ECKey key);

    public Connection getConnection();

    public ArrayList<TextMessageContent> getMessageContentsForPubkey(byte[] pubKey, long from, long to);

    public ArrayList<RawMsg> getMessagesForPubkey(byte[] pubKey, long from, int to);

    public void addDecryptedContent(int pubkey_id, int message_id, int message_type, byte[] decryptedContent, long identity, boolean fromMe);

    public void showTableMessageContent();

    public int getMessageCount();
    
    public void commitDatabase();
}
