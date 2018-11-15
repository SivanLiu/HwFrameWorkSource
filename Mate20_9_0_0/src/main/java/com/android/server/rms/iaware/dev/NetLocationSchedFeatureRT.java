package com.android.server.rms.iaware.dev;

import android.content.Context;
import android.os.SystemClock;
import android.rms.iaware.AppTypeRecoManager;
import android.rms.iaware.AwareLog;
import android.rms.iaware.NetLocationStrategy;
import android.util.ArrayMap;
import com.android.server.rms.iaware.dev.PhoneStatusRecong.CurrentStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class NetLocationSchedFeatureRT extends DevSchedFeatureBase {
    private static final int AWARE_UNKNOWN_TYPE = -1;
    public static final long MODE_ALLOW_SCAN = 0;
    public static final long MODE_NOT_ALLOW_SCAN = -1;
    private static final int MUTIUSER_ADD_UID = 100000;
    public static final int PHONE_STATUS_ABS_STATIC = 1;
    public static final int PHONE_STATUS_DEFAULT = 0;
    public static final int PHONE_STATUS_MOVEMENT_LEVEL_ONE = 4;
    public static final int PHONE_STATUS_MOVEMENT_LEVEL_THREE = 6;
    public static final int PHONE_STATUS_MOVEMENT_LEVEL_TWO = 5;
    public static final int PHONE_STATUS_RELATIVE_STATIC_LEVEL_ONE = 2;
    public static final int PHONE_STATUS_RELATIVE_STATIC_LEVEL_TWO = 3;
    private static final int SCENE_ID_ABS_STATIC = 2111;
    private static final int SCENE_ID_DEFAULT = 2110;
    private static final int SCENE_ID_MOVEMENT_LEVEL_ONE = 2114;
    private static final int SCENE_ID_MOVEMENT_LEVEL_THREE = 2116;
    private static final int SCENE_ID_MOVEMENT_LEVEL_TWO = 2115;
    private static final int SCENE_ID_RELATIVE_STATIC_LEVEL_ONE = 2112;
    private static final int SCENE_ID_RELATIVE_STATIC_LEVEL_TWO = 2113;
    private static final String SCENE_NAME_ABS_STATIC = "static";
    private static final String SCENE_NAME_DEFAULT = "default";
    private static final String SCENE_NAME_MOVEMENT_LEVEL_ONE = "movement_one";
    private static final String SCENE_NAME_MOVEMENT_LEVEL_THREE = "movement_three";
    private static final String SCENE_NAME_MOVEMENT_LEVEL_TWO = "movement_two";
    private static final String SCENE_NAME_RELATIVE_STATIC_LEVEL_ONE = "relative_static_one";
    private static final String SCENE_NAME_RELATIVE_STATIC_LEVEL_TWO = "relative_static_two";
    private static final int SYSTEM_UID = 1000;
    private static final String TAG = "NetLocationSchedFeatureRT";
    private static final int THIRD_APP_UID = 10000;
    private static final ArrayMap<Integer, Integer> sPhoneStatusToSceneId = new ArrayMap();
    private Context mContext;
    private DevXmlConfig mDevXmlConfig = new DevXmlConfig();
    private boolean mIsInNavi = false;
    private boolean mIsParseXmlOk = false;
    private String mNetLocationName;
    private volatile int mPhoneStatus = 0;
    private final Map<Integer, SceneInfo> mSceneMap = new ArrayMap();

    static {
        sPhoneStatusToSceneId.put(Integer.valueOf(0), Integer.valueOf(SCENE_ID_DEFAULT));
        sPhoneStatusToSceneId.put(Integer.valueOf(1), Integer.valueOf(SCENE_ID_ABS_STATIC));
        sPhoneStatusToSceneId.put(Integer.valueOf(2), Integer.valueOf(SCENE_ID_RELATIVE_STATIC_LEVEL_ONE));
        sPhoneStatusToSceneId.put(Integer.valueOf(3), Integer.valueOf(SCENE_ID_RELATIVE_STATIC_LEVEL_TWO));
        sPhoneStatusToSceneId.put(Integer.valueOf(4), Integer.valueOf(SCENE_ID_MOVEMENT_LEVEL_ONE));
        sPhoneStatusToSceneId.put(Integer.valueOf(5), Integer.valueOf(SCENE_ID_MOVEMENT_LEVEL_TWO));
        sPhoneStatusToSceneId.put(Integer.valueOf(6), Integer.valueOf(SCENE_ID_MOVEMENT_LEVEL_THREE));
    }

    public NetLocationSchedFeatureRT(Context context, String name) {
        super(context);
        this.mContext = context;
        this.mNetLocationName = name;
        initXmlMap();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mNetLocationName is ");
        stringBuilder.append(this.mNetLocationName);
        stringBuilder.append(" create NetLocationSchedFeatureRT success");
        AwareLog.d(str, stringBuilder.toString());
    }

    public boolean handleUpdateCustConfig() {
        initXmlMap();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mNetLocationName is ");
        stringBuilder.append(this.mNetLocationName);
        stringBuilder.append(" update cust config success. mSceneMap is ");
        stringBuilder.append(this.mSceneMap);
        AwareLog.d(str, stringBuilder.toString());
        return true;
    }

    private void initXmlMap() {
        this.mSceneMap.clear();
        boolean z = initSceneRullMap() && isParseXmlOk();
        this.mIsParseXmlOk = z;
        if (!this.mIsParseXmlOk) {
            this.mSceneMap.clear();
        }
    }

    private boolean initSceneRullMap() {
        Map<String, ArrayList<String>> sceneRullMap = new ArrayMap();
        ArrayList<String> rullList = new ArrayList();
        rullList.add(SceneInfo.ITEM_RULE_ALLOW);
        rullList.add(SceneInfo.ITEM_RULE_SERVICE);
        rullList.add(SceneInfo.ITEM_RULE_NOT_ALLOW);
        sceneRullMap.put("default", rullList);
        ArrayList<String> rullList1 = new ArrayList();
        rullList1.add(SceneInfo.ITEM_RULE_ALLOW);
        rullList1.add(SceneInfo.ITEM_RULE_SERVICE);
        sceneRullMap.put(SCENE_NAME_ABS_STATIC, rullList1);
        ArrayList<String> rullList2 = new ArrayList();
        rullList2.add(SceneInfo.ITEM_RULE_ALLOW);
        rullList2.add(SceneInfo.ITEM_RULE_SERVICE);
        sceneRullMap.put(SCENE_NAME_RELATIVE_STATIC_LEVEL_ONE, rullList2);
        ArrayList<String> rullList3 = new ArrayList();
        rullList3.add(SceneInfo.ITEM_RULE_ALLOW);
        rullList3.add(SceneInfo.ITEM_RULE_SERVICE);
        sceneRullMap.put(SCENE_NAME_RELATIVE_STATIC_LEVEL_TWO, rullList3);
        ArrayList<String> rullList4 = new ArrayList();
        rullList4.add(SceneInfo.ITEM_RULE_ALLOW);
        rullList4.add(SceneInfo.ITEM_RULE_SERVICE);
        sceneRullMap.put(SCENE_NAME_MOVEMENT_LEVEL_ONE, rullList4);
        ArrayList<String> rullList5 = new ArrayList();
        rullList5.add(SceneInfo.ITEM_RULE_ALLOW);
        rullList5.add(SceneInfo.ITEM_RULE_SERVICE);
        sceneRullMap.put(SCENE_NAME_MOVEMENT_LEVEL_TWO, rullList5);
        ArrayList<String> rullList6 = new ArrayList();
        rullList6.add(SceneInfo.ITEM_RULE_ALLOW);
        rullList6.add(SceneInfo.ITEM_RULE_SERVICE);
        sceneRullMap.put(SCENE_NAME_MOVEMENT_LEVEL_THREE, rullList6);
        return this.mDevXmlConfig.readDevStrategy(this.mSceneMap, this.mNetLocationName, sceneRullMap);
    }

    private List<CurrentStatus> getMotionStatus() {
        PhoneStatusRecong.getInstance().getDeviceStatus();
        return PhoneStatusRecong.getInstance().getCurrentStatus();
    }

    private boolean isParseXmlOk() {
        for (Entry<Integer, Integer> entry : sPhoneStatusToSceneId.entrySet()) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("mNetLocationName is ");
            stringBuilder.append(this.mNetLocationName);
            stringBuilder.append(" value is ");
            stringBuilder.append(entry.getValue());
            AwareLog.d(str, stringBuilder.toString());
            if (entry.getValue() == null) {
                return false;
            }
        }
        return true;
    }

    public NetLocationStrategy getNetLocationStrategy(String pkgName, int uid) {
        String str = pkgName;
        int i = uid;
        String str2;
        StringBuilder stringBuilder;
        if (str == null || i <= 0 || !this.mIsParseXmlOk || this.mIsInNavi) {
            str2 = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("mNetLocationName is ");
            stringBuilder.append(this.mNetLocationName);
            stringBuilder.append(" mIsInNavi is ");
            stringBuilder.append(this.mIsInNavi);
            stringBuilder.append(" mIsParseXmlOk is ");
            stringBuilder.append(this.mIsParseXmlOk);
            stringBuilder.append(" getNetLocationStrategy null");
            AwareLog.d(str2, stringBuilder.toString());
            return null;
        }
        long mode = 0;
        long timestamp = SystemClock.elapsedRealtime();
        if (i == 1000 || i > 10000) {
            int pkgType = AppTypeRecoManager.getInstance().getAppType(str);
            String str3 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("mNetLocationName is ");
            stringBuilder2.append(this.mNetLocationName);
            stringBuilder2.append(" pkgType is ");
            stringBuilder2.append(pkgType);
            stringBuilder2.append(" pkgName is ");
            stringBuilder2.append(str);
            stringBuilder2.append(" uid is ");
            stringBuilder2.append(i);
            AwareLog.d(str3, stringBuilder2.toString());
            if (pkgType == -1) {
                mode = 0;
            }
            long mode2 = mode;
            List<CurrentStatus> curDevStatus = getMotionStatus();
            if (curDevStatus == null) {
                str2 = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append(this.mNetLocationName);
                stringBuilder.append(" get device status failed.");
                AwareLog.e(str2, stringBuilder.toString());
                return null;
            }
            String str4 = TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("mNetLocationName is ");
            stringBuilder3.append(this.mNetLocationName);
            stringBuilder3.append(", pkgName:");
            stringBuilder3.append(str);
            stringBuilder3.append(", curDevStatus:");
            stringBuilder3.append(curDevStatus);
            AwareLog.d(str4, stringBuilder3.toString());
            return getAppropriateStrategy(curDevStatus, str, pkgType, i, mode2, timestamp);
        }
        String str5 = TAG;
        StringBuilder stringBuilder4 = new StringBuilder();
        stringBuilder4.append("allow scan, mNetLocationName is ");
        stringBuilder4.append(this.mNetLocationName);
        stringBuilder4.append(" getNetLocationStrategy mode is ");
        stringBuilder4.append(0);
        stringBuilder4.append(" pkgName is ");
        stringBuilder4.append(str);
        AwareLog.d(str5, stringBuilder4.toString());
        return new NetLocationStrategy(0, timestamp);
    }

    private NetLocationStrategy getAppropriateStrategy(List<CurrentStatus> curDevStatus, String pkgName, int pkgType, int uid, long modeInit, long timestampInit) {
        String str = pkgName;
        NetLocationStrategy netLocationStrategy = null;
        String str2;
        StringBuilder stringBuilder;
        if (curDevStatus == null || str == null) {
            str2 = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append(this.mNetLocationName);
            stringBuilder.append(", curDevStatus or pkgName is null");
            AwareLog.d(str2, stringBuilder.toString());
            return null;
        }
        long mode = modeInit;
        long timestamp = timestampInit;
        for (CurrentStatus devStatus : curDevStatus) {
            if (devStatus != null) {
                timestamp = devStatus.getTimestamp();
                int phoneStatus = devStatus.getPhoneStatus();
                String str3 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append(this.mNetLocationName);
                stringBuilder2.append(", Current status :");
                stringBuilder2.append(phoneStatus);
                stringBuilder2.append(", current time :");
                stringBuilder2.append(timestamp);
                AwareLog.d(str3, stringBuilder2.toString());
                int sceneId = convertPhoneStatusToSceneId(phoneStatus);
                SceneInfo sceneInfo = (SceneInfo) this.mSceneMap.get(Integer.valueOf(sceneId));
                if (sceneInfo == null) {
                    String str4 = TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("no scene for ");
                    stringBuilder3.append(this.mNetLocationName);
                    stringBuilder3.append(", sceneId:");
                    stringBuilder3.append(sceneId);
                    AwareLog.i(str4, stringBuilder3.toString());
                    return netLocationStrategy;
                }
                int index = sceneInfo.isMatch(new Object[]{str, Integer.valueOf(pkgType)});
                if (index >= 0) {
                    mode = sceneInfo.getMode(index);
                    if (uid % 100000 == 1000) {
                        RuleBase ruleBase = sceneInfo.getRuleBase(index);
                        StringBuilder stringBuilder4;
                        if (ruleBase == null || !(ruleBase instanceof RuleService)) {
                            String str5 = TAG;
                            stringBuilder4 = new StringBuilder();
                            stringBuilder4.append(str);
                            stringBuilder4.append(" is system app and not RuleService obj, out of control");
                            AwareLog.d(str5, stringBuilder4.toString());
                            return null;
                        }
                        str2 = TAG;
                        stringBuilder4 = new StringBuilder();
                        stringBuilder4.append("mNetLocationName is ");
                        stringBuilder4.append(this.mNetLocationName);
                        stringBuilder4.append(" system uid");
                        AwareLog.d(str2, stringBuilder4.toString());
                        if (!((RuleService) ruleBase).getServiceList().contains(str)) {
                            mode = 0;
                        }
                    } else if (uid % 100000 < 10000) {
                        mode = 0;
                    }
                    str2 = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("match success, mNetLocationName is ");
                    stringBuilder.append(this.mNetLocationName);
                    stringBuilder.append(", phoneStatus:");
                    stringBuilder.append(phoneStatus);
                    AwareLog.d(str2, stringBuilder.toString());
                    str2 = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("iaware strategy, mNetLocationName is ");
                    stringBuilder.append(this.mNetLocationName);
                    stringBuilder.append(" getNetLocationStrategy mode is ");
                    stringBuilder.append(mode);
                    stringBuilder.append(" pkgName is ");
                    stringBuilder.append(str);
                    AwareLog.d(str2, stringBuilder.toString());
                    return new NetLocationStrategy(mode, timestamp);
                }
                netLocationStrategy = null;
            }
        }
        str2 = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("iaware strategy, mNetLocationName is ");
        stringBuilder.append(this.mNetLocationName);
        stringBuilder.append(" getNetLocationStrategy mode is ");
        stringBuilder.append(mode);
        stringBuilder.append(" pkgName is ");
        stringBuilder.append(str);
        AwareLog.d(str2, stringBuilder.toString());
        return new NetLocationStrategy(mode, timestamp);
    }

    private int convertPhoneStatusToSceneId(int phoneStatus) {
        Integer convertType = (Integer) sPhoneStatusToSceneId.get(Integer.valueOf(phoneStatus));
        if (convertType == null) {
            return SCENE_ID_DEFAULT;
        }
        return convertType.intValue();
    }

    public boolean handlerNaviStatus(boolean isInNavi) {
        this.mIsInNavi = isInNavi;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mNetLocationName is ");
        stringBuilder.append(this.mNetLocationName);
        stringBuilder.append(" mIsInNavi is ");
        stringBuilder.append(this.mIsInNavi);
        AwareLog.d(str, stringBuilder.toString());
        return true;
    }
}
