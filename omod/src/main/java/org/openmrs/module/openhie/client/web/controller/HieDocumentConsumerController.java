package org.openmrs.module.openhie.client.web.controller;

import java.io.InputStream;
import java.io.StringWriter;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.api.context.Context;
import org.openmrs.module.openhie.client.api.HealthInformationExchangeService;
import org.openmrs.module.openhie.client.exception.HealthInformationExchangeException;
import org.openmrs.module.openhie.client.hie.model.DocumentInfo;
import org.openmrs.module.openhie.client.web.model.Document;
import org.openmrs.module.shr.cdahandler.configuration.CdaHandlerConfiguration;
import org.openmrs.module.shr.cdahandler.configuration.CdaHandlerConfigurationFactory;
import org.openmrs.web.controller.PortletController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Portlet controller for the HIE Patient (ODD) summary if available
 * @author Justin
 */
@Controller
@RequestMapping("/hieDocumentConsumer")
public class HieDocumentConsumerController extends PortletController {

	protected static Log log = LogFactory.getLog(HieDocumentConsumerController.class);
	// CDA Handler configuration
	protected final CdaHandlerConfiguration m_cdaConfiguration = CdaHandlerConfigurationFactory.getInstance();
	
	/**
	 * Populate the model with the On-Demand Document Data
	 */
	@Override
	protected void populateModel(HttpServletRequest request,
			Map<String, Object> model) {
		HealthInformationExchangeService hieService = Context.getService(HealthInformationExchangeService.class);
		// TODO Auto-generated method stub
		Object pidFromModel = model.get("patientId");
		Integer pid = pidFromModel instanceof Integer ? (Integer)pidFromModel : Integer.parseInt(pidFromModel.toString());
		Patient patient = Context.getPatientService().getPatient(pid);
		
		// Get the patient record from the HIE
		try {
			Patient patientInfo = hieService.getPatient(pidFromModel.toString(), this.m_cdaConfiguration.getPatientRoot());
			model.put("isRegistered", patientInfo != null);
			model.put("hiePatientInfo", patientInfo);
			if(patientInfo != null)
			{
				// query for the ODD document
				List<DocumentInfo> results = hieService.queryDocuments(patientInfo, true, (Date)null, "2.16.840.1.113883.10.20.1", "HL7");
				if(results.size() > 0)
				{
					byte[] documentContent = hieService.fetchDocument(results.get(0));
					model.put("document", Document.createInstance(documentContent));
				}
			}
		} catch (HealthInformationExchangeException e) {
			log.error(e);
		}
		super.populateModel(request, model);
	}
	
	
}
