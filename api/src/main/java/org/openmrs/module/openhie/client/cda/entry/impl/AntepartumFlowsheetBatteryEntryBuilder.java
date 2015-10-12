package org.openmrs.module.openhie.client.cda.entry.impl;

import java.util.Arrays;

import org.apache.commons.lang.NotImplementedException;
import org.marc.everest.datatypes.BL;
import org.marc.everest.datatypes.II;
import org.marc.everest.datatypes.generic.CD;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.ClinicalStatement;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Component4;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Organizer;
import org.marc.everest.rmim.uv.cdar2.vocabulary.ActRelationshipHasComponent;
import org.marc.everest.rmim.uv.cdar2.vocabulary.ActStatus;
import org.marc.everest.rmim.uv.cdar2.vocabulary.x_ActClassDocumentEntryOrganizer;
import org.openmrs.BaseOpenmrsData;
import org.openmrs.ConceptNumeric;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.module.shr.cdahandler.CdaHandlerConstants;

/**
 * Builds the antepartum flowsheet battery entry
 * @author JustinFyfe
 *
 */
public class AntepartumFlowsheetBatteryEntryBuilder extends EntryBuilderImpl {

	/**
	 * Generate the flowsheet battery
	 */
	public Organizer generate(Obs gestgationalAgeObs, Obs fundalHeightObs, Obs presentationObs, Obs systolicBpObs, Obs diastolicBpObs, Obs weightObs)
	{
		Encounter batteryEnc = gestgationalAgeObs.getEncounter();
		
		// Validate parameters
		if(gestgationalAgeObs == null || !(gestgationalAgeObs.getConcept() instanceof ConceptNumeric))
			throw new IllegalArgumentException("gestgationalAgeObs must be a numeric concept");
		else if(fundalHeightObs != null && !(fundalHeightObs.getConcept() instanceof ConceptNumeric))
			throw new IllegalArgumentException("fundalHeightObs must be a numeric concept");
		else if(presentationObs != null && presentationObs.getValueCoded() != null)
			throw new IllegalArgumentException("presentationObs must be a coded concept");
		else if(systolicBpObs != null && !(systolicBpObs.getConcept() instanceof ConceptNumeric))
			throw new IllegalArgumentException("systolicBpObs must be a numeric concept");
		else if(diastolicBpObs != null && !(diastolicBpObs.getConcept() instanceof ConceptNumeric))
			throw new IllegalArgumentException("diastolicBpObs must be a numeric concept");
		else if(weightObs != null && !(weightObs.getConcept() instanceof ConceptNumeric))
			throw new IllegalArgumentException("weightObs must be a numeric concept");
		else if(fundalHeightObs != null && batteryEnc.getId() != fundalHeightObs.getEncounter().getId() ||
				presentationObs != null && batteryEnc.getId() != presentationObs.getEncounter().getId() ||
				systolicBpObs != null && batteryEnc.getId() != systolicBpObs.getEncounter().getId() ||
				diastolicBpObs != null && batteryEnc.getId() != diastolicBpObs.getEncounter().getId() ||
				weightObs != null && batteryEnc.getId() != weightObs.getEncounter().getId())
			throw new IllegalArgumentException("All arguments for the flowsheet panel must come from the same encounter");
		
		Organizer batteryOrganizer = super.createOrganizer(x_ActClassDocumentEntryOrganizer.BATTERY, 
				Arrays.asList(CdaHandlerConstants.ENT_TEMPLATE_ANTEPARTUM_FLOWSHEET_PANEL),
				new CD<String>("57061-4", CdaHandlerConstants.CODE_SYSTEM_LOINC, CdaHandlerConstants.CODE_SYSTEM_NAME_LOINC, null, "Antepartum Flowsheet Panel", null), 
				new II(this.m_cdaConfiguration.getEncounterRoot(), batteryEnc.getId().toString()), 
				ActStatus.Completed, 
				batteryEnc.getEncounterDatetime());
		
		SimpleObservationEntryBuilder obsBuilder = new SimpleObservationEntryBuilder();
		batteryOrganizer.getComponent().add(new Component4(ActRelationshipHasComponent.HasComponent, BL.TRUE, obsBuilder.generate(new CD<String>("11884-4",  CdaHandlerConstants.CODE_SYSTEM_LOINC, CdaHandlerConstants.CODE_SYSTEM_NAME_LOINC, null, "Gestational Age", null), gestgationalAgeObs)));

		if(fundalHeightObs != null)
			batteryOrganizer.getComponent().add(new Component4(ActRelationshipHasComponent.HasComponent, BL.TRUE, obsBuilder.generate(new CD<String>("11881-0",  CdaHandlerConstants.CODE_SYSTEM_LOINC, CdaHandlerConstants.CODE_SYSTEM_NAME_LOINC, null, "Fundal Height by tapemeasure", null), fundalHeightObs)));
		if(systolicBpObs != null)
			batteryOrganizer.getComponent().add(new Component4(ActRelationshipHasComponent.HasComponent, BL.TRUE, obsBuilder.generate(new CD<String>("8480-6",  CdaHandlerConstants.CODE_SYSTEM_LOINC, CdaHandlerConstants.CODE_SYSTEM_NAME_LOINC, null, "Blood pressure - Systolic", null), systolicBpObs)));
		if(diastolicBpObs != null)
			batteryOrganizer.getComponent().add(new Component4(ActRelationshipHasComponent.HasComponent, BL.TRUE, obsBuilder.generate(new CD<String>("8462-4",  CdaHandlerConstants.CODE_SYSTEM_LOINC, CdaHandlerConstants.CODE_SYSTEM_NAME_LOINC, null, "Blood pressure - Diastolic", null), diastolicBpObs)));
		if(weightObs != null)
			batteryOrganizer.getComponent().add(new Component4(ActRelationshipHasComponent.HasComponent, BL.TRUE, obsBuilder.generate(new CD<String>("3141-9",  CdaHandlerConstants.CODE_SYSTEM_LOINC, CdaHandlerConstants.CODE_SYSTEM_NAME_LOINC, null, "Body weight measured", null), weightObs)));
		
		return batteryOrganizer;
	}
	/**
	 * Generate the flowsheet batter based on a grouping obs (TBD)
	 */
	public ClinicalStatement generate(BaseOpenmrsData data) {
		throw new NotImplementedException();
	}

}
