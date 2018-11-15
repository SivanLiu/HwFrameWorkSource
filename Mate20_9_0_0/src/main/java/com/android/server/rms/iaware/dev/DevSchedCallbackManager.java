package com.android.server.rms.iaware.dev;

import android.os.Bundle;
import android.rms.iaware.AwareLog;
import android.rms.iaware.IDeviceSettingCallback;
import com.android.server.rms.iaware.feature.DevSchedFeatureRT;
import java.util.ArrayList;
import java.util.List;

public class DevSchedCallbackManager {
    private static final String TAG = "DevSchedCallbackManager";
    private static Object mLock = new Object();
    private static DevSchedCallbackManager sInstance;
    private final List<DevRemoteCallbackList> mCallbackList = new ArrayList();

    private DevSchedCallbackManager() {
    }

    public static DevSchedCallbackManager getInstance() {
        DevSchedCallbackManager devSchedCallbackManager;
        synchronized (mLock) {
            if (sInstance == null) {
                sInstance = new DevSchedCallbackManager();
            }
            devSchedCallbackManager = sInstance;
        }
        return devSchedCallbackManager;
    }

    public void registerDevModeMethod(int deviceId, IDeviceSettingCallback callback, Bundle args) {
        String str;
        StringBuilder stringBuilder;
        if (callback == null) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("register FAILED, callback is null error. input deviceId:");
            stringBuilder.append(deviceId);
            AwareLog.e(str, stringBuilder.toString());
        } else if (DevSchedFeatureRT.checkDeviceIdAvailable(deviceId)) {
            synchronized (this.mCallbackList) {
                DevRemoteCallbackList devCallback = getDevRemoteCallback(deviceId);
                if (devCallback == null) {
                    devCallback = new DevRemoteCallbackList(deviceId);
                    this.mCallbackList.add(devCallback);
                }
                devCallback.registerDevModeMethod(callback, args);
            }
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("register SUCCESS, mCallbackList:");
            stringBuilder.append(toString());
            AwareLog.d(str, stringBuilder.toString());
            DevSchedFeatureRT.sendCurrentDeviceMode(deviceId);
        } else {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("register FAILED, deviceId is not available. input deviceId:");
            stringBuilder.append(deviceId);
            AwareLog.i(str, stringBuilder.toString());
        }
    }

    /* JADX WARNING: Missing block: B:15:0x004b, code:
            r0 = TAG;
            r1 = new java.lang.StringBuilder();
            r1.append("unregister SUCCESS, mCallbackList:");
            r1.append(toString());
            android.rms.iaware.AwareLog.d(r0, r1.toString());
     */
    /* JADX WARNING: Missing block: B:16:0x0066, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void unregisterDevModeMethod(int deviceId, IDeviceSettingCallback callback, Bundle args) {
        if (callback == null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("unregister FAILED, callback is null error. input deviceId:");
            stringBuilder.append(deviceId);
            AwareLog.e(str, stringBuilder.toString());
            return;
        }
        synchronized (this.mCallbackList) {
            DevRemoteCallbackList devCallback = getDevRemoteCallback(deviceId);
            if (devCallback == null) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("unregister FAILED, Non-Registered device, input deviceId:");
                stringBuilder2.append(deviceId);
                AwareLog.i(str2, stringBuilder2.toString());
                return;
            }
            devCallback.unregisterDevModeMethod(callback, args);
            if (devCallback.getCount() == 0) {
                this.mCallbackList.remove(devCallback);
            }
        }
    }

    public void sendDeviceMode(int deviceId, String packageName, int uid, int mode, Bundle bundle) {
        DevRemoteCallbackList devCallback;
        synchronized (this.mCallbackList) {
            devCallback = getDevRemoteCallback(deviceId);
        }
        String str;
        StringBuilder stringBuilder;
        if (devCallback == null) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("send device mode FAILED, Non-Registered device, input deviceId:");
            stringBuilder.append(deviceId);
            AwareLog.d(str, stringBuilder.toString());
            return;
        }
        devCallback.sendDeviceMode(packageName, uid, mode, bundle);
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("send device mode SUCCESS. deviceId:");
        stringBuilder.append(deviceId);
        stringBuilder.append(", packageName:");
        stringBuilder.append(packageName);
        stringBuilder.append(", mode:");
        stringBuilder.append(mode);
        AwareLog.i(str, stringBuilder.toString());
    }

    private DevRemoteCallbackList getDevRemoteCallback(int deviceId) {
        for (DevRemoteCallbackList callback : this.mCallbackList) {
            if (callback != null) {
                if (deviceId == callback.getDeviceId()) {
                    return callback;
                }
            }
        }
        return null;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        synchronized (this.mCallbackList) {
            sb.append("registered num :");
            sb.append(this.mCallbackList.size());
            sb.append(" [ ");
            sb.append(this.mCallbackList);
            sb.append(" ] ");
        }
        return sb.toString();
    }
}
