package com.example.mail.dto.soapnew;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class CloseContactRequest {

    @XmlElement(name = "id", namespace = "http://nortel.com/CCMMAgentWebservices/")
    private String id;

    @XmlElement(name = "closureText", namespace = "http://nortel.com/CCMMAgentWebservices/")
    private String closureText;

    @XmlElement(name = "closedReasonCodeValue", namespace = "http://nortel.com/CCMMAgentWebservices/")
    private Long closedReasonCodeValue;

    @XmlElement(name = "closedReasonCodeSpecified", namespace = "http://nortel.com/CCMMAgentWebservices/")
    private Boolean closedReasonCodeSpecified;

    @XmlElement(name = "sessionKey", namespace = "http://nortel.com/CCMMAgentWebservices/")
    private String sessionKey;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getClosureText() { return closureText; }
    public void setClosureText(String closureText) { this.closureText = closureText; }

    public Long getClosedReasonCodeValue() { return closedReasonCodeValue; }
    public void setClosedReasonCodeValue(Long closedReasonCodeValue) { this.closedReasonCodeValue = closedReasonCodeValue; }

    public Boolean getClosedReasonCodeSpecified() { return closedReasonCodeSpecified; }
    public void setClosedReasonCodeSpecified(Boolean closedReasonCodeSpecified) { this.closedReasonCodeSpecified = closedReasonCodeSpecified; }

    public String getSessionKey() { return sessionKey; }
    public void setSessionKey(String sessionKey) { this.sessionKey = sessionKey; }
}
