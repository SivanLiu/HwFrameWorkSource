package com.android.server;

import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.IDeviceIdleController;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.Settings.System;
import android.util.Flog;
import android.util.Log;
import android.util.Slog;
import com.android.server.VibetonzProxy.IVibetonzImpl;
import com.android.server.gesture.GestureNavConst;
import dalvik.system.PathClassLoader;
import huawei.android.os.IHwGeneralManager.Stub;
import huawei.cust.HwCfgFilePolicy;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

public class HwGeneralService extends Stub {
    private static String CFG_FILE_SCREEN = "/sys/class/graphics/fb0/panel_info";
    private static String FILE_PATH = "sys/touchscreen/supported_func_indicater";
    private static final int NOT_SUPPORT_SDLOCK = -1;
    public static final int RESULT_NOT_SUPPORT = -10;
    static final String TAG = "HwGeneralService";
    private static final boolean mIsForcetouchDisabled = SystemProperties.getBoolean("ro.config.disable_force_touch", false);
    IDeviceIdleController dic = null;
    private boolean init = false;
    private Context mContext;
    private Handler mHandler;
    private IVibetonzImpl mIVibetonzImpl = this.mVibetonzProxy.getInstance();
    private int mIsCurveScreen = -1;
    private boolean mIsSupportForce = false;
    private float mLimit = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
    private ContentObserver mPressLimitObserver;
    private final ContentResolver mResolver;
    private VibetonzProxy mVibetonzProxy = new VibetonzProxy();

    static {
        try {
            System.loadLibrary("general_jni");
        } catch (UnsatisfiedLinkError e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Load general libarary failed >>>>>");
            stringBuilder.append(e);
            Slog.e(str, stringBuilder.toString());
        }
    }

    public HwGeneralService(Context context, Handler handler) {
        this.mContext = context;
        this.mHandler = handler;
        if (getSDLockSupport()) {
            HwSdLockService.getInstance(context);
        }
        if (getCryptsdSupport()) {
            HwSdCryptdService.getInstance(context);
        }
        this.mResolver = context.getContentResolver();
        initObserver(handler);
    }

    public int setSDLockPassword(String pw) {
        if (getSDLockSupport()) {
            return HwSdLockService.getInstance(this.mContext).setSDLockPassword(pw);
        }
        return -1;
    }

    public int clearSDLockPassword() {
        if (getSDLockSupport()) {
            return HwSdLockService.getInstance(this.mContext).clearSDLockPassword();
        }
        return -1;
    }

    public int unlockSDCard(String pw) {
        if (getSDLockSupport()) {
            return HwSdLockService.getInstance(this.mContext).unlockSDCard(pw);
        }
        return -1;
    }

    public void eraseSDLock() {
        if (getSDLockSupport()) {
            HwSdLockService.getInstance(this.mContext).eraseSDLock();
        }
    }

    public int getSDLockState() {
        if (getSDLockSupport()) {
            return HwSdLockService.getInstance(this.mContext).getSDLockState();
        }
        return -1;
    }

    public String getSDCardId() {
        if (getSDLockSupport()) {
            return HwSdLockService.getInstance(this.mContext).getSDCardId();
        }
        return null;
    }

    private boolean getSDLockSupport() {
        long ident = Binder.clearCallingIdentity();
        try {
            boolean state = SystemProperties.getBoolean("ro.config.support_sdcard_lock", true);
            return state;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void startFileBackup() {
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.HUAWEI_SYSTEM_NODE_ACCESS", null);
        SystemProperties.set("ctl.start", "filebackup");
    }

    public int forceIdle() {
        if (1000 != Binder.getCallingUid()) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("HwGeneralService:forceIdle, permission not allowed. uid = ");
            stringBuilder.append(Binder.getCallingUid());
            Slog.e(str, stringBuilder.toString());
            return -1;
        }
        if (this.dic == null) {
            this.dic = IDeviceIdleController.Stub.asInterface(ServiceManager.getService("deviceidle"));
        }
        if (this.dic != null) {
            try {
                return this.dic.forceIdle();
            } catch (RemoteException e) {
            }
        }
        return -1;
    }

    public boolean isSupportForce() {
        if (!this.init) {
            initForce();
            this.init = true;
            if (mIsForcetouchDisabled) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("mIsForcetouchDisabled = ");
                stringBuilder.append(mIsForcetouchDisabled);
                Log.i(str, stringBuilder.toString());
                this.mIsSupportForce = false;
            }
        }
        return this.mIsSupportForce;
    }

