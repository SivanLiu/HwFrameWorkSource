package com.android.server.wifi;

import android.net.wifi.WifiLinkLayerStats;
import android.util.Log;
import com.huawei.ncdft.HwWifiDFTConnManager;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class HwWiFiLogUtils {
    private static final String DEF_CONSOLE_MS_START = "200";
    private static final String DEF_CONSOLE_MS_STOP = "0";
    private static final String TAG = "HwWiFiLogUtils";
    private static final int VERSION_STATE_BETA = 1;
    private static final int VERSION_STATE_COMM = 2;
    private static final int VERSION_STATE_INIT = 0;
    private static String WIFI_FWLOG_FILE = "/sys/bcm-dhd/dhd_watchdog_time";
    private static HwWiFiLogUtils hwWiFiLogUtils = new HwWiFiLogUtils();
    private boolean mAllowModifyFwLog = false;
    private int mVersionState = 0;
    private WifiNative mWifiNative = null;

    private HwWiFiLogUtils() {
    }

    public static HwWiFiLogUtils getDefault() {
        return hwWiFiLogUtils;
    }

    public static void init(WifiNative wifiNative) {
        getDefault().setWifiNative(wifiNative);
    }

    public void startLinkLayerLog() {
        if (isVersionBeta()) {
            getWifiLinkLayerStatsEx();
        }
    }

    public void stopLinkLayerLog() {
        if (isVersionBeta()) {
            getWifiLinkLayerStatsEx();
            sleep(200);
        }
    }

    private boolean isVersionBeta() {
        if (this.mVersionState == 0) {
            int i;
            if (HwWifiDFTConnManager.getInstance().isCommercialUser()) {
                i = 2;
            } else {
                i = 1;
            }
            this.mVersionState = i;
        }
        if (this.mVersionState == 1) {
            return true;
        }
        return false;
    }

    private void setWifiNative(WifiNative wifiNative) {
        this.mWifiNative = wifiNative;
    }

    private void getWifiLinkLayerStatsEx() {
        if (this.mWifiNative != null) {
            WifiLinkLayerStats stats = this.mWifiNative.getWifiLinkLayerStats("wlan0");
            if (stats != null) {
                Log.d(TAG, stats.toString());
            }
        }
    }

    private static void startFirmwareLogCap() {
        writeToFile(WIFI_FWLOG_FILE, DEF_CONSOLE_MS_START);
    }

    private static void stopFirmwareLogCap() {
        writeToFile(WIFI_FWLOG_FILE, DEF_CONSOLE_MS_STOP);
    }

    private static void writeToFile(String fileName, String value) {
        IOException e;
        Throwable th;
        File file = new File(fileName);
        if (file.exists() && (file.canWrite() ^ 1) == 0) {
            FileOutputStream fileOutputStream = null;
            try {
                FileOutputStream writer = new FileOutputStream(fileName, false);
                try {
                    writer.write(value.getBytes("US-ASCII"));
                    if (writer != null) {
                        try {
                            writer.close();
                        } catch (IOException e2) {
                        }
                    }
                    fileOutputStream = writer;
                } catch (IOException e3) {
                    e = e3;
                    fileOutputStream = writer;
                    try {
                        Log.d(TAG, e.toString());
                        if (fileOutputStream != null) {
                            try {
                                fileOutputStream.close();
                            } catch (IOException e4) {
                            }
                        }
                        return;
                    } catch (Throwable th2) {
                        th = th2;
                        if (fileOutputStream != null) {
                            try {
                                fileOutputStream.close();
                            } catch (IOException e5) {
                            }
                        }
                        throw th;
                    }
                } catch (Throwable th3) {
                    th = th3;
                    fileOutputStream = writer;
                    if (fileOutputStream != null) {
                        fileOutputStream.close();
                    }
                    throw th;
                }
            } catch (IOException e6) {
                e = e6;
                Log.d(TAG, e.toString());
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
                return;
            }
            return;
        }
        Log.d(TAG, fileName + " no premission");
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
        }
    }

    public void firmwareLog(boolean enable) {
        if (enable) {
            startFirmwareLogCap();
        } else {
            stopFirmwareLogCap();
        }
    }
}
