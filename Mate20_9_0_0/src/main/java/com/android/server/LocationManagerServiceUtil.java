package com.android.server;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Handler;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings.Secure;
import android.provider.Settings.System;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Slog;
import com.android.internal.content.PackageMonitor;
import com.android.server.LocationManagerService.Receiver;
import com.android.server.LocationManagerService.UpdateRecord;
import com.android.server.location.GeocoderProxy;
import com.android.server.location.GeocoderProxyUtils;
import com.android.server.location.LocationProviderInterface;
import com.android.server.location.LocationProviderProxy;
import com.android.server.location.LocationProviderProxyUtils;
import com.android.server.wifipro.WifiProCommonUtils;
import com.huawei.utils.reflect.EasyInvokeFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class LocationManagerServiceUtil {
    private static final String FUSEDPROXY_SERVICE_ACTION = "com.android.location.service.FusedProvider";
    private static final String FUSED_LOCATION_SERVICE_ACTION = "com.android.location.service.FusedLocationProvider";
    private static final String GEOCODER_SERVICE_ACTION = "com.android.location.service.GeocodeProvider";
    private static final String GEOFENCE_SERVICE_ACTION = "com.android.location.service.GeofenceProvider";
    public static final String GOOGLE_GMS_PROCESS = "com.google.android.gms";
    public static final String GOOGLE_GMS_UI_PROCESS = "com.google.android.gms.ui";
    public static final String GOOGLE_MAP_PROCESS = "com.google.android.apps.maps";
    private static final String GOOGLE_MAP_STATE = "google_map_state";
    public static final String GOOGLE_NETWORK_PROCESS = "com.google.process.location";
    private static final String NETWORK_LOCATION_SERVICE_ACTION = "com.android.location.service.v3.NetworkLocationProvider";
    public static final int STATE_INSTALLED_ONLY = 0;
    public static final int STATE_INSTALLED_RPOMPTED = 1;
    public static final int STATE_UNINSTALLED = 2;
    private static final String TAG = "LocationManagerServiceUtil";
    private static GeocoderProxyUtils geocoderProxyUtilsUtils = ((GeocoderProxyUtils) EasyInvokeFactory.getInvokeUtils(GeocoderProxyUtils.class));
    private static volatile LocationManagerServiceUtil instance;
    private static LocationProviderProxyUtils locationProviderProxyUtils = ((LocationProviderProxyUtils) EasyInvokeFactory.getInvokeUtils(LocationProviderProxyUtils.class));
    private static int pidGoogleLocation = -99;
    private static LocationManagerServiceUtils utils = ((LocationManagerServiceUtils) EasyInvokeFactory.getInvokeUtils(LocationManagerServiceUtils.class));
    private ActivityManager am = null;
    public int googleMapState = 2;
    private boolean isMultiNlpEnableFlag = false;
    private boolean isMultiNlpEnableLoaded = false;
    private Context mContext = null;
    private String mNLPcivil;
    private String mNLPforeign = GOOGLE_GMS_PROCESS;
    private final PackageMonitor mPackageMonitor = new PackageMonitor() {
        public void onPackageAdded(String packageName, int uid) {
            if (LocationManagerServiceUtil.GOOGLE_MAP_PROCESS.equals(packageName)) {
                String str = LocationManagerServiceUtil.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onPackageAdded ");
                stringBuilder.append(packageName);
                Slog.d(str, stringBuilder.toString());
                LocationManagerServiceUtil.this.setGoogleMapState(0);
            }
            super.onPackageAdded(packageName, uid);
        }

        public void onPackageRemoved(String packageName, int uid) {
            if (LocationManagerServiceUtil.GOOGLE_MAP_PROCESS.equals(packageName)) {
                String str = LocationManagerServiceUtil.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onPackageRemoved ");
                stringBuilder.append(packageName);
                Slog.d(str, stringBuilder.toString());
                LocationManagerServiceUtil.this.shouldMonitorGmsProcess = false;
                LocationManagerServiceUtil.this.setGoogleMapState(2);
            }
            super.onPackageRemoved(packageName, uid);
        }
    };
    private PhoneStateListener mPhoneStateListener;
    private LocationManagerService mService;
    private TelephonyManager mTelephonyManager;
    public boolean shouldMonitorGmsProcess = false;
    public boolean skipGooglePrompt = true;
    public boolean useGoogleNlpNow = false;

    public static void setPidGoogleLocation(int pid, String processName) {
        String str;
        StringBuilder stringBuilder;
        if (GOOGLE_NETWORK_PROCESS.equals(processName)) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("setPidGoogleLocation pid = ");
            stringBuilder.append(pid);
            Slog.d(str, stringBuilder.toString());
            pidGoogleLocation = pid;
        } else if (GOOGLE_MAP_PROCESS.equals(processName) && getDefault() != null && getDefault().isMultiNlpEnable() && getDefault().googleMapState == 0) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("setPidGoogleLocation processName = ");
            stringBuilder.append(processName);
            Slog.d(str, stringBuilder.toString());
            if (getDefault().isGmsProcessRunning()) {
                getDefault().sendProvidersChangedAction();
            } else {
                getDefault().shouldMonitorGmsProcess = true;
            }
        } else if (GOOGLE_GMS_PROCESS.equals(processName) && getDefault() != null && getDefault().isMultiNlpEnable() && getDefault().shouldMonitorGmsProcess) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("setPidGoogleLocation processName = ");
            stringBuilder.append(processName);
            Slog.d(str, stringBuilder.toString());
            getDefault().sendProvidersChangedAction();
            getDefault().shouldMonitorGmsProcess = false;
        }
    }

    public static LocationManagerServiceUtil getDefault(LocationManagerService service, Context context) {
        if (instance == null) {
            instance = new LocationManagerServiceUtil(service, context);
        }
        return instance;
    }

    public static LocationManagerServiceUtil getDefault() {
        return instance;
    }

    private LocationManagerServiceUtil(LocationManagerService service, Context context) {
        this.mService = service;
        this.mContext = context;
        SystemProperties.set("sys.show_google_nlp", "true");
        if ("CN".equalsIgnoreCase(SystemProperties.get(WifiProCommonUtils.KEY_PROP_LOCALE, ""))) {
            SystemProperties.set("sys.show_google_nlp", "false");
            this.googleMapState = getGoogleMapState();
            this.mPackageMonitor.register(this.mContext, null, UserHandle.ALL, true);
            this.mPhoneStateListener = new PhoneStateListener() {
                public void onServiceStateChanged(ServiceState state) {
                    if (state != null) {
                        String numeric = state.getOperatorNumeric();
                        String str = LocationManagerServiceUtil.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("ServiceStateChanged numeric = ");
                        stringBuilder.append(numeric);
                        Slog.d(str, stringBuilder.toString());
                        if (numeric != null && numeric.length() >= 5 && numeric.substring(0, 5).equals("99999")) {
                            return;
                        }
                        String pkgName;
                        String str2;
                        StringBuilder stringBuilder2;
                        if (numeric != null && numeric.length() >= 3 && numeric.substring(0, 3).equals(WifiProCommonUtils.COUNTRY_CODE_CN)) {
                            pkgName = LocationManagerServiceUtil.this.mNLPcivil;
                            str2 = LocationManagerServiceUtil.TAG;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("ServiceStateChanged use pkgName = ");
                            stringBuilder2.append(pkgName);
                            Slog.d(str2, stringBuilder2.toString());
                            LocationManagerServiceUtil.this.useGoogleNlpNow = false;
                            LocationManagerServiceUtil.this.skipGooglePrompt = true;
                            SystemProperties.set("sys.show_google_nlp", "false");
                            LocationManagerServiceUtil.this.bindSpecificService(pkgName, 10, false);
                        } else if (numeric != null && !numeric.equals("")) {
                            pkgName = LocationManagerServiceUtil.this.mNLPcivil;
                            str2 = LocationManagerServiceUtil.TAG;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("ServiceStateChanged use pkgName = ");
                            stringBuilder2.append(pkgName);
                            Slog.d(str2, stringBuilder2.toString());
                            LocationManagerServiceUtil.this.useGoogleNlpNow = false;
                            LocationManagerServiceUtil.this.skipGooglePrompt = false;
                            SystemProperties.set("sys.show_google_nlp", "false");
                            LocationManagerServiceUtil.this.bindSpecificService(pkgName, 10, false);
                        }
                    }
                }
            };
            if (isMultiNlpEnable()) {
                this.mTelephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
                this.mTelephonyManager.listen(this.mPhoneStateListener, 1);
            }
        }
        this.am = (ActivityManager) this.mContext.getSystemService("activity");
    }

    private boolean isGmsProcessRunning() {
        List<RunningAppProcessInfo> appProcessList = ((ActivityManager) this.mContext.getSystemService("activity")).getRunningAppProcesses();
        if (appProcessList == null) {
            return false;
        }
        for (RunningAppProcessInfo appProcess : appProcessList) {
            if (GOOGLE_GMS_PROCESS.equals(appProcess.processName)) {
                return true;
            }
        }
        return false;
    }

    private void setGoogleMapState(int state) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setGoogleMapState. ");
        stringBuilder.append(state);
        Slog.d(str, stringBuilder.toString());
        System.putInt(this.mContext.getContentResolver(), GOOGLE_MAP_STATE, state);
        this.googleMapState = state;
    }

    private int getGoogleMapState() {
        int state = System.getInt(this.mContext.getContentResolver(), GOOGLE_MAP_STATE, 2);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getGoogleMapState. ");
        stringBuilder.append(state);
        Slog.d(str, stringBuilder.toString());
        return state;
    }

    private void sendProvidersChangedAction() {
        Slog.d(TAG, "sendProvidersChangedAction.");
        setGoogleMapState(1);
        if (Secure.getInt(getDefault().mContext.getContentResolver(), "location_mode", 0) != 0) {
            this.mContext.sendBroadcastAsUser(new Intent("android.location.PROVIDERS_CHANGED"), UserHandle.ALL);
            this.mContext.sendBroadcastAsUser(new Intent("android.location.MODE_CHANGED"), UserHandle.ALL);
        }
    }

    public HashMap<String, LocationProviderInterface> getRealProviders() {
        return utils.getRealProviders(this.mService);
    }

    public Set<String> getEnabledProviders() {
        return utils.getEnabledProviders(this.mService);
    }

    public Handler getLocationHandler() {
        return utils.getLocationHandler(this.mService);
    }

    public Object getmLock() {
        return utils.getmLock(this.mService);
    }

    public HashMap<Object, Receiver> getReceivers() {
        return utils.getReceivers(this.mService);
    }

    public void removeUpdatesLocked(Receiver receiver) {
        utils.removeUpdatesLocked(this.mService, receiver);
    }

    private ArrayList<LocationProviderProxy> getProxyProviders() {
        return utils.getProxyProviders(this.mService);
    }

    public void addProviderLocked(LocationProviderInterface provider) {
        utils.addProviderLocked(this.mService, provider);
    }

    private void setGeocodeProvider(GeocoderProxy geocodeProvider) {
        utils.setGeocodeProvider(this.mService, geocodeProvider);
    }

    private GeocoderProxy getGeocoderProvider() {
        return utils.getGeocodeProvider(this.mService);
    }

    private ServiceWatcher getLocationProviderProxyWatcher(LocationProviderProxy locationProviderProxy) {
        return locationProviderProxyUtils.getServiceWatcher(locationProviderProxy);
    }

    private ServiceWatcher getgetGeocoderServiceWatcher(GeocoderProxy geocoderProxy) {
        return geocoderProxyUtilsUtils.getServiceWatcher(geocoderProxy);
    }

    private void updateProvidersLocked() {
        utils.updateProvidersLocked(this.mService);
    }

    public boolean isMockProvider(String provider) {
        return utils.isMockProvider(this.mService, provider).booleanValue();
    }

    public HashMap<String, LocationProviderInterface> getProvidersByName() {
        return utils.getProvidersByName(this.mService);
    }

    public HashMap<String, ArrayList<UpdateRecord>> getRecordsByProvider() {
        return utils.getRecordsByProvider(this.mService);
    }

    public boolean isAllowedByCurrentUserSettingsLocked(String provider) {
        return utils.isAllowedByCurrentUserSettingsLocked(this.mService, provider).booleanValue();
    }

    public boolean isMultiNlpEnable() {
        if (!this.isMultiNlpEnableLoaded) {
            if ("CN".equalsIgnoreCase(SystemProperties.get(WifiProCommonUtils.KEY_PROP_LOCALE, ""))) {
                PackageInfo foreignPackage = null;
                PackageInfo civilPackage = null;
                try {
                    foreignPackage = this.mContext.getPackageManager().getPackageInfo(this.mNLPforeign, 4);
                } catch (NameNotFoundException e) {
                    Slog.e(TAG, "missing foreignPackage");
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
                this.mNLPcivil = SystemProperties.get("ro.config.hw_nlp", "com.baidu.map.location");
                try {
                    civilPackage = this.mContext.getPackageManager().getPackageInfo(this.mNLPcivil, 4);
                } catch (NameNotFoundException e3) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("missing mNLPcivil: ");
                    stringBuilder.append(this.mNLPcivil);
                    Slog.e(str, stringBuilder.toString());
                } catch (Exception e4) {
                    e4.printStackTrace();
                }
                if (!(TextUtils.isEmpty(this.mNLPcivil) || foreignPackage == null || civilPackage == null)) {
                    this.isMultiNlpEnableFlag = true;
                }
            }
            this.isMultiNlpEnableLoaded = true;
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("isMultiNlpEnable: isMultiNlpEnableFlag = ");
            stringBuilder2.append(this.isMultiNlpEnableFlag);
            Slog.e(str2, stringBuilder2.toString());
        }
        return this.isMultiNlpEnableFlag;
    }

    public static boolean useCivilNlpPackage(String action, String packageName) {
        if ((GEOFENCE_SERVICE_ACTION.equals(action) || FUSED_LOCATION_SERVICE_ACTION.equals(action) || FUSEDPROXY_SERVICE_ACTION.equals(action)) && getDefault() != null && getDefault().isMultiNlpEnable()) {
            if (getDefault().useGoogleNlpNow) {
                if (getDefault().mNLPforeign.equals(packageName)) {
                    return true;
                }
            } else if (getDefault().mNLPcivil != null && getDefault().mNLPcivil.equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    public static boolean skipForeignNlpPackage(String action, String packageName) {
        if ((GEOFENCE_SERVICE_ACTION.equals(action) || FUSED_LOCATION_SERVICE_ACTION.equals(action) || FUSEDPROXY_SERVICE_ACTION.equals(action)) && getDefault() != null && getDefault().isMultiNlpEnable() && !getDefault().useGoogleNlpNow && getDefault().mNLPforeign.equals(packageName)) {
            return true;
        }
        return false;
    }

    private void bindSpecificService(String packageName, int version, boolean bind) {
        LocationProviderProxy networkProvider = (LocationProviderProxy) getRealProviders().get("network");
        GeocoderProxy geocoderProxy = getGeocoderProvider();
        boolean needUpdateProvider = false;
        if (bind) {
            if (networkProvider != null) {
                bindSpecificService(getLocationProviderProxyWatcher(networkProvider), packageName, version);
            } else {
                Slog.e(TAG, "bindSpecificService: no network location provider found, init again");
                needUpdateProvider = true;
                networkProvider = LocationProviderProxy.createAndBind(this.mContext, "network", NETWORK_LOCATION_SERVICE_ACTION, 17956962, 17039832, 17236014, getLocationHandler());
                if (networkProvider != null) {
                    getRealProviders().put("network", networkProvider);
                    getProxyProviders().add(networkProvider);
                    addProviderLocked(networkProvider);
                }
            }
            if (geocoderProxy != null) {
                bindSpecificService(getgetGeocoderServiceWatcher(getGeocoderProvider()), packageName, version);
            } else {
                Slog.e(TAG, "bindSpecificService: no geocoder provider found, init agan");
                needUpdateProvider = true;
                setGeocodeProvider(GeocoderProxy.createAndBind(this.mContext, 17956954, 17039808, 17236014, getLocationHandler()));
            }
            if (needUpdateProvider) {
                updateProvidersLocked();
            }
        }
    }

    private void bindSpecificService(ServiceWatcher serviceWatcher, String packageName, int version) {
        if (serviceWatcher != null) {
            if (packageName.equals(serviceWatcher.getBestPackageName())) {
                Slog.d(TAG, "bindSpecificService: same package name, return");
                return;
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("bindSpecificService old mPackageName = ");
            stringBuilder.append(serviceWatcher.getBestPackageName());
            Slog.d(str, stringBuilder.toString());
            str = serviceWatcher.getBestPackageName();
            if (this.mNLPcivil.equals(str)) {
                Slog.d(TAG, "currentPackageName is civil, force Stop");
                this.am.forceStopPackage(str);
            }
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("bindSpecificService new packageName = ");
            stringBuilder2.append(packageName);
            Slog.d(str2, stringBuilder2.toString());
            serviceWatcher.bindToPackageWithLock(packageName, version, false);
        }
    }

    public static boolean shouldSkipGoogleNlp(int pid) {
        if (pid == pidGoogleLocation) {
            String str;
            if (getDefault() == null || !getDefault().isMultiNlpEnable() || !getDefault().skipGooglePrompt || 1 == getDefault().googleMapState) {
                str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("shouldSkipGoogleNlp return false skip process ");
                stringBuilder.append(pid);
                Slog.w(str, stringBuilder.toString());
            } else {
                str = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("skip process ");
                stringBuilder2.append(pid);
                Slog.w(str, stringBuilder2.toString());
                return true;
            }
        }
        return false;
    }

    public Context getContext() {
        return this.mContext;
    }

    public void clearPackageLocation(String packageName) {
        synchronized (getmLock()) {
            Iterator it;
            ArrayList<Receiver> deadReceivers = null;
            for (Receiver receiver : getReceivers().values()) {
                if (receiver.mIdentity.mPackageName.equals(packageName)) {
                    if (deadReceivers == null) {
                        deadReceivers = new ArrayList();
                    }
                    deadReceivers.add(receiver);
                }
            }
            if (deadReceivers != null) {
                it = deadReceivers.iterator();
                while (it.hasNext()) {
                    removeUpdatesLocked((Receiver) it.next());
                }
            }
        }
    }
}
