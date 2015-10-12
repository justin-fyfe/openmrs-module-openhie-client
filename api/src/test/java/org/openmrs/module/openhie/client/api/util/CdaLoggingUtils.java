package org.openmrs.module.openhie.client.api.util;

import java.io.ByteArrayOutputStream;

import org.marc.everest.formatters.interfaces.IFormatterGraphResult;
import org.marc.everest.formatters.xml.its1.XmlIts1Formatter;
import org.marc.everest.interfaces.IResultDetail;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.ClinicalDocument;
import org.openmrs.module.shr.cdahandler.everest.EverestUtil;

public class CdaLoggingUtils {
	
	/**
	 * Get the clinical document as a string
	 */
	public static final String getCdaAsString(ClinicalDocument document)
	{
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		
		XmlIts1Formatter fmtr = EverestUtil.createFormatter();
		IFormatterGraphResult result = fmtr.graph(bos, document);
		for(IResultDetail dtl : result.getDetails())
			if(dtl.getException() != null)
				dtl.getException().printStackTrace();
		return new String(bos.toByteArray());
		
	}
	
}
