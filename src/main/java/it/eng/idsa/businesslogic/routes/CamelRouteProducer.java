package it.eng.idsa.businesslogic.routes;

import de.fhg.aisec.ids.api.infomodel.ConnectorProfile;
import de.fhg.aisec.ids.api.settings.ConnectionSettings;
import de.fhg.aisec.ids.api.settings.ConnectorConfig;
import de.fhg.aisec.ids.api.settings.Settings;
import de.fhg.aisec.ids.camel.idscp2.Idscp2OsgiComponent;
import de.fhg.aisec.ids.camel.idscp2.client.Idscp2ClientComponent;
import de.fhg.aisec.ids.camel.idscp2.client.Idscp2ClientEndpoint;
import it.eng.idsa.businesslogic.configuration.ApplicationConfiguration;
import it.eng.idsa.businesslogic.processor.exception.ExceptionForProcessor;
import it.eng.idsa.businesslogic.processor.exception.ExceptionProcessorConsumer;
import it.eng.idsa.businesslogic.processor.exception.ExceptionProcessorProducer;
import it.eng.idsa.businesslogic.processor.producer.*;
import it.eng.idsa.businesslogic.util.config.keystore.TruststoreConfig;
import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.builder.RouteBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 *
 * @author Milan Karajovic and Gabriele De Luca
 *
 */

@Component
public class CamelRouteProducer extends RouteBuilder {

	private static final Logger logger = LogManager.getLogger(CamelRouteProducer.class);

	@Autowired
	private ApplicationConfiguration configuration;

	@Autowired(required = false)
	ProducerFileRecreatorProcessor fileRecreatorProcessor;

	@Autowired
	ProducerParseReceivedDataProcessorBodyBinary parseReceivedDataProcessorBodyBinary;

	@Autowired
	ProducerParseReceivedDataProcessorBodyFormData parseReceivedDataProcessorBodyFormData;

	@Autowired
	ProducerGetTokenFromDapsProcessor getTokenFromDapsProcessor;

	@Autowired
	ProducerSendTransactionToCHProcessor sendTransactionToCHProcessor;

	@Autowired
	ProducerSendDataToBusinessLogicProcessor sendDataToBusinessLogicProcessor;

	@Autowired
	ProducerParseReceivedResponseMessage parseReceivedResponseMessage;

	@Autowired
	ProducerValidateTokenProcessor validateTokenProcessor;

	@Autowired
	ProducerSendResponseToDataAppProcessor sendResponseToDataAppProcessor;

	@Autowired
	ExceptionProcessorProducer processorException;

	@Autowired
	ProducerParseReceivedDataFromDAppProcessorBodyBinary parseReceivedDataFromDAppProcessorBodyBinary;

	@Autowired
	ExceptionProcessorConsumer exceptionProcessorConsumer;

	@Autowired
	CamelContext camelContext;

	@Value("${application.dataApp.websocket.isEnabled}")
	private boolean isEnabledDataAppWebSocket;

	@Value("${application.idscp.isEnabled}")
	private boolean isEnabledIdscp;

