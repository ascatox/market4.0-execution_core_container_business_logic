package it.eng.idsa.businesslogic.configuration;

/**
 * @author Antonio Scatoloni
 */

public enum CommunicationRole {
    PRODUCER(8098),
    CONSUMER(8086);

    int port;

    CommunicationRole(int port) {
        this.port = port;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}