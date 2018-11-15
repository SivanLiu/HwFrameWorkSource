package com.huawei.nb.utils.reporter.audit;

import com.huawei.android.util.IMonitorEx;
import com.huawei.android.util.IMonitorEx.EventStreamEx;
import com.huawei.nb.utils.reporter.Reporter;

public class DataBaseSizeAudit extends Audit {
    private static final int EVENT_ID = 942010006;
    private static final short PARAM_ID_DATABASE = (short) 1;
    private static final short PARAM_ID_INTERVAL = (short) 0;
    private static final short PARAM_ID_SIZE = (short) 2;
    private final String database;
    private final int interval;
    private final int size;

    private DataBaseSizeAudit(String database, int size, int interval) {
        super(EVENT_ID);
        this.database = database;
        this.size = size;
        this.interval = interval;
    }

    public EventStreamEx createEventStream() {
        EventStreamEx eventStreamEx = IMonitorEx.openEventStream(EVENT_ID);
        if (eventStreamEx != null) {
            eventStreamEx.setParam(eventStreamEx, PARAM_ID_INTERVAL, this.interval);
            eventStreamEx.setParam(eventStreamEx, PARAM_ID_DATABASE, this.database);
            eventStreamEx.setParam(eventStreamEx, PARAM_ID_SIZE, this.size);
        }
        return eventStreamEx;
    }

    public static void report(String database, int size, int interval) {
        Reporter.a(new DataBaseSizeAudit(database, size, interval));
    }
}
