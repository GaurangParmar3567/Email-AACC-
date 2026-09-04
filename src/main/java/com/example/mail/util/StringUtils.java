package com.example.mail.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

public final class StringUtils {

    private StringUtils() {
    }

    public static boolean containsIgnoreCase(String source, String search) {
        return source != null && search != null
                && source.toLowerCase().contains(search.toLowerCase());
    }

    public static <T> List<T> filter(List<T> values, Predicate<T> predicate) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        List<T> result = new ArrayList<>();
        for (T value : values) {
            if (predicate.test(value)) {
                result.add(value);
            }
        }
        return result;
    }
}
