package org.openmrs.module.openhie.client.api.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.marc.everest.datatypes.II;
import org.marc.everest.datatypes.TS;
import org.openmrs.ImplementationId;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.PersonAddress;
import org.openmrs.PersonName;
import org.openmrs.Relationship;
import org.openmrs.api.context.Context;
import org.openmrs.module.openhie.client.configuration.HealthInformationExchangeConfiguration;
import org.openmrs.module.openhie.client.dao.HealthInformationExchangeDao;
import org.openmrs.module.shr.cdahandler.configuration.CdaHandlerConfiguration;
import org.openmrs.module.shr.cdahandler.configuration.CdaHandlerConfigurationFactory;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.app.Connection;
import ca.uhn.hl7v2.app.ConnectionHub;
import ca.uhn.hl7v2.app.Initiator;
import ca.uhn.hl7v2.llp.LLPException;
import ca.uhn.hl7v2.llp.LowerLayerProtocol;
import ca.uhn.hl7v2.llp.MinLowerLayerProtocol;
import ca.uhn.hl7v2.model.DataTypeException;
import ca.uhn.hl7v2.model.Group;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.Structure;
import ca.uhn.hl7v2.model.v25.datatype.CX;
import ca.uhn.hl7v2.model.v25.datatype.XAD;
import ca.uhn.hl7v2.model.v25.datatype.XPN;
import ca.uhn.hl7v2.model.v25.message.ADT_A01;
import ca.uhn.hl7v2.model.v25.message.QBP_Q21;
import ca.uhn.hl7v2.model.v25.segment.MSH;
import ca.uhn.hl7v2.model.v25.segment.PID;
import ca.uhn.hl7v2.parser.PipeParser;
import ca.uhn.hl7v2.util.Terser;


/**
 * Message utilities used by the API
 * @author Justin
 *
 */
public final class MessageUtil {

	private final Log log = LogFactory.getLog(this.getClass());
	
	// locking object
	private final static Object s_lockObject = new Object();
	// Instance
	private static MessageUtil s_instance = null;
	
	// Get the HIE config
	private HealthInformationExchangeConfiguration m_configuration = HealthInformationExchangeConfiguration.getInstance();
	private CdaHandlerConfiguration m_cdaConfiguration = CdaHandlerConfigurationFactory.getInstance();
	
	/**
	 * Creates a new message utility
	 */
	private MessageUtil() {
		
	}
	
	/**
	 * Get an instance of the message utility
	 */
	public static MessageUtil getInstance() {
		if(s_instance == null)
			synchronized (s_lockObject) {
				if(s_instance == null)
					s_instance = new MessageUtil();
			}
		return s_instance;
	}
	
	/**
	 * Send a HAPI message to the server and parse the response
	 * @throws HL7Exception 
	 * @throws IOException 
	 * @throws LLPException 
	 */
	public Message sendMessage(Message request, String endpoint, int port) throws HL7Exception, LLPException, IOException
	{
		LowerLayerProtocol llp = LowerLayerProtocol.makeLLP();
		PipeParser parser = new PipeParser();
		ConnectionHub hub = ConnectionHub.getInstance();
		Connection connection = null;
		try
		{
			if(log.isDebugEnabled())
				log.debug(String.format("Sending to %s:%s : %s", endpoint, port, parser.encode(request)));
			
			connection = hub.attach(endpoint, port, parser, MinLowerLayerProtocol.class);
			Initiator initiator = connection.getInitiator();
			Message response = initiator.sendAndReceive(request);
			
			if(log.isDebugEnabled())
				log.debug(String.format("Response from %s:%s : %s", endpoint, port, parser.encode(response)));
			
			return response;
		}
		finally
		{
			if(connection != null)
				hub.discard(connection);
		}
	}
	
	
	/**
	 * Create a PDQ message based on the search parameters
	 * @throws HL7Exception 
	 */
	public Message createPdqMessage(Map<String, String> queryParameters) throws HL7Exception
	{
        QBP_Q21 message = new QBP_Q21();
        this.updateMSH(message.getMSH(), "QBP", "Q22");
        // What do these statements do?
        Terser terser = new Terser(message);
        
        // Set the query parmaeters
        int qpdRep = 0;
        for(Map.Entry<String, String> entry : queryParameters.entrySet())
        {
	        terser.set(String.format("/QPD-3(%d)-1", qpdRep), entry.getKey());
	        terser.set(String.format("/QPD-3(%d)-2", qpdRep++), entry.getValue());
        }
        
        terser.set("/QPD-1-1", "Q22");
        terser.set("/QPD-1-2", "Find Candidates");
        terser.set("/QPD-1-3", "HL7");
        terser.set("/QPD-2-1", UUID.randomUUID().toString());
        
        return message;
	}

