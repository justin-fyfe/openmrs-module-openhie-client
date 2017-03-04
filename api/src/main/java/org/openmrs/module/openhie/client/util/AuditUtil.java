package org.openmrs.module.openhie.client.util;

import java.lang.management.ManagementFactory;
import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dcm4che3.audit.ActiveParticipant;
import org.dcm4che3.audit.AuditMessage;
import org.dcm4che3.audit.AuditMessages;
import org.dcm4che3.audit.AuditMessages.AuditSourceTypeCode;
import org.dcm4che3.audit.AuditMessages.EventID;
import org.dcm4che3.audit.AuditMessages.EventTypeCode;
import org.dcm4che3.audit.AuditMessages.ParticipantObjectIDTypeCode;
import org.dcm4che3.audit.AuditMessages.RoleIDCode;
import org.dcm4chee.xds2.common.XDSConstants;
import org.dcm4chee.xds2.infoset.ihe.ProvideAndRegisterDocumentSetRequestType;
import org.dcm4chee.xds2.infoset.ihe.RetrieveDocumentSetResponseType;
import org.dcm4chee.xds2.infoset.ihe.RetrieveDocumentSetResponseType.DocumentResponse;
import org.dcm4chee.xds2.infoset.rim.RegistryPackageType;
import org.dcm4chee.xds2.infoset.rim.RegistryResponseType;
import org.dcm4chee.xds2.infoset.util.InfosetUtil;
import org.openmrs.ImplementationId;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.Role;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.module.openhie.client.configuration.CdaHandlerConfiguration;
import org.openmrs.module.openhie.client.exception.HealthInformationExchangeException;
import org.openmrs.module.shr.atna.api.AtnaAuditService;
import org.openmrs.module.shr.atna.configuration.AtnaConfiguration;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v25.message.QBP_Q21;
import ca.uhn.hl7v2.parser.PipeParser;
import ca.uhn.hl7v2.util.Terser;

/**
 * Audit utility for client audits
 * @author JustinFyfe
 *
 */
public class AuditUtil {

	
	// Instance of the audit utility
	private static AuditUtil s_instance;
	private static Object s_lock = new Object();
	private CdaHandlerConfiguration m_configuration = CdaHandlerConfiguration.getInstance();
	private AtnaAuditService m_auditService = Context.getService(AtnaAuditService.class);
	private Log log = LogFactory.getLog(this.getClass());

	/**
	 * Audit utility ctor
	 */
	private AuditUtil() {
		log.debug(String.format("Audit Service %s", this.m_auditService));
		log.debug(String.format("Audit Logger: %s", this.m_auditService.getLogger()));
	}
	
	/**
	 * Get the instance of the audit utility
	 * @return
	 */
	public static AuditUtil getInstance() {
		if(s_instance == null)
			synchronized(s_lock)
			{
				if(s_instance == null)
				{
					s_instance = new AuditUtil();
				}
			}
		return s_instance;
	}
	
	
	/**
	 * Create audit message
	 * @return
	 */
	private AuditMessage createAuditMessageQuery(EventTypeCode eventType, boolean success)
	{
		AuditMessage retVal = new AuditMessage();
		retVal.setEventIdentification(AuditMessages.createEventIdentification(EventID.Query, "E", Calendar.getInstance(), success ? "0" : "12", success ? "Success" : "Failure", eventType));
		
		ImplementationId implementation = Context.getAdministrationService().getImplementationId();

		if(implementation == null)
		{
			implementation = new ImplementationId();
			implementation.setName(AtnaConfiguration.getInstance().getDeviceName());
			implementation.setImplementationId(AtnaConfiguration.getInstance().getDeviceName());
		}

		Location defaultLocation = Context.getLocationService().getDefaultLocation();
		
		retVal.getAuditSourceIdentification().add(AuditMessages.createAuditSourceIdentification(defaultLocation.getName(), implementation.getImplementationId(), AuditSourceTypeCode.WebServerProcess));
		return retVal;
	}

	/**
	 * Create the human requestor
	 * @return
	 */
	private ActiveParticipant createHumanRequestor()
	{
		User currentUser = Context.getAuthenticatedUser();
		ImplementationId implementation = Context.getAdministrationService().getImplementationId();
		if(implementation == null)
		{
			implementation = new ImplementationId();
			implementation.setName("ANON");
			implementation.setImplementationId("ANON");
		}

		String altUserId = String.format("%s\\%s", implementation.getImplementationId(), currentUser.getName()),
				userName = String.format("%s, %s", currentUser.getFamilyName(), currentUser.getGivenName());
		List<RoleIDCode> roles = new ArrayList<AuditMessages.RoleIDCode>();
		for(Role rol : currentUser.getAllRoles())
			roles.add(new RoleIDCode(rol.getName(), null, null));
		
		return AuditMessages.createActiveParticipant(currentUser.getUsername(), altUserId, userName, true, null, null, null, roles.toArray(new RoleIDCode[] {}));
	}
	
