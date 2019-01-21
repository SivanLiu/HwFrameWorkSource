package com.android.server.mtm.iaware.appmng;

import android.app.mtm.iaware.appmng.AppMngConstant.AppCleanSource;
import android.app.mtm.iaware.appmng.AppMngConstant.AppMngFeature;
import android.app.mtm.iaware.appmng.AppMngConstant.AppStartReason;
import android.app.mtm.iaware.appmng.AppMngConstant.AppStartSource;
import android.app.mtm.iaware.appmng.AppMngConstant.BroadcastSource;
import android.app.mtm.iaware.appmng.AppMngConstant.CleanReason;
import android.app.mtm.iaware.appmng.AppMngConstant.EnumWithDesc;
import android.content.Context;
import android.rms.iaware.AwareLog;
import android.util.ArrayMap;
import android.util.ArraySet;
import com.android.server.mtm.iaware.appmng.appstart.AwareAppStartupPolicy;
import com.android.server.mtm.iaware.appmng.appstart.comm.AppStartupUtil;
import com.android.server.mtm.iaware.appmng.policy.AppStartPolicy;
import com.android.server.mtm.iaware.appmng.policy.Policy;
import com.android.server.mtm.iaware.appmng.rule.AppMngRule;
import com.android.server.mtm.iaware.appmng.rule.AppStartRule;
import com.android.server.mtm.iaware.appmng.rule.BroadcastMngRule;
import com.android.server.mtm.iaware.appmng.rule.Config;
import com.android.server.mtm.iaware.appmng.rule.ConfigReader;
import com.android.server.mtm.iaware.appmng.rule.ListItem;
import com.android.server.mtm.iaware.appmng.rule.RuleParserUtil.AppMngTag;
import com.android.server.mtm.taskstatus.ProcessCleaner.CleanType;
import com.android.server.rms.iaware.appmng.AwareAppStartStatusCache;
import com.android.server.rms.iaware.appmng.AwareIntelligentRecg;
import com.android.server.rms.iaware.appmng.AwareWakeUpManager;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Queue;

public class DecisionMaker {
    private static final int APPSTART_POLICY_NOT_ALLOW = 0;
    private static final int APPSTART_XML_POLICY_ALLOW_ALL = 2;
    private static final int APPSTART_XML_POLICY_ALLOW_SMART = 1;
    private static final int DEFAULT_APPMNG_POLICY = CleanType.NONE.ordinal();
    private static final int DEFAULT_APPSTART_POLICY = 1;
    private static final String DEFAULT_STRING = "default";
    private static final int DEFAULT_UID = 0;
    private static final int MAX_HISTORY_LENGTH = 50;
    private static final String REASON_LIST = "list";
    private static final String TAG = "DecisionMaker";
    private static final int UNINIT_VALUE = -1;
    private static DecisionMaker mDecisionMaker = null;
    private ArrayMap<AppMngFeature, ArrayMap<EnumWithDesc, Config>> mAllConfig = new ArrayMap();
    private ArrayMap<AppMngFeature, ArrayMap<EnumWithDesc, ArrayMap<String, ListItem>>> mAllList = new ArrayMap();
    private ArrayMap<AppMngFeature, ArrayMap<String, ArrayList<String>>> mAllMisc = new ArrayMap();
    private ArrayMap<AppMngFeature, ArrayMap<EnumWithDesc, ArrayMap<String, ListItem>>> mAllProcessList = new ArrayMap();
    private ArrayMap<AppCleanSource, Queue<String>> mCleanHistory = new ArrayMap();
    private int mVersion;

    private DecisionMaker() {
    }

    public static synchronized DecisionMaker getInstance() {
        DecisionMaker decisionMaker;
        synchronized (DecisionMaker.class) {
            if (mDecisionMaker == null) {
                mDecisionMaker = new DecisionMaker();
            }
            decisionMaker = mDecisionMaker;
        }
        return decisionMaker;
    }

    public List<AwareProcessBlockInfo> decideAll(List<AwareProcessInfo> processInfoList, int level, AppMngFeature feature, EnumWithDesc config, ArraySet<String> listFilter) {
        return decideAllInternal(processInfoList, level, feature, config, true, listFilter);
    }

    public List<AwareProcessBlockInfo> decideAll(List<AwareProcessInfo> processInfoList, int level, AppMngFeature feature, EnumWithDesc config) {
        return decideAllInternal(processInfoList, level, feature, config, true, null);
    }

