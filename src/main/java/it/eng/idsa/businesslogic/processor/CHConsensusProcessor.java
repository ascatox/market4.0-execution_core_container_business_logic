package it.eng.idsa.businesslogic.processor;

import de.fraunhofer.iais.eis.Message;
import de.fraunhofer.iais.eis.MessageProcessedNotification;
import de.fraunhofer.iais.eis.MessageProcessedNotificationBuilder;
import de.fraunhofer.iais.eis.ids.jsonld.Serializer;
import it.eng.idsa.businesslogic.configuration.WebSocketServerConfigurationB;
import it.eng.idsa.businesslogic.multipart.MultipartMessage;
import it.eng.idsa.businesslogic.multipart.MultipartMessageBuilder;
import it.eng.idsa.businesslogic.processor.consumer.ConsumerMessageBufferBean;
import it.eng.idsa.businesslogic.processor.consumer.websocket.server.ResponseMessageBufferBean;
import it.eng.idsa.businesslogic.processor.producer.ProducerMessageBufferBean;
import it.eng.idsa.businesslogic.processor.producer.ProducerSendDataToBusinessLogicProcessor;
import it.eng.idsa.businesslogic.processor.producer.websocket.client.MessageWebSocketOverHttpSender;
import it.eng.idsa.businesslogic.service.ClearingHouseService;
import it.eng.idsa.businesslogic.service.MultipartMessageService;
import it.eng.idsa.businesslogic.service.MultipartMessageTransformerService;
import it.eng.idsa.businesslogic.service.RejectionMessageService;
import it.eng.idsa.businesslogic.util.RejectionMessageType;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Antonio Scatoloni at 06/05/2020
 */

@Component
public class CHConsensusProcessor implements Processor {
    private static final Logger logger = LogManager.getLogger(CHConsensusProcessor.class);
    public static final String CH_CONSENSUS_PREFIX = "CH-CONSENSUS-";
    public static final String FIRST_PHASE = CH_CONSENSUS_PREFIX + "First-Phase";
    public static final String SECOND_PHASE = CH_CONSENSUS_PREFIX + "Second-Phase";
    public static final String SEPARATOR = "@@@";

    @Autowired
    ProducerSendDataToBusinessLogicProcessor producerSendDataToBusinessLogicProcessor;

    @Autowired
    private MultipartMessageService multipartMessageService;

    @Autowired
    private ClearingHouseService clearingHouseService;

    @Value("${application.camelConsumerPort}")
    private int port;

    @Value("${application.isEnabledClearingHouse}")
    private boolean isEnabledClearingHouse;

    @Value("${application.idscp.isEnabled}")
    private boolean isEnabledIdscp;

    @Value("${application.websocket.isEnabled}")
    private boolean isEnabledWebSocket;

    @Autowired
    private MultipartMessageTransformerService multipartMessageTransformerService;

    @Autowired
    private RejectionMessageService rejectionMessageService;

    @Autowired
    MessageWebSocketOverHttpSender messageWebSocketOverHttpSender;

    @Autowired(required = false)
    WebSocketServerConfigurationB webSocketServerConfiguration;

    private Message message;
    private String webSocketHost;
    private int webSocketPort;


    @Override
    public void process(Exchange exchange) throws Exception {
        if (!isEnabledClearingHouse) {
            exchange.getOut().setHeaders(exchange.getIn().getHeaders());
            exchange.getOut().setBody(exchange.getIn().getBody());
            exchange.getOut().setHeader("Is-Message-Processed-Notification", false);
            return;
        }
        logger.info("I'm inside ECC at Endpoint B port: " + port);
        Map<String, Object> headerParts = exchange.getIn().getHeaders();
        Map<String, Object> multipartMessageParts = exchange.getIn().getBody(HashMap.class);
        String header = multipartMessageParts.get("header").toString();
        String payload = multipartMessageParts.get("payload").toString();
        message = multipartMessageService.getMessage(header);
        if (message instanceof MessageProcessedNotification) {
            if (payload.startsWith(FIRST_PHASE))
                receiveMessageProcessedNotificationFirstPhase(exchange);
            else if (payload.startsWith(SECOND_PHASE))
                receiveMessageProcessedNotificationSecondPhase(exchange);
        } else { //Original Message 1 Phase
            ConsumerMessageBufferBean.getInstance().addMessageBuffer(header, payload);
            sendMessageProcessedNotification(exchange, FIRST_PHASE, null,
                    createForwardTo(FIRST_PHASE));  //Send to EndPoint B in Producer
        }
    }

