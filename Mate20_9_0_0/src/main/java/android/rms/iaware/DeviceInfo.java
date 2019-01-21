package android.rms.iaware;

import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.content.Context;
import android.os.FreezeScreenScene;
import android.os.IBinder;
import android.os.RemoteException;

public final class DeviceInfo {
    public static final int DEFAULT_LEVEL = -1;
    public static final int HIGH_LEVEL = 1;
    public static final int LOW_LEVEL = 3;
    public static final int MID_LEVEL = 2;
    private static final String TAG = "DeviceInfo";
    private static int mLevel = -1;
    private static int mRamSize = 0;

    public static int getDeviceRAM(Context context) {
        if (context == null) {
            AwareLog.e(TAG, "getDeviceRAM context is null");
            return 0;
        } else if (mRamSize != 0) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getDeviceRAM ramSize:");
            stringBuilder.append(mRamSize);
            stringBuilder.append("MB");
            AwareLog.d(str, stringBuilder.toString());
            return mRamSize;
        } else {
            ActivityManager manager = (ActivityManager) context.getSystemService(FreezeScreenScene.ACTIVITY_PARAM);
            if (manager == null) {
                return 0;
            }
            MemoryInfo memInfo = new MemoryInfo();
            manager.getMemoryInfo(memInfo);
            mRamSize = (int) ((((memInfo.totalMem >> 20) + 1023) >> 10) << 10);
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("getDeviceRAM memInfo.totalMem:");
            stringBuilder2.append(memInfo.totalMem);
            stringBuilder2.append(" ramSize:");
            stringBuilder2.append(mRamSize);
            stringBuilder2.append("MB");
            AwareLog.d(str2, stringBuilder2.toString());
            return mRamSize;
        }
    }

    public static int getDeviceLevel() {
        String str;
        StringBuilder stringBuilder;
        if (mLevel != -1) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("getDeviceLevel level:");
            stringBuilder.append(mLevel);
            AwareLog.d(str, stringBuilder.toString());
            return mLevel;
        }
        try {
            IBinder awareservice = IAwareCMSManager.getICMSManager();
            if (awareservice != null) {
                mLevel = IAwareCMSManager.getDeviceLevel(awareservice);
            } else {
                AwareLog.e(TAG, "getDeviceLevel can not find service IAwareCMSService.");
            }
        } catch (RemoteException e) {
            AwareLog.e(TAG, "getDeviceLevel occur RemoteException");
        }
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("getDeviceLevel level:");
        stringBuilder.append(mLevel);
        AwareLog.d(str, stringBuilder.toString());
        return mLevel;
    }
}
