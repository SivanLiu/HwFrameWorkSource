package com.android.server.location;

import android.content.Context;
import android.content.pm.Signature;
import android.location.Address;
import android.location.GeocoderParams;
import android.location.IGeocodeProvider;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import com.android.internal.content.PackageMonitor;
import com.android.server.ServiceWatcher;
import com.android.server.ServiceWatcherUtils;
import com.android.server.wifipro.WifiProCommonUtils;
import com.huawei.utils.reflect.EasyInvokeFactory;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class HwGeocoderProxy extends GeocoderProxy implements HwLocationProxy {
    /* access modifiers changed from: private */
    public static final String AB_NLP_PKNAME = SystemProperties.get("ro.config.hw_nlp", "com.huawei.lbs");
    private static final int ENABLE_GEO_FOR_HMS = 1;
    private static final String GEO_FOR_HMS_FLAG = "enable_geo_for_hms";
    private static final String GOOGLE_NLP_PKNAME = "com.google.android.gms";
    private static final String HMS_NLP_PKNAME = "com.huawei.hwid";
    private static final int NLP_AB = 2;
    private static final int NLP_GOOGLE = 1;
    private static final int NLP_NONE = 0;
    private static final String SERVICE_ACTION = "com.android.location.service.GeocodeProvider";
    private static final String TAG = "HwGeocoderProxy";
    private static ServiceWatcherUtils mServiceWatcherUtils = EasyInvokeFactory.getInvokeUtils(ServiceWatcherUtils.class);
    private boolean isEnableGeoForHms = true;
    private final Context mContext;
    /* access modifiers changed from: private */
    public boolean mIsDualNlpAlive;
    /* access modifiers changed from: private */
    public final Object mLock = new Object();
    private int mNlpUsed = 0;
    private boolean mPMAbRegisted = false;
    private boolean mPMGoogleRegisted = false;
    private final PackageMonitor mPackageMonitorAb = new PackageMonitor() {
        /* class com.android.server.location.HwGeocoderProxy.AnonymousClass2 */

        public boolean onPackageChanged(String packageName, int uid, String[] components) {
            synchronized (HwGeocoderProxy.this.mLock) {
                if (HwGeocoderProxy.AB_NLP_PKNAME.equals(packageName) || HwGeocoderProxy.HMS_NLP_PKNAME.equals(packageName)) {
                    boolean z = true;
                    LBSLog.i(HwGeocoderProxy.TAG, false, "rebind onPackageChanged packageName : %{public}s", packageName);
                    if (!HwGeocoderProxy.this.mStartAB && HwGeocoderProxy.this.mServiceWatcherAb != null) {
                        boolean unused = HwGeocoderProxy.this.mStartAB = HwGeocoderProxy.this.mServiceWatcherAb.start();
                        HwGeocoderProxy hwGeocoderProxy = HwGeocoderProxy.this;
                        if (!HwGeocoderProxy.this.mStartGoogle || !HwGeocoderProxy.this.mStartAB) {
                            z = false;
                        }
                        boolean unused2 = hwGeocoderProxy.mIsDualNlpAlive = z;
                    }
                }
            }
            return HwGeocoderProxy.super.onPackageChanged(packageName, uid, components);
        }
    };
    private final PackageMonitor mPackageMonitorGoogle = new PackageMonitor() {
        /* class com.android.server.location.HwGeocoderProxy.AnonymousClass3 */

        public boolean onPackageChanged(String packageName, int uid, String[] components) {
            synchronized (HwGeocoderProxy.this.mLock) {
                if (packageName.equals("com.google.android.gms")) {
                    boolean z = true;
                    LBSLog.i(HwGeocoderProxy.TAG, false, "rebind onPackageChanged packageName : %{public}s", packageName);
                    if (!HwGeocoderProxy.this.mStartGoogle && HwGeocoderProxy.this.mServiceWatcherGoogle != null) {
                        boolean unused = HwGeocoderProxy.this.mStartGoogle = HwGeocoderProxy.this.mServiceWatcherGoogle.start();
                        HwGeocoderProxy hwGeocoderProxy = HwGeocoderProxy.this;
                        if (!HwGeocoderProxy.this.mStartGoogle || !HwGeocoderProxy.this.mStartAB) {
                            z = false;
                        }
                        boolean unused2 = hwGeocoderProxy.mIsDualNlpAlive = z;
                    }
                }
            }
            return HwGeocoderProxy.super.onPackageChanged(packageName, uid, components);
        }
    };
    private PhoneStateListener mPhoneStateListener;
    /* access modifiers changed from: private */
    public final ServiceWatcher mServiceWatcherAb;
    /* access modifiers changed from: private */
    public final ServiceWatcher mServiceWatcherGoogle;
    /* access modifiers changed from: private */
    public boolean mStartAB;
    /* access modifiers changed from: private */
    public boolean mStartGoogle;
    private TelephonyManager mTelephonyManager;

    public static HwGeocoderProxy createAndBind(Context context, int overlaySwitchResId, int defaultServicePackageNameResId, int initialPackageNamesResId) {
        HwGeocoderProxy proxy = new HwGeocoderProxy(context, overlaySwitchResId, defaultServicePackageNameResId, initialPackageNamesResId);
        if (proxy.bind()) {
            return proxy;
        }
        return null;
    }

    public HwGeocoderProxy(Context context, int overlaySwitchResId, int defaultServicePackageNameResId, int initialPackageNamesResId) {
        super(context);
        this.mContext = context;
        this.isEnableGeoForHms = 1 == Settings.Global.getInt(this.mContext.getContentResolver(), GEO_FOR_HMS_FLAG, 1);
        this.mServiceWatcherGoogle = serviceWatcherCreate(1, overlaySwitchResId, defaultServicePackageNameResId, initialPackageNamesResId, LocationHandlerEx.getGeoInstance());
        this.mServiceWatcherAb = serviceWatcherCreate(2, overlaySwitchResId, defaultServicePackageNameResId, initialPackageNamesResId, LocationHandlerEx.getGeoInstance());
        HwMultiNlpPolicy.getDefault(this.mContext).setHwGeocoderProxy(this);
        LBSLog.i(TAG, false, "isEnableGeoForHms =  %{public}b", Boolean.valueOf(this.isEnableGeoForHms));
    }

    private ServiceWatcher serviceWatcherCreate(int nlp, int overlaySwitchResId, int defaultServicePackageNameResId, int initialPackageNamesResId, Handler handler) {
        String servicePackageName;
        ArrayList<String> initialPackageNames = new ArrayList<>();
        if (nlp == 1) {
            servicePackageName = "com.google.android.gms";
        } else {
            servicePackageName = AB_NLP_PKNAME;
            if (HwMultiNlpPolicy.isOverseasNoGms() && this.isEnableGeoForHms) {
                initialPackageNames.add(HMS_NLP_PKNAME);
            }
        }
        initialPackageNames.add(servicePackageName);
        ArrayList<HashSet<Signature>> signatureSets = ServiceWatcher.getSignatureSets(this.mContext, (String[]) initialPackageNames.toArray(new String[initialPackageNames.size()]));
        ServiceWatcher serviceWatcher = new ServiceWatcher(this.mContext, TAG, SERVICE_ACTION, overlaySwitchResId, defaultServicePackageNameResId, initialPackageNamesResId, handler);
        if (HwMultiNlpPolicy.isOverseasNoGms() && this.isEnableGeoForHms) {
            servicePackageName = null;
        }
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
        this.mIsDualNlpAlive = this.mStartAB && this.mStartGoogle;
        LBSLog.i(TAG, false, "mNlp:start_g %{public}b , start_ab %{public}b", Boolean.valueOf(this.mStartGoogle), Boolean.valueOf(this.mStartAB));
        if (this.mStartGoogle || this.mStartAB) {
            return true;
        }
        return false;
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
            if (this.mNlpUsed == 1) {
                return this.mServiceWatcherGoogle.getCurrentPackageName() + ";" + this.mServiceWatcherAb.getCurrentPackageName();
            }
            return this.mServiceWatcherAb.getCurrentPackageName() + ";" + this.mServiceWatcherGoogle.getCurrentPackageName();
        }
    }

    public String getFromLocation(double latitude, double longitude, int maxResults, GeocoderParams params, List<Address> addrs) {
        this.mNlpUsed = 0;
        String result = getFromLocationImpl(latitude, longitude, maxResults, params, addrs);
        LBSLog.i(TAG, false, "getFromLocation %{public}s", result);
        if (!this.mIsDualNlpAlive || result == null) {
            if (needRequestGlobalAb(params, result)) {
                return getFromLocationGlobalAb(latitude, longitude, maxResults, params, addrs);
            }
            return result;
        } else if (this.mNlpUsed == 1) {
            this.mNlpUsed = 2;
            return getFromLocationImpl(latitude, longitude, maxResults, params, addrs);
        } else {
            this.mNlpUsed = 1;
            return getFromLocationImpl(latitude, longitude, maxResults, params, addrs);
        }
    }

    private String getFromLocationImpl(double latitude, double longitude, int maxResults, GeocoderParams params, List<Address> addrs) {
        ServiceWatcher serviceWatcher = getServiceWatcher();
        if (serviceWatcher == null) {
            return "ServiceWatcher not Available";
        }
        return (String) serviceWatcher.runOnBinderBlocking(new ServiceWatcher.BlockingBinderRunner(latitude, longitude, maxResults, params, addrs) {
            /* class com.android.server.location.$$Lambda$HwGeocoderProxy$3TLUNmqbbHF6tGM_K4JRAlf1psc */
            private final /* synthetic */ double f$0;
            private final /* synthetic */ double f$1;
            private final /* synthetic */ int f$2;
            private final /* synthetic */ GeocoderParams f$3;
            private final /* synthetic */ List f$4;

            {
                this.f$0 = r1;
                this.f$1 = r3;
                this.f$2 = r5;
                this.f$3 = r6;
                this.f$4 = r7;
            }

            public final Object run(IBinder iBinder) {
                return IGeocodeProvider.Stub.asInterface(iBinder).getFromLocation(this.f$0, this.f$1, this.f$2, this.f$3, this.f$4);
            }
        }, "Service not Available");
    }

    public String getFromLocationName(String locationName, double lowerLeftLatitude, double lowerLeftLongitude, double upperRightLatitude, double upperRightLongitude, int maxResults, GeocoderParams params, List<Address> addrs) {
        this.mNlpUsed = 0;
        String result = getFromLocationNameImpl(locationName, lowerLeftLatitude, lowerLeftLongitude, upperRightLatitude, upperRightLongitude, maxResults, params, addrs);
        LBSLog.i(TAG, false, "getFromLocationName %{public}s", result);
        if (!this.mIsDualNlpAlive || result == null) {
            if (needRequestGlobalAb(params, result)) {
                return getFromLocationNameGlobalAb(locationName, lowerLeftLatitude, lowerLeftLongitude, upperRightLatitude, upperRightLongitude, maxResults, params, addrs);
            }
            return result;
        } else if (this.mNlpUsed == 1) {
            this.mNlpUsed = 2;
            return getFromLocationNameImpl(locationName, lowerLeftLatitude, lowerLeftLongitude, upperRightLatitude, upperRightLongitude, maxResults, params, addrs);
        } else {
            this.mNlpUsed = 1;
            return getFromLocationNameImpl(locationName, lowerLeftLatitude, lowerLeftLongitude, upperRightLatitude, upperRightLongitude, maxResults, params, addrs);
        }
    }

    private String getFromLocationNameImpl(String locationName, double lowerLeftLatitude, double lowerLeftLongitude, double upperRightLatitude, double upperRightLongitude, int maxResults, GeocoderParams params, List<Address> addrs) {
        ServiceWatcher serviceWatcher = getServiceWatcher();
        if (serviceWatcher == null) {
            return "ServiceWatcher not Available";
        }
        return (String) serviceWatcher.runOnBinderBlocking(new ServiceWatcher.BlockingBinderRunner(locationName, lowerLeftLatitude, lowerLeftLongitude, upperRightLatitude, upperRightLongitude, maxResults, params, addrs) {
            /* class com.android.server.location.$$Lambda$HwGeocoderProxy$IZqjmY3B9HY2sT4seU2jWT4bSWE */
            private final /* synthetic */ String f$0;
            private final /* synthetic */ double f$1;
            private final /* synthetic */ double f$2;
            private final /* synthetic */ double f$3;
            private final /* synthetic */ double f$4;
            private final /* synthetic */ int f$5;
            private final /* synthetic */ GeocoderParams f$6;
            private final /* synthetic */ List f$7;

            {
                this.f$0 = r1;
                this.f$1 = r2;
                this.f$2 = r4;
                this.f$3 = r6;
                this.f$4 = r8;
                this.f$5 = r10;
                this.f$6 = r11;
                this.f$7 = r12;
            }

            public final Object run(IBinder iBinder) {
                return IGeocodeProvider.Stub.asInterface(iBinder).getFromLocationName(this.f$0, this.f$1, this.f$2, this.f$3, this.f$4, this.f$5, this.f$6, this.f$7);
            }
        }, "Service not Available");
    }

    private ServiceWatcher getServiceWatcher() {
        if (this.mIsDualNlpAlive) {
            int i = this.mNlpUsed;
            if (i == 0) {
                if (HwMultiNlpPolicy.getDefault().shouldUseGoogleNLP(false)) {
                    this.mNlpUsed = 1;
                    return this.mServiceWatcherGoogle;
                }
                this.mNlpUsed = 2;
                return this.mServiceWatcherAb;
            } else if (i == 1) {
                return this.mServiceWatcherGoogle;
            } else {
                if (i == 2) {
                    return this.mServiceWatcherAb;
                }
                LBSLog.i(TAG, false, "should not be here.", new Object[0]);
                return null;
            }
        } else if (this.mStartGoogle) {
            return this.mServiceWatcherGoogle;
        } else {
            if (this.mStartAB) {
                return this.mServiceWatcherAb;
            }
            LBSLog.i(TAG, false, "no service.", new Object[0]);
            return null;
        }
    }

    private void registerPhoneStateListener() {
        this.mPhoneStateListener = new PhoneStateListener() {
            /* class com.android.server.location.HwGeocoderProxy.AnonymousClass1 */

            public void onServiceStateChanged(ServiceState state) {
                if (state != null) {
                    String numeric = state.getOperatorNumeric();
                    if (numeric != null && numeric.length() >= 5 && "99999".equals(numeric.substring(0, 5))) {
                        LBSLog.i(HwGeocoderProxy.TAG, false, "numeric is contain 99999.", new Object[0]);
                    } else if (numeric != null && numeric.length() >= 3 && WifiProCommonUtils.COUNTRY_CODE_CN.equals(numeric.substring(0, 3))) {
                        HwGeocoderProxy.this.abNlpBind();
                    } else if (numeric == null || "".equals(numeric)) {
                        LBSLog.i(HwGeocoderProxy.TAG, false, "numeric is null or not belong to abNlp and googleNlp.", new Object[0]);
                    } else {
                        HwGeocoderProxy.this.googleNlpBind();
                    }
                }
            }
        };
        this.mTelephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
        this.mTelephonyManager.listen(this.mPhoneStateListener, 1);
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
            this.mIsDualNlpAlive = z;
        }
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
            this.mIsDualNlpAlive = z;
        }
    }

    private boolean needRequestGlobalAb(GeocoderParams params, String result) {
        String packageName = params.getClientPackage();
        return (HwMultiNlpPolicy.GLOBAL_NLP_CLIENT_PKG.equals(packageName) || (HwMultiNlpPolicy.GLOBAL_NLP_DEBUG_PKG.equals(packageName) && HwMultiNlpPolicy.isHwLocationDebug(this.mContext))) && result != null && HwMultiNlpPolicy.getDefault(this.mContext).getGlobalGeocoderStart();
    }

    @Override // com.android.server.location.HwLocationProxy
    public void handleServiceAB(boolean shouldStart) {
        if (shouldStart) {
            this.mServiceWatcherAb.start();
        } else {
            mServiceWatcherUtils.unbindLocked(this.mServiceWatcherAb);
        }
    }

    private String getFromLocationGlobalAb(double latitude, double longitude, int maxResults, GeocoderParams params, List<Address> addrs) {
        return (String) this.mServiceWatcherAb.runOnBinderBlocking(new ServiceWatcher.BlockingBinderRunner(latitude, longitude, maxResults, params, addrs) {
            /* class com.android.server.location.$$Lambda$HwGeocoderProxy$snUnjuEIx40G1LJplkmPIqzb3tQ */
            private final /* synthetic */ double f$0;
            private final /* synthetic */ double f$1;
            private final /* synthetic */ int f$2;
            private final /* synthetic */ GeocoderParams f$3;
            private final /* synthetic */ List f$4;

            {
                this.f$0 = r1;
                this.f$1 = r3;
                this.f$2 = r5;
                this.f$3 = r6;
                this.f$4 = r7;
            }

            public final Object run(IBinder iBinder) {
                return IGeocodeProvider.Stub.asInterface(iBinder).getFromLocation(this.f$0, this.f$1, this.f$2, this.f$3, this.f$4);
            }
        }, "Service not Available");
    }

    private String getFromLocationNameGlobalAb(String locationName, double lowerLeftLatitude, double lowerLeftLongitude, double upperRightLatitude, double upperRightLongitude, int maxResults, GeocoderParams params, List<Address> addrs) {
        return (String) this.mServiceWatcherAb.runOnBinderBlocking(new ServiceWatcher.BlockingBinderRunner(locationName, lowerLeftLatitude, lowerLeftLongitude, upperRightLatitude, upperRightLongitude, maxResults, params, addrs) {
            /* class com.android.server.location.$$Lambda$HwGeocoderProxy$ywm89zfnW26ea8xnjfBN9nHc9Qo */
            private final /* synthetic */ String f$0;
            private final /* synthetic */ double f$1;
            private final /* synthetic */ double f$2;
            private final /* synthetic */ double f$3;
            private final /* synthetic */ double f$4;
            private final /* synthetic */ int f$5;
            private final /* synthetic */ GeocoderParams f$6;
            private final /* synthetic */ List f$7;

            {
                this.f$0 = r1;
                this.f$1 = r2;
                this.f$2 = r4;
                this.f$3 = r6;
                this.f$4 = r8;
                this.f$5 = r10;
                this.f$6 = r11;
                this.f$7 = r12;
            }

            public final Object run(IBinder iBinder) {
                return IGeocodeProvider.Stub.asInterface(iBinder).getFromLocationName(this.f$0, this.f$1, this.f$2, this.f$3, this.f$4, this.f$5, this.f$6, this.f$7);
            }
        }, "Service not Available");
    }
}
