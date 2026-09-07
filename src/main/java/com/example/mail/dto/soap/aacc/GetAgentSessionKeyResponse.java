package com.example.mail.dto.soap.aacc;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import lombok.Data;

@Data 
@XmlRootElement(name = "GetAgentSessionKeyResponse", namespace = "")
public class GetAgentSessionKeyResponse {

    public GetAgentSessionKeyResponse() {}
    
    public GetAgentSessionKeyResult getAgentSessionKeyResult ;
    
    public GetAgentSessionKeyResponse(GetAgentSessionKeyResult getAgentSessionKeyResult) {
     this.getAgentSessionKeyResult = getAgentSessionKeyResult;
    }
}