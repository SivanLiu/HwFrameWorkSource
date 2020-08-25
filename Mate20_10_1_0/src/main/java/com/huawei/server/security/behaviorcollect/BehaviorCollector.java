package com.huawei.server.security.behaviorcollect;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.IProcessObserver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Flog;
import android.util.Log;
import android.view.MotionEvent;
import android.view.WindowManagerPolicyConstants;
import com.android.server.policy.WindowManagerPolicy;
import com.huawei.server.security.behaviorcollect.IBehaviorAuthService;
import com.huawei.server.security.behaviorcollect.bean.OnceData;
import com.huawei.server.security.behaviorcollect.bean.SensorData;
import com.huawei.server.security.behaviorcollect.bean.TouchPoint;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.json.JSONException;
import org.json.JSONObject;

public class BehaviorCollector {
    private static final String AUTH_SERVICE_CLASS = "com.huawei.behaviorauth.HwBehaviorAuthService";
    private static final String AUTH_SERVICE_PACKAGE = "com.huawei.behaviorauth";
    private static final int BIND_AUTH_SERVICE_TIMEOUT = 600;
    private static final String BOT_RESULT = "botResult";
    private static final String BUNDLE_KEY = "behavior_data";
    private static final int DB_REPORT_EVENT_ID = 1050;
    private static final int DEFAULT_DATA_SIZE = 10;
    public static final int EVENT_GAME_OFF = 2;
    public static final int EVENT_GAME_ON = 1;
    public static final int EVENT_SCREEN_ON = 3;
    private static final String FAIL_REASON = "failedReason";
    private static final long KILL_AUTH_SERVICE_TIMEOUT = 30000;
    private static final int LATCH_COUNT = 1;
    private static final int MAX_COUNT_DATA = 10;
    private static final int MAX_SENSOR_DATA_COUNT = 100;
    private static final int MAX_TRY_TIMES_TO_BINDSERVICE = 3;
    private static final String PKG_NAME = "pkgName";
    private static final String RUN_TIME = "runTime";
    private static final int SENSOR_DATA_SCALE = 4;
    /* access modifiers changed from: private */
    public static final String TAG = BehaviorCollector.class.getSimpleName();
    private static volatile boolean isActiveTouched = false;
    private static volatile BehaviorCollector single = null;
    private Sensor accelerometerSensor = null;
    private final AlarmManager.OnAlarmListener alarmListener = new AlarmManager.OnAlarmListener() {
        /* class com.huawei.server.security.behaviorcollect.BehaviorCollector.AnonymousClass1 */

        public void onAlarm() {
            Log.d(BehaviorCollector.TAG, "start force stop auth service");
            try {
                ActivityManager.getService().forceStopPackage(BehaviorCollector.AUTH_SERVICE_PACKAGE, -2);
            } catch (RemoteException e) {
                Log.e(BehaviorCollector.TAG, "forceStopPackage cause remote exception");
            }
        }
    };
    private AlarmManager alarmManager = null;
    /* access modifiers changed from: private */
    public volatile Map<String, CollectData> behaviorCache = new ConcurrentHashMap();
    /* access modifiers changed from: private */
    public Context behaviorContext = null;
    /* access modifiers changed from: private */
    public IBehaviorAuthService behaviorDetectClient = null;
    private BehaviorPointerEventListener behaviorPointerListener = null;
    private BehaviorSensorEventListener behaviorSensorEventListener = null;
    private WindowManagerPolicy.WindowManagerFuncs behaviorWindowManagerFuncs = null;
    /* access modifiers changed from: private */
    public volatile BindServiceStatus bindServiceStatus = BindServiceStatus.NOT_BIND;
    /* access modifiers changed from: private */
    public CountDownLatch countDownLatch = null;
    private BehaviorDetectServiceConnection detectServiceConnection = null;
    private Sensor gyroscopeSensor = null;
    private Handler handler = null;
    private HandlerThread handlerThread = null;
    private volatile boolean hasGameHalted = false;
    private volatile boolean hasRegistPointListener = false;
    private volatile boolean hasRegistSensorListener = false;
    private HwProcessObserver hwProcessObserver = null;
    /* access modifiers changed from: private */
    public ArrayList<SensorData> sensorDatas = new ArrayList<>(10);
    private SensorManager sensorManager = null;
    private ArrayList<TouchPoint> touchPoints = new ArrayList<>(10);

