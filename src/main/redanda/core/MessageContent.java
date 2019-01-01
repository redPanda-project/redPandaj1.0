/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package main.redanda.core;

import java.io.Serializable;

/**
 *
 * @author robin
 */
public class MessageContent implements Serializable {

    public static final char CHAT_MESSAGE = 'M';
    public static final char FILE = 'F';
    private byte[] content;

    public MessageContent(byte[] content) {
        this.content = content;
    }

    public static MessageContent generateChatMessage(String msg) {
        String content = CHAT_MESSAGE + msg;
        return new MessageContent(content.getBytes());
    }
//
//    public static MessageContent generateMessageReceived(Msg msg) {
//        //String content = CHAT_MESSAGE + msg;
//        return new MessageContent(content.getBytes());
//    }

    public boolean isChatMessage() {
        return content[0] == CHAT_MESSAGE;
    }

    public boolean isFile() {
        return content[0] == FILE;
    }
}
