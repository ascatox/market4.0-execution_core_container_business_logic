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
    communication.role.consumer.port=8086
    communication.role.producer.port=8098
    communication.role.enabled=CONSUMER
    */

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
        final String enabledR = getEnabled();
        return CommunicationRole.valueOf(enabledR);
    }

}
