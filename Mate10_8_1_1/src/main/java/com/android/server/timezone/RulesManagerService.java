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
import libcore.util.ZoneInfoDB;

public final class RulesManagerService extends Stub {
    static final DistroFormatVersion DISTRO_FORMAT_VERSION_SUPPORTED = new DistroFormatVersion(1, 1);
    static final String REQUIRED_UPDATER_PERMISSION = "android.permission.UPDATE_TIME_ZONE_RULES";
    private static final File SYSTEM_TZ_DATA_FILE = new File("/system/usr/share/zoneinfo/tzdata");
    private static final String TAG = "timezone.RulesManagerService";
    private static final File TZ_DATA_DIR = new File("/data/misc/zoneinfo");
    private final Executor mExecutor;
    private final TimeZoneDistroInstaller mInstaller;
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
            Throwable th;
            Throwable th2 = null;
            EventLogTags.writeTimezoneInstallStarted(RulesManagerService.toStringOrNull(this.mCheckToken));
            boolean success = false;
            ParcelFileDescriptor parcelFileDescriptor = null;
            try {
                parcelFileDescriptor = this.mDistroParcelFileDescriptor;
                int resultCode = mapInstallerResultToApiCode(RulesManagerService.this.mInstaller.stageInstallWithErrorCode(new TimeZoneDistro(new FileInputStream(parcelFileDescriptor.getFileDescriptor(), false))));
                EventLogTags.writeTimezoneInstallComplete(RulesManagerService.toStringOrNull(this.mCheckToken), resultCode);
                RulesManagerService.this.sendFinishedStatus(this.mCallback, resultCode);
                success = true;
                if (parcelFileDescriptor != null) {
                    try {
                        parcelFileDescriptor.close();
                    } catch (Throwable th3) {
                        th2 = th3;
                    }
                }
                if (th2 != null) {
                    try {
                        throw th2;
                    } catch (Exception e) {
                        Slog.w(RulesManagerService.TAG, "Failed to install distro.", e);
                        EventLogTags.writeTimezoneInstallComplete(RulesManagerService.toStringOrNull(this.mCheckToken), 1);
                        RulesManagerService.this.sendFinishedStatus(this.mCallback, 1);
                        return;
                    } finally {
                        RulesManagerService.this.mPackageTracker.recordCheckResult(this.mCheckToken, success);
                        RulesManagerService.this.mOperationInProgress.set(false);
                    }
                } else {
                    RulesManagerService.this.mPackageTracker.recordCheckResult(this.mCheckToken, true);
                    RulesManagerService.this.mOperationInProgress.set(false);
                    return;
                }
            } catch (Throwable th22) {
                Throwable th4 = th22;
                th22 = th;
                th = th4;
            }
            if (parcelFileDescriptor != null) {
                try {
                    parcelFileDescriptor.close();
                } catch (Throwable th5) {
                    if (th22 == null) {
                        th22 = th5;
                    } else if (th22 != th5) {
                        th22.addSuppressed(th5);
                    }
                }
            }
            if (th22 != null) {
                throw th22;
            } else {
                throw th;
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
                packageTrackerStatus = uninstallResult != 0 ? uninstallResult == 1 : true;
                int callbackResultCode = packageTrackerStatus ? 0 : 1;
                EventLogTags.writeTimezoneUninstallComplete(RulesManagerService.toStringOrNull(this.mCheckToken), callbackResultCode);
                RulesManagerService.this.sendFinishedStatus(this.mCallback, callbackResultCode);
            } catch (Exception e) {
                EventLogTags.writeTimezoneUninstallComplete(RulesManagerService.toStringOrNull(this.mCheckToken), 1);
                Slog.w(RulesManagerService.TAG, "Failed to uninstall distro.", e);
                RulesManagerService.this.sendFinishedStatus(this.mCallback, 1);
            } finally {
                RulesManagerService.this.mPackageTracker.recordCheckResult(this.mCheckToken, packageTrackerStatus);
                RulesManagerService.this.mOperationInProgress.set(false);
            }
        }
    }

    private static RulesManagerService create(Context context) {
        RulesManagerServiceHelperImpl helper = new RulesManagerServiceHelperImpl(context);
        return new RulesManagerService(helper, helper, PackageTracker.create(context), new TimeZoneDistroInstaller(TAG, SYSTEM_TZ_DATA_FILE, TZ_DATA_DIR));
    }

    RulesManagerService(PermissionHelper permissionHelper, Executor executor, PackageTracker packageTracker, TimeZoneDistroInstaller timeZoneDistroInstaller) {
        this.mPermissionHelper = permissionHelper;
        this.mExecutor = executor;
        this.mPackageTracker = packageTracker;
        this.mInstaller = timeZoneDistroInstaller;
    }

    public void start() {
        this.mPackageTracker.start();
    }

    public RulesState getRulesState() {
        this.mPermissionHelper.enforceCallerHasPermission(REQUIRED_UPDATER_PERMISSION);
        return getRulesStateInternal();
    }

    private RulesState getRulesStateInternal() {
        RulesState rulesState;
        synchronized (this) {
            try {
                String systemRulesVersion = this.mInstaller.getSystemRulesVersion();
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
                    } catch (Exception e) {
                        Slog.w(TAG, "Failed to read staged distro.", e);
                    }
                }
                int distroStatus = 0;
                DistroRulesVersion installedDistroRulesVersion = null;
                if (!operationInProgress) {
                    try {
                        DistroVersion installedDistroVersion = this.mInstaller.getInstalledDistroVersion();
                        if (installedDistroVersion == null) {
                            distroStatus = 1;
                            installedDistroRulesVersion = null;
                        } else {
                            distroStatus = 2;
                            installedDistroRulesVersion = new DistroRulesVersion(installedDistroVersion.rulesVersion, installedDistroVersion.revision);
                        }
                    } catch (Exception e2) {
                        Slog.w(TAG, "Failed to read installed distro.", e2);
                    }
                }
                rulesState = new RulesState(systemRulesVersion, DISTRO_FORMAT_VERSION_SUPPORTED, operationInProgress, stagedOperationStatus, stagedDistroRulesVersion, distroStatus, installedDistroRulesVersion);
            } catch (IOException e3) {
                Slog.w(TAG, "Failed to read system rules", e3);
                return null;
            }
        }
        return rulesState;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int requestInstall(ParcelFileDescriptor distroParcelFileDescriptor, byte[] checkTokenBytes, ICallback callback) {
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
                } else if (callback == null) {
                    throw new NullPointerException("observer == null");
                } else if (!this.mOperationInProgress.get()) {
                    this.mOperationInProgress.set(true);
                    this.mExecutor.execute(new InstallRunnable(distroParcelFileDescriptor, checkToken, callback));
                    closeParcelFileDescriptorOnExit = false;
                    return 0;
                }
            }
            return 1;
        } catch (Throwable th) {
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
        int i = 0;
        if (this.mPermissionHelper.checkDumpPermission(TAG, pw)) {
            RulesState rulesState = getRulesStateInternal();
            if (args == null || args.length != 2 || !"-format_state".equals(args[0]) || args[1] == null) {
                pw.println("RulesManagerService state: " + toString());
                pw.println("Active rules version (ICU, libcore): " + ICU.getTZDataVersion() + "," + ZoneInfoDB.getInstance().getVersion());
                pw.println("Distro state: " + rulesState.toString());
                this.mPackageTracker.dump(pw);
                return;
            }
            char[] toCharArray = args[1].toCharArray();
            int length = toCharArray.length;
            while (i < length) {
                char c = toCharArray[i];
                String value;
                switch (c) {
                    case HdmiCecKeycode.CEC_KEYCODE_PAUSE_PLAY_FUNCTION /*97*/:
                        pw.println("Active rules version (ICU, libcore): " + ICU.getTZDataVersion() + "," + ZoneInfoDB.getInstance().getVersion());
                        break;
                    case HdmiCecKeycode.CEC_KEYCODE_PAUSE_RECORD_FUNCTION /*99*/:
                        value = "Unknown";
                        if (rulesState != null) {
                            value = distroStatusToString(rulesState.getDistroStatus());
                        }
                        pw.println("Current install state: " + value);
                        break;
                    case 'i':
                        value = "Unknown";
                        if (rulesState != null) {
                            DistroRulesVersion installedRulesVersion = rulesState.getInstalledDistroRulesVersion();
                            if (installedRulesVersion == null) {
                                value = "<None>";
                            } else {
                                value = installedRulesVersion.toDumpString();
                            }
                        }
                        pw.println("Installed rules version: " + value);
                        break;
                    case 'o':
                        value = "Unknown";
                        if (rulesState != null) {
                            value = stagedOperationToString(rulesState.getStagedOperationType());
                        }
                        pw.println("Staged operation: " + value);
                        break;
                    case 'p':
                        value = "Unknown";
                        if (rulesState != null) {
                            value = Boolean.toString(rulesState.isOperationInProgress());
                        }
                        pw.println("Operation in progress: " + value);
                        break;
                    case HdmiCecKeycode.CEC_KEYCODE_F3_GREEN /*115*/:
                        value = "Unknown";
                        if (rulesState != null) {
                            value = rulesState.getSystemRulesVersion();
                        }
                        pw.println("System rules version: " + value);
                        break;
                    case HdmiCecKeycode.CEC_KEYCODE_F4_YELLOW /*116*/:
                        value = "Unknown";
                        if (rulesState != null) {
                            DistroRulesVersion stagedDistroRulesVersion = rulesState.getStagedDistroRulesVersion();
                            if (stagedDistroRulesVersion == null) {
                                value = "<None>";
                            } else {
                                value = stagedDistroRulesVersion.toDumpString();
                            }
                        }
                        pw.println("Staged rules version: " + value);
                        break;
                    default:
                        pw.println("Unknown option: " + c);
                        break;
                }
                i++;
            }
        }
    }

    void notifyIdle() {
        this.mPackageTracker.triggerUpdateIfNeeded(false);
    }

    public String toString() {
        return "RulesManagerService{mOperationInProgress=" + this.mOperationInProgress + '}';
    }

    private static CheckToken createCheckTokenOrThrow(byte[] checkTokenBytes) {
        try {
            return CheckToken.fromByteArray(checkTokenBytes);
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to read token bytes " + Arrays.toString(checkTokenBytes), e);
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
