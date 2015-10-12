package org.openmrs.module.openhie.client.configuration;

import java.util.HashMap;
import java.util.Map;

import org.marc.everest.formatters.FormatterUtil;
import org.openmrs.GlobalProperty;
import org.openmrs.api.context.Context;
import org.openmrs.module.shr.cdahandler.configuration.CdaHandlerConfiguration;

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
    private static final String PROP_ID_REGEX = "openhie-client.id.regex";


    private Map<String, Object> m_cachedProperties = new HashMap<String, Object>();

	
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
    
    /**
     * Get the XDS Repository endpoint
     * @return
     */
    public String getIdRegex() {
    	return this.getOrCreateGlobalProperty(PROP_ID_REGEX, "^(.*)?\\^\\^\\^\\&(.*)?\\&ISO$");
    }
}