    /* access modifiers changed from: private */
    public enum BindServiceStatus {
        NOT_BIND,
        BINDING,
        CANNOT_BIND,
        BIND_FAILED_DIE,
        BIND_FAILED_DISCONNECTED,
        BIND_CONNECTED
    }

    /* access modifiers changed from: private */
    public class CollectData {
        /* access modifiers changed from: private */
        public int count;
        private List<OnceData> rawData;

        private CollectData() {
            this.count = 0;
            this.rawData = new ArrayList(10);
        }

        /* access modifiers changed from: private */
        public void addBehaviorData(OnceData onceData) {
            this.rawData.add(onceData);
            this.count++;
        }

        /* access modifiers changed from: private */
        public boolean hasCollectedFull() {
            return this.count >= 10;
        }

        /* access modifiers changed from: private */
        public void clearData() {
            this.rawData.clear();
            this.count = 0;
        }

        /* access modifiers changed from: private */
        public List<OnceData> getBehaviorData() {
            return this.rawData;
        }
    }

    public static BehaviorCollector getInstance() {
        if (single == null) {
            synchronized (BehaviorCollector.class) {
                if (single == null) {
                    single = new BehaviorCollector();
                }
            }
        }
        return single;
    }

    public static void enableActiveTouch() {
        isActiveTouched = true;
    }

    public void init(Context context, WindowManagerPolicy.WindowManagerFuncs windowManagerFuncs) {
        Log.d(TAG, "init behavior collect");
        this.behaviorContext = context;
        this.behaviorWindowManagerFuncs = windowManagerFuncs;
    }

    public synchronized int addPackage(String pkgName) {
        if (TextUtils.isEmpty(pkgName)) {
            Log.e(TAG, "addPackage:packagename is null.");
            return -1;
        }
        String str = TAG;
        Log.d(str, "call addPackage pkgName = " + pkgName);
        if (!hasInit()) {
            return -9;
        }
        if (this.behaviorCache.containsKey(pkgName)) {
            return -2;
        }
        cancelServiceStopTask();
        if (!bindAuthService()) {
            return -10;
        }
        if (this.behaviorCache.size() == 0) {
            readyResource();
        }
        this.behaviorCache.put(pkgName, new CollectData());
        registerTouchListener();
        return 0;
    }

    public synchronized int removePackage(String pkgName) {
        if (TextUtils.isEmpty(pkgName)) {
            Log.e(TAG, "removePackage:packagename is null.");
            return -1;
        }
        String str = TAG;
        Log.d(str, "call removePackage pkgName = " + pkgName);
        if (!hasInit()) {
            return -9;
        }
        if (!this.behaviorCache.containsKey(pkgName)) {
            return -3;
        }
        this.behaviorCache.remove(pkgName);
        if (this.behaviorCache.size() == 0) {
            releaseResource();
        }
        return 0;
    }

    public synchronized float getBotResultFromModel(String pkgName) {
        float result;
        long startTime = SystemClock.elapsedRealtime();
        if (TextUtils.isEmpty(pkgName)) {
            String str = TAG;
            Log.e(str, "getBotResultFromModel:packagename is null." + startTime);
            dataReport(null, 0, -1.0f);
            return -1.0f;
        }
        String str2 = TAG;
        Log.d(str2, "call getBotResultFromModel pkgName = " + pkgName);
        if (!hasInit()) {
            dataReport(pkgName, 0, -9.0f);
            return -9.0f;
        } else if (!this.behaviorCache.containsKey(pkgName)) {
            dataReport(pkgName, 0, -3.0f);
            return -3.0f;
        } else if (this.behaviorCache.get(pkgName) == null) {
            Log.e(TAG, "has no behavior data collect");
            dataReport(pkgName, 0, -6.0f);
            return -6.0f;
        } else if (this.behaviorCache.get(pkgName).count == 0) {
            dataReport(pkgName, 0, -6.0f);
            return -6.0f;
        } else if (this.behaviorDetectClient == null) {
            Log.e(TAG, "behaviorDetectClient is null, have not bind success");
            dataReport(pkgName, 0, -4.0f);
            return -4.0f;
        } else {
            try {
                result = this.behaviorDetectClient.getBotDetectResult(getBehaviorRawData(pkgName));
            } catch (RemoteException e) {
                String str3 = TAG;
                Log.e(str3, "behaviorDetectClient.getBotDetectResult cause remote exception. e" + e.getMessage());
                result = -5.0f;
            }
            this.behaviorCache.get(pkgName).clearData();
            registerTouchListener();
            dataReport(pkgName, SystemClock.elapsedRealtime() - startTime, result);
            return result;
        }
    }

