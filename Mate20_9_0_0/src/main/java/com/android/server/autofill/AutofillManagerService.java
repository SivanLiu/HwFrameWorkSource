package com.android.server.autofill;

import android.app.ActivityManager;
import android.app.ActivityThread;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.UserInfo;
import android.database.ContentObserver;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteCallback;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ShellCallback;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.UserManagerInternal;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.service.autofill.FillEventHistory;
import android.service.autofill.UserData;
import android.text.TextUtils;
import android.text.TextUtils.SimpleStringSplitter;
import android.util.ArrayMap;
import android.util.LocalLog;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.view.autofill.AutofillId;
import android.view.autofill.AutofillManagerInternal;
import android.view.autofill.AutofillValue;
import android.view.autofill.Helper;
import android.view.autofill.IAutoFillManager.Stub;
import android.view.autofill.IAutoFillManagerClient;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.content.PackageMonitor;
import com.android.internal.os.BackgroundThread;
import com.android.internal.os.IResultReceiver;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.Preconditions;
import com.android.server.FgThread;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.autofill.ui.AutoFillUI;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class AutofillManagerService extends SystemService {
    private static final char COMPAT_PACKAGE_DELIMITER = ':';
    private static final char COMPAT_PACKAGE_URL_IDS_BLOCK_BEGIN = '[';
    private static final char COMPAT_PACKAGE_URL_IDS_BLOCK_END = ']';
    private static final char COMPAT_PACKAGE_URL_IDS_DELIMITER = ',';
    static final String RECEIVER_BUNDLE_EXTRA_SESSIONS = "sessions";
    private static final String TAG = "AutofillManagerService";
    @GuardedBy("mLock")
    private boolean mAllowInstantService;
    private final AutofillCompatState mAutofillCompatState = new AutofillCompatState();
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.CLOSE_SYSTEM_DIALOGS".equals(intent.getAction())) {
                if (Helper.sDebug) {
                    Slog.d(AutofillManagerService.TAG, "Close system dialogs");
                }
                synchronized (AutofillManagerService.this.mLock) {
                    for (int i = 0; i < AutofillManagerService.this.mServicesCache.size(); i++) {
                        ((AutofillManagerServiceImpl) AutofillManagerService.this.mServicesCache.valueAt(i)).destroyFinishedSessionsLocked();
                    }
                }
                AutofillManagerService.this.mUi.hideAll(null);
            }
        }
    };
    private final Context mContext;
    @GuardedBy("mLock")
    private final SparseBooleanArray mDisabledUsers = new SparseBooleanArray();
    private final LocalService mLocalService = new LocalService(this, null);
    private final Object mLock = new Object();
    private final LocalLog mRequestsHistory = new LocalLog(20);
    @GuardedBy("mLock")
    private SparseArray<AutofillManagerServiceImpl> mServicesCache = new SparseArray();
    private final AutoFillUI mUi;
    private final LocalLog mUiLatencyHistory = new LocalLog(20);
    private final LocalLog mWtfHistory = new LocalLog(50);

    final class AutoFillManagerServiceStub extends Stub {
        AutoFillManagerServiceStub() {
        }

        public int addClient(IAutoFillManagerClient client, int userId) {
            int flags;
            synchronized (AutofillManagerService.this.mLock) {
                flags = 0;
                if (AutofillManagerService.this.getServiceForUserLocked(userId).addClientLocked(client)) {
                    flags = 0 | 1;
                }
                if (Helper.sDebug) {
                    flags |= 2;
                }
                if (Helper.sVerbose) {
                    flags |= 4;
                }
            }
            return flags;
        }

        public void removeClient(IAutoFillManagerClient client, int userId) {
            synchronized (AutofillManagerService.this.mLock) {
                AutofillManagerServiceImpl service = AutofillManagerService.this.peekServiceForUserLocked(userId);
                if (service != null) {
                    service.removeClientLocked(client);
                } else if (Helper.sVerbose) {
                    String str = AutofillManagerService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("removeClient(): no service for ");
                    stringBuilder.append(userId);
                    Slog.v(str, stringBuilder.toString());
                }
            }
        }

        public void setAuthenticationResult(Bundle data, int sessionId, int authenticationId, int userId) {
            synchronized (AutofillManagerService.this.mLock) {
                AutofillManagerService.this.getServiceForUserLocked(userId).setAuthenticationResultLocked(data, sessionId, authenticationId, getCallingUid());
            }
        }

        public void setHasCallback(int sessionId, int userId, boolean hasIt) {
            synchronized (AutofillManagerService.this.mLock) {
                AutofillManagerService.this.getServiceForUserLocked(userId).setHasCallback(sessionId, getCallingUid(), hasIt);
            }
        }

        public int startSession(IBinder activityToken, IBinder appCallback, AutofillId autofillId, Rect bounds, AutofillValue value, int userId, boolean hasCallback, int flags, ComponentName componentName, boolean compatMode) {
            Throwable th;
            String str;
            ComponentName componentName2;
            int i = userId;
            IBinder activityToken2 = (IBinder) Preconditions.checkNotNull(activityToken, "activityToken");
            IBinder appCallback2 = (IBinder) Preconditions.checkNotNull(appCallback, "appCallback");
            AutofillId autofillId2 = (AutofillId) Preconditions.checkNotNull(autofillId, "autoFillId");
            ComponentName componentName3 = (ComponentName) Preconditions.checkNotNull(componentName, "componentName");
            String packageName = (String) Preconditions.checkNotNull(componentName3.getPackageName());
            Preconditions.checkArgument(i == UserHandle.getUserId(getCallingUid()), "userId");
            try {
                AutofillManagerService.this.mContext.getPackageManager().getPackageInfoAsUser(packageName, 0, i);
                synchronized (AutofillManagerService.this.mLock) {
                    try {
                        AutofillManagerServiceImpl service = AutofillManagerService.this.getServiceForUserLocked(i);
                        int callingUid = getCallingUid();
                        boolean access$1000 = AutofillManagerService.this.mAllowInstantService;
                        int startSessionLocked = service.startSessionLocked(activityToken2, callingUid, appCallback2, autofillId2, bounds, value, hasCallback, componentName3, compatMode, access$1000, flags);
                        return startSessionLocked;
                    } catch (Throwable th2) {
                        th = th2;
                        throw th;
                    }
                }
            } catch (NameNotFoundException e) {
                str = packageName;
                componentName2 = componentName3;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(str);
                stringBuilder.append(" is not a valid package");
                throw new IllegalArgumentException(stringBuilder.toString(), e);
            }
        }

        /* JADX WARNING: Missing block: B:13:0x0039, code:
            return null;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public FillEventHistory getFillEventHistory() throws RemoteException {
            int userId = UserHandle.getCallingUserId();
            synchronized (AutofillManagerService.this.mLock) {
                AutofillManagerServiceImpl service = AutofillManagerService.this.peekServiceForUserLocked(userId);
                if (service != null) {
                    FillEventHistory fillEventHistory = service.getFillEventHistory(getCallingUid());
                    return fillEventHistory;
                } else if (Helper.sVerbose) {
                    String str = AutofillManagerService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("getFillEventHistory(): no service for ");
                    stringBuilder.append(userId);
                    Slog.v(str, stringBuilder.toString());
                }
            }
        }

        /* JADX WARNING: Missing block: B:13:0x0039, code:
            return null;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public UserData getUserData() throws RemoteException {
            int userId = UserHandle.getCallingUserId();
            synchronized (AutofillManagerService.this.mLock) {
                AutofillManagerServiceImpl service = AutofillManagerService.this.peekServiceForUserLocked(userId);
                if (service != null) {
                    UserData userData = service.getUserData(getCallingUid());
                    return userData;
                } else if (Helper.sVerbose) {
                    String str = AutofillManagerService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("getUserData(): no service for ");
                    stringBuilder.append(userId);
                    Slog.v(str, stringBuilder.toString());
                }
            }
        }

        /* JADX WARNING: Missing block: B:9:0x0024, code:
            return r3;
     */
        /* JADX WARNING: Missing block: B:14:0x0040, code:
            return null;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public String getUserDataId() throws RemoteException {
            int userId = UserHandle.getCallingUserId();
            synchronized (AutofillManagerService.this.mLock) {
                AutofillManagerServiceImpl service = AutofillManagerService.this.peekServiceForUserLocked(userId);
                String str = null;
                if (service != null) {
                    UserData userData = service.getUserData(getCallingUid());
                    if (userData != null) {
                        str = userData.getId();
                    }
                } else if (Helper.sVerbose) {
                    String str2 = AutofillManagerService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("getUserDataId(): no service for ");
                    stringBuilder.append(userId);
                    Slog.v(str2, stringBuilder.toString());
                }
            }
        }

        public void setUserData(UserData userData) throws RemoteException {
            int userId = UserHandle.getCallingUserId();
            synchronized (AutofillManagerService.this.mLock) {
                AutofillManagerServiceImpl service = AutofillManagerService.this.peekServiceForUserLocked(userId);
                if (service != null) {
                    service.setUserData(getCallingUid(), userData);
                } else if (Helper.sVerbose) {
                    String str = AutofillManagerService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("setUserData(): no service for ");
                    stringBuilder.append(userId);
                    Slog.v(str, stringBuilder.toString());
                }
            }
        }

        /* JADX WARNING: Missing block: B:13:0x0039, code:
            return false;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public boolean isFieldClassificationEnabled() throws RemoteException {
            int userId = UserHandle.getCallingUserId();
            synchronized (AutofillManagerService.this.mLock) {
                AutofillManagerServiceImpl service = AutofillManagerService.this.peekServiceForUserLocked(userId);
                if (service != null) {
                    boolean isFieldClassificationEnabled = service.isFieldClassificationEnabled(getCallingUid());
                    return isFieldClassificationEnabled;
                } else if (Helper.sVerbose) {
                    String str = AutofillManagerService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("isFieldClassificationEnabled(): no service for ");
                    stringBuilder.append(userId);
                    Slog.v(str, stringBuilder.toString());
                }
            }
        }

        public String getDefaultFieldClassificationAlgorithm() throws RemoteException {
            int userId = UserHandle.getCallingUserId();
            synchronized (AutofillManagerService.this.mLock) {
                AutofillManagerServiceImpl service = AutofillManagerService.this.peekServiceForUserLocked(userId);
                String defaultFieldClassificationAlgorithm;
                if (service != null) {
                    defaultFieldClassificationAlgorithm = service.getDefaultFieldClassificationAlgorithm(getCallingUid());
                    return defaultFieldClassificationAlgorithm;
                }
                if (Helper.sVerbose) {
                    defaultFieldClassificationAlgorithm = AutofillManagerService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("getDefaultFcAlgorithm(): no service for ");
                    stringBuilder.append(userId);
                    Slog.v(defaultFieldClassificationAlgorithm, stringBuilder.toString());
                }
                return null;
            }
        }

        public String[] getAvailableFieldClassificationAlgorithms() throws RemoteException {
            int userId = UserHandle.getCallingUserId();
            synchronized (AutofillManagerService.this.mLock) {
                AutofillManagerServiceImpl service = AutofillManagerService.this.peekServiceForUserLocked(userId);
                if (service != null) {
                    String[] availableFieldClassificationAlgorithms = service.getAvailableFieldClassificationAlgorithms(getCallingUid());
                    return availableFieldClassificationAlgorithms;
                }
                if (Helper.sVerbose) {
                    String str = AutofillManagerService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("getAvailableFcAlgorithms(): no service for ");
                    stringBuilder.append(userId);
                    Slog.v(str, stringBuilder.toString());
                }
                return null;
            }
        }

        /* JADX WARNING: Missing block: B:13:0x0035, code:
            return null;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public ComponentName getAutofillServiceComponentName() throws RemoteException {
            int userId = UserHandle.getCallingUserId();
            synchronized (AutofillManagerService.this.mLock) {
                AutofillManagerServiceImpl service = AutofillManagerService.this.peekServiceForUserLocked(userId);
                if (service != null) {
                    ComponentName serviceComponentName = service.getServiceComponentName();
                    return serviceComponentName;
                } else if (Helper.sVerbose) {
                    String str = AutofillManagerService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("getAutofillServiceComponentName(): no service for ");
                    stringBuilder.append(userId);
                    Slog.v(str, stringBuilder.toString());
                }
            }
        }

        /* JADX WARNING: Missing block: B:13:0x0052, code:
            return false;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public boolean restoreSession(int sessionId, IBinder activityToken, IBinder appCallback) throws RemoteException {
            int userId = UserHandle.getCallingUserId();
            activityToken = (IBinder) Preconditions.checkNotNull(activityToken, "activityToken");
            appCallback = (IBinder) Preconditions.checkNotNull(appCallback, "appCallback");
            synchronized (AutofillManagerService.this.mLock) {
                AutofillManagerServiceImpl service = (AutofillManagerServiceImpl) AutofillManagerService.this.mServicesCache.get(userId);
                if (service != null) {
                    boolean restoreSession = service.restoreSession(sessionId, getCallingUid(), activityToken, appCallback);
                    return restoreSession;
                } else if (Helper.sVerbose) {
                    String str = AutofillManagerService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("restoreSession(): no service for ");
                    stringBuilder.append(userId);
                    Slog.v(str, stringBuilder.toString());
                }
            }
        }

        public void updateSession(int sessionId, AutofillId autoFillId, Rect bounds, AutofillValue value, int action, int flags, int userId) {
            int i = userId;
            synchronized (AutofillManagerService.this.mLock) {
                AutofillManagerServiceImpl service = AutofillManagerService.this.peekServiceForUserLocked(i);
                if (service != null) {
                    service.updateSessionLocked(sessionId, getCallingUid(), autoFillId, bounds, value, action, flags);
                } else if (Helper.sVerbose) {
                    String str = AutofillManagerService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("updateSession(): no service for ");
                    stringBuilder.append(i);
                    Slog.v(str, stringBuilder.toString());
                }
            }
        }

        /* JADX WARNING: Missing block: B:12:0x0047, code:
            if (r14 == false) goto L_0x0062;
     */
        /* JADX WARNING: Missing block: B:14:0x0061, code:
            return startSession(r16, r17, r18, r19, r20, r13, r22, r23, r24, r27);
     */
        /* JADX WARNING: Missing block: B:15:0x0062, code:
            return r25;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public int updateOrRestartSession(IBinder activityToken, IBinder appCallback, AutofillId autoFillId, Rect bounds, AutofillValue value, int userId, boolean hasCallback, int flags, ComponentName componentName, int sessionId, int action, boolean compatMode) {
            Throwable th;
            int i = userId;
            boolean restart = false;
            synchronized (AutofillManagerService.this.mLock) {
                try {
                    AutofillManagerServiceImpl service = AutofillManagerService.this.peekServiceForUserLocked(i);
                    if (service != null) {
                        restart = service.updateSessionLocked(sessionId, getCallingUid(), autoFillId, bounds, value, action, flags);
                    } else if (Helper.sVerbose) {
                        String str = AutofillManagerService.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("updateOrRestartSession(): no service for ");
                        stringBuilder.append(i);
                        Slog.v(str, stringBuilder.toString());
                    }
                    boolean restart2 = restart;
                    try {
                    } catch (Throwable th2) {
                        th = th2;
                        restart = restart2;
                        throw th;
                    }
                } catch (Throwable th3) {
                    th = th3;
                    throw th;
                }
            }
        }

        public void setAutofillFailure(int sessionId, List<AutofillId> ids, int userId) {
            synchronized (AutofillManagerService.this.mLock) {
                AutofillManagerServiceImpl service = AutofillManagerService.this.peekServiceForUserLocked(userId);
                if (service != null) {
                    service.setAutofillFailureLocked(sessionId, getCallingUid(), ids);
                } else if (Helper.sVerbose) {
                    String str = AutofillManagerService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("setAutofillFailure(): no service for ");
                    stringBuilder.append(userId);
                    Slog.v(str, stringBuilder.toString());
                }
            }
        }

        public void finishSession(int sessionId, int userId) {
            synchronized (AutofillManagerService.this.mLock) {
                AutofillManagerServiceImpl service = AutofillManagerService.this.peekServiceForUserLocked(userId);
                if (service != null) {
                    service.finishSessionLocked(sessionId, getCallingUid());
                } else if (Helper.sVerbose) {
                    String str = AutofillManagerService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("finishSession(): no service for ");
                    stringBuilder.append(userId);
                    Slog.v(str, stringBuilder.toString());
                }
            }
        }

        public void cancelSession(int sessionId, int userId) {
            synchronized (AutofillManagerService.this.mLock) {
                AutofillManagerServiceImpl service = AutofillManagerService.this.peekServiceForUserLocked(userId);
                if (service != null) {
                    service.cancelSessionLocked(sessionId, getCallingUid());
                } else if (Helper.sVerbose) {
                    String str = AutofillManagerService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("cancelSession(): no service for ");
                    stringBuilder.append(userId);
                    Slog.v(str, stringBuilder.toString());
                }
            }
        }

        public void disableOwnedAutofillServices(int userId) {
            synchronized (AutofillManagerService.this.mLock) {
                AutofillManagerServiceImpl service = AutofillManagerService.this.peekServiceForUserLocked(userId);
                if (service != null) {
                    service.disableOwnedAutofillServicesLocked(Binder.getCallingUid());
                } else if (Helper.sVerbose) {
                    String str = AutofillManagerService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("cancelSession(): no service for ");
                    stringBuilder.append(userId);
                    Slog.v(str, stringBuilder.toString());
                }
            }
        }

        public boolean isServiceSupported(int userId) {
            boolean z;
            synchronized (AutofillManagerService.this.mLock) {
                z = AutofillManagerService.this.mDisabledUsers.get(userId) ^ 1;
            }
            return z;
        }

        public boolean isServiceEnabled(int userId, String packageName) {
            synchronized (AutofillManagerService.this.mLock) {
                AutofillManagerServiceImpl service = AutofillManagerService.this.peekServiceForUserLocked(userId);
                if (service != null) {
                    boolean equals = Objects.equals(packageName, service.getServicePackageName());
                    return equals;
                }
                if (Helper.sVerbose) {
                    String str = AutofillManagerService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("isServiceEnabled(): no service for ");
                    stringBuilder.append(userId);
                    Slog.v(str, stringBuilder.toString());
                }
                return false;
            }
        }

        public void onPendingSaveUi(int operation, IBinder token) {
            Preconditions.checkNotNull(token, "token");
            boolean z = operation == 1 || operation == 2;
            Preconditions.checkArgument(z, "invalid operation: %d", new Object[]{Integer.valueOf(operation)});
            synchronized (AutofillManagerService.this.mLock) {
                AutofillManagerServiceImpl service = AutofillManagerService.this.peekServiceForUserLocked(UserHandle.getCallingUserId());
                if (service != null) {
                    service.onPendingSaveUi(operation, token);
                }
            }
        }

        public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            if (DumpUtils.checkDumpPermission(AutofillManagerService.this.mContext, AutofillManagerService.TAG, pw)) {
                String arg;
                boolean showHistory = true;
                boolean uiOnly = false;
                int i = 0;
                if (args != null) {
                    boolean showHistory2 = true;
                    for (String arg2 : args) {
                        int i2 = -1;
                        int hashCode = arg2.hashCode();
                        if (hashCode != 900765093) {
                            if (hashCode != 1098711592) {
                                if (hashCode == 1333069025 && arg2.equals("--help")) {
                                    i2 = 2;
                                }
                            } else if (arg2.equals("--no-history")) {
                                i2 = 0;
                            }
                        } else if (arg2.equals("--ui-only")) {
                            i2 = true;
                        }
                        switch (i2) {
                            case 0:
                                showHistory2 = false;
                                break;
                            case 1:
                                uiOnly = true;
                                break;
                            case 2:
                                pw.println("Usage: dumpsys autofill [--ui-only|--no-history]");
                                return;
                            default:
                                String str = AutofillManagerService.TAG;
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("Ignoring invalid dump arg: ");
                                stringBuilder.append(arg2);
                                Slog.w(str, stringBuilder.toString());
                                break;
                        }
                    }
                    showHistory = showHistory2;
                }
                if (uiOnly) {
                    AutofillManagerService.this.mUi.dump(pw);
                    return;
                }
                boolean oldDebug = Helper.sDebug;
                String prefix = "  ";
                arg2 = "    ";
                try {
                    synchronized (AutofillManagerService.this.mLock) {
                        oldDebug = Helper.sDebug;
                        AutofillManagerService.this.setDebugLocked(true);
                        pw.print("Debug mode: ");
                        pw.println(oldDebug);
                        pw.print("Verbose mode: ");
                        pw.println(Helper.sVerbose);
                        pw.print("Disabled users: ");
                        pw.println(AutofillManagerService.this.mDisabledUsers);
                        pw.print("Max partitions per session: ");
                        pw.println(Helper.sPartitionMaxCount);
                        pw.print("Max visible datasets: ");
                        pw.println(Helper.sVisibleDatasetsMaxCount);
                        if (Helper.sFullScreenMode != null) {
                            pw.print("Overridden full-screen mode: ");
                            pw.println(Helper.sFullScreenMode);
                        }
                        pw.println("User data constraints: ");
                        UserData.dumpConstraints("  ", pw);
                        int size = AutofillManagerService.this.mServicesCache.size();
                        pw.print("Cached services: ");
                        if (size == 0) {
                            pw.println("none");
                        } else {
                            pw.println(size);
                            while (i < size) {
                                pw.print("\nService at index ");
                                pw.println(i);
                                ((AutofillManagerServiceImpl) AutofillManagerService.this.mServicesCache.valueAt(i)).dumpLocked("  ", pw);
                                i++;
                            }
                        }
                        AutofillManagerService.this.mUi.dump(pw);
                        pw.print("Autofill Compat State: ");
                        AutofillManagerService.this.mAutofillCompatState.dump("    ", pw);
                        pw.print("    ");
                        pw.print("from settings: ");
                        pw.println(AutofillManagerService.this.getWhitelistedCompatModePackagesFromSettings());
                        pw.print("Allow instant service: ");
                        pw.println(AutofillManagerService.this.mAllowInstantService);
                    }
                    if (showHistory) {
                        pw.println();
                        pw.println("Requests history:");
                        pw.println();
                        AutofillManagerService.this.mRequestsHistory.reverseDump(fd, pw, args);
                        pw.println();
                        pw.println("UI latency history:");
                        pw.println();
                        AutofillManagerService.this.mUiLatencyHistory.reverseDump(fd, pw, args);
                        pw.println();
                        pw.println("WTF history:");
                        pw.println();
                        AutofillManagerService.this.mWtfHistory.reverseDump(fd, pw, args);
                    }
                    AutofillManagerService.this.setDebugLocked(oldDebug);
                } catch (Throwable th) {
                    AutofillManagerService.this.setDebugLocked(oldDebug);
                }
            }
        }

        public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
            new AutofillManagerServiceShellCommand(AutofillManagerService.this).exec(this, in, out, err, args, callback, resultReceiver);
        }
    }

    static final class AutofillCompatState {
        private final Object mLock = new Object();
        @GuardedBy("mLock")
        private SparseArray<ArrayMap<String, PackageCompatState>> mUserSpecs;

        AutofillCompatState() {
        }

        /* JADX WARNING: Missing block: B:19:0x002b, code:
            return r2;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        boolean isCompatibilityModeRequested(String packageName, long versionCode, int userId) {
            synchronized (this.mLock) {
                boolean z = false;
                if (this.mUserSpecs == null) {
                    return false;
                }
                ArrayMap<String, PackageCompatState> userSpec = (ArrayMap) this.mUserSpecs.get(userId);
                if (userSpec == null) {
                    return false;
                }
                PackageCompatState metadata = (PackageCompatState) userSpec.get(packageName);
                if (metadata == null) {
                    return false;
                } else if (versionCode <= metadata.maxVersionCode) {
                    z = true;
                }
            }
        }

        String[] getUrlBarResourceIds(String packageName, int userId) {
            synchronized (this.mLock) {
                if (this.mUserSpecs == null) {
                    return null;
                }
                ArrayMap<String, PackageCompatState> userSpec = (ArrayMap) this.mUserSpecs.get(userId);
                if (userSpec == null) {
                    return null;
                }
                PackageCompatState metadata = (PackageCompatState) userSpec.get(packageName);
                if (metadata == null) {
                    return null;
                }
                String[] access$900 = metadata.urlBarResourceIds;
                return access$900;
            }
        }

        void addCompatibilityModeRequest(String packageName, long versionCode, String[] urlBarResourceIds, int userId) {
            synchronized (this.mLock) {
                if (this.mUserSpecs == null) {
                    this.mUserSpecs = new SparseArray();
                }
                ArrayMap<String, PackageCompatState> userSpec = (ArrayMap) this.mUserSpecs.get(userId);
                if (userSpec == null) {
                    userSpec = new ArrayMap();
                    this.mUserSpecs.put(userId, userSpec);
                }
                userSpec.put(packageName, new PackageCompatState(versionCode, urlBarResourceIds));
            }
        }

        void removeCompatibilityModeRequests(int userId) {
            synchronized (this.mLock) {
                if (this.mUserSpecs != null) {
                    this.mUserSpecs.remove(userId);
                    if (this.mUserSpecs.size() <= 0) {
                        this.mUserSpecs = null;
                    }
                }
            }
        }

        void reset(int userId) {
            synchronized (this.mLock) {
                if (this.mUserSpecs != null) {
                    this.mUserSpecs.delete(userId);
                    int newSize = this.mUserSpecs.size();
                    if (newSize == 0) {
                        if (Helper.sVerbose) {
                            Slog.v(AutofillManagerService.TAG, "reseting mUserSpecs");
                        }
                        this.mUserSpecs = null;
                    } else if (Helper.sVerbose) {
                        String str = AutofillManagerService.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("mUserSpecs down to ");
                        stringBuilder.append(newSize);
                        Slog.v(str, stringBuilder.toString());
                    }
                }
            }
        }

        private void dump(String prefix, PrintWriter pw) {
            if (this.mUserSpecs == null) {
                pw.println("N/A");
                return;
            }
            pw.println();
            String prefix2 = new StringBuilder();
            prefix2.append(prefix);
            prefix2.append("  ");
            prefix2 = prefix2.toString();
            for (int i = 0; i < this.mUserSpecs.size(); i++) {
                int user = this.mUserSpecs.keyAt(i);
                pw.print(prefix);
                pw.print("User: ");
                pw.println(user);
                ArrayMap<String, PackageCompatState> perUser = (ArrayMap) this.mUserSpecs.valueAt(i);
                for (int j = 0; j < perUser.size(); j++) {
                    String packageName = (String) perUser.keyAt(j);
                    PackageCompatState state = (PackageCompatState) perUser.valueAt(j);
                    pw.print(prefix2);
                    pw.print(packageName);
                    pw.print(": ");
                    pw.println(state);
                }
            }
        }
    }

    private final class LocalService extends AutofillManagerInternal {
        private LocalService() {
        }

        /* synthetic */ LocalService(AutofillManagerService x0, AnonymousClass1 x1) {
            this();
        }

        public void onBackKeyPressed() {
            if (Helper.sDebug) {
                Slog.d(AutofillManagerService.TAG, "onBackKeyPressed()");
            }
            AutofillManagerService.this.mUi.hideAll(null);
        }

        public boolean isCompatibilityModeRequested(String packageName, long versionCode, int userId) {
            return AutofillManagerService.this.mAutofillCompatState.isCompatibilityModeRequested(packageName, versionCode, userId);
        }
    }

    static final class PackageCompatState {
        private final long maxVersionCode;
        private final String[] urlBarResourceIds;

        PackageCompatState(long maxVersionCode, String[] urlBarResourceIds) {
            this.maxVersionCode = maxVersionCode;
            this.urlBarResourceIds = urlBarResourceIds;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("maxVersionCode=");
            stringBuilder.append(this.maxVersionCode);
            stringBuilder.append(", urlBarResourceIds=");
            stringBuilder.append(Arrays.toString(this.urlBarResourceIds));
            return stringBuilder.toString();
        }
    }

    private final class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
            ContentResolver resolver = AutofillManagerService.this.mContext.getContentResolver();
            resolver.registerContentObserver(Secure.getUriFor("autofill_service"), false, this, -1);
            resolver.registerContentObserver(Secure.getUriFor("user_setup_complete"), false, this, -1);
            resolver.registerContentObserver(Global.getUriFor("autofill_compat_mode_allowed_packages"), false, this, -1);
        }

        public void onChange(boolean selfChange, Uri uri, int userId) {
            if (Helper.sVerbose) {
                String str = AutofillManagerService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onChange(): uri=");
                stringBuilder.append(uri);
                stringBuilder.append(", userId=");
                stringBuilder.append(userId);
                Slog.v(str, stringBuilder.toString());
            }
            synchronized (AutofillManagerService.this.mLock) {
                AutofillManagerService.this.updateCachedServiceLocked(userId);
            }
        }
    }

    public AutofillManagerService(Context context) {
        super(context);
        this.mContext = context;
        this.mUi = new AutoFillUI(ActivityThread.currentActivityThread().getSystemUiContext());
        boolean debug = Build.IS_DEBUGGABLE;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Setting debug to ");
        stringBuilder.append(debug);
        Slog.i(str, stringBuilder.toString());
        setDebugLocked(debug);
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.CLOSE_SYSTEM_DIALOGS");
        this.mContext.registerReceiver(this.mBroadcastReceiver, filter, null, FgThread.getHandler());
        UserManagerInternal umi = (UserManagerInternal) LocalServices.getService(UserManagerInternal.class);
        List<UserInfo> users = ((UserManager) context.getSystemService(UserManager.class)).getUsers();
        for (int i = 0; i < users.size(); i++) {
            int userId = ((UserInfo) users.get(i)).id;
            boolean disabled = umi.getUserRestriction(userId, "no_autofill");
            if (disabled) {
                if (disabled) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Disabling Autofill for user ");
                    stringBuilder2.append(userId);
                    Slog.i(str2, stringBuilder2.toString());
                }
                this.mDisabledUsers.put(userId, disabled);
            }
        }
        umi.addUserRestrictionsListener(new -$$Lambda$AutofillManagerService$Yt8ZUfnHlFcXzCNLhvGde5dPRDA(this));
        startTrackingPackageChanges();
    }

    public static /* synthetic */ void lambda$new$0(AutofillManagerService autofillManagerService, int userId, Bundle newRestrictions, Bundle prevRestrictions) {
        boolean disabledNow = newRestrictions.getBoolean("no_autofill", false);
        synchronized (autofillManagerService.mLock) {
            String str;
            StringBuilder stringBuilder;
            if (autofillManagerService.mDisabledUsers.get(userId) == disabledNow && Helper.sDebug) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Autofill restriction did not change for user ");
                stringBuilder.append(userId);
                Slog.d(str, stringBuilder.toString());
                return;
            }
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Updating Autofill for user ");
            stringBuilder.append(userId);
            stringBuilder.append(": disabled=");
            stringBuilder.append(disabledNow);
            Slog.i(str, stringBuilder.toString());
            autofillManagerService.mDisabledUsers.put(userId, disabledNow);
            autofillManagerService.updateCachedServiceLocked(userId, disabledNow);
        }
    }

    private void startTrackingPackageChanges() {
        new PackageMonitor() {
            public void onSomePackagesChanged() {
                synchronized (AutofillManagerService.this.mLock) {
                    AutofillManagerService.this.updateCachedServiceLocked(getChangingUserId());
                }
            }

            public void onPackageUpdateFinished(String packageName, int uid) {
                synchronized (AutofillManagerService.this.mLock) {
                    if (packageName.equals(getActiveAutofillServicePackageName())) {
                        AutofillManagerService.this.removeCachedServiceLocked(getChangingUserId());
                    } else {
                        handlePackageUpdateLocked(packageName);
                    }
                }
            }

            public void onPackageRemoved(String packageName, int uid) {
                synchronized (AutofillManagerService.this.mLock) {
                    int userId = getChangingUserId();
                    AutofillManagerServiceImpl userState = AutofillManagerService.this.peekServiceForUserLocked(userId);
                    if (userState != null) {
                        ComponentName componentName = userState.getServiceComponentName();
                        if (componentName != null && packageName.equals(componentName.getPackageName())) {
                            handleActiveAutofillServiceRemoved(userId);
                        }
                    }
                }
            }

            public boolean onHandleForceStop(Intent intent, String[] packages, int uid, boolean doit) {
                synchronized (AutofillManagerService.this.mLock) {
                    String activePackageName = getActiveAutofillServicePackageName();
                    for (String pkg : packages) {
                        if (!pkg.equals(activePackageName)) {
                            handlePackageUpdateLocked(pkg);
                        } else if (doit) {
                            AutofillManagerService.this.removeCachedServiceLocked(getChangingUserId());
                        } else {
                            return true;
                        }
                    }
                    return false;
                }
            }

            private void handleActiveAutofillServiceRemoved(int userId) {
                AutofillManagerService.this.removeCachedServiceLocked(userId);
                Secure.putStringForUser(AutofillManagerService.this.mContext.getContentResolver(), "autofill_service", null, userId);
            }

            private String getActiveAutofillServicePackageName() {
                AutofillManagerServiceImpl userState = AutofillManagerService.this.peekServiceForUserLocked(getChangingUserId());
                if (userState == null) {
                    return null;
                }
                ComponentName serviceComponent = userState.getServiceComponentName();
                if (serviceComponent == null) {
                    return null;
                }
                return serviceComponent.getPackageName();
            }

            @GuardedBy("mLock")
            private void handlePackageUpdateLocked(String packageName) {
                int size = AutofillManagerService.this.mServicesCache.size();
                for (int i = 0; i < size; i++) {
                    ((AutofillManagerServiceImpl) AutofillManagerService.this.mServicesCache.valueAt(i)).handlePackageUpdateLocked(packageName);
                }
            }
        }.register(this.mContext, null, UserHandle.ALL, true);
    }

    public void onStart() {
        publishBinderService("autofill", new AutoFillManagerServiceStub());
        publishLocalService(AutofillManagerInternal.class, this.mLocalService);
    }

    public void onBootPhase(int phase) {
        if (phase == 600) {
            SettingsObserver settingsObserver = new SettingsObserver(BackgroundThread.getHandler());
        }
    }

    public void onUnlockUser(int userId) {
        synchronized (this.mLock) {
            updateCachedServiceLocked(userId);
        }
    }

    public void onSwitchUser(int userHandle) {
        if (Helper.sDebug) {
            Slog.d(TAG, "Hiding UI when user switched");
        }
        this.mUi.hideAll(null);
    }

    public void onCleanupUser(int userId) {
        synchronized (this.mLock) {
            removeCachedServiceLocked(userId);
        }
    }

    @GuardedBy("mLock")
    AutofillManagerServiceImpl getServiceForUserLocked(int userId) {
        int i = userId;
        int resolvedUserId = ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), i, false, false, null, null);
        AutofillManagerServiceImpl service = (AutofillManagerServiceImpl) this.mServicesCache.get(resolvedUserId);
        if (service != null) {
            return service;
        }
        Context context = this.mContext;
        Object obj = this.mLock;
        LocalLog localLog = this.mRequestsHistory;
        LocalLog localLog2 = this.mUiLatencyHistory;
        LocalLog localLog3 = this.mWtfHistory;
        AutoFillUI autoFillUI = this.mUi;
        service = new AutofillManagerServiceImpl(context, obj, localLog, localLog2, localLog3, resolvedUserId, autoFillUI, this.mAutofillCompatState, this.mDisabledUsers.get(resolvedUserId));
        this.mServicesCache.put(i, service);
        addCompatibilityModeRequestsLocked(service, i);
        return service;
    }

    @GuardedBy("mLock")
    AutofillManagerServiceImpl peekServiceForUserLocked(int userId) {
        return (AutofillManagerServiceImpl) this.mServicesCache.get(ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, false, false, null, null));
    }

    void destroySessions(int userId, IResultReceiver receiver) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("destroySessions() for userId ");
        stringBuilder.append(userId);
        Slog.i(str, stringBuilder.toString());
        this.mContext.enforceCallingPermission("android.permission.MANAGE_AUTO_FILL", TAG);
        synchronized (this.mLock) {
            if (userId != -1) {
                AutofillManagerServiceImpl service = peekServiceForUserLocked(userId);
                if (service != null) {
                    service.destroySessionsLocked();
                }
            } else {
                int size = this.mServicesCache.size();
                for (int i = 0; i < size; i++) {
                    ((AutofillManagerServiceImpl) this.mServicesCache.valueAt(i)).destroySessionsLocked();
                }
            }
        }
        try {
            receiver.send(0, new Bundle());
        } catch (RemoteException e) {
        }
    }

    void listSessions(int userId, IResultReceiver receiver) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("listSessions() for userId ");
        stringBuilder.append(userId);
        Slog.i(str, stringBuilder.toString());
        this.mContext.enforceCallingPermission("android.permission.MANAGE_AUTO_FILL", TAG);
        Bundle resultData = new Bundle();
        ArrayList<String> sessions = new ArrayList();
        synchronized (this.mLock) {
            if (userId != -1) {
                AutofillManagerServiceImpl service = peekServiceForUserLocked(userId);
                if (service != null) {
                    service.listSessionsLocked(sessions);
                }
            } else {
                int size = this.mServicesCache.size();
                for (int i = 0; i < size; i++) {
                    ((AutofillManagerServiceImpl) this.mServicesCache.valueAt(i)).listSessionsLocked(sessions);
                }
            }
        }
        resultData.putStringArrayList(RECEIVER_BUNDLE_EXTRA_SESSIONS, sessions);
        try {
            receiver.send(0, resultData);
        } catch (RemoteException e) {
        }
    }

    void reset() {
        Slog.i(TAG, "reset()");
        this.mContext.enforceCallingPermission("android.permission.MANAGE_AUTO_FILL", TAG);
        synchronized (this.mLock) {
            int size = this.mServicesCache.size();
            for (int i = 0; i < size; i++) {
                ((AutofillManagerServiceImpl) this.mServicesCache.valueAt(i)).destroyLocked();
            }
            this.mServicesCache.clear();
        }
    }

    void setLogLevel(int level) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setLogLevel(): ");
        stringBuilder.append(level);
        Slog.i(str, stringBuilder.toString());
        this.mContext.enforceCallingPermission("android.permission.MANAGE_AUTO_FILL", TAG);
        boolean debug = false;
        boolean verbose = false;
        if (level == 4) {
            verbose = true;
            debug = true;
        } else if (level == 2) {
            debug = true;
        }
        synchronized (this.mLock) {
            setDebugLocked(debug);
            setVerboseLocked(verbose);
        }
    }

    int getLogLevel() {
        this.mContext.enforceCallingPermission("android.permission.MANAGE_AUTO_FILL", TAG);
        synchronized (this.mLock) {
            if (Helper.sVerbose) {
                return 4;
            } else if (Helper.sDebug) {
                return 2;
            } else {
                return 0;
            }
        }
    }

    int getMaxPartitions() {
        int i;
        this.mContext.enforceCallingPermission("android.permission.MANAGE_AUTO_FILL", TAG);
        synchronized (this.mLock) {
            i = Helper.sPartitionMaxCount;
        }
        return i;
    }

    void setMaxPartitions(int max) {
        this.mContext.enforceCallingPermission("android.permission.MANAGE_AUTO_FILL", TAG);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setMaxPartitions(): ");
        stringBuilder.append(max);
        Slog.i(str, stringBuilder.toString());
        synchronized (this.mLock) {
            Helper.sPartitionMaxCount = max;
        }
    }

    int getMaxVisibleDatasets() {
        int i;
        this.mContext.enforceCallingPermission("android.permission.MANAGE_AUTO_FILL", TAG);
        synchronized (this.mLock) {
            i = Helper.sVisibleDatasetsMaxCount;
        }
        return i;
    }

    void setMaxVisibleDatasets(int max) {
        this.mContext.enforceCallingPermission("android.permission.MANAGE_AUTO_FILL", TAG);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setMaxVisibleDatasets(): ");
        stringBuilder.append(max);
        Slog.i(str, stringBuilder.toString());
        synchronized (this.mLock) {
            Helper.sVisibleDatasetsMaxCount = max;
        }
    }

    void getScore(String algorithmName, String value1, String value2, RemoteCallback callback) {
        this.mContext.enforceCallingPermission("android.permission.MANAGE_AUTO_FILL", TAG);
        new FieldClassificationStrategy(this.mContext, -2).getScores(callback, algorithmName, null, Arrays.asList(new AutofillValue[]{AutofillValue.forText(value1)}), new String[]{value2});
    }

    Boolean getFullScreenMode() {
        this.mContext.enforceCallingPermission("android.permission.MANAGE_AUTO_FILL", TAG);
        return Helper.sFullScreenMode;
    }

    void setFullScreenMode(Boolean mode) {
        this.mContext.enforceCallingPermission("android.permission.MANAGE_AUTO_FILL", TAG);
        Helper.sFullScreenMode = mode;
    }

    boolean getAllowInstantService() {
        boolean z;
        this.mContext.enforceCallingPermission("android.permission.MANAGE_AUTO_FILL", TAG);
        synchronized (this.mLock) {
            z = this.mAllowInstantService;
        }
        return z;
    }

    void setAllowInstantService(boolean mode) {
        this.mContext.enforceCallingPermission("android.permission.MANAGE_AUTO_FILL", TAG);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setAllowInstantService(): ");
        stringBuilder.append(mode);
        Slog.i(str, stringBuilder.toString());
        synchronized (this.mLock) {
            this.mAllowInstantService = mode;
        }
    }

    private void setDebugLocked(boolean debug) {
        Helper.sDebug = debug;
        Helper.sDebug = debug;
    }

    private void setVerboseLocked(boolean verbose) {
        Helper.sVerbose = verbose;
        Helper.sVerbose = verbose;
    }

    @GuardedBy("mLock")
    private void removeCachedServiceLocked(int userId) {
        AutofillManagerServiceImpl service = peekServiceForUserLocked(userId);
        if (service != null) {
            this.mServicesCache.delete(userId);
            service.destroyLocked();
            this.mAutofillCompatState.removeCompatibilityModeRequests(userId);
        }
    }

    @GuardedBy("mLock")
    private void updateCachedServiceLocked(int userId) {
        updateCachedServiceLocked(userId, this.mDisabledUsers.get(userId));
    }

    @GuardedBy("mLock")
    private void updateCachedServiceLocked(int userId, boolean disabled) {
        AutofillManagerServiceImpl service = getServiceForUserLocked(userId);
        if (service != null) {
            service.destroySessionsLocked();
            service.updateLocked(disabled);
            if (service.isEnabledLocked()) {
                addCompatibilityModeRequestsLocked(service, userId);
            } else {
                removeCachedServiceLocked(userId);
            }
        }
    }

    private void addCompatibilityModeRequestsLocked(AutofillManagerServiceImpl service, int userId) {
        this.mAutofillCompatState.reset(userId);
        ArrayMap<String, Long> compatPackages = service.getCompatibilityPackagesLocked();
        if (compatPackages != null && !compatPackages.isEmpty()) {
            Map<String, String[]> whiteListedPackages = getWhitelistedCompatModePackages();
            int compatPackageCount = compatPackages.size();
            for (int i = 0; i < compatPackageCount; i++) {
                String packageName = (String) compatPackages.keyAt(i);
                if (whiteListedPackages == null || !whiteListedPackages.containsKey(packageName)) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Ignoring not whitelisted compat package ");
                    stringBuilder.append(packageName);
                    Slog.w(str, stringBuilder.toString());
                } else {
                    Long maxVersionCode = (Long) compatPackages.valueAt(i);
                    if (maxVersionCode != null) {
                        this.mAutofillCompatState.addCompatibilityModeRequest(packageName, maxVersionCode.longValue(), (String[]) whiteListedPackages.get(packageName), userId);
                    }
                }
            }
        }
    }

    private String getWhitelistedCompatModePackagesFromSettings() {
        return Global.getString(this.mContext.getContentResolver(), "autofill_compat_mode_allowed_packages");
    }

    private Map<String, String[]> getWhitelistedCompatModePackages() {
        return getWhitelistedCompatModePackages(getWhitelistedCompatModePackagesFromSettings());
    }

    @VisibleForTesting
    static Map<String, String[]> getWhitelistedCompatModePackages(String setting) {
        if (TextUtils.isEmpty(setting)) {
            return null;
        }
        ArrayMap<String, String[]> compatPackages = new ArrayMap();
        SimpleStringSplitter splitter = new SimpleStringSplitter(COMPAT_PACKAGE_DELIMITER);
        splitter.setString(setting);
        while (splitter.hasNext()) {
            String packageName;
            List<String> urlBarIds;
            String packageBlock = splitter.next();
            int urlBlockIndex = packageBlock.indexOf(91);
            if (urlBlockIndex == -1) {
                packageName = packageBlock;
                urlBarIds = null;
            } else if (packageBlock.charAt(packageBlock.length() - 1) != COMPAT_PACKAGE_URL_IDS_BLOCK_END) {
                packageName = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Ignoring entry '");
                stringBuilder.append(packageBlock);
                stringBuilder.append("' on '");
                stringBuilder.append(setting);
                stringBuilder.append("'because it does not end on '");
                stringBuilder.append(COMPAT_PACKAGE_URL_IDS_BLOCK_END);
                stringBuilder.append("'");
                Slog.w(packageName, stringBuilder.toString());
            } else {
                packageName = packageBlock.substring(0, urlBlockIndex);
                urlBarIds = new ArrayList();
                String urlBarIdsBlock = packageBlock.substring(urlBlockIndex + 1, packageBlock.length() - 1);
                if (Helper.sVerbose) {
                    String str = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("pkg:");
                    stringBuilder2.append(packageName);
                    stringBuilder2.append(": block:");
                    stringBuilder2.append(packageBlock);
                    stringBuilder2.append(": urls:");
                    stringBuilder2.append(urlBarIds);
                    stringBuilder2.append(": block:");
                    stringBuilder2.append(urlBarIdsBlock);
                    stringBuilder2.append(":");
                    Slog.v(str, stringBuilder2.toString());
                }
                SimpleStringSplitter splitter2 = new SimpleStringSplitter(COMPAT_PACKAGE_URL_IDS_DELIMITER);
                splitter2.setString(urlBarIdsBlock);
                while (splitter2.hasNext()) {
                    urlBarIds.add(splitter2.next());
                }
            }
            if (urlBarIds == null) {
                compatPackages.put(packageName, null);
            } else {
                String[] urlBarIdsArray = new String[urlBarIds.size()];
                urlBarIds.toArray(urlBarIdsArray);
                compatPackages.put(packageName, urlBarIdsArray);
            }
        }
        return compatPackages;
    }
}
