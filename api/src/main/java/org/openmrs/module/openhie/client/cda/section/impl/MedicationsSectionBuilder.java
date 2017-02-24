package org.openmrs.module.openhie.client.cda.section.impl;

import java.util.ArrayList;
import java.util.List;

import org.marc.everest.datatypes.BL;
import org.marc.everest.datatypes.II;
import org.marc.everest.datatypes.generic.CE;
import org.marc.everest.datatypes.generic.LIST;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Entry;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Section;
import org.marc.everest.rmim.uv.cdar2.vocabulary.x_ActRelationshipEntry;
import org.openmrs.Obs;
import org.openmrs.module.openhie.client.CdaHandlerConstants;
import org.openmrs.module.openhie.client.cda.entry.impl.MedicationEntryBuilder;

/**
 * Section builder for medications
 * @author JustinFyfe
 *
 */
public class MedicationsSectionBuilder extends SectionBuilderImpl {

	/**
	 * Generate the medications section
	 */
	@Override
	public Section generate(Entry... entries) {
		
		// TODO: Verify entries
		
		Section retVal = super.generate(entries);
		retVal.setTemplateId(LIST.createLIST(new II(CdaHandlerConstants.SCT_TEMPLATE_CCD_MEDICATIONS), new II(CdaHandlerConstants.SCT_TEMPLATE_MEDICATIONS)));
		retVal.setTitle("History of Medication Use");
		retVal.setCode(new CE<String>("10160-0", CdaHandlerConstants.CODE_SYSTEM_LOINC, CdaHandlerConstants.CODE_SYSTEM_NAME_LOINC, null, "HISTORY OF MEDICATION USE", null));
		return retVal;
	}
	
	/**
	 * Generate the section data from a series of medication history observations
	 * @param medicationHistoryObs
	 * @return
	 */
	public Section generate(Obs... medicationHistoryObs)
	{
		List<Entry> entries = new ArrayList<Entry>();
		MedicationEntryBuilder administrationBuilder = new MedicationEntryBuilder();
		if(medicationHistoryObs.length == 0)
			entries.add(new Entry(x_ActRelationshipEntry.HasComponent, BL.TRUE, administrationBuilder.generateUnknown()));
		else for(Obs med : medicationHistoryObs)
				entries.add(new Entry(x_ActRelationshipEntry.HasComponent, BL.TRUE, administrationBuilder.generate(med)));
		
		return this.generate(entries.toArray(new Entry[]{}));

	}

	
}
