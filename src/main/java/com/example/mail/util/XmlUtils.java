package com.example.mail.util;

import java.io.IOException;
import java.io.Writer;

public final class XmlUtils {

    private XmlUtils() {
    }

    public static String escapeXml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    public static String cdata(String value) {
        return value == null ? null : "<![CDATA[" + value.replace("]]>", "]]><![CDATA[>") + "]]>";
    }

    public static String textElement(String name, String value) {
        return value == null ? "" : "<" + name + ">" + escapeXml(value) + "</" + name + ">";
    }

    public static String cdataElement(String name, String value) {
        return value == null ? "" : "<" + name + ">" + cdata(value) + "</" + name + ">";
    }

    public static void escapeXmlCharacters(char[] characters, int start, int length,
                                           boolean isAttribute, Writer writer) throws IOException {
        for (int index = start; index < start + length; index++) {
            char character = characters[index];
            switch (character) {
                case '&': writer.write("&amp;"); break;
                case '<': writer.write("&lt;"); break;
                case '>': writer.write("&gt;"); break;
                case '"': writer.write(isAttribute ? "&quot;" : "\""); break;
                case '\r': writer.write("&#xD;"); break;
                default: writer.write(character);
            }
        }
    }
}
