package org.openmrs.module.openhie.client.cda.document.impl;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.marc.everest.datatypes.BL;
import org.marc.everest.datatypes.II;
import org.marc.everest.datatypes.NullFlavor;
import org.marc.everest.datatypes.TS;
import org.marc.everest.datatypes.generic.LIST;
import org.marc.everest.datatypes.generic.CE;
import org.marc.everest.datatypes.generic.CS;
import org.marc.everest.datatypes.generic.SET;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.AssignedAuthor;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.AssignedCustodian;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Author;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.ClinicalDocument;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Component2;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Component3;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Custodian;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.DocumentationOf;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Performer1;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Section;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.ServiceEvent;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.StructuredBody;
import org.marc.everest.rmim.uv.cdar2.vocabulary.ActRelationshipHasComponent;
import org.marc.everest.rmim.uv.cdar2.vocabulary.BindingRealm;
import org.marc.everest.rmim.uv.cdar2.vocabulary.ContextControl;
import org.marc.everest.rmim.uv.cdar2.vocabulary.ParticipationFunction;
import org.marc.everest.rmim.uv.cdar2.vocabulary.x_BasicConfidentialityKind;
import org.marc.everest.rmim.uv.cdar2.vocabulary.x_ServiceEventPerformer;
import org.openmrs.Encounter;
import org.openmrs.EncounterRole;
import org.openmrs.Patient;
import org.openmrs.Person;
import org.openmrs.Provider;
import org.openmrs.Relationship;
import org.openmrs.Visit;
import org.openmrs.api.context.Context;
import org.openmrs.module.openhie.client.cda.document.DocumentBuilder;
import org.openmrs.module.openhie.client.util.CdaDataUtil;
import org.openmrs.module.shr.cdahandler.CdaHandlerConstants;

/**
 * A generic clinical document builder which does not assign any template
 * information
 * @author JustinFyfe
 *
 */
public class DocumentBuilderImpl implements DocumentBuilder {

	// Record target
	private Patient m_recordTarget;
	// Encounter
	private Encounter m_encounter;
	// Log
	protected final Log log = LogFactory.getLog(this.getClass());
	// CDA data utility 
	private CdaDataUtil m_cdaDataUtil = CdaDataUtil.getInstance();
	
	/**
	 * Get the document type code
	 * @see org.openmrs.module.shr.odd.generator.DocumentGenerator#getDocumentTypeCode()
	 */
    public String getTypeCode() {
		return "34133-9";
    }

	/**
	 * Get the document format code
	 * @see org.openmrs.module.shr.odd.generator.DocumentGenerator#getFormatCode()
	 */
	public String getFormatCode() {
		return "2.16.840.1.113883.10.20.1";
	}
	
	/**
	 * Set the Encounter this document builder will  be representing
	 */
	public void setEncounterEvent(Encounter enc)
	{
		this.m_encounter = enc;
	}
	
	/**
	 * Get the encounter event
	 */
	public Encounter getEncounterEvent()
	{
		return this.m_encounter;
	}
	
	/**
	 * Sets the record target
	 */
	public void setRecordTarget(Patient recordTarget) {
		this.m_recordTarget = recordTarget;
	}

	/**
	 * Gets the currently assigned record target
	 */
	public Patient getRecordTarget() {
		return this.m_recordTarget;
	}


