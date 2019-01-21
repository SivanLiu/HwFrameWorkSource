package com.android.internal.util;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Base64;
import android.util.Xml;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ProtocolException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class XmlUtils {
    private static final String STRING_ARRAY_SEPARATOR = ":";

    public interface ReadMapCallback {
        Object readThisUnknownObjectXml(XmlPullParser xmlPullParser, String str) throws XmlPullParserException, IOException;
    }

    public interface WriteMapCallback {
        void writeUnknownObject(Object obj, String str, XmlSerializer xmlSerializer) throws XmlPullParserException, IOException;
    }

    public static void skipCurrentTag(XmlPullParser parser) throws XmlPullParserException, IOException {
        int outerDepth = parser.getDepth();
        while (true) {
            int next = parser.next();
            int type = next;
            if (next == 1) {
                return;
            }
            if (type == 3) {
                if (parser.getDepth() <= outerDepth) {
                    return;
                }
            }
        }
    }

    public static final int convertValueToList(CharSequence value, String[] options, int defaultValue) {
        if (value != null) {
            for (int i = 0; i < options.length; i++) {
                if (value.equals(options[i])) {
                    return i;
                }
            }
        }
        return defaultValue;
    }

    public static final boolean convertValueToBoolean(CharSequence value, boolean defaultValue) {
        boolean result = false;
        if (value == null) {
            return defaultValue;
        }
        if (value.equals("1") || value.equals("true") || value.equals("TRUE")) {
            result = true;
        }
        return result;
    }

    public static final int convertValueToInt(CharSequence charSeq, int defaultValue) {
        if (charSeq == null) {
            return defaultValue;
        }
        String nm = charSeq.toString();
        int sign = 1;
        int index = 0;
        int len = nm.length();
        int base = 10;
        if ('-' == nm.charAt(0)) {
            sign = -1;
            index = 0 + 1;
        }
        if ('0' == nm.charAt(index)) {
            if (index == len - 1) {
                return 0;
            }
            char c = nm.charAt(index + 1);
            if (StateProperty.TARGET_X == c || 'X' == c) {
                index += 2;
                base = 16;
            } else {
                index++;
                base = 8;
            }
        } else if ('#' == nm.charAt(index)) {
            index++;
            base = 16;
        }
        return Integer.parseInt(nm.substring(index), base) * sign;
    }

    public static int convertValueToUnsignedInt(String value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return parseUnsignedIntAttribute(value);
    }

    public static int parseUnsignedIntAttribute(CharSequence charSeq) {
        String value = charSeq.toString();
        int index = 0;
        int len = value.length();
        int base = 10;
        if ('0' == value.charAt(0)) {
            if (0 == len - 1) {
                return 0;
            }
            char c = value.charAt(0 + 1);
            if (StateProperty.TARGET_X == c || 'X' == c) {
                index = 0 + 2;
                base = 16;
            } else {
                index = 0 + 1;
                base = 8;
            }
        } else if ('#' == value.charAt(0)) {
            index = 0 + 1;
            base = 16;
        }
        return (int) Long.parseLong(value.substring(index), base);
    }

    public static final void writeMapXml(Map val, OutputStream out) throws XmlPullParserException, IOException {
        XmlSerializer serializer = new FastXmlSerializer();
        serializer.setOutput(out, StandardCharsets.UTF_8.name());
        serializer.startDocument(null, Boolean.valueOf(true));
        serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
        writeMapXml(val, null, serializer);
        serializer.endDocument();
    }

    public static final void writeListXml(List val, OutputStream out) throws XmlPullParserException, IOException {
        XmlSerializer serializer = Xml.newSerializer();
        serializer.setOutput(out, StandardCharsets.UTF_8.name());
        serializer.startDocument(null, Boolean.valueOf(true));
        serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
        writeListXml(val, null, serializer);
        serializer.endDocument();
    }

    public static final void writeMapXml(Map val, String name, XmlSerializer out) throws XmlPullParserException, IOException {
        writeMapXml(val, name, out, null);
    }

    public static final void writeMapXml(Map val, String name, XmlSerializer out, WriteMapCallback callback) throws XmlPullParserException, IOException {
        if (val == null) {
            out.startTag(null, "null");
            out.endTag(null, "null");
            return;
        }
        out.startTag(null, "map");
        if (name != null) {
            out.attribute(null, "name", name);
        }
        writeMapXml(val, out, callback);
        out.endTag(null, "map");
    }

    public static final void writeMapXml(Map val, XmlSerializer out, WriteMapCallback callback) throws XmlPullParserException, IOException {
        if (val != null) {
            for (Entry e : val.entrySet()) {
                writeValueXml(e.getValue(), (String) e.getKey(), out, callback);
            }
        }
    }

    public static final void writeListXml(List val, String name, XmlSerializer out) throws XmlPullParserException, IOException {
        if (val == null) {
            out.startTag(null, "null");
            out.endTag(null, "null");
            return;
        }
        out.startTag(null, "list");
        if (name != null) {
            out.attribute(null, "name", name);
        }
        int N = val.size();
        for (int i = 0; i < N; i++) {
            writeValueXml(val.get(i), null, out);
        }
        out.endTag(null, "list");
    }

    public static final void writeSetXml(Set val, String name, XmlSerializer out) throws XmlPullParserException, IOException {
        if (val == null) {
            out.startTag(null, "null");
            out.endTag(null, "null");
            return;
        }
        out.startTag(null, "set");
        if (name != null) {
            out.attribute(null, "name", name);
        }
        for (Object v : val) {
            writeValueXml(v, null, out);
        }
        out.endTag(null, "set");
    }

    public static final void writeByteArrayXml(byte[] val, String name, XmlSerializer out) throws XmlPullParserException, IOException {
        if (val == null) {
            out.startTag(null, "null");
            out.endTag(null, "null");
            return;
        }
        out.startTag(null, "byte-array");
        if (name != null) {
            out.attribute(null, "name", name);
        }
        out.attribute(null, "num", Integer.toString(N));
        StringBuilder sb = new StringBuilder(val.length * 2);
        for (int b : val) {
            int h = (b >> 4) & 15;
            sb.append((char) (h >= 10 ? (97 + h) - 10 : 48 + h));
            h = b & 15;
            sb.append((char) (h >= 10 ? (97 + h) - 10 : 48 + h));
        }
        out.text(sb.toString());
        out.endTag(null, "byte-array");
    }

    public static final void writeIntArrayXml(int[] val, String name, XmlSerializer out) throws XmlPullParserException, IOException {
        if (val == null) {
            out.startTag(null, "null");
            out.endTag(null, "null");
            return;
        }
        out.startTag(null, "int-array");
        if (name != null) {
            out.attribute(null, "name", name);
        }
        out.attribute(null, "num", Integer.toString(N));
        for (int num : val) {
            out.startTag(null, "item");
            out.attribute(null, "value", Integer.toString(num));
            out.endTag(null, "item");
        }
        out.endTag(null, "int-array");
    }

    public static final void writeLongArrayXml(long[] val, String name, XmlSerializer out) throws XmlPullParserException, IOException {
        if (val == null) {
            out.startTag(null, "null");
            out.endTag(null, "null");
            return;
        }
        out.startTag(null, "long-array");
        if (name != null) {
            out.attribute(null, "name", name);
        }
        out.attribute(null, "num", Integer.toString(N));
        for (long l : val) {
            out.startTag(null, "item");
            out.attribute(null, "value", Long.toString(l));
            out.endTag(null, "item");
        }
        out.endTag(null, "long-array");
    }

    public static final void writeDoubleArrayXml(double[] val, String name, XmlSerializer out) throws XmlPullParserException, IOException {
        if (val == null) {
            out.startTag(null, "null");
            out.endTag(null, "null");
            return;
        }
        out.startTag(null, "double-array");
        if (name != null) {
            out.attribute(null, "name", name);
        }
        out.attribute(null, "num", Integer.toString(N));
        for (double d : val) {
            out.startTag(null, "item");
            out.attribute(null, "value", Double.toString(d));
            out.endTag(null, "item");
        }
        out.endTag(null, "double-array");
    }

    public static final void writeStringArrayXml(String[] val, String name, XmlSerializer out) throws XmlPullParserException, IOException {
        if (val == null) {
            out.startTag(null, "null");
            out.endTag(null, "null");
            return;
        }
        out.startTag(null, "string-array");
        if (name != null) {
            out.attribute(null, "name", name);
        }
        out.attribute(null, "num", Integer.toString(N));
        for (String attribute : val) {
            out.startTag(null, "item");
            out.attribute(null, "value", attribute);
            out.endTag(null, "item");
        }
        out.endTag(null, "string-array");
    }

    public static final void writeBooleanArrayXml(boolean[] val, String name, XmlSerializer out) throws XmlPullParserException, IOException {
        if (val == null) {
            out.startTag(null, "null");
            out.endTag(null, "null");
            return;
        }
        out.startTag(null, "boolean-array");
        if (name != null) {
            out.attribute(null, "name", name);
        }
        out.attribute(null, "num", Integer.toString(N));
        for (boolean bool : val) {
            out.startTag(null, "item");
            out.attribute(null, "value", Boolean.toString(bool));
            out.endTag(null, "item");
        }
        out.endTag(null, "boolean-array");
    }

    public static final void writeValueXml(Object v, String name, XmlSerializer out) throws XmlPullParserException, IOException {
        writeValueXml(v, name, out, null);
    }

    private static final void writeValueXml(Object v, String name, XmlSerializer out, WriteMapCallback callback) throws XmlPullParserException, IOException {
        if (v == null) {
            out.startTag(null, "null");
            if (name != null) {
                out.attribute(null, "name", name);
            }
            out.endTag(null, "null");
        } else if (v instanceof String) {
            out.startTag(null, "string");
            if (name != null) {
                out.attribute(null, "name", name);
            }
            out.text(v.toString());
            out.endTag(null, "string");
        } else {
            String typeStr;
            if (v instanceof Integer) {
                typeStr = "int";
            } else if (v instanceof Long) {
                typeStr = "long";
            } else if (v instanceof Float) {
                typeStr = "float";
            } else if (v instanceof Double) {
                typeStr = "double";
            } else if (v instanceof Boolean) {
                typeStr = "boolean";
            } else if (v instanceof byte[]) {
                writeByteArrayXml((byte[]) v, name, out);
                return;
            } else if (v instanceof int[]) {
                writeIntArrayXml((int[]) v, name, out);
                return;
            } else if (v instanceof long[]) {
                writeLongArrayXml((long[]) v, name, out);
                return;
            } else if (v instanceof double[]) {
                writeDoubleArrayXml((double[]) v, name, out);
                return;
            } else if (v instanceof String[]) {
                writeStringArrayXml((String[]) v, name, out);
                return;
            } else if (v instanceof boolean[]) {
                writeBooleanArrayXml((boolean[]) v, name, out);
                return;
            } else if (v instanceof Map) {
                writeMapXml((Map) v, name, out);
                return;
            } else if (v instanceof List) {
                writeListXml((List) v, name, out);
                return;
            } else if (v instanceof Set) {
                writeSetXml((Set) v, name, out);
                return;
            } else if (v instanceof CharSequence) {
                out.startTag(null, "string");
                if (name != null) {
                    out.attribute(null, "name", name);
                }
                out.text(v.toString());
                out.endTag(null, "string");
                return;
            } else if (callback != null) {
                callback.writeUnknownObject(v, name, out);
                return;
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("writeValueXml: unable to write value ");
                stringBuilder.append(v);
                throw new RuntimeException(stringBuilder.toString());
            }
            out.startTag(null, typeStr);
            if (name != null) {
                out.attribute(null, "name", name);
            }
            out.attribute(null, "value", v.toString());
            out.endTag(null, typeStr);
        }
    }

    public static final HashMap<String, ?> readMapXml(InputStream in) throws XmlPullParserException, IOException {
        XmlPullParser parser = Xml.newPullParser();
        parser.setInput(in, StandardCharsets.UTF_8.name());
        return (HashMap) readValueXml(parser, new String[1]);
    }

    public static final ArrayList readListXml(InputStream in) throws XmlPullParserException, IOException {
        XmlPullParser parser = Xml.newPullParser();
        parser.setInput(in, StandardCharsets.UTF_8.name());
        return (ArrayList) readValueXml(parser, new String[1]);
    }

    public static final HashSet readSetXml(InputStream in) throws XmlPullParserException, IOException {
        XmlPullParser parser = Xml.newPullParser();
        parser.setInput(in, null);
        return (HashSet) readValueXml(parser, new String[1]);
    }

    public static final HashMap<String, ?> readThisMapXml(XmlPullParser parser, String endTag, String[] name) throws XmlPullParserException, IOException {
        return readThisMapXml(parser, endTag, name, null);
    }

    public static final HashMap<String, ?> readThisMapXml(XmlPullParser parser, String endTag, String[] name, ReadMapCallback callback) throws XmlPullParserException, IOException {
        HashMap<String, Object> map = new HashMap();
        int eventType = parser.getEventType();
        while (true) {
            StringBuilder stringBuilder;
            if (eventType == 2) {
                map.put(name[0], readThisValueXml(parser, name, callback, false));
            } else if (eventType == 3) {
                if (parser.getName().equals(endTag)) {
                    return map;
                }
                stringBuilder = new StringBuilder();
                stringBuilder.append("Expected ");
                stringBuilder.append(endTag);
                stringBuilder.append(" end tag at: ");
                stringBuilder.append(parser.getName());
                throw new XmlPullParserException(stringBuilder.toString());
            }
            eventType = parser.next();
            if (eventType == 1) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Document ended before ");
                stringBuilder.append(endTag);
                stringBuilder.append(" end tag");
                throw new XmlPullParserException(stringBuilder.toString());
            }
        }
    }

    public static final ArrayMap<String, ?> readThisArrayMapXml(XmlPullParser parser, String endTag, String[] name, ReadMapCallback callback) throws XmlPullParserException, IOException {
        ArrayMap<String, Object> map = new ArrayMap();
        int eventType = parser.getEventType();
        while (true) {
            StringBuilder stringBuilder;
            if (eventType == 2) {
                map.put(name[0], readThisValueXml(parser, name, callback, true));
            } else if (eventType == 3) {
                if (parser.getName().equals(endTag)) {
                    return map;
                }
                stringBuilder = new StringBuilder();
                stringBuilder.append("Expected ");
                stringBuilder.append(endTag);
                stringBuilder.append(" end tag at: ");
                stringBuilder.append(parser.getName());
                throw new XmlPullParserException(stringBuilder.toString());
            }
            eventType = parser.next();
            if (eventType == 1) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Document ended before ");
                stringBuilder.append(endTag);
                stringBuilder.append(" end tag");
                throw new XmlPullParserException(stringBuilder.toString());
            }
        }
    }

    public static final ArrayList readThisListXml(XmlPullParser parser, String endTag, String[] name) throws XmlPullParserException, IOException {
        return readThisListXml(parser, endTag, name, null, false);
    }

    private static final ArrayList readThisListXml(XmlPullParser parser, String endTag, String[] name, ReadMapCallback callback, boolean arrayMap) throws XmlPullParserException, IOException {
        ArrayList list = new ArrayList();
        int eventType = parser.getEventType();
        while (true) {
            StringBuilder stringBuilder;
            if (eventType == 2) {
                list.add(readThisValueXml(parser, name, callback, arrayMap));
            } else if (eventType == 3) {
                if (parser.getName().equals(endTag)) {
                    return list;
                }
                stringBuilder = new StringBuilder();
                stringBuilder.append("Expected ");
                stringBuilder.append(endTag);
                stringBuilder.append(" end tag at: ");
                stringBuilder.append(parser.getName());
                throw new XmlPullParserException(stringBuilder.toString());
            }
            eventType = parser.next();
            if (eventType == 1) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Document ended before ");
                stringBuilder.append(endTag);
                stringBuilder.append(" end tag");
                throw new XmlPullParserException(stringBuilder.toString());
            }
        }
    }

    public static final HashSet readThisSetXml(XmlPullParser parser, String endTag, String[] name) throws XmlPullParserException, IOException {
        return readThisSetXml(parser, endTag, name, null, false);
    }

    private static final HashSet readThisSetXml(XmlPullParser parser, String endTag, String[] name, ReadMapCallback callback, boolean arrayMap) throws XmlPullParserException, IOException {
        HashSet set = new HashSet();
        int eventType = parser.getEventType();
        while (true) {
            StringBuilder stringBuilder;
            if (eventType == 2) {
                set.add(readThisValueXml(parser, name, callback, arrayMap));
            } else if (eventType == 3) {
                if (parser.getName().equals(endTag)) {
                    return set;
                }
                stringBuilder = new StringBuilder();
                stringBuilder.append("Expected ");
                stringBuilder.append(endTag);
                stringBuilder.append(" end tag at: ");
                stringBuilder.append(parser.getName());
                throw new XmlPullParserException(stringBuilder.toString());
            }
            eventType = parser.next();
            if (eventType == 1) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Document ended before ");
                stringBuilder.append(endTag);
                stringBuilder.append(" end tag");
                throw new XmlPullParserException(stringBuilder.toString());
            }
        }
    }

    public static final byte[] readThisByteArrayXml(XmlPullParser parser, String endTag, String[] name) throws XmlPullParserException, IOException {
        try {
            String values;
            StringBuilder stringBuilder;
            int num = Integer.parseInt(parser.getAttributeValue(null, "num"));
            byte[] array = new byte[num];
            int eventType = parser.getEventType();
            while (true) {
                if (eventType == 4) {
                    if (num > 0) {
                        values = parser.getText();
                        if (values == null || values.length() != num * 2) {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Invalid value found in byte-array: ");
                            stringBuilder.append(values);
                        } else {
                            for (int i = 0; i < num; i++) {
                                int nibbleHigh;
                                int nibbleLow;
                                char nibbleHighChar = values.charAt(2 * i);
                                char nibbleLowChar = values.charAt((2 * i) + 1);
                                if (nibbleHighChar > 'a') {
                                    nibbleHigh = (nibbleHighChar - 97) + 10;
                                } else {
                                    nibbleHigh = nibbleHighChar - 48;
                                }
                                if (nibbleLowChar > 'a') {
                                    nibbleLow = (nibbleLowChar - 97) + 10;
                                } else {
                                    nibbleLow = nibbleLowChar - 48;
                                }
                                array[i] = (byte) (((nibbleHigh & 15) << 4) | (nibbleLow & 15));
                            }
                        }
                    }
                } else if (eventType == 3) {
                    if (parser.getName().equals(endTag)) {
                        return array;
                    }
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Expected ");
                    stringBuilder.append(endTag);
                    stringBuilder.append(" end tag at: ");
                    stringBuilder.append(parser.getName());
                    throw new XmlPullParserException(stringBuilder.toString());
                }
                eventType = parser.next();
                if (eventType == 1) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Document ended before ");
                    stringBuilder.append(endTag);
                    stringBuilder.append(" end tag");
                    throw new XmlPullParserException(stringBuilder.toString());
                }
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid value found in byte-array: ");
            stringBuilder.append(values);
            throw new XmlPullParserException(stringBuilder.toString());
        } catch (NullPointerException e) {
            throw new XmlPullParserException("Need num attribute in byte-array");
        } catch (NumberFormatException e2) {
            throw new XmlPullParserException("Not a number in num attribute in byte-array");
        }
    }

    public static final int[] readThisIntArrayXml(XmlPullParser parser, String endTag, String[] name) throws XmlPullParserException, IOException {
        try {
            int num = Integer.parseInt(parser.getAttributeValue(null, "num"));
            parser.next();
            int[] array = new int[num];
            int i = 0;
            int eventType = parser.getEventType();
            while (true) {
                StringBuilder stringBuilder;
                if (eventType == 2) {
                    if (parser.getName().equals("item")) {
                        try {
                            array[i] = Integer.parseInt(parser.getAttributeValue(null, "value"));
                        } catch (NullPointerException e) {
                            throw new XmlPullParserException("Need value attribute in item");
                        } catch (NumberFormatException e2) {
                            throw new XmlPullParserException("Not a number in value attribute in item");
                        }
                    }
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Expected item tag at: ");
                    stringBuilder.append(parser.getName());
                    throw new XmlPullParserException(stringBuilder.toString());
                } else if (eventType == 3) {
                    if (parser.getName().equals(endTag)) {
                        return array;
                    }
                    if (parser.getName().equals("item")) {
                        i++;
                    } else {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Expected ");
                        stringBuilder.append(endTag);
                        stringBuilder.append(" end tag at: ");
                        stringBuilder.append(parser.getName());
                        throw new XmlPullParserException(stringBuilder.toString());
                    }
                }
                eventType = parser.next();
                if (eventType == 1) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Document ended before ");
                    stringBuilder.append(endTag);
                    stringBuilder.append(" end tag");
                    throw new XmlPullParserException(stringBuilder.toString());
                }
            }
        } catch (NullPointerException e3) {
            throw new XmlPullParserException("Need num attribute in int-array");
        } catch (NumberFormatException e4) {
            throw new XmlPullParserException("Not a number in num attribute in int-array");
        }
    }

    public static final long[] readThisLongArrayXml(XmlPullParser parser, String endTag, String[] name) throws XmlPullParserException, IOException {
        try {
            int num = Integer.parseInt(parser.getAttributeValue(null, "num"));
            parser.next();
            long[] array = new long[num];
            int i = 0;
            int eventType = parser.getEventType();
            while (true) {
                StringBuilder stringBuilder;
                if (eventType == 2) {
                    if (parser.getName().equals("item")) {
                        try {
                            array[i] = Long.parseLong(parser.getAttributeValue(null, "value"));
                        } catch (NullPointerException e) {
                            throw new XmlPullParserException("Need value attribute in item");
                        } catch (NumberFormatException e2) {
                            throw new XmlPullParserException("Not a number in value attribute in item");
                        }
                    }
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Expected item tag at: ");
                    stringBuilder.append(parser.getName());
                    throw new XmlPullParserException(stringBuilder.toString());
                } else if (eventType == 3) {
                    if (parser.getName().equals(endTag)) {
                        return array;
                    }
                    if (parser.getName().equals("item")) {
                        i++;
                    } else {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Expected ");
                        stringBuilder.append(endTag);
                        stringBuilder.append(" end tag at: ");
                        stringBuilder.append(parser.getName());
                        throw new XmlPullParserException(stringBuilder.toString());
                    }
                }
                eventType = parser.next();
                if (eventType == 1) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Document ended before ");
                    stringBuilder.append(endTag);
                    stringBuilder.append(" end tag");
                    throw new XmlPullParserException(stringBuilder.toString());
                }
            }
        } catch (NullPointerException e3) {
            throw new XmlPullParserException("Need num attribute in long-array");
        } catch (NumberFormatException e4) {
            throw new XmlPullParserException("Not a number in num attribute in long-array");
        }
    }

    public static final double[] readThisDoubleArrayXml(XmlPullParser parser, String endTag, String[] name) throws XmlPullParserException, IOException {
        try {
            int num = Integer.parseInt(parser.getAttributeValue(null, "num"));
            parser.next();
            double[] array = new double[num];
            int i = 0;
            int eventType = parser.getEventType();
            while (true) {
                StringBuilder stringBuilder;
                if (eventType == 2) {
                    if (parser.getName().equals("item")) {
                        try {
                            array[i] = Double.parseDouble(parser.getAttributeValue(null, "value"));
                        } catch (NullPointerException e) {
                            throw new XmlPullParserException("Need value attribute in item");
                        } catch (NumberFormatException e2) {
                            throw new XmlPullParserException("Not a number in value attribute in item");
                        }
                    }
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Expected item tag at: ");
                    stringBuilder.append(parser.getName());
                    throw new XmlPullParserException(stringBuilder.toString());
                } else if (eventType == 3) {
                    if (parser.getName().equals(endTag)) {
                        return array;
                    }
                    if (parser.getName().equals("item")) {
                        i++;
                    } else {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Expected ");
                        stringBuilder.append(endTag);
                        stringBuilder.append(" end tag at: ");
                        stringBuilder.append(parser.getName());
                        throw new XmlPullParserException(stringBuilder.toString());
                    }
                }
                eventType = parser.next();
                if (eventType == 1) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Document ended before ");
                    stringBuilder.append(endTag);
                    stringBuilder.append(" end tag");
                    throw new XmlPullParserException(stringBuilder.toString());
                }
            }
        } catch (NullPointerException e3) {
            throw new XmlPullParserException("Need num attribute in double-array");
        } catch (NumberFormatException e4) {
            throw new XmlPullParserException("Not a number in num attribute in double-array");
        }
    }

    public static final String[] readThisStringArrayXml(XmlPullParser parser, String endTag, String[] name) throws XmlPullParserException, IOException {
        try {
            int num = Integer.parseInt(parser.getAttributeValue(null, "num"));
            parser.next();
            String[] array = new String[num];
            int i = 0;
            int eventType = parser.getEventType();
            while (true) {
                StringBuilder stringBuilder;
                if (eventType == 2) {
                    if (parser.getName().equals("item")) {
                        try {
                            array[i] = parser.getAttributeValue(null, "value");
                        } catch (NullPointerException e) {
                            throw new XmlPullParserException("Need value attribute in item");
                        } catch (NumberFormatException e2) {
                            throw new XmlPullParserException("Not a number in value attribute in item");
                        }
                    }
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Expected item tag at: ");
                    stringBuilder.append(parser.getName());
                    throw new XmlPullParserException(stringBuilder.toString());
                } else if (eventType == 3) {
                    if (parser.getName().equals(endTag)) {
                        return array;
                    }
                    if (parser.getName().equals("item")) {
                        i++;
                    } else {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Expected ");
                        stringBuilder.append(endTag);
                        stringBuilder.append(" end tag at: ");
                        stringBuilder.append(parser.getName());
                        throw new XmlPullParserException(stringBuilder.toString());
                    }
                }
                eventType = parser.next();
                if (eventType == 1) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Document ended before ");
                    stringBuilder.append(endTag);
                    stringBuilder.append(" end tag");
                    throw new XmlPullParserException(stringBuilder.toString());
                }
            }
        } catch (NullPointerException e3) {
            throw new XmlPullParserException("Need num attribute in string-array");
        } catch (NumberFormatException e4) {
            throw new XmlPullParserException("Not a number in num attribute in string-array");
        }
    }

    public static final boolean[] readThisBooleanArrayXml(XmlPullParser parser, String endTag, String[] name) throws XmlPullParserException, IOException {
        try {
            int num = Integer.parseInt(parser.getAttributeValue(null, "num"));
            parser.next();
            boolean[] array = new boolean[num];
            int i = 0;
            int eventType = parser.getEventType();
            while (true) {
                StringBuilder stringBuilder;
                if (eventType == 2) {
                    if (parser.getName().equals("item")) {
                        try {
                            array[i] = Boolean.parseBoolean(parser.getAttributeValue(null, "value"));
                        } catch (NullPointerException e) {
                            throw new XmlPullParserException("Need value attribute in item");
                        } catch (NumberFormatException e2) {
                            throw new XmlPullParserException("Not a number in value attribute in item");
                        }
                    }
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Expected item tag at: ");
                    stringBuilder.append(parser.getName());
                    throw new XmlPullParserException(stringBuilder.toString());
                } else if (eventType == 3) {
                    if (parser.getName().equals(endTag)) {
                        return array;
                    }
                    if (parser.getName().equals("item")) {
                        i++;
                    } else {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Expected ");
                        stringBuilder.append(endTag);
                        stringBuilder.append(" end tag at: ");
                        stringBuilder.append(parser.getName());
                        throw new XmlPullParserException(stringBuilder.toString());
                    }
                }
                eventType = parser.next();
                if (eventType == 1) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Document ended before ");
                    stringBuilder.append(endTag);
                    stringBuilder.append(" end tag");
                    throw new XmlPullParserException(stringBuilder.toString());
                }
            }
        } catch (NullPointerException e3) {
            throw new XmlPullParserException("Need num attribute in string-array");
        } catch (NumberFormatException e4) {
            throw new XmlPullParserException("Not a number in num attribute in string-array");
        }
    }

    public static final Object readValueXml(XmlPullParser parser, String[] name) throws XmlPullParserException, IOException {
        int eventType = parser.getEventType();
        while (eventType != 2) {
            StringBuilder stringBuilder;
            if (eventType == 3) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Unexpected end tag at: ");
                stringBuilder.append(parser.getName());
                throw new XmlPullParserException(stringBuilder.toString());
            } else if (eventType != 4) {
                eventType = parser.next();
                if (eventType == 1) {
                    throw new XmlPullParserException("Unexpected end of document");
                }
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Unexpected text: ");
                stringBuilder.append(parser.getText());
                throw new XmlPullParserException(stringBuilder.toString());
            }
        }
        return readThisValueXml(parser, name, null, false);
    }

    private static final Object readThisValueXml(XmlPullParser parser, String[] name, ReadMapCallback callback, boolean arrayMap) throws XmlPullParserException, IOException {
        Object res;
        int next;
        int eventType;
        StringBuilder stringBuilder;
        String valueName = parser.getAttributeValue(null, "name");
        String tagName = parser.getName();
        if (tagName.equals("null")) {
            res = null;
        } else if (tagName.equals("string")) {
            String value = "";
            while (true) {
                next = parser.next();
                eventType = next;
                if (next == 1) {
                    throw new XmlPullParserException("Unexpected end of document in <string>");
                } else if (eventType == 3) {
                    if (parser.getName().equals("string")) {
                        name[0] = valueName;
                        return value;
                    }
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Unexpected end tag in <string>: ");
                    stringBuilder.append(parser.getName());
                    throw new XmlPullParserException(stringBuilder.toString());
                } else if (eventType == 4) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(value);
                    stringBuilder2.append(parser.getText());
                    value = stringBuilder2.toString();
                } else if (eventType == 2) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Unexpected start tag in <string>: ");
                    stringBuilder.append(parser.getName());
                    throw new XmlPullParserException(stringBuilder.toString());
                }
            }
        } else {
            res = readThisPrimitiveValueXml(parser, tagName);
            Object res2 = res;
            if (res != null) {
                res = res2;
            } else if (tagName.equals("byte-array")) {
                res = readThisByteArrayXml(parser, "byte-array", name);
                name[0] = valueName;
                return res;
            } else if (tagName.equals("int-array")) {
                res = readThisIntArrayXml(parser, "int-array", name);
                name[0] = valueName;
                return res;
            } else if (tagName.equals("long-array")) {
                res = readThisLongArrayXml(parser, "long-array", name);
                name[0] = valueName;
                return res;
            } else if (tagName.equals("double-array")) {
                res = readThisDoubleArrayXml(parser, "double-array", name);
                name[0] = valueName;
                return res;
            } else if (tagName.equals("string-array")) {
                res = readThisStringArrayXml(parser, "string-array", name);
                name[0] = valueName;
                return res;
            } else if (tagName.equals("boolean-array")) {
                res = readThisBooleanArrayXml(parser, "boolean-array", name);
                name[0] = valueName;
                return res;
            } else if (tagName.equals("map")) {
                parser.next();
                if (arrayMap) {
                    res = readThisArrayMapXml(parser, "map", name, callback);
                } else {
                    res = readThisMapXml(parser, "map", name, callback);
                }
                name[0] = valueName;
                return res;
            } else if (tagName.equals("list")) {
                parser.next();
                res = readThisListXml(parser, "list", name, callback, arrayMap);
                name[0] = valueName;
                return res;
            } else if (tagName.equals("set")) {
                parser.next();
                res = readThisSetXml(parser, "set", name, callback, arrayMap);
                name[0] = valueName;
                return res;
            } else if (callback != null) {
                res = callback.readThisUnknownObjectXml(parser, tagName);
                name[0] = valueName;
                return res;
            } else {
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Unknown tag: ");
                stringBuilder3.append(tagName);
                throw new XmlPullParserException(stringBuilder3.toString());
            }
        }
        while (true) {
            next = parser.next();
            eventType = next;
            if (next == 1) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Unexpected end of document in <");
                stringBuilder.append(tagName);
                stringBuilder.append(">");
                throw new XmlPullParserException(stringBuilder.toString());
            } else if (eventType == 3) {
                if (parser.getName().equals(tagName)) {
                    name[0] = valueName;
                    return res;
                }
                stringBuilder = new StringBuilder();
                stringBuilder.append("Unexpected end tag in <");
                stringBuilder.append(tagName);
                stringBuilder.append(">: ");
                stringBuilder.append(parser.getName());
                throw new XmlPullParserException(stringBuilder.toString());
            } else if (eventType == 4) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Unexpected text in <");
                stringBuilder.append(tagName);
                stringBuilder.append(">: ");
                stringBuilder.append(parser.getName());
                throw new XmlPullParserException(stringBuilder.toString());
            } else if (eventType == 2) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Unexpected start tag in <");
                stringBuilder.append(tagName);
                stringBuilder.append(">: ");
                stringBuilder.append(parser.getName());
                throw new XmlPullParserException(stringBuilder.toString());
            }
        }
    }

    private static final Object readThisPrimitiveValueXml(XmlPullParser parser, String tagName) throws XmlPullParserException, IOException {
        StringBuilder stringBuilder;
        try {
            if (tagName.equals("int")) {
                return Integer.valueOf(Integer.parseInt(parser.getAttributeValue(null, "value")));
            }
            if (tagName.equals("long")) {
                return Long.valueOf(parser.getAttributeValue(null, "value"));
            }
            if (tagName.equals("float")) {
                return new Float(parser.getAttributeValue(null, "value"));
            }
            if (tagName.equals("double")) {
                return new Double(parser.getAttributeValue(null, "value"));
            }
            if (tagName.equals("boolean")) {
                return Boolean.valueOf(parser.getAttributeValue(null, "value"));
            }
            return null;
        } catch (NullPointerException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Need value attribute in <");
            stringBuilder.append(tagName);
            stringBuilder.append(">");
            throw new XmlPullParserException(stringBuilder.toString());
        } catch (NumberFormatException e2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Not a number in value attribute in <");
            stringBuilder.append(tagName);
            stringBuilder.append(">");
            throw new XmlPullParserException(stringBuilder.toString());
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:11:0x003c  */
    /* JADX WARNING: Removed duplicated region for block: B:6:0x000e  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static final void beginDocument(XmlPullParser parser, String firstElementName) throws XmlPullParserException, IOException {
        int type;
        while (true) {
            int next = parser.next();
            type = next;
            if (next == 2 || type == 1) {
                if (type == 2) {
                    throw new XmlPullParserException("No start tag found");
                } else if (!parser.getName().equals(firstElementName)) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Unexpected start tag: found ");
                    stringBuilder.append(parser.getName());
                    stringBuilder.append(", expected ");
                    stringBuilder.append(firstElementName);
                    throw new XmlPullParserException(stringBuilder.toString());
                } else {
                    return;
                }
            }
        }
        if (type == 2) {
        }
    }

    public static final void nextElement(XmlPullParser parser) throws XmlPullParserException, IOException {
        while (true) {
            int next = parser.next();
            int type = next;
            if (next == 2 || type == 1) {
                return;
            }
        }
    }

    public static boolean nextElementWithin(XmlPullParser parser, int outerDepth) throws IOException, XmlPullParserException {
        while (true) {
            int type = parser.next();
            if (type != 1 && (type != 3 || parser.getDepth() != outerDepth)) {
                if (type == 2 && parser.getDepth() == outerDepth + 1) {
                    return true;
                }
            }
        }
        return false;
    }

    public static int readIntAttribute(XmlPullParser in, String name, int defaultValue) {
        String value = in.getAttributeValue(null, name);
        if (TextUtils.isEmpty(value)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static int readIntAttribute(XmlPullParser in, String name) throws IOException {
        String value = in.getAttributeValue(null, name);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("problem parsing ");
            stringBuilder.append(name);
            stringBuilder.append("=");
            stringBuilder.append(value);
            stringBuilder.append(" as int");
            throw new ProtocolException(stringBuilder.toString());
        }
    }

    public static void writeIntAttribute(XmlSerializer out, String name, int value) throws IOException {
        out.attribute(null, name, Integer.toString(value));
    }

    public static long readLongAttribute(XmlPullParser in, String name, long defaultValue) {
        String value = in.getAttributeValue(null, name);
        if (TextUtils.isEmpty(value)) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static long readLongAttribute(XmlPullParser in, String name) throws IOException {
        String value = in.getAttributeValue(null, name);
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("problem parsing ");
            stringBuilder.append(name);
            stringBuilder.append("=");
            stringBuilder.append(value);
            stringBuilder.append(" as long");
            throw new ProtocolException(stringBuilder.toString());
        }
    }

    public static void writeLongAttribute(XmlSerializer out, String name, long value) throws IOException {
        out.attribute(null, name, Long.toString(value));
    }

    public static float readFloatAttribute(XmlPullParser in, String name) throws IOException {
        String value = in.getAttributeValue(null, name);
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("problem parsing ");
            stringBuilder.append(name);
            stringBuilder.append("=");
            stringBuilder.append(value);
            stringBuilder.append(" as long");
            throw new ProtocolException(stringBuilder.toString());
        }
    }

    public static void writeFloatAttribute(XmlSerializer out, String name, float value) throws IOException {
        out.attribute(null, name, Float.toString(value));
    }

    public static boolean readBooleanAttribute(XmlPullParser in, String name) {
        return Boolean.parseBoolean(in.getAttributeValue(null, name));
    }

    public static boolean readBooleanAttribute(XmlPullParser in, String name, boolean defaultValue) {
        String value = in.getAttributeValue(null, name);
        if (value == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }

    public static void writeBooleanAttribute(XmlSerializer out, String name, boolean value) throws IOException {
        out.attribute(null, name, Boolean.toString(value));
    }

    public static Uri readUriAttribute(XmlPullParser in, String name) {
        String value = in.getAttributeValue(null, name);
        if (value != null) {
            return Uri.parse(value);
        }
        return null;
    }

    public static void writeUriAttribute(XmlSerializer out, String name, Uri value) throws IOException {
        if (value != null) {
            out.attribute(null, name, value.toString());
        }
    }

    public static String readStringAttribute(XmlPullParser in, String name) {
        return in.getAttributeValue(null, name);
    }

    public static void writeStringAttribute(XmlSerializer out, String name, CharSequence value) throws IOException {
        if (value != null) {
            out.attribute(null, name, value.toString());
        }
    }

    public static byte[] readByteArrayAttribute(XmlPullParser in, String name) {
        String value = in.getAttributeValue(null, name);
        if (value != null) {
            return Base64.decode(value, 0);
        }
        return null;
    }

    public static void writeByteArrayAttribute(XmlSerializer out, String name, byte[] value) throws IOException {
        if (value != null) {
            out.attribute(null, name, Base64.encodeToString(value, 0));
        }
    }

    public static Bitmap readBitmapAttribute(XmlPullParser in, String name) {
        byte[] value = readByteArrayAttribute(in, name);
        if (value != null) {
            return BitmapFactory.decodeByteArray(value, 0, value.length);
        }
        return null;
    }

    @Deprecated
    public static void writeBitmapAttribute(XmlSerializer out, String name, Bitmap value) throws IOException {
        if (value != null) {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            value.compress(CompressFormat.PNG, 90, os);
            writeByteArrayAttribute(out, name, os.toByteArray());
        }
    }
}
