package com.android.server.timezone;

import android.app.timezone.DistroFormatVersion;
import android.app.timezone.DistroRulesVersion;
import android.app.timezone.ICallback;
import android.app.timezone.IRulesManager.Stub;
import android.app.timezone.RulesState;
import android.content.Context;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Slog;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.annotations.VisibleForTesting.Visibility;
import com.android.server.EventLogTags;
import com.android.server.SystemService;
import com.android.timezone.distro.DistroVersion;
import com.android.timezone.distro.StagedDistroOperation;
import com.android.timezone.distro.TimeZoneDistro;
import com.android.timezone.distro.installer.TimeZoneDistroInstaller;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import libcore.icu.ICU;
import libcore.util.TimeZoneFinder;
import libcore.util.ZoneInfoDB;

public final class RulesManagerService extends Stub {
    @VisibleForTesting(visibility = Visibility.PRIVATE)
    static final DistroFormatVersion DISTRO_FORMAT_VERSION_SUPPORTED = new DistroFormatVersion(2, 1);
    @VisibleForTesting(visibility = Visibility.PRIVATE)
    static final String REQUIRED_QUERY_PERMISSION = "android.permission.QUERY_TIME_ZONE_RULES";
    @VisibleForTesting(visibility = Visibility.PRIVATE)
    static final String REQUIRED_UPDATER_PERMISSION = "android.permission.UPDATE_TIME_ZONE_RULES";
    private static final File SYSTEM_TZ_DATA_FILE = new File("/system/usr/share/zoneinfo/tzdata");
    private static final String TAG = "timezone.RulesManagerService";
    private static final File TZ_DATA_DIR = new File("/data/misc/zoneinfo");
    private final Executor mExecutor;
    private final TimeZoneDistroInstaller mInstaller;
    private final RulesManagerIntentHelper mIntentHelper;
    private final AtomicBoolean mOperationInProgress = new AtomicBoolean(false);
    private final PackageTracker mPackageTracker;
    private final PermissionHelper mPermissionHelper;

    private class InstallRunnable implements Runnable {
        private final ICallback mCallback;
        private final CheckToken mCheckToken;
        private final ParcelFileDescriptor mDistroParcelFileDescriptor;

        InstallRunnable(ParcelFileDescriptor distroParcelFileDescriptor, CheckToken checkToken, ICallback callback) {
            this.mDistroParcelFileDescriptor = distroParcelFileDescriptor;
            this.mCheckToken = checkToken;
            this.mCallback = callback;
        }

        public void run() {
            EventLogTags.writeTimezoneInstallStarted(RulesManagerService.toStringOrNull(this.mCheckToken));
            boolean success = false;
            ParcelFileDescriptor pfd;
            try {
                pfd = this.mDistroParcelFileDescriptor;
                int installerResult = RulesManagerService.this.mInstaller.stageInstallWithErrorCode(new TimeZoneDistro(new FileInputStream(pfd.getFileDescriptor(), false)));
                sendInstallNotificationIntentIfRequired(installerResult);
                int resultCode = mapInstallerResultToApiCode(installerResult);
                EventLogTags.writeTimezoneInstallComplete(RulesManagerService.toStringOrNull(this.mCheckToken), resultCode);
                RulesManagerService.this.sendFinishedStatus(this.mCallback, resultCode);
                success = true;
                if (pfd != null) {
                    pfd.close();
                }
            } catch (Exception e) {
                try {
                    Slog.w(RulesManagerService.TAG, "Failed to install distro.", e);
                    EventLogTags.writeTimezoneInstallComplete(RulesManagerService.toStringOrNull(this.mCheckToken), 1);
                    RulesManagerService.this.sendFinishedStatus(this.mCallback, 1);
                } catch (Throwable th) {
                    RulesManagerService.this.mPackageTracker.recordCheckResult(this.mCheckToken, success);
                    RulesManagerService.this.mOperationInProgress.set(false);
                }
            } catch (Throwable th2) {
                r3.addSuppressed(th2);
            }
            RulesManagerService.this.mPackageTracker.recordCheckResult(this.mCheckToken, success);
            RulesManagerService.this.mOperationInProgress.set(false);
        }

