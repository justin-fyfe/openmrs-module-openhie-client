package org.openmrs.module.openhie.client.api;

import static org.junit.Assert.*;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.PersonName;
import org.openmrs.api.context.Context;
import org.openmrs.module.openhie.client.api.impl.MessageUtil;
import org.openmrs.test.BaseModuleContextSensitiveTest;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.parser.EncodingNotSupportedException;
import ca.uhn.hl7v2.parser.PipeParser;

/**
 * Message utility test
 * @author Justin
 *
 */
public class MessageUtilTest extends BaseModuleContextSensitiveTest {

	@Before
	public void before()
	{
		PatientIdentifierType pit = new PatientIdentifierType();
		pit.setName("1.2.3.4.5");
		pit.setDescription("A Test");
		Context.getPatientService().savePatientIdentifierType(pit);
		pit = new PatientIdentifierType();
		pit.setName("1.2.3.4.5.65.6.7");
		pit.setDescription("A Test");
		Context.getPatientService().savePatientIdentifierType(pit);
		pit = new PatientIdentifierType();
		pit.setName("FOO");
		pit.setDescription("A Test");
		Context.getPatientService().savePatientIdentifierType(pit);
	}
	
	/**
	 * Test the create PDQ message 
	 * @throws HL7Exception 
	 */
	@Test
	public void testCreatePdqMessageName() throws HL7Exception {
		MessageUtil util = MessageUtil.getInstance();
		Message pdqMessage = util.createPdqMessage(new HashMap<String, String>() {{
			put("@PID.5.1", "SMITH");
			put("@PID.5.2", "JOHN");
		}});
		String message = new PipeParser().encode(pdqMessage);
		assertTrue("Must have @PID.5.1^SMITH", message.contains("@PID.5.1^SMITH"));
		assertTrue("Must have @PID.5.2^JOHN", message.contains("@PID.5.2^JOHN"));
	}

	/**
	 * Test the create PDQ message 
	 * @throws HL7Exception 
	 */
	@Test
	public void testCreatePdqMessageNameGender() throws HL7Exception {
		MessageUtil util = MessageUtil.getInstance();
		Message pdqMessage = util.createPdqMessage(new HashMap<String, String>() {{
			put("@PID.5.1", "SMITH");
			put("@PID.5.2", "JOHN");
			put("@PID.7", "M");
		}});
		String message = new PipeParser().encode(pdqMessage);
		assertTrue("Must have @PID.5.1^SMITH", message.contains("@PID.5.1^SMITH"));
		assertTrue("Must have @PID.5.2^JOHN", message.contains("@PID.5.2^JOHN"));
		assertTrue("Must have @PID.7^M", message.contains("@PID.7^M"));
	}

	/**
	 * Create an Admit Message
	 * @throws HL7Exception 
	 */
	@Test
	public void testCreateAdmit() throws HL7Exception {
		Patient testPatient = new Patient();
		testPatient.setGender("F");
		testPatient.setBirthdate(new Date());
		testPatient.addName(new PersonName("John", "T", "Smith"));
		testPatient.getNames().iterator().next().setPreferred(true);
		PatientIdentifierType pit = Context.getPatientService().getPatientIdentifierTypeByName("1.2.3.4.5.65.6.7");
		testPatient.addIdentifier(new PatientIdentifier("123", pit, Context.getLocationService().getDefaultLocation()));
		pit = Context.getPatientService().getPatientIdentifierTypeByName("FOO");
		testPatient.addIdentifier(new PatientIdentifier("AD3", pit, Context.getLocationService().getDefaultLocation()));
		testPatient.setId(1203);
		
		Message admit = MessageUtil.getInstance().createAdmit(testPatient);
		String message = new PipeParser().encode(admit);
		Assert.assertTrue("Expected 123^^^&1.2.3.4.5.65.6.7&ISO", message.contains("123^^^&1.2.3.4.5.65.6.7&ISO"));
		Assert.assertTrue("Expected AD3^^^FOO", message.contains("AD3^^^FOO"));
		Assert.assertTrue("Expected Smith^John^T^^^^L", message.contains("Smith^John^T^^^^L"));
	}

