package com.android.server;

import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IDeviceIdleController;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.Settings.System;
import android.system.ErrnoException;
import android.system.Os;
import android.util.Flog;
import android.util.Log;
import android.util.Slog;
import com.android.server.VibetonzProxy.IVibetonzImpl;
import com.android.server.devicepolicy.StorageUtils;
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
    private static final String MANAGE_USE_SECURITY = "com.huawei.permission.MANAGE_USE_SECURITY";
    private static final int NOT_SUPPORT_SDLOCK = -1;
    private static final int PERMISSION_NOT_ALLOW = -200;
    public static final int RESULT_NOT_SUPPORT = -10;
    static final String TAG = "HwGeneralService";
    IDeviceIdleController dic = null;
    private boolean init = false;
    private Context mContext;
    private Handler mHandler;
    private IVibetonzImpl mIVibetonzImpl = this.mVibetonzProxy.getInstance();
    private int mIsCurveScreen = -1;
    private boolean mIsSupportForce = false;
    private float mLimit = 0.0f;
    private ContentObserver mPressLimitObserver;
    private final ContentResolver mResolver;
    private VibetonzProxy mVibetonzProxy = new VibetonzProxy();

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
            Slog.e(TAG, "HwGeneralService:forceIdle, permission not allowed. uid = " + Binder.getCallingUid());
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

    static {
        try {
            System.loadLibrary("general_jni");
        } catch (UnsatisfiedLinkError e) {
            Slog.e(TAG, "Load general libarary failed >>>>>" + e);
        }
    }

    public boolean isSupportForce() {
        if (!this.init) {
            initForce();
            this.init = true;
        }
        return this.mIsSupportForce;
    }

    public boolean isCurveScreen() {
        if (this.mIsCurveScreen == -1) {
            initScreenState();
        }
        if (this.mIsCurveScreen == 1) {
            return true;
        }
        return false;
    }

    private void parseScreenParams(String strParas) {
        String[] params = strParas.split(",");
        for (String param : params) {
            int pos = param.indexOf(":");
            if (pos > 0 && pos < param.length()) {
                String key = param.substring(0, pos).trim();
                String val = param.substring(pos + 1).trim();
                if ("curved".equalsIgnoreCase(key)) {
                    this.mIsCurveScreen = val.equals("1") ? 1 : 0;
                }
            }
        }
        Log.i(TAG, "parseScreenParams " + this.mIsCurveScreen + " " + strParas);
    }

    private void initScreenState() {
        BufferedReader reader;
        Throwable th;
        BufferedReader bufferedReader = null;
        String line = null;
        this.mIsCurveScreen = 0;
        Log.v(TAG, "sercie initScreenState" + this.mIsCurveScreen);
        FileInputStream fileInputStream = null;
        try {
            FileInputStream fis = new FileInputStream(CFG_FILE_SCREEN);
            try {
                reader = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
            } catch (FileNotFoundException e) {
                fileInputStream = fis;
                Log.e(TAG, "initScreenState file not found exception.");
                if (bufferedReader != null) {
                    try {
                        bufferedReader.close();
                    } catch (IOException e2) {
                        Log.e(TAG, "initScreenState reader close exception: " + e2);
                    }
                }
                if (fileInputStream != null) {
                    try {
                        fileInputStream.close();
                    } catch (IOException e22) {
                        Log.e(TAG, "initScreenState fis close exception: " + e22);
                    }
                }
                Log.i(TAG, "initScreenState isCurveScreen = " + this.mIsCurveScreen + " params = " + line);
            } catch (IOException e3) {
                fileInputStream = fis;
                try {
                    Log.e(TAG, "initScreenState file access io exception.");
                    if (bufferedReader != null) {
                        try {
                            bufferedReader.close();
                        } catch (IOException e222) {
                            Log.e(TAG, "initScreenState reader close exception: " + e222);
                        }
                    }
                    if (fileInputStream != null) {
                        try {
                            fileInputStream.close();
                        } catch (IOException e2222) {
                            Log.e(TAG, "initScreenState fis close exception: " + e2222);
                        }
                    }
                    Log.i(TAG, "initScreenState isCurveScreen = " + this.mIsCurveScreen + " params = " + line);
                } catch (Throwable th2) {
                    th = th2;
                    if (bufferedReader != null) {
                        try {
                            bufferedReader.close();
                        } catch (IOException e22222) {
                            Log.e(TAG, "initScreenState reader close exception: " + e22222);
                        }
                    }
                    if (fileInputStream != null) {
                        try {
                            fileInputStream.close();
                        } catch (IOException e222222) {
                            Log.e(TAG, "initScreenState fis close exception: " + e222222);
                        }
                    }
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                fileInputStream = fis;
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
                throw th;
            }
            try {
                line = reader.readLine();
                if (line != null) {
                    parseScreenParams(line);
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e2222222) {
                        Log.e(TAG, "initScreenState reader close exception: " + e2222222);
                    }
                }
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException e22222222) {
                        Log.e(TAG, "initScreenState fis close exception: " + e22222222);
                    }
                }
                bufferedReader = reader;
            } catch (FileNotFoundException e4) {
                fileInputStream = fis;
                bufferedReader = reader;
                Log.e(TAG, "initScreenState file not found exception.");
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
                Log.i(TAG, "initScreenState isCurveScreen = " + this.mIsCurveScreen + " params = " + line);
            } catch (IOException e5) {
                fileInputStream = fis;
                bufferedReader = reader;
                Log.e(TAG, "initScreenState file access io exception.");
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
                Log.i(TAG, "initScreenState isCurveScreen = " + this.mIsCurveScreen + " params = " + line);
            } catch (Throwable th4) {
                th = th4;
                fileInputStream = fis;
                bufferedReader = reader;
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
                throw th;
            }
        } catch (FileNotFoundException e6) {
            Log.e(TAG, "initScreenState file not found exception.");
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            if (fileInputStream != null) {
                fileInputStream.close();
            }
            Log.i(TAG, "initScreenState isCurveScreen = " + this.mIsCurveScreen + " params = " + line);
        } catch (IOException e7) {
            Log.e(TAG, "initScreenState file access io exception.");
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            if (fileInputStream != null) {
                fileInputStream.close();
            }
            Log.i(TAG, "initScreenState isCurveScreen = " + this.mIsCurveScreen + " params = " + line);
        }
        Log.i(TAG, "initScreenState isCurveScreen = " + this.mIsCurveScreen + " params = " + line);
    }

    private void initForce() {
        Throwable th;
        BufferedReader bufferedReader = null;
        int line2int = 0;
        FileInputStream fileInputStream = null;
        try {
            BufferedReader reader;
            FileInputStream fis = new FileInputStream(FILE_PATH);
            try {
                reader = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
            } catch (FileNotFoundException e) {
                fileInputStream = fis;
                Log.e(TAG, "initForce file not found exception.");
                if (bufferedReader != null) {
                    try {
                        bufferedReader.close();
                    } catch (Exception e2) {
                        Log.e(TAG, "initForce reader close exception: " + e2);
                    }
                }
                if (fileInputStream != null) {
                    try {
                        fileInputStream.close();
                    } catch (Exception e22) {
                        Log.e(TAG, "initForce fis close exception: " + e22);
                    }
                }
                Flog.i(1504, "initForce   mIsSupportForce = " + this.mIsSupportForce + " line2int = " + line2int);
            } catch (IOException e3) {
                fileInputStream = fis;
                try {
                    Log.e(TAG, "initForce file access io exception.");
                    if (bufferedReader != null) {
                        try {
                            bufferedReader.close();
                        } catch (Exception e222) {
                            Log.e(TAG, "initForce reader close exception: " + e222);
                        }
                    }
                    if (fileInputStream != null) {
                        try {
                            fileInputStream.close();
                        } catch (Exception e2222) {
                            Log.e(TAG, "initForce fis close exception: " + e2222);
                        }
                    }
                    Flog.i(1504, "initForce   mIsSupportForce = " + this.mIsSupportForce + " line2int = " + line2int);
                } catch (Throwable th2) {
                    th = th2;
                    if (bufferedReader != null) {
                        try {
                            bufferedReader.close();
                        } catch (Exception e22222) {
                            Log.e(TAG, "initForce reader close exception: " + e22222);
                        }
                    }
                    if (fileInputStream != null) {
                        try {
                            fileInputStream.close();
                        } catch (Exception e222222) {
                            Log.e(TAG, "initForce fis close exception: " + e222222);
                        }
                    }
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                fileInputStream = fis;
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
                throw th;
            }
            try {
                String line = reader.readLine();
                if (line != null) {
                    line2int = Integer.parseInt(line.trim());
                    if ((line2int & 16) == 16) {
                        this.mIsSupportForce = true;
                    }
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (Exception e2222222) {
                        Log.e(TAG, "initForce reader close exception: " + e2222222);
                    }
                }
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (Exception e22222222) {
                        Log.e(TAG, "initForce fis close exception: " + e22222222);
                    }
                }
                bufferedReader = reader;
            } catch (FileNotFoundException e4) {
                fileInputStream = fis;
                bufferedReader = reader;
                Log.e(TAG, "initForce file not found exception.");
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
                Flog.i(1504, "initForce   mIsSupportForce = " + this.mIsSupportForce + " line2int = " + line2int);
            } catch (IOException e5) {
                fileInputStream = fis;
                bufferedReader = reader;
                Log.e(TAG, "initForce file access io exception.");
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
                Flog.i(1504, "initForce   mIsSupportForce = " + this.mIsSupportForce + " line2int = " + line2int);
            } catch (Throwable th4) {
                th = th4;
                fileInputStream = fis;
                bufferedReader = reader;
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
                throw th;
            }
        } catch (FileNotFoundException e6) {
            Log.e(TAG, "initForce file not found exception.");
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            if (fileInputStream != null) {
                fileInputStream.close();
            }
            Flog.i(1504, "initForce   mIsSupportForce = " + this.mIsSupportForce + " line2int = " + line2int);
        } catch (IOException e7) {
            Log.e(TAG, "initForce file access io exception.");
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            if (fileInputStream != null) {
                fileInputStream.close();
            }
            Flog.i(1504, "initForce   mIsSupportForce = " + this.mIsSupportForce + " line2int = " + line2int);
        }
        Flog.i(1504, "initForce   mIsSupportForce = " + this.mIsSupportForce + " line2int = " + line2int);
    }

    private void initObserver(Handler handler) {
        this.mPressLimitObserver = new ContentObserver(handler) {
            public void onChange(boolean selfChange) {
                HwGeneralService.this.mLimit = System.getFloatForUser(HwGeneralService.this.mContext.getContentResolver(), "pressure_habit_threshold", 0.2f, ActivityManager.getCurrentUser());
                Flog.i(1504, "initObserver limit = " + HwGeneralService.this.mLimit);
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
            Slog.e(TAG, "HwGeneralService:mkDataDir, permission not allowed. uid = " + Binder.getCallingUid());
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
        if (!StorageUtils.SDCARD_ROMOUNTED_STATE.equals(SystemProperties.get("ro.emui.test", StorageUtils.SDCARD_RWMOUNTED_STATE))) {
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
        Slog.i(TAG, "readProtectArea: callingPid is " + pid + ",callingUid is " + Binder.getCallingUid());
        return HwProtectAreaService.getInstance(this.mContext).readProtectArea(optItem, readBufLen, readBuf, errorNum);
    }

    public int writeProtectArea(String optItem, int writeLen, String writeBuf, int[] errorNum) {
        int pid = Binder.getCallingPid();
        Slog.i(TAG, "writeProtectArea: callingPid is " + pid + ",callingUid is " + Binder.getCallingUid());
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
        if (this.mContext.checkCallingPermission(MANAGE_USE_SECURITY) != 0) {
            Slog.i(TAG, "getPartitionInfo(): permissin deny");
            return -200;
        } else if (partitionName == null) {
            Slog.i(TAG, "getPartitionInfo error partitionName null");
            return -201;
        } else if (-1 == partitionName.indexOf(46) && -1 == partitionName.indexOf(47)) {
            long result = -1;
            try {
                String pathLink = Os.readlink(String.format("/dev/block/bootdevice/by-name/%s", new Object[]{partitionName}));
                String realPartitionName = pathLink.substring(pathLink.lastIndexOf("/") + 1);
                switch (infoType) {
                    case 1:
                        result = HwPartitionInfo.getPartitionStartPos(realPartitionName);
                        break;
                    case 2:
                        result = HwPartitionInfo.getPartitionSize(realPartitionName);
                        break;
                    case 3:
                        result = HwPartitionInfo.getPartitionMeta(realPartitionName);
                        break;
                    case 4:
                        result = HwPartitionInfo.getPartitionValidNode(realPartitionName);
                        break;
                }
                return result;
            } catch (ErrnoException e) {
                Slog.i(TAG, "getPartitionValidNode IOException !", e);
                return -205;
            }
        } else {
            Slog.i(TAG, "getPartitionInfo error partitionName valid " + partitionName);
            return -202;
        }
    }
}
