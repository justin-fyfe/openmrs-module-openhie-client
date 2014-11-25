package org.openmrs.module.openhie.client.configuration;

import java.util.HashMap;
import java.util.Map;

import org.marc.everest.formatters.FormatterUtil;
import org.openmrs.GlobalProperty;
import org.openmrs.api.context.Context;

/**
 * Health information exchange configuration
 * @author Justin
 *
 */
public class HealthInformationExchangeConfiguration {

	// Lock object
	private static final Object s_lockObject = new Object();
	// Singleton
	private static HealthInformationExchangeConfiguration s_instance;
	
	private static final String PROP_NAME_PDQ_EP = "openhie-client.endpoint.pdq";
	private static final String PROP_NAME_PDQ_EP_PORT = "openhie-client.endpoint.pdq.port";
	private static final String PROP_NAME_PIX_EP = "openhie-client.endpoint.pix";
	private static final String PROP_NAME_PIX_EP_PORT = "openhie-client.endpoint.pix.port";
	private static final String PROP_NAME_XDS_REG_EP = "openhie-client.endpoint.xds.registry";
	private static final String PROP_NAME_XDS_REP_EP = "openhie-client.endpoint.xds.repository";

    
	// Automatically create concepts
    public static final String PROP_AUTOCREATE_CONCEPTS = "openhie-client.autocreate.concepts";
	// Automatically create locations
    public static final String PROP_AUTOCREATE_LOCATIONS = "openhie-client.autocreate.locations";
	// Property name controlling auto-creation of encounter roles
    public static final String PROP_AUTOCREATE_METADATA = "openhie-client.autocreate.metaData";
	// Property name controlling the auto-creation of patient id types
    public static final String PROP_AUTOCREATE_PATIENTIDTYPE = "openhie-client.autocreate.patients.idtype";
	// Property name controlling the auto-creation of patients
    public static final String PROP_AUTOCREATE_PATIENTS = "openhie-client.autocreate.patients";
	// Property name controlling the auto-creation of persons
    public static final String PROP_AUTOCREATE_PERSONS = "openhie-client.autocreate.persons";
	// Property name controlling the auto-creation of users
    public static final String PROP_AUTOCREATE_USERS = "openhie-client.autocreate.users";
	// Property name controlling auto-creation of entities
    public static final String PROP_AUTOCREATE_PROVIDERS = "openhie-client.autocreate.providers";
    // Property controlling the format of complex identifiers
    public static final String PROP_ID_FORMAT = "openhie-client.cda.id.format";
    // Strict validation
    public static final String PROP_VALIDATE_STRUCTURE = "openhie-client.cda.validation.cda";
    // Strict validation
    public static final String PROP_VALIDATE_CONCEPT_STRUCTURE = "openhie-client.cda.validation.conceptStructure";
    // The root of EPIDs
    public static final String PROP_EPID_ROOT = "openhie-client.id.epidRoot";
    // The root of ECIDs
	public static final String PROP_ECID_ROOT = "openhie-client.id.ecidRoot";
    // The root of Objects in the SHR
	public static final String PROP_SHR_ROOT = "openhie-client.id.localOid";
	
    // Update existing
    public static final String PROP_UPDATE_EXISTING = "openhie-client.data.updateExisting";
    
    private final Boolean m_defaultAutoCreateProviders = true;
    private final Boolean m_defaultAutoCreateLocations = true;
    private final Boolean m_defaultAutoCreateConcepts = true;
    private final Boolean m_defaultAutoCreateMetadata = true;
    private final Boolean m_defaultAutoCreatePatients = false;
    private final Boolean m_defaultAutoCreatePatientIdType = false;
    private final Boolean m_defaultAutoCreatePersons = true;
    private final Boolean m_defaultValidateInstances = true;
    private final Boolean m_defaultUpdateExisting = true;
    private final String m_defaultEpidRoot = "";
    private final String m_defaultEcidRoot = "";
    private final String m_defaultShrRoot = "1.2.3.4.5";
    private final Boolean m_defaultAutoCreateUsers = true;
    
    private String m_idFormat = "%2$s^^^&%1$s&ISO";
    
    private Map<String, Object> m_cachedProperties = new HashMap<String, Object>();

