/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redPandaLib;

import org.redPandaLib.core.messages.TextMessageContent;
import org.redPandaLib.core.messages.TextMsg;

/**
 *
 * @author robin
 */
public interface NewMessageListener {

    public void newMessage(TextMessageContent msg);
}
