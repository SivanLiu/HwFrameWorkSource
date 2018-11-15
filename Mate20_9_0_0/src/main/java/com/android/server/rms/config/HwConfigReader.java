package com.android.server.rms.config;

import android.content.Context;
import android.rms.config.ResourceConfig;
import android.rms.iaware.IAwareDecrypt;
import android.rms.utils.Utils;
import android.util.Log;
import android.util.Xml;
import com.android.internal.util.XmlUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import org.xmlpull.v1.XmlPullParser;

public final class HwConfigReader {
    private static String CONFIG_CLOUD_UPDATE_FILEPATH = "/data/system/iaware/iaware_srms_config.xml";
    private static String CONFIG_CUST_FILEPATH = "/data/cust/xml/HwResourceManager.xml";
    private static String CONFIG_DEFAULT_FILEPATH = "/system/etc/HwResourceManager.xml";
    private static final boolean DEBGU_XML_PARSE = Utils.DEBUG;
    public static final int FILE_LOCATION_CLOUD = 0;
    public static final int FILE_LOCATION_LOCAL = 1;
    private static final String TAG = "RMS.HwConfigReader";
    private static final String XML_ATTR_BETA_COUNT_INTERVAL = "beta_count_interval";
    private static final String XML_ATTR_CONTENT = "content";
    private static final String XML_ATTR_COUNT_INTERVAL = "count_interval";
    private static final String XML_ATTR_ID = "id";
    private static final String XML_ATTR_IS_COUNT = "isCount";
    private static final String XML_ATTR_LOOP_INTERVAL = "loop_interval";
    private static final String XML_ATTR_NAME = "name";
    private static final String XML_ATTR_NORMAL = "normal";
    private static final String XML_ATTR_RES_MAX_PEROID = "resource_max_peroid";
    private static final String XML_ATTR_RES_STRATEGY = "resource_strategy";
    private static final String XML_ATTR_RES_THRESHOLD = "resource_threshold";
    private static final String XML_ATTR_SAMPLE_BASE_PEROID = "base_peroid";
    private static final String XML_ATTR_SAMPLE_CYCLE_NUM = "cycle_num";
    private static final String XML_ATTR_SAVE_INTERVAL = "save_interval";
    private static final String XML_ATTR_TOTAL_LOOP_INTERVAL = "total_loop_interval";
    private static final String XML_ATTR_URGENT = "urgent";
    private static final String XML_ATTR_VALUE = "value";
    private static final String XML_ATTR_WARNING = "warning";
    private static final String XML_ATTR__MAX_KEEP_FILES = "max_keep_files";
    private static final String XML_TAG_GROUP = "group";
    private static final String XML_TAG_LEVEL = "level";
    private static final String XML_TAG_RESOURCE = "resource";
    private static final String XML_TAG_STATISTIC = "statistic";
    private static final String XML_TAG_SUBTYPE = "subType";
    private static final String XML_TAG_WHITELIST = "whitelist";
    private int mBetaCountInterval = -1;
    private ArrayList<Integer> mCountGroupID = new ArrayList();
    private int mCountInterval = -1;
    private HashMap<Integer, Group> mDynamicGoups = new HashMap();
    private HashMap<Integer, Group> mGoups = new HashMap();
    private int mMaxKeepFiles = -1;
    private int mSampleBasePeriod = -1;
    private int mSaveInterval = -1;

    private static class Group {
        public final int mID;
        public final boolean mIsCount;
        public final String mName;
        private final HashMap<Integer, ResourceConfig> mResConfigs = new HashMap();
        public final int mSampleCycleNum;
        private final HashMap<Integer, SubType> mSubTypes = new HashMap();
        private final HashMap<Integer, WhiteList> mWhiteLists = new HashMap();

        Group(int id, String name, int sampleCycleNum, boolean isCount) {
            this.mID = id;
            this.mName = name;
            this.mSampleCycleNum = sampleCycleNum;
            this.mIsCount = isCount;
        }

        public void addSubTypeItem(int id, SubType subType) {
            if (this.mSubTypes != null) {
                this.mSubTypes.put(Integer.valueOf(id), subType);
            }
        }

        public void addResConfigItem(int id, ResourceConfig config) {
            if (this.mResConfigs != null) {
                this.mResConfigs.put(Integer.valueOf(id), config);
            }
        }

