package com.android.internal.telephony;

import android.content.Context;
import android.hardware.radio.V1_0.RadioResponseInfo;
import android.hardware.radio.config.V1_0.IRadioConfig;
import android.hardware.radio.config.V1_0.SimSlotStatus;
import android.net.ConnectivityManager;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.IHwBinder.DeathRecipient;
import android.os.Message;
import android.os.Registrant;
import android.os.RemoteException;
import android.os.WorkSource;
import android.telephony.Rlog;
import android.util.SparseArray;
import com.android.internal.telephony.uicc.IccSlotStatus;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLong;

public class RadioConfig extends Handler {
    private static final boolean DBG = true;
    private static final int EVENT_SERVICE_DEAD = 1;
    private static final String TAG = "RadioConfig";
    private static final boolean VDBG = false;
    private static RadioConfig sRadioConfig;
    private final WorkSource mDefaultWorkSource;
    private final boolean mIsMobileNetworkSupported;
    private final RadioConfigIndication mRadioConfigIndication;
    private volatile IRadioConfig mRadioConfigProxy = null;
    private final AtomicLong mRadioConfigProxyCookie = new AtomicLong(0);
    private final RadioConfigResponse mRadioConfigResponse;
    private final SparseArray<RILRequest> mRequestList = new SparseArray();
    private final ServiceDeathRecipient mServiceDeathRecipient;
    protected Registrant mSimSlotStatusRegistrant;

    final class ServiceDeathRecipient implements DeathRecipient {
        ServiceDeathRecipient() {
        }

        public void serviceDied(long cookie) {
            RadioConfig.logd("serviceDied");
            RadioConfig.this.sendMessage(RadioConfig.this.obtainMessage(1, Long.valueOf(cookie)));
        }
    }

    private RadioConfig(Context context) {
        this.mIsMobileNetworkSupported = ((ConnectivityManager) context.getSystemService("connectivity")).isNetworkSupported(0);
        this.mRadioConfigResponse = new RadioConfigResponse(this);
        this.mRadioConfigIndication = new RadioConfigIndication(this);
        this.mServiceDeathRecipient = new ServiceDeathRecipient();
        this.mDefaultWorkSource = new WorkSource(context.getApplicationInfo().uid, context.getPackageName());
    }

    public static RadioConfig getInstance(Context context) {
        if (sRadioConfig == null) {
            sRadioConfig = new RadioConfig(context);
        }
        return sRadioConfig;
    }

