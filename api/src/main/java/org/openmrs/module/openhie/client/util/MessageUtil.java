package org.openmrs.module.openhie.client.util;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dcm4chee.xds2.common.XDSConstants;
import org.dcm4chee.xds2.infoset.ihe.ProvideAndRegisterDocumentSetRequestType;
import org.dcm4chee.xds2.infoset.ihe.ProvideAndRegisterDocumentSetRequestType.Document;
import org.dcm4chee.xds2.infoset.ihe.RetrieveDocumentSetRequestType;
import org.dcm4chee.xds2.infoset.ihe.RetrieveDocumentSetResponseType;
import org.dcm4chee.xds2.infoset.rim.AdhocQueryRequest;
import org.dcm4chee.xds2.infoset.rim.AdhocQueryResponse;
import org.dcm4chee.xds2.infoset.rim.AdhocQueryType;
import org.dcm4chee.xds2.infoset.rim.AssociationType1;
import org.dcm4chee.xds2.infoset.rim.ClassificationType;
import org.dcm4chee.xds2.infoset.rim.ExtrinsicObjectType;
import org.dcm4chee.xds2.infoset.rim.IdentifiableType;
import org.dcm4chee.xds2.infoset.rim.InternationalStringType;
import org.dcm4chee.xds2.infoset.rim.LocalizedStringType;
import org.dcm4chee.xds2.infoset.rim.RegistryObjectListType;
import org.dcm4chee.xds2.infoset.rim.RegistryPackageType;
import org.dcm4chee.xds2.infoset.rim.RegistryResponseType;
import org.dcm4chee.xds2.infoset.rim.ResponseOptionType;
import org.dcm4chee.xds2.infoset.rim.SubmitObjectsRequest;
import org.dcm4chee.xds2.infoset.util.DocumentRegistryPortTypeFactory;
import org.dcm4chee.xds2.infoset.util.DocumentRepositoryPortTypeFactory;
import org.dcm4chee.xds2.infoset.util.InfosetUtil;
import org.dcm4chee.xds2.infoset.ws.registry.DocumentRegistryPortType;
import org.dcm4chee.xds2.infoset.ws.repository.DocumentRepositoryPortType;
import org.marc.everest.datatypes.II;
import org.marc.everest.datatypes.TS;
import org.openmrs.ImplementationId;
import org.openmrs.Location;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.PersonAddress;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonAttributeType;
import org.openmrs.PersonName;
import org.openmrs.Provider;
import org.openmrs.Relationship;
import org.openmrs.api.context.Context;
import org.openmrs.module.openhie.client.configuration.HealthInformationExchangeConfiguration;
import org.openmrs.module.openhie.client.exception.HealthInformationExchangeException;
import org.openmrs.module.openhie.client.hie.model.DocumentInfo;
import org.openmrs.module.shr.cdahandler.configuration.CdaHandlerConfiguration;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.app.Connection;
import ca.uhn.hl7v2.app.ConnectionHub;
import ca.uhn.hl7v2.app.Initiator;
import ca.uhn.hl7v2.llp.LLPException;
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
	private CdaHandlerConfiguration m_cdaConfiguration = CdaHandlerConfiguration.getInstance();
	
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
		this.updatePID(message.getPID(), patient, false);

		return message;
	}
	
	/**
	 * Update the PID segment
	 * @throws HL7Exception 
	 */
	private void updatePID(PID pid, Patient patient, boolean localIdOnly) throws HL7Exception {

		// Update the pid segment with data in the patient
		
		// PID-3
		pid.getPatientIdentifierList(0).getAssigningAuthority().getUniversalID().setValue(this.m_cdaConfiguration.getPatientRoot());
		pid.getPatientIdentifierList(0).getAssigningAuthority().getUniversalIDType().setValue("ISO");
		pid.getPatientIdentifierList(0).getIDNumber().setValue(patient.getId().toString());
		pid.getPatientIdentifierList(0).getIdentifierTypeCode().setValue("PI");
		
		// Other identifiers
		if(!localIdOnly)
			for(PatientIdentifier patIdentifier : patient.getIdentifiers())
			{
				CX patientId = pid.getPatientIdentifierList(pid.getPatientIdentifierList().length);
				if(II.isRootOid(new II(patIdentifier.getIdentifierType().getName())))
				{
					patientId.getAssigningAuthority().getUniversalID().setValue(patIdentifier.getIdentifierType().getName());
					patientId.getAssigningAuthority().getUniversalIDType().setValue("ISO");
				}
				else if(II.isRootOid(new II(patIdentifier.getIdentifierType().getUuid())))
				{
					patientId.getAssigningAuthority().getUniversalID().setValue(patIdentifier.getIdentifierType().getUuid());
					patientId.getAssigningAuthority().getUniversalIDType().setValue("ISO");
				}
				else
					patientId.getAssigningAuthority().getNamespaceID().setValue(patIdentifier.getIdentifierType().getName());
	
				patientId.getIDNumber().setValue(patIdentifier.getIdentifier());
				patientId.getIdentifierTypeCode().setValue("PT");
			}

		// Names
		for(PersonName pn : patient.getNames())
			if(!pn.getFamilyName().equals("(none)") && !pn.getGivenName().equals("(none)"))
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
		if(pn.getFamilyName() != null && !pn.getFamilyName().equals("(none)"))
			xpn.getFamilyName().getSurname().setValue(pn.getFamilyName());
		if(pn.getFamilyName2() != null)
			xpn.getFamilyName().getSurnameFromPartnerSpouse().setValue(pn.getFamilyName2());
		if(pn.getGivenName() != null && !pn.getGivenName().equals("(none)"))
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
        	msh.getSendingFacility().getNamespaceID().setValue("LOCATION");

        msh.getVersionID().getVersionID().setValue("2.5");
	}

	/**
	 * Interpret PID segments
	 * @param response
	 * @return
	 * @throws HL7Exception 
	 */
	public List<Patient> interpretPIDSegments(
			Message response) throws HL7Exception, HealthInformationExchangeException {
		List<Patient> retVal = new ArrayList<Patient>();
		
		Terser terser = new Terser(response);
		// Check for AA and OK in QAK
		if(terser.get("/MSA-1") != null && 
				terser.get("/MSA-1").equals("AE"))
			throw new HealthInformationExchangeException("Server Error");
		else if(terser.get("/QAK-2") != null && 
				terser.get("/QAK-2").equals("NF"))
			return retVal;

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
					
					PatientIdentifierType pit = null;
					
					if(id.getAssigningAuthority().getUniversalID().getValue() != null &&
							!id.getAssigningAuthority().getUniversalID().getValue().isEmpty())
					{
							pit = Context.getPatientService().getPatientIdentifierTypeByName(id.getAssigningAuthority().getUniversalID().getValue());
							if(pit == null)
								pit = Context.getPatientService().getPatientIdentifierTypeByUuid(id.getAssigningAuthority().getUniversalID().getValue());
							else if(!pit.getUuid().equals(id.getAssigningAuthority().getUniversalID().getValue())) // fix the UUID
							{
								log.debug(String.format("Updating %s to have UUID %s", pit.getName(), id.getAssigningAuthority().getUniversalID().getValue()));
								pit.setUuid(id.getAssigningAuthority().getUniversalID().getValue());
								Context.getPatientService().savePatientIdentifierType(pit);
							}
					}
					if(pit == null && id.getAssigningAuthority().getNamespaceID().getValue() != null &&
							!id.getAssigningAuthority().getNamespaceID().getValue().isEmpty())
					{
						pit = Context.getPatientService().getPatientIdentifierTypeByName(id.getAssigningAuthority().getNamespaceID().getValue());
						if(pit != null && !pit.getUuid().equals(id.getAssigningAuthority().getUniversalID().getValue())) // fix the UUID
						{
							log.debug(String.format("Updating %s to have UUID %s", pit.getName(), id.getAssigningAuthority().getUniversalID().getValue()));
							pit.setUuid(id.getAssigningAuthority().getUniversalID().getValue());
							Context.getPatientService().savePatientIdentifierType(pit);
						}

					}
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
					
					if(xpn.getFamilyName().getSurname().getValue() == null || xpn.getFamilyName().getSurname().getValue().isEmpty())
						pn.setFamilyName("(none)");
					else
						pn.setFamilyName(xpn.getFamilyName().getSurname().getValue());
					pn.setFamilyName2(xpn.getFamilyName().getSurnameFromPartnerSpouse().getValue());
					
					// Given name
					if(xpn.getGivenName().getValue() == null || xpn.getGivenName().getValue().isEmpty())
						pn.setGivenName("(none)");
					else
						pn.setGivenName(xpn.getGivenName().getValue());
					pn.setMiddleName(xpn.getSecondAndFurtherGivenNamesOrInitialsThereof().getValue());
					pn.setPrefix(xpn.getPrefixEgDR().getValue());
					
					if("L".equals(xpn.getNameTypeCode().getValue()))
						pn.setPreferred(true);
					
					patient.addName(pn);
				}
				
				if(patient.getNames().size() == 0)
					patient.addName(new PersonName("(none)", null, "(none)"));
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
					pa.setStateProvince(xad.getStateOrProvince().getValue());
					if("H".equals(xad.getAddressType().getValue()))
						pa.setPreferred(true);
					
					patient.addAddress(pa);
					
				}
			
				// Mother's name
				XPN momsName = pid.getMotherSMaidenName(0);
				if(momsName != null)
				{
					PersonAttributeType momNameAtt = Context.getPersonService().getPersonAttributeTypeByName("Mother's Name");
					if(momNameAtt != null)
					{
						PersonAttribute pa = new PersonAttribute(momNameAtt, String.format("%s, %s", momsName.getFamilyName().getSurname().getValue(), momsName.getGivenName().getValue()));
						patient.addAttribute(pa);
					}
				}
				retVal.add(patient);
			}
		}

		return retVal;
	}

	/**
	 * Create an xds query
	 * @throws HealthInformationExchangeException 
	 * @throws JAXBException 
	 */
	public AdhocQueryRequest createXdsQuery(Patient patient, String formatCode, Date since) throws HealthInformationExchangeException, JAXBException
	{
		AdhocQueryRequest retVal = new AdhocQueryRequest();
		retVal.setResponseOption(new ResponseOptionType());
		retVal.getResponseOption().setReturnComposedObjects(true);
		retVal.getResponseOption().setReturnType("LeafClass");
		retVal.setAdhocQuery(new AdhocQueryType());
		retVal.getAdhocQuery().setId(XDSConstants.XDS_FindDocuments);
		
		// Find the ecid
		String ecid = null;
		for(PatientIdentifier id : patient.getIdentifiers())
			if(id.getIdentifierType().getName().equals(this.m_cdaConfiguration.getEcidRoot()) || 
					id.getIdentifierType().getUuid().equals(this.m_cdaConfiguration.getEcidRoot()))
				ecid = String.format("%s^^^&%s&ISO", id.getIdentifier(), this.m_cdaConfiguration.getEcidRoot());
		
		if(ecid == null)
			throw new HealthInformationExchangeException("No enterprise identifier found in patient supplied");

		InfosetUtil.addOrOverwriteSlot(retVal.getAdhocQuery(), XDSConstants.QRY_DOCUMENT_ENTRY_PATIENT_ID, String.format("'%s'",ecid));
		InfosetUtil.addOrOverwriteSlot(retVal.getAdhocQuery(), XDSConstants.QRY_DOCUMENT_ENTRY_STATUS, String.format("'%s'", XDSConstants.STATUS_APPROVED));
		if(formatCode != null)
			InfosetUtil.addOrOverwriteSlot(retVal.getAdhocQuery(), XDSConstants.QRY_DOCUMENT_ENTRY_FORMAT_CODE, String.format("'%s'", formatCode));
		if(since != null)
			InfosetUtil.addOrOverwriteSlot(retVal.getAdhocQuery(), XDSConstants.QRY_DOCUMENT_ENTRY_CREATION_TIME_FROM, String.format("'%s'", new SimpleDateFormat("yyyyMMddHHmm").format(since)));
		
		return retVal;
	}
	/**
	 * Create a PIX message
	 * @throws HL7Exception 
	 */
	public Message createPixMessage(Patient patient, String toAssigningAuthority) throws HL7Exception {
		QBP_Q21 retVal = new QBP_Q21();
		this.updateMSH(retVal.getMSH(), "QBP", "Q23");
		retVal.getMSH().getVersionID().getVersionID().setValue("2.5");

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

	/**
	 * Create the update message
	 * @throws HL7Exception 
	 */
	public Message createUpdate(Patient patient) throws HL7Exception {
		ADT_A01 message = new ADT_A01();
		this.updateMSH(message.getMSH(), "ADT", "A08");
		message.getMSH().getVersionID().getVersionID().setValue("2.3.1");
		//message.getMSH().getMessageType().getMessageStructure().setValue("ADT_A08");
		
		// Move patient data to PID
		this.updatePID(message.getPID(), patient, true);
		
		return message;    
	}

	/**
	 * Send the XDS query
	 * @param createXdsQuery
	 * @return
	 * @throws HealthInformationExchangeException 
	 */
	public AdhocQueryResponse sendXdsQuery(AdhocQueryRequest xdsQuery) throws HealthInformationExchangeException {
		
		DocumentRegistryPortType port = DocumentRegistryPortTypeFactory.getDocumentRegistryPortSoap12(this.m_configuration.getXdsRegistryEndpoint());
		try
		{
			return port.documentRegistryRegistryStoredQuery(xdsQuery);
		}
		catch(Exception e)
		{
			log.error(e);
			throw new HealthInformationExchangeException(e);
		}
    }
	
	/**
	 * Send the XDS query
	 * @param createXdsQuery
	 * @return
	 * @throws HealthInformationExchangeException 
	 */
	public RetrieveDocumentSetResponseType sendRetrieve(RetrieveDocumentSetRequestType retrieveRequest) throws HealthInformationExchangeException {
		
		DocumentRepositoryPortType port = DocumentRepositoryPortTypeFactory.getDocumentRepositoryPortSoap12(this.m_configuration.getXdsRepositoryEndpoint());
		try
		{
			return port.documentRepositoryRetrieveDocumentSet(retrieveRequest);
		}
		catch(Exception e)
		{
			log.error(e);
			throw new HealthInformationExchangeException(e);
		}
    }
	
	
	/**
	 * Interpret the ad-hoc query response
	 */
	public List<DocumentInfo> interpretAdHocQueryResponse(AdhocQueryResponse response, Patient patient, boolean oddOnly)
	{
		List<DocumentInfo> retVal = new ArrayList<DocumentInfo>();
		
		for(JAXBElement<? extends IdentifiableType> jaxElement : response.getRegistryObjectList().getIdentifiable())
		{
			if(jaxElement.getValue() instanceof ExtrinsicObjectType)
			{
				ExtrinsicObjectType eo = (ExtrinsicObjectType)jaxElement.getValue();
				if(oddOnly && !eo.getObjectType().equals("urn:uuid:34268e47-fdf5-41a6-ba33-82133c465248"))
					continue;
					
				DocumentInfo docInfo = new DocumentInfo();
				TS ts = TS.valueOf(InfosetUtil.getSlotValue(eo.getSlot(), XDSConstants.SLOT_NAME_CREATION_TIME, null));
				docInfo.setCreationTime(ts.getDateValue().getTime());
				docInfo.setHash(InfosetUtil.getSlotValue(eo.getSlot(), XDSConstants.SLOT_NAME_HASH, "").getBytes());
				docInfo.setRepositoryId(InfosetUtil.getSlotValue(eo.getSlot(), XDSConstants.SLOT_NAME_REPOSITORY_UNIQUE_ID, ""));
				docInfo.setMimeType(eo.getMimeType());
				docInfo.setPatient(patient);
				if(eo.getName() != null &&
						eo.getName().getLocalizedString() != null &&
						eo.getName().getLocalizedString().size() > 0)
					docInfo.setTitle(eo.getName().getLocalizedString().get(0).getValue());
				docInfo.setUniqueId(InfosetUtil.getExternalIdentifierValue(XDSConstants.UUID_XDSDocumentEntry_uniqueId, eo));
				
				for(ClassificationType ct : eo.getClassification())
					if(ct.getClassificationScheme().equals(XDSConstants.UUID_XDSDocumentEntry_classCode))
						docInfo.setClassCode(ct.getNodeRepresentation());
					else if(ct.getClassificationScheme().equals(XDSConstants.UUID_XDSDocumentEntry_typeCode))
						docInfo.setTypeCode(ct.getNodeRepresentation());
					else if(ct.getClassificationScheme().equals(XDSConstants.UUID_XDSDocumentEntry_formatCode))
						docInfo.setFormatCode(ct.getNodeRepresentation());
				retVal.add(docInfo);
			}
		}
		
		return retVal;
	}

	/**
	 * Create a provide and register document msg
	 * @throws JAXBException 
	 */
	public ProvideAndRegisterDocumentSetRequestType createProvdeAndRegisterDocument(byte[] documentContent, final DocumentInfo info) throws JAXBException {
	    
		ProvideAndRegisterDocumentSetRequestType retVal = new ProvideAndRegisterDocumentSetRequestType();
		SubmitObjectsRequest registryRequest = new SubmitObjectsRequest();
		retVal.setSubmitObjectsRequest(registryRequest);
		
		registryRequest.setRegistryObjectList(new RegistryObjectListType());
		ExtrinsicObjectType oddRegistryObject = new ExtrinsicObjectType();
		// ODD
		oddRegistryObject.setId("Document01");
		oddRegistryObject.setMimeType("text/xml");
//		oddRegistryObject.setObjectType(XDSConstants.UUID_XDSDocumentEntry);
		oddRegistryObject.setName(new InternationalStringType());
		oddRegistryObject.getName().getLocalizedString().add(new LocalizedStringType());
		oddRegistryObject.getName().getLocalizedString().get(0).setValue(info.getTitle());
		
		// Get the earliest time something occurred and the latest
		Date lastEncounter = new Date(0),
				firstEncounter = new Date();
		
		if(info.getRelatedEncounter() != null)
			for(Obs el : info.getRelatedEncounter().getObs())
			{
				if(el.getObsDatetime().before(firstEncounter))
					firstEncounter = el.getEncounter().getVisit().getStartDatetime();
				if(el.getObsDatetime().after(lastEncounter))
					lastEncounter = el.getEncounter().getVisit().getStopDatetime();
			}
		
		TS firstEncounterTs = CdaDataUtil.getInstance().createTS(firstEncounter),
				lastEncounterTs = CdaDataUtil.getInstance().createTS(lastEncounter),
				creationTimeTs = TS.now();
		
		firstEncounterTs.setDateValuePrecision(TS.MINUTENOTIMEZONE);
		lastEncounterTs.setDateValuePrecision(TS.MINUTENOTIMEZONE);
		InfosetUtil.addOrOverwriteSlot(oddRegistryObject, XDSConstants.SLOT_NAME_SERVICE_START_TIME, firstEncounterTs.getValue());
		InfosetUtil.addOrOverwriteSlot(oddRegistryObject, XDSConstants.SLOT_NAME_SERVICE_STOP_TIME, lastEncounterTs.getValue());
		
		oddRegistryObject.setObjectType("urn:uuid:7edca82f-054d-47f2-a032-9b2a5b5186c1");
		
		// Add source patient information
		TS patientDob = CdaDataUtil.getInstance().createTS(info.getPatient().getBirthdate());
		patientDob.setDateValuePrecision(TS.DAY);
		InfosetUtil.addOrOverwriteSlot(oddRegistryObject, XDSConstants.SLOT_NAME_SOURCE_PATIENT_ID, String.format("%s^^^^&%s&ISO", info.getPatient().getId().toString(), this.m_cdaConfiguration.getPatientRoot()));
		InfosetUtil.addOrOverwriteSlot(oddRegistryObject, XDSConstants.SLOT_NAME_SOURCE_PATIENT_INFO,
			String.format("PID-3|%s", String.format("%s^^^^&%s&ISO", info.getPatient().getId().toString(), this.m_cdaConfiguration.getPatientRoot())),
			String.format("PID-5|%s^%s^^^", info.getPatient().getFamilyName(), info.getPatient().getGivenName()),
			String.format("PID-7|%s", patientDob.getValue()),
			String.format("PID-8|%s", info.getPatient().getGender())
			);
		InfosetUtil.addOrOverwriteSlot(oddRegistryObject, XDSConstants.SLOT_NAME_LANGUAGE_CODE, Context.getLocale().toLanguageTag());
		InfosetUtil.addOrOverwriteSlot(oddRegistryObject, XDSConstants.SLOT_NAME_CREATION_TIME, new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
		
		// Unique identifier
		XdsUtil.getInstance().addExtenalIdentifier(oddRegistryObject, XDSConstants.UUID_XDSDocumentEntry_uniqueId, String.format("2.25.%s", UUID.randomUUID().getLeastSignificantBits()), "XDSDocumentEntry.uniqueId");
		XdsUtil.getInstance().addExtenalIdentifier(oddRegistryObject, XDSConstants.UUID_XDSDocumentEntry_patientId, XdsUtil.getInstance().getPatientIdentifier(info.getPatient()), "XDSDocumentEntry.patientId");
		
		// Set classifications
		XdsUtil.getInstance().addCodedValueClassification(oddRegistryObject, XDSConstants.UUID_XDSDocumentEntry_classCode, info.getClassCode(), "LOINC", "XDSDocumentEntry.classCode");
		XdsUtil.getInstance().addCodedValueClassification(oddRegistryObject, XDSConstants.UUID_XDSDocumentEntry_confidentialityCode, "1.3.6.1.4.1.21367.2006.7.101", "Connect-a-thon confidentialityCodes", "XDSDocumentEntry.confidentialityCode");
		XdsUtil.getInstance().addCodedValueClassification(oddRegistryObject, XDSConstants.UUID_XDSDocumentEntry_formatCode, info.getFormatCode(), "1.3.6.1.4.1.19376.1.2.3", "XDSDocumentEntry.formatCode");
		XdsUtil.getInstance().addCodedValueClassification(oddRegistryObject, XDSConstants.UUID_XDSDocumentEntry_healthCareFacilityTypeCode, "Not Available", "Connect-a-thon healthcareFacilityTypeCodes", "XDSDocumentEntry.healthCareFacilityTypeCode");
		XdsUtil.getInstance().addCodedValueClassification(oddRegistryObject, XDSConstants.UUID_XDSDocumentEntry_practiceSettingCode, "Not Available", "Connect-a-thon practiceSettingCodes", "UUID_XDSDocumentEntry.practiceSettingCode");
		XdsUtil.getInstance().addCodedValueClassification(oddRegistryObject, XDSConstants.UUID_XDSDocumentEntry_typeCode, info.getTypeCode(), "LOINC", "XDSDocumentEntry.typeCode");
		
		// Create the submission set
		TS now = TS.now();
		now.setDateValuePrecision(TS.SECONDNOTIMEZONE);
		
		RegistryPackageType regPackage = new RegistryPackageType();
		regPackage.setId("SubmissionSet01");
		InfosetUtil.addOrOverwriteSlot(regPackage, XDSConstants.SLOT_NAME_SUBMISSION_TIME, now.getValue());
		regPackage.setName(oddRegistryObject.getName());
		XdsUtil.getInstance().addCodedValueClassification(regPackage, XDSConstants.UUID_XDSSubmissionSet_contentTypeCode, info.getClassCode(), "LOINC", "XDSSubmissionSet.contentTypeCode");
		
		// Submission set external identifiers
		XdsUtil.getInstance().addExtenalIdentifier(regPackage, XDSConstants.UUID_XDSSubmissionSet_uniqueId, String.format("2.25.%s", UUID.randomUUID().getLeastSignificantBits()), "XDSSubmissionSet.uniqueId");
		XdsUtil.getInstance().addExtenalIdentifier(regPackage, XDSConstants.UUID_XDSSubmissionSet_sourceId, String.format("2.25.%s", UUID.randomUUID().getLeastSignificantBits()), "XDSSubmissionSet.sourceId");
		XdsUtil.getInstance().addExtenalIdentifier(regPackage, XDSConstants.UUID_XDSSubmissionSet_patientId, XdsUtil.getInstance().getPatientIdentifier(info.getPatient()), "XDSSubmissionSet.patientId");
		
		// Add the eo to the submission
		registryRequest.getRegistryObjectList().getIdentifiable().add(
			new JAXBElement<ExtrinsicObjectType>(
					new QName("urn:oasis:names:tc:ebxml-regrep:xsd:rim:3.0","ExtrinsicObject"),
					ExtrinsicObjectType.class,
					oddRegistryObject
				)
			);
		
		// Add the package to the submission
		registryRequest.getRegistryObjectList().getIdentifiable().add(
			new JAXBElement<RegistryPackageType>(
					new QName("urn:oasis:names:tc:ebxml-regrep:xsd:rim:3.0","RegistryPackage"),
					RegistryPackageType.class,
					regPackage
				)
			);
		
		// Add classification for the submission set
		registryRequest.getRegistryObjectList().getIdentifiable().add(
			new JAXBElement<ClassificationType>(
					new QName("urn:oasis:names:tc:ebxml-regrep:xsd:rim:3.0", "Classification"), 
					ClassificationType.class, 
					new ClassificationType() {{
						setId("cl01");
						setClassifiedObject("SubmissionSet01");
						setClassificationNode(XDSConstants.UUID_XDSSubmissionSet);
					}}
				)
			);
		
		// Add an association
		AssociationType1 association = 	new AssociationType1();
		association.setId("as01");
		association.setAssociationType("HasMember");
		association.setSourceObject("SubmissionSet01");
		association.setTargetObject("Document01");
		InfosetUtil.addOrOverwriteSlot(association, XDSConstants.SLOT_NAME_SUBMISSIONSET_STATUS, "Original");
		registryRequest.getRegistryObjectList().getIdentifiable().add(
			new JAXBElement<AssociationType1>(
					new QName("urn:oasis:names:tc:ebxml-regrep:xsd:rim:3.0", "Association"), 
					AssociationType1.class, 
					association)
			);

		// Add author
		List<String> authors = new ArrayList<String>();

		for(Provider pvdr : info.getAuthors())
		{
			ClassificationType authorClass = new ClassificationType();
			authorClass.setClassificationScheme(XDSConstants.UUID_XDSDocumentEntry_author);
			authorClass.setClassifiedObject(oddRegistryObject.getId());
			authorClass.setId(String.format("Classification_%s", UUID.randomUUID().toString()));
			
			String authorText = String.format("%s^%s^%s^^^^^^&%s&ISO", pvdr.getId(), pvdr.getPerson().getFamilyName(), pvdr.getPerson().getGivenName(), this.m_cdaConfiguration.getProviderRoot());
			if(authors.contains(authorText))
				continue;
			else
				authors.add(authorText);
			
			InfosetUtil.addOrOverwriteSlot(authorClass, XDSConstants.SLOT_NAME_AUTHOR_PERSON, authorText);

			oddRegistryObject.getClassification().add(authorClass);
		}
					
		Document doc = new Document();
		doc.setId(oddRegistryObject.getId());
		doc.setValue(documentContent);
		retVal.getDocument().add(doc);
		
		return retVal;
    }

	/**
	 * Provide and register
	 * Auto generated method comment
	 * 
	 * @param request
	 * @return 
	 * @throws HealthInformationExchangeException 
	 */
	public RegistryResponseType sendProvideAndRegister(ProvideAndRegisterDocumentSetRequestType request) throws HealthInformationExchangeException {
		DocumentRepositoryPortType port = DocumentRepositoryPortTypeFactory.getDocumentRepositoryPortSoap12(this.m_configuration.getXdsRepositoryEndpoint());
		try
		{
			return port.documentRepositoryProvideAndRegisterDocumentSetB(request);
		}
		catch(Exception e)
		{
			log.error(e);
			throw new HealthInformationExchangeException(e);
		}
	}
	
}
