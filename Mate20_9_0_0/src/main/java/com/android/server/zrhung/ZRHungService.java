package com.android.server.zrhung;

import android.app.ActivityManagerNative;
import android.app.IProcessObserver;
import android.app.IProcessObserver.Stub;
import android.content.Context;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Slog;
import android.util.ZRHung;
import android.util.ZRHung.HungConfig;
import android.zrhung.ZrHungData;
import android.zrhung.appeye.AppEyeBinderBlock;
import com.android.server.SystemService;
import com.android.server.rms.iaware.memory.utils.MemoryConstant;
import com.android.server.zrhung.appeye.AppEyeMessage;
import com.android.server.zrhung.appeye.AppEyeSocketThread;
import com.huawei.android.app.HwActivityManager;
import java.util.HashMap;
import java.util.Iterator;

public final class ZRHungService extends SystemService implements IZRHungService {
    private static final int MSG_ZRHUNG_INIT = 1;
    private static final int MSG_ZRHUNG_SOCKET_EVENT_RECEIVE = 2;
    private static final int MSG_ZRHUNG_USER_EVENT_DELAY = 3;
    private static final String TAG = "ZRHungService";
    private static final int ZRHUNG_ANR_INTERVAL = 6000;
    private static final int ZRHUNG_USER_EVENT_TIMEOUT = 10000;
    private static IZRHungService mService = null;
    private final HashMap<String, AppEyeANRData> mANRMap = new HashMap();
    private final Context mContext;
    private String[] mDirectRecoverPackageList = null;
    private volatile int mForegroundAppPid;
    private volatile int mForegroundAppUid;
    private final Handler mHandler;
    private Callback mHandlerCallback = new Callback() {
        public boolean handleMessage(Message msg) {
            String str = ZRHungService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("handleMessage :");
            stringBuilder.append(msg.what);
            Slog.d(str, stringBuilder.toString());
            switch (msg.what) {
                case 1:
                    ZRHungService.this.handleInitService();
                    return true;
                case 2:
                    ZRHungService.this.handleAppFreezeEvent((AppEyeMessage) msg.obj);
                    return true;
                case 3:
                    str = ((AppEyeMessage) msg.obj).getAppPkgName();
                    ZrHungData data = new ZrHungData();
                    data.putString("packageName", str);
                    ZRHungService.this.handleUserEvent(data, true);
                    return true;
                default:
                    return false;
            }
        }
    };
    private boolean mIsDirectRecoveryConfiged = false;
    private boolean mIsDirectRecoveryEnabled = false;
    private IProcessObserver mProcessObserver = new Stub() {
        public void onForegroundActivitiesChanged(int pid, int uid, boolean foregroundActivities) {
            if (foregroundActivities) {
                ZRHungService.this.mForegroundAppPid = pid;
                ZRHungService.this.mForegroundAppUid = uid;
            }
        }

        public void onProcessDied(int pid, int uid) {
        }
    };
    private AppEyeSocketThread mSocketThread = null;

    static final class AppEyeANRData {
        public static final int ANR_DIALOG_REMOVED = 1;
        public static final int ANR_DIALOG_SHOWING = 0;
        private int mANRDialogStatus = 1;
        private long mANRTime = 0;
        private String mANRType = null;

        AppEyeANRData(String aNRType, long currentTime) {
            this.mANRType = aNRType;
            this.mANRTime = currentTime;
        }
    }

    public ZRHungService(Context context) {
        super(context);
        setInstance(this);
        this.mContext = context;
        HandlerThread handlerThread = new HandlerThread(TAG);
        handlerThread.start();
        this.mHandler = new Handler(handlerThread.getLooper(), this.mHandlerCallback);
        Slog.d(TAG, "ZRHungService on create!");
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
        Slog.d(TAG, "onBootPhase!");
        if (phase == 1000 && this.mHandler != null) {
            this.mHandler.sendEmptyMessage(1);
        }
    }

    private void registerProcessObserver() {
        try {
            ActivityManagerNative.getDefault().registerProcessObserver(this.mProcessObserver);
        } catch (RemoteException e) {
            Slog.e(TAG, "ZRHungService register ProcessObserver fail");
        }
    }

