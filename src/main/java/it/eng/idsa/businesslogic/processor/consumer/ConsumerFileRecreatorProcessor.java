package it.eng.idsa.businesslogic.processor.consumer;

import de.fraunhofer.iais.eis.Message;
import it.eng.idsa.businesslogic.configuration.WebSocketServerConfigurationB;
import it.eng.idsa.businesslogic.processor.consumer.websocket.server.FileRecreatorBeanServer;
import it.eng.idsa.businesslogic.service.impl.MultiPartMessageServiceImpl;
import it.eng.idsa.businesslogic.service.impl.RejectionMessageServiceImpl;
import it.eng.idsa.businesslogic.util.RejectionMessageType;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * 
 * @author Milan Karajovic and Gabriele De Luca
 *
 */

@Component
@ConditionalOnExpression(
		"${application.websocket.isEnabled:true} or ${application.idscp.isEnabled:true}"
)
public class ConsumerFileRecreatorProcessor implements Processor {
	
	private static final Logger logger = LogManager.getLogger(ConsumerFileRecreatorProcessor.class);
	
	@Autowired
	private WebSocketServerConfigurationB webSocketServerConfiguration;
	
	@Autowired
	private MultiPartMessageServiceImpl multiPartMessageServiceImpl;
	
	@Autowired
	private RejectionMessageServiceImpl rejectionMessageServiceImpl;


	@Override
	public void process(Exchange exchange) throws Exception {
		
		String header;
		String payload;
		Message message=null;
		Map<String, Object> multipartMessageParts = new HashMap<String, Object>();

		//  Receive and recreate Multipart message
		FileRecreatorBeanServer fileRecreatorBean = webSocketServerConfiguration.fileRecreatorBeanWebSocket();
		this.initializeServer(message, fileRecreatorBean);
		Thread fileRecreatorBeanThread = new Thread(fileRecreatorBean, "FileRecreator_"+this.getClass().getSimpleName());
		fileRecreatorBeanThread.start();
		String recreatedMultipartMessage = webSocketServerConfiguration.recreatedMultipartMessageBeanWebSocket().remove();
		
		// Extract header and payload from the multipart message
		try {
			header = multiPartMessageServiceImpl.getHeaderContentString(recreatedMultipartMessage);
			multipartMessageParts.put("header", header);
			payload = multiPartMessageServiceImpl.getPayloadContent(recreatedMultipartMessage);
			if(payload!=null) {
				multipartMessageParts.put("payload", payload);
			}
		} catch (Exception e) {
			logger.error("Error parsing multipart message:" + e);
			// TODO: Send WebSocket rejection message
			
		}
		
		// Return exchange
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
