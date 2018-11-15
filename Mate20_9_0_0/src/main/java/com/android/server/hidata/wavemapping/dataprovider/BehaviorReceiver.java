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
import com.android.server.hidata.wavemapping.dao.BehaviorDAO;
import com.android.server.hidata.wavemapping.dao.LocationDAO;
import com.android.server.hidata.wavemapping.entity.ParameterInfo;
import com.android.server.hidata.wavemapping.util.LogUtil;
import com.android.server.hidata.wavemapping.util.ShowToast;
import java.util.concurrent.atomic.AtomicBoolean;

public class BehaviorReceiver {
    private static final String PG_AR_STATE_ACTION = "com.huawei.intent.action.PG_AR_STATE_ACTION";
    private static final String PG_RECEIVER_PERMISSION = "com.huawei.powergenie.receiverPermission";
    private static final int STEP_INCREASE_THRESHOLD = 5;
    private static final long STEP_RELEASE_THRESHOLD = 5000;
    public static final String TAG;
    private static int batch = 1;
    private static boolean mIsStationary = false;
    private static boolean oldSimReady = false;
    private static boolean screenState = true;
    private BroadcastReceiver arReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                boolean mIsStationaryTmp = intent.getBooleanExtra("stationary", false);
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Stationary =");
                stringBuilder.append(mIsStationaryTmp);
                LogUtil.i(stringBuilder.toString());
                if (BehaviorReceiver.mIsStationary != mIsStationaryTmp) {
                    BehaviorReceiver.access$108();
                    BehaviorReceiver.mIsStationary = mIsStationaryTmp;
                    if (BehaviorReceiver.this.mMachineHandler != null) {
                        if (BehaviorReceiver.mIsStationary) {
                            LogUtil.i("arReceiver: send STATION");
                            BehaviorReceiver.this.mMachineHandler.sendEmptyMessage(113);
                        } else {
                            LogUtil.i("arReceiver send MOVE");
                            BehaviorReceiver.this.mMachineHandler.sendEmptyMessage(110);
                            ShowToast.showToast("Moving by AR");
                        }
                    }
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("AR Status Changed, batch =");
                    stringBuilder.append(BehaviorReceiver.batch);
                    LogUtil.i(stringBuilder.toString());
                }
            }
        }
    };
    private BehaviorDAO behaviorDAO;
    private AtomicBoolean mAccSensorRegistered = new AtomicBoolean(false);
    private BuildBenefitStatisticsChrInfo mBuildBenefitStatisticsChrInfo = null;
    private BuildSpaceUserStatisticsChrInfo mBuildSpaceUserStatisticsChrInfo = null;
    private Context mCtx;
    private FrequentLocation mFrequentLocation = null;
    private int mLastStepCnt;
    private int mLastWifiBatch;
    private int mLastWifiStepCnt;
    private LocationDAO mLocationDAO;
    private Handler mMachineHandler;
    private final StepSensorEventListener mSensorEventListener = new StepSensorEventListener();
    private SensorManager mSensorManager;
    private Sensor mStepCntSensor;
    private ParameterInfo param;
    private int savedBatch = 0;
    private BroadcastReceiver screenReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            LogUtil.i("receive screen state");
            StringBuilder stringBuilder;
            if ("android.intent.action.SCREEN_ON".equals(intent.getAction())) {
                BehaviorReceiver.access$108();
                BehaviorReceiver.screenState = true;
                stringBuilder = new StringBuilder();
                stringBuilder.append("BehaviorReceiver screenOn = ");
                stringBuilder.append(BehaviorReceiver.screenState);
                stringBuilder.append(", batch = ");
                stringBuilder.append(BehaviorReceiver.batch);
                LogUtil.i(stringBuilder.toString());
                BehaviorReceiver.this.registerStepCntSensor();
            } else if ("android.intent.action.SCREEN_OFF".equals(intent.getAction())) {
                BehaviorReceiver.access$108();
                BehaviorReceiver.screenState = false;
                stringBuilder = new StringBuilder();
                stringBuilder.append("BehaviorReceiver screenOff = ");
                stringBuilder.append(BehaviorReceiver.screenState);
                stringBuilder.append(", batch = ");
                stringBuilder.append(BehaviorReceiver.batch);
                LogUtil.i(stringBuilder.toString());
                BehaviorReceiver.this.saveBatch();
                BehaviorReceiver.this.unregisterStepCntSensor();
            }
        }
    };
    private BroadcastReceiver simReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            try {
                if ("android.intent.action.SIM_STATE_CHANGED".equals(intent.getAction())) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("BehaviorReceiver SIM state changed ");
                    stringBuilder.append(intent.getAction());
                    LogUtil.i(stringBuilder.toString());
                    if (BehaviorReceiver.this.mMachineHandler != null) {
                        TelephonyManager tm = (TelephonyManager) BehaviorReceiver.this.mCtx.getSystemService("phone");
                        if (tm == null || 5 != tm.getSimState()) {
                            LogUtil.i("SIM invalid");
                            if (BehaviorReceiver.oldSimReady) {
                                BehaviorReceiver.this.mMachineHandler.sendEmptyMessage(94);
                            }
                            BehaviorReceiver.oldSimReady = false;
                            return;
                        }
                        LogUtil.i("SIM ready");
                        if (!BehaviorReceiver.oldSimReady) {
                            BehaviorReceiver.this.mMachineHandler.sendEmptyMessage(94);
                        }
                        BehaviorReceiver.oldSimReady = true;
                    }
                }
            } catch (Exception e) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("simReceiver:");
                stringBuilder2.append(e.getMessage());
                LogUtil.e(stringBuilder2.toString());
            }
        }
    };
    private BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            LogUtil.i("receive power state");
            if ("android.intent.action.ACTION_POWER_CONNECTED".equals(intent.getAction())) {
                LogUtil.i("BehaviorReceiver Power Connected ");
                BehaviorReceiver.this.uploadBenefitCHR();
                BehaviorReceiver.this.uploadSpaceUserCHR();
            } else if ("android.intent.action.ACTION_POWER_DISCONNECTED".equals(intent.getAction())) {
                LogUtil.i("BehaviorReceiver Power Disconnected ");
                BehaviorReceiver.this.uploadBenefitCHR();
                BehaviorReceiver.this.uploadSpaceUserCHR();
            }
        }
    };

    class StepSensorEventListener implements SensorEventListener {
        private int mMotionDetectedCnt = 0;
        private long mSensorEventRcvdTs = -1;

        public StepSensorEventListener() {
            BehaviorReceiver.this.mLastStepCnt = 0;
        }

        public void reset() {
            BehaviorReceiver.this.mLastStepCnt = 0;
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
                            LogUtil.i("step counter: send MOVE");
                            this.mMotionDetectedCnt = 0;
                            BehaviorReceiver.this.mMachineHandler.sendEmptyMessage(110);
                        }
                    } else if (this.mSensorEventRcvdTs > 0 && currentTimestamp - this.mSensorEventRcvdTs > BehaviorReceiver.STEP_RELEASE_THRESHOLD) {
                        this.mMotionDetectedCnt = 0;
                        LogUtil.i("Step Counter: send STATION");
                        BehaviorReceiver.this.mMachineHandler.sendEmptyMessage(113);
                    }
                    BehaviorReceiver.this.mLastStepCnt = currentStepCnt;
                    this.mSensorEventRcvdTs = currentTimestamp;
                }
            }
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("SensorEventListener::onAccuracyChanged, accuracy = ");
            stringBuilder.append(accuracy);
            LogUtil.i(stringBuilder.toString());
        }
    }

    static /* synthetic */ int access$108() {
        int i = batch;
        batch = i + 1;
        return i;
    }

    static {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("WMapping.");
        stringBuilder.append(BehaviorReceiver.class.getSimpleName());
        TAG = stringBuilder.toString();
    }

    public BehaviorReceiver(Handler handler) {
        LogUtil.i("BehaviorReceiverHandler");
        this.mCtx = ContextManager.getInstance().getContext();
        try {
            this.param = ParamManager.getInstance().getParameterInfo();
            this.behaviorDAO = new BehaviorDAO();
            this.mLocationDAO = new LocationDAO();
            this.savedBatch = this.behaviorDAO.getBatch();
            if (this.savedBatch == 0) {
                this.behaviorDAO.insert(batch);
                LogUtil.d(" no default record, init...");
            } else {
                batch = this.savedBatch + 1;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(" default record, history batch=");
                stringBuilder.append(this.savedBatch);
                LogUtil.i(stringBuilder.toString());
            }
            this.mCtx.registerReceiver(this.arReceiver, new IntentFilter(PG_AR_STATE_ACTION), PG_RECEIVER_PERMISSION, null);
            this.mSensorManager = (SensorManager) this.mCtx.getSystemService("sensor");
            if (this.mSensorManager != null) {
                this.mStepCntSensor = this.mSensorManager.getDefaultSensor(19);
                registerStepCntSensor();
            } else {
                LogUtil.e(" mSensorManager == null");
            }
            IntentFilter screenFilter = new IntentFilter();
            screenFilter.addAction("android.intent.action.SCREEN_ON");
            screenFilter.addAction("android.intent.action.SCREEN_OFF");
            this.mCtx.registerReceiver(this.screenReceiver, screenFilter);
            IntentFilter usbFilter = new IntentFilter();
            usbFilter.addAction("android.intent.action.ACTION_POWER_CONNECTED");
            usbFilter.addAction("android.intent.action.ACTION_POWER_DISCONNECTED");
            this.mCtx.registerReceiver(this.usbReceiver, usbFilter);
            IntentFilter simFilter = new IntentFilter();
            simFilter.addAction("android.intent.action.SIM_STATE_CHANGED");
            this.mCtx.registerReceiver(this.simReceiver, simFilter);
            this.mBuildBenefitStatisticsChrInfo = new BuildBenefitStatisticsChrInfo();
            this.mBuildSpaceUserStatisticsChrInfo = new BuildSpaceUserStatisticsChrInfo();
            this.mMachineHandler = handler;
            uploadBenefitCHR();
            uploadSpaceUserCHR();
        } catch (Exception e) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("start:");
            stringBuilder2.append(e.getMessage());
            LogUtil.e(stringBuilder2.toString());
        }
    }

    private void uploadBenefitCHR() {
        long now = System.currentTimeMillis();
        long oobtime = this.mLocationDAO.getOOBTime();
        long lastupdatetime = this.mLocationDAO.getBenefitCHRTime();
        long maxtime = oobtime > lastupdatetime ? oobtime : lastupdatetime;
        boolean retH = false;
        boolean retO = false;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("now =");
        stringBuilder.append(now);
        stringBuilder.append(" oob time =");
        stringBuilder.append(oobtime);
        stringBuilder.append(" last Benefit CHR time=");
        stringBuilder.append(lastupdatetime);
        LogUtil.i(stringBuilder.toString());
        if (maxtime > 0 && now - maxtime > Constant.MILLISEC_SEVEN_DAYS) {
            retH = this.mBuildBenefitStatisticsChrInfo.commitCHR(Constant.NAME_FREQLOCATION_HOME);
            retO = this.mBuildBenefitStatisticsChrInfo.commitCHR(Constant.NAME_FREQLOCATION_OFFICE);
        }
        if (retH && retO) {
            this.mLocationDAO.updateBenefitCHRTime(now);
        }
    }

    private void uploadSpaceUserCHR() {
        long now = System.currentTimeMillis();
        long oobtime = this.mLocationDAO.getOOBTime();
        long lastupdatetime = this.mLocationDAO.getSpaceUserCHRTime();
        long maxtime = oobtime > lastupdatetime ? oobtime : lastupdatetime;
        boolean retH = false;
        boolean retO = false;
        long uploadTime = Constant.MILLISEC_ONE_MONTH;
        if (this.param != null && this.param.isBetaUser()) {
            uploadTime = Constant.MILLISEC_SEVEN_DAYS;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("now =");
        stringBuilder.append(now);
        stringBuilder.append(" oob time =");
        stringBuilder.append(oobtime);
        stringBuilder.append(" last SpaceUser CHR time=");
        stringBuilder.append(lastupdatetime);
        LogUtil.i(stringBuilder.toString());
        if (maxtime > 0 && now - maxtime > uploadTime) {
            retH = this.mBuildSpaceUserStatisticsChrInfo.commitCHR(Constant.NAME_FREQLOCATION_HOME);
            retO = this.mBuildSpaceUserStatisticsChrInfo.commitCHR(Constant.NAME_FREQLOCATION_OFFICE);
        }
        if (retH && retO) {
            this.mLocationDAO.updateSpaceUserCHRTime(now);
        }
    }

    private void registerStepCntSensor() {
        if (!this.mAccSensorRegistered.get()) {
            LogUtil.i("registerStepCntSensor, mSensorEventListener");
            this.mSensorEventListener.reset();
            this.mSensorManager.registerListener(this.mSensorEventListener, this.mStepCntSensor, 3);
            this.mAccSensorRegistered.set(true);
        }
    }

    private void unregisterStepCntSensor() {
        if (this.mAccSensorRegistered.get() && this.mSensorEventListener != null) {
            LogUtil.i("unregisterStepCntSensor, mSensorEventListener");
            this.mSensorManager.unregisterListener(this.mSensorEventListener);
            this.mAccSensorRegistered.set(false);
        }
    }

    public void checkMoveBtwWifiScans() {
        if (this.mLastWifiBatch == batch) {
            int offsetStepCnt = this.mLastStepCnt - this.mLastWifiStepCnt;
            if (offsetStepCnt > 5) {
                batch++;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(" checkMoveBtwWifiScans, between two scans, steps=");
                stringBuilder.append(offsetStepCnt);
                LogUtil.i(stringBuilder.toString());
            }
        }
        this.mLastWifiStepCnt = this.mLastStepCnt;
        this.mLastWifiBatch = batch;
    }

    private void saveBatch() {
        StringBuilder stringBuilder;
        if (this.behaviorDAO.update(batch)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("BehaviorReceiver, batch update success, batch:");
            stringBuilder.append(batch);
            LogUtil.i(stringBuilder.toString());
            return;
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("BehaviorReceiver, batch update failure, batch :");
        stringBuilder.append(batch);
        LogUtil.d(stringBuilder.toString());
    }

    public void stop() {
        LogUtil.d("wifi stop");
        this.mCtx.unregisterReceiver(this.screenReceiver);
        this.mCtx.unregisterReceiver(this.arReceiver);
    }

    public static int getBatch() {
        return batch;
    }

    public static boolean getScrnState() {
        return screenState;
    }

    public static boolean getArState() {
        return mIsStationary;
    }

    public boolean stopListen() {
        return true;
    }
}
