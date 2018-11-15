package com.huawei.nb.utils.reporter.audit;

import com.huawei.android.util.IMonitorEx.EventStreamEx;

public abstract class Audit {
    private int auditId;

    public abstract EventStreamEx createEventStream();

    protected Audit(int auditId) {
        this.auditId = auditId;
    }

    public int getAuditId() {
        return this.auditId;
    }
}
