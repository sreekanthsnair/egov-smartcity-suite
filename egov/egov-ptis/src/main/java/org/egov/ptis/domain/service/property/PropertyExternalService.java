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
package org.egov.ptis.domain.service.property;

import static java.lang.String.format;
import static java.math.BigDecimal.ZERO;
import static org.egov.ptis.constants.PropertyTaxConstants.*;

import java.io.File;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.egov.collection.integration.models.BillReceiptInfo;
import org.egov.collection.integration.models.ReceiptAccountInfo;
import org.egov.collection.integration.services.CollectionIntegrationService;
import org.egov.commons.Area;
import org.egov.commons.Bank;
import org.egov.commons.Installment;
import org.egov.commons.dao.BankHibernateDAO;
import org.egov.commons.dao.InstallmentHibDao;
import org.egov.dcb.bean.ChequePayment;
import org.egov.dcb.bean.Payment;
import org.egov.demand.dao.EgBillDao;
import org.egov.demand.model.EgBill;
import org.egov.demand.model.EgBillDetails;
import org.egov.demand.model.EgDemandDetails;
import org.egov.eis.entity.Assignment;
import org.egov.infra.admin.master.entity.Boundary;
import org.egov.infra.admin.master.entity.BoundaryType;
import org.egov.infra.admin.master.entity.CrossHierarchy;
import org.egov.infra.admin.master.entity.Department;
import org.egov.infra.admin.master.entity.Module;
import org.egov.infra.admin.master.entity.User;
import org.egov.infra.admin.master.service.BoundaryService;
import org.egov.infra.admin.master.service.BoundaryTypeService;
import org.egov.infra.admin.master.service.DepartmentService;
import org.egov.infra.admin.master.service.ModuleService;
import org.egov.infra.admin.master.service.UserService;
import org.egov.infra.config.core.ApplicationThreadLocals;
import org.egov.infra.config.persistence.datasource.routing.annotation.ReadOnly;
import org.egov.infra.exception.ApplicationRuntimeException;
import org.egov.infra.filestore.entity.FileStoreMapper;
import org.egov.infra.filestore.service.FileStoreService;
import org.egov.infra.persistence.entity.Address;
import org.egov.infra.persistence.entity.CorrespondenceAddress;
import org.egov.infra.persistence.entity.enums.Gender;
import org.egov.infra.rest.client.SimpleRestClient;
import org.egov.infra.utils.DateUtils;
import org.egov.infra.web.utils.WebUtils;
import org.egov.infra.workflow.matrix.entity.WorkFlowMatrix;
import org.egov.infra.workflow.service.SimpleWorkflowService;
import org.egov.infstr.services.PersistenceService;
import org.egov.pims.commons.Position;
import org.egov.ptis.bean.AssessmentInfo;
import org.egov.ptis.bean.FloorDetailsInfo;
import org.egov.ptis.bean.FloorInfo;
import org.egov.ptis.bean.SurveyAssessmentDetails;
import org.egov.ptis.client.bill.PTBillServiceImpl;
import org.egov.ptis.client.integration.utils.CollectionHelper;
import org.egov.ptis.client.model.PenaltyAndRebate;
import org.egov.ptis.client.util.PropertyTaxNumberGenerator;
import org.egov.ptis.client.util.PropertyTaxUtil;
import org.egov.ptis.constants.PropertyTaxConstants;
import org.egov.ptis.domain.bill.PropertyTaxBillable;
import org.egov.ptis.domain.dao.demand.PtDemandDao;
import org.egov.ptis.domain.dao.property.BasicPropertyDAO;
import org.egov.ptis.domain.dao.property.PropertyHibernateDAO;
import org.egov.ptis.domain.dao.property.PropertyMutationDAO;
import org.egov.ptis.domain.dao.property.PropertyMutationMasterDAO;
import org.egov.ptis.domain.dao.property.PropertyStatusHibernateDAO;
import org.egov.ptis.domain.dao.property.PropertyTypeMasterDAO;
import org.egov.ptis.domain.entity.demand.Ptdemand;
import org.egov.ptis.domain.entity.document.DocumentTypeDetails;
import org.egov.ptis.domain.entity.enums.TransactionType;
import org.egov.ptis.domain.entity.property.Apartment;
import org.egov.ptis.domain.entity.property.BasicProperty;
import org.egov.ptis.domain.entity.property.BasicPropertyImpl;
import org.egov.ptis.domain.entity.property.BoundaryCategory;
import org.egov.ptis.domain.entity.property.BuiltUpProperty;
import org.egov.ptis.domain.entity.property.Document;
import org.egov.ptis.domain.entity.property.DocumentType;
import org.egov.ptis.domain.entity.property.Floor;
import org.egov.ptis.domain.entity.property.FloorType;
import org.egov.ptis.domain.entity.property.GisDetails;
import org.egov.ptis.domain.entity.property.Property;
import org.egov.ptis.domain.entity.property.PropertyAddress;
import org.egov.ptis.domain.entity.property.PropertyDetail;
import org.egov.ptis.domain.entity.property.PropertyDocs;
import org.egov.ptis.domain.entity.property.PropertyID;
import org.egov.ptis.domain.entity.property.PropertyImpl;
import org.egov.ptis.domain.entity.property.PropertyMutation;
import org.egov.ptis.domain.entity.property.PropertyMutationMaster;
import org.egov.ptis.domain.entity.property.PropertyOccupation;
import org.egov.ptis.domain.entity.property.PropertyOwnerInfo;
import org.egov.ptis.domain.entity.property.PropertyStatus;
import org.egov.ptis.domain.entity.property.PropertyStatusValues;
import org.egov.ptis.domain.entity.property.PropertyTypeMaster;
import org.egov.ptis.domain.entity.property.PropertyUsage;
import org.egov.ptis.domain.entity.property.RoofType;
import org.egov.ptis.domain.entity.property.StructureClassification;
import org.egov.ptis.domain.entity.property.TaxExemptionReason;
import org.egov.ptis.domain.entity.property.VacantProperty;
import org.egov.ptis.domain.entity.property.WallType;
import org.egov.ptis.domain.entity.property.WoodType;
import org.egov.ptis.domain.entity.property.view.SurveyBean;
import org.egov.ptis.domain.model.AssessmentDetails;
import org.egov.ptis.domain.model.BoundaryDetails;
import org.egov.ptis.domain.model.DocumentDetailsRequest;
import org.egov.ptis.domain.model.ErrorDetails;
import org.egov.ptis.domain.model.FloorDetails;
import org.egov.ptis.domain.model.LocalityDetails;
import org.egov.ptis.domain.model.MasterCodeNamePairDetails;
import org.egov.ptis.domain.model.NewPropertyDetails;
import org.egov.ptis.domain.model.OwnerDetails;
import org.egov.ptis.domain.model.OwnerInformation;
import org.egov.ptis.domain.model.OwnerName;
import org.egov.ptis.domain.model.PayPropertyTaxDetails;
import org.egov.ptis.domain.model.PropertyDetails;
import org.egov.ptis.domain.model.PropertyTaxDetails;
import org.egov.ptis.domain.model.ReceiptDetails;
import org.egov.ptis.domain.model.RestAssessmentDetails;
import org.egov.ptis.domain.model.RestPropertyTaxDetails;
import org.egov.ptis.domain.model.TaxCalculatorRequest;
import org.egov.ptis.domain.model.TaxCalculatorResponse;
import org.egov.ptis.domain.model.ViewPropertyDetails;
import org.egov.ptis.domain.model.enums.BasicPropertyStatus;
import org.egov.ptis.domain.repository.vacancyremission.VacancyRemissionRepository;
import org.egov.ptis.domain.service.transfer.PropertyTransferService;
import org.egov.ptis.exceptions.TaxCalculatorExeption;
import org.egov.ptis.master.service.ApartmentService;
import org.egov.ptis.master.service.CalculatePropertyTaxService;
import org.egov.ptis.master.service.FloorTypeService;
import org.egov.ptis.master.service.PropertyOccupationService;
import org.egov.ptis.master.service.PropertyUsageService;
import org.egov.ptis.master.service.RoofTypeService;
import org.egov.ptis.master.service.StructureClassificationService;
import org.egov.ptis.master.service.TaxExemptionReasonService;
import org.egov.ptis.master.service.WallTypeService;
import org.egov.ptis.master.service.WoodTypeService;
import org.hibernate.Session;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.annotation.Transactional;

public class PropertyExternalService {
    private static final String PROP_SERVICE = "propService";
    private static final String FROM_BOUNDARY_B_WHERE_B_ID_ID = "from Boundary b where b.id = :id";
    private static final Logger LOGGER = LoggerFactory.getLogger(PropertyExternalService.class);
    private static final String ASSESSMENT = "Assessment";
    public static final Integer FLAG_MOBILE_EMAIL = 0;
    public static final Integer FLAG_TAX_DETAILS = 1;
    public static final Integer FLAG_FULL_DETAILS = 2;
    private static final String FORWARD_SUCCESS_COMMENT = "Application has been created by GIS Survey system.";
    private static final String HALF_YEARLY_TAX = "halfYearlyTax";
    private static final String ARV = "ARV";

    @Autowired
    private BasicPropertyDAO basicPropertyDAO;
    @Autowired
    private PtDemandDao ptDemandDAO;
    @Autowired
    private ApplicationContext beanProvider;
    @Autowired
    private PropertyTaxNumberGenerator propertyTaxNumberGenerator;
    @Autowired
    private EgBillDao egBillDAO;
    @Autowired
    private PTBillServiceImpl ptBillServiceImpl;
    @Autowired
    @Qualifier("propertyTaxBillable")
    private PropertyTaxBillable propertyTaxBillable;
    @Autowired
    private PropertyTypeMasterDAO propertyTypeMasterDAO;
    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    private BoundaryService boundaryService;
    @Autowired
    @Qualifier("fileStoreService")
    protected FileStoreService fileStoreService;
    @Autowired
    private CollectionIntegrationService collectionService;
    @Autowired
    private PropertyPersistenceService basicPropertyService;
    @Autowired
    private BoundaryTypeService boundaryTypeService;
    @Autowired
    @Qualifier("workflowService")
    private SimpleWorkflowService<PropertyImpl> propertyWorkflowService;
    @Autowired
    private UserService userService;
    @Autowired
    private BankHibernateDAO bankHibernateDAO;
    @Autowired
    private PropertyMutationDAO propertyMutationDAO;
    @Autowired
    private FloorTypeService floorTypeService;
    @Autowired
    private RoofTypeService roofTypeService;
    @Autowired
    private WallTypeService wallTypeService;
    @Autowired
    private WoodTypeService woodTypeService;
    @Autowired
    private InstallmentHibDao installmentDao;
    @Autowired
    private ModuleService moduleService;

    @Autowired
    PropertyUsageService propertyUsageService;

    @Autowired
    StructureClassificationService structureClassificationService;

    @Autowired
    PropertyTaxUtil propertyTaxUtil;

    @Autowired
    private SimpleRestClient simpleRestClient;

    @Autowired
    private PropertyTransferService propertyTransferService;

    @Autowired
    @Qualifier("documentTypeDetailsService")
    private PersistenceService<DocumentTypeDetails, Long> documentTypeDetailsService;
    @Autowired
    private PropertySurveyService surveyService;
    @Autowired
    private PropertyHibernateDAO propertyHibernateDAO;
    @Autowired
    private ApartmentService apartmentService;
    @Autowired
    private PropertyMutationMasterDAO propertyMutationMasterDAO;
    @Autowired
    private PropertyOccupationService propertyOccupationService;
    @Autowired
    private TaxExemptionReasonService taxExemptionReasonService;
    @Autowired
    private DepartmentService departmentService;
    @Autowired
    private PropertyStatusHibernateDAO propertyStatusHibernateDAO;

    @Autowired
    private CalculatePropertyTaxService calculatePropertyTaxService;

    private PropertyImpl propty = new PropertyImpl();

    public PropertyImpl getPropty() {
        return propty;
    }

    public void setPropty(PropertyImpl propty) {
        this.propty = propty;
    }

    public AssessmentDetails loadAssessmentDetails(final String propertyId, final Integer flag,
            final BasicPropertyStatus status) {
        PropertyImpl property;
        AssessmentDetails assessmentDetail = new AssessmentDetails();
        assessmentDetail.setPropertyID(propertyId);
        assessmentDetail.setFlag(flag);
        validate(assessmentDetail);
        BasicProperty basicProperty = initiateBasicProperty(status, assessmentDetail);
        if (basicProperty != null) {
            property = (PropertyImpl) basicProperty.getProperty();
            if (basicProperty.getLatitude() != null && basicProperty.getLongitude() != null) {
                assessmentDetail.setLatitude(basicProperty.getLatitude());
                assessmentDetail.setLongitude(basicProperty.getLongitude());
            }
            if (flag.equals(FLAG_MOBILE_EMAIL))
                loadPrimaryMobileAndEmail(basicProperty, assessmentDetail);
            if (property != null) {
                final PropertyDetails propertyDetails = new PropertyDetails();
                assessmentDetail.setPropertyDetails(propertyDetails);
                if (flag.equals(FLAG_FULL_DETAILS)) {
                    getAsssessmentDetails(basicProperty, assessmentDetail);
                    loadPropertyDues(property, assessmentDetail);
                }
                if (flag.equals(FLAG_TAX_DETAILS))
                    loadPropertyDues(property, assessmentDetail);
                if (assessmentDetail.isExempted()) {
                    assessmentDetail.getPropertyDetails().setTaxDue(ZERO);
                    assessmentDetail.getPropertyDetails().setCurrentTax(ZERO);
                    assessmentDetail.getPropertyDetails().setArrearTax(ZERO);
                }
            }
        }
        return assessmentDetail;
    }

    private void validate(AssessmentDetails assessmentDetail) {
        if (assessmentDetail.getPropertyID() == null || assessmentDetail.getPropertyID().trim().equals(""))
            throw new ApplicationRuntimeException("PropertyID is null or empty!");
        if (assessmentDetail.getFlag() == null || assessmentDetail.getFlag() > 3)
            throw new ApplicationRuntimeException("Invalid Flag");
    }

    private BasicProperty initiateBasicProperty(BasicPropertyStatus status, AssessmentDetails assessmentDetail) {
        BasicProperty basicProperty = basicPropertyDAO
                .getAllBasicPropertyByPropertyID(assessmentDetail.getPropertyID());
        final ErrorDetails errorDetails = new ErrorDetails();
        if (null != basicProperty && null != basicProperty.getProperty()) {
            assessmentDetail.setStatus(basicProperty.isActive());
            if (status.equals(BasicPropertyStatus.ACTIVE)) {
                if (basicProperty.isActive()) {
                    checkStatusValues(basicProperty, errorDetails);
                } else {
                    errorDetails.setErrorCode(PROPERTY_ACTIVE_ERR_CODE);
                    errorDetails.setErrorMessage(PROPERTY_ACTIVE_NOT_EXISTS);
                    assessmentDetail.setErrorDetails(errorDetails);
                }
            } else if (status.equals(BasicPropertyStatus.INACTIVE)) {
                if (!basicProperty.isActive()) {
                    checkStatusValues(basicProperty, errorDetails);
                } else {
                    errorDetails.setErrorCode(PROPERTY_INACTIVE_ERR_CODE);
                    errorDetails.setErrorMessage(PROPERTY_INACTIVE_ERR_MSG);
                    assessmentDetail.setErrorDetails(errorDetails);
                }
            } else {
                checkStatusValues(basicProperty, errorDetails);
            }
        } else {
            errorDetails.setErrorCode(PROPERTY_NOT_EXIST_ERR_CODE);
            errorDetails.setErrorMessage(PROPERTY_NOT_EXIST_ERR_MSG_PREFIX + assessmentDetail.getPropertyID()
                    + PROPERTY_NOT_EXIST_ERR_MSG_SUFFIX);
        }
        assessmentDetail.setErrorDetails(errorDetails);
        return basicProperty;
    }

    private void checkStatusValues(BasicProperty basicProperty, ErrorDetails errorDetails) {
        final Set<PropertyStatusValues> statusValues = basicProperty.getPropertyStatusValuesSet();
        if (null != statusValues && !statusValues.isEmpty())
            for (final PropertyStatusValues statusValue : statusValues)
                if (statusValue.getPropertyStatus().getStatusCode() == MARK_DEACTIVE) {
                    errorDetails.setErrorCode(PROPERTY_MARK_DEACTIVATE_ERR_CODE);
                    errorDetails.setErrorMessage(PROPERTY_MARK_DEACTIVATE_ERR_MSG);
                }
    }

    private void loadPrimaryMobileAndEmail(BasicProperty basicProperty, AssessmentDetails assessmentDetail) {
        final User primaryOwner = basicProperty.getPrimaryOwner();
        assessmentDetail.setPrimaryEmail(primaryOwner.getEmailId());
        assessmentDetail.setPrimaryMobileNo(primaryOwner.getMobileNumber());
    }

    private void loadPropertyDues(PropertyImpl property, AssessmentDetails assessmentDetail) {
        final Map<String, BigDecimal> resultmap = ptDemandDAO.getDemandCollMap(property);
        if (null != resultmap && !resultmap.isEmpty()) {
            final BigDecimal currDmd = resultmap.get(CURR_FIRSTHALF_DMD_STR)
                    .add(resultmap.get(CURR_SECONDHALF_DMD_STR));
            final BigDecimal arrDmd = resultmap.get(ARR_DMD_STR);
            final BigDecimal currCollection = resultmap.get(CURR_FIRSTHALF_COLL_STR)
                    .add(resultmap.get(CURR_SECONDHALF_COLL_STR));
            final BigDecimal arrCollection = resultmap.get(ARR_COLL_STR);
            final BigDecimal taxDue = currDmd.add(arrDmd).subtract(currCollection)
                    .subtract(arrCollection);
            assessmentDetail.getPropertyDetails().setTaxDue(taxDue);
            assessmentDetail.getPropertyDetails().setCurrentTax(currDmd);
            assessmentDetail.getPropertyDetails().setArrearTax(arrDmd);
        }
    }

    private void getAsssessmentDetails(BasicProperty basicProperty, AssessmentDetails assessmentDetail) {

        // Owner Details
        assessmentDetail.setBoundaryDetails(prepareBoundaryInfo(basicProperty));
        assessmentDetail.setHouseNo(basicProperty.getAddress().getHouseNoBldgApt());
        assessmentDetail.setPropertyAddress(basicProperty.getAddress().toString());
        PropertyImpl property = (PropertyImpl) basicProperty.getProperty();
        if (null != property) {
            assessmentDetail.setOwnerNames(prepareOwnerInfo(property));
            assessmentDetail.setExempted(property.getIsExemptedFromTax());
            // Property Details
            final PropertyDetail propertyDetail = property.getPropertyDetail();
            if (null != propertyDetail) {
                assessmentDetail.getPropertyDetails().setPropertyType(propertyDetail.getPropertyTypeMaster().getType());
                if (propertyDetail.getPropertyUsage() != null)
                    assessmentDetail.getPropertyDetails()
                            .setPropertyUsage(propertyDetail.getPropertyUsage().getUsageName());
                if (null != propertyDetail.getNoofFloors())
                    assessmentDetail.getPropertyDetails().setNoOfFloors(propertyDetail.getNoofFloors());
                else
                    assessmentDetail.getPropertyDetails().setNoOfFloors(0);
            }
        }
    }

    private BoundaryDetails prepareBoundaryInfo(final BasicProperty basicProperty) {
        final BoundaryDetails boundaryDetails = new BoundaryDetails();
        final PropertyID propertyID = basicProperty.getPropertyID();
        if (null != propertyID) {
            if (null != propertyID.getZone()) {
                boundaryDetails.setZoneId(propertyID.getZone().getId());
                boundaryDetails.setZoneNumber(propertyID.getZone().getBoundaryNum());
                boundaryDetails.setZoneName(propertyID.getZone().getName());
                boundaryDetails.setZoneBoundaryType(propertyID.getZone().getBoundaryType().getName());
            }
            if (null != propertyID.getWard()) {
                boundaryDetails.setWardId(propertyID.getWard().getId());
                boundaryDetails.setWardNumber(propertyID.getWard().getBoundaryNum());
                boundaryDetails.setWardName(propertyID.getWard().getName());
                boundaryDetails.setWardBoundaryType(propertyID.getWard().getBoundaryType().getName());
            }
            if (null != propertyID.getElectionBoundary()) {
                boundaryDetails.setAdminWardId(propertyID.getElectionBoundary().getId());
                boundaryDetails.setAdminWardNumber(propertyID.getElectionBoundary().getBoundaryNum());
                boundaryDetails.setAdminWardName(propertyID.getElectionBoundary().getName());
                boundaryDetails.setAdminWardBoundaryType(propertyID.getElectionBoundary().getBoundaryType().getName());
            }
            if (null != propertyID.getArea()) {
                boundaryDetails.setBlockId(propertyID.getArea().getId());
                boundaryDetails.setBlockNumber(propertyID.getArea().getBoundaryNum());
                boundaryDetails.setBlockName(propertyID.getArea().getName());
            }
            if (null != propertyID.getLocality()) {
                boundaryDetails.setLocalityId(propertyID.getLocality().getId());
                boundaryDetails.setLocalityName(propertyID.getLocality().getName());
            }
            if (null != propertyID.getStreet()) {
                boundaryDetails.setStreetId(propertyID.getStreet().getId());
                boundaryDetails.setStreetName(propertyID.getStreet().getName());
            }
        }
        return boundaryDetails;
    }

    private Set<OwnerName> prepareOwnerInfo(final Property property) {
        final List<PropertyOwnerInfo> propertyOwners = property.getBasicProperty().getPropertyOwnerInfo();
        final Set<OwnerName> ownerNames = new HashSet<>(0);
        if (propertyOwners != null && !propertyOwners.isEmpty())
            for (final PropertyOwnerInfo propertyOwner : propertyOwners) {
                final OwnerName ownerName = new OwnerName();
                if (StringUtils.isNotBlank(propertyOwner.getOwner().getAadhaarNumber()))
                    ownerName.setAadhaarNumber(propertyOwner.getOwner().getAadhaarNumber()
                            .replaceAll("\\w(?=\\w{4})", "*"));
                ownerName.setOwnerName(propertyOwner.getOwner().getName());
                if (StringUtils.isNotBlank(propertyOwner.getOwner().getMobileNumber()))
                    ownerName.setMobileNumber(propertyOwner.getOwner().getMobileNumber()
                            .replaceAll("\\w(?=\\w{2})", "*"));
                ownerName.setEmailId(propertyOwner.getOwner().getEmailId());
                ownerNames.add(ownerName);
            }
        return ownerNames;
    }

