package com.android.server.rms.iaware.memory.data.dispatch;

import android.os.Bundle;
import android.rms.iaware.AwareConstant;
import android.rms.iaware.AwareLog;
import android.rms.iaware.CollectData;
import com.android.server.rms.algorithm.AwareUserHabit;
import com.android.server.rms.iaware.memory.data.content.AttrSegments;
import com.android.server.rms.iaware.memory.data.handle.DataAppHandle;
import com.android.server.rms.iaware.memory.data.handle.DataDevStatusHandle;
import com.android.server.rms.iaware.memory.data.handle.DataInputHandle;
import com.android.server.rms.iaware.memory.utils.BigMemoryInfo;
import com.android.server.rms.iaware.memory.utils.PrereadUtils;
import java.util.concurrent.atomic.AtomicBoolean;

public class DataDispatch {
    private static final Object LOCK = new Object();
    private static final String TAG = "AwareMem_DataDispatch";
    private static DataDispatch sDataDispatch;
    private final AtomicBoolean mRunning = new AtomicBoolean(false);

    public static DataDispatch getInstance() {
        DataDispatch dataDispatch;
        synchronized (LOCK) {
            if (sDataDispatch == null) {
                sDataDispatch = new DataDispatch();
            }
            dataDispatch = sDataDispatch;
        }
        return dataDispatch;
    }

    public void start() {
        AwareLog.i(TAG, "start");
        this.mRunning.set(true);
    }

    public void stop() {
        AwareLog.i(TAG, "stop");
        this.mRunning.set(false);
    }

    private int handleAppAndInput(CollectData data, AwareConstant.ResourceType type, long timestamp) {
        AttrSegments attrSegments = parseCollectData(data);
        if (!attrSegments.isValid()) {
            return -1;
        }
        if (type == AwareConstant.ResourceType.RES_APP) {
            return DataAppHandle.getInstance().reportData(timestamp, attrSegments.getEvent().intValue(), attrSegments);
        }
        return DataInputHandle.getInstance().reportData(timestamp, attrSegments.getEvent().intValue(), attrSegments);
    }

    private int handleBundleData(CollectData data, AwareConstant.ResourceType type, long timestamp) {
        Bundle bundle = data.getBundle();
        if (bundle == null) {
            return -1;
        }
        if (type == AwareConstant.ResourceType.RESOURCE_FACE_RECOGNIZE) {
            return DataDevStatusHandle.getInstance().reportData(timestamp, bundle.getInt("eventid"), null);
        } else if (type != AwareConstant.ResourceType.RESOURCE_USERHABIT) {
            return -1;
        } else {
            if (2 != bundle.getInt(AwareUserHabit.USERHABIT_INSTALL_APP_UPDATE)) {
                return 0;
            }
            String pkgName = bundle.getString(AwareUserHabit.USERHABIT_PACKAGE_NAME);
            int uninstallApkUid = bundle.getInt("uid");
            if (pkgName == null) {
                return 0;
            }
            PrereadUtils.getInstance();
            PrereadUtils.removePackageFiles(pkgName);
            BigMemoryInfo.getInstance().removeUidFromBigMemMap(uninstallApkUid, pkgName);
            return 0;
        }
    }

    public int reportData(CollectData data) {
        if (!this.mRunning.get() || data == null) {
            AwareLog.e(TAG, "DataDispatch not start");
            return -1;
        }
        long timestamp = data.getTimeStamp();
        AwareConstant.ResourceType type = AwareConstant.ResourceType.getResourceType(data.getResId());
        switch (AnonymousClass1.$SwitchMap$android$rms$iaware$AwareConstant$ResourceType[type.ordinal()]) {
            case 1:
            case 2:
                return handleAppAndInput(data, type, timestamp);
            case 3:
                return DataDevStatusHandle.getInstance().reportData(timestamp, 20011, null);
            case 4:
                return DataDevStatusHandle.getInstance().reportData(timestamp, 90011, null);
            case 5:
            case 6:
                return handleBundleData(data, type, timestamp);
            default:
                AwareLog.e(TAG, "Invalid ResourceType");
                return -1;
        }
    }

    /* renamed from: com.android.server.rms.iaware.memory.data.dispatch.DataDispatch$1  reason: invalid class name */
    static /* synthetic */ class AnonymousClass1 {
        static final /* synthetic */ int[] $SwitchMap$android$rms$iaware$AwareConstant$ResourceType = new int[AwareConstant.ResourceType.values().length];

        static {
            try {
                $SwitchMap$android$rms$iaware$AwareConstant$ResourceType[AwareConstant.ResourceType.RES_APP.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$android$rms$iaware$AwareConstant$ResourceType[AwareConstant.ResourceType.RES_INPUT.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$android$rms$iaware$AwareConstant$ResourceType[AwareConstant.ResourceType.RESOURCE_SCREEN_ON.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$android$rms$iaware$AwareConstant$ResourceType[AwareConstant.ResourceType.RESOURCE_SCREEN_OFF.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$android$rms$iaware$AwareConstant$ResourceType[AwareConstant.ResourceType.RESOURCE_USERHABIT.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$android$rms$iaware$AwareConstant$ResourceType[AwareConstant.ResourceType.RESOURCE_FACE_RECOGNIZE.ordinal()] = 6;
            } catch (NoSuchFieldError e6) {
            }
        }
    }

    private AttrSegments parseCollectData(CollectData data) {
        String eventData = data.getData();
        AttrSegments.Builder builder = new AttrSegments.Builder();
        builder.addCollectData(eventData);
        return builder.build();
    }
}
