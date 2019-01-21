package com.huawei.android.manufacture;

import android.os.Binder;
import android.os.UserHandle;
import android.util.Log;

public class ManufactureNativeUtils {
    private static final String TAG = "ManufactureNativeUtils";
    private static final int VERSION_CODE = 100000;
    private static final String VERSION_NAME = "1.000.00";
    private static ManufactureNativeUtils instance;

    private native String native_getApparatusModel(int i);

    private native int native_getBackgroundDebugMode();

    private native String native_getBoundPLMNInfo(int i);

    private native String native_getDeviceInfo(int i);

    private native int native_getFuseState();

    private native int native_getNVBackupResult();

    private native String native_getSIMLockDetail();

    private native String native_getTestResult(int i);

    private native String native_getVersionAndTime(int i);

    private native int native_setBackgroundDebugMode(int i, String str);

    private native int native_setPowerState(String str, String str2);

    private native int native_verifySecbootKey(String str);

    private ManufactureNativeUtils() {
        Log.i(TAG, "ManufactureNativeUtils, VERSION_NAME: 1.000.00 VERSION_CODE: 100000");
    }

    private static ManufactureNativeUtils getInstance() {
        synchronized (ManufactureNativeUtils.class) {
            if (instance == null) {
                try {
                    Log.d(TAG, "load loadLibrary");
                    System.loadLibrary("manufacture_jni");
                    instance = new ManufactureNativeUtils();
                } catch (UnsatisfiedLinkError e) {
                    instance = null;
                    Log.e(TAG, "getInstance, load loadLibrary fail!");
                }
            }
        }
        return instance;
    }

    private static boolean isUidSystem() {
        return UserHandle.getAppId(Binder.getCallingUid()) == 1000 || Binder.getCallingUid() == 0;
    }

