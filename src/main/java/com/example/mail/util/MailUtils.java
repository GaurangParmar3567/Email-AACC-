package com.example.mail.util;

import javax.mail.Address;
import javax.mail.internet.InternetAddress;
import java.util.Arrays;
import java.util.stream.Collectors;

public final class MailUtils {

    private MailUtils() {
    }

    public static String addressArrayToString(Address[] addresses) {
        if (addresses == null || addresses.length == 0) {
            return "";
        }
        return Arrays.stream(addresses)
                .map(address -> address instanceof InternetAddress
                        ? ((InternetAddress) address).getAddress()
                        : address.toString())
                .collect(Collectors.joining(","));
    }

    public static boolean isAddressInArray(String email, Address[] addresses) {
        if (email == null || addresses == null) {
            return false;
        }
        for (Address address : addresses) {
            if (address instanceof InternetAddress
                    && ((InternetAddress) address).getAddress().equalsIgnoreCase(email)) {
                return true;
            }
        }
        return false;
    }
}
