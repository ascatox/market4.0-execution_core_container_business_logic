package it.eng.idsa.businesslogic.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author Antonio Scatoloni
 */

@Component
@ConfigurationProperties("communication.role")
public class CommunicationRoleConfiguration {

    /*
    communication.role.producer.host=localhost
    communication.role.consumer.host=localhost
    communication.role.consumer.port=8086
    communication.role.producer.port=8098
    communication.role.enabled=CONSUMER
    */

    private String consumerHost;
    private String producerHost;
    private int consumerPort;
    private int producerPort;

    private String enabled;

    public int getConsumerPort() {
        return consumerPort;
    }

    public void setConsumerPort(int consumerPort) {
        this.consumerPort = consumerPort;
        CommunicationRole.CONSUMER.setPort(consumerPort);
    }

    public String getConsumerHost() {
        return consumerHost;
    }

    public void setConsumerHost(String consumerHost) {
        this.consumerHost = consumerHost;
        CommunicationRole.CONSUMER.setHost(consumerHost);
    }

    public String getProducerHost() {
        return producerHost;
    }

    public void setProducerHost(String producerHost) {
        this.producerHost = producerHost;
        CommunicationRole.PRODUCER.setHost(producerHost);
    }

    public int getProducerPort() {
        return producerPort;
    }

    public void setProducerPort(int producerPort) {
        this.producerPort = producerPort;
        CommunicationRole.PRODUCER.setPort(producerPort);
    }

    public String getEnabled() {
        return enabled;
    }

    public void setEnabled(String enabled) {
        this.enabled = enabled.trim().toUpperCase();
    }

    public CommunicationRole getCommunicationRole() {
        return CommunicationRole.valueOf(getEnabled());
    }

}
