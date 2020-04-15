package it.eng.idsa.businesslogic.processor.producer;

import it.eng.idsa.businesslogic.configuration.WebSocketServerConfigurationA;
import it.eng.idsa.businesslogic.processor.consumer.websocket.server.ResponseMessageBufferBean;
import it.eng.idsa.businesslogic.service.impl.MultiPartMessageServiceImpl;
import nl.tno.ids.common.multipart.MultiPart;
import nl.tno.ids.common.multipart.MultiPartMessage;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 
 * @author Milan Karajovic and Gabriele De Luca
 *
 */

@Component
public class ProducerSendResponseToCallerProcessor implements Processor {

	@Value("${application.isEnabledClearingHouse}")
	private boolean isEnabledClearingHouse;

	@Value("${application.idscp.isEnabled}")
	private boolean isEnabledIdscp;

	@Value("${application.websocket.isEnabled}")
	private boolean isEnabledWebSocket;
	
	@Autowired
	private MultiPartMessageServiceImpl multiPartMessageServiceImpl;
	
	@Autowired
	private WebSocketServerConfigurationA webSocketServerConfiguration;
	
	@Override
	public void process(Exchange exchange) throws Exception {
		
		Map<String, Object> headesParts = new HashMap<String, Object>();
		
		Map<String, Object> multipartMessagePartsReceived = exchange.getIn().getBody(HashMap.class);
		
		// Get header, payload and message
		String header = multipartMessagePartsReceived.get("header").toString();
		String payload = null;
		if(multipartMessagePartsReceived.containsKey("payload")) {
			payload = multipartMessagePartsReceived.get("payload").toString();
		}
		
		// Prepare multipart message as string
		HttpEntity entity = multiPartMessageServiceImpl.createMultipartMessage(header, payload, null);
		String responseString = EntityUtils.toString(entity, "UTF-8");
		
		// Return exchange
		MultiPartMessage multiPartMessage = MultiPart.parseString(responseString);
		String multipartMessageString = MultiPart.toString(multiPartMessage, false);
		String contentType = multiPartMessage.getHttpHeaders().getOrDefault("Content-Type", "multipart/mixed");
		headesParts.put("Content-Type", contentType);
		
		// TODO: Send The MultipartMessage message to the WebSocket
		if(isEnabledIdscp || isEnabledWebSocket) { //TODO Try to remove this config property
			ResponseMessageBufferBean responseMessageServerBean = webSocketServerConfiguration.responseMessageBufferWebSocket();
			responseMessageServerBean.add(multipartMessageString.getBytes());
		}
		
		exchange.getOut().setHeaders(headesParts);
		exchange.getOut().setBody(multipartMessageString);
	}	
}
