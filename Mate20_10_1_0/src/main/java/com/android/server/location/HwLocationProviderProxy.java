package com.android.server.location;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.Signature;
import android.location.Location;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.WorkSource;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.content.PackageMonitor;
import com.android.internal.location.ILocationProvider;
import com.android.internal.location.ILocationProviderManager;
import com.android.internal.location.ProviderProperties;
import com.android.internal.location.ProviderRequest;
import com.android.internal.os.BackgroundThread;
import com.android.server.LocationManagerService;
import com.android.server.ServiceWatcher;
import com.android.server.ServiceWatcherUtils;
import com.android.server.location.AbstractLocationProvider;
import com.android.server.rms.iaware.memory.utils.BigMemoryConstant;
import com.android.server.wifipro.WifiProCommonUtils;
import com.huawei.utils.reflect.EasyInvokeFactory;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class HwLocationProviderProxy extends LocationProviderProxy implements HwLocationProxy, IHwLocationProviderInterface {
    /* access modifiers changed from: private */
    public static final String AB_NLP_PKNAME = SystemProperties.get("ro.config.hw_nlp", "com.huawei.lbs");
    private static final int CHECK_DELAY_TIME = 10000;
    private static final boolean D = LocationManagerService.D;
    private static final String GOOGLE_NLP_PKNAME = "com.google.android.gms";
    private static final int NLP_AB = 2;
    private static final int NLP_GOOGLE = 1;
    private static final int NLP_NONE = 0;
    private static final long RETENTION_PERIOD = 3600000;
    private static final String TAG = "HwLocationProviderProxy";
    private static ServiceWatcherUtils mServiceWatcherUtils = EasyInvokeFactory.getInvokeUtils(ServiceWatcherUtils.class);
    /* access modifiers changed from: private */
    public boolean isDualNlpAlive;
    private int mABNlpPid = 0;
    private Timer mCheckTimer;
    private final Context mContext;
    private boolean mEnabled = false;
    private final ILocationProviderManager.Stub mGoogleManager = new ILocationProviderManager.Stub() {
        /* class com.android.server.location.HwLocationProviderProxy.AnonymousClass2 */

        public void onSetEnabled(boolean enabled) {
            if (!HwMultiNlpPolicy.isChineseVersion() || HwLocationProviderProxy.this.mHwLbsConfigManager.isEnableForParam(LbsConfigContent.CONFIG_GMS_NLP_ENABLE)) {
                HwLocationProviderProxy.this.setEnabled(enabled);
            }
        }

        public void onSetProperties(ProviderProperties properties) {
            HwLocationProviderProxy.this.setProperties(properties);
        }

        public void onReportLocation(Location location) {
            HwLocationProviderProxy.this.reportLocationFilter(location);
        }

        public void onSetAdditionalProviderPackages(List<String> packageNames) {
            HwLocationProviderProxy.this.onSetAdditionalProviderPackages(packageNames);
        }
    };
    private int mGoogleNlpPid = 0;
    /* access modifiers changed from: private */
    public HwLbsConfigManager mHwLbsConfigManager;
    private HwNLPCache mHwNLPCache;
    private long mLastCheckTime = 0;
    private LocationManager mLocationManager;
    /* access modifiers changed from: private */
    public boolean mLocationMonitoring = false;
    private boolean mLocationSuccessAB = false;
    private boolean mLocationSuccessGoogle = false;
    /* access modifiers changed from: private */
    public final Object mLock = new Object();
    private final ILocationProviderManager.Stub mManager = new ILocationProviderManager.Stub() {
        /* class com.android.server.location.HwLocationProviderProxy.AnonymousClass1 */

        public void onSetEnabled(boolean enabled) {
            HwLocationProviderProxy.this.setEnabled(enabled);
        }

        public void onSetProperties(ProviderProperties properties) {
            HwLocationProviderProxy.this.setProperties(properties);
        }

        public void onReportLocation(Location location) {
            HwLocationProviderProxy.this.reportLocationFilter(location);
        }

        public void onSetAdditionalProviderPackages(List<String> packageNames) {
            HwLocationProviderProxy.this.onSetAdditionalProviderPackages(packageNames);
        }
    };
    private int mNlpUsed = 0;
    private boolean mPMAbRegisted = false;
    private boolean mPMGoogleRegisted = false;
    private final PackageMonitor mPackageMonitorAb = new PackageMonitor() {
        /* class com.android.server.location.HwLocationProviderProxy.AnonymousClass6 */

        public boolean onPackageChanged(String packageName, int uid, String[] components) {
            synchronized (HwLocationProviderProxy.this.mLock) {
                if (packageName.equals(HwLocationProviderProxy.AB_NLP_PKNAME)) {
                    boolean z = true;
                    LBSLog.i(HwLocationProviderProxy.TAG, false, "rebind onPackageChanged %{public}s", packageName);
                    if (!HwLocationProviderProxy.this.mStartAB && HwLocationProviderProxy.this.mServiceWatcherAb != null) {
                        boolean unused = HwLocationProviderProxy.this.mStartAB = HwLocationProviderProxy.this.mServiceWatcherAb.start();
                        HwLocationProviderProxy hwLocationProviderProxy = HwLocationProviderProxy.this;
                        if (!HwLocationProviderProxy.this.mStartGoogle || !HwLocationProviderProxy.this.mStartAB) {
                            z = false;
                        }
                        boolean unused2 = hwLocationProviderProxy.isDualNlpAlive = z;
                    }
                }
            }
            return HwLocationProviderProxy.super.onPackageChanged(packageName, uid, components);
        }
    };
    private final PackageMonitor mPackageMonitorGoogle = new PackageMonitor() {
        /* class com.android.server.location.HwLocationProviderProxy.AnonymousClass5 */

        public boolean onPackageChanged(String packageName, int uid, String[] components) {
            synchronized (HwLocationProviderProxy.this.mLock) {
                if (packageName.equals("com.google.android.gms")) {
                    boolean z = true;
                    LBSLog.i(HwLocationProviderProxy.TAG, false, "rebind onPackageChanged %{public}s", packageName);
                    if (!HwLocationProviderProxy.this.mStartGoogle && HwLocationProviderProxy.this.mServiceWatcherGoogle != null) {
                        boolean unused = HwLocationProviderProxy.this.mStartGoogle = HwLocationProviderProxy.this.mServiceWatcherGoogle.start();
                        HwLocationProviderProxy hwLocationProviderProxy = HwLocationProviderProxy.this;
                        if (!HwLocationProviderProxy.this.mStartGoogle || !HwLocationProviderProxy.this.mStartAB) {
                            z = false;
                        }
                        boolean unused2 = hwLocationProviderProxy.isDualNlpAlive = z;
                    }
                }
            }
            return HwLocationProviderProxy.super.onPackageChanged(packageName, uid, components);
        }
    };
    private PhoneStateListener mPhoneStateListener;
    @GuardedBy({"mRequestLock"})
    private ProviderRequest mRequest;
    private final Object mRequestLock = new Object();
    private ProviderRequest mRequestOff = new ProviderRequest();
    /* access modifiers changed from: private */
    public final ServiceWatcher mServiceWatcherAb;
    private ServiceWatcher mServiceWatcherDefault;
    /* access modifiers changed from: private */
    public final ServiceWatcher mServiceWatcherGoogle;
    /* access modifiers changed from: private */
    public boolean mStartAB = false;
    /* access modifiers changed from: private */
    public boolean mStartGoogle = false;
    private TelephonyManager mTelephonyManager;
    @GuardedBy({"mRequestLock"})
    private WorkSource mWorkSource;
    private WorkSource mWorksourceOff = new WorkSource();

    /* access modifiers changed from: private */
    public void reportLocationFilter(Location location) {
        if (reportNLPLocation(Binder.getCallingPid())) {
            if (!this.mHwNLPCache.isJumpingPoint(location)) {
                reportLocation(location);
            }
            this.mHwNLPCache.onLocationChange(location);
        }
    }

    public static HwLocationProviderProxy createAndBind(Context context, AbstractLocationProvider.LocationProviderManager locationProviderManager, String action, int overlaySwitchResId, int defaultServicePackageNameResId, int initialPackageNamesResId) {
        HwLocationProviderProxy proxy = new HwLocationProviderProxy(context, locationProviderManager, action, overlaySwitchResId, defaultServicePackageNameResId, initialPackageNamesResId);
        if (proxy.bind()) {
            return proxy;
        }
        return null;
    }

    public HwLocationProviderProxy(Context context, AbstractLocationProvider.LocationProviderManager locationProviderManager, String action, int overlaySwitchResId, int defaultServicePackageNameResId, int initialPackageNamesResId) {
        super(context, locationProviderManager);
        this.mContext = context;
        this.mRequestOff.interval = 86400000;
        this.mServiceWatcherGoogle = serviceWatcherCreate(1, action, overlaySwitchResId, defaultServicePackageNameResId, initialPackageNamesResId, LocationHandlerEx.getInstance());
        this.mServiceWatcherAb = serviceWatcherCreate(2, action, overlaySwitchResId, defaultServicePackageNameResId, initialPackageNamesResId, LocationHandlerEx.getInstance());
        this.isDualNlpAlive = false;
        this.mServiceWatcherDefault = this.mServiceWatcherGoogle;
        this.mRequest = null;
        this.mWorkSource = new WorkSource();
        this.mHwNLPCache = HwNLPCache.getInstance(this.mContext, locationProviderManager);
        HwMultiNlpPolicy.getDefault(context).setHwLocationProviderProxy(this);
        HwMultiNlpPolicy.getDefault(context).initHwNLPVowifi(BackgroundThread.getHandler());
        this.mLocationManager = (LocationManager) this.mContext.getSystemService("location");
        this.mHwLbsConfigManager = HwLbsConfigManager.getInstance(context);
    }

    private ServiceWatcher serviceWatcherCreate(final int nlp, String action, int overlaySwitchResId, int defaultServicePackageNameResId, int initialPackageNamesResId, Handler handler) {
        String servicePackageName;
        ArrayList<String> initialPackageNames = new ArrayList<>();
        if (nlp == 1) {
            servicePackageName = "com.google.android.gms";
        } else {
            servicePackageName = AB_NLP_PKNAME;
        }
        initialPackageNames.add(servicePackageName);
        ArrayList<HashSet<Signature>> signatureSets = ServiceWatcher.getSignatureSets(this.mContext, (String[]) initialPackageNames.toArray(new String[initialPackageNames.size()]));
        ServiceWatcher serviceWatcher = new ServiceWatcher(this.mContext, TAG, action, overlaySwitchResId, defaultServicePackageNameResId, initialPackageNamesResId, handler) {
            /* class com.android.server.location.HwLocationProviderProxy.AnonymousClass3 */

            /* access modifiers changed from: protected */
            public void onBind() {
                if (nlp == 1) {
                    runOnBinder(new ServiceWatcher.BinderRunner() {
                        /* class com.android.server.location.$$Lambda$HwLocationProviderProxy$3$n5HF8AiOQ2dECDWKMLkoX7zjP4 */

                        public final void run(IBinder iBinder) {
                            HwLocationProviderProxy.this.initializeServiceGoogle(iBinder);
                        }
                    });
                } else {
                    runOnBinder(new ServiceWatcher.BinderRunner() {
                        /* class com.android.server.location.$$Lambda$HwLocationProviderProxy$3$e0hQ8_gn7Yz2r9WrcoWx1PYi6c */

                        public final void run(IBinder iBinder) {
                            HwLocationProviderProxy.this.initializeServiceAB(iBinder);
                        }
                    });
                }
            }

            /* access modifiers changed from: protected */
            public void onUnbind() {
                HwLocationProviderProxy.this.setProperties(null);
            }
        };
        mServiceWatcherUtils.setServicePackageName(serviceWatcher, servicePackageName);
        mServiceWatcherUtils.setSignatureSets(serviceWatcher, signatureSets);
        return serviceWatcher;
    }

    private boolean bind() {
        if (this.mServiceWatcherAb != null && (HwMultiNlpPolicy.isChineseVersion() || !HwMultiNlpPolicy.isGmsExist())) {
            this.mStartAB = this.mServiceWatcherAb.start();
            registerPhoneStateListener();
            if (!this.mStartAB) {
                this.mPMAbRegisted = true;
                this.mPackageMonitorAb.register(this.mContext, (Looper) null, UserHandle.ALL, true);
            }
        }
        if (this.mServiceWatcherGoogle != null && !HwMultiNlpPolicy.isChineseVersion()) {
            this.mStartGoogle = this.mServiceWatcherGoogle.start();
            if (!this.mStartGoogle) {
                this.mPMGoogleRegisted = true;
                this.mPackageMonitorGoogle.register(this.mContext, (Looper) null, UserHandle.ALL, true);
            }
        }
        this.isDualNlpAlive = this.mStartAB && this.mStartGoogle;
        if (!this.mStartGoogle || !HwMultiNlpPolicy.isGmsExist()) {
            this.mNlpUsed = 2;
            this.mServiceWatcherDefault = this.mServiceWatcherAb;
        } else {
            this.mNlpUsed = 1;
            this.mServiceWatcherDefault = this.mServiceWatcherGoogle;
        }
        LBSLog.i(TAG, false, "mNlp:start_g %{public}b , start_ab %{public}b", Boolean.valueOf(this.mStartGoogle), Boolean.valueOf(this.mStartAB));
        if (this.mStartGoogle || this.mStartAB) {
            return true;
        }
        return false;
    }

    /* access modifiers changed from: private */
    public void initializeServiceGoogle(IBinder binder) {
        ILocationProvider service = ILocationProvider.Stub.asInterface(binder);
        try {
            service.setLocationProviderManager(this.mGoogleManager);
            synchronized (this.mRequestLock) {
                if (this.mRequest != null) {
                    service.setRequest(this.mRequest, this.mWorkSource);
                }
            }
        } catch (RemoteException e) {
            LBSLog.w(TAG, false, e.getMessage(), new Object[0]);
        }
    }

    /* access modifiers changed from: private */
    public void initializeServiceAB(IBinder binder) {
        ILocationProvider service = ILocationProvider.Stub.asInterface(binder);
        try {
            service.setLocationProviderManager(this.mManager);
            synchronized (this.mRequestLock) {
                if (this.mRequest != null && (!HwMultiNlpPolicy.getDefault(this.mContext).getGlobalNLPStart() || isWorkSourceContain(this.mWorkSource, HwMultiNlpPolicy.GLOBAL_NLP_CLIENT_PKG))) {
                    service.setRequest(this.mRequest, this.mWorkSource);
                }
            }
        } catch (RemoteException e) {
            LBSLog.w(TAG, false, "exception : %{public}s", e.getMessage());
        }
    }

    public String getConnectedPackageName() {
        if (!this.mStartGoogle || !this.mStartAB) {
            if (this.mStartGoogle) {
                return this.mServiceWatcherGoogle.getCurrentPackageName();
            }
            return this.mServiceWatcherAb.getCurrentPackageName();
        } else if (this.mServiceWatcherGoogle.getCurrentPackageName() == null) {
            return this.mServiceWatcherAb.getCurrentPackageName();
        } else {
            if (this.mServiceWatcherAb.getCurrentPackageName() == null) {
                return this.mServiceWatcherGoogle.getCurrentPackageName();
            }
            if (1 == this.mNlpUsed) {
                return this.mServiceWatcherGoogle.getCurrentPackageName() + ";" + this.mServiceWatcherAb.getCurrentPackageName();
            }
            return this.mServiceWatcherAb.getCurrentPackageName() + ";" + this.mServiceWatcherGoogle.getCurrentPackageName();
        }
    }

    public void setRequest(ProviderRequest request, WorkSource source) {
        LBSLog.i(TAG, false, "setRequest %{public}s, mNlpUsed %{public}d , mLocationMonitoring %{public}b", request, Integer.valueOf(this.mNlpUsed), Boolean.valueOf(this.mLocationMonitoring));
        synchronized (this.mLock) {
            this.mRequest = request;
            this.mWorkSource = source;
        }
        if (!this.isDualNlpAlive || (this.mNlpUsed != 0 && (!this.mLocationMonitoring || (!HwMultiNlpPolicy.getDefault().shouldBeRecheck() && SystemClock.elapsedRealtime() <= this.mLastCheckTime + 3600000)))) {
            this.mLocationMonitoring = false;
            setRequest(request, source, this.mNlpUsed);
            if (HwMultiNlpPolicy.getDefault(this.mContext).getGlobalNLPStart()) {
                if (isWorkSourceContain(source, HwMultiNlpPolicy.GLOBAL_NLP_CLIENT_PKG)) {
                    setRequest(request, source, 2);
                } else {
                    setRequest(this.mRequestOff, this.mWorksourceOff, 2);
                }
            }
        } else {
            setRequest(request, source, 1);
            setRequest(request, source, 2);
            startCheckTask(10000);
        }
    }

    private void setRequest(ProviderRequest request, WorkSource source, int provider) {
        ServiceWatcher serviceWatcher = 1 == provider ? this.mServiceWatcherGoogle : this.mServiceWatcherAb;
        serviceWatcher.runOnBinder(new ServiceWatcher.BinderRunner(provider, request, source, serviceWatcher) {
            /* class com.android.server.location.$$Lambda$HwLocationProviderProxy$AiXLf2KAf6GYdmRpE5sngL3CrOk */
            private final /* synthetic */ int f$1;
            private final /* synthetic */ ProviderRequest f$2;
            private final /* synthetic */ WorkSource f$3;
            private final /* synthetic */ ServiceWatcher f$4;

            {
                this.f$1 = r2;
                this.f$2 = r3;
                this.f$3 = r4;
                this.f$4 = r5;
            }

            public final void run(IBinder iBinder) {
                HwLocationProviderProxy.this.lambda$setRequest$0$HwLocationProviderProxy(this.f$1, this.f$2, this.f$3, this.f$4, iBinder);
            }
        });
        if (1 == provider) {
            this.mHwNLPCache.startCheckCache(request.reportLocation);
        }
    }

    public /* synthetic */ void lambda$setRequest$0$HwLocationProviderProxy(int provider, ProviderRequest request, WorkSource source, ServiceWatcher serviceWatcher, IBinder binder) throws RemoteException {
        ILocationProvider service = ILocationProvider.Stub.asInterface(binder);
        LBSLog.i(TAG, false, "setRequest to %{public}s", getNlpName(provider));
        try {
            service.setRequest(request, source);
        } catch (RemoteException e) {
            LBSLog.w(TAG, false, e.getMessage(), new Object[0]);
        } catch (Exception e2) {
            LBSLog.e(TAG, false, "Exception from %{public}s", serviceWatcher.getCurrentPackageName());
        }
    }

    private void registerPhoneStateListener() {
        this.mPhoneStateListener = new PhoneStateListener() {
            /* class com.android.server.location.HwLocationProviderProxy.AnonymousClass4 */

            public void onServiceStateChanged(ServiceState state) {
                if (state != null) {
                    String numeric = state.getOperatorNumeric();
                    if (numeric != null && numeric.length() >= 5 && "99999".equals(numeric.substring(0, 5))) {
                        return;
                    }
                    if (numeric != null && numeric.length() >= 3 && WifiProCommonUtils.COUNTRY_CODE_CN.equals(numeric.substring(0, 3))) {
                        HwLocationProviderProxy.this.abNlpBind();
                    } else if (numeric != null && !"".equals(numeric)) {
                        HwLocationProviderProxy.this.googleNlpBind();
                    }
                }
            }
        };
        this.mTelephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
        this.mTelephonyManager.listen(this.mPhoneStateListener, 1);
    }

    /* access modifiers changed from: private */
    public void googleNlpBind() {
        ServiceWatcher serviceWatcher;
        if (!this.mStartGoogle && !this.mPMGoogleRegisted && (serviceWatcher = this.mServiceWatcherGoogle) != null) {
            this.mStartGoogle = serviceWatcher.start();
            boolean z = true;
            if (!this.mStartGoogle && !this.mPMGoogleRegisted) {
                this.mPMGoogleRegisted = true;
                this.mPackageMonitorGoogle.register(this.mContext, (Looper) null, UserHandle.ALL, true);
            }
            if (!this.mStartGoogle || !this.mStartAB) {
                z = false;
            }
            this.isDualNlpAlive = z;
        }
    }

    /* access modifiers changed from: private */
    public void abNlpBind() {
        ServiceWatcher serviceWatcher;
        if (!this.mStartAB && !this.mPMAbRegisted && (serviceWatcher = this.mServiceWatcherAb) != null) {
            this.mStartAB = serviceWatcher.start();
            boolean z = true;
            if (!this.mStartAB && !this.mPMAbRegisted) {
                this.mPMAbRegisted = true;
                this.mPackageMonitorAb.register(this.mContext, (Looper) null, UserHandle.ALL, true);
            }
            if (!this.mStartGoogle || !this.mStartAB) {
                z = false;
            }
            this.isDualNlpAlive = z;
        }
    }

    class CheckTask extends TimerTask {
        CheckTask() {
        }

        public void run() {
            HwLocationProviderProxy.this.validNlpChoice();
            boolean unused = HwLocationProviderProxy.this.mLocationMonitoring = false;
        }
    }

    private void cancelCheckTimer() {
        Timer timer = this.mCheckTimer;
        if (timer != null) {
            timer.cancel();
            this.mCheckTimer = null;
        }
    }

    private void startCheckTask(int delytime) {
        cancelCheckTimer();
        this.mCheckTimer = new Timer("LocationCheckTimer");
        this.mCheckTimer.schedule(new CheckTask(), (long) delytime);
    }

    /* access modifiers changed from: private */
    public void validNlpChoice() {
        boolean shouldUseGoogle = HwMultiNlpPolicy.getDefault().shouldUseGoogleNLP(true);
        LBSLog.i(TAG, false, "validNlpChoice google: %{public}b, Ab: %{public}b ,nlp: %{public}s, shouldUseGoogle: %{public}b", Boolean.valueOf(this.mLocationSuccessGoogle), Boolean.valueOf(this.mLocationSuccessAB), getNlpName(this.mNlpUsed), Boolean.valueOf(shouldUseGoogle));
        if (!this.mLocationSuccessGoogle || !this.mLocationSuccessAB) {
            if (this.mLocationSuccessGoogle) {
                if (isEnabled()) {
                    setRequest(this.mRequestOff, this.mWorksourceOff, 2);
                }
                this.mNlpUsed = 1;
                this.mServiceWatcherDefault = this.mServiceWatcherGoogle;
            } else if (this.mLocationSuccessAB) {
                if (isEnabled()) {
                    setRequest(this.mRequestOff, this.mWorksourceOff, 1);
                }
                this.mNlpUsed = 2;
                this.mServiceWatcherDefault = this.mServiceWatcherAb;
            } else if (isEnabled()) {
                this.mNlpUsed = 0;
            }
        } else if (shouldUseGoogle) {
            if (isEnabled()) {
                setRequest(this.mRequestOff, this.mWorksourceOff, 2);
            }
            this.mNlpUsed = 1;
            this.mServiceWatcherDefault = this.mServiceWatcherGoogle;
        } else {
            if (isEnabled()) {
                setRequest(this.mRequestOff, this.mWorksourceOff, 1);
            }
            this.mNlpUsed = 2;
            this.mServiceWatcherDefault = this.mServiceWatcherAb;
        }
        this.mLastCheckTime = SystemClock.elapsedRealtime();
    }

    private boolean isEnabled() {
        return this.mLocationManager.isProviderEnabled("network");
    }

    public void resetNLPFlag() {
        LBSLog.i(TAG, false, "resetNLPFlag ", new Object[0]);
        if (!this.mLocationMonitoring) {
            this.mLocationMonitoring = true;
            this.mLocationSuccessAB = false;
            this.mLocationSuccessGoogle = false;
        }
    }

    private String getAppName(int pid) {
        List<ActivityManager.RunningAppProcessInfo> appProcessList = ((ActivityManager) this.mContext.getSystemService(BigMemoryConstant.BIGMEMINFO_ITEM_TAG)).getRunningAppProcesses();
        if (appProcessList == null) {
            return null;
        }
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcessList) {
            if (pid == appProcess.pid) {
                return appProcess.processName;
            }
        }
        return null;
    }

    private int reportLocationNlp(int pid) {
        int ret = 0;
        int i = this.mABNlpPid;
        if (pid != i || i == 0) {
            int i2 = this.mGoogleNlpPid;
            if (pid != i2 || i2 == 0) {
                String appName = getAppName(pid);
                if (AB_NLP_PKNAME.equals(appName)) {
                    this.mABNlpPid = pid;
                    ret = 2;
                }
                if (appName != null && appName.indexOf("com.google.android.gms") >= 0) {
                    this.mGoogleNlpPid = pid;
                    ret = 1;
                }
                LBSLog.i(TAG, false, "reportLocationNlp %{public}s, pid %{public}d", appName, Integer.valueOf(pid));
                return ret;
            }
            LBSLog.i(TAG, false, "reportLocationNlp %{public}d , google", Integer.valueOf(pid));
            return 1;
        }
        LBSLog.i(TAG, false, "reportLocationNlp %{public}d , ab", Integer.valueOf(pid));
        return 2;
    }

    public boolean reportNLPLocation(int pid) {
        boolean ret;
        LBSLog.i(TAG, false, "reportNLPLocation %{public}d, nlpUsed %{public}d, dualNlp %{public}b, monitoring %{public}b", Integer.valueOf(pid), Integer.valueOf(this.mNlpUsed), Boolean.valueOf(this.isDualNlpAlive), Boolean.valueOf(this.mLocationMonitoring));
        boolean ret2 = true;
        int nlp = reportLocationNlp(pid);
        if (this.isDualNlpAlive) {
            if (this.mLocationMonitoring) {
                if (nlp == 2) {
                    this.mLocationSuccessAB = true;
                } else if (nlp == 1) {
                    this.mLocationSuccessGoogle = true;
                }
                if (nlp == HwMultiNlpPolicy.getDefault().shouldUseNLP() || ((nlp == 2 && !this.mLocationSuccessGoogle) || (nlp == 1 && !this.mLocationSuccessAB))) {
                    ret = true;
                } else {
                    ret = false;
                }
            } else {
                int i = this.mNlpUsed;
                if (nlp == i) {
                    ret = true;
                } else if (i == 0) {
                    if (nlp == 2) {
                        this.mLocationSuccessAB = true;
                    } else if (nlp == 1) {
                        this.mLocationSuccessGoogle = true;
                    }
                    validNlpChoice();
                    ret = true;
                } else {
                    ret = false;
                }
            }
            LBSLog.i(TAG, false, "shouldReportLocation . %{public}b, from %{public}s", Boolean.valueOf(ret), getNlpName(nlp));
            return ret;
        }
        if (nlp == 1) {
            this.mLocationSuccessGoogle = true;
            ret2 = true;
        }
        if (!HwMultiNlpPolicy.getDefault(this.mContext).getGlobalNLPStart() || !this.mLocationSuccessGoogle || nlp != 2) {
            return ret2;
        }
        setRequest(this.mRequestOff, this.mWorksourceOff, 2);
        return false;
    }

    private String getNlpName(int nlp) {
        if (nlp == 1) {
            return "google map";
        }
        if (nlp != 2) {
            return "unkonw";
        }
        return "Amap or baidu map";
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.append("REMOTE SERVICE");
        pw.append(" pkg=").append((CharSequence) this.mServiceWatcherDefault.getCurrentPackageName());
        pw.append('\n');
        this.mServiceWatcherDefault.runOnBinder(new ServiceWatcher.BinderRunner(pw, fd, args) {
            /* class com.android.server.location.$$Lambda$HwLocationProviderProxy$tMswFfjbf4W0lhRgkQNjVPYADNQ */
            private final /* synthetic */ PrintWriter f$1;
            private final /* synthetic */ FileDescriptor f$2;
            private final /* synthetic */ String[] f$3;

            {
                this.f$1 = r2;
                this.f$2 = r3;
                this.f$3 = r4;
            }

            public final void run(IBinder iBinder) {
                HwLocationProviderProxy.this.lambda$dump$1$HwLocationProviderProxy(this.f$1, this.f$2, this.f$3, iBinder);
            }
        });
    }

    public /* synthetic */ void lambda$dump$1$HwLocationProviderProxy(PrintWriter pw, FileDescriptor fd, String[] args, IBinder binder) throws RemoteException {
        ILocationProvider service = ILocationProvider.Stub.asInterface(binder);
        if (service == null) {
            pw.println("service down (null)");
            return;
        }
        pw.flush();
        try {
            service.asBinder().dump(fd, args);
        } catch (RemoteException e) {
            pw.println("service down (RemoteException)");
            LBSLog.w(TAG, false, e.getMessage(), new Object[0]);
        } catch (Exception e2) {
            pw.println("service down (Exception)");
            LBSLog.e(TAG, false, "Exception from: %{public}s ", this.mServiceWatcherDefault.getCurrentPackageName());
        }
    }

    public int getStatus(Bundle extras) {
        return ((Integer) this.mServiceWatcherDefault.runOnBinderBlocking(new ServiceWatcher.BlockingBinderRunner(extras) {
            /* class com.android.server.location.$$Lambda$HwLocationProviderProxy$P2jlvEw1oM7gw3rbzXXdDRggLw */
            private final /* synthetic */ Bundle f$0;

            {
                this.f$0 = r1;
            }

            public final Object run(IBinder iBinder) {
                return Integer.valueOf(ILocationProvider.Stub.asInterface(iBinder).getStatus(this.f$0));
            }
        }, 1)).intValue();
    }

    public long getStatusUpdateTime() {
        return ((Long) this.mServiceWatcherDefault.runOnBinderBlocking($$Lambda$HwLocationProviderProxy$3xlds_S589E5DFHR9FdCv9qD0g.INSTANCE, 0L)).longValue();
    }

    public void sendExtraCommand(String command, Bundle extras) {
        this.mServiceWatcherDefault.runOnBinder(new ServiceWatcher.BinderRunner(command, extras) {
            /* class com.android.server.location.$$Lambda$HwLocationProviderProxy$FZLpoemR_eyOfmBB_dwiArgJpaQ */
            private final /* synthetic */ String f$0;
            private final /* synthetic */ Bundle f$1;

            {
                this.f$0 = r1;
                this.f$1 = r2;
            }

            public final void run(IBinder iBinder) {
                HwLocationProviderProxy.lambda$sendExtraCommand$4(this.f$0, this.f$1, iBinder);
            }
        });
    }

    static /* synthetic */ void lambda$sendExtraCommand$4(String command, Bundle extras, IBinder binder) throws RemoteException {
        try {
            ILocationProvider.Stub.asInterface(binder).sendExtraCommand(command, extras);
        } catch (RemoteException e) {
            LBSLog.w(TAG, false, e.getMessage(), new Object[0]);
        }
    }

    /* access modifiers changed from: private */
    public void onSetAdditionalProviderPackages(List<String> list) {
    }

    private boolean isWorkSourceContain(WorkSource workSource, String packageName) {
        if (workSource == null) {
            return false;
        }
        int size = workSource.size();
        boolean isDebugEnabled = HwMultiNlpPolicy.isHwLocationDebug(this.mContext);
        for (int i = 0; i < size; i++) {
            if ((isDebugEnabled && HwMultiNlpPolicy.GLOBAL_NLP_DEBUG_PKG.equals(workSource.getName(i))) || packageName.equals(workSource.getName(i))) {
                return true;
            }
        }
        return false;
    }

    @Override // com.android.server.location.HwLocationProxy
    public void handleServiceAB(boolean shouldStart) {
        if (shouldStart) {
            this.mServiceWatcherAb.start();
            return;
        }
        setRequest(this.mRequestOff, this.mWorksourceOff, 2);
        mServiceWatcherUtils.unbindLocked(this.mServiceWatcherAb);
    }
}
