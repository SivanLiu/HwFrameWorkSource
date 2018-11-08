package com.android.internal.telephony;

import android.os.AsyncResult;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.telephony.CellInfo;
import android.telephony.NetworkScanRequest;
import android.telephony.RadioAccessSpecifier;
import android.util.Log;
import com.android.internal.telephony.CommandException.Error;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public final class NetworkScanRequestTracker {
    private static final int CMD_INTERRUPT_NETWORK_SCAN = 6;
    private static final int CMD_START_NETWORK_SCAN = 1;
    private static final int CMD_STOP_NETWORK_SCAN = 4;
    private static final int EVENT_INTERRUPT_NETWORK_SCAN_DONE = 7;
    private static final int EVENT_RECEIVE_NETWORK_SCAN_RESULT = 3;
    private static final int EVENT_START_NETWORK_SCAN_DONE = 2;
    private static final int EVENT_STOP_NETWORK_SCAN_DONE = 5;
    private static final String TAG = "ScanRequestTracker";
    private final Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    NetworkScanRequestTracker.this.mScheduler.doStartScan((NetworkScanRequestInfo) msg.obj);
                    return;
                case 2:
                    NetworkScanRequestTracker.this.mScheduler.startScanDone((AsyncResult) msg.obj);
                    return;
                case 3:
                    NetworkScanRequestTracker.this.mScheduler.receiveResult((AsyncResult) msg.obj);
                    return;
                case 4:
                    NetworkScanRequestTracker.this.mScheduler.doStopScan(msg.arg1);
                    return;
                case 5:
                    NetworkScanRequestTracker.this.mScheduler.stopScanDone((AsyncResult) msg.obj);
                    return;
                case 6:
                    NetworkScanRequestTracker.this.mScheduler.doInterruptScan(msg.arg1);
                    return;
                case 7:
                    NetworkScanRequestTracker.this.mScheduler.interruptScanDone((AsyncResult) msg.obj);
                    return;
                default:
                    return;
            }
        }
    };
    private final AtomicInteger mNextNetworkScanRequestId = new AtomicInteger(1);
    private final NetworkScanRequestScheduler mScheduler = new NetworkScanRequestScheduler();

    class NetworkScanRequestInfo implements DeathRecipient {
        private final IBinder mBinder;
        private boolean mIsBinderDead = false;
        private final Messenger mMessenger;
        private final Phone mPhone;
        private final int mPid = Binder.getCallingPid();
        private final NetworkScanRequest mRequest;
        private final int mScanId;
        private final int mUid = Binder.getCallingUid();

        NetworkScanRequestInfo(NetworkScanRequest r, Messenger m, IBinder b, int id, Phone phone) {
            this.mRequest = r;
            this.mMessenger = m;
            this.mBinder = b;
            this.mScanId = id;
            this.mPhone = phone;
            try {
                this.mBinder.linkToDeath(this, 0);
            } catch (RemoteException e) {
                binderDied();
            }
        }

        synchronized void setIsBinderDead(boolean val) {
            this.mIsBinderDead = val;
        }

        synchronized boolean getIsBinderDead() {
            return this.mIsBinderDead;
        }

        NetworkScanRequest getRequest() {
            return this.mRequest;
        }

        void unlinkDeathRecipient() {
            if (this.mBinder != null) {
                this.mBinder.unlinkToDeath(this, 0);
            }
        }

        public void binderDied() {
            Log.e(NetworkScanRequestTracker.TAG, "PhoneInterfaceManager NetworkScanRequestInfo binderDied(" + this.mRequest + ", " + this.mBinder + ")");
            setIsBinderDead(true);
            NetworkScanRequestTracker.this.interruptNetworkScan(this.mScanId);
        }
    }

    private class NetworkScanRequestScheduler {
        private static final /* synthetic */ int[] -com-android-internal-telephony-CommandException$ErrorSwitchesValues = null;
        final /* synthetic */ int[] $SWITCH_TABLE$com$android$internal$telephony$CommandException$Error;
        private NetworkScanRequestInfo mLiveRequestInfo;
        private NetworkScanRequestInfo mPendingRequestInfo;

        private static /* synthetic */ int[] -getcom-android-internal-telephony-CommandException$ErrorSwitchesValues() {
            if (-com-android-internal-telephony-CommandException$ErrorSwitchesValues != null) {
                return -com-android-internal-telephony-CommandException$ErrorSwitchesValues;
            }
            int[] iArr = new int[Error.values().length];
            try {
                iArr[Error.ABORTED.ordinal()] = 9;
            } catch (NoSuchFieldError e) {
            }
            try {
                iArr[Error.DEVICE_IN_USE.ordinal()] = 1;
            } catch (NoSuchFieldError e2) {
            }
            try {
                iArr[Error.DIAL_MODIFIED_TO_DIAL.ordinal()] = 10;
            } catch (NoSuchFieldError e3) {
            }
            try {
                iArr[Error.DIAL_MODIFIED_TO_SS.ordinal()] = 11;
            } catch (NoSuchFieldError e4) {
            }
            try {
                iArr[Error.DIAL_MODIFIED_TO_USSD.ordinal()] = 12;
            } catch (NoSuchFieldError e5) {
            }
            try {
                iArr[Error.EMPTY_RECORD.ordinal()] = 13;
            } catch (NoSuchFieldError e6) {
            }
            try {
                iArr[Error.ENCODING_ERR.ordinal()] = 14;
            } catch (NoSuchFieldError e7) {
            }
            try {
                iArr[Error.FDN_CHECK_FAILURE.ordinal()] = 15;
            } catch (NoSuchFieldError e8) {
            }
            try {
                iArr[Error.GENERIC_FAILURE.ordinal()] = 16;
            } catch (NoSuchFieldError e9) {
            }
            try {
                iArr[Error.ILLEGAL_SIM_OR_ME.ordinal()] = 17;
            } catch (NoSuchFieldError e10) {
            }
            try {
                iArr[Error.INTERNAL_ERR.ordinal()] = 2;
            } catch (NoSuchFieldError e11) {
            }
            try {
                iArr[Error.INVALID_ARGUMENTS.ordinal()] = 3;
            } catch (NoSuchFieldError e12) {
            }
            try {
                iArr[Error.INVALID_CALL_ID.ordinal()] = 18;
            } catch (NoSuchFieldError e13) {
            }
            try {
                iArr[Error.INVALID_MODEM_STATE.ordinal()] = 19;
            } catch (NoSuchFieldError e14) {
            }
            try {
                iArr[Error.INVALID_PARAMETER.ordinal()] = 20;
            } catch (NoSuchFieldError e15) {
            }
            try {
                iArr[Error.INVALID_RESPONSE.ordinal()] = 21;
            } catch (NoSuchFieldError e16) {
            }
            try {
                iArr[Error.INVALID_SIM_STATE.ordinal()] = 22;
            } catch (NoSuchFieldError e17) {
            }
            try {
                iArr[Error.INVALID_SMSC_ADDRESS.ordinal()] = 23;
            } catch (NoSuchFieldError e18) {
            }
            try {
                iArr[Error.INVALID_SMS_FORMAT.ordinal()] = 24;
            } catch (NoSuchFieldError e19) {
            }
            try {
                iArr[Error.INVALID_STATE.ordinal()] = 25;
            } catch (NoSuchFieldError e20) {
            }
            try {
                iArr[Error.LCE_NOT_SUPPORTED.ordinal()] = 26;
            } catch (NoSuchFieldError e21) {
            }
            try {
                iArr[Error.MISSING_RESOURCE.ordinal()] = 27;
            } catch (NoSuchFieldError e22) {
            }
            try {
                iArr[Error.MODEM_ERR.ordinal()] = 4;
            } catch (NoSuchFieldError e23) {
            }
            try {
                iArr[Error.MODE_NOT_SUPPORTED.ordinal()] = 28;
            } catch (NoSuchFieldError e24) {
            }
            try {
                iArr[Error.NETWORK_ERR.ordinal()] = 29;
            } catch (NoSuchFieldError e25) {
            }
            try {
                iArr[Error.NETWORK_NOT_READY.ordinal()] = 30;
            } catch (NoSuchFieldError e26) {
            }
            try {
                iArr[Error.NETWORK_REJECT.ordinal()] = 31;
            } catch (NoSuchFieldError e27) {
            }
            try {
                iArr[Error.NOT_PROVISIONED.ordinal()] = 32;
            } catch (NoSuchFieldError e28) {
            }
            try {
                iArr[Error.NO_MEMORY.ordinal()] = 5;
            } catch (NoSuchFieldError e29) {
            }
            try {
                iArr[Error.NO_NETWORK_FOUND.ordinal()] = 33;
            } catch (NoSuchFieldError e30) {
            }
            try {
                iArr[Error.NO_RESOURCES.ordinal()] = 34;
            } catch (NoSuchFieldError e31) {
            }
            try {
                iArr[Error.NO_SMS_TO_ACK.ordinal()] = 35;
            } catch (NoSuchFieldError e32) {
            }
            try {
                iArr[Error.NO_SUBSCRIPTION.ordinal()] = 36;
            } catch (NoSuchFieldError e33) {
            }
            try {
                iArr[Error.NO_SUCH_ELEMENT.ordinal()] = 37;
            } catch (NoSuchFieldError e34) {
            }
            try {
                iArr[Error.NO_SUCH_ENTRY.ordinal()] = 38;
            } catch (NoSuchFieldError e35) {
            }
            try {
                iArr[Error.OEM_ERROR_1.ordinal()] = 39;
            } catch (NoSuchFieldError e36) {
            }
            try {
                iArr[Error.OEM_ERROR_10.ordinal()] = 40;
            } catch (NoSuchFieldError e37) {
            }
            try {
                iArr[Error.OEM_ERROR_11.ordinal()] = 41;
            } catch (NoSuchFieldError e38) {
            }
            try {
                iArr[Error.OEM_ERROR_12.ordinal()] = 42;
            } catch (NoSuchFieldError e39) {
            }
            try {
                iArr[Error.OEM_ERROR_13.ordinal()] = 43;
            } catch (NoSuchFieldError e40) {
            }
            try {
                iArr[Error.OEM_ERROR_14.ordinal()] = 44;
            } catch (NoSuchFieldError e41) {
            }
            try {
                iArr[Error.OEM_ERROR_15.ordinal()] = 45;
            } catch (NoSuchFieldError e42) {
            }
            try {
                iArr[Error.OEM_ERROR_16.ordinal()] = 46;
            } catch (NoSuchFieldError e43) {
            }
            try {
                iArr[Error.OEM_ERROR_17.ordinal()] = 47;
            } catch (NoSuchFieldError e44) {
            }
            try {
                iArr[Error.OEM_ERROR_18.ordinal()] = 48;
            } catch (NoSuchFieldError e45) {
            }
            try {
                iArr[Error.OEM_ERROR_19.ordinal()] = 49;
            } catch (NoSuchFieldError e46) {
            }
            try {
                iArr[Error.OEM_ERROR_2.ordinal()] = 50;
            } catch (NoSuchFieldError e47) {
            }
            try {
                iArr[Error.OEM_ERROR_20.ordinal()] = 51;
            } catch (NoSuchFieldError e48) {
            }
            try {
                iArr[Error.OEM_ERROR_21.ordinal()] = 52;
            } catch (NoSuchFieldError e49) {
            }
            try {
                iArr[Error.OEM_ERROR_22.ordinal()] = 53;
            } catch (NoSuchFieldError e50) {
            }
            try {
                iArr[Error.OEM_ERROR_23.ordinal()] = 54;
            } catch (NoSuchFieldError e51) {
            }
            try {
                iArr[Error.OEM_ERROR_24.ordinal()] = 55;
            } catch (NoSuchFieldError e52) {
            }
            try {
                iArr[Error.OEM_ERROR_25.ordinal()] = 56;
            } catch (NoSuchFieldError e53) {
            }
            try {
                iArr[Error.OEM_ERROR_3.ordinal()] = 57;
            } catch (NoSuchFieldError e54) {
            }
            try {
                iArr[Error.OEM_ERROR_4.ordinal()] = 58;
            } catch (NoSuchFieldError e55) {
            }
            try {
                iArr[Error.OEM_ERROR_5.ordinal()] = 59;
            } catch (NoSuchFieldError e56) {
            }
            try {
                iArr[Error.OEM_ERROR_6.ordinal()] = 60;
            } catch (NoSuchFieldError e57) {
            }
            try {
                iArr[Error.OEM_ERROR_7.ordinal()] = 61;
            } catch (NoSuchFieldError e58) {
            }
            try {
                iArr[Error.OEM_ERROR_8.ordinal()] = 62;
            } catch (NoSuchFieldError e59) {
            }
            try {
                iArr[Error.OEM_ERROR_9.ordinal()] = 63;
            } catch (NoSuchFieldError e60) {
            }
            try {
                iArr[Error.OPERATION_NOT_ALLOWED.ordinal()] = 6;
            } catch (NoSuchFieldError e61) {
            }
            try {
                iArr[Error.OP_NOT_ALLOWED_BEFORE_REG_NW.ordinal()] = 64;
            } catch (NoSuchFieldError e62) {
            }
            try {
                iArr[Error.OP_NOT_ALLOWED_DURING_VOICE_CALL.ordinal()] = 65;
            } catch (NoSuchFieldError e63) {
            }
            try {
                iArr[Error.PASSWORD_INCORRECT.ordinal()] = 66;
            } catch (NoSuchFieldError e64) {
            }
            try {
                iArr[Error.RADIO_NOT_AVAILABLE.ordinal()] = 7;
            } catch (NoSuchFieldError e65) {
            }
            try {
                iArr[Error.REQUEST_NOT_SUPPORTED.ordinal()] = 8;
            } catch (NoSuchFieldError e66) {
            }
            try {
                iArr[Error.REQUEST_RATE_LIMITED.ordinal()] = 67;
            } catch (NoSuchFieldError e67) {
            }
            try {
                iArr[Error.SERVICE_NOT_SUBSCRIBED.ordinal()] = 68;
            } catch (NoSuchFieldError e68) {
            }
            try {
                iArr[Error.SIM_ABSENT.ordinal()] = 69;
            } catch (NoSuchFieldError e69) {
            }
            try {
                iArr[Error.SIM_ALREADY_POWERED_OFF.ordinal()] = 70;
            } catch (NoSuchFieldError e70) {
            }
            try {
                iArr[Error.SIM_ALREADY_POWERED_ON.ordinal()] = 71;
            } catch (NoSuchFieldError e71) {
            }
            try {
                iArr[Error.SIM_BUSY.ordinal()] = 72;
            } catch (NoSuchFieldError e72) {
            }
            try {
                iArr[Error.SIM_DATA_NOT_AVAILABLE.ordinal()] = 73;
            } catch (NoSuchFieldError e73) {
            }
            try {
                iArr[Error.SIM_ERR.ordinal()] = 74;
            } catch (NoSuchFieldError e74) {
            }
            try {
                iArr[Error.SIM_FULL.ordinal()] = 75;
            } catch (NoSuchFieldError e75) {
            }
            try {
                iArr[Error.SIM_PIN2.ordinal()] = 76;
            } catch (NoSuchFieldError e76) {
            }
            try {
                iArr[Error.SIM_PUK2.ordinal()] = 77;
            } catch (NoSuchFieldError e77) {
            }
            try {
                iArr[Error.SIM_SAP_CONNECT_FAILURE.ordinal()] = 78;
            } catch (NoSuchFieldError e78) {
            }
            try {
                iArr[Error.SIM_SAP_CONNECT_OK_CALL_ONGOING.ordinal()] = 79;
            } catch (NoSuchFieldError e79) {
            }
            try {
                iArr[Error.SIM_SAP_MSG_SIZE_TOO_LARGE.ordinal()] = 80;
            } catch (NoSuchFieldError e80) {
            }
            try {
                iArr[Error.SIM_SAP_MSG_SIZE_TOO_SMALL.ordinal()] = 81;
            } catch (NoSuchFieldError e81) {
            }
            try {
                iArr[Error.SMS_FAIL_RETRY.ordinal()] = 82;
            } catch (NoSuchFieldError e82) {
            }
            try {
                iArr[Error.SS_MODIFIED_TO_DIAL.ordinal()] = 83;
            } catch (NoSuchFieldError e83) {
            }
            try {
                iArr[Error.SS_MODIFIED_TO_SS.ordinal()] = 84;
            } catch (NoSuchFieldError e84) {
            }
            try {
                iArr[Error.SS_MODIFIED_TO_USSD.ordinal()] = 85;
            } catch (NoSuchFieldError e85) {
            }
            try {
                iArr[Error.SUBSCRIPTION_NOT_AVAILABLE.ordinal()] = 86;
            } catch (NoSuchFieldError e86) {
            }
            try {
                iArr[Error.SUBSCRIPTION_NOT_SUPPORTED.ordinal()] = 87;
            } catch (NoSuchFieldError e87) {
            }
            try {
                iArr[Error.SYSTEM_ERR.ordinal()] = 88;
            } catch (NoSuchFieldError e88) {
            }
            try {
                iArr[Error.USSD_MODIFIED_TO_DIAL.ordinal()] = 89;
            } catch (NoSuchFieldError e89) {
            }
            try {
                iArr[Error.USSD_MODIFIED_TO_SS.ordinal()] = 90;
            } catch (NoSuchFieldError e90) {
            }
            try {
                iArr[Error.USSD_MODIFIED_TO_USSD.ordinal()] = 91;
            } catch (NoSuchFieldError e91) {
            }
            try {
                iArr[Error.UT_NO_CONNECTION.ordinal()] = 92;
            } catch (NoSuchFieldError e92) {
            }
            -com-android-internal-telephony-CommandException$ErrorSwitchesValues = iArr;
            return iArr;
        }

        private NetworkScanRequestScheduler() {
        }

        private int rilErrorToScanError(int rilError) {
            switch (rilError) {
                case 0:
                    return 0;
                case 1:
                    Log.e(NetworkScanRequestTracker.TAG, "rilErrorToScanError: RADIO_NOT_AVAILABLE");
                    return 1;
                case 6:
                    Log.e(NetworkScanRequestTracker.TAG, "rilErrorToScanError: REQUEST_NOT_SUPPORTED");
                    return 4;
                case 37:
                    Log.e(NetworkScanRequestTracker.TAG, "rilErrorToScanError: NO_MEMORY");
                    return 1;
                case 38:
                    Log.e(NetworkScanRequestTracker.TAG, "rilErrorToScanError: INTERNAL_ERR");
                    return 1;
                case 40:
                    Log.e(NetworkScanRequestTracker.TAG, "rilErrorToScanError: MODEM_ERR");
                    return 1;
                case 44:
                    Log.e(NetworkScanRequestTracker.TAG, "rilErrorToScanError: INVALID_ARGUMENTS");
                    return 2;
                case 54:
                    Log.e(NetworkScanRequestTracker.TAG, "rilErrorToScanError: OPERATION_NOT_ALLOWED");
                    return 1;
                case 64:
                    Log.e(NetworkScanRequestTracker.TAG, "rilErrorToScanError: DEVICE_IN_USE");
                    return 3;
                default:
                    Log.e(NetworkScanRequestTracker.TAG, "rilErrorToScanError: Unexpected RadioError " + rilError);
                    return 10000;
            }
        }

        private int commandExceptionErrorToScanError(Error error) {
            switch (-getcom-android-internal-telephony-CommandException$ErrorSwitchesValues()[error.ordinal()]) {
                case 1:
                    Log.e(NetworkScanRequestTracker.TAG, "commandExceptionErrorToScanError: DEVICE_IN_USE");
                    return 3;
                case 2:
                    Log.e(NetworkScanRequestTracker.TAG, "commandExceptionErrorToScanError: INTERNAL_ERR");
                    return 1;
                case 3:
                    Log.e(NetworkScanRequestTracker.TAG, "commandExceptionErrorToScanError: INVALID_ARGUMENTS");
                    return 2;
                case 4:
                    Log.e(NetworkScanRequestTracker.TAG, "commandExceptionErrorToScanError: MODEM_ERR");
                    return 1;
                case 5:
                    Log.e(NetworkScanRequestTracker.TAG, "commandExceptionErrorToScanError: NO_MEMORY");
                    return 1;
                case 6:
                    Log.e(NetworkScanRequestTracker.TAG, "commandExceptionErrorToScanError: OPERATION_NOT_ALLOWED");
                    return 1;
                case 7:
                    Log.e(NetworkScanRequestTracker.TAG, "commandExceptionErrorToScanError: RADIO_NOT_AVAILABLE");
                    return 1;
                case 8:
                    Log.e(NetworkScanRequestTracker.TAG, "commandExceptionErrorToScanError: REQUEST_NOT_SUPPORTED");
                    return 4;
                default:
                    Log.e(NetworkScanRequestTracker.TAG, "commandExceptionErrorToScanError: Unexpected CommandExceptionError " + error);
                    return 10000;
            }
        }

        private void doStartScan(NetworkScanRequestInfo nsri) {
            if (nsri == null) {
                Log.e(NetworkScanRequestTracker.TAG, "CMD_START_NETWORK_SCAN: nsri is null");
            } else if (!NetworkScanRequestTracker.this.isValidScan(nsri)) {
                NetworkScanRequestTracker.this.notifyMessenger(nsri, 2, 2, null);
            } else if (nsri.getIsBinderDead()) {
                Log.e(NetworkScanRequestTracker.TAG, "CMD_START_NETWORK_SCAN: Binder has died");
            } else {
                if (!(startNewScan(nsri) || interruptLiveScan(nsri) || cacheScan(nsri))) {
                    NetworkScanRequestTracker.this.notifyMessenger(nsri, 2, 3, null);
                }
            }
        }

        /* JADX WARNING: inconsistent code. */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        private synchronized void startScanDone(AsyncResult ar) {
            NetworkScanRequestInfo nsri = ar.userObj;
            if (nsri == null) {
                Log.e(NetworkScanRequestTracker.TAG, "EVENT_START_NETWORK_SCAN_DONE: nsri is null");
            } else if (this.mLiveRequestInfo == null || nsri.mScanId != this.mLiveRequestInfo.mScanId) {
                Log.e(NetworkScanRequestTracker.TAG, "EVENT_START_NETWORK_SCAN_DONE: nsri does not match mLiveRequestInfo");
            } else if (ar.exception != null || ar.result == null) {
                NetworkScanRequestTracker.this.logEmptyResultOrException(ar);
                if (ar.exception != null) {
                    deleteScanAndMayNotify(nsri, commandExceptionErrorToScanError(((CommandException) ar.exception).getCommandError()), true);
                } else {
                    Log.wtf(NetworkScanRequestTracker.TAG, "EVENT_START_NETWORK_SCAN_DONE: ar.exception can not be null!");
                }
            } else {
                nsri.mPhone.mCi.registerForNetworkScanResult(NetworkScanRequestTracker.this.mHandler, 3, nsri);
            }
        }

        private void receiveResult(AsyncResult ar) {
            NetworkScanRequestInfo nsri = ar.userObj;
            if (nsri == null) {
                Log.e(NetworkScanRequestTracker.TAG, "EVENT_RECEIVE_NETWORK_SCAN_RESULT: nsri is null");
                return;
            }
            if (ar.exception != null || ar.result == null) {
                NetworkScanRequestTracker.this.logEmptyResultOrException(ar);
                deleteScanAndMayNotify(nsri, 10000, true);
                nsri.mPhone.mCi.unregisterForNetworkScanResult(NetworkScanRequestTracker.this.mHandler);
            } else {
                NetworkScanResult nsr = ar.result;
                if (nsr.scanError == 0) {
                    NetworkScanRequestTracker.this.notifyMessenger(nsri, 1, rilErrorToScanError(nsr.scanError), nsr.networkInfos);
                    if (nsr.scanStatus == 2) {
                        deleteScanAndMayNotify(nsri, 0, true);
                        nsri.mPhone.mCi.unregisterForNetworkScanResult(NetworkScanRequestTracker.this.mHandler);
                    }
                } else {
                    if (nsr.networkInfos != null) {
                        NetworkScanRequestTracker.this.notifyMessenger(nsri, 1, 0, nsr.networkInfos);
                    }
                    deleteScanAndMayNotify(nsri, rilErrorToScanError(nsr.scanError), true);
                    nsri.mPhone.mCi.unregisterForNetworkScanResult(NetworkScanRequestTracker.this.mHandler);
                }
            }
        }

        private synchronized void doStopScan(int scanId) {
            if (this.mLiveRequestInfo != null && scanId == this.mLiveRequestInfo.mScanId) {
                this.mLiveRequestInfo.mPhone.stopNetworkScan(NetworkScanRequestTracker.this.mHandler.obtainMessage(5, this.mLiveRequestInfo));
            } else if (this.mPendingRequestInfo == null || scanId != this.mPendingRequestInfo.mScanId) {
                Log.e(NetworkScanRequestTracker.TAG, "stopScan: scan " + scanId + " does not exist!");
            } else {
                NetworkScanRequestTracker.this.notifyMessenger(this.mPendingRequestInfo, 3, 0, null);
                this.mPendingRequestInfo = null;
            }
        }

        private void stopScanDone(AsyncResult ar) {
            NetworkScanRequestInfo nsri = ar.userObj;
            if (nsri == null) {
                Log.e(NetworkScanRequestTracker.TAG, "EVENT_STOP_NETWORK_SCAN_DONE: nsri is null");
                return;
            }
            if (ar.exception != null || ar.result == null) {
                NetworkScanRequestTracker.this.logEmptyResultOrException(ar);
                if (ar.exception != null) {
                    deleteScanAndMayNotify(nsri, commandExceptionErrorToScanError(((CommandException) ar.exception).getCommandError()), true);
                } else {
                    Log.wtf(NetworkScanRequestTracker.TAG, "EVENT_STOP_NETWORK_SCAN_DONE: ar.exception can not be null!");
                }
            } else {
                deleteScanAndMayNotify(nsri, 0, true);
            }
            nsri.mPhone.mCi.unregisterForNetworkScanResult(NetworkScanRequestTracker.this.mHandler);
        }

        private synchronized void doInterruptScan(int scanId) {
            if (this.mLiveRequestInfo == null || scanId != this.mLiveRequestInfo.mScanId) {
                Log.e(NetworkScanRequestTracker.TAG, "doInterruptScan: scan " + scanId + " does not exist!");
            } else {
                this.mLiveRequestInfo.mPhone.stopNetworkScan(NetworkScanRequestTracker.this.mHandler.obtainMessage(7, this.mLiveRequestInfo));
            }
        }

        private void interruptScanDone(AsyncResult ar) {
            NetworkScanRequestInfo nsri = ar.userObj;
            if (nsri == null) {
                Log.e(NetworkScanRequestTracker.TAG, "EVENT_INTERRUPT_NETWORK_SCAN_DONE: nsri is null");
                return;
            }
            nsri.mPhone.mCi.unregisterForNetworkScanResult(NetworkScanRequestTracker.this.mHandler);
            deleteScanAndMayNotify(nsri, 0, false);
        }

        private synchronized boolean interruptLiveScan(NetworkScanRequestInfo nsri) {
            if (this.mLiveRequestInfo == null || this.mPendingRequestInfo != null || nsri.mUid != 1000 || this.mLiveRequestInfo.mUid == 1000) {
                return false;
            }
            doInterruptScan(this.mLiveRequestInfo.mScanId);
            this.mPendingRequestInfo = nsri;
            NetworkScanRequestTracker.this.notifyMessenger(this.mLiveRequestInfo, 2, 10002, null);
            return true;
        }

        private boolean cacheScan(NetworkScanRequestInfo nsri) {
            return false;
        }

        private synchronized boolean startNewScan(NetworkScanRequestInfo nsri) {
            if (this.mLiveRequestInfo != null) {
                return false;
            }
            this.mLiveRequestInfo = nsri;
            nsri.mPhone.startNetworkScan(nsri.getRequest(), NetworkScanRequestTracker.this.mHandler.obtainMessage(2, nsri));
            return true;
        }

        private synchronized void deleteScanAndMayNotify(NetworkScanRequestInfo nsri, int error, boolean notify) {
            if (this.mLiveRequestInfo != null && nsri.mScanId == this.mLiveRequestInfo.mScanId) {
                if (notify) {
                    if (error == 0) {
                        NetworkScanRequestTracker.this.notifyMessenger(nsri, 3, error, null);
                    } else {
                        NetworkScanRequestTracker.this.notifyMessenger(nsri, 2, error, null);
                    }
                }
                this.mLiveRequestInfo = null;
                if (this.mPendingRequestInfo != null) {
                    startNewScan(this.mPendingRequestInfo);
                    this.mPendingRequestInfo = null;
                }
            }
        }
    }

    private void logEmptyResultOrException(AsyncResult ar) {
        if (ar.result == null) {
            Log.e(TAG, "NetworkScanResult: Empty result");
        } else {
            Log.e(TAG, "NetworkScanResult: Exception: " + ar.exception);
        }
    }

    private boolean isValidScan(NetworkScanRequestInfo nsri) {
        if (nsri.mRequest.specifiers == null || nsri.mRequest.specifiers.length > 8) {
            return false;
        }
        for (RadioAccessSpecifier ras : nsri.mRequest.specifiers) {
            if (ras.radioAccessNetwork != 1 && ras.radioAccessNetwork != 2 && ras.radioAccessNetwork != 3) {
                return false;
            }
            if (ras.bands != null && ras.bands.length > 8) {
                return false;
            }
            if (ras.channels != null && ras.channels.length > 32) {
                return false;
            }
        }
        return true;
    }

    private void notifyMessenger(NetworkScanRequestInfo nsri, int what, int err, List<CellInfo> result) {
        Messenger messenger = nsri.mMessenger;
        Message message = Message.obtain();
        message.what = what;
        message.arg1 = err;
        message.arg2 = nsri.mScanId;
        if (result != null) {
            CellInfo[] ci = (CellInfo[]) result.toArray(new CellInfo[result.size()]);
            Bundle b = new Bundle();
            b.putParcelableArray("scanResult", ci);
            message.setData(b);
        } else {
            message.obj = null;
        }
        try {
            messenger.send(message);
        } catch (RemoteException e) {
            Log.e(TAG, "Exception in notifyMessenger: " + e);
        }
    }

    private void interruptNetworkScan(int scanId) {
        this.mHandler.obtainMessage(6, scanId, 0).sendToTarget();
    }

    public int startNetworkScan(NetworkScanRequest request, Messenger messenger, IBinder binder, Phone phone) {
        int scanId = this.mNextNetworkScanRequestId.getAndIncrement();
        this.mHandler.obtainMessage(1, new NetworkScanRequestInfo(request, messenger, binder, scanId, phone)).sendToTarget();
        return scanId;
    }

    public void stopNetworkScan(int scanId) {
        synchronized (this.mScheduler) {
            if (!(this.mScheduler.mLiveRequestInfo != null && scanId == this.mScheduler.mLiveRequestInfo.mScanId && Binder.getCallingUid() == this.mScheduler.mLiveRequestInfo.mUid)) {
                if (!(this.mScheduler.mPendingRequestInfo != null && scanId == this.mScheduler.mPendingRequestInfo.mScanId && Binder.getCallingUid() == this.mScheduler.mPendingRequestInfo.mUid)) {
                    throw new IllegalArgumentException("Scan with id: " + scanId + " does not exist!");
                }
            }
            this.mHandler.obtainMessage(4, scanId, 0).sendToTarget();
        }
    }
}