        public void setWhiteList(Integer type, WhiteList whiteList) {
            this.mWhiteLists.put(type, whiteList);
        }

        public HashMap<Integer, SubType> getSubTypes() {
            return this.mSubTypes;
        }

        public HashMap<Integer, ResourceConfig> getResConfigs() {
            return this.mResConfigs;
        }

        public String getWhiteList() {
            return getWhiteList(Integer.valueOf(0));
        }

        public String getWhiteList(Integer type) {
            WhiteList whiteList = (WhiteList) this.mWhiteLists.get(type);
            if (whiteList != null) {
                return whiteList.getWhiteList();
            }
            return "";
        }
    }

    private static class SubType {
        public final int mID;
        private final ArrayList<Integer> mLevels = new ArrayList();
        public final int mLoopInterval;
        public final String mName;
        public final int mNormalThreshold;
        public final int mResourceMaxPeroid;
        public final int mResourceStrategy;
        public final int mResourceThreshold;
        public final int mTotalLoopInterval;
        public final int mUrgentThreshold;
        public final int mWarningThreshold;

        SubType(int id, String name, int resourceThreshold, int resourceStrategy, int resourceMaxPeroid, int loopInterval, int normalThreshold, int warningThreshold, int urgentThreshold, int totalLoopInterval) {
            this.mID = id;
            this.mName = name;
            this.mResourceThreshold = resourceThreshold;
            this.mResourceStrategy = resourceStrategy;
            this.mResourceMaxPeroid = resourceMaxPeroid;
            this.mLoopInterval = loopInterval;
            this.mNormalThreshold = normalThreshold;
            this.mWarningThreshold = warningThreshold;
            this.mUrgentThreshold = urgentThreshold;
            this.mTotalLoopInterval = totalLoopInterval;
        }

        public void addItem(int level) {
            this.mLevels.add(Integer.valueOf(level));
        }

        public ArrayList<Integer> getLevels() {
            return this.mLevels;
        }
    }

    private static class WhiteList {
        private final String mContent;
        private final String mName;

        WhiteList(String name, String content) {
            this.mName = name;
            this.mContent = content;
        }

        public String getWhiteList() {
            return this.mContent;
        }

        public String getWhiteListName() {
            return this.mName;
        }
    }

