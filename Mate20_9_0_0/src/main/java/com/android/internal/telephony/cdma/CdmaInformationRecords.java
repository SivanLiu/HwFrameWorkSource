package com.android.internal.telephony.cdma;

import android.os.Parcel;

public final class CdmaInformationRecords {
    public static final int RIL_CDMA_CALLED_PARTY_NUMBER_INFO_REC = 1;
    public static final int RIL_CDMA_CALLING_PARTY_NUMBER_INFO_REC = 2;
    public static final int RIL_CDMA_CONNECTED_NUMBER_INFO_REC = 3;
    public static final int RIL_CDMA_DISPLAY_INFO_REC = 0;
    public static final int RIL_CDMA_EXTENDED_DISPLAY_INFO_REC = 7;
    public static final int RIL_CDMA_LINE_CONTROL_INFO_REC = 6;
    public static final int RIL_CDMA_REDIRECTING_NUMBER_INFO_REC = 5;
    public static final int RIL_CDMA_SIGNAL_INFO_REC = 4;
    public static final int RIL_CDMA_T53_AUDIO_CONTROL_INFO_REC = 10;
    public static final int RIL_CDMA_T53_CLIR_INFO_REC = 8;
    public static final int RIL_CDMA_T53_RELEASE_INFO_REC = 9;
    public Object record;

    public static class CdmaDisplayInfoRec {
        public String alpha;
        public int id;

        public CdmaDisplayInfoRec(int id, String alpha) {
            this.id = id;
            this.alpha = alpha;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("CdmaDisplayInfoRec: { id: ");
            stringBuilder.append(CdmaInformationRecords.idToString(this.id));
            stringBuilder.append(", alpha: ");
            stringBuilder.append(this.alpha);
            stringBuilder.append(" }");
            return stringBuilder.toString();
        }
    }

    public static class CdmaLineControlInfoRec {
        public byte lineCtrlPolarityIncluded;
        public byte lineCtrlPowerDenial;
        public byte lineCtrlReverse;
        public byte lineCtrlToggle;

        public CdmaLineControlInfoRec(int lineCtrlPolarityIncluded, int lineCtrlToggle, int lineCtrlReverse, int lineCtrlPowerDenial) {
            this.lineCtrlPolarityIncluded = (byte) lineCtrlPolarityIncluded;
            this.lineCtrlToggle = (byte) lineCtrlToggle;
            this.lineCtrlReverse = (byte) lineCtrlReverse;
            this.lineCtrlPowerDenial = (byte) lineCtrlPowerDenial;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("CdmaLineControlInfoRec: { lineCtrlPolarityIncluded: ");
            stringBuilder.append(this.lineCtrlPolarityIncluded);
            stringBuilder.append(" lineCtrlToggle: ");
            stringBuilder.append(this.lineCtrlToggle);
            stringBuilder.append(" lineCtrlReverse: ");
            stringBuilder.append(this.lineCtrlReverse);
            stringBuilder.append(" lineCtrlPowerDenial: ");
            stringBuilder.append(this.lineCtrlPowerDenial);
            stringBuilder.append(" }");
            return stringBuilder.toString();
        }
    }

    public static class CdmaNumberInfoRec {
        public int id;
        public String number;
        public byte numberPlan;
        public byte numberType;
        public byte pi;
        public byte si;

        public CdmaNumberInfoRec(int id, String number, int numberType, int numberPlan, int pi, int si) {
            this.number = number;
            this.numberType = (byte) numberType;
            this.numberPlan = (byte) numberPlan;
            this.pi = (byte) pi;
            this.si = (byte) si;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("CdmaNumberInfoRec: { id: ");
            stringBuilder.append(CdmaInformationRecords.idToString(this.id));
            stringBuilder.append(", number: <MASKED>, numberType: ");
            stringBuilder.append(this.numberType);
            stringBuilder.append(", numberPlan: ");
            stringBuilder.append(this.numberPlan);
            stringBuilder.append(", pi: ");
            stringBuilder.append(this.pi);
            stringBuilder.append(", si: ");
            stringBuilder.append(this.si);
            stringBuilder.append(" }");
            return stringBuilder.toString();
        }
    }

    public static class CdmaRedirectingNumberInfoRec {
        public static final int REASON_CALLED_DTE_OUT_OF_ORDER = 9;
        public static final int REASON_CALL_FORWARDING_BUSY = 1;
        public static final int REASON_CALL_FORWARDING_BY_THE_CALLED_DTE = 10;
        public static final int REASON_CALL_FORWARDING_NO_REPLY = 2;
        public static final int REASON_CALL_FORWARDING_UNCONDITIONAL = 15;
        public static final int REASON_UNKNOWN = 0;
        public CdmaNumberInfoRec numberInfoRec;
        public int redirectingReason;

        public CdmaRedirectingNumberInfoRec(String number, int numberType, int numberPlan, int pi, int si, int reason) {
            this.numberInfoRec = new CdmaNumberInfoRec(5, number, numberType, numberPlan, pi, si);
            this.redirectingReason = reason;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("CdmaNumberInfoRec: { numberInfoRec: ");
            stringBuilder.append(this.numberInfoRec);
            stringBuilder.append(", redirectingReason: ");
            stringBuilder.append(this.redirectingReason);
            stringBuilder.append(" }");
            return stringBuilder.toString();
        }
    }

