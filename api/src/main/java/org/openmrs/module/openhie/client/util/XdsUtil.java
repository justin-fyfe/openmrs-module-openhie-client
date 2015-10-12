package org.openmrs.module.openhie.client.util;

import javax.xml.bind.JAXBException;

import org.dcm4chee.xds2.infoset.rim.ClassificationType;
import org.dcm4chee.xds2.infoset.rim.ExternalIdentifierType;
import org.dcm4chee.xds2.infoset.rim.InternationalStringType;
import org.dcm4chee.xds2.infoset.rim.LocalizedStringType;
import org.dcm4chee.xds2.infoset.rim.RegistryObjectType;
import org.dcm4chee.xds2.infoset.util.InfosetUtil;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.module.shr.cdahandler.configuration.CdaHandlerConfiguration;

/**
 * Xds Utility class
 */
public final class XdsUtil {
	
	
	// Singleton stuff
	private static final Object s_lockObject = new Object();
	private static XdsUtil s_instance;
	private final CdaHandlerConfiguration m_cdaConfiguration = CdaHandlerConfiguration.getInstance();
	
	/**
	 * Private ctor
	 */
	private XdsUtil()
	{
		
	}
	
	/**
	 * Get instance of the XDS utility
	 */
	public static XdsUtil getInstance() {
		if(s_instance == null)
			synchronized (s_lockObject) {
				if(s_instance == null)
					s_instance = new XdsUtil();
            }
		return s_instance;
	}
	


	/**
	 * Add external identifier
	 */
	public ExternalIdentifierType addExtenalIdentifier(final RegistryObjectType classifiedObj, final String uuid, final String id, final String name) throws JAXBException {
	
		ExternalIdentifierType retVal = new ExternalIdentifierType();
		retVal.setRegistryObject(classifiedObj.getId());
		retVal.setIdentificationScheme(uuid);
		retVal.setValue(id);
		retVal.setName(new InternationalStringType());
		retVal.getName().getLocalizedString().add(new LocalizedStringType());
		retVal.getName().getLocalizedString().get(0).setValue(name);
		retVal.setId(String.format("eid%s", classifiedObj.getExternalIdentifier().size()));
		retVal.setName(new InternationalStringType());
		retVal.getName().getLocalizedString().add(new LocalizedStringType());
		retVal.getName().getLocalizedString().get(0).setValue(name);
		classifiedObj.getExternalIdentifier().add(retVal);
		return retVal;
	}
	
	/**
	 * Create a codified value classification
	 * @throws JAXBException 
	 */
	public ClassificationType addCodedValueClassification(final RegistryObjectType classifiedObj, final String uuid, final String code, final String scheme, String name) throws JAXBException {
	    ClassificationType retVal = new ClassificationType();
	    retVal.setClassifiedObject(classifiedObj.getId());
	    retVal.setClassificationScheme(uuid);
	    retVal.setNodeRepresentation(code);
	    retVal.setName(new InternationalStringType());
		retVal.getName().getLocalizedString().add(new LocalizedStringType());
		retVal.getName().getLocalizedString().get(0).setValue(code);
	    retVal.setId(String.format("cl%s",retVal.hashCode()));
	    InfosetUtil.addOrOverwriteSlot(retVal, "codingScheme", scheme);

	    retVal.setName(new InternationalStringType());
		retVal.getName().getLocalizedString().add(new LocalizedStringType());
		retVal.getName().getLocalizedString().get(0).setValue(name);
	    
	    classifiedObj.getClassification().add(retVal);
	    
	    return retVal;
    }

	/**
	 * Format identifier for XDS meta-data
	 */
	private String formatId(String root, String extension)
	{
		return String.format("%s^^^&%s&ISO", extension, root);
	}
	
	/**
	 * Get the ECID identifier for the patient
	 */
	public String getPatientIdentifier(Patient patient) {
		for(PatientIdentifier pid : patient.getIdentifiers())
			if(pid.getIdentifierType().getName().equals(this.m_cdaConfiguration.getEcidRoot())) // prefer the ecid
				return this.formatId(pid.getIdentifierType().getName(), pid.getIdentifier());
		return String.format(this.m_cdaConfiguration.getPatientRoot(), patient.getId().toString());// use the local identifier as last effort!
    }
}
