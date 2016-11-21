package org.openmrs.module.openhie.client.cda.entry.impl;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.marc.everest.datatypes.BL;
import org.marc.everest.datatypes.ED;
import org.marc.everest.datatypes.EN;
import org.marc.everest.datatypes.ENXP;
import org.marc.everest.datatypes.II;
import org.marc.everest.datatypes.INT;
import org.marc.everest.datatypes.NullFlavor;
import org.marc.everest.datatypes.PQ;
import org.marc.everest.datatypes.SetOperator;
import org.marc.everest.datatypes.TS;
import org.marc.everest.datatypes.generic.CD;
import org.marc.everest.datatypes.generic.CE;
import org.marc.everest.datatypes.generic.CS;
import org.marc.everest.datatypes.generic.CV;
import org.marc.everest.datatypes.generic.DomainTimingEvent;
import org.marc.everest.datatypes.generic.EIVL;
import org.marc.everest.datatypes.generic.IVL;
import org.marc.everest.datatypes.generic.LIST;
import org.marc.everest.datatypes.generic.PIVL;
import org.marc.everest.datatypes.generic.SET;
import org.marc.everest.datatypes.generic.SXCM;
import org.marc.everest.datatypes.interfaces.ISetComponent;
import org.marc.everest.formatters.FormatterUtil;
import org.marc.everest.interfaces.IEnumeratedVocabulary;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Act;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Author;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Consumable;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Criterion;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.EntryRelationship;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.ExternalAct;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.ManufacturedProduct;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Material;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Observation;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Organizer;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Performer2;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Precondition;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Product;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Reference;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.SubstanceAdministration;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Supply;
import org.marc.everest.rmim.uv.cdar2.vocabulary.ActStatus;
import org.marc.everest.rmim.uv.cdar2.vocabulary.ContextControl;
import org.marc.everest.rmim.uv.cdar2.vocabulary.ObservationInterpretation;
import org.marc.everest.rmim.uv.cdar2.vocabulary.RoleClassManufacturedProduct;
import org.marc.everest.rmim.uv.cdar2.vocabulary.x_ActClassDocumentEntryAct;
import org.marc.everest.rmim.uv.cdar2.vocabulary.x_ActClassDocumentEntryOrganizer;
import org.marc.everest.rmim.uv.cdar2.vocabulary.x_ActMoodDocumentObservation;
import org.marc.everest.rmim.uv.cdar2.vocabulary.x_ActRelationshipEntryRelationship;
import org.marc.everest.rmim.uv.cdar2.vocabulary.x_ActRelationshipExternalReference;
import org.marc.everest.rmim.uv.cdar2.vocabulary.x_DocumentActMood;
import org.marc.everest.rmim.uv.cdar2.vocabulary.x_DocumentProcedureMood;
import org.marc.everest.rmim.uv.cdar2.vocabulary.x_DocumentSubstanceMood;
import org.openmrs.BaseOpenmrsData;
import org.openmrs.Concept;
import org.openmrs.Drug;
import org.openmrs.Obs;
import org.openmrs.Provider;
import org.openmrs.activelist.ActiveListItem;
import org.openmrs.api.context.Context;
import org.openmrs.module.openhie.client.cda.entry.EntryBuilder;
import org.openmrs.module.openhie.client.util.CdaDataUtil;
import org.openmrs.module.openhie.client.util.CdaMetadataUtil;
import org.openmrs.module.shr.cdahandler.CdaHandlerConstants;
import org.openmrs.module.shr.cdahandler.api.CdaImportService;
import org.openmrs.module.shr.cdahandler.configuration.CdaHandlerConfiguration;
import org.openmrs.module.shr.cdahandler.obs.ExtendedObs;
import org.openmrs.module.shr.cdahandler.processor.entry.impl.ihe.pcc.ConcernEntryProcessor;
import org.openmrs.util.OpenmrsConstants;

/**
 * Observation entry generator
 * @author JustinFyfe
 *
 */
public abstract class EntryBuilderImpl implements EntryBuilder {

	// CDA data utilities & configuration
	protected CdaDataUtil m_cdaDataUtil = CdaDataUtil.getInstance();
	protected CdaHandlerConfiguration m_cdaConfiguration = CdaHandlerConfiguration.getInstance();
	protected CdaMetadataUtil m_cdaMetadataUtil = CdaMetadataUtil.getInstance();
	
	// Unknown codes
	private static final CD<String> s_drugTreatmentUnknownCode = new CD<String>("182904002", CdaHandlerConstants.CODE_SYSTEM_SNOMED, null, null, "Drug Treatment Unknown", null);
	
	// log
	protected final Log log = LogFactory.getLog(this.getClass());
	protected static final String REGEX_IVL_PQ = "^\\{?([\\d.]*)?\\s(\\w*)?\\s?\\.*\\s?([\\d.]*)?\\s?([\\w]*?)?\\}?$";

	/**
	 * Get the identifier list
	 */
	protected SET<II> getIdentifierList(BaseOpenmrsData source) {
		SET<II> retVal = new SET<II>();
		
		if(source instanceof Obs)
		{
			Obs sourceObs = (Obs)source;
		    if(sourceObs.getAccessionNumber() != null && !sourceObs.getAccessionNumber().isEmpty())
		    {
		    	II ii = this.m_cdaDataUtil.parseIIFromString(sourceObs.getAccessionNumber());
		    	if(ii.getRoot() != null && !ii.getRoot().isEmpty())
		    		retVal.add(ii);
		    }
		    retVal.add(new II(this.m_cdaConfiguration.getObsRoot(), sourceObs.getId().toString()));
		}
		
	    return retVal;
    }
	
