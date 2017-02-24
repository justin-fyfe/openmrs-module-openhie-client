package org.openmrs.module.shr.atna.api.impl;

import org.dcm4che3.audit.AuditMessages;
import org.dcm4che3.net.Connection;
import org.dcm4che3.net.Device;
import org.dcm4che3.net.audit.AuditLogger;
import org.dcm4che3.net.audit.AuditRecordRepository;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.shr.atna.api.AtnaAuditService;
import org.openmrs.module.shr.atna.configuration.AtnaConfiguration;

/**
 * Openmrs service implementation for Atna auditor
 */
public class AtnaAuditServiceImpl extends BaseOpenmrsService implements AtnaAuditService {
	
	// Audit logger
	private Object m_lockObject = new Object();
	protected AtnaConfiguration m_configuration = null;
	protected AuditLogger m_logger = null;
	
	private AuditLogger getOrCreateAuditLogger() {
		if(this.m_logger == null)
		{
			synchronized (this.m_lockObject) {
	            if(this.m_logger == null)
	            {
            		this.m_configuration = AtnaConfiguration.getInstance();	
            		this.m_logger = this.createLoggerDevice().getDeviceExtension(AuditLogger.class);
	            }
            }
		}
		return this.m_logger;
	}
	
	/**
	 * Create logger device
	 */
	private Device createLoggerDevice() { 
		Device device = new Device(this.m_configuration.getDeviceName());
		
		Connection transportConnection = new Connection(this.m_configuration.getAuditRepositoryTransport(), this.m_configuration.getAuditRepositoryEndpoint());
		
		if("audit-udp".equals(transportConnection.getCommonName()))
		{

			transportConnection.setClientBindAddress(this.m_configuration.getLocalBindAddress());
			transportConnection.setProtocol(Connection.Protocol.SYSLOG_UDP);
		}
		else if("audit-tls".equals(transportConnection.getCommonName()))
		{
			if(!this.m_configuration.getLocalBindAddress().equals("0.0.0.0"))
				transportConnection.setClientBindAddress(this.m_configuration.getLocalBindAddress());
				

			device.setKeyStoreURL(this.m_configuration.getKeyStoreFile());
			device.setKeyStorePin(this.m_configuration.getKeyStorePassword());
			device.setKeyStoreType("JKS");
			device.setTrustStoreURL(this.m_configuration.getTrustStoreFile());
			device.setTrustStorePin(this.m_configuration.getTrustStorePassword());
			device.setTrustStoreType("JKS");
			transportConnection.setProtocol(Connection.Protocol.SYSLOG_TLS);
			transportConnection.setTlsCipherSuites("TLS_RSA_WITH_AES_128_CBC_SHA");
		}
		else
			throw new IllegalArgumentException("Connection must be audit-tls or audit-udp");

		transportConnection.setPort(this.m_configuration.getAuditRepositoryPort());
		
		device.addConnection(transportConnection);
		
		AuditRecordRepository repository = new AuditRecordRepository();
		device.addDeviceExtension(repository);
		repository.addConnection(transportConnection);

		AuditLogger logger = new AuditLogger();
		device.addDeviceExtension(logger);
		logger.addConnection(transportConnection);
		logger.setAuditRecordRepositoryDevice(device);
		logger.setSchemaURI(AuditMessages.SCHEMA_URI);
		
		return device;
		
	}
	
	/**
	 * Get the audit logger
	 * @see org.openmrs.module.shr.atna.api.AtnaAuditService#getLogger()
	 */
	public AuditLogger getLogger() {
		return this.getOrCreateAuditLogger();
	}
	
}
