package com.example.mail.controller;

import com.example.mail.dto.response.CustomerMailForReplyDTO;
import com.example.mail.service.CustomerMailService;
import com.example.mail.util.XmlUtils;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;

/**
 * SOAP 1.1 compatibility endpoint for the legacy DemsMailSend.asmx calls.
 * It delegates the mail lookup to the current Maker/Checker implementation,
 * but deliberately keeps the legacy operation and result element names.
 */
@RestController
@RequestMapping(value = "/email-service/sbi/customerMail", produces = MediaType.TEXT_XML_VALUE)
@CrossOrigin(origins = "https://ccdemsuat.sbi:6001")
public class CustomerMailSoapController {

    private static final String MAKER_OPERATION = "GetCustomerMailforMaker";
    private static final String CHECKER_OPERATION = "GetCustomerMailforChecker";
    private final CustomerMailService customerMailService;

    public CustomerMailSoapController(CustomerMailService customerMailService) {
        this.customerMailService = customerMailService;
    }

    @PostMapping(consumes = MediaType.TEXT_XML_VALUE)
    public ResponseEntity<String> getCustomerMail(@RequestBody String requestXml,
                                                   @RequestParam(value = "op", required = false) String operation) {
        try {
            String requestOperation = operation == null || operation.trim().isEmpty()
                    ? findOperation(requestXml) : operation.trim();
            if (!MAKER_OPERATION.equals(requestOperation) && !CHECKER_OPERATION.equals(requestOperation)) {
                return soapFault(HttpStatus.BAD_REQUEST, "Unsupported operation: " + requestOperation);
            }

            String contactId = findRequiredElementText(requestXml, "ContactID");
            CustomerMailForReplyDTO result = MAKER_OPERATION.equals(requestOperation)
                    ? customerMailService.getCustomerMailforMaker(contactId)
                    : customerMailService.getCustomerMailforChecker(contactId);

            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_XML)
                    .body(successResponse(requestOperation, result));
        } catch (Exception exception) {
            return soapFault(HttpStatus.INTERNAL_SERVER_ERROR,
                    exception.getMessage() == null ? "Unable to retrieve customer mail" : exception.getMessage());
        }
    }

    private String findOperation(String requestXml) throws Exception {
        Document document = parse(requestXml);
        Element body = findFirstElement(document.getDocumentElement(), "Body");
        if (body == null) {
            throw new IllegalArgumentException("SOAP Body is required");
        }
        for (Node node = body.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                return localName((Element) node);
            }
        }
        throw new IllegalArgumentException("SOAP operation is required");
    }

    private String findRequiredElementText(String requestXml, String name) throws Exception {
        Element element = findFirstElement(parse(requestXml).getDocumentElement(), name);
        if (element == null || element.getTextContent().trim().isEmpty()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return element.getTextContent().trim();
    }

    private Document parse(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        return factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
    }

    private Element findFirstElement(Element root, String expectedName) {
        if (expectedName.equals(localName(root))) {
            return root;
        }
        for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element match = findFirstElement((Element) node, expectedName);
                if (match != null) {
                    return match;
                }
            }
        }
        return null;
    }

    private String successResponse(String operation, CustomerMailForReplyDTO result) {
        String resultElement = operation + "Result";
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
                + "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                + "<soap:Body><" + operation + "Response xmlns=\"http://tempuri.org/\"><" + resultElement + ">"
                + XmlUtils.textElement("Message", result.getMessage())
                + XmlUtils.cdataElement("ReplyText", result.getReplyText())
                + XmlUtils.textElement("ClosedReason", result.getClosedReason())
                + XmlUtils.textElement("Comment", result.getComment())
                + XmlUtils.textElement("GetError", result.getGetError())
                + "</" + resultElement + "></" + operation + "Response></soap:Body></soap:Envelope>";
    }

    private ResponseEntity<String> soapFault(HttpStatus status, String message) {
        String body = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
                + "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body>"
                + "<soap:Fault><faultcode>soap:Client</faultcode><faultstring>" + XmlUtils.escapeXml(message)
                + "</faultstring></soap:Fault></soap:Body></soap:Envelope>";
        return ResponseEntity.status(status).contentType(MediaType.TEXT_XML).body(body);
    }

    private String localName(Element element) {
        return element.getLocalName() == null ? element.getNodeName() : element.getLocalName();
    }
}
