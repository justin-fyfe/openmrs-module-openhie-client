package org.openmrs.module.openhie.client.web.controller;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.marc.everest.datatypes.II;
import org.marc.everest.datatypes.generic.CD;
import org.marc.everest.formatters.xml.its1.XmlIts1Formatter;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Author;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.ClinicalDocument;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Section;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.Person;
import org.openmrs.activelist.Allergy;
import org.openmrs.activelist.Problem;
import org.openmrs.api.context.Context;
import org.openmrs.module.openhie.client.CdaHandlerConstants;
import org.openmrs.module.openhie.client.api.HealthInformationExchangeService;
import org.openmrs.module.openhie.client.cda.document.DocumentBuilder;
import org.openmrs.module.openhie.client.cda.section.impl.ActiveProblemsSectionBuilder;
import org.openmrs.module.openhie.client.cda.section.impl.AllergiesIntolerancesSectionBuilder;
import org.openmrs.module.openhie.client.cda.section.impl.AntepartumFlowsheetPanelSectionBuilder;
import org.openmrs.module.openhie.client.cda.section.impl.EstimatedDeliveryDateSectionBuilder;
import org.openmrs.module.openhie.client.cda.section.impl.MedicationsSectionBuilder;
import org.openmrs.module.openhie.client.cda.section.impl.VitalSignsSectionBuilder;
import org.openmrs.module.openhie.client.configuration.CdaHandlerConfiguration;
import org.openmrs.module.openhie.client.everest.EverestUtil;
import org.openmrs.module.openhie.client.hie.model.DocumentInfo;
import org.openmrs.module.openhie.client.util.CdaMetadataUtil;
import org.openmrs.module.openhie.client.web.model.DocumentModel;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

/**
 * HIE Document Import controller
 * @author JustinFyfe
 *
 */
@Transactional
@RequestMapping("/module/openhie-client/hieExportDocument")
public class HieDocumentExportController {

	protected final Log log = LogFactory.getLog(this.getClass());
	
