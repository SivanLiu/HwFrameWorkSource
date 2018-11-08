package com.android.server.oemlock;

abstract class OemLock {
    abstract boolean isOemUnlockAllowedByCarrier();

    abstract boolean isOemUnlockAllowedByDevice();

    abstract void setOemUnlockAllowedByCarrier(boolean z, byte[] bArr);

    abstract void setOemUnlockAllowedByDevice(boolean z);

    OemLock() {
    }
}
