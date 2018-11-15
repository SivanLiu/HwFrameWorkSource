package com.huawei.nb.utils.reporter.audit;

import com.huawei.android.util.IMonitorEx;
import com.huawei.android.util.IMonitorEx.EventStreamEx;
import com.huawei.nb.utils.reporter.Reporter;

public class DataBaseAccessAudit extends Audit {
    private static final int EVENT_ID = 942010005;
    private static final short PARAM_ID_DATABASE = (short) 1;
    private static final short PARAM_ID_DETAIL = (short) 2;
    private static final short PARAM_ID_INTERVAL = (short) 0;
    private final String database;
    private final String detail;
    private final int interval;

    private DataBaseAccessAudit(String database, String detail, int interval) {
        super(EVENT_ID);
        this.database = database;
        this.detail = detail;
        this.interval = interval;
    }

    public EventStreamEx createEventStream() {
        EventStreamEx eventStreamEx = IMonitorEx.openEventStream(EVENT_ID);
        if (eventStreamEx != null) {
            eventStreamEx.setParam(eventStreamEx, (short) 0, this.interval);
            eventStreamEx.setParam(eventStreamEx, PARAM_ID_DATABASE, this.database);
            eventStreamEx.setParam(eventStreamEx, PARAM_ID_DETAIL, this.detail);
        }
        return eventStreamEx;
    }

    public static void report(String database, String detail, int interval) {
        Reporter.a(new DataBaseAccessAudit(database, detail, interval));
    }
}