	/**
	 * Generate the CDA
	 */
	public ClinicalDocument generate(Section... sections) {
		try
		{
			ClinicalDocument retVal = new ClinicalDocument();
			retVal.setTypeId(new II("2.16.840.1.113883.1.3", "POCD_HD000040"));
			retVal.setRealmCode(SET.createSET(new CS<BindingRealm>(BindingRealm.UniversalRealmOrContextUsedInEveryInstance)));
			retVal.setTemplateId(LIST.createLIST(new II(CdaHandlerConstants.DOC_TEMPLATE_MEDICAL_DOCUMENTS)));
			// Identifier is the SHR root of the odd document ODD ID + Current Time (making the UUID of the ODD)
			TS idDate = TS.now();
			idDate.setDateValuePrecision(TS.SECONDNOTIMEZONE);
			
			// Set core properties
			retVal.setId(UUID.randomUUID());
			retVal.setEffectiveTime(TS.now());
			
			// Set to Normal, anything above a normal will not be included in the extract
			retVal.setConfidentialityCode(new CE<x_BasicConfidentialityKind>(x_BasicConfidentialityKind.Normal));
			retVal.setLanguageCode(Context.getLocale().toLanguageTag()); // CONF-5
			
			// Custodian
			Custodian custodian = new Custodian();
			custodian.setAssignedCustodian(new AssignedCustodian());
			custodian.getAssignedCustodian().setRepresentedCustodianOrganization(this.m_cdaDataUtil.getCustodianOrganization());
			retVal.setCustodian(custodian);

			// Create documentation of
			// TODO: Do we only need one of these for all events that occur in the CDA or one for each?
			ServiceEvent event = new ServiceEvent(new CS<String>("PCPR")); // CCD CONF-3 & CONF-2
			Date earliestRecord = new Date(),
					lastRecord = new Date(0);
			
			// Assign data form the encounter
			if(this.m_encounter != null)
			{
				Visit visit = this.m_encounter.getVisit();
				if(visit != null)
				{
					earliestRecord = visit.getStartDatetime();
					lastRecord = visit.getStopDatetime();
				}
				else
				{
					lastRecord = earliestRecord = this.m_encounter.getEncounterDatetime();
				}
					
				// Now add participants
				for(Entry<EncounterRole, Set<Provider>> encounterProvider : this.m_encounter.getProvidersByRoles().entrySet())
				{
					
					if(encounterProvider.getKey().getName().equals("AUT"))
						for(Provider pvdr : encounterProvider.getValue())
						{
							Author aut = new Author(ContextControl.OverridingPropagating);
							aut.setTime(new TS());
							aut.getTime().setNullFlavor(NullFlavor.NoInformation);
							aut.setAssignedAuthor(this.m_cdaDataUtil.createAuthorPerson(pvdr));
							retVal.getAuthor().add(aut);
						}
					else if(encounterProvider.getKey().getName().equals("LA")) // There technically are no "legal" attesters to the document here as it is an auto-generated document
						;
					else
						for(Provider pvdr : encounterProvider.getValue())
						{
							Performer1 performer = new Performer1(x_ServiceEventPerformer.PRF, this.m_cdaDataUtil.createAssignedEntity(pvdr));
							performer.setFunctionCode((CE<ParticipationFunction>)this.m_cdaDataUtil.parseCodeFromString(encounterProvider.getKey().getDescription(), CE.class));
							event.getPerformer().add(performer);
						}
				}
		
			}
			else
			{
				earliestRecord = this.m_recordTarget.getDateCreated();
				lastRecord = this.m_recordTarget.getDateChanged();
				
				Person person= Context.getAuthenticatedUser().getPerson();
				Collection<Provider> provider = Context.getProviderService().getProvidersByPerson(person);
				if(provider.size() > 0)
				{
					Author aut = new Author(ContextControl.OverridingPropagating);
					aut.setTime(new TS());
					aut.getTime().setNullFlavor(NullFlavor.NoInformation);
					aut.setAssignedAuthor(this.m_cdaDataUtil.createAuthorPerson(provider.iterator().next()));
					retVal.getAuthor().add(aut);
				}
				
			}
			
			// Set the effective time of records
			Calendar earliestCal = Calendar.getInstance(), 
					latestCal = Calendar.getInstance();
			earliestCal.setTime(earliestRecord);
			
			if(lastRecord != null)
				latestCal.setTime(lastRecord);
			event.setEffectiveTime(new TS(earliestCal), new TS(latestCal)); // CCD CONF-4
			
			// Documentation of
			retVal.getDocumentationOf().add(new DocumentationOf(event));
			
			// Record target
			retVal.getRecordTarget().add(this.m_cdaDataUtil.createRecordTarget(this.m_recordTarget));
			
			// NOK (those within the time covered by this document)
			for(Relationship relatedPerson : Context.getPersonService().getRelationshipsByPerson(this.m_recordTarget))
			{
				// Periodic hull
				retVal.getParticipant().add(this.m_cdaDataUtil.createRelatedPerson(relatedPerson, this.m_recordTarget));
			}
			
			retVal.setComponent(new Component2(ActRelationshipHasComponent.HasComponent, BL.TRUE, new StructuredBody()));
			for(Section sct : sections)
			{
				if(sct == null) continue;
				
				retVal.getComponent().getBodyChoiceIfStructuredBody().getComponent().add(
						new Component3(ActRelationshipHasComponent.HasComponent, BL.TRUE, sct)
						);
				
				// Minify authors
				for(Author aut : sct.getAuthor())
				{
					if(!this.m_cdaDataUtil.containsAuthor(retVal.getAuthor(), aut))
						retVal.getAuthor().add(aut);

				}
				sct.getAuthor().clear();
			}
			
			return retVal;
		}
		catch(Exception e)
		{
			
			log.error(e);
			log.error(ExceptionUtils.getStackTrace(e));
			throw new RuntimeException(e);
		}
	}

}