    public List<AwareProcessBlockInfo> decideAllWithoutList(List<AwareProcessInfo> processInfoList, int level, AppMngFeature feature, EnumWithDesc config) {
        return decideAllInternal(processInfoList, level, feature, config, false, null);
    }

    private List<AwareProcessBlockInfo> decideAllInternal(List<AwareProcessInfo> processInfoList, int level, AppMngFeature feature, EnumWithDesc config, boolean shouldConsiderList, ArraySet<String> listFilter) {
        if (processInfoList == null) {
            return null;
        }
        List<AwareProcessBlockInfo> resultList = new ArrayList();
        for (AwareProcessInfo processInfo : processInfoList) {
            if (processInfo != null) {
                AwareProcessBlockInfo result = decide(processInfo, level, feature, config, shouldConsiderList, listFilter);
                if (result != null) {
                    resultList.add(result);
                }
            }
        }
        return resultList;
    }

    /* JADX WARNING: Missing block: B:24:0x003f, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean decideSystemAppPolicy(String packageName, AppStartSource source, AwareAppStartStatusCache status, int tristate) {
        if (status == null || (status.mFlags & 8) != 0) {
            return true;
        }
        if (tristate == 1 || AppStartSource.SYSTEM_BROADCAST.equals(source) || AppStartSource.THIRD_BROADCAST.equals(source) || AppStartSource.SCHEDULE_RESTART.equals(source) || !status.mIsSystemApp) {
            return false;
        }
        if (AwareAppStartupPolicy.isAppSelfStart(source) && AwareIntelligentRecg.getInstance().isGmsAppAndNeedCtrl(packageName)) {
            return false;
        }
        return true;
    }

    private AwareProcessBlockInfo decide(AwareProcessInfo processInfo, int level, AppMngFeature feature, EnumWithDesc config, boolean shouldConsiderList, ArraySet<String> listFilter) {
        AwareProcessInfo awareProcessInfo = processInfo;
        AppMngFeature appMngFeature = feature;
        EnumWithDesc enumWithDesc = config;
        boolean z = shouldConsiderList;
        ArrayMap<EnumWithDesc, Config> featureRule = (ArrayMap) this.mAllConfig.get(appMngFeature);
        ArrayMap<EnumWithDesc, ArrayMap<String, ListItem>> featureList = (ArrayMap) this.mAllList.get(appMngFeature);
        ArraySet<String> listFilter2;
        if (listFilter == null) {
            listFilter2 = new ArraySet();
        } else {
            listFilter2 = listFilter;
        }
        int i;
        if (awareProcessInfo.mProcInfo == null || awareProcessInfo.mProcInfo.mPackageName == null || awareProcessInfo.mProcInfo.mPackageName.isEmpty()) {
            i = level;
            ArrayMap<String, Integer> detailedReason = new ArrayMap();
            detailedReason.put(AppMngTag.POLICY.getDesc(), Integer.valueOf(CleanType.NONE.ordinal()));
            detailedReason.put("spec", Integer.valueOf(CleanReason.MISSING_PROCESS_INFO.ordinal()));
            return new AwareProcessBlockInfo(CleanReason.MISSING_PROCESS_INFO.getCode(), 0, awareProcessInfo, DEFAULT_APPMNG_POLICY, detailedReason);
        }
        String packageName = (String) awareProcessInfo.mProcInfo.mPackageName.get(0);
        if (AppStartupUtil.isCtsPackage(packageName)) {
            ArrayMap<String, Integer> detailedReason2 = new ArrayMap();
            detailedReason2.put(AppMngTag.POLICY.getDesc(), Integer.valueOf(CleanType.NONE.ordinal()));
            detailedReason2.put("spec", Integer.valueOf(CleanReason.CTS.ordinal()));
            return new AwareProcessBlockInfo(CleanReason.CTS.getCode(), 0, awareProcessInfo, DEFAULT_APPMNG_POLICY, detailedReason2);
        }
        if (!(!z || featureList == null || listFilter2.contains(packageName))) {
            ArrayMap<String, ListItem> list = (ArrayMap) featureList.get(enumWithDesc);
            if (list != null) {
                ListItem item = (ListItem) list.get(packageName);
                if (item != null) {
                    ArrayMap<String, Integer> detailedReason3 = new ArrayMap();
                    detailedReason3.put(AppMngTag.POLICY.getDesc(), Integer.valueOf(item.getPolicy()));
                    detailedReason3.put("spec", Integer.valueOf(CleanReason.LIST.ordinal()));
                    ArrayMap<String, Integer> detailedReason4 = detailedReason3;
                    return new AwareProcessBlockInfo(CleanReason.LIST.getCode(), awareProcessInfo.mProcInfo.mUid, awareProcessInfo, item.getPolicy(), detailedReason4);
                }
            }
        }
        if (featureRule != null) {
            AppMngRule rule = (AppMngRule) featureRule.get(enumWithDesc);
            if (rule != null) {
                return rule.apply(awareProcessInfo, packageName, level, z);
            }
        }
        i = level;
        return new AwareProcessBlockInfo(CleanReason.CONFIG_INVALID.getCode(), awareProcessInfo.mProcInfo.mUid, awareProcessInfo, DEFAULT_APPMNG_POLICY, null);
    }

    public ArrayMap<String, ListItem> getProcessList(AppMngFeature feature, EnumWithDesc config) {
        if (feature == null || config == null) {
            return null;
        }
        ArrayMap<EnumWithDesc, ArrayMap<String, ListItem>> featureProcessList = (ArrayMap) this.mAllProcessList.get(feature);
        if (featureProcessList == null) {
            return null;
        }
        return (ArrayMap) featureProcessList.get(config);
    }

    public int getAppStartPolicy(String packageName, AppStartSource source) {
        ArrayMap<EnumWithDesc, ArrayMap<String, ListItem>> featureList = (ArrayMap) this.mAllList.get(AppMngFeature.APP_START);
        if (featureList == null) {
            return 1;
        }
        ArrayMap<String, ListItem> list = (ArrayMap) featureList.get(source);
        if (list == null) {
            return 0;
        }
        ListItem item = (ListItem) list.get(packageName);
        if (item != null) {
            return item.getPolicy();
        }
        return 0;
    }

    public void addFeatureList(AppMngFeature feature, ArrayMap<EnumWithDesc, ArrayMap<String, ListItem>> addLists) {
        ArrayMap<EnumWithDesc, ArrayMap<String, ListItem>> featureList = (ArrayMap) this.mAllList.get(feature);
        if (featureList != null && addLists != null && !addLists.isEmpty()) {
            ArrayMap<AppMngFeature, ArrayMap<EnumWithDesc, ArrayMap<String, ListItem>>> allList = new ArrayMap();
            for (Entry<AppMngFeature, ArrayMap<EnumWithDesc, ArrayMap<String, ListItem>>> entry : this.mAllList.entrySet()) {
                allList.put((AppMngFeature) entry.getKey(), (ArrayMap) entry.getValue());
            }
            ArrayMap<EnumWithDesc, ArrayMap<String, ListItem>> newFeatureList = (ArrayMap) allList.get(feature);
            if (newFeatureList == null) {
                newFeatureList = new ArrayMap();
                allList.put(feature, newFeatureList);
            }
            for (Entry<EnumWithDesc, ArrayMap<String, ListItem>> entry2 : addLists.entrySet()) {
                EnumWithDesc configEnum = (EnumWithDesc) entry2.getKey();
                ArrayMap<String, ListItem> addPoliciesList = (ArrayMap) entry2.getValue();
                ArrayMap<String, ListItem> policiesList = (ArrayMap) featureList.get(configEnum);
                ArrayMap<String, ListItem> newPoliciesList = new ArrayMap();
                addPolicies(policiesList, addPoliciesList, newPoliciesList);
                newFeatureList.put(configEnum, newPoliciesList);
            }
            this.mAllList = allList;
        }
    }

    public ArrayMap<EnumWithDesc, ArrayMap<String, ListItem>> removeFeatureList(AppMngFeature feature, ArraySet<String> pkgs) {
        ArrayMap<EnumWithDesc, ArrayMap<String, ListItem>> featureList = (ArrayMap) this.mAllList.get(feature);
        if (featureList == null || pkgs == null || pkgs.isEmpty()) {
            return null;
        }
        ArrayMap<AppMngFeature, ArrayMap<EnumWithDesc, ArrayMap<String, ListItem>>> allList = new ArrayMap();
        ArrayMap<EnumWithDesc, ArrayMap<String, ListItem>> removeLists = new ArrayMap();
        ArrayMap<EnumWithDesc, ArrayMap<String, ListItem>> newFeatureList = new ArrayMap();
        for (Entry<EnumWithDesc, ArrayMap<String, ListItem>> entry : featureList.entrySet()) {
            EnumWithDesc configEnum = (EnumWithDesc) entry.getKey();
            ArrayMap<String, ListItem> policies = (ArrayMap) entry.getValue();
            if (!AppMngFeature.APP_CLEAN.equals(feature) || AppCleanSource.MEMORY.equals(configEnum) || AppCleanSource.SMART_CLEAN.equals(configEnum)) {
                ArrayMap<String, ListItem> newPolicies = new ArrayMap();
                ArrayMap<String, ListItem> removePolicies = new ArrayMap();
                removePolicies(policies, pkgs, newPolicies, removePolicies);
                newFeatureList.put(configEnum, newPolicies);
                if (removePolicies.size() > 0) {
                    removeLists.put(configEnum, removePolicies);
                }
            } else {
                newFeatureList.put(configEnum, policies);
            }
        }
        for (Entry<AppMngFeature, ArrayMap<EnumWithDesc, ArrayMap<String, ListItem>>> entry2 : this.mAllList.entrySet()) {
            AppMngFeature appmngFeature = (AppMngFeature) entry2.getKey();
            ArrayMap<EnumWithDesc, ArrayMap<String, ListItem>> lists = (ArrayMap) entry2.getValue();
            if (appmngFeature.equals(feature)) {
                allList.put(appmngFeature, newFeatureList);
            } else {
                allList.put(appmngFeature, lists);
            }
        }
        this.mAllList = allList;
        return removeLists;
    }

    private void removePolicies(ArrayMap<String, ListItem> policiesList, ArraySet<String> pkgs, ArrayMap<String, ListItem> newPoliciesList, ArrayMap<String, ListItem> removePoliciesList) {
        for (Entry<String, ListItem> entry : policiesList.entrySet()) {
            String pkg = (String) entry.getKey();
            ListItem item = (ListItem) entry.getValue();
            if (pkgs.contains(pkg)) {
                removePoliciesList.put(pkg, item);
            } else {
                newPoliciesList.put(pkg, item);
            }
        }
    }

    private void addPolicies(ArrayMap<String, ListItem> policiesList, ArrayMap<String, ListItem> addPoliciesList, ArrayMap<String, ListItem> newPoliciesList) {
        for (Entry<String, ListItem> entry : policiesList.entrySet()) {
            newPoliciesList.put((String) entry.getKey(), (ListItem) entry.getValue());
        }
        for (Entry<String, ListItem> entry2 : addPoliciesList.entrySet()) {
            newPoliciesList.put((String) entry2.getKey(), (ListItem) entry2.getValue());
        }
    }

    public Policy decide(String packageName, AppStartSource source, AwareAppStartStatusCache status, int tristate) {
        ArrayMap<EnumWithDesc, ArrayMap<String, ListItem>> featureList = (ArrayMap) this.mAllList.get(AppMngFeature.APP_START);
        if (featureList != null) {
            ArrayMap<String, ListItem> list = (ArrayMap) featureList.get(source);
            if (list != null) {
                ListItem item = (ListItem) list.get(packageName);
                if (!(item == null || status == null)) {
                    int policy;
                    if (AppStartSource.SYSTEM_BROADCAST.equals(source)) {
                        policy = item.getPolicy(status.mAction);
                        if (policy == -1) {
                            policy = item.getPolicy("default");
                        }
                    } else {
                        policy = item.getPolicy();
                    }
                    switch (policy) {
                        case 1:
                            if (tristate == 1) {
                                policy = -1;
                                break;
                            }
                            break;
                        case 2:
                            policy = 1;
                            break;
                    }
                    if (policy != -1) {
                        return new AppStartPolicy(packageName, policy, AppStartReason.LIST.getDesc());
                    }
                }
            }
        }
        if (decideSystemAppPolicy(packageName, source, status, tristate)) {
            return new AppStartPolicy(packageName, 1, AppStartReason.SYSTEM_APP.getDesc());
        }
        ArrayMap<EnumWithDesc, Config> featureRule = (ArrayMap) this.mAllConfig.get(AppMngFeature.APP_START);
        if (featureRule != null) {
            AppStartRule rule = (AppStartRule) featureRule.get(source);
            if (rule != null) {
                return rule.apply(packageName, source, status, tristate);
            }
        }
        return new AppStartPolicy(packageName, 1, AppStartReason.DEFAULT.getDesc());
    }

    public void updateRule(AppMngFeature feature, Context context) {
        ConfigReader reader = new ConfigReader();
        reader.parseFile(feature, context);
        if (feature == null) {
            this.mAllConfig = reader.getConfig();
            this.mAllList = reader.getList();
            this.mAllProcessList = reader.getProcessList();
            this.mAllMisc = reader.getMisc();
            updateMiscCache();
        } else {
            this.mAllConfig.put(feature, (ArrayMap) reader.getConfig().get(feature));
            this.mAllList.put(feature, (ArrayMap) reader.getList().get(feature));
            this.mAllProcessList.put(feature, (ArrayMap) reader.getProcessList().get(feature));
            this.mAllMisc.put(feature, (ArrayMap) reader.getMisc().get(feature));
        }
        if (feature == null || AppMngFeature.APP_CLEAN.equals(feature)) {
            AwareIntelligentRecg.getInstance().removeAppCleanFeatureGMSList();
            initHistory();
        }
        if (feature == null || AppMngFeature.APP_START.equals(feature)) {
            AwareIntelligentRecg.getInstance().updateBGCheckExcludedInfo(reader.getBGCheckExcludedPkg());
            AwareIntelligentRecg.getInstance().removeAppStartFeatureGMSList();
        }
        this.mVersion = reader.getVersion();
    }

    public ArrayList<String> getRawConfig(String feature, String config) {
        if (config == null || feature == null) {
            return null;
        }
        ArrayMap<String, ArrayList<String>> rawFeature = (ArrayMap) this.mAllMisc.get(AppMngFeature.fromString(feature));
        if (rawFeature == null) {
            return null;
        }
        ArrayList<String> items = (ArrayList) rawFeature.get(config);
        if (items == null) {
            return new ArrayList();
        }
        return items;
    }

    public int getVersion() {
        return this.mVersion;
    }

    public void dump(PrintWriter pw, AppMngFeature feature) {
        if (pw != null) {
            dumpList(pw, feature);
            ArrayMap<EnumWithDesc, Config> featureConfig = (ArrayMap) this.mAllConfig.get(feature);
            StringBuilder stringBuilder;
            if (featureConfig == null) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("there is no [");
                stringBuilder.append(feature);
                stringBuilder.append("] config");
                pw.println(stringBuilder.toString());
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("----------[");
                stringBuilder.append(feature);
                stringBuilder.append("] Rules----------");
                pw.println(stringBuilder.toString());
                for (Entry<EnumWithDesc, Config> featureConfigEntry : featureConfig.entrySet()) {
                    if (featureConfigEntry != null) {
                        Config config = (Config) featureConfigEntry.getValue();
                        if (config != null) {
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("==========[");
                            stringBuilder2.append(featureConfigEntry.getKey());
                            stringBuilder2.append("] Rules==========");
                            pw.println(stringBuilder2.toString());
                            config.dump(pw);
                        }
                    }
                }
            }
        }
    }

    public void dumpList(PrintWriter pw, AppMngFeature feature) {
        if (pw != null) {
            StringBuilder stringBuilder;
            ArrayMap<String, ListItem> configList;
            StringBuilder stringBuilder2;
            ListItem item;
            StringBuilder stringBuilder3;
            ArrayMap<EnumWithDesc, ArrayMap<String, ListItem>> featureList = (ArrayMap) this.mAllList.get(feature);
            if (this.mVersion != -1) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("config version:");
                stringBuilder.append(this.mVersion);
                pw.println(stringBuilder.toString());
            }
            if (featureList == null) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("there is no [");
                stringBuilder.append(feature);
                stringBuilder.append("] list");
                pw.println(stringBuilder.toString());
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("----------[");
                stringBuilder.append(feature);
                stringBuilder.append("] Lists----------");
                pw.println(stringBuilder.toString());
                for (Entry<EnumWithDesc, ArrayMap<String, ListItem>> featureListEntry : featureList.entrySet()) {
                    if (featureListEntry != null) {
                        configList = (ArrayMap) featureListEntry.getValue();
                        if (configList != null) {
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("==========[");
                            stringBuilder2.append(featureListEntry.getKey());
                            stringBuilder2.append("] Lists==========");
                            pw.println(stringBuilder2.toString());
                            for (Entry<String, ListItem> configListEntry : configList.entrySet()) {
                                if (configListEntry != null) {
                                    item = (ListItem) configListEntry.getValue();
                                    if (item != null) {
                                        if (item.getPolicy() != -1) {
                                            stringBuilder3 = new StringBuilder();
                                            stringBuilder3.append("  ");
                                            stringBuilder3.append((String) configListEntry.getKey());
                                            stringBuilder3.append(":");
                                            pw.println(stringBuilder3.toString());
                                            item.dump(pw);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            ArrayMap<EnumWithDesc, ArrayMap<String, ListItem>> featureProcessList = (ArrayMap) this.mAllProcessList.get(feature);
            StringBuilder stringBuilder4;
            if (featureProcessList == null) {
                stringBuilder4 = new StringBuilder();
                stringBuilder4.append("there is no [");
                stringBuilder4.append(feature);
                stringBuilder4.append("] processlist");
                pw.println(stringBuilder4.toString());
            } else {
                stringBuilder4 = new StringBuilder();
                stringBuilder4.append("----------[");
                stringBuilder4.append(feature);
                stringBuilder4.append("] processlist----------");
                pw.println(stringBuilder4.toString());
                for (Entry<EnumWithDesc, ArrayMap<String, ListItem>> featureListEntry2 : featureProcessList.entrySet()) {
                    if (featureListEntry2 != null) {
                        configList = (ArrayMap) featureListEntry2.getValue();
                        if (configList != null) {
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("==========[");
                            stringBuilder2.append(featureListEntry2.getKey());
                            stringBuilder2.append("] ProcessLists==========");
                            pw.println(stringBuilder2.toString());
                            for (Entry<String, ListItem> configListEntry2 : configList.entrySet()) {
                                if (configListEntry2 != null) {
                                    item = (ListItem) configListEntry2.getValue();
                                    if (item != null) {
                                        stringBuilder3 = new StringBuilder();
                                        stringBuilder3.append("  ");
                                        stringBuilder3.append((String) configListEntry2.getKey());
                                        stringBuilder3.append(":");
                                        pw.println(stringBuilder3.toString());
                                        item.dump(pw);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void initHistory() {
        for (AppCleanSource source : AppCleanSource.values()) {
            synchronized (this.mCleanHistory) {
                this.mCleanHistory.put(source, new LinkedList());
            }
        }
    }

    /* JADX WARNING: Missing block: B:17:0x004b, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void updateHistory(AppCleanSource source, String reason) {
        synchronized (this.mCleanHistory) {
            Queue<String> queue = (Queue) this.mCleanHistory.get(source);
            if (queue == null) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("bad request = ");
                stringBuilder.append(source);
                AwareLog.e(str, stringBuilder.toString());
                return;
            }
            if (queue.size() == 50 && ((String) queue.poll()) == null) {
                AwareLog.e(TAG, "poll failed !");
            }
            if (!queue.offer(reason)) {
                AwareLog.e(TAG, "updateHistory failed !");
            }
        }
    }

    public void dumpHistory(PrintWriter pw, AppCleanSource source) {
        if (pw != null) {
            synchronized (this.mCleanHistory) {
                Queue<String> queue = (Queue) this.mCleanHistory.get(source);
                if (queue != null) {
                    for (String history : queue) {
                        pw.println(history);
                    }
                }
            }
        }
    }

    private void updateMiscCache() {
        AwareWakeUpManager.getInstance().updateControlParam();
        AwareWakeUpManager.getInstance().updateWhiteList();
    }

    public BroadcastMngRule getBroadcastMngRule(BroadcastSource source) {
        ArrayMap<EnumWithDesc, Config> featureRule = (ArrayMap) this.mAllConfig.get(AppMngFeature.BROADCAST);
        if (featureRule != null) {
            return (BroadcastMngRule) featureRule.get(source);
        }
        return null;
    }

    public ArrayMap<String, ListItem> getBrListItem(AppMngFeature feature, BroadcastSource source) {
        ArrayMap<EnumWithDesc, ArrayMap<String, ListItem>> featureList = (ArrayMap) this.mAllList.get(feature);
        if (featureList != null) {
            return (ArrayMap) featureList.get(source);
        }
        return null;
    }
}
