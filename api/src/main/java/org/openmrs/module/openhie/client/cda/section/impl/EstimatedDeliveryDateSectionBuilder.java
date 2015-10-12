package org.openmrs.module.openhie.client.cda.section.impl;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Set;

import org.marc.everest.datatypes.BL;
import org.marc.everest.datatypes.II;
import org.marc.everest.datatypes.TS;
import org.marc.everest.datatypes.generic.CE;
import org.marc.everest.datatypes.generic.LIST;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Entry;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.EntryRelationship;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Observation;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Section;
import org.marc.everest.rmim.uv.cdar2.vocabulary.x_ActRelationshipEntry;
import org.marc.everest.rmim.uv.cdar2.vocabulary.x_ActRelationshipEntryRelationship;
import org.openmrs.Obs;
import org.openmrs.module.openhie.client.cda.entry.impl.EntryBuilderImpl;
import org.openmrs.module.openhie.client.cda.entry.impl.EstimatedDeliveryDateObservationBuilder;
import org.openmrs.module.shr.cdahandler.CdaHandlerConstants;

/**
 * EDD section builder
 * @author JustinFyfe
 *
 */
public class EstimatedDeliveryDateSectionBuilder extends SectionBuilderImpl {

	/**
	 * Generate the section
	 */
	@Override
	public Section generate(Entry... entries) {
		// TODO: Verify entries
		
		Section retVal = super.generate(entries);
		retVal.setTemplateId(LIST.createLIST(new II(CdaHandlerConstants.SCT_TEMPLATE_ESTIMATED_DELIVERY_DATES)));
		retVal.setTitle("Estimated Date of Delivery");
		retVal.setCode(new CE<String>("57060-6", CdaHandlerConstants.CODE_SYSTEM_LOINC, CdaHandlerConstants.CODE_SYSTEM_NAME_LOINC, null, "Estimated Date of Delivery", null));
		return retVal;
	}

	
	/**
	 * Generate from an estimated delivery date obs
	 * @param estimatedDeliveryDateObs
	 * @return
	 */
	public Section generate(Obs estimatedDeliveryDateObs, Obs lastMenstrualPeriodObs)
	{
		
		if(estimatedDeliveryDateObs.getValueDate() == null)
			throw new IllegalArgumentException("estimatedDeliveryDateObs must carry Date value");
		else if(lastMenstrualPeriodObs.getValueDate() == null)
			throw new IllegalArgumentException("lastMenstrualPeriodObs must carry Date value");
		
		EstimatedDeliveryDateObservationBuilder eddObsBuilder = new EstimatedDeliveryDateObservationBuilder();
		Observation eddObservation = eddObsBuilder.generate(estimatedDeliveryDateObs, lastMenstrualPeriodObs);
		
		Entry entry = new Entry(x_ActRelationshipEntry.HasComponent, BL.TRUE, eddObservation);
		return this.generate(entry);
		
	}
}