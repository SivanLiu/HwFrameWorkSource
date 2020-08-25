package com.android.server.location;

import android.content.Context;
import android.os.SystemClock;
import com.android.server.hidata.wavemapping.modelservice.ModelBaseService;
import com.huawei.android.location.activityrecognition.HwActivityChangedEvent;
import com.huawei.android.location.activityrecognition.HwActivityChangedExtendEvent;
import com.huawei.android.location.activityrecognition.HwActivityRecognition;
import com.huawei.android.location.activityrecognition.HwActivityRecognitionExtendEvent;
import com.huawei.android.location.activityrecognition.HwActivityRecognitionHardwareSink;
import com.huawei.android.location.activityrecognition.HwActivityRecognitionServiceConnection;
import com.huawei.android.location.activityrecognition.HwEnvironmentChangedEvent;
import com.huawei.android.location.activityrecognition.OtherParameters;

public class HwNlpArFunction {
    private static final String ACTIVITY_AR_K_N_ACT = "android.activity_recognition.ar_k_n_act";
    private static final String ACTIVITY_CLIMBING_MOUNT = "android.activity_recognitio.climbing_mount";
    private static final String ACTIVITY_DROP = "android.activity_recognition.drop";
    private static final String ACTIVITY_ELEVATOR = "android.activity_recognition.elevator";
    private static final String ACTIVITY_FAST_WALKING = "android.activity_recognition.fast_walking";
    private static final String ACTIVITY_IN_VEHICLE = "android.activity_recognition.in_vehicle";
    private static final String ACTIVITY_ON_BICYCLE = "android.activity_recognition.on_bicycle";
    private static final String ACTIVITY_ON_FOOT = "android.activity_recognition.on_foot";
    private static final String ACTIVITY_OUTDOOR = "android.activity_recognition.outdoor";
    private static final String ACTIVITY_RELATIVE_STILL = "android.activity_recognition.relative_still";
    private static final String ACTIVITY_RUNNING = "android.activity_recognition.running";
    private static final String ACTIVITY_RUN_FOR_HEALTH = "android.activity_recognition.run_for_health";
    private static final String ACTIVITY_SMART_FLIGHT = "android.activity_recognition.smart_flight";
    private static final String ACTIVITY_STILL = "android.activity_recognition.still";
    private static final String ACTIVITY_STOP_VEHICLE = "android.activity_recognition.stop_vehicle";
    private static final String ACTIVITY_TILT = "android.activity_recognition.tilting";
    public static final int ACTIVITY_TYPE_DRIVING = 4;
    public static final int ACTIVITY_TYPE_ELEVATOR = 7;
    public static final int ACTIVITY_TYPE_ERROR = -2;
    public static final int ACTIVITY_TYPE_HIGH_SPEED_RAIL = 6;
    public static final int ACTIVITY_TYPE_RIDING = 3;
    public static final int ACTIVITY_TYPE_RUNNING = 2;
    public static final int ACTIVITY_TYPE_STATIONARY = 0;
    public static final int ACTIVITY_TYPE_TRAIN = 5;
    public static final int ACTIVITY_TYPE_UNKNOWN = -1;
    private static final String ACTIVITY_TYPE_VE_HIGH_SPEED_RAIL = "android.activity_recognition.high_speed_rail";
    public static final int ACTIVITY_TYPE_WALKING = 1;
    private static final String ACTIVITY_UNKNOWN = "android.activity_recognition.unknown";
    private static final String ACTIVITY_VE_AUTO = "android.activity_recognitio.auto";
    private static final String ACTIVITY_VE_BUS = "android.activity_recognition.bus";
    private static final String ACTIVITY_VE_CAR = "android.activity_recognition.car";
    private static final String ACTIVITY_VE_METRO = "android.activity_recognition.metro";
    private static final String ACTIVITY_VE_RAIL = "android.activity_recognitio.rail";
    private static final String ACTIVITY_VE_TRAIN = "android.activity_recognition.train";
    private static final String ACTIVITY_VE_UNKNOWN = "android.activity_recognition.ve_unknown";
    private static final String ACTIVITY_WALKING = "android.activity_recognition.walking";
    private static final String ACTIVITY_WALK_FOR_HEALTH = "android.activity_recognition.walk_for_health";
    private static final Object AR_LOCK = new Object();
    private static final int EVENT_TYPE_ENTER = 1;
    private static final int EVENT_TYPE_EXIT = 2;
    private static final long REPORT_INTERVAL = 300000000000L;
    private static final String TAG = "HwNlpArFunction";
    private static volatile HwNlpArFunction arFunction;
    private static String[] mActivities;
    /* access modifiers changed from: private */
    public HwActivityRecognitionServiceConnection connect = new HwActivityRecognitionServiceConnection() {
        /* class com.android.server.location.HwNlpArFunction.AnonymousClass2 */

        @Override // com.huawei.android.location.activityrecognition.HwActivityRecognitionServiceConnection
        public void onServiceConnected() {
            LBSLog.i(HwNlpArFunction.TAG, false, "onServiceConnected()", new Object[0]);
            boolean unused = HwNlpArFunction.this.isConnected = true;
            HwNlpArFunction.this.enableSupportedActivity(HwNlpArFunction.this.getSupportedActivities());
        }

        @Override // com.huawei.android.location.activityrecognition.HwActivityRecognitionServiceConnection
        public void onServiceDisconnected() {
            LBSLog.i(HwNlpArFunction.TAG, false, "onServiceDisconnected()", new Object[0]);
            boolean unused = HwNlpArFunction.this.isConnected = false;
            if (HwNlpArFunction.this.hwAr != null) {
                LBSLog.i(HwNlpArFunction.TAG, false, "reconnect service", new Object[0]);
                HwNlpArFunction.this.hwAr.connectService(HwNlpArFunction.this.hwArSink, HwNlpArFunction.this.connect);
            }
        }
    };
    /* access modifiers changed from: private */
    public HwActivityRecognition hwAr;
    /* access modifiers changed from: private */
    public HwActivityRecognitionHardwareSink hwArSink = new HwActivityRecognitionHardwareSink() {
        /* class com.android.server.location.HwNlpArFunction.AnonymousClass1 */

        @Override // com.huawei.android.location.activityrecognition.HwActivityRecognitionHardwareSink
        public void onActivityChanged(HwActivityChangedEvent activityChangedEvent) {
        }

        @Override // com.huawei.android.location.activityrecognition.HwActivityRecognitionHardwareSink
        public void onActivityExtendChanged(HwActivityChangedExtendEvent activityChangedExtendEvent) {
        }

        @Override // com.huawei.android.location.activityrecognition.HwActivityRecognitionHardwareSink
        public void onEnvironmentChanged(HwEnvironmentChangedEvent environmentChangedEvent) {
        }
    };
    /* access modifiers changed from: private */
    public boolean isConnected = false;

