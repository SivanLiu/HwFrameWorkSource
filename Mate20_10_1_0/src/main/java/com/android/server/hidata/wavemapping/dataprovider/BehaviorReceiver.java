package com.android.server.hidata.wavemapping.dataprovider;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.telephony.TelephonyManager;
import com.android.server.hidata.wavemapping.chr.BuildBenefitStatisticsChrInfo;
import com.android.server.hidata.wavemapping.chr.BuildSpaceUserStatisticsChrInfo;
import com.android.server.hidata.wavemapping.cons.Constant;
import com.android.server.hidata.wavemapping.cons.ContextManager;
import com.android.server.hidata.wavemapping.cons.ParamManager;
import com.android.server.hidata.wavemapping.dao.BehaviorDao;
import com.android.server.hidata.wavemapping.dao.LocationDao;
import com.android.server.hidata.wavemapping.entity.ParameterInfo;
import com.android.server.hidata.wavemapping.util.LogUtil;
import com.android.server.hidata.wavemapping.util.ShowToast;
import com.android.server.intellicom.common.SmartDualCardConsts;
import java.util.concurrent.atomic.AtomicBoolean;

public class BehaviorReceiver {
    private static final int DEFAULT_VALUE = -1;
    private static final String KEY_STATIONARY = "stationary";
    private static final String PG_AR_STATE_ACTION = "com.huawei.intent.action.PG_AR_STATE_ACTION";
    private static final String PG_RECEIVER_PERMISSION = "com.huawei.powergenie.receiverPermission";
    private static final int STEP_INCREASE_THRESHOLD = 5;
    private static final long STEP_RELEASE_THRESHOLD = 5000;
    public static final String TAG = ("WMapping." + BehaviorReceiver.class.getSimpleName());
    /* access modifiers changed from: private */
    public static int batch = 1;
    /* access modifiers changed from: private */
    public static boolean isScreenOn = true;
    /* access modifiers changed from: private */
    public static boolean isStationary = false;
    /* access modifiers changed from: private */
    public static int oldSim1Ready = -1;
    /* access modifiers changed from: private */
    public static int oldSimReady = -1;
    private BroadcastReceiver arReceiver = new BroadcastReceiver() {
        /* class com.android.server.hidata.wavemapping.dataprovider.BehaviorReceiver.AnonymousClass1 */

        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                boolean mIsStationaryTmp = intent.getBooleanExtra(BehaviorReceiver.KEY_STATIONARY, false);
                LogUtil.i(false, "Stationary =%{public}s", String.valueOf(mIsStationaryTmp));
                if (BehaviorReceiver.isStationary != mIsStationaryTmp) {
                    BehaviorReceiver.access$108();
                    boolean unused = BehaviorReceiver.isStationary = mIsStationaryTmp;
                    if (BehaviorReceiver.this.mMachineHandler != null) {
                        if (BehaviorReceiver.isStationary) {
                            LogUtil.i(false, "arReceiver: send STATION", new Object[0]);
                            BehaviorReceiver.this.mMachineHandler.sendEmptyMessage(113);
                        } else {
                            LogUtil.i(false, "arReceiver send MOVE", new Object[0]);
                            BehaviorReceiver.this.mMachineHandler.sendEmptyMessage(110);
                            ShowToast.showToast("Moving by AR");
                        }
                    }
                    LogUtil.i(false, "AR Status Changed, batch =%{public}d", Integer.valueOf(BehaviorReceiver.batch));
                }
            }
        }
    };
    private BehaviorDao behaviorDao;
    private LocationDao locationDao;
    private AtomicBoolean mAccSensorRegistered = new AtomicBoolean(false);
    private BuildBenefitStatisticsChrInfo mBuildBenefitStatisticsChrInfo = null;
    private BuildSpaceUserStatisticsChrInfo mBuildSpaceUserStatisticsChrInfo = null;
    /* access modifiers changed from: private */
    public Context mCtx;
    /* access modifiers changed from: private */
    public int mLastStepCnt;
    private int mLastWifiBatch;
    private int mLastWifiStepCnt;
    /* access modifiers changed from: private */
    public Handler mMachineHandler;
    private final StepSensorEventListener mSensorEventListener = new StepSensorEventListener();
    private SensorManager mSensorManager;
    private Sensor mStepCntSensor;
    private ParameterInfo param;
    private int savedBatch = 0;
    private BroadcastReceiver screenReceiver = new BroadcastReceiver() {
        /* class com.android.server.hidata.wavemapping.dataprovider.BehaviorReceiver.AnonymousClass2 */

        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                LogUtil.i(false, "receive screen state", new Object[0]);
                if (SmartDualCardConsts.SYSTEM_STATE_NAME_SCREEN_ON.equals(intent.getAction())) {
                    BehaviorReceiver.access$108();
                    boolean unused = BehaviorReceiver.isScreenOn = true;
                    LogUtil.i(false, "BehaviorReceiver screenOn = %{public}s, batch = %{public}d", String.valueOf(BehaviorReceiver.isScreenOn), Integer.valueOf(BehaviorReceiver.batch));
                    BehaviorReceiver.this.registerStepCntSensor();
                } else if (SmartDualCardConsts.SYSTEM_STATE_NAME_SCREEN_OFF.equals(intent.getAction())) {
                    BehaviorReceiver.access$108();
                    boolean unused2 = BehaviorReceiver.isScreenOn = false;
                    LogUtil.i(false, "BehaviorReceiver screenOff = %{public}s, batch = %{public}d", String.valueOf(BehaviorReceiver.isScreenOn), Integer.valueOf(BehaviorReceiver.batch));
                    BehaviorReceiver.this.unregisterStepCntSensor();
                }
            }
        }
    };
    private BroadcastReceiver simReceiver = new BroadcastReceiver() {
        /* class com.android.server.hidata.wavemapping.dataprovider.BehaviorReceiver.AnonymousClass4 */

        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                boolean isSendMsg = false;
                if (SmartDualCardConsts.SYSTEM_STATE_NAME_SIM_STATE_CHANGED.equals(intent.getAction())) {
                    LogUtil.i(false, "BehaviorReceiver SIM state changed %{public}s", intent.getAction());
                    if (BehaviorReceiver.this.mMachineHandler != null) {
                        TelephonyManager tm = (TelephonyManager) BehaviorReceiver.this.mCtx.getSystemService("phone");
                        if (tm == null || tm.getSimState(0) != 5) {
                            LogUtil.i(false, "SIM0 invalid : %{public}d", Integer.valueOf(BehaviorReceiver.oldSimReady));
                            if (BehaviorReceiver.oldSimReady != 0) {
                                isSendMsg = true;
                            }
                            int unused = BehaviorReceiver.oldSimReady = 0;
                        } else {
                            LogUtil.i(false, "SIM0 ready : %{public}d", Integer.valueOf(BehaviorReceiver.oldSimReady));
                            if (BehaviorReceiver.oldSimReady != 1) {
                                isSendMsg = true;
                            }
                            int unused2 = BehaviorReceiver.oldSimReady = 1;
                        }
                        if (tm == null || tm.getSimState(1) != 5) {
                            LogUtil.i(false, "SIM1 invalid : %{public}d", Integer.valueOf(BehaviorReceiver.oldSim1Ready));
                            if (BehaviorReceiver.oldSim1Ready != 0) {
                                isSendMsg = true;
                            }
                            int unused3 = BehaviorReceiver.oldSim1Ready = 0;
                        } else {
                            LogUtil.i(false, "SIM1 ready : %{public}d", Integer.valueOf(BehaviorReceiver.oldSim1Ready));
                            if (BehaviorReceiver.oldSim1Ready != 1) {
                                isSendMsg = true;
                            }
                            int unused4 = BehaviorReceiver.oldSim1Ready = 1;
                        }
                        if (isSendMsg) {
                            BehaviorReceiver.this.mMachineHandler.sendEmptyMessage(94);
                        }
                    }
                }
            }
        }
    };
    private BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        /* class com.android.server.hidata.wavemapping.dataprovider.BehaviorReceiver.AnonymousClass3 */

        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                LogUtil.i(false, "receive power state", new Object[0]);
                if ("android.intent.action.ACTION_POWER_CONNECTED".equals(intent.getAction())) {
                    LogUtil.i(false, "BehaviorReceiver Power Connected ", new Object[0]);
                    if (BehaviorReceiver.this.mMachineHandler != null) {
                        BehaviorReceiver.this.mMachineHandler.sendEmptyMessage(150);
                    } else {
                        LogUtil.d(false, "mMachineHandler is null", new Object[0]);
                    }
                    BehaviorReceiver.this.uploadBenefitChr();
                    BehaviorReceiver.this.uploadSpaceUserChr();
                } else if ("android.intent.action.ACTION_POWER_DISCONNECTED".equals(intent.getAction())) {
                    LogUtil.i(false, "BehaviorReceiver Power Disconnected ", new Object[0]);
                    if (BehaviorReceiver.this.mMachineHandler != null) {
                        BehaviorReceiver.this.mMachineHandler.sendEmptyMessage(151);
                    } else {
                        LogUtil.d(false, "mMachineHandler is null", new Object[0]);
                    }
                    BehaviorReceiver.this.uploadBenefitChr();
                    BehaviorReceiver.this.uploadSpaceUserChr();
                }
            }
        }
    };

    static /* synthetic */ int access$108() {
        int i = batch;
        batch = i + 1;
        return i;
    }

    public BehaviorReceiver(Handler handler) {
        LogUtil.i(false, "BehaviorReceiverHandler", new Object[0]);
        this.mCtx = ContextManager.getInstance().getContext();
        this.param = ParamManager.getInstance().getParameterInfo();
        this.behaviorDao = new BehaviorDao();
        this.locationDao = new LocationDao();
        this.savedBatch = this.behaviorDao.getBatch();
        int i = this.savedBatch;
        if (i == 0) {
            this.behaviorDao.insert(batch);
            LogUtil.d(false, " no default record, init...", new Object[0]);
        } else {
            batch = i + 1;
            LogUtil.i(false, " default record, history batch=%{public}d", Integer.valueOf(i));
        }
        this.mCtx.registerReceiver(this.arReceiver, new IntentFilter(PG_AR_STATE_ACTION), PG_RECEIVER_PERMISSION, null);
        this.mSensorManager = (SensorManager) this.mCtx.getSystemService("sensor");
        SensorManager sensorManager = this.mSensorManager;
        if (sensorManager != null) {
            this.mStepCntSensor = sensorManager.getDefaultSensor(19);
            registerStepCntSensor();
        } else {
            LogUtil.e(false, " mSensorManager == null", new Object[0]);
        }
        IntentFilter screenFilter = new IntentFilter();
        screenFilter.addAction(SmartDualCardConsts.SYSTEM_STATE_NAME_SCREEN_ON);
        screenFilter.addAction(SmartDualCardConsts.SYSTEM_STATE_NAME_SCREEN_OFF);
        this.mCtx.registerReceiver(this.screenReceiver, screenFilter);
        IntentFilter usbFilter = new IntentFilter();
        usbFilter.addAction("android.intent.action.ACTION_POWER_CONNECTED");
        usbFilter.addAction("android.intent.action.ACTION_POWER_DISCONNECTED");
        this.mCtx.registerReceiver(this.usbReceiver, usbFilter);
        IntentFilter simFilter = new IntentFilter();
        simFilter.addAction(SmartDualCardConsts.SYSTEM_STATE_NAME_SIM_STATE_CHANGED);
        this.mCtx.registerReceiver(this.simReceiver, simFilter);
        this.mBuildBenefitStatisticsChrInfo = new BuildBenefitStatisticsChrInfo();
        this.mBuildSpaceUserStatisticsChrInfo = new BuildSpaceUserStatisticsChrInfo();
        this.mMachineHandler = handler;
        uploadBenefitChr();
        uploadSpaceUserChr();
    }

    /* access modifiers changed from: private */
    public void uploadBenefitChr() {
        long now = System.currentTimeMillis();
        long oobTime = this.locationDao.getOobTime();
        long lastUpdateTime = this.locationDao.getBenefitChrTime();
        long maxTime = oobTime > lastUpdateTime ? oobTime : lastUpdateTime;
        boolean isRetH = false;
        boolean isRetO = false;
        LogUtil.i(false, "now =%{public}s oob time =%{public}s last Benefit CHR time=%{public}s", String.valueOf(now), String.valueOf(oobTime), String.valueOf(lastUpdateTime));
        if (maxTime > 0 && now - maxTime > Constant.MILLISEC_SEVEN_DAYS) {
            isRetH = this.mBuildBenefitStatisticsChrInfo.commitChr(Constant.NAME_FREQLOCATION_HOME);
            isRetO = this.mBuildBenefitStatisticsChrInfo.commitChr(Constant.NAME_FREQLOCATION_OFFICE);
        }
        if (isRetH && isRetO) {
            this.locationDao.updateBenefitChrTime(now);
        }
    }

    /* access modifiers changed from: private */
    public void uploadSpaceUserChr() {
        long now = System.currentTimeMillis();
        long oobTime = this.locationDao.getOobTime();
        long lastUpdateTime = this.locationDao.getSpaceUserChrTime();
        long maxTime = oobTime > lastUpdateTime ? oobTime : lastUpdateTime;
        boolean isRetH = false;
        boolean isRetO = false;
        long uploadTime = Constant.MILLISEC_ONE_MONTH;
        ParameterInfo parameterInfo = this.param;
        if (parameterInfo != null && parameterInfo.isBetaUser()) {
            uploadTime = Constant.MILLISEC_SEVEN_DAYS;
        }
        LogUtil.i(false, "now =%{public}s oob time =%{public}s last SpaceUser CHR time=%{public}s", String.valueOf(now), String.valueOf(oobTime), String.valueOf(lastUpdateTime));
        if (maxTime > 0 && now - maxTime > uploadTime) {
            isRetH = this.mBuildSpaceUserStatisticsChrInfo.commitChr(Constant.NAME_FREQLOCATION_HOME);
            isRetO = this.mBuildSpaceUserStatisticsChrInfo.commitChr(Constant.NAME_FREQLOCATION_OFFICE);
            this.mBuildSpaceUserStatisticsChrInfo.commitChr(Constant.NAME_FREQLOCATION_OTHER);
        }
        if (isRetH && isRetO) {
            this.locationDao.updateSpaceUserChrTime(now);
        }
    }

    /* access modifiers changed from: private */
    public void registerStepCntSensor() {
        if (!this.mAccSensorRegistered.get()) {
            LogUtil.i(false, "registerStepCntSensor, mSensorEventListener", new Object[0]);
            this.mSensorEventListener.reset();
            this.mSensorManager.registerListener(this.mSensorEventListener, this.mStepCntSensor, 3);
            this.mAccSensorRegistered.set(true);
        }
    }

    /* access modifiers changed from: private */
    public void unregisterStepCntSensor() {
        if (this.mAccSensorRegistered.get() && this.mSensorEventListener != null) {
            LogUtil.i(false, "unregisterStepCntSensor, mSensorEventListener", new Object[0]);
            this.mSensorManager.unregisterListener(this.mSensorEventListener);
            this.mAccSensorRegistered.set(false);
        }
    }

    class StepSensorEventListener implements SensorEventListener {
        private int mMotionDetectedCnt = 0;
        private long mSensorEventRcvdTs = -1;

        public StepSensorEventListener() {
            int unused = BehaviorReceiver.this.mLastStepCnt = 0;
        }

        public void reset() {
            int unused = BehaviorReceiver.this.mLastStepCnt = 0;
            this.mMotionDetectedCnt = 0;
            this.mSensorEventRcvdTs = -1;
        }

        public void onSensorChanged(SensorEvent event) {
            if (event != null && event.sensor != null && event.sensor.getType() == 19) {
                long currentTimestamp = System.currentTimeMillis();
                int currentStepCnt = (int) event.values[0];
                if (BehaviorReceiver.this.mMachineHandler != null) {
                    if (currentStepCnt - BehaviorReceiver.this.mLastStepCnt > 0) {
                        this.mMotionDetectedCnt++;
                        if (this.mMotionDetectedCnt == 5) {
                            LogUtil.i(false, "step counter: send MOVE", new Object[0]);
                            this.mMotionDetectedCnt = 0;
                            BehaviorReceiver.this.mMachineHandler.sendEmptyMessage(110);
                        }
                    } else {
                        long j = this.mSensorEventRcvdTs;
                        if (j > 0 && currentTimestamp - j > 5000) {
                            this.mMotionDetectedCnt = 0;
                            LogUtil.i(false, "Step Counter: send STATION", new Object[0]);
                            BehaviorReceiver.this.mMachineHandler.sendEmptyMessage(113);
                        }
                    }
                    int unused = BehaviorReceiver.this.mLastStepCnt = currentStepCnt;
                    this.mSensorEventRcvdTs = currentTimestamp;
                }
            }
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            LogUtil.i(false, "SensorEventListener::onAccuracyChanged, accuracy = %{public}d", Integer.valueOf(accuracy));
        }
    }

    public void checkMoveBtwWifiScans() {
        int offsetStepCnt;
        int i = this.mLastWifiBatch;
        int i2 = batch;
        if (i == i2 && (offsetStepCnt = this.mLastStepCnt - this.mLastWifiStepCnt) > 5) {
            batch = i2 + 1;
            LogUtil.i(false, " checkMoveBtwWifiScans, between two scans, steps=%{public}d", Integer.valueOf(offsetStepCnt));
        }
        this.mLastWifiStepCnt = this.mLastStepCnt;
        this.mLastWifiBatch = batch;
    }

    public void saveBatch() {
        if (this.behaviorDao.update(batch)) {
            LogUtil.i(false, "BehaviorReceiver, batch update success, batch:%{public}d", Integer.valueOf(batch));
            return;
        }
        LogUtil.d(false, "BehaviorReceiver, batch update failure, batch:%{public}d", Integer.valueOf(batch));
    }

    public void stop() {
        LogUtil.d(false, "wifi stop", new Object[0]);
        this.mCtx.unregisterReceiver(this.screenReceiver);
        this.mCtx.unregisterReceiver(this.arReceiver);
    }

    public static int getBatch() {
        return batch;
    }

    public static boolean getScreenState() {
        return isScreenOn;
    }

    public static boolean getArState() {
        return isStationary;
    }

    public boolean stopListen() {
        return true;
    }
}