	/**
	 * Correct a code to a more preferred code system
	 */
	protected void correctCode(CE<?> code, String... codeSystems) {

		if(code.isNull())
			return;

		// Already preferred
		for(String cs : codeSystems)
			if(code.getCodeSystem().equals(cs))
				return; 
		
		// Get translation
		code.getTranslation().add(new CD(code.getCode(), code.getCodeSystem(), code.getCodeSystemName(), code.getCodeSystemVersion(), code.getDisplayName(), null));
		// Move the first translation to the root code
		for(String cs : codeSystems)
		{
			for(CD<?> tx : code.getTranslation())
				if(tx.getCodeSystem().equals(cs))
				{
					code.setCode(tx.getCode());
					code.setCodeSystem(tx.getCodeSystem());
					code.setDisplayName(tx.getDisplayName());
					code.setCodeSystemName(tx.getCodeSystemName());
					code.setCodeSystemVersion(tx.getCodeSystemVersion());
					code.getTranslation().remove(tx);
					return;
				}
		}
		
		// Not found :| ... Null Flavor it with OTH
		code.setNullFlavor(NullFlavor.Other);
		code.setCodeSystemName(null);
		code.setDisplayName(null);
		code.setCodeSystemVersion(null);
		code.setCode(null);
		code.setCodeSystem(codeSystems[0]);
		
    }
	/**
	 * Create an author node that points to correct information
	 */
	protected Author createAuthorPointer(BaseOpenmrsData sourceData) {
		Author retVal = new Author(ContextControl.OverridingPropagating);
		if(sourceData.getChangedBy() != null)
		{
			retVal.setTime(this.m_cdaDataUtil.createTS(sourceData.getDateChanged()));
			Collection<Provider> providers = Context.getProviderService().getProvidersByPerson(sourceData.getChangedBy().getPerson());
			Provider pvdr = providers.iterator().next();
			retVal.setAssignedAuthor(this.m_cdaDataUtil.createAuthorPerson(pvdr));
//			retVal.setAssignedAuthor(new AssignedAuthor(SET.createSET(new II(this.m_cdaConfiguration.getUserRoot(), sourceData.getChangedBy().getId().toString()))));
		}
		else
		{
			retVal.setTime(this.m_cdaDataUtil.createTS(sourceData.getDateCreated()));
			Collection<Provider> providers = Context.getProviderService().getProvidersByPerson(sourceData.getCreator().getPerson());
			if(providers.size() > 0)
			{
				Provider pvdr = providers.iterator().next();
				log.debug(String.format("Author %s", pvdr));
				retVal.setAssignedAuthor(this.m_cdaDataUtil.createAuthorPerson(pvdr));
			}
			else
			{
				log.error("No provider is found for this observation");
			}
			//retVal.setAssignedAuthor(new AssignedAuthor(SET.createSET(new II(this.m_cdaConfiguration.getUserRoot(), sourceData.getCreator().getId().toString()))));
		}
		return retVal;
    }
	
	/**
	 * Create a consumable
	 */
	private Consumable createConsumable(Concept valueConcept, Drug valueDrug, String preferredCs) {
		// Create the product
		Consumable consumable = new Consumable();
		ManufacturedProduct product = new ManufacturedProduct(RoleClassManufacturedProduct.ManufacturedProduct);
		product.setTemplateId(LIST.createLIST(new II(CdaHandlerConstants.ENT_TEMPLATE_CCD_MEDICATION_PRODUCT), new II(CdaHandlerConstants.ENT_TEMPLATE_PRODUCT)));

		Material manufacturedMaterial = new Material();
		product.setManufacturedDrugOrOtherMaterial(manufacturedMaterial);
		
		
		// Drug code
		CE<String> code = this.m_cdaMetadataUtil.getStandardizedCode(valueConcept, preferredCs, CE.class);
			manufacturedMaterial.setCode(code);
		
		// Now get the drug from the concept
		if(valueDrug != null && valueDrug.getName() != null)
			manufacturedMaterial.setName(new EN(Arrays.asList(new ENXP(valueDrug.getName()))));

		consumable.setManufacturedProduct(product);
		return consumable;
    }


	/**
	 * Set extended observation properties
	 */
	protected void setExtendedObservationProperties(Observation cdaObservation, ExtendedObs extendedObs) {

    	if(extendedObs.getObsInterpretation() != null)
    		cdaObservation.setInterpretationCode(SET.createSET((CE<ObservationInterpretation>)this.m_cdaMetadataUtil.getStandardizedCode(extendedObs.getObsInterpretation(), ObservationInterpretation.Abnormal.getCodeSystem(), CE.class)));
    	if(extendedObs.getObsRepeatNumber() != null)
    		cdaObservation.setRepeatNumber(new INT(extendedObs.getObsRepeatNumber()));
    	
    }

	/**
	 * Get the mood code
	 */
	protected <T extends IEnumeratedVocabulary> CS<T> getMoodCode(Obs obs, Class<T> vocabulary) {
		 if(obs instanceof ExtendedObs)
		    {
		    	ExtendedObs extendedObs = (ExtendedObs)obs;
		    	CS<String> status = this.m_cdaMetadataUtil.getStandardizedCode(extendedObs.getObsMood(), x_DocumentProcedureMood.Eventoccurrence.getCodeSystem(), CS.class);
		    	if(status.isNull())
		    		return new CS<T>(FormatterUtil.fromWireFormat("EVN", vocabulary));
		    	else
		    		return new CS<T>(FormatterUtil.fromWireFormat(status.getCode(), vocabulary));
		    }
			 else
			 {
				 return new CS<T>(FormatterUtil.fromWireFormat("EVN", vocabulary));
			 }
    }

