package com.android.server.devicepolicy;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import com.android.server.devicepolicy.PolicyStruct.PolicyItem;
import com.android.server.devicepolicy.PolicyStruct.PolicyType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class XmlUtil {
    private static final String TAG = XmlUtil.class.getSimpleName();

    public static Bundle readStateFromXml(XmlPullParser parser, String targetTag, String attrName) throws IOException, XmlPullParserException {
        if (TextUtils.isEmpty(targetTag)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("warning_mdm: invalid policy tag:");
            stringBuilder.append(targetTag);
            Log.w(str, stringBuilder.toString());
            return null;
        }
        String str2;
        StringBuilder stringBuilder2;
        int outerDepth = parser.getDepth();
        while (true) {
            int next = parser.next();
            int type = next;
            if (next == 1 || (type == 3 && parser.getDepth() <= outerDepth)) {
                str2 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("warning_mdm: can't find policy tag:");
                stringBuilder2.append(targetTag);
                Log.w(str2, stringBuilder2.toString());
            } else if (type != 3) {
                if (type != 4) {
                    if (targetTag.equals(parser.getName())) {
                        boolean result = Boolean.parseBoolean(parser.getAttributeValue(null, attrName));
                        Bundle bundle = new Bundle();
                        bundle.putBoolean(targetTag, result);
                        return bundle;
                    }
                }
            }
        }
        str2 = TAG;
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("warning_mdm: can't find policy tag:");
        stringBuilder2.append(targetTag);
        Log.w(str2, stringBuilder2.toString());
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

    /* JADX WARNING: Missing block: B:35:0x00c4, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static void writeOnePolicy(XmlSerializer out, PolicyItem item) throws IOException {
        if (PolicyItem.isValidItem(item) && item.hasAnyNonNullAttribute() && isEffectiveItem(item, false)) {
            PolicyItem node = item;
            String tag = node.getPolicyTag();
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("writeOnePolicy- Tag: ");
            stringBuilder.append(tag);
            HwLog.d(str, stringBuilder.toString());
            if (node.getItemType() != PolicyType.LIST) {
                out.startTag(null, tag);
            }
            Bundle attrs = node.getAttributes();
            for (String attrName : attrs.keySet()) {
                switch (node.getItemType()) {
                    case STATE:
                        writeAttrValueToXml(null, attrName, String.valueOf(attrs.getBoolean(attrName)), out);
                        break;
                    case CONFIGURATION:
                        writeAttrValueToXml(null, attrName, attrs.getString(attrName), out);
                        break;
                    case LIST:
                        writeListToXml(tag, attrName, attrs.getStringArrayList(attrName), out);
                        break;
                    default:
                        break;
                }
            }
            while (node.hasLeafItems()) {
                Iterator it = node.getChildItem().iterator();
                while (it.hasNext()) {
                    node = (PolicyItem) it.next();
                    if (node.getItemType() == PolicyType.LIST) {
                        out.startTag(null, tag);
                    }
                    writeOnePolicy(out, node);
                    if (node.getItemType() == PolicyType.LIST) {
                        out.endTag(null, tag);
                    }
                }
            }
            if (node.getItemType() != PolicyType.LIST) {
                out.endTag(null, tag);
            }
        }
    }

    private static boolean isEffectiveItem(PolicyItem item, boolean result) {
        boolean z = false;
        if (!PolicyItem.isValidItem(item) || !item.hasAnyNonNullAttribute()) {
            return false;
        }
        Bundle attrs = item.getAttributes();
        for (String attrName : attrs.keySet()) {
            switch (item.getItemType()) {
                case STATE:
                    return attrs.getBoolean(attrName);
                case CONFIGURATION:
                    if (attrs.getString(attrName) != null) {
                        z = true;
                    }
                    return z;
                case LIST:
                    if (attrs.getStringArrayList(attrName).size() > 0) {
                        z = true;
                    }
                    return z;
                default:
            }
        }
        Iterator it = item.getChildItem().iterator();
        while (it.hasNext()) {
            boolean z2 = result || isEffectiveItem((PolicyItem) it.next(), result);
            result = z2;
        }
        return result;
    }

    private static void writeListToXml(String tag, String attrName, List<String> lists, XmlSerializer out) throws IOException {
        if (lists == null || lists.isEmpty()) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("writePolicy- attrName: [");
            stringBuilder.append(attrName);
            stringBuilder.append("] has no list attrValue");
            HwLog.w(str, stringBuilder.toString());
            return;
        }
        for (String attrValue : lists) {
            writeAttrValueToXml(tag, attrName, attrValue, out);
        }
    }

    private static void writeAttrValueToXml(String tag, String attrName, String attrValue, XmlSerializer out) throws IOException {
        if (TextUtils.isEmpty(attrValue)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("writePolicy- attrName: [");
            stringBuilder.append(attrName);
            stringBuilder.append("] has no string attrValue");
            HwLog.w(str, stringBuilder.toString());
            return;
        }
        if (TextUtils.isEmpty(tag)) {
            out.attribute(null, attrName, attrValue);
        } else {
            out.startTag(null, tag);
            out.attribute(null, attrName, attrValue);
            out.endTag(null, tag);
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:37:0x00ee  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static boolean readItems(XmlPullParser parser, PolicyItem item) throws IOException, XmlPullParserException {
        String xmlTag = parser.getName();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("xml tag:");
        stringBuilder.append(xmlTag);
        HwLog.d(str, stringBuilder.toString());
        boolean result = true;
        if (!PolicyItem.isValidItem(item)) {
            return false;
        }
        String tag = item.getPolicyTag();
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("[policyTag]: ");
        stringBuilder2.append(tag);
        HwLog.d(str2, stringBuilder2.toString());
        if (tag == null || !tag.equals(xmlTag)) {
            return false;
        }
        Iterator it;
        PolicyItem node;
        String str3;
        StringBuilder stringBuilder3;
        Bundle attrs = item.getAttributes();
        boolean hasRead = false;
        for (String attrName : attrs.keySet()) {
            switch (item.getItemType()) {
                case STATE:
                    Object state = readStateFromXml(attrName, parser);
                    if (!(state instanceof Boolean)) {
                        result = false;
                        break;
                    }
                    hasRead = true;
                    attrs.putBoolean(attrName, ((Boolean) state).booleanValue());
                    break;
                case CONFIGURATION:
                    String cfgResult = readCfgFromXml(attrName, parser);
                    if (cfgResult == null) {
                        result = false;
                        break;
                    }
                    if (!item.hasLeafItems()) {
                        hasRead = true;
                    }
                    attrs.putString(attrName, cfgResult);
                    break;
                case LIST:
                    ArrayList<String> attrLists = readListFromXml(tag, attrName, parser);
                    if (attrLists == null) {
                        result = false;
                        break;
                    }
                    hasRead = true;
                    attrs.putStringArrayList(attrName, attrLists);
                    break;
                default:
                    break;
            }
        }
        while (!hasRead) {
            int next = parser.next();
            int outerType = next;
            if (!(next == 1 || outerType == 2)) {
            }
            node = item;
            str3 = TAG;
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append(item.getPolicyName());
            stringBuilder3.append(", has children: ");
            stringBuilder3.append(item.hasLeafItems());
            stringBuilder3.append(" ,number: ");
            stringBuilder3.append(item.getChildItem().size());
            HwLog.d(str3, stringBuilder3.toString());
            while (node.hasLeafItems()) {
                it = node.getChildItem().iterator();
                while (it.hasNext()) {
                    PolicyItem leaf = (PolicyItem) it.next();
                    node = leaf;
                    if (!readItems(parser, leaf)) {
                        return false;
                    }
                }
            }
            return result;
        }
        node = item;
        str3 = TAG;
        stringBuilder3 = new StringBuilder();
        stringBuilder3.append(item.getPolicyName());
        stringBuilder3.append(", has children: ");
        stringBuilder3.append(item.hasLeafItems());
        stringBuilder3.append(" ,number: ");
        stringBuilder3.append(item.getChildItem().size());
        HwLog.d(str3, stringBuilder3.toString());
        while (node.hasLeafItems()) {
        }
        return result;
    }

    private static Object readStateFromXml(String attrName, XmlPullParser parser) {
        if (TextUtils.isEmpty(attrName) || parser == null) {
            HwLog.w(TAG, "readStateFromXml - invalid input para");
            return Integer.valueOf(-1);
        }
        String attrValue = parser.getAttributeValue(null, attrName);
        if (TextUtils.isEmpty(attrValue) || (!attrValue.equals("true") && !attrValue.equals("false"))) {
            return Integer.valueOf(-1);
        }
        return Boolean.valueOf(Boolean.parseBoolean(attrValue));
    }

    private static ArrayList<String> readListFromXml(String tag, String attrName, XmlPullParser parser) throws IOException, XmlPullParserException {
        ArrayList<String> lists = new ArrayList();
        ArrayList<String> arrayList = null;
        if (TextUtils.isEmpty(attrName) || parser == null) {
            HwLog.w(TAG, "readListFromXml - invalid input para");
            return null;
        }
        int outerDepth = parser.getDepth();
        int outerType = parser.getEventType();
        do {
            String outerTag = parser.getName();
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("readListFromXml tag:[");
            stringBuilder.append(outerTag);
            stringBuilder.append("]");
            HwLog.d(str, stringBuilder.toString());
            if (tag.equals(outerTag)) {
                str = parser.getAttributeValue(null, attrName);
                if (str != null) {
                    lists.add(str);
                } else {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("invalid list attrName: ");
                    stringBuilder2.append(attrName);
                    HwLog.w(str2, stringBuilder2.toString());
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
        if (!lists.isEmpty()) {
            arrayList = lists;
        }
        return arrayList;
    }

    private static String readCfgFromXml(String attrName, XmlPullParser parser) throws IOException, XmlPullParserException {
        if (!TextUtils.isEmpty(attrName) && parser != null) {
            return parser.getAttributeValue(null, attrName);
        }
        HwLog.w(TAG, "readCfgFromXml - invalid input para");
        return null;
    }
}
