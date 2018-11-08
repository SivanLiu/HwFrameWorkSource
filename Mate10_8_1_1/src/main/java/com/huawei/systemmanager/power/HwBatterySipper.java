package com.huawei.systemmanager.power;

import com.android.internal.os.BatterySipper;
import com.android.internal.os.BatterySipper.DrainType;

public class HwBatterySipper {
    private BatterySipper mLocalsipper;

    public enum HwDrainType {
        IDLE(DrainType.IDLE),
        CELL(DrainType.CELL),
        PHONE(DrainType.PHONE),
        WIFI(DrainType.WIFI),
        BLUETOOTH(DrainType.BLUETOOTH),
        FLASHLIGHT(DrainType.FLASHLIGHT),
        SCREEN(DrainType.SCREEN),
        APP(DrainType.APP),
        USER(DrainType.USER),
        UNACCOUNTED(DrainType.UNACCOUNTED),
        OVERCOUNTED(DrainType.OVERCOUNTED),
        CAMERA(DrainType.CAMERA),
        MEMORY(DrainType.MEMORY);
        
        private DrainType mLocaldrantype;

        private HwDrainType(DrainType draintype) {
            this.mLocaldrantype = draintype;
        }

        public DrainType getInnerLocalDranType() {
            return this.mLocaldrantype;
        }
    }

    public HwBatterySipper(BatterySipper sipper) {
        this.mLocalsipper = sipper;
    }

    public BatterySipper getBatterySipper() {
        return this.mLocalsipper;
    }

    public String[] getPackages() {
        return this.mLocalsipper.getPackages();
    }

    public int getUid() {
        return this.mLocalsipper.getUid();
    }

    public void add(HwBatterySipper other) {
        this.mLocalsipper.add(other.mLocalsipper);
    }

    public double getTotalPowerMah() {
        return this.mLocalsipper.totalPowerMah;
    }

    public boolean isDrainTypeApp() {
        return this.mLocalsipper.drainType == DrainType.APP;
    }

    public boolean isSameDrainType(HwDrainType drainType) {
        boolean z = false;
        if (drainType == null) {
            return false;
        }
        if (this.mLocalsipper.drainType == drainType.getInnerLocalDranType()) {
            z = true;
        }
        return z;
    }

    public long getCpuTimeMs() {
        return (this.mLocalsipper.uidObj.getUserCpuTimeUs(2) + this.mLocalsipper.uidObj.getSystemCpuTimeUs(2)) / 1000;
    }
}
