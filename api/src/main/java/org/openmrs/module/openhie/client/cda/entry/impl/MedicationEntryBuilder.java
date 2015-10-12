package org.openmrs.module.openhie.client.cda.entry.impl;

import java.util.Arrays;

import org.apache.commons.lang.NotImplementedException;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.ClinicalStatement;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.SubstanceAdministration;
import org.openmrs.BaseOpenmrsData;
import org.openmrs.Obs;
import org.openmrs.module.shr.cdahandler.CdaHandlerConstants;

/**
 * Builder for the medication entry
 * @author JustinFyfe
 *
 */
public class MedicationEntryBuilder extends EntryBuilderImpl {

	
	/**
	 * Generate an unknown indicator
	 * @return
	 */
	public SubstanceAdministration generateUnknown()
	{
		return this.createNoSubstanceAdministration(Arrays.asList(CdaHandlerConstants.ENT_TEMPLATE_MEDICATIONS, CdaHandlerConstants.ENT_TEMPLATE_CCD_MEDICATION_ACTIVITY));
	}
	
	/**
	 * Generate the medication sbadm
	 * @param medicationObs
	 * @return
	 */
	public SubstanceAdministration generate(Obs medicationObs)
	{
		return this.createSubstanceAdministration(Arrays.asList(CdaHandlerConstants.ENT_TEMPLATE_MEDICATIONS, CdaHandlerConstants.ENT_TEMPLATE_CCD_MEDICATION_ACTIVITY), medicationObs);
	}
	
	/**
	 * Generate the medication entry
	 */
	public ClinicalStatement generate(BaseOpenmrsData data) {

		if(data instanceof Obs)
			return this.generate((Obs)data);
		// TODO DrugOrder types
		throw new NotImplementedException();
	}

}