	/**
	 * Create source participant
	 * @return
	 */
	private ActiveParticipant createSourceParticipant(String userId) {
		return AuditMessages.createActiveParticipant(
				userId, 
				ManagementFactory.getRuntimeMXBean().getName(), 
				null, 
				true, 
				AtnaConfiguration.getInstance().getLocalBindAddress(), 
				"2", 
				null, 
				RoleIDCode.Source
			) ;
	}
	
	/**
	 * Create patient search 
	 * @return
	 */
	public AuditMessage createPatientSearch(List<Patient> results, String remoteHost, QBP_Q21 query)
	{
		AuditMessage retVal =  this.createAuditMessageQuery(EventTypeCode.ITI_21_PatientDemographicsQuery, results != null && results.size() > 0);
		retVal.getActiveParticipant().add(this.createHumanRequestor());
		retVal.getActiveParticipant().add(this.createSourceParticipant(String.format("%s|%s", query.getMSH().getSendingApplication().getNamespaceID(), query.getMSH().getSendingFacility().getNamespaceID())));
		retVal.getActiveParticipant().add(AuditMessages.createActiveParticipant(String.format("%s|%s", query.getMSH().getReceivingApplication().getNamespaceID(), query.getMSH().getReceivingFacility().getNamespaceID()), null, null, false, remoteHost, "1", null, AuditMessages.RoleIDCode.Destination));
		
		// Add objects
		PipeParser parser = new PipeParser();
		Terser terser = new Terser(query);
		try {
			retVal.getParticipantObjectIdentification().add(
					AuditMessages.createParticipantObjectIdentification(
							query.getMSH().getMessageControlID().getValue(), 
							new ParticipantObjectIDTypeCode("ITI-21", "IHE Transactions", "Patient Demographics Query"), 
							null, 
							parser.encode(query).getBytes(), 
							"2", 
							"24", 
							null, 
							null, 
							null, 
							AuditMessages.createParticipantObjectDetail("MSH-10", query.getMSH().getMessageControlID().getValue().getBytes())
						));
		} catch (HL7Exception e) {
			log.error("Error constructing query:", e);
		}
		
		// Results
		if(results != null)
			for(Patient res : results)
			{
				if(res == null ||
						res.getPatientIdentifier() == null)
					continue;
				retVal.getParticipantObjectIdentification().add(
					AuditMessages.createParticipantObjectIdentification(
							String.format("%s^^^&%s&ISO", res.getPatientIdentifier().getIdentifier(), res.getPatientIdentifier().getIdentifierType().getUuid()), 
							ParticipantObjectIDTypeCode.PatientNumber, 
							null, 
							null, 
							"1", 
							"1", 
							null, 
							null, 
							null, 
							AuditMessages.createParticipantObjectDetail("MSH-10", query.getMSH().getMessageControlID().getValue().getBytes())	
					)
				);
				
			}
		
		return retVal;
	}

	/**
	 * Create the fetch document request
	 */
	public AuditMessage createFetchDocument(RetrieveDocumentSetResponseType response, String xdsRepositoryEndpoint) {
		
		URI uri = URI.create(xdsRepositoryEndpoint);
		
		AuditMessage retVal =  this.createAuditMessageQuery(EventTypeCode.ITI_43_RetrieveDocumentSet, response != null && response.getRegistryResponse().getStatus().equals(XDSConstants.XDS_B_STATUS_SUCCESS));
		retVal.getActiveParticipant().add(this.createHumanRequestor());
		retVal.getActiveParticipant().add(this.createSourceParticipant("http://anonymous"));
		retVal.getActiveParticipant().add(AuditMessages.createActiveParticipant(null, null, null, false, uri.getHost(), "1", null, AuditMessages.RoleIDCode.Destination));
		
		if(response != null)
			for(DocumentResponse result : response.getDocumentResponse())
			{
				retVal.getParticipantObjectIdentification().add(
					AuditMessages.createParticipantObjectIdentification(
							result.getDocumentUniqueId(), 
							ParticipantObjectIDTypeCode.ReportNumber,
							null, 
							null, 
							"2", 
							"3", 
							null, 
							null, 
							null, 
							AuditMessages.createParticipantObjectDetail("Repository Unique Id", result.getRepositoryUniqueId().getBytes())
						));
			}
		
		return retVal;
		
	}

