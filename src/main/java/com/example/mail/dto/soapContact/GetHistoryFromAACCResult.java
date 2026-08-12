package com.example.mail.dto.soapContact;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class GetHistoryFromAACCResult {

    @XmlElement(name = "MailHistory")
    private List<MailHistory> mailHistory = new ArrayList<>();

    public List<MailHistory> getMailHistory() {
        return mailHistory;
    }

    public void setMailHistory(List<MailHistory> mailHistory) {
        this.mailHistory = mailHistory;
    }
}
