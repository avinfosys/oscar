/**
 * Copyright (c) 2001-2002. Department of Family Medicine, McMaster University. All Rights Reserved.
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
package org.oscarehr.ws.rest;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpSession;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.log4j.Logger;
import org.oscarehr.PMmodule.model.ProgramProvider;
import org.oscarehr.PMmodule.service.ProgramManager;
import org.oscarehr.PMmodule.service.ProviderManager;
import org.oscarehr.casemgmt.dao.CaseManagementNoteLinkDAO;
import org.oscarehr.casemgmt.dao.IssueDAO;
import org.oscarehr.casemgmt.model.CaseManagementCPP;
import org.oscarehr.casemgmt.model.CaseManagementIssue;
import org.oscarehr.casemgmt.model.CaseManagementNote;
import org.oscarehr.casemgmt.model.CaseManagementNoteLink;
import org.oscarehr.casemgmt.model.Issue;
import org.oscarehr.casemgmt.service.CaseManagementManager;
import org.oscarehr.casemgmt.service.NoteSelectionCriteria;
import org.oscarehr.casemgmt.service.NoteSelectionResult;
import org.oscarehr.casemgmt.service.NoteService;
import org.oscarehr.casemgmt.web.CaseManagementEntryAction;
import org.oscarehr.casemgmt.web.NoteDisplay;
import org.oscarehr.casemgmt.web.NoteDisplayLocal;
import org.oscarehr.common.model.CaseManagementTmpSave;
import org.oscarehr.common.model.Provider;
import org.oscarehr.managers.ProgramManager2;
import org.oscarehr.managers.SecurityInfoManager;
import org.oscarehr.util.EncounterUtil;
import org.oscarehr.util.LoggedInInfo;
import org.oscarehr.util.MiscUtils;
import org.oscarehr.util.SpringUtils;
import org.oscarehr.ws.rest.to.GenericRESTResponse;
import org.oscarehr.ws.rest.to.TicklerNoteResponse;
import org.oscarehr.ws.rest.to.model.NoteSelectionTo1;
import org.oscarehr.ws.rest.to.model.NoteTo1;
import org.oscarehr.ws.rest.to.model.TicklerNoteTo1;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import oscar.OscarProperties;
import oscar.oscarEncounter.pageUtil.EctSessionBean;


@Path("/notes")
@Component("notesService")
public class NotesService extends AbstractServiceImpl {

	private static Logger logger = MiscUtils.getLogger();
	
	@Autowired
	private NoteService noteService; 
	
	@Autowired
	private ProgramManager2 programManager2;
	
	@Autowired
	private ProgramManager programMgr;
	
	@Autowired
	private CaseManagementManager caseManagementMgr;
	
	@Autowired
	private ProviderManager providerMgr;
	
	@Autowired
	private IssueDAO issueDao;
	
	@Autowired
	private SecurityInfoManager securityInfoManager;
	
	
	
	@POST
	@Path("/{demographicNo}/all")
	@Produces("application/json")
	@Consumes("application/json")
	public NoteSelectionTo1 getNotesWithFilter(@PathParam("demographicNo") Integer demographicNo ,@DefaultValue("20") @QueryParam("numToReturn") Integer numToReturn,@DefaultValue("0") @QueryParam("offset") Integer offset,JSONObject jsonobject){
		NoteSelectionTo1 returnResult = new NoteSelectionTo1();
		LoggedInInfo loggedInInfo = getLoggedInInfo();
		logger.debug("The config "+jsonobject.toString());
	
		HttpSession se = loggedInInfo.getSession();
		if (se.getAttribute("userrole") == null) {
			logger.error("An Error needs to be added to the returned result, remove this when fixed");
			return returnResult;
		}
		
		String demoNo = ""+demographicNo;

		logger.debug("is client in program");
		// need to check to see if the client is in our program domain
		// if not...don't show this screen!
		String roles = (String) se.getAttribute("userrole");
		if (OscarProperties.getInstance().isOscarLearning() && roles != null && roles.indexOf("moderator") != -1) {
			logger.info("skipping domain check..provider is a moderator");
		} else if (!caseManagementMgr.isClientInProgramDomain(loggedInInfo.getLoggedInProviderNo(), demoNo) && !caseManagementMgr.isClientReferredInProgramDomain(loggedInInfo.getLoggedInProviderNo(), demoNo)) {
			logger.error("A domain error needs to be added to the returned result, remove this when fixed");
			return returnResult;
		}
		
		ProgramProvider pp = programManager2.getCurrentProgramInDomain(getLoggedInInfo(),loggedInInfo.getLoggedInProviderNo());
		String programId = null;
		
		if(pp !=null && pp.getProgramId() != null){
			programId = ""+pp.getProgramId();
		}else{
			programId = String.valueOf(programMgr.getProgramIdByProgramName("OSCAR")); //Default to the oscar program if provider hasn't been assigned to a program
		}
		
		NoteSelectionCriteria criteria = new NoteSelectionCriteria();
		
		criteria.setMaxResults(numToReturn);
		criteria.setFirstResult(offset);
		
		criteria.setDemographicId(demographicNo);
		criteria.setUserRole((String) se.getAttribute("userrole"));
		criteria.setUserName((String) se.getAttribute("user"));
		
		// Note order is not user selectable in this version yet
		criteria.setNoteSort("observation_date_desc");  
		criteria.setSliceFromEndOfList(false);
				

		if (programId != null && !programId.trim().isEmpty()) {
			criteria.setProgramId(programId);
		}
		
		processJsonArray(jsonobject, "filterRoles", criteria.getRoles());
		
		processJsonArray(jsonobject, "filterProviders", criteria.getProviders());
		
		processJsonArray(jsonobject, "filterIssues", criteria.getIssues());
		
		if (logger.isDebugEnabled()) {
			logger.debug("SEARCHING FOR NOTES WITH CRITERIA: " + criteria);
		}
		
		NoteSelectionResult result = noteService.findNotes(loggedInInfo,criteria);
		
		if (logger.isDebugEnabled()) {
			logger.debug("FOUND: " + result);
			for(NoteDisplay nd : result.getNotes()) {
				logger.debug("   " + nd.getClass().getSimpleName() + " " + nd.getNoteId() + " " + nd.getNote());
			}
		}
		
		
		
		returnResult.setMoreNotes(result.isMoreNotes());
		List<NoteTo1> noteList = returnResult.getNotelist();
		for(NoteDisplay nd : result.getNotes()) {
			NoteTo1 note = new NoteTo1();
			note.setNoteId(nd.getNoteId());
			
			note.setIsSigned(nd.isSigned());
			note.setIsEditable(nd.isEditable());
			note.setObservationDate(nd.getObservationDate());
			note.setRevision(nd.getRevision());
			note.setUpdateDate(nd.getUpdateDate());
			note.setProviderName(nd.getProviderName());
			note.setProviderNo(nd.getProviderNo());
			note.setStatus(nd.getStatus());
			note.setProgramName(nd.getProgramName());
			note.setLocation(nd.getLocation());
			note.setRoleName(nd.getRoleName());
			note.setRemoteFacilityId(nd.getRemoteFacilityId());
			note.setUuid(nd.getUuid());
			note.setHasHistory(nd.getHasHistory());
			note.setLocked(nd.isLocked());
			note.setNote(nd.getNote());
			note.setDocument(nd.isDocument());
			note.setRxAnnotation(nd.isRxAnnotation());
			note.setEformData(nd.isEformData());
			note.setEncounterForm(nd.isEncounterForm());
			note.setInvoice(nd.isInvoice());
			note.setTicklerNote(nd.isTicklerNote());
			note.setEncounterType(nd.getEncounterType());
			note.setEditorNames(nd.getEditorNames());
			note.setIssueDescriptions(nd.getIssueDescriptions());
			note.setReadOnly(nd.isReadOnly());
			note.setGroupNote(nd.isGroupNote());
			note.setCpp(nd.isCpp());
			note.setEncounterTime(nd.getEncounterTime());	
			note.setEncounterTransportationTime(nd.getEncounterTransportationTime());
			
			noteList.add(note);
		}
		logger.debug("returning note list size "+noteList.size() +"  numToReturn was "+numToReturn+" offset "+offset );
		
		return returnResult;
	}
	
	
	
	@POST
	@Path("/{demographicNo}/tmpSave")
	@Consumes("application/json")
	@Produces("application/json")
	public NoteTo1 tmpSaveNote(@PathParam("demographicNo") Integer demographicNo ,NoteTo1 note){
		logger.debug("autosave "+note);

		LoggedInInfo loggedInInfo = getLoggedInInfo();//  LoggedInInfo.loggedInInfo.get();
		String providerNo=loggedInInfo.getLoggedInProvider().getProviderNo();

		
		String programId = getProgram(loggedInInfo,providerNo);
		String noteStr = note.getNote();
		String noteId  = ""+note.getNoteId();
		
		try{  
			Integer.parseInt(noteId);
		}catch(Exception e){
			noteId = null;
		}
		
		/* NOT SURE HOW TO HANDLE LOCKS YET!!
		//compare locks and see if they are the same
		CasemgmtNoteLock casemgmtNoteLockSession = (CasemgmtNoteLock)request.getSession().getAttribute("casemgmtNoteLock"+demographicNo);
		try {
			//if other window has acquired lock don't save
			CasemgmtNoteLock casemgmtNoteLock = casemgmtNoteLockDao.find(casemgmtNoteLockSession.getId());
			if( !casemgmtNoteLock.getSessionId().equals(casemgmtNoteLockSession.getSessionId()) ) {
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
				return null;
			}
		}
		catch(Exception e ) {
			//Exception thrown if other window has saved and exited so lock is gone
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			return null;

		}		
		*/
		if (noteStr == null || noteStr.length() == 0) {
			return null;
		}		
		
		//delete from tmp save and then add another
		try {
			caseManagementMgr.deleteTmpSave(providerNo, ""+demographicNo, programId);
			caseManagementMgr.tmpSave(providerNo, ""+demographicNo, programId, noteId, noteStr);
		} catch (Throwable e) {
			logger.error("AutoSave Error: ", e);
		}

		return note;
	}
	
	private String getProgram(LoggedInInfo loggedInInfo,String providerNo){
		ProgramProvider pp = programManager2.getCurrentProgramInDomain(loggedInInfo,providerNo);
		String programId = null;
		
		if(pp !=null && pp.getProgramId() != null){
			programId = ""+pp.getProgramId();
		}else{
			programId = String.valueOf(programMgr.getProgramIdByProgramName("OSCAR")); //Default to the oscar program if provider hasn't been assigned to a program
		}
		return programId;
	}
	
	
	@POST
	@Path("/{demographicNo}/save")
	@Consumes("application/json")
	@Produces("application/json")
	public NoteTo1 saveNote(@PathParam("demographicNo") Integer demographicNo ,NoteTo1 note) throws Exception{
		logger.debug("saveNote "+note);
		LoggedInInfo loggedInInfo = getLoggedInInfo(); //LoggedInInfo.loggedInInfo.get();
		String providerNo=loggedInInfo.getLoggedInProviderNo();
		Provider provider = loggedInInfo.getLoggedInProvider();
		String userName = provider != null ? provider.getFullName() : "";

		String demo = ""+demographicNo;
		
		CaseManagementNote caseMangementNote  = new CaseManagementNote();
		
		caseMangementNote.setDemographic_no(demo);
		caseMangementNote.setProvider(provider);
		caseMangementNote.setProviderNo(providerNo);
		
		if(note.getUuid() != null && !note.getUuid().trim().equals("")){
			caseMangementNote.setUuid(note.getUuid());
		}
		
		String noteTxt = note.getNote();
		noteTxt = org.apache.commons.lang.StringUtils.trimToNull(noteTxt);
		if (noteTxt == null || noteTxt.equals("")) return null;

		caseMangementNote.setNote(noteTxt);
		
		CaseManagementCPP cpp = this.caseManagementMgr.getCPP(demo);
		if (cpp == null) {
			cpp = new CaseManagementCPP();
			cpp.setDemographic_no(demo);
		}
		logger.debug("enc TYPE " +note.getEncounterType());
		caseMangementNote.setEncounter_type(note.getEncounterType());
		
		//caseMangementNote.setHourOfEncounterTime(note.getEncounterTime());
		logger.debug("this is what the encounter time was "+note.getEncounterTime());
		/*String hourOfEncounterTime = request.getParameter("hourOfEncounterTime");
		if (hourOfEncounterTime != null && hourOfEncounterTime != "") {
			note.setHourOfEncounterTime(Integer.valueOf(hourOfEncounterTime));
		}

		String minuteOfEncounterTime = request.getParameter("minuteOfEncounterTime");
		if (minuteOfEncounterTime != null && minuteOfEncounterTime != "") {
			note.setMinuteOfEncounterTime(Integer.valueOf(minuteOfEncounterTime));
		}*/

		logger.debug("this is what the encounter time was "+note.getEncounterTransportationTime());
		/*
		String hourOfEncTransportationTime = request.getParameter("hourOfEncTransportationTime");
		if (hourOfEncTransportationTime != null && hourOfEncTransportationTime != "") {
			note.setHourOfEncTransportationTime(Integer.valueOf(hourOfEncTransportationTime));
		}

		String minuteOfEncTransportationTime = request.getParameter("minuteOfEncTransportationTime");
		if (minuteOfEncTransportationTime != null && minuteOfEncTransportationTime != "") {
			note.setMinuteOfEncTransportationTime(Integer.valueOf(minuteOfEncTransportationTime));
		}
		*/
		//Need to check some how that if a note is signed that it must stay signed, currently this is done in the interface where the save button is not available.
		if(note.getIsSigned()){
			caseMangementNote.setSigning_provider_no(providerNo);
			caseMangementNote.setSigned(true);
		} else {
			caseMangementNote.setSigning_provider_no("");
			caseMangementNote.setSigned(false);
		}
		
		caseMangementNote.setProviderNo(providerNo);
		if (provider != null) caseMangementNote.setProvider(provider);

		//note.getPro
		String programIdString = getProgram(loggedInInfo,providerNo); //might not to convert it.
		Integer programId = null;
		try {
			programId = Integer.parseInt(programIdString);
		} catch (Exception e) {
			logger.warn("Error parsing programId:" + programIdString, e);
		}
		caseMangementNote.setProgram_no(programIdString);
		
		// get the checked issue save into note 
		// this goes into the database casemgmt_issue table
		List<CaseManagementIssue> issuelist = new ArrayList<CaseManagementIssue>();
		
		//CheckBoxBean[] checkedlist = sessionFrm.getIssueCheckList();

		// this gets attached to the CaseManagementNote object
		Set<CaseManagementIssue> issueset = new HashSet<CaseManagementIssue>();
		// wherever this is populated, it's not here...
		Set<CaseManagementNote> noteSet = new HashSet<CaseManagementNote>();
		String ongoing = new String();
		//ongoing = saveCheckedIssues_newCme(request, demo, note, issuelist, checkedlist, issueset, noteSet, ongoing);
		
		caseMangementNote.setIssues(issueset);

		// remove signature and the related issues from note 
		String noteString = note.getNote();
		// noteString = removeSignature(noteString);
		// noteString = removeCurrentIssue(noteString);
		caseMangementNote.setNote(noteString);
		
		/* Not sure how to handle this
		// add issues into notes 
		String includeIssue = request.getParameter("includeIssue");
		if (includeIssue == null || !includeIssue.equals("on")) {
			// set includeissue in note 
			note.setIncludeissue(false);
			sessionFrm.setIncludeIssue("off");
		} else {
			note.setIncludeissue(true);
			// add the related issues to note

			String issueString = new String();
			issueString = createIssueString(issueset);
			// insert the string before signiture

			int index = noteString.indexOf("\n[[");
			if (index >= 0) {
				String begString = noteString.substring(0, index);
				String endString = noteString.substring(index + 1);
				note.setNote(begString + issueString + endString);
			} else {
				note.setNote(noteString + issueString);
			}
		}
		*/
		
		// update appointment and add verify message to note if verified
		boolean verify = false;
		if(note.getIsVerified()){
			verify = true;
		}
		
		
		// update password
		/*
		String passwd = cform.getCaseNote().getPassword();
		if (passwd != null && passwd.trim().length() > 0) {
			note.setPassword(passwd);
			note.setLocked(true);
		}
		 */
		Date now = new Date();
		
		Date observationDate = note.getObservationDate();
		if (observationDate != null && !observationDate.equals("")) {
			if (observationDate.getTime() > now.getTime()) {
				//request.setAttribute("DateError", props.getString("oscarEncounter.futureDate.Msg"));
				caseMangementNote.setObservation_date(now);
			} else{
				caseMangementNote.setObservation_date(observationDate);
			}
		} else if (note.getObservationDate() == null) {
			caseMangementNote.setObservation_date(now);
		}
		
		caseMangementNote.setUpdate_date(now);
		
		/* Currently not available from this method
		// Checks whether the user can set the program via the UI - if so, make sure that they can't screw it up if they do
				if (OscarProperties.getInstance().getBooleanProperty("note_program_ui_enabled", "true")) {
					String noteProgramNo = request.getParameter("_note_program_no");
					String noteRoleId = request.getParameter("_note_role_id");

					if (noteProgramNo != null && noteRoleId != null && noteProgramNo.trim().length() > 0 && noteRoleId.trim().length() > 0) {
						if (noteProgramNo.equalsIgnoreCase("-2") || noteRoleId.equalsIgnoreCase("-2")) {
							throw new Exception("Patient is not admitted to any programs user has access to. [roleId=-2, programNo=-2]");
						} else if (!noteProgramNo.equalsIgnoreCase("-1") && !noteRoleId.equalsIgnoreCase("-1")) {
							note.setProgram_no(noteProgramNo);
							note.setReporter_caisi_role(noteRoleId);
						}
					} else {
								throw new Exception("Missing role id or program number. [roleId=" + noteRoleId + ", programNo=" + noteProgramNo + "]");
					}
				}
		 	*/
		
			
		//if (note.getAppointmentNo() != null) {
		caseMangementNote.setAppointmentNo(note.getAppointmentNo());
		//}
					
		// Save annotation 

		CaseManagementNote annotationNote = null;// (CaseManagementNote) session.getAttribute(attrib_name);

		//String ongoing = null; // figure out this
		String lastSavedNoteString = null;
		String user = loggedInInfo.getLoggedInProvider().getProviderNo();
		String remoteAddr = 	""; // Not sure how to get this	
		caseMangementNote = caseManagementMgr.saveCaseManagementNote(caseMangementNote,issuelist, cpp, ongoing,verify, loggedInInfo.getLocale(),now,annotationNote,userName,user,remoteAddr, lastSavedNoteString) ;
			
		caseManagementMgr.getEditors(caseMangementNote);
		
			
		
		note.setNoteId(Integer.parseInt(""+caseMangementNote.getId()));
		note.setUuid(caseMangementNote.getUuid());
		note.setUpdateDate(caseMangementNote.getUpdate_date());
		note.setObservationDate(caseMangementNote.getObservation_date());
		logger.error("note should return like this " + note.getNote() );
		return note;
		
		
		/*
		//update lock to new note id
		casemgmtNoteLockSession.setNoteId(note.getId());
		logger.info("UPDATING NOTE ID in LOCK");
		casemgmtNoteLockDao.merge(casemgmtNoteLockSession);
		session.setAttribute("casemgmtNoteLock"+demo, casemgmtNoteLockSession);	
		*/


		/*
		String sessionFrmName = "caseManagementEntryForm" + demo;
		CaseManagementEntryFormBean sessionFrm = (CaseManagementEntryFormBean) session.getAttribute(sessionFrmName);
		
		CasemgmtNoteLock casemgmtNoteLockSession = (CasemgmtNoteLock)session.getAttribute("casemgmtNoteLock"+demo);				
		
		try {
			
			if(casemgmtNoteLockSession == null) {
				throw new Exception("SESSION CASEMANAGEMENT NOTE LOCK OBJECT IS NULL");
			}
			
			CasemgmtNoteLock casemgmtNoteLock = casemgmtNoteLockDao.find(casemgmtNoteLockSession.getId());
			//if other window has acquired lock we reject save									
			if( !casemgmtNoteLock.getSessionId().equals(casemgmtNoteLockSession.getSessionId()) || !request.getRequestedSessionId().equals(casemgmtNoteLockSession.getSessionId()) ) {
				logger.info("DO NOT HAVE LOCK FOR " + demo + " PROVIDER " + providerNo + " CONTINUE SAVING LOCAL SESSION " + request.getRequestedSessionId() + " LOCAL IP " + request.getRemoteAddr() + " LOCK SESSION " + casemgmtNoteLockSession.getSessionId() + " LOCK IP " + casemgmtNoteLockSession.getIpAddress());
				return -1L;
			}
		}
		catch(Exception e ) {
			//Exception thrown if other window has saved and exited so lock is gone
			logger.error("Lock not found for " + demo + " provider " + providerNo + " IP " + request.getRemoteAddr(), e);
			return -1L;
		}
		String lastSavedNoteString = (String) session.getAttribute("lastSavedNoteString");
		
		String strBeanName = "casemgmt_oscar_bean" + demo;
		EctSessionBean sessionBean = (EctSessionBean) session.getAttribute(strBeanName);

		return note.getId();
		*/
		
		
	}
	
	
	
	
	
	private String getString(JSONObject jsonobject,String key){
		if(jsonobject.containsKey(key)){
			return jsonobject.getString(key); 
		}
		return null;
	}
	
	@POST
	@Path("/{demographicNo}/getCurrentNote")
	@Consumes("application/json")
	@Produces("application/json")
	public NoteTo1 getCurrentNote(@PathParam("demographicNo") Integer demographicNo ,JSONObject jsonobject){
		logger.debug("getCurrentNote "+jsonobject);
		LoggedInInfo loggedInInfo =  getLoggedInInfo(); //LoggedInInfo.loggedInInfo.get();

		String providerNo=loggedInInfo.getLoggedInProviderNo();

		
		HttpSession session = loggedInInfo.getSession();
		if (session.getAttribute("userrole") == null) {
//			response.sendError(HttpServletResponse.SC_FORBIDDEN);
			return null;
		}

//		CaseManagementEntryFormBean cform = (CaseManagementEntryFormBean) form;
//		cform.setChain("");
//		request.setAttribute("change_flag", "false");
//		request.setAttribute("from", "casemgmt");

		

		String programIdString = getProgram(loggedInInfo,providerNo);
		Integer programId = null;
		try {
			programId = Integer.parseInt(programIdString);
		} catch (Exception e) {
			logger.warn("Error parsing programId:" + programIdString, e);
		}

///////Not sure what this is about??		
//		/* process the request from other module */
//		if (!"casemgmt".equalsIgnoreCase(request.getParameter("from"))) {
//
//			// no demographic number, no page
//			if (request.getParameter("demographicNo") == null || "".equals(request.getParameter("demographicNo"))) {
//				return mapping.findForward("NoDemoERR");
//			}
//			request.setAttribute("from", "");
//		}


		

		CaseManagementNote note = null;

		String nId = getString(jsonobject,"noteId");// request.getParameter("noteId");
		String forceNote = getString(jsonobject,"forceNote");//request.getParameter("forceNote");
		if (forceNote == null) forceNote = "false";

		logger.debug("NoteId " + nId);

		CaseManagementTmpSave tmpsavenote = this.caseManagementMgr.restoreTmpSave(providerNo, ""+demographicNo, programIdString);
		

		logger.debug("Get Note for editing");
		String strBeanName = "casemgmt_oscar_bean" + demographicNo;
		EctSessionBean bean = (EctSessionBean) loggedInInfo.getSession().getAttribute(strBeanName);
		String encType = getString(jsonobject,"encType");
		
		logger.debug("Encounter Type : "+encType);
		
		

		// create a new note
		if (getString(jsonobject,"note_edit") != null && getString(jsonobject,"note_edit").equals("new")) {
			logger.debug("NEW NOTE GENERATED");
//			request.setAttribute("newNoteIdx", request.getParameter("newNoteIdx"));

			note = new CaseManagementNote();
			note.setProviderNo(providerNo);
			Provider prov = new Provider();
			prov.setProviderNo(providerNo);
			note.setProvider(prov);
			note.setDemographic_no(""+demographicNo);

//////This adds the note text i think
//			if (!OscarProperties.getInstance().isPropertyActive("encounter.empty_new_note")) {
//				this.insertReason(request, note);
//			} else {
//				note.setNote("");
//				note.setEncounter_type("");
//			}

			

			if (encType == null || encType.equals("")) {
				note.setEncounter_type("");
			} else {
				note.setEncounter_type(encType);
			}
			if (bean.encType != null && bean.encType.length() > 0) {
				note.setEncounter_type(bean.encType);
			}

//			resetTemp(providerNo, ""+demographicNo, programIdString);

		}
		// get the last temp note?
		else if (tmpsavenote != null && !forceNote.equals("true")) {
			logger.debug("tempsavenote is NOT NULL == noteId :"+tmpsavenote.getNoteId());
			if (tmpsavenote.getNoteId() > 0) {
//				session.setAttribute("newNote", "false");
				note = caseManagementMgr.getNote(String.valueOf(tmpsavenote.getNoteId()));
				logger.debug("Restoring " + String.valueOf(note.getId()));
			} else {
				logger.debug("creating new note");
//				session.setAttribute("newNote", "true");
//				session.setAttribute("issueStatusChanged", "false");
				note = new CaseManagementNote();
				note.setProviderNo(providerNo);
				Provider prov = new Provider();
				prov.setProviderNo(providerNo);
				note.setProvider(prov);
				note.setDemographic_no(""+demographicNo);
			}
			
			note.setNote(tmpsavenote.getNote());
			logger.debug("Setting note to " + note.getNote());

		}
		// get an existing non-temp note?
		else if (nId != null && Integer.parseInt(nId) > 0) {
			logger.debug("Using nId " + nId + " to fetch note");
//			session.setAttribute("newNote", "false");
			note = caseManagementMgr.getNote(nId);

			if (note.getHistory() == null || note.getHistory().equals("")) {
				// old note - we need to save the original in here
				note.setHistory(note.getNote());

				caseManagementMgr.saveNoteSimple(note);
//				addNewNoteLink(Long.parseLong(nId));
			}

		}
		// no note specified, get last unsigned
		else {
			logger.debug("in empty else");
			// A hack to load last unsigned note when not specifying a particular note to edit
			// if there is no unsigned note load a new one
			
			Map unlockedNotesMap = null; //NEED THIS ??
			if ((note = caseManagementMgr.getLastSaved(""+programId, ""+demographicNo, providerNo,unlockedNotesMap)) == null) {
//				session.setAttribute("newNote", "true");
//				//session.setAttribute("issueStatusChanged", "false");
			
				//String encType 
				String apptDate = getString(jsonobject,"apptDate");
				String reason = getString(jsonobject,"reason");
				String appointmentNo = getString(jsonobject,"appointmentNo");
				note = caseManagementMgr.makeNewNote(providerNo, ""+demographicNo, encType, appointmentNo,loggedInInfo.getLocale());
				//note = caseManagementMgr.makeNewNote(providerNo, ""+demographicNo, bean, encType, apptDate, reason,loggedInInfo.locale);
//				note = this.makeNewNote(providerNo, demono, request);				
			}
		}
		

		/*
		 * do the restore if(restore != null && restore.booleanValue() == true) { String tmpsavenote = this.caseManagementMgr.restoreTmpSave(providerNo,demono,programId); if(tmpsavenote != null) { note.setNote(tmpsavenote); } }
		 */
		logger.debug("note ?" +note);
		logger.debug("Set Encounter Type: " + note.getEncounter_type());
		logger.debug("Fetched Note " + String.valueOf(note.getId()));

		logger.debug("Populate Note with editors");
		this.caseManagementMgr.getEditors(note);
		

		// put the new/retrieved not in the form object for rendering on page
		/* set issue checked list */

		// get issues for current demographic, based on provider rights


//		cform.setSign("off");
//		if (!note.isIncludeissue()) cform.setIncludeIssue("off");
//		else cform.setIncludeIssue("on");

//		boolean passwd = caseManagementMgr.getEnabled();
//		String chain = request.getParameter("chain");

		

//		LogAction.addLog((String) session.getAttribute("user"), LogConst.EDIT, LogConst.CON_CME_NOTE, String.valueOf(note.getId()), request.getRemoteAddr(), demono, note.getAuditString());

		//check to see if someone else is editing note in this chart
//		String ipAddress = request.getRemoteAddr();
//		CasemgmtNoteLock casemgmtNoteLock;
//		Long note_id = note.getId() != null && note.getId() >= 0 ? note.getId() : 0L;
//		casemgmtNoteLock = isNoteEdited(note_id, demographicNo, providerNo, ipAddress, request.getRequestedSessionId());
		
//		if( casemgmtNoteLock.isLocked() ) {
//			note = makeNewNote(providerNo, demono, request);
//			cform.setCaseNote(note);
//		}
		
//		session.setAttribute("casemgmtNoteLock"+demono, casemgmtNoteLock);		
		
		

		/*
		 ///Is it a specific thats being requested to edit

	      //YES  -- > load that note

	      //NO
	            //check to see if a note is in tmp-save? 
	                  //YES -->> load that tmp save note
	                  //NO 

	                        //Is there an unsigned note?
	                                //YES -->> load that unsigned save note
	                                //NO
	                                   //Is it a new note? What type?  -->> load the new note (ie visit note, tele note etc)
		 */
		
		NoteTo1 returnNote = new NoteTo1();
		
		NoteDisplay nd = new NoteDisplayLocal(loggedInInfo,note);
		
		returnNote.setNoteId(nd.getNoteId());
		
		returnNote.setIsSigned(nd.isSigned());
		returnNote.setIsEditable(nd.isEditable());
		returnNote.setObservationDate(nd.getObservationDate());
		returnNote.setRevision(nd.getRevision());
		returnNote.setUpdateDate(nd.getUpdateDate());
		returnNote.setProviderName(nd.getProviderName());
		returnNote.setProviderNo(nd.getProviderNo());
		returnNote.setStatus(nd.getStatus());
		returnNote.setProgramName(nd.getProgramName());
		returnNote.setLocation(nd.getLocation());
		returnNote.setRoleName(nd.getRoleName());
		returnNote.setRemoteFacilityId(nd.getRemoteFacilityId());
		returnNote.setUuid(nd.getUuid());
		returnNote.setHasHistory(nd.getHasHistory());
		returnNote.setLocked(nd.isLocked());
		returnNote.setNote(nd.getNote());
		returnNote.setDocument(nd.isDocument());
		returnNote.setRxAnnotation(nd.isRxAnnotation());
		returnNote.setEformData(nd.isEformData());
		returnNote.setEncounterForm(nd.isEncounterForm());
		returnNote.setInvoice(nd.isInvoice());
		returnNote.setTicklerNote(nd.isTicklerNote());
		returnNote.setEncounterType(nd.getEncounterType());
		returnNote.setEditorNames(nd.getEditorNames());
		returnNote.setIssueDescriptions(nd.getIssueDescriptions());
		returnNote.setReadOnly(nd.isReadOnly());
		returnNote.setGroupNote(nd.isGroupNote());
		returnNote.setCpp(nd.isCpp());
		returnNote.setEncounterTime(nd.getEncounterTime());	
		returnNote.setEncounterTransportationTime(nd.getEncounterTransportationTime());
		returnNote.setAppointmentNo(nd.getAppointmentNo());
		
		return returnNote;
	}
	
	
	
	
	
	private void processJsonArray( JSONObject jsonobject, String key, List<String> list){
		if( jsonobject != null && jsonobject.containsKey(key)){
			JSONArray arr = jsonobject.getJSONArray(key);
			for(int i =0; i < arr.size();i++){
				list.add(arr.getString(i));
			}
		}
	 
	}
	

	@GET
	@Path("/ticklerGetNote/{ticklerNo}")
	@Produces("application/json")
	//{"ticklerNote":{"editor":"oscardoc, doctor","note":"note 2","noteId":6,"observationDate":"2014-09-13T13:18:41-04:00","revision":2}}
	public TicklerNoteResponse ticklerGetNote(@PathParam("ticklerNo") Integer ticklerNo ){

		if(!securityInfoManager.hasPrivilege(getLoggedInInfo(), "_tickler", "r", null)) {
			throw new RuntimeException("Access Denied");
		}
		if(!securityInfoManager.hasPrivilege(getLoggedInInfo(), "_eChart", "r", null)) {
			throw new RuntimeException("Access Denied");
		}
		
		TicklerNoteResponse response = new TicklerNoteResponse();
		CaseManagementNoteLink link = caseManagementMgr.getLatestLinkByTableId(CaseManagementNoteLink.TICKLER, Long.valueOf(ticklerNo));
		
		if(link != null) {
			Long noteId = link.getNoteId();
			
			CaseManagementNote note = caseManagementMgr.getNote(noteId.toString());
			
			if(note != null) {
				TicklerNoteTo1 tNote = new TicklerNoteTo1();
				tNote.setNoteId(note.getId().intValue());
				tNote.setNote(note.getNote());
				tNote.setRevision(note.getRevision());
				tNote.setObservationDate(note.getObservation_date());
				tNote.setEditor(providerMgr.getProvider(note.getProviderNo()).getFormattedName());
				response.setTicklerNote(tNote);
			}
		}
		return response;
	}
	
	@POST
	@Path("/ticklerSaveNote")
	@Produces("application/json")
	@Consumes("application/json")
	public GenericRESTResponse ticklerSaveNote(JSONObject json){
		
		if(!securityInfoManager.hasPrivilege(getLoggedInInfo(), "_tickler", "w", null)) {
			throw new RuntimeException("Access Denied");
		}
		if(!securityInfoManager.hasPrivilege(getLoggedInInfo(), "_eChart", "w", null)) {
			throw new RuntimeException("Access Denied");
		}

		logger.info("The config "+json.toString());
		
		String strNote = json.getString("note");
		Integer noteId = json.getInt("noteId");
		
		logger.info("want to save note id " + noteId + " with value " + strNote);
		
		JSONObject tickler = json.getJSONObject("tickler");
		Integer ticklerId = tickler.getInt("id");
		Integer demographicNo = tickler.getInt("demographicNo");
		
		logger.info("tickler id " + ticklerId + ", demographicNo " + demographicNo);
		
		Date creationDate = new Date();
		LoggedInInfo loggedInInfo=this.getLoggedInInfo();
		Provider loggedInProvider = loggedInInfo.getLoggedInProvider();
		
		
		String revision = "1";
		String history = strNote;
		String uuid = null;
		
		if(noteId != null  && noteId.intValue()>0) {
			CaseManagementNote existingNote = caseManagementMgr.getNote(String.valueOf(noteId));
			
			revision = String.valueOf(Integer.valueOf(existingNote.getRevision()).intValue() + 1);
			history = strNote + "\n" + existingNote.getHistory();
			uuid = existingNote.getUuid();
		}
		
		CaseManagementNote cmn = new CaseManagementNote();
		cmn.setAppointmentNo(0);
		cmn.setArchived(false);
		cmn.setCreate_date(creationDate);
		cmn.setDemographic_no(String.valueOf(demographicNo));
		cmn.setEncounter_type(EncounterUtil.EncounterType.FACE_TO_FACE_WITH_CLIENT.getOldDbValue());
		cmn.setNote(strNote);
		cmn.setObservation_date(creationDate);
		
		cmn.setProviderNo(loggedInProvider.getProviderNo());
		cmn.setRevision(revision);
		cmn.setSigned(true);
		cmn.setSigning_provider_no(loggedInProvider.getProviderNo());
		cmn.setUpdate_date(creationDate);
		cmn.setHistory(history);
		//just doing this because the other code does it.
		cmn.setReporter_program_team("null");
		cmn.setUuid(uuid);
		
		
		ProgramProvider pp = programManager2.getCurrentProgramInDomain(getLoggedInInfo(),getLoggedInInfo().getLoggedInProviderNo());
		if(pp != null) {
			cmn.setProgram_no(String.valueOf(pp.getProgramId()));
		} else {
			List<ProgramProvider> ppList = programManager2.getProgramDomain(getLoggedInInfo(),getLoggedInInfo().getLoggedInProviderNo());
			if(ppList != null && ppList.size()>0) {
				cmn.setProgram_no(String.valueOf(ppList.get(0).getProgramId()));
			}
			
		}
		
		//weird place for it , but for now.
		CaseManagementEntryAction.determineNoteRole(cmn,loggedInProvider.getProviderNo(),String.valueOf(demographicNo));
		
		caseManagementMgr.saveNoteSimple(cmn);

		logger.info("note id is " + cmn.getId());
		
		
		//save link, so we know what tickler this note is linked to
		CaseManagementNoteLink link = new CaseManagementNoteLink();
		link.setNoteId(cmn.getId());
		link.setTableId(ticklerId.longValue());
		link.setTableName(CaseManagementNoteLink.TICKLER);
		
		CaseManagementNoteLinkDAO caseManagementNoteLinkDao = (CaseManagementNoteLinkDAO) SpringUtils.getBean("CaseManagementNoteLinkDAO");
		caseManagementNoteLinkDao.save(link);
		
		
		
		Issue issue = this.issueDao.findIssueByTypeAndCode("system", "TicklerNote");
		if(issue == null) {
			logger.warn("missing TicklerNote issue, please run all database updates");
			return null;
		}
		
		CaseManagementIssue cmi = caseManagementMgr.getIssueById(demographicNo.toString(), issue.getId().toString());
		
		if(cmi == null) {
		//save issue..this will make it a "cpp looking" issue in the eChart
			cmi = new CaseManagementIssue();
			cmi.setAcute(false);
			cmi.setCertain(false);
			cmi.setDemographic_no(String.valueOf(demographicNo));
			cmi.setIssue_id(issue.getId());
			cmi.setMajor(false);
			cmi.setProgram_id(Integer.parseInt(cmn.getProgram_no()));
			cmi.setResolved(false);
			cmi.setType(issue.getRole());
			cmi.setUpdate_date(creationDate);
			
			caseManagementMgr.saveCaseIssue(cmi);
			
		}

		cmn.getIssues().add(cmi);
		caseManagementMgr.updateNote(cmn);
		
		 
		
		return new GenericRESTResponse();
	}

}
