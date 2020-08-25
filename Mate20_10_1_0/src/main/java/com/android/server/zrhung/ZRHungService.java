package com.android.server.zrhung;

import android.app.ActivityManager;
import android.app.ActivityThread;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Slog;
import android.util.ZRHung;
import android.zrhung.IFaultEventService;
import android.zrhung.ZrHungData;
import android.zrhung.appeye.AppEyeBinderBlock;
import com.android.server.SystemService;
import com.android.server.rms.iaware.feature.SceneRecogFeature;
import com.android.server.rms.iaware.memory.utils.BigMemoryConstant;
import com.android.server.rms.iaware.memory.utils.MemoryConstant;
import com.android.server.zrhung.appeye.AppEyeMessage;
import com.android.server.zrhung.appeye.AppEyeSocketThread;
import com.huawei.android.app.ActivityManagerEx;
import com.huawei.android.app.HwActivityManager;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;

public final class ZRHungService extends SystemService implements IZRHungService {
    private static final String FAULT_NOTIFY_SERVER_NAME = "hwFaultNotifyService";
    private static final String FAULT_TYPE_ANR = "anr";
    private static final String FAULT_TYPE_CRASH = "crash";
    private static final String FAULT_TYPE_TOMBSTONE = "tombstone";
    private static final int INIT_LIST_SIZE = 16;
    private static final int MSG_ZRHUNG_INIT = 1;
    private static final int MSG_ZRHUNG_NOTIFY_ANR_APP = 4;
    private static final int MSG_ZRHUNG_NOTIFY_CRASH_APP = 5;
    private static final int MSG_ZRHUNG_NOTIFY_TOMBSTONE_APP = 6;
    private static final int MSG_ZRHUNG_SOCKET_EVENT_RECEIVE = 2;
    private static final int MSG_ZRHUNG_USER_EVENT_DELAY = 3;
    private static final String TAG = "ZRHungService";
    private static final int ZRHUNG_ANR_INTERVAL = 6000;
    private static final int ZRHUNG_USER_EVENT_TIMEOUT = 10000;
    private static IZRHungService mService = null;
    private final HashMap<String, AppEyeANRData> mANRMap = new HashMap<>();
    private ActivityManager mActivityManager = null;
    private final Context mContext;
    private String[] mDirectRecoverPackageList = null;
    private final Handler mHandler;
    private Handler.Callback mHandlerCallback = new Handler.Callback() {
        /* class com.android.server.zrhung.ZRHungService.AnonymousClass1 */

        public boolean handleMessage(Message msg) {
            Slog.d(ZRHungService.TAG, "handleMessage :" + msg.what);
            int i = msg.what;
            if (i == 1) {
                ZRHungService.this.handleInitService();
                return true;
            } else if (i == 2) {
                ZRHungService.this.handleAppFreezeEvent((AppEyeMessage) msg.obj);
                return true;
            } else if (i == 3) {
                String packageName = ((AppEyeMessage) msg.obj).getAppPkgName();
                ZrHungData data = new ZrHungData();
                data.putString("packageName", packageName);
                boolean unused = ZRHungService.this.handleUserEvent(data, true);
                return true;
            } else if (i != 4) {
                return false;
            } else {
                AppEyeMessage appEyeMessage = null;
                if (msg.obj instanceof AppEyeMessage) {
                    appEyeMessage = (AppEyeMessage) msg.obj;
                }
                if (appEyeMessage == null) {
                    return false;
                }
                ZRHungService.this.startNotifyApp(appEyeMessage.getAppProcessName(), ZRHungService.FAULT_TYPE_ANR);
                return true;
            }
        }
    };
    private boolean mIsDirectRecoveryConfiged = false;
    private boolean mIsDirectRecoveryEnabled = false;
    private AppEyeSocketThread mSocketThread = null;

    public ZRHungService(Context context) {
        super(context);
        setInstance(this);
        this.mContext = context;
        HandlerThread handlerThread = new HandlerThread(TAG);
        handlerThread.start();
        this.mHandler = new Handler(handlerThread.getLooper(), this.mHandlerCallback);
        this.mActivityManager = (ActivityManager) context.getSystemService(BigMemoryConstant.BIGMEMINFO_ITEM_TAG);
        startFaultNotifyService(context);
        Slog.d(TAG, "ZRHungService on create!");
    }

    /* JADX WARN: Type inference failed for: r0v0, types: [com.android.server.zrhung.FaultNotifyService, android.os.IBinder] */
    private void startFaultNotifyService(Context context) {
        ServiceManager.addService(FAULT_NOTIFY_SERVER_NAME, (IBinder) new FaultNotifyService(this.mContext));
    }

