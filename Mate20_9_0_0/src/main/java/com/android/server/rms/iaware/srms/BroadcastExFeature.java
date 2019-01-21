package com.android.server.rms.iaware.srms;

import android.app.mtm.iaware.appmng.AppMngConstant.AppMngFeature;
import android.app.mtm.iaware.appmng.AppMngConstant.BroadcastSource;
import android.content.Context;
import android.rms.iaware.AwareConstant.FeatureType;
import android.rms.iaware.AwareLog;
import android.rms.iaware.CollectData;
import android.util.ArrayMap;
import android.util.ArraySet;
import com.android.server.mtm.MultiTaskManagerService;
import com.android.server.mtm.iaware.appmng.DecisionMaker;
import com.android.server.mtm.iaware.appmng.rule.ListItem;
import com.android.server.mtm.iaware.srms.AwareBroadcastDumpRadar;
import com.android.server.rms.iaware.IRDataRegister;
import com.android.server.rms.iaware.feature.RFeature;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

public class BroadcastExFeature extends RFeature {
    public static final String BR_FILTER_BRAPPBACKLIST = "filter_blackbrapp_list";
    public static final String BR_FILTER_BRAPPWHITELIST = "filter_whitebrapp_list";
    public static final String BR_FILTER_SWITCH = "filterSwitch";
    public static final String BR_FILTER_WHITELIST = "filter_white_list";
    public static final String BR_GOOGLE_APP_LIST = "br_google_app";
    public static final String BR_SEND_SWITCH = "SendSwitch";
    public static final int FILTER_SWITCH = 1;
    private static final int PARSE_LIST_LENGTH = 2;
    public static final int SEND_SWITCH = 2;
    private static final String TAG = "BroadcastExFeature";
    private static final int VERSION = 3;
    private static ArrayMap<String, ArraySet<String>> mBrFilterBlackApp = new ArrayMap();
    private static HashMap<String, Integer> mBrFilterData = new HashMap();
    private static ArrayMap<String, ArraySet<String>> mBrFilterWhiteApp = new ArrayMap();
    private static ArraySet<String> mBrFilterWhiteList = new ArraySet();
    private static ArraySet<String> mBrGoogleAppList = new ArraySet();
    private static boolean mBroadcastFilterEnable = false;
    private static boolean mBroadcastSendEnable = false;
    private static boolean mFeature = false;
    private static AtomicBoolean mIsInitialized = new AtomicBoolean(false);
    private AwareBroadcastDumpRadar mDumpRadar = null;

    public BroadcastExFeature(Context context, FeatureType featureType, IRDataRegister dataRegister) {
        super(context, featureType, dataRegister);
        initConfig();
    }

    public boolean enable() {
        return false;
    }

    public boolean disable() {
        AwareLog.i(TAG, "BroadcastExFeature disable");
        setEnable(false);
        return true;
    }

    public boolean enableFeatureEx(int realVersion) {
        if (realVersion >= 3) {
            AwareLog.i(TAG, "BroadcastExFeature 3.0 enableFeatureEx");
            setEnable(true);
            return true;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("enableFeatureEx failed, realVersion: ");
        stringBuilder.append(realVersion);
        stringBuilder.append(", BroadcastExFeature Version: ");
        stringBuilder.append(3);
        AwareLog.i(str, stringBuilder.toString());
        return false;
    }

    public boolean reportData(CollectData data) {
        return false;
    }

    private static void setEnable(boolean enable) {
        mFeature = enable;
    }

    public String getDFTDataByVersion(int iawareVer, boolean forBeta, boolean clearData, boolean betaEncode) {
        if (iawareVer < 3) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Feature based on IAware3.0, getBigDataByVersion return null. iawareVer: ");
            stringBuilder.append(iawareVer);
            AwareLog.i(str, stringBuilder.toString());
        } else if (!mFeature) {
            AwareLog.e(TAG, "Broadcast feature is disabled, it is invalid operation to save big data.");
            return null;
        } else if (getDumpRadar() != null) {
            return getDumpRadar().getDFTData(forBeta, clearData, betaEncode);
        }
        return null;
    }

