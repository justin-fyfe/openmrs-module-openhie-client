package org.openmrs.module.openhie.client.cda.section.impl;

import org.marc.everest.datatypes.II;
import org.marc.everest.datatypes.generic.CE;
import org.marc.everest.datatypes.generic.LIST;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Entry;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Section;

/**
 * Active problems section bulder
 * @author JustinFyfe
 *
 */
public class ActiveProblemsSectionBuilder extends SectionBuilderImpl {

	/**
	 * Generate the active problems section
	 */
	@Override
	public Section generate(Entry... entries) {
		
		// TODO: Verify entries
		
		Section retVal = super.generate(entries);
		retVal.setTemplateId(LIST.createLIST(new II("2.16.840.1.113883.10.20.1.11"), new II("1.3.6.1.4.1.19376.1.5.3.1.3.6")));
		retVal.setTitle("Active Problems");
		retVal.setCode(new CE<String>("11450-4", "2.16.840.1.113883.6.1", "LOINC", null, "PROBLEM LIST", null));
		return retVal;
	}

	
}
