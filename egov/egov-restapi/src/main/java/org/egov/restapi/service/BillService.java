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

package org.egov.restapi.service;

import org.apache.commons.lang.StringUtils;
import org.egov.commons.Accountdetailtype;
import org.egov.commons.CChartOfAccountDetail;
import org.egov.commons.CChartOfAccounts;
import org.egov.commons.Fund;
import org.egov.commons.dao.ChartOfAccountsHibernateDAO;
import org.egov.commons.dao.FinancialYearHibernateDAO;
import org.egov.commons.service.*;
import org.egov.commons.utils.EntityType;
import org.egov.egf.expensebill.service.ExpenseBillService;
import org.egov.egf.model.BillPaymentDetails;
import org.egov.egf.utils.FinancialUtils;
import org.egov.infra.admin.master.service.DepartmentService;
import org.egov.model.bills.EgBillPayeedetails;
import org.egov.model.bills.EgBilldetails;
import org.egov.model.bills.EgBillregister;
import org.egov.model.bills.EgBillregistermis;
import org.egov.restapi.constants.RestApiConstants;
import org.egov.restapi.model.BillDetails;
import org.egov.restapi.model.BillPayeeDetails;
import org.egov.restapi.model.BillRegister;
import org.egov.restapi.model.RestErrors;
import org.egov.services.bills.BillsService;
import org.egov.services.masters.SchemeService;
import org.egov.services.masters.SubSchemeService;
import org.egov.works.master.service.ContractorService;
import org.egov.works.models.estimate.ProjectCode;
import org.egov.works.services.ProjectCodeService;
import org.egov.works.utils.WorksConstants;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.validation.Valid;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Transactional(readOnly = true)
public class BillService {

    @Autowired
    private FundService fundService;

    @Autowired
    private FunctionService functionService;

    @Autowired
    private SchemeService schemeService;

    @Autowired
    private SubSchemeService subSchemeService;

    @Autowired
    private ChartOfAccountsService chartOfAccountsService;

    @Autowired
    private ExpenseBillService expenseBillService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private AccountdetailtypeService accountdetailtypeService;

    @Autowired
    private ProjectCodeService projectCodeService;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private ChartOfAccountsHibernateDAO chartOfAccountsHibernateDAO;

    @Autowired
    private FinancialYearHibernateDAO financialYearHibernateDAO;

    @Autowired
    private ContractorService contractorService;

    @Autowired
    private FinancialUtils financialUtils;

    @Autowired
    private BillsService billsService;

    @PersistenceContext
    private EntityManager entityManager;

    public Session getCurrentSession() {
        return entityManager.unwrap(Session.class);
    }

