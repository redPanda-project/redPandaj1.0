/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redPandaLib.core;

import java.nio.ByteBuffer;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.redPandaLib.Main;
import org.redPandaLib.NewMessageListener;
import static org.redPandaLib.core.Test.messageStore;
import org.redPandaLib.core.messages.BlockMsg;
import org.redPandaLib.core.messages.TextMessageContent;
import org.redPandaLib.core.messages.TextMsg;
import org.redPandaLib.crypt.Sha256Hash;
import org.redPandaLib.database.DirectMessageStore;
import org.redPandaLib.services.MessageDownloader;

/**
 *
 * @author rflohr
 */
public class Blocks {

    public static void generate(Channel channel) {

        //get id from chanel in database
        int pubkeyId = Test.messageStore.getPubkeyId(channel.getKey());

        long currentTime = System.currentTimeMillis() - BlockMsg.BLOCK_SYNC_TO_TIME; // have to go back some time to overcome the problem with new messages

        try {
            //get Key Id
            //String query = "SELECT pubkey_id,message_id,content,public_type,timestamp,nonce from message WHERE timestamp > ? and verified = true AND pubkey_id = ?";
            String query = "SELECT message_id,message_type,timestamp,decryptedContent,identity,fromMe,nonce,public_type from channelmessage WHERE pubkey_id =? AND timestamp > ? AND timestamp < ? ORDER BY timestamp ASC";
            PreparedStatement pstmt = Test.messageStore.getConnection().prepareStatement(query);
            pstmt.setInt(1, pubkeyId);
            long asd = BlockMsg.TIME_TO_SYNC_BACK;
            System.out.println("time: " + asd);
            pstmt.setLong(2, asd);
            pstmt.setLong(3, currentTime);

            ResultSet executeQuery = pstmt.executeQuery();

            int msgcount = 0;

            boolean breaked = false;

            byte[] dataArray = new byte[1024 * 200]; //max size of a message!! should be regulated later!
            ByteBuffer buffer = ByteBuffer.wrap(dataArray);

            byte[] dataArrayHash = new byte[1024 * 200];
            ByteBuffer bufferHash = ByteBuffer.wrap(dataArrayHash);

            //ToDo: add hash from all data and count of messages to get an easier sync!
            //System.out.println("dwzdzwd " + executeQuery.next());
            while (executeQuery.next()) {

                int message_id = executeQuery.getInt("message_id");
                int message_type = executeQuery.getInt("message_type");
                byte[] decryptedContent = executeQuery.getBytes("decryptedContent");
                long timestamp = executeQuery.getLong("timestamp");
                long identity = executeQuery.getLong("identity");
                boolean fromMe = executeQuery.getBoolean("fromMe");

                byte public_type = executeQuery.getByte("public_type");
                int nonce = executeQuery.getInt("nonce");

                if (decryptedContent == null) {
                    decryptedContent = "".getBytes();
                }

                //only pack a message into a block if the public_type is 20!
                if (public_type == 20) {

                    //skip content which will be generated regulary, image messages should not have public_type 20! (with new version, old imgs will be deleted)
                    if (message_type != TextMsg.BYTE) {
                        continue;
                    }
                    if (buffer.remaining() < 8 + 4 + 4 + 8 + 4 + decryptedContent.length) {
                        System.out.println("buffer full, exit routine, dont know what to do atm");
                        breaked = true;
                        break;
                    }

                    //System.out.println("Data: msgtyp: " + message_type + " pubtyp: " + public_type + " " + new String(decryptedContent));
                    System.out.println("len: " + decryptedContent.length);
                    buffer.putLong(timestamp);
                    buffer.putInt(nonce);
                    buffer.putInt(message_type);
                    buffer.putLong(identity);
                    buffer.putInt(decryptedContent.length);
                    buffer.put(decryptedContent);

                    msgcount++;

                    //add data to hash bytes:b (dont use all data because when syncing we only need to get these data types)
                    bufferHash.putLong(timestamp);
                    bufferHash.putInt(message_type);
                    bufferHash.putInt(public_type);
                }

            }
            executeQuery.close();
            pstmt.close();

            if (breaked) {
                System.out.println("abort...");
                return;
            }

            //System.out.println("Found - messages: " + msgCnt + "  with  " + textBytes / 1024. + " kb text and " + imageBytes / 1024. + " kb images");
            //System.out.format("%30s %10s %10s %20s %10s %20s %10s %20s %10s %20s\n", "Channel", "pubkeyId", "rawCount", "textbytes", "imageCount", "imageBytes", "infoCount", "infoBytes", "deliveredCount", "deliveredBytes");
            //System.out.format("%30s %10d %10d %20f %10d %20f %10d %20f %10d %20f\n", chan.getName(), pubkeyId, rawMessageCount, textBytes / 1024., imageMessageCount, imageBytes / 1024., infoMessageCount, infoBytes / 1024., deliveredMessageCount, deliveredBytes / 1024.);
            ByteBuffer finalBuffer = ByteBuffer.allocate(buffer.position() + 1 + 8 + 4 + 4);

            System.out.println("size: " + buffer.position());

            ByteBuffer lel = ByteBuffer.allocate(buffer.position());
            lel.put(dataArray, 0, buffer.position());

            dataArray = lel.array();//shrinked to actual size

            finalBuffer.put(BlockMsg.BYTE); //cmd for block
            finalBuffer.putLong(Test.localSettings.identity);//mark that i was the generator of the block
            finalBuffer.putInt(msgcount);
            finalBuffer.putInt(Sha256Hash.create(dataArrayHash).hashCode());
            finalBuffer.put(dataArray);

            double kbs = finalBuffer.position() / 1024.;
            System.out.println("content block size: " + kbs + "  in " + msgcount + " messags.");

            MessageDownloader.channelIdToLatestBlockTimeLock.lock();
            MessageDownloader.channelIdToLatestBlockTime.put(pubkeyId, currentTime);
            MessageDownloader.channelIdToLatestBlockTimeLock.unlock();

//                        System.out.println("hex: " + Utils.bytesToHexString(finalBuffer.array()));
            BlockMsg build = BlockMsg.build(channel, currentTime, 45678, finalBuffer.array());

            BlockMsg addMessage = (BlockMsg) MessageHolder.addMessage(build);
            Test.broadcastMsg(addMessage);
            String text = "New block generated with " + msgcount + " msgs (" + kbs + " kb).";
            Test.messageStore.addDecryptedContent(addMessage.getKey().database_id, (int) addMessage.database_Id, BlockMsg.BYTE, addMessage.timestamp, text.getBytes(), ((BlockMsg) addMessage).getIdentity(), true, addMessage.nonce, addMessage.public_type);
            TextMessageContent textMessageContent = new TextMessageContent(addMessage.database_Id, addMessage.key.database_id, addMessage.public_type, TextMsg.BYTE, addMessage.timestamp, addMessage.decryptedContent, addMessage.channel, addMessage.getIdentity(), text, true);
            textMessageContent.read = true;
            for (NewMessageListener listener : Main.listeners) {
                listener.newMessage(textMessageContent);
            }

            System.out.println("New block saved and send, doing cleanup...");

            //remove old block:
            int removeMessagesFromChannel = messageStore.removeMessagesFromChannel(pubkeyId, BlockMsg.PUBLIC_TYPE, currentTime);
            System.out.println("removed old blocks: " + removeMessagesFromChannel);

            //remove old messages which are encrypted and now saved in the new block (only necessary data)...
            removeMessagesFromChannel = messageStore.removeMessagesFromChannel(pubkeyId, (byte) 20, currentTime);
            System.out.println("removed old encrypted messages: " + removeMessagesFromChannel);

        } catch (SQLException ex) {
            Logger.getLogger(DirectMessageStore.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
