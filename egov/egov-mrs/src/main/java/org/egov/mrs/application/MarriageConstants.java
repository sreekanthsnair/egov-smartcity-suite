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

package org.egov.mrs.application;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MarriageConstants {

    public static final String MODULE_NAME = "Marriage Registration";
    public static final String BOUNDARY_TYPE = "City";
    public static final String ADMINISTRATION_HIERARCHY_TYPE = "ADMINISTRATION";

    public static final String APPROVER_ROLE_NAME = "ULB Operator";
    public static final String DATE_FORMAT_DDMMYYYY = "dd-MM-yyyy";

    public static final String REISSUE_FEECRITERIA = "Re-Issue Fee";
    public static final String ADDITIONAL_RULE_REGISTRATION = "MARRIAGE REGISTRATION";
    public static final double MAX_SIZE = 2097152;
    public static final String CPK_END_POINT_URL = "http://www.chpk.ap.gov.in/CPKServices/MarriageCertificate/Create/";

    // validactions
    public static final String WFLOW_ACTION_STEP_REJECT = "Reject";
    public static final String WFLOW_ACTION_STEP_CANCEL = "Cancel Registration";
    public static final String WFLOW_ACTION_STEP_CANCEL_REISSUE = "Cancel ReIssue";
    public static final String WFLOW_ACTION_STEP_FORWARD = "Forward";
    public static final String WFLOW_ACTION_STEP_APPROVE = "Approve";
    public static final String WFLOW_ACTION_STEP_DIGISIGN = "Sign";
    public static final String WFLOW_ACTION_STEP_PRINTCERTIFICATE = "Print Certificate";

    // Pendingactions
    public static final String WFLOW_PENDINGACTION_PRINTCERTIFICATE = "Certificate Print Pending";
    public static final String WFLOW_PENDINGACTION_DIGISIGNPENDING = "Digital Signature Pending";
    public static final String WFLOW_PENDINGACTION_APPRVLPENDING_DIGISIGN = "Commisioner Approval Pending_DigiSign";
    public static final String WFLOW_PENDINGACTION_APPRVLPENDING_PRINTCERT = "Commisioner Approval Pending_PrintCert";
    public static final String WFLOW_PENDINGACTION_CMO_APPRVLPENDING = "Chief Medical Officer of Health Approval Pending";
    public static final String WFLOW_PENDINGACTION_MHO_APPRVLPENDING = "Municipal Health Officer Approval Pending";
    public static final String WFLOW_PENDINGACTION_CLERK_APPRVLPENDING = "Clerk Approval Pending";
    public static final String WFLOW_PENDINGACTION_APPROVAL_APPROVEPENDING ="Approver Approval Pending";
    public static final String WFSTATE_CLRK_APPROVED="Clerk Approved";
    public static final String WFSTATE_APPROVER_REJECTED ="Approver Rejected Application";
    public static final String WFSTATE_MHO_APPROVED="Municipal Health Officer Approved";
    public static final String WFSTATE_CMOH_APPROVED="Chief Medical Officer of Health Approved";
    public static final String WFSTATE_MARRIAGEAPI_NEW ="MarriageAPI NEW";
    public static final String COMMISSIONER ="Commissioner";
    public static final String MARRIAGE_REGISTRAR = "Marriage Registrar";
    
    public static final String APPROVED = "APPROVED";
    public static final String MARRIAGEFEECOLLECTION_FUCNTION_CODE = "MARRIAGE_FUNCTION_CODE";
    public static final String FILESTORE_MODULECODE = "MRS";
    public static final String SENDSMSFROOMMARRIAGEMODULE = "SENDSMSFROOMMARRIAGEMODULE";
    public static final String SENDEMAILFROOMMARRIAGEMODULE = "SENDEMAILFROOMMARRIAGEMODULE";

    public static final String APPL_INDEX_MODULE_NAME = "Marriage Registration";
    public static final String INDAIN_NATIONALITY ="Indian";

    public static final String REGISTER_NO_OF_DAYS = "90";
    public static final String MARRIAGEREGISTRATION_DAYS_VALIDATION = "MARRIAGEREGISTRATION_DAYS_VALIDATION";
    public static final String LOCATION_HIERARCHY_TYPE = "LOCATION";
    public static final String BOUNDARYTYPE_LOCALITY = "locality";
    public static final String REISSUE_PRINTREJECTIONCERTIFICATE = "REISSUE_PRINTREJECTIONCERTIFICATE";

    public static final String MOM = "MoM";
    public static final String CF_STAMP = "CF_STAMP";
    public static final String AFFIDAVIT = "AFFIDAVIT";
    public static final String MIC = "MIC";
    public static final String SCHOOL_LEAVING_CERT = "SLC";
    public static final String BIRTH_CERTIFICATE = "BC";
    public static final String DIVORCE_CERTIFICATE = "DCA";
    public static final String DEATH_CERTIFICATE = "DCSWA";
    public static final String NOTARY_AFFIDAVIT = "NotaryAffidavit";
    public static final String RATION_CRAD = "RationCard";
    public static final String ELECTRICITY_BILL = "MSEBBILL";
    public static final String TELEPHONE_BILL = "TelephoneBill";
    public static final String PASSPORT = "Passport";
    public static final String AADHAR = "Aadhar";
    public static final String YEAR = "year";
    public static final String NOOFDAYSTOPRINT = "NOOFDAYSTOPRINT";
    public static final String ANONYMOUS_USER= "Anonymous";
    public static final String SOURCE_ONLINE = "ONLINE";

    public static final String DIGITALSIGNINWORKFLOW_ENABLED = "DIGITALSIGN_IN_WORKFLOW";
    public static final String REASSIGN_BUTTONENABLED = "REASSIGN_BUTTONENABLED";
    
    //CITIZEN
    public static final String ROLE_CITIZEN = "CITIZEN";
    
    //Designations
    public static final String CMO_DESIG = "Chief Medical Officer of Health";
    public static final String MHO_DESIG = "Municipal Health Officer";

    public static final String MRG_DEPARTMENT_CODE = "MARRIAGE_DEPARTMENT_CODE";
    public static final String MRS_DEFAULT_FUNCTIONARY_CODE = "MARRIAGE_DEFAULT_FUNCTIONARY_CODE";
    public static final String MRS_DEFAULT_FUND_SRC_CODE = "MARRIAGE_DEFAULT_FUND_SRC_CODE";
    public static final String MRS_DEFAULT_FUND_CODE = "MARRIAGE_DEFAULT_FUND_CODE";

    public static final String APPROVAL_COMMENT = "approvalComent";
    public static final String APPLICATION_NUMBER = "applicationNumber";
    public static final String FILE_STORE_ID_APPLICATION_NUMBER = "fileStoreIdApplicationNumber";
    
    public static final String MRS_HEIRARCHYTYPE = "MARRIAGE_REGISTRATIONUNIT_HEIRARCHYTYPE";
    public static final String MRS_BOUNDARYYTYPE = "MARRIAGE_REGISTRATIONUNIT_BOUNDARYYTYPE";
    
  
    //CSC operator related constants
    public static final String MRS_ROLEFORNONEMPLOYEE = "MRSROLEFORNONEMPLOYEE";
    public static final String CSC_OPERATOR_ROLE = "CSC Operator";
    public static final String MRS_DESIGNATION_CSCOPERATOR = "MRSDESIGNATIONFORCSCOPERATORWORKFLOW";
    public static final String MRS_DEPARTEMENT_CSCOPERATOR = "MRSDEPARTMENTFORCSCOPERATORWORKFLOW";
    public static final String MRS_DESIGNATION_REGISTRARAR = "MRSDESIGNATIONFORMRSREGISTRAR";
    public static final String MRS_DEPARTEMENT_REGISTRARAR = "MRSDEPARTMENTFORMRSREGISTRARAR";
    public static final String CREATED =  "CREATED";
    public static final String JUNIOR_SENIOR_ASSISTANCE_APPROVAL_PENDING ="Junior/Senior Assistance approval pending";
    public static final String CSC_OPERATOR_CREATED =  "CSC Operator created";
    public static final String APPLICATION_PDF = "application/pdf";
    
    
    public static final String SLAFORMARRIAGEREGISTRATION = "SLAFORMARRIAGEREGISTRATION";
    public static final String SLAFORMARRIAGEREISSUE = "SLAFORMARRIAGEREISSUE";
    
    public static final String MEESEVA_OPERATOR_ROLE = "MeeSeva Operator";
    public static final String MEESEVA_REDIRECT_URL = "/meeseva/generatereceipt?transactionServiceNumber=";
    public static final String SOURCE_MEESEVA = "MEESEVA";
    public static final String OPEN = "OPEN";
    public static final String REJECTED = "REJECTED";
    public static final String DIGITALSIGNED = "DIGITALSIGNED";
    public static final String REGISTERED = "REGISTERED";
    public static final String CANCELLED = "CANCELLED";
    
    public static final String STATETYPE_REGISTRATION = "MarriageRegistration";
    public static final String STATETYPE_REISSUE = "ReIssue";
    public static final String N_A = "N/A";
    
    private static final List<String> VENUELIST = new ArrayList<>();
    static {
        VENUELIST.add("Residence");
        VENUELIST.add("Function Hall");
        VENUELIST.add("Worship Place");
        VENUELIST.add("Others");
    }
    private static final List<String> WITENSSRELATION = new ArrayList<>();
    static {
        WITENSSRELATION.add("S/o");
        WITENSSRELATION.add("D/o");
        WITENSSRELATION.add("W/o");
    }

    private MarriageConstants() {
        // To hide implicit public
    }
    
    public static List<String> getMarriageVenues() {
        return Collections.unmodifiableList(VENUELIST);
    }

    public static List<String> getWitnessRelations() {
        return Collections.unmodifiableList(WITENSSRELATION);
    }
}
