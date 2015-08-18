<!-- #-------------------------------------------------------------------------------
# eGov suite of products aim to improve the internal efficiency,transparency, 
#    accountability and the service delivery of the government  organizations.
# 
#     Copyright (C) <2015>  eGovernments Foundation
# 
#     The updated version of eGov suite of products as by eGovernments Foundation 
#     is available at http://www.egovernments.org
# 
#     This program is free software: you can redistribute it and/or modify
#     it under the terms of the GNU General Public License as published by
#     the Free Software Foundation, either version 3 of the License, or
#     any later version.
# 
#     This program is distributed in the hope that it will be useful,
#     but WITHOUT ANY WARRANTY; without even the implied warranty of
#     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#     GNU General Public License for more details.
# 
#     You should have received a copy of the GNU General Public License
#     along with this program. If not, see http://www.gnu.org/licenses/ or 
#     http://www.gnu.org/licenses/gpl.html .
# 
#     In addition to the terms of the GPL license to be adhered to in using this
#     program, the following additional terms are to be complied with:
# 
# 	1) All versions of this program, verbatim or modified must carry this 
# 	   Legal Notice.
# 
# 	2) Any misrepresentation of the origin of the material is prohibited. It 
# 	   is required that all modified versions of this material be marked in 
# 	   reasonable ways as different from the original version.
# 
# 	3) This license does not grant any rights to any user of the program 
# 	   with regards to rights under trademark law for use of the trade names 
# 	   or trademarks of eGovernments Foundation.
# 
#   In case of any queries, you can reach eGovernments Foundation at contact@egovernments.org.
#------------------------------------------------------------------------------- --> 
<%@ taglib prefix="egov" tagdir="/WEB-INF/tags"%> 
<%@ include file="/includes/taglibs.jsp" %>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />

<html>
<head> 
	<title><s:text name='arrearRegReport.search' /></title>
	<script src="<c:url value='/resources/global/js/bootstrap/bootstrap.js' context='/egi'/>"></script>
	<script src="<c:url value='/resources/global/js/bootstrap/typeahead.bundle.js' context='/egi'/>"></script>
	<script type="text/javascript">
	function validateFormAndSubmit(){
		 document.arrearRegForm.action='${pageContext.request.contextPath}/reports/arrearRegisterReport-generateArrearReport.action';
    	 document.arrearRegForm.submit();
	}

	function populateWard() {
		populatewardId( {
			zoneId : document.getElementById("zoneId").value
		});
		document.getElementById("areaId").options.length = 1;
		jQuery('#areaId').val('-1');
	}	

	function populateBlock() {
		populateareaId({
			wardId : document.getElementById("wardId").value
		});
	}

	jQuery(document).ready(function(){
		 jQuery('#localityId').change(function() {
			console.log("came jursidiction"+jQuery('#localityId').val());
			jQuery.ajax({
				url: "/ptis/common/ajaxCommon-blockByLocality.action",
				type: "GET",
				data: {
					locality : jQuery('#localityId').val()
				},
				cache: false,
				dataType: "json",
				success: function (response) {
					jQuery('#zoneId').val(response.zoneId);
					populateWard();
					setTimeout(function(){
  					//your code to be executed after 1 seconds
						jQuery('#wardId').val(response.wardId);
						populateBlock();
						setTimeout(function(){
		  					//your code to be executed after 1 seconds
								jQuery('#areaId').val(response.blockId);
							}, 1000);
					}, 1000); 
				}, 
				error: function (response) {
					console.log("failed"); 
					jQuery('#zoneId').val('-1');
					jQuery('#wardId').val('-1');
					jQuery('#areaId').val('-1');
					alert("No boundary details mapped for locality")
				}
			});
		});
		
	});
	</script>
	<sx:head/>
</head>
	<body>
		<div align="left">
  			<s:actionerror/>
  		</div>
		<s:form name="arrearRegForm" theme="simple" validate="true">
		<div class="formmainbox">
			<div class="formheading"></div>
			<div class="headingbg"><s:text name="arrearRegReport.search"/></div>
				<table width="100%" border="0" cellspacing="0" cellpadding="0">
				 <tr>
				  	<td class="greybox2">&nbsp;</td>
					<td class="greybox"><s:text name="locality"></s:text></td>
					<td class="greybox"><s:select name="localityId" id="localityId" list="dropdownData.localityList"
					listKey="id" listValue="name" headerKey="-1" headerValue="%{getText('default.select')}" value="%{localityId}"/>
					</td>
				    <td class="greybox"><s:text name="Zone"/> :</td>
					<td class="greybox">
						<s:select name="zoneId" id="zoneId" list="dropdownData.Zone"
							listKey="id" listValue="name" headerKey="-1"
							headerValue="%{getText('default.select')}" value="%{zoneId}"
							onchange="populateWard()" />
						<egov:ajaxdropdown id="wardId" fields="['Text','Value']"
							dropdownId="wardId" url="common/ajaxCommon-wardByZone.action" />
					</td>
				 </tr>
				
				<tr>
					<td class="bluebox2">&nbsp;</td>
					<td class="bluebox"><s:text name="Ward"/> :</td>
					<td class="bluebox"><s:select name="wardId" id="wardId" list="dropdownData.wardList"
							listKey="id" listValue="name" headerKey="-1"
							headerValue="%{getText('default.select')}" value="%{wardId}"  
							onchange="populateBlock()"/>
							<egov:ajaxdropdown id="areaId" fields="['Text','Value']"
							dropdownId="areaId" url="common/ajaxCommon-areaByWard.action" />
					</td>
					<td class="bluebox"><s:text name="block"/> :</td>
					<td class="bluebox"><s:select name="areaId" id="areaId" list="dropdownData.blockList"
							listKey="id" listValue="name" headerKey="-1"
							headerValue="%{getText('default.select')}" value="%{areaId}" />
					</td>
				</tr>
				<tr>
					<td colspan="4">&nbsp;</td>
				</tr>
				
	</table>
		<tr>
        	<font size="2"><div align="left" class="mandatory"><s:text name="mandtryFlds"/></div></font>
        </tr>
	</div>
	<div class="buttonbottom" align="center">
		<tr>
		 <td><input type="submit" id="btnsearch" name="btnsearch" value="Search" class="buttonsubmit" onclick="return validateFormAndSubmit();" /></td>
		 <td><input type="button" name="button2" id="button2" value="Close" class="button" onclick="window.close();"/></td>
		</tr>
	</div>
	</s:form>
	</body>
</html>