    //Server B Producer
    private void receiveMessageProcessedNotificationFirstPhase(Exchange exchange) throws Exception {
        String header = ProducerMessageBufferBean.getInstance().getHeader();
        String payload = ProducerMessageBufferBean.getInstance().getPayload();
        Message message = multipartMessageService.getMessage(header);
        if (isEnabledClearingHouse) {
            logger.info("Registering transaction to Clearing House...");
            clearingHouseService.registerTransaction(message, payload);
            logger.info("Transaction successfully registered");
        }
        ProducerMessageBufferBean.getInstance().empty();
        //Send Response MessageProcessedNotification to EndPoint B in Consumer -> Start the 2 Phase!
        if (isEnabledIdscp || isEnabledWebSocket) {
            header = buildMessageProcessedNotification();
            payload = SECOND_PHASE;
            MultipartMessage responseMessage = new MultipartMessageBuilder()
                    .withHeaderContent(header)
                    .withPayloadContent(payload)
                    .build();
            String responseString = multipartMessageTransformerService.multipartMessagetoString(responseMessage, false);
            ResponseMessageBufferBean responseMessageServerBean = webSocketServerConfiguration.responseMessageBufferWebSocket();
            responseMessageServerBean.add(responseString.getBytes());
        } else
            //Send to EndPoint B in Consumer HTTPS Only needed
            sendMessageProcessedNotification(exchange, SECOND_PHASE, null, createForwardTo(SECOND_PHASE));
    }

    private void receiveMessageProcessedNotificationSecondPhase(Exchange exchange) {
        //String payloadOrig = multipartMessageParts.get("payload").toString();
        Map<String, Object> multipartMessageParts = retrieveOriginalMessage(exchange);
        exchange.getIn().getHeaders().put("Is-Message-Processed-Notification", false);
        exchange.getOut().setHeaders(exchange.getIn().getHeaders());
        exchange.getOut().setBody(multipartMessageParts);
        //Go on with Consumer Routes
    }

    private void sendMessageProcessedNotification(Exchange exchange, String payload, String payloadData, String forwardTo) throws Exception {
        if (payloadData != null)
            payload += SEPARATOR + payloadData;
        Map<String, Object> multipartMessageParts = exchange.getIn().getBody(HashMap.class);
        String header = buildMessageProcessedNotification();
        multipartMessageParts.put("header", header);
        multipartMessageParts.put("payload", payload);
        exchange.getIn().setBody(multipartMessageParts);
        MultipartMessage multipartMessage = new MultipartMessageBuilder()
                .withHeaderContent(header)
                .withPayloadContent(payload)
                .build();
        String multipartMessageString = multipartMessageTransformerService.multipartMessagetoString(multipartMessage);
        //Wait Response from ECC Producer B EndPoint
        if (isEnabledWebSocket) {
            extractWebSocketIPAndPort(forwardTo, ProducerSendDataToBusinessLogicProcessor.REGEX_WSS);
            String response = messageWebSocketOverHttpSender.sendMultipartMessageWebSocketOverHttps(webSocketHost, webSocketPort, header, payload);
            handleResponseWebSocket(exchange, message, response, forwardTo, multipartMessageString);
        } else if (isEnabledIdscp) {
            extractWebSocketIPAndPort(forwardTo, ProducerSendDataToBusinessLogicProcessor.REGEX_IDSCP);
            String response = producerSendDataToBusinessLogicProcessor.sendMultipartMessageWebSocket(webSocketHost, webSocketPort, header, payload, null);
            handleResponseWebSocket(exchange, message, response, forwardTo, multipartMessageString);
        } else {
            CloseableHttpResponse response = producerSendDataToBusinessLogicProcessor.forwardMessageBinary(forwardTo, header, payload);
            handleResponse(exchange, message, response, forwardTo, multipartMessageString);
        }
    }