	/**
	 * Get the effective time
	 */
	protected IVL<TS> getEffectiveTime(BaseOpenmrsData data) {
		IVL<TS> retVal = new IVL<TS>();
		
		if(data instanceof ExtendedObs)
		{
			ExtendedObs extendedObs = (ExtendedObs)data;
	    	// status?
	    	if(extendedObs.getObsDatetime() != null &&
	    			extendedObs.getObsStartDate() == null &&
	    			extendedObs.getObsEndDate() == null)
	    		retVal.setValue(this.m_cdaDataUtil.createTS(extendedObs.getObsDatetime()));
	    	else
	    	{
	    		retVal.setValue(null);
		    	if(extendedObs.getObsStartDate() != null)
		    		retVal.setLow(this.m_cdaDataUtil.createTS(extendedObs.getObsStartDate()));
		    	if(extendedObs.getObsEndDate() != null)
		    		retVal.setHigh(this.m_cdaDataUtil.createTS(extendedObs.getObsEndDate()));
			}
	    	
	    	// Null ?
	    	if(extendedObs.getObsDatePrecision() == 0)
	    		retVal.setNullFlavor(NullFlavor.Unknown);
	    	
	    	// Set precision
	    	if(retVal.getValue() != null)
	    		retVal.getValue().setDateValuePrecision(extendedObs.getObsDatePrecision());
	    	if(retVal.getLow() != null)
	    		retVal.getLow().setDateValuePrecision(extendedObs.getObsDatePrecision());
	    	if(retVal.getHigh() != null)
	    		retVal.getHigh().setDateValuePrecision(extendedObs.getObsDatePrecision());
		}
		else if(data instanceof Obs)
		{
			Obs obs = (Obs)data;
			retVal.setValue(this.m_cdaDataUtil.createTS(obs.getObsDatetime()));
		}
		else
			retVal.setValue(this.m_cdaDataUtil.createTS(data.getDateCreated()));
		return retVal;
    }

	/**
	 * Get the status code of the object
	 */
	protected CS<ActStatus> getStatusCode(BaseOpenmrsData obs) {
		 if(obs instanceof ExtendedObs)
	    {
	    	ExtendedObs extendedObs = (ExtendedObs)obs;
	    	CS<String> status = this.m_cdaMetadataUtil.getStandardizedCode(extendedObs.getObsStatus(), ActStatus.Aborted.getCodeSystem(), CS.class);
	    	return new CS<ActStatus>(new ActStatus(status.getCode(), ActStatus.Completed.getCodeSystem()));
	    }
		 else
			 return new CS<ActStatus>(ActStatus.Completed);
    }
	
	/**
	 * Get the template id list
	 * Auto generated method comment
	 * 
	 * @param templateIds
	 * @return
	 */
	protected LIST<II> getTemplateIdList(List<String> templateIds)
	{
		LIST<II> retVal = null;
		if(templateIds.size() > 0)
		{
			retVal = new LIST<II>();
			for(String tplId : templateIds)
				retVal.add(new II(tplId));
		}
		return retVal;

	}
	
	/**
	 * Create an observation from an Obs
	 */
	protected Observation createObservation(List<String> templateId, CD<String> code, Obs sourceObs)
	{
		Observation retVal = new Observation(x_ActMoodDocumentObservation.Eventoccurrence);
	    
		// Template identifiers
		retVal.setTemplateId(this.getTemplateIdList(templateId));
		
	    // Add identifier
		retVal.setId(this.getIdentifierList(sourceObs));
	    
	    // Add the code
	    retVal.setCode(code);
	    
	    // Is there a creation time?
	    retVal.getAuthor().add(this.createAuthorPointer(sourceObs));
	    
	    // Value .. the tricky part
	    retVal.setValue(this.m_cdaDataUtil.getObservationValue(sourceObs));
	    
	    if(sourceObs.getComment() != null)
	        try {
	            retVal.setText(sourceObs.getComment());
            }
            catch (UnsupportedEncodingException e) {
	            // TODO Auto-generated catch block
	            log.error("Error generated", e);
            }

	    
	    // Set the status, mood, and effective time
    	retVal.setStatusCode(this.getStatusCode(sourceObs));
    	retVal.setEffectiveTime(this.getEffectiveTime(sourceObs));
    	retVal.setMoodCode(this.getMoodCode(sourceObs, x_ActMoodDocumentObservation.class));

	    // Extended observation stuff
	    ExtendedObs extendedObs = Context.getService(CdaImportService.class).getExtendedObs(sourceObs.getId());
	    if(extendedObs != null)
	    	this.setExtendedObservationProperties(retVal, extendedObs);
	    
	    // Replacement?
	    if(sourceObs.getPreviousVersion() != null)
	    {
	    	Reference prevRef = new Reference(x_ActRelationshipExternalReference.RPLC);
	    	ExternalAct externalAct = new ExternalAct(new CD<String>("OBS"));
	    	externalAct.setId(SET.createSET(new II(this.m_cdaConfiguration.getObsRoot(), sourceObs.getPreviousVersion().getId().toString())));
	    	if(sourceObs.getPreviousVersion().getAccessionNumber() != null)
	    		externalAct.getId().add(this.m_cdaDataUtil.parseIIFromString(sourceObs.getPreviousVersion().getAccessionNumber()));
	    	prevRef.setExternalActChoice(externalAct);
	    	retVal.getReference().add(prevRef);
	    }
	    	
	    return retVal;
	}
	
