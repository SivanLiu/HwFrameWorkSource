package android.app;

import android.os.Build.VERSION;
import android.os.GraphicsEnvironment;
import android.os.Trace;
import android.util.ArrayMap;
import com.android.internal.os.ClassLoaderFactory;
import dalvik.system.PathClassLoader;
import java.util.Collection;

public class ApplicationLoaders {
    private static final ApplicationLoaders gApplicationLoaders = new ApplicationLoaders();
    private final ArrayMap<String, ClassLoader> mLoaders = new ArrayMap();

    public static ApplicationLoaders getDefault() {
        return gApplicationLoaders;
    }

    ClassLoader getClassLoader(String zip, int targetSdkVersion, boolean isBundled, String librarySearchPath, String libraryPermittedPath, ClassLoader parent, String classLoaderName) {
        return getClassLoader(zip, targetSdkVersion, isBundled, librarySearchPath, libraryPermittedPath, parent, zip, classLoaderName);
    }

    private ClassLoader getClassLoader(String zip, int targetSdkVersion, boolean isBundled, String librarySearchPath, String libraryPermittedPath, ClassLoader parent, String cacheKey, String classLoaderName) {
        Throwable th;
        String str;
        String str2;
        String str3;
        String str4 = zip;
        String str5 = cacheKey;
        ClassLoader baseParent = ClassLoader.getSystemClassLoader().getParent();
        synchronized (this.mLoaders) {
            ClassLoader parent2 = parent == null ? baseParent : parent;
            ClassLoader loader;
            if (parent2 == baseParent) {
                try {
                    loader = (ClassLoader) this.mLoaders.get(str5);
                    if (loader != null) {
                        return loader;
                    }
                    Trace.traceBegin(64, str4);
                    ClassLoader classloader = ClassLoaderFactory.createClassLoader(str4, librarySearchPath, libraryPermittedPath, parent2, targetSdkVersion, isBundled, classLoaderName);
                    Trace.traceEnd(64);
                    Trace.traceBegin(64, "setLayerPaths");
                    GraphicsEnvironment.getInstance().setLayerPaths(classloader, librarySearchPath, libraryPermittedPath);
                    Trace.traceEnd(64);
                    this.mLoaders.put(str5, classloader);
                    return classloader;
                } catch (Throwable th2) {
                    th = th2;
                    str3 = classLoaderName;
                    throw th;
                }
            }
            str = librarySearchPath;
            str2 = libraryPermittedPath;
            Trace.traceBegin(64, str4);
            try {
                loader = ClassLoaderFactory.createClassLoader(str4, null, parent2, classLoaderName);
                Trace.traceEnd(64);
                return loader;
            } catch (Throwable th3) {
                th = th3;
                throw th;
            }
        }
    }

    public ClassLoader createAndCacheWebViewClassLoader(String packagePath, String libsPath, String cacheKey) {
        return getClassLoader(packagePath, VERSION.SDK_INT, false, libsPath, null, null, cacheKey, null);
    }

    void addPath(ClassLoader classLoader, String dexPath) {
        if (classLoader instanceof PathClassLoader) {
            ((PathClassLoader) classLoader).addDexPath(dexPath);
            return;
        }
        throw new IllegalStateException("class loader is not a PathClassLoader");
    }

    void addNative(ClassLoader classLoader, Collection<String> libPaths) {
        if (classLoader instanceof PathClassLoader) {
            ((PathClassLoader) classLoader).addNativePath(libPaths);
            return;
        }
        throw new IllegalStateException("class loader is not a PathClassLoader");
    }
}
