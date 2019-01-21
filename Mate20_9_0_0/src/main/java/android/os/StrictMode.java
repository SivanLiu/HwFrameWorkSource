package android.os;

import android.animation.ValueAnimator;
import android.app.ActivityManager;
import android.app.ActivityThread;
import android.app.IActivityManager;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.MessageQueue.IdleHandler;
import android.os.Parcelable.Creator;
import android.os.strictmode.CleartextNetworkViolation;
import android.os.strictmode.ContentUriWithoutPermissionViolation;
import android.os.strictmode.CustomViolation;
import android.os.strictmode.DiskReadViolation;
import android.os.strictmode.DiskWriteViolation;
import android.os.strictmode.FileUriExposedViolation;
import android.os.strictmode.InstanceCountViolation;
import android.os.strictmode.IntentReceiverLeakedViolation;
import android.os.strictmode.LeakedClosableViolation;
import android.os.strictmode.NetworkViolation;
import android.os.strictmode.NonSdkApiUsedViolation;
import android.os.strictmode.ResourceMismatchViolation;
import android.os.strictmode.ServiceConnectionLeakedViolation;
import android.os.strictmode.SqliteObjectLeakedViolation;
import android.os.strictmode.UnbufferedIoViolation;
import android.os.strictmode.UntaggedSocketViolation;
import android.os.strictmode.Violation;
import android.os.strictmode.WebViewMethodCalledOnWrongThreadViolation;
import android.rms.AppAssociate;
import android.service.notification.ZenModeConfig;
import android.telephony.SubscriptionPlan;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Printer;
import android.util.Singleton;
import android.view.IWindowManager;
import android.view.IWindowManager.Stub;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.os.BackgroundThread;
import com.android.internal.os.RuntimeInit;
import com.android.internal.util.FastPrintWriter;
import com.android.internal.util.HexDump;
import dalvik.system.BlockGuard;
import dalvik.system.BlockGuard.Policy;
import dalvik.system.CloseGuard;
import dalvik.system.CloseGuard.Reporter;
import dalvik.system.VMDebug;
import dalvik.system.VMRuntime;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public final class StrictMode {
    private static final int ALL_THREAD_DETECT_BITS = 63;
    private static final int ALL_VM_DETECT_BITS = -1073676544;
    public static final boolean ART_OPT_ENABLE = SystemProperties.getBoolean("persist.sys.art.opt.enable", false);
    public static final String CLEARTEXT_DETECTED_MSG = "Detected cleartext network traffic from UID ";
    private static final String CLEARTEXT_PROPERTY = "persist.sys.strictmode.clear";
    public static final int DETECT_CUSTOM = 8;
    public static final int DETECT_DISK_READ = 2;
    public static final int DETECT_DISK_WRITE = 1;
    public static final int DETECT_NETWORK = 4;
    public static final int DETECT_RESOURCE_MISMATCH = 16;
    public static final int DETECT_UNBUFFERED_IO = 32;
    public static final int DETECT_VM_ACTIVITY_LEAKS = 1024;
    public static final int DETECT_VM_CLEARTEXT_NETWORK = 16384;
    public static final int DETECT_VM_CLOSABLE_LEAKS = 512;
    public static final int DETECT_VM_CONTENT_URI_WITHOUT_PERMISSION = 32768;
    public static final int DETECT_VM_CURSOR_LEAKS = 256;
    public static final int DETECT_VM_FILE_URI_EXPOSURE = 8192;
    public static final int DETECT_VM_INSTANCE_LEAKS = 2048;
    public static final int DETECT_VM_NON_SDK_API_USAGE = 1073741824;
    public static final int DETECT_VM_REGISTRATION_LEAKS = 4096;
    public static final int DETECT_VM_UNTAGGED_SOCKET = Integer.MIN_VALUE;
    private static final boolean DISABLE = false;
    public static final String DISABLE_PROPERTY = "persist.sys.strictmode.disable";
    private static final HashMap<Class, Integer> EMPTY_CLASS_LIMIT_MAP = new HashMap();
    private static final ViolationLogger LOGCAT_LOGGER = -$$Lambda$StrictMode$1yH8AK0bTwVwZOb9x8HoiSBdzr0.INSTANCE;
    private static final boolean LOG_V = Log.isLoggable(TAG, 2);
    private static final int MAX_OFFENSES_PER_LOOP = 10;
    private static final int MAX_SPAN_TAGS = 20;
    private static final long MIN_DIALOG_INTERVAL_MS = 30000;
    private static final long MIN_LOG_INTERVAL_MS = 1000;
    private static final long MIN_VM_INTERVAL_MS = 1000;
    public static final int NETWORK_POLICY_ACCEPT = 0;
    public static final int NETWORK_POLICY_LOG = 1;
    public static final int NETWORK_POLICY_REJECT = 2;
    private static final Span NO_OP_SPAN = new Span() {
        public void finish() {
        }
    };
    public static final int PENALTY_DEATH = 262144;
    public static final int PENALTY_DEATH_ON_CLEARTEXT_NETWORK = 33554432;
    public static final int PENALTY_DEATH_ON_FILE_URI_EXPOSURE = 67108864;
    public static final int PENALTY_DEATH_ON_NETWORK = 16777216;
    public static final int PENALTY_DIALOG = 131072;
    public static final int PENALTY_DROPBOX = 2097152;
    public static final int PENALTY_FLASH = 1048576;
    public static final int PENALTY_GATHER = 4194304;
    public static final int PENALTY_LOG = 65536;
    private static final String TAG = "StrictMode";
    private static final ThreadLocal<AndroidBlockGuardPolicy> THREAD_ANDROID_POLICY = new ThreadLocal<AndroidBlockGuardPolicy>() {
        protected AndroidBlockGuardPolicy initialValue() {
            return new AndroidBlockGuardPolicy(0);
        }
    };
    private static final ThreadLocal<Handler> THREAD_HANDLER = new ThreadLocal<Handler>() {
        protected Handler initialValue() {
            return new Handler();
        }
    };
    private static final int THREAD_PENALTY_MASK = 24576000;
    public static final String VISUAL_PROPERTY = "persist.sys.strictmode.visual";
    private static final int VM_PENALTY_MASK = 103088128;
    private static final ThreadLocal<ArrayList<ViolationInfo>> gatheredViolations = new ThreadLocal<ArrayList<ViolationInfo>>() {
        protected ArrayList<ViolationInfo> initialValue() {
            return null;
        }
    };
    private static final AtomicInteger sDropboxCallsInFlight = new AtomicInteger(0);
    @GuardedBy("StrictMode.class")
    private static final HashMap<Class, Integer> sExpectedActivityInstanceCount = new HashMap();
    private static boolean sIsIdlerRegistered = false;
    private static long sLastInstanceCountCheckMillis = 0;
    private static final HashMap<Integer, Long> sLastVmViolationTime = new HashMap();
    private static volatile ViolationLogger sLogger = LOGCAT_LOGGER;
    private static final Consumer<String> sNonSdkApiUsageConsumer = -$$Lambda$StrictMode$lu9ekkHJ2HMz0jd3F8K8MnhenxQ.INSTANCE;
    private static final IdleHandler sProcessIdleHandler = new IdleHandler() {
        public boolean queueIdle() {
            long now = SystemClock.uptimeMillis();
            if (now - StrictMode.sLastInstanceCountCheckMillis > StrictMode.MIN_DIALOG_INTERVAL_MS) {
                StrictMode.sLastInstanceCountCheckMillis = now;
                StrictMode.conditionallyCheckInstanceCounts();
            }
            return true;
        }
    };
    private static final ThreadLocal<ThreadSpanState> sThisThreadSpanState = new ThreadLocal<ThreadSpanState>() {
        protected ThreadSpanState initialValue() {
            return new ThreadSpanState();
        }
    };
    private static final ThreadLocal<Executor> sThreadViolationExecutor = new ThreadLocal();
    private static final ThreadLocal<OnThreadViolationListener> sThreadViolationListener = new ThreadLocal();
    private static volatile VmPolicy sVmPolicy = VmPolicy.LAX;
    private static Singleton<IWindowManager> sWindowManager = new Singleton<IWindowManager>() {
        protected IWindowManager create() {
            return Stub.asInterface(ServiceManager.getService(AppAssociate.ASSOC_WINDOW));
        }
    };
    private static final ThreadLocal<ArrayList<ViolationInfo>> violationsBeingTimed = new ThreadLocal<ArrayList<ViolationInfo>>() {
        protected ArrayList<ViolationInfo> initialValue() {
            return new ArrayList();
        }
    };

    private static class AndroidBlockGuardPolicy implements Policy {
        private ArrayMap<Integer, Long> mLastViolationTime;
        private int mPolicyMask;

        public AndroidBlockGuardPolicy(int policyMask) {
            this.mPolicyMask = policyMask;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("AndroidBlockGuardPolicy; mPolicyMask=");
            stringBuilder.append(this.mPolicyMask);
            return stringBuilder.toString();
        }

        public int getPolicyMask() {
            return this.mPolicyMask;
        }

        public void onWriteToDisk() {
            if ((this.mPolicyMask & 1) != 0 && !StrictMode.tooManyViolationsThisLoop()) {
                startHandlingViolationException(new DiskWriteViolation());
            }
        }

        void onCustomSlowCall(String name) {
            if ((this.mPolicyMask & 8) != 0 && !StrictMode.tooManyViolationsThisLoop()) {
                startHandlingViolationException(new CustomViolation(name));
            }
        }

        void onResourceMismatch(Object tag) {
            if ((this.mPolicyMask & 16) != 0 && !StrictMode.tooManyViolationsThisLoop()) {
                startHandlingViolationException(new ResourceMismatchViolation(tag));
            }
        }

        public void onUnbufferedIO() {
            if ((this.mPolicyMask & 32) != 0 && !StrictMode.tooManyViolationsThisLoop()) {
                startHandlingViolationException(new UnbufferedIoViolation());
            }
        }

        public void onReadFromDisk() {
            if ((this.mPolicyMask & 2) != 0 && !StrictMode.tooManyViolationsThisLoop()) {
                startHandlingViolationException(new DiskReadViolation());
            }
        }

        public void onNetwork() {
            if ((this.mPolicyMask & 4) != 0) {
                if ((this.mPolicyMask & 16777216) != 0) {
                    throw new NetworkOnMainThreadException();
                } else if (!StrictMode.tooManyViolationsThisLoop()) {
                    startHandlingViolationException(new NetworkViolation());
                }
            }
        }

        public void setPolicyMask(int policyMask) {
            this.mPolicyMask = policyMask;
        }

        void startHandlingViolationException(Violation e) {
            ViolationInfo info = new ViolationInfo(e, this.mPolicyMask);
            info.violationUptimeMillis = SystemClock.uptimeMillis();
            handleViolationWithTimingAttempt(info);
        }

        void handleViolationWithTimingAttempt(ViolationInfo info) {
            if (Looper.myLooper() == null || (info.mPolicy & StrictMode.THREAD_PENALTY_MASK) == 262144) {
                info.durationMillis = -1;
                onThreadPolicyViolation(info);
                return;
            }
            ArrayList<ViolationInfo> records = (ArrayList) StrictMode.violationsBeingTimed.get();
            if (records.size() < 10) {
                records.add(info);
                if (records.size() <= 1) {
                    IWindowManager windowManager = info.penaltyEnabled(1048576) ? (IWindowManager) StrictMode.sWindowManager.get() : null;
                    boolean isVisual = SystemProperties.getBoolean(StrictMode.VISUAL_PROPERTY, false);
                    if (windowManager != null && isVisual) {
                        try {
                            windowManager.showStrictModeViolation(true);
                        } catch (RemoteException e) {
                            Log.e(StrictMode.TAG, "RemoteException: windowManager.showStrictModeViolation");
                        }
                    }
                    ((Handler) StrictMode.THREAD_HANDLER.get()).postAtFrontOfQueue(new -$$Lambda$StrictMode$AndroidBlockGuardPolicy$Mxbi12aLrPMWhtfmockn9dQK-dQ(this, windowManager, isVisual, records));
                }
            }
        }

        public static /* synthetic */ void lambda$handleViolationWithTimingAttempt$0(AndroidBlockGuardPolicy androidBlockGuardPolicy, IWindowManager windowManager, boolean isVisual, ArrayList records) {
            long loopFinishTime = SystemClock.uptimeMillis();
            int n = 0;
            if (windowManager != null && isVisual) {
                try {
                    windowManager.showStrictModeViolation(false);
                } catch (RemoteException e) {
                    Log.e(StrictMode.TAG, "RemoteException: windowManager.showStrictModeViolation");
                }
            }
            while (n < records.size()) {
                ViolationInfo v = (ViolationInfo) records.get(n);
                v.violationNumThisLoop = n + 1;
                v.durationMillis = (int) (loopFinishTime - v.violationUptimeMillis);
                androidBlockGuardPolicy.onThreadPolicyViolation(v);
                n++;
            }
            records.clear();
        }

        void onThreadPolicyViolation(ViolationInfo info) {
            ViolationInfo violationInfo = info;
            if (StrictMode.LOG_V) {
                String str = StrictMode.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onThreadPolicyViolation; policy=");
                stringBuilder.append(info.mPolicy);
                Log.d(str, stringBuilder.toString());
            }
            boolean justDropBox = true;
            if (violationInfo.penaltyEnabled(4194304)) {
                ArrayList<ViolationInfo> violations = (ArrayList) StrictMode.gatheredViolations.get();
                if (violations == null) {
                    violations = new ArrayList(1);
                    StrictMode.gatheredViolations.set(violations);
                }
                Iterator it = violations.iterator();
                while (it.hasNext()) {
                    if (info.getStackTrace().equals(((ViolationInfo) it.next()).getStackTrace())) {
                        return;
                    }
                }
                violations.add(violationInfo);
                return;
            }
            Integer crashFingerprint = Integer.valueOf(info.hashCode());
            long lastViolationTime = 0;
            if (this.mLastViolationTime != null) {
                Long vtime = (Long) this.mLastViolationTime.get(crashFingerprint);
                if (vtime != null) {
                    lastViolationTime = vtime.longValue();
                }
            } else {
                this.mLastViolationTime = new ArrayMap(1);
            }
            long now = SystemClock.uptimeMillis();
            this.mLastViolationTime.put(crashFingerprint, Long.valueOf(now));
            long timeSinceLastViolationMillis = lastViolationTime == 0 ? SubscriptionPlan.BYTES_UNLIMITED : now - lastViolationTime;
            if (violationInfo.penaltyEnabled(65536) && timeSinceLastViolationMillis > 1000) {
                StrictMode.sLogger.log(violationInfo);
            }
            Violation violation = info.mViolation;
            int violationMaskSubset = 0;
            if (violationInfo.penaltyEnabled(131072) && timeSinceLastViolationMillis > StrictMode.MIN_DIALOG_INTERVAL_MS) {
                violationMaskSubset = 0 | 131072;
            }
            if (violationInfo.penaltyEnabled(2097152) && lastViolationTime == 0) {
                violationMaskSubset |= 2097152;
            }
            if (violationMaskSubset != 0) {
                violationMaskSubset |= info.getViolationBit();
                if ((info.mPolicy & StrictMode.THREAD_PENALTY_MASK) != 2097152) {
                    justDropBox = false;
                }
                if (justDropBox) {
                    StrictMode.dropboxViolationAsync(violationMaskSubset, violationInfo);
                } else {
                    StrictMode.handleApplicationStrictModeViolation(violationMaskSubset, violationInfo);
                }
            }
            if ((info.getPolicyMask() & 262144) == 0) {
                OnThreadViolationListener listener = (OnThreadViolationListener) StrictMode.sThreadViolationListener.get();
                Executor executor = (Executor) StrictMode.sThreadViolationExecutor.get();
                if (!(listener == null || executor == null)) {
                    try {
                        executor.execute(new -$$Lambda$StrictMode$AndroidBlockGuardPolicy$FxZGA9KtfTewqdcxlUwvIe5Nx9I(listener, violation));
                    } catch (RejectedExecutionException e) {
                        Log.e(StrictMode.TAG, "ThreadPolicy penaltyCallback failed", e);
                    }
                }
                return;
            }
            throw new RuntimeException("StrictMode ThreadPolicy violation", violation);
        }

        static /* synthetic */ void lambda$onThreadPolicyViolation$1(OnThreadViolationListener listener, Violation violation) {
            ThreadPolicy oldPolicy = StrictMode.allowThreadViolations();
            try {
                listener.onThreadViolation(violation);
            } finally {
                StrictMode.setThreadPolicy(oldPolicy);
            }
        }
    }

    private static class AndroidCloseGuardReporter implements Reporter {
        private AndroidCloseGuardReporter() {
        }

        /* synthetic */ AndroidCloseGuardReporter(AnonymousClass1 x0) {
            this();
        }

        public void report(String message, Throwable allocationSite) {
            StrictMode.onVmPolicyViolation(new LeakedClosableViolation(message, allocationSite));
        }
    }

    private static final class InstanceTracker {
        private static final HashMap<Class<?>, Integer> sInstanceCounts = new HashMap();
        private final Class<?> mKlass;

        public InstanceTracker(Object instance) {
            this.mKlass = instance.getClass();
            synchronized (sInstanceCounts) {
                Integer value = (Integer) sInstanceCounts.get(this.mKlass);
                int newValue = 1;
                if (value != null) {
                    newValue = 1 + value.intValue();
                }
                sInstanceCounts.put(this.mKlass, Integer.valueOf(newValue));
            }
        }

        protected void finalize() throws Throwable {
            try {
                Integer value;
                int newValue;
                synchronized (sInstanceCounts) {
                    value = (Integer) sInstanceCounts.get(this.mKlass);
                    if (value != null) {
                        newValue = value.intValue() - 1;
                        if (newValue > 0) {
                            sInstanceCounts.put(this.mKlass, Integer.valueOf(newValue));
                        } else {
                            sInstanceCounts.remove(this.mKlass);
                        }
                    }
                }
                if (StrictMode.ART_OPT_ENABLE) {
                    synchronized (StrictMode.class) {
                        value = (Integer) StrictMode.sExpectedActivityInstanceCount.get(this.mKlass);
                        if (value != null) {
                            newValue = value.intValue() - 1;
                            if (newValue > 0) {
                                StrictMode.sExpectedActivityInstanceCount.put(this.mKlass, Integer.valueOf(newValue));
                            } else {
                                StrictMode.sExpectedActivityInstanceCount.remove(this.mKlass);
                            }
                        }
                    }
                }
                super.finalize();
            } catch (Throwable th) {
                super.finalize();
            }
        }

        public static int getInstanceCount(Class<?> klass) {
            int intValue;
            synchronized (sInstanceCounts) {
                Integer value = (Integer) sInstanceCounts.get(klass);
                intValue = value != null ? value.intValue() : 0;
            }
            return intValue;
        }
    }

    public interface OnThreadViolationListener {
        void onThreadViolation(Violation violation);
    }

    public interface OnVmViolationListener {
        void onVmViolation(Violation violation);
    }

    public static class Span {
        private final ThreadSpanState mContainerState;
        private long mCreateMillis;
        private String mName;
        private Span mNext;
        private Span mPrev;

        Span(ThreadSpanState threadState) {
            this.mContainerState = threadState;
        }

        protected Span() {
            this.mContainerState = null;
        }

        /* JADX WARNING: Missing block: B:23:0x0070, code skipped:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void finish() {
            ThreadSpanState state = this.mContainerState;
            synchronized (state) {
                if (this.mName == null) {
                    return;
                }
                if (this.mPrev != null) {
                    this.mPrev.mNext = this.mNext;
                }
                if (this.mNext != null) {
                    this.mNext.mPrev = this.mPrev;
                }
                if (state.mActiveHead == this) {
                    state.mActiveHead = this.mNext;
                }
                state.mActiveSize--;
                if (StrictMode.LOG_V) {
                    String str = StrictMode.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Span finished=");
                    stringBuilder.append(this.mName);
                    stringBuilder.append("; size=");
                    stringBuilder.append(state.mActiveSize);
                    Log.d(str, stringBuilder.toString());
                }
                this.mCreateMillis = -1;
                this.mName = null;
                this.mPrev = null;
                this.mNext = null;
                if (state.mFreeListSize < 5) {
                    this.mNext = state.mFreeListHead;
                    state.mFreeListHead = this;
                    state.mFreeListSize++;
                }
            }
        }
    }

    public static final class ThreadPolicy {
        public static final ThreadPolicy LAX = new ThreadPolicy(0, null, null);
        final Executor mCallbackExecutor;
        final OnThreadViolationListener mListener;
        final int mask;

        public static final class Builder {
            private Executor mExecutor;
            private OnThreadViolationListener mListener;
            private int mMask;

            public Builder() {
                this.mMask = 0;
                this.mMask = 0;
            }

            public Builder(ThreadPolicy policy) {
                this.mMask = 0;
                this.mMask = policy.mask;
                this.mListener = policy.mListener;
                this.mExecutor = policy.mCallbackExecutor;
            }

            public Builder detectAll() {
                detectDiskReads();
                detectDiskWrites();
                detectNetwork();
                int targetSdk = VMRuntime.getRuntime().getTargetSdkVersion();
                if (targetSdk >= 11) {
                    detectCustomSlowCalls();
                }
                if (targetSdk >= 23) {
                    detectResourceMismatches();
                }
                if (targetSdk >= 26) {
                    detectUnbufferedIo();
                }
                return this;
            }

            public Builder permitAll() {
                return disable(63);
            }

            public Builder detectNetwork() {
                return enable(4);
            }

            public Builder permitNetwork() {
                return disable(4);
            }

            public Builder detectDiskReads() {
                return enable(2);
            }

            public Builder permitDiskReads() {
                return disable(2);
            }

            public Builder detectCustomSlowCalls() {
                return enable(8);
            }

            public Builder permitCustomSlowCalls() {
                return disable(8);
            }

            public Builder permitResourceMismatches() {
                return disable(16);
            }

            public Builder detectUnbufferedIo() {
                return enable(32);
            }

            public Builder permitUnbufferedIo() {
                return disable(32);
            }

            public Builder detectResourceMismatches() {
                return enable(16);
            }

            public Builder detectDiskWrites() {
                return enable(1);
            }

            public Builder permitDiskWrites() {
                return disable(1);
            }

            public Builder penaltyDialog() {
                return enable(131072);
            }

            public Builder penaltyDeath() {
                return enable(262144);
            }

            public Builder penaltyDeathOnNetwork() {
                return enable(16777216);
            }

            public Builder penaltyFlashScreen() {
                return enable(1048576);
            }

            public Builder penaltyLog() {
                return enable(65536);
            }

            public Builder penaltyDropBox() {
                return enable(2097152);
            }

            public Builder penaltyListener(Executor executor, OnThreadViolationListener listener) {
                if (executor != null) {
                    this.mListener = listener;
                    this.mExecutor = executor;
                    return this;
                }
                throw new NullPointerException("executor must not be null");
            }

            public Builder penaltyListener(OnThreadViolationListener listener, Executor executor) {
                return penaltyListener(executor, listener);
            }

            private Builder enable(int bit) {
                this.mMask |= bit;
                return this;
            }

            private Builder disable(int bit) {
                this.mMask &= ~bit;
                return this;
            }

            public ThreadPolicy build() {
                if (this.mListener == null && this.mMask != 0 && (this.mMask & 2555904) == 0) {
                    penaltyLog();
                }
                return new ThreadPolicy(this.mMask, this.mListener, this.mExecutor, null);
            }
        }

        /* synthetic */ ThreadPolicy(int x0, OnThreadViolationListener x1, Executor x2, AnonymousClass1 x3) {
            this(x0, x1, x2);
        }

        private ThreadPolicy(int mask, OnThreadViolationListener listener, Executor executor) {
            this.mask = mask;
            this.mListener = listener;
            this.mCallbackExecutor = executor;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[StrictMode.ThreadPolicy; mask=");
            stringBuilder.append(this.mask);
            stringBuilder.append("]");
            return stringBuilder.toString();
        }
    }

    private static class ThreadSpanState {
        public Span mActiveHead;
        public int mActiveSize;
        public Span mFreeListHead;
        public int mFreeListSize;

        private ThreadSpanState() {
        }

        /* synthetic */ ThreadSpanState(AnonymousClass1 x0) {
            this();
        }
    }

    public interface ViolationLogger {
        void log(ViolationInfo violationInfo);
    }

    public static final class VmPolicy {
        public static final VmPolicy LAX = new VmPolicy(0, StrictMode.EMPTY_CLASS_LIMIT_MAP, null, null);
        final HashMap<Class, Integer> classInstanceLimit;
        final Executor mCallbackExecutor;
        final OnVmViolationListener mListener;
        final int mask;

        public static final class Builder {
            private HashMap<Class, Integer> mClassInstanceLimit;
            private boolean mClassInstanceLimitNeedCow;
            private Executor mExecutor;
            private OnVmViolationListener mListener;
            private int mMask;

            public Builder() {
                this.mClassInstanceLimitNeedCow = false;
                this.mMask = 0;
            }

            public Builder(VmPolicy base) {
                this.mClassInstanceLimitNeedCow = false;
                this.mMask = base.mask;
                this.mClassInstanceLimitNeedCow = true;
                this.mClassInstanceLimit = base.classInstanceLimit;
                this.mListener = base.mListener;
                this.mExecutor = base.mCallbackExecutor;
            }

            public Builder setClassInstanceLimit(Class klass, int instanceLimit) {
                if (klass != null) {
                    if (this.mClassInstanceLimitNeedCow) {
                        if (this.mClassInstanceLimit.containsKey(klass) && ((Integer) this.mClassInstanceLimit.get(klass)).intValue() == instanceLimit) {
                            return this;
                        }
                        this.mClassInstanceLimitNeedCow = false;
                        this.mClassInstanceLimit = (HashMap) this.mClassInstanceLimit.clone();
                    } else if (this.mClassInstanceLimit == null) {
                        this.mClassInstanceLimit = new HashMap();
                    }
                    this.mMask |= 2048;
                    this.mClassInstanceLimit.put(klass, Integer.valueOf(instanceLimit));
                    return this;
                }
                throw new NullPointerException("klass == null");
            }

            public Builder detectActivityLeaks() {
                return enable(1024);
            }

            public Builder permitActivityLeaks() {
                return disable(1024);
            }

            public Builder detectNonSdkApiUsage() {
                return enable(1073741824);
            }

            public Builder permitNonSdkApiUsage() {
                return disable(1073741824);
            }

            public Builder detectAll() {
                detectLeakedSqlLiteObjects();
                int targetSdk = VMRuntime.getRuntime().getTargetSdkVersion();
                if (targetSdk >= 11) {
                    detectActivityLeaks();
                    detectLeakedClosableObjects();
                }
                if (targetSdk >= 16) {
                    detectLeakedRegistrationObjects();
                }
                if (targetSdk >= 18) {
                    detectFileUriExposure();
                }
                if (targetSdk >= 23 && SystemProperties.getBoolean(StrictMode.CLEARTEXT_PROPERTY, false)) {
                    detectCleartextNetwork();
                }
                if (targetSdk >= 26) {
                    detectContentUriWithoutPermission();
                    detectUntaggedSockets();
                }
                return this;
            }

            public Builder detectLeakedSqlLiteObjects() {
                return enable(256);
            }

            public Builder detectLeakedClosableObjects() {
                return enable(512);
            }

            public Builder detectLeakedRegistrationObjects() {
                return enable(4096);
            }

            public Builder detectFileUriExposure() {
                return enable(8192);
            }

            public Builder detectCleartextNetwork() {
                return enable(16384);
            }

            public Builder detectContentUriWithoutPermission() {
                return enable(32768);
            }

            public Builder detectUntaggedSockets() {
                return enable(Integer.MIN_VALUE);
            }

            public Builder permitUntaggedSockets() {
                return disable(Integer.MIN_VALUE);
            }

            public Builder penaltyDeath() {
                return enable(262144);
            }

            public Builder penaltyDeathOnCleartextNetwork() {
                return enable(33554432);
            }

            public Builder penaltyDeathOnFileUriExposure() {
                return enable(67108864);
            }

            public Builder penaltyLog() {
                return enable(65536);
            }

            public Builder penaltyDropBox() {
                return enable(2097152);
            }

            public Builder penaltyListener(Executor executor, OnVmViolationListener listener) {
                if (executor != null) {
                    this.mListener = listener;
                    this.mExecutor = executor;
                    return this;
                }
                throw new NullPointerException("executor must not be null");
            }

            public Builder penaltyListener(OnVmViolationListener listener, Executor executor) {
                return penaltyListener(executor, listener);
            }

            private Builder enable(int bit) {
                this.mMask |= bit;
                return this;
            }

            Builder disable(int bit) {
                this.mMask &= ~bit;
                return this;
            }

            public VmPolicy build() {
                if (this.mListener == null && this.mMask != 0 && (this.mMask & 2555904) == 0) {
                    penaltyLog();
                }
                return new VmPolicy(this.mMask, this.mClassInstanceLimit != null ? this.mClassInstanceLimit : StrictMode.EMPTY_CLASS_LIMIT_MAP, this.mListener, this.mExecutor, null);
            }
        }

        /* synthetic */ VmPolicy(int x0, HashMap x1, OnVmViolationListener x2, Executor x3, AnonymousClass1 x4) {
            this(x0, x1, x2, x3);
        }

        private VmPolicy(int mask, HashMap<Class, Integer> classInstanceLimit, OnVmViolationListener listener, Executor executor) {
            if (classInstanceLimit != null) {
                this.mask = mask;
                this.classInstanceLimit = classInstanceLimit;
                this.mListener = listener;
                this.mCallbackExecutor = executor;
                return;
            }
            throw new NullPointerException("classInstanceLimit == null");
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[StrictMode.VmPolicy; mask=");
            stringBuilder.append(this.mask);
            stringBuilder.append("]");
            return stringBuilder.toString();
        }
    }

    public static final class ViolationInfo implements Parcelable {
        public static final Creator<ViolationInfo> CREATOR = new Creator<ViolationInfo>() {
            public ViolationInfo createFromParcel(Parcel in) {
                return new ViolationInfo(in);
            }

            public ViolationInfo[] newArray(int size) {
                return new ViolationInfo[size];
            }
        };
        public String broadcastIntentAction;
        public int durationMillis;
        private final Deque<StackTraceElement[]> mBinderStack;
        private final int mPolicy;
        private String mStackTrace;
        private final Violation mViolation;
        public int numAnimationsRunning;
        public long numInstances;
        public String[] tags;
        public int violationNumThisLoop;
        public long violationUptimeMillis;

        ViolationInfo(Violation tr, int policy) {
            this.mBinderStack = new ArrayDeque();
            this.durationMillis = -1;
            int index = 0;
            this.numAnimationsRunning = 0;
            this.numInstances = -1;
            this.mViolation = tr;
            this.mPolicy = policy;
            this.violationUptimeMillis = SystemClock.uptimeMillis();
            this.numAnimationsRunning = ValueAnimator.getCurrentAnimationsCount();
            Intent broadcastIntent = ActivityThread.getIntentBeingBroadcast();
            if (broadcastIntent != null) {
                this.broadcastIntentAction = broadcastIntent.getAction();
            }
            ThreadSpanState state = (ThreadSpanState) StrictMode.sThisThreadSpanState.get();
            if (tr instanceof InstanceCountViolation) {
                this.numInstances = ((InstanceCountViolation) tr).getNumberOfInstances();
            }
            synchronized (state) {
                int spanActiveCount = state.mActiveSize;
                if (spanActiveCount > 20) {
                    spanActiveCount = 20;
                }
                if (spanActiveCount != 0) {
                    this.tags = new String[spanActiveCount];
                    for (Span iter = state.mActiveHead; iter != null && index < spanActiveCount; iter = iter.mNext) {
                        this.tags[index] = iter.mName;
                        index++;
                    }
                }
            }
        }

        public String getStackTrace() {
            if (this.mStackTrace == null) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new FastPrintWriter(sw, false, 256);
                this.mViolation.printStackTrace(pw);
                for (StackTraceElement[] traces : this.mBinderStack) {
                    pw.append("# via Binder call with stack:\n");
                    for (StackTraceElement traceElement : traces) {
                        pw.append("\tat ");
                        pw.append(traceElement.toString());
                        pw.append(10);
                    }
                }
                pw.flush();
                pw.close();
                this.mStackTrace = sw.toString();
            }
            return this.mStackTrace;
        }

        public String getViolationDetails() {
            return this.mViolation.getMessage();
        }

        public int getPolicyMask() {
            return this.mPolicy;
        }

        boolean penaltyEnabled(int p) {
            return (this.mPolicy & p) != 0;
        }

        void addLocalStack(Throwable t) {
            this.mBinderStack.addFirst(t.getStackTrace());
        }

        public int getViolationBit() {
            if (this.mViolation instanceof DiskWriteViolation) {
                return 1;
            }
            if (this.mViolation instanceof DiskReadViolation) {
                return 2;
            }
            if (this.mViolation instanceof NetworkViolation) {
                return 4;
            }
            if (this.mViolation instanceof CustomViolation) {
                return 8;
            }
            if (this.mViolation instanceof ResourceMismatchViolation) {
                return 16;
            }
            if (this.mViolation instanceof UnbufferedIoViolation) {
                return 32;
            }
            if (this.mViolation instanceof SqliteObjectLeakedViolation) {
                return 256;
            }
            if (this.mViolation instanceof LeakedClosableViolation) {
                return 512;
            }
            if (this.mViolation instanceof InstanceCountViolation) {
                return 2048;
            }
            if ((this.mViolation instanceof IntentReceiverLeakedViolation) || (this.mViolation instanceof ServiceConnectionLeakedViolation)) {
                return 4096;
            }
            if (this.mViolation instanceof FileUriExposedViolation) {
                return 8192;
            }
            if (this.mViolation instanceof CleartextNetworkViolation) {
                return 16384;
            }
            if (this.mViolation instanceof ContentUriWithoutPermissionViolation) {
                return 32768;
            }
            if (this.mViolation instanceof UntaggedSocketViolation) {
                return Integer.MIN_VALUE;
            }
            if (this.mViolation instanceof NonSdkApiUsedViolation) {
                return 1073741824;
            }
            throw new IllegalStateException("missing violation bit");
        }

        public int hashCode() {
            int result = 17;
            if (this.mViolation != null) {
                result = (37 * 17) + this.mViolation.hashCode();
            }
            if (this.numAnimationsRunning != 0) {
                result *= 37;
            }
            if (this.broadcastIntentAction != null) {
                result = (37 * result) + this.broadcastIntentAction.hashCode();
            }
            if (this.tags != null) {
                for (String tag : this.tags) {
                    result = (37 * result) + tag.hashCode();
                }
            }
            return result;
        }

        public ViolationInfo(Parcel in) {
            this(in, false);
        }

        public ViolationInfo(Parcel in, boolean unsetGatheringBit) {
            this.mBinderStack = new ArrayDeque();
            this.durationMillis = -1;
            this.numAnimationsRunning = 0;
            this.numInstances = -1;
            this.mViolation = (Violation) in.readSerializable();
            int binderStackSize = in.readInt();
            for (int i = 0; i < binderStackSize; i++) {
                StackTraceElement[] traceElements = new StackTraceElement[in.readInt()];
                for (int j = 0; j < traceElements.length; j++) {
                    traceElements[j] = new StackTraceElement(in.readString(), in.readString(), in.readString(), in.readInt());
                }
                this.mBinderStack.add(traceElements);
            }
            int rawPolicy = in.readInt();
            if (unsetGatheringBit) {
                this.mPolicy = -4194305 & rawPolicy;
            } else {
                this.mPolicy = rawPolicy;
            }
            this.durationMillis = in.readInt();
            this.violationNumThisLoop = in.readInt();
            this.numAnimationsRunning = in.readInt();
            this.violationUptimeMillis = in.readLong();
            this.numInstances = in.readLong();
            this.broadcastIntentAction = in.readString();
            this.tags = in.readStringArray();
        }

        public void writeToParcel(Parcel dest, int flags) {
            dest.writeSerializable(this.mViolation);
            dest.writeInt(this.mBinderStack.size());
            for (StackTraceElement[] traceElements : this.mBinderStack) {
                dest.writeInt(traceElements.length);
                for (StackTraceElement element : traceElements) {
                    dest.writeString(element.getClassName());
                    dest.writeString(element.getMethodName());
                    dest.writeString(element.getFileName());
                    dest.writeInt(element.getLineNumber());
                }
            }
            int start = dest.dataPosition();
            dest.writeInt(this.mPolicy);
            dest.writeInt(this.durationMillis);
            dest.writeInt(this.violationNumThisLoop);
            dest.writeInt(this.numAnimationsRunning);
            dest.writeLong(this.violationUptimeMillis);
            dest.writeLong(this.numInstances);
            dest.writeString(this.broadcastIntentAction);
            dest.writeStringArray(this.tags);
            int total = dest.dataPosition() - start;
        }

        public void dump(Printer pw, String prefix) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("stackTrace: ");
            stringBuilder.append(getStackTrace());
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("policy: ");
            stringBuilder.append(this.mPolicy);
            pw.println(stringBuilder.toString());
            if (this.durationMillis != -1) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(prefix);
                stringBuilder.append("durationMillis: ");
                stringBuilder.append(this.durationMillis);
                pw.println(stringBuilder.toString());
            }
            if (this.numInstances != -1) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(prefix);
                stringBuilder.append("numInstances: ");
                stringBuilder.append(this.numInstances);
                pw.println(stringBuilder.toString());
            }
            if (this.violationNumThisLoop != 0) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(prefix);
                stringBuilder.append("violationNumThisLoop: ");
                stringBuilder.append(this.violationNumThisLoop);
                pw.println(stringBuilder.toString());
            }
            if (this.numAnimationsRunning != 0) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(prefix);
                stringBuilder.append("numAnimationsRunning: ");
                stringBuilder.append(this.numAnimationsRunning);
                pw.println(stringBuilder.toString());
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("violationUptimeMillis: ");
            stringBuilder.append(this.violationUptimeMillis);
            pw.println(stringBuilder.toString());
            if (this.broadcastIntentAction != null) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(prefix);
                stringBuilder.append("broadcastIntentAction: ");
                stringBuilder.append(this.broadcastIntentAction);
                pw.println(stringBuilder.toString());
            }
            if (this.tags != null) {
                int index = 0;
                String[] strArr = this.tags;
                int length = strArr.length;
                int i = 0;
                while (i < length) {
                    String tag = strArr[i];
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(prefix);
                    stringBuilder2.append("tag[");
                    int index2 = index + 1;
                    stringBuilder2.append(index);
                    stringBuilder2.append("]: ");
                    stringBuilder2.append(tag);
                    pw.println(stringBuilder2.toString());
                    i++;
                    index = index2;
                }
            }
        }

        public int describeContents() {
            return 0;
        }
    }

    static /* synthetic */ void lambda$static$0(ViolationInfo info) {
        String msg;
        if (info.durationMillis != -1) {
            msg = new StringBuilder();
            msg.append("StrictMode policy violation; ~duration=");
            msg.append(info.durationMillis);
            msg.append(" ms:");
            msg = msg.toString();
        } else {
            msg = "StrictMode policy violation:";
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(msg);
        stringBuilder.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
        stringBuilder.append(info.getStackTrace());
        Log.d(str, stringBuilder.toString());
    }

    public static void setViolationLogger(ViolationLogger listener) {
        if (listener == null) {
            listener = LOGCAT_LOGGER;
        }
        sLogger = listener;
    }

    private StrictMode() {
    }

    public static void setThreadPolicy(ThreadPolicy policy) {
        setThreadPolicyMask(policy.mask);
        sThreadViolationListener.set(policy.mListener);
        sThreadViolationExecutor.set(policy.mCallbackExecutor);
    }

    public static void setThreadPolicyMask(int policyMask) {
        setBlockGuardPolicy(policyMask);
        Binder.setThreadStrictModePolicy(policyMask);
    }

    private static void setBlockGuardPolicy(int policyMask) {
        if (policyMask == 0) {
            BlockGuard.setThreadPolicy(BlockGuard.LAX_POLICY);
            return;
        }
        AndroidBlockGuardPolicy androidPolicy;
        Policy policy = BlockGuard.getThreadPolicy();
        if (policy instanceof AndroidBlockGuardPolicy) {
            androidPolicy = (AndroidBlockGuardPolicy) policy;
        } else {
            androidPolicy = (AndroidBlockGuardPolicy) THREAD_ANDROID_POLICY.get();
            BlockGuard.setThreadPolicy(androidPolicy);
        }
        androidPolicy.setPolicyMask(policyMask);
    }

    private static void setCloseGuardEnabled(boolean enabled) {
        if (!(CloseGuard.getReporter() instanceof AndroidCloseGuardReporter)) {
            CloseGuard.setReporter(new AndroidCloseGuardReporter());
        }
        CloseGuard.setEnabled(enabled);
    }

    public static int getThreadPolicyMask() {
        return BlockGuard.getThreadPolicy().getPolicyMask();
    }

    public static ThreadPolicy getThreadPolicy() {
        return new ThreadPolicy(getThreadPolicyMask(), (OnThreadViolationListener) sThreadViolationListener.get(), (Executor) sThreadViolationExecutor.get(), null);
    }

    public static ThreadPolicy allowThreadDiskWrites() {
        return new ThreadPolicy(allowThreadDiskWritesMask(), (OnThreadViolationListener) sThreadViolationListener.get(), (Executor) sThreadViolationExecutor.get(), null);
    }

    public static int allowThreadDiskWritesMask() {
        int oldPolicyMask = getThreadPolicyMask();
        int newPolicyMask = oldPolicyMask & -4;
        if (newPolicyMask != oldPolicyMask) {
            setThreadPolicyMask(newPolicyMask);
        }
        return oldPolicyMask;
    }

    public static ThreadPolicy allowThreadDiskReads() {
        return new ThreadPolicy(allowThreadDiskReadsMask(), (OnThreadViolationListener) sThreadViolationListener.get(), (Executor) sThreadViolationExecutor.get(), null);
    }

    public static int allowThreadDiskReadsMask() {
        int oldPolicyMask = getThreadPolicyMask();
        int newPolicyMask = oldPolicyMask & -3;
        if (newPolicyMask != oldPolicyMask) {
            setThreadPolicyMask(newPolicyMask);
        }
        return oldPolicyMask;
    }

    private static ThreadPolicy allowThreadViolations() {
        ThreadPolicy oldPolicy = getThreadPolicy();
        setThreadPolicyMask(0);
        return oldPolicy;
    }

    private static VmPolicy allowVmViolations() {
        VmPolicy oldPolicy = getVmPolicy();
        sVmPolicy = VmPolicy.LAX;
        return oldPolicy;
    }

    /* JADX WARNING: Missing block: B:20:0x004e, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static boolean isBundledSystemApp(ApplicationInfo ai) {
        if (ai == null || ai.packageName == null) {
            return true;
        }
        if (!ai.isSystemApp() || ai.packageName.equals("com.android.vending") || ai.packageName.equals("com.android.chrome") || ai.packageName.equals("com.android.phone")) {
            return false;
        }
        if (ai.packageName.equals(ZenModeConfig.SYSTEM_AUTHORITY) || ai.packageName.startsWith("android.") || ai.packageName.startsWith("com.android.")) {
            return true;
        }
        return false;
    }

    public static void initThreadDefaults(ApplicationInfo ai) {
        Builder builder = new Builder();
        if ((ai != null ? ai.targetSdkVersion : 10000) >= 11) {
            builder.detectNetwork();
            builder.penaltyDeathOnNetwork();
        }
        if (!(Build.IS_USER || SystemProperties.getBoolean(DISABLE_PROPERTY, false))) {
            if (Build.IS_USERDEBUG) {
                if (isBundledSystemApp(ai)) {
                    builder.detectAll();
                    builder.penaltyDropBox();
                    if (SystemProperties.getBoolean(VISUAL_PROPERTY, false)) {
                        builder.penaltyFlashScreen();
                    }
                }
            } else if (Build.IS_ENG && isBundledSystemApp(ai)) {
                builder.detectAll();
                builder.penaltyDropBox();
                builder.penaltyLog();
                builder.penaltyFlashScreen();
            }
        }
        setThreadPolicy(builder.build());
    }

    public static void initVmDefaults(ApplicationInfo ai) {
        Builder builder = new Builder();
        if ((ai != null ? ai.targetSdkVersion : 10000) >= 24) {
            builder.detectFileUriExposure();
            builder.penaltyDeathOnFileUriExposure();
        }
        if (!(Build.IS_USER || SystemProperties.getBoolean(DISABLE_PROPERTY, false))) {
            if (Build.IS_USERDEBUG) {
                if (isBundledSystemApp(ai)) {
                    builder.detectAll();
                    builder.permitActivityLeaks();
                    builder.penaltyDropBox();
                }
            } else if (Build.IS_ENG && isBundledSystemApp(ai)) {
                builder.detectAll();
                builder.penaltyDropBox();
                builder.penaltyLog();
            }
        }
        setVmPolicy(builder.build());
    }

    public static void enableDeathOnFileUriExposure() {
        sVmPolicy = new VmPolicy(67108864 | (sVmPolicy.mask | 8192), sVmPolicy.classInstanceLimit, sVmPolicy.mListener, sVmPolicy.mCallbackExecutor, null);
    }

    public static void disableDeathOnFileUriExposure() {
        sVmPolicy = new VmPolicy(-67117057 & sVmPolicy.mask, sVmPolicy.classInstanceLimit, sVmPolicy.mListener, sVmPolicy.mCallbackExecutor, null);
    }

    private static int parsePolicyFromMessage(String message) {
        if (message == null || !message.startsWith("policy=")) {
            return 0;
        }
        int spaceIndex = message.indexOf(32);
        if (spaceIndex == -1) {
            return 0;
        }
        try {
            return Integer.parseInt(message.substring(7, spaceIndex));
        } catch (NumberFormatException e) {
            Log.e(TAG, "NumberFormatException: Integer.parseInt");
            return 0;
        }
    }

    private static boolean tooManyViolationsThisLoop() {
        return ((ArrayList) violationsBeingTimed.get()).size() >= 10;
    }

    private static void dropboxViolationAsync(int violationMaskSubset, ViolationInfo info) {
        int outstanding = sDropboxCallsInFlight.incrementAndGet();
        if (outstanding > 20) {
            sDropboxCallsInFlight.decrementAndGet();
            return;
        }
        if (LOG_V) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Dropboxing async; in-flight=");
            stringBuilder.append(outstanding);
            Log.d(str, stringBuilder.toString());
        }
        BackgroundThread.getHandler().post(new -$$Lambda$StrictMode$yZJXPvy2veRNA-xL_SWdXzX_OLg(violationMaskSubset, info));
    }

    static /* synthetic */ void lambda$dropboxViolationAsync$2(int violationMaskSubset, ViolationInfo info) {
        handleApplicationStrictModeViolation(violationMaskSubset, info);
        int outstandingInner = sDropboxCallsInFlight.decrementAndGet();
        if (LOG_V) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Dropbox complete; in-flight=");
            stringBuilder.append(outstandingInner);
            Log.d(str, stringBuilder.toString());
        }
    }

    private static void handleApplicationStrictModeViolation(int violationMaskSubset, ViolationInfo info) {
        int oldMask = getThreadPolicyMask();
        try {
            setThreadPolicyMask(0);
            IActivityManager am = ActivityManager.getService();
            if (am == null) {
                Log.w(TAG, "No activity manager; failed to Dropbox violation.");
            } else {
                am.handleApplicationStrictModeViolation(RuntimeInit.getApplicationObject(), violationMaskSubset, info);
            }
        } catch (RemoteException e) {
            if (!(e instanceof DeadObjectException)) {
                Log.e(TAG, "RemoteException handling StrictMode violation", e);
            }
        } catch (Throwable th) {
            setThreadPolicyMask(oldMask);
        }
        setThreadPolicyMask(oldMask);
    }

    static boolean hasGatheredViolations() {
        return gatheredViolations.get() != null;
    }

    static void clearGatheredViolations() {
        gatheredViolations.set(null);
    }

    public static void conditionallyCheckInstanceCounts() {
        VmPolicy policy = getVmPolicy();
        int policySize = policy.classInstanceLimit.size();
        if (policySize != 0) {
            System.gc();
            System.runFinalization();
            System.gc();
            Class[] classes = (Class[]) policy.classInstanceLimit.keySet().toArray(new Class[policySize]);
            int i = 0;
            long[] instanceCounts = VMDebug.countInstancesOfClasses(classes, false);
            while (i < classes.length) {
                Class klass = classes[i];
                int limit = ((Integer) policy.classInstanceLimit.get(klass)).intValue();
                long instances = instanceCounts[i];
                if (instances > ((long) limit)) {
                    onVmPolicyViolation(new InstanceCountViolation(klass, instances, limit));
                }
                i++;
            }
        }
    }

    public static void setVmPolicy(VmPolicy policy) {
        synchronized (StrictMode.class) {
            sVmPolicy = policy;
            setCloseGuardEnabled(vmClosableObjectLeaksEnabled());
            Looper looper = Looper.getMainLooper();
            if (looper != null) {
                MessageQueue mq = looper.mQueue;
                if (policy.classInstanceLimit.size() != 0) {
                    if ((sVmPolicy.mask & VM_PENALTY_MASK) != 0) {
                        if (!sIsIdlerRegistered) {
                            mq.addIdleHandler(sProcessIdleHandler);
                            sIsIdlerRegistered = true;
                        }
                    }
                }
                mq.removeIdleHandler(sProcessIdleHandler);
                sIsIdlerRegistered = false;
            }
            int networkPolicy = 0;
            if ((sVmPolicy.mask & 16384) != 0) {
                if ((sVmPolicy.mask & 262144) == 0) {
                    if ((sVmPolicy.mask & 33554432) == 0) {
                        networkPolicy = 1;
                    }
                }
                networkPolicy = 2;
            }
            INetworkManagementService netd = INetworkManagementService.Stub.asInterface(ServiceManager.getService("network_management"));
            if (netd != null) {
                try {
                    netd.setUidCleartextNetworkPolicy(Process.myUid(), networkPolicy);
                } catch (RemoteException e) {
                    Log.e(TAG, "RemoteException: netd.setUidCleartextNetworkPolicy");
                }
            } else if (networkPolicy != 0) {
                Log.w(TAG, "Dropping requested network policy due to missing service!");
            }
            if ((sVmPolicy.mask & 1073741824) != 0) {
                VMRuntime.setNonSdkApiUsageConsumer(sNonSdkApiUsageConsumer);
                VMRuntime.setDedupeHiddenApiWarnings(false);
            } else {
                VMRuntime.setNonSdkApiUsageConsumer(null);
                VMRuntime.setDedupeHiddenApiWarnings(true);
            }
        }
    }

    public static VmPolicy getVmPolicy() {
        VmPolicy vmPolicy;
        synchronized (StrictMode.class) {
            vmPolicy = sVmPolicy;
        }
        return vmPolicy;
    }

    public static void enableDefaults() {
        setThreadPolicy(new Builder().detectAll().penaltyLog().build());
        setVmPolicy(new Builder().detectAll().penaltyLog().build());
    }

    public static boolean vmSqliteObjectLeaksEnabled() {
        return (sVmPolicy.mask & 256) != 0;
    }

    public static boolean vmClosableObjectLeaksEnabled() {
        return (sVmPolicy.mask & 512) != 0;
    }

    public static boolean vmRegistrationLeaksEnabled() {
        return (sVmPolicy.mask & 4096) != 0;
    }

    public static boolean vmFileUriExposureEnabled() {
        return (sVmPolicy.mask & 8192) != 0;
    }

    public static boolean vmCleartextNetworkEnabled() {
        return (sVmPolicy.mask & 16384) != 0;
    }

    public static boolean vmContentUriWithoutPermissionEnabled() {
        return (sVmPolicy.mask & 32768) != 0;
    }

    public static boolean vmUntaggedSocketEnabled() {
        return (sVmPolicy.mask & Integer.MIN_VALUE) != 0;
    }

    public static void onSqliteObjectLeaked(String message, Throwable originStack) {
        onVmPolicyViolation(new SqliteObjectLeakedViolation(message, originStack));
    }

    public static void onWebViewMethodCalledOnWrongThread(Throwable originStack) {
        onVmPolicyViolation(new WebViewMethodCalledOnWrongThreadViolation(originStack));
    }

    public static void onIntentReceiverLeaked(Throwable originStack) {
        onVmPolicyViolation(new IntentReceiverLeakedViolation(originStack));
    }

    public static void onServiceConnectionLeaked(Throwable originStack) {
        onVmPolicyViolation(new ServiceConnectionLeakedViolation(originStack));
    }

    public static void onFileUriExposed(Uri uri, String location) {
        String message = new StringBuilder();
        message.append(uri);
        message.append(" exposed beyond app through ");
        message.append(location);
        message = message.toString();
        if ((sVmPolicy.mask & 67108864) == 0) {
            onVmPolicyViolation(new FileUriExposedViolation(message));
            return;
        }
        throw new FileUriExposedException(message);
    }

    public static void onContentUriWithoutPermission(Uri uri, String location) {
        onVmPolicyViolation(new ContentUriWithoutPermissionViolation(uri, location));
    }

    public static void onCleartextNetworkDetected(byte[] firstPacket) {
        StringBuilder stringBuilder;
        byte[] rawAddr = null;
        boolean forceDeath = false;
        if (firstPacket != null) {
            if (firstPacket.length >= 20 && (firstPacket[0] & 240) == 64) {
                rawAddr = new byte[4];
                System.arraycopy(firstPacket, 16, rawAddr, 0, 4);
            } else if (firstPacket.length >= 40 && (firstPacket[0] & 240) == 96) {
                rawAddr = new byte[16];
                System.arraycopy(firstPacket, 24, rawAddr, 0, 16);
            }
        }
        int uid = Process.myUid();
        String msg = new StringBuilder();
        msg.append(CLEARTEXT_DETECTED_MSG);
        msg.append(uid);
        msg = msg.toString();
        if (rawAddr != null) {
            try {
                stringBuilder = new StringBuilder();
                stringBuilder.append(msg);
                stringBuilder.append(" to ");
                stringBuilder.append(InetAddress.getByAddress(rawAddr));
                msg = stringBuilder.toString();
            } catch (UnknownHostException e) {
                String str = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("UnknownHostException: ");
                stringBuilder2.append(msg);
                Log.e(str, stringBuilder2.toString());
            }
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append(msg);
        stringBuilder.append(HexDump.dumpHexString(firstPacket).trim());
        stringBuilder.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
        msg = stringBuilder.toString();
        if ((sVmPolicy.mask & 33554432) != 0) {
            forceDeath = true;
        }
        onVmPolicyViolation(new CleartextNetworkViolation(msg), forceDeath);
    }

    public static void onUntaggedSocket() {
        onVmPolicyViolation(new UntaggedSocketViolation());
    }

    public static void onVmPolicyViolation(Violation originStack) {
        onVmPolicyViolation(originStack, false);
    }

    public static void onVmPolicyViolation(Violation violation, boolean forceDeath) {
        Violation violation2 = violation;
        boolean penaltyLog = true;
        boolean penaltyDropbox = (sVmPolicy.mask & 2097152) != 0;
        boolean z = (sVmPolicy.mask & 262144) != 0 || forceDeath;
        boolean penaltyDeath = z;
        if ((sVmPolicy.mask & 65536) == 0) {
            penaltyLog = false;
        }
        ViolationInfo info = new ViolationInfo(violation2, sVmPolicy.mask);
        info.numAnimationsRunning = 0;
        info.tags = null;
        info.broadcastIntentAction = null;
        Integer fingerprint = Integer.valueOf(info.hashCode());
        long now = SystemClock.uptimeMillis();
        long timeSinceLastViolationMillis = SubscriptionPlan.BYTES_UNLIMITED;
        synchronized (sLastVmViolationTime) {
            if (sLastVmViolationTime.containsKey(fingerprint)) {
                timeSinceLastViolationMillis = now - ((Long) sLastVmViolationTime.get(fingerprint)).longValue();
            }
            if (timeSinceLastViolationMillis > 1000) {
                sLastVmViolationTime.put(fingerprint, Long.valueOf(now));
            }
        }
        if (timeSinceLastViolationMillis > 1000) {
            if (penaltyLog && sLogger != null && timeSinceLastViolationMillis > 1000) {
                sLogger.log(info);
            }
            int violationMaskSubset = 2097152 | (ALL_VM_DETECT_BITS & sVmPolicy.mask);
            if (penaltyDropbox) {
                if (penaltyDeath) {
                    handleApplicationStrictModeViolation(violationMaskSubset, info);
                } else {
                    dropboxViolationAsync(violationMaskSubset, info);
                }
            }
            if (penaltyDeath) {
                System.err.println("StrictMode VmPolicy violation with POLICY_DEATH; shutting down.");
                Process.killProcess(Process.myPid());
                System.exit(10);
            }
            if (!(sVmPolicy.mListener == null || sVmPolicy.mCallbackExecutor == null)) {
                try {
                    sVmPolicy.mCallbackExecutor.execute(new -$$Lambda$StrictMode$UFC_nI1x6u8ZwMQmA7bmj9NHZz4(sVmPolicy.mListener, violation2));
                } catch (RejectedExecutionException e) {
                    Log.e(TAG, "VmPolicy penaltyCallback failed", e);
                }
            }
        }
    }

    static /* synthetic */ void lambda$onVmPolicyViolation$3(OnVmViolationListener listener, Violation violation) {
        VmPolicy oldPolicy = allowVmViolations();
        try {
            listener.onVmViolation(violation);
        } finally {
            setVmPolicy(oldPolicy);
        }
    }

    static void writeGatheredViolationsToParcel(Parcel p) {
        ArrayList<ViolationInfo> violations = (ArrayList) gatheredViolations.get();
        if (violations == null) {
            p.writeInt(0);
        } else {
            int size = Math.min(violations.size(), 3);
            p.writeInt(size);
            for (int i = 0; i < size; i++) {
                ((ViolationInfo) violations.get(i)).writeToParcel(p, 0);
            }
        }
        gatheredViolations.set(null);
    }

    static void readAndHandleBinderCallViolations(Parcel p) {
        Throwable localCallSite = new Throwable();
        boolean currentlyGathering = (4194304 & getThreadPolicyMask()) != 0;
        int size = p.readInt();
        for (int i = 0; i < size; i++) {
            ViolationInfo info = new ViolationInfo(p, !currentlyGathering);
            info.addLocalStack(localCallSite);
            Policy policy = BlockGuard.getThreadPolicy();
            if (policy instanceof AndroidBlockGuardPolicy) {
                ((AndroidBlockGuardPolicy) policy).handleViolationWithTimingAttempt(info);
            }
        }
    }

    private static void onBinderStrictModePolicyChange(int newPolicy) {
        setBlockGuardPolicy(newPolicy);
    }

    public static Span enterCriticalSpan(String name) {
        if (Build.IS_USER) {
            return NO_OP_SPAN;
        }
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("name must be non-null and non-empty");
        }
        Span span;
        ThreadSpanState state = (ThreadSpanState) sThisThreadSpanState.get();
        synchronized (state) {
            if (state.mFreeListHead != null) {
                span = state.mFreeListHead;
                state.mFreeListHead = span.mNext;
                state.mFreeListSize--;
            } else {
                span = new Span(state);
            }
            span.mName = name;
            span.mCreateMillis = SystemClock.uptimeMillis();
            span.mNext = state.mActiveHead;
            span.mPrev = null;
            state.mActiveHead = span;
            state.mActiveSize++;
            if (span.mNext != null) {
                span.mNext.mPrev = span;
            }
            if (LOG_V) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Span enter=");
                stringBuilder.append(name);
                stringBuilder.append("; size=");
                stringBuilder.append(state.mActiveSize);
                Log.d(str, stringBuilder.toString());
            }
        }
        return span;
    }

    public static void noteSlowCall(String name) {
        Policy policy = BlockGuard.getThreadPolicy();
        if (policy instanceof AndroidBlockGuardPolicy) {
            ((AndroidBlockGuardPolicy) policy).onCustomSlowCall(name);
        }
    }

    public static void noteResourceMismatch(Object tag) {
        Policy policy = BlockGuard.getThreadPolicy();
        if (policy instanceof AndroidBlockGuardPolicy) {
            ((AndroidBlockGuardPolicy) policy).onResourceMismatch(tag);
        }
    }

    public static void noteUnbufferedIO() {
        Policy policy = BlockGuard.getThreadPolicy();
        if (policy instanceof AndroidBlockGuardPolicy) {
            policy.onUnbufferedIO();
        }
    }

    public static void noteDiskRead() {
        Policy policy = BlockGuard.getThreadPolicy();
        if (policy instanceof AndroidBlockGuardPolicy) {
            policy.onReadFromDisk();
        }
    }

    public static void noteDiskWrite() {
        Policy policy = BlockGuard.getThreadPolicy();
        if (policy instanceof AndroidBlockGuardPolicy) {
            policy.onWriteToDisk();
        }
    }

    public static Object trackActivity(Object instance) {
        return new InstanceTracker(instance);
    }

    public static void incrementExpectedActivityCount(Class klass) {
        if (klass != null) {
            synchronized (StrictMode.class) {
                if ((sVmPolicy.mask & 1024) == 0) {
                    return;
                }
                Integer expected = (Integer) sExpectedActivityInstanceCount.get(klass);
                Integer newExpected = true;
                if (expected != null) {
                    newExpected = 1 + expected.intValue();
                }
                sExpectedActivityInstanceCount.put(klass, Integer.valueOf(newExpected));
            }
        }
    }

    /* JADX WARNING: Missing block: B:30:0x0055, code skipped:
            r0 = android.os.StrictMode.InstanceTracker.getInstanceCount(r7);
     */
    /* JADX WARNING: Missing block: B:31:0x005b, code skipped:
            if (ART_OPT_ENABLE == false) goto L_0x0064;
     */
    /* JADX WARNING: Missing block: B:32:0x005d, code skipped:
            if (r0 > r2) goto L_0x0067;
     */
    /* JADX WARNING: Missing block: B:34:0x0061, code skipped:
            if (r0 > 10) goto L_0x0067;
     */
    /* JADX WARNING: Missing block: B:35:0x0063, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:36:0x0064, code skipped:
            if (r0 > r2) goto L_0x0067;
     */
    /* JADX WARNING: Missing block: B:37:0x0066, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:38:0x0067, code skipped:
            java.lang.System.gc();
            java.lang.System.runFinalization();
            java.lang.System.gc();
            r3 = dalvik.system.VMDebug.countInstancesOfClass(r7, false);
     */
    /* JADX WARNING: Missing block: B:39:0x0077, code skipped:
            if (r3 <= ((long) r2)) goto L_0x0081;
     */
    /* JADX WARNING: Missing block: B:40:0x0079, code skipped:
            onVmPolicyViolation(new android.os.strictmode.InstanceCountViolation(r7, r3, r2));
     */
    /* JADX WARNING: Missing block: B:41:0x0081, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static void decrementExpectedActivityCount(Class klass) {
        if (klass != null) {
            synchronized (StrictMode.class) {
                if ((sVmPolicy.mask & 1024) == 0) {
                    return;
                }
                int limit;
                Integer expected = (Integer) sExpectedActivityInstanceCount.get(klass);
                int intValue;
                if (ART_OPT_ENABLE) {
                    if (expected != null) {
                        if (expected.intValue() != 0) {
                            intValue = expected.intValue();
                            limit = intValue;
                        }
                    }
                    intValue = 0;
                    limit = intValue;
                } else {
                    if (expected != null) {
                        if (expected.intValue() != 0) {
                            intValue = expected.intValue() - 1;
                            limit = intValue;
                        }
                    }
                    intValue = 0;
                    limit = intValue;
                }
                if (limit == 0) {
                    sExpectedActivityInstanceCount.remove(klass);
                } else {
                    sExpectedActivityInstanceCount.put(klass, Integer.valueOf(limit));
                }
                limit++;
            }
        }
    }
}