    public List<RestErrors> validateBillRegister(@Valid final BillRegister billRegister) {
        final List<RestErrors> errors = new ArrayList<>();
        RestErrors restErrors;
        validateMandatoryFields(billRegister, errors);
        validateBillDates(billRegister, errors);
        if (billRegister.getBillAmount() == null) {
            restErrors = new RestErrors();
            restErrors.setErrorCode(RestApiConstants.THIRD_PARTY_ERR_CODE_NO_BILLAMOUNT);
            restErrors.setErrorMessage(RestApiConstants.THIRD_PARTY_ERR_MSG_NO_BILLAMOUNT);
            errors.add(restErrors);
        }
        if (projectCodeService.findActiveProjectCodeByCode(billRegister.getProjectCode()) == null
                && StringUtils.isBlank(billRegister.getNameOfWork())) {
            restErrors = new RestErrors();
            restErrors.setErrorCode(RestApiConstants.THIRD_PARTY_ERR_CODE_NO_NAMEOFWORK);
            restErrors.setErrorMessage(RestApiConstants.THIRD_PARTY_ERR_MSG_NO_NAMEOFWORK);
            errors.add(restErrors);
        }
        if (StringUtils.isNotBlank(billRegister.getNameOfWork())
                && !billRegister.getNameOfWork().matches(WorksConstants.ALPHANUMERICWITHALLSPECIALCHAR)) {
            restErrors = new RestErrors();
            restErrors.setErrorCode(RestApiConstants.THIRD_PARTY_ERR_CODE_NOT_VALID_NAME_OF_WORK);
            restErrors.setErrorMessage(RestApiConstants.THIRD_PARTY_ERR_MSG_NOT_VALID_NAME_OF_WORK);
            errors.add(restErrors);
        }
        if (StringUtils.isNotBlank(billRegister.getSchemeCode())
                && schemeService.findByCode(billRegister.getSchemeCode()) == null) {
            restErrors = new RestErrors();
            restErrors.setErrorCode(RestApiConstants.THIRD_PARTY_ERR_CODE_NO_SCHEME);
            restErrors.setErrorMessage(RestApiConstants.THIRD_PARTY_ERR_MSG_NO_SCHEME);
            errors.add(restErrors);
        }
        if (StringUtils.isNotBlank(billRegister.getSubSchemeCode())
                && subSchemeService.findByCode(billRegister.getSubSchemeCode()) == null) {
            restErrors = new RestErrors();
            restErrors.setErrorCode(RestApiConstants.THIRD_PARTY_ERR_CODE_NO_SCHEME);
            restErrors.setErrorMessage(RestApiConstants.THIRD_PARTY_ERR_MSG_NO_SCHEME);
            errors.add(restErrors);
        }
        if(StringUtils.isNotBlank(billRegister.getFundCode())){
            List<Fund> fundCode = fundService.getByIsActive(true);
            boolean isValidFundCode = false;
            for(Fund fund : fundCode){
                if(fund.getCode().equals(billRegister.getFundCode())){
                    isValidFundCode = true;
                }
            }
            if(!isValidFundCode){
                restErrors = new RestErrors();
                restErrors.setErrorCode(RestApiConstants.THIRD_PARTY_ERR_CODE_NOT_VALID_FUND_CODE);
                restErrors.setErrorMessage(RestApiConstants.THIRD_PARTY_ERR_MSG_NOT_VALID_FUND_CODE);
                errors.add(restErrors);
            }
        }
        if(StringUtils.isNotBlank(billRegister.getPartyBillNumber())){
            Pattern pattern = Pattern.compile("^[a-zA-Z\\d-/]+$", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(billRegister.getPartyBillNumber());
            boolean isValidBillNumber = matcher.find();
            if (!isValidBillNumber){
                restErrors = new RestErrors();
                restErrors.setErrorCode(RestApiConstants.THIRD_PARTY_ERR_CODE_NOT_VALID_BILLNUMBER);
                restErrors.setErrorMessage(RestApiConstants.THIRD_PARTY_ERR_MSG_NOT_VALID_BILLNUMBER);
                errors.add(restErrors);
            }
        }

        validateBillDetails(billRegister, errors);
        validateBillPayeeDetails(billRegister, errors);

        return errors;
    }

    private void validateMandatoryFields(@Valid final BillRegister billRegister, final List<RestErrors> errors) {
        RestErrors restErrors;
        if (StringUtils.isBlank(billRegister.getDepartmentCode())
                || departmentService.getDepartmentByCode(billRegister.getDepartmentCode()) == null) {
            restErrors = new RestErrors();
            restErrors.setErrorCode(RestApiConstants.THIRD_PARTY_ERR_CODE_NO_DEPARTMENT);
            restErrors.setErrorMessage(RestApiConstants.THIRD_PARTY_ERR_MSG_NO_DEPARTMENT);
            errors.add(restErrors);
        }
        if (StringUtils.isBlank(billRegister.getFunctionCode())
                || functionService.findByCode(billRegister.getFunctionCode()) == null) {
            restErrors = new RestErrors();
            restErrors.setErrorCode(RestApiConstants.THIRD_PARTY_ERR_CODE_NO_FUNCTION);
            restErrors.setErrorMessage(RestApiConstants.THIRD_PARTY_ERR_MSG_NO_FUNCTION);
            errors.add(restErrors);
        }
        if (StringUtils.isBlank(billRegister.getProjectCode())) {
            restErrors = new RestErrors();
            restErrors.setErrorCode(RestApiConstants.THIRD_PARTY_ERR_CODE_NO_WINCODE);
            restErrors.setErrorMessage(RestApiConstants.THIRD_PARTY_ERR_MSG_NO_WINCODE);
            errors.add(restErrors);
        }
        if (StringUtils.isBlank(billRegister.getBillType())) {
            restErrors = new RestErrors();
            restErrors.setErrorCode(RestApiConstants.THIRD_PARTY_ERR_CODE_NO_BILLTYPE);
            restErrors.setErrorMessage(RestApiConstants.THIRD_PARTY_ERR_MSG_NO_BILLTYPE);
            errors.add(restErrors);
        }
        if (StringUtils.isBlank(billRegister.getFundCode()) || fundService.findByCode(billRegister.getFundCode()) == null) {
            restErrors = new RestErrors();
            restErrors.setErrorCode(RestApiConstants.THIRD_PARTY_ERR_CODE_NO_FUND);
            restErrors.setErrorMessage(RestApiConstants.THIRD_PARTY_ERR_MSG_NO_FUND);
            errors.add(restErrors);
        }
        if (StringUtils.isBlank(billRegister.getPayTo())) {
            restErrors = new RestErrors();
            restErrors.setErrorCode(RestApiConstants.THIRD_PARTY_ERR_CODE_NO_PAYTO);
            restErrors.setErrorMessage(RestApiConstants.THIRD_PARTY_ERR_MSG_NO_PAYTO);
            errors.add(restErrors);
        }

    }

    private void validateBillDates(@Valid final BillRegister billRegister, final List<RestErrors> errors) {
        final SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
        RestErrors restErrors;
        if (billRegister.getBillDate() == null) {
            restErrors = new RestErrors();
            restErrors.setErrorCode(RestApiConstants.THIRD_PARTY_ERR_CODE_NO_BILLDATE);
            restErrors.setErrorMessage(RestApiConstants.THIRD_PARTY_ERR_MSG_NO_BILLDATE);
            errors.add(restErrors);
        } else if (billRegister.getBillDate().after(new Date())) {
            restErrors = new RestErrors();
            restErrors.setErrorCode(RestApiConstants.THIRD_PARTY_ERR_CODE_DATE_CANNOT_BE_FUTTURE);
            restErrors.setErrorMessage(
                    sdf.format(billRegister.getBillDate()) + " - " + RestApiConstants.THIRD_PARTY_ERR_MSG_DATE_CANNOT_BE_FUTTURE);
            errors.add(restErrors);
        }else
            try {
                financialYearHibernateDAO.getFinancialYearByDate(billRegister.getBillDate());
            } catch (final Exception e) {
                restErrors = new RestErrors();
                restErrors.setErrorCode(e.getMessage());
                restErrors.setErrorMessage(e.getMessage());
                errors.add(restErrors);
            }
        if (billRegister.getPartyBillDate() != null
                && billRegister.getPartyBillDate().after(new Date())) {
            restErrors = new RestErrors();
            restErrors.setErrorCode(RestApiConstants.THIRD_PARTY_ERR_CODE_DATE_CANNOT_BE_FUTTURE);
            restErrors.setErrorMessage(
                    sdf.format(billRegister.getPartyBillDate()) + " - "
                            + RestApiConstants.THIRD_PARTY_ERR_MSG_DATE_CANNOT_BE_FUTTURE);
            errors.add(restErrors);
        }
    }

    private void validateBillDetails(@Valid final BillRegister billRegister, final List<RestErrors> errors) {
        RestErrors restErrors;
        if (billRegister.getBillDetails() == null || billRegister.getBillDetails().isEmpty()) {
            restErrors = new RestErrors();
            restErrors.setErrorCode(RestApiConstants.THIRD_PARTY_ERR_CODE_NO_DETAILS);
            restErrors.setErrorMessage(RestApiConstants.THIRD_PARTY_ERR_MSG_NO_DETAILS);
            errors.add(restErrors);
        } else if (billRegister.getBillDetails().size() < 2) {
            restErrors = new RestErrors();
            restErrors.setErrorCode(RestApiConstants.THIRD_PARTY_ERR_CODE_MIN_DETAILS);
            restErrors.setErrorMessage(RestApiConstants.THIRD_PARTY_ERR_MSG_MIN_DETAILS);
            errors.add(restErrors);
        } else
            validateDetails(billRegister, errors);
    }

    private void validateDetails(@Valid final BillRegister billRegister, final List<RestErrors> errors) {
        RestErrors restErrors;
        Accountdetailtype projectCodeAccountDetailType = null;
        Boolean isProjectCodeSubledger = false;
        boolean foundNetPayable = false;
        final List<CChartOfAccounts> contractorPayableAccountList = chartOfAccountsHibernateDAO
                .getAccountCodeByPurposeName(WorksConstants.CONTRACTOR_NETPAYABLE_PURPOSE);
        final List<CChartOfAccounts> advancePayableAccountList = chartOfAccountsHibernateDAO
                .getAccountCodeByPurposeName(RestApiConstants.CONTRACTOR_ADVANCE_PURPOSE);
        BigDecimal creditAmount = BigDecimal.ZERO;
        BigDecimal debitAmount = BigDecimal.ZERO;
        for (final BillDetails billDetails : billRegister.getBillDetails())
            if (StringUtils.isBlank(billDetails.getGlcode())) {
                restErrors = new RestErrors();
                restErrors.setErrorCode(RestApiConstants.THIRD_PARTY_ERR_CODE_NO_DETAIL_GLCODE);
                restErrors.setErrorMessage(RestApiConstants.THIRD_PARTY_ERR_MSG_NO_DETAIL_GLCODE);
                errors.add(restErrors);
            } else {
                final CChartOfAccounts coa = chartOfAccountsService
                        .getByGlCode(billDetails.getGlcode());
                if (coa == null) {
                    restErrors = new RestErrors();
                    restErrors.setErrorCode(RestApiConstants.THIRD_PARTY_ERR_CODE_NO_VALID_GLCODE);
                    restErrors.setErrorMessage(
                            billDetails.getGlcode() + " - " + RestApiConstants.THIRD_PARTY_ERR_MSG_NO_VALID_GLCODE);
                    errors.add(restErrors);
                } else if (coa.getClassification() != 4) {
                    restErrors = new RestErrors();
                    restErrors.setErrorCode(RestApiConstants.THIRD_PARTY_ERR_CODE_NO_VALID_DETAIL_GLCODE);
                    restErrors.setErrorMessage(
                            billDetails.getGlcode() + " - " + RestApiConstants.THIRD_PARTY_ERR_MSG_NO_VALID_DETAIL_GLCODE);
                    errors.add(restErrors);
                }
                if (billDetails.getDebitAmount() == null && billDetails.getCreditAmount() == null) {
                    restErrors = new RestErrors();
                    restErrors.setErrorCode(RestApiConstants.THIRD_PARTY_ERR_CODE_EITHER_CREDIT_DEBIT);
                    restErrors.setErrorMessage(RestApiConstants.THIRD_PARTY_ERR_MSG_EITHER_CREDIT_DEBIT);
                    errors.add(restErrors);
                } else if (billDetails.getCreditAmount() != null && billDetails.getDebitAmount() != null
                        && billDetails.getCreditAmount().doubleValue() > 0 &&
                        billDetails.getDebitAmount().doubleValue() > 0) {
                    restErrors = new RestErrors();
                    restErrors.setErrorCode(RestApiConstants.THIRD_PARTY_ERR_CODE_CREDIT_DEBIT_GREATER_ZERO);
                    restErrors.setErrorMessage(RestApiConstants.THIRD_PARTY_ERR_MSG_CREDIT_DEBIT_GREATER_ZERO);
                    errors.add(restErrors);
                }
                if (billDetails.getCreditAmount() == null
                        && billDetails.getDebitAmount().compareTo(BigDecimal.ZERO) <= 0) {
                    restErrors = new RestErrors();
                    restErrors.setErrorCode(RestApiConstants.THIRD_PARTY_ERR_CODE_AMOUNT_SHOULD_GREATER_THAN_ZERO);
                    restErrors.setErrorMessage(RestApiConstants.THIRD_PARTY_ERR_MSG_AMOUNT_SHOULD_GREATER_THAN_ZERO);
                    errors.add(restErrors);
                }
                if (billDetails.getDebitAmount() != null && billDetails.getDebitAmount().compareTo(BigDecimal.ZERO) > 0) {
                    debitAmount = debitAmount.add(billDetails.getDebitAmount());

                    if (coa != null)
                        projectCodeAccountDetailType = chartOfAccountsHibernateDAO.getAccountDetailTypeIdByName(
                                coa.getGlcode(),
                                WorksConstants.PROJECTCODE);
                    if (projectCodeAccountDetailType != null)
                        isProjectCodeSubledger = true;

                    if (coa != null && !coa.getChartOfAccountDetails().isEmpty()) {
                        Boolean isProjectContractorSubLedger = false;
                        final Set<CChartOfAccountDetail> chartOfAccountDetails = coa.getChartOfAccountDetails();
                        for (final CChartOfAccountDetail detail : chartOfAccountDetails)
                            if (detail.getDetailTypeId().getName().equals(WorksConstants.PROJECTCODE) ||
                                    detail.getDetailTypeId().getName().equals(WorksConstants.ACCOUNTDETAIL_TYPE_CONTRACTOR))
                                isProjectContractorSubLedger = true;
                        if (!isProjectContractorSubLedger) {
                            restErrors = new RestErrors();
                            restErrors
                                    .setErrorCode(RestApiConstants.THIRD_PARTY_ERR_CODE_NOT_PROJECT_CONTRACTOR_SUBLEDGER);
                            restErrors.setErrorMessage(coa.getGlcode() + " - " +
                                    RestApiConstants.THIRD_PARTY_ERR_MSG_NOT_PROJECT_CONTRACTOR_SUBLEDGER);
                            errors.add(restErrors);
                        }
                    }
                } else if (billDetails.getCreditAmount() != null) {
                    creditAmount = creditAmount.add(billDetails.getCreditAmount());
                    if (advancePayableAccountList.contains(coa) || contractorPayableAccountList.contains(coa))
                        foundNetPayable = true;
                    if (contractorPayableAccountList != null && !contractorPayableAccountList.isEmpty()
                            && contractorPayableAccountList.contains(coa)
                            && billDetails.getCreditAmount().compareTo(BigDecimal.ZERO) == -1) {
                        restErrors = new RestErrors();
                        restErrors.setErrorCode(RestApiConstants.THIRD_PARTY_ERR_CODE_AMOUNT_NEGATIVE);
                        restErrors.setErrorMessage(RestApiConstants.THIRD_PARTY_ERR_MSG_AMOUNT_NEGATIVE);
                        errors.add(restErrors);
                    } else if (billDetails.getDebitAmount() == null
                            && billDetails.getCreditAmount().compareTo(BigDecimal.ZERO) <= 0) {
                        restErrors = new RestErrors();
                        restErrors.setErrorCode(RestApiConstants.THIRD_PARTY_ERR_CODE_AMOUNT_SHOULD_GREATER_THAN_ZERO);
                        restErrors.setErrorMessage(RestApiConstants.THIRD_PARTY_ERR_MSG_AMOUNT_SHOULD_GREATER_THAN_ZERO);
                        errors.add(restErrors);
                    }
                }
            }
        if (!isProjectCodeSubledger) {
            restErrors = new RestErrors();
            restErrors.setErrorCode(RestApiConstants.THIRD_PARTY_ERR_CODE_NO_DEBIT_CODE_SUBLEDGER);
            restErrors.setErrorMessage(RestApiConstants.THIRD_PARTY_ERR_MSG_NO_DEBIT_CODE_SUBLEDGER);
            errors.add(restErrors);
        }
        if (!creditAmount.equals(debitAmount)) {
            restErrors = new RestErrors();
            restErrors.setErrorCode(RestApiConstants.THIRD_PARTY_ERR_CODE_NOTEQUAL_CREDIT_DEBIT);
            restErrors.setErrorMessage(RestApiConstants.THIRD_PARTY_ERR_MSG_NOTEQUAL_CREDIT_DEBIT);
            errors.add(restErrors);
        }
        if (!foundNetPayable) {
            restErrors = new RestErrors();
            restErrors.setErrorCode(RestApiConstants.THIRD_PARTY_ERR_CODE_NOT_ADVANCE_CONTRACTOR_PAYABLE);
            restErrors.setErrorMessage(RestApiConstants.THIRD_PARTY_ERR_MSG_NOT_ADVANCE_CONTRACTOR_PAYABLE);
            errors.add(restErrors);
        }
    }

    private void validateBillPayeeDetails(@Valid final BillRegister billRegister, final List<RestErrors> errors) {
        RestErrors restErrors;
        if (billRegister.getBillPayeeDetails() == null || billRegister.getBillPayeeDetails().isEmpty()) {
            restErrors = new RestErrors();
            restErrors.setErrorCode(RestApiConstants.THIRD_PARTY_ERR_CODE_NO_PAYEE_DETAILS);
            restErrors.setErrorMessage(RestApiConstants.THIRD_PARTY_ERR_MSG_NO_PAYEE_DETAILS);
            errors.add(restErrors);
        }

        if (billRegister.getBillPayeeDetails() != null && !billRegister.getBillPayeeDetails().isEmpty())
            for (final BillPayeeDetails billPayeeDetails : billRegister.getBillPayeeDetails()) {
                Boolean isCOAExistInDetails = false;
                if (StringUtils.isBlank(billPayeeDetails.getGlcode())) {
                    restErrors = new RestErrors();
                    restErrors.setErrorCode(RestApiConstants.THIRD_PARTY_ERR_CODE_NO_PAYEE_GLCODE);
                    restErrors.setErrorMessage(RestApiConstants.THIRD_PARTY_ERR_MSG_NO_PAYEE_GLCODE);
                    errors.add(restErrors);
                }
                if (StringUtils.isBlank(billPayeeDetails.getAccountDetailType())) {
                    restErrors = new RestErrors();
                    restErrors.setErrorCode(RestApiConstants.THIRD_PARTY_ERR_CODE_NO_PAYEE_ACCOUNTTYPE);
                    restErrors.setErrorMessage(RestApiConstants.THIRD_PARTY_ERR_MSG_NO_PAYEE_ACCOUNTTYPE);
                    errors.add(restErrors);
                } else if (WorksConstants.ACCOUNTDETAIL_TYPE_CONTRACTOR.equals(billPayeeDetails.getAccountDetailType())
                        && contractorService.getContractorByCode(billPayeeDetails.getAccountDetailKey()) == null) {
                    restErrors = new RestErrors();
                    restErrors.setErrorCode(RestApiConstants.THIRD_PARTY_ERR_CODE_NOT_EXIST_CONTRACTOR);
                    restErrors.setErrorMessage(RestApiConstants.THIRD_PARTY_ERR_MSG_NOT_EXIST_CONTRACTOR);
                    errors.add(restErrors);
                } else if (StringUtils.isNotBlank(billRegister.getProjectCode())
                        && WorksConstants.PROJECTCODE.equals(billPayeeDetails.getAccountDetailType())
                        && !billRegister.getProjectCode().equals(billPayeeDetails.getAccountDetailKey())) {
                    restErrors = new RestErrors();
                    restErrors.setErrorCode(RestApiConstants.THIRD_PARTY_ERR_CODE_PROJECTCODE_NOT_MATCHING);
                    restErrors.setErrorMessage(RestApiConstants.THIRD_PARTY_ERR_MSG_PROJECTCODE_NOT_MATCHING);
                    errors.add(restErrors);
                }
                if (StringUtils.isBlank(billPayeeDetails.getAccountDetailKey())) {
                    restErrors = new RestErrors();
                    restErrors.setErrorCode(RestApiConstants.THIRD_PARTY_ERR_CODE_NO_PAYEE_ACCOUNTKEY);
                    restErrors.setErrorMessage(RestApiConstants.THIRD_PARTY_ERR_MSG_NO_PAYEE_ACCOUNTKEY);
                    errors.add(restErrors);
                }
                if (billPayeeDetails.getDebitAmount() == null && billPayeeDetails.getCreditAmount() == null) {
                    restErrors = new RestErrors();
                    restErrors.setErrorCode(RestApiConstants.THIRD_PARTY_ERR_CODE_PAYEE_EITHER_CREDIT_DEBIT);
                    restErrors.setErrorMessage(RestApiConstants.THIRD_PARTY_ERR_MSG_PAYEE_EITHER_CREDIT_DEBIT + " - "
                            + billPayeeDetails.getGlcode());
                    errors.add(restErrors);
                }
                if (billPayeeDetails.getDebitAmount() != null && billPayeeDetails.getCreditAmount() != null
                        && billPayeeDetails.getDebitAmount().doubleValue() > 0
                        && billPayeeDetails.getCreditAmount().doubleValue() > 0) {
                    restErrors = new RestErrors();
                    restErrors.setErrorCode(RestApiConstants.THIRD_PARTY_ERR_CODE_CREDIT_DEBIT_GREATER_ZERO);
                    restErrors.setErrorMessage(RestApiConstants.THIRD_PARTY_ERR_MSG_CREDIT_DEBIT_GREATER_ZERO);
                    errors.add(restErrors);
                }

                final CChartOfAccounts coa = chartOfAccountsService
                        .getByGlCode(billPayeeDetails.getGlcode());
                if (coa != null && !coa.getChartOfAccountDetails().isEmpty()) {
                    Boolean isProjectContractorSubLedger = false;
                    final Set<CChartOfAccountDetail> chartOfAccountDetails = coa.getChartOfAccountDetails();
                    for (final CChartOfAccountDetail detail : chartOfAccountDetails)
                        if (detail.getDetailTypeId().getName().equals(WorksConstants.PROJECTCODE) ||
                                detail.getDetailTypeId().getName().equals(WorksConstants.ACCOUNTDETAIL_TYPE_CONTRACTOR))
                            isProjectContractorSubLedger = true;
                    if (!isProjectContractorSubLedger) {
                        restErrors = new RestErrors();
                        restErrors
                                .setErrorCode(RestApiConstants.THIRD_PARTY_ERR_CODE_NOT_PROJECT_CONTRACTOR_SUBLEDGER);
                        restErrors.setErrorMessage(
                                RestApiConstants.THIRD_PARTY_ERR_MSG_NOT_PROJECT_CONTRACTOR_SUBLEDGER);
                        errors.add(restErrors);
                    }
                }

                for (final BillDetails billDetails : billRegister.getBillDetails())
                    if (billDetails.getGlcode().equals(billPayeeDetails.getGlcode()))
                        isCOAExistInDetails = true;
                if (!isCOAExistInDetails) {
                    restErrors = new RestErrors();
                    restErrors.setErrorCode(RestApiConstants.THIRD_PARTY_ERR_CODE_PAYEE_GLCODE_NOT_IN_DETAILS);
                    restErrors.setErrorMessage(billPayeeDetails.getGlcode() + " - "
                            + RestApiConstants.THIRD_PARTY_ERR_MSG_PAYEE_GLCODE_NOT_IN_DETAILS);
                    errors.add(restErrors);
                }
            }

        Map<String,BigDecimal> amountMap = new HashMap<>();
        for (final BillPayeeDetails billPayeeDetails : billRegister.getBillPayeeDetails()) {
            if (amountMap.get(billPayeeDetails.getGlcode()) == null) {
                amountMap.put(billPayeeDetails.getGlcode(), billPayeeDetails.getCreditAmount());
            }else{
                if(billPayeeDetails.getCreditAmount() != null && billPayeeDetails.getCreditAmount().doubleValue() > 0){
                    amountMap.put(billPayeeDetails.getGlcode(), amountMap.get(billPayeeDetails.getGlcode()).add(billPayeeDetails.getCreditAmount()));
                }
            }
        }
        for(final BillDetails billDetails : billRegister.getBillDetails()){
            if(billDetails.getCreditAmount() != null && billDetails.getCreditAmount().doubleValue() > 0){
                if (amountMap.containsKey(billDetails.getGlcode()) && amountMap.get(billDetails.getGlcode()).compareTo(billDetails.getCreditAmount()) != 0) {
                    restErrors = new RestErrors();
                    restErrors.setErrorCode(RestApiConstants.THIRD_PARTY_ERR_CODE_NOT_MATCHING_CREDIT_AMOUNT);
                    restErrors.setErrorMessage(RestApiConstants.THIRD_PARTY_ERR_MSG_NOT_MATCHING_CREDIT_AMOUNT + " - "
                            + billDetails.getGlcode());
                    errors.add(restErrors);
                }
            }
        }
    }

    public void populateBillRegister(@Valid final EgBillregister egBillregister, @Valid final BillRegister billRegister)
            throws ClassNotFoundException {
        populateEgBillregister(egBillregister, billRegister);
        populateEgBillregisterMis(egBillregister, billRegister);

        for (final BillDetails details : billRegister.getBillDetails())
            populateEgBilldetails(egBillregister, details, billRegister);
    }

    private void populateEgBillregister(@Valid final EgBillregister egBillregister, @Valid final BillRegister billRegister) {
        egBillregister.setBilldate(billRegister.getBillDate());
        egBillregister.setBilltype(billRegister.getBillType());
        egBillregister.setBillamount(billRegister.getBillAmount());
        egBillregister.setExpendituretype(WorksConstants.BILL_EXPENDITURE_TYPE);
        egBillregister.setBillstatus(WorksConstants.APPROVED);
        egBillregister.setStatus(financialUtils.getStatusByModuleAndCode(WorksConstants.CONTRACTORBILL,
                WorksConstants.APPROVED));
    }

    private void populateEgBillregisterMis(@Valid final EgBillregister egBillregister, @Valid final BillRegister billRegister) {
        final EgBillregistermis egBillregistermis = new EgBillregistermis();
        egBillregistermis.setFunction(functionService.findByCode(billRegister.getFunctionCode()));
        egBillregistermis.setFund(fundService.findByCode(billRegister.getFundCode()));
        egBillregistermis.setScheme(schemeService.findByCode(billRegister.getSchemeCode()));
        egBillregistermis.setSubScheme(subSchemeService.findByCode(billRegister.getSubSchemeCode()));
        egBillregistermis.setEgDepartment(departmentService.getDepartmentByCode(billRegister.getDepartmentCode()));
        egBillregistermis.setPayto(billRegister.getPayTo());
        egBillregistermis.setNarration(billRegister.getNarration());
        egBillregistermis.setPartyBillNumber(billRegister.getPartyBillNumber());
        egBillregistermis.setPartyBillDate(billRegister.getPartyBillDate());
        egBillregistermis.setSourcePath(billRegister.getCheckListUrl());

        egBillregister.setEgBillregistermis(egBillregistermis);
    }

    /**
     * @param egBillregister
     * @param details
     * @param billRegister
     * @throws ClassNotFoundException
     *
     * Bill details population is currently handled for Works Bills, separate population logic should be written if other bills
     * has to be supported
     */
    private void populateEgBilldetails(@Valid final EgBillregister egBillregister, @Valid final BillDetails details,
                                       @Valid final BillRegister billRegister) throws ClassNotFoundException {
        final EgBilldetails egBilldetails = new EgBilldetails();
        Accountdetailtype contractorAccountDetailType;
        Accountdetailtype projectCodeAccountDetailType;
        egBilldetails.setGlcodeid(BigDecimal.valueOf(chartOfAccountsService.getByGlCode(details.getGlcode()).getId()));
        egBilldetails.setCreditamount(details.getCreditAmount() != null ? details.getCreditAmount() : null);
        egBilldetails.setDebitamount(details.getDebitAmount() != null ? details.getDebitAmount() : null);
        egBilldetails.setEgBillregister(egBillregister);
        egBilldetails.setLastupdatedtime(new Date());
        egBilldetails.setFunctionid(BigDecimal.valueOf(egBillregister.getEgBillregistermis().getFunction().getId()));
        for (final BillPayeeDetails payeeDetails : billRegister.getBillPayeeDetails()) {
            final CChartOfAccounts coa = chartOfAccountsService
                    .getByGlCode(payeeDetails.getGlcode());
            if (payeeDetails.getCreditAmount() != null && payeeDetails.getCreditAmount().compareTo(BigDecimal.ZERO) != 0) {
                contractorAccountDetailType = chartOfAccountsHibernateDAO.getAccountDetailTypeIdByName(
                        coa.getGlcode(), WorksConstants.ACCOUNTDETAIL_TYPE_CONTRACTOR);
                if (contractorAccountDetailType != null)
                    populateEgBillPayeedetails(egBilldetails, payeeDetails, details);
            } else if (payeeDetails.getDebitAmount() != null && payeeDetails.getDebitAmount().compareTo(BigDecimal.ZERO) != 0) {
                projectCodeAccountDetailType = chartOfAccountsHibernateDAO.getAccountDetailTypeIdByName(coa.getGlcode(),
                        WorksConstants.PROJECTCODE);
                contractorAccountDetailType = chartOfAccountsHibernateDAO.getAccountDetailTypeIdByName(
                        coa.getGlcode(), WorksConstants.ACCOUNTDETAIL_TYPE_CONTRACTOR);
                if (projectCodeAccountDetailType != null || contractorAccountDetailType != null)
                    populateEgBillPayeedetails(egBilldetails, payeeDetails, details);
            }
            egBillregister.addEgBilldetailes(egBilldetails);
        }
    }

    @SuppressWarnings("unchecked")
    private void populateEgBillPayeedetails(@Valid final EgBilldetails egBilldetails, @Valid final BillPayeeDetails payeeDetails,
                                            @Valid final BillDetails details) throws ClassNotFoundException {
        final EgBillPayeedetails billPayeedetails = new EgBillPayeedetails();
        if (payeeDetails.getGlcode() != null && payeeDetails.getGlcode().equals(details.getGlcode())) {
            if (payeeDetails.getCreditAmount() != null && payeeDetails.getCreditAmount().longValue() > 0)
                billPayeedetails.setCreditAmount(payeeDetails.getCreditAmount());
            if (payeeDetails.getDebitAmount() != null && payeeDetails.getDebitAmount().longValue() > 0)
                billPayeedetails.setDebitAmount(payeeDetails.getDebitAmount());
            final Accountdetailtype detailType = accountdetailtypeService
                    .findByName(payeeDetails.getAccountDetailType());
            billPayeedetails.setAccountDetailTypeId(detailType.getId());
            if (payeeDetails.getAccountDetailType() != null) {
                List<EntityType> entities;
                final String table = detailType.getFullQualifiedName();
                final Class<?> service = Class.forName(table);
                String simpleName = service.getSimpleName();
                simpleName = simpleName.substring(0, 1).toLowerCase() + simpleName.substring(1) + "Service";

                final EntityTypeService entityService = (EntityTypeService) applicationContext.getBean(simpleName);
                entities = (List<EntityType>) entityService
                        .filterActiveEntities(payeeDetails.getAccountDetailKey(), 10, detailType.getId());
                billPayeedetails.setAccountDetailKeyId(entities.get(0).getEntityId());
            }
            billPayeedetails.setEgBilldetailsId(egBilldetails);
            billPayeedetails.setLastUpdatedTime(new Date());
            egBilldetails.addEgBillPayeedetail(billPayeedetails);
        }
    }

    public void createProjectCode(@Valid final BillRegister billRegister) {
        final ProjectCode projectCode = projectCodeService.findActiveProjectCodeByCode(billRegister.getProjectCode());
        if (projectCode == null)
            projectCodeService.createProjectCode(billRegister.getProjectCode(), billRegister.getNameOfWork());
    }

    public EgBillregister createBill(@Valid final EgBillregister egBillregister) {
        return expenseBillService.create(egBillregister, null, null, null, "Create And Approve");
    }

    public List<BillPaymentDetails> getBillAndPaymentDetails(String billNo) {
        return billsService.getBillAndPaymentDetails(billNo);
    }



}
