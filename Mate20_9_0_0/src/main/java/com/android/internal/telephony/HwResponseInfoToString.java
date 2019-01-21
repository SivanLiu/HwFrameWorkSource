package com.android.internal.telephony;

import android.hardware.radio.V1_0.CellIdentity;
import android.hardware.radio.V1_0.CellIdentityCdma;
import android.hardware.radio.V1_0.CellInfoType;
import android.hardware.radio.V1_0.DataCallFailCause;
import android.hardware.radio.V1_0.DataRegStateResult;
import android.hardware.radio.V1_0.RegState;
import android.hardware.radio.V1_0.SetupDataCallResult;
import android.hardware.radio.V1_0.VoiceRegStateResult;
import android.telephony.Rlog;
import com.android.internal.telephony.fullnetwork.HwFullNetworkConstants;
import java.util.ArrayList;

public class HwResponseInfoToString {
    private static final String LOG_TAG = "HwResponseInfoToString";
    private static HwResponseInfoToString mInstance = new HwResponseInfoToString();

    public static HwResponseInfoToString getDefault() {
        if (mInstance != null) {
            return mInstance;
        }
        Rlog.d(LOG_TAG, "mInstance has not init.");
        return new HwResponseInfoToString();
    }

    public String retToStringEx(int req, Object ret) {
        if (req == 27 && (ret instanceof SetupDataCallResult)) {
            return dataCallResultToString((SetupDataCallResult) ret);
        }
        if (req == HwFullNetworkConstants.EVENT_VOICE_CALL_ENDED && (ret instanceof ArrayList)) {
            ArrayList<Object> results = (ArrayList) ret;
            int size = results.size();
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < size; i++) {
                Object o = results.get(i);
                if (o instanceof SetupDataCallResult) {
                    sb.append(dataCallResultToString((SetupDataCallResult) o));
                } else {
                    sb.append(o.toString());
                }
            }
            sb.append("]");
            return sb.toString();
        } else if (req == 20 && (ret instanceof VoiceRegStateResult)) {
            return voiceRegStateResultToString((VoiceRegStateResult) ret);
        } else {
            if (req == 21 && (ret instanceof DataRegStateResult)) {
                return dataRegStateResultToString((DataRegStateResult) ret);
            }
            return ret.toString();
        }
    }

    private static String voiceRegStateResultToString(VoiceRegStateResult result) {
        StringBuilder sb = new StringBuilder("");
        sb.append("{");
        sb.append(".regState = ");
        sb.append(RegState.toString(result.regState));
        sb.append(", .rat = ");
        sb.append(result.rat);
        sb.append(", .cssSupported = ");
        sb.append(result.cssSupported);
        sb.append(", .roamingIndicator = ");
        sb.append(result.roamingIndicator);
        sb.append(", .systemIsInPrl = ");
        sb.append(result.systemIsInPrl);
        sb.append(", .defaultRoamingIndicator = ");
        sb.append(result.defaultRoamingIndicator);
        sb.append(", .reasonForDenial = ");
        sb.append(result.reasonForDenial);
        sb.append(", .cellIdentity = ");
        sb.append(cellIdentityToString(result.cellIdentity));
        sb.append("}");
        return sb.toString();
    }

    public final String dataRegStateResultToString(DataRegStateResult result) {
        StringBuilder builder = new StringBuilder("");
        builder.append("{");
        builder.append(".regState = ");
        builder.append(RegState.toString(result.regState));
        builder.append(", .rat = ");
        builder.append(result.rat);
        builder.append(", .reasonDataDenied = ");
        builder.append(result.reasonDataDenied);
        builder.append(", .maxDataCalls = ");
        builder.append(result.maxDataCalls);
        builder.append(", .cellIdentity = ");
        builder.append(cellIdentityToString(result.cellIdentity));
        builder.append("}");
        return builder.toString();
    }

    private static String cellIdentityToString(CellIdentity cellIdentity) {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        builder.append(".cellInfoType = ");
        builder.append(CellInfoType.toString(cellIdentity.cellInfoType));
        builder.append(", .cellIdentityGsm = ");
        builder.append(cellIdentity.cellIdentityGsm);
        builder.append(", .cellIdentityWcdma = ");
        builder.append(cellIdentity.cellIdentityWcdma);
        builder.append(", .cellIdentityCdma = ");
        builder.append(processCellIdentityCdmaArray(cellIdentity.cellIdentityCdma));
        builder.append(", .cellIdentityLte = ");
        builder.append(cellIdentity.cellIdentityLte);
        builder.append(", .cellIdentityTdscdma = ");
        builder.append(cellIdentity.cellIdentityTdscdma);
        builder.append("}");
        return builder.toString();
    }

    private static String processCellIdentityCdmaArray(ArrayList<CellIdentityCdma> cellIdentityCdma) {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        int size = cellIdentityCdma.size();
        for (int i = 0; i < size; i++) {
            CellIdentityCdma cdmaCellIdentity = (CellIdentityCdma) cellIdentityCdma.get(i);
            if (cdmaCellIdentity != null && (cdmaCellIdentity instanceof CellIdentityCdma)) {
                builder.append(cellIdentityCdmaToString(cdmaCellIdentity));
            }
        }
        builder.append("}");
        return builder.toString();
    }

    private static String cellIdentityCdmaToString(CellIdentityCdma cellIdentityCdma) {
        StringBuilder builder = new StringBuilder();
        builder.append(".networkId = ");
        builder.append(cellIdentityCdma.networkId);
        builder.append(", .systemId = ");
        builder.append(cellIdentityCdma.systemId);
        builder.append(", .baseStationId = ");
        builder.append(cellIdentityCdma.baseStationId);
        builder.append(", .longitude = ");
        builder.append("***");
        builder.append(", .latitude = ");
        builder.append("***");
        return builder.toString();
    }

    private static String dataCallResultToString(SetupDataCallResult result) {
        StringBuilder sb = new StringBuilder("");
        sb.append("{");
        sb.append(".status = ");
        sb.append(DataCallFailCause.toString(result.status));
        sb.append(", .suggestedRetryTime = ");
        sb.append(result.suggestedRetryTime);
        sb.append(", .cid = ");
        sb.append(result.cid);
        sb.append(", .active = ");
        sb.append(result.active);
        sb.append(", .type = ");
        sb.append(result.type);
        sb.append(", .ifname = ");
        sb.append(result.ifname);
        sb.append(", .addresses = *");
        sb.append(", .dnses = ");
        sb.append(result.dnses);
        sb.append(", .gateways = *");
        sb.append(", .pcscf = ");
        sb.append(result.pcscf);
        sb.append(", .mtu = ");
        sb.append(result.mtu);
        sb.append("}");
        return sb.toString();
    }

    protected void log(String s) {
        Rlog.d(LOG_TAG, s);
    }
}