	/**
	 * Creates an organizer from the specified content
	 */
	protected Organizer createOrganizer(x_ActClassDocumentEntryOrganizer classCode, List<String> templateId, CD<String> code, II id, ActStatus status,
                                     Date effectiveTime) {
		Organizer retVal = new Organizer(classCode, status);
		retVal.setTemplateId(this.getTemplateIdList(templateId));
		
		// Other attributes
		if(id != null)
			retVal.setId(SET.createSET(id));
		
		// code and effective time
		retVal.setCode(code);
		retVal.setEffectiveTime(this.m_cdaDataUtil.createTS(effectiveTime));
		
		return retVal;
		
    }

	/**
	 * Parse dose quantity
	 */
	private IVL<PQ> parseDoseQuantity(String valueText) {
			Pattern regexPattern = Pattern.compile(REGEX_IVL_PQ);
			Matcher match = regexPattern.matcher(valueText);
			IVL<PQ> retVal = null;
			
			if(match.matches())
			{
				// Group 1 and 2 are the value and dose and 3 and 4 are another
				PQ group1 = null;
				if(match.group(1) != null && !match.group(1).isEmpty())
					group1 = new PQ(new BigDecimal(match.group(1)), match.group(2));
				if(match.groupCount() > 3 && match.group(3) != null && !match.group(3).isEmpty())
				{
					PQ group2 = new PQ(new BigDecimal(match.group(3)), match.group(4));
					// Range
					retVal = new IVL<PQ>(group1, group2);
				}
				else if(valueText.contains("{"))
					retVal = new IVL<PQ>(group1, null);
				else
					retVal = new IVL<PQ>(group1);
				
			}
			else
				throw new RuntimeException(String.format("Can't understand value %s", valueText));
	
			return retVal;
	}

	/**
	 * Create an Act
	 */
	protected Act createAct(x_ActClassDocumentEntryAct classCode, x_DocumentActMood moodCode, List<String> templateId, ActiveListItem activeListItem) {
		Act retVal = new Act();
		retVal.setClassCode(classCode);
		retVal.setMoodCode(moodCode);
		
		retVal.setTemplateId(this.getTemplateIdList(templateId));
		
	    // Add identifier
	    retVal.setId(new SET<II>());
	    if(activeListItem.getStartObs() != null && activeListItem.getStartObs().getAccessionNumber() != null &&
	    		activeListItem.getStartObs().getAccessionNumber().isEmpty())
	    	retVal.getId().add(this.m_cdaDataUtil.parseIIFromString(activeListItem.getStartObs().getAccessionNumber()));
	    
	    retVal.getId().add(new II(this.m_cdaConfiguration.getProblemRoot(), activeListItem.getId().toString()));
	    // Add the code
	    retVal.setCode(new CD<String>());
	    retVal.getCode().setNullFlavor(NullFlavor.NotApplicable);
	    
	    // Now add reference the status code
	    IVL<TS> eft = new IVL<TS>();
	    if(activeListItem.getStartObs() != null)
	    {
    		eft.setLow(this.m_cdaDataUtil.createTS(activeListItem.getStartDate()));
	    	if(activeListItem.getStartObs() != null)
	    	{
	    		// Correct the precision of the dates
	    		ExtendedObs obs = Context.getService(CdaImportService.class).getExtendedObs(activeListItem.getStartObs().getId());
	    		if(obs != null && obs.getObsDatePrecision() == 0)
	    			eft.getLow().setNullFlavor(NullFlavor.Unknown);
	    		else if(obs != null)
	    			eft.getLow().setDateValuePrecision(obs.getObsDatePrecision());
	    	}
	    }
	    else if(activeListItem.getStartDate() != null)
	    {
	    	eft.setLow(this.m_cdaDataUtil.createTS(activeListItem.getStartDate()));
	    }
	    if(activeListItem.getStopObs() != null)
	    {
	    	eft.setHigh(this.m_cdaDataUtil.createTS(activeListItem.getEndDate()));
	    	if(activeListItem.getStopObs() != null)
	    	{
	    		// Correct the precision of the dates
	    		ExtendedObs obs = Context.getService(CdaImportService.class).getExtendedObs(activeListItem.getStopObs().getId());
	    		if(obs != null && obs.getObsDatePrecision() == 0)
	    			eft.getHigh().setNullFlavor(NullFlavor.Unknown);
	    		else if(obs != null)
	    			eft.getHigh().setDateValuePrecision(obs.getObsDatePrecision());
	    	}
	    	
	    }

	    retVal.setEffectiveTime(eft);
	    
	    // Is there a creation time?
    	retVal.getAuthor().add(this.createAuthorPointer(activeListItem));
	    
	    retVal.setStatusCode(ConcernEntryProcessor.calculateCurrentStatus(activeListItem));;
	    
		return retVal;
    }

