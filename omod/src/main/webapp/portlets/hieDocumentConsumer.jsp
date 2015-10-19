<%@ include file="/WEB-INF/template/include.jsp" %>
<openmrs:hasPrivilege privilege="View Encounters">
	<c:choose>
		<c:when test="${not empty model.documents}">
			<table style="width:100%">
			<tr>
				<th>ID</th>
				<th>Title</th>
				<th>Created On</th>
				<th>Author(s)</th>
				<th>Type</th>
				<th>Action</th>
			</tr>
			<c:forEach var="doc" items="${model.documents}">
				<tr>
					<td style="border-bottom:solid 1px #ddd">${doc.uniqueId }</td>
					<td style="border-bottom:solid 1px #ddd">${doc.title }</td>
					<td style="border-bottom:solid 1px #ddd">${doc.creationTime }</td>
					<td style="border-bottom:solid 1px #ddd">
						<c:forEach var="aut" items="${doc.authorDisplayNames }">
							${aut} <br/>
						</c:forEach>
					</td>
					<td style="border-bottom:solid 1px #ddd">${doc.mimeType }</td>
					<td style="border-bottom:solid 1px #ddd">
						<c:url var="importDocumentUrl" value="/module/openhie-client/hieImportDocument.form"/>
						<a href="${importDocumentUrl }?uuid=${doc.uniqueId}&rep=${doc.repositoryId}" target="_blank">View / Import</a>
					</td>
				</tr>
			</c:forEach>
		</table>
		</c:when>
		<c:otherwise>
			<spring:message code="openhie-client.noDocuments"/>
		</c:otherwise>
	</c:choose>
</openmrs:hasPrivilege>
