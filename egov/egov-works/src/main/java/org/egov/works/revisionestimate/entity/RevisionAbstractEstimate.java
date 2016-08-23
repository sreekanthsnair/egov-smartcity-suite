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
package org.egov.works.revisionestimate.entity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.egov.works.abstractestimate.entity.AbstractEstimate;
import org.egov.works.abstractestimate.entity.Activity;

@Entity
@Table(name = "EGW_REVISION_ESTIMATE")
public class RevisionAbstractEstimate extends AbstractEstimate {

    private static final long serialVersionUID = 4389815027807161766L;

    @Transient
    private List<String> revisionEstActions = new ArrayList<String>();

    public enum RevisionEstimateStatus {
        NEW, CREATED, CHECKED, REJECTED, RESUBMITTED, CANCELLED, APPROVED
    }

    @Transient
    private String additionalRule;

    @Transient
    private BigDecimal amountRule;

    private transient List<Activity> nonTenderedActivities = new ArrayList<Activity>(0);

    private transient List<Activity> lumpSumActivities = new ArrayList<Activity>(0);

    private transient List<Activity> changeQuantityNTActivities = new ArrayList<Activity>(0);

    private transient List<Activity> changeQuantityLSActivities = new ArrayList<Activity>(0);

    private transient List<Activity> changeQuantityActivities = new ArrayList<Activity>(0);

    @Override
    public String getStateDetails() {
        return "Revision Estimate : " + getEstimateNumber();
    }

    public List<String> getRevisionEstActions() {
        return revisionEstActions;
    }

    public void setRevisionEstActions(final List<String> revisionEstActions) {
        this.revisionEstActions = revisionEstActions;
    }

    public void deleteNonSORActivities() {
        if (getActivities() != null && getActivities().size() > 0)
            for (final Activity estActivity : getActivities())
                if (estActivity.getNonSor() != null)
                    estActivity.setNonSor(null);
    }

    public String getAdditionalRule() {
        return additionalRule;
    }

    public BigDecimal getAmountRule() {
        return amountRule;
    }

    public void setAdditionalRule(final String additionalRule) {
        this.additionalRule = additionalRule;
    }

    public void setAmountRule(final BigDecimal amountRule) {
        this.amountRule = amountRule;
    }

    public List<Activity> getNonTenderedActivities() {
        return nonTenderedActivities;
    }

    public void setNonTenderedActivities(final List<Activity> nonTenderedActivities) {
        this.nonTenderedActivities = nonTenderedActivities;
    }

    public List<Activity> getLumpSumActivities() {
        return lumpSumActivities;
    }

    public void setLumpSumActivities(final List<Activity> lumpSumActivities) {
        this.lumpSumActivities = lumpSumActivities;
    }

    public List<Activity> getChangeQuantityNTActivities() {
        return changeQuantityNTActivities;
    }

    public void setChangeQuantityNTActivities(final List<Activity> changeQuantityNTActivities) {
        this.changeQuantityNTActivities = changeQuantityNTActivities;
    }

    public List<Activity> getChangeQuantityLSActivities() {
        return changeQuantityLSActivities;
    }

    public void setChangeQuantityLSActivities(final List<Activity> changeQuantityLSActivities) {
        this.changeQuantityLSActivities = changeQuantityLSActivities;
    }

    public List<Activity> getChangeQuantityActivities() {
        return changeQuantityActivities;
    }

    public void setChangeQuantityActivities(final List<Activity> changeQuantityActivities) {
        this.changeQuantityActivities = changeQuantityActivities;
    }

}