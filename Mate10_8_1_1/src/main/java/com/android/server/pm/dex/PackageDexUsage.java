package com.android.server.pm.dex;

import android.os.Build;
import android.util.AtomicFile;
import android.util.Slog;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.FastPrintWriter;
import com.android.server.pm.AbstractStatsBase;
import com.android.server.pm.PackageManagerServiceUtils;
import dalvik.system.VMRuntime;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import libcore.io.IoUtils;
import libcore.util.Objects;

public class PackageDexUsage extends AbstractStatsBase<Void> {
    private static final String CODE_PATH_LINE_CHAR = "+";
    private static final String DEX_LINE_CHAR = "#";
    private static final String LOADING_PACKAGE_CHAR = "@";
    private static final int PACKAGE_DEX_USAGE_SUPPORTED_VERSION_1 = 1;
    private static final int PACKAGE_DEX_USAGE_SUPPORTED_VERSION_2 = 2;
    private static final int PACKAGE_DEX_USAGE_VERSION = 2;
    private static final String PACKAGE_DEX_USAGE_VERSION_HEADER = "PACKAGE_MANAGER__PACKAGE_DEX_USAGE__";
    private static final String SPLIT_CHAR = ",";
    private static final String TAG = "PackageDexUsage";
    static final String UNKNOWN_CLASS_LOADER_CONTEXT = "=UnknownClassLoaderContext=";
    static final String UNSUPPORTED_CLASS_LOADER_CONTEXT = "=UnsupportedClassLoaderContext=";
    static final String VARIABLE_CLASS_LOADER_CONTEXT = "=VariableClassLoaderContext=";
    @GuardedBy("mPackageUseInfoMap")
    private final Map<String, PackageUseInfo> mPackageUseInfoMap = new HashMap();

    public static class DexUseInfo {
        private String mClassLoaderContext;
        private boolean mIsUsedByOtherApps;
        private final Set<String> mLoaderIsas;
        private final Set<String> mLoadingPackages;
        private final int mOwnerUserId;

        public DexUseInfo(boolean isUsedByOtherApps, int ownerUserId, String classLoaderContext, String loaderIsa) {
            this.mIsUsedByOtherApps = isUsedByOtherApps;
            this.mOwnerUserId = ownerUserId;
            this.mClassLoaderContext = classLoaderContext;
            this.mLoaderIsas = new HashSet();
            if (loaderIsa != null) {
                this.mLoaderIsas.add(loaderIsa);
            }
            this.mLoadingPackages = new HashSet();
        }

        public DexUseInfo(DexUseInfo other) {
            this.mIsUsedByOtherApps = other.mIsUsedByOtherApps;
            this.mOwnerUserId = other.mOwnerUserId;
            this.mClassLoaderContext = other.mClassLoaderContext;
            this.mLoaderIsas = new HashSet(other.mLoaderIsas);
            this.mLoadingPackages = new HashSet(other.mLoadingPackages);
        }

        private boolean merge(DexUseInfo dexUseInfo) {
            boolean z;
            boolean oldIsUsedByOtherApps = this.mIsUsedByOtherApps;
            if (this.mIsUsedByOtherApps) {
                z = true;
            } else {
                z = dexUseInfo.mIsUsedByOtherApps;
            }
            this.mIsUsedByOtherApps = z;
            boolean updateIsas = this.mLoaderIsas.addAll(dexUseInfo.mLoaderIsas);
            boolean updateLoadingPackages = this.mLoadingPackages.addAll(dexUseInfo.mLoadingPackages);
            String oldClassLoaderContext = this.mClassLoaderContext;
            if (PackageDexUsage.UNKNOWN_CLASS_LOADER_CONTEXT.equals(this.mClassLoaderContext)) {
                this.mClassLoaderContext = dexUseInfo.mClassLoaderContext;
            } else if (PackageDexUsage.UNSUPPORTED_CLASS_LOADER_CONTEXT.equals(dexUseInfo.mClassLoaderContext)) {
                this.mClassLoaderContext = PackageDexUsage.UNSUPPORTED_CLASS_LOADER_CONTEXT;
            } else if (!(PackageDexUsage.UNSUPPORTED_CLASS_LOADER_CONTEXT.equals(this.mClassLoaderContext) || (Objects.equal(this.mClassLoaderContext, dexUseInfo.mClassLoaderContext) ^ 1) == 0)) {
                this.mClassLoaderContext = PackageDexUsage.VARIABLE_CLASS_LOADER_CONTEXT;
            }
            if (updateIsas || oldIsUsedByOtherApps != this.mIsUsedByOtherApps || updateLoadingPackages) {
                return true;
            }
            return Objects.equal(oldClassLoaderContext, this.mClassLoaderContext) ^ 1;
        }

