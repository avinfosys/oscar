package org.oscarehr.integration.hl7.handlers;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.oscarehr.integration.hl7.model.PatientId;
import org.oscarehr.util.MiscUtils;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.util.Terser;

public class BasePhsStarHandler {

	Logger logger = MiscUtils.getLogger();
	
	Message msg = null;
	Terser t = null;
	
	SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
	SimpleDateFormat sdfShort = new SimpleDateFormat("yyyyMMdd");
	
	protected String extractOrEmpty(String path) throws HL7Exception {
		String value = t.get(path);
		if(value==null)
			value="";
		return value;
	}
	
	protected Date convertToDate(String hl7Data) {
		Date d = null;
		try {
			if(hl7Data.length() == 8)
				d = sdfShort.parse(hl7Data);
			else if(hl7Data.length() == 12){
				d = sdf.parse(hl7Data);
			}
		} catch(ParseException e) {logger.error(e);return null;}
		return d;
	}

	
	///////////////////////////////////////////////////////////////////////////////////////////
	// ADT SEGMENTS
	///////////////////////////////////////////////////////////////////////////////////////////
	
	protected Map<String,PatientId> extractInternalPatientIds() throws HL7Exception {
		Map<String,PatientId> ids = new LinkedHashMap<String,PatientId>();
		int x=0;
		while(true) {
			String identifier = t.get("PID-3("+x+")-1");
			String authority = t.get("PID-3("+x+")-4");
			String typeId = t.get("PID-3("+x+")-5");
			
			if(identifier == null)
				break;
			
			PatientId tmp = new PatientId(identifier, authority, typeId);
			ids.put(typeId,tmp);
			x++;
		}		
		return ids;
	}
	
	protected PatientId extractPatientAccountNumber() throws HL7Exception {
		PatientId id = null;
				
		String identifier = t.get("PID-18-1");
		String authority = t.get("PID-18-4");
		String typeId = t.get("PID-18-5");
		
		if(identifier == null)
			return null;
		
		id = new PatientId(identifier, authority, typeId);
					
		return id;
	}
	


	/*
	 * PID-9 alias
	 * PID-15 language
	 * PID-18 patient acct #
	 * PID-19 SSN
	 * PID-26 citizenship
	 * 
	 */
	protected void extractDemographicData() throws HL7Exception {
		Map<String,PatientId> internalIds = extractInternalPatientIds();			
		String temporaryEpn = t.get("PID-4-1");		
		String lastName = t.get("PID-5-1");
		String firstName = t.get("PID-5-2");
		String middleName = extractOrEmpty("PID-5-3");		
		String strDob = t.get("PID-7-1");
		Date dob = convertToDate(strDob);
		String gender = t.get("PID-8");
			
		//can be multiple addresses, but only interested in (H)ome address.
		String address1 = extractOrEmpty("PID-11-1");
		String address2 = extractOrEmpty("PID-11-2");
		String city = extractOrEmpty("PID-11-3");
		String province = extractOrEmpty("PID-11-4");
		String postalCode = extractOrEmpty("PID-11-5");
		String country = extractOrEmpty("PID-11-6");
		String typeCode = extractOrEmpty("PID-11-7"); //H=home, M=mailing
						
		String phone = extractOrEmpty("PID-13-1");
		String bPhone = extractOrEmpty("PID-14-1");
		String patientDeathIndicator = extractOrEmpty("PID-30-1");
		String pseudoPerson = extractOrEmpty("PID-50-1");
		
		for(String typeId:internalIds.keySet()) {
			logger.debug(internalIds.get(typeId));
		}
		logger.debug("temporary PHS EPN = " + temporaryEpn);
		logger.debug("name=" + lastName + ","+firstName+" " + middleName);
		logger.debug("dob="+dob);
		logger.debug("gender="+gender);
		logger.debug("address1="+address1);
		logger.debug("address2="+address2);
		logger.debug("city="+city);
		logger.debug("province="+province);
		logger.debug("postalCode="+postalCode);
		logger.debug("country="+country);
		logger.debug("typeCode="+typeCode);
		logger.debug("phone="+phone);
		logger.debug("business phone="+bPhone);
		logger.debug("patient death indicator="+patientDeathIndicator);
		logger.debug("pseudoPerson="+pseudoPerson);		
	}
	
