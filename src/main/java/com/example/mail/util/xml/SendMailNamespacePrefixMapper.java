package com.example.mail.util.xml;

import com.sun.xml.bind.marshaller.NamespacePrefixMapper;

public class SendMailNamespacePrefixMapper extends NamespacePrefixMapper {

    private static final String SOAP_NAMESPACE = "http://schemas.xmlsoap.org/soap/envelope/";
    private static final String XSI_NAMESPACE = "http://www.w3.org/2001/XMLSchema-instance";
    private static final String XSD_NAMESPACE = "http://www.w3.org/2001/XMLSchema";
    private static final String TEM_NAMESPACE = "http://tempuri.org/";

    @Override
    public String getPreferredPrefix(String namespaceUri, String suggestion, boolean requirePrefix) {
        if (SOAP_NAMESPACE.equals(namespaceUri)) return "soap";
        if (XSI_NAMESPACE.equals(namespaceUri)) return "xsi";
        if (XSD_NAMESPACE.equals(namespaceUri)) return "xsd";
        if (TEM_NAMESPACE.equals(namespaceUri)) return "";
        return suggestion == null ? "" : suggestion;
    }

    @Override
    public String[] getPreDeclaredNamespaceUris() {
        return new String[] {
            XSI_NAMESPACE,
            XSD_NAMESPACE,
            SOAP_NAMESPACE
        };
    }
}