    public void notifyEvent(int eventType) {
        if (eventType == 1) {
            Log.d(TAG, "receive game on event");
            boolean isRegisterBefore = this.hasRegistPointListener;
            unRegistPointListener();
            if (isRegisterBefore && !this.hasRegistPointListener) {
                this.hasGameHalted = true;
            }
        } else if (eventType != 2) {
            String str = TAG;
            Log.d(str, "unknown event " + eventType);
        } else {
            Log.d(TAG, "receive game off event");
            if (this.hasGameHalted) {
                this.hasGameHalted = false;
                if (this.behaviorCache.size() != 0) {
                    registerTouchListener();
                }
            }
        }
    }

    private boolean hasInit() {
        return (this.behaviorContext == null || this.behaviorWindowManagerFuncs == null) ? false : true;
    }

    private void scheduleServiceStopTask() {
        if (this.alarmManager == null) {
            this.alarmManager = (AlarmManager) this.behaviorContext.getSystemService(AlarmManager.class);
        }
        if (this.alarmManager == null) {
            Log.e(TAG, "alarmManager is null");
            return;
        }
        long origId = Binder.clearCallingIdentity();
        try {
            Log.d(TAG, "start scheduleStopService");
            this.alarmManager.cancel(this.alarmListener);
            this.alarmManager.setExact(2, 30000 + SystemClock.elapsedRealtime(), null, this.alarmListener, null);
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    private void cancelServiceStopTask() {
        if (this.alarmManager == null) {
            this.alarmManager = (AlarmManager) this.behaviorContext.getSystemService(AlarmManager.class);
        }
        if (this.alarmManager == null) {
            Log.e(TAG, "alarmManager is null");
            return;
        }
        long origId = Binder.clearCallingIdentity();
        try {
            Log.d(TAG, "start scheduleStopService");
            this.alarmManager.cancel(this.alarmListener);
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    private synchronized void registerTouchListener() {
        if (this.hasRegistPointListener) {
            Log.d(TAG, "has already regist the point listener");
        } else if (this.behaviorWindowManagerFuncs == null || this.behaviorPointerListener == null) {
            Log.e(TAG, "registerTouchListener failed: ManagerFuncs or Listener is empty");
        } else {
            this.behaviorWindowManagerFuncs.registerPointerEventListener(this.behaviorPointerListener, 0);
            this.hasRegistPointListener = true;
            Log.d(TAG, "regist the point listener successfully");
        }
    }

    private synchronized void unRegistPointListener() {
        if (!this.hasRegistPointListener) {
            Log.d(TAG, "has already release the point listener");
        } else if (this.behaviorWindowManagerFuncs == null || this.behaviorPointerListener == null) {
            Log.e(TAG, "unRegistPointListener failed: ManagerFuncs or Listener is empty");
        } else {
            this.behaviorWindowManagerFuncs.unregisterPointerEventListener(this.behaviorPointerListener, 0);
            this.hasRegistPointListener = false;
            Log.d(TAG, "release the point listener successfullly");
        }
    }

    private Bundle getBehaviorRawData(String pkgName) {
        List<OnceData> dataList = this.behaviorCache.get(pkgName).getBehaviorData();
        String str = TAG;
        Log.d(str, "dataList.size = " + dataList.size());
        Bundle bundle = new Bundle();
        if (dataList instanceof ArrayList) {
            bundle.putParcelableArrayList(BUNDLE_KEY, (ArrayList) dataList);
        }
        return bundle;
    }

    private void readyResource() {
        Log.d(TAG, "begin prepare the environment");
        Object sensorObj = this.behaviorContext.getSystemService("sensor");
        if (sensorObj instanceof SensorManager) {
            this.sensorManager = (SensorManager) sensorObj;
            this.accelerometerSensor = this.sensorManager.getDefaultSensor(1);
            this.gyroscopeSensor = this.sensorManager.getDefaultSensor(4);
        }
        this.handlerThread = new HandlerThread("Behavior Data Thread");
        this.handlerThread.start();
        if (this.handlerThread.getLooper() != null) {
            this.handler = new Handler(this.handlerThread.getLooper());
        }
        this.behaviorPointerListener = new BehaviorPointerEventListener();
        this.behaviorSensorEventListener = new BehaviorSensorEventListener();
        registerProcessObserver();
    }

    /* access modifiers changed from: private */
    public synchronized void releaseResource() {
        Log.d(TAG, "begin release the environment");
        unRegistSensorEventListener();
        unRegistPointListener();
        unbindAuthService();
        scheduleServiceStopTask();
        unRegisterProcessObserver();
        this.sensorManager = null;
        this.accelerometerSensor = null;
        this.gyroscopeSensor = null;
        this.behaviorPointerListener = null;
        this.behaviorSensorEventListener = null;
        if (this.handlerThread != null) {
            this.handlerThread.quitSafely();
            this.handlerThread = null;
        }
        this.handler = null;
    }

    private synchronized boolean bindAuthService() {
        bindAuthServiceOnce();
        if (this.bindServiceStatus == BindServiceStatus.BIND_FAILED_DIE) {
            bindAuthServiceOnce();
        }
        return this.bindServiceStatus == BindServiceStatus.BIND_CONNECTED;
    }

    private synchronized void bindAuthServiceOnce() {
        Log.d(TAG, "begin bindAuthService");
        if (this.bindServiceStatus == BindServiceStatus.BIND_CONNECTED) {
            Log.d(TAG, "has already connected the detect service");
            return;
        }
        this.bindServiceStatus = BindServiceStatus.BINDING;
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(AUTH_SERVICE_PACKAGE, AUTH_SERVICE_CLASS));
        this.detectServiceConnection = new BehaviorDetectServiceConnection();
        this.countDownLatch = new CountDownLatch(1);
        long origId = Binder.clearCallingIdentity();
        try {
            boolean canBindService = this.behaviorContext.bindService(intent, this.detectServiceConnection, 1);
            String str = TAG;
            Log.d(str, "start bind behaviorauth service canBindService = " + canBindService);
            if (!canBindService) {
                this.bindServiceStatus = BindServiceStatus.CANNOT_BIND;
                Log.e(TAG, "can not bind auth service");
                return;
            }
            try {
                this.countDownLatch.await(600, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Log.e(TAG, "await bind auth service failed, cause InterruptedException");
            }
            if (this.bindServiceStatus == BindServiceStatus.BINDING) {
                Log.e(TAG, "connect the bind auth service failed for other reason");
                this.behaviorContext.unbindService(this.detectServiceConnection);
                this.bindServiceStatus = BindServiceStatus.NOT_BIND;
                scheduleServiceStopTask();
            }
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    private void unbindAuthService() {
        BehaviorDetectServiceConnection behaviorDetectServiceConnection;
        if (this.bindServiceStatus == BindServiceStatus.BIND_CONNECTED || this.bindServiceStatus == BindServiceStatus.BINDING) {
            Context context = this.behaviorContext;
            if (!(context == null || (behaviorDetectServiceConnection = this.detectServiceConnection) == null)) {
                context.unbindService(behaviorDetectServiceConnection);
            }
            this.bindServiceStatus = BindServiceStatus.NOT_BIND;
            return;
        }
        Log.d(TAG, "unbind behaviorauth service success");
    }

    class BehaviorDetectServiceConnection implements ServiceConnection {
        BehaviorDetectServiceConnection() {
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(BehaviorCollector.TAG, "behaviorAuthService onServiceConnected.");
            IBehaviorAuthService unused = BehaviorCollector.this.behaviorDetectClient = IBehaviorAuthService.Stub.asInterface(service);
            BindServiceStatus unused2 = BehaviorCollector.this.bindServiceStatus = BindServiceStatus.BIND_CONNECTED;
            BehaviorCollector.this.countDownLatch.countDown();
        }

        public void onServiceDisconnected(ComponentName name) {
            Log.d(BehaviorCollector.TAG, "behaviorAuthService onServiceDisconnected.");
            IBehaviorAuthService unused = BehaviorCollector.this.behaviorDetectClient = null;
            BindServiceStatus unused2 = BehaviorCollector.this.bindServiceStatus = BindServiceStatus.BIND_FAILED_DISCONNECTED;
            BehaviorCollector.this.countDownLatch.countDown();
        }

        public void onBindingDied(ComponentName name) {
            Log.d(BehaviorCollector.TAG, "behaviorAuthService onBindingDied.");
            IBehaviorAuthService unused = BehaviorCollector.this.behaviorDetectClient = null;
            BindServiceStatus unused2 = BehaviorCollector.this.bindServiceStatus = BindServiceStatus.BIND_FAILED_DIE;
            BehaviorCollector.this.countDownLatch.countDown();
        }
    }

    /* access modifiers changed from: private */
    public void cacheBehaviorData() {
        OnceData onceData = new OnceData(isActiveTouched, this.sensorDatas, this.touchPoints);
        synchronized (this) {
            for (String pkgName : this.behaviorCache.keySet()) {
                if (this.behaviorCache.get(pkgName) != null) {
                    if (!this.behaviorCache.get(pkgName).hasCollectedFull()) {
                        this.behaviorCache.get(pkgName).addBehaviorData(onceData);
                    }
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public synchronized void unRegistListenerIfAllFull() {
        boolean isAllFullCollected = true;
        Iterator<String> it = this.behaviorCache.keySet().iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            if (!this.behaviorCache.get(it.next()).hasCollectedFull()) {
                isAllFullCollected = false;
                break;
            }
        }
        if (isAllFullCollected) {
            Log.d(TAG, "beacuse cache data has collected, so unregist listeners");
            unRegistPointListener();
            unRegistSensorEventListener();
        }
    }

    /* access modifiers changed from: private */
    public void clearSensorData() {
        String str = TAG;
        Log.d(str, "start clearSensorData isActiveTouched = " + isActiveTouched);
        isActiveTouched = false;
        this.sensorDatas.clear();
        this.touchPoints.clear();
    }

    /* access modifiers changed from: private */
    public void handleUpEvent(final MotionEvent event) {
        Handler handler2 = this.handler;
        if (handler2 != null) {
            handler2.post(new Runnable() {
                /* class com.huawei.server.security.behaviorcollect.BehaviorCollector.AnonymousClass2 */

                public void run() {
                    BehaviorCollector.this.handleMotionEvent(event);
                    BehaviorCollector.this.unRegistSensorEventListener();
                    BehaviorCollector.this.cacheBehaviorData();
                    BehaviorCollector.this.clearSensorData();
                    BehaviorCollector.this.unRegistListenerIfAllFull();
                }
            });
        }
    }

    /* access modifiers changed from: private */
    public void handleDownEvent(final MotionEvent event) {
        Handler handler2 = this.handler;
        if (handler2 != null) {
            handler2.post(new Runnable() {
                /* class com.huawei.server.security.behaviorcollect.BehaviorCollector.AnonymousClass3 */

                public void run() {
                    BehaviorCollector.this.clearSensorData();
                    BehaviorCollector.this.handleMotionEvent(event);
                    BehaviorCollector.this.registSensorEventListener();
                }
            });
        }
    }

    /* access modifiers changed from: private */
    public void handleMoveEvent(final MotionEvent event) {
        Handler handler2 = this.handler;
        if (handler2 != null) {
            handler2.post(new Runnable() {
                /* class com.huawei.server.security.behaviorcollect.BehaviorCollector.AnonymousClass4 */

                public void run() {
                    BehaviorCollector.this.handleMotionEvent(event);
                }
            });
        }
    }

    /* access modifiers changed from: private */
    public void handleMotionEvent(MotionEvent ev) {
        try {
            TouchPoint tp = new TouchPoint((double) ev.getEventTime(), (double) ev.getX(), (double) ev.getY(), (double) ev.getPressure(), (double) ev.getSize());
            tp.setPointerId(ev.getPointerId(ev.getActionIndex()));
            tp.setOri((double) ev.getOrientation());
            tp.setAction(ev.getAction());
            this.touchPoints.add(tp);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "occur IllegalArgumentException when handle the motion event");
        }
    }

    /* access modifiers changed from: private */
    public synchronized void registSensorEventListener() {
        if (!this.hasRegistSensorListener) {
            if (this.sensorManager != null) {
                if (this.behaviorSensorEventListener != null) {
                    this.sensorManager.registerListener(this.behaviorSensorEventListener, this.accelerometerSensor, 0);
                    this.sensorManager.registerListener(this.behaviorSensorEventListener, this.gyroscopeSensor, 0);
                    this.hasRegistSensorListener = true;
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public synchronized void unRegistSensorEventListener() {
        if (this.hasRegistSensorListener) {
            if (this.sensorManager != null) {
                if (this.behaviorSensorEventListener != null) {
                    this.sensorManager.unregisterListener(this.behaviorSensorEventListener);
                    this.hasRegistSensorListener = false;
                }
            }
        }
    }

    private void registerProcessObserver() {
        if (this.hwProcessObserver == null) {
            this.hwProcessObserver = new HwProcessObserver();
            long origId = Binder.clearCallingIdentity();
            try {
                ActivityManager.getService().registerProcessObserver(this.hwProcessObserver);
                Log.d(TAG, "register process observer success");
            } catch (RemoteException e) {
                Log.e(TAG, "register process observer failed");
                this.hwProcessObserver = null;
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(origId);
                throw th;
            }
            Binder.restoreCallingIdentity(origId);
        }
    }

    private void unRegisterProcessObserver() {
        Log.d(TAG, "start unregister process observer");
        if (this.hwProcessObserver != null) {
            long origId = Binder.clearCallingIdentity();
            try {
                ActivityManager.getService().unregisterProcessObserver(this.hwProcessObserver);
                this.hwProcessObserver = null;
                Log.d(TAG, "unregister process observer success");
            } catch (RemoteException e) {
                Log.e(TAG, "unregister process observer failed");
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(origId);
                throw th;
            }
            Binder.restoreCallingIdentity(origId);
        }
    }

    private void dataReport(String pkgName, long runTime, float botResult) {
        int failedReason;
        JSONObject bdBehaviorAuth = new JSONObject();
        if (botResult < 0.0f) {
            failedReason = (int) botResult;
        } else {
            failedReason = 0;
        }
        try {
            bdBehaviorAuth.put("pkgName", pkgName);
            bdBehaviorAuth.put(RUN_TIME, runTime + "");
            bdBehaviorAuth.put(BOT_RESULT, botResult + "");
            bdBehaviorAuth.put(FAIL_REASON, failedReason + "");
            Flog.bdReport(this.behaviorContext, (int) DB_REPORT_EVENT_ID, bdBehaviorAuth);
            String str = TAG;
            Log.i(str, "report data : " + bdBehaviorAuth.toString());
        } catch (JSONException e) {
            Log.e(TAG, "reportBigData Error");
        }
    }

    private class HwProcessObserver extends IProcessObserver.Stub {
        private HwProcessObserver() {
        }

        public void onForegroundActivitiesChanged(int pid, int uid, boolean isForegroundActivities) {
        }

        public void onProcessDied(int pid, int uid) {
            PackageManager packageManager;
            String[] pkgNames;
            if (BehaviorCollector.this.behaviorCache.size() != 0 && BehaviorCollector.this.behaviorContext != null && (packageManager = BehaviorCollector.this.behaviorContext.getPackageManager()) != null && (pkgNames = packageManager.getPackagesForUid(uid)) != null && pkgNames.length > 0) {
                String pkgName = pkgNames[0];
                if (BehaviorCollector.this.behaviorCache.containsKey(pkgName)) {
                    synchronized (BehaviorCollector.this) {
                        BehaviorCollector.this.behaviorCache.remove(pkgName);
                        String access$000 = BehaviorCollector.TAG;
                        Log.d(access$000, "remove dead package " + pkgName);
                        if (BehaviorCollector.this.behaviorCache.size() == 0) {
                            BehaviorCollector.this.releaseResource();
                        }
                    }
                }
            }
        }

        public void onForegroundServicesChanged(int pid, int uid, int serviceTypes) {
        }
    }

    class BehaviorPointerEventListener implements WindowManagerPolicyConstants.PointerEventListener {
        BehaviorPointerEventListener() {
        }

        public void onPointerEvent(MotionEvent event) {
            int action = event.getAction();
            if (action == 0) {
                BehaviorCollector.this.handleDownEvent(event);
            } else if (action == 1) {
                BehaviorCollector.this.handleUpEvent(event);
            } else if (action != 2) {
                String access$000 = BehaviorCollector.TAG;
                Log.d(access$000, "unknown motion event " + event);
            } else {
                BehaviorCollector.this.handleMoveEvent(event);
            }
        }
    }

    class BehaviorSensorEventListener implements SensorEventListener {
        BehaviorSensorEventListener() {
        }

        public void onSensorChanged(SensorEvent sensorEvent) {
            if (BehaviorCollector.this.sensorDatas.size() < 100) {
                BehaviorCollector.this.sensorDatas.add(new SensorData(sensorEvent.timestamp, sensorEvent.sensor.getType(), BigDecimal.valueOf((double) sensorEvent.values[0]).setScale(4, 4).doubleValue(), BigDecimal.valueOf((double) sensorEvent.values[1]).setScale(4, 4).doubleValue(), BigDecimal.valueOf((double) sensorEvent.values[2]).setScale(4, 4).doubleValue()));
            }
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    }
}
