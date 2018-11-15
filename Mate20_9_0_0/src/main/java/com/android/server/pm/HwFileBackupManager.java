package com.android.server.pm;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageParser.Package;
import android.os.Binder;
import android.os.IBackupSessionCallback;
import android.os.IBackupSessionCallback.Stub;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManagerInternal;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import com.android.server.LocalServices;
import com.android.server.pm.Installer.InstallerException;
import java.util.ArrayList;
import java.util.List;

public final class HwFileBackupManager {
    public static final int BACKUP_TASK_CMD_ARG_MAX = 6;
    public static final int BACKUP_TASK_FAILED = -1;
    public static final int BACKUP_TASK_NO_PERMISSION = -2;
    public static final int BACKUP_TASK_SUCCESS = 0;
    public static final int BACKUP_TASK_UNSUPPORTED_CMD = -3;
    private static final String[] PACKAGE_NAMES_FILE_BACKUP = new String[]{"com.hicloud.android.clone", "com.huawei.intelligent", "com.huawei.KoBackup", "com.huawei.hidisk"};
    private static final String TAG = "HwFileBackupManager_BackupSession";
    private static final int VERSION_CODE = 1;
    private static volatile HwFileBackupManager mInstance;
    private final ArrayList<BackupDeathHandler> mBackupDeathHandlers = new ArrayList();
    private final Installer mInstaller;
    private NativeBackupCallback mNativeBackupCallback = new NativeBackupCallback();
    private final SparseArray<IBackupSessionCallback> mSessions = new SparseArray();
    private UserManagerInternal mUserManagerInternal;

    private class BackupDeathHandler implements DeathRecipient {
        public IBinder mCb;
        public int mSessionId;

        BackupDeathHandler(int sessionId, IBinder cb) {
            this.mSessionId = sessionId;
            this.mCb = cb;
        }

        public void binderDied() {
            String str = HwFileBackupManager.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("backup client with sessionId ");
            stringBuilder.append(this.mSessionId);
            stringBuilder.append(" died");
            Log.w(str, stringBuilder.toString());
            long ident = Binder.clearCallingIdentity();
            HwFileBackupManager.this.finishBackupSession(this.mSessionId);
            Binder.restoreCallingIdentity(ident);
        }

        public IBinder getBinder() {
            return this.mCb;
        }

        public int getSessionId() {
            return this.mSessionId;
        }
    }

    private final class NativeBackupCallback extends Stub {
        private NativeBackupCallback() {
        }

        public void onTaskStatusChanged(int sessionId, int taskId, int statusCode, String appendData) {
            HwFileBackupManager.this.handleNativeBackupSessionCallback(sessionId, taskId, statusCode, appendData);
        }
    }

    private static class PathData {
        private static final String DATA_DATA_PATH = "/data/data/";
        private static final String MULTI_USER_PATH = "/data/user/";
        public boolean isAppDataPath = true;
        public boolean isMultiUserPath;
        public String packageName;
        public String path;
        public int userId = -10000;

        private PathData() {
        }

        public static PathData create(String path) {
            if (TextUtils.isEmpty(path)) {
                return null;
            }
            PathData instance = new PathData();
            instance.path = path;
            if (path.startsWith(MULTI_USER_PATH)) {
                instance.isMultiUserPath = true;
            } else if (path.startsWith(DATA_DATA_PATH)) {
                instance.isMultiUserPath = false;
            } else {
                instance.isAppDataPath = false;
            }
            instance.parsePath();
            return instance;
        }

        private void parsePath() {
            if (this.isAppDataPath) {
                if (this.isMultiUserPath) {
                    parseMultiUserPath();
                } else {
                    parseDefaultDataPath();
                }
                return;
            }
            String str = HwFileBackupManager.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(this.path);
            stringBuilder.append(" is not a app data path,no need parse!");
            Slog.d(str, stringBuilder.toString());
        }

