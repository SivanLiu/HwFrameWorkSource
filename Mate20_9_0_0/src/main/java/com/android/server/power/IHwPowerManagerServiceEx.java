package com.android.server.power;

public interface IHwPowerManagerServiceEx {
    boolean isAwarePreventScreenOn(String str, String str2);

    void requestNoUserActivityNotification(int i);
}
