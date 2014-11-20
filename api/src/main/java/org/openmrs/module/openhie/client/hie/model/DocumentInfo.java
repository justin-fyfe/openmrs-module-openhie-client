package org.openmrs.module.openhie.client.hie.model;

import java.util.List;

import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.module.shr.contenthandler.api.Content;

/**
 * Represents basic information about a document related to a patient
 * @author Justin
 *
 */
public class DocumentInfo {
	
	// Patient of the document
	private Patient patient;
	// title of the document
	private String title;
	// mime type of the document
	private String mimeType;
	// hash of the document
	private byte[] hash;
	// related encounters
	private Encounter relatedEncounter;
	// authors
	private List<String> authorDisplayNames;
	
	/**
	 * @return the patient
	 */
	public Patient getPatient() {
		return patient;
	}
	/**
	 * @param patient the patient to set
	 */
	public void setPatient(Patient patient) {
		this.patient = patient;
	}
	/**
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}
	/**
	 * @param title the title to set
	 */
	public void setTitle(String title) {
		this.title = title;
	}
	/**
	 * @return the mimeType
	 */
	public String getMimeType() {
		return mimeType;
	}
	/**
	 * @param mimeType the mimeType to set
	 */
	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}
	/**
	 * @return the hash
	 */
	public byte[] getHash() {
		return hash;
	}
	/**
	 * @param hash the hash to set
	 */
	public void setHash(byte[] hash) {
		this.hash = hash;
	}
	/**
	 * @return the relatedEncounter
	 */
	public Encounter getRelatedEncounter() {
		return relatedEncounter;
	}
	/**
	 * @param relatedEncounter the relatedEncounter to set
	 */
	public void setRelatedEncounter(Encounter relatedEncounter) {
		this.relatedEncounter = relatedEncounter;
	}
	/**
	 * @return the authorDisplayNames
	 */
	public List<String> getAuthorDisplayNames() {
		return authorDisplayNames;
	}
	/**
	 * @param authorDisplayNames the authorDisplayNames to set
	 */
	public void setAuthorDisplayNames(List<String> authorDisplayNames) {
		this.authorDisplayNames = authorDisplayNames;
	}
}
