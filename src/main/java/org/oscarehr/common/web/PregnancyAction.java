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
package org.oscarehr.common.web;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.lang.WordUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.actions.DispatchAction;
import org.apache.struts.util.LabelValueBean;
import org.oscarehr.common.dao.AbstractCodeSystemDao;
import org.oscarehr.common.dao.AllergyDao;
import org.oscarehr.common.dao.DrugDao;
import org.oscarehr.common.dao.EpisodeDao;
import org.oscarehr.common.dao.MeasurementDao;
import org.oscarehr.common.model.AbstractCodeSystemModel;
import org.oscarehr.common.model.Allergy;
import org.oscarehr.common.model.Drug;
import org.oscarehr.common.model.Episode;
import org.oscarehr.common.model.Measurement;
import org.oscarehr.util.LoggedInInfo;
import org.oscarehr.util.MiscUtils;
import org.oscarehr.util.SpringUtils;

import oscar.form.FrmLabReq07Record;
import oscar.form.FrmONAREnhancedRecord;
import oscar.form.FrmRecord;
import oscar.form.FrmRecordFactory;
import oscar.log.LogAction;
import oscar.log.LogConst;
import oscar.login.DBHelp;

public class PregnancyAction extends DispatchAction {

	private EpisodeDao episodeDao = SpringUtils.getBean(EpisodeDao.class);

	public static Integer getLatestFormIdByPregnancy(Integer episodeId) {
		String sql = "SELECT id from formONAREnhanced WHERE episodeId="+episodeId+" ORDER BY formEdited DESC";                
        ResultSet rs = DBHelp.searchDBRecord(sql);
        try {
	        if(rs.next()) {
	        	Integer id = rs.getInt("id");
	        	return id;
	        }
        }catch(SQLException e) {
        	MiscUtils.getLogger().error("Error",e);
        	return 0;
        }
		return 0;
	}
	
	public ActionForward create(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response)  {
		Integer demographicNo = Integer.parseInt(request.getParameter("demographicNo"));
		String code = request.getParameter("code");
		String codeType = request.getParameter("codetype");
		
		//check for an existing pregnancy
		List<String> codes = new ArrayList<String>();
		codes.add("72892002");
		codes.add("47200007");
		codes.add("16356006");
		codes.add("34801009");
		List<Episode> existingEpisodes = episodeDao.findCurrentByCodeTypeAndCodes(demographicNo,"SnomedCore",codes);
		if(existingEpisodes.size() > 0) {
			request.setAttribute("error","There is already a pregnancy in progress. Please close the existing one before creating a new one.");
			return mapping.findForward("success");
		}
		
		AbstractCodeSystemDao dao = (AbstractCodeSystemDao)SpringUtils.getBean(WordUtils.uncapitalize(codeType) + "Dao");
		AbstractCodeSystemModel mod = dao.findByCode(code);

		if(mod == null) {
			request.setAttribute("error","There was an internal error processing this request, please contact your system administrator");
			return mapping.findForward("success");
		}
		
		//create pregnancy episode
		Episode e = new Episode();
		e.setCode(code);
		e.setCodingSystem(codeType);
		e.setDemographicNo(demographicNo);
		e.setDescription("");
		e.setLastUpdateTime(new Date());
		e.setLastUpdateUser(LoggedInInfo.loggedInInfo.get().loggedInProvider.getProviderNo());
		e.setStatus("Current");
		e.setStartDate(new Date());
		e.setDescription(mod.getDescription());
		episodeDao.persist(e);
		
		//start up a new ar on enhanced form
		try {
			FrmONAREnhancedRecord f = new FrmONAREnhancedRecord();
			Properties p = f.getFormRecord(demographicNo, 0);
			p.setProperty("episodeId", String.valueOf(e.getId()));
			f.saveFormRecord(p);
		}catch(SQLException ee) {
			MiscUtils.getLogger().error("Error",ee);
		}
		
		return mapping.findForward("success");
	}

	public ActionForward complete(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response)  {
		Integer episodeId = Integer.parseInt(request.getParameter("episodeId"));
		Episode e = episodeDao.find(episodeId);
		if(e == null) {			
			request.setAttribute("error","There was an internal error. Please contact tech support.");			
		}
		request.setAttribute("episode",e);
		return mapping.findForward("complete");
	}
	
	public ActionForward doComplete(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response)  {
		//Integer demographicNo = Integer.parseInt(request.getParameter("demographicNo"));
		Integer episodeId = Integer.parseInt(request.getParameter("episodeId"));
		String endDate = request.getParameter("endDate");
		String notes = request.getParameter("notes");
		Episode e = episodeDao.find(episodeId);
		if(e != null) {
			e.setStatus("Complete");
			e.setEndDateStr(endDate);
			e.setNotes(notes);
			episodeDao.merge(e); 
			request.setAttribute("close", true);
		} else {
			request.setAttribute("error","There was an internal error. Please contact tech support.");
			return mapping.findForward("complete");
		}
		return mapping.findForward("complete");
	}
	
