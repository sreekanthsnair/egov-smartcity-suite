<%--
  ~ eGov suite of products aim to improve the internal efficiency,transparency,
  ~    accountability and the service delivery of the government  organizations.
  ~
  ~     Copyright (C) <2015>  eGovernments Foundation
  ~
  ~     The updated version of eGov suite of products as by eGovernments Foundation
  ~     is available at http://www.egovernments.org
  ~
  ~     This program is free software: you can redistribute it and/or modify
  ~     it under the terms of the GNU General Public License as published by
  ~     the Free Software Foundation, either version 3 of the License, or
  ~     any later version.
  ~
  ~     This program is distributed in the hope that it will be useful,
  ~     but WITHOUT ANY WARRANTY; without even the implied warranty of
  ~     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  ~     GNU General Public License for more details.
  ~
  ~     You should have received a copy of the GNU General Public License
  ~     along with this program. If not, see http://www.gnu.org/licenses/ or
  ~     http://www.gnu.org/licenses/gpl.html .
  ~
  ~     In addition to the terms of the GPL license to be adhered to in using this
  ~     program, the following additional terms are to be complied with:
  ~
  ~         1) All versions of this program, verbatim or modified must carry this
  ~            Legal Notice.
  ~
  ~         2) Any misrepresentation of the origin of the material is prohibited. It
  ~            is required that all modified versions of this material be marked in
  ~            reasonable ways as different from the original version.
  ~
  ~         3) This license does not grant any rights to any user of the program
  ~            with regards to rights under trademark law for use of the trade names
  ~            or trademarks of eGovernments Foundation.
  ~
  ~   In case of any queries, you can reach eGovernments Foundation at contact@egovernments.org.
  --%>

<%@ include file="/includes/taglibs.jsp" %>
<html>  
	<head>  
    	<title><s:text name="contractor.grade.master.title" /></title>
    	<script src="<egov:url path='resources/js/contractorgrade.js?${app_release_no}'/>"></script> 
	</head>  
	<body>
	
		<div class="new-page-header">
			<s:if test="%{mode == 'edit'}">
				<s:text name="contractor.grade.header.modify" />
			</s:if><s:else>
				<s:text name="contractor.grade.header" />
			</s:else>
		</div>
		
    	<s:if test="%{hasErrors()}">
        	<div class="alert alert-danger">
          		<s:actionerror/>
          		<s:fielderror/>
        	</div>
    	</s:if>
    	
		<s:if test="%{hasActionMessages()}">
			<div class="alert alert-success">
				 <a href="#" style="font-size:21px;" class="close" data-dismiss="alert" aria-label="close">&times;</a>
				<s:actionmessage theme="simple" escape="false"/>
			</div>
		</s:if>
   
		<s:form action="contractorGrade-save" theme="simple" name="contractorGrade" id="contractorGrade" cssClass="form-horizontal form-groups-bordered">
			<s:token/>
			<s:hidden name="model.id" />
			<s:hidden name="id" />
			<s:hidden name="mode" />
			<%@ include file='contractorGrade-form.jsp'%>
				<div class="row">
					<div class="col-xs-12 text-center buttonholdersearch">
						<s:if test="%{id == null}">
							<s:submit value="Save" method="save" cssClass="btn btn-primary" id="saveButton" name="button" onclick="return validateContractorGradeFormAndSubmit();"/>&nbsp;
						</s:if><s:else>
							<s:submit value="Modify" method="save" cssClass="btn btn-primary" id="modifyButton" name="button" onclick="return validateContractorGradeFormAndSubmit();"/>&nbsp;
						</s:else>
						<s:if test="%{model.id==null}" >
							<input type="button" value="Clear" class="btn btn-default" onclick="clearForm(this.form.id)"/>&nbsp;
						</s:if>
						<input type="button" class="btn btn-default" value="Close" id="closeButton" name="closeButton" onclick="window.close();" />
				     </div>
				</div>
		</s:form> 
	</body>
</html>
