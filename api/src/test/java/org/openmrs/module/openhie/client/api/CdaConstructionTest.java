package org.openmrs.module.openhie.client.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.databene.commons.Assert;
import org.junit.Before;
import org.junit.Test;
import org.marc.everest.datatypes.generic.CD;
import org.marc.everest.formatters.FormatterUtil;
import org.marc.everest.interfaces.IResultDetail;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.ClinicalDocument;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Section;
import org.marc.everest.rmim.uv.cdar2.rim.InfrastructureRoot;
import org.openmrs.GlobalProperty;
import org.openmrs.Obs;
import org.openmrs.Visit;
import org.openmrs.api.context.Context;
import org.openmrs.module.openhie.client.api.util.CdaLoggingUtils;
import org.openmrs.module.openhie.client.cda.document.impl.ApsDocumentBuilder;
import org.openmrs.module.openhie.client.cda.section.impl.AntepartumFlowsheetPanelSectionBuilder;
import org.openmrs.module.openhie.client.cda.section.impl.EstimatedDeliveryDateSectionBuilder;
import org.openmrs.module.openhie.client.cda.section.impl.MedicationsSectionBuilder;
import org.openmrs.module.openhie.client.cda.section.impl.VitalSignsSectionBuilder;
import org.openmrs.module.openhie.client.util.CdaDataUtil;
import org.openmrs.module.openhie.client.util.CdaMetadataUtil;
import org.openmrs.module.shr.cdahandler.CdaHandlerConstants;
import org.openmrs.module.shr.cdahandler.api.CdaImportService;
import org.openmrs.module.shr.cdahandler.configuration.CdaHandlerConfiguration;
import org.openmrs.module.shr.cdahandler.exception.DocumentImportException;
import org.openmrs.module.shr.cdahandler.exception.DocumentValidationException;
import org.openmrs.module.shr.cdahandler.processor.util.OpenmrsConceptUtil;
import org.openmrs.module.shr.cdahandler.processor.util.OpenmrsDataUtil;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.openmrs.util.OpenmrsConstants;

public class CdaConstructionTest extends BaseModuleContextSensitiveTest {

	
	private CdaImportService m_importService;
	// Log
	private final Log log = LogFactory.getLog(this.getClass());
	
	private static final String ACTIVE_LIST_INITIAL_XML = "OnDemandTest.xml";
	
	private static final String CIEL_LIST_INITIAL_XML = "CielList.xml";

	/**
	 * Setup the database and get necessary services
	 * @throws Exception 
	 */
	@Before
	public void beforeTest() throws Exception
	{
		this.m_importService = Context.getService(CdaImportService.class);
		super.clearHibernateCache();
		super.initializeInMemoryDatabase();
		GlobalProperty saveDir = new GlobalProperty(OpenmrsConstants.GLOBAL_PROPERTY_COMPLEX_OBS_DIR, "C:\\data\\");
		Context.getAdministrationService().setGlobalProperty(OpenmrsConstants.GLOBAL_PROPERTY_DEFAULT_LOCATION_NAME, "Elbonia Shared Health Authority DC");
		Context.getAdministrationService().setGlobalProperty(CdaHandlerConfiguration.PROP_VALIDATE_CONCEPT_STRUCTURE, "false");
		Context.getAdministrationService().setGlobalProperty("order.nextOrderNumberSeed", "1");
		Context.getAdministrationService().setGlobalProperty(OpenmrsConstants.GLOBAL_PROPERTY_FALSE_CONCEPT, "1066");
        Context.getAdministrationService().setGlobalProperty("shr-cdahandler.cacheMappedConcepts", "false");
		Context.getAdministrationService().saveGlobalProperty(saveDir);
		//GlobalProperty disableValidation = new GlobalProperty(OpenmrsConstants.GP_DISABLE_VALIDATION, "true");
		//Context.getAdministrationService().saveGlobalProperty(disableValidation);
		Context.checkCoreDataset();
		executeDataSet(ACTIVE_LIST_INITIAL_XML);
		executeDataSet(CIEL_LIST_INITIAL_XML);

	}
	
