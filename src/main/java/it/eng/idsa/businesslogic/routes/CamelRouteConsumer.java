package it.eng.idsa.businesslogic.routes;

import de.fhg.aisec.ids.api.infomodel.ConnectorProfile;
import de.fhg.aisec.ids.api.settings.ConnectionSettings;
import de.fhg.aisec.ids.api.settings.ConnectorConfig;
import de.fhg.aisec.ids.api.settings.Settings;
import de.fhg.aisec.ids.camel.idscp2.Idscp2OsgiComponent;
import de.fhg.aisec.ids.camel.idscp2.server.Idscp2ServerComponent;
import de.fhg.aisec.ids.camel.idscp2.server.Idscp2ServerEndpoint;
import it.eng.idsa.businesslogic.configuration.ApplicationConfiguration;
import it.eng.idsa.businesslogic.processor.consumer.*;
import it.eng.idsa.businesslogic.processor.exception.ExceptionForProcessor;
import it.eng.idsa.businesslogic.processor.exception.ExceptionProcessorConsumer;
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
public class CamelRouteConsumer extends RouteBuilder {

	private static final Logger logger = LogManager.getLogger(CamelRouteConsumer.class);

	@Autowired
	private ApplicationConfiguration configuration;

	@Autowired(required = false)
	ConsumerFileRecreatorProcessor fileRecreatorProcessor;

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

	@Autowired
	ConsumerWebSocketSendDataToDataAppProcessor sendDataToDataAppProcessorOverWS;

	@Autowired
	CamelContext camelContext;

	@Value("${application.idscp.isEnabled}")
	private boolean isEnabledIdscp;

	@Value("${application.websocket.isEnabled}")
	private boolean isEnabledWebSocket;

	@Value("${application.idscp.server.port}")
	private int idscPort;

	@Override
	public void configure() throws Exception {
		logger.debug("Starting Camel Routes...consumer side");
		camelContext.getShutdownStrategy().setLogInflightExchangesOnTimeout(false);
		camelContext.getShutdownStrategy().setTimeout(3);

		Endpoint idscpEndpoint = setupIDSCPEndPoint(getContext());

		//@formatter:off
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
		if(!isEnabledIdscp && !isEnabledWebSocket) {
			from("jetty://https4://0.0.0.0:" + configuration.getCamelConsumerPort() + "/incoming-data-channel/receivedMessage")
					.process(multiPartMessageProcessor)
					.choice()
					.when(header("Is-Enabled-Daps-Interaction").isEqualTo(true))
					.process(validateTokenProcessor)
					//.process(sendToActiveMQ)
					//.process(receiveFromActiveMQ)
					// Send to the Endpoint: F
					.choice()
					.when(header("Is-Enabled-DataApp-WebSocket").isEqualTo(true))
					.process(sendDataToDataAppProcessorOverWS)
					.when(header("Is-Enabled-DataApp-WebSocket").isEqualTo(false))
					.process(sendDataToDataAppProcessor)
					.endChoice()
					.process(multiPartMessageProcessor)
					.process(getTokenFromDapsProcessor)
					.process(sendDataToBusinessLogicProcessor)
					.choice()
					.when(header("Is-Enabled-Clearing-House").isEqualTo(true))
					.process(sendTransactionToCHProcessor)
					.endChoice()
					.when(header("Is-Enabled-Daps-Interaction").isEqualTo(false))
					// Send to the Endpoint: F
					.choice()
					.when(header("Is-Enabled-DataApp-WebSocket").isEqualTo(true))
					.process(sendDataToDataAppProcessorOverWS)
					.when(header("Is-Enabled-DataApp-WebSocket").isEqualTo(false))
					.process(sendDataToDataAppProcessor)
					.endChoice()
					.process(multiPartMessageProcessor)
					.process(sendDataToBusinessLogicProcessor)
					.choice()
					.when(header("Is-Enabled-Clearing-House").isEqualTo(true))
					//.process(sendTransactionToCHProcessor)
					.endChoice()
					.endChoice();
		} else if (isEnabledWebSocket) {
			// End point B. ECC communication (Web Socket or IDSCP)
			from("timer://timerEndpointB?fixedRate=true&period=10s") //EndPoint B
					.process(fileRecreatorProcessor)
					.process(multiPartMessageProcessor)
					.choice()
					.when(header("Is-Enabled-Daps-Interaction").isEqualTo(true))
					.process(validateTokenProcessor)
					// Send to the Endpoint: F
					.choice()
					.when(header("Is-Enabled-DataApp-WebSocket").isEqualTo(true))
					.process(sendDataToDataAppProcessorOverWS)
					.when(header("Is-Enabled-DataApp-WebSocket").isEqualTo(false))
					.process(sendDataToDataAppProcessor)
					.endChoice()
					.process(multiPartMessageProcessor)
					.process(getTokenFromDapsProcessor)
					.process(sendDataToBusinessLogicProcessor)
					.choice()
					.when(header("Is-Enabled-Clearing-House").isEqualTo(true))
					.process(sendTransactionToCHProcessor)
					.endChoice()
					.when(header("Is-Enabled-Daps-Interaction").isEqualTo(false))
					// Send to the Endpoint: F
					.choice()
					.when(header("Is-Enabled-DataApp-WebSocket").isEqualTo(true))
					.process(sendDataToDataAppProcessorOverWS)
					.when(header("Is-Enabled-DataApp-WebSocket").isEqualTo(false))
					.process(sendDataToDataAppProcessor)
					.endChoice()
					.process(multiPartMessageProcessor)
					.process(sendDataToBusinessLogicProcessor)
					.choice()
					.when(header("Is-Enabled-Clearing-House").isEqualTo(true))
					// .process(sendTransactionToCHProcessor)
					.endChoice()
					.endChoice();
		} else if(isEnabledIdscp) {
			from(idscpEndpoint)
					.log("Received via IDS protocol: ${body}")
					.to("cxf://http://consumer-app:8081/temp?dataFormat=MESSAGE");
					//.to("jetty://https4://data-app:8083/incoming-data-app/dataAppIncomingMessageSender");
			//@formatter:on
		}

	}

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
		Idscp2ServerComponent idscp2ServerComponent = camelContext.getComponent("idscp2server", Idscp2ServerComponent.class);
		Idscp2ServerEndpoint idscp2ServerComponentEndpoint = (Idscp2ServerEndpoint) idscp2ServerComponent
				.createEndpoint("idscp2server://0.0.0.0:" + idscPort + "/");
		idscp2ServerComponentEndpoint.setSslContextParameters(truststoreConfig.setupSSLContextParameters());
		return idscp2ServerComponentEndpoint;
	}


}