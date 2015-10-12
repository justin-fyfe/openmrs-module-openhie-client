package org.openmrs.module.openhie.client.cda.document.impl;

import org.marc.everest.datatypes.II;
import org.marc.everest.datatypes.generic.CE;
import org.marc.everest.datatypes.generic.LIST;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.ClinicalDocument;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Section;

/**
 * History and physical document builder
 * @author JustinFyfe
 *
 */
public class HistoryPhysicalDocumentBuilder extends DocumentBuilderImpl {

	/**
	 * Generate the document
	 */
	@Override
	public ClinicalDocument generate(Section... sections) {
		ClinicalDocument retVal = super.generate(sections);
		retVal.setTemplateId(LIST.createLIST(new II("1.3.6.1.4.1.19376.1.5.3.1.1.16.1.4")));
		retVal.setCode(new CE<String>("34117-2", "2.16.840.1.113883.6.1", "LOINC", null, "HISTORY AND PHYSICAL", null));
		retVal.setTitle("History and Physical");
		return retVal;
	}

	
}
