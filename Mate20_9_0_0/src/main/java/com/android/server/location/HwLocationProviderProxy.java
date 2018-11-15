package com.android.server.location;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.content.pm.Signature;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.WorkSource;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.android.internal.content.PackageMonitor;
import com.android.internal.location.ILocationProvider;
import com.android.internal.location.ILocationProvider.Stub;
import com.android.internal.location.ProviderProperties;
import com.android.internal.location.ProviderRequest;
import com.android.server.LocationManagerServiceUtil;
import com.android.server.ServiceWatcher;
import com.android.server.ServiceWatcherUtils;
import com.android.server.wifipro.WifiProCommonUtils;
import com.huawei.utils.reflect.EasyInvokeFactory;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class HwLocationProviderProxy extends LocationProviderProxy implements IHwLocationProviderInterface {
    private static final int CHECK_DELAY_TIME = 10000;
    private static final boolean D = false;
    private static final int NLP_AB = 2;
    private static final int NLP_GOOGLE = 1;
    private static final int NLP_NONE = 0;
    private static final long RETENTION_PERIOD = 3600000;
    private static final String TAG = "HwLocationProviderProxy";
    private static ServiceWatcherUtils mServiceWatcherUtils = ((ServiceWatcherUtils) EasyInvokeFactory.getInvokeUtils(ServiceWatcherUtils.class));
    private int ab_nlp_pid = 0;
    private String ab_nlp_pkName = SystemProperties.get("ro.config.hw_nlp", "com.baidu.map.location");
    private int google_nlp_pid = 0;
    private String google_nlp_pkName = LocationManagerServiceUtil.GOOGLE_GMS_PROCESS;
    private boolean isDualNlpAlive;
    private Timer mCheckTimer;
    private final Context mContext;
    private boolean mEnabled = false;
    private long mLastCheckTime = 0;
    private boolean mLocationMonitoring = false;
    private boolean mLocationSuccess_Ab = false;
    private boolean mLocationSuccess_google = false;
    private Object mLock = new Object();
    private final String mName;
    protected Runnable mNewServiceWorkAb = new Runnable() {
        public void run() {
            boolean enabled;
            ProviderRequest request;
            WorkSource source;
            ILocationProvider service;
            ProviderProperties providerProperties = null;
            synchronized (HwLocationProviderProxy.this.mLock) {
                enabled = HwLocationProviderProxy.this.mEnabled;
                request = HwLocationProviderProxy.this.mRequest;
                source = HwLocationProviderProxy.this.mWorksource;
                service = HwLocationProviderProxy.this.getServiceAb();
            }
            if (service != null) {
                try {
                    String str;
                    StringBuilder stringBuilder;
                    providerProperties = service.getProperties();
                    if (providerProperties == null) {
                        str = HwLocationProviderProxy.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append(HwLocationProviderProxy.this.mServiceWatcherAb.getBestPackageName());
                        stringBuilder.append(" has invalid locatino provider properties");
                        Log.e(str, stringBuilder.toString());
                    }
                    str = HwLocationProviderProxy.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("mNewServiceWorkAb use ab. ");
                    stringBuilder.append(HwLocationProviderProxy.this.mNlpUsed);
                    Log.d(str, stringBuilder.toString());
                    if (enabled) {
                        service.enable();
                        if (HwLocationProviderProxy.this.mNlpUsed == 2 && request != null) {
                            service.setRequest(request, source);
                        }
                    }
                } catch (RemoteException e) {
                    Log.w(HwLocationProviderProxy.TAG, e);
                } catch (Exception e2) {
                    String str2 = HwLocationProviderProxy.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Exception from ");
                    stringBuilder2.append(HwLocationProviderProxy.this.mServiceWatcherAb.getBestPackageName());
                    Log.e(str2, stringBuilder2.toString(), e2);
                }
                ProviderProperties properties = providerProperties;
                synchronized (HwLocationProviderProxy.this.mLock) {
                    HwLocationProviderProxy.this.mPropertiesAb = properties;
                }
            }
        }
    };
    protected Runnable mNewServiceWorkGoogle = new Runnable() {
        public void run() {
            boolean enabled;
            ProviderRequest request;
            WorkSource source;
            ILocationProvider service;
            ProviderProperties providerProperties = null;
            synchronized (HwLocationProviderProxy.this.mLock) {
                enabled = HwLocationProviderProxy.this.mEnabled;
                request = HwLocationProviderProxy.this.mRequest;
                source = HwLocationProviderProxy.this.mWorksource;
                service = HwLocationProviderProxy.this.getServiceGoogle();
            }
            if (service != null) {
                try {
                    String str;
                    StringBuilder stringBuilder;
                    providerProperties = service.getProperties();
                    if (providerProperties == null) {
                        str = HwLocationProviderProxy.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append(HwLocationProviderProxy.this.mServiceWatcherGoogle.getBestPackageName());
                        stringBuilder.append(" has invalid locatino provider properties");
                        Log.e(str, stringBuilder.toString());
                    }
                    str = HwLocationProviderProxy.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("mNewServiceWorkGoogle use google. ");
                    stringBuilder.append(HwLocationProviderProxy.this.mNlpUsed);
                    Log.d(str, stringBuilder.toString());
                    if (enabled) {
                        service.enable();
                        if (HwLocationProviderProxy.this.mNlpUsed == 1 && request != null) {
                            service.setRequest(request, source);
                        }
                    }
                } catch (RemoteException e) {
                    Log.w(HwLocationProviderProxy.TAG, e);
                } catch (Exception e2) {
                    String str2 = HwLocationProviderProxy.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Exception from ");
                    stringBuilder2.append(HwLocationProviderProxy.this.mServiceWatcherGoogle.getBestPackageName());
                    Log.e(str2, stringBuilder2.toString(), e2);
                }
                ProviderProperties properties = providerProperties;
                synchronized (HwLocationProviderProxy.this.mLock) {
                    HwLocationProviderProxy.this.mPropertiesGoogle = properties;
                }
            }
        }
    };
    private int mNlpUsed = 0;
    private boolean mPMAbRegisted = false;
    private boolean mPMGoogleRegisted = false;
    private final PackageMonitor mPackageMonitorAb = new PackageMonitor() {
        public boolean onPackageChanged(String packageName, int uid, String[] components) {
            synchronized (HwLocationProviderProxy.this.mLock) {
                if (packageName.equals(HwLocationProviderProxy.this.ab_nlp_pkName)) {
                    String str = HwLocationProviderProxy.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("rebind onPackageChanged ");
                    stringBuilder.append(packageName);
                    Log.d(str, stringBuilder.toString());
                    if (!(HwLocationProviderProxy.this.start_ab || HwLocationProviderProxy.this.mServiceWatcherAb == null)) {
                        HwLocationProviderProxy.this.start_ab = HwLocationProviderProxy.this.mServiceWatcherAb.start();
                        HwLocationProviderProxy hwLocationProviderProxy = HwLocationProviderProxy.this;
                        boolean z = HwLocationProviderProxy.this.start_g && HwLocationProviderProxy.this.start_ab;
                        hwLocationProviderProxy.isDualNlpAlive = z;
                    }
                }
            }
            return super.onPackageChanged(packageName, uid, components);
        }
    };
    private final PackageMonitor mPackageMonitorGoogle = new PackageMonitor() {
        public boolean onPackageChanged(String packageName, int uid, String[] components) {
            synchronized (HwLocationProviderProxy.this.mLock) {
                if (packageName.equals(HwLocationProviderProxy.this.google_nlp_pkName)) {
                    String str = HwLocationProviderProxy.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("rebind onPackageChanged ");
                    stringBuilder.append(packageName);
                    Log.d(str, stringBuilder.toString());
                    if (!(HwLocationProviderProxy.this.start_g || HwLocationProviderProxy.this.mServiceWatcherGoogle == null)) {
                        HwLocationProviderProxy.this.start_g = HwLocationProviderProxy.this.mServiceWatcherGoogle.start();
                        HwLocationProviderProxy hwLocationProviderProxy = HwLocationProviderProxy.this;
                        boolean z = HwLocationProviderProxy.this.start_g && HwLocationProviderProxy.this.start_ab;
                        hwLocationProviderProxy.isDualNlpAlive = z;
                    }
                }
            }
            return super.onPackageChanged(packageName, uid, components);
        }
    };
    private PhoneStateListener mPhoneStateListener;
    private ProviderProperties mPropertiesAb;
    private ProviderProperties mPropertiesGoogle;
    private ProviderRequest mRequest = null;
    private ProviderRequest mRequestOff = new ProviderRequest();
    private final ServiceWatcher mServiceWatcherAb;
    private ServiceWatcher mServiceWatcherDefault;
    private final ServiceWatcher mServiceWatcherGoogle;
    private TelephonyManager mTelephonyManager;
    private WorkSource mWorksource = new WorkSource();
    private WorkSource mWorksourceOff = new WorkSource();
    private boolean start_ab = false;
    private boolean start_g = false;

    class CheckTask extends TimerTask {
        CheckTask() {
        }

        public void run() {
            HwLocationProviderProxy.this.validNlpChoice();
            HwLocationProviderProxy.this.mLocationMonitoring = false;
        }
    }

    public static HwLocationProviderProxy createAndBind(Context context, String name, String action, int overlaySwitchResId, int defaultServicePackageNameResId, int initialPackageNamesResId, Handler handler) {
        HwLocationProviderProxy proxy = new HwLocationProviderProxy(context, name, action, overlaySwitchResId, defaultServicePackageNameResId, initialPackageNamesResId, handler);
        if (proxy.bind()) {
            return proxy;
        }
        return null;
    }

    private ServiceWatcher serviceWatcherCreate(int nlp, String action, int overlaySwitchResId, int defaultServicePackageNameResId, int initialPackageNamesResId, Handler handler) {
        String servicePackageName;
        Runnable runnable;
        ArrayList<String> initialPackageNames = new ArrayList();
        if (nlp == 1) {
            servicePackageName = this.google_nlp_pkName;
            runnable = this.mNewServiceWorkGoogle;
        } else {
            servicePackageName = this.ab_nlp_pkName;
            runnable = this.mNewServiceWorkAb;
        }
        Runnable newServiceWork = runnable;
        initialPackageNames.add(servicePackageName);
        ArrayList<HashSet<Signature>> signatureSets = ServiceWatcher.getSignatureSets(this.mContext, initialPackageNames);
        Context context = this.mContext;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("HwLocationProviderProxy-");
        stringBuilder.append(this.mName);
        ServiceWatcher serviceWatcher = new ServiceWatcher(context, stringBuilder.toString(), action, overlaySwitchResId, defaultServicePackageNameResId, initialPackageNamesResId, newServiceWork, handler);
        mServiceWatcherUtils.setServicePackageName(serviceWatcher, servicePackageName);
        mServiceWatcherUtils.setSignatureSets(serviceWatcher, signatureSets);
        return serviceWatcher;
    }

    public HwLocationProviderProxy(Context context, String name, String action, int overlaySwitchResId, int defaultServicePackageNameResId, int initialPackageNamesResId, Handler handler) {
        super(context, name);
        this.mContext = context;
        this.mName = name;
        this.mRequestOff.interval = 86400000;
        String str = action;
        int i = overlaySwitchResId;
        int i2 = defaultServicePackageNameResId;
        int i3 = initialPackageNamesResId;
        Handler handler2 = handler;
        this.mServiceWatcherGoogle = serviceWatcherCreate(1, str, i, i2, i3, handler2);
        this.mServiceWatcherAb = serviceWatcherCreate(2, str, i, i2, i3, handler2);
        this.isDualNlpAlive = false;
        this.mServiceWatcherDefault = this.mServiceWatcherGoogle;
    }

    private boolean bind() {
        if (this.mServiceWatcherAb != null && HwMultiNlpPolicy.isChineseVersion()) {
            this.start_ab = this.mServiceWatcherAb.start();
            if (!this.start_ab) {
                this.mPMAbRegisted = true;
                this.mPackageMonitorAb.register(this.mContext, null, UserHandle.ALL, true);
            }
        }
        if (!(this.mServiceWatcherGoogle == null || HwMultiNlpPolicy.isChineseVersion())) {
            this.start_g = this.mServiceWatcherGoogle.start();
            if (!this.start_g) {
                this.mPMGoogleRegisted = true;
                this.mPackageMonitorGoogle.register(this.mContext, null, UserHandle.ALL, true);
            }
        }
        registerPhoneStateListener();
        boolean z = this.start_ab && this.start_g;
        this.isDualNlpAlive = z;
        if (this.start_g) {
            this.mNlpUsed = 1;
            this.mServiceWatcherDefault = this.mServiceWatcherGoogle;
        } else {
            this.mNlpUsed = 2;
            this.mServiceWatcherDefault = this.mServiceWatcherAb;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mNlp:start_g ");
        stringBuilder.append(this.start_g);
        stringBuilder.append(" , start_ab ");
        stringBuilder.append(this.start_ab);
        Log.d(str, stringBuilder.toString());
        if (this.start_g || this.start_ab) {
            return true;
        }
        return false;
    }

    private void registerPhoneStateListener() {
        this.mPhoneStateListener = new PhoneStateListener() {
            public void onServiceStateChanged(ServiceState state) {
                if (state != null) {
                    String numeric = state.getOperatorNumeric();
                    if (numeric != null && numeric.length() >= 5 && numeric.substring(0, 5).equals("99999")) {
                        return;
                    }
                    if (numeric != null && numeric.length() >= 3 && numeric.substring(0, 3).equals(WifiProCommonUtils.COUNTRY_CODE_CN)) {
                        HwLocationProviderProxy.this.abNlpBind();
                    } else if (numeric != null && !numeric.equals("")) {
                        HwLocationProviderProxy.this.googleNlpBind();
                    }
                }
            }
        };
        this.mTelephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
        this.mTelephonyManager.listen(this.mPhoneStateListener, 1);
    }

    private void googleNlpBind() {
        if (!this.start_g && !this.mPMGoogleRegisted && this.mServiceWatcherGoogle != null) {
            this.start_g = this.mServiceWatcherGoogle.start();
            boolean z = true;
            if (!(this.start_g || this.mPMGoogleRegisted)) {
                this.mPMGoogleRegisted = true;
                this.mPackageMonitorGoogle.register(this.mContext, null, UserHandle.ALL, true);
            }
            if (!(this.start_g && this.start_ab)) {
                z = false;
            }
            this.isDualNlpAlive = z;
        }
    }

    private void abNlpBind() {
        if (!this.start_ab && !this.mPMAbRegisted && this.mServiceWatcherAb != null) {
            this.start_ab = this.mServiceWatcherAb.start();
            boolean z = true;
            if (!(this.start_ab || this.mPMAbRegisted)) {
                this.mPMAbRegisted = true;
                this.mPackageMonitorAb.register(this.mContext, null, UserHandle.ALL, true);
            }
            if (!(this.start_g && this.start_ab)) {
                z = false;
            }
            this.isDualNlpAlive = z;
        }
    }

    private ILocationProvider getServiceGoogle() {
        if (this.mServiceWatcherGoogle != null) {
            return Stub.asInterface(this.mServiceWatcherGoogle.getBinder());
        }
        return null;
    }

    private ILocationProvider getServiceAb() {
        if (this.mServiceWatcherAb != null) {
            return Stub.asInterface(this.mServiceWatcherAb.getBinder());
        }
        return null;
    }

    private ILocationProvider getService() {
        if (this.mServiceWatcherDefault != null) {
            return Stub.asInterface(this.mServiceWatcherDefault.getBinder());
        }
        return null;
    }

    public String getConnectedPackageName() {
        if (this.start_g && this.start_ab) {
            if (this.mServiceWatcherGoogle.getBestPackageName() == null) {
                return this.mServiceWatcherAb.getBestPackageName();
            }
            if (this.mServiceWatcherAb.getBestPackageName() == null) {
                return this.mServiceWatcherGoogle.getBestPackageName();
            }
            StringBuilder stringBuilder;
            if (1 == this.mNlpUsed) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(this.mServiceWatcherGoogle.getBestPackageName());
                stringBuilder.append(CPUCustBaseConfig.CPUCONFIG_GAP_IDENTIFIER);
                stringBuilder.append(this.mServiceWatcherAb.getBestPackageName());
                return stringBuilder.toString();
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append(this.mServiceWatcherAb.getBestPackageName());
            stringBuilder.append(CPUCustBaseConfig.CPUCONFIG_GAP_IDENTIFIER);
            stringBuilder.append(this.mServiceWatcherGoogle.getBestPackageName());
            return stringBuilder.toString();
        } else if (this.start_g) {
            return this.mServiceWatcherGoogle.getBestPackageName();
        } else {
            return this.mServiceWatcherAb.getBestPackageName();
        }
    }

    public String getName() {
        return this.mName;
    }

    public ProviderProperties getProperties() {
        ProviderProperties providerProperties;
        synchronized (this.mLock) {
            providerProperties = this.mNlpUsed == 1 ? this.mPropertiesGoogle : this.mPropertiesAb;
        }
        return providerProperties;
    }

    private void enableGoolge() {
        if (this.start_g) {
            ILocationProvider service = getServiceGoogle();
            if (service != null) {
                try {
                    service.enable();
                } catch (RemoteException e) {
                    Log.w(TAG, e);
                } catch (Exception e2) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Exception from ");
                    stringBuilder.append(this.mServiceWatcherGoogle.getBestPackageName());
                    Log.e(str, stringBuilder.toString(), e2);
                }
            }
        }
    }

    private void enableAb() {
        if (this.start_ab) {
            ILocationProvider service = getServiceAb();
            if (service != null) {
                try {
                    service.enable();
                } catch (RemoteException e) {
                    Log.w(TAG, e);
                } catch (Exception e2) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Exception from ");
                    stringBuilder.append(this.mServiceWatcherGoogle.getBestPackageName());
                    Log.e(str, stringBuilder.toString(), e2);
                }
            }
        }
    }

    public void enable() {
        synchronized (this.mLock) {
            this.mEnabled = true;
        }
        enableGoolge();
        enableAb();
    }

    private void disableGoolge() {
        if (this.start_g) {
            ILocationProvider service = getServiceGoogle();
            if (service != null) {
                try {
                    service.disable();
                } catch (RemoteException e) {
                    Log.w(TAG, e);
                } catch (Exception e2) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Exception from ");
                    stringBuilder.append(this.mServiceWatcherGoogle.getBestPackageName());
                    Log.e(str, stringBuilder.toString(), e2);
                }
            }
        }
    }

    private void disableAb() {
        if (this.start_ab) {
            ILocationProvider service = getServiceAb();
            if (service != null) {
                try {
                    service.disable();
                } catch (RemoteException e) {
                    Log.w(TAG, e);
                } catch (Exception e2) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Exception from ");
                    stringBuilder.append(this.mServiceWatcherAb.getBestPackageName());
                    Log.e(str, stringBuilder.toString(), e2);
                }
            }
        }
    }

    public void disable() {
        synchronized (this.mLock) {
            this.mEnabled = false;
        }
        disableGoolge();
        disableAb();
        cancelCheckTimer();
    }

    public boolean isEnabled() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mEnabled;
        }
        return z;
    }

    private void cancelCheckTimer() {
        if (this.mCheckTimer != null) {
            this.mCheckTimer.cancel();
            this.mCheckTimer = null;
        }
    }

    private void startCheckTask(int delytime) {
        cancelCheckTimer();
        this.mCheckTimer = new Timer("LocationCheckTimer");
        this.mCheckTimer.schedule(new CheckTask(), (long) delytime);
    }

    private void validNlpChoice() {
        boolean shouldUseGoogle = HwMultiNlpPolicy.getDefault().shouldUseGoogleNLP(true);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("validNlpChoice google/");
        stringBuilder.append(this.mLocationSuccess_google);
        stringBuilder.append(", Ab/");
        stringBuilder.append(this.mLocationSuccess_Ab);
        stringBuilder.append(" ,nlp/");
        stringBuilder.append(getNlpName(this.mNlpUsed));
        stringBuilder.append(", shouldUseGoogle/");
        stringBuilder.append(shouldUseGoogle);
        Log.d(str, stringBuilder.toString());
        if (this.mLocationSuccess_google && this.mLocationSuccess_Ab) {
            if (shouldUseGoogle) {
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
        } else if (this.mLocationSuccess_google) {
            if (isEnabled()) {
                setRequest(this.mRequestOff, this.mWorksourceOff, 2);
            }
            this.mNlpUsed = 1;
            this.mServiceWatcherDefault = this.mServiceWatcherGoogle;
        } else if (this.mLocationSuccess_Ab) {
            if (isEnabled()) {
                setRequest(this.mRequestOff, this.mWorksourceOff, 1);
            }
            this.mNlpUsed = 2;
            this.mServiceWatcherDefault = this.mServiceWatcherAb;
        } else if (isEnabled()) {
            this.mNlpUsed = 0;
        }
        this.mLastCheckTime = SystemClock.elapsedRealtime();
    }

    private void setRequest(ProviderRequest request, WorkSource source, int provider) {
        ILocationProvider service = 1 == provider ? getServiceGoogle() : getServiceAb();
        ServiceWatcher serviceWatcher = 1 == provider ? this.mServiceWatcherGoogle : this.mServiceWatcherAb;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setRequest to ");
        stringBuilder.append(getNlpName(provider));
        Log.d(str, stringBuilder.toString());
        if (service != null) {
            try {
                service.setRequest(request, source);
                return;
            } catch (RemoteException e) {
                Log.w(TAG, e);
                return;
            } catch (Exception e2) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Exception from ");
                stringBuilder2.append(serviceWatcher.getBestPackageName());
                Log.e(str2, stringBuilder2.toString(), e2);
                return;
            }
        }
        Log.e(TAG, "setRequest service is null.");
    }

    public void setRequest(ProviderRequest request, WorkSource source) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setRequest ");
        stringBuilder.append(request);
        stringBuilder.append(", mNlpUsed ");
        stringBuilder.append(this.mNlpUsed);
        stringBuilder.append(",mLocationMonitoring ");
        stringBuilder.append(this.mLocationMonitoring);
        Log.d(str, stringBuilder.toString());
        synchronized (this.mLock) {
            this.mRequest = request;
            this.mWorksource = source;
        }
        if (!this.isDualNlpAlive || (this.mNlpUsed != 0 && (!this.mLocationMonitoring || (!HwMultiNlpPolicy.getDefault().shouldBeRecheck() && SystemClock.elapsedRealtime() <= this.mLastCheckTime + 3600000)))) {
            this.mLocationMonitoring = false;
            setRequest(request, source, this.mNlpUsed);
            return;
        }
        setRequest(request, source, 1);
        setRequest(request, source, 2);
        startCheckTask(10000);
    }

    public void resetNLPFlag() {
        Log.d(TAG, "resetNLPFlag ");
        if (!this.mLocationMonitoring) {
            this.mLocationMonitoring = true;
            this.mLocationSuccess_Ab = false;
            this.mLocationSuccess_google = false;
        }
    }

    private String getAppName(int pid) {
        List<RunningAppProcessInfo> appProcessList = ((ActivityManager) this.mContext.getSystemService("activity")).getRunningAppProcesses();
        if (appProcessList == null) {
            return null;
        }
        for (RunningAppProcessInfo appProcess : appProcessList) {
            if (pid == appProcess.pid) {
                return appProcess.processName;
            }
        }
        return null;
    }

    private int reportLocationNlp(int pid) {
        int ret = 0;
        String str;
        StringBuilder stringBuilder;
        if (pid == this.ab_nlp_pid && this.ab_nlp_pid != 0) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("reportLocationNlp ");
            stringBuilder.append(pid);
            stringBuilder.append(", ab");
            Log.d(str, stringBuilder.toString());
            return 2;
        } else if (pid != this.google_nlp_pid || this.google_nlp_pid == 0) {
            str = getAppName(pid);
            if (this.ab_nlp_pkName.equals(str)) {
                this.ab_nlp_pid = pid;
                ret = 2;
            }
            if (str != null && str.indexOf(this.google_nlp_pkName) >= 0) {
                this.google_nlp_pid = pid;
                ret = 1;
            }
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("reportLocationNlp ");
            stringBuilder2.append(str);
            stringBuilder2.append(", pid ");
            stringBuilder2.append(pid);
            Log.d(str2, stringBuilder2.toString());
            return ret;
        } else {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("reportLocationNlp ");
            stringBuilder.append(pid);
            stringBuilder.append(", google");
            Log.d(str, stringBuilder.toString());
            return 1;
        }
    }

    public boolean reportNLPLocation(int pid) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("reportNLPLocation ");
        stringBuilder.append(pid);
        stringBuilder.append(", nlpUsed ");
        stringBuilder.append(this.mNlpUsed);
        stringBuilder.append(", dualNlp ");
        stringBuilder.append(this.isDualNlpAlive);
        stringBuilder.append(", monitoring ");
        stringBuilder.append(this.mLocationMonitoring);
        Log.d(str, stringBuilder.toString());
        boolean ret = true;
        if (this.isDualNlpAlive) {
            int nlp = reportLocationNlp(pid);
            if (this.mLocationMonitoring) {
                if (nlp == 2) {
                    this.mLocationSuccess_Ab = true;
                } else if (nlp == 1) {
                    this.mLocationSuccess_google = true;
                }
                if (nlp == HwMultiNlpPolicy.getDefault().shouldUseNLP() || ((nlp == 2 && !this.mLocationSuccess_google) || (nlp == 1 && !this.mLocationSuccess_Ab))) {
                    ret = true;
                } else {
                    ret = false;
                }
            } else if (nlp == this.mNlpUsed) {
                ret = true;
            } else if (this.mNlpUsed == 0) {
                if (nlp == 2) {
                    this.mLocationSuccess_Ab = true;
                } else if (nlp == 1) {
                    this.mLocationSuccess_google = true;
                }
                validNlpChoice();
                ret = true;
            } else {
                ret = false;
            }
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("shouldReportLocation . ");
            stringBuilder2.append(ret);
            stringBuilder2.append(", from ");
            stringBuilder2.append(getNlpName(nlp));
            Log.d(str2, stringBuilder2.toString());
        }
        return ret;
    }

    private String getNlpName(int nlp) {
        switch (nlp) {
            case 1:
                return "google map";
            case 2:
                return "Amap or baidu map";
            default:
                return "unkonw";
        }
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.append("REMOTE SERVICE");
        pw.append(" name=").append(this.mName);
        pw.append(" pkg=").append(this.mServiceWatcherDefault.getBestPackageName());
        pw.append(" version=").append(Integer.toString(this.mServiceWatcherDefault.getBestVersion()));
        pw.append(10);
        ILocationProvider service = getService();
        if (service == null) {
            pw.println("service down (null)");
            return;
        }
        pw.flush();
        try {
            service.asBinder().dump(fd, args);
        } catch (RemoteException e) {
            pw.println("service down (RemoteException)");
            Log.w(TAG, e);
        } catch (Exception e2) {
            pw.println("service down (Exception)");
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Exception from ");
            stringBuilder.append(this.mServiceWatcherDefault.getBestPackageName());
            Log.e(str, stringBuilder.toString(), e2);
        }
    }

    public int getStatus(Bundle extras) {
        ILocationProvider service = getService();
        if (service == null) {
            return 1;
        }
        try {
            return service.getStatus(extras);
        } catch (RemoteException e) {
            Log.w(TAG, e);
            return 1;
        } catch (Exception e2) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Exception from ");
            stringBuilder.append(this.mServiceWatcherDefault.getBestPackageName());
            Log.e(str, stringBuilder.toString(), e2);
            return 1;
        }
    }

    public long getStatusUpdateTime() {
        ILocationProvider service = getService();
        if (service == null) {
            return 0;
        }
        try {
            return service.getStatusUpdateTime();
        } catch (RemoteException e) {
            Log.w(TAG, e);
            return 0;
        } catch (Exception e2) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Exception from ");
            stringBuilder.append(this.mServiceWatcherDefault.getBestPackageName());
            Log.e(str, stringBuilder.toString(), e2);
            return 0;
        }
    }

    public boolean sendExtraCommand(String command, Bundle extras) {
        ILocationProvider service = getService();
        if (service == null) {
            return false;
        }
        try {
            return service.sendExtraCommand(command, extras);
        } catch (RemoteException e) {
            Log.w(TAG, e);
            return false;
        } catch (Exception e2) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Exception from ");
            stringBuilder.append(this.mServiceWatcherDefault.getBestPackageName());
            Log.e(str, stringBuilder.toString(), e2);
            return false;
        }
    }
}
