package com.huawei.nb.utils.reporter.audit;

import com.huawei.android.util.IMonitorEx;
import com.huawei.nb.utils.reporter.Reporter;

public final class CoordinatorInteractionAudit extends Audit {
    private static final int EVENT_ID_INTERACTION_EVENT = 942010105;
    private static final short INTERACTION_EVENT_AFTERNOONSUCCESSTIME = 6;
    private static final short INTERACTION_EVENT_AFTERNOON_FAIL_TIME = 11;
    private static final short INTERACTION_EVENT_APP_PACKAGE = 0;
    private static final short INTERACTION_EVENT_DATE = 3;
    private static final short INTERACTION_EVENT_MIDNIGHTSUCCESSTIME = 8;
    private static final short INTERACTION_EVENT_MIDNIGHT_FAIL_TIME = 13;
    private static final short INTERACTION_EVENT_MORNINGSUCCESSTIME = 4;
    private static final short INTERACTION_EVENT_MORNING_FAIL_TIME = 9;
    private static final short INTERACTION_EVENT_NEED_RETRY_TIME = 14;
    private static final short INTERACTION_EVENT_NETWORKSTATE = 2;
    private static final short INTERACTION_EVENT_NIGHTSUCCESSTIME = 7;
    private static final short INTERACTION_EVENT_NIGHT_FAIL_TIME = 12;
    private static final short INTERACTION_EVENT_NOONSUCCESSTIME = 5;
    private static final short INTERACTION_EVENT_NOON_FAIL_TIME = 10;
    private static final short INTERACTION_EVENT_TRANSFER_AVERAGE_DATASIZE = 19;
    private static final short INTERACTION_EVENT_TRANSFER_AVERAGE_TIME = 17;
    private static final short INTERACTION_EVENT_TRANSFER_MAX_DATASIZE = 20;
    private static final short INTERACTION_EVENT_TRANSFER_MAX_TIME = 18;
    private static final short INTERACTION_EVENT_URL = 1;
    private static final short INTERACTION_EVENT_VERIFY_AVERAGE_TIME = 15;
    private static final short INTERACTION_EVENT_VERIFY_MAX_TIME = 16;
    private long afternoonFailTime;
    private long afternoonSuccessTime;
    private String date = null;
    private long midnightFailTime;
    private long midnightSuccessTime;
    private long morningFailTime;
    private long morningSuccessTime;
    private long needRetryTime;
    private String netWorkState = null;
    private long nightFailTime;
    private long nightSuccessTime;
    private long noonFailTime;
    private long noonSuccessTime;
    private String packageName = null;
    private long tranferMaxTime;
    private long transferAverageDatasize;
    private long transferAverageTime;
    private long transferMaxDatasize;
    private String url = null;
    private long verifyAverageTime;
    private long verifyMaxTime;

    public CoordinatorInteractionAudit() {
        super(EVENT_ID_INTERACTION_EVENT);
    }

    @Override // com.huawei.nb.utils.reporter.audit.Audit
    public IMonitorEx.EventStreamEx createEventStream() {
        IMonitorEx.EventStreamEx eventStreamEx = IMonitorEx.openEventStream((int) EVENT_ID_INTERACTION_EVENT);
        if (eventStreamEx != null) {
            eventStreamEx.setParam(eventStreamEx, (short) INTERACTION_EVENT_APP_PACKAGE, this.packageName);
            eventStreamEx.setParam(eventStreamEx, (short) INTERACTION_EVENT_URL, this.url);
            eventStreamEx.setParam(eventStreamEx, (short) INTERACTION_EVENT_NETWORKSTATE, this.netWorkState);
            eventStreamEx.setParam(eventStreamEx, (short) INTERACTION_EVENT_DATE, this.date);
            eventStreamEx.setParam(eventStreamEx, (short) INTERACTION_EVENT_MORNINGSUCCESSTIME, this.morningSuccessTime);
            eventStreamEx.setParam(eventStreamEx, (short) INTERACTION_EVENT_NOONSUCCESSTIME, this.noonSuccessTime);
            eventStreamEx.setParam(eventStreamEx, (short) INTERACTION_EVENT_AFTERNOONSUCCESSTIME, this.afternoonSuccessTime);
            eventStreamEx.setParam(eventStreamEx, (short) INTERACTION_EVENT_NIGHTSUCCESSTIME, this.nightSuccessTime);
            eventStreamEx.setParam(eventStreamEx, (short) INTERACTION_EVENT_MIDNIGHTSUCCESSTIME, this.midnightSuccessTime);
            eventStreamEx.setParam(eventStreamEx, (short) INTERACTION_EVENT_MORNING_FAIL_TIME, this.morningFailTime);
            eventStreamEx.setParam(eventStreamEx, (short) INTERACTION_EVENT_NOON_FAIL_TIME, this.noonFailTime);
            eventStreamEx.setParam(eventStreamEx, (short) INTERACTION_EVENT_AFTERNOON_FAIL_TIME, this.afternoonFailTime);
            eventStreamEx.setParam(eventStreamEx, (short) INTERACTION_EVENT_NIGHT_FAIL_TIME, this.nightFailTime);
            eventStreamEx.setParam(eventStreamEx, (short) INTERACTION_EVENT_MIDNIGHT_FAIL_TIME, this.midnightFailTime);
            eventStreamEx.setParam(eventStreamEx, (short) INTERACTION_EVENT_NEED_RETRY_TIME, this.needRetryTime);
            eventStreamEx.setParam(eventStreamEx, (short) INTERACTION_EVENT_VERIFY_AVERAGE_TIME, this.verifyAverageTime);
            eventStreamEx.setParam(eventStreamEx, (short) INTERACTION_EVENT_VERIFY_MAX_TIME, this.verifyMaxTime);
            eventStreamEx.setParam(eventStreamEx, (short) INTERACTION_EVENT_TRANSFER_AVERAGE_TIME, this.transferAverageTime);
            eventStreamEx.setParam(eventStreamEx, (short) INTERACTION_EVENT_TRANSFER_MAX_TIME, this.tranferMaxTime);
            eventStreamEx.setParam(eventStreamEx, (short) INTERACTION_EVENT_TRANSFER_AVERAGE_DATASIZE, this.transferAverageDatasize);
            eventStreamEx.setParam(eventStreamEx, (short) INTERACTION_EVENT_TRANSFER_MAX_DATASIZE, this.transferMaxDatasize);
        }
        return eventStreamEx;
    }

