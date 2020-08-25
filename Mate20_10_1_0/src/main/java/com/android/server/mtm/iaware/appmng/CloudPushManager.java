package com.android.server.mtm.iaware.appmng;

import android.app.mtm.iaware.appmng.AppMngConstant;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.rms.iaware.AwareLog;
import android.util.ArrayMap;
import android.util.ArraySet;
import com.android.internal.os.BackgroundThread;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class CloudPushManager {
    private static final int CLOUD_UPDATE_MSG = 0;
    private static final String CPUFGCTRL = "CPUFGCTRL";
    private static final String EFFECTIVE_VERSION = "effectiveVersion";
    private static final String FEATURE_ID = "featureId";
    private static final String FEATURE_NAME = "featureName";
    private static final String FEATURE_SWITCH = "featureSwitch";
    private static final Object LOCK = new Object();
    private static final String PACKAGE_LIST = "pkglist";
    private static final String REMOVE = "remove";
    private static final String TAG = "CloudPushManager";
    private static CloudPushManager sCloudPushManager;
    private CloudPushManagerHandler mCloudPushManagerHandler;
    private final ArrayMap<Integer, FeatureCloudData> mFeatureCloudData = new ArrayMap<>();
    private final ArrayMap<String, Integer> mFeatureInfos = new ArrayMap<>();
    private AtomicBoolean mHasCloudPush = new AtomicBoolean(false);

    private class CloudPushManagerHandler extends Handler {
        public CloudPushManagerHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            if (msg == null) {
                AwareLog.e(CloudPushManager.TAG, "msg is null");
            } else if (msg.what == 0) {
                CloudPushManager.this.handleCloudUpdate(msg);
            }
        }
    }

    private static class FeatureCloudData {
        private String mEffectiveVersion;
        private String mFeatureName;
        private String mFeatureSwith;
        private ArraySet<String> mPkgList = new ArraySet<>();

        public FeatureCloudData(String name, String swith, ArrayList<String> pkgList, String version) {
            this.mFeatureName = name;
            this.mFeatureSwith = swith;
            if (pkgList != null) {
                this.mPkgList.addAll(pkgList);
            }
            this.mEffectiveVersion = version;
        }

        public boolean isPkgInCloudData(String pkg) {
            if (pkg == null) {
                return false;
            }
            return this.mPkgList.contains(pkg);
        }

        public String getFeatureSwith() {
            return this.mFeatureSwith;
        }

        public String toString() {
            return " featureName: " + this.mFeatureName + " featureSwith: " + this.mFeatureSwith + " pkglist: " + this.mPkgList + " effectiveVersion: " + this.mEffectiveVersion;
        }
    }

    private CloudPushManager() {
        initFeatureInfos();
        Looper looper = BackgroundThread.get().getLooper();
        if (looper != null) {
            this.mCloudPushManagerHandler = new CloudPushManagerHandler(looper);
        }
    }

    public static CloudPushManager getInstance() {
        CloudPushManager cloudPushManager;
        synchronized (LOCK) {
            if (sCloudPushManager == null) {
                sCloudPushManager = new CloudPushManager();
            }
            cloudPushManager = sCloudPushManager;
        }
        return cloudPushManager;
    }

    private void initFeatureInfos() {
        synchronized (this.mFeatureInfos) {
            this.mFeatureInfos.put(AppMngConstant.AppCleanSource.SYSTEM_MANAGER.getDesc(), 8000);
            this.mFeatureInfos.put(AppMngConstant.AppCleanSource.MEMORY.getDesc(), 8001);
            this.mFeatureInfos.put(AppMngConstant.AppCleanSource.POWER_GENIE.getDesc(), 8002);
            this.mFeatureInfos.put(AppMngConstant.AppCleanSource.CRASH.getDesc(), 8003);
            this.mFeatureInfos.put(AppMngConstant.AppCleanSource.SMART_CLEAN.getDesc(), 8004);
            this.mFeatureInfos.put(AppMngConstant.AppCleanSource.MEMORY_REPAIR.getDesc(), 8005);
            this.mFeatureInfos.put(AppMngConstant.AppCleanSource.MEMORY_REPAIR_VSS.getDesc(), 8006);
            this.mFeatureInfos.put(AppMngConstant.AppCleanSource.SYSTEM_MEMORY_REPAIR.getDesc(), 8007);
            this.mFeatureInfos.put(AppMngConstant.AppFreezeSource.FAST_FREEZE.getDesc(), 8008);
            this.mFeatureInfos.put(AppMngConstant.AppFreezeSource.CAMERA_FREEZE.getDesc(), 8009);
            this.mFeatureInfos.put(AppMngConstant.AppIoLimitSource.IOLIMIT.getDesc(), 8010);
            this.mFeatureInfos.put(AppMngConstant.AppIoLimitSource.CAMERA_IOLIMIT.getDesc(), 8011);
            this.mFeatureInfos.put(AppMngConstant.AppStartSource.START_SERVICE.getDesc(), 8012);
            this.mFeatureInfos.put(AppMngConstant.AppStartSource.BIND_SERVICE.getDesc(), 8013);
            this.mFeatureInfos.put(AppMngConstant.AppStartSource.PROVIDER.getDesc(), 8014);
            this.mFeatureInfos.put(AppMngConstant.AppStartSource.THIRD_BROADCAST.getDesc(), 8015);
            this.mFeatureInfos.put(AppMngConstant.AppStartSource.SYSTEM_BROADCAST.getDesc(), 8016);
            this.mFeatureInfos.put(AppMngConstant.AppStartSource.THIRD_ACTIVITY.getDesc(), 8017);
            this.mFeatureInfos.put(AppMngConstant.AppStartSource.JOB_SCHEDULE.getDesc(), 8018);
            this.mFeatureInfos.put(AppMngConstant.AppStartSource.ACCOUNT_SYNC.getDesc(), 8019);
            this.mFeatureInfos.put(AppMngConstant.AppStartSource.SCHEDULE_RESTART.getDesc(), 8020);
            this.mFeatureInfos.put(AppMngConstant.AppStartSource.ALARM.getDesc(), 8021);
            this.mFeatureInfos.put(CPUFGCTRL, 8022);
        }
    }

    public boolean isPkgInCloudData(String pkgName, AppMngConstant.EnumWithDesc configEnum) {
        String configName;
        Integer featureId;
        if (!this.mHasCloudPush.get() || pkgName == null || configEnum == null || (configName = configEnum.getDesc()) == null) {
            return false;
        }
        synchronized (this.mFeatureInfos) {
            featureId = this.mFeatureInfos.get(configName);
        }
        if (featureId == null) {
            return false;
        }
        synchronized (this.mFeatureCloudData) {
            FeatureCloudData data = this.mFeatureCloudData.get(featureId);
            if (data == null) {
                return false;
            }
            return data.isPkgInCloudData(pkgName);
        }
    }

    public boolean getFeatureSwitchByFeatureName(String featureName) {
        Integer featureId;
        synchronized (this.mFeatureInfos) {
            featureId = this.mFeatureInfos.get(featureName);
        }
        if (featureId == null) {
            return true;
        }
        synchronized (this.mFeatureCloudData) {
            FeatureCloudData data = this.mFeatureCloudData.get(featureId);
            if (data == null) {
                return true;
            }
            String featureSwitch = data.getFeatureSwith();
            if (featureSwitch == null) {
                return true;
            }
            if ("0".equals(featureSwitch)) {
                return false;
            }
            return true;
        }
    }

    public void reportCloudUpdate(Bundle bundle) {
        AwareLog.i(TAG, "reportCloudUpdate");
        if (this.mCloudPushManagerHandler == null) {
            AwareLog.e(TAG, "mCloudPushManagerHandler is null");
            return;
        }
        Message msg = Message.obtain();
        msg.setData(bundle);
        msg.what = 0;
        this.mCloudPushManagerHandler.sendMessage(msg);
    }

    /* access modifiers changed from: private */
    public void handleCloudUpdate(Message msg) {
        Bundle bundle;
        String featureId;
        if (msg != null && (bundle = msg.getData()) != null && (featureId = bundle.getString(FEATURE_ID)) != null) {
            try {
                int intFeatureid = Integer.parseInt(featureId);
                if (bundle.getString(REMOVE) != null) {
                    synchronized (this.mFeatureCloudData) {
                        this.mFeatureCloudData.remove(Integer.valueOf(intFeatureid));
                    }
                    return;
                }
                String featureName = bundle.getString(FEATURE_NAME);
                String featureSwitch = bundle.getString(FEATURE_SWITCH);
                try {
                    ArrayList<String> pkgList = bundle.getStringArrayList(PACKAGE_LIST);
                    String effectiveVersion = bundle.getString(EFFECTIVE_VERSION);
                    if (featureName != null && featureSwitch != null && effectiveVersion != null) {
                        FeatureCloudData data = new FeatureCloudData(featureName, featureSwitch, pkgList, effectiveVersion);
                        synchronized (this.mFeatureInfos) {
                            if (this.mFeatureInfos.containsValue(Integer.valueOf(intFeatureid))) {
                                synchronized (this.mFeatureCloudData) {
                                    this.mFeatureCloudData.put(Integer.valueOf(intFeatureid), data);
                                }
                                this.mHasCloudPush.set(true);
                            }
                        }
                    }
                } catch (ArrayIndexOutOfBoundsException e) {
                    AwareLog.e(TAG, "getStringArrayList out of bounds exception!");
                }
            } catch (NumberFormatException e2) {
                AwareLog.e(TAG, "featureid format error");
            }
        }
    }

    public void dump(PrintWriter pw) {
        if (pw != null) {
            pw.println("== Need Cloud Push Feature Config ==");
            synchronized (this.mFeatureInfos) {
                pw.println(this.mFeatureInfos);
            }
            pw.println("== Cloud Data ==");
            synchronized (this.mFeatureCloudData) {
                for (Map.Entry<Integer, FeatureCloudData> entry : this.mFeatureCloudData.entrySet()) {
                    if (entry != null) {
                        pw.println("feature id:" + entry.getKey());
                        pw.println(entry.getValue());
                    }
                }
            }
        }
    }
}
