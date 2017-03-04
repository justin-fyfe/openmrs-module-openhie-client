<%@ include file="/WEB-INF/template/include.jsp"%>
<%@ include file="/WEB-INF/template/header.jsp"%>

<spring:htmlEscape defaultHtmlEscape="true" />
<openmrs:require privilege="View Patients" otherwise="/login.htm" redirect="/index.htm" />
<openmrs:htmlInclude file="/scripts/jquery/dataTables/css/dataTables_jui.css"/>
<openmrs:htmlInclude file="/scripts/jquery/dataTables/js/jquery.dataTables.min.js"/>
<openmrs:htmlInclude file="/scripts/jquery-ui/js/openmrsSearch.js" />
	
			
<h2>HIE Patient Search</h2>
<div>
	<b class="boxHeader">Find Patients</b>
	<div class="box">
		This will search the Health Information Exchange for patients matching your query parameters. Note, the results from this result may not exist in your local OpenMRS instance but can be imported.
		<form id="importForm" modelAttribute="patientSearch" method="post"
			enctype="multipart/form-data">
			
			<table>
				<tr>
					<td>Family Name:</td>
					<td><input type="text" name="familyName" value="${patientSearch.familyName }" /></td>
					<td>Given Name:</td>
					<td><input type="text" name="givenName"  value="${patientSearch.givenName }"/></td>
				</tr>
				<tr>
					<td>Date of Birth:</td>
					<td><input name="dateOfBirth" class="hasDatepicker"
						onfocus="showCalendar(this, 60)"
						onchange="clearError('dateofbirth')" id="dateOfbirth"  value="${patientSearch.dateOfBirth }" /></td>
					<td>Gender:</td>
					<td><input type="radio" name="gender" value="F" id="genderF" <c:if test='${patientSearch.gender == "F" }'>checked="checked"</c:if> /><label
						for="genderF">Female</label> <input name="gender" type="radio"
						value="M" id="genderM" <c:if test='${patientSearch.gender == "M" }'>checked="checked"</c:if>/><label for="genderM">Male</label></td>
				</tr>
				<tr>             
					<td>Identifier</td>
					<td colspan="3"><input type="text" name="identifier" value="${patientSearch.identifier }"/><input type="checkbox" name="momsId" id="momsId" value="true" <c:if test='${patientSearch.momsId == "true" }'>checked="checked"</c:if>/><label for="momsId"> Mother's Identifier</label></td>
				</tr>
			</table>
			<br /> <input type="submit" value="Search"> <br />
		</form>
	</div>
	
	<!-- Display results in a simple table -->
	<c:if test="${hasResults}">
		<table style="width:100%">
			<tr>
				<th>ID</th>
				<th>Family Name</th>
				<th>Given Name</th>
				<th>Date Of Birth</th>
				<th>Gender</th>
				<th>Action</th>
			</tr>
			<c:forEach var="patient" items="${results}">
				<tr>
					
					<td style="border-bottom:solid 1px #ddd">${patient.identifier }</td>
					<td style="border-bottom:solid 1px #ddd">${patient.familyName }</td>
					<td style="border-bottom:solid 1px #ddd">${patient.givenName }</td>
					<td style="border-bottom:solid 1px #ddd">${patient.dateOfBirth }</td>
					<td style="border-bottom:solid 1px #ddd">${patient.gender }</td>
					<td style="border-bottom:solid 1px #ddd">
						<c:choose>
							<c:when test="${patient.isImported }">
								<c:url var="viewPatientUrl" value="/patientDashboard.form"/>
								<a href="${viewPatientUrl }?patientId=${patient.openMrsId }">View</a>
							</c:when>
							<c:otherwise>
								<c:url var="importPatientUrl" value="/module/openhie-client/hieImportPatient.form"/>
								<a href="${importPatientUrl }?ecid=${patient.ecid}">Import</a>
							</c:otherwise>
						</c:choose>
					</td>
				</tr>
			</c:forEach>
		</table>
	</c:if>
</div>

<%@ include file="/WEB-INF/template/footer.jsp"%>
