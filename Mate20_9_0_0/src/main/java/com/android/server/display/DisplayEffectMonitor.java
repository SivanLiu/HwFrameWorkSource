package com.android.server.display;

import android.app.ActivityManager;
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
import com.android.server.display.BackLightCommonData.Scene;
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
                DisplayEffectMonitor.this.mSceneRecognition.tryInit();
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
            /* JADX WARNING: Missing block: B:17:0x0070, code:
            return;
     */
            /* Code decompiled incorrectly, please refer to instructions dump. */
            public void handleMessage(Message msg) {
                synchronized (ParamLogPrinter.this.mLock) {
                    if (ParamLogPrinter.this.mFirstTime) {
                    } else if (ParamLogPrinter.this.mLastValue == msg.arg1) {
                        if (ParamLogPrinter.this.mParamIncrease || ParamLogPrinter.this.mParamDecrease) {
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
            Throwable th;
            int i = param;
            synchronized (this.mLock) {
                String str;
                try {
                    if (this.mFirstTime) {
                        this.mStartValue = i;
                        this.mLastValue = this.mStartValue;
                        this.mStartTime = System.currentTimeMillis();
                        this.mLastTime = this.mStartTime;
                        this.mPackageName = packageName;
                        this.mParamIncrease = false;
                        this.mParamDecrease = false;
                        this.mTimeHandler.removeMessages(1);
                        this.mTimeHandler.sendMessageDelayed(this.mTimeHandler.obtainMessage(1, i, 0), 2000);
                        this.mFirstTime = false;
                        return;
                    }
                    str = packageName;
                    if (this.mLastValue == i) {
                    } else if ((!this.mParamIncrease || i >= this.mLastValue) && (!this.mParamDecrease || i <= this.mLastValue)) {
                        if (!(this.mParamIncrease || this.mParamDecrease)) {
                            this.mParamIncrease = i > this.mLastValue;
                            this.mParamDecrease = i < this.mLastValue;
                        }
                        this.mLastValue = i;
                        this.mLastTime = System.currentTimeMillis();
                        this.mTimeHandler.removeMessages(1);
                        this.mTimeHandler.sendMessageDelayed(this.mTimeHandler.obtainMessage(1, i, 0), 2000);
                    } else {
                        printValueChange(this.mStartValue, this.mStartTime, this.mLastValue, this.mLastTime, this.mPackageName);
                        this.mStartValue = this.mLastValue;
                        this.mLastValue = i;
                        this.mStartTime = this.mLastTime;
                        this.mLastTime = System.currentTimeMillis();
                        this.mParamIncrease = this.mLastValue > this.mStartValue;
                        this.mParamDecrease = this.mLastValue < this.mStartValue;
                        this.mTimeHandler.removeMessages(1);
                        this.mTimeHandler.sendMessageDelayed(this.mTimeHandler.obtainMessage(1, i, 0), 2000);
                    }
                } catch (Throwable th2) {
                    th = th2;
                    throw th;
                }
            }
        }

        public void resetParam(int param, String packageName) {
            synchronized (this.mLock) {
                if (this.mFirstTime) {
                    if (this.mLastValue == param) {
                        return;
                    }
                } else if (this.mParamIncrease || this.mParamDecrease) {
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
                    if (this.mParamIncrease || this.mParamDecrease) {
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
                String stringBuilder;
                String str = this.mTAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append(this.mParamName);
                stringBuilder2.append(" ");
                stringBuilder2.append(value);
                stringBuilder2.append(" @");
                stringBuilder2.append(this.mTimeFormater.format(Long.valueOf(time)));
                if (packageName != null) {
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append(" by ");
                    stringBuilder3.append(packageName);
                    stringBuilder = stringBuilder3.toString();
                } else {
                    stringBuilder = "";
                }
                stringBuilder2.append(stringBuilder);
                Slog.i(str, stringBuilder2.toString());
            }
        }

        private void printValueChange(int startValue, long startTime, int endValue, long endTime, String packageName) {
            if (DisplayEffectMonitor.HWFLOW) {
                String stringBuilder;
                String str = this.mTAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append(this.mParamName);
                stringBuilder2.append(" ");
                stringBuilder2.append(startValue);
                stringBuilder2.append(" -> ");
                stringBuilder2.append(endValue);
                stringBuilder2.append(" @");
                stringBuilder2.append(this.mTimeFormater.format(Long.valueOf(endTime)));
                stringBuilder2.append(" ");
                stringBuilder2.append(endTime - startTime);
                stringBuilder2.append("ms");
                if (packageName != null) {
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append(" by ");
                    stringBuilder3.append(packageName);
                    stringBuilder = stringBuilder3.toString();
                } else {
                    stringBuilder = "";
                }
                stringBuilder2.append(stringBuilder);
                Slog.i(str, stringBuilder2.toString());
            }
        }

        private void printResetValue(int value, String packageName) {
            if (DisplayEffectMonitor.HWFLOW) {
                String stringBuilder;
                String str = this.mTAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append(this.mParamName);
                stringBuilder2.append(" reset ");
                stringBuilder2.append(value);
                if (packageName != null) {
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append(" by ");
                    stringBuilder3.append(packageName);
                    stringBuilder = stringBuilder3.toString();
                } else {
                    stringBuilder = "";
                }
                stringBuilder2.append(stringBuilder);
                Slog.i(str, stringBuilder2.toString());
            }
        }

        private void printNameChange(int value1, String packageName1, int value2, String packageName2) {
            if (DisplayEffectMonitor.HWFLOW) {
                StringBuilder stringBuilder;
                String stringBuilder2;
                String str = this.mTAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append(this.mParamName);
                stringBuilder3.append(" ");
                stringBuilder3.append(value1);
                if (packageName1 != null) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(" by ");
                    stringBuilder.append(packageName1);
                    stringBuilder2 = stringBuilder.toString();
                } else {
                    stringBuilder2 = "";
                }
                stringBuilder3.append(stringBuilder2);
                stringBuilder3.append(" -> ");
                stringBuilder3.append(value2);
                if (packageName2 != null) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(" by ");
                    stringBuilder.append(packageName2);
                    stringBuilder2 = stringBuilder.toString();
                } else {
                    stringBuilder2 = "";
                }
                stringBuilder3.append(stringBuilder2);
                Slog.i(str, stringBuilder3.toString());
            }
        }
    }

    private class SceneRecognition {
        private static final int MAX_TRY_INIT_TIMES = 5;
        private Sink mListener;
        private PGSdk mPGSdk;
        private Scene mScene;
        private boolean needTryInitAgain;
        private int tryInitTimes;

        private SceneRecognition() {
        }

        /* synthetic */ SceneRecognition(DisplayEffectMonitor x0, AnonymousClass1 x1) {
            this();
        }

        private void init() {
            this.mPGSdk = PGSdk.getInstance();
            if (this.mPGSdk == null) {
                Slog.e(DisplayEffectMonitor.TAG, "SceneRecognition init() PGSdk.getInstance() failed!");
                this.needTryInitAgain = true;
                return;
            }
            if (DisplayEffectMonitor.HWFLOW) {
                Slog.i(DisplayEffectMonitor.TAG, "SceneRecognition init() PGSdk.getInstance() success!");
            }
            this.needTryInitAgain = false;
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
                this.mPGSdk.enableStateEvent(this.mListener, IDisplayEngineService.DE_ACTION_PG_LAUNCHER_FRONT);
            } catch (RemoteException e) {
                String str = DisplayEffectMonitor.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("SceneRecognition init() enableStateEvent failed! RemoteException:");
                stringBuilder.append(e);
                Slog.e(str, stringBuilder.toString());
            }
        }

        private void tryInit() {
            if (!this.needTryInitAgain) {
                return;
            }
            String str;
            StringBuilder stringBuilder;
            if (this.tryInitTimes < 5) {
                this.tryInitTimes++;
                if (DisplayEffectMonitor.HWFLOW) {
                    str = DisplayEffectMonitor.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("SceneRecognition tryInit() for ");
                    stringBuilder.append(this.tryInitTimes);
                    stringBuilder.append(" times");
                    Slog.i(str, stringBuilder.toString());
                }
                init();
                return;
            }
            str = DisplayEffectMonitor.TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("SceneRecognition tryInit() had tried ");
            stringBuilder.append(this.tryInitTimes);
            stringBuilder.append(" times, give up!");
            Slog.e(str, stringBuilder.toString());
            this.needTryInitAgain = false;
        }

        private void sceneChanged(int stateType, int eventType) {
            switch (stateType) {
                case IDisplayEngineService.DE_ACTION_PG_3DGAME_FRONT /*10002*/:
                case IDisplayEngineService.DE_ACTION_PG_2DGAME_FRONT /*10011*/:
                    if (eventType == 1) {
                        setScene(Scene.GAME);
                        return;
                    } else {
                        setScene(Scene.OTHERS);
                        return;
                    }
                case IDisplayEngineService.DE_ACTION_PG_LAUNCHER_FRONT /*10010*/:
                case IDisplayEngineService.DE_ACTION_PG_VIDEO_END /*10016*/:
                    setScene(Scene.OTHERS);
                    return;
                case IDisplayEngineService.DE_ACTION_PG_VIDEO_START /*10015*/:
                    setScene(Scene.VIDEO);
                    return;
                default:
                    return;
            }
        }

        private void setScene(Scene scene) {
            if (this.mScene != scene) {
                this.mScene = scene;
                ArrayMap<String, Object> params = new ArrayMap();
                params.put(MonitorModule.PARAM_TYPE, "sceneRecognition");
                params.put(MemoryConstant.MEM_POLICY_SCENE, scene.toString());
                DisplayEffectMonitor.this.sendMonitorParam(params);
            }
        }
    }

    static {
        boolean z = true;
        boolean z2 = Log.HWLog || (Log.HWModuleLog && Log.isLoggable(TAG, 3));
        HWDEBUG = z2;
        if (!(Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4)))) {
            z = false;
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
                            String str = TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("getInstance() failed! ");
                            stringBuilder.append(e);
                            Slog.e(str, stringBuilder.toString());
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
        this.mSceneRecognition = new SceneRecognition(this, null);
        this.mBackLightMonitorManager = new BackLightMonitorManager(this);
        if (HWFLOW) {
            Slog.i(TAG, "new instance success");
        }
    }

    public void sendMonitorParam(ArrayMap<String, Object> params) {
        if (params == null || !(params.get(MonitorModule.PARAM_TYPE) instanceof String)) {
            Slog.e(TAG, "sendMonitorParam() input params format error!");
            return;
        }
        String str;
        StringBuilder stringBuilder;
        String paramType = (String) params.get(MonitorModule.PARAM_TYPE);
        if (HWDEBUG) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("sendMonitorParam() paramType: ");
            stringBuilder.append(paramType);
            Slog.d(str, stringBuilder.toString());
        }
        if (paramType != null) {
            if (paramType.equals(TYPE_BOOT_COMPLETED)) {
                bootCompletedInit();
            } else if (this.mBackLightMonitorManager.isParamOwner(paramType)) {
                this.mBackLightMonitorManager.sendMonitorParam(params);
            } else {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("sendMonitorParam() undefine paramType: ");
                stringBuilder.append(paramType);
                Slog.e(str, stringBuilder.toString());
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
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getCurrentTopAppName() getRunningTasks SecurityException :");
            stringBuilder.append(e);
            Slog.e(str, stringBuilder.toString());
            return null;
        }
    }

    private void bootCompletedInit() {
        if (this.mBackLightMonitorManager.needHourTimer()) {
            try {
                initHourTimer();
            } catch (NullPointerException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("initHourTimer() NullPointerException ");
                stringBuilder.append(e);
                Slog.e(str, stringBuilder.toString());
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
        long j = 3600000;
        if (waiTime <= 0) {
            waiTime = debugMode ? 60000 : 3600000;
        }
        long triggerTime = SystemClock.elapsedRealtime() + waiTime;
        if (debugMode) {
            j = 60000;
        }
        long intervalTime = j;
        if (HWFLOW) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("initHourTimer() debugMode=");
            stringBuilder.append(debugMode);
            stringBuilder.append(", waiTime=");
            stringBuilder.append(waiTime);
            stringBuilder.append(", intervalTime=");
            stringBuilder.append(intervalTime);
            Slog.i(str, stringBuilder.toString());
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
