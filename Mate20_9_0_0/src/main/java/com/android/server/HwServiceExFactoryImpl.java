package com.android.server;

import android.content.Context;
import com.android.server.HwServiceExFactory.Factory;
import com.android.server.am.ActivityManagerService;
import com.android.server.am.HwActivityManagerServiceEx;
import com.android.server.am.HwActivityStackSupervisorEx;
import com.android.server.am.HwActivityStarterEx;
import com.android.server.am.IHwActivityManagerInner;
import com.android.server.am.IHwActivityManagerServiceEx;
import com.android.server.audio.HwAudioServiceEx;
import com.android.server.audio.IHwAudioServiceEx;
import com.android.server.audio.IHwAudioServiceInner;
import com.android.server.display.HwDisplayManagerServiceEx;
import com.android.server.display.IHwDisplayManagerInner;
import com.android.server.display.IHwDisplayManagerServiceEx;
import com.android.server.imm.HwInputMethodManagerServiceEx;
import com.android.server.imm.IHwInputMethodManagerInner;
import com.android.server.imm.IHwInputMethodManagerServiceEx;
import com.android.server.input.HwInputManagerServiceEx;
import com.android.server.input.IHwInputManagerInner;
import com.android.server.input.IHwInputManagerServiceEx;
import com.android.server.pm.HwBackgroundDexOptServiceEx;
import com.android.server.pm.HwPackageManagerServiceEx;
import com.android.server.pm.IHwBackgroundDexOptInner;
import com.android.server.pm.IHwBackgroundDexOptServiceEx;
import com.android.server.pm.IHwPackageManagerInner;
import com.android.server.pm.IHwPackageManagerServiceEx;
import com.android.server.power.HwPowerManagerServiceEx;
import com.android.server.power.IHwPowerManagerInner;
import com.android.server.power.IHwPowerManagerServiceEx;
import com.android.server.wm.HwWindowManagerServiceEx;
import com.android.server.wm.IHwWindowManagerInner;
import com.android.server.wm.IHwWindowManagerServiceEx;
import com.huawei.server.am.IHwActivityStackSupervisorEx;
import com.huawei.server.am.IHwActivityStarterEx;

public class HwServiceExFactoryImpl implements Factory {
    private static final String TAG = "HwServiceExFactoryImpl";

    public IHwActivityManagerServiceEx getHwActivityManagerServiceEx(IHwActivityManagerInner ams, Context context) {
        return new HwActivityManagerServiceEx(ams, context);
    }

    public IHwWindowManagerServiceEx getHwWindowManagerServiceEx(IHwWindowManagerInner wms, Context context) {
        return new HwWindowManagerServiceEx(wms, context);
    }

    public IHwPackageManagerServiceEx getHwPackageManagerServiceEx(IHwPackageManagerInner pms, Context context) {
        return new HwPackageManagerServiceEx(pms, context);
    }

    public IHwInputMethodManagerServiceEx getHwInputMethodManagerServiceEx(IHwInputMethodManagerInner ims, Context context) {
        return new HwInputMethodManagerServiceEx(ims, context);
    }

    public IHwBackgroundDexOptServiceEx getHwBackgroundDexOptServiceEx(IHwBackgroundDexOptInner bdos, Context context) {
        return new HwBackgroundDexOptServiceEx(bdos, context);
    }

    public IHwActivityStarterEx getHwActivityStarterEx(ActivityManagerService ams) {
        return new HwActivityStarterEx(ams);
    }

    public IHwAudioServiceEx getHwAudioServiceEx(IHwAudioServiceInner ias, Context context) {
        return new HwAudioServiceEx(ias, context);
    }

    public IHwPowerManagerServiceEx getHwPowerManagerServiceEx(IHwPowerManagerInner pms, Context context) {
        return new HwPowerManagerServiceEx(pms, context);
    }

    public IHwInputManagerServiceEx getHwInputManagerServiceEx(IHwInputManagerInner ims, Context context) {
        return new HwInputManagerServiceEx(ims, context);
    }

    public IHwActivityStackSupervisorEx getHwActivityStackSupervisorEx() {
        return new HwActivityStackSupervisorEx();
    }

    public IHwDisplayManagerServiceEx getHwDisplayManagerServiceEx(IHwDisplayManagerInner dms, Context context) {
        return new HwDisplayManagerServiceEx(dms, context);
    }
}
