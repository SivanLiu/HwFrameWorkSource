package com.android.server.hidata.wavemapping.service;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Handler;
import com.android.server.hidata.wavemapping.cons.ParamManager;
import com.android.server.hidata.wavemapping.dataprovider.BehaviorReceiver;
import com.android.server.hidata.wavemapping.entity.ParameterInfo;
import com.android.server.hidata.wavemapping.util.LogUtil;

public class ActiveCollectDecision {
    private static final int INTERVAL_24HR = 86400000;
    public static final String TAG;
    private static final int TYPE_BACKGROUND_SCAN = 1;
    private static final int TYPE_OUT4G_REC_SCAN = 3;
    private static final int TYPE_STALL_SCAN = 2;
    private Handler activeCltHandler = new Handler();
    private Runnable activeOut4gScanTimer = new Runnable() {
        public void run() {
            long timeCurr = System.currentTimeMillis();
            long offsetScanDuration = timeCurr - ActiveCollectDecision.this.timeLastScan;
            StringBuilder stringBuilder;
            if (ActiveCollectDecision.this.iOut4gScanCnt <= 0 || ActiveCollectDecision.this.iOut4gScanCnt > ActiveCollectDecision.this.paraPeriodOut4gScan.length) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(" end OUT-OF-4G perodical scan, Out4gScanCnt=");
                stringBuilder.append(ActiveCollectDecision.this.iOut4gScanCnt);
                LogUtil.d(stringBuilder.toString());
                ActiveCollectDecision.this.activeCltHandler.removeCallbacks(ActiveCollectDecision.this.activeOut4gScanTimer);
                return;
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append(" run OUT-OF-4G perodical scan, Out4gScanCnt=");
            stringBuilder.append(ActiveCollectDecision.this.iOut4gScanCnt);
            stringBuilder.append(", curTime:");
            stringBuilder.append(timeCurr);
            LogUtil.i(stringBuilder.toString());
            if (ActiveCollectDecision.this.iOut4gScanCnt < ActiveCollectDecision.this.paraPeriodOut4gScan.length) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("  start timer, OUT-OF-4G next period=");
                stringBuilder.append(ActiveCollectDecision.this.paraPeriodOut4gScan[ActiveCollectDecision.this.iOut4gScanCnt]);
                LogUtil.i(stringBuilder.toString());
                ActiveCollectDecision.this.activeCltHandler.postDelayed(this, (long) ActiveCollectDecision.this.paraPeriodOut4gScan[ActiveCollectDecision.this.iOut4gScanCnt]);
                ActiveCollectDecision.this.iOut4gScanCnt = ActiveCollectDecision.this.iOut4gScanCnt + 1;
            } else {
                LogUtil.d("  final OUT-OF-4G scan, no timer");
            }
            if (offsetScanDuration > ((long) ActiveCollectDecision.this.param.getActScanLimit_interval()) && ActiveCollectDecision.this.mwifimanager != null) {
                ActiveCollectDecision.this.mwifimanager.startScan();
                ActiveCollectDecision.this.iOut4gScanDailyCnt = ActiveCollectDecision.this.iOut4gScanDailyCnt + 1;
            }
        }
    };
    private Runnable activeScanTimer = new Runnable() {
        public void run() {
            long timeCurr = System.currentTimeMillis();
            long offsetScanDuration = timeCurr - ActiveCollectDecision.this.timeLastScan;
            StringBuilder stringBuilder;
            if (ActiveCollectDecision.this.iStallScanCnt > 0 && ActiveCollectDecision.this.iStallScanCnt <= ActiveCollectDecision.this.paraPeriodStallScan.length) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(" run STALL perodical scan, StallScanCnt=");
                stringBuilder.append(ActiveCollectDecision.this.iStallScanCnt);
                stringBuilder.append(", curTime:");
                stringBuilder.append(timeCurr);
                LogUtil.i(stringBuilder.toString());
                if (ActiveCollectDecision.this.iStallScanCnt < ActiveCollectDecision.this.paraPeriodStallScan.length) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("  start timer, Stall next period=");
                    stringBuilder.append(ActiveCollectDecision.this.paraPeriodStallScan[ActiveCollectDecision.this.iStallScanCnt]);
                    LogUtil.i(stringBuilder.toString());
                    ActiveCollectDecision.this.activeCltHandler.postDelayed(this, (long) ActiveCollectDecision.this.paraPeriodStallScan[ActiveCollectDecision.this.iStallScanCnt]);
                    ActiveCollectDecision.this.iStallScanCnt = ActiveCollectDecision.this.iStallScanCnt + 1;
                } else {
                    LogUtil.d("  final STALL scan, no timer");
                }
                if (offsetScanDuration > ((long) ActiveCollectDecision.this.param.getActScanLimit_interval()) && ActiveCollectDecision.this.mwifimanager != null) {
                    ActiveCollectDecision.this.mwifimanager.startScan();
                    ActiveCollectDecision.this.iStallScanDailyCnt = ActiveCollectDecision.this.iStallScanDailyCnt + 1;
                }
            } else if (ActiveCollectDecision.this.iBgScanCnt <= 0 || ActiveCollectDecision.this.iBgScanCnt > ActiveCollectDecision.this.paraPeriodBgScan.length) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(" end perodical scan, StallScanCnt=");
                stringBuilder.append(ActiveCollectDecision.this.iStallScanCnt);
                stringBuilder.append(", BgScanCnt=");
                stringBuilder.append(ActiveCollectDecision.this.iBgScanCnt);
                LogUtil.d(stringBuilder.toString());
                ActiveCollectDecision.this.activeCltHandler.removeCallbacks(ActiveCollectDecision.this.activeScanTimer);
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append(" run BACKGROUND perodical scan, BgScanCnt=");
                stringBuilder.append(ActiveCollectDecision.this.iBgScanCnt);
                stringBuilder.append(", curTime:");
                stringBuilder.append(timeCurr);
                LogUtil.i(stringBuilder.toString());
                if (ActiveCollectDecision.this.iBgScanCnt < ActiveCollectDecision.this.paraPeriodBgScan.length) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("  start timer, BACKGROUND next period=");
                    stringBuilder.append(ActiveCollectDecision.this.paraPeriodBgScan[ActiveCollectDecision.this.iBgScanCnt]);
                    LogUtil.i(stringBuilder.toString());
                    ActiveCollectDecision.this.activeCltHandler.postDelayed(this, (long) ActiveCollectDecision.this.paraPeriodBgScan[ActiveCollectDecision.this.iBgScanCnt]);
                    ActiveCollectDecision.this.iBgScanCnt = ActiveCollectDecision.this.iBgScanCnt + 1;
                } else {
                    LogUtil.d("  final BACKGROUND scan");
                }
                if (offsetScanDuration > ((long) ActiveCollectDecision.this.param.getActScanLimit_interval()) && ActiveCollectDecision.this.mwifimanager != null) {
                    ActiveCollectDecision.this.mwifimanager.startScan();
                    ActiveCollectDecision.this.iBgScanDailyCnt = ActiveCollectDecision.this.iBgScanDailyCnt + 1;
                }
            }
        }
    };
    private int iBgScanCnt = 0;
    private int iBgScanDailyCnt = 0;
    private int iOut4gScanCnt = 0;
    private int iOut4gScanDailyCnt = 0;
    private int iStallScanCnt = 0;
    private int iStallScanDailyCnt = 0;
    private BehaviorReceiver mBehaviorHandler = null;
    Context mCtx;
    private WifiManager mwifimanager;
    private int[] paraPeriodBgScan;
    private int[] paraPeriodOut4gScan;
    private int[] paraPeriodStallScan;
    private ParameterInfo param = null;
    private long time1stCollect = 0;
    private long timeLastScan = 0;

    static {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("WMapping.");
        stringBuilder.append(ActiveCollectDecision.class.getSimpleName());
        TAG = stringBuilder.toString();
    }

    public ActiveCollectDecision(Context ctx, BehaviorReceiver behaviorReceiver) {
        LogUtil.i("ActiveCollectDecision");
        this.mCtx = ctx;
        try {
            this.mBehaviorHandler = behaviorReceiver;
            this.mwifimanager = (WifiManager) this.mCtx.getSystemService("wifi");
            LogUtil.i("ParamManager init begin.");
            this.param = ParamManager.getInstance().getParameterInfo();
            this.paraPeriodBgScan = this.param.getActBgScanPeriods();
            this.paraPeriodStallScan = this.param.getActStallScanPeriods();
            this.paraPeriodOut4gScan = this.param.getActOut4gScanPeriods();
        } catch (Exception e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("ActiveCollectDecision:");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
        }
    }

    public boolean startBgScan() {
        LogUtil.i("startBgScan:");
        if (this.iBgScanDailyCnt > this.param.getActScanLimit_bg() || this.iBgScanDailyCnt + this.iStallScanDailyCnt > this.param.getActScanLimit_total()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(" active scan reach limitation, BgScanDailyCnt=");
            stringBuilder.append(this.iBgScanDailyCnt);
            stringBuilder.append(", StallScanDailyCnt=");
            stringBuilder.append(this.iStallScanDailyCnt);
            LogUtil.d(stringBuilder.toString());
        } else {
            BehaviorReceiver behaviorReceiver = this.mBehaviorHandler;
            boolean bScreenState = BehaviorReceiver.getScrnState();
            BehaviorReceiver behaviorReceiver2 = this.mBehaviorHandler;
            boolean bArState = BehaviorReceiver.getArState();
            if (bScreenState && bArState) {
                LogUtil.i(" screen on & stable, start active BACKGROUND scan");
                startPerodicScan(1);
                return true;
            }
        }
        this.timeLastScan = System.currentTimeMillis();
        return false;
    }

    public void stopBgScan() {
        LogUtil.i("stopBgScan:");
        this.iBgScanCnt = 0;
        this.activeCltHandler.removeCallbacks(this.activeScanTimer);
    }

    public boolean startStallScan() {
        LogUtil.i("startStallScan:");
        if (this.iStallScanDailyCnt > this.param.getActScanLimit_stall() || this.iBgScanDailyCnt + this.iStallScanDailyCnt > this.param.getActScanLimit_total()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(" active scan reach limitation, StallScanDailyCnt=");
            stringBuilder.append(this.iStallScanDailyCnt);
            stringBuilder.append(", BgScanDailyCnt=");
            stringBuilder.append(this.iBgScanDailyCnt);
            LogUtil.d(stringBuilder.toString());
        } else {
            BehaviorReceiver behaviorReceiver = this.mBehaviorHandler;
            boolean bScreenState = BehaviorReceiver.getScrnState();
            BehaviorReceiver behaviorReceiver2 = this.mBehaviorHandler;
            boolean bArState = BehaviorReceiver.getArState();
            if (bScreenState && bArState) {
                LogUtil.i(" screen on & stable, start active STALL scan");
                startPerodicScan(2);
                return true;
            }
        }
        this.timeLastScan = System.currentTimeMillis();
        return false;
    }

    public void stopStallScan() {
        LogUtil.i("stopStallScan:");
        this.iStallScanCnt = 0;
        this.activeCltHandler.removeCallbacks(this.activeScanTimer);
    }

    public void triggerRecogScan() {
        LogUtil.i("triggerRecogScan:");
        if (this.mwifimanager != null) {
            this.mwifimanager.startScan();
        }
    }

    private boolean startPerodicScan(int type) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("startPerodicScan, type=");
        stringBuilder.append(type);
        LogUtil.i(stringBuilder.toString());
        long offsetScanDuration = System.currentTimeMillis() - this.timeLastScan;
        if (0 == this.time1stCollect) {
            this.time1stCollect = System.currentTimeMillis();
        } else if (System.currentTimeMillis() - this.time1stCollect > 86400000) {
            this.time1stCollect = 0;
            this.iStallScanDailyCnt = 0;
            this.iBgScanDailyCnt = 0;
            this.iOut4gScanDailyCnt = 0;
        }
        StringBuilder stringBuilder2;
        if (2 == type) {
            if (this.iStallScanCnt == 0) {
                if (offsetScanDuration > ((long) this.param.getActScanLimit_interval()) && this.mwifimanager != null) {
                    this.mwifimanager.startScan();
                    this.iStallScanDailyCnt++;
                }
                if (this.iBgScanCnt > 0) {
                    this.iStallScanCnt = this.iBgScanCnt;
                    this.activeCltHandler.removeCallbacks(this.activeScanTimer);
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(" BACKGROUND scan was already started, BgScanCnt=");
                    stringBuilder2.append(this.iBgScanCnt);
                    LogUtil.d(stringBuilder2.toString());
                }
                if (this.iStallScanCnt < this.paraPeriodStallScan.length) {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(" run STALL active scan timer, StallScanCnt=");
                    stringBuilder2.append(this.iStallScanCnt);
                    stringBuilder2.append(", next period=");
                    stringBuilder2.append(this.paraPeriodStallScan[this.iStallScanCnt]);
                    LogUtil.i(stringBuilder2.toString());
                    this.activeCltHandler.postDelayed(this.activeScanTimer, (long) this.paraPeriodStallScan[this.iStallScanCnt]);
                }
                this.iStallScanCnt++;
            } else {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append(" periodical STALL scan was already started, StallScanCnt=");
                stringBuilder2.append(this.iStallScanCnt);
                stringBuilder2.append(", BgScanCnt=");
                stringBuilder2.append(this.iBgScanCnt);
                LogUtil.d(stringBuilder2.toString());
            }
        } else if (1 == type) {
            if (this.iStallScanCnt + this.iBgScanCnt == 0) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append(" run BACKGROUND active scan timer, BgScanCnt=");
                stringBuilder2.append(this.iBgScanCnt);
                stringBuilder2.append(", next period=");
                stringBuilder2.append(this.paraPeriodBgScan[this.iBgScanCnt]);
                LogUtil.i(stringBuilder2.toString());
                this.activeCltHandler.postDelayed(this.activeScanTimer, (long) this.paraPeriodBgScan[this.iBgScanCnt]);
                this.iBgScanCnt++;
            } else {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append(" periodical scan was already started, StallScanCnt=");
                stringBuilder2.append(this.iStallScanCnt);
                stringBuilder2.append(", BgScanCnt=");
                stringBuilder2.append(this.iBgScanCnt);
                LogUtil.d(stringBuilder2.toString());
            }
        } else if (3 == type) {
            if (this.iOut4gScanCnt == 0) {
                if (offsetScanDuration > ((long) this.param.getActScanLimit_interval()) && this.mwifimanager != null) {
                    this.mwifimanager.startScan();
                    this.iOut4gScanDailyCnt++;
                }
                if (this.iOut4gScanCnt < this.paraPeriodOut4gScan.length) {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(" run STALL active scan timer, Out4gScanCnt=");
                    stringBuilder2.append(this.iOut4gScanCnt);
                    stringBuilder2.append(", next period=");
                    stringBuilder2.append(this.paraPeriodOut4gScan[this.iOut4gScanCnt]);
                    LogUtil.i(stringBuilder2.toString());
                    this.activeCltHandler.postDelayed(this.activeOut4gScanTimer, (long) this.paraPeriodOut4gScan[this.iOut4gScanCnt]);
                }
                this.iOut4gScanCnt++;
            } else {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append(" periodical STALL scan was already started, StallScanCnt=");
                stringBuilder2.append(this.iOut4gScanCnt);
                LogUtil.d(stringBuilder2.toString());
            }
        }
        return true;
    }

    public boolean startOut4gRecgScan() {
        LogUtil.i("startOut4gRecgScan:");
        if (this.iOut4gScanDailyCnt > this.param.getActScanLimit_stall()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(" active scan reach limitation, Out4gScanDailyCnt=");
            stringBuilder.append(this.iOut4gScanDailyCnt);
            LogUtil.d(stringBuilder.toString());
        } else {
            BehaviorReceiver behaviorReceiver = this.mBehaviorHandler;
            if (BehaviorReceiver.getScrnState()) {
                LogUtil.i(" screen on, start active OUT-OF-4G scan");
                startPerodicScan(3);
                return true;
            }
        }
        this.timeLastScan = System.currentTimeMillis();
        return false;
    }

    public void stopOut4gRecgScan() {
        LogUtil.i("stopOut4gRecgScan:");
        this.iOut4gScanCnt = 0;
        this.activeCltHandler.removeCallbacks(this.activeOut4gScanTimer);
    }
}