        private void parseMultiUserPath() {
            int startIndex = MULTI_USER_PATH.length();
            if (startIndex >= this.path.length()) {
                String str = HwFileBackupManager.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(this.path);
                stringBuilder.append(" does not contain userId!");
                Slog.e(str, stringBuilder.toString());
                return;
            }
            int userIdLocation = this.path.indexOf("/", startIndex);
            String str2;
            if (-1 == userIdLocation) {
                str2 = HwFileBackupManager.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append(this.path);
                stringBuilder2.append(" does not contain userId or package name!");
                Slog.e(str2, stringBuilder2.toString());
                return;
            }
            str2 = this.path.substring(startIndex, userIdLocation);
            if (!TextUtils.isEmpty(str2)) {
                try {
                    this.userId = Integer.parseInt(str2);
                } catch (NumberFormatException e) {
                    String str3 = HwFileBackupManager.TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append(this.path);
                    stringBuilder3.append(" does not contain correct userId, find:");
                    stringBuilder3.append(str2);
                    Slog.e(str3, stringBuilder3.toString());
                    return;
                }
            }
            this.packageName = getPackageName(this.path, userIdLocation + 1);
        }

        private void parseDefaultDataPath() {
            this.packageName = getPackageName(this.path, DATA_DATA_PATH.length());
        }

        private String getPackageName(String path, int startIndex) {
            String str;
            if (TextUtils.isEmpty(path) || startIndex >= path.length()) {
                str = HwFileBackupManager.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(path);
                stringBuilder.append(" does not contain package name!");
                Slog.e(str, stringBuilder.toString());
                return null;
            }
            str = "";
            int endIndex = path.indexOf("/", startIndex);
            if (endIndex == -1) {
                str = path.substring(startIndex).trim();
            } else {
                str = path.substring(startIndex, endIndex).trim();
            }
            String str2 = HwFileBackupManager.TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(str);
            stringBuilder2.append(" found in ");
            stringBuilder2.append(path);
            Slog.d(str2, stringBuilder2.toString());
            return str;
        }
    }

    private HwFileBackupManager(Installer installer) {
        this.mInstaller = installer;
    }

    public static HwFileBackupManager getInstance(Installer installer) {
        if (mInstance == null) {
            synchronized (HwFileBackupManager.class) {
                if (mInstance == null) {
                    mInstance = new HwFileBackupManager(installer);
                }
            }
        }
        return mInstance;
    }