    public static void report(CoordinatorInteractionAudit audit) {
        Reporter.a(audit);
    }

    public String getPackageName() {
        return this.packageName;
    }

    public void setPackageName(String packageName2) {
        this.packageName = packageName2;
    }

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String url2) {
        this.url = url2;
    }

    public String getNetWorkState() {
        return this.netWorkState;
    }

    public void setNetWorkState(String netWorkState2) {
        this.netWorkState = netWorkState2;
    }

    public String getDate() {
        return this.date;
    }

    public void setDate(String date2) {
        this.date = date2;
    }

    public long getMorningSuccessTime() {
        return this.morningSuccessTime;
    }

    public void setMorningSuccessTime(long morningSuccessTime2) {
        this.morningSuccessTime = morningSuccessTime2;
    }

    public long getNoonSuccessTime() {
        return this.noonSuccessTime;
    }

    public void setNoonSuccessTime(long noonSuccessTime2) {
        this.noonSuccessTime = noonSuccessTime2;
    }

    public long getAfternoonSuccessTime() {
        return this.afternoonSuccessTime;
    }

    public void setAfternoonSuccessTime(long afternoonSuccessTime2) {
        this.afternoonSuccessTime = afternoonSuccessTime2;
    }

    public long getNightSuccessTime() {
        return this.nightSuccessTime;
    }

    public void setNightSuccessTime(long nightSuccessTime2) {
        this.nightSuccessTime = nightSuccessTime2;
    }

    public long getMidnightSuccessTime() {
        return this.midnightSuccessTime;
    }

    public void setMidnightSuccessTime(long midnightSuccessTime2) {
        this.midnightSuccessTime = midnightSuccessTime2;
    }

    public long getMorningFailTime() {
        return this.morningFailTime;
    }

    public void setMorningFailTime(long morningFailTime2) {
        this.morningFailTime = morningFailTime2;
    }

    public long getNoonFailTime() {
        return this.noonFailTime;
    }

    public void setNoonFailTime(long noonFailTime2) {
        this.noonFailTime = noonFailTime2;
    }

    public long getAfternoonFailTime() {
        return this.afternoonFailTime;
    }

    public void setAfternoonFailTime(long afternoonFailTime2) {
        this.afternoonFailTime = afternoonFailTime2;
    }

    public long getNightFailTime() {
        return this.nightFailTime;
    }

    public void setNightFailTime(long nightFailTime2) {
        this.nightFailTime = nightFailTime2;
    }

    public long getMidnightFailTime() {
        return this.midnightFailTime;
    }

    public void setMidnightFailTime(long midnightFailTime2) {
        this.midnightFailTime = midnightFailTime2;
    }

    public long getNeedRetryTime() {
        return this.needRetryTime;
    }

    public void setNeedRetryTime(long needRetryTime2) {
        this.needRetryTime = needRetryTime2;
    }

    public long getVerifyAverageTime() {
        return this.verifyAverageTime;
    }

    public void setVerifyAverageTime(long verifyAverageTime2) {
        this.verifyAverageTime = verifyAverageTime2;
    }

    public long getVerifyMaxTime() {
        return this.verifyMaxTime;
    }

    public void setVerifyMaxTime(long verifyMaxTime2) {
        this.verifyMaxTime = verifyMaxTime2;
    }

    public long getTransferAverageTime() {
        return this.transferAverageTime;
    }

    public void setTransferAverageTime(long transferAverageTime2) {
        this.transferAverageTime = transferAverageTime2;
    }

    public long getTranferMaxTime() {
        return this.tranferMaxTime;
    }

    public void setTranferMaxTime(long tranferMaxTime2) {
        this.tranferMaxTime = tranferMaxTime2;
    }

    public long getTransferAverageDatasize() {
        return this.transferAverageDatasize;
    }

    public void setTransferAverageDatasize(long transferAverageDatasize2) {
        this.transferAverageDatasize = transferAverageDatasize2;
    }

    public long getTransferMaxDatasize() {
        return this.transferMaxDatasize;
    }

    public void setTransferMaxDatasize(long transferMaxDatasize2) {
        this.transferMaxDatasize = transferMaxDatasize2;
    }
}
