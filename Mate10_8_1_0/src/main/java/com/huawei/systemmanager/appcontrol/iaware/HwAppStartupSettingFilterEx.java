package com.huawei.systemmanager.appcontrol.iaware;

import android.app.mtm.iaware.HwAppStartupSettingFilter;

public class HwAppStartupSettingFilterEx {
    private HwAppStartupSettingFilter mInnerFilter = new HwAppStartupSettingFilter();

    public HwAppStartupSettingFilter getHwAppStartupSettingFilter() {
        return this.mInnerFilter;
    }

    public int[] getPolicy() {
        return this.mInnerFilter.getPolicy();
    }

    public HwAppStartupSettingFilterEx setPolicy(int[] policy) {
        this.mInnerFilter.setPolicy(policy);
        return this;
    }

    public HwAppStartupSettingFilterEx setShow(int[] show) {
        this.mInnerFilter.setShow(show);
        return this;
    }
}