    public static synchronized IZRHungService getInstance() {
        IZRHungService iZRHungService;
        synchronized (ZRHungService.class) {
            iZRHungService = mService;
        }
        return iZRHungService;
    }

    private static void setInstance(IZRHungService service) {
        mService = service;
    }

    public void onStart() {
        Slog.d(TAG, "onStart!");
    }

    public void onBootPhase(int phase) {
        Handler handler;
        Slog.d(TAG, "onBootPhase!");
        if (phase == 1000 && (handler = this.mHandler) != null) {
            handler.sendEmptyMessage(1);
        }
    }

    public boolean sendEvent(ZrHungData args) {
        if (args == null) {
            return false;
        }
        String eventType = args.getString("eventtype");
        char c = 65535;
        switch (eventType.hashCode()) {
            case -74099055:
                if (eventType.equals("socketrecover")) {
                    c = 0;
                    break;
                }
                break;
            case 1777228417:
                if (eventType.equals("recoverresult")) {
                    c = 1;
                    break;
                }
                break;
            case 1902096952:
                if (eventType.equals("notifyapp")) {
                    c = 4;
                    break;
                }
                break;
            case 1914956048:
                if (eventType.equals("showanrdialog")) {
                    c = 3;
                    break;
                }
                break;
            case 1985941039:
                if (eventType.equals("settime")) {
                    c = 2;
                    break;
                }
                break;
        }
        if (c == 0) {
            AppEyeMessage object = (AppEyeMessage) args.get("appeyemessage");
            if (object != null) {
                Message message = this.mHandler.obtainMessage();
                message.what = 2;
                message.obj = object;
                message.sendToTarget();
                Slog.i(TAG, "Receive a socket message from zrhung");
                return true;
            }
            Slog.i(TAG, "NULL message");
            return false;
        } else if (c != 1) {
            if (c == 2) {
                setAppEyeANRDataLocked(args);
                return true;
            } else if (c == 3) {
                return canShowANRDialogs(args);
            } else {
                if (c != 4) {
                    return false;
                }
                return readyToNotifyApp(args);
            }
        } else if (handleUserEvent(args, false)) {
            return true;
        } else {
            Slog.w(TAG, "sendEvent  recoverresult error!");
            return false;
        }
    }

    private boolean readyToNotifyApp(ZrHungData args) {
        AppEyeMessage appEyeMessage = new AppEyeMessage();
        ArrayList<String> lists = new ArrayList<>(16);
        lists.add("processName:" + args.getString(MemoryConstant.MEM_NATIVE_ITEM_PROCESSNAME));
        if (appEyeMessage.parseMsg(lists) != 0) {
            Slog.e(TAG, "Ready to notify app but no process name");
            return false;
        }
        Message message = this.mHandler.obtainMessage();
        message.what = getFaultType(args.getString("faulttype"));
        message.obj = appEyeMessage;
        message.sendToTarget();
        return true;
    }

    /* JADX WARNING: Removed duplicated region for block: B:17:0x003f  */
    /* JADX WARNING: Removed duplicated region for block: B:22:0x0048 A[RETURN] */
    private int getFaultType(String faultType) {
        char c;
        String newFaultType = faultType.toLowerCase(Locale.ROOT);
        int hashCode = newFaultType.hashCode();
        if (hashCode != 96741) {
            if (hashCode != 94921639) {
                if (hashCode == 1836179733 && newFaultType.equals(FAULT_TYPE_TOMBSTONE)) {
                    c = 2;
                    if (c != 0) {
                        return 4;
                    }
                    if (c == 1) {
                        return 5;
                    }
                    if (c != 2) {
                        return 0;
                    }
                    return 6;
                }
            } else if (newFaultType.equals(FAULT_TYPE_CRASH)) {
                c = 1;
                if (c != 0) {
                }
            }
        } else if (newFaultType.equals(FAULT_TYPE_ANR)) {
            c = 0;
            if (c != 0) {
            }
        }
        c = 65535;
        if (c != 0) {
        }
    }

    /* access modifiers changed from: private */
    public void startNotifyApp(String processName, String faultType) {
        try {
            IBinder FaultServer = ServiceManager.getService(FAULT_NOTIFY_SERVER_NAME);
            if (FaultServer != null) {
                IFaultEventService.Stub.asInterface(FaultServer).callBack(processName, faultType, new ArrayList<>(16));
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "failed to notify application");
        }
    }

