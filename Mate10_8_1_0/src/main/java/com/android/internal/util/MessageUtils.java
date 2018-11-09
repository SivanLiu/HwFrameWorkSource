package com.android.internal.util;

import android.util.Log;
import android.util.SparseArray;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class MessageUtils {
    private static final boolean DBG = false;
    public static final String[] DEFAULT_PREFIXES = new String[]{"CMD_", "EVENT_"};
    private static final String TAG = MessageUtils.class.getSimpleName();

    public static class DuplicateConstantError extends Error {
        private DuplicateConstantError() {
        }

        public DuplicateConstantError(String name1, String name2, int value) {
            super(String.format("Duplicate constant value: both %s and %s = %d", new Object[]{name1, name2, Integer.valueOf(value)}));
        }
    }

    public static SparseArray<String> findMessageNames(Class[] classes, String[] prefixes) {
        SparseArray<String> messageNames = new SparseArray();
        for (Class c : classes) {
            try {
                for (Field field : c.getDeclaredFields()) {
                    int modifiers = field.getModifiers();
                    if (((Modifier.isStatic(modifiers) ^ 1) | (Modifier.isFinal(modifiers) ^ 1)) == 0) {
                        String name = field.getName();
                        for (String prefix : prefixes) {
                            if (name.startsWith(prefix)) {
                                try {
                                    field.setAccessible(true);
                                    try {
                                        int value = field.getInt(null);
                                        String previousName = (String) messageNames.get(value);
                                        if (previousName == null || (previousName.equals(name) ^ 1) == 0) {
                                            messageNames.put(value, name);
                                        } else {
                                            throw new DuplicateConstantError(name, previousName, value);
                                        }
                                    } catch (IllegalArgumentException e) {
                                    }
                                } catch (SecurityException e2) {
                                }
                            }
                        }
                        continue;
                    }
                }
                continue;
            } catch (SecurityException e3) {
                Log.e(TAG, "Can't list fields of class " + c.getName());
            }
        }
        return messageNames;
    }

    public static SparseArray<String> findMessageNames(Class[] classNames) {
        return findMessageNames(classNames, DEFAULT_PREFIXES);
    }
}
