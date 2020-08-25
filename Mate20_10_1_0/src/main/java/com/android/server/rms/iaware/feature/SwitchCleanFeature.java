package com.android.server.rms.iaware.feature;

import android.content.Context;
import android.rms.iaware.AwareConstant;
import android.rms.iaware.AwareLog;
import android.rms.iaware.CollectData;
import com.android.server.rms.iaware.IRDataRegister;
import com.android.server.rms.iaware.appmng.AwareSwitchCleanManager;
import java.util.concurrent.atomic.AtomicBoolean;

public class SwitchCleanFeature extends RFeature {
    private static final int BASE_VERSION = 5;
    private static final String TAG = "SwitchCleanFeature";
    private static AtomicBoolean mIsEnabled = new AtomicBoolean(false);

    public SwitchCleanFeature(Context context, AwareConstant.FeatureType type, IRDataRegister dataRegister) {
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
        if (!mIsEnabled.get()) {
            return false;
        }
        mIsEnabled.set(false);
        AwareLog.i(TAG, "SwitchCleanFeature disable");
        AwareSwitchCleanManager.getInstance().disable();
        return true;
    }

    @Override // com.android.server.rms.iaware.feature.RFeature
    public boolean enableFeatureEx(int realVersion) {
        if (realVersion < 5 || mIsEnabled.get()) {
            return false;
        }
        mIsEnabled.set(true);
        AwareLog.i(TAG, "SwitchCleanFeature enableFeatureEx");
        AwareSwitchCleanManager.getInstance().enable(this.mContext);
        return true;
    }
}
