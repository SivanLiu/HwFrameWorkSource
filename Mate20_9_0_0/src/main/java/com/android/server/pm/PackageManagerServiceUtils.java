package com.android.server.pm;

import android.app.AppGlobals;
import android.content.Intent;
import android.content.pm.PackageParser;
import android.content.pm.PackageParser.Package;
import android.content.pm.PackageParser.PackageParserException;
import android.content.pm.PackageParser.SigningDetails;
import android.content.pm.PackageParser.SigningDetails.CertCapabilities;
import android.content.pm.ResolveInfo;
import android.content.pm.Signature;
import android.hardware.biometrics.fingerprint.V2_1.RequestStatus;
import android.os.Build;
import android.os.Debug;
import android.os.Environment;
import android.os.FileUtils;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.system.ErrnoException;
import android.system.Os;
import android.util.ArraySet;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import com.android.internal.util.FastPrintWriter;
import com.android.server.EventLogTags;
import com.android.server.pm.dex.PackageDexUsage.PackageUseInfo;
import com.android.server.power.IHwShutdownThread;
import dalvik.system.VMRuntime;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;
import java.util.zip.GZIPInputStream;

public class PackageManagerServiceUtils {
    private static final long SEVEN_DAYS_IN_MILLISECONDS = 604800000;

    private static ArraySet<String> getPackageNamesForIntent(Intent intent, int userId) {
        List<ResolveInfo> ris = null;
        try {
            ris = AppGlobals.getPackageManager().queryIntentReceivers(intent, null, 0, userId).getList();
        } catch (RemoteException e) {
        }
        ArraySet<String> pkgNames = new ArraySet();
        if (ris != null) {
            for (ResolveInfo ri : ris) {
                pkgNames.add(ri.activityInfo.packageName);
            }
        }
        return pkgNames;
    }

    public static void sortPackagesByUsageDate(List<Package> pkgs, PackageManagerService packageManagerService) {
        if (packageManagerService.isHistoricalPackageUsageAvailable()) {
            Collections.sort(pkgs, -$$Lambda$PackageManagerServiceUtils$ePZ6rsJ05hJ2glmOqcq1_jX6J8w.INSTANCE);
        }
    }

    private static void applyPackageFilter(Predicate<Package> filter, Collection<Package> result, Collection<Package> packages, List<Package> sortTemp, PackageManagerService packageManagerService) {
        for (Package pkg : packages) {
            if (filter.test(pkg)) {
                sortTemp.add(pkg);
            }
        }
        sortPackagesByUsageDate(sortTemp, packageManagerService);
        packages.removeAll(sortTemp);
        for (Package pkg2 : sortTemp) {
            result.add(pkg2);
            Collection<Package> deps = packageManagerService.findSharedNonSystemLibraries(pkg2);
            if (!deps.isEmpty()) {
                deps.removeAll(result);
                result.addAll(deps);
                packages.removeAll(deps);
            }
        }
        sortTemp.clear();
    }

