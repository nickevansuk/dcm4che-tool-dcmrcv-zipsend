/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is part of dcm4che, an implementation of DICOM(TM) in
 * Java(TM), hosted at http://sourceforge.net/projects/dcm4che.
 *
 * The Initial Developer of the Original Code is
 * Nick Evans (http://www.nickevans.me), Cardiff. UK/Europe.
 * Portions created by the Initial Developer are Copyright (C) 2002-2005
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * Nick Evans (http://www.nickevans.me)
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */
package org.dcm4che2.tool.dcmrcv;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Nick Evans (http://www.nickevans.me)
 * @version $Revision: 1 $ $Date: 2011-09-11 12:57:21 +0000 (Sun, 11 Sept 2011) $
 * @since Sept 11, 2011
 */
public class EmailParams {

	static Logger LOG = LoggerFactory.getLogger(EmailParams.class);
	
	private String patientHospitalNumber = null;
	private String patientSurname = null;
	private String patientForename = null;
	
	private String consultantCode = null;
	private Date patientDateOfBirth = null;
	
	private Date seriesDateTime = null;
	

	public EmailParams(String patientID, String patientName, String dateOfBirth, String seriesDate, String seriesTime) {
		setPatientHospitalNumber(patientID);
		
		//Parse patient name
		if (patientName != null) {
			patientName = patientName.replace('^', ' ');
			
			//Use a comma if one is present
			String splitChar = " ";
			if (patientName.contains(",")) splitChar = ",";
			
			//Split the forename and surname
			String[] names = patientName.split(splitChar, 2);
			setPatientSurname(names[0]);
			if (names.length > 1) setPatientForename(names[1]);
		}
		
		//Parse patient date of birth
		if (dateOfBirth != null) {
			DateFormat formatter = new SimpleDateFormat("yyyyMMdd");
			try {
				patientDateOfBirth = (Date)formatter.parse(dateOfBirth);
			} catch (ParseException e) {
				LOG.warn("Date conversion failed for date of birth: {} {}", dateOfBirth);
				patientDateOfBirth = null;
			}
		}
		
		//Parse series date
		if (seriesDate != null && seriesTime != null) {
			DateFormat formatter = new SimpleDateFormat("yyyyMMdd HHmmss");
			try {
				seriesDateTime = (Date)formatter.parse(seriesDate + " " + seriesTime);
			} catch (ParseException e) {
				LOG.warn("Date conversion failed for series date: {} {}", seriesDate, seriesTime);
				seriesDateTime = new Date();
			}
		} else {
			seriesDateTime = new Date();
		}
		
	}

	/*
	 * Produce a subject line of the form:
	 * 	  {Description} \C={Consultant Code} \H={Patient ID Number} \D={Date of Birth} \S={Surname} \F={Forename}
	 */
	public String getParamString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Ultrasound Scan");
		if (consultantCode != null) {
			sb.append(" \\C=");
			sb.append(consultantCode);
		}
		if (patientHospitalNumber != null) {
			sb.append(" \\H=");
			sb.append(patientHospitalNumber);
		}
		if (patientDateOfBirth != null) {
			sb.append(" \\D=");
			DateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
			sb.append(formatter.format(patientDateOfBirth));
		}
		if (patientSurname != null) {
			sb.append(" \\S=");
			sb.append(patientSurname);
		}
		if (patientForename != null) {
			sb.append(" \\F=");
			sb.append(patientForename);
		}
		return sb.toString();
	}
	
	public Date getSeriesDateTime() { return seriesDateTime; }
		
	//Getters and setters for parameters
	public void setPatientHospitalNumber(String patientHospitalNumber) {
		if (patientHospitalNumber != null) {
			this.patientHospitalNumber = patientHospitalNumber.trim();
		}
	}
	public String getPatientHospitalNumber() { return patientHospitalNumber; }
	public void setPatientSurname(String patientSurname) {
		if (patientSurname != null) {
			this.patientSurname = patientSurname.trim();
		}
	}
	public String getPatientSurname() { return patientSurname; }
	public void setPatientForename(String patientForename) { 
		if (patientForename != null) {
			this.patientForename = patientForename.trim(); 
		}
	}
	public String getPatientForename() { return patientForename; }
	public void setConsultantCode(String consultantCode) { 
		if (consultantCode != null)
			consultantCode = consultantCode.replace('^', ' ');
		this.consultantCode = consultantCode;
	}
	public String getConsultantCode() { return consultantCode; }
	public void setDateOfBirth(Date dateOfBirth) { this.patientDateOfBirth = dateOfBirth; }
	public Date getDateOfBirth() { return patientDateOfBirth;	}
}