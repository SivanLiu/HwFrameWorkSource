package com.android.timezone.distro.installer;

import android.util.Slog;
import com.android.timezone.distro.DistroException;
import com.android.timezone.distro.DistroVersion;
import com.android.timezone.distro.FileUtils;
import com.android.timezone.distro.StagedDistroOperation;
import com.android.timezone.distro.TimeZoneDistro;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import libcore.util.TimeZoneFinder;
import libcore.util.ZoneInfoDB.TzData;

public class TimeZoneDistroInstaller {
    private static final String CURRENT_TZ_DATA_DIR_NAME = "current";
    public static final int INSTALL_FAIL_BAD_DISTRO_FORMAT_VERSION = 2;
    public static final int INSTALL_FAIL_BAD_DISTRO_STRUCTURE = 1;
    public static final int INSTALL_FAIL_RULES_TOO_OLD = 3;
    public static final int INSTALL_FAIL_VALIDATION_ERROR = 4;
    public static final int INSTALL_SUCCESS = 0;
    private static final String OLD_TZ_DATA_DIR_NAME = "old";
    private static final String STAGED_TZ_DATA_DIR_NAME = "staged";
    public static final int UNINSTALL_FAIL = 2;
    public static final int UNINSTALL_NOTHING_INSTALLED = 1;
    public static final int UNINSTALL_SUCCESS = 0;
    public static final String UNINSTALL_TOMBSTONE_FILE_NAME = "STAGED_UNINSTALL_TOMBSTONE";
    private static final String WORKING_DIR_NAME = "working";
    private final File currentTzDataDir;
    private final String logTag;
    private final File oldStagedDataDir;
    private final File stagedTzDataDir;
    private final File systemTzDataFile;
    private final File workingDir;

    @Retention(RetentionPolicy.SOURCE)
    private @interface InstallResultType {
    }

    @Retention(RetentionPolicy.SOURCE)
    private @interface UninstallResultType {
    }

    public TimeZoneDistroInstaller(String logTag, File systemTzDataFile, File installDir) {
        this.logTag = logTag;
        this.systemTzDataFile = systemTzDataFile;
        this.oldStagedDataDir = new File(installDir, OLD_TZ_DATA_DIR_NAME);
        this.stagedTzDataDir = new File(installDir, STAGED_TZ_DATA_DIR_NAME);
        this.currentTzDataDir = new File(installDir, CURRENT_TZ_DATA_DIR_NAME);
        this.workingDir = new File(installDir, WORKING_DIR_NAME);
    }

    File getOldStagedDataDir() {
        return this.oldStagedDataDir;
    }

    File getStagedTzDataDir() {
        return this.stagedTzDataDir;
    }

    File getCurrentTzDataDir() {
        return this.currentTzDataDir;
    }

    File getWorkingDir() {
        return this.workingDir;
    }

