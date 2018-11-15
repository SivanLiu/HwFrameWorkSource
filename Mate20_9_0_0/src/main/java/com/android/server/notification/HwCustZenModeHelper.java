package com.android.server.notification;

import android.content.Context;

public class HwCustZenModeHelper {
    private String[] ABNORMAL_WHITE_APPS_UNDER_ZENMODE = new String[]{"com.tencent.mobileqq", "com.tencent.mm", "com.tencent.tim", "com.tencent.pb", "com.immomo.momo", "com.alibaba.mobileim", "im.yixin"};

    public String[] getWhiteApps(Context context) {
        return null;
    }

    public String[] getWhiteAppsInZenMode() {
        return this.ABNORMAL_WHITE_APPS_UNDER_ZENMODE;
    }
}
