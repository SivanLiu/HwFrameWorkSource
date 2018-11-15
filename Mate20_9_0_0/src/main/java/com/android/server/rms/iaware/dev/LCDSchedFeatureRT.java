package com.android.server.rms.iaware.dev;

import android.content.Context;
import android.rms.iaware.AppTypeRecoManager;
import android.rms.iaware.AwareLog;
import android.util.ArrayMap;
import com.android.server.rms.iaware.memory.utils.MemoryConstant;
import java.util.Map;

public class LCDSchedFeatureRT extends DevSchedFeatureBase {
    private static final String TAG = "LCDSchedFeatureRT";
    private static final Map<Integer, Integer> sPgSceneToPreRecongMap = new ArrayMap();

    static {
        sPgSceneToPreRecongMap.put(Integer.valueOf(MemoryConstant.MSG_DIRECT_SWAPPINESS), Integer.valueOf(1));
        sPgSceneToPreRecongMap.put(Integer.valueOf(304), Integer.valueOf(19));
        sPgSceneToPreRecongMap.put(Integer.valueOf(305), Integer.valueOf(9));
        sPgSceneToPreRecongMap.put(Integer.valueOf(MemoryConstant.MSG_PROTECTLRU_SWITCH), Integer.valueOf(18));
        sPgSceneToPreRecongMap.put(Integer.valueOf(MemoryConstant.MSG_PROTECTLRU_SET_PROTECTRATIO), Integer.valueOf(6));
        sPgSceneToPreRecongMap.put(Integer.valueOf(MemoryConstant.MSG_PROTECTLRU_CONFIG_UPDATE), Integer.valueOf(8));
        sPgSceneToPreRecongMap.put(Integer.valueOf(310), Integer.valueOf(5));
        sPgSceneToPreRecongMap.put(Integer.valueOf(MemoryConstant.MSG_SET_PREREAD_PATH), Integer.valueOf(0));
        sPgSceneToPreRecongMap.put(Integer.valueOf(MemoryConstant.MSG_PREREAD_DATA_REMOVE), Integer.valueOf(7));
        sPgSceneToPreRecongMap.put(Integer.valueOf(MemoryConstant.MSG_COMPRESS_GPU), Integer.valueOf(3));
        sPgSceneToPreRecongMap.put(Integer.valueOf(MemoryConstant.MSG_PREREAD_FILE), Integer.valueOf(3));
        sPgSceneToPreRecongMap.put(Integer.valueOf(315), Integer.valueOf(12));
        sPgSceneToPreRecongMap.put(Integer.valueOf(318), Integer.valueOf(6));
        sPgSceneToPreRecongMap.put(Integer.valueOf(319), Integer.valueOf(14));
        sPgSceneToPreRecongMap.put(Integer.valueOf(321), Integer.valueOf(15));
        sPgSceneToPreRecongMap.put(Integer.valueOf(322), Integer.valueOf(10));
        sPgSceneToPreRecongMap.put(Integer.valueOf(323), Integer.valueOf(4));
        sPgSceneToPreRecongMap.put(Integer.valueOf(324), Integer.valueOf(17));
        sPgSceneToPreRecongMap.put(Integer.valueOf(325), Integer.valueOf(2));
    }

    public LCDSchedFeatureRT(Context context, String name) {
        super(context);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("create ");
        stringBuilder.append(name);
        stringBuilder.append("LCDSchedFeatureRT success.");
        AwareLog.d(str, stringBuilder.toString());
    }

    public boolean handleUpdateCustConfig() {
        return true;
    }

    public boolean handlerNaviStatus(boolean isInNavi) {
        return true;
    }

    public int getAppType(String pkgName) {
        if (pkgName == null) {
            return 255;
        }
        int appType = AppTypeRecoManager.getInstance().getAppType(pkgName);
        if (appType <= -1) {
            return 255;
        }
        if (appType <= 255) {
            return appType;
        }
        Integer convertType = (Integer) sPgSceneToPreRecongMap.get(Integer.valueOf(appType));
        if (convertType == null) {
            return appType;
        }
        return convertType.intValue();
    }
}
