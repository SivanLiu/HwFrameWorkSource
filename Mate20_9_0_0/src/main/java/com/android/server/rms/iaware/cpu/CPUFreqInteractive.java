package com.android.server.rms.iaware.cpu;

import android.content.Context;
import android.rms.iaware.AwareLog;
import com.android.server.rms.iaware.sysload.SysLoadManager;
import com.huawei.displayengine.IDisplayEngineService;
import com.huawei.pgmng.plug.PGSdk.Sink;

class CPUFreqInteractive {
    private static final String TAG = "CPUFreqInteractive";
    private CPUAppRecogMngProxy mCPUAppRecogMngProxy;
    private Sink mFreqInteractiveSink = new FreqInteractiveSink();

    private class FreqInteractiveSink implements Sink {
        private FreqInteractiveSink() {
        }

        public void onStateChanged(int stateType, int eventType, int pid, String pkg, int uid) {
            String str = CPUFreqInteractive.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onStateChanged stateType = ");
            stringBuilder.append(stateType);
            stringBuilder.append(" eventType = ");
            stringBuilder.append(eventType);
            stringBuilder.append(" pid = ");
            stringBuilder.append(pid);
            stringBuilder.append(" pkg = ");
            stringBuilder.append(pkg);
            AwareLog.d(str, stringBuilder.toString());
            if (CPUFreqInteractive.this.mCPUAppRecogMngProxy.isGameType(stateType)) {
                if (eventType == 1) {
                    CpuThreadBoost.getInstance().resetBoostCpus();
                    CPUGameScene.getInstance().enterGameSceneMsg();
                    SysLoadManager.getInstance().enterGameSceneMsg();
                } else if (eventType == 2) {
                    CpuThreadBoost.getInstance().setBoostCpus();
                    CPUGameScene.getInstance().exitGameSceneMsg();
                    SysLoadManager.getInstance().exitGameSceneMsg();
                }
            } else if (!CPUFreqInteractive.this.mCPUAppRecogMngProxy.isVideoType(stateType)) {
            } else {
                if (stateType == IDisplayEngineService.DE_ACTION_PG_VIDEO_START) {
                    CpuThreadBoost.getInstance().resetBoostCpus();
                } else if (stateType == IDisplayEngineService.DE_ACTION_PG_VIDEO_END) {
                    CpuThreadBoost.getInstance().setBoostCpus();
                }
            }
        }
    }

    public CPUFreqInteractive(Context context) {
        this.mCPUAppRecogMngProxy = new CPUAppRecogMngProxy(context);
    }

    public void startGameStateMoniter() {
        this.mCPUAppRecogMngProxy.register(this.mFreqInteractiveSink);
    }

    public void stopGameStateMoniter() {
        this.mCPUAppRecogMngProxy.unregister(this.mFreqInteractiveSink);
    }
}
