package com.android.internal.telephony;

import android.hardware.radio.V1_0.CellIdentityCdma;
import android.hardware.radio.V1_0.CellIdentityGsm;
import android.hardware.radio.V1_0.CellIdentityLte;
import android.hardware.radio.V1_0.CellIdentityTdscdma;
import android.hardware.radio.V1_0.CellIdentityWcdma;
import android.hardware.radio.V1_0.DataRegStateResult;
import android.hardware.radio.V1_0.VoiceRegStateResult;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.telephony.CellIdentity;
import android.telephony.NetworkRegistrationState;
import android.telephony.NetworkService;
import android.telephony.NetworkService.NetworkServiceProvider;
import android.telephony.NetworkServiceCallback;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import com.android.internal.telephony.dataconnection.KeepaliveStatus;
import java.util.concurrent.ConcurrentHashMap;

public class CellularNetworkService extends NetworkService {
    private static final boolean DBG = false;
    private static final int GET_CS_REGISTRATION_STATE_DONE = 1;
    private static final int GET_PS_REGISTRATION_STATE_DONE = 2;
    private static final int NETWORK_REGISTRATION_STATE_CHANGED = 3;
    private static final String TAG = CellularNetworkService.class.getSimpleName();

    private class CellularNetworkServiceProvider extends NetworkServiceProvider {
        private final ConcurrentHashMap<Message, NetworkServiceCallback> mCallbackMap = new ConcurrentHashMap();
        private final Handler mHandler;
        private final HandlerThread mHandlerThread = new HandlerThread(CellularNetworkService.class.getSimpleName());
        private final Looper mLooper;
        private final Phone mPhone = PhoneFactory.getPhone(getSlotId());

        CellularNetworkServiceProvider(int slotId) {
            super(CellularNetworkService.this, slotId);
            this.mHandlerThread.start();
            this.mLooper = this.mHandlerThread.getLooper();
            this.mHandler = new Handler(this.mLooper, CellularNetworkService.this) {
                public void handleMessage(Message message) {
                    NetworkServiceCallback callback = (NetworkServiceCallback) CellularNetworkServiceProvider.this.mCallbackMap.remove(message);
                    switch (message.what) {
                        case 1:
                        case 2:
                            if (callback != null) {
                                int resultCode;
                                AsyncResult ar = message.obj;
                                int i = 1;
                                if (message.what != 1) {
                                    i = 2;
                                }
                                NetworkRegistrationState netState = CellularNetworkServiceProvider.this.getRegistrationStateFromResult(ar.result, i);
                                if (ar.exception != null || netState == null) {
                                    resultCode = 5;
                                } else {
                                    resultCode = 0;
                                }
                                try {
                                    callback.onGetNetworkRegistrationStateComplete(resultCode, netState);
                                    break;
                                } catch (Exception e) {
                                    CellularNetworkService cellularNetworkService = CellularNetworkService.this;
                                    StringBuilder stringBuilder = new StringBuilder();
                                    stringBuilder.append("Exception: ");
                                    stringBuilder.append(e);
                                    cellularNetworkService.loge(stringBuilder.toString());
                                    break;
                                }
                            }
                            return;
                            break;
                        case 3:
                            CellularNetworkServiceProvider.this.notifyNetworkRegistrationStateChanged();
                            break;
                        default:
                            return;
                    }
                }
            };
            this.mPhone.mCi.registerForNetworkStateChanged(this.mHandler, 3, null);
        }

