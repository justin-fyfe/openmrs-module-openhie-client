package org.openmrs.module.shr.atna.configuration;

import org.marc.everest.formatters.FormatterUtil;
import org.openmrs.GlobalProperty;
import org.openmrs.api.context.Context;

/**
 * Shared Health Integration Components (SHIC) configuration utility
 * @author justi_000
 *
 */
public final class AtnaConfiguration {

	// Lock object
	private static final Object s_lockObject = new Object();
	// Singleton
	private static AtnaConfiguration s_instance;
	
	private static final String PROP_NAME_JKSTRUST_STORE = "shr-atna.security.trustStore";
	private static final String PROP_NAME_JKSTRUST_PASS = "shr-atna.security.trustStorePassword";
	private static final String PROP_NAME_JKSKEY_STORE = "shr-atna.security.keyStore";
	private static final String PROP_NAME_JKSKEY_PASS = "shr-atna.security.keyStorePassword";
	private static final String PROP_SHR_ROOT = "shr.id.root";
    public static final String PROP_EPID_ROOT = "shr.id.epidRoot";
	private static final String PROP_ECID_ROOT = "shr.id.ecidRoot";
	private static final String PROP_NAME_AR_ENDPOINT = "shr-atna.auditRepository.endpoint";
	private static final String PROP_NAME_AR_TRANSPORT = "shr-atna.auditRepository.transport";
	private static final String PROP_NAME_AR_PORT = "shr-atna.auditRepository.port";
	private static final String PROP_NAME_LOCAL_BIND_ADDR = "shr-atna.auditRepository.localBindAddr";
	private static final String PROP_NAME_DEV_NAME = "shr-atna.deviceName";
	
	private final String m_localBindAddrDefault = "127.0.0.1";
	private final String m_trustStoreDefault = "";
	private final String m_trustStorePasswordDefault = "";
	private final String m_keyStoreDefault = "";
	private final String m_keyStorePasswordDefault = "";
	private final String m_arEndpointDefault = "127.0.0.1";
	private final String m_arTransportDefault = "audit-udp";
	private final int m_arPortDefault = 514;
	private final String m_deviceNameDefault = "OpenSHRInstance";
	
	/**
	 * Shic configuration utility
	 */
	private AtnaConfiguration()
	{
		
	}
	
	/**
	 * Get the instance of the configuration utility
	 * @return
	 */
	public static AtnaConfiguration getInstance()
	{
		if(s_instance == null)
			synchronized (s_lockObject) {
				if(s_instance == null)
					s_instance = new AtnaConfiguration();
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
     * Get the ECID root
     * @return
     */
    public String getDeviceName()  { return this.getOrCreateGlobalProperty(PROP_NAME_DEV_NAME, this.m_deviceNameDefault); }

    /**
     * Get the audit repository endpoint
     * @return
     */
    public String getAuditRepositoryEndpoint() { return this.getOrCreateGlobalProperty(PROP_NAME_AR_ENDPOINT, this.m_arEndpointDefault); }
    /**
     * Get the audit repository trasnport
     * @return
     */
    public String getAuditRepositoryTransport() { return this.getOrCreateGlobalProperty(PROP_NAME_AR_TRANSPORT, this.m_arTransportDefault); }
    /**
     * Get the audit repository port
     * @return
     */
    public int getAuditRepositoryPort() { return this.getOrCreateGlobalProperty(PROP_NAME_AR_PORT, this.m_arPortDefault); }
    /**
     * Get the key store file name
     * @return
     */
    public String getKeyStoreFile() { return this.getOrCreateGlobalProperty(PROP_NAME_JKSKEY_STORE, this.m_keyStoreDefault); }
    /**
     * Get the key store password
     * @return
     */
    public String getKeyStorePassword() { return this.getOrCreateGlobalProperty(PROP_NAME_JKSKEY_PASS, this.m_keyStorePasswordDefault); }
    /**
     * Get the trust store file name
     * @return
     */
    public String getTrustStoreFile() { return this.getOrCreateGlobalProperty(PROP_NAME_JKSTRUST_STORE, this.m_trustStoreDefault); }
    /**
     * Get the trust store password
     * @return
     */
    public String getTrustStorePassword() { return this.getOrCreateGlobalProperty(PROP_NAME_JKSTRUST_PASS, this.m_trustStorePasswordDefault); }

    /**
     * Get the local binding address
     */
	public String getLocalBindAddress() {
		return this.getOrCreateGlobalProperty(PROP_NAME_LOCAL_BIND_ADDR, this.m_localBindAddrDefault); 
	}
	

}
