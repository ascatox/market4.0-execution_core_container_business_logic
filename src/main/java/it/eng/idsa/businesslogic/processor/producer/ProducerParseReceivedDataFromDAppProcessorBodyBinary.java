package it.eng.idsa.businesslogic.processor.producer;

import de.fraunhofer.iais.eis.Message;
import it.eng.idsa.businesslogic.service.impl.MultiPartMessageServiceImpl;
import it.eng.idsa.businesslogic.service.impl.RejectionMessageServiceImpl;
import it.eng.idsa.businesslogic.util.RejectionMessageType;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 
 * @author Antonio Scatoloni
 *
 */

@Component
public class ProducerParseReceivedDataFromDAppProcessorBodyBinary implements Processor {

	private static final Logger logger = LogManager.getLogger(ProducerParseReceivedDataFromDAppProcessorBodyBinary.class);
	public static final String WS_URI = "wss://localhost:8086";

	@Value("${application.isEnabledDapsInteraction}")
	private boolean isEnabledDapsInteraction;

	@Autowired
	private MultiPartMessageServiceImpl multiPartMessageServiceImpl;
	
	@Autowired
	private RejectionMessageServiceImpl rejectionMessageServiceImpl;

	@Override
	public void process(Exchange exchange) throws Exception {

		String contentType;
		String forwardTo;
		String header = null;
		String payload = null;
		Message message = null;
		Map<String, Object> headesParts = new HashMap();
		Map<String, Object> multipartMessageParts = new HashMap();
		String receivedDataBodyBinary = null;

		// Get from the input "exchange"
		Map<String, Object> receivedDataHeader = exchange.getIn().getHeaders();
		try {
			// Create headers parts
			// Put in the header value of the application.property: application.isEnabledDapsInteraction
			headesParts.put("Is-Enabled-Daps-Interaction", isEnabledDapsInteraction);
			contentType = null != receivedDataHeader.get("Content-Type")? receivedDataHeader.get("Content-Type").toString() : null;
			headesParts.put("Content-Type", contentType);

			forwardTo = null != receivedDataHeader.get("Forward-To")? receivedDataHeader.get("Forward-To").toString() : WS_URI;
			headesParts.put("Forward-To", forwardTo);

			// Create multipart message parts
			header = receivedDataHeader.get("header").toString();
			multipartMessageParts.put("header", header);
			payload = receivedDataHeader.get("payload").toString();
			if(payload!=null) {
				multipartMessageParts.put("payload", payload);
			}
			message = multiPartMessageServiceImpl.getMessage(multipartMessageParts.get("header"));
			
			// Return exchange
			exchange.getOut().setHeaders(headesParts);
			exchange.getOut().setBody(multipartMessageParts);

		} catch (Exception e) {
			logger.error("Error parsing multipart message:" + e);
			rejectionMessageServiceImpl.sendRejectionMessage(
					RejectionMessageType.REJECTION_MESSAGE_LOCAL_ISSUES,
					message);
		}

	}

}
