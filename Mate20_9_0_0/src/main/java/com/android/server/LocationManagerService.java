package com.android.server;

import android.app.ActivityManager;
import android.app.ActivityManager.OnUidImportanceListener;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.AppOpsManager;
import android.app.AppOpsManager.OnOpChangedInternalListener;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.app.PendingIntent.OnFinished;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PackageManager.OnPermissionsChangedListener;
import android.content.pm.PackageManagerInternal;
import android.content.pm.PackageManagerInternal.PackagesProvider;
import android.content.pm.ResolveInfo;
import android.content.pm.Signature;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.hardware.location.ActivityRecognitionHardware;
import android.hdm.HwDeviceManager;
import android.hsm.HwSystemManager;
import android.location.Address;
import android.location.Criteria;
import android.location.GeocoderParams;
import android.location.Geofence;
import android.location.IBatchedLocationCallback;
import android.location.IGnssMeasurementsListener;
import android.location.IGnssMeasurementsListener.Stub;
import android.location.IGnssNavigationMessageListener;
import android.location.IGnssStatusListener;
import android.location.IGnssStatusProvider;
import android.location.IGpsGeofenceHardware;
import android.location.ILocationListener;
import android.location.INetInitiatedListener;
import android.location.Location;
import android.location.LocationProvider;
import android.location.LocationRequest;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.WorkSource;
import android.os.WorkSource.WorkChain;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.EventLog;
import android.util.Log;
import android.util.Slog;
import com.android.internal.content.PackageMonitor;
import com.android.internal.location.ProviderProperties;
import com.android.internal.location.ProviderRequest;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.DumpUtils;
import com.android.server.am.HwBroadcastRadarUtil;
import com.android.server.backup.BackupAgentTimeoutParameters;
import com.android.server.location.ActivityRecognitionProxy;
import com.android.server.location.GeocoderProxy;
import com.android.server.location.GeofenceManager;
import com.android.server.location.GeofenceProxy;
import com.android.server.location.GnssBatchingProvider;
import com.android.server.location.GnssLocationProvider;
import com.android.server.location.GnssLocationProvider.GnssMetricsProvider;
import com.android.server.location.GnssLocationProvider.GnssSystemInfoProvider;
import com.android.server.location.GnssMeasurementsProvider;
import com.android.server.location.GnssNavigationMessageProvider;
import com.android.server.location.GpsFreezeListener;
import com.android.server.location.GpsFreezeProc;
import com.android.server.location.HwQuickTTFFMonitor;
import com.android.server.location.IHwGpsActionReporter;
import com.android.server.location.IHwGpsLogServices;
import com.android.server.location.IHwLbsLogger;
import com.android.server.location.IHwLocalLocationProvider;
import com.android.server.location.IHwLocationProviderInterface;
import com.android.server.location.LocationBlacklist;
import com.android.server.location.LocationFudger;
import com.android.server.location.LocationProviderInterface;
import com.android.server.location.LocationProviderProxy;
import com.android.server.location.LocationRequestStatistics;
import com.android.server.location.LocationRequestStatistics.PackageProviderKey;
import com.android.server.location.LocationRequestStatistics.PackageStatistics;
import com.android.server.location.MockProvider;
import com.android.server.location.PassiveProvider;
import com.android.server.pm.DumpState;
import com.android.server.rms.IHwIpcMonitor;
import com.huawei.pgmng.log.LogPower;
import huawei.android.security.IHwBehaviorCollectManager.BehaviorId;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;

