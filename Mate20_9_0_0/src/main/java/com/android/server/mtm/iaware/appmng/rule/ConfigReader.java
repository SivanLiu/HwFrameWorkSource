package com.android.server.mtm.iaware.appmng.rule;

import android.app.mtm.iaware.appmng.AppMngConstant.AppCleanSource;
import android.app.mtm.iaware.appmng.AppMngConstant.AppFreezeSource;
import android.app.mtm.iaware.appmng.AppMngConstant.AppIoLimitSource;
import android.app.mtm.iaware.appmng.AppMngConstant.AppMngFeature;
import android.app.mtm.iaware.appmng.AppMngConstant.AppStartReason;
import android.app.mtm.iaware.appmng.AppMngConstant.AppStartSource;
import android.app.mtm.iaware.appmng.AppMngConstant.BroadcastSource;
import android.app.mtm.iaware.appmng.AppMngConstant.EnumWithDesc;
import android.content.Context;
import android.rms.iaware.AwareLog;
import android.rms.iaware.IAwareDecrypt;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Xml;
import com.android.server.mtm.iaware.appmng.rule.RuleNode.XmlValue;
import com.android.server.mtm.iaware.appmng.rule.RuleParserUtil.AppMngTag;
import com.android.server.mtm.iaware.appmng.rule.RuleParserUtil.AppStartTag;
import com.android.server.mtm.iaware.appmng.rule.RuleParserUtil.BroadcastTag;
import com.android.server.mtm.iaware.appmng.rule.RuleParserUtil.TagEnum;
import com.android.server.rms.iaware.appmng.AwareDefaultConfigList;
import huawei.cust.HwCfgFilePolicy;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map.Entry;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class ConfigReader {
    private static final String ABBR_ALL = "all";
    private static final String ABBR_ALL_BROAD = "allbroad";
    private static final String ABBR_ALL_E_R = "all-R";
    private static final String ABBR_ALL_E_RBB = "all-RBb";
    private static final String ABBR_EXCLUDE_BG_CHECK = "bgcheck";
    private static final String CONFIG_FILEPATH = "/system/etc/appmng_config.xml";
    private static final String CONFIG_UPDATEPATH = "/data/system/iaware/appmng_config.xml";
    private static final String CUST_FILEPATH = "/data/cust/xml/appmng_config_cust.xml";
    private static final String CUST_FILEPATH_RELATED = "xml/appmng_config_cust.xml";
    private static final String DEFAULT_VALUE = "default";
    private static final String DIVIDER_OF_VALUE = ",";
    private static final int INDEX_CORRECT = 1;
    private static final int INDEX_UPDATED = 0;
    private static final boolean IS_ABROAD = AwareDefaultConfigList.isAbroadArea();
    private static final int MIN_SUPPORTED_VERSION = 84;
    private static final String TAG = "AppMng.ConfigReader";
    private static final int UNINIT_VALUE = -1;
    private static final int XML_ATTR_DEFAULT_NUM = 2;
    private static final String XML_ATTR_NAME = "name";
    private static final String XML_ATTR_SCOPE = "scope";
    private static final String XML_ATTR_VALUE = "value";
    private static final String XML_ATTR_VERSION = "version";
    private static final String XML_TAG_ABROAD_LIST = "listabroad";
    private static final String XML_TAG_CONFIG = "config";
    private static final String XML_TAG_FEATURE = "feature";
    private static final String XML_TAG_HWSTOP = "hwstop";
    private static final String XML_TAG_IAWARE = "iaware";
    private static final String XML_TAG_INDEX = "index";
    private static final String XML_TAG_ITEM = "item";
    private static final String XML_TAG_LIST = "list";
    private static final String XML_TAG_MISC = "misc";
    private static final String XML_TAG_POLICY = "policy";
    private static final String XML_TAG_PROCESSLIST = "processlist";
    private static final String XML_TAG_RULE = "rule";
    private static final String XML_TAG_WEIGHT = "weight";
    private ArrayMap<AppMngFeature, ArrayMap<EnumWithDesc, Config>> mAllConfig = new ArrayMap();
    private ArrayMap<AppMngFeature, ArrayMap<EnumWithDesc, ArrayMap<String, ListItem>>> mAllList = new ArrayMap();
    private ArrayMap<AppMngFeature, ArrayMap<String, ArrayList<String>>> mAllMisc = new ArrayMap();
    private ArrayMap<AppMngFeature, ArrayMap<EnumWithDesc, ArrayMap<String, ListItem>>> mAllProcessList = new ArrayMap();
    private ArraySet<String> mBGCheckExcludedPkg = new ArraySet();
    private ArrayMap<AppMngFeature, ArrayList<Boolean>> mConfigCorrectTag = new ArrayMap();
    private boolean mIsPolicyMissing = true;
    private int mVersion = -1;

    /* renamed from: com.android.server.mtm.iaware.appmng.rule.ConfigReader$2 */
    static /* synthetic */ class AnonymousClass2 {
        static final /* synthetic */ int[] $SwitchMap$android$app$mtm$iaware$appmng$AppMngConstant$AppMngFeature = new int[AppMngFeature.values().length];

        static {
            try {
                $SwitchMap$android$app$mtm$iaware$appmng$AppMngConstant$AppMngFeature[AppMngFeature.APP_START.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$android$app$mtm$iaware$appmng$AppMngConstant$AppMngFeature[AppMngFeature.APP_CLEAN.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$android$app$mtm$iaware$appmng$AppMngConstant$AppMngFeature[AppMngFeature.APP_FREEZE.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$android$app$mtm$iaware$appmng$AppMngConstant$AppMngFeature[AppMngFeature.APP_IOLIMIT.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$android$app$mtm$iaware$appmng$AppMngConstant$AppMngFeature[AppMngFeature.BROADCAST.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
        }
    }

    public ConfigReader() {
        for (Object put : AppMngFeature.values()) {
            this.mConfigCorrectTag.put(put, new ArrayList<Boolean>() {
                {
                    add(Boolean.valueOf(false));
                    add(Boolean.valueOf(true));
                }
            });
        }
    }

    public void parseFile(AppMngFeature targetFeature, Context context) {
        File file = new File(CONFIG_UPDATEPATH);
        if (file.exists()) {
            AwareLog.i(TAG, "reading cloud config !");
            parseFileInternal(file, targetFeature, context);
        }
        if (!isConfigComplete(targetFeature) || this.mAllConfig.isEmpty()) {
            file = loadCustConfigFile();
            if (file.exists()) {
                AwareLog.i(TAG, "reading cust config !");
                parseFileInternal(file, targetFeature, context);
            }
            if (!isConfigComplete(targetFeature) || this.mAllConfig.isEmpty()) {
                file = new File(CONFIG_FILEPATH);
                if (file.exists()) {
                    AwareLog.i(TAG, "reading default config !");
                    parseFileInternal(file, targetFeature, context);
                }
                if (!isConfigComplete(targetFeature) || this.mAllConfig.isEmpty()) {
                    AwareLog.e(TAG, "no valid config !");
                }
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:24:0x005a A:{Catch:{ XmlPullParserException -> 0x00cb, IOException -> 0x00bc, NumberFormatException -> 0x00af, all -> 0x00ad }} */
    /* JADX WARNING: Removed duplicated region for block: B:32:0x008a A:{Catch:{ XmlPullParserException -> 0x00cb, IOException -> 0x00bc, NumberFormatException -> 0x00af, all -> 0x00ad }} */
    /* JADX WARNING: Removed duplicated region for block: B:25:0x005b A:{Catch:{ XmlPullParserException -> 0x00cb, IOException -> 0x00bc, NumberFormatException -> 0x00af, all -> 0x00ad }} */
    /* JADX WARNING: Removed duplicated region for block: B:24:0x005a A:{Catch:{ XmlPullParserException -> 0x00cb, IOException -> 0x00bc, NumberFormatException -> 0x00af, all -> 0x00ad }} */
    /* JADX WARNING: Removed duplicated region for block: B:32:0x008a A:{Catch:{ XmlPullParserException -> 0x00cb, IOException -> 0x00bc, NumberFormatException -> 0x00af, all -> 0x00ad }} */
    /* JADX WARNING: Removed duplicated region for block: B:25:0x005b A:{Catch:{ XmlPullParserException -> 0x00cb, IOException -> 0x00bc, NumberFormatException -> 0x00af, all -> 0x00ad }} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void parseFileInternal(File file, AppMngFeature targetFeature, Context context) {
        InputStream rawIs = null;
        InputStream is = null;
        XmlPullParser parser = null;
        AppMngFeature feature = null;
        int next;
        int eventType;
        try {
            rawIs = new FileInputStream(file);
            is = IAwareDecrypt.decryptInputStream(context, rawIs);
            parser = Xml.newPullParser();
            if (parser != null) {
                if (is != null) {
                    parser.setInput(is, StandardCharsets.UTF_8.name());
                    while (true) {
                        next = parser.next();
                        eventType = next;
                        Object obj = 1;
                        if (next != 1) {
                            if (eventType == 2) {
                                String name = parser.getName();
                                int hashCode = name.hashCode();
                                if (hashCode == -1195682923) {
                                    if (name.equals(XML_TAG_IAWARE)) {
                                        switch (obj) {
                                            case null:
                                                break;
                                            case 1:
                                                break;
                                            default:
                                                break;
                                        }
                                    }
                                } else if (hashCode == -979207434) {
                                    if (name.equals(XML_TAG_FEATURE)) {
                                        obj = null;
                                        switch (obj) {
                                            case null:
                                                feature = (AppMngFeature) AppMngFeature.fromString(parser.getAttributeValue(null, "name"));
                                                if (targetFeature != null) {
                                                    if (targetFeature.equals(feature)) {
                                                        parseFeature(parser, feature);
                                                        break;
                                                    }
                                                }
                                                parseFeature(parser, feature);
                                                break;
                                                break;
                                            case 1:
                                                next = Integer.parseInt(parser.getAttributeValue(null, XML_ATTR_VERSION));
                                                if (next >= MIN_SUPPORTED_VERSION) {
                                                    this.mVersion = next;
                                                    break;
                                                }
                                                setConfigFailed(null);
                                                String str = TAG;
                                                StringBuilder stringBuilder = new StringBuilder();
                                                stringBuilder.append("bad version = ");
                                                stringBuilder.append(next);
                                                AwareLog.e(str, stringBuilder.toString());
                                                closeStream(rawIs, is, parser);
                                                return;
                                            default:
                                                break;
                                        }
                                    }
                                }
                                obj = -1;
                                switch (obj) {
                                    case null:
                                        break;
                                    case 1:
                                        break;
                                    default:
                                        break;
                                }
                            }
                        }
                    }
                    closeStream(rawIs, is, parser);
                    return;
                }
            }
            closeStream(rawIs, is, parser);
        } catch (XmlPullParserException e) {
            next = e.getColumnNumber();
            eventType = e.getLineNumber();
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("failed parsing file parser error at line:");
            stringBuilder2.append(eventType);
            stringBuilder2.append(",col:");
            stringBuilder2.append(next);
            AwareLog.e(str2, stringBuilder2.toString());
            setConfigFailed(feature);
        } catch (IOException e2) {
            AwareLog.e(TAG, "failed parsing file IO error ");
            setConfigFailed(feature);
        } catch (NumberFormatException e3) {
            AwareLog.e(TAG, "value number format error");
            setConfigFailed(feature);
        } catch (Throwable th) {
            closeStream(rawIs, is, parser);
        }
    }

    private void closeStream(InputStream rawIs, InputStream is, XmlPullParser parser) {
        if (rawIs != null) {
            try {
                rawIs.close();
            } catch (IOException e) {
                AwareLog.e(TAG, "close file rawIs stream fail!");
            }
        }
        if (is != null) {
            try {
                is.close();
            } catch (IOException e2) {
                AwareLog.e(TAG, "close file is stream fail!");
            }
        }
        if (parser != null) {
            try {
                ((KXmlParser) parser).close();
            } catch (IOException e3) {
                AwareLog.e(TAG, "parser close error");
            }
        }
    }

    /* JADX WARNING: Missing block: B:30:0x0074, code skipped:
            if (r9.equals(XML_TAG_LIST) != false) goto L_0x0082;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void parseFeature(XmlPullParser parser, AppMngFeature feature) throws XmlPullParserException, IOException, NumberFormatException {
        if (feature == null) {
            AwareLog.e(TAG, "feature name is not right or feature is missing");
            return;
        }
        int outerDepth = parser.getDepth();
        ArrayMap<EnumWithDesc, Config> featureConfig = new ArrayMap();
        ArrayMap<EnumWithDesc, ArrayMap<String, ListItem>> featureList = new ArrayMap();
        ArrayMap<EnumWithDesc, ArrayMap<String, ListItem>> featureProcessList = new ArrayMap();
        ArrayMap<String, ArrayList<String>> featureMisc = new ArrayMap();
        while (true) {
            int next = parser.next();
            int eventType = next;
            Object obj = 1;
            if (next != 1 && (3 != eventType || parser.getDepth() > outerDepth)) {
                if (eventType == 2) {
                    String tagName = parser.getName();
                    if (tagName != null) {
                        String configName = parser.getAttributeValue(null, "name");
                        if (configName != null) {
                            switch (tagName.hashCode()) {
                                case -1354792126:
                                    if (tagName.equals(XML_TAG_CONFIG)) {
                                        obj = null;
                                        break;
                                    }
                                case 3322014:
                                    break;
                                case 3351788:
                                    if (tagName.equals(XML_TAG_MISC)) {
                                        obj = 4;
                                        break;
                                    }
                                case 203227021:
                                    if (tagName.equals(XML_TAG_PROCESSLIST)) {
                                        obj = 3;
                                        break;
                                    }
                                case 749196511:
                                    if (tagName.equals(XML_TAG_ABROAD_LIST)) {
                                        obj = 2;
                                        break;
                                    }
                                default:
                                    obj = -1;
                                    break;
                            }
                            switch (obj) {
                                case null:
                                    parseConfig(parser, configName, feature, featureConfig);
                                    break;
                                case 1:
                                    parseList(parser, configName, feature, featureList);
                                    break;
                                case 2:
                                    if (IS_ABROAD) {
                                        parseList(parser, configName, feature, featureList);
                                        break;
                                    }
                                    break;
                                case 3:
                                    parseList(parser, configName, feature, featureProcessList);
                                    break;
                                case 4:
                                    parseMisc(parser, configName, feature, featureMisc);
                                    break;
                                default:
                                    continue;
                            }
                        }
                    }
                }
            }
        }
        if (isNeedUpdate(feature)) {
            this.mAllConfig.put(feature, featureConfig);
            this.mAllList.put(feature, featureList);
            this.mAllProcessList.put(feature, featureProcessList);
            this.mAllMisc.put(feature, featureMisc);
        }
    }

    private void parseList(XmlPullParser parser, String packageName, AppMngFeature feature, ArrayMap<EnumWithDesc, ArrayMap<String, ListItem>> featureList) throws XmlPullParserException, IOException, NumberFormatException {
        int outerDepth = parser.getDepth();
        while (true) {
            int next = parser.next();
            int eventType = next;
            if (next != 1 && (3 != eventType || parser.getDepth() > outerDepth)) {
                if (eventType == 2 && XML_TAG_ITEM.equals(parser.getName())) {
                    String name = parser.getAttributeValue(null, "name");
                    EnumWithDesc configEnum = getConfig(feature, name);
                    if (configEnum != null) {
                        ArrayMap<String, ListItem> policies = (ArrayMap) featureList.get(configEnum);
                        if (policies == null) {
                            policies = new ArrayMap();
                        }
                        ListItem item = (ListItem) policies.get(packageName);
                        if (item == null) {
                            item = new ListItem();
                        }
                        if (AppStartSource.SYSTEM_BROADCAST.equals(configEnum)) {
                            ArrayMap<String, Integer> complicatePolicy = new ArrayMap();
                            parseBroadcast(parser, complicatePolicy);
                            item.setComplicatePolicy(complicatePolicy);
                        } else {
                            item.setPolicy(Integer.parseInt(parser.getAttributeValue(null, "value")));
                        }
                        if (AppMngFeature.APP_START.equals(feature)) {
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append(configEnum.getDesc());
                            stringBuilder.append(AppStartReason.LIST.getDesc());
                            item.setIndex(stringBuilder.toString());
                        }
                        policies.put(packageName, item);
                        featureList.put(configEnum, policies);
                    } else if (!AppMngFeature.APP_START.equals(feature)) {
                        String str = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("no such config named : ");
                        stringBuilder2.append(name);
                        AwareLog.e(str, stringBuilder2.toString());
                        setConfigFailed(feature);
                        return;
                    } else if (!dealWithAbbreviation(name, packageName, Integer.parseInt(parser.getAttributeValue(null, "value")), featureList)) {
                        setConfigFailed(feature);
                        return;
                    }
                }
            }
        }
    }

    private void parseBroadcast(XmlPullParser parser, ArrayMap<String, Integer> policy) throws XmlPullParserException, IOException, NumberFormatException {
        int outerDepth = parser.getDepth();
        while (true) {
            int next = parser.next();
            int eventType = next;
            if (next == 1) {
                return;
            }
            if (3 == eventType && parser.getDepth() <= outerDepth) {
                return;
            }
            if (eventType == 2 && XML_TAG_ITEM.equals(parser.getName())) {
                policy.put(parser.getAttributeValue(null, "name"), Integer.valueOf(Integer.parseInt(parser.getAttributeValue(null, "value"))));
            }
        }
    }

    private void parseMisc(XmlPullParser parser, String miscName, AppMngFeature feature, ArrayMap<String, ArrayList<String>> featureMisc) throws XmlPullParserException, IOException, NumberFormatException {
        int outerDepth = parser.getDepth();
        while (true) {
            int next = parser.next();
            int eventType = next;
            if (next == 1) {
                return;
            }
            if (3 == eventType && parser.getDepth() <= outerDepth) {
                return;
            }
            if (eventType == 2 && XML_TAG_ITEM.equals(parser.getName())) {
                ArrayList<String> miscList = (ArrayList) featureMisc.get(miscName);
                if (miscList == null) {
                    miscList = new ArrayList();
                }
                String content = parser.nextText();
                if (content != null) {
                    miscList.add(content);
                }
                featureMisc.put(miscName, miscList);
            }
        }
    }

    private void parseConfig(XmlPullParser parser, String configName, AppMngFeature feature, ArrayMap<EnumWithDesc, Config> featureConfig) throws XmlPullParserException, IOException, NumberFormatException {
        ArrayMap<String, String> properties = new ArrayMap();
        properties.put("name", configName);
        String configScope = parser.getAttributeValue(null, "scope");
        if (configScope != null) {
            properties.put("scope", configScope);
        }
        RuleNode head = new RuleNode(null, null);
        EnumWithDesc configEnum = getConfig(feature, configName);
        String str;
        if (configEnum == null) {
            str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("no such config named : ");
            stringBuilder.append(configName);
            AwareLog.e(str, stringBuilder.toString());
            setConfigFailed(feature);
            return;
        }
        ArrayList<RuleNode> heads = new ArrayList();
        heads.add(head);
        parseRules(parser, feature, configEnum, heads);
        if (head.hasChild()) {
            Config config;
            if (feature.equals(AppMngFeature.APP_START)) {
                config = new AppStartRule(null, head);
            } else if (feature.equals(AppMngFeature.BROADCAST)) {
                config = new BroadcastMngRule(null, head);
            } else {
                config = new AppMngRule(properties, head);
            }
            featureConfig.put(configEnum, config);
            return;
        }
        str = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("empty named : ");
        stringBuilder2.append(configName);
        AwareLog.e(str, stringBuilder2.toString());
        setConfigFailed(feature);
    }

    /* JADX WARNING: Missing block: B:83:0x01d3, code skipped:
            android.rms.iaware.AwareLog.e(TAG, "bad type or rawValue");
            setConfigFailed(r2);
     */
    /* JADX WARNING: Missing block: B:84:0x01dd, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:88:0x0200, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void parseRules(XmlPullParser parser, AppMngFeature feature, EnumWithDesc config, ArrayList<RuleNode> heads) throws XmlPullParserException, IOException, NumberFormatException {
        String str;
        StringBuilder stringBuilder;
        String rawValue;
        XmlPullParser xmlPullParser = parser;
        AppMngFeature appMngFeature = feature;
        EnumWithDesc enumWithDesc = config;
        int outerDepth = parser.getDepth();
        loop0:
        while (true) {
            int next = parser.next();
            int eventType = next;
            int i;
            if (next != 1) {
                int i2;
                if (3 == eventType && parser.getDepth() <= outerDepth) {
                    i2 = outerDepth;
                    i = eventType;
                    break;
                }
                if (2 == eventType) {
                    String name;
                    String tagName = parser.getName();
                    boolean isRule = XML_TAG_RULE.equals(tagName);
                    if (isRule) {
                        name = xmlPullParser.getAttributeValue(null, "name");
                    } else if (XML_TAG_POLICY.equals(tagName)) {
                        name = XML_TAG_POLICY;
                        this.mIsPolicyMissing = false;
                    } else {
                        i = eventType;
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("bad tag = ");
                        stringBuilder.append(tagName);
                        AwareLog.e(str, stringBuilder.toString());
                        setConfigFailed(appMngFeature);
                        return;
                    }
                    rawValue = xmlPullParser.getAttributeValue(null, "value");
                    TagEnum type = getType(appMngFeature, name);
                    String str2;
                    if (type == null) {
                        i = eventType;
                        str2 = name;
                        break;
                    } else if (rawValue == null) {
                        i2 = outerDepth;
                        i = eventType;
                        str2 = name;
                        break;
                    } else {
                        String[] strArr;
                        String[] values = fastSplit(rawValue, ",");
                        ArrayList<RuleNode> nextHeads = new ArrayList();
                        boolean isItemAdded = true;
                        int i3 = 0;
                        while (i3 < values.length) {
                            if (values[i3] == null) {
                                i = eventType;
                                str2 = name;
                                strArr = values;
                                break loop0;
                            } else if (values[i3].isEmpty()) {
                                i2 = outerDepth;
                                i = eventType;
                                str2 = name;
                                strArr = values;
                                break loop0;
                            } else {
                                XmlValue xmlValue;
                                String str3;
                                if (type.isStringInXml()) {
                                    xmlValue = new XmlValue(values[i3]);
                                } else {
                                    xmlValue = new XmlValue(Integer.parseInt(values[i3]));
                                }
                                RuleNode node = new RuleNode(type, xmlValue);
                                i2 = outerDepth;
                                if (AppMngFeature.APP_START.equals(appMngFeature) != 0) {
                                    if (isRule) {
                                        i = eventType;
                                        str2 = name;
                                        strArr = values;
                                    } else {
                                        i = eventType;
                                        outerDepth = xmlPullParser.getAttributeValue(null, XML_TAG_INDEX);
                                        str2 = name;
                                        name = xmlPullParser.getAttributeValue(null, XML_TAG_HWSTOP);
                                        eventType = new StringBuilder();
                                        strArr = values;
                                        eventType.append(config.getDesc());
                                        eventType.append(outerDepth);
                                        xmlValue.setIndex(eventType.toString());
                                        if (name != null) {
                                            xmlValue.setHwStop(Integer.parseInt(name));
                                        }
                                    }
                                    outerDepth = heads.iterator();
                                    while (outerDepth.hasNext() != 0) {
                                        boolean z = isItemAdded && ((RuleNode) outerDepth.next()).addChildItemSorted(type, node);
                                        isItemAdded = z;
                                        nextHeads.add(node);
                                    }
                                    str3 = null;
                                } else {
                                    i = eventType;
                                    str2 = name;
                                    strArr = values;
                                    if (isRule) {
                                        str3 = null;
                                    } else {
                                        str3 = null;
                                        outerDepth = xmlPullParser.getAttributeValue(null, XML_TAG_WEIGHT);
                                        int weight = -1;
                                        if (outerDepth != 0) {
                                            weight = Integer.parseInt(outerDepth);
                                        }
                                        xmlValue.setWeight(weight);
                                    }
                                    outerDepth = heads.iterator();
                                    while (outerDepth.hasNext()) {
                                        boolean z2 = isItemAdded && ((RuleNode) outerDepth.next()).addChildItem(type, node);
                                        isItemAdded = z2;
                                        nextHeads.add(node);
                                    }
                                }
                                if (isItemAdded) {
                                    i3++;
                                    RuleNode ruleNode = node;
                                    outerDepth = i2;
                                    name = str2;
                                    values = strArr;
                                    String node2 = str3;
                                    eventType = i;
                                } else {
                                    AwareLog.e(TAG, "rules in same level must have same type");
                                    setConfigFailed(appMngFeature);
                                    return;
                                }
                            }
                        }
                        i2 = outerDepth;
                        i = eventType;
                        str2 = name;
                        strArr = values;
                        if (isRule) {
                            this.mIsPolicyMissing = true;
                            parseRules(xmlPullParser, appMngFeature, enumWithDesc, nextHeads);
                            if (this.mIsPolicyMissing) {
                                str = TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("policy missing in feature : ");
                                stringBuilder.append(appMngFeature);
                                stringBuilder.append(", config : ");
                                stringBuilder.append(enumWithDesc);
                                AwareLog.e(str, stringBuilder.toString());
                                setConfigFailed(appMngFeature);
                                return;
                            }
                        } else {
                            continue;
                        }
                    }
                } else {
                    i2 = outerDepth;
                }
                outerDepth = i2;
            } else {
                i = eventType;
                break;
            }
        }
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("values format error rawValue = ");
        stringBuilder.append(rawValue);
        AwareLog.e(str, stringBuilder.toString());
        setConfigFailed(appMngFeature);
    }

    private File loadCustConfigFile() {
        try {
            File cfg = HwCfgFilePolicy.getCfgFile(CUST_FILEPATH_RELATED, 0);
            if (cfg != null) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("cust path is ");
                stringBuilder.append(cfg.getAbsolutePath());
                AwareLog.d(str, stringBuilder.toString());
                return cfg;
            }
        } catch (NoClassDefFoundError e) {
            AwareLog.e(TAG, "loadCustConfigFile NoClassDefFoundError : HwCfgFilePolicy ");
        }
        return new File(CUST_FILEPATH);
    }

    private EnumWithDesc getConfig(AppMngFeature feature, String configName) {
        switch (AnonymousClass2.$SwitchMap$android$app$mtm$iaware$appmng$AppMngConstant$AppMngFeature[feature.ordinal()]) {
            case 1:
                return AppStartSource.fromString(configName);
            case 2:
                return AppCleanSource.fromString(configName);
            case 3:
                return AppFreezeSource.fromString(configName);
            case 4:
                return AppIoLimitSource.fromString(configName);
            case 5:
                return BroadcastSource.fromString(configName);
            default:
                return null;
        }
    }

    private TagEnum getType(AppMngFeature feature, String tagName) {
        switch (AnonymousClass2.$SwitchMap$android$app$mtm$iaware$appmng$AppMngConstant$AppMngFeature[feature.ordinal()]) {
            case 1:
                return AppStartTag.fromString(tagName);
            case 2:
            case 3:
            case 4:
                return AppMngTag.fromString(tagName);
            case 5:
                return BroadcastTag.fromString(tagName);
            default:
                return null;
        }
    }

    /* JADX WARNING: Missing block: B:9:0x002b, code skipped:
            if (r11.equals(ABBR_ALL_E_R) != false) goto L_0x004d;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean dealWithAbbreviation(String name, String packageName, int value, ArrayMap<EnumWithDesc, ArrayMap<String, ListItem>> featureList) {
        AppStartSource[] values = AppStartSource.values();
        int length = values.length;
        int i = 0;
        while (true) {
            boolean z = true;
            if (i >= length) {
                return true;
            }
            AppStartSource tag = values[i];
            ArrayMap<String, ListItem> configList = (ArrayMap) featureList.get(tag);
            switch (name.hashCode()) {
                case -913346042:
                    if (name.equals(ABBR_ALL_E_RBB)) {
                        z = true;
                        break;
                    }
                case -175522845:
                    if (name.equals(ABBR_EXCLUDE_BG_CHECK)) {
                        z = true;
                        break;
                    }
                case 96673:
                    if (name.equals("all")) {
                        z = false;
                        break;
                    }
                case 92904230:
                    break;
                case 1800987009:
                    if (name.equals(ABBR_ALL_BROAD)) {
                        z = true;
                        break;
                    }
                default:
                    z = true;
                    break;
            }
            switch (z) {
                case true:
                    if (AppStartSource.SCHEDULE_RESTART.equals(tag)) {
                        break;
                    }
                case true:
                    if (!(AppStartSource.SYSTEM_BROADCAST.equals(tag) || AppStartSource.THIRD_BROADCAST.equals(tag))) {
                        break;
                    }
                case true:
                    if (!AppStartSource.SYSTEM_BROADCAST.equals(tag)) {
                        if (!AppStartSource.THIRD_BROADCAST.equals(tag)) {
                            if (AppStartSource.SCHEDULE_RESTART.equals(tag)) {
                                break;
                            }
                        }
                        break;
                    }
                    break;
                case false:
                    if (configList == null) {
                        configList = new ArrayMap();
                    }
                    ListItem item = (ListItem) configList.get(packageName);
                    if (item == null) {
                        item = new ListItem();
                    }
                    if (AppStartSource.SYSTEM_BROADCAST.equals(tag)) {
                        ArrayMap<String, Integer> complicatePolicy = new ArrayMap();
                        complicatePolicy.put("default", Integer.valueOf(value));
                        item.setComplicatePolicy(complicatePolicy);
                    } else {
                        item.setPolicy(value);
                    }
                    configList.put(packageName, item);
                    featureList.put(tag, configList);
                    break;
                case true:
                    this.mBGCheckExcludedPkg.add(packageName);
                    break;
                default:
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("no such config named : ");
                    stringBuilder.append(name);
                    AwareLog.e(str, stringBuilder.toString());
                    return false;
            }
            i++;
        }
    }

    private boolean isConfigComplete(AppMngFeature feature) {
        boolean complete = true;
        ArrayList<Boolean> correctTag;
        if (feature == null) {
            for (Entry<AppMngFeature, ArrayList<Boolean>> correctTagEntry : this.mConfigCorrectTag.entrySet()) {
                correctTag = (ArrayList) correctTagEntry.getValue();
                if (!((Boolean) correctTag.get(0)).booleanValue()) {
                    complete = false;
                }
                correctTag.set(1, Boolean.valueOf(true));
            }
            return complete;
        }
        correctTag = (ArrayList) this.mConfigCorrectTag.get(feature);
        complete = ((Boolean) correctTag.get(0)).booleanValue();
        correctTag.set(1, Boolean.valueOf(true));
        return complete;
    }

    private void setConfigFailed(AppMngFeature feature) {
        if (feature == null) {
            for (Entry<AppMngFeature, ArrayList<Boolean>> correctTagEntry : this.mConfigCorrectTag.entrySet()) {
                ((ArrayList) correctTagEntry.getValue()).set(1, Boolean.valueOf(false));
            }
            return;
        }
        ((ArrayList) this.mConfigCorrectTag.get(feature)).set(1, Boolean.valueOf(false));
    }

    private boolean isNeedUpdate(AppMngFeature feature) {
        ArrayList<Boolean> correctTag = (ArrayList) this.mConfigCorrectTag.get(feature);
        boolean isUpdated = ((Boolean) correctTag.get(0)).booleanValue();
        boolean isCorrect = ((Boolean) correctTag.get(1)).booleanValue();
        if (isUpdated) {
            return false;
        }
        correctTag.set(0, Boolean.valueOf(isCorrect));
        return isCorrect;
    }

    private String[] fastSplit(String rawStr, String divider) {
        int prev = 0;
        if (rawStr == null) {
            return new String[0];
        }
        ArrayList<String> res = new ArrayList();
        while (true) {
            int indexOf = rawStr.indexOf(divider, prev);
            int pos = indexOf;
            if (indexOf != -1) {
                res.add(rawStr.substring(prev, pos));
                prev = pos + divider.length();
            } else {
                res.add(rawStr.substring(prev));
                return (String[]) res.toArray(new String[res.size()]);
            }
        }
    }

    public ArrayMap<AppMngFeature, ArrayMap<EnumWithDesc, Config>> getConfig() {
        return this.mAllConfig;
    }

    public ArrayMap<AppMngFeature, ArrayMap<EnumWithDesc, ArrayMap<String, ListItem>>> getList() {
        return this.mAllList;
    }

    public ArrayMap<AppMngFeature, ArrayMap<EnumWithDesc, ArrayMap<String, ListItem>>> getProcessList() {
        return this.mAllProcessList;
    }

    public ArrayMap<AppMngFeature, ArrayMap<String, ArrayList<String>>> getMisc() {
        return this.mAllMisc;
    }

    public ArraySet<String> getBGCheckExcludedPkg() {
        return this.mBGCheckExcludedPkg;
    }

    public int getVersion() {
        return this.mVersion;
    }
}
