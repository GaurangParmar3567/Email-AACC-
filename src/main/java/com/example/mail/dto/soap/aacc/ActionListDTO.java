package com.example.mail.dto.soap.aacc;

import lombok.Data;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class ActionListDTO {

    @XmlElement(name = "AWAction")
    private List<AWActionDTO> awActions = new ArrayList<>();
}