public class LocationManagerService extends AbsLocationManagerService {
    private static final String ACCESS_LOCATION_EXTRA_COMMANDS = "android.permission.ACCESS_LOCATION_EXTRA_COMMANDS";
    private static final String ACCESS_MOCK_LOCATION = "android.permission.ACCESS_MOCK_LOCATION";
    private static final String APKSTART = "apkstart";
    private static final String APKSTOP = "apkstop";
    protected static final long CHECK_LOCATION_INTERVAL = 300000;
    public static final boolean D = false;
    private static final long DEFAULT_BACKGROUND_THROTTLE_INTERVAL_MS = 1800000;
    private static final LocationRequest DEFAULT_LOCATION_REQUEST = new LocationRequest();
    private static final int FOREGROUND_IMPORTANCE_CUTOFF = 125;
    private static final String FUSED_LOCATION_SERVICE_ACTION = "com.android.location.service.FusedLocationProvider";
    private static final long HIGH_POWER_INTERVAL_MS = 300000;
    private static final String INSTALL_LOCATION_PROVIDER = "android.permission.INSTALL_LOCATION_PROVIDER";
    private static final int MAX_PROVIDER_SCHEDULING_JITTER_MS = 100;
    protected static final int MSG_CHECK_LOCATION = 7;
    private static final int MSG_GPSFREEZEPROC_LISTNER = 4;
    private static final int MSG_LOCATION_CHANGED = 1;
    private static final int MSG_LOCATION_REMOVE = 3;
    private static final int MSG_LOCATION_REQUEST = 2;
    private static final int MSG_SUPERVISORY_CONTROL = 6;
    private static final int MSG_WHITELIST_LISTNER = 5;
    private static final long NANOS_PER_MILLI = 1000000;
    private static final String NETWORK_LOCATION_SERVICE_ACTION = "com.android.location.service.v3.NetworkLocationProvider";
    private static final int RESOLUTION_LEVEL_COARSE = 1;
    private static final int RESOLUTION_LEVEL_FINE = 2;
    private static final int RESOLUTION_LEVEL_NONE = 0;
    private static final String TAG = "LocationManagerService";
    private static final String WAKELOCK_KEY = "*location*";
    private ActivityManager mActivityManager;
    private final AppOpsManager mAppOps;
    private final ArraySet<String> mBackgroundThrottlePackageWhitelist = new ArraySet();
    private LocationBlacklist mBlacklist;
    private boolean mCHRFirstReqFlag = false;
    protected final Context mContext;
    private int mCurrentUserId = 0;
    private int[] mCurrentUserProfiles = new int[]{0};
    private final Set<String> mDisabledProviders = new HashSet();
    private final Set<String> mEnabledProviders = new HashSet();
    private GeocoderProxy mGeocodeProvider;
    private GeofenceManager mGeofenceManager;
    private IBatchedLocationCallback mGnssBatchingCallback;
    private LinkedCallback mGnssBatchingDeathCallback;
    private boolean mGnssBatchingInProgress = false;
    private GnssBatchingProvider mGnssBatchingProvider;
    private final ArrayMap<IBinder, Identity> mGnssMeasurementsListeners = new ArrayMap();
    private GnssMeasurementsProvider mGnssMeasurementsProvider;
    private GnssMetricsProvider mGnssMetricsProvider;
    private final ArrayMap<IBinder, Identity> mGnssNavigationMessageListeners = new ArrayMap();
    private GnssNavigationMessageProvider mGnssNavigationMessageProvider;
    private IGnssStatusProvider mGnssStatusProvider;
    private GnssSystemInfoProvider mGnssSystemInfoProvider;
    private IGpsGeofenceHardware mGpsGeofenceProxy;
    private IHwGpsActionReporter mHwGpsActionReporter;
    private IHwLbsLogger mHwLbsLogger;
    private IHwGpsLogServices mHwLocationGpsLogServices;
    private HwQuickTTFFMonitor mHwQuickTTFFMonitor;
    private final HashMap<String, Location> mLastLocation = new HashMap();
    private final HashMap<String, Location> mLastLocationCoarseInterval = new HashMap();
    protected IHwLocalLocationProvider mLocalLocationProvider;
    private LocationFudger mLocationFudger;
    private LocationWorkerHandler mLocationHandler;
    private IHwIpcMonitor mLocationIpcMonitor;
    HandlerThread mLocationThread;
    private final Object mLock = new Object();
    private final HashMap<String, MockProvider> mMockProviders = new HashMap();
    private INetInitiatedListener mNetInitiatedListener;
    private PackageManager mPackageManager;
    private final PackageMonitor mPackageMonitor = new PackageMonitor() {
        public void onPackageDisappeared(String packageName, int reason) {
            synchronized (LocationManagerService.this.mLock) {
                Iterator it;
                ArrayList<Receiver> deadReceivers = null;
                for (Receiver receiver : LocationManagerService.this.mReceivers.values()) {
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
                        LocationManagerService.this.removeUpdatesLocked((Receiver) it.next());
                    }
                }
            }
        }
    };
    private PassiveProvider mPassiveProvider;
    private PowerManager mPowerManager;
    private final ArrayList<LocationProviderInterface> mProviders = new ArrayList();
    private final HashMap<String, LocationProviderInterface> mProvidersByName = new HashMap();
    private final ArrayList<LocationProviderProxy> mProxyProviders = new ArrayList();
    private final HashMap<String, LocationProviderInterface> mRealProviders = new HashMap();
    private final HashMap<Object, Receiver> mReceivers = new HashMap();
    private final HashMap<String, ArrayList<UpdateRecord>> mRecordsByProvider = new HashMap();
    private final LocationRequestStatistics mRequestStatistics = new LocationRequestStatistics();
    private UserManager mUserManager;

    public static final class Identity {
        final String mPackageName;
        final int mPid;
        final int mUid;

        Identity(int uid, int pid, String packageName) {
            this.mUid = uid;
            this.mPid = pid;
            this.mPackageName = packageName;
        }
    }

    private class LinkedCallback implements DeathRecipient {
        private final IBatchedLocationCallback mCallback;

        public LinkedCallback(IBatchedLocationCallback callback) {
            this.mCallback = callback;
        }

        public IBatchedLocationCallback getUnderlyingListener() {
            return this.mCallback;
        }

        public void binderDied() {
            String str = LocationManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Remote Batching Callback died: ");
            stringBuilder.append(this.mCallback);
            Log.d(str, stringBuilder.toString());
            LocationManagerService.this.stopGnssBatch();
            LocationManagerService.this.removeGnssBatchingCallback();
        }
    }

    private class LocationWorkerHandler extends Handler {
        public LocationWorkerHandler(Looper looper) {
            super(looper, null, true);
        }

        public void handleMessage(Message msg) {
            boolean z = false;
            switch (msg.what) {
                case 1:
                    LocationManagerService locationManagerService = LocationManagerService.this;
                    Location location = (Location) msg.obj;
                    if (msg.arg1 == 1) {
                        z = true;
                    }
                    locationManagerService.handleLocationChanged(location, z);
                    break;
                case 2:
                    LocationManagerService.this.mHwGpsActionReporter.uploadLocationAction(1, (String) msg.obj);
                    break;
                case 3:
                    LocationManagerService.this.mHwGpsActionReporter.uploadLocationAction(0, (String) msg.obj);
                    break;
                case 4:
                    boolean isFound = false;
                    String pkg = msg.obj;
                    synchronized (LocationManagerService.this.mLock) {
                        Iterator it;
                        ArrayList<UpdateRecord> records = (ArrayList) LocationManagerService.this.mRecordsByProvider.get("gps");
                        if (records != null) {
                            Iterator it2 = records.iterator();
                            while (it2.hasNext()) {
                                if (((UpdateRecord) it2.next()).mReceiver.mIdentity.mPackageName.equals(pkg)) {
                                    String str = LocationManagerService.TAG;
                                    StringBuilder stringBuilder = new StringBuilder();
                                    stringBuilder.append(" GpsFreezeProc pkgname in gps request:");
                                    stringBuilder.append(pkg);
                                    Log.i(str, stringBuilder.toString());
                                    isFound = true;
                                }
                            }
                        }
                        if (isFound) {
                            LocationManagerService.this.applyRequirementsLocked("gps");
                        }
                        boolean isNetworkFound = false;
                        ArrayList<UpdateRecord> networkRecords = (ArrayList) LocationManagerService.this.mRecordsByProvider.get("network");
                        if (networkRecords != null) {
                            it = networkRecords.iterator();
                            while (it.hasNext()) {
                                if (((UpdateRecord) it.next()).mReceiver.mIdentity.mPackageName.equals(pkg)) {
                                    String str2 = LocationManagerService.TAG;
                                    StringBuilder stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append(" GpsFreezeProc pkgname in network request:");
                                    stringBuilder2.append(pkg);
                                    Log.i(str2, stringBuilder2.toString());
                                    isNetworkFound = true;
                                }
                            }
                        }
                        if (isNetworkFound) {
                            LocationManagerService.this.applyRequirementsLocked("network");
                        }
                        for (Entry<IBinder, Identity> entry : LocationManagerService.this.mGnssMeasurementsListeners.entrySet()) {
                            if (pkg != null && pkg.equals(((Identity) entry.getValue()).mPackageName)) {
                                boolean isFreeze = LocationManagerService.this.isFreeze(((Identity) entry.getValue()).mPackageName);
                                String str3 = LocationManagerService.TAG;
                                StringBuilder stringBuilder3 = new StringBuilder();
                                stringBuilder3.append("gnss measurements listener from ");
                                stringBuilder3.append(((Identity) entry.getValue()).mPackageName);
                                stringBuilder3.append(" is freeze = ");
                                stringBuilder3.append(isFreeze);
                                Log.d(str3, stringBuilder3.toString());
                                if (isFreeze) {
                                    LocationManagerService.this.mGnssMeasurementsProvider.removeListener(Stub.asInterface((IBinder) entry.getKey()));
                                } else {
                                    LocationManagerService.this.mGnssMeasurementsProvider.addListener(Stub.asInterface((IBinder) entry.getKey()));
                                }
                            }
                        }
                    }
                case 5:
                    List<String> pkgList = msg.obj;
                    int type = msg.arg1;
                    LocationManagerService.this.mHwQuickTTFFMonitor;
                    if (type == 3) {
                        LocationManagerService.this.mHwQuickTTFFMonitor.updateAccWhiteList(pkgList);
                        break;
                    }
                    LocationManagerService.this.mHwQuickTTFFMonitor;
                    if (type == 4) {
                        LocationManagerService.this.mHwQuickTTFFMonitor.updateDisableList(pkgList);
                        break;
                    } else if (type == 1) {
                        synchronized (LocationManagerService.this.mLock) {
                            LocationManagerService.this.updateBackgroundThrottlingWhitelistLocked();
                            LocationManagerService.this.updateProvidersLocked();
                        }
                    }
                    break;
                case 6:
                    ArrayList<String> providers = msg.obj;
                    synchronized (LocationManagerService.this.mLock) {
                        Iterator it3 = providers.iterator();
                        while (it3.hasNext()) {
                            LocationManagerService.this.applyRequirementsLocked((String) it3.next());
                        }
                    }
                default:
                    if (!LocationManagerService.this.hwLocationHandleMessage(msg)) {
                        Log.e(LocationManagerService.TAG, "receive unexpected message");
                        break;
                    }
                    return;
            }
        }
    }

    public class Receiver implements DeathRecipient, OnFinished {
        long mAcquireLockTime;
        final int mAllowedResolutionLevel;
        final boolean mHideFromAppOps;
        final Identity mIdentity;
        final Object mKey;
        final ILocationListener mListener;
        boolean mOpHighPowerMonitoring;
        boolean mOpMonitoring;
        int mPendingBroadcasts;
        final PendingIntent mPendingIntent;
        long mReleaseLockTime;
        final HashMap<String, UpdateRecord> mUpdateRecords = new HashMap();
        WakeLock mWakeLock;
        final WorkSource mWorkSource;

        Receiver(ILocationListener listener, PendingIntent intent, int pid, int uid, String packageName, WorkSource workSource, boolean hideFromAppOps) {
            this.mListener = listener;
            this.mPendingIntent = intent;
            if (listener != null) {
                this.mKey = listener.asBinder();
            } else {
                this.mKey = intent;
            }
            this.mAllowedResolutionLevel = LocationManagerService.this.getAllowedResolutionLevel(pid, uid);
            this.mIdentity = new Identity(uid, pid, packageName);
            if (workSource != null && workSource.isEmpty()) {
                workSource = null;
            }
            this.mWorkSource = workSource;
            this.mHideFromAppOps = hideFromAppOps;
            updateMonitoring(true);
            this.mWakeLock = LocationManagerService.this.mPowerManager.newWakeLock(1, LocationManagerService.WAKELOCK_KEY);
            if (workSource == null) {
                workSource = new WorkSource(this.mIdentity.mUid, this.mIdentity.mPackageName);
            }
            this.mWakeLock.setWorkSource(workSource);
        }

        public boolean equals(Object otherObj) {
            return (otherObj instanceof Receiver) && this.mKey.equals(((Receiver) otherObj).mKey);
        }

        public int hashCode() {
            return this.mKey.hashCode();
        }

        public String toString() {
            StringBuilder s = new StringBuilder();
            s.append("Reciever[");
            s.append(Integer.toHexString(System.identityHashCode(this)));
            if (this.mListener != null) {
                s.append(" listener");
            } else {
                s.append(" intent");
            }
            for (String p : this.mUpdateRecords.keySet()) {
                s.append(" ");
                s.append(((UpdateRecord) this.mUpdateRecords.get(p)).toString());
            }
            s.append("]");
            return s.toString();
        }

        public void updateMonitoring(boolean allow) {
            if (!this.mHideFromAppOps) {
                boolean requestingLocation = false;
                boolean requestingHighPowerLocation = false;
                if (allow) {
                    for (UpdateRecord updateRecord : this.mUpdateRecords.values()) {
                        if (LocationManagerService.this.isAllowedByCurrentUserSettingsLocked(updateRecord.mProvider)) {
                            requestingLocation = true;
                            LocationProviderInterface locationProvider = (LocationProviderInterface) LocationManagerService.this.mProvidersByName.get(updateRecord.mProvider);
                            ProviderProperties properties = locationProvider != null ? locationProvider.getProperties() : null;
                            String str = LocationManagerService.TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("mPackageName=");
                            stringBuilder.append(this.mIdentity.mPackageName);
                            stringBuilder.append(", interval=");
                            stringBuilder.append(updateRecord.mRequest.getInterval());
                            stringBuilder.append(",provider:");
                            stringBuilder.append(updateRecord.mProvider);
                            Log.i(str, stringBuilder.toString());
                            if (properties != null && properties.mPowerRequirement == 3 && updateRecord.mRealRequest.getInterval() < BackupAgentTimeoutParameters.DEFAULT_FULL_BACKUP_AGENT_TIMEOUT_MILLIS) {
                                requestingHighPowerLocation = true;
                                break;
                            }
                        }
                    }
                }
                String str2 = LocationManagerService.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("requestingHighPowerLocation =");
                stringBuilder2.append(requestingHighPowerLocation);
                Log.i(str2, stringBuilder2.toString());
                boolean wasOpMonitoring = this.mOpMonitoring;
                this.mOpMonitoring = updateMonitoring(requestingLocation, this.mOpMonitoring, 41);
                if (this.mOpMonitoring != wasOpMonitoring) {
                    LocationManagerService.this.hwSendLocationChangedAction(LocationManagerService.this.mContext, this.mIdentity.mPackageName);
                }
                boolean wasHighPowerMonitoring = this.mOpHighPowerMonitoring;
                this.mOpHighPowerMonitoring = updateMonitoring(requestingHighPowerLocation, this.mOpHighPowerMonitoring, 42);
                if (this.mOpHighPowerMonitoring != wasHighPowerMonitoring) {
                    Intent intent = new Intent("android.location.HIGH_POWER_REQUEST_CHANGE");
                    intent.putExtra("isFrameworkBroadcast", "true");
                    LocationManagerService.this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
                }
            }
        }

        private boolean updateMonitoring(boolean allowMonitoring, boolean currentlyMonitoring, int op) {
            boolean z = false;
            if (currentlyMonitoring) {
                if (!(allowMonitoring && LocationManagerService.this.mAppOps.checkOpNoThrow(op, this.mIdentity.mUid, this.mIdentity.mPackageName) == 0)) {
                    LocationManagerService.this.mAppOps.finishOp(op, this.mIdentity.mUid, this.mIdentity.mPackageName);
                    return false;
                }
            } else if (allowMonitoring) {
                if (LocationManagerService.this.mAppOps.startOpNoThrow(op, this.mIdentity.mUid, this.mIdentity.mPackageName) == 0) {
                    z = true;
                }
                return z;
            }
            return currentlyMonitoring;
        }

        public boolean isListener() {
            return this.mListener != null;
        }

        public boolean isPendingIntent() {
            return this.mPendingIntent != null;
        }

        public ILocationListener getListener() {
            if (this.mListener != null) {
                return this.mListener;
            }
            throw new IllegalStateException("Request for non-existent listener");
        }

        public boolean callStatusChangedLocked(String provider, int status, Bundle extras) {
            if (this.mListener != null) {
                try {
                    synchronized (this) {
                        if (LocationManagerService.this.isFreeze(this.mIdentity.mPackageName)) {
                            return true;
                        }
                        String str = LocationManagerService.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("callStatusChangedLocked receiver ");
                        stringBuilder.append(Integer.toHexString(System.identityHashCode(this)));
                        stringBuilder.append(" mPendingBroadcasts=");
                        stringBuilder.append(this.mPendingBroadcasts);
                        Log.i(str, stringBuilder.toString());
                        this.mListener.onStatusChanged(provider, status, extras);
                        incrementPendingBroadcastsLocked();
                    }
                } catch (RemoteException e) {
                    return false;
                }
            }
            Intent statusChanged = new Intent();
            statusChanged.putExtras(new Bundle(extras));
            statusChanged.putExtra("status", status);
            try {
                synchronized (this) {
                    if (LocationManagerService.this.isFreeze(this.mIdentity.mPackageName)) {
                        return true;
                    }
                    this.mPendingIntent.send(LocationManagerService.this.mContext, 0, statusChanged, this, LocationManagerService.this.mLocationHandler, LocationManagerService.this.getResolutionPermission(this.mAllowedResolutionLevel), PendingIntentUtils.createDontSendToRestrictedAppsBundle(null));
                    incrementPendingBroadcastsLocked();
                }
            } catch (CanceledException e2) {
                return false;
            }
            return true;
        }

        public boolean callLocationChangedLocked(Location location) {
            if (this.mListener != null) {
                try {
                    synchronized (this) {
                        String str;
                        StringBuilder stringBuilder;
                        if (LocationManagerService.this.isFreeze(this.mIdentity.mPackageName)) {
                            str = LocationManagerService.TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("PackageName: ");
                            stringBuilder.append(this.mIdentity.mPackageName);
                            Log.i(str, stringBuilder.toString());
                            return true;
                        }
                        str = LocationManagerService.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("key of receiver: ");
                        stringBuilder.append(Integer.toHexString(System.identityHashCode(this)));
                        stringBuilder.append(" mPendingBroadcasts=");
                        stringBuilder.append(this.mPendingBroadcasts);
                        Log.i(str, stringBuilder.toString());
                        this.mListener.onLocationChanged(new Location(location));
                        incrementPendingBroadcastsLocked();
                    }
                } catch (RemoteException e) {
                    return false;
                }
            }
            Intent locationChanged = new Intent();
            locationChanged.putExtra("location", new Location(location));
            locationChanged.addHwFlags(512);
            try {
                synchronized (this) {
                    if (LocationManagerService.this.isFreeze(this.mIdentity.mPackageName)) {
                        return true;
                    }
                    this.mPendingIntent.send(LocationManagerService.this.mContext, 0, locationChanged, this, LocationManagerService.this.mLocationHandler, LocationManagerService.this.getResolutionPermission(this.mAllowedResolutionLevel), PendingIntentUtils.createDontSendToRestrictedAppsBundle(null));
                    incrementPendingBroadcastsLocked();
                }
            } catch (CanceledException e2) {
                return false;
            }
            return true;
        }

        public boolean callProviderEnabledLocked(String provider, boolean enabled) {
            updateMonitoring(true);
            if (this.mListener != null) {
                try {
                    synchronized (this) {
                        if (LocationManagerService.this.isFreeze(this.mIdentity.mPackageName)) {
                            return true;
                        }
                        String str = LocationManagerService.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("callProviderEnabledLocked receiver ");
                        stringBuilder.append(Integer.toHexString(System.identityHashCode(this)));
                        stringBuilder.append(" mPendingBroadcasts=");
                        stringBuilder.append(this.mPendingBroadcasts);
                        Log.i(str, stringBuilder.toString());
                        if (enabled) {
                            this.mListener.onProviderEnabled(provider);
                        } else {
                            this.mListener.onProviderDisabled(provider);
                        }
                        incrementPendingBroadcastsLocked();
                    }
                } catch (RemoteException e) {
                    return false;
                }
            }
            Intent providerIntent = new Intent();
            providerIntent.putExtra("providerEnabled", enabled);
            try {
                synchronized (this) {
                    if (LocationManagerService.this.isFreeze(this.mIdentity.mPackageName)) {
                        return true;
                    }
                    this.mPendingIntent.send(LocationManagerService.this.mContext, 0, providerIntent, this, LocationManagerService.this.mLocationHandler, LocationManagerService.this.getResolutionPermission(this.mAllowedResolutionLevel), PendingIntentUtils.createDontSendToRestrictedAppsBundle(null));
                    incrementPendingBroadcastsLocked();
                }
            } catch (CanceledException e2) {
                return false;
            }
            return true;
        }

        public void binderDied() {
            Log.i(LocationManagerService.TAG, "Location listener died");
            synchronized (LocationManagerService.this.mLock) {
                LocationManagerService.this.removeUpdatesLocked(this);
            }
            synchronized (this) {
                clearPendingBroadcastsLocked();
            }
        }

        public void onSendFinished(PendingIntent pendingIntent, Intent intent, int resultCode, String resultData, Bundle resultExtras) {
            synchronized (this) {
                decrementPendingBroadcastsLocked();
            }
        }

        private void incrementPendingBroadcastsLocked() {
            int i = this.mPendingBroadcasts;
            this.mPendingBroadcasts = i + 1;
            if (i == 0) {
                this.mAcquireLockTime = SystemClock.elapsedRealtime();
                if (!LocationManagerService.this.mLocationHandler.hasMessages(7)) {
                    LocationManagerService.this.mLocationHandler.sendEmptyMessageDelayed(7, BackupAgentTimeoutParameters.DEFAULT_FULL_BACKUP_AGENT_TIMEOUT_MILLIS);
                }
                this.mWakeLock.acquire();
            }
        }

        private void decrementPendingBroadcastsLocked() {
            int i = this.mPendingBroadcasts - 1;
            this.mPendingBroadcasts = i;
            if (i == 0 && this.mWakeLock.isHeld()) {
                this.mReleaseLockTime = SystemClock.elapsedRealtime();
                this.mWakeLock.release();
            }
            if (this.mPendingBroadcasts < 0) {
                this.mPendingBroadcasts = 0;
            }
        }

        public void clearPendingBroadcastsLocked() {
            if (this.mPendingBroadcasts > 0) {
                this.mPendingBroadcasts = 0;
                if (this.mWakeLock.isHeld()) {
                    this.mReleaseLockTime = SystemClock.elapsedRealtime();
                    this.mWakeLock.release();
                }
            }
        }
    }

    public class UpdateRecord {
        boolean mIsForegroundUid;
        Location mLastFixBroadcast;
        long mLastStatusBroadcast;
        String mProvider;
        final LocationRequest mRealRequest;
        final Receiver mReceiver;
        LocationRequest mRequest;

        UpdateRecord(String provider, LocationRequest request, Receiver receiver) {
            this.mProvider = provider;
            this.mRealRequest = request;
            this.mRequest = request;
            this.mReceiver = receiver;
            this.mIsForegroundUid = LocationManagerService.isImportanceForeground(LocationManagerService.this.mActivityManager.getPackageImportance(this.mReceiver.mIdentity.mPackageName));
            ArrayList<UpdateRecord> records = (ArrayList) LocationManagerService.this.mRecordsByProvider.get(provider);
            if (records == null) {
                records = new ArrayList();
                LocationManagerService.this.mRecordsByProvider.put(provider, records);
            }
            if (!records.contains(this)) {
                records.add(this);
            }
            LocationManagerService.this.mRequestStatistics.startRequesting(this.mReceiver.mIdentity.mPackageName, provider, request.getInterval(), this.mIsForegroundUid);
        }

        void updateForeground(boolean isForeground) {
            this.mIsForegroundUid = isForeground;
            LocationManagerService.this.mRequestStatistics.updateForeground(this.mReceiver.mIdentity.mPackageName, this.mProvider, isForeground);
        }

        void disposeLocked(boolean removeReceiver) {
            LocationManagerService.this.mRequestStatistics.stopRequesting(this.mReceiver.mIdentity.mPackageName, this.mProvider);
            ArrayList<UpdateRecord> globalRecords = (ArrayList) LocationManagerService.this.mRecordsByProvider.get(this.mProvider);
            if (globalRecords != null) {
                globalRecords.remove(this);
            }
            if (removeReceiver) {
                HashMap<String, UpdateRecord> receiverRecords = this.mReceiver.mUpdateRecords;
                if (receiverRecords != null) {
                    receiverRecords.remove(this.mProvider);
                    if (receiverRecords.size() == 0) {
                        LocationManagerService.this.removeUpdatesLocked(this.mReceiver);
                    }
                }
            }
        }

        public String toString() {
            String str;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("UpdateRecord[");
            stringBuilder.append(this.mProvider);
            stringBuilder.append(" ");
            stringBuilder.append(this.mReceiver.mIdentity.mPackageName);
            stringBuilder.append("(");
            stringBuilder.append(this.mReceiver.mIdentity.mUid);
            if (this.mIsForegroundUid) {
                str = " foreground";
            } else {
                str = " background";
            }
            stringBuilder.append(str);
            stringBuilder.append(") ");
            stringBuilder.append(this.mRealRequest);
            stringBuilder.append("]");
            return stringBuilder.toString();
        }
    }

    public LocationManagerService(Context context) {
        this.mContext = context;
        this.mAppOps = (AppOpsManager) context.getSystemService("appops");
        ((PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class)).setLocationPackagesProvider(new PackagesProvider() {
            public String[] getPackages(int userId) {
                return LocationManagerService.this.mContext.getResources().getStringArray(17236014);
            }
        });
        Log.i(TAG, "Constructed");
        if (this.mLocationIpcMonitor == null) {
            this.mLocationIpcMonitor = HwServiceFactory.getIHwIpcMonitor(this.mLock, "location", "location");
            if (this.mLocationIpcMonitor != null) {
                Watchdog.getInstance().addIpcMonitor(this.mLocationIpcMonitor);
            }
        }
        this.mHwLbsLogger = HwServiceFactory.getHwLbsLogger(this.mContext);
    }

    public void systemRunning() {
        synchronized (this.mLock) {
            Log.i(TAG, "systemReady()");
            this.mPackageManager = this.mContext.getPackageManager();
            this.mPowerManager = (PowerManager) this.mContext.getSystemService("power");
            this.mActivityManager = (ActivityManager) this.mContext.getSystemService("activity");
            this.mLocationThread = new HandlerThread("LocationThread");
            this.mLocationThread.start();
            this.mLocationHandler = new LocationWorkerHandler(this.mLocationThread.getLooper());
            this.mLocationFudger = new LocationFudger(this.mContext, this.mLocationHandler);
            this.mBlacklist = new LocationBlacklist(this.mContext, this.mLocationHandler);
            this.mBlacklist.init();
            this.mGeofenceManager = new GeofenceManager(this.mContext, this.mBlacklist);
            this.mAppOps.startWatchingMode(0, null, new OnOpChangedInternalListener() {
                public void onOpChanged(int op, String packageName) {
                    String str = LocationManagerService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("onOpChanged:");
                    stringBuilder.append(op);
                    stringBuilder.append(" ");
                    stringBuilder.append(packageName);
                    Log.i(str, stringBuilder.toString());
                    synchronized (LocationManagerService.this.mLock) {
                        for (Receiver receiver : LocationManagerService.this.mReceivers.values()) {
                            receiver.updateMonitoring(true);
                        }
                        LocationManagerService.this.applyAllProviderRequirementsLocked();
                    }
                }
            });
            this.mPackageManager.addOnPermissionsChangeListener(new OnPermissionsChangedListener() {
                public void onPermissionsChanged(int uid) {
                    synchronized (LocationManagerService.this.mLock) {
                        for (Receiver receiver : LocationManagerService.this.mReceivers.values()) {
                            if (receiver.mIdentity.mUid == uid) {
                                String str = LocationManagerService.TAG;
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("onPermissionsChanged: uid=");
                                stringBuilder.append(uid);
                                Log.i(str, stringBuilder.toString());
                                LocationManagerService.this.applyAllProviderRequirementsLocked();
                                break;
                            }
                        }
                    }
                }
            });
            this.mActivityManager.addOnUidImportanceListener(new OnUidImportanceListener() {
                public void onUidImportance(final int uid, final int importance) {
                    LocationManagerService.this.mLocationHandler.post(new Runnable() {
                        public void run() {
                            LocationManagerService.this.onUidImportanceChanged(uid, importance);
                        }
                    });
                }
            }, FOREGROUND_IMPORTANCE_CUTOFF);
            this.mUserManager = (UserManager) this.mContext.getSystemService("user");
            updateUserProfiles(this.mCurrentUserId);
            updateBackgroundThrottlingWhitelistLocked();
            HwServiceFactory.getHwNLPManager().setLocationManagerService(this, this.mContext);
            HwServiceFactory.getHwNLPManager().setHwMultiNlpPolicy(this.mContext);
            initHwLocationPowerTracker(this.mContext);
            this.mHwLocationGpsLogServices = HwServiceFactory.getHwGpsLogServices(this.mContext);
            this.mLocationHandler.post(new Runnable() {
                public void run() {
                    synchronized (LocationManagerService.this.mLock) {
                        LocationManagerService.this.loadProvidersLocked();
                        LocationManagerService.this.updateProvidersLocked();
                    }
                }
            });
        }
        this.mContext.getContentResolver().registerContentObserver(Secure.getUriFor("location_providers_allowed"), true, new ContentObserver(this.mLocationHandler) {
            public void onChange(boolean selfChange) {
                if (LocationManagerService.this.isGPSDisabled()) {
                    Log.d(LocationManagerService.TAG, "gps is disabled by dpm .");
                }
                synchronized (LocationManagerService.this.mLock) {
                    Log.d(LocationManagerService.TAG, "LOCATION_PROVIDERS_ALLOWED onchange");
                    LocationManagerService.this.updateProvidersLocked();
                }
            }
        }, -1);
        this.mContext.getContentResolver().registerContentObserver(Global.getUriFor("location_background_throttle_interval_ms"), true, new ContentObserver(this.mLocationHandler) {
            public void onChange(boolean selfChange) {
                synchronized (LocationManagerService.this.mLock) {
                    LocationManagerService.this.updateProvidersLocked();
                }
            }
        }, -1);
        this.mContext.getContentResolver().registerContentObserver(Global.getUriFor("location_background_throttle_package_whitelist"), true, new ContentObserver(this.mLocationHandler) {
            public void onChange(boolean selfChange) {
                synchronized (LocationManagerService.this.mLock) {
                    LocationManagerService.this.updateBackgroundThrottlingWhitelistLocked();
                    LocationManagerService.this.updateProvidersLocked();
                }
            }
        }, -1);
        hwQuickGpsSwitch();
        this.mPackageMonitor.register(this.mContext, this.mLocationHandler.getLooper(), true);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.USER_SWITCHED");
        intentFilter.addAction("android.intent.action.MANAGED_PROFILE_ADDED");
        intentFilter.addAction("android.intent.action.MANAGED_PROFILE_REMOVED");
        intentFilter.addAction("android.intent.action.ACTION_SHUTDOWN");
        intentFilter.addAction("android.intent.action.USER_ADDED");
        intentFilter.addAction("android.intent.action.USER_REMOVED");
        this.mContext.registerReceiverAsUser(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if ("android.intent.action.USER_SWITCHED".equals(action)) {
                    LocationManagerService.this.switchUser(intent.getIntExtra("android.intent.extra.user_handle", 0));
                } else if ("android.intent.action.MANAGED_PROFILE_ADDED".equals(action) || "android.intent.action.MANAGED_PROFILE_REMOVED".equals(action)) {
                    LocationManagerService.this.updateUserProfiles(LocationManagerService.this.mCurrentUserId);
                } else if ("android.intent.action.ACTION_SHUTDOWN".equals(action)) {
                    if (getSendingUserId() == -1) {
                        LocationManagerService.this.shutdownComponents();
                    }
                } else if ("android.intent.action.USER_ADDED".equals(action) || "android.intent.action.USER_REMOVED".equals(action)) {
                    int userId = intent.getIntExtra("android.intent.extra.user_handle", -1);
                    if (userId != LocationManagerService.this.mCurrentUserId) {
                        UserInfo ui = LocationManagerService.this.mUserManager.getUserInfo(userId);
                        if (ui != null && ui.profileGroupId == LocationManagerService.this.mCurrentUserId) {
                            LocationManagerService.this.updateUserProfiles(LocationManagerService.this.mCurrentUserId);
                            String str = LocationManagerService.TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("onReceive action:");
                            stringBuilder.append(action);
                            stringBuilder.append(", userId:");
                            stringBuilder.append(userId);
                            stringBuilder.append(", updateUserProfiles for currentUserId:");
                            stringBuilder.append(LocationManagerService.this.mCurrentUserId);
                            Log.i(str, stringBuilder.toString());
                        }
                    }
                }
            }
        }, UserHandle.ALL, intentFilter, null, this.mLocationHandler);
        GpsFreezeProc.getInstance().registerFreezeListener(new GpsFreezeListener() {
            public void onFreezeProChange(String pkg) {
                if (LocationManagerService.this.isAllowedByCurrentUserSettingsLocked("gps")) {
                    LocationManagerService.this.mLocationHandler.sendMessage(Message.obtain(LocationManagerService.this.mLocationHandler, 4, pkg));
                    return;
                }
                Log.i(LocationManagerService.TAG, "LocationManager.GPS_PROVIDER is not enable");
            }

            public void onWhiteListChange(int type, List<String> pkgList) {
                LocationManagerService.this.mLocationHandler.sendMessage(Message.obtain(LocationManagerService.this.mLocationHandler, 5, type, 0, pkgList));
            }
        });
    }

    private void onUidImportanceChanged(int uid, int importance) {
        boolean foreground = isImportanceForeground(importance);
        HashSet<String> affectedProviders = new HashSet(this.mRecordsByProvider.size());
        synchronized (this.mLock) {
            String provider;
            for (Entry<String, ArrayList<UpdateRecord>> entry : this.mRecordsByProvider.entrySet()) {
                provider = (String) entry.getKey();
                Iterator it = ((ArrayList) entry.getValue()).iterator();
                while (it.hasNext()) {
                    UpdateRecord record = (UpdateRecord) it.next();
                    if (record.mReceiver.mIdentity.mUid == uid && record.mIsForegroundUid != foreground) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("request from uid ");
                        stringBuilder.append(uid);
                        stringBuilder.append(" is now ");
                        stringBuilder.append(foreground ? "foreground" : "background)");
                        Log.d(str, stringBuilder.toString());
                        record.mIsForegroundUid = foreground;
                        record.updateForeground(foreground);
                        if (!isThrottlingExemptLocked(record.mReceiver.mIdentity)) {
                            affectedProviders.add(provider);
                        }
                    }
                }
            }
            Iterator it2 = affectedProviders.iterator();
            while (it2.hasNext()) {
                applyRequirementsLocked((String) it2.next());
            }
            for (Entry<IBinder, Identity> entry2 : this.mGnssMeasurementsListeners.entrySet()) {
                if (((Identity) entry2.getValue()).mUid == uid) {
                    provider = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("gnss measurements listener from uid ");
                    stringBuilder2.append(uid);
                    stringBuilder2.append(" is now ");
                    stringBuilder2.append(foreground ? "foreground" : "background)");
                    Log.d(provider, stringBuilder2.toString());
                    if (foreground || isThrottlingExemptLocked((Identity) entry2.getValue())) {
                        this.mGnssMeasurementsProvider.addListener(Stub.asInterface((IBinder) entry2.getKey()));
                    } else {
                        this.mGnssMeasurementsProvider.removeListener(Stub.asInterface((IBinder) entry2.getKey()));
                    }
                }
            }
            for (Entry<IBinder, Identity> entry22 : this.mGnssNavigationMessageListeners.entrySet()) {
                if (((Identity) entry22.getValue()).mUid == uid) {
                    if (foreground || isThrottlingExemptLocked((Identity) entry22.getValue())) {
                        this.mGnssNavigationMessageProvider.addListener(IGnssNavigationMessageListener.Stub.asInterface((IBinder) entry22.getKey()));
                    } else {
                        this.mGnssNavigationMessageProvider.removeListener(IGnssNavigationMessageListener.Stub.asInterface((IBinder) entry22.getKey()));
                    }
                }
            }
        }
    }

    private static boolean isImportanceForeground(int importance) {
        return importance <= FOREGROUND_IMPORTANCE_CUTOFF;
    }

    private void shutdownComponents() {
        LocationProviderInterface gpsProvider = (LocationProviderInterface) this.mProvidersByName.get("gps");
        if (gpsProvider != null && gpsProvider.isEnabled()) {
            gpsProvider.disable();
        }
    }

    void updateUserProfiles(int currentUserId) {
        int[] profileIds = this.mUserManager.getProfileIdsWithDisabled(currentUserId);
        synchronized (this.mLock) {
            this.mCurrentUserProfiles = profileIds;
        }
    }

    private boolean isCurrentProfile(int userId) {
        boolean contains;
        synchronized (this.mLock) {
            contains = ArrayUtils.contains(this.mCurrentUserProfiles, userId);
        }
        return contains;
    }

    private void ensureFallbackFusedProviderPresentLocked(ArrayList<String> pkgs) {
        PackageManager pm = this.mContext.getPackageManager();
        String systemPackageName = this.mContext.getPackageName();
        ArrayList<HashSet<Signature>> sigSets = ServiceWatcher.getSignatureSets(this.mContext, pkgs);
        for (ResolveInfo rInfo : pm.queryIntentServicesAsUser(new Intent(FUSED_LOCATION_SERVICE_ACTION), 128, this.mCurrentUserId)) {
            String packageName = rInfo.serviceInfo.packageName;
            String str;
            StringBuilder stringBuilder;
            try {
                StringBuilder stringBuilder2;
                if (!ServiceWatcher.isSignatureMatch(pm.getPackageInfo(packageName, 64).signatures, sigSets)) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(packageName);
                    stringBuilder.append(" resolves service ");
                    stringBuilder.append(FUSED_LOCATION_SERVICE_ACTION);
                    stringBuilder.append(", but has wrong signature, ignoring");
                    Log.w(str, stringBuilder.toString());
                } else if (rInfo.serviceInfo.metaData == null) {
                    String str2 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Found fused provider without metadata: ");
                    stringBuilder2.append(packageName);
                    Log.w(str2, stringBuilder2.toString());
                } else if (rInfo.serviceInfo.metaData.getInt(ServiceWatcher.EXTRA_SERVICE_VERSION, -1) != 0) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Fallback candidate not version 0: ");
                    stringBuilder.append(packageName);
                    Log.i(str, stringBuilder.toString());
                } else if ((rInfo.serviceInfo.applicationInfo.flags & 1) == 0) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Fallback candidate not in /system: ");
                    stringBuilder.append(packageName);
                    Log.i(str, stringBuilder.toString());
                } else if (pm.checkSignatures(systemPackageName, packageName) != 0) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Fallback candidate not signed the same as system: ");
                    stringBuilder.append(packageName);
                    Log.i(str, stringBuilder.toString());
                } else {
                    String str3 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Found fallback provider: ");
                    stringBuilder2.append(packageName);
                    Log.i(str3, stringBuilder2.toString());
                    return;
                }
            } catch (NameNotFoundException e) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("missing package: ");
                stringBuilder.append(packageName);
                Log.e(str, stringBuilder.toString());
            }
        }
        throw new IllegalStateException("Unable to find a fused location provider that is in the system partition with version 0 and signed with the platform certificate. Such a package is needed to provide a default fused location provider in the event that no other fused location provider has been installed or is currently available. For example, coreOnly boot mode when decrypting the data partition. The fallback must also be marked coreApp=\"true\" in the manifest");
    }

    private void loadProvidersLocked() {
        GnssLocationProvider gnssProvider;
        Resources resources;
        PassiveProvider passiveProvider = new PassiveProvider(this);
        addProviderLocked(passiveProvider);
        this.mEnabledProviders.add(passiveProvider.getName());
        this.mPassiveProvider = passiveProvider;
        GnssLocationProvider gnssProvider2 = HwServiceFactory.createHwGnssLocationProvider(this.mContext, this, this.mLocationHandler.getLooper());
        if (GnssLocationProvider.isSupported()) {
            this.mGnssSystemInfoProvider = gnssProvider2.getGnssSystemInfoProvider();
            this.mGnssBatchingProvider = gnssProvider2.getGnssBatchingProvider();
            this.mGnssMetricsProvider = gnssProvider2.getGnssMetricsProvider();
            this.mGnssStatusProvider = gnssProvider2.getGnssStatusProvider();
            this.mNetInitiatedListener = gnssProvider2.getNetInitiatedListener();
            addProviderLocked(gnssProvider2);
            this.mRealProviders.put("gps", gnssProvider2);
            this.mGnssMeasurementsProvider = gnssProvider2.getGnssMeasurementsProvider();
            this.mGnssNavigationMessageProvider = gnssProvider2.getGnssNavigationMessageProvider();
            this.mGpsGeofenceProxy = gnssProvider2.getGpsGeofenceProxy();
        }
        this.mHwQuickTTFFMonitor = HwQuickTTFFMonitor.getInstance(this.mContext, gnssProvider2);
        this.mHwQuickTTFFMonitor.startMonitor();
        Resources resources2 = this.mContext.getResources();
        ArrayList<String> providerPackageNames = new ArrayList();
        String[] pkgs = resources2.getStringArray(17236014);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("certificates for location providers pulled from: ");
        stringBuilder.append(Arrays.toString(pkgs));
        Log.i(str, stringBuilder.toString());
        if (pkgs != null) {
            providerPackageNames.addAll(Arrays.asList(pkgs));
        }
        ensureFallbackFusedProviderPresentLocked(providerPackageNames);
        LocationProviderProxy networkProvider = HwServiceFactory.locationProviderProxyCreateAndBind(this.mContext, "network", NETWORK_LOCATION_SERVICE_ACTION, 17956962, 17039832, 17236014, this.mLocationHandler);
        if (networkProvider != null) {
            this.mRealProviders.put("network", networkProvider);
            this.mProxyProviders.add(networkProvider);
            addProviderLocked(networkProvider);
        } else {
            Slog.w(TAG, "no network location provider found");
        }
        LocationProviderProxy fusedLocationProvider = LocationProviderProxy.createAndBind(this.mContext, "fused", FUSED_LOCATION_SERVICE_ACTION, 17956953, 17039807, 17236014, this.mLocationHandler);
        if (fusedLocationProvider != null) {
            addProviderLocked(fusedLocationProvider);
            this.mProxyProviders.add(fusedLocationProvider);
            this.mEnabledProviders.add(fusedLocationProvider.getName());
            this.mRealProviders.put("fused", fusedLocationProvider);
        } else {
            Slog.e(TAG, "no fused location provider found", new IllegalStateException("Location service needs a fused location provider"));
        }
        this.mGeocodeProvider = HwServiceFactory.geocoderProxyCreateAndBind(this.mContext, 17956954, 17039808, 17236014, this.mLocationHandler);
        if (this.mGeocodeProvider == null) {
            Slog.e(TAG, "no geocoder provider found");
        }
        checkGeoFencerEnabled(this.mPackageManager);
        if (GeofenceProxy.createAndBind(this.mContext, 17956955, 17039809, 17236014, this.mLocationHandler, this.mGpsGeofenceProxy, null) == null) {
            Slog.d(TAG, "Unable to bind FLP Geofence proxy.");
        }
        this.mLocalLocationProvider = HwServiceFactory.getHwLocalLocationProvider(this.mContext, this);
        enableLocalLocationProviders(gnssProvider2);
        boolean activityRecognitionHardwareIsSupported = ActivityRecognitionHardware.isSupported();
        ActivityRecognitionHardware activityRecognitionHardware = null;
        if (activityRecognitionHardwareIsSupported) {
            activityRecognitionHardware = ActivityRecognitionHardware.getInstance(this.mContext);
        } else {
            Slog.d(TAG, "Hardware Activity-Recognition not supported.");
        }
        if (ActivityRecognitionProxy.createAndBind(this.mContext, this.mLocationHandler, activityRecognitionHardwareIsSupported, activityRecognitionHardware, 17956947, 17039765, 17236014) == null) {
            Slog.d(TAG, "Unable to bind ActivityRecognitionProxy.");
        }
        String[] testProviderStrings = resources2.getStringArray(17236041);
        int length = testProviderStrings.length;
        int i = 0;
        while (i < length) {
            String[] fragments = testProviderStrings[i].split(",");
            PassiveProvider passiveProvider2 = passiveProvider;
            passiveProvider = fragments[0].trim();
            gnssProvider = gnssProvider2;
            if (this.mProvidersByName.get(passiveProvider) == null) {
                resources = resources2;
                addTestProviderLocked(passiveProvider, new ProviderProperties(Boolean.parseBoolean(fragments[1]), Boolean.parseBoolean(fragments[2]), Boolean.parseBoolean(fragments[3]), Boolean.parseBoolean(fragments[4]), Boolean.parseBoolean(fragments[5]), Boolean.parseBoolean(fragments[6]), Boolean.parseBoolean(fragments[7]), Integer.parseInt(fragments[8]), Integer.parseInt(fragments[9])));
                i++;
                passiveProvider = passiveProvider2;
                gnssProvider2 = gnssProvider;
                resources2 = resources;
            } else {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Provider \"");
                stringBuilder2.append(passiveProvider);
                stringBuilder2.append("\" already exists");
                throw new IllegalArgumentException(stringBuilder2.toString());
            }
        }
        gnssProvider = gnssProvider2;
        resources = resources2;
        this.mHwGpsActionReporter = HwServiceFactory.getHwGpsActionReporter(this.mContext, this);
    }

    private void switchUser(int userId) {
        if (this.mCurrentUserId != userId) {
            this.mBlacklist.switchUser(userId);
            this.mLocationHandler.removeMessages(1);
            this.mLocationHandler.removeMessages(2);
            this.mLocationHandler.removeMessages(3);
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("switchUser:");
            stringBuilder.append(userId);
            Log.i(str, stringBuilder.toString());
            synchronized (this.mLock) {
                this.mLastLocation.clear();
                this.mLastLocationCoarseInterval.clear();
                Iterator it = this.mProviders.iterator();
                while (it.hasNext()) {
                    updateProviderListenersLocked(((LocationProviderInterface) it.next()).getName(), false);
                }
                this.mCurrentUserId = userId;
                updateUserProfiles(userId);
                updateProvidersLocked();
            }
        }
    }

    public void locationCallbackFinished(ILocationListener listener) {
        synchronized (this.mLock) {
            Receiver receiver = (Receiver) this.mReceivers.get(listener.asBinder());
            if (receiver != null) {
                synchronized (receiver) {
                    long identity = Binder.clearCallingIdentity();
                    receiver.decrementPendingBroadcastsLocked();
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }
    }

    public int getGnssYearOfHardware() {
        if (this.mGnssSystemInfoProvider != null) {
            return this.mGnssSystemInfoProvider.getGnssYearOfHardware();
        }
        return 0;
    }

    public String getGnssHardwareModelName() {
        if (this.mGnssSystemInfoProvider != null) {
            return this.mGnssSystemInfoProvider.getGnssHardwareModelName();
        }
        return null;
    }

    private boolean hasGnssPermissions(String packageName) {
        int allowedResolutionLevel = getCallerAllowedResolutionLevel();
        checkResolutionLevelIsSufficientForProviderUse(allowedResolutionLevel, "gps");
        int pid = Binder.getCallingPid();
        int uid = Binder.getCallingUid();
        long identity = Binder.clearCallingIdentity();
        try {
            boolean hasLocationAccess = checkLocationAccess(pid, uid, packageName, allowedResolutionLevel);
            if (HwSystemManager.allowOp(this.mContext, 8)) {
                return hasLocationAccess;
            }
            return true;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public int getGnssBatchSize(String packageName) {
        this.mContext.enforceCallingPermission("android.permission.LOCATION_HARDWARE", "Location Hardware permission not granted to access hardware batching");
        if (!hasGnssPermissions(packageName) || this.mGnssBatchingProvider == null) {
            return 0;
        }
        return this.mGnssBatchingProvider.getBatchSize();
    }

    public boolean addGnssBatchingCallback(IBatchedLocationCallback callback, String packageName) {
        this.mContext.enforceCallingPermission("android.permission.LOCATION_HARDWARE", "Location Hardware permission not granted to access hardware batching");
        if (!hasGnssPermissions(packageName) || this.mGnssBatchingProvider == null) {
            return false;
        }
        this.mGnssBatchingCallback = callback;
        this.mGnssBatchingDeathCallback = new LinkedCallback(callback);
        try {
            callback.asBinder().linkToDeath(this.mGnssBatchingDeathCallback, 0);
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, "Remote listener already died.", e);
            return false;
        }
    }

    public void removeGnssBatchingCallback() {
        try {
            this.mGnssBatchingCallback.asBinder().unlinkToDeath(this.mGnssBatchingDeathCallback, 0);
        } catch (NoSuchElementException e) {
            Log.e(TAG, "Couldn't unlink death callback.", e);
        }
        this.mGnssBatchingCallback = null;
        this.mGnssBatchingDeathCallback = null;
    }

    public boolean startGnssBatch(long periodNanos, boolean wakeOnFifoFull, String packageName) {
        this.mContext.enforceCallingPermission("android.permission.LOCATION_HARDWARE", "Location Hardware permission not granted to access hardware batching");
        if (!hasGnssPermissions(packageName) || this.mGnssBatchingProvider == null) {
            return false;
        }
        if (this.mGnssBatchingInProgress) {
            Log.e(TAG, "startGnssBatch unexpectedly called w/o stopping prior batch");
            stopGnssBatch();
        }
        this.mGnssBatchingInProgress = true;
        return this.mGnssBatchingProvider.start(periodNanos, wakeOnFifoFull);
    }

    public void flushGnssBatch(String packageName) {
        this.mContext.enforceCallingPermission("android.permission.LOCATION_HARDWARE", "Location Hardware permission not granted to access hardware batching");
        if (hasGnssPermissions(packageName)) {
            if (!this.mGnssBatchingInProgress) {
                Log.w(TAG, "flushGnssBatch called with no batch in progress");
            }
            if (this.mGnssBatchingProvider != null) {
                this.mGnssBatchingProvider.flush();
            }
            return;
        }
        Log.e(TAG, "flushGnssBatch called without GNSS permissions");
    }

    public boolean stopGnssBatch() {
        this.mContext.enforceCallingPermission("android.permission.LOCATION_HARDWARE", "Location Hardware permission not granted to access hardware batching");
        if (this.mGnssBatchingProvider == null) {
            return false;
        }
        this.mGnssBatchingInProgress = false;
        return this.mGnssBatchingProvider.stop();
    }

    public void reportLocationBatch(List<Location> locations) {
        checkCallerIsProvider();
        if (!isAllowedByCurrentUserSettingsLocked("gps")) {
            Slog.w(TAG, "reportLocationBatch() called without user permission, locations blocked");
        } else if (this.mGnssBatchingCallback == null) {
            Slog.e(TAG, "reportLocationBatch() called without active Callback");
        } else {
            try {
                this.mGnssBatchingCallback.onLocationBatch(locations);
            } catch (RemoteException e) {
                Slog.e(TAG, "mGnssBatchingCallback.onLocationBatch failed", e);
            }
        }
    }

    private void addProviderLocked(LocationProviderInterface provider) {
        this.mProviders.add(provider);
        this.mProvidersByName.put(provider.getName(), provider);
    }

    private void removeProviderLocked(LocationProviderInterface provider) {
        provider.disable();
        this.mProviders.remove(provider);
        this.mProvidersByName.remove(provider.getName());
    }

    private boolean isAllowedByCurrentUserSettingsLocked(String provider) {
        return isAllowedByUserSettingsLockedForUser(provider, this.mCurrentUserId);
    }

    private boolean isAllowedByUserSettingsLockedForUser(String provider, int userId) {
        if (this.mEnabledProviders.contains(provider)) {
            return true;
        }
        if (this.mDisabledProviders.contains(provider)) {
            return false;
        }
        return isLocationProviderEnabledForUser(provider, userId);
    }

    private boolean isAllowedByUserSettingsLocked(String provider, int uid, int userId) {
        if (isCurrentProfile(UserHandle.getUserId(uid)) || isUidALocationProvider(uid)) {
            return isAllowedByUserSettingsLockedForUser(provider, userId);
        }
        return false;
    }

    private String getResolutionPermission(int resolutionLevel) {
        switch (resolutionLevel) {
            case 1:
                return "android.permission.ACCESS_COARSE_LOCATION";
            case 2:
                return "android.permission.ACCESS_FINE_LOCATION";
            default:
                return null;
        }
    }

    private int getAllowedResolutionLevel(int pid, int uid) {
        if (this.mContext.checkPermission("android.permission.ACCESS_FINE_LOCATION", pid, uid) == 0) {
            return 2;
        }
        if (this.mContext.checkPermission("android.permission.ACCESS_COARSE_LOCATION", pid, uid) == 0) {
            return 1;
        }
        return 0;
    }

    private int getCallerAllowedResolutionLevel() {
        return getAllowedResolutionLevel(Binder.getCallingPid(), Binder.getCallingUid());
    }

    private void checkResolutionLevelIsSufficientForGeofenceUse(int allowedResolutionLevel) {
        if (allowedResolutionLevel < 2) {
            throw new SecurityException("Geofence usage requires ACCESS_FINE_LOCATION permission");
        }
    }

    private int getMinimumResolutionLevelForProviderUse(String provider) {
        if ("gps".equals(provider) || "passive".equals(provider)) {
            return 2;
        }
        if ("network".equals(provider) || "fused".equals(provider)) {
            return 1;
        }
        LocationProviderInterface lp = (LocationProviderInterface) this.mMockProviders.get(provider);
        if (lp != null) {
            ProviderProperties properties = lp.getProperties();
            if (properties == null || properties.mRequiresSatellite) {
                return 2;
            }
            if (properties.mRequiresNetwork || properties.mRequiresCell) {
                return 1;
            }
        }
        return 2;
    }

    private void checkResolutionLevelIsSufficientForProviderUse(int allowedResolutionLevel, String providerName) {
        int requiredResolutionLevel = getMinimumResolutionLevelForProviderUse(providerName);
        if (allowedResolutionLevel < requiredResolutionLevel) {
            StringBuilder stringBuilder;
            switch (requiredResolutionLevel) {
                case 1:
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("\"");
                    stringBuilder.append(providerName);
                    stringBuilder.append("\" location provider requires ACCESS_COARSE_LOCATION or ACCESS_FINE_LOCATION permission.");
                    throw new SecurityException(stringBuilder.toString());
                case 2:
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("\"");
                    stringBuilder.append(providerName);
                    stringBuilder.append("\" location provider requires ACCESS_FINE_LOCATION permission.");
                    throw new SecurityException(stringBuilder.toString());
                default:
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Insufficient permission for \"");
                    stringBuilder.append(providerName);
                    stringBuilder.append("\" location provider.");
                    throw new SecurityException(stringBuilder.toString());
            }
        }
    }

    private void checkDeviceStatsAllowed() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.UPDATE_DEVICE_STATS", null);
    }

    private void checkUpdateAppOpsAllowed() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.UPDATE_APP_OPS_STATS", null);
    }

    public static int resolutionLevelToOp(int allowedResolutionLevel) {
        if (allowedResolutionLevel == 0) {
            return -1;
        }
        if (allowedResolutionLevel == 1) {
            return 0;
        }
        return 1;
    }

    boolean reportLocationAccessNoThrow(int pid, int uid, String packageName, int allowedResolutionLevel) {
        int op = resolutionLevelToOp(allowedResolutionLevel);
        boolean z = false;
        if (op >= 0 && this.mAppOps.noteOpNoThrow(op, uid, packageName) != 0) {
            return false;
        }
        if (getAllowedResolutionLevel(pid, uid) >= allowedResolutionLevel) {
            z = true;
        }
        return z;
    }

    boolean checkLocationAccess(int pid, int uid, String packageName, int allowedResolutionLevel) {
        int op = resolutionLevelToOp(allowedResolutionLevel);
        boolean z = false;
        if (op >= 0 && this.mAppOps.checkOp(op, uid, packageName) != 0) {
            return false;
        }
        if (getAllowedResolutionLevel(pid, uid) >= allowedResolutionLevel) {
            z = true;
        }
        return z;
    }

    public List<String> getAllProviders() {
        ArrayList<String> out;
        synchronized (this.mLock) {
            out = new ArrayList(this.mProviders.size());
            Iterator it = this.mProviders.iterator();
            while (it.hasNext()) {
                String name = ((LocationProviderInterface) it.next()).getName();
                if (!"fused".equals(name)) {
                    if (!IHwLocalLocationProvider.LOCAL_PROVIDER.equals(name)) {
                        out.add(name);
                    }
                }
            }
        }
        ArrayList<String> out2 = out;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getAllProviders()=");
        stringBuilder.append(out2);
        Log.i(str, stringBuilder.toString());
        return out2;
    }

    public List<String> getProviders(Criteria criteria, boolean enabledOnly) {
        hwSendBehavior(BehaviorId.LOCATIONMANAGER_GETPROVIDERS);
        int allowedResolutionLevel = getCallerAllowedResolutionLevel();
        int uid = Binder.getCallingUid();
        long identity = Binder.clearCallingIdentity();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("enableOnly=");
        stringBuilder.append(enabledOnly);
        stringBuilder.append("allowedLevel=");
        stringBuilder.append(allowedResolutionLevel);
        Log.i(str, stringBuilder.toString());
        try {
            ArrayList<String> out;
            synchronized (this.mLock) {
                out = new ArrayList(this.mProviders.size());
                Iterator it = this.mProviders.iterator();
                while (it.hasNext()) {
                    LocationProviderInterface provider = (LocationProviderInterface) it.next();
                    String name = provider.getName();
                    if (!"fused".equals(name)) {
                        if (!IHwLocalLocationProvider.LOCAL_PROVIDER.equals(name)) {
                            String str2 = TAG;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("provider=");
                            stringBuilder2.append(name);
                            Log.i(str2, stringBuilder2.toString());
                            if (allowedResolutionLevel >= getMinimumResolutionLevelForProviderUse(name)) {
                                if (enabledOnly && !isAllowedByUserSettingsLocked(name, uid, this.mCurrentUserId)) {
                                    Log.i(TAG, "provider is not enable");
                                } else if (criteria == null || LocationProvider.propertiesMeetCriteria(name, provider.getProperties(), criteria)) {
                                    out.add(name);
                                } else {
                                    Log.i(TAG, "the criteria of provider is not matches");
                                }
                            }
                        }
                    }
                }
            }
            ArrayList<String> out2 = out;
            Binder.restoreCallingIdentity(identity);
            String str3 = TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("getProviders()=");
            stringBuilder3.append(out2);
            Log.i(str3, stringBuilder3.toString());
            return out2;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public String getBestProvider(Criteria criteria, boolean enabledOnly) {
        hwSendBehavior(BehaviorId.LOCATIONMANAGER_GETBESTPROVIDER);
        List<String> providers = getProviders(criteria, enabledOnly);
        String result;
        String str;
        StringBuilder stringBuilder;
        if (providers.isEmpty()) {
            providers = getProviders(null, enabledOnly);
            if (providers.isEmpty()) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("getBestProvider(");
                stringBuilder2.append(criteria);
                stringBuilder2.append(", ");
                stringBuilder2.append(enabledOnly);
                stringBuilder2.append(")=");
                stringBuilder2.append(null);
                Log.i(str2, stringBuilder2.toString());
                return null;
            }
            result = pickBest(providers);
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("getBestProvider(");
            stringBuilder.append(criteria);
            stringBuilder.append(", ");
            stringBuilder.append(enabledOnly);
            stringBuilder.append(")=");
            stringBuilder.append(result);
            Log.i(str, stringBuilder.toString());
            return result;
        }
        result = pickBest(providers);
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("getBestProvider(");
        stringBuilder.append(criteria);
        stringBuilder.append(", ");
        stringBuilder.append(enabledOnly);
        stringBuilder.append(")=");
        stringBuilder.append(result);
        Log.i(str, stringBuilder.toString());
        return result;
    }

    private String pickBest(List<String> providers) {
        if (providers.contains("gps")) {
            return "gps";
        }
        if (providers.contains("network")) {
            return "network";
        }
        return (String) providers.get(0);
    }

    public boolean providerMeetsCriteria(String provider, Criteria criteria) {
        LocationProviderInterface p = (LocationProviderInterface) this.mProvidersByName.get(provider);
        if (p != null) {
            boolean result = LocationProvider.propertiesMeetCriteria(p.getName(), p.getProperties(), criteria);
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("providerMeetsCriteria(");
            stringBuilder.append(provider);
            stringBuilder.append(", ");
            stringBuilder.append(criteria);
            stringBuilder.append(")=");
            stringBuilder.append(result);
            Log.i(str, stringBuilder.toString());
            return result;
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("provider=");
        stringBuilder2.append(provider);
        throw new IllegalArgumentException(stringBuilder2.toString());
    }

    private void updateProvidersLocked() {
        boolean changesMade = false;
        for (int i = this.mProviders.size() - 1; i >= 0; i--) {
            LocationProviderInterface p = (LocationProviderInterface) this.mProviders.get(i);
            boolean isEnabled = p.isEnabled();
            String name = p.getName();
            boolean shouldBeEnabled = isAllowedByCurrentUserSettingsLocked(name);
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Provider name = ");
            stringBuilder.append(name);
            stringBuilder.append(" shouldbeEnabled = ");
            stringBuilder.append(shouldBeEnabled);
            Log.d(str, stringBuilder.toString());
            if (isEnabled && !shouldBeEnabled) {
                updateProviderListenersLocked(name, false);
                this.mLastLocation.clear();
                this.mLastLocationCoarseInterval.clear();
                changesMade = true;
            } else if (!isEnabled && shouldBeEnabled) {
                updateProviderListenersLocked(name, true);
                changesMade = true;
            }
        }
        if (changesMade) {
            this.mContext.sendBroadcastAsUser(new Intent("android.location.PROVIDERS_CHANGED"), UserHandle.ALL);
            Intent intent = new Intent("android.location.MODE_CHANGED");
            intent.addFlags(DumpState.DUMP_SERVICE_PERMISSIONS);
            this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        }
    }

    private void updateProviderListenersLocked(String provider, boolean enabled) {
        int listeners = 0;
        LocationProviderInterface p = (LocationProviderInterface) this.mProvidersByName.get(provider);
        if (p != null) {
            ArrayList<Receiver> deadReceivers = null;
            ArrayList<UpdateRecord> records = (ArrayList) this.mRecordsByProvider.get(provider);
            if (records != null) {
                Iterator it = records.iterator();
                while (it.hasNext()) {
                    UpdateRecord record = (UpdateRecord) it.next();
                    if (isCurrentProfile(UserHandle.getUserId(record.mReceiver.mIdentity.mUid))) {
                        if (!record.mReceiver.callProviderEnabledLocked(provider, enabled)) {
                            if (deadReceivers == null) {
                                deadReceivers = new ArrayList();
                            }
                            deadReceivers.add(record.mReceiver);
                        }
                        listeners++;
                    }
                }
            }
            if (deadReceivers != null) {
                for (int i = deadReceivers.size() - 1; i >= 0; i--) {
                    removeUpdatesLocked((Receiver) deadReceivers.get(i));
                }
            }
            if (enabled) {
                p.enable();
                if (listeners > 0) {
                    applyRequirementsLocked(provider);
                }
            } else {
                p.disable();
            }
        }
    }

    private void applyRequirementsLocked(String provider) {
        String str = provider;
        String str2 = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("applyRequirementsLocked to ");
        stringBuilder.append(str);
        Log.i(str2, stringBuilder.toString());
        LocationProviderInterface p = (LocationProviderInterface) this.mProvidersByName.get(str);
        if (p == null) {
            Log.i(TAG, "LocationProviderInterface is null.");
            return;
        }
        ArrayList<UpdateRecord> records = (ArrayList) this.mRecordsByProvider.get(str);
        WorkSource worksource = new WorkSource();
        ProviderRequest providerRequest = new ProviderRequest();
        ContentResolver resolver = this.mContext.getContentResolver();
        long backgroundThrottleInterval = Global.getLong(resolver, "location_background_throttle_interval_ms", 1800000);
        boolean z = true;
        providerRequest.lowPowerMode = true;
        long backgroundThrottleInterval2;
        if (records != null) {
            Iterator it = records.iterator();
            while (it.hasNext()) {
                ContentResolver resolver2;
                UpdateRecord record = (UpdateRecord) it.next();
                if (!isCurrentProfile(UserHandle.getUserId(record.mReceiver.mIdentity.mUid))) {
                    resolver2 = resolver;
                    backgroundThrottleInterval2 = backgroundThrottleInterval;
                    resolver = z;
                } else if (checkLocationAccess(record.mReceiver.mIdentity.mPid, record.mReceiver.mIdentity.mUid, record.mReceiver.mIdentity.mPackageName, record.mReceiver.mAllowedResolutionLevel)) {
                    LocationRequest locationRequest = record.mRealRequest;
                    long interval = locationRequest.getInterval();
                    StringBuilder stringBuilder2;
                    if ("gps".equals(str) && isFreeze(record.mReceiver.mIdentity.mPackageName)) {
                        String str3 = TAG;
                        stringBuilder2 = new StringBuilder();
                        resolver2 = resolver;
                        stringBuilder2.append("packageName:");
                        stringBuilder2.append(record.mReceiver.mIdentity.mPackageName);
                        stringBuilder2.append(" is freeze, can't start gps");
                        Log.i(str3, stringBuilder2.toString());
                    } else {
                        resolver2 = resolver;
                        if ("network".equals(str) == null || isFreeze(record.mReceiver.mIdentity.mPackageName) == null) {
                            if (isThrottlingExemptLocked(record.mReceiver.mIdentity) == null) {
                                if (record.mIsForegroundUid == null) {
                                    interval = Math.max(interval, backgroundThrottleInterval);
                                }
                                if (interval != locationRequest.getInterval()) {
                                    locationRequest = new LocationRequest(locationRequest);
                                    locationRequest.setInterval(interval);
                                }
                            }
                            record.mRequest = locationRequest;
                            providerRequest.locationRequests.add(locationRequest);
                            if (locationRequest.isLowPowerMode() == null) {
                                providerRequest.lowPowerMode = null;
                            }
                            backgroundThrottleInterval2 = backgroundThrottleInterval;
                            if (interval < providerRequest.interval) {
                                resolver = true;
                                providerRequest.reportLocation = true;
                                providerRequest.interval = interval;
                            } else {
                                resolver = true;
                            }
                        } else {
                            resolver = TAG;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("packageName:");
                            stringBuilder2.append(record.mReceiver.mIdentity.mPackageName);
                            stringBuilder2.append(" is freeze, can't start network");
                            Log.i(resolver, stringBuilder2.toString());
                        }
                    }
                    resolver = resolver2;
                    z = true;
                } else {
                    resolver2 = resolver;
                    backgroundThrottleInterval2 = backgroundThrottleInterval;
                    resolver = true;
                }
                z = resolver;
                resolver = resolver2;
                backgroundThrottleInterval = backgroundThrottleInterval2;
            }
            backgroundThrottleInterval2 = backgroundThrottleInterval;
            if (providerRequest.reportLocation != null) {
                resolver = ((providerRequest.interval + 1000) * 3) / 2;
                Iterator it2 = records.iterator();
                while (it2.hasNext()) {
                    UpdateRecord record2 = (UpdateRecord) it2.next();
                    if (isCurrentProfile(UserHandle.getUserId(record2.mReceiver.mIdentity.mUid))) {
                        LocationRequest locationRequest2 = record2.mRequest;
                        if (providerRequest.locationRequests.contains(locationRequest2)) {
                            if (locationRequest2.getInterval() <= resolver) {
                                if (record2.mReceiver.mWorkSource == null || !isValidWorkSource(record2.mReceiver.mWorkSource)) {
                                    worksource.add(record2.mReceiver.mIdentity.mUid, record2.mReceiver.mIdentity.mPackageName);
                                } else {
                                    worksource.add(record2.mReceiver.mWorkSource);
                                }
                            }
                        }
                    }
                }
            }
        } else {
            backgroundThrottleInterval2 = backgroundThrottleInterval;
            Log.i(TAG, "UpdateRecords is null.");
        }
        p.setRequest(providerRequest, worksource);
        String str4 = TAG;
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("provider request: ");
        stringBuilder3.append(str);
        stringBuilder3.append(" ");
        stringBuilder3.append(providerRequest);
        stringBuilder3.append(" mCHRFirstReqFlag:");
        stringBuilder3.append(this.mCHRFirstReqFlag);
        Log.i(str4, stringBuilder3.toString());
        if (this.mCHRFirstReqFlag) {
            this.mHwLocationGpsLogServices.netWorkLocation(str, providerRequest);
            this.mCHRFirstReqFlag = false;
            Log.i(TAG, "network chr update");
        }
    }

    private static boolean isValidWorkSource(WorkSource workSource) {
        boolean z = true;
        if (workSource.size() > 0) {
            if (workSource.getName(0) == null) {
                z = false;
            }
            return z;
        }
        ArrayList<WorkChain> workChains = workSource.getWorkChains();
        if (workChains == null || workChains.isEmpty() || ((WorkChain) workChains.get(0)).getAttributionTag() == null) {
            z = false;
        }
        return z;
    }

    public String[] getBackgroundThrottlingWhitelist() {
        String[] strArr;
        synchronized (this.mLock) {
            strArr = (String[]) this.mBackgroundThrottlePackageWhitelist.toArray(new String[this.mBackgroundThrottlePackageWhitelist.size()]);
        }
        return strArr;
    }

    private void updateBackgroundThrottlingWhitelistLocked() {
        String setting = Global.getString(this.mContext.getContentResolver(), "location_background_throttle_package_whitelist");
        if (setting == null) {
            setting = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        }
        this.mBackgroundThrottlePackageWhitelist.clear();
        this.mBackgroundThrottlePackageWhitelist.addAll(SystemConfig.getInstance().getAllowUnthrottledLocation());
        this.mBackgroundThrottlePackageWhitelist.addAll(Arrays.asList(setting.split(",")));
        this.mBackgroundThrottlePackageWhitelist.addAll(getPackageWhiteList(1));
    }

    private boolean isThrottlingExemptLocked(Identity identity) {
        if (identity.mUid == 1000 || this.mBackgroundThrottlePackageWhitelist.contains(identity.mPackageName)) {
            return true;
        }
        Iterator it = this.mProxyProviders.iterator();
        while (it.hasNext()) {
            if (identity.mPackageName.equals(((LocationProviderProxy) it.next()).getConnectedPackageName())) {
                return true;
            }
        }
        return false;
    }

    private Receiver getReceiverLocked(ILocationListener listener, int pid, int uid, String packageName, WorkSource workSource, boolean hideFromAppOps) {
        IBinder binder = listener.asBinder();
        Receiver receiver = (Receiver) this.mReceivers.get(binder);
        if (receiver == null) {
            Receiver receiver2 = new Receiver(listener, null, pid, uid, packageName, workSource, hideFromAppOps);
            try {
                receiver2.getListener().asBinder().linkToDeath(receiver2, 0);
                this.mReceivers.put(binder, receiver2);
                receiver = receiver2;
            } catch (RemoteException e) {
                Slog.e(TAG, "linkToDeath failed:", e);
                return null;
            }
        }
        return receiver;
    }

    private Receiver getReceiverLocked(PendingIntent intent, int pid, int uid, String packageName, WorkSource workSource, boolean hideFromAppOps) {
        PendingIntent pendingIntent = intent;
        Receiver receiver = (Receiver) this.mReceivers.get(pendingIntent);
        if (receiver != null) {
            return receiver;
        }
        receiver = new Receiver(null, pendingIntent, pid, uid, packageName, workSource, hideFromAppOps);
        this.mReceivers.put(pendingIntent, receiver);
        return receiver;
    }

    private LocationRequest createSanitizedRequest(LocationRequest request, int resolutionLevel, boolean callerHasLocationHardwarePermission) {
        LocationRequest sanitizedRequest = new LocationRequest(request);
        if (!callerHasLocationHardwarePermission) {
            sanitizedRequest.setLowPowerMode(false);
        }
        if (resolutionLevel < 2) {
            int quality = sanitizedRequest.getQuality();
            if (quality == 100) {
                sanitizedRequest.setQuality(102);
            } else if (quality == 203) {
                sanitizedRequest.setQuality(201);
            }
            if (sanitizedRequest.getInterval() < 600000) {
                sanitizedRequest.setInterval(600000);
            }
            if (sanitizedRequest.getFastestInterval() < 600000) {
                sanitizedRequest.setFastestInterval(600000);
            }
        }
        if (sanitizedRequest.getFastestInterval() > sanitizedRequest.getInterval()) {
            request.setFastestInterval(request.getInterval());
        }
        return sanitizedRequest;
    }

    private void checkPackageName(String packageName) {
        if (packageName != null) {
            int uid = Binder.getCallingUid();
            String[] packages = this.mPackageManager.getPackagesForUid(uid);
            StringBuilder stringBuilder;
            if (packages != null) {
                int length = packages.length;
                int i = 0;
                while (i < length) {
                    if (!packageName.equals(packages[i])) {
                        i++;
                    } else {
                        return;
                    }
                }
                stringBuilder = new StringBuilder();
                stringBuilder.append("invalid package name: ");
                stringBuilder.append(packageName);
                throw new SecurityException(stringBuilder.toString());
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("invalid UID ");
            stringBuilder.append(uid);
            throw new SecurityException(stringBuilder.toString());
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("invalid package name: ");
        stringBuilder2.append(packageName);
        throw new SecurityException(stringBuilder2.toString());
    }

    private void checkPendingIntent(PendingIntent intent) {
        if (intent == null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("invalid pending intent: ");
            stringBuilder.append(intent);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    private Receiver checkListenerOrIntentLocked(ILocationListener listener, PendingIntent intent, int pid, int uid, String packageName, WorkSource workSource, boolean hideFromAppOps) {
        if (intent == null && listener == null) {
            throw new IllegalArgumentException("need either listener or intent");
        } else if (intent != null && listener != null) {
            throw new IllegalArgumentException("cannot register both listener and intent");
        } else if (intent == null) {
            return getReceiverLocked(listener, pid, uid, packageName, workSource, hideFromAppOps);
        } else {
            checkPendingIntent(intent);
            return getReceiverLocked(intent, pid, uid, packageName, workSource, hideFromAppOps);
        }
    }

    public void requestLocationUpdates(LocationRequest request, ILocationListener listener, PendingIntent intent, String packageName) {
        LocationRequest request2;
        Throwable th;
        boolean z;
        LocationRequest locationRequest;
        int i;
        Object obj;
        String str = packageName;
        hwSendBehavior(BehaviorId.LOCATIONMANAGER_REQUESTLOCATIONUPDATES);
        if (request == null) {
            request2 = DEFAULT_LOCATION_REQUEST;
        } else {
            request2 = request;
        }
        checkPackageName(str);
        int allowedResolutionLevel = getCallerAllowedResolutionLevel();
        checkResolutionLevelIsSufficientForProviderUse(allowedResolutionLevel, request2.getProvider());
        WorkSource workSource = request2.getWorkSource();
        if (!(workSource == null || workSource.isEmpty())) {
            checkDeviceStatsAllowed();
        }
        boolean hideFromAppOps = request2.getHideFromAppOps();
        if (hideFromAppOps) {
            checkUpdateAppOpsAllowed();
        }
        LocationRequest sanitizedRequest = createSanitizedRequest(request2, allowedResolutionLevel, this.mContext.checkCallingPermission("android.permission.LOCATION_HARDWARE") == 0);
        int pid = Binder.getCallingPid();
        int uid = Binder.getCallingUid();
        HwSystemManager.allowOp(this.mContext, 8);
        long identity = Binder.clearCallingIdentity();
        int i2;
        int i3;
        long identity2;
        try {
            boolean permission = checkLocationAccess(pid, uid, str, allowedResolutionLevel);
            try {
                String str2 = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(" PID: ");
                stringBuilder.append(pid);
                stringBuilder.append(" , uid : ");
                stringBuilder.append(uid);
                stringBuilder.append(" , packageName :");
                stringBuilder.append(str);
                stringBuilder.append(" , allowedResolutionLevel : ");
                stringBuilder.append(allowedResolutionLevel);
                stringBuilder.append(" , permission : ");
                stringBuilder.append(permission);
                Log.i(str2, stringBuilder.toString());
                if (!permission) {
                    try {
                        this.mHwLocationGpsLogServices.permissionErr(str);
                    } catch (Throwable th2) {
                        th = th2;
                        z = permission;
                        i2 = uid;
                        i3 = pid;
                        locationRequest = request2;
                        i = allowedResolutionLevel;
                        identity2 = identity;
                    }
                }
                Object obj2 = this.mLock;
                synchronized (obj2) {
                    try {
                        if (this.mHwQuickTTFFMonitor != null) {
                            try {
                                this.mHwQuickTTFFMonitor.setPermission(permission);
                            } catch (Throwable th3) {
                                th = th3;
                                obj = obj2;
                                z = permission;
                                i2 = uid;
                                i3 = pid;
                                locationRequest = request2;
                                i = allowedResolutionLevel;
                                identity2 = identity;
                            }
                        }
                        obj = obj2;
                        identity2 = identity;
                        i2 = uid;
                        i3 = pid;
                        try {
                            Receiver recevier = checkListenerOrIntentLocked(listener, intent, pid, uid, str, workSource, hideFromAppOps);
                            if (recevier == null) {
                                Log.e(TAG, "recevier creating failed, value is null");
                                Binder.restoreCallingIdentity(identity2);
                                return;
                            }
                            hwRequestLocationUpdatesLocked(sanitizedRequest, recevier, i3, i2, str);
                            requestLocationUpdatesLocked(sanitizedRequest, recevier, i3, i2, str);
                            Binder.restoreCallingIdentity(identity2);
                        } catch (Throwable th4) {
                            th = th4;
                            try {
                                throw th;
                            } catch (Throwable th5) {
                                th = th5;
                            }
                        }
                    } catch (Throwable th6) {
                        th = th6;
                        obj = obj2;
                        z = permission;
                        i2 = uid;
                        i3 = pid;
                        locationRequest = request2;
                        i = allowedResolutionLevel;
                        identity2 = identity;
                        throw th;
                    }
                }
            } catch (Throwable th7) {
                th = th7;
                z = permission;
                i2 = uid;
                i3 = pid;
                locationRequest = request2;
                i = allowedResolutionLevel;
                identity2 = identity;
                Binder.restoreCallingIdentity(identity2);
                throw th;
            }
        } catch (Throwable th8) {
            th = th8;
            i2 = uid;
            i3 = pid;
            locationRequest = request2;
            i = allowedResolutionLevel;
            identity2 = identity;
            z = false;
            Binder.restoreCallingIdentity(identity2);
            throw th;
        }
    }

    private void requestLocationUpdatesLocked(LocationRequest request, Receiver receiver, int pid, int uid, String packageName) {
        LocationRequest request2;
        Receiver receiver2 = receiver;
        int i = pid;
        int i2 = uid;
        String str = packageName;
        if (request == null) {
            request2 = DEFAULT_LOCATION_REQUEST;
        } else {
            request2 = request;
        }
        String name = request2.getProvider();
        if (name != null) {
            String name2 = getLocationProvider(i2, request2, str, name);
            if (this.mHwGpsActionReporter != null && ("gps".equals(name2) || "network".equals(name2) || "fused".equals(name2))) {
                StringBuilder strBuilder = new StringBuilder(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                strBuilder.append("PROVIDER:");
                strBuilder.append(name2);
                strBuilder.append(",PN:");
                strBuilder.append(str);
                strBuilder.append(",HC:");
                strBuilder.append(Integer.toHexString(System.identityHashCode(receiver)));
                String reportString = strBuilder.toString();
                this.mLocationHandler.removeMessages(2, reportString);
                this.mLocationHandler.sendMessage(Message.obtain(this.mLocationHandler, 2, reportString));
            }
            name = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("request ");
            stringBuilder.append(Integer.toHexString(System.identityHashCode(receiver)));
            stringBuilder.append(" ");
            stringBuilder.append(name2);
            stringBuilder.append(" ");
            stringBuilder.append(request2);
            stringBuilder.append(" from ");
            stringBuilder.append(str);
            stringBuilder.append("(");
            stringBuilder.append(i2);
            stringBuilder.append(")");
            Log.i(name, stringBuilder.toString());
            String hashCode = Integer.toHexString(System.identityHashCode(receiver));
            String str2 = name2;
            String str3 = hashCode;
            String str4 = str;
            this.mHwLocationGpsLogServices.updateApkName(str2, str3, str4, APKSTART, String.valueOf(System.currentTimeMillis()));
            LocationProviderInterface provider = (LocationProviderInterface) this.mProvidersByName.get(name2);
            if (provider != null) {
                UpdateRecord oldRecord = (UpdateRecord) receiver2.mUpdateRecords.put(name2, new UpdateRecord(name2, request2, receiver2));
                boolean z = false;
                if (oldRecord != null) {
                    oldRecord.disposeLocked(false);
                }
                if ("gps".equals(name2)) {
                    LogPower.push(202, str, Integer.toString(pid), Long.toString(request2.getInterval()), new String[]{Integer.toHexString(System.identityHashCode(receiver))});
                }
                this.mCHRFirstReqFlag = false;
                if (isAllowedByUserSettingsLocked(name2, i2, this.mCurrentUserId)) {
                    if (name2.equals("network")) {
                        if (provider instanceof IHwLocationProviderInterface) {
                            ((IHwLocationProviderInterface) provider).resetNLPFlag();
                        } else {
                            Log.d(TAG, "instanceof fail");
                        }
                    }
                    if (!isFreeze(str)) {
                        this.mCHRFirstReqFlag = true;
                    }
                    try {
                        Bundle requestBundle = new Bundle();
                        requestBundle.putLong(WatchlistEventKeys.TIMESTAMP, System.currentTimeMillis());
                        requestBundle.putString(HwBroadcastRadarUtil.KEY_ACTION, "start");
                        requestBundle.putString("provider", name2);
                        requestBundle.putInt("interval", (int) request2.getInterval());
                        requestBundle.putString("pkgName", str);
                        requestBundle.putInt(HwBroadcastRadarUtil.KEY_RECEIVER, System.identityHashCode(receiver));
                        this.mHwLbsLogger.loggerEvent(101, requestBundle);
                    } catch (Exception e) {
                        Log.d(TAG, "exception occured when deliver start action");
                    }
                    HwSystemManager.notifyBackgroundMgr(str, i, i2, 2, 1);
                    this.mHwQuickTTFFMonitor.requestHwQuickTTFF(request2, str, name2, Integer.toHexString(System.identityHashCode(receiver)));
                    applyRequirementsLocked(name2);
                    printFormatLog(str, i, "requestLocationUpdatesLocked", name2);
                } else {
                    receiver2.callProviderEnabledLocked(name2, false);
                    this.mHwLocationGpsLogServices.LocationSettingsOffErr(name2);
                    name = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("provider:");
                    stringBuilder2.append(name2);
                    stringBuilder2.append(" is disable");
                    Log.i(name, stringBuilder2.toString());
                }
                if (this.mLocalLocationProvider != null && this.mLocalLocationProvider.isEnabled() && "gps".equals(name2)) {
                    this.mLocalLocationProvider.requestLocation();
                }
                receiver2.updateMonitoring(true);
                int quality = request2.getQuality();
                if (receiver2.mListener == null) {
                    z = true;
                }
                hwLocationPowerTrackerRecordRequest(str, quality, z);
                return;
            }
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("provider doesn't exist: ");
            stringBuilder3.append(name2);
            throw new IllegalArgumentException(stringBuilder3.toString());
        }
        throw new IllegalArgumentException("provider name must not be null");
    }

    public void removeUpdates(ILocationListener listener, PendingIntent intent, String packageName) {
        hwSendBehavior(BehaviorId.LOCATIONMANAGER_REMOVEUPDATES);
        String str = packageName;
        checkPackageName(str);
        int pid = Binder.getCallingPid();
        int uid = Binder.getCallingUid();
        synchronized (this.mLock) {
            Receiver receiver = checkListenerOrIntentLocked(listener, intent, pid, uid, str, null, false);
            long identity = Binder.clearCallingIdentity();
            if (receiver != null) {
                try {
                    removeUpdatesLocked(receiver);
                } catch (Throwable th) {
                    Binder.restoreCallingIdentity(identity);
                    Throwable th2 = th;
                }
            }
            Binder.restoreCallingIdentity(identity);
        }
    }

    private void removeUpdatesLocked(Receiver receiver) {
        hwRemoveUpdatesLocked(receiver);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("remove ");
        stringBuilder.append(Integer.toHexString(System.identityHashCode(receiver)));
        Log.i(str, stringBuilder.toString());
        str = Integer.toHexString(System.identityHashCode(receiver));
        String str2 = "APKSTOPPROVIDER";
        String str3 = str;
        this.mHwLocationGpsLogServices.updateApkName(str2, str3, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, APKSTOP, String.valueOf(System.currentTimeMillis()));
        if (this.mHwGpsActionReporter != null) {
            stringBuilder = new StringBuilder(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            stringBuilder.append("PROVIDER:all,PN:");
            stringBuilder.append(receiver.mIdentity.mPackageName);
            stringBuilder.append(",HC:");
            stringBuilder.append(Integer.toHexString(System.identityHashCode(receiver)));
            str2 = stringBuilder.toString();
            this.mLocationHandler.removeMessages(3, str2);
            this.mLocationHandler.sendMessage(Message.obtain(this.mLocationHandler, 3, str2));
        }
        try {
            Bundle removeBundle = new Bundle();
            removeBundle.putLong(WatchlistEventKeys.TIMESTAMP, System.currentTimeMillis());
            removeBundle.putString(HwBroadcastRadarUtil.KEY_ACTION, "stop");
            removeBundle.putString("pkgName", receiver.mIdentity.mPackageName);
            removeBundle.putInt(HwBroadcastRadarUtil.KEY_RECEIVER, System.identityHashCode(receiver));
            this.mHwLbsLogger.loggerEvent(101, removeBundle);
        } catch (Exception e) {
            Log.d(TAG, "exception occured when deliver stop session");
        }
        LogPower.push(203, receiver.mIdentity.mPackageName, Integer.toString(receiver.mIdentity.mPid), Integer.toString(-1), new String[]{Integer.toHexString(System.identityHashCode(receiver))});
        HwSystemManager.notifyBackgroundMgr(receiver.mIdentity.mPackageName, receiver.mIdentity.mPid, receiver.mIdentity.mUid, 2, 0);
        if (this.mReceivers.remove(receiver.mKey) != null && receiver.isListener()) {
            receiver.getListener().asBinder().unlinkToDeath(receiver, 0);
            synchronized (receiver) {
                receiver.clearPendingBroadcastsLocked();
            }
        }
        this.mHwQuickTTFFMonitor.removeHwQuickTTFF(receiver.mIdentity.mPackageName, Integer.toHexString(System.identityHashCode(receiver)));
        receiver.updateMonitoring(false);
        hwLocationPowerTrackerRemoveRequest(receiver.mIdentity.mPackageName);
        HashSet<String> providers = new HashSet();
        HashMap<String, UpdateRecord> oldRecords = receiver.mUpdateRecords;
        if (oldRecords != null) {
            for (UpdateRecord record : oldRecords.values()) {
                record.disposeLocked(false);
            }
            providers.addAll(oldRecords.keySet());
        }
        Iterator it = providers.iterator();
        while (it.hasNext()) {
            String provider = (String) it.next();
            String str4 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("isAllowedByCurrentUserSettingsLocked started: ");
            stringBuilder2.append(provider);
            Log.i(str4, stringBuilder2.toString());
            if (isAllowedByCurrentUserSettingsLocked(provider)) {
                applyRequirementsLocked(provider);
            }
        }
    }

    private void applyAllProviderRequirementsLocked() {
        Iterator it = this.mProviders.iterator();
        while (it.hasNext()) {
            LocationProviderInterface p = (LocationProviderInterface) it.next();
            if (isAllowedByCurrentUserSettingsLocked(p.getName())) {
                applyRequirementsLocked(p.getName());
            }
        }
    }

    public Location getLastLocation(LocationRequest request, String packageName) {
        hwSendBehavior(BehaviorId.LOCATIONMANAGER_GETLASTLOCATION);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getLastLocation: ");
        stringBuilder.append(request);
        Log.i(str, stringBuilder.toString());
        if (request == null) {
            request = DEFAULT_LOCATION_REQUEST;
        }
        int allowedResolutionLevel = getCallerAllowedResolutionLevel();
        checkPackageName(packageName);
        checkResolutionLevelIsSufficientForProviderUse(allowedResolutionLevel, request.getProvider());
        HwSystemManager.allowOp(this.mContext, 8);
        int pid = Binder.getCallingPid();
        int uid = Binder.getCallingUid();
        long identity = Binder.clearCallingIdentity();
        try {
            String str2;
            StringBuilder stringBuilder2;
            if (this.mBlacklist.isBlacklisted(packageName)) {
                str2 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("not returning last loc for blacklisted app: ");
                stringBuilder2.append(packageName);
                Log.i(str2, stringBuilder2.toString());
                Binder.restoreCallingIdentity(identity);
                return null;
            } else if (reportLocationAccessNoThrow(pid, uid, packageName, allowedResolutionLevel)) {
                synchronized (this.mLock) {
                    String name = request.getProvider();
                    if (name == null) {
                        name = "fused";
                    }
                    if (((LocationProviderInterface) this.mProvidersByName.get(name)) == null) {
                        Binder.restoreCallingIdentity(identity);
                        return null;
                    } else if (isAllowedByUserSettingsLocked(name, uid, this.mCurrentUserId) || isExceptionAppForUserSettingsLocked(packageName)) {
                        Location location;
                        if (allowedResolutionLevel < 2) {
                            location = (Location) this.mLastLocationCoarseInterval.get(name);
                        } else {
                            location = (Location) this.mLastLocation.get(name);
                        }
                        Location readLastLocationDataBase;
                        if (location == null) {
                            if (isExceptionAppForUserSettingsLocked(packageName)) {
                                readLastLocationDataBase = readLastLocationDataBase();
                                Binder.restoreCallingIdentity(identity);
                                return readLastLocationDataBase;
                            }
                            Binder.restoreCallingIdentity(identity);
                            return null;
                        } else if (allowedResolutionLevel < 2) {
                            Location noGPSLocation = location.getExtraLocation("noGPSLocation");
                            if (noGPSLocation != null) {
                                readLastLocationDataBase = new Location(this.mLocationFudger.getOrCreate(noGPSLocation));
                                Binder.restoreCallingIdentity(identity);
                                return readLastLocationDataBase;
                            }
                            Binder.restoreCallingIdentity(identity);
                            return null;
                        } else {
                            readLastLocationDataBase = new Location(location);
                            Binder.restoreCallingIdentity(identity);
                            return readLastLocationDataBase;
                        }
                    } else {
                        Binder.restoreCallingIdentity(identity);
                        return null;
                    }
                }
            } else {
                str2 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("not returning last loc for no op app: ");
                stringBuilder2.append(packageName);
                Log.i(str2, stringBuilder2.toString());
                Binder.restoreCallingIdentity(identity);
                return null;
            }
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public boolean injectLocation(Location location) {
        this.mContext.enforceCallingPermission("android.permission.LOCATION_HARDWARE", "Location Hardware permission not granted to inject location");
        this.mContext.enforceCallingPermission("android.permission.ACCESS_FINE_LOCATION", "Access Fine Location permission not granted to inject Location");
        if (location == null) {
            return false;
        }
        LocationProviderInterface p = null;
        String provider = location.getProvider();
        if (provider != null) {
            p = (LocationProviderInterface) this.mProvidersByName.get(provider);
        }
        if (p == null) {
            return false;
        }
        synchronized (this.mLock) {
            if (!isAllowedByCurrentUserSettingsLocked(provider)) {
                return false;
            } else if (this.mLastLocation.get(provider) == null) {
                updateLastLocationLocked(location, provider);
                return true;
            } else {
                return false;
            }
        }
    }

    public void requestGeofence(LocationRequest request, Geofence geofence, PendingIntent intent, String packageName) {
        LocationRequest request2;
        Throwable th;
        long identity;
        int i;
        PendingIntent pendingIntent = intent;
        hwSendBehavior(BehaviorId.LOCATIONMANAGER_REQUESTGEOFENCE);
        if (request == null) {
            request2 = DEFAULT_LOCATION_REQUEST;
        } else {
            request2 = request;
        }
        int allowedResolutionLevel = getCallerAllowedResolutionLevel();
        checkResolutionLevelIsSufficientForGeofenceUse(allowedResolutionLevel);
        checkPendingIntent(pendingIntent);
        String str = packageName;
        checkPackageName(str);
        checkResolutionLevelIsSufficientForProviderUse(allowedResolutionLevel, request2.getProvider());
        boolean callerHasLocationHardwarePermission = this.mContext.checkCallingPermission("android.permission.LOCATION_HARDWARE") == 0;
        LocationRequest sanitizedRequest = createSanitizedRequest(request2, allowedResolutionLevel, callerHasLocationHardwarePermission);
        String str2 = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("requestGeofence: ");
        stringBuilder.append(sanitizedRequest);
        stringBuilder.append(" ");
        Geofence geofence2 = geofence;
        stringBuilder.append(geofence2);
        stringBuilder.append(" ");
        stringBuilder.append(pendingIntent);
        Log.i(str2, stringBuilder.toString());
        int uid = Binder.getCallingUid();
        if (UserHandle.getUserId(uid) != 0) {
            Log.w(TAG, "proximity alerts are currently available only to the primary user");
            return;
        }
        Geofence geofence3 = geofence2;
        LocationRequest locationRequest = sanitizedRequest;
        int i2 = uid;
        long identity2 = Binder.clearCallingIdentity();
        LocationRequest locationRequest2;
        boolean z;
        LocationRequest locationRequest3;
        try {
            if (addQcmGeoFencer(geofence3, locationRequest, i2, pendingIntent, str)) {
                locationRequest2 = sanitizedRequest;
                z = callerHasLocationHardwarePermission;
                i2 = allowedResolutionLevel;
                locationRequest3 = request2;
            } else {
                try {
                    try {
                        this.mGeofenceManager.addFence(sanitizedRequest, geofence, pendingIntent, allowedResolutionLevel, uid, packageName);
                    } catch (Throwable th2) {
                        th = th2;
                        identity = identity2;
                    }
                } catch (Throwable th3) {
                    th = th3;
                    i = uid;
                    locationRequest2 = sanitizedRequest;
                    z = callerHasLocationHardwarePermission;
                    i2 = allowedResolutionLevel;
                    locationRequest3 = request2;
                    identity = identity2;
                    Binder.restoreCallingIdentity(identity);
                    throw th;
                }
            }
            Binder.restoreCallingIdentity(identity2);
        } catch (Throwable th4) {
            th = th4;
            i = uid;
            locationRequest2 = sanitizedRequest;
            z = callerHasLocationHardwarePermission;
            i2 = allowedResolutionLevel;
            locationRequest3 = request2;
            identity = identity2;
            Binder.restoreCallingIdentity(identity);
            throw th;
        }
    }

    public void removeGeofence(Geofence geofence, PendingIntent intent, String packageName) {
        checkPendingIntent(intent);
        checkPackageName(packageName);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("removeGeofence: ");
        stringBuilder.append(geofence);
        stringBuilder.append(" ");
        stringBuilder.append(intent);
        Log.i(str, stringBuilder.toString());
        long identity = Binder.clearCallingIdentity();
        try {
            if (!removeQcmGeoFencer(intent)) {
                this.mGeofenceManager.removeFence(geofence, intent);
            }
            Binder.restoreCallingIdentity(identity);
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public boolean registerGnssStatusCallback(IGnssStatusListener callback, String packageName) {
        hwSendBehavior(BehaviorId.LOCATIONMANAGER_REGISTERGNSSSTATUSCALLBACK);
        if (!hasGnssPermissions(packageName) || this.mGnssStatusProvider == null) {
            return false;
        }
        try {
            this.mGnssStatusProvider.registerGnssStatusCallback(callback);
            return true;
        } catch (RemoteException e) {
            Slog.e(TAG, "mGpsStatusProvider.registerGnssStatusCallback failed", e);
            return false;
        }
    }

    public void unregisterGnssStatusCallback(IGnssStatusListener callback) {
        synchronized (this.mLock) {
            try {
                this.mGnssStatusProvider.unregisterGnssStatusCallback(callback);
            } catch (Exception e) {
                Slog.e(TAG, "mGpsStatusProvider.unregisterGnssStatusCallback failed", e);
            }
        }
    }

    /* JADX WARNING: Missing block: B:18:0x0070, code:
            return r4;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean addGnssMeasurementsListener(IGnssMeasurementsListener listener, String packageName) {
        if (!hasGnssPermissions(packageName) || this.mGnssMeasurementsProvider == null) {
            return false;
        }
        synchronized (this.mLock) {
            Identity callerIdentity = new Identity(Binder.getCallingUid(), Binder.getCallingPid(), packageName);
            this.mGnssMeasurementsListeners.put(listener.asBinder(), callerIdentity);
            long identity = Binder.clearCallingIdentity();
            try {
                if ((isThrottlingExemptLocked(callerIdentity) || isImportanceForeground(this.mActivityManager.getPackageImportance(packageName))) && !isFreeze(packageName)) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("addGnssMeasurementsListener ");
                    stringBuilder.append(Integer.toHexString(System.identityHashCode(listener)));
                    stringBuilder.append(" packageName ");
                    stringBuilder.append(packageName);
                    Log.i(str, stringBuilder.toString());
                    boolean addListener = this.mGnssMeasurementsProvider.addListener(listener, packageName);
                } else {
                    Binder.restoreCallingIdentity(identity);
                    return true;
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }
    }

    public void removeGnssMeasurementsListener(IGnssMeasurementsListener listener) {
        if (this.mGnssMeasurementsProvider != null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("removeGnssMeasurementsListener ");
            stringBuilder.append(Integer.toHexString(System.identityHashCode(listener)));
            Log.i(str, stringBuilder.toString());
            synchronized (this.mLock) {
                this.mGnssMeasurementsListeners.remove(listener.asBinder());
                this.mGnssMeasurementsProvider.removeListener(listener);
            }
        }
    }

    public boolean addGnssNavigationMessageListener(IGnssNavigationMessageListener listener, String packageName) {
        if (!hasGnssPermissions(packageName) || this.mGnssNavigationMessageProvider == null) {
            return false;
        }
        synchronized (this.mLock) {
            Identity callerIdentity = new Identity(Binder.getCallingUid(), Binder.getCallingPid(), packageName);
            this.mGnssNavigationMessageListeners.put(listener.asBinder(), callerIdentity);
            long identity = Binder.clearCallingIdentity();
            try {
                if (isThrottlingExemptLocked(callerIdentity) || isImportanceForeground(this.mActivityManager.getPackageImportance(packageName))) {
                    boolean addListener = this.mGnssNavigationMessageProvider.addListener(listener, packageName);
                    Binder.restoreCallingIdentity(identity);
                    return addListener;
                }
                return true;
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }
    }

    public void removeGnssNavigationMessageListener(IGnssNavigationMessageListener listener) {
        if (this.mGnssNavigationMessageProvider != null) {
            synchronized (this.mLock) {
                this.mGnssNavigationMessageListeners.remove(listener.asBinder());
                this.mGnssNavigationMessageProvider.removeListener(listener);
            }
        }
    }

    public boolean sendExtraCommand(String provider, String command, Bundle extras) {
        hwSendBehavior(BehaviorId.LOCATIONMANAGER_SENDEXTRACOMMAND);
        if (provider != null) {
            checkResolutionLevelIsSufficientForProviderUse(getCallerAllowedResolutionLevel(), provider);
            if (this.mContext.checkCallingOrSelfPermission(ACCESS_LOCATION_EXTRA_COMMANDS) == 0) {
                HwSystemManager.allowOp(this.mContext, 8);
                synchronized (this.mLock) {
                    LocationProviderInterface p = (LocationProviderInterface) this.mProvidersByName.get(provider);
                    if (p == null) {
                        return false;
                    }
                    boolean sendExtraCommand = p.sendExtraCommand(command, extras);
                    return sendExtraCommand;
                }
            }
            throw new SecurityException("Requires ACCESS_LOCATION_EXTRA_COMMANDS permission");
        }
        throw new NullPointerException();
    }

    public boolean sendNiResponse(int notifId, int userResponse) {
        if (Binder.getCallingUid() == Process.myUid()) {
            try {
                return this.mNetInitiatedListener.sendNiResponse(notifId, userResponse);
            } catch (RemoteException e) {
                Slog.e(TAG, "RemoteException in LocationManagerService.sendNiResponse");
                return false;
            }
        }
        throw new SecurityException("calling sendNiResponse from outside of the system is not allowed");
    }

    public ProviderProperties getProviderProperties(String provider) {
        if (this.mProvidersByName.get(provider) == null) {
            return null;
        }
        LocationProviderInterface p;
        checkResolutionLevelIsSufficientForProviderUse(getCallerAllowedResolutionLevel(), provider);
        synchronized (this.mLock) {
            p = (LocationProviderInterface) this.mProvidersByName.get(provider);
        }
        if (p == null) {
            return null;
        }
        return p.getProperties();
    }

    /* JADX WARNING: Missing block: B:10:0x001f, code:
            if ((r1 instanceof com.android.server.location.LocationProviderProxy) == false) goto L_0x0039;
     */
    /* JADX WARNING: Missing block: B:11:0x0021, code:
            r0 = ((com.android.server.location.LocationProviderProxy) r1).getConnectedPackageName();
            r2 = r0;
     */
    /* JADX WARNING: Missing block: B:12:0x0029, code:
            if (r0 == null) goto L_0x0038;
     */
    /* JADX WARNING: Missing block: B:13:0x002b, code:
            r3 = r0.split(";");
     */
    /* JADX WARNING: Missing block: B:14:0x0033, code:
            if (r3.length < 2) goto L_0x0038;
     */
    /* JADX WARNING: Missing block: B:15:0x0035, code:
            r2 = r3[0];
     */
    /* JADX WARNING: Missing block: B:16:0x0038, code:
            return r2;
     */
    /* JADX WARNING: Missing block: B:17:0x0039, code:
            return null;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public String getNetworkProviderPackage() {
        synchronized (this.mLock) {
            if (this.mProvidersByName.get("network") == null) {
                return null;
            }
            LocationProviderInterface p = (LocationProviderInterface) this.mProvidersByName.get("network");
        }
    }

    public boolean isLocationEnabledForUser(int userId) {
        checkInteractAcrossUsersPermission(userId);
        long identity = Binder.clearCallingIdentity();
        try {
            boolean z = "location_providers_allowed";
            String allowedProviders = Secure.getStringForUser(this.mContext.getContentResolver(), z, userId);
            z = false;
            if (allowedProviders == null) {
                return z;
            }
            List<String> providerList = Arrays.asList(allowedProviders.split(","));
            for (String provider : ((HashMap) this.mRealProviders.clone()).keySet()) {
                if (!provider.equals("passive") && !provider.equals("fused") && providerList.contains(provider)) {
                    Binder.restoreCallingIdentity(identity);
                    return true;
                }
            }
            Binder.restoreCallingIdentity(identity);
            return false;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void setLocationEnabledForUser(boolean enabled, int userId) {
        this.mContext.enforceCallingPermission("android.permission.WRITE_SECURE_SETTINGS", "Requires WRITE_SECURE_SETTINGS permission");
        checkInteractAcrossUsersPermission(userId);
        long identity = Binder.clearCallingIdentity();
        try {
            synchronized (this.mLock) {
                Set<String> allRealProviders = this.mRealProviders.keySet();
                Set<String> allProvidersSet = new ArraySet(allRealProviders.size() + 2);
                allProvidersSet.addAll(allRealProviders);
                if (!enabled) {
                    allProvidersSet.add("gps");
                    allProvidersSet.add("network");
                }
                if (allProvidersSet.isEmpty()) {
                    Binder.restoreCallingIdentity(identity);
                    return;
                }
                String prefix = enabled ? "+" : "-";
                StringBuilder locationProvidersAllowed = new StringBuilder();
                for (String provider : allProvidersSet) {
                    if (!(provider.equals("passive") || provider.equals("fused"))) {
                        if (!provider.equals(IHwLocalLocationProvider.LOCAL_PROVIDER)) {
                            locationProvidersAllowed.append(prefix);
                            locationProvidersAllowed.append(provider);
                            locationProvidersAllowed.append(",");
                        }
                    }
                }
                locationProvidersAllowed.setLength(locationProvidersAllowed.length() - 1);
                Secure.putStringForUser(this.mContext.getContentResolver(), "location_providers_allowed", locationProvidersAllowed.toString(), userId);
                Binder.restoreCallingIdentity(identity);
            }
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public boolean isProviderEnabledForUser(String provider, int userId) {
        checkInteractAcrossUsersPermission(userId);
        boolean z = false;
        if ("fused".equals(provider)) {
            return false;
        }
        int uid = Binder.getCallingUid();
        synchronized (this.mLock) {
            if (((LocationProviderInterface) this.mProvidersByName.get(provider)) != null && isAllowedByUserSettingsLocked(provider, uid, userId)) {
                z = true;
            }
        }
        return z;
    }

    public boolean setProviderEnabledForUser(String provider, boolean enabled, int userId) {
        this.mContext.enforceCallingPermission("android.permission.WRITE_SECURE_SETTINGS", "Requires WRITE_SECURE_SETTINGS permission");
        checkInteractAcrossUsersPermission(userId);
        if ("fused".equals(provider)) {
            return false;
        }
        long identity = Binder.clearCallingIdentity();
        try {
            synchronized (this.mLock) {
                if (!this.mProvidersByName.containsKey(provider)) {
                    Binder.restoreCallingIdentity(identity);
                    return false;
                } else if (this.mMockProviders.containsKey(provider)) {
                    setTestProviderEnabled(provider, enabled);
                    Binder.restoreCallingIdentity(identity);
                    return true;
                } else {
                    String providerChange = new StringBuilder();
                    providerChange.append(enabled ? "+" : "-");
                    providerChange.append(provider);
                    boolean putStringForUser = Secure.putStringForUser(this.mContext.getContentResolver(), "location_providers_allowed", providerChange.toString(), userId);
                    Binder.restoreCallingIdentity(identity);
                    return putStringForUser;
                }
            }
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(identity);
        }
    }

    private boolean isLocationProviderEnabledForUser(String provider, int userId) {
        long identity = Binder.clearCallingIdentity();
        try {
            boolean delimitedStringContains = TextUtils.delimitedStringContains(Secure.getStringForUser(this.mContext.getContentResolver(), "location_providers_allowed", userId), ',', provider);
            return delimitedStringContains;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    private void checkInteractAcrossUsersPermission(int userId) {
        int uid = Binder.getCallingUid();
        if (UserHandle.getUserId(uid) != userId && ActivityManager.checkComponentPermission("android.permission.INTERACT_ACROSS_USERS", uid, -1, true) != 0) {
            throw new SecurityException("Requires INTERACT_ACROSS_USERS permission");
        }
    }

    private boolean isUidALocationProvider(int uid) {
        if (uid == 1000) {
            return true;
        }
        if (this.mGeocodeProvider != null && doesUidHavePackage(uid, this.mGeocodeProvider.getConnectedPackageName())) {
            return true;
        }
        Iterator it = this.mProxyProviders.iterator();
        while (it.hasNext()) {
            if (doesUidHavePackage(uid, ((LocationProviderProxy) it.next()).getConnectedPackageName())) {
                return true;
            }
        }
        return false;
    }

    private void checkCallerIsProvider() {
        if (this.mContext.checkCallingOrSelfPermission(INSTALL_LOCATION_PROVIDER) != 0 && !isUidALocationProvider(Binder.getCallingUid())) {
            throw new SecurityException("need INSTALL_LOCATION_PROVIDER permission, or UID of a currently bound location provider");
        }
    }

    private boolean doesUidHavePackage(int uid, String packageName) {
        if (packageName == null) {
            return false;
        }
        String[] packageNames = this.mPackageManager.getPackagesForUid(uid);
        if (packageNames == null) {
            return false;
        }
        String[] pNames = packageName.split(";");
        if (pNames.length >= 2) {
            for (String pName : pNames) {
                for (String name : packageNames) {
                    if (pName.equals(name)) {
                        return true;
                    }
                }
            }
        } else {
            for (String pName2 : packageNames) {
                if (packageName.equals(pName2)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void reportLocation(Location location, boolean passive) {
        checkCallerIsProvider();
        if (this.mHwQuickTTFFMonitor.isReport(location)) {
            this.mHwQuickTTFFMonitor.setQuickTTFFLocation(location, passive);
            String provider;
            if (location.isComplete()) {
                HwSystemManager.allowOp(this.mContext, 8);
                provider = passive ? "passive" : location.getProvider();
                if (provider.equals("network")) {
                    LocationProviderInterface p = (LocationProviderInterface) this.mProvidersByName.get(provider);
                    if (p == null || !(p instanceof IHwLocationProviderInterface)) {
                        Log.d(TAG, "instanceof fail");
                    } else if (!((IHwLocationProviderInterface) p).reportNLPLocation(Binder.getCallingPid())) {
                        return;
                    }
                }
                this.mLocationHandler.removeMessages(1, location);
                Message m = Message.obtain(this.mLocationHandler, 1, location);
                m.arg1 = passive;
                this.mLocationHandler.sendMessageAtFrontOfQueue(m);
                return;
            }
            provider = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Dropping incomplete location: ");
            stringBuilder.append(location);
            Log.w(provider, stringBuilder.toString());
            return;
        }
        Log.d(TAG, "QuickTTFFMonitor is not running return");
    }

    private static boolean shouldBroadcastSafe(Location loc, Location lastLoc, UpdateRecord record, long now) {
        boolean z = true;
        if (lastLoc == null) {
            return true;
        }
        if ((loc.getElapsedRealtimeNanos() - lastLoc.getElapsedRealtimeNanos()) / NANOS_PER_MILLI < record.mRealRequest.getFastestInterval() - 100) {
            return false;
        }
        double minDistance = (double) record.mRealRequest.getSmallestDisplacement();
        if ((minDistance > 0.0d && ((double) loc.distanceTo(lastLoc)) <= minDistance) || record.mRealRequest.getNumUpdates() <= 0) {
            return false;
        }
        if (record.mRealRequest.getExpireAt() < now) {
            z = false;
        }
        return z;
    }

    /* JADX WARNING: Removed duplicated region for block: B:99:0x0327  */
    /* JADX WARNING: Removed duplicated region for block: B:98:0x0320  */
    /* JADX WARNING: Removed duplicated region for block: B:109:0x0343  */
    /* JADX WARNING: Removed duplicated region for block: B:102:0x032e  */
    /* JADX WARNING: Removed duplicated region for block: B:94:0x0310  */
    /* JADX WARNING: Removed duplicated region for block: B:98:0x0320  */
    /* JADX WARNING: Removed duplicated region for block: B:99:0x0327  */
    /* JADX WARNING: Removed duplicated region for block: B:102:0x032e  */
    /* JADX WARNING: Removed duplicated region for block: B:109:0x0343  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void handleLocationChangedLocked(Location location, boolean passive) {
        Location location2 = location;
        boolean z = passive;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("incoming location: ");
        stringBuilder.append(location2);
        stringBuilder.append("; passive:");
        stringBuilder.append(z);
        stringBuilder.append("; quickttff:");
        stringBuilder.append(this.mHwQuickTTFFMonitor.isQuickLocation(location2));
        Log.i(str, stringBuilder.toString());
        long now = SystemClock.elapsedRealtime();
        String provider = z ? "passive" : location.getProvider();
        this.mHwQuickTTFFMonitor.removeAllHwQuickTTFF(provider);
        long nowNanos = SystemClock.elapsedRealtimeNanos();
        if (location.getProvider() == "gps" && this.mHwQuickTTFFMonitor.isQuickLocation(location2)) {
            this.mHwLocationGpsLogServices.updateLocation(location2, nowNanos, "quickgps");
        } else {
            this.mHwLocationGpsLogServices.updateLocation(location2, nowNanos, provider);
        }
        updateLocalLocationDB(location2, provider);
        LocationProviderInterface p = (LocationProviderInterface) this.mProvidersByName.get(provider);
        if (p != null) {
            updateLastLocationLocked(location2, provider);
            Location lastLocation = (Location) this.mLastLocation.get(provider);
            if (lastLocation == null) {
                Log.e(TAG, "handleLocationChangedLocked() updateLastLocation failed");
                return;
            }
            Location lastLocationCoarseInterval = (Location) this.mLastLocationCoarseInterval.get(provider);
            if (lastLocationCoarseInterval == null) {
                lastLocationCoarseInterval = new Location(location2);
                this.mLastLocationCoarseInterval.put(provider, lastLocationCoarseInterval);
            }
            Location lastLocationCoarseInterval2 = lastLocationCoarseInterval;
            long timeDiffNanos = location.getElapsedRealtimeNanos() - lastLocationCoarseInterval2.getElapsedRealtimeNanos();
            if (timeDiffNanos > 600000000000L) {
                lastLocationCoarseInterval2.set(location2);
            }
            Location noGPSLocation = lastLocationCoarseInterval2.getExtraLocation("noGPSLocation");
            ArrayList<UpdateRecord> records = (ArrayList) this.mRecordsByProvider.get(provider);
            LocationProviderInterface locationProviderInterface;
            Location location3;
            Location location4;
            long j;
            Location location5;
            ArrayList<UpdateRecord> arrayList;
            if (records == null) {
                locationProviderInterface = p;
                location3 = lastLocation;
                location4 = lastLocationCoarseInterval2;
                j = timeDiffNanos;
                location5 = noGPSLocation;
                arrayList = records;
            } else if (records.size() == 0) {
                long j2 = nowNanos;
                locationProviderInterface = p;
                location3 = lastLocation;
                location4 = lastLocationCoarseInterval2;
                j = timeDiffNanos;
                location5 = noGPSLocation;
                arrayList = records;
            } else {
                Location coarseLocation;
                String str2;
                ArrayList<UpdateRecord> deadUpdateRecords;
                ArrayList<Receiver> deadReceivers;
                Iterator it;
                if (noGPSLocation != null) {
                    coarseLocation = null;
                    coarseLocation = this.mLocationFudger.getOrCreate(noGPSLocation);
                } else {
                    coarseLocation = null;
                }
                nowNanos = p.getStatusUpdateTime();
                location3 = lastLocation;
                Bundle extras = new Bundle();
                lastLocation = p.getStatus(extras);
                if (provider.equals("network")) {
                    ArrayList<Integer> statusList = extras.getIntegerArrayList("status");
                    if (statusList != null) {
                        Iterator p2 = statusList.iterator();
                        while (p2.hasNext()) {
                            ArrayList<Integer> statusList2 = statusList;
                            Integer statusList3 = (Integer) p2.next();
                            Iterator it2 = p2;
                            str2 = TAG;
                            location4 = lastLocationCoarseInterval2;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            j = timeDiffNanos;
                            stringBuilder2.append("list network LocationChanged,  NLP status: ");
                            stringBuilder2.append(statusList3);
                            stringBuilder2.append(" , provider : ");
                            stringBuilder2.append(provider);
                            Log.d(str2, stringBuilder2.toString());
                            this.mHwLocationGpsLogServices.updateNLPStatus(statusList3.intValue());
                            statusList = statusList2;
                            p2 = it2;
                            lastLocationCoarseInterval2 = location4;
                            timeDiffNanos = j;
                        }
                        location4 = lastLocationCoarseInterval2;
                        j = timeDiffNanos;
                    } else {
                        locationProviderInterface = p;
                        location4 = lastLocationCoarseInterval2;
                        j = timeDiffNanos;
                        str = TAG;
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append(" network LocationChanged,  NLP status: ");
                        stringBuilder3.append(lastLocation);
                        stringBuilder3.append(" , provider : ");
                        stringBuilder3.append(provider);
                        Log.d(str, stringBuilder3.toString());
                        this.mHwLocationGpsLogServices.updateNLPStatus(lastLocation);
                    }
                } else {
                    location4 = lastLocationCoarseInterval2;
                    j = timeDiffNanos;
                }
                ArrayList<UpdateRecord> deadUpdateRecords2 = null;
                lastLocationCoarseInterval2 = new ArrayList();
                Iterator receiver = records.iterator();
                ArrayList<Receiver> deadReceivers2 = null;
                while (receiver.hasNext()) {
                    UpdateRecord r = (UpdateRecord) receiver.next();
                    Iterator it3 = receiver;
                    Receiver receiver2 = r.mReceiver;
                    boolean receiverDead = false;
                    location5 = noGPSLocation;
                    int receiverUserId = UserHandle.getUserId(receiver2.mIdentity.mUid);
                    if (isCurrentProfile(receiverUserId)) {
                    } else {
                        if (!isUidALocationProvider(receiver2.mIdentity.mUid)) {
                            deadUpdateRecords = deadUpdateRecords2;
                            deadReceivers = deadReceivers2;
                            arrayList = records;
                            receiver = it3;
                            noGPSLocation = location5;
                            records = arrayList;
                            deadReceivers2 = deadReceivers;
                            deadUpdateRecords2 = deadUpdateRecords;
                        }
                    }
                    arrayList = records;
                    if (this.mBlacklist.isBlacklisted(receiver2.mIdentity.mPackageName)) {
                        deadUpdateRecords = deadUpdateRecords2;
                        deadReceivers = deadReceivers2;
                    } else {
                        deadReceivers = deadReceivers2;
                        deadUpdateRecords = deadUpdateRecords2;
                        StringBuilder stringBuilder4;
                        if (!reportLocationAccessNoThrow(receiver2.mIdentity.mPid, receiver2.mIdentity.mUid, receiver2.mIdentity.mPackageName, receiver2.mAllowedResolutionLevel)) {
                            str2 = TAG;
                            stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("skipping loc update for no op app: ");
                            stringBuilder4.append(receiver2.mIdentity.mPackageName);
                            Log.i(str2, stringBuilder4.toString());
                        } else if (this.mHwQuickTTFFMonitor.isLocationReportToApp(receiver2.mIdentity.mPackageName, provider, location2)) {
                            Location notifyLocation;
                            Bundle extras2;
                            if (receiver2.mAllowedResolutionLevel < 2) {
                                notifyLocation = coarseLocation;
                            } else {
                                notifyLocation = location3;
                            }
                            if (notifyLocation != null) {
                                noGPSLocation = r.mLastFixBroadcast;
                                if (noGPSLocation == null || shouldBroadcastSafe(notifyLocation, noGPSLocation, r, now)) {
                                    if (noGPSLocation == null) {
                                        noGPSLocation = new Location(notifyLocation);
                                        r.mLastFixBroadcast = noGPSLocation;
                                    } else {
                                        noGPSLocation.set(notifyLocation);
                                    }
                                    lastLocationCoarseInterval2.add(Integer.valueOf(System.identityHashCode(receiver2)));
                                    if (receiver2.callLocationChangedLocked(notifyLocation)) {
                                    } else {
                                        String str3 = TAG;
                                        stringBuilder4 = new StringBuilder();
                                        stringBuilder4.append("RemoteException calling onLocationChanged on ");
                                        stringBuilder4.append(receiver2);
                                        Slog.w(str3, stringBuilder4.toString());
                                        receiverDead = true;
                                    }
                                    r.mRealRequest.decrementNumUpdates();
                                    printFormatLog(receiver2.mIdentity.mPackageName, receiver2.mIdentity.mPid, "handleLocationChangedLocked", "report_location");
                                } else {
                                    Location location6 = notifyLocation;
                                }
                            }
                            long prevStatusUpdateTime = r.mLastStatusBroadcast;
                            if (nowNanos > prevStatusUpdateTime) {
                                if (prevStatusUpdateTime == 0 && lastLocation == 2) {
                                    extras2 = extras;
                                    if (r.mRealRequest.getNumUpdates() > null) {
                                    }
                                    if (deadUpdateRecords == null) {
                                    }
                                    deadUpdateRecords2.add(r);
                                    if (receiverDead) {
                                    }
                                    receiver = it3;
                                    noGPSLocation = location5;
                                    records = arrayList;
                                    extras = extras2;
                                } else {
                                    r.mLastStatusBroadcast = nowNanos;
                                    if (!receiver2.callStatusChangedLocked(provider, lastLocation, extras)) {
                                        receiverDead = true;
                                        str2 = TAG;
                                        StringBuilder stringBuilder5 = new StringBuilder();
                                        extras2 = extras;
                                        stringBuilder5.append("RemoteException calling onStatusChanged on ");
                                        stringBuilder5.append(receiver2);
                                        Slog.w(str2, stringBuilder5.toString());
                                        if (r.mRealRequest.getNumUpdates() > null || r.mRealRequest.getExpireAt() < now) {
                                            if (deadUpdateRecords == null) {
                                                deadUpdateRecords2 = new ArrayList();
                                            } else {
                                                deadUpdateRecords2 = deadUpdateRecords;
                                            }
                                            deadUpdateRecords2.add(r);
                                        } else {
                                            deadUpdateRecords2 = deadUpdateRecords;
                                        }
                                        if (receiverDead) {
                                            if (deadReceivers == null) {
                                                extras = new ArrayList();
                                            } else {
                                                extras = deadReceivers;
                                            }
                                            if (!extras.contains(receiver2)) {
                                                extras.add(receiver2);
                                            }
                                            deadReceivers2 = extras;
                                        } else {
                                            deadReceivers2 = deadReceivers;
                                        }
                                        receiver = it3;
                                        noGPSLocation = location5;
                                        records = arrayList;
                                        extras = extras2;
                                    }
                                }
                            }
                            extras2 = extras;
                            if (r.mRealRequest.getNumUpdates() > null) {
                            }
                            if (deadUpdateRecords == null) {
                            }
                            deadUpdateRecords2.add(r);
                            if (receiverDead) {
                            }
                            receiver = it3;
                            noGPSLocation = location5;
                            records = arrayList;
                            extras = extras2;
                        } else {
                            str2 = TAG;
                            stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("skipping qiuckttff loc update for  app: ");
                            stringBuilder4.append(receiver2.mIdentity.mPackageName);
                            Log.i(str2, stringBuilder4.toString());
                        }
                    }
                    receiver = it3;
                    noGPSLocation = location5;
                    records = arrayList;
                    deadReceivers2 = deadReceivers;
                    deadUpdateRecords2 = deadUpdateRecords;
                }
                deadUpdateRecords = deadUpdateRecords2;
                deadReceivers = deadReceivers2;
                location5 = noGPSLocation;
                arrayList = records;
                try {
                    Log.d(TAG, "start deliver location");
                    Bundle locationBundle = new Bundle();
                    locationBundle.putParcelable("location", new Location(location2));
                    locationBundle.putLong(WatchlistEventKeys.TIMESTAMP, System.currentTimeMillis());
                    locationBundle.putIntegerArrayList("receivers", lastLocationCoarseInterval2);
                    this.mHwLbsLogger.loggerEvent(111, locationBundle);
                } catch (Exception e) {
                    Log.d(TAG, "exception occured when deliver location");
                }
                if (deadReceivers != null) {
                    it = deadReceivers.iterator();
                    while (it.hasNext()) {
                        removeUpdatesLocked((Receiver) it.next());
                    }
                }
                if (deadUpdateRecords != null) {
                    it = deadUpdateRecords.iterator();
                    while (it.hasNext()) {
                        ((UpdateRecord) it.next()).disposeLocked(1);
                    }
                    applyRequirementsLocked(provider);
                } else {
                    deadUpdateRecords2 = deadUpdateRecords;
                }
            }
        }
    }

    private void updateLastLocationLocked(Location location, String provider) {
        Location noGPSLocation = location.getExtraLocation("noGPSLocation");
        Location lastLocation = (Location) this.mLastLocation.get(provider);
        if (lastLocation == null) {
            lastLocation = new Location(provider);
            this.mLastLocation.put(provider, lastLocation);
        } else {
            Location lastNoGPSLocation = lastLocation.getExtraLocation("noGPSLocation");
            if (noGPSLocation == null && lastNoGPSLocation != null) {
                location.setExtraLocation("noGPSLocation", lastNoGPSLocation);
            }
        }
        lastLocation.set(location);
    }

    private boolean isMockProvider(String provider) {
        boolean containsKey;
        synchronized (this.mLock) {
            containsKey = this.mMockProviders.containsKey(provider);
        }
        return containsKey;
    }

    /* JADX WARNING: Missing block: B:22:0x0046, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void handleLocationChanged(Location location, boolean passive) {
        Location myLocation = new Location(location);
        String provider = myLocation.getProvider();
        if (!myLocation.isFromMockProvider() && isMockProvider(provider)) {
            myLocation.setIsFromMockProvider(true);
        }
        synchronized (this.mLock) {
            if (isAllowedByCurrentUserSettingsLocked(provider)) {
                if (!passive) {
                    location = screenLocationLocked(location, provider);
                    if (location == null) {
                        return;
                    } else if (!(HwDeviceManager.disallowOp(101) || this.mHwQuickTTFFMonitor.isQuickLocation(location))) {
                        this.mPassiveProvider.updateLocation(myLocation);
                    }
                }
                handleLocationChangedLocked(myLocation, passive);
            }
        }
    }

    public boolean geocoderIsPresent() {
        return this.mGeocodeProvider != null;
    }

    public String getFromLocation(double latitude, double longitude, int maxResults, GeocoderParams params, List<Address> addrs) {
        if (this.mGeocodeProvider != null) {
            return this.mGeocodeProvider.getFromLocation(latitude, longitude, maxResults, params, addrs);
        }
        Log.i(TAG, "mGeocodeProvider is null");
        return null;
    }

    public String getFromLocationName(String locationName, double lowerLeftLatitude, double lowerLeftLongitude, double upperRightLatitude, double upperRightLongitude, int maxResults, GeocoderParams params, List<Address> addrs) {
        if (this.mGeocodeProvider != null) {
            return this.mGeocodeProvider.getFromLocationName(locationName, lowerLeftLatitude, lowerLeftLongitude, upperRightLatitude, upperRightLongitude, maxResults, params, addrs);
        }
        Log.i(TAG, "mGeocodeProvider is null");
        return null;
    }

    private boolean canCallerAccessMockLocation(String opPackageName) {
        return this.mAppOps.noteOp(58, Binder.getCallingUid(), opPackageName) == 0;
    }

    public void addTestProvider(String name, ProviderProperties properties, String opPackageName) {
        if (!canCallerAccessMockLocation(opPackageName)) {
            return;
        }
        if ("passive".equals(name)) {
            throw new IllegalArgumentException("Cannot mock the passive location provider");
        }
        long identity = Binder.clearCallingIdentity();
        synchronized (this.mLock) {
            if ("gps".equals(name) || "network".equals(name) || IHwLocalLocationProvider.LOCAL_PROVIDER.equals(name) || "fused".equals(name)) {
                LocationProviderInterface p = (LocationProviderInterface) this.mProvidersByName.get(name);
                if (p != null) {
                    removeProviderLocked(p);
                }
            }
            setGeoFencerEnabled(false);
            addTestProviderLocked(name, properties);
            updateProvidersLocked();
        }
        Binder.restoreCallingIdentity(identity);
    }

    private void addTestProviderLocked(String name, ProviderProperties properties) {
        if (this.mProvidersByName.get(name) == null) {
            MockProvider provider = new MockProvider(name, this, properties);
            addProviderLocked(provider);
            this.mMockProviders.put(name, provider);
            this.mLastLocation.put(name, null);
            this.mLastLocationCoarseInterval.put(name, null);
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Provider \"");
        stringBuilder.append(name);
        stringBuilder.append("\" already exists");
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public void removeTestProvider(String provider, String opPackageName) {
        if (canCallerAccessMockLocation(opPackageName)) {
            synchronized (this.mLock) {
                clearTestProviderEnabled(provider, opPackageName);
                clearTestProviderLocation(provider, opPackageName);
                clearTestProviderStatus(provider, opPackageName);
                if (((MockProvider) this.mMockProviders.remove(provider)) != null) {
                    long identity = Binder.clearCallingIdentity();
                    removeProviderLocked((LocationProviderInterface) this.mProvidersByName.get(provider));
                    setGeoFencerEnabled(true);
                    LocationProviderInterface realProvider = (LocationProviderInterface) this.mRealProviders.get(provider);
                    if (realProvider != null) {
                        addProviderLocked(realProvider);
                    }
                    this.mLastLocation.put(provider, null);
                    this.mLastLocationCoarseInterval.put(provider, null);
                    updateProvidersLocked();
                    Binder.restoreCallingIdentity(identity);
                } else {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Provider \"");
                    stringBuilder.append(provider);
                    stringBuilder.append("\" unknown");
                    throw new IllegalArgumentException(stringBuilder.toString());
                }
            }
        }
    }

    public void setTestProviderLocation(String provider, Location loc, String opPackageName) {
        if (canCallerAccessMockLocation(opPackageName)) {
            synchronized (this.mLock) {
                MockProvider mockProvider = (MockProvider) this.mMockProviders.get(provider);
                if (mockProvider != null) {
                    Location mock = new Location(loc);
                    mock.setIsFromMockProvider(true);
                    if (!(TextUtils.isEmpty(loc.getProvider()) || provider.equals(loc.getProvider()))) {
                        Object[] objArr = new Object[3];
                        objArr[0] = "33091107";
                        objArr[1] = Integer.valueOf(Binder.getCallingUid());
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append(provider);
                        stringBuilder.append("!=");
                        stringBuilder.append(loc.getProvider());
                        objArr[2] = stringBuilder.toString();
                        EventLog.writeEvent(1397638484, objArr);
                    }
                    long identity = Binder.clearCallingIdentity();
                    mockProvider.setLocation(mock);
                    Binder.restoreCallingIdentity(identity);
                } else {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Provider \"");
                    stringBuilder2.append(provider);
                    stringBuilder2.append("\" unknown");
                    throw new IllegalArgumentException(stringBuilder2.toString());
                }
            }
        }
    }

    public void clearTestProviderLocation(String provider, String opPackageName) {
        if (canCallerAccessMockLocation(opPackageName)) {
            synchronized (this.mLock) {
                MockProvider mockProvider = (MockProvider) this.mMockProviders.get(provider);
                if (mockProvider != null) {
                    mockProvider.clearLocation();
                } else {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Provider \"");
                    stringBuilder.append(provider);
                    stringBuilder.append("\" unknown");
                    throw new IllegalArgumentException(stringBuilder.toString());
                }
            }
        }
    }

    public void setTestProviderEnabled(String provider, boolean enabled, String opPackageName) {
        if (canCallerAccessMockLocation(opPackageName)) {
            setTestProviderEnabled(provider, enabled);
        }
    }

    private void setTestProviderEnabled(String provider, boolean enabled) {
        synchronized (this.mLock) {
            MockProvider mockProvider = (MockProvider) this.mMockProviders.get(provider);
            if (mockProvider != null) {
                long identity = Binder.clearCallingIdentity();
                if (enabled) {
                    mockProvider.enable();
                    this.mEnabledProviders.add(provider);
                    this.mDisabledProviders.remove(provider);
                } else {
                    mockProvider.disable();
                    this.mEnabledProviders.remove(provider);
                    this.mDisabledProviders.add(provider);
                }
                updateProvidersLocked();
                Binder.restoreCallingIdentity(identity);
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Provider \"");
                stringBuilder.append(provider);
                stringBuilder.append("\" unknown");
                throw new IllegalArgumentException(stringBuilder.toString());
            }
        }
    }

    public void clearTestProviderEnabled(String provider, String opPackageName) {
        if (canCallerAccessMockLocation(opPackageName)) {
            synchronized (this.mLock) {
                if (((MockProvider) this.mMockProviders.get(provider)) != null) {
                    long identity = Binder.clearCallingIdentity();
                    this.mEnabledProviders.remove(provider);
                    this.mDisabledProviders.remove(provider);
                    updateProvidersLocked();
                    Binder.restoreCallingIdentity(identity);
                } else {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Provider \"");
                    stringBuilder.append(provider);
                    stringBuilder.append("\" unknown");
                    throw new IllegalArgumentException(stringBuilder.toString());
                }
            }
        }
    }

    public void setTestProviderStatus(String provider, int status, Bundle extras, long updateTime, String opPackageName) {
        if (canCallerAccessMockLocation(opPackageName)) {
            synchronized (this.mLock) {
                MockProvider mockProvider = (MockProvider) this.mMockProviders.get(provider);
                if (mockProvider != null) {
                    mockProvider.setStatus(status, extras, updateTime);
                } else {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Provider \"");
                    stringBuilder.append(provider);
                    stringBuilder.append("\" unknown");
                    throw new IllegalArgumentException(stringBuilder.toString());
                }
            }
        }
    }

    public void clearTestProviderStatus(String provider, String opPackageName) {
        if (canCallerAccessMockLocation(opPackageName)) {
            synchronized (this.mLock) {
                MockProvider mockProvider = (MockProvider) this.mMockProviders.get(provider);
                if (mockProvider != null) {
                    mockProvider.clearStatus();
                } else {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Provider \"");
                    stringBuilder.append(provider);
                    stringBuilder.append("\" unknown");
                    throw new IllegalArgumentException(stringBuilder.toString());
                }
            }
        }
    }

    private void log(String log) {
        if (Log.isLoggable(TAG, 2)) {
            Slog.d(TAG, log);
        }
    }

    /* JADX WARNING: Missing block: B:14:0x002a, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, pw)) {
            synchronized (this.mLock) {
                if (args.length <= 0 || !args[0].equals("--gnssmetrics")) {
                    StringBuilder stringBuilder;
                    StringBuilder stringBuilder2;
                    String provider;
                    Location location;
                    String i;
                    pw.println("Current Location Manager state:");
                    pw.println("  Location Listeners:");
                    for (Receiver receiver : this.mReceivers.values()) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("    ");
                        stringBuilder.append(receiver);
                        pw.println(stringBuilder.toString());
                    }
                    pw.println("  Active Records by Provider:");
                    for (Entry<String, ArrayList<UpdateRecord>> entry : this.mRecordsByProvider.entrySet()) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("    ");
                        stringBuilder.append((String) entry.getKey());
                        stringBuilder.append(":");
                        pw.println(stringBuilder.toString());
                        Iterator it = ((ArrayList) entry.getValue()).iterator();
                        while (it.hasNext()) {
                            UpdateRecord record = (UpdateRecord) it.next();
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("      ");
                            stringBuilder2.append(record);
                            pw.println(stringBuilder2.toString());
                        }
                    }
                    pw.println("  Active GnssMeasurement Listeners:");
                    for (Identity identity : this.mGnssMeasurementsListeners.values()) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("    ");
                        stringBuilder.append(identity.mPid);
                        stringBuilder.append(" ");
                        stringBuilder.append(identity.mUid);
                        stringBuilder.append(" ");
                        stringBuilder.append(identity.mPackageName);
                        stringBuilder.append(": ");
                        stringBuilder.append(isThrottlingExemptLocked(identity));
                        pw.println(stringBuilder.toString());
                    }
                    pw.println("  Active GnssNavigationMessage Listeners:");
                    for (Identity identity2 : this.mGnssNavigationMessageListeners.values()) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("    ");
                        stringBuilder.append(identity2.mPid);
                        stringBuilder.append(" ");
                        stringBuilder.append(identity2.mUid);
                        stringBuilder.append(" ");
                        stringBuilder.append(identity2.mPackageName);
                        stringBuilder.append(": ");
                        stringBuilder.append(isThrottlingExemptLocked(identity2));
                        pw.println(stringBuilder.toString());
                    }
                    pw.println("  Overlay Provider Packages:");
                    Iterator it2 = this.mProviders.iterator();
                    while (it2.hasNext()) {
                        LocationProviderInterface provider2 = (LocationProviderInterface) it2.next();
                        if (provider2 instanceof LocationProviderProxy) {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("    ");
                            stringBuilder.append(provider2.getName());
                            stringBuilder.append(": ");
                            stringBuilder.append(((LocationProviderProxy) provider2).getConnectedPackageName());
                            pw.println(stringBuilder.toString());
                        }
                    }
                    pw.println("  Historical Records by Provider:");
                    for (Entry<PackageProviderKey, PackageStatistics> entry2 : this.mRequestStatistics.statistics.entrySet()) {
                        PackageProviderKey key = (PackageProviderKey) entry2.getKey();
                        PackageStatistics stats = (PackageStatistics) entry2.getValue();
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("    ");
                        stringBuilder2.append(key.packageName);
                        stringBuilder2.append(": ");
                        stringBuilder2.append(key.providerName);
                        stringBuilder2.append(": ");
                        stringBuilder2.append(stats);
                        pw.println(stringBuilder2.toString());
                    }
                    pw.println("  Last Known Locations:");
                    for (Entry<String, Location> entry3 : this.mLastLocation.entrySet()) {
                        provider = (String) entry3.getKey();
                        location = (Location) entry3.getValue();
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("    ");
                        stringBuilder2.append(provider);
                        stringBuilder2.append(": ");
                        stringBuilder2.append(location);
                        pw.println(stringBuilder2.toString());
                    }
                    pw.println("  Last Known Locations Coarse Intervals:");
                    for (Entry<String, Location> entry32 : this.mLastLocationCoarseInterval.entrySet()) {
                        provider = (String) entry32.getKey();
                        location = (Location) entry32.getValue();
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("    ");
                        stringBuilder2.append(provider);
                        stringBuilder2.append(": ");
                        stringBuilder2.append(location);
                        pw.println(stringBuilder2.toString());
                    }
                    this.mGeofenceManager.dump(pw);
                    if (this.mEnabledProviders.size() > 0) {
                        pw.println("  Enabled Providers:");
                        for (String i2 : this.mEnabledProviders) {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("    ");
                            stringBuilder.append(i2);
                            pw.println(stringBuilder.toString());
                        }
                    }
                    if (this.mDisabledProviders.size() > 0) {
                        pw.println("  Disabled Providers:");
                        for (String i22 : this.mDisabledProviders) {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("    ");
                            stringBuilder.append(i22);
                            pw.println(stringBuilder.toString());
                        }
                    }
                    pw.append("  ");
                    this.mBlacklist.dump(pw);
                    if (this.mMockProviders.size() > 0) {
                        pw.println("  Mock Providers:");
                        for (Entry<String, MockProvider> i3 : this.mMockProviders.entrySet()) {
                            ((MockProvider) i3.getValue()).dump(pw, "      ");
                        }
                    }
                    if (!this.mBackgroundThrottlePackageWhitelist.isEmpty()) {
                        pw.println("  Throttling Whitelisted Packages:");
                        it2 = this.mBackgroundThrottlePackageWhitelist.iterator();
                        while (it2.hasNext()) {
                            i22 = (String) it2.next();
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("    ");
                            stringBuilder.append(i22);
                            pw.println(stringBuilder.toString());
                        }
                    }
                    pw.append("  fudger: ");
                    this.mLocationFudger.dump(fd, pw, args);
                    if (args.length <= 0 || !"short".equals(args[0])) {
                        it2 = this.mProviders.iterator();
                        while (it2.hasNext()) {
                            LocationProviderInterface provider3 = (LocationProviderInterface) it2.next();
                            StringBuilder stringBuilder3 = new StringBuilder();
                            stringBuilder3.append(provider3.getName());
                            stringBuilder3.append(" Internal State");
                            pw.print(stringBuilder3.toString());
                            if (provider3 instanceof LocationProviderProxy) {
                                LocationProviderProxy proxy = (LocationProviderProxy) provider3;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append(" (");
                                stringBuilder.append(proxy.getConnectedPackageName());
                                stringBuilder.append(")");
                                pw.print(stringBuilder.toString());
                            }
                            pw.println(":");
                            provider3.dump(fd, pw, args);
                        }
                        if (this.mGnssBatchingInProgress) {
                            pw.println("  GNSS batching in progress");
                        }
                        hwLocationPowerTrackerDump(pw);
                        dumpGpsFreezeProxy(pw);
                        return;
                    }
                } else if (this.mGnssMetricsProvider != null) {
                    pw.append(this.mGnssMetricsProvider.getGnssMetricsAsProtoString());
                }
            }
        }
    }

    private String getCallingAppName(String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            return null;
        }
        ApplicationInfo appInfo = null;
        try {
            appInfo = this.mPackageManager.getApplicationInfo(packageName, 0);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        if (appInfo == null) {
            return packageName;
        }
        return (String) this.mPackageManager.getApplicationLabel(appInfo);
    }

    private String getPackageNameByPid(int pid) {
        if (pid <= 0) {
            return null;
        }
        ActivityManager activityManager = (ActivityManager) this.mContext.getSystemService("activity");
        if (activityManager == null) {
            return null;
        }
        List<RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        if (appProcesses == null) {
            return null;
        }
        String packageName = null;
        for (RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.pid == pid) {
                packageName = appProcess.processName;
                break;
            }
        }
        int indexProcessFlag = -1;
        if (packageName != null) {
            indexProcessFlag = packageName.indexOf(58);
        }
        return indexProcessFlag > 0 ? packageName.substring(0, indexProcessFlag) : packageName;
    }

    private void printFormatLog(String packageName, int pid, String callingMethodName, String tag) {
        String ctaTag = "ctaifs";
        if (!TextUtils.isEmpty(packageName)) {
            StringBuilder stringBuilder;
            String applicationInfo = new StringBuilder();
            applicationInfo.append("<");
            applicationInfo.append(getCallingAppName(packageName));
            applicationInfo.append(">[");
            applicationInfo.append(getCallingAppName(packageName));
            applicationInfo.append("][");
            applicationInfo.append(getPackageNameByPid(pid));
            applicationInfo.append("]:[");
            applicationInfo.append(callingMethodName);
            applicationInfo.append("]");
            applicationInfo = applicationInfo.toString();
            if (tag.equals("gps")) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(applicationInfo);
                stringBuilder.append(" 定位..GPS定位");
                Log.i(ctaTag, stringBuilder.toString());
            }
            if (tag.equals("network")) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(applicationInfo);
                stringBuilder.append(" 定位..Wifi/基站定位");
                Log.i(ctaTag, stringBuilder.toString());
            }
            if (tag.equals("report_location")) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(applicationInfo);
                stringBuilder.append(" 读取定位信息");
                Log.i(ctaTag, stringBuilder.toString());
            }
        }
    }
}
