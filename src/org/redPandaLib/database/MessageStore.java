/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redPandaLib.database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import org.redPandaLib.core.Channel;
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

    public int containsMsg(RawMsg m);

    public int getMsgId(RawMsg m);

    public RawMsg getMessageById(int message_id);

    public ResultSet getAllMessagesForSync(long from, long to, long peer_id);

    public int getPubkeyId(ECKey key);

    public Connection getConnection();

    public ArrayList<TextMessageContent> getMessageContentsForPubkey(byte[] pubKey, long from, long to);

    public ArrayList<RawMsg> getMessagesForPubkey(byte[] pubKey, long from, int to);

    public void addDecryptedContent(int pubkey_id, int message_id, int message_type, long timestamp, byte[] decryptedContent, long identity, boolean fromMe, int nonce, byte public_type);

    public void addStick(int pubkey_id, int message_id, double difficulty, long validTill);

    public void showTableMessageContent();

    public int getMessageCount();

    public int getMessageCountToVerify();

    public void commitDatabase();

    public void removeOldMessages(long timestamp);

    public void removeOldMessagesDecryptedContent(long timestamp);

    public void addMsgIntroducedToMe(long peer_id, int message_id);

    public void addMsgIntroducedToHim(long peer_id, int message_id);

    public int msgCountIntroducedToMe(long peer_id);

    public int msgCountIntroducedToHim(long peer_id);

    public int msgsToUser(long peer_id, long from);

    public void addFilterChannel(long peer_id, int channel_id);

    public void delFilterChannel(long peer_id, int channel_id);

    public void clearFilterChannel(long peer_id);

    public void addMessageToSend(int message_id, int channel_id);

    public void addMessageToSendToSpecificPeer(int message_id, int peer_id);

    public boolean removeMessageToSend(long peer_id, int message_id);

    public boolean removeMessageToSend(long peer_id);

    public ResultSet getMessagesForBackSync(long time, int cnt);

    public void resetMessageCounter();

    public void checkpoint();

    public void addUnreadMessage(long message_id);

    public void markAsRead(long message_id);

    public void addKnownChannel(int forChannel, long identity, int fromChannel, int level);

    public void removeKnownChannelFromIdenity(long identity);

    public HashMap<ECKey, Integer> getAllKnownChannels();

    public void removeKnownChannelForCHannel(int channel_id);

    public void removeMessagesDecryptedContent(Channel channel);

    public void removeMessagesDecryptedContent(int pubkey_id);

    public void moveChannelMessagesToHistory(long olderThan);

    public int removeMessagesFromChannel(int pubkey_id, byte public_type, long timestamp);
}
