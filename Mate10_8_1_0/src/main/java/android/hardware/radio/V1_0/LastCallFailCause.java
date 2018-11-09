package android.hardware.radio.V1_0;

public final class LastCallFailCause {
    public static final int ACCESS_CLASS_BLOCKED = 260;
    public static final int ACCESS_INFORMATION_DISCARDED = 43;
    public static final int ACM_LIMIT_EXCEEDED = 68;
    public static final int BEARER_CAPABILITY_NOT_AUTHORIZED = 57;
    public static final int BEARER_CAPABILITY_UNAVAILABLE = 58;
    public static final int BEARER_SERVICE_NOT_IMPLEMENTED = 65;
    public static final int BUSY = 17;
    public static final int CALL_BARRED = 240;
    public static final int CALL_REJECTED = 21;
    public static final int CDMA_ACCESS_BLOCKED = 1009;
    public static final int CDMA_ACCESS_FAILURE = 1006;
    public static final int CDMA_DROP = 1001;
    public static final int CDMA_INTERCEPT = 1002;
    public static final int CDMA_LOCKED_UNTIL_POWER_CYCLE = 1000;
    public static final int CDMA_NOT_EMERGENCY = 1008;
    public static final int CDMA_PREEMPTED = 1007;
    public static final int CDMA_REORDER = 1003;
    public static final int CDMA_RETRY_ORDER = 1005;
    public static final int CDMA_SO_REJECT = 1004;
    public static final int CHANNEL_UNACCEPTABLE = 6;
    public static final int CONDITIONAL_IE_ERROR = 100;
    public static final int CONGESTION = 34;
    public static final int DESTINATION_OUT_OF_ORDER = 27;
    public static final int DIAL_MODIFIED_TO_DIAL = 246;
    public static final int DIAL_MODIFIED_TO_SS = 245;
    public static final int DIAL_MODIFIED_TO_USSD = 244;
    public static final int ERROR_UNSPECIFIED = 65535;
    public static final int FACILITY_REJECTED = 29;
    public static final int FDN_BLOCKED = 241;
    public static final int IMEI_NOT_ACCEPTED = 243;
    public static final int IMSI_UNKNOWN_IN_VLR = 242;
    public static final int INCOMING_CALLS_BARRED_WITHIN_CUG = 55;
    public static final int INCOMPATIBLE_DESTINATION = 88;
    public static final int INFORMATION_ELEMENT_NON_EXISTENT = 99;
    public static final int INTERWORKING_UNSPECIFIED = 127;
    public static final int INVALID_MANDATORY_INFORMATION = 96;
    public static final int INVALID_NUMBER_FORMAT = 28;
    public static final int INVALID_TRANSACTION_IDENTIFIER = 81;
    public static final int INVALID_TRANSIT_NW_SELECTION = 91;
    public static final int MESSAGE_NOT_COMPATIBLE_WITH_PROTOCOL_STATE = 101;
    public static final int MESSAGE_TYPE_NON_IMPLEMENTED = 97;
    public static final int MESSAGE_TYPE_NOT_COMPATIBLE_WITH_PROTOCOL_STATE = 98;
    public static final int NETWORK_DETACH = 261;
    public static final int NETWORK_OUT_OF_ORDER = 38;
    public static final int NETWORK_REJECT = 252;
    public static final int NETWORK_RESP_TIMEOUT = 251;
    public static final int NORMAL = 16;
    public static final int NORMAL_UNSPECIFIED = 31;
    public static final int NO_ANSWER_FROM_USER = 19;
    public static final int NO_ROUTE_TO_DESTINATION = 3;
    public static final int NO_USER_RESPONDING = 18;
    public static final int NO_VALID_SIM = 249;
    public static final int NUMBER_CHANGED = 22;
    public static final int OEM_CAUSE_1 = 61441;
    public static final int OEM_CAUSE_10 = 61450;
    public static final int OEM_CAUSE_11 = 61451;
    public static final int OEM_CAUSE_12 = 61452;
    public static final int OEM_CAUSE_13 = 61453;
    public static final int OEM_CAUSE_14 = 61454;
    public static final int OEM_CAUSE_15 = 61455;
    public static final int OEM_CAUSE_2 = 61442;
    public static final int OEM_CAUSE_3 = 61443;
    public static final int OEM_CAUSE_4 = 61444;
    public static final int OEM_CAUSE_5 = 61445;
    public static final int OEM_CAUSE_6 = 61446;
    public static final int OEM_CAUSE_7 = 61447;
    public static final int OEM_CAUSE_8 = 61448;
    public static final int OEM_CAUSE_9 = 61449;
    public static final int ONLY_DIGITAL_INFORMATION_BEARER_AVAILABLE = 70;
    public static final int OPERATOR_DETERMINED_BARRING = 8;
    public static final int OUT_OF_SERVICE = 248;
    public static final int PREEMPTION = 25;
    public static final int PROTOCOL_ERROR_UNSPECIFIED = 111;
    public static final int QOS_UNAVAILABLE = 49;
    public static final int RADIO_ACCESS_FAILURE = 253;
    public static final int RADIO_INTERNAL_ERROR = 250;
    public static final int RADIO_LINK_FAILURE = 254;
    public static final int RADIO_LINK_LOST = 255;
    public static final int RADIO_OFF = 247;
    public static final int RADIO_RELEASE_ABNORMAL = 259;
    public static final int RADIO_RELEASE_NORMAL = 258;
    public static final int RADIO_SETUP_FAILURE = 257;
    public static final int RADIO_UPLINK_FAILURE = 256;
    public static final int RECOVERY_ON_TIMER_EXPIRED = 102;
    public static final int REQUESTED_CIRCUIT_OR_CHANNEL_NOT_AVAILABLE = 44;
    public static final int REQUESTED_FACILITY_NOT_IMPLEMENTED = 69;
    public static final int REQUESTED_FACILITY_NOT_SUBSCRIBED = 50;
    public static final int RESOURCES_UNAVAILABLE_OR_UNSPECIFIED = 47;
    public static final int RESP_TO_STATUS_ENQUIRY = 30;
    public static final int SEMANTICALLY_INCORRECT_MESSAGE = 95;
    public static final int SERVICE_OPTION_NOT_AVAILABLE = 63;
    public static final int SERVICE_OR_OPTION_NOT_IMPLEMENTED = 79;
    public static final int SWITCHING_EQUIPMENT_CONGESTION = 42;
    public static final int TEMPORARY_FAILURE = 41;
    public static final int UNOBTAINABLE_NUMBER = 1;
    public static final int USER_NOT_MEMBER_OF_CUG = 87;

