package com.android.server.wifi;

import android.content.Context;
import android.os.Looper;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class SarManager {
    private static final String TAG = "WifiSarManager";
    private boolean mCellOn = false;
    private final Context mContext;
    private int mCurrentSarScenario = 0;
    private boolean mEnableSarTxPowerLimit;
    private final Looper mLooper;
    private final WifiPhoneStateListener mPhoneStateListener;
    private final TelephonyManager mTelephonyManager;
    private boolean mVerboseLoggingEnabled = true;
    private final WifiNative mWifiNative;
    private boolean mWifiStaEnabled = false;

    private class WifiPhoneStateListener extends PhoneStateListener {
        WifiPhoneStateListener(Looper looper) {
            super(looper);
        }

        public void onCallStateChanged(int state, String incomingNumber) {
            String str = SarManager.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Received Phone State Change: ");
            stringBuilder.append(state);
            Log.d(str, stringBuilder.toString());
            if (SarManager.this.mEnableSarTxPowerLimit) {
                SarManager.this.onCellStateChangeEvent(state);
            }
        }
    }

    SarManager(Context context, TelephonyManager telephonyManager, Looper looper, WifiNative wifiNative) {
        this.mContext = context;
        this.mTelephonyManager = telephonyManager;
        this.mWifiNative = wifiNative;
        this.mLooper = looper;
        this.mPhoneStateListener = new WifiPhoneStateListener(looper);
        registerListeners();
    }

    private void registerListeners() {
        this.mEnableSarTxPowerLimit = this.mContext.getResources().getBoolean(17957081);
        if (this.mEnableSarTxPowerLimit) {
            Log.d(TAG, "Registering Listeners for the SAR Manager");
            registerPhoneListener();
        }
    }

    private void onCellStateChangeEvent(int state) {
        boolean currentCellOn = this.mCellOn;
        switch (state) {
            case 0:
                this.mCellOn = false;
                break;
            case 1:
            case 2:
                this.mCellOn = true;
                break;
            default:
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Invalid Cell State: ");
                stringBuilder.append(state);
                Log.e(str, stringBuilder.toString());
                break;
        }
        if (this.mCellOn != currentCellOn) {
            updateSarScenario();
        }
    }

    public void setClientWifiState(int state) {
        if (this.mEnableSarTxPowerLimit) {
            if (state == 1 && this.mWifiStaEnabled) {
                this.mWifiStaEnabled = false;
            } else if (state == 3 && !this.mWifiStaEnabled) {
                this.mWifiStaEnabled = true;
                sendTxPowerScenario(this.mCurrentSarScenario);
            }
        }
    }

    public void enableVerboseLogging(int verbose) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Inside enableVerboseLogging: ");
        stringBuilder.append(verbose);
        Log.d(str, stringBuilder.toString());
        if (verbose > 0) {
            this.mVerboseLoggingEnabled = true;
        } else {
            this.mVerboseLoggingEnabled = false;
        }
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("*** WiFi SAR Manager Dump ***");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Current SAR Scenario is ");
        stringBuilder.append(scenarioToString(this.mCurrentSarScenario));
        pw.println(stringBuilder.toString());
    }

    private void registerPhoneListener() {
        Log.i(TAG, "Registering for telephony call state changes");
        this.mTelephonyManager.listen(this.mPhoneStateListener, 32);
    }

    private void updateSarScenario() {
        int newSarScenario;
        if (this.mCellOn) {
            newSarScenario = 1;
        } else {
            newSarScenario = 0;
        }
        if (newSarScenario != this.mCurrentSarScenario) {
            if (this.mWifiStaEnabled) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Sending SAR Scenario #");
                stringBuilder.append(scenarioToString(newSarScenario));
                Log.d(str, stringBuilder.toString());
                sendTxPowerScenario(newSarScenario);
            }
            this.mCurrentSarScenario = newSarScenario;
        }
    }

    private void sendTxPowerScenario(int newSarScenario) {
        if (!this.mWifiNative.selectTxPowerScenario(newSarScenario)) {
            Log.e(TAG, "Failed to set TX power scenario");
        }
    }

    private String scenarioToString(int scenario) {
        switch (scenario) {
            case 0:
                return "TX_POWER_SCENARIO_NORMAL";
            case 1:
                return "TX_POWER_SCENARIO_VOICE_CALL";
            default:
                return "Invalid Scenario";
        }
    }
}
