package com.huawei.nb.utils.reporter.audit;

import com.huawei.android.util.IMonitorEx;
import com.huawei.nb.utils.reporter.Reporter;

public class DataBaseSizeAudit extends Audit {
    private static final int EVENT_ID = 942010006;
    private static final short PARAM_ID_DATABASE = 1;
    private static final short PARAM_ID_INTERVAL = 0;
    private static final short PARAM_ID_SIZE = 2;
    private final String database;
    private final long interval;
    private final long size;

    private DataBaseSizeAudit(String database2, long size2, long interval2) {
        super(EVENT_ID);
        this.database = database2;
        this.size = size2;
        this.interval = interval2;
    }

    @Override // com.huawei.nb.utils.reporter.audit.Audit
    public IMonitorEx.EventStreamEx createEventStream() {
        IMonitorEx.EventStreamEx eventStreamEx = IMonitorEx.openEventStream((int) EVENT_ID);
        if (eventStreamEx != null) {
            eventStreamEx.setParam(eventStreamEx, (short) PARAM_ID_INTERVAL, this.interval);
            eventStreamEx.setParam(eventStreamEx, (short) PARAM_ID_DATABASE, this.database);
            eventStreamEx.setParam(eventStreamEx, (short) PARAM_ID_SIZE, this.size);
        }
        return eventStreamEx;
    }

    public static void report(String database2, long size2, long interval2) {
        Reporter.a(new DataBaseSizeAudit(database2, size2, interval2));
    }
}
