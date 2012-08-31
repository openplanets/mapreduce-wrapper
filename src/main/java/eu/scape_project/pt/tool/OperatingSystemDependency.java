//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2012.08.23 at 10:04:24 AM MESZ 
//


package eu.scape_project.pt.tool;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;


/**
 * Declares an operating system dependency.
 * 
 * <p>Java class for OperatingSystemDependency complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="OperatingSystemDependency">
 *   &lt;simpleContent>
 *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema>string">
 *       &lt;attribute name="operatingSystemName" use="required" type="{http://scape-project.eu/tool}OperatingSystemName" />
 *       &lt;attribute name="otherOperatingSystemName" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="operatingSystemVersion" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="format" type="{http://scape-project.eu/tool}DependencyFormat" />
 *       &lt;attribute name="otherFormat" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/extension>
 *   &lt;/simpleContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "OperatingSystemDependency", propOrder = {
    "value"
})
public class OperatingSystemDependency {

    @XmlValue
    protected String value;
    @XmlAttribute(required = true)
    protected OperatingSystemName operatingSystemName;
    @XmlAttribute
    protected String otherOperatingSystemName;
    @XmlAttribute
    protected String operatingSystemVersion;
    @XmlAttribute
    protected DependencyFormat format;
    @XmlAttribute
    protected String otherFormat;

    /**
     * Gets the value of the value property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the value of the value property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Gets the value of the operatingSystemName property.
     * 
     * @return
     *     possible object is
     *     {@link OperatingSystemName }
     *     
     */
    public OperatingSystemName getOperatingSystemName() {
        return operatingSystemName;
    }

    /**
     * Sets the value of the operatingSystemName property.
     * 
     * @param value
     *     allowed object is
     *     {@link OperatingSystemName }
     *     
     */
    public void setOperatingSystemName(OperatingSystemName value) {
        this.operatingSystemName = value;
    }

    /**
     * Gets the value of the otherOperatingSystemName property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getOtherOperatingSystemName() {
        return otherOperatingSystemName;
    }

    /**
     * Sets the value of the otherOperatingSystemName property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setOtherOperatingSystemName(String value) {
        this.otherOperatingSystemName = value;
    }

    /**
     * Gets the value of the operatingSystemVersion property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getOperatingSystemVersion() {
        return operatingSystemVersion;
    }

    /**
     * Sets the value of the operatingSystemVersion property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setOperatingSystemVersion(String value) {
        this.operatingSystemVersion = value;
    }

    /**
     * Gets the value of the format property.
     * 
     * @return
     *     possible object is
     *     {@link DependencyFormat }
     *     
     */
    public DependencyFormat getFormat() {
        return format;
    }

    /**
     * Sets the value of the format property.
     * 
     * @param value
     *     allowed object is
     *     {@link DependencyFormat }
     *     
     */
    public void setFormat(DependencyFormat value) {
        this.format = value;
    }

    /**
     * Gets the value of the otherFormat property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getOtherFormat() {
        return otherFormat;
    }

    /**
     * Sets the value of the otherFormat property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setOtherFormat(String value) {
        this.otherFormat = value;
    }

}
