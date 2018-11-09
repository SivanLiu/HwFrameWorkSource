package com.android.server.display;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Slog;
import com.android.server.HwServiceFactory.IDisplayEffectMonitor;
import com.android.server.rms.iaware.memory.utils.MemoryConstant;
import com.huawei.displayengine.IDisplayEngineService;
import com.huawei.pgmng.plug.PGSdk;
import com.huawei.pgmng.plug.PGSdk.Sink;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class DisplayEffectMonitor implements IDisplayEffectMonitor {
    private static final String ACTION_MONITOR_TIMER = "com.android.server.display.action.MONITOR_TIMER";
    private static final long HOUR = 3600000;
    private static final boolean HWDEBUG;
    private static final boolean HWFLOW;
    private static final long MINUTE = 60000;
    private static final String TAG = "DisplayEffectMonitor";
    private static final String TYPE_BOOT_COMPLETED = "bootCompleted";
    private static volatile DisplayEffectMonitor mMonitor;
    private static final Object mMonitorLock = new Object();
    private AlarmManager mAlarmManager;
    private final BackLightMonitorManager mBackLightMonitorManager;
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                Slog.e(DisplayEffectMonitor.TAG, "onReceive() intent is NULL!");
                return;
            }
            if (DisplayEffectMonitor.ACTION_MONITOR_TIMER.equals(intent.getAction())) {
                if (DisplayEffectMonitor.HWFLOW) {
                    Slog.i(DisplayEffectMonitor.TAG, "hour task time up");
                }
                DisplayEffectMonitor.this.mBackLightMonitorManager.triggerUploadTimer();
            }
        }
    };
    private final Context mContext;
    private final HandlerThread mHandlerThread;
    private final SceneRecognition mSceneRecognition;

    public interface MonitorModule {
        public static final String PARAM_TYPE = "paramType";

        boolean isParamOwner(String str);

        void sendMonitorParam(ArrayMap<String, Object> arrayMap);

        void triggerUploadTimer();
    }

    public class ParamLogPrinter {
        private static final int LOG_PRINTER_MSG = 1;
        private static final int mMessageDelayInMs = 2000;
        private boolean mFirstTime = true;
        private long mLastTime;
        private int mLastValue;
        private final Object mLock = new Object();
        private String mPackageName;
        private boolean mParamDecrease;
        private boolean mParamIncrease;
        private final String mParamName;
        private long mStartTime;
        private int mStartValue;
        private final String mTAG;
        private final SimpleDateFormat mTimeFormater = new SimpleDateFormat("HH:mm:ss.SSS");
        private final Handler mTimeHandler = new Handler(DisplayEffectMonitor.this.mHandlerThread.getLooper()) {
            /* JADX WARNING: inconsistent code. */
            /* Code decompiled incorrectly, please refer to instructions dump. */
            public void handleMessage(Message msg) {
                synchronized (ParamLogPrinter.this.mLock) {
                    if (ParamLogPrinter.this.mFirstTime) {
                    } else if (ParamLogPrinter.this.mLastValue == msg.arg1) {
                        if (ParamLogPrinter.this.mParamIncrease || (ParamLogPrinter.this.mParamDecrease ^ 1) == 0) {
                            ParamLogPrinter.this.printValueChange(ParamLogPrinter.this.mStartValue, ParamLogPrinter.this.mStartTime, ParamLogPrinter.this.mLastValue, ParamLogPrinter.this.mLastTime, ParamLogPrinter.this.mPackageName);
                        } else {
                            ParamLogPrinter.this.printSingleValue(ParamLogPrinter.this.mLastValue, ParamLogPrinter.this.mLastTime, ParamLogPrinter.this.mPackageName);
                        }
                        removeMessages(1);
                        ParamLogPrinter.this.mFirstTime = true;
                    }
                }
            }
        };

        public ParamLogPrinter(String paramName, String tag) {
            this.mParamName = paramName;
            this.mTAG = tag;
        }

        public void updateParam(int param, String packageName) {
            boolean z = true;
            synchronized (this.mLock) {
                if (this.mFirstTime) {
                    this.mStartValue = param;
                    this.mLastValue = this.mStartValue;
                    this.mStartTime = System.currentTimeMillis();
                    this.mLastTime = this.mStartTime;
                    this.mPackageName = packageName;
                    this.mParamIncrease = false;
                    this.mParamDecrease = false;
                    this.mTimeHandler.removeMessages(1);
                    this.mTimeHandler.sendMessageDelayed(this.mTimeHandler.obtainMessage(1, param, 0), 2000);
                    this.mFirstTime = false;
                } else if (this.mLastValue == param) {
                } else {
                    boolean z2;
                    if (!this.mParamIncrease || param >= this.mLastValue) {
                        if (!this.mParamDecrease || param <= this.mLastValue) {
                            if (!(this.mParamIncrease || (this.mParamDecrease ^ 1) == 0)) {
                                if (param > this.mLastValue) {
                                    z2 = true;
                                } else {
                                    z2 = false;
                                }
                                this.mParamIncrease = z2;
                                if (param < this.mLastValue) {
                                    z2 = true;
                                } else {
                                    z2 = false;
                                }
                                this.mParamDecrease = z2;
                            }
                            this.mLastValue = param;
                            this.mLastTime = System.currentTimeMillis();
                            this.mTimeHandler.removeMessages(1);
                            this.mTimeHandler.sendMessageDelayed(this.mTimeHandler.obtainMessage(1, param, 0), 2000);
                            return;
                        }
                    }
                    printValueChange(this.mStartValue, this.mStartTime, this.mLastValue, this.mLastTime, this.mPackageName);
                    this.mStartValue = this.mLastValue;
                    this.mLastValue = param;
                    this.mStartTime = this.mLastTime;
                    this.mLastTime = System.currentTimeMillis();
                    if (this.mLastValue > this.mStartValue) {
                        z2 = true;
                    } else {
                        z2 = false;
                    }
                    this.mParamIncrease = z2;
                    if (this.mLastValue >= this.mStartValue) {
                        z = false;
                    }
                    this.mParamDecrease = z;
                    this.mTimeHandler.removeMessages(1);
                    this.mTimeHandler.sendMessageDelayed(this.mTimeHandler.obtainMessage(1, param, 0), 2000);
                }
            }
        }

        public void resetParam(int param, String packageName) {
            synchronized (this.mLock) {
                if (this.mFirstTime) {
                    if (this.mLastValue == param) {
                        return;
                    }
                } else if (this.mParamIncrease || (this.mParamDecrease ^ 1) == 0) {
                    printValueChange(this.mStartValue, this.mStartTime, this.mLastValue, this.mLastTime, this.mPackageName);
                } else {
                    printSingleValue(this.mLastValue, this.mLastTime, this.mPackageName);
                }
                printResetValue(param, packageName);
                this.mLastValue = param;
                this.mLastTime = System.currentTimeMillis();
                this.mTimeHandler.removeMessages(1);
                this.mFirstTime = true;
            }
        }

        public void changeName(int param, String packageName) {
            synchronized (this.mLock) {
                if (!this.mFirstTime) {
                    if (this.mParamIncrease || (this.mParamDecrease ^ 1) == 0) {
                        printValueChange(this.mStartValue, this.mStartTime, this.mLastValue, this.mLastTime, this.mPackageName);
                    } else {
                        printSingleValue(this.mLastValue, this.mLastTime, this.mPackageName);
                    }
                }
                printNameChange(this.mLastValue, this.mPackageName, param, packageName);
                this.mStartValue = param;
                this.mLastValue = this.mStartValue;
                this.mStartTime = System.currentTimeMillis();
                this.mLastTime = this.mStartTime;
                this.mPackageName = packageName;
                this.mParamIncrease = false;
                this.mParamDecrease = false;
                this.mTimeHandler.removeMessages(1);
                this.mTimeHandler.sendMessageDelayed(this.mTimeHandler.obtainMessage(1, param, 0), 2000);
                this.mFirstTime = false;
            }
        }

        private void printSingleValue(int value, long time, String packageName) {
            if (DisplayEffectMonitor.HWFLOW) {
                Slog.i(this.mTAG, this.mParamName + " " + value + " @" + this.mTimeFormater.format(Long.valueOf(time)) + (packageName != null ? " by " + packageName : ""));
            }
        }

        private void printValueChange(int startValue, long startTime, int endValue, long endTime, String packageName) {
            if (DisplayEffectMonitor.HWFLOW) {
                Slog.i(this.mTAG, this.mParamName + " " + startValue + " -> " + endValue + " @" + this.mTimeFormater.format(Long.valueOf(endTime)) + " " + (endTime - startTime) + "ms" + (packageName != null ? " by " + packageName : ""));
            }
        }

        private void printResetValue(int value, String packageName) {
            if (DisplayEffectMonitor.HWFLOW) {
                Slog.i(this.mTAG, this.mParamName + " reset " + value + (packageName != null ? " by " + packageName : ""));
            }
        }

        private void printNameChange(int value1, String packageName1, int value2, String packageName2) {
            if (DisplayEffectMonitor.HWFLOW) {
                Slog.i(this.mTAG, this.mParamName + " " + value1 + (packageName1 != null ? " by " + packageName1 : "") + " -> " + value2 + (packageName2 != null ? " by " + packageName2 : ""));
            }
        }
    }

    private class SceneRecognition {
        private Sink mListener;
        private PGSdk mPGSdk;

        private SceneRecognition() {
        }

        private void init() {
            this.mPGSdk = PGSdk.getInstance();
            if (this.mPGSdk == null) {
                Slog.e(DisplayEffectMonitor.TAG, "SceneRecognition init() PGSdk.getInstance() failed!");
                return;
            }
            this.mListener = new Sink() {
                public void onStateChanged(int stateType, int eventType, int pid, String pkg, int uid) {
                    SceneRecognition.this.sceneChanged(stateType, eventType);
                }
            };
            try {
                this.mPGSdk.enableStateEvent(this.mListener, IDisplayEngineService.DE_ACTION_PG_VIDEO_START);
                this.mPGSdk.enableStateEvent(this.mListener, IDisplayEngineService.DE_ACTION_PG_VIDEO_END);
                this.mPGSdk.enableStateEvent(this.mListener, IDisplayEngineService.DE_ACTION_PG_2DGAME_FRONT);
                this.mPGSdk.enableStateEvent(this.mListener, IDisplayEngineService.DE_ACTION_PG_3DGAME_FRONT);
            } catch (RemoteException e) {
                Slog.e(DisplayEffectMonitor.TAG, "SceneRecognition init() enableStateEvent failed! RemoteException:" + e);
            }
        }

        private void sceneChanged(int stateType, int eventType) {
            switch (stateType) {
                case IDisplayEngineService.DE_ACTION_PG_3DGAME_FRONT /*10002*/:
                case IDisplayEngineService.DE_ACTION_PG_2DGAME_FRONT /*10011*/:
                    if (eventType == 1) {
                        setScene("GAME");
                        return;
                    } else {
                        setScene("OTHERS");
                        return;
                    }
                case IDisplayEngineService.DE_ACTION_PG_VIDEO_START /*10015*/:
                    setScene("VIDEO");
                    return;
                case IDisplayEngineService.DE_ACTION_PG_VIDEO_END /*10016*/:
                    setScene("OTHERS");
                    return;
                default:
                    return;
            }
        }

        private void setScene(String sceneName) {
            ArrayMap<String, Object> params = new ArrayMap();
            params.put(MonitorModule.PARAM_TYPE, "sceneRecognition");
            params.put(MemoryConstant.MEM_POLICY_SCENE, sceneName);
            DisplayEffectMonitor.this.sendMonitorParam(params);
        }
    }

    static {
        boolean z = true;
        boolean isLoggable = !Log.HWLog ? Log.HWModuleLog ? Log.isLoggable(TAG, 3) : false : true;
        HWDEBUG = isLoggable;
        if (!Log.HWINFO) {
            z = Log.HWModuleLog ? Log.isLoggable(TAG, 4) : false;
        }
        HWFLOW = z;
    }

    public static DisplayEffectMonitor getInstance(Context context) {
        if (mMonitor == null) {
            synchronized (mMonitorLock) {
                if (mMonitor == null) {
                    if (context != null) {
                        try {
                            mMonitor = new DisplayEffectMonitor(context);
                        } catch (Exception e) {
                            Slog.e(TAG, "getInstance() failed! " + e);
                        }
                    } else {
                        Slog.w(TAG, "getInstance() failed! input context and instance is both null");
                    }
                }
            }
        }
        return mMonitor;
    }

    private DisplayEffectMonitor(Context context) {
        this.mContext = context;
        this.mHandlerThread = new HandlerThread(TAG);
        this.mHandlerThread.start();
        this.mSceneRecognition = new SceneRecognition();
        this.mBackLightMonitorManager = new BackLightMonitorManager(this);
        if (HWFLOW) {
            Slog.i(TAG, "new instance success");
        }
    }

    public void sendMonitorParam(ArrayMap<String, Object> params) {
        if (params == null || ((params.get(MonitorModule.PARAM_TYPE) instanceof String) ^ 1) != 0) {
            Slog.e(TAG, "sendMonitorParam() input params format error!");
            return;
        }
        String paramType = (String) params.get(MonitorModule.PARAM_TYPE);
        if (HWDEBUG) {
            Slog.d(TAG, "sendMonitorParam() paramType: " + paramType);
        }
        if (paramType != null) {
            if (paramType.equals(TYPE_BOOT_COMPLETED)) {
                bootCompletedInit();
            } else if (this.mBackLightMonitorManager.isParamOwner(paramType)) {
                this.mBackLightMonitorManager.sendMonitorParam(params);
            } else {
                Slog.e(TAG, "sendMonitorParam() undefine paramType: " + paramType);
            }
        }
    }

    public String getCurrentTopAppName() {
        try {
            List<RunningTaskInfo> runningTasks = ((ActivityManager) this.mContext.getSystemService("activity")).getRunningTasks(1);
            if (runningTasks == null || runningTasks.isEmpty()) {
                return null;
            }
            return ((RunningTaskInfo) runningTasks.get(0)).topActivity.getPackageName();
        } catch (SecurityException e) {
            Slog.e(TAG, "getCurrentTopAppName() getRunningTasks SecurityException :" + e);
            return null;
        }
    }

    public boolean isAppForeground(String packageName) {
        if (packageName == null) {
            return false;
        }
        List<RunningAppProcessInfo> runningProcesses = ((ActivityManager) this.mContext.getSystemService("activity")).getRunningAppProcesses();
        if (runningProcesses == null) {
            return false;
        }
        for (RunningAppProcessInfo processInfo : runningProcesses) {
            if (processInfo.importance == 100) {
                for (String activeProcess : processInfo.pkgList) {
                    if (packageName.equals(activeProcess)) {
                        Slog.d(TAG, "isAppForeground() find " + packageName);
                        return true;
                    }
                }
                continue;
            }
        }
        return false;
    }

    private void bootCompletedInit() {
        if (this.mBackLightMonitorManager.needHourTimer()) {
            try {
                initHourTimer();
            } catch (NullPointerException e) {
                Slog.e(TAG, "initHourTimer() NullPointerException " + e);
            }
        }
        if (this.mBackLightMonitorManager.needSceneRecognition()) {
            this.mSceneRecognition.init();
        }
        if (HWFLOW) {
            Slog.i(TAG, "bootCompletedInit() done");
        }
    }

    private void initHourTimer() {
        boolean debugMode = SystemProperties.getBoolean("persist.display.monitor.debug", false);
        Calendar scheduleTime = Calendar.getInstance();
        scheduleTime.setTime(new Date());
        if (debugMode) {
            scheduleTime.set(12, scheduleTime.get(12) + 1);
        } else {
            scheduleTime.set(10, scheduleTime.get(10) + 1);
            scheduleTime.set(12, 0);
        }
        scheduleTime.set(13, 0);
        scheduleTime.set(14, 0);
        long waiTime = scheduleTime.getTimeInMillis() - System.currentTimeMillis();
        if (waiTime <= 0) {
            waiTime = debugMode ? 60000 : 3600000;
        }
        long triggerTime = SystemClock.elapsedRealtime() + waiTime;
        long intervalTime = debugMode ? 60000 : 3600000;
        if (HWFLOW) {
            Slog.i(TAG, "initHourTimer() debugMode=" + debugMode + ", waiTime=" + waiTime + ", intervalTime=" + intervalTime);
        }
        this.mAlarmManager = (AlarmManager) this.mContext.getSystemService("alarm");
        if (this.mAlarmManager == null) {
            Slog.e(TAG, "initHourTimer() getSystemService(ALARM_SERVICE) return null");
            return;
        }
        PendingIntent sender = PendingIntent.getBroadcast(this.mContext, 0, new Intent(ACTION_MONITOR_TIMER), 0);
        this.mContext.registerReceiver(this.mBroadcastReceiver, new IntentFilter(ACTION_MONITOR_TIMER));
        this.mAlarmManager.setRepeating(2, triggerTime, intervalTime, sender);
        if (HWFLOW) {
            Slog.i(TAG, "initHourTimer() done");
        }
    }
}
