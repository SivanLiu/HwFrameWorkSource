package com.android.server;

import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.app.ActivityManagerInternal.ScreenObserver;
import android.app.AlarmManager;
import android.app.AlarmManager.OnAlarmListener;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.ContentObserver;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.TriggerEvent;
import android.hardware.TriggerEventListener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationRequest;
import android.net.INetworkPolicyManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileUtils;
import android.os.Handler;
import android.os.IDeviceIdleController.Stub;
import android.os.IMaintenanceActivityListener;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.PowerManagerInternal;
import android.os.Process;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.ShellCallback;
import android.os.ShellCommand;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings.Global;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.KeyValueListParser;
import android.util.Log;
import android.util.MutableLong;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.TimeUtils;
import android.util.Xml;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.app.IBatteryStats;
import com.android.internal.os.AtomicFile;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.XmlUtils;
import com.android.server.am.BatteryStatsService;
import com.android.server.backup.BackupAgentTimeoutParameters;
import com.android.server.job.controllers.JobStatus;
import com.android.server.net.NetworkPolicyManagerInternal;
import com.android.server.pm.DumpState;
import com.android.server.pm.PackageManagerService;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class DeviceIdleController extends SystemService implements DeviceIdleCallback {
    private static final boolean COMPRESS_TIME = false;
    private static boolean DEBUG = false;
    private static final int EVENT_BUFFER_SIZE = 100;
    private static final int EVENT_DEEP_IDLE = 4;
    private static final int EVENT_DEEP_MAINTENANCE = 5;
    private static final int EVENT_LIGHT_IDLE = 2;
    private static final int EVENT_LIGHT_MAINTENANCE = 3;
    private static final int EVENT_NORMAL = 1;
    private static final int EVENT_NULL = 0;
    private static final int LIGHT_STATE_ACTIVE = 0;
    private static final int LIGHT_STATE_IDLE = 4;
    private static final int LIGHT_STATE_IDLE_MAINTENANCE = 6;
    private static final int LIGHT_STATE_INACTIVE = 1;
    private static final int LIGHT_STATE_OVERRIDE = 7;
    private static final int LIGHT_STATE_PRE_IDLE = 3;
    private static final int LIGHT_STATE_WAITING_FOR_NETWORK = 5;
    private static final int MSG_FINISH_IDLE_OP = 8;
    private static final int MSG_REPORT_ACTIVE = 5;
    private static final int MSG_REPORT_IDLE_OFF = 4;
    private static final int MSG_REPORT_IDLE_ON = 2;
    private static final int MSG_REPORT_IDLE_ON_LIGHT = 3;
    private static final int MSG_REPORT_MAINTENANCE_ACTIVITY = 7;
    private static final int MSG_REPORT_TEMP_APP_WHITELIST_CHANGED = 9;
    private static final int MSG_TEMP_APP_WHITELIST_TIMEOUT = 6;
    private static final int MSG_WRITE_CONFIG = 1;
    private static final int READ_DB_DELAY_TIME = 10000;
    private static final int STATE_ACTIVE = 0;
    private static final int STATE_IDLE = 5;
    private static final int STATE_IDLE_MAINTENANCE = 6;
    private static final int STATE_IDLE_PENDING = 2;
    private static final int STATE_INACTIVE = 1;
    private static final int STATE_LOCATING = 4;
    private static final int STATE_SENSING = 3;
    private static final String TAG = "DeviceIdleController";
    private int mActiveIdleOpCount;
    private WakeLock mActiveIdleWakeLock;
    private AlarmManager mAlarmManager;
    private boolean mAlarmsActive;
    private AnyMotionDetector mAnyMotionDetector;
    private final AppStateTracker mAppStateTracker;
    private IBatteryStats mBatteryStats;
    BinderService mBinderService;
    private boolean mCharging;
    public final AtomicFile mConfigFile = new AtomicFile(new File(getSystemDir(), "deviceidle.xml"));
    private ConnectivityService mConnectivityService;
    private Constants mConstants;
    private long mCurIdleBudget;
    private final OnAlarmListener mDeepAlarmListener = new OnAlarmListener() {
        public void onAlarm() {
            synchronized (DeviceIdleController.this) {
                DeviceIdleController.this.stepIdleStateLocked("s:alarm");
            }
        }
    };
    private boolean mDeepEnabled;
    private final int[] mEventCmds = new int[100];
    private final String[] mEventReasons = new String[100];
    private final long[] mEventTimes = new long[100];
    private boolean mForceIdle;
    private final LocationListener mGenericLocationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            synchronized (DeviceIdleController.this) {
                DeviceIdleController.this.receivedGenericLocationLocked(location);
            }
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        public void onProviderEnabled(String provider) {
        }

        public void onProviderDisabled(String provider) {
        }
    };
    private WakeLock mGoingIdleWakeLock;
    private final LocationListener mGpsLocationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            synchronized (DeviceIdleController.this) {
                DeviceIdleController.this.receivedGpsLocationLocked(location);
            }
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        public void onProviderEnabled(String provider) {
        }

        public void onProviderDisabled(String provider) {
        }
    };
    final MyHandler mHandler = new MyHandler(BackgroundThread.getHandler().getLooper());
    private boolean mHasGps;
    private boolean mHasNetworkLocation;
    private Intent mIdleIntent;
    private final BroadcastReceiver mIdleStartedDoneReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if ("android.os.action.DEVICE_IDLE_MODE_CHANGED".equals(intent.getAction())) {
                DeviceIdleController.this.mHandler.sendEmptyMessageDelayed(8, DeviceIdleController.this.mConstants.MIN_DEEP_MAINTENANCE_TIME);
            } else {
                DeviceIdleController.this.mHandler.sendEmptyMessageDelayed(8, DeviceIdleController.this.mConstants.MIN_LIGHT_MAINTENANCE_TIME);
            }
        }
    };
    private long mInactiveTimeout;
    private final BroadcastReceiver mInteractivityReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            synchronized (DeviceIdleController.this) {
                DeviceIdleController.this.updateInteractivityLocked();
            }
        }
    };
    private boolean mJobsActive;
    private Location mLastGenericLocation;
    private Location mLastGpsLocation;
    private final OnAlarmListener mLightAlarmListener = new OnAlarmListener() {
        public void onAlarm() {
            synchronized (DeviceIdleController.this) {
                DeviceIdleController.this.stepLightIdleStateLocked("s:alarm");
            }
        }
    };
    private boolean mLightEnabled;
    private Intent mLightIdleIntent;
    private int mLightState;
    private ActivityManagerInternal mLocalActivityManager;
    private PowerManagerInternal mLocalPowerManager;
    private boolean mLocated;
    private boolean mLocating;
    private LocationManager mLocationManager;
    private LocationRequest mLocationRequest;
    private final RemoteCallbackList<IMaintenanceActivityListener> mMaintenanceActivityListeners = new RemoteCallbackList();
    private long mMaintenanceStartTime;
    private final MotionListener mMotionListener = new MotionListener(this, null);
    private Sensor mMotionSensor;
    private boolean mNetworkConnected;
    private INetworkPolicyManager mNetworkPolicyManager;
    private NetworkPolicyManagerInternal mNetworkPolicyManagerInternal;
    private long mNextAlarmTime;
    private long mNextIdleDelay;
    private long mNextIdlePendingDelay;
    private long mNextLightAlarmTime;
    private long mNextLightIdleDelay;
    private long mNextSensingTimeoutAlarmTime;
    private boolean mNotMoving;
    private PowerManager mPowerManager;
    private int[] mPowerSaveWhitelistAllAppIdArray = new int[0];
    private final SparseBooleanArray mPowerSaveWhitelistAllAppIds = new SparseBooleanArray();
    private final ArrayMap<String, Integer> mPowerSaveWhitelistApps = new ArrayMap();
    private final ArrayMap<String, Integer> mPowerSaveWhitelistAppsExceptIdle = new ArrayMap();
    private int[] mPowerSaveWhitelistExceptIdleAppIdArray = new int[0];
    private final SparseBooleanArray mPowerSaveWhitelistExceptIdleAppIds = new SparseBooleanArray();
    private final SparseBooleanArray mPowerSaveWhitelistSystemAppIds = new SparseBooleanArray();
    private final SparseBooleanArray mPowerSaveWhitelistSystemAppIdsExceptIdle = new SparseBooleanArray();
    private int[] mPowerSaveWhitelistUserAppIdArray = new int[0];
    private final SparseBooleanArray mPowerSaveWhitelistUserAppIds = new SparseBooleanArray();
    private final ArrayMap<String, Integer> mPowerSaveWhitelistUserApps = new ArrayMap();
    private final ArraySet<String> mPowerSaveWhitelistUserAppsExceptIdle = new ArraySet();
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        /* JADX WARNING: Removed duplicated region for block: B:44:? A:{SYNTHETIC, RETURN} */
        /* JADX WARNING: Removed duplicated region for block: B:37:0x0070  */
        /* JADX WARNING: Removed duplicated region for block: B:24:0x0058  */
        /* JADX WARNING: Removed duplicated region for block: B:17:0x003d  */
        /* JADX WARNING: Removed duplicated region for block: B:44:? A:{SYNTHETIC, RETURN} */
        /* JADX WARNING: Removed duplicated region for block: B:37:0x0070  */
        /* JADX WARNING: Removed duplicated region for block: B:24:0x0058  */
        /* JADX WARNING: Removed duplicated region for block: B:17:0x003d  */
        /* JADX WARNING: Removed duplicated region for block: B:44:? A:{SYNTHETIC, RETURN} */
        /* JADX WARNING: Removed duplicated region for block: B:37:0x0070  */
        /* JADX WARNING: Removed duplicated region for block: B:24:0x0058  */
        /* JADX WARNING: Removed duplicated region for block: B:17:0x003d  */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onReceive(Context context, Intent intent) {
            boolean z;
            String action = intent.getAction();
            int hashCode = action.hashCode();
            boolean z2 = true;
            if (hashCode == -1538406691) {
                if (action.equals("android.intent.action.BATTERY_CHANGED")) {
                    z = true;
                    switch (z) {
                        case false:
                            break;
                        case true:
                            break;
                        case true:
                            break;
                        default:
                            break;
                    }
                }
            } else if (hashCode == -1172645946) {
                if (action.equals("android.net.conn.CONNECTIVITY_CHANGE")) {
                    z = false;
                    switch (z) {
                        case false:
                            break;
                        case true:
                            break;
                        case true:
                            break;
                        default:
                            break;
                    }
                }
            } else if (hashCode == 525384130 && action.equals("android.intent.action.PACKAGE_REMOVED")) {
                z = true;
                switch (z) {
                    case false:
                        DeviceIdleController.this.updateConnectivityState(intent);
                        return;
                    case true:
                        synchronized (DeviceIdleController.this) {
                            hashCode = intent.getIntExtra("plugged", 0);
                            DeviceIdleController deviceIdleController = DeviceIdleController.this;
                            if (hashCode == 0) {
                                z2 = false;
                            }
                            deviceIdleController.updateChargingLocked(z2);
                        }
                        return;
                    case true:
                        if (!intent.getBooleanExtra("android.intent.extra.REPLACING", false)) {
                            Uri data = intent.getData();
                            if (data != null) {
                                String schemeSpecificPart = data.getSchemeSpecificPart();
                                String ssp = schemeSpecificPart;
                                if (schemeSpecificPart != null) {
                                    DeviceIdleController.this.removePowerSaveWhitelistAppInternal(ssp);
                                    return;
                                }
                                return;
                            }
                            return;
                        }
                        return;
                    default:
                        return;
                }
            }
            z = true;
            switch (z) {
                case false:
                    break;
                case true:
                    break;
                case true:
                    break;
                default:
                    break;
            }
        }
    };
    private ArrayMap<String, Integer> mRemovedFromSystemWhitelistApps = new ArrayMap();
    private boolean mReportedMaintenanceActivity;
    private boolean mScreenLocked;
    private ScreenObserver mScreenObserver = new ScreenObserver() {
        public void onAwakeStateChanged(boolean isAwake) {
        }

        public void onKeyguardStateChanged(boolean isShowing) {
            synchronized (DeviceIdleController.this) {
                DeviceIdleController.this.keyguardShowingLocked(isShowing);
            }
        }
    };
    private boolean mScreenOn;
    private final OnAlarmListener mSensingTimeoutAlarmListener = new OnAlarmListener() {
        public void onAlarm() {
            if (DeviceIdleController.this.mState == 3) {
                synchronized (DeviceIdleController.this) {
                    DeviceIdleController.this.becomeInactiveIfAppropriateLocked();
                }
            }
        }
    };
    private SensorManager mSensorManager;
    private int mState;
    private int[] mTempWhitelistAppIdArray = new int[0];
    private final SparseArray<Pair<MutableLong, String>> mTempWhitelistAppIdEndTimes = new SparseArray();

    private final class BinderService extends Stub {
        private BinderService() {
        }

        /* synthetic */ BinderService(DeviceIdleController x0, AnonymousClass1 x1) {
            this();
        }

        public void addPowerSaveWhitelistApp(String name) {
            if (DeviceIdleController.DEBUG) {
                String str = DeviceIdleController.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("addPowerSaveWhitelistApp(name = ");
                stringBuilder.append(name);
                stringBuilder.append(")");
                Slog.i(str, stringBuilder.toString());
            }
            DeviceIdleController.this.getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            long ident = Binder.clearCallingIdentity();
            try {
                DeviceIdleController.this.addPowerSaveWhitelistAppInternal(name);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void removePowerSaveWhitelistApp(String name) {
            if (DeviceIdleController.DEBUG) {
                String str = DeviceIdleController.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("removePowerSaveWhitelistApp(name = ");
                stringBuilder.append(name);
                stringBuilder.append(")");
                Slog.i(str, stringBuilder.toString());
            }
            DeviceIdleController.this.getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            long ident = Binder.clearCallingIdentity();
            try {
                DeviceIdleController.this.removePowerSaveWhitelistAppInternal(name);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void removeSystemPowerWhitelistApp(String name) {
            if (DeviceIdleController.DEBUG) {
                String str = DeviceIdleController.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("removeAppFromSystemWhitelist(name = ");
                stringBuilder.append(name);
                stringBuilder.append(")");
                Slog.d(str, stringBuilder.toString());
            }
            DeviceIdleController.this.getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            long ident = Binder.clearCallingIdentity();
            try {
                DeviceIdleController.this.removeSystemPowerWhitelistAppInternal(name);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void restoreSystemPowerWhitelistApp(String name) {
            if (DeviceIdleController.DEBUG) {
                String str = DeviceIdleController.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("restoreAppToSystemWhitelist(name = ");
                stringBuilder.append(name);
                stringBuilder.append(")");
                Slog.d(str, stringBuilder.toString());
            }
            DeviceIdleController.this.getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            long ident = Binder.clearCallingIdentity();
            try {
                DeviceIdleController.this.restoreSystemPowerWhitelistAppInternal(name);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public String[] getRemovedSystemPowerWhitelistApps() {
            return DeviceIdleController.this.getRemovedSystemPowerWhitelistAppsInternal();
        }

        public String[] getSystemPowerWhitelistExceptIdle() {
            return DeviceIdleController.this.getSystemPowerWhitelistExceptIdleInternal();
        }

        public String[] getSystemPowerWhitelist() {
            return DeviceIdleController.this.getSystemPowerWhitelistInternal();
        }

        public String[] getUserPowerWhitelist() {
            return DeviceIdleController.this.getUserPowerWhitelistInternal();
        }

        public String[] getFullPowerWhitelistExceptIdle() {
            return DeviceIdleController.this.getFullPowerWhitelistExceptIdleInternal();
        }

        public String[] getFullPowerWhitelist() {
            return DeviceIdleController.this.getFullPowerWhitelistInternal();
        }

        public int[] getAppIdWhitelistExceptIdle() {
            return DeviceIdleController.this.getAppIdWhitelistExceptIdleInternal();
        }

        public int[] getAppIdWhitelist() {
            return DeviceIdleController.this.getAppIdWhitelistInternal();
        }

        public int[] getAppIdUserWhitelist() {
            return DeviceIdleController.this.getAppIdUserWhitelistInternal();
        }

        public int[] getAppIdTempWhitelist() {
            return DeviceIdleController.this.getAppIdTempWhitelistInternal();
        }

        public boolean isPowerSaveWhitelistExceptIdleApp(String name) {
            return DeviceIdleController.this.isPowerSaveWhitelistExceptIdleAppInternal(name);
        }

        public int getIdleStateDetailed() {
            DeviceIdleController.this.getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            return DeviceIdleController.this.mState;
        }

        public int getLightIdleStateDetailed() {
            DeviceIdleController.this.getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            return DeviceIdleController.this.mLightState;
        }

        public boolean isPowerSaveWhitelistApp(String name) {
            return DeviceIdleController.this.isPowerSaveWhitelistAppInternal(name);
        }

        public void addPowerSaveTempWhitelistApp(String packageName, long duration, int userId, String reason) throws RemoteException {
            DeviceIdleController.this.addPowerSaveTempWhitelistAppChecked(packageName, duration, userId, reason);
        }

        public long addPowerSaveTempWhitelistAppForMms(String packageName, int userId, String reason) throws RemoteException {
            long duration = DeviceIdleController.this.mConstants.MMS_TEMP_APP_WHITELIST_DURATION;
            DeviceIdleController.this.addPowerSaveTempWhitelistAppChecked(packageName, duration, userId, reason);
            return duration;
        }

        public long addPowerSaveTempWhitelistAppForSms(String packageName, int userId, String reason) throws RemoteException {
            long duration = DeviceIdleController.this.mConstants.SMS_TEMP_APP_WHITELIST_DURATION;
            DeviceIdleController.this.addPowerSaveTempWhitelistAppChecked(packageName, duration, userId, reason);
            return duration;
        }

        public void exitIdle(String reason) {
            DeviceIdleController.this.getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            long ident = Binder.clearCallingIdentity();
            try {
                DeviceIdleController.this.exitIdleInternal(reason);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public boolean registerMaintenanceActivityListener(IMaintenanceActivityListener listener) {
            return DeviceIdleController.this.registerMaintenanceActivityListener(listener);
        }

        public void unregisterMaintenanceActivityListener(IMaintenanceActivityListener listener) {
            DeviceIdleController.this.unregisterMaintenanceActivityListener(listener);
        }

        protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            DeviceIdleController.this.dump(fd, pw, args);
        }

        public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
            new Shell().exec(this, in, out, err, args, callback, resultReceiver);
        }

        public int forceIdle() {
            if (1000 == Binder.getCallingUid()) {
                return DeviceIdleController.this.forceIdleInternal();
            }
            String str = DeviceIdleController.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(" forceIdle error , permission not allowed. uid = ");
            stringBuilder.append(Binder.getCallingUid());
            Slog.e(str, stringBuilder.toString());
            return -1;
        }
    }

    private final class Constants extends ContentObserver {
        private static final String KEY_IDLE_AFTER_INACTIVE_TIMEOUT = "idle_after_inactive_to";
        private static final String KEY_IDLE_FACTOR = "idle_factor";
        private static final String KEY_IDLE_PENDING_FACTOR = "idle_pending_factor";
        private static final String KEY_IDLE_PENDING_TIMEOUT = "idle_pending_to";
        private static final String KEY_IDLE_TIMEOUT = "idle_to";
        private static final String KEY_INACTIVE_TIMEOUT = "inactive_to";
        private static final String KEY_LIGHT_IDLE_AFTER_INACTIVE_TIMEOUT = "light_after_inactive_to";
        private static final String KEY_LIGHT_IDLE_FACTOR = "light_idle_factor";
        private static final String KEY_LIGHT_IDLE_MAINTENANCE_MAX_BUDGET = "light_idle_maintenance_max_budget";
        private static final String KEY_LIGHT_IDLE_MAINTENANCE_MIN_BUDGET = "light_idle_maintenance_min_budget";
        private static final String KEY_LIGHT_IDLE_TIMEOUT = "light_idle_to";
        private static final String KEY_LIGHT_MAX_IDLE_TIMEOUT = "light_max_idle_to";
        private static final String KEY_LIGHT_PRE_IDLE_TIMEOUT = "light_pre_idle_to";
        private static final String KEY_LOCATING_TIMEOUT = "locating_to";
        private static final String KEY_LOCATION_ACCURACY = "location_accuracy";
        private static final String KEY_MAX_IDLE_PENDING_TIMEOUT = "max_idle_pending_to";
        private static final String KEY_MAX_IDLE_TIMEOUT = "max_idle_to";
        private static final String KEY_MAX_TEMP_APP_WHITELIST_DURATION = "max_temp_app_whitelist_duration";
        private static final String KEY_MIN_DEEP_MAINTENANCE_TIME = "min_deep_maintenance_time";
        private static final String KEY_MIN_LIGHT_MAINTENANCE_TIME = "min_light_maintenance_time";
        private static final String KEY_MIN_TIME_TO_ALARM = "min_time_to_alarm";
        private static final String KEY_MMS_TEMP_APP_WHITELIST_DURATION = "mms_temp_app_whitelist_duration";
        private static final String KEY_MOTION_INACTIVE_TIMEOUT = "motion_inactive_to";
        private static final String KEY_NOTIFICATION_WHITELIST_DURATION = "notification_whitelist_duration";
        private static final String KEY_SENSING_TIMEOUT = "sensing_to";
        private static final String KEY_SMS_TEMP_APP_WHITELIST_DURATION = "sms_temp_app_whitelist_duration";
        private static final String KEY_WAIT_FOR_UNLOCK = "wait_for_unlock";
        public long IDLE_AFTER_INACTIVE_TIMEOUT;
        public float IDLE_FACTOR;
        public float IDLE_PENDING_FACTOR;
        public long IDLE_PENDING_TIMEOUT;
        public long IDLE_TIMEOUT;
        public long INACTIVE_TIMEOUT;
        public long LIGHT_IDLE_AFTER_INACTIVE_TIMEOUT;
        public float LIGHT_IDLE_FACTOR;
        public long LIGHT_IDLE_MAINTENANCE_MAX_BUDGET;
        public long LIGHT_IDLE_MAINTENANCE_MIN_BUDGET;
        public long LIGHT_IDLE_TIMEOUT;
        public long LIGHT_MAX_IDLE_TIMEOUT;
        public long LIGHT_PRE_IDLE_TIMEOUT;
        public long LOCATING_TIMEOUT;
        public float LOCATION_ACCURACY;
        public long MAX_IDLE_PENDING_TIMEOUT;
        public long MAX_IDLE_TIMEOUT;
        public long MAX_TEMP_APP_WHITELIST_DURATION;
        public long MIN_DEEP_MAINTENANCE_TIME;
        public long MIN_LIGHT_MAINTENANCE_TIME;
        public long MIN_TIME_TO_ALARM;
        public long MMS_TEMP_APP_WHITELIST_DURATION;
        public long MOTION_INACTIVE_TIMEOUT;
        public long NOTIFICATION_WHITELIST_DURATION;
        public long SENSING_TIMEOUT;
        public long SMS_TEMP_APP_WHITELIST_DURATION;
        public boolean WAIT_FOR_UNLOCK;
        private final KeyValueListParser mParser = new KeyValueListParser(',');
        private final ContentResolver mResolver;
        private final boolean mSmallBatteryDevice;

        public Constants(Handler handler, ContentResolver resolver) {
            super(handler);
            this.mResolver = resolver;
            this.mSmallBatteryDevice = ActivityManager.isSmallBatteryDevice();
            this.mResolver.registerContentObserver(Global.getUriFor("device_idle_constants"), false, this);
            updateConstants();
        }

        public void onChange(boolean selfChange, Uri uri) {
            updateConstants();
        }

        private void updateConstants() {
            synchronized (DeviceIdleController.this) {
                long j;
                try {
                    this.mParser.setString(Global.getString(this.mResolver, "device_idle_constants"));
                } catch (IllegalArgumentException e) {
                    Slog.e(DeviceIdleController.TAG, "Bad device idle settings", e);
                }
                this.LIGHT_IDLE_AFTER_INACTIVE_TIMEOUT = this.mParser.getDurationMillis(KEY_LIGHT_IDLE_AFTER_INACTIVE_TIMEOUT, 180000);
                this.LIGHT_PRE_IDLE_TIMEOUT = this.mParser.getDurationMillis(KEY_LIGHT_PRE_IDLE_TIMEOUT, 180000);
                this.LIGHT_IDLE_TIMEOUT = this.mParser.getDurationMillis(KEY_LIGHT_IDLE_TIMEOUT, BackupAgentTimeoutParameters.DEFAULT_FULL_BACKUP_AGENT_TIMEOUT_MILLIS);
                this.LIGHT_IDLE_FACTOR = this.mParser.getFloat(KEY_LIGHT_IDLE_FACTOR, 2.0f);
                this.LIGHT_MAX_IDLE_TIMEOUT = this.mParser.getDurationMillis(KEY_LIGHT_MAX_IDLE_TIMEOUT, 900000);
                this.LIGHT_IDLE_MAINTENANCE_MIN_BUDGET = this.mParser.getDurationMillis(KEY_LIGHT_IDLE_MAINTENANCE_MIN_BUDGET, 60000);
                this.LIGHT_IDLE_MAINTENANCE_MAX_BUDGET = this.mParser.getDurationMillis(KEY_LIGHT_IDLE_MAINTENANCE_MAX_BUDGET, BackupAgentTimeoutParameters.DEFAULT_FULL_BACKUP_AGENT_TIMEOUT_MILLIS);
                this.MIN_LIGHT_MAINTENANCE_TIME = this.mParser.getDurationMillis(KEY_MIN_LIGHT_MAINTENANCE_TIME, 5000);
                this.MIN_DEEP_MAINTENANCE_TIME = this.mParser.getDurationMillis(KEY_MIN_DEEP_MAINTENANCE_TIME, 30000);
                long inactiveTimeoutDefault = ((long) ((this.mSmallBatteryDevice ? 15 : 30) * 60)) * 1000;
                this.INACTIVE_TIMEOUT = this.mParser.getDurationMillis(KEY_INACTIVE_TIMEOUT, inactiveTimeoutDefault);
                KeyValueListParser keyValueListParser = this.mParser;
                String str = KEY_SENSING_TIMEOUT;
                if (DeviceIdleController.DEBUG) {
                    j = 60000;
                } else {
                    j = 240000;
                }
                this.SENSING_TIMEOUT = keyValueListParser.getDurationMillis(str, j);
                keyValueListParser = this.mParser;
                str = KEY_LOCATING_TIMEOUT;
                if (DeviceIdleController.DEBUG) {
                    j = 15000;
                } else {
                    j = 30000;
                }
                this.LOCATING_TIMEOUT = keyValueListParser.getDurationMillis(str, j);
                this.LOCATION_ACCURACY = this.mParser.getFloat(KEY_LOCATION_ACCURACY, 20.0f);
                this.MOTION_INACTIVE_TIMEOUT = this.mParser.getDurationMillis(KEY_MOTION_INACTIVE_TIMEOUT, 600000);
                this.IDLE_AFTER_INACTIVE_TIMEOUT = this.mParser.getDurationMillis(KEY_IDLE_AFTER_INACTIVE_TIMEOUT, ((long) ((this.mSmallBatteryDevice ? 15 : 30) * 60)) * 1000);
                this.IDLE_PENDING_TIMEOUT = this.mParser.getDurationMillis(KEY_IDLE_PENDING_TIMEOUT, BackupAgentTimeoutParameters.DEFAULT_FULL_BACKUP_AGENT_TIMEOUT_MILLIS);
                this.MAX_IDLE_PENDING_TIMEOUT = this.mParser.getDurationMillis(KEY_MAX_IDLE_PENDING_TIMEOUT, 600000);
                this.IDLE_PENDING_FACTOR = this.mParser.getFloat(KEY_IDLE_PENDING_FACTOR, 2.0f);
                this.IDLE_TIMEOUT = this.mParser.getDurationMillis(KEY_IDLE_TIMEOUT, SettingsObserver.DEFAULT_STRONG_USAGE_TIMEOUT);
                this.MAX_IDLE_TIMEOUT = this.mParser.getDurationMillis(KEY_MAX_IDLE_TIMEOUT, 21600000);
                this.IDLE_FACTOR = this.mParser.getFloat(KEY_IDLE_FACTOR, 2.0f);
                this.MIN_TIME_TO_ALARM = this.mParser.getDurationMillis(KEY_MIN_TIME_TO_ALARM, SettingsObserver.DEFAULT_STRONG_USAGE_TIMEOUT);
                this.MAX_TEMP_APP_WHITELIST_DURATION = this.mParser.getDurationMillis(KEY_MAX_TEMP_APP_WHITELIST_DURATION, BackupAgentTimeoutParameters.DEFAULT_FULL_BACKUP_AGENT_TIMEOUT_MILLIS);
                this.MMS_TEMP_APP_WHITELIST_DURATION = this.mParser.getDurationMillis(KEY_MMS_TEMP_APP_WHITELIST_DURATION, 60000);
                this.SMS_TEMP_APP_WHITELIST_DURATION = this.mParser.getDurationMillis(KEY_SMS_TEMP_APP_WHITELIST_DURATION, 20000);
                this.NOTIFICATION_WHITELIST_DURATION = this.mParser.getDurationMillis(KEY_NOTIFICATION_WHITELIST_DURATION, 30000);
                this.WAIT_FOR_UNLOCK = this.mParser.getBoolean(KEY_WAIT_FOR_UNLOCK, false);
            }
        }

        void dump(PrintWriter pw) {
            pw.println("  Settings:");
            pw.print("    ");
            pw.print(KEY_LIGHT_IDLE_AFTER_INACTIVE_TIMEOUT);
            pw.print("=");
            TimeUtils.formatDuration(this.LIGHT_IDLE_AFTER_INACTIVE_TIMEOUT, pw);
            pw.println();
            pw.print("    ");
            pw.print(KEY_LIGHT_PRE_IDLE_TIMEOUT);
            pw.print("=");
            TimeUtils.formatDuration(this.LIGHT_PRE_IDLE_TIMEOUT, pw);
            pw.println();
            pw.print("    ");
            pw.print(KEY_LIGHT_IDLE_TIMEOUT);
            pw.print("=");
            TimeUtils.formatDuration(this.LIGHT_IDLE_TIMEOUT, pw);
            pw.println();
            pw.print("    ");
            pw.print(KEY_LIGHT_IDLE_FACTOR);
            pw.print("=");
            pw.print(this.LIGHT_IDLE_FACTOR);
            pw.println();
            pw.print("    ");
            pw.print(KEY_LIGHT_MAX_IDLE_TIMEOUT);
            pw.print("=");
            TimeUtils.formatDuration(this.LIGHT_MAX_IDLE_TIMEOUT, pw);
            pw.println();
            pw.print("    ");
            pw.print(KEY_LIGHT_IDLE_MAINTENANCE_MIN_BUDGET);
            pw.print("=");
            TimeUtils.formatDuration(this.LIGHT_IDLE_MAINTENANCE_MIN_BUDGET, pw);
            pw.println();
            pw.print("    ");
            pw.print(KEY_LIGHT_IDLE_MAINTENANCE_MAX_BUDGET);
            pw.print("=");
            TimeUtils.formatDuration(this.LIGHT_IDLE_MAINTENANCE_MAX_BUDGET, pw);
            pw.println();
            pw.print("    ");
            pw.print(KEY_MIN_LIGHT_MAINTENANCE_TIME);
            pw.print("=");
            TimeUtils.formatDuration(this.MIN_LIGHT_MAINTENANCE_TIME, pw);
            pw.println();
            pw.print("    ");
            pw.print(KEY_MIN_DEEP_MAINTENANCE_TIME);
            pw.print("=");
            TimeUtils.formatDuration(this.MIN_DEEP_MAINTENANCE_TIME, pw);
            pw.println();
            pw.print("    ");
            pw.print(KEY_INACTIVE_TIMEOUT);
            pw.print("=");
            TimeUtils.formatDuration(this.INACTIVE_TIMEOUT, pw);
            pw.println();
            pw.print("    ");
            pw.print(KEY_SENSING_TIMEOUT);
            pw.print("=");
            TimeUtils.formatDuration(this.SENSING_TIMEOUT, pw);
            pw.println();
            pw.print("    ");
            pw.print(KEY_LOCATING_TIMEOUT);
            pw.print("=");
            TimeUtils.formatDuration(this.LOCATING_TIMEOUT, pw);
            pw.println();
            pw.print("    ");
            pw.print(KEY_LOCATION_ACCURACY);
            pw.print("=");
            pw.print(this.LOCATION_ACCURACY);
            pw.print("m");
            pw.println();
            pw.print("    ");
            pw.print(KEY_MOTION_INACTIVE_TIMEOUT);
            pw.print("=");
            TimeUtils.formatDuration(this.MOTION_INACTIVE_TIMEOUT, pw);
            pw.println();
            pw.print("    ");
            pw.print(KEY_IDLE_AFTER_INACTIVE_TIMEOUT);
            pw.print("=");
            TimeUtils.formatDuration(this.IDLE_AFTER_INACTIVE_TIMEOUT, pw);
            pw.println();
            pw.print("    ");
            pw.print(KEY_IDLE_PENDING_TIMEOUT);
            pw.print("=");
            TimeUtils.formatDuration(this.IDLE_PENDING_TIMEOUT, pw);
            pw.println();
            pw.print("    ");
            pw.print(KEY_MAX_IDLE_PENDING_TIMEOUT);
            pw.print("=");
            TimeUtils.formatDuration(this.MAX_IDLE_PENDING_TIMEOUT, pw);
            pw.println();
            pw.print("    ");
            pw.print(KEY_IDLE_PENDING_FACTOR);
            pw.print("=");
            pw.println(this.IDLE_PENDING_FACTOR);
            pw.print("    ");
            pw.print(KEY_IDLE_TIMEOUT);
            pw.print("=");
            TimeUtils.formatDuration(this.IDLE_TIMEOUT, pw);
            pw.println();
            pw.print("    ");
            pw.print(KEY_MAX_IDLE_TIMEOUT);
            pw.print("=");
            TimeUtils.formatDuration(this.MAX_IDLE_TIMEOUT, pw);
            pw.println();
            pw.print("    ");
            pw.print(KEY_IDLE_FACTOR);
            pw.print("=");
            pw.println(this.IDLE_FACTOR);
            pw.print("    ");
            pw.print(KEY_MIN_TIME_TO_ALARM);
            pw.print("=");
            TimeUtils.formatDuration(this.MIN_TIME_TO_ALARM, pw);
            pw.println();
            pw.print("    ");
            pw.print(KEY_MAX_TEMP_APP_WHITELIST_DURATION);
            pw.print("=");
            TimeUtils.formatDuration(this.MAX_TEMP_APP_WHITELIST_DURATION, pw);
            pw.println();
            pw.print("    ");
            pw.print(KEY_MMS_TEMP_APP_WHITELIST_DURATION);
            pw.print("=");
            TimeUtils.formatDuration(this.MMS_TEMP_APP_WHITELIST_DURATION, pw);
            pw.println();
            pw.print("    ");
            pw.print(KEY_SMS_TEMP_APP_WHITELIST_DURATION);
            pw.print("=");
            TimeUtils.formatDuration(this.SMS_TEMP_APP_WHITELIST_DURATION, pw);
            pw.println();
            pw.print("    ");
            pw.print(KEY_NOTIFICATION_WHITELIST_DURATION);
            pw.print("=");
            TimeUtils.formatDuration(this.NOTIFICATION_WHITELIST_DURATION, pw);
            pw.println();
            pw.print("    ");
            pw.print(KEY_WAIT_FOR_UNLOCK);
            pw.print("=");
            pw.println(this.WAIT_FOR_UNLOCK);
        }
    }

    public class LocalService {
        public void addPowerSaveTempWhitelistApp(int callingUid, String packageName, long duration, int userId, boolean sync, String reason) {
            DeviceIdleController.this.addPowerSaveTempWhitelistAppInternal(callingUid, packageName, duration, userId, sync, reason);
        }

        public void addPowerSaveTempWhitelistAppDirect(int appId, long duration, boolean sync, String reason) {
            DeviceIdleController.this.addPowerSaveTempWhitelistAppDirectInternal(0, appId, duration, sync, reason);
        }

        public long getNotificationWhitelistDuration() {
            return DeviceIdleController.this.mConstants.NOTIFICATION_WHITELIST_DURATION;
        }

        public void setJobsActive(boolean active) {
            DeviceIdleController.this.setJobsActive(active);
        }

        public void setAlarmsActive(boolean active) {
            DeviceIdleController.this.setAlarmsActive(active);
        }

        public boolean isAppOnWhitelist(int appid) {
            return DeviceIdleController.this.isAppOnWhitelistInternal(appid);
        }

        public int[] getPowerSaveWhitelistUserAppIds() {
            return DeviceIdleController.this.getPowerSaveWhitelistUserAppIds();
        }

        public int[] getPowerSaveTempWhitelistAppIds() {
            return DeviceIdleController.this.getAppIdTempWhitelistInternal();
        }
    }

    private final class MotionListener extends TriggerEventListener implements SensorEventListener {
        boolean active;

        private MotionListener() {
            this.active = false;
        }

        /* synthetic */ MotionListener(DeviceIdleController x0, AnonymousClass1 x1) {
            this();
        }

        public void onTrigger(TriggerEvent event) {
            synchronized (DeviceIdleController.this) {
                this.active = false;
                DeviceIdleController.this.motionLocked();
            }
        }

        public void onSensorChanged(SensorEvent event) {
            synchronized (DeviceIdleController.this) {
                DeviceIdleController.this.mSensorManager.unregisterListener(this, DeviceIdleController.this.mMotionSensor);
                this.active = false;
                DeviceIdleController.this.motionLocked();
            }
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        public boolean registerLocked() {
            boolean success;
            if (DeviceIdleController.this.mMotionSensor.getReportingMode() == 2) {
                success = DeviceIdleController.this.mSensorManager.requestTriggerSensor(DeviceIdleController.this.mMotionListener, DeviceIdleController.this.mMotionSensor);
            } else {
                success = DeviceIdleController.this.mSensorManager.registerListener(DeviceIdleController.this.mMotionListener, DeviceIdleController.this.mMotionSensor, 3);
            }
            if (success) {
                this.active = true;
            } else {
                String str = DeviceIdleController.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unable to register for ");
                stringBuilder.append(DeviceIdleController.this.mMotionSensor);
                Slog.e(str, stringBuilder.toString());
            }
            return success;
        }

        public void unregisterLocked() {
            if (DeviceIdleController.this.mMotionSensor.getReportingMode() == 2) {
                DeviceIdleController.this.mSensorManager.cancelTriggerSensor(DeviceIdleController.this.mMotionListener, DeviceIdleController.this.mMotionSensor);
            } else {
                DeviceIdleController.this.mSensorManager.unregisterListener(DeviceIdleController.this.mMotionListener);
            }
            this.active = false;
        }
    }

    final class MyHandler extends Handler {
        MyHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            String str;
            if (DeviceIdleController.DEBUG) {
                str = DeviceIdleController.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("handleMessage(");
                stringBuilder.append(msg.what);
                stringBuilder.append(")");
                Slog.d(str, stringBuilder.toString());
            }
            int i = 1;
            int i2 = 0;
            boolean deepChanged;
            boolean lightChanged;
            int activeUid;
            switch (msg.what) {
                case 1:
                    DeviceIdleController.this.handleWriteConfigFile();
                    return;
                case 2:
                case 3:
                    boolean lightChanged2;
                    EventLogTags.writeDeviceIdleOnStart();
                    if (msg.what == 2) {
                        deepChanged = DeviceIdleController.this.mLocalPowerManager.setDeviceIdleMode(true);
                        lightChanged2 = DeviceIdleController.this.mLocalPowerManager.setLightDeviceIdleMode(false);
                    } else {
                        deepChanged = DeviceIdleController.this.mLocalPowerManager.setDeviceIdleMode(false);
                        lightChanged2 = DeviceIdleController.this.mLocalPowerManager.setLightDeviceIdleMode(true);
                    }
                    try {
                        DeviceIdleController.this.mNetworkPolicyManager.setDeviceIdleMode(true);
                        IBatteryStats access$900 = DeviceIdleController.this.mBatteryStats;
                        if (msg.what == 2) {
                            i = 2;
                        }
                        access$900.noteDeviceIdleMode(i, null, Process.myUid());
                    } catch (RemoteException e) {
                    }
                    if (deepChanged) {
                        DeviceIdleController.this.getContext().sendBroadcastAsUser(DeviceIdleController.this.mIdleIntent, UserHandle.ALL);
                    }
                    if (lightChanged2) {
                        DeviceIdleController.this.getContext().sendBroadcastAsUser(DeviceIdleController.this.mLightIdleIntent, UserHandle.ALL);
                    }
                    EventLogTags.writeDeviceIdleOnComplete();
                    DeviceIdleController.this.mGoingIdleWakeLock.release();
                    return;
                case 4:
                    EventLogTags.writeDeviceIdleOffStart(Shell.NIGHT_MODE_STR_UNKNOWN);
                    deepChanged = DeviceIdleController.this.mLocalPowerManager.setDeviceIdleMode(false);
                    lightChanged = DeviceIdleController.this.mLocalPowerManager.setLightDeviceIdleMode(false);
                    try {
                        DeviceIdleController.this.mNetworkPolicyManager.setDeviceIdleMode(false);
                        DeviceIdleController.this.mBatteryStats.noteDeviceIdleMode(0, null, Process.myUid());
                    } catch (RemoteException e2) {
                    }
                    if (deepChanged) {
                        DeviceIdleController.this.incActiveIdleOps();
                        DeviceIdleController.this.getContext().sendOrderedBroadcastAsUser(DeviceIdleController.this.mIdleIntent, UserHandle.ALL, null, DeviceIdleController.this.mIdleStartedDoneReceiver, null, 0, null, null);
                    }
                    if (lightChanged) {
                        DeviceIdleController.this.incActiveIdleOps();
                        DeviceIdleController.this.getContext().sendOrderedBroadcastAsUser(DeviceIdleController.this.mLightIdleIntent, UserHandle.ALL, null, DeviceIdleController.this.mIdleStartedDoneReceiver, null, 0, null, null);
                    }
                    DeviceIdleController.this.decActiveIdleOps();
                    EventLogTags.writeDeviceIdleOffComplete();
                    return;
                case 5:
                    String str2;
                    str = msg.obj;
                    activeUid = msg.arg1;
                    if (str != null) {
                        str2 = str;
                    } else {
                        str2 = Shell.NIGHT_MODE_STR_UNKNOWN;
                    }
                    EventLogTags.writeDeviceIdleOffStart(str2);
                    lightChanged = DeviceIdleController.this.mLocalPowerManager.setDeviceIdleMode(false);
                    boolean lightChanged3 = DeviceIdleController.this.mLocalPowerManager.setLightDeviceIdleMode(false);
                    try {
                        DeviceIdleController.this.mNetworkPolicyManager.setDeviceIdleMode(false);
                        DeviceIdleController.this.mBatteryStats.noteDeviceIdleMode(0, str, activeUid);
                    } catch (RemoteException e3) {
                    }
                    if (lightChanged) {
                        DeviceIdleController.this.getContext().sendBroadcastAsUser(DeviceIdleController.this.mIdleIntent, UserHandle.ALL);
                    }
                    if (lightChanged3) {
                        DeviceIdleController.this.getContext().sendBroadcastAsUser(DeviceIdleController.this.mLightIdleIntent, UserHandle.ALL);
                    }
                    EventLogTags.writeDeviceIdleOffComplete();
                    return;
                case 6:
                    DeviceIdleController.this.checkTempAppWhitelistTimeout(msg.arg1);
                    return;
                case 7:
                    if (msg.arg1 != 1) {
                        lightChanged = false;
                    }
                    deepChanged = lightChanged;
                    activeUid = DeviceIdleController.this.mMaintenanceActivityListeners.beginBroadcast();
                    while (true) {
                        i = i2;
                        if (i < activeUid) {
                            try {
                                ((IMaintenanceActivityListener) DeviceIdleController.this.mMaintenanceActivityListeners.getBroadcastItem(i)).onMaintenanceActivityChanged(deepChanged);
                            } catch (RemoteException e4) {
                            } catch (Throwable th) {
                                DeviceIdleController.this.mMaintenanceActivityListeners.finishBroadcast();
                            }
                            i2 = i + 1;
                        } else {
                            DeviceIdleController.this.mMaintenanceActivityListeners.finishBroadcast();
                            return;
                        }
                    }
                case 8:
                    DeviceIdleController.this.decActiveIdleOps();
                    return;
                case 9:
                    int appId = msg.arg1;
                    if (msg.arg2 != 1) {
                        lightChanged = false;
                    }
                    DeviceIdleController.this.mNetworkPolicyManagerInternal.onTempPowerSaveWhitelistChange(appId, lightChanged);
                    return;
                default:
                    return;
            }
        }
    }

    class Shell extends ShellCommand {
        int userId = 0;

        Shell() {
        }

        public int onCommand(String cmd) {
            return DeviceIdleController.this.onShellCommand(this, cmd);
        }

        public void onHelp() {
            DeviceIdleController.dumpHelp(getOutPrintWriter());
        }
    }

    static {
        boolean z = Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4));
        DEBUG = z;
    }

    private static String stateToString(int state) {
        switch (state) {
            case 0:
                return "ACTIVE";
            case 1:
                return "INACTIVE";
            case 2:
                return "IDLE_PENDING";
            case 3:
                return "SENSING";
            case 4:
                return "LOCATING";
            case 5:
                return "IDLE";
            case 6:
                return "IDLE_MAINTENANCE";
            default:
                return Integer.toString(state);
        }
    }

    private static String lightStateToString(int state) {
        switch (state) {
            case 0:
                return "ACTIVE";
            case 1:
                return "INACTIVE";
            case 3:
                return "PRE_IDLE";
            case 4:
                return "IDLE";
            case 5:
                return "WAITING_FOR_NETWORK";
            case 6:
                return "IDLE_MAINTENANCE";
            case 7:
                return "OVERRIDE";
            default:
                return Integer.toString(state);
        }
    }

    private void addEvent(int cmd, String reason) {
        if (this.mEventCmds[0] != cmd) {
            System.arraycopy(this.mEventCmds, 0, this.mEventCmds, 1, 99);
            System.arraycopy(this.mEventTimes, 0, this.mEventTimes, 1, 99);
            System.arraycopy(this.mEventReasons, 0, this.mEventReasons, 1, 99);
            this.mEventCmds[0] = cmd;
            this.mEventTimes[0] = SystemClock.elapsedRealtime();
            this.mEventReasons[0] = reason;
        }
    }

    public void onAnyMotionResult(int result) {
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onAnyMotionResult(");
            stringBuilder.append(result);
            stringBuilder.append(")");
            Slog.d(str, stringBuilder.toString());
        }
        if (result != -1) {
            synchronized (this) {
                cancelSensingTimeoutAlarmLocked();
            }
        }
        if (result == 1 || result == -1) {
            synchronized (this) {
                handleMotionDetectedLocked(this.mConstants.INACTIVE_TIMEOUT, "non_stationary");
            }
        } else if (result != 0) {
        } else {
            if (this.mState == 3) {
                synchronized (this) {
                    this.mNotMoving = true;
                    stepIdleStateLocked("s:stationary");
                }
            } else if (this.mState == 4) {
                synchronized (this) {
                    this.mNotMoving = true;
                    if (this.mLocated) {
                        stepIdleStateLocked("s:stationary");
                    }
                }
            }
        }
    }

    public DeviceIdleController(Context context) {
        super(context);
        this.mAppStateTracker = new AppStateTracker(context, FgThread.get().getLooper());
        LocalServices.addService(AppStateTracker.class, this.mAppStateTracker);
    }

    boolean isAppOnWhitelistInternal(int appid) {
        boolean z;
        synchronized (this) {
            z = Arrays.binarySearch(this.mPowerSaveWhitelistAllAppIdArray, appid) >= 0;
        }
        return z;
    }

    int[] getPowerSaveWhitelistUserAppIds() {
        int[] iArr;
        synchronized (this) {
            iArr = this.mPowerSaveWhitelistUserAppIdArray;
        }
        return iArr;
    }

    private static File getSystemDir() {
        return new File(Environment.getDataDirectory(), "system");
    }

    public void onStart() {
        PackageManager pm = getContext().getPackageManager();
        synchronized (this) {
            boolean z = getContext().getResources().getBoolean(17956949);
            this.mDeepEnabled = z;
            this.mLightEnabled = z;
            SystemConfig sysConfig = SystemConfig.getInstance();
            ArraySet<String> allowPowerExceptIdle = sysConfig.getAllowInPowerSaveExceptIdle();
            for (int i = 0; i < allowPowerExceptIdle.size(); i++) {
                try {
                    ApplicationInfo ai = pm.getApplicationInfo((String) allowPowerExceptIdle.valueAt(i), DumpState.DUMP_DEXOPT);
                    int appid = UserHandle.getAppId(ai.uid);
                    this.mPowerSaveWhitelistAppsExceptIdle.put(ai.packageName, Integer.valueOf(appid));
                    this.mPowerSaveWhitelistSystemAppIdsExceptIdle.put(appid, true);
                } catch (NameNotFoundException e) {
                }
            }
            ArraySet<String> allowPower = sysConfig.getAllowInPowerSave();
            for (int i2 = 0; i2 < allowPower.size(); i2++) {
                try {
                    ApplicationInfo ai2 = pm.getApplicationInfo((String) allowPower.valueAt(i2), DumpState.DUMP_DEXOPT);
                    int appid2 = UserHandle.getAppId(ai2.uid);
                    this.mPowerSaveWhitelistAppsExceptIdle.put(ai2.packageName, Integer.valueOf(appid2));
                    this.mPowerSaveWhitelistSystemAppIdsExceptIdle.put(appid2, true);
                    this.mPowerSaveWhitelistApps.put(ai2.packageName, Integer.valueOf(appid2));
                    this.mPowerSaveWhitelistSystemAppIds.put(appid2, true);
                } catch (NameNotFoundException e2) {
                }
            }
            this.mConstants = new Constants(this.mHandler, getContext().getContentResolver());
            readConfigFileLocked();
            updateWhitelistAppIdsLocked();
            this.mNetworkConnected = true;
            this.mScreenOn = true;
            this.mScreenLocked = false;
            this.mCharging = true;
            this.mState = 0;
            this.mLightState = 0;
            this.mInactiveTimeout = this.mConstants.INACTIVE_TIMEOUT;
        }
        this.mBinderService = new BinderService(this, null);
        publishBinderService("deviceidle", this.mBinderService);
        publishLocalService(LocalService.class, new LocalService());
    }

    public void onBootPhase(int phase) {
        if (phase == 500) {
            synchronized (this) {
                this.mAlarmManager = (AlarmManager) getContext().getSystemService("alarm");
                this.mBatteryStats = BatteryStatsService.getService();
                this.mLocalActivityManager = (ActivityManagerInternal) getLocalService(ActivityManagerInternal.class);
                this.mLocalPowerManager = (PowerManagerInternal) getLocalService(PowerManagerInternal.class);
                this.mPowerManager = (PowerManager) getContext().getSystemService(PowerManager.class);
                this.mActiveIdleWakeLock = this.mPowerManager.newWakeLock(1, "deviceidle_maint");
                this.mActiveIdleWakeLock.setReferenceCounted(false);
                this.mGoingIdleWakeLock = this.mPowerManager.newWakeLock(1, "deviceidle_going_idle");
                this.mGoingIdleWakeLock.setReferenceCounted(true);
                this.mConnectivityService = (ConnectivityService) ServiceManager.getService("connectivity");
                this.mNetworkPolicyManager = INetworkPolicyManager.Stub.asInterface(ServiceManager.getService("netpolicy"));
                this.mNetworkPolicyManagerInternal = (NetworkPolicyManagerInternal) getLocalService(NetworkPolicyManagerInternal.class);
                this.mSensorManager = (SensorManager) getContext().getSystemService("sensor");
                int sigMotionSensorId = getContext().getResources().getInteger(17694736);
                if (sigMotionSensorId > 0) {
                    this.mMotionSensor = this.mSensorManager.getDefaultSensor(sigMotionSensorId, true);
                }
                if (this.mMotionSensor == null && getContext().getResources().getBoolean(17956892)) {
                    this.mMotionSensor = this.mSensorManager.getDefaultSensor(26, true);
                }
                if (this.mMotionSensor == null) {
                    this.mMotionSensor = this.mSensorManager.getDefaultSensor(17, true);
                }
                if (getContext().getResources().getBoolean(17956893)) {
                    this.mLocationManager = (LocationManager) getContext().getSystemService("location");
                    this.mLocationRequest = new LocationRequest().setQuality(100).setInterval(0).setFastestInterval(0).setNumUpdates(1);
                }
                this.mAnyMotionDetector = new AnyMotionDetector((PowerManager) getContext().getSystemService("power"), this.mHandler, this.mSensorManager, this, ((float) getContext().getResources().getInteger(17694737)) / 100.0f);
                this.mAppStateTracker.onSystemServicesReady();
                this.mIdleIntent = new Intent("android.os.action.DEVICE_IDLE_MODE_CHANGED");
                this.mIdleIntent.addFlags(1342177280);
                this.mLightIdleIntent = new Intent("android.os.action.LIGHT_DEVICE_IDLE_MODE_CHANGED");
                this.mLightIdleIntent.addFlags(1342177280);
                IntentFilter filter = new IntentFilter();
                filter.addAction("android.intent.action.BATTERY_CHANGED");
                getContext().registerReceiver(this.mReceiver, filter);
                filter = new IntentFilter();
                filter.addAction("android.intent.action.PACKAGE_REMOVED");
                filter.addDataScheme("package");
                getContext().registerReceiver(this.mReceiver, filter);
                filter = new IntentFilter();
                filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
                getContext().registerReceiver(this.mReceiver, filter);
                filter = new IntentFilter();
                filter.addAction("android.intent.action.SCREEN_OFF");
                filter.addAction("android.intent.action.SCREEN_ON");
                getContext().registerReceiver(this.mInteractivityReceiver, filter);
                this.mLocalActivityManager.setDeviceIdleWhitelist(this.mPowerSaveWhitelistAllAppIdArray, this.mPowerSaveWhitelistExceptIdleAppIdArray);
                this.mLocalPowerManager.setDeviceIdleWhitelist(this.mPowerSaveWhitelistAllAppIdArray);
                this.mLocalActivityManager.registerScreenObserver(this.mScreenObserver);
                passWhiteListsToForceAppStandbyTrackerLocked();
                updateInteractivityLocked();
            }
            updateConnectivityState(null);
        } else if (phase == 1000) {
            Slog.d(TAG, "PHASE_BOOT_COMPLETED");
            this.mHandler.postDelayed(new Runnable() {
                private static final int MAX_TRY_TIMES = 3;
                private int count = 0;

                public void run() {
                    boolean z = true;
                    this.count++;
                    if (this.count < 3) {
                        z = false;
                    }
                    if (DeviceIdleController.this.updateWhitelistFromDB(z)) {
                        synchronized (this) {
                            DeviceIdleController.this.writeConfigFileLocked();
                        }
                    } else if (this.count < 3) {
                        DeviceIdleController.this.mHandler.postDelayed(this, JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
                    }
                }
            }, JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
        }
    }

    public boolean addPowerSaveWhitelistAppInternal(String name) {
        synchronized (this) {
            try {
                if (this.mPowerSaveWhitelistUserApps.put(name, Integer.valueOf(UserHandle.getAppId(getContext().getPackageManager().getApplicationInfo(name, DumpState.DUMP_CHANGES).uid))) == null) {
                    reportPowerSaveWhitelistChangedLocked();
                    updateWhitelistAppIdsLocked();
                    writeConfigFileLocked();
                }
            } catch (NameNotFoundException e) {
                return false;
            } catch (Throwable th) {
            }
        }
        return true;
    }

    public boolean removePowerSaveWhitelistAppInternal(String name) {
        synchronized (this) {
            if (this.mPowerSaveWhitelistUserApps.remove(name) != null) {
                reportPowerSaveWhitelistChangedLocked();
                updateWhitelistAppIdsLocked();
                writeConfigFileLocked();
                return true;
            }
            return false;
        }
    }

    public boolean getPowerSaveWhitelistAppInternal(String name) {
        boolean containsKey;
        synchronized (this) {
            containsKey = this.mPowerSaveWhitelistUserApps.containsKey(name);
        }
        return containsKey;
    }

    void resetSystemPowerWhitelistInternal() {
        synchronized (this) {
            this.mPowerSaveWhitelistApps.putAll(this.mRemovedFromSystemWhitelistApps);
            this.mRemovedFromSystemWhitelistApps.clear();
            reportPowerSaveWhitelistChangedLocked();
            updateWhitelistAppIdsLocked();
            writeConfigFileLocked();
        }
    }

    public boolean restoreSystemPowerWhitelistAppInternal(String name) {
        synchronized (this) {
            if (this.mRemovedFromSystemWhitelistApps.containsKey(name)) {
                this.mPowerSaveWhitelistApps.put(name, (Integer) this.mRemovedFromSystemWhitelistApps.remove(name));
                reportPowerSaveWhitelistChangedLocked();
                updateWhitelistAppIdsLocked();
                writeConfigFileLocked();
                return true;
            }
            return false;
        }
    }

    public boolean removeSystemPowerWhitelistAppInternal(String name) {
        synchronized (this) {
            if (this.mPowerSaveWhitelistApps.containsKey(name)) {
                this.mRemovedFromSystemWhitelistApps.put(name, (Integer) this.mPowerSaveWhitelistApps.remove(name));
                reportPowerSaveWhitelistChangedLocked();
                updateWhitelistAppIdsLocked();
                writeConfigFileLocked();
                return true;
            }
            return false;
        }
    }

    public boolean addPowerSaveWhitelistExceptIdleInternal(String name) {
        synchronized (this) {
            try {
                if (this.mPowerSaveWhitelistAppsExceptIdle.put(name, Integer.valueOf(UserHandle.getAppId(getContext().getPackageManager().getApplicationInfo(name, DumpState.DUMP_CHANGES).uid))) == null) {
                    this.mPowerSaveWhitelistUserAppsExceptIdle.add(name);
                    reportPowerSaveWhitelistChangedLocked();
                    this.mPowerSaveWhitelistExceptIdleAppIdArray = buildAppIdArray(this.mPowerSaveWhitelistAppsExceptIdle, this.mPowerSaveWhitelistUserApps, this.mPowerSaveWhitelistExceptIdleAppIds);
                    passWhiteListsToForceAppStandbyTrackerLocked();
                }
            } catch (NameNotFoundException e) {
                return false;
            } catch (Throwable th) {
            }
        }
        return true;
    }

    public void resetPowerSaveWhitelistExceptIdleInternal() {
        synchronized (this) {
            if (this.mPowerSaveWhitelistAppsExceptIdle.removeAll(this.mPowerSaveWhitelistUserAppsExceptIdle)) {
                reportPowerSaveWhitelistChangedLocked();
                this.mPowerSaveWhitelistExceptIdleAppIdArray = buildAppIdArray(this.mPowerSaveWhitelistAppsExceptIdle, this.mPowerSaveWhitelistUserApps, this.mPowerSaveWhitelistExceptIdleAppIds);
                this.mPowerSaveWhitelistUserAppsExceptIdle.clear();
                passWhiteListsToForceAppStandbyTrackerLocked();
            }
        }
    }

    public boolean getPowerSaveWhitelistExceptIdleInternal(String name) {
        boolean containsKey;
        synchronized (this) {
            containsKey = this.mPowerSaveWhitelistAppsExceptIdle.containsKey(name);
        }
        return containsKey;
    }

    public String[] getSystemPowerWhitelistExceptIdleInternal() {
        String[] apps;
        synchronized (this) {
            int size = this.mPowerSaveWhitelistAppsExceptIdle.size();
            apps = new String[size];
            for (int i = 0; i < size; i++) {
                apps[i] = (String) this.mPowerSaveWhitelistAppsExceptIdle.keyAt(i);
            }
        }
        return apps;
    }

    public String[] getSystemPowerWhitelistInternal() {
        String[] apps;
        synchronized (this) {
            int size = this.mPowerSaveWhitelistApps.size();
            apps = new String[size];
            for (int i = 0; i < size; i++) {
                apps[i] = (String) this.mPowerSaveWhitelistApps.keyAt(i);
            }
        }
        return apps;
    }

    public String[] getRemovedSystemPowerWhitelistAppsInternal() {
        String[] apps;
        synchronized (this) {
            int size = this.mRemovedFromSystemWhitelistApps.size();
            apps = new String[size];
            for (int i = 0; i < size; i++) {
                apps[i] = (String) this.mRemovedFromSystemWhitelistApps.keyAt(i);
            }
        }
        return apps;
    }

    public String[] getUserPowerWhitelistInternal() {
        String[] apps;
        synchronized (this) {
            apps = new String[this.mPowerSaveWhitelistUserApps.size()];
            for (int i = 0; i < this.mPowerSaveWhitelistUserApps.size(); i++) {
                apps[i] = (String) this.mPowerSaveWhitelistUserApps.keyAt(i);
            }
        }
        return apps;
    }

    public String[] getFullPowerWhitelistExceptIdleInternal() {
        String[] apps;
        synchronized (this) {
            int i;
            apps = new String[(this.mPowerSaveWhitelistAppsExceptIdle.size() + this.mPowerSaveWhitelistUserApps.size())];
            int i2 = 0;
            int cur = 0;
            for (i = 0; i < this.mPowerSaveWhitelistAppsExceptIdle.size(); i++) {
                apps[cur] = (String) this.mPowerSaveWhitelistAppsExceptIdle.keyAt(i);
                cur++;
            }
            while (true) {
                i = i2;
                if (i < this.mPowerSaveWhitelistUserApps.size()) {
                    apps[cur] = (String) this.mPowerSaveWhitelistUserApps.keyAt(i);
                    cur++;
                    i2 = i + 1;
                }
            }
        }
        return apps;
    }

    public String[] getFullPowerWhitelistInternal() {
        String[] apps;
        synchronized (this) {
            int i;
            apps = new String[(this.mPowerSaveWhitelistApps.size() + this.mPowerSaveWhitelistUserApps.size())];
            int i2 = 0;
            int cur = 0;
            for (i = 0; i < this.mPowerSaveWhitelistApps.size(); i++) {
                apps[cur] = (String) this.mPowerSaveWhitelistApps.keyAt(i);
                cur++;
            }
            while (true) {
                i = i2;
                if (i < this.mPowerSaveWhitelistUserApps.size()) {
                    apps[cur] = (String) this.mPowerSaveWhitelistUserApps.keyAt(i);
                    cur++;
                    i2 = i + 1;
                }
            }
        }
        return apps;
    }

    public boolean isPowerSaveWhitelistExceptIdleAppInternal(String packageName) {
        boolean z;
        synchronized (this) {
            if (!this.mPowerSaveWhitelistAppsExceptIdle.containsKey(packageName)) {
                if (!this.mPowerSaveWhitelistUserApps.containsKey(packageName)) {
                    z = false;
                }
            }
            z = true;
        }
        return z;
    }

    public boolean isPowerSaveWhitelistAppInternal(String packageName) {
        boolean z;
        synchronized (this) {
            if (!this.mPowerSaveWhitelistApps.containsKey(packageName)) {
                if (!this.mPowerSaveWhitelistUserApps.containsKey(packageName)) {
                    z = false;
                }
            }
            z = true;
        }
        return z;
    }

    public int[] getAppIdWhitelistExceptIdleInternal() {
        int[] iArr;
        synchronized (this) {
            iArr = this.mPowerSaveWhitelistExceptIdleAppIdArray;
        }
        return iArr;
    }

    public int[] getAppIdWhitelistInternal() {
        int[] iArr;
        synchronized (this) {
            iArr = this.mPowerSaveWhitelistAllAppIdArray;
        }
        return iArr;
    }

    public int[] getAppIdUserWhitelistInternal() {
        int[] iArr;
        synchronized (this) {
            iArr = this.mPowerSaveWhitelistUserAppIdArray;
        }
        return iArr;
    }

    public int[] getAppIdTempWhitelistInternal() {
        int[] iArr;
        synchronized (this) {
            iArr = this.mTempWhitelistAppIdArray;
        }
        return iArr;
    }

    void addPowerSaveTempWhitelistAppChecked(String packageName, long duration, int userId, String reason) throws RemoteException {
        getContext().enforceCallingPermission("android.permission.CHANGE_DEVICE_IDLE_TEMP_WHITELIST", "No permission to change device idle whitelist");
        int callingUid = Binder.getCallingUid();
        int userId2 = ActivityManager.getService().handleIncomingUser(Binder.getCallingPid(), callingUid, userId, false, false, "addPowerSaveTempWhitelistApp", null);
        long token = Binder.clearCallingIdentity();
        try {
            addPowerSaveTempWhitelistAppInternal(callingUid, packageName, duration, userId2, true, reason);
            Binder.restoreCallingIdentity(token);
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(token);
            Throwable th2 = th;
        }
    }

    void removePowerSaveTempWhitelistAppChecked(String packageName, int userId) throws RemoteException {
        getContext().enforceCallingPermission("android.permission.CHANGE_DEVICE_IDLE_TEMP_WHITELIST", "No permission to change device idle whitelist");
        userId = ActivityManager.getService().handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, false, false, "removePowerSaveTempWhitelistApp", null);
        long token = Binder.clearCallingIdentity();
        try {
            removePowerSaveTempWhitelistAppInternal(packageName, userId);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    void addPowerSaveTempWhitelistAppInternal(int callingUid, String packageName, long duration, int userId, boolean sync, String reason) {
        try {
            addPowerSaveTempWhitelistAppDirectInternal(callingUid, UserHandle.getAppId(getContext().getPackageManager().getPackageUidAsUser(packageName, userId)), duration, sync, reason);
        } catch (NameNotFoundException e) {
        }
    }

    /* JADX WARNING: Missing block: B:40:0x00c0, code skipped:
            if (r6 == false) goto L_?;
     */
    /* JADX WARNING: Missing block: B:41:0x00c2, code skipped:
            r1.mNetworkPolicyManagerInternal.onTempPowerSaveWhitelistChange(r2, true);
     */
    /* JADX WARNING: Missing block: B:50:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:51:?, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void addPowerSaveTempWhitelistAppDirectInternal(int callingUid, int appId, long duration, boolean sync, String reason) {
        Throwable th;
        int i = appId;
        String str = reason;
        long timeNow = SystemClock.elapsedRealtime();
        boolean informWhitelistChanged = false;
        synchronized (this) {
            long j;
            long j2;
            try {
                int callingAppId = UserHandle.getAppId(callingUid);
                if (callingAppId >= 10000) {
                    try {
                        if (!this.mPowerSaveWhitelistSystemAppIds.get(callingAppId)) {
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Calling app ");
                            stringBuilder.append(UserHandle.formatUid(callingUid));
                            stringBuilder.append(" is not on whitelist");
                            throw new SecurityException(stringBuilder.toString());
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        j = duration;
                        throw th;
                    }
                }
                j2 = duration;
                try {
                    j = Math.min(j2, this.mConstants.MAX_TEMP_APP_WHITELIST_DURATION);
                    Pair<MutableLong, String> entry = (Pair) this.mTempWhitelistAppIdEndTimes.get(i);
                    boolean newEntry = entry == null;
                    if (newEntry) {
                        entry = new Pair(new MutableLong(0), str);
                        this.mTempWhitelistAppIdEndTimes.put(i, entry);
                    }
                    ((MutableLong) entry.first).value = timeNow + j;
                    if (DEBUG) {
                        String str2 = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Adding AppId ");
                        stringBuilder2.append(i);
                        stringBuilder2.append(" to temp whitelist. New entry: ");
                        stringBuilder2.append(newEntry);
                        Slog.d(str2, stringBuilder2.toString());
                    }
                    if (newEntry) {
                        try {
                            this.mBatteryStats.noteEvent(32785, str, i);
                        } catch (RemoteException e) {
                        }
                        try {
                            postTempActiveTimeoutMessage(i, j);
                            updateTempWhitelistAppIdsLocked(i, true);
                            if (sync) {
                                informWhitelistChanged = true;
                            } else {
                                this.mHandler.obtainMessage(9, i, 1).sendToTarget();
                            }
                            reportTempWhitelistChangedLocked();
                        } catch (Throwable th3) {
                            th = th3;
                            throw th;
                        }
                    }
                } catch (Throwable th4) {
                    th = th4;
                    j = j2;
                    throw th;
                }
            } catch (Throwable th5) {
                th = th5;
                j2 = duration;
                j = j2;
                throw th;
            }
        }
    }

    private void removePowerSaveTempWhitelistAppInternal(String packageName, int userId) {
        try {
            removePowerSaveTempWhitelistAppDirectInternal(UserHandle.getAppId(getContext().getPackageManager().getPackageUidAsUser(packageName, userId)));
        } catch (NameNotFoundException e) {
        }
    }

    private void removePowerSaveTempWhitelistAppDirectInternal(int appId) {
        synchronized (this) {
            int idx = this.mTempWhitelistAppIdEndTimes.indexOfKey(appId);
            if (idx < 0) {
                return;
            }
            String reason = ((Pair) this.mTempWhitelistAppIdEndTimes.valueAt(idx)).second;
            this.mTempWhitelistAppIdEndTimes.removeAt(idx);
            onAppRemovedFromTempWhitelistLocked(appId, reason);
        }
    }

    private void postTempActiveTimeoutMessage(int uid, long delay) {
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("postTempActiveTimeoutMessage: uid=");
            stringBuilder.append(uid);
            stringBuilder.append(", delay=");
            stringBuilder.append(delay);
            Slog.d(str, stringBuilder.toString());
        }
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(6, uid, 0), delay);
    }

    /* JADX WARNING: Missing block: B:17:0x007d, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void checkTempAppWhitelistTimeout(int uid) {
        long timeNow = SystemClock.elapsedRealtime();
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("checkTempAppWhitelistTimeout: uid=");
            stringBuilder.append(uid);
            stringBuilder.append(", timeNow=");
            stringBuilder.append(timeNow);
            Slog.d(str, stringBuilder.toString());
        }
        synchronized (this) {
            Pair<MutableLong, String> entry = (Pair) this.mTempWhitelistAppIdEndTimes.get(uid);
            if (entry == null) {
            } else if (timeNow >= ((MutableLong) entry.first).value) {
                this.mTempWhitelistAppIdEndTimes.delete(uid);
                onAppRemovedFromTempWhitelistLocked(uid, (String) entry.second);
            } else {
                if (DEBUG) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Time to remove UID ");
                    stringBuilder2.append(uid);
                    stringBuilder2.append(": ");
                    stringBuilder2.append(((MutableLong) entry.first).value);
                    Slog.d(str2, stringBuilder2.toString());
                }
                postTempActiveTimeoutMessage(uid, ((MutableLong) entry.first).value - timeNow);
            }
        }
    }

    @GuardedBy("this")
    private void onAppRemovedFromTempWhitelistLocked(int appId, String reason) {
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Removing appId ");
            stringBuilder.append(appId);
            stringBuilder.append(" from temp whitelist");
            Slog.d(str, stringBuilder.toString());
        }
        updateTempWhitelistAppIdsLocked(appId, false);
        this.mHandler.obtainMessage(9, appId, 0).sendToTarget();
        reportTempWhitelistChangedLocked();
        try {
            this.mBatteryStats.noteEvent(16401, reason, appId);
        } catch (RemoteException e) {
        }
    }

    public void exitIdleInternal(String reason) {
        synchronized (this) {
            becomeActiveLocked(reason, Binder.getCallingUid());
        }
    }

    /* JADX WARNING: Missing block: B:26:0x0046, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void updateConnectivityState(Intent connIntent) {
        ConnectivityService cm;
        synchronized (this) {
            cm = this.mConnectivityService;
        }
        if (cm != null) {
            NetworkInfo ni = cm.getActiveNetworkInfo();
            synchronized (this) {
                boolean conn;
                if (ni == null) {
                    conn = false;
                } else if (connIntent == null) {
                    conn = ni.isConnected();
                } else {
                    if (ni.getType() != connIntent.getIntExtra("networkType", -1)) {
                        return;
                    }
                    conn = connIntent.getBooleanExtra("noConnectivity", false) ^ 1;
                }
                if (conn != this.mNetworkConnected) {
                    this.mNetworkConnected = conn;
                    if (conn && this.mLightState == 5) {
                        stepLightIdleStateLocked("network");
                    }
                }
            }
        }
    }

    void updateInteractivityLocked() {
        boolean screenOn = this.mPowerManager.isInteractive();
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("updateInteractivityLocked: screenOn=");
            stringBuilder.append(screenOn);
            Slog.d(str, stringBuilder.toString());
        }
        if (!screenOn && this.mScreenOn) {
            this.mScreenOn = false;
            if (!this.mForceIdle) {
                becomeInactiveIfAppropriateLocked();
            }
        } else if (screenOn) {
            this.mScreenOn = true;
            if (!this.mForceIdle) {
                if (!this.mScreenLocked || !this.mConstants.WAIT_FOR_UNLOCK) {
                    becomeActiveLocked("screen", Process.myUid());
                }
            }
        }
    }

    void updateChargingLocked(boolean charging) {
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("updateChargingLocked: charging=");
            stringBuilder.append(charging);
            Slog.i(str, stringBuilder.toString());
        }
        if (!charging && this.mCharging) {
            this.mCharging = false;
            if (!this.mForceIdle) {
                becomeInactiveIfAppropriateLocked();
            }
        } else if (charging) {
            this.mCharging = charging;
            if (!this.mForceIdle) {
                becomeActiveLocked("charging", Process.myUid());
            }
        }
    }

    void keyguardShowingLocked(boolean showing) {
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("keyguardShowing=");
            stringBuilder.append(showing);
            Slog.i(str, stringBuilder.toString());
        }
        if (this.mScreenLocked != showing) {
            this.mScreenLocked = showing;
            if (this.mScreenOn && !this.mForceIdle && !this.mScreenLocked) {
                becomeActiveLocked("unlocked", Process.myUid());
            }
        }
    }

    void scheduleReportActiveLocked(String activeReason, int activeUid) {
        this.mHandler.sendMessage(this.mHandler.obtainMessage(5, activeUid, 0, activeReason));
    }

    void becomeActiveLocked(String activeReason, int activeUid) {
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("becomeActiveLocked, reason = ");
            stringBuilder.append(activeReason);
            Slog.i(str, stringBuilder.toString());
        }
        if (this.mState != 0 || this.mLightState != 0) {
            EventLogTags.writeDeviceIdle(0, activeReason);
            EventLogTags.writeDeviceIdleLight(0, activeReason);
            scheduleReportActiveLocked(activeReason, activeUid);
            this.mState = 0;
            this.mLightState = 0;
            this.mInactiveTimeout = this.mConstants.INACTIVE_TIMEOUT;
            this.mCurIdleBudget = 0;
            this.mMaintenanceStartTime = 0;
            resetIdleManagementLocked();
            resetLightIdleManagementLocked();
            addEvent(1, activeReason);
        }
    }

    void becomeInactiveIfAppropriateLocked() {
        if (DEBUG) {
            Slog.d(TAG, "becomeInactiveIfAppropriateLocked()");
        }
        if ((!this.mScreenOn && !this.mCharging) || this.mForceIdle) {
            if (this.mState == 0 && this.mDeepEnabled) {
                this.mState = 1;
                if (DEBUG) {
                    Slog.d(TAG, "Moved from STATE_ACTIVE to STATE_INACTIVE");
                }
                resetIdleManagementLocked();
                scheduleAlarmLocked(this.mInactiveTimeout, false);
                EventLogTags.writeDeviceIdle(this.mState, "no activity");
            }
            if (this.mLightState == 0 && this.mLightEnabled) {
                this.mLightState = 1;
                if (DEBUG) {
                    Slog.d(TAG, "Moved from LIGHT_STATE_ACTIVE to LIGHT_STATE_INACTIVE");
                }
                resetLightIdleManagementLocked();
                scheduleLightAlarmLocked(this.mConstants.LIGHT_IDLE_AFTER_INACTIVE_TIMEOUT);
                EventLogTags.writeDeviceIdleLight(this.mLightState, "no activity");
            }
        }
    }

    void resetIdleManagementLocked() {
        this.mNextIdlePendingDelay = 0;
        this.mNextIdleDelay = 0;
        if (this.mLightState == 0) {
            this.mNextLightIdleDelay = 0;
        }
        cancelAlarmLocked();
        cancelSensingTimeoutAlarmLocked();
        cancelLocatingLocked();
        stopMonitoringMotionLocked();
        this.mAnyMotionDetector.stop();
    }

    void resetLightIdleManagementLocked() {
        cancelLightAlarmLocked();
    }

    void exitForceIdleLocked() {
        if (this.mForceIdle) {
            this.mForceIdle = false;
            if (this.mScreenOn || this.mCharging) {
                becomeActiveLocked("exit-force", Process.myUid());
            }
        }
    }

    void stepLightIdleStateLocked(String reason) {
        if (this.mLightState != 7) {
            if (DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("stepLightIdleStateLocked: mLightState=");
                stringBuilder.append(this.mLightState);
                Slog.d(str, stringBuilder.toString());
            }
            EventLogTags.writeDeviceIdleLightStep();
            int i = this.mLightState;
            if (i != 1) {
                switch (i) {
                    case 3:
                    case 6:
                        break;
                    case 4:
                    case 5:
                        if (!this.mNetworkConnected && this.mLightState != 5) {
                            scheduleLightAlarmLocked(this.mNextLightIdleDelay);
                            if (DEBUG) {
                                Slog.d(TAG, "Moved to LIGHT_WAITING_FOR_NETWORK.");
                            }
                            this.mLightState = 5;
                            EventLogTags.writeDeviceIdleLight(this.mLightState, reason);
                            break;
                        }
                        this.mActiveIdleOpCount = 1;
                        this.mActiveIdleWakeLock.acquire();
                        this.mMaintenanceStartTime = SystemClock.elapsedRealtime();
                        if (this.mCurIdleBudget < this.mConstants.LIGHT_IDLE_MAINTENANCE_MIN_BUDGET) {
                            this.mCurIdleBudget = this.mConstants.LIGHT_IDLE_MAINTENANCE_MIN_BUDGET;
                        } else if (this.mCurIdleBudget > this.mConstants.LIGHT_IDLE_MAINTENANCE_MAX_BUDGET) {
                            this.mCurIdleBudget = this.mConstants.LIGHT_IDLE_MAINTENANCE_MAX_BUDGET;
                        }
                        scheduleLightAlarmLocked(this.mCurIdleBudget);
                        if (DEBUG) {
                            Slog.d(TAG, "Moved from LIGHT_STATE_IDLE to LIGHT_STATE_IDLE_MAINTENANCE.");
                        }
                        this.mLightState = 6;
                        EventLogTags.writeDeviceIdleLight(this.mLightState, reason);
                        addEvent(3, null);
                        this.mHandler.sendEmptyMessage(4);
                        break;
                }
            }
            this.mCurIdleBudget = this.mConstants.LIGHT_IDLE_MAINTENANCE_MIN_BUDGET;
            this.mNextLightIdleDelay = this.mConstants.LIGHT_IDLE_TIMEOUT;
            this.mMaintenanceStartTime = 0;
            if (!isOpsInactiveLocked()) {
                this.mLightState = 3;
                EventLogTags.writeDeviceIdleLight(this.mLightState, reason);
                scheduleLightAlarmLocked(this.mConstants.LIGHT_PRE_IDLE_TIMEOUT);
            }
            if (this.mMaintenanceStartTime != 0) {
                long duration = SystemClock.elapsedRealtime() - this.mMaintenanceStartTime;
                if (duration < this.mConstants.LIGHT_IDLE_MAINTENANCE_MIN_BUDGET) {
                    this.mCurIdleBudget += this.mConstants.LIGHT_IDLE_MAINTENANCE_MIN_BUDGET - duration;
                } else {
                    this.mCurIdleBudget -= duration - this.mConstants.LIGHT_IDLE_MAINTENANCE_MIN_BUDGET;
                }
            }
            this.mMaintenanceStartTime = 0;
            scheduleLightAlarmLocked(this.mNextLightIdleDelay);
            this.mNextLightIdleDelay = Math.min(this.mConstants.LIGHT_MAX_IDLE_TIMEOUT, (long) (((float) this.mNextLightIdleDelay) * this.mConstants.LIGHT_IDLE_FACTOR));
            if (this.mNextLightIdleDelay < this.mConstants.LIGHT_IDLE_TIMEOUT) {
                this.mNextLightIdleDelay = this.mConstants.LIGHT_IDLE_TIMEOUT;
            }
            if (DEBUG) {
                Slog.d(TAG, "Moved to LIGHT_STATE_IDLE.");
            }
            this.mLightState = 4;
            EventLogTags.writeDeviceIdleLight(this.mLightState, reason);
            addEvent(2, null);
            this.mGoingIdleWakeLock.acquire();
            this.mHandler.sendEmptyMessage(3);
        }
    }

    /* JADX WARNING: Missing block: B:39:0x0139, code skipped:
            if (r0.mLocating != false) goto L_0x0228;
     */
    /* JADX WARNING: Missing block: B:40:0x013d, code skipped:
            cancelAlarmLocked();
            cancelLocatingLocked();
            r0.mAnyMotionDetector.stop();
     */
    /* JADX WARNING: Missing block: B:41:0x0148, code skipped:
            scheduleAlarmLocked(r0.mNextIdleDelay, true);
     */
    /* JADX WARNING: Missing block: B:42:0x014f, code skipped:
            if (DEBUG == false) goto L_0x016e;
     */
    /* JADX WARNING: Missing block: B:43:0x0151, code skipped:
            r4 = TAG;
            r9 = new java.lang.StringBuilder();
            r9.append("Moved to STATE_IDLE. Next alarm in ");
            r9.append(r0.mNextIdleDelay);
            r9.append(" ms.");
            android.util.Slog.d(r4, r9.toString());
     */
    /* JADX WARNING: Missing block: B:44:0x016e, code skipped:
            r0.mNextIdleDelay = (long) (((float) r0.mNextIdleDelay) * r0.mConstants.IDLE_FACTOR);
     */
    /* JADX WARNING: Missing block: B:45:0x017b, code skipped:
            if (DEBUG == false) goto L_0x0195;
     */
    /* JADX WARNING: Missing block: B:46:0x017d, code skipped:
            r4 = TAG;
            r9 = new java.lang.StringBuilder();
            r9.append("Setting mNextIdleDelay = ");
            r9.append(r0.mNextIdleDelay);
            android.util.Slog.d(r4, r9.toString());
     */
    /* JADX WARNING: Missing block: B:47:0x0195, code skipped:
            r0.mNextIdleDelay = java.lang.Math.min(r0.mNextIdleDelay, r0.mConstants.MAX_IDLE_TIMEOUT);
     */
    /* JADX WARNING: Missing block: B:48:0x01a9, code skipped:
            if (r0.mNextIdleDelay >= r0.mConstants.IDLE_TIMEOUT) goto L_0x01b1;
     */
    /* JADX WARNING: Missing block: B:49:0x01ab, code skipped:
            r0.mNextIdleDelay = r0.mConstants.IDLE_TIMEOUT;
     */
    /* JADX WARNING: Missing block: B:50:0x01b1, code skipped:
            r0.mState = 5;
     */
    /* JADX WARNING: Missing block: B:51:0x01b6, code skipped:
            if (r0.mLightState == 7) goto L_0x01bd;
     */
    /* JADX WARNING: Missing block: B:52:0x01b8, code skipped:
            r0.mLightState = 7;
            cancelLightAlarmLocked();
     */
    /* JADX WARNING: Missing block: B:53:0x01bd, code skipped:
            com.android.server.EventLogTags.writeDeviceIdle(r0.mState, r1);
            addEvent(4, null);
            r0.mGoingIdleWakeLock.acquire();
            r0.mHandler.sendEmptyMessage(2);
     */
    /* JADX WARNING: Missing block: B:64:0x0228, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void stepIdleStateLocked(String reason) {
        String str = reason;
        if (DEBUG) {
            String str2 = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("stepIdleStateLocked: mState=");
            stringBuilder.append(this.mState);
            Slog.d(str2, stringBuilder.toString());
        }
        EventLogTags.writeDeviceIdleStep();
        if (this.mConstants.MIN_TIME_TO_ALARM + SystemClock.elapsedRealtime() <= this.mAlarmManager.getNextWakeFromIdleTime()) {
            switch (this.mState) {
                case 1:
                    startMonitoringMotionLocked();
                    scheduleAlarmLocked(this.mConstants.IDLE_AFTER_INACTIVE_TIMEOUT, false);
                    this.mNextIdlePendingDelay = this.mConstants.IDLE_PENDING_TIMEOUT;
                    this.mNextIdleDelay = this.mConstants.IDLE_TIMEOUT;
                    this.mState = 2;
                    if (DEBUG) {
                        Slog.d(TAG, "Moved from STATE_INACTIVE to STATE_IDLE_PENDING.");
                    }
                    EventLogTags.writeDeviceIdle(this.mState, str);
                    break;
                case 2:
                    this.mState = 3;
                    if (DEBUG) {
                        Slog.d(TAG, "Moved from STATE_IDLE_PENDING to STATE_SENSING.");
                    }
                    EventLogTags.writeDeviceIdle(this.mState, str);
                    scheduleSensingTimeoutAlarmLocked(this.mConstants.SENSING_TIMEOUT);
                    cancelLocatingLocked();
                    this.mNotMoving = false;
                    this.mLocated = false;
                    this.mLastGenericLocation = null;
                    this.mLastGpsLocation = null;
                    if (!this.mForceIdle) {
                        this.mAnyMotionDetector.checkForAnyMotion();
                        break;
                    }
                    break;
                case 3:
                    cancelSensingTimeoutAlarmLocked();
                    this.mState = 4;
                    if (DEBUG) {
                        Slog.d(TAG, "Moved from STATE_SENSING to STATE_LOCATING.");
                    }
                    EventLogTags.writeDeviceIdle(this.mState, str);
                    scheduleAlarmLocked(this.mConstants.LOCATING_TIMEOUT, false);
                    if (this.mForceIdle) {
                        if (DEBUG) {
                            Slog.d(TAG, "forceidle, not check locating");
                            break;
                        }
                    }
                    if (this.mLocationManager == null || this.mLocationManager.getProvider("network") == null) {
                        this.mHasNetworkLocation = false;
                    } else {
                        this.mLocationManager.requestLocationUpdates(this.mLocationRequest, this.mGenericLocationListener, this.mHandler.getLooper());
                        this.mLocating = true;
                    }
                    if (this.mLocationManager == null || this.mLocationManager.getProvider("gps") == null) {
                        this.mHasGps = false;
                    } else {
                        this.mHasGps = true;
                        this.mLocationManager.requestLocationUpdates("gps", 1000, 5.0f, this.mGpsLocationListener, this.mHandler.getLooper());
                        this.mLocating = true;
                    }
                    break;
                    break;
                case 4:
                    break;
                case 5:
                    this.mActiveIdleOpCount = 1;
                    this.mActiveIdleWakeLock.acquire();
                    scheduleAlarmLocked(this.mNextIdlePendingDelay, false);
                    if (DEBUG) {
                        String str3 = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Moved from STATE_IDLE to STATE_IDLE_MAINTENANCE. Next alarm in ");
                        stringBuilder2.append(this.mNextIdlePendingDelay);
                        stringBuilder2.append(" ms.");
                        Slog.d(str3, stringBuilder2.toString());
                    }
                    this.mMaintenanceStartTime = SystemClock.elapsedRealtime();
                    this.mNextIdlePendingDelay = Math.min(this.mConstants.MAX_IDLE_PENDING_TIMEOUT, (long) (((float) this.mNextIdlePendingDelay) * this.mConstants.IDLE_PENDING_FACTOR));
                    if (this.mNextIdlePendingDelay < this.mConstants.IDLE_PENDING_TIMEOUT) {
                        this.mNextIdlePendingDelay = this.mConstants.IDLE_PENDING_TIMEOUT;
                    }
                    this.mState = 6;
                    EventLogTags.writeDeviceIdle(this.mState, str);
                    addEvent(5, null);
                    this.mHandler.sendEmptyMessage(4);
                    break;
                case 6:
                    break;
            }
        }
        if (this.mState != 0) {
            becomeActiveLocked("alarm", Process.myUid());
            becomeInactiveIfAppropriateLocked();
        }
    }

    void incActiveIdleOps() {
        synchronized (this) {
            this.mActiveIdleOpCount++;
        }
    }

    void decActiveIdleOps() {
        synchronized (this) {
            this.mActiveIdleOpCount--;
            if (this.mActiveIdleOpCount <= 0) {
                exitMaintenanceEarlyIfNeededLocked();
                this.mActiveIdleWakeLock.release();
            }
        }
    }

    void setJobsActive(boolean active) {
        synchronized (this) {
            this.mJobsActive = active;
            reportMaintenanceActivityIfNeededLocked();
            if (!active) {
                exitMaintenanceEarlyIfNeededLocked();
            }
        }
    }

    void setAlarmsActive(boolean active) {
        synchronized (this) {
            this.mAlarmsActive = active;
            if (!active) {
                exitMaintenanceEarlyIfNeededLocked();
            }
        }
    }

    boolean registerMaintenanceActivityListener(IMaintenanceActivityListener listener) {
        boolean z;
        synchronized (this) {
            this.mMaintenanceActivityListeners.register(listener);
            z = this.mReportedMaintenanceActivity;
        }
        return z;
    }

    void unregisterMaintenanceActivityListener(IMaintenanceActivityListener listener) {
        synchronized (this) {
            this.mMaintenanceActivityListeners.unregister(listener);
        }
    }

    void reportMaintenanceActivityIfNeededLocked() {
        boolean active = this.mJobsActive;
        if (active != this.mReportedMaintenanceActivity) {
            this.mReportedMaintenanceActivity = active;
            this.mHandler.sendMessage(this.mHandler.obtainMessage(7, this.mReportedMaintenanceActivity, 0));
        }
    }

    boolean isOpsInactiveLocked() {
        return (this.mActiveIdleOpCount > 0 || this.mJobsActive || this.mAlarmsActive) ? false : true;
    }

    void exitMaintenanceEarlyIfNeededLocked() {
        if ((this.mState == 6 || this.mLightState == 6 || this.mLightState == 3) && isOpsInactiveLocked()) {
            long now = SystemClock.elapsedRealtime();
            if (DEBUG) {
                StringBuilder sb = new StringBuilder();
                sb.append("Exit: start=");
                TimeUtils.formatDuration(this.mMaintenanceStartTime, sb);
                sb.append(" now=");
                TimeUtils.formatDuration(now, sb);
                Slog.d(TAG, sb.toString());
            }
            if (this.mState == 6) {
                stepIdleStateLocked("s:early");
            } else if (this.mLightState == 3) {
                stepLightIdleStateLocked("s:predone");
            } else {
                stepLightIdleStateLocked("s:early");
            }
        }
    }

    void motionLocked() {
        if (DEBUG) {
            Slog.d(TAG, "motionLocked()");
        }
        handleMotionDetectedLocked(this.mConstants.MOTION_INACTIVE_TIMEOUT, "motion");
    }

    void handleMotionDetectedLocked(long timeout, String type) {
        boolean becomeInactive = false;
        if (this.mState != 0) {
            boolean lightIdle = this.mLightState == 4 || this.mLightState == 5 || this.mLightState == 6;
            if (!lightIdle) {
                scheduleReportActiveLocked(type, Process.myUid());
                addEvent(1, type);
            }
            this.mState = 0;
            this.mInactiveTimeout = timeout;
            this.mCurIdleBudget = 0;
            this.mMaintenanceStartTime = 0;
            EventLogTags.writeDeviceIdle(this.mState, type);
            becomeInactive = true;
        }
        if (this.mLightState == 7) {
            this.mLightState = 0;
            EventLogTags.writeDeviceIdleLight(this.mLightState, type);
            becomeInactive = true;
        }
        if (becomeInactive) {
            becomeInactiveIfAppropriateLocked();
        }
    }

    void receivedGenericLocationLocked(Location location) {
        if (this.mState != 4) {
            cancelLocatingLocked();
            return;
        }
        if (DEBUG) {
            Slog.d(TAG, "Generic location: ");
        }
        this.mLastGenericLocation = new Location(location);
        if (location.getAccuracy() <= this.mConstants.LOCATION_ACCURACY || !this.mHasGps) {
            this.mLocated = true;
            if (this.mNotMoving) {
                stepIdleStateLocked("s:location");
            }
        }
    }

    void receivedGpsLocationLocked(Location location) {
        if (this.mState != 4) {
            cancelLocatingLocked();
            return;
        }
        if (DEBUG) {
            Slog.d(TAG, "GPS location: ");
        }
        this.mLastGpsLocation = new Location(location);
        if (location.getAccuracy() <= this.mConstants.LOCATION_ACCURACY) {
            this.mLocated = true;
            if (this.mNotMoving) {
                stepIdleStateLocked("s:gps");
            }
        }
    }

    void startMonitoringMotionLocked() {
        if (DEBUG) {
            Slog.d(TAG, "startMonitoringMotionLocked()");
        }
        if (this.mMotionSensor != null && !this.mMotionListener.active) {
            this.mMotionListener.registerLocked();
        }
    }

    void stopMonitoringMotionLocked() {
        if (DEBUG) {
            Slog.d(TAG, "stopMonitoringMotionLocked()");
        }
        if (this.mMotionSensor != null && this.mMotionListener.active) {
            this.mMotionListener.unregisterLocked();
        }
    }

    void cancelAlarmLocked() {
        if (this.mNextAlarmTime != 0) {
            this.mNextAlarmTime = 0;
            this.mAlarmManager.cancel(this.mDeepAlarmListener);
        }
    }

    void cancelLightAlarmLocked() {
        if (this.mNextLightAlarmTime != 0) {
            this.mNextLightAlarmTime = 0;
            this.mAlarmManager.cancel(this.mLightAlarmListener);
        }
    }

    void cancelLocatingLocked() {
        if (this.mLocating) {
            this.mLocationManager.removeUpdates(this.mGenericLocationListener);
            this.mLocationManager.removeUpdates(this.mGpsLocationListener);
            this.mLocating = false;
        }
    }

    void cancelSensingTimeoutAlarmLocked() {
        if (this.mNextSensingTimeoutAlarmTime != 0) {
            this.mNextSensingTimeoutAlarmTime = 0;
            this.mAlarmManager.cancel(this.mSensingTimeoutAlarmListener);
        }
    }

    void scheduleAlarmLocked(long delay, boolean idleUntil) {
        long j = delay;
        boolean z = idleUntil;
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("scheduleAlarmLocked(");
            stringBuilder.append(j);
            stringBuilder.append(", ");
            stringBuilder.append(z);
            stringBuilder.append(")");
            Slog.d(str, stringBuilder.toString());
        }
        if (this.mMotionSensor != null) {
            this.mNextAlarmTime = SystemClock.elapsedRealtime() + j;
            if (z) {
                this.mAlarmManager.setIdleUntil(2, this.mNextAlarmTime, "DeviceIdleController.deep", this.mDeepAlarmListener, this.mHandler);
            } else {
                AlarmManager alarmManager = this.mAlarmManager;
                long j2 = this.mNextAlarmTime;
                long j3 = j2;
                alarmManager.set(2, j3, "DeviceIdleController.deep", this.mDeepAlarmListener, this.mHandler);
            }
        }
    }

    void scheduleLightAlarmLocked(long delay) {
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("scheduleLightAlarmLocked(");
            stringBuilder.append(delay);
            stringBuilder.append(")");
            Slog.d(str, stringBuilder.toString());
        }
        this.mNextLightAlarmTime = SystemClock.elapsedRealtime() + delay;
        this.mAlarmManager.set(2, this.mNextLightAlarmTime, "DeviceIdleController.light", this.mLightAlarmListener, this.mHandler);
    }

    void scheduleSensingTimeoutAlarmLocked(long delay) {
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("scheduleSensingAlarmLocked(");
            stringBuilder.append(delay);
            stringBuilder.append(")");
            Slog.d(str, stringBuilder.toString());
        }
        this.mNextSensingTimeoutAlarmTime = SystemClock.elapsedRealtime() + delay;
        this.mAlarmManager.set(2, this.mNextSensingTimeoutAlarmTime, "DeviceIdleController.sensing", this.mSensingTimeoutAlarmListener, this.mHandler);
    }

    private static int[] buildAppIdArray(ArrayMap<String, Integer> systemApps, ArrayMap<String, Integer> userApps, SparseBooleanArray outAppIds) {
        int i;
        outAppIds.clear();
        int i2 = 0;
        if (systemApps != null) {
            for (i = 0; i < systemApps.size(); i++) {
                outAppIds.put(((Integer) systemApps.valueAt(i)).intValue(), true);
            }
        }
        if (userApps != null) {
            for (i = 0; i < userApps.size(); i++) {
                outAppIds.put(((Integer) userApps.valueAt(i)).intValue(), true);
            }
        }
        int size = outAppIds.size();
        int[] appids = new int[size];
        while (i2 < size) {
            appids[i2] = outAppIds.keyAt(i2);
            i2++;
        }
        return appids;
    }

    private void updateWhitelistAppIdsLocked() {
        this.mPowerSaveWhitelistExceptIdleAppIdArray = buildAppIdArray(this.mPowerSaveWhitelistAppsExceptIdle, this.mPowerSaveWhitelistUserApps, this.mPowerSaveWhitelistExceptIdleAppIds);
        this.mPowerSaveWhitelistAllAppIdArray = buildAppIdArray(this.mPowerSaveWhitelistApps, this.mPowerSaveWhitelistUserApps, this.mPowerSaveWhitelistAllAppIds);
        this.mPowerSaveWhitelistUserAppIdArray = buildAppIdArray(null, this.mPowerSaveWhitelistUserApps, this.mPowerSaveWhitelistUserAppIds);
        if (this.mLocalActivityManager != null) {
            this.mLocalActivityManager.setDeviceIdleWhitelist(this.mPowerSaveWhitelistAllAppIdArray, this.mPowerSaveWhitelistExceptIdleAppIdArray);
        }
        if (this.mLocalPowerManager != null) {
            if (DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Setting wakelock whitelist to ");
                stringBuilder.append(Arrays.toString(this.mPowerSaveWhitelistAllAppIdArray));
                Slog.d(str, stringBuilder.toString());
            }
            this.mLocalPowerManager.setDeviceIdleWhitelist(this.mPowerSaveWhitelistAllAppIdArray);
        }
        passWhiteListsToForceAppStandbyTrackerLocked();
    }

    private void updateTempWhitelistAppIdsLocked(int appId, boolean adding) {
        String str;
        StringBuilder stringBuilder;
        int size = this.mTempWhitelistAppIdEndTimes.size();
        if (this.mTempWhitelistAppIdArray.length != size) {
            this.mTempWhitelistAppIdArray = new int[size];
        }
        for (int i = 0; i < size; i++) {
            this.mTempWhitelistAppIdArray[i] = this.mTempWhitelistAppIdEndTimes.keyAt(i);
        }
        if (this.mLocalActivityManager != null) {
            if (DEBUG) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Setting activity manager temp whitelist to ");
                stringBuilder.append(Arrays.toString(this.mTempWhitelistAppIdArray));
                Slog.d(str, stringBuilder.toString());
            }
            this.mLocalActivityManager.updateDeviceIdleTempWhitelist(this.mTempWhitelistAppIdArray, appId, adding);
        }
        if (this.mLocalPowerManager != null) {
            if (DEBUG) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Setting wakelock temp whitelist to ");
                stringBuilder.append(Arrays.toString(this.mTempWhitelistAppIdArray));
                Slog.d(str, stringBuilder.toString());
            }
            this.mLocalPowerManager.setDeviceIdleTempWhitelist(this.mTempWhitelistAppIdArray);
        }
        passWhiteListsToForceAppStandbyTrackerLocked();
    }

    private void reportPowerSaveWhitelistChangedLocked() {
        Intent intent = new Intent("android.os.action.POWER_SAVE_WHITELIST_CHANGED");
        intent.addFlags(1073741824);
        getContext().sendBroadcastAsUser(intent, UserHandle.SYSTEM);
    }

    private void reportTempWhitelistChangedLocked() {
        Intent intent = new Intent("android.os.action.POWER_SAVE_TEMP_WHITELIST_CHANGED");
        intent.addFlags(1073741824);
        getContext().sendBroadcastAsUser(intent, UserHandle.SYSTEM);
    }

    private void passWhiteListsToForceAppStandbyTrackerLocked() {
        this.mAppStateTracker.setPowerSaveWhitelistAppIds(this.mPowerSaveWhitelistExceptIdleAppIdArray, this.mPowerSaveWhitelistUserAppIdArray, this.mTempWhitelistAppIdArray);
    }

    void readConfigFileLocked() {
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Reading config from ");
            stringBuilder.append(this.mConfigFile.getBaseFile());
            Slog.d(str, stringBuilder.toString());
        }
        this.mPowerSaveWhitelistUserApps.clear();
        try {
            FileInputStream stream = this.mConfigFile.openRead();
            try {
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(stream, StandardCharsets.UTF_8.name());
                readConfigFileLocked(parser);
                try {
                    stream.close();
                } catch (IOException e) {
                }
            } catch (XmlPullParserException e2) {
                stream.close();
            } catch (Throwable th) {
                try {
                    stream.close();
                } catch (IOException e3) {
                }
                throw th;
            }
        } catch (FileNotFoundException e4) {
        }
    }

    private boolean updateWhitelistFromDB(boolean ignoreDbNotExist) {
        PackageManager pm = getContext().getPackageManager();
        Bundle bundle = null;
        ArrayList<String> protectlist = null;
        int i = 0;
        try {
            Slog.d(TAG, "begin to read protectlist from DB");
            bundle = getContext().getContentResolver().call(Uri.parse("content://com.huawei.android.smartpowerprovider"), "hsm_get_freeze_list", "protect", null);
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("read protectlist fail:");
            stringBuilder.append(e);
            Slog.d(str, stringBuilder.toString());
            if (!ignoreDbNotExist) {
                return false;
            }
        }
        if (bundle != null) {
            protectlist = bundle.getStringArrayList("frz_protect");
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("protect list: ");
            stringBuilder2.append(protectlist);
            Slog.d(str2, stringBuilder2.toString());
        } else {
            Slog.d(TAG, "read protectlist wrong , Bundle is null");
        }
        HashSet<String> protectPkgsExt = new HashSet<String>() {
            {
                add(PackageManagerService.PLATFORM_PACKAGE_NAME);
                add("com.android.phone");
                add("org.simalliance.openmobileapi.service");
                add("com.android.cellbroadcastreceiver");
                add("com.android.providers.media");
                add("com.android.exchange");
                add("com.android.providers.downloads");
                add("com.facebook.services");
                add("com.google.android.tetheringentitlement");
                add("com.google.android.ims");
            }
        };
        List<PackageInfo> packages = pm.getInstalledPackages(8192);
        synchronized (this) {
            while (i < packages.size()) {
                String pkgName = ((PackageInfo) packages.get(i)).packageName;
                if (!this.mPowerSaveWhitelistUserApps.containsKey(pkgName)) {
                    try {
                        ApplicationInfo ai = pm.getApplicationInfo(pkgName, 8192);
                        if ((protectlist != null && protectlist.contains(pkgName)) || protectPkgsExt.contains(pkgName)) {
                            this.mPowerSaveWhitelistUserApps.put(ai.packageName, Integer.valueOf(UserHandle.getAppId(ai.uid)));
                        }
                    } catch (NameNotFoundException e2) {
                        String str3 = TAG;
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("NameNotFound: ");
                        stringBuilder3.append(pkgName);
                        Slog.d(str3, stringBuilder3.toString());
                    }
                }
                i++;
            }
            updateWhitelistAppIdsLocked();
            reportPowerSaveWhitelistChangedLocked();
        }
        return true;
    }

    /* JADX WARNING: Removed duplicated region for block: B:44:0x00bc A:{Catch:{ IllegalStateException -> 0x013e, NullPointerException -> 0x0126, NumberFormatException -> 0x010e, XmlPullParserException -> 0x00f6, IOException -> 0x00de, IndexOutOfBoundsException -> 0x00c5 }} */
    /* JADX WARNING: Removed duplicated region for block: B:7:0x0016 A:{Catch:{ IllegalStateException -> 0x013e, NullPointerException -> 0x0126, NumberFormatException -> 0x010e, XmlPullParserException -> 0x00f6, IOException -> 0x00de, IndexOutOfBoundsException -> 0x00c5 }} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void readConfigFileLocked(XmlPullParser parser) {
        int type;
        String str;
        StringBuilder stringBuilder;
        PackageManager pm = getContext().getPackageManager();
        while (true) {
            try {
                int next = parser.next();
                type = next;
                if (next == 2 || type == 1) {
                    if (type != 2) {
                        next = parser.getDepth();
                        while (true) {
                            int next2 = parser.next();
                            type = next2;
                            if (next2 == 1) {
                                return;
                            }
                            if (type != 3 || parser.getDepth() > next) {
                                if (type != 3) {
                                    if (type != 4) {
                                        String tagName = parser.getName();
                                        Object obj = -1;
                                        int hashCode = tagName.hashCode();
                                        if (hashCode != 3797) {
                                            if (hashCode == 111376009) {
                                                if (tagName.equals("un-wl")) {
                                                    obj = 1;
                                                }
                                            }
                                        } else if (tagName.equals("wl")) {
                                            obj = null;
                                        }
                                        String name;
                                        switch (obj) {
                                            case null:
                                                name = parser.getAttributeValue(null, "n");
                                                if (name != null) {
                                                    try {
                                                        ApplicationInfo ai = pm.getApplicationInfo(name, DumpState.DUMP_CHANGES);
                                                        this.mPowerSaveWhitelistUserApps.put(ai.packageName, Integer.valueOf(UserHandle.getAppId(ai.uid)));
                                                        break;
                                                    } catch (NameNotFoundException e) {
                                                        break;
                                                    }
                                                }
                                                break;
                                            case 1:
                                                name = parser.getAttributeValue(null, "n");
                                                if (this.mPowerSaveWhitelistApps.containsKey(name)) {
                                                    this.mRemovedFromSystemWhitelistApps.put(name, (Integer) this.mPowerSaveWhitelistApps.remove(name));
                                                    break;
                                                }
                                                break;
                                            default:
                                                name = TAG;
                                                StringBuilder stringBuilder2 = new StringBuilder();
                                                stringBuilder2.append("Unknown element under <config>: ");
                                                stringBuilder2.append(parser.getName());
                                                Slog.w(name, stringBuilder2.toString());
                                                XmlUtils.skipCurrentTag(parser);
                                                break;
                                        }
                                    }
                                }
                            } else {
                                return;
                            }
                        }
                    }
                    throw new IllegalStateException("no start tag found");
                }
            } catch (IllegalStateException e2) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Failed parsing config ");
                stringBuilder.append(e2);
                Slog.w(str, stringBuilder.toString());
                return;
            } catch (NullPointerException e3) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Failed parsing config ");
                stringBuilder.append(e3);
                Slog.w(str, stringBuilder.toString());
                return;
            } catch (NumberFormatException e4) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Failed parsing config ");
                stringBuilder.append(e4);
                Slog.w(str, stringBuilder.toString());
                return;
            } catch (XmlPullParserException e5) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Failed parsing config ");
                stringBuilder.append(e5);
                Slog.w(str, stringBuilder.toString());
                return;
            } catch (IOException e6) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Failed parsing config ");
                stringBuilder.append(e6);
                Slog.w(str, stringBuilder.toString());
                return;
            } catch (IndexOutOfBoundsException e7) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Failed parsing config ");
                stringBuilder.append(e7);
                Slog.w(str, stringBuilder.toString());
                return;
            }
        }
        if (type != 2) {
        }
    }

    void writeConfigFileLocked() {
        this.mHandler.removeMessages(1);
        this.mHandler.sendEmptyMessageDelayed(1, 5000);
    }

    void handleWriteConfigFile() {
        ByteArrayOutputStream memStream = new ByteArrayOutputStream();
        try {
            synchronized (this) {
                XmlSerializer out = new FastXmlSerializer();
                out.setOutput(memStream, StandardCharsets.UTF_8.name());
                writeConfigFileLocked(out);
            }
        } catch (IOException e) {
        }
        synchronized (this.mConfigFile) {
            FileOutputStream stream = null;
            try {
                stream = this.mConfigFile.startWrite();
                memStream.writeTo(stream);
                stream.flush();
                FileUtils.sync(stream);
                stream.close();
                this.mConfigFile.finishWrite(stream);
            } catch (IOException e2) {
                Slog.w(TAG, "Error writing config file", e2);
                this.mConfigFile.failWrite(stream);
            }
        }
    }

    void writeConfigFileLocked(XmlSerializer out) throws IOException {
        out.startDocument(null, Boolean.valueOf(true));
        out.startTag(null, "config");
        int i = 0;
        for (int i2 = 0; i2 < this.mPowerSaveWhitelistUserApps.size(); i2++) {
            String name = (String) this.mPowerSaveWhitelistUserApps.keyAt(i2);
            out.startTag(null, "wl");
            out.attribute(null, "n", name);
            out.endTag(null, "wl");
        }
        while (i < this.mRemovedFromSystemWhitelistApps.size()) {
            out.startTag(null, "un-wl");
            out.attribute(null, "n", (String) this.mRemovedFromSystemWhitelistApps.keyAt(i));
            out.endTag(null, "un-wl");
            i++;
        }
        out.endTag(null, "config");
        out.endDocument();
    }

    static void dumpHelp(PrintWriter pw) {
        pw.println("Device idle controller (deviceidle) commands:");
        pw.println("  help");
        pw.println("    Print this help text.");
        pw.println("  step [light|deep]");
        pw.println("    Immediately step to next state, without waiting for alarm.");
        pw.println("  force-idle [light|deep]");
        pw.println("    Force directly into idle mode, regardless of other device state.");
        pw.println("  force-inactive");
        pw.println("    Force to be inactive, ready to freely step idle states.");
        pw.println("  unforce");
        pw.println("    Resume normal functioning after force-idle or force-inactive.");
        pw.println("  get [light|deep|force|screen|charging|network]");
        pw.println("    Retrieve the current given state.");
        pw.println("  disable [light|deep|all]");
        pw.println("    Completely disable device idle mode.");
        pw.println("  enable [light|deep|all]");
        pw.println("    Re-enable device idle mode after it had previously been disabled.");
        pw.println("  enabled [light|deep|all]");
        pw.println("    Print 1 if device idle mode is currently enabled, else 0.");
        pw.println("  whitelist");
        pw.println("    Print currently whitelisted apps.");
        pw.println("  whitelist [package ...]");
        pw.println("    Add (prefix with +) or remove (prefix with -) packages.");
        pw.println("  sys-whitelist [package ...|reset]");
        pw.println("    Prefix the package with '-' to remove it from the system whitelist or '+' to put it back in the system whitelist.");
        pw.println("    Note that only packages that were earlier removed from the system whitelist can be added back.");
        pw.println("    reset will reset the whitelist to the original state");
        pw.println("    Prints the system whitelist if no arguments are specified");
        pw.println("  except-idle-whitelist [package ...|reset]");
        pw.println("    Prefix the package with '+' to add it to whitelist or '=' to check if it is already whitelisted");
        pw.println("    [reset] will reset the whitelist to it's original state");
        pw.println("    Note that unlike <whitelist> cmd, changes made using this won't be persisted across boots");
        pw.println("  tempwhitelist");
        pw.println("    Print packages that are temporarily whitelisted.");
        pw.println("  tempwhitelist [-u USER] [-d DURATION] [-r] [package]");
        pw.println("    Temporarily place package in whitelist for DURATION milliseconds.");
        pw.println("    If no DURATION is specified, 10 seconds is used");
        pw.println("    If [-r] option is used, then the package is removed from temp whitelist and any [-d] is ignored");
        pw.println("  motion");
        pw.println("    Simulate a motion event to bring the device out of deep doze");
    }

    /* JADX WARNING: Removed duplicated region for block: B:201:0x02fe A:{Catch:{ all -> 0x02d7, all -> 0x0347 }} */
    /* JADX WARNING: Removed duplicated region for block: B:203:0x0308 A:{Catch:{ all -> 0x02d7, all -> 0x0347 }} */
    /* JADX WARNING: Removed duplicated region for block: B:209:0x0328 A:{Catch:{ all -> 0x02d7, all -> 0x0347 }} */
    /* JADX WARNING: Removed duplicated region for block: B:245:0x03a2 A:{Catch:{ all -> 0x037b, all -> 0x03d0 }} */
    /* JADX WARNING: Removed duplicated region for block: B:247:0x03ac A:{Catch:{ all -> 0x037b, all -> 0x03d0 }} */
    /* JADX WARNING: Removed duplicated region for block: B:249:0x03b1 A:{Catch:{ all -> 0x037b, all -> 0x03d0 }} */
    /* JADX WARNING: Missing block: B:328:?, code skipped:
            r0 = new java.lang.StringBuilder();
            r0.append("Package must be prefixed with +, -, or =: ");
            r0.append(r6);
            r10.println(r0.toString());
     */
    /* JADX WARNING: Missing block: B:329:0x0508, code skipped:
            android.os.Binder.restoreCallingIdentity(r13);
     */
    /* JADX WARNING: Missing block: B:330:0x050c, code skipped:
            return -1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    int onShellCommand(Shell shell, String cmd) {
        Exception e;
        Shell shell2 = shell;
        String str = cmd;
        PrintWriter pw = shell.getOutPrintWriter();
        long token;
        String arg;
        StringBuilder stringBuilder;
        long token2;
        String arg2;
        int curLightState;
        boolean z;
        String arg3;
        StringBuilder stringBuilder2;
        if ("step".equals(str)) {
            getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            synchronized (this) {
                try {
                    token = Binder.clearCallingIdentity();
                    arg = shell.getNextArg();
                    if (arg != null) {
                        if (!"deep".equals(arg)) {
                            if ("light".equals(arg)) {
                                stepLightIdleStateLocked("s:shell");
                                pw.print("Stepped to light: ");
                                pw.println(lightStateToString(this.mLightState));
                            } else {
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("Unknown idle mode: ");
                                stringBuilder.append(arg);
                                pw.println(stringBuilder.toString());
                            }
                            Binder.restoreCallingIdentity(token);
                        }
                    }
                    stepIdleStateLocked("s:shell");
                    pw.print("Stepped to deep: ");
                    pw.println(stateToString(this.mState));
                    Binder.restoreCallingIdentity(token);
                } catch (Throwable th) {
                    throw th;
                }
            }
        } else if ("force-idle".equals(str)) {
            getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            synchronized (this) {
                try {
                    token2 = Binder.clearCallingIdentity();
                    arg2 = shell.getNextArg();
                    if (arg2 != null) {
                        if (!"deep".equals(arg2)) {
                            if ("light".equals(arg2)) {
                                this.mForceIdle = true;
                                becomeInactiveIfAppropriateLocked();
                                for (curLightState = this.mLightState; curLightState != 4; curLightState = this.mLightState) {
                                    stepLightIdleStateLocked("s:shell");
                                    if (curLightState == this.mLightState) {
                                        pw.print("Unable to go light idle; stopped at ");
                                        pw.println(lightStateToString(this.mLightState));
                                        exitForceIdleLocked();
                                        Binder.restoreCallingIdentity(token2);
                                        return -1;
                                    }
                                }
                                pw.println("Now forced in to light idle mode");
                            } else {
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("Unknown idle mode: ");
                                stringBuilder.append(arg2);
                                pw.println(stringBuilder.toString());
                            }
                            Binder.restoreCallingIdentity(token2);
                        }
                    }
                    if (this.mDeepEnabled) {
                        this.mForceIdle = true;
                        becomeInactiveIfAppropriateLocked();
                        for (curLightState = this.mState; curLightState != 5; curLightState = this.mState) {
                            stepIdleStateLocked("s:shell");
                            if (curLightState == this.mState) {
                                pw.print("Unable to go deep idle; stopped at ");
                                pw.println(stateToString(this.mState));
                                exitForceIdleLocked();
                                Binder.restoreCallingIdentity(token2);
                                return -1;
                            }
                        }
                        pw.println("Now forced in to deep idle mode");
                        Binder.restoreCallingIdentity(token2);
                    } else {
                        pw.println("Unable to go deep idle; not enabled");
                        Binder.restoreCallingIdentity(token2);
                        return -1;
                    }
                } catch (Throwable th2) {
                    throw th2;
                }
            }
        } else if ("force-inactive".equals(str)) {
            getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            synchronized (this) {
                try {
                    token = Binder.clearCallingIdentity();
                    this.mForceIdle = true;
                    becomeInactiveIfAppropriateLocked();
                    pw.print("Light state: ");
                    pw.print(lightStateToString(this.mLightState));
                    pw.print(", deep state: ");
                    pw.println(stateToString(this.mState));
                    Binder.restoreCallingIdentity(token);
                } catch (Throwable th22) {
                    throw th22;
                }
            }
        } else if ("unforce".equals(str)) {
            getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            synchronized (this) {
                try {
                    token = Binder.clearCallingIdentity();
                    exitForceIdleLocked();
                    pw.print("Light state: ");
                    pw.print(lightStateToString(this.mLightState));
                    pw.print(", deep state: ");
                    pw.println(stateToString(this.mState));
                    Binder.restoreCallingIdentity(token);
                } catch (Throwable th222) {
                    throw th222;
                }
            }
        } else if ("get".equals(str)) {
            getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            synchronized (this) {
                try {
                    arg2 = shell.getNextArg();
                    if (arg2 != null) {
                        token2 = Binder.clearCallingIdentity();
                        switch (arg2.hashCode()) {
                            case -907689876:
                                if (arg2.equals("screen")) {
                                    z = true;
                                    break;
                                }
                            case 3079404:
                                if (arg2.equals("deep")) {
                                    z = true;
                                    break;
                                }
                            case 97618667:
                                if (arg2.equals("force")) {
                                    z = true;
                                    break;
                                }
                            case 102970646:
                                if (arg2.equals("light")) {
                                    z = false;
                                    break;
                                }
                            case 1436115569:
                                if (arg2.equals("charging")) {
                                    z = true;
                                    break;
                                }
                            case 1843485230:
                                if (arg2.equals("network")) {
                                    z = true;
                                    break;
                                }
                            default:
                        }
                        z = true;
                        switch (z) {
                            case false:
                                pw.println(lightStateToString(this.mLightState));
                                break;
                            case true:
                                pw.println(stateToString(this.mState));
                                break;
                            case true:
                                pw.println(this.mForceIdle);
                                break;
                            case true:
                                pw.println(this.mScreenOn);
                                break;
                            case true:
                                pw.println(this.mCharging);
                                break;
                            case true:
                                pw.println(this.mNetworkConnected);
                                break;
                            default:
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("Unknown get option: ");
                                stringBuilder.append(arg2);
                                pw.println(stringBuilder.toString());
                                break;
                        }
                        Binder.restoreCallingIdentity(token2);
                    } else {
                        pw.println("Argument required");
                    }
                } catch (Throwable th2222) {
                    throw th2222;
                }
            }
        } else if ("disable".equals(str)) {
            getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            synchronized (this) {
                try {
                    token = Binder.clearCallingIdentity();
                    arg = shell.getNextArg();
                    z = false;
                    boolean valid = false;
                    if (arg != null) {
                        if (!"deep".equals(arg)) {
                            StringBuilder stringBuilder3;
                            if ("all".equals(arg)) {
                            }
                            if (arg == null || "light".equals(arg) || "all".equals(arg)) {
                                valid = true;
                                if (this.mLightEnabled) {
                                    this.mLightEnabled = false;
                                    z = true;
                                    pw.println("Light idle mode disabled");
                                }
                            }
                            if (z) {
                                stringBuilder3 = new StringBuilder();
                                stringBuilder3.append(arg == null ? "all" : arg);
                                stringBuilder3.append("-disabled");
                                becomeActiveLocked(stringBuilder3.toString(), Process.myUid());
                            }
                            if (!valid) {
                                stringBuilder3 = new StringBuilder();
                                stringBuilder3.append("Unknown idle mode: ");
                                stringBuilder3.append(arg);
                                pw.println(stringBuilder3.toString());
                            }
                            Binder.restoreCallingIdentity(token);
                        }
                    }
                    valid = true;
                    if (this.mDeepEnabled) {
                        this.mDeepEnabled = false;
                        z = true;
                        pw.println("Deep idle mode disabled");
                    }
                    valid = true;
                    if (this.mLightEnabled) {
                    }
                    if (z) {
                    }
                    if (valid) {
                    }
                    Binder.restoreCallingIdentity(token);
                } catch (Throwable th22222) {
                    throw th22222;
                }
            }
        } else if ("enable".equals(str)) {
            getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            synchronized (this) {
                try {
                    token = Binder.clearCallingIdentity();
                    arg = shell.getNextArg();
                    z = false;
                    boolean valid2 = false;
                    if (arg != null) {
                        if (!"deep".equals(arg)) {
                            if ("all".equals(arg)) {
                            }
                            if (arg == null || "light".equals(arg) || "all".equals(arg)) {
                                valid2 = true;
                                if (!this.mLightEnabled) {
                                    this.mLightEnabled = true;
                                    z = true;
                                    pw.println("Light idle mode enable");
                                }
                            }
                            if (z) {
                                becomeInactiveIfAppropriateLocked();
                            }
                            if (!valid2) {
                                StringBuilder stringBuilder4 = new StringBuilder();
                                stringBuilder4.append("Unknown idle mode: ");
                                stringBuilder4.append(arg);
                                pw.println(stringBuilder4.toString());
                            }
                            Binder.restoreCallingIdentity(token);
                        }
                    }
                    valid2 = true;
                    if (!this.mDeepEnabled) {
                        this.mDeepEnabled = true;
                        z = true;
                        pw.println("Deep idle mode enabled");
                    }
                    valid2 = true;
                    if (this.mLightEnabled) {
                    }
                    if (z) {
                    }
                    if (valid2) {
                    }
                    Binder.restoreCallingIdentity(token);
                } catch (Throwable th222222) {
                    throw th222222;
                }
            }
        } else if ("enabled".equals(str)) {
            synchronized (this) {
                try {
                    arg3 = shell.getNextArg();
                    if (arg3 != null) {
                        if (!"all".equals(arg3)) {
                            if ("deep".equals(arg3)) {
                                pw.println(this.mDeepEnabled ? "1" : Integer.valueOf(0));
                            } else if ("light".equals(arg3)) {
                                pw.println(this.mLightEnabled ? "1" : Integer.valueOf(0));
                            } else {
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("Unknown idle mode: ");
                                stringBuilder2.append(arg3);
                                pw.println(stringBuilder2.toString());
                            }
                        }
                    }
                    Object valueOf = (this.mDeepEnabled && this.mLightEnabled) ? "1" : Integer.valueOf(0);
                    pw.println(valueOf);
                } catch (Throwable th2222222) {
                    throw th2222222;
                }
            }
        } else {
            char c = '=';
            String arg4;
            long token3;
            char op;
            if ("whitelist".equals(str)) {
                arg4 = shell.getNextArg();
                if (arg4 != null) {
                    getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
                    long token4 = Binder.clearCallingIdentity();
                    while (true) {
                        token3 = token4;
                        try {
                            if (arg4.length() >= 1) {
                                if (arg4.charAt(0) != '-' && arg4.charAt(0) != '+' && arg4.charAt(0) != c) {
                                    break;
                                }
                                op = arg4.charAt(0);
                                arg2 = arg4.substring(1);
                                StringBuilder stringBuilder5;
                                if (op == '+') {
                                    if (addPowerSaveWhitelistAppInternal(arg2)) {
                                        StringBuilder stringBuilder6 = new StringBuilder();
                                        stringBuilder6.append("Added: ");
                                        stringBuilder6.append(arg2);
                                        pw.println(stringBuilder6.toString());
                                    } else {
                                        stringBuilder5 = new StringBuilder();
                                        stringBuilder5.append("Unknown package: ");
                                        stringBuilder5.append(arg2);
                                        pw.println(stringBuilder5.toString());
                                    }
                                } else if (op != '-') {
                                    pw.println(getPowerSaveWhitelistAppInternal(arg2));
                                } else if (removePowerSaveWhitelistAppInternal(arg2)) {
                                    stringBuilder5 = new StringBuilder();
                                    stringBuilder5.append("Removed: ");
                                    stringBuilder5.append(arg2);
                                    pw.println(stringBuilder5.toString());
                                }
                                arg3 = shell.getNextArg();
                                arg4 = arg3;
                                if (arg3 == null) {
                                    Binder.restoreCallingIdentity(token3);
                                    break;
                                }
                                token4 = token3;
                                c = '=';
                            }
                        } catch (Throwable th22222222) {
                            Binder.restoreCallingIdentity(token3);
                            throw th22222222;
                        }
                    }
                }
                synchronized (this) {
                    curLightState = 0;
                    while (curLightState < this.mPowerSaveWhitelistAppsExceptIdle.size()) {
                        try {
                            pw.print("system-excidle,");
                            pw.print((String) this.mPowerSaveWhitelistAppsExceptIdle.keyAt(curLightState));
                            pw.print(",");
                            pw.println(this.mPowerSaveWhitelistAppsExceptIdle.valueAt(curLightState));
                            curLightState++;
                        } catch (Throwable th222222222) {
                            throw th222222222;
                        }
                    }
                    for (curLightState = 0; curLightState < this.mPowerSaveWhitelistApps.size(); curLightState++) {
                        pw.print("system,");
                        pw.print((String) this.mPowerSaveWhitelistApps.keyAt(curLightState));
                        pw.print(",");
                        pw.println(this.mPowerSaveWhitelistApps.valueAt(curLightState));
                    }
                    for (curLightState = 0; curLightState < this.mPowerSaveWhitelistUserApps.size(); curLightState++) {
                        pw.print("user,");
                        pw.print((String) this.mPowerSaveWhitelistUserApps.keyAt(curLightState));
                        pw.print(",");
                        pw.println(this.mPowerSaveWhitelistUserApps.valueAt(curLightState));
                    }
                }
            } else if ("tempwhitelist".equals(str)) {
                token3 = JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY;
                z = false;
                while (true) {
                    boolean removePkg = z;
                    arg3 = shell.getNextOption();
                    arg4 = arg3;
                    if (arg3 != null) {
                        if ("-u".equals(arg4)) {
                            arg3 = shell.getNextArg();
                            if (arg3 == null) {
                                pw.println("-u requires a user number");
                                return -1;
                            }
                            shell2.userId = Integer.parseInt(arg3);
                        } else if ("-d".equals(arg4)) {
                            arg3 = shell.getNextArg();
                            if (arg3 == null) {
                                pw.println("-d requires a duration");
                                return -1;
                            }
                            token3 = Long.parseLong(arg3);
                        } else if ("-r".equals(arg4)) {
                            z = true;
                        }
                        z = removePkg;
                    } else {
                        String arg5 = shell.getNextArg();
                        String str2;
                        String str3;
                        if (arg5 == null) {
                            str2 = arg4;
                            if (removePkg) {
                                pw.println("[-r] requires a package name");
                                return -1;
                            }
                            dumpTempWhitelistSchedule(pw, false);
                        } else if (removePkg) {
                            try {
                                removePowerSaveTempWhitelistAppChecked(arg5, shell2.userId);
                                str3 = arg5;
                                str2 = arg4;
                            } catch (Exception e2) {
                                e = e2;
                                str3 = arg5;
                                str2 = arg4;
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("Failed: ");
                                stringBuilder2.append(e);
                                pw.println(stringBuilder2.toString());
                                return -1;
                            }
                        } else {
                            try {
                                try {
                                    addPowerSaveTempWhitelistAppChecked(arg5, token3, shell2.userId, "shell");
                                } catch (Exception e3) {
                                    e = e3;
                                }
                            } catch (Exception e4) {
                                e = e4;
                                str3 = arg5;
                                str2 = arg4;
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("Failed: ");
                                stringBuilder2.append(e);
                                pw.println(stringBuilder2.toString());
                                return -1;
                            }
                        }
                    }
                }
            } else if ("except-idle-whitelist".equals(str)) {
                getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
                token = Binder.clearCallingIdentity();
                try {
                    arg3 = shell.getNextArg();
                    if (arg3 == null) {
                        pw.println("No arguments given");
                        Binder.restoreCallingIdentity(token);
                        return -1;
                    }
                    if ("reset".equals(arg3)) {
                        resetPowerSaveWhitelistExceptIdleInternal();
                    } else {
                        StringBuilder stringBuilder7;
                        while (arg3.length() >= 1) {
                            if (arg3.charAt(0) != '-' && arg3.charAt(0) != '+' && arg3.charAt(0) != '=') {
                                break;
                            }
                            char op2 = arg3.charAt(0);
                            String pkg = arg3.substring(1);
                            if (op2 == '+') {
                                StringBuilder stringBuilder8;
                                if (addPowerSaveWhitelistExceptIdleInternal(pkg)) {
                                    stringBuilder8 = new StringBuilder();
                                    stringBuilder8.append("Added: ");
                                    stringBuilder8.append(pkg);
                                    pw.println(stringBuilder8.toString());
                                } else {
                                    stringBuilder8 = new StringBuilder();
                                    stringBuilder8.append("Unknown package: ");
                                    stringBuilder8.append(pkg);
                                    pw.println(stringBuilder8.toString());
                                }
                            } else if (op2 == '=') {
                                pw.println(getPowerSaveWhitelistExceptIdleInternal(pkg));
                            } else {
                                stringBuilder7 = new StringBuilder();
                                stringBuilder7.append("Unknown argument: ");
                                stringBuilder7.append(arg3);
                                pw.println(stringBuilder7.toString());
                                Binder.restoreCallingIdentity(token);
                                return -1;
                            }
                            arg4 = shell.getNextArg();
                            arg3 = arg4;
                            if (arg4 == null) {
                            }
                        }
                        stringBuilder7 = new StringBuilder();
                        stringBuilder7.append("Package must be prefixed with +, -, or =: ");
                        stringBuilder7.append(arg3);
                        pw.println(stringBuilder7.toString());
                        Binder.restoreCallingIdentity(token);
                        return -1;
                    }
                    Binder.restoreCallingIdentity(token);
                } catch (Throwable th2222222222) {
                    Binder.restoreCallingIdentity(token);
                    throw th2222222222;
                }
            } else if ("sys-whitelist".equals(str)) {
                String arg6 = shell.getNextArg();
                if (arg6 != null) {
                    getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
                    token3 = Binder.clearCallingIdentity();
                    try {
                        if ("reset".equals(arg6)) {
                            resetSystemPowerWhitelistInternal();
                        } else {
                            while (arg6.length() >= 1) {
                                if (arg6.charAt(0) != '-' && arg6.charAt(0) != '+') {
                                    break;
                                }
                                op = arg6.charAt(0);
                                arg2 = arg6.substring(1);
                                StringBuilder stringBuilder9;
                                if (op != '+') {
                                    if (op == '-') {
                                        if (removeSystemPowerWhitelistAppInternal(arg2)) {
                                            stringBuilder9 = new StringBuilder();
                                            stringBuilder9.append("Removed ");
                                            stringBuilder9.append(arg2);
                                            pw.println(stringBuilder9.toString());
                                        }
                                    }
                                } else if (restoreSystemPowerWhitelistAppInternal(arg2)) {
                                    stringBuilder9 = new StringBuilder();
                                    stringBuilder9.append("Restored ");
                                    stringBuilder9.append(arg2);
                                    pw.println(stringBuilder9.toString());
                                }
                                arg3 = shell.getNextArg();
                                arg6 = arg3;
                                if (arg3 == null) {
                                }
                            }
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Package must be prefixed with + or - ");
                            stringBuilder.append(arg6);
                            pw.println(stringBuilder.toString());
                            Binder.restoreCallingIdentity(token3);
                            return -1;
                        }
                        Binder.restoreCallingIdentity(token3);
                    } catch (Throwable th22222222222) {
                        Binder.restoreCallingIdentity(token3);
                        throw th22222222222;
                    }
                }
                synchronized (this) {
                    curLightState = 0;
                    while (curLightState < this.mPowerSaveWhitelistApps.size()) {
                        try {
                            pw.print((String) this.mPowerSaveWhitelistApps.keyAt(curLightState));
                            pw.print(",");
                            pw.println(this.mPowerSaveWhitelistApps.valueAt(curLightState));
                            curLightState++;
                        } catch (Throwable th222222222222) {
                            throw th222222222222;
                        }
                    }
                }
            } else if (!"motion".equals(str)) {
                return shell.handleDefaultCommands(cmd);
            } else {
                getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
                synchronized (this) {
                    try {
                        token = Binder.clearCallingIdentity();
                        motionLocked();
                        pw.print("Light state: ");
                        pw.print(lightStateToString(this.mLightState));
                        pw.print(", deep state: ");
                        pw.println(stateToString(this.mState));
                        Binder.restoreCallingIdentity(token);
                    } catch (Throwable th2222222222222) {
                        throw th2222222222222;
                    }
                }
            }
        }
        return 0;
    }

    void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        PrintWriter printWriter = pw;
        Object obj = args;
        if (DumpUtils.checkDumpPermission(getContext(), TAG, printWriter)) {
            int userId;
            int i;
            int i2 = 0;
            if (obj != null) {
                userId = 0;
                i = 0;
                while (i < obj.length) {
                    String arg = obj[i];
                    if ("-h".equals(arg)) {
                        dumpHelp(pw);
                        return;
                    }
                    if ("-u".equals(arg)) {
                        i++;
                        if (i < obj.length) {
                            userId = Integer.parseInt(obj[i]);
                        }
                    } else if (!"-a".equals(arg)) {
                        if (arg.length() <= 0 || arg.charAt(0) != '-') {
                            Shell shell = new Shell();
                            shell.userId = userId;
                            String[] newArgs = new String[(obj.length - i)];
                            System.arraycopy(obj, i, newArgs, 0, obj.length - i);
                            shell.exec(this.mBinderService, null, fd, null, newArgs, null, new ResultReceiver(null));
                            return;
                        }
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Unknown option: ");
                        stringBuilder.append(arg);
                        printWriter.println(stringBuilder.toString());
                        return;
                    }
                    i++;
                }
            }
            synchronized (this) {
                this.mConstants.dump(printWriter);
                if (this.mEventCmds[0] != 0) {
                    printWriter.println("  Idling history:");
                    long now = SystemClock.elapsedRealtime();
                    for (int i3 = 99; i3 >= 0; i3--) {
                        if (this.mEventCmds[i3] != 0) {
                            String label;
                            switch (this.mEventCmds[i3]) {
                                case 1:
                                    label = "     normal";
                                    break;
                                case 2:
                                    label = " light-idle";
                                    break;
                                case 3:
                                    label = "light-maint";
                                    break;
                                case 4:
                                    label = "  deep-idle";
                                    break;
                                case 5:
                                    label = " deep-maint";
                                    break;
                                default:
                                    label = "         ??";
                                    break;
                            }
                            printWriter.print("    ");
                            printWriter.print(label);
                            printWriter.print(": ");
                            TimeUtils.formatDuration(this.mEventTimes[i3], now, printWriter);
                            if (this.mEventReasons[i3] != null) {
                                printWriter.print(" (");
                                printWriter.print(this.mEventReasons[i3]);
                                printWriter.print(")");
                            }
                            pw.println();
                        }
                    }
                }
                i = this.mPowerSaveWhitelistAppsExceptIdle.size();
                if (i > 0) {
                    printWriter.println("  Whitelist (except idle) system apps:");
                    for (userId = 0; userId < i; userId++) {
                        printWriter.print("    ");
                        printWriter.println((String) this.mPowerSaveWhitelistAppsExceptIdle.keyAt(userId));
                    }
                }
                i = this.mPowerSaveWhitelistApps.size();
                if (i > 0) {
                    printWriter.println("  Whitelist system apps:");
                    for (userId = 0; userId < i; userId++) {
                        printWriter.print("    ");
                        printWriter.println((String) this.mPowerSaveWhitelistApps.keyAt(userId));
                    }
                }
                i = this.mRemovedFromSystemWhitelistApps.size();
                if (i > 0) {
                    printWriter.println("  Removed from whitelist system apps:");
                    for (userId = 0; userId < i; userId++) {
                        printWriter.print("    ");
                        printWriter.println((String) this.mRemovedFromSystemWhitelistApps.keyAt(userId));
                    }
                }
                i = this.mPowerSaveWhitelistUserApps.size();
                if (i > 0) {
                    printWriter.println("  Whitelist user apps:");
                    for (userId = 0; userId < i; userId++) {
                        printWriter.print("    ");
                        printWriter.println((String) this.mPowerSaveWhitelistUserApps.keyAt(userId));
                    }
                }
                i = this.mPowerSaveWhitelistExceptIdleAppIds.size();
                if (i > 0) {
                    printWriter.println("  Whitelist (except idle) all app ids:");
                    for (userId = 0; userId < i; userId++) {
                        printWriter.print("    ");
                        printWriter.print(this.mPowerSaveWhitelistExceptIdleAppIds.keyAt(userId));
                        pw.println();
                    }
                }
                i = this.mPowerSaveWhitelistUserAppIds.size();
                if (i > 0) {
                    printWriter.println("  Whitelist user app ids:");
                    for (userId = 0; userId < i; userId++) {
                        printWriter.print("    ");
                        printWriter.print(this.mPowerSaveWhitelistUserAppIds.keyAt(userId));
                        pw.println();
                    }
                }
                i = this.mPowerSaveWhitelistAllAppIds.size();
                if (i > 0) {
                    printWriter.println("  Whitelist all app ids:");
                    for (userId = 0; userId < i; userId++) {
                        printWriter.print("    ");
                        printWriter.print(this.mPowerSaveWhitelistAllAppIds.keyAt(userId));
                        pw.println();
                    }
                }
                dumpTempWhitelistSchedule(printWriter, true);
                int size = this.mTempWhitelistAppIdArray != null ? this.mTempWhitelistAppIdArray.length : 0;
                if (size > 0) {
                    printWriter.println("  Temp whitelist app ids:");
                    while (i2 < size) {
                        printWriter.print("    ");
                        printWriter.print(this.mTempWhitelistAppIdArray[i2]);
                        pw.println();
                        i2++;
                    }
                }
                printWriter.print("  mLightEnabled=");
                printWriter.print(this.mLightEnabled);
                printWriter.print("  mDeepEnabled=");
                printWriter.println(this.mDeepEnabled);
                printWriter.print("  mForceIdle=");
                printWriter.println(this.mForceIdle);
                printWriter.print("  mMotionSensor=");
                printWriter.println(this.mMotionSensor);
                printWriter.print("  mScreenOn=");
                printWriter.println(this.mScreenOn);
                printWriter.print("  mScreenLocked=");
                printWriter.println(this.mScreenLocked);
                printWriter.print("  mNetworkConnected=");
                printWriter.println(this.mNetworkConnected);
                printWriter.print("  mCharging=");
                printWriter.println(this.mCharging);
                printWriter.print("  mMotionActive=");
                printWriter.println(this.mMotionListener.active);
                printWriter.print("  mNotMoving=");
                printWriter.println(this.mNotMoving);
                printWriter.print("  mLocating=");
                printWriter.print(this.mLocating);
                printWriter.print(" mHasGps=");
                printWriter.print(this.mHasGps);
                printWriter.print(" mHasNetwork=");
                printWriter.print(this.mHasNetworkLocation);
                printWriter.print(" mLocated=");
                printWriter.println(this.mLocated);
                if (this.mLastGenericLocation != null) {
                    printWriter.print("  mLastGenericLocation=");
                    printWriter.println(this.mLastGenericLocation);
                }
                if (this.mLastGpsLocation != null) {
                    printWriter.print("  mLastGpsLocation=");
                    printWriter.println(this.mLastGpsLocation);
                }
                printWriter.print("  mState=");
                printWriter.print(stateToString(this.mState));
                printWriter.print(" mLightState=");
                printWriter.println(lightStateToString(this.mLightState));
                printWriter.print("  mInactiveTimeout=");
                TimeUtils.formatDuration(this.mInactiveTimeout, printWriter);
                pw.println();
                if (this.mActiveIdleOpCount != 0) {
                    printWriter.print("  mActiveIdleOpCount=");
                    printWriter.println(this.mActiveIdleOpCount);
                }
                if (this.mNextAlarmTime != 0) {
                    printWriter.print("  mNextAlarmTime=");
                    TimeUtils.formatDuration(this.mNextAlarmTime, SystemClock.elapsedRealtime(), printWriter);
                    pw.println();
                }
                if (this.mNextIdlePendingDelay != 0) {
                    printWriter.print("  mNextIdlePendingDelay=");
                    TimeUtils.formatDuration(this.mNextIdlePendingDelay, printWriter);
                    pw.println();
                }
                if (this.mNextIdleDelay != 0) {
                    printWriter.print("  mNextIdleDelay=");
                    TimeUtils.formatDuration(this.mNextIdleDelay, printWriter);
                    pw.println();
                }
                if (this.mNextLightIdleDelay != 0) {
                    printWriter.print("  mNextIdleDelay=");
                    TimeUtils.formatDuration(this.mNextLightIdleDelay, printWriter);
                    pw.println();
                }
                if (this.mNextLightAlarmTime != 0) {
                    printWriter.print("  mNextLightAlarmTime=");
                    TimeUtils.formatDuration(this.mNextLightAlarmTime, SystemClock.elapsedRealtime(), printWriter);
                    pw.println();
                }
                if (this.mCurIdleBudget != 0) {
                    printWriter.print("  mCurIdleBudget=");
                    TimeUtils.formatDuration(this.mCurIdleBudget, printWriter);
                    pw.println();
                }
                if (this.mMaintenanceStartTime != 0) {
                    printWriter.print("  mMaintenanceStartTime=");
                    TimeUtils.formatDuration(this.mMaintenanceStartTime, SystemClock.elapsedRealtime(), printWriter);
                    pw.println();
                }
                if (this.mJobsActive) {
                    printWriter.print("  mJobsActive=");
                    printWriter.println(this.mJobsActive);
                }
                if (this.mAlarmsActive) {
                    printWriter.print("  mAlarmsActive=");
                    printWriter.println(this.mAlarmsActive);
                }
            }
        }
    }

    void dumpTempWhitelistSchedule(PrintWriter pw, boolean printTitle) {
        int size = this.mTempWhitelistAppIdEndTimes.size();
        if (size > 0) {
            String prefix = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
            if (printTitle) {
                pw.println("  Temp whitelist schedule:");
                prefix = "    ";
            }
            long timeNow = SystemClock.elapsedRealtime();
            for (int i = 0; i < size; i++) {
                pw.print(prefix);
                pw.print("UID=");
                pw.print(this.mTempWhitelistAppIdEndTimes.keyAt(i));
                pw.print(": ");
                Pair<MutableLong, String> entry = (Pair) this.mTempWhitelistAppIdEndTimes.valueAt(i);
                TimeUtils.formatDuration(((MutableLong) entry.first).value, timeNow, pw);
                pw.print(" - ");
                pw.println((String) entry.second);
            }
        }
    }

    public int forceIdleInternal() {
        synchronized (this) {
            if (!this.mDeepEnabled) {
                Slog.d(TAG, "Unable to go idle; not enabled");
                return -1;
            } else if (this.mForceIdle) {
                Slog.d(TAG, "now it is in ForceIdle by dump");
                return 0;
            } else {
                this.mForceIdle = true;
                becomeInactiveIfAppropriateLocked();
                int curState = this.mState;
                while (curState != 5) {
                    stepIdleStateLocked("s:shell");
                    if (curState == this.mState) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Unable to go idle; stopped at ");
                        stringBuilder.append(stateToString(this.mState));
                        Slog.d(str, stringBuilder.toString());
                        exitForceIdleLocked();
                        return -1;
                    }
                    curState = this.mState;
                }
                this.mForceIdle = false;
                Slog.d(TAG, "Now forced in to idle mode");
                return 0;
            }
        }
    }
}
