package com.huawei.nb.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.PowerManager;
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
    private static final String BOOT_MODE_KEY = "ro.huawei.odmf.mode";
    private static final String BOOT_MODE_VALUE_BASIC = "basic";
    private static final String BOOT_MODE_VALUE_NORMAL = "normal";
    public static final String CHINA = "China";
    public static final String DEVICE_ATTESTATION_MANAGER = "com.huawei.attestation.HwAttestationManager";
    private static final String DEVICE_INFO_COMMON = "common";
    private static final int DEVICE_TOKEN_KEY_NO_BASE = 2;
    private static final String DOMESTIC_BETA_USER = "3";
    public static final String OVERSEA = "Oversea";
    private static final String SEARCH_SWITCH_KEY = "ro.config.hw_globalSearch";
    private static final String TAG = "DeviceUtil";
    private static String sDeviceToken = null;

    public static boolean isScreenOn(Context context) {
        if (context == null) {
            return false;
        }
        return ((PowerManager) context.getSystemService("power")).isInteractive();
    }

    public static String getSerialNumber() {
        try {
            if (Build.VERSION.SDK_INT < 26) {
                return Build.SERIAL;
            }
            return Build.getSerial();
        } catch (SecurityException e) {
            DSLog.e("DeviceUtil VerifyViaHWMember Exception : getSerial without READ_PHONE_STATE permission", new Object[0]);
            return "";
        }
    }

    public static String getVersionName(Context context) {
        PackageInfo packageInfo;
        if (context == null || (packageInfo = getPackageInfo(context)) == null) {
            return "";
        }
        return packageInfo.versionName;
    }

    public static int getVersionCode(Context context) {
        PackageInfo pi = getPackageInfo(context);
        if (pi != null) {
            return pi.versionCode;
        }
        return 0;
    }

    private static PackageInfo getPackageInfo(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            if (pm != null) {
                return pm.getPackageInfo(context.getPackageName(), 0);
            }
            return null;
        } catch (PackageManager.NameNotFoundException | RuntimeException e) {
            DSLog.e("DeviceUtil getPackageInfo fail!" + e.getMessage(), new Object[0]);
            return null;
        }
    }

    public static String getEMMCID() {
        int deviceIdTypeEmmc = getIntFiled(DEVICE_ATTESTATION_MANAGER, "DEVICE_ID_TYPE_EMMC", -1);
        if (deviceIdTypeEmmc == -1) {
            DSLog.e("DeviceUtil getAttestationSignature failed: deviceIdTypeEmmc == -1", new Object[0]);
            return "";
        }
        Object retObj = null;
        try {
            Class<?> cls = Class.forName(DEVICE_ATTESTATION_MANAGER);
            if (cls == null) {
                return "";
            }
            retObj = cls.getDeclaredMethod("getDeviceID", Integer.TYPE).invoke(cls.newInstance(), Integer.valueOf(deviceIdTypeEmmc));
            if (retObj != null) {
                return new String((byte[]) retObj, StandardCharsets.UTF_8);
            }
            DSLog.e("DeviceUtil emmcID is empty!", new Object[0]);
            return "";
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
            DSLog.e("DeviceUtil getEMMCID failed: ", new Object[0]);
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
        } catch (IllegalAccessException | IllegalArgumentException | NoSuchFieldException e2) {
            DSLog.e("DeviceUtil getIntFiled failed: ", new Object[0]);
            return def;
        }
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
        try {
            return ((Integer) UserHandle.class.getDeclaredMethod("getCallingUserId", new Class[0]).invoke(null, new Object[0])).intValue();
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            DSLog.e("DeviceUtil getCurrentUId Exception :", new Object[0]);
            return -1;
        }
    }

    public static boolean isDomesticBetaUser() {
        return "3".equals(SystemPropertiesEx.get("ro.logsystem.usertype"));
    }

    public static String getChipset() {
        String hard = SystemPropertiesEx.get("ro.hardware");
        if (TextUtils.isEmpty(hard)) {
            return DEVICE_INFO_COMMON;
        }
        if ("kirin720".equals(hard) || "kirin810".equals(hard)) {
            return "orlando";
        }
        return hard;
    }

    public static String getDistrict() {
        return "CN".equalsIgnoreCase(SystemPropertiesEx.get("ro.product.locale.region", "")) ? CHINA : OVERSEA;
    }

    public static String getRegion() {
        return SystemPropertiesEx.get("ro.product.locale.region", "");
    }

    public static String getProductFamily() {
        return "tablet".equalsIgnoreCase(SystemPropertiesEx.get("ro.build.characteristics")) ? "tablet" : "phone";
    }

    public static String getEmuiFamily() {
        return isLiteDevice() ? "lite" : "full";
    }

    public static boolean isLiteDevice() {
        return SystemPropertiesEx.getBoolean("ro.build.hw_emui_lite.enable", false);
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
        String hardware = hard.toLowerCase(Locale.ENGLISH);
        if (hardware.contains("kirin") || hardware.contains("hisi")) {
            return "Hisi";
        }
        if (hardware.contains("msm") || hardware.contains("qsd") || hardware.contains("apq")) {
            return "Qcom";
        }
        return DEVICE_INFO_COMMON;
    }

    public static String getModel() {
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
        boolean z;
        Boolean result;
        boolean z2 = true;
        if (context == null || dirUid < 0) {
            Object[] objArr = new Object[2];
            if (context == null) {
                z = true;
            } else {
                z = false;
            }
            objArr[0] = Boolean.valueOf(z);
            objArr[1] = Integer.valueOf(dirUid);
            DSLog.e("DeviceUtil isUserKnown err param, context null[%s] or dirUid invalid [%s]", objArr);
            return null;
        }
        try {
            if (UserManager.class.getDeclaredMethod("getUserInfo", Integer.TYPE).invoke((UserManager) context.getSystemService("user"), Integer.valueOf(dirUid)) == null) {
                z2 = false;
            }
            result = Boolean.valueOf(z2);
        } catch (IllegalAccessException | NoSuchMethodException | RuntimeException | InvocationTargetException e) {
            DSLog.e("DeviceUtil isUserKnown failed with exception: " + e.getClass().getName(), new Object[0]);
            result = null;
        }
        return result;
    }

    public static String getAndroidVersion() {
        return Build.VERSION.RELEASE;
    }

    public static String getProductModel() {
        return Build.MODEL;
    }

    public static String getProductBoard() {
        return SystemPropertiesEx.get("ro.product.board");
    }

    public static boolean isBasicMode() {
        String bootModeValue = SystemPropertiesEx.get(BOOT_MODE_KEY, BOOT_MODE_VALUE_NORMAL);
        boolean searchSwitchValue = SystemPropertiesEx.getBoolean(SEARCH_SWITCH_KEY, true);
        if (BOOT_MODE_VALUE_BASIC.equalsIgnoreCase(bootModeValue)) {
            if (!searchSwitchValue) {
                return true;
            }
            DSLog.e("NaturalBase Basic Mode Can Not Provide Search Service", new Object[0]);
        }
        return false;
    }
}
