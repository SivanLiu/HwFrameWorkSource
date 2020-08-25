package com.android.server.tv;

import android.os.SystemProperties;

public interface HwTvPowerManagerPolicy {
    public static final boolean IS_TV = "tv".equalsIgnoreCase(SystemProperties.get("ro.build.characteristics"));

    void bootCompleted();

    boolean isWakeLockDisabled(String str, int i, int i2);

    boolean isWakelockCauseWakeUpDisabled();

    void onEarlyGoToSleep(int i);

    void onEarlyShutdownBegin(boolean z);

    void onKeyOperation();

    void systemReady();

    static default boolean isTvMode(int uiMode) {
        return uiMode == 4;
    }
}