        private void sendInstallNotificationIntentIfRequired(int installerResult) {
            if (installerResult == 0) {
                RulesManagerService.this.mIntentHelper.sendTimeZoneOperationStaged();
            }
        }

        private int mapInstallerResultToApiCode(int installerResult) {
            switch (installerResult) {
                case 0:
                    return 0;
                case 1:
                    return 2;
                case 2:
                    return 3;
                case 3:
                    return 4;
                case 4:
                    return 5;
                default:
                    return 1;
            }
        }
    }

    private class UninstallRunnable implements Runnable {
        private final ICallback mCallback;
        private final CheckToken mCheckToken;

        UninstallRunnable(CheckToken checkToken, ICallback callback) {
            this.mCheckToken = checkToken;
            this.mCallback = callback;
        }

        public void run() {
            EventLogTags.writeTimezoneUninstallStarted(RulesManagerService.toStringOrNull(this.mCheckToken));
            boolean packageTrackerStatus = false;
            try {
                int uninstallResult = RulesManagerService.this.mInstaller.stageUninstall();
                sendUninstallNotificationIntentIfRequired(uninstallResult);
                boolean z = uninstallResult == 0 || uninstallResult == 1;
                packageTrackerStatus = z;
                int callbackResultCode = packageTrackerStatus ? 0 : 1;
                EventLogTags.writeTimezoneUninstallComplete(RulesManagerService.toStringOrNull(this.mCheckToken), callbackResultCode);
                RulesManagerService.this.sendFinishedStatus(this.mCallback, callbackResultCode);
            } catch (Exception e) {
                EventLogTags.writeTimezoneUninstallComplete(RulesManagerService.toStringOrNull(this.mCheckToken), 1);
                Slog.w(RulesManagerService.TAG, "Failed to uninstall distro.", e);
                RulesManagerService.this.sendFinishedStatus(this.mCallback, 1);
            } catch (Throwable th) {
                RulesManagerService.this.mPackageTracker.recordCheckResult(this.mCheckToken, packageTrackerStatus);
                RulesManagerService.this.mOperationInProgress.set(false);
            }
            RulesManagerService.this.mPackageTracker.recordCheckResult(this.mCheckToken, packageTrackerStatus);
            RulesManagerService.this.mOperationInProgress.set(false);
        }

        private void sendUninstallNotificationIntentIfRequired(int uninstallResult) {
            switch (uninstallResult) {
                case 0:
                    RulesManagerService.this.mIntentHelper.sendTimeZoneOperationStaged();
                    return;
                case 1:
                    RulesManagerService.this.mIntentHelper.sendTimeZoneOperationUnstaged();
                    return;
                default:
                    return;
            }
        }
    }

    public static class Lifecycle extends SystemService {
        public Lifecycle(Context context) {
            super(context);
        }

        public void onStart() {
            RulesManagerService service = RulesManagerService.create(getContext());
            service.start();
            publishBinderService("timezone", service);
            publishLocalService(RulesManagerService.class, service);
        }
    }

    private static RulesManagerService create(Context context) {
        RulesManagerServiceHelperImpl helper = new RulesManagerServiceHelperImpl(context);
        return new RulesManagerService(helper, helper, helper, PackageTracker.create(context), new TimeZoneDistroInstaller(TAG, SYSTEM_TZ_DATA_FILE, TZ_DATA_DIR));
    }

    @VisibleForTesting(visibility = Visibility.PRIVATE)
    RulesManagerService(PermissionHelper permissionHelper, Executor executor, RulesManagerIntentHelper intentHelper, PackageTracker packageTracker, TimeZoneDistroInstaller timeZoneDistroInstaller) {
        this.mPermissionHelper = permissionHelper;
        this.mExecutor = executor;
        this.mIntentHelper = intentHelper;
        this.mPackageTracker = packageTracker;
        this.mInstaller = timeZoneDistroInstaller;
    }

    public void start() {
        this.mPackageTracker.start();
    }

    public RulesState getRulesState() {
        this.mPermissionHelper.enforceCallerHasPermission(REQUIRED_QUERY_PERMISSION);
        return getRulesStateInternal();
    }

