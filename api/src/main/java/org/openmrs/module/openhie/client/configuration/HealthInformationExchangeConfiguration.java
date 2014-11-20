package org.openmrs.module.openhie.client.configuration;

import org.marc.everest.formatters.FormatterUtil;
import org.openmrs.GlobalProperty;
import org.openmrs.api.context.Context;
import org.openmrs.module.shr.atna.configuration.AtnaConfiguration;

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
	
	private static final String PROP_NAME_PDQ_EP = "ohie-client.endpoint.pdq";
	private static final String PROP_NAME_PDQ_EP_PORT = "ohie-client.endpoint.pdq.port";
	private static final String PROP_NAME_PIX_EP = "ohie-client.endpoint.pix";
	private static final String PROP_NAME_PIX_EP_PORT = "ohie-client.endpoint.pix.port";
	private static final String PROP_NAME_XDS_REG_EP = "ohie-client.endpoint.xds.registry";
	private static final String PROP_NAME_XDS_REP_EP = "ohie-client.endpoint.xds.repository";
	

	/**
	 * Shic configuration utility
	 */
	private HealthInformationExchangeConfiguration() {
		
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
     * Read a global property
     */
    private <T> T getOrCreateGlobalProperty(String propertyName, T defaultValue)
    {
		String propertyValue = Context.getAdministrationService().getGlobalProperty(propertyName);
		if(propertyValue != null && !propertyValue.isEmpty())
			return (T)FormatterUtil.fromWireFormat(propertyValue, defaultValue.getClass());
		else
		{
			Context.getAdministrationService().saveGlobalProperty(new GlobalProperty(propertyName, defaultValue.toString()));
			return defaultValue;
		}
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