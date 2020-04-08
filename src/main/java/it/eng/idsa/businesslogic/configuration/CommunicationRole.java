package it.eng.idsa.businesslogic.configuration;

import org.springframework.stereotype.Component;

/**
 *  @author Antonio Scatoloni
 */


public enum CommunicationRole {
    PRODUCER(8098),
    CONSUMER(8086);

    private int port;

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