    /**
	 * Get the shr-cdahandler.autocreate.concepts value
	 */
	public boolean getAutoCreateConcepts()
	{
		return this.getOrCreateGlobalProperty(PROP_AUTOCREATE_CONCEPTS, this.m_defaultAutoCreateConcepts);
	}
    
    /**
	 * Get the shr-cdahandler.autocreate.locations value
	 */
	public boolean getAutoCreateLocations()
	{
		return this.getOrCreateGlobalProperty(PROP_AUTOCREATE_LOCATIONS, this.m_defaultAutoCreateLocations);
	}

	/**
	 * Get the shr-cdahandler.autocreate.metadata value
	 * @return
	 */
	public boolean getAutoCreateMetaData() {
		return this.getOrCreateGlobalProperty(PROP_AUTOCREATE_METADATA, this.m_defaultAutoCreateMetadata);
    }

	/**
	 * Get the shr-cdahandler.autocreate.patientidtype value
	 */
	public boolean getAutoCreatePatientIdType()
	{
		return this.getOrCreateGlobalProperty(PROP_AUTOCREATE_PATIENTIDTYPE, this.m_defaultAutoCreatePatientIdType);
	}
	
	/**
	 * Get the shr-cdahandler.autocreate.patients value
	 */
	public boolean getAutoCreatePatients() {
		return this.getOrCreateGlobalProperty(PROP_AUTOCREATE_PATIENTS, this.m_defaultAutoCreatePatients);
    }
	
	/**
	 * Get the shr-cdahandler.autocreate.persons value
	 * @return
	 */
	public boolean getAutoCreatePersons() {
		return this.getOrCreateGlobalProperty(PROP_AUTOCREATE_PERSONS, this.m_defaultAutoCreatePersons);
    }

	/**
	 * Get the shr-cdahandler.autocreate.providers value
	 */
	public boolean getAutoCreateProviders() {
		return this.getOrCreateGlobalProperty(PROP_AUTOCREATE_PROVIDERS, this.m_defaultAutoCreateProviders);
    }

	/**
	 * Get the shr-cdahandler.autocreate.users value
	 * @return
	 */
	public boolean getAutoCreateUsers()	{
		return this.getOrCreateGlobalProperty(PROP_AUTOCREATE_USERS,  this.m_defaultAutoCreateUsers);
		
	}
	
	/**
	 * Get the EPID root
	 * @return
	 */
	public String getEpidRoot() {
		return this.getOrCreateGlobalProperty(PROP_EPID_ROOT, this.m_defaultEpidRoot);
	}
	
	/**
	 * Get the shr-cdahandler.idformat value
	 */
	public String getIdFormat() {
		return this.getOrCreateGlobalProperty(PROP_ID_FORMAT, this.m_idFormat);
    }

	/**
	 * Get the shr-cdahandler.id.ecidRoot value
	 */
	public String getEcidRoot() {
		return this.getOrCreateGlobalProperty(PROP_ECID_ROOT, this.m_defaultEcidRoot);
    }

	/**
	 * Get the root of SHR records
	 */
	public String getShrRoot() {
		return this.getOrCreateGlobalProperty(PROP_SHR_ROOT, this.m_defaultShrRoot);
	}
	
	/**
	 * Get the root of Visits
	 */
	public String getVisitRoot() {
		return this.getShrRoot() + ".1";
	}
	
	/**
	 * Get the root of encounters
	 */
	public String getEncounterRoot() {
		return this.getShrRoot() + ".2";
	}	

	/**
	 * Get the root of Obs
	 */
	public String getObsRoot() {
		return this.getShrRoot() + ".3";
	}	

	/**
	 * Get the root of orders
	 */
	public String getOrderRoot() {
		return this.getShrRoot() + ".4";
	}	

	/**
	 * Get the root of problems
	 */
	public String getProblemRoot() {
		return this.getShrRoot() + ".5";
	}	

	/**
	 * Get the root of allergies
	 */
	public String getAllergyRoot() {
		return this.getShrRoot() + ".6";
	}	

	/**
	 * Get internal provider identifiers
	 */
	public String getProviderRoot() {
		return this.getShrRoot() + ".7";
    }

