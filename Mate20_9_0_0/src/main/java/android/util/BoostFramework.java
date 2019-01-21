package android.util;

import android.os.Environment;
import android.view.MotionEvent;
import dalvik.system.PathClassLoader;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class BoostFramework {
    private static final String PERFORMANCE_CLASS = "com.qualcomm.qti.Performance";
    private static final String PERFORMANCE_JAR = "/system/framework/QPerformance.jar";
    private static final String PERFORMANCE_JAR_VENDOR = "/system/vendor/framework/QPerformance.jar";
    private static final String TAG = "BoostFramework";
    private static Method mAcquireFunc = null;
    private static Method mAcquireTouchFunc = null;
    private static Constructor<Class> mConstructor = null;
    private static Method mIOPStart = null;
    private static Method mIOPStop = null;
    private static boolean mIsLoaded = false;
    private static Method mReleaseFunc = null;
    private Object mPerf = null;

    private boolean checkExistinSys(String path) {
        File rootdir = Environment.getRootDirectory();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(rootdir.getPath());
        stringBuilder.append("/framework");
        File fileSys = new File(stringBuilder.toString(), path);
        String str = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(fileSys.getAbsolutePath());
        stringBuilder2.append(" be checked ! ");
        Log.v(str, stringBuilder2.toString());
        if (fileSys.exists()) {
            return true;
        }
        return false;
    }

    public BoostFramework() {
        if (!mIsLoaded) {
            try {
                PathClassLoader perfClassLoader;
                if (checkExistinSys("QPerformance.jar")) {
                    perfClassLoader = new PathClassLoader(PERFORMANCE_JAR, ClassLoader.getSystemClassLoader());
                } else {
                    perfClassLoader = new PathClassLoader(PERFORMANCE_JAR_VENDOR, ClassLoader.getSystemClassLoader());
                }
                Class perfClass = perfClassLoader.loadClass(PERFORMANCE_CLASS);
                mConstructor = perfClass.getConstructor(new Class[0]);
                mAcquireFunc = perfClass.getDeclaredMethod("perfLockAcquire", new Class[]{Integer.TYPE, int[].class});
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("mAcquireFunc method = ");
                stringBuilder.append(mAcquireFunc);
                Log.v(str, stringBuilder.toString());
                mReleaseFunc = perfClass.getDeclaredMethod("perfLockRelease", new Class[0]);
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("mReleaseFunc method = ");
                stringBuilder.append(mReleaseFunc);
                Log.v(str, stringBuilder.toString());
                mAcquireTouchFunc = perfClass.getDeclaredMethod("perfLockAcquireTouch", new Class[]{MotionEvent.class, DisplayMetrics.class, Integer.TYPE, int[].class});
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("mAcquireTouchFunc method = ");
                stringBuilder.append(mAcquireTouchFunc);
                Log.v(str, stringBuilder.toString());
                mIOPStart = perfClass.getDeclaredMethod("perfIOPrefetchStart", new Class[]{Integer.TYPE, String.class});
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("mIOPStart method = ");
                stringBuilder2.append(mIOPStart);
                Log.v(str2, stringBuilder2.toString());
                mIOPStop = perfClass.getDeclaredMethod("perfIOPrefetchStop", new Class[0]);
                str2 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("mIOPStop method = ");
                stringBuilder2.append(mIOPStop);
                Log.v(str2, stringBuilder2.toString());
                mIsLoaded = true;
            } catch (Exception e) {
                String str3 = TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("BoostFramework() : Exception_1 = ");
                stringBuilder3.append(e);
                Log.e(str3, stringBuilder3.toString());
            }
        }
        try {
            if (mConstructor != null) {
                this.mPerf = mConstructor.newInstance(new Object[0]);
            }
        } catch (Exception e2) {
            String str4 = TAG;
            StringBuilder stringBuilder4 = new StringBuilder();
            stringBuilder4.append("BoostFramework() : Exception_2 = ");
            stringBuilder4.append(e2);
            Log.e(str4, stringBuilder4.toString());
        }
        String str5 = TAG;
        StringBuilder stringBuilder5 = new StringBuilder();
        stringBuilder5.append("BoostFramework() : mPerf = ");
        stringBuilder5.append(this.mPerf);
        Log.v(str5, stringBuilder5.toString());
    }

    public int perfLockAcquire(int duration, int... list) {
        try {
            return ((Integer) mAcquireFunc.invoke(this.mPerf, new Object[]{Integer.valueOf(duration), list})).intValue();
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Exception ");
            stringBuilder.append(e);
            Log.e(str, stringBuilder.toString());
            return -1;
        }
    }

    public int perfLockRelease() {
        try {
            return ((Integer) mReleaseFunc.invoke(this.mPerf, new Object[0])).intValue();
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Exception ");
            stringBuilder.append(e);
            Log.e(str, stringBuilder.toString());
            return -1;
        }
    }

    public int perfLockAcquireTouch(MotionEvent ev, DisplayMetrics metrics, int duration, int... list) {
        try {
            return ((Integer) mAcquireTouchFunc.invoke(this.mPerf, new Object[]{ev, metrics, Integer.valueOf(duration), list})).intValue();
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Exception ");
            stringBuilder.append(e);
            Log.e(str, stringBuilder.toString());
            return -1;
        }
    }

    public int perfIOPrefetchStart(int pid, String pkg_name) {
        try {
            return ((Integer) mIOPStart.invoke(this.mPerf, new Object[]{Integer.valueOf(pid), pkg_name})).intValue();
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Exception ");
            stringBuilder.append(e);
            Log.e(str, stringBuilder.toString());
            return -1;
        }
    }

    public int perfIOPrefetchStop() {
        try {
            return ((Integer) mIOPStop.invoke(this.mPerf, new Object[0])).intValue();
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Exception ");
            stringBuilder.append(e);
            Log.e(str, stringBuilder.toString());
            return -1;
        }
    }
}
