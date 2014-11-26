<%@ include file="/WEB-INF/template/include.jsp"%>
<%@ include file="/WEB-INF/template/header.jsp"%>

<spring:htmlEscape defaultHtmlEscape="true" />

<h2>HIE Patient Search</h2>
<div>
	<b class="boxheader">Find Patients</b>
	<div class="box">
		This will search the Health Information Exchange for patients matching your query parameters. Note, the results from this result may not exist in your local OpenMRS instance but can be imported.
		<form id="importForm" modelAttribute="patientSearch" method="post"
			enctype="multipart/form-data">
			<table>
				<tr>
					<td>Family Name:</td>
					<td><input type="text" name="familyName" /></td>
					<td>Given Name:</td>
					<td><input type="text" name="givenName" /></td>
				</tr>
				<tr>
					<td>Date of Birth:</td>
					<td><input name="dateOfBirth" class="hasDatepicker"
						onfocus="showCalendar(this, 60)"
						onchange="clearError('dateofbirth')" id="dateofbirth" /></td>
					<td>Gender:</td>
					<td><input type="radio" name="gender" value="F" id="genderF" /><label
						for="genderF">Female</label> <input name="gender" type="radio"
						value="M" id="genderM" /><label for="genderM">Male</label></td>
				</tr>
				<tr>             
					<td>Identifier</td>
					<td colspan="3"><input type="text" name="identifier"/><input type="checkbox" name="momsId" id="momsId" value="true"/><label for="momsId"> Mother's Identifier</label></td>
				</tr>
			</table>
			<br /> <input type="submit" value="Search"> <br />
		</form>


	</div>
</div>

<%@ include file="/WEB-INF/template/footer.jsp"%>
