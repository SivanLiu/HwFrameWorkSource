package com.huawei.android.location.activityrecognition;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build.VERSION;
import android.os.Handler;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.Message;
import android.os.RemoteException;
import android.text.TextUtils;
import com.huawei.android.location.activityrecognition.IActivityRecognitionHardwareService.Stub;
import com.huawei.systemserver.activityrecognition.HwActivityChangedEvent;
import com.huawei.systemserver.activityrecognition.HwActivityChangedExtendEvent;
import com.huawei.systemserver.activityrecognition.HwActivityRecognitionEvent;
import com.huawei.systemserver.activityrecognition.HwActivityRecognitionExtendEvent;
import com.huawei.systemserver.activityrecognition.HwEnvironmentChangedEvent;
import com.huawei.systemserver.activityrecognition.IActivityRecognitionHardwareService;
import com.huawei.systemserver.activityrecognition.IActivityRecognitionHardwareSink;
import com.huawei.systemserver.activityrecognition.OtherParameters;
import java.util.ArrayList;
import java.util.List;

public class HwActivityRecognition {
    public static final String ACTIVITY_FAST_WALKING = "android.activity_recognition.fast_walking";
    public static final String ACTIVITY_IN_VEHICLE = "android.activity_recognition.in_vehicle";
    public static final String ACTIVITY_ON_BICYCLE = "android.activity_recognition.on_bicycle";
    public static final String ACTIVITY_RUNNING = "android.activity_recognition.running";
    public static final String ACTIVITY_STILL = "android.activity_recognition.still";
    public static final String ACTIVITY_TYPE_VE_HIGH_SPEED_RAIL = "android.activity_recognition.high_speed_rail";
    public static final String ACTIVITY_WALKING = "android.activity_recognition.walking";
    private static final String AIDL_MESSAGE_SERVICE_CLASS = "com.huawei.android.location.activityrecognition.ActivityRecognitionService";
    private static final String AIDL_MESSAGE_SERVICE_CLASS_O = "com.huawei.systemserver.activityrecognition.ActivityRecognitionService";
    private static final String AIDL_MESSAGE_SERVICE_PACKAGE = "com.huawei.android.location.activityrecognition";
    private static final String AIDL_MESSAGE_SERVICE_PACKAGE_O = "com.huawei.systemserver";
    private static final int ANDROID_O = 25;
    public static final String ENV_TYPE_HOME = "android.activity_recognition.env_home";
    public static final String ENV_TYPE_OFFICE = "android.activity_recognition.env_office";
    public static final String ENV_TYPE_WAY_HOME = "android.activity_recognition.env_way_home";
    public static final String ENV_TYPE_WAY_OFFICE = "android.activity_recognition.env_way_office";
    public static final int EVENT_TYPE_ENTER = 1;
    public static final int EVENT_TYPE_EXIT = 2;
    private static final int MSG_BIND = 0;
    private static final int MSG_RECONNECTION = 1;
    private static final String TAG = "ARMoudle.HwActivityRecognition";
    private static final int sdkVersion = VERSION.SDK_INT;
    private ServiceDeathHandler deathHandler;
    private int mConnectCount = 0;
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName arg0, IBinder service) {
            SDKLog.d(HwActivityRecognition.TAG, "Connection service ok");
            HwActivityRecognition.this.mHandler.removeMessages(1);
            if (HwActivityRecognition.sdkVersion < HwActivityRecognition.ANDROID_O) {
                HwActivityRecognition.this.mService = Stub.asInterface(service);
            } else {
                HwActivityRecognition.this.mService_O = IActivityRecognitionHardwareService.Stub.asInterface(service);
            }
            HwActivityRecognition.this.registerSink();
            HwActivityRecognition.this.notifyServiceDied();
            if (HwActivityRecognition.sdkVersion < HwActivityRecognition.ANDROID_O) {
                HwActivityRecognition.this.mHandler.sendEmptyMessage(0);
            } else {
                HwActivityRecognition.this.mServiceConnection.onServiceConnected();
            }
        }

        public void onServiceDisconnected(ComponentName name) {
            if (HwActivityRecognition.sdkVersion < HwActivityRecognition.ANDROID_O) {
                HwActivityRecognition.this.mService = null;
            } else {
                HwActivityRecognition.this.mService_O = null;
            }
            HwActivityRecognition.this.mServiceConnection.onServiceDisconnected();
        }
    };
    private Context mContext = null;
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    HwActivityRecognition.this.handleProviderLoad();
                    return;
                case 1:
                    HwActivityRecognition.this.bindService();
                    return;
                default:
                    return;
            }
        }
    };
    private IActivityRecognitionHardwareService mService = null;
    private HwActivityRecognitionServiceConnection mServiceConnection = null;
    private IActivityRecognitionHardwareService mService_O = null;
    private IActivityRecognitionHardwareSink mSink;
    private IActivityRecognitionHardwareSink mSink_O;
    private String packageName;

    static class ActivityEvent {
        private String activity;
        private int eventType;
        private OtherParameters otherParams;
        private long timestampNs;

        public String getActivity() {
            return this.activity;
        }

        public int getEventType() {
            return this.eventType;
        }

        public long getTimestampNs() {
            return this.timestampNs;
        }

        public OtherParameters getOtherParams() {
            return this.otherParams;
        }

        public ActivityEvent(String activity, int eventType, long timestampNs, OtherParameters otherParams) {
            this.activity = activity;
            this.eventType = eventType;
            this.timestampNs = timestampNs;
            this.otherParams = otherParams;
        }
    }

    private class ServiceDeathHandler implements DeathRecipient {
        private ServiceDeathHandler() {
        }

        public void binderDied() {
            SDKLog.d(HwActivityRecognition.TAG, "Ar service has died!");
            if (HwActivityRecognition.this.mServiceConnection != null) {
                HwActivityRecognition.this.mServiceConnection.onServiceDisconnected();
            }
            if (HwActivityRecognition.sdkVersion < HwActivityRecognition.ANDROID_O) {
                if (HwActivityRecognition.this.mService != null) {
                    HwActivityRecognition.this.mService.asBinder().unlinkToDeath(HwActivityRecognition.this.deathHandler, 0);
                    HwActivityRecognition.this.mService = null;
                }
            } else if (HwActivityRecognition.this.mService_O != null) {
                HwActivityRecognition.this.mService_O.asBinder().unlinkToDeath(HwActivityRecognition.this.deathHandler, 0);
                HwActivityRecognition.this.mService_O = null;
            }
        }
    }

    public HwActivityRecognition(Context context) {
        SDKLog.d(TAG, "HwActivityRecognition, android version:" + sdkVersion);
        if (context != null) {
            this.mContext = context;
            this.packageName = context.getPackageName();
            this.deathHandler = new ServiceDeathHandler();
        }
    }

    public boolean connectService(HwActivityRecognitionHardwareSink sink, HwActivityRecognitionServiceConnection connection) {
        SDKLog.d(TAG, "connectService");
        if (connection == null || sink == null) {
            SDKLog.e(TAG, "connection or sink is null.");
            return false;
        }
        this.mServiceConnection = connection;
        if (sdkVersion < ANDROID_O) {
            if (this.mService == null) {
                this.mSink = createActivityRecognitionHardwareSink(sink);
                bindService();
            }
        } else if (this.mService_O == null) {
            this.mSink_O = createActivityRecognitionHardwareSink_O(sink);
            bindService();
        }
        return true;
    }

    public boolean disconnectService() {
        SDKLog.d(TAG, "disconnectService");
        if (sdkVersion < ANDROID_O) {
            if (this.mService != null) {
                this.mService.asBinder().unlinkToDeath(this.deathHandler, 0);
            } else {
                SDKLog.e(TAG, "mService is null.");
                return false;
            }
        } else if (this.mService_O != null) {
            this.mService_O.asBinder().unlinkToDeath(this.deathHandler, 0);
        } else {
            SDKLog.e(TAG, "mService_O is null.");
            return false;
        }
        unregisterSink();
        this.mContext.unbindService(this.mConnection);
        this.mServiceConnection.onServiceDisconnected();
        if (sdkVersion < ANDROID_O) {
            this.mService = null;
        } else {
            this.mService_O = null;
        }
        this.mConnectCount = 0;
        this.mHandler.removeMessages(1);
        this.mHandler.removeMessages(0);
        return true;
    }

    private boolean registerSink() {
        if (sdkVersion < ANDROID_O) {
            return registerSink_N();
        }
        return registerSink_O();
    }

    private boolean registerSink_N() {
        boolean result = false;
        SDKLog.d(TAG, "registerSink_N");
        if (this.mService == null || this.mSink == null) {
            SDKLog.e(TAG, "mService or mSink is null.");
            return false;
        }
        try {
            result = this.mService.registerSink(this.mSink);
        } catch (RemoteException e) {
            SDKLog.e(TAG, "registerSink error:" + e.getMessage());
        }
        return result;
    }

    private boolean registerSink_O() {
        boolean result = false;
        SDKLog.d(TAG, "registerSink_O");
        if (this.mService_O == null || this.mSink_O == null) {
            SDKLog.e(TAG, "mService_O or mSink_O is null.");
            return false;
        }
        try {
            result = this.mService_O.registerSink(this.packageName, this.mSink_O);
        } catch (RemoteException e) {
            SDKLog.e(TAG, "registerSink error:" + e.getMessage());
        }
        return result;
    }

    private boolean unregisterSink() {
        if (sdkVersion < ANDROID_O) {
            return unregisterSink_N();
        }
        return unregisterSink_O();
    }

    private boolean unregisterSink_N() {
        boolean result = false;
        SDKLog.d(TAG, "unregisterSink_N");
        if (this.mService == null || this.mSink == null) {
            SDKLog.e(TAG, "mService or mSink is null.");
            return false;
        }
        try {
            result = this.mService.unregisterSink(this.mSink);
        } catch (RemoteException e) {
            SDKLog.e(TAG, "unregisterSink error:" + e.getMessage());
        }
        return result;
    }

    private boolean unregisterSink_O() {
        boolean result = false;
        SDKLog.d(TAG, "unregisterSink_O");
        if (this.mService_O == null || this.mSink_O == null) {
            SDKLog.e(TAG, "mService_O or mService_O is null.");
            return false;
        }
        try {
            result = this.mService_O.unregisterSink(this.packageName, this.mSink_O);
        } catch (RemoteException e) {
            SDKLog.e(TAG, "unregisterSink error:" + e.getMessage());
        }
        return result;
    }

    public int getSupportedModule() {
        SDKLog.d(TAG, "getSupportedModule");
        if (this.mService_O != null) {
            try {
                return this.mService_O.getSupportedModule();
            } catch (RemoteException e) {
                SDKLog.e(TAG, "getSupportedModule error:" + e.getMessage());
                return 0;
            }
        }
        SDKLog.e(TAG, "mService_O is null.");
        return 0;
    }

    public String[] getSupportedActivities() {
        if (sdkVersion < ANDROID_O) {
            return getSupportedActivities_N();
        }
        return getSupportedActivities_O();
    }

    private String[] getSupportedActivities_N() {
        SDKLog.d(TAG, "getSupportedActivities_N");
        if (this.mService != null) {
            try {
                return this.mService.getSupportedActivities();
            } catch (RemoteException e) {
                SDKLog.e(TAG, "getSupportedActivities error:" + e.getMessage());
                return new String[0];
            }
        }
        SDKLog.e(TAG, "mService is null.");
        return new String[0];
    }

    private String[] getSupportedActivities_O() {
        SDKLog.d(TAG, "getSupportedActivities_O");
        if (this.mService_O != null) {
            try {
                return this.mService_O.getSupportedActivities();
            } catch (RemoteException e) {
                SDKLog.e(TAG, "getSupportedActivities error:" + e.getMessage());
                return new String[0];
            }
        }
        SDKLog.e(TAG, "mService_O is null.");
        return new String[0];
    }

    public String[] getSupportedEnvironments() {
        SDKLog.d(TAG, "getSupportedEnvironments");
        if (this.mService_O != null) {
            try {
                return this.mService_O.getSupportedEnvironments();
            } catch (RemoteException e) {
                SDKLog.e(TAG, "getSupportedEnvironments error:" + e.getMessage());
                return new String[0];
            }
        }
        SDKLog.e(TAG, "mService_O is null.");
        return new String[0];
    }

    public boolean enableActivityEvent(String activity, int eventType, long reportLatencyNs) {
        if (sdkVersion < ANDROID_O) {
            return enableActivityEvent_N(activity, eventType, reportLatencyNs);
        }
        return enableActivityEvent_O(activity, eventType, reportLatencyNs);
    }

    public boolean enableActivityEvent_N(String activity, int eventType, long reportLatencyNs) {
        SDKLog.d(TAG, "enableActivityEvent");
        boolean result = false;
        if (!TextUtils.isEmpty(activity)) {
            if (reportLatencyNs >= 0) {
                SDKLog.d(TAG, new StringBuilder(String.valueOf(activity)).append(",").append(eventType).append(",").append(reportLatencyNs).toString());
                if (this.mService != null) {
                    try {
                        result = this.mService.enableActivityEvent(activity, eventType, reportLatencyNs);
                    } catch (RemoteException e) {
                        SDKLog.e(TAG, "enableActivityEvent error:" + e.getMessage());
                    }
                    return result;
                }
                SDKLog.e(TAG, "mService is null.");
                return false;
            }
        }
        SDKLog.e(TAG, "activity is null or reportLatencyNs < 0");
        return false;
    }

    public boolean enableActivityEvent_O(String activity, int eventType, long reportLatencyNs) {
        SDKLog.d(TAG, "enableActivityEvent");
        boolean result = false;
        if (!TextUtils.isEmpty(activity)) {
            if (reportLatencyNs >= 0) {
                SDKLog.d(TAG, new StringBuilder(String.valueOf(activity)).append(",").append(eventType).append(",").append(reportLatencyNs).toString());
                if (this.mService_O != null) {
                    try {
                        result = this.mService_O.enableActivityEvent(this.packageName, activity, eventType, reportLatencyNs);
                    } catch (RemoteException e) {
                        SDKLog.e(TAG, "enableActivityEvent error:" + e.getMessage());
                    }
                    return result;
                }
                SDKLog.e(TAG, "mService is null.");
                return false;
            }
        }
        SDKLog.e(TAG, "activity is null or reportLatencyNs < 0");
        return false;
    }

    public boolean enableActivityEvent(String activity, int eventType, long reportLatencyNs, OtherParameters params) {
        SDKLog.d(TAG, "enableActivityExtendEvent");
        if (!TextUtils.isEmpty(activity)) {
            boolean z;
            if (reportLatencyNs < 0) {
                z = true;
            } else {
                z = false;
            }
            if (!(z || params == null)) {
                SDKLog.d(TAG, new StringBuilder(String.valueOf(activity)).append(",").append(eventType).append(",").append(reportLatencyNs).append(",").append(params.toString()).toString());
                if (this.mService_O != null) {
                    boolean result = false;
                    try {
                        result = this.mService_O.enableActivityExtendEvent(this.packageName, activity, eventType, reportLatencyNs, tranferToOtherParameters_O(params));
                    } catch (RemoteException e) {
                        SDKLog.e(TAG, "enableActivityextendEvent error:" + e.getMessage());
                    }
                    SDKLog.d(TAG, "activityExtendEventEnable:" + result);
                    return result;
                }
                SDKLog.e(TAG, "mService is null.");
                return false;
            }
        }
        SDKLog.e(TAG, "activity is null or reportLatencyNs < 0 or params is null.");
        return false;
    }

    private OtherParameters tranferToOtherParameters_O(OtherParameters params) {
        if (params != null) {
            return new OtherParameters(params.getmParam1(), params.getmParam2(), params.getmParam3(), params.getmParam4(), params.getmParam5());
        }
        return null;
    }

    public boolean disableActivityEvent(String activity, int eventType) {
        if (sdkVersion < ANDROID_O) {
            return disableActivityEvent_N(activity, eventType);
        }
        return disableActivityEvent_O(activity, eventType);
    }

    private boolean disableActivityEvent_N(String activity, int eventType) {
        boolean result = false;
        SDKLog.d(TAG, "disableActivityEvent");
        if (TextUtils.isEmpty(activity)) {
            SDKLog.e(TAG, "activity is null.");
            return false;
        }
        SDKLog.d(TAG, new StringBuilder(String.valueOf(activity)).append(",").append(eventType).toString());
        if (this.mService != null) {
            try {
                result = this.mService.disableActivityEvent(activity, eventType);
            } catch (RemoteException e) {
                SDKLog.e(TAG, "disableActivityEvent error:" + e.getMessage());
            }
            return result;
        }
        SDKLog.e(TAG, "mService is null.");
        return false;
    }

    private boolean disableActivityEvent_O(String activity, int eventType) {
        boolean result = false;
        SDKLog.d(TAG, "disableActivityEvent");
        if (TextUtils.isEmpty(activity)) {
            SDKLog.e(TAG, "activity is null.");
            return false;
        }
        SDKLog.d(TAG, new StringBuilder(String.valueOf(activity)).append(",").append(eventType).toString());
        if (this.mService_O != null) {
            try {
                result = this.mService_O.disableActivityEvent(this.packageName, activity, eventType);
            } catch (RemoteException e) {
                SDKLog.e(TAG, "disableActivityEvent error:" + e.getMessage());
            }
            return result;
        }
        SDKLog.e(TAG, "mService is null.");
        return false;
    }

    public String getCurrentActivity() {
        SDKLog.d(TAG, "getCurrentActivity");
        String activity = "unknown";
        if (this.mService != null) {
            try {
                activity = this.mService.getCurrentActivity();
            } catch (RemoteException e) {
                SDKLog.e(TAG, "getCurrentActivity error:" + e.getMessage());
            }
            return activity;
        }
        SDKLog.e(TAG, "mService is null.");
        return activity;
    }

    public HwActivityChangedExtendEvent getCurrentActivityExtend() {
        SDKLog.d(TAG, "getCurrentActivityExtend");
        if (this.mService_O != null) {
            HwActivityChangedExtendEvent hwActivityEvent = null;
            try {
                hwActivityEvent = this.mService_O.getCurrentActivity();
            } catch (RemoteException e) {
                SDKLog.e(TAG, "getCurrentActivity error:" + e.getMessage());
            }
            SDKLog.d(TAG, "hwActivityEvent:" + hwActivityEvent);
            return tranferToHwActivityChangedExtendEvent(hwActivityEvent);
        }
        SDKLog.e(TAG, "mService is null.");
        return null;
    }

    public boolean initEnvironmentFunction(String environment, OtherParameters params) {
        SDKLog.d(TAG, "initEnvironmentFunction");
        if (TextUtils.isEmpty(environment) || params == null) {
            SDKLog.e(TAG, "environment or params is null.");
            return false;
        }
        SDKLog.d(TAG, new StringBuilder(String.valueOf(environment)).append(",").append(params.toString()).toString());
        if (this.mService_O != null) {
            boolean result = false;
            try {
                result = this.mService_O.initEnvironmentFunction(this.packageName, environment, tranferToOtherParameters_O(params));
            } catch (RemoteException e) {
                SDKLog.e(TAG, "initEnvironmentFunction error:" + e.getMessage());
            }
            SDKLog.d(TAG, "environmentInit:" + result);
            return result;
        }
        SDKLog.e(TAG, "mService is null.");
        return false;
    }

    public boolean exitEnvironmentFunction(String environment, OtherParameters params) {
        SDKLog.d(TAG, "exitEnvironmentFunction");
        if (TextUtils.isEmpty(environment) || params == null) {
            SDKLog.e(TAG, "environment or params is null.");
            return false;
        }
        SDKLog.d(TAG, new StringBuilder(String.valueOf(environment)).append(",").append(params.toString()).toString());
        if (this.mService_O != null) {
            boolean result = false;
            try {
                result = this.mService_O.exitEnvironmentFunction(this.packageName, environment, tranferToOtherParameters_O(params));
            } catch (RemoteException e) {
                SDKLog.e(TAG, "exitEnvironmentFunction error:" + e.getMessage());
            }
            SDKLog.d(TAG, "environmentExit:" + result);
            return result;
        }
        SDKLog.e(TAG, "mService is null.");
        return false;
    }

    public boolean enableEnvironmentEvent(String environment, int eventType, long reportLatencyNs, OtherParameters params) {
        SDKLog.d(TAG, "enableEnvironmentEvent");
        if (!TextUtils.isEmpty(environment)) {
            boolean z;
            if (reportLatencyNs < 0) {
                z = true;
            } else {
                z = false;
            }
            if (!(z || params == null)) {
                SDKLog.d(TAG, new StringBuilder(String.valueOf(environment)).append(",").append(eventType).append(",").append(reportLatencyNs).append(",").append(params.toString()).toString());
                if (this.mService_O != null) {
                    boolean result = false;
                    try {
                        result = this.mService_O.enableEnvironmentEvent(this.packageName, environment, eventType, reportLatencyNs, tranferToOtherParameters_O(params));
                    } catch (RemoteException e) {
                        SDKLog.e(TAG, "enableEnvironmentEvent error:" + e.getMessage());
                    }
                    SDKLog.d(TAG, "environmentEnable:" + result);
                    return result;
                }
                SDKLog.e(TAG, "mService is null.");
                return false;
            }
        }
        SDKLog.e(TAG, "environment is null.");
        return false;
    }

    public boolean disableEnvironmentEvent(String environment, int eventType) {
        SDKLog.d(TAG, "disableEnvironmentEvent");
        if (TextUtils.isEmpty(environment)) {
            SDKLog.e(TAG, "environment is null.");
            return false;
        }
        SDKLog.d(TAG, new StringBuilder(String.valueOf(environment)).append(",").append(eventType).toString());
        if (this.mService_O != null) {
            boolean result = false;
            try {
                result = this.mService_O.disableEnvironmentEvent(this.packageName, environment, eventType);
            } catch (RemoteException e) {
                SDKLog.e(TAG, "disableEnvironmentEvent error:" + e.getMessage());
            }
            SDKLog.d(TAG, "environmentDisable:" + result);
            return result;
        }
        SDKLog.e(TAG, "mService is null.");
        return false;
    }

    public HwEnvironmentChangedEvent getCurrentEnvironment() {
        SDKLog.d(TAG, "getCurrentEnvironment");
        if (this.mService_O != null) {
            HwEnvironmentChangedEvent hwEnvironmentEvent = null;
            try {
                hwEnvironmentEvent = this.mService_O.getCurrentEnvironment();
            } catch (RemoteException e) {
                SDKLog.e(TAG, "getCurrentEnvironment error:" + e.getMessage());
            }
            SDKLog.d(TAG, "hwEnvironmentEvent:" + hwEnvironmentEvent);
            return tranferToHwEnvironmentChangedEvent(hwEnvironmentEvent);
        }
        SDKLog.e(TAG, "mService is null.");
        return null;
    }

    public boolean flush() {
        if (sdkVersion < ANDROID_O) {
            return flush_N();
        }
        return flush_O();
    }

    private boolean flush_N() {
        boolean result = false;
        SDKLog.d(TAG, "flush");
        if (this.mService != null) {
            try {
                result = this.mService.flush();
            } catch (RemoteException e) {
                SDKLog.e(TAG, "flush error:" + e.getMessage());
            }
            return result;
        }
        SDKLog.e(TAG, "mService is null.");
        return false;
    }

    private boolean flush_O() {
        boolean result = false;
        SDKLog.d(TAG, "flush");
        if (this.mService_O != null) {
            try {
                result = this.mService_O.flush();
            } catch (RemoteException e) {
                SDKLog.e(TAG, "flush error:" + e.getMessage());
            }
            return result;
        }
        SDKLog.e(TAG, "mService is null.");
        return false;
    }

    private IActivityRecognitionHardwareSink createActivityRecognitionHardwareSink(final HwActivityRecognitionHardwareSink sink) {
        if (sink != null) {
            return new IActivityRecognitionHardwareSink.Stub() {
                public void onActivityChanged(HwActivityChangedEvent event) throws RemoteException {
                    sink.onActivityChanged(event);
                }
            };
        }
        return null;
    }

    private HwActivityChangedEvent tranferToHwActivityChangedEvent(HwActivityChangedEvent event) {
        if (event == null) {
            return null;
        }
        List<ActivityEvent> events = new ArrayList();
        for (HwActivityRecognitionEvent e : event.getActivityRecognitionEvents()) {
            events.add(new ActivityEvent(e.getActivity(), e.getEventType(), e.getTimestampNs(), null));
        }
        HwActivityRecognitionEvent[] activityRecognitionEventArray = new HwActivityRecognitionEvent[events.size()];
        for (int j = 0; j < events.size(); j++) {
            ActivityEvent arEvent = (ActivityEvent) events.get(j);
            activityRecognitionEventArray[j] = new HwActivityRecognitionEvent(arEvent.getActivity(), arEvent.getEventType(), arEvent.getTimestampNs());
        }
        return new HwActivityChangedEvent(activityRecognitionEventArray);
    }

    private HwActivityChangedExtendEvent tranferToHwActivityChangedExtendEvent(HwActivityChangedExtendEvent event) {
        if (event == null) {
            return null;
        }
        List<ActivityEvent> events = new ArrayList();
        for (HwActivityRecognitionExtendEvent e : event.getActivityRecognitionExtendEvents()) {
            events.add(new ActivityEvent(e.getActivity(), e.getEventType(), e.getTimestampNs(), tranferToOtherParameters_N(e.getOtherParams())));
        }
        HwActivityRecognitionExtendEvent[] activityRecognitionEventArray = new HwActivityRecognitionExtendEvent[events.size()];
        for (int j = 0; j < events.size(); j++) {
            ActivityEvent arEvent = (ActivityEvent) events.get(j);
            activityRecognitionEventArray[j] = new HwActivityRecognitionExtendEvent(arEvent.getActivity(), arEvent.getEventType(), arEvent.getTimestampNs(), null);
        }
        return new HwActivityChangedExtendEvent(activityRecognitionEventArray);
    }

    private HwEnvironmentChangedEvent tranferToHwEnvironmentChangedEvent(HwEnvironmentChangedEvent event) {
        if (event == null) {
            return null;
        }
        List<ActivityEvent> events = new ArrayList();
        for (HwActivityRecognitionExtendEvent e : event.getEnvironmentRecognitionEvents()) {
            events.add(new ActivityEvent(e.getActivity(), e.getEventType(), e.getTimestampNs(), tranferToOtherParameters_N(e.getOtherParams())));
        }
        HwActivityRecognitionExtendEvent[] activityRecognitionEventArray = new HwActivityRecognitionExtendEvent[events.size()];
        for (int j = 0; j < events.size(); j++) {
            ActivityEvent arEvent = (ActivityEvent) events.get(j);
            activityRecognitionEventArray[j] = new HwActivityRecognitionExtendEvent(arEvent.getActivity(), arEvent.getEventType(), arEvent.getTimestampNs(), null);
        }
        return new HwEnvironmentChangedEvent(activityRecognitionEventArray);
    }

    private OtherParameters tranferToOtherParameters_N(OtherParameters otherParams) {
        if (otherParams != null) {
            return new OtherParameters(otherParams.getmParam1(), otherParams.getmParam2(), otherParams.getmParam3(), otherParams.getmParam4(), otherParams.getmParam5());
        }
        return null;
    }

    private IActivityRecognitionHardwareSink createActivityRecognitionHardwareSink_O(final HwActivityRecognitionHardwareSink sink) {
        if (sink != null) {
            return new IActivityRecognitionHardwareSink.Stub() {
                public void onActivityChanged(HwActivityChangedEvent event) throws RemoteException {
                    sink.onActivityChanged(HwActivityRecognition.this.tranferToHwActivityChangedEvent(event));
                }

                public void onActivityExtendChanged(HwActivityChangedExtendEvent event) throws RemoteException {
                    sink.onActivityExtendChanged(HwActivityRecognition.this.tranferToHwActivityChangedExtendEvent(event));
                }

                public void onEnvironmentChanged(HwEnvironmentChangedEvent event) throws RemoteException {
                    sink.onEnvironmentChanged(HwActivityRecognition.this.tranferToHwEnvironmentChangedEvent(event));
                }
            };
        }
        return null;
    }

    private void handleProviderLoad() {
        try {
            if (this.mService != null) {
                if (this.mService.providerLoadOk()) {
                    this.mHandler.removeMessages(0);
                    this.mServiceConnection.onServiceConnected();
                    return;
                }
                this.mHandler.sendEmptyMessageDelayed(0, 500);
            }
        } catch (RemoteException e) {
            SDKLog.e(TAG, "providerLoadOk fail");
        }
    }

    private void bindService() {
        if (this.mConnectCount <= 10) {
            Intent bindIntent;
            if (sdkVersion < ANDROID_O) {
                if (this.mService == null) {
                    SDKLog.d(TAG, new StringBuilder(String.valueOf(this.mContext.getPackageName())).append(" bind ar service.").toString());
                    bindIntent = new Intent();
                    bindIntent.setClassName(AIDL_MESSAGE_SERVICE_PACKAGE, AIDL_MESSAGE_SERVICE_CLASS);
                    this.mContext.bindService(bindIntent, this.mConnection, 1);
                    this.mConnectCount++;
                    this.mHandler.sendEmptyMessageDelayed(1, 2000);
                }
            } else if (this.mService_O == null) {
                SDKLog.d(TAG, new StringBuilder(String.valueOf(this.mContext.getPackageName())).append(" bind ar service.").toString());
                bindIntent = new Intent();
                bindIntent.setClassName(AIDL_MESSAGE_SERVICE_PACKAGE_O, AIDL_MESSAGE_SERVICE_CLASS_O);
                this.mContext.bindService(bindIntent, this.mConnection, 1);
                this.mConnectCount++;
                this.mHandler.sendEmptyMessageDelayed(1, 2000);
            }
            return;
        }
        SDKLog.d(TAG, "try connect 10 times, connection fail");
    }

    private void notifyServiceDied() {
        try {
            if (sdkVersion < ANDROID_O) {
                if (this.mService != null) {
                    this.mService.asBinder().linkToDeath(this.deathHandler, 0);
                }
            } else if (this.mService_O != null) {
                this.mService_O.asBinder().linkToDeath(this.deathHandler, 0);
            }
        } catch (RemoteException e) {
            SDKLog.e(TAG, "IBinder register linkToDeath function fail.");
        }
    }
}
