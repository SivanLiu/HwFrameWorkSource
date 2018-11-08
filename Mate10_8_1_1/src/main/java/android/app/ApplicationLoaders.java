package android.app;

import android.common.HwFrameworkFactory;
import android.os.Build.VERSION;
import android.os.Trace;
import android.util.ArrayMap;
import com.android.internal.os.ClassLoaderFactory;
import dalvik.system.PathClassLoader;

public class ApplicationLoaders {
    private static final ApplicationLoaders gApplicationLoaders = new ApplicationLoaders();
    private final ArrayMap<String, ClassLoader> mLoaders = new ArrayMap();

    private static native void setupVulkanLayerPath(ClassLoader classLoader, String str);

    public static ApplicationLoaders getDefault() {
        return gApplicationLoaders;
    }

    ClassLoader getClassLoader(String zip, int targetSdkVersion, boolean isBundled, String librarySearchPath, String libraryPermittedPath, ClassLoader parent, String classLoaderName) {
        return getClassLoader(zip, targetSdkVersion, isBundled, librarySearchPath, libraryPermittedPath, parent, zip, classLoaderName);
    }

    private ClassLoader getClassLoader(String zip, int targetSdkVersion, boolean isBundled, String librarySearchPath, String libraryPermittedPath, ClassLoader parent, String cacheKey, String classLoaderName) {
        ClassLoader baseParent = ClassLoader.getSystemClassLoader().getParent();
        synchronized (this.mLoaders) {
            if (parent == null) {
                ClassLoader featureParent = HwFrameworkFactory.getHwFLClassLoaderParent(zip);
                if (featureParent != null) {
                    baseParent = featureParent;
                }
                parent = baseParent;
            }
            ClassLoader loader;
            if (parent == baseParent) {
                loader = (ClassLoader) this.mLoaders.get(cacheKey);
                if (loader != null) {
                    return loader;
                }
                Trace.traceBegin(64, zip);
                ClassLoader classloader = ClassLoaderFactory.createClassLoader(zip, librarySearchPath, libraryPermittedPath, parent, targetSdkVersion, isBundled, classLoaderName);
                Trace.traceEnd(64);
                Trace.traceBegin(64, "setupVulkanLayerPath");
                setupVulkanLayerPath(classloader, librarySearchPath);
                Trace.traceEnd(64);
                this.mLoaders.put(cacheKey, classloader);
                return classloader;
            }
            Trace.traceBegin(64, zip);
            loader = ClassLoaderFactory.createClassLoader(zip, null, parent, classLoaderName);
            Trace.traceEnd(64);
            return loader;
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
}