        public boolean isUsedByOtherApps() {
            return this.mIsUsedByOtherApps;
        }

        public int getOwnerUserId() {
            return this.mOwnerUserId;
        }

        public Set<String> getLoaderIsas() {
            return this.mLoaderIsas;
        }

        public Set<String> getLoadingPackages() {
            return this.mLoadingPackages;
        }

        public String getClassLoaderContext() {
            return this.mClassLoaderContext;
        }

        public boolean isUnsupportedClassLoaderContext() {
            return PackageDexUsage.UNSUPPORTED_CLASS_LOADER_CONTEXT.equals(this.mClassLoaderContext);
        }

        public boolean isUnknownClassLoaderContext() {
            return PackageDexUsage.UNKNOWN_CLASS_LOADER_CONTEXT.equals(this.mClassLoaderContext);
        }

        public boolean isVariableClassLoaderContext() {
            return PackageDexUsage.VARIABLE_CLASS_LOADER_CONTEXT.equals(this.mClassLoaderContext);
        }
    }

    public static class PackageUseInfo {
        private final Map<String, Set<String>> mCodePathsUsedByOtherApps;
        private final Map<String, DexUseInfo> mDexUseInfoMap;
        private boolean mUsedByOtherAppsBeforeUpgrade;

        public PackageUseInfo() {
            this.mCodePathsUsedByOtherApps = new HashMap();
            this.mDexUseInfoMap = new HashMap();
        }

        public PackageUseInfo(PackageUseInfo other) {
            this.mCodePathsUsedByOtherApps = new HashMap();
            for (Entry<String, Set<String>> e : other.mCodePathsUsedByOtherApps.entrySet()) {
                this.mCodePathsUsedByOtherApps.put((String) e.getKey(), new HashSet((Collection) e.getValue()));
            }
            this.mDexUseInfoMap = new HashMap();
            for (Entry<String, DexUseInfo> e2 : other.mDexUseInfoMap.entrySet()) {
                this.mDexUseInfoMap.put((String) e2.getKey(), new DexUseInfo((DexUseInfo) e2.getValue()));
            }
        }

        private boolean mergeCodePathUsedByOtherApps(String codePath, boolean isUsedByOtherApps, String owningPackageName, String loadingPackage) {
            if (!isUsedByOtherApps) {
                return false;
            }
            boolean newLoadingPackage;
            boolean newCodePath = false;
            Set<String> loadingPackages = (Set) this.mCodePathsUsedByOtherApps.get(codePath);
            if (loadingPackages == null) {
                loadingPackages = new HashSet();
                this.mCodePathsUsedByOtherApps.put(codePath, loadingPackages);
                newCodePath = true;
            }
            if (loadingPackage == null || (loadingPackage.equals(owningPackageName) ^ 1) == 0) {
                newLoadingPackage = false;
            } else {
                newLoadingPackage = loadingPackages.add(loadingPackage);
            }
            if (newCodePath) {
                newLoadingPackage = true;
            }
            return newLoadingPackage;
        }

        public boolean isUsedByOtherApps(String codePath) {
            return this.mCodePathsUsedByOtherApps.containsKey(codePath);
        }

        public Map<String, DexUseInfo> getDexUseInfoMap() {
            return this.mDexUseInfoMap;
        }

        public Set<String> getLoadingPackages(String codePath) {
            return (Set) this.mCodePathsUsedByOtherApps.getOrDefault(codePath, null);
        }

        public boolean isAnyCodePathUsedByOtherApps() {
            return this.mCodePathsUsedByOtherApps.isEmpty() ^ 1;
        }

        boolean clearCodePathUsedByOtherApps() {
            this.mUsedByOtherAppsBeforeUpgrade = true;
            if (this.mCodePathsUsedByOtherApps.isEmpty()) {
                return false;
            }
            this.mCodePathsUsedByOtherApps.clear();
            return true;
        }
    }

