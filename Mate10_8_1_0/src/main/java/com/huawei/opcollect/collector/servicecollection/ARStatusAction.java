package com.huawei.opcollect.collector.servicecollection;

import android.content.Context;
import android.text.TextUtils;
import com.huawei.android.location.activityrecognition.HwActivityChangedEvent;
import com.huawei.android.location.activityrecognition.HwActivityChangedExtendEvent;
import com.huawei.android.location.activityrecognition.HwActivityRecognition;
import com.huawei.android.location.activityrecognition.HwActivityRecognitionEvent;
import com.huawei.android.location.activityrecognition.HwActivityRecognitionHardwareSink;
import com.huawei.android.location.activityrecognition.HwActivityRecognitionServiceConnection;
import com.huawei.android.location.activityrecognition.HwEnvironmentChangedEvent;
import com.huawei.nb.model.collectencrypt.RawARStatus;
import com.huawei.opcollect.odmf.OdmfCollectScheduler;
import com.huawei.opcollect.strategy.Action;
import com.huawei.opcollect.utils.OPCollectConstant;
import com.huawei.opcollect.utils.OPCollectLog;
import com.huawei.opcollect.utils.OPCollectUtils;
import java.lang.ref.WeakReference;
import java.util.Date;

public class ARStatusAction extends Action {
    private static final int ACTIVITY_DEFAULT = -1;
    private static final int ACTIVITY_FAST_WALKING = 6;
    private static final int ACTIVITY_IN_VEHICLE = 0;
    private static final int ACTIVITY_ON_BICYCLE = 1;
    private static final int ACTIVITY_RUNNING = 3;
    private static final int ACTIVITY_STILL = 4;
    private static final int ACTIVITY_TYPE_VE_HIGH_SPEED_RAIL = 5;
    private static final int ACTIVITY_UNKNOWN = 7;
    private static final int ACTIVITY_WALKING = 2;
    private static final int EVENT_TYPE_ENTER = 1;
    private static final int EVENT_TYPE_EXIT = 2;
    private static final int EVENT_TYPE_UNKNOWN = -1;
    private static final long REPORT_LATENCY_NS = 60000000000L;
    private static final String TAG = "ARStatusAction";
    private static ARStatusAction sInstance = null;
    private int mEventType = -1;
    private HwActivityRecognition mHwActivityRecognition;
    private HwActivityRecognitionHardwareSink mHwActivityRecognitionHardwareSink = null;
    private HwActivityRecognitionServiceConnection mHwActivityRecognitionServiceConnection = null;
    private final Object mLock = new Object();
    private int mMotionType = -1;

    private static class MyActivityRecognitionHardwareSink implements HwActivityRecognitionHardwareSink {
        private final WeakReference<ARStatusAction> service;

        MyActivityRecognitionHardwareSink(ARStatusAction service) {
            this.service = new WeakReference(service);
        }

        public void onActivityChanged(HwActivityChangedEvent activityChangedEvent) {
            if (activityChangedEvent != null) {
                ARStatusAction action = (ARStatusAction) this.service.get();
                if (action != null) {
                    action.mMotionType = -1;
                    for (HwActivityRecognitionEvent event : activityChangedEvent.getActivityRecognitionEvents()) {
                        if (event != null) {
                            int eventType = event.getEventType();
                            if (1 == eventType) {
                                action.mEventType = eventType;
                                String activityType = event.getActivity();
                                if (!TextUtils.isEmpty(activityType)) {
                                    if (HwActivityRecognition.ACTIVITY_IN_VEHICLE.equals(activityType)) {
                                        action.mMotionType = 0;
                                    } else if (HwActivityRecognition.ACTIVITY_ON_BICYCLE.equals(activityType)) {
                                        action.mMotionType = 1;
                                    } else if (HwActivityRecognition.ACTIVITY_WALKING.equals(activityType)) {
                                        action.mMotionType = 2;
                                    } else if (HwActivityRecognition.ACTIVITY_RUNNING.equals(activityType)) {
                                        action.mMotionType = 3;
                                    } else if (HwActivityRecognition.ACTIVITY_STILL.equals(activityType)) {
                                        action.mMotionType = 4;
                                    } else if (HwActivityRecognition.ACTIVITY_TYPE_VE_HIGH_SPEED_RAIL.equals(activityType)) {
                                        action.mMotionType = 5;
                                    } else if (HwActivityRecognition.ACTIVITY_FAST_WALKING.equals(activityType)) {
                                        action.mMotionType = 6;
                                    } else {
                                        action.mMotionType = 7;
                                    }
                                }
                            }
                        }
                    }
                    if (!(-1 == action.mMotionType || 7 == action.mMotionType)) {
                        action.perform();
                    }
                }
            }
        }