	/**
	 * Create a substance administration from an observation
	 */
	protected SubstanceAdministration createSubstanceAdministration(List<String> templateIds, Obs sourceObs) {
		
		SubstanceAdministration retVal = new SubstanceAdministration();

		// Set the mood code
	    ExtendedObs extendedObs = Context.getService(CdaImportService.class).getExtendedObs(sourceObs.getId());

    	retVal.setMoodCode(x_DocumentSubstanceMood.Eventoccurrence);
	    
	    retVal.setTemplateId(this.getTemplateIdList(templateIds));

	    // Identifiers
	    retVal.setId(this.getIdentifierList(sourceObs));
	    
	    retVal.getAuthor().add(this.createAuthorPointer(sourceObs));
	    
	    // This is the time that the medication was taken
    	IVL<TS> effectiveTimePeriod = new IVL<TS>();
    	SXCM<TS> effectiveTimeInstant = new SXCM<TS>();
    	ISetComponent<TS> frequencyExpression = null, 
    			effectiveTime = null;

    	retVal.setMoodCode(this.getMoodCode(sourceObs, x_DocumentSubstanceMood.class));
    	//retVal.setStatusCode(this.getStatusCode(sourceObs));
		retVal.setStatusCode(ActStatus.Completed);
    	
	    // Effective time and extended observation properties
	    if(extendedObs != null)
	    {
	    	if(extendedObs.getObsRepeatNumber() != null)
	    		retVal.setRepeatNumber(new INT(extendedObs.getObsRepeatNumber()));
	    	
	    	// Set times
	    	effectiveTimePeriod = this.getEffectiveTime(extendedObs);
	    	effectiveTimeInstant.setValue(effectiveTimePeriod.getValue());
	    }
	    
	    // Now sub-observations
	    Set<Obs> componentObs = sourceObs.getGroupMembers();
	    
	    int cSequence = 0;
	    
	    // Process the sub-observations
	    for(Obs component : componentObs)
	    {
	    	switch(component.getConcept().getId())
	    	{
	    		case CdaHandlerConstants.CONCEPT_ID_MEDICATION_TEXT:
	    			if(component.getValueText().startsWith("Instructions"))
	    			{
	    				Act instructionsAct = new Act(x_ActClassDocumentEntryAct.Act, x_DocumentActMood.Eventoccurrence, new CD<String>("PINSTRUCT", CdaHandlerConstants.CODE_SYSTEM_IHE_ACT_CODE));
	    				instructionsAct.setTemplateId(LIST.createLIST(new II(CdaHandlerConstants.ENT_TEMPLATE_CCD_INSTRUCTIONS), new II(CdaHandlerConstants.ENT_TEMPLATE_MEDICATION_INSTRUCTIONS)));
	    				instructionsAct.setText(new ED(component.getValueText()));
	    			}
	    			else if(component.getValueText().startsWith("Pre-Condition"))
	    			{
	    				Precondition condition = new Precondition();
	    				condition.setCriterion(new Criterion());
	    				condition.getCriterion().setText(new ED(component.getValueText()));
	    				retVal.getPrecondition().add(condition);
	    			}
	    			else if(retVal.getText() == null)
	    				retVal.setText(new ED(component.getValueText()));
	    		case CdaHandlerConstants.CONCEPT_ID_MEDICATION_START_DATE:
	    			if(extendedObs == null)
	    			{
	    				effectiveTimePeriod.setLow(this.m_cdaDataUtil.createTS(component.getValueDate()));
	    				effectiveTimePeriod.getLow().setDateValuePrecision(TS.DAY);
	    			}
	    			break;
	    		case CdaHandlerConstants.CONCEPT_ID_MEDICATION_STOP_DATE:
	    			if(extendedObs == null)
	    			{
	    				effectiveTimePeriod.setHigh(this.m_cdaDataUtil.createTS(component.getValueDate()));
	    				effectiveTimePeriod.getHigh().setDateValuePrecision(TS.DAY);
	    			}
	    			break;
	    		case CdaHandlerConstants.CONCEPT_ID_IMMUNIZATION_DATE:
	    			if(extendedObs == null)
	    			{
	    				effectiveTimeInstant.setValue(this.m_cdaDataUtil.createTS(component.getValueDate()));
	    				effectiveTimeInstant.getValue().setDateValuePrecision(TS.DAY);
	    			}
	    			break;
	    			
	    		case CdaHandlerConstants.CONCEPT_ID_MEDICATION_DRUG:
	    		{
					Drug valueDrug = component.getValueDrug();
					Concept concept = component.getValueCoded();
					if(valueDrug != null)
						concept = valueDrug.getConcept();
	    			retVal.setConsumable(this.createConsumable(concept, valueDrug, CdaHandlerConstants.CODE_SYSTEM_RXNORM));
	    		}
	    			break;
	    		case CdaHandlerConstants.CONCEPT_ID_IMMUNIZATION_DRUG:
	    		{
					Drug valueDrug = component.getValueDrug();
					Concept concept = component.getValueCoded();
					if(valueDrug != null)
						concept = valueDrug.getConcept();
	    			retVal.setConsumable(this.createConsumable(concept, valueDrug, CdaHandlerConstants.CODE_SYSTEM_CVX));
	    		}
	    			break;
	    		case CdaHandlerConstants.CONCEPT_ID_IMMUNIZATION_SEQUENCE:
	    			Observation seriesObservation = new Observation(x_ActMoodDocumentObservation.Eventoccurrence);
	    			seriesObservation.setTemplateId(LIST.createLIST(new II(CdaHandlerConstants.ENT_TEMPLATE_IMMUNIZATION_SERIES)));
	    			seriesObservation.setStatusCode(ActStatus.Completed);
	    			seriesObservation.setCode(new CD<String>("30973-2", CdaHandlerConstants.CODE_SYSTEM_LOINC, CdaHandlerConstants.CODE_SYSTEM_NAME_LOINC, null, "Dose Number", null));
	    			seriesObservation.setValue(new INT(component.getValueNumeric().intValue()));
	    			retVal.getEntryRelationship().add(new EntryRelationship(x_ActRelationshipEntryRelationship.SUBJ, BL.TRUE, seriesObservation));
	    			break;
	    		case CdaHandlerConstants.CONCEPT_ID_SIGN_SYMPTOM_PRESENT:
	    		    if(component.getValueCoded().getId().toString().equals(Context.getAdministrationService().getGlobalProperty(OpenmrsConstants.GLOBAL_PROPERTY_FALSE_CONCEPT)))
	    		    	retVal.setNegationInd(BL.TRUE);
	    			break;
	    		case CdaHandlerConstants.CONCEPT_ID_MEDICATION_QUANTITY: // Quantity is a numeric value and means we don't have / need units because of the form
	    			if(retVal.getDoseQuantity() == null)
	    				retVal.setDoseQuantity(new PQ(BigDecimal.valueOf(component.getValueNumeric()), null));
	    			break;
	    		case CdaHandlerConstants.CONCEPT_ID_SUPPLY:
	    		{
	    			
	    			EntryRelationship supplyRelationship = new EntryRelationship();
	    			Supply supply = new Supply();
	    			supply.setTemplateId(LIST.createLIST(new II(CdaHandlerConstants.ENT_TEMPLATE_CCD_SUPPLY_ACTIVITY), new II(CdaHandlerConstants.ENT_TEMPLATE_SUPPLY)));
	    			supplyRelationship.setClinicalStatement(supply);
	    			retVal.getEntryRelationship().add(supplyRelationship);
	    			
	    			// Obs for processing extended properties
	    			ExtendedObs supplyExtended = Context.getService(CdaImportService.class).getExtendedObs(component.getId());
	    			if(supplyExtended != null)
	    			{
	    				if(supplyExtended.getObsRepeatNumber() != null)
	    					supply.setRepeatNumber(new INT(supplyExtended.getObsRepeatNumber()));
	    		    	if(supplyExtended.getObsMood() != null)
	    		    		supply.setMoodCode(this.m_cdaMetadataUtil.getStandardizedCode(supplyExtended.getObsMood(), x_ActMoodDocumentObservation.Definition.getCodeSystem(), CS.class));
	    			}
	    			
	    		    // Identifiers
	    			retVal.setId(this.getIdentifierList(sourceObs));
	    		    
	    			// Add author data
	    			supply.getAuthor().add(this.createAuthorPointer(component));
	    			
	    			// Process children
	    			Set<Obs> supplyChildren = component.getGroupMembers(); // ODD TYPE: this.m_service.getObsGroupMembers(component);
	    			for(Obs supplyComponent : supplyChildren)
	    			{
	    				switch(supplyComponent.getConcept().getId())
	    				{
	    					case CdaHandlerConstants.CONCEPT_ID_DATE_OF_EVENT:
	    						if(supply.getPerformer().size() == 0)
	    							supply.getPerformer().add(new Performer2());
	    						supply.getPerformer().get(0).setTime(this.m_cdaDataUtil.createTS(supplyComponent.getValueDate()));
	    						supply.getPerformer().get(0).getTime().getValue().setDateValuePrecision(TS.DAY);
	    						break;
	    					case CdaHandlerConstants.CONCEPT_ID_PROVIDER_NAME:
	    						if(supply.getPerformer().size() == 0)
	    							supply.getPerformer().add(new Performer2());
	    						// Get the provider
	    						Provider provider = Context.getProviderService().getProviderByIdentifier(supplyComponent.getValueText());
	    						if(provider != null)
	    							supply.getPerformer().get(0).setAssignedEntity(this.m_cdaDataUtil.createAssignedEntity(provider));
								break;
	    					case CdaHandlerConstants.CONCEPT_ID_MEDICATION_DISPENSED:
	    						if(supply.getQuantity() == null)
	    							supply.setQuantity(new PQ(new BigDecimal(supplyComponent.getValueNumeric()), null));
	    						break;
	    					case CdaHandlerConstants.CONCEPT_ID_MEDICATION_STRENGTH:
	    						IVL<PQ> quantity = this.parseDoseQuantity(supplyComponent.getValueText());
	    						if(quantity != null && quantity.getValue() != null)
	    							supply.setQuantity(quantity.getValue());
	    						break;
	    					case CdaHandlerConstants.CONCEPT_ID_MEDICATION_TREATMENT_NUMBER:
	    						supplyRelationship.setSequenceNumber(supplyComponent.getValueNumeric().intValue());
	    						break;
	    					case CdaHandlerConstants.CONCEPT_ID_MEDICATION_DRUG:
	    					{
	    						Drug valueDrug = supplyComponent.getValueDrug();
	    						Concept concept = supplyComponent.getValueCoded();
	    						if(valueDrug != null)
	    							concept = valueDrug.getConcept();
	    						Consumable cons = this.createConsumable(concept, supplyComponent.getValueDrug(), CdaHandlerConstants.CODE_SYSTEM_RXNORM);
	    						supply.setProduct(new Product(cons.getManufacturedProduct()));
	    					}
	    						break;
    						default:
    							throw new RuntimeException("Don't understand how to represent medication supply observation");
	    							
	    				}
	    			}
	    			break;
	    		}
	    		case CdaHandlerConstants.CONCEPT_ID_MEDICATION_FREQUENCY:
	    			switch(component.getValueCoded().getId())
	    			{
	    				case CdaHandlerConstants.MEDICATION_FREQUENCY_ONCE:
	    					frequencyExpression = null;
	    					break;
	    				case CdaHandlerConstants.MEDICATION_FREQUENCY_30_MINS:
	    					frequencyExpression = new PIVL<TS>(null, new PQ(new BigDecimal("30"), "min"));
	    					break;
	    				case CdaHandlerConstants.MEDICATION_FREQUENCY_8_HOURS:
	    					frequencyExpression = new PIVL<TS>(null, new PQ(new BigDecimal("8"), "h"));
	    					break;
	    				case CdaHandlerConstants.MEDICATION_FREQUENCY_12_HOURS:
	    					frequencyExpression = new PIVL<TS>(null, new PQ(new BigDecimal("12"), "h"));
	    					break;
	    				case CdaHandlerConstants.MEDICATION_FREQUENCY_24_HOURS:
	    					frequencyExpression = new PIVL<TS>(null, new PQ(new BigDecimal("24"), "h"));
	    					break;
	    				case CdaHandlerConstants.MEDICATION_FREQUENCY_36_HOURS:
	    					frequencyExpression = new PIVL<TS>(null, new PQ(new BigDecimal("36"), "h"));
	    					break;
	    				case CdaHandlerConstants.MEDICATION_FREQUENCY_48_HOURS:
	    					frequencyExpression = new PIVL<TS>(null, new PQ(new BigDecimal("48"), "h"));
	    					break;
	    				case CdaHandlerConstants.MEDICATION_FREQUENCY_72_HOURS:
	    					frequencyExpression = new PIVL<TS>(null, new PQ(new BigDecimal("72"), "h"));
	    					break;
	    				case CdaHandlerConstants.MEDICATION_FREQUENCY_ONCE_DAILY:
	    					frequencyExpression = new PIVL<TS>(null, new PQ(new BigDecimal("1"), "d"));
	    					break;
	    				case CdaHandlerConstants.MEDICATION_FREQUENCY_AT_BEDTIME:
	    					frequencyExpression = new EIVL<TS>(DomainTimingEvent.HourOfSleep, null);
	    					break;
	    				case CdaHandlerConstants.MEDICATION_FREQUENCY_ONCE_DAILY_EVENING:
	    					frequencyExpression = new EIVL<TS>(DomainTimingEvent.BetweenDinnerAndSleep, null);
	    					break;
	    				case CdaHandlerConstants.MEDICATION_FREQUENCY_ONCE_DAILY_MORNING:
	    					frequencyExpression = new EIVL<TS>(DomainTimingEvent.BeforeBreakfast, null);
	    					break;
	    				case CdaHandlerConstants.MEDICATION_FREQUENCY_TWICE_DAILY_AFTER_MEALS:
	    					frequencyExpression = new EIVL<TS>(DomainTimingEvent.AfterMeal, new IVL<PQ>(new PQ(new BigDecimal("12"), "h")));
	    					break;
	    				case CdaHandlerConstants.MEDICATION_FREQUENCY_THRICE_DAILY_AFTER_MEALS:
	    					frequencyExpression = new EIVL<TS>(DomainTimingEvent.AfterMeal, new IVL<PQ>(new PQ(new BigDecimal("8"), "h")));
	    					break;
	    				case CdaHandlerConstants.MEDICATION_FREQUENCY_FOUR_TIMES_DAILY_AFTER_MEALS:
	    					frequencyExpression = new EIVL<TS>(DomainTimingEvent.AfterMeal, new IVL<PQ>(new PQ(new BigDecimal("6"), "h")));
	    					break;
	    				case CdaHandlerConstants.MEDICATION_FREQUENCY_TWICE_DAILY_BEFORE_MEALS:
	    					frequencyExpression = new EIVL<TS>(DomainTimingEvent.BeforeMeal, new IVL<PQ>(new PQ(new BigDecimal("12"), "h")));
	    					break;
	    				case CdaHandlerConstants.MEDICATION_FREQUENCY_THRICE_DAILY_BEFORE_MEALS:
	    					frequencyExpression = new EIVL<TS>(DomainTimingEvent.BeforeMeal, new IVL<PQ>(new PQ(new BigDecimal("8"), "h")));
	    					break;
	    				case CdaHandlerConstants.MEDICATION_FREQUENCY_FOUR_TIMES_DAILY_BEFORE_MEALS:
	    					frequencyExpression = new EIVL<TS>(DomainTimingEvent.BeforeMeal, new IVL<PQ>(new PQ(new BigDecimal("6"), "h")));
	    					break;
    					default:
    						if(component.getValueCoded().getId() > CdaHandlerConstants.MEDICATION_FREQUENCY_30_MINS && component.getValueCoded().getId() < CdaHandlerConstants.MEDICATION_FREQUENCY_8_HOURS)
    							frequencyExpression = new PIVL<TS>(null, new PQ(new BigDecimal(component.getValueCoded().getId() - CdaHandlerConstants.MEDICATION_FREQUENCY_30_MINS), "h"));
    						else
    						{
    							EIVL<TS> other = new EIVL<TS>();
    							CV<String> domainTimingEvent = this.m_cdaMetadataUtil.getStandardizedCode(component.getValueCoded(), DomainTimingEvent.AfterBreakfast.getCodeSystem(), CV.class);
    							other.setEvent(new CS<DomainTimingEvent>(FormatterUtil.fromWireFormat(domainTimingEvent.getCode(), DomainTimingEvent.class)));
    							frequencyExpression = other;
    						}
    						break;
	    			}
    				break;
	    		case CdaHandlerConstants.CONCEPT_ID_MEDICATION_STRENGTH: // use strength
	    			// Strength is measured as a IVL<PQ> which is serialized to a string when stored in OpenMRS
	    			// This string is in the following format:
	    			// 0[.##] XX - Exact dose
	    			// {0[.##] XX .. } - At least X dose
	    			// {0[.##] XX .. 0.[##] YY} - Between X and Y dose
	    			// { .. 0.[##] YY} - AT most Y dose
	    			retVal.setDoseQuantity(this.parseDoseQuantity(component.getValueText()));
	    			break;
	    		case CdaHandlerConstants.CONCEPT_ID_MEDICATION_HISTORY: // A sub-observation
	    		{
	    			SubstanceAdministration statement = this.createSubstanceAdministration(templateIds, component);
	    			EntryRelationship entryRelation = new EntryRelationship(x_ActRelationshipEntryRelationship.HasComponent, BL.TRUE);
	    			entryRelation.setSequenceNumber(++cSequence);
	    			entryRelation.setClinicalStatement(statement);
	    			retVal.getEntryRelationship().add(entryRelation);
	    			
	    			break;
	    		}
	    		case CdaHandlerConstants.CONCEPT_ID_MEDICATION_FORM:
	    			retVal.setAdministrationUnitCode(this.m_cdaMetadataUtil.getStandardizedCode(component.getValueCoded(), CdaHandlerConstants.CODE_SYSTEM_SNOMED, CE.class));
	    			break;
	    		case CdaHandlerConstants.CONCEPT_ID_PROCEDURE:
					retVal.setCode(this.m_cdaMetadataUtil.getStandardizedCode(component.getValueCoded(), null, CD.class));
					
					// Treatment is unknown?
					if(retVal.getCode().semanticEquals(s_drugTreatmentUnknownCode) == BL.TRUE)
						return null;
					break;
	    		default:
	    			// The codes that need to be determined at runtime
                    if(component.getConcept().getUuid().equals(CdaHandlerConstants.RMIM_CONCEPT_UUID_ROUTE_OF_ADM))
                    	retVal.setRouteCode(this.m_cdaMetadataUtil.getStandardizedCode(component.getValueCoded(), CdaHandlerConstants.CODE_SYSTEM_ROUTE_OF_ADMINISTRATION, CE.class));
                    else if(component.getConcept().getUuid().equals(CdaHandlerConstants.RMIM_CONCEPT_UUID_REASON))
                    {
                    	EntryRelationship reasonRelation = new EntryRelationship(x_ActRelationshipEntryRelationship.HasReason, BL.TRUE);
                    	Act reasonAct = new Act(x_ActClassDocumentEntryAct.Act, x_DocumentActMood.Eventoccurrence);
                    	reasonAct.setTemplateId(LIST.createLIST(new II(CdaHandlerConstants.ENT_TEMPLATE_INTERNAL_REFERENCE)));
                    	reasonAct.setId(SET.createSET(this.m_cdaDataUtil.parseIIFromString(component.getValueText())));
                    	reasonRelation.setClinicalStatement(reasonAct);
                    	retVal.getEntryRelationship().add(reasonRelation);
                    }
                    else
                    	throw new RuntimeException("Don't understand how to represent medication component observation");
	    	}

	    }
	    
    	// We have medication history  
    	if(sourceObs.getConcept().getId().equals(CdaHandlerConstants.CONCEPT_ID_MEDICATION_HISTORY))
    	{
    		retVal.getEffectiveTime().add(effectiveTimePeriod);
    		if(frequencyExpression != null)
    		{
    			((SXCM<TS>)frequencyExpression).setOperator(SetOperator.Intersect); 
    			retVal.getEffectiveTime().add(frequencyExpression);
    		}
    	}
    	else
    		retVal.getEffectiveTime().add(effectiveTimeInstant);

    	if(sourceObs.getComment() != null)
    		retVal.setText(new ED(sourceObs.getComment()));
	    return retVal;
    }

