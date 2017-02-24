package org.openmrs.module.openhie.client.api.impl;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.transaction.NotSupportedException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dcm4che3.audit.AuditMessage;
import org.dcm4chee.xds2.infoset.ihe.ProvideAndRegisterDocumentSetRequestType;
import org.dcm4chee.xds2.infoset.ihe.RetrieveDocumentSetRequestType;
import org.dcm4chee.xds2.infoset.ihe.RetrieveDocumentSetRequestType.DocumentRequest;
import org.dcm4chee.xds2.infoset.ihe.RetrieveDocumentSetResponseType;
import org.dcm4chee.xds2.infoset.rim.AdhocQueryResponse;
import org.dcm4chee.xds2.infoset.rim.RegistryResponseType;
import org.marc.everest.datatypes.II;
import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PersonAddress;
import org.openmrs.PersonName;
import org.openmrs.api.DuplicateIdentifierException;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.openhie.client.api.HealthInformationExchangeService;
import org.openmrs.module.openhie.client.configuration.CdaHandlerConfiguration;
import org.openmrs.module.openhie.client.configuration.HealthInformationExchangeConfiguration;
import org.openmrs.module.openhie.client.dao.HealthInformationExchangeDao;
import org.openmrs.module.openhie.client.exception.HealthInformationExchangeException;
import org.openmrs.module.openhie.client.hie.model.DocumentInfo;
import org.openmrs.module.openhie.client.util.AuditUtil;
import org.openmrs.module.openhie.client.util.MessageUtil;
import org.openmrs.module.shr.atna.api.AtnaAuditService;