        public void onActivityExtendChanged(HwActivityChangedExtendEvent hwActivityChangedExtendEvent) {
        }

        public void onEnvironmentChanged(HwEnvironmentChangedEvent hwEnvironmentChangedEvent) {
        }
    }

    private static class MyActivityRecognitionServiceConnection implements HwActivityRecognitionServiceConnection {
        private final WeakReference<ARStatusAction> service;

        MyActivityRecognitionServiceConnection(ARStatusAction service) {
            this.service = new WeakReference(service);
        }

        public void onServiceConnected() {
            OPCollectLog.r(ARStatusAction.TAG, "onServiceConnected()");
            ARStatusAction action = (ARStatusAction) this.service.get();
            if (!(action == null || action.enableAREvent())) {
                OPCollectLog.i(ARStatusAction.TAG, "No supported activity.");
            }
        }

        public void onServiceDisconnected() {
            OPCollectLog.r(ARStatusAction.TAG, "onServiceDisconnected()");
        }
    }

    private ARStatusAction(Context context, String name) {
        super(context, name);
        setDailyRecordNum(queryDailyRecordNum(RawARStatus.class));
    }

    public static synchronized ARStatusAction getInstance(Context context) {
        ARStatusAction aRStatusAction;
        synchronized (ARStatusAction.class) {
            if (sInstance == null) {
                sInstance = new ARStatusAction(context, OPCollectConstant.AR_ACTION_NAME);
            }
            aRStatusAction = sInstance;
        }
        return aRStatusAction;
    }

    public void enable() {
        super.enable();
        synchronized (this.mLock) {
            if (this.mHwActivityRecognition == null) {
                this.mHwActivityRecognition = new HwActivityRecognition(this.mContext);
            }
            if (this.mHwActivityRecognitionHardwareSink == null) {
                this.mHwActivityRecognitionHardwareSink = new MyActivityRecognitionHardwareSink(this);
            }
            if (this.mHwActivityRecognitionServiceConnection == null) {
                this.mHwActivityRecognitionServiceConnection = new MyActivityRecognitionServiceConnection(this);
            }
            this.mHwActivityRecognition.connectService(this.mHwActivityRecognitionHardwareSink, this.mHwActivityRecognitionServiceConnection);
        }
    }

    public void disable() {
        super.disable();
        disableAREvent();
        synchronized (this.mLock) {
            if (this.mHwActivityRecognition != null) {
                this.mHwActivityRecognition.disconnectService();
                this.mHwActivityRecognitionServiceConnection = null;
                this.mHwActivityRecognitionHardwareSink = null;
                this.mHwActivityRecognition = null;
            }
        }
    }

    public boolean destroy() {
        super.destroy();
        destroyInstance();
        return true;
    }

    private static synchronized void destroyInstance() {
        synchronized (ARStatusAction.class) {
            sInstance = null;
        }
    }