    public static List<Package> getPackagesForDexopt(Collection<Package> packages, PackageManagerService packageManagerService) {
        Predicate<Package> remainingPredicate;
        ArrayList<Package> remainingPkgs = new ArrayList(packages);
        LinkedList<Package> result = new LinkedList();
        ArrayList<Package> sortTemp = new ArrayList(remainingPkgs.size());
        applyPackageFilter(-$$Lambda$PackageManagerServiceUtils$QMV-UHbRIK26QMZL5iM27MchX7U.INSTANCE, result, remainingPkgs, sortTemp, packageManagerService);
        packageManagerService.filterShellApps(remainingPkgs, result);
        applyPackageFilter(new -$$Lambda$PackageManagerServiceUtils$nPt0Hym3GvYeWA2vwfOLFDxZmCE(getPackageNamesForIntent(new Intent("android.intent.action.PRE_BOOT_COMPLETED"), null)), result, remainingPkgs, sortTemp, packageManagerService);
        applyPackageFilter(new -$$Lambda$PackageManagerServiceUtils$fMBP3pPR7BB2hICieRxkdNG-3H8(packageManagerService.getDexManager()), result, remainingPkgs, sortTemp, packageManagerService);
        if (remainingPkgs.isEmpty() || !packageManagerService.isHistoricalPackageUsageAvailable()) {
            remainingPredicate = -$$Lambda$PackageManagerServiceUtils$hVRkjdaFuAMTY9J9JQ7JyWMYCHA.INSTANCE;
        } else {
            Predicate<Package> remainingPredicate2;
            long estimatedPreviousSystemUseTime = ((Package) Collections.max(remainingPkgs, -$$Lambda$PackageManagerServiceUtils$whx96xO50U3fax1NRe1upTcx9jc.INSTANCE)).getLatestForegroundPackageUseTimeInMills();
            if (estimatedPreviousSystemUseTime != 0) {
                remainingPredicate2 = new -$$Lambda$PackageManagerServiceUtils$p5q19y4-2x-i747j_hTNL1EMzt0(estimatedPreviousSystemUseTime - 604800000);
            } else {
                remainingPredicate2 = -$$Lambda$PackageManagerServiceUtils$Fz3elZ0VmMMv9-wl_G3AN15dUU8.INSTANCE;
            }
            remainingPredicate = remainingPredicate2;
            sortPackagesByUsageDate(remainingPkgs, packageManagerService);
        }
        applyPackageFilter(remainingPredicate, result, remainingPkgs, sortTemp, packageManagerService);
        return result;
    }

    static /* synthetic */ boolean lambda$getPackagesForDexopt$5(long cutoffTime, Package pkg) {
        return pkg.getLatestForegroundPackageUseTimeInMills() >= cutoffTime;
    }

    public static boolean isUnusedSinceTimeInMillis(long firstInstallTime, long currentTimeInMillis, long thresholdTimeinMillis, PackageUseInfo packageUseInfo, long latestPackageUseTimeInMillis, long latestForegroundPackageUseTimeInMillis) {
        boolean z = false;
        if (currentTimeInMillis - firstInstallTime < thresholdTimeinMillis) {
            return false;
        }
        if (currentTimeInMillis - latestForegroundPackageUseTimeInMillis < thresholdTimeinMillis) {
            return false;
        }
        boolean isActiveInBackgroundAndUsedByOtherPackages = currentTimeInMillis - latestPackageUseTimeInMillis < thresholdTimeinMillis && packageUseInfo.isAnyCodePathUsedByOtherApps();
        if (!isActiveInBackgroundAndUsedByOtherPackages) {
            z = true;
        }
        return z;
    }

    public static String realpath(File path) throws IOException {
        try {
            return Os.realpath(path.getAbsolutePath());
        } catch (ErrnoException ee) {
            throw ee.rethrowAsIOException();
        }
    }

