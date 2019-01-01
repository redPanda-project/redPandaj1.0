package main.redanda.socketio;

public class DPeer {


    String url;
    String nodeId;

    public DPeer() {
    }

    public DPeer(String url, String nodeId) {
        this.url = url;
        this.nodeId = nodeId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }
}
