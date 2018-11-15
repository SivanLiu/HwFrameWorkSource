package com.leisen.wallet.sdk;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;

public class AppConfig {
    public static final String APDU_ACTIVATE05 = "80F001010A4F08A000000632010105";
    public static final String APDU_ACTIVATE06 = "80F001010A4F08A000000632010106";
    public static final String APDU_DEACTIVATE05 = "80F001000A4F08A000000632010105";
    public static final String APDU_DEACTIVATE06 = "80F001000A4F08A000000632010106";
    public static final String APDU_GETCIN = "80CA004500";
    public static final String APDU_GETCPLC = "80CA9f7f00";
    public static final String APDU_GETIIN = "80CA004200";
    public static String CLIENTVERSION = "2.0.6";
    public static String MOBILETYPE = null;
    public static String SN = null;
    public static String STREAMURL = "https://tsm.hicloud.com:9001/TSMAPKP/HwTSMServer/applicationBusiness.action";
    public static final String VERSION = "1.0";

    public static final void init(Context context) {
        SN = getSN(context);
        MOBILETYPE = Build.MODEL;
    }

    private static String getImsi(Context context) {
        return "";
    }

    private static String getSN(Context context) {
        return "deprecated";
    }

    private static String getVersionName(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }
}
