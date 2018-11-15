package com.huawei.nb.utils.reporter.audit;

import com.huawei.android.util.IMonitorEx;
import com.huawei.android.util.IMonitorEx.EventStreamEx;
import com.huawei.nb.utils.reporter.Reporter;

public class CoordinatorInteractionAudit extends Audit {
    private static final int EVENT_ID_INTERACTION_EVENT = 942010105;
    private static final short INTERACTION_EVENT_AFTERNOONSUCCESSTIME = (short) 6;
    private static final short INTERACTION_EVENT_AFTERNOON_FAIL_TIME = (short) 11;
    private static final short INTERACTION_EVENT_APP_PACKAGE = (short) 0;
    private static final short INTERACTION_EVENT_DATE = (short) 3;
    private static final short INTERACTION_EVENT_MIDNIGHTSUCCESSTIME = (short) 8;
    private static final short INTERACTION_EVENT_MIDNIGHT_FAIL_TIME = (short) 13;
    private static final short INTERACTION_EVENT_MORNINGSUCCESSTIME = (short) 4;
    private static final short INTERACTION_EVENT_MORNING_FAIL_TIME = (short) 9;
    private static final short INTERACTION_EVENT_NEED_RETRY_TIME = (short) 14;
    private static final short INTERACTION_EVENT_NETWORKSTATE = (short) 2;
    private static final short INTERACTION_EVENT_NIGHTSUCCESSTIME = (short) 7;
    private static final short INTERACTION_EVENT_NIGHT_FAIL_TIME = (short) 12;
    private static final short INTERACTION_EVENT_NOONSUCCESSTIME = (short) 5;
    private static final short INTERACTION_EVENT_NOON_FAIL_TIME = (short) 10;
    private static final short INTERACTION_EVENT_TRANSFER_AVERAGE_DATASIZE = (short) 19;
    private static final short INTERACTION_EVENT_TRANSFER_AVERAGE_TIME = (short) 17;
    private static final short INTERACTION_EVENT_TRANSFER_MAX_DATASIZE = (short) 20;
    private static final short INTERACTION_EVENT_TRANSFER_MAX_TIME = (short) 18;
    private static final short INTERACTION_EVENT_URL = (short) 1;
    private static final short INTERACTION_EVENT_VERIFY_AVERAGE_TIME = (short) 15;
    private static final short INTERACTION_EVENT_VERIFY_MAX_TIME = (short) 16;
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

    private CoordinatorInteractionAudit(String packageName, String url, String netWorkState, String date, long morningSuccessTime, long noonSuccessTime, long afternoonSuccessTime, long nightSuccessTime, long midnightSuccessTime, long morningFailTime, long noonFailTime, long afternoonFailTime, long nightFailTime, long midnightFailTime, long needRetryTime, long verifyAverageTime, long verifyMaxTime, long transferAverageTime, long tranferMaxTime, long transferAverageDatasize, long transferMaxDatasize) {
        super(EVENT_ID_INTERACTION_EVENT);
        this.packageName = packageName;
        this.url = url;
        this.netWorkState = netWorkState;
        this.date = date;
        this.morningSuccessTime = morningSuccessTime;
        this.noonSuccessTime = noonSuccessTime;
        this.afternoonSuccessTime = afternoonSuccessTime;
        this.nightSuccessTime = nightSuccessTime;
        this.midnightSuccessTime = midnightSuccessTime;
        this.morningFailTime = morningFailTime;
        this.noonFailTime = noonFailTime;
        this.afternoonFailTime = afternoonFailTime;
        this.nightFailTime = nightFailTime;
        this.midnightFailTime = midnightFailTime;
        this.needRetryTime = needRetryTime;
        this.verifyAverageTime = verifyAverageTime;
        this.verifyMaxTime = verifyMaxTime;
        this.transferAverageTime = transferAverageTime;
        this.tranferMaxTime = tranferMaxTime;
        this.transferAverageDatasize = transferAverageDatasize;
        this.transferMaxDatasize = transferMaxDatasize;
    }

