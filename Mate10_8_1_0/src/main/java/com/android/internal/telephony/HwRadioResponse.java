package com.android.internal.telephony;

import android.hardware.radio.V1_0.CellInfo;
import android.hardware.radio.V1_0.RadioResponseInfo;
import android.hardware.radio.V1_0.SetupDataCallResult;
import android.hardware.radio.V1_0.UusInfo;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import android.telephony.UiccAuthResponse;
import android.telephony.UiccAuthResponse.UiccAuthResponseData;
import android.text.TextUtils;
import com.android.internal.telephony.uicc.HwIccUtils;
import com.android.internal.telephony.uicc.IccUtils;
import java.util.ArrayList;
import java.util.Collections;
import vendor.huawei.hardware.radio.V1_0.CsgNetworkInfo;
import vendor.huawei.hardware.radio.V1_0.RILDeviceVersionResponse;
import vendor.huawei.hardware.radio.V1_0.RILDsFlowInfoResponse;
import vendor.huawei.hardware.radio.V1_0.RILImsCall;
import vendor.huawei.hardware.radio.V1_0.RILPreferredPLMNSelector;
import vendor.huawei.hardware.radio.V1_0.RILRADIOSYSINFO;
import vendor.huawei.hardware.radio.V1_0.RILUICCAUTHAUTSTYPE;
import vendor.huawei.hardware.radio.V1_0.RILUICCAUTHCKTYPE;
import vendor.huawei.hardware.radio.V1_0.RILUICCAUTHIKTYPE;
import vendor.huawei.hardware.radio.V1_0.RILUICCAUTHRESPCHALLENGETYPE;
import vendor.huawei.hardware.radio.V1_0.RILUICCAUTHRESPONSE;
import vendor.huawei.hardware.radio.V1_0.RILUICCAUTHRESTYPE;
import vendor.huawei.hardware.radio.V1_0.RspMsgPayload;
import vendor.huawei.hardware.radio.V1_1.RILImsCallEx;
import vendor.huawei.hardware.radio.V1_2.Call_V1_2;
import vendor.huawei.hardware.radio.V1_2.CsgNetworkInfo_1_1;
import vendor.huawei.hardware.radio.V1_2.RILImsCallV1_2;

public class HwRadioResponse extends RadioResponse {
    private static final int DEVICE_VERSION_LENGTH = 11;
    private static final int SYSTEM_INFO_EX_LENGTH = 7;
    private static final int TRAFFIC_DATA_LENGTH = 6;
    private int mPhoneId;
    RIL mRil;
    private String mTag = null;

    public HwRadioResponse(RIL ril) {
        super(ril);
        this.mRil = ril;
        this.mPhoneId = 0;
        this.mTag = "HwRadioResponse" + this.mPhoneId;
    }

    public void acknowledgeRequest(int serial) {
        this.mRil.processRequestAck(serial);
    }