    public PropertyTaxDetails getPropertyTaxDetails(final String assessmentNo, final String oldAssessmentNo,
            final String category) {
        PropertyTaxDetails propertyTaxDetails;
        BasicProperty basicProperty = null;
        List<BasicProperty> basicProperties = new ArrayList<>();
        final ErrorDetails errorDetails = new ErrorDetails();
        if (StringUtils.isNotBlank(assessmentNo))
            basicProperty = basicPropertyDAO.getBasicPropertyByPropertyID(assessmentNo);
        else if (StringUtils.isNotBlank(oldAssessmentNo)) {
            basicProperties = (List<BasicProperty>) basicPropertyDAO.getBasicPropertyByOldMunipalNo(oldAssessmentNo);
            if (!basicProperties.isEmpty() && basicProperties.size() == 1)
                basicProperty = basicProperties.get(0);
        }
        if (!basicProperties.isEmpty() && basicProperties.size() > 1) {
            propertyTaxDetails = new PropertyTaxDetails();
            errorDetails.setErrorCode(PROPERTY_DUPLICATE_ERR_CODE);
            errorDetails.setErrorMessage(PROPERTY_DUPLICATE_ERR_MSG + oldAssessmentNo);
            propertyTaxDetails.setErrorDetails(errorDetails);
        } else if (basicProperty != null) {
            Property property = basicProperty.getProperty();
            if (property != null && property.getIsExemptedFromTax()) {
                propertyTaxDetails = new PropertyTaxDetails();
                errorDetails.setErrorCode(PROPERTY_EXEMPTED_ERR_CODE);
                errorDetails.setErrorMessage(PROPERTY_EXEMPTED_ERR_MSG);
                propertyTaxDetails.setErrorDetails(errorDetails);
            } else {
                propertyTaxDetails = getPropertyTaxDetails(basicProperty, category);
                if (propertyTaxDetails.getErrorDetails() == null) {
                    errorDetails.setErrorCode(THIRD_PARTY_ERR_CODE_SUCCESS);
                    errorDetails.setErrorMessage(THIRD_PARTY_ERR_MSG_SUCCESS);
                    propertyTaxDetails.setErrorDetails(errorDetails);
                }
            }
        } else {
            propertyTaxDetails = new PropertyTaxDetails();
            errorDetails.setErrorCode(PROPERTY_NOT_EXIST_ERR_CODE);
            errorDetails.setErrorMessage(
                    PROPERTY_NOT_EXIST_ERR_MSG_PREFIX + assessmentNo + PROPERTY_NOT_EXIST_ERR_MSG_SUFFIX);
            propertyTaxDetails.setErrorDetails(errorDetails);
        }
        return propertyTaxDetails;
    }

    public List<PropertyTaxDetails> getPropertyTaxDetails(final String assessmentNo, final String ownerName,
            final String mobileNumber, final String category, final String doorNo) {
        final List<BasicProperty> basicProperties = basicPropertyDAO.getBasicPropertiesForTaxDetails(assessmentNo,
                ownerName, mobileNumber, category, doorNo);
        List<PropertyTaxDetails> propTxDetailsList = new ArrayList<>();
        if (null != basicProperties && !basicProperties.isEmpty()) {
            for (final BasicProperty basicProperty : basicProperties) {
                final PropertyTaxDetails propertyTaxDetails = getPropertyTaxDetails(basicProperty, category);
                propTxDetailsList.add(propertyTaxDetails);
            }
        } else {
            PropertyTaxDetails propertyTaxDetails = new PropertyTaxDetails();
            final ErrorDetails errorDetails = new ErrorDetails();
            errorDetails.setErrorCode(PROPERTY_NOT_EXIST_ERR_CODE);
            errorDetails.setErrorMessage(ASSESSMENT + PROPERTY_NOT_EXIST_ERR_MSG_SUFFIX);
            propertyTaxDetails.setErrorDetails(errorDetails);
            propTxDetailsList.add(propertyTaxDetails);
        }
        return propTxDetailsList;
    }

    public List<PropertyTaxDetails> getPropertyTaxDetails(final String circleName, final String zoneName,
            final String wardName, final String blockName, final String ownerName, final String doorNo,
            final String aadhaarNumber, final String mobileNumber) {
        List<PropertyTaxDetails> propTxDetailsList = null;
        final List<BasicProperty> basicPropertyList = basicPropertyDAO.getBasicPropertiesForTaxDetails(circleName,
                zoneName, wardName, blockName, ownerName, doorNo, aadhaarNumber, mobileNumber);
        if (null != basicPropertyList) {
            propTxDetailsList = new ArrayList<>();
            for (final BasicProperty basicProperty : basicPropertyList) {
                final PropertyTaxDetails propertyTaxDetails = getPropertyTaxDetails(basicProperty, null);
                propTxDetailsList.add(propertyTaxDetails);
            }
        }
        return propTxDetailsList;
    }

    // TODO: Needs to check whether this method is required or not.
    // Also existing authentication functionality can be used
    public Boolean authenticateUser(final String username, final String password) {
        Boolean isAuthenticated = false;
        if (username.equals("mahesh") && password.equals("demo"))
            isAuthenticated = true;
        return isAuthenticated;
    }

    private PropertyTaxDetails getPropertyTaxDetails(final BasicProperty basicProperty, String category) {
        final PropertyTaxDetails propertyTaxDetails = new PropertyTaxDetails();
        final ErrorDetails errorDetails = new ErrorDetails();
        if (null != basicProperty) {
            final String assessmentNo = basicProperty.getUpicNo();
            if (!basicProperty.isActive()) {
                errorDetails.setErrorCode(PROPERTY_DEACTIVATE_ERR_CODE);
                errorDetails.setErrorMessage(PROPERTY_DEACTIVATE_ERR_MSG);
                propertyTaxDetails.setErrorDetails(errorDetails);
            } else {
                final Set<PropertyStatusValues> statusValues = basicProperty.getPropertyStatusValuesSet();
                if (null != statusValues && !statusValues.isEmpty())
                    for (final PropertyStatusValues statusValue : statusValues)
                        if (statusValue.getPropertyStatus().getStatusCode() == MARK_DEACTIVE) {
                            errorDetails.setErrorCode(PROPERTY_MARK_DEACTIVATE_ERR_CODE);
                            errorDetails.setErrorMessage(PROPERTY_MARK_DEACTIVATE_ERR_MSG);
                        }
            }
            final Property property = basicProperty.getProperty();
            ptDemandDAO.getDemandCollMap(property);

            if (!StringUtils.isBlank(category)) {
                String propType = property.getPropertyDetail().getPropertyTypeMaster().getCode();
                if (CATEGORY_TYPE_PROPERTY_TAX.equals(category)) {
                    if (propType.equals(OWNERSHIP_TYPE_VAC_LAND)) {
                        errorDetails.setErrorCode(THIRD_PARTY_ERR_CODE_PROPERTY_TAX_ASSESSMENT_NOT_FOUND);
                        errorDetails.setErrorMessage(THIRD_PARTY_ERR_MSG_PROPERTY_TAX_ASSESSMENT_NOT_FOUND);
                        propertyTaxDetails.setErrorDetails(errorDetails);
                        return propertyTaxDetails;
                    }
                } else if (CATEGORY_TYPE_VACANTLAND_TAX.equals(category)) {
                    if (!propType.equals(OWNERSHIP_TYPE_VAC_LAND)) {
                        errorDetails.setErrorCode(THIRD_PARTY_ERR_CODE_VACANTLAND_ASSESSMENT_NOT_FOUND);
                        errorDetails.setErrorMessage(THIRD_PARTY_ERR_MSG_VACANTLAND_ASSESSMENT_NOT_FOUND);
                        propertyTaxDetails.setErrorDetails(errorDetails);
                        return propertyTaxDetails;
                    }
                } else {
                    errorDetails.setErrorCode(THIRD_PARTY_ERR_CODE_WRONG_CATEGORY);
                    errorDetails.setErrorMessage(THIRD_PARTY_ERR_MSG_WRONG_CATEGORY);
                    propertyTaxDetails.setErrorDetails(errorDetails);
                    return propertyTaxDetails;
                }
            }
            final List<PropertyOwnerInfo> propOwnerInfos = property.getBasicProperty().getPropertyOwnerInfo();
            propertyTaxDetails.setOwnerDetails(new ArrayList<OwnerDetails>(0));
            OwnerDetails ow;
            for (int i = 0; i < propOwnerInfos.size(); i++) {
                final PropertyOwnerInfo propOwnerInfo = propOwnerInfos.get(i);
                final String ownerName = propOwnerInfo.getOwner().getName();
                if (null != ownerName && ownerName.trim().length() != 0) {
                    ow = new OwnerDetails();
                    ow.setOwnerName(ownerName);
                    ow.setMobileNo(propOwnerInfo.getOwner().getMobileNumber());
                    propertyTaxDetails.getOwnerDetails().add(ow);
                }
            }
            propertyTaxDetails.setPropertyAddress(property.getBasicProperty().getAddress().toString());
            propertyTaxDetails.setAssessmentNo(property.getBasicProperty().getUpicNo());
            propertyTaxDetails.setOldAssessmentNo(property.getBasicProperty().getOldMuncipalNum());
            propertyTaxDetails.setLocalityName(property.getBasicProperty().getPropertyID().getLocality().getName());

            propertyTaxBillable.setBasicProperty(basicProperty);
            propertyTaxBillable.setLevyPenalty(Boolean.TRUE);
            Map<Installment, PenaltyAndRebate> calculatedPenalty = propertyTaxBillable.getCalculatedPenalty();

            final List<Object> list = ptDemandDAO.getPropertyTaxDetails(assessmentNo);
            if (!list.isEmpty())
                propertyTaxDetails.setTaxDetails(new ArrayList<RestPropertyTaxDetails>(0));
            else {
                return propertyTaxDetails;
            }
            String loopInstallment = "";
            RestPropertyTaxDetails arrearDetails = null;
            BigDecimal total = BigDecimal.ZERO;
            for (final Object record : list) {

                final Object[] data = (Object[]) record;
                final String taxType = (String) data[0];

                final String installment = (String) data[1];
                final Double dmd = (Double) data[2];
                final Double col = (Double) data[3];
                final BigDecimal demand = BigDecimal.valueOf(dmd.doubleValue());
                final BigDecimal collection = BigDecimal.valueOf(col.doubleValue());
                if (loopInstallment.isEmpty()) {
                    loopInstallment = installment;
                    arrearDetails = new RestPropertyTaxDetails();
                    arrearDetails.setInstallment(installment);
                }
                if (loopInstallment.equals(installment)) {

                    if (DEMANDRSN_CODE_PENALTY_FINES.equalsIgnoreCase(taxType))
                        arrearDetails.setPenalty(demand.subtract(collection));
                    else if (DEMANDRSN_CODE_CHQ_BOUNCE_PENALTY.equalsIgnoreCase(taxType))
                        arrearDetails.setChqBouncePenalty(demand.subtract(collection));
                    else
                        total = total.add(demand.subtract(collection));

                } else {
                    arrearDetails.setTaxAmount(total);
                    arrearDetails.setTotalAmount(total.add(arrearDetails.getChqBouncePenalty()));
                    propertyTaxDetails.getTaxDetails().add(arrearDetails);
                    loopInstallment = installment;
                    arrearDetails = new RestPropertyTaxDetails();
                    arrearDetails.setInstallment(installment);
                    total = BigDecimal.ZERO;
                    if (DEMANDRSN_CODE_PENALTY_FINES.equalsIgnoreCase(taxType))
                        arrearDetails.setPenalty(demand.subtract(collection));
                    else if (DEMANDRSN_CODE_CHQ_BOUNCE_PENALTY.equalsIgnoreCase(taxType))
                        arrearDetails.setChqBouncePenalty(demand.subtract(collection));
                    else
                        total = total.add(demand.subtract(collection));

                }
            }
            if (arrearDetails != null) {
                arrearDetails.setTaxAmount(total);
                arrearDetails.setTotalAmount(total.add(arrearDetails.getChqBouncePenalty()));
                propertyTaxDetails.getTaxDetails().add(arrearDetails);
            }

            Set<Installment> keySet = calculatedPenalty.keySet();

            // for all years data
            for (RestPropertyTaxDetails details : propertyTaxDetails.getTaxDetails()) {
                // loop trough the penalty
                for (Installment inst : keySet) {
                    if (inst.getDescription().equalsIgnoreCase(details.getInstallment())) {
                        details.setPenalty(calculatedPenalty.get(inst).getPenalty());
                        details.setRebate(calculatedPenalty.get(inst).getRebate());
                        details.setTotalAmount(details.getTotalAmount().add(calculatedPenalty.get(inst).getPenalty()));
                        if (details.getRebate() != null) {
                            details.setTotalAmount(details.getTotalAmount().subtract(details.getRebate()));
                        }
                        break;
                    }
                }

            }
        }

        return propertyTaxDetails;
    }

    public ReceiptDetails payPropertyTax(final PayPropertyTaxDetails payPropertyTaxDetails, String propertyType) {
        ReceiptDetails receiptDetails = null;
        ErrorDetails errorDetails;
        BigDecimal totalAmountToBePaid = BigDecimal.ZERO;
        final BasicProperty basicProperty = basicPropertyDAO
                .getBasicPropertyByPropertyID(payPropertyTaxDetails.getAssessmentNo());
        if (propertyType.equalsIgnoreCase(OWNERSHIP_TYPE_VAC_LAND))
            propertyTaxBillable.setVacantLandTaxPayment(true);

        propertyTaxBillable.setBasicProperty(basicProperty);
        propertyTaxBillable.setReferenceNumber(propertyTaxNumberGenerator
                .generateBillNumber(basicProperty.getPropertyID().getWard().getBoundaryNum().toString()));
        propertyTaxBillable.setBillType(egBillDAO.getBillTypeByCode(BILLTYPE_AUTO));
        propertyTaxBillable.setLevyPenalty(Boolean.TRUE);
        propertyTaxBillable.setTransanctionReferenceNumber(payPropertyTaxDetails.getTransactionId());
        final EgBill egBill = ptBillServiceImpl.generateBill(propertyTaxBillable);

        for (EgBillDetails billDetails : egBill.getEgBillDetails()) {
            if (!(billDetails.getDescription().contains(PropertyTaxConstants.DEMANDRSN_STR_ADVANCE)
                    || billDetails.getDescription().contains(PropertyTaxConstants.DEMANDRSN_CODE_REBATE))
                    && billDetails.getCrAmount().compareTo(BigDecimal.ZERO) > 0) {
                totalAmountToBePaid = totalAmountToBePaid.add(billDetails.getCrAmount());
            } else if (billDetails.getDescription().contains(PropertyTaxConstants.DEMANDRSN_CODE_REBATE)) {
                totalAmountToBePaid = totalAmountToBePaid.subtract(billDetails.getDrAmount());
            }
        }
        totalAmountToBePaid = totalAmountToBePaid.setScale(0, BigDecimal.ROUND_CEILING);
        final CollectionHelper collectionHelper = new CollectionHelper(egBill);
        final Map<String, String> paymentDetailsMap = new HashMap<>();
        paymentDetailsMap.put(TOTAL_AMOUNT, payPropertyTaxDetails.getPaymentAmount().toString());
        paymentDetailsMap.put(PAID_BY, payPropertyTaxDetails.getPaidBy());
        if (THIRD_PARTY_PAYMENT_MODE_CHEQUE.equalsIgnoreCase(payPropertyTaxDetails.getPaymentMode().toLowerCase())
                || THIRD_PARTY_PAYMENT_MODE_DD.equalsIgnoreCase(payPropertyTaxDetails.getPaymentMode().toLowerCase())) {
            paymentDetailsMap.put(ChequePayment.INSTRUMENTNUMBER, payPropertyTaxDetails.getChqddNo());
            paymentDetailsMap.put(ChequePayment.INSTRUMENTDATE,
                    ChequePayment.CHEQUE_DATE_FORMAT.format(payPropertyTaxDetails.getChqddDate()));
            paymentDetailsMap.put(ChequePayment.BRANCHNAME, payPropertyTaxDetails.getBranchName());
            final Long validatesBankId = validateBank(payPropertyTaxDetails.getBankName());
            paymentDetailsMap.put(ChequePayment.BANKID, validatesBankId.toString());
        }
        final Payment payment = Payment.create(payPropertyTaxDetails.getPaymentMode().toLowerCase(), paymentDetailsMap);
        final BillReceiptInfo billReceiptInfo = collectionHelper.executeCollection(payment,
                payPropertyTaxDetails.getSource());

        if (null != billReceiptInfo) {
            receiptDetails = new ReceiptDetails();
            receiptDetails.setReceiptNo(billReceiptInfo.getReceiptNum());
            receiptDetails.setReceiptDate(formatDate(billReceiptInfo.getReceiptDate()));
            receiptDetails.setPayeeName(billReceiptInfo.getPayeeName());
            receiptDetails.setPayeeAddress(billReceiptInfo.getPayeeAddress());
            receiptDetails.setBillReferenceNo(billReceiptInfo.getBillReferenceNum());
            receiptDetails.setServiceName(billReceiptInfo.getServiceName());
            receiptDetails.setDescription(billReceiptInfo.getDescription());
            receiptDetails.setPaidBy(billReceiptInfo.getPaidBy());
            receiptDetails.setPaymentAmount(billReceiptInfo.getTotalAmount());
            receiptDetails.setPaymentMode(payPropertyTaxDetails.getPaymentMode());
            receiptDetails.setTransactionId(billReceiptInfo.getManualReceiptNumber());

            String[] paidFrom = null;
            String[] paidTo = null;
            Installment fromInstallment = null;
            Installment toInstallment = null;
            if (totalAmountToBePaid.compareTo(BigDecimal.ZERO) > 0) {
                List<ReceiptAccountInfo> receiptAccountsList = new ArrayList<>(
                        billReceiptInfo.getAccountDetails());
                Collections.sort(receiptAccountsList, new Comparator<ReceiptAccountInfo>() {
                    @Override
                    public int compare(ReceiptAccountInfo rcptAcctInfo1, ReceiptAccountInfo rcptAcctInfo2) {
                        if (rcptAcctInfo1.getOrderNumber() != null && rcptAcctInfo2.getOrderNumber() != null)
                            return rcptAcctInfo1.getOrderNumber().compareTo(rcptAcctInfo2.getOrderNumber());
                        return 0;
                    }
                });
                for (ReceiptAccountInfo rcptAcctInfo : receiptAccountsList) {
                    if (rcptAcctInfo.getCrAmount().compareTo(ZERO) > 0
                            && !rcptAcctInfo.getDescription().contains(PropertyTaxConstants.DEMANDRSN_STR_ADVANCE)) {
                        if (paidFrom == null)
                            paidFrom = rcptAcctInfo.getDescription().split("-", 2);
                        paidTo = rcptAcctInfo.getDescription().split("-", 2);
                    }
                }
                Module module = moduleService.getModuleByName(PropertyTaxConstants.PTMODULENAME);
                if (paidFrom != null)
                    fromInstallment = installmentDao.getInsatllmentByModuleAndDescription(
                            module, paidFrom[1]);
                if (paidTo != null)
                    toInstallment = installmentDao.getInsatllmentByModuleAndDescription(
                            module, paidTo[1]);
            }
            /**
             * If collection is done for complete current financial year only, todate shown is last date of current financial year
             * and payment type is 'Fully'. In case, collection is done for complete current financial year with advance, todate
             * shown is last date of current financial year and payment type is 'Advance'. In case, collection is only for
             * advance, collection period will be blank and payment type is 'Advance'
             */
            if (totalAmountToBePaid.compareTo(BigDecimal.ZERO) == 0) {
                receiptDetails.setPaymentPeriod(StringUtils.EMPTY);
                receiptDetails.setPaymentType(PAYMENT_TYPE_ADVANCE);
            } else
                receiptDetails.setPaymentPeriod(DateUtils.getDefaultFormattedDate(fromInstallment.getFromDate())
                        .concat(" to ").concat(DateUtils.getDefaultFormattedDate(toInstallment.getToDate())));

            if (payPropertyTaxDetails.getPaymentAmount().compareTo(totalAmountToBePaid) > 0)
                receiptDetails.setPaymentType(PAYMENT_TYPE_ADVANCE);
            else if (totalAmountToBePaid.compareTo(payPropertyTaxDetails.getPaymentAmount()) > 0)
                receiptDetails.setPaymentType(PAYMENT_TYPE_PARTIALLY);
            else
                receiptDetails.setPaymentType(PAYMENT_TYPE_FULLY);

            basicPropertyService.update(basicProperty);
            errorDetails = new ErrorDetails();
            errorDetails.setErrorCode(THIRD_PARTY_ERR_CODE_SUCCESS);
            errorDetails.setErrorMessage(THIRD_PARTY_ERR_MSG_SUCCESS);

            receiptDetails.setErrorDetails(errorDetails);
        }
        return receiptDetails;
    }

    private Long validateBank(final String bankCodeOrName) {

        Bank bank = bankHibernateDAO.getBankByCode(bankCodeOrName);
        if (bank == null)
            // Tries by name if code not found
            bank = bankHibernateDAO.getBankByCode(bankCodeOrName);
        return Long.valueOf(bank.getId());

    }

    public ErrorDetails payWaterTax(final String consumerNo, final String paymentMode, final BigDecimal totalAmount,
            final String paidBy) {
        ErrorDetails errorDetails = validatePaymentDetails(consumerNo, paymentMode, totalAmount, paidBy);
        if (null != errorDetails)
            return errorDetails;
        else {
            errorDetails = new ErrorDetails();
            errorDetails.setErrorCode(THIRD_PARTY_ERR_CODE_SUCCESS);
            errorDetails.setErrorMessage(THIRD_PARTY_ERR_MSG_SUCCESS);
        }
        return errorDetails;
    }

    public List<MasterCodeNamePairDetails> getPropertyTypeMasterDetails() {
        final List<MasterCodeNamePairDetails> propTypeMasterDetailsList = new ArrayList<>(0);
        final List<PropertyTypeMaster> propertyTypeMasters = propertyTypeMasterDAO.findAllExcludeEWSHS();
        for (final PropertyTypeMaster propertyTypeMaster : propertyTypeMasters) {
            final MasterCodeNamePairDetails propTypeMasterDetails = new MasterCodeNamePairDetails();
            propTypeMasterDetails.setCode(propertyTypeMaster.getCode());
            propTypeMasterDetails.setName(propertyTypeMaster.getType());
            propTypeMasterDetailsList.add(propTypeMasterDetails);
        }
        return propTypeMasterDetailsList;
    }

