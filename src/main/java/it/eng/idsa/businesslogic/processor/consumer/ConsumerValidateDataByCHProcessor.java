package it.eng.idsa.businesslogic.processor.consumer;

import de.fraunhofer.iais.eis.Message;
import it.eng.idsa.businesslogic.service.ClearingHouseService;
import it.eng.idsa.businesslogic.service.MultipartMessageService;
import it.eng.idsa.businesslogic.service.RejectionMessageService;
import it.eng.idsa.businesslogic.util.RejectionMessageType;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 
 * @author Antonio Scatoloni at 04/05/2020
 *
 */

@Component
public class ConsumerValidateDataByCHProcessor implements Processor {
	private static final Logger logger = LogManager.getLogger(ConsumerValidateDataByCHProcessor.class);

	@Autowired
	private MultipartMessageService multipartMessageService;

	@Autowired
	private ClearingHouseService clearingHouseService;

	@Autowired
	private RejectionMessageService rejectionMessageService;

	@Override
	public void process(Exchange exchange) throws Exception {

		// Prepare data for CH
		Map<String, Object> multipartMessageParts = exchange.getIn().getBody(HashMap.class);
		Message message = multipartMessageService.getMessage(multipartMessageParts.get("header"));
		String payload = multipartMessageParts.get("payload").toString();
		// Validate data to CH
		boolean match = clearingHouseService.validateData(message, payload);
		if(match == false) {
			logger.error("Data Don't MATCH with Clearing House registered information!");
			rejectionMessageService.sendRejectionMessage(
					RejectionMessageType.REJECTION_CH_VALIDATION,
					message);
		}
		exchange.getOut().setHeaders(exchange.getIn().getHeaders());
		exchange.getOut().setBody(exchange.getIn().getBody());
	}

}
