package com.huawei.nb.utils.reporter.audit;

import com.huawei.android.util.IMonitorEx;
import com.huawei.nb.utils.reporter.Reporter;
import java.util.ArrayList;
import java.util.List;

public class CoordinatorNetworkErrorAudit extends Audit {
    private static final int EVENT_ID_INTERACTION_EVENT = 942010107;
    private List<String> fieldList = new ArrayList();

    public CoordinatorNetworkErrorAudit() {
        super(EVENT_ID_INTERACTION_EVENT);
    }

    @Override // com.huawei.nb.utils.reporter.audit.Audit
    public IMonitorEx.EventStreamEx createEventStream() {
        IMonitorEx.EventStreamEx eventStreamEx = IMonitorEx.openEventStream((int) EVENT_ID_INTERACTION_EVENT);
        short fieldListSize = (short) this.fieldList.size();
        if (eventStreamEx != null) {
            for (short i = 0; i < fieldListSize; i = (short) (i + 1)) {
                eventStreamEx.setParam(eventStreamEx, i, this.fieldList.get(i));
            }
        }
        return eventStreamEx;
    }

    public static void report(CoordinatorNetworkErrorAudit audit) {
        Reporter.auditWithoutDuplicate(audit);
    }

    public void addStatusCodeToList(String statusCode) {
        this.fieldList.add(statusCode);
    }

    public void addResponseMessageToList(String responseMessage) {
        this.fieldList.add(responseMessage);
    }

    public void addPackageNameToList(String packageName) {
        this.fieldList.add(packageName);
    }

    public void addUrlToList(String url) {
        this.fieldList.add(url);
    }

    public void addNetworkToList(String network) {
        this.fieldList.add(network);
    }

    public void addDateToList(String date) {
        this.fieldList.add(date);
    }
}