import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v25.message.QBP_Q21;
import ca.uhn.hl7v2.util.Terser;

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
	private CdaHandlerConfiguration m_cdaConfiguration = CdaHandlerConfiguration.getInstance();

	// DAO
	private HealthInformationExchangeDao dao;
	
	/**
	 * @param dao the dao to set
	 */
	public void setDao(HealthInformationExchangeDao dao) {
		this.dao = dao;
	}

	/**
	 * Update patient ECID 
	 * @throws HealthInformationExchangeException 
	 */
	public void updatePatientEcid(Patient patient) throws HealthInformationExchangeException
	{
		// Resolve patient identifier
		PatientIdentifier pid = this.resolvePatientIdentifier(patient, this.m_cdaConfiguration.getEcidRoot());
		if(pid != null)
		{
			PatientIdentifier existingPid = patient.getPatientIdentifier(pid.getIdentifierType());
			if(existingPid != null && !existingPid.getIdentifier().equals(pid.getIdentifier()))
			{
					existingPid.setIdentifier(pid.getIdentifier());
					Context.getPatientService().savePatientIdentifier(existingPid);	
			}
			else if(existingPid == null)
			{
				pid.setPatient(patient);
				Context.getPatientService().savePatientIdentifier(pid);
			}
			else
				return;
		}
		else
			throw new HealthInformationExchangeException("Patient has been removed from the HIE");
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
				queryParams.put("@PID.7", new SimpleDateFormat("yyyy").format(dateOfBirth));
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
			
		AtnaAuditService auditSvc= Context.getService(AtnaAuditService.class);
		AuditMessage auditMessage = null;
		Message pdqRequest = null;
		
		// Send the message and construct the result set
		try
		{
			pdqRequest = this.m_messageUtil.createPdqMessage(queryParams);
			Message	response = this.m_messageUtil.sendMessage(pdqRequest, this.m_configuration.getPdqEndpoint(), this.m_configuration.getPdqPort());
			
			Terser terser = new Terser(response);
			if(!terser.get("/MSA-1").endsWith("A"))
				throw new HealthInformationExchangeException("Error querying data");
			
			
			List<Patient> retVal = this.m_messageUtil.interpretPIDSegments(response);
			auditMessage = AuditUtil.getInstance().createPatientSearch(retVal, this.m_configuration.getPdqEndpoint(), (QBP_Q21)pdqRequest);
			return retVal;
		}
		catch(Exception e)
		{
			log.error("Error in PDQ Search", e);
			if(pdqRequest != null)
				auditMessage = AuditUtil.getInstance().createPatientSearch(null, this.m_configuration.getPdqEndpoint(), (QBP_Q21)pdqRequest);

			throw new HealthInformationExchangeException(e);
		}
		finally
		{
			if(auditMessage != null)
				try {
					auditSvc.getLogger().write(Calendar.getInstance(), auditMessage);
				} catch (Exception e) {
					log.error(e);
				}
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

		// Auditing stuff
		AtnaAuditService auditSvc= Context.getService(AtnaAuditService.class);
		AuditMessage auditMessage = null;
		Message request = null;
		
		try
		{
			request = this.m_messageUtil.createPdqMessage(queryParameters);
			Message response = this.m_messageUtil.sendMessage(request, this.m_configuration.getPdqEndpoint(), this.m_configuration.getPdqPort());
			
			List<Patient> pats = this.m_messageUtil.interpretPIDSegments(response);
			auditMessage = AuditUtil.getInstance().createPatientSearch(pats, this.m_configuration.getPdqEndpoint(), (QBP_Q21)request);

			if(pats.size() > 1)
				throw new DuplicateIdentifierException("More than one patient exists");
			else if(pats.size() == 0)
				return null;
			else
				return pats.get(0);
		}
		catch(Exception e)
		{
			log.error("Error in PDQ Search", e);

			if(request != null)
				auditMessage = AuditUtil.getInstance().createPatientSearch(null, this.m_configuration.getPdqEndpoint(), (QBP_Q21)request);

			throw new HealthInformationExchangeException(e);
		}
		finally
		{
			if(auditMessage != null)
				try {
					auditSvc.getLogger().write(Calendar.getInstance(), auditMessage);
				} catch (Exception e) {
					log.error(e);
				}
		}
	}

	/**
	 * Import the patient from the PDQ supplier
	 * @throws HealthInformationExchangeException 
	 */
	public Patient importPatient(Patient patient) throws HealthInformationExchangeException 
	{
		Patient existingPatientRecord = this.matchWithExistingPatient(patient);
		
		// Existing? Then update this from that
		if(existingPatientRecord != null)
		{
			
			// Add new identifiers
			for(PatientIdentifier id : patient.getIdentifiers())
			{
				boolean hasId = false;
				for(PatientIdentifier eid : existingPatientRecord.getIdentifiers())
					hasId |= eid.getIdentifier().equals(id.getIdentifier()) && eid.getIdentifierType().getId().equals(id.getIdentifierType().getId());
				if(!hasId)
					existingPatientRecord.getIdentifiers().add(id);
			}
			
			// update names
			existingPatientRecord.getNames().clear();
			for(PersonName name : patient.getNames())
				existingPatientRecord.addName(name);
			// update addr
			existingPatientRecord.getAddresses().clear();
			for(PersonAddress addr : patient.getAddresses())
				existingPatientRecord.addAddress(addr);
			
			// Update deceased
			existingPatientRecord.setDead(patient.getDead());
			existingPatientRecord.setDeathDate(patient.getDeathDate());
			existingPatientRecord.setBirthdate(patient.getBirthdate());
			existingPatientRecord.setBirthdateEstimated(patient.getBirthdateEstimated());
			existingPatientRecord.setGender(patient.getGender());
			
			patient = existingPatientRecord;
		}
		else
		{
			boolean isPreferred = false;
			for(PatientIdentifier id : patient.getIdentifiers())
				if(id.getIdentifierType().getName().equals(this.m_cdaConfiguration.getEcidRoot()) ||
						id.getIdentifierType().getUuid().equals(this.m_cdaConfiguration.getEcidRoot()))
				{
					id.setPreferred(true);
					isPreferred = true;
				}
			
			if(!isPreferred)
				patient.getIdentifiers().iterator().next().setPreferred(true);

		}
		
		Patient importedPatient = Context.getPatientService().savePatient(patient);
		// Now notify
		this.exportPatient(patient);
		return importedPatient;
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
	 * @throws HealthInformationExchangeException 
	 */
	public byte[] fetchDocument(DocumentInfo document) throws HealthInformationExchangeException {
		
		AtnaAuditService auditSvc = Context.getService(AtnaAuditService.class);
		AuditMessage auditMessage = null;
		try
		{
			RetrieveDocumentSetRequestType request = new RetrieveDocumentSetRequestType();
			DocumentRequest di = new DocumentRequest();
			di.setDocumentUniqueId(document.getUniqueId());
			di.setRepositoryUniqueId(document.getRepositoryId());
			request.getDocumentRequest().add(di);
			
			RetrieveDocumentSetResponseType response = this.m_messageUtil.sendRetrieve(request);
			if(response.getDocumentResponse().size() == 0)
				throw new HealthInformationExchangeException("No results returned");
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			response.getDocumentResponse().get(0).getDocument().writeTo(bos);

			auditMessage = AuditUtil.getInstance().createFetchDocument(response, this.m_configuration.getXdsRepositoryEndpoint());
			
			return bos.toByteArray();
		}
		catch(Exception e)
		{
			log.error("Error in XDS Fetch", e);
			auditMessage = AuditUtil.getInstance().createFetchDocument(null, this.m_configuration.getXdsRepositoryEndpoint());
			throw new HealthInformationExchangeException(e);
		}
		finally
		{
			if(auditMessage != null)
				try
				{
					auditSvc.getLogger().write(Calendar.getInstance(), auditMessage);
				}
				catch(Exception e)
				{
					log.error(e);
				}
		}
	}

	/**
	 * Import a document into the OpenMRS datastore
	 * @throws HealthInformationExchangeException 
	 */
	public Encounter importDocument(DocumentInfo document) throws HealthInformationExchangeException {
		
		throw new HealthInformationExchangeException(new NotSupportedException("Document import not supported on OpenMRS 1.8.2"));
	}

	/**
	 * Export encounters as a document
	 * @throws HealthInformationExchangeException 
	 */
	public DocumentInfo exportDocument(byte[] documentContent, DocumentInfo info) throws HealthInformationExchangeException {
		
		AtnaAuditService auditSvc = Context.getService(AtnaAuditService.class);
		AuditMessage auditMessage = null;
		ProvideAndRegisterDocumentSetRequestType request = null;
		try
		{
			this.updatePatientEcid(info.getPatient());
			request = this.m_messageUtil.createProvdeAndRegisterDocument(documentContent, info);
			RegistryResponseType response = this.m_messageUtil.sendProvideAndRegister(request);
			if(!response.getStatus().contains("Success"))
				throw new Exception("Could not execute provide and register");
			
			auditMessage = AuditUtil.getInstance().createExportDocument(request, response, this.m_configuration.getXdsRepositoryEndpoint());
			return info;
		}
		catch(Exception e)
		{
			log.error("Error in XDS Export", e);
			if(request != null)
				auditMessage = AuditUtil.getInstance().createExportDocument(request, null, this.m_configuration.getXdsRepositoryEndpoint());
			throw new HealthInformationExchangeException(e);
		}
		finally
		{
			if(auditMessage != null)
				try
				{
					auditSvc.getLogger().write(Calendar.getInstance(), auditMessage);
				}
				catch(Exception e)
				{
					log.error(e);
				}
		}
	}

	/**
	 * Export a patient to the HIE
	 * @throws HealthInformationExchangeException 
	 */
	public void exportPatient(Patient patient) throws HealthInformationExchangeException {
		// TODO Auto-generated method stub
		
		Message admitMessage = null;
		AtnaAuditService auditSvc = Context.getService(AtnaAuditService.class);
		AuditMessage auditMessage = null;

		try
		{
			admitMessage = this.m_messageUtil.createAdmit(patient);
			Message response = this.m_messageUtil.sendMessage(admitMessage, this.m_configuration.getPixEndpoint(), this.m_configuration.getPixPort());
			
			Terser terser = new Terser(response);
			if(!terser.get("/MSA-1").endsWith("A"))
				throw new HealthInformationExchangeException("Error querying data");
			auditMessage = AuditUtil.getInstance().createPatientAdmit(patient, this.m_configuration.getPixEndpoint(), admitMessage, true);

		}
		catch(Exception e)
		{
			log.error(e);
			if(auditMessage != null)
				auditMessage = AuditUtil.getInstance().createPatientAdmit(patient, this.m_configuration.getPixEndpoint(), admitMessage, false);

			throw new HealthInformationExchangeException(e);
		}
		finally
		{
			if(auditMessage != null)
				try
				{
					auditSvc.getLogger().write(Calendar.getInstance(), auditMessage);
				}
				catch(Exception e)
				{
					log.error(e);
				}
		}	

	}

	/**
	 * Resolve patient identifier of the patient
	 * @throws HealthInformationExchangeException 
	 */
	public PatientIdentifier resolvePatientIdentifier(Patient patient,
			String toAssigningAuthority) throws HealthInformationExchangeException {
		
		AtnaAuditService auditSvc = Context.getService(AtnaAuditService.class);
		AuditMessage auditMessage = null;

		Message request = null;
		try
		{
			request = this.m_messageUtil.createPixMessage(patient, toAssigningAuthority);
			Message response = this.m_messageUtil.sendMessage(request, this.m_configuration.getPixEndpoint(), this.m_configuration.getPixPort());
			
			// Interpret the result
			List<Patient> candidate = this.m_messageUtil.interpretPIDSegments(response);
			auditMessage = AuditUtil.getInstance().createPatientResolve(candidate, this.m_configuration.getPixEndpoint(), request);
			if(candidate.size() == 0)
				return null;
			else
				return candidate.get(0).getIdentifiers().iterator().next();
		}
		catch(Exception e)
		{
			log.error(e);
			if(request != null)
				auditMessage = AuditUtil.getInstance().createPatientResolve(null, this.m_configuration.getPixEndpoint(), request);

			throw new HealthInformationExchangeException(e);
		}
		finally
		{
			if(auditMessage != null)
				try
				{
					auditSvc.getLogger().write(Calendar.getInstance(), auditMessage);
				}
				catch(Exception e)
				{
					log.error(e);
				}
		}
	}

	/**
	 * Query for documents matching the specified criteria
	 * @throws HealthInformationExchangeException 
	 */
	public List<DocumentInfo> queryDocuments(Patient patientInfo,
			boolean oddOnly, Date sinceDate, String formatCode,
			String formatCodingScheme) throws HealthInformationExchangeException {

		// Get documents
		try
		{
			// Format code
			String fmtCode = null;
			if(formatCode != null && formatCodingScheme != null)
				fmtCode = String.format("%s^^%s", formatCode, formatCodingScheme);
			this.updatePatientEcid(patientInfo);

			AdhocQueryResponse queryResponse = this.m_messageUtil.sendXdsQuery(this.m_messageUtil.createXdsQuery(patientInfo, fmtCode, sinceDate));
			return this.m_messageUtil.interpretAdHocQueryResponse(queryResponse, patientInfo, oddOnly);
		}
		catch(Exception e)
		{
			throw new HealthInformationExchangeException(e);
		}
		
	}

	/**
	 * Match an external patient with internal patient
	 * @see org.openmrs.module.openhie.client.api.HealthInformationExchangeService#matchWithExistingPatient(org.openmrs.Patient)
	 */
	public Patient matchWithExistingPatient(Patient remotePatient) {
		Patient candidate = null;
		// Does this patient have an identifier from our assigning authority?
		for(PatientIdentifier pid : remotePatient.getIdentifiers())
			if(pid.getIdentifierType().getName().equals(this.m_cdaConfiguration.getPatientRoot()))
				try
				{
					candidate = Context.getPatientService().getPatient(Integer.parseInt(pid.getIdentifier()));
				}
				catch(Exception e)
				{
					
				}
		
		// This patient may be an existing patient, so we just don't want to add it!
		if(candidate == null)
			for(PatientIdentifier pid : remotePatient.getIdentifiers())
			{
				candidate = this.dao.getPatientByIdentifier(pid.getIdentifier(), pid.getIdentifierType());
				if(candidate != null)
					break;
			}
		
		return candidate;
    }

	/**
	 * Update the patient record
	 * @see org.openmrs.module.openhie.client.api.HealthInformationExchangeService#updatePatient(org.openmrs.Patient)
	 */
	public void updatePatient(Patient patient) throws HealthInformationExchangeException {
		
		// TODO Auto-generated method stub
		AtnaAuditService auditSvc = Context.getService(AtnaAuditService.class);
		AuditMessage auditMessage = null;

		Message admitMessage = null;
		try
		{
			admitMessage = this.m_messageUtil.createUpdate(patient);
			Message	response = this.m_messageUtil.sendMessage(admitMessage, this.m_configuration.getPixEndpoint(), this.m_configuration.getPixPort());
			
			Terser terser = new Terser(response);
			if(!terser.get("/MSA-1").endsWith("A"))
				throw new HealthInformationExchangeException("Error querying data");
			auditMessage = AuditUtil.getInstance().createPatientAdmit(patient, this.m_configuration.getPixEndpoint(), admitMessage, true);


		}
		catch(Exception e)
		{
			log.error(e);
			if(auditMessage != null)
				auditMessage = AuditUtil.getInstance().createPatientAdmit(patient, this.m_configuration.getPixEndpoint(), admitMessage, false);

			throw new HealthInformationExchangeException(e);
		}
		finally
		{
			if(auditMessage != null)
				try
				{
					auditSvc.getLogger().write(Calendar.getInstance(), auditMessage);
				}
				catch(Exception e)
				{
					log.error(e);
				}
		}	
		
    }

}
