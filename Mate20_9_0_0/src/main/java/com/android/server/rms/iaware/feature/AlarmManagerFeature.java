package com.android.server.rms.iaware.feature;

import android.content.Context;
import android.rms.iaware.AwareConstant.FeatureType;
import android.rms.iaware.AwareConstant.ResourceType;
import android.rms.iaware.AwareLog;
import android.rms.iaware.CollectData;
import com.android.server.rms.iaware.IRDataRegister;
import com.android.server.rms.iaware.appmng.AlarmManagerDumpRadar;
import com.android.server.rms.iaware.appmng.AwareWakeUpManager;

public class AlarmManagerFeature extends RFeature {
    private static final int BASE_VERSION = 2;
    private static final String TAG = "AlarmManagerFeature";
    private static boolean mFeature = false;

    /* renamed from: com.android.server.rms.iaware.feature.AlarmManagerFeature$1 */
    static /* synthetic */ class AnonymousClass1 {
        static final /* synthetic */ int[] $SwitchMap$android$rms$iaware$AwareConstant$ResourceType = new int[ResourceType.values().length];

        static {
            try {
                $SwitchMap$android$rms$iaware$AwareConstant$ResourceType[ResourceType.RESOURCE_SCREEN_OFF.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$android$rms$iaware$AwareConstant$ResourceType[ResourceType.RESOURCE_SCREEN_ON.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
        }
    }

    public AlarmManagerFeature(Context context, FeatureType featureType, IRDataRegister dataRegister) {
        super(context, featureType, dataRegister);
    }

    public boolean reportData(CollectData data) {
        if (data == null) {
            return false;
        }
        ResourceType[] types = ResourceType.values();
        if (types.length <= data.getResId() || data.getResId() < 0) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("bad resId = ");
            stringBuilder.append(data.getResId());
            AwareLog.e(str, stringBuilder.toString());
            return false;
        }
        switch (AnonymousClass1.$SwitchMap$android$rms$iaware$AwareConstant$ResourceType[types[data.getResId()].ordinal()]) {
            case 1:
                AwareWakeUpManager.getInstance().screenOff();
                break;
            case 2:
                AwareWakeUpManager.getInstance().screenOn();
                break;
        }
        return true;
    }

    public boolean enable() {
        AwareLog.i(TAG, "enable failed! feature based on IAware2.0, enable() method should not be called!");
        return false;
    }

    public boolean disable() {
        AwareLog.i(TAG, "disable iaware AlarmManagerFeature!");
        setEnable(false);
        subscribeData(false);
        return true;
    }

    public boolean enableFeatureEx(int realVersion) {
        if (realVersion < 2) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("enableFeatureEx failed, realVersion: ");
            stringBuilder.append(realVersion);
            stringBuilder.append(", AlarmManagerFeature baseVersion: ");
            stringBuilder.append(2);
            AwareLog.i(str, stringBuilder.toString());
            return false;
        }
        AwareLog.i(TAG, "enableFeatureEx iaware AlarmManagerFeature!");
        setEnable(true);
        subscribeData(true);
        return true;
    }

    private static void setEnable(boolean enable) {
        mFeature = enable;
    }

    private void subscribeData(boolean enable) {
        if (this.mIRDataRegister == null) {
            AwareLog.e(TAG, "mIRDataRegister is null, can't subscribe data!");
            return;
        }
        if (enable) {
            this.mIRDataRegister.subscribeData(ResourceType.RESOURCE_SCREEN_OFF, this.mFeatureType);
            this.mIRDataRegister.subscribeData(ResourceType.RESOURCE_SCREEN_ON, this.mFeatureType);
        } else {
            this.mIRDataRegister.unSubscribeData(ResourceType.RESOURCE_SCREEN_OFF, this.mFeatureType);
            this.mIRDataRegister.unSubscribeData(ResourceType.RESOURCE_SCREEN_ON, this.mFeatureType);
        }
    }

    public String saveBigData(boolean clear) {
        AwareLog.i(TAG, "Feature based on IAware2.0, saveBigData return null.");
        return null;
    }

    public String getBigDataByVersion(int iawareVer, boolean forBeta, boolean clearData) {
        if (mFeature && forBeta && iawareVer >= 2) {
            return AlarmManagerDumpRadar.getInstance().saveBigData(clearData);
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("bigdata is not support, mFeature=");
        stringBuilder.append(mFeature);
        stringBuilder.append(", iawareVer=");
        stringBuilder.append(iawareVer);
        AwareLog.e(str, stringBuilder.toString());
        return null;
    }

    public static boolean isEnable() {
        return mFeature;
    }
}
