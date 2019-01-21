package android.app;

import android.app.ActivityThread.ActivityClientRecord;
import android.content.ComponentCallbacks;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.MemoryLeakMonitorManager;
import android.util.Log;
import android.view.autofill.AutofillManager.AutofillClient;
import android.view.autofill.Helper;
import java.util.ArrayList;

public class Application extends ContextWrapper implements ComponentCallbacks2 {
    private static final String TAG = "Application";
    private ArrayList<ActivityLifecycleCallbacks> mActivityLifecycleCallbacks = new ArrayList();
    private ArrayList<OnProvideAssistDataListener> mAssistCallbacks = null;
    private ArrayList<ComponentCallbacks> mComponentCallbacks = new ArrayList();
    public LoadedApk mLoadedApk;

    public interface ActivityLifecycleCallbacks {
        void onActivityCreated(Activity activity, Bundle bundle);

        void onActivityDestroyed(Activity activity);

        void onActivityPaused(Activity activity);

        void onActivityResumed(Activity activity);

        void onActivitySaveInstanceState(Activity activity, Bundle bundle);

        void onActivityStarted(Activity activity);

        void onActivityStopped(Activity activity);
    }

    public interface OnProvideAssistDataListener {
        void onProvideAssistData(Activity activity, Bundle bundle);
    }

    public Application() {
        super(null);
    }

    public void onCreate() {
    }

    public void installMemoryLeakMonitor() {
        MemoryLeakMonitorManager.installMemoryLeakMonitor(this);
    }

    public void onTerminate() {
    }

    public void onConfigurationChanged(Configuration newConfig) {
        Object[] callbacks = collectComponentCallbacks();
        if (callbacks != null) {
            for (Object obj : callbacks) {
                ((ComponentCallbacks) obj).onConfigurationChanged(newConfig);
            }
        }
    }

    public void onLowMemory() {
        Object[] callbacks = collectComponentCallbacks();
        if (callbacks != null) {
            for (Object obj : callbacks) {
                ((ComponentCallbacks) obj).onLowMemory();
            }
        }
    }

    public void onTrimMemory(int level) {
        Object[] callbacks = collectComponentCallbacks();
        if (callbacks != null) {
            for (Object c : callbacks) {
                if (c instanceof ComponentCallbacks2) {
                    ((ComponentCallbacks2) c).onTrimMemory(level);
                }
            }
        }
    }

    public void registerComponentCallbacks(ComponentCallbacks callback) {
        synchronized (this.mComponentCallbacks) {
            this.mComponentCallbacks.add(callback);
        }
    }

    public void unregisterComponentCallbacks(ComponentCallbacks callback) {
        synchronized (this.mComponentCallbacks) {
            this.mComponentCallbacks.remove(callback);
        }
    }

    public void registerActivityLifecycleCallbacks(ActivityLifecycleCallbacks callback) {
        synchronized (this.mActivityLifecycleCallbacks) {
            this.mActivityLifecycleCallbacks.add(callback);
        }
    }

    public void unregisterActivityLifecycleCallbacks(ActivityLifecycleCallbacks callback) {
        synchronized (this.mActivityLifecycleCallbacks) {
            this.mActivityLifecycleCallbacks.remove(callback);
        }
    }

    public void registerOnProvideAssistDataListener(OnProvideAssistDataListener callback) {
        synchronized (this) {
            if (this.mAssistCallbacks == null) {
                this.mAssistCallbacks = new ArrayList();
            }
            this.mAssistCallbacks.add(callback);
        }
    }

    public void unregisterOnProvideAssistDataListener(OnProvideAssistDataListener callback) {
        synchronized (this) {
            if (this.mAssistCallbacks != null) {
                this.mAssistCallbacks.remove(callback);
            }
        }
    }

    public static String getProcessName() {
        return ActivityThread.currentProcessName();
    }

    final void attach(Context context) {
        attachBaseContext(context);
        this.mLoadedApk = ContextImpl.getImpl(context).mPackageInfo;
    }

    void dispatchActivityCreated(Activity activity, Bundle savedInstanceState) {
        Object[] callbacks = collectActivityLifecycleCallbacks();
        if (callbacks != null) {
            for (Object obj : callbacks) {
                ((ActivityLifecycleCallbacks) obj).onActivityCreated(activity, savedInstanceState);
            }
        }
    }

    void dispatchActivityStarted(Activity activity) {
        Object[] callbacks = collectActivityLifecycleCallbacks();
        if (callbacks != null) {
            for (Object obj : callbacks) {
                ((ActivityLifecycleCallbacks) obj).onActivityStarted(activity);
            }
        }
    }

