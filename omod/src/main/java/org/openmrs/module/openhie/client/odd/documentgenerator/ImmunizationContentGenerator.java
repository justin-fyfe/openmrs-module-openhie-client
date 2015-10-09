package org.openmrs.module.openhie.client.odd.documentgenerator;

import org.marc.everest.datatypes.BL;
import org.marc.everest.datatypes.II;
import org.marc.everest.datatypes.generic.CE;
import org.marc.everest.datatypes.generic.LIST;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.ClinicalDocument;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Component2;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.StructuredBody;
import org.marc.everest.rmim.uv.cdar2.vocabulary.ActRelationshipHasComponent;
import org.openmrs.module.shr.cdahandler.CdaHandlerConstants;
import org.openmrs.module.shr.odd.generator.DocumentGenerator;
import org.openmrs.module.shr.odd.generator.document.impl.DocumentGeneratorImpl;
import org.openmrs.module.shr.odd.generator.section.impl.ImmunizationsSectionGenerator;
import org.openmrs.module.shr.odd.model.OnDemandDocumentRegistration;

public class ImmunizationContentGenerator extends DocumentGeneratorImpl implements DocumentGenerator {
	// Document code
		private final CE<String> m_documentCode = new CE<String>("11369-6", CdaHandlerConstants.CODE_SYSTEM_LOINC, CdaHandlerConstants.CODE_SYSTEM_NAME_LOINC, null, "Immunization History", null);
				
		/**
		 * Generate the CCD
		 * @see org.openmrs.module.shr.odd.generator.DocumentGenerator#generateDocument(org.openmrs.module.shr.odd.model.OnDemandDocumentRegistration)
		 */
		public ClinicalDocument generateDocument(OnDemandDocumentRegistration oddRegistration) {
			ClinicalDocument retVal = super.createHeader(oddRegistration);
			
			// CCD CONF-1
			retVal.setCode(m_documentCode);
			retVal.setTitle(retVal.getCode().getDisplayName());
			// CCD Template ID (CCD CONF-7 & CONF-8)
			retVal.setTemplateId(LIST.createLIST(
				new II(CdaHandlerConstants.DOC_TEMPLATE_IMMUNIZATION_CONTENT),
				new II(CdaHandlerConstants.DOC_TEMPLATE_CDA4CDT)
			));
			
			
			// CCD body must be structured
			retVal.setComponent(new Component2(ActRelationshipHasComponent.HasComponent, BL.TRUE));
			retVal.getComponent().setBodyChoice(new StructuredBody());
			
			// Now add the required sections
			super.generateSections(oddRegistration,
				retVal,
				ImmunizationsSectionGenerator.class
			);
			
			// retVal.getComponent().getBodyChoiceIfStructuredBody().getComponent().addAll(sections);
			
			return retVal;
		}

		/**
		 * Get the document type code
		 * @see org.openmrs.module.shr.odd.generator.DocumentGenerator#getDocumentTypeCode()
		 */
	    public CE<String> getDocumentTypeCode() {
		    return this.m_documentCode;
	    }

		public CE<String> getFormatCode() {
			return new CE<String>("urn:ihe:pcc:ic:2009", "1.3.6.1.4.1.19376.1.2.3");
		}
		
}