	public ActionForward doDelete(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response)  {
		//Integer demographicNo = Integer.parseInt(request.getParameter("demographicNo"));
		Integer episodeId = Integer.parseInt(request.getParameter("episodeId"));
		Episode e = episodeDao.find(episodeId);
		String notes = request.getParameter("notes");
		if(e != null) {
			e.setNotes(notes);
			e.setStatus("Deleted");
			e.setEndDate(new Date());
			episodeDao.merge(e); 
			request.setAttribute("close", true);
		} else {
			request.setAttribute("error","There was an internal error. Please contact tech support.");
			return mapping.findForward("complete");
		}
		return mapping.findForward("complete");
	}
	
	public ActionForward list(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response)  {
		Integer demographicNo = Integer.parseInt(request.getParameter("demographicNo"));
		
		List<String> codes = new ArrayList<String>();
		codes.add("72892002");
		codes.add("47200007");
		codes.add("16356006");
		codes.add("34801009");
		List<Episode> episodes = episodeDao.findCurrentByCodeTypeAndCodes(demographicNo,"SnomedCore",codes);
		List<Episode> episodes2 = episodeDao.findCompletedByCodeTypeAndCodes(demographicNo,"SnomedCore",codes);
		episodes.addAll(episodes2);
		request.setAttribute("episodes",episodes);
		return mapping.findForward("list");
	}
	
	public ActionForward createGBSLabReq(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response)  throws SQLException {
		Integer demographicNo = Integer.parseInt(request.getParameter("demographicNo"));
		String penicillin = request.getParameter("penicillin");
		
		FrmLabReq07Record lr = new FrmLabReq07Record();
		Properties p = lr.getFormRecord(demographicNo, 0);
		p = lr.getFormCustRecord(p, LoggedInInfo.loggedInInfo.get().loggedInProvider.getProviderNo());
		if(penicillin != null && penicillin.equals("checked")) {
			p.setProperty("o_otherTests1","Vaginal Anal GBS with sensitivities (pt allergic to penicillin)");
		} else {
			p.setProperty("o_otherTests1","Vaginal Anal GBS");
		}
		
		//int recId = lr.saveFormRecord(p);
		request.getSession().setAttribute("labReq07"+demographicNo,p);
	
		return null;
	}

	public ActionForward createMCVLabReq(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response)  throws SQLException {
		Integer demographicNo = Integer.parseInt(request.getParameter("demographicNo"));
		String ferritin = request.getParameter("ferritin");
		String hbElectrophoresis = request.getParameter("hb_electrophoresis");
				
		FrmLabReq07Record lr = new FrmLabReq07Record();
		Properties p = lr.getFormRecord(demographicNo, 0);
		p = lr.getFormCustRecord(p, LoggedInInfo.loggedInInfo.get().loggedInProvider.getProviderNo());
		
		if(ferritin != null && ferritin.equals("checked")) {
			p.setProperty("b_ferritin","checked=\"checked\"");			
		} 
		if(hbElectrophoresis != null && hbElectrophoresis.equals("checked")) {
			p.setProperty("o_otherTests1", "Hb Electrophoresis");
		} 
		
		request.getSession().setAttribute("labReq07"+demographicNo,p);
	
		return null;
	}
	
	public ActionForward getAllergies(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response)  throws IOException {
		Integer demographicNo = Integer.parseInt(request.getParameter("demographicNo"));
		AllergyDao allergyDao = SpringUtils.getBean(AllergyDao.class);
		List<Allergy> allergies = allergyDao.findActiveAllergies(demographicNo);
		StringBuilder output = new StringBuilder();
		for(Allergy allergy:allergies) {
			output.append(allergy.getDescription() + ":" + allergy.getReaction() + "\n");
		}
		
		JSONObject json = JSONObject.fromObject(new LabelValueBean("allergies",output.toString().trim()));
		response.getWriter().println(json);
		return null;
	}
	
	public ActionForward getMeds(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response)  throws IOException {
		Integer demographicNo = Integer.parseInt(request.getParameter("demographicNo"));
		DrugDao drugDao = SpringUtils.getBean(DrugDao.class);
		List<Drug> drugs = drugDao.findByDemographicId(demographicNo, false);
		StringBuilder output = new StringBuilder();
		for(Drug drug:drugs) {
			output.append(drug.getSpecial().replaceAll("\n", " ") + "\n");			
		}
		
		JSONObject json = JSONObject.fromObject(new LabelValueBean("meds",output.toString().trim()));
		response.getWriter().println(json);
		return null;
	}
	
