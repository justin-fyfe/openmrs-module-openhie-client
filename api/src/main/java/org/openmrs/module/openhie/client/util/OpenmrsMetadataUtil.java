package org.openmrs.module.openhie.client.util;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.marc.everest.datatypes.generic.CD;
import org.marc.everest.datatypes.generic.CE;
import org.marc.everest.datatypes.generic.CS;
import org.marc.everest.interfaces.IEnumeratedVocabulary;
import org.openmrs.Concept;
import org.openmrs.ConceptClass;
import org.openmrs.EncounterType;
import org.openmrs.OrderType;
import org.openmrs.PersonAttributeType;
import org.openmrs.RelationshipType;
import org.openmrs.api.context.Context;
import org.openmrs.module.openhie.client.CdaHandlerConstants;
import org.openmrs.module.openhie.client.configuration.CdaHandlerConfiguration;
import org.openmrs.module.openhie.client.exception.HealthInformationExchangeException;
import org.openmrs.util.OpenmrsConstants;

/**
 * Utilities for OpenMRS MetaData creation/lookup
 * @author Justin Fyfe
 *
 */
public class OpenmrsMetadataUtil {
	
	/**
	 * Get the singleton instance
	 */
	public static OpenmrsMetadataUtil getInstance()
	{
		if(s_instance == null)
		{
			synchronized (s_lockObject) {
				if(s_instance == null) // Another thread might have created while we were waiting for a lock
					s_instance = new OpenmrsMetadataUtil();
			}
		}
		return s_instance;
	}

	// Log
	protected final Log log = LogFactory.getLog(this.getClass());
	// singleton instance
	private static OpenmrsMetadataUtil s_instance;
	
	private static Object s_lockObject = new Object();
	
	// Auto create encounter roles
	private final CdaHandlerConfiguration m_configuration = CdaHandlerConfiguration.getInstance();
	
	/**
	 * Private ctor
	 */
	protected OpenmrsMetadataUtil()
	{
		
	}

	/**
	 * Creates a person attribute 
	 */
	private PersonAttributeType createPersonAttributeType(String attributeName, String dataType, String description) {
		if(!this.m_configuration.getAutoCreateMetaData())
			throw new IllegalStateException("Cannot create attribute type");
		PersonAttributeType res = new PersonAttributeType();
		res.setName(attributeName);
		res.setFormat(dataType);
		res.setDescription(description);
		res.setForeignKey(0);
		res = Context.getPersonService().savePersonAttributeType(res);
		return res;
    }

	/**
	 * Get the encounter type
	 * @param code The code representing the type (class) of section
	 * @return The encounter type
	 * @throws DocumentImportException 
	 */
	public EncounterType getOrCreateEncounterType(CE<String> code) throws HealthInformationExchangeException {

		// Get the codekey and code display
		String codeKey = DatatypeProcessorUtil.getInstance().formatCodeValue(code);
		String display = code.getDisplayName();
		if(display == null || display.isEmpty())
			display = code.getCode();
		
		EncounterType encounterType = null;
		for(EncounterType type : Context.getEncounterService().getAllEncounterTypes())
			if(type.getDescription().equals(codeKey))
				encounterType = type;
				
		if(encounterType == null && this.m_configuration.getAutoCreateMetaData()) {
			encounterType = new EncounterType();
			encounterType.setName(display);
			encounterType.setDescription(codeKey);
			encounterType = Context.getEncounterService().saveEncounterType(encounterType);
		} 
		else if(encounterType == null && !this.m_configuration.getAutoCreateMetaData())
			throw new HealthInformationExchangeException(String.format("Encounter type %s is unknown", code.getCode()));
		
		return encounterType;
	}
	
	
	/**
	 * Get the person marital status attribute type
	 * @throws DocumentImportException 
	 */
	public PersonAttributeType getOrCreatePersonMaritalStatusAttribute() throws HealthInformationExchangeException
	{
		
		PersonAttributeType res = this.getPersonAttributeType(CdaHandlerConstants.ATTRIBUTE_NAME_CIVIL_STATUS);
		if(res == null)
		{
			res = this.createPersonAttributeType(
				CdaHandlerConstants.ATTRIBUTE_NAME_CIVIL_STATUS, 
				"org.openmrs.Concept",
				"Civil Status");
			Concept civilStatusConcept = Context.getConceptService().getConcept(OpenmrsConstants.CIVIL_STATUS_CONCEPT_ID);
			res.setForeignKey(civilStatusConcept.getId());
			res = Context.getPersonService().savePersonAttributeType(res);
		}
		return res;
		
	}

