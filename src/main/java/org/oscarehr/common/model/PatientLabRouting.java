/*
 * Copyright (c) 2010. Department of Family Medicine, McMaster University. All Rights Reserved.
 * 
 * This software is published under the GPL GNU General Public License.
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version. 
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. 
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA. 
 *
 * This software was written for the
 * Department of Family Medicine
 * McMaster University
 * Hamilton
 * Ontario, Canada
 */
package org.oscarehr.common.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "patientLabRouting")
public class PatientLabRouting extends AbstractModel<Integer> {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(name = "lab_no")
	/** This is also referred to as segmentId in parts of the code... */
	private int labNumber;

	@Column(name = "lab_type")
	private String labType;
	
	@Column(name = "demographic_no")
	private Integer demographicNumber;

	public int getLabNumber() {
    	return labNumber;
    }

	public void setLabNumber(int labNumber) {
    	this.labNumber = labNumber;
    }

	public String getLabType() {
    	return labType;
    }

	public void setLabType(String labType) {
    	this.labType = labType;
    }

	public Integer getDemographicNumber() {
    	return demographicNumber;
    }

	public void setDemographicNumber(Integer demographicNumber) {
    	this.demographicNumber = demographicNumber;
    }

	@Override
    public Integer getId() {
    	return id;
    }
}