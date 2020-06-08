package it.eng.idsa.businesslogic.processor.exception;

import it.eng.idsa.businesslogic.configuration.WebSocketServerConfigurationB;
import it.eng.idsa.businesslogic.processor.consumer.websocket.server.ResponseMessageBufferBean;
import it.eng.idsa.businesslogic.service.MultipartMessageService;
import it.eng.idsa.multipart.builder.MultipartMessageBuilder;
import it.eng.idsa.multipart.domain.MultipartMessage;
import it.eng.idsa.multipart.processor.MultipartMessageProcessor;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 
 * @author Milan Karajovic and Gabriele De Luca
 *
 */

@Component
public class ExceptionProcessorConsumer implements Processor {
	
	@Autowired
	MultipartMessageService multipartMessageService;

	@Value("${application.idscp.isEnabled}")
	private boolean isEnabledIdscp;

	@Value("${application.websocket.isEnabled}")
	private boolean isEnabledWebSocket;

	@Autowired(required = false)
	private WebSocketServerConfigurationB webSocketServerConfiguration;

	@Override
	public void process(Exchange exchange) throws Exception {
		
		Exception exception = (Exception) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
		String message = multipartMessageService.getHeaderContentString(exception.getMessage());
		
		MultipartMessage multipartMessage = new MultipartMessageBuilder()
    			.withHeaderContent(message)
    			.build();
    	String multipartMessageString = MultipartMessageProcessor.multipartMessagetoString(multipartMessage, false);
//		if(isEnabledIdscp || isEnabledWebSocket) {
//			ResponseMessageBufferBean responseMessageServerBean = webSocketServerConfiguration.responseMessageBufferWebSocket();
//			responseMessageServerBean.add(multipartMessageString.getBytes());
//		}
		exchange.getOut().setBody(multipartMessageString);
		exchange.getOut().setHeader("header", multipartMessageString);
		exchange.getOut().setHeader("payload", "RejectionMessage");

		
	}

}
