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
        String sharedLibrariesClassPath = encodeClasspath(sharedLibraries);
        String baseApkContextClassLoader = encodeClassLoader(sharedLibrariesClassPath, info.classLoaderName);
        if (info.getSplitCodePaths() == null) {
            return new String[]{baseApkContextClassLoader};
        }
        String[] splitRelativeCodePaths = getSplitRelativeCodePaths(info);
        String sharedLibrariesAndBaseClassPath = encodeClasspath(sharedLibrariesClassPath, new File(info.getBaseCodePath()).getName());
        String[] classLoaderContexts = new String[(splitRelativeCodePaths.length + 1)];
        if (!pathsWithCode[0]) {
            baseApkContextClassLoader = null;
        }
        classLoaderContexts[0] = baseApkContextClassLoader;
        int i;
        if (!info.requestsIsolatedSplitLoading() || info.splitDependencies == null) {
            String classpath = sharedLibrariesAndBaseClassPath;
            for (i = 1; i < classLoaderContexts.length; i++) {
                classLoaderContexts[i] = pathsWithCode[i] ? encodeClassLoader(classpath, info.classLoaderName) : null;
                classpath = encodeClasspath(classpath, splitRelativeCodePaths[i - 1]);
            }
        } else {
            String[] splitClassLoaderEncodingCache = new String[splitRelativeCodePaths.length];
            for (i = 0; i < splitRelativeCodePaths.length; i++) {
                splitClassLoaderEncodingCache[i] = encodeClassLoader(splitRelativeCodePaths[i], info.splitClassLoaderNames[i]);
            }
            String splitDependencyOnBase = encodeClassLoader(sharedLibrariesAndBaseClassPath, info.classLoaderName);
            SparseArray<int[]> splitDependencies = info.splitDependencies;
            for (i = 1; i < splitDependencies.size(); i++) {
                int splitIndex = splitDependencies.keyAt(i);
                if (pathsWithCode[splitIndex]) {
                    getParentDependencies(splitIndex, splitClassLoaderEncodingCache, splitDependencies, classLoaderContexts, splitDependencyOnBase);
                }
            }
            for (i = 1; i < classLoaderContexts.length; i++) {
                String splitClassLoader = encodeClassLoader("", info.splitClassLoaderNames[i - 1]);
                if (pathsWithCode[i]) {
                    if (classLoaderContexts[i] != null) {
                        splitClassLoader = encodeClassLoaderChain(splitClassLoader, classLoaderContexts[i]);
                    }
                    classLoaderContexts[i] = splitClassLoader;
                } else {
                    classLoaderContexts[i] = null;
                }
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
        String splitContext;
        int parent = ((int[]) splitDependencies.get(index))[0];
        String parentDependencies = getParentDependencies(parent, splitClassLoaderEncodingCache, splitDependencies, classLoaderContexts, splitDependencyOnBase);
        if (parent == 0) {
            splitContext = parentDependencies;
        } else {
            splitContext = encodeClassLoaderChain(splitClassLoaderEncodingCache[parent - 1], parentDependencies);
        }
        classLoaderContexts[index] = splitContext;
        return splitContext;
    }

    private static String encodeClasspath(String[] classpathElements) {
        if (classpathElements == null || classpathElements.length == 0) {
            return "";
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
        return classpath.isEmpty() ? newElement : classpath + ":" + newElement;
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
            Slog.wtf(TAG, "Unsupported classLoaderName: " + classLoaderName);
        }
        return classLoaderDexoptEncoding + "[" + classpath + "]";
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
        return cl1 + ";" + cl2;
    }

    static String[] processContextForDexLoad(List<String> classLoadersNames, List<String> classPaths) {
        if (classLoadersNames.size() != classPaths.size()) {
            throw new IllegalArgumentException("The size of the class loader names and the dex paths do not match.");
        } else if (classLoadersNames.isEmpty()) {
            throw new IllegalArgumentException("Empty classLoadersNames");
        } else {
            int i;
            String parentContext = "";
            for (i = 1; i < classLoadersNames.size(); i++) {
                if (!ClassLoaderFactory.isValidClassLoaderName((String) classLoadersNames.get(i))) {
                    return null;
                }
                parentContext = encodeClassLoaderChain(parentContext, encodeClassLoader(encodeClasspath(((String) classPaths.get(i)).split(File.pathSeparator)), (String) classLoadersNames.get(i)));
            }
            String loadingClassLoader = (String) classLoadersNames.get(0);
            if (!ClassLoaderFactory.isValidClassLoaderName(loadingClassLoader)) {
                return null;
            }
            String[] loadedDexPaths = ((String) classPaths.get(0)).split(File.pathSeparator);
            String[] loadedDexPathsContext = new String[loadedDexPaths.length];
            String currentLoadedDexPathClasspath = "";
            for (i = 0; i < loadedDexPaths.length; i++) {
                String dexPath = loadedDexPaths[i];
                loadedDexPathsContext[i] = encodeClassLoaderChain(encodeClassLoader(currentLoadedDexPathClasspath, loadingClassLoader), parentContext);
                currentLoadedDexPathClasspath = encodeClasspath(currentLoadedDexPathClasspath, dexPath);
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
                Slog.wtf(TAG, "Split paths have different base paths: " + basePath + " and " + baseCodePath);
            }
        }
        return splitRelativeCodePaths;
    }
}
