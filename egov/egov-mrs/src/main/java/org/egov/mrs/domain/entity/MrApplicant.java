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
package org.egov.mrs.domain.entity;

import org.egov.common.entity.EducationalQualification;
import org.egov.common.entity.Nationality;
import org.egov.infra.filestore.entity.FileStoreMapper;
import org.egov.infra.persistence.entity.AbstractAuditable;
import org.egov.mrs.domain.enums.MaritalStatus;
import org.egov.mrs.domain.enums.ReligionPractice;
import org.egov.mrs.masters.entity.MarriageReligion;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.SafeHtml;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.*;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import static org.egov.infra.validation.constants.ValidationErrorCode.INVALID_ALPHABETS_WITH_SPACE;
import static org.egov.infra.validation.constants.ValidationErrorCode.INVALID_ALPHANUMERIC_WITH_SPECIAL_CHARS;
import static org.egov.infra.validation.constants.ValidationRegex.ALPHABETS_WITH_SPACE;
import static org.egov.infra.validation.constants.ValidationRegex.ALPHANUMERIC_WITH_SPECIAL_CHARS;

import java.io.IOException;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "egmrs_applicant")
@SequenceGenerator(name = MrApplicant.SEQ_APPLICANT, sequenceName = MrApplicant.SEQ_APPLICANT, allocationSize = 1)
public class MrApplicant extends AbstractAuditable {

    public static final String SEQ_APPLICANT = "SEQ_EGMRS_APPLICANT";
    private static final long serialVersionUID = -4678440835941976527L;
    @Id
    @GeneratedValue(generator = SEQ_APPLICANT, strategy = GenerationType.SEQUENCE)
    private Long id;

    @Embedded
    private Name name;

