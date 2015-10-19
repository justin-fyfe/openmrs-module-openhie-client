package org.openmrs.module.openhie.client.api;

import java.util.Date;
import java.util.List;

import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.api.OpenmrsService;
import org.openmrs.module.openhie.client.exception.HealthInformationExchangeException;
import org.openmrs.module.openhie.client.hie.model.DocumentInfo;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of the HealthInformationExchangeService
 * @author Justin
 */
@Transactional(rollbackFor=HealthInformationExchangeException.class)
public interface HealthInformationExchangeService extends OpenmrsService {

	/**
	 * Searches the PDQ supplier for patients matching the specified search string and returns
	 * patients matching the supplied string 
	 * @param patientSearchString
	 * @return
	 */
	public List<Patient> searchPatient(String familyName, String givenName, Date dateOfBirth, boolean fuzzyDate, String gender, PatientIdentifier patientIdentifier, PatientIdentifier mothersIdentifier) throws HealthInformationExchangeException;
	
	/**
	 * Searches for patients with the specified patient identity string 
	 */
	public Patient getPatient(String identifier, String assigningAuthority) throws HealthInformationExchangeException;
	
	/**
	 * Resolve an HIE patient identifier 
	 * @throws HealthInformationExchangeException 
	 */
	public PatientIdentifier resolvePatientIdentifier(Patient patient, String toAssigningAuthority) throws HealthInformationExchangeException;
	
	/**
	 * Forces an update of the patient's ECID data
	 * @param patient
	 */
	public void updatePatientEcid(Patient patient) throws HealthInformationExchangeException;
	
	/**
	 * Import the specified patient data from the PDQ supplier
	 * @param identifier
	 * @param asigningAuthority
	 * @return
	 * @throws HealthInformationExchangeException 
	 */
	public Patient importPatient(Patient patient) throws HealthInformationExchangeException;
	
	/**
	 * Matches an external patient with an internal 
	 */
	public Patient matchWithExistingPatient(Patient remotePatient);
	
	/**
	 * Export patient demographic record to the CR
	 * @param patient
	 */
	public void exportPatient(Patient patient) throws HealthInformationExchangeException;

	/**
	 * Export patient demographic record to the CR
	 * @param patient
	 */
	public void updatePatient(Patient patient) throws HealthInformationExchangeException;

	
	/**
	 * Get all HIE documents for the specified patient
	 */
	public List<DocumentInfo> getDocuments(Patient patient) throws HealthInformationExchangeException;
	
	/**
	 * Get the document contents from the HIE
	 */
	public byte[] fetchDocument(DocumentInfo document) throws HealthInformationExchangeException;
	
	/**
	 * Perform a document import of the specified document information object
	 */
	public Encounter importDocument(DocumentInfo document) throws HealthInformationExchangeException;
	
	/**
	 * Export the specified encounters as a document to the HIE
	 * @param encounters
	 * @return
	 */
	public DocumentInfo exportDocument(byte[] documentContent, DocumentInfo info) throws HealthInformationExchangeException;

	/**
	 * Query for documents with the matching criteria
	 * @throws HealthInformationExchangeException 
	 */
	public List<DocumentInfo> queryDocuments(Patient patientInfo, boolean oddOnly, Date sinceDate,
			String formatCode, String formatCodingScheme) throws HealthInformationExchangeException;

	
}
