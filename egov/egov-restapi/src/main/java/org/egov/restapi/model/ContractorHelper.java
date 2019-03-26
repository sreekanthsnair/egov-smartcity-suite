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
package org.egov.restapi.model;

import org.hibernate.validator.constraints.SafeHtml;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ContractorHelper implements Serializable {

    private static final long serialVersionUID = -1433064701524657608L;
    @SafeHtml
    private String code;
    @SafeHtml
    private String name;
    @SafeHtml
    private String correspondenceAddress;
    @SafeHtml
    private String paymentAddress;
    @SafeHtml
    private String contactPerson;
    @SafeHtml
    private String email;
    @SafeHtml
    private String narration;
    @SafeHtml
    private String panNumber;
    @SafeHtml
    private String tinNumber;
    @SafeHtml
    private String bankName;
    @SafeHtml
    private String ifscCode;
    @SafeHtml
    private String bankAccount;
    @SafeHtml
    private String pwdApprovalCode;
    @SafeHtml
    private String exemptionName;
    @SafeHtml
    private String mobileNumber;
    private List<ContractorDetailsRequest> contractorDetails = new ArrayList<>();

    public String getPaymentAddress() {
        return paymentAddress;
    }

    public void setPaymentAddress(final String paymentAddress) {
        this.paymentAddress = paymentAddress;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(final String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(final String email) {
        this.email = email;
    }

    public String getNarration() {
        return narration;
    }

    public void setNarration(final String narration) {
        this.narration = narration;
    }

    public String getPanNumber() {
        return panNumber;
    }

    public void setPanNumber(final String panNumber) {
        this.panNumber = panNumber;
    }

    public String getTinNumber() {
        return tinNumber;
    }

    public void setTinNumber(final String tinNumber) {
        this.tinNumber = tinNumber;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(final String bankName) {
        this.bankName = bankName;
    }

    public String getIfscCode() {
        return ifscCode;
    }

    public void setIfscCode(final String ifscCode) {
        this.ifscCode = ifscCode;
    }

    public String getBankAccount() {
        return bankAccount;
    }

    public void setBankAccount(final String bankAccount) {
        this.bankAccount = bankAccount;
    }

    public String getPwdApprovalCode() {
        return pwdApprovalCode;
    }

    public void setPwdApprovalCode(final String pwdApprovalCode) {
        this.pwdApprovalCode = pwdApprovalCode;
    }

    public String getExemptionName() {
        return exemptionName;
    }

    public void setExemptionName(final String exemptionName) {
        this.exemptionName = exemptionName;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(final String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public List<ContractorDetailsRequest> getContractorDetails() {
        return contractorDetails;
    }

    public void setContractorDetails(List<ContractorDetailsRequest> contractorDetails) {
        this.contractorDetails = contractorDetails;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getCorrespondenceAddress() {
        return correspondenceAddress;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCorrespondenceAddress(String correspondenceAddress) {
        this.correspondenceAddress = correspondenceAddress;
    }

}
