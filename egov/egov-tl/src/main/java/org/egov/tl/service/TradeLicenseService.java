/*
 *    eGov  SmartCity eGovernance suite aims to improve the internal efficiency,transparency,
 *    accountability and the service delivery of the government  organizations.
 *
 *     Copyright (C) 2018  eGovernments Foundation
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

package org.egov.tl.service;

import org.apache.commons.lang3.StringUtils;
import org.egov.commons.CFinancialYear;
import org.egov.commons.Installment;
import org.egov.commons.dao.EgwStatusHibernateDAO;
import org.egov.commons.dao.InstallmentHibDao;
import org.egov.demand.dao.DemandGenericHibDao;
import org.egov.demand.model.BillReceipt;
import org.egov.demand.model.EgDemand;
import org.egov.demand.model.EgDemandDetails;
import org.egov.demand.model.EgDemandReason;
import org.egov.demand.model.EgDemandReasonMaster;
import org.egov.eis.entity.Assignment;
import org.egov.eis.service.AssignmentService;
import org.egov.eis.service.DesignationService;
import org.egov.eis.service.EisCommonService;
import org.egov.eis.service.PositionMasterService;
import org.egov.infra.admin.master.entity.Department;
import org.egov.infra.admin.master.entity.Module;
import org.egov.infra.admin.master.entity.User;
import org.egov.infra.admin.master.service.CityService;
import org.egov.infra.admin.master.service.DepartmentService;
import org.egov.infra.admin.master.service.ModuleService;
import org.egov.infra.config.persistence.datasource.routing.annotation.ReadOnly;
import org.egov.infra.exception.ApplicationRuntimeException;
import org.egov.infra.filestore.entity.FileStoreMapper;
import org.egov.infra.filestore.service.FileStoreService;
import org.egov.infra.reporting.engine.ReportFormat;
import org.egov.infra.reporting.engine.ReportOutput;
import org.egov.infra.reporting.engine.ReportRequest;
import org.egov.infra.reporting.engine.ReportService;
import org.egov.infra.security.utils.SecurityUtils;
import org.egov.infra.validation.exception.ValidationException;
import org.egov.infra.workflow.entity.State;
import org.egov.infra.workflow.entity.StateHistory;
import org.egov.infra.workflow.matrix.entity.WorkFlowMatrix;
import org.egov.infra.workflow.service.SimpleWorkflowService;
import org.egov.pims.commons.Designation;
import org.egov.pims.commons.Position;
import org.egov.tl.entity.FeeMatrixDetail;
import org.egov.tl.entity.LicenseDemand;
import org.egov.tl.entity.LicenseDocument;
import org.egov.tl.entity.LicenseDocumentType;
import org.egov.tl.entity.LicenseSubCategoryDetails;
import org.egov.tl.entity.NatureOfBusiness;
import org.egov.tl.entity.TradeLicense;
import org.egov.tl.entity.WorkflowBean;
import org.egov.tl.entity.contracts.DemandNoticeForm;
import org.egov.tl.entity.contracts.OnlineSearchForm;
import org.egov.tl.entity.contracts.SearchForm;
import org.egov.tl.entity.enums.RateType;
import org.egov.tl.repository.LicenseDocumentTypeRepository;
import org.egov.tl.repository.LicenseRepository;
import org.egov.tl.repository.SearchTradeRepository;
import org.egov.tl.repository.specs.SearchTradeSpec;
import org.egov.tl.service.es.LicenseApplicationIndexService;
import org.egov.tl.utils.LicenseNumberUtils;
import org.egov.tl.utils.LicenseUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import static java.lang.String.format;
import static java.math.BigDecimal.ZERO;
import static org.apache.commons.lang.StringEscapeUtils.escapeXml;
import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.egov.infra.config.core.ApplicationThreadLocals.getMunicipalityName;
import static org.egov.infra.reporting.engine.ReportFormat.PDF;
import static org.egov.infra.reporting.util.ReportUtil.CONTENT_TYPES;
import static org.egov.infra.utils.ApplicationConstant.NA;
import static org.egov.infra.utils.DateUtils.currentDateToDefaultDateFormat;
import static org.egov.infra.utils.DateUtils.getDefaultFormattedDate;
import static org.egov.infra.utils.DateUtils.toYearFormat;
import static org.egov.infra.utils.FileUtils.addFilesToZip;
import static org.egov.infra.utils.FileUtils.byteArrayToFile;
import static org.egov.infra.utils.FileUtils.toByteArray;
import static org.egov.infra.utils.StringUtils.append;
import static org.egov.tl.utils.Constants.*;
import static org.hibernate.criterion.MatchMode.ANYWHERE;

@Service("tradeLicenseService")
@Transactional(readOnly = true)
public class TradeLicenseService {

    private static final String ARREAR = "arrear";
    private static final String CURRENT = "current";
    private static final String PENALTY = "penalty";
    private static final String ERROR_WF_INITIATOR_NOT_DEFINED = "error.wf.initiator.not.defined";
    private static final String ERROR_WF_NEXT_OWNER_NOT_FOUND = "error.wf.next.owner.not.found";
    private static final String NEW_STATE = "NEW";
    private static final String REVENUE_CLERK_JA_APPROVED = "Revenue Clerk/JA Approved";

    @PersistenceContext
    protected EntityManager entityManager;

    @Autowired
    protected InstallmentHibDao installmentDao;

    @Autowired
    protected LicenseNumberUtils licenseNumberUtils;

    @Autowired
    protected LicenseDocumentTypeService licenseDocumentTypeService;

    @Autowired
    protected AssignmentService assignmentService;

    @Autowired
    protected FileStoreService fileStoreService;

    @Autowired
    protected FeeMatrixService feeMatrixService;

    @Autowired
    protected LicenseDocumentTypeRepository licenseDocumentTypeRepository;

    @Autowired
    protected LicenseApplicationIndexService licenseApplicationIndexService;

    @Autowired
    protected SecurityUtils securityUtils;

    @Autowired
    protected DemandGenericHibDao demandGenericDao;

    @Autowired
    protected ValidityService validityService;

    @Autowired
    @Qualifier("tradeLicenseWorkflowService")
    protected SimpleWorkflowService<TradeLicense> licenseWorkflowService;

    @Autowired
    protected LicenseRepository licenseRepository;

    @Autowired
    protected LicenseStatusService licenseStatusService;

    @Autowired
    protected LicenseAppTypeService licenseAppTypeService;

    @Autowired
    protected PositionMasterService positionMasterService;

    @Autowired
    protected NatureOfBusinessService natureOfBusinessService;

    @Autowired
    protected EgwStatusHibernateDAO egwStatusHibernateDAO;

    @Autowired
    protected DesignationService designationService;

    @Autowired
    protected LicenseConfigurationService licenseConfigurationService;

    @Autowired
    protected TradeLicenseSmsAndEmailService tradeLicenseSmsAndEmailService;

    @Autowired
    private PenaltyRatesService penaltyRatesService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private SubCategoryDetailsService subCategoryDetailsService;

    @Autowired
    private FeeTypeService feeTypeService;

    @Autowired
    private LicenseCitizenPortalService licenseCitizenPortalService;

    @Autowired
    private ModuleService moduleService;

    @Autowired
    private ReportService reportService;

    @Autowired
    private LicenseUtils licenseUtils;

    @Autowired
    private SearchTradeRepository searchTradeRepository;

    @Autowired
    private CityService cityService;

    @Autowired
    private EisCommonService eisCommonService;

    public Module getModuleName() {
        return moduleService.getModuleByName(TRADE_LICENSE);
    }

    public TradeLicense getLicenseById(Long id) {
        return this.licenseRepository.findOne(id);
    }

    private List<Assignment> getAssignments() {
        Department nextAssigneeDept = departmentService.getDepartmentByCode(PUBLIC_HEALTH_DEPT_CODE);
        Designation nextAssigneeDesig = designationService.getDesignationByName(JA_DESIGNATION);
        List<Assignment> assignmentList = getAssignmentsForDeptAndDesignation(nextAssigneeDept, nextAssigneeDesig);
        if (assignmentList.isEmpty()) {
            nextAssigneeDesig = Optional.ofNullable(designationService.getDesignationByName(SA_DESIGNATION)).
                    orElseThrow(() -> new ValidationException(ERROR_WF_INITIATOR_NOT_DEFINED, ERROR_WF_INITIATOR_NOT_DEFINED));
            assignmentList = getAssignmentsForDeptAndDesignation(nextAssigneeDept, nextAssigneeDesig);
        }
        if (assignmentList.isEmpty()) {
            nextAssigneeDesig = Optional.ofNullable(designationService.getDesignationByName(RC_DESIGNATION)).
                    orElseThrow(() -> new ValidationException(ERROR_WF_INITIATOR_NOT_DEFINED, ERROR_WF_INITIATOR_NOT_DEFINED));
            assignmentList = getAssignmentsForDeptAndDesignation(nextAssigneeDept, nextAssigneeDesig);
        }
        return assignmentList;
    }

    private List<Assignment> getAssignmentsForDeptAndDesignation(Department nextAssigneeDept, Designation nextAssigneeDesig) {
        return assignmentService.
                findAllAssignmentsByDeptDesigAndDates(nextAssigneeDept.getId(), nextAssigneeDesig.getId(), new Date());
    }

    public void raiseNewDemand(TradeLicense license) {
        final LicenseDemand ld = new LicenseDemand();
        final Module moduleName = this.getModuleName();
        final Installment installment = this.installmentDao.getInsatllmentByModuleForGivenDate(moduleName,
                license.getCommencementDate());
        ld.setIsHistory("N");
        ld.setEgInstallmentMaster(installment);
        ld.setLicense(license);
        ld.setIsLateRenewal('0');
        ld.setCreateDate(new Date());
        ld.setModifiedDate(new Date());
        final List<FeeMatrixDetail> feeMatrixDetails = this.feeMatrixService.getLicenseFeeDetails(license,
                license.getCommencementDate());
        for (final FeeMatrixDetail fm : feeMatrixDetails) {
            final EgDemandReasonMaster reasonMaster = this.demandGenericDao
                    .getDemandReasonMasterByCode(fm.getFeeMatrix().getFeeType().getName(), moduleName);
            final EgDemandReason reason = this.demandGenericDao.getDmdReasonByDmdReasonMsterInstallAndMod(reasonMaster, installment, moduleName);
            if (fm.getFeeMatrix().getFeeType().getName().contains("Late"))
                continue;

            if (reason != null) {
                BigDecimal tradeAmt = calculateAmountByRateType(license, fm);
                ld.getEgDemandDetails().add(EgDemandDetails.fromReasonAndAmounts(tradeAmt, reason, ZERO));
            }
        }

        calcPenaltyDemandDetails(license, ld);
        ld.recalculateBaseDemand();
        license.setLicenseDemand(ld);
    }

    private BigDecimal calculateAmountByRateType(TradeLicense license, FeeMatrixDetail feeMatrixDetail) {
        Long feeTypeId = feeTypeService.findByName(LICENSE_FEE_TYPE).getId();
        LicenseSubCategoryDetails licenseSubCategoryDetails = subCategoryDetailsService.getSubcategoryDetailBySubcategoryAndFeeType(license.getTradeName().getId(), feeTypeId);
        BigDecimal amt = ZERO;
        if (licenseSubCategoryDetails != null) {
            if (RateType.FLAT_BY_RANGE.equals(licenseSubCategoryDetails.getRateType()))
                amt = feeMatrixDetail.getAmount();
            else if (RateType.PERCENTAGE.equals(licenseSubCategoryDetails.getRateType()))
                amt = license.getTradeArea_weight().multiply(feeMatrixDetail.getAmount())
                        .divide(BigDecimal.valueOf(100));
            else if (RateType.UNIT_BY_RANGE.equals(licenseSubCategoryDetails.getRateType()))
                amt = license.getTradeArea_weight().multiply(feeMatrixDetail.getAmount());
        }
        return amt;
    }

    public TradeLicense updateDemandForChangeTradeArea(TradeLicense license) {
        final LicenseDemand licenseDemand = license.getLicenseDemand();
        Date date = new Date();
        final Set<EgDemandDetails> demandDetails = licenseDemand.getEgDemandDetails();
        final Date licenseDate = license.isNewApplication() ? license.getCommencementDate()
                : license.getLicenseDemand().getEgInstallmentMaster().getFromDate();
        final List<FeeMatrixDetail> feeList = this.feeMatrixService.getLicenseFeeDetails(license, licenseDate);
        for (final EgDemandDetails dmd : demandDetails)
            for (final FeeMatrixDetail fm : feeList)
                if (licenseDemand.getEgInstallmentMaster().equals(dmd.getEgDemandReason().getEgInstallmentMaster()) &&
                        dmd.getEgDemandReason().getEgDemandReasonMaster().getCode()
                                .equalsIgnoreCase(fm.getFeeMatrix().getFeeType().getName())) {
                    BigDecimal tradeAmt = calculateAmountByRateType(license, fm);
                    dmd.setAmount(tradeAmt);
                    dmd.setModifiedDate(date);
                }
        calcPenaltyDemandDetails(license, licenseDemand);
        licenseDemand.recalculateBaseDemand();
        return license;

    }

    public void calcPenaltyDemandDetails(TradeLicense license, EgDemand demand) {
        Map<Installment, BigDecimal> installmentPenalty = new HashMap<>();
        Map<Installment, EgDemandDetails> penaltyDetails = getInstallmentWisePenaltyDemandDetails(demand);
        Map<Installment, EgDemandDetails> demandDetails = getInstallmentWiseLicenseDemandDetails(demand);
        if (license.isNewApplication())
            installmentPenalty = getCalculatedPenalty(license, license.getCommencementDate(), new Date(), demand);
        else if (license.isReNewApplication())
            installmentPenalty = getCalculatedPenalty(license, null, new Date(), demand);
        for (final Map.Entry<Installment, BigDecimal> penalty : installmentPenalty.entrySet()) {
            EgDemandDetails penaltyDemandDetail = penaltyDetails.get(penalty.getKey());
            EgDemandDetails licenseDemandDetail = demandDetails.get(penalty.getKey());
            if (penalty.getValue().signum() > 0) {
                if (penaltyDemandDetail != null && licenseDemandDetail.getBalance().signum() > 0)
                    penaltyDemandDetail.setAmount(penalty.getValue().setScale(0, RoundingMode.HALF_UP));
                else if (licenseDemandDetail.getBalance().signum() > 0) {
                    penaltyDemandDetail = insertPenaltyDmdDetail(penalty.getKey(), penalty.getValue().setScale(0, RoundingMode.HALF_UP));
                    if (penaltyDemandDetail != null)
                        demand.getEgDemandDetails().add(penaltyDemandDetail);
                }
            } else if (penalty.getValue().signum() == 0 && penaltyDemandDetail != null) {
                penaltyDemandDetail.setAmount(penalty.getValue().setScale(0, RoundingMode.HALF_UP));
            }
        }
    }

    private Map<Installment, EgDemandDetails> getInstallmentWisePenaltyDemandDetails(final EgDemand currentDemand) {
        final Map<Installment, EgDemandDetails> penaltyDemandDetails = new TreeMap<>();
        if (currentDemand != null)
            for (final EgDemandDetails dmdDet : currentDemand.getEgDemandDetails())
                if (dmdDet.getEgDemandReason().getEgDemandReasonMaster().getCode().equals(PENALTY_DMD_REASON_CODE))
                    penaltyDemandDetails.put(dmdDet.getEgDemandReason().getEgInstallmentMaster(), dmdDet);

        return penaltyDemandDetails;
    }

    private Map<Installment, EgDemandDetails> getInstallmentWiseLicenseDemandDetails(final EgDemand currentDemand) {
        final Map<Installment, EgDemandDetails> demandDetails = new TreeMap<>();
        if (currentDemand != null)
            for (final EgDemandDetails dmdDet : currentDemand.getEgDemandDetails())
                if (!dmdDet.getEgDemandReason().getEgDemandReasonMaster().getCode().equals(PENALTY_DMD_REASON_CODE))
                    demandDetails.put(dmdDet.getEgDemandReason().getEgInstallmentMaster(), dmdDet);

        return demandDetails;
    }

    private Map<Installment, BigDecimal> getCalculatedPenalty(TradeLicense license, Date fromDate, Date collectionDate,
                                                              EgDemand demand) {
        final Map<Installment, BigDecimal> installmentPenalty = new HashMap<>();
        for (final EgDemandDetails demandDetails : demand.getEgDemandDetails()) {
            if (!demandDetails.getEgDemandReason().getEgDemandReasonMaster().getCode().equals(PENALTY_DMD_REASON_CODE)
                    && demandDetails.getAmount().subtract(demandDetails.getAmtCollected()).signum() >= 0) {
                if (fromDate == null) {
                    installmentPenalty.put(demandDetails.getEgDemandReason().getEgInstallmentMaster(),
                            penaltyRatesService.calculatePenalty(license, demandDetails.getEgDemandReason().getEgInstallmentMaster().getFromDate(),
                                    collectionDate, demandDetails.getAmount()));
                } else {
                    installmentPenalty.put(demandDetails.getEgDemandReason().getEgInstallmentMaster(),
                            penaltyRatesService.calculatePenalty(license, fromDate, collectionDate, demandDetails.getAmount()));
                }
            }
        }
        return installmentPenalty;
    }

    private EgDemandDetails insertPenaltyDmdDetail(Installment inst, BigDecimal penaltyAmount) {
        EgDemandDetails demandDetail = null;
        if (penaltyAmount != null && penaltyAmount.compareTo(ZERO) > 0) {
            Module module = getModuleName();
            final EgDemandReasonMaster egDemandReasonMaster = demandGenericDao.getDemandReasonMasterByCode(
                    PENALTY_DMD_REASON_CODE,
                    module);
            if (egDemandReasonMaster == null)
                throw new ApplicationRuntimeException(" Penalty Demand reason Master is null in method  insertPenalty");

            final EgDemandReason egDemandReason = demandGenericDao.getDmdReasonByDmdReasonMsterInstallAndMod(
                    egDemandReasonMaster, inst, module);

            if (egDemandReason == null)
                throw new ApplicationRuntimeException(" Penalty Demand reason is null in method  insertPenalty ");

            demandDetail = createDemandDetails(egDemandReason, ZERO, penaltyAmount);
        }
        return demandDetail;
    }

    private EgDemandDetails createDemandDetails(final EgDemandReason egDemandReason, final BigDecimal amtCollected,
                                                final BigDecimal dmdAmount) {
        return EgDemandDetails.fromReasonAndAmounts(dmdAmount, egDemandReason, amtCollected);
    }

    public void recalculateDemand(final List<FeeMatrixDetail> feeList, TradeLicense license) {
        final LicenseDemand licenseDemand = license.getCurrentDemand();
        // Recalculating current demand detail according to fee matrix
        for (final EgDemandDetails dmd : licenseDemand.getEgDemandDetails())
            for (final FeeMatrixDetail fm : feeList)
                if (licenseDemand.getEgInstallmentMaster().equals(dmd.getEgDemandReason().getEgInstallmentMaster()) &&
                        dmd.getEgDemandReason().getEgDemandReasonMaster().getCode()
                                .equalsIgnoreCase(fm.getFeeMatrix().getFeeType().getName())) {
                    BigDecimal tradeAmt = calculateAmountByRateType(license, fm);
                    dmd.setAmount(tradeAmt.setScale(0, RoundingMode.HALF_UP));
                }
        calcPenaltyDemandDetails(license, licenseDemand);
        licenseDemand.recalculateBaseDemand();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void raiseDemand(TradeLicense licenze, final Module module, final Installment installment) {
        // Refetching license in this txn to avoid lazy initialization issue
        TradeLicense license = licenseRepository.findOne(licenze.getId());
        Map<EgDemandReason, EgDemandDetails> reasonWiseDemandDetails = getReasonWiseDemandDetails(license.getLicenseDemand());
        license.setLicenseAppType(licenseAppTypeService.getLicenseAppTypeByCode(RENEW_APPTYPE_CODE));
        for (FeeMatrixDetail feeMatrixDetail : feeMatrixService.getLicenseFeeDetails(license, installment.getFromDate())) {
            String feeType = feeMatrixDetail.getFeeMatrix().getFeeType().getName();
            if (feeType.contains("Late"))
                continue;
            EgDemandReason reason = demandGenericDao.getDmdReasonByDmdReasonMsterInstallAndMod(
                    demandGenericDao.getDemandReasonMasterByCode(feeType, module), installment, module);
            if (reason == null)
                throw new ValidationException("TL-007", "Demand reason missing for " + feeType);
            EgDemandDetails licenseDemandDetail = reasonWiseDemandDetails.get(reason);
            BigDecimal tradeAmt = calculateAmountByRateType(license, feeMatrixDetail);
            if (licenseDemandDetail == null)
                license.getLicenseDemand().getEgDemandDetails()
                        .add(EgDemandDetails.fromReasonAndAmounts(tradeAmt, reason, ZERO));
            else if (licenseDemandDetail.getBalance().compareTo(ZERO) != 0)
                licenseDemandDetail.setAmount(tradeAmt);
            if (license.getCurrentDemand().getEgInstallmentMaster().getInstallmentYear().before(installment.getInstallmentYear()))
                license.getLicenseDemand().setEgInstallmentMaster(installment);

        }
        license.getLicenseDemand().recalculateBaseDemand();
        licenseRepository.save(license);
    }

    public Map<EgDemandReason, EgDemandDetails> getReasonWiseDemandDetails(final EgDemand currentDemand) {
        final Map<EgDemandReason, EgDemandDetails> reasonWiseDemandDetails = new HashMap<>();
        if (currentDemand != null)
            for (final EgDemandDetails demandDetail : currentDemand.getEgDemandDetails())
                if (LICENSE_FEE_TYPE.equals(demandDetail.getEgDemandReason().getEgDemandReasonMaster().getCode()))
                    reasonWiseDemandDetails.put(demandDetail.getEgDemandReason(), demandDetail);
        return reasonWiseDemandDetails;
    }

    public void transitionWorkFlow(TradeLicense license, final WorkflowBean workflowBean) {
        DateTime currentDate = new DateTime();
        User user = this.securityUtils.getCurrentUser();
        if (BUTTONREJECT.equalsIgnoreCase(workflowBean.getWorkFlowAction())) {
            Position initiatorPosition = license.getCurrentState().getInitiatorPosition();
            List<Position> userPositions = positionMasterService.getPositionsForEmployee(securityUtils.getCurrentUser().getId());
            if (userPositions.contains(initiatorPosition) && ("Rejected".equals(license.getState().getValue())
                    || "License Created".equals(license.getState().getValue())))
                license.transition().end().withSenderName(user.getUsername() + DELIMITER_COLON + user.getName())
                        .withComments(workflowBean.getApproverComments())
                        .withDateInfo(currentDate.toDate());
            else {
                final String stateValue = WORKFLOW_STATE_REJECTED;
                license.transition().progressWithStateCopy().withSenderName(user.getUsername() + DELIMITER_COLON + user.getName())
                        .withComments(workflowBean.getApproverComments())
                        .withStateValue(stateValue).withDateInfo(currentDate.toDate())
                        .withOwner(initiatorPosition)
                        .withNextAction(WF_STATE_SANITORY_INSPECTOR_APPROVAL_PENDING);
            }

        } else if (GENERATECERTIFICATE.equalsIgnoreCase(workflowBean.getWorkFlowAction())) {
            final WorkFlowMatrix wfmatrix = this.licenseWorkflowService.getWfMatrix(license.getStateType(), null,
                    null, workflowBean.getAdditionaRule(), license.getCurrentState().getValue(), null);
            license.transition().end().withSenderName(user.getUsername() + DELIMITER_COLON + user.getName())
                    .withComments(workflowBean.getApproverComments())
                    .withStateValue(wfmatrix.getNextState()).withDateInfo(currentDate.toDate())
                    .withOwner(license.getCurrentState().getInitiatorPosition())
                    .withNextAction(wfmatrix.getNextAction());
        } else {
            if (!license.hasState()) {
                Position wfInitiator;
                List<Assignment> assignments = assignmentService.getAllActiveEmployeeAssignmentsByEmpId(user.getId());
                if (assignments.isEmpty()) {
                    throw new ValidationException(ERROR_WF_INITIATOR_NOT_DEFINED, "No officials assigned to process this application");
                } else {
                    wfInitiator = assignments.get(0).getPosition();
                }
                final WorkFlowMatrix wfmatrix = this.licenseWorkflowService.getWfMatrix(license.getStateType(), null,
                        null, workflowBean.getAdditionaRule(), workflowBean.getCurrentState(), null);
                license.transition().start().withSenderName(user.getUsername() + DELIMITER_COLON + user.getName())
                        .withComments(workflowBean.getApproverComments())
                        .withNatureOfTask(license.getLicenseAppType().getName())
                        .withStateValue(wfmatrix.getNextState()).withDateInfo(currentDate.toDate()).withOwner(wfInitiator)
                        .withNextAction(wfmatrix.getNextAction()).withInitiator(wfInitiator);
                license.setEgwStatus(
                        egwStatusHibernateDAO.getStatusByModuleAndCode(TRADELICENSEMODULE, APPLICATION_STATUS_CREATED_CODE));
            } else if (BUTTONAPPROVE.equalsIgnoreCase(workflowBean.getWorkFlowAction())) {
                Position commissioner = getCommissionerPosition();
                if (APPLICATION_STATUS_APPROVED_CODE.equals(license.getEgwStatus().getCode())) {
                    if (licenseConfigurationService.digitalSignEnabled())
                        license.transition().progressWithStateCopy()
                                .withSenderName(user.getUsername() + DELIMITER_COLON + user.getName())
                                .withComments(workflowBean.getApproverComments())
                                .withStateValue(WF_ACTION_DIGI_SIGN_COMMISSION_NO_COLLECTION)
                                .withDateInfo(currentDate.toDate())
                                .withOwner(commissioner)
                                .withNextAction(WF_ACTION_DIGI_PENDING);
                    else
                        license.transition().progressWithStateCopy()
                                .withSenderName(user.getUsername() + DELIMITER_COLON + user.getName())
                                .withComments(workflowBean.getApproverComments())
                                .withStateValue(WF_COMMISSIONER_APPRVD_WITHOUT_COLLECTION)
                                .withDateInfo(currentDate.toDate())
                                .withOwner(license.getCurrentState().getInitiatorPosition())
                                .withNextAction(WF_CERTIFICATE_GEN_PENDING);
                } else if (APPLICATION_STATUS_SECONDCOLLECTION_CODE.equals(license.getEgwStatus().getCode())) {
                    final WorkFlowMatrix wfmatrix = this.licenseWorkflowService.getWfMatrix(license.getStateType(), null,
                            null, workflowBean.getAdditionaRule(), license.getCurrentState().getValue(), null);
                    license.transition().progressWithStateCopy()
                            .withSenderName(user.getUsername() + DELIMITER_COLON + user.getName())
                            .withComments(workflowBean.getApproverComments())
                            .withStateValue(wfmatrix.getNextState()).withDateInfo(currentDate.toDate())
                            .withOwner(commissioner)
                            .withNextAction(wfmatrix.getNextAction());
                }

            } else {
                Position pos = null;
                if (workflowBean.getApproverPositionId() != null && workflowBean.getApproverPositionId() > 0)
                    pos = positionMasterService.getPositionById(workflowBean.getApproverPositionId());
                final WorkFlowMatrix wfmatrix = this.licenseWorkflowService.getWfMatrix(license.getStateType(), null,
                        null, workflowBean.getAdditionaRule(), license.getCurrentState().getValue(), null);
                license.transition().progressWithStateCopy()
                        .withSenderName(user.getUsername() + DELIMITER_COLON + user.getName())
                        .withComments(workflowBean.getApproverComments())
                        .withStateValue(wfmatrix.getNextState()).withDateInfo(currentDate.toDate())
                        .withOwner(pos)
                        .withNextAction(wfmatrix.getNextAction());
            }

        }
    }

    public Position getCommissionerPosition() {
        return positionMasterService.getPositionsForEmployee(securityUtils.getCurrentUser().getId())
                .stream()
                .filter(position -> position.getDeptDesig().getDesignation().getName().equals(COMMISSIONER_DESGN))
                .findFirst()
                .orElseThrow(
                        () -> new ValidationException("error.wf.comm.pos.not.found", "You are not authorized approve this application"));
    }

    public WorkFlowMatrix getWorkFlowMatrixApi(TradeLicense license, WorkflowBean workflowBean) {
        return this.licenseWorkflowService.getWfMatrix(license.getStateType(), null,
                null, workflowBean.getAdditionaRule(), workflowBean.getCurrentState(), null);
    }

    public void processAndStoreDocument(TradeLicense license) {
        license.getDocuments().forEach(document -> {
            document.setType(licenseDocumentTypeRepository.findOne(document.getType().getId()));
            if (!(document.getUploads().isEmpty() || document.getUploadsFileName().isEmpty())) {
                int fileCount = 0;
                for (final File file : document.getUploads()) {
                    final FileStoreMapper fileStore = this.fileStoreService.store(file,
                            document.getUploadsFileName().get(fileCount),
                            document.getUploadsContentType().get(fileCount++), "EGTL");
                    document.getFiles().add(fileStore);
                }
                document.setEnclosed(true);
                document.setDocDate(new Date());
            } else if (document.getType().isMandatory() && document.getFiles().isEmpty() && document.getId() == null) {
                document.getFiles().clear();
                throw new ValidationException("TL-004", "TL-004", document.getType().getName());
            }
            document.setLicense(license);
        });
    }

    public List<NatureOfBusiness> getAllNatureOfBusinesses() {
        return natureOfBusinessService.getNatureOfBusinesses();
    }

    public TradeLicense getLicenseByLicenseNumber(final String licenseNumber) {
        return this.licenseRepository.findByLicenseNumber(licenseNumber);
    }

    public TradeLicense getLicenseByApplicationNumber(final String applicationNumber) {
        return this.licenseRepository.findByApplicationNumber(applicationNumber);
    }

    public Map<String, Map<String, BigDecimal>> getOutstandingFee(TradeLicense license) {
        final Map<String, Map<String, BigDecimal>> outstandingFee = new HashMap<>();
        final LicenseDemand licenseDemand = license.getCurrentDemand();
        for (final EgDemandDetails demandDetail : licenseDemand.getEgDemandDetails()) {
            final String demandReason = demandDetail.getEgDemandReason().getEgDemandReasonMaster().getReasonMaster();
            final Installment installmentYear = demandDetail.getEgDemandReason().getEgInstallmentMaster();
            Map<String, BigDecimal> feeByTypes;
            if (outstandingFee.containsKey(demandReason))
                feeByTypes = outstandingFee.get(demandReason);
            else {
                feeByTypes = new HashMap<>();
                feeByTypes.put(ARREAR, ZERO);
                feeByTypes.put(CURRENT, ZERO);
            }
            final BigDecimal demandAmount = demandDetail.getAmount().subtract(demandDetail.getAmtCollected());
            if (installmentYear.equals(licenseDemand.getEgInstallmentMaster()))
                feeByTypes.put(CURRENT, demandAmount);
            else
                feeByTypes.put(ARREAR, feeByTypes.get(ARREAR).add(demandAmount));
            outstandingFee.put(demandReason, feeByTypes);
        }
        return outstandingFee;

    }

    /**
     * This method will return arrears, current tax and penalty on arrears tax.
     *
     * @param license
     * @param currentInstallment
     * @param previousInstallment
     * @return
     */
    public Map<String, Map<String, BigDecimal>> getOutstandingFeeForDemandNotice(TradeLicense license,
                                                                                 final Installment currentInstallment, final Installment previousInstallment) {
        final Map<String, Map<String, BigDecimal>> outstandingFee = new HashMap<>();

        final LicenseDemand licenseDemand = license.getCurrentDemand();
        // 31st december will be considered as cutoff date for penalty calculation.
        final Date endDateOfPreviousFinancialYear = new DateTime(previousInstallment.getFromDate()).withMonthOfYear(12)
                .withDayOfMonth(31).toDate();

        for (final EgDemandDetails demandDetail : licenseDemand.getEgDemandDetails()) {
            final String demandReason = demandDetail.getEgDemandReason().getEgDemandReasonMaster().getReasonMaster();
            final Installment installmentYear = demandDetail.getEgDemandReason().getEgInstallmentMaster();
            Map<String, BigDecimal> feeByTypes;
            if (!demandReason.equalsIgnoreCase(PENALTY_DMD_REASON_CODE)) {
                if (outstandingFee.containsKey(demandReason))
                    feeByTypes = outstandingFee.get(demandReason);
                else {
                    feeByTypes = new HashMap<>();
                    feeByTypes.put(ARREAR, ZERO);
                    feeByTypes.put(CURRENT, ZERO);
                    feeByTypes.put(PENALTY, ZERO);
                }
                final BigDecimal demandAmount = demandDetail.getAmount().subtract(demandDetail.getAmtCollected());

                if (demandAmount.compareTo(BigDecimal.valueOf(0)) > 0)
                    if (installmentYear.equals(currentInstallment))
                        feeByTypes.put(CURRENT, feeByTypes.get(CURRENT).add(demandAmount));
                    else {
                        feeByTypes.put(ARREAR, feeByTypes.get(ARREAR).add(demandAmount));
                        // Calculate penalty by passing installment startdate and end of dec 31st date of previous installment
                        // dates using penalty master.
                        final BigDecimal penaltyAmt = penaltyRatesService.calculatePenalty(license, installmentYear.getFromDate(),
                                endDateOfPreviousFinancialYear, demandAmount);
                        feeByTypes.put(PENALTY, feeByTypes.get(PENALTY).add(penaltyAmt));
                    }
                outstandingFee.put(demandReason, feeByTypes);
            }
        }

        return outstandingFee;

    }

    public BigDecimal calculateFeeAmount(final TradeLicense license) {
        final Date licenseDate = license.isNewApplication() ? license.getCommencementDate()
                : license.getLicenseDemand().getEgInstallmentMaster().getFromDate();
        final List<FeeMatrixDetail> feeList = this.feeMatrixService.getLicenseFeeDetails(license, licenseDate);
        BigDecimal totalAmount = ZERO;
        for (final FeeMatrixDetail fm : feeList) {
            BigDecimal tradeAmt = calculateAmountByRateType(license, fm);
            totalAmount = totalAmount.add(tradeAmt);
        }
        return totalAmount;
    }

    public BigDecimal recalculateLicenseFee(final LicenseDemand licenseDemand) {
        BigDecimal licenseFee = ZERO;
        for (final EgDemandDetails demandDetail : licenseDemand.getEgDemandDetails())
            if (demandDetail.getEgDemandReason().getEgDemandReasonMaster().getReasonMaster().equals(LICENSE_FEE_TYPE)
                    && licenseDemand.getEgInstallmentMaster().equals(demandDetail.getEgDemandReason().getEgInstallmentMaster()))
                licenseFee = licenseFee.add(demandDetail.getAmtCollected());
        return licenseFee;
    }

    @Transactional
    public TradeLicense saveClosure(TradeLicense license, final WorkflowBean workflowBean) {
        if (license.hasState() && !license.getState().isEnded())
            throw new ValidationException("lic.appl.wf.validation", "Cannot initiate Closure process, application under processing");
        license.setNewWorkflow(false);
        Position position = null;
        if (workflowBean.getApproverPositionId() != null) {
            position = positionMasterService.getPositionById(workflowBean.getApproverPositionId());
        }
        if (license.getState() == null || license.hasState() && license.getState().isEnded()) {
            final WorkFlowMatrix wfmatrix = this.licenseWorkflowService.getWfMatrix(license.getStateType(), null,
                    null, workflowBean.getAdditionaRule(), NEW_STATE, null);
            final List<Assignment> assignments = assignmentService.getAllActiveEmployeeAssignmentsByEmpId(this.securityUtils.getCurrentUser().getId());
            if (securityUtils.currentUserIsEmployee()) {
                Position wfInitiator = null;
                if (license.getState() == null || license.transitionCompleted()) {
                    if (!assignments.isEmpty())
                        wfInitiator = assignments.get(0).getPosition();
                    else
                        throw new ValidationException(ERROR_WF_NEXT_OWNER_NOT_FOUND, "No employee assigned to process Closure application", "Closure");
                }
                if (license.hasState()) {
                    license.transition().startNext();
                } else {
                    license.transition().start();
                }
                User currentUser = this.securityUtils.getCurrentUser();
                license.transition()
                        .withSenderName(currentUser.getUsername() + DELIMITER_COLON + currentUser.getName())
                        .withComments(workflowBean.getApproverComments()).withNatureOfTask(license.getLicenseAppType().getName())
                        .withStateValue(wfmatrix.getNextState()).withDateInfo(new DateTime().toDate()).withOwner(position)
                        .withNextAction(wfmatrix.getNextAction()).withInitiator(wfInitiator).withExtraInfo(license.getLicenseAppType().getName());
            } else
                closureWfWithOperator(license);
            if (!currentUserIsMeeseva())
                license.setApplicationNumber(licenseNumberUtils.generateApplicationNumber());
            license.setEgwStatus(egwStatusHibernateDAO
                    .getStatusByModuleAndCode(TRADELICENSEMODULE, APPLICATION_STATUS_CREATED_CODE));
            license.setStatus(licenseStatusService.getLicenseStatusByName(LICENSE_STATUS_ACKNOWLEDGED));
            license.setLicenseAppType(licenseAppTypeService.getClosureLicenseApplicationType());
            tradeLicenseSmsAndEmailService.sendLicenseClosureMessage(license, workflowBean.getWorkFlowAction());

        }
        this.licenseRepository.save(license);
        if (securityUtils.currentUserIsCitizen())
            licenseCitizenPortalService.onCreate(license);
        licenseApplicationIndexService.createOrUpdateLicenseApplicationIndex(license);
        return license;
    }

    @Transactional
    public void cancelLicenseWorkflow(TradeLicense license, final WorkflowBean workflowBean) {
        final User currentUser = this.securityUtils.getCurrentUser();
        Position owner = null;
        if (workflowBean.getApproverPositionId() != null)
            owner = positionMasterService.getPositionById(workflowBean.getApproverPositionId());
        final WorkFlowMatrix wfmatrix = this.licenseWorkflowService.getWfMatrix(license.getStateType(), null,
                null, workflowBean.getAdditionaRule(), workflowBean.getCurrentState(), null);
        if (workflowBean.getWorkFlowAction() != null && workflowBean.getWorkFlowAction().contains(BUTTONREJECT))
            if (WORKFLOW_STATE_REJECTED.equals(license.getState().getValue())) {
                license.setEgwStatus(egwStatusHibernateDAO
                        .getStatusByModuleAndCode(TRADELICENSEMODULE, APPLICATION_STATUS_GENECERT_CODE));
                license.setStatus(licenseStatusService.getLicenseStatusByName(LICENSE_STATUS_ACTIVE));
                license.setActive(true);
                if (license.getState().getExtraInfo() != null)
                    license.setLicenseAppType(licenseAppTypeService.getLicenseAppTypeByName(license.getState().getExtraInfo()));
                license.transition().end().withSenderName(currentUser.getUsername() + DELIMITER_COLON + currentUser.getName())
                        .withComments(workflowBean.getApproverComments())
                        .withDateInfo(new DateTime().toDate());
            } else {
                license.setEgwStatus(egwStatusHibernateDAO
                        .getStatusByModuleAndCode(TRADELICENSEMODULE, APPLICATION_STATUS_CREATED_CODE));
                license.setStatus(licenseStatusService.getLicenseStatusByName(LICENSE_STATUS_ACKNOWLEDGED));
                final String stateValue = WORKFLOW_STATE_REJECTED;
                license.transition().progressWithStateCopy()
                        .withSenderName(currentUser.getUsername() + DELIMITER_COLON + currentUser.getName())
                        .withComments(workflowBean.getApproverComments())
                        .withStateValue(stateValue).withDateInfo(new DateTime().toDate())
                        .withOwner(license.getState().getInitiatorPosition()).withNextAction("SI/SS Approval Pending");

            }
        else if (NEW_STATE.equals(license.getState().getValue())) {
            final WorkFlowMatrix newwfmatrix = this.licenseWorkflowService.getWfMatrix(license.getStateType(), null,
                    null, workflowBean.getAdditionaRule(), NEW_STATE, null);
            license.transition().progressWithStateCopy()
                    .withSenderName(currentUser.getUsername() + DELIMITER_COLON + currentUser.getName())
                    .withComments(workflowBean.getApproverComments())
                    .withStateValue(newwfmatrix.getNextState()).withDateInfo(new DateTime().toDate()).withOwner(owner)
                    .withNextAction(newwfmatrix.getNextAction());
            license.setEgwStatus(egwStatusHibernateDAO
                    .getStatusByModuleAndCode(TRADELICENSEMODULE, APPLICATION_STATUS_CREATED_CODE));
            license.setStatus(licenseStatusService.getLicenseStatusByName(LICENSE_STATUS_ACKNOWLEDGED));
        } else if (REVENUE_CLERK_JA_APPROVED.equals(license.getState().getValue()) ||
                WORKFLOW_STATE_REJECTED.equals(license.getState().getValue())) {
            license.setEgwStatus(egwStatusHibernateDAO.getStatusByModuleAndCode(TRADELICENSEMODULE, APPLICATION_STATUS_CREATED_CODE));
            license.setStatus(licenseStatusService.getLicenseStatusByName(LICENSE_STATUS_UNDERWORKFLOW));
            license.transition().progressWithStateCopy()
                    .withSenderName(currentUser.getUsername() + DELIMITER_COLON + currentUser.getName())
                    .withComments(workflowBean.getApproverComments())
                    .withStateValue(wfmatrix.getNextState()).withDateInfo(new DateTime().toDate()).withOwner(owner)
                    .withNextAction(wfmatrix.getNextAction());
        }

        this.licenseRepository.save(license);
        licenseCitizenPortalService.onUpdate(license);
        licenseApplicationIndexService.createOrUpdateLicenseApplicationIndex(license);
    }

    private void closureWfWithOperator(TradeLicense license) {
        final String currentUserRoles = securityUtils.getCurrentUser().getRoles().toString();
        String comment = "";
        if (currentUserRoles.contains(CSCOPERATOR))
            comment = "CSC Operator Initiated";
        else if (currentUserRoles.contains("PUBLIC"))
            comment = "Citizen applied for closure";
        else if (currentUserRoles.contains(MEESEVAOPERATOR))
            comment = "Meeseva Operator Initiated";
        List<Assignment> assignmentList = getAssignments();
        if (assignmentList.isEmpty()) {
            throw new ValidationException(ERROR_WF_INITIATOR_NOT_DEFINED, ERROR_WF_INITIATOR_NOT_DEFINED);
        } else {
            final Assignment wfAssignment = assignmentList.get(0);
            if (license.hasState()) {
                license.transition().startNext();
            } else {
                license.transition().start();
            }
            license.transition().withSenderName(
                    wfAssignment.getEmployee().getUsername() + DELIMITER_COLON + wfAssignment.getEmployee().getName())
                    .withComments(comment).withNatureOfTask(license.getLicenseAppType().getName())
                    .withStateValue(NEW_STATE).withDateInfo(new Date()).withOwner(wfAssignment.getPosition())
                    .withNextAction("SI/SS Approval Pending").withInitiator(wfAssignment.getPosition()).withExtraInfo(license.getLicenseAppType().getName());
            license.setEgwStatus(
                    egwStatusHibernateDAO.getStatusByModuleAndCode(TRADELICENSEMODULE, APPLICATION_STATUS_CREATED_CODE));
        }
    }

    public List<Long> getLicenseIdsForDemandGeneration(CFinancialYear financialYear) {
        Installment installment = installmentDao.getInsatllmentByModuleForGivenDate(getModuleName(),
                financialYear.getStartingDate());
        return licenseRepository.findLicenseIdsForDemandGeneration(installment.getFromDate());
    }

    public TradeLicense closureWithMeeseva(TradeLicense license, WorkflowBean wfBean) {
        return saveClosure(license, wfBean);
    }

    public Boolean currentUserIsMeeseva() {
        return securityUtils.getCurrentUser().hasRole(MEESEVAOPERATOR);
    }

    @Transactional
    public void digitalSignTransition(String applicationNumber) {
        final User user = securityUtils.getCurrentUser();
        if (isNotBlank(applicationNumber)) {
            TradeLicense license = licenseRepository.findByApplicationNumber(applicationNumber);
            final DateTime currentDate = new DateTime();
            license.setEgwStatus(egwStatusHibernateDAO
                    .getStatusByModuleAndCode(TRADELICENSEMODULE, APPLICATION_STATUS_APPROVED_CODE));
            license.transition().progressWithStateCopy().withSenderName(user.getUsername() + "::" + user.getName())
                    .withComments(WF_DIGI_SIGNED)
                    .withStateValue(WF_DIGI_SIGNED)
                    .withDateInfo(currentDate.toDate())
                    .withOwner(license.getCurrentState().getInitiatorPosition())
                    .withNextAction("");
            license.setCertificateFileId(license.getDigiSignedCertFileStoreId());
            licenseRepository.save(license);
            tradeLicenseSmsAndEmailService.sendSMsAndEmailOnDigitalSign(license);
            licenseApplicationIndexService.createOrUpdateLicenseApplicationIndex(license);
        }

    }

    @Transactional
    public void save(TradeLicense license) {
        final BigDecimal currentDemandAmount = recalculateLicenseFee(license.getCurrentDemand());
        final BigDecimal feematrixDmdAmt = calculateFeeAmount(license);
        if (feematrixDmdAmt.compareTo(currentDemandAmount) >= 0)
            updateDemandForChangeTradeArea(license);
        processAndStoreDocument(license);
        licenseRepository.save(license);
    }

    @Transactional
    public void updateTradeLicense(final TradeLicense license, final WorkflowBean workflowBean) {
        processAndStoreDocument(license);
        licenseRepository.save(license);
        tradeLicenseSmsAndEmailService.sendSmsAndEmail(license, workflowBean.getWorkFlowAction());
        licenseApplicationIndexService.createOrUpdateLicenseApplicationIndex(license);
    }


    public void updateStatusInWorkFlowProgress(TradeLicense license, final String workFlowAction) {

        List<Position> userPositions = positionMasterService.getPositionsForEmployee(securityUtils.getCurrentUser().getId());
        if (BUTTONAPPROVE.equals(workFlowAction)) {
            if (isEmpty(license.getLicenseNumber()) && license.isNewApplication())
                license.setLicenseNumber(licenseNumberUtils.generateLicenseNumber());

            if (license.getCurrentDemand().getBaseDemand().compareTo(license.getCurrentDemand().getAmtCollected()) <= 0)
                license.setEgwStatus(egwStatusHibernateDAO.getStatusByModuleAndCode(TRADELICENSEMODULE, APPLICATION_STATUS_APPROVED_CODE));
            else
                license.setEgwStatus(egwStatusHibernateDAO.getStatusByModuleAndCode(TRADELICENSEMODULE, APPLICATION_STATUS_SECONDCOLLECTION_CODE));
            generateAndStoreCertificate(license);

        }
        if (BUTTONAPPROVE.equals(workFlowAction) || BUTTONFORWARD.equals(workFlowAction)) {
            license.setStatus(licenseStatusService.getLicenseStatusByCode(STATUS_UNDERWORKFLOW));
            if (license.getState().getValue().equals(WF_REVENUECLERK_APPROVED))
                license.setEgwStatus(egwStatusHibernateDAO.getStatusByModuleAndCode(TRADELICENSEMODULE, APPLICATION_STATUS_INSPE_CODE));
            else if (license.getState().getValue().equals(WORKFLOW_STATE_REJECTED))
                license.setEgwStatus(egwStatusHibernateDAO.getStatusByModuleAndCode(TRADELICENSEMODULE, APPLICATION_STATUS_CREATED_CODE));
        }

        if (GENERATECERTIFICATE.equals(workFlowAction)) {
            license.setActive(true);
            license.setStatus(licenseStatusService.getLicenseStatusByCode(STATUS_ACTIVE));
            // setting license to non-legacy, old license number will be the only tracking
            // to check a license created as legacy or new hereafter.
            license.setLegacy(false);
            validityService.applyLicenseValidity(license);
            license.setEgwStatus(egwStatusHibernateDAO.getStatusByModuleAndCode(TRADELICENSEMODULE, APPLICATION_STATUS_GENECERT_CODE));
        }
        if (BUTTONREJECT.equals(workFlowAction))
            if (license.getLicenseAppType() != null && userPositions.contains(license.getCurrentState().getInitiatorPosition())
                    && ("Rejected".equals(license.getState().getValue()))
                    || "License Created".equals(license.getState().getValue())) {
                license.setStatus(licenseStatusService.getLicenseStatusByCode(STATUS_CANCELLED));
                license.setEgwStatus(egwStatusHibernateDAO.getStatusByModuleAndCode(TRADELICENSEMODULE, APPLICATION_STATUS_CANCELLED));
                if (license.isNewApplication())
                    license.setActive(false);
            } else {
                license.setStatus(licenseStatusService.getLicenseStatusByCode(STATUS_REJECTED));
                license.setEgwStatus(egwStatusHibernateDAO.getStatusByModuleAndCode(TRADELICENSEMODULE, APPLICATION_STATUS_REJECTED));
            }
        if (license.hasState() && license.getState().getValue().contains(WF_REVENUECLERK_APPROVED)) {
            final BigDecimal currentDemandAmount = recalculateLicenseFee(license.getCurrentDemand());
            final BigDecimal recalDemandAmount = calculateFeeAmount(license);
            if (recalDemandAmount.compareTo(currentDemandAmount) >= 0)
                updateDemandForChangeTradeArea(license);
        }
    }

    public ReportOutput generateLicenseCertificate(TradeLicense license, boolean isProvisional) {
        String reportTemplate;
        if (CITY_GRADE_CORPORATION.equals(cityService.getCityGrade()))
            reportTemplate = "tl_licenseCertificateForCorp";
        else
            reportTemplate = "tl_licenseCertificate";
        ReportOutput reportOutput = reportService.createReport(new ReportRequest(reportTemplate, license,
                getReportParamsForCertificate(license, isProvisional)));
        reportOutput.setReportName(license.generateCertificateFileName());
        return reportOutput;
    }

    private Map<String, Object> getReportParamsForCertificate(TradeLicense license, boolean isProvisional) {

        final Map<String, Object> reportParams = new HashMap<>();
        reportParams.put("applicationnumber", license.getApplicationNumber());
        reportParams.put("applicantName", license.getLicensee().getApplicantName());
        reportParams.put("licencenumber", license.getLicenseNumber());
        reportParams.put("wardName", license.getBoundary().getName());
        reportParams.put("cscNumber", "");
        reportParams.put("nameOfEstablishment", escapeXml(license.getNameOfEstablishment()));
        reportParams.put("licenceAddress", escapeXml(license.getAddress()));
        reportParams.put("municipality", cityService.getMunicipalityName());
        reportParams.put("district", cityService.getDistrictName());
        reportParams.put("category", escapeXml(license.getCategory().getName()));
        reportParams.put("subCategory", escapeXml(license.getTradeName().getName()));
        reportParams.put("appType", license.isNewApplication() ? "New Trade" : "Renewal");
        reportParams.put("currentDate", currentDateToDefaultDateFormat());
        reportParams.put("carporationulbType", getMunicipalityName().contains("Corporation"));
        Optional<EgDemandDetails> demandDetails = license.getCurrentDemand().getEgDemandDetails().stream()
                .sorted(Comparator.comparing(EgDemandDetails::getInstallmentEndDate).reversed())
                .filter(demandDetail -> demandDetail.getEgDemandReason().getEgDemandReasonMaster().getReasonMaster().equals(LICENSE_FEE_TYPE))
                .filter(demandDetail -> demandDetail.getAmtCollected().doubleValue() > 0)
                .findFirst();
        BigDecimal amtPaid;
        String installmentYear;
        if (demandDetails.isPresent()) {
            amtPaid = demandDetails.get().getAmtCollected();
            installmentYear = toYearFormat(demandDetails.get().getInstallmentStartDate()) + "-" +
                    toYearFormat(demandDetails.get().getInstallmentEndDate());
        } else {
            throw new ValidationException("License Fee is not paid", "License Fee is not paid");
        }

        reportParams.put("installMentYear", installmentYear);
        reportParams.put("applicationdate", getDefaultFormattedDate(license.getApplicationDate()));
        reportParams.put("demandUpdateDate", getDefaultFormattedDate(license.getCurrentDemand().getModifiedDate()));
        reportParams.put("demandTotalamt", amtPaid);

        User approver;
        if (isProvisional || license.getApprovedBy() == null) {
            approver = licenseUtils.getCommissionerAssignment().getEmployee();
        } else {
            approver = license.getApprovedBy();
        }
        ByteArrayInputStream commissionerSign = new ByteArrayInputStream(
                approver == null || approver.getSignature() == null ? new byte[0] : approver.getSignature());
        reportParams.put("commissionerSign", commissionerSign);

        if (isProvisional)
            reportParams.put("certificateType", "provisional");
        else {
            reportParams.put("qrCode", license.qrCode(installmentYear, amtPaid));
        }

        return reportParams;
    }

    @ReadOnly
    public List<String> getTradeLicenseForGivenParam(final String paramValue, final String paramType) {
        List<String> licenseList = new ArrayList<>();
        if (isNotBlank(paramValue) && isNotBlank(paramType)) {
            if (SEARCH_BY_APPNO.equals(paramType))
                licenseList = licenseRepository.findAllApplicationNumberLike(paramValue);

            else if (SEARCH_BY_LICENSENO.equals(paramType))
                licenseList = licenseRepository.findAllLicenseNumberLike(paramValue);

            else if (SEARCH_BY_OLDLICENSENO.equals(paramType))
                licenseList = licenseRepository.findAllOldLicenseNumberLike(paramValue);

            else if (SEARCH_BY_TRADETITLE.equals(paramType))
                licenseList = licenseRepository.findAllNameOfEstablishmentLike(paramValue);

            else if (SEARCH_BY_TRADEOWNERNAME.equals(paramType))
                licenseList = licenseRepository.findAllApplicantNameLike(paramValue);

            else if (SEARCH_BY_PROPERTYASSESSMENTNO.equals(paramType))
                licenseList = licenseRepository.findAllAssessmentNoLike(paramValue);

            else if (SEARCH_BY_MOBILENO.equals(paramType))
                licenseList = licenseRepository.findAllMobilePhoneNumberLike(paramValue);
        }

        return licenseList;
    }

    @ReadOnly
    public Page<SearchForm> searchTradeLicense(final SearchForm searchForm) {
        Pageable pageable = new PageRequest(searchForm.pageNumber(),
                searchForm.pageSize(), searchForm.orderDir(), searchForm.orderBy());
        User currentUser = securityUtils.getCurrentUser();
        Page<TradeLicense> licenses = searchTradeRepository.findAll(SearchTradeSpec.searchTrade(searchForm), pageable);
        List<SearchForm> searchResults = new ArrayList<>();
        licenses.forEach(license ->
                searchResults.add(new SearchForm(license, currentUser, getOwnerName(license), licenseConfigurationService.getFeeCollectorRoles()))
        );
        return new PageImpl<>(searchResults, pageable, licenses.getTotalElements());
    }

    @ReadOnly
    public List<OnlineSearchForm> onlineSearchTradeLicense(final OnlineSearchForm searchForm) {
        final Criteria searchCriteria = entityManager.unwrap(Session.class).createCriteria(TradeLicense.class);
        searchCriteria.createAlias("licensee", "licc").createAlias("category", "cat")
                .createAlias("tradeName", "subcat").createAlias("status", "licstatus");
        if (isNotBlank(searchForm.getApplicationNumber()))
            searchCriteria.add(Restrictions.eq("applicationNumber", searchForm.getApplicationNumber()).ignoreCase());
        if (isNotBlank(searchForm.getLicenseNumber()))
            searchCriteria.add(Restrictions.eq("licenseNumber", searchForm.getLicenseNumber()).ignoreCase());
        if (isNotBlank(searchForm.getMobileNo()))
            searchCriteria.add(Restrictions.eq("licc.mobilePhoneNumber", searchForm.getMobileNo()));
        if (isNotBlank(searchForm.getTradeOwnerName()))
            searchCriteria.add(Restrictions.like("licc.applicantName", searchForm.getTradeOwnerName(), ANYWHERE));


        searchCriteria.add(Restrictions.isNotNull("applicationNumber"));
        searchCriteria.addOrder(Order.asc("id"));
        List<OnlineSearchForm> searchResult = new ArrayList<>();
        for (TradeLicense license : (List<TradeLicense>) searchCriteria.list()) {
            if (license != null)
                searchResult.add(new OnlineSearchForm(license, getDemandColl(license)));
        }
        return searchResult;
    }

    public BigDecimal[] getDemandColl(TradeLicense license) {
        BigDecimal[] dmdColl = new BigDecimal[3];
        Arrays.fill(dmdColl, ZERO);
        final Installment latestInstallment = this.installmentDao.getInsatllmentByModuleForGivenDate(getModuleName(),
                new DateTime().withMonthOfYear(4).withDayOfMonth(1).toDate());
        license.getCurrentDemand().getEgDemandDetails().stream().forEach(egDemandDetails -> {
                    if (latestInstallment.equals(egDemandDetails.getEgDemandReason().getEgInstallmentMaster())) {
                        dmdColl[1] = dmdColl[1].add(egDemandDetails.getAmount());
                        dmdColl[2] = dmdColl[2].add(egDemandDetails.getAmtCollected());
                    } else {
                        dmdColl[0] = dmdColl[0].add(egDemandDetails.getAmount());
                        dmdColl[2] = dmdColl[2].add(egDemandDetails.getAmtCollected());
                    }
                }
        );
        return dmdColl;
    }

    @ReadOnly
    public List<DemandNoticeForm> getLicenseDemandNotices(final DemandNoticeForm demandNoticeForm) {
        final Criteria searchCriteria = entityManager.unwrap(Session.class).createCriteria(TradeLicense.class);
        searchCriteria.createAlias("licensee", "licc").createAlias("category", "cat").createAlias("tradeName", "subcat")
                .createAlias("status", "licstatus").createAlias("natureOfBusiness", "nob")
                .createAlias("licenseDemand", "licDemand").createAlias("licenseAppType", "appType")
                .add(Restrictions.ne("appType.code", CLOSURE_APPTYPE_CODE));
        if (isNotBlank(demandNoticeForm.getLicenseNumber()))
            searchCriteria.add(Restrictions.eq("licenseNumber", demandNoticeForm.getLicenseNumber()).ignoreCase());
        if (isNotBlank(demandNoticeForm.getOldLicenseNumber()))
            searchCriteria
                    .add(Restrictions.eq("oldLicenseNumber", demandNoticeForm.getOldLicenseNumber()).ignoreCase());
        if (demandNoticeForm.getCategoryId() != null)
            searchCriteria.add(Restrictions.eq("cat.id", demandNoticeForm.getCategoryId()));
        if (demandNoticeForm.getSubCategoryId() != null)
            searchCriteria.add(Restrictions.eq("subcat.id", demandNoticeForm.getSubCategoryId()));
        if (demandNoticeForm.getWardId() != null)
            searchCriteria.createAlias("parentBoundary", "wards")
                    .add(Restrictions.eq("wards.id", demandNoticeForm.getWardId()));
        if (demandNoticeForm.getElectionWard() != null)
            searchCriteria.createAlias("adminWard", "electionWard")
                    .add(Restrictions.eq("electionWard.id", demandNoticeForm.getElectionWard()));
        if (demandNoticeForm.getLocalityId() != null)
            searchCriteria.createAlias("boundary", "locality")
                    .add(Restrictions.eq("locality.id", demandNoticeForm.getLocalityId()));
        if (demandNoticeForm.getStatusId() == null)
            searchCriteria.add(Restrictions.ne("licstatus.statusCode", StringUtils.upperCase("CAN")));
        else
            searchCriteria.add(Restrictions.eq("status.id", demandNoticeForm.getStatusId()));
        searchCriteria
                .add(Restrictions.eq("isActive", true))
                .add(Restrictions.eq("nob.name", PERMANENT_NATUREOFBUSINESS))
                .add(Restrictions.gtProperty("licDemand.baseDemand", "licDemand.amtCollected"))
                .addOrder(Order.asc("id"));
        final List<DemandNoticeForm> finalList = new LinkedList<>();

        for (final TradeLicense license : (List<TradeLicense>) searchCriteria.list()) {
            LicenseDemand licenseDemand = license.getCurrentDemand();
            if (licenseDemand != null) {
                Installment currentInstallment = licenseDemand.getEgInstallmentMaster();
                List<Installment> previousInstallment = installmentDao
                        .fetchPreviousInstallmentsInDescendingOrderByModuleAndDate(
                                licenseUtils.getModule(TRADE_LICENSE), currentInstallment.getToDate(), 1);
                Map<String, Map<String, BigDecimal>> outstandingFees = getOutstandingFeeForDemandNotice(license,
                        currentInstallment, previousInstallment.get(0));
                Map<String, BigDecimal> licenseFees = outstandingFees.get(LICENSE_FEE_TYPE);
                finalList.add(new DemandNoticeForm(license, licenseFees, getOwnerName(license)));
            }
        }
        return finalList;
    }

    public String getOwnerName(TradeLicense license) {
        String ownerName = NA;
        if (license.getState() != null && license.currentAssignee() != null) {
            List<Assignment> assignmentList = assignmentService
                    .getAssignmentsForPosition(license.currentAssignee().getId(), new Date());
            if (!assignmentList.isEmpty())
                ownerName = assignmentList.get(0).getEmployee().getName();
            ownerName = format("%s [%s]", ownerName, license.currentAssignee().getName());
        }
        return ownerName;

    }

    public List<HashMap<String, Object>> populateHistory(final TradeLicense tradeLicense) {
        final List<HashMap<String, Object>> processHistoryDetails = new ArrayList<>();
        if (tradeLicense.hasState()) {
            State<Position> state = tradeLicense.getCurrentState();
            final HashMap<String, Object> currentStateDetail = new HashMap<>();
            currentStateDetail.put("date", state.getLastModifiedDate());
            currentStateDetail.put("updatedBy", state.getSenderName().contains(DELIMITER_COLON)
                    ? state.getSenderName().split(DELIMITER_COLON)[1] :state.getSenderName());
            currentStateDetail.put("status", state.isEnded() ? "Completed" : state.getValue());
            currentStateDetail.put("comments", defaultString(state.getComments()));
            User ownerUser = state.getOwnerUser();
            Position ownerPosition = state.getOwnerPosition();
            if (ownerPosition != null) {
                User usr = eisCommonService.getUserForPosition(ownerPosition.getId(), state.getLastModifiedDate());
                currentStateDetail.put("user", usr == null ? NA : usr.getName());
            } else
                currentStateDetail.put("user", ownerUser == null ? NA : ownerUser.getName());

            processHistoryDetails.add(currentStateDetail);
            state.getHistory().stream().sorted(Comparator.comparing(StateHistory<Position>::getLastModifiedDate).reversed()).
                    forEach(sh -> processHistoryDetails.add(constructHistory(sh)));
        }
        return processHistoryDetails;
    }

    private HashMap<String, Object> constructHistory(StateHistory<Position> stateHistory) {
        final HashMap<String, Object> processHistory = new HashMap<>();
        processHistory.put("date", stateHistory.getLastModifiedDate());
        processHistory.put("updatedBy", stateHistory.getSenderName().contains(DELIMITER_COLON)
                ? stateHistory.getSenderName().split(DELIMITER_COLON)[1] : stateHistory.getSenderName());
        processHistory.put("status", stateHistory.getValue());
        processHistory.put("comments", defaultString(stateHistory.getComments()));
        Position ownerPosition = stateHistory.getOwnerPosition();
        User ownerUser = stateHistory.getOwnerUser();
        if (ownerPosition != null) {
            User userPos = eisCommonService.getUserForPosition(ownerPosition.getId(), stateHistory.getLastModifiedDate());
            processHistory.put("user", userPos == null ? NA : userPos.getName());
        } else
            processHistory.put("user",
                    ownerUser == null ? NA : ownerUser.getName());
        return processHistory;
    }

    @ReadOnly
    public List<TradeLicense> getLicenses(Example license) {
        return licenseRepository.findAll(license);
    }

    public List<BillReceipt> getReceipts(TradeLicense license) {
        return demandGenericDao.getBillReceipts(license.getCurrentDemand());
    }

    public LicenseDocumentType getLicenseDocumentType(Long id) {
        return licenseDocumentTypeRepository.findOne(id);
    }

    public Map<String, Map<String, List<LicenseDocument>>> getAttachedDocument(Long licenseId) {

        List<LicenseDocument> licenseDocuments = getLicenseById(licenseId).getDocuments();
        Map<String, Map<String, List<LicenseDocument>>> licenseDocumentDetails = new HashMap<>();
        licenseDocumentDetails.put(NEW_APPTYPE_CODE, new HashMap<>());
        licenseDocumentDetails.put(RENEW_APPTYPE_CODE, new HashMap<>());
        licenseDocumentDetails.put(CLOSURE_APPTYPE_CODE, new HashMap<>());

        for (LicenseDocument document : licenseDocuments) {
            String docType = document.getType().getName();
            String appType = document.getType().getApplicationType().getCode();

            if (licenseDocumentDetails.get(appType).containsKey(docType)) {
                licenseDocumentDetails.get(appType).get(docType).add(document);
            } else {
                List<LicenseDocument> documents = new ArrayList<>();
                documents.add(document);
                licenseDocumentDetails.get(appType).put(docType, documents);
            }
        }
        return licenseDocumentDetails;
    }

    public ReportOutput generateAcknowledgment(String uid) {
        TradeLicense license = getLicenseByUID(uid);
        Map<String, Object> reportParams = new HashMap<>();
        reportParams.put("amount", license.getTotalBalance());
        ReportRequest reportRequest = new ReportRequest("tl_license_acknowledgment", license, reportParams);
        reportRequest.setReportFormat(ReportFormat.PDF);
        ReportOutput reportOutput = reportService.createReport(reportRequest);
        reportOutput.setReportName(append("license_ack_", license.getApplicationNumber()));
        return reportOutput;
    }

    @ReadOnly
    public ReportOutput generateClosureNotice(String reportFormat) {
        ReportOutput reportOutput = new ReportOutput();
        Map<String, Object> reportParams = new HashMap<>();
        List<TradeLicense> licenses = searchTradeRepository.findLicenseClosureByCurrentInstallmentYear(new Date());
        if (licenses.isEmpty()) {
            reportOutput.setReportName("tl_closure_notice");
            reportOutput.setReportFormat(ReportFormat.PDF);
            reportOutput.setReportOutputData("No Data".getBytes());
        } else {
            reportParams.put("License", licenses);
            reportParams.put("corp", cityService.getCityGrade());
            reportParams.put("currentDate", currentDateToDefaultDateFormat());
            reportParams.put("municipality", cityService.getMunicipalityName());
            reportOutput = reportService.createReport(
                    new ReportRequest("tl_closure_notice", licenses, reportParams));
        }
        if (reportFormat.equalsIgnoreCase("zip"))
            reportOutput.setReportOutputData(toByteArray(addFilesToZip(byteArrayToFile(reportOutput.getReportOutputData(),
                    "tl_closure_notice_", ".pdf").toFile())));
        return reportOutput;
    }

    public void generateAndStoreCertificate(TradeLicense license) {
        FileStoreMapper fileStore = fileStoreService.store(generateLicenseCertificate(license, false).getReportOutputData(),
                license.generateCertificateFileName() + ".pdf", CONTENT_TYPES.get(PDF), TL_FILE_STORE_DIR);
        license.setCertificateFileId(fileStore.getFileStoreId());
    }

    public TradeLicense getLicenseByUID(String uid) {
        return licenseRepository.findByUid(uid);
    }
}