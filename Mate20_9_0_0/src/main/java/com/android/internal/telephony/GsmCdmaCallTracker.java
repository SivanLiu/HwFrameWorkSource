package com.android.internal.telephony;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hdm.HwDeviceManager;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.Registrant;
import android.os.RegistrantList;
import android.os.SystemProperties;
import android.os.UserManager;
import android.telephony.CarrierConfigManager;
import android.telephony.CellLocation;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.text.TextUtils;
import android.util.EventLog;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.Call.SrvccState;
import com.android.internal.telephony.CommandsInterface.RadioState;
import com.android.internal.telephony.PhoneConstants.State;
import com.android.internal.telephony.PhoneInternalInterface.SuppService;
import com.android.internal.telephony.cdma.CdmaCallWaitingNotification;
import com.android.internal.telephony.metrics.TelephonyMetrics;
import com.android.internal.telephony.vsim.VSimUtilsInner;
import huawei.cust.HwCustUtils;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GsmCdmaCallTracker extends AbstractGsmCdmaCallTracker {
    private static final boolean DBG_POLL = false;
    private static final String LOG_TAG = "GsmCdmaCallTracker";
    private static final int MAX_CONNECTIONS_CDMA = 8;
    public static final int MAX_CONNECTIONS_GSM = 19;
    private static final int MAX_CONNECTIONS_PER_CALL_CDMA = 1;
    private static final int MAX_CONNECTIONS_PER_CALL_GSM = 5;
    private static final boolean REPEAT_POLLING = false;
    private static final boolean VDBG = false;
    boolean callSwitchPending = false;
    RegistrantList cdmaWaitingNumberChangedRegistrants = new RegistrantList();
    private int m3WayCallFlashDelay;
    public GsmCdmaCall mBackgroundCall = new GsmCdmaCall(this);
    private RegistrantList mCallWaitingRegistrants = new RegistrantList();
    @VisibleForTesting
    public GsmCdmaConnection[] mConnections;
    private boolean mDesiredMute = false;
    private ArrayList<GsmCdmaConnection> mDroppedDuringPoll = new ArrayList(19);
    private BroadcastReceiver mEcmExitReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("android.intent.action.EMERGENCY_CALLBACK_MODE_CHANGED")) {
                boolean isInEcm = intent.getBooleanExtra("phoneinECMState", false);
                GsmCdmaCallTracker gsmCdmaCallTracker = GsmCdmaCallTracker.this;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Received ACTION_EMERGENCY_CALLBACK_MODE_CHANGED isInEcm = ");
                stringBuilder.append(isInEcm);
                gsmCdmaCallTracker.log(stringBuilder.toString());
                if (!isInEcm) {
                    List<Connection> toNotify = new ArrayList();
                    toNotify.addAll(GsmCdmaCallTracker.this.mRingingCall.getConnections());
                    toNotify.addAll(GsmCdmaCallTracker.this.mForegroundCall.getConnections());
                    toNotify.addAll(GsmCdmaCallTracker.this.mBackgroundCall.getConnections());
                    if (GsmCdmaCallTracker.this.mPendingMO != null) {
                        toNotify.add(GsmCdmaCallTracker.this.mPendingMO);
                    }
                    for (Connection connection : toNotify) {
                        if (connection != null) {
                            connection.onExitedEcmMode();
                        }
                    }
                }
            }
        }
    };
    public GsmCdmaCall mForegroundCall = new GsmCdmaCall(this);
    private boolean mHangupPendingMO;
    private HwCustGsmCdmaCallTracker mHwGCT;
    private boolean mIsEcmTimerCanceled;
    private boolean mIsInCsRedial = false;
    private boolean mIsInEmergencyCall;
    private TelephonyMetrics mMetrics = TelephonyMetrics.getInstance();
    private int mPendingCallClirMode;
    private boolean mPendingCallInEcm;
    private GsmCdmaConnection mPendingMO;
    public GsmCdmaPhone mPhone;
    public GsmCdmaCall mRingingCall = new GsmCdmaCall(this);
    public State mState = State.IDLE;
    public RegistrantList mVoiceCallEndedRegistrants = new RegistrantList();
    private RegistrantList mVoiceCallStartedRegistrants = new RegistrantList();

    public GsmCdmaCallTracker(GsmCdmaPhone phone) {
        super(phone);
        this.mPhone = phone;
        this.mCi = phone.mCi;
        this.mCi.registerForCallStateChanged(this, 2, null);
        this.mCi.registerForOn(this, 9, null);
        this.mCi.registerForNotAvailable(this, 10, null);
        this.mCi.registerForRSrvccStateChanged(this, 50, null);
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.EMERGENCY_CALLBACK_MODE_CHANGED");
        this.mPhone.getContext().registerReceiver(this.mEcmExitReceiver, filter);
        updatePhoneType(true);
        this.mHwGCT = (HwCustGsmCdmaCallTracker) HwCustUtils.createObj(HwCustGsmCdmaCallTracker.class, new Object[]{this.mPhone});
    }

    public void updatePhoneType() {
        updatePhoneType(false);
    }

    private void updatePhoneType(boolean duringInit) {
        if (!duringInit) {
            reset();
            pollCallsWhenSafe();
        }
        if (this.mPhone.isPhoneTypeGsm()) {
            this.mConnections = new GsmCdmaConnection[19];
            this.mCi.unregisterForCallWaitingInfo(this);
            if (this.mIsInEmergencyCall) {
                this.mPhone.mDcTracker.setInternalDataEnabled(true);
                return;
            }
            return;
        }
        this.mConnections = new GsmCdmaConnection[8];
        this.mPendingCallInEcm = false;
        this.mIsInEmergencyCall = false;
        this.mPendingCallClirMode = 0;
        this.mIsEcmTimerCanceled = false;
        this.m3WayCallFlashDelay = 0;
        this.mCi.registerForCallWaitingInfo(this, 15, null);
    }

    private void reset() {
        Rlog.d(LOG_TAG, "reset");
        for (GsmCdmaConnection gsmCdmaConnection : this.mConnections) {
            if (gsmCdmaConnection != null) {
                gsmCdmaConnection.onDisconnect(36);
                gsmCdmaConnection.dispose();
            }
        }
        if (this.mPendingMO != null) {
            this.mPendingMO.dispose();
        }
        this.mConnections = null;
        this.mPendingMO = null;
        clearDisconnected();
    }

    protected void finalize() {
        Rlog.d(LOG_TAG, "GsmCdmaCallTracker finalized");
    }

    public void registerForVoiceCallStarted(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mVoiceCallStartedRegistrants.add(r);
        if (this.mState != State.IDLE) {
            r.notifyRegistrant(new AsyncResult(null, null, null));
        }
    }

    public void unregisterForVoiceCallStarted(Handler h) {
        this.mVoiceCallStartedRegistrants.remove(h);
    }

    public void registerForVoiceCallEnded(Handler h, int what, Object obj) {
        this.mVoiceCallEndedRegistrants.add(new Registrant(h, what, obj));
    }

    public void unregisterForVoiceCallEnded(Handler h) {
        this.mVoiceCallEndedRegistrants.remove(h);
    }

    public void registerForCallWaiting(Handler h, int what, Object obj) {
        this.mCallWaitingRegistrants.add(new Registrant(h, what, obj));
    }

    public void unregisterForCallWaiting(Handler h) {
        this.mCallWaitingRegistrants.remove(h);
    }

    public void registerForCdmaWaitingNumberChanged(Handler h, int what, Object obj) {
        Rlog.i(LOG_TAG, "registerForCdmaWaitingNumberChanged!");
        this.cdmaWaitingNumberChangedRegistrants.add(new Registrant(h, what, obj));
    }

    public void unregisterForCdmaWaitingNumberChanged(Handler h) {
        Rlog.i(LOG_TAG, "unregisterForCdmaWaitingNumberChanged!");
        this.cdmaWaitingNumberChangedRegistrants.remove(h);
    }

    private void fakeHoldForegroundBeforeDial() {
        List<Connection> connCopy = (List) this.mForegroundCall.mConnections.clone();
        int s = connCopy.size();
        for (int i = 0; i < s; i++) {
            ((GsmCdmaConnection) connCopy.get(i)).fakeHoldBeforeDial();
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:26:0x00b1  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized Connection dial(String dialString, int clirMode, UUSInfo uusInfo, Bundle intentExtras) throws CallStateException {
        clearDisconnected();
        HwTelephonyFactory.getHwChrServiceManager().reportCallException("Telephony", this.mPhone.getSubId(), 0, "AP_FLOW_SUC");
        if (canDial()) {
            String origNumber = dialString;
            dialString = convertNumberIfNecessary(this.mPhone, dialString);
            if (this.mForegroundCall.getState() == Call.State.ACTIVE) {
                switchWaitingOrHoldingAndActive();
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                }
                fakeHoldForegroundBeforeDial();
            }
            if (this.mForegroundCall.getState() == Call.State.IDLE) {
                this.mPendingMO = new GsmCdmaConnection(this.mPhone, checkForTestEmergencyNumber(dialString), this, this.mForegroundCall, PhoneNumberUtils.isLocalEmergencyNumber(this.mPhone.getContext(), dialString));
                this.mHangupPendingMO = false;
                this.mMetrics.writeRilDial(this.mPhone.getPhoneId(), this.mPendingMO, clirMode, uusInfo);
                if (!(this.mPendingMO.getAddress() == null || this.mPendingMO.getAddress().length() == 0)) {
                    if (this.mPendingMO.getAddress().indexOf(78) < 0) {
                        setMute(false);
                        this.mCi.dial(this.mPendingMO.getAddress(), clirMode, uusInfo, obtainCompleteMessage());
                        if (this.mNumberConverted) {
                            this.mPendingMO.setConverted(origNumber);
                            this.mNumberConverted = false;
                        }
                        updatePhoneState();
                        this.mPhone.notifyPreciseCallStateChanged();
                    }
                }
                this.mPendingMO.mCause = 7;
                pollCallsWhenSafe();
                if (this.mNumberConverted) {
                }
                updatePhoneState();
                this.mPhone.notifyPreciseCallStateChanged();
            } else {
                throw new CallStateException("cannot dial in current state");
            }
        }
        throw new CallStateException("cannot dial in current state");
        return this.mPendingMO;
    }

    private void handleEcmTimer(int action) {
        this.mPhone.handleTimerInEmergencyCallbackMode(action);
        switch (action) {
            case 0:
                this.mIsEcmTimerCanceled = false;
                return;
            case 1:
                this.mIsEcmTimerCanceled = true;
                return;
            default:
                String str = LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("handleEcmTimer, unsupported action ");
                stringBuilder.append(action);
                Rlog.e(str, stringBuilder.toString());
                return;
        }
    }

    private void disableDataCallInEmergencyCall(String dialString) {
        if (PhoneNumberUtils.isLocalEmergencyNumber(this.mPhone.getContext(), dialString)) {
            log("disableDataCallInEmergencyCall");
            setIsInEmergencyCall();
        }
    }

    public void setIsInEmergencyCall() {
        this.mIsInEmergencyCall = true;
        this.mPhone.mDcTracker.setInternalDataEnabled(false);
        this.mPhone.notifyEmergencyCallRegistrants(true);
        this.mPhone.sendEmergencyCallStateChange(true);
    }

    private Connection dial(String dialString, int clirMode) throws CallStateException {
        int i = clirMode;
        clearDisconnected();
        HwTelephonyFactory.getHwChrServiceManager().reportCallException("Telephony", this.mPhone.getSubId(), 0, "AP_FLOW_SUC");
        if (canDial()) {
            String dialString2;
            TelephonyManager tm = (TelephonyManager) this.mPhone.getContext().getSystemService("phone");
            String origNumber = dialString;
            String operatorIsoContry = tm.getNetworkCountryIsoForPhone(this.mPhone.getPhoneId());
            String simIsoContry = tm.getSimCountryIsoForPhone(this.mPhone.getPhoneId());
            boolean internationalRoaming = (TextUtils.isEmpty(operatorIsoContry) || TextUtils.isEmpty(simIsoContry) || simIsoContry.equals(operatorIsoContry)) ? false : true;
            if (internationalRoaming) {
                boolean z;
                if ("us".equals(simIsoContry)) {
                    z = internationalRoaming && !"vi".equals(operatorIsoContry);
                    internationalRoaming = z;
                } else if ("vi".equals(simIsoContry)) {
                    z = internationalRoaming && !"us".equals(operatorIsoContry);
                    internationalRoaming = z;
                }
            }
            if (internationalRoaming) {
                dialString2 = convertNumberIfNecessary(this.mPhone, dialString);
            } else {
                dialString2 = dialString;
            }
            boolean isPhoneInEcmMode = this.mPhone.isInEcm();
            boolean isEmergencyCall = PhoneNumberUtils.isLocalEmergencyNumber(this.mPhone.getContext(), dialString2);
            if (isPhoneInEcmMode && isEmergencyCall) {
                handleEcmTimer(1);
            }
            if (this.mForegroundCall.getState() == Call.State.ACTIVE) {
                return dialThreeWay(dialString2);
            }
            GsmCdmaConnection gsmCdmaConnection = r0;
            GsmCdmaConnection gsmCdmaConnection2 = new GsmCdmaConnection(this.mPhone, checkForTestEmergencyNumber(dialString2), this, this.mForegroundCall, isEmergencyCall);
            this.mPendingMO = gsmCdmaConnection;
            this.mHangupPendingMO = false;
            if (this.mPendingMO.getAddress() == null || this.mPendingMO.getAddress().length() == 0 || this.mPendingMO.getAddress().indexOf(78) >= 0) {
                this.mPendingMO.mCause = 7;
                pollCallsWhenSafe();
            } else {
                setMute(false);
                disableDataCallInEmergencyCall(dialString2);
                if (!isPhoneInEcmMode || (isPhoneInEcmMode && isEmergencyCall)) {
                    this.mCi.dial(this.mPendingMO.getDialAddress(), i, obtainCompleteMessage());
                } else {
                    this.mPhone.exitEmergencyCallbackMode();
                    this.mPhone.setOnEcbModeExitResponse(this, 14, null);
                    this.mPendingCallClirMode = i;
                    this.mPendingCallInEcm = true;
                }
            }
            if (this.mNumberConverted) {
                this.mPendingMO.setConverted(origNumber);
                this.mNumberConverted = false;
            }
            updatePhoneState();
            this.mPhone.notifyPreciseCallStateChanged();
            return this.mPendingMO;
        }
        String str = dialString;
        throw new CallStateException("cannot dial in current state");
    }

    private Connection dialThreeWay(String dialString) {
        if (this.mForegroundCall.isIdle()) {
            return null;
        }
        disableDataCallInEmergencyCall(dialString);
        this.mPendingMO = new GsmCdmaConnection(this.mPhone, checkForTestEmergencyNumber(dialString), this, this.mForegroundCall, this.mIsInEmergencyCall);
        PersistableBundle bundle = ((CarrierConfigManager) this.mPhone.getContext().getSystemService("carrier_config")).getConfig();
        if (bundle != null) {
            this.m3WayCallFlashDelay = bundle.getInt("cdma_3waycall_flash_delay_int");
        } else {
            this.m3WayCallFlashDelay = 0;
        }
        if (this.m3WayCallFlashDelay > 0) {
            this.mCi.sendCDMAFeatureCode("", obtainMessage(20));
        } else {
            this.mCi.sendCDMAFeatureCode(this.mPendingMO.getAddress(), obtainMessage(16));
        }
        return this.mPendingMO;
    }

    public Connection dial(String dialString) throws CallStateException {
        if (isPhoneTypeGsm()) {
            return dial(dialString, 0, null);
        }
        return dial(dialString, 0);
    }

    public Connection dial(String dialString, UUSInfo uusInfo, Bundle intentExtras) throws CallStateException {
        return dial(dialString, 0, uusInfo, intentExtras);
    }

    private Connection dial(String dialString, int clirMode, Bundle intentExtras) throws CallStateException {
        return dial(dialString, clirMode, null, intentExtras);
    }

    public void acceptCall() throws CallStateException {
        HwTelephonyFactory.getHwChrServiceManager().reportCallException("Telephony", this.mPhone.getSubId(), 2, "AP_FLOW_SUC");
        if (this.mRingingCall.getState() == Call.State.INCOMING) {
            Rlog.i("phone", "acceptCall: incoming...");
            setMute(false);
            this.mCi.acceptCall(obtainCompleteMessage());
        } else if (this.mRingingCall.getState() == Call.State.WAITING) {
            if (isPhoneTypeGsm()) {
                setMute(false);
            } else {
                GsmCdmaConnection cwConn = (GsmCdmaConnection) this.mRingingCall.getLatestConnection();
                if (cwConn != null) {
                    this.mRingingCall.setLastRingNumberAndChangeTime(cwConn.getAddress());
                    cwConn.updateParent(this.mRingingCall, this.mForegroundCall);
                    cwConn.onConnectedInOrOut();
                }
                updatePhoneState();
            }
            switchWaitingOrHoldingAndActive();
        } else {
            throw new CallStateException("phone not ringing");
        }
    }

    public void rejectCall() throws CallStateException {
        if (this.mRingingCall.getState().isRinging()) {
            this.mCi.rejectCall(obtainCompleteMessage());
            return;
        }
        throw new CallStateException("phone not ringing");
    }

    private void flashAndSetGenericTrue() {
        this.mCi.sendCDMAFeatureCode("", obtainMessage(8));
        this.mPhone.notifyPreciseCallStateChanged();
    }

    public void switchWaitingOrHoldingAndActive() throws CallStateException {
        if (this.mRingingCall.getState() == Call.State.INCOMING) {
            throw new CallStateException("cannot be in the incoming state");
        } else if (isPhoneTypeGsm()) {
            if (!this.callSwitchPending) {
                this.mCi.switchWaitingOrHoldingAndActive(obtainCompleteMessage(8));
                this.callSwitchPending = true;
            }
        } else if (this.mForegroundCall.getConnections().size() > 1) {
            flashAndSetGenericTrue();
        } else {
            this.mCi.sendCDMAFeatureCode("", obtainMessage(8));
        }
    }

    public void conference() {
        if (isPhoneTypeGsm()) {
            this.mCi.conference(obtainCompleteMessage(11));
        } else {
            flashAndSetGenericTrue();
        }
    }

    public void explicitCallTransfer() {
        this.mCi.explicitCallTransfer(obtainCompleteMessage(13));
    }

    public void clearDisconnected() {
        internalClearDisconnected();
        updatePhoneState();
        this.mPhone.notifyPreciseCallStateChanged();
    }

    public boolean canConference() {
        return this.mForegroundCall.getState() == Call.State.ACTIVE && this.mBackgroundCall.getState() == Call.State.HOLDING && !this.mBackgroundCall.isFull() && !this.mForegroundCall.isFull();
    }

    private boolean canDial() {
        String disableCall = SystemProperties.get("ro.telephony.disable-call", "false");
        Call.State fgCallState = this.mForegroundCall.getState();
        Call.State bgCallState = this.mBackgroundCall.getState();
        RadioState radioState = this.mCi.getRadioState();
        boolean z = false;
        boolean ret = radioState == RadioState.RADIO_ON && this.mPendingMO == null && !this.mRingingCall.isRinging() && !disableCall.equals("true") && ((isPhoneTypeGsm() && ((fgCallState == Call.State.IDLE || fgCallState == Call.State.DISCONNECTED || fgCallState == Call.State.ACTIVE) && (bgCallState == Call.State.IDLE || bgCallState == Call.State.DISCONNECTED || bgCallState == Call.State.HOLDING))) || !(isPhoneTypeGsm() || (this.mForegroundCall.getState() != Call.State.ACTIVE && this.mForegroundCall.getState().isAlive() && this.mBackgroundCall.getState().isAlive())));
        if (!ret) {
            String str = "canDial is false\n((radioState=%s) == CommandsInterface.RadioState.RADIO_ON)::=%s\n&& pendingMO == null::=%s\n&& !ringingCall.isRinging()::=%s\n&& !disableCall.equals(\"true\")::=%s\n&& (!foregroundCall.getState().isAlive()::=%s\n   || foregroundCall.getState() == GsmCdmaCall.State.ACTIVE::=%s\n   ||!backgroundCall.getState().isAlive())::=%s)";
            Object[] objArr = new Object[8];
            objArr[0] = radioState;
            objArr[1] = Boolean.valueOf(radioState == RadioState.RADIO_ON);
            objArr[2] = Boolean.valueOf(this.mPendingMO == null);
            objArr[3] = Boolean.valueOf(this.mRingingCall.isRinging() ^ 1);
            objArr[4] = Boolean.valueOf(disableCall.equals("true") ^ 1);
            objArr[5] = Boolean.valueOf(this.mForegroundCall.getState().isAlive() ^ 1);
            if (this.mForegroundCall.getState() == Call.State.ACTIVE) {
                z = true;
            }
            objArr[6] = Boolean.valueOf(z);
            objArr[7] = Boolean.valueOf(1 ^ this.mBackgroundCall.getState().isAlive());
            log(String.format(str, objArr));
        }
        return ret;
    }

    public boolean canTransfer() {
        boolean z = false;
        if (isPhoneTypeGsm()) {
            if ((this.mForegroundCall.getState() == Call.State.ACTIVE || this.mForegroundCall.getState() == Call.State.ALERTING || this.mForegroundCall.getState() == Call.State.DIALING) && this.mBackgroundCall.getState() == Call.State.HOLDING) {
                z = true;
            }
            return z;
        }
        Rlog.e(LOG_TAG, "canTransfer: not possible in CDMA");
        return false;
    }

    private void internalClearDisconnected() {
        this.mRingingCall.clearDisconnected();
        this.mForegroundCall.clearDisconnected();
        this.mBackgroundCall.clearDisconnected();
    }

    private Message obtainCompleteMessage() {
        return obtainCompleteMessage(4);
    }

    private Message obtainCompleteMessage(int what) {
        this.mPendingOperations++;
        this.mLastRelevantPoll = null;
        this.mNeedsPoll = true;
        return obtainMessage(what);
    }

    private void operationComplete() {
        this.mPendingOperations--;
        if (this.mPendingOperations == 0 && this.mNeedsPoll) {
            this.mLastRelevantPoll = obtainMessage(1);
            if (mIsShowRedirectNumber) {
                this.mCi.getCurrentCallsEx(this.mLastRelevantPoll);
            } else {
                this.mCi.getCurrentCalls(this.mLastRelevantPoll);
            }
        } else if (this.mPendingOperations < 0) {
            Rlog.e(LOG_TAG, "GsmCdmaCallTracker.pendingOperations < 0");
            this.mPendingOperations = 0;
        }
    }

    private void updatePhoneState() {
        State oldState = this.mState;
        if (this.mRingingCall.isRinging()) {
            this.mState = State.RINGING;
        } else if (this.mPendingMO == null && this.mForegroundCall.isIdle() && this.mBackgroundCall.isIdle()) {
            Phone imsPhone = this.mPhone.getImsPhone();
            if (!(imsPhone == null || this.mIsInCsRedial)) {
                log("[SRVCC] updatePhoneState -> clean ims HandoverCall.");
                imsPhone.callEndCleanupHandOverCallIfAny();
            }
            this.mState = State.IDLE;
        } else {
            this.mState = State.OFFHOOK;
        }
        if (this.mState != State.IDLE || oldState == this.mState) {
            if (oldState == State.IDLE && oldState != this.mState) {
                this.mVoiceCallStartedRegistrants.notifyRegistrants(new AsyncResult(null, null, null));
            }
        } else if (notifyRegistrantsDelayed()) {
            this.mVoiceCallEndedRegistrants.notifyRegistrants(new AsyncResult(null, null, null));
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("update phone state, old=");
        stringBuilder.append(oldState);
        stringBuilder.append(" new=");
        stringBuilder.append(this.mState);
        log(stringBuilder.toString());
        if (this.mState != oldState || (this.mIsSrvccHappened && State.IDLE == this.mState)) {
            this.mPhone.notifyPhoneStateChanged();
            this.mMetrics.writePhoneState(this.mPhone.getPhoneId(), this.mState);
            if (this.mIsSrvccHappened) {
                this.mIsSrvccHappened = false;
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:126:0x033f A:{Catch:{ CallStateException -> 0x0123 }} */
    /* JADX WARNING: Missing block: B:213:0x0549, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected synchronized void handlePollCalls(AsyncResult ar) {
        AsyncResult asyncResult = ar;
        synchronized (this) {
            if (HwDeviceManager.disallowOp(1)) {
                Rlog.i(LOG_TAG, "HwDeviceManager disallow open call.");
                return;
            }
            List polledCalls;
            Connection polledCalls2;
            Connection newUnknownConnectionCdma;
            boolean hasAnyCallDisconnected;
            StringBuilder stringBuilder;
            String str;
            boolean hasAnyCallDisconnected2;
            if (asyncResult.exception == null) {
                polledCalls = asyncResult.result;
            } else if (VSimUtilsInner.isVSimPhone(this.mPhone)) {
                polledCalls = new ArrayList();
                log("vsim phone handlePollCalls");
            } else if (isCommandExceptionRadioNotAvailable(asyncResult.exception)) {
                polledCalls = new ArrayList();
                log("no DriverCall");
            } else {
                pollCallsAfterDelay();
                log("pollCallsAfterDelay");
                return;
            }
            Connection newRinging = null;
            ArrayList<Connection> newUnknownConnectionsGsm = new ArrayList();
            Connection newUnknownConnectionCdma2 = null;
            boolean hasNonHangupStateChanged = false;
            boolean hasAnyCallDisconnected3 = false;
            boolean unknownConnectionAppeared = false;
            int handoverConnectionsSize = this.mHandoverConnections.size();
            boolean noConnectionExists = true;
            syncHoConnection();
            int i = 0;
            int curDC = 0;
            int dcSize = polledCalls.size();
            while (i < this.mConnections.length) {
                DriverCall dc;
                List polledCalls3;
                StringBuilder stringBuilder2;
                boolean changed;
                boolean z;
                Connection conn = this.mConnections[i];
                DriverCall driverCall = null;
                if (curDC < dcSize) {
                    driverCall = (DriverCall) polledCalls.get(curDC);
                    DriverCall dc2 = driverCall;
                    if (driverCall.index == i + 1) {
                        curDC++;
                        dc = dc2;
                        polledCalls3 = polledCalls;
                        polledCalls2 = conn;
                        if (!(polledCalls2 == null && dc == null)) {
                            noConnectionExists = false;
                        }
                        stringBuilder2 = new StringBuilder();
                        newUnknownConnectionCdma = newUnknownConnectionCdma2;
                        stringBuilder2.append("poll: conn[i=");
                        stringBuilder2.append(i);
                        stringBuilder2.append("]=");
                        stringBuilder2.append(polledCalls2);
                        stringBuilder2.append(", dc=");
                        stringBuilder2.append(dc);
                        log(stringBuilder2.toString());
                        Connection newRinging2;
                        StringBuilder stringBuilder3;
                        if (polledCalls2 == null || dc == null) {
                            hasAnyCallDisconnected = hasAnyCallDisconnected3;
                            if (polledCalls2 == null && dc == null) {
                                if (isPhoneTypeGsm()) {
                                    this.mDroppedDuringPoll.add(polledCalls2);
                                } else {
                                    if (dcSize != 0) {
                                        log("conn != null, dc == null. Still have connections in the call list");
                                        this.mDroppedDuringPoll.add(polledCalls2);
                                    } else {
                                        int count = this.mForegroundCall.mConnections.size();
                                        int n = 0;
                                        while (n < count) {
                                            stringBuilder = new StringBuilder();
                                            int count2 = count;
                                            stringBuilder.append("adding fgCall cn ");
                                            stringBuilder.append(n);
                                            stringBuilder.append(" to droppedDuringPoll");
                                            log(stringBuilder.toString());
                                            this.mDroppedDuringPoll.add((GsmCdmaConnection) this.mForegroundCall.mConnections.get(n));
                                            n++;
                                            count = count2;
                                        }
                                        count = this.mRingingCall.mConnections.size();
                                        n = 0;
                                        while (n < count) {
                                            stringBuilder = new StringBuilder();
                                            int count3 = count;
                                            stringBuilder.append("adding rgCall cn ");
                                            stringBuilder.append(n);
                                            stringBuilder.append(" to droppedDuringPoll");
                                            log(stringBuilder.toString());
                                            this.mDroppedDuringPoll.add((GsmCdmaConnection) this.mRingingCall.mConnections.get(n));
                                            n++;
                                            count = count3;
                                        }
                                    }
                                    if (this.mIsEcmTimerCanceled) {
                                        handleEcmTimer(0);
                                    }
                                    checkAndEnableDataCallAfterEmergencyCallDropped();
                                }
                                this.mConnections[i] = null;
                                newUnknownConnectionCdma2 = newUnknownConnectionCdma;
                                i++;
                                polledCalls = polledCalls3;
                                hasAnyCallDisconnected3 = hasAnyCallDisconnected;
                                asyncResult = ar;
                            } else if (polledCalls2 != null || dc == null || polledCalls2.compareTo(dc) || !isPhoneTypeGsm()) {
                                if (!(polledCalls2 == null || dc == null)) {
                                    if (!isPhoneTypeGsm() || polledCalls2.isIncoming() == dc.isMT) {
                                        changed = polledCalls2.update(dc);
                                        if (!hasNonHangupStateChanged) {
                                            if (!changed) {
                                                z = false;
                                                changed = z;
                                                hasNonHangupStateChanged = changed;
                                            }
                                        }
                                        z = true;
                                        changed = z;
                                        hasNonHangupStateChanged = changed;
                                    } else if (dc.isMT) {
                                        this.mDroppedDuringPoll.add(polledCalls2);
                                        this.mConnections[i] = new GsmCdmaConnection(this.mPhone, dc, this, i);
                                        newRinging2 = checkMtFindNewRinging(dc, i);
                                        if (newRinging2 == null) {
                                            unknownConnectionAppeared = true;
                                            newUnknownConnectionCdma2 = polledCalls2;
                                        } else {
                                            newUnknownConnectionCdma2 = newUnknownConnectionCdma;
                                        }
                                        checkAndEnableDataCallAfterEmergencyCallDropped();
                                        newRinging = newRinging2;
                                        i++;
                                        polledCalls = polledCalls3;
                                        hasAnyCallDisconnected3 = hasAnyCallDisconnected;
                                        asyncResult = ar;
                                    } else {
                                        str = LOG_TAG;
                                        stringBuilder3 = new StringBuilder();
                                        stringBuilder3.append("Error in RIL, Phantom call appeared ");
                                        stringBuilder3.append(dc);
                                        Rlog.e(str, stringBuilder3.toString());
                                    }
                                }
                                newUnknownConnectionCdma2 = newUnknownConnectionCdma;
                                i++;
                                polledCalls = polledCalls3;
                                hasAnyCallDisconnected3 = hasAnyCallDisconnected;
                                asyncResult = ar;
                            } else {
                                this.mDroppedDuringPoll.add(polledCalls2);
                                this.mConnections[i] = new GsmCdmaConnection(this.mPhone, dc, this, i);
                                if (this.mConnections[i].getCall() == this.mRingingCall) {
                                    newRinging = this.mConnections[i];
                                }
                                changed = true;
                            }
                        } else {
                            if (this.mPendingMO != null && this.mPendingMO.compareTo(dc) && getHoConnection(dc) == null) {
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("poll: pendingMO=");
                                stringBuilder2.append(this.mPendingMO);
                                log(stringBuilder2.toString());
                                this.mConnections[i] = this.mPendingMO;
                                this.mPendingMO.mIndex = i;
                                this.mPendingMO.update(dc);
                                this.mPendingMO = null;
                                if (this.mHangupPendingMO) {
                                    this.mHangupPendingMO = false;
                                    if (!isPhoneTypeGsm() && this.mIsEcmTimerCanceled) {
                                        handleEcmTimer(0);
                                    }
                                    try {
                                        stringBuilder2 = new StringBuilder();
                                        stringBuilder2.append("poll: hangupPendingMO, hangup conn ");
                                        stringBuilder2.append(i);
                                        log(stringBuilder2.toString());
                                        hangup(this.mConnections[i]);
                                    } catch (CallStateException ex) {
                                        Rlog.e(LOG_TAG, "unexpected error on hangup");
                                    }
                                } else {
                                    hasAnyCallDisconnected = hasAnyCallDisconnected3;
                                }
                            } else {
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("pendingMo=");
                                stringBuilder2.append(this.mPendingMO);
                                stringBuilder2.append(", dc=");
                                stringBuilder2.append(dc);
                                log(stringBuilder2.toString());
                                hasAnyCallDisconnected = hasAnyCallDisconnected3;
                                this.mConnections[i] = new GsmCdmaConnection(this.mPhone, dc, this, i);
                                newRinging2 = getHoConnection(dc);
                                stringBuilder3 = new StringBuilder();
                                stringBuilder3.append("[SRVCC] handlepollcall --> hoConnection:");
                                stringBuilder3.append(newRinging2);
                                log(stringBuilder3.toString());
                                if (newRinging2 != null) {
                                    this.mConnections[i].setPostDialString(newRinging2.getOrigDialString());
                                    this.mConnections[i].migrateFrom(newRinging2);
                                    if (!(newRinging2.mPreHandoverState == Call.State.ACTIVE || newRinging2.mPreHandoverState == Call.State.HOLDING || dc.state != DriverCall.State.ACTIVE)) {
                                        Rlog.d(LOG_TAG, "[SRVCC] mConnections onConnectedInOrOut");
                                        this.mConnections[i].onConnectedInOrOut();
                                    }
                                    if (dc.state == DriverCall.State.ACTIVE || dc.state == DriverCall.State.HOLDING) {
                                        this.mConnections[i].releaseWakeLock();
                                    }
                                    this.mHandoverConnections.remove(newRinging2);
                                    this.mRemovedHandoverConnections.add(newRinging2);
                                    String str2 = LOG_TAG;
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("[SRVCC] notifyHandoverStateChanged mConnections[i]=");
                                    stringBuilder.append(this.mConnections[i]);
                                    Rlog.d(str2, stringBuilder.toString());
                                    this.mPhone.notifyHandoverStateChanged(this.mConnections[i]);
                                } else {
                                    newRinging = checkMtFindNewRinging(dc, i);
                                    if (newRinging == null) {
                                        unknownConnectionAppeared = true;
                                        if (isPhoneTypeGsm()) {
                                            newUnknownConnectionsGsm.add(this.mConnections[i]);
                                        } else {
                                            newUnknownConnectionCdma = this.mConnections[i];
                                        }
                                    }
                                }
                            }
                            changed = true;
                        }
                        hasNonHangupStateChanged = changed;
                        newUnknownConnectionCdma2 = newUnknownConnectionCdma;
                        i++;
                        polledCalls = polledCalls3;
                        hasAnyCallDisconnected3 = hasAnyCallDisconnected;
                        asyncResult = ar;
                    } else {
                        driverCall = null;
                    }
                }
                dc = driverCall;
                polledCalls3 = polledCalls;
                polledCalls2 = conn;
                noConnectionExists = false;
                stringBuilder2 = new StringBuilder();
                newUnknownConnectionCdma = newUnknownConnectionCdma2;
                stringBuilder2.append("poll: conn[i=");
                stringBuilder2.append(i);
                stringBuilder2.append("]=");
                stringBuilder2.append(polledCalls2);
                stringBuilder2.append(", dc=");
                stringBuilder2.append(dc);
                log(stringBuilder2.toString());
                if (polledCalls2 == null) {
                }
                hasAnyCallDisconnected = hasAnyCallDisconnected3;
                if (polledCalls2 == null) {
                }
                if (polledCalls2 != null) {
                }
                if (isPhoneTypeGsm()) {
                }
                changed = polledCalls2.update(dc);
                if (hasNonHangupStateChanged) {
                }
                z = true;
                changed = z;
                hasNonHangupStateChanged = changed;
                newUnknownConnectionCdma2 = newUnknownConnectionCdma;
                i++;
                polledCalls = polledCalls3;
                hasAnyCallDisconnected3 = hasAnyCallDisconnected;
                asyncResult = ar;
            }
            newUnknownConnectionCdma = newUnknownConnectionCdma2;
            hasAnyCallDisconnected = hasAnyCallDisconnected3;
            if (!isPhoneTypeGsm() && noConnectionExists) {
                checkAndEnableDataCallAfterEmergencyCallDropped();
            }
            if (this.mPendingMO != null) {
                str = LOG_TAG;
                StringBuilder stringBuilder4 = new StringBuilder();
                stringBuilder4.append("Pending MO dropped before poll fg state:");
                stringBuilder4.append(this.mForegroundCall.getState());
                Rlog.d(str, stringBuilder4.toString());
                this.mDroppedDuringPoll.add(this.mPendingMO);
                this.mPendingMO = null;
                this.mHangupPendingMO = false;
                if (!isPhoneTypeGsm()) {
                    if (this.mPendingCallInEcm) {
                        this.mPendingCallInEcm = false;
                    }
                    checkAndEnableDataCallAfterEmergencyCallDropped();
                }
            }
            if (newRinging != null) {
                HwTelephonyFactory.getHwChrServiceManager().reportCallException("Telephony", this.mPhone.getSubId(), 1, LOG_TAG);
                this.mPhone.notifyNewRingingConnection(newRinging);
            }
            ArrayList<GsmCdmaConnection> locallyDisconnectedConnections = new ArrayList();
            polledCalls2 = newUnknownConnectionCdma;
            for (int i2 = this.mDroppedDuringPoll.size() - 1; i2 >= 0; i2--) {
                newUnknownConnectionCdma2 = (GsmCdmaConnection) this.mDroppedDuringPoll.get(i2);
                hasAnyCallDisconnected3 = false;
                if (newUnknownConnectionCdma2.isIncoming() && newUnknownConnectionCdma2.getConnectTime() == 0) {
                    if (newUnknownConnectionCdma2.mCause == 3) {
                        i = 16;
                    } else {
                        i = 1;
                    }
                    StringBuilder stringBuilder5 = new StringBuilder();
                    stringBuilder5.append("missed/rejected call, conn.cause=");
                    stringBuilder5.append(newUnknownConnectionCdma2.mCause);
                    log(stringBuilder5.toString());
                    stringBuilder5 = new StringBuilder();
                    stringBuilder5.append("setting cause to ");
                    stringBuilder5.append(i);
                    log(stringBuilder5.toString());
                    this.mDroppedDuringPoll.remove(i2);
                    boolean hasAnyCallDisconnected4 = hasAnyCallDisconnected | newUnknownConnectionCdma2.onDisconnect(i);
                    hasAnyCallDisconnected3 = true;
                    locallyDisconnectedConnections.add(newUnknownConnectionCdma2);
                    hasAnyCallDisconnected = hasAnyCallDisconnected4;
                } else if (newUnknownConnectionCdma2.mCause == 3 || newUnknownConnectionCdma2.mCause == 7) {
                    this.mDroppedDuringPoll.remove(i2);
                    hasAnyCallDisconnected2 = hasAnyCallDisconnected | newUnknownConnectionCdma2.onDisconnect(newUnknownConnectionCdma2.mCause);
                    hasAnyCallDisconnected3 = true;
                    locallyDisconnectedConnections.add(newUnknownConnectionCdma2);
                    hasAnyCallDisconnected = hasAnyCallDisconnected2;
                }
                if (!isPhoneTypeGsm() && wasDisconnected && unknownConnectionAppeared && newUnknownConnectionCdma2 == polledCalls2) {
                    unknownConnectionAppeared = false;
                    polledCalls2 = null;
                }
            }
            if (locallyDisconnectedConnections.size() > 0) {
                this.mMetrics.writeRilCallList(this.mPhone.getPhoneId(), locallyDisconnectedConnections);
            }
            Iterator<Connection> it = this.mHandoverConnections.iterator();
            while (it.hasNext()) {
                newUnknownConnectionCdma2 = (Connection) it.next();
                stringBuilder = new StringBuilder();
                stringBuilder.append("[SRVCC] handlePollCalls - disconnect hoConn= ");
                stringBuilder.append(newUnknownConnectionCdma2);
                stringBuilder.append(" hoConn.State= ");
                stringBuilder.append(newUnknownConnectionCdma2.getState());
                log(stringBuilder.toString());
                if (newUnknownConnectionCdma2.getState().isRinging()) {
                    hasAnyCallDisconnected2 = hasAnyCallDisconnected | newUnknownConnectionCdma2.onDisconnect(1);
                } else {
                    hasAnyCallDisconnected2 = hasAnyCallDisconnected | newUnknownConnectionCdma2.onDisconnect(-1);
                }
                hasAnyCallDisconnected = hasAnyCallDisconnected2;
                it.remove();
            }
            if (this.mDroppedDuringPoll.size() > 0) {
                this.mCi.getLastCallFailCause(obtainNoPollCompleteMessage(5));
            } else {
                this.mIsInCsRedial = false;
            }
            if (false) {
                pollCallsAfterDelay();
            }
            if (newRinging != null || hasNonHangupStateChanged || hasAnyCallDisconnected) {
                internalClearDisconnected();
            }
            updatePhoneState();
            if (unknownConnectionAppeared) {
                if (isPhoneTypeGsm()) {
                    Iterator it2 = newUnknownConnectionsGsm.iterator();
                    while (it2.hasNext()) {
                        newUnknownConnectionCdma2 = (Connection) it2.next();
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Notify unknown for ");
                        stringBuilder.append(newUnknownConnectionCdma2);
                        log(stringBuilder.toString());
                        this.mPhone.notifyUnknownConnection(newUnknownConnectionCdma2);
                    }
                } else {
                    this.mPhone.notifyUnknownConnection(polledCalls2);
                }
            }
            if (hasNonHangupStateChanged || newRinging != null || hasAnyCallDisconnected) {
                this.mPhone.notifyPreciseCallStateChanged();
                updateMetrics(this.mConnections);
            }
            if (handoverConnectionsSize > 0 && this.mHandoverConnections.size() == 0) {
                Phone imsPhone = this.mPhone.getImsPhone();
                if (imsPhone != null) {
                    log("[SRVCC] handlePollCalls - handover connection mapped, clean HandoverCall.");
                    imsPhone.callEndCleanupHandOverCallIfAny();
                }
            }
        }
    }

    private void updateMetrics(GsmCdmaConnection[] connections) {
        ArrayList<GsmCdmaConnection> activeConnections = new ArrayList();
        for (GsmCdmaConnection conn : connections) {
            if (conn != null) {
                activeConnections.add(conn);
            }
        }
        this.mMetrics.writeRilCallList(this.mPhone.getPhoneId(), activeConnections);
    }

    private void handleRadioNotAvailable() {
        pollCallsWhenSafe();
    }

    private void dumpState() {
        int i;
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Phone State:");
        stringBuilder.append(this.mState);
        Rlog.i(str, stringBuilder.toString());
        str = LOG_TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("Ringing call: ");
        stringBuilder.append(this.mRingingCall.toString());
        Rlog.i(str, stringBuilder.toString());
        List l = this.mRingingCall.getConnections();
        int s = l.size();
        for (i = 0; i < s; i++) {
            Rlog.i(LOG_TAG, l.get(i).toString());
        }
        String str2 = LOG_TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Foreground call: ");
        stringBuilder2.append(this.mForegroundCall.toString());
        Rlog.i(str2, stringBuilder2.toString());
        l = this.mForegroundCall.getConnections();
        s = l.size();
        for (i = 0; i < s; i++) {
            Rlog.i(LOG_TAG, l.get(i).toString());
        }
        str2 = LOG_TAG;
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Background call: ");
        stringBuilder2.append(this.mBackgroundCall.toString());
        Rlog.i(str2, stringBuilder2.toString());
        l = this.mBackgroundCall.getConnections();
        s = l.size();
        for (i = 0; i < s; i++) {
            Rlog.i(LOG_TAG, l.get(i).toString());
        }
    }

    public void hangup(GsmCdmaConnection conn) throws CallStateException {
        if (conn.mOwner == this) {
            if (conn == this.mPendingMO) {
                log("hangup: set hangupPendingMO to true");
                this.mHangupPendingMO = true;
            } else if (!isPhoneTypeGsm() && conn.getCall() == this.mRingingCall && this.mRingingCall.getState() == Call.State.WAITING) {
                this.mRingingCall.setLastRingNumberAndChangeTime(conn.getAddress());
                conn.onLocalDisconnect();
                updatePhoneState();
                this.mPhone.notifyPreciseCallStateChanged();
                return;
            } else {
                try {
                    this.mMetrics.writeRilHangup(this.mPhone.getPhoneId(), conn, conn.getGsmCdmaIndex());
                    this.mCi.hangupConnection(conn.getGsmCdmaIndex(), obtainCompleteMessage());
                } catch (CallStateException e) {
                    String str = LOG_TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("GsmCdmaCallTracker WARN: hangup() on absent connection ");
                    stringBuilder.append(conn);
                    Rlog.w(str, stringBuilder.toString());
                }
            }
            conn.onHangupLocal();
            GsmCdmaCall call = conn.getCall();
            if (call != null && call.getConnections().size() == 1) {
                call.setState(Call.State.DISCONNECTING);
            }
            Call.State state = conn.getState();
            if (!((this.mPhone.isPhoneTypeCdma() || this.mPhone.isPhoneTypeCdmaLte()) && this.mForegroundCall.isMultiparty())) {
                delaySendRilRecoveryMsg(state);
            }
            state = conn.getState();
            if (state == Call.State.IDLE || state == Call.State.DISCONNECTED) {
                cleanRilRecovery();
            }
            return;
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("GsmCdmaConnection ");
        stringBuilder2.append(conn);
        stringBuilder2.append("does not belong to GsmCdmaCallTracker ");
        stringBuilder2.append(this);
        throw new CallStateException(stringBuilder2.toString());
    }

    public void separate(GsmCdmaConnection conn) throws CallStateException {
        if (conn.mOwner == this) {
            try {
                this.mCi.separateConnection(conn.getGsmCdmaIndex(), obtainCompleteMessage(12));
                return;
            } catch (CallStateException e) {
                String str = LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("GsmCdmaCallTracker WARN: separate() on absent connection ");
                stringBuilder.append(conn);
                Rlog.w(str, stringBuilder.toString());
                return;
            }
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("GsmCdmaConnection ");
        stringBuilder2.append(conn);
        stringBuilder2.append("does not belong to GsmCdmaCallTracker ");
        stringBuilder2.append(this);
        throw new CallStateException(stringBuilder2.toString());
    }

    public void setMute(boolean mute) {
        if (((UserManager) this.mPhone.getContext().getSystemService("user")).getUserRestrictions().getBoolean("no_unmute_microphone", false)) {
            this.mDesiredMute = true;
        } else {
            this.mDesiredMute = mute;
        }
        this.mCi.setMute(this.mDesiredMute, null);
    }

    public boolean getMute() {
        return this.mDesiredMute;
    }

    public void hangup(GsmCdmaCall call) throws CallStateException {
        if (call.getConnections().size() != 0) {
            if (call == this.mRingingCall) {
                log("(ringing) hangup waiting or background");
                logHangupEvent(call);
                if (!IS_SUPPORT_RIL_RECOVERY) {
                    this.mCi.hangupWaitingOrBackground(obtainCompleteMessage());
                } else if (this.mHwGCT == null || this.mHwGCT.getRejectCallCause(call) == -1) {
                    hangupAllConnections(call);
                } else {
                    log("rejectCallForCause !!!");
                    this.mHwGCT.rejectCallForCause(this.mCi, call, obtainCompleteMessage());
                }
                delaySendRilRecoveryMsg(call.getState());
            } else if (call == this.mForegroundCall) {
                if (call.isDialingOrAlerting()) {
                    log("(foregnd) hangup dialing or alerting...");
                    hangup((GsmCdmaConnection) call.getConnections().get(0));
                } else if (isPhoneTypeGsm() && this.mRingingCall.isRinging()) {
                    log("hangup all conns in active/background call, without affecting ringing call");
                    hangupAllConnections(call);
                } else {
                    logHangupEvent(call);
                    hangupForegroundResumeBackground();
                    delaySendRilRecoveryMsg(call.getState());
                }
            } else if (call == this.mBackgroundCall) {
                if (this.mRingingCall.isRinging()) {
                    log("hangup all conns in background call");
                    hangupAllConnections(call);
                } else {
                    hangupAllConnections(call);
                }
                delaySendRilRecoveryMsg(call.getState());
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("GsmCdmaCall ");
                stringBuilder.append(call);
                stringBuilder.append("does not belong to GsmCdmaCallTracker ");
                stringBuilder.append(this);
                throw new RuntimeException(stringBuilder.toString());
            }
            Call.State state = call.getState();
            if (state == Call.State.IDLE || state == Call.State.DISCONNECTED) {
                cleanRilRecovery();
            }
            call.onHangupLocal();
            this.mPhone.notifyPreciseCallStateChanged();
            return;
        }
        throw new CallStateException("no connections in call");
    }

    private void logHangupEvent(GsmCdmaCall call) {
        int count = call.mConnections.size();
        for (int i = 0; i < count; i++) {
            int call_index;
            GsmCdmaConnection cn = (GsmCdmaConnection) call.mConnections.get(i);
            try {
                call_index = cn.getGsmCdmaIndex();
            } catch (CallStateException e) {
                call_index = -1;
            }
            this.mMetrics.writeRilHangup(this.mPhone.getPhoneId(), cn, call_index);
        }
    }

    public void hangupWaitingOrBackground() {
        log("hangupWaitingOrBackground");
        logHangupEvent(this.mBackgroundCall);
        this.mCi.hangupWaitingOrBackground(obtainCompleteMessage());
    }

    public void hangupForegroundResumeBackground() {
        log("hangupForegroundResumeBackground");
        this.mCi.hangupForegroundResumeBackground(obtainCompleteMessage());
    }

    public void hangupConnectionByIndex(GsmCdmaCall call, int index) throws CallStateException {
        int count = call.mConnections.size();
        for (int i = 0; i < count; i++) {
            GsmCdmaConnection cn = (GsmCdmaConnection) call.mConnections.get(i);
            try {
                if (!cn.mDisconnected && cn.getGsmCdmaIndex() == index) {
                    this.mMetrics.writeRilHangup(this.mPhone.getPhoneId(), cn, cn.getGsmCdmaIndex());
                    this.mCi.hangupConnection(index, obtainCompleteMessage());
                    return;
                }
            } catch (CallStateException ex) {
                String str = LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("hangupConnectionByIndex, caught ");
                stringBuilder.append(ex);
                Rlog.e(str, stringBuilder.toString());
            }
        }
        throw new CallStateException("no GsmCdma index found");
    }

    public void hangupAllConnections(GsmCdmaCall call) {
        int count = call.mConnections.size();
        for (int i = 0; i < count; i++) {
            GsmCdmaConnection cn = (GsmCdmaConnection) call.mConnections.get(i);
            try {
                if (!cn.mDisconnected) {
                    this.mMetrics.writeRilHangup(this.mPhone.getPhoneId(), cn, cn.getGsmCdmaIndex());
                    this.mCi.hangupConnection(cn.getGsmCdmaIndex(), obtainCompleteMessage());
                }
            } catch (CallStateException ex) {
                String str = LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("hangupConnectionByIndex caught ");
                stringBuilder.append(ex);
                Rlog.e(str, stringBuilder.toString());
            }
        }
    }

    public GsmCdmaConnection getConnectionByIndex(GsmCdmaCall call, int index) throws CallStateException {
        int count = call.mConnections.size();
        for (int i = 0; i < count; i++) {
            GsmCdmaConnection cn = (GsmCdmaConnection) call.mConnections.get(i);
            if (!cn.mDisconnected && cn.getGsmCdmaIndex() == index) {
                return cn;
            }
        }
        return null;
    }

    private void notifyCallWaitingInfo(CdmaCallWaitingNotification obj) {
        if (this.mCallWaitingRegistrants != null) {
            this.mCallWaitingRegistrants.notifyRegistrants(new AsyncResult(null, obj, null));
        }
    }

    private void notifyWaitingNumberChanged() {
        Rlog.i(LOG_TAG, "notifyWaitingNumberChanged");
        if (this.cdmaWaitingNumberChangedRegistrants != null) {
            this.cdmaWaitingNumberChangedRegistrants.notifyRegistrants(new AsyncResult(null, null, null));
        }
    }

    private void handleCallWaitingInfo(CdmaCallWaitingNotification cw) {
        GsmCdmaConnection cwConn;
        if (!isPhoneTypeGsm() && this.mRingingCall.getState() == Call.State.WAITING) {
            cwConn = (GsmCdmaConnection) this.mRingingCall.getLatestConnection();
            if (!(cwConn == null || cw.number == null || !cw.number.equals(cwConn.getAddress()))) {
                long passedTime = System.currentTimeMillis() - cwConn.getCreateTime();
                if (passedTime > 0 && passedTime < 30000) {
                    Rlog.d(LOG_TAG, "Ignoring callingwaiting events received for same number within 30s");
                    return;
                }
            }
        }
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("the ringingcall connection size = ");
        stringBuilder.append(this.mRingingCall.mConnections.size());
        Rlog.i(str, stringBuilder.toString());
        if (this.mRingingCall.mConnections.size() > 0) {
            if (((GsmCdmaConnection) this.mRingingCall.getLatestConnection()).isNewConnection(cw)) {
                Rlog.i(LOG_TAG, "Update the ringing connection if the call is from another number.");
                notifyWaitingNumberChanged();
                GsmCdmaConnection gsmCdmaConnection = new GsmCdmaConnection(this.mPhone.getContext(), cw, this, this.mRingingCall);
            }
        } else if (this.mRingingCall.isTooFrequency(cw.number)) {
            this.mRingingCall.setLastRingNumberAndChangeTime(cw.number);
            Rlog.i(LOG_TAG, "ringing too frequency, ignore this callwaiting message.");
            return;
        } else {
            Rlog.i(LOG_TAG, "new callwaiting message, create a connection");
            cwConn = new GsmCdmaConnection(this.mPhone.getContext(), cw, this, this.mRingingCall);
        }
        updatePhoneState();
        notifyCallWaitingInfo(cw);
    }

    private SuppService getFailedService(int what) {
        if (what == 8) {
            return SuppService.SWITCH;
        }
        switch (what) {
            case 11:
                return SuppService.CONFERENCE;
            case 12:
                return SuppService.SEPARATE;
            case 13:
                return SuppService.TRANSFER;
            default:
                return SuppService.UNKNOWN;
        }
    }

    public void handleMessage(Message msg) {
        int i = msg.what;
        StringBuilder stringBuilder;
        if (i != 20) {
            AsyncResult ar;
            if (i == 50) {
                Rlog.d(LOG_TAG, "Event EVENT_RSRVCC_STATE_CHANGED Received");
                ar = msg.obj;
                if (ar.exception == null) {
                    this.mIsInCsRedial = true;
                    return;
                }
                String str = LOG_TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("RSrvcc exception: ");
                stringBuilder2.append(ar.exception);
                Rlog.e(str, stringBuilder2.toString());
            } else if (i != 201) {
                switch (i) {
                    case 1:
                        Rlog.d(LOG_TAG, "Event EVENT_POLL_CALLS_RESULT Received");
                        if (msg == this.mLastRelevantPoll) {
                            log("handle EVENT_POLL_CALL_RESULT: set needsPoll=F");
                            this.mNeedsPoll = false;
                            this.mLastRelevantPoll = null;
                            handlePollCalls((AsyncResult) msg.obj);
                            return;
                        }
                        return;
                    case 2:
                    case 3:
                        pollCallsWhenSafe();
                        return;
                    case 4:
                        operationComplete();
                        return;
                    case 5:
                        int causeCode;
                        int causeCode2;
                        String vendorCause = null;
                        AsyncResult ar2 = msg.obj;
                        operationComplete();
                        if (ar2.exception != null) {
                            causeCode = 16;
                            Rlog.i(LOG_TAG, "Exception during getLastCallFailCause, assuming normal disconnect");
                        } else {
                            LastCallFailCause failCause = ar2.result;
                            causeCode2 = failCause.causeCode;
                            vendorCause = failCause.vendorCause;
                            causeCode = causeCode2;
                        }
                        if (causeCode == 34 || causeCode == 41 || causeCode == 42 || causeCode == 44 || causeCode == 49 || causeCode == 58 || causeCode == 65535) {
                            CellLocation loc = this.mPhone.getCellLocation();
                            int cid = -1;
                            if (loc != null) {
                                if (isPhoneTypeGsm()) {
                                    cid = ((GsmCellLocation) loc).getCid();
                                } else {
                                    cid = ((CdmaCellLocation) loc).getBaseStationId();
                                }
                            }
                            EventLog.writeEvent(EventLogTags.CALL_DROP, new Object[]{Integer.valueOf(causeCode), Integer.valueOf(cid), Integer.valueOf(TelephonyManager.getDefault().getNetworkType())});
                        }
                        causeCode2 = this.mDroppedDuringPoll.size();
                        for (int i2 = 0; i2 < causeCode2; i2++) {
                            ((GsmCdmaConnection) this.mDroppedDuringPoll.get(i2)).onRemoteDisconnect(causeCode, vendorCause);
                        }
                        updatePhoneState();
                        this.mPhone.notifyPreciseCallStateChanged();
                        this.mMetrics.writeRilCallList(this.mPhone.getPhoneId(), this.mDroppedDuringPoll);
                        this.mDroppedDuringPoll.clear();
                        this.mIsInCsRedial = false;
                        return;
                    default:
                        switch (i) {
                            case 8:
                                if (isPhoneTypeGsm()) {
                                    this.callSwitchPending = false;
                                    if (((AsyncResult) msg.obj).exception != null) {
                                        this.mPhone.notifySuppServiceFailed(getFailedService(msg.what));
                                    }
                                    operationComplete();
                                    return;
                                }
                                return;
                            case 9:
                                handleRadioAvailable();
                                return;
                            case 10:
                                handleRadioNotAvailable();
                                return;
                            case 11:
                                if (isPhoneTypeGsm()) {
                                    if (!(((AsyncResult) msg.obj).exception == null || msg.obj.exception == null)) {
                                        this.mPhone.notifySuppServiceFailed(getFailedService(msg.what));
                                        List<Connection> conn = this.mForegroundCall.getConnections();
                                        if (conn != null) {
                                            Rlog.d(LOG_TAG, "Notify merge failure");
                                            if (conn.size() != 0) {
                                                ((Connection) conn.get(0)).onConferenceMergeFailed();
                                            }
                                        }
                                    }
                                    operationComplete();
                                    return;
                                }
                                return;
                            case 12:
                            case 13:
                                if (isPhoneTypeGsm()) {
                                    if (((AsyncResult) msg.obj).exception != null) {
                                        this.mPhone.notifySuppServiceFailed(getFailedService(msg.what));
                                    }
                                    operationComplete();
                                    return;
                                } else if (msg.what != 8) {
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("unexpected event ");
                                    stringBuilder.append(msg.what);
                                    stringBuilder.append(" not handled by phone type ");
                                    stringBuilder.append(this.mPhone.getPhoneType());
                                    throw new RuntimeException(stringBuilder.toString());
                                } else {
                                    return;
                                }
                            case 14:
                                if (isPhoneTypeGsm()) {
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("unexpected event ");
                                    stringBuilder.append(msg.what);
                                    stringBuilder.append(" not handled by phone type ");
                                    stringBuilder.append(this.mPhone.getPhoneType());
                                    throw new RuntimeException(stringBuilder.toString());
                                }
                                if (this.mPendingCallInEcm) {
                                    this.mCi.dial(this.mPendingMO.getDialAddress(), this.mPendingCallClirMode, obtainCompleteMessage());
                                    this.mPendingCallInEcm = false;
                                }
                                this.mPhone.unsetOnEcbModeExitResponse(this);
                                return;
                            case 15:
                                if (isPhoneTypeGsm()) {
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("unexpected event ");
                                    stringBuilder.append(msg.what);
                                    stringBuilder.append(" not handled by phone type ");
                                    stringBuilder.append(this.mPhone.getPhoneType());
                                    throw new RuntimeException(stringBuilder.toString());
                                }
                                ar = (AsyncResult) msg.obj;
                                if (ar.exception == null) {
                                    handleCallWaitingInfo((CdmaCallWaitingNotification) ar.result);
                                    Rlog.d(LOG_TAG, "Event EVENT_CALL_WAITING_INFO_CDMA Received");
                                    return;
                                }
                                return;
                            case 16:
                                if (isPhoneTypeGsm()) {
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("unexpected event ");
                                    stringBuilder.append(msg.what);
                                    stringBuilder.append(" not handled by phone type ");
                                    stringBuilder.append(this.mPhone.getPhoneType());
                                    throw new RuntimeException(stringBuilder.toString());
                                } else if (msg.obj.exception == null && this.mPendingMO != null) {
                                    this.mPendingMO.onConnectedInOrOut();
                                    this.mPendingMO = null;
                                    return;
                                } else {
                                    return;
                                }
                            default:
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("unexpected event ");
                                stringBuilder.append(msg.what);
                                stringBuilder.append(" not handled by phone type ");
                                stringBuilder.append(this.mPhone.getPhoneType());
                                throw new RuntimeException(stringBuilder.toString());
                        }
                }
            } else if (CallTracker.IS_SUPPORT_RIL_RECOVERY) {
                SystemProperties.set("ril.reset.write_dump", "true");
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("restartRild for hungup call,ril.reset.write_dump=");
                stringBuilder3.append(SystemProperties.get("ril.reset.write_dump"));
                log(stringBuilder3.toString());
                this.mCi.restartRild(null);
            }
        } else if (isPhoneTypeGsm()) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("unexpected event ");
            stringBuilder.append(msg.what);
            stringBuilder.append(" not handled by phone type ");
            stringBuilder.append(this.mPhone.getPhoneType());
            throw new RuntimeException(stringBuilder.toString());
        } else if (((AsyncResult) msg.obj).exception == null) {
            postDelayed(new Runnable() {
                public void run() {
                    if (GsmCdmaCallTracker.this.mPendingMO != null) {
                        GsmCdmaCallTracker.this.mCi.sendCDMAFeatureCode(GsmCdmaCallTracker.this.mPendingMO.getAddress(), GsmCdmaCallTracker.this.obtainMessage(16));
                    }
                }
            }, (long) this.m3WayCallFlashDelay);
        } else {
            this.mPendingMO = null;
            Rlog.w(LOG_TAG, "exception happened on Blank Flash for 3-way call");
        }
    }

    private void checkAndEnableDataCallAfterEmergencyCallDropped() {
        if (this.mIsInEmergencyCall) {
            this.mIsInEmergencyCall = false;
            boolean inEcm = this.mPhone.isInEcm();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("checkAndEnableDataCallAfterEmergencyCallDropped,isInEcm=");
            stringBuilder.append(inEcm);
            log(stringBuilder.toString());
            if (!inEcm) {
                this.mPhone.mDcTracker.setInternalDataEnabled(true);
                this.mPhone.notifyEmergencyCallRegistrants(false);
            }
            this.mPhone.sendEmergencyCallStateChange(false);
        }
    }

    private Connection checkMtFindNewRinging(DriverCall dc, int i) {
        if (this.mConnections[i].getCall() == this.mRingingCall) {
            Connection newRinging = this.mConnections[i];
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Notify new ring ");
            stringBuilder.append(dc);
            log(stringBuilder.toString());
            return newRinging;
        }
        String str = LOG_TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Phantom call appeared ");
        stringBuilder2.append(dc);
        Rlog.e(str, stringBuilder2.toString());
        if (dc.state == DriverCall.State.ALERTING || dc.state == DriverCall.State.DIALING) {
            return null;
        }
        this.mConnections[i].onConnectedInOrOut();
        if (dc.state != DriverCall.State.HOLDING) {
            return null;
        }
        this.mConnections[i].onStartedHolding();
        return null;
    }

    public boolean isInEmergencyCall() {
        return this.mIsInEmergencyCall;
    }

    private boolean isPhoneTypeGsm() {
        return this.mPhone.getPhoneType() == 1;
    }

    public GsmCdmaPhone getPhone() {
        return this.mPhone;
    }

    protected void log(String msg) {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[");
        stringBuilder.append(this.mPhone.getPhoneId());
        stringBuilder.append("] ");
        stringBuilder.append(msg);
        Rlog.d(str, stringBuilder.toString());
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        int i;
        pw.println("GsmCdmaCallTracker extends:");
        super.dump(fd, pw, args);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mConnections: length=");
        stringBuilder.append(this.mConnections.length);
        pw.println(stringBuilder.toString());
        for (i = 0; i < this.mConnections.length; i++) {
            pw.printf("  mConnections[%d]=%s\n", new Object[]{Integer.valueOf(i), this.mConnections[i]});
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(" mVoiceCallEndedRegistrants=");
        stringBuilder2.append(this.mVoiceCallEndedRegistrants);
        pw.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(" mVoiceCallStartedRegistrants=");
        stringBuilder2.append(this.mVoiceCallStartedRegistrants);
        pw.println(stringBuilder2.toString());
        if (!isPhoneTypeGsm()) {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(" mCallWaitingRegistrants=");
            stringBuilder2.append(this.mCallWaitingRegistrants);
            pw.println(stringBuilder2.toString());
        }
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(" mDroppedDuringPoll: size=");
        stringBuilder2.append(this.mDroppedDuringPoll.size());
        pw.println(stringBuilder2.toString());
        for (i = 0; i < this.mDroppedDuringPoll.size(); i++) {
            pw.printf("  mDroppedDuringPoll[%d]=%s\n", new Object[]{Integer.valueOf(i), this.mDroppedDuringPoll.get(i)});
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mRingingCall=");
        stringBuilder.append(this.mRingingCall);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mForegroundCall=");
        stringBuilder.append(this.mForegroundCall);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mBackgroundCall=");
        stringBuilder.append(this.mBackgroundCall);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mPendingMO=");
        stringBuilder.append(this.mPendingMO);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mHangupPendingMO=");
        stringBuilder.append(this.mHangupPendingMO);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mPhone=");
        stringBuilder.append(this.mPhone);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mDesiredMute=");
        stringBuilder.append(this.mDesiredMute);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mState=");
        stringBuilder.append(this.mState);
        pw.println(stringBuilder.toString());
        if (!isPhoneTypeGsm()) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(" mPendingCallInEcm=");
            stringBuilder.append(this.mPendingCallInEcm);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(" mIsInEmergencyCall=");
            stringBuilder.append(this.mIsInEmergencyCall);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(" mPendingCallClirMode=");
            stringBuilder.append(this.mPendingCallClirMode);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(" mIsEcmTimerCanceled=");
            stringBuilder.append(this.mIsEcmTimerCanceled);
            pw.println(stringBuilder.toString());
        }
    }

    public State getState() {
        return this.mState;
    }

    public int getMaxConnectionsPerCall() {
        if (this.mPhone.isPhoneTypeGsm()) {
            return 5;
        }
        return 1;
    }

    public void cleanupCalls() {
        pollCallsWhenSafe();
    }

    public boolean isInCsRedial() {
        return this.mIsInCsRedial;
    }

    private void syncHoConnection() {
        Phone imsPhone = this.mPhone.getImsPhone();
        if (imsPhone != null && this.mSrvccState == SrvccState.STARTED) {
            ArrayList<Connection> hoConnections = imsPhone.getHandoverConnection();
            if (hoConnections != null) {
                int list_size = hoConnections.size();
                for (int i = 0; i < list_size; i++) {
                    Connection conn = (Connection) hoConnections.get(i);
                    if (!(this.mHandoverConnections.contains(conn) || this.mRemovedHandoverConnections.contains(conn))) {
                        this.mHandoverConnections.add(conn);
                    }
                }
            }
        }
    }

    public void markCallRejectCause(String telecomCallId, int cause) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("markCallRejectByUser, telecomCallId: ");
        stringBuilder.append(telecomCallId);
        stringBuilder.append(", cause:");
        stringBuilder.append(cause);
        log(stringBuilder.toString());
        if (this.mHwGCT == null) {
            log("mHwGCT is null!");
        } else {
            this.mHwGCT.markCallRejectCause(telecomCallId, cause);
        }
    }
}
