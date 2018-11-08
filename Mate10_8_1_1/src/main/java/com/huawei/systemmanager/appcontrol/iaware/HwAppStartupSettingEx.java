package com.huawei.systemmanager.appcontrol.iaware;

import android.app.mtm.iaware.HwAppStartupSetting;

public class HwAppStartupSettingEx {
    private HwAppStartupSetting mInnerSetting;

    public HwAppStartupSettingEx(String packageName, int[] policy, int[] modifier, int[] show) {
        this.mInnerSetting = new HwAppStartupSetting(packageName, policy, modifier, show);
    }

    public HwAppStartupSettingEx(HwAppStartupSetting hwAppStartupSetting) {
        this.mInnerSetting = hwAppStartupSetting;
    }

    public String getPackageName() {
        return this.mInnerSetting.getPackageName();
    }

    public int getPolicy(int type) {
        return this.mInnerSetting.getPolicy(type);
    }

    public int getModifier(int type) {
        return this.mInnerSetting.getModifier(type);
    }

    public int getShow(int type) {
        return this.mInnerSetting.getShow(type);
    }

    public HwAppStartupSetting getHwAppStartupSetting() {
        return this.mInnerSetting;
    }
}