    /* access modifiers changed from: private */
    public void handleInitService() {
        if (this.mSocketThread == null) {
            this.mSocketThread = new AppEyeSocketThread();
            this.mSocketThread.start();
            Slog.d(TAG, "handleInitService!");
            return;
        }
        Slog.d(TAG, "socketThread already start!");
    }

    /* access modifiers changed from: private */
    public void handleAppFreezeEvent(AppEyeMessage message) {
        if (message == null) {
            Slog.e(TAG, "null message or mPath received from Server");
            return;
        }
        Slog.d(TAG, "received message:" + message.toString());
        if (!isAdbDebuggingEnabled() || !isAppDebuggingEnabled(message.getAppPkgName())) {
            String command = message.getCommand();
            char c = 65535;
            int hashCode = command.hashCode();
            if (hashCode != -1547232625) {
                if (hashCode != -1164891468) {
                    if (hashCode == 1702423020 && command.equals(AppEyeMessage.KILL_MUlTI_PROCESSES)) {
                        c = 2;
                    }
                } else if (command.equals(AppEyeMessage.NOTIFY_USER)) {
                    c = 0;
                }
            } else if (command.equals(AppEyeMessage.KILL_PROCESS)) {
                c = 1;
            }
            if (c == 0) {
                notifyRecoverEventToUser(message);
            } else if (c == 1) {
                killApplicationProcess(message.getAppPid(), message.getAppPkgName());
            } else if (c != 2) {
                Slog.e(TAG, "invalid command: " + command);
            } else {
                Iterator<Integer> it = message.getAppPidList().iterator();
                while (it.hasNext()) {
                    killApplicationProcess(it.next().intValue(), message.getAppPkgName());
                }
            }
        } else {
            Slog.w(TAG, "app debug is enabled, do not handle app freeze event.");
        }
    }

    private boolean isAppDebuggingEnabled(String packageName) {
        ApplicationInfo appInfo;
        if (packageName == null) {
            return false;
        }
        try {
            if (ActivityThread.currentApplication() == null || ActivityThread.currentApplication().getApplicationContext() == null || ActivityThread.currentApplication().getApplicationContext().getPackageManager() == null || (appInfo = ActivityThread.currentApplication().getApplicationContext().getPackageManager().getApplicationInfo(packageName, 1)) == null || (appInfo.flags & 2) == 0) {
                return false;
            }
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            Slog.e(TAG, "could not get package name");
        }
        return false;
    }

    private boolean isAdbDebuggingEnabled() {
        try {
            if (Settings.Global.getInt(this.mContext.getContentResolver(), "adb_enabled") > 0) {
                return true;
            }
            return false;
        } catch (Settings.SettingNotFoundException e) {
            Slog.d(TAG, "Setting not found ");
            return false;
        }
    }

    private void notifyRecoverEventToUser(AppEyeMessage message) {
        int pid = message.getAppPid();
        int uid = message.getAppUid();
        String processName = message.getAppProcessName();
        String packageName = message.getAppPkgName();
        if (processName != null && packageName != null) {
            Slog.d(TAG, "notifyRecoverEventToUser -- pid:" + pid + " uid:" + uid + " processName:" + processName + " packageName:" + packageName);
            int foregroundAppPid = getForegroundPid();
            if (pid != foregroundAppPid) {
                Slog.d(TAG, "notifyRecoverEventToUser return due to no match foreground app foregroundAppPid:" + foregroundAppPid + " mTargetAppPid:" + pid + " mTargetAppUid:" + uid);
                return;
            }
            this.mHandler.removeMessages(3);
            HwActivityManager.handleShowAppEyeAnrUi(pid, uid, processName, packageName);
            Message msg = this.mHandler.obtainMessage();
            msg.what = 3;
            msg.obj = message;
            this.mHandler.sendMessageDelayed(msg, 10000);
        }
    }

    private int getForegroundPid() {
        Bundle topBundle = ActivityManagerEx.getTopActivity();
        if (topBundle != null) {
            return topBundle.getInt(SceneRecogFeature.DATA_PID);
        }
        Slog.d(TAG, "can not get current top activity!");
        return 0;
    }

    /* access modifiers changed from: private */
    public boolean handleUserEvent(ZrHungData args, boolean isTimeout) {
        if (args == null) {
            Slog.e(TAG, "null args received!");
            return false;
        } else if (handleDirectRecoverPackages(args.getString("packageName"), args.getInt(SceneRecogFeature.DATA_PID))) {
            return true;
        } else {
            if (isTimeout) {
                args.putString("result", "Timeout");
            } else if (!this.mHandler.hasMessages(3) || args.getString("result") == null) {
                return false;
            } else {
                this.mHandler.removeMessages(3);
            }
            if (!sendAppFreezeEvent(278, args, null, "user handled recover")) {
                Slog.e(TAG, "send App Freeze recover result error!");
            }
            return true;
        }
    }

