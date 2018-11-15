package com.android.server;

import android.app.ActivityManager;
import android.app.ActivityThread;
import android.app.AppGlobals;
import android.app.AppOpsManager;
import android.app.AppOpsManager.OpEntry;
import android.app.AppOpsManager.PackageOps;
import android.app.AppOpsManagerInternal;
import android.common.HwFrameworkFactory;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManagerInternal;
import android.content.pm.PackageManagerInternal.ExternalSourcesPolicy;
import android.content.pm.UserInfo;
import android.database.ContentObserver;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.Process;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.ShellCallback;
import android.os.ShellCommand;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.StorageManagerInternal;
import android.os.storage.StorageManagerInternal.ExternalStorageMountPolicy;
import android.provider.Settings.Global;
import android.rms.HwSysResource;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.AtomicFile;
import android.util.KeyValueListParser;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import android.util.TimeUtils;
import android.util.Xml;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.app.IAppOpsActiveCallback;
import com.android.internal.app.IAppOpsCallback;
import com.android.internal.app.IAppOpsService;
import com.android.internal.app.IAppOpsService.Stub;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.Preconditions;
import com.android.internal.util.XmlUtils;
import com.android.internal.util.function.pooled.PooledLambda;
import com.android.server.job.controllers.JobStatus;
import com.android.server.pm.PackageManagerService;
import com.android.server.power.IHwShutdownThread;
import com.android.server.slice.SliceClientPermissions.SliceAuthority;
import huawei.android.security.IHwBehaviorCollectManager.BehaviorId;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import libcore.util.EmptyArray;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class AppOpsService extends Stub {
    private static final int CURRENT_VERSION = 1;
    static final boolean DEBUG = false;
    private static final int NO_VERSION = -1;
    private static final int[] PROCESS_STATE_TO_UID_STATE = new int[]{0, 0, 1, 2, 3, 3, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 5, 5, 5};
    static final String TAG = "AppOps";
    private static final int UID_ANY = -2;
    static final String[] UID_STATE_NAMES = new String[]{"pers ", "top  ", "fgsvc", "fg   ", "bg   ", "cch  "};
    static final String[] UID_STATE_REJECT_ATTRS = new String[]{"rp", "rt", "rfs", "rf", "rb", "rc"};
    static final String[] UID_STATE_TIME_ATTRS = new String[]{"tp", "tt", "tfs", "tf", "tb", "tc"};
    static final long WRITE_DELAY = 1800000;
    final ArrayMap<IBinder, SparseArray<ActiveCallback>> mActiveWatchers = new ArrayMap();
    private final AppOpsManagerInternalImpl mAppOpsManagerInternal = new AppOpsManagerInternalImpl(this, null);
    private HwSysResource mAppOpsResource;
    final SparseArray<SparseArray<Restriction>> mAudioRestrictions = new SparseArray();
    final ArrayMap<IBinder, ClientState> mClients = new ArrayMap();
    private final Constants mConstants;
    Context mContext;
    boolean mFastWriteScheduled;
    final AtomicFile mFile;
    final Handler mHandler;
    long mLastUptime;
    final ArrayMap<IBinder, ModeCallback> mModeWatchers = new ArrayMap();
    final SparseArray<ArraySet<ModeCallback>> mOpModeWatchers = new SparseArray();
    private final ArrayMap<IBinder, ClientRestrictionState> mOpUserRestrictions = new ArrayMap();
    final ArrayMap<String, ArraySet<ModeCallback>> mPackageModeWatchers = new ArrayMap();
    SparseIntArray mProfileOwners;
    @VisibleForTesting
    final SparseArray<UidState> mUidStates = new SparseArray();
    final Runnable mWriteRunner = new Runnable() {
        public void run() {
            synchronized (AppOpsService.this) {
                AppOpsService.this.mWriteScheduled = false;
                AppOpsService.this.mFastWriteScheduled = false;
                new AsyncTask<Void, Void, Void>() {
                    protected Void doInBackground(Void... params) {
                        AppOpsService.this.writeState();
                        return null;
                    }
                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[]) null);
            }
        }
    };
    boolean mWriteScheduled;

    final class ActiveCallback implements DeathRecipient {
        final IAppOpsActiveCallback mCallback;
        final int mCallingPid;
        final int mCallingUid;
        final int mWatchingUid;

        ActiveCallback(IAppOpsActiveCallback callback, int watchingUid, int callingUid, int callingPid) {
            this.mCallback = callback;
            this.mWatchingUid = watchingUid;
            this.mCallingUid = callingUid;
            this.mCallingPid = callingPid;
            try {
                this.mCallback.asBinder().linkToDeath(this, 0);
            } catch (RemoteException e) {
            }
        }

        public String toString() {
            StringBuilder sb = new StringBuilder(128);
            sb.append("ActiveCallback{");
            sb.append(Integer.toHexString(System.identityHashCode(this)));
            sb.append(" watchinguid=");
            UserHandle.formatUid(sb, this.mWatchingUid);
            sb.append(" from uid=");
            UserHandle.formatUid(sb, this.mCallingUid);
            sb.append(" pid=");
            sb.append(this.mCallingPid);
            sb.append('}');
            return sb.toString();
        }

        void destroy() {
            this.mCallback.asBinder().unlinkToDeath(this, 0);
        }

        public void binderDied() {
            AppOpsService.this.stopWatchingActive(this.mCallback);
        }
    }

    private final class AppOpsManagerInternalImpl extends AppOpsManagerInternal {
        private AppOpsManagerInternalImpl() {
        }

        /* synthetic */ AppOpsManagerInternalImpl(AppOpsService x0, AnonymousClass1 x1) {
            this();
        }

        public void setDeviceAndProfileOwners(SparseIntArray owners) {
            synchronized (AppOpsService.this) {
                AppOpsService.this.mProfileOwners = owners;
            }
        }
    }

    static final class ChangeRec {
        final int op;
        final String pkg;
        final int uid;

        ChangeRec(int _op, int _uid, String _pkg) {
            this.op = _op;
            this.uid = _uid;
            this.pkg = _pkg;
        }
    }

    private final class ClientRestrictionState implements DeathRecipient {
        SparseArray<String[]> perUserExcludedPackages;
        SparseArray<boolean[]> perUserRestrictions;
        private final IBinder token;

        public ClientRestrictionState(IBinder token) throws RemoteException {
            token.linkToDeath(this, 0);
            this.token = token;
        }

        public boolean setRestriction(int code, boolean restricted, String[] excludedPackages, int userId) {
            int[] users;
            int i;
            boolean changed = false;
            if (this.perUserRestrictions == null && restricted) {
                this.perUserRestrictions = new SparseArray();
            }
            int i2 = 0;
            if (userId == -1) {
                List<UserInfo> liveUsers = UserManager.get(AppOpsService.this.mContext).getUsers(false);
                users = new int[liveUsers.size()];
                for (i = 0; i < liveUsers.size(); i++) {
                    users[i] = ((UserInfo) liveUsers.get(i)).id;
                }
            } else {
                users = new int[]{userId};
            }
            int[] users2 = users;
            if (this.perUserRestrictions != null) {
                int numUsers = users2.length;
                while (i2 < numUsers) {
                    i = users2[i2];
                    boolean[] userRestrictions = (boolean[]) this.perUserRestrictions.get(i);
                    if (userRestrictions == null && restricted) {
                        userRestrictions = new boolean[78];
                        this.perUserRestrictions.put(i, userRestrictions);
                    }
                    if (!(userRestrictions == null || userRestrictions[code] == restricted)) {
                        userRestrictions[code] = restricted;
                        if (!restricted && isDefault(userRestrictions)) {
                            this.perUserRestrictions.remove(i);
                            userRestrictions = null;
                        }
                        changed = true;
                    }
                    if (userRestrictions != null) {
                        boolean noExcludedPackages = ArrayUtils.isEmpty(excludedPackages);
                        if (this.perUserExcludedPackages == null && !noExcludedPackages) {
                            this.perUserExcludedPackages = new SparseArray();
                        }
                        if (!(this.perUserExcludedPackages == null || Arrays.equals(excludedPackages, (Object[]) this.perUserExcludedPackages.get(i)))) {
                            if (noExcludedPackages) {
                                this.perUserExcludedPackages.remove(i);
                                if (this.perUserExcludedPackages.size() <= 0) {
                                    this.perUserExcludedPackages = null;
                                }
                            } else {
                                this.perUserExcludedPackages.put(i, excludedPackages);
                            }
                            changed = true;
                        }
                    }
                    i2++;
                }
            }
            return changed;
        }

        public boolean hasRestriction(int restriction, String packageName, int userId) {
            if (this.perUserRestrictions == null) {
                return false;
            }
            boolean[] restrictions = (boolean[]) this.perUserRestrictions.get(userId);
            if (restrictions == null || !restrictions[restriction]) {
                return false;
            }
            if (this.perUserExcludedPackages == null) {
                return true;
            }
            String[] perUserExclusions = (String[]) this.perUserExcludedPackages.get(userId);
            if (perUserExclusions == null) {
                return true;
            }
            return true ^ ArrayUtils.contains(perUserExclusions, packageName);
        }

        public void removeUser(int userId) {
            if (this.perUserExcludedPackages != null) {
                this.perUserExcludedPackages.remove(userId);
                if (this.perUserExcludedPackages.size() <= 0) {
                    this.perUserExcludedPackages = null;
                }
            }
            if (this.perUserRestrictions != null) {
                this.perUserRestrictions.remove(userId);
                if (this.perUserRestrictions.size() <= 0) {
                    this.perUserRestrictions = null;
                }
            }
        }

        public boolean isDefault() {
            return this.perUserRestrictions == null || this.perUserRestrictions.size() <= 0;
        }

        public void binderDied() {
            synchronized (AppOpsService.this) {
                AppOpsService.this.mOpUserRestrictions.remove(this.token);
                if (this.perUserRestrictions == null) {
                    return;
                }
                int userCount = this.perUserRestrictions.size();
                for (int i = 0; i < userCount; i++) {
                    boolean[] restrictions = (boolean[]) this.perUserRestrictions.valueAt(i);
                    int restrictionCount = restrictions.length;
                    for (int j = 0; j < restrictionCount; j++) {
                        if (restrictions[j]) {
                            AppOpsService.this.mHandler.post(new -$$Lambda$AppOpsService$ClientRestrictionState$1l-YeBkF_Y04gZU4mqxsyXZNtwY(this, j));
                        }
                    }
                }
                destroy();
            }
        }

        public void destroy() {
            this.token.unlinkToDeath(this, 0);
        }

        private boolean isDefault(boolean[] array) {
            if (ArrayUtils.isEmpty(array)) {
                return true;
            }
            for (boolean value : array) {
                if (value) {
                    return false;
                }
            }
            return true;
        }
    }

    final class ClientState extends Binder implements DeathRecipient {
        final IBinder mAppToken;
        final int mPid;
        final ArrayList<Op> mStartedOps = new ArrayList();

        ClientState(IBinder appToken) {
            this.mAppToken = appToken;
            this.mPid = Binder.getCallingPid();
            if (!(appToken instanceof Binder)) {
                try {
                    this.mAppToken.linkToDeath(this, 0);
                } catch (RemoteException e) {
                }
            }
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("ClientState{mAppToken=");
            stringBuilder.append(this.mAppToken);
            stringBuilder.append(", pid=");
            stringBuilder.append(this.mPid);
            stringBuilder.append('}');
            return stringBuilder.toString();
        }

        public void binderDied() {
            synchronized (AppOpsService.this) {
                for (int i = this.mStartedOps.size() - 1; i >= 0; i--) {
                    AppOpsService.this.finishOperationLocked((Op) this.mStartedOps.get(i), true);
                }
                AppOpsService.this.mClients.remove(this.mAppToken);
            }
        }
    }

    private final class Constants extends ContentObserver {
        private static final String KEY_BG_STATE_SETTLE_TIME = "bg_state_settle_time";
        private static final String KEY_FG_SERVICE_STATE_SETTLE_TIME = "fg_service_state_settle_time";
        private static final String KEY_TOP_STATE_SETTLE_TIME = "top_state_settle_time";
        public long BG_STATE_SETTLE_TIME;
        public long FG_SERVICE_STATE_SETTLE_TIME;
        public long TOP_STATE_SETTLE_TIME;
        private final KeyValueListParser mParser = new KeyValueListParser(',');
        private ContentResolver mResolver;

        public Constants(Handler handler) {
            super(handler);
            updateConstants();
        }

        public void startMonitoring(ContentResolver resolver) {
            this.mResolver = resolver;
            this.mResolver.registerContentObserver(Global.getUriFor("app_ops_constants"), false, this);
            updateConstants();
        }

        public void onChange(boolean selfChange, Uri uri) {
            updateConstants();
        }

        private void updateConstants() {
            String value;
            if (this.mResolver != null) {
                value = Global.getString(this.mResolver, "app_ops_constants");
            } else {
                value = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
            }
            synchronized (AppOpsService.this) {
                try {
                    this.mParser.setString(value);
                } catch (IllegalArgumentException e) {
                    Slog.e(AppOpsService.TAG, "Bad app ops settings", e);
                }
                this.TOP_STATE_SETTLE_TIME = this.mParser.getDurationMillis(KEY_TOP_STATE_SETTLE_TIME, 30000);
                this.FG_SERVICE_STATE_SETTLE_TIME = this.mParser.getDurationMillis(KEY_FG_SERVICE_STATE_SETTLE_TIME, JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
                this.BG_STATE_SETTLE_TIME = this.mParser.getDurationMillis(KEY_BG_STATE_SETTLE_TIME, 1000);
            }
        }

        void dump(PrintWriter pw) {
            pw.println("  Settings:");
            pw.print("    ");
            pw.print(KEY_TOP_STATE_SETTLE_TIME);
            pw.print("=");
            TimeUtils.formatDuration(this.TOP_STATE_SETTLE_TIME, pw);
            pw.println();
            pw.print("    ");
            pw.print(KEY_FG_SERVICE_STATE_SETTLE_TIME);
            pw.print("=");
            TimeUtils.formatDuration(this.FG_SERVICE_STATE_SETTLE_TIME, pw);
            pw.println();
            pw.print("    ");
            pw.print(KEY_BG_STATE_SETTLE_TIME);
            pw.print("=");
            TimeUtils.formatDuration(this.BG_STATE_SETTLE_TIME, pw);
            pw.println();
        }
    }

    final class ModeCallback implements DeathRecipient {
        final IAppOpsCallback mCallback;
        final int mCallingPid;
        final int mCallingUid;
        final int mFlags;
        final int mWatchingUid;

        ModeCallback(IAppOpsCallback callback, int watchingUid, int flags, int callingUid, int callingPid) {
            this.mCallback = callback;
            this.mWatchingUid = watchingUid;
            this.mFlags = flags;
            this.mCallingUid = callingUid;
            this.mCallingPid = callingPid;
            try {
                this.mCallback.asBinder().linkToDeath(this, 0);
            } catch (RemoteException e) {
            }
        }

        public boolean isWatchingUid(int uid) {
            return uid == -2 || this.mWatchingUid < 0 || this.mWatchingUid == uid;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder(128);
            sb.append("ModeCallback{");
            sb.append(Integer.toHexString(System.identityHashCode(this)));
            sb.append(" watchinguid=");
            UserHandle.formatUid(sb, this.mWatchingUid);
            sb.append(" flags=0x");
            sb.append(Integer.toHexString(this.mFlags));
            sb.append(" from uid=");
            UserHandle.formatUid(sb, this.mCallingUid);
            sb.append(" pid=");
            sb.append(this.mCallingPid);
            sb.append('}');
            return sb.toString();
        }

        void unlinkToDeath() {
            this.mCallback.asBinder().unlinkToDeath(this, 0);
        }

        public void binderDied() {
            AppOpsService.this.stopWatchingMode(this.mCallback);
        }
    }

    static final class Op {
        int duration;
        int mode;
        final int op;
        final String packageName;
        String proxyPackageName;
        int proxyUid = -1;
        long[] rejectTime = new long[6];
        int startNesting;
        long startRealtime;
        long[] time = new long[6];
        final int uid;
        final UidState uidState;

        Op(UidState _uidState, String _packageName, int _op) {
            this.uidState = _uidState;
            this.uid = _uidState.uid;
            this.packageName = _packageName;
            this.op = _op;
            this.mode = AppOpsManager.opToDefaultMode(this.op);
        }

        boolean hasAnyTime() {
            int i = 0;
            while (i < 6) {
                if (this.time[i] != 0 || this.rejectTime[i] != 0) {
                    return true;
                }
                i++;
            }
            return false;
        }

        int getMode() {
            return this.uidState.evalMode(this.mode);
        }
    }

    static final class Ops extends SparseArray<Op> {
        final boolean isPrivileged;
        final String packageName;
        final UidState uidState;

        Ops(String _packageName, UidState _uidState, boolean _isPrivileged) {
            this.packageName = _packageName;
            this.uidState = _uidState;
            this.isPrivileged = _isPrivileged;
        }
    }

    private static final class Restriction {
        private static final ArraySet<String> NO_EXCEPTIONS = new ArraySet();
        ArraySet<String> exceptionPackages;
        int mode;

        private Restriction() {
            this.exceptionPackages = NO_EXCEPTIONS;
        }

        /* synthetic */ Restriction(AnonymousClass1 x0) {
            this();
        }
    }

    static class Shell extends ShellCommand {
        static final Binder sBinder = new Binder();
        final IAppOpsService mInterface;
        final AppOpsService mInternal;
        IBinder mToken;
        int mode;
        String modeStr;
        int nonpackageUid;
        int op;
        String opStr;
        String packageName;
        int packageUid;
        int userId = 0;

        Shell(IAppOpsService iface, AppOpsService internal) {
            this.mInterface = iface;
            this.mInternal = internal;
            try {
                this.mToken = this.mInterface.getToken(sBinder);
            } catch (RemoteException e) {
            }
        }

        public int onCommand(String cmd) {
            return AppOpsService.onShellCommand(this, cmd);
        }

        public void onHelp() {
            AppOpsService.dumpCommandHelp(getOutPrintWriter());
        }

        private static int strOpToOp(String op, PrintWriter err) {
            try {
                return AppOpsManager.strOpToOp(op);
            } catch (IllegalArgumentException e) {
                try {
                    return Integer.parseInt(op);
                } catch (NumberFormatException e2) {
                    try {
                        return AppOpsManager.strDebugOpToOp(op);
                    } catch (IllegalArgumentException e3) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Error: ");
                        stringBuilder.append(e3.getMessage());
                        err.println(stringBuilder.toString());
                        return -1;
                    }
                }
            }
        }

        static int strModeToMode(String modeStr, PrintWriter err) {
            for (int i = AppOpsManager.MODE_NAMES.length - 1; i >= 0; i--) {
                if (AppOpsManager.MODE_NAMES[i].equals(modeStr)) {
                    return i;
                }
            }
            try {
                return Integer.parseInt(modeStr);
            } catch (NumberFormatException e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Error: Mode ");
                stringBuilder.append(modeStr);
                stringBuilder.append(" is not valid");
                err.println(stringBuilder.toString());
                return -1;
            }
        }

        int parseUserOpMode(int defMode, PrintWriter err) throws RemoteException {
            this.userId = -2;
            this.opStr = null;
            this.modeStr = null;
            while (true) {
                String nextArg = getNextArg();
                String argument = nextArg;
                if (nextArg == null) {
                    break;
                } else if ("--user".equals(argument)) {
                    this.userId = UserHandle.parseUserArg(getNextArgRequired());
                } else if (this.opStr == null) {
                    this.opStr = argument;
                } else if (this.modeStr == null) {
                    this.modeStr = argument;
                    break;
                }
            }
            if (this.opStr == null) {
                err.println("Error: Operation not specified.");
                return -1;
            }
            this.op = strOpToOp(this.opStr, err);
            if (this.op < 0) {
                return -1;
            }
            if (this.modeStr != null) {
                int strModeToMode = strModeToMode(this.modeStr, err);
                this.mode = strModeToMode;
                if (strModeToMode < 0) {
                    return -1;
                }
            }
            this.mode = defMode;
            return 0;
        }

        int parseUserPackageOp(boolean reqOp, PrintWriter err) throws RemoteException {
            this.userId = -2;
            this.packageName = null;
            this.opStr = null;
            while (true) {
                String nextArg = getNextArg();
                String argument = nextArg;
                if (nextArg == null) {
                    break;
                } else if ("--user".equals(argument)) {
                    this.userId = UserHandle.parseUserArg(getNextArgRequired());
                } else if (this.packageName == null) {
                    this.packageName = argument;
                } else if (this.opStr == null) {
                    this.opStr = argument;
                    break;
                }
            }
            if (this.packageName == null) {
                err.println("Error: Package name not specified.");
                return -1;
            } else if (this.opStr == null && reqOp) {
                err.println("Error: Operation not specified.");
                return -1;
            } else {
                if (this.opStr != null) {
                    this.op = strOpToOp(this.opStr, err);
                    if (this.op < 0) {
                        return -1;
                    }
                }
                this.op = -1;
                if (this.userId == -2) {
                    this.userId = ActivityManager.getCurrentUser();
                }
                this.nonpackageUid = -1;
                try {
                    this.nonpackageUid = Integer.parseInt(this.packageName);
                } catch (NumberFormatException e) {
                }
                if (this.nonpackageUid == -1 && this.packageName.length() > 1 && this.packageName.charAt(0) == 'u' && this.packageName.indexOf(46) < 0) {
                    int i = 1;
                    while (i < this.packageName.length() && this.packageName.charAt(i) >= '0' && this.packageName.charAt(i) <= '9') {
                        i++;
                    }
                    if (i > 1 && i < this.packageName.length()) {
                        int i2;
                        try {
                            int user = Integer.parseInt(this.packageName.substring(1, i));
                            char type = this.packageName.charAt(i);
                            i++;
                            i2 = i;
                            while (i2 < this.packageName.length() && this.packageName.charAt(i2) >= '0' && this.packageName.charAt(i2) <= '9') {
                                try {
                                    i2++;
                                } catch (NumberFormatException e2) {
                                }
                            }
                            if (i2 > i) {
                                try {
                                    int typeVal = Integer.parseInt(this.packageName.substring(i, i2));
                                    if (type == 'a') {
                                        this.nonpackageUid = UserHandle.getUid(user, typeVal + 10000);
                                    } else if (type == 's') {
                                        this.nonpackageUid = UserHandle.getUid(user, typeVal);
                                    }
                                } catch (NumberFormatException e3) {
                                }
                            }
                        } catch (NumberFormatException e4) {
                            i2 = i;
                        }
                    }
                }
                if (this.nonpackageUid != -1) {
                    this.packageName = null;
                } else {
                    this.packageUid = AppOpsService.resolveUid(this.packageName);
                    if (this.packageUid < 0) {
                        this.packageUid = AppGlobals.getPackageManager().getPackageUid(this.packageName, 8192, this.userId);
                    }
                    if (this.packageUid < 0) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Error: No UID for ");
                        stringBuilder.append(this.packageName);
                        stringBuilder.append(" in user ");
                        stringBuilder.append(this.userId);
                        err.println(stringBuilder.toString());
                        return -1;
                    }
                }
                return 0;
            }
        }
    }

    @VisibleForTesting
    static final class UidState {
        public SparseBooleanArray foregroundOps;
        public boolean hasForegroundWatchers;
        public SparseIntArray opModes;
        public int pendingState = 5;
        public long pendingStateCommitTime;
        public ArrayMap<String, Ops> pkgOps;
        public int startNesting;
        public int state = 5;
        public final int uid;

        public UidState(int uid) {
            this.uid = uid;
        }

        public void clear() {
            this.pkgOps = null;
            this.opModes = null;
        }

        public boolean isDefault() {
            return (this.pkgOps == null || this.pkgOps.isEmpty()) && (this.opModes == null || this.opModes.size() <= 0);
        }

        int evalMode(int mode) {
            if (mode != 4) {
                return mode;
            }
            return this.state <= 2 ? 0 : 1;
        }

        private void evalForegroundWatchers(int op, SparseArray<ArraySet<ModeCallback>> watchers, SparseBooleanArray which) {
            boolean curValue = which.get(op, false);
            ArraySet<ModeCallback> callbacks = (ArraySet) watchers.get(op);
            if (callbacks != null) {
                int cbi = callbacks.size() - 1;
                while (!curValue && cbi >= 0) {
                    if ((((ModeCallback) callbacks.valueAt(cbi)).mFlags & 1) != 0) {
                        this.hasForegroundWatchers = true;
                        curValue = true;
                    }
                    cbi--;
                }
            }
            which.put(op, curValue);
        }

        public void evalForegroundOps(SparseArray<ArraySet<ModeCallback>> watchers) {
            int i;
            SparseBooleanArray which = null;
            this.hasForegroundWatchers = false;
            if (this.opModes != null) {
                for (i = this.opModes.size() - 1; i >= 0; i--) {
                    if (this.opModes.valueAt(i) == 4) {
                        if (which == null) {
                            which = new SparseBooleanArray();
                        }
                        evalForegroundWatchers(this.opModes.keyAt(i), watchers, which);
                    }
                }
            }
            if (this.pkgOps != null) {
                for (i = this.pkgOps.size() - 1; i >= 0; i--) {
                    Ops ops = (Ops) this.pkgOps.valueAt(i);
                    for (int j = ops.size() - 1; j >= 0; j--) {
                        if (((Op) ops.valueAt(j)).mode == 4) {
                            if (which == null) {
                                which = new SparseBooleanArray();
                            }
                            evalForegroundWatchers(ops.keyAt(j), watchers, which);
                        }
                    }
                }
            }
            this.foregroundOps = which;
        }
    }

    public AppOpsService(File storagePath, Handler handler) {
        LockGuard.installLock((Object) this, 0);
        this.mFile = new AtomicFile(storagePath, "appops");
        this.mHandler = handler;
        this.mConstants = new Constants(this.mHandler);
        readState();
    }

    public void publish(Context context) {
        this.mContext = context;
        ServiceManager.addService("appops", asBinder());
        LocalServices.addService(AppOpsManagerInternal.class, this.mAppOpsManagerInternal);
    }

    public void systemReady() {
        this.mConstants.startMonitoring(this.mContext.getContentResolver());
        synchronized (this) {
            boolean changed = false;
            for (int i = this.mUidStates.size() - 1; i >= 0; i--) {
                UidState uidState = (UidState) this.mUidStates.valueAt(i);
                if (ArrayUtils.isEmpty(getPackagesForUid(uidState.uid))) {
                    uidState.clear();
                    this.mUidStates.removeAt(i);
                    changed = true;
                } else {
                    ArrayMap<String, Ops> pkgs = uidState.pkgOps;
                    if (pkgs == null) {
                        continue;
                    } else {
                        Iterator<Ops> it = pkgs.values().iterator();
                        while (it.hasNext()) {
                            Ops ops = (Ops) it.next();
                            int curUid = -1;
                            try {
                                curUid = AppGlobals.getPackageManager().getPackageUid(ops.packageName, 8192, UserHandle.getUserId(ops.uidState.uid));
                            } catch (RemoteException e) {
                            }
                            if (curUid != ops.uidState.uid) {
                                String str = TAG;
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("Pruning old package ");
                                stringBuilder.append(ops.packageName);
                                stringBuilder.append(SliceAuthority.DELIMITER);
                                stringBuilder.append(ops.uidState);
                                stringBuilder.append(": new uid=");
                                stringBuilder.append(curUid);
                                Slog.i(str, stringBuilder.toString());
                                it.remove();
                                changed = true;
                            }
                        }
                        if (uidState.isDefault()) {
                            this.mUidStates.removeAt(i);
                        }
                    }
                }
            }
            if (changed) {
                scheduleFastWriteLocked();
            }
        }
        ((PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class)).setExternalSourcesPolicy(new ExternalSourcesPolicy() {
            public int getPackageTrustedToInstallApps(String packageName, int uid) {
                int appOpMode = AppOpsService.this.checkOperation(66, uid, packageName);
                if (appOpMode == 0) {
                    return 0;
                }
                if (appOpMode != 2) {
                    return 2;
                }
                return 1;
            }
        });
        ((StorageManagerInternal) LocalServices.getService(StorageManagerInternal.class)).addExternalStoragePolicy(new ExternalStorageMountPolicy() {
            public int getMountMode(int uid, String packageName) {
                if (Process.isIsolated(uid) || AppOpsService.this.noteOperation(59, uid, packageName) != 0) {
                    return 0;
                }
                if (AppOpsService.this.noteOperation(60, uid, packageName) != 0) {
                    return 2;
                }
                return 3;
            }

            public boolean hasExternalStorage(int uid, String packageName) {
                int mountMode = getMountMode(uid, packageName);
                return mountMode == 2 || mountMode == 3;
            }
        });
    }

    /* JADX WARNING: Missing block: B:38:0x00a2, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void packageRemoved(int uid, String packageName) {
        synchronized (this) {
            UidState uidState = (UidState) this.mUidStates.get(uid);
            if (uidState == null) {
                return;
            }
            int i;
            Ops ops = null;
            if (uidState.pkgOps != null) {
                ops = (Ops) uidState.pkgOps.remove(packageName);
            }
            if (ops != null && uidState.pkgOps.isEmpty() && getPackagesForUid(uid).length <= 0) {
                this.mUidStates.remove(uid);
            }
            int clientCount = this.mClients.size();
            for (i = 0; i < clientCount; i++) {
                ClientState client = (ClientState) this.mClients.valueAt(i);
                if (client.mStartedOps != null) {
                    for (int j = client.mStartedOps.size() - 1; j >= 0; j--) {
                        Op op = (Op) client.mStartedOps.get(j);
                        if (uid == op.uid && packageName.equals(op.packageName)) {
                            finishOperationLocked(op, true);
                            client.mStartedOps.remove(j);
                            if (op.startNesting <= 0) {
                                scheduleOpActiveChangedIfNeededLocked(op.op, uid, packageName, false);
                            }
                        }
                    }
                }
            }
            if (ops != null) {
                scheduleFastWriteLocked();
                i = ops.size();
                for (int i2 = 0; i2 < i; i2++) {
                    Op op2 = (Op) ops.valueAt(i2);
                    if (op2.duration == -1) {
                        scheduleOpActiveChangedIfNeededLocked(op2.op, op2.uid, op2.packageName, false);
                    }
                }
            }
        }
    }

    public void uidRemoved(int uid) {
        synchronized (this) {
            if (this.mUidStates.indexOfKey(uid) >= 0) {
                this.mUidStates.remove(uid);
                if (this.mAppOpsResource != null) {
                    this.mAppOpsResource.clear(uid, null, 0);
                }
                scheduleFastWriteLocked();
            }
        }
    }

    public void updateUidProcState(int uid, int procState) {
        synchronized (this) {
            UidState uidState = getUidStateLocked(uid, true);
            int newState = PROCESS_STATE_TO_UID_STATE[procState];
            if (!(uidState == null || uidState.pendingState == newState)) {
                long settleTime;
                int oldPendingState = uidState.pendingState;
                uidState.pendingState = newState;
                if (newState < uidState.state || newState <= 2) {
                    commitUidPendingStateLocked(uidState);
                } else if (uidState.pendingStateCommitTime == 0) {
                    if (uidState.state <= 1) {
                        settleTime = this.mConstants.TOP_STATE_SETTLE_TIME;
                    } else if (uidState.state <= 2) {
                        settleTime = this.mConstants.FG_SERVICE_STATE_SETTLE_TIME;
                    } else {
                        settleTime = this.mConstants.BG_STATE_SETTLE_TIME;
                    }
                    uidState.pendingStateCommitTime = SystemClock.uptimeMillis() + settleTime;
                }
                if (uidState.startNesting != 0) {
                    settleTime = System.currentTimeMillis();
                    for (int i = uidState.pkgOps.size() - 1; i >= 0; i--) {
                        Ops ops = (Ops) uidState.pkgOps.valueAt(i);
                        for (int j = ops.size() - 1; j >= 0; j--) {
                            Op op = (Op) ops.valueAt(j);
                            if (op.startNesting > 0) {
                                op.time[oldPendingState] = settleTime;
                                op.time[newState] = settleTime;
                            }
                        }
                    }
                }
            }
        }
    }

    public void shutdown() {
        Slog.w(TAG, "Writing app ops before shutdown...");
        boolean doWrite = false;
        synchronized (this) {
            if (this.mWriteScheduled) {
                this.mWriteScheduled = false;
                doWrite = true;
            }
        }
        if (doWrite) {
            writeState();
        }
    }

    private ArrayList<OpEntry> collectOps(Ops pkgOps, int[] ops) {
        Ops ops2 = pkgOps;
        int[] iArr = ops;
        long elapsedNow = SystemClock.elapsedRealtime();
        int i = -1;
        long duration;
        long elapsedNow2;
        if (iArr == null) {
            ArrayList<OpEntry> resOps = new ArrayList();
            int j = 0;
            while (j < pkgOps.size()) {
                long j2;
                Op curOp = (Op) ops2.valueAt(j);
                boolean running = curOp.duration == i;
                if (running) {
                    j2 = elapsedNow - curOp.startRealtime;
                } else {
                    j2 = (long) curOp.duration;
                }
                duration = j2;
                int i2 = curOp.op;
                int i3 = curOp.mode;
                long[] jArr = curOp.time;
                long[] jArr2 = curOp.rejectTime;
                i = (int) duration;
                elapsedNow2 = elapsedNow;
                int i4 = curOp.proxyUid;
                String str = curOp.proxyPackageName;
                OpEntry opEntry = r11;
                OpEntry opEntry2 = new OpEntry(i2, i3, jArr, jArr2, i, running, i4, str);
                resOps.add(opEntry);
                j++;
                elapsedNow = elapsedNow2;
                i = -1;
            }
            return resOps;
        }
        elapsedNow2 = elapsedNow;
        ArrayList<OpEntry> resOps2 = null;
        int j3 = 0;
        while (j3 < iArr.length) {
            Op curOp2 = (Op) ops2.get(iArr[j3]);
            if (curOp2 != null) {
                long j4;
                if (resOps2 == null) {
                    resOps2 = new ArrayList();
                }
                boolean running2 = curOp2.duration == -1;
                if (running2) {
                    j4 = elapsedNow2 - curOp2.startRealtime;
                } else {
                    j4 = (long) curOp2.duration;
                }
                duration = j4;
                OpEntry opEntry3 = r7;
                OpEntry opEntry4 = new OpEntry(curOp2.op, curOp2.mode, curOp2.time, curOp2.rejectTime, (int) duration, running2, curOp2.proxyUid, curOp2.proxyPackageName);
                resOps2.add(opEntry3);
            }
            j3++;
            ops2 = pkgOps;
        }
        return resOps2;
    }

    private ArrayList<OpEntry> collectOps(SparseIntArray uidOps, int[] ops) {
        SparseIntArray sparseIntArray = uidOps;
        int[] iArr = ops;
        ArrayList<OpEntry> resOps = null;
        int j = 0;
        if (iArr == null) {
            resOps = new ArrayList();
            while (j < uidOps.size()) {
                resOps.add(new OpEntry(sparseIntArray.keyAt(j), sparseIntArray.valueAt(j), 0, 0, 0, -1, null));
                j++;
            }
        } else {
            while (j < iArr.length) {
                int index = sparseIntArray.indexOfKey(iArr[j]);
                if (index >= 0) {
                    if (resOps == null) {
                        resOps = new ArrayList();
                    }
                    resOps.add(new OpEntry(sparseIntArray.keyAt(index), sparseIntArray.valueAt(index), 0, 0, 0, -1, null));
                }
                j++;
            }
        }
        return resOps;
    }

    public List<PackageOps> getPackagesForOps(int[] ops) {
        ArrayList<PackageOps> res;
        this.mContext.enforcePermission("android.permission.GET_APP_OPS_STATS", Binder.getCallingPid(), Binder.getCallingUid(), null);
        synchronized (this) {
            ArrayList<PackageOps> res2;
            try {
                int uidStateCount = this.mUidStates.size();
                ArrayList<PackageOps> res3 = null;
                int i = 0;
                while (i < uidStateCount) {
                    try {
                        UidState uidState = (UidState) this.mUidStates.valueAt(i);
                        if (!(uidState.pkgOps == null || uidState.pkgOps.isEmpty())) {
                            ArrayMap<String, Ops> packages = uidState.pkgOps;
                            int packageCount = packages.size();
                            res2 = res3;
                            int j = 0;
                            while (j < packageCount) {
                                try {
                                    Ops pkgOps = (Ops) packages.valueAt(j);
                                    ArrayList<OpEntry> resOps = collectOps(pkgOps, ops);
                                    if (resOps != null) {
                                        if (res2 == null) {
                                            res2 = new ArrayList();
                                        }
                                        res2.add(new PackageOps(pkgOps.packageName, pkgOps.uidState.uid, resOps));
                                    }
                                    j++;
                                } catch (Throwable th) {
                                    res = th;
                                    throw res;
                                }
                            }
                            res3 = res2;
                        }
                        i++;
                    } catch (Throwable th2) {
                        res = th2;
                        res2 = res3;
                        throw res;
                    }
                }
                return res3;
            } catch (Throwable th3) {
                res2 = null;
                res = th3;
                throw res;
            }
        }
    }

    public List<PackageOps> getOpsForPackage(int uid, String packageName, int[] ops) {
        this.mContext.enforcePermission("android.permission.GET_APP_OPS_STATS", Binder.getCallingPid(), Binder.getCallingUid(), null);
        String resolvedPackageName = resolvePackageName(uid, packageName);
        if (resolvedPackageName == null) {
            return Collections.emptyList();
        }
        synchronized (this) {
            Ops pkgOps = getOpsRawLocked(uid, resolvedPackageName, false, false);
            if (pkgOps == null) {
                return null;
            }
            ArrayList<OpEntry> resOps = collectOps(pkgOps, ops);
            if (resOps == null) {
                return null;
            }
            ArrayList<PackageOps> res = new ArrayList();
            res.add(new PackageOps(pkgOps.packageName, pkgOps.uidState.uid, resOps));
            return res;
        }
    }

    public List<PackageOps> getUidOps(int uid, int[] ops) {
        this.mContext.enforcePermission("android.permission.GET_APP_OPS_STATS", Binder.getCallingPid(), Binder.getCallingUid(), null);
        synchronized (this) {
            UidState uidState = getUidStateLocked(uid, null);
            if (uidState == null) {
                return null;
            }
            ArrayList<OpEntry> resOps = collectOps(uidState.opModes, ops);
            if (resOps == null) {
                return null;
            }
            ArrayList<PackageOps> res = new ArrayList();
            res.add(new PackageOps(null, uidState.uid, resOps));
            return res;
        }
    }

    private void pruneOp(Op op, int uid, String packageName) {
        if (!op.hasAnyTime()) {
            Ops ops = getOpsRawLocked(uid, packageName, false, false);
            if (ops != null) {
                ops.remove(op.op);
                if (ops.size() <= 0) {
                    UidState uidState = ops.uidState;
                    ArrayMap<String, Ops> pkgOps = uidState.pkgOps;
                    if (pkgOps != null) {
                        pkgOps.remove(ops.packageName);
                        if (pkgOps.isEmpty()) {
                            uidState.pkgOps = null;
                        }
                        if (uidState.isDefault()) {
                            this.mUidStates.remove(uid);
                        }
                    }
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:16:0x0024, code:
            r6.mContext.enforcePermission("android.permission.MANAGE_APP_OPS_MODES", android.os.Binder.getCallingPid(), android.os.Binder.getCallingUid(), null);
     */
    /* JADX WARNING: Missing block: B:17:0x0034, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void enforceManageAppOpsModes(int callingPid, int callingUid, int targetUid) {
        if (callingPid != Process.myPid()) {
            int callingUser = UserHandle.getUserId(callingUid);
            synchronized (this) {
                if (this.mProfileOwners == null || this.mProfileOwners.get(callingUser, -1) != callingUid || targetUid < 0 || callingUser != UserHandle.getUserId(targetUid)) {
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:24:0x007f, code:
            r13 = getPackagesForUid(r21);
            r1 = null;
     */
    /* JADX WARNING: Missing block: B:25:0x0084, code:
            monitor-enter(r19);
     */
    /* JADX WARNING: Missing block: B:27:?, code:
            r0 = (android.util.ArraySet) r7.mOpModeWatchers.get(r10);
     */
    /* JADX WARNING: Missing block: B:28:0x008d, code:
            if (r0 == null) goto L_0x00b8;
     */
    /* JADX WARNING: Missing block: B:29:0x008f, code:
            r2 = r0.size();
     */
    /* JADX WARNING: Missing block: B:30:0x0093, code:
            r3 = null;
            r1 = 0;
     */
    /* JADX WARNING: Missing block: B:31:0x0095, code:
            if (r1 >= r2) goto L_0x00b7;
     */
    /* JADX WARNING: Missing block: B:33:?, code:
            r4 = (com.android.server.AppOpsService.ModeCallback) r0.valueAt(r1);
            r5 = new android.util.ArraySet();
            java.util.Collections.addAll(r5, r13);
     */
    /* JADX WARNING: Missing block: B:34:0x00a5, code:
            if (r3 != null) goto L_0x00ad;
     */
    /* JADX WARNING: Missing block: B:35:0x00a7, code:
            r3 = new android.util.ArrayMap();
     */
    /* JADX WARNING: Missing block: B:36:0x00ad, code:
            r3.put(r4, r5);
     */
    /* JADX WARNING: Missing block: B:37:0x00b0, code:
            r1 = r1 + 1;
     */
    /* JADX WARNING: Missing block: B:38:0x00b3, code:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:39:0x00b4, code:
            r1 = r3;
     */
    /* JADX WARNING: Missing block: B:40:0x00b7, code:
            r1 = r3;
     */
    /* JADX WARNING: Missing block: B:42:?, code:
            r2 = r13.length;
     */
    /* JADX WARNING: Missing block: B:43:0x00b9, code:
            r14 = r1;
            r1 = r0;
            r0 = 0;
     */
    /* JADX WARNING: Missing block: B:44:0x00bc, code:
            if (r0 >= r2) goto L_0x0100;
     */
    /* JADX WARNING: Missing block: B:46:?, code:
            r3 = r13[r0];
            r1 = (android.util.ArraySet) r7.mPackageModeWatchers.get(r3);
     */
    /* JADX WARNING: Missing block: B:47:0x00c9, code:
            if (r1 == null) goto L_0x00f8;
     */
    /* JADX WARNING: Missing block: B:48:0x00cb, code:
            if (r14 != null) goto L_0x00d3;
     */
    /* JADX WARNING: Missing block: B:49:0x00cd, code:
            r14 = new android.util.ArrayMap();
     */
    /* JADX WARNING: Missing block: B:50:0x00d3, code:
            r4 = r1.size();
            r5 = r11;
     */
    /* JADX WARNING: Missing block: B:51:0x00d8, code:
            if (r5 >= r4) goto L_0x00f8;
     */
    /* JADX WARNING: Missing block: B:52:0x00da, code:
            r6 = (com.android.server.AppOpsService.ModeCallback) r1.valueAt(r5);
            r15 = (android.util.ArraySet) r14.get(r6);
     */
    /* JADX WARNING: Missing block: B:53:0x00e6, code:
            if (r15 != null) goto L_0x00f1;
     */
    /* JADX WARNING: Missing block: B:54:0x00e8, code:
            r15 = new android.util.ArraySet();
            r14.put(r6, r15);
     */
    /* JADX WARNING: Missing block: B:55:0x00f1, code:
            r15.add(r3);
            r5 = r5 + 1;
     */
    /* JADX WARNING: Missing block: B:56:0x00f8, code:
            r0 = r0 + 1;
            r11 = false;
     */
    /* JADX WARNING: Missing block: B:57:0x00fc, code:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:58:0x00fd, code:
            r1 = r14;
     */
    /* JADX WARNING: Missing block: B:59:0x0100, code:
            monitor-exit(r19);
     */
    /* JADX WARNING: Missing block: B:60:0x0101, code:
            if (r14 != null) goto L_0x0105;
     */
    /* JADX WARNING: Missing block: B:61:0x0103, code:
            return;
     */
    /* JADX WARNING: Missing block: B:63:0x0109, code:
            if (0 >= r14.size()) goto L_0x016f;
     */
    /* JADX WARNING: Missing block: B:64:0x010b, code:
            r11 = (com.android.server.AppOpsService.ModeCallback) r14.keyAt(0);
            r15 = (android.util.ArraySet) r14.valueAt(0);
     */
    /* JADX WARNING: Missing block: B:65:0x0119, code:
            if (r15 != null) goto L_0x0138;
     */
    /* JADX WARNING: Missing block: B:66:0x011b, code:
            r12 = r7.mHandler;
            r12.sendMessage(com.android.internal.util.function.pooled.PooledLambda.obtainMessage(com.android.server.-$$Lambda$AppOpsService$lxgFmOnGguOiLyfUZbyOpNBfTVw.INSTANCE, r7, r11, java.lang.Integer.valueOf(r10), java.lang.Integer.valueOf(r21), (java.lang.String) r12));
     */
    /* JADX WARNING: Missing block: B:67:0x0138, code:
            r12 = r15.size();
            r1 = 0;
     */
    /* JADX WARNING: Missing block: B:68:0x013d, code:
            r6 = r1;
     */
    /* JADX WARNING: Missing block: B:69:0x013e, code:
            if (r6 >= r12) goto L_0x0169;
     */
    /* JADX WARNING: Missing block: B:70:0x0140, code:
            r16 = (java.lang.String) r15.valueAt(r6);
            r5 = r7.mHandler;
            r8 = r5;
            r17 = r6;
            r8.sendMessage(com.android.internal.util.function.pooled.PooledLambda.obtainMessage(com.android.server.-$$Lambda$AppOpsService$lxgFmOnGguOiLyfUZbyOpNBfTVw.INSTANCE, r7, r11, java.lang.Integer.valueOf(r10), java.lang.Integer.valueOf(r21), r16));
            r1 = r17 + 1;
            r8 = r21;
     */
    /* JADX WARNING: Missing block: B:71:0x0169, code:
            r0 = 0 + 1;
            r8 = r21;
     */
    /* JADX WARNING: Missing block: B:72:0x016f, code:
            return;
     */
    /* JADX WARNING: Missing block: B:73:0x0170, code:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:75:?, code:
            monitor-exit(r19);
     */
    /* JADX WARNING: Missing block: B:76:0x0172, code:
            throw r0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void setUidMode(int code, int uid, int mode) {
        int i = uid;
        int i2 = mode;
        enforceManageAppOpsModes(Binder.getCallingPid(), Binder.getCallingUid(), i);
        verifyIncomingOp(code);
        int code2 = AppOpsManager.opToSwitch(code);
        synchronized (this) {
            int defaultMode = AppOpsManager.opToDefaultMode(code2);
            boolean z = false;
            UidState uidState = getUidStateLocked(i, false);
            SparseIntArray reportedPackageCount = null;
            if (uidState == null) {
                if (i2 == defaultMode) {
                    return;
                }
                uidState = new UidState(i);
                uidState.opModes = new SparseIntArray();
                uidState.opModes.put(code2, i2);
                this.mUidStates.put(i, uidState);
                scheduleWriteLocked();
            } else if (uidState.opModes == null) {
                if (i2 != defaultMode) {
                    uidState.opModes = new SparseIntArray();
                    uidState.opModes.put(code2, i2);
                    scheduleWriteLocked();
                }
            } else if (uidState.opModes.get(code2) == i2) {
            } else {
                if (i2 == defaultMode) {
                    uidState.opModes.delete(code2);
                    if (uidState.opModes.size() <= 0) {
                        uidState.opModes = null;
                    }
                } else {
                    uidState.opModes.put(code2, i2);
                }
                scheduleWriteLocked();
            }
        }
    }

    public void setMode(int code, int uid, String packageName, int mode) {
        HwFrameworkFactory.getHwBehaviorCollectManager().sendBehavior(BehaviorId.APPOPS_SETMODE);
        enforceManageAppOpsModes(Binder.getCallingPid(), Binder.getCallingUid(), uid);
        verifyIncomingOp(code);
        ArraySet<ModeCallback> repCbs = null;
        code = AppOpsManager.opToSwitch(code);
        synchronized (this) {
            UidState uidState = getUidStateLocked(uid, null);
            Op op = getOpLocked(code, uid, packageName, true);
            if (!(op == null || op.mode == mode)) {
                op.mode = mode;
                if (uidState != null) {
                    uidState.evalForegroundOps(this.mOpModeWatchers);
                }
                ArraySet<ModeCallback> cbs = (ArraySet) this.mOpModeWatchers.get(code);
                if (cbs != null) {
                    if (null == null) {
                        repCbs = new ArraySet();
                    }
                    repCbs.addAll(cbs);
                }
                cbs = (ArraySet) this.mPackageModeWatchers.get(packageName);
                if (cbs != null) {
                    if (repCbs == null) {
                        repCbs = new ArraySet();
                    }
                    repCbs.addAll(cbs);
                }
                if (mode == AppOpsManager.opToDefaultMode(op.op)) {
                    pruneOp(op, uid, packageName);
                }
                scheduleFastWriteLocked();
            }
        }
        if (repCbs != null) {
            this.mHandler.sendMessage(PooledLambda.obtainMessage(-$$Lambda$AppOpsService$1lQKm3WHEUQsD7KzYyJ5stQSc04.INSTANCE, this, repCbs, Integer.valueOf(code), Integer.valueOf(uid), packageName));
        }
    }

    private void notifyOpChanged(ArraySet<ModeCallback> callbacks, int code, int uid, String packageName) {
        for (int i = 0; i < callbacks.size(); i++) {
            notifyOpChanged((ModeCallback) callbacks.valueAt(i), code, uid, packageName);
        }
    }

    private void notifyOpChanged(ModeCallback callback, int code, int uid, String packageName) {
        if (uid == -2 || callback.mWatchingUid < 0 || callback.mWatchingUid == uid) {
            long identity = Binder.clearCallingIdentity();
            try {
                callback.mCallback.opChanged(code, uid, packageName);
            } catch (RemoteException e) {
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(identity);
            }
            Binder.restoreCallingIdentity(identity);
        }
    }

    private static HashMap<ModeCallback, ArrayList<ChangeRec>> addCallbacks(HashMap<ModeCallback, ArrayList<ChangeRec>> callbacks, int op, int uid, String packageName, ArraySet<ModeCallback> cbs) {
        if (cbs == null) {
            return callbacks;
        }
        if (callbacks == null) {
            callbacks = new HashMap();
        }
        int N = cbs.size();
        boolean duplicate = false;
        for (int i = 0; i < N; i++) {
            ModeCallback cb = (ModeCallback) cbs.valueAt(i);
            ArrayList<ChangeRec> reports = (ArrayList) callbacks.get(cb);
            if (reports != null) {
                int reportCount = reports.size();
                for (int j = 0; j < reportCount; j++) {
                    ChangeRec report = (ChangeRec) reports.get(j);
                    if (report.op == op && report.pkg.equals(packageName)) {
                        duplicate = true;
                        break;
                    }
                }
            } else {
                reports = new ArrayList();
                callbacks.put(cb, reports);
            }
            if (!duplicate) {
                reports.add(new ChangeRec(op, uid, packageName));
            }
        }
        return callbacks;
    }

    /* JADX WARNING: Removed duplicated region for block: B:65:0x0116 A:{Catch:{ all -> 0x01dd, all -> 0x01ef }} */
    /* JADX WARNING: Removed duplicated region for block: B:95:0x01c7 A:{Catch:{ all -> 0x01dd, all -> 0x01ef }} */
    /* JADX WARNING: Removed duplicated region for block: B:136:0x01d5 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:97:0x01d0 A:{Catch:{ all -> 0x01dd, all -> 0x01ef }} */
    /* JADX WARNING: Missing block: B:101:0x01e5, code:
            r22 = r5;
            r23 = r6;
     */
    /* JADX WARNING: Missing block: B:102:0x01e9, code:
            if (r0 == false) goto L_0x01f7;
     */
    /* JADX WARNING: Missing block: B:103:0x01eb, code:
            scheduleFastWriteLocked();
     */
    /* JADX WARNING: Missing block: B:109:0x01f8, code:
            if (r11 == null) goto L_0x0262;
     */
    /* JADX WARNING: Missing block: B:110:0x01fa, code:
            r0 = r11.entrySet().iterator();
     */
    /* JADX WARNING: Missing block: B:112:0x0206, code:
            if (r0.hasNext() == false) goto L_0x0262;
     */
    /* JADX WARNING: Missing block: B:113:0x0208, code:
            r12 = (java.util.Map.Entry) r0.next();
            r13 = (com.android.server.AppOpsService.ModeCallback) r12.getKey();
            r14 = (java.util.ArrayList) r12.getValue();
            r1 = 0;
     */
    /* JADX WARNING: Missing block: B:114:0x021e, code:
            r15 = r1;
     */
    /* JADX WARNING: Missing block: B:115:0x0223, code:
            if (r15 >= r14.size()) goto L_0x025b;
     */
    /* JADX WARNING: Missing block: B:116:0x0225, code:
            r6 = (com.android.server.AppOpsService.ChangeRec) r14.get(r15);
            r5 = r7.mHandler;
            r1 = com.android.server.-$$Lambda$AppOpsService$lxgFmOnGguOiLyfUZbyOpNBfTVw.INSTANCE;
            r4 = java.lang.Integer.valueOf(r6.op);
            r16 = java.lang.Integer.valueOf(r6.uid);
            r28 = r0;
            r0 = r5;
            r19 = r22;
            r5 = r16;
            r20 = r6;
            r16 = r23;
            r0.sendMessage(com.android.internal.util.function.pooled.PooledLambda.obtainMessage(r1, r7, r13, r4, r5, r6.pkg));
            r1 = r15 + 1;
            r0 = r28;
     */
    /* JADX WARNING: Missing block: B:117:0x025b, code:
            r28 = r0;
            r19 = r22;
            r16 = r23;
     */
    /* JADX WARNING: Missing block: B:118:0x0262, code:
            r19 = r22;
            r16 = r23;
     */
    /* JADX WARNING: Missing block: B:119:0x0266, code:
            return;
     */
    /* JADX WARNING: Missing block: B:120:0x0267, code:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:121:0x0268, code:
            r19 = r22;
            r16 = r23;
            r1 = r11;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void resetAllModes(int reqUserId, String reqPackageName) {
        Throwable th;
        int i;
        int i2;
        HashMap<ModeCallback, ArrayList<ChangeRec>> hashMap;
        int callingUid;
        SparseIntArray sparseIntArray;
        String str = reqPackageName;
        int callingPid = Binder.getCallingPid();
        int callingUid2 = Binder.getCallingUid();
        int reqUserId2 = ActivityManager.handleIncomingUser(callingPid, callingUid2, reqUserId, true, true, "resetAllModes", null);
        int reqUid = -1;
        if (str != null) {
            try {
                reqUid = AppGlobals.getPackageManager().getPackageUid(str, 8192, reqUserId2);
            } catch (RemoteException e) {
            }
        }
        int reqUid2 = reqUid;
        enforceManageAppOpsModes(callingPid, callingUid2, reqUid2);
        synchronized (this) {
            boolean changed = false;
            try {
                int i3 = this.mUidStates.size() - 1;
                HashMap<ModeCallback, ArrayList<ChangeRec>> callbacks = null;
                while (true) {
                    reqUid = i3;
                    if (reqUid < 0) {
                        break;
                    }
                    try {
                        int callingPid2;
                        Map<String, Ops> packages;
                        Iterator<Entry<String, Ops>> it;
                        boolean uidChanged;
                        UidState uidState = (UidState) this.mUidStates.valueAt(reqUid);
                        SparseIntArray opModes = uidState.opModes;
                        if (opModes != null) {
                            SparseIntArray opModes2;
                            String packageName;
                            Object opModes3;
                            if (uidState.uid == reqUid2 || reqUid2 == -1) {
                                int j = opModes.size() - 1;
                                while (j >= 0) {
                                    int code = opModes.keyAt(j);
                                    if (AppOpsManager.opAllowsReset(code)) {
                                        opModes.removeAt(j);
                                        if (opModes.size() <= 0) {
                                            try {
                                                uidState.opModes = null;
                                            } catch (Throwable th2) {
                                                th = th2;
                                                i = callingUid2;
                                                i2 = callingPid;
                                                hashMap = callbacks;
                                            }
                                        }
                                        String[] packagesForUid = getPackagesForUid(uidState.uid);
                                        int length = packagesForUid.length;
                                        opModes2 = opModes;
                                        opModes = callbacks;
                                        int callbacks2 = 0;
                                        while (callbacks2 < length) {
                                            int i4;
                                            try {
                                                i4 = length;
                                                callingUid = callingUid2;
                                                try {
                                                    callingPid2 = callingPid;
                                                    packageName = packagesForUid[callbacks2];
                                                } catch (Throwable th3) {
                                                    th = th3;
                                                    sparseIntArray = opModes;
                                                    i2 = callingPid;
                                                    i = callingUid;
                                                }
                                            } catch (Throwable th4) {
                                                th = th4;
                                                sparseIntArray = opModes;
                                                i = callingUid2;
                                                i2 = callingPid;
                                            }
                                            try {
                                                opModes = addCallbacks(opModes, code, uidState.uid, packageName, (ArraySet) this.mOpModeWatchers.get(code));
                                                opModes3 = addCallbacks(opModes, code, uidState.uid, packageName, (ArraySet) this.mPackageModeWatchers.get(packageName));
                                                callbacks2++;
                                                length = i4;
                                                callingUid2 = callingUid;
                                                callingPid = callingPid2;
                                            } catch (Throwable th5) {
                                                th = th5;
                                                sparseIntArray = opModes;
                                            }
                                        }
                                        callingUid = callingUid2;
                                        callingPid2 = callingPid;
                                        callbacks = opModes;
                                    } else {
                                        opModes2 = opModes;
                                        callingUid = callingUid2;
                                        callingPid2 = callingPid;
                                    }
                                    j--;
                                    opModes = opModes2;
                                    callingUid2 = callingUid;
                                    callingPid = callingPid2;
                                }
                            } else {
                                opModes2 = opModes;
                                callingUid = callingUid2;
                                callingPid2 = callingPid;
                                if (uidState.pkgOps != null && (reqUserId2 == -1 || reqUserId2 == UserHandle.getUserId(uidState.uid))) {
                                    packages = uidState.pkgOps;
                                    it = packages.entrySet().iterator();
                                    uidChanged = false;
                                    while (it.hasNext()) {
                                        Entry<String, Ops> ent = (Entry) it.next();
                                        packageName = (String) ent.getKey();
                                        if (str == null || str.equals(packageName)) {
                                            boolean changed2;
                                            Map<String, Ops> packages2;
                                            Entry<String, Ops> ent2;
                                            Ops pkgOps = (Ops) ent.getValue();
                                            int j2 = pkgOps.size() - 1;
                                            while (j2 >= 0) {
                                                Op curOp = (Op) pkgOps.valueAt(j2);
                                                if (AppOpsManager.opAllowsReset(curOp.op)) {
                                                    changed2 = changed;
                                                    if (curOp.mode != AppOpsManager.opToDefaultMode(curOp.op)) {
                                                        curOp.mode = AppOpsManager.opToDefaultMode(curOp.op);
                                                        uidChanged = true;
                                                        boolean changed3 = true;
                                                        packages2 = packages;
                                                        ent2 = ent;
                                                        opModes3 = addCallbacks(callbacks, curOp.op, curOp.uid, packageName, (ArraySet) this.mOpModeWatchers.get(curOp.op));
                                                        HashMap<ModeCallback, ArrayList<ChangeRec>> packages3 = addCallbacks(opModes3, curOp.op, curOp.uid, packageName, (ArraySet) this.mPackageModeWatchers.get(packageName));
                                                        if (!curOp.hasAnyTime()) {
                                                            pkgOps.removeAt(j2);
                                                        }
                                                        callbacks = packages3;
                                                        changed = changed3;
                                                        j2--;
                                                        packages = packages2;
                                                        ent = ent2;
                                                    } else {
                                                        packages2 = packages;
                                                        ent2 = ent;
                                                    }
                                                } else {
                                                    changed2 = changed;
                                                    packages2 = packages;
                                                    ent2 = ent;
                                                }
                                                changed = changed2;
                                                j2--;
                                                packages = packages2;
                                                ent = ent2;
                                            }
                                            changed2 = changed;
                                            packages2 = packages;
                                            ent2 = ent;
                                            if (pkgOps.size() == 0) {
                                                it.remove();
                                            }
                                            changed = changed2;
                                            packages = packages2;
                                        }
                                    }
                                    if (uidState.isDefault()) {
                                        this.mUidStates.remove(uidState.uid);
                                    }
                                    if (!uidChanged) {
                                        uidState.evalForegroundOps(this.mOpModeWatchers);
                                    }
                                }
                                i3 = reqUid - 1;
                                callingUid2 = callingUid;
                                callingPid = callingPid2;
                            }
                        }
                        callingUid = callingUid2;
                        callingPid2 = callingPid;
                        packages = uidState.pkgOps;
                        it = packages.entrySet().iterator();
                        uidChanged = false;
                        while (it.hasNext()) {
                        }
                        if (uidState.isDefault()) {
                        }
                        if (!uidChanged) {
                        }
                        i3 = reqUid - 1;
                        callingUid2 = callingUid;
                        callingPid = callingPid2;
                    } catch (Throwable th6) {
                        th = th6;
                        hashMap = callbacks;
                    }
                }
            } catch (Throwable th7) {
                th = th7;
                i = callingUid2;
                i2 = callingPid;
                while (true) {
                    try {
                        break;
                    } catch (Throwable th8) {
                        th = th8;
                    }
                }
                throw th;
            }
        }
    }

    private void evalAllForegroundOpsLocked() {
        for (int uidi = this.mUidStates.size() - 1; uidi >= 0; uidi--) {
            UidState uidState = (UidState) this.mUidStates.valueAt(uidi);
            if (uidState.foregroundOps != null) {
                uidState.evalForegroundOps(this.mOpModeWatchers);
            }
        }
    }

    public void startWatchingMode(int op, String packageName, IAppOpsCallback callback) {
        startWatchingModeWithFlags(op, packageName, 0, callback);
    }

    public void startWatchingModeWithFlags(int op, String packageName, int flags, IAppOpsCallback callback) {
        Throwable th;
        int i = op;
        String str = packageName;
        int callingUid = Binder.getCallingUid();
        int callingPid = Binder.getCallingPid();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Invalid op code: ");
        stringBuilder.append(i);
        Preconditions.checkArgumentInRange(i, -1, 77, stringBuilder.toString());
        if (callback != null) {
            synchronized (this) {
                int opToSwitch;
                int i2;
                if (i != -1) {
                    try {
                        opToSwitch = AppOpsManager.opToSwitch(op);
                    } catch (Throwable th2) {
                        th = th2;
                        i2 = i;
                    }
                } else {
                    opToSwitch = i;
                }
                i2 = opToSwitch;
                try {
                    ArraySet<ModeCallback> cbs;
                    ModeCallback cb = (ModeCallback) this.mModeWatchers.get(callback.asBinder());
                    if (cb == null) {
                        cb = new ModeCallback(callback, -1, flags, callingUid, callingPid);
                        this.mModeWatchers.put(callback.asBinder(), cb);
                    }
                    if (this.mAppOpsResource == null) {
                        this.mAppOpsResource = HwFrameworkFactory.getHwResource(14);
                    }
                    if (i2 != -1) {
                        cbs = (ArraySet) this.mOpModeWatchers.get(i2);
                        if (cbs == null) {
                            cbs = new ArraySet();
                            this.mOpModeWatchers.put(i2, cbs);
                        }
                        cbs.add(cb);
                    }
                    if (str != null) {
                        cbs = (ArraySet) this.mPackageModeWatchers.get(str);
                        if (cbs == null) {
                            cbs = new ArraySet();
                            this.mPackageModeWatchers.put(str, cbs);
                        }
                        cbs.add(cb);
                        if (this.mAppOpsResource != null) {
                            this.mAppOpsResource.acquire(Binder.getCallingUid(), str, 0, cbs.size());
                        }
                    }
                    evalAllForegroundOpsLocked();
                } catch (Throwable th3) {
                    th = th3;
                    throw th;
                }
            }
        }
    }

    public void stopWatchingMode(IAppOpsCallback callback) {
        if (callback != null) {
            synchronized (this) {
                ModeCallback cb = (ModeCallback) this.mModeWatchers.remove(callback.asBinder());
                if (cb != null) {
                    int i;
                    ArraySet<ModeCallback> cbs;
                    cb.unlinkToDeath();
                    for (i = this.mOpModeWatchers.size() - 1; i >= 0; i--) {
                        cbs = (ArraySet) this.mOpModeWatchers.valueAt(i);
                        cbs.remove(cb);
                        if (cbs.size() <= 0) {
                            this.mOpModeWatchers.removeAt(i);
                        }
                    }
                    for (i = this.mPackageModeWatchers.size() - 1; i >= 0; i--) {
                        cbs = (ArraySet) this.mPackageModeWatchers.valueAt(i);
                        cbs.remove(cb);
                        if (cbs.size() <= 0) {
                            this.mPackageModeWatchers.removeAt(i);
                        }
                    }
                }
                evalAllForegroundOpsLocked();
            }
        }
    }

    public IBinder getToken(IBinder clientToken) {
        ClientState cs;
        synchronized (this) {
            cs = (ClientState) this.mClients.get(clientToken);
            if (cs == null) {
                cs = new ClientState(clientToken);
                this.mClients.put(clientToken, cs);
            }
        }
        return cs;
    }

    public int checkOperation(int code, int uid, String packageName) {
        verifyIncomingUid(uid);
        verifyIncomingOp(code);
        String resolvedPackageName = resolvePackageName(uid, packageName);
        if (resolvedPackageName == null) {
            return 1;
        }
        synchronized (this) {
            if (isOpRestrictedLocked(uid, code, resolvedPackageName)) {
                return 1;
            }
            code = AppOpsManager.opToSwitch(code);
            UidState uidState = getUidStateLocked(uid, false);
            if (uidState == null || uidState.opModes == null || uidState.opModes.indexOfKey(code) < 0) {
                Op op = getOpLocked(code, uid, resolvedPackageName, false);
                int opToDefaultMode;
                if (op == null) {
                    opToDefaultMode = AppOpsManager.opToDefaultMode(code);
                    return opToDefaultMode;
                }
                opToDefaultMode = op.mode;
                return opToDefaultMode;
            }
            int i = uidState.opModes.get(code);
            return i;
        }
    }

    public int checkAudioOperation(int code, int usage, int uid, String packageName) {
        boolean suspended;
        try {
            suspended = isPackageSuspendedForUser(packageName, uid);
        } catch (IllegalArgumentException e) {
            suspended = false;
        }
        if (suspended) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Audio disabled for suspended package=");
            stringBuilder.append(packageName);
            stringBuilder.append(" for uid=");
            stringBuilder.append(uid);
            Slog.i(str, stringBuilder.toString());
            return 1;
        }
        synchronized (this) {
            int mode = checkRestrictionLocked(code, usage, uid, packageName);
            if (mode != 0) {
                return mode;
            }
            return checkOperation(code, uid, packageName);
        }
    }

    private boolean isPackageSuspendedForUser(String pkg, int uid) {
        try {
            return AppGlobals.getPackageManager().isPackageSuspendedForUser(pkg, UserHandle.getUserId(uid));
        } catch (RemoteException e) {
            throw new SecurityException("Could not talk to package manager service");
        }
    }

    private int checkRestrictionLocked(int code, int usage, int uid, String packageName) {
        SparseArray<Restriction> usageRestrictions = (SparseArray) this.mAudioRestrictions.get(code);
        if (usageRestrictions != null) {
            Restriction r = (Restriction) usageRestrictions.get(usage);
            if (!(r == null || r.exceptionPackages.contains(packageName))) {
                return r.mode;
            }
        }
        return 0;
    }

    public void setAudioRestriction(int code, int usage, int uid, int mode, String[] exceptionPackages) {
        enforceManageAppOpsModes(Binder.getCallingPid(), Binder.getCallingUid(), uid);
        verifyIncomingUid(uid);
        verifyIncomingOp(code);
        synchronized (this) {
            SparseArray<Restriction> usageRestrictions = (SparseArray) this.mAudioRestrictions.get(code);
            if (usageRestrictions == null) {
                usageRestrictions = new SparseArray();
                this.mAudioRestrictions.put(code, usageRestrictions);
            }
            usageRestrictions.remove(usage);
            if (mode != 0) {
                Restriction r = new Restriction();
                r.mode = mode;
                if (exceptionPackages != null) {
                    r.exceptionPackages = new ArraySet(N);
                    for (String pkg : exceptionPackages) {
                        if (pkg != null) {
                            r.exceptionPackages.add(pkg.trim());
                        }
                    }
                }
                usageRestrictions.put(usage, r);
            }
        }
        this.mHandler.sendMessage(PooledLambda.obtainMessage(-$$Lambda$AppOpsService$UKMH8n9xZqCOX59uFPylskhjBgo.INSTANCE, this, Integer.valueOf(code), Integer.valueOf(-2)));
    }

    public int checkPackage(int uid, String packageName) {
        Preconditions.checkNotNull(packageName);
        synchronized (this) {
            if (getOpsRawLocked(uid, packageName, true, true) != null) {
                return 0;
            }
            return 2;
        }
    }

    public int noteProxyOperation(int code, String proxyPackageName, int proxiedUid, String proxiedPackageName) {
        verifyIncomingOp(code);
        int proxyUid = Binder.getCallingUid();
        String resolveProxyPackageName = resolvePackageName(proxyUid, proxyPackageName);
        if (resolveProxyPackageName == null) {
            return 1;
        }
        int proxyMode = noteOperationUnchecked(code, proxyUid, resolveProxyPackageName, -1, null);
        if (proxyMode != 0 || Binder.getCallingUid() == proxiedUid) {
            return proxyMode;
        }
        String resolveProxiedPackageName = resolvePackageName(proxiedUid, proxiedPackageName);
        if (resolveProxiedPackageName == null) {
            return 1;
        }
        return noteOperationUnchecked(code, proxiedUid, resolveProxiedPackageName, proxyMode, resolveProxyPackageName);
    }

    public int noteOperation(int code, int uid, String packageName) {
        verifyIncomingUid(uid);
        verifyIncomingOp(code);
        String resolvedPackageName = resolvePackageName(uid, packageName);
        if (resolvedPackageName == null) {
            return 1;
        }
        return noteOperationUnchecked(code, uid, resolvedPackageName, 0, null);
    }

    private int noteOperationUnchecked(int code, int uid, String packageName, int proxyUid, String proxyPackageName) {
        synchronized (this) {
            Ops ops = getOpsRawLocked(uid, packageName, true, false);
            if (ops == null) {
                return 2;
            }
            Op op = getOpLocked(ops, code, true);
            if (isOpRestrictedLocked(uid, code, packageName)) {
                return 1;
            }
            UidState uidState = ops.uidState;
            if (op.duration == -1) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Noting op not finished: uid ");
                stringBuilder.append(uid);
                stringBuilder.append(" pkg ");
                stringBuilder.append(packageName);
                stringBuilder.append(" code ");
                stringBuilder.append(code);
                stringBuilder.append(" time=");
                stringBuilder.append(op.time[uidState.state]);
                stringBuilder.append(" duration=");
                stringBuilder.append(op.duration);
                Slog.w(str, stringBuilder.toString());
            }
            op.duration = 0;
            int switchCode = AppOpsManager.opToSwitch(code);
            if (uidState.opModes == null || uidState.opModes.indexOfKey(switchCode) < 0) {
                int mode = (switchCode != code ? getOpLocked(ops, switchCode, true) : op).getMode();
                if (mode != 0) {
                    op.rejectTime[uidState.state] = System.currentTimeMillis();
                    return mode;
                }
            }
            int uidMode = uidState.evalMode(uidState.opModes.get(switchCode));
            if (uidMode != 0) {
                op.rejectTime[uidState.state] = System.currentTimeMillis();
                return uidMode;
            }
            op.time[uidState.state] = System.currentTimeMillis();
            op.rejectTime[uidState.state] = 0;
            op.proxyUid = proxyUid;
            op.proxyPackageName = proxyPackageName;
            return 0;
        }
    }

    public void startWatchingActive(int[] ops, IAppOpsActiveCallback callback) {
        int watchedUid = -1;
        int callingUid = Binder.getCallingUid();
        int callingPid = Binder.getCallingPid();
        if (this.mContext.checkCallingOrSelfPermission("android.permission.WATCH_APPOPS") != 0) {
            watchedUid = callingUid;
        }
        int i = 0;
        if (ops != null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid op code in: ");
            stringBuilder.append(Arrays.toString(ops));
            Preconditions.checkArrayElementsInRange(ops, 0, 77, stringBuilder.toString());
        }
        if (callback != null) {
            synchronized (this) {
                SparseArray<ActiveCallback> callbacks = (SparseArray) this.mActiveWatchers.get(callback.asBinder());
                if (callbacks == null) {
                    callbacks = new SparseArray();
                    this.mActiveWatchers.put(callback.asBinder(), callbacks);
                }
                SparseArray<ActiveCallback> callbacks2 = callbacks;
                ActiveCallback activeCallback = new ActiveCallback(callback, watchedUid, callingUid, callingPid);
                int length = ops.length;
                while (i < length) {
                    callbacks2.put(ops[i], activeCallback);
                    i++;
                }
            }
        }
    }

    public void stopWatchingActive(IAppOpsActiveCallback callback) {
        if (callback != null) {
            synchronized (this) {
                SparseArray<ActiveCallback> activeCallbacks = (SparseArray) this.mActiveWatchers.remove(callback.asBinder());
                if (activeCallbacks == null) {
                    return;
                }
                int callbackCount = activeCallbacks.size();
                for (int i = 0; i < callbackCount; i++) {
                    if (i == 0) {
                        ((ActiveCallback) activeCallbacks.valueAt(i)).destroy();
                    }
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:48:0x00b8, code:
            return 0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int startOperation(IBinder token, int code, int uid, String packageName, boolean startIfModeDefault) {
        Throwable th;
        int i = code;
        int i2 = uid;
        verifyIncomingUid(i2);
        verifyIncomingOp(i);
        String resolvedPackageName = resolvePackageName(uid, packageName);
        if (resolvedPackageName == null) {
            return 1;
        }
        ClientState client = (ClientState) token;
        synchronized (this) {
            String str;
            try {
                Ops ops = getOpsRawLocked(i2, resolvedPackageName, true, false);
                if (ops == null) {
                    return 2;
                }
                Op op = getOpLocked(ops, i, true);
                if (isOpRestrictedLocked(i2, i, resolvedPackageName)) {
                    return 1;
                }
                int switchCode = AppOpsManager.opToSwitch(code);
                UidState uidState = ops.uidState;
                if (uidState.opModes == null || uidState.opModes.indexOfKey(switchCode) < 0) {
                    int mode = (switchCode != i ? getOpLocked(ops, switchCode, true) : op).getMode();
                    if (!(mode == 0 || (startIfModeDefault && mode == 3))) {
                        op.rejectTime[uidState.state] = System.currentTimeMillis();
                        return mode;
                    }
                }
                int uidMode = uidState.evalMode(uidState.opModes.get(switchCode));
                if (!(uidMode == 0 || (startIfModeDefault && uidMode == 3))) {
                    op.rejectTime[uidState.state] = System.currentTimeMillis();
                    return uidMode;
                }
                if (op.startNesting == 0) {
                    op.startRealtime = SystemClock.elapsedRealtime();
                    op.time[uidState.state] = System.currentTimeMillis();
                    op.rejectTime[uidState.state] = 0;
                    op.duration = -1;
                    scheduleOpActiveChangedIfNeededLocked(i, i2, packageName, true);
                } else {
                    str = packageName;
                }
                op.startNesting++;
                uidState.startNesting++;
                if (client.mStartedOps != null) {
                    client.mStartedOps.add(op);
                }
            } catch (Throwable th2) {
                th = th2;
                throw th;
            }
        }
    }

    /* JADX WARNING: Missing block: B:23:0x006c, code:
            return;
     */
    /* JADX WARNING: Missing block: B:34:0x00b3, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void finishOperation(IBinder token, int code, int uid, String packageName) {
        verifyIncomingUid(uid);
        verifyIncomingOp(code);
        String resolvedPackageName = resolvePackageName(uid, packageName);
        if (resolvedPackageName != null && (token instanceof ClientState)) {
            ClientState client = (ClientState) token;
            synchronized (this) {
                Op op = getOpLocked(code, uid, resolvedPackageName, true);
                if (op == null) {
                } else if (client.mStartedOps.remove(op)) {
                    finishOperationLocked(op, false);
                    if (op.startNesting <= 0) {
                        scheduleOpActiveChangedIfNeededLocked(code, uid, packageName, false);
                    }
                } else {
                    long identity = Binder.clearCallingIdentity();
                    try {
                        String str;
                        StringBuilder stringBuilder;
                        if (((PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class)).getPackageUid(resolvedPackageName, 0, UserHandle.getUserId(uid)) < 0) {
                            str = TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Finishing op=");
                            stringBuilder.append(AppOpsManager.opToName(code));
                            stringBuilder.append(" for non-existing package=");
                            stringBuilder.append(resolvedPackageName);
                            stringBuilder.append(" in uid=");
                            stringBuilder.append(uid);
                            Slog.i(str, stringBuilder.toString());
                        } else {
                            Binder.restoreCallingIdentity(identity);
                            str = TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Operation not started: uid=");
                            stringBuilder.append(op.uid);
                            stringBuilder.append(" pkg=");
                            stringBuilder.append(op.packageName);
                            stringBuilder.append(" op=");
                            stringBuilder.append(AppOpsManager.opToName(op.op));
                            Slog.wtf(str, stringBuilder.toString());
                        }
                    } finally {
                        Binder.restoreCallingIdentity(identity);
                    }
                }
            }
        }
    }

    private void scheduleOpActiveChangedIfNeededLocked(int code, int uid, String packageName, boolean active) {
        ArraySet<ActiveCallback> dispatchedCallbacks = null;
        int callbackListCount = this.mActiveWatchers.size();
        for (int i = 0; i < callbackListCount; i++) {
            ActiveCallback callback = (ActiveCallback) ((SparseArray) this.mActiveWatchers.valueAt(i)).get(code);
            if (callback != null && (callback.mWatchingUid < 0 || callback.mWatchingUid == uid)) {
                if (dispatchedCallbacks == null) {
                    dispatchedCallbacks = new ArraySet();
                }
                dispatchedCallbacks.add(callback);
            }
        }
        if (dispatchedCallbacks != null) {
            this.mHandler.sendMessage(PooledLambda.obtainMessage(-$$Lambda$AppOpsService$NC5g1JY4YR6y4VePru4TO7AKp8M.INSTANCE, this, dispatchedCallbacks, Integer.valueOf(code), Integer.valueOf(uid), packageName, Boolean.valueOf(active)));
        }
    }

    private void notifyOpActiveChanged(ArraySet<ActiveCallback> callbacks, int code, int uid, String packageName, boolean active) {
        long identity = Binder.clearCallingIdentity();
        try {
            int callbackCount = callbacks.size();
            for (int i = 0; i < callbackCount; i++) {
                try {
                    ((ActiveCallback) callbacks.valueAt(i)).mCallback.opActiveChanged(code, uid, packageName, active);
                } catch (RemoteException e) {
                }
            }
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public int permissionToOpCode(String permission) {
        if (permission == null) {
            return -1;
        }
        return AppOpsManager.permissionToOpCode(permission);
    }

    void finishOperationLocked(Op op, boolean finishNested) {
        UidState uidState;
        if (op.startNesting <= 1 || finishNested) {
            if (op.startNesting == 1 || finishNested) {
                op.duration = (int) (SystemClock.elapsedRealtime() - op.startRealtime);
                op.time[op.uidState.state] = System.currentTimeMillis();
            } else {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Finishing op nesting under-run: uid ");
                stringBuilder.append(op.uid);
                stringBuilder.append(" pkg ");
                stringBuilder.append(op.packageName);
                stringBuilder.append(" code ");
                stringBuilder.append(op.op);
                stringBuilder.append(" time=");
                stringBuilder.append(op.time);
                stringBuilder.append(" duration=");
                stringBuilder.append(op.duration);
                stringBuilder.append(" nesting=");
                stringBuilder.append(op.startNesting);
                Slog.w(str, stringBuilder.toString());
            }
            if (op.startNesting >= 1) {
                uidState = op.uidState;
                uidState.startNesting -= op.startNesting;
            }
            op.startNesting = 0;
            return;
        }
        op.startNesting--;
        uidState = op.uidState;
        uidState.startNesting--;
    }

    private void verifyIncomingUid(int uid) {
        if (uid != Binder.getCallingUid() && Binder.getCallingPid() != Process.myPid()) {
            this.mContext.enforcePermission("android.permission.UPDATE_APP_OPS_STATS", Binder.getCallingPid(), Binder.getCallingUid(), null);
        }
    }

    private void verifyIncomingOp(int op) {
        if (op < 0 || op >= 78) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Bad operation #");
            stringBuilder.append(op);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    private UidState getUidStateLocked(int uid, boolean edit) {
        UidState uidState = (UidState) this.mUidStates.get(uid);
        if (uidState == null) {
            if (!edit) {
                return null;
            }
            uidState = new UidState(uid);
            this.mUidStates.put(uid, uidState);
        } else if (uidState.pendingStateCommitTime != 0) {
            if (uidState.pendingStateCommitTime < this.mLastUptime) {
                commitUidPendingStateLocked(uidState);
            } else {
                this.mLastUptime = SystemClock.uptimeMillis();
                if (uidState.pendingStateCommitTime < this.mLastUptime) {
                    commitUidPendingStateLocked(uidState);
                }
            }
        }
        return uidState;
    }

    private void commitUidPendingStateLocked(UidState uidState) {
        UidState uidState2 = uidState;
        boolean z = true;
        boolean lastForeground = uidState2.state <= 2;
        boolean nowForeground = uidState2.pendingState <= 2;
        uidState2.state = uidState2.pendingState;
        uidState2.pendingStateCommitTime = 0;
        if (uidState2.hasForegroundWatchers && lastForeground != nowForeground) {
            int fgi = uidState2.foregroundOps.size() - 1;
            while (true) {
                int fgi2 = fgi;
                if (fgi2 >= 0) {
                    if (uidState2.foregroundOps.valueAt(fgi2)) {
                        int code = uidState2.foregroundOps.keyAt(fgi2);
                        ArraySet<ModeCallback> callbacks = (ArraySet) this.mOpModeWatchers.get(code);
                        if (callbacks != null) {
                            fgi = callbacks.size() - z;
                            while (true) {
                                int cbi = fgi;
                                if (cbi < 0) {
                                    break;
                                }
                                ModeCallback callback = (ModeCallback) callbacks.valueAt(cbi);
                                if ((callback.mFlags & z) != 0 && callback.isWatchingUid(uidState2.uid)) {
                                    int i = 4;
                                    boolean z2 = (uidState2.opModes == null || uidState2.opModes.get(code) != 4) ? false : z;
                                    boolean doAllPackages = z2;
                                    if (uidState2.pkgOps != null) {
                                        fgi = uidState2.pkgOps.size() - z;
                                        while (true) {
                                            int pkgi = fgi;
                                            if (pkgi < 0) {
                                                break;
                                            }
                                            int pkgi2;
                                            int i2;
                                            ModeCallback callback2;
                                            Op op = (Op) ((Ops) uidState2.pkgOps.valueAt(pkgi)).get(code);
                                            if (doAllPackages || (op != null && op.mode == i)) {
                                                pkgi2 = pkgi;
                                                i2 = 4;
                                                callback2 = callback;
                                                this.mHandler.sendMessage(PooledLambda.obtainMessage(-$$Lambda$AppOpsService$lxgFmOnGguOiLyfUZbyOpNBfTVw.INSTANCE, this, callback, Integer.valueOf(code), Integer.valueOf(uidState2.uid), (String) uidState2.pkgOps.keyAt(pkgi)));
                                            } else {
                                                pkgi2 = pkgi;
                                                i2 = i;
                                                callback2 = callback;
                                            }
                                            fgi = pkgi2 - 1;
                                            i = i2;
                                            callback = callback2;
                                        }
                                    }
                                }
                                fgi = cbi - 1;
                                z = true;
                            }
                        }
                    }
                    fgi = fgi2 - 1;
                    z = true;
                } else {
                    return;
                }
            }
        }
    }

    private Ops getOpsRawLocked(int uid, String packageName, boolean edit, boolean uidMismatchExpected) {
        UidState uidState = getUidStateLocked(uid, edit);
        if (uidState == null) {
            return null;
        }
        if (uidState.pkgOps == null) {
            if (!edit) {
                return null;
            }
            uidState.pkgOps = new ArrayMap();
        }
        Ops ops = (Ops) uidState.pkgOps.get(packageName);
        if (ops == null) {
            if (!edit) {
                return null;
            }
            boolean isPrivileged = false;
            if (uid != 0) {
                long ident = Binder.clearCallingIdentity();
                int pkgUid = -1;
                try {
                    ApplicationInfo appInfo = ActivityThread.getPackageManager().getApplicationInfo(packageName, 268435456, UserHandle.getUserId(uid));
                    if (appInfo != null) {
                        pkgUid = appInfo.uid;
                        isPrivileged = (appInfo.privateFlags & 8) != 0;
                    } else {
                        pkgUid = resolveUid(packageName);
                        if (pkgUid >= 0) {
                            isPrivileged = false;
                        }
                    }
                } catch (RemoteException e) {
                    Slog.w(TAG, "Could not contact PackageManager", e);
                } catch (Throwable th) {
                    Binder.restoreCallingIdentity(ident);
                }
                if (pkgUid != uid) {
                    if (!uidMismatchExpected) {
                        new RuntimeException("here").fillInStackTrace();
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Bad call: specified package ");
                        stringBuilder.append(packageName);
                        stringBuilder.append(" under uid ");
                        stringBuilder.append(uid);
                        stringBuilder.append(" but it is really ");
                        stringBuilder.append(pkgUid);
                        Slog.w(str, stringBuilder.toString());
                    }
                    Binder.restoreCallingIdentity(ident);
                    return null;
                }
                Binder.restoreCallingIdentity(ident);
            }
            ops = new Ops(packageName, uidState, isPrivileged);
            uidState.pkgOps.put(packageName, ops);
        }
        return ops;
    }

    private void scheduleWriteLocked() {
        if (!this.mWriteScheduled) {
            this.mWriteScheduled = true;
            this.mHandler.postDelayed(this.mWriteRunner, 1800000);
        }
    }

    private void scheduleFastWriteLocked() {
        if (!this.mFastWriteScheduled) {
            this.mWriteScheduled = true;
            this.mFastWriteScheduled = true;
            this.mHandler.removeCallbacks(this.mWriteRunner);
            this.mHandler.postDelayed(this.mWriteRunner, JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
        }
    }

    private Op getOpLocked(int code, int uid, String packageName, boolean edit) {
        Ops ops = getOpsRawLocked(uid, packageName, edit, null);
        if (ops == null) {
            return null;
        }
        return getOpLocked(ops, code, edit);
    }

    private Op getOpLocked(Ops ops, int code, boolean edit) {
        Op op = (Op) ops.get(code);
        if (op == null) {
            if (!edit) {
                return null;
            }
            op = new Op(ops.uidState, ops.packageName, code);
            ops.put(code, op);
        }
        if (edit) {
            scheduleWriteLocked();
        }
        return op;
    }

    private boolean isOpRestrictedLocked(int uid, int code, String packageName) {
        int userHandle = UserHandle.getUserId(uid);
        int restrictionSetCount = this.mOpUserRestrictions.size();
        int i = 0;
        while (i < restrictionSetCount) {
            ClientRestrictionState restrictionState = (ClientRestrictionState) this.mOpUserRestrictions.valueAt(i);
            if (restrictionState == null || !restrictionState.hasRestriction(code, packageName, userHandle)) {
                i++;
            } else {
                if (AppOpsManager.opAllowSystemBypassRestriction(code)) {
                    synchronized (this) {
                        Ops ops = getOpsRawLocked(uid, packageName, true, false);
                        if (ops == null || !ops.isPrivileged) {
                        } else {
                            return false;
                        }
                    }
                }
                return true;
            }
        }
        return false;
    }

    /* JADX WARNING: Removed duplicated region for block: B:45:0x00a3 A:{SYNTHETIC, Splitter: B:45:0x00a3} */
    /* JADX WARNING: Removed duplicated region for block: B:16:0x002e A:{Catch:{ IllegalStateException -> 0x015b, NullPointerException -> 0x0138, NumberFormatException -> 0x0115, XmlPullParserException -> 0x00f3, IOException -> 0x00d1, IndexOutOfBoundsException -> 0x00af }} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void readState() {
        StringBuilder stringBuilder;
        int oldVersion = -1;
        synchronized (this.mFile) {
            synchronized (this) {
                try {
                    FileInputStream stream = this.mFile.openRead();
                    this.mUidStates.clear();
                    String versionString;
                    try {
                        int type;
                        XmlPullParser parser = Xml.newPullParser();
                        parser.setInput(stream, StandardCharsets.UTF_8.name());
                        while (true) {
                            int next = parser.next();
                            type = next;
                            if (next == 2 || type == 1) {
                                if (type != 2) {
                                    versionString = parser.getAttributeValue(null, "v");
                                    if (versionString != null) {
                                        oldVersion = Integer.parseInt(versionString);
                                    }
                                    int outerDepth = parser.getDepth();
                                    while (true) {
                                        int next2 = parser.next();
                                        type = next2;
                                        if (next2 != 1 && (type != 3 || parser.getDepth() > outerDepth)) {
                                            if (type != 3) {
                                                if (type != 4) {
                                                    String tagName = parser.getName();
                                                    if (tagName.equals(AbsLocationManagerService.DEL_PKG)) {
                                                        readPackage(parser);
                                                    } else if (tagName.equals("uid")) {
                                                        readUidOps(parser);
                                                    } else {
                                                        String str = TAG;
                                                        StringBuilder stringBuilder2 = new StringBuilder();
                                                        stringBuilder2.append("Unknown element under <app-ops>: ");
                                                        stringBuilder2.append(parser.getName());
                                                        Slog.w(str, stringBuilder2.toString());
                                                        XmlUtils.skipCurrentTag(parser);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    if (!true) {
                                        this.mUidStates.clear();
                                    }
                                    try {
                                        stream.close();
                                    } catch (IOException e) {
                                    }
                                } else {
                                    throw new IllegalStateException("no start tag found");
                                }
                            }
                        }
                        if (type != 2) {
                        }
                    } catch (IllegalStateException e2) {
                        versionString = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Failed parsing ");
                        stringBuilder.append(e2);
                        Slog.w(versionString, stringBuilder.toString());
                        if (null == null) {
                            this.mUidStates.clear();
                        }
                        stream.close();
                    } catch (NullPointerException e3) {
                        versionString = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Failed parsing ");
                        stringBuilder.append(e3);
                        Slog.w(versionString, stringBuilder.toString());
                        if (null == null) {
                            this.mUidStates.clear();
                        }
                        stream.close();
                    } catch (NumberFormatException e4) {
                        versionString = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Failed parsing ");
                        stringBuilder.append(e4);
                        Slog.w(versionString, stringBuilder.toString());
                        if (null == null) {
                            this.mUidStates.clear();
                        }
                        stream.close();
                    } catch (XmlPullParserException e5) {
                        versionString = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Failed parsing ");
                        stringBuilder.append(e5);
                        Slog.w(versionString, stringBuilder.toString());
                        if (null == null) {
                            this.mUidStates.clear();
                        }
                        stream.close();
                    } catch (IOException e6) {
                        versionString = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Failed parsing ");
                        stringBuilder.append(e6);
                        Slog.w(versionString, stringBuilder.toString());
                        if (null == null) {
                            this.mUidStates.clear();
                        }
                        stream.close();
                    } catch (IndexOutOfBoundsException e7) {
                        try {
                            versionString = TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Failed parsing ");
                            stringBuilder.append(e7);
                            Slog.w(versionString, stringBuilder.toString());
                            if (null == null) {
                                this.mUidStates.clear();
                            }
                            stream.close();
                        } catch (Throwable th) {
                            if (null == null) {
                                this.mUidStates.clear();
                            }
                            try {
                                stream.close();
                            } catch (IOException e8) {
                            }
                        }
                    }
                } catch (FileNotFoundException e9) {
                    String str2 = TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("No existing app ops ");
                    stringBuilder3.append(this.mFile.getBaseFile());
                    stringBuilder3.append("; starting empty");
                    Slog.i(str2, stringBuilder3.toString());
                    return;
                }
            }
        }
        synchronized (this) {
            upgradeLocked(oldVersion);
        }
    }

    private void upgradeRunAnyInBackgroundLocked() {
        for (int i = 0; i < this.mUidStates.size(); i++) {
            UidState uidState = (UidState) this.mUidStates.valueAt(i);
            if (uidState != null) {
                int idx;
                if (uidState.opModes != null) {
                    idx = uidState.opModes.indexOfKey(63);
                    if (idx >= 0) {
                        uidState.opModes.put(70, uidState.opModes.valueAt(idx));
                    }
                }
                if (uidState.pkgOps != null) {
                    boolean changed = false;
                    for (idx = 0; idx < uidState.pkgOps.size(); idx++) {
                        Ops ops = (Ops) uidState.pkgOps.valueAt(idx);
                        if (ops != null) {
                            Op op = (Op) ops.get(63);
                            if (!(op == null || op.mode == AppOpsManager.opToDefaultMode(op.op))) {
                                Op copy = new Op(op.uidState, op.packageName, 70);
                                copy.mode = op.mode;
                                ops.put(70, copy);
                                changed = true;
                            }
                        }
                    }
                    if (changed) {
                        uidState.evalForegroundOps(this.mOpModeWatchers);
                    }
                }
            }
        }
    }

    private void upgradeLocked(int oldVersion) {
        if (oldVersion < 1) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Upgrading app-ops xml from version ");
            stringBuilder.append(oldVersion);
            stringBuilder.append(" to ");
            stringBuilder.append(1);
            Slog.d(str, stringBuilder.toString());
            if (oldVersion == -1) {
                upgradeRunAnyInBackgroundLocked();
            }
            scheduleFastWriteLocked();
        }
    }

    void readUidOps(XmlPullParser parser) throws NumberFormatException, XmlPullParserException, IOException {
        int uid = Integer.parseInt(parser.getAttributeValue(null, "n"));
        int outerDepth = parser.getDepth();
        while (true) {
            int next = parser.next();
            int type = next;
            if (next == 1) {
                return;
            }
            if (type == 3 && parser.getDepth() <= outerDepth) {
                return;
            }
            if (type != 3) {
                if (type != 4) {
                    if (parser.getName().equals("op")) {
                        int code = Integer.parseInt(parser.getAttributeValue(null, "n"));
                        int mode = Integer.parseInt(parser.getAttributeValue(null, "m"));
                        UidState uidState = getUidStateLocked(uid, true);
                        if (uidState.opModes == null) {
                            uidState.opModes = new SparseIntArray();
                        }
                        uidState.opModes.put(code, mode);
                    } else {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Unknown element under <uid-ops>: ");
                        stringBuilder.append(parser.getName());
                        Slog.w(str, stringBuilder.toString());
                        XmlUtils.skipCurrentTag(parser);
                    }
                }
            }
        }
    }

    void readPackage(XmlPullParser parser) throws NumberFormatException, XmlPullParserException, IOException {
        String pkgName = parser.getAttributeValue(null, "n");
        int outerDepth = parser.getDepth();
        while (true) {
            int next = parser.next();
            int type = next;
            if (next == 1) {
                return;
            }
            if (type == 3 && parser.getDepth() <= outerDepth) {
                return;
            }
            if (type != 3) {
                if (type != 4) {
                    if (parser.getName().equals("uid")) {
                        readUid(parser, pkgName);
                    } else {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Unknown element under <pkg>: ");
                        stringBuilder.append(parser.getName());
                        Slog.w(str, stringBuilder.toString());
                        XmlUtils.skipCurrentTag(parser);
                    }
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:104:0x020a, code:
            r24 = r0;
            r17 = 3;
            r20 = 4;
     */
    /* JADX WARNING: Missing block: B:114:0x0280, code:
            r24 = r0;
     */
    /* JADX WARNING: Missing block: B:115:0x0282, code:
            r9 = r9 - 1;
            r11 = r17;
            r13 = r20;
            r0 = r24;
            r10 = 1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void readUid(XmlPullParser parser, String pkgName) throws NumberFormatException, XmlPullParserException, IOException {
        XmlPullParser xmlPullParser = parser;
        String str = pkgName;
        String str2 = null;
        int uid = Integer.parseInt(xmlPullParser.getAttributeValue(null, "n"));
        String isPrivilegedString = xmlPullParser.getAttributeValue(null, "p");
        boolean isPrivileged = false;
        int i = 1;
        if (isPrivilegedString == null) {
            try {
                if (ActivityThread.getPackageManager() != null) {
                    ApplicationInfo appInfo = ActivityThread.getPackageManager().getApplicationInfo(str, 0, UserHandle.getUserId(uid));
                    if (appInfo != null) {
                        isPrivileged = (appInfo.privateFlags & 8) != 0;
                    }
                } else {
                    return;
                }
            } catch (RemoteException e) {
                Slog.w(TAG, "Could not contact PackageManager", e);
            }
        } else {
            isPrivileged = Boolean.parseBoolean(isPrivilegedString);
        }
        int outerDepth = parser.getDepth();
        while (true) {
            int next = parser.next();
            int type = next;
            int outerDepth2;
            if (next == i) {
            } else if (type != 3 || parser.getDepth() > outerDepth) {
                if (type == 3) {
                    outerDepth2 = outerDepth;
                } else if (type == 4) {
                    outerDepth2 = outerDepth;
                } else if (parser.getName().equals("op")) {
                    UidState uidState = getUidStateLocked(uid, i);
                    if (uidState.pkgOps == null) {
                        uidState.pkgOps = new ArrayMap();
                    }
                    Op op = new Op(uidState, str, Integer.parseInt(xmlPullParser.getAttributeValue(str2, "n")));
                    int i2 = parser.getAttributeCount() - i;
                    while (i2 >= 0) {
                        int i3;
                        str2 = xmlPullParser.getAttributeName(i2);
                        String value = xmlPullParser.getAttributeValue(i2);
                        switch (str2.hashCode()) {
                            case 100:
                                if (str2.equals("d")) {
                                    i3 = i;
                                    break;
                                }
                            case 109:
                                if (str2.equals("m")) {
                                    i3 = 0;
                                    break;
                                }
                            case 114:
                                if (str2.equals("r")) {
                                    i3 = 17;
                                    break;
                                }
                            case HdmiCecKeycode.CEC_KEYCODE_F4_YELLOW /*116*/:
                                if (str2.equals("t")) {
                                    i3 = 16;
                                    break;
                                }
                            case 3584:
                                if (str2.equals("pp")) {
                                    i3 = 3;
                                    break;
                                }
                            case 3589:
                                if (str2.equals("pu")) {
                                    i3 = 2;
                                    break;
                                }
                            case 3632:
                                if (str2.equals("rb")) {
                                    i3 = 14;
                                    break;
                                }
                            case 3633:
                                if (str2.equals("rc")) {
                                    i3 = 15;
                                    break;
                                }
                            case 3636:
                                if (str2.equals("rf")) {
                                    i3 = 13;
                                    break;
                                }
                            case 3646:
                                if (str2.equals("rp")) {
                                    i3 = 10;
                                    break;
                                }
                            case 3650:
                                if (str2.equals("rt")) {
                                    i3 = 11;
                                    break;
                                }
                            case 3694:
                                if (str2.equals("tb")) {
                                    i3 = 8;
                                    break;
                                }
                            case 3695:
                                if (str2.equals("tc")) {
                                    i3 = 9;
                                    break;
                                }
                            case 3698:
                                if (str2.equals("tf")) {
                                    i3 = 7;
                                    break;
                                }
                            case 3708:
                                if (str2.equals("tp")) {
                                    i3 = 4;
                                    break;
                                }
                            case 3712:
                                if (str2.equals("tt")) {
                                    i3 = 5;
                                    break;
                                }
                            case 112831:
                                if (str2.equals("rfs")) {
                                    i3 = 12;
                                    break;
                                }
                            case 114753:
                                if (str2.equals("tfs")) {
                                    i3 = 6;
                                    break;
                                }
                            default:
                                i3 = -1;
                                break;
                        }
                        int i4;
                        int i5;
                        switch (i3) {
                            case 0:
                                i4 = 3;
                                i5 = 4;
                                op.mode = Integer.parseInt(value);
                                break;
                            case 1:
                                i4 = 3;
                                i5 = 4;
                                op.duration = Integer.parseInt(value);
                                break;
                            case 2:
                                i4 = 3;
                                i5 = 4;
                                op.proxyUid = Integer.parseInt(value);
                                break;
                            case 3:
                                i4 = 3;
                                i5 = 4;
                                op.proxyPackageName = value;
                                break;
                            case 4:
                                i4 = 3;
                                i5 = 4;
                                op.time[0] = Long.parseLong(value);
                                break;
                            case 5:
                                i4 = 3;
                                i5 = 4;
                                op.time[i] = Long.parseLong(value);
                                break;
                            case 6:
                                i4 = 3;
                                i5 = 4;
                                op.time[2] = Long.parseLong(value);
                                break;
                            case 7:
                                i5 = 4;
                                i4 = 3;
                                op.time[3] = Long.parseLong(value);
                                break;
                            case 8:
                                i5 = 4;
                                op.time[4] = Long.parseLong(value);
                                outerDepth2 = outerDepth;
                                i4 = 3;
                                break;
                            case 9:
                                op.time[5] = Long.parseLong(value);
                                break;
                            case 10:
                                op.rejectTime[0] = Long.parseLong(value);
                                break;
                            case 11:
                                op.rejectTime[i] = Long.parseLong(value);
                                break;
                            case 12:
                                op.rejectTime[2] = Long.parseLong(value);
                                break;
                            case 13:
                                op.rejectTime[3] = Long.parseLong(value);
                                break;
                            case 14:
                                op.rejectTime[4] = Long.parseLong(value);
                                break;
                            case 15:
                                op.rejectTime[5] = Long.parseLong(value);
                                break;
                            case 16:
                                op.time[i] = Long.parseLong(value);
                                break;
                            case 17:
                                op.rejectTime[i] = Long.parseLong(value);
                                break;
                            default:
                                i4 = 3;
                                i5 = 4;
                                String str3 = TAG;
                                StringBuilder stringBuilder = new StringBuilder();
                                outerDepth2 = outerDepth;
                                stringBuilder.append("Unknown attribute in 'op' tag: ");
                                stringBuilder.append(str2);
                                Slog.w(str3, stringBuilder.toString());
                                break;
                        }
                    }
                    outerDepth2 = outerDepth;
                    outerDepth = (Ops) uidState.pkgOps.get(str);
                    if (outerDepth == null) {
                        outerDepth = new Ops(str, uidState, isPrivileged);
                        uidState.pkgOps.put(str, outerDepth);
                    }
                    outerDepth.put(op.op, op);
                } else {
                    outerDepth2 = outerDepth;
                    String str4 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Unknown element under <pkg>: ");
                    stringBuilder2.append(parser.getName());
                    Slog.w(str4, stringBuilder2.toString());
                    XmlUtils.skipCurrentTag(parser);
                }
                outerDepth = outerDepth2;
                str2 = null;
                i = 1;
            } else {
                outerDepth2 = outerDepth;
            }
        }
        UidState uidState2 = getUidStateLocked(uid, false);
        if (uidState2 != null) {
            uidState2.evalForegroundOps(this.mOpModeWatchers);
        }
    }

    /* JADX WARNING: Missing block: B:56:?, code:
            r10 = r9.getOps();
            r11 = r12;
     */
    /* JADX WARNING: Missing block: B:58:0x0140, code:
            if (r11 >= r10.size()) goto L_0x021d;
     */
    /* JADX WARNING: Missing block: B:59:0x0142, code:
            r13 = (android.app.AppOpsManager.OpEntry) r10.get(r11);
            r5.startTag(r0, "op");
            r5.attribute(r0, "n", java.lang.Integer.toString(r13.getOp()));
     */
    /* JADX WARNING: Missing block: B:60:0x0168, code:
            if (r13.getMode() == android.app.AppOpsManager.opToDefaultMode(r13.getOp())) goto L_0x0178;
     */
    /* JADX WARNING: Missing block: B:62:?, code:
            r5.attribute(r0, "m", java.lang.Integer.toString(r13.getMode()));
     */
    /* JADX WARNING: Missing block: B:63:0x0178, code:
            r14 = r12;
     */
    /* JADX WARNING: Missing block: B:65:0x017a, code:
            if (r14 >= 6) goto L_0x01d0;
     */
    /* JADX WARNING: Missing block: B:67:?, code:
            r19 = r13;
            r12 = r13.getLastTimeFor(r14);
     */
    /* JADX WARNING: Missing block: B:68:0x018a, code:
            if (r12 == 0) goto L_0x019b;
     */
    /* JADX WARNING: Missing block: B:70:0x0194, code:
            r20 = r4;
     */
    /* JADX WARNING: Missing block: B:72:?, code:
            r5.attribute(null, UID_STATE_TIME_ATTRS[r14], java.lang.Long.toString(r12));
     */
    /* JADX WARNING: Missing block: B:73:0x019b, code:
            r20 = r4;
     */
    /* JADX WARNING: Missing block: B:74:0x019d, code:
            r0 = r19;
            r24 = r8;
            r23 = r9;
            r8 = r0.getLastRejectTimeFor(r14);
     */
    /* JADX WARNING: Missing block: B:75:0x01af, code:
            if (r8 == 0) goto L_0x01c0;
     */
    /* JADX WARNING: Missing block: B:76:0x01b1, code:
            r25 = r6;
            r5.attribute(0, UID_STATE_REJECT_ATTRS[r14], java.lang.Long.toString(r8));
     */
    /* JADX WARNING: Missing block: B:77:0x01c0, code:
            r25 = r6;
     */
    /* JADX WARNING: Missing block: B:78:0x01c2, code:
            r14 = r14 + 1;
            r13 = r0;
            r4 = r20;
            r9 = r23;
            r8 = r24;
            r6 = r25;
     */
    /* JADX WARNING: Missing block: B:79:0x01d0, code:
            r20 = r4;
            r25 = r6;
            r24 = r8;
            r23 = r9;
            r0 = r13;
            r4 = r0.getDuration();
     */
    /* JADX WARNING: Missing block: B:80:0x01dd, code:
            if (r4 == null) goto L_0x01e9;
     */
    /* JADX WARNING: Missing block: B:81:0x01df, code:
            r5.attribute(null, "d", java.lang.Integer.toString(r4));
     */
    /* JADX WARNING: Missing block: B:82:0x01e9, code:
            r6 = r0.getProxyUid();
     */
    /* JADX WARNING: Missing block: B:83:0x01ee, code:
            if (r6 == -1) goto L_0x01fb;
     */
    /* JADX WARNING: Missing block: B:84:0x01f0, code:
            r5.attribute(null, "pu", java.lang.Integer.toString(r6));
     */
    /* JADX WARNING: Missing block: B:85:0x01fb, code:
            r8 = r0.getProxyPackageName();
     */
    /* JADX WARNING: Missing block: B:86:0x01ff, code:
            if (r8 == null) goto L_0x0208;
     */
    /* JADX WARNING: Missing block: B:87:0x0201, code:
            r5.attribute(null, "pp", r8);
     */
    /* JADX WARNING: Missing block: B:88:0x0208, code:
            r5.endTag(null, "op");
            r11 = r11 + 1;
            r4 = r20;
            r9 = r23;
            r8 = r24;
            r6 = r25;
            r0 = null;
            r12 = false;
     */
    /* JADX WARNING: Missing block: B:89:0x021d, code:
            r20 = r4;
            r25 = r6;
            r24 = r8;
            r23 = r9;
            r0 = "uid";
            r5.endTag(null, r0);
     */
    /* JADX WARNING: Missing block: B:90:0x022c, code:
            r8 = r24;
     */
    /* JADX WARNING: Missing block: B:106:0x026a, code:
            r0 = e;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void writeState() {
        IOException e;
        Throwable th;
        String str;
        PackageOps packageOps;
        synchronized (this.mFile) {
            try {
                List<PackageOps> list;
                FileOutputStream stream = this.mFile.startWrite();
                String str2 = null;
                List<PackageOps> allOps = getPackagesForOps(null);
                XmlSerializer out = new FastXmlSerializer();
                out.setOutput(stream, StandardCharsets.UTF_8.name());
                out.startDocument(null, Boolean.valueOf(true));
                out.startTag(null, "app-ops");
                out.attribute(null, "v", String.valueOf(1));
                int uidStateCount = this.mUidStates.size();
                int i = 0;
                while (i < uidStateCount) {
                    try {
                        UidState uidState = (UidState) this.mUidStates.valueAt(i);
                        if (!(uidState == null || uidState.opModes == null || uidState.opModes.size() <= 0)) {
                            out.startTag(null, "uid");
                            out.attribute(null, "n", Integer.toString(uidState.uid));
                            SparseIntArray uidOpModes = uidState.opModes;
                            int opCount = uidOpModes.size();
                            for (int j = 0; j < opCount; j++) {
                                int op = uidOpModes.keyAt(j);
                                int mode = uidOpModes.valueAt(j);
                                out.startTag(null, "op");
                                out.attribute(null, "n", Integer.toString(op));
                                out.attribute(null, "m", Integer.toString(mode));
                                out.endTag(null, "op");
                            }
                            out.endTag(null, "uid");
                        }
                        i++;
                    } catch (IOException e2) {
                        e = e2;
                        list = allOps;
                    }
                }
                int uidStateCount2;
                if (allOps != null) {
                    String lastPkg = null;
                    while (0 < allOps.size()) {
                        try {
                            PackageOps pkg = (PackageOps) allOps.get(0);
                            if (pkg == null) {
                                list = allOps;
                                uidStateCount2 = uidStateCount;
                            } else if (pkg.getPackageName() == null) {
                                list = allOps;
                                uidStateCount2 = uidStateCount;
                            } else {
                                if (!pkg.getPackageName().equals(lastPkg)) {
                                    if (lastPkg != null) {
                                        out.endTag(str2, AbsLocationManagerService.DEL_PKG);
                                    }
                                    lastPkg = pkg.getPackageName();
                                    out.startTag(str2, AbsLocationManagerService.DEL_PKG);
                                    out.attribute(str2, "n", lastPkg);
                                }
                                out.startTag(str2, "uid");
                                out.attribute(str2, "n", Integer.toString(pkg.getUid()));
                                synchronized (this) {
                                    try {
                                        Ops ops = getOpsRawLocked(pkg.getUid(), pkg.getPackageName(), false, false);
                                        boolean z;
                                        if (ops != null) {
                                            try {
                                                out.attribute(str2, "p", Boolean.toString(ops.isPrivileged));
                                                z = false;
                                            } catch (Throwable th2) {
                                                th = th2;
                                                list = allOps;
                                                uidStateCount2 = uidStateCount;
                                                str = lastPkg;
                                                packageOps = pkg;
                                            }
                                        } else {
                                            z = false;
                                            out.attribute(str2, "p", Boolean.toString(false));
                                        }
                                    } catch (Throwable th3) {
                                        th = th3;
                                        list = allOps;
                                        uidStateCount2 = uidStateCount;
                                        str = lastPkg;
                                        packageOps = pkg;
                                    }
                                }
                            }
                            int i2 = 0 + 1;
                            allOps = list;
                            uidStateCount = uidStateCount2;
                        } catch (IOException e3) {
                            e = e3;
                            list = allOps;
                            Slog.w(TAG, "Failed to write state, restoring backup.", e);
                            this.mFile.failWrite(stream);
                            return;
                        }
                    }
                    uidStateCount2 = uidStateCount;
                    if (lastPkg != null) {
                        out.endTag(null, AbsLocationManagerService.DEL_PKG);
                    }
                } else {
                    uidStateCount2 = uidStateCount;
                }
                out.endTag(null, "app-ops");
                out.endDocument();
                this.mFile.finishWrite(stream);
            } catch (IOException e4) {
                String str3 = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to write state: ");
                stringBuilder.append(e4);
                Slog.w(str3, stringBuilder.toString());
                return;
            }
        }
        return;
        while (true) {
            try {
                break;
            } catch (Throwable th4) {
                th = th4;
            }
        }
        throw th;
    }

    public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
        new Shell(this, this).exec(this, in, out, err, args, callback, resultReceiver);
    }

    static void dumpCommandHelp(PrintWriter pw) {
        pw.println("AppOps service (appops) commands:");
        pw.println("  help");
        pw.println("    Print this help text.");
        pw.println("  start [--user <USER_ID>] <PACKAGE | UID> <OP> ");
        pw.println("    Starts a given operation for a particular application.");
        pw.println("  stop [--user <USER_ID>] <PACKAGE | UID> <OP> ");
        pw.println("    Stops a given operation for a particular application.");
        pw.println("  set [--user <USER_ID>] <PACKAGE | UID> <OP> <MODE>");
        pw.println("    Set the mode for a particular application and operation.");
        pw.println("  get [--user <USER_ID>] <PACKAGE | UID> [<OP>]");
        pw.println("    Return the mode for a particular application and optional operation.");
        pw.println("  query-op [--user <USER_ID>] <OP> [<MODE>]");
        pw.println("    Print all packages that currently have the given op in the given mode.");
        pw.println("  reset [--user <USER_ID>] [<PACKAGE>]");
        pw.println("    Reset the given application or all applications to default modes.");
        pw.println("  write-settings");
        pw.println("    Immediately write pending changes to storage.");
        pw.println("  read-settings");
        pw.println("    Read the last written settings, replacing current state in RAM.");
        pw.println("  options:");
        pw.println("    <PACKAGE> an Android package name.");
        pw.println("    <OP>      an AppOps operation.");
        pw.println("    <MODE>    one of allow, ignore, deny, or default");
        pw.println("    <USER_ID> the user id under which the package is installed. If --user is not");
        pw.println("              specified, the current user is assumed.");
    }

    /* JADX WARNING: Removed duplicated region for block: B:198:0x01df A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:122:0x01d8 A:{Catch:{ all -> 0x00d5, RemoteException -> 0x032b }} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    static int onShellCommand(Shell shell, String cmd) {
        Shell shell2 = shell;
        String str = cmd;
        if (str == null) {
            return shell.handleDefaultCommands(cmd);
        }
        StringBuilder stringBuilder;
        PrintWriter pw = shell.getOutPrintWriter();
        PrintWriter err = shell.getErrPrintWriter();
        long now;
        try {
            boolean z;
            switch (cmd.hashCode()) {
                case -1703718319:
                    if (str.equals("write-settings")) {
                        z = true;
                        break;
                    }
                case -1166702330:
                    if (str.equals("query-op")) {
                        z = true;
                        break;
                    }
                case 102230:
                    if (str.equals("get")) {
                        z = true;
                        break;
                    }
                case 113762:
                    if (str.equals("set")) {
                        z = false;
                        break;
                    }
                case 3540994:
                    if (str.equals("stop")) {
                        z = true;
                        break;
                    }
                case 108404047:
                    if (str.equals("reset")) {
                        z = true;
                        break;
                    }
                case 109757538:
                    if (str.equals("start")) {
                        z = true;
                        break;
                    }
                case 2085703290:
                    if (str.equals("read-settings")) {
                        z = true;
                        break;
                    }
                default:
                    z = true;
                    break;
            }
            int res;
            int mode;
            List<PackageOps> ops;
            List<OpEntry> entries;
            int j;
            OpEntry ent;
            switch (z) {
                case false:
                    res = shell2.parseUserPackageOp(true, err);
                    if (res < 0) {
                        return res;
                    }
                    String modeStr = shell.getNextArg();
                    if (modeStr == null) {
                        err.println("Error: Mode not specified.");
                        return -1;
                    }
                    mode = Shell.strModeToMode(modeStr, err);
                    if (mode < 0) {
                        return -1;
                    }
                    if (shell2.packageName != null) {
                        shell2.mInterface.setMode(shell2.op, shell2.packageUid, shell2.packageName, mode);
                    } else {
                        shell2.mInterface.setUidMode(shell2.op, shell2.nonpackageUid, mode);
                    }
                    return 0;
                case true:
                    res = shell2.parseUserPackageOp(false, err);
                    if (res < 0) {
                        return res;
                    }
                    int i;
                    int[] iArr = null;
                    IAppOpsService iAppOpsService;
                    if (shell2.packageName != null) {
                        iAppOpsService = shell2.mInterface;
                        i = shell2.packageUid;
                        String str2 = shell2.packageName;
                        if (shell2.op != -1) {
                            iArr = new int[]{shell2.op};
                        }
                        ops = iAppOpsService.getOpsForPackage(i, str2, iArr);
                    } else {
                        iAppOpsService = shell2.mInterface;
                        i = shell2.nonpackageUid;
                        if (shell2.op != -1) {
                            iArr = new int[]{shell2.op};
                        }
                        ops = iAppOpsService.getUidOps(i, iArr);
                    }
                    if (ops == null || ops.size() <= 0) {
                        pw.println("No operations.");
                        if (shell2.op > -1 && shell2.op < 78) {
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Default mode: ");
                            stringBuilder2.append(AppOpsManager.modeToName(AppOpsManager.opToDefaultMode(shell2.op)));
                            pw.println(stringBuilder2.toString());
                        }
                        return 0;
                    }
                    now = System.currentTimeMillis();
                    for (i = 0; i < ops.size(); i++) {
                        entries = ((PackageOps) ops.get(i)).getOps();
                        for (j = 0; j < entries.size(); j++) {
                            ent = (OpEntry) entries.get(j);
                            pw.print(AppOpsManager.opToName(ent.getOp()));
                            pw.print(": ");
                            pw.print(AppOpsManager.modeToName(ent.getMode()));
                            if (ent.getTime() != 0) {
                                pw.print("; time=");
                                TimeUtils.formatDuration(now - ent.getTime(), pw);
                                pw.print(" ago");
                            }
                            if (ent.getRejectTime() != 0) {
                                pw.print("; rejectTime=");
                                TimeUtils.formatDuration(now - ent.getRejectTime(), pw);
                                pw.print(" ago");
                            }
                            if (ent.getDuration() == -1) {
                                pw.print(" (running)");
                            } else if (ent.getDuration() != 0) {
                                pw.print("; duration=");
                                TimeUtils.formatDuration((long) ent.getDuration(), pw);
                            }
                            pw.println();
                        }
                    }
                    return 0;
                case true:
                    res = shell2.parseUserOpMode(1, err);
                    if (res < 0) {
                        return res;
                    }
                    ops = shell2.mInterface.getPackagesForOps(new int[]{shell2.op});
                    if (ops == null || ops.size() <= 0) {
                        pw.println("No operations.");
                        return 0;
                    }
                    for (mode = 0; mode < ops.size(); mode++) {
                        PackageOps pkg = (PackageOps) ops.get(mode);
                        boolean hasMatch = false;
                        entries = ((PackageOps) ops.get(mode)).getOps();
                        j = 0;
                        while (j < entries.size()) {
                            ent = (OpEntry) entries.get(j);
                            if (ent.getOp() == shell2.op && ent.getMode() == shell2.mode) {
                                hasMatch = true;
                                if (!hasMatch) {
                                    pw.println(pkg.getPackageName());
                                }
                            } else {
                                j++;
                            }
                        }
                        if (!hasMatch) {
                        }
                    }
                    return 0;
                case true:
                    String packageName = null;
                    res = -2;
                    while (true) {
                        String nextArg = shell.getNextArg();
                        String argument = nextArg;
                        if (nextArg == null) {
                            if (res == -2) {
                                res = ActivityManager.getCurrentUser();
                            }
                            shell2.mInterface.resetAllModes(res, packageName);
                            pw.print("Reset all modes for: ");
                            if (res == -1) {
                                pw.print("all users");
                            } else {
                                pw.print("user ");
                                pw.print(res);
                            }
                            pw.print(", ");
                            if (packageName == null) {
                                pw.println("all packages");
                            } else {
                                pw.print("package ");
                                pw.println(packageName);
                            }
                            return 0;
                        } else if ("--user".equals(argument)) {
                            res = UserHandle.parseUserArg(shell.getNextArgRequired());
                        } else if (packageName == null) {
                            packageName = argument;
                        } else {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Error: Unsupported argument: ");
                            stringBuilder.append(argument);
                            err.println(stringBuilder.toString());
                            return -1;
                        }
                    }
                case true:
                    shell2.mInternal.enforceManageAppOpsModes(Binder.getCallingPid(), Binder.getCallingUid(), -1);
                    now = Binder.clearCallingIdentity();
                    try {
                        synchronized (shell2.mInternal) {
                            shell2.mInternal.mHandler.removeCallbacks(shell2.mInternal.mWriteRunner);
                        }
                        shell2.mInternal.writeState();
                        pw.println("Current settings written.");
                        Binder.restoreCallingIdentity(now);
                        return 0;
                    } catch (Throwable th) {
                        Binder.restoreCallingIdentity(now);
                    }
                case true:
                    shell2.mInternal.enforceManageAppOpsModes(Binder.getCallingPid(), Binder.getCallingUid(), -1);
                    now = Binder.clearCallingIdentity();
                    shell2.mInternal.readState();
                    pw.println("Last settings read.");
                    Binder.restoreCallingIdentity(now);
                    return 0;
                case true:
                    res = shell2.parseUserPackageOp(true, err);
                    if (res < 0) {
                        return res;
                    }
                    if (shell2.packageName == null) {
                        return -1;
                    }
                    shell2.mInterface.startOperation(shell2.mToken, shell2.op, shell2.packageUid, shell2.packageName, true);
                    return 0;
                case true:
                    res = shell2.parseUserPackageOp(true, err);
                    if (res < 0) {
                        return res;
                    }
                    if (shell2.packageName == null) {
                        return -1;
                    }
                    shell2.mInterface.finishOperation(shell2.mToken, shell2.op, shell2.packageUid, shell2.packageName);
                    return 0;
                default:
                    return shell.handleDefaultCommands(cmd);
            }
        } catch (RemoteException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Remote exception: ");
            stringBuilder.append(e);
            pw.println(stringBuilder.toString());
            return -1;
        } catch (Throwable th2) {
            Binder.restoreCallingIdentity(now);
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("Remote exception: ");
        stringBuilder.append(e);
        pw.println(stringBuilder.toString());
        return -1;
    }

    private void dumpHelp(PrintWriter pw) {
        pw.println("AppOps service (appops) dump options:");
        pw.println("  -h");
        pw.println("    Print this help text.");
        pw.println("  --op [OP]");
        pw.println("    Limit output to data associated with the given app op code.");
        pw.println("  --mode [MODE]");
        pw.println("    Limit output to data associated with the given app op mode.");
        pw.println("  --package [PACKAGE]");
        pw.println("    Limit output to data associated with the given package name.");
    }

    private void dumpTimesLocked(PrintWriter pw, String firstPrefix, String prefix, long[] times, long now, SimpleDateFormat sdf, Date date) {
        PrintWriter printWriter = pw;
        boolean hasTime = false;
        int i = 0;
        for (int i2 = 0; i2 < 6; i2++) {
            if (times[i2] != 0) {
                hasTime = true;
                break;
            }
        }
        if (hasTime) {
            Date date2;
            boolean first = true;
            while (i < 6) {
                if (times[i] != 0) {
                    printWriter.print(first ? firstPrefix : prefix);
                    first = false;
                    printWriter.print(UID_STATE_NAMES[i]);
                    printWriter.print(" = ");
                    date.setTime(times[i]);
                    printWriter.print(sdf.format(date));
                    printWriter.print(" (");
                    TimeUtils.formatDuration(times[i] - now, printWriter);
                    printWriter.println(")");
                } else {
                    date2 = date;
                }
                i++;
            }
            date2 = date;
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:276:0x0585 A:{Catch:{ all -> 0x0538 }} */
    /* JADX WARNING: Removed duplicated region for block: B:275:0x0582 A:{Catch:{ all -> 0x0538 }} */
    /* JADX WARNING: Removed duplicated region for block: B:279:0x058b A:{Catch:{ all -> 0x0538 }} */
    /* JADX WARNING: Removed duplicated region for block: B:278:0x0588 A:{Catch:{ all -> 0x0538 }} */
    /* JADX WARNING: Removed duplicated region for block: B:328:0x063c  */
    /* JADX WARNING: Removed duplicated region for block: B:295:0x05c1 A:{Catch:{ all -> 0x0538 }} */
    /* JADX WARNING: Removed duplicated region for block: B:336:0x0650  */
    /* JADX WARNING: Missing block: B:411:0x07b7, code:
            if (r10 != r2.op) goto L_0x07c3;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        int dumpMode;
        Throwable th;
        AppOpsService appOpsService = this;
        PrintWriter printWriter = pw;
        String[] strArr = args;
        if (DumpUtils.checkDumpAndUsageStatsPermission(appOpsService.mContext, TAG, printWriter)) {
            int dumpMode2;
            int dumpUid;
            int dumpUid2;
            int dumpOp;
            int i;
            int dumpOp2;
            String dumpPackage;
            String dumpPackage2 = null;
            if (strArr != null) {
                dumpMode2 = -1;
                dumpUid = -1;
                dumpUid2 = -1;
                dumpOp = 0;
                while (dumpOp < strArr.length) {
                    String arg = strArr[dumpOp];
                    if ("-h".equals(arg)) {
                        appOpsService.dumpHelp(printWriter);
                        return;
                    }
                    if (!"-a".equals(arg)) {
                        StringBuilder stringBuilder;
                        if ("--op".equals(arg)) {
                            dumpOp++;
                            if (dumpOp >= strArr.length) {
                                printWriter.println("No argument for --op option");
                                return;
                            }
                            dumpUid2 = Shell.strOpToOp(strArr[dumpOp], printWriter);
                            if (dumpUid2 < 0) {
                                return;
                            }
                        } else if ("--package".equals(arg)) {
                            i = dumpOp + 1;
                            if (i >= strArr.length) {
                                printWriter.println("No argument for --package option");
                                return;
                            }
                            dumpPackage2 = strArr[i];
                            try {
                                dumpUid = AppGlobals.getPackageManager().getPackageUid(dumpPackage2, 12591104, 0);
                            } catch (RemoteException e) {
                            }
                            if (dumpUid < 0) {
                                dumpOp = new StringBuilder();
                                dumpOp.append("Unknown package: ");
                                dumpOp.append(dumpPackage2);
                                printWriter.println(dumpOp.toString());
                                return;
                            }
                            dumpUid = UserHandle.getAppId(dumpUid);
                            dumpOp = i;
                        } else if ("--mode".equals(arg)) {
                            dumpOp++;
                            if (dumpOp >= strArr.length) {
                                printWriter.println("No argument for --mode option");
                                return;
                            }
                            dumpMode2 = Shell.strModeToMode(strArr[dumpOp], printWriter);
                            if (dumpMode2 < 0) {
                                return;
                            }
                        } else if (arg.length() <= 0 || arg.charAt(0) != '-') {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Unknown command: ");
                            stringBuilder.append(arg);
                            printWriter.println(stringBuilder.toString());
                            return;
                        } else {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Unknown option: ");
                            stringBuilder.append(arg);
                            printWriter.println(stringBuilder.toString());
                            return;
                        }
                    }
                    dumpOp++;
                }
                dumpOp2 = dumpUid2;
                dumpMode = dumpMode2;
                dumpPackage = dumpPackage2;
            } else {
                dumpOp2 = -1;
                dumpPackage = null;
                dumpMode = -1;
                dumpUid = -1;
            }
            synchronized (this) {
                int poi;
                printWriter.println("Current AppOps Service state:");
                appOpsService.mConstants.dump(printWriter);
                pw.println();
                long now = System.currentTimeMillis();
                long nowElapsed = SystemClock.elapsedRealtime();
                long nowUptime = SystemClock.uptimeMillis();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                Date date = new Date();
                boolean needSep = false;
                if (dumpOp2 < 0 && dumpMode < 0 && dumpPackage == null) {
                    if (appOpsService.mProfileOwners != null) {
                        printWriter.println("  Profile owners:");
                        for (poi = 0; poi < appOpsService.mProfileOwners.size(); poi++) {
                            printWriter.print("    User #");
                            printWriter.print(appOpsService.mProfileOwners.keyAt(poi));
                            printWriter.print(": ");
                            UserHandle.formatUid(printWriter, appOpsService.mProfileOwners.valueAt(poi));
                            pw.println();
                        }
                        pw.println();
                    }
                }
                int i2;
                String str;
                int i3;
                int i4;
                PrintWriter printWriter2;
                try {
                    boolean printedHeader;
                    boolean needSep2;
                    boolean printedHeader2;
                    int j;
                    ModeCallback cb;
                    int j2;
                    long now2;
                    UidState uidState;
                    int dumpUid3;
                    if (appOpsService.mOpModeWatchers.size() > 0) {
                        boolean needSep3;
                        printedHeader = false;
                        needSep2 = false;
                        dumpUid2 = 0;
                        while (true) {
                            needSep3 = needSep2;
                            if (dumpUid2 >= appOpsService.mOpModeWatchers.size()) {
                                break;
                            }
                            if (dumpOp2 < 0 || dumpOp2 == appOpsService.mOpModeWatchers.keyAt(dumpUid2)) {
                                boolean printedOpHeader = false;
                                ArraySet<ModeCallback> callbacks = (ArraySet) appOpsService.mOpModeWatchers.valueAt(dumpUid2);
                                printedHeader2 = printedHeader;
                                j = 0;
                                while (j < callbacks.size()) {
                                    ArraySet<ModeCallback> callbacks2;
                                    cb = (ModeCallback) callbacks.valueAt(j);
                                    if (dumpPackage != null) {
                                        callbacks2 = callbacks;
                                        if (cb.mWatchingUid >= null && dumpUid != UserHandle.getAppId(cb.mWatchingUid)) {
                                            j++;
                                            callbacks = callbacks2;
                                            strArr = args;
                                        }
                                    } else {
                                        callbacks2 = callbacks;
                                    }
                                    needSep3 = true;
                                    if (!printedHeader2) {
                                        printWriter.println("  Op mode watchers:");
                                        printedHeader2 = true;
                                    }
                                    if (!printedOpHeader) {
                                        printWriter.print("    Op ");
                                        printWriter.print(AppOpsManager.opToName(appOpsService.mOpModeWatchers.keyAt(dumpUid2)));
                                        printWriter.println(":");
                                        printedOpHeader = true;
                                    }
                                    printWriter.print("      #");
                                    printWriter.print(j);
                                    printWriter.print(": ");
                                    printWriter.println(cb);
                                    j++;
                                    callbacks = callbacks2;
                                    strArr = args;
                                }
                                printedHeader = printedHeader2;
                            }
                            needSep2 = needSep3;
                            dumpUid2++;
                            strArr = args;
                        }
                        needSep = needSep3;
                    }
                    if (appOpsService.mPackageModeWatchers.size() <= 0 || dumpOp2 >= 0) {
                        needSep2 = needSep;
                    } else {
                        printedHeader = false;
                        needSep2 = needSep;
                        needSep = false;
                        while (needSep < appOpsService.mPackageModeWatchers.size()) {
                            try {
                                if (dumpPackage == null || dumpPackage.equals(appOpsService.mPackageModeWatchers.keyAt(needSep))) {
                                    boolean needSep4;
                                    boolean printedHeader3;
                                    needSep2 = true;
                                    if (!printedHeader) {
                                        printWriter.println("  Package mode watchers:");
                                        printedHeader = true;
                                    }
                                    printWriter.print("    Pkg ");
                                    printWriter.print((String) appOpsService.mPackageModeWatchers.keyAt(needSep));
                                    printWriter.println(":");
                                    ArraySet<ModeCallback> callbacks3 = (ArraySet) appOpsService.mPackageModeWatchers.valueAt(needSep);
                                    j2 = 0;
                                    while (true) {
                                        needSep4 = needSep2;
                                        printedHeader3 = printedHeader;
                                        j = j2;
                                        if (j >= callbacks3.size()) {
                                            break;
                                        }
                                        printWriter.print("      #");
                                        printWriter.print(j);
                                        printWriter.print(": ");
                                        printWriter.println(callbacks3.valueAt(j));
                                        j2 = j + 1;
                                        needSep2 = needSep4;
                                        printedHeader = printedHeader3;
                                    }
                                    needSep2 = needSep4;
                                    printedHeader = printedHeader3;
                                }
                                needSep++;
                            } catch (Throwable th2) {
                                th = th2;
                                i2 = dumpUid;
                                str = dumpPackage;
                                i3 = dumpMode;
                                i4 = dumpOp2;
                                printWriter2 = printWriter;
                                dumpUid = appOpsService;
                                throw th;
                            }
                        }
                    }
                    if (appOpsService.mModeWatchers.size() > 0 && dumpOp2 < 0) {
                        boolean needSep5;
                        printedHeader = false;
                        for (dumpUid2 = 0; dumpUid2 < appOpsService.mModeWatchers.size(); dumpUid2++) {
                            boolean needSep6;
                            cb = (ModeCallback) appOpsService.mModeWatchers.valueAt(dumpUid2);
                            if (dumpPackage != null) {
                                needSep5 = needSep2;
                                if (cb.mWatchingUid < false && dumpUid != UserHandle.getAppId(cb.mWatchingUid)) {
                                    needSep2 = needSep5;
                                }
                            }
                            if (printedHeader) {
                                needSep6 = true;
                            } else {
                                needSep6 = true;
                                printWriter.println("  All op mode watchers:");
                                printedHeader = true;
                            }
                            printWriter.print("    ");
                            printWriter.print(Integer.toHexString(System.identityHashCode(appOpsService.mModeWatchers.keyAt(dumpUid2))));
                            printWriter.print(": ");
                            printWriter.println(cb);
                            needSep2 = needSep6;
                        }
                        needSep5 = needSep2;
                    }
                    if (appOpsService.mActiveWatchers.size() <= 0 || dumpMode >= 0) {
                        now2 = now;
                        uidState = true;
                    } else {
                        boolean needSep7;
                        needSep2 = true;
                        printedHeader = false;
                        dumpUid2 = 0;
                        while (dumpUid2 < appOpsService.mActiveWatchers.size()) {
                            SparseArray<ActiveCallback> activeWatchers = (SparseArray) appOpsService.mActiveWatchers.valueAt(dumpUid2);
                            if (activeWatchers.size() <= 0) {
                                now2 = now;
                                needSep7 = needSep2;
                            } else {
                                needSep7 = needSep2;
                                ActiveCallback cb2 = (ActiveCallback) activeWatchers.valueAt(false);
                                if (dumpOp2 < 0 || activeWatchers.indexOfKey(dumpOp2) >= 0) {
                                    ActiveCallback cb3;
                                    if (dumpPackage != null) {
                                        now2 = now;
                                        cb3 = cb2;
                                        if (cb3.mWatchingUid >= null && dumpUid != UserHandle.getAppId(cb3.mWatchingUid)) {
                                        }
                                    } else {
                                        now2 = now;
                                        cb3 = cb2;
                                    }
                                    if (!printedHeader) {
                                        printWriter.println("  All op active watchers:");
                                        printedHeader = true;
                                    }
                                    printWriter.print("    ");
                                    printWriter.print(Integer.toHexString(System.identityHashCode(appOpsService.mActiveWatchers.keyAt(dumpUid2))));
                                    printWriter.println(" ->");
                                    printWriter.print("        [");
                                    now = activeWatchers.size();
                                    dumpUid2 = 0;
                                    while (dumpUid2 < now) {
                                        if (dumpUid2 > 0) {
                                            printWriter.print(' ');
                                        }
                                        printWriter.print(AppOpsManager.opToName(activeWatchers.keyAt(dumpUid2)));
                                        if (dumpUid2 < now - 1) {
                                            printWriter.print(',');
                                        }
                                        dumpUid2++;
                                    }
                                    printWriter.println("]");
                                    printWriter.print("        ");
                                    printWriter.println(cb3);
                                } else {
                                    now2 = now;
                                }
                            }
                            dumpUid2++;
                            needSep2 = needSep7;
                            now = now2;
                        }
                        now2 = now;
                        needSep7 = needSep2;
                        uidState = true;
                    }
                    if (appOpsService.mClients.size() <= 0 || dumpMode >= 0) {
                        dumpUid3 = dumpUid;
                    } else {
                        boolean needSep8;
                        needSep2 = true;
                        boolean printedHeader4 = false;
                        dumpUid2 = 0;
                        while (dumpUid2 < appOpsService.mClients.size()) {
                            try {
                                ClientState cs = (ClientState) appOpsService.mClients.valueAt(dumpUid2);
                                if (cs.mStartedOps.size() > 0) {
                                    printedHeader2 = false;
                                    boolean printedClient = false;
                                    boolean printedHeader5 = printedHeader4;
                                    i = 0;
                                    while (true) {
                                        dumpUid3 = dumpUid;
                                        if (i >= cs.mStartedOps.size()) {
                                            break;
                                        }
                                        Op dumpUid4 = (Op) cs.mStartedOps.get(i);
                                        if (dumpOp2 >= 0) {
                                            needSep8 = needSep2;
                                            if (dumpUid4.op != dumpOp2) {
                                                i++;
                                                dumpUid = dumpUid3;
                                                needSep2 = needSep8;
                                            }
                                        } else {
                                            needSep8 = needSep2;
                                        }
                                        if (dumpPackage == null || dumpPackage.equals(dumpUid4.packageName)) {
                                            if (!printedHeader5) {
                                                printWriter.println("  Clients:");
                                                printedHeader5 = true;
                                            }
                                            if (!printedClient) {
                                                printWriter.print("    ");
                                                printWriter.print(appOpsService.mClients.keyAt(dumpUid2));
                                                printWriter.println(":");
                                                printWriter.print("      ");
                                                printWriter.println(cs);
                                                printedClient = true;
                                            }
                                            if (!printedHeader2) {
                                                printWriter.println("      Started ops:");
                                                printedHeader2 = true;
                                            }
                                            printWriter.print("        ");
                                            printWriter.print("uid=");
                                            printWriter.print(dumpUid4.uid);
                                            printWriter.print(" pkg=");
                                            printWriter.print(dumpUid4.packageName);
                                            printWriter.print(" op=");
                                            printWriter.println(AppOpsManager.opToName(dumpUid4.op));
                                            i++;
                                            dumpUid = dumpUid3;
                                            needSep2 = needSep8;
                                        } else {
                                            i++;
                                            dumpUid = dumpUid3;
                                            needSep2 = needSep8;
                                        }
                                    }
                                    needSep8 = needSep2;
                                    printedHeader4 = printedHeader5;
                                } else {
                                    dumpUid3 = dumpUid;
                                    needSep8 = needSep2;
                                }
                                dumpUid2++;
                                dumpUid = dumpUid3;
                                needSep2 = needSep8;
                            } catch (Throwable th3) {
                                th = th3;
                                i2 = dumpUid;
                                str = dumpPackage;
                                i3 = dumpMode;
                                boolean z = dumpOp2;
                                printWriter2 = printWriter;
                                dumpUid = appOpsService;
                                throw th;
                            }
                        }
                        dumpUid3 = dumpUid;
                        needSep8 = needSep2;
                    }
                    try {
                        boolean printedHeader6;
                        int usage;
                        AppOpsService appOpsService2;
                        if (appOpsService.mAudioRestrictions.size() > 0 && dumpOp2 < false && dumpPackage != null && dumpMode < 0) {
                            printedHeader6 = false;
                            dumpUid2 = 0;
                            while (dumpUid2 < appOpsService.mAudioRestrictions.size()) {
                                try {
                                    String op = AppOpsManager.opToName(appOpsService.mAudioRestrictions.keyAt(dumpUid2));
                                    SparseArray<Restriction> restrictions = (SparseArray) appOpsService.mAudioRestrictions.valueAt(dumpUid2);
                                    printedHeader = printedHeader6;
                                    dumpUid = 0;
                                    while (dumpUid < restrictions.size()) {
                                        SparseArray<Restriction> restrictions2;
                                        if (!printedHeader) {
                                            printWriter.println("  Audio Restrictions:");
                                            printedHeader = true;
                                            needSep2 = true;
                                        }
                                        usage = restrictions.keyAt(dumpUid);
                                        boolean needSep9 = needSep2;
                                        printWriter.print("    ");
                                        printWriter.print(op);
                                        printWriter.print(" usage=");
                                        printWriter.print(AudioAttributes.usageToString(usage));
                                        Restriction r = (Restriction) restrictions.valueAt(dumpUid);
                                        String op2 = op;
                                        printWriter.print(": mode=");
                                        printWriter.println(AppOpsManager.modeToName(r.mode));
                                        if (!r.exceptionPackages.isEmpty()) {
                                            printWriter.println("      Exceptions:");
                                            i = 0;
                                            while (true) {
                                                restrictions2 = restrictions;
                                                if (i >= r.exceptionPackages.size()) {
                                                    break;
                                                }
                                                printWriter.print("        ");
                                                printWriter.println((String) r.exceptionPackages.valueAt(i));
                                                i++;
                                                restrictions = restrictions2;
                                            }
                                        } else {
                                            restrictions2 = restrictions;
                                        }
                                        dumpUid++;
                                        needSep2 = needSep9;
                                        op = op2;
                                        restrictions = restrictions2;
                                    }
                                    dumpUid2++;
                                    printedHeader6 = printedHeader;
                                } catch (Throwable th4) {
                                    th = th4;
                                    str = dumpPackage;
                                    i3 = dumpMode;
                                    i4 = dumpOp2;
                                    printWriter2 = printWriter;
                                    appOpsService2 = appOpsService;
                                    i2 = dumpUid3;
                                }
                            }
                        }
                        if (needSep2) {
                            pw.println();
                        }
                        dumpUid2 = 0;
                        while (true) {
                            usage = dumpUid2;
                            int i5;
                            if (usage < appOpsService.mUidStates.size()) {
                                int i6;
                                SimpleDateFormat sdf2;
                                long nowUptime2;
                                boolean uidState2;
                                int code;
                                UidState uidState3 = (UidState) appOpsService.mUidStates.valueAt(usage);
                                SparseIntArray opModes = uidState3.opModes;
                                ArrayMap<String, Ops> pkgOps = uidState3.pkgOps;
                                boolean needSep10;
                                if (dumpOp2 >= 0 || dumpPackage != null || dumpMode >= 0) {
                                    boolean hasOp;
                                    boolean hasPackage;
                                    if (dumpOp2 >= 0) {
                                        if (uidState3.opModes == null || uidState3.opModes.indexOfKey(dumpOp2) < 0) {
                                            boolean hasMode;
                                            needSep = false;
                                            printedHeader6 = dumpPackage != null ? uidState : false;
                                            printedHeader2 = dumpMode >= 0 ? uidState : false;
                                            if (!printedHeader2 || opModes == null) {
                                                hasOp = needSep;
                                                hasPackage = printedHeader6;
                                            } else {
                                                hasMode = printedHeader2;
                                                j2 = 0;
                                                while (true) {
                                                    int opi = j2;
                                                    if (!hasMode) {
                                                        hasOp = needSep;
                                                        hasPackage = printedHeader6;
                                                        dumpUid = opi;
                                                        if (dumpUid >= opModes.size()) {
                                                            break;
                                                        }
                                                        if (opModes.valueAt(dumpUid) == dumpMode) {
                                                            hasMode = true;
                                                        }
                                                        j2 = dumpUid + 1;
                                                        needSep = hasOp;
                                                        printedHeader6 = hasPackage;
                                                    } else {
                                                        hasOp = needSep;
                                                        hasPackage = printedHeader6;
                                                        break;
                                                    }
                                                }
                                                printedHeader2 = hasMode;
                                            }
                                            if (pkgOps == null) {
                                                printedHeader6 = hasPackage;
                                                dumpUid2 = 0;
                                                while (true) {
                                                    if (!hasOp || !printedHeader6 || !printedHeader2) {
                                                        i6 = usage;
                                                        if (dumpUid2 >= pkgOps.size()) {
                                                            sdf2 = sdf;
                                                            needSep10 = needSep2;
                                                            break;
                                                        }
                                                        Ops ops = (Ops) pkgOps.valueAt(dumpUid2);
                                                        if (!(hasOp || ops == null || ops.indexOfKey(dumpOp2) < 0)) {
                                                            hasOp = true;
                                                        }
                                                        if (printedHeader2) {
                                                            sdf2 = sdf;
                                                            needSep10 = needSep2;
                                                        } else {
                                                            hasMode = printedHeader2;
                                                            j2 = 0;
                                                            while (true) {
                                                                int opi2 = j2;
                                                                if (!hasMode) {
                                                                    needSep10 = needSep2;
                                                                    sdf2 = sdf;
                                                                    sdf = opi2;
                                                                    if (sdf >= ops.size()) {
                                                                        break;
                                                                    }
                                                                    if (((Op) ops.valueAt(sdf)).mode == dumpMode) {
                                                                        hasMode = true;
                                                                    }
                                                                    j2 = sdf + 1;
                                                                    needSep2 = needSep10;
                                                                    sdf = sdf2;
                                                                } else {
                                                                    sdf2 = sdf;
                                                                    needSep10 = needSep2;
                                                                    break;
                                                                }
                                                            }
                                                            printedHeader2 = hasMode;
                                                        }
                                                        if (!printedHeader6 && dumpPackage.equals(ops.packageName)) {
                                                            printedHeader6 = true;
                                                        }
                                                        dumpUid2++;
                                                        usage = i6;
                                                        needSep2 = needSep10;
                                                        sdf = sdf2;
                                                    } else {
                                                        sdf2 = sdf;
                                                        needSep10 = needSep2;
                                                        i6 = usage;
                                                        break;
                                                    }
                                                }
                                                hasPackage = printedHeader6;
                                            } else {
                                                sdf2 = sdf;
                                                needSep10 = needSep2;
                                                i6 = usage;
                                            }
                                            if (!(uidState3.foregroundOps == null || hasOp)) {
                                                if (uidState3.foregroundOps.indexOfKey(dumpOp2) > 0) {
                                                    hasOp = true;
                                                }
                                            }
                                            if (hasOp || !hasPackage) {
                                                nowUptime2 = nowUptime;
                                                str = dumpPackage;
                                                i3 = dumpMode;
                                                i4 = dumpOp2;
                                                printWriter2 = printWriter;
                                                appOpsService2 = appOpsService;
                                                now = now2;
                                                i2 = dumpUid3;
                                                i5 = i6;
                                                sdf = sdf2;
                                            } else if (!printedHeader2) {
                                                nowUptime2 = nowUptime;
                                                str = dumpPackage;
                                                i3 = dumpMode;
                                                i4 = dumpOp2;
                                                printWriter2 = printWriter;
                                                appOpsService2 = appOpsService;
                                                now = now2;
                                                i2 = dumpUid3;
                                                i5 = i6;
                                                sdf = sdf2;
                                            }
                                            needSep2 = needSep10;
                                            printWriter = printWriter2;
                                            appOpsService = appOpsService2;
                                            now2 = now;
                                            dumpUid3 = i2;
                                            dumpPackage = str;
                                            dumpMode = i3;
                                            dumpOp2 = i4;
                                            uidState2 = true;
                                            dumpUid2 = i5 + 1;
                                            nowUptime = nowUptime2;
                                        }
                                    }
                                    needSep = uidState;
                                    if (dumpPackage != null) {
                                    }
                                    if (dumpMode >= 0) {
                                    }
                                    if (printedHeader2) {
                                    }
                                    hasOp = needSep;
                                    hasPackage = printedHeader6;
                                    if (pkgOps == null) {
                                    }
                                    if (uidState3.foregroundOps.indexOfKey(dumpOp2) > 0) {
                                    }
                                    if (hasOp) {
                                    }
                                    nowUptime2 = nowUptime;
                                    str = dumpPackage;
                                    i3 = dumpMode;
                                    i4 = dumpOp2;
                                    printWriter2 = printWriter;
                                    appOpsService2 = appOpsService;
                                    now = now2;
                                    i2 = dumpUid3;
                                    i5 = i6;
                                    sdf = sdf2;
                                    needSep2 = needSep10;
                                    printWriter = printWriter2;
                                    appOpsService = appOpsService2;
                                    now2 = now;
                                    dumpUid3 = i2;
                                    dumpPackage = str;
                                    dumpMode = i3;
                                    dumpOp2 = i4;
                                    uidState2 = true;
                                    dumpUid2 = i5 + 1;
                                    nowUptime = nowUptime2;
                                } else {
                                    sdf2 = sdf;
                                    needSep10 = needSep2;
                                    i6 = usage;
                                }
                                printWriter.print("  Uid ");
                                UserHandle.formatUid(printWriter, uidState3.uid);
                                printWriter.println(":");
                                printWriter.print("    state=");
                                printWriter.println(UID_STATE_NAMES[uidState3.state]);
                                if (uidState3.state != uidState3.pendingState) {
                                    printWriter.print("    pendingState=");
                                    printWriter.println(UID_STATE_NAMES[uidState3.pendingState]);
                                }
                                if (uidState3.pendingStateCommitTime != 0) {
                                    printWriter.print("    pendingStateCommitTime=");
                                    TimeUtils.formatDuration(uidState3.pendingStateCommitTime, nowUptime, printWriter);
                                    pw.println();
                                }
                                if (uidState3.startNesting != 0) {
                                    printWriter.print("    startNesting=");
                                    printWriter.println(uidState3.startNesting);
                                }
                                if (uidState3.foregroundOps != null && (dumpMode < 0 || dumpMode == 4)) {
                                    printWriter.println("    foregroundOps:");
                                    dumpUid2 = 0;
                                    while (dumpUid2 < uidState3.foregroundOps.size()) {
                                        if (dumpOp2 < 0 || dumpOp2 == uidState3.foregroundOps.keyAt(dumpUid2)) {
                                            printWriter.print("      ");
                                            printWriter.print(AppOpsManager.opToName(uidState3.foregroundOps.keyAt(dumpUid2)));
                                            printWriter.print(": ");
                                            printWriter.println(uidState3.foregroundOps.valueAt(dumpUid2) ? "WATCHER" : "SILENT");
                                        }
                                        dumpUid2++;
                                    }
                                    printWriter.print("    hasForegroundWatchers=");
                                    printWriter.println(uidState3.hasForegroundWatchers);
                                }
                                if (opModes != null) {
                                    dumpUid2 = opModes.size();
                                    for (dumpUid = 0; dumpUid < dumpUid2; dumpUid++) {
                                        code = opModes.keyAt(dumpUid);
                                        poi = opModes.valueAt(dumpUid);
                                        if ((dumpOp2 < 0 || dumpOp2 == code) && (dumpMode < 0 || dumpMode == poi)) {
                                            printWriter.print("      ");
                                            printWriter.print(AppOpsManager.opToName(code));
                                            printWriter.print(": mode=");
                                            printWriter.println(AppOpsManager.modeToName(poi));
                                        }
                                    }
                                }
                                if (pkgOps != null) {
                                    dumpUid2 = 0;
                                    while (true) {
                                        usage = dumpUid2;
                                        if (usage >= pkgOps.size()) {
                                            nowUptime2 = nowUptime;
                                            str = dumpPackage;
                                            i3 = dumpMode;
                                            i4 = dumpOp2;
                                            printWriter2 = printWriter;
                                            appOpsService2 = appOpsService;
                                            now = now2;
                                            i2 = dumpUid3;
                                            i5 = i6;
                                            sdf = sdf2;
                                            break;
                                        }
                                        Ops ops2 = (Ops) pkgOps.valueAt(usage);
                                        if (dumpPackage != null) {
                                            if (!dumpPackage.equals(ops2.packageName)) {
                                                continue;
                                                i6 = i6;
                                                printWriter = printWriter;
                                                appOpsService = appOpsService;
                                                dumpUid2 = usage + 1;
                                                now2 = now2;
                                                sdf2 = sdf2;
                                                uidState3 = uidState3;
                                                dumpUid3 = dumpUid3;
                                                dumpPackage = dumpPackage;
                                                dumpMode = dumpMode;
                                                pkgOps = pkgOps;
                                                dumpOp2 = dumpOp2;
                                                uidState2 = true;
                                                opModes = opModes;
                                                nowUptime = nowUptime;
                                            }
                                        }
                                        printedHeader6 = false;
                                        dumpUid2 = 0;
                                        while (true) {
                                            code = dumpUid2;
                                            if (code >= ops2.size()) {
                                                continue;
                                                break;
                                            }
                                            long nowUptime3;
                                            boolean printedPackage;
                                            ArrayMap<String, Ops> pkgOps2;
                                            int j3;
                                            Ops ops3;
                                            int pkgi;
                                            Op op3 = (Op) ops2.valueAt(code);
                                            if (dumpOp2 >= 0) {
                                                nowUptime3 = nowUptime;
                                            } else {
                                                nowUptime3 = nowUptime;
                                            }
                                            if (dumpMode < 0 || dumpMode == op3.mode) {
                                                if (!printedHeader6) {
                                                    printWriter.print("    Package ");
                                                    printWriter.print(ops2.packageName);
                                                    printWriter.println(":");
                                                    printedHeader6 = true;
                                                }
                                                nowUptime = printedHeader6;
                                                printWriter.print("      ");
                                                printWriter.print(AppOpsManager.opToName(op3.op));
                                                printWriter.print(" (");
                                                printWriter.print(AppOpsManager.modeToName(op3.mode));
                                                dumpUid = AppOpsManager.opToSwitch(op3.op);
                                                if (dumpUid != op3.op) {
                                                    printWriter.print(" / switch ");
                                                    printWriter.print(AppOpsManager.opToName(dumpUid));
                                                    Op switchObj = (Op) ops2.get(dumpUid);
                                                    if (switchObj != null) {
                                                        printedPackage = nowUptime;
                                                        nowUptime = switchObj.mode;
                                                    } else {
                                                        printedPackage = nowUptime;
                                                        nowUptime = AppOpsManager.opToDefaultMode(dumpUid);
                                                    }
                                                    printWriter.print("=");
                                                    printWriter.print(AppOpsManager.modeToName(nowUptime));
                                                } else {
                                                    printedPackage = nowUptime;
                                                }
                                                printWriter.println("): ");
                                                nowUptime2 = nowUptime3;
                                                int pkgi2 = usage;
                                                Op op4 = op3;
                                                i2 = dumpUid3;
                                                str = dumpPackage;
                                                i3 = dumpMode;
                                                nowUptime = opModes;
                                                pkgOps2 = pkgOps;
                                                j3 = code;
                                                now = now2;
                                                sdf = sdf2;
                                                try {
                                                    appOpsService.dumpTimesLocked(printWriter, "          Access: ", "                  ", op3.time, now, sdf, date);
                                                    dumpPackage2 = "          Reject: ";
                                                    String str2 = "                  ";
                                                    long[] jArr = op4.rejectTime;
                                                    i4 = dumpOp2;
                                                    ops3 = ops2;
                                                    dumpMode2 = uidState;
                                                    PrintWriter printWriter3 = printWriter;
                                                    uidState = uidState3;
                                                    Object obj = null;
                                                    String str3 = dumpPackage2;
                                                    Op op5 = op4;
                                                    i5 = i6;
                                                    pkgi = pkgi2;
                                                    String str4 = str2;
                                                    printWriter2 = printWriter;
                                                    long[] jArr2 = jArr;
                                                    appOpsService2 = appOpsService;
                                                    appOpsService.dumpTimesLocked(printWriter3, str3, str4, jArr2, now, sdf, date);
                                                    if (op5.duration == -1) {
                                                        printWriter2.print("          Running start at: ");
                                                        TimeUtils.formatDuration(nowElapsed - op5.startRealtime, printWriter2);
                                                        pw.println();
                                                    } else if (op5.duration != 0) {
                                                        printWriter2.print("          duration=");
                                                        TimeUtils.formatDuration((long) op5.duration, printWriter2);
                                                        pw.println();
                                                    }
                                                    if (op5.startNesting != 0) {
                                                        printWriter2.print("          startNesting=");
                                                        printWriter2.println(op5.startNesting);
                                                    }
                                                    i6 = i5;
                                                    printWriter = printWriter2;
                                                    appOpsService = appOpsService2;
                                                    dumpUid2 = j3 + 1;
                                                    ops2 = ops3;
                                                    now2 = now;
                                                    sdf2 = sdf;
                                                    uidState3 = uidState;
                                                    usage = pkgi;
                                                    dumpUid3 = i2;
                                                    dumpPackage = str;
                                                    dumpMode = i3;
                                                    pkgOps = pkgOps2;
                                                    printedHeader6 = printedPackage;
                                                    dumpOp2 = i4;
                                                    uidState2 = true;
                                                    opModes = nowUptime;
                                                    nowUptime = nowUptime2;
                                                } catch (Throwable th5) {
                                                    th = th5;
                                                }
                                            }
                                            printedPackage = printedHeader6;
                                            str = dumpPackage;
                                            i3 = dumpMode;
                                            nowUptime = opModes;
                                            pkgOps2 = pkgOps;
                                            j3 = code;
                                            i4 = dumpOp2;
                                            ops3 = ops2;
                                            uidState = uidState3;
                                            pkgi = usage;
                                            printWriter2 = printWriter;
                                            appOpsService2 = appOpsService;
                                            now = now2;
                                            i2 = dumpUid3;
                                            i5 = i6;
                                            sdf = sdf2;
                                            nowUptime2 = nowUptime3;
                                            i6 = i5;
                                            printWriter = printWriter2;
                                            appOpsService = appOpsService2;
                                            dumpUid2 = j3 + 1;
                                            ops2 = ops3;
                                            now2 = now;
                                            sdf2 = sdf;
                                            uidState3 = uidState;
                                            usage = pkgi;
                                            dumpUid3 = i2;
                                            dumpPackage = str;
                                            dumpMode = i3;
                                            pkgOps = pkgOps2;
                                            printedHeader6 = printedPackage;
                                            dumpOp2 = i4;
                                            uidState2 = true;
                                            opModes = nowUptime;
                                            nowUptime = nowUptime2;
                                        }
                                        i6 = i6;
                                        printWriter = printWriter;
                                        appOpsService = appOpsService;
                                        dumpUid2 = usage + 1;
                                        now2 = now2;
                                        sdf2 = sdf2;
                                        uidState3 = uidState3;
                                        dumpUid3 = dumpUid3;
                                        dumpPackage = dumpPackage;
                                        dumpMode = dumpMode;
                                        pkgOps = pkgOps;
                                        dumpOp2 = dumpOp2;
                                        uidState2 = true;
                                        opModes = opModes;
                                        nowUptime = nowUptime;
                                    }
                                } else {
                                    nowUptime2 = nowUptime;
                                    str = dumpPackage;
                                    i3 = dumpMode;
                                    i4 = dumpOp2;
                                    printWriter2 = printWriter;
                                    appOpsService2 = appOpsService;
                                    now = now2;
                                    i2 = dumpUid3;
                                    i5 = i6;
                                    sdf = sdf2;
                                }
                                needSep2 = true;
                                printWriter = printWriter2;
                                appOpsService = appOpsService2;
                                now2 = now;
                                dumpUid3 = i2;
                                dumpPackage = str;
                                dumpMode = i3;
                                dumpOp2 = i4;
                                uidState2 = true;
                                dumpUid2 = i5 + 1;
                                nowUptime = nowUptime2;
                            } else {
                                str = dumpPackage;
                                i3 = dumpMode;
                                i4 = dumpOp2;
                                printWriter2 = printWriter;
                                appOpsService2 = appOpsService;
                                now = now2;
                                i2 = dumpUid3;
                                if (needSep2) {
                                    pw.println();
                                }
                                dumpOp = appOpsService2.mOpUserRestrictions.size();
                                i5 = 0;
                                while (i5 < dumpOp) {
                                    int userRestrictionCount;
                                    IBinder token;
                                    long now3;
                                    IBinder token2 = (IBinder) appOpsService2.mOpUserRestrictions.keyAt(i5);
                                    ClientRestrictionState dumpMode3 = (ClientRestrictionState) appOpsService2.mOpUserRestrictions.valueAt(i5);
                                    StringBuilder stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append("  User restrictions for token ");
                                    stringBuilder2.append(token2);
                                    stringBuilder2.append(":");
                                    printWriter2.println(stringBuilder2.toString());
                                    dumpOp2 = dumpMode3.perUserRestrictions != null ? dumpMode3.perUserRestrictions.size() : 0;
                                    if (dumpOp2 > 0) {
                                        printWriter2.println("      Restricted ops:");
                                        poi = 0;
                                        while (poi < dumpOp2) {
                                            j = dumpMode3.perUserRestrictions.keyAt(poi);
                                            boolean[] restrictedOps = (boolean[]) dumpMode3.perUserRestrictions.valueAt(poi);
                                            if (restrictedOps == null) {
                                                userRestrictionCount = dumpOp;
                                                token = token2;
                                                now3 = now;
                                            } else {
                                                StringBuilder restrictedOpsValue = new StringBuilder();
                                                restrictedOpsValue.append("[");
                                                int restrictedOpCount = restrictedOps.length;
                                                j2 = 0;
                                                while (true) {
                                                    userRestrictionCount = dumpOp;
                                                    dumpOp = j2;
                                                    if (dumpOp >= restrictedOpCount) {
                                                        break;
                                                    }
                                                    if (restrictedOps[dumpOp]) {
                                                        token = token2;
                                                        now3 = now;
                                                        if (restrictedOpsValue.length() > 1) {
                                                            restrictedOpsValue.append(", ");
                                                        }
                                                        restrictedOpsValue.append(AppOpsManager.opToName(dumpOp));
                                                    } else {
                                                        token = token2;
                                                        now3 = now;
                                                    }
                                                    j2 = dumpOp + 1;
                                                    dumpOp = userRestrictionCount;
                                                    token2 = token;
                                                    now = now3;
                                                }
                                                token = token2;
                                                now3 = now;
                                                restrictedOpsValue.append("]");
                                                printWriter2.print("        ");
                                                printWriter2.print("user: ");
                                                printWriter2.print(j);
                                                printWriter2.print(" restricted ops: ");
                                                printWriter2.println(restrictedOpsValue);
                                            }
                                            poi++;
                                            dumpOp = userRestrictionCount;
                                            token2 = token;
                                            now = now3;
                                        }
                                    }
                                    userRestrictionCount = dumpOp;
                                    token = token2;
                                    now3 = now;
                                    dumpOp = dumpMode3.perUserExcludedPackages != null ? dumpMode3.perUserExcludedPackages.size() : 0;
                                    if (dumpOp > 0) {
                                        printWriter2.println("      Excluded packages:");
                                        for (dumpPackage = null; dumpPackage < dumpOp; dumpPackage++) {
                                            int userId = dumpMode3.perUserExcludedPackages.keyAt(dumpPackage);
                                            String[] packageNames = (String[]) dumpMode3.perUserExcludedPackages.valueAt(dumpPackage);
                                            printWriter2.print("        ");
                                            printWriter2.print("user: ");
                                            printWriter2.print(userId);
                                            printWriter2.print(" packages: ");
                                            printWriter2.println(Arrays.toString(packageNames));
                                        }
                                    }
                                    i5++;
                                    dumpOp = userRestrictionCount;
                                    now = now3;
                                }
                                return;
                            }
                        }
                    } catch (Throwable th6) {
                        th = th6;
                        str = dumpPackage;
                        i3 = dumpMode;
                        i4 = dumpOp2;
                        printWriter2 = printWriter;
                        dumpUid = appOpsService;
                        i2 = dumpUid3;
                    }
                } catch (Throwable th7) {
                    th = th7;
                    i2 = dumpUid;
                    str = dumpPackage;
                    i3 = dumpMode;
                    i4 = dumpOp2;
                    printWriter2 = printWriter;
                    dumpUid = appOpsService;
                    throw th;
                }
            }
        }
    }

    public void setUserRestrictions(Bundle restrictions, IBinder token, int userHandle) {
        checkSystemUid("setUserRestrictions");
        Preconditions.checkNotNull(restrictions);
        Preconditions.checkNotNull(token);
        for (int i = 0; i < 78; i++) {
            String restriction = AppOpsManager.opToRestriction(i);
            if (restriction != null) {
                setUserRestrictionNoCheck(i, restrictions.getBoolean(restriction, false), token, userHandle, null);
            }
        }
    }

    public void setUserRestriction(int code, boolean restricted, IBinder token, int userHandle, String[] exceptionPackages) {
        if (Binder.getCallingPid() != Process.myPid()) {
            this.mContext.enforcePermission("android.permission.MANAGE_APP_OPS_RESTRICTIONS", Binder.getCallingPid(), Binder.getCallingUid(), null);
        }
        if (userHandle == UserHandle.getCallingUserId() || this.mContext.checkCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL") == 0 || this.mContext.checkCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS") == 0) {
            verifyIncomingOp(code);
            Preconditions.checkNotNull(token);
            setUserRestrictionNoCheck(code, restricted, token, userHandle, exceptionPackages);
            return;
        }
        throw new SecurityException("Need INTERACT_ACROSS_USERS_FULL or INTERACT_ACROSS_USERS to interact cross user ");
    }

    private void setUserRestrictionNoCheck(int code, boolean restricted, IBinder token, int userHandle, String[] exceptionPackages) {
        synchronized (this) {
            ClientRestrictionState restrictionState = (ClientRestrictionState) this.mOpUserRestrictions.get(token);
            if (restrictionState == null) {
                try {
                    restrictionState = new ClientRestrictionState(token);
                    this.mOpUserRestrictions.put(token, restrictionState);
                } catch (RemoteException e) {
                    return;
                }
            }
            if (restrictionState.setRestriction(code, restricted, exceptionPackages, userHandle)) {
                this.mHandler.sendMessage(PooledLambda.obtainMessage(-$$Lambda$AppOpsService$UKMH8n9xZqCOX59uFPylskhjBgo.INSTANCE, this, Integer.valueOf(code), Integer.valueOf(-2)));
            }
            if (restrictionState.isDefault()) {
                this.mOpUserRestrictions.remove(token);
                restrictionState.destroy();
            }
        }
    }

    private void notifyWatchersOfChange(int code, int uid) {
        synchronized (this) {
            ArraySet<ModeCallback> callbacks = (ArraySet) this.mOpModeWatchers.get(code);
            if (callbacks == null) {
                return;
            }
            ArraySet clonedCallbacks = new ArraySet(callbacks);
            notifyOpChanged(clonedCallbacks, code, uid, null);
        }
    }

    public void removeUser(int userHandle) throws RemoteException {
        checkSystemUid("removeUser");
        synchronized (this) {
            for (int i = this.mOpUserRestrictions.size() - 1; i >= 0; i--) {
                ((ClientRestrictionState) this.mOpUserRestrictions.valueAt(i)).removeUser(userHandle);
            }
            removeUidsForUserLocked(userHandle);
        }
    }

    public boolean isOperationActive(int code, int uid, String packageName) {
        if (Binder.getCallingUid() != uid && this.mContext.checkCallingOrSelfPermission("android.permission.WATCH_APPOPS") != 0) {
            return false;
        }
        verifyIncomingOp(code);
        if (resolvePackageName(uid, packageName) == null) {
            return false;
        }
        synchronized (this) {
            for (int i = this.mClients.size() - 1; i >= 0; i--) {
                ClientState client = (ClientState) this.mClients.valueAt(i);
                for (int j = client.mStartedOps.size() - 1; j >= 0; j--) {
                    Op op = (Op) client.mStartedOps.get(j);
                    if (op.op == code && op.uid == uid) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    private void removeUidsForUserLocked(int userHandle) {
        for (int i = this.mUidStates.size() - 1; i >= 0; i--) {
            if (UserHandle.getUserId(this.mUidStates.keyAt(i)) == userHandle) {
                this.mUidStates.removeAt(i);
            }
        }
    }

    private void checkSystemUid(String function) {
        if (Binder.getCallingUid() != 1000) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(function);
            stringBuilder.append(" must by called by the system");
            throw new SecurityException(stringBuilder.toString());
        }
    }

    protected void scheduleWriteLockedHook(int code) {
    }

    private static String resolvePackageName(int uid, String packageName) {
        if (uid == 0) {
            return "root";
        }
        if (uid == IHwShutdownThread.SHUTDOWN_ANIMATION_WAIT_TIME) {
            return "com.android.shell";
        }
        if (uid == 1013) {
            return "media";
        }
        if (uid == 1041) {
            return "audioserver";
        }
        if (uid == 1047) {
            return "cameraserver";
        }
        if (uid == 1000 && packageName == null) {
            return PackageManagerService.PLATFORM_PACKAGE_NAME;
        }
        return packageName;
    }

    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static int resolveUid(String packageName) {
        if (packageName == null) {
            return -1;
        }
        int i;
        switch (packageName.hashCode()) {
            case -31178072:
                if (packageName.equals("cameraserver")) {
                    i = 4;
                    break;
                }
            case 3506402:
                if (packageName.equals("root")) {
                    i = 0;
                    break;
                }
            case 103772132:
                if (packageName.equals("media")) {
                    i = 2;
                    break;
                }
            case 109403696:
                if (packageName.equals("shell")) {
                    i = 1;
                    break;
                }
            case 1344606873:
                if (packageName.equals("audioserver")) {
                    i = 3;
                    break;
                }
            default:
                i = -1;
                break;
        }
        switch (i) {
            case 0:
                return 0;
            case 1:
                return IHwShutdownThread.SHUTDOWN_ANIMATION_WAIT_TIME;
            case 2:
                return 1013;
            case 3:
                return 1041;
            case 4:
                return 1047;
            default:
                return -1;
        }
    }

    private static String[] getPackagesForUid(int uid) {
        String[] packageNames = null;
        try {
            packageNames = AppGlobals.getPackageManager().getPackagesForUid(uid);
        } catch (RemoteException e) {
        }
        if (packageNames == null) {
            return EmptyArray.STRING;
        }
        return packageNames;
    }
}