    public static final java.lang.String dumpBitfield(int r1) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: android.hardware.radio.V1_0.LastCallFailCause.dumpBitfield(int):java.lang.String
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:116)
	at jadx.core.dex.nodes.ClassNode.load(ClassNode.java:249)
	at jadx.core.ProcessClass.process(ProcessClass.java:31)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
Caused by: jadx.core.utils.exceptions.DecodeException: Unknown instruction: not-int
	at jadx.core.dex.instructions.InsnDecoder.decode(InsnDecoder.java:568)
	at jadx.core.dex.instructions.InsnDecoder.process(InsnDecoder.java:56)
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:102)
	... 5 more
*/
        /*
        // Can't load method instructions.
        */
        throw new UnsupportedOperationException("Method not decompiled: android.hardware.radio.V1_0.LastCallFailCause.dumpBitfield(int):java.lang.String");
    }

    public static final String toString(int o) {
        if (o == 1) {
            return "UNOBTAINABLE_NUMBER";
        }
        if (o == 3) {
            return "NO_ROUTE_TO_DESTINATION";
        }
        if (o == 6) {
            return "CHANNEL_UNACCEPTABLE";
        }
        if (o == 8) {
            return "OPERATOR_DETERMINED_BARRING";
        }
        if (o == 16) {
            return "NORMAL";
        }
        if (o == 17) {
            return "BUSY";
        }
        if (o == 18) {
            return "NO_USER_RESPONDING";
        }
        if (o == 19) {
            return "NO_ANSWER_FROM_USER";
        }
        if (o == 21) {
            return "CALL_REJECTED";
        }
        if (o == 22) {
            return "NUMBER_CHANGED";
        }
        if (o == 25) {
            return "PREEMPTION";
        }
        if (o == 27) {
            return "DESTINATION_OUT_OF_ORDER";
        }
        if (o == 28) {
            return "INVALID_NUMBER_FORMAT";
        }
        if (o == 29) {
            return "FACILITY_REJECTED";
        }
        if (o == 30) {
            return "RESP_TO_STATUS_ENQUIRY";
        }
        if (o == 31) {
            return "NORMAL_UNSPECIFIED";
        }
        if (o == 34) {
            return "CONGESTION";
        }
        if (o == 38) {
            return "NETWORK_OUT_OF_ORDER";
        }
        if (o == 41) {
            return "TEMPORARY_FAILURE";
        }
        if (o == 42) {
            return "SWITCHING_EQUIPMENT_CONGESTION";
        }
        if (o == 43) {
            return "ACCESS_INFORMATION_DISCARDED";
        }
        if (o == 44) {
            return "REQUESTED_CIRCUIT_OR_CHANNEL_NOT_AVAILABLE";
        }
        if (o == 47) {
            return "RESOURCES_UNAVAILABLE_OR_UNSPECIFIED";
        }
        if (o == 49) {
            return "QOS_UNAVAILABLE";
        }
        if (o == 50) {
            return "REQUESTED_FACILITY_NOT_SUBSCRIBED";
        }
        if (o == 55) {
            return "INCOMING_CALLS_BARRED_WITHIN_CUG";
        }
        if (o == 57) {
            return "BEARER_CAPABILITY_NOT_AUTHORIZED";
        }
        if (o == 58) {
            return "BEARER_CAPABILITY_UNAVAILABLE";
        }
        if (o == 63) {
            return "SERVICE_OPTION_NOT_AVAILABLE";
        }
        if (o == 65) {
            return "BEARER_SERVICE_NOT_IMPLEMENTED";
        }
        if (o == 68) {
            return "ACM_LIMIT_EXCEEDED";
        }
        if (o == 69) {
            return "REQUESTED_FACILITY_NOT_IMPLEMENTED";
        }
        if (o == 70) {
            return "ONLY_DIGITAL_INFORMATION_BEARER_AVAILABLE";
        }
        if (o == 79) {
            return "SERVICE_OR_OPTION_NOT_IMPLEMENTED";
        }
        if (o == 81) {
            return "INVALID_TRANSACTION_IDENTIFIER";
        }
        if (o == 87) {
            return "USER_NOT_MEMBER_OF_CUG";
        }
        if (o == 88) {
            return "INCOMPATIBLE_DESTINATION";
        }
        if (o == 91) {
            return "INVALID_TRANSIT_NW_SELECTION";
        }
        if (o == 95) {
            return "SEMANTICALLY_INCORRECT_MESSAGE";
        }
        if (o == 96) {
            return "INVALID_MANDATORY_INFORMATION";
        }
        if (o == 97) {
            return "MESSAGE_TYPE_NON_IMPLEMENTED";
        }
        if (o == 98) {
            return "MESSAGE_TYPE_NOT_COMPATIBLE_WITH_PROTOCOL_STATE";
        }
        if (o == 99) {
            return "INFORMATION_ELEMENT_NON_EXISTENT";
        }
        if (o == 100) {
            return "CONDITIONAL_IE_ERROR";
        }
        if (o == 101) {
            return "MESSAGE_NOT_COMPATIBLE_WITH_PROTOCOL_STATE";
        }
        if (o == 102) {
            return "RECOVERY_ON_TIMER_EXPIRED";
        }
        if (o == 111) {
            return "PROTOCOL_ERROR_UNSPECIFIED";
        }
        if (o == 127) {
            return "INTERWORKING_UNSPECIFIED";
        }
        if (o == 240) {
            return "CALL_BARRED";
        }
        if (o == 241) {
            return "FDN_BLOCKED";
        }
        if (o == 242) {
            return "IMSI_UNKNOWN_IN_VLR";
        }
        if (o == 243) {
            return "IMEI_NOT_ACCEPTED";
        }
        if (o == 244) {
            return "DIAL_MODIFIED_TO_USSD";
        }
        if (o == 245) {
            return "DIAL_MODIFIED_TO_SS";
        }
        if (o == 246) {
            return "DIAL_MODIFIED_TO_DIAL";
        }
        if (o == 247) {
            return "RADIO_OFF";
        }
        if (o == 248) {
            return "OUT_OF_SERVICE";
        }
        if (o == 249) {
            return "NO_VALID_SIM";
        }
        if (o == 250) {
            return "RADIO_INTERNAL_ERROR";
        }
        if (o == 251) {
            return "NETWORK_RESP_TIMEOUT";
        }
        if (o == 252) {
            return "NETWORK_REJECT";
        }
        if (o == 253) {
            return "RADIO_ACCESS_FAILURE";
        }
        if (o == 254) {
            return "RADIO_LINK_FAILURE";
        }
        if (o == 255) {
            return "RADIO_LINK_LOST";
        }
        if (o == 256) {
            return "RADIO_UPLINK_FAILURE";
        }
        if (o == 257) {
            return "RADIO_SETUP_FAILURE";
        }
        if (o == 258) {
            return "RADIO_RELEASE_NORMAL";
        }
        if (o == 259) {
            return "RADIO_RELEASE_ABNORMAL";
        }
        if (o == 260) {
            return "ACCESS_CLASS_BLOCKED";
        }
        if (o == 261) {
            return "NETWORK_DETACH";
        }
        if (o == 1000) {
            return "CDMA_LOCKED_UNTIL_POWER_CYCLE";
        }
        if (o == 1001) {
            return "CDMA_DROP";
        }
        if (o == 1002) {
            return "CDMA_INTERCEPT";
        }
        if (o == 1003) {
            return "CDMA_REORDER";
        }
        if (o == 1004) {
            return "CDMA_SO_REJECT";
        }
        if (o == 1005) {
            return "CDMA_RETRY_ORDER";
        }
        if (o == 1006) {
            return "CDMA_ACCESS_FAILURE";
        }
        if (o == 1007) {
            return "CDMA_PREEMPTED";
        }
        if (o == 1008) {
            return "CDMA_NOT_EMERGENCY";
        }
        if (o == 1009) {
            return "CDMA_ACCESS_BLOCKED";
        }
        if (o == OEM_CAUSE_1) {
            return "OEM_CAUSE_1";
        }
        if (o == OEM_CAUSE_2) {
            return "OEM_CAUSE_2";
        }
        if (o == OEM_CAUSE_3) {
            return "OEM_CAUSE_3";
        }
        if (o == OEM_CAUSE_4) {
            return "OEM_CAUSE_4";
        }
        if (o == OEM_CAUSE_5) {
            return "OEM_CAUSE_5";
        }
        if (o == OEM_CAUSE_6) {
            return "OEM_CAUSE_6";
        }
        if (o == OEM_CAUSE_7) {
            return "OEM_CAUSE_7";
        }
        if (o == OEM_CAUSE_8) {
            return "OEM_CAUSE_8";
        }
        if (o == OEM_CAUSE_9) {
            return "OEM_CAUSE_9";
        }
        if (o == OEM_CAUSE_10) {
            return "OEM_CAUSE_10";
        }
        if (o == OEM_CAUSE_11) {
            return "OEM_CAUSE_11";
        }
        if (o == OEM_CAUSE_12) {
            return "OEM_CAUSE_12";
        }
        if (o == OEM_CAUSE_13) {
            return "OEM_CAUSE_13";
        }
        if (o == OEM_CAUSE_14) {
            return "OEM_CAUSE_14";
        }
        if (o == OEM_CAUSE_15) {
            return "OEM_CAUSE_15";
        }
        if (o == 65535) {
            return "ERROR_UNSPECIFIED";
        }
        return "0x" + Integer.toHexString(o);
    }
}
