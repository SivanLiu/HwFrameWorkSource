package android.app;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.hardware.input.InputManager;
import android.os.Bundle;
import android.os.Debug;
import android.os.IBinder;
import android.os.Looper;
import android.os.MessageQueue;
import android.os.MessageQueue.IdleHandler;
import android.os.Parcelable;
import android.os.PerformanceCollector;
import android.os.PersistableBundle;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.TestLooperManager;
import android.os.Trace;
import android.os.UserHandle;
import android.util.AndroidRuntimeException;
import android.util.Jlog;
import android.util.Log;
import android.view.IWindowManager.Stub;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import com.android.internal.content.ReferrerIntent;
import java.io.File;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

public class Instrumentation {
    public static final String REPORT_KEY_IDENTIFIER = "id";
    public static final String REPORT_KEY_STREAMRESULT = "stream";
    private static final String TAG = "Instrumentation";
    private List<ActivityMonitor> mActivityMonitors;
    private Context mAppContext;
    private boolean mAutomaticPerformanceSnapshots = false;
    private ComponentName mComponent;
    private Context mInstrContext;
    private MessageQueue mMessageQueue = null;
    private Bundle mPerfMetrics = new Bundle();
    private PerformanceCollector mPerformanceCollector;
    private Thread mRunner;
    private final Object mSync = new Object();
    private ActivityThread mThread = null;
    private UiAutomation mUiAutomation;
    private IUiAutomationConnection mUiAutomationConnection;
    private List<ActivityWaiter> mWaitingActivities;
    private IInstrumentationWatcher mWatcher;

    /* renamed from: android.app.Instrumentation$1ContextMenuRunnable */
    class AnonymousClass1ContextMenuRunnable implements Runnable {
        private final Activity activity;
        private final int flags;
        private final int identifier;
        boolean returnValue;

        public AnonymousClass1ContextMenuRunnable(Activity _activity, int _identifier, int _flags) {
            this.activity = _activity;
            this.identifier = _identifier;
            this.flags = _flags;
        }

        public void run() {
            this.returnValue = this.activity.getWindow().performContextMenuIdentifierAction(this.identifier, this.flags);
        }
    }

    /* renamed from: android.app.Instrumentation$1MenuRunnable */
    class AnonymousClass1MenuRunnable implements Runnable {
        private final Activity activity;
        private final int flags;
        private final int identifier;
        boolean returnValue;

        public AnonymousClass1MenuRunnable(Activity _activity, int _identifier, int _flags) {
            this.activity = _activity;
            this.identifier = _identifier;
            this.flags = _flags;
        }

        public void run() {
            this.returnValue = this.activity.getWindow().performPanelIdentifierAction(0, this.identifier, this.flags);
        }
    }

    public static class ActivityMonitor {
        private final boolean mBlock;
        private final String mClass;
        int mHits;
        private final boolean mIgnoreMatchingSpecificIntents;
        Activity mLastActivity;
        private final ActivityResult mResult;
        private final IntentFilter mWhich;

        public ActivityMonitor(IntentFilter which, ActivityResult result, boolean block) {
            this.mHits = 0;
            this.mLastActivity = null;
            this.mWhich = which;
            this.mClass = null;
            this.mResult = result;
            this.mBlock = block;
            this.mIgnoreMatchingSpecificIntents = false;
        }

        public ActivityMonitor(String cls, ActivityResult result, boolean block) {
            this.mHits = 0;
            this.mLastActivity = null;
            this.mWhich = null;
            this.mClass = cls;
            this.mResult = result;
            this.mBlock = block;
            this.mIgnoreMatchingSpecificIntents = false;
        }

        public ActivityMonitor() {
            this.mHits = 0;
            this.mLastActivity = null;
            this.mWhich = null;
            this.mClass = null;
            this.mResult = null;
            this.mBlock = false;
            this.mIgnoreMatchingSpecificIntents = true;
        }

        final boolean ignoreMatchingSpecificIntents() {
            return this.mIgnoreMatchingSpecificIntents;
        }

        public final IntentFilter getFilter() {
            return this.mWhich;
        }

        public final ActivityResult getResult() {
            return this.mResult;
        }

        public final boolean isBlocking() {
            return this.mBlock;
        }

        public final int getHits() {
            return this.mHits;
        }

        public final Activity getLastActivity() {
            return this.mLastActivity;
        }