	/**
	 * Create the admit patient message
	 * @param patient
	 * @return
	 * @throws HL7Exception 
	 */
	public Message createAdmit(Patient patient) throws HL7Exception
	{
		ADT_A01 message = new ADT_A01();
		this.updateMSH(message.getMSH(), "ADT", "A01");
		message.getMSH().getVersionID().getVersionID().setValue("2.3.1");
		
		// Move patient data to PID
		this.updatePID(message.getPID(), patient);

		return message;
	}
	
	/**
	 * Update the PID segment
	 * @throws HL7Exception 
	 */
	private void updatePID(PID pid, Patient patient) throws HL7Exception {

		// Update the pid segment with data in the patient
		
		// PID-3
		pid.getPatientIdentifierList(0).getAssigningAuthority().getUniversalID().setValue(this.m_cdaConfiguration.getPatientRoot());
		pid.getPatientIdentifierList(0).getAssigningAuthority().getUniversalIDType().setValue("ISO");
		pid.getPatientIdentifierList(0).getIDNumber().setValue(patient.getId().toString());
		pid.getPatientIdentifierList(0).getIdentifierTypeCode().setValue("PI");
		
		// Other identifiers
		for(PatientIdentifier patIdentifier : patient.getIdentifiers())
		{
			CX patientId = pid.getPatientIdentifierList(pid.getPatientIdentifierList().length);
			if(II.isRootOid(new II(patIdentifier.getIdentifierType().getName())))
			{
				patientId.getAssigningAuthority().getUniversalID().setValue(patIdentifier.getIdentifierType().getName());
				patientId.getAssigningAuthority().getUniversalIDType().setValue("ISO");
			}
			else
				patientId.getAssigningAuthority().getNamespaceID().setValue(patIdentifier.getIdentifierType().getName());

			patientId.getIDNumber().setValue(patIdentifier.getIdentifier());
			patientId.getIdentifierTypeCode().setValue("PT");
		}

		// Names
		for(PersonName pn : patient.getNames())
			this.updateXPN(pid.getPatientName(pid.getPatientName().length), pn);
		
		// Gender
		pid.getAdministrativeSex().setValue(patient.getGender());
		
		// Date of birth
		if(patient.getBirthdateEstimated())
			pid.getDateTimeOfBirth().getTime().setValue(new SimpleDateFormat("yyyy").format(patient.getBirthdate()));
		else
			pid.getDateTimeOfBirth().getTime().setValue(new SimpleDateFormat("yyyyMMdd").format(patient.getBirthdate()));
		
		// Addresses
		for(PersonAddress pa : patient.getAddresses())
		{
			XAD xad = pid.getPatientAddress(pid.getPatientAddress().length);
			if(pa.getAddress1() != null)
				xad.getStreetAddress().getStreetOrMailingAddress().setValue(pa.getAddress1());
			if(pa.getAddress2() != null)
				xad.getOtherDesignation().setValue(pa.getAddress2());
			if(pa.getCityVillage() != null)
				xad.getCity().setValue(pa.getCityVillage());
			if(pa.getCountry() != null)
				xad.getCountry().setValue(pa.getCountry());
			if(pa.getCountyDistrict() != null)
				xad.getCountyParishCode().setValue(pa.getCountyDistrict());
			if(pa.getPostalCode() != null)
				xad.getZipOrPostalCode().setValue(pa.getPostalCode());
			if(pa.getStateProvince() != null)
				xad.getStateOrProvince().setValue(pa.getStateProvince());
			
			if(pa.getPreferred())
				xad.getAddressType().setValue("L");
		}
		
		// Death?
		if(patient.getDead())
		{
			pid.getPatientDeathIndicator().setValue("Y");
			pid.getPatientDeathDateAndTime().getTime().setDatePrecision(patient.getDeathDate().getYear(), patient.getDeathDate().getMonth(), patient.getDeathDate().getDay());
		}
		
		// Mother?
		for(Relationship rel : Context.getPersonService().getRelationships(patient))
		{
			if(rel.getRelationshipType().getDescription().contains("MTH") &&
					patient.equals(rel.getPersonB())) //MOTHER?
			{
				// TODO: Find a better ID 
				this.updateXPN(pid.getMotherSMaidenName(0), rel.getPersonB().getNames().iterator().next());
				pid.getMotherSIdentifier(0).getAssigningAuthority().getUniversalID().setValue(this.m_cdaConfiguration.getPatientRoot());
				pid.getMotherSIdentifier(0).getAssigningAuthority().getUniversalIDType().setValue("ISO");
				pid.getMotherSIdentifier(0).getIDNumber().setValue(String.format("%s",rel.getPersonB().getId()));
			}
				
		}
		
	}

