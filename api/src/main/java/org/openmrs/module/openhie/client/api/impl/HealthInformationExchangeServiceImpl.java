package org.openmrs.module.openhie.client.api.impl;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.marc.everest.datatypes.II;
import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.api.DuplicateIdentifierException;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.openhie.client.api.HealthInformationExchangeService;
import org.openmrs.module.openhie.client.configuration.HealthInformationExchangeConfiguration;
import org.openmrs.module.openhie.client.dao.HealthInformationExchangeDao;
import org.openmrs.module.openhie.client.exception.HealthInformationExchangeException;
import org.openmrs.module.openhie.client.hie.model.DocumentInfo;
import org.openmrs.module.shr.cdahandler.configuration.CdaHandlerConfiguration;
import org.openmrs.module.shr.cdahandler.configuration.CdaHandlerConfigurationFactory;

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
	// Get CDA handler configruation
	private CdaHandlerConfiguration m_cdaConfiguration = CdaHandlerConfigurationFactory.getInstance();
	
	// DAO
	private HealthInformationExchangeDao dao;
	
	/**
	 * @param dao the dao to set
	 */
	public void setDao(HealthInformationExchangeDao dao) {
		this.dao = dao;
	}

	/**
	 * Search the PDQ supplier for the specified patient data
	 * @throws HealthInformationExchangeException 
	 */
	public List<Patient> searchPatient(String familyName, String givenName,
			Date dateOfBirth, boolean fuzzyDate, String gender,
			PatientIdentifier identifier,
			PatientIdentifier mothersIdentifier) throws HealthInformationExchangeException {

		Map<String, String> queryParams = new HashMap<String, String>();
		if(familyName != null && !familyName.isEmpty())
			queryParams.put("@PID.5.1", familyName);
		if(givenName != null && !givenName.isEmpty())
			queryParams.put("@PID.5.2", givenName);
		if(dateOfBirth != null)
		{
			if(fuzzyDate)
				queryParams.put("@PID.7", String.format("%s", dateOfBirth.getYear()));
			else
				queryParams.put("@PID.7", new SimpleDateFormat("yyyyMMdd").format(dateOfBirth));
		}
		if(gender != null && !gender.isEmpty())
			queryParams.put("@PID.8", gender);
		if(identifier != null)
		{
			queryParams.put("@PID.3.1", identifier.getIdentifier());
			
			if(identifier.getIdentifierType() != null)
			{
				if(II.isRootOid(new II(identifier.getIdentifierType().getName())))
				{
					queryParams.put("@PID.3.4.2", identifier.getIdentifierType().getName());
					queryParams.put("@PID.3.4.3", "ISO");
				}
				else
					queryParams.put("@PID.3.4", identifier.getIdentifierType().getName());
			}
		}
		if(mothersIdentifier != null)
		{
			
			queryParams.put("@PID.21.1", mothersIdentifier.getIdentifier());
			
			if(mothersIdentifier.getIdentifierType() != null)
			{
				if(II.isRootOid(new II(mothersIdentifier.getIdentifierType().getName())))
				{
					queryParams.put("@PID.21.4.2", mothersIdentifier.getIdentifierType().getName());
					queryParams.put("@PID.21.4.3", "ISO");
				}
				else
					queryParams.put("@PID.21.4", mothersIdentifier.getIdentifierType().getName());
			}
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
	 * @throws HealthInformationExchangeException 
	 */
	public Patient getPatient(String identifier,
			String assigningAuthority) throws HealthInformationExchangeException {

		Map<String, String> queryParameters = new HashMap<String, String>();
		queryParameters.put("@PID.3.1", identifier);
		queryParameters.put("@PID.3.4.2", assigningAuthority);
		queryParameters.put("@PID.3.4.3", "ISO");
		
		try
		{
			Message request = this.m_messageUtil.createPdqMessage(queryParameters),
					response = this.m_messageUtil.sendMessage(request, this.m_configuration.getPdqEndpoint(), this.m_configuration.getPdqPort());
			
			List<Patient> pats = this.m_messageUtil.interpretPIDSegments(response);
			if(pats.size() > 1)
				throw new DuplicateIdentifierException("More than one patient exists");
			else if(pats.size() == 0)
				return null;
			else
				return pats.get(0);
		}
		catch(Exception e)
		{
			log.error(e);
			throw new HealthInformationExchangeException(e);
		}
	}

	/**
	 * Import the patient from the PDQ supplier
	 * @throws HealthInformationExchangeException 
	 */
	public Patient importPatient(Patient patient) 
	{
		Patient existingPatientRecord = null;
		
		// Does this patient have an identifier from our assigning authority?
		for(PatientIdentifier pid : patient.getIdentifiers())
			if(pid.getIdentifierType().getName().equals(this.m_cdaConfiguration.getPatientRoot()))
				existingPatientRecord = Context.getPatientService().getPatient(Integer.parseInt(pid.getIdentifier()));
		
		// This patient may be an existing patient, so we just don't want to add it!
		if(existingPatientRecord != null)
			for(PatientIdentifier pid : patient.getIdentifiers())
			{
				existingPatientRecord = this.dao.getPatientByIdentifier(pid.getIdentifier(), pid.getIdentifierType());
				if(patient != null)
					break;
			}
		
		// Existing? Then update this from that
		if(existingPatientRecord != null)
			patient.setId(existingPatientRecord.getId());
		
		return Context.getPatientService().savePatient(patient);
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

	/**
	 * Resolve patient identifier of the patient
	 * @throws HealthInformationExchangeException 
	 */
	public PatientIdentifier resolvePatientIdentifier(Patient patient,
			String toAssigningAuthority) throws HealthInformationExchangeException {
		try
		{
			Message request = this.m_messageUtil.createPixMessage(patient, toAssigningAuthority),
					response = this.m_messageUtil.sendMessage(request, this.m_configuration.getPixEndpoint(), this.m_configuration.getPixPort());
			
			// Interpret the result
			List<Patient> candidate = this.m_messageUtil.interpretPIDSegments(response);
			if(candidate.size() == 0)
				return null;
			else
				return candidate.get(0).getIdentifiers().iterator().next();
		}
		catch(Exception e)
		{
			log.error(e);
			throw new HealthInformationExchangeException(e);
		}
	}

	/**
	 * Query for documents matching the specified criteria
	 */
	public List<DocumentInfo> queryDocuments(Patient patientInfo,
			boolean oddOnly, Date sinceDate, String formatCode,
			String formatCodingScheme) {
		// TODO Auto-generated method stub
		return null;
	}

}
