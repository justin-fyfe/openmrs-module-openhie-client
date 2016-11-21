package org.openmrs.module.openhie.client.cda.section.impl;

import java.util.ArrayList;

import org.marc.everest.datatypes.BL;
import org.marc.everest.datatypes.II;
import org.marc.everest.datatypes.generic.CE;
import org.marc.everest.datatypes.generic.LIST;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Entry;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Section;
import org.marc.everest.rmim.uv.cdar2.vocabulary.x_ActRelationshipEntry;
import org.openmrs.activelist.Problem;
import org.openmrs.module.openhie.client.cda.entry.impl.ProblemConcernEntryBuilder;
import org.openmrs.module.shr.cdahandler.CdaHandlerConstants;
import org.openmrs.module.shr.cdahandler.processor.entry.impl.ihe.pcc.ProblemConcernEntryProcessor;
import org.openmrs.web.dwr.ProblemListItem;

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
		retVal.setTemplateId(LIST.createLIST(new II(CdaHandlerConstants.SCT_TEMPLATE_ACTIVE_PROBLEMS), new II(CdaHandlerConstants.SCT_TEMPLATE_CCD_PROBLEM)));
		retVal.setTitle("Active Problems");
		retVal.setCode(new CE<String>("11450-4", CdaHandlerConstants.CODE_SYSTEM_LOINC, "LOINC", null, "PROBLEM LIST", null));
		return retVal;
	}

	/**
	 * Generate the section with the specified problem list
	 * @param problem
	 * @return
	 */
	public Section generate(Problem... problem)
	{
		ArrayList<Entry> entries = new ArrayList<Entry>();
		
		ProblemConcernEntryBuilder builder = new ProblemConcernEntryBuilder();
		for(Problem prob : problem)
		{
			Entry ent = new Entry(x_ActRelationshipEntry.HasComponent, BL.TRUE, builder.generate(prob));
			entries.add(ent);
		}
		
		return this.generate(entries.toArray(new Entry[]{}));
	}
	
}
