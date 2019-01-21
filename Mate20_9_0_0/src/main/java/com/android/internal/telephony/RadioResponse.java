package com.android.internal.telephony;

import android.hardware.radio.V1_0.ActivityStatsInfo;
import android.hardware.radio.V1_0.AppStatus;
import android.hardware.radio.V1_0.Call;
import android.hardware.radio.V1_0.CallForwardInfo;
import android.hardware.radio.V1_0.CardStatus;
import android.hardware.radio.V1_0.Carrier;
import android.hardware.radio.V1_0.CarrierRestrictions;
import android.hardware.radio.V1_0.CdmaBroadcastSmsConfigInfo;
import android.hardware.radio.V1_0.CellInfo;
import android.hardware.radio.V1_0.DataRegStateResult;
import android.hardware.radio.V1_0.GsmBroadcastSmsConfigInfo;
import android.hardware.radio.V1_0.HardwareConfig;
import android.hardware.radio.V1_0.IccIoResult;
import android.hardware.radio.V1_0.LastCallFailCauseInfo;
import android.hardware.radio.V1_0.LceDataInfo;
import android.hardware.radio.V1_0.LceStatusInfo;
import android.hardware.radio.V1_0.NeighboringCell;
import android.hardware.radio.V1_0.OperatorInfo;
import android.hardware.radio.V1_0.RadioCapability;
import android.hardware.radio.V1_0.RadioResponseInfo;
import android.hardware.radio.V1_0.SendSmsResult;
import android.hardware.radio.V1_0.SetupDataCallResult;
import android.hardware.radio.V1_0.SignalStrength;
import android.hardware.radio.V1_0.UusInfo;
import android.hardware.radio.V1_0.VoiceRegStateResult;
import android.hardware.radio.V1_1.KeepaliveStatus;
import android.hardware.radio.V1_2.IRadioResponse.Stub;
import android.os.AsyncResult;
import android.os.Message;
import android.os.SystemClock;
import android.service.carrier.CarrierIdentifier;
import android.telephony.ModemActivityInfo;
import android.telephony.NeighboringCellInfo;
import android.telephony.PhoneNumberUtils;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Base64;
import com.android.internal.telephony.gsm.SmsBroadcastConfigInfo;
import com.android.internal.telephony.uicc.IccCardApplicationStatus;
import com.android.internal.telephony.uicc.IccCardStatus;
import com.android.internal.telephony.uicc.IccUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RadioResponse extends Stub {
    private static final int CDMA_BROADCAST_SMS_NO_OF_SERVICE_CATEGORIES = 31;
    private static final int CDMA_BSI_NO_OF_INTS_STRUCT = 3;
    RIL mRil;

    public RadioResponse(RIL ril) {
        this.mRil = ril;
    }

    static void sendMessageResponse(Message msg, Object ret) {
        if (msg != null) {
            AsyncResult.forMessage(msg, ret, null);
            msg.sendToTarget();
        }
    }

    public void acknowledgeRequest(int serial) {
        this.mRil.processRequestAck(serial);
    }

    public void getIccCardStatusResponse(RadioResponseInfo responseInfo, CardStatus cardStatus) {
        responseIccCardStatus(responseInfo, cardStatus);
    }

    public void getIccCardStatusResponse_1_2(RadioResponseInfo responseInfo, android.hardware.radio.V1_2.CardStatus cardStatus) {
        responseIccCardStatus_1_2(responseInfo, cardStatus);
    }

    public void supplyIccPinForAppResponse(RadioResponseInfo responseInfo, int remainingAttempts) {
        responseInts(responseInfo, remainingAttempts);
    }

    public void supplyIccPukForAppResponse(RadioResponseInfo responseInfo, int remainingAttempts) {
        responseInts(responseInfo, remainingAttempts);
    }

    public void supplyIccPin2ForAppResponse(RadioResponseInfo responseInfo, int remainingAttempts) {
        responseInts(responseInfo, remainingAttempts);
    }

    public void supplyIccPuk2ForAppResponse(RadioResponseInfo responseInfo, int remainingAttempts) {
        responseInts(responseInfo, remainingAttempts);
    }

    public void changeIccPinForAppResponse(RadioResponseInfo responseInfo, int remainingAttempts) {
        responseInts(responseInfo, remainingAttempts);
    }

    public void changeIccPin2ForAppResponse(RadioResponseInfo responseInfo, int remainingAttempts) {
        responseInts(responseInfo, remainingAttempts);
    }

    public void supplyNetworkDepersonalizationResponse(RadioResponseInfo responseInfo, int retriesRemaining) {
        responseInts(responseInfo, retriesRemaining);
    }

    public void getCurrentCallsResponse(RadioResponseInfo responseInfo, ArrayList<Call> calls) {
        responseCurrentCalls(responseInfo, calls);
    }

    public void getCurrentCallsResponse_1_2(RadioResponseInfo responseInfo, ArrayList<android.hardware.radio.V1_2.Call> calls) {
        responseCurrentCalls_1_2(responseInfo, calls);
    }

    public void dialResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void getIMSIForAppResponse(RadioResponseInfo responseInfo, String imsi) {
        responseString(responseInfo, imsi);
    }

    public void hangupConnectionResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void hangupWaitingOrBackgroundResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void hangupForegroundResumeBackgroundResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void switchWaitingOrHoldingAndActiveResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void conferenceResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void rejectCallResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void getLastCallFailCauseResponse(RadioResponseInfo responseInfo, LastCallFailCauseInfo fcInfo) {
        responseLastCallFailCauseInfo(responseInfo, fcInfo);
    }

    public void getSignalStrengthResponse(RadioResponseInfo responseInfo, SignalStrength sigStrength) {
        responseSignalStrength(responseInfo, sigStrength);
    }

    public void getSignalStrengthResponse_1_2(RadioResponseInfo responseInfo, android.hardware.radio.V1_2.SignalStrength signalStrength) {
        responseSignalStrength_1_2(responseInfo, signalStrength);
    }

    public void getVoiceRegistrationStateResponse(RadioResponseInfo responseInfo, VoiceRegStateResult voiceRegResponse) {
        RILRequest rr = this.mRil.processResponse(responseInfo);
        if (rr != null) {
            if (responseInfo.error == 0) {
                sendMessageResponse(rr.mResult, voiceRegResponse);
            }
            this.mRil.processResponseDone(rr, responseInfo, voiceRegResponse);
        }
    }

    public void getVoiceRegistrationStateResponse_1_2(RadioResponseInfo responseInfo, android.hardware.radio.V1_2.VoiceRegStateResult voiceRegResponse) {
        RILRequest rr = this.mRil.processResponse(responseInfo);
        if (rr != null) {
            if (responseInfo.error == 0) {
                sendMessageResponse(rr.mResult, voiceRegResponse);
            }
            this.mRil.processResponseDone(rr, responseInfo, voiceRegResponse);
        }
    }

    public void getDataRegistrationStateResponse(RadioResponseInfo responseInfo, DataRegStateResult dataRegResponse) {
        RILRequest rr = this.mRil.processResponse(responseInfo);
        if (rr != null) {
            if (responseInfo.error == 0) {
                sendMessageResponse(rr.mResult, dataRegResponse);
            }
            this.mRil.processResponseDone(rr, responseInfo, dataRegResponse);
        }
    }

    public void getDataRegistrationStateResponse_1_2(RadioResponseInfo responseInfo, android.hardware.radio.V1_2.DataRegStateResult dataRegResponse) {
        RILRequest rr = this.mRil.processResponse(responseInfo);
        if (rr != null) {
            if (responseInfo.error == 0) {
                sendMessageResponse(rr.mResult, dataRegResponse);
            }
            this.mRil.processResponseDone(rr, responseInfo, dataRegResponse);
        }
    }

    public void getOperatorResponse(RadioResponseInfo responseInfo, String longName, String shortName, String numeric) {
        responseStrings(responseInfo, longName, shortName, numeric);
    }

    public void setRadioPowerResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void sendDtmfResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void sendSmsResponse(RadioResponseInfo responseInfo, SendSmsResult sms) {
        responseSms(responseInfo, sms);
    }

    public void sendSMSExpectMoreResponse(RadioResponseInfo responseInfo, SendSmsResult sms) {
        responseSms(responseInfo, sms);
    }

    public void setupDataCallResponse(RadioResponseInfo responseInfo, SetupDataCallResult setupDataCallResult) {
        responseSetupDataCall(responseInfo, setupDataCallResult);
    }

    public void iccIOForAppResponse(RadioResponseInfo responseInfo, IccIoResult iccIo) {
        responseIccIo(responseInfo, iccIo);
    }

    public void sendUssdResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void cancelPendingUssdResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void getClirResponse(RadioResponseInfo responseInfo, int n, int m) {
        responseInts(responseInfo, n, m);
    }

    public void setClirResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void getCallForwardStatusResponse(RadioResponseInfo responseInfo, ArrayList<CallForwardInfo> callForwardInfos) {
        responseCallForwardInfo(responseInfo, callForwardInfos);
    }

    public void setCallForwardResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void getCallWaitingResponse(RadioResponseInfo responseInfo, boolean enable, int serviceClass) {
        responseInts(responseInfo, enable, serviceClass);
    }

    public void setCallWaitingResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void acknowledgeLastIncomingGsmSmsResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void acceptCallResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void deactivateDataCallResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void getFacilityLockForAppResponse(RadioResponseInfo responseInfo, int response) {
        responseInts(responseInfo, response);
    }

    public void setFacilityLockForAppResponse(RadioResponseInfo responseInfo, int retry) {
        responseInts(responseInfo, retry);
    }

    public void setBarringPasswordResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void getNetworkSelectionModeResponse(RadioResponseInfo responseInfo, boolean selection) {
        responseInts(responseInfo, selection);
    }

    public void setNetworkSelectionModeAutomaticResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void setNetworkSelectionModeManualResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void getAvailableNetworksResponse(RadioResponseInfo responseInfo, ArrayList<OperatorInfo> networkInfos) {
        responseOperatorInfos(responseInfo, networkInfos);
    }

    public void startNetworkScanResponse(RadioResponseInfo responseInfo) {
        responseScanStatus(responseInfo);
    }

    public void stopNetworkScanResponse(RadioResponseInfo responseInfo) {
        responseScanStatus(responseInfo);
    }

    public void startDtmfResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void stopDtmfResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void getBasebandVersionResponse(RadioResponseInfo responseInfo, String version) {
        responseString(responseInfo, version);
    }

    public void separateConnectionResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void setMuteResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void getMuteResponse(RadioResponseInfo responseInfo, boolean enable) {
        responseInts(responseInfo, enable);
    }

    public void getClipResponse(RadioResponseInfo responseInfo, int status) {
        responseInts(responseInfo, status);
    }

    public void getDataCallListResponse(RadioResponseInfo responseInfo, ArrayList<SetupDataCallResult> dataCallResultList) {
        this.mRil.responseDataCallList(responseInfo, dataCallResultList);
    }

    public void sendOemRilRequestRawResponse(RadioResponseInfo responseInfo, ArrayList<Byte> arrayList) {
    }

    public void setSuppServiceNotificationsResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void writeSmsToSimResponse(RadioResponseInfo responseInfo, int index) {
        responseInts(responseInfo, index);
    }

    public void deleteSmsOnSimResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void setBandModeResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void getAvailableBandModesResponse(RadioResponseInfo responseInfo, ArrayList<Integer> bandModes) {
        responseIntArrayList(responseInfo, bandModes);
    }

    public void sendEnvelopeResponse(RadioResponseInfo responseInfo, String commandResponse) {
        responseString(responseInfo, commandResponse);
    }

    public void sendTerminalResponseToSimResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void handleStkCallSetupRequestFromSimResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void explicitCallTransferResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void setPreferredNetworkTypeResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void getPreferredNetworkTypeResponse(RadioResponseInfo responseInfo, int nwType) {
        this.mRil.mPreferredNetworkType = nwType;
        responseInts(responseInfo, nwType);
    }

    public void getNeighboringCidsResponse(RadioResponseInfo responseInfo, ArrayList<NeighboringCell> cells) {
        responseCellList(responseInfo, cells);
    }

    public void setLocationUpdatesResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void setCdmaSubscriptionSourceResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void setCdmaRoamingPreferenceResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void getCdmaRoamingPreferenceResponse(RadioResponseInfo responseInfo, int type) {
        responseInts(responseInfo, type);
    }

    public void setTTYModeResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void getTTYModeResponse(RadioResponseInfo responseInfo, int mode) {
        responseInts(responseInfo, mode);
    }

    public void setPreferredVoicePrivacyResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void getPreferredVoicePrivacyResponse(RadioResponseInfo responseInfo, boolean enable) {
        responseInts(responseInfo, enable);
    }

    public void sendCDMAFeatureCodeResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void sendBurstDtmfResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void sendCdmaSmsResponse(RadioResponseInfo responseInfo, SendSmsResult sms) {
        responseSms(responseInfo, sms);
    }

    public void acknowledgeLastIncomingCdmaSmsResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void getGsmBroadcastConfigResponse(RadioResponseInfo responseInfo, ArrayList<GsmBroadcastSmsConfigInfo> configs) {
        responseGmsBroadcastConfig(responseInfo, configs);
    }

    public void setGsmBroadcastConfigResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void setGsmBroadcastActivationResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void getCdmaBroadcastConfigResponse(RadioResponseInfo responseInfo, ArrayList<CdmaBroadcastSmsConfigInfo> configs) {
        responseCdmaBroadcastConfig(responseInfo, configs);
    }

    public void setCdmaBroadcastConfigResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void setCdmaBroadcastActivationResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void getCDMASubscriptionResponse(RadioResponseInfo responseInfo, String mdn, String hSid, String hNid, String min, String prl) {
        responseStrings(responseInfo, mdn, hSid, hNid, min, prl);
    }

    public void writeSmsToRuimResponse(RadioResponseInfo responseInfo, int index) {
        responseInts(responseInfo, index);
    }

    public void deleteSmsOnRuimResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void getDeviceIdentityResponse(RadioResponseInfo responseInfo, String imei, String imeisv, String esn, String meid) {
        responseStrings(responseInfo, imei, imeisv, esn, meid);
    }

    public void exitEmergencyCallbackModeResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void getSmscAddressResponse(RadioResponseInfo responseInfo, String smsc) {
        responseString(responseInfo, smsc);
    }

    public void setSmscAddressResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void reportSmsMemoryStatusResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void reportStkServiceIsRunningResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void getCdmaSubscriptionSourceResponse(RadioResponseInfo responseInfo, int source) {
        responseInts(responseInfo, source);
    }

    public void requestIsimAuthenticationResponse(RadioResponseInfo responseInfo, String response) {
        throw new RuntimeException("Inexplicable response received for requestIsimAuthentication");
    }

    public void acknowledgeIncomingGsmSmsWithPduResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void sendEnvelopeWithStatusResponse(RadioResponseInfo responseInfo, IccIoResult iccIo) {
        responseIccIo(responseInfo, iccIo);
    }

    public void getVoiceRadioTechnologyResponse(RadioResponseInfo responseInfo, int rat) {
        responseInts(responseInfo, rat);
    }

    public void getCellInfoListResponse(RadioResponseInfo responseInfo, ArrayList<CellInfo> cellInfo) {
        responseCellInfoList(responseInfo, cellInfo);
    }

    public void getCellInfoListResponse_1_2(RadioResponseInfo responseInfo, ArrayList<android.hardware.radio.V1_2.CellInfo> cellInfo) {
        responseCellInfoList_1_2(responseInfo, cellInfo);
    }

    public void setCellInfoListRateResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void setInitialAttachApnResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void getImsRegistrationStateResponse(RadioResponseInfo responseInfo, boolean isRegistered, int ratFamily) {
        responseInts(responseInfo, isRegistered, ratFamily);
    }

    public void sendImsSmsResponse(RadioResponseInfo responseInfo, SendSmsResult sms) {
        responseSms(responseInfo, sms);
    }

    public void iccTransmitApduBasicChannelResponse(RadioResponseInfo responseInfo, IccIoResult result) {
        responseIccIo(responseInfo, result);
    }

    public void iccOpenLogicalChannelResponse(RadioResponseInfo responseInfo, int channelId, ArrayList<Byte> selectResponse) {
        ArrayList<Integer> arr = new ArrayList();
        arr.add(Integer.valueOf(channelId));
        for (int i = 0; i < selectResponse.size(); i++) {
            arr.add(Integer.valueOf(((Byte) selectResponse.get(i)).byteValue()));
        }
        responseIntArrayList(responseInfo, arr);
    }

    public void iccCloseLogicalChannelResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void iccTransmitApduLogicalChannelResponse(RadioResponseInfo responseInfo, IccIoResult result) {
        responseIccIo(responseInfo, result);
    }

    public void nvReadItemResponse(RadioResponseInfo responseInfo, String result) {
        responseString(responseInfo, result);
    }

    public void nvWriteItemResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void nvWriteCdmaPrlResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void nvResetConfigResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void setUiccSubscriptionResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void setDataAllowedResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void getHardwareConfigResponse(RadioResponseInfo responseInfo, ArrayList<HardwareConfig> config) {
        responseHardwareConfig(responseInfo, config);
    }

    public void requestIccSimAuthenticationResponse(RadioResponseInfo responseInfo, IccIoResult result) {
        responseICC_IOBase64(responseInfo, result);
    }

    public void setDataProfileResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void requestShutdownResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void getRadioCapabilityResponse(RadioResponseInfo responseInfo, RadioCapability rc) {
        RILRequest rr = this.mRil.processResponse(responseInfo);
        if (rr != null) {
            RadioCapability ret = RIL.convertHalRadioCapability(rc, this.mRil);
            if (responseInfo.error == 6 || responseInfo.error == 2) {
                ret = this.mRil.makeStaticRadioCapability();
                responseInfo.error = 0;
            }
            if (responseInfo.error == 0) {
                sendMessageResponse(rr.mResult, ret);
            }
            this.mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    public void setRadioCapabilityResponse(RadioResponseInfo responseInfo, RadioCapability rc) {
        responseRadioCapability(responseInfo, rc);
    }

    public void startLceServiceResponse(RadioResponseInfo responseInfo, LceStatusInfo statusInfo) {
        responseLceStatus(responseInfo, statusInfo);
    }

    public void stopLceServiceResponse(RadioResponseInfo responseInfo, LceStatusInfo statusInfo) {
        responseLceStatus(responseInfo, statusInfo);
    }

    public void pullLceDataResponse(RadioResponseInfo responseInfo, LceDataInfo lceInfo) {
        responseLceData(responseInfo, lceInfo);
    }

    public void getModemActivityInfoResponse(RadioResponseInfo responseInfo, ActivityStatsInfo activityInfo) {
        responseActivityData(responseInfo, activityInfo);
    }

    public void setAllowedCarriersResponse(RadioResponseInfo responseInfo, int numAllowed) {
        responseInts(responseInfo, numAllowed);
    }

    public void getAllowedCarriersResponse(RadioResponseInfo responseInfo, boolean allAllowed, CarrierRestrictions carriers) {
        responseCarrierIdentifiers(responseInfo, allAllowed, carriers);
    }

    public void sendDeviceStateResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void setCarrierInfoForImsiEncryptionResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void setIndicationFilterResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void setSimCardPowerResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void setSignalStrengthReportingCriteriaResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void setLinkCapacityReportingCriteriaResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void setSimCardPowerResponse_1_1(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void startKeepaliveResponse(RadioResponseInfo responseInfo, KeepaliveStatus keepaliveStatus) {
        RILRequest rr = this.mRil.processResponse(responseInfo);
        if (rr != null) {
            com.android.internal.telephony.dataconnection.KeepaliveStatus ret;
            int i = responseInfo.error;
            if (i == 0) {
                i = convertHalKeepaliveStatusCode(keepaliveStatus.code);
                if (i < 0) {
                    ret = new com.android.internal.telephony.dataconnection.KeepaliveStatus(1);
                } else {
                    ret = new com.android.internal.telephony.dataconnection.KeepaliveStatus(keepaliveStatus.sessionHandle, i);
                }
            } else if (i == 6) {
                ret = new com.android.internal.telephony.dataconnection.KeepaliveStatus(1);
                responseInfo.error = 0;
            } else if (i != 42) {
                ret = new com.android.internal.telephony.dataconnection.KeepaliveStatus(3);
            } else {
                ret = new com.android.internal.telephony.dataconnection.KeepaliveStatus(2);
            }
            sendMessageResponse(rr.mResult, ret);
            this.mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    public void stopKeepaliveResponse(RadioResponseInfo responseInfo) {
        RILRequest rr = this.mRil.processResponse(responseInfo);
        if (rr != null && responseInfo.error == 0) {
            sendMessageResponse(rr.mResult, null);
            this.mRil.processResponseDone(rr, responseInfo, null);
        }
    }

    private int convertHalKeepaliveStatusCode(int halCode) {
        switch (halCode) {
            case 0:
                return 0;
            case 1:
                return 1;
            case 2:
                return 2;
            default:
                RIL ril = this.mRil;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Invalid Keepalive Status");
                stringBuilder.append(halCode);
                ril.riljLog(stringBuilder.toString());
                return -1;
        }
    }

    private IccCardStatus convertHalCardStatus(CardStatus cardStatus) {
        IccCardStatus iccCardStatus = new IccCardStatus();
        iccCardStatus.setCardState(cardStatus.cardState);
        iccCardStatus.setUniversalPinState(cardStatus.universalPinState);
        iccCardStatus.mGsmUmtsSubscriptionAppIndex = cardStatus.gsmUmtsSubscriptionAppIndex;
        iccCardStatus.mCdmaSubscriptionAppIndex = cardStatus.cdmaSubscriptionAppIndex;
        iccCardStatus.mImsSubscriptionAppIndex = cardStatus.imsSubscriptionAppIndex;
        int numApplications = cardStatus.applications.size();
        if (numApplications > 8) {
            numApplications = 8;
        }
        iccCardStatus.mApplications = new IccCardApplicationStatus[numApplications];
        for (int i = 0; i < numApplications; i++) {
            AppStatus rilAppStatus = (AppStatus) cardStatus.applications.get(i);
            IccCardApplicationStatus appStatus = new IccCardApplicationStatus();
            appStatus.app_type = appStatus.AppTypeFromRILInt(rilAppStatus.appType);
            appStatus.app_state = appStatus.AppStateFromRILInt(rilAppStatus.appState);
            appStatus.perso_substate = appStatus.PersoSubstateFromRILInt(rilAppStatus.persoSubstate);
            appStatus.aid = rilAppStatus.aidPtr;
            appStatus.app_label = rilAppStatus.appLabelPtr;
            appStatus.pin1_replaced = rilAppStatus.pin1Replaced;
            appStatus.pin1 = appStatus.PinStateFromRILInt(rilAppStatus.pin1);
            appStatus.pin2 = appStatus.PinStateFromRILInt(rilAppStatus.pin2);
            iccCardStatus.mApplications[i] = appStatus;
            RIL ril = this.mRil;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("IccCardApplicationStatus ");
            stringBuilder.append(i);
            stringBuilder.append(":");
            stringBuilder.append(appStatus.toString());
            ril.riljLog(stringBuilder.toString());
        }
        return iccCardStatus;
    }

    private void responseIccCardStatus(RadioResponseInfo responseInfo, CardStatus cardStatus) {
        RILRequest rr = this.mRil.processResponse(responseInfo);
        if (rr != null) {
            IccCardStatus iccCardStatus = convertHalCardStatus(cardStatus);
            RIL ril = this.mRil;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("responseIccCardStatus: from HIDL: ");
            stringBuilder.append(iccCardStatus);
            ril.riljLog(stringBuilder.toString());
            if (responseInfo.error == 0) {
                sendMessageResponse(rr.mResult, iccCardStatus);
            }
            this.mRil.processResponseDone(rr, responseInfo, iccCardStatus);
        }
    }

    private void responseIccCardStatus_1_2(RadioResponseInfo responseInfo, android.hardware.radio.V1_2.CardStatus cardStatus) {
        RILRequest rr = this.mRil.processResponse(responseInfo);
        if (rr != null) {
            IccCardStatus iccCardStatus = convertHalCardStatus(cardStatus.base);
            iccCardStatus.physicalSlotIndex = cardStatus.physicalSlotId;
            iccCardStatus.atr = cardStatus.atr;
            iccCardStatus.iccid = cardStatus.iccid;
            RIL ril = this.mRil;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("responseIccCardStatus: from HIDL: ");
            stringBuilder.append(iccCardStatus);
            ril.riljLog(stringBuilder.toString());
            if (responseInfo.error == 0) {
                sendMessageResponse(rr.mResult, iccCardStatus);
            }
            this.mRil.processResponseDone(rr, responseInfo, iccCardStatus);
        }
    }

    private void responseInts(RadioResponseInfo responseInfo, int... var) {
        ArrayList<Integer> ints = new ArrayList();
        for (int valueOf : var) {
            ints.add(Integer.valueOf(valueOf));
        }
        responseIntArrayList(responseInfo, ints);
    }

    private void responseIntArrayList(RadioResponseInfo responseInfo, ArrayList<Integer> var) {
        RILRequest rr = this.mRil.processResponse(responseInfo);
        if (rr != null) {
            int i;
            int[] ret = new int[var.size()];
            int i2 = 0;
            for (i = 0; i < var.size(); i++) {
                ret[i] = ((Integer) var.get(i)).intValue();
            }
            if (responseInfo.error == 0) {
                sendMessageResponse(rr.mResult, ret);
            } else {
                i = rr.mRequest;
                if (!(i == 2 || i == 4 || i == 43)) {
                    switch (i) {
                        case 6:
                        case 7:
                        case 8:
                            break;
                    }
                }
                int[] response = new int[var.size()];
                while (i2 < var.size()) {
                    response[i2] = ((Integer) var.get(i2)).intValue();
                    i2++;
                }
                ret = response;
            }
            this.mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    private void responseCurrentCalls(RadioResponseInfo responseInfo, ArrayList<Call> calls) {
        RILRequest rr = this.mRil.processResponse(responseInfo);
        if (rr != null) {
            int num = calls.size();
            ArrayList<DriverCall> dcCalls = new ArrayList(num);
            for (int i = 0; i < num; i++) {
                DriverCall dc = new DriverCall();
                dc.state = DriverCall.stateFromCLCC(((Call) calls.get(i)).state);
                dc.index = ((Call) calls.get(i)).index;
                dc.TOA = ((Call) calls.get(i)).toa;
                dc.isMpty = ((Call) calls.get(i)).isMpty;
                dc.isMT = ((Call) calls.get(i)).isMT;
                dc.als = ((Call) calls.get(i)).als;
                dc.isVoice = ((Call) calls.get(i)).isVoice;
                dc.isVoicePrivacy = ((Call) calls.get(i)).isVoicePrivacy;
                dc.number = ((Call) calls.get(i)).number;
                dc.numberPresentation = DriverCall.presentationFromCLIP(((Call) calls.get(i)).numberPresentation);
                dc.name = ((Call) calls.get(i)).name;
                dc.namePresentation = DriverCall.presentationFromCLIP(((Call) calls.get(i)).namePresentation);
                if (((Call) calls.get(i)).uusInfo.size() == 1) {
                    dc.uusInfo = new UUSInfo();
                    dc.uusInfo.setType(((UusInfo) ((Call) calls.get(i)).uusInfo.get(0)).uusType);
                    dc.uusInfo.setDcs(((UusInfo) ((Call) calls.get(i)).uusInfo.get(0)).uusDcs);
                    if (TextUtils.isEmpty(((UusInfo) ((Call) calls.get(i)).uusInfo.get(0)).uusData)) {
                        this.mRil.riljLog("responseCurrentCalls: uusInfo data is null or empty");
                    } else {
                        dc.uusInfo.setUserData(((UusInfo) ((Call) calls.get(i)).uusInfo.get(0)).uusData.getBytes());
                    }
                    this.mRil.riljLogv(String.format("Incoming UUS : type=%d, dcs=%d, length=%d", new Object[]{Integer.valueOf(dc.uusInfo.getType()), Integer.valueOf(dc.uusInfo.getDcs()), Integer.valueOf(dc.uusInfo.getUserData().length)}));
                    RIL ril = this.mRil;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Incoming UUS : data (hex): ");
                    stringBuilder.append(IccUtils.bytesToHexString(dc.uusInfo.getUserData()));
                    ril.riljLogv(stringBuilder.toString());
                } else {
                    this.mRil.riljLogv("Incoming UUS : NOT present!");
                }
                dc.number = PhoneNumberUtils.stringFromStringAndTOA(dc.number, dc.TOA);
                dcCalls.add(dc);
                if (dc.isVoicePrivacy) {
                    this.mRil.mVoicePrivacyOnRegistrants.notifyRegistrants();
                    this.mRil.riljLog("InCall VoicePrivacy is enabled");
                } else {
                    this.mRil.mVoicePrivacyOffRegistrants.notifyRegistrants();
                    this.mRil.riljLog("InCall VoicePrivacy is disabled");
                }
            }
            Collections.sort(dcCalls);
            if (num == 0 && this.mRil.mTestingEmergencyCall.getAndSet(false) && this.mRil.mEmergencyCallbackModeRegistrant != null) {
                this.mRil.riljLog("responseCurrentCalls: call ended, testing emergency call, notify ECM Registrants");
                this.mRil.mEmergencyCallbackModeRegistrant.notifyRegistrant();
            }
            if (responseInfo.error == 0) {
                sendMessageResponse(rr.mResult, dcCalls);
            }
            this.mRil.processResponseDone(rr, responseInfo, dcCalls);
        }
    }

    private void responseCurrentCalls_1_2(RadioResponseInfo responseInfo, ArrayList<android.hardware.radio.V1_2.Call> calls) {
        RILRequest rr = this.mRil.processResponse(responseInfo);
        if (rr != null) {
            int num = calls.size();
            ArrayList<DriverCall> dcCalls = new ArrayList(num);
            for (int i = 0; i < num; i++) {
                DriverCall dc = new DriverCall();
                dc.state = DriverCall.stateFromCLCC(((android.hardware.radio.V1_2.Call) calls.get(i)).base.state);
                dc.index = ((android.hardware.radio.V1_2.Call) calls.get(i)).base.index;
                dc.TOA = ((android.hardware.radio.V1_2.Call) calls.get(i)).base.toa;
                dc.isMpty = ((android.hardware.radio.V1_2.Call) calls.get(i)).base.isMpty;
                dc.isMT = ((android.hardware.radio.V1_2.Call) calls.get(i)).base.isMT;
                dc.als = ((android.hardware.radio.V1_2.Call) calls.get(i)).base.als;
                dc.isVoice = ((android.hardware.radio.V1_2.Call) calls.get(i)).base.isVoice;
                dc.isVoicePrivacy = ((android.hardware.radio.V1_2.Call) calls.get(i)).base.isVoicePrivacy;
                dc.number = ((android.hardware.radio.V1_2.Call) calls.get(i)).base.number;
                dc.numberPresentation = DriverCall.presentationFromCLIP(((android.hardware.radio.V1_2.Call) calls.get(i)).base.numberPresentation);
                dc.name = ((android.hardware.radio.V1_2.Call) calls.get(i)).base.name;
                dc.namePresentation = DriverCall.presentationFromCLIP(((android.hardware.radio.V1_2.Call) calls.get(i)).base.namePresentation);
                if (((android.hardware.radio.V1_2.Call) calls.get(i)).base.uusInfo.size() == 1) {
                    dc.uusInfo = new UUSInfo();
                    dc.uusInfo.setType(((UusInfo) ((android.hardware.radio.V1_2.Call) calls.get(i)).base.uusInfo.get(0)).uusType);
                    dc.uusInfo.setDcs(((UusInfo) ((android.hardware.radio.V1_2.Call) calls.get(i)).base.uusInfo.get(0)).uusDcs);
                    if (TextUtils.isEmpty(((UusInfo) ((android.hardware.radio.V1_2.Call) calls.get(i)).base.uusInfo.get(0)).uusData)) {
                        this.mRil.riljLog("responseCurrentCalls: uusInfo data is null or empty");
                    } else {
                        dc.uusInfo.setUserData(((UusInfo) ((android.hardware.radio.V1_2.Call) calls.get(i)).base.uusInfo.get(0)).uusData.getBytes());
                    }
                    this.mRil.riljLogv(String.format("Incoming UUS : type=%d, dcs=%d, length=%d", new Object[]{Integer.valueOf(dc.uusInfo.getType()), Integer.valueOf(dc.uusInfo.getDcs()), Integer.valueOf(dc.uusInfo.getUserData().length)}));
                    RIL ril = this.mRil;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Incoming UUS : data (hex): ");
                    stringBuilder.append(IccUtils.bytesToHexString(dc.uusInfo.getUserData()));
                    ril.riljLogv(stringBuilder.toString());
                } else {
                    this.mRil.riljLogv("Incoming UUS : NOT present!");
                }
                dc.number = PhoneNumberUtils.stringFromStringAndTOA(dc.number, dc.TOA);
                dc.audioQuality = ((android.hardware.radio.V1_2.Call) calls.get(i)).audioQuality;
                dcCalls.add(dc);
                if (dc.isVoicePrivacy) {
                    this.mRil.mVoicePrivacyOnRegistrants.notifyRegistrants();
                    this.mRil.riljLog("InCall VoicePrivacy is enabled");
                } else {
                    this.mRil.mVoicePrivacyOffRegistrants.notifyRegistrants();
                    this.mRil.riljLog("InCall VoicePrivacy is disabled");
                }
            }
            Collections.sort(dcCalls);
            if (num == 0 && this.mRil.mTestingEmergencyCall.getAndSet(false) && this.mRil.mEmergencyCallbackModeRegistrant != null) {
                this.mRil.riljLog("responseCurrentCalls: call ended, testing emergency call, notify ECM Registrants");
                this.mRil.mEmergencyCallbackModeRegistrant.notifyRegistrant();
            }
            if (responseInfo.error == 0) {
                sendMessageResponse(rr.mResult, dcCalls);
            }
            this.mRil.processResponseDone(rr, responseInfo, dcCalls);
        }
    }

    private void responseVoid(RadioResponseInfo responseInfo) {
        RILRequest rr = this.mRil.processResponse(responseInfo);
        if (rr != null) {
            if (responseInfo.error == 0) {
                sendMessageResponse(rr.mResult, null);
            }
            this.mRil.processResponseDone(rr, responseInfo, null);
        }
    }

    private void responseString(RadioResponseInfo responseInfo, String str) {
        RILRequest rr = this.mRil.processResponse(responseInfo);
        if (rr != null) {
            if (responseInfo.error == 0) {
                sendMessageResponse(rr.mResult, str);
            }
            this.mRil.processResponseDone(rr, responseInfo, str);
        }
    }

    private void responseStrings(RadioResponseInfo responseInfo, String... str) {
        ArrayList<String> strings = new ArrayList();
        for (Object add : str) {
            strings.add(add);
        }
        responseStringArrayList(this.mRil, responseInfo, strings);
    }

    static void responseStringArrayList(RIL ril, RadioResponseInfo responseInfo, ArrayList<String> strings) {
        RILRequest rr = ril.processResponse(responseInfo);
        if (rr != null) {
            String[] ret = new String[strings.size()];
            for (int i = 0; i < strings.size(); i++) {
                ret[i] = (String) strings.get(i);
            }
            if (responseInfo.error == 0) {
                sendMessageResponse(rr.mResult, ret);
            }
            ril.processResponseDone(rr, responseInfo, ret);
        }
    }

    private void responseLastCallFailCauseInfo(RadioResponseInfo responseInfo, LastCallFailCauseInfo fcInfo) {
        RILRequest rr = this.mRil.processResponse(responseInfo);
        if (rr != null) {
            LastCallFailCause ret = new LastCallFailCause();
            ret.causeCode = fcInfo.causeCode;
            ret.vendorCause = fcInfo.vendorCause;
            if (responseInfo.error == 0) {
                sendMessageResponse(rr.mResult, ret);
            }
            this.mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    private void responseSignalStrength(RadioResponseInfo responseInfo, SignalStrength signalStrength) {
        RILRequest rr = this.mRil.processResponse(responseInfo);
        if (rr != null) {
            android.telephony.SignalStrength ret = RIL.convertHalSignalStrength(signalStrength);
            if (responseInfo.error == 0) {
                sendMessageResponse(rr.mResult, ret);
            }
            this.mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    private void responseSignalStrength_1_2(RadioResponseInfo responseInfo, android.hardware.radio.V1_2.SignalStrength signalStrength) {
        RILRequest rr = this.mRil.processResponse(responseInfo);
        if (rr != null) {
            android.telephony.SignalStrength ret = RIL.convertHalSignalStrength_1_2(signalStrength);
            if (responseInfo.error == 0) {
                sendMessageResponse(rr.mResult, ret);
            }
            this.mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    private void responseSms(RadioResponseInfo responseInfo, SendSmsResult sms) {
        RILRequest rr = this.mRil.processResponse(responseInfo);
        if (rr != null) {
            SmsResponse ret = new SmsResponse(sms.messageRef, sms.ackPDU, sms.errorCode);
            if (responseInfo.error == 0) {
                sendMessageResponse(rr.mResult, ret);
            }
            this.mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    private void responseSetupDataCall(RadioResponseInfo responseInfo, SetupDataCallResult setupDataCallResult) {
        RILRequest rr = this.mRil.processResponse(responseInfo);
        if (rr != null) {
            if (responseInfo.error == 0) {
                sendMessageResponse(rr.mResult, setupDataCallResult);
            }
            this.mRil.processResponseDone(rr, responseInfo, setupDataCallResult);
        }
    }

    private void responseIccIo(RadioResponseInfo responseInfo, IccIoResult result) {
        RILRequest rr = this.mRil.processResponse(responseInfo);
        if (rr != null) {
            com.android.internal.telephony.uicc.IccIoResult ret = new com.android.internal.telephony.uicc.IccIoResult(result.sw1, result.sw2, result.simResponse);
            if (responseInfo.error == 0) {
                sendMessageResponse(rr.mResult, ret);
            }
            this.mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    private void responseCallForwardInfo(RadioResponseInfo responseInfo, ArrayList<CallForwardInfo> callForwardInfos) {
        RILRequest rr = this.mRil.processResponse(responseInfo);
        if (rr != null) {
            CallForwardInfo[] ret = new CallForwardInfo[callForwardInfos.size()];
            int i = 0;
            while (i < callForwardInfos.size()) {
                ret[i] = new CallForwardInfo();
                ret[i].status = ((CallForwardInfo) callForwardInfos.get(i)).status;
                ret[i].reason = ((CallForwardInfo) callForwardInfos.get(i)).reason;
                ret[i].serviceClass = ((CallForwardInfo) callForwardInfos.get(i)).serviceClass;
                ret[i].toa = ((CallForwardInfo) callForwardInfos.get(i)).toa;
                ret[i].number = ((CallForwardInfo) callForwardInfos.get(i)).number;
                ret[i].timeSeconds = ((CallForwardInfo) callForwardInfos.get(i)).timeSeconds;
                if (1 == ret[i].status && ret[i].number == null) {
                    ret[i].number = "";
                    this.mRil.riljLog("number is null pointer, set number to a null string");
                }
                i++;
            }
            if (responseInfo.error == 0) {
                sendMessageResponse(rr.mResult, ret);
            }
            this.mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    private static String convertOpertatorInfoToString(int status) {
        if (status == 0) {
            return "unknown";
        }
        if (status == 1) {
            return "available";
        }
        if (status == 2) {
            return "current";
        }
        if (status == 3) {
            return "forbidden";
        }
        return "";
    }

    private void responseOperatorInfos(RadioResponseInfo responseInfo, ArrayList<OperatorInfo> networkInfos) {
        RILRequest rr = this.mRil.processResponse(responseInfo);
        if (rr != null) {
            ArrayList<OperatorInfo> ret = new ArrayList();
            for (int i = 0; i < networkInfos.size(); i++) {
                ret.add(new OperatorInfo(((OperatorInfo) networkInfos.get(i)).alphaLong, ((OperatorInfo) networkInfos.get(i)).alphaShort, ((OperatorInfo) networkInfos.get(i)).operatorNumeric, convertOpertatorInfoToString(((OperatorInfo) networkInfos.get(i)).status)));
            }
            if (responseInfo.error == 0) {
                sendMessageResponse(rr.mResult, ret);
            }
            this.mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    private void responseScanStatus(RadioResponseInfo responseInfo) {
        RILRequest rr = this.mRil.processResponse(responseInfo);
        if (rr != null) {
            NetworkScanResult nsr = null;
            if (responseInfo.error == 0) {
                nsr = new NetworkScanResult(1, 0, null);
                sendMessageResponse(rr.mResult, nsr);
            }
            this.mRil.processResponseDone(rr, responseInfo, nsr);
        }
    }

    private void responseDataCallList(RadioResponseInfo responseInfo, ArrayList<SetupDataCallResult> dataCallResultList) {
        RILRequest rr = this.mRil.processResponse(responseInfo);
        if (rr != null) {
            if (responseInfo.error == 0) {
                sendMessageResponse(rr.mResult, dataCallResultList);
            }
            this.mRil.processResponseDone(rr, responseInfo, dataCallResultList);
        }
    }

    private void responseCellList(RadioResponseInfo responseInfo, ArrayList<NeighboringCell> cells) {
        RILRequest rr = this.mRil.processResponse(responseInfo);
        if (rr != null) {
            ArrayList<NeighboringCellInfo> ret = new ArrayList();
            int i = 0;
            int radioType = ((TelephonyManager) this.mRil.mContext.getSystemService("phone")).getDataNetworkType(SubscriptionManager.getSubId(this.mRil.mPhoneId.intValue())[0]);
            if (radioType != 0) {
                while (i < cells.size()) {
                    ret.add(new NeighboringCellInfo(((NeighboringCell) cells.get(i)).rssi, ((NeighboringCell) cells.get(i)).cid, radioType));
                    i++;
                }
            }
            if (responseInfo.error == 0) {
                sendMessageResponse(rr.mResult, ret);
            }
            this.mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    private void responseGmsBroadcastConfig(RadioResponseInfo responseInfo, ArrayList<GsmBroadcastSmsConfigInfo> configs) {
        RILRequest rr = this.mRil.processResponse(responseInfo);
        if (rr != null) {
            ArrayList<SmsBroadcastConfigInfo> ret = new ArrayList();
            for (int i = 0; i < configs.size(); i++) {
                ret.add(new SmsBroadcastConfigInfo(((GsmBroadcastSmsConfigInfo) configs.get(i)).fromServiceId, ((GsmBroadcastSmsConfigInfo) configs.get(i)).toServiceId, ((GsmBroadcastSmsConfigInfo) configs.get(i)).fromCodeScheme, ((GsmBroadcastSmsConfigInfo) configs.get(i)).toCodeScheme, ((GsmBroadcastSmsConfigInfo) configs.get(i)).selected));
            }
            if (responseInfo.error == 0) {
                sendMessageResponse(rr.mResult, ret);
            }
            this.mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    private void responseCdmaBroadcastConfig(RadioResponseInfo responseInfo, ArrayList<CdmaBroadcastSmsConfigInfo> configs) {
        RILRequest rr = this.mRil.processResponse(responseInfo);
        if (rr != null) {
            int[] ret;
            int numServiceCategories = configs.size();
            int j = 0;
            if (numServiceCategories == 0) {
                ret = new int[94];
                ret[0] = 31;
                for (int i = 1; i < 94; i += 3) {
                    ret[i + 0] = i / 3;
                    ret[i + 1] = 1;
                    ret[i + 2] = 0;
                }
            } else {
                ret = new int[((numServiceCategories * 3) + 1)];
                ret[0] = numServiceCategories;
                int i2 = 1;
                while (j < configs.size()) {
                    ret[i2] = ((CdmaBroadcastSmsConfigInfo) configs.get(j)).serviceCategory;
                    ret[i2 + 1] = ((CdmaBroadcastSmsConfigInfo) configs.get(j)).language;
                    ret[i2 + 2] = ((CdmaBroadcastSmsConfigInfo) configs.get(j)).selected;
                    j++;
                    i2 += 3;
                }
            }
            if (responseInfo.error == 0) {
                sendMessageResponse(rr.mResult, ret);
            }
            this.mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    private void responseCellInfoList(RadioResponseInfo responseInfo, ArrayList<CellInfo> cellInfo) {
        RILRequest rr = this.mRil.processResponse(responseInfo);
        if (rr != null) {
            ArrayList<android.telephony.CellInfo> ret = RIL.convertHalCellInfoList(cellInfo);
            if (responseInfo.error == 0) {
                sendMessageResponse(rr.mResult, ret);
            }
            this.mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    private void responseCellInfoList_1_2(RadioResponseInfo responseInfo, ArrayList<android.hardware.radio.V1_2.CellInfo> cellInfo) {
        RILRequest rr = this.mRil.processResponse(responseInfo);
        if (rr != null) {
            ArrayList<android.telephony.CellInfo> ret = RIL.convertHalCellInfoList_1_2(cellInfo);
            if (responseInfo.error == 0) {
                sendMessageResponse(rr.mResult, ret);
            }
            this.mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    private void responseActivityData(RadioResponseInfo responseInfo, ActivityStatsInfo activityInfo) {
        RadioResponseInfo radioResponseInfo = responseInfo;
        ActivityStatsInfo activityStatsInfo = activityInfo;
        RILRequest rr = this.mRil.processResponse(radioResponseInfo);
        if (rr != null) {
            ModemActivityInfo ret;
            int i = 0;
            if (radioResponseInfo.error == 0) {
                int sleepModeTimeMs = activityStatsInfo.sleepModeTimeMs;
                int idleModeTimeMs = activityStatsInfo.idleModeTimeMs;
                int[] txModeTimeMs = new int[5];
                while (i < 5) {
                    txModeTimeMs[i] = activityStatsInfo.txmModetimeMs[i];
                    i++;
                }
                ret = new ModemActivityInfo(SystemClock.elapsedRealtime(), sleepModeTimeMs, idleModeTimeMs, txModeTimeMs, activityStatsInfo.rxModeTimeMs, 0);
            } else {
                ret = new ModemActivityInfo(0, 0, 0, new int[5], 0, 0);
                radioResponseInfo.error = 0;
            }
            sendMessageResponse(rr.mResult, ret);
            this.mRil.processResponseDone(rr, radioResponseInfo, ret);
        }
    }

    private void responseHardwareConfig(RadioResponseInfo responseInfo, ArrayList<HardwareConfig> config) {
        RILRequest rr = this.mRil.processResponse(responseInfo);
        if (rr != null) {
            ArrayList<HardwareConfig> ret = RIL.convertHalHwConfigList(config, this.mRil);
            if (responseInfo.error == 0) {
                sendMessageResponse(rr.mResult, ret);
            }
            this.mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    private void responseICC_IOBase64(RadioResponseInfo responseInfo, IccIoResult result) {
        RILRequest rr = this.mRil.processResponse(responseInfo);
        if (rr != null) {
            byte[] bArr;
            int i = result.sw1;
            int i2 = result.sw2;
            if (result.simResponse.equals("")) {
                bArr = (byte[]) null;
            } else {
                bArr = Base64.decode(result.simResponse, 0);
            }
            com.android.internal.telephony.uicc.IccIoResult ret = new com.android.internal.telephony.uicc.IccIoResult(i, i2, bArr);
            if (responseInfo.error == 0) {
                sendMessageResponse(rr.mResult, ret);
            }
            this.mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    private void responseRadioCapability(RadioResponseInfo responseInfo, RadioCapability rc) {
        RILRequest rr = this.mRil.processResponse(responseInfo);
        if (rr != null) {
            RadioCapability ret = RIL.convertHalRadioCapability(rc, this.mRil);
            if (responseInfo.error == 0) {
                sendMessageResponse(rr.mResult, ret);
            }
            this.mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    private void responseLceStatus(RadioResponseInfo responseInfo, LceStatusInfo statusInfo) {
        RILRequest rr = this.mRil.processResponse(responseInfo);
        if (rr != null) {
            ArrayList<Integer> ret = new ArrayList();
            ret.add(Integer.valueOf(statusInfo.lceStatus));
            ret.add(Integer.valueOf(Byte.toUnsignedInt(statusInfo.actualIntervalMs)));
            if (responseInfo.error == 0) {
                sendMessageResponse(rr.mResult, ret);
            }
            this.mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    private void responseLceData(RadioResponseInfo responseInfo, LceDataInfo lceInfo) {
        RILRequest rr = this.mRil.processResponse(responseInfo);
        if (rr != null) {
            LinkCapacityEstimate ret = RIL.convertHalLceData(lceInfo, this.mRil);
            if (responseInfo.error == 0) {
                sendMessageResponse(rr.mResult, ret);
            }
            this.mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    private void responseCarrierIdentifiers(RadioResponseInfo responseInfo, boolean allAllowed, CarrierRestrictions carriers) {
        RadioResponseInfo radioResponseInfo = responseInfo;
        CarrierRestrictions carrierRestrictions = carriers;
        RILRequest rr = this.mRil.processResponse(radioResponseInfo);
        if (rr != null) {
            List<CarrierIdentifier> ret = new ArrayList();
            int i = 0;
            while (i < carrierRestrictions.allowedCarriers.size()) {
                String mcc = ((Carrier) carrierRestrictions.allowedCarriers.get(i)).mcc;
                String mnc = ((Carrier) carrierRestrictions.allowedCarriers.get(i)).mnc;
                String spn = null;
                String imsi = null;
                String gid1 = null;
                String gid2 = null;
                int matchType = ((Carrier) carrierRestrictions.allowedCarriers.get(i)).matchType;
                String matchData = ((Carrier) carrierRestrictions.allowedCarriers.get(i)).matchData;
                if (matchType == 1) {
                    spn = matchData;
                } else if (matchType == 2) {
                    imsi = matchData;
                } else if (matchType == 3) {
                    gid1 = matchData;
                } else if (matchType == 4) {
                    gid2 = matchData;
                }
                CarrierIdentifier carrierIdentifier = r7;
                CarrierIdentifier carrierIdentifier2 = new CarrierIdentifier(mcc, mnc, spn, imsi, gid1, gid2);
                ret.add(carrierIdentifier);
                i++;
                carrierRestrictions = carriers;
            }
            if (radioResponseInfo.error == 0) {
                sendMessageResponse(rr.mResult, ret);
            }
            this.mRil.processResponseDone(rr, radioResponseInfo, ret);
        }
    }
}
