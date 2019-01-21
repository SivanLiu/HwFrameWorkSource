package com.android.internal.telephony.dataconnection;

import android.content.Context;
import android.net.NetworkCapabilities;
import android.net.NetworkFactory;
import android.net.NetworkRequest;
import android.net.StringNetworkSpecifier;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.telephony.Rlog;
import android.text.TextUtils;
import android.util.LocalLog;
import com.android.internal.telephony.HwVSimPhoneSwitcher;
import com.android.internal.util.IndentingPrintWriter;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map.Entry;

public class HwVSimTelephonyNetworkFactory extends NetworkFactory {
    private static final int EVENT_ACTIVE_PHONE_SWITCH = 3;
    private static final int EVENT_NETWORK_RELEASE = 2;
    private static final int EVENT_NETWORK_REQUEST = 1;
    private static final boolean RELEASE = false;
    private static final boolean REQUEST = true;
    private static final int REQUEST_LOG_SIZE = 40;
    private static final int TELEPHONY_NETWORK_SCORE = 50;
    public final String LOG_TAG;
    private final DcTracker mDcTracker;
    private final Handler mInternalHandler;
    private boolean mIsActive;
    private int mPhoneId;
    private final HwVSimPhoneSwitcher mPhoneSwitcher;
    private final HashMap<NetworkRequest, LocalLog> mSpecificRequests = new HashMap();

