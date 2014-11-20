package org.openmrs.module.openhie.client.api.impl;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.marc.everest.datatypes.II;
import org.openmrs.Patient;
import org.openmrs.Encounter;
import org.openmrs.PatientIdentifier;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.openhie.client.Exception.HealthInformationExchangeException;
import org.openmrs.module.openhie.client.api.HealthInformationExchangeService;
import org.openmrs.module.openhie.client.configuration.HealthInformationExchangeConfiguration;
import org.openmrs.module.openhie.client.hie.model.DocumentInfo;

import ca.uhn.hl7v2.model.Message;

/**
 * Implementation of the health information exchange service
 * @author Justin
 *
 */
public class HealthInformationExchangeServiceImpl extends BaseOpenmrsService
		implements HealthInformationExchangeService {

	// Log
	private static Log log = LogFactory.getLog(HealthInformationExchangeServiceImpl.class);
	// Message utility
	private MessageUtil m_messageUtil = MessageUtil.getInstance();
	// Get health information exchange information
	private HealthInformationExchangeConfiguration m_configuration = HealthInformationExchangeConfiguration.getInstance();
	
	/**
	 * Search the PDQ supplier for the specified patient data
	 * @throws HealthInformationExchangeException 
	 */
	public List<Patient> searchPatient(String familyName, String givenName,
			Date dateOfBirth, boolean fuzzyDate, PatientIdentifier identifier,
			PatientIdentifier mothersIdentifier) throws HealthInformationExchangeException {

		Map<String, String> queryParams = new HashMap<String, String>();
		if(familyName != null)
			queryParams.put("@PID.5.1", familyName);
		if(givenName != null)
			queryParams.put("@PID.5.2", givenName);
		if(dateOfBirth != null)
		{
			if(fuzzyDate)
				queryParams.put("@PID.8", String.format("%s", dateOfBirth.getYear()));
			else
				queryParams.put("@PID.8", new SimpleDateFormat("yyyyMMdd").format(dateOfBirth));
		}
		if(identifier != null)
		{
			queryParams.put("@PID.3.1", identifier.getIdentifier());
			
			if(II.isRootOid(new II(identifier.getIdentifierType().getName())))
			{
				queryParams.put("@PID.3.4.2", identifier.getIdentifierType().getName());
				queryParams.put("@PID.3.4.3", "ISO");
			}
			else
				queryParams.put("@PID.3.4", identifier.getIdentifierType().getName());
		}
		if(mothersIdentifier != null)
		{
			queryParams.put("@PID.21.1", mothersIdentifier.getIdentifier());
			
			if(II.isRootOid(new II(mothersIdentifier.getIdentifierType().getName())))
			{
				queryParams.put("@PID.21.4.2", mothersIdentifier.getIdentifierType().getName());
				queryParams.put("@PID.21.4.3", "ISO");
			}
			else
				queryParams.put("@PID.21.4", mothersIdentifier.getIdentifierType().getName());
		}
			
		// Send the message and construct the result set
		try
		{
			Message pdqRequest = this.m_messageUtil.createPdqMessage(queryParams),
					response = this.m_messageUtil.sendMessage(pdqRequest, this.m_configuration.getPdqEndpoint(), this.m_configuration.getPdqPort());
			
			return this.m_messageUtil.interpretPIDSegments(response);
		}
		catch(Exception e)
		{
			log.error(e);
			throw new HealthInformationExchangeException(e);
		}
	}

	/**
	 * Search the PDQ supplier for the specified patient data with identifier
	 */
	public List<Patient> searchPatient(String identifier,
			String assigningAuthority) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Get documents for the specified patient
	 */
	public List<DocumentInfo> getDocuments(Patient patient) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Fetch a document from the XDS repository endpoint
	 */
	public byte[] fetchDocument(DocumentInfo document) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Import a document into the OpenMRS datastore
	 */
	public Encounter importDocument(DocumentInfo document) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Export encounters as a document
	 */
	public DocumentInfo exportDocument(List<Encounter> encounters) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Export a patient to the HIE
	 */
	public void exportPatient(Patient patient) {
		// TODO Auto-generated method stub
		
	}

}
