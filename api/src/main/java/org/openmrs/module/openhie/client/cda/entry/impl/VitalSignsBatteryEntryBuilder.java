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
 * Vital signs battery entry builder
 * @author JustinFyfe
 *
 */
public class VitalSignsBatteryEntryBuilder extends EntryBuilderImpl {

	
	
	/**
	 * Generate the clincal statement from an encounter
	 */
	public ClinicalStatement generate(BaseOpenmrsData data) {
		throw new NotImplementedException();
	}

	/**
	 * Create the organizer from the discrete obs
	 */		
	public ClinicalStatement generate(Obs systolicBpObs, Obs diastolicBpObs, Obs weightObs, Obs heightObs, Obs temperatureObs) {
		Encounter batteryEnc = systolicBpObs.getEncounter();
		
		/*if(systolicBpObs != null && !(systolicBpObs.getConcept() instanceof ConceptNumeric))
			throw new IllegalArgumentException("systolicBpObs must be a numeric concept");
		else if(diastolicBpObs != null && !(diastolicBpObs.getConcept() instanceof ConceptNumeric))
			throw new IllegalArgumentException("diastolicBpObs must be a numeric concept");
		else if(weightObs != null && !(weightObs.getConcept() instanceof ConceptNumeric))
			throw new IllegalArgumentException("weightObs must be a numeric concept");
		else if(heightObs != null && !(heightObs.getConcept() instanceof ConceptNumeric))
			throw new IllegalArgumentException("heightObs must be a numeric concept");
		else if(temperatureObs != null && !(temperatureObs.getConcept() instanceof ConceptNumeric))
			throw new IllegalArgumentException("temperatureObs must be a numeric concept");
		else*/ if(heightObs != null && batteryEnc.getId() != heightObs.getEncounter().getId() ||
				systolicBpObs != null && batteryEnc.getId() != systolicBpObs.getEncounter().getId() ||
				diastolicBpObs != null && batteryEnc.getId() != diastolicBpObs.getEncounter().getId() ||
				temperatureObs != null && batteryEnc.getId() != temperatureObs.getEncounter().getId() ||
				weightObs != null && batteryEnc.getId() != weightObs.getEncounter().getId())
			throw new IllegalArgumentException("All arguments for the flowsheet panel must come from the same encounter");
		
		Organizer batteryOrganizer = super.createOrganizer(x_ActClassDocumentEntryOrganizer.BATTERY, 
				Arrays.asList(CdaHandlerConstants.ENT_TEMPLATE_VITAL_SIGNS_ORGANIZER, CdaHandlerConstants.ENT_TEMPLATE_CCD_VITAL_SIGNS_ORGANIZER),
				new CD<String>("46680005", CdaHandlerConstants.CODE_SYSTEM_SNOMED, "SNOMED CT", null, "Vital Signs", null), 
				new II(this.m_cdaConfiguration.getEncounterRoot(), batteryEnc.getId().toString()), 
				ActStatus.Completed, 
				batteryEnc.getEncounterDatetime());
		
		SimpleObservationEntryBuilder obsBuilder = new SimpleObservationEntryBuilder();
		if(systolicBpObs != null)
			batteryOrganizer.getComponent().add(new Component4(ActRelationshipHasComponent.HasComponent, BL.TRUE, obsBuilder.generate(new CD<String>("8480-6",  CdaHandlerConstants.CODE_SYSTEM_LOINC, CdaHandlerConstants.CODE_SYSTEM_NAME_LOINC, null, "Blood pressure - Systolic", null), systolicBpObs)));
		if(diastolicBpObs != null)
			batteryOrganizer.getComponent().add(new Component4(ActRelationshipHasComponent.HasComponent, BL.TRUE, obsBuilder.generate(new CD<String>("8462-4",  CdaHandlerConstants.CODE_SYSTEM_LOINC, CdaHandlerConstants.CODE_SYSTEM_NAME_LOINC, null, "Blood pressure - Diastolic", null), diastolicBpObs)));
		if(weightObs != null)
			batteryOrganizer.getComponent().add(new Component4(ActRelationshipHasComponent.HasComponent, BL.TRUE, obsBuilder.generate(new CD<String>("3141-9",  CdaHandlerConstants.CODE_SYSTEM_LOINC, CdaHandlerConstants.CODE_SYSTEM_NAME_LOINC, null, "Body weight measured", null), weightObs)));
		if(heightObs != null)
			batteryOrganizer.getComponent().add(new Component4(ActRelationshipHasComponent.HasComponent, BL.TRUE, obsBuilder.generate(new CD<String>("8302-2",  CdaHandlerConstants.CODE_SYSTEM_LOINC, CdaHandlerConstants.CODE_SYSTEM_NAME_LOINC, null, "Body height measured", null), heightObs)));
		if(temperatureObs != null)
			batteryOrganizer.getComponent().add(new Component4(ActRelationshipHasComponent.HasComponent, BL.TRUE, obsBuilder.generate(new CD<String>("8310-5",  CdaHandlerConstants.CODE_SYSTEM_LOINC, CdaHandlerConstants.CODE_SYSTEM_NAME_LOINC, null, "Body Temperature", null), temperatureObs)));
	
		return batteryOrganizer;
	}

}
