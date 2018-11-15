package com.huawei.nb.utils.reporter.audit;

import com.huawei.android.util.IMonitorEx;
import com.huawei.android.util.IMonitorEx.EventStreamEx;
import com.huawei.nb.utils.reporter.Reporter;

public class CoordinatorSwitchChangeAudit extends Audit {
    private static final int EVENT_ID_SWITCHCHANGE_EVENT = 942010104;
    private static final short SWITCHCHANGE_EVENT_STATE_VARCHAR = (short) 0;
    private static final short SWITCHCHANGE_EVENT_TRIGGER_VARCHAR = (short) 3;
    private static final short SWITCHCHANGE_EVENT_URL_VARCHAR = (short) 1;
    private static final short SWITCHCHANGE_EVENT_VERSION_VARCHAR = (short) 2;
    private String stateInfo = null;
    private String triggerInfo = null;
    private String urlInfo = null;
    private String versionInfo = null;

    public CoordinatorSwitchChangeAudit(String stateInfo, String urlInfo, String versionInfo, String triggerInfo) {
        super(EVENT_ID_SWITCHCHANGE_EVENT);
        this.stateInfo = stateInfo;
        this.urlInfo = urlInfo;
        this.versionInfo = versionInfo;
        this.triggerInfo = triggerInfo;
    }

    public EventStreamEx createEventStream() {
        EventStreamEx eventStreamEx = IMonitorEx.openEventStream(EVENT_ID_SWITCHCHANGE_EVENT);
        if (eventStreamEx != null) {
            eventStreamEx.setParam(eventStreamEx, SWITCHCHANGE_EVENT_STATE_VARCHAR, this.stateInfo);
            eventStreamEx.setParam(eventStreamEx, SWITCHCHANGE_EVENT_URL_VARCHAR, this.urlInfo);
            eventStreamEx.setParam(eventStreamEx, SWITCHCHANGE_EVENT_VERSION_VARCHAR, this.versionInfo);
            eventStreamEx.setParam(eventStreamEx, SWITCHCHANGE_EVENT_TRIGGER_VARCHAR, this.triggerInfo);
        }
        return eventStreamEx;
    }

    public static void report(String stateInfo, String urlInfo, String versionInfo, String triggerInfo) {
        Reporter.a(new CoordinatorSwitchChangeAudit(stateInfo, urlInfo, versionInfo, triggerInfo));
    }
}
