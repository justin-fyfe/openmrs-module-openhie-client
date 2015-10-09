package org.openmrs.module.openhie.client.extension.html;

import org.openmrs.api.context.Context;
import org.openmrs.module.web.extension.PatientDashboardTabExt;


public class VaccinationInformationTab extends PatientDashboardTabExt {

	/**
	 * Get the portlet URL
	 */
	@Override
	public String getPortletUrl() {
		return "vaccinationTab";
	}

	/**
	 * Get privilege
	 */
	@Override
	public String getRequiredPrivilege() {
		return "View Encounters";
	}

	/**
	 * Get the TAB ID
	 */
	@Override
	public String getTabId() {
		return "vaccinationTab";
	}

	/**
	 * Get the tab name
	 */
	@Override
	public String getTabName() {
		return Context.getMessageSourceService().getMessage("openhie-client.vaccinations.header");
	}

}