    protected boolean execute() {
        RawARStatus rawARStatus = new RawARStatus();
        rawARStatus.setMTimeStamp(new Date());
        rawARStatus.setMMotionType(Integer.valueOf(this.mMotionType));
        rawARStatus.setMStatus(Integer.valueOf(this.mEventType));
        rawARStatus.setMReservedText(OPCollectUtils.formatCurrentTime());
        OdmfCollectScheduler.getInstance().getDataHandler().obtainMessage(4, rawARStatus).sendToTarget();
        return true;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean enableAREvent() {
        OPCollectLog.r(TAG, "enableAREvent.");
        boolean z = false;
        boolean isEnableBicycle = false;
        boolean isEnableWalking = false;
        boolean z2 = false;
        boolean isEnableStill = false;
        boolean isEnableVE_HIGH_SPEED_RAIL = false;
        boolean isEnableFAST_WALKING = false;
        synchronized (this.mLock) {
            if (this.mHwActivityRecognition != null) {
                if (this.mHwActivityRecognition.getSupportedActivities().length == 0) {
                    OPCollectLog.i(TAG, "getSupportedActivities length is 0");
                    return false;
                }
                if (this.mHwActivityRecognition.enableActivityEvent(HwActivityRecognition.ACTIVITY_IN_VEHICLE, 1, REPORT_LATENCY_NS)) {
                    z = this.mHwActivityRecognition.enableActivityEvent(HwActivityRecognition.ACTIVITY_IN_VEHICLE, 2, REPORT_LATENCY_NS);
                } else {
                    z = false;
                }
                if (!z) {
                    OPCollectLog.r(TAG, "enable vehicle enter exit failed.");
                }
                if (this.mHwActivityRecognition.enableActivityEvent(HwActivityRecognition.ACTIVITY_ON_BICYCLE, 1, REPORT_LATENCY_NS)) {
                    isEnableBicycle = this.mHwActivityRecognition.enableActivityEvent(HwActivityRecognition.ACTIVITY_ON_BICYCLE, 2, REPORT_LATENCY_NS);
                } else {
                    isEnableBicycle = false;
                }
                if (!isEnableBicycle) {
                    OPCollectLog.r(TAG, "enable bicycle enter exit failed.");
                }
                if (this.mHwActivityRecognition.enableActivityEvent(HwActivityRecognition.ACTIVITY_WALKING, 1, REPORT_LATENCY_NS)) {
                    isEnableWalking = this.mHwActivityRecognition.enableActivityEvent(HwActivityRecognition.ACTIVITY_WALKING, 2, REPORT_LATENCY_NS);
                } else {
                    isEnableWalking = false;
                }
                if (!isEnableWalking) {
                    OPCollectLog.r(TAG, "enable walk enter exit failed.");
                }
                if (this.mHwActivityRecognition.enableActivityEvent(HwActivityRecognition.ACTIVITY_RUNNING, 1, REPORT_LATENCY_NS)) {
                    z2 = this.mHwActivityRecognition.enableActivityEvent(HwActivityRecognition.ACTIVITY_RUNNING, 2, REPORT_LATENCY_NS);
                } else {
                    z2 = false;
                }
                if (!z2) {
                    OPCollectLog.r(TAG, "enable running enter exit failed.");
                }
                if (this.mHwActivityRecognition.enableActivityEvent(HwActivityRecognition.ACTIVITY_STILL, 1, REPORT_LATENCY_NS)) {
                    isEnableStill = this.mHwActivityRecognition.enableActivityEvent(HwActivityRecognition.ACTIVITY_STILL, 2, REPORT_LATENCY_NS);
                } else {
                    isEnableStill = false;
                }
                if (!isEnableStill) {
                    OPCollectLog.r(TAG, "enable still enter exit failed.");
                }
                if (this.mHwActivityRecognition.enableActivityEvent(HwActivityRecognition.ACTIVITY_TYPE_VE_HIGH_SPEED_RAIL, 1, REPORT_LATENCY_NS)) {
                    isEnableVE_HIGH_SPEED_RAIL = this.mHwActivityRecognition.enableActivityEvent(HwActivityRecognition.ACTIVITY_TYPE_VE_HIGH_SPEED_RAIL, 2, REPORT_LATENCY_NS);
                } else {
                    isEnableVE_HIGH_SPEED_RAIL = false;
                }
                if (!isEnableVE_HIGH_SPEED_RAIL) {
                    OPCollectLog.r(TAG, "enable high speed rail enter exit failed.");
                }
                if (this.mHwActivityRecognition.enableActivityEvent(HwActivityRecognition.ACTIVITY_FAST_WALKING, 1, REPORT_LATENCY_NS)) {
                    isEnableFAST_WALKING = this.mHwActivityRecognition.enableActivityEvent(HwActivityRecognition.ACTIVITY_FAST_WALKING, 2, REPORT_LATENCY_NS);
                } else {
                    isEnableFAST_WALKING = false;
                }
                if (!isEnableFAST_WALKING) {
                    OPCollectLog.r(TAG, "enable fast walking enter exit failed.");
                }
            }
        }
    }

    public boolean disableAREvent() {
        OPCollectLog.r(TAG, "disableAREvent.");
        boolean z = false;
        boolean isDisableBicycle = false;
        boolean isDisableWalking = false;
        boolean isDisableRunning = false;
        boolean isDisableStill = false;
        boolean isDisableVE_HIGH_SPEED_RAIL = false;
        boolean isDisableFAST_WALKING = false;
        synchronized (this.mLock) {
            if (this.mHwActivityRecognition != null) {
                if (this.mHwActivityRecognition.disableActivityEvent(HwActivityRecognition.ACTIVITY_IN_VEHICLE, 1)) {
                    z = this.mHwActivityRecognition.disableActivityEvent(HwActivityRecognition.ACTIVITY_IN_VEHICLE, 2);
                } else {
                    z = false;
                }
                if (!z) {
                    OPCollectLog.r(TAG, "disable vehicle enter exit failed.");
                }
                if (this.mHwActivityRecognition.disableActivityEvent(HwActivityRecognition.ACTIVITY_ON_BICYCLE, 1)) {
                    isDisableBicycle = this.mHwActivityRecognition.disableActivityEvent(HwActivityRecognition.ACTIVITY_ON_BICYCLE, 2);
                } else {
                    isDisableBicycle = false;
                }
                if (!isDisableBicycle) {
                    OPCollectLog.r(TAG, "disable bicycle enter exit failed.");
                }
                if (this.mHwActivityRecognition.disableActivityEvent(HwActivityRecognition.ACTIVITY_WALKING, 1)) {
                    isDisableWalking = this.mHwActivityRecognition.disableActivityEvent(HwActivityRecognition.ACTIVITY_WALKING, 2);
                } else {
                    isDisableWalking = false;
                }
                if (!isDisableWalking) {
                    OPCollectLog.r(TAG, "disable walk enter exit failed.");
                }
                if (this.mHwActivityRecognition.disableActivityEvent(HwActivityRecognition.ACTIVITY_RUNNING, 1)) {
                    isDisableRunning = this.mHwActivityRecognition.disableActivityEvent(HwActivityRecognition.ACTIVITY_RUNNING, 2);
                } else {
                    isDisableRunning = false;
                }
                if (!isDisableRunning) {
                    OPCollectLog.r(TAG, "disable running enter exit failed.");
                }
                if (this.mHwActivityRecognition.disableActivityEvent(HwActivityRecognition.ACTIVITY_STILL, 1)) {
                    isDisableStill = this.mHwActivityRecognition.disableActivityEvent(HwActivityRecognition.ACTIVITY_STILL, 2);
                } else {
                    isDisableStill = false;
                }
                if (!isDisableStill) {
                    OPCollectLog.r(TAG, "disable still enter exit failed.");
                }
                if (this.mHwActivityRecognition.disableActivityEvent(HwActivityRecognition.ACTIVITY_TYPE_VE_HIGH_SPEED_RAIL, 1)) {
                    isDisableVE_HIGH_SPEED_RAIL = this.mHwActivityRecognition.disableActivityEvent(HwActivityRecognition.ACTIVITY_TYPE_VE_HIGH_SPEED_RAIL, 2);
                } else {
                    isDisableVE_HIGH_SPEED_RAIL = false;
                }
                if (!isDisableVE_HIGH_SPEED_RAIL) {
                    OPCollectLog.r(TAG, "disable high speed rail enter exit failed.");
                }
                if (this.mHwActivityRecognition.disableActivityEvent(HwActivityRecognition.ACTIVITY_FAST_WALKING, 1)) {
                    isDisableFAST_WALKING = this.mHwActivityRecognition.disableActivityEvent(HwActivityRecognition.ACTIVITY_FAST_WALKING, 2);
                } else {
                    isDisableFAST_WALKING = false;
                }
                if (!isDisableFAST_WALKING) {
                    OPCollectLog.r(TAG, "disable fast walking enter exit failed.");
                }
            }
        }
        if (z || isDisableBicycle || isDisableWalking || isDisableFAST_WALKING || isDisableStill || isDisableRunning) {
            return true;
        }
        return isDisableVE_HIGH_SPEED_RAIL;
    }
}
