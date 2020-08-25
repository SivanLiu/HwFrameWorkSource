package com.android.server.rms.iaware.feature;

import android.content.Context;
import android.os.SystemProperties;
import android.rms.iaware.AwareConstant;
import android.rms.iaware.AwareLog;
import android.rms.iaware.CollectData;
import com.android.server.rms.iaware.IRDataRegister;

public class ProfileFeature extends RFeature {
    private static final int BASE_VERSION = 5;
    private static final String SWITCH_PROFILE_FEATURE = "persist.sys.iaware.profile";
    private static final String TAG = "ProfileFeature";

    public ProfileFeature(Context context, AwareConstant.FeatureType type, IRDataRegister dataRegister) {
        super(context, type, dataRegister);
    }

    @Override // com.android.server.rms.iaware.feature.RFeature
    public boolean reportData(CollectData data) {
        return false;
    }

    @Override // com.android.server.rms.iaware.feature.RFeature
    public boolean enable() {
        return false;
    }

    @Override // com.android.server.rms.iaware.feature.RFeature
    public boolean disable() {
        SystemProperties.set(SWITCH_PROFILE_FEATURE, "false");
        AwareLog.i(TAG, "ProfileFeature disable");
        return true;
    }

    @Override // com.android.server.rms.iaware.feature.RFeature
    public boolean enableFeatureEx(int realVersion) {
        if (realVersion < 5) {
            SystemProperties.set(SWITCH_PROFILE_FEATURE, "false");
            return false;
        }
        SystemProperties.set(SWITCH_PROFILE_FEATURE, "true");
        AwareLog.i(TAG, "ProfileFeature enableFeatureEx");
        return true;
    }
}
