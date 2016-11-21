<%@ include file="/WEB-INF/template/include.jsp"%>
<%@ include file="/WEB-INF/template/header.jsp"%>

<spring:htmlEscape defaultHtmlEscape="true" />
<openmrs:require privilege="View Patients" otherwise="/login.htm" redirect="/index.htm" />
<openmrs:htmlInclude file="/scripts/jquery/dataTables/css/dataTables_jui.css"/>
<openmrs:htmlInclude file="/scripts/jquery/dataTables/js/jquery.dataTables.min.js"/>
<openmrs:htmlInclude file="/scripts/jquery-ui/js/openmrsSearch.js" />

<h2>Export HIE Document (Step 1 of 2)</h2>
<form id="importForm" method="get"
			enctype="multipart/form-data">
	<input type="hidden" name="pid" value="${patient.id}"/>
	<div class="boxHeader"><openmrs:message code="Patient.title"/></div>
	<div class="box$">
		<table class="personName">
			<thead>
				<tr class="patientDemographicsHeaderRow">
					<th class="patientDemographicsPersonNameHeader"><openmrs:message code="Person.names"/></th>
					<openmrs:forEachDisplayAttributeType personType="patient" displayType="viewing" var="attrType">
						<th class="patientDemographicsPersonAttTypeHeader"><openmrs:message code="PersonAttributeType.${fn:replace(attrType.name, ' ', '')}" text="${attrType.name}"/></th>
					</openmrs:forEachDisplayAttributeType>
				</tr>
			</thead>
			<tbody>
				<tr class="patientDemographicsRow">
					<td valign="top" class="patientDemographicsData" >
						<c:forEach var="name" items="${patient.names}" varStatus="status">
							<c:if test="${!name.voided}">
								<% request.setAttribute("name", pageContext.getAttribute("name")); %>
								<spring:nestedPath path="name">
									<openmrs:portlet url="nameLayout" id="namePortlet" size="quickView" parameters="layoutShowExtended=true" />
								</spring:nestedPath>
							</c:if>
						</c:forEach>
					</td>
					<openmrs:forEachDisplayAttributeType personType="patient" displayType="viewing" var="attrType">
						<td valign="top" class="patientDemographicsAttrName">${patient.attributeMap[attrType.name]}</td>
					</openmrs:forEachDisplayAttributeType>
				</tr>
			</tbody>
		</table>
	</div>
	
	<br/>
	
	<div class="boxHeader"><openmrs:message code="Person.addresses"/></div>
	<div class="box">
		<table class="personAddress">
			<thead>
				<openmrs:portlet url="addressLayout" id="addressPortlet" size="columnHeaders" parameters="layoutShowTable=false|layoutShowExtended=true" />
			</thead>
			<tbody>
				<c:forEach var="address" items="${patient.addresses}" varStatus="status">
					<c:if test="${!address.voided}">
					<% request.setAttribute("address", pageContext.getAttribute("address")); %>
					<spring:nestedPath path="address">
						<openmrs:portlet url="addressLayout" id="addressPortlet" size="inOneRow" parameters="layoutMode=view|layoutShowTable=false|layoutShowExtended=true" />
					</spring:nestedPath>
					</c:if>
				</c:forEach>
			</tbody>
		</table>
	</div>

	<p>Please select the encounter to export (or "Extract") and select an export format</p>

	<fieldset>
		<legend>Export Data</legend>
		<label for="encid">Encounter:</label>
		<select size="1" name="encid" id="encid">
			<option value="0">Extract (most recent data from all encounters)</option>
			<optgroup label="Encounters">
				<c:forEach var="enc" items="${encounters}">
					<option value="${enc.id }">Encounter by ${enc.creator.familyName }, ${enc.creator.givenName } on ${enc.dateCreated }</option>
				</c:forEach>
			</optgroup>
		</select>
	</fieldset>
	<fieldset>
		<legend>Export Template</legend>
		<label for="template">Template:</label>
		<select size="1" name="template" id="template">
			<option value="org.openmrs.module.openhie.client.cda.document.impl.DocumentBuilderImpl">Generic CDA</option>
			<option value="org.openmrs.module.openhie.client.cda.document.impl.ApsDocumentBuilder">Antepartum Summary</option>
			<option value="org.openmrs.module.openhie.client.cda.document.impl.HistoryPhysicalDocumentBuilder">History &amp; Physical</option>
			<option value="org.openmrs.module.openhie.client.cda.document.impl.ImmunizationDocumentBuilder">Immunization Content</option>
		</select>
	</fieldset>
	<input type="button" onClick="window.close();" value="Cancel"/>
	<input type="submit" value="Preview Document">
</form>


<%@ include file="/WEB-INF/template/footer.jsp"%>
