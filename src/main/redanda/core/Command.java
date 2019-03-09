package main.redanda.core;

public class Command {

    public static final byte GET_PEER_LIST = (byte) 1;// standalone command
    public static final byte PEERLIST = (byte) 2;
    public static final byte SYNC = (byte) 3;// standalone command

    // below 10 was used for redPanda 1.0, can be removed later
    public static final byte DHT_STORE = (byte) 20;//
    public static final byte DHT_GET = (byte) 21;//
    public static final byte DHT_FIND_NODES = (byte) 22;//

    public static final byte FP_FILL = (byte) 25;//
    public static final byte FP_FILL_ACK = (byte) 25;//


    public static final byte UPDATE_REQUEST_TIMESTAMP = (byte) 80;// standalone command
    public static final byte UPDATE_ANSWER_TIMESTAMP = (byte) 81;// update timestamp: 1 long
    public static final byte UPDATE_REQUEST_CONTENT = (byte) 82;//
    public static final byte UPDATE_ANSWER_CONTENT = (byte) 83;//
    public static final byte ANDROID_UPDATE_REQUEST_TIMESTAMP = (byte) 84;// standalone command
    public static final byte ANDROID_UPDATE_ANSWER_TIMESTAMP = (byte) 85;// update timestamp: 1 long
    public static final byte ANDROID_UPDATE_REQUEST_CONTENT = (byte) 86;//
    public static final byte ANDROID_UPDATE_ANSWER_CONTENT = (byte) 87;//
    public static final byte PING = (byte) 100;// standalone command
    public static final byte PONG = (byte) 101;// standalone command

    //kademlia cmds
    public static final byte KADEMLIA_STORE = (byte) 120;// standalone command
    public static final byte JOB_ACK = (byte) 130;// standalone command

    public static final byte STICKS = (byte) 150;

    //200 - 250 reserved for app commands (via socketio)!
    public static final byte getAndroidTimeStamp = (byte) 200;
    public static final byte getAndroidApk = (byte) 201;
    public static final byte authenticate = (byte) 202;
    public static final byte dhtStore = (byte) 203;


    public static final byte DISCONNECT = (byte) 254;
    public static final byte ERROR = (byte) 255;


}
