package main.redanda.core;

public class Stats {

    private static int socketioConnectionsLiveTime = 0;

    public static int getSocketioConnectionsLiveTime() {
        return socketioConnectionsLiveTime;
    }

    public static void incSocketioConnectionsLiveTime() {
        Stats.socketioConnectionsLiveTime++;
    }
}
