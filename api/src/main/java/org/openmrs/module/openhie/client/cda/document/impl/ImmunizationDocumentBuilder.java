package org.openmrs.module.openhie.client.cda.document.impl;

import org.marc.everest.datatypes.II;
import org.marc.everest.datatypes.generic.CE;
import org.marc.everest.datatypes.generic.LIST;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.ClinicalDocument;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Section;

/**
 * Builds an immunization content (IC) document
 * @author JustinFyfe
 *
 */
public class ImmunizationDocumentBuilder extends DocumentBuilderImpl {

	/**
	 * Generate the document
	 */
	@Override
	public ClinicalDocument generate(Section... sections) {
		ClinicalDocument retVal = super.generate(sections);
		retVal.setTemplateId(LIST.createLIST(new II("1.3.6.1.4.1.19376.1.5.3.1.1.18.1.2")));
		retVal.setCode(new CE<String>("11369-6", "2.16.840.1.113883.6.1", "LOINC", null, "HISTORY OF IMMUNIZATIONS", null));
		retVal.setTitle("History of Immunizations");
		return retVal;
	}
}
