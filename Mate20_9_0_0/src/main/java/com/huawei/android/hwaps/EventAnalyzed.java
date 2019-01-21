package com.huawei.android.hwaps;

import android.app.ActivityThread;
import android.app.HwApsInterface;
import android.aps.IApsManager;
import android.aps.IApsManagerServiceCallback;
import android.aps.IApsManagerServiceCallback.Stub;
import android.common.HwFrameworkFactory;
import android.content.Context;
import android.os.Handler;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.Log;
import com.huawei.android.hwaps.FpsRequest.SceneTypeE;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class EventAnalyzed implements IEventAnalyzed {
    public static final int ACTION_CANCEL = 3;
    public static final int ACTION_DOWN = 0;
    public static final int ACTION_MASK = 255;
    public static final int ACTION_MOVE = 2;
    public static final int ACTION_OUTSIDE = 4;
    public static final int ACTION_POINTER_DOWN = 5;
    public static final int ACTION_POINTER_UP = 6;
    public static final int ACTION_UP = 1;
    public static final int MAX_POWERKIT_FPS = 60;
    public static final int MIN_POWERKIT_FPS = 15;
    public static final int REGISTER_AGAIN = 180000;
    private static final int STOP_TOUCH_MAX_FPS_TIME = 3000;
    private static final String TAG = "Hwaps";
    private static final String VERSION = "9.0.0.21";
    private boolean isRegistedApsManagerServiceCallback = false;
    private IApsManager mApsManager;
    public IApsManagerServiceCallback mApsManagerServiceCallback;
    private int mBaseFps = 60;
    private FpsRequest mBaseFpsRequest;
    private Context mContext;
    private boolean mControlByAppSelf = false;
    private int mEventAnalyzeCount = 0;
    private boolean mFirstDynamicAdjustFps = true;
    private boolean mFirstTimeResumeFps = true;
    private GameState mGameState;
    private Handler mHandler;
    private boolean mHasIdentifyProcess = false;
    private int mIdleFps = -1;
    private int mIdleMaxFps = -1;
    private FpsRequest mIdleMaxFpsRequest;
    private boolean mIsGameProcess = false;
    private boolean mIsNeedRegisterCallback = true;
    private int mLastMode = -1;
    private long mLastRegisterTimeStampMs = -1;
    private int mMaxFps = -1;
    private int mMode = -1;
    private String mPkgName;
    private ResumeFpsByTouch mResumeFpsByTouch;
    private SdrController mSdrController;
    private int mStopTouchMaxFpsTime = 0;
    private Timer mTimer;
    private boolean mTouchResumeEnable = false;
    private FpsRequest miAwareFpsRequest;

    class ApsManagerServiceCallback extends Stub {
        ApsManagerServiceCallback() {
        }

        public void onAppsInfoChanged(List<String> list) {
        }

        public void doCallback(int apsCallbackCode, int data) {
            try {
                String str = EventAnalyzed.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("ApsManagerServiceCallback, doCallback, apsCallbackCode:");
                stringBuilder.append(apsCallbackCode);
                stringBuilder.append(", data:");
                stringBuilder.append(data);
                ApsCommon.logD(str, stringBuilder.toString());
                switch (apsCallbackCode) {
                    case EventAnalyzed.ACTION_DOWN /*0*/:
                        EventAnalyzed.this.setFps(data);
                        return;
                    case EventAnalyzed.ACTION_UP /*1*/:
                        EventAnalyzed.this.setResolutionRatio(data);
                        return;
                    case 2:
                        return;
                    case 3:
                        return;
                    default:
                        str = EventAnalyzed.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("ApsManagerServiceCallback, doCallback, error apsCallbackCode:");
                        stringBuilder.append(apsCallbackCode);
                        Log.e(str, stringBuilder.toString());
                        return;
                }
            } catch (Exception e) {
                e.printStackTrace();
                String str2 = EventAnalyzed.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("ApsManagerServiceCallback, doCallback, exception:");
                stringBuilder2.append(e);
                Log.e(str2, stringBuilder2.toString());
            }
        }
    }

    private class ResumeFpsByTouch implements Runnable {
        private ResumeFpsByTouch() {
        }

        /* synthetic */ ResumeFpsByTouch(EventAnalyzed x0, AnonymousClass1 x1) {
            this();
        }

        public void run() {
            if (EventAnalyzed.this.mResumeFpsByTouch != null) {
                EventAnalyzed.this.mIdleMaxFpsRequest.stop();
                EventAnalyzed.this.mTouchResumeEnable = true;
                String str = EventAnalyzed.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("APS: adjust to the base fps = ");
                stringBuilder.append(EventAnalyzed.this.mBaseFps);
                ApsCommon.logD(str, stringBuilder.toString());
            }
        }
    }

    public EventAnalyzed() {
        String pkgName;
        try {
            pkgName = ActivityThread.currentPackageName();
            this.mApsManager = HwFrameworkFactory.getApsManager();
            if (pkgName != null && this.mApsManager != null) {
                int texture = this.mApsManager.getTexture(pkgName);
                HwApsInterface.nativeSetGameTextureQuality(-1 == texture ? 100 : texture);
            }
        } catch (Exception e) {
            e.printStackTrace();
            pkgName = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("EventAnalyzed nativeSetGameTextureQuality error, exception ");
            stringBuilder.append(e);
            Log.e(pkgName, stringBuilder.toString());
        }
    }

    public void initAPS(Context context, int screenWidth, int myPid) {
        Log.i(TAG, "APS: version is 9.0.0.21");
        if (this.mContext == null) {
            this.mContext = context.getApplicationContext();
        }
        if (this.mHandler == null) {
            this.mHandler = new Handler();
        }
        if (this.mSdrController == null) {
            this.mSdrController = SdrController.getInstance();
        }
        this.mPkgName = context.getPackageName();
        this.mGameState = new GameState();
        this.mGameState.setGamePid(myPid);
        this.mBaseFpsRequest = new FpsRequest(SceneTypeE.EXACTLY_IDENTIFY);
        this.miAwareFpsRequest = new FpsRequest(SceneTypeE.EXACTLY_IDENTIFY);
        this.mIdleMaxFpsRequest = new FpsRequest(SceneTypeE.OPENGL_SETTING);
        this.mResumeFpsByTouch = new ResumeFpsByTouch(this, null);
        this.mStopTouchMaxFpsTime = SystemProperties.getInt("debug.aps.stoptouch.time", STOP_TOUCH_MAX_FPS_TIME);
    }

    public boolean isAPSReady() {
        return SystemProperties.getInt("debug.aps.enable", 0) == 9998;
    }

    public boolean isGameProcess(String pkgName) {
        if (this.mHasIdentifyProcess) {
            return this.mIsGameProcess;
        }
        int enable = SystemProperties.getInt("debug.aps.enable", 0);
        if (enable == 9997 || enable == 9998) {
            if (SystemProperties.get("debug.aps.process.name", "").equals(pkgName)) {
                this.mIsGameProcess = true;
            } else {
                this.mIsGameProcess = false;
            }
            this.mHasIdentifyProcess = true;
        }
        return this.mIsGameProcess;
    }

    public void setHasOnPaused(boolean hasOnPaused) {
        if (this.mTimer != null) {
            this.mTimer.cancel();
            this.mTimer = null;
            this.mFirstDynamicAdjustFps = true;
        }
        if (this.mHandler != null && this.mResumeFpsByTouch != null) {
            this.mFirstTimeResumeFps = true;
        }
    }

    private boolean isPerformanceMode(int mode) {
        return mode == 9997;
    }

    private boolean isPowerMode(int mode) {
        return mode == 9998;
    }

    private void processBaseFps() {
        if (this.mBaseFps >= 15 && this.mBaseFps < 60) {
            this.mBaseFpsRequest.start(this.mBaseFps);
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("APS: setFps: set the mBaseFps = ");
            stringBuilder.append(this.mBaseFps);
            stringBuilder.append("and the package = ");
            stringBuilder.append(this.mPkgName);
            Log.d(str, stringBuilder.toString());
        }
    }

    private void processIdleFps(int action) {
        if (this.mIdleFps > 0 && this.mIdleMaxFps > 0) {
            if (action == 0 || this.mFirstTimeResumeFps) {
                this.mFirstTimeResumeFps = false;
                this.mHandler.removeCallbacks(this.mResumeFpsByTouch);
                this.mIdleMaxFpsRequest.start(this.mIdleMaxFps);
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("APS: resume to the fps = ");
                stringBuilder.append(this.mIdleMaxFps);
                ApsCommon.logD(str, stringBuilder.toString());
            } else if (action == 1) {
                this.mHandler.postDelayed(this.mResumeFpsByTouch, (long) this.mStopTouchMaxFpsTime);
            }
        }
    }

    private void processDynamicFps(int action) {
        if (this.mFirstDynamicAdjustFps && action != 0) {
            if (this.mTimer == null) {
                this.mTimer = new Timer();
            }
            this.mTimer.schedule(new TimerTask() {
                public void run() {
                    EventAnalyzed.this.checkAndRegisterCallback();
                }
            }, 1000, 60000);
            this.mFirstDynamicAdjustFps = false;
        }
    }

    private void reInitPara() {
        this.mBaseFps = this.mApsManager.getFps(this.mPkgName);
        this.mMaxFps = this.mApsManager.getMaxFps(this.mPkgName);
        int i = 60;
        if (this.mMaxFps <= 60 || this.mMaxFps - 60 < this.mBaseFps) {
            if (this.mBaseFps != -1) {
                i = this.mBaseFps;
            }
            this.mBaseFps = i;
        } else if (isPerformanceMode(this.mMode)) {
            this.mBaseFps = 60;
            this.mIdleFps = 60;
            this.mIdleMaxFps = 60;
        } else {
            this.mIdleFps = this.mBaseFps;
            this.mIdleMaxFps = this.mMaxFps - 60;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("APS: reInitPara :mBaseFps = ");
        stringBuilder.append(this.mBaseFps);
        stringBuilder.append("; mMaxFps = ");
        stringBuilder.append(this.mMaxFps);
        stringBuilder.append("; mIdleFps = ");
        stringBuilder.append(this.mIdleFps);
        stringBuilder.append("; mIdleMaxFps = ");
        stringBuilder.append(this.mIdleMaxFps);
        Log.e(str, stringBuilder.toString());
    }

    public void processAnalyze(Context context, int action, long eventTime, int x, int y, int pointCount, long downTime) {
        this.mMode = SystemProperties.getInt("debug.aps.enable", 0);
        if (isPerformanceMode(this.mMode)) {
            if (this.mLastMode != this.mMode) {
                reInitPara();
                if (this.mBaseFpsRequest != null) {
                    this.mBaseFpsRequest.stop();
                }
            }
        } else if (isPowerMode(this.mMode) && this.mLastMode != this.mMode) {
            reInitPara();
            processBaseFps();
        }
        processIdleFps(action);
        processDynamicFps(action);
        this.mLastMode = this.mMode;
    }

    private void checkAndRegisterCallback() {
        String str;
        StringBuilder stringBuilder;
        long nowTimeStampMs = SystemClock.uptimeMillis();
        if (this.mLastRegisterTimeStampMs != -1 && nowTimeStampMs - this.mLastRegisterTimeStampMs >= 180000) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("checkAndRegisterCallback, time up, register again ! mPkgName:");
            stringBuilder.append(this.mPkgName);
            ApsCommon.logD(str, stringBuilder.toString());
            this.isRegistedApsManagerServiceCallback = false;
        }
        if (!this.isRegistedApsManagerServiceCallback) {
            if ((this.mPkgName == null || this.mPkgName.isEmpty()) && this.mContext != null) {
                this.mPkgName = this.mContext.getPackageName();
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("checkAndRegisterCallback, mPkgName == null, get mPkgName again. Then get mPkgName:");
                stringBuilder.append(this.mPkgName);
                ApsCommon.logD(str, stringBuilder.toString());
            }
            if (this.mPkgName == null || this.mPkgName.isEmpty()) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("checkAndRegisterCallback, mPkgName error, do register at next touch event. mPkgName = ");
                stringBuilder.append(this.mPkgName);
                Log.e(str, stringBuilder.toString());
                return;
            }
            str = SystemProperties.get("sys.aps.gameProcessName", "");
            String whiteListProcessName = SystemProperties.get("debug.aps.process.name", "");
            if (str.equals(this.mPkgName) || this.mIsGameProcess || whiteListProcessName.equals(this.mPkgName)) {
                if (this.mSdrController == null && SdrController.isSupportApsSdr()) {
                    this.mSdrController = SdrController.getInstance();
                    ApsCommon.logD(TAG, "SDR: checkAndRegisterCallback: SdrController init");
                }
                if (this.mApsManagerServiceCallback == null) {
                    this.mApsManagerServiceCallback = new ApsManagerServiceCallback();
                }
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("checkAndRegisterCallback, register Callback in ApsManagerService function start 1! mPkgName: ");
                stringBuilder2.append(this.mPkgName);
                ApsCommon.logD(str2, stringBuilder2.toString());
                registerCallbackInApsManagerService();
            }
        }
    }

    public void setResolutionRatio(int ratio) {
        float sdrRatio = -1.0f;
        boolean isSdrCase = this.mSdrController.IsSdrCase();
        if (ratio >= 1000) {
            sdrRatio = (((float) ratio) * 1.0f) / 100000.0f;
        }
        if (isSdrCase && sdrRatio > 0.0f && sdrRatio <= 1.0f) {
            this.mSdrController.setSdrRatio(sdrRatio);
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("APS: SDR: setResolutionRatio: isSdrCase = ");
        stringBuilder.append(isSdrCase);
        stringBuilder.append("; ratio = ");
        stringBuilder.append(sdrRatio);
        Log.d(str, stringBuilder.toString());
    }

    public void setFps(int fps) {
        if (fps >= 15 && fps < 60) {
            this.miAwareFpsRequest.start(fps);
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("APS: iAware setFps: set the fps = ");
            stringBuilder.append(fps);
            stringBuilder.append("and the package = ");
            stringBuilder.append(this.mPkgName);
            Log.d(str, stringBuilder.toString());
        } else if (fps >= 60) {
            this.miAwareFpsRequest.stop();
            Log.d(TAG, "APS: iAware resume the fps");
        } else if (fps == -1) {
            this.miAwareFpsRequest.stop();
            Log.d(TAG, "APS: app is background and iAware resume the fps");
        }
    }

    private void registerCallbackInApsManagerService() {
        try {
            if (!this.isRegistedApsManagerServiceCallback) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("EventAnalyzed, registerCallbackInApsManagerService, mPkgName:");
                stringBuilder.append(this.mPkgName);
                ApsCommon.logD(str, stringBuilder.toString());
                if (this.mApsManager == null) {
                    this.mApsManager = HwFrameworkFactory.getApsManager();
                }
                if (!(this.mApsManagerServiceCallback == null || this.mPkgName == null)) {
                    this.mApsManager.registerCallback(this.mPkgName, this.mApsManagerServiceCallback);
                    this.isRegistedApsManagerServiceCallback = true;
                    this.mLastRegisterTimeStampMs = SystemClock.uptimeMillis();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("registerCallbackInApsManagerService, exception:");
            stringBuilder2.append(e);
            Log.e(str2, stringBuilder2.toString());
        }
    }

    private void unregisterCallbackInApsManagerService() {
        try {
            if (this.isRegistedApsManagerServiceCallback) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("unregisterCallbackInApsManagerService, mPkgName:");
                stringBuilder.append(this.mPkgName);
                ApsCommon.logD(str, stringBuilder.toString());
                if (this.mApsManager == null) {
                    this.mApsManager = HwFrameworkFactory.getApsManager();
                }
                this.mApsManager.registerCallback(this.mPkgName, null);
                this.isRegistedApsManagerServiceCallback = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("unregisterCallbackInApsManagerService, exception:");
            stringBuilder2.append(e);
            Log.e(str2, stringBuilder2.toString());
        }
    }

    protected void finalize() {
        unregisterCallbackInApsManagerService();
    }

    public int getCustScreenDimDurationLocked(int screenOffTimeout) {
        int maxDimRatio = SystemProperties.getInt("sys.aps.maxDimRatio", -1);
        int minBrightDuration = SystemProperties.getInt("sys.aps.minBrightDuration", -1);
        if (maxDimRatio == -1 || minBrightDuration == -1 || screenOffTimeout <= minBrightDuration) {
            return -1;
        }
        return (screenOffTimeout * maxDimRatio) / 100;
    }

    public boolean StopSdrForSpecial(String Info, int keyCode) {
        return false;
    }

    public int setGameProcessName(String processName, int pid, int gameType) {
        return 0;
    }
}