    public static String packagesToString(Collection<Package> c) {
        StringBuilder sb = new StringBuilder();
        for (Package pkg : c) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(pkg.packageName);
        }
        return sb.toString();
    }

    public static boolean checkISA(String isa) {
        for (String abi : Build.SUPPORTED_ABIS) {
            if (VMRuntime.getInstructionSet(abi).equals(isa)) {
                return true;
            }
        }
        return false;
    }

    public static long getLastModifiedTime(Package pkg) {
        File srcFile = new File(pkg.codePath);
        if (!srcFile.isDirectory()) {
            return srcFile.lastModified();
        }
        long maxModifiedTime = new File(pkg.baseCodePath).lastModified();
        if (pkg.splitCodePaths != null) {
            for (int i = pkg.splitCodePaths.length - 1; i >= 0; i--) {
                maxModifiedTime = Math.max(maxModifiedTime, new File(pkg.splitCodePaths[i]).lastModified());
            }
        }
        return maxModifiedTime;
    }

    private static File getSettingsProblemFile() {
        return new File(new File(Environment.getDataDirectory(), "system"), "uiderrors.txt");
    }

    public static void dumpCriticalInfo(ProtoOutputStream proto) {
        BufferedReader in;
        try {
            in = new BufferedReader(new FileReader(getSettingsProblemFile()));
            String line = null;
            while (true) {
                String readLine = in.readLine();
                line = readLine;
                if (readLine == null) {
                    $closeResource(null, in);
                    return;
                } else if (!line.contains("ignored: updated version")) {
                    proto.write(2237677961223L, line);
                }
            }
        } catch (IOException e) {
        } catch (Throwable th) {
            $closeResource(r1, in);
        }
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

    public static void dumpCriticalInfo(PrintWriter pw, String msg) {
        BufferedReader in;
        try {
            in = new BufferedReader(new FileReader(getSettingsProblemFile()));
            String line = null;
            while (true) {
                String readLine = in.readLine();
                line = readLine;
                if (readLine == null) {
                    $closeResource(null, in);
                    return;
                } else if (!line.contains("ignored: updated version")) {
                    if (msg != null) {
                        pw.print(msg);
                    }
                    pw.println(line);
                }
            }
        } catch (IOException e) {
        } catch (Throwable th) {
            $closeResource(r1, in);
        }
    }

    public static void logCriticalInfo(int priority, String msg) {
        Slog.println(priority, "PackageManager", msg);
        EventLogTags.writePmCriticalInfo(msg);
        try {
            File fname = getSettingsProblemFile();
            PrintWriter pw = new FastPrintWriter(new FileOutputStream(fname, true));
            String dateString = new SimpleDateFormat().format(new Date(System.currentTimeMillis()));
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(dateString);
            stringBuilder.append(": ");
            stringBuilder.append(msg);
            pw.println(stringBuilder.toString());
            pw.close();
            FileUtils.setPermissions(fname.toString(), 508, -1, -1);
        } catch (IOException e) {
        }
    }

    public static void enforceShellRestriction(String restriction, int callingUid, int userHandle) {
        if (callingUid != IHwShutdownThread.SHUTDOWN_ANIMATION_WAIT_TIME) {
            return;
        }
        StringBuilder stringBuilder;
        if (userHandle >= 0 && PackageManagerService.sUserManager.hasUserRestriction(restriction, userHandle)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Shell does not have permission to access user ");
            stringBuilder.append(userHandle);
            throw new SecurityException(stringBuilder.toString());
        } else if (userHandle < 0) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Unable to check shell permission for user ");
            stringBuilder.append(userHandle);
            stringBuilder.append("\n\t");
            stringBuilder.append(Debug.getCallers(3));
            Slog.e("PackageManager", stringBuilder.toString());
        }
    }

    public static String deriveAbiOverride(String abiOverride, PackageSetting settings) {
        if ("-".equals(abiOverride)) {
            return null;
        }
        if (abiOverride != null) {
            return abiOverride;
        }
        if (settings != null) {
            return settings.cpuAbiOverrideString;
        }
        return null;
    }

    public static int compareSignatures(Signature[] s1, Signature[] s2) {
        int i = 1;
        if (s1 == null) {
            if (s2 != null) {
                i = -1;
            }
            return i;
        } else if (s2 == null) {
            return -2;
        } else {
            if (s1.length != s2.length) {
                return -3;
            }
            int i2 = 0;
            if (s1.length == 1) {
                if (!s1[0].equals(s2[0])) {
                    i2 = -3;
                }
                return i2;
            }
            ArraySet<Signature> set1 = new ArraySet();
            for (Signature sig : s1) {
                set1.add(sig);
            }
            ArraySet<Signature> set2 = new ArraySet();
            for (Signature sig2 : s2) {
                set2.add(sig2);
            }
            if (set1.equals(set2)) {
                return 0;
            }
            return -3;
        }
    }

    private static boolean matchSignaturesCompat(String packageName, PackageSignatures packageSignatures, SigningDetails parsedSignatures) {
        ArraySet<Signature> existingSet = new ArraySet();
        for (Signature sig : packageSignatures.mSigningDetails.signatures) {
            existingSet.add(sig);
        }
        ArraySet<Signature> scannedCompatSet = new ArraySet();
        for (Signature sig2 : parsedSignatures.signatures) {
            try {
                for (Signature chainSig : sig2.getChainSignatures()) {
                    scannedCompatSet.add(chainSig);
                }
            } catch (CertificateEncodingException e) {
                scannedCompatSet.add(sig2);
            }
        }
        if (scannedCompatSet.equals(existingSet)) {
            packageSignatures.mSigningDetails = parsedSignatures;
            return true;
        }
        if (parsedSignatures.hasPastSigningCertificates()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Existing package ");
            stringBuilder.append(packageName);
            stringBuilder.append(" has flattened signing certificate chain. Unable to install newer version with rotated signing certificate.");
            logCriticalInfo(4, stringBuilder.toString());
        }
        return false;
    }

    private static boolean matchSignaturesRecover(String packageName, SigningDetails existingSignatures, SigningDetails parsedSignatures, @CertCapabilities int flags) {
        StringBuilder stringBuilder;
        String msg = null;
        try {
            if (parsedSignatures.checkCapabilityRecover(existingSignatures, flags)) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Recovered effectively matching certificates for ");
                stringBuilder.append(packageName);
                logCriticalInfo(4, stringBuilder.toString());
                return true;
            }
        } catch (CertificateException e) {
            msg = e.getMessage();
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("Failed to recover certificates for ");
        stringBuilder.append(packageName);
        stringBuilder.append(": ");
        stringBuilder.append(msg);
        logCriticalInfo(4, stringBuilder.toString());
        return false;
    }

    private static boolean matchSignatureInSystem(PackageSetting pkgSetting, PackageSetting disabledPkgSetting) {
        try {
            PackageParser.collectCertificates(disabledPkgSetting.pkg, true);
            if (!pkgSetting.signatures.mSigningDetails.checkCapability(disabledPkgSetting.signatures.mSigningDetails, 1)) {
                if (!disabledPkgSetting.signatures.mSigningDetails.checkCapability(pkgSetting.signatures.mSigningDetails, 8)) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Updated system app mismatches cert on /system: ");
                    stringBuilder.append(pkgSetting.name);
                    logCriticalInfo(6, stringBuilder.toString());
                    return false;
                }
            }
            return true;
        } catch (PackageParserException e) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Failed to collect cert for ");
            stringBuilder2.append(pkgSetting.name);
            stringBuilder2.append(": ");
            stringBuilder2.append(e.getMessage());
            logCriticalInfo(6, stringBuilder2.toString());
            return false;
        }
    }

    static boolean isApkVerityEnabled() {
        return SystemProperties.getInt("ro.apk_verity.mode", 0) != 0;
    }

    static boolean isApkVerificationForced(PackageSetting disabledPs) {
        return disabledPs != null && disabledPs.isPrivileged() && isApkVerityEnabled();
    }

    public static boolean verifySignatures(PackageSetting pkgSetting, PackageSetting disabledPkgSetting, SigningDetails parsedSignatures, boolean compareCompat, boolean compareRecover) throws PackageManagerException {
        boolean match;
        StringBuilder stringBuilder;
        String packageName = pkgSetting.name;
        boolean compatMatch = false;
        boolean z = false;
        if (pkgSetting.signatures.mSigningDetails.signatures != null) {
            match = parsedSignatures.checkCapability(pkgSetting.signatures.mSigningDetails, 1) || pkgSetting.signatures.mSigningDetails.checkCapability(parsedSignatures, 8);
            if (!match && compareCompat) {
                match = matchSignaturesCompat(packageName, pkgSetting.signatures, parsedSignatures);
                compatMatch = match;
            }
            if (!match && compareRecover) {
                boolean z2 = matchSignaturesRecover(packageName, pkgSetting.signatures.mSigningDetails, parsedSignatures, 1) || matchSignaturesRecover(packageName, parsedSignatures, pkgSetting.signatures.mSigningDetails, 8);
                match = z2;
            }
            if (!match && isApkVerificationForced(disabledPkgSetting)) {
                match = matchSignatureInSystem(pkgSetting, disabledPkgSetting);
            }
            if (!match) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Package ");
                stringBuilder.append(packageName);
                stringBuilder.append(" signatures do not match previously installed version; ignoring!");
                throw new PackageManagerException(-7, stringBuilder.toString());
            }
        }
        if (!(pkgSetting.sharedUser == null || pkgSetting.sharedUser.signatures.mSigningDetails == SigningDetails.UNKNOWN)) {
            match = parsedSignatures.checkCapability(pkgSetting.sharedUser.signatures.mSigningDetails, 2) || pkgSetting.sharedUser.signatures.mSigningDetails.checkCapability(parsedSignatures, 2);
            if (!match && compareCompat) {
                match = matchSignaturesCompat(packageName, pkgSetting.sharedUser.signatures, parsedSignatures);
            }
            if (!match && compareRecover) {
                if (matchSignaturesRecover(packageName, pkgSetting.sharedUser.signatures.mSigningDetails, parsedSignatures, 2) || matchSignaturesRecover(packageName, parsedSignatures, pkgSetting.sharedUser.signatures.mSigningDetails, 2)) {
                    z = true;
                }
                match = z;
                compatMatch |= match;
            }
            if (!match) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Package ");
                stringBuilder.append(packageName);
                stringBuilder.append(" has no signatures that match those in shared user ");
                stringBuilder.append(pkgSetting.sharedUser.name);
                stringBuilder.append("; ignoring!");
                throw new PackageManagerException(-8, stringBuilder.toString());
            }
        }
        return compatMatch;
    }

    public static int decompressFile(File srcFile, File dstFile) throws ErrnoException {
        Throwable th;
        Throwable th2;
        if (PackageManagerService.DEBUG_COMPRESSION) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Decompress file; src: ");
            stringBuilder.append(srcFile.getAbsolutePath());
            stringBuilder.append(", dst: ");
            stringBuilder.append(dstFile.getAbsolutePath());
            Slog.i("PackageManager", stringBuilder.toString());
        }
        InputStream fileIn;
        try {
            fileIn = new GZIPInputStream(new FileInputStream(srcFile));
            OutputStream fileOut = new FileOutputStream(dstFile, false);
            try {
                FileUtils.copy(fileIn, fileOut);
                Os.chmod(dstFile.getAbsolutePath(), 420);
                $closeResource(null, fileOut);
                $closeResource(null, fileIn);
                return 1;
            } catch (Throwable th22) {
                Throwable th3 = th22;
                th22 = th;
                th = th3;
            }
            $closeResource(th22, fileOut);
            throw th;
        } catch (IOException e) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Failed to decompress file; src: ");
            stringBuilder2.append(srcFile.getAbsolutePath());
            stringBuilder2.append(", dst: ");
            stringBuilder2.append(dstFile.getAbsolutePath());
            logCriticalInfo(6, stringBuilder2.toString());
            return RequestStatus.SYS_ETIMEDOUT;
        } catch (Throwable th4) {
            $closeResource(r1, fileIn);
        }
    }

    public static File[] getCompressedFiles(String codePath) {
        File stubCodePath = new File(codePath);
        String stubName = stubCodePath.getName();
        int idx = stubName.lastIndexOf(PackageManagerService.STUB_SUFFIX);
        if (idx < 0 || stubName.length() != PackageManagerService.STUB_SUFFIX.length() + idx) {
            return null;
        }
        File stubParentDir = stubCodePath.getParentFile();
        if (stubParentDir == null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unable to determine stub parent dir for codePath: ");
            stringBuilder.append(codePath);
            Slog.e("PackageManager", stringBuilder.toString());
            return null;
        }
        File[] files = new File(stubParentDir, stubName.substring(0, idx)).listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(PackageManagerService.COMPRESSED_EXTENSION);
            }
        });
        if (PackageManagerService.DEBUG_COMPRESSION && files != null && files.length > 0) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("getCompressedFiles[");
            stringBuilder2.append(codePath);
            stringBuilder2.append("]: ");
            stringBuilder2.append(Arrays.toString(files));
            Slog.i("PackageManager", stringBuilder2.toString());
        }
        return files;
    }

    public static boolean compressedFileExists(String codePath) {
        File[] compressedFiles = getCompressedFiles(codePath);
        return compressedFiles != null && compressedFiles.length > 0;
    }
}
