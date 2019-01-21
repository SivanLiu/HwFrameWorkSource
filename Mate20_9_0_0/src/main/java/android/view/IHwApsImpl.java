package android.view;

import android.content.Context;

public interface IHwApsImpl {
    boolean StopSdrForSpecial(String str, int i);

    void adaptPowerSave(Context context, MotionEvent motionEvent);

    int getCustScreenDimDurationLocked(int i);

    void initAPS(Context context, int i, int i2);

    boolean isAPSReady();

    boolean isDebugPartialUpdateOn();

    boolean isGameProcess(String str);

    boolean isInPowerTest();

    boolean isSupportAps();

    boolean isSupportApsColorPlus(String str);

    boolean isSupportApsPartialUpdate();

    void powerCtroll();

    void setAPSOnPause();

    int setGameProcessName(String str, int i, int i2);
}
