package com.huawei.odmf.utils;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class StringUtil {
    private StringUtil() {
    }

    public static boolean isBlank(String str) {
        return str == null || str.length() == 0;
    }

    public static String join(Collection<?> list, String separator) {
        if (list.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (Object item : list) {
            if (item != null) {
                builder.append(separator).append(item);
            }
        }
        if (builder.length() == 0) {
            return "";
        }
        return builder.toString().substring(separator.length());
    }

    public static <T> String join(T[] array, String separator) {
        return join(Arrays.asList(array), separator);
    }

    public static Set<String> array2Set(String[] array) {
        if (array == null) {
            return new HashSet();
        }
        for (int i = 0; i < array.length; i++) {
            array[i] = array[i].trim();
        }
        return new HashSet(Arrays.asList(array));
    }
}
