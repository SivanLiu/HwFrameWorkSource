package com.huawei.nb.utils.reporter.audit;

import com.huawei.android.util.IMonitorEx;
import com.huawei.nb.utils.reporter.Reporter;

public final class DataBaseAccessAudit extends Audit {
    private static final int EVENT_ID = 942010005;
    private static final short PARAM_ID_DATABASE = 1;
    private static final short PARAM_ID_DETAIL = 2;
    private static final short PARAM_ID_INTERVAL = 0;
    private final String database;
    private final String detail;
    private final long interval;

    private DataBaseAccessAudit(String database2, String detail2, long interval2) {
        super(EVENT_ID);
        this.database = database2;
        this.detail = detail2;
        this.interval = interval2;
    }

    @Override // com.huawei.nb.utils.reporter.audit.Audit
    public IMonitorEx.EventStreamEx createEventStream() {
        IMonitorEx.EventStreamEx eventStreamEx = IMonitorEx.openEventStream((int) EVENT_ID);
        if (eventStreamEx != null) {
            eventStreamEx.setParam(eventStreamEx, 0, this.interval);
            eventStreamEx.setParam(eventStreamEx, (short) PARAM_ID_DATABASE, this.database);
            eventStreamEx.setParam(eventStreamEx, (short) PARAM_ID_DETAIL, this.detail);
        }
        return eventStreamEx;
    }

    public static void report(String database2, String detail2, long interval2) {
        Reporter.a(new DataBaseAccessAudit(database2, detail2, interval2));
    }
}