    private void handleNativeBackupSessionCallback(int sessionId, int taskId, int statusCode, String appendData) {
        synchronized (this.mSessions) {
            IBackupSessionCallback callback = (IBackupSessionCallback) this.mSessions.get(sessionId);
            if (callback == null) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("no callback set for session:");
                stringBuilder.append(sessionId);
                Log.e(str, stringBuilder.toString());
                return;
            }
            try {
                callback.onTaskStatusChanged(sessionId, taskId, statusCode, appendData);
            } catch (RemoteException e) {
                Log.w(TAG, "callback binder death!");
            }
        }
    }

    public int startBackupSession(IBackupSessionCallback callback) {
        Slog.i(TAG, "application bind call startBackupSession");
        try {
            if (this.mInstaller == null) {
                Slog.e(TAG, "installer is null!");
                return -1;
            }
            int sessionId = this.mInstaller.startBackupSession(this.mNativeBackupCallback);
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("application startBackupSession sessionid:");
            stringBuilder.append(sessionId);
            Slog.i(str, stringBuilder.toString());
            if (sessionId < 0) {
                return -1;
            }
            synchronized (this.mBackupDeathHandlers) {
                BackupDeathHandler hdlr = new BackupDeathHandler(sessionId, callback.asBinder());
                try {
                    callback.asBinder().linkToDeath(hdlr, 0);
                    this.mBackupDeathHandlers.add(hdlr);
                } catch (RemoteException e) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("startBackupSession() could not link to ");
                    stringBuilder2.append(callback.asBinder());
                    stringBuilder2.append(" binder death");
                    Log.w(str2, stringBuilder2.toString());
                    return -1;
                }
            }
            synchronized (this.mSessions) {
                this.mSessions.put(sessionId, callback);
            }
            return sessionId;
        } catch (InstallerException e2) {
            Slog.w(TAG, "Trouble startBackupSession", e2);
            return -1;
        }
    }

    /* JADX WARNING: Missing block: B:17:0x004b, code:
            if (android.text.TextUtils.isEmpty(r7) == false) goto L_0x004f;
     */
    /* JADX WARNING: Missing block: B:19:0x004e, code:
            return -3;
     */
    /* JADX WARNING: Missing block: B:21:0x0051, code:
            if (r5.mInstaller != null) goto L_0x005b;
     */
    /* JADX WARNING: Missing block: B:22:0x0053, code:
            android.util.Slog.e(TAG, "installer is null!");
     */
    /* JADX WARNING: Missing block: B:23:0x005a, code:
            return -1;
     */
    /* JADX WARNING: Missing block: B:25:0x0061, code:
            return r5.mInstaller.executeBackupTask(r6, r7);
     */
    /* JADX WARNING: Missing block: B:26:0x0062, code:
            r1 = move-exception;
     */
    /* JADX WARNING: Missing block: B:27:0x0063, code:
            android.util.Slog.w(TAG, "Trouble executeBackupTask", r1);
     */
    /* JADX WARNING: Missing block: B:28:0x006a, code:
            return -1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int executeBackupTask(int sessionId, String taskCmd) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("bind call executeBackupTask on session:");
        stringBuilder.append(sessionId);
        Slog.i(str, stringBuilder.toString());
        if (sessionId == -1 && "getVersionCode".equalsIgnoreCase(taskCmd)) {
            return 1;
        }
        synchronized (this.mSessions) {
            if (this.mSessions.indexOfKey(sessionId) < 0) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("no session with id=");
                stringBuilder2.append(sessionId);
                Slog.e(str2, stringBuilder2.toString());
                return -1;
            }
        }
    }

    public int finishBackupSession(int sessionId) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("bind call finishBackupSession sessionId:");
        stringBuilder.append(sessionId);
        Slog.i(str, stringBuilder.toString());
        int result = -1;
        try {
            if (this.mInstaller == null) {
                Slog.e(TAG, "installer is null!");
                return -1;
            }
            result = this.mInstaller.finishBackupSession(sessionId);
            synchronized (this.mSessions) {
                this.mSessions.remove(sessionId);
            }
            synchronized (this.mBackupDeathHandlers) {
                for (int i = this.mBackupDeathHandlers.size() - 1; i >= 0; i--) {
                    BackupDeathHandler hdlr = (BackupDeathHandler) this.mBackupDeathHandlers.get(i);
                    if (hdlr.getSessionId() == sessionId) {
                        this.mBackupDeathHandlers.remove(i);
                        try {
                            hdlr.getBinder().unlinkToDeath(hdlr, 0);
                        } catch (Exception e) {
                        }
                    }
                }
            }
            return result;
        } catch (InstallerException e2) {
            Slog.w(TAG, "Trouble finishBackupSession", e2);
        }
    }

    public boolean checkBackupPackageName(String pkgName) {
        boolean result = false;
        for (CharSequence equals : PACKAGE_NAMES_FILE_BACKUP) {
            if (TextUtils.equals(pkgName, equals)) {
                result = true;
                break;
            }
        }
        if (!result) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("BackupSession checkBackupPackageName failed, pkgName is ");
            stringBuilder.append(pkgName);
            Slog.d(str, stringBuilder.toString());
        }
        return result;
    }

    private String normalizeTaskCmd(String taskCmd, List<String> cmdInfo) {
        String arg;
        String[] args = taskCmd.split(" ");
        StringBuilder sbTaskCmd = new StringBuilder();
        for (String arg2 : args) {
            arg2 = arg2.replace(" ", "");
            if (!TextUtils.isEmpty(arg2)) {
                if (cmdInfo != null) {
                    cmdInfo.add(arg2);
                }
                sbTaskCmd.append(arg2);
                sbTaskCmd.append(" ");
            }
        }
        String normalizedTaskCmd = sbTaskCmd.toString().trim();
        arg2 = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("BackupSession prepareBackupTaskCmd,after normalize is:");
        stringBuilder.append(normalizedTaskCmd);
        Slog.d(arg2, stringBuilder.toString());
        return normalizedTaskCmd;
    }

    public String prepareBackupTaskCmd(String taskCmd, ArrayMap<String, Package> packages) {
        if (TextUtils.isEmpty(taskCmd)) {
            return null;
        }
        List<String> cmdInfo = new ArrayList(6);
        String normalizedTaskCmd = normalizeTaskCmd(taskCmd, cmdInfo);
        if (cmdInfo.size() > 6) {
            return null;
        }
        if (cmdInfo.size() < 4) {
            return normalizedTaskCmd;
        }
        String srcPath = (String) cmdInfo.get(2);
        String destPath;
        if (isUsableSrcPath(PathData.create(srcPath))) {
            destPath = (String) cmdInfo.get(3);
            PathData destPathData = PathData.create(destPath);
            StringBuilder sb;
            if (isUsableDestPath(destPathData)) {
                synchronized (packages) {
                    Package pkg = (Package) packages.get(destPathData.packageName);
                    if (pkg == null || pkg.applicationInfo == null) {
                        Slog.d(TAG, "BackupSession prepareBackupTaskCmd, target path must begin with a existing app's data directory since we need get seinfo for task cmd!");
                        return null;
                    }
                    ApplicationInfo app = pkg.applicationInfo;
                    sb = new StringBuilder(normalizedTaskCmd);
                    sb.append(" ");
                    sb.append(app.seInfo);
                    sb.append(" ");
                    if (destPathData.isMultiUserPath) {
                        sb.append(UserHandle.getUid(destPathData.userId, app.uid));
                    } else {
                        sb.append(app.uid);
                    }
                    String stringBuilder = sb.toString();
                    return stringBuilder;
                }
            }
            String str = TAG;
            sb = new StringBuilder();
            sb.append(destPath);
            sb.append(" is not a supported dest path!");
            Slog.e(str, sb.toString());
            return null;
        }
        destPath = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(srcPath);
        stringBuilder2.append(" is not a supported src data path!");
        Slog.e(destPath, stringBuilder2.toString());
        return null;
    }

    private UserManagerInternal getUserManagerInternal() {
        if (this.mUserManagerInternal == null) {
            this.mUserManagerInternal = (UserManagerInternal) LocalServices.getService(UserManagerInternal.class);
        }
        return this.mUserManagerInternal;
    }

    private boolean isUsableSrcPath(PathData pathData) {
        if (pathData == null) {
            return false;
        }
        if (pathData.isAppDataPath) {
            return isUsableAppDataPath(pathData);
        }
        return true;
    }

    private boolean isUsableDestPath(PathData pathData) {
        if (pathData != null && !TextUtils.isEmpty(pathData.packageName)) {
            return isUsableAppDataPath(pathData);
        }
        Slog.e(TAG, "dest path does not contain package name, check package name is null!");
        return false;
    }

    private boolean isUsableAppDataPath(PathData pathData) {
        if (pathData == null || !pathData.isAppDataPath) {
            return false;
        }
        if (!pathData.isMultiUserPath || pathData.userId == 0) {
            return true;
        }
        if (HwPackageManagerService.isSupportCloneAppInCust(pathData.packageName) && getUserManagerInternal().isClonedProfile(pathData.userId)) {
            return true;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(pathData.path);
        stringBuilder.append(" is not a support clone app data path!");
        Slog.e(str, stringBuilder.toString());
        return false;
    }
}
