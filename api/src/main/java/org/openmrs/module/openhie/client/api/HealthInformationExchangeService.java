package org.openmrs.module.openhie.client.api;

import java.util.Date;
import java.util.List;

import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.api.OpenmrsService;
import org.openmrs.module.openhie.client.exception.HealthInformationExchangeException;
import org.openmrs.module.openhie.client.hie.model.DocumentInfo;

/**
 * Implementation of the HealthInformationExchangeService
 * @author Justin
 */
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
	 * Import the specified patient data from the PDQ supplier
	 * @param identifier
	 * @param asigningAuthority
	 * @return
	 */
	public Patient importPatient(Patient patient);
	
	/**
	 * Export patient demographic record to the CR
	 * @param patient
	 */
	public void exportPatient(Patient patient) throws HealthInformationExchangeException;
	
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
	public DocumentInfo exportDocument(List<Encounter> encounters) throws HealthInformationExchangeException;

	/**
	 * Query for documents with the matching criteria
	 */
	public List<DocumentInfo> queryDocuments(Patient patientInfo, boolean oddOnly, Date sinceDate,
			String formatCode, String formatCodingScheme);
	
}
