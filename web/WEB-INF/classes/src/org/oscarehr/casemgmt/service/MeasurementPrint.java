package org.oscarehr.casemgmt.service;


import java.io.IOException;
import java.util.Date;

import org.apache.log4j.Logger;
import org.oscarehr.casemgmt.util.ExtPrint;
import org.oscarehr.util.MiscUtils;

import com.lowagie.text.Chunk;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;

public class MeasurementPrint implements ExtPrint {

	private static Logger logger = MiscUtils.getLogger();
	
	
	@Override
	public void printExt(CaseManagementPrintPdf engine) throws IOException, DocumentException{
		logger.info("measurement print!!!!");
		if( engine.getNewPage() )
            engine.getDocument().newPage();
        else
            engine.setNewPage(true);
        
        Font obsfont = new Font(engine.getBaseFont(), engine.FONTSIZE, Font.UNDERLINE);                
       
        
        Paragraph p = new Paragraph();
        p.setAlignment(Paragraph.ALIGN_CENTER);
        Phrase phrase = new Phrase(engine.LEADING, "\n\n", engine.getFont());
        p.add(phrase);
        phrase = new Phrase(engine.LEADING, "Measurements", obsfont);        
        p.add(phrase);
        engine.getDocument().add(p);
        
        //go through each appt in reverge chronological order, and print the measurements
        p = new Paragraph();
        phrase = new Phrase(engine.LEADING, "", engine.getFont());              
        Chunk chunk = new Chunk("Documentation Date: " + engine.getFormatter().format(new Date()) + "\n", obsfont);
        phrase.add(chunk);                    
        p.add(phrase);
      
        engine.getDocument().add(p);
        
	}

}