package com.android.server.rms.iaware.cpu;

import android.content.Context;
import android.rms.iaware.AwareLog;
import com.android.server.rms.iaware.sysload.SysLoadManager;
import com.huawei.android.pgmng.plug.PowerKit;

public class CPUFreqInteractive {
    private static final String TAG = "CPUFreqInteractive";
    /* access modifiers changed from: private */
    public CPUAppRecogMngProxy mCpuAppRecogMngProxy;
    private PowerKit.Sink mFreqInteractiveSink = new FreqInteractiveSink();

    public CPUFreqInteractive(Context context) {
        this.mCpuAppRecogMngProxy = new CPUAppRecogMngProxy(context);
    }

    public void startGameStateMoniter() {
        this.mCpuAppRecogMngProxy.register(this.mFreqInteractiveSink);
    }

    public void stopGameStateMoniter() {
        this.mCpuAppRecogMngProxy.unregister(this.mFreqInteractiveSink);
    }

    private class FreqInteractiveSink implements PowerKit.Sink {
        private FreqInteractiveSink() {
        }

        public void onStateChanged(int stateType, int eventType, int pid, String pkg, int uid) {
            AwareLog.d(CPUFreqInteractive.TAG, "onStateChanged stateType = " + stateType + " eventType = " + eventType + " pid = " + pid + " pkg = " + pkg);
            if (CPUFreqInteractive.this.mCpuAppRecogMngProxy.isGameType(stateType)) {
                if (eventType == 1) {
                    CpuThreadBoost.getInstance().resetBoostCpus();
                    CPUGameScene.getInstance().enterGameSceneMsg();
                    SysLoadManager.getInstance().enterGameSceneMsg();
                }
                if (eventType == 2) {
                    CpuThreadBoost.getInstance().setBoostCpus();
                    CPUGameScene.getInstance().exitGameSceneMsg();
                    SysLoadManager.getInstance().exitGameSceneMsg();
                }
            } else if (CPUFreqInteractive.this.mCpuAppRecogMngProxy.isVideoType(stateType)) {
                if (stateType == 10015) {
                    CpuThreadBoost.getInstance().resetBoostCpus();
                }
                if (stateType == 10016) {
                    CpuThreadBoost.getInstance().setBoostCpus();
                }
            }
        }
    }
}
