package vendor.huawei.hardware.radio.V1_0;

public final class RILImsRadioTechType {
    public static final int IMS_RADIO_TECH_TYPE_1X_RTT = 6;
    public static final int IMS_RADIO_TECH_TYPE_EDGE = 2;
    public static final int IMS_RADIO_TECH_TYPE_EHRPD = 13;
    public static final int IMS_RADIO_TECH_TYPE_EVDO_0 = 7;
    public static final int IMS_RADIO_TECH_TYPE_EVDO_A = 8;
    public static final int IMS_RADIO_TECH_TYPE_EVDO_B = 12;
    public static final int IMS_RADIO_TECH_TYPE_GPRS = 1;
    public static final int IMS_RADIO_TECH_TYPE_GSM = 16;
    public static final int IMS_RADIO_TECH_TYPE_HSDPA = 9;
    public static final int IMS_RADIO_TECH_TYPE_HSPA = 11;
    public static final int IMS_RADIO_TECH_TYPE_HSPAP = 15;
    public static final int IMS_RADIO_TECH_TYPE_HSUPA = 10;
    public static final int IMS_RADIO_TECH_TYPE_IS95A = 4;
    public static final int IMS_RADIO_TECH_TYPE_IS95B = 5;
    public static final int IMS_RADIO_TECH_TYPE_IWLAN = 19;
    public static final int IMS_RADIO_TECH_TYPE_LTE = 14;
    public static final int IMS_RADIO_TECH_TYPE_TD_SCDMA = 17;
    public static final int IMS_RADIO_TECH_TYPE_UMTS = 3;
    public static final int IMS_RADIO_TECH_TYPE_UNKNOW = 0;
    public static final int IMS_RADIO_TECH_TYPE_WIFI = 18;
    public static final int IMS_RAIDO_THCH_TYPE_ANY = -1;

    public static final java.lang.String dumpBitfield(int r1) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: vendor.huawei.hardware.radio.V1_0.RILImsRadioTechType.dumpBitfield(int):java.lang.String
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:116)
	at jadx.core.dex.nodes.ClassNode.load(ClassNode.java:249)
	at jadx.core.ProcessClass.process(ProcessClass.java:31)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
Caused by: jadx.core.utils.exceptions.DecodeException: Unknown instruction: not-int
	at jadx.core.dex.instructions.InsnDecoder.decode(InsnDecoder.java:568)
	at jadx.core.dex.instructions.InsnDecoder.process(InsnDecoder.java:56)
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:102)
	... 7 more
*/
        /*
        // Can't load method instructions.
        */
        throw new UnsupportedOperationException("Method not decompiled: vendor.huawei.hardware.radio.V1_0.RILImsRadioTechType.dumpBitfield(int):java.lang.String");
    }

    public static final String toString(int o) {
        if (o == -1) {
            return "IMS_RAIDO_THCH_TYPE_ANY";
        }
        if (o == 0) {
            return "IMS_RADIO_TECH_TYPE_UNKNOW";
        }
        if (o == 1) {
            return "IMS_RADIO_TECH_TYPE_GPRS";
        }
        if (o == 2) {
            return "IMS_RADIO_TECH_TYPE_EDGE";
        }
        if (o == 3) {
            return "IMS_RADIO_TECH_TYPE_UMTS";
        }
        if (o == 4) {
            return "IMS_RADIO_TECH_TYPE_IS95A";
        }
        if (o == 5) {
            return "IMS_RADIO_TECH_TYPE_IS95B";
        }
        if (o == 6) {
            return "IMS_RADIO_TECH_TYPE_1X_RTT";
        }
        if (o == 7) {
            return "IMS_RADIO_TECH_TYPE_EVDO_0";
        }
        if (o == 8) {
            return "IMS_RADIO_TECH_TYPE_EVDO_A";
        }
        if (o == 9) {
            return "IMS_RADIO_TECH_TYPE_HSDPA";
        }
        if (o == 10) {
            return "IMS_RADIO_TECH_TYPE_HSUPA";
        }
        if (o == 11) {
            return "IMS_RADIO_TECH_TYPE_HSPA";
        }
        if (o == 12) {
            return "IMS_RADIO_TECH_TYPE_EVDO_B";
        }
        if (o == 13) {
            return "IMS_RADIO_TECH_TYPE_EHRPD";
        }
        if (o == 14) {
            return "IMS_RADIO_TECH_TYPE_LTE";
        }
        if (o == 15) {
            return "IMS_RADIO_TECH_TYPE_HSPAP";
        }
        if (o == 16) {
            return "IMS_RADIO_TECH_TYPE_GSM";
        }
        if (o == 17) {
            return "IMS_RADIO_TECH_TYPE_TD_SCDMA";
        }
        if (o == 18) {
            return "IMS_RADIO_TECH_TYPE_WIFI";
        }
        if (o == 19) {
            return "IMS_RADIO_TECH_TYPE_IWLAN";
        }
        return "0x" + Integer.toHexString(o);
    }
}
