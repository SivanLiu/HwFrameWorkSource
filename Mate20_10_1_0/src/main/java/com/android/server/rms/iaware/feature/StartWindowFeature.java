package com.android.server.rms.iaware.feature;

import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.rms.iaware.AwareConfig;
import android.rms.iaware.AwareConstant;
import android.rms.iaware.AwareLog;
import android.rms.iaware.CollectData;
import android.rms.iaware.IAwareCMSManager;
import com.android.server.rms.iaware.IRDataRegister;
import java.util.Map;

public class StartWindowFeature extends RFeature {
    private static final int FEATURE_CONCURRENT_VERSION = 5;
    private static final int FEATURE_MIN_VERSION = 3;
    private static final String KIRIN_980 = "kirin980";
    private static final String KIRIN_990 = "kirin990";
    private static final String SUB_SWITCH_OFF = "0";
    private static final String SUB_SWITCH_ON = "1";
    private static final String TAG = "StartWindowFeature";
    private static final String TAG_CONFIG_NAME = "subSwitch";
    private static final String TAG_FEATURE_NAME = "StartWindowConcurrent";
    private static final String TAG_ITEM_SUBSWITCH = "subSwitch";
    private static boolean mConcurrentSwitch = false;
    private static boolean mFeature = false;
    private static boolean mIsConcurrent = SystemProperties.getBoolean("persist.sys.iaware.startwindow.concurrent", true);
    private static final boolean mStartWindowEnabled = SystemProperties.getBoolean("persist.sys.aware.stwin.enable", true);

    public StartWindowFeature(Context context, AwareConstant.FeatureType featureType, IRDataRegister dataRegister) {
        super(context, featureType, dataRegister);
    }

    @Override // com.android.server.rms.iaware.feature.RFeature
    public boolean enable() {
        AwareLog.i(TAG, "StartWindowFeature is a iaware3.0 feature, don't allow enable!");
        return false;
    }

    @Override // com.android.server.rms.iaware.feature.RFeature
    public boolean enableFeatureEx(int realVersion) {
        if (realVersion < 3) {
            AwareLog.i(TAG, "the min version of StartWindowFeature is 3, but current version is " + realVersion + ", don't allow enable!");
            return false;
        }
        if (realVersion < 5) {
            AwareLog.i(TAG, "enable white startwindow feature only");
        } else {
            enableConcurrentTransition();
        }
        setEnable(true);
        return true;
    }

    private void enableConcurrentTransition() {
        if (mIsConcurrent) {
            String custSwitch = getCustConfig();
            if (!"0".equals(custSwitch)) {
                if ("1".equals(custSwitch) || checkIfNeedConcurrentDefault()) {
                    setConcurrentSwitch(true);
                    AwareLog.i(TAG, "perform startwindow transition animation feature concurrently.");
                }
            }
        }
    }

    public static boolean getConcurrentSwitch() {
        return mConcurrentSwitch;
    }

    @Override // com.android.server.rms.iaware.feature.RFeature
    public boolean disable() {
        AwareLog.i(TAG, "disable white startwindow feature");
        disableConcurrentTransition();
        setEnable(false);
        return true;
    }

    private void disableConcurrentTransition() {
        setConcurrentSwitch(false);
        AwareLog.i(TAG, "disable concurrent transition.");
    }

    private static void setConcurrentSwitch(boolean conSwitch) {
        mConcurrentSwitch = conSwitch;
    }

    @Override // com.android.server.rms.iaware.feature.RFeature
    public boolean reportData(CollectData data) {
        return false;
    }

    private static void setEnable(boolean enable) {
        mFeature = enable;
    }

    public static boolean isStartWindowEnable() {
        return mFeature && mStartWindowEnabled;
    }

    private boolean checkIfNeedConcurrentDefault() {
        String platform = SystemProperties.get("ro.board.platform", "");
        return KIRIN_980.equals(platform) || KIRIN_990.equals(platform);
    }

    private String getCustConfig() {
        Map<String, String> configPropertries;
        AwareConfig configList = null;
        try {
            IBinder awareservice = IAwareCMSManager.getICMSManager();
            if (awareservice == null) {
                AwareLog.w(TAG, "can not find service awareservice.");
                return "";
            }
            configList = IAwareCMSManager.getCustConfig(awareservice, TAG_FEATURE_NAME, "subSwitch");
            if (configList == null) {
                AwareLog.i(TAG, "no cust config use default setting");
                return "";
            }
            for (AwareConfig.Item item : configList.getConfigList()) {
                if (item != null && (configPropertries = item.getProperties()) != null) {
                    return configPropertries.get("subSwitch");
                }
            }
            return "";
        } catch (RemoteException e) {
            AwareLog.e(TAG, "getConfig RemoteException!");
        }
    }
}
