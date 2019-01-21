package com.android.framework.protobuf.nano;

import android.telecom.Logging.Session;
import android.text.format.DateFormat;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Map.Entry;

public final class MessageNanoPrinter {
    private static final String INDENT = "  ";
    private static final int MAX_STRING_LEN = 200;

    private MessageNanoPrinter() {
    }

    public static <T extends MessageNano> String print(T message) {
        StringBuilder stringBuilder;
        if (message == null) {
            return "";
        }
        StringBuffer buf = new StringBuffer();
        try {
            print(null, message, new StringBuffer(), buf);
            return buf.toString();
        } catch (IllegalAccessException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Error printing proto: ");
            stringBuilder.append(e.getMessage());
            return stringBuilder.toString();
        } catch (InvocationTargetException e2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Error printing proto: ");
            stringBuilder.append(e2.getMessage());
            return stringBuilder.toString();
        }
    }

    private static void print(String identifier, Object object, StringBuffer indentBuf, StringBuffer buf) throws IllegalAccessException, InvocationTargetException {
        String identifier2;
        Map<?, ?> map = object;
        StringBuffer stringBuffer = indentBuf;
        StringBuffer stringBuffer2 = buf;
        if (map != null) {
            int length;
            if (map instanceof MessageNano) {
                String fieldName;
                int len;
                int origIndentBufLength = indentBuf.length();
                if (identifier != null) {
                    stringBuffer2.append(stringBuffer);
                    stringBuffer2.append(deCamelCaseify(identifier));
                    stringBuffer2.append(" <\n");
                    stringBuffer.append(INDENT);
                }
                Class<?> clazz = object.getClass();
                Field[] i = clazz.getFields();
                length = i.length;
                int i2 = 0;
                while (i2 < length) {
                    Field[] fieldArr;
                    int i3;
                    Field field = i[i2];
                    int modifiers = field.getModifiers();
                    fieldName = field.getName();
                    if ("cachedSize".equals(fieldName) || (modifiers & 1) != 1 || (modifiers & 8) == 8 || fieldName.startsWith(Session.SESSION_SEPARATION_CHAR_CHILD) || fieldName.endsWith(Session.SESSION_SEPARATION_CHAR_CHILD)) {
                        fieldArr = i;
                        i3 = length;
                    } else {
                        Class<?> fieldType = field.getType();
                        Object value = field.get(map);
                        if (fieldType.isArray()) {
                            if (fieldType.getComponentType() != Byte.TYPE) {
                                len = value == null ? 0 : Array.getLength(value);
                                int i4 = 0;
                                while (true) {
                                    fieldArr = i;
                                    int i5 = i4;
                                    if (i5 >= len) {
                                        break;
                                    }
                                    i3 = length;
                                    print(fieldName, Array.get(value, i5), stringBuffer, stringBuffer2);
                                    i4 = i5 + 1;
                                    i = fieldArr;
                                    length = i3;
                                }
                            } else {
                                print(fieldName, value, stringBuffer, stringBuffer2);
                                fieldArr = i;
                            }
                            i3 = length;
                        } else {
                            fieldArr = i;
                            i3 = length;
                            print(fieldName, value, stringBuffer, stringBuffer2);
                        }
                    }
                    i2++;
                    i = fieldArr;
                    length = i3;
                }
                Method[] methods = clazz.getMethods();
                len = methods.length;
                i2 = 0;
                while (i2 < len) {
                    Method[] methodArr;
                    String name = methods[i2].getName();
                    if (name.startsWith("set")) {
                        fieldName = name.substring(3);
                        Method hazzer = null;
                        try {
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("has");
                            stringBuilder.append(fieldName);
                            try {
                                if (((Boolean) clazz.getMethod(stringBuilder.toString(), new Class[0]).invoke(map, new Object[0])).booleanValue()) {
                                    Method getter = null;
                                    try {
                                        StringBuilder stringBuilder2 = new StringBuilder();
                                        stringBuilder2.append("get");
                                        stringBuilder2.append(fieldName);
                                        methodArr = methods;
                                        try {
                                            print(fieldName, clazz.getMethod(stringBuilder2.toString(), new Class[0]).invoke(map, new Object[0]), stringBuffer, stringBuffer2);
                                        } catch (NoSuchMethodException e) {
                                        }
                                    } catch (NoSuchMethodException e2) {
                                        methodArr = methods;
                                    }
                                    i2++;
                                    methods = methodArr;
                                }
                            } catch (NoSuchMethodException e3) {
                                methodArr = methods;
                            }
                        } catch (NoSuchMethodException e4) {
                            methodArr = methods;
                        }
                    }
                    methodArr = methods;
                    i2++;
                    methods = methodArr;
                }
                if (identifier != null) {
                    stringBuffer.setLength(origIndentBufLength);
                    stringBuffer2.append(stringBuffer);
                    stringBuffer2.append(">\n");
                }
            } else if (map instanceof Map) {
                Map<?, ?> map2 = map;
                identifier2 = deCamelCaseify(identifier);
                for (Entry<?, ?> entry : map2.entrySet()) {
                    stringBuffer2.append(stringBuffer);
                    stringBuffer2.append(identifier2);
                    stringBuffer2.append(" <\n");
                    length = indentBuf.length();
                    stringBuffer.append(INDENT);
                    print("key", entry.getKey(), stringBuffer, stringBuffer2);
                    print("value", entry.getValue(), stringBuffer, stringBuffer2);
                    stringBuffer.setLength(length);
                    stringBuffer2.append(stringBuffer);
                    stringBuffer2.append(">\n");
                }
                return;
            } else {
                String identifier3 = deCamelCaseify(identifier);
                stringBuffer2.append(stringBuffer);
                stringBuffer2.append(identifier3);
                stringBuffer2.append(": ");
                if (map instanceof String) {
                    identifier2 = sanitizeString((String) map);
                    stringBuffer2.append("\"");
                    stringBuffer2.append(identifier2);
                    stringBuffer2.append("\"");
                } else if (map instanceof byte[]) {
                    appendQuotedBytes((byte[]) map, stringBuffer2);
                } else {
                    stringBuffer2.append(map);
                }
                stringBuffer2.append("\n");
                return;
            }
        }
        identifier2 = identifier;
    }

