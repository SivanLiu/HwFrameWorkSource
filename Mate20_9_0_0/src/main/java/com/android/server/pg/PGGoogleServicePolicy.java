package com.android.server.pg;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.net.NetworkInfo;
import android.net.util.NetworkConstants;
import android.os.Handler;
import android.os.SystemProperties;
import android.os.WorkSource;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.android.server.HwServiceFactory;
import com.android.server.display.DisplayTransformManager;
import com.android.server.power.IHwShutdownThread;
import com.huawei.pgmng.log.LogPower;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class PGGoogleServicePolicy {
    private static final String GOOGLE_GMS_PAC = "com.google.android.gms";
    private static final String GOOGLE_GSF_PAC = "com.google.android.gsf";
    private static final Boolean IS_CHINA_MARKET;
    private static final long STATIC_WAKELOCK_CHECK_TIME_MAX = 1800000;
    private static final long STATIC_WAKELOCK_CHECK_TIME_MIN = 60000;
    private static final String TAG = "PGGoogleServicePolicy";
    private static final String US_GOOGLE_URL = "http://www.google.com";
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (PGGoogleServicePolicy.this.mEnabled) {
                String action = intent.getAction();
                if (action != null) {
                    if (action.equals("android.intent.action.BOOT_COMPLETED")) {
                        PGGoogleServicePolicy.this.mWakeLockHandler.removeCallbacks(PGGoogleServicePolicy.this.mWakelockMonitor);
                        PGGoogleServicePolicy.this.mWakeLockHandler.postDelayed(PGGoogleServicePolicy.this.mWakelockMonitor, 0);
                    } else if (action.equals("android.net.conn.CONNECTIVITY_CHANGE")) {
                        NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra("networkInfo");
                        if (networkInfo != null && networkInfo.isConnected()) {
                            PGGoogleServicePolicy.this.mCheckDuration = 60000;
                            PGGoogleServicePolicy.this.mWakeLockHandler.removeCallbacks(PGGoogleServicePolicy.this.mWakelockMonitor);
                            PGGoogleServicePolicy.this.mWakeLockHandler.postDelayed(PGGoogleServicePolicy.this.mWakelockMonitor, 0);
                        }
                    }
                }
            }
        }
    };
    private long mCheckDuration = 60000;
    private Context mContext;
    private boolean mEnabled = true;
    private int mGmsUid = -1;
    private boolean mIsGoogleServerConnectedOK = (IS_CHINA_MARKET.booleanValue() ^ 1);
    private Handler mWakeLockHandler;
    private Runnable mWakelockMonitor = new Runnable() {
        public void run() {
            Log.d(PGGoogleServicePolicy.TAG, "check isGoogleConnect start");
            new Thread() {
                public void run() {
                    PGGoogleServicePolicy.this.mGmsUid = PGGoogleServicePolicy.this.getGmsUid();
                    if (PGGoogleServicePolicy.this.isGoogleConnectOK()) {
                        PGGoogleServicePolicy.this.mIsGoogleServerConnectedOK = true;
                        Log.d(PGGoogleServicePolicy.TAG, "connect google success, PreventWake change to invalid");
                    } else {
                        PGGoogleServicePolicy.this.mIsGoogleServerConnectedOK = false;
                        Log.d(PGGoogleServicePolicy.TAG, "connect google failed, PreventWake change to valid");
                    }
                    LogPower.push(HdmiCecKeycode.UI_SOUND_PRESENTATION_TREBLE_NEUTRAL, PGGoogleServicePolicy.this.mIsGoogleServerConnectedOK ? "1" : "0");
                    HwServiceFactory.reportGoogleConn(PGGoogleServicePolicy.this.mIsGoogleServerConnectedOK);
                    PGGoogleServicePolicy.this.mWakeLockHandler.removeCallbacks(PGGoogleServicePolicy.this.mWakelockMonitor);
                    if (PGGoogleServicePolicy.this.isShangHaiTimeZone() || PGGoogleServicePolicy.this.mIsGoogleServerConnectedOK) {
                        PGGoogleServicePolicy.this.mWakeLockHandler.postDelayed(PGGoogleServicePolicy.this.mWakelockMonitor, 1800000);
                        return;
                    }
                    PGGoogleServicePolicy.this.mWakeLockHandler.postDelayed(PGGoogleServicePolicy.this.mWakelockMonitor, PGGoogleServicePolicy.this.mCheckDuration);
                    String str = PGGoogleServicePolicy.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("retry after ");
                    stringBuilder.append(PGGoogleServicePolicy.this.mCheckDuration);
                    Log.d(str, stringBuilder.toString());
                    PGGoogleServicePolicy.access$330(PGGoogleServicePolicy.this, 2);
                    if (PGGoogleServicePolicy.this.mCheckDuration >= 1800000) {
                        PGGoogleServicePolicy.this.mCheckDuration = 1800000;
                    }
                }
            }.start();
        }
    };

    static /* synthetic */ long access$330(PGGoogleServicePolicy x0, long x1) {
        long j = x0.mCheckDuration * x1;
        x0.mCheckDuration = j;
        return j;
    }

    static {
        boolean z = false;
        if (SystemProperties.getInt("ro.config.hw_optb", 0) == 156) {
            z = true;
        }
        IS_CHINA_MARKET = Boolean.valueOf(z);
    }

    public PGGoogleServicePolicy(Context context) {
        this.mContext = context;
        this.mWakeLockHandler = new Handler();
    }

    public void onSystemReady() {
        if (IS_CHINA_MARKET.booleanValue()) {
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.BOOT_COMPLETED");
            filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
            this.mContext.registerReceiver(this.mBroadcastReceiver, filter, null, null);
        }
    }

    private boolean isShangHaiTimeZone() {
        return "Asia/Shanghai".equals(SystemProperties.get("persist.sys.timezone", BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS));
    }

    private boolean isGoogleConnectOK() {
        HttpURLConnection conn = null;
        Boolean isConnectOk = Boolean.valueOf(false);
        BufferedReader reader = null;
        try {
            try {
                conn = (HttpURLConnection) new URL(US_GOOGLE_URL).openConnection();
                conn.setConnectTimeout(IHwShutdownThread.SHUTDOWN_ANIMATION_WAIT_TIME);
                conn.setReadTimeout(10000);
                conn.connect();
                int httpResponseCode = conn.getResponseCode();
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("httpResponseCode = ");
                stringBuilder.append(httpResponseCode);
                Log.d(str, stringBuilder.toString());
                if (DisplayTransformManager.LEVEL_COLOR_MATRIX_GRAYSCALE == httpResponseCode) {
                    if (IS_CHINA_MARKET.booleanValue()) {
                        str = TelephonyManager.getDefault().getSimOperator();
                        if (str != null && str.startsWith("460")) {
                            reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                            StringBuffer sb = new StringBuffer();
                            while (true) {
                                String readLine = reader.readLine();
                                String line = readLine;
                                if (readLine == null) {
                                    break;
                                }
                                sb.append(line);
                                int length = sb.toString().length();
                                if (length > 2) {
                                    isConnectOk = Boolean.valueOf(true);
                                    String str2 = TAG;
                                    StringBuilder stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append("openrator is ");
                                    stringBuilder2.append(str);
                                    stringBuilder2.append("httpResponseData length : ");
                                    stringBuilder2.append(length);
                                    Log.d(str2, stringBuilder2.toString());
                                    break;
                                }
                            }
                        } else {
                            isConnectOk = Boolean.valueOf(true);
                        }
                    } else {
                        isConnectOk = Boolean.valueOf(true);
                    }
                }
                if (conn != null) {
                    conn.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        Log.w(TAG, "close reader Exception.");
                    }
                }
            } catch (Exception e2) {
                Log.d(TAG, "failed to connect google.");
                if (conn != null) {
                    conn.disconnect();
                }
                if (reader != null) {
                    reader.close();
                }
            } catch (Throwable th) {
                if (conn != null) {
                    conn.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e3) {
                        Log.w(TAG, "close reader Exception.");
                    }
                }
            }
            if (isConnectOk.booleanValue()) {
                return true;
            }
            return false;
        } catch (MalformedURLException e4) {
            Log.d(TAG, "PreventWake MalformedURLException");
            return false;
        }
    }

    private int getGmsUid() {
        ApplicationInfo ai = null;
        try {
            ai = this.mContext.getPackageManager().getApplicationInfo(GOOGLE_GMS_PAC, 0);
        } catch (Exception e) {
            Log.d(TAG, "failed to get application info");
        }
        if (ai == null) {
            return -1;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("gmsUid = ");
        stringBuilder.append(ai.uid);
        Log.d(str, stringBuilder.toString());
        return ai.uid;
    }

    public boolean isGmsWakeLockFilterTag(int flags, String packageName, WorkSource ws) {
        if (!(packageName == null || 1 != (NetworkConstants.ARP_HWTYPE_RESERVED_HI & flags) || this.mIsGoogleServerConnectedOK)) {
            if (packageName.contains(GOOGLE_GMS_PAC) || packageName.contains(GOOGLE_GSF_PAC)) {
                Log.d(TAG, "prevent gms/gsf hold partial wakelock");
                return true;
            } else if (ws != null) {
                for (int i = 0; i < ws.size(); i++) {
                    if (ws.get(i) == this.mGmsUid) {
                        Log.d(TAG, "worksource has gms, prevent hold partial wakelock");
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