        /* JADX WARNING: Missing block: B:8:0x000f, code skipped:
            return 4;
     */
        /* JADX WARNING: Missing block: B:10:0x0011, code skipped:
            return 3;
     */
        /* JADX WARNING: Missing block: B:12:0x0013, code skipped:
            return 2;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        private int getRegStateFromHalRegState(int halRegState) {
            if (halRegState != 10) {
                switch (halRegState) {
                    case 0:
                        break;
                    case 1:
                        return 1;
                    case 2:
                        break;
                    case 3:
                        break;
                    case 4:
                        break;
                    case 5:
                        return 5;
                    default:
                        switch (halRegState) {
                            case 12:
                                break;
                            case 13:
                                break;
                            case 14:
                                break;
                            default:
                                return 0;
                        }
                }
            }
            return 0;
        }

        private boolean isEmergencyOnly(int halRegState) {
            switch (halRegState) {
                case 10:
                case 12:
                case 13:
                case 14:
                    return true;
                default:
                    return false;
            }
        }

        private int[] getAvailableServices(int regState, int domain, boolean emergencyOnly) {
            if (emergencyOnly) {
                return new int[]{5};
            } else if (regState != 5 && regState != 1) {
                return null;
            } else {
                if (domain == 2) {
                    return new int[]{2};
                } else if (domain == 1) {
                    return new int[]{1, 3, 4};
                } else {
                    return null;
                }
            }
        }

        private int getAccessNetworkTechnologyFromRat(int rilRat) {
            return ServiceState.rilRadioTechnologyToNetworkType(rilRat);
        }

        private NetworkRegistrationState getRegistrationStateFromResult(Object result, int domain) {
            if (result == null) {
                return null;
            }
            if (domain == 1) {
                return createRegistrationStateFromVoiceRegState(result);
            }
            if (domain == 2) {
                return createRegistrationStateFromDataRegState(result);
            }
            return null;
        }

        private NetworkRegistrationState createRegistrationStateFromVoiceRegState(Object result) {
            VoiceRegStateResult voiceRegStateResult = result;
            int regState;
            int accessNetworkTechnology;
            boolean emergencyOnly;
            int roamingIndicator;
            int systemIsInPrl;
            int transportType;
            int transportType2;
            if (voiceRegStateResult instanceof VoiceRegStateResult) {
                VoiceRegStateResult voiceRegState = voiceRegStateResult;
                regState = getRegStateFromHalRegState(voiceRegState.regState);
                accessNetworkTechnology = getAccessNetworkTechnologyFromRat(voiceRegState.rat);
                int reasonForDenial = voiceRegState.reasonForDenial;
                emergencyOnly = isEmergencyOnly(voiceRegState.regState);
                boolean cssSupported = voiceRegState.cssSupported;
                roamingIndicator = voiceRegState.roamingIndicator;
                systemIsInPrl = voiceRegState.systemIsInPrl;
                int defaultRoamingIndicator = voiceRegState.defaultRoamingIndicator;
                transportType = 1;
                transportType2 = 1;
                return new NetworkRegistrationState(1, 1, regState, accessNetworkTechnology, reasonForDenial, emergencyOnly, getAvailableServices(regState, 1, emergencyOnly), convertHalCellIdentityToCellIdentity(voiceRegState.cellIdentity), cssSupported, roamingIndicator, systemIsInPrl, defaultRoamingIndicator);
            }
            transportType = 1;
            transportType2 = 1;
            if (!(voiceRegStateResult instanceof android.hardware.radio.V1_2.VoiceRegStateResult)) {
                return null;
            }
            android.hardware.radio.V1_2.VoiceRegStateResult voiceRegState2 = (android.hardware.radio.V1_2.VoiceRegStateResult) voiceRegStateResult;
            int regState2 = getRegStateFromHalRegState(voiceRegState2.regState);
            accessNetworkTechnology = getAccessNetworkTechnologyFromRat(voiceRegState2.rat);
            regState = voiceRegState2.reasonForDenial;
            boolean emergencyOnly2 = isEmergencyOnly(voiceRegState2.regState);
            emergencyOnly = voiceRegState2.cssSupported;
            int roamingIndicator2 = voiceRegState2.roamingIndicator;
            roamingIndicator = voiceRegState2.systemIsInPrl;
            systemIsInPrl = voiceRegState2.defaultRoamingIndicator;
            boolean z = emergencyOnly2;
            int i = regState;
            int i2 = regState2;
            return new NetworkRegistrationState(transportType, transportType2, regState2, accessNetworkTechnology, regState, emergencyOnly2, getAvailableServices(regState2, transportType2, emergencyOnly2), convertHalCellIdentityToCellIdentity(voiceRegState2.cellIdentity), emergencyOnly, roamingIndicator2, roamingIndicator, systemIsInPrl);
        }

        private NetworkRegistrationState createRegistrationStateFromDataRegState(Object result) {
            DataRegStateResult dataRegStateResult = result;
            int regState;
            int accessNetworkTechnology;
            int reasonForDenial;
            boolean emergencyOnly;
            int maxDataCalls;
            if (dataRegStateResult instanceof DataRegStateResult) {
                DataRegStateResult dataRegState = dataRegStateResult;
                regState = getRegStateFromHalRegState(dataRegState.regState);
                accessNetworkTechnology = getAccessNetworkTechnologyFromRat(dataRegState.rat);
                reasonForDenial = dataRegState.reasonDataDenied;
                emergencyOnly = isEmergencyOnly(dataRegState.regState);
                maxDataCalls = dataRegState.maxDataCalls;
                return new NetworkRegistrationState(1, 2, regState, accessNetworkTechnology, reasonForDenial, emergencyOnly, getAvailableServices(regState, 2, emergencyOnly), convertHalCellIdentityToCellIdentity(dataRegState.cellIdentity), maxDataCalls);
            } else if (!(dataRegStateResult instanceof android.hardware.radio.V1_2.DataRegStateResult)) {
                return null;
            } else {
                android.hardware.radio.V1_2.DataRegStateResult dataRegState2 = (android.hardware.radio.V1_2.DataRegStateResult) dataRegStateResult;
                regState = getRegStateFromHalRegState(dataRegState2.regState);
                accessNetworkTechnology = getAccessNetworkTechnologyFromRat(dataRegState2.rat);
                reasonForDenial = dataRegState2.reasonDataDenied;
                emergencyOnly = isEmergencyOnly(dataRegState2.regState);
                maxDataCalls = dataRegState2.maxDataCalls;
                boolean z = emergencyOnly;
                int i = reasonForDenial;
                return new NetworkRegistrationState(1, 2, regState, accessNetworkTechnology, reasonForDenial, emergencyOnly, getAvailableServices(regState, 2, emergencyOnly), convertHalCellIdentityToCellIdentity(dataRegState2.cellIdentity), maxDataCalls);
            }
        }

        private CellIdentity convertHalCellIdentityToCellIdentity(android.hardware.radio.V1_0.CellIdentity cellIdentity) {
            if (cellIdentity == null) {
                return null;
            }
            CellIdentity result = null;
            switch (cellIdentity.cellInfoType) {
                case 1:
                    if (cellIdentity.cellIdentityGsm.size() == 1) {
                        CellIdentityGsm cellIdentityGsm = (CellIdentityGsm) cellIdentity.cellIdentityGsm.get(0);
                        result = new android.telephony.CellIdentityGsm(cellIdentityGsm.lac, cellIdentityGsm.cid, cellIdentityGsm.arfcn, cellIdentityGsm.bsic, cellIdentityGsm.mcc, cellIdentityGsm.mnc, null, null);
                        break;
                    }
                    break;
                case 2:
                    if (cellIdentity.cellIdentityCdma.size() == 1) {
                        CellIdentityCdma cellIdentityCdma = (CellIdentityCdma) cellIdentity.cellIdentityCdma.get(0);
                        result = new android.telephony.CellIdentityCdma(cellIdentityCdma.networkId, cellIdentityCdma.systemId, cellIdentityCdma.baseStationId, cellIdentityCdma.longitude, cellIdentityCdma.latitude);
                        break;
                    }
                    break;
                case 3:
                    if (cellIdentity.cellIdentityLte.size() == 1) {
                        CellIdentityLte cellIdentityLte = (CellIdentityLte) cellIdentity.cellIdentityLte.get(0);
                        result = new android.telephony.CellIdentityLte(cellIdentityLte.ci, cellIdentityLte.pci, cellIdentityLte.tac, cellIdentityLte.earfcn, KeepaliveStatus.INVALID_HANDLE, cellIdentityLte.mcc, cellIdentityLte.mnc, null, null);
                        break;
                    }
                    break;
                case 4:
                    if (cellIdentity.cellIdentityWcdma.size() == 1) {
                        CellIdentityWcdma cellIdentityWcdma = (CellIdentityWcdma) cellIdentity.cellIdentityWcdma.get(0);
                        result = new android.telephony.CellIdentityWcdma(cellIdentityWcdma.lac, cellIdentityWcdma.cid, cellIdentityWcdma.psc, cellIdentityWcdma.uarfcn, cellIdentityWcdma.mcc, cellIdentityWcdma.mnc, null, null);
                        break;
                    }
                    break;
                case 5:
                    if (cellIdentity.cellIdentityTdscdma.size() == 1) {
                        CellIdentityTdscdma cellIdentityTdscdma = (CellIdentityTdscdma) cellIdentity.cellIdentityTdscdma.get(0);
                        result = new android.telephony.CellIdentityTdscdma(cellIdentityTdscdma.mcc, cellIdentityTdscdma.mnc, cellIdentityTdscdma.lac, cellIdentityTdscdma.cid, cellIdentityTdscdma.cpid);
                        break;
                    }
                    break;
            }
            return result;
        }

        private CellIdentity convertHalCellIdentityToCellIdentity(android.hardware.radio.V1_2.CellIdentity cellIdentity) {
            if (cellIdentity == null) {
                return null;
            }
            CellIdentity result = null;
            switch (cellIdentity.cellInfoType) {
                case 1:
                    if (cellIdentity.cellIdentityGsm.size() == 1) {
                        android.hardware.radio.V1_2.CellIdentityGsm cellIdentityGsm = (android.hardware.radio.V1_2.CellIdentityGsm) cellIdentity.cellIdentityGsm.get(0);
                        result = new android.telephony.CellIdentityGsm(cellIdentityGsm.base.lac, cellIdentityGsm.base.cid, cellIdentityGsm.base.arfcn, cellIdentityGsm.base.bsic, cellIdentityGsm.base.mcc, cellIdentityGsm.base.mnc, cellIdentityGsm.operatorNames.alphaLong, cellIdentityGsm.operatorNames.alphaShort);
                        break;
                    }
                    break;
                case 2:
                    if (cellIdentity.cellIdentityCdma.size() == 1) {
                        android.hardware.radio.V1_2.CellIdentityCdma cellIdentityCdma = (android.hardware.radio.V1_2.CellIdentityCdma) cellIdentity.cellIdentityCdma.get(0);
                        result = new android.telephony.CellIdentityCdma(cellIdentityCdma.base.networkId, cellIdentityCdma.base.systemId, cellIdentityCdma.base.baseStationId, cellIdentityCdma.base.longitude, cellIdentityCdma.base.latitude, cellIdentityCdma.operatorNames.alphaLong, cellIdentityCdma.operatorNames.alphaShort);
                        break;
                    }
                    break;
                case 3:
                    if (cellIdentity.cellIdentityLte.size() == 1) {
                        android.hardware.radio.V1_2.CellIdentityLte cellIdentityLte = (android.hardware.radio.V1_2.CellIdentityLte) cellIdentity.cellIdentityLte.get(0);
                        result = new android.telephony.CellIdentityLte(cellIdentityLte.base.ci, cellIdentityLte.base.pci, cellIdentityLte.base.tac, cellIdentityLte.base.earfcn, cellIdentityLte.bandwidth, cellIdentityLte.base.mcc, cellIdentityLte.base.mnc, cellIdentityLte.operatorNames.alphaLong, cellIdentityLte.operatorNames.alphaShort);
                        break;
                    }
                    break;
                case 4:
                    if (cellIdentity.cellIdentityWcdma.size() == 1) {
                        android.hardware.radio.V1_2.CellIdentityWcdma cellIdentityWcdma = (android.hardware.radio.V1_2.CellIdentityWcdma) cellIdentity.cellIdentityWcdma.get(0);
                        result = new android.telephony.CellIdentityWcdma(cellIdentityWcdma.base.lac, cellIdentityWcdma.base.cid, cellIdentityWcdma.base.psc, cellIdentityWcdma.base.uarfcn, cellIdentityWcdma.base.mcc, cellIdentityWcdma.base.mnc, cellIdentityWcdma.operatorNames.alphaLong, cellIdentityWcdma.operatorNames.alphaShort);
                        break;
                    }
                    break;
                case 5:
                    if (cellIdentity.cellIdentityTdscdma.size() == 1) {
                        android.hardware.radio.V1_2.CellIdentityTdscdma cellIdentityTdscdma = (android.hardware.radio.V1_2.CellIdentityTdscdma) cellIdentity.cellIdentityTdscdma.get(0);
                        result = new android.telephony.CellIdentityTdscdma(cellIdentityTdscdma.base.mcc, cellIdentityTdscdma.base.mnc, cellIdentityTdscdma.base.lac, cellIdentityTdscdma.base.cid, cellIdentityTdscdma.base.cpid, cellIdentityTdscdma.operatorNames.alphaLong, cellIdentityTdscdma.operatorNames.alphaShort);
                        break;
                    }
                    break;
            }
            return result;
        }

        public void getNetworkRegistrationState(int domain, NetworkServiceCallback callback) {
            Message message;
            if (domain == 1) {
                message = Message.obtain(this.mHandler, 1);
                this.mCallbackMap.put(message, callback);
                this.mPhone.mCi.getVoiceRegistrationState(message);
            } else if (domain == 2) {
                message = Message.obtain(this.mHandler, 2);
                this.mCallbackMap.put(message, callback);
                this.mPhone.mCi.getDataRegistrationState(message);
            } else {
                CellularNetworkService cellularNetworkService = CellularNetworkService.this;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getNetworkRegistrationState invalid domain ");
                stringBuilder.append(domain);
                cellularNetworkService.loge(stringBuilder.toString());
                callback.onGetNetworkRegistrationStateComplete(2, null);
            }
        }

        protected void onDestroy() {
            super.onDestroy();
            this.mCallbackMap.clear();
            this.mHandlerThread.quit();
            this.mPhone.mCi.unregisterForNetworkStateChanged(this.mHandler);
        }
    }

    protected NetworkServiceProvider createNetworkServiceProvider(int slotId) {
        if (SubscriptionManager.isValidSlotIndex(slotId) || slotId == 2) {
            return new CellularNetworkServiceProvider(slotId);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Tried to Cellular network service with invalid slotId ");
        stringBuilder.append(slotId);
        loge(stringBuilder.toString());
        return null;
    }

    private void log(String s) {
        Rlog.d(TAG, s);
    }

    private void loge(String s) {
        Rlog.e(TAG, s);
    }
}