    private static String deCamelCaseify(String identifier) {
        StringBuffer out = new StringBuffer();
        for (int i = 0; i < identifier.length(); i++) {
            char currentChar = identifier.charAt(i);
            if (i == 0) {
                out.append(Character.toLowerCase(currentChar));
            } else if (Character.isUpperCase(currentChar)) {
                out.append('_');
                out.append(Character.toLowerCase(currentChar));
            } else {
                out.append(currentChar);
            }
        }
        return out.toString();
    }

    private static String sanitizeString(String str) {
        if (!str.startsWith("http") && str.length() > 200) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(str.substring(0, 200));
            stringBuilder.append("[...]");
            str = stringBuilder.toString();
        }
        return escapeString(str);
    }

    private static String escapeString(String str) {
        int strLen = str.length();
        StringBuilder b = new StringBuilder(strLen);
        for (int i = 0; i < strLen; i++) {
            char original = str.charAt(i);
            if (original < ' ' || original > '~' || original == '\"' || original == DateFormat.QUOTE) {
                b.append(String.format("\\u%04x", new Object[]{Integer.valueOf(original)}));
            } else {
                b.append(original);
            }
        }
        return b.toString();
    }

    private static void appendQuotedBytes(byte[] bytes, StringBuffer builder) {
        if (bytes == null) {
            builder.append("\"\"");
            return;
        }
        builder.append('\"');
        for (int ch : bytes) {
            int ch2 = ch2 & 255;
            if (ch2 == 92 || ch2 == 34) {
                builder.append('\\');
                builder.append((char) ch2);
            } else if (ch2 < 32 || ch2 >= 127) {
                builder.append(String.format("\\%03o", new Object[]{Integer.valueOf(ch2)}));
            } else {
                builder.append((char) ch2);
            }
        }
        builder.append('\"');
    }
}
