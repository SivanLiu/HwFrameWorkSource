package com.android.server.devicepolicy;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import com.android.server.devicepolicy.PolicyStruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class XmlUtil {
    private static final String SUPPORT_MULTIPLE_USERS = "support_multiple_users";
    private static final String TAG = XmlUtil.class.getSimpleName();

    public static Bundle readStateFromXml(XmlPullParser parser, String targetTag, String attrName) throws IOException, XmlPullParserException {
        if (TextUtils.isEmpty(targetTag)) {
            String str = TAG;
            Log.w(str, "warning_mdm: invalid policy tag:" + targetTag);
            return null;
        }
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type == 1 || (type == 3 && parser.getDepth() <= outerDepth)) {
                String str2 = TAG;
                Log.w(str2, "warning_mdm: can't find policy tag:" + targetTag);
            } else if (type != 3 && type != 4 && targetTag.equals(parser.getName())) {
                boolean result = Boolean.parseBoolean(parser.getAttributeValue(null, attrName));
                Bundle bundle = new Bundle();
                bundle.putBoolean(targetTag, result);
                return bundle;
            }
        }
        String str22 = TAG;
        Log.w(str22, "warning_mdm: can't find policy tag:" + targetTag);
        return null;
    }

    public static void writeListToXml(XmlSerializer out, String outerTag, String innerTag, String attrName, List<String> someList) throws IllegalArgumentException, IllegalStateException, IOException {
        if (someList != null && !someList.isEmpty()) {
            out.startTag(null, outerTag);
            for (String value : someList) {
                out.startTag(null, innerTag);
                out.attribute(null, attrName, value);
                out.endTag(null, innerTag);
            }
            out.endTag(null, outerTag);
        }
    }

    public static void writeOnePolicy(XmlSerializer out, PolicyStruct.PolicyItem item) throws IOException {
        if (PolicyStruct.PolicyItem.isValidItem(item) && item.hasAnyNonNullAttribute() && isEffectiveItem(item, false)) {
            PolicyStruct.PolicyItem node = item;
            String tag = node.getPolicyTag();
            String str = TAG;
            HwLog.d(str, "writeOnePolicy- Tag: " + tag);
            if (!(node.getItemType() == PolicyStruct.PolicyType.LIST || node.getItemType() == PolicyStruct.PolicyType.CONFIGLIST)) {
                out.startTag(null, tag);
            }
            Bundle attrs = node.getAttributes();
            boolean supportMultipleUsers = node.isSuppportMultipleUsers();
            for (String attrName : attrs.keySet()) {
                int i = AnonymousClass1.$SwitchMap$com$android$server$devicepolicy$PolicyStruct$PolicyType[node.getItemType().ordinal()];
                if (i == 1) {
                    writeAttrValueToXml(null, attrName, String.valueOf(attrs.getBoolean(attrName)), out);
                    if (supportMultipleUsers) {
                        writeAttrValueToXml(null, SUPPORT_MULTIPLE_USERS, String.valueOf(true), out);
                    }
                } else if (i == 2) {
                    writeAttrValueToXml(null, attrName, attrs.getString(attrName), out);
                    if (supportMultipleUsers) {
                        writeAttrValueToXml(null, SUPPORT_MULTIPLE_USERS, String.valueOf(true), out);
                    }
                } else if (i == 3 || i == 4) {
                    writeListToXml(tag, attrName, attrs.getStringArrayList(attrName), out);
                }
            }
            while (node.hasLeafItems()) {
                Iterator<PolicyStruct.PolicyItem> it = node.getChildItem().iterator();
                while (it.hasNext()) {
                    node = it.next();
                    if (node.getItemType() == PolicyStruct.PolicyType.LIST || node.getItemType() == PolicyStruct.PolicyType.CONFIGLIST) {
                        out.startTag(null, tag);
                        if (attrs.keySet().isEmpty() && supportMultipleUsers) {
                            out.attribute(null, SUPPORT_MULTIPLE_USERS, String.valueOf(true));
                        }
                    }
                    writeOnePolicy(out, node);
                    if (node.getItemType() == PolicyStruct.PolicyType.LIST || node.getItemType() == PolicyStruct.PolicyType.CONFIGLIST) {
                        out.endTag(null, tag);
                    }
                }
            }
            if (node.getItemType() != PolicyStruct.PolicyType.LIST && node.getItemType() != PolicyStruct.PolicyType.CONFIGLIST) {
                out.endTag(null, tag);
            }
        }
    }

    /* renamed from: com.android.server.devicepolicy.XmlUtil$1  reason: invalid class name */
    static /* synthetic */ class AnonymousClass1 {
        static final /* synthetic */ int[] $SwitchMap$com$android$server$devicepolicy$PolicyStruct$PolicyType = new int[PolicyStruct.PolicyType.values().length];

        static {
            try {
                $SwitchMap$com$android$server$devicepolicy$PolicyStruct$PolicyType[PolicyStruct.PolicyType.STATE.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$server$devicepolicy$PolicyStruct$PolicyType[PolicyStruct.PolicyType.CONFIGURATION.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$android$server$devicepolicy$PolicyStruct$PolicyType[PolicyStruct.PolicyType.LIST.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$android$server$devicepolicy$PolicyStruct$PolicyType[PolicyStruct.PolicyType.CONFIGLIST.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:7:0x0022  */
    private static boolean isEffectiveItem(PolicyStruct.PolicyItem item, boolean result) {
        if (!PolicyStruct.PolicyItem.isValidItem(item) || !item.hasAnyNonNullAttribute()) {
            return false;
        }
        Bundle attrs = item.getAttributes();
        for (String attrName : attrs.keySet()) {
            int i = AnonymousClass1.$SwitchMap$com$android$server$devicepolicy$PolicyStruct$PolicyType[item.getItemType().ordinal()];
            if (i == 1) {
                return attrs.getBoolean(attrName);
            }
            if (i != 2) {
                if (i == 3 || i == 4) {
                    if (attrs.getStringArrayList(attrName).size() > 0) {
                        return true;
                    }
                    return false;
                }
                while (r2.hasNext()) {
                }
            } else if (attrs.getString(attrName) != null) {
                return true;
            } else {
                return false;
            }
        }
        Iterator<PolicyStruct.PolicyItem> it = item.getChildItem().iterator();
        while (it.hasNext()) {
            result = result || isEffectiveItem(it.next(), result);
        }
        return result;
    }

    private static void writeListToXml(String tag, String attrName, List<String> lists, XmlSerializer out) throws IOException {
        if (lists == null || lists.isEmpty()) {
            String str = TAG;
            HwLog.w(str, "writePolicy- attrName: [" + attrName + "] has no list attrValue");
            return;
        }
        for (String attrValue : lists) {
            writeAttrValueToXml(tag, attrName, attrValue, out);
        }
    }

    private static void writeAttrValueToXml(String tag, String attrName, String attrValue, XmlSerializer out) throws IOException {
        if (TextUtils.isEmpty(attrValue)) {
            String str = TAG;
            HwLog.w(str, "writePolicy- attrName: [" + attrName + "] has no string attrValue");
        } else if (!TextUtils.isEmpty(tag)) {
            out.startTag(null, tag);
            out.attribute(null, attrName, attrValue);
            out.endTag(null, tag);
        } else {
            out.attribute(null, attrName, attrValue);
        }
    }

    public static boolean readItems(XmlPullParser parser, PolicyStruct.PolicyItem item) throws IOException, XmlPullParserException {
        String xmlTag = parser.getName();
        String str = TAG;
        HwLog.d(str, "xml tag:" + xmlTag);
        boolean result = true;
        if (!PolicyStruct.PolicyItem.isValidItem(item)) {
            return false;
        }
        String tag = item.getPolicyTag();
        String str2 = TAG;
        HwLog.d(str2, "[policyTag]: " + tag);
        if (tag == null || !tag.equals(xmlTag)) {
            return false;
        }
        Bundle attrs = item.getAttributes();
        boolean hasRead = false;
        for (String attrName : attrs.keySet()) {
            int i = AnonymousClass1.$SwitchMap$com$android$server$devicepolicy$PolicyStruct$PolicyType[item.getItemType().ordinal()];
            if (i == 1) {
                Object state = readStateFromXml(attrName, parser);
                if (state instanceof Boolean) {
                    hasRead = true;
                    attrs.putBoolean(attrName, ((Boolean) state).booleanValue());
                } else {
                    result = false;
                }
            } else if (i == 2) {
                String cfgResult = readCfgFromXml(attrName, parser);
                if (cfgResult != null) {
                    if (!item.hasLeafItems()) {
                        hasRead = true;
                    }
                    attrs.putString(attrName, cfgResult);
                } else {
                    result = false;
                }
            } else if (i == 3 || i == 4) {
                ArrayList<String> attrLists = readListFromXml(tag, attrName, parser);
                if (attrLists != null) {
                    hasRead = true;
                    attrs.putStringArrayList(attrName, attrLists);
                } else {
                    result = false;
                }
            }
        }
        while (!hasRead) {
            int outerType = parser.next();
            if (outerType != 1) {
                if (outerType == 2) {
                    break;
                }
            } else {
                break;
            }
        }
        PolicyStruct.PolicyItem node = item;
        String str3 = TAG;
        HwLog.d(str3, item.getPolicyName() + ", has children: " + item.hasLeafItems() + " ,number: " + item.getChildItem().size());
        while (node.hasLeafItems()) {
            Iterator<PolicyStruct.PolicyItem> it = node.getChildItem().iterator();
            while (true) {
                if (it.hasNext()) {
                    PolicyStruct.PolicyItem leaf = it.next();
                    node = leaf;
                    if (!readItems(parser, leaf)) {
                        return false;
                    }
                }
            }
        }
        return result;
    }

    private static Object readStateFromXml(String attrName, XmlPullParser parser) {
        if (TextUtils.isEmpty(attrName) || parser == null) {
            HwLog.w(TAG, "readStateFromXml - invalid input para");
            return -1;
        }
        String attrValue = parser.getAttributeValue(null, attrName);
        if (TextUtils.isEmpty(attrValue) || (!attrValue.equals("true") && !attrValue.equals("false"))) {
            return -1;
        }
        return Boolean.valueOf(Boolean.parseBoolean(attrValue));
    }

    private static ArrayList<String> readListFromXml(String tag, String attrName, XmlPullParser parser) throws IOException, XmlPullParserException {
        ArrayList<String> lists = new ArrayList<>();
        if (TextUtils.isEmpty(attrName) || parser == null) {
            HwLog.w(TAG, "readListFromXml - invalid input para");
            return null;
        }
        int outerDepth = parser.getDepth();
        int outerType = parser.getEventType();
        do {
            String outerTag = parser.getName();
            String str = TAG;
            HwLog.d(str, "readListFromXml tag:[" + outerTag + "]");
            if (tag.equals(outerTag)) {
                String value = parser.getAttributeValue(null, attrName);
                if (value != null) {
                    lists.add(value);
                } else {
                    String str2 = TAG;
                    HwLog.w(str2, "invalid list attrName: " + attrName);
                }
            }
            while (true) {
                int oldLineNumber = parser.getLineNumber();
                int newType = parser.next();
                if (parser.getDepth() != outerDepth && newType == 3) {
                    break;
                }
                int newLineNumber = parser.getLineNumber();
                if ((outerType == 1 || oldLineNumber != newLineNumber || newType == 2) && newType != 4) {
                    break;
                }
            }
            if (parser.getEventType() == 1) {
                break;
            }
        } while (parser.getDepth() == outerDepth);
        if (lists.isEmpty()) {
            return null;
        }
        return lists;
    }

    private static String readCfgFromXml(String attrName, XmlPullParser parser) throws IOException, XmlPullParserException {
        if (!TextUtils.isEmpty(attrName) && parser != null) {
            return parser.getAttributeValue(null, attrName);
        }
        HwLog.w(TAG, "readCfgFromXml - invalid input para");
        return null;
    }
}
