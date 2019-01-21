package huawei.cust;

import android.telephony.Rlog;
import android.text.TextUtils;
import java.util.HashMap;
import java.util.Map;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class HwCarrierConfigXmlUtils {
    private static final String ARRAY_NAME = "array";
    private static final String BOOL_NAME = "bool";
    private static final String DICT_NAME = "dict";
    private static final String DOUBLE_NAME = "double";
    private static final HashMap EMPTY = new HashMap();
    private static final String HASH_VALUE = "hashvalue";
    private static final String INT_NAME = "int";
    private static final String KEY_NAME = "key";
    private static final String LOG_TAG = "HwCarrierConfigXmlUtils";
    private static final String START_TAG = "config";
    private static final String STRING_NAME = "string";

    public static Map read(XmlPullParser input) throws Exception {
        Map result = new HashMap();
        if (input == null) {
            return result;
        }
        while (true) {
            int next = input.next();
            int e = next;
            if (next == 1) {
                return result;
            }
            if (e == 2) {
                if (START_TAG.equalsIgnoreCase(input.getName())) {
                    String hashValue = input.getAttributeValue(null, HASH_VALUE);
                    if (!TextUtils.isEmpty(hashValue)) {
                        result.put(HASH_VALUE, hashValue);
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("parse hashvalue attribute = ");
                        stringBuilder.append(hashValue);
                        log(stringBuilder.toString());
                    }
                } else {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("begin parse name ");
                    stringBuilder2.append(input.getName());
                    log(stringBuilder2.toString());
                    int outerDepth = input.getDepth();
                    String startTag = input.getName();
                    String[] tagName = new String[1];
                    int event;
                    do {
                        int next2 = input.next();
                        event = next2;
                        if (next2 == 1 || (event == 3 && input.getDepth() >= outerDepth)) {
                            break;
                        }
                    } while (event != 2);
                    result.putAll(readXml(input, startTag, tagName));
                }
            }
        }
    }

    private static Map readXml(XmlPullParser parser, String tagName, String[] outKey) throws Exception {
        HwCarrierConfigDictValue dictValue = new HwCarrierConfigDictValue();
        if (readComplexValueXml(parser, tagName, outKey, dictValue)) {
            return (Map) dictValue.getData();
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("readXml tagName ");
        stringBuilder.append(tagName);
        stringBuilder.append(" is error");
        loge(stringBuilder.toString());
        return EMPTY;
    }

    private static boolean readComplexValueXml(XmlPullParser parser, String tagName, String[] outKey, HWCarrierConfigComplexValue complexValue) {
        StringBuilder stringBuilder;
        try {
            int eventType = parser.getEventType();
            do {
                if (eventType == 2) {
                    complexValue.addData(outKey[0], readValueXml(parser, outKey));
                } else if (eventType == 3) {
                    if (parser.getName().equals(tagName)) {
                        return true;
                    }
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Expected ");
                    stringBuilder.append(tagName);
                    stringBuilder.append(" end tag, but it is ");
                    stringBuilder.append(parser.getName());
                    loge(stringBuilder.toString());
                    return false;
                }
                eventType = parser.next();
            } while (eventType != 1);
        } catch (Exception e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Exception in  ");
            stringBuilder.append(tagName);
            stringBuilder.append(" Error: ");
            stringBuilder.append(e.toString());
            loge(stringBuilder.toString());
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("no found ended before ");
        stringBuilder2.append(tagName);
        stringBuilder2.append(" end tag");
        loge(stringBuilder2.toString());
        return false;
    }

    private static Object readValueXml(XmlPullParser parser, String[] outKey) throws XmlPullParserException {
        String key = parser.getAttributeValue(null, "key");
        String typeName = parser.getName();
        int i = -1;
        StringBuilder stringBuilder;
        StringBuilder stringBuilder2;
        try {
            switch (typeName.hashCode()) {
                case -1325958191:
                    if (typeName.equals(DOUBLE_NAME)) {
                        i = 3;
                        break;
                    }
                    break;
                case -891985903:
                    if (typeName.equals(STRING_NAME)) {
                        i = 0;
                        break;
                    }
                    break;
                case 104431:
                    if (typeName.equals(INT_NAME)) {
                        i = 1;
                        break;
                    }
                    break;
                case 3029738:
                    if (typeName.equals(BOOL_NAME)) {
                        i = 2;
                        break;
                    }
                    break;
                case 3083190:
                    if (typeName.equals(DICT_NAME)) {
                        i = 4;
                        break;
                    }
                    break;
                case 93090393:
                    if (typeName.equals(ARRAY_NAME)) {
                        i = 5;
                        break;
                    }
                    break;
                default:
                    break;
            }
            Object result;
            switch (i) {
                case 0:
                    outKey[0] = key;
                    return parser.nextText();
                case 1:
                    outKey[0] = key;
                    return Integer.valueOf(Integer.parseInt(parser.nextText().trim()));
                case 2:
                    outKey[0] = key;
                    return Boolean.valueOf(parser.nextText().trim());
                case 3:
                    outKey[0] = key;
                    return Double.valueOf(parser.nextText().trim());
                case 4:
                    parser.next();
                    HwCarrierConfigDictValue dictValue = new HwCarrierConfigDictValue();
                    if (readComplexValueXml(parser, DICT_NAME, outKey, dictValue)) {
                        result = dictValue.getData();
                        outKey[0] = key;
                        return result;
                    }
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("readComplexValueXml is Error for <");
                    stringBuilder.append(typeName);
                    stringBuilder.append(">");
                    throw new XmlPullParserException(stringBuilder.toString());
                case 5:
                    parser.next();
                    HwCarrierConfigArrayValue arrayValue = new HwCarrierConfigArrayValue();
                    if (readComplexValueXml(parser, ARRAY_NAME, outKey, arrayValue)) {
                        result = arrayValue.getData();
                        outKey[0] = key;
                        return result;
                    }
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("readComplexValueXml is Error for <");
                    stringBuilder.append(typeName);
                    stringBuilder.append(">");
                    throw new XmlPullParserException(stringBuilder.toString());
                default:
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("No unknown type for parse ");
                    stringBuilder3.append(typeName);
                    loge(stringBuilder3.toString());
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("No unknown type for parse <");
                    stringBuilder2.append(typeName);
                    stringBuilder2.append(">");
                    throw new XmlPullParserException(stringBuilder2.toString());
            }
        } catch (Exception e) {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("It is Error for parse <");
            stringBuilder2.append(typeName);
            stringBuilder2.append(">");
            loge(stringBuilder2.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("It is Error for parse <");
            stringBuilder.append(typeName);
            stringBuilder.append(">");
            throw new XmlPullParserException(stringBuilder.toString());
        }
    }

    private static void log(String msg) {
        Rlog.d(LOG_TAG, msg);
    }

    private static void loge(String msg) {
        Rlog.e(LOG_TAG, msg);
    }
}
