package com.android.server.power;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Log;
import com.android.server.hidata.arbitration.HwArbitrationDEFS;
import com.android.server.policy.HwPolicyFactory;
import com.android.server.power.ShutdownThread.CloseDialogReceiver;
import com.android.server.wm.WindowManagerService;
import huawei.cust.HwCfgFilePolicy;
import java.io.File;

public class HwShutdownThreadImpl implements IHwShutdownThread {
    private static final boolean DEBUG;
    private static final int MAX_SHUTDOWN_ANIM_WAIT_MSEC = 15000;
    private static final int PHONE_STATE_POLL_SLEEP_MSEC = 500;
    private static final int SCREEN_ROTATION_SETTING = 8002;
    private static final String SHUTDOWN_ANIMATION_STATUS_PROPERTY = "sys.shutdown.animationstate";
    private static final int SHUTDOWN_ANIMATION_WAIT_TIME = 2000;
    private static final String TAG = "HwShutdownThread";
    public static final String mNotchProp = SystemProperties.get("ro.config.hw_notch_size", "");
    private static String mShutdown_path1 = "/data/cust/media/shutdownanimation.zip";
    private static String mShutdown_path2 = "/data/local/shutdownanimation.zip";
    private static String mShutdown_path3 = "/system/media/shutdownanimation.zip";
    private static AlertDialog sRootConfirmDialog;
    private boolean isHaveShutdownAnimation = false;
    private boolean mHwShutDownAnimationStart;

