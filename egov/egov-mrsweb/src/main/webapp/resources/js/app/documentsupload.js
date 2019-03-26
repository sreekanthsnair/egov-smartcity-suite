/*
 *    eGov  SmartCity eGovernance suite aims to improve the internal efficiency,transparency,
 *    accountability and the service delivery of the government  organizations.
 *
 *     Copyright (C) 2017  eGovernments Foundation
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
 *            Further, all user interfaces, including but not limited to citizen facing interfaces, 
 *            Urban Local Bodies interfaces, dashboards, mobile applications, of the program and any 
 *            derived works should carry eGovernments Foundation logo on the top right corner.
 *
 *            For the logo, please refer http://egovernments.org/html/logo/egov_logo.png.
 *            For any further queries on attribution, including queries on brand guidelines, 
 *            please contact contact@egovernments.org
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
 *
 */
$(document).ready(function(){
	
	var fileformatsinclude = ['doc','docx','xls','xlsx','rtf','pdf','jpeg','gif','jpg','png','txt','zip','rar']; 
	
	$('.file-ellipsis.upload-file').change( function(e) {		
		/*validation for file upload*/
		myfile= $( this ).val();
		var ext = myfile.split('.').pop();
		if($.inArray(ext.toLowerCase(), fileformatsinclude) == -1){
			bootbox.alert("Please upload "+fileformatsinclude+" format documents only");
			$( this ).val('');
			return false; 
		}
		
		var fileInput = $(this);
   		var maxSize = 2097152; //file size  in bytes(2MB)
		if(fileInput.get(0).files.length){
			var fileSize = this.files[0].size; // in bytes
			var charlen = (this.value.split('/').pop().split('\\').pop()).length;
			if(charlen > 255){
				bootbox.alert('Document name should not exceed 255 characters!');
				fileInput.replaceWith(fileInput.val('').clone(true));
				return false;			
			} 
			else if(fileSize > maxSize){
				bootbox.alert('File size should not exceed 2 MB!');
				fileInput.replaceWith(fileInput.val('').clone(true));
				return false;
			}			
		}	
		
	});
	
var fileimageformatsinclude = ['jpeg','jpg','png','gif']; 
	
	$('.validate-file').change( function(e) {		
		/*validation for file upload*/
		myfile= $( this ).val();
		var ext = myfile.split('.').pop();
		if($.inArray(ext.toLowerCase(), fileimageformatsinclude) == -1){
			bootbox.alert("Please upload "+fileimageformatsinclude+" format images only");
			$( this ).val('');
			return false;    
		}
		var image = $( this ).prop('files')[0];
		var fileReader = new FileReader();
		var inputUpload = $(this);

		fileReader.onload = function(e) {   
           $( $(inputUpload).siblings('img') ).prop('src', e.target.result);
		}
       
		fileReader.readAsDataURL(image);
		
		var fileInput = $(this);
		var maxSize = 2097152; // file size in
								// bytes(2MB)
		var inMB = maxSize / 1024 / 1024;
		if (fileInput.get(0).files.length) {
			var fileSize = this.files[0].size; // in
												// bytes
			var charlen = (this.value
					.split('/').pop().split(
							'\\').pop()).length;
			if (charlen > 255) {
				bootbox
						.alert('File length should not exceed 255 characters!');
				fileInput.replaceWith(fileInput
						.val('').clone(true));
				return false;
			} else if (fileSize > maxSize) {
				bootbox
						.alert('File size should not exceed '
								+ inMB + ' MB!');
				fileInput.replaceWith(fileInput
						.val('').clone(true));
				return false;
			}
		}
	});
	
	
});
