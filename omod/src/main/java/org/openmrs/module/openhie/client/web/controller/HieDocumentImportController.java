package org.openmrs.module.openhie.client.web.controller;

import java.text.ParseException;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.openhie.client.api.HealthInformationExchangeService;
import org.openmrs.module.openhie.client.exception.HealthInformationExchangeException;
import org.openmrs.module.openhie.client.hie.model.DocumentInfo;
import org.openmrs.module.openhie.client.web.model.DocumentModel;
import org.openmrs.module.openhie.client.web.model.PatientSearchModel;
import org.openmrs.module.shr.cdahandler.configuration.CdaHandlerConfiguration;
import org.openmrs.web.controller.PortletController;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

/**
 * HIE Document Import controller
 * @author JustinFyfe
 *
 */
@Transactional
@RequestMapping("/module/openhie-client/hieImportDocument")
public class HieDocumentImportController {

	protected final Log log = LogFactory.getLog(this.getClass());
	/**
	 * Handle the get operation
	 * @param model
	 */
	@RequestMapping(method = RequestMethod.GET)
	public ModelAndView index(ModelMap model,  @RequestParam(value = "uuid") String uuid, @RequestParam(value="rep") String rep) {
		
		
		if(uuid == null)
			throw new IllegalArgumentException("uuid must be supplied");
		
		try
		{
			HealthInformationExchangeService service = Context.getService(HealthInformationExchangeService.class);
			CdaHandlerConfiguration config = CdaHandlerConfiguration.getInstance();
			DocumentInfo docInfo = new DocumentInfo();
			docInfo.setUniqueId(uuid);
			docInfo.setRepositoryId(rep);
			
			model.put("document", DocumentModel.createInstance(service.fetchDocument(docInfo)));
			return new ModelAndView("/module/openhie-client/hieImportDocument", model);
		}
		catch(HealthInformationExchangeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			model.put("error", e.getMessage());
			return new ModelAndView("/module/openhie-client/hieImportDocument", model);
		}
	}
	
	/**
	 * Handle the post
	 * @return 
	 * @throws ParseException 
	 */
	@RequestMapping(method = RequestMethod.POST)
	public ModelAndView doImport(Map<String, Object> model, @RequestParam(value = "uuid") String uuid, @RequestParam(value="rep") String rep) throws ParseException
	{
		HealthInformationExchangeService hieService = Context.getService(HealthInformationExchangeService.class);
		DocumentInfo docInfo = new DocumentInfo();
		docInfo.setUniqueId(uuid);
		docInfo.setRepositoryId(rep);
		try {
			hieService.importDocument(docInfo);
			model.clear();
			return new ModelAndView("/module/openhie-client/hieImportDocument", model);
		} catch (HealthInformationExchangeException e) {
			// TODO Auto-generated catch block
			return new ModelAndView("/module/openhie-client/hieImportDocument", model);
		}
	}
	
}