    public int stageInstallWithErrorCode(TimeZoneDistro distro) throws IOException {
        if (this.oldStagedDataDir.exists()) {
            FileUtils.deleteRecursive(this.oldStagedDataDir);
        }
        if (this.workingDir.exists()) {
            FileUtils.deleteRecursive(this.workingDir);
        }
        Slog.i(this.logTag, "Unpacking / verifying time zone update");
        TzData tzData;
        File tzLookupFile;
        try {
            unpackDistro(distro, this.workingDir);
            try {
                DistroVersion distroVersion = readDistroVersion(this.workingDir);
                String str;
                if (distroVersion == null) {
                    Slog.i(this.logTag, "Update not applied: Distro version could not be loaded");
                    deleteBestEffort(this.oldStagedDataDir);
                    deleteBestEffort(this.workingDir);
                    return 1;
                } else if (!DistroVersion.isCompatibleWithThisDevice(distroVersion)) {
                    str = this.logTag;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Update not applied: Distro format version check failed: ");
                    stringBuilder.append(distroVersion);
                    Slog.i(str, stringBuilder.toString());
                    deleteBestEffort(this.oldStagedDataDir);
                    deleteBestEffort(this.workingDir);
                    return 2;
                } else if (!checkDistroDataFilesExist(this.workingDir)) {
                    Slog.i(this.logTag, "Update not applied: Distro is missing required data file(s)");
                    deleteBestEffort(this.oldStagedDataDir);
                    deleteBestEffort(this.workingDir);
                    return 1;
                } else if (checkDistroRulesNewerThanSystem(this.systemTzDataFile, distroVersion)) {
                    File zoneInfoFile = new File(this.workingDir, TimeZoneDistro.TZDATA_FILE_NAME);
                    tzData = TzData.loadTzData(zoneInfoFile.getPath());
                    if (tzData == null) {
                        str = this.logTag;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Update not applied: ");
                        stringBuilder2.append(zoneInfoFile);
                        stringBuilder2.append(" could not be loaded");
                        Slog.i(str, stringBuilder2.toString());
                        deleteBestEffort(this.oldStagedDataDir);
                        deleteBestEffort(this.workingDir);
                        return 4;
                    }
                    StringBuilder stringBuilder3;
                    try {
                        tzData.validate();
                        tzData.close();
                        tzLookupFile = new File(this.workingDir, TimeZoneDistro.TZLOOKUP_FILE_NAME);
                        if (tzLookupFile.exists()) {
                            StringBuilder stringBuilder4;
                            TimeZoneFinder.createInstance(tzLookupFile.getPath()).validate();
                            Slog.i(this.logTag, "Applying time zone update");
                            FileUtils.makeDirectoryWorldAccessible(this.workingDir);
                            if (this.stagedTzDataDir.exists()) {
                                str = this.logTag;
                                stringBuilder4 = new StringBuilder();
                                stringBuilder4.append("Moving ");
                                stringBuilder4.append(this.stagedTzDataDir);
                                stringBuilder4.append(" to ");
                                stringBuilder4.append(this.oldStagedDataDir);
                                Slog.i(str, stringBuilder4.toString());
                                FileUtils.rename(this.stagedTzDataDir, this.oldStagedDataDir);
                            } else {
                                str = this.logTag;
                                stringBuilder4 = new StringBuilder();
                                stringBuilder4.append("Nothing to unstage at ");
                                stringBuilder4.append(this.stagedTzDataDir);
                                Slog.i(str, stringBuilder4.toString());
                            }
                            str = this.logTag;
                            stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("Moving ");
                            stringBuilder4.append(this.workingDir);
                            stringBuilder4.append(" to ");
                            stringBuilder4.append(this.stagedTzDataDir);
                            Slog.i(str, stringBuilder4.toString());
                            FileUtils.rename(this.workingDir, this.stagedTzDataDir);
                            str = this.logTag;
                            stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("Install staged: ");
                            stringBuilder4.append(this.stagedTzDataDir);
                            stringBuilder4.append(" successfully created");
                            Slog.i(str, stringBuilder4.toString());
                            deleteBestEffort(this.oldStagedDataDir);
                            deleteBestEffort(this.workingDir);
                            return 0;
                        }
                        String str2 = this.logTag;
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("Update not applied: ");
                        stringBuilder3.append(tzLookupFile);
                        stringBuilder3.append(" does not exist");
                        Slog.i(str2, stringBuilder3.toString());
                        deleteBestEffort(this.oldStagedDataDir);
                        deleteBestEffort(this.workingDir);
                        return 1;
                    } catch (IOException e) {
                        String str3 = this.logTag;
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("Update not applied: ");
                        stringBuilder3.append(zoneInfoFile);
                        stringBuilder3.append(" failed validation");
                        Slog.i(str3, stringBuilder3.toString(), e);
                        tzData.close();
                        deleteBestEffort(this.oldStagedDataDir);
                        deleteBestEffort(this.workingDir);
                        return 4;
                    }
                } else {
                    Slog.i(this.logTag, "Update not applied: Distro rules version check failed");
                    deleteBestEffort(this.oldStagedDataDir);
                    deleteBestEffort(this.workingDir);
                    return 3;
                }
            } catch (DistroException e2) {
                String str4 = this.logTag;
                StringBuilder stringBuilder5 = new StringBuilder();
                stringBuilder5.append("Invalid distro version: ");
                stringBuilder5.append(e2.getMessage());
                Slog.i(str4, stringBuilder5.toString());
                deleteBestEffort(this.oldStagedDataDir);
                deleteBestEffort(this.workingDir);
                return 1;
            }
        } catch (IOException e3) {
            String str5 = this.logTag;
            StringBuilder stringBuilder6 = new StringBuilder();
            stringBuilder6.append("Update not applied: ");
            stringBuilder6.append(tzLookupFile);
            stringBuilder6.append(" failed validation");
            Slog.i(str5, stringBuilder6.toString(), e3);
            deleteBestEffort(this.oldStagedDataDir);
            deleteBestEffort(this.workingDir);
            return 4;
        } catch (Throwable th) {
            deleteBestEffort(this.oldStagedDataDir);
            deleteBestEffort(this.workingDir);
        }
    }

    public int stageUninstall() throws IOException {
        Slog.i(this.logTag, "Uninstalling time zone update");
        if (this.oldStagedDataDir.exists()) {
            FileUtils.deleteRecursive(this.oldStagedDataDir);
        }
        if (this.workingDir.exists()) {
            FileUtils.deleteRecursive(this.workingDir);
        }
        int i;
        try {
            String str;
            StringBuilder stringBuilder;
            if (this.stagedTzDataDir.exists()) {
                str = this.logTag;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Moving ");
                stringBuilder.append(this.stagedTzDataDir);
                stringBuilder.append(" to ");
                stringBuilder.append(this.oldStagedDataDir);
                Slog.i(str, stringBuilder.toString());
                FileUtils.rename(this.stagedTzDataDir, this.oldStagedDataDir);
            } else {
                str = this.logTag;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Nothing to unstage at ");
                stringBuilder.append(this.stagedTzDataDir);
                Slog.i(str, stringBuilder.toString());
            }
            i = 1;
            if (this.currentTzDataDir.exists()) {
                FileUtils.ensureDirectoriesExist(this.workingDir, true);
                FileUtils.createEmptyFile(new File(this.workingDir, UNINSTALL_TOMBSTONE_FILE_NAME));
                str = this.logTag;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Moving ");
                stringBuilder.append(this.workingDir);
                stringBuilder.append(" to ");
                stringBuilder.append(this.stagedTzDataDir);
                Slog.i(str, stringBuilder.toString());
                FileUtils.rename(this.workingDir, this.stagedTzDataDir);
                str = this.logTag;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Uninstall staged: ");
                stringBuilder.append(this.stagedTzDataDir);
                stringBuilder.append(" successfully created");
                Slog.i(str, stringBuilder.toString());
                deleteBestEffort(this.oldStagedDataDir);
                deleteBestEffort(this.workingDir);
                return 0;
            }
            str = this.logTag;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Nothing to uninstall at ");
            stringBuilder2.append(this.currentTzDataDir);
            Slog.i(str, stringBuilder2.toString());
            return i;
        } finally {
            deleteBestEffort(this.oldStagedDataDir);
            i = this.workingDir;
            deleteBestEffort(i);
        }
    }