    public boolean updateResConfig(Context context) {
        this.mDynamicGoups.clear();
        boolean isLoadSuccess = parseConfig(context, 0);
        if (Utils.DEBUG || Log.HWLog) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("updateResConfig() isLoadSuccess:");
            stringBuilder.append(isLoadSuccess);
            Log.d(str, stringBuilder.toString());
        }
        return isLoadSuccess;
    }

    public boolean loadResConfig(Context context) {
        boolean isDynamicLoadSuccess = parseConfig(context, 0);
        boolean isStaticLoadSuccess = parseConfig(context, 1);
        if (Utils.DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("loadResConfig dynamicLoad:");
            stringBuilder.append(isDynamicLoadSuccess);
            stringBuilder.append(", staticLoad:");
            stringBuilder.append(isStaticLoadSuccess);
            Log.d(str, stringBuilder.toString());
        }
        if (isDynamicLoadSuccess || isStaticLoadSuccess) {
            return true;
        }
        return false;
    }

    public boolean parseConfig(Context context, int fileLocation) {
        if (context == null) {
            Log.e(TAG, "Context is null while parsing config file of RMS");
            return false;
        }
        File fileForParse;
        if (fileLocation == 0) {
            fileForParse = new File(CONFIG_CLOUD_UPDATE_FILEPATH);
        } else {
            fileForParse = new File(CONFIG_CUST_FILEPATH);
        }
        InputStream is = null;
        Group group = null;
        SubType subType = null;
        boolean ret = true;
        try {
            if (fileForParse.exists()) {
                is = new FileInputStream(fileForParse);
            } else if (fileLocation != 0) {
                is = new FileInputStream(new File(CONFIG_DEFAULT_FILEPATH));
            }
            is = IAwareDecrypt.decryptInputStream(context, is);
            if (is == null) {
                ret = false;
            }
            if (ret) {
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(is, null);
                XmlUtils.beginDocument(parser, XML_TAG_RESOURCE);
                while (true) {
                    XmlUtils.nextElement(parser);
                    String element = parser.getName();
                    if (DEBGU_XML_PARSE) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("[res count] element:");
                        stringBuilder.append(element);
                        Log.d(str, stringBuilder.toString());
                    }
                    if (element == null) {
                        break;
                    }
                    if (XML_TAG_STATISTIC.equals(element)) {
                        parseNodeStatistic(parser, fileLocation);
                    }
                    int level;
                    String str2;
                    if (XML_TAG_GROUP.equals(element)) {
                        group = parseNodeGroup(parser, fileLocation);
                    } else if (XML_TAG_SUBTYPE.equals(element)) {
                        subType = parseNodeSubType(parser, group);
                    } else if ("level".equals(element)) {
                        level = XmlUtils.readIntAttribute(parser, "value", 0);
                        if (subType != null) {
                            subType.addItem(level);
                        }
                        if (DEBGU_XML_PARSE) {
                            str2 = TAG;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("[res count] level: ");
                            stringBuilder2.append(level);
                            Log.d(str2, stringBuilder2.toString());
                        }
                    } else if (XML_TAG_WHITELIST.equals(element)) {
                        level = XmlUtils.readIntAttribute(parser, "id", 0);
                        str2 = XmlUtils.readStringAttribute(parser, "name");
                        String content = XmlUtils.readStringAttribute(parser, XML_ATTR_CONTENT);
                        if (str2 == null) {
                            str2 = "";
                        }
                        if (!(content == null || group == null)) {
                            group.setWhiteList(Integer.valueOf(level), new WhiteList(str2, content));
                        }
                    }
                }
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                }
            }
            if (Utils.DEBUG) {
                dump();
            }
            return ret;
        } catch (Exception e2) {
            Log.e(TAG, "[res count] read xml failed.", e2);
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e3) {
                }
            }
            return false;
        } catch (Throwable th) {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e4) {
                }
            }
        }
    }

    private void parseNodeStatistic(XmlPullParser parser, int fileLocation) {
        if (fileLocation != 0) {
            this.mSampleBasePeriod = XmlUtils.readIntAttribute(parser, XML_ATTR_SAMPLE_BASE_PEROID, -1);
            this.mSaveInterval = XmlUtils.readIntAttribute(parser, XML_ATTR_SAVE_INTERVAL, -1);
            this.mCountInterval = XmlUtils.readIntAttribute(parser, XML_ATTR_COUNT_INTERVAL, -1);
            this.mBetaCountInterval = XmlUtils.readIntAttribute(parser, XML_ATTR_BETA_COUNT_INTERVAL, this.mCountInterval);
            this.mMaxKeepFiles = XmlUtils.readIntAttribute(parser, XML_ATTR__MAX_KEEP_FILES, -1);
            if (DEBGU_XML_PARSE) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("[res count] save intervel:");
                stringBuilder.append(this.mSaveInterval);
                stringBuilder.append(" count intervel(minutes):");
                stringBuilder.append(this.mCountInterval);
                stringBuilder.append(" sample base period:");
                stringBuilder.append(this.mSampleBasePeriod);
                stringBuilder.append(" Beta Count Interval (minutes):");
                stringBuilder.append(this.mBetaCountInterval);
                stringBuilder.append(" max keep files:");
                stringBuilder.append(this.mMaxKeepFiles);
                Log.d(str, stringBuilder.toString());
            }
        }
    }

    private Group parseNodeGroup(XmlPullParser parser, int fileLocation) {
        int id = XmlUtils.readIntAttribute(parser, "id", -1);
        String name = XmlUtils.readStringAttribute(parser, "name");
        int cycleNum = XmlUtils.readIntAttribute(parser, XML_ATTR_SAMPLE_CYCLE_NUM, -1);
        boolean isCount = XmlUtils.readBooleanAttribute(parser, XML_ATTR_IS_COUNT, false);
        Group group = new Group(id, name, cycleNum, isCount);
        if (fileLocation == 0) {
            this.mDynamicGoups.put(Integer.valueOf(id), group);
        } else {
            this.mGoups.put(Integer.valueOf(id), group);
        }
        if (isCount && fileLocation == 1) {
            this.mCountGroupID.add(Integer.valueOf(id));
        }
        if (DEBGU_XML_PARSE) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[res count] group: ");
            stringBuilder.append(id);
            stringBuilder.append(" name: ");
            stringBuilder.append(name);
            stringBuilder.append(" cycle_num: ");
            stringBuilder.append(cycleNum);
            stringBuilder.append(" isCount: ");
            stringBuilder.append(isCount);
            Log.d(str, stringBuilder.toString());
        }
        return group;
    }

    private SubType parseNodeSubType(XmlPullParser parser, Group group) {
        XmlPullParser xmlPullParser = parser;
        Group group2 = group;
        int id = XmlUtils.readIntAttribute(xmlPullParser, "id", -1);
        String name = XmlUtils.readStringAttribute(xmlPullParser, "name");
        int resourceThreshold = XmlUtils.readIntAttribute(xmlPullParser, XML_ATTR_RES_THRESHOLD, -1);
        int resourceStrategy = XmlUtils.readIntAttribute(xmlPullParser, XML_ATTR_RES_STRATEGY, -1);
        int resourceMaxPeroid = XmlUtils.readIntAttribute(xmlPullParser, XML_ATTR_RES_MAX_PEROID, -1);
        int loopInterval = XmlUtils.readIntAttribute(xmlPullParser, XML_ATTR_LOOP_INTERVAL, -1);
        int normalThreshold = XmlUtils.readIntAttribute(xmlPullParser, XML_ATTR_NORMAL, -1);
        int warningThreshold = XmlUtils.readIntAttribute(xmlPullParser, XML_ATTR_WARNING, -1);
        int urgentThreshold = XmlUtils.readIntAttribute(xmlPullParser, XML_ATTR_URGENT, -1);
        int totalLoopInterval = XmlUtils.readIntAttribute(xmlPullParser, XML_ATTR_TOTAL_LOOP_INTERVAL, -1);
        int urgentThreshold2 = urgentThreshold;
        int warningThreshold2 = warningThreshold;
        int normalThreshold2 = normalThreshold;
        int loopInterval2 = loopInterval;
        int resourceMaxPeroid2 = resourceMaxPeroid;
        int resourceStrategy2 = resourceStrategy;
        int resourceThreshold2 = resourceThreshold;
        SubType subType = new SubType(id, name, resourceThreshold, resourceStrategy, resourceMaxPeroid, loopInterval, normalThreshold2, warningThreshold2, urgentThreshold2, totalLoopInterval);
        if (group2 != null) {
            group2.addSubTypeItem(id, subType);
        }
        SubType subType2 = subType;
        ResourceConfig config = new ResourceConfig(id, resourceThreshold2, resourceStrategy2, resourceMaxPeroid2, loopInterval2, name, normalThreshold2, warningThreshold2, urgentThreshold2, totalLoopInterval);
        if (group2 != null) {
            group2.addResConfigItem(id, config);
        }
        if (DEBGU_XML_PARSE) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[res count] subType: ");
            stringBuilder.append(id);
            stringBuilder.append(" name: ");
            stringBuilder.append(name);
            stringBuilder.append(" resourceThreshold: ");
            stringBuilder.append(resourceThreshold2);
            stringBuilder.append(" resourceStrategy: ");
            stringBuilder.append(resourceStrategy2);
            stringBuilder.append(" resourceMaxPeroid: ");
            stringBuilder.append(resourceMaxPeroid2);
            stringBuilder.append(" loopInterval: ");
            stringBuilder.append(loopInterval2);
            stringBuilder.append(" normalThreshold: ");
            stringBuilder.append(normalThreshold2);
            stringBuilder.append(" warningThreshold: ");
            stringBuilder.append(warningThreshold2);
            stringBuilder.append(" urgentThreshold: ");
            stringBuilder.append(urgentThreshold2);
            stringBuilder.append(" totalLoopInterval: ");
            stringBuilder.append(totalLoopInterval);
            Log.d(str, stringBuilder.toString());
        } else {
            resourceMaxPeroid = warningThreshold2;
            loopInterval = normalThreshold2;
            normalThreshold = loopInterval2;
            warningThreshold = resourceMaxPeroid2;
            urgentThreshold = resourceStrategy2;
            int i = resourceThreshold2;
        }
        return subType2;
    }

    private void dump() {
        Log.d(TAG, "[res count] ====== dump reource manage config xml ======");
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[res count] sample base period (ms) : ");
        stringBuilder.append(this.mSampleBasePeriod);
        stringBuilder.append(" save interval (minutes):");
        stringBuilder.append(this.mSaveInterval);
        stringBuilder.append(" count intervel (minutes):");
        stringBuilder.append(this.mCountInterval);
        stringBuilder.append(" Beta Count Interval (minutes):");
        stringBuilder.append(this.mBetaCountInterval);
        stringBuilder.append(" max keep files:");
        stringBuilder.append(this.mMaxKeepFiles);
        Log.d(str, stringBuilder.toString());
        for (Entry gEntry : this.mGoups.entrySet()) {
            Group g = (Group) gEntry.getValue();
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("[res count] gourp id: ");
            stringBuilder2.append(g.mID);
            stringBuilder2.append(" name: ");
            stringBuilder2.append(g.mName);
            stringBuilder2.append(" sampleCycleNum: ");
            stringBuilder2.append(g.mSampleCycleNum);
            stringBuilder2.append(" isCount: ");
            stringBuilder2.append(g.mIsCount);
            stringBuilder2.append(" whitelist:");
            stringBuilder2.append(g.getWhiteList());
            Log.d(str2, stringBuilder2.toString());
            for (Entry sEntry : g.getSubTypes().entrySet()) {
                SubType s = (SubType) sEntry.getValue();
                String str3 = TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("[res count] subType id: ");
                stringBuilder3.append(s.mID);
                stringBuilder3.append(" name: ");
                stringBuilder3.append(s.mName);
                stringBuilder3.append(" ResourceThreshold: ");
                stringBuilder3.append(s.mResourceThreshold);
                stringBuilder3.append(" ResourceStrategy: ");
                stringBuilder3.append(s.mResourceStrategy);
                stringBuilder3.append(" ResourceMaxPeroid: ");
                stringBuilder3.append(s.mResourceMaxPeroid);
                stringBuilder3.append(" totalLoopInterval: ");
                stringBuilder3.append(s.mTotalLoopInterval);
                stringBuilder3.append(" LoopInterval: ");
                stringBuilder3.append(s.mLoopInterval);
                stringBuilder3.append(" NormalThreshold: ");
                stringBuilder3.append(s.mNormalThreshold);
                stringBuilder3.append(" WarningThreshold: ");
                stringBuilder3.append(s.mWarningThreshold);
                stringBuilder3.append(" UrgentThreshold: ");
                stringBuilder3.append(s.mUrgentThreshold);
                stringBuilder3.append(" Levles: ");
                stringBuilder3.append(s.getLevels());
                Log.d(str3, stringBuilder3.toString());
            }
        }
        String str4 = TAG;
        StringBuilder stringBuilder4 = new StringBuilder();
        stringBuilder4.append("[res count] the IDs of the group which need to count: ");
        stringBuilder4.append(getCountGroupID());
        Log.d(str4, stringBuilder4.toString());
    }

    public int getGroupNum() {
        if (this.mGoups != null) {
            return this.mGoups.size();
        }
        return 0;
    }

    public String getGroupName(int groupID) {
        String name = "";
        try {
            return ((Group) this.mGoups.get(Integer.valueOf(groupID))).mName;
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[res count] can't get the group name: ");
            stringBuilder.append(groupID);
            Log.e(str, stringBuilder.toString(), e);
            return name;
        }
    }

    public boolean isCount(int groupID) {
        try {
            return ((Group) this.mGoups.get(Integer.valueOf(groupID))).mIsCount;
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[res count] can't get the group isCount: ");
            stringBuilder.append(groupID);
            Log.e(str, stringBuilder.toString(), e);
            return false;
        }
    }

    public int getGroupSampleCycleNum(int groupID) {
        try {
            return ((Group) this.mGoups.get(Integer.valueOf(groupID))).mSampleCycleNum;
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[res count] can't get the Group Sample Cycle Num: ");
            stringBuilder.append(groupID);
            Log.e(str, stringBuilder.toString(), e);
            return -1;
        }
    }

    public String getWhiteList(int groupID) {
        return getWhiteList(groupID, 0);
    }

    public String getWhiteList(int groupID, int type) {
        String tempWhiteListDynamic;
        String whiteList = "";
        String whiteListDynamic = "";
        String whiteListStatic = "";
        if (!(this.mDynamicGoups.size() == 0 || this.mDynamicGoups.get(Integer.valueOf(groupID)) == null)) {
            tempWhiteListDynamic = ((Group) this.mDynamicGoups.get(Integer.valueOf(groupID))).getWhiteList(Integer.valueOf(type));
            if (tempWhiteListDynamic != null) {
                whiteListDynamic = tempWhiteListDynamic;
                if (groupID == 32 && ((type == 3 || type == 4) && !whiteListDynamic.isEmpty())) {
                    if (Utils.DEBUG) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("getWhiteList  id:");
                        stringBuilder.append(groupID);
                        stringBuilder.append(", dynamicList:");
                        stringBuilder.append(whiteListDynamic);
                        stringBuilder.append(", staticList:");
                        stringBuilder.append(whiteListStatic);
                        stringBuilder.append(", type:");
                        stringBuilder.append(type);
                        Log.d(str, stringBuilder.toString());
                    }
                    return whiteListDynamic;
                }
            }
        }
        if (!(this.mGoups.size() == 0 || this.mGoups.get(Integer.valueOf(groupID)) == null)) {
            tempWhiteListDynamic = ((Group) this.mGoups.get(Integer.valueOf(groupID))).getWhiteList(Integer.valueOf(type));
            if (tempWhiteListDynamic != null) {
                whiteListStatic = tempWhiteListDynamic;
            }
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(whiteListDynamic);
        stringBuilder2.append(CPUCustBaseConfig.CPUCONFIG_GAP_IDENTIFIER);
        stringBuilder2.append(whiteListStatic);
        whiteList = stringBuilder2.toString();
        if (Utils.DEBUG) {
            tempWhiteListDynamic = TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("getWhiteList  id:");
            stringBuilder3.append(groupID);
            stringBuilder3.append(", dynamicList:");
            stringBuilder3.append(whiteListDynamic);
            stringBuilder3.append(", staticList:");
            stringBuilder3.append(whiteListStatic);
            stringBuilder3.append(", mergeResult:");
            stringBuilder3.append(whiteList);
            Log.d(tempWhiteListDynamic, stringBuilder3.toString());
        }
        return whiteList;
    }

    public ResourceConfig getResConfig(int groupID, int subType) {
        try {
            return (ResourceConfig) ((Group) this.mGoups.get(Integer.valueOf(groupID))).getResConfigs().get(Integer.valueOf(subType));
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[res count] can't get the res config, group: ");
            stringBuilder.append(groupID);
            stringBuilder.append(" subType: ");
            stringBuilder.append(subType);
            Log.e(str, stringBuilder.toString(), e);
            return null;
        }
    }

    public int getSubTypeNum(int groupID) {
        try {
            return ((Group) this.mGoups.get(Integer.valueOf(groupID))).getSubTypes().size();
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[res count] can't get the subType num, group: ");
            stringBuilder.append(groupID);
            Log.e(str, stringBuilder.toString(), e);
            return 0;
        }
    }

    public String getSubTypeName(int groupID, int subType) {
        String name = "";
        try {
            return ((SubType) ((Group) this.mGoups.get(Integer.valueOf(groupID))).getSubTypes().get(Integer.valueOf(subType))).mName;
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[res count] can't get the subType name, group: ");
            stringBuilder.append(groupID);
            stringBuilder.append(" subType: ");
            stringBuilder.append(subType);
            Log.e(str, stringBuilder.toString(), e);
            return name;
        }
    }

    public int getResourceThreshold(int groupID, int subType) {
        try {
            return ((SubType) ((Group) this.mGoups.get(Integer.valueOf(groupID))).getSubTypes().get(Integer.valueOf(subType))).mResourceThreshold;
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[res count] can't get the subType ResourceThreshold, group: ");
            stringBuilder.append(groupID);
            stringBuilder.append(" subType: ");
            stringBuilder.append(subType);
            Log.e(str, stringBuilder.toString(), e);
            return -1;
        }
    }

    public int getResourceStrategy(int groupID, int subType) {
        try {
            return ((SubType) ((Group) this.mGoups.get(Integer.valueOf(groupID))).getSubTypes().get(Integer.valueOf(subType))).mResourceStrategy;
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[res count] can't get the subType ResourceStrategy, group: ");
            stringBuilder.append(groupID);
            stringBuilder.append(" subType: ");
            stringBuilder.append(subType);
            Log.e(str, stringBuilder.toString(), e);
            return -1;
        }
    }

    public int getResourceMaxPeroid(int groupID, int subType) {
        try {
            return ((SubType) ((Group) this.mGoups.get(Integer.valueOf(groupID))).getSubTypes().get(Integer.valueOf(subType))).mResourceMaxPeroid;
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[res count] can't get the subType ResourceMaxPeroid, group: ");
            stringBuilder.append(groupID);
            stringBuilder.append(" subType: ");
            stringBuilder.append(subType);
            Log.e(str, stringBuilder.toString(), e);
            return -1;
        }
    }

    public int getTotalLoopInterval(int groupID, int subType) {
        try {
            return ((SubType) ((Group) this.mGoups.get(Integer.valueOf(groupID))).getSubTypes().get(Integer.valueOf(subType))).mTotalLoopInterval;
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[res count] can't get the subType TotalLoopInterval, group: ");
            stringBuilder.append(groupID);
            stringBuilder.append(" subType: ");
            stringBuilder.append(subType);
            Log.e(str, stringBuilder.toString(), e);
            return -1;
        }
    }

    public int getLoopInterval(int groupID, int subType) {
        try {
            return ((SubType) ((Group) this.mGoups.get(Integer.valueOf(groupID))).getSubTypes().get(Integer.valueOf(subType))).mLoopInterval;
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[res count] can't get the subType LoopInterval, group: ");
            stringBuilder.append(groupID);
            stringBuilder.append(" subType: ");
            stringBuilder.append(subType);
            Log.e(str, stringBuilder.toString(), e);
            return -1;
        }
    }

    public int getNormalThreshold(int groupID, int subType) {
        try {
            return ((SubType) ((Group) this.mGoups.get(Integer.valueOf(groupID))).getSubTypes().get(Integer.valueOf(subType))).mNormalThreshold;
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[res count] can't get the subType NormalThreshold, group: ");
            stringBuilder.append(groupID);
            stringBuilder.append(" subType: ");
            stringBuilder.append(subType);
            Log.e(str, stringBuilder.toString(), e);
            return -1;
        }
    }

    public int getWarningThreshold(int groupID, int subType) {
        try {
            return ((SubType) ((Group) this.mGoups.get(Integer.valueOf(groupID))).getSubTypes().get(Integer.valueOf(subType))).mWarningThreshold;
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[res count] can't get the subType WarningThreshold, group: ");
            stringBuilder.append(groupID);
            stringBuilder.append(" subType: ");
            stringBuilder.append(subType);
            Log.e(str, stringBuilder.toString(), e);
            return -1;
        }
    }

    public int getUrgentThreshold(int groupID, int subType) {
        try {
            return ((SubType) ((Group) this.mGoups.get(Integer.valueOf(groupID))).getSubTypes().get(Integer.valueOf(subType))).mUrgentThreshold;
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[res count] can't get the subType UrgentThreshold, group: ");
            stringBuilder.append(groupID);
            stringBuilder.append(" subType: ");
            stringBuilder.append(subType);
            Log.e(str, stringBuilder.toString(), e);
            return -1;
        }
    }

    public ArrayList<Integer> getSubTypeLevels(int groupID, int subType) {
        try {
            return ((SubType) ((Group) this.mGoups.get(Integer.valueOf(groupID))).getSubTypes().get(Integer.valueOf(subType))).getLevels();
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[res count] can't get the subType levels, group: ");
            stringBuilder.append(groupID);
            stringBuilder.append(" subType: ");
            stringBuilder.append(subType);
            Log.e(str, stringBuilder.toString(), e);
            return null;
        }
    }

    public ArrayList<Integer> getCountGroupID() {
        return this.mCountGroupID;
    }

    public int getSaveInterval() {
        return this.mSaveInterval;
    }

    public int getCountInterval(boolean isBetaUser) {
        return isBetaUser ? this.mBetaCountInterval : this.mCountInterval;
    }

    public int getSampleBasePeriod() {
        return this.mSampleBasePeriod;
    }

    public int getMaxKeepFiles() {
        return this.mMaxKeepFiles;
    }
}
