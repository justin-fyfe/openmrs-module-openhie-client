package org.openmrs.module.openhie.client.api;

import static org.junit.Assert.*;

import java.util.Date;
import java.util.HashMap;

import org.junit.Assert;
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
import ca.uhn.hl7v2.parser.PipeParser;

/**
 * Message utility test
 * @author Justin
 *
 */
public class MessageUtilTest extends BaseModuleContextSensitiveTest {

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
		PatientIdentifierType pit = new PatientIdentifierType();
		pit.setName("1.2.3.4.5.65.6.7");
		testPatient.addIdentifier(new PatientIdentifier("123", pit, Context.getLocationService().getDefaultLocation()));
		pit = new PatientIdentifierType();
		pit.setName("FOO");
		testPatient.addIdentifier(new PatientIdentifier("AD3", pit, Context.getLocationService().getDefaultLocation()));
		testPatient.setId(1203);
		
		Message admit = MessageUtil.getInstance().createAdmit(testPatient);
		String message = new PipeParser().encode(admit);
		Assert.assertTrue("Expected 123^^^&1.2.3.4.5.65.6.7&ISO", message.contains("123^^^&1.2.3.4.5.65.6.7&ISO"));
		Assert.assertTrue("Expected AD3^^^FOO", message.contains("AD3^^^FOO"));
		Assert.assertTrue("Expected Smith^John^T^^^^L", message.contains("Smith^John^T^^^^L"));
	}

}
