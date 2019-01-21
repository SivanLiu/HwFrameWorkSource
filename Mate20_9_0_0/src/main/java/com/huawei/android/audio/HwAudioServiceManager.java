package com.huawei.android.audio;

import android.app.ActivityThread;
import android.media.IAudioService;
import android.media.MediaRecorder;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import android.util.Singleton;
import com.android.internal.os.PowerProfile;
import com.huawei.android.audio.IHwAudioServiceManager.Stub;

public class HwAudioServiceManager {
    private static final Singleton<IHwAudioServiceManager> IAudioServiceManagerSingleton = new Singleton<IHwAudioServiceManager>() {
        protected IHwAudioServiceManager create() {
            try {
                return Stub.asInterface(HwAudioServiceManager.getAudioService().getHwInnerService());
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    };
    private static final String TAG = "HwAudioServiceManager";
    private static IAudioService sService;

    private static IAudioService getAudioService() {
        if (sService != null) {
            return sService;
        }
        sService = IAudioService.Stub.asInterface(ServiceManager.getService(PowerProfile.POWER_AUDIO));
        return sService;
    }

    public static IHwAudioServiceManager getService() {
        return (IHwAudioServiceManager) IAudioServiceManagerSingleton.get();
    }

    public static int setSoundEffectState(boolean restore, String packageName, boolean isOnTop, String reserved) {
        try {
            return getService().setSoundEffectState(restore, packageName, isOnTop, reserved);
        } catch (RemoteException e) {
            Log.e(TAG, "setSoundEffectState failed: catch RemoteException!");
            return -1;
        }
    }

    public static void checkRecordActive(int audioSource) {
        if (audioSource >= 0 && audioSource <= MediaRecorder.getAudioSourceMax()) {
            try {
                if (!getService().checkRecordActive()) {
                    getService().checkMicMute();
                }
            } catch (RemoteException e) {
                Log.e(TAG, "checkRecordActive or checkMicMute failed: catch RemoteException!");
            }
        }
    }

    public static void sendRecordStateChangedIntent(int state) {
        try {
            getService().sendRecordStateChangedIntent(MediaRecorder.class.getSimpleName(), state, Process.myPid(), ActivityThread.currentPackageName());
        } catch (RemoteException e) {
            Log.e(TAG, "sendRecordStateChangedIntent failed: catch RemoteException!");
        }
    }

    public static int getRecordConcurrentType() {
        try {
            return getService().getRecordConcurrentType(ActivityThread.currentPackageName());
        } catch (RemoteException e) {
            Log.e(TAG, "getRecordConcurrentType failed: catch RemoteException!");
            return 0;
        }
    }
}
