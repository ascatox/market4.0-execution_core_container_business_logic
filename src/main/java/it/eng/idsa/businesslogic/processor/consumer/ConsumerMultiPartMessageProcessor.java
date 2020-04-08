package it.eng.idsa.businesslogic.processor.consumer;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import de.fraunhofer.iais.eis.Message;
import it.eng.idsa.businesslogic.service.impl.MultiPartMessageServiceImpl;
import it.eng.idsa.businesslogic.service.impl.RejectionMessageServiceImpl;
import it.eng.idsa.businesslogic.util.RejectionMessageType;

/**
 * 
 * @author Milan Karajovic and Gabriele De Luca
 *
 */

@Component
public class ConsumerMultiPartMessageProcessor implements Processor {
	
	private static final Logger logger = LogManager.getLogger(ConsumerMultiPartMessageProcessor.class);
	
	@Value("${application.isEnabledDapsInteraction}")
	private boolean isEnabledDapsInteraction;

	@Value("${application.websocket.isEnabled}")
	private boolean isEnabledWebSocket;

	@Autowired
	private MultiPartMessageServiceImpl multiPartMessageServiceImpl;
	
	@Autowired
	private RejectionMessageServiceImpl rejectionMessageServiceImpl;


	@Override
	public void process(Exchange exchange) throws Exception {
		
		String header;
		String payload;
		Message message=null;
		Map<String, Object> headesParts = new HashMap();
		Map<String, Object> multipartMessageParts = new HashMap();
		
		if(!exchange.getIn().getHeaders().containsKey("header"))
		{
			logger.error("Multipart message header is null");
			rejectionMessageServiceImpl.sendRejectionMessage(
					RejectionMessageType.REJECTION_MESSAGE_COMMON, 
					message);
		}
		try {
			
			// Create headers parts
			// Put in the header value of the application.property: application.isEnabledDapsInteraction
			headesParts.put("Is-Enabled-Daps-Interaction", isEnabledDapsInteraction);

			if(exchange.getIn().getHeaders().containsKey("payload")) {
				payload=exchange.getIn().getHeader("payload").toString();
				if(payload.equals("RejectionMessage")) {
					// Create multipart message for the RejectionMessage
					header= multiPartMessageServiceImpl.getHeader(exchange.getIn().getHeader("header").toString());
					multipartMessageParts.put("header", header);
				} else {
					// Create multipart message with payload
					header=exchange.getIn().getHeader("header").toString();
					multipartMessageParts.put("header", header);
					payload=exchange.getIn().getHeader("payload").toString();
					multipartMessageParts.put("payload", payload);
					message=multiPartMessageServiceImpl.getMessage(multipartMessageParts.get("header"));
				}
			}else {
				// Create multipart message without payload
				header=exchange.getIn().getHeader("header").toString();
				multipartMessageParts.put("header", header);
				message=multiPartMessageServiceImpl.getMessage(multipartMessageParts.get("header"));
			}

			headesParts.put("Is-Enabled-WebSocket", isEnabledWebSocket);
			// Return exchange
			exchange.getOut().setHeaders(headesParts);
			exchange.getOut().setBody(multipartMessageParts);
			
		} catch (Exception e) {
			logger.error("Error parsing multipart message:" + e);
			rejectionMessageServiceImpl.sendRejectionMessage(
					RejectionMessageType.REJECTION_MESSAGE_COMMON, 
					message);
		}
	}
	
}