	/**
	 * Updates the PN with the XPN
	 * @param xpn
	 * @param pn
	 * @throws DataTypeException
	 */
	private void updateXPN(XPN xpn, PersonName pn) throws DataTypeException {
		if(pn.getFamilyName() != null)
			xpn.getFamilyName().getSurname().setValue(pn.getFamilyName());
		if(pn.getFamilyName2() != null)
			xpn.getFamilyName().getSurnameFromPartnerSpouse().setValue(pn.getFamilyName2());
		if(pn.getGivenName() != null)
			xpn.getGivenName().setValue(pn.getGivenName());
		if(pn.getMiddleName() != null)
			xpn.getSecondAndFurtherGivenNamesOrInitialsThereof().setValue(pn.getMiddleName());
		if(pn.getPrefix() != null)
			xpn.getPrefixEgDR().setValue(pn.getPrefix());
		
		if(pn.getPreferred())
			xpn.getNameTypeCode().setValue("L");
		else
			xpn.getNameTypeCode().setValue("U");

	}

	/**
	 * Update MSH
	 * @param msh
	 * @throws DataTypeException 
	 */
	private void updateMSH(MSH msh, String messageCode, String triggerEvent) throws DataTypeException {
        msh.getFieldSeparator().setValue("|");
        msh.getEncodingCharacters().setValue("^~\\&");
        msh.getAcceptAcknowledgmentType().setValue("AL"); // Always send response
        msh.getDateTimeOfMessage().getTime().setValue(new SimpleDateFormat("yyyyMMddHHmmss").format(new Date())); // DateTime of message
        msh.getMessageControlID().setValue(UUID.randomUUID().toString()); // Unique id for message
        msh.getMessageType().getMessageStructure().setValue(msh.getMessage().getName()); // Message Structure Type
        msh.getMessageType().getMessageCode().setValue(messageCode); // Message Structure Code
        msh.getMessageType().getTriggerEvent().setValue(triggerEvent); // Trigger Event
        msh.getProcessingID().getProcessingID().setValue("P"); // Production
        msh.getReceivingApplication().getNamespaceID().setValue("CR"); // Client Registry
        msh.getReceivingFacility().getNamespaceID().setValue("MOH_CAAT"); // Mohawk College of Applied Arts and Technology
        
        ImplementationId implementation = Context.getAdministrationService().getImplementationId();
        if(implementation != null)
	        msh.getSendingApplication().getNamespaceID().setValue(implementation.getName()); // What goes here?
        else
        	msh.getSendingApplication().getNamespaceID().setValue("UNNAMEDOPENMRS");
        
        Location defaultLocale = Context.getLocationService().getDefaultLocation();
        if(defaultLocale != null)
	        msh.getSendingFacility().getNamespaceID().setValue(defaultLocale.getName()); // You're at the college... right?
        else
        	msh.getSendingApplication().getNamespaceID().setValue("UNNAMEDOPENMRS");

        msh.getVersionID().getVersionID().setValue("2.5");
	}

