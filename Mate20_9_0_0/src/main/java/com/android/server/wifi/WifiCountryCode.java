package com.android.server.wifi;

import android.text.TextUtils;
import android.util.Log;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class WifiCountryCode {
    private static final String TAG = "WifiCountryCode";
    private boolean DBG = false;
    private String mCurrentCountryCode = null;
    private String mDefaultCountryCode = null;
    private boolean mReady = false;
    private boolean mRevertCountryCodeOnCellularLoss;
    private String mTelephonyCountryCode = null;
    private final WifiNative mWifiNative;

    public WifiCountryCode(WifiNative wifiNative, String oemDefaultCountryCode, boolean revertCountryCodeOnCellularLoss) {
        this.mWifiNative = wifiNative;
        this.mRevertCountryCodeOnCellularLoss = revertCountryCodeOnCellularLoss;
        if (!TextUtils.isEmpty(oemDefaultCountryCode)) {
            this.mDefaultCountryCode = oemDefaultCountryCode.toUpperCase();
        } else if (this.mRevertCountryCodeOnCellularLoss) {
            Log.w(TAG, "config_wifi_revert_country_code_on_cellular_loss is set, but there is no default country code.");
            this.mRevertCountryCodeOnCellularLoss = false;
            return;
        }
        if (this.mRevertCountryCodeOnCellularLoss) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Country code will be reverted to ");
            stringBuilder.append(this.mDefaultCountryCode);
            stringBuilder.append(" on MCC loss");
            Log.d(str, stringBuilder.toString());
        }
    }

    public void enableVerboseLogging(int verbose) {
        if (verbose > 0) {
            this.DBG = true;
        } else {
            this.DBG = false;
        }
    }

    public synchronized void airplaneModeEnabled() {
        if (this.DBG) {
            Log.d(TAG, "Airplane Mode Enabled");
        }
        this.mTelephonyCountryCode = null;
    }

    public synchronized void setReadyForChange(boolean ready) {
        if (this.DBG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Set ready: ");
            stringBuilder.append(ready);
            Log.d(str, stringBuilder.toString());
        }
        this.mReady = ready;
        if (this.mReady) {
            updateCountryCode();
        }
    }

    public synchronized boolean setCountryCode(String countryCode) {
        if (this.DBG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Receive set country code request: ");
            stringBuilder.append(countryCode);
            Log.d(str, stringBuilder.toString());
        }
        if (!TextUtils.isEmpty(countryCode)) {
            this.mTelephonyCountryCode = countryCode.toUpperCase();
        } else if (this.mRevertCountryCodeOnCellularLoss) {
            if (this.DBG) {
                Log.d(TAG, "Received empty country code, reset to default country code");
            }
            this.mTelephonyCountryCode = null;
        }
        if (this.mReady) {
            updateCountryCode();
        }
        return true;
    }

    public synchronized String getCountryCodeSentToDriver() {
        return this.mCurrentCountryCode;
    }

    public synchronized String getCountryCode() {
        return pickCountryCode();
    }

    public synchronized void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        StringBuilder stringBuilder;
        if (this.mCurrentCountryCode != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("CountryCode sent to driver: ");
            stringBuilder.append(this.mCurrentCountryCode);
            pw.println(stringBuilder.toString());
        } else if (pickCountryCode() != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("CountryCode: ");
            stringBuilder.append(pickCountryCode());
            stringBuilder.append(" was not sent to driver");
            pw.println(stringBuilder.toString());
        } else {
            pw.println("CountryCode was not initialized");
        }
    }

    private void updateCountryCode() {
        if (this.DBG) {
            Log.d(TAG, "Update country code");
        }
        String country = pickCountryCode();
        if (country != null) {
            setCountryCodeNative(country);
        }
    }

    private String pickCountryCode() {
        if (this.mTelephonyCountryCode != null) {
            return this.mTelephonyCountryCode;
        }
        if (this.mDefaultCountryCode != null) {
            return this.mDefaultCountryCode;
        }
        return null;
    }

    private boolean setCountryCodeNative(String country) {
        String str;
        StringBuilder stringBuilder;
        if (this.mWifiNative.setCountryCode(this.mWifiNative.getClientInterfaceName(), country)) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Succeeded to set country code to: ");
            stringBuilder.append(country);
            Log.d(str, stringBuilder.toString());
            this.mCurrentCountryCode = country;
            return true;
        }
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("Failed to set country code to: ");
        stringBuilder.append(country);
        Log.d(str, stringBuilder.toString());
        return false;
    }
}
