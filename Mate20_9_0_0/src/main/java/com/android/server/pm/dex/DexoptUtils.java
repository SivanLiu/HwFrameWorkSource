package com.android.server.pm.dex;

import android.content.pm.ApplicationInfo;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.os.ClassLoaderFactory;
import com.android.server.pm.PackageDexOptimizer;
import java.io.File;
import java.util.List;

public final class DexoptUtils {
    private static final String TAG = "DexoptUtils";

    private DexoptUtils() {
    }

    public static String[] getClassLoaderContexts(ApplicationInfo info, String[] sharedLibraries, boolean[] pathsWithCode) {
        ApplicationInfo applicationInfo = info;
        String sharedLibrariesClassPath = encodeClasspath(sharedLibraries);
        String baseApkContextClassLoader = encodeClassLoader(sharedLibrariesClassPath, applicationInfo.classLoaderName);
        int i = 0;
        int i2 = 1;
        if (applicationInfo.getSplitCodePaths() == null) {
            return new String[]{baseApkContextClassLoader};
        }
        String[] splitRelativeCodePaths = getSplitRelativeCodePaths(applicationInfo);
        String sharedLibrariesAndBaseClassPath = encodeClasspath(sharedLibrariesClassPath, new File(applicationInfo.getBaseCodePath()).getName());
        String[] classLoaderContexts = new String[(splitRelativeCodePaths.length + 1)];
        classLoaderContexts[0] = pathsWithCode[0] ? baseApkContextClassLoader : null;
        String classpath;
        if (!applicationInfo.requestsIsolatedSplitLoading() || applicationInfo.splitDependencies == null) {
            classpath = sharedLibrariesAndBaseClassPath;
            while (i2 < classLoaderContexts.length) {
                classLoaderContexts[i2] = pathsWithCode[i2] ? encodeClassLoader(classpath, applicationInfo.classLoaderName) : null;
                classpath = encodeClasspath(classpath, splitRelativeCodePaths[i2 - 1]);
                i2++;
            }
        } else {
            String[] splitClassLoaderEncodingCache = new String[splitRelativeCodePaths.length];
            while (i < splitRelativeCodePaths.length) {
                splitClassLoaderEncodingCache[i] = encodeClassLoader(splitRelativeCodePaths[i], applicationInfo.splitClassLoaderNames[i]);
                i++;
            }
            classpath = encodeClassLoader(sharedLibrariesAndBaseClassPath, applicationInfo.classLoaderName);
            SparseArray<int[]> splitDependencies = applicationInfo.splitDependencies;
            for (int i3 = 1; i3 < splitDependencies.size(); i3++) {
                int splitIndex = splitDependencies.keyAt(i3);
                if (pathsWithCode[splitIndex]) {
                    getParentDependencies(splitIndex, splitClassLoaderEncodingCache, splitDependencies, classLoaderContexts, classpath);
                }
            }
            while (i2 < classLoaderContexts.length) {
                String splitClassLoader = encodeClassLoader(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, applicationInfo.splitClassLoaderNames[i2 - 1]);
                if (pathsWithCode[i2]) {
                    classLoaderContexts[i2] = classLoaderContexts[i2] == null ? splitClassLoader : encodeClassLoaderChain(splitClassLoader, classLoaderContexts[i2]);
                } else {
                    classLoaderContexts[i2] = null;
                }
                i2++;
            }
        }
        return classLoaderContexts;
    }

    private static String getParentDependencies(int index, String[] splitClassLoaderEncodingCache, SparseArray<int[]> splitDependencies, String[] classLoaderContexts, String splitDependencyOnBase) {
        if (index == 0) {
            return splitDependencyOnBase;
        }
        if (classLoaderContexts[index] != null) {
            return classLoaderContexts[index];
        }
        int parent = ((int[]) splitDependencies.get(index))[0];
        String parentDependencies = getParentDependencies(parent, splitClassLoaderEncodingCache, splitDependencies, classLoaderContexts, splitDependencyOnBase);
        String splitContext = parent == 0 ? parentDependencies : encodeClassLoaderChain(splitClassLoaderEncodingCache[parent - 1], parentDependencies);
        classLoaderContexts[index] = splitContext;
        return splitContext;
    }

    private static String encodeClasspath(String[] classpathElements) {
        if (classpathElements == null || classpathElements.length == 0) {
            return BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        }
        StringBuilder sb = new StringBuilder();
        for (String element : classpathElements) {
            if (sb.length() != 0) {
                sb.append(":");
            }
            sb.append(element);
        }
        return sb.toString();
    }

