package com.android.server.wifi;

import android.content.ContentResolver;
import android.content.Context;
import android.os.SystemProperties;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class WifiSettingsStore {
    private static final String TAG = "WifiSettingsStore";
    static final int WIFI_DISABLED = 0;
    private static final int WIFI_DISABLED_AIRPLANE_ON = 3;
    static final int WIFI_ENABLED = 1;
    private static final int WIFI_ENABLED_AIRPLANE_OVERRIDE = 2;
    private boolean mAirplaneModeOn = false;
    private boolean mCheckSavedStateAtBoot = false;
    private final Context mContext;
    private int mPersistWifiState = 0;
    private boolean mScanAlwaysAvailable;

    WifiSettingsStore(Context context) {
        this.mContext = context;
        this.mAirplaneModeOn = getPersistedAirplaneModeOn();
        this.mPersistWifiState = getPersistedWifiState();
        this.mScanAlwaysAvailable = getPersistedScanAlwaysAvailable();
    }

    /* JADX WARNING: Missing block: B:16:0x001d, code:
            return r1;
     */
    /* JADX WARNING: Missing block: B:22:0x0025, code:
            return r1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized boolean isWifiToggleEnabled() {
        boolean z = true;
        if (!this.mCheckSavedStateAtBoot) {
            this.mCheckSavedStateAtBoot = true;
            if (testAndClearWifiSavedState()) {
                return true;
            }
        }
        if (this.mAirplaneModeOn) {
            if (this.mPersistWifiState != 2) {
                z = false;
            }
        } else if (this.mPersistWifiState == 0) {
            z = false;
        }
    }

    public synchronized boolean isAirplaneModeOn() {
        return this.mAirplaneModeOn;
    }

    public synchronized boolean isScanAlwaysAvailable() {
        boolean z;
        z = !this.mAirplaneModeOn && this.mScanAlwaysAvailable;
        return z;
    }

    /* JADX WARNING: Missing block: B:17:0x0022, code:
            return true;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized boolean handleWifiToggled(boolean wifiEnabled) {
        if (this.mAirplaneModeOn && !isAirplaneToggleable()) {
            return false;
        }
        if (!wifiEnabled) {
            persistWifiState(0);
        } else if (this.mAirplaneModeOn) {
            persistWifiState(2);
        } else {
            persistWifiState(1);
        }
    }

    /* JADX WARNING: Missing block: B:20:0x0056, code:
            return true;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    synchronized boolean handleAirplaneModeToggled() {
        if (isAirplaneSensitive()) {
            this.mAirplaneModeOn = getPersistedAirplaneModeOn();
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("mAirplaneModeOn:");
            stringBuilder.append(this.mAirplaneModeOn);
            stringBuilder.append(", mPersistWifiState:");
            stringBuilder.append(this.mPersistWifiState);
            Log.d(str, stringBuilder.toString());
            if (this.mAirplaneModeOn) {
                if (this.mPersistWifiState == 1) {
                    persistWifiState(3);
                }
            } else if (testAndClearWifiSavedState() || this.mPersistWifiState == 2) {
                persistWifiState(1);
            }
        } else {
            Log.d(TAG, "airplane not sensitive");
            return false;
        }
    }

    synchronized void handleWifiScanAlwaysAvailableToggled() {
        this.mScanAlwaysAvailable = getPersistedScanAlwaysAvailable();
    }

    void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mPersistWifiState ");
        stringBuilder.append(this.mPersistWifiState);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mAirplaneModeOn ");
        stringBuilder.append(this.mAirplaneModeOn);
        pw.println(stringBuilder.toString());
    }

    private void persistWifiState(int state) {
        ContentResolver cr = this.mContext.getContentResolver();
        this.mPersistWifiState = state;
        Global.putInt(cr, "wifi_on", state);
    }

    private boolean isAirplaneSensitive() {
        String airplaneModeRadios = Global.getString(this.mContext.getContentResolver(), "airplane_mode_radios");
        return airplaneModeRadios == null || airplaneModeRadios.contains("wifi");
    }

    private boolean isAirplaneToggleable() {
        String toggleableRadios = Global.getString(this.mContext.getContentResolver(), "airplane_mode_toggleable_radios");
        return toggleableRadios != null && toggleableRadios.contains("wifi");
    }

    private boolean testAndClearWifiSavedState() {
        int wifiSavedState = getWifiSavedState();
        if (wifiSavedState == 1) {
            setWifiSavedState(0);
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("wifiSavedState:");
        stringBuilder.append(wifiSavedState);
        Log.d(str, stringBuilder.toString());
        if (wifiSavedState == 1) {
            return true;
        }
        return false;
    }

    public void setWifiSavedState(int state) {
        Global.putInt(this.mContext.getContentResolver(), "wifi_saved_state", state);
    }

    public int getWifiSavedState() {
        try {
            return Global.getInt(this.mContext.getContentResolver(), "wifi_saved_state");
        } catch (SettingNotFoundException e) {
            return 0;
        }
    }

    private int getPersistedWifiState() {
        ContentResolver cr = this.mContext.getContentResolver();
        try {
            return Global.getInt(cr, "wifi_on");
        } catch (SettingNotFoundException e) {
            Global.putInt(cr, "wifi_on", 0);
            return 0;
        }
    }

    private boolean getPersistedAirplaneModeOn() {
        return Global.getInt(this.mContext.getContentResolver(), "airplane_mode_on", 0) == 1;
    }

    private boolean getPersistedScanAlwaysAvailable() {
        boolean z = true;
        if (Global.getInt(this.mContext.getContentResolver(), "wifi_scan_always_enabled", 0) != 1) {
            z = false;
        }
        boolean ret = z;
        if (!ret || !"factory".equals(SystemProperties.get("ro.runmode", "normal"))) {
            return ret;
        }
        Log.d(TAG, "factory version, WIFI_SCAN_ALWAYS_AVAILABLE doesnt work");
        return false;
    }

    public int getLocationModeSetting(Context context) {
        return Secure.getInt(context.getContentResolver(), "location_mode", 0);
    }

    public int getLocationModeSetting(Context context, int userid) {
        return Secure.getIntForUser(context.getContentResolver(), "location_mode", 0, userid);
    }
}
