package com.android.server.pm;

import android.accounts.IAccountManager;
import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.content.ComponentName;
import android.content.IIntentReceiver;
import android.content.IIntentSender;
import android.content.Intent;
import android.content.Intent.CommandOptionHandler;
import android.content.IntentSender;
import android.content.pm.ApplicationInfo;
import android.content.pm.FeatureInfo;
import android.content.pm.IPackageDataObserver.Stub;
import android.content.pm.IPackageInstaller;
import android.content.pm.IPackageManager;
import android.content.pm.InstrumentationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller.Session;
import android.content.pm.PackageInstaller.SessionParams;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PackageParser;
import android.content.pm.PackageParser.PackageLite;
import android.content.pm.PackageParser.PackageParserException;
import android.content.pm.ParceledListSlice;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.content.pm.VersionedPackage;
import android.content.pm.dex.DexMetadataHelper;
import android.content.pm.dex.ISnapshotRuntimeProfileCallback;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.hdm.HwDeviceManager;
import android.net.Uri;
import android.net.util.NetworkConstants;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IUserManager;
import android.os.ParcelFileDescriptor;
import android.os.ParcelFileDescriptor.AutoCloseInputStream;
import android.os.PersistableBundle;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.ShellCommand;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.system.ErrnoException;
import android.system.Os;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.PrintWriterPrinter;
import com.android.internal.content.PackageHelper;
import com.android.internal.util.ArrayUtils;
import com.android.server.LocalServices;
import com.android.server.SystemConfig;
import com.android.server.backup.internal.BackupHandler;
import com.android.server.net.NetworkPolicyManagerService;
import com.android.server.slice.SliceClientPermissions.SliceAuthority;
import com.android.server.voiceinteraction.DatabaseHelper.SoundModelContract;
import com.android.server.wm.WindowManagerService.H;
import dalvik.system.DexFile;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import libcore.io.IoUtils;
import libcore.io.Streams;

class PackageManagerShellCommand extends ShellCommand {
    private static final String ART_PROFILE_SNAPSHOT_DEBUG_LOCATION = "/data/misc/profman/";
    private static final int REPAIR_MODE_USER_ID = 127;
    private static final String STDIN_PATH = "-";
    boolean mBrief;
    boolean mComponents;
    final IPackageManager mInterface;
    private final WeakHashMap<String, Resources> mResourceCache = new WeakHashMap();
    int mTargetUser;

    static class ClearDataObserver extends Stub {
        boolean finished;
        boolean result;

        ClearDataObserver() {
        }

        public void onRemoveCompleted(String packageName, boolean succeeded) throws RemoteException {
            synchronized (this) {
                this.finished = true;
                this.result = succeeded;
                notifyAll();
            }
        }
    }

    private static class InstallParams {
        String installerPackageName;
        SessionParams sessionParams;
        int userId;

        private InstallParams() {
            this.userId = -1;
        }

        /* synthetic */ InstallParams(AnonymousClass1 x0) {
            this();
        }
    }

    private static class LocalIntentReceiver {
        private IIntentSender.Stub mLocalSender;
        private final SynchronousQueue<Intent> mResult;

