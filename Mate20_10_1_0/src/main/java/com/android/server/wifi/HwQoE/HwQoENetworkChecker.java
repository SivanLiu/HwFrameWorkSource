package com.android.server.wifi.HwQoE;

import android.content.Context;
import android.os.Handler;
import com.huawei.hwwifiproservice.HwNetworkPropertyChecker;

public class HwQoENetworkChecker {
    /* access modifiers changed from: private */
    public HwNetworkPropertyChecker mHwNetworkPropertychecker;
    private NetworkCheckThread mNetworkCheckThread;

    public HwQoENetworkChecker(Context context, Handler handler) {
        this.mHwNetworkPropertychecker = new HwNetworkPropertyChecker(context, null, null, true, null, false);
        this.mNetworkCheckThread = new NetworkCheckThread(handler);
    }

    public void start() {
        HwQoEUtils.logE(false, "NetworkCheckThread start", new Object[0]);
        this.mNetworkCheckThread.start();
    }

    private class NetworkCheckThread extends Thread {
        private static final int HAVE_INTERNET_ACCESS = 204;
        private Handler mHandler;

        public NetworkCheckThread(Handler handler) {
            this.mHandler = handler;
        }

        public void run() {
            HwQoEUtils.logE(false, "NetworkCheckThread run", new Object[0]);
            int respCode = HwQoENetworkChecker.this.mHwNetworkPropertychecker.isCaptivePortal(true);
            HwQoEUtils.logE(false, "NetworkCheckThread respCode = %{public}d", Integer.valueOf(respCode));
            HwQoENetworkChecker.this.mHwNetworkPropertychecker.release();
            if (respCode == 204) {
                this.mHandler.sendEmptyMessage(103);
            } else {
                this.mHandler.sendEmptyMessage(104);
            }
        }
    }
}
