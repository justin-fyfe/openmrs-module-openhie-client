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

public final class Document {
			
	private String m_html;
	
	private static Log log = LogFactory.getLog(Document.class);
	/**
	 * Can only be created by static method
	 */
	private Document() {
		
	}
	/**
	 * Transform the CDA to XML
	 * @param in
	 * @throws TransformerException
	 */
	public static Document createInstance(byte[] documentData) {
		InputStream in = null;
		try
		{
			in = new ByteArrayInputStream(documentData);
			TransformerFactory factory = TransformerFactory.newInstance();
			Source xslt = new StreamSource(Document.class.getClassLoader().getResourceAsStream("cda.xsl"));
			Transformer transformer = factory.newTransformer(xslt);
			
			Source text = new StreamSource(in);
			StringWriter sw = new StringWriter();
			transformer.transform(text, new StreamResult(sw));
			Document retVal = new Document();
			retVal.m_html = sw.toString();
			retVal.applyFormatting();
			log.error(retVal.m_html);
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
