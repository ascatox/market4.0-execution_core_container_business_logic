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
public class WebSocketServerConfigurationB implements WebSocketServerConfiguration {

    @Value("${communication.ws.endpointB.port}")
    private int port;

    @Bean(name="frameBufferWebSocketB")
    @Scope("singleton")
    @Qualifier(value="FrameBufferBeanB")
    public FrameBufferBean frameBufferWebSocket() {
        return new FrameBufferBean();
    }


    /**
     * @author Antonio Scatoloni
     * @return
     */
    @Bean(name="httpsServerWebSocketB")
    @Scope("singleton")
    @Qualifier(value="HttpWebSocketServerBeanB")
    public HttpWebSocketServerBean httpsServerWebSocket() {
        HttpWebSocketServerBean httpWebSocketServerBean = new HttpWebSocketServerBean();
        httpWebSocketServerBean.setPort(port);
        httpWebSocketServerBean.setMessagingServlet(HttpWebSocketMessagingServletB.class);
        return httpWebSocketServerBean;
    }

    /**
     * @author Antonio Scatoloni
     * @return
     */
    @Bean(name="messagingLogicB")
    @Scope("singleton")
    @Qualifier(value="MessagingLogicB")
    public HttpWebSocketMessagingLogicB messagingLogic() {
        HttpWebSocketMessagingLogicB httpWebSocketMessagingLogic = HttpWebSocketMessagingLogicB.getInstance();
        httpWebSocketMessagingLogic.setWebSocketServerConfiguration(this);
        return httpWebSocketMessagingLogic;
    }

    @Bean(name="fileRecreatorBeanWebSocketB")
    @Scope("singleton")
    @Qualifier(value="FileRecreatorBeanServerB")
    public FileRecreatorBeanServer fileRecreatorBeanWebSocket() {
        FileRecreatorBeanServer fileRecreatorBeanServer = new FileRecreatorBeanServer();
        fileRecreatorBeanServer.setWebSocketServerConfiguration(this);
        return fileRecreatorBeanServer;
    }

    @Bean(name="recreatedMultipartMessageBeanWebSocketB")
    @Scope("singleton")
    @Qualifier(value="RecreatedMultipartMessageBeanB")
    public RecreatedMultipartMessageBean recreatedMultipartMessageBeanWebSocket() {
        return new RecreatedMultipartMessageBean();
    }

    @Bean(name="responseMessageBufferWebSocketB")
    @Scope("singleton")
    @Qualifier(value="ResponseMessageBufferBeanB")
    public ResponseMessageBufferBean responseMessageBufferWebSocket() {
        return new ResponseMessageBufferBean();
    }

    @Bean(name="responseMessageSendPartialWebSocketB")
    @Scope("singleton")
    @Qualifier(value="ResponseMessageSendPartialServerB")
    public ResponseMessageSendPartialServer responseMessageSendPartialWebSocket() {
        ResponseMessageSendPartialServer responseMessageSendPartialServer = new ResponseMessageSendPartialServer();
        responseMessageSendPartialServer.setWebSocketServerConfiguration(this);
        return responseMessageSendPartialServer;
    }

}
