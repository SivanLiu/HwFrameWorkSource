package com.android.server.os;

import android.app.ActivityManagerInternal;
import android.os.Binder;
import android.os.FileUtils;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.Slog;
import com.android.server.LocalServices;
import com.android.server.SystemServiceManager;
import com.android.server.Watchdog;
import com.android.server.am.ActivityManagerService;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public final class HwBootCheck {
    private static final boolean DEBUG_MAIN_THREAD_BLOCK;
    public static final int MESSAGE_CHECK_AFTER_AMS_INIT = 100;
    public static final int MESSAGE_CHECK_AFTER_BOOT_DEXOPT = 102;
    public static final int MESSAGE_CHECK_AFTER_PMS_INIT = 101;
    public static final int MESSAGE_CHECK_APP_DEXOPT = 104;
    public static final int MESSAGE_CHECK_FRAMEWORK_JAR_DEXOPT = 105;
    public static final int MESSAGE_CHECK_PERFORM_SYSTEM_SERVER_DEXOPT = 103;
    private static final String TAG = "HwBootFail";
    static boolean isBootSuccess = false;
    private static Handler mBootCheckHandler;
    private static HandlerThread mBootCheckThread;
    private static StringBuilder mBootInfo = new StringBuilder();

    private static final class BootCheckHandler extends Handler {
        public BootCheckHandler(Looper looper) {
            super(looper, null, true);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 100:
                case 101:
                case 102:
                case 103:
                case 104:
                case 105:
                    try {
                        String str = HwBootCheck.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("mBootCheckHandler: ");
                        stringBuilder.append(msg.what);
                        Slog.i(str, stringBuilder.toString());
                        HwBootCheck.addBootInfo("performSystemServerDexOpt: installer.waitForConnection");
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("currBootScene is: ");
                        stringBuilder2.append(msg.what);
                        HwBootCheck.addBootInfo(stringBuilder2.toString());
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("mSystemReady is: ");
                        stringBuilder2.append(((ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class)).isSystemReady());
                        HwBootCheck.addBootInfo(stringBuilder2.toString());
                        HwBootCheck.bootSceneEnd(msg.what);
                        if (HwBootCheck.DEBUG_MAIN_THREAD_BLOCK && !HwBootCheck.isBootSuccess) {
                            HwBootCheck.addBootFailedLog();
                            return;
                        }
                        return;
                    } catch (Exception ex) {
                        String str2 = HwBootCheck.TAG;
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("BootCheckHandler exception: ");
                        stringBuilder3.append(ex.toString());
                        Slog.e(str2, stringBuilder3.toString());
                        return;
                    }
                default:
                    return;
            }
        }
    }

    static {
        boolean z = false;
        if (SystemProperties.getInt("ro.logsystem.usertype", 1) == 3) {
            z = true;
        }
        DEBUG_MAIN_THREAD_BLOCK = z;
    }

    private static void addBootFailedLog() {
        ArrayList pids = new ArrayList();
        pids.add(Integer.valueOf(Process.myPid()));
        int[] nativePidsInt = Process.getPidsForCommands(Watchdog.NATIVE_STACKS_OF_INTEREST);
        ArrayList nativePids = null;
        if (nativePidsInt != null) {
            nativePids = new ArrayList(nativePidsInt.length);
            for (int i : nativePidsInt) {
                nativePids.add(Integer.valueOf(i));
            }
        }
        File stack = ActivityManagerService.dumpStackTraces(true, pids, null, null, nativePids);
        if (stack == null) {
            stack = dumpStackTraces();
        }
        Watchdog.getInstance().addKernelLog();
        addBootInfo(((SystemServiceManager) LocalServices.getService(SystemServiceManager.class)).dumpInfo());
        SystemClock.sleep(2000);
        HwBootFail.bootFailError(83886081, 1, HwBootFail.creatFrameworkBootFailLog(stack, getBootInfo()));
    }

    private static File dumpStackTraces() {
        String tracesPath = SystemProperties.get("dalvik.vm.stack-trace-file", null);
        if (tracesPath == null || tracesPath.length() == 0) {
            return null;
        }
        File tracesFile = new File(tracesPath);
        try {
            if (tracesFile.exists() && tracesFile.delete()) {
                Slog.w(TAG, "Unable to delete boot fail traces file");
            }
            if (tracesFile.createNewFile()) {
                FileUtils.setPermissions(tracesFile.getPath(), 438, -1, -1);
            }
            Process.sendSignal(Process.myPid(), 3);
            return tracesFile;
        } catch (IOException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unable to prepare boot fail traces file: ");
            stringBuilder.append(tracesPath);
            Slog.w(str, stringBuilder.toString(), e);
            return null;
        }
    }

    private static void ensureThreadLocked() {
        if (mBootCheckThread == null) {
            mBootCheckThread = new HandlerThread("bootCheck");
            mBootCheckThread.start();
            mBootCheckHandler = new BootCheckHandler(mBootCheckThread.getLooper());
        }
    }

    public static HandlerThread getHandlerThread() {
        HandlerThread handlerThread;
        synchronized (HwBootCheck.class) {
            ensureThreadLocked();
            handlerThread = mBootCheckThread;
        }
        return handlerThread;
    }

    public static Handler getHandler() {
        Handler handler;
        synchronized (HwBootCheck.class) {
            ensureThreadLocked();
            handler = mBootCheckHandler;
        }
        return handler;
    }

    public static void bootCheckThreadQuit() {
        mBootInfo.delete(0, mBootInfo.length());
        getHandlerThread().quit();
    }

    public static boolean bootSceneStart(int sceneId, long maxTime) {
        try {
            String str;
            StringBuilder stringBuilder;
            if (10000 <= Binder.getCallingUid()) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("permission not allowed. uid = ");
                stringBuilder.append(Binder.getCallingUid());
                Slog.e(str, stringBuilder.toString());
                return false;
            } else if (getHandlerThread().isAlive()) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("bootSceneStart :");
                stringBuilder.append(sceneId);
                Slog.i(str, stringBuilder.toString());
                if (!mBootCheckHandler.hasMessages(sceneId)) {
                    mBootCheckHandler.sendEmptyMessageDelayed(sceneId, maxTime);
                }
                return true;
            } else {
                Slog.w(TAG, "mBootCheckThread is not alive");
                return false;
            }
        } catch (Exception ex) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("get ex:");
            stringBuilder2.append(ex);
            Slog.e(str2, stringBuilder2.toString());
            return false;
        }
    }

    public static boolean bootSceneEnd(int sceneId) {
        try {
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
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("bootSceneEnd :");
            stringBuilder.append(sceneId);
            Slog.i(str, stringBuilder.toString());
            if (mBootCheckHandler.hasMessages(sceneId)) {
                mBootCheckHandler.removeMessages(sceneId);
            }
            return true;
        } catch (Exception ex) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("has ex:");
            stringBuilder2.append(ex);
            Slog.e(str2, stringBuilder2.toString());
            return false;
        }
    }

    public static String getBootInfo() {
        try {
            if (10000 > Binder.getCallingUid()) {
                return mBootInfo.toString();
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("permission not allowed. uid = ");
            stringBuilder.append(Binder.getCallingUid());
            Slog.e(str, stringBuilder.toString());
            return null;
        } catch (Exception ex) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("get exception ex:");
            stringBuilder2.append(ex);
            Slog.e(str2, stringBuilder2.toString());
            return null;
        }
    }

    public static boolean addBootInfo(String info) {
        try {
            if (isBootSuccess) {
                return false;
            }
            if (10000 <= Binder.getCallingUid()) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("permission not allowed. uid = ");
                stringBuilder.append(Binder.getCallingUid());
                Slog.e(str, stringBuilder.toString());
                return false;
            } else if (mBootInfo == null) {
                return false;
            } else {
                StringBuilder stringBuilder2 = mBootInfo;
                stringBuilder2.append(info);
                stringBuilder2.append("\n");
                return true;
            }
        } catch (Exception ex) {
            String str2 = TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("exception info ex:");
            stringBuilder3.append(ex);
            Slog.e(str2, stringBuilder3.toString());
            return false;
        }
    }
}