    public PropertyTypeMaster getPropertyTypeMasterByCode(final String propertyTypeMasterCode) {
        return propertyTypeMasterDAO.getPropertyTypeMasterByCode(propertyTypeMasterCode);
    }

    public List<MasterCodeNamePairDetails> getPropertyTypeCategoryDetails(final String categoryCode) {
        final List<MasterCodeNamePairDetails> mstrCodeNamePairDetailsList = new ArrayList<>(0);
        Map<String, String> codeNameMap;
        final PropertyTypeMaster propertyTypeMasters = propertyTypeMasterDAO.getPropertyTypeMasterByCode(categoryCode);
        if (null != propertyTypeMasters) {
            if (propertyTypeMasters.getCode().equalsIgnoreCase(OWNERSHIP_TYPE_VAC_LAND))
                codeNameMap = VAC_LAND_PROPERTY_TYPE_CATEGORY;
            else
                codeNameMap = NON_VAC_LAND_PROPERTY_TYPE_CATEGORY;
            if (null != codeNameMap && !codeNameMap.isEmpty())
                for (final String code : codeNameMap.keySet()) {
                    final MasterCodeNamePairDetails mstrCodeNamepairDetails = new MasterCodeNamePairDetails();
                    mstrCodeNamepairDetails.setCode(code);
                    mstrCodeNamepairDetails.setName(codeNameMap.get(code));
                    mstrCodeNamePairDetailsList.add(mstrCodeNamepairDetails);
                }
        }
        return mstrCodeNamePairDetailsList;
    }

    public List<MasterCodeNamePairDetails> getPropertyTypes() {
        final List<MasterCodeNamePairDetails> mstrCodeNamePairDetailsList = new ArrayList<>();
        MasterCodeNamePairDetails mstrCodeNamepairDetails;
        final Map<String, String> vacLandMap = VAC_LAND_PROPERTY_TYPE_CATEGORY;
        final Map<String, String> nonVacLandMap = NON_VAC_LAND_PROPERTY_TYPE_CATEGORY;

        for (final String key : vacLandMap.keySet()) {
            mstrCodeNamepairDetails = new MasterCodeNamePairDetails();
            mstrCodeNamepairDetails.setCode(key);
            mstrCodeNamepairDetails.setName(vacLandMap.get(key));
            mstrCodeNamePairDetailsList.add(mstrCodeNamepairDetails);
        }

        for (final String code : nonVacLandMap.keySet()) {
            mstrCodeNamepairDetails = new MasterCodeNamePairDetails();
            mstrCodeNamepairDetails.setCode(code);
            mstrCodeNamepairDetails.setName(nonVacLandMap.get(code));
            mstrCodeNamePairDetailsList.add(mstrCodeNamepairDetails);
        }
        return mstrCodeNamePairDetailsList;
    }

    public List<MasterCodeNamePairDetails> getApartmentsAndComplexes() {
        final List<MasterCodeNamePairDetails> mstrCodeNamePairDetailsList = new ArrayList<>(0);
        final List<Apartment> apartmentList = apartmentService.getAllApartments();
        if (null != apartmentList)
            for (final Apartment apartment : apartmentList) {
                final MasterCodeNamePairDetails mstrCodeNamePairDetails = new MasterCodeNamePairDetails();
                mstrCodeNamePairDetails.setCode(apartment.getCode());
                mstrCodeNamePairDetails.setName(apartment.getName());
                mstrCodeNamePairDetailsList.add(mstrCodeNamePairDetails);
            }
        return mstrCodeNamePairDetailsList;
    }

    public List<MasterCodeNamePairDetails> getReasonsForChangeProperty(final String reason) {
        final List<MasterCodeNamePairDetails> mstrCodeNamePairDetailsList = new ArrayList<>(0);
        final List<PropertyMutationMaster> propMutationMasterList = propertyMutationMasterDAO
                .getAllPropertyMutationMastersByType(reason);
        if (null != propMutationMasterList)
            for (final PropertyMutationMaster propMutationMaster : propMutationMasterList) {
                final MasterCodeNamePairDetails mstrCodeNamePairDetails = new MasterCodeNamePairDetails();
                mstrCodeNamePairDetails.setCode(propMutationMaster.getCode());
                mstrCodeNamePairDetails.setName(propMutationMaster.getMutationName());
                mstrCodeNamePairDetailsList.add(mstrCodeNamePairDetails);
            }
        return mstrCodeNamePairDetailsList;
    }

    public List<MasterCodeNamePairDetails> getBoundariesByBoundaryTypeAndHierarchyType(final String boundaryType,
            final String hierarchyType) {
        final List<MasterCodeNamePairDetails> mstrCodeNamePairDetailsList = new ArrayList<>(0);
        final List<Boundary> boundaryList = boundaryService
                .getActiveBoundariesByBndryTypeNameAndHierarchyTypeName(boundaryType, hierarchyType);
        if (boundaryList != null)
            for (final Boundary boundary : boundaryList) {
                final MasterCodeNamePairDetails mstrCodeNamePairDetails = new MasterCodeNamePairDetails();
                mstrCodeNamePairDetails.setCode(boundary.getBoundaryNum().toString());
                mstrCodeNamePairDetails.setName(boundary.getName());
                mstrCodeNamePairDetailsList.add(mstrCodeNamePairDetails);
            }
        return mstrCodeNamePairDetailsList;
    }

    public LocalityDetails getLocalityDetailsByLocalityCode(final String localityCode) {
        final Long boundaryNo = Long.valueOf(localityCode.substring(0, localityCode.indexOf('~')).trim());
        final String name = localityCode.substring(localityCode.indexOf('~') + 1).trim();
        LocalityDetails localityDetails = null;
        Query qry = entityManager.createQuery("from Boundary b where b.boundaryNum = :boundaryNo and b.name = :name");
        qry.setParameter("boundaryNo", boundaryNo);
        qry.setParameter("name", name);
        List list = qry.getResultList();
        if (null != list && !list.isEmpty()) {
            localityDetails = new LocalityDetails();
            final Boundary boundary = (Boundary) list.get(0);
            qry = entityManager.createQuery("from CrossHierarchy cr where cr.child = :child");
            qry.setParameter("child", boundary);
            list = qry.getResultList();
            if (null != list && !list.isEmpty()) {
                final CrossHierarchy crossHeirarchyImpl = (CrossHierarchy) list.get(0);
                qry = entityManager.createQuery(FROM_BOUNDARY_B_WHERE_B_ID_ID);
                qry.setParameter("id", crossHeirarchyImpl.getParent().getId());
                list = qry.getResultList();
                if (null != list && !list.isEmpty()) {
                    final Boundary block = (Boundary) list.get(0);
                    localityDetails.setBlockName(block.getName());
                    qry = entityManager.createQuery(FROM_BOUNDARY_B_WHERE_B_ID_ID);
                    qry.setParameter("id", block.getParent().getId());
                    list = qry.getResultList();
                    if (null != list && !list.isEmpty()) {
                        final Boundary ward = (Boundary) list.get(0);
                        localityDetails.setWardName(ward.getName());
                        qry = entityManager.createQuery(FROM_BOUNDARY_B_WHERE_B_ID_ID);
                        qry.setParameter("id", ward.getParent().getId());
                        list = qry.getResultList();
                        if (null != list && !list.isEmpty()) {
                            final Boundary zone = (Boundary) list.get(0);
                            localityDetails.setZoneName(zone.getName());
                        }
                    }
                }
            }
        }
        return localityDetails;
    }

    public Boundary getBoundaryByNumberAndType(final String boundaryNum, final String boundaryTypeName,
            final String hierarchyName) {
        final BoundaryType boundaryType = boundaryTypeService
                .getBoundaryTypeByNameAndHierarchyTypeName(boundaryTypeName, hierarchyName);
        return boundaryService.getBoundaryByTypeAndNo(boundaryType, Long.valueOf(boundaryNum));
    }

    public List<MasterCodeNamePairDetails> getEnumerationBlocks() {
        final List<MasterCodeNamePairDetails> mstrCodeNamePairDetailsList = new ArrayList<>();
        final List<Boundary> enumerationBlockList = boundaryService
                .getActiveBoundariesByBndryTypeNameAndHierarchyTypeName(ELECTIONWARD_BNDRY_TYPE,
                        ELECTION_HIERARCHY_TYPE);
        for (final Boundary boundary : enumerationBlockList) {
            final MasterCodeNamePairDetails mstrCodeNamePairDetails = new MasterCodeNamePairDetails();
            mstrCodeNamePairDetails.setCode(boundary.getBoundaryNum().toString());
            mstrCodeNamePairDetails.setName(boundary.getName());
            mstrCodeNamePairDetailsList.add(mstrCodeNamePairDetails);
        }
        return mstrCodeNamePairDetailsList;
    }

    public List<MasterCodeNamePairDetails> getFloorTypes() {
        final List<MasterCodeNamePairDetails> mstrCodeNamePairDetailsList = new ArrayList<>();
        final List<FloorType> floorTypeList = floorTypeService.getAllFloors();
        for (final FloorType floorType : floorTypeList) {
            final MasterCodeNamePairDetails mstrCodeNamePairDetails = new MasterCodeNamePairDetails();
            mstrCodeNamePairDetails.setCode(floorType.getId().toString());
            mstrCodeNamePairDetails.setName(floorType.getName());
            mstrCodeNamePairDetailsList.add(mstrCodeNamePairDetails);
        }
        return mstrCodeNamePairDetailsList;
    }

    public List<MasterCodeNamePairDetails> getRoofTypes() {
        final List<MasterCodeNamePairDetails> mstrCodeNamePairDetailsList = new ArrayList<>();
        final List<RoofType> roofTypeList = roofTypeService.getAllRoofTypes();
        for (final RoofType roofType : roofTypeList) {
            final MasterCodeNamePairDetails mstrCodeNamePairDetails = new MasterCodeNamePairDetails();
            mstrCodeNamePairDetails.setCode(roofType.getId().toString());
            mstrCodeNamePairDetails.setName(roofType.getName());
            mstrCodeNamePairDetailsList.add(mstrCodeNamePairDetails);
        }
        return mstrCodeNamePairDetailsList;
    }

    public List<MasterCodeNamePairDetails> getWallTypes() {
        final List<MasterCodeNamePairDetails> mstrCodeNamePairDetailsList = new ArrayList<>();
        final List<WallType> wallTypeList = wallTypeService.getAllWalls();
        for (final WallType wallType : wallTypeList) {
            final MasterCodeNamePairDetails mstrCodeNamePairDetails = new MasterCodeNamePairDetails();
            mstrCodeNamePairDetails.setCode(wallType.getId().toString());
            mstrCodeNamePairDetails.setName(wallType.getName());
            mstrCodeNamePairDetailsList.add(mstrCodeNamePairDetails);
        }
        return mstrCodeNamePairDetailsList;
    }

    public List<MasterCodeNamePairDetails> getWoodTypes() {
        final List<MasterCodeNamePairDetails> mstrCodeNamePairDetailsList = new ArrayList<>();
        final List<WoodType> woodTypeList = woodTypeService.getAllWoodTypes();
        for (final WoodType woodType : woodTypeList) {
            final MasterCodeNamePairDetails mstrCodeNamePairDetails = new MasterCodeNamePairDetails();
            mstrCodeNamePairDetails.setCode(woodType.getId().toString());
            mstrCodeNamePairDetails.setName(woodType.getName());
            mstrCodeNamePairDetailsList.add(mstrCodeNamePairDetails);
        }
        return mstrCodeNamePairDetailsList;
    }

    public List<MasterCodeNamePairDetails> getBuildingClassifications() {
        final List<MasterCodeNamePairDetails> mstrCodeNamePairDetailsList = new ArrayList<>();
        final List<StructureClassification> structClsfList = structureClassificationService.getAllStructureTypes();
        for (final StructureClassification structClsf : structClsfList) {
            final MasterCodeNamePairDetails mstrCodeNamePairDetails = new MasterCodeNamePairDetails();
            mstrCodeNamePairDetails.setCode(structClsf.getConstrTypeCode());
            mstrCodeNamePairDetails.setName(structClsf.getTypeName());
            mstrCodeNamePairDetailsList.add(mstrCodeNamePairDetails);
        }
        return mstrCodeNamePairDetailsList;
    }

    public List<MasterCodeNamePairDetails> getNatureOfUsages() {
        final List<MasterCodeNamePairDetails> mstrCodeNamePairDetailsList = new ArrayList<>();
        final List<PropertyUsage> usageList = propertyUsageService.getAllActivePropertyUsages();
        for (final PropertyUsage propUsage : usageList) {
            final MasterCodeNamePairDetails mstrCodeNamePairDetails = new MasterCodeNamePairDetails();
            mstrCodeNamePairDetails.setCode(propUsage.getUsageCode());
            mstrCodeNamePairDetails.setName(propUsage.getUsageName());
            mstrCodeNamePairDetailsList.add(mstrCodeNamePairDetails);
        }
        return mstrCodeNamePairDetailsList;
    }

    public List<MasterCodeNamePairDetails> getOccupancies() {
        final List<MasterCodeNamePairDetails> mstrCodeNamePairDetailsList = new ArrayList<>();
        final List<PropertyOccupation> propOccupList = propertyOccupationService.getAllPropertyOccupations();
        for (final PropertyOccupation propOccup : propOccupList) {
            final MasterCodeNamePairDetails mstrCodeNamePairDetails = new MasterCodeNamePairDetails();
            mstrCodeNamePairDetails.setCode(propOccup.getOccupancyCode());
            mstrCodeNamePairDetails.setName(propOccup.getOccupation());
            mstrCodeNamePairDetailsList.add(mstrCodeNamePairDetails);
        }
        return mstrCodeNamePairDetailsList;
    }

    public PropertyOccupation getPropertyOccupationByOccupancyCode(final String occupancyCode) {
        return propertyOccupationService.getPropertyOccupationByCode(occupancyCode);
    }

    public List<MasterCodeNamePairDetails> getExemptionCategories() {
        final List<MasterCodeNamePairDetails> mstrCodeNamePairDetailsList = new ArrayList<>();
        final List<TaxExemptionReason> taxExemptionReasonList = taxExemptionReasonService.getAllActiveTaxExemptions();
        for (final TaxExemptionReason taxExemptionReason : taxExemptionReasonList) {
            final MasterCodeNamePairDetails mstrCodeNamePairDetails = new MasterCodeNamePairDetails();
            mstrCodeNamePairDetails.setCode(taxExemptionReason.getCode());
            mstrCodeNamePairDetails.setName(taxExemptionReason.getName());
            mstrCodeNamePairDetailsList.add(mstrCodeNamePairDetails);
        }
        return mstrCodeNamePairDetailsList;
    }

    public List<MasterCodeNamePairDetails> getApproverDepartments() {
        final List<MasterCodeNamePairDetails> mstrCodeNamePairDetailsList = new ArrayList<>();
        final List<Department> approverDepartmentList = departmentService.getAllDepartments();
        for (final Department approverDepartment : approverDepartmentList) {
            final MasterCodeNamePairDetails mstrCodeNamePairDetails = new MasterCodeNamePairDetails();
            mstrCodeNamePairDetails.setCode(approverDepartment.getCode());
            mstrCodeNamePairDetails.setName(approverDepartment.getName());
            mstrCodeNamePairDetailsList.add(mstrCodeNamePairDetails);
        }
        return mstrCodeNamePairDetailsList;
    }

    @Transactional
    public NewPropertyDetails createNewProperty(ViewPropertyDetails viewpropertyDetails) throws ParseException {

        NewPropertyDetails newPropertyDetails = null;
        final PropertyService propService = beanProvider.getBean(PROP_SERVICE, PropertyService.class);
        BasicProperty basicProperty = createBscPropty(viewpropertyDetails, propService);
        final Address ownerCorrAddr = createCorrespondenceAddress(viewpropertyDetails, basicProperty.getAddress());
        PropertyImpl property = (PropertyImpl) basicProperty.getWFProperty();
        property.setSource(SOURCE_SURVEY);
        property.setReferenceId(viewpropertyDetails.getReferenceId());
        List<File> fileAttachments = new ArrayList<>(0);
        List<String> uploadContentTypes = new ArrayList<>(0);
        List<String> uploadFileNames = new ArrayList<>(0);
        basicProperty.setIsTaxXMLMigrated(STATUS_YES_XML_MIGRATION);
        if (viewpropertyDetails.getTwSigned()) {
            viewpropertyDetails.setTwSigned(false);
        }
        processAndStoreDocumentsWithReason(basicProperty, DOCS_CREATE_PROPERTY, fileAttachments, uploadFileNames,
                uploadContentTypes);
        basicPropertyService.createOwners(property, basicProperty, ownerCorrAddr);
        /*
         * Duplicate GIS property will be persisted, which will be used for generating comparison reports
         */
        PropertyImpl gisProperty = (PropertyImpl) property.createPropertyclone();
        Ptdemand ptdemand = property.getPtDemandSet().iterator().next();
        Ptdemand gisPtdemand = gisProperty.getPtDemandSet().iterator().next();
        if (gisPtdemand != null)
            gisPtdemand.getDmdCalculations().setAlv(ptdemand.getDmdCalculations().getAlv());
        if (!gisProperty.getPropertyDetail().getFloorDetails().isEmpty()) {
            for (Floor floor : gisProperty.getPropertyDetail().getFloorDetails())
                floor.setPropertyDetail(gisProperty.getPropertyDetail());
        }
        gisProperty.setStatus('G');
        gisProperty.setSource(SOURCE_SURVEY);
        BigDecimal gisTax = BigDecimal.ZERO;
        if (gisPtdemand != null) {
            gisTax = propService.getSurveyTax(gisProperty, gisPtdemand.getEgInstallmentMaster().getFromDate());
        }
        GisDetails gisDetails = new GisDetails();
        gisDetails.setGisProperty(gisProperty);
        gisDetails.setApplicationProperty(property);
        gisDetails.setGisTax(gisTax);
        gisDetails.setApplicationTax(gisTax);
        gisDetails.setGisZone(basicProperty.getPropertyID().getZone());
        gisDetails.setPropertyZone(basicProperty.getPropertyID().getZone());
        gisProperty.setGisDetails(gisDetails);
        property.setGisDetails(gisDetails);

        basicProperty.addProperty(gisProperty);
        transitionWorkFlow(property, propService, PROPERTY_MODE_CREATE);
        basicPropertyService.applyAuditing(property.getState());
        SurveyBean surveyBean = new SurveyBean();
        surveyBean.setProperty(property);
        surveyBean.setGisTax(gisTax);
        surveyBean.setApplicationTax(gisTax);
        surveyBean.setAgeOfCompletion(propService.getSlaValue(APPLICATION_TYPE_NEW_ASSESSENT));

        basicProperty = basicPropertyService.persist(basicProperty);
        propService.updateIndexes(property, APPLICATION_TYPE_NEW_ASSESSENT);
        surveyService.updateSurveyIndex(APPLICATION_TYPE_NEW_ASSESSENT, surveyBean);
        saveDocumentTypeDetails(basicProperty, viewpropertyDetails);
        if (null != basicProperty) {
            newPropertyDetails = new NewPropertyDetails();
            newPropertyDetails.setReferenceId(viewpropertyDetails.getReferenceId());
            newPropertyDetails.setApplicationNo(property.getApplicationNo());
            final ErrorDetails errorDetails = new ErrorDetails();
            errorDetails.setErrorCode(THIRD_PARTY_ERR_CODE_SUCCESS);
            errorDetails.setErrorMessage(THIRD_PARTY_ERR_MSG_SUCCESS);
            newPropertyDetails.setErrorDetails(errorDetails);
        }
        return newPropertyDetails;
    }

    private BasicProperty createBscPropty(ViewPropertyDetails viewPropertyDetails, final PropertyService propService)
            throws ParseException {

        final PropertyImpl property;
        final BasicProperty basicProperty = setBasicPropertyValues(viewPropertyDetails, propService);
        final PropertyTypeMaster propertyTypeMaster = getPropertyTypeMasterByCode(viewPropertyDetails.getPropertyTypeMaster());
        final PropertyImpl propertyImpl = createPropertyWithBasicDetails(viewPropertyDetails.getPropertyTypeMaster());
        propertyImpl.setBasicProperty(basicProperty);

        setPropertyDetails(viewPropertyDetails, propertyImpl);

        if (!viewPropertyDetails.getPropertyTypeMaster().equalsIgnoreCase(OWNERSHIP_TYPE_VAC_LAND)) {
            FloorType floorType = null;
            RoofType roofType = null;
            WallType wallType = null;
            WoodType woodType = null;
            if (StringUtils.isNotBlank(viewPropertyDetails.getFloorType()))
                floorType = floorTypeService.getFloorTypeById(Long.valueOf(viewPropertyDetails.getFloorType()));
            if (StringUtils.isNotBlank(viewPropertyDetails.getRoofType()))
                roofType = roofTypeService.getRoofTypeById(Long.valueOf(viewPropertyDetails.getRoofType()));
            if (StringUtils.isNotBlank(viewPropertyDetails.getWallType()))
                wallType = wallTypeService.getWallTypeById(Long.valueOf(viewPropertyDetails.getWallType()));
            if (StringUtils.isNotBlank(viewPropertyDetails.getWoodType()))
                woodType = woodTypeService.getWoodTypeById(Long.valueOf(viewPropertyDetails.getWoodType()));

            propertyImpl.getPropertyDetail().setFloorDetailsProxy(getFloorList(viewPropertyDetails.getFloorDetails()));
            setAmenities(viewPropertyDetails, propertyImpl);

            property = propService.createProperty(propertyImpl, viewPropertyDetails.getExtentOfSite(),
                    viewPropertyDetails.getMutationReason(),
                    propertyTypeMaster.getId().toString(), null, null, STATUS_ISACTIVE, null, null,
                    floorType != null ? floorType.getId() : null, roofType != null ? roofType.getId() : null,
                    wallType != null ? wallType.getId() : null, woodType != null ? woodType.getId() : null, null, null,
                    null, null, Boolean.FALSE);
        } else {
            setVacantLandDetails(viewPropertyDetails, propertyImpl);
            property = propService.createProperty(propertyImpl, viewPropertyDetails.getExtentOfSite(),
                    viewPropertyDetails.getMutationReason(),
                    propertyTypeMaster.getId().toString(), null, null, STATUS_ISACTIVE, viewPropertyDetails.getRegdDocNo(), null,
                    null,
                    null, null, null, null, null, viewPropertyDetails.getVlPlotArea(),
                    viewPropertyDetails.getLaAuthority(),
                    Boolean.FALSE);
        }

        Date propCompletionDate = getCompletionDate(propService, property);
        property.setStatus(STATUS_WORKFLOW);
        property.setPropertyModifyReason(PROP_CREATE_RSN);
        property.getPropertyDetail().setCategoryType(viewPropertyDetails.getCategory());
        basicProperty.addProperty(property);

        basicProperty.setPropOccupationDate(propCompletionDate);
        setBasicPropOwnerInfo(viewPropertyDetails, basicProperty);

        if (property != null && !property.getDocuments().isEmpty())
            propService.processAndStoreDocument(property.getDocuments());
        try {
            propService.createDemand(propertyImpl, propCompletionDate);
        } catch (TaxCalculatorExeption e) {
            LOGGER.error("Error while calculating Taxes", e.getMessage());
        }
        return basicProperty;
    }