    public EventStreamEx createEventStream() {
        EventStreamEx eventStreamEx = IMonitorEx.openEventStream(EVENT_ID_INTERACTION_EVENT);
        if (eventStreamEx != null) {
            eventStreamEx.setParam(eventStreamEx, INTERACTION_EVENT_APP_PACKAGE, this.packageName);
            eventStreamEx.setParam(eventStreamEx, INTERACTION_EVENT_URL, this.url);
            eventStreamEx.setParam(eventStreamEx, INTERACTION_EVENT_NETWORKSTATE, this.netWorkState);
            eventStreamEx.setParam(eventStreamEx, INTERACTION_EVENT_DATE, this.date);
            eventStreamEx.setParam(eventStreamEx, INTERACTION_EVENT_MORNINGSUCCESSTIME, this.morningSuccessTime);
            eventStreamEx.setParam(eventStreamEx, INTERACTION_EVENT_NOONSUCCESSTIME, this.noonSuccessTime);
            eventStreamEx.setParam(eventStreamEx, INTERACTION_EVENT_AFTERNOONSUCCESSTIME, this.afternoonSuccessTime);
            eventStreamEx.setParam(eventStreamEx, INTERACTION_EVENT_NIGHTSUCCESSTIME, this.nightSuccessTime);
            eventStreamEx.setParam(eventStreamEx, INTERACTION_EVENT_MIDNIGHTSUCCESSTIME, this.midnightSuccessTime);
            eventStreamEx.setParam(eventStreamEx, INTERACTION_EVENT_MORNING_FAIL_TIME, this.morningFailTime);
            eventStreamEx.setParam(eventStreamEx, INTERACTION_EVENT_NOON_FAIL_TIME, this.noonFailTime);
            eventStreamEx.setParam(eventStreamEx, INTERACTION_EVENT_AFTERNOON_FAIL_TIME, this.afternoonFailTime);
            eventStreamEx.setParam(eventStreamEx, INTERACTION_EVENT_NIGHT_FAIL_TIME, this.nightFailTime);
            eventStreamEx.setParam(eventStreamEx, INTERACTION_EVENT_MIDNIGHT_FAIL_TIME, this.midnightFailTime);
            eventStreamEx.setParam(eventStreamEx, INTERACTION_EVENT_NEED_RETRY_TIME, this.needRetryTime);
            eventStreamEx.setParam(eventStreamEx, INTERACTION_EVENT_VERIFY_AVERAGE_TIME, this.verifyAverageTime);
            eventStreamEx.setParam(eventStreamEx, INTERACTION_EVENT_VERIFY_MAX_TIME, this.verifyMaxTime);
            eventStreamEx.setParam(eventStreamEx, INTERACTION_EVENT_TRANSFER_AVERAGE_TIME, this.transferAverageTime);
            eventStreamEx.setParam(eventStreamEx, INTERACTION_EVENT_TRANSFER_MAX_TIME, this.tranferMaxTime);
            eventStreamEx.setParam(eventStreamEx, INTERACTION_EVENT_TRANSFER_AVERAGE_DATASIZE, this.transferAverageDatasize);
            eventStreamEx.setParam(eventStreamEx, INTERACTION_EVENT_TRANSFER_MAX_DATASIZE, this.transferMaxDatasize);
        }
        return eventStreamEx;
    }

    public static void report(String packageName, String url, String netWorkState, String date, long morningSuccessTime, long noonSuccessTime, long afternoonSuccessTime, long nightSuccessTime, long midnightSuccessTime, long morningFailTime, long noonFailTime, long afternoonFailTime, long nightFailTime, long midnightFailTime, long needRetryTime, long verifyAverageTime, long verifyMaxTime, long transferAverageTime, long tranferMaxTime, long transferAverageDatasize, long transferMaxDatasize) {
        Reporter.a(new CoordinatorInteractionAudit(packageName, url, netWorkState, date, morningSuccessTime, noonSuccessTime, afternoonSuccessTime, nightSuccessTime, midnightSuccessTime, morningFailTime, noonFailTime, afternoonFailTime, nightFailTime, midnightFailTime, needRetryTime, verifyAverageTime, verifyMaxTime, transferAverageTime, tranferMaxTime, transferAverageDatasize, transferMaxDatasize));
    }
}
