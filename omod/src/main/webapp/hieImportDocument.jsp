<%@ include file="/WEB-INF/template/include.jsp"%>

<spring:htmlEscape defaultHtmlEscape="true" />
<openmrs:require privilege="View Patients" otherwise="/login.htm" redirect="/index.htm" />
<openmrs:htmlInclude file="/scripts/jquery/dataTables/css/dataTables_jui.css"/>
<openmrs:htmlInclude file="/scripts/jquery/dataTables/js/jquery.dataTables.min.js"/>
<openmrs:htmlInclude file="/scripts/jquery-ui/js/openmrsSearch.js" />

<h2>Import HIE Document</h2>
<p>Review the following document before importing</p>

<form id="importForm" modelAttribute="importPatient" method="post"
			enctype="multipart/form-data">
<c:choose>
	<c:when test="${document == null || document.html != null}">
		<div style="height:600px; width:100%; overflow:scroll; border:solid 1px #000; padding:10px">
			${document.html}
		</div>
		<div>
			<input type="submit" onClick="return confirm('This action will import all discete data from this document into your local OpenMRS instance. Do you want to continue?');" value="Import Document">
			<input type="button" onClick="window.close();" value="Cancel"/>
		</div>
	</c:when>
	<c:otherwise>
		<script type="text/javascript">
			window.close();
		</script>
	</c:otherwise>
</c:choose>
</form>


