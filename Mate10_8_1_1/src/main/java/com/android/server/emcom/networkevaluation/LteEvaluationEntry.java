package com.android.server.emcom.networkevaluation;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.android.server.emcom.EmcomThread;
import com.android.server.emcom.SmartcareConstants;
import java.util.Arrays;

class LteEvaluationEntry {
    static final int CELLULAR_TYPE_2G = 2000;
    static final int CELLULAR_TYPE_3G = 3000;
    static final int CELLULAR_TYPE_LTE = 4000;
    static final int CELLULAR_TYPE_UNKNOWN = 1000;
    private static final int INVALID_3G_SIGNAL_STRENGTH = 32767;
    private static final int INVALID_SIGNAL_STRENGTH = 99;
    private static final int INVALID_VALUE = -1;
    private static final int METRIC_COUNT = 8;
    private static final int METRIC_INTERVAL = 1000;
    private static final int MSG_TIMER_TRIGGERED = 10001;
    private static final String TAG = "LteEvaluation";
    private static volatile LteEvaluationEntry sLteEvaluationEntry;
    private int mCardNumber = 1;
    private int mCellularType;
    private Context mContext;
    private int mDataCardIndex;
    private Handler mOutputHandler;
    private PhoneStateListener[] mPhoneStateListener;
    private boolean mRunning;
    private int[] mSignalStrengthArray;
    private int[] mSignalStrengthStorage;
    private int mSignalStrengthWindowIndex;
    private TelephonyManager mTelephonyManager;
    private Handler mTimeHandler;
    private boolean mWindowFull;