    public void handleMessage(Message message) {
        if (message.what == 1) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("handleMessage: EVENT_SERVICE_DEAD cookie = ");
            stringBuilder.append(message.obj);
            stringBuilder.append(" mRadioConfigProxyCookie = ");
            stringBuilder.append(this.mRadioConfigProxyCookie.get());
            logd(stringBuilder.toString());
            if (((Long) message.obj).longValue() == this.mRadioConfigProxyCookie.get()) {
                resetProxyAndRequestList("EVENT_SERVICE_DEAD", null);
            }
        }
    }

    private void clearRequestList(int error, boolean loggable) {
        synchronized (this.mRequestList) {
            int count = this.mRequestList.size();
            if (loggable) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("clearRequestList: mRequestList=");
                stringBuilder.append(count);
                logd(stringBuilder.toString());
            }
            for (int i = 0; i < count; i++) {
                RILRequest rr = (RILRequest) this.mRequestList.valueAt(i);
                if (loggable) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(i);
                    stringBuilder2.append(": [");
                    stringBuilder2.append(rr.mSerial);
                    stringBuilder2.append("] ");
                    stringBuilder2.append(requestToString(rr.mRequest));
                    logd(stringBuilder2.toString());
                }
                rr.onError(error, null);
                rr.release();
            }
            this.mRequestList.clear();
        }
    }

    private void resetProxyAndRequestList(String caller, Exception e) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(caller);
        stringBuilder.append(": ");
        stringBuilder.append(e);
        loge(stringBuilder.toString());
        this.mRadioConfigProxy = null;
        this.mRadioConfigProxyCookie.incrementAndGet();
        RILRequest.resetSerial();
        clearRequestList(1, false);
        getRadioConfigProxy(null);
    }

    public IRadioConfig getRadioConfigProxy(Message result) {
        if (!this.mIsMobileNetworkSupported) {
            if (result != null) {
                AsyncResult.forMessage(result, null, CommandException.fromRilErrno(1));
                result.sendToTarget();
            }
            return null;
        } else if (this.mRadioConfigProxy != null) {
            return this.mRadioConfigProxy;
        } else {
            try {
                this.mRadioConfigProxy = IRadioConfig.getService(true);
                if (this.mRadioConfigProxy != null) {
                    this.mRadioConfigProxy.linkToDeath(this.mServiceDeathRecipient, this.mRadioConfigProxyCookie.incrementAndGet());
                    this.mRadioConfigProxy.setResponseFunctions(this.mRadioConfigResponse, this.mRadioConfigIndication);
                } else {
                    loge("getRadioConfigProxy: mRadioConfigProxy == null");
                }
            } catch (RemoteException | RuntimeException e) {
                this.mRadioConfigProxy = null;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getRadioConfigProxy: RadioConfigProxy getService/setResponseFunctions: ");
                stringBuilder.append(e);
                loge(stringBuilder.toString());
            }
            if (this.mRadioConfigProxy == null) {
                loge("getRadioConfigProxy: mRadioConfigProxy == null");
                if (result != null) {
                    AsyncResult.forMessage(result, null, CommandException.fromRilErrno(1));
                    result.sendToTarget();
                }
            }
            return this.mRadioConfigProxy;
        }
    }

    private RILRequest obtainRequest(int request, Message result, WorkSource workSource) {
        RILRequest rr = RILRequest.obtain(request, result, workSource);
        synchronized (this.mRequestList) {
            this.mRequestList.append(rr.mSerial, rr);
        }
        return rr;
    }

    private RILRequest findAndRemoveRequestFromList(int serial) {
        RILRequest rr;
        synchronized (this.mRequestList) {
            rr = (RILRequest) this.mRequestList.get(serial);
            if (rr != null) {
                this.mRequestList.remove(serial);
            }
        }
        return rr;
    }

    public RILRequest processResponse(RadioResponseInfo responseInfo) {
        int serial = responseInfo.serial;
        int error = responseInfo.error;
        int type = responseInfo.type;
        if (type != 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("processResponse: Unexpected response type ");
            stringBuilder.append(type);
            loge(stringBuilder.toString());
        }
        RILRequest rr = findAndRemoveRequestFromList(serial);
        if (rr != null) {
            return rr;
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("processResponse: Unexpected response! serial: ");
        stringBuilder2.append(serial);
        stringBuilder2.append(" error: ");
        stringBuilder2.append(error);
        loge(stringBuilder2.toString());
        return null;
    }

    public void getSimSlotsStatus(Message result) {
        IRadioConfig radioConfigProxy = getRadioConfigProxy(result);
        if (radioConfigProxy != null) {
            RILRequest rr = obtainRequest(144, result, this.mDefaultWorkSource);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(rr.serialString());
            stringBuilder.append("> ");
            stringBuilder.append(requestToString(rr.mRequest));
            logd(stringBuilder.toString());
            try {
                radioConfigProxy.getSimSlotsStatus(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                resetProxyAndRequestList("getSimSlotsStatus", e);
            }
        }
    }

    public void setSimSlotsMapping(int[] physicalSlots, Message result) {
        IRadioConfig radioConfigProxy = getRadioConfigProxy(result);
        if (radioConfigProxy != null) {
            RILRequest rr = obtainRequest(145, result, this.mDefaultWorkSource);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(rr.serialString());
            stringBuilder.append("> ");
            stringBuilder.append(requestToString(rr.mRequest));
            stringBuilder.append(" ");
            stringBuilder.append(Arrays.toString(physicalSlots));
            logd(stringBuilder.toString());
            try {
                radioConfigProxy.setSimSlotsMapping(rr.mSerial, primitiveArrayToArrayList(physicalSlots));
            } catch (RemoteException | RuntimeException e) {
                resetProxyAndRequestList("setSimSlotsMapping", e);
            }
        }
    }

    private static ArrayList<Integer> primitiveArrayToArrayList(int[] arr) {
        ArrayList<Integer> arrayList = new ArrayList(arr.length);
        for (int i : arr) {
            arrayList.add(Integer.valueOf(i));
        }
        return arrayList;
    }

    static String requestToString(int request) {
        switch (request) {
            case 144:
                return "GET_SLOT_STATUS";
            case 145:
                return "SET_LOGICAL_TO_PHYSICAL_SLOT_MAPPING";
            default:
                return "<unknown request>";
        }
    }

    public void registerForSimSlotStatusChanged(Handler h, int what, Object obj) {
        this.mSimSlotStatusRegistrant = new Registrant(h, what, obj);
    }

    public void unregisterForSimSlotStatusChanged(Handler h) {
        if (this.mSimSlotStatusRegistrant != null && this.mSimSlotStatusRegistrant.getHandler() == h) {
            this.mSimSlotStatusRegistrant.clear();
            this.mSimSlotStatusRegistrant = null;
        }
    }

    static ArrayList<IccSlotStatus> convertHalSlotStatus(ArrayList<SimSlotStatus> halSlotStatusList) {
        ArrayList<IccSlotStatus> response = new ArrayList(halSlotStatusList.size());
        Iterator it = halSlotStatusList.iterator();
        while (it.hasNext()) {
            SimSlotStatus slotStatus = (SimSlotStatus) it.next();
            IccSlotStatus iccSlotStatus = new IccSlotStatus();
            iccSlotStatus.setCardState(slotStatus.cardState);
            iccSlotStatus.setSlotState(slotStatus.slotState);
            iccSlotStatus.logicalSlotIndex = slotStatus.logicalSlotId;
            iccSlotStatus.atr = slotStatus.atr;
            iccSlotStatus.iccid = slotStatus.iccid;
            response.add(iccSlotStatus);
        }
        return response;
    }

    private static void logd(String log) {
        Rlog.d(TAG, log);
    }

    private static void loge(String log) {
        Rlog.e(TAG, log);
    }
}