	/**
	 * Get the telecommunications address provider type
	 * @return The attribute type representing provider's telecommunications address
	 * @throws DocumentImportException 
	 */
	public PersonAttributeType getOrCreatePersonOrganizationAttribute() throws HealthInformationExchangeException
	{
		PersonAttributeType res = this.getPersonAttributeType(CdaHandlerConstants.ATTRIBUTE_NAME_ORGANIZATION);
		if(res == null)
			res = this.createPersonAttributeType(
				CdaHandlerConstants.ATTRIBUTE_NAME_ORGANIZATION, 
				"org.openmrs.Location",
				"Related Organization");
		return res;
	}

	/**
	 * Get the telecommunications address provider type
	 * @return The attribute type representing provider's telecommunications address
	 * @throws DocumentImportException 
	 */
	public PersonAttributeType getOrCreatePersonTelecomAttribute() throws HealthInformationExchangeException
	{
		PersonAttributeType res = this.getPersonAttributeType(CdaHandlerConstants.ATTRIBUTE_NAME_TELECOM);
		if(res == null)
			res = this.createPersonAttributeType(
				CdaHandlerConstants.ATTRIBUTE_NAME_TELECOM, 
				"java.lang.String",
				"Telecom Address");
		return res;
	}

	/**
	 * Get or create relationship type
	 * @throws DocumentImportException 
	 */
	public RelationshipType getOrCreateRelationshipType(CE<String> relationship) throws HealthInformationExchangeException {
		
		// TODO: Find a better way of mapping this code as there are a few code ssytems that have similar codes
		String relationshipTypeName = DatatypeProcessorUtil.getInstance().formatCodeValue(relationship);
		RelationshipType visitType = null;
		for(RelationshipType type : Context.getPersonService().getAllRelationshipTypes())
			if(type.getDescription() != null && type.getDescription().equals(relationshipTypeName))
				visitType = type;
		
		if(visitType == null && this.m_configuration.getAutoCreateMetaData())
		{
			visitType = new RelationshipType();
			visitType.setName(relationshipTypeName);
			visitType.setDescription(relationshipTypeName);
			visitType.setaIsToB(relationship.getCode());
			visitType.setbIsToA(relationship.getCode());
			visitType = Context.getPersonService().saveRelationshipType(visitType);
		}
		else if(visitType == null && !this.m_configuration.getAutoCreateMetaData())
			throw new HealthInformationExchangeException(String.format("Cannot find specified relationship type %s", relationship));
		return visitType;
    }
		/**
	 * Get the person attribute type
	 */
	private PersonAttributeType getPersonAttributeType(String name) {
	    return Context.getPersonService().getPersonAttributeTypeByName(name);
    }

	/**
	 * Get or create the order type for procedures
	 * @throws DocumentImportException 
	 */
	public OrderType getOrCreateProcedureOrderType() {
		OrderType res = Context.getOrderService().getOrderTypeByUuid(CdaHandlerConstants.UUID_ORDER_TYPE_PROCEDURE);
		if(res == null && this.m_configuration.getAutoCreateMetaData())
		{
			res = new OrderType("Procedure Order", "Procedure Order");
			res.setUuid(CdaHandlerConstants.UUID_ORDER_TYPE_PROCEDURE);
			res = Context.getOrderService().saveOrderType(res);
		}
		return res;
    }

	/**
	 * Get or create observation order type
	 */
	public OrderType getOrCreateObservationOrderType() {
		OrderType res = Context.getOrderService().getOrderTypeByUuid(CdaHandlerConstants.UUID_ORDER_TYPE_OBSERVATION);
		if(res == null && this.m_configuration.getAutoCreateMetaData())
		{
			res = new OrderType("Observation Order", "Observation Order");
			res.setUuid(CdaHandlerConstants.UUID_ORDER_TYPE_OBSERVATION);
			res = Context.getOrderService().saveOrderType(res);
		}
		return res;
    }

	
}