    @SafeHtml
    @Length(max = 20)
    @Pattern(regexp = ALPHABETS_WITH_SPACE, message = INVALID_ALPHABETS_WITH_SPACE)
    private String otherName;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "religion")
    private MarriageReligion religion;

    /* @NotNull */
    @Enumerated(EnumType.STRING)
    private ReligionPractice religionPractice;

    @NotNull
    @Column(name = "ageinyears")
    private Integer ageInYearsAsOnMarriage;

    @NotNull
    @Column(name = "ageinmonths")
    private Integer ageInMonthsAsOnMarriage;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "relationstatus")
    private MaritalStatus maritalStatus;

    @SafeHtml
    @Length(max = 60)
    @Pattern(regexp = ALPHABETS_WITH_SPACE, message = INVALID_ALPHABETS_WITH_SPACE)
    private String occupation;

    @SafeHtml
    @Length(max = 20)
    @Column(insertable = false, updatable = false)
    private String aadhaarNo;

    @Transient
    private byte[] photo;
    @Transient
    private byte[] signature;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "photoFileStore")
    private FileStoreMapper photoFileStore;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "signatureFileStore")
    private FileStoreMapper signatureFileStore;

    @Valid
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "proofsattached")
    private IdentityProof proofsAttached;

    @Embedded
    @Valid
    private Contact contactInfo;

    @NotNull
    @Valid
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "applicant")
    private Set<MrApplicantDocument> applicantDocuments = new HashSet<>();

    @NotNull
    @SafeHtml
    @Length(max = 110)
    @Pattern(regexp = ALPHABETS_WITH_SPACE, message = INVALID_ALPHABETS_WITH_SPACE)
    private String parentsName;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "qualification")
    private EducationalQualification qualification;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nationality")
    private Nationality nationality;

    @NotNull
    @SafeHtml
    @Length(max = 100)
    @Pattern(regexp = ALPHANUMERIC_WITH_SPECIAL_CHARS, message = INVALID_ALPHANUMERIC_WITH_SPECIAL_CHARS)
    private String street;

    @NotNull
    @SafeHtml
    @Length(max = 100)
    @Pattern(regexp = ALPHABETS_WITH_SPACE, message = INVALID_ALPHABETS_WITH_SPACE)
    private String locality;

    @NotNull
    @SafeHtml
    @Length(max = 30)
    @Pattern(regexp = ALPHABETS_WITH_SPACE, message = INVALID_ALPHABETS_WITH_SPACE)
    private String city;

    @Transient
    private List<MarriageDocument> documents;

    private transient MultipartFile photoFile;

    private transient MultipartFile signatureFile;

    private transient String encodedPhoto;
    private transient String encodedSignature;

    private boolean handicapped = false;

    public String getFullName() {
        String fullName = getName().getFirstName();

        fullName += getName().getMiddleName() == null ? "" : " " + getName().getMiddleName();
        fullName += getName().getLastName() == null ? "" : " " + getName().getLastName();

        return fullName;
    }

    /**
     * Copies MultipartFile bytes to persistent byte array
     *
     * @throws IOException
     */
    public void copyPhotoAndSignatureToByteArray() throws IOException {
        setPhoto(FileCopyUtils.copyToByteArray(getPhotoFile().getInputStream()));

        if (getSignatureFile() != null)
            setSignature(FileCopyUtils.copyToByteArray(getSignatureFile().getInputStream()));
    }

    public boolean isCopyFilesToByteArray() {
        return photoFile != null && photoFile.getSize() > 0 || signatureFile != null && signatureFile.getSize() > 0;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(final Long id) {
        this.id = id;
    }

    public String getOtherName() {
        return otherName;
    }

    public void setOtherName(final String otherName) {
        this.otherName = otherName;
    }

    public MarriageReligion getReligion() {
        return religion;
    }

    public void setReligion(final MarriageReligion religion) {
        this.religion = religion;
    }

    public ReligionPractice getReligionPractice() {
        return religionPractice;
    }

    public void setReligionPractice(final ReligionPractice religionPractice) {
        this.religionPractice = religionPractice;
    }

    public Integer getAgeInYearsAsOnMarriage() {
        return ageInYearsAsOnMarriage;
    }

    public void setAgeInYearsAsOnMarriage(final Integer ageInYearsAsOnMarriage) {
        this.ageInYearsAsOnMarriage = ageInYearsAsOnMarriage;
    }

    public Integer getAgeInMonthsAsOnMarriage() {
        return ageInMonthsAsOnMarriage;
    }

    public void setAgeInMonthsAsOnMarriage(final Integer ageInMonthsAsOnMarriage) {
        this.ageInMonthsAsOnMarriage = ageInMonthsAsOnMarriage;
    }

    public MaritalStatus getMaritalStatus() {
        return maritalStatus;
    }

    public void setMaritalStatus(final MaritalStatus maritalStatus) {
        this.maritalStatus = maritalStatus;
    }

    public String getOccupation() {
        return occupation;
    }

    public void setOccupation(final String occupation) {
        this.occupation = occupation;
    }

    public String getAadhaarNo() {
        return aadhaarNo;
    }

    public void setAadhaarNo(final String aadhaarNo) {
        this.aadhaarNo = aadhaarNo;
    }

    public byte[] getSignature() {
        return signature;
    }

    public void setSignature(final byte[] signature) {
        this.signature = signature;
    }

    public byte[] getPhoto() {
        return photo;
    }

    public void setPhoto(final byte[] photo) {
        this.photo = photo;
    }

    public Name getName() {
        return name;
    }

    public void setName(final Name name) {
        this.name = name;
    }

    public IdentityProof getProofsAttached() {
        return proofsAttached;
    }

    public void setProofsAttached(final IdentityProof proofsAttached) {
        this.proofsAttached = proofsAttached;
    }

    public Contact getContactInfo() {
        return contactInfo;
    }

    public void setContactInfo(final Contact contactInfo) {
        this.contactInfo = contactInfo;
    }

    public List<MarriageDocument> getDocuments() {
        return documents;
    }

    public void setDocuments(final List<MarriageDocument> documents) {
        this.documents = documents;
    }

    public void addApplicantDocument(final MrApplicantDocument applicantDocument) {
        applicantDocument.setApplicant(this);
        getApplicantDocuments().add(applicantDocument);
    }

    public MultipartFile getPhotoFile() {
        return photoFile;
    }

    public void setPhotoFile(final MultipartFile photoFile) {
        this.photoFile = photoFile;
    }

    public MultipartFile getSignatureFile() {
        return signatureFile;
    }

    public void setSignatureFile(final MultipartFile signatureFile) {
        this.signatureFile = signatureFile;
    }

    public String getEncodePhotoToString() {
        return Base64.getEncoder().encodeToString(getPhoto());
    }

    public String getEncodedPhoto() {
        return encodedPhoto;
    }

    public void setEncodedPhoto(final String encodedPhoto) {
        this.encodedPhoto = encodedPhoto;
    }

    public String getEncodedSignature() {
        return encodedSignature;
    }

    public void setEncodedSignature(final String encodedSignature) {
        this.encodedSignature = encodedSignature;
    }

    public FileStoreMapper getPhotoFileStore() {
        return photoFileStore;
    }

    public void setPhotoFileStore(final FileStoreMapper photoFileStore) {
        this.photoFileStore = photoFileStore;
    }

    public FileStoreMapper getSignatureFileStore() {
        return signatureFileStore;
    }

    public void setSignatureFileStore(final FileStoreMapper signatureFileStore) {
        this.signatureFileStore = signatureFileStore;
    }

    public boolean isHandicapped() {
        return handicapped;
    }

    public void setHandicapped(final boolean handicapped) {
        this.handicapped = handicapped;
    }

    public String getParentsName() {
        return parentsName;
    }

    public void setParentsName(final String parentsName) {
        this.parentsName = parentsName;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(final String street) {
        this.street = street;
    }

    public String getCity() {
        return city;
    }

    public void setCity(final String city) {
        this.city = city;
    }

    public Nationality getNationality() {
        return nationality;
    }

    public void setNationality(final Nationality nationality) {
        this.nationality = nationality;
    }


    public String getLocality() {
        return locality;
    }

    public void setLocality(String locality) {
        this.locality = locality;
    }

    public EducationalQualification getQualification() {
        return qualification;
    }

    public void setQualification(EducationalQualification qualification) {
        this.qualification = qualification;
    }

    public Set<MrApplicantDocument> getApplicantDocuments() {
        return applicantDocuments;
    }

    public void setApplicantDocuments(final Set<MrApplicantDocument> applicantDocuments) {
        this.applicantDocuments = applicantDocuments;
    }

}
