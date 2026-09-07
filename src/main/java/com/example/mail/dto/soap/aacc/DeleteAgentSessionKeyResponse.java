package com.example.mail.dto.soap.aacc;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;


import lombok.Data;

@Data
@XmlRootElement(name = "DeleteAgentSessionKeyResponse", namespace = "")
public class DeleteAgentSessionKeyResponse {

    public DeleteAgentSessionKeyResponse() {}
    
    public DeleteAgentSessionKeyResult deleteAgentSessionKeyResult ;
    
    public DeleteAgentSessionKeyResponse(DeleteAgentSessionKeyResult deleteAgentSessionKeyResult) {
     this.deleteAgentSessionKeyResult = deleteAgentSessionKeyResult;
    }
}