    /* JADX WARNING: Removed duplicated region for block: B:11:0x0023 A:{Splitter: B:5:0x000c, ExcHandler: com.android.timezone.distro.DistroException (r2_2 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:26:0x0055 A:{Splitter: B:17:0x0037, ExcHandler: com.android.timezone.distro.DistroException (r5_4 'e' java.lang.Exception)} */
    /* JADX WARNING: Missing block: B:11:0x0023, code:
            r2 = move-exception;
     */
    /* JADX WARNING: Missing block: B:13:?, code:
            android.util.Slog.w(TAG, "Failed to read installed distro.", r2);
     */
    /* JADX WARNING: Missing block: B:26:0x0055, code:
            r5 = move-exception;
     */
    /* JADX WARNING: Missing block: B:28:?, code:
            android.util.Slog.w(TAG, "Failed to read staged distro.", r5);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private RulesState getRulesStateInternal() {
        RulesState rulesState;
        synchronized (this) {
            DistroRulesVersion installedDistroRulesVersion = null;
            try {
                String systemRulesVersion = this.mInstaller.getSystemRulesVersion();
                int distroStatus = 0;
                try {
                    DistroVersion installedDistroVersion = this.mInstaller.getInstalledDistroVersion();
                    if (installedDistroVersion == null) {
                        distroStatus = 1;
                        installedDistroRulesVersion = null;
                    } else {
                        distroStatus = 2;
                        installedDistroRulesVersion = new DistroRulesVersion(installedDistroVersion.rulesVersion, installedDistroVersion.revision);
                    }
                } catch (Exception e) {
                }
                boolean operationInProgress = this.mOperationInProgress.get();
                DistroRulesVersion stagedDistroRulesVersion = null;
                int stagedOperationStatus = 0;
                if (!operationInProgress) {
                    try {
                        StagedDistroOperation stagedDistroOperation = this.mInstaller.getStagedDistroOperation();
                        if (stagedDistroOperation == null) {
                            stagedOperationStatus = 1;
                        } else if (stagedDistroOperation.isUninstall) {
                            stagedOperationStatus = 2;
                        } else {
                            stagedOperationStatus = 3;
                            DistroVersion stagedDistroVersion = stagedDistroOperation.distroVersion;
                            stagedDistroRulesVersion = new DistroRulesVersion(stagedDistroVersion.rulesVersion, stagedDistroVersion.revision);
                        }
                    } catch (Exception e2) {
                    }
                }
                int stagedOperationStatus2 = stagedOperationStatus;
                rulesState = new RulesState(systemRulesVersion, DISTRO_FORMAT_VERSION_SUPPORTED, operationInProgress, stagedOperationStatus2, stagedDistroRulesVersion, distroStatus, installedDistroRulesVersion);
            } catch (IOException e3) {
                Slog.w(TAG, "Failed to read system rules", e3);
                return null;
            }
        }
        return rulesState;
    }

    /* JADX WARNING: Missing block: B:13:0x0026, code:
            if (r8 == null) goto L_0x0037;
     */
    /* JADX WARNING: Missing block: B:14:0x0028, code:
            if (r1 == false) goto L_0x0037;
     */
    /* JADX WARNING: Missing block: B:16:?, code:
            r8.close();
     */
    /* JADX WARNING: Missing block: B:17:0x002e, code:
            r3 = move-exception;
     */
    /* JADX WARNING: Missing block: B:18:0x002f, code:
            android.util.Slog.w(TAG, "Failed to close distroParcelFileDescriptor", r3);
     */
    /* JADX WARNING: Missing block: B:25:0x004a, code:
            if (r8 == null) goto L_0x005b;
     */
    /* JADX WARNING: Missing block: B:26:0x004c, code:
            if (null == null) goto L_0x005b;
     */
    /* JADX WARNING: Missing block: B:28:?, code:
            r8.close();
     */
    /* JADX WARNING: Missing block: B:29:0x0052, code:
            r3 = move-exception;
     */
    /* JADX WARNING: Missing block: B:30:0x0053, code:
            android.util.Slog.w(TAG, "Failed to close distroParcelFileDescriptor", r3);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int requestInstall(ParcelFileDescriptor distroParcelFileDescriptor, byte[] checkTokenBytes, ICallback callback) {
        Throwable th;
        boolean closeParcelFileDescriptorOnExit = true;
        try {
            this.mPermissionHelper.enforceCallerHasPermission(REQUIRED_UPDATER_PERMISSION);
            CheckToken checkToken = null;
            if (checkTokenBytes != null) {
                checkToken = createCheckTokenOrThrow(checkTokenBytes);
            }
            EventLogTags.writeTimezoneRequestInstall(toStringOrNull(checkToken));
            synchronized (this) {
                if (distroParcelFileDescriptor == null) {
                    throw new NullPointerException("distroParcelFileDescriptor == null");
                } else if (callback != null) {
                    try {
                        if (this.mOperationInProgress.get()) {
                        } else {
                            this.mOperationInProgress.set(true);
                            this.mExecutor.execute(new InstallRunnable(distroParcelFileDescriptor, checkToken, callback));
                            try {
                            } catch (Throwable th2) {
                                Throwable th3 = th2;
                                closeParcelFileDescriptorOnExit = false;
                                th = th3;
                                throw th;
                            }
                        }
                    } catch (Throwable th4) {
                        th = th4;
                        throw th;
                    }
                } else {
                    throw new NullPointerException("observer == null");
                }
            }
            return 1;
            return 0;
        } catch (Throwable th5) {
            if (distroParcelFileDescriptor != null && closeParcelFileDescriptorOnExit) {
                try {
                    distroParcelFileDescriptor.close();
                } catch (IOException e) {
                    Slog.w(TAG, "Failed to close distroParcelFileDescriptor", e);
                }
            }
        }
    }

    public int requestUninstall(byte[] checkTokenBytes, ICallback callback) {
        this.mPermissionHelper.enforceCallerHasPermission(REQUIRED_UPDATER_PERMISSION);
        CheckToken checkToken = null;
        if (checkTokenBytes != null) {
            checkToken = createCheckTokenOrThrow(checkTokenBytes);
        }
        EventLogTags.writeTimezoneRequestUninstall(toStringOrNull(checkToken));
        synchronized (this) {
            if (callback == null) {
                throw new NullPointerException("callback == null");
            } else if (this.mOperationInProgress.get()) {
                return 1;
            } else {
                this.mOperationInProgress.set(true);
                this.mExecutor.execute(new UninstallRunnable(checkToken, callback));
                return 0;
            }
        }
    }

    private void sendFinishedStatus(ICallback callback, int resultCode) {
        try {
            callback.onFinished(resultCode);
        } catch (RemoteException e) {
            Slog.e(TAG, "Unable to notify observer of result", e);
        }
    }

    public void requestNothing(byte[] checkTokenBytes, boolean success) {
        this.mPermissionHelper.enforceCallerHasPermission(REQUIRED_UPDATER_PERMISSION);
        CheckToken checkToken = null;
        if (checkTokenBytes != null) {
            checkToken = createCheckTokenOrThrow(checkTokenBytes);
        }
        EventLogTags.writeTimezoneRequestNothing(toStringOrNull(checkToken));
        this.mPackageTracker.recordCheckResult(checkToken, success);
        EventLogTags.writeTimezoneNothingComplete(toStringOrNull(checkToken));
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (this.mPermissionHelper.checkDumpPermission(TAG, pw)) {
            RulesState rulesState = getRulesStateInternal();
            if (args != null && args.length == 2) {
                int i = 0;
                if ("-format_state".equals(args[0]) && args[1] != null) {
                    char[] toCharArray = args[1].toCharArray();
                    int length = toCharArray.length;
                    while (i < length) {
                        char c = toCharArray[i];
                        StringBuilder stringBuilder;
                        String value;
                        StringBuilder stringBuilder2;
                        DistroRulesVersion installedRulesVersion;
                        switch (c) {
                            case HdmiCecKeycode.CEC_KEYCODE_PAUSE_PLAY_FUNCTION /*97*/:
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("Active rules version (ICU, ZoneInfoDB, TimeZoneFinder): ");
                                stringBuilder.append(ICU.getTZDataVersion());
                                stringBuilder.append(",");
                                stringBuilder.append(ZoneInfoDB.getInstance().getVersion());
                                stringBuilder.append(",");
                                stringBuilder.append(TimeZoneFinder.getInstance().getIanaVersion());
                                pw.println(stringBuilder.toString());
                                break;
                            case HdmiCecKeycode.CEC_KEYCODE_PAUSE_RECORD_FUNCTION /*99*/:
                                value = "Unknown";
                                if (rulesState != null) {
                                    value = distroStatusToString(rulesState.getDistroStatus());
                                }
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("Current install state: ");
                                stringBuilder2.append(value);
                                pw.println(stringBuilder2.toString());
                                break;
                            case 'i':
                                value = "Unknown";
                                if (rulesState != null) {
                                    installedRulesVersion = rulesState.getInstalledDistroRulesVersion();
                                    if (installedRulesVersion == null) {
                                        value = "<None>";
                                    } else {
                                        value = installedRulesVersion.toDumpString();
                                    }
                                }
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("Installed rules version: ");
                                stringBuilder2.append(value);
                                pw.println(stringBuilder2.toString());
                                break;
                            case 'o':
                                value = "Unknown";
                                if (rulesState != null) {
                                    value = stagedOperationToString(rulesState.getStagedOperationType());
                                }
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("Staged operation: ");
                                stringBuilder2.append(value);
                                pw.println(stringBuilder2.toString());
                                break;
                            case 'p':
                                value = "Unknown";
                                if (rulesState != null) {
                                    value = Boolean.toString(rulesState.isOperationInProgress());
                                }
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("Operation in progress: ");
                                stringBuilder2.append(value);
                                pw.println(stringBuilder2.toString());
                                break;
                            case HdmiCecKeycode.CEC_KEYCODE_F3_GREEN /*115*/:
                                value = "Unknown";
                                if (rulesState != null) {
                                    value = rulesState.getSystemRulesVersion();
                                }
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("System rules version: ");
                                stringBuilder2.append(value);
                                pw.println(stringBuilder2.toString());
                                break;
                            case HdmiCecKeycode.CEC_KEYCODE_F4_YELLOW /*116*/:
                                value = "Unknown";
                                if (rulesState != null) {
                                    installedRulesVersion = rulesState.getStagedDistroRulesVersion();
                                    if (installedRulesVersion == null) {
                                        value = "<None>";
                                    } else {
                                        value = installedRulesVersion.toDumpString();
                                    }
                                }
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("Staged rules version: ");
                                stringBuilder2.append(value);
                                pw.println(stringBuilder2.toString());
                                break;
                            default:
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("Unknown option: ");
                                stringBuilder.append(c);
                                pw.println(stringBuilder.toString());
                                break;
                        }
                        i++;
                    }
                    return;
                }
            }
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("RulesManagerService state: ");
            stringBuilder3.append(toString());
            pw.println(stringBuilder3.toString());
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Active rules version (ICU, ZoneInfoDB, TimeZoneFinder): ");
            stringBuilder3.append(ICU.getTZDataVersion());
            stringBuilder3.append(",");
            stringBuilder3.append(ZoneInfoDB.getInstance().getVersion());
            stringBuilder3.append(",");
            stringBuilder3.append(TimeZoneFinder.getInstance().getIanaVersion());
            pw.println(stringBuilder3.toString());
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Distro state: ");
            stringBuilder3.append(rulesState.toString());
            pw.println(stringBuilder3.toString());
            this.mPackageTracker.dump(pw);
        }
    }

    void notifyIdle() {
        this.mPackageTracker.triggerUpdateIfNeeded(false);
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("RulesManagerService{mOperationInProgress=");
        stringBuilder.append(this.mOperationInProgress);
        stringBuilder.append('}');
        return stringBuilder.toString();
    }

    private static CheckToken createCheckTokenOrThrow(byte[] checkTokenBytes) {
        try {
            return CheckToken.fromByteArray(checkTokenBytes);
        } catch (IOException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unable to read token bytes ");
            stringBuilder.append(Arrays.toString(checkTokenBytes));
            throw new IllegalArgumentException(stringBuilder.toString(), e);
        }
    }

    private static String distroStatusToString(int distroStatus) {
        switch (distroStatus) {
            case 1:
                return "None";
            case 2:
                return "Installed";
            default:
                return "Unknown";
        }
    }

    private static String stagedOperationToString(int stagedOperationType) {
        switch (stagedOperationType) {
            case 1:
                return "None";
            case 2:
                return "Uninstall";
            case 3:
                return "Install";
            default:
                return "Unknown";
        }
    }

    private static String toStringOrNull(Object obj) {
        return obj == null ? null : obj.toString();
    }
}
