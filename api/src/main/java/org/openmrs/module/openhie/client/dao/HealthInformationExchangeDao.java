package org.openmrs.module.openhie.client.dao;

import org.openmrs.Patient;
import org.openmrs.PatientIdentifierType;

/**
 * A DAO that is used by the HIE to assist in the maintenance of OpenMRS
 * data store with an HIE
 */
public interface HealthInformationExchangeDao {

	/**
	 * Get a patient by identifier, throws a multiple exception when there are multiple / conflicting patients with the specified identifier
	 */
	public Patient getPatientByIdentifier(String idNumber, PatientIdentifierType idType);
	
}
