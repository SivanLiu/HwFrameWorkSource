package com.android.internal.telephony.dataconnection;

import android.app.PendingIntent;
import android.net.NetworkCapabilities;
import android.net.NetworkConfig;
import android.net.NetworkRequest;
import android.telephony.Rlog;
import android.text.TextUtils;
import android.util.LocalLog;
import android.util.SparseIntArray;
import com.android.internal.telephony.DctConstants.State;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneInternalInterface;
import com.android.internal.telephony.RetryManager;
import com.android.internal.util.IndentingPrintWriter;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ApnContext {
    protected static final boolean DBG = true;
    private static final String SLOG_TAG = "ApnContext";
    public final String LOG_TAG;
    private ApnSetting mApnSetting;
    private final String mApnType;
    private boolean mConcurrentVoiceAndDataAllowed;
    private final AtomicInteger mConnectionGeneration = new AtomicInteger(0);
    AtomicBoolean mDataEnabled;
    DcAsyncChannel mDcAc;
    private final DcTracker mDcTracker;
    AtomicBoolean mDependencyMet;
    private final ArrayList<LocalLog> mLocalLogs = new ArrayList();
    private final ArrayList<NetworkRequest> mNetworkRequests = new ArrayList();
    private final Phone mPhone;
    String mReason;
    PendingIntent mReconnectAlarmIntent;
    private final Object mRefCountLock = new Object();
    private final SparseIntArray mRetriesLeftPerErrorCode = new SparseIntArray();
    private final RetryManager mRetryManager;
    private State mState;
    private final LocalLog mStateLocalLog = new LocalLog(50);
    DcFailCause pdpFailCause = DcFailCause.NONE;
    public final int priority;

    public ApnContext(Phone phone, String apnType, String logTag, NetworkConfig config, DcTracker tracker) {
        this.mPhone = phone;
        this.mApnType = apnType;
        this.mState = State.IDLE;
        setReason(PhoneInternalInterface.REASON_DATA_ENABLED);
        this.mDataEnabled = new AtomicBoolean(false);
        this.mDependencyMet = new AtomicBoolean(config.dependencyMet);
        this.priority = config.priority;
        this.LOG_TAG = logTag;
        this.mDcTracker = tracker;
        this.mRetryManager = new RetryManager(phone, apnType);
    }

    public String getApnType() {
        return this.mApnType;
    }

    public synchronized DcAsyncChannel getDcAc() {
        return this.mDcAc;
    }

    public synchronized void setDataConnectionAc(DcAsyncChannel dcac) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setDataConnectionAc: old dcac=");
        stringBuilder.append(this.mDcAc);
        stringBuilder.append(" new dcac=");
        stringBuilder.append(dcac);
        stringBuilder.append(" this=");
        stringBuilder.append(this);
        log(stringBuilder.toString());
        this.mDcAc = dcac;
    }

    public synchronized void releaseDataConnection(String reason) {
        if (this.mDcAc != null) {
            this.mDcAc.tearDown(this, reason, null);
            this.mDcAc = null;
        }
        setState(State.IDLE);
    }

    public synchronized PendingIntent getReconnectIntent() {
        return this.mReconnectAlarmIntent;
    }

    public synchronized void setReconnectIntent(PendingIntent intent) {
        this.mReconnectAlarmIntent = intent;
    }

    public synchronized ApnSetting getApnSetting() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getApnSetting: apnSetting=");
        stringBuilder.append(this.mApnSetting);
        log(stringBuilder.toString());
        return this.mApnSetting;
    }

    public synchronized void setApnSetting(ApnSetting apnSetting) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setApnSetting: apnSetting=");
        stringBuilder.append(apnSetting);
        log(stringBuilder.toString());
        this.mApnSetting = apnSetting;
    }

    public synchronized void setWaitingApns(ArrayList<ApnSetting> waitingApns) {
        this.mRetryManager.setWaitingApns(waitingApns);
    }

    public ApnSetting getNextApnSetting() {
        return this.mRetryManager.getNextApnSetting();
    }

    public void setModemSuggestedDelay(long delay) {
        this.mRetryManager.setModemSuggestedDelay(delay);
    }

    public long getDelayForNextApn(boolean failFastEnabled) {
        RetryManager retryManager = this.mRetryManager;
        boolean z = failFastEnabled || isFastRetryReason();
        return retryManager.getDelayForNextApn(z);
    }

    public void markApnPermanentFailed(ApnSetting apn) {
        this.mRetryManager.markApnPermanentFailed(apn);
    }

    public ArrayList<ApnSetting> getWaitingApns() {
        return this.mRetryManager.getWaitingApns();
    }

    public synchronized void setConcurrentVoiceAndDataAllowed(boolean allowed) {
        this.mConcurrentVoiceAndDataAllowed = allowed;
    }

    public synchronized boolean isConcurrentVoiceAndDataAllowed() {
        return this.mConcurrentVoiceAndDataAllowed;
    }

    public synchronized void setState(State s) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setState: ");
        stringBuilder.append(s);
        stringBuilder.append(", previous state:");
        stringBuilder.append(this.mState);
        log(stringBuilder.toString());
        if (this.mState != s) {
            LocalLog localLog = this.mStateLocalLog;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("State changed from ");
            stringBuilder2.append(this.mState);
            stringBuilder2.append(" to ");
            stringBuilder2.append(s);
            localLog.log(stringBuilder2.toString());
            this.mState = s;
        }
        if (this.mState == State.FAILED && this.mRetryManager.getWaitingApns() != null) {
            this.mRetryManager.getWaitingApns().clear();
        }
    }

    public synchronized State getState() {
        return this.mState;
    }

    public boolean isDisconnected() {
        State currentState = getState();
        return currentState == State.IDLE || currentState == State.FAILED;
    }

    public synchronized void setReason(String reason) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("set reason as ");
        stringBuilder.append(reason);
        stringBuilder.append(",current state ");
        stringBuilder.append(this.mState);
        log(stringBuilder.toString());
        this.mReason = reason;
    }

    public synchronized String getReason() {
        return this.mReason;
    }

    public boolean isReady() {
        return this.mDataEnabled.get() && this.mDependencyMet.get();
    }

    public synchronized void setPdpFailCause(DcFailCause pdpFailReason) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("pdpFailCause is ");
        stringBuilder.append(pdpFailReason);
        log(stringBuilder.toString());
        this.pdpFailCause = pdpFailReason;
    }

    public synchronized DcFailCause getPdpFailCause() {
        return this.pdpFailCause;
    }

    public boolean isConnectable() {
        return isReady() && (this.mState == State.IDLE || this.mState == State.SCANNING || this.mState == State.RETRYING || this.mState == State.FAILED);
    }

    private boolean isFastRetryReason() {
        return PhoneInternalInterface.REASON_NW_TYPE_CHANGED.equals(this.mReason) || PhoneInternalInterface.REASON_APN_CHANGED.equals(this.mReason);
    }

    public boolean isConnectedOrConnecting() {
        return isReady() && (this.mState == State.CONNECTED || this.mState == State.CONNECTING || this.mState == State.SCANNING || this.mState == State.RETRYING);
    }

    public void setEnabled(boolean enabled) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("set enabled as ");
        stringBuilder.append(enabled);
        stringBuilder.append(", current state is ");
        stringBuilder.append(this.mDataEnabled.get());
        log(stringBuilder.toString());
        this.mDataEnabled.set(enabled);
    }

    public boolean isEnabled() {
        return this.mDataEnabled.get();
    }

    public void setDependencyMet(boolean met) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("set mDependencyMet as ");
        stringBuilder.append(met);
        stringBuilder.append(" current state is ");
        stringBuilder.append(this.mDependencyMet.get());
        log(stringBuilder.toString());
        this.mDependencyMet.set(met);
    }

    public boolean getDependencyMet() {
        return this.mDependencyMet.get();
    }

    public boolean isProvisioningApn() {
        String provisioningApn = this.mPhone.getContext().getResources().getString(17040534);
        if (TextUtils.isEmpty(provisioningApn) || this.mApnSetting == null || this.mApnSetting.apn == null) {
            return false;
        }
        return this.mApnSetting.apn.equals(provisioningApn);
    }

    public void requestLog(String str) {
        synchronized (this.mRefCountLock) {
            Iterator it = this.mLocalLogs.iterator();
            while (it.hasNext()) {
                ((LocalLog) it.next()).log(str);
            }
        }
    }

    public void requestNetwork(NetworkRequest networkRequest, LocalLog log) {
        synchronized (this.mRefCountLock) {
            StringBuilder stringBuilder;
            if (!this.mLocalLogs.contains(log)) {
                if (!this.mNetworkRequests.contains(networkRequest)) {
                    this.mLocalLogs.add(log);
                    this.mNetworkRequests.add(networkRequest);
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("add new request: ");
                    stringBuilder.append(networkRequest);
                    log(stringBuilder.toString());
                    this.mDcTracker.setEnabled(apnIdForApnName(this.mApnType), true);
                }
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("ApnContext.requestNetwork has duplicate add - ");
            stringBuilder.append(this.mNetworkRequests.size());
            log.log(stringBuilder.toString());
        }
    }

    public void releaseNetwork(NetworkRequest networkRequest, LocalLog log) {
        synchronized (this.mRefCountLock) {
            if (this.mLocalLogs.contains(log)) {
                this.mLocalLogs.remove(log);
            } else {
                log.log("ApnContext.releaseNetwork can't find this log");
            }
            StringBuilder stringBuilder;
            if (this.mNetworkRequests.contains(networkRequest)) {
                this.mNetworkRequests.remove(networkRequest);
                stringBuilder = new StringBuilder();
                stringBuilder.append("ApnContext.releaseNetwork left with ");
                stringBuilder.append(this.mNetworkRequests.size());
                stringBuilder.append(" requests.");
                log.log(stringBuilder.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append("release request: ");
                stringBuilder.append(networkRequest);
                log(stringBuilder.toString());
                if (this.mNetworkRequests.size() > 0) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("releaseNetwork left with ");
                    stringBuilder.append(this.mNetworkRequests.size());
                    stringBuilder.append(" requests, first is ");
                    stringBuilder.append(((NetworkRequest) this.mNetworkRequests.get(0)).toString());
                    log(stringBuilder.toString());
                }
                if (this.mNetworkRequests.size() == 0) {
                    this.mDcTracker.setEnabled(apnIdForApnName(this.mApnType), false);
                }
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("ApnContext.releaseNetwork can't find this request (");
                stringBuilder.append(networkRequest);
                stringBuilder.append(")");
                log.log(stringBuilder.toString());
            }
        }
    }

    public List<NetworkRequest> getNetworkRequests() {
        ArrayList arrayList;
        synchronized (this.mRefCountLock) {
            arrayList = new ArrayList(this.mNetworkRequests);
        }
        return arrayList;
    }

    public boolean hasNoRestrictedRequests(boolean excludeDun) {
        synchronized (this.mRefCountLock) {
            Iterator it = this.mNetworkRequests.iterator();
            while (it.hasNext()) {
                NetworkRequest nr = (NetworkRequest) it.next();
                if (!excludeDun || !nr.networkCapabilities.hasCapability(2)) {
                    if (!nr.networkCapabilities.hasCapability(13)) {
                        return false;
                    }
                }
            }
            return true;
        }
    }

    public void resetErrorCodeRetries() {
        requestLog("ApnContext.resetErrorCodeRetries");
        log("ApnContext.resetErrorCodeRetries");
        String[] config = this.mPhone.getContext().getResources().getStringArray(17235995);
        synchronized (this.mRetriesLeftPerErrorCode) {
            this.mRetriesLeftPerErrorCode.clear();
            for (String c : config) {
                String[] errorValue = c.split(",");
                if (errorValue == null || errorValue.length != 2) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Exception parsing config_retries_per_error_code: ");
                    stringBuilder.append(c);
                    log(stringBuilder.toString());
                } else {
                    int errorCode = 0;
                    try {
                        errorCode = Integer.parseInt(errorValue[0]);
                        int count = Integer.parseInt(errorValue[1]);
                        if (count > 0 && errorCode > 0) {
                            this.mRetriesLeftPerErrorCode.put(errorCode, count);
                        }
                    } catch (NumberFormatException e) {
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Exception parsing config_retries_per_error_code: ");
                        stringBuilder2.append(e);
                        log(stringBuilder2.toString());
                    }
                }
            }
        }
    }

    public boolean restartOnError(int errorCode) {
        int retriesLeft;
        boolean result = false;
        synchronized (this.mRetriesLeftPerErrorCode) {
            retriesLeft = this.mRetriesLeftPerErrorCode.get(errorCode);
            switch (retriesLeft) {
                case 0:
                    break;
                case 1:
                    resetErrorCodeRetries();
                    result = true;
                    break;
                default:
                    this.mRetriesLeftPerErrorCode.put(errorCode, retriesLeft - 1);
                    result = false;
                    break;
            }
        }
        String str = new StringBuilder();
        str.append("ApnContext.restartOnError(");
        str.append(errorCode);
        str.append(") found ");
        str.append(retriesLeft);
        str.append(" and returned ");
        str.append(result);
        str = str.toString();
        log(str);
        requestLog(str);
        return result;
    }

    public int incAndGetConnectionGeneration() {
        return this.mConnectionGeneration.incrementAndGet();
    }

    public int getConnectionGeneration() {
        return this.mConnectionGeneration.get();
    }

    long getRetryAfterDisconnectDelay() {
        return this.mRetryManager.getRetryAfterDisconnectDelay();
    }

    public static int apnIdForType(int networkType) {
        if (networkType == 0) {
            return 0;
        }
        if (networkType == 45) {
            return 19;
        }
        switch (networkType) {
            case 2:
                return 1;
            case 3:
                return 2;
            case 4:
                return 3;
            default:
                switch (networkType) {
                    case 10:
                        return 6;
                    case 11:
                        return 5;
                    case 12:
                        return 7;
                    default:
                        switch (networkType) {
                            case 14:
                                return 8;
                            case 15:
                                return 9;
                            default:
                                return -1;
                        }
                }
        }
    }

    public static int apnIdForNetworkRequest(NetworkRequest nr) {
        NetworkCapabilities nc = nr.networkCapabilities;
        if (nc.getTransportTypes().length > 0 && !nc.hasTransport(0)) {
            return -1;
        }
        int apnId = -1;
        boolean z = false;
        if (nc.hasCapability(12)) {
            apnId = 0;
        }
        if (nc.hasCapability(0)) {
            if (apnId != -1) {
                z = true;
            }
            apnId = 1;
        }
        if (nc.hasCapability(1)) {
            if (apnId != -1) {
                z = true;
            }
            apnId = 2;
        }
        if (nc.hasCapability(2)) {
            if (apnId != -1) {
                z = true;
            }
            apnId = 3;
        }
        if (nc.hasCapability(3)) {
            if (apnId != -1) {
                z = true;
            }
            apnId = 6;
        }
        if (nc.hasCapability(4)) {
            if (apnId != -1) {
                z = true;
            }
            apnId = 5;
        }
        if (nc.hasCapability(5)) {
            if (apnId != -1) {
                z = true;
            }
            apnId = 7;
        }
        if (nc.hasCapability(7)) {
            if (apnId != -1) {
                z = true;
            }
            apnId = 8;
        }
        if (nc.hasCapability(8)) {
            if (apnId != -1) {
                z = true;
            }
            Rlog.d(SLOG_TAG, "RCS APN type not yet supported");
        }
        if (nc.hasCapability(9)) {
            if (apnId != -1) {
                z = true;
            }
            apnId = 19;
        }
        if (nc.hasCapability(10)) {
            if (apnId != -1) {
                z = true;
            }
            apnId = 9;
        }
        if (nc.hasCapability(23)) {
            if (apnId != -1) {
                z = true;
            }
            apnId = 12;
        }
        if (nc.hasCapability(24)) {
            if (apnId != -1) {
                z = true;
            }
            apnId = 13;
        }
        if (nc.hasCapability(25)) {
            if (apnId != -1) {
                z = true;
            }
            apnId = 14;
        }
        if (nc.hasCapability(26)) {
            if (apnId != -1) {
                z = true;
            }
            apnId = 15;
        }
        if (nc.hasCapability(27)) {
            if (apnId != -1) {
                z = true;
            }
            apnId = 16;
        }
        if (nc.hasCapability(28)) {
            if (apnId != -1) {
                z = true;
            }
            apnId = 17;
        }
        if (nc.hasCapability(29)) {
            if (apnId != -1) {
                z = true;
            }
            apnId = 18;
        }
        if (nc.hasCapability(30)) {
            if (apnId != -1) {
                z = true;
            }
            apnId = 20;
        }
        if (z) {
            Rlog.d(SLOG_TAG, "Multiple apn types specified in request - result is unspecified!");
        }
        if (apnId == -1) {
            String str = SLOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unsupported NetworkRequest in Telephony: nr=");
            stringBuilder.append(nr);
            Rlog.d(str, stringBuilder.toString());
        }
        return apnId;
    }

    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static int apnIdForApnName(String type) {
        String str = type;
        int hashCode = type.hashCode();
        switch (hashCode) {
            case 3023943:
                if (str.equals("bip0")) {
                    hashCode = 11;
                    break;
                }
            case 3023944:
                if (str.equals("bip1")) {
                    hashCode = 12;
                    break;
                }
            case 3023945:
                if (str.equals("bip2")) {
                    hashCode = 13;
                    break;
                }
            case 3023946:
                if (str.equals("bip3")) {
                    hashCode = 14;
                    break;
                }
            case 3023947:
                if (str.equals("bip4")) {
                    hashCode = 15;
                    break;
                }
            case 3023948:
                if (str.equals("bip5")) {
                    hashCode = 16;
                    break;
                }
            case 3023949:
                if (str.equals("bip6")) {
                    hashCode = 17;
                    break;
                }
            default:
                switch (hashCode) {
                    case -1490587420:
                        if (str.equals("internaldefault")) {
                            hashCode = 18;
                            break;
                        }
                    case 3352:
                        if (str.equals("ia")) {
                            hashCode = 8;
                            break;
                        }
                    case 98292:
                        if (str.equals("cbs")) {
                            hashCode = 7;
                            break;
                        }
                    case 99837:
                        if (str.equals("dun")) {
                            hashCode = 3;
                            break;
                        }
                    case 104399:
                        if (str.equals("ims")) {
                            hashCode = 5;
                            break;
                        }
                    case 108243:
                        if (str.equals("mms")) {
                            hashCode = 1;
                            break;
                        }
                    case 3149046:
                        if (str.equals("fota")) {
                            hashCode = 6;
                            break;
                        }
                    case 3541982:
                        if (str.equals("supl")) {
                            hashCode = 2;
                            break;
                        }
                    case 3673178:
                        if (str.equals("xcap")) {
                            hashCode = 10;
                            break;
                        }
                    case 99285510:
                        if (str.equals("hipri")) {
                            hashCode = 4;
                            break;
                        }
                    case 1544803905:
                        if (str.equals("default")) {
                            hashCode = 0;
                            break;
                        }
                    case 1629013393:
                        if (str.equals("emergency")) {
                            hashCode = 9;
                            break;
                        }
                }
                hashCode = -1;
                break;
        }
        switch (hashCode) {
            case 0:
                return 0;
            case 1:
                return 1;
            case 2:
                return 2;
            case 3:
                return 3;
            case 4:
                return 4;
            case 5:
                return 5;
            case 6:
                return 6;
            case 7:
                return 7;
            case 8:
                return 8;
            case 9:
                return 9;
            case 10:
                return 19;
            case 11:
                return 12;
            case 12:
                return 13;
            case 13:
                return 14;
            case 14:
                return 15;
            case 15:
                return 16;
            case 16:
                return 17;
            case 17:
                return 18;
            case 18:
                return 20;
            default:
                return -1;
        }
    }

    private static String apnNameForApnId(int id) {
        switch (id) {
            case 0:
                return "default";
            case 1:
                return "mms";
            case 2:
                return "supl";
            case 3:
                return "dun";
            case 4:
                return "hipri";
            case 5:
                return "ims";
            case 6:
                return "fota";
            case 7:
                return "cbs";
            case 8:
                return "ia";
            case 9:
                return "emergency";
            case 12:
                return "bip0";
            case 13:
                return "bip1";
            case 14:
                return "bip2";
            case 15:
                return "bip3";
            case 16:
                return "bip4";
            case 17:
                return "bip5";
            case 18:
                return "bip6";
            case 19:
                return "xcap";
            case 20:
                return "internaldefault";
            default:
                String str = SLOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unknown id (");
                stringBuilder.append(id);
                stringBuilder.append(") in apnIdToType");
                Rlog.d(str, stringBuilder.toString());
                return "default";
        }
    }

    public synchronized String toString() {
        StringBuilder stringBuilder;
        stringBuilder = new StringBuilder();
        stringBuilder.append("{mApnType=");
        stringBuilder.append(this.mApnType);
        stringBuilder.append(" mState=");
        stringBuilder.append(getState());
        stringBuilder.append(" mWaitingApns={");
        stringBuilder.append(this.mRetryManager.getWaitingApns());
        stringBuilder.append("} mApnSetting={");
        stringBuilder.append(this.mApnSetting);
        stringBuilder.append("} mReason=");
        stringBuilder.append(this.mReason);
        stringBuilder.append(" mDataEnabled=");
        stringBuilder.append(this.mDataEnabled);
        stringBuilder.append(" mDependencyMet=");
        stringBuilder.append(this.mDependencyMet);
        stringBuilder.append("}");
        return stringBuilder.toString();
    }

    private void log(String s) {
        String str = this.LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[");
        stringBuilder.append(this.mPhone != null ? this.mPhone.getPhoneId() : -1);
        stringBuilder.append("][ApnContext:");
        stringBuilder.append(this.mApnType);
        stringBuilder.append("] ");
        stringBuilder.append(s);
        Rlog.d(str, stringBuilder.toString());
    }

    public void dump(FileDescriptor fd, PrintWriter printWriter, String[] args) {
        IndentingPrintWriter pw = new IndentingPrintWriter(printWriter, "  ");
        synchronized (this.mRefCountLock) {
            Iterator it;
            pw.println(toString());
            if (this.mNetworkRequests.size() > 0) {
                pw.println("NetworkRequests:");
                pw.increaseIndent();
                it = this.mNetworkRequests.iterator();
                while (it.hasNext()) {
                    pw.println((NetworkRequest) it.next());
                }
                pw.decreaseIndent();
            }
            pw.increaseIndent();
            it = this.mLocalLogs.iterator();
            while (it.hasNext()) {
                ((LocalLog) it.next()).dump(fd, pw, args);
                pw.println("-----");
            }
            pw.decreaseIndent();
            pw.println("Historical APN state:");
            pw.increaseIndent();
            this.mStateLocalLog.dump(fd, pw, args);
            pw.decreaseIndent();
            pw.println(this.mRetryManager);
            pw.println("--------------------------");
        }
    }

    public boolean isLastApnSetting() {
        return this.mRetryManager.isLastApnSetting();
    }

    public void resetRetryCount() {
        this.mRetryManager.resetRetryCount();
    }
}
