package org.openmrs.module.openhie.client.web.controller;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.marc.everest.datatypes.generic.CE;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.ClinicalDocument;
import org.marc.everest.rmim.uv.cdar2.vocabulary.ParticipationAuthorOriginator;
import org.openmrs.Concept;
import org.openmrs.Drug;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.Visit;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.module.openhie.client.api.HealthInformationExchangeService;
import org.openmrs.module.openhie.client.exception.HealthInformationExchangeException;
import org.openmrs.module.openhie.client.hie.model.DocumentInfo;
import org.openmrs.module.openhie.client.odd.documentgenerator.ImmunizationContentGenerator;
import org.openmrs.module.shr.cdahandler.CdaHandlerConstants;
import org.openmrs.module.shr.cdahandler.everest.EverestUtil;
import org.openmrs.module.shr.cdahandler.exception.DocumentImportException;
import org.openmrs.module.shr.cdahandler.processor.util.OpenmrsConceptUtil;
import org.openmrs.module.shr.cdahandler.processor.util.OpenmrsMetadataUtil;
import org.openmrs.module.shr.odd.generator.document.impl.CcdGenerator;
import org.openmrs.module.shr.odd.generator.section.impl.ImmunizationsSectionGenerator;
import org.openmrs.module.shr.odd.model.OnDemandDocumentEncounterLink;
import org.openmrs.module.shr.odd.model.OnDemandDocumentRegistration;
import org.openmrs.module.shr.odd.model.OnDemandDocumentType;
import org.openmrs.module.shr.odd.util.OddMetadataUtil;
import org.openmrs.obs.ComplexData;
import org.openmrs.web.controller.PortletController;
import org.springframework.transaction.annotation.Transactional;

/**
 * Vaccination tab controller 
 */
@Transactional
public class VaccinationTabController extends PortletController {

	protected static Log log = LogFactory.getLog(VaccinationTabController.class);