    public boolean sendEvent(ZrHungData args) {
        if (args == null) {
            return false;
        }
        String eventType = args.getString("eventtype");
        int i = -1;
        int hashCode = eventType.hashCode();
        if (hashCode != -74099055) {
            if (hashCode != 1777228417) {
                if (hashCode != 1914956048) {
                    if (hashCode == 1985941039 && eventType.equals("settime")) {
                        i = 2;
                    }
                } else if (eventType.equals("showanrdialog")) {
                    i = 3;
                }
            } else if (eventType.equals("recoverresult")) {
                i = true;
            }
        } else if (eventType.equals("socketrecover")) {
            i = false;
        }
        switch (i) {
            case 0:
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
            case 1:
                if (handleUserEvent(args, false)) {
                    return true;
                }
                Slog.w(TAG, "sendEvent  recoverresult error!");
                return false;
            case 2:
                setAppEyeANRDataLocked(args);
                return true;
            case 3:
                if (canShowANRDialogs(args)) {
                    return true;
                }
                return false;
            default:
                return false;
        }
    }

    private void handleInitService() {
        if (this.mSocketThread == null) {
            this.mSocketThread = new AppEyeSocketThread();
            this.mSocketThread.start();
            Slog.d(TAG, "handleInitService!");
            registerProcessObserver();
            return;
        }
        Slog.d(TAG, "socketThread already start!");
    }

    private void handleAppFreezeEvent(AppEyeMessage message) {
        if (message == null) {
            Slog.e(TAG, "null message or mPath received from Server");
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("received message:");
        stringBuilder.append(message.toString());
        Slog.d(str, stringBuilder.toString());
        str = message.getCommand();
        Object obj = -1;
        int hashCode = str.hashCode();
        if (hashCode != -1547232625) {
            if (hashCode != -1164891468) {
                if (hashCode == 1702423020 && str.equals(AppEyeMessage.KILL_MUlTI_PROCESSES)) {
                    obj = 2;
                }
            } else if (str.equals(AppEyeMessage.NOTIFY_USER)) {
                obj = null;
            }
        } else if (str.equals(AppEyeMessage.KILL_PROCESS)) {
            obj = 1;
        }
        switch (obj) {
            case null:
                notifyRecoverEventToUser(message);
                break;
            case 1:
                killApplicationProcess(message.getAppPid());
                break;
            case 2:
                Iterator it = message.getAppPidList().iterator();
                while (it.hasNext()) {
                    killApplicationProcess(((Integer) it.next()).intValue());
                }
                break;
            default:
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("invalid command: ");
                stringBuilder2.append(str);
                Slog.e(str2, stringBuilder2.toString());
                break;
        }
    }

    private void notifyRecoverEventToUser(AppEyeMessage message) {
        int pid = message.getAppPid();
        int uid = message.getAppUid();
        String processName = message.getAppProcessName();
        String packageName = message.getAppPkgName();
        if (!(processName == null || packageName == null)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("notifyRecoverEventToUser -- pid:");
            stringBuilder.append(pid);
            stringBuilder.append(" uid:");
            stringBuilder.append(uid);
            stringBuilder.append(" processName:");
            stringBuilder.append(processName);
            stringBuilder.append(" packageName:");
            stringBuilder.append(packageName);
            Slog.d(str, stringBuilder.toString());
            if (pid != this.mForegroundAppPid) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("notifyRecoverEventToUser return due to no match foreground app mForegroundAppPid:");
                stringBuilder.append(this.mForegroundAppPid);
                stringBuilder.append(" mForegroundAppUid:");
                stringBuilder.append(this.mForegroundAppUid);
                stringBuilder.append(" mTargetAppPid:");
                stringBuilder.append(pid);
                stringBuilder.append(" mTargetAppUid:");
                stringBuilder.append(uid);
                Slog.d(str, stringBuilder.toString());
                return;
            }
            this.mHandler.removeMessages(3);
            HwActivityManager.handleShowAppEyeAnrUi(pid, uid, processName, packageName);
            Message msg = this.mHandler.obtainMessage();
            msg.what = 3;
            msg.obj = message;
            this.mHandler.sendMessageDelayed(msg, MemoryConstant.MIN_INTERVAL_OP_TIMEOUT);
        }
    }

    private boolean handleUserEvent(ZrHungData args, boolean isTimeout) {
        if (args == null) {
            Slog.e(TAG, "null args received!");
            return false;
        } else if (handleDirectRecoverPackages(args.getString("packageName"), args.getInt("pid"))) {
            return true;
        } else {
            if (isTimeout) {
                args.putString("result", "Timeout");
            } else if (!this.mHandler.hasMessages(3) || args.getString("result") == null) {
                return false;
            } else {
                this.mHandler.removeMessages(3);
            }
            if (!sendAppFreezeEvent((short) 278, args, null, "user handled recover")) {
                Slog.e(TAG, "send App Freeze recover result error!");
            }
            return true;
        }
    }

