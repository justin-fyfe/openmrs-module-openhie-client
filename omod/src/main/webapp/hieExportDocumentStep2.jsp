<%@ include file="/WEB-INF/template/include.jsp"%>
<%@ include file="/WEB-INF/template/header.jsp"%>

<spring:htmlEscape defaultHtmlEscape="true" />
<openmrs:require privilege="View Patients" otherwise="/login.htm" redirect="/index.htm" />
<openmrs:htmlInclude file="/scripts/jquery/dataTables/css/dataTables_jui.css"/>
<openmrs:htmlInclude file="/scripts/jquery/dataTables/js/jquery.dataTables.min.js"/>
<openmrs:htmlInclude file="/scripts/jquery-ui/js/openmrsSearch.js" />

<h2>Export HIE Document (Step 2 of 2)</h2>
<p>The following document will be shared with the configured Health Information Exchange (HIE), review the contents and press "share" to submit the document</p>

<form id="importForm" modelAttribute="importPatient" method="post"
			enctype="multipart/form-data">
<c:choose>
	<c:when test="${document != null && document.html != null}">
		<div style="height:600px; width:100%; overflow:scroll; border:solid 1px #000; padding:10px">
			${document.html}
		</div>
		<div>
			<input type="submit" onClick="return confirm('This action will export protected health information from your copy of OpenMRS and share it with others. Are you sure you want to do this?');" value="Share Document">
			<input type="button" onClick="window.close();" value="Cancel"/>
		</div>
	</c:when>
</c:choose>
</form>

<%@ include file="/WEB-INF/template/footer.jsp"%>