	@Override
	public void configure() throws Exception {
		logger.debug("Starting Camel Routes...producer side");

		camelContext.getShutdownStrategy().setLogInflightExchangesOnTimeout(false);
		camelContext.getShutdownStrategy().setTimeout(3);

		Endpoint idscpEndpoint = setupIDSCPEndPoint(getContext());

		onException(ExceptionForProcessor.class, RuntimeException.class)
				.handled(true)
				.process(processorException);

		//@formatter:off
		if(!isEnabledDataAppWebSocket) {
			if(!isEnabledIdscp) {
				// Camel SSL - Endpoint: A - Body binary
				from("jetty://https4://0.0.0.0:" + configuration.getCamelProducerPort() + "/incoming-data-app/multipartMessageBodyBinary")
						.process(parseReceivedDataProcessorBodyBinary)
						.choice()
						.when(header("Is-Enabled-Daps-Interaction").isEqualTo(true))
						.process(getTokenFromDapsProcessor)
						//						.process(sendToActiveMQ)
						//						.process(receiveFromActiveMQ)
						// Send data to Endpoint B
						.process(sendDataToBusinessLogicProcessor)
						.process(parseReceivedResponseMessage)
						.process(validateTokenProcessor)
						.process(sendResponseToDataAppProcessor)
						.choice()
						.when(header("Is-Enabled-Clearing-House").isEqualTo(true))
						.process(sendTransactionToCHProcessor)
						.endChoice()
						.when(header("Is-Enabled-Daps-Interaction").isEqualTo(false))
						//						.process(sendToActiveMQ)
						//						.process(receiveFromActiveMQ)
						// Send data to Endpoint B
						.process(sendDataToBusinessLogicProcessor)
						.process(parseReceivedResponseMessage)
						.process(sendResponseToDataAppProcessor)
						.choice()
						.when(header("Is-Enabled-Clearing-House").isEqualTo(true))
						.process(sendTransactionToCHProcessor)
						.endChoice()
						.endChoice();


				// Camel SSL - Endpoint: A - Body form-data
				from("jetty://https4://0.0.0.0:" + configuration.getCamelProducerPort() + "/incoming-data-app/multipartMessageBodyFormData")
						.process(parseReceivedDataProcessorBodyFormData)
						.choice()
						.when(header("Is-Enabled-Daps-Interaction").isEqualTo(true))
						.process(getTokenFromDapsProcessor)
						//						.process(sendToActiveMQ)
						//						.process(receiveFromActiveMQ)
						// Send data to Endpoint B
						.process(sendDataToBusinessLogicProcessor)
						.process(parseReceivedResponseMessage)
						.process(validateTokenProcessor)
						.process(sendResponseToDataAppProcessor)
						.choice()
						.when(header("Is-Enabled-Clearing-House").isEqualTo(true))
						.process(sendTransactionToCHProcessor)
						.endChoice()
						.when(header("Is-Enabled-Daps-Interaction").isEqualTo(false))
						//					.process(sendToActiveMQ)
						//					.process(receiveFromActiveMQ)
						// Send data to Endpoint B
						.process(sendDataToBusinessLogicProcessor)
						.process(parseReceivedResponseMessage)
						.process(sendResponseToDataAppProcessor)
						.choice()
						.when(header("Is-Enabled-Clearing-House").isEqualTo(true))
						.process(sendTransactionToCHProcessor)
						.endChoice()
						.endChoice();
			} else { //IDSCP Enabled
				from("jetty://https4://0.0.0.0:" + configuration.getCamelProducerPort() + "/incoming-data-app/multipartMessageBodyBinary")
						.convertBodyTo(String.class)
						.log("Sent via IDS protocol: ${body}")
						.to(idscpEndpoint);
			}
		} else {
			// End point A. Coomunication between Data App and ECC Producer.
			from("timer://timerEndpointA?fixedRate=true&period=10s") //EndPoint A
					.process(fileRecreatorProcessor)
					.process(parseReceivedDataFromDAppProcessorBodyBinary)
					.choice()
					.when(header("Is-Enabled-Daps-Interaction").isEqualTo(true))
					.process(getTokenFromDapsProcessor)
					// Send data to Endpoint B
					.process(sendDataToBusinessLogicProcessor)
					.process(parseReceivedResponseMessage)
					.process(validateTokenProcessor)
					//.process(sendResponseToDataAppProcessor)
					.process(sendResponseToDataAppProcessor)
					.choice()
					.when(header("Is-Enabled-Clearing-House").isEqualTo(true))
					.process(sendTransactionToCHProcessor)
					.endChoice()
					.when(header("Is-Enabled-Daps-Interaction").isEqualTo(false))
					// Send data to Endpoint B
					.process(sendDataToBusinessLogicProcessor)
					.process(parseReceivedResponseMessage)
					//.process(sendResponseToDataAppProcessor)
					.process(sendResponseToDataAppProcessor)
					.choice()
					.when(header("Is-Enabled-Clearing-House").isEqualTo(true))
					.process(sendTransactionToCHProcessor)
					.endChoice()
					.endChoice();
			//@formatter:on
		}
	}

	@Value("${application.idscp.ttc.host}")
	private String ttcConsumerHost;

	@Value("${application.idscp.ttc.port}")
	private int ttcConsumerPort;

	@Autowired
	private TruststoreConfig truststoreConfig;

	private Endpoint setupIDSCPEndPoint(CamelContext camelContext) throws Exception {
		//TODO Use Spring for this
		Idscp2OsgiComponent idscp2OsgiComponent = new Idscp2OsgiComponent();
		idscp2OsgiComponent.activate();
		idscp2OsgiComponent.setSettings(new Settings() {
			@Override
			public ConnectorConfig getConnectorConfig() {
				return new ConnectorConfig();
			}

			@Override
			public void setConnectorConfig(ConnectorConfig connectorConfig) {

			}

			@Override
			public ConnectorProfile getConnectorProfile() {
				return new ConnectorProfile();
			}

			@Override
			public void setConnectorProfile(ConnectorProfile connectorProfile) {

			}

			@Override
			public String getConnectorJsonLd() {
				return null;
			}

			@Override
			public void setConnectorJsonLd(String s) {

			}

			@Override
			public String getDynamicAttributeToken() {
				return null;
			}

			@Override
			public void setDynamicAttributeToken(String s) {

			}

			@Override
			public ConnectionSettings getConnectionSettings(String s) {
				return null;
			}

			@Override
			public void setConnectionSettings(String s, ConnectionSettings connectionSettings) {

			}

			@Override
			public Map<String, ConnectionSettings> getAllConnectionSettings() {
				return null;
			}
		});
		Idscp2ClientComponent idscp2ClientComponent = camelContext.getComponent("idscp2client", Idscp2ClientComponent.class);
		Idscp2ClientEndpoint idscp2ClientComponentEndpoint = (Idscp2ClientEndpoint) idscp2ClientComponent
				.createEndpoint(("idscp2client://" + ttcConsumerHost + ":" + ttcConsumerPort + "/"));
		idscp2ClientComponentEndpoint.setSslContextParameters(truststoreConfig.setupSSLContextParameters());
		return idscp2ClientComponentEndpoint;
				//wsComponent.createEndpoint("idscp2client://"+ttcConsumerHost+":"+ttcConsumerPort+"/?attestation=0");
	}

}