    public boolean isCurveScreen() {
        if (this.mIsCurveScreen == -1) {
            initScreenState();
        }
        return this.mIsCurveScreen == 1;
    }

    private void parseScreenParams(String strParas) {
        String[] params = strParas.split(",");
        for (String param : params) {
            int pos = param.indexOf(":");
            if (pos > 0 && pos < param.length()) {
                String key = param.substring(0, pos).trim();
                String val = param.substring(pos + 1).trim();
                if ("curved".equalsIgnoreCase(key)) {
                    this.mIsCurveScreen = val.equals("1");
                }
            }
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("parseScreenParams ");
        stringBuilder.append(this.mIsCurveScreen);
        stringBuilder.append(" ");
        stringBuilder.append(strParas);
        Log.i(str, stringBuilder.toString());
    }

    private void initScreenState() {
        String readLine;
        String str;
        StringBuilder stringBuilder;
        IOException e;
        BufferedReader reader = null;
        String line = null;
        this.mIsCurveScreen = 0;
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("sercie initScreenState");
        stringBuilder2.append(this.mIsCurveScreen);
        Log.v(str2, stringBuilder2.toString());
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(CFG_FILE_SCREEN);
            reader = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
            readLine = reader.readLine();
            line = readLine;
            if (readLine != null) {
                parseScreenParams(line);
            }
            try {
                reader.close();
            } catch (IOException e2) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("initScreenState reader close exception: ");
                stringBuilder.append(e2);
                Log.e(str, stringBuilder.toString());
            }
            try {
                fis.close();
            } catch (IOException e3) {
                e2 = e3;
                str = TAG;
                stringBuilder = new StringBuilder();
            }
        } catch (FileNotFoundException e4) {
            Log.e(TAG, "initScreenState file not found exception.");
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e22) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("initScreenState reader close exception: ");
                    stringBuilder.append(e22);
                    Log.e(str, stringBuilder.toString());
                }
            }
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e5) {
                    e22 = e5;
                    str = TAG;
                    stringBuilder = new StringBuilder();
                }
            }
        } catch (IOException e6) {
            Log.e(TAG, "initScreenState file access io exception.");
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e222) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("initScreenState reader close exception: ");
                    stringBuilder.append(e222);
                    Log.e(str, stringBuilder.toString());
                }
            }
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e7) {
                    e222 = e7;
                    str = TAG;
                    stringBuilder = new StringBuilder();
                }
            }
        } catch (Throwable th) {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e8) {
                    String str3 = TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("initScreenState reader close exception: ");
                    stringBuilder3.append(e8);
                    Log.e(str3, stringBuilder3.toString());
                }
            }
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e82) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("initScreenState fis close exception: ");
                    stringBuilder.append(e82);
                    Log.e(TAG, stringBuilder.toString());
                }
            }
        }
        readLine = TAG;
        StringBuilder stringBuilder4 = new StringBuilder();
        stringBuilder4.append("initScreenState isCurveScreen = ");
        stringBuilder4.append(this.mIsCurveScreen);
        stringBuilder4.append(" params = ");
        stringBuilder4.append(line);
        Log.i(readLine, stringBuilder4.toString());
        stringBuilder.append("initScreenState fis close exception: ");
        stringBuilder.append(e222);
        Log.e(str, stringBuilder.toString());
        readLine = TAG;
        StringBuilder stringBuilder42 = new StringBuilder();
        stringBuilder42.append("initScreenState isCurveScreen = ");
        stringBuilder42.append(this.mIsCurveScreen);
        stringBuilder42.append(" params = ");
        stringBuilder42.append(line);
        Log.i(readLine, stringBuilder42.toString());
    }

    private void initForce() {
        String str;
        StringBuilder stringBuilder;
        Exception e;
        BufferedReader reader = null;
        int line2int = 0;
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(FILE_PATH);
            reader = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
            String readLine = reader.readLine();
            String line = readLine;
            if (readLine != null) {
                line2int = Integer.parseInt(line.trim());
                if ((line2int & 16) == 16) {
                    this.mIsSupportForce = true;
                }
            }
            try {
                reader.close();
            } catch (Exception e2) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("initForce reader close exception: ");
                stringBuilder.append(e2);
                Log.e(str, stringBuilder.toString());
            }
            try {
                fis.close();
            } catch (Exception e3) {
                e2 = e3;
                str = TAG;
                stringBuilder = new StringBuilder();
            }
        } catch (FileNotFoundException e4) {
            Log.e(TAG, "initForce file not found exception.");
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e22) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("initForce reader close exception: ");
                    stringBuilder.append(e22);
                    Log.e(str, stringBuilder.toString());
                }
            }
            if (fis != null) {
                try {
                    fis.close();
                } catch (Exception e5) {
                    e22 = e5;
                    str = TAG;
                    stringBuilder = new StringBuilder();
                }
            }
        } catch (IOException e6) {
            Log.e(TAG, "initForce file access io exception.");
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e222) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("initForce reader close exception: ");
                    stringBuilder.append(e222);
                    Log.e(str, stringBuilder.toString());
                }
            }
            if (fis != null) {
                try {
                    fis.close();
                } catch (Exception e7) {
                    e222 = e7;
                    str = TAG;
                    stringBuilder = new StringBuilder();
                }
            }
        } catch (Throwable th) {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e8) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("initForce reader close exception: ");
                    stringBuilder2.append(e8);
                    Log.e(str2, stringBuilder2.toString());
                }
            }
            if (fis != null) {
                try {
                    fis.close();
                } catch (Exception e82) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("initForce fis close exception: ");
                    stringBuilder.append(e82);
                    Log.e(TAG, stringBuilder.toString());
                }
            }
        }
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("initForce   mIsSupportForce = ");
        stringBuilder3.append(this.mIsSupportForce);
        stringBuilder3.append(" line2int = ");
        stringBuilder3.append(line2int);
        Flog.i(1504, stringBuilder3.toString());
        stringBuilder.append("initForce fis close exception: ");
        stringBuilder.append(e222);
        Log.e(str, stringBuilder.toString());
        StringBuilder stringBuilder32 = new StringBuilder();
        stringBuilder32.append("initForce   mIsSupportForce = ");
        stringBuilder32.append(this.mIsSupportForce);
        stringBuilder32.append(" line2int = ");
        stringBuilder32.append(line2int);
        Flog.i(1504, stringBuilder32.toString());
    }

    private void initObserver(Handler handler) {
        this.mPressLimitObserver = new ContentObserver(handler) {
            public void onChange(boolean selfChange) {
                HwGeneralService.this.mLimit = System.getFloatForUser(HwGeneralService.this.mContext.getContentResolver(), "pressure_habit_threshold", 0.2f, ActivityManager.getCurrentUser());
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("initObserver limit = ");
                stringBuilder.append(HwGeneralService.this.mLimit);
                Flog.i(1504, stringBuilder.toString());
            }
        };
        this.mResolver.registerContentObserver(System.getUriFor("pressure_habit_threshold"), false, this.mPressLimitObserver, -1);
        this.mLimit = System.getFloatForUser(this.mContext.getContentResolver(), "pressure_habit_threshold", 0.2f, ActivityManager.getCurrentUser());
    }

    public float getPressureLimit() {
        return this.mLimit;
    }

    public void playIvtEffect(String effectName) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.VIBRATE") != 0) {
            Log.e(TAG, "playIvtEffect Method requires android.Manifest.permission.VIBRATE permission");
        } else {
            this.mIVibetonzImpl.playIvtEffect(effectName);
        }
    }

    public void stopPlayEffect() {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.VIBRATE") != 0) {
            Log.e(TAG, "stopPlayEffect Method requires android.Manifest.permission.VIBRATE permission");
        } else {
            this.mIVibetonzImpl.stopPlayEffect();
        }
    }

    public void pausePlayEffect(String effectName) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.VIBRATE") != 0) {
            Log.e(TAG, "pausePlayEffect Method requires android.Manifest.permission.VIBRATE permission");
        } else {
            this.mIVibetonzImpl.pausePlayEffect(effectName);
        }
    }

    public void resumePausedEffect(String effectName) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.VIBRATE") != 0) {
            Log.e(TAG, "resumePausedEffect Method requires android.Manifest.permission.VIBRATE permission");
        } else {
            this.mIVibetonzImpl.resumePausedEffect(effectName);
        }
    }

    public boolean isPlaying(String effectName) {
        return this.mIVibetonzImpl.isPlaying(effectName);
    }

    public boolean startHaptic(int callerID, int ringtoneType, Uri uri) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.VIBRATE") == 0) {
            return this.mIVibetonzImpl.startHaptic(this.mContext, callerID, ringtoneType, uri);
        }
        Log.e(TAG, "startHaptic Method requires android.Manifest.permission.VIBRATE permission");
        return false;
    }

    public boolean hasHaptic(Uri uri) {
        return this.mIVibetonzImpl.hasHaptic(this.mContext, uri);
    }

    public void stopHaptic() {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.VIBRATE") != 0) {
            Log.e(TAG, "stopHaptic Method requires android.Manifest.permission.VIBRATE permission");
        } else {
            this.mIVibetonzImpl.stopHaptic();
        }
    }

    public void resetTouchWeight() {
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.CHECK_TOUCH_WEIGHT", null);
        HwTouchWeightService.getInstance(this.mContext, this.mHandler).resetTouchWeight();
    }

    public String getTouchWeightValue() {
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.CHECK_TOUCH_WEIGHT", null);
        return HwTouchWeightService.getInstance(this.mContext, this.mHandler).getTouchWeightValue();
    }

    public boolean mkDataDir(String path) {
        if (1000 != Binder.getCallingUid()) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("HwGeneralService:mkDataDir, permission not allowed. uid = ");
            stringBuilder.append(Binder.getCallingUid());
            Slog.e(str, stringBuilder.toString());
            return false;
        } else if (path == null || "".equals(path)) {
            Log.e(TAG, "path is null");
            return false;
        } else if (path.startsWith("/data/")) {
            File file = new File(path);
            if (file.exists()) {
                return true;
            }
            return file.mkdir();
        } else {
            Log.e(TAG, "path not startsWith data dir");
            return false;
        }
    }

    public Messenger getTestService() {
        if (!"true".equals(SystemProperties.get("ro.emui.test", "false"))) {
            return null;
        }
        try {
            PathClassLoader loader = new PathClassLoader("/system/framework/HwServiceTest.jar", getClass().getClassLoader());
            Thread.currentThread().setContextClassLoader(loader);
            Class clazz = loader.loadClass("com.huawei.test.systemserver.service.TestRunnerService");
            return (Messenger) clazz.getMethod("onBind", null).invoke(clazz.getConstructor(new Class[]{Context.class}).newInstance(new Object[]{this.mContext}), null);
        } catch (Throwable e) {
            Log.e(TAG, "can  not get test service", e);
            return null;
        }
    }

    public int readProtectArea(String optItem, int readBufLen, String[] readBuf, int[] errorNum) {
        int pid = Binder.getCallingPid();
        int uid = Binder.getCallingUid();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("readProtectArea: callingPid is ");
        stringBuilder.append(pid);
        stringBuilder.append(",callingUid is ");
        stringBuilder.append(uid);
        Slog.i(str, stringBuilder.toString());
        return HwProtectAreaService.getInstance(this.mContext).readProtectArea(optItem, readBufLen, readBuf, errorNum);
    }

    public int writeProtectArea(String optItem, int writeLen, String writeBuf, int[] errorNum) {
        int pid = Binder.getCallingPid();
        int uid = Binder.getCallingUid();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("writeProtectArea: callingPid is ");
        stringBuilder.append(pid);
        stringBuilder.append(",callingUid is ");
        stringBuilder.append(uid);
        Slog.i(str, stringBuilder.toString());
        return HwProtectAreaService.getInstance(this.mContext).writeProtectArea(optItem, writeLen, writeBuf, errorNum);
    }

    public int setSdCardCryptdEnable(boolean enable, String volId) {
        if (getCryptsdSupport()) {
            return HwSdCryptdService.getInstance(this.mContext).setSdCardCryptdEnable(enable, volId);
        }
        return -10;
    }

    public int unlockSdCardKey(int userId, int serialNumber, byte[] token, byte[] secret) {
        if (getCryptsdSupport()) {
            return HwSdCryptdService.getInstance(this.mContext).unlockSdCardKey(userId, serialNumber, token, secret);
        }
        return -10;
    }

    public int addSdCardUserKeyAuth(int userId, int serialNumber, byte[] token, byte[] secret) {
        if (getCryptsdSupport()) {
            return HwSdCryptdService.getInstance(this.mContext).addSdCardUserKeyAuth(userId, serialNumber, token, secret);
        }
        return -10;
    }

    private boolean getCryptsdSupport() {
        long ident = Binder.clearCallingIdentity();
        try {
            boolean state = SystemProperties.getBoolean("ro.config.support_sdcard_crypt", true);
            return state;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public int backupSecretkey() {
        if (getCryptsdSupport()) {
            return HwSdCryptdService.getInstance(this.mContext).backupSecretkey();
        }
        return -10;
    }

    public boolean supportHwPush() {
        File jarFile = HwCfgFilePolicy.getCfgFile("jars/hwpush.jar", 0);
        if (jarFile != null && jarFile.exists()) {
            Slog.i(TAG, "push jarFile is exist in cust");
            return true;
        } else if (new File("/system/framework/hwpush.jar").exists()) {
            Slog.i(TAG, "push jarFile is exist in system");
            return true;
        } else {
            Slog.i(TAG, "push jarFile is not exist");
            return false;
        }
    }

    public long getPartitionInfo(String partitionName, int infoType) {
        if (HwStorageManagerService.sSelf != null) {
            return HwStorageManagerService.sSelf.getPartitionInfo(partitionName, infoType);
        }
        Slog.i(TAG, "error getPartitionInfo HwGeneralService NULL PTR");
        return -1;
    }

    public String mountCifs(String source, String option, IBinder binder) {
        return HwMountManagerService.getInstance(this.mContext).mountCifs(source, option, binder);
    }

    public void unmountCifs(String mountPoint) {
        HwMountManagerService.getInstance(this.mContext).unmountCifs(mountPoint);
    }

    public int isSupportedCifs() {
        return HwMountManagerService.getInstance(this.mContext).isSupportedCifs();
    }

    public int getLocalDevStat(int dev) {
        return HwLocalDevManagerService.getInstance(this.mContext).getLocalDevStat(dev);
    }

    public String getDeviceId(int dev) {
        return HwLocalDevManagerService.getInstance(this.mContext).getDeviceId(dev);
    }

    public int doSdcardCheckRW() {
        return HwLocalDevManagerService.getInstance(this.mContext).doSdcardCheckRW();
    }
}