    private static String encodeClasspath(String classpath, String newElement) {
        if (classpath.isEmpty()) {
            return newElement;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(classpath);
        stringBuilder.append(":");
        stringBuilder.append(newElement);
        return stringBuilder.toString();
    }

    static String encodeClassLoader(String classpath, String classLoaderName) {
        if (classpath.equals(PackageDexOptimizer.SKIP_SHARED_LIBRARY_CHECK)) {
            return classpath;
        }
        String classLoaderDexoptEncoding = classLoaderName;
        if (ClassLoaderFactory.isPathClassLoaderName(classLoaderName)) {
            classLoaderDexoptEncoding = "PCL";
        } else if (ClassLoaderFactory.isDelegateLastClassLoaderName(classLoaderName)) {
            classLoaderDexoptEncoding = "DLC";
        } else {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unsupported classLoaderName: ");
            stringBuilder.append(classLoaderName);
            Slog.wtf(str, stringBuilder.toString());
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(classLoaderDexoptEncoding);
        stringBuilder2.append("[");
        stringBuilder2.append(classpath);
        stringBuilder2.append("]");
        return stringBuilder2.toString();
    }

    static String encodeClassLoaderChain(String cl1, String cl2) {
        if (cl1.equals(PackageDexOptimizer.SKIP_SHARED_LIBRARY_CHECK) || cl2.equals(PackageDexOptimizer.SKIP_SHARED_LIBRARY_CHECK)) {
            return PackageDexOptimizer.SKIP_SHARED_LIBRARY_CHECK;
        }
        if (cl1.isEmpty()) {
            return cl2;
        }
        if (cl2.isEmpty()) {
            return cl1;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(cl1);
        stringBuilder.append(";");
        stringBuilder.append(cl2);
        return stringBuilder.toString();
    }

    static String[] processContextForDexLoad(List<String> classLoadersNames, List<String> classPaths) {
        if (classLoadersNames.size() != classPaths.size()) {
            throw new IllegalArgumentException("The size of the class loader names and the dex paths do not match.");
        } else if (classLoadersNames.isEmpty()) {
            throw new IllegalArgumentException("Empty classLoadersNames");
        } else {
            int i;
            String parentContext = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
            for (i = 1; i < classLoadersNames.size(); i++) {
                if (!ClassLoaderFactory.isValidClassLoaderName((String) classLoadersNames.get(i))) {
                    return null;
                }
                parentContext = encodeClassLoaderChain(parentContext, encodeClassLoader(encodeClasspath(((String) classPaths.get(i)).split(File.pathSeparator)), (String) classLoadersNames.get(i)));
            }
            i = 0;
            String loadingClassLoader = (String) classLoadersNames.get(0);
            if (!ClassLoaderFactory.isValidClassLoaderName(loadingClassLoader)) {
                return null;
            }
            String[] loadedDexPaths = ((String) classPaths.get(0)).split(File.pathSeparator);
            String[] loadedDexPathsContext = new String[loadedDexPaths.length];
            String currentLoadedDexPathClasspath = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
            while (i < loadedDexPaths.length) {
                String dexPath = loadedDexPaths[i];
                loadedDexPathsContext[i] = encodeClassLoaderChain(encodeClassLoader(currentLoadedDexPathClasspath, loadingClassLoader), parentContext);
                currentLoadedDexPathClasspath = encodeClasspath(currentLoadedDexPathClasspath, dexPath);
                i++;
            }
            return loadedDexPathsContext;
        }
    }

    private static String[] getSplitRelativeCodePaths(ApplicationInfo info) {
        String baseCodePath = new File(info.getBaseCodePath()).getParent();
        String[] splitCodePaths = info.getSplitCodePaths();
        String[] splitRelativeCodePaths = new String[splitCodePaths.length];
        for (int i = 0; i < splitCodePaths.length; i++) {
            File pathFile = new File(splitCodePaths[i]);
            splitRelativeCodePaths[i] = pathFile.getName();
            String basePath = pathFile.getParent();
            if (!basePath.equals(baseCodePath)) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Split paths have different base paths: ");
                stringBuilder.append(basePath);
                stringBuilder.append(" and ");
                stringBuilder.append(baseCodePath);
                Slog.wtf(str, stringBuilder.toString());
            }
        }
        return splitRelativeCodePaths;
    }
}
