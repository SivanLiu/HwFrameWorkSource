package com.android.server.pm;

import android.content.Context;
import android.content.pm.IOtaDexopt.Stub;
import android.content.pm.PackageParser.Package;
import android.os.Environment;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.ShellCallback;
import android.os.storage.StorageManager;
import android.util.Log;
import android.util.Slog;
import com.android.internal.logging.MetricsLogger;
import com.android.server.os.HwBootFail;
import com.android.server.pm.Installer.InstallerException;
import com.android.server.pm.PackageDexOptimizer.ForcedUpdatePackageDexOptimizer;
import com.android.server.pm.dex.DexoptOptions;
import com.android.server.slice.SliceClientPermissions.SliceAuthority;
import java.io.File;
import java.io.FileDescriptor;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class OtaDexoptService extends Stub {
    private static final long BULK_DELETE_THRESHOLD = 1073741824;
    private static final boolean DEBUG_DEXOPT = true;
    private static final String[] NO_LIBRARIES = new String[]{PackageDexOptimizer.SKIP_SHARED_LIBRARY_CHECK};
    private static final String TAG = "OTADexopt";
    private long availableSpaceAfterBulkDelete;
    private long availableSpaceAfterDexopt;
    private long availableSpaceBefore;
    private int completeSize;
    private int dexoptCommandCountExecuted;
    private int dexoptCommandCountTotal;
    private int importantPackageCount;
    private final Context mContext;
    private List<String> mDexoptCommands;
    private final PackageManagerService mPackageManagerService;
    private long otaDexoptTimeStart;
    private int otherPackageCount;

    private static class OTADexoptPackageDexOptimizer extends ForcedUpdatePackageDexOptimizer {
        public OTADexoptPackageDexOptimizer(Installer installer, Object installLock, Context context) {
            super(installer, installLock, context, "*otadexopt*");
        }
    }

    public OtaDexoptService(Context context, PackageManagerService packageManagerService) {
        this.mContext = context;
        this.mPackageManagerService = packageManagerService;
    }

    public static OtaDexoptService main(Context context, PackageManagerService packageManagerService) {
        OtaDexoptService ota = new OtaDexoptService(context, packageManagerService);
        ServiceManager.addService("otadexopt", ota);
        ota.moveAbArtifacts(packageManagerService.mInstaller);
        return ota;
    }

    public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
        new OtaDexoptShellCommand(this).exec(this, in, out, err, args, callback, resultReceiver);
    }

    public synchronized void prepare() throws RemoteException {
        if (this.mDexoptCommands == null) {
            List<Package> important;
            List<Package> others;
            synchronized (this.mPackageManagerService.mPackages) {
                important = PackageManagerServiceUtils.getPackagesForDexopt(this.mPackageManagerService.mPackages.values(), this.mPackageManagerService);
                others = new ArrayList(this.mPackageManagerService.mPackages.values());
                others.removeAll(important);
                this.mDexoptCommands = new ArrayList((3 * this.mPackageManagerService.mPackages.size()) / 2);
            }
            for (Package p : important) {
                this.mDexoptCommands.addAll(generatePackageDexopts(p, 4));
            }
            for (Package p2 : others) {
                if (p2.coreApp) {
                    throw new IllegalStateException("Found a core app that's not important");
                }
                this.mDexoptCommands.addAll(generatePackageDexopts(p2, 0));
            }
            this.completeSize = this.mDexoptCommands.size();
            long spaceAvailable = getAvailableSpace();
            if (spaceAvailable < BULK_DELETE_THRESHOLD) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Low on space, deleting oat files in an attempt to free up space: ");
                stringBuilder.append(PackageManagerServiceUtils.packagesToString(others));
                Log.i(str, stringBuilder.toString());
                for (Package pkg : others) {
                    this.mPackageManagerService.deleteOatArtifactsOfPackage(pkg.packageName);
                }
            }
            prepareMetricsLogging(important.size(), others.size(), spaceAvailable, getAvailableSpace());
        } else {
            throw new IllegalStateException("already called prepare()");
        }
    }

    public synchronized void cleanup() throws RemoteException {
        Log.i(TAG, "Cleaning up OTA Dexopt state.");
        this.mDexoptCommands = null;
        this.availableSpaceAfterDexopt = getAvailableSpace();
        performMetricsLogging();
    }

    public synchronized boolean isDone() throws RemoteException {
        if (this.mDexoptCommands != null) {
        } else {
            throw new IllegalStateException("done() called before prepare()");
        }
        return this.mDexoptCommands.isEmpty();
    }

    public synchronized float getProgress() throws RemoteException {
        if (this.completeSize == 0) {
            return 1.0f;
        }
        return ((float) (this.completeSize - this.mDexoptCommands.size())) / ((float) this.completeSize);
    }

    public synchronized String nextDexoptCommand() throws RemoteException {
        if (this.mDexoptCommands == null) {
            throw new IllegalStateException("dexoptNextPackage() called before prepare()");
        } else if (this.mDexoptCommands.isEmpty()) {
            return "(all done)";
        } else {
            String next = (String) this.mDexoptCommands.remove(0);
            String str;
            StringBuilder stringBuilder;
            if (getAvailableSpace() > 0) {
                this.dexoptCommandCountExecuted++;
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Next command: ");
                stringBuilder.append(next);
                Log.d(str, stringBuilder.toString());
                return next;
            }
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Not enough space for OTA dexopt, stopping with ");
            stringBuilder.append(this.mDexoptCommands.size() + 1);
            stringBuilder.append(" commands left.");
            Log.w(str, stringBuilder.toString());
            this.mDexoptCommands.clear();
            return "(no free space)";
        }
    }

    private long getMainLowSpaceThreshold() {
        long lowThreshold = StorageManager.from(this.mContext).getStorageLowBytes(Environment.getDataDirectory());
        if (lowThreshold != 0) {
            return lowThreshold;
        }
        throw new IllegalStateException("Invalid low memory threshold");
    }

    private long getAvailableSpace() {
        return Environment.getDataDirectory().getUsableSpace() - getMainLowSpaceThreshold();
    }

    private synchronized List<String> generatePackageDexopts(Package pkg, int compilationReason) {
        final List<String> commands;
        commands = new ArrayList();
        PackageDexOptimizer optimizer = new OTADexoptPackageDexOptimizer(new Installer(this.mContext, true) {
            public void dexopt(String apkPath, int uid, String pkgName, String instructionSet, int dexoptNeeded, String outputPath, int dexFlags, String compilerFilter, String volumeUuid, String sharedLibraries, String seInfo, boolean downgrade, int targetSdkVersion, String profileName, String dexMetadataPath, String dexoptCompilationReason) throws InstallerException {
                StringBuilder builder = new StringBuilder();
                builder.append("9 ");
                builder.append("dexopt");
                encodeParameter(builder, apkPath);
                encodeParameter(builder, Integer.valueOf(uid));
                encodeParameter(builder, pkgName);
                encodeParameter(builder, instructionSet);
                encodeParameter(builder, Integer.valueOf(dexoptNeeded));
                encodeParameter(builder, outputPath);
                encodeParameter(builder, Integer.valueOf(dexFlags));
                encodeParameter(builder, compilerFilter);
                encodeParameter(builder, volumeUuid);
                encodeParameter(builder, sharedLibraries);
                encodeParameter(builder, seInfo);
                encodeParameter(builder, Boolean.valueOf(downgrade));
                encodeParameter(builder, Integer.valueOf(targetSdkVersion));
                encodeParameter(builder, profileName);
                encodeParameter(builder, dexMetadataPath);
                encodeParameter(builder, dexoptCompilationReason);
                commands.add(builder.toString());
            }

            private void encodeParameter(StringBuilder builder, Object arg) {
                builder.append(' ');
                if (arg == null) {
                    builder.append('!');
                    return;
                }
                String txt = String.valueOf(arg);
                if (txt.indexOf(0) == -1 && txt.indexOf(32) == -1 && !"!".equals(txt)) {
                    builder.append(txt);
                    return;
                }
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Invalid argument while executing ");
                stringBuilder.append(arg);
                throw new IllegalArgumentException(stringBuilder.toString());
            }
        }, this.mPackageManagerService.mInstallLock, this.mContext);
        String[] libraryDependencies = pkg.usesLibraryFiles;
        if (pkg.isSystem()) {
            libraryDependencies = NO_LIBRARIES;
        }
        optimizer.performDexOpt(pkg, libraryDependencies, null, null, this.mPackageManagerService.getDexManager().getPackageUseInfoOrDefault(pkg.packageName), new DexoptOptions(pkg.packageName, compilationReason, 4));
        return commands;
    }

    public synchronized void dexoptNextPackage() throws RemoteException {
        throw new UnsupportedOperationException();
    }

    private void moveAbArtifacts(Installer installer) {
        Installer installer2;
        if (this.mDexoptCommands != null) {
            installer2 = installer;
            throw new IllegalStateException("Should not be ota-dexopting when trying to move.");
        } else if (this.mPackageManagerService.isUpgrade()) {
            int packagePaths = 0;
            int pathsSuccessful = 0;
            for (Package pkg : this.mPackageManagerService.getPackages()) {
                if (pkg != null && PackageDexOptimizer.canOptimizePackage(pkg)) {
                    if (pkg.codePath == null) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Package ");
                        stringBuilder.append(pkg);
                        stringBuilder.append(" can be optimized but has null codePath");
                        Slog.w(str, stringBuilder.toString());
                    } else if (!(pkg.codePath.startsWith("/system") || pkg.codePath.startsWith("/vendor") || pkg.codePath.startsWith("/product"))) {
                        String[] instructionSets = InstructionSets.getAppDexInstructionSets(pkg.applicationInfo);
                        List<String> paths = pkg.getAllCodePathsExcludingResourceOnly();
                        for (String dexCodeInstructionSet : InstructionSets.getDexCodeInstructionSets(instructionSets)) {
                            for (String path : paths) {
                                int packagePaths2 = packagePaths + 1;
                                try {
                                    installer.moveAb(path, dexCodeInstructionSet, PackageDexOptimizer.getOatDir(new File(pkg.codePath)).getAbsolutePath());
                                    pathsSuccessful++;
                                } catch (InstallerException e) {
                                }
                                packagePaths = packagePaths2;
                            }
                            installer2 = installer;
                        }
                    }
                }
                installer2 = installer;
            }
            installer2 = installer;
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Moved ");
            stringBuilder2.append(pathsSuccessful);
            stringBuilder2.append(SliceAuthority.DELIMITER);
            stringBuilder2.append(packagePaths);
            Slog.i(str2, stringBuilder2.toString());
        } else {
            Slog.d(TAG, "No upgrade, skipping A/B artifacts check.");
        }
    }

    private void prepareMetricsLogging(int important, int others, long spaceBegin, long spaceBulk) {
        this.availableSpaceBefore = spaceBegin;
        this.availableSpaceAfterBulkDelete = spaceBulk;
        this.availableSpaceAfterDexopt = 0;
        this.importantPackageCount = important;
        this.otherPackageCount = others;
        this.dexoptCommandCountTotal = this.mDexoptCommands.size();
        this.dexoptCommandCountExecuted = 0;
        this.otaDexoptTimeStart = System.nanoTime();
    }

    private static int inMegabytes(long value) {
        long in_mega_bytes = value / 1048576;
        if (in_mega_bytes <= 2147483647L) {
            return (int) in_mega_bytes;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Recording ");
        stringBuilder.append(in_mega_bytes);
        stringBuilder.append("MB of free space, overflowing range");
        Log.w(str, stringBuilder.toString());
        return HwBootFail.STAGE_BOOT_SUCCESS;
    }

    private void performMetricsLogging() {
        long finalTime = System.nanoTime();
        MetricsLogger.histogram(this.mContext, "ota_dexopt_available_space_before_mb", inMegabytes(this.availableSpaceBefore));
        MetricsLogger.histogram(this.mContext, "ota_dexopt_available_space_after_bulk_delete_mb", inMegabytes(this.availableSpaceAfterBulkDelete));
        MetricsLogger.histogram(this.mContext, "ota_dexopt_available_space_after_dexopt_mb", inMegabytes(this.availableSpaceAfterDexopt));
        MetricsLogger.histogram(this.mContext, "ota_dexopt_num_important_packages", this.importantPackageCount);
        MetricsLogger.histogram(this.mContext, "ota_dexopt_num_other_packages", this.otherPackageCount);
        MetricsLogger.histogram(this.mContext, "ota_dexopt_num_commands", this.dexoptCommandCountTotal);
        MetricsLogger.histogram(this.mContext, "ota_dexopt_num_commands_executed", this.dexoptCommandCountExecuted);
        MetricsLogger.histogram(this.mContext, "ota_dexopt_time_s", (int) TimeUnit.NANOSECONDS.toSeconds(finalTime - this.otaDexoptTimeStart));
    }
}
