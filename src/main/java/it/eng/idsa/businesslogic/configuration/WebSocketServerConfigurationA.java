package it.eng.idsa.businesslogic.configuration;

import it.eng.idsa.businesslogic.processor.consumer.websocket.server.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

/**
 * @author Milan Karajovic and Gabriele De Luca
 */

@Configuration
public class WebSocketServerConfigurationA implements WebSocketServerConfiguration {

    @Value("${communication.ws.endpointA.port}")
    private int port;

    @Bean(name="frameBufferWebSocketA")
    @Scope("singleton")
    @Qualifier(value="FrameBufferBeanA")
    public FrameBufferBean frameBufferWebSocket() {
        return new FrameBufferBean();
    }

    @Bean(name="httpsServerWebSocketA")
    @Scope("singleton")
    @Qualifier(value="HttpWebSocketServerBeanA")
    public HttpWebSocketServerBean httpsServerWebSocket() {
        HttpWebSocketServerBean httpWebSocketServerBean = new HttpWebSocketServerBean();
        httpWebSocketServerBean.setPort(port);
        httpWebSocketServerBean.setMessagingServlet(HttpWebSocketMessagingServletA.class);
        return httpWebSocketServerBean;
    }

    @Bean(name="messagingLogicA")
    @Scope("singleton")
    @Qualifier(value="MessagingLogicA")
    public HttpWebSocketMessagingLogicA messagingLogic() {
        HttpWebSocketMessagingLogicA httpWebSocketMessagingLogic = HttpWebSocketMessagingLogicA.getInstance();
        httpWebSocketMessagingLogic.setWebSocketServerConfiguration(this);
        return httpWebSocketMessagingLogic;
    }

    @Bean(name="fileRecreatorBeanWebSocketA")
    @Scope("singleton")
    @Qualifier(value="FileRecreatorBeanServerA")
    public FileRecreatorBeanServer fileRecreatorBeanWebSocket() {
        FileRecreatorBeanServer fileRecreatorBeanServer = new FileRecreatorBeanServer();
        fileRecreatorBeanServer.setWebSocketServerConfiguration(this);
        return fileRecreatorBeanServer;
    }

    @Bean(name="recreatedMultipartMessageBeanWebSocketA")
    @Scope("singleton")
    @Qualifier(value="RecreatedMultipartMessageBeanA")
    public RecreatedMultipartMessageBean recreatedMultipartMessageBeanWebSocket() {
        return new RecreatedMultipartMessageBean();
    }

    @Bean(name="responseMessageBufferWebSocketA")
    @Scope("singleton")
    @Qualifier(value="ResponseMessageBufferBeanA")
    public ResponseMessageBufferBean responseMessageBufferWebSocket() {
        return new ResponseMessageBufferBean();
    }

    @Bean(name="responseMessageSendPartialWebSocketA")
    @Scope("singleton")
    @Qualifier(value="ResponseMessageSendPartialServerA")
    public ResponseMessageSendPartialServer responseMessageSendPartialWebSocket() {
        ResponseMessageSendPartialServer responseMessageSendPartialServer = new ResponseMessageSendPartialServer();
        responseMessageSendPartialServer.setWebSocketServerConfiguration(this);
        return responseMessageSendPartialServer;
    }

}