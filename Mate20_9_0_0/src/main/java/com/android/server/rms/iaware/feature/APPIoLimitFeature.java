package com.android.server.rms.iaware.feature;

import android.content.Context;
import android.os.Bundle;
import android.rms.iaware.AwareConstant.FeatureType;
import android.rms.iaware.AwareConstant.ResourceType;
import android.rms.iaware.AwareLog;
import android.rms.iaware.CollectData;
import com.android.server.mtm.iaware.appmng.appiolimit.AwareAppIoLimitMng;
import com.android.server.rms.iaware.IRDataRegister;
import java.util.concurrent.atomic.AtomicBoolean;

public class APPIoLimitFeature extends RFeature {
    private static final int MIN_VERSION = 2;
    private static final String TAG = "APPIoLimitFeature";
    private static AtomicBoolean mIsInitialized = new AtomicBoolean(false);

    /* renamed from: com.android.server.rms.iaware.feature.APPIoLimitFeature$1 */
    static /* synthetic */ class AnonymousClass1 {
        static final /* synthetic */ int[] $SwitchMap$android$rms$iaware$AwareConstant$ResourceType = new int[ResourceType.values().length];

        static {
            try {
                $SwitchMap$android$rms$iaware$AwareConstant$ResourceType[ResourceType.RESOURCE_SCREEN_ON.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$android$rms$iaware$AwareConstant$ResourceType[ResourceType.RESOURCE_SCREEN_OFF.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$android$rms$iaware$AwareConstant$ResourceType[ResourceType.RESOURCE_APPASSOC.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$android$rms$iaware$AwareConstant$ResourceType[ResourceType.RESOURCE_STATUS_BAR.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$android$rms$iaware$AwareConstant$ResourceType[ResourceType.RESOURCE_MEDIA_BTN.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
        }
    }

    public APPIoLimitFeature(Context context, FeatureType featureType, IRDataRegister dataRegister) {
        super(context, featureType, dataRegister);
        initConfig();
    }

    public boolean reportData(CollectData data) {
        if (data == null) {
            return false;
        }
        Bundle bAppSSoc;
        switch (AnonymousClass1.$SwitchMap$android$rms$iaware$AwareConstant$ResourceType[ResourceType.getResourceType(data.getResId()).ordinal()]) {
            case 1:
                AwareAppIoLimitMng.getInstance().report(20011);
                return true;
            case 2:
                AwareAppIoLimitMng.getInstance().report(90011);
                return true;
            case 3:
                bAppSSoc = data.getBundle();
                if (bAppSSoc != null) {
                    AwareAppIoLimitMng.getInstance().report(bAppSSoc.getInt("relationType"), bAppSSoc);
                }
                return true;
            case 4:
                bAppSSoc = data.getBundle();
                if (bAppSSoc != null) {
                    AwareAppIoLimitMng.getInstance().report(bAppSSoc.getInt("eventid"));
                }
                return true;
            case 5:
                AwareLog.d(TAG, "click media button");
                bAppSSoc = data.getBundle();
                if (bAppSSoc != null) {
                    AwareAppIoLimitMng.getInstance().report(bAppSSoc.getInt("eventid"), bAppSSoc);
                }
                return true;
            default:
                return false;
        }
    }

    public boolean enable() {
        return false;
    }

    public boolean disable() {
        AwareLog.d(TAG, "disable App Io Limit Feature!");
        if (this.mIRDataRegister != null) {
            this.mIRDataRegister.unSubscribeData(ResourceType.RESOURCE_SCREEN_ON, this.mFeatureType);
            this.mIRDataRegister.unSubscribeData(ResourceType.RESOURCE_SCREEN_OFF, this.mFeatureType);
            this.mIRDataRegister.unSubscribeData(ResourceType.RESOURCE_APPASSOC, this.mFeatureType);
            this.mIRDataRegister.unSubscribeData(ResourceType.RESOURCE_STATUS_BAR, this.mFeatureType);
            this.mIRDataRegister.unSubscribeData(ResourceType.RESOURCE_MEDIA_BTN, this.mFeatureType);
        }
        AwareAppIoLimitMng.disable();
        return true;
    }

    public boolean enableFeatureEx(int realVersion) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("enable App Io Limit Feature version:!");
        stringBuilder.append(realVersion);
        AwareLog.d(str, stringBuilder.toString());
        if (realVersion >= 2) {
            if (this.mIRDataRegister != null) {
                this.mIRDataRegister.subscribeData(ResourceType.RESOURCE_SCREEN_ON, this.mFeatureType);
                this.mIRDataRegister.subscribeData(ResourceType.RESOURCE_SCREEN_OFF, this.mFeatureType);
                this.mIRDataRegister.subscribeData(ResourceType.RESOURCE_APPASSOC, this.mFeatureType);
                this.mIRDataRegister.subscribeData(ResourceType.RESOURCE_STATUS_BAR, this.mFeatureType);
                this.mIRDataRegister.subscribeData(ResourceType.RESOURCE_MEDIA_BTN, this.mFeatureType);
            }
            AwareAppIoLimitMng.enable();
        }
        return true;
    }

    private void initConfig() {
        if (!mIsInitialized.get()) {
            AwareAppIoLimitMng.getInstance();
            mIsInitialized.set(true);
        }
    }
}