    public static class CdmaSignalInfoRec {
        public int alertPitch;
        public boolean isPresent;
        public int signal;
        public int signalType;

        public CdmaSignalInfoRec(int isPresent, int signalType, int alertPitch, int signal) {
            this.isPresent = isPresent != 0;
            this.signalType = signalType;
            this.alertPitch = alertPitch;
            this.signal = signal;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("CdmaSignalInfo: { isPresent: ");
            stringBuilder.append(this.isPresent);
            stringBuilder.append(", signalType: ");
            stringBuilder.append(this.signalType);
            stringBuilder.append(", alertPitch: ");
            stringBuilder.append(this.alertPitch);
            stringBuilder.append(", signal: ");
            stringBuilder.append(this.signal);
            stringBuilder.append(" }");
            return stringBuilder.toString();
        }
    }

    public static class CdmaT53AudioControlInfoRec {
        public byte downlink;
        public byte uplink;

        public CdmaT53AudioControlInfoRec(int uplink, int downlink) {
            this.uplink = (byte) uplink;
            this.downlink = (byte) downlink;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("CdmaT53AudioControlInfoRec: { uplink: ");
            stringBuilder.append(this.uplink);
            stringBuilder.append(" downlink: ");
            stringBuilder.append(this.downlink);
            stringBuilder.append(" }");
            return stringBuilder.toString();
        }
    }

    public static class CdmaT53ClirInfoRec {
        public byte cause;

        public CdmaT53ClirInfoRec(int cause) {
            this.cause = (byte) cause;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("CdmaT53ClirInfoRec: { cause: ");
            stringBuilder.append(this.cause);
            stringBuilder.append(" }");
            return stringBuilder.toString();
        }
    }

    public CdmaInformationRecords(CdmaDisplayInfoRec obj) {
        this.record = obj;
    }

    public CdmaInformationRecords(CdmaNumberInfoRec obj) {
        this.record = obj;
    }

    public CdmaInformationRecords(CdmaSignalInfoRec obj) {
        this.record = obj;
    }

    public CdmaInformationRecords(CdmaRedirectingNumberInfoRec obj) {
        this.record = obj;
    }

    public CdmaInformationRecords(CdmaLineControlInfoRec obj) {
        this.record = obj;
    }

    public CdmaInformationRecords(CdmaT53ClirInfoRec obj) {
        this.record = obj;
    }

    public CdmaInformationRecords(CdmaT53AudioControlInfoRec obj) {
        this.record = obj;
    }

    public CdmaInformationRecords(Parcel p) {
        int id = p.readInt();
        switch (id) {
            case 0:
            case 7:
                this.record = new CdmaDisplayInfoRec(id, p.readString());
                return;
            case 1:
            case 2:
            case 3:
                this.record = new CdmaNumberInfoRec(id, p.readString(), p.readInt(), p.readInt(), p.readInt(), p.readInt());
                return;
            case 4:
                this.record = new CdmaSignalInfoRec(p.readInt(), p.readInt(), p.readInt(), p.readInt());
                return;
            case 5:
                this.record = new CdmaRedirectingNumberInfoRec(p.readString(), p.readInt(), p.readInt(), p.readInt(), p.readInt(), p.readInt());
                return;
            case 6:
                this.record = new CdmaLineControlInfoRec(p.readInt(), p.readInt(), p.readInt(), p.readInt());
                return;
            case 8:
                this.record = new CdmaT53ClirInfoRec(p.readInt());
                return;
            case 10:
                this.record = new CdmaT53AudioControlInfoRec(p.readInt(), p.readInt());
                return;
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("RIL_UNSOL_CDMA_INFO_REC: unsupported record. Got ");
                stringBuilder.append(idToString(id));
                stringBuilder.append(" ");
                throw new RuntimeException(stringBuilder.toString());
        }
    }

    public static String idToString(int id) {
        switch (id) {
            case 0:
                return "RIL_CDMA_DISPLAY_INFO_REC";
            case 1:
                return "RIL_CDMA_CALLED_PARTY_NUMBER_INFO_REC";
            case 2:
                return "RIL_CDMA_CALLING_PARTY_NUMBER_INFO_REC";
            case 3:
                return "RIL_CDMA_CONNECTED_NUMBER_INFO_REC";
            case 4:
                return "RIL_CDMA_SIGNAL_INFO_REC";
            case 5:
                return "RIL_CDMA_REDIRECTING_NUMBER_INFO_REC";
            case 6:
                return "RIL_CDMA_LINE_CONTROL_INFO_REC";
            case 7:
                return "RIL_CDMA_EXTENDED_DISPLAY_INFO_REC";
            case 8:
                return "RIL_CDMA_T53_CLIR_INFO_REC";
            case 9:
                return "RIL_CDMA_T53_RELEASE_INFO_REC";
            case 10:
                return "RIL_CDMA_T53_AUDIO_CONTROL_INFO_REC";
            default:
                return "<unknown record>";
        }
    }
}
