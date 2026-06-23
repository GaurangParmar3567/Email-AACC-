package com.example.mail.controller;

import com.example.mail.model.ContactAction;
import com.example.mail.model.Email;
import com.example.mail.repository.ContactActionRepository;
import com.example.mail.repository.EmailRepository;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/agentwebservices")
public class AvayaCompatibleController {

    private final Logger logger = LoggerFactory.getLogger("MAIL_SERVICES_AVAAYA_LOGGER");

    private final EmailRepository emailRepository;
    private final ContactActionRepository contactActionRepository;

    public AvayaCompatibleController(EmailRepository emailRepository, ContactActionRepository contactActionRepository) {
        this.emailRepository = emailRepository;
        this.contactActionRepository = contactActionRepository;
    }

    @PostMapping("/ReadContact")
    public ResponseEntity<ReadContactResponse> readContact(@RequestBody ReadContactRequest request) {
        if (!isValidSessionKey(request.getSessionKey())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<Email> email = emailRepository.findByContactId(request.getId());
        if (email == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        ReadContactResponse response = new ReadContactResponse();
        ReadContactResult result = new ReadContactResult();

        result.setId(email.get(0).getContactId());
        result.setCustomerID(email.get(0).getCustomerId());
        result.setOriginalSubject(email.get(0).getOriginalSubject());
        result.setSource(email.get(0).getSource());
        result.setStatus(email.get(0).getStatus());
        result.setSkillset(new Skillset(email.get(0).getSkillsetId(), email.get(0).getSkillsetName()));
        result.setPriority(email.get(0).getPriority());
        result.setTimezone(email.get(0).getTimezone());
        result.setArrivalTime(new TimeValue(email.get(0).getArrivalTime()));
        result.setOpenTime(email.get(0).getOpenTime() != null ? new TimeValue(email.get(0).getOpenTime()) : null);
        result.setOpenDuration(email.get(0).getOpenDuration());
        result.setMailTo(email.get(0).getMailTo());
        result.setMailFrom(email.get(0).getMailFrom());
        result.setMailCc(email.get(0).getMailCc());
        result.setContactType(email.get(0).getContactType());

        if (email.get(0).getAgentId() != null) {
            result.setAgent(new Agent(
                    email.get(0).getAgentId(),
                    email.get(0).getAgentFirstName(),
                    email.get(0).getAgentLastName()
            ));
        }

        List<ContactAction> actions = new ArrayList<>();
        for(Email email1 : email){
            actions = contactActionRepository.findByContactId(email1.getContactId());
        }
        List<AWAction> awActions = actions.stream()
                .map(this::convertToAWAction)
                .collect(Collectors.toList());
        result.setActionList(awActions);

        response.setReadContactResult(result);
        return ResponseEntity.ok(response);
    }

    private AWAction convertToAWAction(ContactAction action) {
        AWAction awAction = new AWAction();
        awAction.setId(action.getActionId());
        awAction.setContactID(action.getContactId());
        awAction.setSubject(action.getSubject());
        awAction.setText(action.getTextContent());
        awAction.setTextHTML(action.getTextHtml());
        awAction.setCallbackStatus(action.getCallbackStatus());
        awAction.setCreationTime(new TimeValue(action.getCreationTime()));
        awAction.setSource(action.getSource());
        awAction.setComment(action.getComment());
        awAction.setTimeAllocated(action.getTimeAllocated());
        awAction.setOutboundTalkTime(action.getOutboundTalkTime());
        awAction.setOutboundDispositionCode(action.getOutboundDispositionCode());
        awAction.setActionType(action.getActionType());

        if (action.getAgentId() != null) {
            awAction.setAgent(new Agent(
                    action.getAgentId(),
                    action.getAgentFirstName(),
                    action.getAgentLastName()
            ));
        }

        if (action.getClosedReasonName() != null) {
            awAction.setClosedReason(new ClosedReason(
                    action.getClosedReasonName(),
                    action.getClosedReasonNumericValue()
            ));
        }

        return awAction;
    }

    private boolean isValidSessionKey(String sessionKey) {
        // Implement proper session validation
        return sessionKey != null && sessionKey.startsWith("beFj");
    }

    @Data
    public static class ReadContactRequest {
        private Long id;
        private String sessionKey;
    }

    @Data
    public static class ReadContactResponse {
        private ReadContactResult readContactResult;
    }

    @Data
    public static class ReadContactResult {
        private Long id;
        private Long customerID;
        private String originalSubject;
        private String source;
        private String status;
        private Skillset skillset;
        private String priority;
        private Integer timezone;
        private TimeValue arrivalTime;
        private TimeValue openTime;
        private Integer openDuration;
        private String mailTo;
        private String mailFrom;
        private String mailCc;
        private String contactType;
        private Agent agent;
        private List<AWAction> actionList;
    }

    @Data
    public static class Skillset {
        private Long id;
        private String name;

        public Skillset(Long id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    @Data
    public static class Agent {
        private Long id;
        private String firstName;
        private String lastName;

        public Agent(Long id, String firstName, String lastName) {
            this.id = id;
            this.firstName = firstName;
            this.lastName = lastName;
        }
    }

    @Data
    public static class AWAction {
        private Long id;
        private Long contactID;
        private String subject;
        private String text;
        private String textHTML;
        private String callbackStatus;
        private TimeValue creationTime;
        private String source;
        private String comment;
        private Integer timeAllocated;
        private Integer outboundTalkTime;
        private String outboundDispositionCode;
        private String actionType;
        private Agent agent;
        private ClosedReason closedReason;
    }

    @Data
    public static class TimeValue {
        private Long milliseconds;

        public TimeValue(Long milliseconds) {
            this.milliseconds = milliseconds;
        }
    }

    @Data
    public static class ClosedReason {
        private String name;
        private Integer numericValue;

        public ClosedReason(String name, Integer numericValue) {
            this.name = name;
            this.numericValue = numericValue;
        }
    }
}
