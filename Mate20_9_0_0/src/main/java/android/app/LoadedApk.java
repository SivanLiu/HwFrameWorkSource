package android.app;

import android.common.HwFrameworkFactory;
import android.content.BroadcastReceiver;
import android.content.BroadcastReceiver.PendingResult;
import android.content.ComponentName;
import android.content.Context;
import android.content.IIntentReceiver;
import android.content.IIntentReceiver.Stub;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PackageParser;
import android.content.pm.dex.ArtManager;
import android.content.pm.split.SplitDependencyLoader;
import android.content.res.AssetManager;
import android.content.res.CompatibilityInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.FileUtils;
import android.os.Handler;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.Looper;
import android.os.Process;
import android.os.RemoteException;
import android.os.StrictMode;
import android.os.StrictMode.ThreadPolicy;
import android.os.SystemProperties;
import android.os.Trace;
import android.os.UserHandle;
import android.rms.HwSysResource;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.HwPCUtils;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import android.view.DisplayAdjustments;
import com.android.internal.util.ArrayUtils;
import dalvik.system.VMRuntime;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public final class LoadedApk {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    static final boolean DEBUG = false;
    private static final String PROPERTY_NAME_APPEND_NATIVE = "pi.append_native_lib_paths";
    static final String TAG = "LoadedApk";
    static AtomicInteger mBroadcastDebugID = new AtomicInteger(0);
    private final ActivityThread mActivityThread;
    private AppComponentFactory mAppComponentFactory;
    private String mAppDir;
    private Application mApplication;
    private ApplicationInfo mApplicationInfo;
    private final ClassLoader mBaseClassLoader;
    private ClassLoader mClassLoader;
    private File mCredentialProtectedDataDirFile;
    private String mDataDir;
    private File mDataDirFile;
    private File mDeviceProtectedDataDirFile;
    private final DisplayAdjustments mDisplayAdjustments = new DisplayAdjustments();
    private final boolean mIncludeCode;
    private String mLibDir;
    private String[] mOverlayDirs;
    final String mPackageName;
    private HwSysResource mReceiverResource;
    private final ArrayMap<Context, ArrayMap<BroadcastReceiver, ReceiverDispatcher>> mReceivers = new ArrayMap();
    private final boolean mRegisterPackage;
    private String mResDir;
    Resources mResources;
    private final boolean mSecurityViolation;
    private final ArrayMap<Context, ArrayMap<ServiceConnection, ServiceDispatcher>> mServices = new ArrayMap();
    private String[] mSplitAppDirs;
    private String[] mSplitClassLoaderNames;
    private SplitDependencyLoaderImpl mSplitLoader;
    private String[] mSplitNames;
    private String[] mSplitResDirs;
    private final ArrayMap<Context, ArrayMap<ServiceConnection, ServiceDispatcher>> mUnboundServices = new ArrayMap();
    private final ArrayMap<Context, ArrayMap<BroadcastReceiver, ReceiverDispatcher>> mUnregisteredReceivers = new ArrayMap();

    static final class ReceiverDispatcher {
        final Handler mActivityThread;
        final Context mContext;
        boolean mForgotten;
        final Stub mIIntentReceiver;
        final Instrumentation mInstrumentation;
        final IntentReceiverLeaked mLocation;
        final BroadcastReceiver mReceiver;
        final boolean mRegistered;
        RuntimeException mUnregisterLocation;

        final class Args extends PendingResult {
            private Intent mCurIntent;
            int mDebugID;
            private boolean mDispatched;
            private Intent mLastIntent;
            private final boolean mOrdered;
            private Throwable mPreviousRunStacktrace;
            final /* synthetic */ ReceiverDispatcher this$0;

            public Args(ReceiverDispatcher this$0, Intent intent, int resultCode, String resultData, Bundle resultExtras, boolean ordered, boolean sticky, int sendingUser, int debugID) {
                ReceiverDispatcher receiverDispatcher = this$0;
                this.this$0 = receiverDispatcher;
                super(resultCode, resultData, resultExtras, receiverDispatcher.mRegistered ? 1 : 2, ordered, sticky, receiverDispatcher.mIIntentReceiver.asBinder(), sendingUser, intent.getFlags());
                this.mCurIntent = intent;
                this.mOrdered = ordered;
                this.mDebugID = debugID;
            }

            public final Runnable getRunnable() {
                return new -$$Lambda$LoadedApk$ReceiverDispatcher$Args$_BumDX2UKsnxLVrE6UJsJZkotuA(this);
            }

            public static /* synthetic */ void lambda$getRunnable$0(Args args) {
                BroadcastReceiver receiver = args.this$0.mReceiver;
                boolean ordered = args.mOrdered;
                if (ActivityThread.DEBUG_BROADCAST) {
                    int seq = args.mCurIntent.getIntExtra("seq", -1);
                    String str = ActivityThread.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Dispatching broadcast ");
                    stringBuilder.append(args.mCurIntent.getAction());
                    stringBuilder.append(" seq=");
                    stringBuilder.append(seq);
                    stringBuilder.append(" to ");
                    stringBuilder.append(args.this$0.mReceiver);
                    Slog.i(str, stringBuilder.toString());
                    str = ActivityThread.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("  mRegistered=");
                    stringBuilder.append(args.this$0.mRegistered);
                    stringBuilder.append(" mOrderedHint=");
                    stringBuilder.append(ordered);
                    Slog.i(str, stringBuilder.toString());
                }
                IActivityManager mgr = ActivityManager.getService();
                Intent intent = args.mCurIntent;
                String str2;
                StringBuilder stringBuilder2;
                if (intent == null) {
                    str2 = LoadedApk.TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Null intent being dispatched, mDispatched=");
                    stringBuilder2.append(args.mDispatched);
                    stringBuilder2.append(": run() previously called at ");
                    stringBuilder2.append(Log.getStackTraceString(args.mPreviousRunStacktrace));
                    Log.wtf(str2, stringBuilder2.toString());
                    return;
                }
                args.mCurIntent = null;
                args.mDispatched = true;
                args.mPreviousRunStacktrace = new Throwable("Previous stacktrace");
                if (receiver == null || intent == null || args.this$0.mForgotten) {
                    if (args.this$0.mRegistered && ordered) {
                        if (ActivityThread.DEBUG_BROADCAST) {
                            str2 = ActivityThread.TAG;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Finishing null broadcast to ");
                            stringBuilder2.append(args.this$0.mReceiver);
                            Slog.i(str2, stringBuilder2.toString());
                        }
                        args.sendFinished(mgr);
                    }
                    return;
                }
                Trace.traceBegin(64, "broadcastReceiveReg");
                try {
                    ClassLoader cl = args.this$0.mReceiver.getClass().getClassLoader();
                    intent.setExtrasClassLoader(cl);
                    intent.prepareToEnterProcess();
                    args.setExtrasClassLoader(cl);
                    receiver.setPendingResult(args);
                    args.mLastIntent = intent;
                    if (Log.HWINFO) {
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("broadcast[");
                        stringBuilder3.append(intent.getAction());
                        stringBuilder3.append("](");
                        stringBuilder3.append(args.mDebugID);
                        stringBuilder3.append(") call onReceive[");
                        stringBuilder3.append(receiver.getClass().getName());
                        stringBuilder3.append("]");
                        Trace.traceBegin(64, stringBuilder3.toString());
                    }
                    receiver.onReceive(args.this$0.mContext, intent);
                    if (Log.HWINFO) {
                        Trace.traceEnd(64);
                    }
                } catch (Exception e) {
                    if (args.this$0.mRegistered && ordered) {
                        if (ActivityThread.DEBUG_BROADCAST) {
                            String str3 = ActivityThread.TAG;
                            StringBuilder stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("Finishing failed broadcast to ");
                            stringBuilder4.append(args.this$0.mReceiver);
                            Slog.i(str3, stringBuilder4.toString());
                        }
                        args.sendFinished(mgr);
                    }
                    if (args.this$0.mInstrumentation == null || !args.this$0.mInstrumentation.onException(args.this$0.mReceiver, e)) {
                        Trace.traceEnd(64);
                        StringBuilder stringBuilder5 = new StringBuilder();
                        stringBuilder5.append("Error receiving broadcast ");
                        stringBuilder5.append(intent);
                        stringBuilder5.append(" in ");
                        stringBuilder5.append(args.this$0.mReceiver);
                        throw new RuntimeException(stringBuilder5.toString(), e);
                    }
                }
                if (receiver.getPendingResult() != null) {
                    args.finish();
                }
                Trace.traceEnd(64);
            }

            public String toString() {
                StringBuilder sb = new StringBuilder();
                try {
                    if (this.this$0.mReceiver != null) {
                        sb.append(" receiver=");
                        sb.append(this.this$0.mReceiver.getClass().getName());
                        sb.append(" act=");
                        if (this.mLastIntent != null) {
                            sb.append(this.mLastIntent.getAction());
                        } else {
                            sb.append("null");
                        }
                    }
                } catch (Exception e) {
                    Log.i(LoadedApk.TAG, "Could not get Class Name", e);
                }
                return sb.toString();
            }
        }

        static final class InnerReceiver extends Stub {
            final WeakReference<ReceiverDispatcher> mDispatcher;
            final ReceiverDispatcher mStrongRef;

            InnerReceiver(ReceiverDispatcher rd, boolean strong) {
                this.mDispatcher = new WeakReference(rd);
                this.mStrongRef = strong ? rd : null;
            }

            public void performReceive(Intent intent, int resultCode, String data, Bundle extras, boolean ordered, boolean sticky, int sendingUser) {
                ReceiverDispatcher rd;
                InnerReceiver innerReceiver;
                Intent intent2 = intent;
                Bundle bundle = extras;
                if (intent2 == null) {
                    Log.wtf(LoadedApk.TAG, "Null intent received");
                    rd = null;
                    innerReceiver = this;
                } else {
                    innerReceiver = this;
                    rd = (ReceiverDispatcher) innerReceiver.mDispatcher.get();
                }
                ReceiverDispatcher rd2 = rd;
                if (ActivityThread.DEBUG_BROADCAST) {
                    int seq = intent2.getIntExtra("seq", -1);
                    String str = ActivityThread.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Receiving broadcast ");
                    stringBuilder.append(intent2.getAction());
                    stringBuilder.append(" seq=");
                    stringBuilder.append(seq);
                    stringBuilder.append(" to ");
                    stringBuilder.append(rd2 != null ? rd2.mReceiver : null);
                    Slog.i(str, stringBuilder.toString());
                }
                if (rd2 != null) {
                    rd2.performReceive(intent2, resultCode, data, bundle, ordered, sticky, sendingUser);
                    return;
                }
                if (ActivityThread.DEBUG_BROADCAST) {
                    Slog.i(ActivityThread.TAG, "Finishing broadcast to unregistered receiver");
                }
                IActivityManager mgr = ActivityManager.getService();
                if (bundle != null) {
                    try {
                        bundle.setAllowFds(false);
                    } catch (RemoteException e) {
                        throw e.rethrowFromSystemServer();
                    }
                }
                mgr.finishReceiver(innerReceiver, resultCode, data, bundle, false, intent2.getFlags());
            }
        }

        ReceiverDispatcher(BroadcastReceiver receiver, Context context, Handler activityThread, Instrumentation instrumentation, boolean registered) {
            if (activityThread != null) {
                this.mIIntentReceiver = new InnerReceiver(this, registered ^ 1);
                this.mReceiver = receiver;
                this.mContext = context;
                this.mActivityThread = activityThread;
                this.mInstrumentation = instrumentation;
                this.mRegistered = registered;
                this.mLocation = new IntentReceiverLeaked(null);
                this.mLocation.fillInStackTrace();
                return;
            }
            throw new NullPointerException("Handler must not be null");
        }

        void validate(Context context, Handler activityThread) {
            StringBuilder stringBuilder;
            if (this.mContext != context) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Receiver ");
                stringBuilder.append(this.mReceiver);
                stringBuilder.append(" registered with differing Context (was ");
                stringBuilder.append(this.mContext);
                stringBuilder.append(" now ");
                stringBuilder.append(context);
                stringBuilder.append(")");
                throw new IllegalStateException(stringBuilder.toString());
            } else if (this.mActivityThread != activityThread) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Receiver ");
                stringBuilder.append(this.mReceiver);
                stringBuilder.append(" registered with differing handler (was ");
                stringBuilder.append(this.mActivityThread);
                stringBuilder.append(" now ");
                stringBuilder.append(activityThread);
                stringBuilder.append(")");
                throw new IllegalStateException(stringBuilder.toString());
            }
        }

        IntentReceiverLeaked getLocation() {
            return this.mLocation;
        }

        BroadcastReceiver getIntentReceiver() {
            return this.mReceiver;
        }

        IIntentReceiver getIIntentReceiver() {
            return this.mIIntentReceiver;
        }

        void setUnregisterLocation(RuntimeException ex) {
            this.mUnregisterLocation = ex;
        }

        RuntimeException getUnregisterLocation() {
            return this.mUnregisterLocation;
        }

        public void performReceive(Intent intent, int resultCode, String data, Bundle extras, boolean ordered, boolean sticky, int sendingUser) {
            StringBuilder sb;
            Intent intent2 = intent;
            int debugID = 0;
            if (Log.HWINFO) {
                debugID = LoadedApk.mBroadcastDebugID.incrementAndGet();
            }
            int debugID2 = debugID;
            Args args = new Args(this, intent2, resultCode, data, extras, ordered, sticky, sendingUser, debugID2);
            if (intent2 == null) {
                Log.wtf(LoadedApk.TAG, "Null intent received");
            } else if (ActivityThread.DEBUG_BROADCAST) {
                int seq = intent2.getIntExtra("seq", -1);
                String str = ActivityThread.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Enqueueing broadcast ");
                stringBuilder.append(intent2.getAction());
                stringBuilder.append(" seq=");
                stringBuilder.append(seq);
                stringBuilder.append(" to ");
                stringBuilder.append(this.mReceiver);
                Slog.i(str, stringBuilder.toString());
            }
            if (Log.HWINFO) {
                Object obj = null;
                Looper looper = this.mActivityThread != null ? this.mActivityThread.getLooper() : null;
                Thread thread = looper != null ? looper.getThread() : null;
                sb = new StringBuilder();
                sb.append("broadcast[");
                sb.append(intent2 != null ? intent2.getAction() : null);
                sb.append("](");
                sb.append(debugID2);
                sb.append(") send msg to thread[");
                sb.append(thread != null ? thread.getName() : null);
                sb.append("(");
                if (thread != null) {
                    obj = Long.valueOf(thread.getId());
                }
                sb.append(obj);
                sb.append(")");
                sb.append("], msg queue length[");
                sb.append(looper != null ? looper.getQueue().getMessageCount() : 0);
                sb.append("]");
                Trace.traceBegin(64, sb.toString());
                if (ActivityThread.DEBUG_HW_BROADCAST) {
                    Slog.i(ActivityThread.TAG, sb.toString());
                }
            }
            if ((intent2 == null || !this.mActivityThread.post(args.getRunnable())) && this.mRegistered && ordered) {
                IActivityManager mgr = ActivityManager.getService();
                if (ActivityThread.DEBUG_BROADCAST) {
                    String str2 = ActivityThread.TAG;
                    sb = new StringBuilder();
                    sb.append("Finishing sync broadcast to ");
                    sb.append(this.mReceiver);
                    Slog.i(str2, sb.toString());
                }
                args.sendFinished(mgr);
            }
            if (Log.HWINFO) {
                Trace.traceEnd(64);
            }
        }
    }

    static final class ServiceDispatcher {
        private final ArrayMap<ComponentName, ConnectionInfo> mActiveConnections = new ArrayMap();
        private final Handler mActivityThread;
        private final ServiceConnection mConnection;
        private final Context mContext;
        private final int mFlags;
        private boolean mForgotten;
        private final InnerConnection mIServiceConnection = new InnerConnection(this);
        private final ServiceConnectionLeaked mLocation;
        private RuntimeException mUnbindLocation;

        private static class ConnectionInfo {
            IBinder binder;
            DeathRecipient deathMonitor;

            private ConnectionInfo() {
            }
        }

        private final class RunConnection implements Runnable {
            final int mCommand;
            final boolean mDead;
            final ComponentName mName;
            final IBinder mService;

            RunConnection(ComponentName name, IBinder service, int command, boolean dead) {
                this.mName = name;
                this.mService = service;
                this.mCommand = command;
                this.mDead = dead;
            }

            public void run() {
                if (this.mCommand == 0) {
                    ServiceDispatcher.this.doConnected(this.mName, this.mService, this.mDead);
                } else if (this.mCommand == 1) {
                    ServiceDispatcher.this.doDeath(this.mName, this.mService);
                }
            }
        }

        private final class DeathMonitor implements DeathRecipient {
            final ComponentName mName;
            final IBinder mService;

            DeathMonitor(ComponentName name, IBinder service) {
                this.mName = name;
                this.mService = service;
            }

            public void binderDied() {
                ServiceDispatcher.this.death(this.mName, this.mService);
            }
        }

        private static class InnerConnection extends IServiceConnection.Stub {
            final WeakReference<ServiceDispatcher> mDispatcher;

            InnerConnection(ServiceDispatcher sd) {
                this.mDispatcher = new WeakReference(sd);
            }

            public void connected(ComponentName name, IBinder service, boolean dead) throws RemoteException {
                ServiceDispatcher sd = (ServiceDispatcher) this.mDispatcher.get();
                if (sd != null) {
                    sd.connected(name, service, dead);
                }
            }
        }

        ServiceDispatcher(ServiceConnection conn, Context context, Handler activityThread, int flags) {
            this.mConnection = conn;
            this.mContext = context;
            this.mActivityThread = activityThread;
            this.mLocation = new ServiceConnectionLeaked(null);
            this.mLocation.fillInStackTrace();
            this.mFlags = flags;
        }

        void validate(Context context, Handler activityThread) {
            StringBuilder stringBuilder;
            if (this.mContext != context) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("ServiceConnection ");
                stringBuilder.append(this.mConnection);
                stringBuilder.append(" registered with differing Context (was ");
                stringBuilder.append(this.mContext);
                stringBuilder.append(" now ");
                stringBuilder.append(context);
                stringBuilder.append(")");
                throw new RuntimeException(stringBuilder.toString());
            } else if (this.mActivityThread != activityThread) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("ServiceConnection ");
                stringBuilder.append(this.mConnection);
                stringBuilder.append(" registered with differing handler (was ");
                stringBuilder.append(this.mActivityThread);
                stringBuilder.append(" now ");
                stringBuilder.append(activityThread);
                stringBuilder.append(")");
                throw new RuntimeException(stringBuilder.toString());
            }
        }

        void doForget() {
            synchronized (this) {
                for (int i = 0; i < this.mActiveConnections.size(); i++) {
                    ConnectionInfo ci = (ConnectionInfo) this.mActiveConnections.valueAt(i);
                    ci.binder.unlinkToDeath(ci.deathMonitor, 0);
                }
                this.mActiveConnections.clear();
                this.mForgotten = true;
            }
        }

        ServiceConnectionLeaked getLocation() {
            return this.mLocation;
        }

        ServiceConnection getServiceConnection() {
            return this.mConnection;
        }

        IServiceConnection getIServiceConnection() {
            return this.mIServiceConnection;
        }

        int getFlags() {
            return this.mFlags;
        }

        void setUnbindLocation(RuntimeException ex) {
            this.mUnbindLocation = ex;
        }

        RuntimeException getUnbindLocation() {
            return this.mUnbindLocation;
        }

        public void connected(ComponentName name, IBinder service, boolean dead) {
            if (name == null || !"com.android.systemui.keyguard.KeyguardService".equals(name.getClassName())) {
                if (this.mActivityThread != null) {
                    this.mActivityThread.post(new RunConnection(name, service, 0, dead));
                } else {
                    doConnected(name, service, dead);
                }
                return;
            }
            doConnected(name, service, dead);
        }

        public void death(ComponentName name, IBinder service) {
            if (this.mActivityThread != null) {
                this.mActivityThread.post(new RunConnection(name, service, 1, false));
            } else {
                doDeath(name, service);
            }
        }

        /* JADX WARNING: Missing block: B:26:0x004b, code skipped:
            if (r0 == null) goto L_0x0052;
     */
        /* JADX WARNING: Missing block: B:27:0x004d, code skipped:
            r4.mConnection.onServiceDisconnected(r5);
     */
        /* JADX WARNING: Missing block: B:28:0x0052, code skipped:
            if (r7 == false) goto L_0x0059;
     */
        /* JADX WARNING: Missing block: B:29:0x0054, code skipped:
            r4.mConnection.onBindingDied(r5);
     */
        /* JADX WARNING: Missing block: B:30:0x0059, code skipped:
            if (r6 == null) goto L_0x0061;
     */
        /* JADX WARNING: Missing block: B:31:0x005b, code skipped:
            r4.mConnection.onServiceConnected(r5, r6);
     */
        /* JADX WARNING: Missing block: B:32:0x0061, code skipped:
            r4.mConnection.onNullBinding(r5);
     */
        /* JADX WARNING: Missing block: B:33:0x0066, code skipped:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void doConnected(ComponentName name, IBinder service, boolean dead) {
            synchronized (this) {
                if (this.mForgotten) {
                    return;
                }
                ConnectionInfo old = (ConnectionInfo) this.mActiveConnections.get(name);
                if (old == null || old.binder != service) {
                    if (service != null) {
                        ConnectionInfo info = new ConnectionInfo();
                        info.binder = service;
                        info.deathMonitor = new DeathMonitor(name, service);
                        try {
                            service.linkToDeath(info.deathMonitor, 0);
                            this.mActiveConnections.put(name, info);
                        } catch (RemoteException e) {
                            this.mActiveConnections.remove(name);
                            return;
                        }
                    }
                    this.mActiveConnections.remove(name);
                    if (old != null) {
                        old.binder.unlinkToDeath(old.deathMonitor, 0);
                    }
                }
            }
        }

        public void doDeath(ComponentName name, IBinder service) {
            synchronized (this) {
                ConnectionInfo old = (ConnectionInfo) this.mActiveConnections.get(name);
                if (old != null) {
                    if (old.binder == service) {
                        this.mActiveConnections.remove(name);
                        old.binder.unlinkToDeath(old.deathMonitor, 0);
                        this.mConnection.onServiceDisconnected(name);
                        return;
                    }
                }
            }
        }
    }

    private static class WarningContextClassLoader extends ClassLoader {
        private static boolean warned = false;

        private WarningContextClassLoader() {
        }

        private void warn(String methodName) {
            if (!warned) {
                warned = true;
                Thread.currentThread().setContextClassLoader(getParent());
                String str = ActivityThread.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("ClassLoader.");
                stringBuilder.append(methodName);
                stringBuilder.append(": The class loader returned by Thread.getContextClassLoader() may fail for processes that host multiple applications. You should explicitly specify a context class loader. For example: Thread.setContextClassLoader(getClass().getClassLoader());");
                Slog.w(str, stringBuilder.toString());
            }
        }

        public URL getResource(String resName) {
            warn("getResource");
            return getParent().getResource(resName);
        }

        public Enumeration<URL> getResources(String resName) throws IOException {
            warn("getResources");
            return getParent().getResources(resName);
        }

        public InputStream getResourceAsStream(String resName) {
            warn("getResourceAsStream");
            return getParent().getResourceAsStream(resName);
        }

        public Class<?> loadClass(String className) throws ClassNotFoundException {
            warn("loadClass");
            return getParent().loadClass(className);
        }

        public void setClassAssertionStatus(String cname, boolean enable) {
            warn("setClassAssertionStatus");
            getParent().setClassAssertionStatus(cname, enable);
        }

        public void setPackageAssertionStatus(String pname, boolean enable) {
            warn("setPackageAssertionStatus");
            getParent().setPackageAssertionStatus(pname, enable);
        }

        public void setDefaultAssertionStatus(boolean enable) {
            warn("setDefaultAssertionStatus");
            getParent().setDefaultAssertionStatus(enable);
        }

        public void clearAssertionStatus() {
            warn("clearAssertionStatus");
            getParent().clearAssertionStatus();
        }
    }

    private class SplitDependencyLoaderImpl extends SplitDependencyLoader<NameNotFoundException> {
        private final ClassLoader[] mCachedClassLoaders;
        private final String[][] mCachedResourcePaths;

        SplitDependencyLoaderImpl(SparseArray<int[]> dependencies) {
            super(dependencies);
            this.mCachedResourcePaths = new String[(LoadedApk.this.mSplitNames.length + 1)][];
            this.mCachedClassLoaders = new ClassLoader[(LoadedApk.this.mSplitNames.length + 1)];
        }

        protected boolean isSplitCached(int splitIdx) {
            return this.mCachedClassLoaders[splitIdx] != null;
        }

        protected void constructSplit(int splitIdx, int[] configSplitIndices, int parentSplitIdx) throws NameNotFoundException {
            ArrayList<String> splitPaths = new ArrayList();
            int i = 0;
            int length;
            if (splitIdx == 0) {
                LoadedApk.this.createOrUpdateClassLoaderLocked(null);
                this.mCachedClassLoaders[0] = LoadedApk.this.mClassLoader;
                for (int configSplitIdx : configSplitIndices) {
                    splitPaths.add(LoadedApk.this.mSplitResDirs[configSplitIdx - 1]);
                }
                this.mCachedResourcePaths[0] = (String[]) splitPaths.toArray(new String[splitPaths.size()]);
                return;
            }
            this.mCachedClassLoaders[splitIdx] = ApplicationLoaders.getDefault().getClassLoader(LoadedApk.this.mSplitAppDirs[splitIdx - 1], LoadedApk.this.getTargetSdkVersion(), false, null, null, this.mCachedClassLoaders[parentSplitIdx], LoadedApk.this.mSplitClassLoaderNames[splitIdx - 1]);
            Collections.addAll(splitPaths, this.mCachedResourcePaths[parentSplitIdx]);
            splitPaths.add(LoadedApk.this.mSplitResDirs[splitIdx - 1]);
            length = configSplitIndices.length;
            while (i < length) {
                splitPaths.add(LoadedApk.this.mSplitResDirs[configSplitIndices[i] - 1]);
                i++;
            }
            this.mCachedResourcePaths[splitIdx] = (String[]) splitPaths.toArray(new String[splitPaths.size()]);
        }

        private int ensureSplitLoaded(String splitName) throws NameNotFoundException {
            int idx = 0;
            if (splitName != null) {
                idx = Arrays.binarySearch(LoadedApk.this.mSplitNames, splitName);
                if (idx >= 0) {
                    idx++;
                } else {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Split name '");
                    stringBuilder.append(splitName);
                    stringBuilder.append("' is not installed");
                    throw new NameNotFoundException(stringBuilder.toString());
                }
            }
            loadDependenciesForSplit(idx);
            return idx;
        }

        ClassLoader getClassLoaderForSplit(String splitName) throws NameNotFoundException {
            return this.mCachedClassLoaders[ensureSplitLoaded(splitName)];
        }

        String[] getSplitPathsForSplit(String splitName) throws NameNotFoundException {
            return this.mCachedResourcePaths[ensureSplitLoaded(splitName)];
        }
    }

    Application getApplication() {
        return this.mApplication;
    }

    public LoadedApk(ActivityThread activityThread, ApplicationInfo aInfo, CompatibilityInfo compatInfo, ClassLoader baseLoader, boolean securityViolation, boolean includeCode, boolean registerPackage) {
        this.mActivityThread = activityThread;
        setApplicationInfo(aInfo);
        this.mPackageName = aInfo.packageName;
        this.mBaseClassLoader = baseLoader;
        this.mSecurityViolation = securityViolation;
        this.mIncludeCode = includeCode;
        this.mRegisterPackage = registerPackage;
        this.mDisplayAdjustments.setCompatibilityInfo(compatInfo);
        this.mAppComponentFactory = createAppFactory(this.mApplicationInfo, this.mBaseClassLoader);
    }

    private static ApplicationInfo adjustNativeLibraryPaths(ApplicationInfo info) {
        if (!(info.primaryCpuAbi == null || info.secondaryCpuAbi == null)) {
            String runtimeIsa = VMRuntime.getRuntime().vmInstructionSet();
            String secondaryIsa = VMRuntime.getInstructionSet(info.secondaryCpuAbi);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("ro.dalvik.vm.isa.");
            stringBuilder.append(secondaryIsa);
            String secondaryDexCodeIsa = SystemProperties.get(stringBuilder.toString());
            if (runtimeIsa.equals(secondaryDexCodeIsa.isEmpty() ? secondaryIsa : secondaryDexCodeIsa)) {
                ApplicationInfo modified = new ApplicationInfo(info);
                modified.nativeLibraryDir = modified.secondaryNativeLibraryDir;
                modified.primaryCpuAbi = modified.secondaryCpuAbi;
                return modified;
            }
        }
        return info;
    }

    LoadedApk(ActivityThread activityThread) {
        this.mActivityThread = activityThread;
        this.mApplicationInfo = new ApplicationInfo();
        this.mApplicationInfo.packageName = "android";
        this.mPackageName = "android";
        this.mAppDir = null;
        this.mResDir = null;
        this.mSplitAppDirs = null;
        this.mSplitResDirs = null;
        this.mSplitClassLoaderNames = null;
        this.mOverlayDirs = null;
        this.mDataDir = null;
        this.mDataDirFile = null;
        this.mDeviceProtectedDataDirFile = null;
        this.mCredentialProtectedDataDirFile = null;
        this.mLibDir = null;
        this.mBaseClassLoader = null;
        this.mSecurityViolation = false;
        this.mIncludeCode = true;
        this.mRegisterPackage = false;
        this.mClassLoader = ClassLoader.getSystemClassLoader();
        this.mResources = Resources.getSystem();
        this.mAppComponentFactory = createAppFactory(this.mApplicationInfo, this.mClassLoader);
    }

    void installSystemApplicationInfo(ApplicationInfo info, ClassLoader classLoader) {
        this.mApplicationInfo = info;
        this.mClassLoader = classLoader;
        this.mAppComponentFactory = createAppFactory(info, classLoader);
    }

    private AppComponentFactory createAppFactory(ApplicationInfo appInfo, ClassLoader cl) {
        if (!(appInfo.appComponentFactory == null || cl == null)) {
            try {
                return (AppComponentFactory) cl.loadClass(appInfo.appComponentFactory).newInstance();
            } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
                Slog.e(TAG, "Unable to instantiate appComponentFactory", e);
            }
        }
        return AppComponentFactory.DEFAULT;
    }

    public AppComponentFactory getAppFactory() {
        return this.mAppComponentFactory;
    }

    public String getPackageName() {
        return this.mPackageName;
    }

    public ApplicationInfo getApplicationInfo() {
        return this.mApplicationInfo;
    }

    public int getTargetSdkVersion() {
        return this.mApplicationInfo.targetSdkVersion;
    }

    public boolean isSecurityViolation() {
        return this.mSecurityViolation;
    }

    public CompatibilityInfo getCompatibilityInfo() {
        return this.mDisplayAdjustments.getCompatibilityInfo();
    }

    public void setCompatibilityInfo(CompatibilityInfo compatInfo) {
        this.mDisplayAdjustments.setCompatibilityInfo(compatInfo);
    }

    private static String[] getLibrariesFor(String packageName) {
        ApplicationInfo ai = null;
        try {
            ai = ActivityThread.getPackageManager().getApplicationInfo(packageName, 1024, UserHandle.myUserId());
            if (ai == null) {
                return null;
            }
            return ai.sharedLibraryFiles;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void updateApplicationInfo(ApplicationInfo aInfo, List<String> oldPaths) {
        ApplicationInfo applicationInfo = aInfo;
        setApplicationInfo(aInfo);
        ArrayList<String> newPaths = new ArrayList();
        makePaths(this.mActivityThread, applicationInfo, newPaths);
        ArrayList addedPaths = new ArrayList(newPaths.size());
        if (oldPaths != null) {
            for (String path : newPaths) {
                String apkName = path.substring(path.lastIndexOf(File.separator));
                boolean match = false;
                for (String oldPath : oldPaths) {
                    if (apkName.equals(oldPath.substring(oldPath.lastIndexOf(File.separator)))) {
                        match = true;
                        break;
                    }
                }
                if (!match) {
                    addedPaths.add(path);
                }
            }
        } else {
            addedPaths.addAll(newPaths);
        }
        synchronized (this) {
            createOrUpdateClassLoaderLocked(addedPaths);
            if (this.mResources != null) {
                try {
                    String[] splitPaths = getSplitPaths(null);
                    ResourcesManager.getInstance().setHwThemeType(this.mResDir, this.mApplicationInfo.hwThemeType);
                    if (HwPCUtils.enabled() && ActivityThread.currentActivityThread() != null && HwPCUtils.isValidExtDisplayId(ActivityThread.currentActivityThread().mDisplayId)) {
                        this.mResources = ResourcesManager.getInstance().getResources(null, this.mResDir, splitPaths, this.mOverlayDirs, this.mApplicationInfo.sharedLibraryFiles, ActivityThread.currentActivityThread().mDisplayId, null, getCompatibilityInfo(), getClassLoader());
                    } else {
                        this.mResources = ResourcesManager.getInstance().getResources(null, this.mResDir, splitPaths, this.mOverlayDirs, this.mApplicationInfo.sharedLibraryFiles, 0, null, getCompatibilityInfo(), getClassLoader());
                    }
                } catch (NameNotFoundException e) {
                    NameNotFoundException nameNotFoundException = e;
                    throw new AssertionError("null split not found");
                }
            }
        }
        this.mAppComponentFactory = createAppFactory(applicationInfo, this.mClassLoader);
    }

    private void setApplicationInfo(ApplicationInfo aInfo) {
        int myUid = Process.myUid();
        aInfo = adjustNativeLibraryPaths(aInfo);
        this.mApplicationInfo = aInfo;
        this.mAppDir = aInfo.sourceDir;
        this.mResDir = aInfo.uid == myUid ? aInfo.sourceDir : aInfo.publicSourceDir;
        this.mOverlayDirs = aInfo.resourceDirs;
        this.mDataDir = aInfo.dataDir;
        this.mLibDir = aInfo.nativeLibraryDir;
        this.mDataDirFile = FileUtils.newFileOrNull(aInfo.dataDir);
        this.mDeviceProtectedDataDirFile = FileUtils.newFileOrNull(aInfo.deviceProtectedDataDir);
        this.mCredentialProtectedDataDirFile = FileUtils.newFileOrNull(aInfo.credentialProtectedDataDir);
        this.mSplitNames = aInfo.splitNames;
        this.mSplitAppDirs = aInfo.splitSourceDirs;
        this.mSplitResDirs = aInfo.uid == myUid ? aInfo.splitSourceDirs : aInfo.splitPublicSourceDirs;
        this.mSplitClassLoaderNames = aInfo.splitClassLoaderNames;
        if (aInfo.requestsIsolatedSplitLoading() && !ArrayUtils.isEmpty(this.mSplitNames)) {
            this.mSplitLoader = new SplitDependencyLoaderImpl(aInfo.splitDependencies);
        }
    }

    public static void makePaths(ActivityThread activityThread, ApplicationInfo aInfo, List<String> outZipPaths) {
        makePaths(activityThread, false, aInfo, outZipPaths, null);
    }

    public static void makePaths(ActivityThread activityThread, boolean isBundledApp, ApplicationInfo aInfo, List<String> outZipPaths, List<String> outLibPaths) {
        String instrumentationAppDir;
        String instrumentedAppDir;
        ActivityThread activityThread2 = activityThread;
        ApplicationInfo applicationInfo = aInfo;
        List<String> list = outZipPaths;
        List<String> list2 = outLibPaths;
        String appDir = applicationInfo.sourceDir;
        String libDir = applicationInfo.nativeLibraryDir;
        String[] sharedLibraries = applicationInfo.sharedLibraryFiles;
        outZipPaths.clear();
        list.add(appDir);
        if (!(applicationInfo.splitSourceDirs == null || aInfo.requestsIsolatedSplitLoading())) {
            Collections.addAll(list, applicationInfo.splitSourceDirs);
        }
        if (list2 != null) {
            outLibPaths.clear();
        }
        String[] instrumentationLibs = null;
        if (activityThread2 != null) {
            String instrumentationPackageName = activityThread2.mInstrumentationPackageName;
            instrumentationAppDir = activityThread2.mInstrumentationAppDir;
            String[] instrumentationSplitAppDirs = activityThread2.mInstrumentationSplitAppDirs;
            String instrumentationLibDir = activityThread2.mInstrumentationLibDir;
            instrumentedAppDir = activityThread2.mInstrumentedAppDir;
            String[] instrumentedSplitAppDirs = activityThread2.mInstrumentedSplitAppDirs;
            String instrumentedLibDir = activityThread2.mInstrumentedLibDir;
            if (appDir.equals(instrumentationAppDir) || appDir.equals(instrumentedAppDir)) {
                outZipPaths.clear();
                list.add(instrumentationAppDir);
                if (!aInfo.requestsIsolatedSplitLoading()) {
                    if (instrumentationSplitAppDirs != null) {
                        Collections.addAll(list, instrumentationSplitAppDirs);
                    }
                    if (!instrumentationAppDir.equals(instrumentedAppDir)) {
                        list.add(instrumentedAppDir);
                        if (instrumentedSplitAppDirs != null) {
                            Collections.addAll(list, instrumentedSplitAppDirs);
                        }
                    }
                }
                if (list2 != null) {
                    list2.add(instrumentationLibDir);
                    if (!instrumentationLibDir.equals(instrumentedLibDir)) {
                        list2.add(instrumentedLibDir);
                    }
                }
                if (!instrumentedAppDir.equals(instrumentationAppDir)) {
                    instrumentationLibs = getLibrariesFor(instrumentationPackageName);
                }
            }
        }
        if (list2 != null) {
            if (outLibPaths.isEmpty()) {
                list2.add(libDir);
            }
            if (applicationInfo.primaryCpuAbi != null) {
                if (applicationInfo.targetSdkVersion < 24) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("/system/fake-libs");
                    stringBuilder.append(VMRuntime.is64BitAbi(applicationInfo.primaryCpuAbi) ? "64" : "");
                    list2.add(stringBuilder.toString());
                }
                for (String instrumentationAppDir2 : outZipPaths) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(instrumentationAppDir2);
                    stringBuilder2.append("!/lib/");
                    stringBuilder2.append(applicationInfo.primaryCpuAbi);
                    list2.add(stringBuilder2.toString());
                }
            }
            if (isBundledApp) {
                list2.add(System.getProperty("java.library.path"));
            }
        }
        if (sharedLibraries != null) {
            int index = 0;
            for (String lib : sharedLibraries) {
                if (!list.contains(lib)) {
                    list.add(index, lib);
                    index++;
                    appendApkLibPathIfNeeded(lib, applicationInfo, list2);
                }
            }
        }
        if (instrumentationLibs != null) {
            for (String instrumentedAppDir2 : instrumentationLibs) {
                if (!list.contains(instrumentedAppDir2)) {
                    list.add(0, instrumentedAppDir2);
                    appendApkLibPathIfNeeded(instrumentedAppDir2, applicationInfo, list2);
                }
            }
        }
    }

    private static void appendApkLibPathIfNeeded(String path, ApplicationInfo applicationInfo, List<String> outLibPaths) {
        if (outLibPaths != null && applicationInfo.primaryCpuAbi != null && path.endsWith(PackageParser.APK_FILE_EXTENSION) && applicationInfo.targetSdkVersion >= 26) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(path);
            stringBuilder.append("!/lib/");
            stringBuilder.append(applicationInfo.primaryCpuAbi);
            outLibPaths.add(stringBuilder.toString());
        }
    }

    ClassLoader getSplitClassLoader(String splitName) throws NameNotFoundException {
        if (this.mSplitLoader == null) {
            return this.mClassLoader;
        }
        return this.mSplitLoader.getClassLoaderForSplit(splitName);
    }

    String[] getSplitPaths(String splitName) throws NameNotFoundException {
        if (this.mSplitLoader == null) {
            return this.mSplitResDirs;
        }
        return this.mSplitLoader.getSplitPathsForSplit(splitName);
    }

    private void createOrUpdateClassLoaderLocked(List<String> addedPaths) {
        List<String> list = addedPaths;
        if (!this.mPackageName.equals("android")) {
            if (!Objects.equals(this.mPackageName, ActivityThread.currentPackageName()) && this.mIncludeCode) {
                try {
                    ActivityThread.getPackageManager().notifyPackageUse(this.mPackageName, 6);
                } catch (RemoteException re) {
                    throw re.rethrowFromSystemServer();
                }
            }
            if (this.mRegisterPackage) {
                try {
                    ActivityManager.getService().addPackageDependency(this.mPackageName);
                } catch (RemoteException re2) {
                    throw re2.rethrowFromSystemServer();
                }
            }
            ArrayList zipPaths = new ArrayList(10);
            ArrayList libPaths = new ArrayList(10);
            boolean isBundledApp = (this.mApplicationInfo.isSystemApp() && !this.mApplicationInfo.isUpdatedSystemApp()) || (this.mApplicationInfo.hwFlags & 536870912) != 0;
            String defaultSearchPaths = System.getProperty("java.library.path");
            boolean treatVendorApkAsUnbundled = defaultSearchPaths.contains("/vendor/lib") ^ true;
            if (this.mApplicationInfo.getCodePath() != null && this.mApplicationInfo.isVendor() && treatVendorApkAsUnbundled) {
                isBundledApp = false;
            }
            boolean isBundledApp2 = isBundledApp;
            makePaths(this.mActivityThread, isBundledApp2, this.mApplicationInfo, zipPaths, libPaths);
            String libraryPermittedPath = this.mDataDir;
            if (isBundledApp2) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(libraryPermittedPath);
                stringBuilder.append(File.pathSeparator);
                stringBuilder.append(Paths.get(getAppDir(), new String[0]).getParent().toString());
                libraryPermittedPath = stringBuilder.toString();
                stringBuilder = new StringBuilder();
                stringBuilder.append(libraryPermittedPath);
                stringBuilder.append(File.pathSeparator);
                stringBuilder.append(defaultSearchPaths);
                libraryPermittedPath = stringBuilder.toString();
            }
            String libraryPermittedPath2 = libraryPermittedPath;
            String librarySearchPath = TextUtils.join(File.pathSeparator, libPaths);
            if (this.mIncludeCode) {
                StringBuilder stringBuilder2;
                boolean isBundledApp3 = isBundledApp2;
                if (zipPaths.size() == 1) {
                    libraryPermittedPath = (String) zipPaths.get(0);
                } else {
                    libraryPermittedPath = TextUtils.join(File.pathSeparator, zipPaths);
                }
                String zip = libraryPermittedPath;
                isBundledApp = false;
                if (this.mClassLoader == null) {
                    ThreadPolicy oldPolicy = StrictMode.allowThreadDiskReads();
                    this.mClassLoader = ApplicationLoaders.getDefault().getClassLoader(zip, this.mApplicationInfo.targetSdkVersion, isBundledApp3, librarySearchPath, libraryPermittedPath2, this.mBaseClassLoader, this.mApplicationInfo.classLoaderName);
                    this.mAppComponentFactory = createAppFactory(this.mApplicationInfo, this.mClassLoader);
                    StrictMode.setThreadPolicy(oldPolicy);
                    isBundledApp = true;
                }
                boolean needToSetupJitProfiles = isBundledApp;
                if (!libPaths.isEmpty() && SystemProperties.getBoolean(PROPERTY_NAME_APPEND_NATIVE, true)) {
                    ThreadPolicy oldPolicy2 = StrictMode.allowThreadDiskReads();
                    try {
                        ApplicationLoaders.getDefault().addNative(this.mClassLoader, libPaths);
                    } finally {
                        StrictMode.setThreadPolicy(oldPolicy2);
                    }
                }
                ArrayList extraLibPaths = new ArrayList(3);
                String abiSuffix = VMRuntime.getRuntime().is64Bit() ? "64" : "";
                if (!defaultSearchPaths.contains("/vendor/lib")) {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("/vendor/lib");
                    stringBuilder2.append(abiSuffix);
                    extraLibPaths.add(stringBuilder2.toString());
                }
                if (!defaultSearchPaths.contains("/odm/lib")) {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("/odm/lib");
                    stringBuilder2.append(abiSuffix);
                    extraLibPaths.add(stringBuilder2.toString());
                }
                if (!defaultSearchPaths.contains("/product/lib")) {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("/product/lib");
                    stringBuilder2.append(abiSuffix);
                    extraLibPaths.add(stringBuilder2.toString());
                }
                if (!extraLibPaths.isEmpty()) {
                    ThreadPolicy oldPolicy3 = StrictMode.allowThreadDiskReads();
                    try {
                        ApplicationLoaders.getDefault().addNative(this.mClassLoader, extraLibPaths);
                    } finally {
                        StrictMode.setThreadPolicy(oldPolicy3);
                    }
                }
                if (list != null && addedPaths.size() > 0) {
                    ApplicationLoaders.getDefault().addPath(this.mClassLoader, TextUtils.join(File.pathSeparator, list));
                    needToSetupJitProfiles = true;
                }
                if (needToSetupJitProfiles && !ActivityThread.isSystem()) {
                    setupJitProfileSupport();
                }
                return;
            }
            if (this.mClassLoader == null) {
                ThreadPolicy oldPolicy4 = StrictMode.allowThreadDiskReads();
                this.mClassLoader = ApplicationLoaders.getDefault().getClassLoader("", this.mApplicationInfo.targetSdkVersion, isBundledApp2, librarySearchPath, libraryPermittedPath2, this.mBaseClassLoader, null);
                StrictMode.setThreadPolicy(oldPolicy4);
                this.mAppComponentFactory = AppComponentFactory.DEFAULT;
            }
        } else if (this.mClassLoader == null) {
            if (this.mBaseClassLoader != null) {
                this.mClassLoader = this.mBaseClassLoader;
            } else {
                this.mClassLoader = ClassLoader.getSystemClassLoader();
            }
            this.mAppComponentFactory = createAppFactory(this.mApplicationInfo, this.mClassLoader);
        }
    }

    public ClassLoader getClassLoader() {
        ClassLoader classLoader;
        synchronized (this) {
            if (this.mClassLoader == null) {
                createOrUpdateClassLoaderLocked(null);
            }
            classLoader = this.mClassLoader;
        }
        return classLoader;
    }

    private void setupJitProfileSupport() {
        if (SystemProperties.getBoolean("dalvik.vm.usejitprofiles", false) && this.mApplicationInfo.uid == Process.myUid()) {
            List<String> codePaths = new ArrayList();
            if ((this.mApplicationInfo.flags & 4) != 0) {
                codePaths.add(this.mApplicationInfo.sourceDir);
            }
            if (this.mApplicationInfo.splitSourceDirs != null) {
                Collections.addAll(codePaths, this.mApplicationInfo.splitSourceDirs);
            }
            if (!codePaths.isEmpty()) {
                int i = codePaths.size() - 1;
                while (i >= 0) {
                    VMRuntime.registerAppInfo(ArtManager.getCurrentProfilePath(this.mPackageName, UserHandle.myUserId(), i == 0 ? null : this.mApplicationInfo.splitNames[i - 1]), new String[]{(String) codePaths.get(i)});
                    i--;
                }
                DexLoadReporter.getInstance().registerAppDataDir(this.mPackageName, this.mDataDir);
            }
        }
    }

    private void initializeJavaContextClassLoader() {
        try {
            PackageInfo pi = HwFrameworkFactory.getHwApiCacheManagerEx().getPackageInfoAsUser(ActivityThread.getPackageManager(), this.mPackageName, 268435456, UserHandle.myUserId());
            if (pi == null) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unable to get package info for ");
                stringBuilder.append(this.mPackageName);
                stringBuilder.append("; is package not installed?");
                Slog.w(str, stringBuilder.toString());
                return;
            }
            ClassLoader contextClassLoader;
            boolean sharable = false;
            boolean sharedUserIdSet = pi.sharedUserId != null;
            boolean processNameNotDefault = (pi.applicationInfo == null || this.mPackageName.equals(pi.applicationInfo.processName)) ? false : true;
            if (sharedUserIdSet || processNameNotDefault) {
                sharable = true;
            }
            if (sharable) {
                contextClassLoader = new WarningContextClassLoader();
            } else {
                contextClassLoader = this.mClassLoader;
            }
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public String getAppDir() {
        return this.mAppDir;
    }

    public String getLibDir() {
        return this.mLibDir;
    }

    public String getResDir() {
        return this.mResDir;
    }

    public String[] getSplitAppDirs() {
        return this.mSplitAppDirs;
    }

    public String[] getSplitResDirs() {
        return this.mSplitResDirs;
    }

    public String[] getOverlayDirs() {
        return this.mOverlayDirs;
    }

    public String getDataDir() {
        return this.mDataDir;
    }

    public File getDataDirFile() {
        return this.mDataDirFile;
    }

    public File getDeviceProtectedDataDirFile() {
        return this.mDeviceProtectedDataDirFile;
    }

    public File getCredentialProtectedDataDirFile() {
        return this.mCredentialProtectedDataDirFile;
    }

    public AssetManager getAssets() {
        Resources resources = getResources();
        return resources != null ? resources.getAssets() : null;
    }

    public Resources getResources() {
        if (this.mResources == null) {
            try {
                String[] splitPaths = getSplitPaths(null);
                ResourcesManager.getInstance().setHwThemeType(this.mResDir, this.mApplicationInfo.hwThemeType);
                if (HwPCUtils.enabled() && ActivityThread.currentActivityThread() != null && HwPCUtils.isValidExtDisplayId(ActivityThread.currentActivityThread().mDisplayId)) {
                    this.mResources = ResourcesManager.getInstance().getResources(null, this.mResDir, splitPaths, this.mOverlayDirs, this.mApplicationInfo.sharedLibraryFiles, ActivityThread.currentActivityThread().mDisplayId, null, getCompatibilityInfo(), getClassLoader());
                } else {
                    this.mResources = ResourcesManager.getInstance().getResources(null, this.mResDir, splitPaths, this.mOverlayDirs, this.mApplicationInfo.sharedLibraryFiles, 0, null, getCompatibilityInfo(), getClassLoader());
                }
            } catch (NameNotFoundException e) {
                throw new AssertionError("null split not found");
            }
        }
        if (this.mResources != null) {
            this.mResources.getImpl().getHwResourcesImpl().setPackageName(getPackageName());
            this.mResources.setPackageName(getPackageName());
        }
        return this.mResources;
    }

    public void getHwReceiverResource() {
        if (this.mReceiverResource == null) {
            this.mReceiverResource = HwFrameworkFactory.getHwResource(12);
        }
    }

    public Application makeApplication(boolean forceDefaultAppClass, Instrumentation instrumentation) {
        StringBuilder stringBuilder;
        if (this.mApplication != null) {
            return this.mApplication;
        }
        Trace.traceBegin(64, "makeApplication");
        Application app = null;
        String appClass = this.mApplicationInfo.className;
        if (forceDefaultAppClass || appClass == null) {
            appClass = "android.app.Application";
        }
        try {
            ClassLoader cl = getClassLoader();
            if (!this.mPackageName.equals("android")) {
                Trace.traceBegin(64, "initializeJavaContextClassLoader");
                initializeJavaContextClassLoader();
                Trace.traceEnd(64);
            }
            ContextImpl appContext = ContextImpl.createAppContext(this.mActivityThread, this);
            app = this.mActivityThread.mInstrumentation.newApplication(cl, appClass, appContext);
            appContext.setOuterContext(app);
        } catch (Exception e) {
            if (!this.mActivityThread.mInstrumentation.onException(null, e)) {
                Trace.traceEnd(64);
                stringBuilder = new StringBuilder();
                stringBuilder.append("Unable to instantiate application ");
                stringBuilder.append(appClass);
                stringBuilder.append(": ");
                stringBuilder.append(e.toString());
                throw new RuntimeException(stringBuilder.toString(), e);
            }
        }
        this.mActivityThread.mAllApplications.add(app);
        this.mApplication = app;
        if (instrumentation != null) {
            try {
                instrumentation.callApplicationOnCreate(app);
            } catch (Exception e2) {
                if (!instrumentation.onException(app, e2)) {
                    Trace.traceEnd(64);
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Unable to create application ");
                    stringBuilder.append(app.getClass().getName());
                    stringBuilder.append(": ");
                    stringBuilder.append(e2.toString());
                    throw new RuntimeException(stringBuilder.toString(), e2);
                }
            }
        }
        Exception e22 = getAssets();
        if (e22 != null) {
            SparseArray<String> packageIdentifiers = e22.getAssignedPackageIdentifiers();
            int N = packageIdentifiers.size();
            for (int i = 0; i < N; i++) {
                int id = packageIdentifiers.keyAt(i);
                if (!(id == 1 || id == 127)) {
                    rewriteRValues(getClassLoader(), (String) packageIdentifiers.valueAt(i), id);
                }
            }
        }
        Trace.traceEnd(64);
        return app;
    }

    private void rewriteRValues(ClassLoader cl, String packageName, int id) {
        Throwable cause;
        StringBuilder stringBuilder;
        try {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(packageName);
            stringBuilder2.append(".R");
            try {
                try {
                    cl.loadClass(stringBuilder2.toString()).getMethod("onResourcesLoaded", new Class[]{Integer.TYPE}).invoke(null, new Object[]{Integer.valueOf(id)});
                } catch (IllegalAccessException e) {
                    cause = e;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Failed to rewrite resource references for ");
                    stringBuilder.append(packageName);
                    throw new RuntimeException(stringBuilder.toString(), cause);
                } catch (InvocationTargetException e2) {
                    cause = e2.getCause();
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Failed to rewrite resource references for ");
                    stringBuilder.append(packageName);
                    throw new RuntimeException(stringBuilder.toString(), cause);
                }
            } catch (NoSuchMethodException e3) {
            }
        } catch (ClassNotFoundException e4) {
            String str = TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("No resource references to update in package ");
            stringBuilder3.append(packageName);
            Log.i(str, stringBuilder3.toString());
        }
    }

    public void removeContextRegistrations(Context context, String who, String what) {
        int i;
        boolean reportRegistrationLeaks = StrictMode.vmRegistrationLeaksEnabled();
        synchronized (this.mReceivers) {
            ArrayMap<BroadcastReceiver, ReceiverDispatcher> rmap = (ArrayMap) this.mReceivers.remove(context);
            i = 0;
            if (rmap != null) {
                int i2 = 0;
                while (i2 < rmap.size()) {
                    ReceiverDispatcher rd = (ReceiverDispatcher) rmap.valueAt(i2);
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(what);
                    stringBuilder.append(" ");
                    stringBuilder.append(who);
                    stringBuilder.append(" has leaked IntentReceiver ");
                    stringBuilder.append(rd.getIntentReceiver());
                    stringBuilder.append(" that was originally registered here. Are you missing a call to unregisterReceiver()?");
                    IntentReceiverLeaked leak = new IntentReceiverLeaked(stringBuilder.toString());
                    leak.setStackTrace(rd.getLocation().getStackTrace());
                    Slog.e(ActivityThread.TAG, leak.getMessage(), leak);
                    if (reportRegistrationLeaks) {
                        StrictMode.onIntentReceiverLeaked(leak);
                    }
                    try {
                        ActivityManager.getService().unregisterReceiver(rd.getIIntentReceiver());
                        i2++;
                    } catch (RemoteException i3) {
                        throw i3.rethrowFromSystemServer();
                    }
                }
            }
            this.mUnregisteredReceivers.remove(context);
        }
        synchronized (this.mServices) {
            ArrayMap<ServiceConnection, ServiceDispatcher> smap = (ArrayMap) this.mServices.remove(context);
            if (smap != null) {
                while (i3 < smap.size()) {
                    ServiceDispatcher sd = (ServiceDispatcher) smap.valueAt(i3);
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(what);
                    stringBuilder2.append(" ");
                    stringBuilder2.append(who);
                    stringBuilder2.append(" has leaked ServiceConnection ");
                    stringBuilder2.append(sd.getServiceConnection());
                    stringBuilder2.append(" that was originally bound here");
                    ServiceConnectionLeaked leak2 = new ServiceConnectionLeaked(stringBuilder2.toString());
                    leak2.setStackTrace(sd.getLocation().getStackTrace());
                    Slog.e(ActivityThread.TAG, leak2.getMessage(), leak2);
                    if (reportRegistrationLeaks) {
                        StrictMode.onServiceConnectionLeaked(leak2);
                    }
                    try {
                        ActivityManager.getService().unbindService(sd.getIServiceConnection());
                        sd.doForget();
                        i3++;
                    } catch (RemoteException e) {
                        throw e.rethrowFromSystemServer();
                    }
                }
            }
            this.mUnboundServices.remove(context);
        }
    }

    private void checkRecevierRegisteredLeakLocked() {
        int count = 0;
        int contextNum = 0;
        for (Entry<Context, ArrayMap<BroadcastReceiver, ReceiverDispatcher>> entry : this.mReceivers.entrySet()) {
            count += ((ArrayMap) entry.getValue()).size();
            contextNum++;
        }
        if (2 == this.mReceiverResource.acquire(getApplicationInfo().uid, getPackageName(), (this.mApplicationInfo.flags & 1) != 0 ? 2 : 0, count)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(getPackageName());
            stringBuilder.append(" registered ");
            stringBuilder.append(count);
            stringBuilder.append(" Receivers  in ");
            stringBuilder.append(contextNum);
            stringBuilder.append(" Contexts");
            stringBuilder.append(10);
            stringBuilder.append(Log.getStackTraceString(new Throwable()));
            Log.e(TAG, stringBuilder.toString());
            throw new AssertionError("Register too many Broadcast Receivers");
        }
    }

    public IIntentReceiver getReceiverDispatcher(BroadcastReceiver r, Context context, Handler handler, Instrumentation instrumentation, boolean registered) {
        IIntentReceiver iIntentReceiver;
        synchronized (this.mReceivers) {
            ReceiverDispatcher rd = null;
            ArrayMap<BroadcastReceiver, ReceiverDispatcher> map = null;
            if (registered) {
                try {
                    map = (ArrayMap) this.mReceivers.get(context);
                    if (map != null) {
                        rd = (ReceiverDispatcher) map.get(r);
                    }
                } finally {
                }
            }
            if (rd == null) {
                rd = new ReceiverDispatcher(r, context, handler, instrumentation, registered);
                if (registered) {
                    if (map == null) {
                        map = new ArrayMap();
                        this.mReceivers.put(context, map);
                    }
                    map.put(r, rd);
                    if (this.mReceiverResource != null) {
                        checkRecevierRegisteredLeakLocked();
                    }
                }
            } else {
                rd.validate(context, handler);
            }
            rd.mForgotten = false;
            iIntentReceiver = rd.getIIntentReceiver();
        }
        return iIntentReceiver;
    }

    public IIntentReceiver forgetReceiverDispatcher(Context context, BroadcastReceiver r) {
        IIntentReceiver iIntentReceiver;
        synchronized (this.mReceivers) {
            ReceiverDispatcher rd;
            ArrayMap<BroadcastReceiver, ReceiverDispatcher> holder;
            RuntimeException ex;
            ArrayMap<BroadcastReceiver, ReceiverDispatcher> map = (ArrayMap) this.mReceivers.get(context);
            if (map != null) {
                rd = (ReceiverDispatcher) map.get(r);
                if (rd != null) {
                    map.remove(r);
                    if (map.size() == 0) {
                        this.mReceivers.remove(context);
                    }
                    if (r.getDebugUnregister()) {
                        holder = (ArrayMap) this.mUnregisteredReceivers.get(context);
                        if (holder == null) {
                            holder = new ArrayMap();
                            this.mUnregisteredReceivers.put(context, holder);
                        }
                        ex = new IllegalArgumentException("Originally unregistered here:");
                        ex.fillInStackTrace();
                        rd.setUnregisterLocation(ex);
                        holder.put(r, rd);
                    }
                    rd.mForgotten = true;
                    iIntentReceiver = rd.getIIntentReceiver();
                }
            }
            holder = (ArrayMap) this.mUnregisteredReceivers.get(context);
            if (holder != null) {
                rd = (ReceiverDispatcher) holder.get(r);
                if (rd != null) {
                    ex = rd.getUnregisterLocation();
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Unregistering Receiver ");
                    stringBuilder.append(r);
                    stringBuilder.append(" that was already unregistered");
                    throw new IllegalArgumentException(stringBuilder.toString(), ex);
                }
            }
            StringBuilder stringBuilder2;
            if (context == null) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Unbinding Receiver ");
                stringBuilder2.append(r);
                stringBuilder2.append(" from Context that is no longer in use: ");
                stringBuilder2.append(context);
                throw new IllegalStateException(stringBuilder2.toString());
            }
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Receiver not registered: ");
            stringBuilder2.append(r);
            throw new IllegalArgumentException(stringBuilder2.toString());
        }
        return iIntentReceiver;
    }

    public final IServiceConnection getServiceDispatcher(ServiceConnection c, Context context, Handler handler, int flags) {
        IServiceConnection iServiceConnection;
        synchronized (this.mServices) {
            ServiceDispatcher sd = null;
            ArrayMap<ServiceConnection, ServiceDispatcher> map = (ArrayMap) this.mServices.get(context);
            if (map != null) {
                sd = (ServiceDispatcher) map.get(c);
            }
            if (sd == null) {
                sd = new ServiceDispatcher(c, context, handler, flags);
                if (map == null) {
                    map = new ArrayMap();
                    this.mServices.put(context, map);
                }
                map.put(c, sd);
            } else {
                sd.validate(context, handler);
            }
            iServiceConnection = sd.getIServiceConnection();
        }
        return iServiceConnection;
    }

    public final IServiceConnection forgetServiceDispatcher(Context context, ServiceConnection c) {
        IServiceConnection iServiceConnection;
        synchronized (this.mServices) {
            ServiceDispatcher sd;
            ArrayMap<ServiceConnection, ServiceDispatcher> holder;
            RuntimeException ex;
            ArrayMap<ServiceConnection, ServiceDispatcher> map = (ArrayMap) this.mServices.get(context);
            if (map != null) {
                sd = (ServiceDispatcher) map.get(c);
                if (sd != null) {
                    map.remove(c);
                    sd.doForget();
                    if (map.size() == 0) {
                        this.mServices.remove(context);
                    }
                    if ((sd.getFlags() & 2) != 0) {
                        holder = (ArrayMap) this.mUnboundServices.get(context);
                        if (holder == null) {
                            holder = new ArrayMap();
                            this.mUnboundServices.put(context, holder);
                        }
                        ex = new IllegalArgumentException("Originally unbound here:");
                        ex.fillInStackTrace();
                        sd.setUnbindLocation(ex);
                        holder.put(c, sd);
                    }
                    iServiceConnection = sd.getIServiceConnection();
                }
            }
            holder = (ArrayMap) this.mUnboundServices.get(context);
            if (holder != null) {
                sd = (ServiceDispatcher) holder.get(c);
                if (sd != null) {
                    ex = sd.getUnbindLocation();
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Unbinding Service ");
                    stringBuilder.append(c);
                    stringBuilder.append(" that was already unbound");
                    throw new IllegalArgumentException(stringBuilder.toString(), ex);
                }
            }
            StringBuilder stringBuilder2;
            if (context == null) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Unbinding Service ");
                stringBuilder2.append(c);
                stringBuilder2.append(" from Context that is no longer in use: ");
                stringBuilder2.append(context);
                throw new IllegalStateException(stringBuilder2.toString());
            }
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Service not registered: ");
            stringBuilder2.append(c);
            throw new IllegalArgumentException(stringBuilder2.toString());
        }
        return iServiceConnection;
    }
}
