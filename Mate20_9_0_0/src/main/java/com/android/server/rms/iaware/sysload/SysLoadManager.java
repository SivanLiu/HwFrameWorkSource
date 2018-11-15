package com.android.server.rms.iaware.sysload;

import android.content.Context;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder.DeathRecipient;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.rms.iaware.AwareConstant;
import android.rms.iaware.AwareLog;
import android.rms.iaware.CollectData;
import android.rms.iaware.DeviceInfo;
import android.rms.iaware.ISceneCallback;
import android.util.ArrayMap;
import com.android.internal.os.BackgroundThread;
import com.android.server.input.HwInputManagerService;
import com.android.server.pfw.autostartup.comm.XmlConst.PreciseIgnore;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

public class SysLoadManager {
    private static final int FINGERSENSE_OPT = 4;
    private static final int GAME_SCENE_DELAYED = 0;
    private static final int INPUT_FILTER_OPT = 1;
    private static final int MSG_BASE_VALUE = 100;
    private static final int MSG_CHECK_STATUS = 108;
    private static final int MSG_CLOSE_FEATURE = 107;
    private static final int MSG_ENTER_GAME_SCENE = 101;
    private static final int MSG_EXIT_GAME_SCENE = 102;
    private static final int MSG_SINGLE_HAND_OFF = 106;
    private static final int MSG_SINGLE_HAND_ON = 105;
    private static final int MSG_WAKEUP_LOCK = 103;
    private static final int MSG_WAKEUP_RELEASE = 104;
    private static final int PROPERTY_OPT = 8;
    private static final int SINGLE_HAND_OPT = 2;
    private static final int SKIP_USER_ACTIVITY = 16;
    private static final String SYSLOAD_SINGLEHAND_TYPE = "LazyMode";
    private static final String TAG = "SysLoadManager";
    private static final AtomicBoolean mIsFeatureEnable = new AtomicBoolean(false);
    private static SysLoadManager sInstance;
    private static Object syncObject = new Object();
    private final ArrayMap<CallbackRecord, Integer> mCallbacks;
    private Context mContext;
    private HighLoadHandler mHighLoadHandler;
    private HwInputManagerService mInputManagerService;
    private int mInputStatus;
    private int mInputStatusCache;
    private AtomicBoolean mIsGameScene;
    private int mLockNum;
    private PowerManager mPowerManager;
    private int mSingleMode;
    private final SyncRoot mSyncRoot;

    private final class CallbackRecord implements DeathRecipient {
        private final ISceneCallback mCallback;
        public final int mUid;

        public CallbackRecord(int uid, ISceneCallback callback) {
            this.mUid = uid;
            this.mCallback = callback;
        }

        public void binderDied() {
            String str = SysLoadManager.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("GameScene listener for uid ");
            stringBuilder.append(this.mUid);
            stringBuilder.append(" died.");
            AwareLog.d(str, stringBuilder.toString());
            SysLoadManager.this.onCallbackDied(this);
        }

        public void notifySceneChangedAsync(boolean start, int scene) {
            try {
                this.mCallback.onSceneChanged(scene, start, 0, 0, "");
            } catch (RemoteException e) {
                String str = SysLoadManager.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to notify application ");
                stringBuilder.append(this.mUid);
                stringBuilder.append(" that game scene changed, assuming it died.");
                AwareLog.e(str, stringBuilder.toString());
                binderDied();
            }
        }
    }

