package it.eng.idsa.businesslogic.processor.consumer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import it.eng.idsa.businesslogic.configuration.CommunicationRole;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import de.fraunhofer.iais.eis.Message;
import it.eng.idsa.businesslogic.configuration.WebSocketServerConfiguration;
import it.eng.idsa.businesslogic.processor.consumer.websocket.server.FileRecreatorBeanServer;
import it.eng.idsa.businesslogic.service.impl.MultiPartMessageServiceImpl;
import it.eng.idsa.businesslogic.service.impl.RejectionMessageServiceImpl;
import it.eng.idsa.businesslogic.util.RejectionMessageType;

/**
 * 
 * @author Milan Karajovic and Gabriele De Luca
 *
 */

@Component
public class ConsumerFileRecreatorProcessor implements Processor {
	
	private static final Logger logger = LogManager.getLogger(ConsumerFileRecreatorProcessor.class);
	
	@Autowired
	private WebSocketServerConfiguration webSocketServerConfiguration;
	
	@Autowired
	private MultiPartMessageServiceImpl multiPartMessageServiceImpl;
	
	@Autowired
	private RejectionMessageServiceImpl rejectionMessageServiceImpl;


	@Value("${spring.profiles.active:}")
	private String activeProfiles;

	@Override
	public void process(Exchange exchange) throws Exception {
		
		String header;
		String payload;
		Message message=null;
		Map<String, Object> headesParts = new HashMap();
		Map<String, Object> multipartMessageParts = new HashMap();
		
		//  Receive and recreate Multipart message
		Optional<String> profile = Arrays.stream(activeProfiles.split(",")).findFirst();
		CommunicationRole communicationRole = CommunicationRole.valueOf(profile.get());
		FileRecreatorBeanServer fileRecreatorBean = webSocketServerConfiguration.fileRecreatorBeanWebSocket();
		fileRecreatorBean.setCommunicationRole(communicationRole);
		this.initializeServer(message, fileRecreatorBean);
		Thread fileRecreatorBeanThread = new Thread(fileRecreatorBean, "FileRecreator");
		fileRecreatorBeanThread.start();
		String recreatedMultipartMessage = webSocketServerConfiguration.recreatedMultipartMessageBeanWebSocket().remove();
		
		// Extract header and payload from the multipart message
		try {
			header = multiPartMessageServiceImpl.getHeader(recreatedMultipartMessage);
			multipartMessageParts.put("header", header);
			payload = multiPartMessageServiceImpl.getPayload(recreatedMultipartMessage);
			if(payload!=null) {
				multipartMessageParts.put("payload", payload);
			}
		} catch (Exception e) {
			logger.error("Error parsing multipart message:" + e);
			// TODO: Send WebSocket rejection message
			
		}
		
		// Return exchange
		multipartMessageParts.put(CommunicationRole.class.getSimpleName(), profile.get());
		exchange.getOut().setHeaders(multipartMessageParts);
	}

	private void initializeServer(Message message, FileRecreatorBeanServer fileRecreatorBean) {
		try {
			fileRecreatorBean.setup();
		} catch(Exception e) {
			logger.info("... can not initilize the IdscpServer");
			rejectionMessageServiceImpl.sendRejectionMessage(
					RejectionMessageType.REJECTION_COMMUNICATION_LOCAL_ISSUES, 
					message);
		}
	}

}
