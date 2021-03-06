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
package org.egov.wtms.masters.service;

import org.egov.wtms.masters.entity.ConnectionCategory;
import org.egov.wtms.masters.entity.DonationHeader;
import org.egov.wtms.masters.entity.PropertyType;
import org.egov.wtms.masters.entity.UsageType;
import org.egov.wtms.masters.repository.DonationHeaderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class DonationHeaderService {

    private final DonationHeaderRepository donationHeaderRepository;

    @Autowired
    public DonationHeaderService(final DonationHeaderRepository donationHeaderRepository) {
        this.donationHeaderRepository = donationHeaderRepository;
    }

    public DonationHeader findBy(final Long donationHeaderId) {
        return donationHeaderRepository.findOne(donationHeaderId);
    }

    @Transactional
    public DonationHeader persistDonationHeader(final DonationHeader donationHeader) {
        return donationHeaderRepository.save(donationHeader);
    }

    public List<DonationHeader> findAll() {
        return donationHeaderRepository.findAll();
    }

    public List<DonationHeader> findAllByCategory(final ConnectionCategory category) {
        return donationHeaderRepository.findAllByCategory(category);
    }

    public List<DonationHeader> findAllByUsageType(final UsageType usageType) {
        return donationHeaderRepository.findAllByUsageType(usageType);
    }

    public DonationHeader load(final Long id) {
        return donationHeaderRepository.getOne(id);
    }

    public List<DonationHeader> findByCategoryandUsage(final ConnectionCategory category, final UsageType usageType) {
        return donationHeaderRepository.findByCategoryAndUsageType(category, usageType);
    }

    public DonationHeader findByPropertyandCategoryandUsageandMinPipeSize(final PropertyType propertyType,
            final ConnectionCategory category, final UsageType usageType, final double pipeSize) {
        return donationHeaderRepository.findByPropertyandCategoryAndUsageTypeAndPipeSize(propertyType, category,
                usageType, pipeSize);
    }

    // findDonationDetailsByPropertyAndCategoryAndUsageandPipeSize
    public List<DonationHeader> findDonationDetailsByPropertyAndCategoryAndUsageandPipeSize(
            final PropertyType propertyType, final ConnectionCategory categoryType, final UsageType usageType,
            final double minPipeSize, final double maxPipeSize) {
        return donationHeaderRepository.findDonationByPropertyAndCategoryAndUsageandMinPipeSizeAndMaxPipesize(
                propertyType, categoryType, usageType, minPipeSize, maxPipeSize);
    }

}