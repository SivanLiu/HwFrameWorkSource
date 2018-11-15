package com.huawei.nb.utils;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;
import com.huawei.android.os.SystemPropertiesEx;
import com.huawei.nb.security.SHA256Utils;
import com.huawei.nb.utils.logger.DSLog;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class DeviceUtil {
    public static final String DEVICE_ATTESTATION_MANAGER = "com.huawei.attestation.HwAttestationManager";
    private static final String DEVICE_INFO_COMMON = "common";
    private static final int DEVICE_TOKEN_KEYNO_BASE = 2;
    private static final String DOMESTIC_BETA_USER = "3";
    private static final String TAG = "DeviceUtil";
    private static String sDeviceToken = null;

    public static String getSerialNumber() {
        try {
            if (VERSION.SDK_INT < 26) {
                return Build.SERIAL;
            }
            return Build.getSerial();
        } catch (SecurityException e) {
            DSLog.e("DeviceUtil VerifyViaHWMember Exception : getSerial without READ_PHONE_STATE permission", new Object[0]);
            return "";
        }
    }

    public static String getVersionName(Context context) {
        String versionName = null;
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (NameNotFoundException e) {
            DSLog.e("DeviceUtil getVersionName fail!" + e.getMessage(), new Object[0]);
            return versionName;
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:13:0x005a  */
    /* JADX WARNING: Removed duplicated region for block: B:10:0x0044  */
    /* JADX WARNING: Removed duplicated region for block: B:10:0x0044  */
    /* JADX WARNING: Removed duplicated region for block: B:13:0x005a  */
    /* JADX WARNING: Removed duplicated region for block: B:13:0x005a  */
    /* JADX WARNING: Removed duplicated region for block: B:10:0x0044  */
    /* JADX WARNING: Removed duplicated region for block: B:10:0x0044  */
    /* JADX WARNING: Removed duplicated region for block: B:13:0x005a  */
    /* JADX WARNING: Removed duplicated region for block: B:13:0x005a  */
    /* JADX WARNING: Removed duplicated region for block: B:10:0x0044  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static String getEMMCID() {
        if (getIntFiled(DEVICE_ATTESTATION_MANAGER, "DEVICE_ID_TYPE_EMMC", -1) == -1) {
            DSLog.e("DeviceUtil getAttestationSignature failed: deviceIdTypeEmmc == -1", new Object[0]);
            return "";
        }
        Object retObj = null;
        try {
            Class<?> cls = Class.forName(DEVICE_ATTESTATION_MANAGER);
            if (cls == null) {
                return "";
            }
            retObj = cls.getDeclaredMethod("getDeviceID", new Class[]{Integer.TYPE}).invoke(cls.newInstance(), new Object[]{Integer.valueOf(deviceIdTypeEmmc)});
            if (retObj == null) {
                return new String((byte[]) retObj, StandardCharsets.UTF_8);
            }
            DSLog.e("DeviceUtil emmcID is empty!", new Object[0]);
            return "";
        } catch (ClassNotFoundException e) {
            DSLog.e("DeviceUtil getEMMCID failed: ", new Object[0]);
            if (retObj == null) {
            }
        } catch (NoSuchMethodException e2) {
            DSLog.e("DeviceUtil getEMMCID failed: ", new Object[0]);
            if (retObj == null) {
            }
        } catch (InvocationTargetException e3) {
            DSLog.e("DeviceUtil getEMMCID failed: ", new Object[0]);
            if (retObj == null) {
            }
        } catch (IllegalAccessException e4) {
            DSLog.e("DeviceUtil getEMMCID failed: ", new Object[0]);
            if (retObj == null) {
            }
        } catch (InstantiationException e5) {
            DSLog.e("DeviceUtil getEMMCID failed: ", new Object[0]);
            if (retObj == null) {
            }
        }
    }

    public static int getIntFiled(String className, String filedName, int def) {
        Class<?> cls = null;
        try {
            cls = Class.forName(className);
        } catch (ClassNotFoundException e) {
            DSLog.e("DeviceUtil getIntFiled failed: ClassNotFoundException", new Object[0]);
        }
        if (cls == null) {
            return def;
        }
        try {
            return cls.getField(filedName).getInt(null);
        } catch (IllegalArgumentException e2) {
        } catch (IllegalAccessException e3) {
        } catch (NoSuchFieldException e4) {
        }
        DSLog.e("DeviceUtil getIntFiled failed: ", new Object[0]);
        return def;
    }

    public static String getDeviceToken(Context context) {
        if (context == null) {
            DSLog.e("DeviceUtil [getDeviceToken] context is null, please call HttpClient.init(Context) first.", new Object[0]);
            return "";
        }
        if (sDeviceToken == null) {
            String mDeviceId = getSerialNumber();
            if (TextUtils.isEmpty(mDeviceId)) {
                DSLog.e("DeviceUtil [getDeviceToken] SN is null or empty, return null.", new Object[0]);
                return "";
            }
            int mUserId = getCurrentUserId();
            if (mUserId < 0) {
                DSLog.e("DeviceUtil [getDeviceToken] UserId is illegal, return null", new Object[0]);
                return "";
            }
            DSLog.d("DeviceUtil [getDeviceToken]  UserId : " + mUserId, new Object[0]);
            String signedDeviceId = SHA256Utils.sha256Encrypt(mDeviceId);
            StringBuilder deviceTokenBuilder = new StringBuilder();
            if (TextUtils.isEmpty(signedDeviceId)) {
                DSLog.w("DeviceUtil signedDeviceId is null or empty, use the old format of DeviceToken", new Object[0]);
                deviceTokenBuilder.append("1,");
                deviceTokenBuilder.append("1464c17ea1a1112");
                sDeviceToken = deviceTokenBuilder.toString();
            } else {
                deviceTokenBuilder.append(2);
                deviceTokenBuilder.append(",");
                deviceTokenBuilder.append(signedDeviceId).append("_").append(mUserId);
                sDeviceToken = deviceTokenBuilder.toString();
            }
        }
        return sDeviceToken;
    }

    public static int getCurrentUserId() {
        int userid = -1;
        try {
            return ((Integer) UserHandle.class.getDeclaredMethod("getCallingUserId", new Class[0]).invoke(null, new Object[0])).intValue();
        } catch (NoSuchMethodException e) {
        } catch (IllegalAccessException e2) {
        } catch (InvocationTargetException e3) {
        }
        DSLog.e("DeviceUtil getCurrentUId Exception :", new Object[0]);
        return userid;
    }

    public static boolean isDomesticBetaUser() {
        return "3".equals(SystemPropertiesEx.get("ro.logsystem.usertype"));
    }

    public static String getSystemVersion() {
        return Build.DISPLAY;
    }

    public static String getChipset() {
        String hard = SystemPropertiesEx.get("ro.hardware");
        if (TextUtils.isEmpty(hard)) {
            return DEVICE_INFO_COMMON;
        }
        return hard;
    }

    public static String getDistrict() {
        return "CN".equalsIgnoreCase(SystemPropertiesEx.get("ro.product.locale.region", "")) ? "China" : "Oversea";
    }

    public static String getProductFamily() {
        return "tablet".equalsIgnoreCase(SystemPropertiesEx.get("ro.build.characteristics")) ? "tablet" : "phone";
    }

    public static String getEmuiFamily() {
        return SystemPropertiesEx.getBoolean("ro.build.hw_emui_lite.enable", false) ? "lite" : "full";
    }

    public static String getProduct() {
        String product = SystemPropertiesEx.get("ro.build.product");
        return TextUtils.isEmpty(product) ? DEVICE_INFO_COMMON : product;
    }

    public static String getChipsetVendor() {
        String hard = SystemPropertiesEx.get("ro.hardware");
        if (TextUtils.isEmpty(hard)) {
            return DEVICE_INFO_COMMON;
        }
        String hardware = hard.toLowerCase(Locale.ROOT);
        if (hardware.contains("kirin") || hardware.contains("hisi")) {
            return "Hisi";
        }
        if (hardware.contains("msm") || hardware.contains("qsd") || hardware.contains("apq")) {
            return "Qcom";
        }
        return DEVICE_INFO_COMMON;
    }

    public static String getProductModel() {
        String productName = SystemPropertiesEx.get("ro.product.name");
        if (!TextUtils.isEmpty(productName)) {
            String[] infos = productName.split("-");
            if (infos.length >= 2) {
                return infos[1];
            }
        }
        return DEVICE_INFO_COMMON;
    }

    public static Boolean isUserKnown(Context context, int dirUid) {
        Boolean result;
        Exception e;
        boolean z = true;
        if (context == null || dirUid < 0) {
            String str = "DeviceUtil isUserKnown err param, context null[%s] or dirUid invalid [%s]";
            Object[] objArr = new Object[2];
            objArr[0] = Boolean.valueOf(context == null);
            objArr[1] = Integer.valueOf(dirUid);
            DSLog.e(str, objArr);
            return null;
        }
        try {
            if (UserManager.class.getDeclaredMethod("getUserInfo", new Class[]{Integer.TYPE}).invoke((UserManager) context.getSystemService("user"), new Object[]{Integer.valueOf(dirUid)}) == null) {
                z = false;
            }
            result = Boolean.valueOf(z);
        } catch (RuntimeException e2) {
            e = e2;
            DSLog.e("DeviceUtil isUserKnown failed with exception: " + e.getClass().getName(), new Object[0]);
            result = null;
            return result;
        } catch (NoSuchMethodException e3) {
            e = e3;
            DSLog.e("DeviceUtil isUserKnown failed with exception: " + e.getClass().getName(), new Object[0]);
            result = null;
            return result;
        } catch (IllegalAccessException e4) {
            e = e4;
            DSLog.e("DeviceUtil isUserKnown failed with exception: " + e.getClass().getName(), new Object[0]);
            result = null;
            return result;
        } catch (InvocationTargetException e5) {
            e = e5;
            DSLog.e("DeviceUtil isUserKnown failed with exception: " + e.getClass().getName(), new Object[0]);
            result = null;
            return result;
        }
        return result;
    }
}