	/**
	 * Build the document
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	private DocumentModel buildDocument(String pid, String encid, Class<? extends DocumentBuilder> clazz) throws InstantiationException, IllegalAccessException
	{
		
		DocumentBuilder builder = clazz.newInstance();
		
		builder.setRecordTarget(Context.getPatientService().getPatient(Integer.parseInt(pid)));
		if(!encid.equals("0"))
			builder.setEncounterEvent(Context.getEncounterService().getEncounter(Integer.parseInt(pid)));

		
		Obs estimatedDeliveryDateObs = null, lastMenstrualPeriodObs = null,
				prepregnancyWeightObs = null, gestgationalAgeObs = null,
				fundalHeightObs = null, systolicBpObs = null,
				diastolicBpObs = null, weightObs = null,
				heightObs = null, presentationObs = null,
				temperatureObs = null;
		List<Obs> medicationObs = new ArrayList<Obs>();
		
		// Obs relevant to this encounter
		Collection<Obs> relevantObs = null;
		if(builder.getEncounterEvent() == Context.getObsService().getObservationsByPerson(builder.getRecordTarget()))
			relevantObs = builder.getEncounterEvent().getAllObs();
		else
			relevantObs = Context.getObsService().getObservationsByPerson(builder.getRecordTarget());
		
		for(Obs obs : relevantObs)
		{
			CD<String> loincCode = CdaMetadataUtil.getInstance().getStandardizedCode(obs.getConcept(), CdaHandlerConstants.CODE_SYSTEM_LOINC, CD.class);
			int conceptId = obs.getConcept().getId();
			// EDD Stuff
			if(obs.getConcept().getId().equals(CdaHandlerConstants.CONCEPT_ID_MEDICATION_HISTORY))
				medicationObs.add(obs);
			else if((conceptId == 5596 || loincCode.getCode() != null && loincCode.getCode().equals("11778-8")) &&
					(estimatedDeliveryDateObs == null || obs.getDateCreated().after(estimatedDeliveryDateObs.getDateCreated())))
					estimatedDeliveryDateObs = obs;
			else if(conceptId == 1427 || loincCode.getCode() != null && loincCode.getCode().equals("8655-2") &&
					(lastMenstrualPeriodObs == null || obs.getDateCreated().after(lastMenstrualPeriodObs.getDateCreated())))
				lastMenstrualPeriodObs = obs;
			else if(loincCode.getCode() != null && loincCode.getCode().equals("8348-5") &&
					(prepregnancyWeightObs == null || obs.getDateCreated().after(prepregnancyWeightObs.getDateCreated())))
				prepregnancyWeightObs = obs;
			else if((conceptId == 1438  || loincCode.getCode() != null && loincCode.getCode().equals("11884-4")) &&
					(gestgationalAgeObs == null || obs.getDateCreated().after(gestgationalAgeObs.getDateCreated())))
				gestgationalAgeObs = obs;
			else if(conceptId == 1439  || loincCode.getCode() != null && loincCode.getCode().equals("11881-0") &&
					(fundalHeightObs == null || obs.getDateCreated().after(fundalHeightObs.getDateCreated())))
				fundalHeightObs = obs;
			else if((conceptId == 5085 || loincCode.getCode() != null && loincCode.getCode().equals("8480-6")) &&
					(systolicBpObs == null || obs.getDateCreated().after(systolicBpObs.getDateCreated())))
				systolicBpObs = obs;
			else if((conceptId == 5086 || loincCode.getCode() != null && loincCode.getCode().equals("8462-4")) &&
					(diastolicBpObs == null || obs.getDateCreated().after(diastolicBpObs.getDateCreated())))
				diastolicBpObs = obs;
			else if((conceptId == 5089  || loincCode.getCode() != null && loincCode.getCode().equals("3141-9")) &&
					(weightObs == null || obs.getDateCreated().after(weightObs.getDateCreated())))
				weightObs = obs;
			else if(loincCode.getCode() != null && loincCode.getCode().equals("11876-0") &&
					(presentationObs == null || obs.getDateCreated().after(presentationObs.getDateCreated())))
				presentationObs = obs;
			else if((conceptId == 5090  || loincCode.getCode() != null && loincCode.getCode().equals("8302-2")) &&
					(heightObs== null || obs.getDateCreated().after(heightObs.getDateCreated())))
				heightObs = obs;
			else if((conceptId == 5088  || loincCode.getCode() != null && loincCode.getCode().equals("8310-5")) &&
					(temperatureObs == null || obs.getDateCreated().after(temperatureObs.getDateCreated())))
				temperatureObs = obs;
			
		}

		
		EstimatedDeliveryDateSectionBuilder eddSectionBuilder = new EstimatedDeliveryDateSectionBuilder();
		AntepartumFlowsheetPanelSectionBuilder flowsheetSectionBuilder = new AntepartumFlowsheetPanelSectionBuilder();
		VitalSignsSectionBuilder vitalSignsSectionBuilder = new VitalSignsSectionBuilder();
		MedicationsSectionBuilder medSectionBuilder = new MedicationsSectionBuilder();
		ActiveProblemsSectionBuilder probBuilder = new ActiveProblemsSectionBuilder();
		AllergiesIntolerancesSectionBuilder allergyBuilder = new AllergiesIntolerancesSectionBuilder();
	
		Section eddSection = null, flowsheetSection = null, vitalSignsSection = null,
				medicationsSection = null, probSection = null, allergySection = null;

		if(estimatedDeliveryDateObs != null && lastMenstrualPeriodObs != null)
			eddSection = eddSectionBuilder.generate(estimatedDeliveryDateObs, lastMenstrualPeriodObs);
		
		if(gestgationalAgeObs!= null && systolicBpObs!= null && diastolicBpObs!= null && weightObs != null)
			flowsheetSection = flowsheetSectionBuilder.generate(prepregnancyWeightObs, gestgationalAgeObs, fundalHeightObs, presentationObs, systolicBpObs, diastolicBpObs, weightObs);
		
		if(systolicBpObs != null &&  diastolicBpObs != null && weightObs != null && heightObs != null && temperatureObs != null)
			vitalSignsSection = vitalSignsSectionBuilder.generate(systolicBpObs, diastolicBpObs, weightObs, heightObs, temperatureObs);
		
		
		medicationsSection = medSectionBuilder.generate(medicationObs.toArray(new Obs[]{}));
		
		Problem[] problems = Context.getActiveListService().getActiveListItems(builder.getRecordTarget(), Problem.ACTIVE_LIST_TYPE).toArray(new Problem[] {});
		Allergy [] allergies = Context.getActiveListService().getActiveListItems(builder.getRecordTarget(), Allergy.ACTIVE_LIST_TYPE).toArray(new Allergy[] {});
		
		if(problems.length > 0)
			probSection = probBuilder.generate(problems);
		
		if(allergies.length > 0)
			allergySection = allergyBuilder.generate(allergies);

		
		// Formatter
		XmlIts1Formatter formatter = EverestUtil.createFormatter();
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try
		{
			ClinicalDocument document = builder.generate(eddSection, flowsheetSection, vitalSignsSection, medicationsSection, probSection, allergySection);
			formatter.graph(bos, document);
			log.debug(String.format("Generated Document: %s", new String(bos.toByteArray())));
			return DocumentModel.createInstance(bos.toByteArray(), builder.getTypeCode(), builder.getFormatCode(), document);
		}
		catch(Exception e)
		{
			log.error("Error generating document:", e);
			log.error(String.format("Generated Document: %s", new String(bos.toByteArray())));
			throw new RuntimeException(e);
		}
		finally
		{
			try {
				bos.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	
	/**
	 * Handle the get operation
	 * @param model
	 */
	@RequestMapping(method = RequestMethod.GET)
	public ModelAndView index(ModelMap model,  @RequestParam(value = "pid") String pid, @RequestParam(value = "encid")String encid, @RequestParam(value = "template")String template) {
		if(pid == null)
			throw new IllegalArgumentException("pid must be supplied");

		
		Patient patient = Context.getPatientService().getPatient(Integer.parseInt(pid));
		
		if(encid == null || encid.equals(""))
		{
			
			// Load the patient
			//Patient patient = Context.getPatientService().getPatient(Integer.parseInt(pid));
			List<Encounter> encounters = Context.getEncounterService().getEncountersByPatient(patient);
			model.put("patient", patient);
			model.put("encounters", encounters);
			return new ModelAndView("/module/openhie-client/hieExportDocumentStep1", model);

		}
		else
		{
			try
			{
				// Generate the document for preview
				model.put("document", this.buildDocument(pid, encid, (Class<? extends DocumentBuilder>) Class.forName(template)));
				model.put("patient", patient);
				return new ModelAndView("/module/openhie-client/hieExportDocumentStep2", model);
			}
			catch(Exception e) {
				// TODO Auto-generated catch block
				log.error("Error generating document", e);
				e.printStackTrace();
				model.put("error", e.getMessage());
				return new ModelAndView("/module/openhie-client/hieExportDocumentStep2", model);
			} 
		}
	}
	