	/*
	 * Doesn't seem to work with HL7 v2.2
	 */
	protected void extractAdditionalDemographicData() throws HL7Exception {
		logger.info("PD1");
		
		try {
			t.get("/PD1-4-1");
		}catch(HL7Exception e) {logger.error(e);return;}
		logger.info("PD1-1");
		String practionerId = t.get("/PD1-4-1");
		String practionerLastName = this.extractOrEmpty("/PD1-4-2");
		String practionerFirstName = this.extractOrEmpty("/PD1-4-3");
		String practionerMiddleName = this.extractOrEmpty("/PD1-4-4");
		
		logger.debug("Additional Demographic");
		logger.debug("practionerId="+practionerId);
		logger.debug("practioner name="+ practionerLastName + "," + practionerFirstName + " " + practionerMiddleName);
		
	}
	
	/*
	 * 2 - patient class
	 * 3- patient location
	 * 10 - hospital service code
	 * 17 - admitting dr
	 * 18 - patient type
	 * 19 - visit #
	 * 39 - service facility 
	 * 44/45 - admit start/end times
	 * 50 - alternate visit id (phs temp patient acct #)
	 */
	protected void extractPatientVisitData() throws HL7Exception {
		
	}
	
	/*
	 * 3-2 - admit reason
	 * 8 - expected admit date
	 * 9 - expected discharge date
	 * 12 - visit descr
	 * 
	 */
	protected void extractAdditionalPatientVisitData() throws HL7Exception {
		
	}
	
	/*
	 * 2 - FF
	 * 4 - Description
	 * 6 - Diagnosis type (A = admitting)
	 */
	protected void extractDiagnosis() throws HL7Exception {
		
	}
	
	
	
	
	
	//////////////////////////////////////////////////////////////////////////
	// SCHEDULING SEGMENTS
	//////////////////////////////////////////////////////////////////////////
	protected void extractScheduleData() throws HL7Exception {		
		String aptId = t.get("/SCH-2(0)-1");
		String aptIdAss = t.get("/SCH-2(0)-2");
		
		//appointment_id
		String patBookingId = t.get("/SCH-2(1)-1");
		String patBookingIdAss = t.get("/SCH-2(1)-2");
		
		//program name
		String ruCode = t.get("/SCH-5-2");
				
		/*
		 * S12^^HS
		 * ..see comments in spec
		 */
		String eventReason1 = this.extractOrEmpty("/SCH-6-1");
		String eventReason2 = this.extractOrEmpty("/SCH-6-2");
		String eventReason3 = this.extractOrEmpty("/SCH-6-3");
		
		
		//SCH-7 appt reason - not on s12
		String appointmentReason = this.extractOrEmpty("/SCH-7-1");	
		
		//SCH-8 appt type (ASMT^ASSESSMENT DIABETES^HS)
		String abr = t.get("SCH-8-1");
		String apptType = t.get("SCH-8-2");
		String apptTypeAss = t.get("SCH-8-3");
						
		String aptDuration = t.get("SCH-9-1");		
		String aptDurationUnit = t.get("SCH-10-1");		
		String startTimeStr = t.get("SCH-11-4");
		Date startTime = this.convertToDate(startTimeStr);		
		
		String fillerContactLastName = t.get("SCH-16-2");
		String fillerContactFirstName = t.get("SCH-16-3");
				
		logger.debug("aptId="+aptId);
		logger.debug("aptIdAss="+aptIdAss);
		logger.debug("patBookingId="+patBookingId);
		logger.debug("patBookingIdAss="+patBookingIdAss);
		logger.debug("ruCode="+ruCode);
		logger.debug("appt type="+apptType);
		logger.debug("aptDuration="+aptDuration);
		logger.debug("aptDurationUnit="+aptDurationUnit);
		logger.debug("startTime="+startTime);
		logger.debug("filler contact = "+ fillerContactLastName + "," + fillerContactFirstName );		
	}
	
