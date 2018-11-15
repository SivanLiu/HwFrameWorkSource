package com.android.server.rms.iaware.feature;

import android.content.Context;
import android.rms.iaware.AwareConstant.FeatureType;
import android.rms.iaware.AwareLog;
import android.rms.iaware.CollectData;
import com.android.server.rms.iaware.IRDataRegister;
import com.android.server.rms.iaware.appmng.AwareAppPreloadResourceManager;

public class PreloadResourceFeature extends RFeature {
    private static final int BASE_VERSION = 3;
    private static final String TAG = "PreloadResourceFeature";

    public PreloadResourceFeature(Context context, FeatureType type, IRDataRegister dataRegister) {
        super(context, type, dataRegister);
    }

    public boolean enable() {
        return false;
    }

    public boolean enableFeatureEx(int realVersion) {
        if (realVersion < 3) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("enableFeatureEx failed, realVersion: ");
            stringBuilder.append(realVersion);
            stringBuilder.append(", baseVersion: ");
            stringBuilder.append(3);
            AwareLog.i(str, stringBuilder.toString());
            return false;
        }
        AwareAppPreloadResourceManager.getInstance().enable();
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("enableFeatureEx realVersion=");
        stringBuilder2.append(realVersion);
        AwareLog.d(str2, stringBuilder2.toString());
        return true;
    }

    public boolean disable() {
        AwareAppPreloadResourceManager.getInstance().disable();
        AwareLog.d(TAG, "PreloadResourceFeature  disable");
        return true;
    }

    public boolean reportData(CollectData data) {
        return false;
    }
}
