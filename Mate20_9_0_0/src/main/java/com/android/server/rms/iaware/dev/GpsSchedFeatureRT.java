package com.android.server.rms.iaware.dev;

import android.content.Context;
import android.rms.iaware.AwareConfig.Item;
import android.rms.iaware.AwareConfig.SubItem;
import android.rms.iaware.AwareLog;
import android.rms.iaware.LogIAware;
import android.util.ArrayMap;
import com.android.server.rms.iaware.memory.data.content.AttrSegments;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GpsSchedFeatureRT extends DevSchedFeatureBase {
    private static final String ACTIVITY_SCENE_NAME = "Activity_Start";
    private static final String ATTR_RULE_ACTIVITY_IN = "Activity_In";
    private static final String CONFIG_GPS_STRATEGY = "gps_strategy";
    private static final String FEATURE_TITLE = "DevSchedFeature";
    public static final String GPS_SPLIT_SYMBOL = ",";
    private static final String ITEM_GPS_MODE = "mode";
    private static final String ITEM_PACKAGE_NAME = "package_name";
    private static final String ITEM_RULE = "rule";
    private static final String ITEM_SCENE_NAME = "scenename";
    private static final String TAG = "GpsSchedFeatureRT";
    private final List<GpsActivityInfo> mActivityInfoList = new ArrayList();
    private String mCurrentActivityName;
    private String mCurrentPackageName;

    public GpsSchedFeatureRT(Context context, String name) {
        super(context);
        loadGpsActivityInfo(this.mActivityInfoList);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("create ");
        stringBuilder.append(name);
        stringBuilder.append("GpsSchedFeatureRT success");
        AwareLog.d(str, stringBuilder.toString());
    }

    public boolean handleResAppData(long timestamp, int event, AttrSegments attrSegments) {
        if (event != 15019 && event != 85019) {
            return false;
        }
        handleActivityEvent(event, attrSegments);
        return true;
    }

    private void handleActivityEvent(int event, AttrSegments attrSegments) {
        int i = event;
        if (attrSegments.isValid()) {
            ArrayMap<String, String> appInfo = attrSegments.getSegment("calledApp");
            if (appInfo == null) {
                AwareLog.i(TAG, "appInfo is NULL");
                return;
            }
            String packageName = (String) appInfo.get("packageName");
            String activityName = (String) appInfo.get("activityName");
            if (15019 == i) {
                this.mCurrentPackageName = packageName;
                this.mCurrentActivityName = activityName;
            }
            int pid = -1;
            int uid;
            try {
                uid = Integer.parseInt((String) appInfo.get("uid"));
                try {
                    int pid2 = Integer.parseInt((String) appInfo.get("pid"));
                    String str;
                    StringBuilder stringBuilder;
                    if (inInvalidActivityInfo(packageName, activityName, uid, pid2)) {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("isInvalidActivityInfo, packageName: ");
                        stringBuilder.append(packageName);
                        stringBuilder.append(", activityName: ");
                        stringBuilder.append(activityName);
                        stringBuilder.append(", uid: ");
                        stringBuilder.append(uid);
                        stringBuilder.append(", pid:");
                        stringBuilder.append(pid2);
                        AwareLog.i(str, stringBuilder.toString());
                        return;
                    }
                    GpsActivityInfo devActivityInfo = queryActivityInfo(packageName, activityName);
                    if (devActivityInfo != null) {
                        int mode = devActivityInfo.getLocationMode();
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("activity match success, packageName : ");
                        stringBuilder.append(packageName);
                        stringBuilder.append(", activityName : ");
                        stringBuilder.append(activityName);
                        stringBuilder.append(", mode : ");
                        stringBuilder.append(mode);
                        stringBuilder.append(", event : ");
                        stringBuilder.append(i);
                        stringBuilder.append(", uid is ");
                        stringBuilder.append(uid);
                        stringBuilder.append(", pid is ");
                        stringBuilder.append(pid2);
                        AwareLog.d(str, stringBuilder.toString());
                        reportActivityStateToNRT(i, packageName, activityName, uid, pid2, mode);
                    }
                } catch (NumberFormatException e) {
                    AwareLog.e(TAG, "get uid fail, happend NumberFormatException");
                }
            } catch (NumberFormatException e2) {
                uid = -1;
                AwareLog.e(TAG, "get uid fail, happend NumberFormatException");
            }
        }
    }

    private boolean inInvalidActivityInfo(String packageName, String activityName, int uid, int pid) {
        return packageName == null || activityName == null || uid <= 1000 || pid < 0;
    }

    private GpsActivityInfo queryActivityInfo(String packageName, String activityName) {
        if (packageName == null || activityName == null) {
            return null;
        }
        int size = this.mActivityInfoList.size();
        for (int index = 0; index < size; index++) {
            GpsActivityInfo devActivityInfo = (GpsActivityInfo) this.mActivityInfoList.get(index);
            if (devActivityInfo == null) {
                return null;
            }
            if (devActivityInfo.isMatch(packageName, activityName)) {
                return devActivityInfo;
            }
        }
        return null;
    }

    private void reportActivityStateToNRT(int event, String packageName, String activityName, int uid, int pid, int mode) {
        String str;
        StringBuilder stringBuilder;
        if (packageName == null || activityName == null) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("input param error, packageName is");
            stringBuilder.append(packageName);
            stringBuilder.append(", activityName is ");
            stringBuilder.append(activityName);
            AwareLog.e(str, stringBuilder.toString());
            return;
        }
        if (15019 == event) {
            reportActivityState(packageName, activityName, uid, pid, mode, true);
        } else if (85019 == event) {
            if (!packageName.equals(this.mCurrentPackageName) || queryActivityInfo(this.mCurrentPackageName, this.mCurrentActivityName) == null) {
                reportActivityState(packageName, activityName, uid, pid, mode, false);
            } else {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("switch control activity ");
                stringBuilder.append(activityName);
                stringBuilder.append(" to control activity ");
                stringBuilder.append(this.mCurrentActivityName);
                stringBuilder.append(", do not remove activity control. package name : ");
                stringBuilder.append(packageName);
                AwareLog.d(str, stringBuilder.toString());
            }
        }
    }

    private void reportActivityState(String packageName, String activityName, int uid, int pid, int mode, boolean in) {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(FEATURE_TITLE);
        stringBuffer.append(CPUCustBaseConfig.CPUCONFIG_INVALID_STR);
        stringBuffer.append(packageName);
        stringBuffer.append(CPUCustBaseConfig.CPUCONFIG_INVALID_STR);
        stringBuffer.append(activityName);
        stringBuffer.append(CPUCustBaseConfig.CPUCONFIG_INVALID_STR);
        stringBuffer.append(uid);
        stringBuffer.append(CPUCustBaseConfig.CPUCONFIG_INVALID_STR);
        stringBuffer.append(pid);
        stringBuffer.append(CPUCustBaseConfig.CPUCONFIG_INVALID_STR);
        stringBuffer.append(mode);
        LogIAware.report(in ? 2104 : 2105, stringBuffer.toString());
    }

    public boolean handleUpdateCustConfig() {
        LogIAware.report(2106, "");
        loadGpsActivityInfo(this.mActivityInfoList);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("update cust config success, mActivityInfoList is ");
        stringBuilder.append(this.mActivityInfoList);
        AwareLog.d(str, stringBuilder.toString());
        return true;
    }

    private void loadGpsActivityInfo(List<GpsActivityInfo> activityInfoList) {
        if (activityInfoList != null) {
            activityInfoList.clear();
            List<Item> itemList = DevXmlConfig.getItemList(CONFIG_GPS_STRATEGY);
            if (itemList == null) {
                AwareLog.e(TAG, "parse gps strategy config error!");
                return;
            }
            for (Item item : itemList) {
                if (item != null) {
                    Map<String, String> configPropertries = item.getProperties();
                    if (configPropertries != null) {
                        if (ACTIVITY_SCENE_NAME.equals((String) configPropertries.get("scenename"))) {
                            List<SubItem> subItemList = item.getSubItemList();
                            if (subItemList == null || subItemList.size() == 0) {
                                AwareLog.e(TAG, " subItemList is null");
                            } else {
                                parseSubItemList(subItemList, activityInfoList);
                            }
                        }
                    }
                }
            }
        }
    }

    private void parseSubItemList(List<SubItem> subItemList, List<GpsActivityInfo> activityInfoList) {
        if (subItemList != null && activityInfoList != null) {
            for (SubItem subItem : subItemList) {
                if (subItem != null) {
                    Map<String, String> properties = subItem.getProperties();
                    if (properties != null) {
                        if (ATTR_RULE_ACTIVITY_IN.equals((String) properties.get(ITEM_RULE))) {
                            String packageName = (String) properties.get("package_name");
                            if (packageName != null) {
                                try {
                                    GpsActivityInfo devActivityInfo = new GpsActivityInfo(packageName, Integer.parseInt((String) properties.get("mode")));
                                    if (devActivityInfo.loadActivitys(subItem.getValue())) {
                                        activityInfoList.add(devActivityInfo);
                                    }
                                } catch (NumberFormatException e) {
                                    String str = TAG;
                                    StringBuilder stringBuilder = new StringBuilder();
                                    stringBuilder.append("NumberFormatException, mode is not number! String mode is ");
                                    stringBuilder.append((String) properties.get("mode"));
                                    AwareLog.e(str, stringBuilder.toString());
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public boolean handlerNaviStatus(boolean isInNavi) {
        return true;
    }

    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append("GpsSchedFeatureRT : ");
        s.append(", GpsActivityInfo num : ");
        s.append(this.mActivityInfoList.size());
        s.append(", GpsActivityInfo : ");
        s.append(this.mActivityInfoList.toString());
        return s.toString();
    }
}
