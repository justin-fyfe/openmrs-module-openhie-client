<%@ include file="/WEB-INF/template/include.jsp"%>
<%@ include file="/WEB-INF/template/header.jsp"%>

<spring:htmlEscape defaultHtmlEscape="true" />
<openmrs:require privilege="View Patients" otherwise="/login.htm" redirect="/index.htm" />
<openmrs:htmlInclude file="/scripts/jquery/dataTables/css/dataTables_jui.css"/>
<openmrs:htmlInclude file="/scripts/jquery/dataTables/js/jquery.dataTables.min.js"/>
<openmrs:htmlInclude file="/scripts/jquery-ui/js/openmrsSearch.js" />

<h2>Export HIE Document (Step 2 of 2)</h2>
<p>The following document will be shared with the configured Health Information Exchange (HIE), review the contents and press "share" to submit the document</p>

<form id="importForm" method="post"
			enctype="multipart/form-data">
<c:choose>
	<c:when test="${error != null}">
		The document export was successful
	</c:when>
	<c:otherwise>
		The document export failed with reason ${error}
	</c:otherwise>
</c:choose>
	<input type="button" onClick="window.close()" value="Close"/>
</form>


<%@ include file="/WEB-INF/template/footer.jsp"%>
