package org.redPandaLib.core;

public class Command {

    public static final byte GET_PEER_LIST = (byte) 1;// standalone command
    public static final byte PEERLIST = (byte) 2;
    public static final byte SYNC = (byte) 3;// standalone command
    public static final byte UPDATE_REQUEST_TIMESTAMP = (byte) 80;// standalone command
    public static final byte UPDATE_ANSWER_TIMESTAMP = (byte) 81;// update timestamp: 1 long
    public static final byte UPDATE_REQUEST_CONTENT = (byte) 82;//
    public static final byte UPDATE_ANSWER_CONTENT = (byte) 83;//
    public static final byte PING = (byte) 100;// standalone command
    public static final byte PONG = (byte) 101;// standalone command
    public static final byte STICKS = (byte) 150;
    public static final byte DISCONNECT = (byte) 254;
    public static final byte ERROR = (byte) 255;


}