    private class InternalHandler extends Handler {
        public InternalHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    HwVSimTelephonyNetworkFactory.this.onNeedNetworkFor(msg);
                    return;
                case 2:
                    HwVSimTelephonyNetworkFactory.this.onReleaseNetworkFor(msg);
                    return;
                case 3:
                    HwVSimTelephonyNetworkFactory.this.onActivePhoneSwitch();
                    return;
                default:
                    return;
            }
        }
    }

    public HwVSimTelephonyNetworkFactory(HwVSimPhoneSwitcher phoneSwitcher, Looper looper, Context context, int phoneId, DcTracker dcTracker) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("VSimTelphonyNetFactory[");
        stringBuilder.append(phoneId);
        stringBuilder.append("]");
        super(looper, context, stringBuilder.toString(), null);
        this.mInternalHandler = new InternalHandler(looper);
        setCapabilityFilter(makeNetworkFilter(phoneId));
        setScoreFilter(50);
        this.mPhoneSwitcher = phoneSwitcher;
        this.mPhoneId = phoneId;
        stringBuilder = new StringBuilder();
        stringBuilder.append("VSimTelephonyNetFactory[");
        stringBuilder.append(phoneId);
        stringBuilder.append("]");
        this.LOG_TAG = stringBuilder.toString();
        this.mDcTracker = dcTracker;
        this.mIsActive = false;
        this.mPhoneSwitcher.registerForActivePhoneSwitch(this.mPhoneId, this.mInternalHandler, 3, null);
        register();
    }

    private NetworkCapabilities makeNetworkFilter(int subscriptionId) {
        NetworkCapabilities nc = new NetworkCapabilities();
        nc.addTransportType(0);
        nc.addCapability(0);
        nc.setNetworkSpecifier(new StringNetworkSpecifier(String.valueOf(subscriptionId)));
        return nc;
    }

    private void applyRequests(HashMap<NetworkRequest, LocalLog> requestMap, boolean action, String logStr) {
        for (Entry<NetworkRequest, LocalLog> entry : requestMap.entrySet()) {
            NetworkRequest networkRequest = (NetworkRequest) entry.getKey();
            LocalLog localLog = (LocalLog) entry.getValue();
            localLog.log(logStr);
            if (action) {
                this.mDcTracker.requestNetwork(networkRequest, localLog);
            } else {
                this.mDcTracker.releaseNetwork(networkRequest, localLog);
            }
        }
    }

    private void onActivePhoneSwitch() {
        boolean newIsActive = this.mPhoneSwitcher.isPhoneActive(this.mPhoneId);
        if (this.mIsActive != newIsActive) {
            this.mIsActive = newIsActive;
            String logString = new StringBuilder();
            logString.append("onActivePhoneSwitch(");
            logString.append(this.mIsActive);
            logString.append(")");
            logString = logString.toString();
            log(logString);
            applyRequests(this.mSpecificRequests, this.mIsActive, logString);
        }
    }

    public void needNetworkFor(NetworkRequest networkRequest, int score) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("needNetworkFor - ");
        stringBuilder.append(networkRequest);
        log(stringBuilder.toString());
        Message msg = this.mInternalHandler.obtainMessage(1);
        msg.obj = networkRequest;
        msg.sendToTarget();
    }

    private void onNeedNetworkFor(Message msg) {
        NetworkRequest networkRequest = msg.obj;
        boolean isApplicable = false;
        LocalLog localLog = null;
        StringNetworkSpecifier specifier = (StringNetworkSpecifier) networkRequest.networkCapabilities.getNetworkSpecifier();
        if (!(specifier == null || TextUtils.isEmpty(specifier.toString()))) {
            localLog = (LocalLog) this.mSpecificRequests.get(networkRequest);
            if (localLog == null) {
                localLog = new LocalLog(40);
                this.mSpecificRequests.put(networkRequest, localLog);
                isApplicable = true;
            }
        }
        if (localLog != null) {
            String s;
            StringBuilder stringBuilder;
            if (this.mIsActive && isApplicable) {
                s = "onNeedNetworkFor";
                localLog.log(s);
                stringBuilder = new StringBuilder();
                stringBuilder.append(s);
                stringBuilder.append(" ");
                stringBuilder.append(networkRequest);
                log(stringBuilder.toString());
                this.mDcTracker.requestNetwork(networkRequest, localLog);
            } else {
                s = "not acting";
                localLog.log(s);
                stringBuilder = new StringBuilder();
                stringBuilder.append(s);
                stringBuilder.append(" ");
                stringBuilder.append(networkRequest);
                log(stringBuilder.toString());
            }
        }
    }

    public void releaseNetworkFor(NetworkRequest networkRequest) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("releaseNetworkFor - ");
        stringBuilder.append(networkRequest);
        log(stringBuilder.toString());
        Message msg = this.mInternalHandler.obtainMessage(2);
        msg.obj = networkRequest;
        msg.sendToTarget();
    }

    private void onReleaseNetworkFor(Message msg) {
        NetworkRequest networkRequest = msg.obj;
        LocalLog localLog = null;
        boolean isApplicable = false;
        StringNetworkSpecifier specifier = (StringNetworkSpecifier) networkRequest.networkCapabilities.getNetworkSpecifier();
        if (!(specifier == null || TextUtils.isEmpty(specifier.toString()))) {
            localLog = (LocalLog) this.mSpecificRequests.remove(networkRequest);
            isApplicable = localLog != null;
        }
        if (localLog != null) {
            String s;
            StringBuilder stringBuilder;
            if (isApplicable) {
                s = "onReleaseNetworkFor";
                localLog.log(s);
                stringBuilder = new StringBuilder();
                stringBuilder.append(s);
                stringBuilder.append(" ");
                stringBuilder.append(networkRequest);
                log(stringBuilder.toString());
                this.mDcTracker.releaseNetwork(networkRequest, localLog);
            } else {
                s = "not releasing";
                localLog.log(s);
                stringBuilder = new StringBuilder();
                stringBuilder.append(s);
                stringBuilder.append(" ");
                stringBuilder.append(networkRequest);
                log(stringBuilder.toString());
            }
        }
    }

    protected void log(String s) {
        Rlog.d(this.LOG_TAG, s);
    }

    public void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        IndentingPrintWriter pw = new IndentingPrintWriter(writer, "  ");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.LOG_TAG);
        stringBuilder.append(" mSubId=");
        stringBuilder.append(this.mPhoneId);
        pw.println(stringBuilder.toString());
        pw.println("Specific Requests:");
    }
}
