package com.huawei.nb.coordinator.helper;

import android.content.Context;
import com.huawei.nb.model.coordinator.CoordinatorAudit;

final /* synthetic */ class HelperDatabaseManager$$Lambda$0 implements Runnable {
    private final Context arg$1;
    private final CoordinatorAudit arg$2;

    HelperDatabaseManager$$Lambda$0(Context context, CoordinatorAudit coordinatorAudit) {
        this.arg$1 = context;
        this.arg$2 = coordinatorAudit;
    }

    public void run() {
        HelperDatabaseManager.lambda$insertCoordinatorAudit$0$HelperDatabaseManager(this.arg$1, this.arg$2);
    }
}