    private AwareBroadcastDumpRadar getDumpRadar() {
        if (MultiTaskManagerService.self() != null) {
            this.mDumpRadar = MultiTaskManagerService.self().getIawareBrRadar();
        }
        return this.mDumpRadar;
    }

    public static boolean isFeatureEnabled(int type) {
        boolean z = false;
        if (type == 1) {
            if (mFeature && mBroadcastFilterEnable) {
                z = true;
            }
            return z;
        } else if (type != 2) {
            return false;
        } else {
            if (mFeature && mBroadcastSendEnable) {
                z = true;
            }
            return z;
        }
    }

    private void initConfig() {
        if (!mIsInitialized.get()) {
            mIsInitialized.set(true);
            updateConfig();
        }
    }

    public static void updateConfig() {
        ArrayList<String> filter = DecisionMaker.getInstance().getRawConfig(AppMngFeature.BROADCAST.getDesc(), BR_FILTER_SWITCH);
        if (filter != null && filter.size() == 1) {
            mBroadcastFilterEnable = switchOn((String) filter.get(0));
        }
        filter = DecisionMaker.getInstance().getRawConfig(AppMngFeature.BROADCAST.getDesc(), BR_SEND_SWITCH);
        if (filter != null && filter.size() > 0) {
            mBroadcastSendEnable = switchOn((String) filter.get(0));
        }
        getBrList();
        getBrDataPolicy();
    }

    private static boolean switchOn(String value) {
        try {
            if (Integer.parseInt(value) == 1) {
                return true;
            }
            return false;
        } catch (NumberFormatException e) {
            AwareLog.e(TAG, "iaware_brEx value format error");
            return false;
        }
    }

    private static void getBrList() {
        ArrayList<String> whiteFilterList = DecisionMaker.getInstance().getRawConfig(AppMngFeature.BROADCAST.getDesc(), BR_FILTER_WHITELIST);
        if (whiteFilterList != null) {
            synchronized (mBrFilterWhiteList) {
                mBrFilterWhiteList.clear();
                int size = whiteFilterList.size();
                for (int index = 0; index < size; index++) {
                    mBrFilterWhiteList.add((String) whiteFilterList.get(index));
                }
            }
        }
        ArrayList<String> backBrAppFilterList = DecisionMaker.getInstance().getRawConfig(AppMngFeature.BROADCAST.getDesc(), BR_FILTER_BRAPPBACKLIST);
        if (backBrAppFilterList != null) {
            ArrayMap<String, ArraySet<String>> filterBackBrApp = parseResult(backBrAppFilterList);
            synchronized (mBrFilterBlackApp) {
                mBrFilterBlackApp.clear();
                mBrFilterBlackApp.putAll(filterBackBrApp);
            }
        }
        ArrayList<String> whiteBrAppFilterList = DecisionMaker.getInstance().getRawConfig(AppMngFeature.BROADCAST.getDesc(), BR_FILTER_BRAPPWHITELIST);
        if (whiteBrAppFilterList != null) {
            ArrayMap<String, ArraySet<String>> filterWhiteBrApp = parseResult(whiteBrAppFilterList);
            synchronized (mBrFilterWhiteApp) {
                mBrFilterWhiteApp.clear();
                mBrFilterWhiteApp.putAll(filterWhiteBrApp);
            }
        }
        ArrayList<String> googleAppList = DecisionMaker.getInstance().getRawConfig(AppMngFeature.BROADCAST.getDesc(), BR_GOOGLE_APP_LIST);
        if (googleAppList != null) {
            synchronized (mBrGoogleAppList) {
                mBrGoogleAppList.clear();
                int size2 = googleAppList.size();
                for (int index2 = 0; index2 < size2; index2++) {
                    mBrGoogleAppList.add((String) googleAppList.get(index2));
                }
            }
        }
    }

