package org.openmrs.module.openhie.client.aop;

import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.openhie.client.api.HealthInformationExchangeService;
import org.openmrs.module.openhie.client.exception.HealthInformationExchangeException;
import org.openmrs.module.shr.cdahandler.configuration.CdaHandlerConfiguration;
import org.springframework.aop.AfterReturningAdvice;

/**
 * After returning from the save method of the Patient service
 */
public class PatientUpdateAdvice implements AfterReturningAdvice {
	
	private final Log log = LogFactory.getLog(this.getClass());

	private final CdaHandlerConfiguration m_configuration = CdaHandlerConfiguration.getInstance();
	
	/**
	 * Runs everytime a patient a updated
	 * @see org.springframework.aop.AfterReturningAdvice#afterReturning(java.lang.Object, java.lang.reflect.Method, java.lang.Object[], java.lang.Object)
	 */
	public void afterReturning(Object returnValue, Method method, Object[] args, Object target) throws Throwable {
		if(method.getName().equals("savePatient") && target instanceof PatientService)
		{
			log.debug("Sending update to the HIE for new patient data...");
			try
			{
				Patient patient = (Patient)returnValue;
				// Does this patient have an ECID? 
				boolean hasEcid = false;
				for(PatientIdentifier pid : patient.getIdentifiers())
					hasEcid |= pid.getIdentifierType().getName().equals(this.m_configuration.getEcidRoot());
						
				HealthInformationExchangeService hieService = Context.getService(HealthInformationExchangeService.class);
				if(hasEcid)
					hieService.updatePatient(patient);
				else
				{
					hieService.exportPatient(patient);
					PatientIdentifier pid = hieService.resolvePatientIdentifier(patient, this.m_configuration.getEcidRoot());
					patient.addIdentifier(pid);
				}
			}
			catch(HealthInformationExchangeException e)
			{
				log.error(e);
			}
		}
	}
	
}