    private String buildMessageProcessedNotification() throws DatatypeConfigurationException, IOException {
        GregorianCalendar gcal = new GregorianCalendar();
        XMLGregorianCalendar xgcal = DatatypeFactory.newInstance()
                .newXMLGregorianCalendar(gcal);
        gcal = new GregorianCalendar();
        xgcal = DatatypeFactory.newInstance()
                .newXMLGregorianCalendar(gcal);
        ArrayList<URI> recipientConnectors = new ArrayList<URI>();
        MessageProcessedNotificationBuilder messageProcessedNotificationBuilder = new MessageProcessedNotificationBuilder();
        MessageProcessedNotification messageProcessedNotification = messageProcessedNotificationBuilder
                ._issuerConnector_(URI.create("autogenerated"))
                ._issued_(xgcal)._modelVersion_("2.1.0-SNAPSHOT")
                .build();
        return new Serializer().serialize(messageProcessedNotification);
    }

    private void handleResponse(Exchange exchange, Message message, CloseableHttpResponse response, String forwardTo,
                                String multipartMessageBody)
            throws UnsupportedOperationException, IOException {
        producerSendDataToBusinessLogicProcessor.handleResponse(exchange, message, response, forwardTo, multipartMessageBody);
        exchange.getOut().setHeader("Is-Message-Processed-Notification", true);
    }

    private void handleResponseWebSocket(Exchange exchange, Message message, String responseString,
                                         String forwardTo, String multipartMessageBody) {
        if (responseString == null) {
            logger.info("...communication error");
            rejectionMessageService.sendRejectionMessage(
                    RejectionMessageType.REJECTION_COMMUNICATION_LOCAL_ISSUES,
                    message);
        } else {
            logger.info("Successful response: " + responseString);
            Map<String, Object> multipartMessageParts = retrieveOriginalMessage(exchange);
            exchange.getOut().setHeaders(exchange.getIn().getHeaders());
            exchange.getOut().setBody(multipartMessageParts);
        }
        exchange.getOut().setHeader("Is-Message-Processed-Notification", false);
    }

    private void extractWebSocketIPAndPort(String forwardTo, String regexForwardTo) {
        // Split URL into protocol, host, port
        Pattern pattern = Pattern.compile(regexForwardTo);
        Matcher matcher = pattern.matcher(forwardTo);
        matcher.find();

        this.webSocketHost = matcher.group(2);
        this.webSocketPort = Integer.parseInt(matcher.group(4));
    }

    //TODO FROM Application Properties
    private String createForwardTo(String phase) {
        String forwardTo = null;
        if (phase.startsWith(FIRST_PHASE)) { //Send from Consumer Application to B EndPoint Producer
            if (isEnabledWebSocket) {
                forwardTo = "wss://localhost:8098";
            } else if (isEnabledIdscp) {
                forwardTo = "idscp://localhost:8098";
            } else {
                forwardTo = "https://localhost:8889/incoming-data-channel/receivedMessage";
            }
        } else if (phase.startsWith(SECOND_PHASE)) { //Send from Consumer Application to B EndPoint Consumer only HTTPS!
            forwardTo = "https://localhost:8890/incoming-data-channel/receivedMessage";
        }
        return forwardTo;
    }

    private Map<String, Object> retrieveOriginalMessage(Exchange exchange) {
        Map<String, Object> multipartMessageParts = exchange.getIn().getBody(HashMap.class);
        String header = ConsumerMessageBufferBean.getInstance().getHeader();
        String payload = ConsumerMessageBufferBean.getInstance().getPayload();
        multipartMessageParts.put("header", header);
        multipartMessageParts.put("payload", payload);
        ConsumerMessageBufferBean.getInstance().empty();
        return multipartMessageParts;
    }

}
