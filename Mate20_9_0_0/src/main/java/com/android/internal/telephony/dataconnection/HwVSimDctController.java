package com.android.internal.telephony.dataconnection;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkFactory;
import android.net.NetworkRequest;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.telephony.Rlog;
import android.util.LocalLog;
import android.util.Log;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.dataconnection.HwVSimDcSwitchAsyncChannel.RequestInfo;
import com.android.internal.telephony.vsim.HwVSimLog;
import java.util.HashMap;

public class HwVSimDctController extends Handler {
    private static final int EVENT_DATA_ATTACHED = 500;
    private static final int EVENT_DATA_DETACHED = 600;
    private static final int EVENT_EXECUTE_ALL_REQUESTS = 102;
    private static final int EVENT_EXECUTE_REQUEST = 101;
    private static final int EVENT_PROCESS_REQUESTS = 100;
    private static final int EVENT_RELEASE_ALL_REQUESTS = 104;
    private static final int EVENT_RELEASE_REQUEST = 103;
    private static boolean HWDBG = false;
    private static final boolean HWLOGW_E = true;
    private static final String LOG_TAG = "VSimDctController";
    private static final Object mLock = new Object();
    private static HwVSimDctController sDctController;
    private HwVSimDcSwitchAsyncChannel mDcSwitchAsyncChannel;
    private Handler mDcSwitchStateHandler;
    private HwVSimDcSwitchStateMachine mDcSwitchStateMachine;
    private NetworkFactory mNetworkFactory;
    private Messenger mNetworkFactoryMessenger;
    private NetworkCapabilities mNetworkFilter;
    private Phone mPhone;
    private HashMap<Integer, RequestInfo> mRequestInfos = new HashMap();
    private Handler mRspHandler = new Handler() {
        public void handleMessage(Message msg) {
            int i = msg.what;
            if (i == 500) {
                HwVSimDctController.logd("EVENT_PHONE2_DATA_ATTACH.");
                HwVSimDctController.this.mDcSwitchAsyncChannel.notifyDataAttached();
            } else if (i == HwVSimDctController.EVENT_DATA_DETACHED) {
                HwVSimDctController.logd("EVENT_PHONE2_DATA_DETACH.");
                HwVSimDctController.this.mDcSwitchAsyncChannel.notifyDataDetached();
            }
        }
    };

    private class VSimNetworkFactory extends NetworkFactory {
        private static final int MAX_LOG_LINES_PER_REQUEST = 50;

        public VSimNetworkFactory(Looper l, Context c, String TAG, Phone phone, NetworkCapabilities nc) {
            super(l, c, TAG, nc);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("NetworkCapabilities: ");
            stringBuilder.append(nc);
            log(stringBuilder.toString());
        }

        protected void needNetworkFor(NetworkRequest networkRequest, int score) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Cellular needs Network for ");
            stringBuilder.append(networkRequest);
            log(stringBuilder.toString());
            DcTracker dcTracker = HwVSimDctController.this.mPhone.mDcTracker;
            String apn = HwVSimDctController.this.apnForNetworkRequest(networkRequest);
            if (dcTracker.isApnSupported(apn)) {
                HwVSimDctController.this.requestNetwork(networkRequest, dcTracker.getApnPriority(apn), new LocalLog(50));
                return;
            }
            String str = "Unsupported APN";
            log("Unsupported APN");
        }

