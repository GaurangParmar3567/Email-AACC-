package com.example.mail.dto;

import com.sun.xml.bind.marshaller.NamespacePrefixMapper;

public class MyNamespacePrefixMapper extends NamespacePrefixMapper {
    @Override
    public String getPreferredPrefix(String namespaceUri, String suggestion, boolean requirePrefix) {
        if ("http://schemas.xmlsoap.org/soap/envelope/".equals(namespaceUri)) return "";
        if ("http://www.w3.org/2001/XMLSchema-instance".equals(namespaceUri)) return "";
        if ("http://www.w3.org/2001/XMLSchema".equals(namespaceUri)) return "";
        return suggestion;
    }

    @Override
    public String[] getPreDeclaredNamespaceUris() {
        return new String[] {
                "http://schemas.xmlsoap.org/soap/envelope/",
                "http://www.w3.org/2001/XMLSchema-instance",
                "http://www.w3.org/2001/XMLSchema"
        };
    }
}
