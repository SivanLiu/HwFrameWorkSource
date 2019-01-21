package com.android.internal.telephony;

import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.SystemProperties;
import android.telephony.CarrierConfigManager;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import com.android.internal.telephony.Call.SrvccState;
import com.android.internal.telephony.CommandException.Error;
import com.android.internal.telephony.PhoneConstants.State;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;

public abstract class CallTracker extends Handler {
    private static final boolean DBG_POLL = false;
    protected static final int EVENT_CALL_STATE_CHANGE = 2;
    protected static final int EVENT_CALL_WAITING_INFO_CDMA = 15;
    protected static final int EVENT_CONFERENCE_RESULT = 11;
    protected static final int EVENT_ECT_RESULT = 13;
    protected static final int EVENT_EXIT_ECM_RESPONSE_CDMA = 14;
    protected static final int EVENT_GET_LAST_CALL_FAIL_CAUSE = 5;
    protected static final int EVENT_OPERATION_COMPLETE = 4;
    protected static final int EVENT_POLL_CALLS_RESULT = 1;
    protected static final int EVENT_RADIO_AVAILABLE = 9;
    protected static final int EVENT_RADIO_NOT_AVAILABLE = 10;
    protected static final int EVENT_REPOLL_AFTER_DELAY = 3;
    protected static final int EVENT_RIL_RECOVERY = 201;
    protected static final int EVENT_RSRVCC_STATE_CHANGED = 50;
    protected static final int EVENT_SEPARATE_RESULT = 12;
    protected static final int EVENT_SWITCH_RESULT = 8;
    protected static final int EVENT_THREE_WAY_DIAL_BLANK_FLASH = 20;
    protected static final int EVENT_THREE_WAY_DIAL_L2_RESULT_CDMA = 16;
    public static final boolean IS_SUPPORT_RIL_RECOVERY = HwModemCapability.isCapabilitySupport(8);
    protected static final int MAX_END_CALL_DURATION = 35000;
    static final int POLL_DELAY_MSEC = 250;
    private static final int VALID_COMPARE_LENGTH = 3;
    protected static final boolean mIsShowRedirectNumber = SystemProperties.get("ro.product.custom", "NULL").contains("docomo");
    public CommandsInterface mCi;
    protected ArrayList<Connection> mHandoverConnections = new ArrayList();
    protected boolean mIsSrvccHappened;
    protected Message mLastRelevantPoll;
    protected boolean mNeedsPoll;
    protected boolean mNumberConverted = false;
    protected int mPendingOperations;
    protected ArrayList<Connection> mRemovedHandoverConnections = new ArrayList();
    protected SrvccState mSrvccState = SrvccState.NONE;

    public abstract State getState();

    public abstract void handleMessage(Message message);

    protected abstract void handlePollCalls(AsyncResult asyncResult);

    protected abstract void log(String str);

    public abstract void registerForVoiceCallEnded(Handler handler, int i, Object obj);

    public abstract void registerForVoiceCallStarted(Handler handler, int i, Object obj);

    public abstract void unregisterForVoiceCallEnded(Handler handler);

    public abstract void unregisterForVoiceCallStarted(Handler handler);

    protected void pollCallsWhenSafe() {
        this.mNeedsPoll = true;
        if (checkNoOperationsPending()) {
            this.mLastRelevantPoll = obtainMessage(1);
            if (mIsShowRedirectNumber) {
                this.mCi.getCurrentCallsEx(this.mLastRelevantPoll);
            } else {
                this.mCi.getCurrentCalls(this.mLastRelevantPoll);
            }
        }
    }

    protected void pollCallsAfterDelay() {
        Message msg = obtainMessage();
        msg.what = 3;
        sendMessageDelayed(msg, 250);
    }

    protected boolean isCommandExceptionRadioNotAvailable(Throwable e) {
        return e != null && (e instanceof CommandException) && ((CommandException) e).getCommandError() == Error.RADIO_NOT_AVAILABLE;
    }

