package com.android.server.rms.iaware.srms;

import android.content.Context;
import android.os.SystemProperties;
import android.rms.iaware.AwareConstant.FeatureType;
import android.rms.iaware.AwareLog;
import android.rms.iaware.CollectData;
import com.android.server.mtm.iaware.appmng.appstart.AwareAppStartupPolicy;
import com.android.server.rms.iaware.IRDataRegister;
import com.android.server.rms.iaware.feature.RFeature;

public class AppStartupFeature extends RFeature {
    private static final int MIN_VERSION = 2;
    private static final String TAG = "AppStartupFeature";
    private static boolean mBetaUser;
    private static boolean mFeature = SystemProperties.getBoolean("persist.sys.appstart.enable", false);

    static {
        boolean z = false;
        if (SystemProperties.getInt("ro.logsystem.usertype", 1) == 3) {
            z = true;
        }
        mBetaUser = z;
    }

    public AppStartupFeature(Context context, FeatureType type, IRDataRegister dataRegister) {
        super(context, type, dataRegister);
    }

    public static boolean isAppStartupEnabled() {
        return mFeature;
    }

    public static boolean isBetaUser() {
        return mBetaUser;
    }

    public boolean reportData(CollectData data) {
        return true;
    }

    public boolean enable() {
        setEnable(false);
        return false;
    }

    public boolean disable() {
        setEnable(false);
        return true;
    }

    public boolean enableFeatureEx(int realVersion) {
        if (realVersion < 2) {
            setEnable(false);
            return false;
        }
        setEnable(true);
        AwareAppStartupPolicy policy = AwareAppStartupPolicy.self();
        if (policy != null) {
            policy.initSystemUidCache();
        }
        return true;
    }

    public String getBigDataByVersion(int iawareVer, boolean forBeta, boolean clearData) {
        String str;
        StringBuilder stringBuilder;
        if (!mFeature || iawareVer < 2) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("bigdata is not support, mFeature=");
            stringBuilder.append(mFeature);
            stringBuilder.append(", iawareVer=");
            stringBuilder.append(iawareVer);
            AwareLog.e(str, stringBuilder.toString());
            return null;
        } else if (mBetaUser == forBeta) {
            return SRMSDumpRadar.getInstance().saveStartupBigData(forBeta, clearData, false);
        } else {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("request bigdata is not match, betaUser=");
            stringBuilder.append(mBetaUser);
            stringBuilder.append(", forBeta=");
            stringBuilder.append(forBeta);
            stringBuilder.append(", clearData=");
            stringBuilder.append(clearData);
            AwareLog.i(str, stringBuilder.toString());
            return null;
        }
    }

    public static void setEnable(boolean on) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("iaware appstartup feature changed: ");
        stringBuilder.append(mFeature);
        stringBuilder.append("->");
        stringBuilder.append(on);
        AwareLog.i(str, stringBuilder.toString());
        if (mFeature != on) {
            AwareAppStartupPolicy policy = AwareAppStartupPolicy.self();
            if (policy != null) {
                policy.setEnable(on);
            }
            mFeature = on;
        }
    }
}