        private LocalIntentReceiver() {
            this.mResult = new SynchronousQueue();
            this.mLocalSender = new IIntentSender.Stub() {
                public void send(int code, Intent intent, String resolvedType, IBinder whitelistToken, IIntentReceiver finishedReceiver, String requiredPermission, Bundle options) {
                    try {
                        LocalIntentReceiver.this.mResult.offer(intent, 5, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            };
        }

        /* synthetic */ LocalIntentReceiver(AnonymousClass1 x0) {
            this();
        }

        public IntentSender getIntentSender() {
            return new IntentSender(this.mLocalSender);
        }

        public Intent getResult() {
            try {
                return (Intent) this.mResult.take();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static class SnapshotRuntimeProfileCallback extends ISnapshotRuntimeProfileCallback.Stub {
        private CountDownLatch mDoneSignal;
        private int mErrCode;
        private ParcelFileDescriptor mProfileReadFd;
        private boolean mSuccess;

        private SnapshotRuntimeProfileCallback() {
            this.mSuccess = false;
            this.mErrCode = -1;
            this.mProfileReadFd = null;
            this.mDoneSignal = new CountDownLatch(1);
        }

        /* synthetic */ SnapshotRuntimeProfileCallback(AnonymousClass1 x0) {
            this();
        }

        public void onSuccess(ParcelFileDescriptor profileReadFd) {
            this.mSuccess = true;
            try {
                this.mProfileReadFd = profileReadFd.dup();
            } catch (IOException e) {
                e.printStackTrace();
            }
            this.mDoneSignal.countDown();
        }

        public void onError(int errCode) {
            this.mSuccess = false;
            this.mErrCode = errCode;
            this.mDoneSignal.countDown();
        }

        boolean waitTillDone() {
            boolean done = false;
            try {
                done = this.mDoneSignal.await(10000000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
            }
            if (done && this.mSuccess) {
                return true;
            }
            return false;
        }
    }

    PackageManagerShellCommand(PackageManagerService service) {
        this.mInterface = service;
    }

    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int onCommand(String cmd) {
        StringBuilder stringBuilder;
        if (cmd == null) {
            return handleDefaultCommands(cmd);
        }
        PrintWriter pw = getOutPrintWriter();
        try {
            int i;
            switch (cmd.hashCode()) {
                case -2102802879:
                    if (cmd.equals("set-harmful-app-warning")) {
                        i = 55;
                        break;
                    }
                case -1967190973:
                    if (cmd.equals("install-abandon")) {
                        i = 8;
                        break;
                    }
                case -1937348290:
                    if (cmd.equals("get-install-location")) {
                        i = 16;
                        break;
                    }
                case -1852006340:
                    if (cmd.equals("suspend")) {
                        i = 34;
                        break;
                    }
                case -1846646502:
                    if (cmd.equals("get-max-running-users")) {
                        i = 50;
                        break;
                    }
                case -1741208611:
                    if (cmd.equals("set-installer")) {
                        i = 52;
                        break;
                    }
                case -1347307837:
                    if (cmd.equals("has-feature")) {
                        i = 54;
                        break;
                    }
                case -1298848381:
                    if (cmd.equals("enable")) {
                        i = 27;
                        break;
                    }
                case -1267782244:
                    if (cmd.equals("get-instantapp-resolver")) {
                        i = 53;
                        break;
                    }
                case -1231004208:
                    if (cmd.equals("resolve-activity")) {
                        i = 3;
                        break;
                    }
                case -1102348235:
                    if (cmd.equals("get-privapp-deny-permissions")) {
                        i = 41;
                        break;
                    }
                case -1091400553:
                    if (cmd.equals("get-oem-permissions")) {
                        i = 42;
                        break;
                    }
                case -1070704814:
                    if (cmd.equals("get-privapp-permissions")) {
                        i = 40;
                        break;
                    }
                case -1032029296:
                    if (cmd.equals("disable-user")) {
                        i = 29;
                        break;
                    }
                case -934343034:
                    if (cmd.equals("revoke")) {
                        i = 37;
                        break;
                    }
                case -919935069:
                    if (cmd.equals("dump-profiles")) {
                        i = 23;
                        break;
                    }
                case -840566949:
                    if (cmd.equals("unhide")) {
                        i = 33;
                        break;
                    }
                case -625596190:
                    if (cmd.equals("uninstall")) {
                        i = 25;
                        break;
                    }
                case -623224643:
                    if (cmd.equals("get-app-link")) {
                        i = 44;
                        break;
                    }
                case -539710980:
                    if (cmd.equals("create-user")) {
                        i = 46;
                        break;
                    }
                case -458695741:
                    if (cmd.equals("query-services")) {
                        i = 5;
                        break;
                    }
                case -444750796:
                    if (cmd.equals("bg-dexopt-job")) {
                        i = 22;
                        break;
                    }
                case -440994401:
                    if (cmd.equals("query-receivers")) {
                        i = 6;
                        break;
                    }
                case -339687564:
                    if (cmd.equals("remove-user")) {
                        i = 47;
                        break;
                    }
                case -220055275:
                    if (cmd.equals("set-permission-enforced")) {
                        i = 39;
                        break;
                    }
                case -140205181:
                    if (cmd.equals("unsuspend")) {
                        i = 35;
                        break;
                    }
                case -132384343:
                    if (cmd.equals("install-commit")) {
                        i = 10;
                        break;
                    }
                case -129863314:
                    if (cmd.equals("install-create")) {
                        i = 11;
                        break;
                    }
                case -115000827:
                    if (cmd.equals("default-state")) {
                        i = 31;
                        break;
                    }
                case -87258188:
                    if (cmd.equals("move-primary-storage")) {
                        i = 18;
                        break;
                    }
                case 3095028:
                    if (cmd.equals("dump")) {
                        i = true;
                        break;
                    }
                case 3202370:
                    if (cmd.equals("hide")) {
                        i = 32;
                        break;
                    }
                case 3322014:
                    if (cmd.equals("list")) {
                        i = 2;
                        break;
                    }
                case 3433509:
                    if (cmd.equals("path")) {
                        i = false;
                        break;
                    }
                case 18936394:
                    if (cmd.equals("move-package")) {
                        i = 17;
                        break;
                    }
                case 86600360:
                    if (cmd.equals("get-max-users")) {
                        i = 49;
                        break;
                    }
                case 94746189:
                    if (cmd.equals("clear")) {
                        i = 26;
                        break;
                    }
                case 98615580:
                    if (cmd.equals("grant")) {
                        i = 36;
                        break;
                    }
                case 107262333:
                    if (cmd.equals("install-existing")) {
                        i = 14;
                        break;
                    }
                case 139892533:
                    if (cmd.equals("get-harmful-app-warning")) {
                        i = 56;
                        break;
                    }
                case 287820022:
                    if (cmd.equals("install-remove")) {
                        i = 12;
                        break;
                    }
                case 359572742:
                    if (cmd.equals("reset-permissions")) {
                        i = 38;
                        break;
                    }
                case 467549856:
                    if (cmd.equals("snapshot-profile")) {
                        i = 24;
                        break;
                    }
                case 798023112:
                    if (cmd.equals("install-destroy")) {
                        i = 9;
                        break;
                    }
                case 826473335:
                    if (cmd.equals("uninstall-system-updates")) {
                        i = 57;
                        break;
                    }
                case 925176533:
                    if (cmd.equals("set-user-restriction")) {
                        i = 48;
                        break;
                    }
                case 925767985:
                    if (cmd.equals("set-app-link")) {
                        i = 43;
                        break;
                    }
                case 950491699:
                    if (cmd.equals("compile")) {
                        i = 19;
                        break;
                    }
                case 1053409810:
                    if (cmd.equals("query-activities")) {
                        i = 4;
                        break;
                    }
                case 1124603675:
                    if (cmd.equals("force-dex-opt")) {
                        i = 21;
                        break;
                    }
                case 1177857340:
                    if (cmd.equals("trim-caches")) {
                        i = 45;
                        break;
                    }
                case 1429366290:
                    if (cmd.equals("set-home-activity")) {
                        i = 51;
                        break;
                    }
                case 1538306349:
                    if (cmd.equals("install-write")) {
                        i = 13;
                        break;
                    }
                case 1671308008:
                    if (cmd.equals("disable")) {
                        i = 28;
                        break;
                    }
                case 1697997009:
                    if (cmd.equals("disable-until-used")) {
                        i = 30;
                        break;
                    }
                case 1746695602:
                    if (cmd.equals("set-install-location")) {
                        i = 15;
                        break;
                    }
                case 1783979817:
                    if (cmd.equals("reconcile-secondary-dex-files")) {
                        i = 20;
                        break;
                    }
                case 1957569947:
                    if (cmd.equals("install")) {
                        i = 7;
                        break;
                    }
                default:
            }
            i = -1;
            String nextArg;
            switch (i) {
                case 0:
                    return runPath();
                case 1:
                    return runDump();
                case 2:
                    return runList();
                case 3:
                    return runResolveActivity();
                case 4:
                    return runQueryIntentActivities();
                case 5:
                    return runQueryIntentServices();
                case 6:
                    return runQueryIntentReceivers();
                case 7:
                    return runInstall();
                case 8:
                case 9:
                    return runInstallAbandon();
                case 10:
                    return runInstallCommit();
                case 11:
                    return runInstallCreate();
                case 12:
                    return runInstallRemove();
                case 13:
                    return runInstallWrite();
                case 14:
                    return runInstallExisting();
                case 15:
                    return runSetInstallLocation();
                case 16:
                    return runGetInstallLocation();
                case 17:
                    return runMovePackage();
                case 18:
                    return runMovePrimaryStorage();
                case H.REPORT_WINDOWS_CHANGE /*19*/:
                    return runCompile();
                case 20:
                    return runreconcileSecondaryDexFiles();
                case BackupHandler.MSG_OP_COMPLETE /*21*/:
                    return runForceDexOpt();
                case H.REPORT_HARD_KEYBOARD_STATUS_CHANGE /*22*/:
                    return runDexoptJob();
                case H.BOOT_TIMEOUT /*23*/:
                    return runDumpProfiles();
                case 24:
                    return runSnapshotProfile();
                case H.SHOW_STRICT_MODE_VIOLATION /*25*/:
                    return runUninstall();
                case H.DO_ANIMATION_CALLBACK /*26*/:
                    nextArg = UserHandle.getCallingUserId();
                    try {
                        nextArg = ActivityManager.getCurrentUser();
                    } catch (Exception e) {
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Exception:");
                        stringBuilder2.append(e.getMessage());
                        pw.println(stringBuilder2.toString());
                    }
                    if (nextArg != REPAIR_MODE_USER_ID) {
                        return runClear();
                    }
                    pw.println("Failure [adb shell pm clear is not supported in REPAIR MODE]");
                    return -1;
                case 27:
                    return runSetEnabledSetting(1);
                case NetworkConstants.ARP_PAYLOAD_LEN /*28*/:
                    return runSetEnabledSetting(2);
                case HdmiCecKeycode.CEC_KEYCODE_NUMBER_ENTRY_MODE /*29*/:
                    return runSetEnabledSetting(3);
                case 30:
                    return runSetEnabledSetting(4);
                case HdmiCecKeycode.CEC_KEYCODE_NUMBER_12 /*31*/:
                    return runSetEnabledSetting(0);
                case 32:
                    return runSetHiddenSetting(true);
                case 33:
                    return runSetHiddenSetting(false);
                case 34:
                    return runSuspend(true);
                case 35:
                    return runSuspend(false);
                case 36:
                    return runGrantRevokePermission(true);
                case 37:
                    return runGrantRevokePermission(false);
                case 38:
                    return runResetPermissions();
                case 39:
                    return runSetPermissionEnforced();
                case 40:
                    return runGetPrivappPermissions();
                case 41:
                    return runGetPrivappDenyPermissions();
                case HdmiCecKeycode.CEC_KEYCODE_DOT /*42*/:
                    return runGetOemPermissions();
                case HdmiCecKeycode.CEC_KEYCODE_ENTER /*43*/:
                    return runSetAppLink();
                case HdmiCecKeycode.CEC_KEYCODE_CLEAR /*44*/:
                    return runGetAppLink();
                case NetworkPolicyManagerService.TYPE_RAPID /*45*/:
                    return runTrimCaches();
                case H.WINDOW_REPLACEMENT_TIMEOUT /*46*/:
                    return runCreateUser();
                case 47:
                    return runRemoveUser();
                case 48:
                    return runSetUserRestriction();
                case 49:
                    return runGetMaxUsers();
                case HdmiCecKeycode.CEC_KEYCODE_PREVIOUS_CHANNEL /*50*/:
                    return runGetMaxRunningUsers();
                case 51:
                    return runSetHomeActivity();
                case 52:
                    return runSetInstaller();
                case 53:
                    return runGetInstantAppResolver();
                case 54:
                    return runHasFeature();
                case 55:
                    return runSetHarmfulAppWarning();
                case 56:
                    return runGetHarmfulAppWarning();
                case H.NOTIFY_KEYGUARD_TRUSTED_CHANGED /*57*/:
                    return uninstallSystemUpdates();
                default:
                    nextArg = getNextArg();
                    if (nextArg == null) {
                        if (cmd.equalsIgnoreCase("-l")) {
                            return runListPackages(false);
                        }
                        if (cmd.equalsIgnoreCase("-lf")) {
                            return runListPackages(true);
                        }
                    } else if (getNextArg() == null && cmd.equalsIgnoreCase("-p")) {
                        return displayPackageFilePath(nextArg, 0);
                    }
                    return handleDefaultCommands(cmd);
            }
        } catch (RemoteException e2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Remote exception: ");
            stringBuilder.append(e2);
            pw.println(stringBuilder.toString());
            return -1;
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("Remote exception: ");
        stringBuilder.append(e2);
        pw.println(stringBuilder.toString());
        return -1;
    }

    private int uninstallSystemUpdates() {
        PrintWriter pw = getOutPrintWriter();
        List<String> failedUninstalls = new LinkedList();
        try {
            ParceledListSlice<ApplicationInfo> packages = this.mInterface.getInstalledApplications(DumpState.DUMP_DEXOPT, 0);
            IPackageInstaller installer = this.mInterface.getPackageInstaller();
            for (ApplicationInfo info : packages.getList()) {
                if (info.isUpdatedSystemApp()) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Uninstalling updates to ");
                    stringBuilder.append(info.packageName);
                    stringBuilder.append("...");
                    pw.println(stringBuilder.toString());
                    LocalIntentReceiver receiver = new LocalIntentReceiver();
                    installer.uninstall(new VersionedPackage(info.packageName, info.versionCode), null, 0, receiver.getIntentSender(), 0);
                    if (receiver.getResult().getIntExtra("android.content.pm.extra.STATUS", 1) != 0) {
                        failedUninstalls.add(info.packageName);
                    }
                }
            }
            if (failedUninstalls.isEmpty()) {
                pw.println("Success");
                return 1;
            }
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Failure [Couldn't uninstall packages: ");
            stringBuilder2.append(TextUtils.join(", ", failedUninstalls));
            stringBuilder2.append("]");
            pw.println(stringBuilder2.toString());
            return 0;
        } catch (RemoteException e) {
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Failure [");
            stringBuilder3.append(e.getClass().getName());
            stringBuilder3.append(" - ");
            stringBuilder3.append(e.getMessage());
            stringBuilder3.append("]");
            pw.println(stringBuilder3.toString());
            return 0;
        }
    }

    private void setParamsSize(InstallParams params, String inPath) {
        if (params.sessionParams.sizeBytes == -1 && !STDIN_PATH.equals(inPath)) {
            ParcelFileDescriptor fd = openFileForSystem(inPath, "r");
            if (fd != null) {
                try {
                    params.sessionParams.setSize(PackageHelper.calculateInstalledSize(new PackageLite(null, PackageParser.parseApkLite(fd.getFileDescriptor(), inPath, 0), null, null, null, null, null, null), params.sessionParams.abiOverride, fd.getFileDescriptor()));
                    try {
                        fd.close();
                    } catch (IOException e) {
                    }
                } catch (PackageParserException | IOException e2) {
                    PrintWriter errPrintWriter = getErrPrintWriter();
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Error: Failed to parse APK file: ");
                    stringBuilder.append(inPath);
                    errPrintWriter.println(stringBuilder.toString());
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Error: Failed to parse APK file: ");
                    stringBuilder.append(inPath);
                    throw new IllegalArgumentException(stringBuilder.toString(), e2);
                } catch (Throwable th) {
                    try {
                        fd.close();
                    } catch (IOException e3) {
                    }
                }
            } else {
                PrintWriter errPrintWriter2 = getErrPrintWriter();
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Error: Can't open file: ");
                stringBuilder2.append(inPath);
                errPrintWriter2.println(stringBuilder2.toString());
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Error: Can't open file: ");
                stringBuilder2.append(inPath);
                throw new IllegalArgumentException(stringBuilder2.toString());
            }
        }
    }

    private int displayPackageFilePath(String pckg, int userId) throws RemoteException {
        PackageInfo info = this.mInterface.getPackageInfo(pckg, 0, userId);
        if (info == null || info.applicationInfo == null) {
            return 1;
        }
        PrintWriter pw = getOutPrintWriter();
        pw.print("package:");
        pw.println(info.applicationInfo.sourceDir);
        if (!ArrayUtils.isEmpty(info.applicationInfo.splitSourceDirs)) {
            for (String splitSourceDir : info.applicationInfo.splitSourceDirs) {
                pw.print("package:");
                pw.println(splitSourceDir);
            }
        }
        return 0;
    }

    private int runPath() throws RemoteException {
        int userId = 0;
        String option = getNextOption();
        if (option != null && option.equals("--user")) {
            userId = UserHandle.parseUserArg(getNextArgRequired());
        }
        String pkg = getNextArgRequired();
        if (pkg != null) {
            return displayPackageFilePath(pkg, userId);
        }
        getErrPrintWriter().println("Error: no package specified");
        return 1;
    }

    /* Code decompiled incorrectly, please refer to instructions dump. */
    private int runList() throws RemoteException {
        PrintWriter pw = getOutPrintWriter();
        String type = getNextArg();
        if (type == null) {
            pw.println("Error: didn't specify type of data to list");
            return -1;
        }
        int i;
        switch (type.hashCode()) {
            case -997447790:
                if (type.equals("permission-groups")) {
                    i = 5;
                    break;
                }
            case -807062458:
                if (type.equals("package")) {
                    i = 3;
                    break;
                }
            case -290659267:
                if (type.equals("features")) {
                    i = 0;
                    break;
                }
            case 111578632:
                if (type.equals(SoundModelContract.KEY_USERS)) {
                    i = 7;
                    break;
                }
            case 544550766:
                if (type.equals("instrumentation")) {
                    i = 1;
                    break;
                }
            case 750867693:
                if (type.equals("packages")) {
                    i = 4;
                    break;
                }
            case 812757657:
                if (type.equals("libraries")) {
                    i = 2;
                    break;
                }
            case 1133704324:
                if (type.equals("permissions")) {
                    i = 6;
                    break;
                }
            default:
                i = -1;
                break;
        }
        switch (i) {
            case 0:
                return runListFeatures();
            case 1:
                return runListInstrumentation();
            case 2:
                return runListLibraries();
            case 3:
            case 4:
                return runListPackages(false);
            case 5:
                return runListPermissionGroups();
            case 6:
                return runListPermissions();
            case 7:
                ServiceManager.getService("user").shellCommand(getInFileDescriptor(), getOutFileDescriptor(), getErrFileDescriptor(), new String[]{"list"}, getShellCallback(), adoptResultReceiver());
                return 0;
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Error: unknown list type '");
                stringBuilder.append(type);
                stringBuilder.append("'");
                pw.println(stringBuilder.toString());
                return -1;
        }
    }

    private int runListFeatures() throws RemoteException {
        PrintWriter pw = getOutPrintWriter();
        List<FeatureInfo> list = this.mInterface.getSystemAvailableFeatures().getList();
        Collections.sort(list, new Comparator<FeatureInfo>() {
            public int compare(FeatureInfo o1, FeatureInfo o2) {
                if (o1.name == o2.name) {
                    return 0;
                }
                if (o1.name == null) {
                    return -1;
                }
                if (o2.name == null) {
                    return 1;
                }
                return o1.name.compareTo(o2.name);
            }
        });
        int count = list != null ? list.size() : 0;
        for (int p = 0; p < count; p++) {
            FeatureInfo fi = (FeatureInfo) list.get(p);
            pw.print("feature:");
            if (fi.name != null) {
                pw.print(fi.name);
                if (fi.version > 0) {
                    pw.print("=");
                    pw.print(fi.version);
                }
                pw.println();
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("reqGlEsVersion=0x");
                stringBuilder.append(Integer.toHexString(fi.reqGlEsVersion));
                pw.println(stringBuilder.toString());
            }
        }
        return 0;
    }

    /* JADX WARNING: Removed duplicated region for block: B:18:0x0044  */
    /* JADX WARNING: Removed duplicated region for block: B:13:0x0025 A:{Catch:{ RuntimeException -> 0x009f }} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private int runListInstrumentation() throws RemoteException {
        PrintWriter pw = getOutPrintWriter();
        boolean showSourceDir = false;
        String targetPackage = null;
        while (true) {
            try {
                String nextArg = getNextArg();
                String opt = nextArg;
                int i;
                if (nextArg != null) {
                    if (opt.hashCode() == 1497) {
                        if (opt.equals("-f")) {
                            i = 0;
                            if (i != 0) {
                                showSourceDir = true;
                            } else if (opt.charAt(0) != '-') {
                                targetPackage = opt;
                            } else {
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("Error: Unknown option: ");
                                stringBuilder.append(opt);
                                pw.println(stringBuilder.toString());
                                return -1;
                            }
                        }
                    }
                    i = -1;
                    if (i != 0) {
                    }
                } else {
                    List<InstrumentationInfo> list = this.mInterface.queryInstrumentation(targetPackage, 0).getList();
                    Collections.sort(list, new Comparator<InstrumentationInfo>() {
                        public int compare(InstrumentationInfo o1, InstrumentationInfo o2) {
                            return o1.targetPackage.compareTo(o2.targetPackage);
                        }
                    });
                    i = list != null ? list.size() : 0;
                    for (int p = 0; p < i; p++) {
                        InstrumentationInfo ii = (InstrumentationInfo) list.get(p);
                        pw.print("instrumentation:");
                        if (showSourceDir) {
                            pw.print(ii.sourceDir);
                            pw.print("=");
                        }
                        pw.print(new ComponentName(ii.packageName, ii.name).flattenToShortString());
                        pw.print(" (target=");
                        pw.print(ii.targetPackage);
                        pw.println(")");
                    }
                    return 0;
                }
            } catch (RuntimeException ex) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Error: ");
                stringBuilder2.append(ex.toString());
                pw.println(stringBuilder2.toString());
                return -1;
            }
        }
    }

    private int runListLibraries() throws RemoteException {
        PrintWriter pw = getOutPrintWriter();
        List<String> list = new ArrayList();
        String[] rawList = this.mInterface.getSystemSharedLibraryNames();
        for (Object add : rawList) {
            list.add(add);
        }
        Collections.sort(list, new Comparator<String>() {
            public int compare(String o1, String o2) {
                if (o1 == o2) {
                    return 0;
                }
                if (o1 == null) {
                    return -1;
                }
                if (o2 == null) {
                    return 1;
                }
                return o1.compareTo(o2);
            }
        });
        int i = list.size();
        for (int p = 0; p < i; p++) {
            String lib = (String) list.get(p);
            pw.print("library:");
            pw.println(lib);
        }
        return 0;
    }

    /* JADX WARNING: Removed duplicated region for block: B:132:0x00db A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:78:0x0109 A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:77:0x0107 A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:76:0x0105 A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:75:0x0103 A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:74:0x0102 A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:73:0x0100 A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:72:0x00fe A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:71:0x00fb A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:70:0x00f9 A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:69:0x00f5 A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:68:0x00ea A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:67:0x00de A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:132:0x00db A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:78:0x0109 A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:77:0x0107 A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:76:0x0105 A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:75:0x0103 A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:74:0x0102 A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:73:0x0100 A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:72:0x00fe A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:71:0x00fb A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:70:0x00f9 A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:69:0x00f5 A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:68:0x00ea A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:67:0x00de A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:132:0x00db A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:78:0x0109 A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:77:0x0107 A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:76:0x0105 A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:75:0x0103 A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:74:0x0102 A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:73:0x0100 A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:72:0x00fe A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:71:0x00fb A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:70:0x00f9 A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:69:0x00f5 A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:68:0x00ea A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:67:0x00de A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:132:0x00db A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:78:0x0109 A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:77:0x0107 A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:76:0x0105 A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:75:0x0103 A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:74:0x0102 A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:73:0x0100 A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:72:0x00fe A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:71:0x00fb A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:70:0x00f9 A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:69:0x00f5 A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:68:0x00ea A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:67:0x00de A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:132:0x00db A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:78:0x0109 A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:77:0x0107 A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:76:0x0105 A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:75:0x0103 A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:74:0x0102 A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:73:0x0100 A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:72:0x00fe A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:71:0x00fb A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:70:0x00f9 A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:69:0x00f5 A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:68:0x00ea A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:67:0x00de A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:132:0x00db A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:78:0x0109 A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:77:0x0107 A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:76:0x0105 A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:75:0x0103 A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:74:0x0102 A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:73:0x0100 A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:72:0x00fe A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:71:0x00fb A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:70:0x00f9 A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:69:0x00f5 A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:68:0x00ea A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:67:0x00de A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:132:0x00db A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:78:0x0109 A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:77:0x0107 A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:76:0x0105 A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:75:0x0103 A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:74:0x0102 A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:73:0x0100 A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:72:0x00fe A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:71:0x00fb A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:70:0x00f9 A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:69:0x00f5 A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:68:0x00ea A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:67:0x00de A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:132:0x00db A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:78:0x0109 A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:77:0x0107 A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:76:0x0105 A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:75:0x0103 A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:74:0x0102 A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:73:0x0100 A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:72:0x00fe A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:71:0x00fb A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:70:0x00f9 A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:69:0x00f5 A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:68:0x00ea A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:67:0x00de A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:132:0x00db A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:78:0x0109 A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:77:0x0107 A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:76:0x0105 A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:75:0x0103 A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:74:0x0102 A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:73:0x0100 A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:72:0x00fe A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:71:0x00fb A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:70:0x00f9 A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:69:0x00f5 A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:68:0x00ea A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Removed duplicated region for block: B:67:0x00de A:{Catch:{ RuntimeException -> 0x0122 }} */
    /* JADX WARNING: Missing block: B:91:0x015c, code skipped:
            if (r3.packageName.contains(r0) == false) goto L_0x015f;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private int runListPackages(boolean showSourceDir) throws RemoteException {
        RuntimeException ex;
        int i;
        StringBuilder stringBuilder;
        PackageManagerShellCommand packageManagerShellCommand = this;
        PrintWriter pw = getOutPrintWriter();
        boolean showVersionCode = false;
        int uid = -1;
        boolean listInstaller = false;
        boolean showUid = false;
        boolean listSystem = false;
        boolean listThirdParty = false;
        boolean listDisabled = false;
        boolean listEnabled = false;
        boolean showSourceDir2 = showSourceDir;
        int getFlags = 0;
        int userId = 0;
        while (true) {
            int userId2 = userId;
            int i2;
            try {
                String nextOption = getNextOption();
                String opt = nextOption;
                int hashCode;
                if (nextOption != null) {
                    nextOption = opt;
                    try {
                        Object obj;
                        hashCode = nextOption.hashCode();
                        if (hashCode != -493830763) {
                            if (hashCode != 1446) {
                                if (hashCode != 1480) {
                                    if (hashCode != NetworkConstants.ETHER_MTU) {
                                        if (hashCode != 1503) {
                                            if (hashCode != 1510) {
                                                if (hashCode != 1512) {
                                                    if (hashCode != 43014832) {
                                                        if (hashCode != 1333469547) {
                                                            switch (hashCode) {
                                                                case 1495:
                                                                    if (nextOption.equals("-d")) {
                                                                        obj = null;
                                                                        break;
                                                                    }
                                                                case 1496:
                                                                    if (nextOption.equals("-e")) {
                                                                        obj = 1;
                                                                        break;
                                                                    }
                                                                case 1497:
                                                                    if (nextOption.equals("-f")) {
                                                                        obj = 2;
                                                                        break;
                                                                    }
                                                                default:
                                                            }
                                                        } else if (nextOption.equals("--user")) {
                                                            obj = 10;
                                                            switch (obj) {
                                                                case null:
                                                                    listDisabled = true;
                                                                    break;
                                                                case 1:
                                                                    listEnabled = true;
                                                                    break;
                                                                case 2:
                                                                    showSourceDir2 = true;
                                                                    break;
                                                                case 3:
                                                                    listInstaller = true;
                                                                    break;
                                                                case 4:
                                                                    break;
                                                                case 5:
                                                                    listSystem = true;
                                                                    break;
                                                                case 6:
                                                                    showUid = true;
                                                                    break;
                                                                case 7:
                                                                    getFlags |= 8192;
                                                                    break;
                                                                case 8:
                                                                    listThirdParty = true;
                                                                    break;
                                                                case 9:
                                                                    showVersionCode = true;
                                                                    break;
                                                                case 10:
                                                                    userId2 = UserHandle.parseUserArg(getNextArgRequired());
                                                                    break;
                                                                case 11:
                                                                    showUid = true;
                                                                    uid = Integer.parseInt(getNextArgRequired());
                                                                    break;
                                                                default:
                                                                    StringBuilder stringBuilder2 = new StringBuilder();
                                                                    stringBuilder2.append("Error: Unknown option: ");
                                                                    stringBuilder2.append(nextOption);
                                                                    pw.println(stringBuilder2.toString());
                                                                    return -1;
                                                            }
                                                            userId = userId2;
                                                        }
                                                    } else if (nextOption.equals("--uid")) {
                                                        obj = 11;
                                                        switch (obj) {
                                                            case null:
                                                                break;
                                                            case 1:
                                                                break;
                                                            case 2:
                                                                break;
                                                            case 3:
                                                                break;
                                                            case 4:
                                                                break;
                                                            case 5:
                                                                break;
                                                            case 6:
                                                                break;
                                                            case 7:
                                                                break;
                                                            case 8:
                                                                break;
                                                            case 9:
                                                                break;
                                                            case 10:
                                                                break;
                                                            case 11:
                                                                break;
                                                            default:
                                                                break;
                                                        }
                                                        userId = userId2;
                                                    }
                                                } else if (nextOption.equals("-u")) {
                                                    obj = 7;
                                                    switch (obj) {
                                                        case null:
                                                            break;
                                                        case 1:
                                                            break;
                                                        case 2:
                                                            break;
                                                        case 3:
                                                            break;
                                                        case 4:
                                                            break;
                                                        case 5:
                                                            break;
                                                        case 6:
                                                            break;
                                                        case 7:
                                                            break;
                                                        case 8:
                                                            break;
                                                        case 9:
                                                            break;
                                                        case 10:
                                                            break;
                                                        case 11:
                                                            break;
                                                        default:
                                                            break;
                                                    }
                                                    userId = userId2;
                                                }
                                            } else if (nextOption.equals("-s")) {
                                                obj = 5;
                                                switch (obj) {
                                                    case null:
                                                        break;
                                                    case 1:
                                                        break;
                                                    case 2:
                                                        break;
                                                    case 3:
                                                        break;
                                                    case 4:
                                                        break;
                                                    case 5:
                                                        break;
                                                    case 6:
                                                        break;
                                                    case 7:
                                                        break;
                                                    case 8:
                                                        break;
                                                    case 9:
                                                        break;
                                                    case 10:
                                                        break;
                                                    case 11:
                                                        break;
                                                    default:
                                                        break;
                                                }
                                                userId = userId2;
                                            }
                                        } else if (nextOption.equals("-l")) {
                                            obj = 4;
                                            switch (obj) {
                                                case null:
                                                    break;
                                                case 1:
                                                    break;
                                                case 2:
                                                    break;
                                                case 3:
                                                    break;
                                                case 4:
                                                    break;
                                                case 5:
                                                    break;
                                                case 6:
                                                    break;
                                                case 7:
                                                    break;
                                                case 8:
                                                    break;
                                                case 9:
                                                    break;
                                                case 10:
                                                    break;
                                                case 11:
                                                    break;
                                                default:
                                                    break;
                                            }
                                            userId = userId2;
                                        }
                                    } else if (nextOption.equals("-i")) {
                                        obj = 3;
                                        switch (obj) {
                                            case null:
                                                break;
                                            case 1:
                                                break;
                                            case 2:
                                                break;
                                            case 3:
                                                break;
                                            case 4:
                                                break;
                                            case 5:
                                                break;
                                            case 6:
                                                break;
                                            case 7:
                                                break;
                                            case 8:
                                                break;
                                            case 9:
                                                break;
                                            case 10:
                                                break;
                                            case 11:
                                                break;
                                            default:
                                                break;
                                        }
                                        userId = userId2;
                                    }
                                } else if (nextOption.equals("-U")) {
                                    obj = 6;
                                    switch (obj) {
                                        case null:
                                            break;
                                        case 1:
                                            break;
                                        case 2:
                                            break;
                                        case 3:
                                            break;
                                        case 4:
                                            break;
                                        case 5:
                                            break;
                                        case 6:
                                            break;
                                        case 7:
                                            break;
                                        case 8:
                                            break;
                                        case 9:
                                            break;
                                        case 10:
                                            break;
                                        case 11:
                                            break;
                                        default:
                                            break;
                                    }
                                    userId = userId2;
                                }
                            } else if (nextOption.equals("-3")) {
                                obj = 8;
                                switch (obj) {
                                    case null:
                                        break;
                                    case 1:
                                        break;
                                    case 2:
                                        break;
                                    case 3:
                                        break;
                                    case 4:
                                        break;
                                    case 5:
                                        break;
                                    case 6:
                                        break;
                                    case 7:
                                        break;
                                    case 8:
                                        break;
                                    case 9:
                                        break;
                                    case 10:
                                        break;
                                    case 11:
                                        break;
                                    default:
                                        break;
                                }
                                userId = userId2;
                            }
                        } else if (nextOption.equals("--show-versioncode")) {
                            obj = 9;
                            switch (obj) {
                                case null:
                                    break;
                                case 1:
                                    break;
                                case 2:
                                    break;
                                case 3:
                                    break;
                                case 4:
                                    break;
                                case 5:
                                    break;
                                case 6:
                                    break;
                                case 7:
                                    break;
                                case 8:
                                    break;
                                case 9:
                                    break;
                                case 10:
                                    break;
                                case 11:
                                    break;
                                default:
                                    break;
                            }
                            userId = userId2;
                        }
                        obj = -1;
                        switch (obj) {
                            case null:
                                break;
                            case 1:
                                break;
                            case 2:
                                break;
                            case 3:
                                break;
                            case 4:
                                break;
                            case 5:
                                break;
                            case 6:
                                break;
                            case 7:
                                break;
                            case 8:
                                break;
                            case 9:
                                break;
                            case 10:
                                break;
                            case 11:
                                break;
                            default:
                                break;
                        }
                        userId = userId2;
                    } catch (RuntimeException e) {
                        ex = e;
                        i = getFlags;
                        i2 = userId2;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Error: ");
                        stringBuilder.append(ex.toString());
                        pw.println(stringBuilder.toString());
                        return -1;
                    }
                }
                nextOption = getNextArg();
                ParceledListSlice<PackageInfo> slice = packageManagerShellCommand.mInterface.getInstalledPackages(getFlags, userId2);
                List<PackageInfo> packages = slice.getList();
                getFlags = packages.size();
                int p = 0;
                while (true) {
                    ParceledListSlice<PackageInfo> slice2 = slice;
                    hashCode = p;
                    int count;
                    if (hashCode < getFlags) {
                        String filter;
                        count = getFlags;
                        PackageInfo getFlags2 = (PackageInfo) packages.get(hashCode);
                        if (nextOption != null) {
                            i2 = userId2;
                        } else {
                            i2 = userId2;
                            if (uid == -1 || getFlags2.applicationInfo.uid == uid) {
                                boolean isSystem = (getFlags2.applicationInfo.flags & 1) != 0;
                                if (listDisabled) {
                                    filter = nextOption;
                                    if (getFlags2.applicationInfo.enabled) {
                                        p = hashCode + 1;
                                        slice = slice2;
                                        getFlags = count;
                                        userId2 = i2;
                                        nextOption = filter;
                                        packageManagerShellCommand = this;
                                    }
                                } else {
                                    filter = nextOption;
                                }
                                if ((!listEnabled || getFlags2.applicationInfo.enabled) && ((!listSystem || isSystem) && !(listThirdParty && isSystem))) {
                                    pw.print("package:");
                                    if (showSourceDir2) {
                                        pw.print(getFlags2.applicationInfo.sourceDir);
                                        pw.print("=");
                                    }
                                    pw.print(getFlags2.packageName);
                                    if (showVersionCode) {
                                        pw.print(" versionCode:");
                                        pw.print(getFlags2.applicationInfo.versionCode);
                                    }
                                    if (listInstaller) {
                                        pw.print("  installer=");
                                        pw.print(packageManagerShellCommand.mInterface.getInstallerPackageName(getFlags2.packageName));
                                    }
                                    if (showUid) {
                                        pw.print(" uid:");
                                        pw.print(getFlags2.applicationInfo.uid);
                                    }
                                    pw.println();
                                }
                                p = hashCode + 1;
                                slice = slice2;
                                getFlags = count;
                                userId2 = i2;
                                nextOption = filter;
                                packageManagerShellCommand = this;
                            }
                        }
                        filter = nextOption;
                        p = hashCode + 1;
                        slice = slice2;
                        getFlags = count;
                        userId2 = i2;
                        nextOption = filter;
                        packageManagerShellCommand = this;
                    } else {
                        count = getFlags;
                        i2 = userId2;
                        return 0;
                    }
                }
            } catch (RuntimeException e2) {
                ex = e2;
                i = getFlags;
                i2 = userId2;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Error: ");
                stringBuilder.append(ex.toString());
                pw.println(stringBuilder.toString());
                return -1;
            }
        }
    }

    private int runListPermissionGroups() throws RemoteException {
        PrintWriter pw = getOutPrintWriter();
        List<PermissionGroupInfo> pgs = this.mInterface.getAllPermissionGroups(0).getList();
        int count = pgs.size();
        for (int p = 0; p < count; p++) {
            PermissionGroupInfo pgi = (PermissionGroupInfo) pgs.get(p);
            pw.print("permission group:");
            pw.println(pgi.name);
        }
        return 0;
    }

    private int runListPermissions() throws RemoteException {
        PrintWriter pw = getOutPrintWriter();
        boolean groups = false;
        boolean userOnly = false;
        boolean summary = false;
        boolean labels = false;
        boolean dangerousOnly = false;
        while (true) {
            String nextOption = getNextOption();
            String opt = nextOption;
            int hashCode;
            if (nextOption != null) {
                boolean labels2;
                int i = -1;
                hashCode = opt.hashCode();
                if (hashCode != 1495) {
                    if (hashCode != 1510) {
                        if (hashCode != 1512) {
                            switch (hashCode) {
                                case 1497:
                                    if (opt.equals("-f")) {
                                        i = 1;
                                        break;
                                    }
                                    break;
                                case 1498:
                                    if (opt.equals("-g")) {
                                        i = 2;
                                        break;
                                    }
                                    break;
                            }
                        } else if (opt.equals("-u")) {
                            i = 4;
                        }
                    } else if (opt.equals("-s")) {
                        i = 3;
                    }
                } else if (opt.equals("-d")) {
                    i = 0;
                }
                switch (i) {
                    case 0:
                        dangerousOnly = true;
                        continue;
                    case 1:
                        labels2 = true;
                        break;
                    case 2:
                        groups = true;
                        continue;
                    case 3:
                        groups = true;
                        labels2 = true;
                        summary = true;
                        break;
                    case 4:
                        userOnly = true;
                        continue;
                    default:
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Error: Unknown option: ");
                        stringBuilder.append(opt);
                        pw.println(stringBuilder.toString());
                        return 1;
                }
                labels = labels2;
            } else {
                PackageManagerShellCommand packageManagerShellCommand;
                ArrayList<String> groupList = new ArrayList();
                if (groups) {
                    packageManagerShellCommand = this;
                    List<PermissionGroupInfo> infos = packageManagerShellCommand.mInterface.getAllPermissionGroups(0).getList();
                    hashCode = infos.size();
                    for (int i2 = 0; i2 < hashCode; i2++) {
                        groupList.add(((PermissionGroupInfo) infos.get(i2)).name);
                    }
                    groupList.add(null);
                } else {
                    packageManagerShellCommand = this;
                    groupList.add(getNextArg());
                }
                if (dangerousOnly) {
                    pw.println("Dangerous Permissions:");
                    pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                    packageManagerShellCommand.doListPermissions(groupList, groups, labels, summary, 1, 1);
                    if (userOnly) {
                        pw.println("Normal Permissions:");
                        pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                        doListPermissions(groupList, groups, labels, summary, 0, 0);
                    }
                } else if (userOnly) {
                    pw.println("Dangerous and Normal Permissions:");
                    pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                    doListPermissions(groupList, groups, labels, summary, 0, 1);
                } else {
                    pw.println("All Permissions:");
                    pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                    doListPermissions(groupList, groups, labels, summary, -10000, 10000);
                }
                return 0;
            }
        }
    }

    private Intent parseIntentAndUser() throws URISyntaxException {
        this.mTargetUser = -2;
        this.mBrief = false;
        this.mComponents = false;
        Intent intent = Intent.parseCommandArgs(this, new CommandOptionHandler() {
            public boolean handleOption(String opt, ShellCommand cmd) {
                if ("--user".equals(opt)) {
                    PackageManagerShellCommand.this.mTargetUser = UserHandle.parseUserArg(cmd.getNextArgRequired());
                    return true;
                } else if ("--brief".equals(opt)) {
                    PackageManagerShellCommand.this.mBrief = true;
                    return true;
                } else if (!"--components".equals(opt)) {
                    return false;
                } else {
                    PackageManagerShellCommand.this.mComponents = true;
                    return true;
                }
            }
        });
        this.mTargetUser = ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), this.mTargetUser, false, false, null, null);
        return intent;
    }

    private void printResolveInfo(PrintWriterPrinter pr, String prefix, ResolveInfo ri, boolean brief, boolean components) {
        if (brief || components) {
            ComponentName comp;
            if (ri.activityInfo != null) {
                comp = new ComponentName(ri.activityInfo.packageName, ri.activityInfo.name);
            } else if (ri.serviceInfo != null) {
                comp = new ComponentName(ri.serviceInfo.packageName, ri.serviceInfo.name);
            } else if (ri.providerInfo != null) {
                comp = new ComponentName(ri.providerInfo.packageName, ri.providerInfo.name);
            } else {
                comp = null;
            }
            if (comp != null) {
                StringBuilder stringBuilder;
                if (!components) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(prefix);
                    stringBuilder.append("priority=");
                    stringBuilder.append(ri.priority);
                    stringBuilder.append(" preferredOrder=");
                    stringBuilder.append(ri.preferredOrder);
                    stringBuilder.append(" match=0x");
                    stringBuilder.append(Integer.toHexString(ri.match));
                    stringBuilder.append(" specificIndex=");
                    stringBuilder.append(ri.specificIndex);
                    stringBuilder.append(" isDefault=");
                    stringBuilder.append(ri.isDefault);
                    pr.println(stringBuilder.toString());
                }
                stringBuilder = new StringBuilder();
                stringBuilder.append(prefix);
                stringBuilder.append(comp.flattenToShortString());
                pr.println(stringBuilder.toString());
                return;
            }
        }
        ri.dump(pr, prefix);
    }

    private int runResolveActivity() {
        try {
            Intent intent = parseIntentAndUser();
            try {
                ResolveInfo ri = this.mInterface.resolveIntent(intent, intent.getType(), 0, this.mTargetUser);
                PrintWriter pw = getOutPrintWriter();
                if (ri == null) {
                    pw.println("No activity found");
                } else {
                    printResolveInfo(new PrintWriterPrinter(pw), BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, ri, this.mBrief, this.mComponents);
                }
                return 0;
            } catch (RemoteException e) {
                throw new RuntimeException("Failed calling service", e);
            }
        } catch (URISyntaxException e2) {
            throw new RuntimeException(e2.getMessage(), e2);
        }
    }

    private int runQueryIntentActivities() {
        try {
            Intent intent = parseIntentAndUser();
            try {
                List<ResolveInfo> result = this.mInterface.queryIntentActivities(intent, intent.getType(), 0, this.mTargetUser).getList();
                PrintWriter pw = getOutPrintWriter();
                if (result != null) {
                    if (result.size() > 0) {
                        PrintWriterPrinter pr;
                        int i;
                        if (this.mComponents) {
                            pr = new PrintWriterPrinter(pw);
                            for (i = 0; i < result.size(); i++) {
                                printResolveInfo(pr, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, (ResolveInfo) result.get(i), this.mBrief, this.mComponents);
                            }
                        } else {
                            pw.print(result.size());
                            pw.println(" activities found:");
                            pr = new PrintWriterPrinter(pw);
                            for (i = 0; i < result.size(); i++) {
                                pw.print("  Activity #");
                                pw.print(i);
                                pw.println(":");
                                printResolveInfo(pr, "    ", (ResolveInfo) result.get(i), this.mBrief, this.mComponents);
                            }
                        }
                        return 0;
                    }
                }
                pw.println("No activities found");
                return 0;
            } catch (RemoteException e) {
                throw new RuntimeException("Failed calling service", e);
            }
        } catch (URISyntaxException e2) {
            throw new RuntimeException(e2.getMessage(), e2);
        }
    }

    private int runQueryIntentServices() {
        try {
            Intent intent = parseIntentAndUser();
            try {
                List<ResolveInfo> result = this.mInterface.queryIntentServices(intent, intent.getType(), 0, this.mTargetUser).getList();
                PrintWriter pw = getOutPrintWriter();
                if (result != null) {
                    if (result.size() > 0) {
                        PrintWriterPrinter pr;
                        int i;
                        if (this.mComponents) {
                            pr = new PrintWriterPrinter(pw);
                            for (i = 0; i < result.size(); i++) {
                                printResolveInfo(pr, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, (ResolveInfo) result.get(i), this.mBrief, this.mComponents);
                            }
                        } else {
                            pw.print(result.size());
                            pw.println(" services found:");
                            pr = new PrintWriterPrinter(pw);
                            for (i = 0; i < result.size(); i++) {
                                pw.print("  Service #");
                                pw.print(i);
                                pw.println(":");
                                printResolveInfo(pr, "    ", (ResolveInfo) result.get(i), this.mBrief, this.mComponents);
                            }
                        }
                        return 0;
                    }
                }
                pw.println("No services found");
                return 0;
            } catch (RemoteException e) {
                throw new RuntimeException("Failed calling service", e);
            }
        } catch (URISyntaxException e2) {
            throw new RuntimeException(e2.getMessage(), e2);
        }
    }

    private int runQueryIntentReceivers() {
        try {
            Intent intent = parseIntentAndUser();
            try {
                List<ResolveInfo> result = this.mInterface.queryIntentReceivers(intent, intent.getType(), 0, this.mTargetUser).getList();
                PrintWriter pw = getOutPrintWriter();
                if (result != null) {
                    if (result.size() > 0) {
                        PrintWriterPrinter pr;
                        int i;
                        if (this.mComponents) {
                            pr = new PrintWriterPrinter(pw);
                            for (i = 0; i < result.size(); i++) {
                                printResolveInfo(pr, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, (ResolveInfo) result.get(i), this.mBrief, this.mComponents);
                            }
                        } else {
                            pw.print(result.size());
                            pw.println(" receivers found:");
                            pr = new PrintWriterPrinter(pw);
                            for (i = 0; i < result.size(); i++) {
                                pw.print("  Receiver #");
                                pw.print(i);
                                pw.println(":");
                                printResolveInfo(pr, "    ", (ResolveInfo) result.get(i), this.mBrief, this.mComponents);
                            }
                        }
                        return 0;
                    }
                }
                pw.println("No receivers found");
                return 0;
            } catch (RemoteException e) {
                throw new RuntimeException("Failed calling service", e);
            }
        } catch (URISyntaxException e2) {
            throw new RuntimeException(e2.getMessage(), e2);
        }
    }

    private int runInstall() throws RemoteException {
        PrintWriter pw = getOutPrintWriter();
        InstallParams params = makeInstallParams();
        String inPath = getNextArg();
        setParamsSize(params, inPath);
        int sessionId = doCreateSession(params.sessionParams, params.installerPackageName, params.userId);
        boolean abandonSession = true;
        if (inPath == null) {
            try {
                if (params.sessionParams.sizeBytes == -1) {
                    pw.println("Error: must either specify a package size or an APK file");
                    if (abandonSession) {
                        try {
                            doAbandonSession(sessionId, false);
                        } catch (Exception e) {
                        }
                    }
                    return 1;
                }
            } catch (Throwable th) {
                if (abandonSession) {
                    try {
                        doAbandonSession(sessionId, false);
                    } catch (Exception e2) {
                    }
                }
            }
        }
        if (HwDeviceManager.disallowOp(6)) {
            System.err.println("Failure [MDM_FORBID_ADB_INSTALL]");
            if (abandonSession) {
                try {
                    doAbandonSession(sessionId, false);
                } catch (Exception e3) {
                }
            }
            return 1;
        }
        if (doWriteSplit(sessionId, inPath, params.sessionParams.sizeBytes, "base.apk", false) != 0) {
            if (abandonSession) {
                try {
                    doAbandonSession(sessionId, false);
                } catch (Exception e4) {
                }
            }
            return 1;
        } else if (doCommitSession(sessionId, false) != 0) {
            if (abandonSession) {
                try {
                    doAbandonSession(sessionId, false);
                } catch (Exception e5) {
                }
            }
            return 1;
        } else {
            abandonSession = false;
            pw.println("Success");
            if (null != null) {
                try {
                    doAbandonSession(sessionId, false);
                } catch (Exception e6) {
                }
            }
            return 0;
        }
    }

    private int runInstallAbandon() throws RemoteException {
        return doAbandonSession(Integer.parseInt(getNextArg()), true);
    }

    private int runInstallCommit() throws RemoteException {
        return doCommitSession(Integer.parseInt(getNextArg()), true);
    }

    private int runInstallCreate() throws RemoteException {
        PrintWriter pw = getOutPrintWriter();
        InstallParams installParams = makeInstallParams();
        int sessionId = doCreateSession(installParams.sessionParams, installParams.installerPackageName, installParams.userId);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Success: created install session [");
        stringBuilder.append(sessionId);
        stringBuilder.append("]");
        pw.println(stringBuilder.toString());
        return 0;
    }

    private int runInstallWrite() throws RemoteException {
        long sizeBytes = -1;
        while (true) {
            String nextOption = getNextOption();
            String opt = nextOption;
            if (nextOption == null) {
                return doWriteSplit(Integer.parseInt(getNextArg()), getNextArg(), sizeBytes, getNextArg(), true);
            } else if (opt.equals("-S")) {
                sizeBytes = Long.parseLong(getNextArg());
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unknown option: ");
                stringBuilder.append(opt);
                throw new IllegalArgumentException(stringBuilder.toString());
            }
        }
    }

    private int runInstallRemove() throws RemoteException {
        PrintWriter pw = getOutPrintWriter();
        int sessionId = Integer.parseInt(getNextArg());
        String splitName = getNextArg();
        if (splitName != null) {
            return doRemoveSplit(sessionId, splitName, true);
        }
        pw.println("Error: split name not specified");
        return 1;
    }

    private int runInstallExisting() throws RemoteException {
        PrintWriter pw = getOutPrintWriter();
        int userId = 0;
        int installFlags = 0;
        while (true) {
            String nextOption = getNextOption();
            String opt = nextOption;
            if (nextOption != null) {
                int i = -1;
                int hashCode = opt.hashCode();
                if (hashCode != -951415743) {
                    if (hashCode != 1051781117) {
                        if (hashCode != 1333024815) {
                            if (hashCode == 1333469547 && opt.equals("--user")) {
                                i = 0;
                            }
                        } else if (opt.equals("--full")) {
                            i = 3;
                        }
                    } else if (opt.equals("--ephemeral")) {
                        i = 1;
                    }
                } else if (opt.equals("--instant")) {
                    i = 2;
                }
                switch (i) {
                    case 0:
                        userId = UserHandle.parseUserArg(getNextArgRequired());
                        break;
                    case 1:
                    case 2:
                        installFlags = (installFlags | 2048) & -16385;
                        break;
                    case 3:
                        installFlags = (installFlags & -2049) | 16384;
                        break;
                    default:
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Error: Unknown option: ");
                        stringBuilder.append(opt);
                        pw.println(stringBuilder.toString());
                        return 1;
                }
            }
            nextOption = getNextArg();
            if (nextOption == null) {
                pw.println("Error: package name not specified");
                return 1;
            }
            try {
                StringBuilder stringBuilder2;
                if (this.mInterface.installExistingPackageAsUser(nextOption, userId, installFlags, 0) != -3) {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Package ");
                    stringBuilder2.append(nextOption);
                    stringBuilder2.append(" installed for user: ");
                    stringBuilder2.append(userId);
                    pw.println(stringBuilder2.toString());
                    return 0;
                }
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Package ");
                stringBuilder2.append(nextOption);
                stringBuilder2.append(" doesn't exist");
                throw new NameNotFoundException(stringBuilder2.toString());
            } catch (NameNotFoundException | RemoteException e) {
                pw.println(e.toString());
                return 1;
            }
        }
    }

    private int runSetInstallLocation() throws RemoteException {
        String arg = getNextArg();
        if (arg == null) {
            getErrPrintWriter().println("Error: no install location specified.");
            return 1;
        }
        try {
            if (this.mInterface.setInstallLocation(Integer.parseInt(arg))) {
                return 0;
            }
            getErrPrintWriter().println("Error: install location has to be a number.");
            return 1;
        } catch (NumberFormatException e) {
            getErrPrintWriter().println("Error: install location has to be a number.");
            return 1;
        }
    }

    private int runGetInstallLocation() throws RemoteException {
        int loc = this.mInterface.getInstallLocation();
        String locStr = "invalid";
        if (loc == 0) {
            locStr = Shell.NIGHT_MODE_STR_AUTO;
        } else if (loc == 1) {
            locStr = "internal";
        } else if (loc == 2) {
            locStr = "external";
        }
        PrintWriter outPrintWriter = getOutPrintWriter();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(loc);
        stringBuilder.append("[");
        stringBuilder.append(locStr);
        stringBuilder.append("]");
        outPrintWriter.println(stringBuilder.toString());
        return 0;
    }

    public int runMovePackage() throws RemoteException {
        String packageName = getNextArg();
        if (packageName == null) {
            getErrPrintWriter().println("Error: package name not specified");
            return 1;
        }
        String volumeUuid = getNextArg();
        if ("internal".equals(volumeUuid)) {
            volumeUuid = null;
        }
        int moveId = this.mInterface.movePackage(packageName, volumeUuid);
        int status = this.mInterface.getMoveStatus(moveId);
        while (!PackageManager.isMoveStatusFinished(status)) {
            SystemClock.sleep(1000);
            status = this.mInterface.getMoveStatus(moveId);
        }
        if (status == -100) {
            getOutPrintWriter().println("Success");
            return 0;
        }
        PrintWriter errPrintWriter = getErrPrintWriter();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Failure [");
        stringBuilder.append(status);
        stringBuilder.append("]");
        errPrintWriter.println(stringBuilder.toString());
        return 1;
    }

    public int runMovePrimaryStorage() throws RemoteException {
        String volumeUuid = getNextArg();
        if ("internal".equals(volumeUuid)) {
            volumeUuid = null;
        }
        int moveId = this.mInterface.movePrimaryStorage(volumeUuid);
        int status = this.mInterface.getMoveStatus(moveId);
        while (!PackageManager.isMoveStatusFinished(status)) {
            SystemClock.sleep(1000);
            status = this.mInterface.getMoveStatus(moveId);
        }
        if (status == -100) {
            getOutPrintWriter().println("Success");
            return 0;
        }
        PrintWriter errPrintWriter = getErrPrintWriter();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Failure [");
        stringBuilder.append(status);
        stringBuilder.append("]");
        errPrintWriter.println(stringBuilder.toString());
        return 1;
    }

    /* JADX WARNING: Removed duplicated region for block: B:147:0x00aa A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:61:0x00ef  */
    /* JADX WARNING: Removed duplicated region for block: B:60:0x00eb  */
    /* JADX WARNING: Removed duplicated region for block: B:58:0x00e7  */
    /* JADX WARNING: Removed duplicated region for block: B:57:0x00e0  */
    /* JADX WARNING: Removed duplicated region for block: B:56:0x00d9  */
    /* JADX WARNING: Removed duplicated region for block: B:55:0x00d2  */
    /* JADX WARNING: Removed duplicated region for block: B:54:0x00ca  */
    /* JADX WARNING: Removed duplicated region for block: B:53:0x00c6  */
    /* JADX WARNING: Removed duplicated region for block: B:52:0x00bf  */
    /* JADX WARNING: Removed duplicated region for block: B:147:0x00aa A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:61:0x00ef  */
    /* JADX WARNING: Removed duplicated region for block: B:60:0x00eb  */
    /* JADX WARNING: Removed duplicated region for block: B:58:0x00e7  */
    /* JADX WARNING: Removed duplicated region for block: B:57:0x00e0  */
    /* JADX WARNING: Removed duplicated region for block: B:56:0x00d9  */
    /* JADX WARNING: Removed duplicated region for block: B:55:0x00d2  */
    /* JADX WARNING: Removed duplicated region for block: B:54:0x00ca  */
    /* JADX WARNING: Removed duplicated region for block: B:53:0x00c6  */
    /* JADX WARNING: Removed duplicated region for block: B:52:0x00bf  */
    /* JADX WARNING: Removed duplicated region for block: B:147:0x00aa A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:61:0x00ef  */
    /* JADX WARNING: Removed duplicated region for block: B:60:0x00eb  */
    /* JADX WARNING: Removed duplicated region for block: B:58:0x00e7  */
    /* JADX WARNING: Removed duplicated region for block: B:57:0x00e0  */
    /* JADX WARNING: Removed duplicated region for block: B:56:0x00d9  */
    /* JADX WARNING: Removed duplicated region for block: B:55:0x00d2  */
    /* JADX WARNING: Removed duplicated region for block: B:54:0x00ca  */
    /* JADX WARNING: Removed duplicated region for block: B:53:0x00c6  */
    /* JADX WARNING: Removed duplicated region for block: B:52:0x00bf  */
    /* JADX WARNING: Removed duplicated region for block: B:147:0x00aa A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:61:0x00ef  */
    /* JADX WARNING: Removed duplicated region for block: B:60:0x00eb  */
    /* JADX WARNING: Removed duplicated region for block: B:58:0x00e7  */
    /* JADX WARNING: Removed duplicated region for block: B:57:0x00e0  */
    /* JADX WARNING: Removed duplicated region for block: B:56:0x00d9  */
    /* JADX WARNING: Removed duplicated region for block: B:55:0x00d2  */
    /* JADX WARNING: Removed duplicated region for block: B:54:0x00ca  */
    /* JADX WARNING: Removed duplicated region for block: B:53:0x00c6  */
    /* JADX WARNING: Removed duplicated region for block: B:52:0x00bf  */
    /* JADX WARNING: Removed duplicated region for block: B:147:0x00aa A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:61:0x00ef  */
    /* JADX WARNING: Removed duplicated region for block: B:60:0x00eb  */
    /* JADX WARNING: Removed duplicated region for block: B:58:0x00e7  */
    /* JADX WARNING: Removed duplicated region for block: B:57:0x00e0  */
    /* JADX WARNING: Removed duplicated region for block: B:56:0x00d9  */
    /* JADX WARNING: Removed duplicated region for block: B:55:0x00d2  */
    /* JADX WARNING: Removed duplicated region for block: B:54:0x00ca  */
    /* JADX WARNING: Removed duplicated region for block: B:53:0x00c6  */
    /* JADX WARNING: Removed duplicated region for block: B:52:0x00bf  */
    /* JADX WARNING: Removed duplicated region for block: B:147:0x00aa A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:61:0x00ef  */
    /* JADX WARNING: Removed duplicated region for block: B:60:0x00eb  */
    /* JADX WARNING: Removed duplicated region for block: B:58:0x00e7  */
    /* JADX WARNING: Removed duplicated region for block: B:57:0x00e0  */
    /* JADX WARNING: Removed duplicated region for block: B:56:0x00d9  */
    /* JADX WARNING: Removed duplicated region for block: B:55:0x00d2  */
    /* JADX WARNING: Removed duplicated region for block: B:54:0x00ca  */
    /* JADX WARNING: Removed duplicated region for block: B:53:0x00c6  */
    /* JADX WARNING: Removed duplicated region for block: B:52:0x00bf  */
    /* JADX WARNING: Removed duplicated region for block: B:147:0x00aa A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:61:0x00ef  */
    /* JADX WARNING: Removed duplicated region for block: B:60:0x00eb  */
    /* JADX WARNING: Removed duplicated region for block: B:58:0x00e7  */
    /* JADX WARNING: Removed duplicated region for block: B:57:0x00e0  */
    /* JADX WARNING: Removed duplicated region for block: B:56:0x00d9  */
    /* JADX WARNING: Removed duplicated region for block: B:55:0x00d2  */
    /* JADX WARNING: Removed duplicated region for block: B:54:0x00ca  */
    /* JADX WARNING: Removed duplicated region for block: B:53:0x00c6  */
    /* JADX WARNING: Removed duplicated region for block: B:52:0x00bf  */
    /* JADX WARNING: Removed duplicated region for block: B:147:0x00aa A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:61:0x00ef  */
    /* JADX WARNING: Removed duplicated region for block: B:60:0x00eb  */
    /* JADX WARNING: Removed duplicated region for block: B:58:0x00e7  */
    /* JADX WARNING: Removed duplicated region for block: B:57:0x00e0  */
    /* JADX WARNING: Removed duplicated region for block: B:56:0x00d9  */
    /* JADX WARNING: Removed duplicated region for block: B:55:0x00d2  */
    /* JADX WARNING: Removed duplicated region for block: B:54:0x00ca  */
    /* JADX WARNING: Removed duplicated region for block: B:53:0x00c6  */
    /* JADX WARNING: Removed duplicated region for block: B:52:0x00bf  */
    /* JADX WARNING: Removed duplicated region for block: B:147:0x00aa A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:61:0x00ef  */
    /* JADX WARNING: Removed duplicated region for block: B:60:0x00eb  */
    /* JADX WARNING: Removed duplicated region for block: B:58:0x00e7  */
    /* JADX WARNING: Removed duplicated region for block: B:57:0x00e0  */
    /* JADX WARNING: Removed duplicated region for block: B:56:0x00d9  */
    /* JADX WARNING: Removed duplicated region for block: B:55:0x00d2  */
    /* JADX WARNING: Removed duplicated region for block: B:54:0x00ca  */
    /* JADX WARNING: Removed duplicated region for block: B:53:0x00c6  */
    /* JADX WARNING: Removed duplicated region for block: B:52:0x00bf  */
    /* JADX WARNING: Removed duplicated region for block: B:104:0x018d  */
    /* JADX WARNING: Removed duplicated region for block: B:102:0x0177  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private int runCompile() throws RemoteException {
        PackageManagerShellCommand failedPackages = this;
        PrintWriter pw = getOutPrintWriter();
        boolean checkProfiles = SystemProperties.getBoolean("dalvik.vm.usejitprofiles", false);
        boolean forceCompilation = false;
        boolean allPackages = false;
        boolean clearProfileData = false;
        String compilerFilter = null;
        String compilationReason = null;
        String checkProfilesRaw = null;
        boolean secondaryDex = false;
        String split = null;
        while (true) {
            String nextOption = getNextOption();
            String opt = nextOption;
            int hashCode;
            StringBuilder stringBuilder;
            if (nextOption != null) {
                int i;
                boolean forceCompilation2;
                hashCode = opt.hashCode();
                if (hashCode != -1615291473) {
                    if (hashCode != -1614046854) {
                        if (hashCode != 1492) {
                            if (hashCode != 1494) {
                                if (hashCode != 1497) {
                                    if (hashCode != 1504) {
                                        if (hashCode != 1509) {
                                            if (hashCode != 1269477022) {
                                                if (hashCode == 1690714782 && opt.equals("--check-prof")) {
                                                    i = 5;
                                                    switch (i) {
                                                        case 0:
                                                            allPackages = true;
                                                            continue;
                                                            continue;
                                                            continue;
                                                            continue;
                                                            continue;
                                                            continue;
                                                            continue;
                                                            continue;
                                                            continue;
                                                            continue;
                                                        case 1:
                                                            clearProfileData = true;
                                                            continue;
                                                            continue;
                                                            continue;
                                                            continue;
                                                            continue;
                                                            continue;
                                                            continue;
                                                            continue;
                                                            continue;
                                                            continue;
                                                        case 2:
                                                            forceCompilation2 = true;
                                                            break;
                                                        case 3:
                                                            compilerFilter = getNextArgRequired();
                                                            continue;
                                                            continue;
                                                            continue;
                                                            continue;
                                                            continue;
                                                            continue;
                                                            continue;
                                                            continue;
                                                            continue;
                                                            continue;
                                                        case 4:
                                                            compilationReason = getNextArgRequired();
                                                            continue;
                                                            continue;
                                                            continue;
                                                            continue;
                                                            continue;
                                                            continue;
                                                            continue;
                                                            continue;
                                                            continue;
                                                            continue;
                                                        case 5:
                                                            checkProfilesRaw = getNextArgRequired();
                                                            continue;
                                                            continue;
                                                            continue;
                                                            continue;
                                                            continue;
                                                            continue;
                                                            continue;
                                                            continue;
                                                            continue;
                                                            continue;
                                                        case 6:
                                                            forceCompilation2 = true;
                                                            compilationReason = "install";
                                                            clearProfileData = true;
                                                            break;
                                                        case 7:
                                                            secondaryDex = true;
                                                            continue;
                                                            continue;
                                                            continue;
                                                            continue;
                                                            continue;
                                                            continue;
                                                            continue;
                                                            continue;
                                                            continue;
                                                            continue;
                                                        case 8:
                                                            split = getNextArgRequired();
                                                            continue;
                                                            continue;
                                                            continue;
                                                            continue;
                                                            continue;
                                                            continue;
                                                            continue;
                                                            continue;
                                                            continue;
                                                            continue;
                                                        default:
                                                            stringBuilder = new StringBuilder();
                                                            stringBuilder.append("Error: Unknown option: ");
                                                            stringBuilder.append(opt);
                                                            pw.println(stringBuilder.toString());
                                                            return 1;
                                                    }
                                                    forceCompilation = forceCompilation2;
                                                }
                                            } else if (opt.equals("--secondary-dex")) {
                                                i = 7;
                                                switch (i) {
                                                    case 0:
                                                        break;
                                                    case 1:
                                                        break;
                                                    case 2:
                                                        break;
                                                    case 3:
                                                        break;
                                                    case 4:
                                                        break;
                                                    case 5:
                                                        break;
                                                    case 6:
                                                        break;
                                                    case 7:
                                                        break;
                                                    case 8:
                                                        break;
                                                    default:
                                                        break;
                                                }
                                                forceCompilation = forceCompilation2;
                                            }
                                        } else if (opt.equals("-r")) {
                                            i = 4;
                                            switch (i) {
                                                case 0:
                                                    break;
                                                case 1:
                                                    break;
                                                case 2:
                                                    break;
                                                case 3:
                                                    break;
                                                case 4:
                                                    break;
                                                case 5:
                                                    break;
                                                case 6:
                                                    break;
                                                case 7:
                                                    break;
                                                case 8:
                                                    break;
                                                default:
                                                    break;
                                            }
                                            forceCompilation = forceCompilation2;
                                        }
                                    } else if (opt.equals("-m")) {
                                        i = 3;
                                        switch (i) {
                                            case 0:
                                                break;
                                            case 1:
                                                break;
                                            case 2:
                                                break;
                                            case 3:
                                                break;
                                            case 4:
                                                break;
                                            case 5:
                                                break;
                                            case 6:
                                                break;
                                            case 7:
                                                break;
                                            case 8:
                                                break;
                                            default:
                                                break;
                                        }
                                        forceCompilation = forceCompilation2;
                                    }
                                } else if (opt.equals("-f")) {
                                    i = 2;
                                    switch (i) {
                                        case 0:
                                            break;
                                        case 1:
                                            break;
                                        case 2:
                                            break;
                                        case 3:
                                            break;
                                        case 4:
                                            break;
                                        case 5:
                                            break;
                                        case 6:
                                            break;
                                        case 7:
                                            break;
                                        case 8:
                                            break;
                                        default:
                                            break;
                                    }
                                    forceCompilation = forceCompilation2;
                                }
                            } else if (opt.equals("-c")) {
                                i = 1;
                                switch (i) {
                                    case 0:
                                        break;
                                    case 1:
                                        break;
                                    case 2:
                                        break;
                                    case 3:
                                        break;
                                    case 4:
                                        break;
                                    case 5:
                                        break;
                                    case 6:
                                        break;
                                    case 7:
                                        break;
                                    case 8:
                                        break;
                                    default:
                                        break;
                                }
                                forceCompilation = forceCompilation2;
                            }
                        } else if (opt.equals("-a")) {
                            i = 0;
                            switch (i) {
                                case 0:
                                    break;
                                case 1:
                                    break;
                                case 2:
                                    break;
                                case 3:
                                    break;
                                case 4:
                                    break;
                                case 5:
                                    break;
                                case 6:
                                    break;
                                case 7:
                                    break;
                                case 8:
                                    break;
                                default:
                                    break;
                            }
                            forceCompilation = forceCompilation2;
                        }
                    } else if (opt.equals("--split")) {
                        i = 8;
                        switch (i) {
                            case 0:
                                break;
                            case 1:
                                break;
                            case 2:
                                break;
                            case 3:
                                break;
                            case 4:
                                break;
                            case 5:
                                break;
                            case 6:
                                break;
                            case 7:
                                break;
                            case 8:
                                break;
                            default:
                                break;
                        }
                        forceCompilation = forceCompilation2;
                    }
                } else if (opt.equals("--reset")) {
                    i = 6;
                    switch (i) {
                        case 0:
                            break;
                        case 1:
                            break;
                        case 2:
                            break;
                        case 3:
                            break;
                        case 4:
                            break;
                        case 5:
                            break;
                        case 6:
                            break;
                        case 7:
                            break;
                        case 8:
                            break;
                        default:
                            break;
                    }
                    forceCompilation = forceCompilation2;
                }
                i = -1;
                switch (i) {
                    case 0:
                        break;
                    case 1:
                        break;
                    case 2:
                        break;
                    case 3:
                        break;
                    case 4:
                        break;
                    case 5:
                        break;
                    case 6:
                        break;
                    case 7:
                        break;
                    case 8:
                        break;
                    default:
                        break;
                }
                forceCompilation = forceCompilation2;
            } else {
                if (checkProfilesRaw != null) {
                    if ("true".equals(checkProfilesRaw)) {
                        checkProfiles = true;
                    } else if ("false".equals(checkProfilesRaw)) {
                        checkProfiles = false;
                    } else {
                        pw.println("Invalid value for \"--check-prof\". Expected \"true\" or \"false\".");
                        return 1;
                    }
                }
                if (compilerFilter != null && compilationReason != null) {
                    pw.println("Cannot use compilation filter (\"-m\") and compilation reason (\"-r\") at the same time");
                    return 1;
                } else if (compilerFilter == null && compilationReason == null) {
                    pw.println("Cannot run without any of compilation filter (\"-m\") and compilation reason (\"-r\") at the same time");
                    return 1;
                } else if (allPackages && split != null) {
                    pw.println("-a cannot be specified together with --split");
                    return 1;
                } else if (!secondaryDex || split == null) {
                    String targetCompilerFilter;
                    StringBuilder stringBuilder2;
                    List<String> packageNames;
                    int i2;
                    String packageName;
                    List<String> failedPackages2;
                    List<String> packageNames2;
                    String opt2;
                    if (compilerFilter == null) {
                        targetCompilerFilter = -1;
                        hashCode = 0;
                        while (hashCode < PackageManagerServiceCompilerMapping.REASON_STRINGS.length) {
                            if (PackageManagerServiceCompilerMapping.REASON_STRINGS[hashCode].equals(compilationReason)) {
                                targetCompilerFilter = hashCode;
                                if (targetCompilerFilter != -1) {
                                    stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append("Error: Unknown compilation reason: ");
                                    stringBuilder2.append(compilationReason);
                                    pw.println(stringBuilder2.toString());
                                    return 1;
                                }
                                targetCompilerFilter = PackageManagerServiceCompilerMapping.getCompilerFilterForReason(targetCompilerFilter);
                            } else {
                                hashCode++;
                            }
                        }
                        if (targetCompilerFilter != -1) {
                        }
                    } else if (DexFile.isValidCompilerFilter(compilerFilter)) {
                        targetCompilerFilter = compilerFilter;
                    } else {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Error: \"");
                        stringBuilder.append(compilerFilter);
                        stringBuilder.append("\" is not a valid compilation filter.");
                        pw.println(stringBuilder.toString());
                        return 1;
                    }
                    if (allPackages) {
                        packageNames = failedPackages.mInterface.getAllPackages();
                        i2 = 1;
                    } else {
                        packageName = getNextArg();
                        if (packageName == null) {
                            pw.println("Error: package name not specified");
                            return 1;
                        }
                        i2 = 1;
                        packageNames = Collections.singletonList(packageName);
                    }
                    List<String> failedPackages3 = new ArrayList();
                    int index = 0;
                    Iterator compilerFilter2 = packageNames.iterator();
                    while (compilerFilter2.hasNext()) {
                        boolean allPackages2;
                        int index2;
                        boolean result;
                        Iterator it = compilerFilter2;
                        compilerFilter = (String) compilerFilter2.next();
                        if (clearProfileData) {
                            failedPackages.mInterface.clearApplicationProfileData(compilerFilter);
                        }
                        if (allPackages) {
                            StringBuilder stringBuilder3 = new StringBuilder();
                            allPackages2 = allPackages;
                            allPackages = index + 1;
                            stringBuilder3.append(allPackages);
                            index2 = allPackages;
                            stringBuilder3.append(SliceAuthority.DELIMITER);
                            stringBuilder3.append(packageNames.size());
                            stringBuilder3.append(": ");
                            stringBuilder3.append(compilerFilter);
                            pw.println(stringBuilder3.toString());
                            pw.flush();
                        } else {
                            allPackages2 = allPackages;
                            index2 = index;
                        }
                        if (secondaryDex) {
                            failedPackages2 = failedPackages3;
                            packageNames2 = packageNames;
                            opt2 = opt;
                            result = failedPackages.mInterface.performDexOptSecondary(compilerFilter, targetCompilerFilter, forceCompilation);
                            allPackages = true;
                        } else {
                            List<String> failedPackages4 = failedPackages3;
                            IPackageManager iPackageManager = failedPackages.mInterface;
                            packageNames2 = packageNames;
                            failedPackages2 = failedPackages4;
                            allPackages = true;
                            opt2 = opt;
                            result = iPackageManager.performDexOptMode(compilerFilter, checkProfiles, targetCompilerFilter, forceCompilation, true, split);
                        }
                        if (!result) {
                            failedPackages2.add(compilerFilter);
                        }
                        failedPackages3 = failedPackages2;
                        boolean z = allPackages;
                        opt = opt2;
                        compilerFilter2 = it;
                        allPackages = allPackages2;
                        index = index2;
                        packageNames = packageNames2;
                        failedPackages = this;
                    }
                    failedPackages2 = failedPackages3;
                    packageNames2 = packageNames;
                    int i3 = i2;
                    opt2 = opt;
                    if (failedPackages2.isEmpty()) {
                        pw.println("Success");
                        return 0;
                    } else if (failedPackages2.size() == i3) {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Failure: package ");
                        stringBuilder2.append((String) failedPackages2.get(0));
                        stringBuilder2.append(" could not be compiled");
                        pw.println(stringBuilder2.toString());
                        return i3;
                    } else {
                        pw.print("Failure: the following packages could not be compiled: ");
                        boolean is_first = true;
                        for (String packageName2 : failedPackages2) {
                            if (is_first) {
                                is_first = false;
                            } else {
                                pw.print(", ");
                            }
                            pw.print(packageName2);
                        }
                        pw.println();
                        return i3;
                    }
                } else {
                    pw.println("--secondary-dex cannot be specified together with --split");
                    return 1;
                }
            }
        }
    }

    private int runreconcileSecondaryDexFiles() throws RemoteException {
        this.mInterface.reconcileSecondaryDexFiles(getNextArg());
        return 0;
    }

    public int runForceDexOpt() throws RemoteException {
        this.mInterface.forceDexOpt(getNextArgRequired());
        return 0;
    }

    private int runDexoptJob() throws RemoteException {
        List<String> packageNames = new ArrayList();
        while (true) {
            String nextArg = getNextArg();
            String arg = nextArg;
            if (nextArg == null) {
                break;
            }
            packageNames.add(arg);
        }
        return this.mInterface.runBackgroundDexoptJob(packageNames.isEmpty() ? null : packageNames) ? 0 : -1;
    }

    private int runDumpProfiles() throws RemoteException {
        this.mInterface.dumpProfiles(getNextArg());
        return 0;
    }

    private int runSnapshotProfile() throws RemoteException {
        OutputStream outStream;
        Throwable th;
        Throwable th2;
        PrintWriter pw = getOutPrintWriter();
        String packageName = getNextArg();
        boolean isBootImage = PackageManagerService.PLATFORM_PACKAGE_NAME.equals(packageName);
        String codePath = null;
        while (true) {
            String nextArg = getNextArg();
            String opt = nextArg;
            int i = 0;
            if (nextArg != null) {
                if (!(opt.hashCode() == -684928411 && opt.equals("--code-path"))) {
                    i = -1;
                }
                if (i != 0) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Unknown arg: ");
                    stringBuilder.append(opt);
                    pw.write(stringBuilder.toString());
                    return -1;
                } else if (isBootImage) {
                    pw.write("--code-path cannot be used for the boot image.");
                    return -1;
                } else {
                    codePath = getNextArg();
                }
            } else {
                nextArg = null;
                if (!isBootImage) {
                    PackageInfo packageInfo = this.mInterface.getPackageInfo(packageName, 0, 0);
                    if (packageInfo == null) {
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Package not found ");
                        stringBuilder2.append(packageName);
                        pw.write(stringBuilder2.toString());
                        return -1;
                    }
                    nextArg = packageInfo.applicationInfo.getBaseCodePath();
                    if (codePath == null) {
                        codePath = nextArg;
                    }
                }
                String codePath2 = codePath;
                String baseCodePath = nextArg;
                ISnapshotRuntimeProfileCallback snapshotRuntimeProfileCallback = new SnapshotRuntimeProfileCallback();
                String callingPackage = Binder.getCallingUid() == 0 ? "root" : "com.android.shell";
                int profileType = isBootImage ? 1 : 0;
                if (this.mInterface.getArtManager().isRuntimeProfilingEnabled(profileType, callingPackage)) {
                    SnapshotRuntimeProfileCallback callback = snapshotRuntimeProfileCallback;
                    String baseCodePath2 = baseCodePath;
                    this.mInterface.getArtManager().snapshotRuntimeProfile(profileType, packageName, codePath2, snapshotRuntimeProfileCallback, callingPackage);
                    SnapshotRuntimeProfileCallback callback2 = callback;
                    if (callback2.waitTillDone()) {
                        InputStream inStream;
                        try {
                            StringBuilder stringBuilder3;
                            String outputFileSuffix;
                            inStream = new AutoCloseInputStream(callback2.mProfileReadFd);
                            if (!isBootImage) {
                                if (!Objects.equals(baseCodePath2, codePath2)) {
                                    stringBuilder3 = new StringBuilder();
                                    stringBuilder3.append(STDIN_PATH);
                                    stringBuilder3.append(new File(codePath2).getName());
                                    codePath = stringBuilder3.toString();
                                    outputFileSuffix = codePath;
                                    stringBuilder3 = new StringBuilder();
                                    stringBuilder3.append(ART_PROFILE_SNAPSHOT_DEBUG_LOCATION);
                                    stringBuilder3.append(packageName);
                                    stringBuilder3.append(outputFileSuffix);
                                    stringBuilder3.append(".prof");
                                    callingPackage = stringBuilder3.toString();
                                    outStream = new FileOutputStream(callingPackage);
                                    Streams.copy(inStream, outStream);
                                    $closeResource(null, outStream);
                                    Os.chmod(callingPackage, 420);
                                    $closeResource(null, inStream);
                                    return 0;
                                }
                            }
                            codePath = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
                            outputFileSuffix = codePath;
                            stringBuilder3 = new StringBuilder();
                            stringBuilder3.append(ART_PROFILE_SNAPSHOT_DEBUG_LOCATION);
                            stringBuilder3.append(packageName);
                            stringBuilder3.append(outputFileSuffix);
                            stringBuilder3.append(".prof");
                            callingPackage = stringBuilder3.toString();
                            outStream = new FileOutputStream(callingPackage);
                            try {
                                Streams.copy(inStream, outStream);
                                $closeResource(null, outStream);
                                Os.chmod(callingPackage, 420);
                                $closeResource(null, inStream);
                                return 0;
                            } catch (Throwable th3) {
                                th2 = th3;
                            }
                        } catch (ErrnoException | IOException e) {
                            StringBuilder stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("Error when reading the profile fd: ");
                            stringBuilder4.append(e.getMessage());
                            pw.println(stringBuilder4.toString());
                            e.printStackTrace(pw);
                            return -1;
                        } catch (Throwable th22) {
                        }
                    }
                    pw.println("Error: callback not called");
                    return callback2.mErrCode;
                }
                pw.println("Error: Runtime profiling is not enabled");
                return -1;
            }
        }
        $closeResource(th, outStream);
        throw th22;
    }

    private static /* synthetic */ void $closeResource(Throwable x0, AutoCloseable x1) {
        if (x0 != null) {
            try {
                x1.close();
                return;
            } catch (Throwable th) {
                x0.addSuppressed(th);
                return;
            }
        }
        x1.close();
    }

    private int runUninstall() throws RemoteException {
        PrintWriter pw = getOutPrintWriter();
        int flags = 0;
        int userId = -1;
        long versionCode = -1;
        while (true) {
            String nextOption = getNextOption();
            String opt = nextOption;
            int i = -1;
            if (nextOption != null) {
                int hashCode = opt.hashCode();
                if (hashCode != 1502) {
                    if (hashCode != 1333469547) {
                        if (hashCode == 1884113221 && opt.equals("--versionCode")) {
                            i = 2;
                        }
                    } else if (opt.equals("--user")) {
                        i = 1;
                    }
                } else if (opt.equals("-k")) {
                    i = 0;
                }
                switch (i) {
                    case 0:
                        flags |= 1;
                        break;
                    case 1:
                        userId = UserHandle.parseUserArg(getNextArgRequired());
                        break;
                    case 2:
                        versionCode = Long.parseLong(getNextArgRequired());
                        break;
                    default:
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Error: Unknown option: ");
                        stringBuilder.append(opt);
                        pw.println(stringBuilder.toString());
                        return 1;
                }
            }
            nextOption = getNextArg();
            if (nextOption == null) {
                pw.println("Error: package name not specified");
                return 1;
            }
            String splitName = getNextArg();
            if (splitName != null) {
                return runRemoveSplit(nextOption, splitName);
            }
            StringBuilder stringBuilder2;
            userId = translateUserId(userId, true, "runUninstall");
            if (userId == -1) {
                userId = 0;
                flags |= 2;
            } else {
                PackageInfo info = this.mInterface.getPackageInfo(nextOption, 67108864, userId);
                if (info == null) {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Failure [not installed for ");
                    stringBuilder2.append(userId);
                    stringBuilder2.append("]");
                    pw.println(stringBuilder2.toString());
                    return 1;
                }
                if ((info.applicationInfo.flags & 1) != 0) {
                    flags |= 4;
                }
            }
            LocalIntentReceiver receiver = new LocalIntentReceiver();
            this.mInterface.getPackageInstaller().uninstall(new VersionedPackage(nextOption, versionCode), null, flags, receiver.getIntentSender(), userId);
            Intent result = receiver.getResult();
            if (result.getIntExtra("android.content.pm.extra.STATUS", 1) == 0) {
                pw.println("Success");
                return 0;
            }
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Failure [");
            stringBuilder2.append(result.getStringExtra("android.content.pm.extra.STATUS_MESSAGE"));
            stringBuilder2.append("]");
            pw.println(stringBuilder2.toString());
            return 1;
        }
    }

    private int runRemoveSplit(String packageName, String splitName) throws RemoteException {
        PrintWriter pw = getOutPrintWriter();
        SessionParams sessionParams = new SessionParams(2);
        sessionParams.installFlags = 2 | sessionParams.installFlags;
        sessionParams.appPackageName = packageName;
        int sessionId = doCreateSession(sessionParams, 0, -1);
        boolean abandonSession = true;
        try {
            if (doRemoveSplit(sessionId, splitName, false) != 0) {
                if (abandonSession) {
                    try {
                        doAbandonSession(sessionId, false);
                    } catch (Exception e) {
                    }
                }
                return 1;
            } else if (doCommitSession(sessionId, false) != 0) {
                if (abandonSession) {
                    try {
                        doAbandonSession(sessionId, false);
                    } catch (Exception e2) {
                    }
                }
                return 1;
            } else {
                abandonSession = false;
                pw.println("Success");
                if (null != null) {
                    try {
                        doAbandonSession(sessionId, false);
                    } catch (Exception e3) {
                    }
                }
                return 0;
            }
        } catch (Throwable th) {
            if (abandonSession) {
                try {
                    doAbandonSession(sessionId, false);
                } catch (Exception e4) {
                }
            }
        }
    }

    private int runClear() throws RemoteException {
        int userId = 0;
        String option = getNextOption();
        if (option != null && option.equals("--user")) {
            userId = UserHandle.parseUserArg(getNextArgRequired());
        }
        String pkg = getNextArg();
        if (pkg == null) {
            getErrPrintWriter().println("Error: no package specified");
            return 1;
        }
        ClearDataObserver obs = new ClearDataObserver();
        ActivityManager.getService().clearApplicationUserData(pkg, false, obs, userId);
        synchronized (obs) {
            while (!obs.finished) {
                try {
                    obs.wait();
                } catch (InterruptedException e) {
                }
            }
        }
        if (obs.result) {
            getOutPrintWriter().println("Success");
            return 0;
        }
        getErrPrintWriter().println("Failed");
        return 1;
    }

    private static String enabledSettingToString(int state) {
        switch (state) {
            case 0:
                return HealthServiceWrapper.INSTANCE_VENDOR;
            case 1:
                return "enabled";
            case 2:
                return "disabled";
            case 3:
                return "disabled-user";
            case 4:
                return "disabled-until-used";
            default:
                return Shell.NIGHT_MODE_STR_UNKNOWN;
        }
    }

    private int runSetEnabledSetting(int state) throws RemoteException {
        int userId = 0;
        String option = getNextOption();
        if (option != null && option.equals("--user")) {
            userId = UserHandle.parseUserArg(getNextArgRequired());
        }
        String pkg = getNextArg();
        if (pkg == null) {
            getErrPrintWriter().println("Error: no package or component specified");
            return 1;
        }
        ComponentName cn = ComponentName.unflattenFromString(pkg);
        StringBuilder stringBuilder;
        PrintWriter outPrintWriter;
        if (cn == null) {
            IPackageManager iPackageManager = this.mInterface;
            stringBuilder = new StringBuilder();
            stringBuilder.append("shell:");
            stringBuilder.append(Process.myUid());
            iPackageManager.setApplicationEnabledSetting(pkg, state, 0, userId, stringBuilder.toString());
            outPrintWriter = getOutPrintWriter();
            stringBuilder = new StringBuilder();
            stringBuilder.append("Package ");
            stringBuilder.append(pkg);
            stringBuilder.append(" new state: ");
            stringBuilder.append(enabledSettingToString(this.mInterface.getApplicationEnabledSetting(pkg, userId)));
            outPrintWriter.println(stringBuilder.toString());
            return 0;
        }
        this.mInterface.setComponentEnabledSetting(cn, state, 0, userId);
        outPrintWriter = getOutPrintWriter();
        stringBuilder = new StringBuilder();
        stringBuilder.append("Component ");
        stringBuilder.append(cn.toShortString());
        stringBuilder.append(" new state: ");
        stringBuilder.append(enabledSettingToString(this.mInterface.getComponentEnabledSetting(cn, userId)));
        outPrintWriter.println(stringBuilder.toString());
        return 0;
    }

    private int runSetHiddenSetting(boolean state) throws RemoteException {
        int userId = 0;
        String option = getNextOption();
        if (option != null && option.equals("--user")) {
            userId = UserHandle.parseUserArg(getNextArgRequired());
        }
        String pkg = getNextArg();
        if (pkg == null) {
            getErrPrintWriter().println("Error: no package or component specified");
            return 1;
        }
        this.mInterface.setApplicationHiddenSettingAsUser(pkg, state, userId);
        PrintWriter outPrintWriter = getOutPrintWriter();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Package ");
        stringBuilder.append(pkg);
        stringBuilder.append(" new hidden state: ");
        stringBuilder.append(this.mInterface.getApplicationHiddenSettingAsUser(pkg, userId));
        outPrintWriter.println(stringBuilder.toString());
        return 0;
    }

    /* JADX WARNING: Missing block: B:6:0x002d, code skipped:
            if (r13.equals("--user") != false) goto L_0x0077;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private int runSuspend(boolean suspendedState) {
        Exception e;
        PrintWriter pw = getOutPrintWriter();
        String dialogMessage = null;
        PersistableBundle appExtras = new PersistableBundle();
        int userId = 0;
        PersistableBundle launcherExtras = new PersistableBundle();
        while (true) {
            String nextOption = getNextOption();
            String opt = nextOption;
            int i = 0;
            StringBuilder stringBuilder;
            if (nextOption != null) {
                switch (opt.hashCode()) {
                    case -39471105:
                        if (opt.equals("--dialogMessage")) {
                            i = 1;
                            break;
                        }
                    case 42995488:
                        if (opt.equals("--aed")) {
                            i = 4;
                            break;
                        }
                    case 42995496:
                        if (opt.equals("--ael")) {
                            i = 2;
                            break;
                        }
                    case 42995503:
                        if (opt.equals("--aes")) {
                            i = 3;
                            break;
                        }
                    case 43006059:
                        if (opt.equals("--led")) {
                            i = 7;
                            break;
                        }
                    case 43006067:
                        if (opt.equals("--lel")) {
                            i = 5;
                            break;
                        }
                    case 43006074:
                        if (opt.equals("--les")) {
                            i = 6;
                            break;
                        }
                    case 1333469547:
                        break;
                    default:
                        i = -1;
                        break;
                }
                switch (i) {
                    case 0:
                        userId = UserHandle.parseUserArg(getNextArgRequired());
                        break;
                    case 1:
                        dialogMessage = getNextArgRequired();
                        break;
                    case 2:
                    case 3:
                    case 4:
                    case 5:
                    case 6:
                    case 7:
                        nextOption = getNextArgRequired();
                        String val = getNextArgRequired();
                        if (!suspendedState) {
                            break;
                        }
                        PersistableBundle bundleToInsert = opt.startsWith("--a") ? appExtras : launcherExtras;
                        char charAt = opt.charAt(4);
                        if (charAt == 'd') {
                            bundleToInsert.putDouble(nextOption, Double.valueOf(val).doubleValue());
                        } else if (charAt == 'l') {
                            bundleToInsert.putLong(nextOption, Long.valueOf(val).longValue());
                        } else if (charAt == 's') {
                            bundleToInsert.putString(nextOption, val);
                        }
                        break;
                    default:
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Error: Unknown option: ");
                        stringBuilder.append(opt);
                        pw.println(stringBuilder.toString());
                        return 1;
                }
            }
            String packageName = getNextArg();
            if (packageName == null) {
                pw.println("Error: package name not specified");
                return 1;
            }
            String packageName2;
            try {
                packageName2 = packageName;
                try {
                    this.mInterface.setPackagesSuspendedAsUser(new String[]{packageName}, suspendedState, appExtras, launcherExtras, dialogMessage, Binder.getCallingUid() == 0 ? "root" : "com.android.shell", userId);
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Package ");
                    stringBuilder.append(packageName2);
                    stringBuilder.append(" new suspended state: ");
                    stringBuilder.append(this.mInterface.isPackageSuspendedForUser(packageName2, userId));
                    pw.println(stringBuilder.toString());
                    return 0;
                } catch (RemoteException | IllegalArgumentException e2) {
                    e = e2;
                    pw.println(e.toString());
                    return 1;
                }
            } catch (RemoteException | IllegalArgumentException e3) {
                e = e3;
                packageName2 = packageName;
                pw.println(e.toString());
                return 1;
            }
        }
    }

    private int runGrantRevokePermission(boolean grant) throws RemoteException {
        String nextOption;
        int userId = 0;
        while (true) {
            nextOption = getNextOption();
            String opt = nextOption;
            if (nextOption == null) {
                break;
            } else if (opt.equals("--user")) {
                userId = UserHandle.parseUserArg(getNextArgRequired());
            }
        }
        nextOption = getNextArg();
        if (nextOption == null) {
            getErrPrintWriter().println("Error: no package specified");
            return 1;
        }
        String perm = getNextArg();
        if (perm == null) {
            getErrPrintWriter().println("Error: no permission specified");
            return 1;
        }
        if (grant) {
            this.mInterface.grantRuntimePermission(nextOption, perm, userId);
        } else {
            this.mInterface.revokeRuntimePermission(nextOption, perm, userId);
        }
        return 0;
    }

    private int runResetPermissions() throws RemoteException {
        this.mInterface.resetRuntimePermissions();
        return 0;
    }

    private int runSetPermissionEnforced() throws RemoteException {
        String permission = getNextArg();
        if (permission == null) {
            getErrPrintWriter().println("Error: no permission specified");
            return 1;
        }
        String enforcedRaw = getNextArg();
        if (enforcedRaw == null) {
            getErrPrintWriter().println("Error: no enforcement specified");
            return 1;
        }
        this.mInterface.setPermissionEnforced(permission, Boolean.parseBoolean(enforcedRaw));
        return 0;
    }

    private boolean isVendorApp(String pkg) {
        boolean z = false;
        try {
            PackageInfo info = this.mInterface.getPackageInfo(pkg, 0, 0);
            if (info != null && info.applicationInfo.isVendor()) {
                z = true;
            }
            return z;
        } catch (RemoteException e) {
            return false;
        }
    }

    private boolean isProductApp(String pkg) {
        boolean z = false;
        try {
            PackageInfo info = this.mInterface.getPackageInfo(pkg, 0, 0);
            if (info != null && info.applicationInfo.isProduct()) {
                z = true;
            }
            return z;
        } catch (RemoteException e) {
            return false;
        }
    }

    private int runGetPrivappPermissions() {
        String pkg = getNextArg();
        if (pkg == null) {
            getErrPrintWriter().println("Error: no package specified.");
            return 1;
        }
        ArraySet<String> privAppPermissions;
        if (isVendorApp(pkg)) {
            privAppPermissions = SystemConfig.getInstance().getVendorPrivAppPermissions(pkg);
        } else if (isProductApp(pkg)) {
            privAppPermissions = SystemConfig.getInstance().getProductPrivAppPermissions(pkg);
        } else {
            privAppPermissions = SystemConfig.getInstance().getPrivAppPermissions(pkg);
        }
        getOutPrintWriter().println(privAppPermissions == null ? "{}" : privAppPermissions.toString());
        return 0;
    }

    private int runGetPrivappDenyPermissions() {
        String pkg = getNextArg();
        if (pkg == null) {
            getErrPrintWriter().println("Error: no package specified.");
            return 1;
        }
        ArraySet<String> privAppPermissions;
        if (isVendorApp(pkg)) {
            privAppPermissions = SystemConfig.getInstance().getVendorPrivAppDenyPermissions(pkg);
        } else if (isProductApp(pkg)) {
            privAppPermissions = SystemConfig.getInstance().getProductPrivAppDenyPermissions(pkg);
        } else {
            privAppPermissions = SystemConfig.getInstance().getPrivAppDenyPermissions(pkg);
        }
        getOutPrintWriter().println(privAppPermissions == null ? "{}" : privAppPermissions.toString());
        return 0;
    }

    private int runGetOemPermissions() {
        String pkg = getNextArg();
        if (pkg == null) {
            getErrPrintWriter().println("Error: no package specified.");
            return 1;
        }
        Map<String, Boolean> oemPermissions = SystemConfig.getInstance().getOemPermissions(pkg);
        if (oemPermissions == null || oemPermissions.isEmpty()) {
            getOutPrintWriter().println("{}");
        } else {
            oemPermissions.forEach(new -$$Lambda$PackageManagerShellCommand$-OZpz58K2HXVuHDuVYKnCu6oo4c(this));
        }
        return 0;
    }

    public static /* synthetic */ void lambda$runGetOemPermissions$0(PackageManagerShellCommand packageManagerShellCommand, String permission, Boolean granted) {
        PrintWriter outPrintWriter = packageManagerShellCommand.getOutPrintWriter();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(permission);
        stringBuilder.append(" granted:");
        stringBuilder.append(granted);
        outPrintWriter.println(stringBuilder.toString());
    }

    private String linkStateToString(int state) {
        switch (state) {
            case 0:
                return "undefined";
            case 1:
                return "ask";
            case 2:
                return "always";
            case 3:
                return "never";
            case 4:
                return "always ask";
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unknown link state: ");
                stringBuilder.append(state);
                return stringBuilder.toString();
        }
    }

    /* Code decompiled incorrectly, please refer to instructions dump. */
    private int runSetAppLink() throws RemoteException {
        int userId = 0;
        while (true) {
            String nextOption = getNextOption();
            String opt = nextOption;
            PrintWriter errPrintWriter;
            if (nextOption == null) {
                nextOption = getNextArg();
                if (nextOption == null) {
                    getErrPrintWriter().println("Error: no package specified.");
                    return 1;
                }
                String modeString = getNextArg();
                if (modeString == null) {
                    getErrPrintWriter().println("Error: no app link state specified.");
                    return 1;
                }
                int i;
                String toLowerCase = modeString.toLowerCase();
                switch (toLowerCase.hashCode()) {
                    case -1414557169:
                        if (toLowerCase.equals("always")) {
                            i = 1;
                            break;
                        }
                    case -1038130864:
                        if (toLowerCase.equals("undefined")) {
                            i = 0;
                            break;
                        }
                    case 96889:
                        if (toLowerCase.equals("ask")) {
                            i = 2;
                            break;
                        }
                    case 104712844:
                        if (toLowerCase.equals("never")) {
                            i = 4;
                            break;
                        }
                    case 1182785979:
                        if (toLowerCase.equals("always-ask")) {
                            i = 3;
                            break;
                        }
                    default:
                        i = -1;
                        break;
                }
                switch (i) {
                    case 0:
                        i = 0;
                        break;
                    case 1:
                        i = 2;
                        break;
                    case 2:
                        i = 1;
                        break;
                    case 3:
                        i = 4;
                        break;
                    case 4:
                        i = 3;
                        break;
                    default:
                        errPrintWriter = getErrPrintWriter();
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Error: unknown app link state '");
                        stringBuilder.append(modeString);
                        stringBuilder.append("'");
                        errPrintWriter.println(stringBuilder.toString());
                        return 1;
                }
                PackageInfo info = this.mInterface.getPackageInfo(nextOption, 0, userId);
                StringBuilder stringBuilder2;
                if (info == null) {
                    errPrintWriter = getErrPrintWriter();
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Error: package ");
                    stringBuilder2.append(nextOption);
                    stringBuilder2.append(" not found.");
                    errPrintWriter.println(stringBuilder2.toString());
                    return 1;
                } else if ((info.applicationInfo.privateFlags & 16) == 0) {
                    errPrintWriter = getErrPrintWriter();
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Error: package ");
                    stringBuilder2.append(nextOption);
                    stringBuilder2.append(" does not handle web links.");
                    errPrintWriter.println(stringBuilder2.toString());
                    return 1;
                } else if (this.mInterface.updateIntentVerificationStatus(nextOption, i, userId)) {
                    return 0;
                } else {
                    errPrintWriter = getErrPrintWriter();
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Error: unable to update app link status for ");
                    stringBuilder2.append(nextOption);
                    errPrintWriter.println(stringBuilder2.toString());
                    return 1;
                }
            } else if (opt.equals("--user")) {
                userId = UserHandle.parseUserArg(getNextArgRequired());
            } else {
                errPrintWriter = getErrPrintWriter();
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Error: unknown option: ");
                stringBuilder3.append(opt);
                errPrintWriter.println(stringBuilder3.toString());
                return 1;
            }
        }
    }

    private int runGetAppLink() throws RemoteException {
        int userId = 0;
        while (true) {
            String nextOption = getNextOption();
            String opt = nextOption;
            PrintWriter errPrintWriter;
            if (nextOption == null) {
                nextOption = getNextArg();
                if (nextOption == null) {
                    getErrPrintWriter().println("Error: no package specified.");
                    return 1;
                }
                PackageInfo info = this.mInterface.getPackageInfo(nextOption, 0, userId);
                StringBuilder stringBuilder;
                if (info == null) {
                    errPrintWriter = getErrPrintWriter();
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Error: package ");
                    stringBuilder.append(nextOption);
                    stringBuilder.append(" not found.");
                    errPrintWriter.println(stringBuilder.toString());
                    return 1;
                } else if ((info.applicationInfo.privateFlags & 16) == 0) {
                    errPrintWriter = getErrPrintWriter();
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Error: package ");
                    stringBuilder.append(nextOption);
                    stringBuilder.append(" does not handle web links.");
                    errPrintWriter.println(stringBuilder.toString());
                    return 1;
                } else {
                    getOutPrintWriter().println(linkStateToString(this.mInterface.getIntentVerificationStatus(nextOption, userId)));
                    return 0;
                }
            } else if (opt.equals("--user")) {
                userId = UserHandle.parseUserArg(getNextArgRequired());
            } else {
                errPrintWriter = getErrPrintWriter();
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Error: unknown option: ");
                stringBuilder2.append(opt);
                errPrintWriter.println(stringBuilder2.toString());
                return 1;
            }
        }
    }

    private int runTrimCaches() throws RemoteException {
        String size = getNextArg();
        if (size == null) {
            getErrPrintWriter().println("Error: no size specified");
            return 1;
        }
        String size2 = true;
        int len = size.length();
        char c = size.charAt(len - 1);
        if (c < '0' || c > '9') {
            if (c == 'K' || c == 'k') {
                size2 = 1024;
            } else if (c == 'M' || c == 'm') {
                size2 = DumpState.DUMP_DEXOPT;
            } else if (c == 'G' || c == 'g') {
                size2 = 1073741824;
            } else {
                PrintWriter errPrintWriter = getErrPrintWriter();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Invalid suffix: ");
                stringBuilder.append(c);
                errPrintWriter.println(stringBuilder.toString());
                return 1;
            }
            size = size.substring(0, len - 1);
        }
        long multiplier = size2;
        size2 = size;
        try {
            long sizeVal = Long.parseLong(size2) * multiplier;
            size = getNextArg();
            if ("internal".equals(size)) {
                size = null;
            }
            String volumeUuid = size;
            ClearDataObserver obs = new ClearDataObserver();
            this.mInterface.freeStorageAndNotify(volumeUuid, sizeVal, 2, obs);
            synchronized (obs) {
                while (!obs.finished) {
                    try {
                        obs.wait();
                    } catch (InterruptedException e) {
                    }
                }
            }
            return 0;
        } catch (NumberFormatException e2) {
            NumberFormatException numberFormatException = e2;
            PrintWriter errPrintWriter2 = getErrPrintWriter();
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Error: expected number at: ");
            stringBuilder2.append(size2);
            errPrintWriter2.println(stringBuilder2.toString());
            return 1;
        }
    }

    private static boolean isNumber(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public int runCreateUser() throws RemoteException {
        int userId = -1;
        int flags = 0;
        while (true) {
            String nextOption = getNextOption();
            String opt = nextOption;
            if (nextOption == null) {
                nextOption = getNextArg();
                if (nextOption == null) {
                    getErrPrintWriter().println("Error: no user name specified.");
                    return 1;
                }
                UserInfo info;
                String name = nextOption;
                IUserManager um = IUserManager.Stub.asInterface(ServiceManager.getService("user"));
                IAccountManager accm = IAccountManager.Stub.asInterface(ServiceManager.getService("account"));
                if ((flags & 8) != 0) {
                    int parentUserId = userId >= 0 ? userId : 0;
                    info = um.createRestrictedProfile(name, parentUserId);
                    accm.addSharedAccountsFromParentUser(parentUserId, userId, Process.myUid() == 0 ? "root" : "com.android.shell");
                } else if (userId < 0) {
                    info = um.createUser(name, flags);
                } else {
                    info = um.createProfileForUser(name, flags, userId, null);
                }
                UserInfo info2 = info;
                if (info2 != null) {
                    PrintWriter outPrintWriter = getOutPrintWriter();
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Success: created user id ");
                    stringBuilder.append(info2.id);
                    outPrintWriter.println(stringBuilder.toString());
                    return 0;
                }
                getErrPrintWriter().println("Error: couldn't create User.");
                return 1;
            } else if ("--profileOf".equals(opt)) {
                userId = UserHandle.parseUserArg(getNextArgRequired());
            } else if ("--managed".equals(opt)) {
                flags |= 32;
            } else if ("--restricted".equals(opt)) {
                flags |= 8;
            } else if ("--ephemeral".equals(opt)) {
                flags |= 256;
            } else if ("--guest".equals(opt)) {
                flags |= 4;
            } else if ("--demo".equals(opt)) {
                flags |= 512;
            } else {
                PrintWriter errPrintWriter = getErrPrintWriter();
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Error: unknown option ");
                stringBuilder2.append(opt);
                errPrintWriter.println(stringBuilder2.toString());
                return 1;
            }
        }
    }

    public int runRemoveUser() throws RemoteException {
        String arg = getNextArg();
        if (arg == null) {
            getErrPrintWriter().println("Error: no user id specified.");
            return 1;
        }
        int userId = UserHandle.parseUserArg(arg);
        if (IUserManager.Stub.asInterface(ServiceManager.getService("user")).removeUser(userId)) {
            getOutPrintWriter().println("Success: removed user");
            return 0;
        }
        PrintWriter errPrintWriter = getErrPrintWriter();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Error: couldn't remove user id ");
        stringBuilder.append(userId);
        errPrintWriter.println(stringBuilder.toString());
        return 1;
    }

    public int runSetUserRestriction() throws RemoteException {
        boolean value;
        int userId = 0;
        String opt = getNextOption();
        if (opt != null && "--user".equals(opt)) {
            userId = UserHandle.parseUserArg(getNextArgRequired());
        }
        String restriction = getNextArg();
        String arg = getNextArg();
        if ("1".equals(arg)) {
            value = true;
        } else if ("0".equals(arg)) {
            value = false;
        } else {
            getErrPrintWriter().println("Error: valid value not specified");
            return 1;
        }
        IUserManager.Stub.asInterface(ServiceManager.getService("user")).setUserRestriction(restriction, value, userId);
        return 0;
    }

    public int runGetMaxUsers() {
        PrintWriter outPrintWriter = getOutPrintWriter();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Maximum supported users: ");
        stringBuilder.append(UserManager.getMaxSupportedUsers());
        outPrintWriter.println(stringBuilder.toString());
        return 0;
    }

    public int runGetMaxRunningUsers() {
        ActivityManagerInternal activityManagerInternal = (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
        PrintWriter outPrintWriter = getOutPrintWriter();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Maximum supported running users: ");
        stringBuilder.append(activityManagerInternal.getMaxRunningUsers());
        outPrintWriter.println(stringBuilder.toString());
        return 0;
    }

    private InstallParams makeInstallParams() {
        SessionParams sessionParams = new SessionParams(1);
        InstallParams params = new InstallParams();
        params.sessionParams = sessionParams;
        boolean replaceExisting = true;
        while (true) {
            String nextOption = getNextOption();
            String opt = nextOption;
            if (nextOption == null) {
                return params;
            }
            int i = -1;
            switch (opt.hashCode()) {
                case -1950997763:
                    if (opt.equals("--force-uuid")) {
                        i = 23;
                        break;
                    }
                    break;
                case -1777984902:
                    if (opt.equals("--dont-kill")) {
                        i = 9;
                        break;
                    }
                    break;
                case -1624001065:
                    if (opt.equals("--hwhdb")) {
                        i = 25;
                        break;
                    }
                    break;
                case -1313152697:
                    if (opt.equals("--install-location")) {
                        i = 22;
                        break;
                    }
                    break;
                case -1137116608:
                    if (opt.equals("--instantapp")) {
                        i = 18;
                        break;
                    }
                    break;
                case -951415743:
                    if (opt.equals("--instant")) {
                        i = 17;
                        break;
                    }
                    break;
                case -706813505:
                    if (opt.equals("--referrer")) {
                        i = 11;
                        break;
                    }
                    break;
                case 1477:
                    if (opt.equals("-R")) {
                        i = 2;
                        break;
                    }
                    break;
                case 1478:
                    if (opt.equals("-S")) {
                        i = 14;
                        break;
                    }
                    break;
                case 1495:
                    if (opt.equals("-d")) {
                        i = 7;
                        break;
                    }
                    break;
                case 1497:
                    if (opt.equals("-f")) {
                        i = 6;
                        break;
                    }
                    break;
                case 1498:
                    if (opt.equals("-g")) {
                        i = 8;
                        break;
                    }
                    break;
                case NetworkConstants.ETHER_MTU /*1500*/:
                    if (opt.equals("-i")) {
                        i = 3;
                        break;
                    }
                    break;
                case 1503:
                    if (opt.equals("-l")) {
                        i = false;
                        break;
                    }
                    break;
                case 1507:
                    if (opt.equals("-p")) {
                        i = 12;
                        break;
                    }
                    break;
                case 1509:
                    if (opt.equals("-r")) {
                        i = 1;
                        break;
                    }
                    break;
                case 1510:
                    if (opt.equals("-s")) {
                        i = 5;
                        break;
                    }
                    break;
                case 1511:
                    if (opt.equals("-t")) {
                        i = 4;
                        break;
                    }
                    break;
                case 42995400:
                    if (opt.equals("--abi")) {
                        i = 15;
                        break;
                    }
                    break;
                case 43010092:
                    if (opt.equals("--pkg")) {
                        i = 13;
                        break;
                    }
                    break;
                case 148207464:
                    if (opt.equals("--originating-uri")) {
                        i = 10;
                        break;
                    }
                    break;
                case 1051781117:
                    if (opt.equals("--ephemeral")) {
                        i = 16;
                        break;
                    }
                    break;
                case 1067504745:
                    if (opt.equals("--preload")) {
                        i = 20;
                        break;
                    }
                    break;
                case 1333024815:
                    if (opt.equals("--full")) {
                        i = 19;
                        break;
                    }
                    break;
                case 1333469547:
                    if (opt.equals("--user")) {
                        i = 21;
                        break;
                    }
                    break;
                case 2015272120:
                    if (opt.equals("--force-sdk")) {
                        i = 24;
                        break;
                    }
                    break;
            }
            switch (i) {
                case 0:
                    sessionParams.installFlags |= 1;
                    break;
                case 1:
                    break;
                case 2:
                    replaceExisting = false;
                    break;
                case 3:
                    params.installerPackageName = getNextArg();
                    if (params.installerPackageName == null) {
                        throw new IllegalArgumentException("Missing installer package");
                    }
                    break;
                case 4:
                    sessionParams.installFlags |= 4;
                    break;
                case 5:
                    sessionParams.installFlags |= 8;
                    break;
                case 6:
                    sessionParams.installFlags |= 16;
                    break;
                case 7:
                    sessionParams.installFlags |= 128;
                    break;
                case 8:
                    sessionParams.installFlags |= 256;
                    break;
                case 9:
                    sessionParams.installFlags |= 4096;
                    break;
                case 10:
                    sessionParams.originatingUri = Uri.parse(getNextArg());
                    break;
                case 11:
                    sessionParams.referrerUri = Uri.parse(getNextArg());
                    break;
                case 12:
                    sessionParams.mode = 2;
                    sessionParams.appPackageName = getNextArg();
                    if (sessionParams.appPackageName == null) {
                        throw new IllegalArgumentException("Missing inherit package name");
                    }
                    break;
                case 13:
                    sessionParams.appPackageName = getNextArg();
                    if (sessionParams.appPackageName == null) {
                        throw new IllegalArgumentException("Missing package name");
                    }
                    break;
                case 14:
                    long sizeBytes = Long.parseLong(getNextArg());
                    if (sizeBytes > 0) {
                        sessionParams.setSize(sizeBytes);
                        break;
                    }
                    throw new IllegalArgumentException("Size must be positive");
                case 15:
                    sessionParams.abiOverride = checkAbiArgument(getNextArg());
                    break;
                case 16:
                case 17:
                case 18:
                    sessionParams.setInstallAsInstantApp(true);
                    break;
                case H.REPORT_WINDOWS_CHANGE /*19*/:
                    sessionParams.setInstallAsInstantApp(false);
                    break;
                case 20:
                    sessionParams.setInstallAsVirtualPreload();
                    break;
                case BackupHandler.MSG_OP_COMPLETE /*21*/:
                    params.userId = UserHandle.parseUserArg(getNextArgRequired());
                    break;
                case H.REPORT_HARD_KEYBOARD_STATUS_CHANGE /*22*/:
                    sessionParams.installLocation = Integer.parseInt(getNextArg());
                    break;
                case H.BOOT_TIMEOUT /*23*/:
                    sessionParams.installFlags |= 512;
                    sessionParams.volumeUuid = getNextArg();
                    if ("internal".equals(sessionParams.volumeUuid)) {
                        sessionParams.volumeUuid = null;
                        break;
                    }
                    break;
                case 24:
                    sessionParams.installFlags |= 8192;
                    break;
                case H.SHOW_STRICT_MODE_VIOLATION /*25*/:
                    params.sessionParams.hdbEncode = getNextArg();
                    params.sessionParams.hdbArgIndex = getArgPos();
                    params.sessionParams.hdbArgs = getArgs();
                    break;
                default:
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Unknown option ");
                    stringBuilder.append(opt);
                    throw new IllegalArgumentException(stringBuilder.toString());
            }
            if (replaceExisting) {
                sessionParams.installFlags |= 2;
            }
        }
    }

    private int runSetHomeActivity() {
        PrintWriter pw = getOutPrintWriter();
        int userId = 0;
        while (true) {
            String nextOption = getNextOption();
            String opt = nextOption;
            if (nextOption != null) {
                int i = -1;
                if (opt.hashCode() == 1333469547 && opt.equals("--user")) {
                    i = 0;
                }
                if (i != 0) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Error: Unknown option: ");
                    stringBuilder.append(opt);
                    pw.println(stringBuilder.toString());
                    return 1;
                }
                userId = UserHandle.parseUserArg(getNextArgRequired());
            } else {
                nextOption = getNextArg();
                ComponentName componentName = nextOption != null ? ComponentName.unflattenFromString(nextOption) : null;
                if (componentName == null) {
                    pw.println("Error: component name not specified or invalid");
                    return 1;
                }
                try {
                    this.mInterface.setHomeActivity(componentName, userId);
                    pw.println("Success");
                    return 0;
                } catch (Exception e) {
                    pw.println(e.toString());
                    return 1;
                }
            }
        }
    }

    private int runSetInstaller() throws RemoteException {
        String targetPackage = getNextArg();
        String installerPackageName = getNextArg();
        if (targetPackage == null || installerPackageName == null) {
            getErrPrintWriter().println("Must provide both target and installer package names");
            return 1;
        }
        this.mInterface.setInstallerPackageName(targetPackage, installerPackageName);
        getOutPrintWriter().println("Success");
        return 0;
    }

    private int runGetInstantAppResolver() {
        PrintWriter pw = getOutPrintWriter();
        try {
            ComponentName instantAppsResolver = this.mInterface.getInstantAppResolverComponent();
            if (instantAppsResolver == null) {
                return 1;
            }
            pw.println(instantAppsResolver.flattenToString());
            return 0;
        } catch (Exception e) {
            pw.println(e.toString());
            return 1;
        }
    }

    private int runHasFeature() {
        PrintWriter err = getErrPrintWriter();
        String featureName = getNextArg();
        int i = 1;
        if (featureName == null) {
            err.println("Error: expected FEATURE name");
            return 1;
        }
        int version;
        String versionString = getNextArg();
        if (versionString == null) {
            version = 0;
        } else {
            try {
                version = Integer.parseInt(versionString);
            } catch (NumberFormatException e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Error: illegal version number ");
                stringBuilder.append(versionString);
                err.println(stringBuilder.toString());
                return 1;
            } catch (RemoteException e2) {
                err.println(e2.toString());
                return 1;
            }
        }
        boolean hasFeature = this.mInterface.hasSystemFeature(featureName, version);
        getOutPrintWriter().println(hasFeature);
        if (hasFeature) {
            i = 0;
        }
        return i;
    }

    private int runDump() {
        String pkg = getNextArg();
        if (pkg == null) {
            getErrPrintWriter().println("Error: no package specified");
            return 1;
        }
        ActivityManager.dumpPackageStateStatic(getOutFileDescriptor(), pkg);
        return 0;
    }

    private int runSetHarmfulAppWarning() throws RemoteException {
        int userId = -2;
        while (true) {
            String nextOption = getNextOption();
            String opt = nextOption;
            if (nextOption == null) {
                userId = translateUserId(userId, false, "runSetHarmfulAppWarning");
                this.mInterface.setHarmfulAppWarning(getNextArgRequired(), getNextArg(), userId);
                return 0;
            } else if (opt.equals("--user")) {
                userId = UserHandle.parseUserArg(getNextArgRequired());
            } else {
                PrintWriter errPrintWriter = getErrPrintWriter();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Error: Unknown option: ");
                stringBuilder.append(opt);
                errPrintWriter.println(stringBuilder.toString());
                return -1;
            }
        }
    }

    private int runGetHarmfulAppWarning() throws RemoteException {
        int userId = -2;
        while (true) {
            String nextOption = getNextOption();
            String opt = nextOption;
            if (nextOption == null) {
                userId = translateUserId(userId, false, "runGetHarmfulAppWarning");
                CharSequence warning = this.mInterface.getHarmfulAppWarning(getNextArgRequired(), userId);
                if (TextUtils.isEmpty(warning)) {
                    return 1;
                }
                getOutPrintWriter().println(warning);
                return 0;
            } else if (opt.equals("--user")) {
                userId = UserHandle.parseUserArg(getNextArgRequired());
            } else {
                PrintWriter errPrintWriter = getErrPrintWriter();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Error: Unknown option: ");
                stringBuilder.append(opt);
                errPrintWriter.println(stringBuilder.toString());
                return -1;
            }
        }
    }

    private static String checkAbiArgument(String abi) {
        if (TextUtils.isEmpty(abi)) {
            throw new IllegalArgumentException("Missing ABI argument");
        } else if (STDIN_PATH.equals(abi)) {
            return abi;
        } else {
            for (String supportedAbi : Build.SUPPORTED_ABIS) {
                if (supportedAbi.equals(abi)) {
                    return abi;
                }
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("ABI ");
            stringBuilder.append(abi);
            stringBuilder.append(" not supported on this device");
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    private int translateUserId(int userId, boolean allowAll, String logContext) {
        return ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, allowAll, true, logContext, "pm command");
    }

    private int doCreateSession(SessionParams params, String installerPackageName, int userId) throws RemoteException {
        userId = translateUserId(userId, true, "runInstallCreate");
        if (userId == -1) {
            userId = 0;
            params.installFlags |= 64;
        }
        return this.mInterface.getPackageInstaller().createSession(params, installerPackageName, userId);
    }

    /* JADX WARNING: Removed duplicated region for block: B:18:0x0069  */
    /* JADX WARNING: Removed duplicated region for block: B:16:0x005f  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private int doWriteSplit(int sessionId, String inPath, long sizeBytes, String splitName, boolean logSuccess) throws RemoteException {
        ParcelFileDescriptor parcelFileDescriptor;
        ParcelFileDescriptor fd;
        long sizeBytes2;
        String str = inPath;
        PrintWriter pw = getOutPrintWriter();
        if (STDIN_PATH.equals(str)) {
            parcelFileDescriptor = new ParcelFileDescriptor(getInFileDescriptor());
        } else if (str != null) {
            parcelFileDescriptor = openFileForSystem(str, "r");
            if (parcelFileDescriptor == null) {
                return -1;
            }
            long sizeBytes3 = parcelFileDescriptor.getStatSize();
            if (sizeBytes3 < 0) {
                PrintWriter errPrintWriter = getErrPrintWriter();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unable to get size of: ");
                stringBuilder.append(str);
                errPrintWriter.println(stringBuilder.toString());
                return -1;
            }
            fd = parcelFileDescriptor;
            sizeBytes2 = sizeBytes3;
            if (sizeBytes2 > 0) {
                getErrPrintWriter().println("Error: must specify a APK size");
                return 1;
            }
            Session session = null;
            try {
                session = new Session(this.mInterface.getPackageInstaller().openSession(sessionId));
                session.write(splitName, 0, sizeBytes2, fd);
                if (logSuccess) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Success: streamed ");
                    stringBuilder2.append(sizeBytes2);
                    stringBuilder2.append(" bytes");
                    pw.println(stringBuilder2.toString());
                }
                IoUtils.closeQuietly(session);
                return 0;
            } catch (IOException e) {
                PrintWriter errPrintWriter2 = getErrPrintWriter();
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Error: failed to write; ");
                stringBuilder3.append(e.getMessage());
                errPrintWriter2.println(stringBuilder3.toString());
                IoUtils.closeQuietly(session);
                return 1;
            } catch (Throwable th) {
                IoUtils.closeQuietly(session);
                throw th;
            }
        } else {
            parcelFileDescriptor = new ParcelFileDescriptor(getInFileDescriptor());
        }
        sizeBytes2 = sizeBytes;
        fd = parcelFileDescriptor;
        if (sizeBytes2 > 0) {
        }
    }

    private int doRemoveSplit(int sessionId, String splitName, boolean logSuccess) throws RemoteException {
        PrintWriter pw = getOutPrintWriter();
        try {
            Session session = new Session(this.mInterface.getPackageInstaller().openSession(sessionId));
            session.removeSplit(splitName);
            if (logSuccess) {
                pw.println("Success");
            }
            IoUtils.closeQuietly(session);
            return 0;
        } catch (IOException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Error: failed to remove split; ");
            stringBuilder.append(e.getMessage());
            pw.println(stringBuilder.toString());
            IoUtils.closeQuietly(null);
            return 1;
        } catch (Throwable th) {
            IoUtils.closeQuietly(null);
            throw th;
        }
    }

    private int doCommitSession(int sessionId, boolean logSuccess) throws RemoteException {
        PrintWriter pw = getOutPrintWriter();
        Session session = null;
        try {
            session = new Session(this.mInterface.getPackageInstaller().openSession(sessionId));
            DexMetadataHelper.validateDexPaths(session.getNames());
        } catch (IOException | IllegalStateException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Warning [Could not validate the dex paths: ");
            stringBuilder.append(e.getMessage());
            stringBuilder.append("]");
            pw.println(stringBuilder.toString());
        } catch (Throwable th) {
            IoUtils.closeQuietly(session);
        }
        LocalIntentReceiver receiver = new LocalIntentReceiver();
        session.commit(receiver.getIntentSender());
        Intent result = receiver.getResult();
        int status = result.getIntExtra("android.content.pm.extra.STATUS", 1);
        if (status != 0) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Failure [");
            stringBuilder2.append(result.getStringExtra("android.content.pm.extra.STATUS_MESSAGE"));
            stringBuilder2.append("]");
            pw.println(stringBuilder2.toString());
        } else if (logSuccess) {
            pw.println("Success");
        }
        IoUtils.closeQuietly(session);
        return status;
    }

    private int doAbandonSession(int sessionId, boolean logSuccess) throws RemoteException {
        PrintWriter pw = getOutPrintWriter();
        try {
            Session session = new Session(this.mInterface.getPackageInstaller().openSession(sessionId));
            session.abandon();
            if (logSuccess) {
                pw.println("Success");
            }
            IoUtils.closeQuietly(session);
            return 0;
        } catch (Throwable th) {
            IoUtils.closeQuietly(null);
        }
    }

    private void doListPermissions(ArrayList<String> groupList, boolean groups, boolean labels, boolean summary, int startProtectionLevel, int endProtectionLevel) throws RemoteException {
        ArrayList arrayList = groupList;
        PrintWriter pw = getOutPrintWriter();
        int groupCount = groupList.size();
        int i = 0;
        int i2 = 0;
        while (i2 < groupCount) {
            ArrayList<String> arrayList2;
            String groupName = (String) arrayList.get(i2);
            String prefix = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
            if (groups) {
                if (i2 > 0) {
                    pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                }
                if (groupName != null) {
                    PermissionGroupInfo pgi = this.mInterface.getPermissionGroupInfo(groupName, i);
                    StringBuilder stringBuilder;
                    if (!summary) {
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append(labels ? "+ " : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                        stringBuilder2.append("group:");
                        stringBuilder2.append(pgi.name);
                        pw.println(stringBuilder2.toString());
                        if (labels) {
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("  package:");
                            stringBuilder2.append(pgi.packageName);
                            pw.println(stringBuilder2.toString());
                            if (getResources(pgi) != null) {
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("  label:");
                                stringBuilder.append(loadText(pgi, pgi.labelRes, pgi.nonLocalizedLabel));
                                pw.println(stringBuilder.toString());
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("  description:");
                                stringBuilder.append(loadText(pgi, pgi.descriptionRes, pgi.nonLocalizedDescription));
                                pw.println(stringBuilder.toString());
                            }
                        }
                    } else if (getResources(pgi) != null) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append(loadText(pgi, pgi.labelRes, pgi.nonLocalizedLabel));
                        stringBuilder.append(": ");
                        pw.print(stringBuilder.toString());
                    } else {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append(pgi.name);
                        stringBuilder.append(": ");
                        pw.print(stringBuilder.toString());
                    }
                } else {
                    StringBuilder stringBuilder3 = new StringBuilder();
                    String str = (!labels || summary) ? BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS : "+ ";
                    stringBuilder3.append(str);
                    stringBuilder3.append("ungrouped:");
                    pw.println(stringBuilder3.toString());
                }
                prefix = "  ";
            }
            List<PermissionInfo> ps = this.mInterface.queryPermissionsByGroup((String) arrayList.get(i2), i).getList();
            int count = ps.size();
            boolean first = true;
            int p = i;
            while (p < count) {
                PermissionInfo pi = (PermissionInfo) ps.get(p);
                if (!groups || groupName != null || pi.group == null) {
                    i = pi.protectionLevel & 15;
                    if (i >= startProtectionLevel && i <= endProtectionLevel) {
                        Resources res;
                        if (summary) {
                            if (first) {
                                first = false;
                            } else {
                                pw.print(", ");
                            }
                            res = getResources(pi);
                            if (res != null) {
                                pw.print(loadText(pi, pi.labelRes, pi.nonLocalizedLabel));
                            } else {
                                pw.print(pi.name);
                            }
                        } else {
                            StringBuilder stringBuilder4 = new StringBuilder();
                            stringBuilder4.append(prefix);
                            stringBuilder4.append(labels ? "+ " : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                            stringBuilder4.append("permission:");
                            stringBuilder4.append(pi.name);
                            pw.println(stringBuilder4.toString());
                            if (labels) {
                                stringBuilder4 = new StringBuilder();
                                stringBuilder4.append(prefix);
                                stringBuilder4.append("  package:");
                                stringBuilder4.append(pi.packageName);
                                pw.println(stringBuilder4.toString());
                                res = getResources(pi);
                                if (res != null) {
                                    StringBuilder stringBuilder5 = new StringBuilder();
                                    stringBuilder5.append(prefix);
                                    stringBuilder5.append("  label:");
                                    stringBuilder5.append(loadText(pi, pi.labelRes, pi.nonLocalizedLabel));
                                    pw.println(stringBuilder5.toString());
                                    stringBuilder4 = new StringBuilder();
                                    stringBuilder4.append(prefix);
                                    stringBuilder4.append("  description:");
                                    stringBuilder4.append(loadText(pi, pi.descriptionRes, pi.nonLocalizedDescription));
                                    pw.println(stringBuilder4.toString());
                                }
                                stringBuilder4 = new StringBuilder();
                                stringBuilder4.append(prefix);
                                stringBuilder4.append("  protectionLevel:");
                                stringBuilder4.append(PermissionInfo.protectionToString(pi.protectionLevel));
                                pw.println(stringBuilder4.toString());
                            }
                        }
                    }
                }
                p++;
                arrayList2 = groupList;
            }
            if (summary) {
                pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            }
            i2++;
            arrayList2 = groupList;
            i = 0;
        }
    }

    private String loadText(PackageItemInfo pii, int res, CharSequence nonLocalized) throws RemoteException {
        if (nonLocalized != null) {
            return nonLocalized.toString();
        }
        if (res != 0) {
            Resources r = getResources(pii);
            if (r != null) {
                try {
                    return r.getString(res);
                } catch (NotFoundException e) {
                }
            }
        }
        return null;
    }

    private Resources getResources(PackageItemInfo pii) throws RemoteException {
        Resources res = (Resources) this.mResourceCache.get(pii.packageName);
        if (res != null) {
            return res;
        }
        ApplicationInfo ai = this.mInterface.getApplicationInfo(pii.packageName, 0, 0);
        AssetManager am = new AssetManager();
        am.addAssetPath(ai.publicSourceDir);
        res = new Resources(am, null, null);
        this.mResourceCache.put(pii.packageName, res);
        return res;
    }

    public void onHelp() {
        PrintWriter pw = getOutPrintWriter();
        pw.println("Package manager (package) commands:");
        pw.println("  help");
        pw.println("    Print this help text.");
        pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        pw.println("  path [--user USER_ID] PACKAGE");
        pw.println("    Print the path to the .apk of the given PACKAGE.");
        pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        pw.println("  dump PACKAGE");
        pw.println("    Print various system state associated with the given PACKAGE.");
        pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        pw.println("  list features");
        pw.println("    Prints all features of the system.");
        pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        pw.println("  has-feature FEATURE_NAME [version]");
        pw.println("    Prints true and returns exit status 0 when system has a FEATURE_NAME,");
        pw.println("    otherwise prints false and returns exit status 1");
        pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        pw.println("  list instrumentation [-f] [TARGET-PACKAGE]");
        pw.println("    Prints all test packages; optionally only those targeting TARGET-PACKAGE");
        pw.println("    Options:");
        pw.println("      -f: dump the name of the .apk file containing the test package");
        pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        pw.println("  list libraries");
        pw.println("    Prints all system libraries.");
        pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        pw.println("  list packages [-f] [-d] [-e] [-s] [-3] [-i] [-l] [-u] [-U] ");
        pw.println("      [--uid UID] [--user USER_ID] [FILTER]");
        pw.println("    Prints all packages; optionally only those whose name contains");
        pw.println("    the text in FILTER.  Options are:");
        pw.println("      -f: see their associated file");
        pw.println("      -d: filter to only show disabled packages");
        pw.println("      -e: filter to only show enabled packages");
        pw.println("      -s: filter to only show system packages");
        pw.println("      -3: filter to only show third party packages");
        pw.println("      -i: see the installer for the packages");
        pw.println("      -l: ignored (used for compatibility with older releases)");
        pw.println("      -U: also show the package UID");
        pw.println("      -u: also include uninstalled packages");
        pw.println("      --uid UID: filter to only show packages with the given UID");
        pw.println("      --user USER_ID: only list packages belonging to the given user");
        pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        pw.println("  list permission-groups");
        pw.println("    Prints all known permission groups.");
        pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        pw.println("  list permissions [-g] [-f] [-d] [-u] [GROUP]");
        pw.println("    Prints all known permissions; optionally only those in GROUP.  Options are:");
        pw.println("      -g: organize by group");
        pw.println("      -f: print all information");
        pw.println("      -s: short summary");
        pw.println("      -d: only list dangerous permissions");
        pw.println("      -u: list only the permissions users will see");
        pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        pw.println("  resolve-activity [--brief] [--components] [--user USER_ID] INTENT");
        pw.println("    Prints the activity that resolves to the given INTENT.");
        pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        pw.println("  query-activities [--brief] [--components] [--user USER_ID] INTENT");
        pw.println("    Prints all activities that can handle the given INTENT.");
        pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        pw.println("  query-services [--brief] [--components] [--user USER_ID] INTENT");
        pw.println("    Prints all services that can handle the given INTENT.");
        pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        pw.println("  query-receivers [--brief] [--components] [--user USER_ID] INTENT");
        pw.println("    Prints all broadcast receivers that can handle the given INTENT.");
        pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        pw.println("  install [-lrtsfdg] [-i PACKAGE] [--user USER_ID|all|current]");
        pw.println("       [-p INHERIT_PACKAGE] [--install-location 0/1/2]");
        pw.println("       [--originating-uri URI] [---referrer URI]");
        pw.println("       [--abi ABI_NAME] [--force-sdk]");
        pw.println("       [--preload] [--instantapp] [--full] [--dont-kill]");
        pw.println("       [--force-uuid internal|UUID] [--pkg PACKAGE] [-S BYTES] [PATH|-]");
        pw.println("    Install an application.  Must provide the apk data to install, either as a");
        pw.println("    file path or '-' to read from stdin.  Options are:");
        pw.println("      -l: forward lock application");
        pw.println("      -R: disallow replacement of existing application");
        pw.println("      -t: allow test packages");
        pw.println("      -i: specify package name of installer owning the app");
        pw.println("      -s: install application on sdcard");
        pw.println("      -f: install application on internal flash");
        pw.println("      -d: allow version code downgrade (debuggable packages only)");
        pw.println("      -p: partial application install (new split on top of existing pkg)");
        pw.println("      -g: grant all runtime permissions");
        pw.println("      -S: size in bytes of package, required for stdin");
        pw.println("      --user: install under the given user.");
        pw.println("      --dont-kill: installing a new feature split, don't kill running app");
        pw.println("      --originating-uri: set URI where app was downloaded from");
        pw.println("      --referrer: set URI that instigated the install of the app");
        pw.println("      --pkg: specify expected package name of app being installed");
        pw.println("      --abi: override the default ABI of the platform");
        pw.println("      --instantapp: cause the app to be installed as an ephemeral install app");
        pw.println("      --full: cause the app to be installed as a non-ephemeral full app");
        pw.println("      --install-location: force the install location:");
        pw.println("          0=auto, 1=internal only, 2=prefer external");
        pw.println("      --force-uuid: force install on to disk volume with given UUID");
        pw.println("      --force-sdk: allow install even when existing app targets platform");
        pw.println("          codename but new one targets a final API level");
        pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        pw.println("  install-create [-lrtsfdg] [-i PACKAGE] [--user USER_ID|all|current]");
        pw.println("       [-p INHERIT_PACKAGE] [--install-location 0/1/2]");
        pw.println("       [--originating-uri URI] [---referrer URI]");
        pw.println("       [--abi ABI_NAME] [--force-sdk]");
        pw.println("       [--preload] [--instantapp] [--full] [--dont-kill]");
        pw.println("       [--force-uuid internal|UUID] [--pkg PACKAGE] [-S BYTES]");
        pw.println("    Like \"install\", but starts an install session.  Use \"install-write\"");
        pw.println("    to push data into the session, and \"install-commit\" to finish.");
        pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        pw.println("  install-write [-S BYTES] SESSION_ID SPLIT_NAME [PATH|-]");
        pw.println("    Write an apk into the given install session.  If the path is '-', data");
        pw.println("    will be read from stdin.  Options are:");
        pw.println("      -S: size in bytes of package, required for stdin");
        pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        pw.println("  install-commit SESSION_ID");
        pw.println("    Commit the given active install session, installing the app.");
        pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        pw.println("  install-abandon SESSION_ID");
        pw.println("    Delete the given active install session.");
        pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        pw.println("  set-install-location LOCATION");
        pw.println("    Changes the default install location.  NOTE this is only intended for debugging;");
        pw.println("    using this can cause applications to break and other undersireable behavior.");
        pw.println("    LOCATION is one of:");
        pw.println("    0 [auto]: Let system decide the best location");
        pw.println("    1 [internal]: Install on internal device storage");
        pw.println("    2 [external]: Install on external media");
        pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        pw.println("  get-install-location");
        pw.println("    Returns the current install location: 0, 1 or 2 as per set-install-location.");
        pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        pw.println("  move-package PACKAGE [internal|UUID]");
        pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        pw.println("  move-primary-storage [internal|UUID]");
        pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        pw.println("  pm uninstall [-k] [--user USER_ID] [--versionCode VERSION_CODE] PACKAGE [SPLIT]");
        pw.println("    Remove the given package name from the system.  May remove an entire app");
        pw.println("    if no SPLIT name is specified, otherwise will remove only the split of the");
        pw.println("    given app.  Options are:");
        pw.println("      -k: keep the data and cache directories around after package removal.");
        pw.println("      --user: remove the app from the given user.");
        pw.println("      --versionCode: only uninstall if the app has the given version code.");
        pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        pw.println("  clear [--user USER_ID] PACKAGE");
        pw.println("    Deletes all data associated with a package.");
        pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        pw.println("  enable [--user USER_ID] PACKAGE_OR_COMPONENT");
        pw.println("  disable [--user USER_ID] PACKAGE_OR_COMPONENT");
        pw.println("  disable-user [--user USER_ID] PACKAGE_OR_COMPONENT");
        pw.println("  disable-until-used [--user USER_ID] PACKAGE_OR_COMPONENT");
        pw.println("  default-state [--user USER_ID] PACKAGE_OR_COMPONENT");
        pw.println("    These commands change the enabled state of a given package or");
        pw.println("    component (written as \"package/class\").");
        pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        pw.println("  hide [--user USER_ID] PACKAGE_OR_COMPONENT");
        pw.println("  unhide [--user USER_ID] PACKAGE_OR_COMPONENT");
        pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        pw.println("  suspend [--user USER_ID] TARGET-PACKAGE");
        pw.println("    Suspends the specified package (as user).");
        pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        pw.println("  unsuspend [--user USER_ID] TARGET-PACKAGE");
        pw.println("    Unsuspends the specified package (as user).");
        pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        pw.println("  grant [--user USER_ID] PACKAGE PERMISSION");
        pw.println("  revoke [--user USER_ID] PACKAGE PERMISSION");
        pw.println("    These commands either grant or revoke permissions to apps.  The permissions");
        pw.println("    must be declared as used in the app's manifest, be runtime permissions");
        pw.println("    (protection level dangerous), and the app targeting SDK greater than Lollipop MR1.");
        pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        pw.println("  reset-permissions");
        pw.println("    Revert all runtime permissions to their default state.");
        pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        pw.println("  set-permission-enforced PERMISSION [true|false]");
        pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        pw.println("  get-privapp-permissions TARGET-PACKAGE");
        pw.println("    Prints all privileged permissions for a package.");
        pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        pw.println("  get-privapp-deny-permissions TARGET-PACKAGE");
        pw.println("    Prints all privileged permissions that are denied for a package.");
        pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        pw.println("  get-oem-permissions TARGET-PACKAGE");
        pw.println("    Prints all OEM permissions for a package.");
        pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        pw.println("  set-app-link [--user USER_ID] PACKAGE {always|ask|never|undefined}");
        pw.println("  get-app-link [--user USER_ID] PACKAGE");
        pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        pw.println("  trim-caches DESIRED_FREE_SPACE [internal|UUID]");
        pw.println("    Trim cache files to reach the given free space.");
        pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        pw.println("  create-user [--profileOf USER_ID] [--managed] [--restricted] [--ephemeral]");
        pw.println("      [--guest] USER_NAME");
        pw.println("    Create a new user with the given USER_NAME, printing the new user identifier");
        pw.println("    of the user.");
        pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        pw.println("  remove-user USER_ID");
        pw.println("    Remove the user with the given USER_IDENTIFIER, deleting all data");
        pw.println("    associated with that user");
        pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        pw.println("  set-user-restriction [--user USER_ID] RESTRICTION VALUE");
        pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        pw.println("  get-max-users");
        pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        pw.println("  get-max-running-users");
        pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        pw.println("  compile [-m MODE | -r REASON] [-f] [-c] [--split SPLIT_NAME]");
        pw.println("          [--reset] [--check-prof (true | false)] (-a | TARGET-PACKAGE)");
        pw.println("    Trigger compilation of TARGET-PACKAGE or all packages if \"-a\".  Options are:");
        pw.println("      -a: compile all packages");
        pw.println("      -c: clear profile data before compiling");
        pw.println("      -f: force compilation even if not needed");
        pw.println("      -m: select compilation mode");
        pw.println("          MODE is one of the dex2oat compiler filters:");
        pw.println("            assume-verified");
        pw.println("            extract");
        pw.println("            verify");
        pw.println("            quicken");
        pw.println("            space-profile");
        pw.println("            space");
        pw.println("            speed-profile");
        pw.println("            speed");
        pw.println("            everything");
        pw.println("      -r: select compilation reason");
        pw.println("          REASON is one of:");
        for (String append : PackageManagerServiceCompilerMapping.REASON_STRINGS) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("            ");
            stringBuilder.append(append);
            pw.println(stringBuilder.toString());
        }
        pw.println("      --reset: restore package to its post-install state");
        pw.println("      --check-prof (true | false): look at profiles when doing dexopt?");
        pw.println("      --secondary-dex: compile app secondary dex files");
        pw.println("      --split SPLIT: compile only the given split name");
        pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        pw.println("  force-dex-opt PACKAGE");
        pw.println("    Force immediate execution of dex opt for the given PACKAGE.");
        pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        pw.println("  bg-dexopt-job");
        pw.println("    Execute the background optimizations immediately.");
        pw.println("    Note that the command only runs the background optimizer logic. It may");
        pw.println("    overlap with the actual job but the job scheduler will not be able to");
        pw.println("    cancel it. It will also run even if the device is not in the idle");
        pw.println("    maintenance mode.");
        pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        pw.println("  reconcile-secondary-dex-files TARGET-PACKAGE");
        pw.println("    Reconciles the package secondary dex files with the generated oat files.");
        pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        pw.println("  dump-profiles TARGET-PACKAGE");
        pw.println("    Dumps method/class profile files to");
        pw.println("    /data/misc/profman/TARGET-PACKAGE.txt");
        pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        pw.println("  snapshot-profile TARGET-PACKAGE [--code-path path]");
        pw.println("    Take a snapshot of the package profiles to");
        pw.println("    /data/misc/profman/TARGET-PACKAGE[-code-path].prof");
        pw.println("    If TARGET-PACKAGE=android it will take a snapshot of the boot image");
        pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        pw.println("  set-home-activity [--user USER_ID] TARGET-COMPONENT");
        pw.println("    Set the default home activity (aka launcher).");
        pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        pw.println("  set-installer PACKAGE INSTALLER");
        pw.println("    Set installer package name");
        pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        pw.println("  get-instantapp-resolver");
        pw.println("    Return the name of the component that is the current instant app installer.");
        pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        pw.println("  set-harmful-app-warning [--user <USER_ID>] <PACKAGE> [<WARNING>]");
        pw.println("    Mark the app as harmful with the given warning message.");
        pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        pw.println("  get-harmful-app-warning [--user <USER_ID>] <PACKAGE>");
        pw.println("    Return the harmful app warning message for the given app, if present");
        pw.println();
        pw.println("  uninstall-system-updates");
        pw.println("    Remove updates to all system applications and fall back to their /system version.");
        pw.println();
        Intent.printIntentArgsHelp(pw, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
    }
}
