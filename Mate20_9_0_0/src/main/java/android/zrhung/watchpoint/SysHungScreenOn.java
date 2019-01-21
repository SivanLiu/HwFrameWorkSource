package android.zrhung.watchpoint;

import android.os.Binder;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.util.Slog;
import android.util.ZRHung;
import android.util.ZRHung.HungConfig;
import android.zrhung.ZrHungData;
import android.zrhung.ZrHungImpl;
import com.android.internal.os.BackgroundThread;
import huawei.android.provider.HwSettings.System;
import java.text.SimpleDateFormat;
import java.util.Date;

public final class SysHungScreenOn extends ZrHungImpl {
    public static final String HUNG_CONFIG_ENABLE = "1";
    public static final int MESSAGE_CHECK_SCREENON = 1000;
    private static final String TAG = "ZrHung.SysHungScreenOn";
    private static SysHungScreenOn mSysHungScreenOn = null;
    private HungConfig mHungConfig = null;
    private String mHungConfigEnable = "null";
    private int mHungConfigStatus = -1;
    private int mScreenODelayTime = 5000;
    private Handler mScreenOnCheckHandler = new ScreenOnCheckHandler(BackgroundThread.getHandler().getLooper());
    private StringBuilder mScreenOnInfo = new StringBuilder();

    private final class ScreenOnCheckHandler extends Handler {
        public ScreenOnCheckHandler(Looper looper) {
            super(looper, null, true);
        }

        public void handleMessage(Message msg) {
            if (msg.what == 1000) {
                SysHungScreenOn.this.sendEvent(null);
            }
        }
    }

    public SysHungScreenOn(String wpName) {
        super(wpName);
    }

    public static synchronized SysHungScreenOn getInstance(String wpName) {
        SysHungScreenOn sysHungScreenOn;
        synchronized (SysHungScreenOn.class) {
            if (mSysHungScreenOn == null) {
                mSysHungScreenOn = new SysHungScreenOn(wpName);
            }
            sysHungScreenOn = mSysHungScreenOn;
        }
        return sysHungScreenOn;
    }

    public int init(ZrHungData args) {
        try {
            if (this.mHungConfig == null || this.mHungConfigStatus == 1) {
                this.mHungConfig = ZRHung.getHungConfig((short) 11);
                if (this.mHungConfig != null) {
                    this.mHungConfigStatus = this.mHungConfig.status;
                    String[] value = this.mHungConfig.value.split(",");
                    this.mHungConfigEnable = value[0];
                    if (value.length > 1) {
                        this.mScreenODelayTime = Integer.parseInt(value[1]);
                    }
                }
            }
            return 0;
        } catch (Exception ex) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("exception info ex:");
            stringBuilder.append(ex);
            Slog.e(str, stringBuilder.toString());
            return -2;
        }
    }

    public boolean start(ZrHungData args) {
        try {
            init(null);
            if (!isBootCompleted()) {
                return false;
            }
            if (!this.mScreenOnCheckHandler.hasMessages(1000)) {
                this.mScreenOnCheckHandler.sendEmptyMessageDelayed(1000, (long) this.mScreenODelayTime);
                Slog.d(TAG, "SysHungScreenOn start");
            }
            return true;
        } catch (Exception ex) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("get ex:");
            stringBuilder.append(ex);
            Slog.e(str, stringBuilder.toString());
            return false;
        }
    }

    public boolean check(ZrHungData args) {
        return false;
    }

    public boolean cancelCheck(ZrHungData args) {
        return false;
    }

    public boolean stop(ZrHungData args) {
        try {
            if (!isBootCompleted()) {
                return false;
            }
            if (this.mScreenOnCheckHandler.hasMessages(1000)) {
                this.mScreenOnCheckHandler.removeMessages(1000);
                Slog.d(TAG, "SysHungScreenOn stop");
            }
            this.mScreenOnInfo.delete(0, this.mScreenOnInfo.length());
            return true;
        } catch (Exception ex) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("has ex:");
            stringBuilder.append(ex);
            Slog.e(str, stringBuilder.toString());
            return false;
        }
    }

    public boolean sendEvent(ZrHungData args) {
        try {
            if (this.mHungConfig != null && this.mHungConfigStatus == 0 && "1".equals(this.mHungConfigEnable)) {
                if (!ZRHung.sendHungEvent(true, null, this.mScreenOnInfo.toString())) {
                    Slog.e(TAG, " ZRHung.sendHungEvent failed!");
                }
                Slog.i(TAG, this.mScreenOnInfo.toString());
                return true;
            }
            this.mScreenOnInfo.delete(0, this.mScreenOnInfo.length());
            return true;
        } catch (Exception ex) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("ScreenOnCheckHandler exception: ");
            stringBuilder.append(ex.toString());
            Slog.e(str, stringBuilder.toString());
            return false;
        }
    }

    public ZrHungData query() {
        return null;
    }

    public synchronized boolean addInfo(ZrHungData args) {
        try {
            if (!isBootCompleted()) {
                return false;
            }
            String str;
            StringBuilder stringBuilder;
            if (10000 <= Binder.getCallingUid()) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("permission not allowed. uid = ");
                stringBuilder.append(Binder.getCallingUid());
                Slog.e(str, stringBuilder.toString());
                return false;
            }
            str = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            if (this.mScreenOnInfo == null) {
                return false;
            }
            stringBuilder = this.mScreenOnInfo;
            stringBuilder.append(str);
            stringBuilder.append(":");
            stringBuilder.append(args.getString("addScreenOnInfo"));
            stringBuilder.append("\n");
            return true;
        } catch (Exception ex) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("exception info ex:");
            stringBuilder2.append(ex);
            Slog.e(str2, stringBuilder2.toString());
            return false;
        }
    }

    private boolean isBootCompleted() {
        return "1".equals(SystemProperties.get("sys.boot_completed", System.FINGERSENSE_KNUCKLE_GESTURE_OFF));
    }
}
