package org.openmrs.module.openhie.client.util;

import java.util.ArrayDeque;
import java.util.Queue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.marc.everest.datatypes.ED;
import org.marc.everest.datatypes.II;
import org.marc.everest.datatypes.NullFlavor;
import org.marc.everest.datatypes.generic.CD;
import org.marc.everest.datatypes.generic.CE;
import org.marc.everest.datatypes.generic.CS;
import org.marc.everest.datatypes.generic.CV;
import org.marc.everest.datatypes.generic.SET;
import org.openmrs.Concept;
import org.openmrs.ConceptMap;
import org.openmrs.Location;
import org.openmrs.Person;
import org.openmrs.PersonAttribute;
import org.openmrs.api.context.Context;
import org.openmrs.module.openhie.client.CdaHandlerConstants;
import org.openmrs.module.openhie.client.configuration.CdaHandlerConfiguration;

/**
 * The On-Demand document metadata util
 */
public final class CdaMetadataUtil {
	
	protected Log log = LogFactory.getLog(getClass());

	
	// Singleton stuff
	private static final Object s_lockObject = new Object();
	private static CdaMetadataUtil s_instance;
	
	// Get the ODD service
	private final CdaHandlerConfiguration m_cdaConfiguration = CdaHandlerConfiguration.getInstance();
	
	
	/**
	 * Private ctor
	 */
	private CdaMetadataUtil()
	{
		
	}
	
	/**
	 * Get instance of the ODD meta-data utility
	 */
	public static CdaMetadataUtil getInstance() {
		if(s_instance == null)
			synchronized (s_lockObject) {
				if(s_instance == null)
					s_instance = new CdaMetadataUtil();
            }
		return s_instance;
	}
	
	/**
	 * Get the provider attribute
	 */
	public PersonAttribute getProviderAttribute(Person pvdr, String attributeName) {
		for(PersonAttribute att : pvdr.getActiveAttributes())
			if(att.getAttributeType().getName().equals(attributeName))
				return att;
		return null;
    }

	public <T extends CS> T getStandardizedCode(Concept value, String targetCodeSystem, Class<T> clazz) {
	    
		try {
	        T retVal = null;
	        
	        if(value == null)
	        {
	        	retVal = clazz.newInstance();
	        	retVal.setNullFlavor(NullFlavor.NoInformation);
	        	return retVal;
	        }
	        else
	        {
	        	retVal = clazz.newInstance();
	        	retVal.setNullFlavor(NullFlavor.Other);
	        }
	        	
	        // First, we need to find the reference term that represents the most applicable
	        /*Queue<ConceptReferenceTerm> preferredCodes = new ArrayDeque<ConceptReferenceTerm>(),
	        		equivalentCodes = new ArrayDeque<ConceptReferenceTerm>(),
    				narrowerCodes = new ArrayDeque<ConceptReferenceTerm>();
	        
	        // Mappings
	        String targetCodeSystemName = this.m_conceptUtil.mapOidToConceptSourceName(targetCodeSystem);
	        for(ConceptMap mapping : value.getConceptMappings())
	        {
	        	if(mapping.getConceptMapType().getName().equalsIgnoreCase("SAME-AS"))
	        	{
	        		ConceptReferenceTerm candidateTerm = mapping.getConceptReferenceTerm();
	        		if(targetCodeSystem == null ||
	        				targetCodeSystemName.equals(candidateTerm.getConceptSource().getName()) ||
	        				targetCodeSystem.equals(candidateTerm.getConceptSource().getHl7Code()))
	        			preferredCodes.add(candidateTerm);
	        		else
	        			equivalentCodes.add(candidateTerm);
	        	}
	        	else 
	        	{
	        		ConceptReferenceTerm candidateTerm = mapping.getConceptReferenceTerm();
	        		if(targetCodeSystem == null ||
	        				targetCodeSystemName.equals(candidateTerm.getConceptSource().getName()) ||
	        				targetCodeSystem.equals(candidateTerm.getConceptSource().getHl7Code()))
	        			narrowerCodes.add(candidateTerm);
	        	}
	        }
	        
	        // No SAME-AS but maybe a narrower code?
	        if(preferredCodes.size() == 0)
	        {
	        	if(narrowerCodes.size() > 0)
	        		preferredCodes.add(narrowerCodes.poll());
	        	else
	        		log.warn(String.format("Could not find code for %s in %s", value, targetCodeSystem));
	        }
	        // Now that we have a term, let's see if we can select a preferred term
	        ConceptReferenceTerm preferredTerm = preferredCodes.poll();
	        if(preferredTerm == null) // No preferred terms!
	        {
	        	retVal = clazz.newInstance();
	        	retVal.setNullFlavor(NullFlavor.Other);
	        	if(retVal instanceof CV)
	        		((CV<?>)retVal).setCodeSystem(targetCodeSystemName);
	        }
	        else
	        	retVal = this.createCode(preferredTerm, clazz);
        	*/
	        if(retVal instanceof CV)
	        {
		        ((CV<?>)retVal).setCodeSystem(targetCodeSystem);
	        	if(value.getPreferredName(Context.getLocale()) != null)
	    			((CV<?>)retVal).setOriginalText(new ED(value.getPreferredName(Context.getLocale()).getName(), null));
	        	else if(value.getName() != null)
	        		((CV<?>)retVal).setOriginalText(new ED(value.getName().getName(), null));
	        	else if(value.getDescription() != null)
	        		((CV<?>)retVal).setOriginalText(new ED(value.getDescription().getDescription(), null));
	        }

    		
	        // Are there other preferred terms
        	if(retVal instanceof CE)
        	{
        		SET<CD<?>> translations = new SET<CD<?>>();

        		translations.add(this.createCode(value, CD.class));
        		
		        // Add translations if any
		        if(!translations.isEmpty())
		        	((CE) retVal).setTranslation(translations);
        	}
        	
        	return retVal;
        }
        catch (Exception e) {
        	log.error("Error creating code", e);
        	return null;
        }
	    
    }

	/**
	 * Create the actual code data from the referenceTerm
	 */
	
	private <T extends CS> T createCode(Concept term, Class<T> clazz) {
		try
		{
			T retVal = clazz.newInstance();
	    	retVal.setCode(term.getId().toString());
	    	if(retVal instanceof CV)
	    	{
	    		((CV<?>)retVal).setCodeSystem(CdaHandlerConstants.CODE_SYSTEM_CIEL);
	    	}
	    	
	    	return retVal;
		}
		catch(Exception e)
		{
			log.error("Error creating code", e);
			return null;
		}
    }
    
	
}