    void dispatchActivityResumed(Activity activity) {
        Object[] callbacks = collectActivityLifecycleCallbacks();
        if (callbacks != null) {
            for (Object obj : callbacks) {
                ((ActivityLifecycleCallbacks) obj).onActivityResumed(activity);
            }
        }
    }

    void dispatchActivityPaused(Activity activity) {
        Object[] callbacks = collectActivityLifecycleCallbacks();
        if (callbacks != null) {
            for (Object obj : callbacks) {
                ((ActivityLifecycleCallbacks) obj).onActivityPaused(activity);
            }
        }
    }

    void dispatchActivityStopped(Activity activity) {
        Object[] callbacks = collectActivityLifecycleCallbacks();
        if (callbacks != null) {
            for (Object obj : callbacks) {
                ((ActivityLifecycleCallbacks) obj).onActivityStopped(activity);
            }
        }
    }

    void dispatchActivitySaveInstanceState(Activity activity, Bundle outState) {
        Object[] callbacks = collectActivityLifecycleCallbacks();
        if (callbacks != null) {
            for (Object obj : callbacks) {
                ((ActivityLifecycleCallbacks) obj).onActivitySaveInstanceState(activity, outState);
            }
        }
    }

    void dispatchActivityDestroyed(Activity activity) {
        Object[] callbacks = collectActivityLifecycleCallbacks();
        if (callbacks != null) {
            for (Object obj : callbacks) {
                ((ActivityLifecycleCallbacks) obj).onActivityDestroyed(activity);
            }
        }
    }

    private Object[] collectComponentCallbacks() {
        Object[] callbacks = null;
        synchronized (this.mComponentCallbacks) {
            if (this.mComponentCallbacks.size() > 0) {
                callbacks = this.mComponentCallbacks.toArray();
            }
        }
        return callbacks;
    }

    private Object[] collectActivityLifecycleCallbacks() {
        Object[] callbacks = null;
        synchronized (this.mActivityLifecycleCallbacks) {
            if (this.mActivityLifecycleCallbacks.size() > 0) {
                callbacks = this.mActivityLifecycleCallbacks.toArray();
            }
        }
        return callbacks;
    }

    /* JADX WARNING: Missing block: B:8:0x000e, code skipped:
            if (r0 == null) goto L_0x001e;
     */
    /* JADX WARNING: Missing block: B:9:0x0010, code skipped:
            r1 = 0;
     */
    /* JADX WARNING: Missing block: B:11:0x0012, code skipped:
            if (r1 >= r0.length) goto L_0x001e;
     */
    /* JADX WARNING: Missing block: B:12:0x0014, code skipped:
            ((android.app.Application.OnProvideAssistDataListener) r0[r1]).onProvideAssistData(r4, r5);
            r1 = r1 + 1;
     */
    /* JADX WARNING: Missing block: B:13:0x001e, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void dispatchOnProvideAssistData(Activity activity, Bundle data) {
        synchronized (this) {
            if (this.mAssistCallbacks == null) {
                return;
            }
            Object[] callbacks = this.mAssistCallbacks.toArray();
        }
    }

    public AutofillClient getAutofillClient() {
        AutofillClient client = super.getAutofillClient();
        if (client != null) {
            return client;
        }
        if (Helper.sVerbose) {
            Log.v(TAG, "getAutofillClient(): null on super, trying to find activity thread");
        }
        ActivityThread activityThread = ActivityThread.currentActivityThread();
        if (activityThread == null) {
            return null;
        }
        int activityCount = activityThread.mActivities.size();
        for (int i = 0; i < activityCount; i++) {
            ActivityClientRecord record = (ActivityClientRecord) activityThread.mActivities.valueAt(i);
            if (record != null) {
                Activity activity = record.activity;
                if (activity != null && activity.getWindow().getDecorView().hasFocus()) {
                    if (Helper.sVerbose) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("getAutofillClient(): found activity for ");
                        stringBuilder.append(this);
                        stringBuilder.append(": ");
                        stringBuilder.append(activity);
                        Log.v(str, stringBuilder.toString());
                    }
                    return activity;
                }
            }
        }
        if (Helper.sVerbose) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("getAutofillClient(): none of the ");
            stringBuilder2.append(activityCount);
            stringBuilder2.append(" activities on ");
            stringBuilder2.append(this);
            stringBuilder2.append(" have focus");
            Log.v(str2, stringBuilder2.toString());
        }
        return null;
    }
}
