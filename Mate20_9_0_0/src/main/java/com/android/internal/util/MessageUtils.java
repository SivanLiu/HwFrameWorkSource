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

    /* JADX WARNING: Removed duplicated region for block: B:30:0x0073 A:{ExcHandler: IllegalAccessException | SecurityException (e java.lang.Throwable), Splitter:B:14:0x0049} */
    /* JADX WARNING: Failed to process nested try/catch */
    /* JADX WARNING: Missing block: B:28:0x0070, code skipped:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:29:0x0071, code skipped:
            r1 = r0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static SparseArray<String> findMessageNames(Class[] classes, String[] prefixes) {
        Class[] clsArr = classes;
        String[] strArr = prefixes;
        SparseArray<String> messageNames = new SparseArray();
        int length = clsArr.length;
        int i = 0;
        while (i < length) {
            Class c = clsArr[i];
            String className = c.getName();
            String prefix;
            try {
                Field[] fields = c.getDeclaredFields();
                int length2 = fields.length;
                int i2 = 0;
                while (i2 < length2) {
                    Field field = fields[i2];
                    int modifiers = field.getModifiers();
                    if (((Modifier.isStatic(modifiers) ^ 1) | (Modifier.isFinal(modifiers) ^ 1)) == 0) {
                        String name = field.getName();
                        int length3 = strArr.length;
                        int i3 = 0;
                        while (i3 < length3) {
                            prefix = strArr[i3];
                            if (name.startsWith(prefix)) {
                                try {
                                    field.setAccessible(true);
                                    int value = field.getInt(0);
                                    prefix = (String) messageNames.get(value);
                                    if (prefix != null) {
                                        if (!prefix.equals(name)) {
                                            throw new DuplicateConstantError(name, prefix, value);
                                        }
                                    }
                                    messageNames.put(value, name);
                                } catch (IllegalAccessException | SecurityException e) {
                                }
                            }
                            i3++;
                            clsArr = classes;
                            strArr = prefixes;
                        }
                        continue;
                    }
                    i2++;
                    clsArr = classes;
                    strArr = prefixes;
                }
                continue;
            } catch (SecurityException e2) {
                SecurityException securityException = e2;
                prefix = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Can't list fields of class ");
                stringBuilder.append(className);
                Log.e(prefix, stringBuilder.toString());
            }
            i++;
            clsArr = classes;
            strArr = prefixes;
        }
        return messageNames;
    }

    public static SparseArray<String> findMessageNames(Class[] classNames) {
        return findMessageNames(classNames, DEFAULT_PREFIXES);
    }
}
