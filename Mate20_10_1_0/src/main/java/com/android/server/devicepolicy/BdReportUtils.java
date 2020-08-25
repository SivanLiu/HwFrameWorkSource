package com.android.server.devicepolicy;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import com.android.server.hidata.arbitration.HwArbitrationDEFS;
import com.android.server.pm.auth.HwCertificationManager;
import com.huawei.android.statistical.StatisticalUtils;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

public class BdReportUtils {
    private static final int LEGAL_RECORD_NUM = 4;
    private static final String SEPARATOR = ":";
    private static final int SIGNATURE_LENGTH = 16;
    private static final String TAG = "BdReportUtils";
    private static final String TERMINATOR = ";";

    public static void reportSetPolicyPkgData(ComponentName who, Context reportContext, Bundle policyData) {
        if (who != null && reportContext != null && policyData != null) {
            String ownerPkgName = who.getPackageName();
            if (!TextUtils.isEmpty(ownerPkgName)) {
                String[] ownerPkgData = getOwnerPkgData(ownerPkgName, reportContext);
                String[] pkgPolicyData = getPkgPolicyData(policyData);
                StatisticalUtils.reporte(reportContext, 1022, String.format(Locale.ENGLISH, "{oPkg:%s,oPkgSig:%s,oPkgVer:%s,pkg:%s,bundle:%s}", ownerPkgName, ownerPkgData[0], ownerPkgData[1], pkgPolicyData[0], pkgPolicyData[1]));
            }
        }
    }

    public static void reportInstallPkgData(String ownerPkgName, String pkgName, Context reportContext) {
        if (!TextUtils.isEmpty(ownerPkgName) && reportContext != null) {
            StatisticalUtils.reporte(reportContext, (int) HwArbitrationDEFS.MSG_STATE_OUT_OF_SERVICE, String.format(Locale.ENGLISH, "{oPkg:%s,pkg:%s}", ownerPkgName, getHashCodeForString(pkgName)));
        }
    }

    public static void reportUninstallPkgData(String ownerPkgName, String pkgName, Context reportContext) {
        if (!TextUtils.isEmpty(ownerPkgName) && reportContext != null) {
            StatisticalUtils.reporte(reportContext, 1021, String.format(Locale.ENGLISH, "{oPkg:%s,pkg:%s}", ownerPkgName, getHashCodeForString(pkgName)));
        }
    }

    private static String[] getOwnerPkgData(String ownerPkgName, Context reportContext) {
        String[] ownerPkgData = {"null", "null"};
        String ownerPkgSignature = HwCertificationManager.getIntance().getSignatureOfCert(ownerPkgName, 0);
        if (!TextUtils.isEmpty(ownerPkgSignature)) {
            if (ownerPkgSignature.length() < 16) {
                ownerPkgData[0] = ownerPkgSignature;
            } else {
                ownerPkgData[0] = ownerPkgSignature.substring(0, 16);
            }
        }
        PackageManager packageManager = reportContext.getPackageManager();
        if (packageManager == null) {
            return ownerPkgData;
        }
        String ownerPkgVersion = "null";
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(ownerPkgName, 0);
            if (packageInfo != null) {
                ownerPkgVersion = packageInfo.versionName;
            }
        } catch (PackageManager.NameNotFoundException e) {
            HwLog.e(TAG, "getOwnerPkgData NameNotFoundException");
        }
        if (!TextUtils.isEmpty(ownerPkgVersion)) {
            ownerPkgData[1] = ownerPkgVersion;
        }
        return ownerPkgData;
    }

    private static String[] getPkgPolicyData(Bundle policyData) {
        char c = 2;
        String[] pkgPolicyData = {"null", "null"};
        String diffValue = policyData.getString("diffValue");
        if (TextUtils.isEmpty(diffValue)) {
            return pkgPolicyData;
        }
        pkgPolicyData[0] = "";
        String[] split = diffValue.split(";");
        int length = split.length;
        int i = 0;
        while (i < length) {
            String[] infoList = split[i].split(":");
            if (infoList.length == 4) {
                pkgPolicyData[0] = pkgPolicyData[0] + getHashCodeForString(infoList[0]) + ";";
                if ("null".equals(pkgPolicyData[1])) {
                    boolean isAddItem = policyData.getBoolean("isAddItem");
                    pkgPolicyData[1] = "true".equals(infoList[c]) ? "1" : "0";
                    StringBuilder sb = new StringBuilder();
                    sb.append(pkgPolicyData[1]);
                    sb.append("true".equals(infoList[3]) ? "1" : "0");
                    pkgPolicyData[1] = sb.toString();
                    StringBuilder sb2 = new StringBuilder();
                    sb2.append(pkgPolicyData[1]);
                    sb2.append(isAddItem ? "1" : "0");
                    pkgPolicyData[1] = sb2.toString();
                }
            }
            i++;
            c = 2;
        }
        if ("null".equals(pkgPolicyData[1])) {
            pkgPolicyData[0] = "null";
        }
        return pkgPolicyData;
    }

    private static String getHashCodeForString(String data) {
        if (TextUtils.isEmpty(data)) {
            return "null";
        }
        try {
            String dataSha256 = sha256(data.getBytes("UTF-8"));
            if (TextUtils.isEmpty(dataSha256) || "null".equals(dataSha256)) {
                return "null";
            }
            return dataSha256.substring(0, 32);
        } catch (UnsupportedEncodingException e) {
            HwLog.e(TAG, "getHashCodeForString UnsupportedEncodingException");
            return "null";
        }
    }

    private static String sha256(byte[] data) {
        if (data == null) {
            return "null";
        }
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(data);
            return bytesToString(messageDigest.digest());
        } catch (NoSuchAlgorithmException e) {
            HwLog.e(TAG, "sha256 NoSuchAlgorithmException");
            return "null";
        }
    }

    private static String bytesToString(byte[] bytes) {
        if (bytes == null) {
            return "null";
        }
        char[] hexChars = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        char[] chars = new char[(bytes.length * 2)];
        for (int j = 0; j < bytes.length; j++) {
            int byteValue = bytes[j] & 255;
            chars[j * 2] = hexChars[byteValue >>> 4];
            chars[(j * 2) + 1] = hexChars[byteValue & 15];
        }
        return new String(chars).toUpperCase(Locale.ENGLISH);
    }
}