    private BasicProperty setBasicPropertyValues(ViewPropertyDetails viewPropertyDetails, PropertyService propService) {
        BasicProperty basicProperty = new BasicPropertyImpl();
        basicProperty.setActive(Boolean.TRUE);
        basicProperty.setSource(SOURCEOFDATA_SURVEY);

        // Get PropertyStatus object to set the status of the property
        final PropertyStatus propertyStatus = propertyStatusHibernateDAO.getPropertyStatusByCode(PROPERTY_STATUS_WORKFLOW);
        basicProperty.setStatus(propertyStatus);
        basicProperty.setUnderWorkflow(Boolean.TRUE);
        basicProperty.setParcelId(viewPropertyDetails.getParcelId());
        basicProperty.setLatitude(viewPropertyDetails.getLatitude());
        basicProperty.setLongitude(viewPropertyDetails.getLongitude());
        // Set isBillCreated property value as false
        basicProperty.setIsBillCreated(STATUS_BILL_NOTCREATED);
        // Set PropertyMutationMaster object
        PropertyMutationMaster propertyMutationMaster = propertyMutationMasterDAO
                .getPropertyMutationMasterByCode(viewPropertyDetails.getMutationReason());
        basicProperty.setPropertyMutationMaster(propertyMutationMaster);
        // Creating Property Address object
        final Boundary block = getBoundaryByNumberAndType(viewPropertyDetails.getBlockName(), BLOCK, REVENUE_HIERARCHY_TYPE);
        final PropertyAddress propAddress = createPropAddress(viewPropertyDetails, block);
        basicProperty.setAddress(propAddress);
        // Creating PropertyID object based on basic property, localityCode and boundary map direction
        final PropertyID propertyID = createPropertID(basicProperty, viewPropertyDetails, block);
        basicProperty.setPropertyID(propertyID);

        // need to pass parent property index, in case of bifurcation
        if (propertyMutationMaster.getCode().equals(PROP_CREATE_RSN_BIFUR))
            basicProperty.addPropertyStatusValues(propService.createPropStatVal(basicProperty, PROP_CREATE_RSN, null,
                    null, null, null, viewPropertyDetails.getParentPropertyAssessmentNo()));

        return basicProperty;
    }

    private void setBasicPropOwnerInfo(ViewPropertyDetails viewPropertyDetails, BasicProperty basicProperty) {
        if (StringUtils.isNotBlank(viewPropertyDetails.getDocType())
                && PropertyTaxConstants.DOCUMENT_NAME_NOTARY_DOCUMENT.equals(viewPropertyDetails.getDocType()))
            basicProperty
                    .setPropertyOwnerInfoProxy(getNotaryOwners(viewPropertyDetails.getOwnerDetails().get(0).getMobileNumber()));
        else
            basicProperty.setPropertyOwnerInfoProxy(getPropertyOwnerInfoList(viewPropertyDetails.getOwnerDetails()));
    }

    private List<PropertyOwnerInfo> getNotaryOwners(String mobileNo) {
        List<PropertyOwnerInfo> notaryOwnersList = new ArrayList<>();
        PropertyOwnerInfo notaryPropOwner = new PropertyOwnerInfo();
        User notaryUser = userService.getUserByUsername(PropertyTaxConstants.NOTARY_DOCUMENT_OWNER);
        notaryUser.setMobileNumber(mobileNo);
        notaryPropOwner.setOwner(notaryUser);
        notaryOwnersList.add(notaryPropOwner);
        return notaryOwnersList;
    }

    private void setPropertyDetails(ViewPropertyDetails viewPropertyDetails, PropertyImpl propertyImpl) throws ParseException {
        propertyImpl.getPropertyDetail().setCorrAddressDiff(viewPropertyDetails.getIsCorrAddrDiff());
        if (!viewPropertyDetails.getFloorDetailsEntered()) {
            // vacant Land
            propertyImpl.getPropertyDetail().setEffectiveDate(convertStringToDate(viewPropertyDetails.getEffectiveDate()));
        } else {
            // private Land without appurtenant
            propertyImpl.getPropertyDetail()
                    .setEffectiveDate(convertStringToDate(viewPropertyDetails.getFloorDetails().get(0).getOccupancyDate()));
        }
        if (StringUtils.isNotBlank(viewPropertyDetails.getApartmentCmplx())) {
            final Apartment apartment = apartmentService.getApartmentByCode(viewPropertyDetails.getApartmentCmplx());
            propertyImpl.getPropertyDetail().setApartment(apartment);
        }

        propertyImpl.getPropertyDetail().setOccupancyCertificationNo(viewPropertyDetails.getOccupancyCertificationNo());
        propertyImpl.getPropertyDetail().setOccupancyCertificationDate(viewPropertyDetails.getOccupancyCertificationDate());
        if (StringUtils.isNotBlank(viewPropertyDetails.getDocType())
                && PropertyTaxConstants.DOCUMENT_NAME_NOTARY_DOCUMENT.equals(viewPropertyDetails.getDocType()))
            propertyImpl.getPropertyDetail().setStructure(true);
    }

    private Date getCompletionDate(PropertyService propService, PropertyImpl propertyImpl) {
        Date propCompletionDate;
        if (!propertyImpl.getPropertyDetail().getPropertyTypeMaster().getCode().equalsIgnoreCase(OWNERSHIP_TYPE_VAC_LAND))
            propCompletionDate = propService
                    .getLowestDtOfCompFloorWise(propertyImpl.getPropertyDetail().getFloorDetailsProxy());
        else
            propCompletionDate = propertyImpl.getPropertyDetail().getDateOfCompletion();

        return propCompletionDate;
    }

    private void setVacantLandDetails(ViewPropertyDetails viewPropertyDetails, PropertyImpl propertyImpl) throws ParseException {
        propertyImpl.getPropertyDetail()
                .setDateOfCompletion(viewPropertyDetails.getEffectiveDate() != null
                        ? convertStringToDate(viewPropertyDetails.getEffectiveDate()) : null);
        propertyImpl.getPropertyDetail().setCurrentCapitalValue(viewPropertyDetails.getCurrentCapitalValue());
        propertyImpl.getPropertyDetail().setSurveyNumber(viewPropertyDetails.getSurveyNumber());
        propertyImpl.getPropertyDetail().setPattaNumber(viewPropertyDetails.getPattaNumber());
        propertyImpl.getPropertyDetail()
                .setLayoutPermitNo(viewPropertyDetails.getLpNo() != null ? viewPropertyDetails.getLpNo() : null);
        propertyImpl.getPropertyDetail().setLayoutPermitDate(viewPropertyDetails.getLpDate() != null
                ? convertStringToDate(viewPropertyDetails.getLpDate()) : null);
        final Area area = new Area();
        area.setArea(viewPropertyDetails.getVacantLandArea());
        propertyImpl.getPropertyDetail().setSitalArea(area);
        propertyImpl.getPropertyDetail().setMarketValue(viewPropertyDetails.getMarketValue());
    }

    private void setAmenities(ViewPropertyDetails viewPropertyDetails, PropertyImpl propertyImpl) {
        propertyImpl.getPropertyDetail().setLift(viewPropertyDetails.getHasLift());
        propertyImpl.getPropertyDetail().setToilets(viewPropertyDetails.getHasToilet());
        propertyImpl.getPropertyDetail().setWaterTap(viewPropertyDetails.getHasWaterTap());
        propertyImpl.getPropertyDetail().setElectricity(viewPropertyDetails.getHasElectricity());
        propertyImpl.getPropertyDetail().setAttachedBathRoom(viewPropertyDetails.getHasAttachedBathroom());
        propertyImpl.getPropertyDetail().setWaterHarvesting(viewPropertyDetails.getHasWaterHarvesting());
        propertyImpl.getPropertyDetail().setCable(viewPropertyDetails.getHasCableConnection());
    }

    private PropertyAddress createPropAddress(ViewPropertyDetails viewPropertyDetails, final Boundary block) {
        final Address propAddr = new PropertyAddress();
        propAddr.setHouseNoBldgApt(viewPropertyDetails.getDoorNo());
        propAddr.setAreaLocalitySector(getBoundaryByNumberAndType(viewPropertyDetails.getLocalityName(),
                LOCALITY_BNDRY_TYPE, LOCATION_HIERARCHY_TYPE).getName());
        String cityName = ApplicationThreadLocals.getCityName();
        propAddr.setStreetRoadLine(block.getParent().getName());
        propAddr.setCityTownVillage(cityName);

        if (StringUtils.isNotBlank(viewPropertyDetails.getPinCode()))
            propAddr.setPinCode(viewPropertyDetails.getPinCode());
        return (PropertyAddress) propAddr;
    }

    private Address createCorrespondenceAddress(ViewPropertyDetails viewPropertyDetails, PropertyAddress propAddr) {
        final Address ownerCorrAddr = new CorrespondenceAddress();
        if (viewPropertyDetails.getIsCorrAddrDiff()) {
            ownerCorrAddr.setAreaLocalitySector(viewPropertyDetails.getCorrAddr1());
            ownerCorrAddr.setStreetRoadLine(viewPropertyDetails.getCorrAddr2());
            ownerCorrAddr.setPinCode(viewPropertyDetails.getCorrPinCode());
        } else {
            ownerCorrAddr.setAreaLocalitySector(propAddr.getAreaLocalitySector());
            ownerCorrAddr.setHouseNoBldgApt(propAddr.getHouseNoBldgApt());
            ownerCorrAddr.setStreetRoadLine(propAddr.getStreetRoadLine());
            ownerCorrAddr.setPinCode(propAddr.getPinCode());
        }
        return ownerCorrAddr;
    }

    private PropertyID createPropertID(final BasicProperty basicProperty, ViewPropertyDetails viewPropertyDetails,
            final Boundary block) {

        final PropertyID propertyID = new PropertyID();
        final Boundary ward = getBoundaryByNumberAndType(viewPropertyDetails.getWardName(), WARD, REVENUE_HIERARCHY_TYPE);
        final Boundary zone = getBoundaryByNumberAndType(viewPropertyDetails.getZoneName(), ZONE, REVENUE_HIERARCHY_TYPE);
        final Boundary locality = getBoundaryByNumberAndType(viewPropertyDetails.getLocalityName(), LOCALITY_BNDRY_TYPE,
                LOCATION_HIERARCHY_TYPE);
        propertyID.setArea(block);
        propertyID.setWard(ward);
        propertyID.setZone(zone);
        propertyID.setLocality(locality);
        propertyID.setBasicProperty(basicProperty);
        final Boundary electionBoundary = getBoundaryByNumberAndType(viewPropertyDetails.getElectionWardName(), WARD,
                ADMIN_HIERARCHY_TYPE);
        if (electionBoundary != null) {
            propertyID.setElectionBoundary(electionBoundary);
            basicProperty.setBoundary(electionBoundary);
        }
        if (StringUtils.isNotBlank(viewPropertyDetails.getNorthBoundary()))
            propertyID.setNorthBoundary(viewPropertyDetails.getNorthBoundary());
        if (StringUtils.isNotBlank(viewPropertyDetails.getSouthBoundary()))
            propertyID.setSouthBoundary(viewPropertyDetails.getSouthBoundary());
        if (StringUtils.isNotBlank(viewPropertyDetails.getEastBoundary()))
            propertyID.setEastBoundary(viewPropertyDetails.getEastBoundary());
        if (StringUtils.isNotBlank(viewPropertyDetails.getWestBoundary()))
            propertyID.setWestBoundary(viewPropertyDetails.getWestBoundary());
        propertyID.setBasicProperty(basicProperty);
        return propertyID;
    }

    public void saveDocumentTypeDetails(BasicProperty basicProperty, ViewPropertyDetails viewPropertyDetails)
            throws ParseException {
        DocumentTypeDetails documentTypeDetails = new DocumentTypeDetails();
        documentTypeDetails.setBasicPropertyId(basicProperty.getId());
        documentTypeDetails.setDocumentName(viewPropertyDetails.getDocType());
        documentTypeDetails.setDocumentNo(viewPropertyDetails.getRegdDocNo());
        documentTypeDetails.setDocumentDate(StringUtils.isNotBlank(viewPropertyDetails.getRegdDocDate())
                ? convertStringToDate(viewPropertyDetails.getRegdDocDate()) : null);
        documentTypeDetails.setCourtName(viewPropertyDetails.getCourtName());
        documentTypeDetails.setProceedingNo(viewPropertyDetails.getMroProcNo());
        documentTypeDetails.setProceedingDate(StringUtils.isNotBlank(viewPropertyDetails.getMroProcDate())
                ? convertStringToDate(viewPropertyDetails.getMroProcDate()) : null);
        documentTypeDetails.setSigned(viewPropertyDetails.getTwSigned());
        documentTypeDetailsService.persist(documentTypeDetails);
    }

    /**
     * This method is used to validate the payment details to do the payments.
     *
     * @param assessmentNo - assessment number or property number
     * @param paymentMode - mode of payment
     * @param totalAmount - total amount
     * @param paidBy - name of the payer
     * @return
     */
    public ErrorDetails validatePaymentDetails(final String assessmentNo, final String paymentMode,
            final BigDecimal totalAmount, final String paidBy) {
        ErrorDetails errorDetails = null;
        if (assessmentNo == null || assessmentNo.trim().length() == 0) {
            errorDetails = new ErrorDetails();
            errorDetails.setErrorCode(THIRD_PARTY_ERR_CODE_ASSESSMENT_NO_REQUIRED);
            errorDetails.setErrorMessage(THIRD_PARTY_ERR_MSG_ASSESSMENT_NO_REQUIRED);
        } else {
            if (assessmentNo.trim().length() > 0 && assessmentNo.trim().length() < 10) {
                errorDetails = new ErrorDetails();
                errorDetails.setErrorCode(THIRD_PARTY_ERR_CODE_ASSESSMENT_NO_LEN);
                errorDetails.setErrorMessage(THIRD_PARTY_ERR_MSG_ASSESSMENT_NO_LEN);
            }
            if (!basicPropertyDAO.isAssessmentNoExist(assessmentNo)) {
                errorDetails = new ErrorDetails();
                errorDetails.setErrorCode(THIRD_PARTY_ERR_CODE_ASSESSMENT_NO_NOT_FOUND);
                errorDetails.setErrorMessage(THIRD_PARTY_ERR_MSG_ASSESSMENT_NO_NOT_FOUND);
            }
        }

        if (paymentMode == null || paymentMode.trim().length() == 0) {
            errorDetails = new ErrorDetails();
            errorDetails.setErrorCode(THIRD_PARTY_ERR_CODE_PAYMENT_MODE_REQUIRED);
            errorDetails.setErrorMessage(THIRD_PARTY_ERR_MSG_PAYMENT_MODE_REQUIRED);
        } else if (!THIRD_PARTY_PAYMENT_MODE_CASH.equalsIgnoreCase(paymentMode.trim())
                && !THIRD_PARTY_PAYMENT_MODE_CHEQUE.equalsIgnoreCase(paymentMode.trim())
                && !THIRD_PARTY_PAYMENT_MODE_DD.equalsIgnoreCase(paymentMode.trim())) {
            errorDetails = new ErrorDetails();
            errorDetails.setErrorCode(THIRD_PARTY_ERR_CODE_PAYMENT_MODE_INVALID);
            errorDetails.setErrorMessage(THIRD_PARTY_ERR_MSG_PAYMENT_MODE_INVALID);
        }
        return errorDetails;
    }

    private String formatDate(final Date date) {
        final SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
        return sdf.format(date);
    }

    public Date convertStringToDate(final String dateInString) throws ParseException {
        final SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
        return sdf.parse(dateInString);
    }

    private void processAndStoreDocumentsWithReason(final BasicProperty basicProperty, final String reason,
            final List<File> fileAttachments, final List<String> uploadFileNames,
            final List<String> uploadContentTypes) {
        if (!fileAttachments.isEmpty()) {
            int fileCount = 0;
            for (final File file : fileAttachments) {
                final FileStoreMapper fileStore = fileStoreService.store(file, uploadFileNames.get(fileCount),
                        uploadContentTypes.get(fileCount++), FILESTORE_MODULE_NAME);
                final PropertyDocs propertyDoc = new PropertyDocs();
                propertyDoc.setSupportDoc(fileStore);
                propertyDoc.setBasicProperty(basicProperty);
                propertyDoc.setReason(reason);
                basicProperty.addDocs(propertyDoc);
            }
        }
    }

    private PropertyImpl createPropertyWithBasicDetails(final String propertyType) {
        final PropertyImpl propertyImpl = new PropertyImpl();
        if (propertyType.equalsIgnoreCase(OWNERSHIP_TYPE_VAC_LAND))
            propertyImpl.setPropertyDetail(new VacantProperty());
        else
            propertyImpl.setPropertyDetail(new BuiltUpProperty());
        propertyImpl.setBasicProperty(new BasicPropertyImpl());
        return propertyImpl;
    }

    private List<Floor> getFloorList(final List<FloorDetails> floorDetailsList) throws ParseException {
        final List<Floor> floorList = new ArrayList<>(0);
        for (final FloorDetails floorDetails : floorDetailsList) {
            final Floor floor = new Floor();
            if (StringUtils.isNotBlank(floorDetails.getFloorNoCode()))
                floor.setFloorNo(Integer.valueOf(floorDetails.getFloorNoCode()));
            if (StringUtils.isNotBlank(floorDetails.getBuildClassificationCode()))
                floor.setStructureClassification(structureClassificationService
                        .getClassificationByCode(floorDetails.getBuildClassificationCode()));
            if (StringUtils.isNotBlank(floorDetails.getNatureOfUsageCode()))
                floor.setPropertyUsage(propertyUsageService.getUsageByCode(floorDetails.getNatureOfUsageCode()));
            if (StringUtils.isNotBlank(floorDetails.getOccupancyCode()))
                floor.setPropertyOccupation(getPropertyOccupationByOccupancyCode(floorDetails.getOccupancyCode()));
            if (StringUtils.isNotBlank(floorDetails.getFirmName()))
                floor.setFirmName(floorDetails.getFirmName());
            if (StringUtils.isNotBlank(floorDetails.getOccupantName()))
                floor.setOccupantName(floorDetails.getOccupantName());
            if (StringUtils.isNotBlank(floorDetails.getConstructionDate()))
                floor.setConstructionDate(convertStringToDate(floorDetails.getConstructionDate()));
            if (StringUtils.isNotBlank(floorDetails.getOccupancyDate()))
                floor.setOccupancyDate(convertStringToDate(floorDetails.getOccupancyDate()));
            floor.setCreatedDate(new Date());
            final Area builtUpArea = new Area();
            builtUpArea.setBreadth(floorDetails.getPlinthBreadth());
            builtUpArea.setLength(floorDetails.getPlinthLength());
            floor.setBuiltUpArea(builtUpArea);
            floor.setUnstructuredLand(floorDetails.getUnstructuredLand());
            if (!floor.getUnstructuredLand()) {
                builtUpArea.setArea((float) (Math.round(builtUpArea.getBreadth() * builtUpArea.getLength() * 100.0) / 100.0));
            } else {
                builtUpArea.setArea(floorDetails.getPlinthArea());
            }
            if (StringUtils.isNotBlank(floorDetails.getBuildingPermissionNo()))
                floor.setBuildingPermissionNo(floorDetails.getBuildingPermissionNo());
            if (StringUtils.isNotBlank(floorDetails.getBuildingPermissionDate()))
                floor.setBuildingPermissionDate(convertStringToDate(floorDetails.getBuildingPermissionDate()));
            final Area buildingPlanPlinthArea = new Area();
            buildingPlanPlinthArea.setArea(floorDetails.getBuildingPlanPlinthArea());
            floor.setBuildingPlanPlinthArea(buildingPlanPlinthArea);

            floorList.add(floor);
        }
        return floorList;
    }

    private List<PropertyOwnerInfo> getPropertyOwnerInfoList(final List<OwnerInformation> ownerInfoList) {
        final List<PropertyOwnerInfo> proeprtyOwnerInfoList = new ArrayList<>(0);
        for (final OwnerInformation ownerInfo : ownerInfoList) {
            final PropertyOwnerInfo propOwner = new PropertyOwnerInfo();
            final User owner = new User();
            owner.setAadhaarNumber(
                    StringUtils.isNotBlank(ownerInfo.getAadhaarNo()) ? ownerInfo.getAadhaarNo() : StringUtils.EMPTY);
            owner.setSalutation(StringUtils.isNotBlank(ownerInfo.getSalutationCode()) ? ownerInfo.getSalutationCode()
                    : StringUtils.EMPTY);
            owner.setName(StringUtils.isNotBlank(ownerInfo.getName()) ? ownerInfo.getName() : StringUtils.EMPTY);
            owner.setGender(Gender.valueOf(ownerInfo.getGender()));
            owner.setMobileNumber(StringUtils.isNotBlank(ownerInfo.getMobileNumber()) ? ownerInfo.getMobileNumber()
                    : StringUtils.EMPTY);
            owner.setEmailId(
                    StringUtils.isNotBlank(ownerInfo.getEmailId()) ? ownerInfo.getEmailId() : StringUtils.EMPTY);
            owner.setGuardianRelation(StringUtils.isNotBlank(ownerInfo.getGuardianRelation())
                    ? ownerInfo.getGuardianRelation() : StringUtils.EMPTY);
            owner.setGuardian(
                    StringUtils.isNotBlank(ownerInfo.getGuardian()) ? ownerInfo.getGuardian() : StringUtils.EMPTY);

            propOwner.setOwner(owner);
            proeprtyOwnerInfoList.add(propOwner);
        }
        return proeprtyOwnerInfoList;
    }

