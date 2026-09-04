package com.example.mail.controller;

import com.example.mail.dto.response.CustomerMailForReplyDTO;
import com.example.mail.dto.soap.aacc.*;
import com.example.mail.dto.soap.checker.SendToCheckerResult;
import com.example.mail.dto.soap.checker.SoapSendToCheckerRequestEnvelope;
import com.example.mail.dto.soap.checker.SoapSendToCheckerResponseEnvelope;
import com.example.mail.exception.ContactNotFoundException;
import com.example.mail.service.AaccContactService;
import com.example.mail.service.CustomerMailService;
import com.example.mail.service.EmailDisplayService;
import com.example.mail.service.MakerTransferStatusService;
import com.example.mail.util.XmlUtils;
import com.example.mail.util.xml.CustomerMailNamespacePrefixMapper;
import com.example.mail.util.xml.MyNamespacePrefixMapper;
import com.sun.xml.bind.marshaller.CharacterEscapeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.bind.Unmarshaller;
import java.io.StringReader;
import java.io.StringWriter;

@RestController
@RequestMapping(value = "/email-service/sbi", consumes = MediaType.TEXT_XML_VALUE, produces = MediaType.TEXT_XML_VALUE)
@CrossOrigin(origins = "https://ccdemsuat.sbi:6001")
public class ContactSoapController {

    private final Logger logger = LoggerFactory.getLogger("MAIL_SERVICES_AVAAYA_LOGGER");
    private final AaccContactService aaccContactService;
    private final EmailDisplayService emailDisplayService;
    private final MakerTransferStatusService makerTransferStatusService;
    private final CustomerMailService customerMailService;

    public ContactSoapController(AaccContactService aaccContactService,
                                 EmailDisplayService emailDisplayService,
                                 MakerTransferStatusService makerTransferStatusService,
                                 CustomerMailService customerMailService) {
        this.aaccContactService = aaccContactService;
        this.emailDisplayService = emailDisplayService;
        this.makerTransferStatusService = makerTransferStatusService;
        this.customerMailService = customerMailService;
    }

    @PostMapping("/getContactAACC")
    public ResponseEntity<String> readContact(@RequestBody String rawXmlRequestBody) {
        try {
            SoapRequestEnvelope request = unmarshal(rawXmlRequestBody, SoapRequestEnvelope.class);
            if (request.getBody() == null || request.getBody().getReadContact() == null) {
                return soapFault(HttpStatus.BAD_REQUEST, "Invalid SOAP request: missing ReadContact element");
            }
            ReadContactResponse response = new ReadContactResponse();
            response.setReadContactResult(aaccContactService.getContactDetails(
                    request.getBody().getReadContact().getId()));
            SoapResponseBody body = new SoapResponseBody();
            body.setReadContactResponse(response);
            SoapResponseEnvelope envelope = new SoapResponseEnvelope();
            envelope.setBody(body);
            return xmlResponse(marshal(envelope));
        } catch (ContactNotFoundException exception) {
            return soapFault(HttpStatus.NOT_FOUND, exception.getMessage());
        } catch (Exception exception) {
            logger.error("Failure while processing ReadContact SOAP request", exception);
            return soapFault(HttpStatus.INTERNAL_SERVER_ERROR, "SOAP Processing Exception");
        }
    }

