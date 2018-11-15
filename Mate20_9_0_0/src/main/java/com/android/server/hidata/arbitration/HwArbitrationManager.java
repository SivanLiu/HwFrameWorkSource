package com.android.server.hidata.arbitration;

import android.content.Context;
import android.emcom.EmcomManager;
import android.os.SystemProperties;
import com.android.server.hidata.appqoe.HwAPPQoEManager;
import com.android.server.hidata.channelqoe.HwChannelQoEManager;
import com.android.server.hidata.histream.HwHiStreamManager;
import com.android.server.hidata.mplink.HwMplinkManager;
import com.android.server.hidata.wavemapping.HwWaveMappingManager;

public class HwArbitrationManager {
    private static final boolean HiStreamEnabled = SystemProperties.getBoolean("ro.config.hw_histream.enabled", true);
    private static final String TAG = "HiData_HwArbitrationManager";
    private static HwArbitrationManager mHwArbitrationManager;
    private HwAPPQoEManager mAPPQoEManager = null;
    private Context mContext;
    private IHiDataCHRCallBack mHiDataCHRCallBack;
    private HwArbitrationChrImpl mHwArbitrationChrImpl;
    private HwArbitrationStateMachine mHwArbitrationStateMachine;
    private HwArbitrationCallbackImpl mHwArbitrtionCallbackImpl = null;
    private HwHiStreamManager mHwHiStreamManager = null;
    private HwWaveMappingManager mHwWaveMappingManager = null;
    private HwMplinkManager mMplinkManager = null;
    private HwArbitrationStateMonitor mStateMonitor = null;

    public static HwArbitrationManager createInstance(Context context, IHiDataCHRCallBack chrcallBack) {
        if (mHwArbitrationManager == null) {
            mHwArbitrationManager = new HwArbitrationManager(context, chrcallBack);
        }
        return mHwArbitrationManager;
    }

    public static HwArbitrationManager getInstance() {
        HwArbitrationCommonUtils.logI(TAG, "HwArbitrationManager getInstance");
        return mHwArbitrationManager;
    }

    private HwArbitrationManager(Context context, IHiDataCHRCallBack chrcallBack) {
        this.mContext = context;
        this.mHwArbitrationStateMachine = HwArbitrationStateMachine.getInstance(this.mContext);
        this.mHwArbitrtionCallbackImpl = HwArbitrationCallbackImpl.getInstance(this.mContext);
        this.mHiDataCHRCallBack = chrcallBack;
        this.mHwArbitrationChrImpl = HwArbitrationChrImpl.createInstance();
        this.mHwArbitrationChrImpl.registArbitationChrCallBack(chrcallBack);
        modelInit(context);
        this.mStateMonitor = HwArbitrationStateMonitor.createHwArbitrationStateMonitor(this.mContext, this.mHwArbitrationStateMachine.getHandler());
        this.mStateMonitor.startMonitor();
        HwArbitrationCommonUtils.logI(TAG, "init HwArbitration completed!");
    }

    private void modelInit(Context context) {
        this.mMplinkManager = HwMplinkManager.createInstance(context);
        this.mMplinkManager.registMpLinkCallback(this.mHwArbitrtionCallbackImpl);
        this.mMplinkManager.registCHRCallback(this.mHiDataCHRCallBack);
        this.mAPPQoEManager = HwAPPQoEManager.createHwAPPQoEManager(context);
        this.mAPPQoEManager.registerAppQoECallback(this.mHwArbitrtionCallbackImpl, true);
        HwChannelQoEManager.createInstance(context);
        this.mHwWaveMappingManager = HwWaveMappingManager.getInstance(context);
        this.mHwWaveMappingManager.registerWaveMappingCallback(this.mHwArbitrtionCallbackImpl);
        if (true == HiStreamEnabled) {
            this.mHwHiStreamManager = HwHiStreamManager.createInstance(this.mContext);
            this.mHwHiStreamManager.registCHRCallback(this.mHiDataCHRCallBack);
            this.mHwHiStreamManager.registerHistreamQoeCallback(this.mHwArbitrtionCallbackImpl);
        }
    }

    public boolean getWifiPlusFlagFromHiData() {
        if (this.mHwArbitrtionCallbackImpl == null) {
            return isSmartMpEnable();
        }
        boolean z = this.mHwArbitrtionCallbackImpl.getWifiPlusFlagFromHiData() || isSmartMpEnable();
        return z;
    }

    private boolean isSmartMpEnable() {
        return EmcomManager.getInstance().isSmartMpEnable();
    }
}
