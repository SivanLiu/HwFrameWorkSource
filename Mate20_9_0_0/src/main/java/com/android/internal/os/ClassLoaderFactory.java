package com.android.internal.os;

import android.os.Trace;
import dalvik.system.DelegateLastClassLoader;
import dalvik.system.DexClassLoader;
import dalvik.system.PathClassLoader;

public class ClassLoaderFactory {
    private static final String DELEGATE_LAST_CLASS_LOADER_NAME = DelegateLastClassLoader.class.getName();
    private static final String DEX_CLASS_LOADER_NAME = DexClassLoader.class.getName();
    private static final String PATH_CLASS_LOADER_NAME = PathClassLoader.class.getName();

    private static native String createClassloaderNamespace(ClassLoader classLoader, int i, String str, String str2, boolean z, boolean z2);

    private ClassLoaderFactory() {
    }

    public static boolean isValidClassLoaderName(String name) {
        return name != null && (isPathClassLoaderName(name) || isDelegateLastClassLoaderName(name));
    }

    public static boolean isPathClassLoaderName(String name) {
        return name == null || PATH_CLASS_LOADER_NAME.equals(name) || DEX_CLASS_LOADER_NAME.equals(name);
    }

    public static boolean isDelegateLastClassLoaderName(String name) {
        return DELEGATE_LAST_CLASS_LOADER_NAME.equals(name);
    }

    public static ClassLoader createClassLoader(String dexPath, String librarySearchPath, ClassLoader parent, String classloaderName) {
        if (isPathClassLoaderName(classloaderName)) {
            return new PathClassLoader(dexPath, librarySearchPath, parent);
        }
        if (isDelegateLastClassLoaderName(classloaderName)) {
            return new DelegateLastClassLoader(dexPath, librarySearchPath, parent);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Invalid classLoaderName: ");
        stringBuilder.append(classloaderName);
        throw new AssertionError(stringBuilder.toString());
    }

    public static ClassLoader createClassLoader(String dexPath, String librarySearchPath, String libraryPermittedPath, ClassLoader parent, int targetSdkVersion, boolean isNamespaceShared, String classloaderName) {
        String str = dexPath;
        String str2 = librarySearchPath;
        ClassLoader classLoader = createClassLoader(str, str2, parent, classloaderName);
        boolean isForVendor = false;
        for (String path : str.split(":")) {
            if (path.startsWith("/vendor/")) {
                isForVendor = true;
                break;
            }
        }
        boolean isForVendor2 = isForVendor;
        Trace.traceBegin(64, "createClassloaderNamespace");
        String errorMessage = createClassloaderNamespace(classLoader, targetSdkVersion, str2, libraryPermittedPath, isNamespaceShared, isForVendor2);
        Trace.traceEnd(64);
        if (errorMessage == null) {
            return classLoader;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Unable to create namespace for the classloader ");
        stringBuilder.append(classLoader);
        stringBuilder.append(": ");
        stringBuilder.append(errorMessage);
        throw new UnsatisfiedLinkError(stringBuilder.toString());
    }
}