    public BillReceiptInfo validateTransanctionIdPresent(final String transantion, String propertyType) {
        if (propertyType.equals(OWNERSHIP_TYPE_VAC_LAND))
            return collectionService.getReceiptInfo(SERVICE_CODE_VACANTLANDTAX, transantion);
        else
            return collectionService.getReceiptInfo(PTIS_COLLECTION_SERVICE_CODE, transantion);
    }

    private PropertyImpl transitionWorkFlow(PropertyImpl property, PropertyService propService, String mode) {
        final DateTime currentDate = new DateTime();
        final User user = userService.getUserById(ApplicationThreadLocals.getUserId());
        final String approverComments = FORWARD_SUCCESS_COMMENT;
        String currentState;
        String additionalRule;
        String natureOftask;
        if (mode.equals(PROPERTY_MODE_CREATE)) {
            currentState = "Created";
            additionalRule = NEW_ASSESSMENT;
            natureOftask = NATURE_NEW_ASSESSMENT;
        } else {
            currentState = "Created";
            additionalRule = ADDTIONAL_RULE_ALTER_ASSESSMENT;
            natureOftask = NATURE_ALTERATION;
        }
        final Assignment assignment = getAssignment(property, propService);
        Position pos = assignment.getPosition();
        final WorkFlowMatrix wfmatrix = propertyWorkflowService.getWfMatrix(property.getStateType(), null, null,
                additionalRule, currentState, null);
        property.transition().start().withSenderName(user.getUsername() + "::" + user.getName())
                .withComments(approverComments).withStateValue(wfmatrix.getNextState())
                .withDateInfo(currentDate.toDate()).withOwner(pos).withNextAction(wfmatrix.getNextAction())
                .withNatureOfTask(natureOftask).withInitiator(assignment.getPosition());

        return property;
    }

    public Assignment getAssignment(PropertyImpl property, PropertyService propService) {
        return propService.getMappedAssignmentForCscOperator(property.getBasicProperty());
    }

    @SuppressWarnings("unchecked")
    public List<MasterCodeNamePairDetails> getDocumentTypes() {
        final List<MasterCodeNamePairDetails> mstrCodeNamePairDetailsList = new ArrayList<>(0);
        final List<DocumentType> documentTypesList = entityManager.createQuery("from DocumentType order by id")
                .getResultList();
        for (final DocumentType documentType : documentTypesList) {
            final MasterCodeNamePairDetails mstrCodeNamePairDetails = new MasterCodeNamePairDetails();
            mstrCodeNamePairDetails.setCode(documentType.getId().toString());
            mstrCodeNamePairDetails.setName(documentType.getName());
            mstrCodeNamePairDetailsList.add(mstrCodeNamePairDetails);
        }
        return mstrCodeNamePairDetailsList;
    }

    /**
     * Fetches Assessment Details - owner details, tax dues, plinth area, mutation fee related information
     * 
     * @param assessmentNo
     * @return
     */
    public RestAssessmentDetails fetchAssessmentDetails(final String assessmentNo, BigDecimal marketValue, BigDecimal regValue,
            final HttpServletRequest request) {
        PropertyImpl property;
        RestAssessmentDetails assessmentDetails = new RestAssessmentDetails();
        BasicProperty basicProperty = basicPropertyDAO.getAllBasicPropertyByPropertyID(assessmentNo);
        if (basicProperty != null) {
            assessmentDetails.setAssessmentNo(basicProperty.getUpicNo());
            assessmentDetails.setPropertyAddress(basicProperty.getAddress().toString());
            property = (PropertyImpl) basicProperty.getProperty();
            assessmentDetails.setLocalityName(basicProperty.getPropertyID().getLocality().getName());
            if (property != null) {
                assessmentDetails.setOwnerDetails(prepareOwnerInfo(property));
                if (property.getPropertyDetail().getTotalBuiltupArea() != null
                        && property.getPropertyDetail().getTotalBuiltupArea().getArea() != null)
                    assessmentDetails.setPlinthArea(property.getPropertyDetail().getTotalBuiltupArea().getArea());
            }
            assessmentDetails.setRevenueWard(basicProperty.getPropertyID().getWard().getName());
            assessmentDetails.setDoorNo(
                    basicProperty.getAddress().getHouseNoBldgApt() == null ? "N/A"
                            : basicProperty.getAddress().getHouseNoBldgApt());
            AssessmentDetails assmtDetails = getDuesForProperty(request, assessmentNo, "");
            assessmentDetails.setTotalPropTaxDue(assmtDetails.getPropertyDue());
            assessmentDetails.setWaterChargesDue(assmtDetails.getWaterTaxDue());
            assessmentDetails.setSwerageDue(assmtDetails.getSewerageDue());
            assessmentDetails.setTotalTaxDue(assessmentDetails.getTotalPropTaxDue()
                    .add(assessmentDetails.getWaterChargesDue().add(assessmentDetails.getSwerageDue())));
            assessmentDetails.setMarketValue(marketValue == null ? BigDecimal.ZERO : marketValue);
            assessmentDetails.setRegistrationValue(regValue == null ? BigDecimal.ZERO : regValue);
            assessmentDetails.setMutationFee(propertyTransferService.calculateMutationFee(assessmentDetails.getMarketValue(),
                    assessmentDetails.getRegistrationValue()));
        }
        getMutationDetails(assessmentNo, assessmentDetails);

        return assessmentDetails;
    }

    private void getMutationDetails(String assessmentNo, RestAssessmentDetails assessmentDetails) {
        PropertyMutation propertyMutation = getLatestPropertyMutationByAssesmentNo(assessmentNo);
        if (propertyMutation != null) {
            if (StringUtils.isNotBlank(propertyMutation.getReceiptNum())) {
                assessmentDetails.setIsMutationFeePaid("Y");
                assessmentDetails.setFeeReceipt(propertyMutation.getReceiptNum());
                assessmentDetails.setFeeReceiptAmount(propertyMutation.getMutationFee());
                final Query qry = entityManager
                        .createQuery("select receiptdate from ReceiptHeader where receiptnumber = :receiptNum");
                qry.setParameter("receiptNum", propertyMutation.getReceiptNum());
                assessmentDetails.setFeeReceiptDate(DateUtils.getDefaultFormattedDate((Date) qry.getSingleResult()));
            }

            assessmentDetails.setApplicationNo(propertyMutation.getApplicationNo());
        } else {
            assessmentDetails.setIsMutationFeePaid("N");
            assessmentDetails.setFeeReceipt("");
            assessmentDetails.setFeeReceiptDate("");
            assessmentDetails.setApplicationNo("");
        }
    }

    /**
     * Fetches Assessment Details - owner details, tax dues, plinth area, mutation fee related information - used in MeeSeva
     * 
     * @param applicationNo
     * @return RestAssessmentDetails
     */
    public RestAssessmentDetails loadAssessmentDetails(final String applicationNo) {
        // FIXME move this method to meeseva itself
        RestAssessmentDetails assessmentDetails = new RestAssessmentDetails();
        PropertyMutation propertyMutation = getPropertyMutationByAssesmentNoAndApplicationNo(null, applicationNo);
        BasicProperty basicProperty;
        PropertyImpl property;
        if (propertyMutation != null) {
            basicProperty = propertyMutation.getBasicProperty();
            if (basicProperty != null) {
                assessmentDetails.setAssessmentNo(basicProperty.getUpicNo());
                assessmentDetails.setPropertyAddress(basicProperty.getAddress().toString());
                property = (PropertyImpl) basicProperty.getProperty();
                assessmentDetails.setLocalityName(basicProperty.getPropertyID().getLocality().getName());
                if (property != null) {
                    assessmentDetails.setOwnerDetails(prepareOwnerInfo(property));
                    if (property.getPropertyDetail().getTotalBuiltupArea() != null
                            && property.getPropertyDetail().getTotalBuiltupArea().getArea() != null)
                        assessmentDetails.setPlinthArea(property.getPropertyDetail().getTotalBuiltupArea().getArea());
                    Ptdemand currentPtdemand = ptDemandDAO.getNonHistoryCurrDmdForProperty(property);
                    BigDecimal totalTaxDue = BigDecimal.ZERO;
                    if (currentPtdemand != null) {
                        for (EgDemandDetails demandDetails : currentPtdemand.getEgDemandDetails()) {
                            if (demandDetails.getAmount().compareTo(demandDetails.getAmtCollected()) > 0) {
                                totalTaxDue = totalTaxDue
                                        .add(demandDetails.getAmount().subtract(demandDetails.getAmtCollected()));
                            }
                        }
                    }
                    assessmentDetails.setTotalTaxDue(totalTaxDue);
                }
            }
            if (StringUtils.isNotBlank(propertyMutation.getReceiptNum())) {
                assessmentDetails.setFeeReceipt(propertyMutation.getReceiptNum());
                assessmentDetails.setIsMutationFeePaid("Y");
                assessmentDetails.setMutationFee(BigDecimal.ZERO);
            } else {
                assessmentDetails.setIsMutationFeePaid("N");
                assessmentDetails.setMutationFee(propertyMutation.getMutationFee());
            }

            assessmentDetails.setApplicationNo(propertyMutation.getApplicationNo());
        } else {
            assessmentDetails.setIsMutationFeePaid("N");
        }
        return assessmentDetails;
    }

    /**
     * API for Mutation Fee Payment
     * 
     * @param payPropertyTaxDetails
     * @return ReceiptDetails
     */
    public ReceiptDetails payMutationFee(final PayPropertyTaxDetails payPropertyTaxDetails) {
        ReceiptDetails receiptDetails = null;
        ErrorDetails errorDetails;
        final BasicProperty basicProperty = basicPropertyDAO
                .getBasicPropertyByPropertyID(payPropertyTaxDetails.getAssessmentNo());
        PropertyMutation propertyMutation = getLatestPropertyMutationByAssesmentNo(
                payPropertyTaxDetails.getAssessmentNo());
        propertyTaxBillable.setBasicProperty(basicProperty);
        propertyTaxBillable.setTransanctionReferenceNumber(payPropertyTaxDetails.getTransactionId());
        propertyTaxBillable.setMutationFeePayment(Boolean.TRUE);
        propertyTaxBillable.setMutationFee(payPropertyTaxDetails.getPaymentAmount());
        propertyTaxBillable.setCallbackForApportion(Boolean.FALSE);
        if (propertyMutation != null)
            propertyTaxBillable.setMutationApplicationNo(propertyMutation.getApplicationNo());
        propertyTaxBillable
                .setReferenceNumber(propertyTaxNumberGenerator.generateManualBillNumber(basicProperty.getPropertyID()));

        final EgBill egBill = ptBillServiceImpl.generateBill(propertyTaxBillable);
        final CollectionHelper collectionHelper = new CollectionHelper(egBill);
        final Map<String, String> paymentDetailsMap = new HashMap<>();
        paymentDetailsMap.put(TOTAL_AMOUNT, payPropertyTaxDetails.getPaymentAmount().toString());
        paymentDetailsMap.put(PAID_BY, egBill.getCitizenName());
        if (THIRD_PARTY_PAYMENT_MODE_CHEQUE.equalsIgnoreCase(payPropertyTaxDetails.getPaymentMode().toLowerCase())
                || THIRD_PARTY_PAYMENT_MODE_DD.equalsIgnoreCase(payPropertyTaxDetails.getPaymentMode().toLowerCase())) {
            paymentDetailsMap.put(ChequePayment.INSTRUMENTNUMBER, payPropertyTaxDetails.getChqddNo());
            paymentDetailsMap.put(ChequePayment.INSTRUMENTDATE,
                    ChequePayment.CHEQUE_DATE_FORMAT.format(payPropertyTaxDetails.getChqddDate()));
            paymentDetailsMap.put(ChequePayment.BRANCHNAME, payPropertyTaxDetails.getBranchName());
            final Long validatesBankId = validateBank(payPropertyTaxDetails.getBankName());
            paymentDetailsMap.put(ChequePayment.BANKID, validatesBankId.toString());
        }
        final Payment payment = Payment.create(payPropertyTaxDetails.getPaymentMode().toLowerCase(), paymentDetailsMap);
        collectionHelper.setIsMutationFeePayment(true);
        final BillReceiptInfo billReceiptInfo = collectionHelper.executeCollection(payment,
                payPropertyTaxDetails.getSource());

        if (null != billReceiptInfo) {
            receiptDetails = new ReceiptDetails();
            receiptDetails.setReceiptNo(billReceiptInfo.getReceiptNum());
            receiptDetails.setReceiptDate(formatDate(billReceiptInfo.getReceiptDate()));
            receiptDetails.setPayeeName(billReceiptInfo.getPayeeName());
            receiptDetails.setPayeeAddress(billReceiptInfo.getPayeeAddress());
            receiptDetails.setBillReferenceNo(billReceiptInfo.getBillReferenceNum());
            receiptDetails.setServiceName(billReceiptInfo.getServiceName());
            receiptDetails.setDescription(billReceiptInfo.getDescription());
            receiptDetails.setPaidBy(billReceiptInfo.getPaidBy());
            receiptDetails.setPaymentAmount(billReceiptInfo.getTotalAmount());
            receiptDetails.setPaymentMode(payPropertyTaxDetails.getPaymentMode());
            receiptDetails.setTransactionId(billReceiptInfo.getManualReceiptNumber());
            errorDetails = new ErrorDetails();
            errorDetails.setErrorCode(THIRD_PARTY_ERR_CODE_SUCCESS);
            errorDetails.setErrorMessage(THIRD_PARTY_ERR_MSG_SUCCESS);

            receiptDetails.setErrorDetails(errorDetails);
        }
        return receiptDetails;
    }

    /**
     * Validate the payment amount entered for mutation
     * 
     * @param assessmentNo
     * @param paymentAmount
     * @return boolean
     */
    public boolean validateMutationFee(String assessmentNo, BigDecimal paymentAmount) {
        boolean validFee = true;
        PropertyMutation propertyMutation = getLatestPropertyMutationByAssesmentNo(assessmentNo);
        if (propertyMutation != null) {
            if (paymentAmount.compareTo(propertyMutation.getMutationFee()) > 0) {
                validFee = false;
            }
        } else {
            validFee = false;
        }
        return validFee;
    }

    /**
     * Fetch PropertyMutation for given assessmentNo and applicationNo
     * 
     * @param assessmentNo
     * @param applicationNo
     * @return PropertyMutation
     */
    public PropertyMutation getPropertyMutationByAssesmentNoAndApplicationNo(String assessmentNo,
            String applicationNo) {
        return propertyMutationDAO
                .getPropertyMutationForAssessmentNoAndApplicationNumber(assessmentNo, applicationNo);
    }

    /**
     * Fetch PropertyMutation for given assessmentNo
     * 
     * @param assessmentNo
     * @return PropertyMutation
     */
    public PropertyMutation getLatestPropertyMutationByAssesmentNo(String assessmentNo) {
        return propertyMutationDAO.getPropertyLatestMutationForAssessmentNo(assessmentNo);
    }

    /**
     * API provides List of ward-block-locality mapping for Revenue Wards
     * 
     * @return List
     */
    public List<Object[]> getWardBlockLocalityMapping() {
        StringBuilder queryString = new StringBuilder();
        queryString.append(
                "select parent.parent.boundaryNum as wardnum, parent.parent.name as wardname, parent.boundaryNum as blocknum,");
        queryString.append(" parent.name as blockname, child.boundaryNum as localitynum, child.name as localityname");
        queryString.append(" from CrossHierarchy ch, Boundary parent, Boundary child");
        queryString.append(
                " where ch.parent.id = parent.id  and ch.child.id = child.id  and ch.parentType.name=:block and");
        queryString.append(
                " ch.childType.name=:locality and parent.parent.boundaryType.hierarchyType.name=:hierarchyType");
        List<Object[]> boundaryDetails = entityManager.unwrap(Session.class).createQuery(queryString.toString())
                .setParameter("block", BLOCK).setParameter("locality", LOCALITY_BNDRY_TYPE)
                .setParameter("hierarchyType", REVENUE_HIERARCHY_TYPE).list();
        return boundaryDetails;
    }

    /**
     * API provides ward-wise property details
     * 
     * @param ulbCode
     * @param wardNum
     * @return List
     */

    @ReadOnly
    public List<AssessmentInfo> getPropertyDetailsForWard(String ulbCode, String wardNum, String assessmentNo,
            String doorNo, String oldAssessmentNo) {
        Long wardId = null;
        if (StringUtils.isNotBlank(wardNum)) {
            Boundary ward = getBoundaryByNumberAndType(wardNum, WARD, REVENUE_HIERARCHY_TYPE);
            wardId = ward.getId();
        }

        List<AssessmentInfo> propertyDetails = new ArrayList<>();
        List<BasicProperty> basicProperties = basicPropertyDAO.getActiveBasicPropertiesForWard(wardId, assessmentNo,
                doorNo, oldAssessmentNo);
        if (!basicProperties.isEmpty()) {
            AssessmentInfo assessmentInfo;
            for (BasicProperty basicProperty : basicProperties) {
                assessmentInfo = new AssessmentInfo();
                assessmentInfo.setUlbCode(ulbCode);
                prepareProperyDetailsInfo(basicProperty, assessmentInfo);
                propertyDetails.add(assessmentInfo);
            }
        }
        return propertyDetails;
    }

    /**
     * API to set each property details
     * 
     * @param basicProperty
     * @param viewPropertyDetails
     */
    private void prepareProperyDetailsInfo(BasicProperty basicProperty, AssessmentInfo assessmentInfo) {
        Property property = basicProperty.getProperty();
        assessmentInfo.setOldAssessmentNumber(basicProperty.getOldMuncipalNum());
        assessmentInfo.setAssessmentNumber(basicProperty.getUpicNo());
        assessmentInfo.setCategory(basicProperty.getProperty().getPropertyDetail().getPropertyTypeMaster() != null
                ? basicProperty.getProperty().getPropertyDetail().getPropertyTypeMaster().getType() : "");
        PropertyID propertyID = basicProperty.getPropertyID();
        if (property != null) {
            PropertyDetail propertyDetail = property.getPropertyDetail();
            assessmentInfo.setExemption(property.getTaxExemptedReason() == null ? "N" : "Y");
            populatePropertyDetails(basicProperty, assessmentInfo, propertyID, propertyDetail);
        }

        populateOwnerAndAddressDetails(basicProperty, assessmentInfo, propertyID);
    }

    /**
     * API to populate owner and address details
     * 
     * @param basicProperty
     * @param viewPropertyDetails
     * @param ownerAddress
     * @param propertyID
     */
    public void populateOwnerAndAddressDetails(BasicProperty basicProperty, AssessmentInfo assessmentInfo,
            PropertyID propertyID) {
        if (!basicProperty.getPropertyOwnerInfo().isEmpty()) {
            for (PropertyOwnerInfo propOwner : basicProperty.getPropertyOwnerInfo()) {
                List<Address> addrSet = propOwner.getOwner().getAddress();
                for (final Address address : addrSet) {
                    assessmentInfo.setDoorNo(
                            address.getHouseNoBldgApt() == null ? NOT_AVAILABLE : address.getHouseNoBldgApt());
                    break;
                }
            }
            assessmentInfo.setOwnerDetails(getOwnerDetails(basicProperty));
        }
        getAddressDetails(basicProperty, assessmentInfo, propertyID);
    }

    private void getAddressDetails(BasicProperty basicProperty, AssessmentInfo assessmentInfo, PropertyID propertyID) {
        if (basicProperty.getAddress() != null)
            assessmentInfo.setPropertyAddress(basicProperty.getAddress().toString());
        if (propertyID.getZone() != null && StringUtils.isNotBlank(propertyID.getZone().getName()))
            assessmentInfo.setRevZone(propertyID.getZone().getName());
        if (propertyID.getWard() != null && StringUtils.isNotBlank(propertyID.getWard().getName()))
            assessmentInfo.setRevWard(propertyID.getWard().getName());
        if (propertyID.getArea() != null && StringUtils.isNotBlank(propertyID.getArea().getName()))
            assessmentInfo.setRevBlock(propertyID.getArea().getName());
        if (propertyID.getLocality() != null && StringUtils.isNotBlank(propertyID.getLocality().getName()))
            assessmentInfo.setLocalityName(propertyID.getLocality().getName());
        if (propertyID.getElectionBoundary() != null
                && StringUtils.isNotBlank(propertyID.getElectionBoundary().getName()))
            assessmentInfo.setElectionWardName(propertyID.getElectionBoundary().getName());
    }

    /**
     * API to set property level details
     * 
     * @param basicProperty
     * @param viewPropertyDetails
     * @param propertyID
     * @param propertyDetail
     */
    public void populatePropertyDetails(BasicProperty basicProperty, AssessmentInfo assessmentInfo,
            PropertyID propertyID, PropertyDetail propertyDetail) {
        assessmentInfo.setEffectiveDate(DateUtils.getDefaultFormattedDate(basicProperty.getPropOccupationDate()));
        assessmentInfo
                .setPropertyType(PropertyTaxConstants.PROPERTY_TYPE_CATEGORIES.get(propertyDetail.getCategoryType()));
        assessmentInfo.setApartmentCmplx(
                propertyDetail.getApartment() == null ? NOT_AVAILABLE : propertyDetail.getApartment().getName());
        assessmentInfo.setExtentOfSite(propertyDetail.getSitalArea() == null ? NOT_AVAILABLE
                : propertyDetail.getSitalArea().getArea().toString());
        assessmentInfo.setRegdDocNo(basicProperty.getRegdDocNo());
        if (basicProperty.getRegdDocDate() != null)
            assessmentInfo.setRegdDocDate(DateUtils.getDefaultFormattedDate(basicProperty.getRegdDocDate()));

        if (!propertyDetail.getPropertyTypeMaster().getCode().equalsIgnoreCase(OWNERSHIP_TYPE_VAC_LAND)) {
            getAmenitiesAndConstructionTypeDetails(assessmentInfo, propertyDetail);
            assessmentInfo.setFloorInfo(getFloorDetails(propertyDetail));
        } else
            getVacantLandDetails(assessmentInfo, propertyDetail, propertyID);

        final Query query = entityManager.createNamedQuery("DOCUMENT_TYPE_DETAILS_BY_ID");
        query.setParameter("basicProperty", propertyID.getBasicProperty().getId());
        List<DocumentTypeDetails> docTypeDetailsList = query.getResultList();
        if (!docTypeDetailsList.isEmpty()) {
            DocumentTypeDetails docTypeDetails = docTypeDetailsList.get(0);
            if (docTypeDetails != null) {
                assessmentInfo.setDocType(docTypeDetails.getDocumentName());
                assessmentInfo.setMroProcNo(docTypeDetails.getProceedingNo());
                if (docTypeDetails.getProceedingDate() != null)
                    assessmentInfo
                            .setMroProcDate(DateUtils.getDefaultFormattedDate(docTypeDetails.getProceedingDate()));
                assessmentInfo.setCourtName(docTypeDetails.getCourtName());
                assessmentInfo.setTwSigned(docTypeDetails.isSigned());
            }
        }
    }

