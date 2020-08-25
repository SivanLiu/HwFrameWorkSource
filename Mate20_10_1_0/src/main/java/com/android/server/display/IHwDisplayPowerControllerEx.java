package com.android.server.display;

import android.os.Bundle;

public interface IHwDisplayPowerControllerEx {

    public interface Callbacks {
        AutomaticBrightnessController getAutomaticBrightnessController();

        ManualBrightnessController getManualBrightnessController();

        void onTpKeepStateChanged(boolean z);
    }

    boolean getHwBrightnessData(String str, Bundle bundle, int[] iArr);

    boolean getTpKeep();

    void initTpKeepParamters();

    void sendProximityBroadcast(boolean z);

    boolean setHwBrightnessData(String str, Bundle bundle, int[] iArr);

    void setTPDozeMode(boolean z);
}
