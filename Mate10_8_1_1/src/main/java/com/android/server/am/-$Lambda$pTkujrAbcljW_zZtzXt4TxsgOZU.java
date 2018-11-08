package com.android.server.am;

import java.util.concurrent.ThreadFactory;

final /* synthetic */ class -$Lambda$pTkujrAbcljW_zZtzXt4TxsgOZU implements ThreadFactory {
    public static final /* synthetic */ -$Lambda$pTkujrAbcljW_zZtzXt4TxsgOZU $INST$0 = new -$Lambda$pTkujrAbcljW_zZtzXt4TxsgOZU();

    private final /* synthetic */ Thread $m$0(Runnable arg0) {
        return BatteryExternalStatsWorker.lambda$-com_android_server_am_BatteryExternalStatsWorker_2654(arg0);
    }

    private /* synthetic */ -$Lambda$pTkujrAbcljW_zZtzXt4TxsgOZU() {
    }

    public final Thread newThread(Runnable runnable) {
        return $m$0(runnable);
    }
}
