package org.openmrs.module.openhie.client.cda.document.impl;

import org.marc.everest.datatypes.II;
import org.marc.everest.datatypes.generic.CE;
import org.marc.everest.datatypes.generic.LIST;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.ClinicalDocument;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Section;
import org.openmrs.module.openhie.client.CdaHandlerConstants;

/**
 * Antepartum Summary document builder
 * @author JustinFyfe
 *
 */
public class ApsDocumentBuilder extends DocumentBuilderImpl {

	/**
	 * Get the document type code
	 * @see org.openmrs.module.shr.odd.generator.DocumentGenerator#getDocumentTypeCode()
	 */
	@Override
    public String getTypeCode() {
		return "57055-6";
    }

	/**
	 * Get the document format code
	 * @see org.openmrs.module.shr.odd.generator.DocumentGenerator#getFormatCode()
	 */
    @Override
	public String getFormatCode() {
		return "1.3.6.1.4.1.19376.1.5.3.1.1.11.2";
	}
	
	/**
	 * Generate the APS summary
	 */
	@Override
	public ClinicalDocument generate(Section... sections) {
		// TODO Auto-generated method stub
		ClinicalDocument retVal = super.generate(sections);
		retVal.setTemplateId(LIST.createLIST(new II(CdaHandlerConstants.DOC_TEMPLATE_MEDICAL_SUMMARY), new II(CdaHandlerConstants.DOC_TEMPLATE_ANTEPARTUM_SUMMARY)));
		retVal.setCode(new CE<String>("57055-6", CdaHandlerConstants.CODE_SYSTEM_LOINC, CdaHandlerConstants.CODE_SYSTEM_NAME_LOINC, null, "Antepartum Summary Note", null));
		retVal.setTitle("Antepartum Summary");
		return retVal;
	}

	
}