	/**
	 * Do the parsing of a CDA
	 */
	private Visit doParseCda(String resourceName)
	{
		
		URL validAphpSample = this.getClass().getResource(resourceName);
		File fileUnderTest = new File(validAphpSample.getFile());
		FileInputStream fs = null;
		try
		{
			fs = new FileInputStream(fileUnderTest);
			Visit parsedVisit = this.m_importService.importDocument(fs);
			assertEquals(parsedVisit, Context.getVisitService().getVisitByUuid(parsedVisit.getUuid()));
			return parsedVisit;
		}
		catch(DocumentValidationException e)
		{
			log.error(String.format("Error in %s", FormatterUtil.toWireFormat(((InfrastructureRoot)e.getTarget()).getTemplateId())));
			for(IResultDetail dtl : e.getValidationIssues())
				log.error(String.format("%s %s", dtl.getType(), dtl.getMessage()));
			return null;
		}
		catch(DocumentImportException e)
		{
			log.error("Error generated", e);
			return null;
		}
        catch (FileNotFoundException e) {
	        // TODO Auto-generated catch block
	        log.error("Error generated", e);
	        return null;
        }

	}
	
	/**
	 * Generate a simple document first by importing and then by exporting
	 */
	@Test
	public void testGenerateSimpleAps() {
		
		Visit v1 = this.doParseCda("/aps_first_visit.xml");
		assertEquals(Visit.class, v1.getClass());
		
		// Assert we can create an APS
		Obs estimatedDeliveryDateObs = null, lastMenstrualPeriodObs = null,
			prepregnancyWeightObs = null, gestgationalAgeObs = null,
			fundalHeightObs = null, systolicBpObs = null,
			diastolicBpObs = null, weightObs = null,
			heightObs = null, presentationObs = null,
			temperatureObs = null;
		List<Obs> medicationObs = new ArrayList<Obs>();
		
		for(Obs obs : Context.getObsService().getObservationsByPerson(v1.getPatient()))
		{
			CD<String> loincCode = CdaMetadataUtil.getInstance().getStandardizedCode(obs.getConcept(), CdaHandlerConstants.CODE_SYSTEM_LOINC, CD.class);
			// EDD Stuff
			if(obs.getConcept().getId().equals(CdaHandlerConstants.CONCEPT_ID_MEDICATION_HISTORY))
				medicationObs.add(obs);
			else if(loincCode == null || loincCode.getCode() == null)
				continue;
			else if(loincCode.getCode().equals("11778-8"))
				estimatedDeliveryDateObs = obs;
			else if(loincCode.getCode().equals("8655-2"))
				lastMenstrualPeriodObs = obs;
			else if(loincCode.getCode().equals("8348-5"))
				prepregnancyWeightObs = obs;
			else if(loincCode.getCode().equals("11884-4"))
				gestgationalAgeObs = obs;
			else if(loincCode.getCode().equals("11881-0"))
				fundalHeightObs = obs;
			else if(loincCode.getCode().equals("8480-6"))
				systolicBpObs = obs;
			else if(loincCode.getCode().equals("8462-4"))
				diastolicBpObs = obs;
			else if(loincCode.getCode().equals("3141-9"))
				weightObs = obs;
			else if(loincCode.getCode().equals("11876-0"))
				presentationObs = obs;
			else if(loincCode.getCode().equals("8302-2"))
				heightObs = obs;
			else if(loincCode.getCode().equals("8310-5"))
				temperatureObs = obs;
			
		}

		EstimatedDeliveryDateSectionBuilder eddSectionBuilder = new EstimatedDeliveryDateSectionBuilder();
		AntepartumFlowsheetPanelSectionBuilder flowsheetSectionBuilder = new AntepartumFlowsheetPanelSectionBuilder();
		VitalSignsSectionBuilder vitalSignsSectionBuilder = new VitalSignsSectionBuilder();
		MedicationsSectionBuilder medSectionBuilder = new MedicationsSectionBuilder();
		
		Section eddSection = eddSectionBuilder.generate(estimatedDeliveryDateObs, lastMenstrualPeriodObs),
				flowsheetSection = flowsheetSectionBuilder.generate(prepregnancyWeightObs, gestgationalAgeObs, fundalHeightObs, presentationObs, systolicBpObs, diastolicBpObs, weightObs),
				vitalSignsSection = vitalSignsSectionBuilder.generate(systolicBpObs, diastolicBpObs, weightObs, heightObs, temperatureObs),
				medicationsSection = medSectionBuilder.generate(medicationObs.toArray(new Obs[]{})); 
		ApsDocumentBuilder apsBuilder = new ApsDocumentBuilder();
		apsBuilder.setRecordTarget(v1.getPatient());
		apsBuilder.setEncounterEvent(v1.getEncounters().iterator().next());
		ClinicalDocument doc = apsBuilder.generate(eddSection, flowsheetSection, vitalSignsSection, medicationsSection);
		
		log.error(CdaLoggingUtils.getCdaAsString(doc));
		
		assertEquals(2, doc.getTemplateId().size());
		
	}

}
