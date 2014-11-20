<%@ include file="/WEB-INF/template/include.jsp" %>

<openmrs:hasPrivilege privilege="View Encounters">
	<c:choose>
		<c:when test="${model.isPatientRegistered}">
		
		</c:when>
	</c:choose>
</openmrs:hasPrivilege>
