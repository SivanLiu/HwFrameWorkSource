package android.filterfw.core;

import android.util.Log;
import dalvik.system.PathClassLoader;
import java.lang.reflect.Constructor;
import java.util.HashSet;
import java.util.Iterator;

public class FilterFactory {
    private static final String TAG = "FilterFactory";
    private static Object mClassLoaderGuard = new Object();
    private static ClassLoader mCurrentClassLoader = Thread.currentThread().getContextClassLoader();
    private static HashSet<String> mLibraries = new HashSet();
    private static boolean mLogVerbose = Log.isLoggable(TAG, 2);
    private static FilterFactory mSharedFactory;
    private HashSet<String> mPackages = new HashSet();

    public static FilterFactory sharedFactory() {
        if (mSharedFactory == null) {
            mSharedFactory = new FilterFactory();
        }
        return mSharedFactory;
    }

    /* JADX WARNING: Missing block: B:12:0x0031, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static void addFilterLibrary(String libraryPath) {
        if (mLogVerbose) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Adding filter library ");
            stringBuilder.append(libraryPath);
            Log.v(str, stringBuilder.toString());
        }
        synchronized (mClassLoaderGuard) {
            if (!mLibraries.contains(libraryPath)) {
                mLibraries.add(libraryPath);
                mCurrentClassLoader = new PathClassLoader(libraryPath, mCurrentClassLoader);
            } else if (mLogVerbose) {
                Log.v(TAG, "Library already added");
            }
        }
    }

    public void addPackage(String packageName) {
        if (mLogVerbose) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Adding package ");
            stringBuilder.append(packageName);
            Log.v(str, stringBuilder.toString());
        }
        this.mPackages.add(packageName);
    }

    public Filter createFilterByClassName(String className, String filterName) {
        if (mLogVerbose) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Looking up class ");
            stringBuilder.append(className);
            Log.v(str, stringBuilder.toString());
        }
        Class filterClass = null;
        Iterator it = this.mPackages.iterator();
        while (it.hasNext()) {
            String packageName = (String) it.next();
            try {
                if (mLogVerbose) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Trying ");
                    stringBuilder2.append(packageName);
                    stringBuilder2.append(".");
                    stringBuilder2.append(className);
                    Log.v(str2, stringBuilder2.toString());
                }
                synchronized (mClassLoaderGuard) {
                    ClassLoader classLoader = mCurrentClassLoader;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append(packageName);
                    stringBuilder3.append(".");
                    stringBuilder3.append(className);
                    filterClass = classLoader.loadClass(stringBuilder3.toString());
                }
                if (filterClass != null) {
                    break;
                }
            } catch (ClassNotFoundException e) {
            }
        }
        if (filterClass != null) {
            return createFilterByClass(filterClass, filterName);
        }
        StringBuilder stringBuilder4 = new StringBuilder();
        stringBuilder4.append("Unknown filter class '");
        stringBuilder4.append(className);
        stringBuilder4.append("'!");
        throw new IllegalArgumentException(stringBuilder4.toString());
    }

    public Filter createFilterByClass(Class filterClass, String filterName) {
        try {
            filterClass.asSubclass(Filter.class);
            Filter filter = null;
            Constructor filterConstructor = null;
            StringBuilder stringBuilder;
            try {
                try {
                    filter = (Filter) filterClass.getConstructor(new Class[]{String.class}).newInstance(new Object[]{filterName});
                } catch (Throwable th) {
                }
                if (filter != null) {
                    return filter;
                }
                stringBuilder = new StringBuilder();
                stringBuilder.append("Could not construct the filter '");
                stringBuilder.append(filterName);
                stringBuilder.append("'!");
                throw new IllegalArgumentException(stringBuilder.toString());
            } catch (NoSuchMethodException e) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("The filter class '");
                stringBuilder.append(filterClass);
                stringBuilder.append("' does not have a constructor of the form <init>(String name)!");
                throw new IllegalArgumentException(stringBuilder.toString());
            }
        } catch (ClassCastException e2) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Attempting to allocate class '");
            stringBuilder2.append(filterClass);
            stringBuilder2.append("' which is not a subclass of Filter!");
            throw new IllegalArgumentException(stringBuilder2.toString());
        }
    }
}