    public DistroVersion getInstalledDistroVersion() throws DistroException, IOException {
        if (this.currentTzDataDir.exists()) {
            return readDistroVersion(this.currentTzDataDir);
        }
        return null;
    }

    public StagedDistroOperation getStagedDistroOperation() throws DistroException, IOException {
        if (!this.stagedTzDataDir.exists()) {
            return null;
        }
        if (new File(this.stagedTzDataDir, UNINSTALL_TOMBSTONE_FILE_NAME).exists()) {
            return StagedDistroOperation.uninstall();
        }
        return StagedDistroOperation.install(readDistroVersion(this.stagedTzDataDir));
    }

    public String getSystemRulesVersion() throws IOException {
        return readSystemRulesVersion(this.systemTzDataFile);
    }

    private void deleteBestEffort(File dir) {
        if (dir.exists()) {
            String str = this.logTag;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Deleting ");
            stringBuilder.append(dir);
            Slog.i(str, stringBuilder.toString());
            try {
                FileUtils.deleteRecursive(dir);
            } catch (IOException e) {
                String str2 = this.logTag;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Unable to delete ");
                stringBuilder2.append(dir);
                Slog.w(str2, stringBuilder2.toString(), e);
            }
        }
    }

    private void unpackDistro(TimeZoneDistro distro, File targetDir) throws IOException {
        String str = this.logTag;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Unpacking update content to: ");
        stringBuilder.append(targetDir);
        Slog.i(str, stringBuilder.toString());
        distro.extractTo(targetDir);
    }

    private boolean checkDistroDataFilesExist(File unpackedContentDir) throws IOException {
        Slog.i(this.logTag, "Verifying distro contents");
        return FileUtils.filesExist(unpackedContentDir, TimeZoneDistro.TZDATA_FILE_NAME, TimeZoneDistro.ICU_DATA_FILE_NAME);
    }

    private DistroVersion readDistroVersion(File distroDir) throws DistroException, IOException {
        String str = this.logTag;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Reading distro format version: ");
        stringBuilder.append(distroDir);
        Slog.d(str, stringBuilder.toString());
        File distroVersionFile = new File(distroDir, TimeZoneDistro.DISTRO_VERSION_FILE_NAME);
        if (distroVersionFile.exists()) {
            return DistroVersion.fromBytes(FileUtils.readBytes(distroVersionFile, DistroVersion.DISTRO_VERSION_FILE_LENGTH));
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("No distro version file found: ");
        stringBuilder2.append(distroVersionFile);
        throw new DistroException(stringBuilder2.toString());
    }

    private boolean checkDistroRulesNewerThanSystem(File systemTzDataFile, DistroVersion distroVersion) throws IOException {
        Slog.i(this.logTag, "Reading /system rules version");
        String systemRulesVersion = readSystemRulesVersion(systemTzDataFile);
        String distroRulesVersion = distroVersion.rulesVersion;
        boolean canApply = distroRulesVersion.compareTo(systemRulesVersion) >= 0;
        String str;
        StringBuilder stringBuilder;
        if (canApply) {
            str = this.logTag;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Passed rules version check: distroRulesVersion=");
            stringBuilder.append(distroRulesVersion);
            stringBuilder.append(", systemRulesVersion=");
            stringBuilder.append(systemRulesVersion);
            Slog.i(str, stringBuilder.toString());
        } else {
            str = this.logTag;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Failed rules version check: distroRulesVersion=");
            stringBuilder.append(distroRulesVersion);
            stringBuilder.append(", systemRulesVersion=");
            stringBuilder.append(systemRulesVersion);
            Slog.i(str, stringBuilder.toString());
        }
        return canApply;
    }

    private String readSystemRulesVersion(File systemTzDataFile) throws IOException {
        if (systemTzDataFile.exists()) {
            return TzData.getRulesVersion(systemTzDataFile);
        }
        Slog.i(this.logTag, "tzdata file cannot be found in /system");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("system tzdata does not exist: ");
        stringBuilder.append(systemTzDataFile);
        throw new FileNotFoundException(stringBuilder.toString());
    }
}
