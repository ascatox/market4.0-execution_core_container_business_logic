package it.eng.idsa.businesslogic.processor.consumer;

import it.eng.idsa.businesslogic.processor.consumer.websocket.server.FrameBufferBean;

/**
 * @author Antonio Scatoloni at 06/05/2020
 */

public class ConsumerMessageBufferBean {
    private volatile String header;
    private volatile String payload;
    private static ConsumerMessageBufferBean instance;

    private ConsumerMessageBufferBean() {
    }

    public static ConsumerMessageBufferBean getInstance() {
        if (instance == null) {
            synchronized (FrameBufferBean.class) {
                if (instance == null) {
                    instance = new ConsumerMessageBufferBean();
                }
            }
        }
        return instance;
    }

    public void addMessageBuffer(String header, String payload){
        this.header = header;
        this.payload = payload;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }


    public void empty() {
        this.header = null;
        this.payload = null;
    }
}
