//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vhudson-jaxb-ri-2.1-2 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2018.07.04 at 02:49:47 PM CEST 
//


package it.gov.digitpa.schemas._2011.psp;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ctInfoContoDiAccreditoPair complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ctInfoContoDiAccreditoPair">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="ibanAccredito" type="{}stIBANIdentifier"/>
 *         &lt;element name="idBancaSeller" type="{}stSellerBankIdentifier"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ctInfoContoDiAccreditoPair", propOrder = {
    "ibanAccredito",
    "idBancaSeller"
})
public class CtInfoContoDiAccreditoPair {

    @XmlElement(required = true)
    protected String ibanAccredito;
    @XmlElement(required = true)
    protected String idBancaSeller;

    /**
     * Gets the value of the ibanAccredito property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getIbanAccredito() {
        return this.ibanAccredito;
    }

    /**
     * Sets the value of the ibanAccredito property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setIbanAccredito(String value) {
        this.ibanAccredito = value;
    }

    /**
     * Gets the value of the idBancaSeller property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getIdBancaSeller() {
        return this.idBancaSeller;
    }

    /**
     * Sets the value of the idBancaSeller property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setIdBancaSeller(String value) {
        this.idBancaSeller = value;
    }

}