	/**
	 * Unknown drug treatment
	 */
	protected SubstanceAdministration createNoSubstanceAdministration(List<String> templateIds) {
		SubstanceAdministration retVal = new SubstanceAdministration(x_DocumentSubstanceMood.Eventoccurrence);
		retVal.setTemplateId(this.getTemplateIdList(templateIds));
		
		retVal.getEffectiveTime().add(new IVL<TS>(new TS(), TS.now()));
		((IVL<TS>)retVal.getEffectiveTime().get(0)).getLow().setNullFlavor(NullFlavor.Unknown);
		
		retVal.setCode(s_drugTreatmentUnknownCode);
		
		retVal.setStatusCode(ActStatus.Completed);
		
		retVal.setId(SET.createSET(new II(UUID.randomUUID())));
//		retVal.getId().get(0).setNullFlavor(NullFlavor.NotApplicable);
		
		retVal.setDoseQuantity(new PQ());
		retVal.getDoseQuantity().setNullFlavor(NullFlavor.NotApplicable);
		
		retVal.setAdministrationUnitCode(new CE<String>());
		retVal.getAdministrationUnitCode().setNullFlavor(NullFlavor.NotApplicable);
		
		retVal.setConsumable(new Consumable());
		retVal.getConsumable().setManufacturedProduct(new ManufacturedProduct());
		retVal.getConsumable().getManufacturedProduct().setTemplateId(this.getTemplateIdList(Arrays.asList(CdaHandlerConstants.ENT_TEMPLATE_PRODUCT, CdaHandlerConstants.ENT_TEMPLATE_CCD_MEDICATION_PRODUCT)));
		retVal.getConsumable().getManufacturedProduct().setManufacturedDrugOrOtherMaterial(new Material());
		retVal.getConsumable().getManufacturedProduct().getManufacturedDrugOrOtherMaterialIfManufacturedMaterial().setCode(new CE<String>());
		retVal.getConsumable().getManufacturedProduct().getManufacturedDrugOrOtherMaterialIfManufacturedMaterial().getCode().setNullFlavor(NullFlavor.NotApplicable);
		retVal.getConsumable().getManufacturedProduct().getManufacturedDrugOrOtherMaterialIfManufacturedMaterial().getCode().setOriginalText(new ED("Not Applicable"));
		
		retVal.getAuthor().add(this.m_cdaDataUtil.getOpenSHRInstanceAuthor());
		return retVal;
    }


}