    public void processRadioResponse(RadioResponseInfo responseInfo, Object ret) {
        RILRequest rr = this.mRil.processResponse(responseInfo);
        if (rr != null) {
            if (responseInfo.error == 0) {
                sendMessageResponse(rr.mResult, ret);
            }
            this.mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    public void RspMsg(RadioResponseInfo responseInfo, int msgId, RspMsgPayload payload) {
        Object ret = null;
        if (payload == null) {
            Rlog.d(this.mTag, "got null payload msgId = " + msgId);
            return;
        }
        switch (msgId) {
            case 158:
            case 162:
            case 175:
            case 177:
            case 178:
            case 182:
            case 188:
            case 189:
            case 194:
            case 196:
            case 199:
            case 205:
            case 243:
            case 245:
            case 267:
            case 271:
            case 276:
            case 285:
            case 287:
            case 289:
            case 291:
            case 295:
            case 296:
            case 297:
            case 301:
            case 302:
            case 307:
            case 337:
                break;
            case 168:
                ret = processInts(payload);
                break;
            case 169:
                ret = processString(payload);
                break;
            case 173:
                ret = processInts(payload);
                break;
            case 174:
                ret = processStrings(payload);
                break;
            case 176:
                ret = processInts(payload);
                break;
            case 192:
                ret = processInts(payload);
                break;
            case 206:
                ret = processInts(payload);
                break;
            case 209:
                ret = processString(payload);
                break;
            case 214:
                ret = processInts(payload);
                break;
            case 215:
                ret = processInts(payload);
                break;
            case 219:
                ret = processString(payload);
                break;
            case 241:
                ret = processInts(payload);
                break;
            case 242:
                ret = responseNetworkInfoWithActs(payload);
                break;
            case 252:
                ret = responseICCID(payload.strData);
                break;
            case 264:
                ret = processInts(payload);
                break;
            case 265:
                ret = processInts(payload);
                break;
            case 286:
                ret = processInts(payload);
                break;
            case 288:
                ret = processInts(payload);
                break;
            case 292:
                ret = processInts(payload);
                break;
            case 303:
                ret = processInts(payload);
                break;
            case 304:
                ret = processInts(payload);
                break;
            case 306:
                ret = processStrings(payload);
                break;
            case 308:
                ret = processInts(payload);
                break;
            case 328:
                ret = processInts(payload);
                break;
            case 330:
                ret = processInts(payload);
                break;
            case 342:
                ret = processInts(payload);
                break;
            default:
                extendRspMsg(responseInfo, msgId, payload, true);
                return;
        }
        if (1 != null) {
            if (responseInfo.error != 0) {
                ret = null;
            }
            processRadioResponse(responseInfo, ret);
        }
    }

    public void extendRspMsg(RadioResponseInfo responseInfo, int msgId, RspMsgPayload payload, boolean validMsgId) {
        Object ret = null;
        switch (msgId) {
            case 298:
            case 311:
            case 315:
            case 343:
            case 345:
            case 352:
            case 353:
            case 356:
            case 358:
                break;
            case 331:
                ret = HwRadioIndication.convertHwHalSignalStrength((int[]) processInts(payload));
                break;
            case 344:
                ret = processInts(payload);
                break;
            default:
                validMsgId = false;
                this.mRil.processResponseDone(this.mRil.processResponse(responseInfo), responseInfo, null);
                Rlog.d(this.mTag, "got invalid msgId = " + msgId);
                break;
        }
        if (validMsgId) {
            if (responseInfo.error != 0) {
                ret = null;
            }
            processRadioResponse(responseInfo, ret);
        }
    }

    public void uiccAuthResponse(RadioResponseInfo responseInfo, RILUICCAUTHRESPONSE uiccAuthRst) {
        responseUiccAuth(responseInfo, uiccAuthRst);
    }

    private void responseUiccAuth(RadioResponseInfo responseInfo, RILUICCAUTHRESPONSE uiccAuthRst) {
        Rlog.d(this.mTag, "Response of RIL_REQUEST_HW_VOWIFI_UICC_AUTH");
        RILRequest rr = this.mRil.processResponse(responseInfo);
        if (rr != null) {
            Object ret = null;
            if (responseInfo.error == 0) {
                Rlog.d(this.mTag, "NO ERROR,start to process GbaAuth");
                UiccAuthResponse uiccResponse = new UiccAuthResponse();
                uiccResponse.mResult = uiccAuthRst.authStatus;
                Rlog.d(this.mTag, "responseUiccAuth, mStatus=" + uiccResponse.mResult);
                if (uiccResponse.mResult == 0) {
                    RILUICCAUTHRESPCHALLENGETYPE authChang = uiccAuthRst.authChallenge;
                    RILUICCAUTHRESTYPE resDatas = authChang.resData;
                    uiccResponse.mUiccAuthChallenge.mResData = new UiccAuthResponseData();
                    uiccResponse.mUiccAuthChallenge.mResData.present = resDatas.resPresent;
                    uiccResponse.mUiccAuthChallenge.mResData.data = IccUtils.hexStringToBytes(resDatas.res);
                    uiccResponse.mUiccAuthChallenge.mResData.len = uiccResponse.mUiccAuthChallenge.mResData.data.length;
                    RILUICCAUTHIKTYPE ikType = authChang.ikData;
                    uiccResponse.mUiccAuthChallenge.mIkData = new UiccAuthResponseData();
                    uiccResponse.mUiccAuthChallenge.mIkData.present = ikType.ikPresent;
                    uiccResponse.mUiccAuthChallenge.mIkData.data = IccUtils.hexStringToBytes(ikType.ik);
                    uiccResponse.mUiccAuthChallenge.mIkData.len = uiccResponse.mUiccAuthChallenge.mIkData.data.length;
                    RILUICCAUTHCKTYPE ckDatas = authChang.ckData;
                    uiccResponse.mUiccAuthChallenge.mCkData = new UiccAuthResponseData();
                    uiccResponse.mUiccAuthChallenge.mCkData.present = ckDatas.ckPresent;
                    uiccResponse.mUiccAuthChallenge.mCkData.data = IccUtils.hexStringToBytes(ckDatas.ck);
                    uiccResponse.mUiccAuthChallenge.mCkData.len = uiccResponse.mUiccAuthChallenge.mCkData.data.length;
                } else {
                    RILUICCAUTHAUTSTYPE autsDatas = uiccAuthRst.authSyncfail.autsData;
                    uiccResponse.mUiccAuthSyncFail.present = autsDatas.autsPresent;
                    uiccResponse.mUiccAuthSyncFail.data = IccUtils.hexStringToBytes(autsDatas.auts);
                    uiccResponse.mUiccAuthSyncFail.len = uiccResponse.mUiccAuthSyncFail.data.length;
                }
                sendMessageResponse(rr.mResult, uiccResponse);
                ret = uiccResponse;
            }
            this.mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    private Object responseICCID(String response) {
        return HwIccUtils.hexStringToBcd(response);
    }

    private Object processInts(RspMsgPayload payload) {
        int numInts = payload.nDatas.size();
        int[] response = new int[numInts];
        for (int i = 0; i < numInts; i++) {
            response[i] = ((Integer) payload.nDatas.get(i)).intValue();
        }
        return response;
    }

    private Object processStrings(RspMsgPayload payload) {
        int numStrings = payload.strDatas.size();
        String[] response = new String[numStrings];
        for (int i = 0; i < numStrings; i++) {
            response[i] = (String) payload.strDatas.get(i);
        }
        return response;
    }

    private Object processString(RspMsgPayload payload) {
        return payload.strData;
    }

    public void getDeviceVersionResponse(RadioResponseInfo responseInfo, RILDeviceVersionResponse deviceVersion) {
        if (deviceVersion == null) {
            Rlog.d(this.mTag, "getDeviceVersionResponse: deviceVersion is null.");
            return;
        }
        processRadioResponse(responseInfo, new String[]{deviceVersion.buildTime, deviceVersion.externalSwVersion, deviceVersion.internalSwVersion, deviceVersion.externalDbVersion, deviceVersion.internalDbVersion, deviceVersion.externalHwVersion, deviceVersion.internalHwVersion, deviceVersion.externalDutName, deviceVersion.internalDutName, deviceVersion.configurateVersion, deviceVersion.prlVersion});
    }

    public void getDsFlowInfoResponse(RadioResponseInfo responseInfo, RILDsFlowInfoResponse dsFlowInfo) {
        if (dsFlowInfo == null) {
            Rlog.d(this.mTag, "getDsFlowInfoResponse: dsFlowInfo is null.");
            return;
        }
        processRadioResponse(responseInfo, new String[]{dsFlowInfo.lastDsTime, dsFlowInfo.lastTxFlow, dsFlowInfo.lastRxFlow, dsFlowInfo.totalDsTime, dsFlowInfo.totalTxFlow, dsFlowInfo.totalRxFlow});
    }

    public void getPolListResponse(RadioResponseInfo responseInfo, RILPreferredPLMNSelector preferredplmnselector) {
    }

    public void getSystemInfoExResponse(RadioResponseInfo responseInfo, RILRADIOSYSINFO sysInfo) {
        if (sysInfo == null) {
            Rlog.d(this.mTag, "getSystemInfoExResponse: sysInfo is null.");
            return;
        }
        processRadioResponse(responseInfo, new int[]{sysInfo.sysSubmode, sysInfo.srvStatus, sysInfo.srvDomain, sysInfo.roamStatus, sysInfo.sysMode, sysInfo.simState, sysInfo.lockState});
    }

    private Object responseNetworkInfoWithActs(RspMsgPayload payload) {
        int numInts = payload.nDatas.size();
        int[] response = new int[(numInts * 6)];
        for (int i = 0; i < numInts * 6; i++) {
            response[i] = ((Integer) payload.nDatas.get(i)).intValue();
        }
        return response;
    }

    public void getCurrentImsCallsResponse(RadioResponseInfo responseInfo, ArrayList<RILImsCall> arrayList) {
    }

    public void getAvailableCsgIdsResponse(RadioResponseInfo responseInfo, ArrayList<CsgNetworkInfo> csgNetworkInfos) {
        responseCsgNetworkInfos(responseInfo, csgNetworkInfos);
    }

    public void manualSelectionCsgIdResponse(RadioResponseInfo responseInfo) {
        processRadioResponse(responseInfo, null);
    }

    private void responseCsgNetworkInfos(RadioResponseInfo responseInfo, ArrayList<CsgNetworkInfo> csgNetworkInfos) {
        RILRequest rr = this.mRil.processResponse(responseInfo);
        Rlog.d(this.mTag, "Response of AvailableCsgIds rr: " + rr + "error: " + responseInfo.error + " csgNetworkInfos: " + csgNetworkInfos.size());
        if (rr != null) {
            ArrayList<HwHisiCsgNetworkInfo> ret = new ArrayList();
            int csgNetworkInfoSize = csgNetworkInfos.size();
            for (int i = 0; i < csgNetworkInfoSize; i++) {
                ret.add(new HwHisiCsgNetworkInfo(((CsgNetworkInfo) csgNetworkInfos.get(i)).plmn, ((CsgNetworkInfo) csgNetworkInfos.get(i)).csgId, ((CsgNetworkInfo) csgNetworkInfos.get(i)).networkRat));
            }
            if (responseInfo.error == 0) {
                sendMessageResponse(rr.mResult, ret);
            }
            this.mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    public void getAvailableCsgIdsResponse_1_1(RadioResponseInfo responseInfo, ArrayList<CsgNetworkInfo_1_1> csgNetworkInfos) {
        Rlog.d(this.mTag, "csg...getAvailableCsgIdsResponse_1_1");
        responseExtendersCsgNetworkInfos(responseInfo, csgNetworkInfos);
    }

    private void responseExtendersCsgNetworkInfos(RadioResponseInfo responseInfo, ArrayList<CsgNetworkInfo_1_1> csgNetworkInfos) {
        RILRequest rr = this.mRil.processResponse(responseInfo);
        Rlog.d(this.mTag, "Response of AvailableCsgIds rr: " + rr + "error: " + responseInfo.error + " csgNetworkInfos: " + csgNetworkInfos.size());
        if (rr != null) {
            ArrayList<HwHisiCsgNetworkInfo> ret = new ArrayList();
            int csgNetworkInfoSize = csgNetworkInfos.size();
            for (int i = 0; i < csgNetworkInfoSize; i++) {
                ret.add(getmHwHisiCsgNetworkInfo((CsgNetworkInfo_1_1) csgNetworkInfos.get(i)));
            }
            if (responseInfo.error == 0) {
                sendMessageResponse(rr.mResult, ret);
            }
            this.mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    private HwHisiCsgNetworkInfo getmHwHisiCsgNetworkInfo(CsgNetworkInfo_1_1 csginfo) {
        return new HwHisiCsgNetworkInfo(csginfo.plmn, csginfo.csgId, csginfo.networkRat, csginfo.csgId_type, csginfo.csgId_name, csginfo.longName, csginfo.shortName, csginfo.rsrp, csginfo.rsrq, csginfo.isConnected, converInfoToString(csginfo.csgType));
    }

    public void getCurrentImsCallsWithImsDomainResponse(RadioResponseInfo responseInfo, ArrayList<RILImsCallEx> arrayList) {
    }

    private static String converInfoToString(int state) {
        if (state == 1) {
            return "allow_list";
        }
        if (state == 2) {
            return "operator_list";
        }
        if (state == 3) {
            return "forbiden_list";
        }
        if (state == 4) {
            return "unallow_list";
        }
        return "";
    }

    public void getCellInfoListOtdoaResponse(RadioResponseInfo responseInfo, ArrayList<CellInfo> arrayList) {
    }

    public void deactivateDataCallEmergencyResponse(RadioResponseInfo responseInfo) {
    }

    public void setupDataCallEmergencyResponse(RadioResponseInfo responseInfo, SetupDataCallResult setupDataCallResult) {
    }

    public void getCurrentCallsResponseV1_2(RadioResponseInfo responseInfo, ArrayList<Call_V1_2> calls) {
        responseCurrentCallsEx(responseInfo, calls);
    }

    private void responseCurrentCallsEx(RadioResponseInfo responseInfo, ArrayList<Call_V1_2> calls) {
        RILRequest rr = this.mRil.processResponse(responseInfo);
        if (rr != null) {
            int num = calls.size();
            ArrayList<DriverCall> dcCalls = new ArrayList(num);
            for (int i = 0; i < num; i++) {
                DriverCall dc = new DriverCall();
                dc.state = DriverCall.stateFromCLCC(((Call_V1_2) calls.get(i)).call.state);
                dc.index = ((Call_V1_2) calls.get(i)).call.index;
                dc.TOA = ((Call_V1_2) calls.get(i)).call.toa;
                dc.isMpty = ((Call_V1_2) calls.get(i)).call.isMpty;
                dc.isMT = ((Call_V1_2) calls.get(i)).call.isMT;
                dc.als = ((Call_V1_2) calls.get(i)).call.als;
                dc.isVoice = ((Call_V1_2) calls.get(i)).call.isVoice;
                dc.isVoicePrivacy = ((Call_V1_2) calls.get(i)).call.isVoicePrivacy;
                dc.number = ((Call_V1_2) calls.get(i)).call.number;
                dc.numberPresentation = DriverCall.presentationFromCLIP(((Call_V1_2) calls.get(i)).call.numberPresentation);
                dc.name = ((Call_V1_2) calls.get(i)).call.name;
                dc.namePresentation = DriverCall.presentationFromCLIP(((Call_V1_2) calls.get(i)).call.namePresentation);
                if (((Call_V1_2) calls.get(i)).call.uusInfo.size() == 1) {
                    dc.uusInfo = new UUSInfo();
                    dc.uusInfo.setType(((UusInfo) ((Call_V1_2) calls.get(i)).call.uusInfo.get(0)).uusType);
                    dc.uusInfo.setDcs(((UusInfo) ((Call_V1_2) calls.get(i)).call.uusInfo.get(0)).uusDcs);
                    if (TextUtils.isEmpty(((UusInfo) ((Call_V1_2) calls.get(i)).call.uusInfo.get(0)).uusData)) {
                        this.mRil.riljLog("responseCurrentCallsEx: uusInfo data is null or empty");
                    } else {
                        dc.uusInfo.setUserData(((UusInfo) ((Call_V1_2) calls.get(i)).call.uusInfo.get(0)).uusData.getBytes());
                    }
                    this.mRil.riljLogv(String.format("Incoming UUS : type=%d, dcs=%d, length=%d", new Object[]{Integer.valueOf(dc.uusInfo.getType()), Integer.valueOf(dc.uusInfo.getDcs()), Integer.valueOf(dc.uusInfo.getUserData().length)}));
                    this.mRil.riljLogv("Incoming UUS : data (hex): " + IccUtils.bytesToHexString(dc.uusInfo.getUserData()));
                } else {
                    this.mRil.riljLogv("Incoming UUS : NOT present!");
                }
                dc.number = PhoneNumberUtils.stringFromStringAndTOA(dc.number, dc.TOA);
                dc.redirectNumber = ((Call_V1_2) calls.get(i)).redirectNumber;
                dc.redirectNumberTOA = ((Call_V1_2) calls.get(i)).redirectNumberToa;
                dc.redirectNumberPresentation = DriverCall.presentationFromCLIP(((Call_V1_2) calls.get(i)).redirectNumberPresentation);
                dc.redirectNumber = PhoneNumberUtils.stringFromStringAndTOA(dc.redirectNumber, dc.redirectNumberTOA);
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

    public void getCurrentImsCallsResponseV1_2(RadioResponseInfo responseInfo, ArrayList<RILImsCallV1_2> arrayList) {
    }
}