	/**
     * @see org.openmrs.web.controller.PortletController#populateModel(javax.servlet.http.HttpServletRequest, java.util.Map)
     */
    @Override
    protected void populateModel(HttpServletRequest request, Map<String, Object> model) {

		Object pidFromModel = model.get("patientId");
		Integer pid = pidFromModel instanceof Integer ? (Integer)pidFromModel : Integer.parseInt(pidFromModel.toString());
		Patient patient = Context.getPatientService().getPatient(pid);

		
		// Post?
		if(request.getMethod().equals("POST") && request.getParameterValues("vaccine") != null)
		{
			Encounter enc = new Encounter();
			try {
				enc.setEncounterDatetime(new Date());
				enc.setPatient(patient);
				enc.setEncounterType(OpenmrsMetadataUtil.getInstance().getOrCreateEncounterType(new CE<String>("11369-1", CdaHandlerConstants.CODE_SYSTEM_LOINC, null, null, "Immunization History", null)));
				enc.setLocation(Context.getLocationService().getDefaultLocation());
				//enc.setProvider(Context.getAuthenticatedUser());
				Context.getEncounterService().saveEncounter(enc);

				Visit v = new Visit();
				v.setStartDatetime(new Date());
				v.setStopDatetime(new Date());
				v.setPatient(patient);
				v.setVisitType(OpenmrsMetadataUtil.getInstance().getVisitType("Immunization Summary"));
				v.setLocation(Context.getLocationService().getDefaultLocation());
				v.addEncounter(enc);
				v = Context.getVisitService().saveVisit(v);
				
            }
            catch (DocumentImportException e) {
	            // TODO Auto-generated catch block
	            log.error("Error generated", e);
            }
			Obs obsGroup;
            try {
	            obsGroup = new Obs(patient, new ImmunizationsSectionGenerator().getSectionObsGroupConcept(), new Date(), Context.getLocationService().getDefaultLocation());
				obsGroup.setEncounter(enc);
				ComplexData cda = new ComplexData(UUID.randomUUID().toString(), "Generated from VAC form");
				obsGroup.setComplexData(cda);
				Context.getObsService().saveObs(obsGroup, null);
				
				// Add vaccination
				for(String val : request.getParameterValues("vaccine"))
				{
					log.warn("registering " + val);
					Integer conceptId = Integer.parseInt(val);
					Concept concept = Context.getConceptService().getConcept(conceptId);
					this.addVaccination(patient, obsGroup, concept);
				}				
            }
            catch (APIException e) {
	            // TODO Auto-generated catch block
	            log.error("Error generated", e);
            }
            catch (DocumentImportException e) {
	            // TODO Auto-generated catch block
	            log.error("Error generated", e);
            }
            
            HealthInformationExchangeService hieService = Context.getService(HealthInformationExchangeService.class);
            try {
    			// Create a fake ODD reg for the ODD mod
    			OnDemandDocumentRegistration oddReg = new OnDemandDocumentRegistration();
    			oddReg.setEncounterLinks(new HashSet<OnDemandDocumentEncounterLink>());
				OnDemandDocumentEncounterLink oddel = new OnDemandDocumentEncounterLink();
				oddel.setEncounter(enc);
				oddReg.getEncounterLinks().add(oddel);
    			oddReg.setId(enc.getId());
    			oddReg.setPatient(patient);
    			oddReg.setTitle(enc.getEncounterType().getDescription());
    			OnDemandDocumentType icType = new OnDemandDocumentType();
    			icType.setId(4);
    			icType.setName("Immunization Content");
    			oddReg.setType(icType);
    			ByteArrayOutputStream bos = new ByteArrayOutputStream();
    			ClinicalDocument doc = new ImmunizationContentGenerator().generateDocument(oddReg);
    			EverestUtil.createFormatter().graph(bos, doc);
    			log.debug(bos.toByteArray());
    			DocumentInfo info = new DocumentInfo();
    			info.setPatient(patient);
    			info.setRelatedEncounter(enc);
    			info.setMimeType("text/xml");
    			info.setTitle(enc.getEncounterType().getName());
    			info.setFormatCode("urn:ihe:pcc:ic:2009");
    			info.setClassCode("11369-6");
	            hieService.exportDocument(bos.toByteArray(), info);
            }
            catch (HealthInformationExchangeException e) {
	            // TODO Auto-generated catch block
	            log.error("Error generated", e);
            }
            
		}
		
		// Now, get observations for the model
		for(Obs immuzObs : Context.getObsService().getObservations(patient, Context.getConceptService().getConcept(CdaHandlerConstants.CONCEPT_ID_IMMUNIZATION_DRUG), false))
		{
			log.error(immuzObs.getValueCoded());
			model.put("v" + immuzObs.getValueCoded().getId().toString(), String.format("- Given %s", new SimpleDateFormat("yyyy-MM-dd").format(immuzObs.getObsDatetime())));
		}
	    // TODO Auto-generated method stub
	    //super.populateModel(request, model);
    }

	/**
	 * Add the vaccination
	 * @throws DocumentImportException 
	 */
	private void addVaccination(Patient patient, Obs obsGroup, Concept concept) throws DocumentImportException {
		Obs immunization = new Obs(patient, Context.getConceptService().getConcept(CdaHandlerConstants.CONCEPT_ID_IMMUNIZATION_HISTORY), new Date(), Context.getLocationService().getDefaultLocation());
		immunization.setObsGroup(obsGroup);
		immunization.setEncounter(obsGroup.getEncounter());
		// Add group members
		Obs immunizationDrug = new Obs(patient, Context.getConceptService().getConcept(CdaHandlerConstants.CONCEPT_ID_IMMUNIZATION_DRUG), new Date(), Context.getLocationService().getDefaultLocation());
		// Get the concept drug
		//CE<String> drugCode = OddMetadataUtil.getInstance().getStandardizedCode(concept, CdaHandlerConstants.CODE_SYSTEM_CVX, CE.class);
		immunizationDrug.setValueCoded(concept);
		immunization.addGroupMember(immunizationDrug);
		immunizationDrug.setEncounter(obsGroup.getEncounter());
		
		Obs immunizationDate = new Obs(patient, Context.getConceptService().getConcept(CdaHandlerConstants.CONCEPT_ID_IMMUNIZATION_DATE), new Date(), Context.getLocationService().getDefaultLocation());
		immunizationDate.setValueDate(new Date());
		immunization.addGroupMember(immunizationDate);
		immunizationDate.setEncounter(obsGroup.getEncounter());

		Context.getObsService().saveObs(immunization, null);
		
	    
    } 
	
	
	
}
