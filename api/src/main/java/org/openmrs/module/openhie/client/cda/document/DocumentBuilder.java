package org.openmrs.module.openhie.client.cda.document;

import java.util.List;

import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.ClinicalDocument;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Section;
import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.Provider;
import org.openmrs.RelationshipType;

/**
 * Represents a document builder which can construct a clinical document of a particular type
 * @author JustinFyfe
 *
 */
public interface DocumentBuilder {

	/**
	 * Set the Encounter this document builder will  be representing
	 */
	public void setEncounterEvent(Encounter enc);
	
	/**
	 * Get the classcode
	 * @return
	 */
	public String getTypeCode();
	
	/**
	 * Get the format code
	 * @return
	 */
	public String getFormatCode();
	
	/**
	 * Get the encounter event
	 */
	public Encounter getEncounterEvent();
	
	/**
	 * Set the record target of this document
	 * @param recordTarget
	 */
	public void setRecordTarget(Patient recordTarget);
	/**
	 * Get the record target of this document
	 * @return
	 */
	public Patient getRecordTarget();
	
	/**
	 * Generate the document
	 * @return
	 */
	ClinicalDocument generate(Section... sections);
	
}
