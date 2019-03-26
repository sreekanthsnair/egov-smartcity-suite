<%--
  ~    eGov  SmartCity eGovernance suite aims to improve the internal efficiency,transparency,
  ~    accountability and the service delivery of the government  organizations.
  ~
  ~     Copyright (C) 2018  eGovernments Foundation
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
  ~            Further, all user interfaces, including but not limited to citizen facing interfaces,
  ~            Urban Local Bodies interfaces, dashboards, mobile applications, of the program and any
  ~            derived works should carry eGovernments Foundation logo on the top right corner.
  ~
  ~            For the logo, please refer http://egovernments.org/html/logo/egov_logo.png.
  ~            For any further queries on attribution, including queries on brand guidelines,
  ~            please contact contact@egovernments.org
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
  ~
  --%>


<%@ page contentType="text/html" language="java"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form"%>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring"%>
<%@ taglib uri="/WEB-INF/taglib/cdn.tld" prefix="cdn"%>

<div class="row">
	<div class="col-md-12">
		<form:form class="form-horizontal form-groups-bordered" action="" id="searchFormForPayment"
                   modelAttribute="onlineSearchRequest" method="get">
			<div class="panel panel-primary" data-collapsed="0">
				<div class="panel-heading">
					<div class="panel-title">
						<strong><spring:message code='license.search' /></strong>
					</div>
				</div>
				<div class="panel-body"></div>
				<div class="form-group">
					<label class="col-sm-2 control-label text-right"> <spring:message
							code='search.licensee.no' /></label>
					<div class="col-sm-3 add-margin">
						<input type="text" id="licenseNumber"
							class="form-control typeahead" placeholder="" autocomplete="off" />
						<form:hidden path="licenseNumber" id="licenseNumber" />
					</div>
					<label class="col-sm-3 control-label text-right"> <spring:message
							code='license.applicationnumber' /></label>
					<div class="col-sm-3 add-margin">
						<input type="text" id="applicationNumber"
							class="form-control typeahead" placeholder="" autocomplete="off" />
						<form:hidden path="applicationNumber" id="applicationNumber" />
					</div>
				</div>
				<div class="form-group">
					<label class="col-sm-2 control-label text-right"> <spring:message
							code='search.licensee.mobileNo'/></label>
					<div class="col-sm-3 add-margin">
						<form:input type="text" path="mobileNo" id="mobileNo" data-pattern="number"
									minlength="10" maxlength="10" class="form-control patternvalidation"
									placeholder="" autocomplete="off"/>
					</div>
					<label class="col-sm-3 control-label text-right"> <spring:message
							code='licensee.applicantname'/></label>
					<div class="col-sm-3 add-margin">
						<form:input type="text" path="tradeOwnerName" id="tradeOwnerName"
									class="form-control" placeholder="" autocomplete="off"/>
					</div>
				</div>
			</div>
			<div class="row">
				<div class="text-center">
					<button type="button" id="btnsearch" class="btn btn-primary">
						<spring:message code="lbl.search" />
					</button>
					<button type="reset" class="btn btn-default"><spring:message code="lbl.reset" /></button>
					<button type="button" class="btn btn-default" data-dismiss="modal" onclick="window.close();">
						<spring:message code="lbl.close"/>
					</button>
				</div>
			</div>
		</form:form>
	</div>
</div>

<div class="row display-hide report-section">
	<div class="col-md-12 report-table-container">
		<table class="table table-bordered table-hover multiheadertbl" id="tblSearchTrade">
			<thead>
				<tr>
					<th><spring:message code="license.applicationnumber"/></th>
					<th><spring:message code="lbl.license.no"/></th>
					<th><spring:message code="lbl.trade.owner"/></th>
					<th><spring:message code="search.licensee.mobileNo"/></th>
					<th><spring:message code="search.license.status"/></th>
					<th><spring:message code="search.license.propertyNo"/></th>
					<th><spring:message code="lbl.arrears.fee"/></th>
					<th><spring:message code="lbl.current.fee"/></th>
					<th><spring:message code="lbl.total.fee.collected"/></th>
					<th><spring:message code="lbl.action"/></th>
				</tr>
			</thead>
		</table>
	</div>
</div>
<link rel="stylesheet"
	href="<cdn:url  value='/resources/global/css/bootstrap/typeahead.css' context='/egi'/>">
<script type="text/javascript"
	src="<cdn:url  value='/resources/global/js/bootstrap/typeahead.bundle.js' context='/egi'/>"></script>
<link rel="stylesheet"
	href="<cdn:url value='/resources/global/css/jquery/plugins/datatables/jquery.dataTables.min.css' context='/egi'/>" />
<link rel="stylesheet"
	href="<cdn:url value='/resources/global/css/jquery/plugins/datatables/dataTables.bootstrap.min.css' context='/egi'/>">
<link rel="stylesheet"
	  href="<cdn:url value='/resources/global/css/jquery/plugins/datatables/fixedColumns.dataTables.min.css' context='/egi'/>">
<script type="text/javascript"
	src="<cdn:url  value='/resources/global/js/jquery/plugins/datatables/jquery.dataTables.min.js' context='/egi'/>"></script>
<script type="text/javascript"
		src="<cdn:url  value='/resources/global/js/jquery/plugins/datatables/extensions/fixed columns/dataTables.fixedColumns.min.js' context='/egi'/>"></script>

<script type="text/javascript"
	src="<cdn:url  value='/resources/global/js/jquery/plugins/datatables/dataTables.bootstrap.js' context='/egi'/>"></script>
<script type="text/javascript"
	src="<cdn:url  value='/resources/global/js/jquery/plugins/jquery.validate.min.js' context='/egi'/>"></script>
<script type="text/javascript"
	src="<cdn:url  value='/resources/js/app/trade-license-search-for-pay.js?rnd=${app_release_no}'/>"></script>
