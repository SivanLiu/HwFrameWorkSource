package com.android.server.location;

import android.content.Context;
import android.location.Location;
import android.os.Message;
import android.util.Log;
import java.util.Properties;

public interface IHwGnssLocationProvider {
    public static final boolean DEBUG = (Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 3)));
    public static final boolean GPS_DBG = true;
    public static final String TAG = "GnssLocationProvider";

    boolean checkLowPowerMode();

    int getPreferredAccuracy();

    String getSvType(int i);

    void handleGnssNavigatingStateChange(boolean z);

    void handleReportLocationEx(boolean z, Location location);

    void hwHandleMessage(Message message);

    boolean isLocalDBEnabled();

    void loadPropertiesFromResourceEx(Context context, Properties properties);

    boolean sendExtraCommandEx(String str);

    boolean shouldRestartNavi();

    void startNavigatingEx();
}
