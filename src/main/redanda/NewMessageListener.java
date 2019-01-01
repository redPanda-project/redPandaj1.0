/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package main.redanda;

import main.redanda.core.messages.TextMessageContent;

/**
 *
 * @author robin
 */
public interface NewMessageListener {

    public void newMessage(TextMessageContent msg);
}