    private class LteTimeHandler extends Handler {
        LteTimeHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (LteEvaluationEntry.this.mTimeHandler != null) {
                switch (msg.what) {
                    case 10001:
                        if (LteEvaluationEntry.this.mRunning) {
                            int signalStrength = LteEvaluationEntry.this.getSpecifiedCellSignalStrength(LteEvaluationEntry.this.mDataCardIndex);
                            if (-1 == signalStrength) {
                                Log.d(LteEvaluationEntry.TAG, "got an invalid signal strength");
                            } else {
                                LteEvaluationEntry.this.mSignalStrengthArray[LteEvaluationEntry.this.mSignalStrengthWindowIndex] = signalStrength;
                                LteEvaluationEntry lteEvaluationEntry = LteEvaluationEntry.this;
                                lteEvaluationEntry.mSignalStrengthWindowIndex = lteEvaluationEntry.mSignalStrengthWindowIndex + 1;
                                if (LteEvaluationEntry.this.mWindowFull) {
                                    LteEvaluationEntry.this.executeRankingProcess(8);
                                    if (LteEvaluationEntry.this.mSignalStrengthWindowIndex >= 8) {
                                        LteEvaluationEntry.this.mSignalStrengthWindowIndex = 0;
                                    }
                                } else if (LteEvaluationEntry.this.mSignalStrengthWindowIndex >= 8) {
                                    LteEvaluationEntry.this.executeRankingProcess(8);
                                    LteEvaluationEntry.this.mWindowFull = true;
                                    LteEvaluationEntry.this.mSignalStrengthWindowIndex = 0;
                                } else {
                                    LteEvaluationEntry.this.executeRankingProcess(LteEvaluationEntry.this.mSignalStrengthWindowIndex);
                                }
                            }
                            LteEvaluationEntry.this.triggerDelayed();
                            break;
                        }
                        break;
                    default:
                        Log.d(LteEvaluationEntry.TAG, "received a illegal message");
                        break;
                }
            }
        }
    }

    private LteEvaluationEntry(Context context, Handler handler) {
        this.mContext = context;
        this.mOutputHandler = handler;
        this.mSignalStrengthArray = new int[8];
        this.mTimeHandler = new LteTimeHandler(EmcomThread.getInstanceLooper());
        this.mCardNumber = TelephonyManager.getDefault().getPhoneCount();
        this.mSignalStrengthStorage = new int[this.mCardNumber];
        this.mPhoneStateListener = new PhoneStateListener[this.mCardNumber];
        for (int i = 0; i < this.mCardNumber; i++) {
            this.mPhoneStateListener[i] = getPhoneStateListener(i);
        }
        if (this.mContext != null) {
            this.mTelephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
        }
    }

    public static LteEvaluationEntry getInstance(Context context, Handler handler) {
        if (sLteEvaluationEntry == null) {
            if (context == null || handler == null) {
                Log.e(TAG, "return null");
                return null;
            }
            synchronized (LteEvaluationEntry.class) {
                if (sLteEvaluationEntry == null) {
                    sLteEvaluationEntry = new LteEvaluationEntry(context, handler);
                }
            }
        }
        return sLteEvaluationEntry;
    }

    void startEvaluation(int subId) {
        if (this.mRunning) {
            Log.d(TAG, "attempt to start a new LTE evaluation while a evaluation is already running");
            return;
        }
        Log.d(TAG, "start LTE evaluation");
        this.mDataCardIndex = subId;
        this.mSignalStrengthWindowIndex = 0;
        Arrays.fill(this.mSignalStrengthArray, 0);
        this.mWindowFull = false;
        this.mRunning = true;
        registerPhoneListener();
        triggerDelayed();
    }

    void stopEvaluation() {
        if (this.mRunning) {
            this.mSignalStrengthWindowIndex = 0;
            this.mWindowFull = false;
            this.mRunning = false;
            unregisterPhoneListener();
            Log.d(TAG, "stopped LTE evaluation");
            return;
        }
        Log.d(TAG, "attempt to stop a stopped LTE evaluation");
    }

    private void triggerDelayed() {
        this.mTimeHandler.sendMessageDelayed(this.mTimeHandler.obtainMessage(10001), 1000);
    }

    private void cleanUpSignalStrengthArray() {
        if (this.mSignalStrengthArray == null) {
            this.mSignalStrengthArray = new int[8];
        }
        Arrays.fill(this.mSignalStrengthArray, 0);
        this.mSignalStrengthWindowIndex = 0;
    }

    private void executeRankingProcess(int length) {
        if (this.mSignalStrengthArray == null || length <= 0 || this.mOutputHandler == null) {
            Log.e(TAG, "try to executeRankingProcess() with null signalStrengthArray or handler");
        } else {
            LteRankingProcess.staticStartRanking(this.mSignalStrengthArray, length, this.mOutputHandler, this.mCellularType);
        }
    }

    private int getSpecifiedCellSignalStrength(int subId) {
        if (subId < 0 || subId > this.mCardNumber - 1) {
            Log.e(TAG, "illegal card index in getSpecifiedCellSignalStrength");
            return -1;
        } else if (this.mSignalStrengthStorage == null) {
            Log.e(TAG, "null signal strength in getSpecifiedCellSignalStrength");
            return -1;
        } else {
            Log.d(TAG, "get a cell signal strength " + this.mSignalStrengthStorage[subId] + " ,with subId " + subId);
            return this.mSignalStrengthStorage[subId];
        }
    }

    private void registerPhoneListener() {
        if (this.mTelephonyManager == null) {
            Log.e(TAG, "null mTelephonyManager, abort");
            return;
        }
        for (int i = 0; i < this.mCardNumber; i++) {
            if (this.mPhoneStateListener[i] != null) {
                this.mTelephonyManager.listen(this.mPhoneStateListener[i], 256);
            }
        }
        Log.d(TAG, "started listening cellular signal strength changes");
    }

    private void unregisterPhoneListener() {
        if (this.mTelephonyManager == null) {
            Log.e(TAG, "null mTelephonyManager, abort");
            return;
        }
        for (int i = 0; i < this.mCardNumber; i++) {
            if (this.mPhoneStateListener[i] != null) {
                this.mTelephonyManager.listen(this.mPhoneStateListener[i], 0);
            }
        }
        Log.d(TAG, "stopped listening cellular signal strength changes");
    }

    private PhoneStateListener getPhoneStateListener(final int subId) {
        return new PhoneStateListener(Integer.valueOf(subId)) {
            public void onDataConnectionStateChanged(int state, int networkType) {
                Log.d(LteEvaluationEntry.TAG, "DataConnectionStateChanged  sub = " + subId + ",state = " + state + ", networkType = " + networkType);
            }

            public void onSignalStrengthsChanged(SignalStrength signalStrength) {
                Log.d(LteEvaluationEntry.TAG, "onSignalStrengthsChanged, signalStrength = " + signalStrength);
                LteEvaluationEntry.this.notifyMobileSignalStrengthChanged(subId, signalStrength);
            }
        };
    }

    private void notifyMobileSignalStrengthChanged(int subId, SignalStrength signal) {
        if (subId < 0 || subId > this.mCardNumber - 1) {
            Log.e(TAG, "illegal subId in notifyMobileSignalStrengthChanged");
        } else {
            updateSignalStrength(subId, signal);
        }
    }

    public void updateSignalStrength(int subId, SignalStrength ss) {
        if (subId < 0 || subId > this.mCardNumber - 1) {
            Log.e(TAG, "illegal subId in updateSignalStrength");
            return;
        }
        Log.d(TAG, "updateSignalStrength with subId " + subId);
        int lteRsrp = ss.getLteRsrp();
        if (99 == lteRsrp || lteRsrp == 0 || SmartcareConstants.INVALID == lteRsrp || -1 == lteRsrp) {
            this.mSignalStrengthStorage[subId] = -1;
            int cdmaDbm = ss.getEvdoDbm();
            if (99 == cdmaDbm || cdmaDbm == 0 || SmartcareConstants.INVALID == cdmaDbm || INVALID_3G_SIGNAL_STRENGTH == cdmaDbm || -1 == cdmaDbm) {
                this.mSignalStrengthStorage[subId] = -1;
                int wcdmaRscp = ss.getWcdmaRscp();
                if (99 == wcdmaRscp || SmartcareConstants.INVALID == wcdmaRscp || wcdmaRscp == 0 || INVALID_3G_SIGNAL_STRENGTH == wcdmaRscp || -1 == wcdmaRscp) {
                    this.mSignalStrengthStorage[subId] = -1;
                    int gsmSignalStrength = ss.getGsmSignalStrength();
                    if (99 == gsmSignalStrength || gsmSignalStrength == 0 || SmartcareConstants.INVALID == gsmSignalStrength || -1 == gsmSignalStrength) {
                        this.mSignalStrengthStorage[subId] = -1;
                        this.mCellularType = 1000;
                        Log.e(TAG, "updateSignalStrength() triggered but no data was updated!");
                        return;
                    }
                    this.mSignalStrengthStorage[subId] = gsmSignalStrength;
                    this.mCellularType = 2000;
                    Log.d(TAG, "updated a signal strength for 2G network " + gsmSignalStrength);
                    return;
                }
                this.mSignalStrengthStorage[subId] = wcdmaRscp;
                this.mCellularType = CELLULAR_TYPE_3G;
                Log.d(TAG, "updated a signal strength for wcdma 3G network " + wcdmaRscp);
                return;
            }
            this.mSignalStrengthStorage[subId] = cdmaDbm;
            this.mCellularType = CELLULAR_TYPE_3G;
            Log.d(TAG, "updated a signal strength for cdma 3G network " + cdmaDbm);
            return;
        }
        this.mSignalStrengthStorage[subId] = lteRsrp;
        this.mCellularType = 4000;
        Log.d(TAG, "updated a signal strength for 4G network " + lteRsrp);
    }
}
