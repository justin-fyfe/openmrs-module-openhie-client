<%@ include file="/WEB-INF/template/include.jsp" %>

<openmrs:hasPrivilege privilege="View Encounters">
	<c:choose>
		<c:when test="${model.isPatientRegistered}">
			<spring:message code="openhie-client.patientNotRegistered"/>
		</c:when>
		<c:otherwise>
		Yay!
		</c:otherwise>
	</c:choose>
</openmrs:hasPrivilege>
