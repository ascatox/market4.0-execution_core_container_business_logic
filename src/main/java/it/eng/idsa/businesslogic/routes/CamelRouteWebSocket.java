

package it.eng.idsa.businesslogic.routes;

import it.eng.idsa.businesslogic.configuration.ApplicationConfiguration;
import it.eng.idsa.businesslogic.configuration.CommunicationRole;
import it.eng.idsa.businesslogic.processor.consumer.*;
import it.eng.idsa.businesslogic.processor.exception.ExceptionProcessorConsumer;
import it.eng.idsa.businesslogic.processor.exception.ExceptionProcessorProducer;
import it.eng.idsa.businesslogic.processor.producer.*;
import org.apache.camel.builder.RouteBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 *  @author Antonio Scatoloni
 */

@Component
public class CamelRouteWebSocket extends RouteBuilder {

    private static final Logger logger = LogManager.getLogger(CamelRouteWebSocket.class);

    @Autowired
    private ApplicationConfiguration configuration;

    @Autowired
    ProducerParseReceivedDataProcessorBodyBinary parseReceivedDataProcessorBodyBinary;

    @Autowired
    ProducerParseReceivedDataFromDAppProcessorBodyBinary producerParseReceivedDataFromDAppProcessorBodyBinary;

    @Autowired
    ProducerGetTokenFromDapsProcessor getTokenFromDapsProcessor;

    @Autowired
    ProducerSendTransactionToCHProcessor sendTransactionToCHProcessor;

    @Autowired
    ConsumerSendDataToBusinessLogicProcessor consumerSendDataToBusinessLogicProcessor;

    @Autowired
    ProducerSendDataToBusinessLogicProcessor producerSendDataToBusinessLogicProcessor;

    @Autowired
    ProducerParseReceivedResponseMessage parseReceivedResponseMessage;

    @Autowired
    ProducerValidateTokenProcessor validateTokenProcessor;

    @Autowired
    ProducerSendResponseToDataAppProcessor sendResponseToDataAppProcessor;

    @Autowired
    ExceptionProcessorProducer processorException;

    @Autowired
    ConsumerMultiPartMessageProcessor multiPartMessageProcessor;

    @Autowired
    ConsumerFileRecreatorProcessor fileRecreatorProcessor;

    @Autowired
    ConsumerSendDataToDataAppProcessor sendDataToDataAppProcessor;

    @Autowired
    ExceptionProcessorConsumer exceptionProcessorConsumer;

    @Autowired
    ConsumerExceptionMultiPartMessageProcessor exceptionMultiPartMessageProcessor;

    @Autowired
    ConsumerWebSocketSendDataToDataAppProcessor sendDataToDataAppProcessorOverWS;


    @Override
    public void configure() throws Exception {
        from("timer://simpleTimer?repeatCount=-1")
                .process(fileRecreatorProcessor)
                .choice() //CONSUMER SIDE
                .when(header(CommunicationRole.class.getSimpleName())
                        .isEqualToIgnoreCase(CommunicationRole.CONSUMER.name()))
                    .process(multiPartMessageProcessor)
                    .choice()
                    .when(header("Is-Enabled-Daps-Interaction").isEqualTo(true))
                        .process(validateTokenProcessor)
                        // Send to the Endpoint: F
                        .choice()
                        .when(header("Is-Enabled-WebSocket").isEqualTo(true))
                            .process(sendDataToDataAppProcessorOverWS)
                        .when(header("Is-Enabled-WebSocket").isEqualTo(false))
                            .process(sendDataToDataAppProcessor)
                        .endChoice()
                        .process(multiPartMessageProcessor)
                        .process(getTokenFromDapsProcessor)
                        .process(consumerSendDataToBusinessLogicProcessor)
                    .choice()
                    .when(header("Is-Enabled-Clearing-House").isEqualTo(true))
                        .process(sendTransactionToCHProcessor)
                    .endChoice()
                     .when(header("Is-Enabled-Daps-Interaction").isEqualTo(false))
                        // Send to the Endpoint: F
                    .choice()
                    .when(header("Is-Enabled-WebSocket").isEqualTo(true))
                        .process(sendDataToDataAppProcessorOverWS)
                    .when(header("Is-Enabled-WebSocket").isEqualTo(false))
                        .process(sendDataToDataAppProcessor)
                    .endChoice()
                        .process(multiPartMessageProcessor)
                        .process(consumerSendDataToBusinessLogicProcessor)
                    .choice()
                    .when(header("Is-Enabled-Clearing-House").isEqualTo(true))
                        .process(sendTransactionToCHProcessor)
                 .endChoice()
                .endChoice()

                .endChoice() //PRODUCER SIDE
                .when(header(CommunicationRole.class.getSimpleName())
                        .isEqualToIgnoreCase(CommunicationRole.PRODUCER.name()))
                    .process(producerParseReceivedDataFromDAppProcessorBodyBinary)
                    .choice()
                    .when(header("Is-Enabled-Daps-Interaction").isEqualTo(true))
                        .process(getTokenFromDapsProcessor)
                        // Send data to Endpoint B
                        .process(producerSendDataToBusinessLogicProcessor)
                        .process(parseReceivedResponseMessage)
                        .process(validateTokenProcessor)
                        .process(sendResponseToDataAppProcessor)
                    .choice()
                    .when(header("Is-Enabled-Clearing-House").isEqualTo(true))
                        .process(sendTransactionToCHProcessor)
                    .endChoice()
                    .when(header("Is-Enabled-Daps-Interaction").isEqualTo(false))
                    // Send data to Endpoint B
                        .process(producerSendDataToBusinessLogicProcessor)
                        .process(parseReceivedResponseMessage)
                        //.process(sendResponseToDataAppProcessor)
                        .process(consumerSendDataToBusinessLogicProcessor) //TODO
                    .choice()
                    .when(header("Is-Enabled-Clearing-House").isEqualTo(true))
                        .process(sendTransactionToCHProcessor)
                    .endChoice()
                    .endChoice()
                .endChoice();
    }
}


