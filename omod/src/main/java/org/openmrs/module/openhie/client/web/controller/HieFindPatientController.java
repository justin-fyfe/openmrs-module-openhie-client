package org.openmrs.module.openhie.client.web.controller;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.api.context.Context;
import org.openmrs.module.openhie.client.api.HealthInformationExchangeService;
import org.openmrs.module.openhie.client.exception.HealthInformationExchangeException;
import org.openmrs.module.openhie.client.web.model.PatientResultModel;
import org.openmrs.module.openhie.client.web.model.PatientSearchModel;
import org.openmrs.web.controller.PortletController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.ModelAndView;

/**
 * Find Patient in the HIE Controller
 * @author Justin
 *
 */
@Controller
@RequestMapping("/module/openhie-client/hieFindPatient")
@SessionAttributes("patientSearch")
public class HieFindPatientController {

	protected final Log log = LogFactory.getLog(this.getClass());
	/**
	 * Handle the get operation
	 * @param model
	 */
	@RequestMapping(method = RequestMethod.GET)
	public void index(ModelMap model) {
		if(model.get("patientSearch") == null)
			model.put("patientSearch", new PatientSearchModel());
	}
	
	/**
	 * Handle the post
	 * @return 
	 * @throws ParseException 
	 */
	@RequestMapping(method = RequestMethod.POST)
	public ModelAndView doSearch(Map<String, Object> model, @ModelAttribute("patientSearch") PatientSearchModel search) throws ParseException
	{
		// Service for the HIE
		HealthInformationExchangeService service = Context.getService(HealthInformationExchangeService.class);
		Date dobDate = null;
		boolean isFuzzy = false;
		PatientIdentifier identifier = null,
				momsIdentifier = null;
		
		// Date format
		if(search.getDateOfBirth() != null && !search.getDateOfBirth().isEmpty())
		{
			dobDate = new SimpleDateFormat("yyyyMMdd".substring(0, search.getDateOfBirth().length())).parse(search.getDateOfBirth());
			isFuzzy = search.getDateOfBirth().length() < 8;
		}
		if(search.getIdentifier() != null && !search.getIdentifier().isEmpty())
		{
			if(search.getMomsId() != null && search.getMomsId().equals("true"))
				momsIdentifier = new PatientIdentifier(search.getIdentifier(), null, null);
			else
				identifier = new PatientIdentifier(search.getIdentifier(), null, null);
			
		}
		
		try {
			List<Patient> results = service.searchPatient(search.getFamilyName(), search.getGivenName(), dobDate, isFuzzy, search.getGender(), identifier, momsIdentifier);
			List<PatientResultModel> modelResult = new ArrayList<PatientResultModel>();
			for(Patient result : results)
				modelResult.add(new PatientResultModel(result));
			
			model.put("hasResults", modelResult.size() > 0);
			model.put("results", modelResult);
			model.put("patientSearch", search);
			
			return new ModelAndView("/module/openhie-client/hieFindPatient", model);
		} catch (HealthInformationExchangeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			model.put("error", e.getMessage());
			return new ModelAndView("/module/openhie-client/hieFindPatient", model);
		}
		
	}
	
}
