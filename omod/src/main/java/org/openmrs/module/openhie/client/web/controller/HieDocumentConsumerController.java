package org.openmrs.module.openhie.client.web.controller;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.marc.everest.formatters.interfaces.IXmlStructureFormatter;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.ClinicalDocument;
import org.openmrs.Patient;
import org.openmrs.Visit;
import org.openmrs.api.context.Context;
import org.openmrs.module.openhie.client.api.HealthInformationExchangeService;
import org.openmrs.module.openhie.client.exception.HealthInformationExchangeException;
import org.openmrs.module.openhie.client.hie.model.DocumentInfo;
import org.openmrs.module.openhie.client.web.model.Document;
import org.openmrs.module.shr.cdahandler.CdaImporter;
import org.openmrs.module.shr.cdahandler.configuration.CdaHandlerConfiguration;
import org.openmrs.module.shr.cdahandler.everest.EverestUtil;
import org.openmrs.module.shr.cdahandler.exception.DocumentImportException;
import org.openmrs.web.controller.PortletController;
import org.springframework.transaction.annotation.Transactional;

/**
 * Portlet controller for the HIE Patient (ODD) summary if available
 * @author Justin
 */
@Transactional
public class HieDocumentConsumerController extends PortletController {

	protected static Log log = LogFactory.getLog(HieDocumentConsumerController.class);
	// CDA Handler configuration
	protected final CdaHandlerConfiguration m_cdaConfiguration = CdaHandlerConfiguration.getInstance();

	
	/**
	 * Populate the model with the On-Demand Document Data
	 */
	@Override
	protected void populateModel(HttpServletRequest request,
			Map<String, Object> model) {
		log.debug("Populating model");
		HealthInformationExchangeService hieService = Context.getService(HealthInformationExchangeService.class);
		// TODO Auto-generated method stub
		Object pidFromModel = model.get("patientId");
		Integer pid = pidFromModel instanceof Integer ? (Integer)pidFromModel : Integer.parseInt(pidFromModel.toString());
		Patient patient = Context.getPatientService().getPatient(pid);
		
		// Get the patient record from the HIE
		try {
			DocumentInfo docInfo = new DocumentInfo();
			if(request.getMethod().equals("POST") && request.getParameter("documentId") != null)
			{
				docInfo.setUniqueId(request.getParameter("documentId"));
				docInfo.setRepositoryId(request.getParameter("repositoryId"));
				hieService.importDocument(docInfo);
				model.put("wasImported", true);
			}
			else
			{
				// Now we have to look for documents
				List<DocumentInfo> results = hieService.queryDocuments(patient, true, null, null, null);
				if(results.size() > 0)
					docInfo = results.get(0);
			}
			
			model.put("documentUid", docInfo.getUniqueId());
			model.put("repositoryId", docInfo.getRepositoryId());
			byte[] documentContent = hieService.fetchDocument(docInfo);
			model.put("document", Document.createInstance(documentContent));
			
		} catch (Exception e) {
			model.put("error", e.getMessage());
			log.error(e);
		}
	}
	
	
}