	/**
	 * Create export document audit
	 */
	public AuditMessage createExportDocument(ProvideAndRegisterDocumentSetRequestType request,
			RegistryResponseType response, String xdsRepositoryEndpoint) {
		URI uri = URI.create(xdsRepositoryEndpoint);
		
		AuditMessage retVal =  this.createAuditMessageQuery(EventTypeCode.ITI_41_ProvideAndRegisterDocumentSetB, response != null && response.getStatus().equals(XDSConstants.XDS_B_STATUS_SUCCESS));
		retVal.getActiveParticipant().add(this.createHumanRequestor());
		retVal.getActiveParticipant().add(this.createSourceParticipant("http://anonymous"));
		retVal.getActiveParticipant().add(AuditMessages.createActiveParticipant(null, null, null, false, uri.getHost(), "1", null, AuditMessages.RoleIDCode.Destination));

		RegistryPackageType pack = InfosetUtil.getRegistryPackage(request.getSubmitObjectsRequest(), XDSConstants.UUID_XDSSubmissionSet);
		if(pack != null)
			retVal.getParticipantObjectIdentification().add(
				AuditMessages.createParticipantObjectIdentification(
						InfosetUtil.getExternalIdentifierValue(XDSConstants.UUID_XDSSubmissionSet_uniqueId, pack), 
						new ParticipantObjectIDTypeCode(XDSConstants.UUID_XDSSubmissionSet, "IHE XDS Metadata", "submission set classificationNode"),
						null, 
						null, 
						"2", 
						"20", 
						null, 
						null, 
						null 
					));
		
		return retVal;
	}
	
	/**
	 * Create patient search 
	 * @return
	 * @throws HealthInformationExchangeException 
	 */
	public AuditMessage createPatientResolve(List<Patient> results, String remoteHost, Message query) throws HealthInformationExchangeException
	{
		try
		{
			Terser terser = new Terser(query);
			AuditMessage retVal =  this.createAuditMessageQuery(EventTypeCode.ITI_9_PIXQuery, results != null && results.size() > 0);
			retVal.getActiveParticipant().add(this.createHumanRequestor());
			retVal.getActiveParticipant().add(this.createSourceParticipant(String.format("%s|%s", terser.get("/MSH-3"), terser.get("/MSH-4"))));
			retVal.getActiveParticipant().add(AuditMessages.createActiveParticipant(String.format("%s|%s", terser.get("/MSH-5"), terser.get("/MSH-6")), null, null, false, remoteHost, "1", null, AuditMessages.RoleIDCode.Destination));
			
			// Add objects
			PipeParser parser = new PipeParser();
			try {
				retVal.getParticipantObjectIdentification().add(
						AuditMessages.createParticipantObjectIdentification(
								terser.get("/MSH-10"), 
								new ParticipantObjectIDTypeCode("ITI-9", "IHE Transactions", "PIX Query"), 
								null, 
								parser.encode(query).getBytes(), 
								"2", 
								"24", 
								null, 
								null, 
								null, 
								AuditMessages.createParticipantObjectDetail("MSH-10", terser.get("/MSH-10").getBytes())
							));
			} catch (HL7Exception e) {
				log.error("Error constructing query:", e);
			}
			
			// Results
			if(results != null)
				for(Patient res : results)
				{
					if(res == null ||
							res.getPatientIdentifier() == null)
						continue;
					retVal.getParticipantObjectIdentification().add(
						AuditMessages.createParticipantObjectIdentification(
								String.format("%s^^^&%s&ISO", res.getPatientIdentifier().getIdentifier(), res.getPatientIdentifier().getIdentifierType().getUuid()), 
								ParticipantObjectIDTypeCode.PatientNumber, 
								null, 
								null, 
								"1", 
								"1", 
								null, 
								null, 
								null
						)
					);
					
				}
			
			return retVal;
		}
		catch(Exception e)
		{
			log.error("Error creating audit", e);
			throw new HealthInformationExchangeException("Error creating audit", e);
		}
	}

	/**
	 * Create patient search 
	 * @return
	 * @throws HealthInformationExchangeException 
	 */
	public AuditMessage createPatientAdmit(Patient patient, String remoteHost, Message query, Boolean success) throws HealthInformationExchangeException
	{
		try
		{
			Terser terser = new Terser(query);
			AuditMessage retVal =  this.createAuditMessageQuery(EventTypeCode.ITI_8_PatientIdentityFeed, success);
			if(terser.get("/MSH-9-2").equals("A08"))
				retVal.getEventIdentification().setEventActionCode("U");
			else
				retVal.getEventIdentification().setEventActionCode("C");
			
			retVal.getActiveParticipant().add(this.createHumanRequestor());
			retVal.getActiveParticipant().add(this.createSourceParticipant(String.format("%s|%s", terser.get("/MSH-3"), terser.get("/MSH-4"))));
			retVal.getActiveParticipant().add(AuditMessages.createActiveParticipant(String.format("%s|%s", terser.get("/MSH-5"), terser.get("/MSH-6")), null, null, false, remoteHost, "1", null, AuditMessages.RoleIDCode.Destination));
			
			if(patient != null)
				retVal.getParticipantObjectIdentification().add(
					AuditMessages.createParticipantObjectIdentification(
							String.format("%s^^^&%s&ISO", patient.getId(), this.m_configuration.getPatientRoot()), 
							ParticipantObjectIDTypeCode.PatientNumber, 
							null, 
							null, 
							"1", 
							"1", 
							null, 
							null, 
							null	
					)
				);
					
			
			return retVal;
		}
		catch(Exception e)
		{
			log.error("Error creating audit", e);
			throw new HealthInformationExchangeException("Error creating audit", e);
		}
	}

}
