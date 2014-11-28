/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.openhie.client;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.module.ModuleActivator;
import org.openmrs.module.shr.cdahandler.processor.document.impl.ihe.pcc.MedicalSummaryDocumentProcessor;
import org.openmrs.module.shr.cdahandler.processor.entry.impl.ihe.pcc.ImmunizationEntryProcessor;
import org.openmrs.module.shr.cdahandler.processor.entry.impl.ihe.pcc.MedicationsEntryProcessor;
import org.openmrs.module.shr.cdahandler.processor.entry.impl.ihe.pcc.NormalDosingMedicationsEntryProcessor;
import org.openmrs.module.shr.cdahandler.processor.entry.impl.ihe.pcc.TaperedDosingMedicationsEntryProcessor;
import org.openmrs.module.shr.cdahandler.processor.factory.impl.ClasspathScannerUtil;
import org.openmrs.module.shr.cdahandler.processor.section.impl.ihe.pcc.ImmunizationsSectionProcessor;
import org.openmrs.module.shr.cdahandler.processor.section.impl.ihe.pcc.MedicationsSectionProcessor;

/**
 * This class contains the logic that is run every time this module is either started or shutdown
 */
public class HealthInformationExchangeClientActivator implements ModuleActivator {
	
	// Log
	private Log log = LogFactory.getLog(this.getClass());
	
	/**
	 * @see ModuleActivator#willRefreshContext()
	 */
	public void willRefreshContext() {
		log.info("Refreshing HIE Interface Module");
	}
	
	/**
	 * @see ModuleActivator#contextRefreshed()
	 */
	public void contextRefreshed() {
		log.info("HIE Interface Module refreshed");
	}
	
	/**
	 * @see ModuleActivator#willStart()
	 */
	public void willStart() {
		log.info("Starting HIE Interface Module");
	}
	
	/**
	 * @see ModuleActivator#started()
	 */
	public void started() {
		ClasspathScannerUtil util = ClasspathScannerUtil.getInstance();
		util.registerProcessor(ImmunizationsSectionProcessor.class);
		util.registerProcessor(MedicationsSectionProcessor.class);
		util.registerProcessor(ImmunizationEntryProcessor.class);
		util.registerProcessor(MedicationsEntryProcessor.class);
		util.registerProcessor(NormalDosingMedicationsEntryProcessor.class);
		util.registerProcessor(TaperedDosingMedicationsEntryProcessor.class);
		
		log.info("HIE Interface Module started");
	}
	
	/**
	 * @see ModuleActivator#willStop()
	 */
	public void willStop() {
		log.info("Stopping HIE Interface Module");
	}
	
	/**
	 * @see ModuleActivator#stopped()
	 */
	public void stopped() {
		log.info("HIE Interface Module stopped");
	}
	
}
