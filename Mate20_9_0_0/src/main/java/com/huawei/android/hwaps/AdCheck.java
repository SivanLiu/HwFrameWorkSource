package com.huawei.android.hwaps;

import android.os.SystemProperties;

public class AdCheck {
    private static final int STATUS_AD_BLOCK = 1;
    private static final int STATUS_AD_NOT_BLOCK = 2;
    private static final int STATUS_NOT_AD = 0;
    private static final String TAG = "AdCheck";
    private static int mAdBlockMaxCountProp = -1;
    private static String[] mAdKeyNames = new String[]{"AdView", "AdWebView", "adwoad", "mobads", "admob", "adsmogo"};
    private static int mAdNoBlockCountProp = -1;
    private static int mSupportApsProp = -1;
    private static AdCheck sInstance = null;
    private int mAdBlockCount = 0;

    public static boolean isSupportAdCheck() {
        if (-1 == mSupportApsProp) {
            mSupportApsProp = SystemProperties.getInt("sys.aps.support", 0);
        }
        if (32 == (mSupportApsProp & 32)) {
            return true;
        }
        return false;
    }

    public static synchronized AdCheck getInstance() {
        AdCheck adCheck;
        synchronized (AdCheck.class) {
            if (sInstance == null) {
                sInstance = new AdCheck();
            }
            adCheck = sInstance;
        }
        return adCheck;
    }

    public boolean isAdCheckEnable(String pkgName) {
        if (SystemProperties.get("debug.aps.process.name", "").equals(pkgName)) {
            return true;
        }
        return false;
    }

    public int checkAd(String clsName) {
        int i = 0;
        if (clsName == null || clsName.isEmpty()) {
            return 0;
        }
        String str;
        StringBuilder stringBuilder;
        int result = 0;
        String[] strArr = mAdKeyNames;
        int length = strArr.length;
        while (i < length) {
            if (clsName.contains(strArr[i])) {
                if (checkBlockMaxCount()) {
                    result = 2;
                } else {
                    result = 1;
                }
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("checkAd:");
                stringBuilder.append(result);
                stringBuilder.append(", clsName:");
                stringBuilder.append(clsName);
                ApsCommon.logD(str, stringBuilder.toString());
                return result;
            }
            i++;
        }
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("checkAd:");
        stringBuilder.append(result);
        stringBuilder.append(", clsName:");
        stringBuilder.append(clsName);
        ApsCommon.logD(str, stringBuilder.toString());
        return result;
    }

    private int getAdBlockMaxCountProp() {
        if (-1 == mAdBlockMaxCountProp) {
            mAdBlockMaxCountProp = SystemProperties.getInt("debug.adblock.maxcount", 0);
        }
        return mAdBlockMaxCountProp;
    }

    private int getAdNoBlockCountProp() {
        if (-1 == mAdNoBlockCountProp) {
            mAdNoBlockCountProp = SystemProperties.getInt("debug.adnoblock.count", 0);
        }
        return mAdNoBlockCountProp;
    }

    private boolean checkBlockMaxCount() {
        mAdBlockMaxCountProp = getAdBlockMaxCountProp();
        if (mAdBlockMaxCountProp == 0) {
            return false;
        }
        mAdNoBlockCountProp = getAdNoBlockCountProp();
        boolean result = false;
        this.mAdBlockCount++;
        if (this.mAdBlockCount > mAdBlockMaxCountProp) {
            result = true;
        }
        if (this.mAdBlockCount > mAdBlockMaxCountProp + mAdNoBlockCountProp) {
            this.mAdBlockCount = 0;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("checkBlockMaxCount:");
        stringBuilder.append(result);
        stringBuilder.append(", BlockCount:");
        stringBuilder.append(this.mAdBlockCount);
        stringBuilder.append(", MaxCount:");
        stringBuilder.append(mAdBlockMaxCountProp);
        stringBuilder.append(", NoBlockCount:");
        stringBuilder.append(mAdNoBlockCountProp);
        ApsCommon.logD(str, stringBuilder.toString());
        return result;
    }
}