	public ActionForward saveFormAjax(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws IOException {
        int newID = 0;
        FrmRecord rec = null;
        JSONObject jsonObj = null;
      
        try {
            FrmRecordFactory recorder = new FrmRecordFactory();
            rec = recorder.factory(request.getParameter("form_class"));
            Properties props = new Properties();
                 
            boolean bMulPage = request.getParameter("c_lastVisited") != null ? true : false;
            String name;

            if (bMulPage) {
                String curPageNum = request.getParameter("c_lastVisited");
                String commonField = request.getParameter("commonField") != null ? request
                        .getParameter("commonField") : "&'";
                curPageNum = curPageNum.length() > 3 ? ("" + curPageNum.charAt(0)) : curPageNum;

                //copy an old record
                props = rec.getFormRecord(Integer.parseInt(request.getParameter("demographic_no")), Integer
                        .parseInt(request.getParameter("formId")));

                //empty the current page
                Properties currentParam = new Properties();
                for (Enumeration varEnum = request.getParameterNames(); varEnum.hasMoreElements();) {
                    name = (String) varEnum.nextElement();
                    currentParam.setProperty(name, "");
                }
                for (Enumeration varEnum = props.propertyNames(); varEnum.hasMoreElements();) {
                    name = (String) varEnum.nextElement();
                    // kick off the current page elements, commonField on the current page
                    if (name.startsWith(curPageNum + "_")
                            || (name.startsWith(commonField) && currentParam.containsKey(name))) {
                        props.remove(name);
                    }
                }
            }

            //update the current record
            for (Enumeration varEnum = request.getParameterNames(); varEnum.hasMoreElements();) {
                name = (String) varEnum.nextElement();                    
                props.setProperty(name, request.getParameter(name));                    
            }

            props.setProperty("provider_no", (String) request.getSession().getAttribute("user"));
            newID = rec.saveFormRecord(props);
            String ip = request.getRemoteAddr();
            LogAction.addLog((String) request.getSession().getAttribute("user"), LogConst.ADD, request
                    .getParameter("form_class"), "" + newID, ip,request.getParameter("demographic_no"));

            
            jsonObj = JSONObject.fromObject(new LabelValueBean("result",String.valueOf(newID)));
            

        } catch (Exception ex) {
           MiscUtils.getLogger().error("error",ex);
           jsonObj = JSONObject.fromObject(new LabelValueBean("result","error"));
           
        }

        response.getWriter().print(jsonObj.toString());
        
		return null;
	}
	
	public ActionForward getMeasurementsAjax(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws IOException {
		String demographicNo = request.getParameter("demographicNo");
		String type = request.getParameter("type");
		
		MeasurementDao md = SpringUtils.getBean(MeasurementDao.class);
		List<Measurement> m = md.findByType(Integer.parseInt(demographicNo), type);
		
		JSONArray json = JSONArray.fromObject(m);
		response.getWriter().print(json.toString());
		
	    return null;
	}
	
	public ActionForward saveMeasurementAjax(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws IOException {
		String demographicNo = request.getParameter("demographicNo");
		String type = request.getParameter("type");
		String value = request.getParameter("value");
		
		MeasurementDao md = SpringUtils.getBean(MeasurementDao.class);
		
		Measurement m = new Measurement();
		m.setAppointmentNo(0);
		m.setComments("");
		m.setDataField(value);
		m.setDateObserved(new Date());
		m.setDemographicId(Integer.parseInt(demographicNo));
		m.setMeasuringInstruction("");
		m.setProviderNo(LoggedInInfo.loggedInInfo.get().loggedInProvider.getProviderNo());
		m.setType(type);
		
		md.persist(m);
		
		JSONObject jsonObj = JSONObject.fromObject(new LabelValueBean("result","success"));
		response.getWriter().print(jsonObj);
		
	    return null;
	}
	
	/*
	 * Tests, and the LOINC code, I could find for it
	 * 
	 * Hemoglobin (718-7) 
	 * HIV (public health - manually entered for now) (X50045) - hiv serology (GDML:HIV)
	 * MCV (787-2)
	 * ABO - GDML (4490) test name is "Blood Group" - includes RH. Textual though
	 * RH (10331-7)
	 * PAP Smear (GDML:GY04)
	 * Antibody screen (8061-4)?? (GDML:4482) 
	 * Gonnorhea
	 * Rubella (25514-1)?
	 * Chlamydia - urine GDML:CHLD
	 * HbsAG (5196-1) GDML:HB1
	 * Urine C&S
	 * VDRL (public health) (X100666)
	 * Sickle Cell
	 */
	public ActionForward getAR1Labs(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws IOException {
		String demographicNo = request.getParameter("demographicNo");
	
		JSONArray json = new JSONArray();
		
		MeasurementDao md = SpringUtils.getBean(MeasurementDao.class);
		List<Measurement> m = md.findByType(Integer.parseInt(demographicNo), "HEMO");
		if(m.size()>0) {
			json.add(m.get(0));
		}
		m = md.findByType(Integer.parseInt(demographicNo), "MCV");
		if(m.size()>0) {
			json.add(m.get(0));
		}
		
		response.getWriter().print(json.toString());
		
	    return null;
	}
}