    private HwNlpArFunction(Context context) {
    }

    public static HwNlpArFunction getInstance(Context context) {
        if (arFunction == null) {
            synchronized (AR_LOCK) {
                if (arFunction == null) {
                    arFunction = new HwNlpArFunction(context);
                }
            }
        }
        return arFunction;
    }

    public boolean isConnected() {
        return this.isConnected;
    }

    /* access modifiers changed from: private */
    public void enableSupportedActivity(String[] activities) {
        if (activities != null && activities.length > 0) {
            for (String activity : activities) {
                if (!"android.activity_recognition.smart_flight".equals(activity)) {
                    enableActivityEventExtend(activity, 1, REPORT_INTERVAL);
                    enableActivityEventExtend(activity, 2, REPORT_INTERVAL);
                }
            }
        }
    }

    public void connectService(Context context) {
        this.hwAr = new HwActivityRecognition(context);
        if (this.hwAr != null) {
            LBSLog.i(TAG, false, "connectService", new Object[0]);
            this.hwAr.connectService(this.hwArSink, this.connect);
        }
    }

    public boolean enableActivityEventExtend(String activity, int eventType, long reportLatencyNs) {
        if (this.hwAr == null) {
            return false;
        }
        return this.hwAr.enableActivityEvent(activity, eventType, reportLatencyNs, new OtherParameters(1.0d, 0.0d, 0.0d, 0.0d, ""));
    }

    public String[] getSupportedActivities() {
        LBSLog.i(TAG, false, "getSupportedActivities", new Object[0]);
        HwActivityRecognition hwActivityRecognition = this.hwAr;
        if (hwActivityRecognition != null && mActivities == null) {
            mActivities = hwActivityRecognition.getSupportedActivities();
        }
        return mActivities;
    }

    public boolean flush() {
        HwActivityRecognition hwActivityRecognition = this.hwAr;
        if (hwActivityRecognition != null) {
            return hwActivityRecognition.flush();
        }
        return false;
    }

    public HwActivityChangedExtendEvent getCurrentActivityExtend() {
        HwActivityRecognition hwActivityRecognition = this.hwAr;
        if (hwActivityRecognition != null) {
            return hwActivityRecognition.getCurrentActivityExtend();
        }
        return null;
    }