	/**
	 * Test the interpretation of the PID segment
	 * @throws HL7Exception 
	 * @throws EncodingNotSupportedException 
	 */
	@Test
	public void testInterpretPidSimple() throws EncodingNotSupportedException, HL7Exception
	{
		String aMessageWithPID = "MSH|^~\\&|CR1^^|MOH_CAAT^^|TEST_HARNESS^^|TEST^^|20141104174451||RSP^K23^RSP_K21|TEST-CR-05-10|P|2.5\r" + 
								"PID|||RJ-439^^^TEST&1.2.3.4.5&ISO||JONES^JENNIFER^^^^^L|SMITH^^^^^^L|19840125|F|||123 Main Street West ^^NEWARK^NJ^30293||^PRN^PH^^^409^3049506||||||";
		
		Message mut = new PipeParser().parse(aMessageWithPID);
		List<Patient> pat = MessageUtil.getInstance().interpretPIDSegments(mut);
		
		Assert.assertEquals(1, pat.size());
		Assert.assertEquals("RJ-439", pat.get(0).getIdentifiers().iterator().next().getIdentifier());
		Assert.assertEquals("1.2.3.4.5", pat.get(0).getIdentifiers().iterator().next().getIdentifierType().getName());
		Assert.assertEquals("F", pat.get(0).getGender());
		Assert.assertEquals(false, pat.get(0).getBirthdateEstimated());
	}
	
	/**
	 * Test the interpretation of the PID segment
	 * @throws HL7Exception 
	 * @throws EncodingNotSupportedException 
	 */
	@Test
	public void testInterpretPidMultiIdentifier() throws EncodingNotSupportedException, HL7Exception
	{
		String aMessageWithPID = "MSH|^~\\&|CR1^^|MOH_CAAT^^|TEST_HARNESS^^|TEST^^|20141104174451||RSP^K23^RSP_K21|TEST-CR-05-10|P|2.5\r" + 
								"PID|||RJ-439^^^TEST&1.2.3.4.5&ISO~TEST-222^^^FOO||JONES^JENNIFER^^^^^L|SMITH^^^^^^L|19840125|F|||123 Main Street West ^^NEWARK^NJ^30293||^PRN^PH^^^409^3049506||||||";
		
		Message mut = new PipeParser().parse(aMessageWithPID);
		List<Patient> pat = MessageUtil.getInstance().interpretPIDSegments(mut);
		Assert.assertEquals(1, pat.size());
		Assert.assertEquals(2, pat.get(0).getIdentifiers().size());
	}

	/**
	 * Test the interpretation of the PID segment
	 * @throws HL7Exception 
	 * @throws EncodingNotSupportedException 
	 */
	@Test
	public void testInterpretPidMultiResults() throws EncodingNotSupportedException, HL7Exception
	{
		String aMessageWithPID = "MSH|^~\\&|CR1^^|MOH_CAAT^^|TEST_HARNESS^^|TEST^^|20141104174451||RSP^K23^RSP_K21|TEST-CR-05-10|P|2.5\r" + 
								"PID|||RJ-439^^^TEST&1.2.3.4.5&ISO||JONES^JENNIFER^^^^^L|SMITH^^^^^^L|19840125|F|||123 Main Street West ^^NEWARK^NJ^30293||^PRN^PH^^^409^3049506||||||\r" +
								"PID|||RJ-442^^^FOO||FOSTER^FANNY^FULL^^^^L|FOSTER^MARY^^^^^L|1970|F|||123 W34 St^^FRESNO^CA^3049506||^PRN^PH^^^419^31495|^^PH^^^034^059434|EN|S|||||\r";
		
		Message mut = new PipeParser().parse(aMessageWithPID);
		List<Patient> pat = MessageUtil.getInstance().interpretPIDSegments(mut);
		Assert.assertEquals(2, pat.size());
	}

}