    /**
     * API to set the construction details of the property
     * 
     * @param viewPropertyDetails
     * @param propertyDetail
     */
    private void getAmenitiesAndConstructionTypeDetails(AssessmentInfo assessmentInfo, PropertyDetail propertyDetail) {
        assessmentInfo.setHasLift(propertyDetail.isLift());
        assessmentInfo.setHasToilet(propertyDetail.isToilets());
        assessmentInfo.setHasWaterTap(propertyDetail.isWaterTap());
        assessmentInfo.setHasElectricity(propertyDetail.isElectricity());
        assessmentInfo.setHasAttachedBathroom(propertyDetail.isAttachedBathRoom());
        assessmentInfo.setHasWaterHarvesting(propertyDetail.isWaterHarvesting());
        assessmentInfo.setHasCableConnection(propertyDetail.isCable());
        if (propertyDetail.getFloorType() != null)
            assessmentInfo.setFloorType(propertyDetail.getFloorType().getName());
        if (propertyDetail.getRoofType() != null)
            assessmentInfo.setRoofType(propertyDetail.getRoofType().getName());
        assessmentInfo.setWallType(
                propertyDetail.getWallType() == null ? NOT_AVAILABLE : propertyDetail.getWallType().getName());
        assessmentInfo.setWoodType(
                propertyDetail.getWoodType() == null ? NOT_AVAILABLE : propertyDetail.getWoodType().getName());
    }

    /**
     * API to set owner details
     * 
     * @param basicProperty
     * @return List
     */
    private List<OwnerInformation> getOwnerDetails(BasicProperty basicProperty) {
        List<OwnerInformation> ownerDetails = new ArrayList<>();
        OwnerInformation ownerInfo;
        User owner;
        for (PropertyOwnerInfo propOwnerInfo : basicProperty.getPropertyOwnerInfo()) {
            ownerInfo = new OwnerInformation();
            owner = propOwnerInfo.getOwner();
            ownerInfo.setAadhaarNo(owner.getAadhaarNumber());
            ownerInfo.setMobileNumber(owner.getMobileNumber());
            ownerInfo.setName(owner.getName());
            ownerInfo.setGender(owner.getGender().name());
            ownerInfo.setEmailId(owner.getEmailId());
            ownerInfo.setGuardianRelation(owner.getGuardianRelation());
            ownerInfo.setGuardian(owner.getGuardian());

            ownerDetails.add(ownerInfo);
        }
        return ownerDetails;
    }

    /**
     * API to set floor details
     * 
     * @param propertyDetail
     * @return List
     */
    private List<FloorInfo> getFloorDetails(PropertyDetail propertyDetail) {
        List<FloorInfo> floorDetails = new ArrayList<>();
        FloorInfo floorInfo;
        for (Floor floor : propertyDetail.getFloorDetails()) {
            floorInfo = new FloorInfo();
            if (floor.getFloorNo() != null)
                floorInfo.setFloorNo(FLOOR_MAP.get(floor.getFloorNo()));
            if (floor.getStructureClassification() != null
                    && StringUtils.isNotBlank(floor.getStructureClassification().getDescription()))
                floorInfo.setBuildClassification(floor.getStructureClassification().getDescription());
            if (floor.getPropertyUsage() != null && StringUtils.isNotBlank(floor.getPropertyUsage().getUsageName()))
                floorInfo.setNatureOfUsage(floor.getPropertyUsage().getUsageName());
            floorInfo.setFirmName(StringUtils.isBlank(floor.getFirmName()) ? NOT_AVAILABLE : floor.getFirmName());
            floorInfo.setOccupancy(floor.getPropertyOccupation().getOccupation());
            floorInfo.setOccupantName(
                    StringUtils.isBlank(floor.getOccupantName()) ? NOT_AVAILABLE : floor.getOccupantName());
            floorInfo.setConstructionDate(floor.getConstructionDate() == null ? NOT_AVAILABLE
                    : DateUtils.getDefaultFormattedDate(floor.getConstructionDate()));
            if (floor.getOccupancyDate() != null)
                floorInfo.setOccupancyDate(DateUtils.getDefaultFormattedDate(floor.getOccupancyDate()));
            if (floor.getBuiltUpArea() != null && floor.getBuiltUpArea().getLength() != null)
                floorInfo.setPlinthLength(floor.getBuiltUpArea().getLength());
            if (floor.getBuiltUpArea() != null && floor.getBuiltUpArea().getBreadth() != null)
                floorInfo.setPlinthBreadth(floor.getBuiltUpArea().getBreadth());
            if (floor.getBuiltUpArea() != null && floor.getBuiltUpArea().getArea() != null)
                floorInfo.setPlinthArea(floor.getBuiltUpArea().getArea());
            if (StringUtils.isNotBlank(floor.getBuildingPermissionNo()))
                floorInfo.setBuildingPermissionNo(floor.getBuildingPermissionNo());
            floorInfo.setBuildingPermissionDate(floor.getBuildingPermissionDate() == null ? NOT_AVAILABLE
                    : DateUtils.getDefaultFormattedDate(floor.getBuildingPermissionDate()));
            floorInfo.setBuildingPlanPlinthArea(
                    floor.getBuildingPlanPlinthArea() == null ? 0.0F : floor.getBuildingPlanPlinthArea().getArea());

            floorDetails.add(floorInfo);
        }
        return floorDetails;
    }

    /**
     * API to set Vacant land details
     * 
     * @param viewPropertyDetails
     * @param propertyDetail
     * @param propertyID
     */
    private void getVacantLandDetails(AssessmentInfo assessmentInfo, PropertyDetail propertyDetail,
            PropertyID propertyID) {
        if (StringUtils.isNotBlank(propertyDetail.getSurveyNumber()))
            assessmentInfo.setSurveyNumber(propertyDetail.getSurveyNumber());
        if (StringUtils.isNotBlank(propertyDetail.getPattaNumber()))
            assessmentInfo.setPattaNumber(propertyDetail.getPattaNumber());
        if (propertyDetail.getSitalArea() != null && propertyDetail.getSitalArea().getArea() != null)
            assessmentInfo.setVacantLandArea(propertyDetail.getSitalArea().getArea());
        if (propertyDetail.getMarketValue() != null)
            assessmentInfo.setMarketValue(propertyDetail.getMarketValue());
        if (propertyDetail.getCurrentCapitalValue() != null)
            assessmentInfo.setCurrentCapitalValue(propertyDetail.getCurrentCapitalValue());
        if (propertyDetail.getDateOfCompletion() != null)
            assessmentInfo.setEffectiveDate(DateUtils.getDefaultFormattedDate(propertyDetail.getDateOfCompletion()));
        if (propertyDetail.getVacantLandPlotArea() != null
                && StringUtils.isNotBlank(propertyDetail.getVacantLandPlotArea().getName()))
            assessmentInfo.setVlPlotArea(propertyDetail.getVacantLandPlotArea().getName());
        if (propertyDetail.getLayoutApprovalAuthority() != null
                && StringUtils.isNotBlank(propertyDetail.getLayoutApprovalAuthority().getName()))
            assessmentInfo.setLaAuthority(propertyDetail.getLayoutApprovalAuthority().getName());
        assessmentInfo.setLpNo(propertyDetail.getLayoutPermitNo());
        if (propertyDetail.getLayoutPermitDate() != null)
            assessmentInfo.setLpDate(DateUtils.getDefaultFormattedDate(propertyDetail.getLayoutPermitDate()));
        getBoundariesForVacantLand(assessmentInfo, propertyID);
    }

    private void getBoundariesForVacantLand(AssessmentInfo assessmentInfo, PropertyID propertyID) {
        if (StringUtils.isNotBlank(propertyID.getNorthBoundary()))
            assessmentInfo.setNorthBoundary(propertyID.getNorthBoundary());
        if (StringUtils.isNotBlank(propertyID.getNorthBoundary()))
            assessmentInfo.setEastBoundary(propertyID.getEastBoundary());
        if (StringUtils.isNotBlank(propertyID.getWestBoundary()))
            assessmentInfo.setWestBoundary(propertyID.getWestBoundary());
        if (StringUtils.isNotBlank(propertyID.getSouthBoundary()))
            assessmentInfo.setSouthBoundary(propertyID.getSouthBoundary());
    }

    /**
     * Gives the count of properties for the given input criteria
     * 
     * @param transactionType
     * @param fromDate
     * @param toDate
     * @return
     * @throws ParseException
     */

    @ReadOnly
    public Long getPropertiesCount(String transactionType, String fromDate, String toDate) throws ParseException {
        StringBuilder queryString = new StringBuilder();
        queryString.append(
                "select count(distinct prop.basicProperty.id) from PropertyImpl prop, PropertyMaterlizeView pmv ");
        queryString.append(
                " where prop.basicProperty.id = pmv.basicPropertyID and (cast(prop.createdDate as date)) between :fromDate and :toDate ");
        queryString.append(" and upper(prop.propertyModifyReason) like :modifyReason and prop.status in ('A','I') ");
        if (transactionType.equalsIgnoreCase(PropertyTaxConstants.TRANSACTION_TYPE_CREATE))
            queryString.append(" and prop.demolitionReason is null ");
        else if (transactionType.equalsIgnoreCase(PropertyTaxConstants.TRANSACTION_TYPE_DEMOLITION))
            queryString.append(" and prop.demolitionReason is not null ");
        final Query qry = entityManager.createQuery(queryString.toString());
        qry.setParameter("fromDate", convertStringToDate(fromDate));
        qry.setParameter("toDate", convertStringToDate(toDate));
        qry.setParameter("modifyReason", "%".concat(transactionType.toUpperCase()).concat("%"));
        return (Long) qry.getResultList().get(0);
    }

    /**
     * Gives details of the properties for the selected input criteria
     * 
     * @param transactionType
     * @param fromDate
     * @param toDate
     * @return
     * @throws ParseException
     */

    @ReadOnly
    public List<SurveyAssessmentDetails> getPropertyDetailsForSurvey(String transactionType, String fromDate,
            String toDate) throws ParseException {
        StringBuilder queryString = new StringBuilder();
        List<SurveyAssessmentDetails> assessmentDetailsList = new ArrayList<>();
        queryString.append(
                "select prop, pmv.houseNo, pmv.propertyAddress, pmv.sitalArea, (coalesce(pmv.arrearDemand,0)+coalesce(pmv.aggrArrearPenaly,0)+coalesce(pmv.aggrCurrFirstHalfPenaly,0)+");
        queryString.append(
                "coalesce(pmv.aggrCurrSecondHalfPenaly,0)+coalesce(pmv.aggrCurrFirstHalfDmd,0)+coalesce(pmv.aggrCurrSecondHalfDmd,0)) from PropertyImpl prop, PropertyMaterlizeView pmv ");
        queryString.append(
                " where prop.basicProperty.id = pmv.basicPropertyID and (cast(prop.createdDate as date)) between :fromDate and :toDate ");
        queryString.append(" and upper(prop.propertyModifyReason) like :modifyReason and prop.status in ('A','I') ");
        if (transactionType.equalsIgnoreCase(PropertyTaxConstants.TRANSACTION_TYPE_CREATE))
            queryString.append(" and prop.demolitionReason is null ");
        else if (transactionType.equalsIgnoreCase(PropertyTaxConstants.TRANSACTION_TYPE_DEMOLITION))
            queryString.append(" and prop.demolitionReason is not null ");
        final Query qry = entityManager.createQuery(queryString.toString());
        qry.setParameter("fromDate", convertStringToDate(fromDate));
        qry.setParameter("toDate", convertStringToDate(toDate));
        qry.setParameter("modifyReason", "%".concat(transactionType.toUpperCase()).concat("%"));

        List<Object[]> propertyDetails = qry.getResultList();
        if (!propertyDetails.isEmpty()) {
            SurveyAssessmentDetails assessmentDetails;
            for (Object[] objArr : propertyDetails) {
                assessmentDetails = new SurveyAssessmentDetails();
                preparePropertyDetails(objArr, assessmentDetails);
                assessmentDetailsList.add(assessmentDetails);
            }
        }
        return assessmentDetailsList;
    }

    private void preparePropertyDetails(Object[] obj, SurveyAssessmentDetails assessmentDetails) {
        Property property = (Property) obj[0];
        BasicProperty basicProperty = property.getBasicProperty();
        if (StringUtils.isNotBlank(basicProperty.getUpicNo()))
            assessmentDetails.setAssessmentNo(basicProperty.getUpicNo());
        assessmentDetails.setDoorNo(obj[1].toString());
        assessmentDetails.setPropertyAddress(obj[2].toString());
        if (property.getPropertyDetail().getPropertyTypeMaster() != null
                && StringUtils.isNotBlank(property.getPropertyDetail().getPropertyTypeMaster().getType()))
            assessmentDetails.setPropertyType(property.getPropertyDetail().getPropertyTypeMaster().getType());
        assessmentDetails.setPropertyCategory(
                PropertyTaxConstants.PROPERTY_TYPE_CATEGORIES.get(property.getPropertyDetail().getCategoryType()));
        assessmentDetails.setAssessmentYear(DateUtils.toYearFormat(basicProperty.getPropOccupationDate()));
        assessmentDetails.setTotalTax(new BigDecimal(obj[4].toString()));
        assessmentDetails.setTotalSitalArea(new BigDecimal(obj[3].toString()));
        assessmentDetails.setOwnerDetails(getOwnerDetails(basicProperty));
    }

    /**
     * API to update property - used in Mobile App
     * 
     * @param viewPropertyDetails
     * @return NewPropertyDetails
     * @throws ParseException
     */
    @Transactional
    public NewPropertyDetails updateProperty(ViewPropertyDetails viewPropertyDetails) throws ParseException {
        NewPropertyDetails newPropertyDetails = null;
        BigDecimal activeTax = BigDecimal.ZERO;
        final PropertyService propService = beanProvider.getBean(PROP_SERVICE, PropertyService.class);
        BasicProperty basicProperty = updateBasicProperty(viewPropertyDetails, propService);
        PropertyImpl property = (PropertyImpl) basicProperty.getWFProperty();
        PropertyImpl activeProperty = basicProperty.getActiveProperty();
        property.getPropertyDetail().setCategoryType(viewPropertyDetails.getCategory());
        basicProperty.setUnderWorkflow(Boolean.TRUE);
        basicProperty.setParcelId(viewPropertyDetails.getParcelId());
        basicProperty.setLatitude(viewPropertyDetails.getLatitude());
        basicProperty.setLongitude(viewPropertyDetails.getLongitude());
        property.setReferenceId(viewPropertyDetails.getReferenceId());

        /*
         * Duplicate GIS property will be persisted, which will be used for generating comparison reports
         */
        PropertyImpl gisProperty = (PropertyImpl) property.createPropertyclone();
        if (!gisProperty.getPropertyDetail().getFloorDetails().isEmpty()) {
            for (Floor floor : gisProperty.getPropertyDetail().getFloorDetails()) {
                floor.setPropertyDetail(gisProperty.getPropertyDetail());
                basicPropertyService.applyAuditing(floor);
            }
        }
        gisProperty.setStatus('G');
        gisProperty.setSource(SOURCE_SURVEY);
        Ptdemand ptdemand = property.getPtDemandSet().iterator().next();
        Ptdemand gisPtdemand = gisProperty.getPtDemandSet().iterator().next();
        if (gisPtdemand != null)
            gisPtdemand.getDmdCalculations().setAlv(ptdemand.getDmdCalculations().getAlv());
        basicProperty.addProperty(gisProperty);

        basicPropertyService.applyAuditing(gisPtdemand.getDmdCalculations());
        transitionWorkFlow(property, propService, PROPERTY_MODE_MODIFY);
        basicPropertyService.applyAuditing(property.getState());
        if (basicProperty.getWFProperty() != null && basicProperty.getWFProperty().getPtDemandSet() != null
                && !basicProperty.getWFProperty().getPtDemandSet().isEmpty()) {
            for (Ptdemand ptDemand : basicProperty.getWFProperty().getPtDemandSet()) {
                basicPropertyService.applyAuditing(ptDemand.getDmdCalculations());
            }
        }
        basicProperty = basicPropertyService.update(basicProperty);
        propService.updateIndexes(property, PropertyTaxConstants.APPLICATION_TYPE_ALTER_ASSESSENT);
        if (SOURCE_SURVEY.equalsIgnoreCase(property.getSource())) {
            SurveyBean surveyBean = new SurveyBean();
            surveyBean.setProperty(property);
            BigDecimal totalTax = propService.getSurveyTax(gisProperty, gisPtdemand.getEgInstallmentMaster().getFromDate());
            surveyBean.setGisTax(totalTax);
            surveyBean.setApplicationTax(totalTax);
            surveyBean.setAgeOfCompletion(propService.getSlaValue(APPLICATION_TYPE_ALTER_ASSESSENT));
            if (activeProperty != null) {
                Ptdemand activePtDemand = ptDemandDAO.getNonHistoryCurrDmdForProperty(activeProperty);
                Map<String, Installment> yearwiseInstMap = propertyTaxUtil
                        .getInstallmentsForCurrYear(gisPtdemand.getEgInstallmentMaster().getFromDate());
                Date firstInstStartDate = yearwiseInstMap.get(PropertyTaxConstants.CURRENTYEAR_FIRST_HALF).getFromDate();
                Date secondInstStartDate = yearwiseInstMap.get(PropertyTaxConstants.CURRENTYEAR_SECOND_HALF).getFromDate();
                for (EgDemandDetails demandDetail : activePtDemand.getEgDemandDetails()) {
                    if (firstInstStartDate.equals(demandDetail.getInstallmentStartDate())
                            || secondInstStartDate.equals(demandDetail.getInstallmentStartDate())
                                    && !PropertyTaxConstants.DEMANDRSN_CODE_PENALTY_FINES.equalsIgnoreCase(
                                            demandDetail.getEgDemandReason().getEgDemandReasonMaster().getCode())
                                    && !PropertyTaxConstants.DEMANDRSN_CODE_CHQ_BOUNCE_PENALTY.equalsIgnoreCase(
                                            demandDetail.getEgDemandReason().getEgDemandReasonMaster().getCode()))
                        activeTax = activeTax.add(demandDetail.getAmount());
                }
                surveyBean.setSystemTax(activeTax);
            }
            surveyService.updateSurveyIndex(APPLICATION_TYPE_ALTER_ASSESSENT, surveyBean);
        }
        if (basicProperty != null) {
            newPropertyDetails = new NewPropertyDetails();
            newPropertyDetails.setApplicationNo(basicProperty.getWFProperty().getApplicationNo());
            final ErrorDetails errorDetails = new ErrorDetails();
            errorDetails.setErrorCode(THIRD_PARTY_ERR_CODE_SUCCESS);
            errorDetails.setErrorMessage(THIRD_PARTY_ERR_MSG_SUCCESS);
            newPropertyDetails.setErrorDetails(errorDetails);
        }
        return newPropertyDetails;
    }

