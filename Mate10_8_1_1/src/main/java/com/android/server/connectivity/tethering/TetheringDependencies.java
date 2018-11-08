package com.android.server.connectivity.tethering;

import android.net.util.SharedLog;
import android.os.Handler;

public class TetheringDependencies {
    public OffloadHardwareInterface getOffloadHardwareInterface(Handler h, SharedLog log) {
        return new OffloadHardwareInterface(h, log);
    }
}
