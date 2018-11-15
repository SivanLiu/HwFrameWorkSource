package com.huawei.opcollect.collector.servicecollection;

import android.content.Context;
import android.os.Build.VERSION;
import android.os.PowerManager;
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
import com.huawei.opcollect.strategy.AbsActionParam;
import com.huawei.opcollect.strategy.Action;
import com.huawei.opcollect.utils.OPCollectConstant;
import com.huawei.opcollect.utils.OPCollectLog;
import com.huawei.opcollect.utils.OPCollectUtils;
import java.lang.ref.WeakReference;
import java.util.Date;

public class ARStatusAction extends Action {
    private static final int ACTIVITY_DEFAULT = -1;
    private static final int ACTIVITY_FAST_WALKING = 24;
    private static final int ACTIVITY_IN_VEHICLE = 0;
    private static final int ACTIVITY_ON_BICYCLE = 1;
    private static final int ACTIVITY_RUNNING = 3;
    private static final int ACTIVITY_STILL = 4;
    private static final int ACTIVITY_TYPE_VE_HIGH_SPEED_RAIL = 20;
    private static final int ACTIVITY_UNKNOWN = 63;
    private static final int ACTIVITY_WALKING = 2;
    private static final int EVENT_TYPE_ENTER = 1;
    private static final int EVENT_TYPE_EXIT = 2;
    private static final int EVENT_TYPE_UNKNOWN = -1;
    private static final long REPORT_LATENCY_NS = 60000000000L;
    private static final long REPORT_LATENCY_NS_SCREEM_OFF = 200000000000L;
    private static final String TAG = "ARStatusAction";
    public static final int TYPE_CONNECTED = 0;
    public static final int TYPE_SCREEN_OFF = 1;
    public static final int TYPE_SCREEN_ON = 2;
    private static ARStatusAction sInstance = null;
    private int mEventType = -1;
    private HwActivityRecognition mHwActivityRecognition;
    private HwActivityRecognitionHardwareSink mHwActivityRecognitionHardwareSink = null;
    private HwActivityRecognitionServiceConnection mHwActivityRecognitionServiceConnection = null;
    private final Object mLock = new Object();

    private static class ARActionParam extends AbsActionParam {
        private int eventType;
        private int motionType;
        private long timestampNs;

        ARActionParam(int motionType, int eventType, long timestampNs) {
            this.motionType = motionType;
            this.eventType = eventType;
            this.timestampNs = timestampNs;
        }

        int getMotionType() {
            return this.motionType;
        }

        int getEventType() {
            return this.eventType;
        }

        long getTimestampNs() {
            return this.timestampNs;
        }

        public String toString() {
            return "ARActionParam{motionType=" + this.motionType + ", eventType=" + this.eventType + ", timestampNs=" + this.timestampNs + "} " + super.toString();
        }
    }

    private static class MyActivityRecognitionHardwareSink implements HwActivityRecognitionHardwareSink {
        private final WeakReference<ARStatusAction> service;

        MyActivityRecognitionHardwareSink(ARStatusAction service) {
            this.service = new WeakReference(service);
        }

