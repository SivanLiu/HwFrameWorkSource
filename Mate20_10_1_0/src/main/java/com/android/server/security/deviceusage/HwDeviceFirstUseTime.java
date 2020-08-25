package com.android.server.security.deviceusage;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.util.NtpTrustedTime;
import android.util.Slog;
import com.android.server.intellicom.common.SmartDualCardConsts;
import java.util.Date;

public class HwDeviceFirstUseTime {
    private static final long GET_TIME_DELAY = 3600000;
    private static final long GET_TIME_DELAY_MOBILE_CONNECTION = 21600000;
    private static final long INVALID_TIME = -1;
    /* access modifiers changed from: private */
    public static final boolean IS_HW_DEBUG = (Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4)));
    private static final long NTP_INTERVAL = 86400000;
    private static final String TAG = "HwDeviceFirstUseTime";
    private static final int TYPE_HAS_GET_TIME = 7;
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        /* class com.android.server.security.deviceusage.HwDeviceFirstUseTime.AnonymousClass2 */

        public void onReceive(Context context, Intent intent) {
            if (intent == null || TextUtils.isEmpty(intent.getAction())) {
                Slog.e(HwDeviceFirstUseTime.TAG, "onReceive intent or action is null!");
                return;
            }
            String action = intent.getAction();
            if (HwDeviceFirstUseTime.IS_HW_DEBUG) {
                Slog.d(HwDeviceFirstUseTime.TAG, "onReceive action " + action);
            }
            if (HwDeviceFirstUseTime.this.mConnectivityManager == null) {
                Slog.e(HwDeviceFirstUseTime.TAG, "onReceive mConnectivityManager is null!");
                return;
            }
            NetworkInfo networkInfo = HwDeviceFirstUseTime.this.mConnectivityManager.getActiveNetworkInfo();
            if (!SmartDualCardConsts.ACTION_CONNECTIVITY_CHANGE.equals(action) || HwDeviceFirstUseTime.this.mHandler == null) {
                Slog.w(HwDeviceFirstUseTime.TAG, "onReceive error broadcast!");
            } else if (networkInfo != null && networkInfo.isAvailable() && !HwDeviceFirstUseTime.this.mIsGetTimeFlag) {
                HwDeviceFirstUseTime.this.mHandler.post(HwDeviceFirstUseTime.this.mGetTimeRunnable);
            }
        }
    };
    private Handler mCollectionHandler;
    /* access modifiers changed from: private */
    public ConnectivityManager mConnectivityManager;
    private Context mContext;
    /* access modifiers changed from: private */
    public long mCurrentTime = -1;
    /* access modifiers changed from: private */
    public Runnable mGetTimeRunnable = new Runnable() {
        /* class com.android.server.security.deviceusage.HwDeviceFirstUseTime.AnonymousClass1 */

        public void run() {
            if (HwDeviceFirstUseTime.IS_HW_DEBUG) {
                Slog.d(HwDeviceFirstUseTime.TAG, "threadRun");
            }
            if (!HwDeviceFirstUseTime.this.mIsGetTimeFlag && HwDeviceFirstUseTime.this.mHandler != null) {
                if (HwDeviceFirstUseTime.this.mCurrentTime == -1 || !HwDeviceFirstUseTime.this.isMobileNetworkConnected() || HwDeviceFirstUseTime.this.mCurrentTime - SystemClock.elapsedRealtime() > 21600000) {
                    if (HwDeviceFirstUseTime.this.mNtpTime.getCacheAge() >= 86400000) {
                        HwDeviceFirstUseTime.this.mNtpTime.forceRefresh();
                    }
                    if (HwDeviceFirstUseTime.this.mNtpTime.getCacheAge() < 86400000) {
                        long time = HwDeviceFirstUseTime.this.mNtpTime.getCachedNtpTime();
                        if (time != 0) {
                            HwDeviceFirstUseTime.this.obtainHasGetTime(time);
                        }
                        if (HwDeviceFirstUseTime.IS_HW_DEBUG) {
                            Slog.d(HwDeviceFirstUseTime.TAG, "NTP server returned: " + time + " (" + new Date(time) + ")");
                        }
                    } else if (!HwDeviceFirstUseTime.this.isNetworkConnected()) {
                        HwDeviceFirstUseTime.this.mHandler.removeCallbacks(HwDeviceFirstUseTime.this.mGetTimeRunnable);
                    } else {
                        long unused = HwDeviceFirstUseTime.this.mCurrentTime = SystemClock.elapsedRealtime();
                        HwDeviceFirstUseTime.this.mHandler.postDelayed(HwDeviceFirstUseTime.this.mGetTimeRunnable, 3600000);
                    }
                } else {
                    HwDeviceFirstUseTime.this.mHandler.postDelayed(HwDeviceFirstUseTime.this.mGetTimeRunnable, 21600000);
                }
            }
        }
    };
    /* access modifiers changed from: private */
    public Handler mHandler;
    private HandlerThread mHandlerThread;
    /* access modifiers changed from: private */
    public boolean mIsGetTimeFlag = false;
    /* access modifiers changed from: private */
    public NtpTrustedTime mNtpTime;

    public HwDeviceFirstUseTime(Context context, Handler handler) {
        if (IS_HW_DEBUG) {
            Slog.d(TAG, TAG);
        }
        this.mContext = context;
        this.mCollectionHandler = handler;
        this.mNtpTime = NtpTrustedTime.getInstance(this.mContext);
        this.mConnectivityManager = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        this.mHandlerThread = new HandlerThread("HwDeviceFirstUseTimeThread");
    }

    public void start() {
        this.mHandlerThread.start();
        this.mHandler = new Handler(this.mHandlerThread.getLooper());
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(SmartDualCardConsts.ACTION_CONNECTIVITY_CHANGE);
        this.mContext.registerReceiver(this.mBroadcastReceiver, intentFilter);
    }

    public void triggerGetFirstUseTime() {
        Handler handler;
        if (IS_HW_DEBUG) {
            Slog.d(TAG, "getFirstUseTime");
        }
        if (isNetworkConnected() && !this.mIsGetTimeFlag && (handler = this.mHandler) != null) {
            handler.post(this.mGetTimeRunnable);
        }
    }

    /* access modifiers changed from: private */
    public void obtainHasGetTime(long time) {
        if (this.mHandler != null) {
            this.mIsGetTimeFlag = true;
            this.mCurrentTime = -1;
            this.mContext.unregisterReceiver(this.mBroadcastReceiver);
            this.mHandler.removeCallbacks(this.mGetTimeRunnable);
            if (this.mCollectionHandler != null) {
                Message msg = Message.obtain();
                msg.what = 7;
                msg.obj = Long.valueOf(time);
                this.mCollectionHandler.sendMessage(msg);
            }
        }
    }

    /* access modifiers changed from: private */
    public boolean isNetworkConnected() {
        ConnectivityManager connectivityManager = this.mConnectivityManager;
        if (connectivityManager == null) {
            Slog.e(TAG, "isNetworkConnected mConnectivityManager is null!");
            return false;
        }
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo == null || !networkInfo.isConnected()) {
            return false;
        }
        return true;
    }

    /* access modifiers changed from: private */
    public boolean isMobileNetworkConnected() {
        ConnectivityManager connectivityManager = this.mConnectivityManager;
        if (connectivityManager == null) {
            Slog.e(TAG, "isMobileNetworkConnected mConnectivityManager is null!");
            return false;
        }
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo == null || !networkInfo.isConnected() || networkInfo.getType() != 0) {
            return false;
        }
        return true;
    }
}
