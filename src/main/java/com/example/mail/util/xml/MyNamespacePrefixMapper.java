package com.example.mail.util.xml;

import com.sun.xml.bind.marshaller.NamespacePrefixMapper;

public class MyNamespacePrefixMapper extends NamespacePrefixMapper {
    @Override
    public String getPreferredPrefix(String namespaceUri, String suggestion, boolean requirePrefix) {
        if ("http://schemas.xmlsoap.org/soap/envelope/".equals(namespaceUri)) return "soap";
        if ("http://tempuri.org/".equals(namespaceUri)) return "";
        if ("http://nortel.com/CCMMAgentWebservices/".equals(namespaceUri)) return "";
        return suggestion == null ? "" : suggestion;
    }

    @Override
    public String[] getPreDeclaredNamespaceUris() {
        return new String[] {"http://schemas.xmlsoap.org/soap/envelope/"};
    }
}