        protected void releaseNetworkFor(NetworkRequest networkRequest) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Cellular releasing Network for ");
            stringBuilder.append(networkRequest);
            log(stringBuilder.toString());
            if (HwVSimDctController.this.mPhone.mDcTracker.isApnSupported(HwVSimDctController.this.apnForNetworkRequest(networkRequest))) {
                HwVSimDctController.this.releaseNetwork(networkRequest);
            } else {
                log("Unsupported APN");
            }
        }

        protected void log(String s) {
            if (HwVSimDctController.HWDBG) {
                String str = HwVSimDctController.LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("[TNF ");
                stringBuilder.append(HwVSimDctController.this.mPhone.getSubId());
                stringBuilder.append("]");
                stringBuilder.append(s);
                Rlog.d(str, stringBuilder.toString());
            }
        }
    }

    static {
        boolean z = Log.HWLog || (Log.HWModuleLog && Log.isLoggable(LOG_TAG, 3));
        HWDBG = z;
    }

    public static HwVSimDctController makeDctController(Phone phone, Looper looper) {
        HwVSimDctController hwVSimDctController;
        synchronized (mLock) {
            if (sDctController == null) {
                sDctController = new HwVSimDctController(phone, looper);
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("makeDctController: X sDctController=");
                stringBuilder.append(sDctController);
                logd(stringBuilder.toString());
                hwVSimDctController = sDctController;
            } else {
                throw new RuntimeException("VSimDctController already created");
            }
        }
        return hwVSimDctController;
    }

    public static HwVSimDctController getInstance() {
        HwVSimDctController hwVSimDctController;
        synchronized (mLock) {
            if (sDctController != null) {
                hwVSimDctController = sDctController;
            } else {
                throw new RuntimeException("DctController.getInstance can't be called before makeDCTController()");
            }
        }
        return hwVSimDctController;
    }

    private HwVSimDctController(Phone phone, Looper looper) {
        super(looper);
        logd("DctController()");
        this.mPhone = phone;
        Phone phone2 = this.mPhone;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("DcSwitchStateMachine-");
        stringBuilder.append(2);
        this.mDcSwitchStateMachine = new HwVSimDcSwitchStateMachine(this, phone2, stringBuilder.toString(), 2);
        this.mDcSwitchStateMachine.start();
        this.mDcSwitchAsyncChannel = new HwVSimDcSwitchAsyncChannel(this.mDcSwitchStateMachine, 2);
        this.mDcSwitchStateHandler = new Handler();
        StringBuilder stringBuilder2;
        if (this.mDcSwitchAsyncChannel.fullyConnectSync(this.mPhone.getContext(), this.mDcSwitchStateHandler, this.mDcSwitchStateMachine.getHandler()) == 0) {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("DctController: Connect success: ");
            stringBuilder2.append(2);
            logd(stringBuilder2.toString());
        } else {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("DctController: Could not connect to ");
            stringBuilder2.append(2);
            loge(stringBuilder2.toString());
        }
        this.mPhone.getServiceStateTracker().registerForDataConnectionAttached(this.mRspHandler, 500, null);
        this.mPhone.getServiceStateTracker().registerForDataConnectionDetached(this.mRspHandler, EVENT_DATA_DETACHED, null);
        ConnectivityManager cm = (ConnectivityManager) this.mPhone.getContext().getSystemService("connectivity");
        this.mNetworkFilter = new NetworkCapabilities();
        this.mNetworkFilter.addTransportType(0);
        this.mNetworkFilter.addCapability(1);
        this.mNetworkFilter.addCapability(2);
        this.mNetworkFilter.addCapability(12);
        this.mNetworkFactory = new VSimNetworkFactory(getLooper(), this.mPhone.getContext(), "VSimNetworkFactory", this.mPhone, this.mNetworkFilter);
        this.mNetworkFactory.setScoreFilter(50);
        this.mNetworkFactoryMessenger = new Messenger(this.mNetworkFactory);
        cm.registerNetworkFactory(this.mNetworkFactoryMessenger, "Telephony");
    }

    public void dispose() {
        logd("DctController.dispose");
        ((ConnectivityManager) this.mPhone.getContext().getSystemService("connectivity")).unregisterNetworkFactory(this.mNetworkFactoryMessenger);
        this.mNetworkFactoryMessenger = null;
    }

    public void handleMessage(Message msg) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("handleMessage msg=");
        stringBuilder.append(msg);
        logd(stringBuilder.toString());
        switch (msg.what) {
            case 100:
                onProcessRequest();
                return;
            case EVENT_EXECUTE_REQUEST /*101*/:
                onExecuteRequest((RequestInfo) msg.obj);
                return;
            case EVENT_EXECUTE_ALL_REQUESTS /*102*/:
                onExecuteAllRequests(msg.arg1);
                return;
            case EVENT_RELEASE_REQUEST /*103*/:
                onReleaseRequest((RequestInfo) msg.obj);
                return;
            case EVENT_RELEASE_ALL_REQUESTS /*104*/:
                onReleaseAllRequests(msg.arg1);
                return;
            default:
                stringBuilder = new StringBuilder();
                stringBuilder.append("Un-handled message [");
                stringBuilder.append(msg.what);
                stringBuilder.append("]");
                loge(stringBuilder.toString());
                return;
        }
    }

    private static void logd(String s) {
        HwVSimLog.VSimLogD(LOG_TAG, s);
    }

    private static void loge(String s) {
        HwVSimLog.VSimLogD(LOG_TAG, s);
    }

    private int requestNetwork(NetworkRequest request, int priority, LocalLog l) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("requestNetwork request=");
        stringBuilder.append(request);
        stringBuilder.append(", priority=");
        stringBuilder.append(priority);
        logd(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("Dctc.requestNetwork, priority=");
        stringBuilder.append(priority);
        l.log(stringBuilder.toString());
        this.mRequestInfos.put(Integer.valueOf(request.requestId), new RequestInfo(request, priority, l));
        processRequests();
        return 1;
    }

    private int releaseNetwork(NetworkRequest request) {
        RequestInfo requestInfo = (RequestInfo) this.mRequestInfos.get(Integer.valueOf(request.requestId));
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("releaseNetwork request=");
        stringBuilder.append(request);
        stringBuilder.append(", requestInfo=");
        stringBuilder.append(requestInfo);
        logd(stringBuilder.toString());
        if (requestInfo != null) {
            requestInfo.log("DctController.releaseNetwork");
        }
        this.mRequestInfos.remove(Integer.valueOf(request.requestId));
        releaseRequest(requestInfo);
        processRequests();
        return 1;
    }

    void processRequests() {
        logd("processRequests");
        sendMessage(obtainMessage(100));
    }

    void executeRequest(RequestInfo request) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("executeRequest, request= ");
        stringBuilder.append(request);
        logd(stringBuilder.toString());
        sendMessage(obtainMessage(EVENT_EXECUTE_REQUEST, request));
    }

    void executeAllRequests(int phoneId) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("executeAllRequests, phone:");
        stringBuilder.append(phoneId);
        logd(stringBuilder.toString());
        sendMessage(obtainMessage(EVENT_EXECUTE_ALL_REQUESTS, phoneId, 0));
    }

    void releaseRequest(RequestInfo request) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("releaseRequest, request= ");
        stringBuilder.append(request);
        logd(stringBuilder.toString());
        sendMessage(obtainMessage(EVENT_RELEASE_REQUEST, request));
    }

    void releaseAllRequests(int phoneId) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("releaseAllRequests, phone:");
        stringBuilder.append(phoneId);
        logd(stringBuilder.toString());
        sendMessage(obtainMessage(EVENT_RELEASE_ALL_REQUESTS, phoneId, 0));
    }

    private void onProcessRequest() {
        for (Object obj : this.mRequestInfos.keySet()) {
            RequestInfo requestInfo = (RequestInfo) this.mRequestInfos.get(obj);
            if (!requestInfo.executed) {
                this.mDcSwitchAsyncChannel.connect(requestInfo);
            }
        }
    }

    private void onExecuteRequest(RequestInfo requestInfo) {
        if (!requestInfo.executed && this.mRequestInfos.containsKey(Integer.valueOf(requestInfo.request.requestId))) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onExecuteRequest request=");
            stringBuilder.append(requestInfo);
            logd(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("DctController.onExecuteRequest - executed=");
            stringBuilder.append(requestInfo.executed);
            requestInfo.log(stringBuilder.toString());
            requestInfo.executed = true;
            String apn = apnForNetworkRequest(requestInfo.request);
            ApnContext apnContext = (ApnContext) this.mPhone.mDcTracker.mApnContexts.get(apn);
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("DcTracker.incApnRefCount on ");
            stringBuilder2.append(apn);
            stringBuilder2.append(" found ");
            stringBuilder2.append(apnContext);
            logd(stringBuilder2.toString());
            if (apnContext != null) {
                apnContext.requestNetwork(requestInfo.getNetworkRequest(), requestInfo.getLog());
            }
        }
    }

    private void onExecuteAllRequests(int phoneId) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onExecuteAllRequests phoneId=");
        stringBuilder.append(phoneId);
        logd(stringBuilder.toString());
        for (Object obj : this.mRequestInfos.keySet()) {
            onExecuteRequest((RequestInfo) this.mRequestInfos.get(obj));
        }
    }

    private void onReleaseRequest(RequestInfo requestInfo) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onReleaseRequest request=");
        stringBuilder.append(requestInfo);
        logd(stringBuilder.toString());
        if (requestInfo != null) {
            requestInfo.log("DctController.onReleaseRequest");
            if (requestInfo.executed) {
                String apn = apnForNetworkRequest(requestInfo.request);
                ApnContext apnContext = (ApnContext) this.mPhone.mDcTracker.mApnContexts.get(apn);
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("DcTracker.decApnRefCount on ");
                stringBuilder2.append(apn);
                stringBuilder2.append(" found ");
                stringBuilder2.append(apnContext);
                logd(stringBuilder2.toString());
                if (apnContext != null) {
                    apnContext.releaseNetwork(requestInfo.getNetworkRequest(), requestInfo.getLog());
                }
                requestInfo.executed = false;
            }
        }
    }

    private void onReleaseAllRequests(int phoneId) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onReleaseAllRequests phoneId=");
        stringBuilder.append(phoneId);
        logd(stringBuilder.toString());
        for (Object obj : this.mRequestInfos.keySet()) {
            onReleaseRequest((RequestInfo) this.mRequestInfos.get(obj));
        }
    }

    private String apnForNetworkRequest(NetworkRequest nr) {
        NetworkCapabilities nc = nr.networkCapabilities;
        if (nc.getTransportTypes().length > 0 && !nc.hasTransport(0)) {
            return null;
        }
        int type = -1;
        String name = null;
        boolean error = false;
        if (nc.hasCapability(12)) {
            name = "default";
            type = 0;
        }
        if (nc.hasCapability(1)) {
            if (name != null) {
                error = true;
            }
            name = "supl";
            type = 3;
        }
        if (nc.hasCapability(2)) {
            if (name != null) {
                error = true;
            }
            name = "dun";
            type = 4;
        }
        if (error) {
            loge("Multiple apn types specified in request - result is unspecified!");
        }
        if (type != -1 && name != null) {
            return name;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Unsupported NetworkRequest in Telephony: nr=");
        stringBuilder.append(nr);
        loge(stringBuilder.toString());
        return null;
    }
}
