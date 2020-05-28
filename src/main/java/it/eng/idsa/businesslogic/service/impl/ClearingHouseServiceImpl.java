/**
 * 
 */
package it.eng.idsa.businesslogic.service.impl;

import de.fraunhofer.iais.eis.LogNotification;
import de.fraunhofer.iais.eis.LogNotificationBuilder;
import de.fraunhofer.iais.eis.Message;
import de.fraunhofer.iais.eis.ids.jsonld.Serializer;
import it.eng.idsa.businesslogic.configuration.ApplicationConfiguration;
import it.eng.idsa.businesslogic.service.ClearingHouseService;
import it.eng.idsa.businesslogic.service.HashFileService;
import it.eng.idsa.businesslogic.service.RejectionMessageService;
import it.eng.idsa.businesslogic.util.RejectionMessageType;
import it.eng.idsa.clearinghouse.model.Body;
import it.eng.idsa.clearinghouse.model.NotificationContent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JsonObject;
import org.json.simple.Jsoner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.*;


/**
 * @author Milan Karajovic and Gabriele De Luca
 *
 */

@Service
@Transactional
public class ClearingHouseServiceImpl implements ClearingHouseService {
	@Autowired
	private ApplicationConfiguration configuration;

	private static final Logger logger = LogManager.getLogger(ClearingHouseServiceImpl.class);
	private final static String informationModelVersion = getInformationModelVersion();

	private static URI connectorURI;

	@Autowired
	private HashFileService hashService;

	@Autowired
	private RejectionMessageService rejectionMessageService;

	//@Autowired
	//private RetryTemplate retryTemplate;


