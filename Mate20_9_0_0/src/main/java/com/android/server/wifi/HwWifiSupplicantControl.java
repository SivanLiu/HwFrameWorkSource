package com.android.server.wifi;

import android.os.Environment;
import android.telephony.TelephonyManager;
import android.util.LocalLog;

public class HwWifiSupplicantControl extends WifiSupplicantControl {
    private static final String DEFAULT_CERTIFICATE_PATH;
    public static final String TAG = "HwWifiSupplicantControl";
    private WifiNative mWifiNative;

    static {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(Environment.getDataDirectory().getPath());
        stringBuilder.append("/wapi_certificate");
        DEFAULT_CERTIFICATE_PATH = stringBuilder.toString();
    }

    HwWifiSupplicantControl(TelephonyManager telephonyManager, WifiNative wifiNative, LocalLog localLog) {
        super(telephonyManager, wifiNative, localLog);
        this.mWifiNative = wifiNative;
    }
}