    public PackageDexUsage() {
        super("package-dex-usage.list", "PackageDexUsage_DiskWriter", false);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean record(String owningPackageName, String dexPath, int ownerUserId, String loaderIsa, boolean isUsedByOtherApps, boolean primaryOrSplit, String loadingPackageName, String classLoaderContext) {
        if (!PackageManagerServiceUtils.checkISA(loaderIsa)) {
            throw new IllegalArgumentException("loaderIsa " + loaderIsa + " is unsupported");
        } else if (classLoaderContext == null) {
            throw new IllegalArgumentException("Null classLoaderContext");
        } else {
            synchronized (this.mPackageUseInfoMap) {
                PackageUseInfo packageUseInfo = (PackageUseInfo) this.mPackageUseInfoMap.get(owningPackageName);
                DexUseInfo newData;
                if (packageUseInfo == null) {
                    packageUseInfo = new PackageUseInfo();
                    if (primaryOrSplit) {
                        packageUseInfo.mergeCodePathUsedByOtherApps(dexPath, isUsedByOtherApps, owningPackageName, loadingPackageName);
                    } else {
                        newData = new DexUseInfo(isUsedByOtherApps, ownerUserId, classLoaderContext, loaderIsa);
                        packageUseInfo.mDexUseInfoMap.put(dexPath, newData);
                        maybeAddLoadingPackage(owningPackageName, loadingPackageName, newData.mLoadingPackages);
                    }
                    this.mPackageUseInfoMap.put(owningPackageName, packageUseInfo);
                    return true;
                } else if (primaryOrSplit) {
                    boolean -wrap0 = packageUseInfo.mergeCodePathUsedByOtherApps(dexPath, isUsedByOtherApps, owningPackageName, loadingPackageName);
                    return -wrap0;
                } else {
                    newData = new DexUseInfo(isUsedByOtherApps, ownerUserId, classLoaderContext, loaderIsa);
                    boolean updateLoadingPackages = maybeAddLoadingPackage(owningPackageName, loadingPackageName, newData.mLoadingPackages);
                    DexUseInfo existingData = (DexUseInfo) packageUseInfo.mDexUseInfoMap.get(dexPath);
                    if (existingData == null) {
                        packageUseInfo.mDexUseInfoMap.put(dexPath, newData);
                        return true;
                    } else if (ownerUserId != existingData.mOwnerUserId) {
                        throw new IllegalArgumentException("Trying to change ownerUserId for  dex path " + dexPath + " from " + existingData.mOwnerUserId + " to " + ownerUserId);
                    } else if (existingData.merge(newData)) {
                        updateLoadingPackages = true;
                    }
                }
            }
        }
    }

    public void read() {
        read((Void) null);
    }

    void maybeWriteAsync() {
        maybeWriteAsync(null);
    }

    void writeNow() {
        writeInternal(null);
    }

    protected void writeInternal(Void data) {
        AtomicFile file = getFile();
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = file.startWrite();
            OutputStreamWriter osw = new OutputStreamWriter(fileOutputStream);
            write(osw);
            osw.flush();
            file.finishWrite(fileOutputStream);
        } catch (IOException e) {
            if (fileOutputStream != null) {
                file.failWrite(fileOutputStream);
            }
            Slog.e(TAG, "Failed to write usage for dex files", e);
        }
    }

    void write(Writer out) {
        Map<String, PackageUseInfo> packageUseInfoMapClone = clonePackageUseInfoMap();
        FastPrintWriter fpw = new FastPrintWriter(out);
        fpw.print(PACKAGE_DEX_USAGE_VERSION_HEADER);
        fpw.println(2);
        for (Entry<String, PackageUseInfo> pEntry : packageUseInfoMapClone.entrySet()) {
            PackageUseInfo packageUseInfo = (PackageUseInfo) pEntry.getValue();
            fpw.println((String) pEntry.getKey());
            for (Entry<String, Set<String>> codeEntry : packageUseInfo.mCodePathsUsedByOtherApps.entrySet()) {
                Set<String> loadingPackages = (Set) codeEntry.getValue();
                fpw.println(CODE_PATH_LINE_CHAR + ((String) codeEntry.getKey()));
                fpw.println(LOADING_PACKAGE_CHAR + String.join(SPLIT_CHAR, loadingPackages));
            }
            for (Entry<String, DexUseInfo> dEntry : packageUseInfo.mDexUseInfoMap.entrySet()) {
                DexUseInfo dexUseInfo = (DexUseInfo) dEntry.getValue();
                fpw.println(DEX_LINE_CHAR + ((String) dEntry.getKey()));
                fpw.print(String.join(SPLIT_CHAR, new CharSequence[]{Integer.toString(dexUseInfo.mOwnerUserId), writeBoolean(dexUseInfo.mIsUsedByOtherApps)}));
                for (String isa : dexUseInfo.mLoaderIsas) {
                    fpw.print(SPLIT_CHAR + isa);
                }
                fpw.println();
                fpw.println(LOADING_PACKAGE_CHAR + String.join(SPLIT_CHAR, dexUseInfo.mLoadingPackages));
                fpw.println(dexUseInfo.getClassLoaderContext());
            }
        }
        fpw.flush();
    }