    protected Connection getHoConnection(DriverCall dc) {
        Connection hoConn;
        StringBuilder stringBuilder;
        StringBuilder stringBuilder2;
        Iterator it = this.mHandoverConnections.iterator();
        while (it.hasNext()) {
            hoConn = (Connection) it.next();
            stringBuilder = new StringBuilder();
            stringBuilder.append("getHoConnection - compare number: hoConn= ");
            stringBuilder.append(hoConn.toString());
            log(stringBuilder.toString());
            if (hoConn.getAddress() != null && !"".equals(dc.number) && hoConn.getAddress().contains(dc.number)) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("getHoConnection: Handover connection match found = ");
                stringBuilder2.append(hoConn.toString());
                log(stringBuilder2.toString());
                return hoConn;
            }
        }
        it = this.mHandoverConnections.iterator();
        while (it.hasNext()) {
            hoConn = (Connection) it.next();
            stringBuilder = new StringBuilder();
            stringBuilder.append("getHoConnection: compare state hoConn= ");
            stringBuilder.append(hoConn.toString());
            log(stringBuilder.toString());
            if (hoConn.getStateBeforeHandover() == Call.stateFromDCState(dc.state)) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("getHoConnection: Handover connection match found = ");
                stringBuilder2.append(hoConn.toString());
                log(stringBuilder2.toString());
                return hoConn;
            }
        }
        return null;
    }

    protected void notifySrvccState(SrvccState state, ArrayList<Connection> c) {
        this.mSrvccState = state;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[SRVCC] notifySrvccState -> mSrvccState:");
        stringBuilder.append(this.mSrvccState);
        log(stringBuilder.toString());
        if (state == SrvccState.STARTED && c != null) {
            this.mHandoverConnections.addAll(c);
            this.mIsSrvccHappened = true;
        } else if (state == SrvccState.COMPLETED) {
            if (c != null) {
                Iterator it = c.iterator();
                while (it.hasNext()) {
                    Connection conn = (Connection) it.next();
                    if (!(this.mHandoverConnections.contains(conn) || this.mRemovedHandoverConnections.contains(conn))) {
                        this.mHandoverConnections.add(conn);
                    }
                }
            }
            log(" [SRVCC] notifySrvccState -> clear mRemovedHandoverConnections");
            this.mRemovedHandoverConnections.clear();
        } else {
            this.mHandoverConnections.clear();
            this.mRemovedHandoverConnections.clear();
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("[SRVCC] notifySrvccState: mHandoverConnections= ");
        stringBuilder.append(this.mHandoverConnections.toString());
        log(stringBuilder.toString());
    }

    protected void handleRadioAvailable() {
        pollCallsWhenSafe();
    }

    protected Message obtainNoPollCompleteMessage(int what) {
        this.mPendingOperations++;
        this.mLastRelevantPoll = null;
        return obtainMessage(what);
    }

    private boolean checkNoOperationsPending() {
        return this.mPendingOperations == 0;
    }

    protected String checkForTestEmergencyNumber(String dialString) {
        String testEn = SystemProperties.get("ril.test.emergencynumber");
        if (TextUtils.isEmpty(testEn)) {
            return dialString;
        }
        String[] values = testEn.split(":");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("checkForTestEmergencyNumber: values.length=");
        stringBuilder.append(values.length);
        log(stringBuilder.toString());
        if (values.length != 2 || !values[0].equals(PhoneNumberUtils.stripSeparators(dialString))) {
            return dialString;
        }
        if (this.mCi != null) {
            this.mCi.testingEmergencyCall();
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("checkForTestEmergencyNumber: remap ");
        stringBuilder.append(dialString);
        stringBuilder.append(" to ");
        stringBuilder.append(values[1]);
        log(stringBuilder.toString());
        return values[1];
    }

    /* JADX WARNING: Removed duplicated region for block: B:39:0x00dd A:{RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:37:0x00d5  */
    /* JADX WARNING: Removed duplicated region for block: B:37:0x00d5  */
    /* JADX WARNING: Removed duplicated region for block: B:39:0x00dd A:{RETURN} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected String convertNumberIfNecessary(Phone phone, String dialNumber) {
        String str = dialNumber;
        if (str == null) {
            return str;
        }
        String[] convertMaps = null;
        PersistableBundle bundle = ((CarrierConfigManager) phone.getContext().getSystemService("carrier_config")).getConfig();
        if (bundle != null) {
            convertMaps = bundle.getStringArray("dial_string_replace_string_array");
        }
        if (convertMaps == null) {
            log("convertNumberIfNecessary convertMaps is null");
            return str;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("convertNumberIfNecessary Roaming convertMaps.length ");
        stringBuilder.append(convertMaps.length);
        stringBuilder.append(" dialNumber.length() ");
        stringBuilder.append(dialNumber.length());
        log(stringBuilder.toString());
        if (convertMaps.length < 1 || dialNumber.length() < 3) {
            return str;
        }
        String outNumber = "";
        for (String convertMap : convertMaps) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("convertNumberIfNecessary: ");
            stringBuilder2.append(convertMap);
            log(stringBuilder2.toString());
            String[] entry = convertMap.split(":");
            if (entry != null && entry.length > 1) {
                String dsToReplace = entry[0];
                String dsReplacement = entry[1];
                if (!TextUtils.isEmpty(dsToReplace) && str.equals(dsToReplace)) {
                    if (TextUtils.isEmpty(dsReplacement) || !dsReplacement.endsWith("MDN")) {
                        outNumber = dsReplacement;
                        if (TextUtils.isEmpty(outNumber)) {
                            return str;
                        }
                        log("convertNumberIfNecessary: convert service number");
                        this.mNumberConverted = true;
                        return outNumber;
                    }
                    String mdn = phone.getLine1Number();
                    if (!TextUtils.isEmpty(mdn)) {
                        if (mdn.startsWith("+")) {
                            outNumber = mdn;
                        } else {
                            StringBuilder stringBuilder3 = new StringBuilder();
                            stringBuilder3.append(dsReplacement.substring(0, dsReplacement.length() - 3));
                            stringBuilder3.append(mdn);
                            outNumber = stringBuilder3.toString();
                        }
                    }
                    if (TextUtils.isEmpty(outNumber)) {
                    }
                }
            }
        }
        if (TextUtils.isEmpty(outNumber)) {
        }
    }

    public void cleanRilRecovery() {
        if (IS_SUPPORT_RIL_RECOVERY && hasMessages(EVENT_RIL_RECOVERY)) {
            log("remove msg:EVENT_RIL_RECOVERY");
            removeMessages(EVENT_RIL_RECOVERY);
        }
    }

    public void delaySendRilRecoveryMsg(Call.State state) {
        if (IS_SUPPORT_RIL_RECOVERY && state != Call.State.DISCONNECTING && !hasMessages(EVENT_RIL_RECOVERY)) {
            log("delay send EVENT_RIL_RECOVERY");
            sendMessageDelayed(obtainMessage(EVENT_RIL_RECOVERY), 35000);
        }
    }

    public void cleanupCalls() {
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("CallTracker:");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(" mPendingOperations=");
        stringBuilder.append(this.mPendingOperations);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mNeedsPoll=");
        stringBuilder.append(this.mNeedsPoll);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mLastRelevantPoll=");
        stringBuilder.append(this.mLastRelevantPoll);
        pw.println(stringBuilder.toString());
    }

    public void markCallRejectCause(String telecomCallId, int cause) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("markCallRejectByUser ,telecomCallId: ");
        stringBuilder.append(telecomCallId);
        stringBuilder.append(", cause: ");
        stringBuilder.append(cause);
        log(stringBuilder.toString());
    }
}
