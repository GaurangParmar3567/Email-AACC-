package com.example.mail.dto.soapnew;

import lombok.Data;
import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class AttachmentListDTO {

    @XmlElement(name = "AWAttachment")
    private List<AWAttachmentDTO> awAttachments = new ArrayList<>();
}