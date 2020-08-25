package com.android.server.tv;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Log;
import com.android.server.LocalServices;
import com.android.server.policy.WindowManagerPolicy;
import java.util.HashSet;
import java.util.Set;

public class HwTvPowerManager implements HwTvPowerManagerPolicy {
    private static final long CHECK_USER_OPERATION_TIMEOUT = 300000;
    private static final boolean DEBUG = Log.HWINFO;
    private static final String FACTORY_AT_MODE_BOE = "BOE";
    private static final String FACTORY_AT_MODE_CVT = "CVT";
    private static final int MSG_CHECK_USER_OPERATION_AFTER_BOOT = 1;
    private static final String PROP_FACTORY_AT_MODE = "factory.at.mode";
    private static final String PROP_HISUSPEND_MODE = "sys.hw_mc.tvpower.suspend_mode";
    private static final String PROP_REBOOT_SCENE = "persist.sys.reboot.scene";
    private static final String REBOOT_SCENE_NORMAL = "normal";
    private static final String SUSPEND_MODE_LIGHTSLEEP = "lightsleep";
    private static final String SUSPEND_MODE_SHUTDOWN = "str";
    private static final String TAG = "HwTvPowerManager";
    private static final Set<String> WAKELOCK_WHITE_LIST = new HashSet<String>(3) {
        /* class com.android.server.tv.HwTvPowerManager.AnonymousClass1 */

        {
            add("com.huawei.homevision.poweronoffsvr1000");
            add("com.huawei.homevision.tvservice1000");
            add("com.hisilicon.android.hiRMService1000");
        }
    };
    private Context mContext;
    private Handler mHandler = new TvHandler();
    private boolean mIsUserOperationChecking;
    private WindowManagerPolicy mPolicy;
    private PowerManager mPowerManager;

    public HwTvPowerManager(Context context) {
        this.mContext = context;
    }

    @Override // com.android.server.tv.HwTvPowerManagerPolicy
    public void systemReady() {
        this.mPowerManager = (PowerManager) this.mContext.getSystemService("power");
        this.mPolicy = (WindowManagerPolicy) LocalServices.getService(WindowManagerPolicy.class);
    }

    @Override // com.android.server.tv.HwTvPowerManagerPolicy
    public void bootCompleted() {
        if (isRunningInTvMode() && isUserSetuped() && !isInTvFactoryMode()) {
            String rebootScene = SystemProperties.get(PROP_REBOOT_SCENE);
            boolean isNormalReboot = REBOOT_SCENE_NORMAL.equalsIgnoreCase(rebootScene);
            Log.i(TAG, "rebootScene:" + rebootScene);
            if (!isNormalReboot) {
                scheduleCheckUserOperation();
            }
            SystemProperties.set(PROP_REBOOT_SCENE, "");
        }
    }

    @Override // com.android.server.tv.HwTvPowerManagerPolicy
    public void onEarlyShutdownBegin(boolean isReboot) {
        if (isReboot && isRunningInTvMode()) {
            Log.i(TAG, "normal reboot begin");
            SystemProperties.set(PROP_REBOOT_SCENE, REBOOT_SCENE_NORMAL);
        }
    }

    @Override // com.android.server.tv.HwTvPowerManagerPolicy
    public void onKeyOperation() {
        if (this.mIsUserOperationChecking) {
            if (DEBUG) {
                Log.i(TAG, "onKeyOperation, cancel user operation check");
            }
            cancelUserOperationCheck();
        }
    }

    @Override // com.android.server.tv.HwTvPowerManagerPolicy
    public void onEarlyGoToSleep(int flags) {
        if (isRunningInTvMode()) {
            Log.i(TAG, "goToSleep flags:0x" + Integer.toHexString(flags));
            if ((65536 & flags) != 0) {
                SystemProperties.set(PROP_HISUSPEND_MODE, SUSPEND_MODE_SHUTDOWN);
            } else {
                SystemProperties.set(PROP_HISUSPEND_MODE, SUSPEND_MODE_LIGHTSLEEP);
            }
        }
    }

    @Override // com.android.server.tv.HwTvPowerManagerPolicy
    public boolean isWakeLockDisabled(String packageName, int pid, int uid) {
        Set<String> set = WAKELOCK_WHITE_LIST;
        if (set.contains(packageName + uid)) {
            return false;
        }
        return true;
    }

    @Override // com.android.server.tv.HwTvPowerManagerPolicy
    public boolean isWakelockCauseWakeUpDisabled() {
        return SUSPEND_MODE_SHUTDOWN.equals(SystemProperties.get(PROP_HISUSPEND_MODE));
    }

    final class TvHandler extends Handler {
        TvHandler() {
        }

        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                HwTvPowerManager.this.handleCheckUserOperationTimeout();
            }
        }
    }

    private void scheduleCheckUserOperation() {
        this.mIsUserOperationChecking = true;
        if (!this.mHandler.hasMessages(1)) {
            this.mHandler.sendEmptyMessageDelayed(1, 300000);
        }
    }

    /* access modifiers changed from: private */
    public void handleCheckUserOperationTimeout() {
        if (this.mIsUserOperationChecking) {
            Log.i(TAG, "no operation after boot in 300000ms, then goToSleep");
            cancelUserOperationCheck();
            PowerManager powerManager = this.mPowerManager;
            if (powerManager != null) {
                powerManager.goToSleep(SystemClock.uptimeMillis());
            }
        }
    }

    private void cancelUserOperationCheck() {
        this.mIsUserOperationChecking = false;
        this.mHandler.removeMessages(1);
    }

    private boolean isRunningInTvMode() {
        WindowManagerPolicy windowManagerPolicy = this.mPolicy;
        return windowManagerPolicy != null && HwTvPowerManagerPolicy.isTvMode(windowManagerPolicy.getUiMode());
    }

    private boolean isInTvFactoryMode() {
        String mode = SystemProperties.get(PROP_FACTORY_AT_MODE, "");
        return FACTORY_AT_MODE_BOE.equalsIgnoreCase(mode) || FACTORY_AT_MODE_CVT.equalsIgnoreCase(mode);
    }

    private boolean isUserSetuped() {
        return Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "user_setup_complete", 0, -2) != 0;
    }
}
