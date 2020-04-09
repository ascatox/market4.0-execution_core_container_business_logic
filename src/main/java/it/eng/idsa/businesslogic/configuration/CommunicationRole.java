package it.eng.idsa.businesslogic.configuration;

/**
 * @author Antonio Scatoloni
 */

public enum CommunicationRole {
    PRODUCER("localhost", 8098),
    CONSUMER("localhost",8086);

    String host;
    int port;

    CommunicationRole(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}