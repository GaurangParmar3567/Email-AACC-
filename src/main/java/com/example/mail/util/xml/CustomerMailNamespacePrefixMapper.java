package com.example.mail.util.xml;

import com.sun.xml.bind.marshaller.NamespacePrefixMapper;

public class CustomerMailNamespacePrefixMapper extends NamespacePrefixMapper {

    private static final String SOAP_NAMESPACE = "http://schemas.xmlsoap.org/soap/envelope/";

    @Override
    public String getPreferredPrefix(String namespaceUri, String suggestion, boolean requirePrefix) {
        return SOAP_NAMESPACE.equals(namespaceUri) ? "soap" : "";
    }

    @Override
    public String[] getPreDeclaredNamespaceUris() {
        return new String[]{SOAP_NAMESPACE};
    }
}