	@Override
	public boolean registerTransaction(Message correlatedMessage, String payload) {
		// TODO Auto-generated method stub
		try {
			logger.debug("registerTransaction...");
			try {
				connectorURI = new URI(configuration.getUriSchema() + configuration.getUriAuthority() + configuration.getUriConnector() + UUID.randomUUID().toString());
			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			String endpoint = configuration.getClearingHouseUrl();
			RestTemplate restTemplate = new RestTemplate();

			NotificationContent notificationContent = createNotificationContent(correlatedMessage, payload);
			String hash = notificationContent.getBody().getPayload();
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			String msgSerialized = new Serializer().serializePlainJson(notificationContent);
			logger.info("msgSerialized to CH=" + msgSerialized);
			JsonObject jsonObject = (JsonObject) Jsoner.deserialize(msgSerialized);

			//JSONParser parser = new JSONParser();
			//JSONObject jsonObject = (JSONObject) parser.parse(msgSerialized);

			HttpEntity<JsonObject> entity = new HttpEntity<>(jsonObject, headers);

			logger.info("Sending Data to the Clearing House " + endpoint + " ...");
			restTemplate.postForObject(endpoint, entity, String.class);
			logger.info("Data [LogNotitication.id=" + notificationContent.getHeader().getId() + "] sent to the Clearing House " + endpoint);
			hashService.recordHash(hash, payload, notificationContent);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public boolean validateData(Message message, String payload) {
		NotificationContent notificationContent = null;
		try {
			notificationContent = createNotificationContent(message, payload);
			final String id = notificationContent.getBody().getHeader().getId().toString();
			RestTemplate restTemplate = new RestTemplate();
//			ResponseEntity<NotificationContent[]> notificationContentResponse = retryTemplate.execute(context ->
//					getNotificationContentByCH(id, restTemplate));
			ResponseEntity<NotificationContent[]> notificationContentResponse = getNotificationContentByCH(id, restTemplate);
			if (notificationContentResponse.getStatusCode().isError() ||
					notificationContentResponse.getBody() == null ||
					notificationContentResponse.getBody().length == 0) {
				logger.error("ClearingHouse Service Verification! Data retrieved from CH at " + configuration.getClearingHouseUrl() + " is EMPTY!");
				rejectionMessageService.sendRejectionMessage(
						RejectionMessageType.REJECTION_CH_VALIDATION_LOCAL_ISSUES,
						message);
			}
			NotificationContent[] notificationContents = notificationContentResponse.getBody();
			List<NotificationContent> notificationContentList = Arrays.asList(notificationContents);
			Collections.sort(notificationContentList, new Comparator<NotificationContent>() {
						//Sort with Issued Date DESC
						@Override
						public int compare(NotificationContent o1, NotificationContent o2) {
							GregorianCalendar issued1 = o1.getHeader().getIssued().toGregorianCalendar();
							GregorianCalendar issued2 = o2.getHeader().getIssued().toGregorianCalendar();
							if(issued1.before(issued2))
								return 1;
							else if(issued1.equals(issued2))
								return 0;
							else
								return -1;
						}
					});
			NotificationContent notificationContentRet = notificationContentList.get(0); //Take the most recent
			if (notificationContentRet.getBody().getPayload().equals(notificationContent.getBody().getPayload())) {
				return true;
			}
		} catch (Exception e) {
			logger.error("ClearingHouse Service Verification! Error: " + e.getMessage());
			rejectionMessageService.sendRejectionMessage(
					RejectionMessageType.REJECTION_CH_VALIDATION_LOCAL_ISSUES,
					message);
		}
		return false;
	}

	public ResponseEntity<NotificationContent[]> getNotificationContentByCH(String id, RestTemplate restTemplate) {
		try {
			logger.info("getNotificationContentByCH at: "+ LocalDateTime.now().toString());
			String endpoint = configuration.getClearingHouseUrl();
			UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(endpoint + "/correlated")
					.queryParam("Id", id);
			return restTemplate.getForEntity(uriBuilder.toUriString(),
					NotificationContent[].class);
		} catch (HttpServerErrorException e) {
			throw new RuntimeException(e);
		}
	}

	private static String getInformationModelVersion() {
		return "2.1.0-SNAPSHOT";
	}
		/*String currnetInformationModelVersion = null;
		try {
	
			InputStream is = RejectionMessageServiceImpl.class.getClassLoader().getResourceAsStream("META-INF/maven/it.eng.idsa/market4.0-execution_core_container_business_logic/pom.xml");
			MavenXpp3Reader reader = new MavenXpp3Reader();
			Model model = reader.read(is);
			MavenProject project = new MavenProject(model);
			Properties props = project.getProperties(); 
			if (props.get("information.model.version")!=null) {
				return props.get("information.model.version").toString();
			}
			for (int i = 0; i < model.getDependencies().size(); i++) {
				if (model.getDependencies().get(i).getGroupId().equalsIgnoreCase("de.fraunhofer.iais.eis.ids.infomodel")){
					String version=model.getDependencies().get(i).getVersion();
					// If we want, we can delete "-SNAPSHOT" from the version
					//					if (version.contains("-SNAPSHOT")) {
					//						version=version.substring(0,version.indexOf("-SNAPSHOT"));
					//					}
					currnetInformationModelVersion=version;
				}
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
		return currnetInformationModelVersion;
	}*/

	private URI whoIAm() {
		//TODO 
		return URI.create("auto-generated");
	}

	private NotificationContent createNotificationContent(Message correlatedMessage, String payload) throws Exception {
		//Create Message for Clearing House
		GregorianCalendar gcal = new GregorianCalendar();
		XMLGregorianCalendar xgcal = DatatypeFactory.newInstance()
				.newXMLGregorianCalendar(gcal);
		gcal = new GregorianCalendar();
		xgcal = DatatypeFactory.newInstance()
				.newXMLGregorianCalendar(gcal);
		ArrayList<URI> recipientConnectors = new ArrayList<URI>();
		recipientConnectors.add(connectorURI);

		LogNotification logNotification = new LogNotificationBuilder()
				._modelVersion_(informationModelVersion)
				._issuerConnector_(whoIAm())
				._issued_(xgcal).build();

		NotificationContent notificationContent = new NotificationContent();
		notificationContent.setHeader(logNotification);
		Body body = new Body();
		body.setHeader(correlatedMessage);
		String hash = hashService.hash(payload);
		body.setPayload(hash);
		notificationContent.setBody(body);
		return notificationContent;
	}
}