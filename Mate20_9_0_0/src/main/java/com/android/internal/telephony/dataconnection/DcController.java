package com.android.internal.telephony.dataconnection;

import android.hardware.radio.V1_2.ScanIntervalRange;
import android.net.INetworkPolicyListener;
import android.net.LinkAddress;
import android.net.LinkProperties.CompareResult;
import android.net.NetworkPolicyManager;
import android.net.NetworkPolicyManager.Listener;
import android.net.NetworkUtils;
import android.os.AsyncResult;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.telephony.PhoneStateListener;
import android.telephony.Rlog;
import android.telephony.TelephonyManager;
import android.telephony.data.DataCallResponse;
import com.android.internal.telephony.DctConstants.Activity;
import com.android.internal.telephony.HwModemCapability;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneInternalInterface;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class DcController extends StateMachine {
    static final int DATA_CONNECTION_ACTIVE_PH_LINK_DORMANT = 1;
    static final int DATA_CONNECTION_ACTIVE_PH_LINK_INACTIVE = 0;
    static final int DATA_CONNECTION_ACTIVE_PH_LINK_UP = 2;
    static final int DATA_CONNECTION_ACTIVE_UNKNOWN = Integer.MAX_VALUE;
    private static final boolean DBG = true;
    private static final boolean VDBG = false;
    private final DataServiceManager mDataServiceManager;
    private final HashMap<Integer, DataConnection> mDcListActiveByCid = new HashMap();
    final ArrayList<DataConnection> mDcListAll = new ArrayList();
    private final DcTesterDeactivateAll mDcTesterDeactivateAll;
    private DccDefaultState mDccDefaultState;
    private final DcTracker mDct;
    private volatile boolean mExecutingCarrierChange;
    private final INetworkPolicyListener mListener;
    final NetworkPolicyManager mNetworkPolicyManager;
    private final Phone mPhone;
    private PhoneStateListener mPhoneStateListener;
    final TelephonyManager mTelephonyManager;

    private class DccDefaultState extends State {
        private DccDefaultState() {
        }

        /* synthetic */ DccDefaultState(DcController x0, AnonymousClass1 x1) {
            this();
        }

        public void enter() {
            if (DcController.this.mPhone != null && DcController.this.mDataServiceManager.getTransportType() == 1) {
                DcController.this.mPhone.mCi.registerForRilConnected(DcController.this.getHandler(), 262149, null);
            }
            DcController.this.mDataServiceManager.registerForDataCallListChanged(DcController.this.getHandler(), 262151);
            if (DcController.this.mNetworkPolicyManager != null) {
                DcController.this.mNetworkPolicyManager.registerListener(DcController.this.mListener);
            }
        }

        public void exit() {
            int i = 0;
            int i2 = DcController.this.mPhone != null ? 1 : 0;
            if (DcController.this.mDataServiceManager.getTransportType() == 1) {
                i = 1;
            }
            if ((i2 & i) != 0) {
                DcController.this.mPhone.mCi.unregisterForRilConnected(DcController.this.getHandler());
            }
            DcController.this.mDataServiceManager.unregisterForDataCallListChanged(DcController.this.getHandler());
            if (DcController.this.mDcTesterDeactivateAll != null) {
                DcController.this.mDcTesterDeactivateAll.dispose();
            }
            if (DcController.this.mNetworkPolicyManager != null) {
                DcController.this.mNetworkPolicyManager.unregisterListener(DcController.this.mListener);
            }
        }

        public boolean processMessage(Message msg) {
            int i = msg.what;
            AsyncResult ar;
            if (i == 262149) {
                ar = (AsyncResult) msg.obj;
                if (ar.exception == null) {
                    DcController dcController = DcController.this;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("DccDefaultState: msg.what=EVENT_RIL_CONNECTED mRilVersion=");
                    stringBuilder.append(ar.result);
                    dcController.log(stringBuilder.toString());
                } else {
                    DcController.this.log("DccDefaultState: Unexpected exception on EVENT_RIL_CONNECTED");
                }
            } else if (i == 262151) {
                ar = msg.obj;
                if (ar.exception == null) {
                    onDataStateChanged((ArrayList) ar.result);
                } else {
                    DcController.this.log("DccDefaultState: EVENT_DATA_STATE_CHANGED: exception; likely radio not available, ignore");
                }
            }
            return true;
        }

        /* JADX WARNING: Removed duplicated region for block: B:92:0x03c1  */
        /* JADX WARNING: Removed duplicated region for block: B:95:0x03ca  */
        /* JADX WARNING: Removed duplicated region for block: B:92:0x03c1  */
        /* JADX WARNING: Removed duplicated region for block: B:95:0x03ca  */
        /* JADX WARNING: Removed duplicated region for block: B:8:0x004b A:{LOOP_END, LOOP:10: B:6:0x0045->B:8:0x004b} */
        /* JADX WARNING: Removed duplicated region for block: B:12:0x0070  */
        /* JADX WARNING: Removed duplicated region for block: B:19:0x00c9  */
        /* JADX WARNING: Removed duplicated region for block: B:103:0x0411  */
        /* JADX WARNING: Removed duplicated region for block: B:107:0x0444 A:{LOOP_END, LOOP:17: B:105:0x043e->B:107:0x0444} */
        /* JADX WARNING: Removed duplicated region for block: B:111:0x045f A:{LOOP_END, LOOP:18: B:109:0x0459->B:111:0x045f} */
        /* JADX WARNING: Missing block: B:117:0x048c, code skipped:
            r0 = th;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        private void onDataStateChanged(ArrayList<DataCallResponse> dcsList) {
            ArrayList<DataConnection> dcListAll;
            HashMap<Integer, DataConnection> dcListActiveByCid;
            ArrayList<DataConnection> dcsToRetry;
            DcController dcController;
            StringBuilder stringBuilder;
            ArrayList<ApnContext> apnsToCleanup;
            boolean isAnyDataCallDormant;
            boolean isAnyDataCallActive;
            boolean isSupportRcnInd;
            Iterator it;
            HashMap<Integer, DataCallResponse> hashMap;
            HashMap<Integer, DataConnection> hashMap2;
            DcController dcController2;
            StringBuilder stringBuilder2;
            Iterator it2;
            HashMap<Integer, DataCallResponse> dataCallResponseListByCid = DcController.this.mDcListAll;
            synchronized (dataCallResponseListByCid) {
                try {
                    dcListAll = new ArrayList(DcController.this.mDcListAll);
                    dcListActiveByCid = new HashMap(DcController.this.mDcListActiveByCid);
                } finally {
                    ArrayList<DataCallResponse> arrayList = dcsList;
                    while (true) {
                    }
                    while (r4.hasNext()) {
                        break;
                    }
                    dcsToRetry = new ArrayList();
                    for (DataConnection dc : dcListActiveByCid.values()) {
                    }
                    dcController = DcController.this;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("onDataStateChanged: dcsToRetry=");
                    stringBuilder.append(dcsToRetry);
                    dcController.log(stringBuilder.toString());
                    apnsToCleanup = new ArrayList();
                    isAnyDataCallDormant = false;
                    isAnyDataCallActive = false;
                    isSupportRcnInd = HwModemCapability.isCapabilitySupport(true);
                    it = dcsList.iterator();
                    while (it.hasNext()) {
                    }
                    hashMap = dataCallResponseListByCid;
                    hashMap2 = dcListActiveByCid;
                    if (isAnyDataCallDormant) {
                    }
                    dcController2 = DcController.this;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("onDataStateChanged: Data Activity updated to NONE. isAnyDataCallActive = ");
                    stringBuilder2.append(isAnyDataCallActive);
                    stringBuilder2.append(" isAnyDataCallDormant = ");
                    stringBuilder2.append(isAnyDataCallDormant);
                    dcController2.log(stringBuilder2.toString());
                    if (isAnyDataCallActive) {
                    }
                    dcController2 = DcController.this;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("onDataStateChanged: dcsToRetry=");
                    stringBuilder2.append(dcsToRetry);
                    stringBuilder2.append(" apnsToCleanup=");
                    stringBuilder2.append(apnsToCleanup);
                    dcController2.lr(stringBuilder2.toString());
                    it2 = apnsToCleanup.iterator();
                    while (it2.hasNext()) {
                    }
                    it2 = dcsToRetry.iterator();
                    while (it2.hasNext()) {
                    }
                }
            }
            dataCallResponseListByCid = DcController.this;
            Iterator stringBuilder3 = new StringBuilder();
            stringBuilder3.append("onDataStateChanged: dcsList=");
            while (stringBuilder3.hasNext()) {
                DataCallResponse dcs = (DataCallResponse) stringBuilder3.next();
                dataCallResponseListByCid.put(Integer.valueOf(dcs.getCallId()), dcs);
                break;
            }
            dcsToRetry = new ArrayList();
            for (DataConnection dc2 : dcListActiveByCid.values()) {
                if (dataCallResponseListByCid.get(Integer.valueOf(dc2.mCid)) == null) {
                    DcController dcController3 = DcController.this;
                    StringBuilder stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("onDataStateChanged: add to retry dc=");
                    stringBuilder4.append(dc2);
                    dcController3.log(stringBuilder4.toString());
                    dcsToRetry.add(dc2);
                }
            }
            dcController = DcController.this;
            stringBuilder = new StringBuilder();
            stringBuilder.append("onDataStateChanged: dcsToRetry=");
            stringBuilder.append(dcsToRetry);
            dcController.log(stringBuilder.toString());
            apnsToCleanup = new ArrayList();
            isAnyDataCallDormant = false;
            isAnyDataCallActive = false;
            isSupportRcnInd = HwModemCapability.isCapabilitySupport(true);
            it = dcsList.iterator();
            while (it.hasNext()) {
                DataCallResponse newState = (DataCallResponse) it.next();
                DataConnection dc3 = (DataConnection) dcListActiveByCid.get(Integer.valueOf(newState.getCallId()));
                if (dc3 != null) {
                    ArrayList<DataConnection> dcListAll2;
                    if (dc3.mApnContexts.size() == 0) {
                        DcController.this.loge("onDataStateChanged: no connected apns, ignore");
                    } else {
                        DcController dcController4 = DcController.this;
                        StringBuilder stringBuilder5 = new StringBuilder();
                        stringBuilder5.append("onDataStateChanged: Found ConnId=");
                        stringBuilder5.append(newState.getCallId());
                        stringBuilder5.append(" newState=");
                        stringBuilder5.append(newState.toString());
                        dcController4.log(stringBuilder5.toString());
                        StringBuilder stringBuilder6;
                        if (newState.getActive() == 0) {
                            for (ApnContext apnContext : dc3.mApnContexts.keySet()) {
                                apnContext.setReason(PhoneInternalInterface.REASON_LOST_DATA_CONNECTION);
                            }
                            if (DcController.this.mDct.isCleanupRequired.get()) {
                                apnsToCleanup.addAll(dc3.mApnContexts.keySet());
                                DcController.this.mDct.isCleanupRequired.set(false);
                            } else {
                                DcFailCause failCause = DcFailCause.fromInt(newState.getStatus());
                                if (failCause.isRestartRadioFail(DcController.this.mPhone.getContext(), DcController.this.mPhone.getSubId())) {
                                    DcController dcController5 = DcController.this;
                                    stringBuilder6 = new StringBuilder();
                                    dcListAll2 = dcListAll;
                                    stringBuilder6.append("onDataStateChanged: X restart radio, failCause=");
                                    stringBuilder6.append(failCause);
                                    dcController5.log(stringBuilder6.toString());
                                    DcController.this.mDct.sendRestartRadio();
                                } else {
                                    dcListAll2 = dcListAll;
                                    if (DcController.this.mDct.isPermanentFailure(failCause) != null) {
                                        dcListAll = DcController.this;
                                        stringBuilder5 = new StringBuilder();
                                        stringBuilder5.append("onDataStateChanged: inactive, add to cleanup list. failCause=");
                                        stringBuilder5.append(failCause);
                                        dcListAll.log(stringBuilder5.toString());
                                        apnsToCleanup.addAll(dc3.mApnContexts.keySet());
                                    } else {
                                        dcListAll = DcController.this;
                                        stringBuilder5 = new StringBuilder();
                                        stringBuilder5.append("onDataStateChanged: inactive, add to retry list. failCause=");
                                        stringBuilder5.append(failCause);
                                        dcListAll.log(stringBuilder5.toString());
                                        if (DcController.this.mDct.needRetryAfterDisconnected(failCause) == null) {
                                            DcController.this.log("onDataStateChanged: not needRetryAfterDisconnected !");
                                            DcController.this.mDct.setRetryAfterDisconnectedReason(dc3, apnsToCleanup);
                                        } else {
                                            dcsToRetry.add(dc3);
                                        }
                                    }
                                }
                            }
                        } else {
                            dcListAll2 = dcListAll;
                            dcListAll = dc3.updateLinkProperty(newState);
                            if (dcListAll.oldLp.equals(dcListAll.newLp)) {
                                DcController.this.log("onDataStateChanged: no change");
                            } else {
                                ArrayList<DataConnection> arrayList2;
                                if (dcListAll.oldLp.isIdenticalInterfaceName(dcListAll.newLp)) {
                                    boolean z;
                                    if (!(dcListAll.oldLp.isIdenticalDnses(dcListAll.newLp) && dcListAll.oldLp.isIdenticalRoutes(dcListAll.newLp) && dcListAll.oldLp.isIdenticalHttpProxy(dcListAll.newLp) && dcListAll.oldLp.isIdenticalAddresses(dcListAll.newLp))) {
                                        if (DcController.this.mPhone.getPhoneType() != 2) {
                                            hashMap = dataCallResponseListByCid;
                                        } else if (!isSupportRcnInd) {
                                            hashMap = dataCallResponseListByCid;
                                        }
                                        CompareResult<LinkAddress> car = dcListAll.oldLp.compareAddresses(dcListAll.newLp);
                                        dcController4 = DcController.this;
                                        stringBuilder5 = new StringBuilder();
                                        stringBuilder5.append("onDataStateChanged: oldLp=");
                                        stringBuilder5.append(dcListAll.oldLp);
                                        stringBuilder5.append(" newLp=");
                                        stringBuilder5.append(dcListAll.newLp);
                                        stringBuilder5.append(" car=");
                                        stringBuilder5.append(car);
                                        dcController4.log(stringBuilder5.toString());
                                        boolean needToClean = false;
                                        for (LinkAddress added : car.added) {
                                            CompareResult<LinkAddress> car2;
                                            hashMap2 = dcListActiveByCid;
                                            Iterator it3 = car.removed.iterator();
                                            while (it3.hasNext()) {
                                                car2 = car;
                                                LinkAddress car3 = (LinkAddress) it3.next();
                                                Iterator it4 = it3;
                                                LinkAddress removed = car3;
                                                if (NetworkUtils.addressTypeMatches(car3.getAddress(), added.getAddress()) != null) {
                                                    needToClean = true;
                                                    break;
                                                } else {
                                                    car = car2;
                                                    it3 = it4;
                                                }
                                            }
                                            car2 = car;
                                            dcListActiveByCid = hashMap2;
                                            car = car2;
                                        }
                                        hashMap2 = dcListActiveByCid;
                                        if (needToClean) {
                                            DcController dcController6 = DcController.this;
                                            StringBuilder stringBuilder7 = new StringBuilder();
                                            stringBuilder7.append("onDataStateChanged: addr change, cleanup apns=");
                                            stringBuilder7.append(dc3.mApnContexts);
                                            stringBuilder7.append(" oldLp=");
                                            stringBuilder7.append(dcListAll.oldLp);
                                            stringBuilder7.append(" newLp=");
                                            stringBuilder7.append(dcListAll.newLp);
                                            dcController6.log(stringBuilder7.toString());
                                            apnsToCleanup.addAll(dc3.mApnContexts.keySet());
                                            arrayList2 = dcListAll;
                                        } else {
                                            DcController.this.log("onDataStateChanged: simple change");
                                            dataCallResponseListByCid = dc3.mApnContexts.keySet().iterator();
                                            while (dataCallResponseListByCid.hasNext()) {
                                                arrayList2 = dcListAll;
                                                DcController.this.mPhone.notifyDataConnection("linkPropertiesChanged", ((ApnContext) dataCallResponseListByCid.next()).getApnType());
                                                dcListAll = arrayList2;
                                            }
                                            arrayList2 = dcListAll;
                                        }
                                    }
                                    dcController4 = DcController.this;
                                    stringBuilder6 = new StringBuilder();
                                    stringBuilder6.append("onDataStateChanged: no changes, is CDMA Phone:");
                                    hashMap = dataCallResponseListByCid;
                                    if (DcController.this.mPhone.getPhoneType() == 2) {
                                        z = true;
                                    } else {
                                        z = false;
                                    }
                                    stringBuilder6.append(z);
                                    stringBuilder6.append(" is Supprot Reconnect ind:");
                                    stringBuilder6.append(isSupportRcnInd);
                                    dcController4.log(stringBuilder6.toString());
                                    hashMap2 = dcListActiveByCid;
                                } else {
                                    arrayList2 = dcListAll;
                                    hashMap = dataCallResponseListByCid;
                                    hashMap2 = dcListActiveByCid;
                                    apnsToCleanup.addAll(dc3.mApnContexts.keySet());
                                    dcListAll = DcController.this;
                                    stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append("onDataStateChanged: interface change, cleanup apns=");
                                    stringBuilder2.append(dc3.mApnContexts);
                                    dcListAll.log(stringBuilder2.toString());
                                }
                                if (newState.getActive() == 2) {
                                    isAnyDataCallActive = true;
                                }
                                if (newState.getActive() == 1) {
                                    isAnyDataCallDormant = true;
                                }
                                dcListAll = dcListAll2;
                                dataCallResponseListByCid = hashMap;
                                dcListActiveByCid = hashMap2;
                            }
                        }
                        hashMap = dataCallResponseListByCid;
                        hashMap2 = dcListActiveByCid;
                        if (newState.getActive() == 2) {
                        }
                        if (newState.getActive() == 1) {
                        }
                        dcListAll = dcListAll2;
                        dataCallResponseListByCid = hashMap;
                        dcListActiveByCid = hashMap2;
                    }
                    dcListAll2 = dcListAll;
                    hashMap = dataCallResponseListByCid;
                    hashMap2 = dcListActiveByCid;
                    if (newState.getActive() == 2) {
                    }
                    if (newState.getActive() == 1) {
                    }
                    dcListAll = dcListAll2;
                    dataCallResponseListByCid = hashMap;
                    dcListActiveByCid = hashMap2;
                }
            }
            hashMap = dataCallResponseListByCid;
            hashMap2 = dcListActiveByCid;
            if (isAnyDataCallDormant || isAnyDataCallActive) {
                dcController2 = DcController.this;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("onDataStateChanged: Data Activity updated to NONE. isAnyDataCallActive = ");
                stringBuilder2.append(isAnyDataCallActive);
                stringBuilder2.append(" isAnyDataCallDormant = ");
                stringBuilder2.append(isAnyDataCallDormant);
                dcController2.log(stringBuilder2.toString());
                if (isAnyDataCallActive) {
                    DcController.this.mDct.sendStartNetStatPoll(Activity.NONE);
                }
            } else {
                DcController.this.log("onDataStateChanged: Data Activity updated to DORMANT. stopNetStatePoll");
                DcController.this.mDct.sendStopNetStatPoll(Activity.DORMANT);
            }
            dcController2 = DcController.this;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("onDataStateChanged: dcsToRetry=");
            stringBuilder2.append(dcsToRetry);
            stringBuilder2.append(" apnsToCleanup=");
            stringBuilder2.append(apnsToCleanup);
            dcController2.lr(stringBuilder2.toString());
            it2 = apnsToCleanup.iterator();
            while (it2.hasNext()) {
                DcController.this.mDct.sendCleanUpConnection(true, (ApnContext) it2.next());
            }
            it2 = dcsToRetry.iterator();
            while (it2.hasNext()) {
                DataConnection dc4 = (DataConnection) it2.next();
                DcController dcController7 = DcController.this;
                StringBuilder stringBuilder8 = new StringBuilder();
                stringBuilder8.append("onDataStateChanged: send EVENT_LOST_CONNECTION dc.mTag=");
                stringBuilder8.append(dc4.mTag);
                dcController7.log(stringBuilder8.toString());
                dc4.sendMessage(262153, dc4.mTag);
            }
        }
    }

    private DcController(String name, Phone phone, DcTracker dct, DataServiceManager dataServiceManager, Handler handler) {
        super(name, handler);
        DcTesterDeactivateAll dcTesterDeactivateAll = null;
        this.mDccDefaultState = new DccDefaultState(this, null);
        this.mListener = new Listener() {
            public void onSubscriptionOverride(int subId, int overrideMask, int overrideValue) {
                if (DcController.this.mPhone != null && DcController.this.mPhone.getSubId() == subId) {
                    synchronized (DcController.this.mDcListAll) {
                        HashMap<Integer, DataConnection> dcListActiveByCid = new HashMap(DcController.this.mDcListActiveByCid);
                    }
                    for (DataConnection dc : dcListActiveByCid.values()) {
                        dc.onSubscriptionOverride(overrideMask, overrideValue);
                    }
                }
            }
        };
        setLogRecSize(ScanIntervalRange.MAX);
        log("E ctor");
        this.mPhone = phone;
        this.mDct = dct;
        this.mDataServiceManager = dataServiceManager;
        addState(this.mDccDefaultState);
        setInitialState(this.mDccDefaultState);
        log("X ctor");
        this.mPhoneStateListener = new PhoneStateListener(handler.getLooper()) {
            public void onCarrierNetworkChange(boolean active) {
                DcController.this.mExecutingCarrierChange = active;
            }
        };
        this.mTelephonyManager = (TelephonyManager) phone.getContext().getSystemService("phone");
        this.mNetworkPolicyManager = (NetworkPolicyManager) phone.getContext().getSystemService("netpolicy");
        if (Build.IS_DEBUGGABLE) {
            dcTesterDeactivateAll = new DcTesterDeactivateAll(this.mPhone, this, getHandler());
        }
        this.mDcTesterDeactivateAll = dcTesterDeactivateAll;
        if (this.mTelephonyManager != null) {
            this.mTelephonyManager.listen(this.mPhoneStateListener, 65536);
        }
    }

    public static DcController makeDcc(Phone phone, DcTracker dct, DataServiceManager dataServiceManager, Handler handler) {
        return new DcController("Dcc", phone, dct, dataServiceManager, handler);
    }

    void dispose() {
        log("dispose: call quiteNow()");
        if (this.mTelephonyManager != null) {
            this.mTelephonyManager.listen(this.mPhoneStateListener, 0);
        }
        quitNow();
    }

    void addDc(DataConnection dc) {
        synchronized (this.mDcListAll) {
            this.mDcListAll.add(dc);
        }
    }

    void removeDc(DataConnection dc) {
        synchronized (this.mDcListAll) {
            this.mDcListActiveByCid.remove(Integer.valueOf(dc.mCid));
            this.mDcListAll.remove(dc);
        }
    }

    public void addActiveDcByCid(DataConnection dc) {
        if (dc.mCid < 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("addActiveDcByCid dc.mCid < 0 dc=");
            stringBuilder.append(dc);
            log(stringBuilder.toString());
        }
        synchronized (this.mDcListAll) {
            this.mDcListActiveByCid.put(Integer.valueOf(dc.mCid), dc);
        }
    }

    public DataConnection getActiveDcByCid(int cid) {
        DataConnection dataConnection;
        synchronized (this.mDcListAll) {
            dataConnection = (DataConnection) this.mDcListActiveByCid.get(Integer.valueOf(cid));
        }
        return dataConnection;
    }

    void removeActiveDcByCid(DataConnection dc) {
        synchronized (this.mDcListAll) {
            if (((DataConnection) this.mDcListActiveByCid.remove(Integer.valueOf(dc.mCid))) == null) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("removeActiveDcByCid removedDc=null dc=");
                stringBuilder.append(dc);
                log(stringBuilder.toString());
            }
        }
    }

    boolean isExecutingCarrierChange() {
        return this.mExecutingCarrierChange;
    }

    void getDataCallList() {
        log("DcController:getDataCallList");
        this.mPhone.mCi.getDataCallList(obtainMessage(262151));
    }

    private void lr(String s) {
        logAndAddLogRec(s);
    }

    protected void log(String s) {
        Rlog.d(getName(), s);
    }

    protected void loge(String s) {
        Rlog.e(getName(), s);
    }

    protected String getWhatToString(int what) {
        String info = DataConnection.cmdToString(what);
        if (info == null) {
            return DcAsyncChannel.cmdToString(what);
        }
        return info;
    }

    public String toString() {
        String stringBuilder;
        synchronized (this.mDcListAll) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("mDcListAll=");
            stringBuilder2.append(this.mDcListAll);
            stringBuilder2.append(" mDcListActiveByCid=");
            stringBuilder2.append(this.mDcListActiveByCid);
            stringBuilder = stringBuilder2.toString();
        }
        return stringBuilder;
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        super.dump(fd, pw, args);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(" mPhone=");
        stringBuilder.append(this.mPhone);
        pw.println(stringBuilder.toString());
        synchronized (this.mDcListAll) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(" mDcListAll=");
            stringBuilder2.append(this.mDcListAll);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(" mDcListActiveByCid=");
            stringBuilder2.append(this.mDcListActiveByCid);
            pw.println(stringBuilder2.toString());
        }
    }
}
