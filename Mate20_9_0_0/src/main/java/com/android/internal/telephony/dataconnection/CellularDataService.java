package com.android.internal.telephony.dataconnection;

import android.hardware.radio.V1_0.SetupDataCallResult;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.NetworkUtils;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import android.telephony.data.DataCallResponse;
import android.telephony.data.DataProfile;
import android.telephony.data.DataService;
import android.telephony.data.DataService.DataServiceProvider;
import android.telephony.data.DataServiceCallback;
import android.text.TextUtils;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.CommandException.Error;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CellularDataService extends DataService {
    private static final int DATA_CALL_LIST_CHANGED = 6;
    private static final boolean DBG = false;
    private static final int DEACTIVATE_DATA_ALL_COMPLETE = 2;
    private static final int GET_DATA_CALL_LIST_COMPLETE = 5;
    private static final int SETUP_DATA_CALL_COMPLETE = 1;
    private static final int SET_DATA_PROFILE_COMPLETE = 4;
    private static final int SET_INITIAL_ATTACH_APN_COMPLETE = 3;
    private static final String TAG = CellularDataService.class.getSimpleName();

    private class CellularDataServiceProvider extends DataServiceProvider {
        private final Map<Message, DataServiceCallback> mCallbackMap;
        private final Handler mHandler;
        private final Looper mLooper;
        private final Phone mPhone;

        private CellularDataServiceProvider(int slotId) {
            super(CellularDataService.this, slotId);
            this.mCallbackMap = new HashMap();
            this.mPhone = PhoneFactory.getPhone(getSlotId());
            HandlerThread thread = new HandlerThread(CellularDataService.class.getSimpleName());
            thread.start();
            this.mLooper = thread.getLooper();
            this.mHandler = new Handler(this.mLooper, CellularDataService.this) {
                public void handleMessage(Message message) {
                    DataServiceCallback callback = (DataServiceCallback) CellularDataServiceProvider.this.mCallbackMap.remove(message);
                    AsyncResult ar = message.obj;
                    int i = 0;
                    switch (message.what) {
                        case 1:
                            SetupDataCallResult result = ar.result;
                            i = 0;
                            if (ar.exception != null) {
                                if ((ar.exception instanceof CommandException) && ((CommandException) ar.exception).getCommandError() == Error.RADIO_NOT_AVAILABLE) {
                                    i = 4;
                                } else {
                                    i = 1;
                                }
                            }
                            callback.onSetupDataCallComplete(i, CellularDataService.this.convertDataCallResult(result));
                            break;
                        case 2:
                            if (ar.exception != null) {
                                i = 4;
                            }
                            callback.onDeactivateDataCallComplete(i);
                            break;
                        case 3:
                            if (ar.exception != null) {
                                i = 4;
                            }
                            callback.onSetInitialAttachApnComplete(i);
                            break;
                        case 4:
                            if (ar.exception != null) {
                                i = 4;
                            }
                            callback.onSetDataProfileComplete(i);
                            break;
                        case 5:
                            List list;
                            if (ar.exception != null) {
                                i = 4;
                            }
                            if (ar.exception != null) {
                                list = null;
                            } else {
                                list = CellularDataServiceProvider.this.getDataCallList((List) ar.result);
                            }
                            callback.onGetDataCallListComplete(i, list);
                            break;
                        case 6:
                            CellularDataServiceProvider.this.notifyDataCallListChanged(CellularDataServiceProvider.this.getDataCallList((List) ar.result));
                            break;
                        default:
                            CellularDataService cellularDataService = CellularDataService.this;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Unexpected event: ");
                            stringBuilder.append(message.what);
                            cellularDataService.loge(stringBuilder.toString());
                            return;
                    }
                }
            };
            this.mPhone.mCi.registerForDataCallListChanged(this.mHandler, 6, null);
        }

        private List<DataCallResponse> getDataCallList(List<SetupDataCallResult> dcList) {
            List<DataCallResponse> dcResponseList = new ArrayList();
            for (SetupDataCallResult dcResult : dcList) {
                dcResponseList.add(CellularDataService.this.convertDataCallResult(dcResult));
            }
            return dcResponseList;
        }

        public void setupDataCall(int radioTechnology, DataProfile dataProfile, boolean isRoaming, boolean allowRoaming, int reason, LinkProperties linkProperties, DataServiceCallback callback) {
            DataServiceCallback dataServiceCallback = callback;
            Message message = null;
            if (dataServiceCallback != null) {
                message = Message.obtain(this.mHandler, 1);
                this.mCallbackMap.put(message, dataServiceCallback);
            }
            this.mPhone.mCi.setupDataCall(radioTechnology, dataProfile, isRoaming, allowRoaming, reason, linkProperties, message);
        }

        public void deactivateDataCall(int cid, int reason, DataServiceCallback callback) {
            Message message = null;
            if (callback != null) {
                message = Message.obtain(this.mHandler, 2);
                this.mCallbackMap.put(message, callback);
            }
            this.mPhone.mCi.deactivateDataCall(cid, reason, message);
        }

        public void setInitialAttachApn(DataProfile dataProfile, boolean isRoaming, DataServiceCallback callback) {
            Message message = null;
            if (callback != null) {
                message = Message.obtain(this.mHandler, 3);
                this.mCallbackMap.put(message, callback);
            }
            this.mPhone.mCi.setInitialAttachApn(dataProfile, isRoaming, message);
        }

        public void setDataProfile(List<DataProfile> dps, boolean isRoaming, DataServiceCallback callback) {
            Message message = null;
            if (callback != null) {
                message = Message.obtain(this.mHandler, 4);
                this.mCallbackMap.put(message, callback);
            }
            this.mPhone.mCi.setDataProfile((DataProfile[]) dps.toArray(new DataProfile[dps.size()]), isRoaming, message);
        }

        public void getDataCallList(DataServiceCallback callback) {
            Message message = null;
            if (callback != null) {
                message = Message.obtain(this.mHandler, 5);
                this.mCallbackMap.put(message, callback);
            }
            this.mPhone.mCi.getDataCallList(message);
        }
    }

    public DataServiceProvider createDataServiceProvider(int slotId) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Cellular data service created for slot ");
        stringBuilder.append(slotId);
        log(stringBuilder.toString());
        if (SubscriptionManager.isValidSlotIndex(slotId) || slotId == 2) {
            return new CellularDataServiceProvider(slotId);
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("Tried to cellular data service with invalid slotId ");
        stringBuilder.append(slotId);
        loge(stringBuilder.toString());
        return null;
    }

    @VisibleForTesting
    public DataCallResponse convertDataCallResult(SetupDataCallResult dcResult) {
        StringBuilder stringBuilder;
        SetupDataCallResult setupDataCallResult = dcResult;
        if (setupDataCallResult == null) {
            return null;
        }
        int length;
        String address;
        String[] addresses = null;
        if (!TextUtils.isEmpty(setupDataCallResult.addresses)) {
            addresses = setupDataCallResult.addresses.split("\\s+");
        }
        String[] addresses2 = addresses;
        List<LinkAddress> laList = new ArrayList();
        int i = 0;
        if (addresses2 != null) {
            for (String address2 : addresses2) {
                address = address2.trim();
                if (!address.isEmpty()) {
                    try {
                        LinkAddress la;
                        if (address.split("/").length == 2) {
                            la = new LinkAddress(address);
                        } else {
                            InetAddress ia = NetworkUtils.numericToInetAddress(address);
                            la = new LinkAddress(ia, ia instanceof Inet4Address ? 32 : 64);
                        }
                        laList.add(la);
                    } catch (IllegalArgumentException e) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Unknown address: ");
                        stringBuilder.append(address);
                        stringBuilder.append(", exception = ");
                        stringBuilder.append(e);
                        loge(stringBuilder.toString());
                    }
                }
            }
        }
        addresses = null;
        if (!TextUtils.isEmpty(setupDataCallResult.dnses)) {
            addresses = setupDataCallResult.dnses.split("\\s+");
        }
        String[] dnses = addresses;
        List<InetAddress> dnsList = new ArrayList();
        if (dnses != null) {
            for (String address22 : dnses) {
                address = address22.trim();
                try {
                    dnsList.add(NetworkUtils.numericToInetAddress(address));
                } catch (IllegalArgumentException e2) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Unknown dns: ");
                    stringBuilder.append(address);
                    stringBuilder.append(", exception = ");
                    stringBuilder.append(e2);
                    loge(stringBuilder.toString());
                }
            }
        }
        addresses = null;
        if (!TextUtils.isEmpty(setupDataCallResult.gateways)) {
            addresses = setupDataCallResult.gateways.split("\\s+");
        }
        String[] gateways = addresses;
        List<InetAddress> gatewayList = new ArrayList();
        if (gateways != null) {
            length = gateways.length;
            while (i < length) {
                String gateway = gateways[i].trim();
                try {
                    gatewayList.add(NetworkUtils.numericToInetAddress(gateway));
                } catch (IllegalArgumentException e22) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Unknown gateway: ");
                    stringBuilder2.append(gateway);
                    stringBuilder2.append(", exception = ");
                    stringBuilder2.append(e22);
                    loge(stringBuilder2.toString());
                }
                i++;
            }
        }
        return new DataCallResponse(setupDataCallResult.status, setupDataCallResult.suggestedRetryTime, setupDataCallResult.cid, setupDataCallResult.active, setupDataCallResult.type, setupDataCallResult.ifname, laList, dnsList, gatewayList, new ArrayList(Arrays.asList(setupDataCallResult.pcscf.trim().split("\\s+"))), setupDataCallResult.mtu);
    }

    private void log(String s) {
        Rlog.d(TAG, s);
    }

    private void loge(String s) {
        Rlog.e(TAG, s);
    }
}