	/**
	 * Get internal location identifier root
	 */
	public String getLocationRoot() {
		return this.getShrRoot() + ".8";
    }

	/**
	 * Get internal patient root identifiers
	 */
	public String getPatientRoot() {
		return this.getShrRoot() + ".9";
    }

	/**
	 * Get the root oid for Users
	 */
	public String getUserRoot() {
		return this.getShrRoot() + ".10";
    }


	
	/**
     * Read a global property
     */
    private <T> T getOrCreateGlobalProperty(String propertyName, T defaultValue)
    {
		Object retVal = this.m_cachedProperties.get(propertyName);
		
		if(retVal != null)
			return (T)retVal;
		else 
		{
			String propertyValue = Context.getAdministrationService().getGlobalProperty(propertyName);
			if(propertyValue != null && !propertyValue.isEmpty())
			{
				T value = (T)FormatterUtil.fromWireFormat(propertyValue, defaultValue.getClass()); 
				synchronized (s_lockObject) {
					this.m_cachedProperties.put(propertyName, value);
                }
				return value;
			}
			else
			{
				Context.getAdministrationService().saveGlobalProperty(new GlobalProperty(propertyName, defaultValue.toString()));
				synchronized (s_lockObject) {
					this.m_cachedProperties.put(propertyName, defaultValue);
                }
				return defaultValue;
			}
		}
    }

	/**
	 * Get the shr-cdahandler.updatedExisting value
	 * 
	 * This affects the way that the SHR will persist entries / documents with identical IDs. If set to true
	 * then the SHR will simply perform an update. When false, all IDs must be unique or else a duplicate exception
	 * is thrown
	 * @return
	 */
	public boolean getUpdateExisting() {
		return this.getOrCreateGlobalProperty(PROP_UPDATE_EXISTING, this.m_defaultUpdateExisting);
    }

	/**
	 * Get the shr-cdahandler.validate.concept value
	 */
	public boolean getValidateConceptStructure() {
		return this.getOrCreateGlobalProperty(PROP_VALIDATE_CONCEPT_STRUCTURE, this.m_defaultValidateInstances);
    }

	/**
	 * Get the shr-cdahandler.validate.cda value
	 * @return
	 */
	public boolean getValidationEnabled() {
		return this.getOrCreateGlobalProperty(PROP_VALIDATE_STRUCTURE, this.m_defaultValidateInstances);
    }


	/**
	 * HIE configuration utility
	 */
	HealthInformationExchangeConfiguration() {
		
	}
	
	/**
	 * Get the instance of the configuration utility
	 * @return
	 */
	public static HealthInformationExchangeConfiguration getInstance()
	{
		if(s_instance == null)
			synchronized (s_lockObject) {
				if(s_instance == null)
					s_instance = new HealthInformationExchangeConfiguration();
			}
		return s_instance;
	}
	
	
    /**
     * Get the PDQ endpoint
     * @return
     */
    public String getPdqEndpoint() {
    	return this.getOrCreateGlobalProperty(PROP_NAME_PDQ_EP, "127.0.0.1");
    }
    
    /**
     * Get the PDQ port
     * @return
     */
    public Integer getPdqPort() {
    	return this.getOrCreateGlobalProperty(PROP_NAME_PDQ_EP_PORT, 2100);
    }
    
    /**
     * Get the PIX Endpoint
     * @return
     */
    public String getPixEndpoint() {
    	return this.getOrCreateGlobalProperty(PROP_NAME_PIX_EP, "127.0.0.1");
    }
    
    /**
     * Get the PIX POrt
     * @return
     */
    public Integer getPixPort() {
    	return this.getOrCreateGlobalProperty(PROP_NAME_PIX_EP_PORT, 2100);
    }
    
    /**
     * Get the XDS Registry endpoint
     * @return
     */
    public String getXdsRegistryEndpoint() {
    	return this.getOrCreateGlobalProperty(PROP_NAME_XDS_REG_EP, "http://localhost/xdsregistry");
    }
    
    /**
     * Get the XDS Repository endpoint
     * @return
     */
    public String getXdsRepositoryEndpoint() {
    	return this.getOrCreateGlobalProperty(PROP_NAME_XDS_REP_EP, "http://localhost/xdsrepository");
    }
    
}