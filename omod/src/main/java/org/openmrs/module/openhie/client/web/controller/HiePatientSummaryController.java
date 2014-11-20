package org.openmrs.module.openhie.client.web.controller;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.LogFactory;
import org.openmrs.web.controller.PortletController;

/**
 * Portlet controller for the HIE Patient (ODD) summary if available
 * @author Justin
 */
public class HiePatientSummaryController extends PortletController {

	protected static Log log = LogFactory.getLog(HiePatientSummaryController.class);

	/**
	 * Populate the model with the On-Demand Document Data
	 */
	@Override
	protected void populateModel(HttpServletRequest request,
			Map<String, Object> model) {
		// TODO Auto-generated method stub
		super.populateModel(request, model);
	}
	
	
	
}
