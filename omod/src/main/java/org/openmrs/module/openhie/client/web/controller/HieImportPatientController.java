package org.openmrs.module.openhie.client.web.controller;

import java.text.ParseException;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.openhie.client.api.HealthInformationExchangeService;
import org.openmrs.module.openhie.client.configuration.CdaHandlerConfiguration;
import org.openmrs.module.openhie.client.exception.HealthInformationExchangeException;
import org.openmrs.module.openhie.client.web.model.PatientSearchModel;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/module/openhie-client/hieImportPatient")
public class HieImportPatientController {

	protected final Log log = LogFactory.getLog(this.getClass());
	/**
	 * Handle the get operation
	 * @param model
	 */
	@RequestMapping(method = RequestMethod.GET)
	public ModelAndView index(ModelMap model,  @RequestParam(value = "ecid") String ecid) {

		if(ecid == null)
			throw new IllegalArgumentException("ecid must be supplied");
		
		try
		{
			HealthInformationExchangeService service = Context.getService(HealthInformationExchangeService.class);
			CdaHandlerConfiguration config = CdaHandlerConfiguration.getInstance();
			model.put("patient", service.getPatient(ecid, config.getEcidRoot()));
			return new ModelAndView("/module/openhie-client/hieImportPatient", model);
		}
		catch(HealthInformationExchangeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			model.put("error", e.getMessage());
			return new ModelAndView("/module/openhie-client/hieImportPatient", model);
		}
	}
	
	/**
	 * Handle the post
	 * @return 
	 * @throws ParseException 
	 */
	@RequestMapping(method = RequestMethod.POST)
	public ModelAndView doImport(Map<String, Object> model, @ModelAttribute("importPatient") PatientSearchModel search, @RequestParam(value = "ecid") String ecid) throws ParseException
	{
		if(ecid == null)
			throw new IllegalArgumentException("ecid must be supplied");
		
		try
		{
			// Service for the HIE
			HealthInformationExchangeService service = Context.getService(HealthInformationExchangeService.class);
			CdaHandlerConfiguration config = CdaHandlerConfiguration.getInstance();
			Patient pat = service.getPatient(ecid, config.getEcidRoot());
			pat = service.importPatient(pat);
			return new ModelAndView("redirect:/patientDashboard.form?patientId=" + pat.getId().toString() );
		}
		catch(HealthInformationExchangeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			model.put("error", e.getMessage());
			return new ModelAndView("/module/openhie-client/hieImportPatient", model);
		}
	}
}
