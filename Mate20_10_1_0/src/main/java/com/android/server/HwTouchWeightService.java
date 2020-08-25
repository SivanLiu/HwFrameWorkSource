package com.android.server;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class HwTouchWeightService {
    private static final String FILE_PATH = "/sys/touchscreen/touch_weight";
    private static final int GET_WEIGHT_TIMEOUT = 1800;
    private static final int GET_WEIGHT_TIMEOUT_MESSAGE = 100;
    private static final int HAERT_BEAT_MESSAGE = 101;
    private static final int HAERT_BEAT_TIME = 1000;
    private static final String HAERT_BEAT_VALUE = "2";
    private static final String RUN_TOUCH_WEIGHT_VALUE = "1";
    private static final String STOP_TOUCH_WEIGHT_VALUE = "0";
    private static final String TAG = "HwTouchWeightService";
    private static volatile HwTouchWeightService mInstance = null;
    private boolean isFeatureEnabled = false;
    private boolean isFeatureSupport = false;
    private Handler mHandler;

    public HwTouchWeightService(Context context, Handler handler) {
        this.mHandler = new TouchWeightHandler(handler.getLooper());
        this.isFeatureSupport = isFeatureSupport();
    }

    private boolean isFeatureSupport() {
        if (new File(FILE_PATH).exists()) {
            return true;
        }
        return false;
    }

    /* JADX WARNING: Code restructure failed: missing block: B:22:0x0052, code lost:
        r1 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:23:0x0053, code lost:
        if (r0 != null) goto L_0x0055;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:25:?, code lost:
        r0.close();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:31:?, code lost:
        r0.close();
     */
    /* JADX WARNING: Failed to process nested try/catch */
    /* JADX WARNING: Removed duplicated region for block: B:22:0x0052 A[ExcHandler: all (r1v1 'th' java.lang.Throwable A[CUSTOM_DECLARE]), PHI: r0 
      PHI: (r0v6 'in' java.io.FileInputStream) = (r0v3 'in' java.io.FileInputStream), (r0v3 'in' java.io.FileInputStream), (r0v3 'in' java.io.FileInputStream), (r0v9 'in' java.io.FileInputStream) binds: [B:11:0x0010, B:12:?, B:13:0x0012, B:14:?] A[DONT_GENERATE, DONT_INLINE], Splitter:B:11:0x0010] */
    /* JADX WARNING: Removed duplicated region for block: B:30:0x005e A[SYNTHETIC, Splitter:B:30:0x005e] */
    public synchronized String getTouchWeightValue() {
        FileInputStream in;
        if (!this.isFeatureSupport) {
            return null;
        }
        if (!this.isFeatureEnabled) {
            enableTouchWeight();
        }
        in = null;
        try {
            in = new FileInputStream(new File(FILE_PATH));
            byte[] b = new byte[64];
            String val = new String(b, 0, in.read(b), "utf-8");
            Log.i("touch weight", "HwTouchWeightService getTouchWeightValue " + val);
            try {
                in.close();
            } catch (IOException e) {
            }
            return val;
        } catch (FileNotFoundException e2) {
        } catch (IOException e3) {
            if (in != null) {
            }
            resetTimeOut();
            Log.i("touch weight", "HwTouchWeightService getTouchWeightValue null");
            return null;
        } catch (Throwable th) {
        }
        if (in != null) {
            in.close();
        }
        resetTimeOut();
        Log.i("touch weight", "HwTouchWeightService getTouchWeightValue null");
        return null;
        throw th;
    }

    public synchronized void resetTouchWeight() {
        if (this.isFeatureSupport) {
            if (this.mHandler.hasMessages(101)) {
                this.mHandler.removeMessages(101);
            }
            this.isFeatureEnabled = false;
            setTouchWeightValue("0");
        }
    }

    private final class TouchWeightHandler extends Handler {
        public TouchWeightHandler(Looper looper) {
            super(looper, null, true);
        }

        public void handleMessage(Message msg) {
            int i = msg.what;
            if (i == 100) {
                HwTouchWeightService.this.resetTouchWeight();
            } else if (i == 101) {
                HwTouchWeightService.this.startHeartBeat();
            }
        }
    }

    private void enableTouchWeight() {
        setTouchWeightValue("1");
        this.isFeatureEnabled = true;
        startHeartBeat();
    }

    private void setTouchWeightValue(String val) {
        Log.i("touch weight", "HwTouchWeightService setTouchWeightValue " + val);
        FileOutputStream fileOutWriteMode = null;
        try {
            fileOutWriteMode = new FileOutputStream(new File(FILE_PATH));
            fileOutWriteMode.write(val.getBytes("utf-8"));
            try {
                fileOutWriteMode.close();
            } catch (IOException e) {
            }
        } catch (FileNotFoundException e2) {
            if (fileOutWriteMode != null) {
                fileOutWriteMode.close();
            }
        } catch (IOException e3) {
            if (fileOutWriteMode != null) {
                fileOutWriteMode.close();
            }
        } catch (Throwable th) {
            if (fileOutWriteMode != null) {
                try {
                    fileOutWriteMode.close();
                } catch (IOException e4) {
                }
            }
            throw th;
        }
    }

    public static synchronized HwTouchWeightService getInstance(Context context, Handler handler) {
        HwTouchWeightService hwTouchWeightService;
        synchronized (HwTouchWeightService.class) {
            if (mInstance == null) {
                mInstance = new HwTouchWeightService(context, handler);
            }
            hwTouchWeightService = mInstance;
        }
        return hwTouchWeightService;
    }

    /* access modifiers changed from: private */
    public void startHeartBeat() {
        setTouchWeightValue("2");
        Handler handler = this.mHandler;
        if (handler == null) {
            Log.e(TAG, "startHeartBeat mHandler is null");
            return;
        }
        this.mHandler.sendMessageDelayed(handler.obtainMessage(101), 1000);
    }

    private void resetTimeOut() {
        if (this.mHandler.hasMessages(100)) {
            this.mHandler.removeMessages(100);
        }
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(100), 1800);
    }
}