        public void onActivityChanged(HwActivityChangedEvent activityChangedEvent) {
            if (activityChangedEvent != null) {
                ARStatusAction action = (ARStatusAction) this.service.get();
                if (action != null) {
                    for (HwActivityRecognitionEvent event : activityChangedEvent.getActivityRecognitionEvents()) {
                        if (event != null) {
                            int eventType = event.getEventType();
                            long timestampNs = event.getTimestampNs();
                            String activityType = event.getActivity();
                            if (!TextUtils.isEmpty(activityType)) {
                                int motionType;
                                if (HwActivityRecognition.ACTIVITY_IN_VEHICLE.equals(activityType)) {
                                    motionType = 0;
                                } else if (HwActivityRecognition.ACTIVITY_ON_BICYCLE.equals(activityType)) {
                                    motionType = 1;
                                } else if (HwActivityRecognition.ACTIVITY_WALKING.equals(activityType)) {
                                    motionType = 2;
                                } else if (HwActivityRecognition.ACTIVITY_RUNNING.equals(activityType)) {
                                    motionType = 3;
                                } else if (HwActivityRecognition.ACTIVITY_STILL.equals(activityType)) {
                                    motionType = 4;
                                } else if (HwActivityRecognition.ACTIVITY_TYPE_VE_HIGH_SPEED_RAIL.equals(activityType)) {
                                    motionType = ARStatusAction.ACTIVITY_TYPE_VE_HIGH_SPEED_RAIL;
                                } else if (HwActivityRecognition.ACTIVITY_FAST_WALKING.equals(activityType)) {
                                    motionType = ARStatusAction.ACTIVITY_FAST_WALKING;
                                } else {
                                    motionType = ARStatusAction.ACTIVITY_UNKNOWN;
                                }
                                if (ARStatusAction.ACTIVITY_UNKNOWN != motionType) {
                                    action.performWithArgs(new ARActionParam(motionType, eventType, timestampNs));
                                }
                            }
                        }
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
            if (action != null) {
                boolean screenStatus = false;
                PowerManager pm = null;
                if (action.mContext != null) {
                    pm = (PowerManager) action.mContext.getSystemService("power");
                }
                if (pm != null && VERSION.SDK_INT >= ARStatusAction.ACTIVITY_TYPE_VE_HIGH_SPEED_RAIL) {
                    screenStatus = pm.isInteractive();
                }
                if (!action.enableAREvent(screenStatus ? 0 : 1)) {
                    OPCollectLog.i(ARStatusAction.TAG, "No supported activity.");
                }
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

    protected boolean executeWithArgs(AbsActionParam absActionParam) {
        if (absActionParam == null || !(absActionParam instanceof ARActionParam)) {
            return false;
        }
        ARActionParam actionParam = (ARActionParam) absActionParam;
        RawARStatus rawARStatus = new RawARStatus();
        rawARStatus.setMTimeStamp(new Date());
        rawARStatus.setMMotionType(Integer.valueOf(actionParam.getMotionType()));
        rawARStatus.setMStatus(Integer.valueOf(actionParam.getEventType()));
        rawARStatus.setMReservedText(OPCollectUtils.formatCurrentTime());
        OdmfCollectScheduler.getInstance().getDataHandler().obtainMessage(4, rawARStatus).sendToTarget();
        return true;
    }

    /* JADX WARNING: Missing block: B:63:0x0128, code:
            if (r5 != false) goto L_0x0136;
     */
    /* JADX WARNING: Missing block: B:64:0x012a, code:
            if (r0 != false) goto L_0x0136;
     */
    /* JADX WARNING: Missing block: B:65:0x012c, code:
            if (r1 != false) goto L_0x0136;
     */
    /* JADX WARNING: Missing block: B:66:0x012e, code:
            if (r4 != false) goto L_0x0136;
     */
    /* JADX WARNING: Missing block: B:67:0x0130, code:
            if (r6 != false) goto L_0x0136;
     */
    /* JADX WARNING: Missing block: B:68:0x0132, code:
            if (r3 != false) goto L_0x0136;
     */
    /* JADX WARNING: Missing block: B:69:0x0134, code:
            if (r2 == false) goto L_0x014e;
     */
    /* JADX WARNING: Missing block: B:83:?, code:
            return true;
     */
    /* JADX WARNING: Missing block: B:84:?, code:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean enableAREvent(int type) {
        OPCollectLog.r(TAG, "enableAREvent, type is " + type);
        long reportLantencyNs = type == 1 ? REPORT_LATENCY_NS_SCREEM_OFF : REPORT_LATENCY_NS;
        boolean isEnableVehicle = false;
        boolean isEnableBicycle = false;
        boolean isEnableWalking = false;
        boolean isEnableRunning = false;
        boolean isEnableStill = false;
        boolean isEnableVE_HIGH_SPEED_RAIL = false;
        boolean isEnableFAST_WALKING = false;
        synchronized (this.mLock) {
            if (this.mHwActivityRecognition != null) {
                if (this.mHwActivityRecognition.getSupportedActivities().length == 0) {
                    OPCollectLog.i(TAG, "getSupportedActivities length is 0");
                    return false;
                }
                isEnableVehicle = this.mHwActivityRecognition.enableActivityEvent(HwActivityRecognition.ACTIVITY_IN_VEHICLE, 1, reportLantencyNs) && this.mHwActivityRecognition.enableActivityEvent(HwActivityRecognition.ACTIVITY_IN_VEHICLE, 2, reportLantencyNs);
                if (!isEnableVehicle) {
                    OPCollectLog.r(TAG, "enable vehicle enter exit failed.");
                }
                isEnableBicycle = this.mHwActivityRecognition.enableActivityEvent(HwActivityRecognition.ACTIVITY_ON_BICYCLE, 1, reportLantencyNs) && this.mHwActivityRecognition.enableActivityEvent(HwActivityRecognition.ACTIVITY_ON_BICYCLE, 2, reportLantencyNs);
                if (!isEnableBicycle) {
                    OPCollectLog.r(TAG, "enable bicycle enter exit failed.");
                }
                isEnableWalking = this.mHwActivityRecognition.enableActivityEvent(HwActivityRecognition.ACTIVITY_WALKING, 1, reportLantencyNs) && this.mHwActivityRecognition.enableActivityEvent(HwActivityRecognition.ACTIVITY_WALKING, 2, reportLantencyNs);
                if (!isEnableWalking) {
                    OPCollectLog.r(TAG, "enable walk enter exit failed.");
                }
                isEnableRunning = this.mHwActivityRecognition.enableActivityEvent(HwActivityRecognition.ACTIVITY_RUNNING, 1, reportLantencyNs) && this.mHwActivityRecognition.enableActivityEvent(HwActivityRecognition.ACTIVITY_RUNNING, 2, reportLantencyNs);
                if (!isEnableRunning) {
                    OPCollectLog.r(TAG, "enable running enter exit failed.");
                }
                isEnableStill = this.mHwActivityRecognition.enableActivityEvent(HwActivityRecognition.ACTIVITY_STILL, 1, reportLantencyNs) && this.mHwActivityRecognition.enableActivityEvent(HwActivityRecognition.ACTIVITY_STILL, 2, reportLantencyNs);
                if (!isEnableStill) {
                    OPCollectLog.r(TAG, "enable still enter exit failed.");
                }
                isEnableVE_HIGH_SPEED_RAIL = this.mHwActivityRecognition.enableActivityEvent(HwActivityRecognition.ACTIVITY_TYPE_VE_HIGH_SPEED_RAIL, 1, reportLantencyNs) && this.mHwActivityRecognition.enableActivityEvent(HwActivityRecognition.ACTIVITY_TYPE_VE_HIGH_SPEED_RAIL, 2, reportLantencyNs);
                if (!isEnableVE_HIGH_SPEED_RAIL) {
                    OPCollectLog.r(TAG, "enable high speed rail enter exit failed.");
                }
                isEnableFAST_WALKING = this.mHwActivityRecognition.enableActivityEvent(HwActivityRecognition.ACTIVITY_FAST_WALKING, 1, reportLantencyNs) && this.mHwActivityRecognition.enableActivityEvent(HwActivityRecognition.ACTIVITY_FAST_WALKING, 2, reportLantencyNs);
                if (!isEnableFAST_WALKING) {
                    OPCollectLog.r(TAG, "enable fast walking enter exit failed.");
                }
            }
        }
    }

    public boolean disableAREvent() {
        OPCollectLog.r(TAG, "disableAREvent.");
        boolean isDisableVehicle = false;
        boolean isDisableBicycle = false;
        boolean isDisableWalking = false;
        boolean isDisableRunning = false;
        boolean isDisableStill = false;
        boolean isDisableVE_HIGH_SPEED_RAIL = false;
        boolean isDisableFAST_WALKING = false;
        synchronized (this.mLock) {
            if (this.mHwActivityRecognition != null) {
                isDisableVehicle = this.mHwActivityRecognition.disableActivityEvent(HwActivityRecognition.ACTIVITY_IN_VEHICLE, 1) && this.mHwActivityRecognition.disableActivityEvent(HwActivityRecognition.ACTIVITY_IN_VEHICLE, 2);
                if (!isDisableVehicle) {
                    OPCollectLog.r(TAG, "disable vehicle enter exit failed.");
                }
                isDisableBicycle = this.mHwActivityRecognition.disableActivityEvent(HwActivityRecognition.ACTIVITY_ON_BICYCLE, 1) && this.mHwActivityRecognition.disableActivityEvent(HwActivityRecognition.ACTIVITY_ON_BICYCLE, 2);
                if (!isDisableBicycle) {
                    OPCollectLog.r(TAG, "disable bicycle enter exit failed.");
                }
                isDisableWalking = this.mHwActivityRecognition.disableActivityEvent(HwActivityRecognition.ACTIVITY_WALKING, 1) && this.mHwActivityRecognition.disableActivityEvent(HwActivityRecognition.ACTIVITY_WALKING, 2);
                if (!isDisableWalking) {
                    OPCollectLog.r(TAG, "disable walk enter exit failed.");
                }
                isDisableRunning = this.mHwActivityRecognition.disableActivityEvent(HwActivityRecognition.ACTIVITY_RUNNING, 1) && this.mHwActivityRecognition.disableActivityEvent(HwActivityRecognition.ACTIVITY_RUNNING, 2);
                if (!isDisableRunning) {
                    OPCollectLog.r(TAG, "disable running enter exit failed.");
                }
                isDisableStill = this.mHwActivityRecognition.disableActivityEvent(HwActivityRecognition.ACTIVITY_STILL, 1) && this.mHwActivityRecognition.disableActivityEvent(HwActivityRecognition.ACTIVITY_STILL, 2);
                if (!isDisableStill) {
                    OPCollectLog.r(TAG, "disable still enter exit failed.");
                }
                isDisableVE_HIGH_SPEED_RAIL = this.mHwActivityRecognition.disableActivityEvent(HwActivityRecognition.ACTIVITY_TYPE_VE_HIGH_SPEED_RAIL, 1) && this.mHwActivityRecognition.disableActivityEvent(HwActivityRecognition.ACTIVITY_TYPE_VE_HIGH_SPEED_RAIL, 2);
                if (!isDisableVE_HIGH_SPEED_RAIL) {
                    OPCollectLog.r(TAG, "disable high speed rail enter exit failed.");
                }
                isDisableFAST_WALKING = this.mHwActivityRecognition.disableActivityEvent(HwActivityRecognition.ACTIVITY_FAST_WALKING, 1) && this.mHwActivityRecognition.disableActivityEvent(HwActivityRecognition.ACTIVITY_FAST_WALKING, 2);
                if (!isDisableFAST_WALKING) {
                    OPCollectLog.r(TAG, "disable fast walking enter exit failed.");
                }
            }
        }
        if (isDisableVehicle || isDisableBicycle || isDisableWalking || isDisableFAST_WALKING || isDisableStill || isDisableRunning || isDisableVE_HIGH_SPEED_RAIL) {
            return true;
        }
        return false;
    }
}
