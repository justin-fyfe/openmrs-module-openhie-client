package org.openmrs.module.openhie.client.cda.entry.impl;

import java.util.Arrays;

import org.marc.everest.datatypes.generic.CD;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.ClinicalStatement;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Observation;
import org.openmrs.BaseOpenmrsData;
import org.openmrs.Obs;
import org.openmrs.module.openhie.client.CdaHandlerConstants;

/**
 * Simple observation entry builder
 * @author JustinFyfe
 *
 */
public class SimpleObservationEntryBuilder extends EntryBuilderImpl {

	/**
	 * Create the simple observation
	 */
	public Observation generate(CD<String> code, Obs obs)
	{
		return this.createObservation(Arrays.asList(CdaHandlerConstants.ENT_TEMPLATE_SIMPLE_OBSERVATION), code, obs);
	}
	
	/**
	 * Generate the simple observation
	 */
	public ClinicalStatement generate(BaseOpenmrsData data) {
		if(data instanceof Obs)
		{
			Obs obs = (Obs)data;
			CD<String> code = this.m_cdaMetadataUtil.getStandardizedCode(obs.getConcept(), CdaHandlerConstants.CODE_SYSTEM_LOINC, CD.class);
			if(code == null) // Get SNOMED as an alternate
				code = this.m_cdaMetadataUtil.getStandardizedCode(obs.getConcept(), CdaHandlerConstants.CODE_SYSTEM_SNOMED, CD.class);
			
			return this.generate(code, obs);
		}
		throw new IllegalArgumentException("data must be Obs");
	}

}
