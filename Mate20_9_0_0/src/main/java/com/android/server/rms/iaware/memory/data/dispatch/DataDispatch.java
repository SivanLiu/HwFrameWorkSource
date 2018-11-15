package com.android.server.rms.iaware.memory.data.dispatch;

import android.os.Bundle;
import android.rms.iaware.AwareConstant.ResourceType;
import android.rms.iaware.AwareLog;
import android.rms.iaware.CollectData;
import com.android.server.rms.algorithm.AwareUserHabit;
import com.android.server.rms.iaware.memory.data.content.AttrSegments;
import com.android.server.rms.iaware.memory.data.content.AttrSegments.Builder;
import com.android.server.rms.iaware.memory.data.handle.DataAppHandle;
import com.android.server.rms.iaware.memory.data.handle.DataDevStatusHandle;
import com.android.server.rms.iaware.memory.data.handle.DataInputHandle;
import com.android.server.rms.iaware.memory.utils.PrereadUtils;
import java.util.concurrent.atomic.AtomicBoolean;

public class DataDispatch {
    private static final String TAG = "AwareMem_DataDispatch";
    private static DataDispatch sDataDispatch;
    private final AtomicBoolean mRunning = new AtomicBoolean(false);

    /* renamed from: com.android.server.rms.iaware.memory.data.dispatch.DataDispatch$1 */
    static /* synthetic */ class AnonymousClass1 {
        static final /* synthetic */ int[] $SwitchMap$android$rms$iaware$AwareConstant$ResourceType = new int[ResourceType.values().length];

        static {
            try {
                $SwitchMap$android$rms$iaware$AwareConstant$ResourceType[ResourceType.RES_APP.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$android$rms$iaware$AwareConstant$ResourceType[ResourceType.RES_INPUT.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$android$rms$iaware$AwareConstant$ResourceType[ResourceType.RESOURCE_SCREEN_ON.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$android$rms$iaware$AwareConstant$ResourceType[ResourceType.RESOURCE_SCREEN_OFF.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$android$rms$iaware$AwareConstant$ResourceType[ResourceType.RESOURCE_USERHABIT.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
        }
    }

    public static DataDispatch getInstance() {
        DataDispatch dataDispatch;
        synchronized (DataDispatch.class) {
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

    public int reportData(CollectData data) {
        if (!this.mRunning.get() || data == null) {
            AwareLog.e(TAG, "DataDispatch not start");
            return -1;
        }
        long timestamp = data.getTimeStamp();
        int ret = -1;
        AttrSegments attrSegments;
        switch (AnonymousClass1.$SwitchMap$android$rms$iaware$AwareConstant$ResourceType[ResourceType.getResourceType(data.getResId()).ordinal()]) {
            case 1:
                attrSegments = parseCollectData(data);
                if (attrSegments.isValid()) {
                    ret = DataAppHandle.getInstance().reportData(timestamp, attrSegments.getEvent().intValue(), attrSegments);
                    break;
                }
                break;
            case 2:
                attrSegments = parseCollectData(data);
                if (attrSegments.isValid()) {
                    ret = DataInputHandle.getInstance().reportData(timestamp, attrSegments.getEvent().intValue(), attrSegments);
                    break;
                }
                break;
            case 3:
                ret = DataDevStatusHandle.getInstance().reportData(timestamp, 20011, null);
                break;
            case 4:
                ret = DataDevStatusHandle.getInstance().reportData(timestamp, 90011, null);
                break;
            case 5:
                Bundle bundle = data.getBundle();
                if (bundle != null && 2 == bundle.getInt(AwareUserHabit.USERHABIT_INSTALL_APP_UPDATE)) {
                    String pkgName = bundle.getString(AwareUserHabit.USERHABIT_PACKAGE_NAME);
                    if (pkgName != null) {
                        PrereadUtils.getInstance();
                        PrereadUtils.removePackageFiles(pkgName);
                        break;
                    }
                }
                break;
            default:
                AwareLog.e(TAG, "Invalid ResourceType");
                ret = -1;
                break;
        }
        return ret;
    }

    private AttrSegments parseCollectData(CollectData data) {
        String eventData = data.getData();
        Builder builder = new Builder();
        builder.addCollectData(eventData);
        return builder.build();
    }
}