    protected void readInternal(Void data) {
        IOException e;
        Object obj;
        Throwable th;
        BufferedReader in = null;
        try {
            BufferedReader in2 = new BufferedReader(new InputStreamReader(getFile().openRead()));
            try {
                read(in2);
                IoUtils.closeQuietly(in2);
                in = in2;
            } catch (FileNotFoundException e2) {
                in = in2;
                IoUtils.closeQuietly(in);
            } catch (IOException e3) {
                e = e3;
                obj = in2;
                try {
                    Slog.w(TAG, "Failed to parse package dex usage.", e);
                    IoUtils.closeQuietly(r3);
                } catch (Throwable th2) {
                    th = th2;
                    IoUtils.closeQuietly(r3);
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                obj = in2;
                IoUtils.closeQuietly(r3);
                throw th;
            }
        } catch (FileNotFoundException e4) {
            IoUtils.closeQuietly(in);
        } catch (IOException e5) {
            e = e5;
            Slog.w(TAG, "Failed to parse package dex usage.", e);
            IoUtils.closeQuietly(r3);
        }
    }

    void read(Reader reader) throws IOException {
        Map<String, PackageUseInfo> data = new HashMap();
        BufferedReader in = new BufferedReader(reader);
        String versionLine = in.readLine();
        if (versionLine == null) {
            throw new IllegalStateException("No version line found.");
        } else if (versionLine.startsWith(PACKAGE_DEX_USAGE_VERSION_HEADER)) {
            int version = Integer.parseInt(versionLine.substring(PACKAGE_DEX_USAGE_VERSION_HEADER.length()));
            if (isSupportedVersion(version)) {
                String str = null;
                PackageUseInfo packageUseInfo = null;
                Set<String> supportedIsas = new HashSet();
                for (String abi : Build.SUPPORTED_ABIS) {
                    supportedIsas.add(VMRuntime.getInstructionSet(abi));
                }
                while (true) {
                    String line = in.readLine();
                    if (line == null) {
                        synchronized (this.mPackageUseInfoMap) {
                            this.mPackageUseInfoMap.clear();
                            this.mPackageUseInfoMap.putAll(data);
                        }
                        return;
                    } else if (line.startsWith(DEX_LINE_CHAR)) {
                        if (str == null) {
                            throw new IllegalStateException("Malformed PackageDexUsage file. Expected package line before dex line.");
                        }
                        String dexPath = line.substring(DEX_LINE_CHAR.length());
                        line = in.readLine();
                        if (line == null) {
                            throw new IllegalStateException("Could not find dexUseInfo line");
                        }
                        elems = line.split(SPLIT_CHAR);
                        if (elems.length < 3) {
                            throw new IllegalStateException("Invalid PackageDexUsage line: " + line);
                        }
                        Set<String> loadingPackages = maybeReadLoadingPackages(in, version);
                        DexUseInfo dexUseInfo = new DexUseInfo(readBoolean(elems[1]), Integer.parseInt(elems[0]), maybeReadClassLoaderContext(in, version), null);
                        dexUseInfo.mLoadingPackages.addAll(loadingPackages);
                        for (int i = 2; i < elems.length; i++) {
                            String isa = elems[i];
                            if (supportedIsas.contains(isa)) {
                                dexUseInfo.mLoaderIsas.add(elems[i]);
                            } else {
                                Slog.wtf(TAG, "Unsupported ISA when parsing PackageDexUsage: " + isa);
                            }
                        }
                        if (supportedIsas.isEmpty()) {
                            Slog.wtf(TAG, "Ignore dexPath when parsing PackageDexUsage because of unsupported isas. dexPath=" + dexPath);
                        } else {
                            packageUseInfo.mDexUseInfoMap.put(dexPath, dexUseInfo);
                        }
                    } else if (!line.startsWith(CODE_PATH_LINE_CHAR)) {
                        if (version >= 2) {
                            str = line;
                            packageUseInfo = new PackageUseInfo();
                        } else {
                            elems = line.split(SPLIT_CHAR);
                            if (elems.length != 2) {
                                throw new IllegalStateException("Invalid PackageDexUsage line: " + line);
                            }
                            str = elems[0];
                            packageUseInfo = new PackageUseInfo();
                            packageUseInfo.mUsedByOtherAppsBeforeUpgrade = readBoolean(elems[1]);
                        }
                        data.put(str, packageUseInfo);
                    } else if (version < 2) {
                        throw new IllegalArgumentException("Unexpected code path line when parsing PackageDexUseData: " + line);
                    } else {
                        packageUseInfo.mCodePathsUsedByOtherApps.put(line.substring(CODE_PATH_LINE_CHAR.length()), maybeReadLoadingPackages(in, version));
                    }
                }
            } else {
                throw new IllegalStateException("Unexpected version: " + version);
            }
        } else {
            throw new IllegalStateException("Invalid version line: " + versionLine);
        }
    }

    private String maybeReadClassLoaderContext(BufferedReader in, int version) throws IOException {
        String str = null;
        if (version >= 2) {
            str = in.readLine();
            if (str == null) {
                throw new IllegalStateException("Could not find the classLoaderContext line.");
            }
        }
        return str == null ? UNKNOWN_CLASS_LOADER_CONTEXT : str;
    }

    private Set<String> maybeReadLoadingPackages(BufferedReader in, int version) throws IOException {
        if (version < 2) {
            return Collections.emptySet();
        }
        String line = in.readLine();
        if (line == null) {
            throw new IllegalStateException("Could not find the loadingPackages line.");
        } else if (line.length() == LOADING_PACKAGE_CHAR.length()) {
            return Collections.emptySet();
        } else {
            Set<String> result = new HashSet();
            Collections.addAll(result, line.substring(LOADING_PACKAGE_CHAR.length()).split(SPLIT_CHAR));
            return result;
        }
    }

    private boolean maybeAddLoadingPackage(String owningPackage, String loadingPackage, Set<String> loadingPackages) {
        return !owningPackage.equals(loadingPackage) ? loadingPackages.add(loadingPackage) : false;
    }

    private boolean isSupportedVersion(int version) {
        if (version == 1 || version == 2) {
            return true;
        }
        return false;
    }

    void syncData(Map<String, Set<Integer>> packageToUsersMap, Map<String, Set<String>> packageToCodePaths) {
        synchronized (this.mPackageUseInfoMap) {
            Iterator<Entry<String, PackageUseInfo>> pIt = this.mPackageUseInfoMap.entrySet().iterator();
            while (pIt.hasNext()) {
                Entry<String, PackageUseInfo> pEntry = (Entry) pIt.next();
                String packageName = (String) pEntry.getKey();
                PackageUseInfo packageUseInfo = (PackageUseInfo) pEntry.getValue();
                Set<Integer> users = (Set) packageToUsersMap.get(packageName);
                if (users == null) {
                    pIt.remove();
                } else {
                    Iterator<Entry<String, DexUseInfo>> dIt = packageUseInfo.mDexUseInfoMap.entrySet().iterator();
                    while (dIt.hasNext()) {
                        if (!users.contains(Integer.valueOf(((DexUseInfo) ((Entry) dIt.next()).getValue()).mOwnerUserId))) {
                            dIt.remove();
                        }
                    }
                    Set<String> codePaths = (Set) packageToCodePaths.get(packageName);
                    Iterator<Entry<String, Set<String>>> codeIt = packageUseInfo.mCodePathsUsedByOtherApps.entrySet().iterator();
                    while (codeIt.hasNext()) {
                        if (!codePaths.contains(((Entry) codeIt.next()).getKey())) {
                            codeIt.remove();
                        }
                    }
                    if (packageUseInfo.mUsedByOtherAppsBeforeUpgrade) {
                        for (String codePath : codePaths) {
                            packageUseInfo.mergeCodePathUsedByOtherApps(codePath, true, null, null);
                        }
                    } else if (!packageUseInfo.isAnyCodePathUsedByOtherApps() && packageUseInfo.mDexUseInfoMap.isEmpty()) {
                        pIt.remove();
                    }
                }
            }
        }
    }

    boolean clearUsedByOtherApps(String packageName) {
        synchronized (this.mPackageUseInfoMap) {
            PackageUseInfo packageUseInfo = (PackageUseInfo) this.mPackageUseInfoMap.get(packageName);
            if (packageUseInfo == null) {
                return false;
            }
            boolean clearCodePathUsedByOtherApps = packageUseInfo.clearCodePathUsedByOtherApps();
            return clearCodePathUsedByOtherApps;
        }
    }

    public boolean removePackage(String packageName) {
        boolean z;
        synchronized (this.mPackageUseInfoMap) {
            z = this.mPackageUseInfoMap.remove(packageName) != null;
        }
        return z;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    boolean removeUserPackage(String packageName, int userId) {
        synchronized (this.mPackageUseInfoMap) {
            PackageUseInfo packageUseInfo = (PackageUseInfo) this.mPackageUseInfoMap.get(packageName);
            if (packageUseInfo == null) {
                return false;
            }
            boolean updated = false;
            Iterator<Entry<String, DexUseInfo>> dIt = packageUseInfo.mDexUseInfoMap.entrySet().iterator();
            while (dIt.hasNext()) {
                if (((DexUseInfo) ((Entry) dIt.next()).getValue()).mOwnerUserId == userId) {
                    dIt.remove();
                    updated = true;
                }
            }
            if (packageUseInfo.mDexUseInfoMap.isEmpty() && (packageUseInfo.isAnyCodePathUsedByOtherApps() ^ 1) != 0) {
                this.mPackageUseInfoMap.remove(packageName);
                updated = true;
            }
        }
    }

    boolean removeDexFile(String packageName, String dexFile, int userId) {
        synchronized (this.mPackageUseInfoMap) {
            PackageUseInfo packageUseInfo = (PackageUseInfo) this.mPackageUseInfoMap.get(packageName);
            if (packageUseInfo == null) {
                return false;
            }
            boolean removeDexFile = removeDexFile(packageUseInfo, dexFile, userId);
            return removeDexFile;
        }
    }

    private boolean removeDexFile(PackageUseInfo packageUseInfo, String dexFile, int userId) {
        DexUseInfo dexUseInfo = (DexUseInfo) packageUseInfo.mDexUseInfoMap.get(dexFile);
        if (dexUseInfo == null || dexUseInfo.mOwnerUserId != userId) {
            return false;
        }
        packageUseInfo.mDexUseInfoMap.remove(dexFile);
        return true;
    }

    PackageUseInfo getPackageUseInfo(String packageName) {
        PackageUseInfo packageUseInfo = null;
        synchronized (this.mPackageUseInfoMap) {
            PackageUseInfo useInfo = (PackageUseInfo) this.mPackageUseInfoMap.get(packageName);
            if (useInfo != null) {
                packageUseInfo = new PackageUseInfo(useInfo);
            }
        }
        return packageUseInfo;
    }

    Set<String> getAllPackagesWithSecondaryDexFiles() {
        Set<String> packages = new HashSet();
        synchronized (this.mPackageUseInfoMap) {
            for (Entry<String, PackageUseInfo> entry : this.mPackageUseInfoMap.entrySet()) {
                if (!((PackageUseInfo) entry.getValue()).mDexUseInfoMap.isEmpty()) {
                    packages.add((String) entry.getKey());
                }
            }
        }
        return packages;
    }

    public void clear() {
        synchronized (this.mPackageUseInfoMap) {
            this.mPackageUseInfoMap.clear();
        }
    }

    private Map<String, PackageUseInfo> clonePackageUseInfoMap() {
        Map<String, PackageUseInfo> clone = new HashMap();
        synchronized (this.mPackageUseInfoMap) {
            for (Entry<String, PackageUseInfo> e : this.mPackageUseInfoMap.entrySet()) {
                clone.put((String) e.getKey(), new PackageUseInfo((PackageUseInfo) e.getValue()));
            }
        }
        return clone;
    }

    private String writeBoolean(boolean bool) {
        return bool ? "1" : "0";
    }

    private boolean readBoolean(String bool) {
        if ("0".equals(bool)) {
            return false;
        }
        if ("1".equals(bool)) {
            return true;
        }
        throw new IllegalArgumentException("Unknown bool encoding: " + bool);
    }

    public String dump() {
        StringWriter sw = new StringWriter();
        write(sw);
        return sw.toString();
    }
}