    public static int setPowerState(String testScene, String para) throws Exception {
        if (isUidSystem()) {
            ManufactureNativeUtils instance = getInstance();
            if (instance != null) {
                int ret = instance.native_setPowerState(testScene, para);
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("setPowerState, ret = ");
                stringBuilder.append(ret);
                Log.d(str, stringBuilder.toString());
                return ret;
            }
            Log.e(TAG, "setPowerState, instance is null");
            throw new Exception("not support!");
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("disallowed call for uid ");
        stringBuilder2.append(Binder.getCallingUid());
        throw new SecurityException(stringBuilder2.toString());
    }

    public static String getDeviceInfo(int id) throws Exception {
        if (isUidSystem()) {
            String ret = "";
            ManufactureNativeUtils instance = getInstance();
            if (instance != null) {
                ret = instance.native_getDeviceInfo(id);
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getDeviceInfo, ret = ");
                stringBuilder.append(ret);
                Log.d(str, stringBuilder.toString());
                return ret;
            }
            Log.e(TAG, "getDeviceInfo, instance is null");
            throw new Exception("not support!");
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("disallowed call for uid ");
        stringBuilder2.append(Binder.getCallingUid());
        throw new SecurityException(stringBuilder2.toString());
    }

    public static String getTestResult(int testid) throws Exception {
        if (isUidSystem()) {
            String ret = "";
            ManufactureNativeUtils instance = getInstance();
            if (instance != null) {
                ret = instance.native_getTestResult(testid);
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getTestResult, ret = ");
                stringBuilder.append(ret);
                Log.d(str, stringBuilder.toString());
                return ret;
            }
            Log.e(TAG, "getTestResult, instance is null");
            throw new Exception("not support!");
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("disallowed call for uid ");
        stringBuilder2.append(Binder.getCallingUid());
        throw new SecurityException(stringBuilder2.toString());
    }

    public static String getApparatusModel(int apparatusId) throws Exception {
        if (isUidSystem()) {
            String ret = "";
            ManufactureNativeUtils instance = getInstance();
            if (instance != null) {
                ret = instance.native_getApparatusModel(apparatusId);
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getApparatusModel, ret = ");
                stringBuilder.append(ret);
                Log.d(str, stringBuilder.toString());
                return ret;
            }
            Log.e(TAG, "getApparatusModel, instance is null");
            throw new Exception("not support!");
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("disallowed call for uid ");
        stringBuilder2.append(Binder.getCallingUid());
        throw new SecurityException(stringBuilder2.toString());
    }

    public static String getVersionInfo(int id) throws Exception {
        if (isUidSystem()) {
            String ret = "";
            ManufactureNativeUtils instance = getInstance();
            if (instance != null) {
                ret = instance.native_getVersionAndTime(id);
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getVersionInfo, ret = ");
                stringBuilder.append(ret);
                Log.d(str, stringBuilder.toString());
                return ret;
            }
            Log.e(TAG, "getVersionInfo, instance is null");
            throw new Exception("not support!");
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("disallowed call for uid ");
        stringBuilder2.append(Binder.getCallingUid());
        throw new SecurityException(stringBuilder2.toString());
    }

    public static int getNVBackupResult() throws Exception {
        if (isUidSystem()) {
            ManufactureNativeUtils instance = getInstance();
            if (instance != null) {
                int ret = instance.native_getNVBackupResult();
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getNVBackupResult, ret = ");
                stringBuilder.append(ret);
                Log.d(str, stringBuilder.toString());
                return ret;
            }
            Log.e(TAG, "getNVBackupResult, instance is null");
            throw new Exception("not support!");
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("disallowed call for uid ");
        stringBuilder2.append(Binder.getCallingUid());
        throw new SecurityException(stringBuilder2.toString());
    }

    public static int getBackgroundDebugMode() throws Exception {
        if (isUidSystem()) {
            ManufactureNativeUtils instance = getInstance();
            if (instance != null) {
                int ret = instance.native_getBackgroundDebugMode();
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getBackgroundDebugMode, ret = ");
                stringBuilder.append(ret);
                Log.d(str, stringBuilder.toString());
                return ret;
            }
            Log.e(TAG, "getBackgroundDebugMode, instance is null");
            throw new Exception("not support!");
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("disallowed call for uid ");
        stringBuilder2.append(Binder.getCallingUid());
        throw new SecurityException(stringBuilder2.toString());
    }

    public static int setBackgroundDebugMode(int mode, String password) throws Exception {
        if (isUidSystem()) {
            ManufactureNativeUtils instance = getInstance();
            if (instance != null) {
                int ret = instance.native_setBackgroundDebugMode(mode, password);
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("setBackgroundDebugMode, ret = ");
                stringBuilder.append(ret);
                Log.d(str, stringBuilder.toString());
                return ret;
            }
            Log.e(TAG, "setBackgroundDebugMode, instance is null");
            throw new Exception("not support!");
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("disallowed call for uid ");
        stringBuilder2.append(Binder.getCallingUid());
        throw new SecurityException(stringBuilder2.toString());
    }

    public static int getFuseState() throws Exception {
        if (isUidSystem()) {
            ManufactureNativeUtils instance = getInstance();
            if (instance != null) {
                int ret = instance.native_getFuseState();
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getFuseState, ret = ");
                stringBuilder.append(ret);
                Log.d(str, stringBuilder.toString());
                return ret;
            }
            Log.e(TAG, "getFuseState, instance is null");
            throw new Exception("not support!");
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("disallowed call for uid ");
        stringBuilder2.append(Binder.getCallingUid());
        throw new SecurityException(stringBuilder2.toString());
    }

    public static int verifySecbootKey(String key) throws Exception {
        if (isUidSystem()) {
            ManufactureNativeUtils instance = getInstance();
            if (instance != null) {
                int ret = instance.native_verifySecbootKey(key);
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("verifySecbootKey, ret = ");
                stringBuilder.append(ret);
                Log.d(str, stringBuilder.toString());
                return ret;
            }
            Log.e(TAG, "verifySecbootKey, instance is null");
            throw new Exception("not support!");
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("disallowed call for uid ");
        stringBuilder2.append(Binder.getCallingUid());
        throw new SecurityException(stringBuilder2.toString());
    }

    public static String getSIMLockInfo() throws Exception {
        if (isUidSystem()) {
            String ret = "";
            ManufactureNativeUtils instance = getInstance();
            if (instance != null) {
                ret = instance.native_getSIMLockDetail();
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getSIMLockInfo, ret = ");
                stringBuilder.append(ret);
                Log.d(str, stringBuilder.toString());
                return ret;
            }
            Log.e(TAG, "getSIMLockInfo, instance is null");
            throw new Exception("not support!");
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("disallowed call for uid ");
        stringBuilder2.append(Binder.getCallingUid());
        throw new SecurityException(stringBuilder2.toString());
    }

    public static String getPLMNInfo(int id) throws Exception {
        if (isUidSystem()) {
            String ret = "";
            ManufactureNativeUtils instance = getInstance();
            if (instance != null) {
                ret = instance.native_getBoundPLMNInfo(id);
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getPLMNInfo, ret = ");
                stringBuilder.append(ret);
                Log.d(str, stringBuilder.toString());
                return ret;
            }
            Log.e(TAG, "getPLMNInfo, instance is null");
            throw new Exception("not support!");
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("disallowed call for uid ");
        stringBuilder2.append(Binder.getCallingUid());
        throw new SecurityException(stringBuilder2.toString());
    }
}