    @PostMapping("/SendToChecker")
    public ResponseEntity<String> sendToChecker(@RequestBody String rawXmlRequestBody) {
        try {
            SoapSendToCheckerRequestEnvelope request = unmarshal(rawXmlRequestBody,
                    SoapSendToCheckerRequestEnvelope.class);
            if (request.getBody() == null || request.getBody().getSendToChecker() == null
                    || request.getBody().getSendToChecker().getObjMail() == null) {
                return soapFault(HttpStatus.BAD_REQUEST, "Invalid SOAP request: missing SendToChecker objMail payload");
            }
            makerTransferStatusService.saveMakerTransferDetails(
                    request.getBody().getSendToChecker().getObjMail());
            SendToCheckerResult result = new SendToCheckerResult();
            result.setMailId(request.getBody().getSendToChecker().getObjMail().getMailId());
            result.setMessage("Maker transfer details saved successfully");
            SoapSendToCheckerResponseEnvelope envelope = new SoapSendToCheckerResponseEnvelope();
            envelope.getBody().setSendToCheckerResponse(result);
            return xmlResponse(marshal(envelope));
        } catch (JAXBException exception) {
            return soapFault(HttpStatus.BAD_REQUEST, "Invalid SendToChecker SOAP request");
        } catch (IllegalArgumentException exception) {
            return soapFault(HttpStatus.BAD_REQUEST, exception.getMessage());
        } catch (Exception exception) {
            logger.error("Failure while processing SendToChecker SOAP request", exception);
            return soapFault(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to save maker transfer details");
        }
    }

    @PostMapping("/TransferContactToSkillset")
    public ResponseEntity<String> transferContactToSkillset(@RequestBody String rawXmlRequestBody) {
        try {
            TransferContactToSkillsetRequestEnvelope envelope = unmarshal(rawXmlRequestBody,
                    TransferContactToSkillsetRequestEnvelope.class);
            if (envelope.getBody() == null || envelope.getBody().getTransferContactToSkillset() == null) {
                return soapFault(HttpStatus.BAD_REQUEST, "Invalid SOAP request: missing TransferContactToSkillset payload");
            }
            TransferContactToSkillsetRequest request = envelope.getBody().getTransferContactToSkillset();
            if (request.getId() == null || request.getId().trim().isEmpty() || request.getSkillsetId() == null) {
                return soapFault(HttpStatus.BAD_REQUEST, "id and skillsetId are required");
            }
            long contactId = Long.parseLong(request.getId().trim());
            TransferContactToSkillsetResponseEnvelope responseEnvelope = new TransferContactToSkillsetResponseEnvelope();
            responseEnvelope.getBody().setTransferContactToSkillsetResponse(
                    new TransferContactToSkillsetResult(
                            aaccContactService.transferToSkillset(contactId, request.getSkillsetId())));
            return xmlResponse(marshal(responseEnvelope));
        } catch (ContactNotFoundException exception) {
            return soapFault(HttpStatus.NOT_FOUND, exception.getMessage());
        } catch (NumberFormatException exception) {
            return soapFault(HttpStatus.BAD_REQUEST, "Invalid contact id format");
        } catch (IllegalArgumentException exception) {
            return soapFault(HttpStatus.BAD_REQUEST, exception.getMessage());
        } catch (Exception exception) {
            logger.error("Failure while processing TransferContactToSkillset SOAP request", exception);
            return soapFault(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to transfer contact to skillset");
        }
    }

    @PostMapping("/GetAllClosedReasonCodes")
    public ResponseEntity<String> getAllClosedReasonCodes(@RequestBody String rawXmlRequestBody) {
        try {
            SoapRequestEnvelope request = unmarshal(rawXmlRequestBody, SoapRequestEnvelope.class);
            if (request.getBody() == null || request.getBody().getGetAllClosedReasonCodes() == null) {
                return soapFault(HttpStatus.BAD_REQUEST, "Invalid SOAP request: missing GetAllClosedReasonCodes element");
            }
            GetAllClosedReasonCodesResult result = new GetAllClosedReasonCodesResult();
            result.getAwClosedReasonCodes().addAll(aaccContactService.getClosedReasonCodes());
            GetAllClosedReasonCodesResponse response = new GetAllClosedReasonCodesResponse();
            response.setGetAllClosedReasonCodesResult(result);
            SoapResponseBody body = new SoapResponseBody();
            body.setGetAllClosedReasonCodesResponse(response);
            SoapResponseEnvelope envelope = new SoapResponseEnvelope();
            envelope.setBody(body);
            return xmlResponse(marshal(envelope));
        } catch (Exception exception) {
            logger.error("Failure while processing GetAllClosedReasonCodes SOAP request", exception);
            return soapFault(HttpStatus.INTERNAL_SERVER_ERROR, "SOAP Processing Exception");
        }
    }

    @PostMapping("/GetHistoryFromAACC")
    public ResponseEntity<String> getHistoryFromAACC(@RequestBody String rawXmlRequestBody) {
        try {
            SoapHistoryRequestEnvelope request = unmarshal(rawXmlRequestBody, SoapHistoryRequestEnvelope.class);
            if (request.getBody() == null || request.getBody().getGetHistoryFromAACC() == null) {
                return soapFault(HttpStatus.BAD_REQUEST, "Invalid SOAP request: missing GetHistoryFromAACC element");
            }
            GetHistoryFromAACCRequest input = request.getBody().getGetHistoryFromAACC();
            GetHistoryFromAACCResponse response = new GetHistoryFromAACCResponse();
            response.setGetHistoryFromAACCResult(aaccContactService.getHistory(
                    input.getSearchType(), input.getSearchValue()));
            SoapHistoryResponseBody body = new SoapHistoryResponseBody();
            body.setGetHistoryFromAACCResponse(response);
            SoapHistoryResponseEnvelope envelope = new SoapHistoryResponseEnvelope();
            envelope.setBody(body);
            return xmlResponse(marshal(envelope));
        } catch (Exception exception) {
            logger.error("Failure while processing GetHistoryFromAACC SOAP request", exception);
            return soapFault(HttpStatus.INTERNAL_SERVER_ERROR, "SOAP Processing Exception");
        }
    }

    @PostMapping("/getContact")
    public ResponseEntity<String> getContact(@RequestBody String rawXmlRequestBody) {
        try {
            SoapContactRequestEnvelope request = unmarshal(rawXmlRequestBody, SoapContactRequestEnvelope.class);
            Long agentId = request.getBody().getGetContact().getStrAgentID();
            Long contactId = emailDisplayService.assignNextPendingEmail(agentId);
            GetContactResult result = new GetContactResult();
            result.setContactId(contactId == null ? 0L : contactId);
            result.setMessage(contactId == null || contactId == 0L ? "Invalid Input" : "Valid Input");
            GetContactResponse response = new GetContactResponse();
            response.setGetContactResult(result);
            SoapContactResponseBody body = new SoapContactResponseBody();
            body.setGetContactResponse(response);
            SoapContactResponseEnvelope envelope = new SoapContactResponseEnvelope();
            envelope.setBody(body);
            return xmlResponse(marshal(envelope));
        } catch (Exception exception) {
            logger.error("Failure while processing GetContact SOAP request", exception);
            return soapFault(HttpStatus.INTERNAL_SERVER_ERROR, "SOAP Processing Exception");
        }
    }

    @PostMapping("/CloseContact")
    public ResponseEntity<String> closeContact(@RequestBody String rawXmlRequestBody) {
        try {
            SoapRequestEnvelope requestEnvelope = unmarshal(rawXmlRequestBody, SoapRequestEnvelope.class);
            if (requestEnvelope.getBody() == null || requestEnvelope.getBody().getCloseContact() == null) {
                return soapFault(HttpStatus.BAD_REQUEST, "Invalid SOAP request: missing CloseContact element");
            }
            CloseContactRequest request = requestEnvelope.getBody().getCloseContact();
            long contactId = Long.parseUnsignedLong(request.getId());
            long actionId = aaccContactService.closeContact(contactId, request.getClosureText(),
                    request.getClosedReasonCodeValue(), request.getClosedReasonCodeSpecified());
            CloseContactResponse response = new CloseContactResponse();
            response.setCloseContactResult(new CloseContactResult(actionId));
            SoapResponseBody body = new SoapResponseBody();
            body.setCloseContactResponse(response);
            SoapResponseEnvelope envelope = new SoapResponseEnvelope();
            envelope.setBody(body);
            return xmlResponse(marshal(envelope));
        } catch (ContactNotFoundException exception) {
            return soapFault(HttpStatus.NOT_FOUND, exception.getMessage());
        } catch (NumberFormatException exception) {
            return soapFault(HttpStatus.BAD_REQUEST, "Invalid contact id format");
        } catch (Exception exception) {
            logger.error("Failure while processing CloseContact SOAP request", exception);
            return soapFault(HttpStatus.INTERNAL_SERVER_ERROR, "SOAP Processing Exception");
        }
    }

    @PostMapping("/getCustomerMail")
    public ResponseEntity<String> getCustomerMail(@RequestBody String rawXmlRequestBody) {
        try {
            SoapCustomerMailRequestEnvelope requestEnvelope = unmarshal(
                    rawXmlRequestBody, SoapCustomerMailRequestEnvelope.class);
            if (requestEnvelope.getBody() == null) {
                return soapFault(HttpStatus.BAD_REQUEST, "SOAP Body is required");
            }
            CustomerMailRequest request;
            boolean makerOperation;
            if (requestEnvelope.getBody().getGetCustomerMailforMaker() != null) {
                request = requestEnvelope.getBody().getGetCustomerMailforMaker();
                makerOperation = true;
            } else if (requestEnvelope.getBody().getGetCustomerMailforChecker() != null) {
                request = requestEnvelope.getBody().getGetCustomerMailforChecker();
                makerOperation = false;
            } else {
                return soapFault(HttpStatus.BAD_REQUEST,
                        "GetCustomerMailforMaker or GetCustomerMailforChecker is required");
            }
            if (request.getContactId() == null || request.getContactId().trim().isEmpty()) {
                return soapFault(HttpStatus.BAD_REQUEST, "ContactID is required");
            }

            String operation = makerOperation ? "GetCustomerMailforMaker" : "GetCustomerMailforChecker";
            CustomerMailForReplyDTO mail = makerOperation
                    ? customerMailService.getCustomerMailforMaker(request.getContactId().trim())
                    : customerMailService.getCustomerMailforChecker(request.getContactId().trim());
            CustomerMailResult result = new CustomerMailResult();
            result.setMessage(mail.getMessage());
            result.setReplyText(XmlUtils.cdata(mail.getReplyText()));
            result.setClosedReason(mail.getClosedReason());
            result.setComment(mail.getComment());
            result.setGetError(mail.getGetError());

            SoapCustomerMailResponseEnvelope responseEnvelope = new SoapCustomerMailResponseEnvelope();
            CustomerMailResponseBody responseBody = new CustomerMailResponseBody();
            if (makerOperation) {
                MakerCustomerMailResponse response = new MakerCustomerMailResponse();
                response.setResult(result);
                responseBody.setGetCustomerMailforMakerResponse(response);
            } else {
                CheckerCustomerMailResponse response = new CheckerCustomerMailResponse();
                response.setResult(result);
                responseBody.setGetCustomerMailforCheckerResponse(response);
            }
            responseEnvelope.setBody(responseBody);

            Marshaller marshaller = JAXBContext.newInstance(
                    SoapCustomerMailResponseEnvelope.class,
                    CustomerMailResponseBody.class,
                    MakerCustomerMailResponse.class,
                    CheckerCustomerMailResponse.class,
                    CustomerMailResult.class).createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.FALSE);
            marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper",
                    new CustomerMailNamespacePrefixMapper());
            marshaller.setProperty("com.sun.xml.bind.marshaller.CharacterEscapeHandler",
                    (CharacterEscapeHandler) (characters, start, length, isAttVal, writer) -> {
                        String value = new String(characters, start, length);
                        if (value.startsWith("<![CDATA[") && value.endsWith("]]>") && !isAttVal) {
                            writer.write(value);
                        } else {
                            XmlUtils.escapeXmlCharacters(characters, start, length, isAttVal, writer);
                        }
                    });
            StringWriter writer = new StringWriter();
            marshaller.marshal(responseEnvelope, writer);
            logger.debug("Generated SOAP response for {} and ContactID {}", operation, request.getContactId());
            return xmlResponse(writer.toString());
        } catch (JAXBException exception) {
            return soapFault(HttpStatus.BAD_REQUEST, "Invalid GetCustomerMail SOAP request");
        } catch (ContactNotFoundException exception) {
            return soapFault(HttpStatus.NOT_FOUND, exception.getMessage());
        } catch (Exception exception) {
            logger.error("Failure while processing GetCustomerMail SOAP request", exception);
            return soapFault(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to retrieve customer mail");
        }
    }

    private <T> T unmarshal(String xml, Class<T> type) throws JAXBException {
        Unmarshaller unmarshaller = JAXBContext.newInstance(type).createUnmarshaller();
        return type.cast(unmarshaller.unmarshal(new StringReader(xml)));
    }

    private String marshal(Object envelope) throws JAXBException {
        Marshaller marshaller = JAXBContext.newInstance(envelope.getClass()).createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.FALSE);
        try {
            marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper", new MyNamespacePrefixMapper());
        } catch (PropertyException exception) {
            marshaller.setProperty("org.glassfish.jaxb.namespacePrefixMapper", new MyNamespacePrefixMapper());
        }
        StringWriter writer = new StringWriter();
        marshaller.marshal(envelope, writer);
        return writer.toString();
    }

    private ResponseEntity<String> xmlResponse(String body) {
        return ResponseEntity.ok().contentType(MediaType.TEXT_XML).body(body);
    }

    private ResponseEntity<String> soapFault(HttpStatus status, String message) {
        return ResponseEntity.status(status).contentType(MediaType.TEXT_XML).body(
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
                        + "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body>"
                        + "<soap:Fault><faultcode>soap:Client</faultcode><faultstring>"
                        + XmlUtils.escapeXml(message == null ? "Invalid SOAP request" : message)
                        + "</faultstring></soap:Fault></soap:Body></soap:Envelope>");
    }
}
