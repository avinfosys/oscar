//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vhudson-jaxb-ri-2.1-558 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2011.06.03 at 01:48:51 AM EDT 
//


package org.oscarehr.hospitalReportManager.xsd;

import java.math.BigInteger;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;


/**
 * <p>Java class for hypoglycemicEpisodes complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="hypoglycemicEpisodes">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="NumOfReportedEpisodes" type="{http://www.w3.org/2001/XMLSchema}integer"/>
 *         &lt;element name="Date" type="{cds_dt}dateYYYYMMDD"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "hypoglycemicEpisodes", propOrder = {
    "numOfReportedEpisodes",
    "date"
})
public class HypoglycemicEpisodes {

    @XmlElement(name = "NumOfReportedEpisodes", required = true)
    protected BigInteger numOfReportedEpisodes;
    @XmlElement(name = "Date", required = true)
    protected XMLGregorianCalendar date;

    /**
     * Gets the value of the numOfReportedEpisodes property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getNumOfReportedEpisodes() {
        return numOfReportedEpisodes;
    }

    /**
     * Sets the value of the numOfReportedEpisodes property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setNumOfReportedEpisodes(BigInteger value) {
        this.numOfReportedEpisodes = value;
    }

    /**
     * Gets the value of the date property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getDate() {
        return date;
    }

    /**
     * Sets the value of the date property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setDate(XMLGregorianCalendar value) {
        this.date = value;
    }

}