    private static ArrayMap<String, ArraySet<String>> parseResult(ArrayList<String> results) {
        ArrayMap<String, ArraySet<String>> parseResult = new ArrayMap();
        int size = results.size();
        for (int index = 0; index < size; index++) {
            String[] contentArray = ((String) results.get(index)).split(":");
            if (contentArray.length == 2) {
                ArraySet<String> apps = new ArraySet();
                int i = 0;
                String action = contentArray[0].trim();
                String[] appPkgName = contentArray[1].trim().split(CPUCustBaseConfig.CPUCONFIG_GAP_IDENTIFIER);
                int appSize = appPkgName.length;
                while (i < appSize) {
                    apps.add(appPkgName[i].trim());
                    i++;
                }
                parseResult.put(action, apps);
            } else {
                AwareLog.e(TAG, "iaware_brEx value format error");
            }
        }
        return parseResult;
    }

    private static void getBrDataPolicy() {
        ArrayMap<String, ListItem> filterBrDatas = DecisionMaker.getInstance().getBrListItem(AppMngFeature.BROADCAST, BroadcastSource.BROADCAST_FILTER);
        if (filterBrDatas != null) {
            synchronized (mBrFilterData) {
                mBrFilterData.clear();
                for (Entry<String, ListItem> ent : filterBrDatas.entrySet()) {
                    String action = (String) ent.getKey();
                    ListItem item = (ListItem) ent.getValue();
                    if (!(action == null || item == null)) {
                        mBrFilterData.put(action, Integer.valueOf(item.getPolicy()));
                    }
                }
            }
        }
    }

    public static boolean isBrFilterWhiteList(String pkgName) {
        boolean contains;
        synchronized (mBrFilterWhiteList) {
            contains = mBrFilterWhiteList.contains(pkgName);
        }
        return contains;
    }

    public static ArraySet<String> getBrFilterWhiteList() {
        ArraySet<String> whiteList = new ArraySet();
        synchronized (mBrFilterWhiteList) {
            whiteList.addAll(mBrFilterWhiteList);
        }
        return whiteList;
    }

    public static int getBrFilterPolicy(String action) {
        synchronized (mBrFilterData) {
            Integer policy = (Integer) mBrFilterData.get(action);
            if (policy != null) {
                int intValue = policy.intValue();
                return intValue;
            }
            return -1;
        }
    }

    public static boolean isBrFilterBlackApp(String action, String pkgName) {
        synchronized (mBrFilterBlackApp) {
            ArraySet<String> apps = (ArraySet) mBrFilterBlackApp.get(action);
            if (apps != null) {
                boolean contains = apps.contains(pkgName);
                return contains;
            }
            return false;
        }
    }

    public static ArrayMap<String, ArraySet<String>> getBrFilterBlackApp() {
        ArrayMap arrayMap;
        synchronized (mBrFilterBlackApp) {
            arrayMap = new ArrayMap(mBrFilterBlackApp);
        }
        return arrayMap;
    }

    public static boolean isBrFilterWhiteApp(String action, String pkgName) {
        synchronized (mBrFilterWhiteApp) {
            ArraySet<String> apps = (ArraySet) mBrFilterWhiteApp.get(action);
            if (apps != null) {
                boolean contains = apps.contains(pkgName);
                return contains;
            }
            return false;
        }
    }

    public static ArrayMap<String, ArraySet<String>> getBrFilterWhiteApp() {
        ArrayMap arrayMap;
        synchronized (mBrFilterWhiteApp) {
            arrayMap = new ArrayMap(mBrFilterWhiteApp);
        }
        return arrayMap;
    }

    public static ArraySet<String> getBrGoogleAppList() {
        ArraySet<String> googleAppList = new ArraySet();
        synchronized (mBrGoogleAppList) {
            googleAppList.addAll(mBrGoogleAppList);
        }
        return googleAppList;
    }

    public static boolean isBrGoogleApp(String pkgName) {
        boolean contains;
        synchronized (mBrGoogleAppList) {
            contains = mBrGoogleAppList.contains(pkgName);
        }
        return contains;
    }
}