	/*
	 * AIS-3 CP codes?
	 */
	protected void extractAppointmentInformation() throws HL7Exception {
		String serviceCode = t.get("AIS-3-1"); //procedure code
		String probookingName = t.get("AIS-3-2"); //procedure name (could have changed)
		String abrev = t.get("AIS-3-4"); //enterprise level procedure code
		String proName = t.get("AIS-3-5");	//static procedure name
						
		String strStartTime = t.get("AIS-4-1");
		Date startTime = this.convertToDate(strStartTime);				
		
		String offset = t.get("AIS-5-1");		
		String offsetUnit = t.get("AIS-6-1");		
		String duration = t.get("AIS-7-1");		
		String durationUnit = t.get("AIS-8-1");		
		
		String statusCode = t.get("AIS-10-1"); //BOOKED or CANCEL
		
		logger.debug("serviceCode="+serviceCode);
		logger.debug("proBookingName="+probookingName);
		logger.debug("abrev="+abrev);
		logger.debug("proName="+proName);
		logger.debug("startTime="+startTime);
		logger.debug("offset="+offset);
		logger.debug("offset unit="+offsetUnit);
		logger.debug("duration="+duration);
		logger.debug("duration unit="+durationUnit);
		logger.debug("statusCode="+statusCode);
		
	}
	
	protected void extractAppointmentLocation() throws HL7Exception {
		String ruCode = t.get("AIL-3-1");
		String roomAbbr = t.get("AIL-3-2");
		String facilityCode = t.get("AIL-3-4");
		String descr = t.get("AIL-3-9");
						
		String locationType=t.get("AIL-4-1");
		String locationTypePool=t.get("AIL-4-2");
				
		String strStartTime = t.get("AIL-6-1");
		Date startTime = this.convertToDate(strStartTime);
					
		String offset = t.get("AIL-7-1");		
		String offsetUnit = t.get("AIL-8-1");
		String duration = t.get("AIL-9-1");
		String durationUnit = t.get("AIL-10-1");
		String statusCode = t.get("AIL-12-1"); //BOOKED or CANCEL
		
		logger.debug("ruCode="+ruCode);
		logger.debug("roomAbbr="+roomAbbr);
		logger.debug("FacilityCode="+facilityCode);
		logger.debug("descr="+descr);
		logger.debug("locationType="+locationType);
		logger.debug("locationTypePool="+locationTypePool);
		logger.debug("startTime="+startTime);
		logger.debug("offset="+offset);
		logger.debug("offset unit="+offsetUnit);
		logger.debug("duration="+duration);
		logger.debug("duration unit="+durationUnit);
		logger.debug("statusCode="+statusCode);				
	}
	
	protected void extractAppointmentPersonnel() throws HL7Exception {
		String practId = t.get("AIP-3-1");
		String lastName = t.get("AIP-3-2");
		String firstName = t.get("AIP-3-3");
		String middleName = this.extractOrEmpty("AIP-3-4");
				
		String role = t.get("AIP-4-1");
		String rolePool = t.get("AIP-4-2");
				
		String strStartTime = t.get("AIP-6-1");
		Date startTime = this.convertToDate(strStartTime);
		
		String offset = t.get("AIP-7-1");		
		String offsetUnit = t.get("AIP-8-1");		
		String duration = t.get("AIP-9-1");		
		String durationUnit = t.get("AIP-10-1");		
		String statusCode = t.get("AIP-12-1"); //BOOKED or CANCEL
		
		logger.info("practId="+practId);
		logger.info("name="+lastName + "," + firstName + " " + middleName);
		logger.info("role="+role);
		logger.info("startTime="+startTime);
		logger.info("offset="+offset);
		logger.info("offset unit="+offsetUnit);
		logger.info("duration="+duration);
		logger.info("duration unit="+durationUnit);
		logger.info("statusCode="+statusCode);
			
	}
}