        public final Activity waitForActivity() {
            Activity res;
            synchronized (this) {
                while (this.mLastActivity == null) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                    }
                }
                res = this.mLastActivity;
                this.mLastActivity = null;
            }
            return res;
        }

        public final Activity waitForActivityWithTimeout(long timeOut) {
            synchronized (this) {
                if (this.mLastActivity == null) {
                    try {
                        wait(timeOut);
                    } catch (InterruptedException e) {
                    }
                }
                if (this.mLastActivity == null) {
                    return null;
                }
                Activity res = this.mLastActivity;
                this.mLastActivity = null;
                return res;
            }
        }

        public ActivityResult onStartActivity(Intent intent) {
            return null;
        }

        /* JADX WARNING: Missing block: B:23:0x0047, code skipped:
            return false;
     */
        /* JADX WARNING: Missing block: B:27:0x0050, code skipped:
            return true;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        final boolean match(Context who, Activity activity, Intent intent) {
            if (this.mIgnoreMatchingSpecificIntents) {
                return false;
            }
            synchronized (this) {
                if (this.mWhich == null || this.mWhich.match(who.getContentResolver(), intent, true, Instrumentation.TAG) >= 0) {
                    if (this.mClass != null) {
                        String cls = null;
                        if (activity != null) {
                            cls = activity.getClass().getName();
                        } else if (intent.getComponent() != null) {
                            cls = intent.getComponent().getClassName();
                        }
                        if (cls == null || !this.mClass.equals(cls)) {
                        }
                    }
                    if (activity != null) {
                        this.mLastActivity = activity;
                        notifyAll();
                    }
                } else {
                    return false;
                }
            }
        }
    }

    public static final class ActivityResult {
        private final int mResultCode;
        private final Intent mResultData;

        public ActivityResult(int resultCode, Intent resultData) {
            this.mResultCode = resultCode;
            this.mResultData = resultData;
        }

        public int getResultCode() {
            return this.mResultCode;
        }

        public Intent getResultData() {
            return this.mResultData;
        }
    }

    private static final class ActivityWaiter {
        public Activity activity;
        public final Intent intent;

        public ActivityWaiter(Intent _intent) {
            this.intent = _intent;
        }
    }

    private static final class EmptyRunnable implements Runnable {
        private EmptyRunnable() {
        }

        public void run() {
        }
    }

    private final class InstrumentationThread extends Thread {
        public InstrumentationThread(String name) {
            super(name);
        }

        public void run() {
            try {
                Process.setThreadPriority(-8);
            } catch (RuntimeException e) {
                String str = Instrumentation.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Exception setting priority of instrumentation thread ");
                stringBuilder.append(Process.myTid());
                Log.w(str, stringBuilder.toString(), e);
            }
            if (Instrumentation.this.mAutomaticPerformanceSnapshots) {
                Instrumentation.this.startPerformanceSnapshot();
            }
            Instrumentation.this.onStart();
        }
    }

    private static final class SyncRunnable implements Runnable {
        private boolean mComplete;
        private final Runnable mTarget;

        public SyncRunnable(Runnable target) {
            this.mTarget = target;
        }

        public void run() {
            this.mTarget.run();
            synchronized (this) {
                this.mComplete = true;
                notifyAll();
            }
        }

        public void waitForComplete() {
            synchronized (this) {
                while (!this.mComplete) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                    }
                }
            }
        }
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface UiAutomationFlags {
    }

    private final class ActivityGoing implements IdleHandler {
        private final ActivityWaiter mWaiter;

        public ActivityGoing(ActivityWaiter waiter) {
            this.mWaiter = waiter;
        }

        public final boolean queueIdle() {
            synchronized (Instrumentation.this.mSync) {
                Instrumentation.this.mWaitingActivities.remove(this.mWaiter);
                Instrumentation.this.mSync.notifyAll();
            }
            return false;
        }
    }

    private static final class Idler implements IdleHandler {
        private final Runnable mCallback;
        private boolean mIdle = false;

        public Idler(Runnable callback) {
            this.mCallback = callback;
        }

        public final boolean queueIdle() {
            if (this.mCallback != null) {
                this.mCallback.run();
            }
            synchronized (this) {
                this.mIdle = true;
                notifyAll();
            }
            return false;
        }

        public void waitForIdle() {
            synchronized (this) {
                while (!this.mIdle) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                    }
                }
            }
        }
    }

    public void setActivityThread(Object thread) {
        this.mThread = (ActivityThread) thread;
    }

    private void checkInstrumenting(String method) {
        if (this.mInstrContext == null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(method);
            stringBuilder.append(" cannot be called outside of instrumented processes");
            throw new RuntimeException(stringBuilder.toString());
        }
    }

    public void onCreate(Bundle arguments) {
    }

    public void start() {
        if (this.mRunner == null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Instr: ");
            stringBuilder.append(getClass().getName());
            this.mRunner = new InstrumentationThread(stringBuilder.toString());
            this.mRunner.start();
            return;
        }
        throw new RuntimeException("Instrumentation already started");
    }

    public void onStart() {
    }

    public boolean onException(Object obj, Throwable e) {
        return false;
    }

    public void sendStatus(int resultCode, Bundle results) {
        if (this.mWatcher != null) {
            try {
                this.mWatcher.instrumentationStatus(this.mComponent, resultCode, results);
            } catch (RemoteException e) {
                this.mWatcher = null;
            }
        }
    }

    public void addResults(Bundle results) {
        try {
            ActivityManager.getService().addInstrumentationResults(this.mThread.getApplicationThread(), results);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public void finish(int resultCode, Bundle results) {
        if (this.mAutomaticPerformanceSnapshots) {
            endPerformanceSnapshot();
        }
        if (this.mPerfMetrics != null) {
            if (results == null) {
                results = new Bundle();
            }
            results.putAll(this.mPerfMetrics);
        }
        if (!(this.mUiAutomation == null || this.mUiAutomation.isDestroyed())) {
            this.mUiAutomation.disconnect();
            this.mUiAutomation = null;
        }
        this.mThread.finishInstrumentation(resultCode, results);
    }

    public void setAutomaticPerformanceSnapshots() {
        this.mAutomaticPerformanceSnapshots = true;
        this.mPerformanceCollector = new PerformanceCollector();
    }

    public void startPerformanceSnapshot() {
        if (!isProfiling()) {
            this.mPerformanceCollector.beginSnapshot(null);
        }
    }

    public void endPerformanceSnapshot() {
        if (!isProfiling()) {
            this.mPerfMetrics = this.mPerformanceCollector.endSnapshot();
        }
    }

    public void onDestroy() {
    }

    public Context getContext() {
        return this.mInstrContext;
    }

    public ComponentName getComponentName() {
        return this.mComponent;
    }

    public Context getTargetContext() {
        return this.mAppContext;
    }

    public String getProcessName() {
        return this.mThread.getProcessName();
    }

    public boolean isProfiling() {
        return this.mThread.isProfiling();
    }

    public void startProfiling() {
        if (this.mThread.isProfiling()) {
            File file = new File(this.mThread.getProfileFilePath());
            file.getParentFile().mkdirs();
            Debug.startMethodTracing(file.toString(), 8388608);
        }
    }

    public void stopProfiling() {
        if (this.mThread.isProfiling()) {
            Debug.stopMethodTracing();
        }
    }

    public void setInTouchMode(boolean inTouch) {
        try {
            Stub.asInterface(ServiceManager.getService(Context.WINDOW_SERVICE)).setInTouchMode(inTouch);
        } catch (RemoteException e) {
        }
    }

    public void waitForIdle(Runnable recipient) {
        this.mMessageQueue.addIdleHandler(new Idler(recipient));
        this.mThread.getHandler().post(new EmptyRunnable());
    }

    public void waitForIdleSync() {
        validateNotAppThread();
        Idler idler = new Idler(null);
        this.mMessageQueue.addIdleHandler(idler);
        this.mThread.getHandler().post(new EmptyRunnable());
        idler.waitForIdle();
    }

    public void runOnMainSync(Runnable runner) {
        validateNotAppThread();
        SyncRunnable sr = new SyncRunnable(runner);
        this.mThread.getHandler().post(sr);
        sr.waitForComplete();
    }

    public Activity startActivitySync(Intent intent) {
        return startActivitySync(intent, null);
    }

    public Activity startActivitySync(Intent intent, Bundle options) {
        Activity activity;
        validateNotAppThread();
        synchronized (this.mSync) {
            intent = new Intent(intent);
            ActivityInfo ai = intent.resolveActivityInfo(getTargetContext().getPackageManager(), 0);
            if (ai != null) {
                String myProc = this.mThread.getProcessName();
                if (ai.processName.equals(myProc)) {
                    intent.setComponent(new ComponentName(ai.applicationInfo.packageName, ai.name));
                    ActivityWaiter aw = new ActivityWaiter(intent);
                    if (this.mWaitingActivities == null) {
                        this.mWaitingActivities = new ArrayList();
                    }
                    this.mWaitingActivities.add(aw);
                    getTargetContext().startActivity(intent, options);
                    do {
                        try {
                            this.mSync.wait();
                        } catch (InterruptedException e) {
                        }
                    } while (this.mWaitingActivities.contains(aw));
                    activity = aw.activity;
                } else {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Intent in process ");
                    stringBuilder.append(myProc);
                    stringBuilder.append(" resolved to different process ");
                    stringBuilder.append(ai.processName);
                    stringBuilder.append(": ");
                    stringBuilder.append(intent);
                    throw new RuntimeException(stringBuilder.toString());
                }
            }
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Unable to resolve activity for: ");
            stringBuilder2.append(intent);
            throw new RuntimeException(stringBuilder2.toString());
        }
        return activity;
    }

    public void addMonitor(ActivityMonitor monitor) {
        synchronized (this.mSync) {
            if (this.mActivityMonitors == null) {
                this.mActivityMonitors = new ArrayList();
            }
            this.mActivityMonitors.add(monitor);
        }
    }

    public ActivityMonitor addMonitor(IntentFilter filter, ActivityResult result, boolean block) {
        ActivityMonitor am = new ActivityMonitor(filter, result, block);
        addMonitor(am);
        return am;
    }

    public ActivityMonitor addMonitor(String cls, ActivityResult result, boolean block) {
        ActivityMonitor am = new ActivityMonitor(cls, result, block);
        addMonitor(am);
        return am;
    }

    public boolean checkMonitorHit(ActivityMonitor monitor, int minHits) {
        waitForIdleSync();
        synchronized (this.mSync) {
            if (monitor.getHits() < minHits) {
                return false;
            }
            this.mActivityMonitors.remove(monitor);
            return true;
        }
    }

    public Activity waitForMonitor(ActivityMonitor monitor) {
        Activity activity = monitor.waitForActivity();
        synchronized (this.mSync) {
            this.mActivityMonitors.remove(monitor);
        }
        return activity;
    }

    public Activity waitForMonitorWithTimeout(ActivityMonitor monitor, long timeOut) {
        Activity activity = monitor.waitForActivityWithTimeout(timeOut);
        synchronized (this.mSync) {
            this.mActivityMonitors.remove(monitor);
        }
        return activity;
    }

    public void removeMonitor(ActivityMonitor monitor) {
        synchronized (this.mSync) {
            this.mActivityMonitors.remove(monitor);
        }
    }

    public boolean invokeMenuActionSync(Activity targetActivity, int id, int flag) {
        AnonymousClass1MenuRunnable mr = new AnonymousClass1MenuRunnable(targetActivity, id, flag);
        runOnMainSync(mr);
        return mr.returnValue;
    }

    public boolean invokeContextMenuAction(Activity targetActivity, int id, int flag) {
        validateNotAppThread();
        sendKeySync(new KeyEvent(0, 23));
        waitForIdleSync();
        try {
            Thread.sleep((long) ViewConfiguration.getLongPressTimeout());
            sendKeySync(new KeyEvent(1, 23));
            waitForIdleSync();
            AnonymousClass1ContextMenuRunnable cmr = new AnonymousClass1ContextMenuRunnable(targetActivity, id, flag);
            runOnMainSync(cmr);
            return cmr.returnValue;
        } catch (InterruptedException e) {
            Log.e(TAG, "Could not sleep for long press timeout", e);
            return false;
        }
    }

    public void sendStringSync(String text) {
        if (text != null) {
            KeyEvent[] events = KeyCharacterMap.load(-1).getEvents(text.toCharArray());
            if (events != null) {
                for (KeyEvent changeTimeRepeat : events) {
                    sendKeySync(KeyEvent.changeTimeRepeat(changeTimeRepeat, SystemClock.uptimeMillis(), 0));
                }
            }
        }
    }

    public void sendKeySync(KeyEvent event) {
        validateNotAppThread();
        long downTime = event.getDownTime();
        long eventTime = event.getEventTime();
        int action = event.getAction();
        int code = event.getKeyCode();
        int repeatCount = event.getRepeatCount();
        int metaState = event.getMetaState();
        int deviceId = event.getDeviceId();
        int scancode = event.getScanCode();
        int source = event.getSource();
        int flags = event.getFlags();
        if (source == 0) {
            source = 257;
        }
        int source2 = source;
        if (eventTime == 0) {
            eventTime = SystemClock.uptimeMillis();
        }
        if (downTime == 0) {
            downTime = eventTime;
        }
        InputManager.getInstance().injectInputEvent(new KeyEvent(downTime, eventTime, action, code, repeatCount, metaState, deviceId, scancode, flags | 8, source2), 2);
    }

    public void sendKeyDownUpSync(int key) {
        sendKeySync(new KeyEvent(0, key));
        sendKeySync(new KeyEvent(1, key));
    }

    public void sendCharacterSync(int keyCode) {
        sendKeySync(new KeyEvent(0, keyCode));
        sendKeySync(new KeyEvent(1, keyCode));
    }

    public void sendPointerSync(MotionEvent event) {
        validateNotAppThread();
        if ((event.getSource() & 2) == 0) {
            event.setSource(4098);
        }
        InputManager.getInstance().injectInputEvent(event, 2);
    }

    public void sendTrackballEventSync(MotionEvent event) {
        validateNotAppThread();
        if ((event.getSource() & 4) == 0) {
            event.setSource(65540);
        }
        InputManager.getInstance().injectInputEvent(event, 2);
    }

    public Application newApplication(ClassLoader cl, String className, Context context) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        Application app = getFactory(context.getPackageName()).instantiateApplication(cl, className);
        app.attach(context);
        return app;
    }

    public static Application newApplication(Class<?> clazz, Context context) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        Application app = (Application) clazz.newInstance();
        app.attach(context);
        return app;
    }

    public void callApplicationOnCreate(Application app) {
        Trace.traceBegin(64, "app.onCreate");
        app.onCreate();
        Trace.traceEnd(64);
    }

    public Activity newActivity(Class<?> clazz, Context context, IBinder token, Application application, Intent intent, ActivityInfo info, CharSequence title, Activity parent, String id, Object lastNonConfigurationInstance) throws InstantiationException, IllegalAccessException {
        Application application2;
        Activity activity = (Activity) clazz.newInstance();
        if (application == null) {
            application2 = new Application();
        } else {
            application2 = application;
        }
        activity.attach(context, null, this, token, 0, application2, intent, info, title, parent, id, (NonConfigurationInstances) lastNonConfigurationInstance, new Configuration(), null, null, null, null);
        return activity;
    }

    public Activity newActivity(ClassLoader cl, String className, Intent intent) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        String pkg = (intent == null || intent.getComponent() == null) ? null : intent.getComponent().getPackageName();
        return getFactory(pkg).instantiateActivity(cl, className, intent);
    }

    private AppComponentFactory getFactory(String pkg) {
        if (pkg == null) {
            Log.e(TAG, "No pkg specified, disabling AppComponentFactory");
            return AppComponentFactory.DEFAULT;
        } else if (this.mThread == null) {
            Log.e(TAG, "Uninitialized ActivityThread, likely app-created Instrumentation, disabling AppComponentFactory", new Throwable());
            return AppComponentFactory.DEFAULT;
        } else {
            LoadedApk apk = this.mThread.peekPackageInfo(pkg, true);
            if (apk == null) {
                apk = this.mThread.getSystemContext().mPackageInfo;
            }
            return apk.getAppFactory();
        }
    }

    private void prePerformCreate(Activity activity) {
        if (this.mWaitingActivities != null) {
            synchronized (this.mSync) {
                int N = this.mWaitingActivities.size();
                for (int i = 0; i < N; i++) {
                    ActivityWaiter aw = (ActivityWaiter) this.mWaitingActivities.get(i);
                    if (aw.intent.filterEquals(activity.getIntent())) {
                        aw.activity = activity;
                        this.mMessageQueue.addIdleHandler(new ActivityGoing(aw));
                    }
                }
            }
        }
    }

    private void postPerformCreate(Activity activity) {
        if (this.mActivityMonitors != null) {
            synchronized (this.mSync) {
                int N = this.mActivityMonitors.size();
                for (int i = 0; i < N; i++) {
                    ((ActivityMonitor) this.mActivityMonitors.get(i)).match(activity, activity, activity.getIntent());
                }
            }
        }
    }

    public void callActivityOnCreate(Activity activity, Bundle icicle) {
        prePerformCreate(activity);
        if (Jlog.isPerfTest()) {
            Jlog.i(3041, Jlog.getMessage(TAG, "callActivityOnCreate", Integer.valueOf(Process.myPid())));
        }
        activity.performCreate(icicle);
        if (Jlog.isPerfTest()) {
            Jlog.i(3042, Jlog.getMessage(TAG, "callActivityOnCreate", Integer.valueOf(Process.myPid())));
        }
        postPerformCreate(activity);
    }

    public void callActivityOnCreate(Activity activity, Bundle icicle, PersistableBundle persistentState) {
        prePerformCreate(activity);
        activity.performCreate(icicle, persistentState);
        postPerformCreate(activity);
    }

    public void callActivityOnDestroy(Activity activity) {
        activity.performDestroy();
    }

    public void callActivityOnRestoreInstanceState(Activity activity, Bundle savedInstanceState) {
        activity.performRestoreInstanceState(savedInstanceState);
    }

    public void callActivityOnRestoreInstanceState(Activity activity, Bundle savedInstanceState, PersistableBundle persistentState) {
        activity.performRestoreInstanceState(savedInstanceState, persistentState);
    }

    public void callActivityOnPostCreate(Activity activity, Bundle icicle) {
        activity.onPostCreate(icicle);
    }

    public void callActivityOnPostCreate(Activity activity, Bundle icicle, PersistableBundle persistentState) {
        activity.onPostCreate(icicle, persistentState);
    }

    public void callActivityOnNewIntent(Activity activity, Intent intent) {
        activity.performNewIntent(intent);
    }

    public void callActivityOnNewIntent(Activity activity, ReferrerIntent intent) {
        String oldReferrer = activity.mReferrer;
        if (intent != null) {
            try {
                activity.mReferrer = intent.mReferrer;
            } catch (Throwable th) {
                activity.mReferrer = oldReferrer;
            }
        }
        callActivityOnNewIntent(activity, intent != null ? new Intent((Intent) intent) : null);
        activity.mReferrer = oldReferrer;
    }

    public void callActivityOnStart(Activity activity) {
        Trace.traceBegin(64, "onStart");
        if (Jlog.isPerfTest()) {
            Jlog.i(3045, Jlog.getMessage(TAG, "callActivityOnStart", Integer.valueOf(Process.myPid())));
        }
        activity.onStart();
        if (Jlog.isPerfTest()) {
            Jlog.i(3046, Jlog.getMessage(TAG, "callActivityOnStart", Integer.valueOf(Process.myPid())));
        }
        Trace.traceEnd(64);
    }

    public void callActivityOnRestart(Activity activity) {
        Trace.traceBegin(64, "onRestart");
        activity.onRestart();
        Trace.traceEnd(64);
    }

    public void callActivityOnResume(Activity activity) {
        activity.mResumed = true;
        Trace.traceBegin(64, "onResume");
        if (Jlog.isPerfTest()) {
            Jlog.i(3047, Jlog.getMessage(TAG, "callActivityOnResume", Integer.valueOf(Process.myPid())));
        }
        activity.onResume();
        if (Jlog.isPerfTest()) {
            Jlog.i(3048, Jlog.getMessage(TAG, "callActivityOnResume", Integer.valueOf(Process.myPid())));
        }
        Trace.traceEnd(64);
        if (this.mActivityMonitors != null) {
            synchronized (this.mSync) {
                int N = this.mActivityMonitors.size();
                for (int i = 0; i < N; i++) {
                    ((ActivityMonitor) this.mActivityMonitors.get(i)).match(activity, activity, activity.getIntent());
                }
            }
        }
    }

    public void callActivityOnStop(Activity activity) {
        Trace.traceBegin(64, "onStop");
        if (Jlog.isPerfTest()) {
            Jlog.i(3053, Jlog.getMessage(TAG, "callActivityOnStop", Integer.valueOf(Process.myPid())));
        }
        activity.onStop();
        if (Jlog.isPerfTest()) {
            Jlog.i(3054, Jlog.getMessage(TAG, "callActivityOnStop", Integer.valueOf(Process.myPid())));
        }
        Trace.traceEnd(64);
    }

    public void callActivityOnSaveInstanceState(Activity activity, Bundle outState) {
        activity.performSaveInstanceState(outState);
    }

    public void callActivityOnSaveInstanceState(Activity activity, Bundle outState, PersistableBundle outPersistentState) {
        activity.performSaveInstanceState(outState, outPersistentState);
    }

    public void callActivityOnPause(Activity activity) {
        if (Jlog.isPerfTest()) {
            Jlog.i(3026, Jlog.getMessage(TAG, "callActivityOnPause", Integer.valueOf(Process.myPid())));
        }
        activity.performPause();
        if (Jlog.isPerfTest()) {
            Jlog.i(3027, Jlog.getMessage(TAG, "callActivityOnPause", Integer.valueOf(Process.myPid())));
        }
    }

    public void callActivityOnUserLeaving(Activity activity) {
        activity.performUserLeaving();
    }

    @Deprecated
    public void startAllocCounting() {
        Runtime.getRuntime().gc();
        Runtime.getRuntime().runFinalization();
        Runtime.getRuntime().gc();
        Debug.resetAllCounts();
        Debug.startAllocCounting();
    }

    @Deprecated
    public void stopAllocCounting() {
        Runtime.getRuntime().gc();
        Runtime.getRuntime().runFinalization();
        Runtime.getRuntime().gc();
        Debug.stopAllocCounting();
    }

    private void addValue(String key, int value, Bundle results) {
        if (results.containsKey(key)) {
            List<Integer> list = results.getIntegerArrayList(key);
            if (list != null) {
                list.add(Integer.valueOf(value));
                return;
            }
            return;
        }
        ArrayList<Integer> list2 = new ArrayList();
        list2.add(Integer.valueOf(value));
        results.putIntegerArrayList(key, list2);
    }

    public Bundle getAllocCounts() {
        Bundle results = new Bundle();
        results.putLong("global_alloc_count", (long) Debug.getGlobalAllocCount());
        results.putLong("global_alloc_size", (long) Debug.getGlobalAllocSize());
        results.putLong("global_freed_count", (long) Debug.getGlobalFreedCount());
        results.putLong("global_freed_size", (long) Debug.getGlobalFreedSize());
        results.putLong("gc_invocation_count", (long) Debug.getGlobalGcInvocationCount());
        return results;
    }

    public Bundle getBinderCounts() {
        Bundle results = new Bundle();
        results.putLong("sent_transactions", (long) Debug.getBinderSentTransactions());
        results.putLong("received_transactions", (long) Debug.getBinderReceivedTransactions());
        return results;
    }

    /* JADX WARNING: Missing block: B:28:0x0065, code skipped:
            return r0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public ActivityResult execStartActivity(Context who, IBinder contextThread, IBinder token, Activity target, Intent intent, int requestCode, Bundle options) {
        RemoteException e;
        Parcelable parcelable;
        Context context = who;
        Activity activity = target;
        Intent intent2 = intent;
        IApplicationThread whoThread = (IApplicationThread) contextThread;
        ActivityResult activityResult = null;
        Parcelable referrer = activity != null ? target.onProvideReferrer() : null;
        if (referrer != null) {
            intent2.putExtra(Intent.EXTRA_REFERRER, referrer);
        }
        if (this.mActivityMonitors != null) {
            synchronized (this.mSync) {
                int N = this.mActivityMonitors.size();
                int i = 0;
                while (i < N) {
                    ActivityMonitor am = (ActivityMonitor) this.mActivityMonitors.get(i);
                    ActivityResult result = null;
                    if (am.ignoreMatchingSpecificIntents()) {
                        result = am.onStartActivity(intent2);
                    }
                    if (result != null) {
                        am.mHits++;
                        return result;
                    } else if (am.match(context, null, intent2)) {
                        am.mHits++;
                        if (am.isBlocking()) {
                            if (requestCode >= 0) {
                                activityResult = am.getResult();
                            }
                        }
                    } else {
                        i++;
                    }
                }
            }
        }
        try {
            intent.migrateExtraStreamToClipData();
            intent2.prepareToLeaveProcess(context);
            if (Jlog.isPerfTest()) {
                try {
                    String str = TAG;
                    String str2 = "execStartActivity";
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("whopkg=");
                    stringBuilder.append(context != null ? who.getPackageName() : "unknow");
                    stringBuilder.append("&");
                    stringBuilder.append(Intent.toPkgClsString(intent));
                    Jlog.i(3022, Jlog.getMessage(str, str2, stringBuilder.toString()));
                } catch (RemoteException e2) {
                    e = e2;
                    parcelable = referrer;
                    throw new RuntimeException("Failure from system", e);
                }
            }
            try {
                checkStartActivityResult(ActivityManager.getService().startActivity(whoThread, who.getBasePackageName(), intent2, intent2.resolveTypeIfNeeded(who.getContentResolver()), token, activity != null ? activity.mEmbeddedID : null, requestCode, 0, null, options), intent2);
                return null;
            } catch (RemoteException e3) {
                e = e3;
            }
        } catch (RemoteException e4) {
            e = e4;
            parcelable = referrer;
            throw new RuntimeException("Failure from system", e);
        }
    }

    public void execStartActivities(Context who, IBinder contextThread, IBinder token, Activity target, Intent[] intents, Bundle options) {
        execStartActivitiesAsUser(who, contextThread, token, target, intents, options, who.getUserId());
    }

    public int execStartActivitiesAsUser(Context who, IBinder contextThread, IBinder token, Activity target, Intent[] intents, Bundle options, int userId) {
        Context context = who;
        Intent[] intentArr = intents;
        IApplicationThread whoThread = (IApplicationThread) contextThread;
        if (this.mActivityMonitors != null) {
            synchronized (this.mSync) {
                int N = this.mActivityMonitors.size();
                int i = 0;
                while (i < N) {
                    ActivityMonitor am = (ActivityMonitor) this.mActivityMonitors.get(i);
                    ActivityResult result = null;
                    if (am.ignoreMatchingSpecificIntents()) {
                        result = am.onStartActivity(intentArr[0]);
                    }
                    if (result != null) {
                        am.mHits++;
                        return -96;
                    } else if (am.match(context, null, intentArr[0])) {
                        am.mHits++;
                        if (am.isBlocking()) {
                            return -96;
                        }
                    } else {
                        i++;
                    }
                }
            }
        }
        try {
            int i2;
            String[] resolvedTypes = new String[intentArr.length];
            for (i2 = 0; i2 < intentArr.length; i2++) {
                intentArr[i2].migrateExtraStreamToClipData();
                intentArr[i2].prepareToLeaveProcess(context);
                resolvedTypes[i2] = intentArr[i2].resolveTypeIfNeeded(context.getContentResolver());
            }
            i2 = ActivityManager.getService().startActivities(whoThread, context.getBasePackageName(), intentArr, resolvedTypes, token, options, userId);
            checkStartActivityResult(i2, intentArr[0]);
            return i2;
        } catch (RemoteException e) {
            throw new RuntimeException("Failure from system", e);
        }
    }

    /* JADX WARNING: Missing block: B:21:0x0053, code skipped:
            return r13;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public ActivityResult execStartActivity(Context who, IBinder contextThread, IBinder token, String target, Intent intent, int requestCode, Bundle options) {
        Context context = who;
        Intent intent2 = intent;
        IApplicationThread whoThread = (IApplicationThread) contextThread;
        ActivityResult activityResult = null;
        if (this.mActivityMonitors != null) {
            synchronized (this.mSync) {
                int N = this.mActivityMonitors.size();
                int i = 0;
                while (i < N) {
                    ActivityMonitor am = (ActivityMonitor) this.mActivityMonitors.get(i);
                    ActivityResult result = null;
                    if (am.ignoreMatchingSpecificIntents()) {
                        result = am.onStartActivity(intent2);
                    }
                    if (result != null) {
                        am.mHits++;
                        return result;
                    } else if (am.match(context, null, intent2)) {
                        am.mHits++;
                        if (am.isBlocking()) {
                            if (requestCode >= 0) {
                                activityResult = am.getResult();
                            }
                        }
                    } else {
                        i++;
                    }
                }
            }
        }
        try {
            intent.migrateExtraStreamToClipData();
            intent2.prepareToLeaveProcess(context);
            if (Jlog.isPerfTest() && Jlog.isPerfTest()) {
                String str = TAG;
                String str2 = "execStartActivity";
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("whopkg=");
                stringBuilder.append(context != null ? who.getPackageName() : "unknow");
                stringBuilder.append("&");
                stringBuilder.append(Intent.toPkgClsString(intent));
                Jlog.i(3022, Jlog.getMessage(str, str2, stringBuilder.toString()));
            }
            ActivityResult activityResult2 = null;
            checkStartActivityResult(ActivityManager.getService().startActivity(whoThread, who.getBasePackageName(), intent2, intent2.resolveTypeIfNeeded(who.getContentResolver()), token, target, requestCode, 0, null, options), intent2);
            return activityResult2;
        } catch (RemoteException e) {
            throw new RuntimeException("Failure from system", e);
        }
    }

    /* JADX WARNING: Missing block: B:21:0x0053, code skipped:
            return r14;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public ActivityResult execStartActivity(Context who, IBinder contextThread, IBinder token, String resultWho, Intent intent, int requestCode, Bundle options, UserHandle user) {
        Context context = who;
        Intent intent2 = intent;
        IApplicationThread whoThread = (IApplicationThread) contextThread;
        ActivityResult activityResult = null;
        if (this.mActivityMonitors != null) {
            synchronized (this.mSync) {
                int N = this.mActivityMonitors.size();
                int i = 0;
                while (i < N) {
                    ActivityMonitor am = (ActivityMonitor) this.mActivityMonitors.get(i);
                    ActivityResult result = null;
                    if (am.ignoreMatchingSpecificIntents()) {
                        result = am.onStartActivity(intent2);
                    }
                    if (result != null) {
                        am.mHits++;
                        return result;
                    } else if (am.match(context, null, intent2)) {
                        am.mHits++;
                        if (am.isBlocking()) {
                            if (requestCode >= 0) {
                                activityResult = am.getResult();
                            }
                        }
                    } else {
                        i++;
                    }
                }
            }
        }
        try {
            intent.migrateExtraStreamToClipData();
            intent2.prepareToLeaveProcess(context);
            if (Jlog.isPerfTest()) {
                String str = TAG;
                String str2 = "execStartActivity";
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("whopkg=");
                stringBuilder.append(context != null ? who.getPackageName() : "unknow");
                stringBuilder.append("&");
                stringBuilder.append(Intent.toPkgClsString(intent));
                Jlog.i(3022, Jlog.getMessage(str, str2, stringBuilder.toString()));
            }
            ActivityResult activityResult2 = null;
            checkStartActivityResult(ActivityManager.getService().startActivityAsUser(whoThread, who.getBasePackageName(), intent2, intent2.resolveTypeIfNeeded(who.getContentResolver()), token, resultWho, requestCode, 0, null, options, user.getIdentifier()), intent2);
            return activityResult2;
        } catch (RemoteException e) {
            throw new RuntimeException("Failure from system", e);
        }
    }

    /* JADX WARNING: Missing block: B:21:0x0055, code skipped:
            return r14;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public ActivityResult execStartActivityAsCaller(Context who, IBinder contextThread, IBinder token, Activity target, Intent intent, int requestCode, Bundle options, boolean ignoreTargetSecurity, int userId) {
        RemoteException e;
        Intent intent2;
        Context context = who;
        Activity activity = target;
        Intent intent3 = intent;
        IApplicationThread whoThread = (IApplicationThread) contextThread;
        ActivityResult activityResult = null;
        if (this.mActivityMonitors != null) {
            synchronized (this.mSync) {
                int N = this.mActivityMonitors.size();
                int i = 0;
                while (i < N) {
                    ActivityMonitor am = (ActivityMonitor) this.mActivityMonitors.get(i);
                    ActivityResult result = null;
                    if (am.ignoreMatchingSpecificIntents()) {
                        result = am.onStartActivity(intent3);
                    }
                    if (result != null) {
                        am.mHits++;
                        return result;
                    } else if (am.match(context, null, intent3)) {
                        am.mHits++;
                        if (am.isBlocking()) {
                            if (requestCode >= 0) {
                                activityResult = am.getResult();
                            }
                        }
                    } else {
                        i++;
                    }
                }
            }
        }
        try {
            intent.migrateExtraStreamToClipData();
            intent3.prepareToLeaveProcess(context);
            if (Jlog.isPerfTest()) {
                String str = TAG;
                String str2 = "execStartActivity";
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("whopkg=");
                stringBuilder.append(context != null ? who.getPackageName() : "unknow");
                stringBuilder.append("&");
                stringBuilder.append(Intent.toPkgClsString(intent));
                Jlog.i(3022, Jlog.getMessage(str, str2, stringBuilder.toString()));
            }
            ActivityResult activityResult2 = null;
            try {
                try {
                    checkStartActivityResult(ActivityManager.getService().startActivityAsCaller(whoThread, who.getBasePackageName(), intent3, intent3.resolveTypeIfNeeded(who.getContentResolver()), token, activity != null ? activity.mEmbeddedID : null, requestCode, 0, null, options, ignoreTargetSecurity, userId), intent);
                    return activityResult2;
                } catch (RemoteException e2) {
                    e = e2;
                }
            } catch (RemoteException e3) {
                e = e3;
                intent2 = intent;
                throw new RuntimeException("Failure from system", e);
            }
        } catch (RemoteException e4) {
            e = e4;
            intent2 = intent3;
            throw new RuntimeException("Failure from system", e);
        }
    }

    public void execStartActivityFromAppTask(Context who, IBinder contextThread, IAppTask appTask, Intent intent, Bundle options) {
        IApplicationThread whoThread = (IApplicationThread) contextThread;
        if (this.mActivityMonitors != null) {
            synchronized (this.mSync) {
                int N = this.mActivityMonitors.size();
                int i = 0;
                while (i < N) {
                    ActivityMonitor am = (ActivityMonitor) this.mActivityMonitors.get(i);
                    ActivityResult result = null;
                    if (am.ignoreMatchingSpecificIntents()) {
                        result = am.onStartActivity(intent);
                    }
                    if (result != null) {
                        am.mHits++;
                        return;
                    } else if (am.match(who, null, intent)) {
                        am.mHits++;
                        if (am.isBlocking()) {
                            return;
                        }
                    } else {
                        i++;
                    }
                }
            }
        }
        try {
            intent.migrateExtraStreamToClipData();
            intent.prepareToLeaveProcess(who);
            checkStartActivityResult(appTask.startActivity(whoThread.asBinder(), who.getBasePackageName(), intent, intent.resolveTypeIfNeeded(who.getContentResolver()), options), intent);
        } catch (RemoteException e) {
            throw new RuntimeException("Failure from system", e);
        }
    }

    final void init(ActivityThread thread, Context instrContext, Context appContext, ComponentName component, IInstrumentationWatcher watcher, IUiAutomationConnection uiAutomationConnection) {
        this.mThread = thread;
        this.mThread.getLooper();
        this.mMessageQueue = Looper.myQueue();
        this.mInstrContext = instrContext;
        this.mAppContext = appContext;
        this.mComponent = component;
        this.mWatcher = watcher;
        this.mUiAutomationConnection = uiAutomationConnection;
    }

    final void basicInit(ActivityThread thread) {
        this.mThread = thread;
    }

    public static void checkStartActivityResult(int res, Object intent) {
        if (ActivityManager.isStartResultFatalError(res)) {
            StringBuilder stringBuilder;
            switch (res) {
                case -100:
                    throw new IllegalStateException("Cannot start voice activity on a hidden session");
                case ActivityManager.START_VOICE_NOT_ACTIVE_SESSION /*-99*/:
                    throw new IllegalStateException("Session calling startVoiceActivity does not match active session");
                case ActivityManager.START_NOT_VOICE_COMPATIBLE /*-97*/:
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Starting under voice control not allowed for: ");
                    stringBuilder.append(intent);
                    throw new SecurityException(stringBuilder.toString());
                case ActivityManager.START_CANCELED /*-96*/:
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Activity could not be started for ");
                    stringBuilder.append(intent);
                    throw new AndroidRuntimeException(stringBuilder.toString());
                case ActivityManager.START_NOT_ACTIVITY /*-95*/:
                    throw new IllegalArgumentException("PendingIntent is not an activity");
                case ActivityManager.START_PERMISSION_DENIED /*-94*/:
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Not allowed to start activity ");
                    stringBuilder.append(intent);
                    throw new SecurityException(stringBuilder.toString());
                case ActivityManager.START_FORWARD_AND_REQUEST_CONFLICT /*-93*/:
                    throw new AndroidRuntimeException("FORWARD_RESULT_FLAG used while also requesting a result");
                case ActivityManager.START_CLASS_NOT_FOUND /*-92*/:
                case ActivityManager.START_INTENT_NOT_RESOLVED /*-91*/:
                    if (!(intent instanceof Intent) || ((Intent) intent).getComponent() == null) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("No Activity found to handle ");
                        stringBuilder.append(intent);
                        throw new ActivityNotFoundException(stringBuilder.toString());
                    }
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Unable to find explicit activity class ");
                    stringBuilder.append(((Intent) intent).getComponent().toShortString());
                    stringBuilder.append("; have you declared this activity in your AndroidManifest.xml?");
                    throw new ActivityNotFoundException(stringBuilder.toString());
                case ActivityManager.START_ASSISTANT_HIDDEN_SESSION /*-90*/:
                    throw new IllegalStateException("Cannot start assistant activity on a hidden session");
                case ActivityManager.START_ASSISTANT_NOT_ACTIVE_SESSION /*-89*/:
                    throw new IllegalStateException("Session calling startAssistantActivity does not match active session");
                default:
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Unknown error code ");
                    stringBuilder.append(res);
                    stringBuilder.append(" when starting ");
                    stringBuilder.append(intent);
                    throw new AndroidRuntimeException(stringBuilder.toString());
            }
        }
    }

    private final void validateNotAppThread() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new RuntimeException("This method can not be called from the main application thread");
        }
    }

    public UiAutomation getUiAutomation() {
        return getUiAutomation(0);
    }

    public UiAutomation getUiAutomation(int flags) {
        boolean mustCreateNewAutomation = this.mUiAutomation == null || this.mUiAutomation.isDestroyed();
        if (this.mUiAutomationConnection == null) {
            return null;
        }
        if (!mustCreateNewAutomation && this.mUiAutomation.getFlags() == flags) {
            return this.mUiAutomation;
        }
        if (mustCreateNewAutomation) {
            this.mUiAutomation = new UiAutomation(getTargetContext().getMainLooper(), this.mUiAutomationConnection);
        } else {
            this.mUiAutomation.disconnect();
        }
        this.mUiAutomation.connect(flags);
        return this.mUiAutomation;
    }

    public TestLooperManager acquireLooperManager(Looper looper) {
        checkInstrumenting("acquireLooperManager");
        return new TestLooperManager(looper);
    }
}
