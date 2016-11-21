package org.openmrs.module.openhie.client.web.model;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.ClinicalDocument;

public final class DocumentModel {
			
	private String m_html;
	private ClinicalDocument m_doc;
	private String m_formatCode;
	private String m_typeCode;
	private byte[] m_data;
	
	private static Log log = LogFactory.getLog(DocumentModel.class);
	/**
	 * Can only be created by static method
	 */
	private DocumentModel() {
		
	}

	public String getFormatCode() { return this.m_formatCode; }
	public String getTypeCode() { return this.m_typeCode; }
	public ClinicalDocument getDocument() { return this.m_doc; }
	public byte[] getData() { return this.m_data; }
	
	/**
	 * Create representation on bare data
	 * @param documentData
	 * @return
	 */
	public static DocumentModel createInstance(byte[] documentData) {
		return createInstance(documentData, null, null, null);
	}
	
	/**
	 * Transform the CDA to XML
	 * @param in
	 * @throws TransformerException
	 */
	public static DocumentModel createInstance(byte[] documentData, String typeCode, String formatCode, ClinicalDocument doc) {
		InputStream in = null;
		try
		{
			in = new ByteArrayInputStream(documentData);
			TransformerFactory factory = TransformerFactory.newInstance();
			Source xslt = new StreamSource(DocumentModel.class.getClassLoader().getResourceAsStream("cda.xsl"));
			Transformer transformer = factory.newTransformer(xslt);
			
			Source text = new StreamSource(in);
			StringWriter sw = new StringWriter();
			transformer.transform(text, new StreamResult(sw));
			DocumentModel retVal = new DocumentModel();
			retVal.m_html = sw.toString();
			retVal.applyFormatting();
			log.error(retVal.m_html);
			retVal.m_typeCode = typeCode;
			retVal.m_formatCode = formatCode;
			retVal.m_doc = doc;
			retVal.m_data = documentData;
			return retVal;
		} catch (TransformerException e) {
			log.error(e);
			return null;
		}
		finally
		{
			if(in != null)
				try {
					in.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			
		}
	}
	
	public void applyFormatting() {
		m_html = m_html.substring(m_html.indexOf("<body>") + "<body>".length());
		m_html = m_html.substring(0, m_html.indexOf("</body>"));
	}
	
	public String getHtml() {
		return m_html;
	}

}