    public String getCurrentActivity() {
        HwActivityRecognition hwActivityRecognition = this.hwAr;
        if (hwActivityRecognition != null) {
            return hwActivityRecognition.getCurrentActivity();
        }
        return ModelBaseService.UNKONW_IDENTIFY_RET;
    }

    public void disconnectService() {
        HwActivityRecognition hwActivityRecognition = this.hwAr;
        if (hwActivityRecognition != null && this.isConnected) {
            hwActivityRecognition.disconnectService();
            this.isConnected = false;
            mActivities = null;
        }
    }

    /* JADX INFO: Can't fix incorrect switch cases order, some code will duplicate */
    private int getActivityCode(String activity) {
        char c;
        switch (activity.hashCode()) {
            case -1623963183:
                if (activity.equals("android.activity_recognition.metro")) {
                    c = 6;
                    break;
                }
                c = 65535;
                break;
            case -1617985952:
                if (activity.equals("android.activity_recognition.still")) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case -1617129792:
                if (activity.equals("android.activity_recognition.train")) {
                    c = 7;
                    break;
                }
                c = 65535;
                break;
            case -1392503759:
                if (activity.equals("android.activity_recognition.walking")) {
                    c = 3;
                    break;
                }
                c = 65535;
                break;
            case -960535433:
                if (activity.equals("android.activity_recognition.running")) {
                    c = 5;
                    break;
                }
                c = 65535;
                break;
            case -519602411:
                if (activity.equals("android.activity_recognitio.auto")) {
                    c = '\n';
                    break;
                }
                c = 65535;
                break;
            case -462034632:
                if (activity.equals("android.activity_recognition.bus")) {
                    c = '\b';
                    break;
                }
                c = 65535;
                break;
            case -462034292:
                if (activity.equals("android.activity_recognition.car")) {
                    c = '\t';
                    break;
                }
                c = 65535;
                break;
            case -29156121:
                if (activity.equals("android.activity_recognition.on_bicycle")) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            case 307973342:
                if (activity.equals("android.activity_recognition.fast_walking")) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            case 457431430:
                if (activity.equals("android.activity_recognition.on_foot")) {
                    c = 4;
                    break;
                }
                c = 65535;
                break;
            case 668806655:
                if (activity.equals("android.activity_recognition.high_speed_rail")) {
                    c = '\f';
                    break;
                }
                c = 65535;
                break;
            case 1498807042:
                if (activity.equals("android.activity_recognition.unknown")) {
                    c = '\r';
                    break;
                }
                c = 65535;
                break;
            case 1609753690:
                if (activity.equals("android.activity_recognition.in_vehicle")) {
                    c = 11;
                    break;
                }
                c = 65535;
                break;
            default:
                c = 65535;
                break;
        }
        switch (c) {
            case 0:
                return 0;
            case 1:
                return 3;
            case 2:
            case 3:
            case 4:
                return 1;
            case 5:
                return 2;
            case 6:
            case 7:
                return 5;
            case '\b':
            case '\t':
            case '\n':
            case 11:
                return 4;
            case '\f':
                return 6;
            case '\r':
                return -1;
            default:
                return -1;
        }
    }

    private String getCurrentActivityString() {
        HwActivityChangedExtendEvent activityChangedEvent = getCurrentActivityExtend();
        if (activityChangedEvent == null) {
            return "android.activity_recognition.unknown";
        }
        int confidence = 0;
        HwActivityRecognitionExtendEvent realEvent = null;
        for (HwActivityRecognitionExtendEvent event : activityChangedEvent.getActivityRecognitionExtendEvents()) {
            if (event != null && event.getEventType() == 1 && event.getConfidence() > 0) {
                long arInterval = SystemClock.elapsedRealtimeNanos() - event.getTimestampNs();
                LBSLog.i(TAG, false, "arInterval: %{public}d ms", Long.valueOf(arInterval / 1000000));
                if (arInterval >= 0 && event.getConfidence() > confidence) {
                    confidence = event.getConfidence();
                    realEvent = event;
                }
            }
        }
        if (realEvent == null || realEvent.getActivity() == null) {
            return "android.activity_recognition.unknown";
        }
        return realEvent.getActivity();
    }

    public int requestUserArState() {
        String activity = getCurrentActivityString();
        if (!"android.activity_recognition.unknown".equals(activity) && activity != null && !"".equals(activity)) {
            return getActivityCode(activity);
        }
        return -1;
    }
}