    private boolean sendAppFreezeEvent(short wpId, ZrHungData args, String cmdBuf, String buffer) {
        StringBuilder sb = new StringBuilder();
        if (args != null) {
            try {
                String recoverresult = args.getString("result");
                if (recoverresult != null) {
                    sb.append("result = ");
                    sb.append(recoverresult);
                    sb.append('\n');
                }
                int uid = args.getInt("uid");
                if (uid > 0) {
                    sb.append("uid = ");
                    sb.append(Integer.toString(uid));
                    sb.append('\n');
                }
                int pid = args.getInt(SceneRecogFeature.DATA_PID);
                if (pid > 0) {
                    sb.append("pid = ");
                    sb.append(Integer.toString(pid));
                    sb.append('\n');
                }
                String pkgName = args.getString("packageName");
                if (pkgName != null) {
                    sb.append("packageName = ");
                    sb.append(pkgName);
                    sb.append('\n');
                }
                String procName = args.getString(MemoryConstant.MEM_NATIVE_ITEM_PROCESSNAME);
                if (procName != null) {
                    sb.append("processName = ");
                    sb.append(procName);
                    sb.append('\n');
                }
            } catch (Exception e) {
                Slog.e(TAG, "exception info ex");
                return false;
            }
        }
        if (buffer != null) {
            sb.append(buffer);
        }
        boolean result = ZRHung.sendHungEvent(wpId, cmdBuf, sb.toString());
        Slog.d(TAG, " sendAppFreezeEvent message:" + sb.toString());
        if (result) {
            return true;
        }
        Slog.e(TAG, " sendAppFreezeEvent failed!");
        return true;
    }

    static final class AppEyeANRData {
        public static final int ANR_DIALOG_REMOVED = 1;
        public static final int ANR_DIALOG_SHOWING = 0;
        /* access modifiers changed from: private */
        public int mANRDialogStatus = 1;
        /* access modifiers changed from: private */
        public long mANRTime = 0;
        /* access modifiers changed from: private */
        public String mANRType = null;

        AppEyeANRData(String aNRType, long currentTime) {
            this.mANRType = aNRType;
            this.mANRTime = currentTime;
        }
    }

    private boolean canShowANRDialogs(ZrHungData args) {
        boolean flag = true;
        synchronized (this.mANRMap) {
            if (args != null) {
                String packageName = args.getString("packageName");
                String currentANRType = args.getString("result");
                int pid = args.getInt(SceneRecogFeature.DATA_PID);
                if (handleDirectRecoverPackages(packageName, pid)) {
                    return false;
                }
                if (!(packageName == null || currentANRType == null)) {
                    if (currentANRType.equals("original")) {
                        if (!canShowOriginalANRDialogsLocked(packageName)) {
                            Slog.d(TAG, " forbid ORIGNAL ANR Dialogs");
                            flag = false;
                        } else {
                            Slog.d(TAG, " show ORIGNAL ANR Dialogs");
                        }
                    } else if (!canShowAppEyeANRDialogsLocked(packageName)) {
                        Slog.d(TAG, " forbid APPEYE ANR Dialogs");
                        flag = false;
                    } else {
                        Slog.d(TAG, " show APPEYE ANR Dialogs");
                    }
                    if (flag && (flag = isTargetAnrProcessStillAlive(pid))) {
                        resetAppEyeANRDataLocked(packageName, currentANRType, 0);
                    }
                }
            }
            return flag;
        }
    }

    private boolean canShowOriginalANRDialogsLocked(String packageName) {
        AppEyeANRData lastANRData;
        long currentTime = SystemClock.uptimeMillis();
        if (packageName == null || (lastANRData = this.mANRMap.get(packageName)) == null || !lastANRData.mANRType.equals("appeye")) {
            return true;
        }
        return updateShowAnrDialogFlagIfNeeded(lastANRData, currentTime, "OriginalANRDialog");
    }

    private boolean canShowAppEyeANRDialogsLocked(String packageName) {
        AppEyeANRData lastANRData;
        long currentTime = SystemClock.uptimeMillis();
        if (packageName == null || (lastANRData = this.mANRMap.get(packageName)) == null) {
            return true;
        }
        return updateShowAnrDialogFlagIfNeeded(lastANRData, currentTime, "AppEyeANRDialog");
    }

