package com.android.server;

import android.net.NetworkScorerAppData;
import com.android.server.NetworkScoreService.ScoringServiceConnection;
import java.util.function.Function;

final /* synthetic */ class -$Lambda$Ash-36Gr90yYPZEIENlguvJE7uk implements Function {
    public static final /* synthetic */ -$Lambda$Ash-36Gr90yYPZEIENlguvJE7uk $INST$0 = new -$Lambda$Ash-36Gr90yYPZEIENlguvJE7uk();

    private final /* synthetic */ Object $m$0(Object arg0) {
        return new ScoringServiceConnection((NetworkScorerAppData) arg0);
    }

    private /* synthetic */ -$Lambda$Ash-36Gr90yYPZEIENlguvJE7uk() {
    }

    public final Object apply(Object obj) {
        return $m$0(obj);
    }
}