	/**
	 * Handle the post
	 * @return 
	 * @throws ParseException 
	 */
	@RequestMapping(method = RequestMethod.POST)
	public ModelAndView doImport(Map<String, Object> model, @RequestParam(value = "pid") String pid, @RequestParam(value = "encid")String encid, @RequestParam(value = "template")String template) throws ParseException
	{
		if(pid == null)
			throw new IllegalArgumentException("pid must be supplied");
		if(encid == null)
			throw new IllegalArgumentException("pid must be supplied");

		try
		{
			HealthInformationExchangeService service = Context.getService(HealthInformationExchangeService.class);
			CdaHandlerConfiguration config = CdaHandlerConfiguration.getInstance();

			DocumentModel docModel = this.buildDocument(pid, encid, (Class<? extends DocumentBuilder>) Class.forName(template));
			
			DocumentInfo docInfo = new DocumentInfo();
			docInfo.setUniqueId(String.format("2.25.%s", UUID.randomUUID().getMostSignificantBits()));
			
			if(!encid.equals("0"))
				docInfo.setRelatedEncounter(Context.getEncounterService().getEncounter(Integer.parseInt(encid)));
			
			docInfo.setClassCode(docModel.getTypeCode());
			docInfo.setFormatCode(docModel.getFormatCode());
			docInfo.setCreationTime(new Date());
			docInfo.setMimeType("text/xml");
			docInfo.setPatient(Context.getPatientService().getPatient(Integer.parseInt(pid)));
			docInfo.setTitle(docModel.getDocument().getTitle().getValue());

			List<Person> provs = new ArrayList<Person>();
			for(Author aut : docModel.getDocument().getAuthor())
			{
				// Load the author
				for(II id : aut.getAssignedAuthor().getId())
					if(id.getRoot().equals(CdaHandlerConfiguration.getInstance().getProviderRoot()))
						provs.add(Context.getPersonService().getPerson(Integer.parseInt(id.getExtension())));
			}
			docInfo.setAuthors(provs);
			
			docInfo = service.exportDocument(docModel.getData(), docInfo);
			
			// Generate the document
			model.put("document", docModel);
			return new ModelAndView("/module/openhie-client/hieExportDocumentStep3", model);
		}
		catch(Exception e) {
			// TODO Auto-generated catch block
			log.error("Error generating document", e);
			e.printStackTrace();
			model.put("error", e.getMessage());
			return new ModelAndView("/module/openhie-client/hieExportDocumentStep3", model);
		} 
	}
	
}
