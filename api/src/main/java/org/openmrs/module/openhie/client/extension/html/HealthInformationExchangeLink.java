package org.openmrs.module.openhie.client.extension.html;

import org.openmrs.api.context.Context;
import org.openmrs.module.web.extension.LinkExt;

/**
 * Link to the end of OpenMRS which allows access to HIE data
 * @author Justin
 *
 */
public class HealthInformationExchangeLink extends LinkExt {

	/**
	 * Get the label of the extension
	 */
	@Override
	public String getLabel() {
		return Context.getMessageSourceService().getMessage("openhie-client.hiePortlet.linkText");
	}

	/**
	 * Get the required priv.
	 */
	@Override
	public String getRequiredPrivilege() {
		return "Create Patient";
	}

	/**
	 * Get the url that this portlet will point to
	 */
	@Override
	public String getUrl() {
		return "module/openhie-client/findPatient.form";
	}

}