    private boolean sendAppFreezeEvent(short wpId, ZrHungData args, String cmdBuf, String buffer) {
        String str;
        StringBuilder stringBuilder;
        StringBuilder sb = new StringBuilder();
        if (args != null) {
            try {
                String recoverresult = args.getString("result");
                if (recoverresult != null) {
                    sb.append("result = ");
                    sb.append(recoverresult);
                    sb.append(10);
                }
                int uid = args.getInt("uid");
                if (uid > 0) {
                    sb.append("uid = ");
                    sb.append(Integer.toString(uid));
                    sb.append(10);
                }
                int pid = args.getInt("pid");
                if (pid > 0) {
                    sb.append("pid = ");
                    sb.append(Integer.toString(pid));
                    sb.append(10);
                }
                String pkgName = args.getString("packageName");
                if (pkgName != null) {
                    sb.append("packageName = ");
                    sb.append(pkgName);
                    sb.append(10);
                }
                String procName = args.getString("processName");
                if (procName != null) {
                    sb.append("processName = ");
                    sb.append(procName);
                    sb.append(10);
                }
            } catch (Exception ex) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("exception info ex:");
                stringBuilder.append(ex);
                Slog.e(str, stringBuilder.toString());
                return false;
            }
        }
        if (buffer != null) {
            sb.append(buffer);
        }
        Exception ex2 = ZRHung.sendHungEvent(wpId, cmdBuf, sb.toString());
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append(" sendAppFreezeEvent message:");
        stringBuilder.append(sb.toString());
        Slog.d(str, stringBuilder.toString());
        if (ex2 == null) {
            Slog.e(TAG, " sendAppFreezeEvent failed!");
        }
        return true;
    }

    /* JADX WARNING: Missing block: B:26:0x006a, code:
            return r0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean canShowANRDialogs(ZrHungData args) {
        boolean flag = true;
        synchronized (this.mANRMap) {
            if (args != null) {
                String packageName = args.getString("packageName");
                String currentANRType = args.getString("result");
                int pid = args.getInt("pid");
                if (handleDirectRecoverPackages(packageName, pid)) {
                    return false;
                } else if (!(packageName == null || currentANRType == null)) {
                    if (currentANRType.equals("original")) {
                        if (canShowOriginalANRDialogsLocked(packageName)) {
                            Slog.d(TAG, " show ORIGNAL ANR Dialogs");
                        } else {
                            Slog.d(TAG, " forbid ORIGNAL ANR Dialogs");
                            flag = false;
                        }
                    } else if (canShowAppEyeANRDialogsLocked(packageName)) {
                        Slog.d(TAG, " show APPEYE ANR Dialogs");
                    } else {
                        Slog.d(TAG, " forbid APPEYE ANR Dialogs");
                        flag = false;
                    }
                    if (flag) {
                        flag = isTargetAnrProcessStillAlive(pid);
                        if (flag) {
                            resetAppEyeANRDataLocked(packageName, currentANRType, 0);
                        }
                    }
                }
            }
        }
    }

    private boolean canShowOriginalANRDialogsLocked(String packageName) {
        long currentTime = SystemClock.uptimeMillis();
        if (packageName == null) {
            return true;
        }
        AppEyeANRData lastANRData = (AppEyeANRData) this.mANRMap.get(packageName);
        if (lastANRData == null || !lastANRData.mANRType.equals("appeye")) {
            return true;
        }
        return updateShowAnrDialogFlagIfNeeded(lastANRData, currentTime, "OriginalANRDialog");
    }

    private boolean canShowAppEyeANRDialogsLocked(String packageName) {
        long currentTime = SystemClock.uptimeMillis();
        if (packageName == null) {
            return true;
        }
        AppEyeANRData lastANRData = (AppEyeANRData) this.mANRMap.get(packageName);
        if (lastANRData != null) {
            return updateShowAnrDialogFlagIfNeeded(lastANRData, currentTime, "AppEyeANRDialog");
        }
        return true;
    }

    private boolean updateShowAnrDialogFlagIfNeeded(AppEyeANRData lastANRData, long curTime, String dialogType) {
        boolean isShowDialog = true;
        String lastANRType = lastANRData.mANRType;
        int dialogStatus = lastANRData.mANRDialogStatus;
        long lastAnrTime = lastANRData.mANRTime;
        StringBuilder sb = new StringBuilder(" forbid Show ");
        sb.append(dialogType);
        if (dialogStatus == 1 && curTime - lastAnrTime < 6000) {
            sb.append(", last anr dialog removed less than 6s");
            isShowDialog = false;
        }
        if (dialogStatus == 0 && curTime - lastAnrTime < MemoryConstant.MIN_INTERVAL_OP_TIMEOUT) {
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
        AppEyeANRData lastANRData = (AppEyeANRData) this.mANRMap.get(packageName);
        if (lastANRData != null) {
            lastANRData.mANRTime = currentTime;
            lastANRData.mANRType = aNRType;
            lastANRData.mANRDialogStatus = dialogStatus;
            return;
        }
        this.mANRMap.put(packageName, new AppEyeANRData(aNRType, currentTime));
    }

    private void setAppEyeANRDataLocked(ZrHungData args) {
        synchronized (this.mANRMap) {
            String packageName = args.getString("packageName");
            if (packageName != null) {
                AppEyeANRData lastANRData = (AppEyeANRData) this.mANRMap.get(packageName);
                if (lastANRData != null) {
                    lastANRData.mANRTime = SystemClock.uptimeMillis();
                    lastANRData.mANRDialogStatus = 1;
                }
            }
        }
    }

    private void killApplicationProcess(int pid) {
        if (pid > 0) {
            String str;
            StringBuilder stringBuilder;
            if (AppEyeBinderBlock.isNativeProcess(pid) != 0) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append(" process is native process or not exist, pid:");
                stringBuilder.append(pid);
                Slog.e(str, stringBuilder.toString());
                return;
            }
            try {
                Process.killProcess(pid);
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("kill process pid:");
                stringBuilder.append(pid);
                Slog.e(str, stringBuilder.toString());
            } catch (Exception e) {
                Slog.e(TAG, "kill process error");
            }
        }
    }

    private boolean handleDirectRecoverPackages(String packageName, int pid) {
        if (packageName == null) {
            return false;
        }
        if (!this.mIsDirectRecoveryConfiged) {
            initRecoverPackageList();
        }
        if (!this.mIsDirectRecoveryEnabled || this.mDirectRecoverPackageList == null) {
            return false;
        }
        String[] strArr = this.mDirectRecoverPackageList;
        int length = strArr.length;
        int i = 0;
        while (i < length) {
            if (!packageName.equals(strArr[i]) || pid <= 0) {
                i++;
            } else {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("kill process pid:");
                stringBuilder.append(pid);
                stringBuilder.append(" packageName:");
                stringBuilder.append(packageName);
                stringBuilder.append(" reason:directRecover");
                Slog.e(str, stringBuilder.toString());
                Process.killProcess(pid);
                ZrHungData data = new ZrHungData();
                data.putString("packageName", packageName);
                data.putString("result", "Direct Kill");
                sendAppFreezeEvent((short) 278, data, null, "direct recover");
                return true;
            }
        }
        return false;
    }

    private void initRecoverPackageList() {
        HungConfig cfg = ZRHung.getHungConfig((short) 278);
        if (cfg == null || cfg.status != 0) {
            Slog.e(TAG, "initRecoverPackageList failed!");
            return;
        }
        this.mDirectRecoverPackageList = cfg.value.split(",");
        if (this.mDirectRecoverPackageList == null) {
            Slog.e(TAG, "initRecoverPackageList failed, due to null list!");
            this.mIsDirectRecoveryConfiged = true;
        } else if (this.mDirectRecoverPackageList.length < 2) {
            Slog.e(TAG, "initRecoverPackageList failed, due to err config!");
            this.mIsDirectRecoveryConfiged = true;
        } else {
            this.mIsDirectRecoveryConfiged = true;
            this.mIsDirectRecoveryEnabled = this.mDirectRecoverPackageList[0].trim().equals("1");
        }
    }

    private boolean isTargetAnrProcessStillAlive(int pid) {
        String procName = AppEyeBinderBlock.readProcName(Integer.toString(pid));
        if (procName != null && !"unknown".equals(procName)) {
            return true;
        }
        Slog.e(TAG, "target anr process has been removed, do not show dialog!");
        return false;
    }
}
