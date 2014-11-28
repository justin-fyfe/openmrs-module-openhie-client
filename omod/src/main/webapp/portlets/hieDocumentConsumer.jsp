<%@ include file="/WEB-INF/template/include.jsp" %>
<openmrs:hasPrivilege privilege="View Encounters">
	<c:choose>
		<c:when test="${not empty model.document}">
			<form method="post" modelAttribute="importCommand">
				<input type="hidden" name="documentId" value="${model.documentUid }"/>
				<input type="hidden" name="repositoryId" value="${model.repositoryId }"/>
				<b class="boxHeader">HIE Patient Summary Note</b>
				<div class="box">
					<c:choose>
						<c:when test="${model.wasImported }">	
							<c:url var="viewVisitUrl" value="/admin/visit/visit.form"/>
								The data from this document was imported successfully
								</a>								
						</c:when>
						<c:otherwise>
							<div>You can import this document by pressing <input type="submit" value="Import Document"/></div>
						</c:otherwise>
					</c:choose>
					<div style="border-bottom:solid 1px" class="cda">
						${model.document.html }
					</div>
				</div>
			</form>
		</c:when>
		<c:otherwise>
			<spring:message code="openhie-client.patientNotRegistered"/>
		</c:otherwise>
	</c:choose>
</openmrs:hasPrivilege>