    /**
     * Updates the BasicProperty based on the input
     * 
     * @param viewPropertyDetails
     * @param propService
     * @return
     * @throws ParseException
     */
    private BasicProperty updateBasicProperty(ViewPropertyDetails viewPropertyDetails, PropertyService propService)
            throws ParseException {
        BasicProperty basicProperty = basicPropertyDAO
                .getBasicPropertyByPropertyID(viewPropertyDetails.getAssessmentNumber());
        PropertyImpl oldProperty = (PropertyImpl) basicProperty.getProperty();
        PropertyImpl propertyImpl = createPropertyWithBasicDetails(viewPropertyDetails.getPropertyTypeMaster());
        Date propCompletionDate;
        final PropertyTypeMaster propertyTypeMaster = getPropertyTypeMasterByCode(
                viewPropertyDetails.getPropertyTypeMaster());
        propertyImpl.getPropertyDetail().setEffectiveDate(convertStringToDate(viewPropertyDetails.getEffectiveDate()));
        if (StringUtils.isNotBlank(viewPropertyDetails.getApartmentCmplx())) {
            final Apartment apartment = apartmentService.getApartmentByCode(viewPropertyDetails.getApartmentCmplx());
            propertyImpl.getPropertyDetail().setApartment(apartment);
        }
        propertyImpl.setSource(SOURCE_SURVEY);
        propertyImpl.getPropertyDetail().setOccupancyCertificationNo(viewPropertyDetails.getOccupancyCertificationNo());
        propertyImpl.getPropertyDetail().setOccupancyCertificationDate(viewPropertyDetails.getOccupancyCertificationDate());
        final PropertyMutationMaster propMutMstr = propertyMutationMasterDAO
                .getPropertyMutationMasterByCode(PROPERTY_MODIFY_REASON_ADD_OR_ALTER);
        basicProperty.setPropertyMutationMaster(propMutMstr);

        if (!propertyTypeMaster.getCode().equalsIgnoreCase(OWNERSHIP_TYPE_VAC_LAND)) {
            FloorType floorType;
            RoofType roofType;
            WallType wallType;
            WoodType woodType;
            PropertyDetail propDetail = basicProperty.getActiveProperty().getPropertyDetail();
            if (StringUtils.isNotBlank(viewPropertyDetails.getFloorType()))
                floorType = floorTypeService.getFloorTypeById(Long.valueOf(viewPropertyDetails.getFloorType()));
            else
                floorType = propDetail.getFloorType();
            if (StringUtils.isNotBlank(viewPropertyDetails.getRoofType()))
                roofType = roofTypeService.getRoofTypeById(Long.valueOf(viewPropertyDetails.getRoofType()));
            else
                roofType = propDetail.getRoofType();
            if (StringUtils.isNotBlank(viewPropertyDetails.getWallType()))
                wallType = wallTypeService.getWallTypeById(Long.valueOf(viewPropertyDetails.getWallType()));
            else
                wallType = propDetail.getWallType();
            if (StringUtils.isNotBlank(viewPropertyDetails.getWoodType()))
                woodType = woodTypeService.getWoodTypeById(Long.valueOf(viewPropertyDetails.getWoodType()));
            else
                woodType = propDetail.getWoodType();

            propertyImpl.getPropertyDetail().setFloorDetailsProxy(getFloorList(viewPropertyDetails.getFloorDetails()));
            propertyImpl.getPropertyDetail().setLift(viewPropertyDetails.getHasLift());
            propertyImpl.getPropertyDetail().setToilets(viewPropertyDetails.getHasToilet());
            propertyImpl.getPropertyDetail().setWaterTap(viewPropertyDetails.getHasWaterTap());
            propertyImpl.getPropertyDetail().setElectricity(viewPropertyDetails.getHasElectricity());
            propertyImpl.getPropertyDetail().setAttachedBathRoom(viewPropertyDetails.getHasAttachedBathroom());
            propertyImpl.getPropertyDetail().setWaterHarvesting(viewPropertyDetails.getHasWaterHarvesting());
            propertyImpl.getPropertyDetail().setCable(viewPropertyDetails.getHasCableConnection());

            String extentOfSite = null;
            if (StringUtils.isNotBlank(viewPropertyDetails.getExtentOfSite())) {
                propertyImpl.getPropertyDetail().setExtentSite(Double.valueOf(viewPropertyDetails.getExtentOfSite()));
                extentOfSite = viewPropertyDetails.getExtentOfSite();
            } else if (!viewPropertyDetails.getPropertyTypeMaster().equalsIgnoreCase(OWNERSHIP_TYPE_VAC_LAND)) {
                extentOfSite = propDetail.getExtentSite() != null ? propDetail.getExtentSite().toString()
                        : propDetail.getSitalArea().getArea().toString();
            }

            propertyImpl = propService.createProperty(propertyImpl, extentOfSite, propMutMstr.getCode(),
                    propertyTypeMaster.getId().toString(), null, null, STATUS_WORKFLOW, propertyImpl.getDocNumber(),
                    null, floorType != null ? floorType.getId() : null, roofType != null ? roofType.getId() : null,
                    wallType != null ? wallType.getId() : null, woodType != null ? woodType.getId() : null, null, null,
                    null, null, Boolean.FALSE);
        } else {
            propertyImpl.getPropertyDetail()
                    .setDateOfCompletion(convertStringToDate(viewPropertyDetails.getEffectiveDate()));
            propertyImpl.getPropertyDetail().setCurrentCapitalValue(viewPropertyDetails.getCurrentCapitalValue());
            propertyImpl.getPropertyDetail().setSurveyNumber(viewPropertyDetails.getSurveyNumber());
            propertyImpl.getPropertyDetail().setPattaNumber(viewPropertyDetails.getPattaNumber());
            propertyImpl.getPropertyDetail()
                    .setLayoutPermitNo(viewPropertyDetails.getLpNo() != null ? viewPropertyDetails.getLpNo() : null);
            propertyImpl.getPropertyDetail().setLayoutPermitDate(viewPropertyDetails.getLpDate() != null
                    ? convertStringToDate(viewPropertyDetails.getLpDate()) : null);
            final Area area = new Area();
            if (viewPropertyDetails.getVacantLandArea() != null)
                area.setArea(viewPropertyDetails.getVacantLandArea());
            propertyImpl.getPropertyDetail().setSitalArea(area);
            if (viewPropertyDetails.getMarketValue() != null)
                propertyImpl.getPropertyDetail().setMarketValue(viewPropertyDetails.getMarketValue());

            propertyImpl = propService.createProperty(propertyImpl,
                    String.valueOf(viewPropertyDetails.getVacantLandArea()), propMutMstr.getCode(),
                    propertyTypeMaster.getId().toString(), null, null, STATUS_WORKFLOW, propertyImpl.getDocNumber(),
                    null, null, null, null, null, null, null, viewPropertyDetails.getVlPlotArea(),
                    viewPropertyDetails.getLaAuthority(), Boolean.FALSE);

            if (StringUtils.isNotBlank(viewPropertyDetails.getNorthBoundary()))
                basicProperty.getPropertyID().setNorthBoundary(viewPropertyDetails.getNorthBoundary());
            if (StringUtils.isNotBlank(viewPropertyDetails.getSouthBoundary()))
                basicProperty.getPropertyID().setSouthBoundary(viewPropertyDetails.getSouthBoundary());
            if (StringUtils.isNotBlank(viewPropertyDetails.getEastBoundary()))
                basicProperty.getPropertyID().setEastBoundary(viewPropertyDetails.getEastBoundary());
            if (StringUtils.isNotBlank(viewPropertyDetails.getWestBoundary()))
                basicProperty.getPropertyID().setWestBoundary(viewPropertyDetails.getWestBoundary());
        }
        if (!propertyTypeMaster.getCode().equalsIgnoreCase(OWNERSHIP_TYPE_VAC_LAND))
            propCompletionDate = propService
                    .getLowestDtOfCompFloorWise(propertyImpl.getPropertyDetail().getFloorDetailsProxy());
        else
            propCompletionDate = propertyImpl.getPropertyDetail().getDateOfCompletion();
        basicProperty.setPropOccupationDate(propCompletionDate);

        propertyImpl.setPropertyModifyReason(PROPERTY_MODIFY_REASON_ADD_OR_ALTER);
        propertyImpl.setBasicProperty(basicProperty);
        propertyImpl.setEffectiveDate(propCompletionDate);
        final Long oldPropTypeId = oldProperty.getPropertyDetail().getPropertyTypeMaster().getId();
        final PropertyTypeMaster vacantPropTypeMstr = propertyTypeMasterDAO
                .getPropertyTypeMasterByCode(OWNERSHIP_TYPE_VAC_LAND);

        // if modifying from OPEN_PLOT to OTHERS property type
        if ((oldPropTypeId == vacantPropTypeMstr.getId() && propertyTypeMaster.getId() != vacantPropTypeMstr.getId()
                || oldPropTypeId != vacantPropTypeMstr.getId()
                        && propertyTypeMaster.getId() == vacantPropTypeMstr.getId())
                && !propertyImpl.getStatus().equals(STATUS_WORKFLOW))
            if (vacantPropTypeMstr != null && vacantPropTypeMstr.getId() == propertyTypeMaster.getId())
                changePropertyDetail(propertyImpl, new VacantProperty(), 0);
            else
                changePropertyDetail(propertyImpl, new BuiltUpProperty(), propertyImpl.getPropertyDetail().getNoofFloors());

        Property modProperty = null;
        try {
            modProperty = propService.modifyDemand(propertyImpl, oldProperty);

        } catch (TaxCalculatorExeption e) {

        }

        if (modProperty != null && !modProperty.getDocuments().isEmpty())
            propService.processAndStoreDocument(modProperty.getDocuments());

        if (modProperty == null)
            basicProperty.addProperty(propertyImpl);
        else
            basicProperty.addProperty(modProperty);

        return basicProperty;
    }

    /**
     * Changes the property details to BuiltUpProperty or VacantProperty
     * 
     * @param modProperty - the property which is getting modified
     * @param propDetail - PropertyDetail type, either BuiltUpProperty or VacantProperty
     * @param numOfFloors - the no. of floors which is depending on PropertyDetail
     */
    private void changePropertyDetail(final Property modProperty, final PropertyDetail propDetail,
            final Integer numOfFloors) {

        final PropertyDetail propertyDetail = modProperty.getPropertyDetail();

        propDetail.setSitalArea(propertyDetail.getSitalArea());
        propDetail.setTotalBuiltupArea(propertyDetail.getTotalBuiltupArea());
        propDetail.setCommBuiltUpArea(propertyDetail.getCommBuiltUpArea());
        propDetail.setPlinthArea(propertyDetail.getPlinthArea());
        propDetail.setCommVacantLand(propertyDetail.getCommVacantLand());
        propDetail.setSurveyNumber(propertyDetail.getSurveyNumber());
        propDetail.setFieldVerified(propertyDetail.getFieldVerified());
        propDetail.setFieldVerificationDate(propertyDetail.getFieldVerificationDate());
        propDetail.setFloorDetails(propertyDetail.getFloorDetails());
        propDetail.setPropertyDetailsID(propertyDetail.getPropertyDetailsID());
        propDetail.setWaterMeterNum(propertyDetail.getWaterMeterNum());
        propDetail.setElecMeterNum(propertyDetail.getElecMeterNum());
        propDetail.setNoofFloors(numOfFloors);
        propDetail.setFieldIrregular(propertyDetail.getFieldIrregular());
        propDetail.setDateOfCompletion(propertyDetail.getDateOfCompletion());
        propDetail.setProperty(propertyDetail.getProperty());
        propDetail.setUpdatedTime(propertyDetail.getUpdatedTime());
        propDetail.setPropertyTypeMaster(propertyDetail.getPropertyTypeMaster());
        propDetail.setPropertyType(propertyDetail.getPropertyType());
        propDetail.setInstallment(propertyDetail.getInstallment());
        propDetail.setPropertyOccupation(propertyDetail.getPropertyOccupation());
        propDetail.setPropertyMutationMaster(propertyDetail.getPropertyMutationMaster());
        propDetail.setComZone(propertyDetail.getComZone());
        propDetail.setCornerPlot(propertyDetail.getCornerPlot());
        propDetail.setCable(propertyDetail.isCable());
        propDetail.setAttachedBathRoom(propertyDetail.isAttachedBathRoom());
        propDetail.setElectricity(propertyDetail.isElectricity());
        propDetail.setWaterTap(propertyDetail.isWaterTap());
        propDetail.setWaterHarvesting(propertyDetail.isWaterHarvesting());
        propDetail.setLift(propertyDetail.isLift());
        propDetail.setToilets(propertyDetail.isToilets());
        propDetail.setFloorType(propertyDetail.getFloorType());
        propDetail.setRoofType(propertyDetail.getRoofType());
        propDetail.setWallType(propertyDetail.getWallType());
        propDetail.setWoodType(propertyDetail.getWoodType());
        propDetail.setExtentSite(propertyDetail.getExtentSite());
        propDetail.setExtentAppartenauntLand(propertyDetail.getExtentAppartenauntLand());
        if (numOfFloors == 0)
            propDetail.setPropertyUsage(propertyDetail.getPropertyUsage());
        else
            propDetail.setPropertyUsage(null);
        propDetail.setManualAlv(propertyDetail.getManualAlv());
        propDetail.setOccupierName(propertyDetail.getOccupierName());

        modProperty.setPropertyDetail(propDetail);

    }

    /**
     * Changes the property details from {@link BuiltUpProperty} to {@link VacantProperty}
     *
     * @return vacant property details
     * @see VacantProperty
     */

    private VacantProperty changePropertyDetail(final PropertyImpl property) {

        final PropertyDetail propertyDetail = property.getPropertyDetail();
        final VacantProperty vacantProperty = new VacantProperty(propertyDetail.getSitalArea(),
                propertyDetail.getTotalBuiltupArea(), propertyDetail.getCommBuiltUpArea(),
                propertyDetail.getPlinthArea(), propertyDetail.getCommVacantLand(), propertyDetail.getNonResPlotArea(),
                false, propertyDetail.getSurveyNumber(), propertyDetail.getFieldVerified(),
                propertyDetail.getFieldVerificationDate(), propertyDetail.getFloorDetails(),
                propertyDetail.getPropertyDetailsID(), propertyDetail.getWaterMeterNum(),
                propertyDetail.getElecMeterNum(), 0, propertyDetail.getFieldIrregular(),
                propertyDetail.getDateOfCompletion(), propertyDetail.getProperty(), propertyDetail.getUpdatedTime(),
                propertyDetail.getPropertyUsage(), null, propertyDetail.getPropertyTypeMaster(),
                propertyDetail.getPropertyType(), propertyDetail.getInstallment(),
                propertyDetail.getPropertyOccupation(), propertyDetail.getPropertyMutationMaster(),
                propertyDetail.getComZone(), propertyDetail.getCornerPlot(),
                propertyDetail.getExtentSite() != null ? propertyDetail.getExtentSite() : 0.0,
                propertyDetail.getExtentAppartenauntLand() != null ? propertyDetail.getExtentAppartenauntLand() : 0.0,
                propertyDetail.getFloorType(), propertyDetail.getRoofType(), propertyDetail.getWallType(),
                propertyDetail.getWoodType(), propertyDetail.isLift(), propertyDetail.isToilets(),
                propertyDetail.isWaterTap(), propertyDetail.isStructure(), propertyDetail.isElectricity(),
                propertyDetail.isAttachedBathRoom(), propertyDetail.isWaterHarvesting(), propertyDetail.isCable(),
                propertyDetail.getSiteOwner(), propertyDetail.getPattaNumber(), propertyDetail.getCurrentCapitalValue(),
                propertyDetail.getMarketValue(), propertyDetail.getCategoryType(),
                propertyDetail.getOccupancyCertificationNo(), propertyDetail.getOccupancyCertificationDate(),
                propertyDetail.isAppurtenantLandChecked(),
                propertyDetail.isCorrAddressDiff(), propertyDetail.getPropertyDepartment(),
                propertyDetail.getVacantLandPlotArea(), propertyDetail.getLayoutApprovalAuthority(),
                propertyDetail.getLayoutPermitNo(), propertyDetail.getLayoutPermitDate());
        vacantProperty.setManualAlv(propertyDetail.getManualAlv());
        vacantProperty.setOccupierName(propertyDetail.getOccupierName());

        return vacantProperty;
    }

    public String getPropertyType(String assessmentno) {
        String pType = "";
        final BasicProperty basicProperty = basicPropertyDAO.getBasicPropertyByPropertyID(assessmentno);
        PropertyTypeMaster ptypeMaster = basicProperty.getProperty().getPropertyDetail().getPropertyTypeMaster();
        if (ptypeMaster != null)
            pType = ptypeMaster.getCode();
        return pType;
    }

    public NewPropertyDetails createAppurTenantProperties(ViewPropertyDetails viewPropertyDetails) throws ParseException {
        NewPropertyDetails newPropertyDetails = null;
        final PropertyService propService = beanProvider.getBean(PROP_SERVICE, PropertyService.class);
        BasicProperty vacantBasicProperty = new BasicPropertyImpl();
        final BasicProperty nonVacantBasicProperty = createBasicProp(viewPropertyDetails, propService);
        updatePropertyStatusValuesRemarks(nonVacantBasicProperty);
        PropertyImpl nonVacantProperty = null;
        try {
            nonVacantProperty = createNonVacantProperty(nonVacantBasicProperty, viewPropertyDetails, propService);
            vacantBasicProperty = createBasicProp(viewPropertyDetails, propService);
            updatePropertyStatusValuesRefProperty(nonVacantBasicProperty, vacantBasicProperty);
            createVacantProperty(nonVacantProperty, vacantBasicProperty, viewPropertyDetails, propService);
        } catch (final TaxCalculatorExeption e) {
            LOGGER.error("Error while calculating Taxes", e.getMessage());
        }

        if (null != nonVacantBasicProperty) {
            String applicationNumber = nonVacantBasicProperty.getProperty().getApplicationNo().concat(", ")
                    .concat(vacantBasicProperty.getProperty().getApplicationNo());
            newPropertyDetails = new NewPropertyDetails();
            newPropertyDetails.setApplicationNo(applicationNumber);
            final ErrorDetails errorDetails = new ErrorDetails();
            errorDetails.setErrorCode(THIRD_PARTY_ERR_CODE_SUCCESS);
            errorDetails.setErrorMessage(THIRD_PARTY_ERR_MSG_SUCCESS);
            newPropertyDetails.setErrorDetails(errorDetails);
        }
        return newPropertyDetails;

    }

    private PropertyImpl createNonVacantProperty(final BasicProperty nonVacantBasicProperty,
            ViewPropertyDetails viewPropertyDetails, final PropertyService propService)
            throws TaxCalculatorExeption, ParseException {
        PropertyImpl nonVacProp = createAppurTenantProperty(nonVacantBasicProperty, Boolean.TRUE, viewPropertyDetails,
                propService);
        persistAndMessage(nonVacantBasicProperty);
        saveDocumentTypeDetails(nonVacantBasicProperty, viewPropertyDetails);
        return nonVacProp;
    }

    private void createVacantProperty(final PropertyImpl nonVacantProperty, final BasicProperty vacantBasicProperty,
            ViewPropertyDetails viewPropertyDetails, final PropertyService propService)
            throws TaxCalculatorExeption, ParseException {
        final PropertyImpl vacantProperty = createAppurTenantProperty(vacantBasicProperty, Boolean.FALSE, viewPropertyDetails,
                propService);
        vacantProperty.setPropertyDetail(changePropertyDetail(vacantProperty));
        vacantProperty.getPropertyDetail().setCategoryType(getCategoryByNonVacantPropertyType(nonVacantProperty));
        vacantProperty.getPropertyDetail().setAppurtenantLandChecked(null);
        vacantBasicProperty.getAddress().setHouseNoBldgApt(null);
        vacantProperty.getPropertyDetail().getFloorDetails().clear();
        persistAndMessage(vacantBasicProperty);
        saveDocumentTypeDetails(vacantBasicProperty, viewPropertyDetails);
    }

    private void updatePropertyStatusValuesRefProperty(final BasicProperty nonVacantBasicProperty,
            final BasicProperty vacantBasicProperty) {
        final PropertyStatusValues propStatusVal = updatePropertyStatusValuesRemarks(vacantBasicProperty);
        propStatusVal.setReferenceBasicProperty(nonVacantBasicProperty);
    }

    private PropertyStatusValues updatePropertyStatusValuesRemarks(final BasicProperty basicProperty) {
        final PropertyStatusValues propStatusVal = basicProperty.getPropertyStatusValuesSet().iterator().next();
        propStatusVal.setRemarks(APPURTENANT_PROPERTY);
        return propStatusVal;
    }

    private String getCategoryByNonVacantPropertyType(final PropertyImpl nonVacantProperty) {
        final String propertyType = nonVacantProperty.getPropertyDetail().getPropertyTypeMaster().getCode();
        return OWNERSHIP_TYPE_PRIVATE.equals(propertyType) || OWNERSHIP_TYPE_VAC_LAND.equals(propertyType)
                ? CATEGORY_VACANT_LAND
                : OWNERSHIP_TYPE_STATE_GOVT.equals(propertyType) ? CATEGORY_STATE_GOVT : CATEGORY_CENTRAL_GOVT;
    }

    private void persistAndMessage(final BasicProperty basicProperty) {
        basicPropertyService.persist(basicProperty);
    }

    private BasicProperty createBasicProp(ViewPropertyDetails viewPropertyDetails, final PropertyService propService) {

        final BasicProperty basicProperty = new BasicPropertyImpl();
        basicProperty.setActive(Boolean.TRUE);
        basicProperty.setSource(SOURCEOFDATA_SURVEY);
        // Creating Property Address object
        final Boundary block = getBoundaryByNumberAndType(viewPropertyDetails.getBlockName(), BLOCK, REVENUE_HIERARCHY_TYPE);
        final PropertyAddress propAddress = createPropAddress(viewPropertyDetails, block);
        final Address ownerCorrAddr = createCorrespondenceAddress(viewPropertyDetails, propAddress);
        basicProperty.setAddress(propAddress);

        // Creating PropertyID object based on basic property, localityCode and
        // boundary map direction
        final PropertyID propertyID = createPropertID(basicProperty, viewPropertyDetails, block);
        basicProperty.setPropertyID(propertyID);

        // Get PropertyStatus object to set the status of the property
        final PropertyStatus propertyStatus = propertyStatusHibernateDAO.getPropertyStatusByCode(PROPERTY_STATUS_WORKFLOW);
        basicProperty.setStatus(propertyStatus);
        basicProperty.setUnderWorkflow(Boolean.TRUE);
        basicProperty.setParcelId(viewPropertyDetails.getParcelId());
        basicProperty.setLatitude(viewPropertyDetails.getLatitude());
        basicProperty.setLongitude(viewPropertyDetails.getLongitude());

        // Set PropertyMutationMaster object
        final PropertyMutationMaster propertyMutationMaster = propertyMutationMasterDAO
                .getPropertyMutationMasterByCode(viewPropertyDetails.getMutationReason());
        basicProperty.setPropertyMutationMaster(propertyMutationMaster);
        // need to pass parent property index, in case of bifurcation
        if (propertyMutationMaster.getCode().equals(PROP_CREATE_RSN_BIFUR) || viewPropertyDetails.getIsExtentAppurtenantLand())
            basicProperty.addPropertyStatusValues(propService.createPropStatVal(basicProperty, PROP_CREATE_RSN, null,
                    null, null, null, viewPropertyDetails.getParentPropertyAssessmentNo() != null
                            ? viewPropertyDetails.getParentPropertyAssessmentNo() : null));
        // Set isBillCreated property value as false
        basicProperty.setIsBillCreated(STATUS_BILL_NOTCREATED);
        basicProperty.setBoundary(propertyID.getElectionBoundary());
        basicProperty.setPropertyOwnerInfoProxy(getPropertyOwnerInfoList(viewPropertyDetails.getOwnerDetails()));
        propty.setBasicProperty(basicProperty);

        if (!viewPropertyDetails.getIsExtentAppurtenantLand()) {
            basicPropertyService.createOwners(propty, basicProperty, ownerCorrAddr);
            propty.setBasicProperty(basicProperty);
        } else
            basicPropertyService.createOwnersForAppurTenant(propty, basicProperty, ownerCorrAddr);
        propty.setPropertyModifyReason(PROP_CREATE_RSN);
        return basicProperty;
    }

