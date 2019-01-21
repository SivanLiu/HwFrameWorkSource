package android.os;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.provider.Settings.Global;
import android.provider.SettingsStringUtil;
import android.util.Log;
import dalvik.system.VMRuntime;
import java.io.File;

public class GraphicsEnvironment {
    private static final boolean DEBUG = false;
    private static final String PROPERTY_GFX_DRIVER = "ro.gfx.driver.0";
    private static final String TAG = "GraphicsEnvironment";
    private static final GraphicsEnvironment sInstance = new GraphicsEnvironment();
    private ClassLoader mClassLoader;
    private String mDebugLayerPath;
    private String mLayerPath;

    private static native void setDebugLayers(String str);

    private static native void setDriverPath(String str);

    private static native void setLayerPaths(ClassLoader classLoader, String str);

    public static GraphicsEnvironment getInstance() {
        return sInstance;
    }

    public void setup(Context context) {
        setupGpuLayers(context);
        chooseDriver(context);
    }

    private static boolean isDebuggable(Context context) {
        return (context.getApplicationInfo().flags & 2) > 0;
    }

    public void setLayerPaths(ClassLoader classLoader, String layerPath, String debugLayerPath) {
        this.mClassLoader = classLoader;
        this.mLayerPath = layerPath;
        this.mDebugLayerPath = debugLayerPath;
    }

    private void setupGpuLayers(Context context) {
        String layerPaths = "";
        if (isDebuggable(context) && Global.getInt(context.getContentResolver(), Global.ENABLE_GPU_DEBUG_LAYERS, 0) != 0) {
            String gpuDebugApp = Global.getString(context.getContentResolver(), Global.GPU_DEBUG_APP);
            String packageName = context.getPackageName();
            if (!(gpuDebugApp == null || packageName == null || gpuDebugApp.isEmpty() || packageName.isEmpty() || !gpuDebugApp.equals(packageName))) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("GPU debug layers enabled for ");
                stringBuilder.append(packageName);
                Log.i(str, stringBuilder.toString());
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append(this.mDebugLayerPath);
                stringBuilder2.append(SettingsStringUtil.DELIMITER);
                layerPaths = stringBuilder2.toString();
                str = Global.getString(context.getContentResolver(), Global.GPU_DEBUG_LAYERS);
                String str2 = TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Debug layer list: ");
                stringBuilder3.append(str);
                Log.i(str2, stringBuilder3.toString());
                if (!(str == null || str.isEmpty())) {
                    setDebugLayers(str);
                }
            }
        }
        StringBuilder stringBuilder4 = new StringBuilder();
        stringBuilder4.append(layerPaths);
        stringBuilder4.append(this.mLayerPath);
        setLayerPaths(this.mClassLoader, stringBuilder4.toString());
    }

    private static void chooseDriver(Context context) {
        String driverPackageName = SystemProperties.get(PROPERTY_GFX_DRIVER);
        if (driverPackageName != null && !driverPackageName.isEmpty()) {
            ApplicationInfo ai = context.getApplicationInfo();
            if (!ai.isPrivilegedApp() && (!ai.isSystemApp() || ai.isUpdatedSystemApp())) {
                String abi;
                StringBuilder sb;
                try {
                    ApplicationInfo driverInfo = context.getPackageManager().getApplicationInfo(driverPackageName, 1048576);
                    abi = chooseAbi(driverInfo);
                    if (abi != null) {
                        if (driverInfo.targetSdkVersion < 26) {
                            Log.w(TAG, "updated driver package is not known to be compatible with O");
                            return;
                        }
                        sb = new StringBuilder();
                        sb.append(driverInfo.nativeLibraryDir);
                        sb.append(File.pathSeparator);
                        sb.append(driverInfo.sourceDir);
                        sb.append("!/lib/");
                        sb.append(abi);
                        setDriverPath(sb.toString());
                    }
                } catch (NameNotFoundException e) {
                    abi = TAG;
                    sb = new StringBuilder();
                    sb.append("driver package '");
                    sb.append(driverPackageName);
                    sb.append("' not installed");
                    Log.w(abi, sb.toString());
                }
            }
        }
    }

    public static void earlyInitEGL() {
        new Thread(-$$Lambda$GraphicsEnvironment$U4RqBlx5-Js31-71IFOgvpvoAFg.INSTANCE, "EGL Init").start();
    }

    private static String chooseAbi(ApplicationInfo ai) {
        String isa = VMRuntime.getCurrentInstructionSet();
        if (ai.primaryCpuAbi != null && isa.equals(VMRuntime.getInstructionSet(ai.primaryCpuAbi))) {
            return ai.primaryCpuAbi;
        }
        if (ai.secondaryCpuAbi == null || !isa.equals(VMRuntime.getInstructionSet(ai.secondaryCpuAbi))) {
            return null;
        }
        return ai.secondaryCpuAbi;
    }
}