	/**
	 * Interpret PID segments
	 * @param response
	 * @return
	 * @throws HL7Exception 
	 */
	public List<Patient> interpretPIDSegments(
			Message response) throws HL7Exception {
		List<Patient> retVal = new ArrayList<Patient>();
		
		Location defaultLocation = Context.getLocationService().getDefaultLocation();
		
		for(Structure queryResponseStruct : response.getAll("QUERY_RESPONSE"))
		{
			Group queryResponseGroup = (Group)queryResponseStruct;
			for(Structure pidStruct : queryResponseGroup.getAll("PID"))
			{
				PID pid = (PID)pidStruct;
				Patient patient = new Patient();
				// Attempt to load a patient by identifier
				for(CX id : pid.getPatientIdentifierList())
				{
					PatientIdentifierType pit = Context.getPatientService().getPatientIdentifierType(id.getAssigningAuthority().getUniversalID().getValue());
					if(pit == null) 
						pit = Context.getPatientService().getPatientIdentifierTypeByName(id.getAssigningAuthority().getNamespaceID().getValue());
					if(pit == null)
						continue;
					PatientIdentifier patId = new PatientIdentifier(
							id.getIDNumber().getValue(),
							pit,
							defaultLocation
							);
					
					// Do not include the local identifier
					if(id.getAssigningAuthority().getUniversalID().equals(this.m_cdaConfiguration.getPatientRoot()))
						patId.setPreferred(true);
					
					patient.addIdentifier(patId);
				}
				
				// Attempt to copy names
				for(XPN xpn : pid.getPatientName())
				{
					PersonName pn = new PersonName();
					pn.setFamilyName(xpn.getFamilyName().getSurname().getValue());
					pn.setFamilyName2(xpn.getFamilyName().getSurnameFromPartnerSpouse().getValue());
					pn.setGivenName(xpn.getGivenName().getValue());
					pn.setMiddleName(xpn.getSecondAndFurtherGivenNamesOrInitialsThereof().getValue());
					pn.setPrefix(xpn.getPrefixEgDR().getValue());
					
					if("L".equals(xpn.getNameTypeCode().getValue()))
						pn.setPreferred(true);
					
					patient.addName(pn);
				}
				
				// Copy gender
				patient.setGender(pid.getAdministrativeSex().getValue());
				// Copy DOB
				if(pid.getDateTimeOfBirth().getTime().getValue() != null)
				{
					TS tsTemp = TS.valueOf(pid.getDateTimeOfBirth().getTime().getValue());
					patient.setBirthdate(tsTemp.getDateValue().getTime());
					patient.setBirthdateEstimated(tsTemp.getDateValuePrecision() < TS.DAY);
				}
				
				// Death details
				if(pid.getPatientDeathDateAndTime().getTime().getValue() != null)
				{
					TS tsTemp = TS.valueOf(pid.getPatientDeathDateAndTime().getTime().getValue());
					patient.setDeathDate(tsTemp.getDateValue().getTime());
				}
				patient.setDead("Y".equals(pid.getPatientDeathIndicator().getValue()));
				
				
				// Addresses
				for(XAD xad : pid.getPatientAddress())
				{
					PersonAddress pa = new PersonAddress();
					pa.setAddress1(xad.getStreetAddress().getStreetOrMailingAddress().getValue());
					pa.setAddress2(xad.getOtherDesignation().getValue());
					pa.setCityVillage(xad.getCity().getValue());
					pa.setCountry(xad.getCountry().getValue());
					pa.setCountyDistrict(xad.getCountyParishCode().getValue());
					pa.setPostalCode(xad.getZipOrPostalCode().getValue());
	
					if("L".equals(xad.getAddressType().getValue()))
						pa.setPreferred(true);
					
					patient.addAddress(pa);
					
				}
			
				retVal.add(patient);
			}
		}

		return retVal;
	}

	/**
	 * Create a PIX message
	 * @throws HL7Exception 
	 */
	public Message createPixMessage(Patient patient, String toAssigningAuthority) throws HL7Exception {
		QBP_Q21 retVal = new QBP_Q21();
		this.updateMSH(retVal.getMSH(), "QBP", "Q23");
		retVal.getMSH().getVersionID().getVersionID().setValue("2.3.1");

		Terser queryTerser = new Terser(retVal);
		queryTerser.set("/QPD-3-1", patient.getId().toString());
		queryTerser.set("/QPD-3-4-2", this.m_cdaConfiguration.getPatientRoot());
		queryTerser.set("/QPD-3-4-3", "ISO");
		
		// To domain
		if(II.isRootOid(new II(toAssigningAuthority)))
		{
			queryTerser.set("/QPD-4-4-2", toAssigningAuthority);
			queryTerser.set("/QPD-4-4-3", "ISO");
		}
		else
			queryTerser.set("/QPD-4-4-1", toAssigningAuthority);
		
		return retVal;
	}
	
}