    private boolean updateShowAnrDialogFlagIfNeeded(AppEyeANRData lastANRData, long curTime, String dialogType) {
        boolean isShowDialog = true;
        String unused = lastANRData.mANRType;
        int dialogStatus = lastANRData.mANRDialogStatus;
        long lastAnrTime = lastANRData.mANRTime;
        StringBuilder sb = new StringBuilder(" forbid Show ");
        sb.append(dialogType);
        if (dialogStatus == 1 && curTime - lastAnrTime < 6000) {
            sb.append(", last anr dialog removed less than 6s");
            isShowDialog = false;
        }
        if (dialogStatus == 0 && curTime - lastAnrTime < 10000) {
            sb.append(", exist showing or pending dialog");
            isShowDialog = false;
        }
        if (!isShowDialog) {
            Slog.d(TAG, sb.toString());
        }
        return isShowDialog;
    }

    private void resetAppEyeANRDataLocked(String packageName, String aNRType, int dialogStatus) {
        long currentTime = SystemClock.uptimeMillis();
        AppEyeANRData lastANRData = this.mANRMap.get(packageName);
        if (lastANRData != null) {
            long unused = lastANRData.mANRTime = currentTime;
            String unused2 = lastANRData.mANRType = aNRType;
            int unused3 = lastANRData.mANRDialogStatus = dialogStatus;
            return;
        }
        this.mANRMap.put(packageName, new AppEyeANRData(aNRType, currentTime));
    }

    private void setAppEyeANRDataLocked(ZrHungData args) {
        AppEyeANRData lastANRData;
        synchronized (this.mANRMap) {
            String packageName = args.getString("packageName");
            if (!(packageName == null || (lastANRData = this.mANRMap.get(packageName)) == null)) {
                long unused = lastANRData.mANRTime = SystemClock.uptimeMillis();
                int unused2 = lastANRData.mANRDialogStatus = 1;
            }
        }
    }

    private void killApplicationProcess(int pid, String pkgName) {
        if (pid <= 0) {
            return;
        }
        if (AppEyeBinderBlock.isNativeProcess(pid) != 0) {
            Slog.e(TAG, " process is native process or not exist, pid:" + pid);
            return;
        }
        try {
            if (this.mActivityManager != null) {
                this.mActivityManager.forceStopPackageAsUser(pkgName, -2);
                Slog.e(TAG, "BF and NFW forceStop package: " + pkgName);
                return;
            }
            Slog.e(TAG, "forcestopApps preocess: mActivityManager is null");
        } catch (Exception e) {
            Slog.e(TAG, "kill process error");
        }
    }

    private boolean handleDirectRecoverPackages(String packageName, int pid) {
        String[] strArr;
        if (packageName == null) {
            return false;
        }
        if (!this.mIsDirectRecoveryConfiged) {
            initRecoverPackageList();
        }
        if (!this.mIsDirectRecoveryEnabled || (strArr = this.mDirectRecoverPackageList) == null) {
            return false;
        }
        int length = strArr.length;
        int i = 0;
        while (i < length) {
            if (!packageName.equals(strArr[i]) || pid <= 0) {
                i++;
            } else {
                Slog.e(TAG, "kill process pid:" + pid + " packageName:" + packageName + " reason:directRecover");
                Process.killProcess(pid);
                ZrHungData data = new ZrHungData();
                data.putString("packageName", packageName);
                data.putString("result", "Direct Kill");
                sendAppFreezeEvent(278, data, null, "direct recover");
                return true;
            }
        }
        return false;
    }

    private void initRecoverPackageList() {
        ZRHung.HungConfig cfg = ZRHung.getHungConfig(278);
        if (cfg == null || cfg.status != 0) {
            Slog.e(TAG, "initRecoverPackageList failed!");
            return;
        }
        this.mDirectRecoverPackageList = cfg.value.split(",");
        String[] strArr = this.mDirectRecoverPackageList;
        if (strArr == null) {
            Slog.e(TAG, "initRecoverPackageList failed, due to null list!");
            this.mIsDirectRecoveryConfiged = true;
        } else if (strArr.length < 2) {
            Slog.e(TAG, "initRecoverPackageList failed, due to err config!");
            this.mIsDirectRecoveryConfiged = true;
        } else {
            this.mIsDirectRecoveryConfiged = true;
            this.mIsDirectRecoveryEnabled = strArr[0].trim().equals("1");
        }
    }

    private boolean isTargetAnrProcessStillAlive(int pid) {
        Slog.d("AppEyeUiProbe", "isTargetAlive:" + pid);
        return Files.exists(Paths.get("/proc/" + pid + "/comm", new String[0]), new LinkOption[0]);
    }
}
