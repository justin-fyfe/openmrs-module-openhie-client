<%@ include file="/WEB-INF/template/include.jsp" %>
<openmrs:hasPrivilege privilege="View Encounters">

<b class="boxHeader">Vaccination History</b>
<div class="box">
	<form id="importForm" modelAttribute="vaccinations" method="post"
		enctype="multipart/form-data">
			

		<ul style="list-style:none">
			<li><input type="checkbox" <c:if test="${not empty model.v160214 }"> checked="checked" disabled="disabled"</c:if> value="783" name="vaccine" id="opv0"><label for="opv0">OPV0 ${model.v160214 }</label></li>
			<li><input type="checkbox" value="783" <c:if test="${not empty model.v783 }"> checked="checked" disabled="disabled"</c:if> name="vaccine" id="opv1"><label for="opv1">OPV1 ${model.v783 }</label></li>
			<li><input type="checkbox" <c:if test="${not empty model.v71902 }"> checked="checked" disabled="disabled"</c:if> value="886" name="vaccine" id="bcg0"><label for="bcg0">BCG0 ${model.v71902 }</label></li>
			<li><input type="checkbox" <c:if test="${not empty model.v1423 }"> checked="checked" disabled="disabled"</c:if> value="1423" name="vaccine" id="penta1"><label for="penta1">PENTA1 ${model.v1423 }</label></li>
			<li><input type="checkbox" value="83531" <c:if test="${not empty model.v83531 }">checked="checked" disabled="disabled"</c:if> name="vaccine" id="rota1"><label for="rota1">ROTA1 ${model.v83531 }</label></li>
			<li><input type="checkbox" value="162342" <c:if test="${not empty model.v162342 }">checked="checked" disabled="disabled"</c:if> name="vaccine" id="pcv1"><label for="pcv1">PCV1 ${model.v162342 }</label></li>
		</ul>
		
		<input type="submit" value="Register Vaccinations"/>
	</form>
</div>
</openmrs:hasPrivilege>