    static {
        boolean z = Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4));
        DEBUG = z;
    }

    public boolean isShutDownAnimationAvailable() {
        boolean z;
        boolean doseShutDownAnimationExist = false;
        try {
            if (HwCfgFilePolicy.getCfgFile("media/shutdownanimation.zip", 0) != null || isMccmncCustZipExist() || isIccidCustZipExist() || isBootCustZipExist() || isDataZipExist()) {
                doseShutDownAnimationExist = true;
            }
        } catch (NoClassDefFoundError e) {
            Log.d(TAG, "HwCfgFilePolicy NoClassDefFoundError");
        }
        if (!doseShutDownAnimationExist) {
            z = new File(mShutdown_path1).exists() || new File(mShutdown_path2).exists() || new File(mShutdown_path3).exists();
            doseShutDownAnimationExist = z;
        }
        z = SystemProperties.getBoolean("ro.config.use_shutdown_anim", true);
        if (doseShutDownAnimationExist && z) {
            return true;
        }
        return false;
    }

    public static boolean hasNotchInScreen() {
        return TextUtils.isEmpty(mNotchProp) ^ 1;
    }

    public boolean isDoShutdownAnimation() {
        if (hasNotchInScreen()) {
            transferSwitchStatusToSurfaceFlinger(0);
        }
        if (!isShutDownAnimationAvailable()) {
            return false;
        }
        try {
            ((WindowManagerService) ServiceManager.getService("window")).freezeOrThawRotation(0);
        } catch (Exception e) {
        }
        this.isHaveShutdownAnimation = true;
        try {
            Log.d(TAG, "ctl.start shutanim service!");
            SystemProperties.set("ctl.start", "shutanim");
        } catch (Exception e2) {
            Log.e(TAG, "run shutdown animation failed", e2);
        }
        this.mHwShutDownAnimationStart = true;
        return true;
    }

    public static void transferSwitchStatusToSurfaceFlinger(int val) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Transfer Switch status to SurfaceFlinger , val = ");
        stringBuilder.append(val);
        Log.i(str, stringBuilder.toString());
        Parcel dataIn = Parcel.obtain();
        try {
            IBinder sfBinder = ServiceManager.getService("SurfaceFlinger");
            dataIn.writeInt(val);
            if (!(sfBinder == null || sfBinder.transact(SCREEN_ROTATION_SETTING, dataIn, null, 1))) {
                Log.e(TAG, "transferSwitchStatusToSurfaceFlinger error!");
            }
        } catch (RemoteException e) {
            Log.e(TAG, "transferSwitchStatusToSurfaceFlinger RemoteException on notify screen rotation animation end");
        } catch (Throwable th) {
            dataIn.recycle();
        }
        dataIn.recycle();
    }

    private static boolean isIccidCustZipExist() {
        String iccid = SystemProperties.get("persist.sys.iccid", "0");
        if (!"0".equals(iccid)) {
            String matchPath = new StringBuilder();
            matchPath.append("media/shutdownanimation_");
            matchPath.append(iccid);
            matchPath.append(".zip");
            try {
                if (HwCfgFilePolicy.getCfgFile(matchPath.toString(), 0) != null) {
                    return true;
                }
            } catch (NoClassDefFoundError e) {
                Log.d(TAG, "HwCfgFilePolicy Iccid NoClassDefFoundError");
            }
        }
        return false;
    }

    private static boolean isMccmncCustZipExist() {
        String mccmnc = SystemProperties.get("persist.sys.mccmnc", "0");
        if (!"0".equals(mccmnc)) {
            String matchPath = new StringBuilder();
            matchPath.append("media/shutdownanimation_");
            matchPath.append(mccmnc);
            matchPath.append(".zip");
            try {
                if (HwCfgFilePolicy.getCfgFile(matchPath.toString(), 0) != null) {
                    return true;
                }
            } catch (NoClassDefFoundError e) {
                Log.d(TAG, "HwCfgFilePolicy Mccmnc NoClassDefFoundError");
            }
        }
        return false;
    }

    private static boolean isBootCustZipExist() {
        String boot = SystemProperties.get("persist.sys.boot", "0");
        if (!"0".equals(boot)) {
            String matchPath = new StringBuilder();
            matchPath.append("media/shutdownanimation_");
            matchPath.append(boot);
            matchPath.append(".zip");
            try {
                if (HwCfgFilePolicy.getCfgFile(matchPath.toString(), 0) != null) {
                    return true;
                }
            } catch (NoClassDefFoundError e) {
                Log.e(TAG, "HwCfgFilePolicy boot NoClassDefFoundError");
            }
        }
        return false;
    }

    private static boolean isDataZipExist() {
        try {
            String dataShutdowAnimationFileName;
            String str;
            String[] shutdowAnimationFiles = HwCfgFilePolicy.getCfgPolicyDir(0);
            String matchPath = "media/shutdownanimation.zip";
            for (int i = shutdowAnimationFiles.length - 1; i >= 0; i--) {
                dataShutdowAnimationFileName = new StringBuilder();
                dataShutdowAnimationFileName.append("/data/hw_init");
                dataShutdowAnimationFileName.append(shutdowAnimationFiles[i]);
                File file = new File(dataShutdowAnimationFileName.toString(), matchPath);
                if (file.exists()) {
                    str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("file name is");
                    stringBuilder.append(file.getPath());
                    Log.d(str, stringBuilder.toString());
                    return true;
                }
            }
            String boot = SystemProperties.get("persist.sys.boot", "0");
            if (!"0".equals(boot)) {
                dataShutdowAnimationFileName = new StringBuilder();
                dataShutdowAnimationFileName.append("media/shutdownanimation_");
                dataShutdowAnimationFileName.append(boot);
                dataShutdowAnimationFileName.append(".zip");
                dataShutdowAnimationFileName = dataShutdowAnimationFileName.toString();
                for (int i2 = shutdowAnimationFiles.length - 1; i2 >= 0; i2--) {
                    str = new StringBuilder();
                    str.append("/data/hw_init");
                    str.append(shutdowAnimationFiles[i2]);
                    File file2 = new File(str.toString(), dataShutdowAnimationFileName);
                    if (file2.exists()) {
                        String str2 = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("file name is");
                        stringBuilder2.append(file2.getPath());
                        Log.d(str2, stringBuilder2.toString());
                        return true;
                    }
                }
            }
            dataShutdowAnimationFileName = SystemProperties.get("persist.sys.mccmnc", "0");
            if (!"0".equals(dataShutdowAnimationFileName)) {
                String matchPath_mccmnc = new StringBuilder();
                matchPath_mccmnc.append("media/shutdownanimation_");
                matchPath_mccmnc.append(dataShutdowAnimationFileName);
                matchPath_mccmnc.append(".zip");
                matchPath_mccmnc = matchPath_mccmnc.toString();
                for (int i3 = shutdowAnimationFiles.length - 1; i3 >= 0; i3--) {
                    String dataShutdowAnimationFileName2 = new StringBuilder();
                    dataShutdowAnimationFileName2.append("/data/hw_init");
                    dataShutdowAnimationFileName2.append(shutdowAnimationFiles[i3]);
                    File file3 = new File(dataShutdowAnimationFileName2.toString(), matchPath_mccmnc);
                    if (file3.exists()) {
                        String str3 = TAG;
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("file name is");
                        stringBuilder3.append(file3.getPath());
                        Log.d(str3, stringBuilder3.toString());
                        return true;
                    }
                }
            }
        } catch (NoClassDefFoundError e) {
            Log.e(TAG, "HwCfgFilePolicy boot NoClassDefFoundError");
        }
        return false;
    }

    public void waitShutdownAnimation() {
        if (this.isHaveShutdownAnimation) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Log.e(TAG, "shutdown animation thread sleep 2s failed", e);
            }
        }
    }

    public boolean needRebootDialog(String rebootReason, Context context) {
        if (rebootReason == null || !rebootReason.equals("huawei_reboot")) {
            return false;
        }
        rebootDialog(context);
        return true;
    }

    public boolean needRebootProgressDialog(boolean reboot, Context context) {
        if (!reboot || HwPolicyFactory.isHwGlobalActionsShowing()) {
            return false;
        }
        rebootProgressDialog(context);
        return true;
    }

    private static synchronized void rebootDialog(final Context context) {
        synchronized (HwShutdownThreadImpl.class) {
            CloseDialogReceiver closer = new CloseDialogReceiver(context);
            if (sRootConfirmDialog != null) {
                sRootConfirmDialog.dismiss();
            }
            sRootConfirmDialog = new Builder(context, 33947691).setTitle(33685517).setMessage(33685518).setIcon(17301543).setPositiveButton(17039379, new OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    ShutdownThread.beginShutdownSequence(context);
                }
            }).setNegativeButton(17039369, null).create();
            closer.dialog = sRootConfirmDialog;
            sRootConfirmDialog.setOnDismissListener(closer);
            sRootConfirmDialog.getWindow().setType(HwArbitrationDEFS.MSG_MPLINK_UNBIND_SUCCESS);
            sRootConfirmDialog.show();
        }
    }

    private static void rebootProgressDialog(Context context) {
        ProgressDialog pd = new ProgressDialog(context, 33947691);
        pd.setTitle(context.getText(33685515));
        pd.setMessage(context.getText(33685565));
        pd.setIndeterminate(true);
        pd.setCancelable(false);
        pd.getWindow().setType(HwArbitrationDEFS.MSG_MPLINK_UNBIND_SUCCESS);
        pd.show();
    }

    public void resetValues() {
        this.mHwShutDownAnimationStart = false;
    }

    public void waitShutdownAnimationComplete(Context context, long shutDownBegin) {
        if (this.mHwShutDownAnimationStart && shutDownBegin > 0) {
            long endTime = 15000 + shutDownBegin;
            long delay = endTime - SystemClock.elapsedRealtime();
            boolean shutDownAnimationFinish = "true".equals(SystemProperties.get(SHUTDOWN_ANIMATION_STATUS_PROPERTY, "false"));
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("ShutDown Animation Status delay:");
            stringBuilder.append(delay);
            stringBuilder.append(", shutDownAnimationFinish:");
            stringBuilder.append(shutDownAnimationFinish);
            stringBuilder.append(", shutDownBegin:");
            stringBuilder.append(shutDownBegin);
            stringBuilder.append(", endTime:");
            stringBuilder.append(endTime);
            Log.i(str, stringBuilder.toString());
            while (delay > 0 && !shutDownAnimationFinish) {
                SystemClock.sleep(500);
                delay = endTime - SystemClock.elapsedRealtime();
                shutDownAnimationFinish = "true".equals(SystemProperties.get(SHUTDOWN_ANIMATION_STATUS_PROPERTY, "false"));
            }
            SystemProperties.set(SHUTDOWN_ANIMATION_STATUS_PROPERTY, "false");
        }
    }
}
