package com.huawei.android.hwaps;

import android.app.HwApsInterface;
import android.os.SystemClock;
import android.os.SystemProperties;

public class SdrController {
    public static final int KEYCODE_BACK = 4;
    public static final int KEYCODE_HOME = 3;
    public static final int KEYCODE_MENU = 82;
    public static final int KEYCODE_POWER = 26;
    private static final String TAG = "SdrController";
    private static boolean mIsModuleTurnOn;
    private static SdrController sInstance = null;
    private static boolean sIsFirstCheck = true;
    private static boolean sIsSupportApsSdr = false;
    public static long slastSetPropertyTimeStamp = 0;
    public float mRatio = 2.0f;

    static {
        boolean z = true;
        if ((SystemProperties.getInt("sys.aps.support", 0) & 278528) == 0) {
            z = false;
        }
        mIsModuleTurnOn = z;
    }

    public static synchronized SdrController getInstance() {
        SdrController sdrController;
        synchronized (SdrController.class) {
            if (sInstance == null) {
                sInstance = new SdrController();
            }
            sdrController = sInstance;
        }
        return sdrController;
    }

    public static boolean isSupportApsSdr() {
        if (sIsFirstCheck) {
            if (2048 == (SystemProperties.getInt("sys.aps.support", 0) & 2048)) {
                sIsSupportApsSdr = true;
            } else {
                ApsCommon.logI(TAG, "SDR: control: Dcr module is not supported");
            }
            sIsFirstCheck = false;
        }
        return sIsSupportApsSdr;
    }

    public void setSdrRatio(float ratio) {
        this.mRatio = ratio;
        HwApsInterface.nativeSetSdrRatio(ratio);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("SDR: control: setSdrRatio  : ");
        stringBuilder.append(this.mRatio);
        ApsCommon.logD(str, stringBuilder.toString());
    }

    public float getCurrentSdrRatio() {
        return HwApsInterface.nativeGetCurrentSdrRatio();
    }

    public boolean IsSdrCase() {
        boolean isSdrCase = HwApsInterface.nativeIsSdrCase();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("SDR: control: check if sdr can be run. [result:");
        stringBuilder.append(isSdrCase);
        stringBuilder.append("]");
        ApsCommon.logD(str, stringBuilder.toString());
        return isSdrCase;
    }

    public static boolean StopSdrForSpecial(String info, int keyCode) {
        if (4 == keyCode && mIsModuleTurnOn) {
            setPropertyForKeyCode(keyCode);
        }
        return true;
    }

    /* JADX WARNING: Removed duplicated region for block: B:16:0x0049  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static void setPropertyForKeyCode(int keyCode) {
        boolean needSetPropertyForKeyCode;
        long currentTime = SystemClock.uptimeMillis();
        int i = 1;
        boolean isGame = SystemProperties.get("sys.aps.gameProcessName", "").isEmpty() ^ true;
        boolean isBrowser = false;
        if (!isGame) {
            isBrowser = SystemProperties.get("sys.aps.browserProcessName", "").isEmpty() ^ 1;
            if (!isBrowser) {
                return;
            }
        }
        if (slastSetPropertyTimeStamp != 0) {
            if (currentTime - slastSetPropertyTimeStamp <= ((long) (isGame ? 5000 : 2500))) {
                needSetPropertyForKeyCode = false;
                if (needSetPropertyForKeyCode) {
                    String msg = new StringBuilder();
                    msg.append(Long.toString(currentTime));
                    msg.append("|");
                    msg.append(Integer.toString(keyCode));
                    msg = msg.toString();
                    try {
                        new Thread(new Runnable() {
                            public void run() {
                                SystemProperties.set("sys.aps.keycode", msg);
                            }
                        }).start();
                    } catch (Exception e) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("SDR: Controller, setPropertyForKeyCode failed to setproperties.");
                        stringBuilder.append(e);
                        ApsCommon.logD(str, stringBuilder.toString());
                    }
                    slastSetPropertyTimeStamp = currentTime;
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("SDR: Controller, setPropertyForKeyCode 2.5/5s, isGame:");
                    stringBuilder2.append(isGame ? 1 : 0);
                    stringBuilder2.append(", isBrowser:");
                    if (!isBrowser) {
                        i = 0;
                    }
                    stringBuilder2.append(i);
                    stringBuilder2.append(", keycode: ");
                    stringBuilder2.append(keyCode);
                    stringBuilder2.append(", msg: ");
                    stringBuilder2.append(msg);
                    ApsCommon.logD(str2, stringBuilder2.toString());
                }
            }
        }
        needSetPropertyForKeyCode = true;
        if (needSetPropertyForKeyCode) {
        }
    }
}
