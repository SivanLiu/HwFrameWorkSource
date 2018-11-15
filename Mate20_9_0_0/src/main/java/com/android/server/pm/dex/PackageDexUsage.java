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
import java.util.Objects;
import java.util.Set;
import libcore.io.IoUtils;

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
            boolean oldIsUsedByOtherApps = this.mIsUsedByOtherApps;
            boolean z = this.mIsUsedByOtherApps || dexUseInfo.mIsUsedByOtherApps;
            this.mIsUsedByOtherApps = z;
            z = this.mLoaderIsas.addAll(dexUseInfo.mLoaderIsas);
            boolean updateLoadingPackages = this.mLoadingPackages.addAll(dexUseInfo.mLoadingPackages);
            String oldClassLoaderContext = this.mClassLoaderContext;
            if (PackageDexUsage.UNKNOWN_CLASS_LOADER_CONTEXT.equals(this.mClassLoaderContext)) {
                this.mClassLoaderContext = dexUseInfo.mClassLoaderContext;
            } else if (PackageDexUsage.UNSUPPORTED_CLASS_LOADER_CONTEXT.equals(dexUseInfo.mClassLoaderContext)) {
                this.mClassLoaderContext = PackageDexUsage.UNSUPPORTED_CLASS_LOADER_CONTEXT;
            } else if (!(PackageDexUsage.UNSUPPORTED_CLASS_LOADER_CONTEXT.equals(this.mClassLoaderContext) || Objects.equals(this.mClassLoaderContext, dexUseInfo.mClassLoaderContext))) {
                this.mClassLoaderContext = PackageDexUsage.VARIABLE_CLASS_LOADER_CONTEXT;
            }
            if (z || oldIsUsedByOtherApps != this.mIsUsedByOtherApps || updateLoadingPackages || !Objects.equals(oldClassLoaderContext, this.mClassLoaderContext)) {
                return true;
            }
            return false;
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
            boolean z = false;
            if (!isUsedByOtherApps) {
                return false;
            }
            boolean newCodePath = false;
            Set<String> loadingPackages = (Set) this.mCodePathsUsedByOtherApps.get(codePath);
            if (loadingPackages == null) {
                loadingPackages = new HashSet();
                this.mCodePathsUsedByOtherApps.put(codePath, loadingPackages);
                newCodePath = true;
            }
            boolean newLoadingPackage = (loadingPackage == null || loadingPackage.equals(owningPackageName) || !loadingPackages.add(loadingPackage)) ? false : true;
            if (newCodePath || newLoadingPackage) {
                z = true;
            }
            return z;
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

    /* JADX WARNING: Missing block: B:31:0x008a, code:
            return r10;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean record(String owningPackageName, String dexPath, int ownerUserId, String loaderIsa, boolean isUsedByOtherApps, boolean primaryOrSplit, String loadingPackageName, String classLoaderContext) {
        String str = owningPackageName;
        String str2 = dexPath;
        int i = ownerUserId;
        String str3 = loaderIsa;
        boolean z = isUsedByOtherApps;
        String str4 = loadingPackageName;
        String str5 = classLoaderContext;
        if (!PackageManagerServiceUtils.checkISA(loaderIsa)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("loaderIsa ");
            stringBuilder.append(str3);
            stringBuilder.append(" is unsupported");
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (str5 != null) {
            synchronized (this.mPackageUseInfoMap) {
                PackageUseInfo packageUseInfo = (PackageUseInfo) this.mPackageUseInfoMap.get(str);
                boolean z2 = true;
                DexUseInfo newData;
                if (packageUseInfo == null) {
                    packageUseInfo = new PackageUseInfo();
                    if (primaryOrSplit) {
                        packageUseInfo.mergeCodePathUsedByOtherApps(str2, z, str, str4);
                    } else {
                        newData = new DexUseInfo(z, i, str5, str3);
                        packageUseInfo.mDexUseInfoMap.put(str2, newData);
                        maybeAddLoadingPackage(str, str4, newData.mLoadingPackages);
                    }
                    this.mPackageUseInfoMap.put(str, packageUseInfo);
                    return true;
                } else if (primaryOrSplit) {
                    z2 = packageUseInfo.mergeCodePathUsedByOtherApps(str2, z, str, str4);
                    return z2;
                } else {
                    newData = new DexUseInfo(z, i, str5, str3);
                    boolean updateLoadingPackages = maybeAddLoadingPackage(str, str4, newData.mLoadingPackages);
                    DexUseInfo existingData = (DexUseInfo) packageUseInfo.mDexUseInfoMap.get(str2);
                    if (existingData == null) {
                        packageUseInfo.mDexUseInfoMap.put(str2, newData);
                        return true;
                    } else if (i != existingData.mOwnerUserId) {
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Trying to change ownerUserId for  dex path ");
                        stringBuilder2.append(str2);
                        stringBuilder2.append(" from ");
                        stringBuilder2.append(existingData.mOwnerUserId);
                        stringBuilder2.append(" to ");
                        stringBuilder2.append(i);
                        throw new IllegalArgumentException(stringBuilder2.toString());
                    } else if (!(existingData.merge(newData) || updateLoadingPackages)) {
                        z2 = false;
                    }
                }
            }
        } else {
            throw new IllegalArgumentException("Null classLoaderContext");
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
        FileOutputStream f = null;
        try {
            f = file.startWrite();
            OutputStreamWriter osw = new OutputStreamWriter(f);
            write(osw);
            osw.flush();
            file.finishWrite(f);
        } catch (IOException e) {
            if (f != null) {
                file.failWrite(f);
            }
            Slog.e(TAG, "Failed to write usage for dex files", e);
        }
    }

    void write(Writer out) {
        Map<String, PackageUseInfo> packageUseInfoMapClone = clonePackageUseInfoMap();
        FastPrintWriter fpw = new FastPrintWriter(out);
        fpw.print(PACKAGE_DEX_USAGE_VERSION_HEADER);
        int i = 2;
        fpw.println(2);
        for (Entry<String, PackageUseInfo> pEntry : packageUseInfoMapClone.entrySet()) {
            String codePath;
            StringBuilder stringBuilder;
            PackageUseInfo packageUseInfo = (PackageUseInfo) pEntry.getValue();
            fpw.println((String) pEntry.getKey());
            for (Entry<String, Set<String>> codeEntry : packageUseInfo.mCodePathsUsedByOtherApps.entrySet()) {
                codePath = (String) codeEntry.getKey();
                Set<String> loadingPackages = (Set) codeEntry.getValue();
                stringBuilder = new StringBuilder();
                stringBuilder.append(CODE_PATH_LINE_CHAR);
                stringBuilder.append(codePath);
                fpw.println(stringBuilder.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append(LOADING_PACKAGE_CHAR);
                stringBuilder.append(String.join(SPLIT_CHAR, loadingPackages));
                fpw.println(stringBuilder.toString());
            }
            for (Entry<String, DexUseInfo> dEntry : packageUseInfo.mDexUseInfoMap.entrySet()) {
                codePath = (String) dEntry.getKey();
                DexUseInfo dexUseInfo = (DexUseInfo) dEntry.getValue();
                stringBuilder = new StringBuilder();
                stringBuilder.append(DEX_LINE_CHAR);
                stringBuilder.append(codePath);
                fpw.println(stringBuilder.toString());
                CharSequence charSequence = SPLIT_CHAR;
                CharSequence[] charSequenceArr = new CharSequence[i];
                charSequenceArr[0] = Integer.toString(dexUseInfo.mOwnerUserId);
                charSequenceArr[1] = writeBoolean(dexUseInfo.mIsUsedByOtherApps);
                fpw.print(String.join(charSequence, charSequenceArr));
                for (String isa : dexUseInfo.mLoaderIsas) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(SPLIT_CHAR);
                    stringBuilder2.append(isa);
                    fpw.print(stringBuilder2.toString());
                }
                fpw.println();
                stringBuilder = new StringBuilder();
                stringBuilder.append(LOADING_PACKAGE_CHAR);
                stringBuilder.append(String.join(SPLIT_CHAR, dexUseInfo.mLoadingPackages));
                fpw.println(stringBuilder.toString());
                fpw.println(dexUseInfo.getClassLoaderContext());
                i = 2;
            }
            i = 2;
        }
        fpw.flush();
    }

    protected void readInternal(Void data) {
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(getFile().openRead()));
            read(in);
        } catch (FileNotFoundException e) {
        } catch (IOException e2) {
            Slog.w(TAG, "Failed to parse package dex usage.", e2);
        } catch (Throwable th) {
            IoUtils.closeQuietly(in);
        }
        IoUtils.closeQuietly(in);
    }

    void read(Reader reader) throws IOException {
        HashMap data = new HashMap();
        BufferedReader in = new BufferedReader(reader);
        String versionLine = in.readLine();
        StringBuilder stringBuilder;
        if (versionLine == null) {
            throw new IllegalStateException("No version line found.");
        } else if (versionLine.startsWith(PACKAGE_DEX_USAGE_VERSION_HEADER)) {
            int version = Integer.parseInt(versionLine.substring(PACKAGE_DEX_USAGE_VERSION_HEADER.length()));
            if (isSupportedVersion(version)) {
                Set<String> supportedIsas = new HashSet();
                int i = 0;
                for (String abi : Build.SUPPORTED_ABIS) {
                    supportedIsas.add(VMRuntime.getInstructionSet(abi));
                }
                PackageUseInfo currentPackageData = null;
                String currentPackage = null;
                while (true) {
                    String readLine = in.readLine();
                    String line = readLine;
                    if (readLine != null) {
                        String currentPackage2;
                        Reader reader2;
                        StringBuilder stringBuilder2;
                        if (!line.startsWith(DEX_LINE_CHAR)) {
                            currentPackage2 = currentPackage;
                            if (!line.startsWith(CODE_PATH_LINE_CHAR)) {
                                int i2;
                                if (version >= 2) {
                                    currentPackage = line;
                                    currentPackageData = new PackageUseInfo();
                                    i2 = 0;
                                } else {
                                    String[] elems = line.split(SPLIT_CHAR);
                                    if (elems.length == 2) {
                                        i2 = 0;
                                        currentPackage = elems[0];
                                        currentPackageData = new PackageUseInfo();
                                        currentPackageData.mUsedByOtherAppsBeforeUpgrade = readBoolean(elems[1]);
                                    } else {
                                        stringBuilder2 = new StringBuilder();
                                        stringBuilder2.append("Invalid PackageDexUsage line: ");
                                        stringBuilder2.append(line);
                                        throw new IllegalStateException(stringBuilder2.toString());
                                    }
                                }
                                data.put(currentPackage, currentPackageData);
                                i = i2;
                                reader2 = reader;
                            } else if (version >= 2) {
                                currentPackageData.mCodePathsUsedByOtherApps.put(line.substring(CODE_PATH_LINE_CHAR.length()), maybeReadLoadingPackages(in, version));
                            } else {
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("Unexpected code path line when parsing PackageDexUseData: ");
                                stringBuilder.append(line);
                                throw new IllegalArgumentException(stringBuilder.toString());
                            }
                        } else if (currentPackage != null) {
                            readLine = line.substring(DEX_LINE_CHAR.length());
                            line = in.readLine();
                            if (line != null) {
                                String[] elems2 = line.split(SPLIT_CHAR);
                                String[] elems3;
                                if (elems2.length >= 3) {
                                    int ownerUserId;
                                    Set<String> loadingPackages = maybeReadLoadingPackages(in, version);
                                    String classLoaderContext = maybeReadClassLoaderContext(in, version);
                                    int ownerUserId2 = Integer.parseInt(elems2[i]);
                                    boolean isUsedByOtherApps = readBoolean(elems2[1]);
                                    currentPackage2 = currentPackage;
                                    DexUseInfo dexUseInfo = new DexUseInfo(isUsedByOtherApps, ownerUserId2, classLoaderContext, null);
                                    dexUseInfo.mLoadingPackages.addAll(loadingPackages);
                                    int i3 = 2;
                                    while (true) {
                                        currentPackage = i3;
                                        boolean isUsedByOtherApps2 = isUsedByOtherApps;
                                        if (currentPackage >= elems2.length) {
                                            break;
                                        }
                                        Set<String> loadingPackages2;
                                        String isa = elems2[currentPackage];
                                        if (supportedIsas.contains(isa)) {
                                            loadingPackages2 = loadingPackages;
                                            ownerUserId = ownerUserId2;
                                            dexUseInfo.mLoaderIsas.add(elems2[currentPackage]);
                                            elems3 = elems2;
                                        } else {
                                            loadingPackages2 = loadingPackages;
                                            ownerUserId = ownerUserId2;
                                            String str = TAG;
                                            StringBuilder stringBuilder3 = new StringBuilder();
                                            elems3 = elems2;
                                            stringBuilder3.append("Unsupported ISA when parsing PackageDexUsage: ");
                                            stringBuilder3.append(isa);
                                            Slog.wtf(str, stringBuilder3.toString());
                                        }
                                        i3 = currentPackage + 1;
                                        isUsedByOtherApps = isUsedByOtherApps2;
                                        loadingPackages = loadingPackages2;
                                        ownerUserId2 = ownerUserId;
                                        elems2 = elems3;
                                    }
                                    ownerUserId = ownerUserId2;
                                    elems3 = elems2;
                                    if (supportedIsas.isEmpty() != null) {
                                        currentPackage = TAG;
                                        StringBuilder stringBuilder4 = new StringBuilder();
                                        stringBuilder4.append("Ignore dexPath when parsing PackageDexUsage because of unsupported isas. dexPath=");
                                        stringBuilder4.append(readLine);
                                        Slog.wtf(currentPackage, stringBuilder4.toString());
                                    } else {
                                        currentPackageData.mDexUseInfoMap.put(readLine, dexUseInfo);
                                    }
                                } else {
                                    elems3 = elems2;
                                    stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append("Invalid PackageDexUsage line: ");
                                    stringBuilder2.append(line);
                                    throw new IllegalStateException(stringBuilder2.toString());
                                }
                            }
                            throw new IllegalStateException("Could not find dexUseInfo line");
                        } else {
                            throw new IllegalStateException("Malformed PackageDexUsage file. Expected package line before dex line.");
                        }
                        currentPackage = currentPackage2;
                        reader2 = reader;
                        i = 0;
                    } else {
                        synchronized (this.mPackageUseInfoMap) {
                            this.mPackageUseInfoMap.clear();
                            this.mPackageUseInfoMap.putAll(data);
                        }
                        return;
                    }
                }
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("Unexpected version: ");
            stringBuilder.append(version);
            throw new IllegalStateException(stringBuilder.toString());
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid version line: ");
            stringBuilder.append(versionLine);
            throw new IllegalStateException(stringBuilder.toString());
        }
    }

    private String maybeReadClassLoaderContext(BufferedReader in, int version) throws IOException {
        String context = null;
        if (version >= 2) {
            context = in.readLine();
            if (context == null) {
                throw new IllegalStateException("Could not find the classLoaderContext line.");
            }
        }
        return context == null ? UNKNOWN_CLASS_LOADER_CONTEXT : context;
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
        return !owningPackage.equals(loadingPackage) && loadingPackages.add(loadingPackage);
    }

    private boolean isSupportedVersion(int version) {
        return version == 1 || version == 2;
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

    /* JADX WARNING: Missing block: B:21:0x0052, code:
            return r2;
     */
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
            if (packageUseInfo.mDexUseInfoMap.isEmpty() && !packageUseInfo.isAnyCodePathUsedByOtherApps()) {
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
        PackageUseInfo packageUseInfo;
        synchronized (this.mPackageUseInfoMap) {
            PackageUseInfo useInfo = (PackageUseInfo) this.mPackageUseInfoMap.get(packageName);
            packageUseInfo = useInfo == null ? null : new PackageUseInfo(useInfo);
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
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Unknown bool encoding: ");
        stringBuilder.append(bool);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public String dump() {
        StringWriter sw = new StringWriter();
        write(sw);
        return sw.toString();
    }
}
