/*
 * eGov suite of products aim to improve the internal efficiency,transparency,
 *    accountability and the service delivery of the government  organizations.
 *
 *     Copyright (C) <2015>  eGovernments Foundation
 *
 *     The updated version of eGov suite of products as by eGovernments Foundation
 *     is available at http://www.egovernments.org
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see http://www.gnu.org/licenses/ or
 *     http://www.gnu.org/licenses/gpl.html .
 *
 *     In addition to the terms of the GPL license to be adhered to in using this
 *     program, the following additional terms are to be complied with:
 *
 *         1) All versions of this program, verbatim or modified must carry this
 *            Legal Notice.
 *
 *         2) Any misrepresentation of the origin of the material is prohibited. It
 *            is required that all modified versions of this material be marked in
 *            reasonable ways as different from the original version.
 *
 *         3) This license does not grant any rights to any user of the program
 *            with regards to rights under trademark law for use of the trade names
 *            or trademarks of eGovernments Foundation.
 *
 *   In case of any queries, you can reach eGovernments Foundation at contact@egovernments.org.
 */
jQuery('#btnsearch').click(function(e) {
	$('#selectall').prop('checked', false);
	callAjaxSearch();
});

var $activities = "";

function getFormData($form) {
	var unindexed_array = $form.serializeArray();
	var indexed_array = {};

	$.map(unindexed_array, function(n, i) {
		indexed_array[n['name']] = n['value'];
	});

	return indexed_array;
}


function callAjaxSearch() {
	drillDowntableContainer = jQuery("#resultTable");
	jQuery('.report-section').removeClass('display-hide');
	reportdatatable = drillDowntableContainer
			.dataTable({
				ajax : {
					url : "/egworks/revisionestimate/ajax-searchactivities",
					type : "POST",
					"data" : getFormData(jQuery('form'))
				},
				"sPaginationType" : "bootstrap",
				"bDestroy" : true,
				'bAutoWidth': false,
				"sDom" : "<'row'<'col-xs-12 hidden col-right'f>r>t<'row'<'col-xs-3'i><'col-xs-3 col-right'l><'col-xs-3 col-right'<'export-data'T>><'col-xs-3 text-right'p>>",
				"aLengthMenu" : [ [ 10, 25, 50, -1 ], [ 10, 25, 50, "All" ] ],
				"oTableTools" : {
					"sSwfPath" : "../../../../../../egi/resources/global/swf/copy_csv_xls_pdf.swf",
					"aButtons" : []
				},
				"fnRowCallback" : function(row, data, index) {
					$('td:eq(0)',row).html(index+1);
					if(data.id != null)
						$('td:eq(1)',row).html('<input type="checkbox" name="selectActivity" class="selectActivity" data="'+ data.id +'">');
					if(data.summary != null)
						$('td:eq(4)',row).html('<span>'+ data.summary +'</span><span/><a href="#" class="hintanchor" title="'+ data.description +'"><i class="fa fa-question-circle" aria-hidden="true"></i></a></span>');
					$('td:eq(8)',row).html(parseFloat(data.rate).toFixed(2));
				},
				aaSorting : [],
				columns : [ { 
						"data" : "", "sClass" : "text-center"} , {
						"data" : "", "sClass" : "text-center", "bSortable": false} ,{
						"data" : "sorCode", "sClass" : "text-right"},{
						"data" : "categoryType", "sClass" : "text-center"},{
						"data" : "", "sClass" : "text-center"},{
						"data" : "sorNonSorType", "sClass" : "text-center"},{
						"data" : "uom", "sClass" : "text-center"},{
						"data" : "approvedQuantity", "sClass" : "text-center"},{
						"data" : "", "sClass" : "text-center"}],
				"fnInitComplete": function(oSettings, json) {
					$activities = json;
			    }
			});
}

$(document).ready(function() {
	$('#selectall').click(function(event) {
	    if(this.checked) {
	        $('.selectActivity').each(function() {
	            this.checked = true;
	        });
	    }else{
	        $('.selectActivity').each(function() {
	            this.checked = false;
	        });         
	    }
	});
	
	$('#btnadd').click(function() {
		var selectedActivities = "";
		$('.selectActivity').each(function() {
			if(this.checked)
				selectedActivities += $(this).attr('data') + ",";
		});
		if(selectedActivities == "") {
			bootbox.alert($('#errorSelect').val());
			return;
		} else {
			window.opener.populateActivities($activities, selectedActivities);
			window.close();
		}
	});
});