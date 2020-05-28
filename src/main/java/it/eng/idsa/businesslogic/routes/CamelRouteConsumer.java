package it.eng.idsa.businesslogic.routes;

import it.eng.idsa.businesslogic.configuration.ApplicationConfiguration;
import it.eng.idsa.businesslogic.processor.CHConsensusProcessor;
import it.eng.idsa.businesslogic.processor.consumer.*;
import it.eng.idsa.businesslogic.processor.exception.ExceptionForProcessor;
import it.eng.idsa.businesslogic.processor.exception.ExceptionProcessorConsumer;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 
 * @author Milan Karajovic and Gabriele De Luca
 *
 */

@Component
public class CamelRouteConsumer extends RouteBuilder {
	
	private static final Logger logger = LogManager.getLogger(CamelRouteConsumer.class);
	
	@Autowired
	private ApplicationConfiguration configuration;
	
	@Autowired
	ConsumerValidateTokenProcessor validateTokenProcessor;

	@Autowired
	ConsumerMultiPartMessageProcessor multiPartMessageProcessor;
	
	@Autowired
	ConsumerSendDataToDataAppProcessor sendDataToDataAppProcessor;
	
	@Autowired
	ConsumerSendTransactionToCHProcessor sendTransactionToCHProcessor;
	
	@Autowired
	ExceptionProcessorConsumer exceptionProcessorConsumer;
	
	@Autowired
	ConsumerGetTokenFromDapsProcessor getTokenFromDapsProcessor;
	
	@Autowired
	ConsumerSendDataToBusinessLogicProcessor sendDataToBusinessLogicProcessor;
	
	@Autowired
	ConsumerExceptionMultiPartMessageProcessor exceptionMultiPartMessageProcessor;
	
	//@Autowired
	//CHConsensusProcessor chConsensusProcessor;

	//@Autowired
	//ConsumerValidateDataByCHProcessor consumerValidateDataByCHProcessor;

	@Autowired
	ConsumerSendToActiveMQ sendToActiveMQ;

	@Autowired
	ConsumerReceiveFromActiveMQ receiveFromActiveMQ;

    @Autowired
    CamelContext camelContext;

	@Value("${application.idscp.isEnabled}")
	private boolean isEnabledIdscp;

	@Value("${application.websocket.isEnabled}")
	private boolean isEnabledWebSocket;

	@Override
	public void configure() throws Exception {
		logger.debug("Starting Camel Routes...consumer side");
        camelContext.getShutdownStrategy().setLogInflightExchangesOnTimeout(false);
        camelContext.getShutdownStrategy().setTimeout(3);

		onException(ExceptionForProcessor.class, RuntimeException.class)
			.handled(true)
			.process(exceptionProcessorConsumer)
			.process(exceptionMultiPartMessageProcessor)
			.choice()
				.when(header("Is-Enabled-Daps-Interaction").isEqualTo(true))
					.process(getTokenFromDapsProcessor)
					.process(sendDataToBusinessLogicProcessor)
					.choice()
						.when(header("Is-Enabled-Clearing-House").isEqualTo(true))
						//.process(sendTransactionToCHProcessor)
					.endChoice()
				.when(header("Is-Enabled-Daps-Interaction").isEqualTo(false))
					.process(sendDataToBusinessLogicProcessor)
					.choice()
						.when(header("Is-Enabled-Clearing-House").isEqualTo(true))
						//.process(sendTransactionToCHProcessor)
					.endChoice()
			.endChoice();

		// Camel SSL - Endpoint: B
		if(!isEnabledIdscp && !isEnabledWebSocket)
			from("jetty://https4://0.0.0.0:"+configuration.getCamelConsumerPort()+"/incoming-data-channel/receivedMessage")
			.process(multiPartMessageProcessor)
			.choice()
				.when(header("Is-Enabled-Daps-Interaction").isEqualTo(true))
					.process(validateTokenProcessor)
					//.process(sendToActiveMQ)
					//.process(chConsensusProcessor)
					.choice()
					.when(header("Is-Message-Processed-Notification").isEqualTo(false))
						//.process(receiveFromActiveMQ)
						.choice()
						.when(header("Is-Enabled-Clearing-House").isEqualTo(true))
							//.process(consumerValidateDataByCHProcessor)
						.endChoice()
						// Send to the Endpoint: F
						.process(sendDataToDataAppProcessor)
						.process(multiPartMessageProcessor)
						.process(getTokenFromDapsProcessor)
						.process(sendDataToBusinessLogicProcessor)
						.choice()
							.when(header("Is-Enabled-Clearing-House").isEqualTo(true))
								//.process(sendTransactionToCHProcessor)
						.endChoice()
					.endChoice()
				.when(header("Is-Enabled-Daps-Interaction").isEqualTo(false))
					//.process(chConsensusProcessor)
					.choice()
					.when(header("Is-Message-Processed-Notification").isEqualTo(false))
						//.process(sendToActiveMQ)
						//.process(sendToActiveMQ)
						.choice()
						.when(header("Is-Enabled-Clearing-House").isEqualTo(true))
						//	.process(consumerValidateDataByCHProcessor)
						.endChoice()
						// Send to the Endpoint: F
						.process(sendDataToDataAppProcessor)
						.process(multiPartMessageProcessor)
						.process(sendDataToBusinessLogicProcessor)
//						.choice()
//							.when(header("Is-Enabled-Clearing-House").isEqualTo(true))
//								//.process(sendTransactionToCHProcessor)
//						.endChoice()
			.endChoice()
		.endChoice();
		
		// TODO: Improve this initialization
		// Camel WebSocket - Endpoint B
		//boolean startupRoute = true;
		/*from("timer://simpleTimer?repeatCount=-1")
			.process(fileRecreatorProcessor)
			.process(multiPartMessageProcessor)
			.choice()
				.when(header("Is-Enabled-Daps-Interaction").isEqualTo(true))
					.process(validateTokenProcessor)
					.process(sendToActiveMQ)
					.process(receiveFromActiveMQ)
					// Send to the Endpoint: F
					.process(sendDataToDataAppProcessor)
					.process(multiPartMessageProcessor)
					.process(getTokenFromDapsProcessor)
					.process(sendDataToBusinessLogicProcessor)
					.choice()
						.when(header("Is-Enabled-Clearing-House").isEqualTo(true))
							//.process(sendTransactionToCHProcessor)
					.endChoice()
				.when(header("Is-Enabled-Daps-Interaction").isEqualTo(false))
					.process(sendToActiveMQ)
					.process(receiveFromActiveMQ)
					// Send to the Endpoint: F
					.process(sendDataToDataAppProcessor)
					.process(multiPartMessageProcessor)
					.process(sendDataToBusinessLogicProcessor)
					.choice()
						.when(header("Is-Enabled-Clearing-House").isEqualTo(true))
							//.process(sendTransactionToCHProcessor)
					.endChoice()
			.endChoice();
		 */
	}
}