    private class HighLoadHandler extends Handler {
        public HighLoadHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            String str;
            StringBuilder stringBuilder;
            super.handleMessage(msg);
            if (AwareConstant.CURRENT_USER_TYPE == 3) {
                str = SysLoadManager.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("handleMessage what = ");
                stringBuilder.append(msg.what);
                stringBuilder.append(" mLockNum:");
                stringBuilder.append(SysLoadManager.this.mLockNum);
                stringBuilder.append(" gameMode:");
                stringBuilder.append(SysLoadManager.this.mIsGameScene.get());
                AwareLog.i(str, stringBuilder.toString());
            }
            switch (msg.what) {
                case 101:
                    SysLoadManager.this.setGameScene();
                    return;
                case 102:
                    SysLoadManager.this.resetGameScene();
                    return;
                case 103:
                    SysLoadManager.this.mLockNum = SysLoadManager.this.mLockNum + 1;
                    if (SysLoadManager.this.mLockNum == 1) {
                        SysLoadManager.this.setIawareGameMode(true);
                        return;
                    }
                    return;
                case 104:
                    SysLoadManager.this.mLockNum = SysLoadManager.this.mLockNum - 1;
                    if (SysLoadManager.this.mLockNum <= 0) {
                        SysLoadManager.this.setIawareGameMode(false);
                        SysLoadManager.this.mLockNum = 0;
                        return;
                    }
                    return;
                case 105:
                    SysLoadManager.this.sendInputAwareSingleMode(true);
                    return;
                case 106:
                    SysLoadManager.this.sendInputAwareSingleMode(false);
                    return;
                case 107:
                    SysLoadManager.this.mLockNum = 0;
                    return;
                case 108:
                    if (SysLoadManager.this.mIsGameScene.get()) {
                        AwareLog.w(SysLoadManager.TAG, "check status current is game mode");
                        SysLoadManager.this.resetGameScene();
                        return;
                    } else if (SysLoadManager.this.mLockNum > 0) {
                        SysLoadManager.this.mLockNum = 0;
                        SysLoadManager.this.mInputStatus = SysLoadManager.this.mInputStatus & -17;
                        SysLoadManager.this.inputOptDisable();
                        return;
                    } else {
                        return;
                    }
                default:
                    str = SysLoadManager.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("error msg what = ");
                    stringBuilder.append(msg.what);
                    AwareLog.e(str, stringBuilder.toString());
                    return;
            }
        }
    }

    public static final class SyncRoot {
    }

    private SysLoadManager() {
        this.mHighLoadHandler = null;
        this.mIsGameScene = new AtomicBoolean(false);
        this.mLockNum = 0;
        this.mCallbacks = new ArrayMap();
        this.mSyncRoot = new SyncRoot();
        this.mInputManagerService = null;
        this.mPowerManager = null;
        this.mContext = null;
        this.mInputStatus = 2;
        this.mInputStatusCache = -1;
        this.mSingleMode = 0;
        this.mHighLoadHandler = new HighLoadHandler(BackgroundThread.get().getLooper());
    }

    public static SysLoadManager getInstance() {
        SysLoadManager sysLoadManager;
        synchronized (syncObject) {
            if (sInstance == null) {
                sInstance = new SysLoadManager();
            }
            sysLoadManager = sInstance;
        }
        return sysLoadManager;
    }

    private final void setIawareGameMode(boolean lock) {
        if (this.mIsGameScene.get()) {
            if (lock) {
                this.mInputStatus |= 16;
            } else {
                this.mInputStatus &= -17;
            }
            sendInputIawareMode(this.mInputStatus);
        }
    }

    private final void sendInputAwareSingleMode(boolean singleMode) {
        if (singleMode) {
            this.mInputStatus &= -3;
        } else {
            this.mInputStatus |= 2;
        }
        sendInputIawareMode(this.mInputStatus);
    }

    private final void sendInputIawareMode(int status) {
        if (this.mInputManagerService != null && this.mInputStatusCache != status) {
            if (AwareConstant.CURRENT_USER_TYPE == 3) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("send input mode finished cur mode:");
                stringBuilder.append(this.mInputStatusCache);
                stringBuilder.append(" to mode:");
                stringBuilder.append(status);
                AwareLog.i(str, stringBuilder.toString());
            }
            this.mInputStatusCache = status;
            this.mInputManagerService.setIawareGameMode(this.mInputStatusCache);
        }
    }

    public void enable() {
        if (mIsFeatureEnable.get()) {
            AwareLog.d(TAG, "SysLoadManager has already enable!");
        } else if (DeviceInfo.getDeviceLevel() < 0) {
            AwareLog.e(TAG, "Device Level unknow!");
        } else {
            mIsFeatureEnable.set(true);
        }
    }

    public void setContext(Context context) {
        this.mContext = context;
    }

    public void disable() {
        if (mIsFeatureEnable.get()) {
            mIsFeatureEnable.set(false);
            this.mHighLoadHandler.sendEmptyMessageDelayed(107, 0);
            if (this.mIsGameScene.get()) {
                inputOptDisable();
            }
            return;
        }
        AwareLog.d(TAG, "SysLoadManager has already disable!");
    }

    public void enterGameSceneMsg() {
        if (mIsFeatureEnable.get()) {
            this.mHighLoadHandler.removeMessages(101);
            this.mHighLoadHandler.sendEmptyMessageDelayed(101, 0);
        }
    }

    public void exitGameSceneMsg() {
        if (mIsFeatureEnable.get()) {
            this.mHighLoadHandler.removeMessages(102);
            this.mHighLoadHandler.sendEmptyMessageDelayed(102, 0);
        }
    }

    public void enterLauncher() {
        if (mIsFeatureEnable.get()) {
            this.mHighLoadHandler.sendEmptyMessageDelayed(108, 0);
        }
    }

    public void setInputManagerService(HwInputManagerService inputManagerService) {
        this.mInputManagerService = inputManagerService;
    }

    private void inputOptEnable() {
        if (DeviceInfo.getDeviceLevel() > 1) {
            this.mInputStatus |= 4;
        }
        if (this.mLockNum > 0) {
            this.mInputStatus |= 16;
        }
        sendInputIawareMode(this.mInputStatus);
    }

    private void inputOptDisable() {
        if (DeviceInfo.getDeviceLevel() > 1) {
            this.mInputStatus &= -5;
        }
        this.mLockNum = 0;
        this.mInputStatus &= -17;
        sendInputIawareMode(this.mInputStatus);
    }

    public void notifyWakeLock(int uid, int pid, String packageName, String tag) {
        if (!mIsFeatureEnable.get()) {
            AwareLog.d(TAG, "SysLoadOpt has already disable!");
        } else if (this.mInputManagerService != null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("acquire wakelock, pid: ");
            stringBuilder.append(pid);
            stringBuilder.append(", uid: ");
            stringBuilder.append(uid);
            stringBuilder.append(", packageName: ");
            stringBuilder.append(packageName);
            stringBuilder.append(", tag: ");
            stringBuilder.append(tag);
            AwareLog.d(str, stringBuilder.toString());
            this.mHighLoadHandler.sendEmptyMessageDelayed(103, 0);
        }
    }

    public void notifyWakeLockRelease(int uid, int pid, String packageName, String tag) {
        if (!mIsFeatureEnable.get()) {
            AwareLog.d(TAG, "SysLoadOpt has already disable!");
        } else if (this.mInputManagerService != null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("release wakelock, pid: ");
            stringBuilder.append(pid);
            stringBuilder.append(", uid: ");
            stringBuilder.append(uid);
            stringBuilder.append(", packageName: ");
            stringBuilder.append(packageName);
            stringBuilder.append(", tag: ");
            stringBuilder.append(tag);
            AwareLog.d(str, stringBuilder.toString());
            if (this.mIsGameScene.get()) {
                notifyToPMSUserActivity();
            }
            this.mHighLoadHandler.sendEmptyMessageDelayed(104, 0);
        }
    }

    private void notifyToPMSUserActivity() {
        if (this.mPowerManager == null && this.mContext != null) {
            this.mPowerManager = (PowerManager) this.mContext.getSystemService("power");
        }
        if (this.mPowerManager != null) {
            this.mPowerManager.userActivity(SystemClock.uptimeMillis(), 2, 0);
        } else {
            AwareLog.e(TAG, "power manager service is null");
        }
    }

    private void setGameScene() {
        if (mIsFeatureEnable.get()) {
            this.mIsGameScene.set(true);
            dispatchGameSceneChanged(true);
            inputOptEnable();
        }
    }

    private void resetGameScene() {
        if (mIsFeatureEnable.get()) {
            if (this.mLockNum > 0) {
                notifyToPMSUserActivity();
            }
            this.mIsGameScene.set(false);
            dispatchGameSceneChanged(false);
            inputOptDisable();
        }
    }

    public boolean isScene(int scene) {
        if (scene != 2) {
            return false;
        }
        return this.mIsGameScene.get();
    }

    public void reportData(CollectData data) {
        if (data != null) {
            Bundle bundle = data.getBundle();
            if (bundle != null) {
                this.mSingleMode = bundle.getInt(SYSLOAD_SINGLEHAND_TYPE);
                if (this.mSingleMode == 0) {
                    this.mHighLoadHandler.sendEmptyMessageDelayed(106, 0);
                } else {
                    this.mHighLoadHandler.sendEmptyMessageDelayed(105, 0);
                }
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("current SingleMode:");
                stringBuilder.append(this.mSingleMode == 0 ? "off" : PreciseIgnore.COMP_SCREEN_ON_VALUE_);
                AwareLog.d(str, stringBuilder.toString());
            }
        }
    }

    public boolean isLiteSysLoadEnable() {
        return mIsFeatureEnable.get();
    }

    public void registerCallback(ISceneCallback callback, int scene) {
        if (mIsFeatureEnable.get() && callback != null) {
            int callingUid = Binder.getCallingUid();
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("game scene registerCallback callback ");
            stringBuilder.append(callback);
            stringBuilder.append(" callingUid ");
            stringBuilder.append(callingUid);
            stringBuilder.append(" callingPid ");
            stringBuilder.append(Binder.getCallingPid());
            AwareLog.d(str, stringBuilder.toString());
            synchronized (this.mSyncRoot) {
                CallbackRecord record = new CallbackRecord(callingUid, callback);
                try {
                    callback.asBinder().linkToDeath(record, 0);
                } catch (RemoteException e) {
                    AwareLog.e(TAG, "Couldn't register for the death!!!!!");
                }
                this.mCallbacks.put(record, Integer.valueOf(scene));
                if ((scene & 3) != 0) {
                    record.notifySceneChangedAsync(this.mIsGameScene.get(), 2);
                }
            }
        }
    }

    private void onCallbackDied(CallbackRecord record) {
        synchronized (this.mSyncRoot) {
            this.mCallbacks.remove(record);
        }
    }

    private void dispatchGameSceneChanged(boolean start) {
        ArrayList<CallbackRecord> tempCallbacks = new ArrayList();
        synchronized (this.mSyncRoot) {
            tempCallbacks.clear();
            for (Entry<CallbackRecord, Integer> m : this.mCallbacks.entrySet()) {
                CallbackRecord callback = (CallbackRecord) m.getKey();
                Integer secens = (Integer) m.getValue();
                if (!(secens == null || (secens.intValue() & 3) == 0)) {
                    tempCallbacks.add(callback);
                }
            }
        }
        int count = tempCallbacks.size();
        for (int i = 0; i < count; i++) {
            ((CallbackRecord) tempCallbacks.get(i)).notifySceneChangedAsync(start, 2);
        }
        tempCallbacks.clear();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("dispatchGameSceneChanged count ");
        stringBuilder.append(count);
        AwareLog.d(str, stringBuilder.toString());
    }
}