    private PropertyImpl createAppurTenantProperty(final BasicProperty basicProperty, final Boolean nonVacant,
            ViewPropertyDetails viewPropertyDetails, final PropertyService propService)
            throws TaxCalculatorExeption, ParseException {

        final PropertyTypeMaster propertyTypeMaster = getPropertyTypeMasterByCode(viewPropertyDetails.getPropertyTypeMaster());
        PropertyImpl propertyImpl = createPropertyWithBasicDetails(viewPropertyDetails.getPropertyTypeMaster());
        if (nonVacant) {
            // private Land without appurtenant
            propertyImpl.getPropertyDetail()
                    .setEffectiveDate(convertStringToDate(viewPropertyDetails.getFloorDetails().get(0).getOccupancyDate()));

            FloorType floorType = null;
            RoofType roofType = null;
            WallType wallType = null;
            WoodType woodType = null;
            if (StringUtils.isNotBlank(viewPropertyDetails.getFloorType()))
                floorType = floorTypeService.getFloorTypeById(Long.valueOf(viewPropertyDetails.getFloorType()));
            if (StringUtils.isNotBlank(viewPropertyDetails.getRoofType()))
                roofType = roofTypeService.getRoofTypeById(Long.valueOf(viewPropertyDetails.getRoofType()));
            if (StringUtils.isNotBlank(viewPropertyDetails.getWallType()))
                wallType = wallTypeService.getWallTypeById(Long.valueOf(viewPropertyDetails.getWallType()));
            if (StringUtils.isNotBlank(viewPropertyDetails.getWoodType()))
                woodType = woodTypeService.getWoodTypeById(Long.valueOf(viewPropertyDetails.getWoodType()));

            propertyImpl.getPropertyDetail().setFloorDetailsProxy(getFloorList(viewPropertyDetails.getFloorDetails()));
            propertyImpl.getPropertyDetail().setLift(viewPropertyDetails.getHasLift());
            propertyImpl.getPropertyDetail().setToilets(viewPropertyDetails.getHasToilet());
            propertyImpl.getPropertyDetail().setWaterTap(viewPropertyDetails.getHasWaterTap());
            propertyImpl.getPropertyDetail().setElectricity(viewPropertyDetails.getHasElectricity());
            propertyImpl.getPropertyDetail().setAttachedBathRoom(viewPropertyDetails.getHasAttachedBathroom());
            propertyImpl.getPropertyDetail().setWaterHarvesting(viewPropertyDetails.getHasWaterHarvesting());
            propertyImpl.getPropertyDetail().setCable(viewPropertyDetails.getHasCableConnection());

            propertyImpl = propService.createProperty(propertyImpl, viewPropertyDetails.getExtentOfSite(),
                    viewPropertyDetails.getMutationReason(),
                    propertyTypeMaster.getId().toString(), null, null, STATUS_ISACTIVE, null, null,
                    floorType != null ? floorType.getId() : null, roofType != null ? roofType.getId() : null,
                    wallType != null ? wallType.getId() : null, woodType != null ? woodType.getId() : null, null, null,
                    null, null, nonVacant);
        } else {
            // vacant Land
            propertyImpl.getPropertyDetail().setEffectiveDate(convertStringToDate(viewPropertyDetails.getEffectiveDate()));
            propertyImpl.getPropertyDetail()
                    .setDateOfCompletion(viewPropertyDetails.getEffectiveDate() != null
                            ? convertStringToDate(viewPropertyDetails.getEffectiveDate()) : null);
            propertyImpl.getPropertyDetail().setCurrentCapitalValue(viewPropertyDetails.getCurrentCapitalValue());
            propertyImpl.getPropertyDetail().setSurveyNumber(viewPropertyDetails.getSurveyNumber());
            propertyImpl.getPropertyDetail().setPattaNumber(viewPropertyDetails.getPattaNumber());
            propertyImpl.getPropertyDetail().setLayoutPermitNo(viewPropertyDetails.getLpNo());
            propertyImpl.getPropertyDetail().setLayoutPermitDate(viewPropertyDetails.getLpDate() != null
                    ? convertStringToDate(viewPropertyDetails.getLpDate()) : null);
            final Area area = new Area();
            area.setArea(viewPropertyDetails.getVacantLandArea());
            propertyImpl.getPropertyDetail().setSitalArea(area);
            propertyImpl.getPropertyDetail().setMarketValue(viewPropertyDetails.getMarketValue());
            propertyImpl = propService.createProperty(propertyImpl, viewPropertyDetails.getExtentOfSite(),
                    viewPropertyDetails.getMutationReason(),
                    propertyTypeMaster.getId().toString(), null, null, STATUS_ISACTIVE, viewPropertyDetails.getRegdDocNo(), null,
                    null,
                    null, null, null, null, null,
                    viewPropertyDetails.getVlPlotArea() != null ? viewPropertyDetails.getVlPlotArea() : null,
                    viewPropertyDetails.getVlPlotArea() != null ? viewPropertyDetails.getVlPlotArea() : null,
                    Boolean.FALSE);
        }
        propertyImpl.setBasicProperty(basicProperty);
        propertyImpl.setPropertyModifyReason(PROP_CREATE_RSN);
        propertyImpl.setReferenceId(viewPropertyDetails.getReferenceId());

        Date propCompletionDate;
        if (!propertyImpl.getPropertyDetail().getPropertyTypeMaster().getCode()
                .equalsIgnoreCase(OWNERSHIP_TYPE_VAC_LAND))
            propCompletionDate = propService
                    .getLowestDtOfCompFloorWise(propertyImpl.getPropertyDetail().getFloorDetailsProxy());
        else
            propCompletionDate = propertyImpl.getPropertyDetail().getDateOfCompletion();

        basicProperty.setPropOccupationDate(propCompletionDate);
        basicProperty.setPropertyOwnerInfoProxy(getPropertyOwnerInfoList(viewPropertyDetails.getOwnerDetails()));
        basicProperty.setPropOccupationDate(propCompletionDate);
        basicProperty.addProperty(propertyImpl);
        if (basicProperty.getSource() == PropertyTaxConstants.SOURCEOFDATA_APPLICATION) {
            if (!propertyImpl.getDocuments().isEmpty())
                propService.processAndStoreDocument(propertyImpl.getDocuments());
            propService.createDemand(propertyImpl, propCompletionDate);
        }
        basicProperty.setUnderWorkflow(Boolean.TRUE);
        basicProperty.setIsTaxXMLMigrated(STATUS_YES_XML_MIGRATION);
        transitionWorkFlow(propertyImpl, propService, PROPERTY_MODE_CREATE);
        basicPropertyService.applyAuditing(propertyImpl.getState());
        propService.updateIndexes(propertyImpl, APPLICATION_TYPE_NEW_ASSESSENT);
        propService.processAndStoreDocument(propertyImpl.getAssessmentDocuments());
        return propertyImpl;
    }

    public Property getPropertyByBasicPropertyID(BasicProperty basicpropertyId) {
        StringBuilder queryString = new StringBuilder();
        queryString.append("from PropertyImpl prop where prop.basicProperty =:basicpropertyId  and prop.status in('A','I') ");
        Property property;
        final Query qry = entityManager.createQuery(queryString.toString());
        qry.setParameter("basicpropertyId", basicpropertyId);
        property = (Property) qry.getSingleResult();
        return property;
    }

    public Boolean isBoundaryActive(final String boundaryNum, final String boundaryTypeName,
            final String hierarchyName) {
        Boolean isActive = Boolean.FALSE;
        final BoundaryType boundaryType = boundaryTypeService
                .getBoundaryTypeByNameAndHierarchyTypeName(boundaryTypeName, hierarchyName);
        final Boundary boundary = boundaryService.getBoundaryByTypeAndNo(boundaryType, Long.valueOf(boundaryNum));

        if (boundary != null && boundary.isActive()) {
            isActive = Boolean.TRUE;
        }
        return isActive;
    }

    @SuppressWarnings("unchecked")
    public Boolean isActiveUnitRateExists(FloorDetails floorDetails, String zoneNumber, String usageCode,
            String classificationCode) throws ParseException {
        Boolean isActive = Boolean.FALSE;
        Boundary zone = getBoundaryByNumberAndType(zoneNumber, ZONE, REVENUE_HIERARCHY_TYPE);
        PropertyUsage usage = propertyUsageService.getUsageByCode(usageCode);
        StructureClassification sc = structureClassificationService.getClassificationByCode(classificationCode);
        final List<Installment> taxInstallments = propertyTaxUtil
                .getInstallmentsListByEffectiveDate(convertStringToDate(floorDetails.getOccupancyDate()));
        List<BoundaryCategory> categories;
        for (Installment installment : taxInstallments) {

            if (betweenOrBefore(convertStringToDate(floorDetails.getOccupancyDate()), installment.getFromDate(),
                    installment.getToDate())) {
                categories = (List<BoundaryCategory>) entityManager.createNamedQuery(QUERY_BASERATE_BY_OCCUPANCY_ZONE)
                        .setParameter("boundary", zone.getId()).setParameter("usage", usage.getId())
                        .setParameter("structure", sc.getId())
                        .setParameter("fromDate", convertStringToDate(floorDetails.getOccupancyDate()))
                        .setParameter("toDate", installment.getToDate()).getResultList();
                if (categories.isEmpty()) {
                    isActive = Boolean.TRUE;
                }
            }

        }

        return isActive;
    }

    private Boolean between(final Date date, final Date fromDate, final Date toDate) {
        return (date.after(fromDate) || date.equals(fromDate)) && date.before(toDate) || date.equals(toDate);
    }

    private Boolean betweenOrBefore(final Date date, final Date fromDate, final Date toDate) {
        return between(date, fromDate, toDate) || date.before(fromDate);
    }

    public Boundary getBoundarybyboundaryNumberTypeHierarchy(final String boundaryNum, final BoundaryType boundaryType) {
        return boundaryService.getBoundaryByTypeAndNo(boundaryType, Long.valueOf(boundaryNum));
    }

    public BoundaryType getBoundaryTypeByNameandHierarchy(final String boundaryTypeName,
            final String hierarchyName) {
        return boundaryTypeService
                .getBoundaryTypeByNameAndHierarchyTypeName(boundaryTypeName, hierarchyName);
    }

    @SuppressWarnings("unchecked")
    public Boolean isCrossHierarchyMappingExist(Long localityId, Long wardId, Long blockId, Long wardBoundaryTypeId) {
        Boolean isMappingExists = Boolean.FALSE;

        StringBuilder queryString = new StringBuilder();
        queryString.append(
                "select parent.parent.boundaryNum as wardnum, parent.parent.name as wardname, parent.boundaryNum as blocknum,");
        queryString.append(" parent.name as blockname, child.boundaryNum as localitynum, child.name as localityname");
        queryString.append(" from CrossHierarchy ch, Boundary parent, Boundary child");
        queryString.append(
                " where ch.parent.id = :blockId  and ch.child.id = :localityId  and ch.parentType.name=:block and");
        queryString.append(
                " ch.childType.name=:locality and parent.parent.boundaryType.id=:wardBoundaryTypeId and parent.parent.id=:wardId");
        List<Object[]> boundaryDetails = entityManager.unwrap(Session.class).createQuery(queryString.toString())
                .setParameter("block", BLOCK).setParameter("locality", LOCALITY_BNDRY_TYPE)
                .setParameter("wardBoundaryTypeId", wardBoundaryTypeId).setParameter("blockId", blockId)
                .setParameter("localityId", localityId).setParameter("wardId", wardId).list();

        if (!boundaryDetails.isEmpty()) {
            isMappingExists = Boolean.TRUE;
        }
        return isMappingExists;
    }

    @SuppressWarnings("unchecked")
    public Boolean isCrossHierarchyMappingExistforLoclityandElecWrd(Long localityId, Long electionWardId) {
        Boolean isMappingExists = Boolean.FALSE;

        StringBuilder queryString = new StringBuilder();
        queryString.append(" from CrossHierarchy ch");
        queryString.append(
                " where ch.parent.id = :electionWardId  and ch.child.id = :localityId  ");
        List<CrossHierarchy> boundaryDetails = entityManager.unwrap(Session.class).createQuery(queryString.toString())
                .setParameter("electionWardId", electionWardId).setParameter("localityId", localityId).list();

        if (!boundaryDetails.isEmpty()) {
            isMappingExists = Boolean.TRUE;
        }
        return isMappingExists;
    }

    public AssessmentDetails getDuesForProperty(final HttpServletRequest request, String assessmentNo, String oldAssessmentNo) {
        AssessmentDetails assessmentDetails = new AssessmentDetails();
        BasicProperty basicProperty = fetchBasicProperty(assessmentNo, oldAssessmentNo);
        if (basicProperty != null) {
            assessmentDetails.setPropertyID(basicProperty.getUpicNo());
            assessmentDetails.setOldAssessmentNo(basicProperty.getOldMuncipalNum());
            assessmentDetails.setPropertyAddress(basicProperty.getAddress().toString());
            assessmentDetails.setOwners(basicProperty.getFullOwnerName());
            assessmentDetails.setStatus(basicProperty.getActive());
            Property property = basicProperty.getProperty();
            if (property != null) {
                assessmentDetails.setExempted(property.getIsExemptedFromTax());
                if (!property.getIsExemptedFromTax()) {
                    Map<String, BigDecimal> dmdCollMap = ptDemandDAO.getDemandIncludingPenaltyCollMap(property);
                    if (!dmdCollMap.isEmpty()) {
                        BigDecimal totalDemand = dmdCollMap.get(ARR_DMD_STR).add(dmdCollMap.get(CURR_FIRSTHALF_DMD_STR))
                                .add(dmdCollMap.get(CURR_SECONDHALF_DMD_STR));
                        BigDecimal totalColl = dmdCollMap.get(ARR_COLL_STR).add(dmdCollMap.get(CURR_FIRSTHALF_COLL_STR))
                                .add(dmdCollMap.get(CURR_SECONDHALF_COLL_STR));
                        assessmentDetails.setPropertyDue(totalDemand.subtract(totalColl));
                    }
                }
            }
            String restURL = format(PropertyTaxConstants.WTMS_TAXDUE_RESTURL,
                    WebUtils.extractRequestDomainURL(request, false), basicProperty.getUpicNo());
            Map<String, Object> taxDetails = simpleRestClient.getRESTResponseAsMap(restURL);
            if (!taxDetails.isEmpty()) {
                assessmentDetails.setWaterTaxDue(BigDecimal.valueOf((double) taxDetails.get(WATER_TAX_DUES)));
                assessmentDetails.setConnectionCount(Double.valueOf(taxDetails.get("connectionCount").toString()).intValue());
            }
            restURL = format(PropertyTaxConstants.STMS_TAXDUE_RESTURL,
                    WebUtils.extractRequestDomainURL(request, false), basicProperty.getUpicNo());
            taxDetails = simpleRestClient.getRESTResponseAsMap(restURL);
            if (!taxDetails.isEmpty())
                assessmentDetails.setSewerageDue(BigDecimal.valueOf((double) taxDetails.get("totalTaxDue")));
        }
        return assessmentDetails;
    }

    public TaxCalculatorResponse calculateTaxes(TaxCalculatorRequest taxCalculatorRequest) throws ParseException {
        TaxCalculatorResponse taxCalculatorResponse = new TaxCalculatorResponse();
        BigDecimal taxVariance;
        BigDecimal arvVariance;
        final Map<String, Installment> currYearInstMap = propertyTaxUtil.getInstallmentsForCurrYear(new Date());
        PropertyService propService = beanProvider.getBean(PROP_SERVICE, PropertyService.class);
        BasicProperty basicProperty = basicPropertyDAO.getBasicPropertyByPropertyID(taxCalculatorRequest.getAssessmentNo());
        PropertyImpl propertyImpl = (PropertyImpl) basicProperty.getProperty();
        taxCalculatorResponse.setAssessmentNo(basicProperty.getUpicNo());
        taxCalculatorResponse.setZoneNo(basicProperty.getPropertyID().getZone().getBoundaryNum());

        if (propertyImpl != null) {
            if (StringUtils.isNotBlank(propertyImpl.getReferenceId()))
                taxCalculatorResponse.setReferenceId(propertyImpl.getReferenceId());
            Map<String, String> taxDetailsMap = new HashMap<>();
            Map<String, BigDecimal> calculationsMap = getARVAndTaxDetails(propService, propertyImpl,
                    currYearInstMap.get(CURRENTYEAR_SECOND_HALF), false);
            taxCalculatorResponse.setExistingARV(calculationsMap.get(ARV));
            taxCalculatorResponse.setExistingHalfYearlyTax(calculationsMap.get(HALF_YEARLY_TAX));
            propertyImpl.setReferenceId(taxCalculatorRequest.getReferenceId());
            FloorDetailsInfo floorDetails = prepareFloorDetails(taxCalculatorRequest, basicProperty);
            try {
                taxDetailsMap = calculatePropertyTaxService.calculateTaxes(floorDetails);
            } catch (TaxCalculatorExeption e) {
                LOGGER.error("create : There are no Unit rates defined for chosen combinations", e);
            }
            calculationsMap = getARVAndTaxDetails(propService, propertyImpl, currYearInstMap.get(CURRENTYEAR_SECOND_HALF), true);
            taxCalculatorResponse.setCalculatedARV(
                    new BigDecimal(taxDetailsMap.get("Annual Rental Value").replaceAll("[a-zA-Z,]", "").trim()));
            taxCalculatorResponse
                    .setNewHalfYearlyTax(new BigDecimal(taxDetailsMap.get("Total Tax").replaceAll("[a-zA-Z,]", "").trim())
                            .subtract(new BigDecimal(
                                    taxDetailsMap.get("Unauthorized Penalty").replaceAll("[a-zA-Z,]", "").trim())));

            if (taxCalculatorResponse.getExistingHalfYearlyTax().compareTo(ZERO) > 0)
                taxVariance = ((taxCalculatorResponse.getNewHalfYearlyTax()
                        .subtract(taxCalculatorResponse.getExistingHalfYearlyTax())).multiply(BIGDECIMAL_100))
                                .divide(taxCalculatorResponse.getExistingHalfYearlyTax(), 1, BigDecimal.ROUND_HALF_UP);
            else
                taxVariance = BIGDECIMAL_100;
            taxCalculatorResponse.setTaxVariance(taxVariance);
            if (taxCalculatorResponse.getExistingARV().compareTo(ZERO) > 0) {
                arvVariance = ((taxCalculatorResponse.getCalculatedARV()
                        .subtract(taxCalculatorResponse.getExistingARV())).multiply(BIGDECIMAL_100))
                                .divide(taxCalculatorResponse.getExistingARV(), 1, BigDecimal.ROUND_HALF_UP);
            } else
                arvVariance = BIGDECIMAL_100;
            taxCalculatorResponse.setArvVariance(arvVariance);
        }
        return taxCalculatorResponse;
    }

    private FloorDetailsInfo prepareFloorDetails(TaxCalculatorRequest taxCalculatorRequest, BasicProperty basicProperty)
            throws ParseException {
        FloorDetailsInfo floorDetails = new FloorDetailsInfo();
        floorDetails.setZoneId(basicProperty.getPropertyID().getZone().getId());
        floorDetails.setOccupancyDate(
                DateUtils.getDate(taxCalculatorRequest.getFloorDetails().get(0).getOccupancyDate(), "dd-MM-yyyy"));
        List<Floor> floorList = getFloorList(taxCalculatorRequest.getFloorDetails());
        for (Floor floor : floorList) {
            FloorDetailsInfo floorDetailsInfo = new FloorDetailsInfo();
            floorDetailsInfo.setClassificationId(floor.getStructureClassification().getId());
            floorDetailsInfo.setConstructedPlinthArea((floor.getBuiltUpArea().getArea()));
            floorDetailsInfo.setConstructionDate((floor.getConstructionDate()));
            floorDetailsInfo.setFloorId((floor.getFloorNo().toString()));
            floorDetailsInfo.setOccupancyId((floor.getPropertyOccupation().getId()));
            floorDetailsInfo.setPlinthAreaInBuildingPlan((floor.getBuildingPlanPlinthArea().getArea()));
            floorDetailsInfo.setUsageId((floor.getPropertyUsage().getId()));
            floorDetails.getFloorTemp().add(floorDetailsInfo);
        }
        return floorDetails;
    }

    private Map<String, BigDecimal> getARVAndTaxDetails(PropertyService propService, PropertyImpl propertyImpl,
            Installment installment, boolean forNewCalculation) {
        Map<String, BigDecimal> calculationDetailsMap = new HashMap<>();
        Ptdemand ptDemand;
        if (!propertyImpl.getPtDemandSet().isEmpty()) {
            if (forNewCalculation)
                ptDemand = propertyImpl.getPtDemandSet().iterator().next();
            else
                ptDemand = ptDemandDAO.getNonHistoryCurrDmdForProperty(propertyImpl);
            if (ptDemand != null) {
                BigDecimal halfYearlyTax = propService.getTotalTaxExcludingUACPenalty(installment, ptDemand);
                calculationDetailsMap.put(HALF_YEARLY_TAX, halfYearlyTax);
                if (ptDemand.getDmdCalculations() != null)
                    calculationDetailsMap.put(ARV, ptDemand.getDmdCalculations().getAlv() == null ? ZERO
                            : ptDemand.getDmdCalculations().getAlv());
            }

        }

        return calculationDetailsMap;
    }

    public BasicProperty fetchBasicProperty(String assessmentNo, String oldAssessmentNo) {
        BasicProperty basicProperty = null;
        if (StringUtils.isNotBlank(assessmentNo))
            basicProperty = basicPropertyDAO.getBasicPropertyByPropertyID(assessmentNo);
        if (basicProperty == null && (StringUtils.isNotBlank(oldAssessmentNo))) {
            List<BasicProperty> basicProperties = (List<BasicProperty>) basicPropertyDAO
                    .getBasicPropertyByOldMunipalNo(oldAssessmentNo);
            if (!basicProperties.isEmpty() && basicProperties.size() == 1)
                basicProperty = basicProperties.get(0);
        }
        return basicProperty;
    }

    public Property getPropertyByApplicationNo(String applicationNo) {
        return propertyHibernateDAO.getPropertyByApplicationNo(applicationNo);
    }

    public NewPropertyDetails saveDocument(DocumentDetailsRequest documentDetails, String applicationNo) {
        Property property = getPropertyByApplicationNo(applicationNo);
        NewPropertyDetails newPropertyDetails = new NewPropertyDetails();
        PropertyService propService = beanProvider.getBean(PROP_SERVICE, PropertyService.class);
        VacancyRemissionRepository vacancyRemissionRepository = beanProvider.getBean("vacancyRemissionRepository",
                VacancyRemissionRepository.class);
        Document document;
        List<Document> docs = new ArrayList<>();
        List<DocumentType> documentTypes = propService.getDocumentTypesForTransactionType(TransactionType.CREATE);
        for (DocumentType documentType : documentTypes) {
            document = new Document();
            document.setType(vacancyRemissionRepository.findDocumentTypeByNameAndTransactionType(
                    documentType.getName(), TransactionType.CREATE));
            if (DOCUMENT_TYPE_PHOTO_OF_ASSESSMENT.equalsIgnoreCase(documentType.getName()))
                document.setFiles(propService.addToFileStore(documentDetails.getPhotoFile()));
            docs.add(document);
        }
        property.setDocuments(docs);
        basicPropertyService.update(property.getBasicProperty());
        newPropertyDetails.setApplicationNo(applicationNo);
        ErrorDetails errorDetails = new ErrorDetails();
        errorDetails.setErrorCode(THIRD_PARTY_DOCS_UPLOAD_SUCCESS_CODE);
        errorDetails.setErrorMessage(THIRD_PARTY_DOCS_UPLOAD_SUCCESS_MSG);
        newPropertyDetails.setErrorDetails(errorDetails);
        return newPropertyDetails;
    }
}
